package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.lang.reflect.Method;

/** Mobile-only IPTV navigation refinement. Keeps existing MainActivity behavior intact. */
public final class MobileIptvUi {
    private static final int TOOLBAR_BG = Color.rgb(23, 23, 28);

    private MobileIptvUi() {}

    public static void install(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        View rootView = activity.findViewById(R.id.mainRoot);
        if (!(rootView instanceof LinearLayout)) return;
        LinearLayout root = (LinearLayout) rootView;
        if (root.getTag(R.id.mainRoot) != null && "nm7_toolbar_v2".equals(root.getTag(R.id.mainRoot).toString())) return;
        root.setTag(R.id.mainRoot, "nm7_toolbar_v2");

        View importPanel = activity.findViewById(R.id.importPanel);
        if (importPanel != null) importPanel.setVisibility(View.GONE);

        EditText search = activity.findViewById(R.id.inputSearch);
        Button reload = activity.findViewById(R.id.btnReloadUrl);
        Button options = activity.findViewById(R.id.btnWallpaper);
        View clear = activity.findViewById(R.id.btnClearFilters);
        View all = activity.findViewById(R.id.btnAllChannels);
        View favorites = activity.findViewById(R.id.btnFavorites);
        View recent = activity.findViewById(R.id.btnRecent);
        if (search == null || reload == null || options == null) return;

        ViewGroup searchParent = search.getParent() instanceof ViewGroup ? (ViewGroup) search.getParent() : null;
        ViewGroup reloadParent = reload.getParent() instanceof ViewGroup ? (ViewGroup) reload.getParent() : null;
        ViewGroup optionsParent = options.getParent() instanceof ViewGroup ? (ViewGroup) options.getParent() : null;
        if (searchParent != null) searchParent.removeView(search);
        if (reloadParent != null) reloadParent.removeView(reload);
        if (optionsParent != null) optionsParent.removeView(options);
        if (clear != null) clear.setVisibility(View.GONE);
        if (all != null) all.setVisibility(View.GONE);
        if (favorites != null) favorites.setVisibility(View.GONE);
        if (recent != null) recent.setVisibility(View.GONE);

        if (searchParent != null && searchParent.getChildCount() == 0) searchParent.setVisibility(View.GONE);
        if (reloadParent != null && reloadParent.getChildCount() == 0) reloadParent.setVisibility(View.GONE);

        LinearLayout toolbar = new LinearLayout(activity);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setBackgroundColor(TOOLBAR_BG);
        toolbar.setPadding(dp(activity, 4), dp(activity, 3), dp(activity, 4), dp(activity, 3));

        search.setSingleLine(true);
        search.setTextSize(14);
        search.setHint("Tìm kênh…");
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(0, dp(activity, 46), 1f);
        searchLp.setMarginEnd(dp(activity, 5));
        toolbar.addView(search, searchLp);

        styleButton(reload, "↻", "Tải lại kênh");
        toolbar.addView(reload, new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 46)));

        LinearLayout.LayoutParams optionLp = new LinearLayout.LayoutParams(dp(activity, 52), dp(activity, 46));
        optionLp.setMarginStart(dp(activity, 5));
        styleButton(options, "⚙", "Tùy chọn");
        toolbar.addView(options, optionLp);

        int insertAt = Math.min(1, root.getChildCount());
        root.addView(toolbar, insertAt);

        options.setOnClickListener(v -> showNavigationOptions(activity, all, favorites, recent));
    }

    private static void styleButton(Button button, String text, String description) {
        button.setText(text);
        button.setContentDescription(description);
        button.setTextSize(20);
        button.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, 0, 0, 0);
    }

    private static void showNavigationOptions(Activity activity, View all, View favorites, View recent) {
        String[] items = {"Tất cả kênh", "★ Yêu thích", "◷ Gần đây", "Tùy chọn ứng dụng…"};
        new AlertDialog.Builder(activity)
                .setTitle("Tùy chọn IPTV")
                .setItems(items, (dialog, which) -> {
                    if (which == 0 && all != null) all.performClick();
                    else if (which == 1 && favorites != null) favorites.performClick();
                    else if (which == 2 && recent != null) recent.performClick();
                    else if (which == 3) invokeMainSettings(activity);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private static void invokeMainSettings(Activity activity) {
        try {
            Method method = MainActivity.class.getDeclaredMethod("showSettings");
            method.setAccessible(true);
            method.invoke(activity);
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
