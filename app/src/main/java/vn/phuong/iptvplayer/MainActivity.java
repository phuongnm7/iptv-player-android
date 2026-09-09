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
    private TextView txtEmpty;
    private ProgressBar progress;
    private int duplicateCount, missingUrlCount, activeSection;
    private String currentSource = "", selectedGroup = "";
    private boolean loading, importExpanded = true;
    private int wallpaperGeneration;

    @Override protected void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); setupViews(); restoreSession(); }

    private void setupViews() {
        setContentView(R.layout.activity_main); Insets.apply(findViewById(R.id.mainRoot)); applyWallpaper();
        inputUrl=findViewById(R.id.inputUrl); inputSearch=findViewById(R.id.inputSearch); groupRow=findViewById(R.id.groupRow);
        txtEmpty=findViewById(R.id.txtEmpty); progress=findViewById(R.id.progress);
        ListView list=findViewById(R.id.listChannels);
        adapter=new ChannelAdapter(this,new ChannelAdapter.Listener(){@Override public void onSelectionChanged(){updateSummary();}@Override public void onFavoriteChanged(Channel c,boolean f){toast(f?"Đã thêm vào Yêu thích":"Đã bỏ khỏi Yêu thích");if(activeSection==1)filter();else adapter.notifyDataSetChanged();}});
        list.setAdapter(adapter); list.setItemsCanFocus(false); list.setEmptyView(txtEmpty); list.setOnItemClickListener((p,v,i,id)->play(adapter.getItem(i))); list.setOnItemLongClickListener((p,v,i,id)->{showChannelActions(adapter.getItem(i));return true;});
        findViewById(R.id.btnLoadUrl).setOnClickListener(v->loadFromUrl()); findViewById(R.id.btnOpenFile).setOnClickListener(v->openFilePicker());
        findViewById(R.id.btnPlayUrl).setOnClickListener(v->playDirect()); findViewById(R.id.btnSources).setOnClickListener(v->{boolean expand=!importExpanded;setImportExpanded(expand);if(expand){inputUrl.setText("");inputUrl.requestFocus();}}); findViewById(R.id.btnPlaylists).setOnClickListener(v->showPlaylistSources()); findViewById(R.id.btnWallpaper).setOnClickListener(v->showSettings());
        findViewById(R.id.btnAllChannels).setOnClickListener(v->selectSection(0)); findViewById(R.id.btnFavorites).setOnClickListener(v->selectSection(1)); findViewById(R.id.btnRecent).setOnClickListener(v->selectSection(2)); findViewById(R.id.btnClearFilters).setOnClickListener(v->{inputSearch.setText("");selectedGroup="";updateGroupButtons();filter();}); findViewById(R.id.btnAbout).setOnClickListener(v->showAbout());
        inputSearch.addTextChangedListener(new TextWatcher(){@Override public void beforeTextChanged(CharSequence s,int st,int c,int a){}@Override public void onTextChanged(CharSequence s,int st,int b,int c){filter();}@Override public void afterTextChanged(Editable e){}});
        rebuildGroups(); setImportExpanded(allChannels.isEmpty()); updateSectionButtons(); applyInterfaceMode(list);
    }

    private void restoreSession() {
        setLoading(true);
        io.execute(()->{try{SessionStore.State state=SessionStore.load(getApplicationContext());ui(()->{if(state!=null&&state.result!=null&&!state.result.channels.isEmpty()){showPlaylist(state.result,state.source);if(state.source.startsWith("http"))inputUrl.setText(state.source.split("\\n")[0]);setLoading(false);}else loadDefaultPlaylist();});}catch(Exception e){ui(this::loadDefaultPlaylist);}});
    }

    private void loadDefaultPlaylist(){inputUrl.setText(DEFAULT_PLAYLIST);loadFromUrl();}

    private void loadFromUrl(){String source=inputUrl.getText().toString().trim();if(!M3uParser.isNetworkUrl(source)||!(source.startsWith("http://")||source.startsWith("https://"))){toast("URL phải bắt đầu bằng http:// hoặc https://");setLoading(false);return;}setLoading(true);io.execute(()->{HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(source).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(true);c.setRequestProperty("User-Agent","Nm7-IPTV/1.7 Android");int status=c.getResponseCode();if(status<200||status>=300)throw new Exception("HTTP "+status);String effective=c.getURL().toString(),type=c.getContentType(),description=source.equals(effective)?source:source+"\nChuyển hướng: "+effective;if(type!=null&&(type.startsWith("video/")||type.contains("dash+xml"))){Channel direct=new Channel("Luồng trực tiếp","Phát trực tiếp",effective,"","",java.util.Collections.emptyMap());if(type.contains("dash+xml"))direct.options().add("#KODIPROP:inputstream.adaptive.manifest_type=mpd");ui(()->showPlaylist(new M3uParser.Result(java.util.Collections.singletonList(direct),0,0),description));return;}String content;try(InputStream s=new BufferedInputStream(c.getInputStream())){content=readText(s);}M3uParser.Result result=parser.parse(content,effective);ui(()->showPlaylist(result,description));}catch(Exception e){ui(()->showError("Không tải được playlist: "+readable(e)));}finally{if(c!=null)c.disconnect();}});}

    private void openFilePicker(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");startActivityForResult(i,OPEN_M3U);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();if(request==OPEN_M3U)readLocalFile(uri);if(request==PICK_WALLPAPER){io.execute(()->{try{WallpaperStore.importPhoto(getApplicationContext(),uri);ui(()->{applyWallpaper();toast("Đã đổi hình nền");});}catch(Exception e){ui(()->toast("Không mở được hình nền: "+readable(e)));}});}}
    private void readLocalFile(Uri uri){setLoading(true);io.execute(()->{try(InputStream s=getContentResolver().openInputStream(uri)){if(s==null)throw new Exception("Không thể mở tệp");M3uParser.Result r=parser.parse(readText(s),"");ui(()->showPlaylist(r,uri.toString()));}catch(Exception e){ui(()->showError("Không đọc được tệp: "+readable(e)));}});}
    private String readText(InputStream s)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int total=0,n;while((n=s.read(chunk))!=-1){total+=n;if(total>MAX_PLAYLIST_BYTES)throw new Exception("Playlist lớn hơn 8 MB. Với link video, dùng Phát URL.");b.write(chunk,0,n);}return b.toString(StandardCharsets.UTF_8.name());}
    private void showPlaylist(M3uParser.Result r,String source){if(r.channels.isEmpty()){setLoading(false);new AlertDialog.Builder(this).setTitle("Không có kênh hợp lệ").setMessage("Không thay thế playlist đang mở.").setPositiveButton("Đóng",null).show();return;}allChannels.clear();allChannels.addAll(r.channels);duplicateCount=r.duplicateCount;missingUrlCount=r.missingUrlCount;currentSource=source;inputSearch.setText("");selectedGroup="";rebuildGroups();filter();setLoading(false);setImportExpanded(false);saveSession();String first=source.split("\\n",2)[0];if(first.startsWith("http://")||first.startsWith("https://"))try{PlaylistSourceStore.add(this,"",first);}catch(Exception ignored){}}
    private void rebuildGroups(){Set<String> u=new LinkedHashSet<>();for(Channel c:allChannels)u.add(c.group());List<String> groups=new ArrayList<>(u);groups.sort(String.CASE_INSENSITIVE_ORDER);if(!selectedGroup.isEmpty()&&!u.contains(selectedGroup))selectedGroup="";groupRow.removeAllViews();addGroupButton(getString(R.string.all_groups),"");for(String g:groups)addGroupButton(g,g);updateGroupButtons();}
    private void addGroupButton(String label,String value){Button b=new Button(this);b.setTag(value);b.setText(label);b.setTextSize(13);b.setAllCaps(false);b.setSingleLine(true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,dp(46));p.setMarginEnd(dp(8));groupRow.addView(b,p);b.setOnClickListener(v->{selectedGroup=(String)v.getTag();updateGroupButtons();filter();findViewById(R.id.listChannels).requestFocus();});}
    private void updateGroupButtons(){if(groupRow==null)return;for(int i=0;i<groupRow.getChildCount();i++){View c=groupRow.getChildAt(i);boolean active=selectedGroup.equals(c.getTag());c.setSelected(active);c.setBackgroundResource(active?R.drawable.button_primary:R.drawable.button_secondary);if(c instanceof Button)((Button)c).setTextColor(getColor(active?R.color.navy:R.color.text_primary));}}
    private void filter(){if(adapter==null)return;String q=inputSearch.getText().toString().trim().toLowerCase(Locale.ROOT);List<Channel> f=new ArrayList<>();for(Channel c:allChannels){boolean gm=selectedGroup.isEmpty()||c.group().equals(selectedGroup),sm=activeSection==0||(activeSection==1&&AppPreferences.isFavorite(this,c))||(activeSection==2&&AppPreferences.isRecent(this,c)),qm=q.isEmpty()||c.name().toLowerCase(Locale.ROOT).contains(q)||c.group().toLowerCase(Locale.ROOT).contains(q)||c.url().toLowerCase(Locale.ROOT).contains(q);if(gm&&sm&&qm)f.add(c);}if(activeSection==2)f.sort((a,b)->Integer.compare(AppPreferences.recentRank(this,a),AppPreferences.recentRank(this,b)));adapter.submit(f);updateSummary();}
    private void updateSummary(){}
    private void play(Channel c){AppPreferences.recordRecent(this,c);Intent i=new Intent(this,PlayerActivity.class);i.putExtra(PlayerActivity.EXTRA_NAME,c.name());i.putExtra(PlayerActivity.EXTRA_URL,c.url());Bundle h=new Bundle();for(java.util.Map.Entry<String,String> e:c.headers().entrySet())h.putString(e.getKey(),e.getValue());i.putExtra(PlayerActivity.EXTRA_HEADERS,h);i.putExtra(PlayerActivity.EXTRA_MIME,c.mimeHint());i.putStringArrayListExtra(PlayerActivity.EXTRA_OPTIONS,new ArrayList<>(c.options()));startActivity(i);}
    private void selectSection(int s){activeSection=s;updateSectionButtons();filter();}
    private void updateSectionButtons(){int[] ids={R.id.btnAllChannels,R.id.btnFavorites,R.id.btnRecent};for(int i=0;i<ids.length;i++){View b=findViewById(ids[i]);b.setAlpha(i==activeSection?1f:.62f);b.setSelected(i==activeSection);}if(txtEmpty!=null)txtEmpty.setText(activeSection==1?"Chưa có kênh yêu thích":activeSection==2?"Chưa có kênh đã xem":"Không tìm thấy kênh");}
    private void saveSession(){try{SessionStore.save(getApplicationContext(),SessionStore.snapshot(allChannels),currentSource,duplicateCount,missingUrlCount);}catch(Exception ignored){}}
    private void setLoading(boolean v){loading=v;if(progress!=null)progress.setVisibility(v?View.VISIBLE:View.GONE);}
    private void setImportExpanded(boolean expanded){importExpanded=expanded;View section=findViewById(R.id.importPanel);if(section!=null)section.setVisibility(expanded?View.VISIBLE:View.GONE);}
    private void showPlaylistSources(){
        List<PlaylistSourceStore.Source> sources=PlaylistSourceStore.load(this);
        LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);rows.setPadding(dp(12),dp(4),dp(12),dp(8));
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(rows,new android.widget.ScrollView.LayoutParams(android.widget.ScrollView.LayoutParams.MATCH_PARENT,android.widget.ScrollView.LayoutParams.WRAP_CONTENT));
        final AlertDialog[] holder=new AlertDialog[1];
        if(sources.isEmpty()){
            TextView empty=new TextView(this);empty.setText("Chưa có nguồn IPTV. Bấm THÊM NGUỒN để tạo nguồn mới.");empty.setTextSize(16);empty.setPadding(dp(8),dp(18),dp(8),dp(18));rows.addView(empty);
        }
        for(int i=0;i<sources.size();i++){
            final int index=i;PlaylistSourceStore.Source source=sources.get(i);
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(6),dp(10),dp(6),dp(10));
            TextView nameView=new TextView(this);nameView.setText(source.name);nameView.setTextSize(18);nameView.setTextColor(getColor(R.color.text_primary));
            TextView urlView=new TextView(this);urlView.setText(source.url);urlView.setTextSize(13);urlView.setTextColor(getColor(R.color.text_secondary));urlView.setMaxLines(2);
            LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setPadding(0,dp(6),0,0);
            Button select=new Button(this);select.setText("Chọn");select.setAllCaps(false);
            Button edit=new Button(this);edit.setText("Sửa");edit.setAllCaps(false);
            Button remove=new Button(this);remove.setText("Xóa");remove.setAllCaps(false);
            LinearLayout.LayoutParams actionParams=new LinearLayout.LayoutParams(0,dp(46),1f);actionParams.setMarginEnd(dp(6));
            actions.addView(select,actionParams);actions.addView(edit,actionParams);LinearLayout.LayoutParams lastParams=new LinearLayout.LayoutParams(0,dp(46),1f);actions.addView(remove,lastParams);
            row.addView(nameView);row.addView(urlView);row.addView(actions);rows.addView(row,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
            select.setOnClickListener(v->{holder[0].dismiss();inputUrl.setText(source.url);loadFromUrl();});
            edit.setOnClickListener(v->{holder[0].dismiss();showSourceEditor(source,index);});
            remove.setOnClickListener(v->{holder[0].dismiss();confirmDeleteSource(source,index);});
        }
        holder[0]=new AlertDialog.Builder(this).setTitle("Quản lý nguồn IPTV").setView(scroll)
                .setPositiveButton("Thêm nguồn",(d,w)->showSourceEditor(null,-1))
                .setNegativeButton("Đóng",null).create();
        holder[0].show();
    }
    private void showSourceEditor(PlaylistSourceStore.Source source,int index){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(22),dp(8),dp(22),0);
        EditText nameInput=new EditText(this);nameInput.setHint("Tên nguồn (có thể để trống)");nameInput.setSingleLine(true);
        EditText urlInput=new EditText(this);urlInput.setHint("https://.../playlist.m3u");urlInput.setSingleLine(true);urlInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);
        if(source!=null){nameInput.setText(source.name);urlInput.setText(source.url);}
        form.addView(nameInput,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        form.addView(urlInput,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(source==null?"Thêm nguồn IPTV":"Chỉnh sửa nguồn IPTV").setView(form)
                .setPositiveButton("Lưu",null).setNegativeButton("Hủy",null).create();
        dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
            try{
                String name=nameInput.getText().toString(),url=urlInput.getText().toString();
                if(source==null)PlaylistSourceStore.add(this,name,url);else PlaylistSourceStore.update(this,index,name,url);
                dialog.dismiss();toast(source==null?"Đã thêm nguồn":"Đã cập nhật nguồn");showPlaylistSources();
            }catch(Exception e){urlInput.setError(readable(e));urlInput.requestFocus();}
        }));
        dialog.show();
    }
    private void confirmDeleteSource(PlaylistSourceStore.Source source,int index){
        new AlertDialog.Builder(this).setTitle("Xóa nguồn IPTV?").setMessage(source.name+"\n"+source.url)
                .setPositiveButton("Xóa",(d,w)->{try{PlaylistSourceStore.remove(this,index);toast("Đã xóa nguồn");showPlaylistSources();}catch(Exception e){showError("Không xóa được nguồn: "+readable(e));}})
                .setNegativeButton("Hủy",null).show();
    }
    private void playDirect(){String source=inputUrl.getText().toString().trim();if(source.isEmpty()){toast("Nhập URL để phát");return;}Channel c=new Channel("URL trực tiếp","Trực tiếp",source,"","",java.util.Collections.emptyMap());play(c);}
    private void showSettings(){
        boolean tv=AppPreferences.isTvInterface(this);
        String mode=AppPreferences.interfaceMode(this);
        String modeLabel="tv".equals(mode)?"TV":"mobile".equals(mode)?"Mobile":"Tự động";
        String urls=AppPreferences.showUrls(this)?"Ẩn URL trong danh sách":"Hiện URL trong danh sách";
        String rows=AppPreferences.compactRows(this)?"Hàng kênh thoải mái":"Hàng kênh thu gọn";
        String fps=AppPreferences.showFps(this)?"Ẩn FPS khi xem":"Hiện FPS khi xem";
        String clock=AppPreferences.showClock(this)?"Ẩn đồng hồ khi xem":"Hiện đồng hồ khi xem";
        String playerSource=AppPreferences.showPlayerSource(this)?"Ẩn nguồn phát khi xem":"Hiện nguồn phát khi xem";
        String background=AppPreferences.backgroundPlayback(this)?"Tắt phát nền khi khóa màn hình/nhấn Home":"Bật phát nền khi khóa màn hình/nhấn Home";
        List<String> items=new ArrayList<>(java.util.Arrays.asList("Giao diện: "+modeLabel,"Đổi hình nền",urls,rows,fps,clock,playerSource));
        final int backgroundIndex;if(tv)backgroundIndex=-1;else{backgroundIndex=items.size();items.add(background);}
        final int recentIndex=items.size();items.add("Xóa lịch sử Gần đây");
        final int aboutIndex=items.size();items.add("Thông tin ứng dụng");
        new AlertDialog.Builder(this).setTitle("Tùy chọn ứng dụng")
                .setItems(items.toArray(new String[0]),(dialog,which)->{
                    if(which==0)chooseInterfaceMode();
                    if(which==1)chooseWallpaper();
                    if(which==2){AppPreferences.setShowUrls(this,!AppPreferences.showUrls(this));adapter.notifyDataSetChanged();}
                    if(which==3){AppPreferences.setCompactRows(this,!AppPreferences.compactRows(this));adapter.notifyDataSetChanged();}
                    if(which==4)AppPreferences.setShowFps(this,!AppPreferences.showFps(this));
                    if(which==5)AppPreferences.setShowClock(this,!AppPreferences.showClock(this));
                    if(which==6)AppPreferences.setShowPlayerSource(this,!AppPreferences.showPlayerSource(this));
                    if(which==backgroundIndex){boolean enabled=!AppPreferences.backgroundPlayback(this);AppPreferences.setBackgroundPlayback(this,enabled);if(!enabled)stopService(new Intent(this,BackgroundPlaybackService.class));if(enabled&&android.os.Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},104);toast(enabled?"Đã bật phát nền":"Đã tắt phát nền");}
                    if(which==recentIndex){AppPreferences.clearRecent(this);if(activeSection==2)filter();toast("Đã xóa lịch sử");}
                    if(which==aboutIndex)showAbout();
                }).setNegativeButton("Đóng",null).show();
    }
    private void chooseInterfaceMode(){
        String[] labels={"Tự động theo thiết bị","Mobile — cảm ứng","TV — điều khiển D-pad"};
        String[] values={"auto","mobile","tv"};
        new AlertDialog.Builder(this).setTitle("Chọn giao diện")
                .setSingleChoiceItems(labels,java.util.Arrays.asList(values).indexOf(AppPreferences.interfaceMode(this)),(dialog,which)->{
                    AppPreferences.setInterfaceMode(this,values[which]);dialog.dismiss();recreate();
                }).setNegativeButton("Đóng",null).show();
    }
    private void applyInterfaceMode(ListView list){
        boolean tv=AppPreferences.isTvInterface(this);
        list.setDividerHeight(dp(tv?9:5));
        if(tv)findViewById(R.id.btnAllChannels).post(()->findViewById(R.id.btnAllChannels).requestFocus());
    }
    private void chooseWallpaper(){
        new AlertDialog.Builder(this).setTitle("Hình nền")
                .setItems(new String[]{"Xanh đêm","Biển sâu","Tím","Chọn ảnh trên máy"},(dialog,which)->{
                    if(which==3){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/*");startActivityForResult(intent,PICK_WALLPAPER);}
                    else{WallpaperStore.setStyle(this,which);applyWallpaper();}
                }).setNegativeButton("Đóng",null).show();
    }
    private void applyWallpaper(){
        int generation=++wallpaperGeneration;
        io.execute(()->{android.graphics.drawable.Drawable background=WallpaperStore.load(getApplicationContext());ui(()->{if(generation==wallpaperGeneration)findViewById(R.id.mainRoot).setBackground(background);});});
    }
    private void showAbout(){new AlertDialog.Builder(this).setTitle("Nm7 IPTV Player").setMessage("Trình phát IPTV cho Android.").setPositiveButton("Đóng",null).show();}
    private void showChannelActions(Channel c){new AlertDialog.Builder(this).setTitle(c.name()).setItems(new String[]{AppPreferences.isFavorite(this,c)?"Bỏ Yêu thích":"Thêm vào Yêu thích","Phát"},(d,w)->{if(w==0){AppPreferences.toggleFavorite(this,c);filter();}else play(c);}).show();}
    private void ui(Runnable r){runOnUiThread(r);}
    private void showError(String m){setLoading(false);new AlertDialog.Builder(this).setTitle("Lỗi").setMessage(m).setPositiveButton("Đóng",null).show();}
    private String readable(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
    private void toast(String m){Toast.makeText(this,m,Toast.LENGTH_SHORT).show();}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
}
