package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.media3.common.util.UnstableApi;

/** Adds left/right group navigation over the mobile channel list without changing TV D-pad behavior. */
@UnstableApi
public final class MobileGroupSwipeProvider extends ContentProvider implements Application.ActivityLifecycleCallbacks {
    private MainActivity activity;
    private ListView list;
    private LinearLayout groupRow;
    private float downX;
    private float downY;

    @Override public boolean onCreate() {
        if (getContext() != null) {
            if (AppPreferences.isPhysicalTv(getContext())) return true;
            ((Application) getContext().getApplicationContext()).registerActivityLifecycleCallbacks(this);
        }
        return true;
    }

    @Override public void onActivityResumed(Activity resumed) {
        if (!(resumed instanceof MainActivity) || AppPreferences.isTvInterface(resumed)) return;
        MainActivity main = (MainActivity) resumed;
        if (activity == main && list != null) return;
        detach();
        activity = main;
        list = main.findViewById(R.id.listChannels);
        groupRow = main.findViewById(R.id.groupRow);
        if (list == null || groupRow == null) return;
        list.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downX = event.getX();
                downY = event.getY();
                return false;
            }
            if (action == MotionEvent.ACTION_UP) {
                float dx = event.getX() - downX;
                float dy = event.getY() - downY;
                float threshold = dp(main, 72);
                if (Math.abs(dx) >= threshold && Math.abs(dx) > Math.abs(dy) * 1.35f) {
                    switchGroup(dx < 0 ? 1 : -1);
                    return true;
                }
            }
            return false;
        });
    }

    private void switchGroup(int direction) {
        if (groupRow == null || groupRow.getChildCount() == 0) return;
        int active = 0;
        for (int i = 0; i < groupRow.getChildCount(); i++) {
            if (groupRow.getChildAt(i).isSelected()) { active = i; break; }
        }
        int count = groupRow.getChildCount();
        int next = (active + direction + count) % count;
        View target = groupRow.getChildAt(next);
        target.performClick();
        View parent = (View) groupRow.getParent();
        if (parent instanceof HorizontalScrollView) {
            int x = Math.max(0, target.getLeft() - dp(activity, 16));
            parent.post(() -> ((HorizontalScrollView) parent).smoothScrollTo(x, 0));
        }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private void detach() {
        if (list != null) list.setOnTouchListener(null);
        activity = null;
        list = null;
        groupRow = null;
    }

    @Override public void onActivityDestroyed(Activity destroyed) { if (destroyed == activity) detach(); }
    @Override public void onActivityCreated(Activity a, Bundle b) { }
    @Override public void onActivityStarted(Activity a) { }
    @Override public void onActivityPaused(Activity a) { }
    @Override public void onActivityStopped(Activity a) { }
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String sort) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
