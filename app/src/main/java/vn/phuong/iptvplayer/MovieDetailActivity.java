package vn.phuong.iptvplayer;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;
import java.util.List;
import vn.phuong.iptvplayer.movie.*;

public final class MovieDetailActivity extends Activity {
    private MoviePlugin plugin;
    private String contentId;
    private LinearLayout root;
    @Override protected void onCreate(Bundle b){
        super.onCreate(b); MoviePluginManager.registerDefaults();
        String[] parts=MoviePluginManager.splitContentId(getIntent().getStringExtra("content_id"));
        plugin=MoviePluginManager.find(parts[0]); contentId=parts[1]; build(); load();
    }
    private void build(){ScrollView s=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(16),dp(16),dp(80));root.setBackgroundColor(Color.rgb(12,12,16));s.addView(root);setContentView(s);HomeTabBar.attach(this,HomeTabBar.TAB_MOVIE);}
    private void load(){root.removeAllViews();TextView t=txt("Đang tải...",16,true);root.addView(t);if(plugin==null)return;plugin.detail(this,contentId,new MoviePlugin.Callback(){public void onSuccess(Object d){runOnUiThread(()->show((MovieContent.Detail)d));}public void onError(Throwable e){runOnUiThread(()->err(e));}});}
    private void show(MovieContent.Detail d){root.removeAllViews();if(d==null||d.item==null){err(null);return;}MovieContent.Item i=d.item;root.addView(txt(i.title,24,true));root.addView(txt((i.year.isEmpty()?"":i.year+"  ")+(i.rating.isEmpty()?"":"★ "+i.rating),15,false),new LinearLayout.LayoutParams(-1,dp(42)));if(!i.genres.isEmpty())root.addView(txt(android.text.TextUtils.join(" • ",i.genres),14,false));if(!d.director.isEmpty())root.addView(txt("Đạo diễn: "+d.director,14,false));if(!i.overview.isEmpty())root.addView(txt(i.overview,15,false));Space sp=new Space(this);root.addView(sp,new LinearLayout.LayoutParams(1,dp(18)));if(d.seasons.isEmpty()) addPlay("▶ Xem phim",null,null);else for(MovieContent.Season s:d.seasons){root.addView(txt(s.name,19,true),new LinearLayout.LayoutParams(-1,dp(44)));for(MovieContent.Episode e:s.episodes)addPlay("Tập "+e.number+" — "+e.name,String.valueOf(s.number),String.valueOf(e.number));}}
    private void addPlay(String label,String season,String episode){Button b=new Button(this);b.setText(label);b.setOnClickListener(v->resolve(season,episode));root.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));}
    private void resolve(String season,String episode){if(plugin==null)return;Toast.makeText(this,"Đang lấy nguồn phát...",Toast.LENGTH_SHORT).show();plugin.sources(this,contentId,season,episode,new MoviePlugin.Callback(){public void onSuccess(Object d){List<MovieContent.Source>s=(List<MovieContent.Source>)d;if(s==null||s.isEmpty()){runOnUiThread(()->err(new IllegalStateException("Không có nguồn phát")));return;}MovieContent.Source best=pick(s);runOnUiThread(()->play(best));}public void onError(Throwable e){runOnUiThread(()->err(e));}});}
    private MovieContent.Source pick(List<MovieContent.Source>s){MovieContent.Source best=s.get(0);for(MovieContent.Source x:s){if(score(x)>score(best))best=x;}return best;}
    private int score(MovieContent.Source s){String q=s.quality==null?"":s.quality;return q.contains("2160")?4:q.contains("4K")?4:q.contains("1080")?3:q.contains("720")?2:1;}
    private void play(MovieContent.Source s){Toast.makeText(this,"Mở nguồn "+(s.quality.isEmpty()?"":s.quality),Toast.LENGTH_SHORT).show();android.content.Intent i=new android.content.Intent(this,PlayerActivity.class);i.putExtra("url",s.url);i.putExtra("name","Movie");i.putExtra("mime",s.mime);SharedPlaybackSession.setTab(this,SharedPlaybackSession.TAB_MOVIE);startActivity(i);}
    private TextView txt(String s,float z,boolean b){TextView v=new TextView(this);v.setText(s);v.setTextColor(Color.WHITE);v.setTextSize(z);v.setTypeface(Typeface.DEFAULT,b?Typeface.BOLD:Typeface.NORMAL);v.setPadding(0,dp(6),0,dp(6));return v;}
    private void err(Throwable e){root.removeAllViews();root.addView(txt("Không tải được: "+(e==null?"":e.getMessage()),15,false));}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}