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

import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** TV-only decoder recovery. Playlist retry lives in MainActivity so startup is covered too. */
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
            main.postDelayed(this, 700L);
        }
    };

    private void attachDecoderRecovery(PlayerActivity activity) {
        ExoPlayer player = privatePlayer(activity);
        if (player == null || attachedPlayers.containsKey(player)) return;
        Player.Listener listener = new Player.Listener() {
            @Override public void onPlayerError(PlaybackException error) {
                String name = error.getErrorCodeName();
                if (name != null && name.contains("DECOD")) recoverDecoder(activity);
            }
        };
        player.addListener(listener);
        attachedPlayers.put(player, listener);
        PlaybackException existing = player.getPlayerError();
        if (existing != null) {
            String name = existing.getErrorCodeName();
            if (name != null && name.contains("DECOD")) recoverDecoder(activity);
        }
    }

    private void recoverDecoder(PlayerActivity activity) {
        String url = privateString(activity, "url");
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
        if (status != null) status.setText("Decoder TV lỗi • đang thử luồng tối đa " + target + "p…");
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
                main.postDelayed(() -> attachDecoderRecovery(activity), 350L);
            } catch (Exception ignored) {
                setPrivateInt(activity, "quality", restoreQuality);
            }
        }, 250L);
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
