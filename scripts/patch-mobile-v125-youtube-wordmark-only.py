#!/usr/bin/env python3
"""NM7 IPTV Mobile 1.10.125 — logo/font ONLY.

Critical constraint from user:
- v1.10.123 geometry is accepted.
- Do NOT change header position, header height, logo display position,
  camera/search position, chip row, feed, status bar, or any other behavior.
- Only correct the visual YouTube logo and wordmark font.

The previous 1.10.124 failure happened because its workflow was based on a stale
layout source and replaced the whole final Browse layout. v125 deliberately starts
from the v1.10.123 workflow and runs one tiny final patch after v123.

The wordmark asset is a tight crop directly from the user's original YouTube
reference image 223392.jpg, so the exact YouTube glyphs are preserved without
substituting a different font.
"""
from pathlib import Path
import shutil

LAYOUT=Path("third_party/SmartTube-droid/smarttubedroid/src/main/res/layout/browse_activity.xml")
ASSET=Path("scripts/mobile-ui/res/drawable-nodpi/nm7_youtube_wordmark_exact.png")
DST=Path("third_party/SmartTube-droid/smarttubedroid/src/main/res/drawable-nodpi/nm7_youtube_wordmark_exact.png")

if not LAYOUT.is_file() or not ASSET.is_file():
    raise SystemExit("v125: required layout/asset missing")

DST.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(ASSET, DST)

s=LAYOUT.read_text(encoding="utf-8")

# Find the v1.10.123 wordmark container. Keep the container's width/height, so
# layout geometry remains exactly the same. Only replace its two visual children.
start=s.find("""            <LinearLayout
                android:id="@+id/nm7_youtube_wordmark"
""")
if start < 0:
    raise SystemExit("v125: accepted v123 wordmark container not found")

end=s.find("            <Space", start)
if end < 0:
    raise SystemExit("v125: wordmark end anchor not found")

prefix=s[start:end]
# Preserve the accepted outer container attributes from v123.
outer_end=prefix.find(">\n")
if outer_end < 0:
    raise SystemExit("v125: wordmark outer start malformed")
outer=prefix[:outer_end+2]

new_block=outer+"""                <ImageView
                    android:id="@+id/nm7_youtube_wordmark_exact"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:src="@drawable/nm7_youtube_wordmark_exact"
                    android:scaleType="center"
                    android:adjustViewBounds="false"
                    android:contentDescription="YouTube" />

            </LinearLayout>

"""
s=s[:start]+new_block+s[end:]

LAYOUT.write_text(s,encoding="utf-8")
print("NM7 Mobile 1.10.125: YouTube logo/font appearance corrected; geometry untouched")
