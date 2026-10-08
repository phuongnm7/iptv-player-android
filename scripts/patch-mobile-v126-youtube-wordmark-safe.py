#!/usr/bin/env python3
"""NM7 Mobile 1.10.126 — YouTube logo/font only, using the accepted v1.10.123 layout.

Do not alter header position/height, camera/search positions, chip row, feed, status-bar
policy, or any other feature. The v1.10.123 layout geometry was accepted by the user.

The 1.10.125 failure was caused by replacing the accepted wordmark view/container with
a different view/id. v126 preserves the original accepted resource id:
    @id/nm7_youtube_wordmark
and only changes its visual child to an exact crop of the user's original YouTube logo.
"""
from pathlib import Path
import shutil

LAYOUT=Path("third_party/SmartTube-droid/smarttubedroid/src/main/res/layout/browse_activity.xml")
ASSET=Path("scripts/mobile-ui/res/drawable-nodpi/nm7_youtube_wordmark_exact.png")
DST_ASSET=Path("third_party/SmartTube-droid/smarttubedroid/src/main/res/drawable-nodpi/nm7_youtube_wordmark_exact.png")

if not LAYOUT.is_file() or not ASSET.is_file():
    raise SystemExit("v126: required layout/asset missing")

DST_ASSET.parent.mkdir(parents=True,exist_ok=True)
shutil.copyfile(ASSET,DST_ASSET)

s=LAYOUT.read_text(encoding="utf-8")

# v1.10.123 has a LinearLayout with id nm7_youtube_wordmark followed by the Space
# that leads to the camera/search controls. Replace only this logo area. The header
# itself, its padding and all controls remain byte-for-byte untouched.
start=s.find('''            <LinearLayout
                android:id="@+id/nm7_youtube_wordmark"''')
if start<0:
    raise SystemExit("v126: accepted v123 wordmark container not found")

end=s.find('            <Space',start)
if end<0:
    raise SystemExit("v126: wordmark end anchor not found")

new='''            <ImageView
                android:id="@+id/nm7_youtube_wordmark"
                android:layout_width="115dp"
                android:layout_height="30dp"
                android:src="@drawable/nm7_youtube_wordmark_exact"
                android:scaleType="fitCenter"
                android:adjustViewBounds="false"
                android:contentDescription="YouTube" />

'''
s=s[:start]+new+s[end:]
LAYOUT.write_text(s,encoding="utf-8")
print("NM7 Mobile 1.10.126: exact YouTube logo/font only; accepted view id preserved")
