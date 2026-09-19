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

/** Single Mobile navigation bar, overlaid at the bottom of NM7, player, and SmartTube Browse. */
public final class HomeTabBar {
    private static final int TAG_KEY = R.id.mainRoot;
    private static final int BG = Color.rgb(23, 23, 28);
    private static final int SELECTED = Color.rgb(255, 122, 0);
    private static final int UNSELECTED = Color.rgb(135, 137, 145);
    private static final String BROWSE = "com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity";

    private HomeTabBar() {}

    public static void attach(Activity activity, boolean youtubeSelected) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof FrameLayout)) return;
        FrameLayout host = (FrameLayout) content;
        if (host.getTag(TAG_KEY) != null) return;
        host.setTag(TAG_KEY, Boolean.TRUE);

        LinearLayout bar = new LinearLayout(activity);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setBackground(new ColorDrawable(BG));
        bar.setElevation(dp(activity, 8));
        bar.setPadding(0, dp(activity, 4), 0, dp(activity, 4));

        addItem(activity, bar, R.drawable.nm7_nav_youtube, youtubeSelected, () -> {
            if (!youtubeSelected) openBrowse(activity);
        });

        addItem(activity, bar, R.drawable.nm7_nav_iptv, !youtubeSelected, () -> {
            if (youtubeSelected) openIptv(activity);
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

    private static void startWithoutAnimation(Activity activity, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, 0);
    }

    private static void openIptv(Activity activity) {
        // Changing tabs is not the same as selecting an IPTV channel. Restore IPTV only
        // when the user had a channel playing before YouTube took ownership. Otherwise
        // keep the YouTube mini-player alive above the IPTV channel list.
        boolean resumeIptv = MobileInlinePlayerProviderV2.shouldResumeIptvAfterYoutube();
        // Leave YouTube alive while the returning IPTV channel buffers.
        PlayerActivity.cancelYoutubeHandoff();
        SharedPlaybackSession.setTab(activity, SharedPlaybackSession.TAB_IPTV);
        MobileNm7Application.markTabSwitch();
        Intent intent;
        if (resumeIptv && !MobileInlinePlayerProviderV2.hasSession()
                && (MobileNm7Application.hasIptvPlayer() || SharedPlaybackSession.loadIptv(activity) != null)) {
            intent = new Intent(activity, PlayerActivity.class);
        } else {
            intent = new Intent(activity, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startWithoutAnimation(activity, intent);
    }

    private static void openBrowse(Activity activity) {
        SharedPlaybackSession.setTab(activity, SharedPlaybackSession.TAB_YOUTUBE);
        MobileNm7Application.markTabSwitch();
        PlayerActivity.prepareForYoutubeHandoff(activity);
        // Do NOT finish PlayerActivity here. Selecting the YouTube tab is only navigation;
        // IPTV must continue playing until SmartTube PlaybackActivity actually starts a video.
        // MobileNm7Application pauses/releases IPTV at that exact playback-start event.
        SmartTubeRuntime.initialize(activity.getApplicationContext());
        try {
            Class<?> browse = Class.forName(BROWSE);
            Intent intent = new Intent(activity, browse);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startWithoutAnimation(activity, intent);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            Intent fallback = new Intent(activity, SmartTubeHomeActivity.class);
            fallback.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startWithoutAnimation(activity, fallback);
        }
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
        icon.setColorFilter(selected ? SELECTED : UNSELECTED);
        icon.setContentDescription(null);
        int size = dp(activity, 30);
        item.addView(icon, new LinearLayout.LayoutParams(size, size));

        TextView label = new TextView(activity);
        label.setText(iconRes == R.drawable.nm7_nav_youtube ? "YouTube" : "IPTV");
        label.setTextSize(11);
        label.setGravity(Gravity.CENTER);
        label.setTextColor(selected ? Color.WHITE : UNSELECTED);
        label.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        item.addView(label, new LinearLayout.LayoutParams(-2, dp(activity, 18)));

        bar.addView(item, new LinearLayout.LayoutParams(0, dp(activity, 56), 1f));
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
