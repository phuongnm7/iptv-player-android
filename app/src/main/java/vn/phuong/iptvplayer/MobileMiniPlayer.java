package vn.phuong.iptvplayer;

import android.app.Activity;
import android.view.Gravity;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;

/** One bounded touch surface shared across native Browse and the IPTV list. */
public final class MobileMiniPlayer {
    private static FrameLayout host;
    private static TextureView surface;
    private static final String PLAYER = "com.liskovsoft.smartyoutubetv2.droid.ui.playback.PlaybackActivity";
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
            int width = Math.min((int)(220 * d), activity.getResources().getDisplayMetrics().widthPixels - (int)(24 * d));
            FrameLayout.LayoutParams box = new FrameLayout.LayoutParams(width, width * 9 / 16, Gravity.BOTTOM | Gravity.END);
            box.bottomMargin = (int)(80 * d); // 64dp navigation + 16dp clear separation.
            box.rightMargin = (int)(12 * d);
            TextureView video = new TextureView(activity);
            next.addView(video, new FrameLayout.LayoutParams(-1, -1));
            video.setOnClickListener(v -> {
                try { bridge.getMethod("restoreNm7Player").invoke(null); }
                catch (ReflectiveOperationException e) { android.util.Log.e("NM7Playback", "Restore mini", e); }
            });
            ImageButton close = new ImageButton(activity);
            close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            close.setContentDescription("Đóng video YouTube");
            next.addView(close, new FrameLayout.LayoutParams((int)(40*d), (int)(40*d), Gravity.TOP | Gravity.END));
            close.setOnClickListener(v -> MobileNm7Application.stopYoutubeForIptv());
            root.addView(next, box);
            final FrameLayout previous = host;
            host = next;
            surface = video;

            // Keep the old surface alive until the replacement TextureView is actually ready.
            // Removing it earlier can leave ExoPlayer audio running with no render surface.
            final Runnable attachSurface = () -> {
                if (host != next || surface != video || activity.isFinishing() || activity.isDestroyed()) return;
                try {
                    bridge.getMethod("attachNm7MiniPlayer", TextureView.class).invoke(null, video);
                    if (previous != null && previous.getParent() instanceof ViewGroup)
                        ((ViewGroup) previous.getParent()).removeView(previous);
                } catch (ReflectiveOperationException | RuntimeException error) {
                    android.util.Log.e("NM7Playback", "Attach ready mini surface", error);
                }
            };
            if (video.isAvailable()) {
                video.post(attachSurface);
            } else {
                video.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    @Override public void onSurfaceTextureAvailable(android.graphics.SurfaceTexture st, int w, int h) {
                        video.post(attachSurface);
                    }
                    @Override public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture st, int w, int h) { }
                    @Override public boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture st) { return true; }
                    @Override public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture st) { }
                });
            }

        } catch (ReflectiveOperationException | RuntimeException e) {
            android.util.Log.e("NM7Playback", "Attach mini", e);
        }
    }

    public static void remove() {
        if (host != null && host.getParent() instanceof ViewGroup)
            ((ViewGroup) host.getParent()).removeView(host);
        host = null;
        surface = null;
    }
}
