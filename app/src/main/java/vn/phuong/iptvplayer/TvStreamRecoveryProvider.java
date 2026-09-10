package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.ui.PlayerView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * TV-only decoder recovery.
 *
 * Some Android TV firmwares advertise a 4K codec as supported, then fail after decoding starts.
 * ExoPlayer's normal decoder fallback mainly helps decoder-initialization failures, not every
 * runtime MediaCodec failure. This provider therefore adds four TV-specific recovery stages:
 * adaptive 1080p, adaptive 720p, synchronous MediaCodec, then software-first MediaCodec.
 */
@androidx.media3.common.util.UnstableApi
public final class TvStreamRecoveryProvider extends ContentProvider implements Application.ActivityLifecycleCallbacks {
    private final Handler main = new Handler(Looper.getMainLooper());
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

    private final Runnable playerWatch = new Runnable() {
        @Override public void run() {
            PlayerActivity activity = activePlayerActivity;
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
            attachDecoderRecovery(activity);
            main.postDelayed(this, 300L);
        }
    };

    private void attachDecoderRecovery(PlayerActivity activity) {
        ExoPlayer player = privatePlayer(activity);
        if (player == null || attachedPlayers.containsKey(player)) return;
        Player.Listener listener = new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                if (isDecoderError(error)) recoverDecoder(activity);
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) markReady(activity, player);
            }
        };
        player.addListener(listener);
        attachedPlayers.put(player, listener);
        PlaybackException existing = player.getPlayerError();
        if (existing != null && isDecoderError(existing)) recoverDecoder(activity);
    }

    private static boolean isDecoderError(PlaybackException error) {
        String name = error == null ? null : error.getErrorCodeName();
        return name != null && name.contains("DECOD");
    }

    private void recoverDecoder(PlayerActivity activity) {
        String url = privateString(activity, "url");
        DecoderState state = decoderStates.get(activity);
        if (state == null || !url.equals(state.url)) {
            state = new DecoderState(url, privateInt(activity, "quality", Integer.MAX_VALUE));
            decoderStates.put(activity, state);
        }
        if (state.recovering) return;
        state.recovering = true;

        View error = activity.findViewById(R.id.playerError);
        if (error != null) error.setVisibility(View.GONE);

        if (state.stage == 0) {
            state.stage++;
            restartAdaptive(activity, state, 1080);
        } else if (state.stage == 1) {
            state.stage++;
            restartAdaptive(activity, state, 720);
        } else if (state.stage == 2) {
            state.stage++;
            startCustomCodecFallback(activity, state, false);
        } else if (state.stage == 3) {
            state.stage++;
            startCustomCodecFallback(activity, state, true);
        } else {
            state.recovering = false;
            showFinalDecoderError(activity);
        }
    }

    private void restartAdaptive(PlayerActivity activity, DecoderState state, int target) {
        TextView status = activity.findViewById(R.id.txtPlayerStatus);
        if (status != null) status.setText("Decoder TV lỗi • đang thử luồng tối đa " + target + "p…");
        int restoreQuality = state.originalQuality;
        main.postDelayed(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            try {
                setPrivateInt(activity, "quality", target);
                setPrivateLong(activity, "position", 0L);
                setPrivateBoolean(activity, "resumePlayback", true);
                invokePrivate(activity, "releasePlayer");
                invokePrivate(activity, "startPlayer");
            } catch (Exception ignored) {
                state.recovering = false;
                recoverDecoder(activity);
            } finally {
                // startPlayer applies the quality limit to the newly created player immediately.
                // Restore the preference field so the next channel is not permanently capped.
                setPrivateInt(activity, "quality", restoreQuality);
            }
            main.postDelayed(() -> {
                state.recovering = false;
                attachDecoderRecovery(activity);
                ExoPlayer current = privatePlayer(activity);
                PlaybackException existing = current == null ? null : current.getPlayerError();
                if (existing != null && isDecoderError(existing)) recoverDecoder(activity);
            }, 450L);
        }, 180L);
    }

    private void startCustomCodecFallback(PlayerActivity activity, DecoderState state, boolean softwareFirst) {
        TextView status = activity.findViewById(R.id.txtPlayerStatus);
        if (status != null) {
            status.setText(softwareFirst
                    ? "Decoder TV lỗi • đang thử bộ giải mã phần mềm…"
                    : "Decoder TV lỗi • đang thử chế độ codec tương thích…");
        }
        main.postDelayed(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            try {
                invokePrivate(activity, "releasePlayer");
                ExoPlayer fallback = buildFallbackPlayer(activity, softwareFirst, state);
                setPrivateObject(activity, "player", fallback);
                PlayerView playerView = activity.findViewById(R.id.playerView);
                playerView.setPlayer(fallback);
                fallback.prepare();
                fallback.play();
                attachedPlayers.put(fallback, state.listener);
            } catch (Exception error) {
                state.recovering = false;
                recoverDecoder(activity);
            }
        }, 180L);
    }

    private ExoPlayer buildFallbackPlayer(PlayerActivity activity, boolean softwareFirst, DecoderState state) throws Exception {
        String url = privateString(activity, "url");
        String mime = privateString(activity, "mime");
        @SuppressWarnings("unchecked") ArrayList<String> options = (ArrayList<String>) privateObject(activity, "options");
        if (options == null) options = new ArrayList<>();
        Bundle headerBundle = (Bundle) privateObject(activity, "currentHeaders");
        Map<String,String> headers = new LinkedHashMap<>();
        if (headerBundle != null) {
            for (String key : headerBundle.keySet()) {
                String value = headerBundle.getString(key);
                if (value != null) headers.put(key, value);
            }
        }
        String userAgent = headers.containsKey("User-Agent") ? headers.get("User-Agent") : "Nm7-IPTV/1.10.12 Android TV";
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setConnectTimeoutMs(25_000)
                .setReadTimeoutMs(60_000)
                .setAllowCrossProtocolRedirects(true)
                .setDefaultRequestProperties(headers);
        DefaultDataSource.Factory data = new DefaultDataSource.Factory(activity, http);
        DefaultMediaSourceFactory mediaFactory = new DefaultMediaSourceFactory(data)
                .setLoadErrorHandlingPolicy(new DefaultLoadErrorHandlingPolicy(8));

        MediaItem.Builder item = new MediaItem.Builder().setUri(url);
        String inferred = mime.isEmpty() ? StreamSpec.inferMime(url, options) : mime;
        if (inferred != null && !inferred.isEmpty()) item.setMimeType(inferred);

        DrmSpec drm = DrmSpec.create(privateString(activity, "drmSystem"), privateString(activity, "drmLicense"));
        if (drm.remoteClearKey()) throw new IllegalStateException("ClearKey chưa được giải quyết");
        DrmPlayback.configure(drm, item, mediaFactory);

        DefaultRenderersFactory renderers = new DefaultRenderersFactory(activity)
                .setEnableDecoderFallback(true)
                .forceDisableMediaCodecAsynchronousQueueing();
        if (softwareFirst) renderers.setMediaCodecSelector(MediaCodecSelector.PREFER_SOFTWARE);

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 60_000, 1_000, 2_500)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();
        ExoPlayer fallback = new ExoPlayer.Builder(activity, renderers)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(mediaFactory)
                .build();
        fallback.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(), true);
        fallback.setHandleAudioBecomingNoisy(true);
        fallback.setMediaItem(item.build());

        state.listener = new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                if (isDecoderError(error)) {
                    state.recovering = false;
                    recoverDecoder(activity);
                } else {
                    state.recovering = false;
                    showFallbackError(activity, error);
                }
            }
            @Override public void onVideoSizeChanged(VideoSize size) {
                TextView status = activity.findViewById(R.id.txtPlayerStatus);
                if (status != null && size.width > 0) status.setText(size.width + " × " + size.height + " • độ phân giải thực tế");
            }
            @Override public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) markReady(activity, fallback);
                else if (playbackState == Player.STATE_BUFFERING) {
                    TextView status = activity.findViewById(R.id.txtPlayerStatus);
                    if (status != null) status.setText("Đang tải luồng…");
                }
            }
        };
        fallback.addListener(state.listener);
        fallback.addAnalyticsListener(new AnalyticsListener() {
            @Override public void onVideoEnabled(EventTime eventTime, DecoderCounters counters) {
                setPrivateObject(activity, "videoCounters", counters);
            }
        });
        return fallback;
    }

    private void markReady(PlayerActivity activity, ExoPlayer current) {
        DecoderState state = decoderStates.get(activity);
        if (state != null) {
            state.recovering = false;
            state.readyPlayer = current;
            int stageSnapshot = state.stage;
            main.postDelayed(() -> {
                DecoderState now = decoderStates.get(activity);
                if (now == state && now.readyPlayer == current && current.isPlaying() && now.stage == stageSnapshot) {
                    // A player that stays healthy for a while gets a fresh recovery budget.
                    now.stage = 0;
                }
            }, 30_000L);
        }
        View error = activity.findViewById(R.id.playerError);
        if (error != null) error.setVisibility(View.GONE);
        TextView status = activity.findViewById(R.id.txtPlayerStatus);
        VideoSize size = current.getVideoSize();
        if (status != null) status.setText(size.width > 0
                ? size.width + " × " + size.height + " • độ phân giải thực tế"
                : "Đang phát");
    }

    private void showFallbackError(PlayerActivity activity, PlaybackException error) {
        View panel = activity.findViewById(R.id.playerError);
        TextView text = activity.findViewById(R.id.txtPlayerError);
        if (panel != null) panel.setVisibility(View.VISIBLE);
        if (text != null) text.setText(error.getErrorCodeName() + "\nKhông thể duy trì luồng trên bộ giải mã TV này.");
    }

    private void showFinalDecoderError(PlayerActivity activity) {
        View panel = activity.findViewById(R.id.playerError);
        TextView text = activity.findViewById(R.id.txtPlayerError);
        if (panel != null) panel.setVisibility(View.VISIBLE);
        if (text != null) text.setText("ERROR_CODE_DECODING_FAILED\nĐã thử 1080p, 720p, codec tương thích và decoder phần mềm.\nNguồn này có thể dùng codec/profile mà TV không hỗ trợ.");
    }

    private static ExoPlayer privatePlayer(PlayerActivity activity) {
        Object value = privateObject(activity, "player");
        return value instanceof ExoPlayer ? (ExoPlayer) value : null;
    }
    private static Object privateObject(Object target, String name) {
        try { Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target); }
        catch (Exception ignored) { return null; }
    }
    private static String privateString(Object target, String name) {
        Object value = privateObject(target, name);
        return value == null ? "" : value.toString();
    }
    private static int privateInt(Object target, String name, int fallback) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); return f.getInt(target); }
        catch (Exception ignored) { return fallback; }
    }
    private static void setPrivateObject(Object target, String name, Object value) {
        try { Field f = target.getClass().getDeclaredField(name); f.setAccessible(true); f.set(target, value); } catch (Exception ignored) { }
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

    private static final class DecoderState {
        final String url;
        final int originalQuality;
        int stage;
        boolean recovering;
        Player.Listener listener;
        ExoPlayer readyPlayer;
        DecoderState(String url, int originalQuality) { this.url = url; this.originalQuality = originalQuality; }
    }

    @Override public void onActivityCreated(Activity a, Bundle b) { }
    @Override public void onActivityStarted(Activity a) { }
    @Override public void onActivityStopped(Activity a) { }
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
    @Override public void onActivityDestroyed(Activity a) {
        decoderStates.remove(a);
        if (a == activePlayerActivity) activePlayerActivity = null;
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String sort) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}