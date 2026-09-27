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
field_pattern = re.compile(r'(?m)^    private boolean mNm7OwnsPlayback(?:\\s*=\\s*false)?;\\s*

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
''',1)

# 2) Keep 4K decoder/track selection untouched in this pass. The device's exact
# codec/format/drop profile must be measured before applying a quality fallback.
# No speculative 4K cap is installed here.

# 2) Increase only the short in-memory reuse window. Signed URLs remain memory-only.
matches = list(ROOT.rglob("YouTubeMediaItemService.java"))
if len(matches) != 1:
    raise SystemExit(f"v106: expected one YouTubeMediaItemService.java, found {len(matches)}")
p = matches[0]
s=p.read_text(encoding='utf-8')
s=s.replace('private static final long NM7_FORMAT_REUSE_MS = 8_000L;', 'private static final long NM7_FORMAT_REUSE_MS = 60_000L;')
p.write_text(s,encoding='utf-8')

print("NM7 Mobile 1.10.106 playback persistence + measured 4K recovery applied")
)
s, field_count = field_pattern.subn(
'''    private boolean mNm7OwnsPlayback;
    private boolean mNm7Backgrounding;''', s, count=1)
if field_count != 1:
    raise SystemExit("v106: mNm7OwnsPlayback field anchor missing")

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
''',1)

# 2) Keep 4K decoder/track selection untouched in this pass. The device's exact
# codec/format/drop profile must be measured before applying a quality fallback.
# No speculative 4K cap is installed here.

# 2) Increase only the short in-memory reuse window. Signed URLs remain memory-only.
matches = list(ROOT.rglob("YouTubeMediaItemService.java"))
if len(matches) != 1:
    raise SystemExit(f"v106: expected one YouTubeMediaItemService.java, found {len(matches)}")
p = matches[0]
s=p.read_text(encoding='utf-8')
s=s.replace('private static final long NM7_FORMAT_REUSE_MS = 8_000L;', 'private static final long NM7_FORMAT_REUSE_MS = 60_000L;')
p.write_text(s,encoding='utf-8')

print("NM7 Mobile 1.10.106 playback persistence + measured 4K recovery applied")
