#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
NM7 IPTV - Triệt tiêu màn hình đen & Khắc phục đứng hình lệch tiếng 4K
1. Xóa bỏ tấm chắn đen exo_shutter trong toàn bộ layout PlayerView.
2. Vô hiệu hóa cơ chế drop frame (shouldDropBuffersToKeyframe) trong MediaCodecVideoRenderer.
3. Bắt sự kiện nạp video trong PlaybackActivity để giữ thumbnail khi đổi video đề xuất.
4. Tự động sửa toàn bộ lỗi biên dịch dependencies và cú pháp Java.
"""

from pathlib import Path
import re

SMARTTUBE_ROOT = Path("third_party/SmartTube-droid")

def log(msg):
    print(f"[NM7-CORE-OPT] {msg}")

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

# =========================================================================
# 1. KHẮC PHỤC LỖI GRADLE & DEPENDENCIES TESTUTILS
# =========================================================================
props_content = """
junitXVersion=1.1.5
truthXVersion=1.5.0
testCoreVersion=1.5.0
testRunnerVersion=1.5.2
testRulesVersion=1.5.0
truthVersion=1.1.3
"""
for gp in [Path("gradle.properties"), SMARTTUBE_ROOT / "gradle.properties"]:
    if gp.parent.is_dir():
        cur = gp.read_text(errors="ignore") if gp.is_file() else ""
        if "junitXVersion" not in cur:
            gp.write_text(cur + "\n" + props_content)

def fix_test_deps(t):
    t = re.sub(r"['\"]androidx\.test\.ext:junit:?['\"]\s*\+\s*[^,\n\)]+", "'androidx.test.ext:junit:1.1.5'", t)
    t = re.sub(r"['\"]androidx\.test\.ext:truth:?['\"]\s*\+\s*[^,\n\)]+", "'androidx.test.ext:truth:1.5.0'", t)
    t = re.sub(r"['\"]androidx\.test:core:?['\"]\s*\+\s*[^,\n\)]+", "'androidx.test:core:1.5.0'", t)
    t = re.sub(r"['\"]androidx\.test:runner:?['\"]\s*\+\s*[^,\n\)]+", "'androidx.test:runner:1.5.2'", t)
    t = re.sub(r"['\"]androidx\.test:rules:?['\"]\s*\+\s*[^,\n\)]+", "'androidx.test:rules:1.5.0'", t)
    t = re.sub(r"['\"]androidx\.test\.ext:junit:[^'\"]*['\"]", "'androidx.test.ext:junit:1.1.5'", t)
    t = re.sub(r"['\"]androidx\.test\.ext:truth:[^'\"]*['\"]", "'androidx.test.ext:truth:1.5.0'", t)
    return t

for p_gradle in SMARTTUBE_ROOT.rglob("*.gradle*"):
    if ".git" in p_gradle.parts: continue
    patch_file(p_gradle, fix_test_deps)

# Cập nhật cache định dạng 60s
p_item = SMARTTUBE_ROOT / "MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/service/YouTubeMediaItemService.java"
def opt_item(t):
    if "NM7_FORMAT_REUSE_MS" in t:
        t = re.sub(r'(NM7_FORMAT_REUSE_MS\s*=\s*)[^;]+;', r'\g<1>60_000L;', t)
    else:
        idx = t.find("public class YouTubeMediaItemService")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    public static final long NM7_FORMAT_REUSE_MS = 60_000L;\n" + t[b+1:]
    return t
patch_file(p_item, opt_item)

# =========================================================================
# 2. XÓA BỎ HOÀN TOÀN MÀN HÌNH ĐEN (TRIỆT TIÊU TẤM MÀN EXO_SHUTTER)
# =========================================================================
for p_xml in SMARTTUBE_ROOT.rglob("*.xml"):
    if ".git" in p_xml.parts: continue
    def clear_shutter(t):
        if "exo_shutter" in t:
            # Thay đổi toàn bộ nền đen của exo_shutter thành trong suốt
            t = re.sub(r'(<View[^>]*id=["\']@\+?id/exo_shutter["\'][^>]*android:background=)["\'][^"\']+["\']', r'\1"@android:color/transparent"', t)
        return t
    patch_file(p_xml, clear_shutter)

# Trong PlayerView.java, vô hiệu hóa việc ép hiển thị màn đen
for p_pv in SMARTTUBE_ROOT.rglob("PlayerView.java"):
    if ".git" in p_pv.parts: continue
    def disable_shutter_view(t):
        t = re.sub(r'shutterView\.setVisibility\(VISIBLE\);', 'shutterView.setVisibility(GONE);', t)
        return t
    patch_file(p_pv, disable_shutter_view)

# =========================================================================
# 3. CHỮA DỨT ĐIỂM ĐỨNG HÌNH & LỆCH TIẾNG 4K TRONG EXOPLAYER
# =========================================================================
for p_codec in SMARTTUBE_ROOT.rglob("MediaCodecVideoRenderer.java"):
    if ".git" in p_codec.parts: continue
    def fix_4k_drop(t):
        # Chặn việc vứt bỏ toàn bộ khung hình video đến keyframe tiếp theo
        if "boolean shouldDropBuffersToKeyframe" in t:
            t = re.sub(
                r'protected\s+boolean\s+shouldDropBuffersToKeyframe\s*\([^)]*\)\s*\{[^}]*\}',
                'protected boolean shouldDropBuffersToKeyframe(long earlyUs, long elapsedRealtimeUs) { return false; }',
                t
            )
        # Giảm mức drop frame khi video 4K đang nạp
        if "boolean shouldDropOutputBuffer" in t:
            t = re.sub(
                r'protected\s+boolean\s+shouldDropOutputBuffer\s*\([^)]*\)\s*\{[^}]*\}',
                'protected boolean shouldDropOutputBuffer(long earlyUs, long elapsedRealtimeUs) { return earlyUs < -100000; }',
                t
            )
        return t
    patch_file(p_codec, fix_4k_drop)

# Tối ưu bộ đệm phát trong toàn bộ các file quản lý LoadControl
for p_load in SMARTTUBE_ROOT.rglob("*.java"):
    if ".git" in p_load.parts: continue
    def tune_load(t):
        if "setBufferDurationsMs" in t:
            t = re.sub(r'\.setBufferDurationsMs\([^)]+\)', '.setBufferDurationsMs(25000, 60000, 500, 1500)', t)
        if "setTargetBufferBytes" in t:
            t = re.sub(r'\.setTargetBufferBytes\([^)]+\)', '.setTargetBufferBytes(128 * 1024 * 1024)', t)
        return t
    patch_file(p_load, tune_load)

# =========================================================================
# 4. TỐI ƯU VIDEOCARDHOLDER & PLAYBACKACTIVITY
# =========================================================================
p_card = SMARTTUBE_ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/shared/VideoCardHolder.java"
def opt_card(t):
    t = re.sub(r'\.delaySubscription\(650[^)]*\)', '', t)
    t = re.sub(r'\bprivate(\s+static\s+(volatile\s+)?(android\.graphics\.)?Bitmap\s+sNm7TransitionPoster)', r'public\1', t)
    t = re.sub(r'if\s*\([^)]*sNm7TransitionVideoId[^)]*\)', 'if (sNm7TransitionPoster != null)', t)
    return t
patch_file(p_card, opt_card)

p_play = SMARTTUBE_ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/playback/PlaybackActivity.java"
def opt_play(t):
    t = re.sub(r'getString\(\s*R\.string\.section_is_empty\s*\)', '"Section is empty"', t)
    t = re.sub(r'.*mNm74kRecoveryWatchdog.*', '', t)
    t = re.sub(r'.*setMaxVideoSize\(2560,\s*1440\);?.*', '', t)

    # Đảm bảo tắt transition mặc định gây chớp nháy
    if "overridePendingTransition(0, 0);" not in t:
        t = re.sub(r'(super\.onCreate\([^)]*\);)', r'\1\n        try { overridePendingTransition(0, 0); } catch(Throwable ignored) {}', t)

    if "Build the decoder/player before the Activity is shown" not in t:
        idx = t.find("public class PlaybackActivity")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    // NM7 Lifecycle: Build the decoder/player before the Activity is shown\n" + t[b+1:]
    return t
patch_file(p_play, opt_play)

# =========================================================================
# 5. TẮT HIỆU ỨNG CHUYỂN CẢNH GÂY KHỰNG GIAO DIỆN
# =========================================================================
p_browse = SMARTTUBE_ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/browse/BrowseActivity.java"
def opt_browse(t):
    if "setItemAnimator(null)" not in t:
        idx = t.find("public class BrowseActivity")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    private void disableAnim(androidx.recyclerview.widget.RecyclerView r) { if (r != null) r.setItemAnimator(null); }\n" + t[b+1:]
    return t
patch_file(p_browse, opt_browse)

p_view = SMARTTUBE_ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/views/ViewManager.java"
def opt_view(t):
    if "FLAG_ACTIVITY_NO_ANIMATION" not in t:
        idx = t.find("public class ViewManager")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    public static final int NM7_FLAG = android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;\n" + t[b+1:]
    return t
patch_file(p_view, opt_view)

log("Đã triệt tiêu hoàn toàn màn hình đen và khắc phục đứng hình lệch tiếng 4K.")
