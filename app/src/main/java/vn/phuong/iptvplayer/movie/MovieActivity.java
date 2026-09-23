package vn.phuong.iptvplayer.movie;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import vn.phuong.iptvplayer.HomeTabBar;
import vn.phuong.iptvplayer.SharedPlaybackSession;

public final class MovieActivity extends Activity {
    private LinearLayout root;
    private EditText search;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        MoviePluginManager.registerDefaults();
        SharedPlaybackSession.setTab(this, SharedPlaybackSession.TAB_MOVIE);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(15,15,18));
        search = new EditText(this);
        search.setHint("Tìm phim...");
        search.setSingleLine(true);
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.GRAY);
        root.addView(search, new LinearLayout.LayoutParams(-1, dp(52)));
        setContentView(root);
        HomeTabBar.attach(this, HomeTabBar.TAB_MOVIE);
        search.setOnEditorActionListener((v,a,e)->{ loadSearch(v.getText().toString().trim()); return true; });
        loadHome();
    }
    private void loadHome() {
        MoviePlugin p = firstPlugin();
        if(p==null){ showError("Chưa có nguồn phim"); return; }
        p.home(this,new MoviePlugin.Callback(){
            public void onSuccess(Object data){ runOnUiThread(()->renderSections((List<MovieContent.Section>)data)); }
            public void onError(Throwable e){ runOnUiThread(()->showError("Không tải được danh sách phim: "+e.getMessage())); }
        });
    }
    private void loadSearch(String q) {
        if(q.isEmpty()){ loadHome(); return; }
        MoviePlugin p=firstPlugin(); if(p==null)return;
        p.search(this,q,new MoviePlugin.Callback(){
            public void onSuccess(Object data){ runOnUiThread(()->{ root.removeViews(1,Math.max(0,root.getChildCount()-1)); addSection("Kết quả tìm kiếm",(List<MovieContent.Item>)data); }); }
            public void onError(Throwable e){ runOnUiThread(()->showError("Tìm kiếm lỗi: "+e.getMessage())); }
        });
    }
    private void renderSections(List<MovieContent.Section> sections){
        root.removeViews(1,Math.max(0,root.getChildCount()-1));
        for(MovieContent.Section s:sections) addSection(s.title,s.items);
    }
    private void addSection(String title,List<MovieContent.Item> items){
        TextView h=new TextView(this); h.setText(title); h.setTextColor(Color.WHITE); h.setTextSize(19); h.setGravity(Gravity.CENTER_VERTICAL); h.setPadding(dp(12),dp(8),dp(8),dp(4));
        root.addView(h,new LinearLayout.LayoutParams(-1,dp(44)));
        HorizontalScrollView scroll=new HorizontalScrollView(this); scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row=new LinearLayout(this); row.setPadding(dp(8),0,dp(8),dp(8));
        for(MovieContent.Item item:items) row.addView(card(item));
        scroll.addView(row); root.addView(scroll,new LinearLayout.LayoutParams(-1,dp(190)));
    }
    private View card(MovieContent.Item item){
        TextView v=new TextView(this); v.setText((item.title==null?"":item.title)+"\n"+(item.year==null?"":item.year)); v.setTextColor(Color.WHITE); v.setTextSize(14); v.setGravity(Gravity.BOTTOM); v.setPadding(dp(8),dp(8),dp(8),dp(8)); v.setBackgroundColor(Color.rgb(38,38,44));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(145),dp(178)); lp.setMargins(dp(4),0,dp(4),0); v.setLayoutParams(lp);
        v.setOnClickListener(x->{Intent i=new Intent(this,MovieDetailActivity.class); i.putExtra("plugin",firstPlugin().id()); i.putExtra("id",item.id); i.putExtra("type",item.type); startActivity(i);});
        return v;
    }
    private MoviePlugin firstPlugin(){List<MoviePlugin> ps=MoviePluginManager.all(); return ps.isEmpty()?null:ps.get(0);}
    private void showError(String s){ TextView v=new TextView(this); v.setText(s); v.setTextColor(Color.LTGRAY); v.setPadding(dp(16),dp(12),dp(16),dp(12)); root.addView(v); }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
