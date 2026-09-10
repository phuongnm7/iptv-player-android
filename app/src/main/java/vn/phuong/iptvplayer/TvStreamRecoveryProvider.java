package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** TV-only reliability layer: resilient playlist loading and decoder recovery. */
@androidx.media3.common.util.UnstableApi
public final class TvStreamRecoveryProvider extends ContentProvider implements Application.ActivityLifecycleCallbacks {
    private static final int MAX_PLAYLIST_BYTES = 8 * 1024 * 1024;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean playlistLoading = new AtomicBoolean(false);
    private final Map<ExoPlayer, Player.Listener> attachedPlayers = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<PlayerActivity, DecoderState> decoderStates = Collections.synchronizedMap(new WeakHashMap<>());
    private PlayerActivity activePlayerActivity;

    @Override public boolean onCreate() {
        if (getContext() != null && AppPreferences.isPhysicalTv(getContext())) {
            ((Application) getContext().getApplicationContext()).registerActivityLifecycleCallbacks(this);
        }
        return true;
    }

    @Override public void onActivityResumed(Activity activity) {
        if (activity instanceof MainActivity) installPlaylistHooks((MainActivity) activity);
        if (activity instanceof PlayerActivity) {
            activePlayerActivity = (PlayerActivity) activity;
            main.removeCallbacks(playerWatch);
            main.post(playerWatch);
        }
    }

    @Override public void onActivityPaused(Activity activity) {
        if (activity == activePlayerActivity) {
            activePlayerActivity = null;
            main.removeCallbacks(playerWatch);
        }
    }

    private void installPlaylistHooks(MainActivity activity) {
        EditText input = activity.findViewById(R.id.inputUrl);
        View load = activity.findViewById(R.id.btnLoadUrl);
        View reload = activity.findViewById(R.id.btnReloadUrl);
        if (input == null || load == null) return;
        load.setOnClickListener(v -> loadPlaylist(activity, input.getText().toString().trim()));
        if (reload != null) reload.setOnClickListener(v -> {
            String source = privateString(activity, "currentSource");
            if (source != null && source.contains("\n")) source = source.split("\n", 2)[0];
            if (source == null || source.trim().isEmpty()) source = input.getText().toString().trim();
            input.setText(source);
            loadPlaylist(activity, source.trim());
        });
    }

    private void loadPlaylist(MainActivity activity, String source) {
        if (source == null || !(source.startsWith("http://") || source.startsWith("https://"))) {
            new AlertDialog.Builder(activity).setTitle("Lỗi").setMessage("URL phải bắt đầu bằng http:// hoặc https://")
                    .setPositiveButton("Đóng", null).show();
            return;
        }
        if (!playlistLoading.compareAndSet(false, true)) return;
        ProgressBar progress = activity.findViewById(R.id.progress);
        if (progress != null) progress.setVisibility(View.VISIBLE);
        io.execute(() -> {
            Exception last = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(source).openConnection();
                    connection.setConnectTimeout(25_000);
                    connection.setReadTimeout(60_000);
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestProperty("User-Agent", "Nm7-IPTV/1.10.11 Android-TV");
                    connection.setRequestProperty("Accept", "application/vnd.apple.mpegurl,application/x-mpegURL,text/plain,*/*");
                    connection.setRequestProperty("Connection", "keep-alive");
                    int code = connection.getResponseCode();
                    if (code < 200 || code >= 300) throw new java.io.IOException("HTTP " + code);
                    String effective = connection.getURL().toString();
                    String type = connection.getContentType();
                    String description = source.equals(effective) ? source : source + "\nChuyển hướng: " + effective;
                    M3uParser.Result result;
                    if (type != null && (type.startsWith("video/") || type.contains("dash+xml"))) {
                        Channel direct = new Channel("Luồng trực tiếp", "Phát trực tiếp", effective, "", "", Collections.emptyMap());
                        if (type.contains("dash+xml")) direct.options().add("#KODIPROP:inputstream.adaptive.manifest_type=mpd");
                        result = new M3uParser.Result(Collections.singletonList(direct), 0, 0);
                    } else {
                        String text;
                        try (InputStream in = new BufferedInputStream(connection.getInputStream())) {
                            text = readText(in);
                        }
                        result = new M3uParser().parse(text, effective);
                    }
                    M3uParser.Result ready = result;
                    main.post(() -> {
                        playlistLoading.set(false);
                        if (progress != null) progress.setVisibility(View.GONE);
                        invokeShowPlaylist(activity, ready, description);
                    });
                    return;
                } catch (Exception error) {
                    last = error;
                    if (attempt < 3) {
                        int shownAttempt = attempt + 1;
                        main.post(() -> {
                            TextView summary = activity.findViewById(R.id.txtSummary);
                            if (summary != null) summary.setText("Kết nối chậm • đang thử lại " + shownAttempt + "/3…");
                        });
                        try { Thread.sleep(attempt == 1 ? 700L : 1600L); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); break; }
                    }
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
            Exception failure = last;
            main.post(() -> {
                playlistLoading.set(false);
                if (progress != null) progress.setVisibility(View.GONE);
                String message = failure == null ? "Không tải được playlist" : readable(failure);
                new AlertDialog.Builder(activity).setTitle("Lỗi")
                        .setMessage("Không tải được playlist sau 3 lần: " + message)
                        .setPositiveButton("Thử lại", (d, w) -> loadPlaylist(activity, source))
                        .setNegativeButton("Đóng", null).show();
            });
        });
    }

    private static String readText(InputStream input) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > MAX_PLAYLIST_BYTES) throw new java.io.IOException("Playlist lớn hơn 8 MB");
            out.write(buffer, 0, count);
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }

    private static void invokeShowPlaylist(MainActivity activity, M3uParser.Result result, String description) {
        try {
            Method method = MainActivity.class.getDeclaredMethod("showPlaylist", M3uParser.Result.class, String.class);
            method.setAccessible(true);
            method.invoke(activity, result, description);
        } catch (Exception error) {
            new AlertDialog.Builder(activity).setTitle("Lỗi").setMessage("Không thể cập nhật danh sách kênh: " + readable(error))
                    .setPositiveButton("Đóng", null).show();
        }
    }

    private final Runnable playerWatch = new Runnable() {
        @Override public void run() {
            PlayerActivity activity = activePlayerActivity;
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
            attachDecoderRecovery(activity);
            main.postDelayed(this, 900L);
        }
    };

    private void attachDecoderRecovery(PlayerActivity activity) {
        ExoPlayer player = privatePlayer(activity);
        if (player == null || attachedPlayers.containsKey(player)) return;
        Player.Listener listener = new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                String name = error.getErrorCodeName();
                if (name == null || !name.contains("DECOD")) return;
                recoverDecoder(activity, name);
            }
        };
        player.addListener(listener);
        attachedPlayers.put(player, listener);
        PlaybackException existing = player.getPlayerError();
        if (existing != null) {
            String name = existing.getErrorCodeName();
            if (name != null && name.contains("DECOD")) recoverDecoder(activity, name);
        }
    }

    private void recoverDecoder(PlayerActivity activity, String errorName) {
        String url = privateString(activity, "url");
        if (url == null) url = "";
        DecoderState state = decoderStates.get(activity);
        if (state == null || !url.equals(state.url)) {
            state = new DecoderState(url, privateInt(activity, "quality", Integer.MAX_VALUE));
            decoderStates.put(activity, state);
        }
        final int target;
        if (state.stage == 0) target = 1080;
        else if (state.stage == 1) target = 720;
        else return;
        state.stage++;
        TextView status = activity.findViewById(R.id.txtPlayerStatus);
        if (status != null) status.setText("Decoder TV lỗi • đang thử lại tối đa " + target + "p…");
        View error = activity.findViewById(R.id.playerError);
        if (error != null) error.setVisibility(View.GONE);
        int restoreQuality = state.originalQuality;
        main.postDelayed(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            try {
                setPrivateInt(activity, "quality", target);
                setPrivateLong(activity, "position", 0L);
                setPrivateBoolean(activity, "resumePlayback", true);
                invokePrivate(activity, "releasePlayer");
                invokePrivate(activity, "startPlayer");
                setPrivateInt(activity, "quality", restoreQuality);
                main.postDelayed(() -> attachDecoderRecovery(activity), 500L);
            } catch (Exception ignored) {
                setPrivateInt(activity, "quality", restoreQuality);
            }
        }, 350L);
    }

    private static ExoPlayer privatePlayer(PlayerActivity activity) {
        try {
            Field field = PlayerActivity.class.getDeclaredField("player");
            field.setAccessible(true);
            return (ExoPlayer) field.get(activity);
        } catch (Exception ignored) { return null; }
    }

    private static String privateString(Object target, String name) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); Object value = f.get(target); return value == null ? "" : value.toString(); }
        catch (Exception ignored) { return ""; }
    }
    private static int privateInt(Object target, String name, int fallback) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.getInt(target); }
        catch (Exception ignored) { return fallback; }
    }
    private static void setPrivateInt(Object target, String name, int value) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); f.setInt(target, value); } catch (Exception ignored) { }
    }
    private static void setPrivateLong(Object target, String name, long value) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); f.setLong(target, value); } catch (Exception ignored) { }
    }
    private static void setPrivateBoolean(Object target, String name, boolean value) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); f.setBoolean(target, value); } catch (Exception ignored) { }
    }
    private static void invokePrivate(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }
    private static String readable(Exception error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        String message = cause.getMessage();
        return message == null || message.trim().isEmpty() ? cause.getClass().getSimpleName() : message;
    }

    private static final class DecoderState {
        final String url;
        final int originalQuality;
        int stage;
        DecoderState(String url, int originalQuality) { this.url = url; this.originalQuality = originalQuality; }
    }

    @Override public void onActivityCreated(Activity a, Bundle b) { }
    @Override public void onActivityStarted(Activity a) { }
    @Override public void onActivityStopped(Activity a) { }
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
    @Override public void onActivityDestroyed(Activity a) { if (a == activePlayerActivity) activePlayerActivity = null; }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String sort) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
