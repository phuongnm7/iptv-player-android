"""NM7 Mobile 1.10.93: robust YouTube startup handoff and buffer recovery.

Builds on v1.10.92 and the supplied device recording:
- keep the clicked-card poster opaque throughout decoder startup;
- require actual playback progression before removing the poster;
- restore a sane initial buffer threshold;
- leave IPTV/status bar/avatar/live chat/background playback/navigation unchanged.
"""
from pathlib import Path

ROOT = Path("third_party/SmartTube-droid")
PHONE = ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui"
PLAYBACK = PHONE / "playback/PlaybackActivity.java"
INIT = ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/other/ExoPlayerInitializer.java"

s = PLAYBACK.read_text(encoding="utf-8")

marker = "                boolean renderedNewFrame = rendered >= mNm7PosterRenderedBaseline + 2;"
if s.count(marker) != 1:
    raise SystemExit(f"v93: expected exactly one v92 render probe marker, found {s.count(marker)}")

start = s.index(marker)
end_marker = '                    return;\\n                }'
end = s.index(end_marker, start) + len(end_marker)
old_block = s[start:end]
if "nm7HideStartupPoster" not in old_block:
    raise SystemExit("v93: v92 render probe does not contain expected poster-hide decision")

new_block = """                boolean renderedNewFrame = rendered >= mNm7PosterRenderedBaseline + 2;
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
s = s[:start] + new_block + s[end:]

old_timeout = """                if (readyAndPlaying && elapsed >= 1800L) {
                    nm7HideStartupPoster("poster_timeout");
                    return;
                }"""
new_timeout = """                if (elapsed >= 1800L) {
                    nm7HideStartupPoster("poster_timeout");
                    return;
                }"""
if old_timeout not in s:
    raise SystemExit("v93: stale readyAndPlaying timeout anchor not found")
s = s.replace(old_timeout, new_timeout, 1)

PLAYBACK.write_text(s, encoding="utf-8")

e = INIT.read_text(encoding="utf-8")
old_buffer = "int bufferForPlaybackMs = 200; // NM7 1.10.92: faster initial start; rebuffer reserve unchanged."
new_buffer = "int bufferForPlaybackMs = 500; // NM7 1.10.93: restore startup resilience; avoid premature playback stalls."
if old_buffer in e:
    if e.count(old_buffer) != 1:
        raise SystemExit(f"v93: expected exactly one v92 startup buffer, found {e.count(old_buffer)}")
    e = e.replace(old_buffer, new_buffer, 1)
elif "int bufferForPlaybackMs = 500; // NM7 1.10.93:" not in e:
    raise SystemExit("v93: v92 startup buffer anchor not found")
INIT.write_text(e, encoding="utf-8")

print("NM7 Mobile 1.10.93 robust startup handoff patch applied")
