package vn.phuong.iptvplayer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;

/** Shared top-level NM7 tabs used by both the IPTV home and the YouTube screen. */
public final class HomeTabBar {
    private static final int TAG_KEY = 0x4E4D3701;

    private HomeTabBar() {}

    public static void attach(Activity activity, LinearLayout root, boolean youtubeSelected) {
        if (root == null || root.getTag(TAG_KEY) != null) return;
        root.setTag(TAG_KEY, Boolean.TRUE);

        LinearLayout tabs = new LinearLayout(activity);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER_VERTICAL);
        tabs.setPadding(dp(activity, 2), dp(activity, 2), dp(activity, 2), dp(activity, 6));

        Button iptv = tabButton(activity, "IPTV", !youtubeSelected);
        Button youtube = tabButton(activity, "YouTube", youtubeSelected);

        LinearLayout.LayoutParams iptvLp = new LinearLayout.LayoutParams(0, dp(activity, 46), 1f);
        iptvLp.setMarginEnd(dp(activity, 6));
        tabs.addView(iptv, iptvLp);
        tabs.addView(youtube, new LinearLayout.LayoutParams(0, dp(activity, 46), 1f));

        iptv.setOnClickListener(v -> {
            if (youtubeSelected) activity.finish();
        });
        youtube.setOnClickListener(v -> {
            if (!youtubeSelected) {
                Intent intent = new Intent(activity, YoutubeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                activity.startActivity(intent);
            }
        });

        root.addView(tabs, 0, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private static Button tabButton(Activity activity, String text, boolean selected) {
        Button b = new Button(activity);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(activity, 4), 0, dp(activity, 4), 0);
        b.setTextColor(selected ? Color.WHITE : Color.LTGRAY);
        b.setBackgroundResource(selected ? R.drawable.button_primary : R.drawable.button_secondary);
        return b;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
