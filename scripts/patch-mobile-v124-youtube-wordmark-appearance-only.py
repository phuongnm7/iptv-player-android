#!/usr/bin/env python3
"""1.10.124: appearance-only YouTube wordmark replacement.

Do not change header position, header height, left inset, icon positions or chip/feed
geometry. Keep the same 76dp x 30dp wordmark display box used by v123.

Only the rendered logo/text asset is replaced with a tight crop from the user-supplied
YouTube original screenshot 223392.jpg.
"""
from pathlib import Path
import shutil

SRC_ASSET = Path("scripts/mobile-ui/res/drawable-nodpi/nm7_youtube_wordmark_exact.png")
DST_ASSET = Path("third_party/SmartTube-droid/smarttubedroid/src/main/res/drawable-nodpi/nm7_youtube_wordmark_exact.png")
DST_LAYOUT = Path("third_party/SmartTube-droid/smarttubedroid/src/main/res/layout/browse_activity.xml")

if not SRC_ASSET.is_file() or not DST_LAYOUT.is_file():
    raise SystemExit("v124: required asset/layout missing")

DST_ASSET.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(SRC_ASSET, DST_ASSET)

s = DST_LAYOUT.read_text(encoding="utf-8")
start = s.find("""            <LinearLayout
                android:id="@+id/nm7_youtube_wordmark" """.rstrip())
# The exact v123 opening block is stable; fall back to whitespace-independent anchor.
if start < 0:
    start = s.find('            <LinearLayout\n                android:id="@+id/nm7_youtube_wordmark"')
if start < 0:
    raise SystemExit("v124: v123 wordmark container not found")

end = s.find("            <Space", start)
if end < 0:
    raise SystemExit("v124: wordmark end anchor not found")

new_block = """            <ImageView
                android:id="@+id/nm7_youtube_wordmark"
                android:layout_width="76dp"
                android:layout_height="30dp"
                android:src="@drawable/nm7_youtube_wordmark_exact"
                android:scaleType="fitStart"
                android:gravity="center_vertical|start"
                android:adjustViewBounds="false"
                android:contentDescription="YouTube" />

"""
s = s[:start] + new_block + s[end:]
DST_LAYOUT.write_text(s, encoding="utf-8")
print("NM7 1.10.124: YouTube logo/text asset replaced only; geometry unchanged")
