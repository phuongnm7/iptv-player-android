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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import vn.phuong.iptvplayer.PlayerActivity;
import vn.phuong.iptvplayer.SharedPlaybackSession;

public final class MovieDetailActivity extends Activity {
    private LinearLayout root;
    private MoviePlugin plugin;
    private String contentId;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        MoviePluginManager.registerDefaults();
        SharedPlaybackSession.setTab(this,SharedPlaybackSession.TAB_MOVIE);
        plugin=MoviePluginManager.find(getIntent().getStringExtra("plugin"));
        String id=getIntent().getStringExtra("id");
        String type=getIntent().getStringExtra("type");
        contentId=MoviePluginManager.contentId(plugin==null?"":plugin.id(),("show".equals(type)?"show:":"movie:")+id);

        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10,10,13));
        ScrollView scroll=new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        if(plugin==null){text("Không tìm thấy nguồn phim.");return;}
        plugin.detail(this,contentId.substring(contentId.indexOf(':')+1),new MoviePlugin.Callback(){
            public void onSuccess(Object d){runOnUiThread(()->render((MovieContent.Detail)d));}
            public void onError(Throwable e){runOnUiThread(()->text("Không tải được thông tin: "+safe(e)));}
        });
    }

    private void render(MovieContent.Detail d){
        root.removeAllViews();
        if(d==null||d.item==null){text("Không có dữ liệu phim.");return;}

        if(!d.item.backdrop.isEmpty()){
            ImageView backdrop=new ImageView(this);
            backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
            root.addView(backdrop,new LinearLayout.LayoutParams(-1,dp(190)));
            loadImage(d.item.backdrop,backdrop);
        }

        LinearLayout body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14),dp(12),dp(14),dp(30));
        root.addView(body);

        LinearLayout head=new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        ImageView poster=new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        head.addView(poster,new LinearLayout.LayoutParams(dp(120),dp(178)));
        loadImage(d.item.poster,poster);

        LinearLayout meta=new LinearLayout(this);
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setPadding(dp(12),0,0,0);
        TextView title=label(d.item.title+" "+d.item.year,22);
        title.setTypeface(null,android.graphics.Typeface.BOLD);
        meta.addView(title);
        meta.addView(label("⭐ "+(empty(d.item.rating)?"-":d.item.rating),15));
        meta.addView(label(d.item.genres.isEmpty()?"":String.join(" • ",d.item.genres),14));
        if(!empty(d.director))meta.addView(label("Đạo diễn: "+d.director,14));
        head.addView(meta,new LinearLayout.LayoutParams(0,-1,1));
        body.addView(head);

        if(!empty(d.item.overview)){
            body.addView(label("Nội dung",18));
            body.addView(label(d.item.overview,14));
        }

        if(d.seasons.isEmpty()){
            addPlay(body,"▶  Xem phim",d.item.id,"","");
        }else{
            body.addView(label("Tập phim",18));
            for(MovieContent.Season s:d.seasons){
                body.addView(label(s.name,16));
                for(MovieContent.Episode e:s.episodes)addPlay(body,"▶  S"+s.number+" • E"+e.number+"  "+e.name,d.item.id,String.valueOf(s.number),String.valueOf(e.number));
            }
        }
    }

    private void addPlay(LinearLayout parent,String title,String id,String season,String episode){
        TextView v=label(title,15);
        v.setTextColor(Color.WHITE);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(Color.rgb(40,40,48));bg.setCornerRadius(dp(6));
        v.setBackground(bg);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setPadding(dp(14),0,dp(10),0);
        v.setFocusable(true);v.setClickable(true);
        v.setOnClickListener(x->resolveAndPlay(id,season,episode));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(50));
        lp.setMargins(0,dp(4),0,dp(4));
        parent.addView(v,lp);
    }

    private void resolveAndPlay(String id,String season,String episode){
        plugin.sources(this,contentId.substring(contentId.indexOf(':')+1),season,episode,new MoviePlugin.Callback(){
            public void onSuccess(Object data){runOnUiThread(()->{
                List<MovieContent.Source> s=(List<MovieContent.Source>)data;
                if(s==null||s.isEmpty()){text("Không có nguồn phát.");return;}
                java.util.Collections.sort(s,(a,b)->Integer.compare(score(b.quality),score(a.quality)));
                MovieContent.Source best=s.get(0);
                Intent i=new Intent(MovieDetailActivity.this,PlayerActivity.class);
                i.putExtra(PlayerActivity.EXTRA_NAME,"Movie");
                i.putExtra(PlayerActivity.EXTRA_URL,best.url);
                i.putExtra(PlayerActivity.EXTRA_MIME,best.mime==null?"":best.mime);
                android.os.Bundle headers=sourceHeaders(best);
                i.putExtra(PlayerActivity.EXTRA_HEADERS,headers);

                ArrayList<String> fallbackUrls=new ArrayList<>();
                ArrayList<android.os.Bundle> fallbackHeaders=new ArrayList<>();
                for(int idx=1;idx<s.size();idx++){
                    MovieContent.Source x=s.get(idx);
                    if(x.url==null||x.url.isEmpty())continue;
                    fallbackUrls.add(x.url);
                    fallbackHeaders.add(sourceHeaders(x));
                }
                i.putStringArrayListExtra(PlayerActivity.EXTRA_FALLBACK_URLS,fallbackUrls);
                i.putParcelableArrayListExtra(PlayerActivity.EXTRA_FALLBACK_HEADERS,fallbackHeaders);
                startActivity(i);
            });}
            public void onError(Throwable e){runOnUiThread(()->text("Lỗi tải link phát: "+safe(e)));}
        });
    }

    private android.os.Bundle sourceHeaders(MovieContent.Source source){
        android.os.Bundle h=new android.os.Bundle();
        if(source!=null){
            for(java.util.Map.Entry<String,String> e:source.headers.entrySet()){
                if(e.getKey()!=null&&e.getValue()!=null&&!e.getValue().isEmpty())h.putString(e.getKey(),e.getValue());
            }
            if(source.referer!=null&&!source.referer.isEmpty()&&!h.containsKey("Referer"))h.putString("Referer",source.referer);
            if(source.origin!=null&&!source.origin.isEmpty()&&!h.containsKey("Origin"))h.putString("Origin",source.origin);
        }
        if(!h.containsKey("User-Agent"))h.putString("User-Agent","Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
        return h;
    }

    private int score(String q){
        if(q==null)return 0;
        q=q.toLowerCase();
        if(q.contains("2160")||q.contains("4k"))return 4;
        if(q.contains("1080"))return 3;
        if(q.contains("720"))return 2;
        return 1;
    }
    private TextView label(String s,float size){
        TextView v=new TextView(this);v.setText(s==null?"":s);v.setTextColor(Color.WHITE);v.setTextSize(size);
        v.setPadding(0,dp(5),0,dp(5));return v;
    }
    private void text(String s){root.addView(label(s,15),new LinearLayout.LayoutParams(-1,-2));}
    private boolean empty(String s){return s==null||s.trim().isEmpty();}
    private String safe(Throwable e){return e==null?"lỗi không xác định":(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());}

    private void loadImage(String url,ImageView target){
        if(empty(url))return;
        new Thread(()->{
            try{
                HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
                c.setConnectTimeout(8000);c.setReadTimeout(12000);
                c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");c.setRequestProperty("Accept","image/avif,image/webp,image/apng,image/*,*/*;q=0.8");c.setRequestProperty("Referer","https://www.themoviedb.org/");
                InputStream in=c.getInputStream();Bitmap b=BitmapFactory.decodeStream(in);in.close();c.disconnect();
                if(b!=null)runOnUiThread(()->target.setImageBitmap(b));
            }catch(Throwable ignored){}
        }).start();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
