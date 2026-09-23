package vn.phuong.iptvplayer;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.graphics.drawable.GradientDrawable;
import java.util.List;
import vn.phuong.iptvplayer.movie.*;

public final class MovieActivity extends Activity {
    private LinearLayout root;
    private ProgressBar loading;
    private MoviePlugin plugin;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        MoviePluginManager.registerDefaults();
        plugin = MoviePluginManager.find("novahd");
        build();
        loadHome();
    }

    private void build() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(76));
        root.setBackgroundColor(Color.rgb(12,12,16));
        scroll.addView(root);
        setContentView(scroll);
        HomeTabBar.attach(this, HomeTabBar.TAB_MOVIE);
    }

    private void loadHome() {
        root.removeAllViews();
        loading = new ProgressBar(this);
        root.addView(loading, new LinearLayout.LayoutParams(-1, dp(48)));
        if (plugin == null) return;
        plugin.home(this, new MoviePlugin.Callback() {
            public void onSuccess(Object data) { runOnUiThread(() -> showSections((List<MovieContent.Section>) data)); }
            public void onError(Throwable error) { runOnUiThread(() -> showError(error)); }
        });
    }

    private void showSections(List<MovieContent.Section> sections) {
        root.removeAllViews();
        addSearch();
        for (MovieContent.Section section : sections) addSection(section);
    }

    private void addSearch() {
        EditText search = new EditText(this);
        search.setHint("Tìm phim...");
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.GRAY);
        search.setSingleLine(true);
        search.setPadding(dp(14),0,dp(14),0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(30,30,36)); bg.setCornerRadius(dp(12));
        search.setBackground(bg);
        root.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));
        search.setOnEditorActionListener((v,a,e)->{String q=search.getText().toString().trim();if(!q.isEmpty()) doSearch(q);return true;});
        Space sp=new Space(this);root.addView(sp,new LinearLayout.LayoutParams(1,dp(12)));
    }

    private void doSearch(String q) {
        root.removeAllViews();
        TextView title=text("🔎 Kết quả: "+q,18,true); root.addView(title,new LinearLayout.LayoutParams(-1,dp(48)));
        plugin.search(this,q,new MoviePlugin.Callback(){
            public void onSuccess(Object data){runOnUiThread(()->showItems((List<MovieContent.Item>)data));}
            public void onError(Throwable e){runOnUiThread(()->showError(e));}
        });
    }

    private void addSection(MovieContent.Section s) {
        TextView title=text(s.title,18,true); root.addView(title,new LinearLayout.LayoutParams(-1,dp(44)));
        HorizontalScrollView hsv=new HorizontalScrollView(this); hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        for(MovieContent.Item item:s.items) row.addView(card(item));
        hsv.addView(row); root.addView(hsv,new LinearLayout.LayoutParams(-1,dp(250)));
        Space sp=new Space(this);root.addView(sp,new LinearLayout.LayoutParams(1,dp(10)));
    }

    private void showItems(List<MovieContent.Item> items) {
        for(MovieContent.Item i:items) root.addView(card(i));
    }

    private View card(MovieContent.Item item) {
        TextView v=text(item.title,14,false);v.setTextColor(Color.WHITE);v.setGravity(LEFT|CENTER_VERTICAL);
        v.setPadding(dp(8),0,dp(8),0);v.setMaxLines(2);
        v.setBackgroundColor(Color.rgb(30,30,36));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(170),dp(52));p.setMargins(dp(5),0,dp(5),0);
        v.setOnClickListener(x->{android.content.Intent i=new android.content.Intent(this,MovieDetailActivity.class);i.putExtra("content_id",MoviePluginManager.contentId(plugin.id(),item.type+":"+item.id));startActivity(i);});
        return v;
    }

    private TextView text(String s,float size,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTypeface(Typeface.DEFAULT,bold?Typeface.BOLD:Typeface.NORMAL);return v;}
    private void showError(Throwable e){root.removeAllViews();TextView v=text("Không tải được Movie: "+(e==null?"":e.getMessage()),15,false);v.setTextColor(Color.WHITE);root.addView(v,new LinearLayout.LayoutParams(-1,dp(80)));}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}