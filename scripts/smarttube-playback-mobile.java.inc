    private static PlaybackActivity sNm7Active;
    private static boolean sNm7Mini;
    private static boolean sNm7ResumeAfterIptv;
    private boolean mNm7Stopped;
    private boolean mNm7OwnsPlayback;
    private Object mNm7BackCallback;

    public static boolean isNm7SessionActive() {
        return sNm7Active != null && !sNm7Active.mNm7Stopped && !sNm7Active.isDestroyed()
                && !sNm7Active.isFinishing() && sNm7Active.mPlayer != null;
    }

    public static boolean isNm7MiniPlayerActive() {
        return sNm7Mini && sNm7Active != null && !sNm7Active.isDestroyed()
                && !sNm7Active.isFinishing() && sNm7Active.mPlayer != null;
    }

    public static void attachNm7MiniPlayer(android.view.View previousSurface, android.view.View nextSurface) {
        PlaybackActivity active = sNm7Active;
        if (!isNm7MiniPlayerActive()) return;
        if (!(nextSurface instanceof com.google.android.exoplayer2.ui.PlayerView)) return;

        if (previousSurface instanceof com.google.android.exoplayer2.ui.PlayerView) {
            ((com.google.android.exoplayer2.ui.PlayerView) previousSurface).setPlayer(null);
        }
        active.mPlayerView.setPlayer(null);
        com.google.android.exoplayer2.ui.PlayerView mini =
                (com.google.android.exoplayer2.ui.PlayerView) nextSurface;
        mini.setUseController(false);
        mini.setPlayer(active.mPlayer);

        // Mini-player is foreground playback inside BrowseActivity, not background playback.
        // Keep the decoder/renderers running while PlaybackActivity itself is paused.
        active.blockEngine(false);

        if (sNm7ResumeAfterIptv) {
            sNm7ResumeAfterIptv = false;
            active.blockEngine(false);
            active.mPlayer.setPlayWhenReady(true);
        }
    }

    public static void restoreNm7Player() {
        PlaybackActivity active = sNm7Active;
        if (active == null || active.isDestroyed() || active.isFinishing()) return;
        try {
            Class.forName("vn.phuong.iptvplayer.MobileMiniPlayer").getMethod("remove").invoke(null);
        } catch (ReflectiveOperationException error) {
            android.util.Log.e("NM7Playback", "Remove mini surface", error);
        }
        if (active.mPlayer != null) {
            active.mPlayerView.setPlayer(active.mPlayer);
        }
        sNm7Mini = false;
        active.startActivity(new Intent(active, active.getClass())
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_USER_ACTION));
    }

    public static void suspendForNm7Iptv() {
        PlaybackActivity active = sNm7Active;
        if (active == null || active.isDestroyed() || active.isFinishing() || active.mPlayer == null) return;

        // IPTV may own playback now, but keep the YouTube session and exact video/position alive.
        sNm7Mini = true;
        sNm7ResumeAfterIptv = active.mPlayer.getPlayWhenReady();
        active.mNm7Stopped = false;
        active.mNm7OwnsPlayback = false;
        active.blockEngine(true);
        active.mPlayer.setPlayWhenReady(false);
        active.mPlayerView.setPlayer(null);
        active.nm7SetBackground(false);
        active.stopService(new Intent().setClassName(active,
                "vn.phuong.iptvplayer.BackgroundPlaybackService"));
    }

    public static void stopForNm7Iptv() {
        PlaybackActivity active = sNm7Active;
        sNm7Mini = false;
        sNm7ResumeAfterIptv = false;
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
        if (enabled && mNm7OwnsPlayback) {
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
