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
            video.setClickable(false);
            video.setFocusable(false);
            next.addView(video, new FrameLayout.LayoutParams(-1, -1));

            View tapShield = new View(activity);
            tapShield.setClickable(true);
            tapShield.setFocusable(true);
            tapShield.setOnClickListener(v -> {
                try {
                    bridge.getMethod("restoreNm7Player").invoke(null);
                } catch (ReflectiveOperationException error) {
                    android.util.Log.e("NM7Playback", "Restore mini", error);
                }
            });
            next.addView(tapShield, new FrameLayout.LayoutParams(-1, -1));
            tapShield.setOnTouchListener(new View.OnTouchListener() {
                float downX, downY, startX, startY;
                boolean dragging;
                @Override public boolean onTouch(View view, android.view.MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            downX = event.getRawX(); downY = event.getRawY();
                            startX = next.getTranslationX(); startY = next.getTranslationY();
                            dragging = false;
                            return true;
                        case android.view.MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - downX, dy = event.getRawY() - downY;
                            if (Math.abs(dx) + Math.abs(dy) > android.view.ViewConfiguration.get(activity).getScaledTouchSlop()) dragging = true;
                            if (dragging) {
                                float x = Math.max(0, Math.min(root.getWidth() - next.getWidth(), next.getLeft() + startX + dx));
                                float y = Math.max(0, Math.min(root.getHeight() - next.getHeight() - 80 * d, next.getTop() + startY + dy));
                                next.setTranslationX(x - next.getLeft());
                                next.setTranslationY(y - next.getTop());
                            }
                            return true;
                        case android.view.MotionEvent.ACTION_UP:
                            if (!dragging) view.performClick();
                            return true;
                        case android.view.MotionEvent.ACTION_CANCEL:
                            return true;
                        default: return true;
                    }
                }
            });
            next.setClickable(true);

            ImageButton close = new ImageButton(activity);
            close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            close.setContentDescription("Đóng video YouTube");
            next.addView(close, new FrameLayout.LayoutParams(
                    (int)(40 * d), (int)(40 * d), Gravity.TOP | Gravity.END));
            close.setOnClickListener(v -> MobileNm7Application.stopYoutubeForIptv());

            ImageButton playPause = new ImageButton(activity);
            playPause.setBackgroundColor(0x66000000);
            playPause.setColorFilter(android.graphics.Color.WHITE);
            next.addView(playPause, new FrameLayout.LayoutParams(
                    (int)(48 * d), (int)(48 * d), Gravity.TOP | Gravity.START));
            android.widget.ProgressBar buffering = new android.widget.ProgressBar(activity);
            buffering.setClickable(false);
            next.addView(buffering, new FrameLayout.LayoutParams((int)(32 * d), (int)(32 * d), Gravity.CENTER));
            android.widget.TextView status = new android.widget.TextView(activity);
            status.setTextColor(android.graphics.Color.WHITE);
            status.setBackgroundColor(0xaa000000);
            status.setTextSize(12);
            status.setGravity(Gravity.CENTER);
            status.setText("Lỗi phát • bấm ▶ để thử lại");
            next.addView(status, new FrameLayout.LayoutParams(-1, (int)(32 * d), Gravity.BOTTOM));
            Runnable refreshControl = new Runnable() {
                @Override public void run() {
                    if (host != next || !next.isAttachedToWindow()) return;
                    try {
                        int state = ((Number) bridge.getMethod("getNm7PlaybackState").invoke(null)).intValue();
                        buffering.setVisibility(state == 2 ? View.VISIBLE : View.GONE);
                        status.setVisibility(state == -1 ? View.VISIBLE : View.GONE);
                        boolean playing = state != -1 && Boolean.TRUE.equals(bridge.getMethod("isNm7Playing").invoke(null));
                        playPause.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                        playPause.setContentDescription(playing ? "Tạm dừng YouTube" : "Phát YouTube");
                    } catch (ReflectiveOperationException ignored) { }
                    next.postDelayed(this, 500);
                }
            };
            playPause.setOnClickListener(v -> {
                try { bridge.getMethod("toggleNm7Playback").invoke(null); }
                catch (ReflectiveOperationException error) {
                    android.util.Log.e("NM7Playback", "Toggle mini playback", error);
                }
            });

            root.addView(next, box);

            final FrameLayout previousHost = host;
            final View previousSurface = surface;
            host = next;
            surface = video;
            next.post(refreshControl);

            video.post(() -> {
                if (host != next || surface != video
                        || activity.isFinishing() || activity.isDestroyed()) return;
                try {
                    bridge.getMethod("attachNm7MiniPlayer", View.class, View.class)
                            .invoke(null, previousSurface, video);
                    if (previousHost != null && previousHost.getParent() instanceof ViewGroup) {
                        ((ViewGroup) previousHost.getParent()).removeView(previousHost);
                    }
                } catch (ReflectiveOperationException | RuntimeException error) {
                    android.util.Log.e("NM7Playback", "Switch mini PlayerView", error);
                }
            });
        } catch (ReflectiveOperationException | RuntimeException error) {
            android.util.Log.e("NM7Playback", "Attach mini", error);
        }
    }

    private static View createPlayerView(Activity activity) throws ReflectiveOperationException {
        // Texture-backed PlayerView keeps ExoPlayer's supported target handoff while
        // avoiding separate SurfaceView windows across paused/resumed Activities.
        return android.view.LayoutInflater.from(activity).inflate(R.layout.nm7_mini_player, null, false);
    }

    public static View surfaceView() {
        return surface;
    }

    public static boolean containsPoint(float rawX, float rawY) {
        if (host == null || !host.isShown()) return false;
        android.graphics.Rect bounds = new android.graphics.Rect();
        return host.getGlobalVisibleRect(bounds) && bounds.contains((int) rawX, (int) rawY);
    }

    public static void remove() {
        if (host != null && host.getParent() instanceof ViewGroup) {
            ((ViewGroup) host.getParent()).removeView(host);
        }
        host = null;
        surface = null;
    }
}

