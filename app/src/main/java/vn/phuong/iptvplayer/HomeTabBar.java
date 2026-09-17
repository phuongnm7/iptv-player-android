package vn.phuong.iptvplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

/** Main Mobile navigation: exactly two top-level sections, YouTube and IPTV. */
public final class HomeTabBar {
    /* Use a real application resource id for View.setTag(int, Object). */
    private static final int TAG_KEY = R.id.mainRoot;
    private static final int BG = Color.rgb(23, 23, 28);
    private static final int SELECTED = Color.rgb(255, 122, 0);
    private static final int UNSELECTED = Color.rgb(135, 137, 145);

    private HomeTabBar() {}

    public static void attach(Activity activity, LinearLayout root, boolean youtubeSelected) {
        if (root == null || root.getTag(TAG_KEY) != null) return;
        root.setTag(TAG_KEY, Boolean.TRUE);

        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackground(new ColorDrawable(BG));
        bar.setPadding(0, dp(activity, 4), 0, dp(activity, 4));

        addItem(activity, bar, R.drawable.nm7_nav_youtube, youtubeSelected, () -> {
            if (!youtubeSelected) {
                Intent intent = new Intent(activity, SmartTubeHomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            }
        });

        addItem(activity, bar, R.drawable.nm7_nav_iptv, !youtubeSelected, () -> {
            if (youtubeSelected) {
                Intent intent = new Intent(activity, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
                activity.finish();
            }
        });

        root.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 64)));
    }

    private static void addItem(Activity activity, LinearLayout bar, int iconRes, boolean selected, Runnable action) {
        LinearLayout item = new LinearLayout(activity);
        item.setGravity(Gravity.CENTER);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(v -> action.run());

        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconRes);
        icon.setImageTintList(ColorStateList.valueOf(selected ? SELECTED : UNSELECTED));
        icon.setContentDescription(null);
        int size = dp(activity, 32);
        item.addView(icon, new LinearLayout.LayoutParams(size, size));

        bar.addView(item, new LinearLayout.LayoutParams(0, dp(activity, 56), 1f));
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
