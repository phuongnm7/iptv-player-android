package vn.phuong.iptvplayer.movie;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.util.List;

public final class MoviePluginManagerActivity extends Activity {
    private LinearLayout list;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        MoviePluginManager.registerDefaults();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10,10,13));

        TextView title = new TextView(this);
        title.setText("Quản lý plugin phim");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(16), dp(8), dp(16), dp(8));
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView info = new TextView(this);
        info.setText("Bật plugin để nguồn phim xuất hiện trong Movie. NovaHD là plugin mặc định.");
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(14);
        info.setPadding(dp(16), 0, dp(16), dp(12));
        root.addView(info);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        Button back = new Button(this);
        back.setText("Quay lại Movie");
        back.setOnClickListener(v -> finish());
        root.addView(back, new LinearLayout.LayoutParams(-1, dp(52)));

        setContentView(root);
        render();
    }

    private void render() {
        list.removeAllViews();
        List<MoviePlugin> plugins = MoviePluginManager.all();
        if (plugins.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Chưa có plugin.");
            empty.setTextColor(Color.LTGRAY);
            empty.setPadding(dp(16), dp(16), dp(16), dp(16));
            list.addView(empty);
            return;
        }

        for (MoviePlugin plugin : plugins) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(10), dp(16), dp(10));

            LinearLayout text = new LinearLayout(this);
            text.setOrientation(LinearLayout.VERTICAL);
            TextView name = new TextView(this);
            name.setText(plugin.name());
            name.setTextColor(Color.WHITE);
            name.setTextSize(17);
            TextView id = new TextView(this);
            id.setText("ID: " + plugin.id());
            id.setTextColor(Color.GRAY);
            id.setTextSize(12);
            text.addView(name);
            text.addView(id);

            Switch enabled = new Switch(this);
            enabled.setChecked(MoviePluginManager.isEnabled(this, plugin.id()));
            enabled.setOnCheckedChangeListener((button, checked) ->
                    MoviePluginManager.setEnabled(this, plugin.id(), checked));

            row.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(enabled, new LinearLayout.LayoutParams(-2, -2));
            list.addView(row);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
