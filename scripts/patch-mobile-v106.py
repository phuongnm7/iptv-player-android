"""NM7 Mobile 1.10.106 — preserve YouTube playback across background and avoid repeated format resolution.

Scope:
- Keep the existing ExoPlayer/MediaItem/buffer alive across HOME/background.
- Do not run the SmartTube presenter pause/dispose path for a normal background transition.
- Rebind the existing PlayerView only when an actual mini/target restore is pending.
- Extend the process-local format-info reuse window for rapid reopen.
- Do not change avatar, status bar, spinner, IPTV navigation, or 4K track selection.
"""
from pathlib import Path

ROOT = Path("third_party/SmartTube-droid")
PHONE = ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui"
PLAY = PHONE / "playback/PlaybackActivity.java"

s = PLAY.read_text(encoding="utf-8")

# Background state marker. Fail closed if the stable lifecycle field moved.
if "mNm7Backgrounding" not in s:
    anchor = "    private boolean mNm7Stopped;"
    if s.count(anchor) != 1:
        raise SystemExit("v106: mNm7Stopped field anchor missing")
    s = s.replace(anchor, anchor + "\n    private boolean mNm7Backgrounding;", 1)

# Foreground: ordinary HOME return must reuse the existing player and target.
old = """        mIsBackPressed = false;
        nm7SetBackground(false);
        if (mPlayer == null) initializePlayer();
        blockEngine(false);
        // Every foreground entry owns the full target, including opening another video
        // while a mini session exists (not only a tap on the mini overlay).
        mNm7LeavingForMini = false;
        completeNm7RestoreOnResume();
        mPlaybackPresenter.onViewResumed();
"""
new = """        mIsBackPressed = false;
        mNm7Backgrounding = false;
        nm7SetBackground(false);
        if (mPlayer == null) initializePlayer();
        blockEngine(false);
        // Ordinary HOME -> foreground: keep the same player, MediaItem, position and
        // buffer. Only a real mini/target transition performs a surface restore.
        mNm7LeavingForMini = false;
        if (sNm7Mini || sNm7RestorePending || mNm7TargetRestoreWaiting) {
            completeNm7RestoreOnResume();
        } else if (mPlayerView.getPlayer() != mPlayer) {
            bindNm7PlayerTarget();
        }
        mPlaybackPresenter.onViewResumed();
"""
if old not in s:
    raise SystemExit("v106: onResume anchor missing")
s = s.replace(old, new, 1)

# HOME/lock: do not block the engine and do not dispose presenter state.
old = """    protected void onPause() {
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
    }"""
new = """    protected void onPause() {
        cancelNm7TargetRestore(true);
        // HOME/lock/background is not a YouTube player teardown boundary.
        // Keep ExoPlayer and its current MediaItem/buffer alive.
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
        // Presenter pause can dispose selected-video loading and force another
        // format-resolution request after HOME -> foreground.
        if (!sNm7Mini && !mNm7Backgrounding) mPlaybackPresenter.onViewPaused();
        showHideWidgets(false);
    }"""
if old not in s:
    raise SystemExit("v106: onPause anchor missing")
s = s.replace(old, new, 1)

old = """    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!mNm7Stopped && !isFinishing() && mPlayer != null) {
            blockEngine(true);
            if (mPlayer.getPlayWhenReady()) nm7SetBackground(true);
        }
    }"""
new = """    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!mNm7Stopped && !isFinishing() && mPlayer != null) {
            mNm7Backgrounding = true;
            // HOME must not block the decoder/renderers. Keep the current stream hot.
            blockEngine(false);
            if (mPlayer.getPlayWhenReady()) nm7SetBackground(true);
        }
    }"""
if old not in s:
    raise SystemExit("v106: onUserLeaveHint anchor missing")
s = s.replace(old, new, 1)

PLAY.write_text(s, encoding="utf-8")

# Reuse the existing fast format resolver briefly in RAM. This does not persist
# signed googlevideo URLs to disk.
matches = list(ROOT.rglob("YouTubeMediaItemService.java"))
if len(matches) != 1:
    raise SystemExit(f"v106: expected one YouTubeMediaItemService.java, found {len(matches)}")
p = matches[0]
s = p.read_text(encoding="utf-8")
old = "private static final long NM7_FORMAT_REUSE_MS = 8_000L;"
new = "private static final long NM7_FORMAT_REUSE_MS = 60_000L;"
if old not in s:
    raise SystemExit("v106: format reuse constant anchor missing")
s = s.replace(old, new, 1)
p.write_text(s, encoding="utf-8")

print("NM7 Mobile 1.10.106 playback persistence + format reuse applied")
