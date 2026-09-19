    private static PlaybackActivity sNm7Active;
    private static boolean sNm7Mini;
    private boolean mNm7Stopped;
    private boolean mNm7OwnsPlayback;
    private Object mNm7BackCallback;

    public static boolean isNm7MiniPlayerActive() {
        return sNm7Mini && sNm7Active != null && !sNm7Active.isDestroyed()
                && !sNm7Active.isFinishing() && sNm7Active.mPlayer != null;
    }

    public static void attachNm7MiniPlayer(android.view.TextureView surface) {
        PlaybackActivity active = sNm7Active;
        if (!isNm7MiniPlayerActive()) return;
        active.mPlayerView.setPlayer(null);
        active.mPlayer.setVideoTextureView(surface);
        // Surface attachment must not change the user's pause state.
    }

    public static void restoreNm7Player() {
        PlaybackActivity active = sNm7Active;
        if (active == null || active.isDestroyed() || active.isFinishing()) return;
        if (active.mPlayer != null) {
            active.mPlayer.setVideoTextureView(null);
            active.mPlayerView.setPlayer(active.mPlayer);
        }
        sNm7Mini = false;
        active.startActivity(new Intent(active, active.getClass())
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_USER_ACTION));
    }

    public static void stopForNm7Iptv() {
        PlaybackActivity active = sNm7Active;
        sNm7Mini = false;
        if (active == null || active.isDestroyed()) return;
        active.mNm7Stopped = true;
        active.blockEngine(false);
        active.nm7SetBackground(false);
        active.stopService(new Intent().setClassName(active, "vn.phuong.iptvplayer.BackgroundPlaybackService"));
        active.finishReally();
        sNm7Active = null;
    }

    private void nm7SetBackground(boolean enabled) {
        System.setProperty("nm7.youtube.background", enabled ? "1" : "0");
        try {
            Class.forName("vn.phuong.iptvplayer.SharedPlaybackSession")
                    .getMethod("setYoutubeBackground", android.content.Context.class, boolean.class)
                    .invoke(null, this, enabled);
        } catch (ReflectiveOperationException | RuntimeException error) {
            android.util.Log.e("NM7Playback", "Background session bridge failed", error);
        }
        if (enabled) {
            try {
                Intent service = new Intent().setClassName(this, "vn.phuong.iptvplayer.BackgroundPlaybackService");
                service.putExtra("youtube", true);
                service.putExtra("channel_name", "YouTube");
                if (VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
            } catch (RuntimeException error) {
                android.util.Log.e("NM7Playback", "Background service failed", error);
            }
        }
    }

    // API 33 types isolated from class loading on Android 6-12.
    private static final class Nm7BackApi {
        static void register(PlaybackActivity activity) {
            android.window.OnBackInvokedCallback callback = activity::onBackPressed;
            activity.mNm7BackCallback = callback;
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
        }
        static void unregister(PlaybackActivity activity) {
            if (activity.mNm7BackCallback != null) {
                activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(
                        (android.window.OnBackInvokedCallback) activity.mNm7BackCallback);
                activity.mNm7BackCallback = null;
            }
        }
    }
