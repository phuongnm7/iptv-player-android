"""NM7 Mobile 1.10.93: robust YouTube startup handoff and buffer recovery.

Based on 1.10.92 and the supplied device recording:
- keep the clicked-card poster opaque throughout decoder startup;
- require actual playback progression, not only rendered-buffer count, before removing poster;
- restore a sane initial buffer threshold; 200ms was too aggressive and can amplify startup stalls;
- never change IPTV, status bar, avatar, live chat, background playback, or navigation.
"""
from pathlib import Path

ROOT = Path("third_party/SmartTube-droid")
PHONE = ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui"
PLAYBACK = PHONE / "playback/PlaybackActivity.java"
INIT = ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/other/ExoPlayerInitializer.java"

def once(s, old, new, label):
    if s.count(old) != 1:
        raise SystemExit(f"v93: expected exactly one {label}, found {s.count(old)}")
    return s.replace(old, new, 1)

s = PLAYBACK.read_text(encoding="utf-8")

# Do not treat decoder output-buffer count alone as proof that the video is moving.
# A repeated/held first frame can still trigger onRenderedFirstFrame and a render
# counter while the player is not actually advancing.
old_probe = """                boolean renderedNewFrame = rendered >= mNm7PosterRenderedBaseline + 2;
                long elapsed = android.os.SystemClock.elapsedRealtime() - mNm7PosterProbeStartedAt;
                if (renderedNewFrame && elapsed >= 90L
                        && (mNm7FirstFrameSeenAt == 0L
                            || android.os.SystemClock.elapsedRealtime() - mNm7FirstFrameSeenAt >= 60L)) {
                    nm7HideStartupPoster("decoder_moving_frames_fast");
                    return;
                }
"""
new_probe = """                boolean renderedNewFrame = rendered >= mNm7PosterRenderedBaseline + 2;
                long elapsed = android.os.SystemClock.elapsedRealtime() - mNm7PosterProbeStartedAt;
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
                }
"""
s = once(s, old_probe, new_probe, "v92 render probe (threshold +2)")

# Keep the poster during a short startup window if the decoder is ready but has not
# yet entered isPlaying(). This prevents a blank/surface flash during renderer attach.
old_fallback = """                if (elapsed >= 1200L) {
                    nm7HideStartupPoster("poster_timeout");
                    return;
                }
"""
new_fallback = """                if (elapsed >= 1800L) {
                    nm7HideStartupPoster("poster_timeout");
                    return;
                }
"""
if old_fallback in s:
    s = once(s, old_fallback, new_fallback, "poster timeout")

PLAYBACK.write_text(s, encoding="utf-8")

e = INIT.read_text(encoding="utf-8")
old_buffer = "int bufferForPlaybackMs = 200; // NM7 1.10.92: faster initial start; rebuffer reserve unchanged."
new_buffer = "int bufferForPlaybackMs = 500; // NM7 1.10.93: restore startup resilience; avoid premature playback stalls."
e = once(e, old_buffer, new_buffer, "v92 startup buffer")
INIT.write_text(e, encoding="utf-8")

print("NM7 Mobile 1.10.93 robust startup handoff patch applied")
