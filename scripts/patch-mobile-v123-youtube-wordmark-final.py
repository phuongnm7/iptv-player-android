#!/usr/bin/env python3
"""NM7 IPTV Mobile 1.10.123 — restore the YouTube wordmark as real UI, not a screenshot.

The device image 223390.jpg proves the previous 150x33 PNG contains extra blank canvas:
the visible YouTube mark is rendered far smaller than the ImageView box. It is therefore
not an overlay problem anymore; it is a bad asset choice. Recreate the mark from a
vector play-button + native text so the geometry is deterministic.

Reference geometry from the supplied YouTube screenshot:
- left-aligned around 16–20dp;
- red play mark ~40x28dp;
- "YouTube" immediately to its right;
- header height ~48dp;
- icons aligned center-right;
- no white asset canvas/margins.

Nothing in the player/feed data path is changed.
"""
from pathlib import Path
import re
import shutil

ROOT=Path("third_party/SmartTube-droid")
LAYOUT=ROOT/"smarttubedroid/src/main/res/layout/browse_activity.xml"
ICON_SRC=Path("scripts/mobile-ui/res/drawable-nodpi/nm7_youtube_icon.xml")
ICON_DST=ROOT/"smarttubedroid/src/main/res/drawable-nodpi/nm7_youtube_icon.xml"

if not LAYOUT.is_file() or not ICON_SRC.is_file():
    raise SystemExit("v123: required UI sources missing")

shutil.copyfile(ICON_SRC, ICON_DST)
s=LAYOUT.read_text(encoding="utf-8")

# Final normal-flow header dimensions.
s=s.replace(
'''        android:id="@+id/nm7_youtube_header"
        android:layout_width="match_parent"
        android:layout_height="64dp"
        android:paddingStart="16dp"''',
'''        android:id="@+id/nm7_youtube_header"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:paddingStart="16dp"''',1)

# Replace the old ImageView wordmark with a real logo + native YouTube text.
old=re.compile(r'''            <ImageView
                android:id="@+id/nm7_youtube_wordmark"
                .*?
                android:contentDescription="YouTube"
                android:elevation="8dp"
                android:translationZ="8dp" />

            <Space''',re.S)
if not old.search(s):
    raise SystemExit("v123: old wordmark ImageView not found")

new='''            <LinearLayout
                android:id="@+id/nm7_youtube_wordmark"
                android:layout_width="wrap_content"
                android:layout_height="match_parent"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:background="@android:color/transparent">

                <ImageView
                    android:id="@+id/nm7_youtube_logo"
                    android:layout_width="40dp"
                    android:layout_height="28dp"
                    android:src="@drawable/nm7_youtube_icon"
                    android:scaleType="fitCenter"
                    android:contentDescription="YouTube" />

                <TextView
                    android:id="@+id/nm7_youtube_text"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="3dp"
                    android:text="YouTube"
                    android:textColor="#FF0F0F0F"
                    android:textSize="20sp"
                    android:fontFamily="sans-serif-medium"
                    android:includeFontPadding="false"
                    android:gravity="center_vertical" />
            </LinearLayout>

            <Space'''

s=old.sub(new,s,1)

# Match the reference header vertical density and remove any synthetic overlay ordering.
s=s.replace('android:elevation="4dp"', 'android:elevation="2dp"',1)
s=s.replace('android:translationZ="8dp"','',1)

LAYOUT.write_text(s,encoding="utf-8")
print("NM7 Mobile 1.10.123 native YouTube wordmark UI applied")
