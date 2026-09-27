#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from pathlib import Path
import re

SMARTTUBE_ROOT = Path("third_party/SmartTube-droid")

def log(msg):
    print(f"[NM7-OPT] {msg}")

def patch_file(path: Path, transform_fn):
    if not path.is_file():
        log(f"Warning: Not found {path}")
        return False
    orig = path.read_text(encoding="utf-8", errors="ignore")
    updated = transform_fn(orig)
    if updated != orig:
        path.write_text(updated, encoding="utf-8")
        log(f"Patched {path.name}")
        return True
    return False

# 1. Tối ưu buffer load video cực nhanh (150ms)
p_exo = SMARTTUBE_ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/other/ExoPlayerInitializer.java"
def opt_exo(t):
    if "bufferForPlaybackMs = 150" not in t:
        t = re.sub(r'int\s+bufferForPlaybackMs\s*=\s*\d+;', 'int bufferForPlaybackMs = 150;', t)
        t = re.sub(r'int\s+bufferForPlaybackAfterRebufferMs\s*=\s*\d+;', 'int bufferForPlaybackAfterRebufferMs = 500;', t)
    return t
patch_file(p_exo, opt_exo)

# 2. Tối ưu tái sử dụng format YouTube metadata (60s cache)
p_item = SMARTTUBE_ROOT / "MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/service/YouTubeMediaItemService.java"
def opt_item(t):
    if "NM7_FORMAT_REUSE_MS = 60_000L" not in t:
        idx = t.find("public class YouTubeMediaItemService")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    public static final long NM7_FORMAT_REUSE_MS = 60_000L;\n" + t[b+1:]
    return t
patch_file(p_item, opt_item)

# 3. Chuyển trang/danh mục mượt mà, tắt animation gây drop frame
p_browse = SMARTTUBE_ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/browse/BrowseActivity.java"
def opt_browse(t):
    if "setItemAnimator(null)" not in t:
        t = re.sub(
            r'(super\.onCreate\([^)]*\);)',
            r'\1\n        if (findViewById(android.R.id.list) instanceof androidx.recyclerview.widget.RecyclerView) {\n            ((androidx.recyclerview.widget.RecyclerView) findViewById(android.R.id.list)).setItemAnimator(null);\n        }',
            t
        )
    return t
patch_file(p_browse, opt_browse)

# 4. Tắt hiệu ứng chuyển cảnh nặng của Activity
p_view = SMARTTUBE_ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/views/ViewManager.java"
def opt_view(t):
    if "FLAG_ACTIVITY_NO_ANIMATION" not in t:
        t = re.sub(
            r'(intent\.addFlags\([^)]+\);)',
            r'\1\n        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION);',
            t
        )
    return t
patch_file(p_view, opt_view)

# 5. Tối ưu Playback: Ẩn ProgressBar ngay khi có frame đầu và gỡ watchdog gây giật 4K
p_play = SMARTTUBE_ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/playback/PlaybackActivity.java"
def opt_play(t):
    if "mNm7FirstFrameRendered = true;" not in t:
        t = re.sub(
            r'(public\s+void\s+onRenderedFirstFrame\s*\([^)]*\)\s*\{)',
            r'\1\n        mNm7FirstFrameRendered = true;\n        if (mProgressBar != null) mProgressBar.setVisibility(View.GONE);',
            t
        )
    t = re.sub(r'.*mNm74kRecoveryWatchdog.*', '', t)
    t = re.sub(r'.*setMaxVideoSize\(2560,\s*1440\);?.*', '', t)
    return t
patch_file(p_play, opt_play)

log("Done applying optimizations.")
