#!/usr/bin/env python3
"""NM7 Mobile 1.10.124 — logo/text appearance only.

IMPORTANT: do not change header position, header height, left inset, logo display
box, create/search icon positions, chip row, feed, or system-bar behavior.

Source of truth:
- 223392.jpg = original YouTube
- 223393.jpg = NM7 comparison

The existing 1.10.123 layout geometry is already accepted by the user. Only replace
the rendered YouTube wordmark with a tightly cropped raster taken from the original
YouTube screenshot and keep the same 76dp x 30dp display box.
"""
from pathlib import Path
import shutil

SRC_LAYOUT=Path("scripts/mobile-ui/res/layout/browse_activity.xml")
SRC_ASSET=Path("scripts/mobile-ui/res/drawable-nodpi/nm7_youtube_wordmark_exact.png")
DST_LAYOUT=Path("third_party/SmartTube-droid/smarttubedroid/src/main/res/layout/browse_activity.xml")
DST_ASSET=Path("third_party/SmartTube-droid/smarttubedroid/src/main/res/drawable-nodpi/nm7_youtube_wordmark_exact.png")

if not SRC_LAYOUT.is_file() or not SRC_ASSET.is_file() or not DST_LAYOUT.exists():
    raise SystemExit("v124: required assets/layout missing")

# Copy exact original YouTube wordmark crop.
DST_ASSET.parent.mkdir(parents=True,exist_ok=True)
shutil.copyfile(SRC_ASSET,DST_ASSET)

s=DST_LAYOUT.read_text(encoding="utf-8")

# Locate ONLY the current wordmark container and replace its internal renderer.
start=s.find('            <LinearLayout
                android:id="@+id/nm7_youtube_wordmark"')
if start<0:
    raise SystemExit("v124: current wordmark container missing")
end=s.find('            <Space',start)
if end<0:
    raise SystemExit("v124: wordmark Space anchor missing")

new='''            <ImageView
                android:id="@+id/nm7_youtube_wordmark"
                android:layout_width="76dp"
                android:layout_height="30dp"
                android:src="@drawable/nm7_youtube_wordmark_exact"
                android:scaleType="fitStart"
                android:gravity="center_vertical|start"
                android:adjustViewBounds="false"
                android:contentDescription="YouTube" />

'''
s=s[:start]+new+s[end:]
DST_LAYOUT.write_text(s,encoding="utf-8")
print("NM7 Mobile 1.10.124 logo/text appearance-only correction applied")
