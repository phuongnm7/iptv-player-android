"""NM7 Mobile 1.10.106 — playback persistence + measured 4K recovery.

Targets:
- Do not tear down/pause the YouTube playback owner merely because the Activity
  goes to background. Keep the same ExoPlayer, MediaItem, position and buffer.
- Do not run presenter pause/dispose on a normal HOME/background transition.
- Make repeated reopen of the same video reuse the in-memory resolved format long
  enough to avoid another YouTube format-resolution round trip.
- Measure 4K decoder drops. Prefer 4K30 when the selected 4K stream is high-FPS;
  if the device still cannot sustain 4K, fall back to 1440p without restarting
  the player or seeking. Normal 720p/1080p playback is untouched.
- Preserve avatar, portrait status-bar and spinner behavior.
"""
from pathlib import Path
import re

ROOT = Path("third_party/SmartTube-droid")
PHONE = ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui"
PLAY = PHONE / "playback/PlaybackActivity.java"

# 1) Keep the player alive during normal HOME/background transitions.
s = PLAY.read_text(encoding="utf-8")
s = s.replace(
'''    private boolean mNm7OwnsPlayback;
''',
'''    private boolean mNm7OwnsPlayback;
    private boolean mNm7Backgrounding;
    private boolean mNm74kPolicyApplied;
    private boolean mNm74kFallbackApplied;
    private Runnable mNm74kRecoveryWatchdog;
''', 1)

s = s.replace(
'''        mIsBackPressed = false;
        nm7SetBackground(false);
        if (mPlayer == null) initializePlayer();
        blockEngine(false);
        // Every foreground entry owns the full target, including opening another video
''',
'''        mIsBackPressed = false;
        mNm7Backgrounding = false;
        nm7SetBackground(false);
        if (mPlayer == null) initializePlayer();
        blockEngine(false);
        // A normal foreground return must reuse the existing player/media item.
        // Only a real mini-player/target transition is allowed to run the surface restore.
''', 1)

s = s.replace(
'''        completeNm7RestoreOnResume();
        mPlaybackPresenter.onViewResumed();
''',
'''        if (sNm7Mini || sNm7RestorePending || mNm7TargetRestoreWaiting) {
            completeNm7RestoreOnResume();
        } else {
            // Ordinary HOME -> foreground: keep the existing PlayerView binding and
            // presenter state. Re-running the presenter pause/resume pair can dispose
            // the selected-video request and force another format lookup.
            if (mPlayerView.getPlayer() != mPlayer) {
                bindNm7PlayerTarget();
            }
        }
        mPlaybackPresenter.onViewResumed();
''', 1)

old_pause='''    protected void onPause() {
        cancelNm7TargetRestore(true);
        // When Back enters NM7 mini, playback remains foreground in BrowseActivity.
        // Do not block the decoder/renderers or notify presenter of a full playback pause.
        if (sNm7Mini && !mNm7Stopped && !isFinishing() && mPlayer != null) {
            blockEngine(false);
            nm7SetBackground(false);
        } else if (!mNm7Stopped && !isFinishing() && mPlayer != null) {
            blockEngine(true);
            if (mPlayer.getPlayWhenReady()) nm7SetBackground(true);
        }
        super.onPause();
        if (!sNm7Mini) mPlaybackPresenter.onViewPaused();
        showHideWidgets(false);
    }'''
new_pause='''    protected void onPause() {
        cancelNm7TargetRestore(true);
        // HOME/lock/background is NOT a player lifecycle boundary for NM7 Mobile.
        // Keep ExoPlayer attached to the same MediaItem so the existing buffer,
        // decoder state and current position survive the return to the app.
        if (!mNm7Stopped && !isFinishing() && mPlayer != null) {
            if (sNm7Mini) {
                blockEngine(false);
                nm7SetBackground(false);
            } else {
                mNm7Backgrounding = true;
                blockEngine(false);
                if (mPlayer.getPlayWhenReady()) nm7SetBackground(true);
            }
        }
        super.onPause();
        // Do not call onViewPaused during a normal background transition: that path
        // can dispose/rebind selected-video loading and cause a second network load.
        if (!sNm7Mini && !mNm7Backgrounding) mPlaybackPresenter.onViewPaused();
        showHideWidgets(false);
    }'''
if old_pause not in s: raise SystemExit("v106: onPause anchor missing")
s=s.replace(old_pause,new_pause,1)

old_leave='''    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!mNm7Stopped && !isFinishing() && mPlayer != null) {
            blockEngine(true);
            if (mPlayer.getPlayWhenReady()) nm7SetBackground(true);
        }
    }'''
new_leave='''    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!mNm7Stopped && !isFinishing() && mPlayer != null) {
            mNm7Backgrounding = true;
            // Never block the playback engine for HOME. Android may keep the same
            // Activity/player alive, so the decoder and buffered media must remain hot.
            blockEngine(false);
            if (mPlayer.getPlayWhenReady()) nm7SetBackground(true);
        }
    }'''
if old_leave not in s: raise SystemExit("v106: onUserLeaveHint anchor missing")
s=s.replace(old_leave,new_leave,1)

# reset 4K policy when a new media item/player is created
s=s.replace('''        mNm7FirstFrameRendered = false;
''','''        mNm7FirstFrameRendered = false;
        mNm74kPolicyApplied = false;
        mNm74kFallbackApplied = false;
''',1)

# 2) Add measured 4K policy after the existing observer setup.
anchor='''    private void ensureNm7PlaybackObserver() {
'''
helper=r'''    private void applyNm74kPolicyIfNeeded() {
        if (mPlayer == null || mNm7TrackSelector == null || mNm74kPolicyApplied) return;
        try {
            com.google.android.exoplayer2.Format f = mPlayer.getVideoFormat();
            if (f == null || (f.width < 3840 && f.height < 2160)) return;
            float fps = f.frameRate;
            if (fps >= 50f) {
                // Keep 4K resolution but avoid asking a phone decoder to sustain 4K60.
                mNm7TrackSelector.setParameters(
                        mNm7TrackSelector.buildUponParameters()
                                .setMaxVideoFrameRate(30)
                                .setExceedVideoConstraintsIfNecessary(false));
                android.util.Log.i("NM7Playback",
                        "4k_policy=prefer_4k30 width=" + f.width + " height=" + f.height
                                + " fps=" + fps + " mime=" + f.sampleMimeType);
            } else {
                android.util.Log.i("NM7Playback",
                        "4k_policy=native width=" + f.width + " height=" + f.height
                                + " fps=" + fps + " mime=" + f.sampleMimeType);
            }
            mNm74kPolicyApplied = true;
        } catch (RuntimeException error) {
            android.util.Log.w("NM7Playback", "4k policy failed", error);
        }
    }

    private void armNm74kRecoveryWatchdog() {
        if (mPlayer == null || mNm7Stopped || mNm74kFallbackApplied) return;
        if (mNm74kRecoveryWatchdog != null) mHandler.removeCallbacks(mNm74kRecoveryWatchdog);
        final com.google.android.exoplayer2.SimpleExoPlayer observed = mPlayer;
        final com.google.android.exoplayer2.DecoderCounters baseline = observed.getVideoDecoderCounters();
        final int baselineDropped = baseline != null ? baseline.droppedBufferCount : 0;
        mNm74kRecoveryWatchdog = () -> {
            if (mPlayer != observed || mNm7Stopped || mNm74kFallbackApplied) return;
            try {
                com.google.android.exoplayer2.Format f = observed.getVideoFormat();
                com.google.android.exoplayer2.DecoderCounters c = observed.getVideoDecoderCounters();
                if (f != null && c != null && (f.width >= 3840 || f.height >= 2160)) {
                    int dropped = Math.max(0, c.droppedBufferCount - baselineDropped);
                    if (dropped >= 15 && observed.getPlayWhenReady()) {
                        mNm74kFallbackApplied = true;
                        long position = Math.max(0L, observed.getCurrentPosition());
                        android.util.Log.w("NM7Playback",
                                "4k_recovery=1440p dropped=" + dropped
                                        + " position=" + position
                                        + " width=" + f.width + " height=" + f.height);
                        mNm7TrackSelector.setParameters(
                                mNm7TrackSelector.buildUponParameters()
                                        .setMaxVideoSize(2560, 1440)
                                        .setMaxVideoFrameRate(30)
                                        .setExceedVideoConstraintsIfNecessary(false));
                        // No seek/restart: ExoPlayer changes the selected representation
                        // while retaining the current timeline and playback position.
                    }
                }
            } catch (RuntimeException error) {
                android.util.Log.w("NM7Playback", "4k recovery watchdog failed", error);
            }
        };
        mHandler.postDelayed(mNm74kRecoveryWatchdog, 3000L);
    }

'''
if anchor not in s: raise SystemExit("v106: observer anchor missing")
s=s.replace(anchor,helper+anchor,1)

# Apply the 4K policy from the stable player-state observer. Match the method
# generically because the pinned SmartTube source may qualify Player constants.
method_match = re.search(r'public void onPlayerStateChanged\\s*\\([^)]*\\)\\s*\\{', s)
if not method_match:
    raise SystemExit("v106: onPlayerStateChanged observer missing")
method_end = method_match.end()
ready_match = re.search(r'if\\s*\\(\\s*playbackState\\s*==[^\\n\\{]+\\)\\s*\\{', s[method_end:])
if not ready_match:
    raise SystemExit("v106: STATE_READY branch missing")
insert_at = method_end + ready_match.end()
s = s[:insert_at] + '''
                    applyNm74kPolicyIfNeeded();
                    if (playWhenReady) armNm74kRecoveryWatchdog();''' + s[insert_at:]

PLAY.write_text(s,encoding='utf-8')

# 3) Increase only the short in-memory reuse window. Signed URLs remain memory-only.
p=ROOT/"common/src/main/java/com/liskovsoft/youtubeapi/service/YouTubeMediaItemService.java"
s=p.read_text(encoding='utf-8')
s=s.replace('private static final long NM7_FORMAT_REUSE_MS = 8_000L;', 'private static final long NM7_FORMAT_REUSE_MS = 60_000L;')
p.write_text(s,encoding='utf-8')

print("NM7 Mobile 1.10.106 playback persistence + measured 4K recovery applied")
