package vn.phuong.iptvplayer;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

/** One bounded touch surface shared across native Browse and the IPTV list. */
public final class MobileMiniPlayer {
    private static FrameLayout host;
    private static View surface;
    private static final String PLAYER = "com.liskovsoft.smartyoutubetv2.droid.ui.playback.PlaybackActivity";
    private static final String EXO_PLAYER_VIEW = "com.google.android.exoplayer2.ui.PlayerView";

    private MobileMiniPlayer() { }

    public static void attach(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) return;
        try {
            Class<?> bridge = Class.forName(PLAYER);
            if (!Boolean.TRUE.equals(bridge.getMethod("isNm7MiniPlayerActive").invoke(null))) {
                remove();
                return;
            }
            ViewGroup root = activity.findViewById(android.R.id.content);
            if (!(root instanceof FrameLayout)) return;
            if (host != null && host.getParent() == root) return;

            float d = activity.getResources().getDisplayMetrics().density;
            FrameLayout next = new FrameLayout(activity);
            next.setBackgroundColor(0xff111318);
            next.setElevation(4 * d);
            int width = Math.min((int)(220 * d),
                    activity.getResources().getDisplayMetrics().widthPixels - (int)(24 * d));
            FrameLayout.LayoutParams box = new FrameLayout.LayoutParams(
                    width, width * 9 / 16, Gravity.BOTTOM | Gravity.END);
            box.bottomMargin = (int)(80 * d);
            box.rightMargin = (int)(12 * d);

            View video = createPlayerView(activity);
            next.addView(video, new FrameLayout.LayoutParams(-1, -1));
            video.setOnClickListener(v -> {
                try {
                    bridge.getMethod("restoreNm7Player").invoke(null);
                } catch (ReflectiveOperationException error) {
                    android.util.Log.e("NM7Playback", "Restore mini", error);
                }
            });

            ImageButton close = new ImageButton(activity);
            close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            close.setContentDescription("Đóng video YouTube");
            next.addView(close, new FrameLayout.LayoutParams(
                    (int)(40 * d), (int)(40 * d), Gravity.TOP | Gravity.END));
            close.setOnClickListener(v -> MobileNm7Application.stopYoutubeForIptv());

            root.addView(next, box);

            final FrameLayout previousHost = host;
            final View previousSurface = surface;
            host = next;
            surface = video;

            // Let ExoPlayer PlayerView own its SurfaceView/TextureView lifecycle.
            // No manual clearVideoSurface()/seekTo() is used: those calls caused
            // decoder stalls and audio-only mini playback on some phones.
            video.post(() -> {
                if (host != next || surface != video
                        || activity.isFinishing() || activity.isDestroyed()) return;
                try {
                    bridge.getMethod("attachNm7MiniPlayer", View.class, View.class)
                            .invoke(null, previousSurface, video);
                    if (previousHost != null && previousHost.getParent() instanceof ViewGroup)
                        ((ViewGroup) previousHost.getParent()).removeView(previousHost);
                } catch (ReflectiveOperationException | RuntimeException error) {
                    android.util.Log.e("NM7Playback", "Attach mini PlayerView", error);
                }
            });
        } catch (ReflectiveOperationException | RuntimeException error) {
            android.util.Log.e("NM7Playback", "Attach mini", error);
        }
    }

    private static View createPlayerView(Activity activity) throws ReflectiveOperationException {
        Class<?> cls = Class.forName(EXO_PLAYER_VIEW);
        Object object = cls.getConstructor(android.content.Context.class).newInstance(activity);
        cls.getMethod("setUseController", boolean.class).invoke(object, false);
        return (View) object;
    }

    private static void detachPlayerView(View view) {
        if (view == null) return;
        try {
            for (java.lang.reflect.Method method : view.getClass().getMethods()) {
                if ("setPlayer".equals(method.getName()) && method.getParameterTypes().length == 1) {
                    method.invoke(view, new Object[]{null});
                    return;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            android.util.Log.e("NM7Playback", "Detach mini PlayerView", error);
        }
    }

    public static void remove() {
        detachPlayerView(surface);
        if (host != null && host.getParent() instanceof ViewGroup)
            ((ViewGroup) host.getParent()).removeView(host);
        host = null;
        surface = null;
    }
}
