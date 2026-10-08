#!/usr/bin/env python3
"""NM7 Mobile 1.10.128 — YouTube logo/font appearance ONLY.

User acceptance boundary:
- 1.10.127 has the correct position and size.
- Change NOTHING about header geometry, status bar, camera/search, chips, feed,
  playback, IPTV, or any other feature.
- Only replace the drawable used by the existing @id/nm7_youtube_wordmark.

The exact logo/text crop is taken from the supplied original YouTube screenshot
223414.jpg. This preserves the original YouTube glyph shape instead of substituting
an Android font.
"""
from pathlib import Path
import shutil

SRC_ASSET=Path("scripts/mobile-ui/res/drawable-nodpi/nm7_youtube_wordmark_exact.png")
DST_ASSET=Path("third_party/SmartTube-droid/smarttubedroid/src/main/res/drawable-nodpi/nm7_youtube_wordmark_exact.png")
LAYOUT=Path("third_party/SmartTube-droid/smarttubedroid/src/main/res/layout/browse_activity.xml")

if not SRC_ASSET.is_file() or not DST_ASSET.parent.is_dir() or not LAYOUT.is_file():
    raise SystemExit("v128: required asset/layout missing")

shutil.copyfile(SRC_ASSET,DST_ASSET)

s=LAYOUT.read_text(encoding="utf-8")
old='android:src="@drawable/nm7_youtube_wordmark"'
new='android:src="@drawable/nm7_youtube_wordmark_exact"'
if old not in s:
    raise SystemExit("v128: expected v1.10.127 wordmark drawable reference not found")

# Appearance-only: change exactly one XML attribute. Width/height/position remain untouched.
s2=s.replace(old,new,1)
if s2==s:
    raise SystemExit("v128: no layout change made")
LAYOUT.write_text(s2,encoding="utf-8")

# Hard guard: fail if the accepted geometry changed in this patch.
assert 'android:layout_width="76dp"' in s2
assert 'android:layout_height="30dp"' in s2
assert 'android:paddingStart="16dp"' in s2
print("v1.10.128: exact YouTube logo/font asset substituted; geometry untouched")
