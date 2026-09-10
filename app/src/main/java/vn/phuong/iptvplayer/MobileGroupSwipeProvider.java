package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;

import androidx.media3.common.util.UnstableApi;

/** Adds smooth left/right group navigation over the mobile channel list without changing TV D-pad behavior. */
@UnstableApi
public final class MobileGroupSwipeProvider extends ContentProvider implements Application.ActivityLifecycleCallbacks {
    private MainActivity activity;
    private ListView list;
    private LinearLayout groupRow;
    private float downX;
    private float downY;
    private int touchSlop;
    private boolean horizontalSwipe;
    private VelocityTracker velocityTracker;

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

        // Mobile rows should be visually compact. Also remove the old long-press
        // channel action dialog: tap plays, the star button controls favourites.
        list.setDividerHeight(dp(main, 2));
        list.setOnItemLongClickListener(null);
        touchSlop = ViewConfiguration.get(main).getScaledTouchSlop();

        list.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                recycleVelocityTracker();
                velocityTracker = VelocityTracker.obtain();
                velocityTracker.addMovement(event);
                downX = event.getX();
                downY = event.getY();
                horizontalSwipe = false;
                return false;
            }

            if (velocityTracker != null) velocityTracker.addMovement(event);
            float dx = event.getX() - downX;
            float dy = event.getY() - downY;

            if (action == MotionEvent.ACTION_MOVE) {
                if (!horizontalSwipe
                        && Math.abs(dx) > Math.max(touchSlop * 2, dp(main, 18))
                        && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                    horizontalSwipe = true;
                    view.cancelLongPress();
                    view.setPressed(false);
                    if (view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return horizontalSwipe;
            }

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                boolean consumed = horizontalSwipe;
                float velocityX = 0f;
                float velocityY = 0f;
                if (velocityTracker != null) {
                    velocityTracker.computeCurrentVelocity(1000);
                    velocityX = velocityTracker.getXVelocity();
                    velocityY = velocityTracker.getYVelocity();
                }
                if (action == MotionEvent.ACTION_UP && horizontalSwipe) {
                    boolean enoughDistance = Math.abs(dx) >= dp(main, 42);
                    boolean enoughVelocity = Math.abs(velocityX) >= dp(main, 520)
                            && Math.abs(velocityX) > Math.abs(velocityY) * 1.15f;
                    if ((enoughDistance || enoughVelocity) && Math.abs(dx) > Math.abs(dy) * 1.1f) {
                        switchGroup(dx < 0 ? 1 : -1);
                    }
                }
                recycleVelocityTracker();
                horizontalSwipe = false;
                if (view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(false);
                return consumed;
            }
            return horizontalSwipe;
        });
    }

    private void switchGroup(int direction) {
        if (groupRow == null || groupRow.getChildCount() == 0 || list == null) return;
        int active = 0;
        for (int i = 0; i < groupRow.getChildCount(); i++) {
            if (groupRow.getChildAt(i).isSelected()) { active = i; break; }
        }
        int count = groupRow.getChildCount();
        int next = Math.max(0, Math.min(count - 1, active + direction));
        if (next == active) return;

        View target = groupRow.getChildAt(next);
        target.performClick();

        list.animate().cancel();
        list.setTranslationX(direction > 0 ? dp(activity, 12) : -dp(activity, 12));
        list.setAlpha(.9f);
        list.animate().translationX(0f).alpha(1f).setDuration(130).start();

        View parent = (View) groupRow.getParent();
        if (parent instanceof HorizontalScrollView) {
            int viewport = parent.getWidth();
            int x = Math.max(0, target.getLeft() - Math.max(dp(activity, 16), (viewport - target.getWidth()) / 2));
            parent.post(() -> ((HorizontalScrollView) parent).smoothScrollTo(x, 0));
        }
    }

    private void recycleVelocityTracker() {
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private void detach() {
        recycleVelocityTracker();
        if (list != null) list.setOnTouchListener(null);
        activity = null;
        list = null;
        groupRow = null;
        horizontalSwipe = false;
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
