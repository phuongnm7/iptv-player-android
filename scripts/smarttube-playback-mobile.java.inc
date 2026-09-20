    private static PlaybackActivity sNm7Active;
    private static boolean sNm7Mini;
    private static boolean sNm7RestorePending;
    private static boolean sNm7ResumeAfterIptv;
    private boolean mNm7Stopped;
    private boolean mNm7LeavingForMini;
    private boolean mNm7TargetRestoreWaiting;
    private boolean mNm7ResumeAfterTarget;
    private com.google.android.exoplayer2.video.VideoListener mNm7FirstFrameListener;
    private Runnable mNm7TargetRestoreTimeout;
    private long mNm7TargetRestoreStarted;
    private com.google.android.exoplayer2.ui.PlayerView mNm7VideoTarget;
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

        active.blockEngine(false);
        com.google.android.exoplayer2.ui.PlayerView mini =
                (com.google.android.exoplayer2.ui.PlayerView) nextSurface;
        mini.setUseController(false);
        // Track the target in the player owner, not in a potentially stale overlay host.
        com.google.android.exoplayer2.ui.PlayerView oldTarget = active.mNm7VideoTarget;
        if (oldTarget == null) oldTarget = active.mPlayerView;
        com.google.android.exoplayer2.ui.PlayerView.switchTargetView(active.mPlayer, oldTarget, mini);
        active.mNm7VideoTarget = mini;
        android.util.Log.i("NM7Playback", "target=mini state=" + active.mPlayer.getPlaybackState());

        if (sNm7ResumeAfterIptv) {
            sNm7ResumeAfterIptv = false;
            active.mPlayer.setPlayWhenReady(true);
        }
    }

    public static void restoreNm7Player() {
        PlaybackActivity active = sNm7Active;
        if (active == null || active.isDestroyed() || active.isFinishing() || active.mPlayer == null) return;

        // Do not switch the video target while PlaybackActivity is still stopped.
        // Bring it to foreground first; onResume completes the surface handoff.
        if (sNm7RestorePending) return;
        active.beginNm7TargetRestore();
        sNm7RestorePending = true;
        active.startActivity(new Intent(active, active.getClass())
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION
                        | Intent.FLAG_ACTIVITY_NO_USER_ACTION));
    }

    private void completeNm7RestoreOnResume() {
        if (mPlayer == null) return;
        com.google.android.exoplayer2.ui.PlayerView oldTarget = mNm7VideoTarget;
        if (oldTarget == null) {
            try {
                android.view.View view = (android.view.View) Class.forName("vn.phuong.iptvplayer.MobileMiniPlayer")
                        .getMethod("surfaceView").invoke(null);
                if (view instanceof com.google.android.exoplayer2.ui.PlayerView)
                    oldTarget = (com.google.android.exoplayer2.ui.PlayerView) view;
            } catch (ReflectiveOperationException error) {
                android.util.Log.e("NM7Playback", "Read mini target", error);
            }
        }
        if (oldTarget != mPlayerView && !mNm7TargetRestoreWaiting) beginNm7TargetRestore();
        if (mNm7TargetRestoreWaiting) armNm7FirstFrameResume();
        if (oldTarget != mPlayerView) {
            com.google.android.exoplayer2.ui.PlayerView.switchTargetView(mPlayer, oldTarget, mPlayerView);
        } else if (mPlayerView.getPlayer() != mPlayer) {
            mPlayerView.setPlayer(mPlayer);
        }
        mNm7VideoTarget = mPlayerView;
        sNm7RestorePending = false;
        sNm7Mini = false;
        if (sNm7ResumeAfterIptv) {
            sNm7ResumeAfterIptv = false;
            if (mNm7TargetRestoreWaiting) mNm7ResumeAfterTarget = true;
            else mPlayer.setPlayWhenReady(true);
        }
        blockEngine(false);
        try {
            Class.forName("vn.phuong.iptvplayer.MobileMiniPlayer").getMethod("remove").invoke(null);
        } catch (ReflectiveOperationException error) {
            android.util.Log.e("NM7Playback", "Remove mini after foreground handoff", error);
        }
        android.util.Log.i("NM7Playback", "target=fullscreen; mini=false; back enabled");
    }

    private void beginNm7TargetRestore() {
        if (mPlayer == null || mNm7TargetRestoreWaiting) return;
        mNm7TargetRestoreWaiting = true;
        mNm7ResumeAfterTarget = mPlayer.getPlayWhenReady() || sNm7ResumeAfterIptv;
        mNm7TargetRestoreStarted = android.os.SystemClock.elapsedRealtime();
        // Pause the shared media clock, not just audio, during the window/surface handoff.
        // A paused ExoPlayer renders its first frame on a new surface before playback resumes.
        if (mNm7ResumeAfterTarget) mPlayer.setPlayWhenReady(false);
    }

    private void armNm7FirstFrameResume() {
        if (mPlayer == null || !mNm7TargetRestoreWaiting || mNm7FirstFrameListener != null) return;
        mNm7FirstFrameListener = new com.google.android.exoplayer2.video.VideoListener() {
            @Override public void onRenderedFirstFrame() {
                if (mNm7VideoTarget != mPlayerView || sNm7Mini || mNm7Stopped) return;
                android.util.Log.i("NM7Playback", "restore_first_frame_ms="
                        + (android.os.SystemClock.elapsedRealtime() - mNm7TargetRestoreStarted));
                cancelNm7TargetRestore(true);
            }
        };
        mPlayer.addVideoListener(mNm7FirstFrameListener);
        mNm7TargetRestoreTimeout = () -> {
            android.util.Log.w("NM7Playback", "restore first-frame timeout; releasing transition gate");
            cancelNm7TargetRestore(!mNm7Stopped && !sNm7Mini && !isFinishing());
        };
        // Never leave an audio-only stream or a decoder with no frame callback paused forever.
        mHandler.postDelayed(mNm7TargetRestoreTimeout, 750);
    }

    private void cancelNm7TargetRestore(boolean resume) {
        if (mNm7TargetRestoreTimeout != null) mHandler.removeCallbacks(mNm7TargetRestoreTimeout);
        if (mNm7FirstFrameListener != null && mPlayer != null) mPlayer.removeVideoListener(mNm7FirstFrameListener);
        boolean play = mNm7TargetRestoreWaiting && mNm7ResumeAfterTarget;
        mNm7TargetRestoreTimeout = null;
        mNm7FirstFrameListener = null;
        mNm7TargetRestoreWaiting = false;
        mNm7ResumeAfterTarget = false;
        if (resume && play && mPlayer != null && !mNm7Stopped) mPlayer.setPlayWhenReady(true);
    }

    public static boolean consumeNm7BrowseBack() {
        if (!isNm7MiniPlayerActive()) return false;
        sNm7RestorePending = false;
        try {
            Class.forName("vn.phuong.iptvplayer.MobileMiniPlayer").getMethod("remove").invoke(null);
        } catch (ReflectiveOperationException error) {
            android.util.Log.e("NM7Playback", "Remove mini from Browse Back", error);
        }
        stopForNm7Iptv();
        return true;
    }

    public static void suspendForNm7Iptv() {
        PlaybackActivity active = sNm7Active;
        if (active == null || active.isDestroyed() || active.isFinishing() || active.mPlayer == null) return;

        // IPTV may own playback now, but keep the YouTube session and exact video/position alive.
        sNm7Mini = true;
        sNm7ResumeAfterIptv = active.mPlayer.getPlayWhenReady() || active.mNm7ResumeAfterTarget;
        active.cancelNm7TargetRestore(false);
        active.mNm7Stopped = false;
        active.mNm7OwnsPlayback = false;
        active.blockEngine(true);
        active.mPlayer.setPlayWhenReady(false);
        if (active.mNm7VideoTarget != null) active.mNm7VideoTarget.setPlayer(null);
        active.mNm7VideoTarget = null;
        active.mPlayerView.setPlayer(null);
        active.nm7SetBackground(false);
        active.stopService(new Intent().setClassName(active,
                "vn.phuong.iptvplayer.BackgroundPlaybackService"));
    }

    public static void stopForNm7Iptv() {
        PlaybackActivity active = sNm7Active;
        sNm7Mini = false;
        sNm7RestorePending = false;
        sNm7ResumeAfterIptv = false;
        if (active == null || active.isDestroyed()) return;
        active.mNm7Stopped = true;
        active.cancelNm7TargetRestore(false);
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

