package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.phuong.iptvplayer.movie.Film4kSource;
import vn.phuong.iptvplayer.movie.MovieHttp;
import vn.phuong.iptvplayer.movie.MovieJsSource;
import vn.phuong.iptvplayer.movie.MovieModels.MovieDetail;
import vn.phuong.iptvplayer.movie.MovieModels.MovieEpisode;
import vn.phuong.iptvplayer.movie.MovieModels.MovieItem;
import vn.phuong.iptvplayer.movie.MovieModels.Playback;
import vn.phuong.iptvplayer.movie.MoviePluginStore;
import vn.phuong.iptvplayer.movie.MovieSource;

public final class MovieActivity extends Activity {
    private static final int OPEN_PLUGIN=601;
    private Spinner sourceSpinner;
    private EditText search;
    private LinearLayout content;
    private ProgressBar progress;
    private TextView status,title;
    private final ExecutorService imageIo=Executors.newFixedThreadPool(3);
    private final Map<String,Bitmap> bitmapCache=new LinkedHashMap<String,Bitmap>(48,0.75f,true){protected boolean removeEldestEntry(Map.Entry<String,Bitmap> e){return size()>48;}};
    private final List<SourceEntry> entries=new ArrayList<>();
    private MovieSource source;
    private List<MovieItem> homeItems=new ArrayList<>();
    private boolean showingDetail;

    private static final class SourceEntry{
        final String label; final FileRef file;
        SourceEntry(String l,FileRef f){label=l;file=f;}
    }
    private static final class FileRef{final java.io.File file;FileRef(java.io.File f){file=f;}}

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        buildUi();
        rebuildSources();
        if(!entries.isEmpty())selectSource(0);
    }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(8),dp(6),dp(8),dp(76));root.setBackgroundResource(R.color.navy);
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView h=new TextView(this);h.setText("🎬 MOVIE");h.setTextColor(getColor(R.color.text_primary));h.setTextSize(20);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        top.addView(h,new LinearLayout.LayoutParams(0,dp(44),1));
        Button add=new Button(this);add.setText("＋ Nguồn");add.setAllCaps(false);add.setTextSize(12);add.setOnClickListener(v->openPluginPicker());
        top.addView(add,new LinearLayout.LayoutParams(dp(92),dp(44)));root.addView(top);
        LinearLayout src=new LinearLayout(this);src.setGravity(Gravity.CENTER_VERTICAL);
        sourceSpinner=new Spinner(this);src.addView(sourceSpinner,new LinearLayout.LayoutParams(0,dp(46),1));
        Button reload=new Button(this);reload.setText("↻");reload.setContentDescription("Tải lại phim");reload.setOnClickListener(v->loadHome());src.addView(reload,new LinearLayout.LayoutParams(dp(52),dp(46)));root.addView(src);
        LinearLayout searchRow=new LinearLayout(this);search=new EditText(this);search.setSingleLine(true);search.setHint("Tìm phim…");search.setTextColor(getColor(R.color.text_primary));search.setHintTextColor(getColor(R.color.text_secondary));searchRow.addView(search,new LinearLayout.LayoutParams(0,dp(46),1));
        Button go=new Button(this);go.setText("Tìm");go.setAllCaps(false);go.setOnClickListener(v->doSearch());searchRow.addView(go,new LinearLayout.LayoutParams(dp(60),dp(46)));root.addView(searchRow);
        status=new TextView(this);status.setTextColor(getColor(R.color.text_secondary));status.setTextSize(12);status.setPadding(dp(4),dp(3),dp(4),dp(3));root.addView(status);
        progress=new ProgressBar(this);progress.setVisibility(View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        search.setOnEditorActionListener((v,a,e)->{doSearch();return true;});
        SharedPlaybackSession.setTab(this,SharedPlaybackSession.TAB_MOVIE);
    }

    private void rebuildSources(){
        entries.clear();entries.add(new SourceEntry("Film4k",null));
        for(java.io.File f:MoviePluginStore.list(this))entries.add(new SourceEntry(f.getName(),new FileRef(f)));
        ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item){@Override public View getView(int p,android.view.View v,android.view.ViewGroup g){TextView t=(TextView)super.getView(p,v,g);t.setTextColor(getColor(R.color.text_primary));return t;}};
        for(SourceEntry e:entries)a.add(e.label);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);sourceSpinner.setAdapter(a);
        sourceSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){selectSource(pos);}});
    }

    private void selectSource(int pos){
        if(pos<0||pos>=entries.size())return;if(source!=null)source.close();
        SourceEntry e=entries.get(pos);
        try{
            source=e.file==null?new Film4kSource():new MovieJsSource(this,e.file.file.getName(),MoviePluginStore.read(e.file.file));
            showingDetail=false;loadHome();
        }catch(Exception ex){showError("Không mở được nguồn phim: "+ex.getMessage());}
    }

    private void loadHome(){
        if(source==null)return; showingDetail=false;setBusy(true);status.setText("Đang tải phim…");source.loadHome(new MovieSource.Callback<List<MovieItem>>(){
            public void onSuccess(List<MovieItem> value){runOnUiThread(()->{homeItems=value;renderHome(value);setBusy(false);status.setText(value.size()+" phim");});}
            public void onError(String m){runOnUiThread(()->{setBusy(false);showError(m);});}
        });
    }

    private void doSearch(){
        String q=search.getText().toString().trim();if(q.isEmpty()){loadHome();return;}if(source==null)return;setBusy(true);status.setText("Đang tìm…");source.search(q,new MovieSource.Callback<List<MovieItem>>(){
            public void onSuccess(List<MovieItem> v){runOnUiThread(()->{renderHome(v);setBusy(false);status.setText(v.size()+" kết quả");});}
            public void onError(String m){runOnUiThread(()->{setBusy(false);showError(m);});}
        });
    }

    private void renderHome(List<MovieItem> items){
        content.removeAllViews();
        if(items==null||items.isEmpty()){empty("Không có phim để hiển thị.");return;}
        TextView head=label("Phim");content.addView(head,new LinearLayout.LayoutParams(-1,dp(42)));
        LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);
        int col=0;LinearLayout row=null;
        for(MovieItem m:items){
            if(col==0){row=new LinearLayout(this);row.setPadding(0,0,0,dp(8));grid.addView(row,new LinearLayout.LayoutParams(-1,dp(222)));} addCard(row,m);col=(col+1)%3;
        }
        content.addView(grid);
    }

    private void addCard(LinearLayout row,MovieItem m){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.TOP);card.setPadding(dp(3),0,dp(3),0);card.setFocusable(true);card.setClickable(true);
        ImageView iv=new ImageView(this);iv.setScaleType(ImageView.ScaleType.CENTER_CROP);iv.setBackgroundResource(R.drawable.panel);card.addView(iv,new LinearLayout.LayoutParams(-1,dp(164)));
        TextView t=label(m.title);t.setTextSize(12);t.setMaxLines(2);t.setPadding(dp(2),dp(4),dp(2),0);card.addView(t,new LinearLayout.LayoutParams(-1,dp(46)));
        card.setOnClickListener(v->openDetail(m));row.addView(card,new LinearLayout.LayoutParams(0,-1,1));
        loadImage(m.posterUrl,iv);
    }

    private TextView label(String s){TextView t=new TextView(this);t.setText(s==null?"":s);t.setTextColor(getColor(R.color.text_primary));t.setTextSize(15);return t;}
    private void empty(String s){TextView t=label(s);t.setGravity(Gravity.CENTER);content.addView(t,new LinearLayout.LayoutParams(-1,dp(180)));}

    private void openDetail(MovieItem item){
        showingDetail=true;setBusy(true);status.setText("Đang tải chi tiết…");content.removeAllViews();
        source.loadDetail(item,new MovieSource.Callback<MovieDetail>(){
            public void onSuccess(MovieDetail d){runOnUiThread(()->{setBusy(false);renderDetail(d);});}
            public void onError(String m){runOnUiThread(()->{setBusy(false);showError(m);});}
        });
    }

    private void renderDetail(MovieDetail d){
        content.removeAllViews();
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);
        ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);top.addView(poster,new LinearLayout.LayoutParams(dp(128),dp(190)));loadImage(d.movie.posterUrl,poster);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(10),0,0,0);info.addView(label(d.movie.title),new LinearLayout.LayoutParams(-1,dp(50)));TextView meta=label((d.movie.year.isEmpty()?"":d.movie.year+"  •  ")+(d.rating.isEmpty()?"":d.rating+"  •  ") + (d.movie.quality.isEmpty()?"FHD":d.movie.quality));meta.setTextColor(getColor(R.color.accent));info.addView(meta);TextView cat=label(d.category);cat.setTextSize(12);cat.setTextColor(getColor(R.color.text_secondary));info.addView(cat);top.addView(info,new LinearLayout.LayoutParams(0,-2,1));content.addView(top);
        TextView desc=label(d.description==null?"":d.description);desc.setPadding(0,dp(10),0,dp(10));content.addView(desc);
        if(d.movie.show&&!d.episodes.isEmpty())content.addView(label("Danh sách tập"),new LinearLayout.LayoutParams(-1,dp(42)));
        for(MovieEpisode ep:d.episodes){Button b=new Button(this);b.setText(ep.name);b.setAllCaps(false);b.setOnClickListener(v->playEpisode(ep));content.addView(b,new LinearLayout.LayoutParams(-1,dp(48)));}}
    private void playEpisode(MovieEpisode ep){
        setBusy(true);status.setText("Đang lấy luồng phát…");source.loadPlayback(ep,new MovieSource.Callback<Playback>(){
            public void onSuccess(Playback p){runOnUiThread(()->{setBusy(false);Intent i=new Intent(MovieActivity.this,PlayerActivity.class);i.putExtra(PlayerActivity.EXTRA_NAME,ep.name);i.putExtra(PlayerActivity.EXTRA_URL,p.url);i.putExtra(PlayerActivity.EXTRA_MIME,p.mime);Bundle h=new Bundle();for(Map.Entry<String,String> e:p.headers.entrySet())h.putString(e.getKey(),e.getValue());i.putExtra(PlayerActivity.EXTRA_HEADERS,h);startActivity(i);});}
            public void onError(String m){runOnUiThread(()->{setBusy(false);showError(m);});}
        });
    }

    private void loadImage(final String url,final ImageView target){
        if(url==null||url.isEmpty())return;Bitmap cached; synchronized(bitmapCache){cached=bitmapCache.get(url);}if(cached!=null){target.setImageBitmap(cached);return;}
        imageIo.execute(()->{HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(true);c.setUseCaches(true);c.setRequestProperty("User-Agent","NM7-Movie/1.10.113");try(InputStream in=c.getInputStream()){Bitmap b=BitmapFactory.decodeStream(in);if(b!=null){synchronized(bitmapCache){bitmapCache.put(url,b);}runOnUiThread(()->target.setImageBitmap(b));}}}catch(Exception ignored){}finally{if(c!=null)c.disconnect();}});
    }

    private void openPluginPicker(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/javascript");startActivityForResult(i,OPEN_PLUGIN);}
    @Override protected void onActivityResult(int r,int result,Intent data){super.onActivityResult(r,result,data);if(r!=OPEN_PLUGIN||result!=RESULT_OK||data==null||data.getData()==null)return;Uri u=data.getData();new Thread(()->{try{String name=uriName(u);java.io.File f=MoviePluginStore.importFile(this,u,name);runOnUiThread(()->{rebuildSources();for(int i=0;i<entries.size();i++)if(entries.get(i).file!=null&&entries.get(i).file.file.equals(f)){sourceSpinner.setSelection(i);break;}status.setText("Đã thêm plugin "+f.getName());});}catch(Exception e){runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Plugin không hợp lệ").setMessage(e.getMessage()).setPositiveButton("Đóng",null).show());}}).start();}
    private String uriName(Uri u){String p=u.getLastPathSegment();if(p==null||p.isEmpty())return "movie-plugin.js";int slash=p.lastIndexOf('/');return slash>=0?p.substring(slash+1):p;}
    private void setBusy(boolean b){progress.setVisibility(b?View.VISIBLE:View.GONE);}
    private void showError(String s){status.setText(s);if(content!=null&&content.getChildCount()==0)empty(s);}
    @Override public void onBackPressed(){if(showingDetail){showingDetail=false;loadHome();}else super.onBackPressed();}
    @Override protected void onDestroy(){if(source!=null)source.close();imageIo.shutdownNow();super.onDestroy();}
    private int dp(int x){return Math.round(x*getResources().getDisplayMetrics().density);}
}