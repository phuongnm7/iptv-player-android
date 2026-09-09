package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@androidx.media3.common.util.UnstableApi
public final class MainActivity extends Activity {
    private static final int OPEN_M3U = 101;
    private static final int PICK_WALLPAPER = 103;
    private static final int MAX_PLAYLIST_BYTES = 8 * 1024 * 1024;
    private static final String DEFAULT_PLAYLIST = PlaylistSourceStore.DEFAULT_URL;

    private final ExecutorService io = SessionStore.IO;
    private final M3uParser parser = new M3uParser();
    private final List<Channel> allChannels = new ArrayList<>();
    private ChannelAdapter adapter;
    private EditText inputUrl, inputSearch;
    private LinearLayout groupRow;
    private TextView txtSource, txtSummary, txtEmpty;
    private ProgressBar progress;
    private int duplicateCount, missingUrlCount, activeSection;
    private String currentSource = "", selectedGroup = "";
    private boolean loading, importExpanded = true;

    @Override protected void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); setupViews(); restoreSession(); }

    private void setupViews() {
        setContentView(R.layout.activity_main); Insets.apply(findViewById(R.id.mainRoot));
        inputUrl=findViewById(R.id.inputUrl); inputSearch=findViewById(R.id.inputSearch); groupRow=findViewById(R.id.groupRow);
        txtSource=findViewById(R.id.txtSource); txtSummary=findViewById(R.id.txtSummary); txtEmpty=findViewById(R.id.txtEmpty); progress=findViewById(R.id.progress);
        ListView list=findViewById(R.id.listChannels);
        adapter=new ChannelAdapter(this,new ChannelAdapter.Listener(){@Override public void onSelectionChanged(){updateSummary();}@Override public void onFavoriteChanged(Channel c,boolean f){toast(f?"Đã thêm vào Yêu thích":"Đã bỏ khỏi Yêu thích");if(activeSection==1)filter();else adapter.notifyDataSetChanged();}});
        list.setAdapter(adapter); list.setItemsCanFocus(false); list.setEmptyView(txtEmpty); list.setOnItemClickListener((p,v,i,id)->play(adapter.getItem(i))); list.setOnItemLongClickListener((p,v,i,id)->{showChannelActions(adapter.getItem(i));return true;});
        findViewById(R.id.btnLoadUrl).setOnClickListener(v->loadFromUrl()); findViewById(R.id.btnOpenFile).setOnClickListener(v->openFilePicker());
        findViewById(R.id.btnPlayUrl).setOnClickListener(v->playDirect()); findViewById(R.id.btnSources).setOnClickListener(v->setImportExpanded(!importExpanded)); findViewById(R.id.btnPlaylists).setOnClickListener(v->showPlaylistSources()); findViewById(R.id.btnWallpaper).setOnClickListener(v->showSettings());
        findViewById(R.id.btnAllChannels).setOnClickListener(v->selectSection(0)); findViewById(R.id.btnFavorites).setOnClickListener(v->selectSection(1)); findViewById(R.id.btnRecent).setOnClickListener(v->selectSection(2)); findViewById(R.id.btnClearFilters).setOnClickListener(v->{inputSearch.setText("");selectedGroup="";updateGroupButtons();filter();}); findViewById(R.id.btnAbout).setOnClickListener(v->showAbout()); txtSource.setOnClickListener(v->showSource(currentSource));
        inputSearch.addTextChangedListener(new TextWatcher(){@Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}@Override public void onTextChanged(CharSequence s,int st,int b,int c){filter();}@Override public void afterTextChanged(Editable e){}});
        rebuildGroups(); setImportExpanded(allChannels.isEmpty()); updateSectionButtons();
    }

    private void restoreSession() {
        setLoading(true);
        io.execute(()->{try{SessionStore.State state=SessionStore.load(getApplicationContext());ui(()->{if(state!=null&&state.result!=null&&!state.result.channels.isEmpty()){showPlaylist(state.result,state.source);if(state.source.startsWith("http"))inputUrl.setText(state.source.split("\\n")[0]);setLoading(false);}else loadDefaultPlaylist();});}catch(Exception e){ui(this::loadDefaultPlaylist);}});
    }

    private void loadDefaultPlaylist(){inputUrl.setText(DEFAULT_PLAYLIST);loadFromUrl();}

    private void loadFromUrl(){String source=inputUrl.getText().toString().trim();if(!M3uParser.isNetworkUrl(source)||!(source.startsWith("http://")||source.startsWith("https://"))){toast("URL phải bắt đầu bằng http:// hoặc https://");setLoading(false);return;}setLoading(true);io.execute(()->{HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(source).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(true);c.setRequestProperty("User-Agent","Nm7-IPTV/1.7 Android");int status=c.getResponseCode();if(status<200||status>=300)throw new Exception("HTTP "+status);String effective=c.getURL().toString(),type=c.getContentType(),description=source.equals(effective)?source:source+"\nChuyển hướng: "+effective;if(type!=null&&(type.startsWith("video/")||type.contains("dash+xml"))){Channel direct=new Channel("Luồng trực tiếp","Phát trực tiếp",effective,"","",java.util.Collections.emptyMap());if(type.contains("dash+xml"))direct.options().add("#KODIPROP:inputstream.adaptive.manifest_type=mpd");ui(()->showPlaylist(new M3uParser.Result(java.util.Collections.singletonList(direct),0,0),description));return;}String content;try(InputStream s=new BufferedInputStream(c.getInputStream())){content=readText(s);}M3uParser.Result result=parser.parse(content,effective);ui(()->showPlaylist(result,description));}catch(Exception e){ui(()->showError("Không tải được playlist: "+readable(e)));}finally{if(c!=null)c.disconnect();}});}

    private void openFilePicker(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,OPEN_M3U);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();if(request==OPEN_M3U)readLocalFile(uri);if(request==PICK_WALLPAPER){toast("Tính năng hình nền không khả dụng trong bản rút gọn này");}}
    private void readLocalFile(Uri uri){setLoading(true);io.execute(()->{try(InputStream s=getContentResolver().openInputStream(uri)){if(s==null)throw new Exception("Không thể mở tệp");M3uParser.Result r=parser.parse(readText(s),"");ui(()->showPlaylist(r,uri.toString()));}catch(Exception e){ui(()->showError("Không đọc được tệp: "+readable(e)));}});}
    private String readText(InputStream s)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int total=0,n;while((n=s.read(chunk))!=-1){total+=n;if(total>MAX_PLAYLIST_BYTES)throw new Exception("Playlist lớn hơn 8 MB. Với link video, dùng Phát URL.");b.write(chunk,0,n);}return b.toString(StandardCharsets.UTF_8.name());}
    private void showPlaylist(M3uParser.Result r,String source){if(r.channels.isEmpty()){setLoading(false);new AlertDialog.Builder(this).setTitle("Không có kênh hợp lệ").setMessage("Không thay thế playlist đang mở.").setPositiveButton("Đóng",null).show();return;}allChannels.clear();allChannels.addAll(r.channels);duplicateCount=r.duplicateCount;missingUrlCount=r.missingUrlCount;currentSource=source;txtSource.setText("Nguồn: "+source);inputSearch.setText("");selectedGroup="";rebuildGroups();filter();setLoading(false);setImportExpanded(false);saveSession();String first=source.split("\\n",2)[0];if(first.startsWith("http://")||first.startsWith("https://"))try{PlaylistSourceStore.add(this,"",first);}catch(Exception ignored){}}
    private void rebuildGroups(){Set<String> u=new LinkedHashSet<>();for(Channel c:allChannels)u.add(c.group());List<String> groups=new ArrayList<>(u);groups.sort(String.CASE_INSENSITIVE_ORDER);if(!selectedGroup.isEmpty()&&!u.contains(selectedGroup))selectedGroup="";groupRow.removeAllViews();addGroupButton(getString(R.string.all_groups),"");for(String g:groups)addGroupButton(g,g);updateGroupButtons();}
    private void addGroupButton(String label,String value){Button b=new Button(this);b.setTag(value);b.setText(label);b.setTextSize(13);b.setAllCaps(false);b.setSingleLine(true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,dp(46));p.setMarginEnd(dp(8));groupRow.addView(b,p);b.setOnClickListener(v->{selectedGroup=(String)v.getTag();updateGroupButtons();filter();findViewById(R.id.listChannels).requestFocus();});}
    private void updateGroupButtons(){if(groupRow==null)return;for(int i=0;i<groupRow.getChildCount();i++){View c=groupRow.getChildAt(i);boolean active=selectedGroup.equals(c.getTag());c.setSelected(active);c.setBackgroundResource(active?R.drawable.button_primary:R.drawable.button_secondary);if(c instanceof Button)((Button)c).setTextColor(getColor(active?R.color.navy:R.color.text_primary));}}
    private void filter(){if(adapter==null)return;String q=inputSearch.getText().toString().trim().toLowerCase(Locale.ROOT);List<Channel> f=new ArrayList<>();for(Channel c:allChannels){boolean gm=selectedGroup.isEmpty()||c.group().equals(selectedGroup),sm=activeSection==0||(activeSection==1&&AppPreferences.isFavorite(this,c))||(activeSection==2&&AppPreferences.isRecent(this,c)),qm=q.isEmpty()||c.name().toLowerCase(Locale.ROOT).contains(q)||c.group().toLowerCase(Locale.ROOT).contains(q)||c.url().toLowerCase(Locale.ROOT).contains(q);if(gm&&sm&&qm)f.add(c);}if(activeSection==2)f.sort((a,b)->Integer.compare(AppPreferences.recentRank(this,a),AppPreferences.recentRank(this,b)));adapter.submit(f);updateSummary();}
    private void updateSummary(){txtSummary.setText(adapter.getCount()+"/"+allChannels.size()+" kênh\n"+duplicateCount+" trùng đã bỏ • "+missingUrlCount+" thiếu/sai URL đã bỏ");}
    private void play(Channel c){AppPreferences.recordRecent(this,c);Intent i=new Intent(this,PlayerActivity.class);i.putExtra(PlayerActivity.EXTRA_NAME,c.name());i.putExtra(PlayerActivity.EXTRA_URL,c.url());Bundle h=new Bundle();for(java.util.Map.Entry<String,String> e:c.headers().entrySet())h.putString(e.getKey(),e.getValue());i.putExtra(PlayerActivity.EXTRA_HEADERS,h);i.putExtra(PlayerActivity.EXTRA_MIME,c.mimeHint());i.putStringArrayListExtra(PlayerActivity.EXTRA_OPTIONS,new ArrayList<>(c.options()));startActivity(i);}
    private void selectSection(int s){activeSection=s;updateSectionButtons();filter();}
    private void updateSectionButtons(){int[] ids={R.id.btnAllChannels,R.id.btnFavorites,R.id.btnRecent};for(int i=0;i<ids.length;i++){View b=findViewById(ids[i]);b.setAlpha(i==activeSection?1f:.62f);b.setSelected(i==activeSection);}if(txtEmpty!=null)txtEmpty.setText(activeSection==1?"Chưa có kênh yêu thích":activeSection==2?"Chưa có kênh đã xem":"Không tìm thấy kênh");}
    private void saveSession(){try{SessionStore.save(getApplicationContext(),SessionStore.snapshot(allChannels),currentSource,duplicateCount,missingUrlCount);}catch(Exception ignored){}}
    private void setLoading(boolean v){loading=v;if(progress!=null)progress.setVisibility(v?View.VISIBLE:View.GONE);}
    private void setImportExpanded(boolean expanded){importExpanded=expanded;View section=findViewById(R.id.importPanel);if(section!=null)section.setVisibility(expanded?View.VISIBLE:View.GONE);}
    private void showPlaylistSources(){List<PlaylistSourceStore.Source> sources=PlaylistSourceStore.load(this);String[] labels=new String[sources.size()];for(int i=0;i<sources.size();i++)labels[i]=sources.get(i).name;new AlertDialog.Builder(this).setTitle("Nguồn IPTV").setItems(labels,(d,w)->{inputUrl.setText(sources.get(w).url);loadFromUrl();}).setNegativeButton("Đóng",null).show();}
    private void playDirect(){String source=inputUrl.getText().toString().trim();if(source.isEmpty()){toast("Nhập URL để phát");return;}Channel c=new Channel("URL trực tiếp","Trực tiếp",source,"","",java.util.Collections.emptyMap());play(c);}
    private void showSettings(){new AlertDialog.Builder(this).setTitle("Cài đặt").setMessage("Tùy chọn nguồn phát và giao diện có thể được cấu hình trong ứng dụng.").setPositiveButton("Đóng",null).show();}
    private void showAbout(){new AlertDialog.Builder(this).setTitle("Nm7 IPTV Player").setMessage("Trình phát IPTV cho Android.").setPositiveButton("Đóng",null).show();}
    private void showSource(String source){TextView v=new TextView(this);v.setText(source);v.setTextIsSelectable(true);v.setPadding(dp(20),dp(20),dp(20),dp(20));new AlertDialog.Builder(this).setTitle("Nguồn hiện tại").setView(v).setPositiveButton("Đóng",null).show();}
    private void showChannelActions(Channel c){new AlertDialog.Builder(this).setTitle(c.name()).setItems(new String[]{AppPreferences.isFavorite(this,c)?"Bỏ Yêu thích":"Thêm vào Yêu thích","Phát"},(d,w)->{if(w==0){AppPreferences.toggleFavorite(this,c);filter();}else play(c);}).show();}
    private void ui(Runnable r){runOnUiThread(r);}
    private void showError(String m){setLoading(false);new AlertDialog.Builder(this).setTitle("Lỗi").setMessage(m).setPositiveButton("Đóng",null).show();}
    private String readable(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    private void toast(String m){Toast.makeText(this,m,Toast.LENGTH_SHORT).show();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
