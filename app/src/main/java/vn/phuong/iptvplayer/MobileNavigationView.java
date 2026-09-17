package vn.phuong.iptvplayer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * Static Mobile-only navigation bar. It is declared directly in activity_main.xml
 * so it does not depend on Application ActivityLifecycleCallbacks timing.
 */
public final class MobileNavigationView extends LinearLayout {
    public MobileNavigationView(Context context) {
        super(context);
        init(context);
    }

    public MobileNavigationView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MobileNavigationView(Context context, android.util.AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        setBackground(new ColorDrawable(Color.rgb(23, 23, 28)));
        setPadding(0, dp(context, 4), 0, dp(context, 4));
        post(() -> buildItems(context));
    }

    private void buildItems(Context context) {
        if (getChildCount() != 0) return;
        addItem(context, R.drawable.nm7_nav_youtube, false, v -> {
            if (!(context instanceof Activity)) return;
            Activity activity = (Activity) context;
            android.content.Intent intent = new android.content.Intent(activity, SmartTubeHomeActivity.class);
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            activity.startActivity(intent);
        });
        addItem(context, R.drawable.nm7_nav_iptv, true, v -> { });
    }

    private void addItem(Context context, int iconRes, boolean selected, View.OnClickListener listener) {
        LinearLayout item = new LinearLayout(context);
        item.setOrientation(VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setFocusable(true);
        item.setOnClickListener(listener);

        ImageView icon = new ImageView(context);
        icon.setImageResource(iconRes);
        icon.setColorFilter(Color.rgb(255, 122, 0));
        icon.setContentDescription(null);
        item.addView(icon, new LinearLayout.LayoutParams(dp(context, 32), dp(context, 32)));

        addView(item, new LinearLayout.LayoutParams(0, dp(context, 56), 1f));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
