package vn.phuong.iptvplayer.movie;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import vn.phuong.iptvplayer.HomeTabBar;
import vn.phuong.iptvplayer.SharedPlaybackSession;

public final class MovieActivity extends Activity {
    private LinearLayout content;
    private EditText search;
    private ProgressBar progress;
    private MoviePlugin activePlugin;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        MoviePluginManager.registerDefaults();
        SharedPlaybackSession.setTab(this, SharedPlaybackSession.TAB_MOVIE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10,10,13));

        search = new EditText(this);
        search.setHint("🔎  Tìm phim, tên diễn viên...");
        search.setSingleLine(true);
        search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setTextColor(Color.WHITE);
        search.setHintTextColor(Color.GRAY);
        search.setPadding(dp(14),0,dp(14),0);
        root.addView(search,new LinearLayout.LayoutParams(-1,dp(52)));

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        root.addView(progress,new LinearLayout.LayoutParams(-1,dp(4)));

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0,dp(6),0,dp(72));
        scroll.addView(content);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        setContentView(root);
        HomeTabBar.attach(this,HomeTabBar.TAB_MOVIE);

        search.setOnEditorActionListener((v,a,e)->{
            String q=v.getText().toString().trim();
            if(q.isEmpty()) loadHome(); else loadSearch(q);
            return true;
        });
        loadHome();
    }

    private void loadHome() {
        activePlugin=firstPlugin();
        if(activePlugin==null){showError("Chưa có plugin phim.");return;}
        setLoading(true);
        activePlugin.home(this,new MoviePlugin.Callback(){
            public void onSuccess(Object data){runOnUiThread(()->{setLoading(false);renderSections((List<MovieContent.Section>)data);});}
            public void onError(Throwable e){runOnUiThread(()->{setLoading(false);showError("Không tải được phim: "+safe(e));});}
        });
    }

    private void loadSearch(String q) {
        activePlugin=firstPlugin();
        if(activePlugin==null)return;
        setLoading(true);
        activePlugin.search(this,q,new MoviePlugin.Callback(){
            public void onSuccess(Object data){runOnUiThread(()->{setLoading(false);content.removeAllViews();addSection("Kết quả tìm kiếm", (List<MovieContent.Item>)data);});}
            public void onError(Throwable e){runOnUiThread(()->{setLoading(false);showError("Tìm kiếm lỗi: "+safe(e));});}
        });
    }

    private void renderSections(List<MovieContent.Section> sections) {
        content.removeAllViews();
        if(sections==null||sections.isEmpty()){showError("Không có dữ liệu phim.");return;}
        for(MovieContent.Section s:sections)addSection(s.title,s.items);
    }

    private void addSection(String title,List<MovieContent.Item> items) {
        if(items==null||items.isEmpty())return;
        TextView h=label(title,19);
        content.addView(h,new LinearLayout.LayoutParams(-1,dp(44)));

        HorizontalScrollView scroll=new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row=new LinearLayout(this);
        row.setPadding(dp(8),0,dp(8),dp(8));
        for(MovieContent.Item item:items)row.addView(card(item));
        scroll.addView(row);
        content.addView(scroll,new LinearLayout.LayoutParams(-1,dp(224)));
    }

    private View card(MovieContent.Item item) {
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setFocusable(true);
        box.setClickable(true);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(Color.rgb(30,30,36));
        bg.setCornerRadius(dp(6));
        box.setBackground(bg);

        ImageView poster=new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        box.addView(poster,new LinearLayout.LayoutParams(dp(145),dp(178)));
        loadImage(item.poster,poster);

        TextView title=new TextView(this);
        title.setText((item.title==null||item.title.isEmpty())?"Không tên":item.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(13);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setPadding(dp(7),dp(5),dp(7),dp(2));
        box.addView(title,new LinearLayout.LayoutParams(dp(145),dp(38)));

        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(145),dp(218));
        lp.setMargins(dp(4),0,dp(4),0);
        box.setLayoutParams(lp);
        box.setOnClickListener(v->openDetail(item));

        return box;
    }

    private void openDetail(MovieContent.Item item) {
        MoviePlugin p=activePlugin!=null?activePlugin:firstPlugin();
        if(p==null)return;
        Intent i=new Intent(this,MovieDetailActivity.class);
        i.putExtra("plugin",p.id());
        i.putExtra("id",item.id);
        i.putExtra("type",item.type);
        startActivity(i);
    }

    private MoviePlugin firstPlugin(){List<MoviePlugin> ps=MoviePluginManager.all();return ps.isEmpty()?null:ps.get(0);}

    private TextView label(String s,float size){
        TextView v=new TextView(this);
        v.setText(s==null?"":s);
        v.setTextColor(Color.WHITE);
        v.setTextSize(size);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setPadding(dp(12),dp(7),dp(8),0);
        return v;
    }

    private void showError(String s){
        TextView v=label(s,15);
        v.setTextColor(Color.LTGRAY);
        content.removeAllViews();
        content.addView(v,new LinearLayout.LayoutParams(-1,-2));
    }

    private void setLoading(boolean loading){progress.setVisibility(loading?View.VISIBLE:View.GONE);}

    private String safe(Throwable e){return e==null?"lỗi không xác định":(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}

    private void loadImage(String url,ImageView target){
        if(url==null||url.isEmpty()){target.setImageResource(android.R.drawable.ic_menu_report_image);return;}
        new Thread(()->{
            try{
                HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
                c.setConnectTimeout(8000);c.setReadTimeout(12000);
                c.setRequestProperty("User-Agent","NM7-Mobile-Movie/1.10.75");
                InputStream in=c.getInputStream();
                Bitmap b=BitmapFactory.decodeStream(in);
                in.close();c.disconnect();
                if(b!=null)runOnUiThread(()->target.setImageBitmap(b));
            }catch(Throwable ignored){}
        }).start();
    }

    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
