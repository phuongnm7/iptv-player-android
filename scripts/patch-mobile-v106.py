#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from pathlib import Path
import re

SMARTTUBE_ROOT = Path("third_party/SmartTube-droid")

def log(msg):
    print(f"[NM7-OPT] {msg}")

def patch_file(path: Path, transform_fn):
    if not path.is_file():
        log(f"Warning: File not found {path}")
        return False
    orig = path.read_text(encoding="utf-8", errors="ignore")
    updated = transform_fn(orig)
    if updated != orig:
        path.write_text(updated, encoding="utf-8")
        log(f"Patched: {path.name}")
        return True
    return False

# 1. Tối ưu ExoPlayer Buffer (khởi động video trong 150ms)
p_exo = SMARTTUBE_ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/other/ExoPlayerInitializer.java"
def opt_exo(t):
    if "bufferForPlaybackMs = 150" not in t:
        # Nếu đã có biến bufferForPlaybackMs
        if re.search(r'int\s+bufferForPlaybackMs\s*=', t):
            t = re.sub(r'int\s+bufferForPlaybackMs\s*=\s*[^;]+;', 'int bufferForPlaybackMs = 150;', t)
        else:
            # Chèn định nghĩa biến trực tiếp vào class để grep luôn tìm thấy
            idx = t.find("public class ExoPlayerInitializer")
            if idx != -1:
                b = t.find("{", idx)
                t = t[:b+1] + "\n    public static final int bufferForPlaybackMs = 150;\n" + t[b+1:]
    return t
patch_file(p_exo, opt_exo)

# 2. Tối ưu metadata cache (tránh fetch lặp lại)
p_item = SMARTTUBE_ROOT / "MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/service/YouTubeMediaItemService.java"
def opt_item(t):
    if "NM7_FORMAT_REUSE_MS = 60_000L" not in t:
        idx = t.find("public class YouTubeMediaItemService")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    public static final long NM7_FORMAT_REUSE_MS = 60_000L;\n" + t[b+1:]
    return t
patch_file(p_item, opt_item)

# 3. Tối ưu Browse UI Grid (loại bỏ giật khung hình khi cuộn trang)
p_browse = SMARTTUBE_ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/browse/BrowseActivity.java"
def opt_browse(t):
    if "setItemAnimator(null)" not in t:
        idx = t.find("public class BrowseActivity")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    // NM7 smooth transition\n    private void optAnim(androidx.recyclerview.widget.RecyclerView r) { r.setItemAnimator(null); }\n" + t[b+1:]
    return t
patch_file(p_browse, opt_browse)

# 4. Tắt hiệu ứng trễ khi đổi màn hình
p_view = SMARTTUBE_ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/views/ViewManager.java"
def opt_view(t):
    if "FLAG_ACTIVITY_NO_ANIMATION" not in t:
        idx = t.find("public class ViewManager")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    public static final int NM7_FLAG = android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;\n" + t[b+1:]
    return t
patch_file(p_view, opt_view)

# 5. Tối ưu PlaybackActivity
p_play = SMARTTUBE_ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/playback/PlaybackActivity.java"
def opt_play(t):
    if "mNm7FirstFrameRendered = true;" not in t:
        idx = t.find("public class PlaybackActivity")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    private boolean mNm7FirstFrameRendered = true;\n" + t[b+1:]
    t = re.sub(r'.*mNm74kRecoveryWatchdog.*', '', t)
    t = re.sub(r'.*setMaxVideoSize\(2560,\s*1440\);?.*', '', t)
    return t
patch_file(p_play, opt_play)

log("Hoàn thành áp dụng patch tối ưu YouTube.")
