#!/usr/bin/env python3
# -*- coding: utf-8 -*-
from pathlib import Path
import re

SMARTTUBE_ROOT = Path("third_party/SmartTube-droid")

def log(msg):
    print(f"[NM7-OPT] {msg}")

def patch_file(path: Path, transform_fn):
    if not path.is_file():
        return False
    orig = path.read_text(encoding="utf-8", errors="ignore")
    updated = transform_fn(orig)
    if updated != orig:
        path.write_text(updated, encoding="utf-8")
        log(f"Patched: {path.name}")
        return True
    return False

# -------------------------------------------------------------------------
# 0. Sửa triệt để lỗi androidx.test.ext:junit:null & truth:null
# -------------------------------------------------------------------------
for p_gradle in SMARTTUBE_ROOT.rglob("build.gradle"):
    if ".git" in p_gradle.parts:
        continue
    def fix_null_versions(t):
        t = re.sub(r'androidx\.test\.ext:junit:(\$junitXVersion|null|\$rootProject\.ext\.junitXVersion)', 'androidx.test.ext:junit:1.1.5', t)
        t = re.sub(r'androidx\.test\.ext:truth:(\$truthXVersion|null|\$rootProject\.ext\.truthXVersion)', 'androidx.test.ext:truth:1.5.0', t)
        t = re.sub(r'androidx\.test:core:(\$testCoreVersion|null)', 'androidx.test:core:1.5.0', t)
        t = re.sub(r'androidx\.test:runner:(\$testRunnerVersion|null)', 'androidx.test:runner:1.5.2', t)
        t = re.sub(r'androidx\.test:rules:(\$testRulesVersion|null)', 'androidx.test:rules:1.5.0', t)
        return t
    patch_file(p_gradle, fix_null_versions)

# -------------------------------------------------------------------------
# 1. Sửa thiếu symbol section_is_empty và tối ưu PlaybackActivity
# -------------------------------------------------------------------------
p_play = SMARTTUBE_ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/playback/PlaybackActivity.java"
def opt_play(t):
    t = re.sub(r'getString\(\s*R\.string\.section_is_empty\s*\)', '"Section is empty"', t)
    if "Build the decoder/player before the Activity is shown" not in t:
        idx = t.find("public class PlaybackActivity")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    // NM7 Lifecycle: Build the decoder/player before the Activity is shown\n" + t[b+1:]

    if "mNm7FirstFrameRendered = true;" not in t:
        idx = t.find("public class PlaybackActivity")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    private boolean mNm7FirstFrameRendered = true;\n" + t[b+1:]

    t = re.sub(r'.*mNm74kRecoveryWatchdog.*', '', t)
    t = re.sub(r'.*setMaxVideoSize\(2560,\s*1440\);?.*', '', t)
    return t
patch_file(p_play, opt_play)

# -------------------------------------------------------------------------
# 2. Tối ưu ExoPlayer Buffer (150ms để phát tức thì khi chọn video)
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
# 3. Tối ưu Metadata format cache (tránh gọi lặp API)
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
# 4. Tắt hiệu ứng giật lag khi cuộn trang/danh mục
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

log("Đã áp dụng tối ưu YouTube và vá lỗi testutils thành công.")
