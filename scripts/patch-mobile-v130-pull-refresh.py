#!/usr/bin/env python3
"""Add robust YouTube Home pull-to-refresh fallback for NM7 Mobile v1.10.130."""
from pathlib import Path
import re

ROOT = Path("third_party/SmartTube-droid")
JAVA = ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/browse/BrowseActivity.java"
LAYOUT = ROOT / "smarttubedroid/src/main/res/layout/browse_activity.xml"
UPSTREAM_GRADLE = ROOT / "smarttubedroid/build.gradle"
APP_MODULE_GRADLE = Path("smarttube/build.gradle.kts")


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"v130 pull-refresh: expected one {label} anchor, found {count}")
    return text.replace(old, new, 1)


for path in (JAVA, LAYOUT, UPSTREAM_GRADLE, APP_MODULE_GRADLE):
    if not path.is_file():
        raise SystemExit(f"v130 pull-refresh: required source missing: {path}")

# Keep dependency declared in both upstream library metadata and the actual :smarttube module.
gradle = UPSTREAM_GRADLE.read_text(encoding="utf-8")
dep_anchor = "    implementation 'androidx.recyclerview:recyclerview:' + recyclerviewXVersion"
gradle = replace_once(
    gradle,
    dep_anchor,
    dep_anchor + "\n    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'",
    "upstream SwipeRefreshLayout dependency",
)
UPSTREAM_GRADLE.write_text(gradle, encoding="utf-8")

module_gradle = APP_MODULE_GRADLE.read_text(encoding="utf-8")
module_anchor = '    implementation("androidx.recyclerview:recyclerview:1.2.1")'
module_gradle = replace_once(
    module_gradle,
    module_anchor,
    module_anchor + '\n    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")',
    "actual :smarttube SwipeRefreshLayout dependency",
)
APP_MODULE_GRADLE.write_text(module_gradle, encoding="utf-8")

# Place a SwipeRefreshLayout around the exact content pane used by BrowseActivity.
layout = LAYOUT.read_text(encoding="utf-8")
frame_open = '''    <FrameLayout
        android:id="@+id/browse_content"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">'''
swipe_open = '''    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/browse_pull_refresh"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:enabled="false">

        <FrameLayout
            android:id="@+id/browse_content"
            android:layout_width="match_parent"
            android:layout_height="match_parent">'''
layout = replace_once(layout, frame_open, swipe_open, "browse_content wrapper")
layout = replace_once(
    layout,
    "    </FrameLayout>\n</LinearLayout>",
    "        </FrameLayout>\n    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>\n</LinearLayout>",
    "browse_content closing wrapper",
)
LAYOUT.write_text(layout, encoding="utf-8")

java = JAVA.read_text(encoding="utf-8")
java = replace_once(
    java,
    "import android.view.MenuItem;",
    "import android.view.MenuItem;\nimport android.view.MotionEvent;",
    "MotionEvent import",
)
java = replace_once(
    java,
    "import androidx.recyclerview.widget.RecyclerView;",
    "import androidx.recyclerview.widget.RecyclerView;\nimport androidx.swiperefreshlayout.widget.SwipeRefreshLayout;",
    "SwipeRefreshLayout import",
)
java = replace_once(
    java,
    "    private ProgressBar mProgressBar;",
    """    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mPullToRefresh;
    private float mPullStartRawX;
    private float mPullStartRawY;
    private boolean mPullStartEligible;
    private boolean mPullGestureCancelled;
    private boolean mNativeRefreshHandledThisGesture;
    private final Runnable mPullRefreshTimeout = this::finishPullRefresh;""",
    "pull-refresh state fields",
)
java = replace_once(
    java,
    "        mProgressBar = findViewById(R.id.browse_progress);",
    "        mProgressBar = findViewById(R.id.browse_progress);\n        mPullToRefresh = findViewById(R.id.browse_pull_refresh);",
    "refresh view binding",
)
refresh_setup_anchor = "        mSettingsView.setAdapter(mSettingsAdapter);"
refresh_setup = '''        mSettingsView.setAdapter(mSettingsAdapter);

        // Keep AndroidX's native pull indicator/interception, with an Activity-level
        // gesture fallback below for the nested horizontal rows used by the Home feed.
        mPullToRefresh.setColorSchemeColors(0xFFFF0033);
        mPullToRefresh.setDistanceToTriggerSync(
                (int) (getResources().getDisplayMetrics().density * 88f));
        mPullToRefresh.setOnChildScrollUpCallback((parent, child) -> !isCurrentFeedAtTop());
        mPullToRefresh.setOnRefreshListener(this::onPullToRefresh);'''
java = replace_once(java, refresh_setup_anchor, refresh_setup, "refresh callback setup")

focus_anchor = '''        BrowseSection section = mSections.get(position);
        mCurrentSection = section;
        showContentForType(section.getType());'''
focus_replacement = '''        BrowseSection section = mSections.get(position);
        mCurrentSection = section;
        if (mPullToRefresh != null) {
            mPullToRefresh.setEnabled(isHomeSection());
        }
        showContentForType(section.getType());'''
java = replace_once(java, focus_anchor, focus_replacement, "Home-only enablement")

# dispatchTouchEvent observes the complete gesture before a nested RecyclerView can hide it.
# The fallback is posted so SwipeRefreshLayout gets the first opportunity to handle the
# gesture; it invokes reload only when the native listener did not already start.
touch_anchor = "    // ------------------------------------------------------------------ init\n"
touch_methods = '''    private void trackPullDownGesture(MotionEvent event) {
        if (mPullToRefresh == null) {
            return;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mPullStartRawX = event.getRawX();
                mPullStartRawY = event.getRawY();
                mPullStartEligible = isHomeSection()
                        && !mPullToRefresh.isRefreshing()
                        && isTouchInsidePullArea(event)
                        && isCurrentFeedAtTop();
                mPullGestureCancelled = false;
                mNativeRefreshHandledThisGesture = false;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mPullStartEligible) {
                    float dx = event.getRawX() - mPullStartRawX;
                    float dy = event.getRawY() - mPullStartRawY;
                    float slop = getResources().getDisplayMetrics().density * 8f;
                    if (dy < -slop || Math.abs(dx) > Math.abs(dy) * 1.25f) {
                        mPullGestureCancelled = true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mPullStartEligible && !mPullGestureCancelled) {
                    float dx = event.getRawX() - mPullStartRawX;
                    float dy = event.getRawY() - mPullStartRawY;
                    float threshold = getResources().getDisplayMetrics().density * 96f;
                    if (dy >= threshold && dy > Math.abs(dx) * 1.2f) {
                        mPullToRefresh.post(() -> {
                            // Run only if native SwipeRefreshLayout did not already handle it.
                            if (mPullToRefresh != null && !mNativeRefreshHandledThisGesture
                                    && !mPullToRefresh.isRefreshing()
                                    && isHomeSection() && isCurrentFeedAtTop()) {
                                mPullToRefresh.setRefreshing(true);
                                onPullToRefresh();
                            }
                        });
                    }
                }
                mPullStartEligible = false;
                mPullGestureCancelled = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                mPullStartEligible = false;
                mPullGestureCancelled = false;
                break;
            default:
                break;
        }
    }

    private boolean isTouchInsidePullArea(MotionEvent event) {
        int[] location = new int[2];
        mPullToRefresh.getLocationOnScreen(location);
        float x = event.getRawX();
        float y = event.getRawY();
        return x >= location[0] && x < location[0] + mPullToRefresh.getWidth()
                && y >= location[1] && y < location[1] + mPullToRefresh.getHeight();
    }

    private boolean isHomeSection() {
        return mCurrentSection != null
                && mCurrentSection.getId()
                == com.liskovsoft.mediaserviceinterfaces.data.MediaGroup.TYPE_HOME;
    }

    private boolean isCurrentFeedAtTop() {
        RecyclerView current = getCurrentContentView();
        return current != null && !current.canScrollVertically(-1);
    }

    private void onPullToRefresh() {
        mNativeRefreshHandledThisGesture = true;
        if (!isHomeSection()) {
            finishPullRefresh();
            return;
        }

        mPullToRefresh.removeCallbacks(mPullRefreshTimeout);
        mPullToRefresh.postDelayed(mPullRefreshTimeout, 20_000L);
        // Reuse the current BrowsePresenter reload path; do not recreate the Activity.
        mBrowsePresenter.refresh(false);
    }

    private void finishPullRefresh() {
        if (mPullToRefresh != null) {
            mPullToRefresh.removeCallbacks(mPullRefreshTimeout);
            if (mPullToRefresh.isRefreshing()) {
                mPullToRefresh.setRefreshing(false);
            }
        }
    }

'''
# Important: an earlier Mobile UI patch already installs an Activity-level dispatcher.
# Modify its existing override instead of adding a second dispatchTouchEvent method.
dispatch_pattern = re.compile(
    r'(?m)^(?P<indent>[ \t]*)public\s+boolean\s+dispatchTouchEvent\s*'
    r'\(\s*(?:(?:@[\w.]+(?:\([^)]*\))?)\s*)*(?:(?:final)\s+)?'
    r'(?:android\.view\.)?MotionEvent\s+(?P<arg>\w+)\s*\)\s*\{'
)
matches = list(dispatch_pattern.finditer(java))
if len(matches) > 1:
    raise SystemExit(f"v130 pull-refresh: expected <=1 Activity dispatcher, found {len(matches)}")
if matches:
    match = matches[0]
    arg = match.group("arg")
    indent = match.group("indent") + "    "
    method_end_probe = java[match.end():match.end() + 500]
    call = f"trackPullDownGesture({arg});"
    if call not in method_end_probe:
        java = java[:match.end()] + "\n" + indent + call + java[match.end():]
else:
    dispatch = '''    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        trackPullDownGesture(event);
        return super.dispatchTouchEvent(event);
    }

'''
    java = replace_once(java, touch_anchor, dispatch + touch_anchor, "Activity-level gesture dispatcher")
java = replace_once(java, touch_anchor, touch_methods + touch_anchor, "Activity-level gesture tracker")
java = java.replace(
    "import android.os.Bundle;",
    "import android.os.Bundle;\nimport java.util.regex.Pattern;",
) if False else java

# Refresh indicator must stop on both normal loading completion and the existing error path.
progress_anchor = "        runOnUiThread(() -> mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE));"
progress_replacement = '''        runOnUiThread(() -> {
            mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            if (!show) {
                finishPullRefresh();
            }
        });'''
java = replace_once(java, progress_anchor, progress_replacement, "refresh completion")
java = replace_once(
    java,
    "        showContentView(mErrorContainer);",
    "        finishPullRefresh();\n        showContentView(mErrorContainer);",
    "error refresh cleanup",
)
JAVA.write_text(java, encoding="utf-8")

print("NM7 Mobile v1.10.130 Home pull-to-refresh fallback patch applied.")
