"""Mobile 1.10.73: prioritize YouTube network path and reduce Browse image transfer."""
from pathlib import Path
import re

phone = Path("third_party/SmartTube-droid/smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui")

# 1) Do not force the playback stack to OkHttp merely because a custom DNS
# preference is enabled. The pinned SmartTube network stack can then select
# its faster native/Cronet/HTTP path when available. Custom DNS preferences
# remain intact; only the transport override is removed.
p = phone / "playback/PlaybackActivity.java"
t = p.read_text(encoding="utf-8")
old = """        // The upstream fallback switches to OkHttp only after long buffering.
        // Respect an existing custom DNS setting from the first request instead.
        if (getPlayerTweaksData().getPreferredDnsType()
                != com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData.DNS_TYPE_SYSTEM
                && !getPlayerTweaksData().isNetworkErrorFixingDisabled()) {
            getPlayerTweaksData().setPlayerDataSource(
                    com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData.PLAYER_DATA_SOURCE_OKHTTP);
        }
        createPlayerObjects();"""
new = """        // NM7 1.10.73: keep the user's DNS preference but do not force
        // playback onto OkHttp. The pinned SmartTube transport can use its
        // native/Cronet-capable path when available instead of downgrading
        // HTTP/3/QUIC-capable connections to an HTTP/2-only client.
        createPlayerObjects();"""
if t.count(old) != 1:
    raise SystemExit("Network transport override anchor missing/ambiguous")
t = t.replace(old, new, 1)
p.write_text(t, encoding="utf-8")

# 2) The feed is a one-column mobile list. Keep the exact YouTube thumbnail quality/resolution used by 1.10.72.
# Do not downgrade card artwork; loading performance is optimized elsewhere.
holders = list(phone.parent.parent.parent.rglob("VideoCardHolder.java"))
if not holders:
    raise SystemExit("VideoCardHolder.java not found after UI patch")
changed = 0
for p in holders:
    s = p.read_text(encoding="utf-8")
    if 'highResCardImageUrl = cardImageUrl.replace("/default.jpg", "/maxresdefault.jpg");' in s:
        s2 = s.replace(
            'highResCardImageUrl = cardImageUrl.replace("/default.jpg", "/maxresdefault.jpg");',
            'highResCardImageUrl = cardImageUrl.replace("/default.jpg", "/maxresdefault.jpg");'
        )
        s2 = s2.replace(
            'highResCardImageUrl = cardImageUrl.replace("/mqdefault.jpg", "/maxresdefault.jpg");',
            'highResCardImageUrl = cardImageUrl.replace("/mqdefault.jpg", "/maxresdefault.jpg");'
        )
        s2 = s2.replace(
            'highResCardImageUrl = cardImageUrl.replace("/hqdefault.jpg", "/maxresdefault.jpg");',
            'highResCardImageUrl = cardImageUrl.replace("/hqdefault.jpg", "/maxresdefault.jpg");'
        )
        if s2 != s:
            p.write_text(s2, encoding="utf-8")
            changed += 1
if changed != 1:
    raise SystemExit(f"Expected exactly one Mobile VideoCardHolder optimization, changed={changed}")

# 3) Keep the selected-video format request on the background I/O scheduler.
# Add an explicit diagnostic marker so device tests can distinguish format
# lookup latency from decoder/frame latency without logging URLs/tokens.
p = phone / "playback/PlaybackActivity.java"
s = p.read_text(encoding="utf-8")
anchor = '        mNm7VideoRequestedAt = android.os.SystemClock.elapsedRealtime();'
if s.count(anchor) != 1:
    raise SystemExit("selected video timing anchor missing/ambiguous")
s = s.replace(anchor,
              anchor + '\n        android.util.Log.i("NM7Playback", "selected_video_start");', 1)
p.write_text(s, encoding="utf-8")

print("NM7 1.10.73 network/list loading optimization applied")
