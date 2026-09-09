package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.LinearLayout;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.VideoSize;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.AspectRatioFrameLayout;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.text.SimpleDateFormat;
import java.util.Date;

@UnstableApi
public final class PlayerActivity extends Activity {
    public static final String EXTRA_NAME = "name", EXTRA_URL = "url";
    public static final String EXTRA_USER_AGENT = "user_agent", EXTRA_REFERER = "referer", EXTRA_ORIGIN = "origin";
    public static final String EXTRA_HEADERS = "headers", EXTRA_MIME = "mime", EXTRA_OPTIONS = "options";
    private ExoPlayer player;
    private PlayerView playerView;
    private TextView status;
    private TextView fpsView;
    private String url, name, mime;
    private String drmSystem = "", drmLicense = "";
    private ArrayList<String> options = new ArrayList<>();
    private Bundle currentHeaders = new Bundle();
    private long position;
    private boolean resumePlayback = true;
    private int quality = Integer.MAX_VALUE;
    private int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private WifiManager.MulticastLock multicastLock;
    private DecoderCounters videoCounters;
    private final FpsMeter fpsMeter = new FpsMeter();
    private final ExecutorService drmIo = Executors.newSingleThreadExecutor();
    private boolean resolvingClearKey;
    private boolean activityStarted;
    private final Handler fpsHandler = new Handler(Looper.getMainLooper());
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private TextView clockView;
    private View quickPanel;
    private LinearLayout quickGroupRow;
    private ListView quickChannelList;
    private TextView quickEmpty;
    private QuickChannelAdapter quickAdapter;
    private final List<Channel> quickChannels = new ArrayList<>();
    private String quickGroup = "";
    private int consumedRemoteKey = KeyEvent.KEYCODE_UNKNOWN;
    private final Runnable clockUpdate = new Runnable() {
        @Override public void run() {
            if (clockView != null) clockView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            clockHandler.postDelayed(this, 30_000);
        }
    };
    private final Runnable fpsUpdate = new Runnable() {
        @Override public void run() {
            if (player != null && videoCounters != null) {
                double fps = fpsMeter.sample(android.os.SystemClock.elapsedRealtime(),
                        videoCounters.renderedOutputBufferCount, player.isPlaying());
                fpsView.setText(Double.isNaN(fps) ? "FPS: đang đo…" : String.format(Locale.ROOT, "FPS thực tế: %.1f", fps));
            } else fpsView.setText("FPS: chưa có hình");
            fpsHandler.postDelayed(this, 2000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_player);
        Insets.apply(findViewById(R.id.playerRoot));
        url = value(EXTRA_URL); name = value(EXTRA_NAME); mime = value(EXTRA_MIME);
        ArrayList<String> passedOptions = getIntent().getStringArrayListExtra(EXTRA_OPTIONS);
        if (passedOptions != null) options = passedOptions;
        Bundle passedHeaders = getIntent().getBundleExtra(EXTRA_HEADERS);
        if (passedHeaders != null) currentHeaders = passedHeaders;
        DrmSpec initialDrm = DrmSpec.fromOptions(options); drmSystem = initialDrm.system; drmLicense = initialDrm.license;
        if (state != null) {
            position = state.getLong("position"); resumePlayback = state.getBoolean("playing", true);
            quality = state.getInt("quality", Integer.MAX_VALUE); resizeMode = state.getInt("resize", AspectRatioFrameLayout.RESIZE_MODE_FIT);
            mime = state.getString("mime", mime); drmSystem = state.getString("drm_system", drmSystem); drmLicense = state.getString("drm_license", drmLicense);
        }
        playerView = findViewById(R.id.playerView); status = findViewById(R.id.txtPlayerStatus); fpsView = findViewById(R.id.txtFps); clockView = findViewById(R.id.txtClock);
        quickPanel = findViewById(R.id.quickChannelPanel); quickGroupRow = findViewById(R.id.quickGroupRow); quickChannelList = findViewById(R.id.quickChannelList); quickEmpty = findViewById(R.id.txtQuickEmpty);
        quickAdapter = new QuickChannelAdapter(this); quickChannelList.setAdapter(quickAdapter); quickChannelList.setEmptyView(quickEmpty);
        quickChannelList.setOnItemClickListener((parent, view, p, id) -> switchChannel(quickAdapter.getItem(p)));
        fpsView.setVisibility(AppPreferences.showFps(this) ? View.VISIBLE : View.GONE); clockView.setVisibility(AppPreferences.showClock(this) ? View.VISIBLE : View.GONE);
        playerView.setResizeMode(resizeMode); ((TextView) findViewById(R.id.txtPlayerTitle)).setText(name);
        TextView source = findViewById(R.id.txtPlayerUrl); source.setText(url); source.setVisibility(AppPreferences.showPlayerSource(this) ? View.VISIBLE : View.GONE); source.setOnClickListener(v -> showSource());
        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> findViewById(R.id.playerHeader).setVisibility(visibility));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish()); findViewById(R.id.btnQuality).setOnClickListener(v -> chooseQuality());
        findViewById(R.id.btnFormat).setOnClickListener(v -> chooseFormat()); findViewById(R.id.btnRotate).setOnClickListener(v -> chooseOrientation());
        findViewById(R.id.btnDrm).setOnClickListener(v -> configureDrm()); findViewById(R.id.btnResize).setOnClickListener(v -> chooseResizeMode());
        findViewById(R.id.btnRetry).setOnClickListener(v -> { position = 0; resumePlayback = true; releasePlayer(); startPlayer(); }); loadQuickChannels();
    }
    @Override protected void onStart() { super.onStart(); activityStarted = true; fpsHandler.post(fpsUpdate); clockHandler.post(clockUpdate); startPlayer(); }
    private void startPlayer() {
        if (player != null) return; String scheme = Uri.parse(url).getScheme();
        if (scheme == null || !(scheme.matches("(?i)https?|rtsp|udp|rtmp"))) { showError("Bản này chưa hỗ trợ giao thức " + scheme + ". Không thể bảo đảm mọi giao thức IPTV."); return; }
        try {
            Map<String,String> headers = new LinkedHashMap<>(); if (currentHeaders != null) for (String key : currentHeaders.keySet()) { String v=currentHeaders.getString(key); if(v!=null) headers.put(key,v); }
            String ua=headers.containsKey("User-Agent")?headers.get("User-Agent"):"Nm7-IPTV/1.7 Android";
            DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent(ua).setConnectTimeoutMs(15000).setReadTimeoutMs(20000).setDefaultRequestProperties(headers);
            DefaultDataSource.Factory data=new DefaultDataSource.Factory(this,http); DefaultMediaSourceFactory mediaFactory=new DefaultMediaSourceFactory(data);
            MediaItem.Builder builder=new MediaItem.Builder().setUri(url); String inferred=mime.isEmpty()?StreamSpec.inferMime(url,options):mime; if(inferred!=null&&!inferred.isEmpty()) builder.setMimeType(inferred);
            DrmSpec drm=DrmSpec.create(drmSystem,drmLicense); findViewById(R.id.btnDrm).setVisibility(drm.hasDrm()?View.VISIBLE:View.GONE); if(drm.remoteClearKey()){resolveRemoteClearKey(drm,headers);return;} DrmPlayback.configure(drm,builder,mediaFactory);
            player=new ExoPlayer.Builder(this,new DefaultRenderersFactory(this).setEnableDecoderFallback(true)).setMediaSourceFactory(mediaFactory).build();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true); player.setHandleAudioBecomingNoisy(true); playerView.setPlayer(player);
            player.addAnalyticsListener(new AnalyticsListener(){@Override public void onVideoEnabled(EventTime e,DecoderCounters c){videoCounters=c;fpsMeter.reset();}}); applyQuality();
            player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException e){showError(e.getErrorCodeName()+"\nKiểm tra URL, quyền truy cập, kết nối mạng và codec của thiết bị.");}@Override public void onVideoSizeChanged(VideoSize s){status.setText(s.width+" × "+s.height+" • độ phân giải thực tế");}@Override public void onPlaybackStateChanged(int s){if(s==Player.STATE_BUFFERING)status.setText("Đang tải luồng…");if(s==Player.STATE_READY){findViewById(R.id.playerError).setVisibility(View.GONE);VideoSize v=player.getVideoSize();status.setText(v.width>0?v.width+" × "+v.height+" • độ phân giải thực tế":"Đang phát âm thanh");}}});
            MediaItem item=builder.build(); if("rtsp".equalsIgnoreCase(scheme)) player.setMediaSource(new RtspMediaSource.Factory().setForceUseRtpTcp(true).setUserAgent(ua).createMediaSource(item)); else {if("udp".equalsIgnoreCase(scheme))acquireMulticast();player.setMediaItem(item);} if(position>0)player.seekTo(position); player.prepare();player.setPlayWhenReady(resumePlayback);
        } catch(Exception error){releasePlayer();String m=error.getMessage();showError(m==null?"Không mở được nguồn phát: "+error.getClass().getSimpleName():m);}
    }
    private void resolveRemoteClearKey(DrmSpec drm, Map<String,String> streamHeaders) { if(resolvingClearKey)return;resolvingClearKey=true;status.setText("Đang lấy giấy phép ClearKey…");drmIo.execute(()->{HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(drm.license).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(true);c.setRequestMethod("GET");for(Map.Entry<String,String> e:drm.headers.entrySet())c.setRequestProperty(e.getKey(),e.getValue());String sh=Uri.parse(url).getHost(),lh=Uri.parse(drm.license).getHost();if(sh!=null&&sh.equalsIgnoreCase(lh))for(String n:new String[]{"User-Agent","Referer","Origin","Cookie"}){String v=streamHeaders.get(n);if(v!=null&&!v.isEmpty()&&!drm.headers.containsKey(n))c.setRequestProperty(n,v);}if(c.getRequestProperty("User-Agent")==null)c.setRequestProperty("User-Agent","Dalvik/2.1.0");int code=c.getResponseCode();if(code<200||code>=300)throw new IllegalArgumentException("HTTP "+code);ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n,total=0;while((n=in.read(b))>=0){total+=n;if(total>65536)throw new IllegalArgumentException("phản hồi quá lớn");out.write(b,0,n);}}String r=out.toString("UTF-8").trim();DrmPlayback.clearKeyResponse(r);runOnUiThread(()->{resolvingClearKey=false;drmLicense=r;if(activityStarted&&!isFinishing()&&!isDestroyed())startPlayer();});}catch(Exception e){runOnUiThread(()->{resolvingClearKey=false;showError("Không lấy được giấy phép ClearKey bằng GET. Kiểm tra token hoặc quyền truy cập nguồn.");});}finally{if(c!=null)c.disconnect();}}); }
    private void chooseQuality(){String[] l={"Tự động / tối đa theo thiết bị","Full HD — tối đa 1080p","2K/QHD — tối đa 1440p","4K UHD — tối đa 2160p"};int[] h={Integer.MAX_VALUE,1080,1440,2160};new AlertDialog.Builder(this).setTitle("Giới hạn chất lượng").setItems(l,(d,i)->{quality=h[i];applyQuality();}).setNegativeButton("Đóng",null).show();}
    private void applyQuality(){if(player==null)return;int w=quality==Integer.MAX_VALUE?Integer.MAX_VALUE:quality*16/9;player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setViewportSize(Integer.MAX_VALUE,Integer.MAX_VALUE,true).setMaxVideoSize(w,quality).build());}
    private void chooseFormat(){String[] l={"Tự nhận diện","HLS / M3U8","DASH / MPD","SmoothStreaming","MPEG-TS"};String[] t={"",MimeTypes.APPLICATION_M3U8,MimeTypes.APPLICATION_MPD,MimeTypes.APPLICATION_SS,MimeTypes.VIDEO_MP2T};new AlertDialog.Builder(this).setTitle("Định dạng nguồn (khi URL không có đuôi)").setItems(l,(d,i)->{mime=t[i];position=0;resumePlayback=true;releasePlayer();startPlayer();}).setNegativeButton("Đóng",null).show();}
    private void chooseOrientation(){String[] l={"Tự động theo điện thoại","Màn hình ngang","Màn hình dọc"};int[] v={ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT};new AlertDialog.Builder(this).setTitle("Xoay màn hình khi xem").setItems(l,(d,i)->setRequestedOrientation(v[i])).setNegativeButton("Đóng",null).show();}
    private void chooseResizeMode(){String[] l={"Vừa màn hình (Fit)","Phóng đầy màn hình (Zoom)","Kéo đầy khung (Fill)"};int[] v={AspectRatioFrameLayout.RESIZE_MODE_FIT,AspectRatioFrameLayout.RESIZE_MODE_ZOOM,AspectRatioFrameLayout.RESIZE_MODE_FILL};new AlertDialog.Builder(this).setTitle("Tỷ lệ và khung hình").setItems(l,(d,i)->{resizeMode=v[i];playerView.setResizeMode(resizeMode);}).setNegativeButton("Đóng",null).show();}
    private void configureDrm(){LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(32,8,32,0);android.widget.Spinner type=new android.widget.Spinner(this);String[] values={"Widevine","ClearKey","PlayReady (Android TV)"};type.setAdapter(new android.widget.ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values));if("clearkey".equals(drmSystem))type.setSelection(1);if("playready".equals(drmSystem))type.setSelection(2);EditText license=new EditText(this);license.setHint("URL giấy phép hoặc ClearKey KID:KEY");license.setSingleLine(false);license.setMaxLines(4);license.setText(drmLicense);f.addView(type);f.addView(license);new AlertDialog.Builder(this).setTitle("DRM do nhà cung cấp cấp").setMessage("Không nhập khóa hoặc giấy phép bạn không có quyền sử dụng. Dữ liệu này chỉ giữ trong màn hình phát hiện tại.").setView(f).setPositiveButton("Áp dụng",(d,w)->{drmSystem=new String[]{"widevine","clearkey","playready"}[type.getSelectedItemPosition()];drmLicense=license.getText().toString().trim();position=0;resumePlayback=true;releasePlayer();startPlayer();}).setNeutralButton("Tắt DRM",(d,w)->{drmSystem="";drmLicense="";releasePlayer();startPlayer();}).setNegativeButton("Đóng",null).show();}
    private void loadQuickChannels(){SessionStore.IO.execute(()->{try{SessionStore.State s=SessionStore.load(getApplicationContext());runOnUiThread(()->{if(isFinishing()||isDestroyed())return;quickChannels.clear();if(s!=null)quickChannels.addAll(s.result.channels);rebuildQuickGroups();filterQuickChannels();});}catch(Exception ignored){runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())filterQuickChannels();});}});}
    private void rebuildQuickGroups(){Set<String> u=new LinkedHashSet<>();for(Channel c:quickChannels)u.add(c.group());List<String> g=new ArrayList<>(u);g.sort(String.CASE_INSENSITIVE_ORDER);if(!quickGroup.isEmpty()&&!u.contains(quickGroup))quickGroup="";quickGroupRow.removeAllViews();addQuickGroup("Tất cả","");for(String x:g)addQuickGroup(x,x);updateQuickGroupButtons();}
    private void addQuickGroup(String l,String v){Button b=new Button(this);b.setTag(v);b.setText(l);b.setAllCaps(false);b.setTextSize(12);b.setSingleLine(true);b.setFocusable(true);b.setFocusableInTouchMode(false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,dp(44));p.setMarginEnd(dp(7));quickGroupRow.addView(b,p);b.setOnClickListener(x->{quickGroup=(String)v;updateQuickGroupButtons();filterQuickChannels();if(quickAdapter.getCount()>0)quickChannelList.requestFocus();});}
    private void updateQuickGroupButtons(){for(int i=0;i<quickGroupRow.getChildCount();i++){View c=quickGroupRow.getChildAt(i);boolean a=quickGroup.equals(c.getTag());c.setSelected(a);c.setBackgroundResource(a?R.drawable.button_primary:R.drawable.button_secondary);if(c instanceof Button)((Button)c).setTextColor(getColor(a?R.color.navy:R.color.text_primary));}}
    private void filterQuickChannels(){List<Channel> f=new ArrayList<>();for(Channel c:quickChannels)if(quickGroup.isEmpty()||quickGroup.equals(c.group()))f.add(c);String id=currentChannelId();quickAdapter.submit(f,id);int cur=0;for(int i=0;i<f.size();i++)if(AppPreferences.id(f.get(i)).equals(id)){cur=i;break;}if(!f.isEmpty())quickChannelList.setSelection(cur);}
    private String currentChannelId(){for(Channel c:quickChannels)if(isCurrentChannel(c))return AppPreferences.id(c);return "";}
    private boolean isCurrentChannel(Channel c){if(!c.url().equals(url)||!c.options().equals(options))return false;if(currentHeaders.size()!=c.headers().size())return false;for(Map.Entry<String,String> e:c.headers().entrySet())if(!e.getValue().equals(currentHeaders.getString(e.getKey())))return false;return true;}
    private void showQuickChannels(){quickPanel.setVisibility(View.VISIBLE);filterQuickChannels();if(quickAdapter.getCount()>0)quickChannelList.post(()->quickChannelList.requestFocus());else if(quickGroupRow.getChildCount()>0)quickGroupRow.getChildAt(0).requestFocus();}
    private void hideQuickChannels(){quickPanel.setVisibility(View.GONE);playerView.requestFocus();}
    private void switchChannel(Channel c){if(c==null)return;hideQuickChannels();if(isCurrentChannel(c))return;name=c.name();url=c.url();mime=c.mimeHint();options=new ArrayList<>(c.options());DrmSpec d=DrmSpec.fromOptions(options);drmSystem=d.system;drmLicense=d.license;currentHeaders=new Bundle();for(Map.Entry<String,String> e:c.headers().entrySet())currentHeaders.putString(e.getKey(),e.getValue());getIntent().putExtra(EXTRA_NAME,name);getIntent().putExtra(EXTRA_URL,url);getIntent().putExtra(EXTRA_HEADERS,currentHeaders);getIntent().putExtra(EXTRA_MIME,mime);getIntent().putStringArrayListExtra(EXTRA_OPTIONS,options);((TextView)findViewById(R.id.txtPlayerTitle)).setText(name);((TextView)findViewById(R.id.txtPlayerUrl)).setText(url);findViewById(R.id.playerError).setVisibility(View.GONE);AppPreferences.recordRecent(this,c);position=0;resumePlayback=true;resolvingClearKey=false;releasePlayer();filterQuickChannels();startPlayer();}
    private boolean switchRelative(int delta){for(int i=0;i<quickChannels.size();i++)if(isCurrentChannel(quickChannels.get(i))){switchChannel(quickChannels.get((i+delta+quickChannels.size())%quickChannels.size()));playerView.hideController();return true;}return false;}
    @Override public boolean dispatchKeyEvent(KeyEvent event){int key=event.getKeyCode();if(event.getAction()==KeyEvent.ACTION_UP&&key==consumedRemoteKey){consumedRemoteKey=KeyEvent.KEYCODE_UNKNOWN;return true;}if(event.getAction()==KeyEvent.ACTION_DOWN){if(event.getRepeatCount()>0&&key==consumedRemoteKey)return true;boolean panel=quickPanel!=null&&quickPanel.getVisibility()==View.VISIBLE;if(key==KeyEvent.KEYCODE_BACK&&panel){hideQuickChannels();consumedRemoteKey=key;return true;}if(AppPreferences.isTvInterface(this)&&!panel){boolean controller=playerView.isControllerFullyVisible();boolean handled=false;if((key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_NUMPAD_ENTER)&&!controller){playerView.showController();handled=true;}else if(!controller&&key==KeyEvent.KEYCODE_DPAD_LEFT){showQuickChannels();handled=true;}else if(!controller&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)){handled=switchRelative(key==KeyEvent.KEYCODE_DPAD_UP?1:-1);}else if(key==KeyEvent.KEYCODE_MENU){AppPreferences.setShowPlayerSource(this,!AppPreferences.showPlayerSource(this));findViewById(R.id.txtPlayerUrl).setVisibility(AppPreferences.showPlayerSource(this)?View.VISIBLE:View.GONE);handled=true;}if(handled){consumedRemoteKey=key;return true;}}}return super.dispatchKeyEvent(event);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);} private void acquireMulticast(){WifiManager wifi=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);if(wifi!=null){multicastLock=wifi.createMulticastLock("iptv-stream");multicastLock.setReferenceCounted(false);multicastLock.acquire();}}
    private void showSource(){TextView v=new TextView(this);v.setText(url);v.setTextIsSelectable(true);v.setPadding(24,16,24,16);android.widget.ScrollView s=new android.widget.ScrollView(this);s.addView(v);new AlertDialog.Builder(this).setTitle("URL nguồn").setView(s).setPositiveButton("Đóng",null).show();}
    private void showError(String m){findViewById(R.id.playerError).setVisibility(View.VISIBLE);((TextView)findViewById(R.id.txtPlayerError)).setText(m);playerView.showController();} private String value(String k){String v=getIntent().getStringExtra(k);return v==null?"":v;}
    private void rememberPosition(){if(player!=null){position=player.isCurrentMediaItemLive()?0:player.getCurrentPosition();resumePlayback=player.getPlayWhenReady();}}
    @Override protected void onSaveInstanceState(Bundle out){rememberPosition();out.putLong("position",position);out.putBoolean("playing",resumePlayback);out.putInt("quality",quality);out.putInt("resize",resizeMode);out.putString("mime",mime);super.onSaveInstanceState(out);} @Override protected void onStop(){activityStarted=false;fpsHandler.removeCallbacks(fpsUpdate);clockHandler.removeCallbacks(clockUpdate);rememberPosition();releasePlayer();super.onStop();}
    private void releasePlayer(){if(player!=null){playerView.setPlayer(null);player.release();player=null;}videoCounters=null;fpsMeter.reset();if(multicastLock!=null){if(multicastLock.isHeld())multicastLock.release();multicastLock=null;}} @Override protected void onDestroy(){drmIo.shutdownNow();releasePlayer();super.onDestroy();}
}
