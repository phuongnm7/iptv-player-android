package vn.phuong.iptvplayer.movie;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;
import vn.phuong.iptvplayer.PlayerActivity;
import vn.phuong.iptvplayer.SharedPlaybackSession;

public final class MovieDetailActivity extends Activity {
    private LinearLayout root;
    private MoviePlugin plugin; private String contentId;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        MoviePluginManager.registerDefaults();
        SharedPlaybackSession.setTab(this,SharedPlaybackSession.TAB_MOVIE);
        plugin=MoviePluginManager.find(getIntent().getStringExtra("plugin"));
        String id=getIntent().getStringExtra("id"), type=getIntent().getStringExtra("type");
        contentId=MoviePluginManager.contentId(plugin==null?"":plugin.id(),(type==null||!"show".equals(type)?"movie:":"show:")+id);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(15,15,18)); setContentView(root);
        if(plugin==null){ text("Không tìm thấy nguồn phim"); return; }
        plugin.detail(this,contentId.substring(contentId.indexOf(':')+1),new MoviePlugin.Callback(){
            public void onSuccess(Object d){runOnUiThread(()->render((MovieContent.Detail)d));}
            public void onError(Throwable e){runOnUiThread(()->text("Không tải được thông tin: "+e.getMessage()));}
        });
    }
    private void render(MovieContent.Detail d){
        if(d==null||d.item==null){text("Không có dữ liệu");return;}
        text(d.item.title+"  "+d.item.year);
        text("⭐ "+d.item.rating+"   "+String.join(", ",d.item.genres));
        text(d.overviewSafe());
        if(d.seasons.isEmpty()){ addPlay(d.item.id,"",""); return; }
        for(MovieContent.Season s:d.seasons){
            text(s.name);
            for(MovieContent.Episode e:s.episodes) addPlay(d.item.id,String.valueOf(s.number),String.valueOf(e.number));
        }
    }
    private void addPlay(String id,String season,String episode){
        TextView v=new TextView(this); v.setText("▶ Phát"+(season.isEmpty()?"":"  S"+season+" E"+episode)); v.setTextColor(Color.WHITE); v.setGravity(Gravity.CENTER_VERTICAL); v.setPadding(dp(14),0,dp(8),0); v.setBackgroundColor(Color.rgb(45,45,52));
        v.setOnClickListener(x->resolveAndPlay(id,season,episode)); root.addView(v,new LinearLayout.LayoutParams(-1,dp(52)));
    }
    private void resolveAndPlay(String id,String season,String episode){
        plugin.sources(this,contentId.substring(contentId.indexOf(':')+1),season,episode,new MoviePlugin.Callback(){
            public void onSuccess(Object data){runOnUiThread(()->{List<MovieContent.Source> s=(List<MovieContent.Source>)data; if(s.isEmpty()){text("Không có link phát");return;} MovieContent.Source best=s.get(0); for(MovieContent.Source x:s)if(score(x.quality)>score(best.quality))best=x; Intent i=new Intent(MovieDetailActivity.this,PlayerActivity.class); i.putExtra(PlayerActivity.EXTRA_NAME,"Movie"); i.putExtra(PlayerActivity.EXTRA_URL,best.url); i.putExtra(PlayerActivity.EXTRA_MIME,best.mime); if(!best.referer.isEmpty())i.putExtra(PlayerActivity.EXTRA_REFERER,best.referer); startActivity(i);});}
            public void onError(Throwable e){runOnUiThread(()->text("Lỗi tải link phát: "+e.getMessage()));}
        });
    }
    private int score(String q){if(q==null)return 0; q=q.toLowerCase(); if(q.contains("2160")||q.contains("4k"))return 4; if(q.contains("1080"))return 3; if(q.contains("720"))return 2; return 1;}
    private void text(String s){TextView v=new TextView(this);v.setText(s==null?"":s);v.setTextColor(Color.WHITE);v.setTextSize(16);v.setPadding(dp(12),dp(8),dp(12),dp(8));root.addView(v,new LinearLayout.LayoutParams(-1,-2));}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
