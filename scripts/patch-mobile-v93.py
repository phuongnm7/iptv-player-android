"""NM7 Mobile 1.10.93: robust YouTube startup handoff and buffer recovery."""
from pathlib import Path
import re

ROOT = Path("third_party/SmartTube-droid")
PHONE = ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui"
PLAYBACK = PHONE / "playback/PlaybackActivity.java"
INIT = ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/other/ExoPlayerInitializer.java"

s = PLAYBACK.read_text(encoding="utf-8")

pattern = re.compile(
    r'(?ms)^(\s*boolean renderedNewFrame = rendered >= mNm7PosterRenderedBaseline \+ 2;\s*)'
    r'if \(renderedNewFrame && elapsed >= 90L\s*'
    r'&& \(mNm7FirstFrameSeenAt == 0L\s*'
    r'\|\| android\.os\.SystemClock\.elapsedRealtime\(\) - mNm7FirstFrameSeenAt >= 60L\)\)\s*'
    r'\{\s*'
    r'nm7HideStartupPoster\("decoder_moving_frames_fast"\);\s*'
    r'return;\s*'
    r'\}'
)
replacement = """\\1long firstFrameAge = mNm7FirstFrameSeenAt == 0L
        ? 0L
        : android.os.SystemClock.elapsedRealtime() - mNm7FirstFrameSeenAt;
boolean actuallyPlaying = mPlayer.getPlaybackState() == com.google.android.exoplayer2.Player.STATE_READY
        && mPlayer.getPlayWhenReady()
        && mPlayer.isPlaying();
if (renderedNewFrame && elapsed >= 220L
        && mNm7FirstFrameSeenAt > 0L
        && firstFrameAge >= 140L
        && actuallyPlaying) {
    nm7HideStartupPoster("decoder_playing_stable");
    return;
}"""
s, count = pattern.subn(replacement, s, count=1)
if count != 1:
    raise SystemExit(f"v93: expected exactly one v92 poster decision, found {count}")

old_timeout = "if (elapsed >= 1200L)"
new_timeout = "if (elapsed >= 1800L)"
if s.count(old_timeout) == 1:
    s = s.replace(old_timeout, new_timeout, 1)
elif "if (elapsed >= 1800L)" not in s:
    raise SystemExit("v93: poster timeout anchor not found")

PLAYBACK.write_text(s, encoding="utf-8")

e = INIT.read_text(encoding="utf-8")
old_buffer = "int bufferForPlaybackMs = 200; // NM7 1.10.92: faster initial start; rebuffer reserve unchanged."
new_buffer = "int bufferForPlaybackMs = 500; // NM7 1.10.93: restore startup resilience; avoid premature playback stalls."
if e.count(old_buffer) == 1:
    e = e.replace(old_buffer, new_buffer, 1)
elif "int bufferForPlaybackMs = 500; // NM7 1.10.93:" not in e:
    raise SystemExit("v93: startup buffer anchor not found")
INIT.write_text(e, encoding="utf-8")

print("NM7 Mobile 1.10.93 robust startup handoff patch applied")
