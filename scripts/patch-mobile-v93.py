"""NM7 Mobile 1.10.93: robust YouTube startup handoff and buffer recovery."""
from pathlib import Path

ROOT = Path("third_party/SmartTube-droid")
PHONE = ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui"
PLAYBACK = PHONE / "playback/PlaybackActivity.java"
INIT = ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/other/ExoPlayerInitializer.java"

s = PLAYBACK.read_text(encoding="utf-8")

old_hide = """                boolean renderedNewFrame = rendered >= mNm7PosterRenderedBaseline + 2;
                if (renderedNewFrame && elapsed >= 90L
                        && (mNm7FirstFrameSeenAt == 0L
                            || android.os.SystemClock.elapsedRealtime() - mNm7FirstFrameSeenAt >= 60L)) {
                    nm7HideStartupPoster("decoder_moving_frames_fast");
                    return;
                }"""
new_hide = """                boolean renderedNewFrame = rendered >= mNm7PosterRenderedBaseline + 2;
                long firstFrameAge = mNm7FirstFrameSeenAt == 0L
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
if s.count(old_hide) != 1:
    raise SystemExit(f"v93: expected exactly one v92 poster decision, found {s.count(old_hide)}")
s = s.replace(old_hide, new_hide, 1)

old_timeout = """                if (elapsed >= 1200L) {
                    nm7HideStartupPoster("poster_timeout");
                    return;
                }"""
new_timeout = """                if (elapsed >= 1800L) {
                    nm7HideStartupPoster("poster_timeout");
                    return;
                }"""
if s.count(old_timeout) == 1:
    s = s.replace(old_timeout, new_timeout, 1)
elif "elapsed >= 1800L" not in s:
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
