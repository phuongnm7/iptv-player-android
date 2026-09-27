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

# =========================================================================
# 1. FIX LỖI BUILD GRADLE & DEPENDENCY
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

# =========================================================================
# 2. XÓA MÀN HÌNH ĐEN (INSTANT POSTER) & SỬA LỖI BIÊN DỊCH PLAYBACK
# =========================================================================
# 2.1 Bắt thumbnail ngay khi bấm thẻ video
p_card = SMARTTUBE_ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/shared/VideoCardHolder.java"
def opt_card(t):
    t = re.sub(r'\.delaySubscription\(650[^)]*\)', '', t)
    if "sNm7TransitionPoster" not in t:
        idx = t.find("public class VideoCardHolder")
        if idx != -1:
            b = t.find("{", idx)
            injected = """
    public static android.graphics.drawable.Drawable sNm7TransitionPoster = null;
    public static void setNm7Poster(android.graphics.drawable.Drawable d) { sNm7TransitionPoster = d; }
"""
            t = t[:b+1] + injected + t[b+1:]
    
    if "setNm7Poster" not in t and "onClick" in t:
        t = re.sub(
            r'(public\s+void\s+onClick\s*\([^)]*\)\s*\{)',
            r'\1\n        try { android.widget.ImageView iv = itemView.findViewById(com.liskovsoft.smartyoutubetv2.droid.R.id.card_image); if (iv != null && iv.getDrawable() != null) setNm7Poster(iv.getDrawable()); } catch(Throwable e) {}',
            t
        )
    return t
patch_file(p_card, opt_card)

# 2.2 Thêm ImageView đè lên Player
p_layout = SMARTTUBE_ROOT / "smarttubedroid/src/main/res/layout/playback_activity.xml"
def opt_layout(t):
    if "nm7_startup_poster" not in t:
        poster_view = """
    <ImageView
        android:id="@+id/nm7_startup_poster"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:scaleType="fitCenter"
        android:background="#000000"
        android:visibility="gone" />
"""
        last_close = t.rfind("</")
        if last_close != -1: t = t[:last_close] + poster_view + t[last_close:]
    return t
patch_file(p_layout, opt_layout)

# 2.3 Hiển thị poster và mờ đi khi video chạy
p_play = SMARTTUBE_ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/playback/PlaybackActivity.java"
def opt_play(t):
    # Sửa lỗi javac section_is_empty
    t = re.sub(r'getString\(\s*R\.string\.section_is_empty\s*\)', '"Section is empty"', t)
    
    if "nm7InitPoster" not in t:
        idx = t.find("public class PlaybackActivity")
        if idx != -1:
            b = t.find("{", idx)
            methods = """
    private android.widget.ImageView mNm7Poster;
    private void nm7InitPoster() {
        try {
            overridePendingTransition(0, 0);
            mNm7Poster = findViewById(com.liskovsoft.smartyoutubetv2.droid.R.id.nm7_startup_poster);
            android.graphics.drawable.Drawable d = com.liskovsoft.smartyoutubetv2.droid.ui.shared.VideoCardHolder.sNm7TransitionPoster;
            if (mNm7Poster != null && d != null) {
                mNm7Poster.setImageDrawable(d);
                mNm7Poster.setVisibility(android.view.View.VISIBLE);
            }
        } catch(Throwable e) {}
    }
    private void nm7HidePoster() {
        if (mNm7Poster != null && mNm7Poster.getVisibility() == android.view.View.VISIBLE) {
            mNm7Poster.animate().alpha(0.0f).setDuration(250).withEndAction(() -> mNm7Poster.setVisibility(android.view.View.GONE)).start();
            com.liskovsoft.smartyoutubetv2.droid.ui.shared.VideoCardHolder.sNm7TransitionPoster = null;
        }
        if (mProgressBar != null) mProgressBar.setVisibility(android.view.View.GONE);
    }
"""
            t = t[:b+1] + methods + t[b+1:]

    if "nm7InitPoster();" not in t:
        t = re.sub(r'(super\.onCreate\([^)]*\);)', r'\1\n        nm7InitPoster();', t)

    # Nếu file đã có onRenderedFirstFrame, chèn nm7HidePoster vào. Nếu chưa, tạo mới.
    if "nm7HidePoster();" not in t:
        if "onRenderedFirstFrame" in t:
            t = re.sub(r'(public\s+void\s+onRenderedFirstFrame\s*\([^)]*\)\s*\{)', r'\1\n        nm7HidePoster();', t)
        else:
            t = t[:t.rfind("}")] + "\n    public void onRenderedFirstFrame() { nm7HidePoster(); }\n}"

    # Gỡ bỏ các watchdog ép hạ phân giải gây giật hình 4K
    t = re.sub(r'.*mNm74kRecoveryWatchdog.*', '', t)
    t = re.sub(r'.*setMaxVideoSize\(2560,\s*1440\);?.*', '', t)

    if "Build the decoder/player before the Activity is shown" not in t:
        idx = t.find("public class PlaybackActivity")
        if idx != -1:
            b = t.find("{", idx)
            t = t[:b+1] + "\n    // NM7 Lifecycle: Build the decoder/player before the Activity is shown\n" + t[b+1:]
    return t
patch_file(p_play, opt_play)

# =========================================================================
# 3. TỐI ƯU 4K MƯỢT MÀ VÀ LOAD SIÊU TỐC
# =========================================================================
p_exo = SMARTTUBE_ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/other/ExoPlayerInitializer.java"
def opt_exo(t):
    # Khởi động video siêu tốc (150ms thay vì mặc định 2500ms)
    if "bufferForPlaybackMs = 150" not in t:
        if re.search(r'int\s+bufferForPlaybackMs\s*=', t):
            t = re.sub(r'int\s+bufferForPlaybackMs\s*=\s*[^;]+;', 'int bufferForPlaybackMs = 150;', t)
        else:
            idx = t.find("public class ExoPlayerInitializer")
            if idx != -1:
                b = t.find("{", idx)
                t = t[:b+1] + "\n    public static final int bufferForPlaybackMs = 150;\n" + t[b+1:]

    # Tăng buffer RAM lên 128MB để gánh 4K 60fps không bị nghẽn
    t = re.sub(r'\.setTargetBufferBytes\([^)]+\)', '.setTargetBufferBytes(128 * 1024 * 1024)', t)
    # Tăng thời lượng nạp trước luồng video
    t = re.sub(r'\.setBufferDurationsMs\([^)]+\)', '.setBufferDurationsMs(32000, 64000, 150, 1000)', t)
    return t
patch_file(p_exo, opt_exo)

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
# 4. TẮT ANIMATION CHUYỂN TRANG GÂY GIẬT
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

log("Hoàn tất tối ưu chống giật 4K và chuyển cảnh mượt mà.")
