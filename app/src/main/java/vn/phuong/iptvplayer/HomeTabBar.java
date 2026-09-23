package vn.phuong.iptvplayer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import vn.phuong.iptvplayer.movie.MovieActivity;

public final class HomeTabBar {
    public static final int TAB_YOUTUBE = 0;
    public static final int TAB_IPTV = 1;
    public static final int TAB_MOVIE = 2;
    private static final int TAG_KEY = R.id.mainRoot;
    private static final String BAR_TAG = "nm7_home_tab_bar";
    private static final int BG = Color.rgb(23, 23, 28);
    private static final int SELECTED = Color.rgb(255, 122, 0);
    private static final int UNSELECTED = Color.rgb(135, 137, 145);
    private static final String BROWSE = "com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity";

    private HomeTabBar() {}

    public static void attach(Activity activity, boolean youtubeSelected) { attach(activity, youtubeSelected ? TAB_YOUTUBE : TAB_IPTV); }

    public static void attach(Activity activity, int selectedTab) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) return;
        FrameLayout host = (FrameLayout) content;
        if (host.getTag(TAG_KEY) != null) return;
        host.setTag(TAG_KEY, Boolean.TRUE);

        LinearLayout bar = new LinearLayout(activity);
        bar.setTag(BAR_TAG);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackground(new ColorDrawable(selectedTab == TAB_YOUTUBE ? Color.WHITE : BG));
        bar.setElevation(dp(activity, 8));
        bar.setPadding(0, dp(activity, 4), 0, dp(activity, 4));

        addItem(activity, bar, R.drawable.nm7_nav_youtube, selectedTab == TAB_YOUTUBE, "YouTube", () -> {
            if (selectedTab != TAB_YOUTUBE) openBrowse(activity);
        });

        addItem(activity, bar, R.drawable.nm7_nav_iptv, selectedTab == TAB_IPTV, "IPTV", () -> {
            if (selectedTab != TAB_IPTV) openIptv(activity);
        });

        addItem(activity, bar, R.drawable.nm7_nav_library, selectedTab == TAB_MOVIE, "Movie", () -> {
            if (selectedTab != TAB_MOVIE) openMovie(activity);
        });

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 64), Gravity.BOTTOM);
        host.addView(bar, lp);
        if (activity instanceof MainActivity) {
            View root = activity.findViewById(R.id.mainRoot);
            if (root != null) {
                root.setPadding(root.getPaddingLeft(), root.getPaddingTop(), root.getPaddingRight(),
                        root.getPaddingBottom() + dp(activity, 64));
            }
        }
    }

    public static void setVisible(Activity activity, boolean visible) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) content;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (BAR_TAG.equals(child.getTag())) {
                child.setVisibility(visible ? View.VISIBLE : View.GONE);
                return;
            }
        }
    }

    private static void startWithoutAnimation(Activity activity, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    private static void openMovie(Activity activity) {
        SharedPlaybackSession.setTab(activity, SharedPlaybackSession.TAB_MOVIE);
        Intent intent = new Intent(activity, MovieActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startWithoutAnimation(activity, intent);
    }

    private static void openIptv(Activity activity) {
        PlayerActivity.cancelYoutubeHandoff();
        SharedPlaybackSession.setTab(activity, SharedPlaybackSession.TAB_IPTV);
        MobileNm7Application.markTabSwitch();
        Intent intent = new Intent(activity, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startWithoutAnimation(activity, intent);
        activity.getWindow().getDecorView().postDelayed(
                () -> MobileInlinePlayerProviderV2.resumeForIptvTab(activity), 180L);
    }

    private static void openBrowse(Activity activity) {
        boolean hasIptvPlayer = MobileNm7Application.hasIptvPlayer();
        if (hasIptvPlayer) MobileNm7Application.markTabSwitch();

        // A previous playback session must never be restored merely because its
        // static flag is still set. A stale session was causing the YouTube tab to
        // immediately return to IPTV. First bring the real BrowseActivity forward.
        if (MobileNm7Application.bringSmartTubeToFront()) {
            SharedPlaybackSession.setTab(activity, SharedPlaybackSession.TAB_YOUTUBE);
            return;
        }

        if (hasIptvPlayer) PlayerActivity.prepareForYoutubeHandoff(activity);

        if (SmartTubeRuntime.openBrowse(activity.getApplicationContext())) {
            SharedPlaybackSession.setTab(activity, SharedPlaybackSession.TAB_YOUTUBE);
        } else {
            PlayerActivity.cancelYoutubeHandoff();
            SharedPlaybackSession.setTab(activity, SharedPlaybackSession.TAB_IPTV);
            android.util.Log.e("NM7Navigation", "SmartTube Browse failed to start");
        }
    }

    private static void addItem(Activity activity, LinearLayout bar, int iconRes, boolean selected, String title, Runnable action) {
        LinearLayout item = new LinearLayout(activity);
        item.setGravity(Gravity.CENTER);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(v -> action.run());

        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconRes);
        boolean light = activity.getClass().getName().equals(BROWSE);
        icon.setColorFilter(light ? (selected ? Color.BLACK : 0xff606060) : (selected ? SELECTED : UNSELECTED));
        icon.setContentDescription(null);
        int size = dp(activity, 30);
        item.addView(icon, new LinearLayout.LayoutParams(size, size));

        TextView label = new TextView(activity);
        label.setText(title);
        item.setContentDescription(title);
        label.setTextSize(11);
        label.setGravity(Gravity.CENTER);
        label.setTextColor(light ? (selected ? Color.BLACK : 0xff606060) : (selected ? Color.WHITE : UNSELECTED));
        label.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        item.addView(label, new LinearLayout.LayoutParams(-2, dp(activity, 18)));

        bar.addView(item, new LinearLayout.LayoutParams(0, dp(activity, 56), 1f));
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
