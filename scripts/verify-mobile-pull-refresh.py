#!/usr/bin/env python3
"""Fail-closed structural verification for NM7 1.10.129 pull-to-refresh."""
from pathlib import Path

root = Path("third_party/SmartTube-droid")
java = (root / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/browse/BrowseActivity.java").read_text(encoding="utf-8")
layout = (root / "smarttubedroid/src/main/res/layout/browse_activity.xml").read_text(encoding="utf-8")
gradle = (root / "smarttubedroid/build.gradle").read_text(encoding="utf-8")
app_gradle = Path("app/build.gradle.kts").read_text(encoding="utf-8")
smarttube_gradle = Path("smarttube/build.gradle.kts").read_text(encoding="utf-8")

checks = [
    ("uses SwipeRefreshLayout", "SwipeRefreshLayout" in java),
    ("layout includes pull-refresh container", 'android:id="@+id/browse_pull_refresh"' in layout),
    ("browse content remains inside refresh target", 'android:id="@+id/browse_content"' in layout and 'android:layout_height="match_parent">' in layout),
    ("SwipeRefreshLayout dependency is declared in compiled :smarttube module", "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0" in smarttube_gradle),
    ("pull gesture is gated to Home", "mPullToRefresh.setEnabled(isHomeSection())" in java and "MediaGroup.TYPE_HOME" in java),
    ("gesture checks feed scroll position", "current.canScrollVertically(-1)" in java),
    ("reload uses existing presenter", "mBrowsePresenter.refresh(false);" in java),
    ("spinner ends after loading/error", "mPullToRefresh.setRefreshing(false)" in java and "if (!show && mPullToRefresh" in java),
    ("versionName incremented", 'versionName = "1.10.129"' in app_gradle),
    ("versionCode incremented", "versionCode = 145" in app_gradle),
]
for label, ok in checks:
    if not ok:
        raise SystemExit("FAIL: " + label)
    print("PASS: " + label)
print(f"PASS: {len(checks)} pull-to-refresh structural checks")
