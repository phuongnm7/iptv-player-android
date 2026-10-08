#!/usr/bin/env python3
"""NM7 Mobile 1.10.119 — device/video-driven YouTube Browse chrome fix.

Evidence used:
- user device screenshot 223375.jpg
- supplied screen recording 223364.mp4 and its extracted frames
The recording shows the header and feed being laid out in conflicting states:
the YouTube logo can be drawn into the status-bar transition, and the feed can start
hundreds of pixels below the chip row.

The prior v118 patch did NOT actually transform the source layout because the v57-v114
patch chain rewrote it later. This patch is deliberately self-contained and runs AFTER
v114, so the final APK receives the intended architecture.

Scope:
- final Browse layout only;
- status-bar reservation through a dedicated spacer child;
- real CoordinatorLayout/AppBarLayout/RecyclerView hierarchy;
- first-content scroll reset only when a section becomes active / first data batch arrives;
- YouTube wordmark size/alignment.

No playback/IPTV/network/avatar/spinner/comment/lifecycle behavior is changed.
"""
from pathlib import Path
import re
import shutil

ROOT=Path("third_party/SmartTube-droid")
SRC=Path("scripts/mobile-ui/res/layout/browse_activity.xml")
DST=ROOT/"smarttubedroid/src/main/res/layout/browse_activity.xml"
BROWSE=ROOT/"smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/browse/BrowseActivity.java"

if not SRC.is_file() or not BROWSE.is_file():
    raise SystemExit("v119: missing source files")

xml=SRC.read_text(encoding="utf-8")

# Final layout architecture. The source intentionally remains the v114-compatible
# LinearLayout for earlier patches; v119 converts it after v114 has finished.
xml=xml.replace(
    '<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"',
    '<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"',
    1)
xml=xml.replace('    android:orientation="vertical"
', '', 1)

# Root owns no top inset/padding. A dedicated spacer owns the status-bar reservation.
xml=xml.replace('    android:paddingBottom="64dp"
', '    android:paddingBottom="64dp"
', 1)

spacer='''    <View
        android:id="@+id/nm7_status_bar_spacer"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:background="#FFFFFF" />

'''
marker='    <com.google.android.material.appbar.AppBarLayout
'
if 'android:id="@+id/nm7_status_bar_spacer"' not in xml:
    if marker not in xml: raise SystemExit("v119: AppBar marker missing")
    xml=xml.replace(marker,spacer+marker,1)

# Make AppBarLayout a real scrolling dependency.
xml=xml.replace(
'''        android:background="#FFFFFF"
        android:elevation="2dp">''',
'''        android:background="#FFFFFF"
        android:elevation="2dp"
        app:liftOnScroll="false">''',1)

# Header and chip strip scroll as one app bar.
xml=xml.replace(
'''            android:gravity="center_vertical"
            android:orientation="horizontal">''',
'''            android:gravity="center_vertical"
            android:orientation="horizontal"
            app:layout_scrollFlags="scroll|enterAlways">''',1)
xml=xml.replace(
'''            android:fillViewport="false"
            android:paddingStart="8dp"''',
'''            android:fillViewport="false"
            app:layout_scrollFlags="scroll|enterAlways"
            android:paddingStart="8dp"''',1)

# Bigger wordmark: the reference logo is wider/taller than the former 76x30dp box.
xml=xml.replace(
'''                android:layout_width="76dp"
                android:layout_height="30dp"''',
'''                android:layout_width="92dp"
                android:layout_height="36dp"''',1)

# Content is a scrolling sibling of AppBarLayout, never weighted normal-flow content.
xml=xml.replace(
'''        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">''',
'''        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">''',1)

# Visible RecyclerViews: never add a fake top gap. Preserve horizontal margins.
xml=xml.replace('android:paddingTop="8dp"
            android:paddingStart="12dp"',
                'android:paddingTop="0dp"
            android:paddingStart="12dp"')
xml=xml.replace('android:paddingTop="0dp"
            android:paddingBottom="12dp"',
                'android:paddingTop="0dp"
            android:paddingBottom="0dp"',1)

xml=xml.replace('</LinearLayout>
', '</androidx.coordinatorlayout.widget.CoordinatorLayout>
')
DST.parent.mkdir(parents=True,exist_ok=True)
DST.write_text(xml,encoding="utf-8")

s=BROWSE.read_text(encoding="utf-8")

# ---------------- status bar: spacer, not root padding ----------------
sig="    private void applyNm7PortraitSystemBars() {"
pos=s.find(sig)
if pos<0: raise SystemExit("v119: status-bar method missing")
brace=s.find("{",pos); depth=0; end=-1
for i in range(brace,len(s)):
    if s[i]=="{": depth+=1
    elif s[i]=="}":
        depth-=1
        if depth==0:
            end=i+1; break
if end<0: raise SystemExit("v119: status-bar method end missing")

method='''    private void applyNm7PortraitSystemBars() {
        if (isLandscape()) return;

        final android.view.Window window = getWindow();
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
                | android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.setStatusBarColor(android.graphics.Color.WHITE);

        if (android.os.Build.VERSION.SDK_INT >= 30) {
            // Android 15 target apps may be edge-to-edge. Keep the window edge-to-edge and
            // reserve the actual status bar height with a real child view below the inset.
            window.setDecorFitsSystemWindows(false);
            android.view.WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.show(android.view.WindowInsets.Type.statusBars());
                controller.show(android.view.WindowInsets.Type.navigationBars());
                controller.setSystemBarsAppearance(
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else {
            int flags=window.getDecorView().getSystemUiVisibility();
            flags &= ~(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            if (android.os.Build.VERSION.SDK_INT >= 23) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            window.getDecorView().setSystemUiVisibility(flags);
        }

        final View root=findViewById(R.id.browse_root);
        final View spacer=findViewById(R.id.nm7_status_bar_spacer);
        if (root != null && spacer != null) {
            root.setPadding(0, 0, 0, root.getPaddingBottom());
            root.setOnApplyWindowInsetsListener((v,insets)->{
                int top;
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    top=insets.getInsets(android.view.WindowInsets.Type.statusBars()).top;
                } else {
                    top=insets.getSystemWindowInsetTop();
                }
                android.view.ViewGroup.LayoutParams lp=spacer.getLayoutParams();
                int safeTop=Math.max(0,top);
                if (lp.height != safeTop) {
                    lp.height=safeTop;
                    spacer.setLayoutParams(lp);
                }
                return insets;
            });
            root.requestApplyInsets();
        }
    }
'''
s=s[:pos]+method+s[end:]

# ---------------- remove legacy manual scroll-chrome padding ----------------
m=re.search(r'(?ms)^    private void setNm7ChromeHidden\(boolean hidden\) \{.*?^    \}',s)
if m:
    s=s[:m.start()]+'''    private void setNm7ChromeHidden(boolean hidden) {
        // v119: CoordinatorLayout/AppBarLayout owns the chrome transition.
        mNm7ChromeHidden = hidden;
    }'''+s[m.end():]

m=re.search(r'(?ms)^    private void installNm7ScrollChrome\(RecyclerView list\) \{.*?^    \}',s)
if m:
    s=s[:m.start()]+'''    private void installNm7ScrollChrome(RecyclerView list) {
        // v119: never add header-height padding to the feed.
        if (list != null) {
            list.setClipToPadding(true);
            list.setPadding(list.getPaddingLeft(), 0, list.getPaddingRight(), 0);
        }
    }'''+s[m.end():]

# ---------------- first data = top of feed ----------------
if "mNm7FirstFeedLayoutFixed" not in s:
    anchor='    private boolean mJustCreated;'
    if anchor not in s: raise SystemExit("v119: field anchor missing")
    s=s.replace(anchor,anchor+'\n    private boolean mNm7FirstFeedLayoutFixed;',1)

helper_anchor='    // ------------------------------------------------------------------ tab plumbing\n'
helper='''    private void nm7FixInitialFeedPosition() {
        if (mNm7FirstFeedLayoutFixed) return;
        mNm7FirstFeedLayoutFixed=true;
        RecyclerView view=getCurrentContentView();
        if (view != null) {
            view.stopScroll();
            view.post(() -> {
                if (view.getLayoutManager() != null) {
                    view.getLayoutManager().scrollToPosition(0);
                }
            });
        }
    }

'''
if "private void nm7FixInitialFeedPosition" not in s:
    if helper_anchor not in s: raise SystemExit("v119: tab anchor missing")
    s=s.replace(helper_anchor,helper_anchor+helper,1)

focus_old='''        BrowseSection section = mSections.get(position);
        mCurrentSection = section;
        showContentForType(section.getType());

        mBrowsePresenter.onSectionFocused(section.getId());'''
focus_new='''        BrowseSection section = mSections.get(position);
        mCurrentSection = section;
        mNm7FirstFeedLayoutFixed = false;
        showContentForType(section.getType());
        nm7FixInitialFeedPosition();

        mBrowsePresenter.onSectionFocused(section.getId());'''
if focus_old in s:
    s=s.replace(focus_old,focus_new,1)

# After data arrives, post a single top reset for the currently active feed.
anchor='''        showContentForType(type);

        if (type == BrowseSection.TYPE_ROW) {'''
if anchor in s:
    s=s.replace(anchor,'''        showContentForType(type);
        nm7FixInitialFeedPosition();

        if (type == BrowseSection.TYPE_ROW) {''',1)

BROWSE.write_text(s,encoding="utf-8")

print("NM7 Mobile 1.10.119 final video-driven chrome correction applied")
