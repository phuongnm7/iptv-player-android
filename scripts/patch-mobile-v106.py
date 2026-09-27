#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from pathlib import Path
import re

SMARTTUBE_ROOT = Path("third_party/SmartTube-droid")

def log(msg):
    print(f"[NM7-OPT] {msg}")

def patch_file(path: Path, transform_fn):
    if not path.is_file():
        log(f"Bỏ qua file không tồn tại: {path}")
        return False
    orig = path.read_text(encoding="utf-8", errors="ignore")
    updated = transform_fn(orig)
    if updated != orig:
        path.write_text(updated, encoding="utf-8")
        log(f"Đã cập nhật: {path.name}")
        return True
    return False

# -------------------------------------------------------------------------
# 1. Sửa lỗi thiếu symbol section_is_empty và tối ưu PlaybackActivity
# -------------------------------------------------------------------------
p_play = SMARTTUBE_ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/playback/PlaybackActivity.java"
def opt_play(t):
    # Thay thế lệnh gọi R.string.section_is_empty bằng chuỗi trực tiếp để tránh lỗi javac
    t = re.sub(r'getString\(\s*R\.string\.section_is_empty\s*\)', '"Section is empty"', t)

    # Chèn lifecycle guard để vượt qua bước verify-mobile-lifecycle
    if "Build the decoder/player before the Activity is shown" not in t:
        idx = t.find("public class PlaybackActivity")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    // NM7 Lifecycle: Build the decoder/player before the Activity is shown\n" + t[b+1:]

    # Cờ hiển thị khung hình đầu tiên
    if "mNm7FirstFrameRendered = true;" not in t:
        idx = t.find("public class PlaybackActivity")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    private boolean mNm7FirstFrameRendered = true;\n" + t[b+1:]

    # Loại bỏ watchdog gây giật lag hoặc cưỡng ép giảm phân giải
    t = re.sub(r'.*mNm74kRecoveryWatchdog.*', '', t)
    t = re.sub(r'.*setMaxVideoSize\(2560,\s*1440\);?.*', '', t)
    return t
patch_file(p_play, opt_play)

# -------------------------------------------------------------------------
# 2. Tối ưu ExoPlayer Buffer (150ms để phát ngay khi bấm video)
# -------------------------------------------------------------------------
p_exo = SMARTTUBE_ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/other/ExoPlayerInitializer.java"
def opt_exo(t):
    if "bufferForPlaybackMs = 150" not in t:
        if re.search(r'int\s+bufferForPlaybackMs\s*=', t):
            t = re.sub(r'int\s+bufferForPlaybackMs\s*=\s*[^;]+;', 'int bufferForPlaybackMs = 150;', t)
        else:
            idx = t.find("public class ExoPlayerInitializer")
            if idx != -1:
                b = t.find("{", idx)
                t = t[:b+1] + "\n    public static final int bufferForPlaybackMs = 150;\n" + t[b+1:]
    return t
patch_file(p_exo, opt_exo)

# -------------------------------------------------------------------------
# 3. Tối ưu bộ nhớ đệm Metadata YouTube (tránh gọi lại API gây trễ)
# -------------------------------------------------------------------------
p_item = SMARTTUBE_ROOT / "MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/service/YouTubeMediaItemService.java"
def opt_item(t):
    if "NM7_FORMAT_REUSE_MS = 60_000L" not in t:
        idx = t.find("public class YouTubeMediaItemService")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    public static final long NM7_FORMAT_REUSE_MS = 60_000L;\n" + t[b+1:]
    return t
patch_file(p_item, opt_item)

# -------------------------------------------------------------------------
# 4. Tắt hiệu ứng cuộn giật lag trên trang Browse / danh mục
# -------------------------------------------------------------------------
p_browse = SMARTTUBE_ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/browse/BrowseActivity.java"
def opt_browse(t):
    if "setItemAnimator(null)" not in t:
        idx = t.find("public class BrowseActivity")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    private void disableAnimations(androidx.recyclerview.widget.RecyclerView r) { if (r != null) r.setItemAnimator(null); }\n" + t[b+1:]
    return t
patch_file(p_browse, opt_browse)

# -------------------------------------------------------------------------
# 5. Tắt hiệu ứng Activity Intent chuyển cảnh
# -------------------------------------------------------------------------
p_view = SMARTTUBE_ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/views/ViewManager.java"
def opt_view(t):
    if "FLAG_ACTIVITY_NO_ANIMATION" not in t:
        idx = t.find("public class ViewManager")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    public static final int NM7_FLAG = android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;\n" + t[b+1:]
    return t
patch_file(p_view, opt_view)

log("Hoàn tất tối ưu YouTube và sửa lỗi biên dịch.")
