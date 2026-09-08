package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
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
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@UnstableApi
public final class PlayerActivity extends Activity {
    public static final String EXTRA_NAME = "name", EXTRA_URL = "url";
    public static final String EXTRA_USER_AGENT = "user_agent", EXTRA_REFERER = "referer", EXTRA_ORIGIN = "origin";
    public static final String EXTRA_HEADERS = "headers", EXTRA_MIME = "mime";
    private ExoPlayer player;
    private PlayerView playerView;
    private TextView status;
    private String url, name, mime;
    private long position;
    private boolean resumePlayback = true;
    private int quality = Integer.MAX_VALUE;
    private WifiManager.MulticastLock multicastLock;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_player);
        Insets.apply(findViewById(R.id.playerRoot));
        url = value(EXTRA_URL);
        name = value(EXTRA_NAME);
        mime = value(EXTRA_MIME);
        if (state != null) {
            position = state.getLong("position");
            resumePlayback = state.getBoolean("playing", true);
            quality = state.getInt("quality", Integer.MAX_VALUE);
            mime = state.getString("mime", mime);
        }
        playerView = findViewById(R.id.playerView);
        status = findViewById(R.id.txtPlayerStatus);
        ((TextView) findViewById(R.id.txtPlayerTitle)).setText(name);
        TextView source = findViewById(R.id.txtPlayerUrl);
        source.setText(url);
        source.setOnClickListener(v -> showSource());
        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility ->
                findViewById(R.id.playerHeader).setVisibility(visibility));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnQuality).setOnClickListener(v -> chooseQuality());
        findViewById(R.id.btnFormat).setOnClickListener(v -> chooseFormat());
        findViewById(R.id.btnRetry).setOnClickListener(v -> { position = 0; resumePlayback = true; releasePlayer(); startPlayer(); });
    }

    @Override protected void onStart() {
        super.onStart();
        startPlayer();
    }

    private void startPlayer() {
        if (player != null) return;
        String scheme = Uri.parse(url).getScheme();
        if (scheme == null || !(scheme.matches("(?i)https?|rtsp|udp|rtmp"))) {
            showError("Bản này chưa hỗ trợ giao thức " + scheme + ". Không thể bảo đảm mọi giao thức IPTV.");
            return;
        }
        try {
            Map<String, String> headers = new LinkedHashMap<>();
            Bundle bundle = getIntent().getBundleExtra(EXTRA_HEADERS);
            if (bundle != null) for (String key : bundle.keySet()) {
                String value = bundle.getString(key);
                if (value != null) headers.put(key, value);
            }
            String ua = headers.containsKey("User-Agent") ? headers.get("User-Agent") : "IPTV-Player/1.1 Android";
            DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                    .setUserAgent(ua).setConnectTimeoutMs(15000).setReadTimeoutMs(20000)
                    .setDefaultRequestProperties(headers);
            DefaultDataSource.Factory data = new DefaultDataSource.Factory(this, http);
            player = new ExoPlayer.Builder(this, new DefaultRenderersFactory(this).setEnableDecoderFallback(true))
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(data)).build();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
            player.setHandleAudioBecomingNoisy(true);
            playerView.setPlayer(player);
            applyQuality();
            player.addListener(new Player.Listener() {
                @Override public void onPlayerError(PlaybackException error) {
                    showError(error.getErrorCodeName() + "\nKiểm tra URL, quyền truy cập, kết nối mạng và codec của thiết bị.");
                }
                @Override public void onVideoSizeChanged(VideoSize size) {
                    status.setText(size.width + " × " + size.height + " • độ phân giải thực tế");
                }
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_BUFFERING) status.setText("Đang tải luồng…");
                    if (state == Player.STATE_READY) {
                        findViewById(R.id.playerError).setVisibility(View.GONE);
                        VideoSize size = player.getVideoSize();
                        status.setText(size.width > 0 ? size.width + " × " + size.height + " • độ phân giải thực tế" : "Đang phát âm thanh");
                    }
                }
            });
            MediaItem.Builder builder = new MediaItem.Builder().setUri(url);
            String inferred = mime.isEmpty() ? inferMime(url) : mime;
            if (inferred != null && !inferred.isEmpty()) builder.setMimeType(inferred);
            MediaItem item = builder.build();
            if ("rtsp".equalsIgnoreCase(scheme)) {
                player.setMediaSource(new RtspMediaSource.Factory().setForceUseRtpTcp(true).setUserAgent(ua).createMediaSource(item));
            } else {
                if ("udp".equalsIgnoreCase(scheme)) acquireMulticast();
                player.setMediaItem(item);
            }
            if (position > 0) player.seekTo(position);
            player.prepare();
            player.setPlayWhenReady(resumePlayback);
        } catch (Exception error) {
            releasePlayer();
            showError("Không mở được nguồn phát: " + error.getClass().getSimpleName());
        }
    }

    private void chooseQuality() {
        String[] labels = {"Tự động / tối đa theo thiết bị", "Full HD — tối đa 1080p", "2K/QHD — tối đa 1440p", "4K UHD — tối đa 2160p"};
        int[] heights = {Integer.MAX_VALUE, 1080, 1440, 2160};
        new AlertDialog.Builder(this).setTitle("Giới hạn chất lượng")
                .setItems(labels, (dialog, index) -> { quality = heights[index]; applyQuality(); })
                .setNegativeButton("Đóng", null).show();
    }

    private void applyQuality() {
        if (player == null) return;
        int width = quality == Integer.MAX_VALUE ? Integer.MAX_VALUE : quality * 16 / 9;
        player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon()
                .setViewportSize(Integer.MAX_VALUE, Integer.MAX_VALUE, true)
                .setMaxVideoSize(width, quality).build());
    }

    private void chooseFormat() {
        String[] labels = {"Tự nhận diện", "HLS / M3U8", "DASH / MPD", "SmoothStreaming", "MPEG-TS"};
        String[] types = {"", MimeTypes.APPLICATION_M3U8, MimeTypes.APPLICATION_MPD, MimeTypes.APPLICATION_SS, MimeTypes.VIDEO_MP2T};
        new AlertDialog.Builder(this).setTitle("Định dạng nguồn (khi URL không có đuôi)")
                .setItems(labels, (dialog, index) -> {
                    mime = types[index]; position = 0; resumePlayback = true;
                    releasePlayer(); startPlayer();
                }).setNegativeButton("Đóng", null).show();
    }

    private void acquireMulticast() {
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifi != null) {
            multicastLock = wifi.createMulticastLock("iptv-stream");
            multicastLock.setReferenceCounted(false);
            multicastLock.acquire();
        }
    }

    private void showSource() {
        TextView view = new TextView(this);
        view.setText(url); view.setTextIsSelectable(true); view.setPadding(24, 16, 24, 16);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(view);
        new AlertDialog.Builder(this).setTitle("URL nguồn").setView(scroll).setPositiveButton("Đóng", null).show();
    }

    private void showError(String message) {
        findViewById(R.id.playerError).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.txtPlayerError)).setText(message);
        playerView.showController();
    }

    private String inferMime(String value) {
        String path = Uri.parse(value).getPath();
        String lower = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".m3u8") || lower.endsWith(".m3u")) return MimeTypes.APPLICATION_M3U8;
        if (lower.endsWith(".mpd")) return MimeTypes.APPLICATION_MPD;
        if (lower.contains(".ism/manifest") || lower.contains(".isml/manifest")) return MimeTypes.APPLICATION_SS;
        return null;
    }

    private String value(String key) {
        String value = getIntent().getStringExtra(key);
        return value == null ? "" : value;
    }

    private void rememberPosition() {
        if (player != null) {
            position = player.isCurrentMediaItemLive() ? 0 : player.getCurrentPosition();
            resumePlayback = player.getPlayWhenReady();
        }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        rememberPosition();
        out.putLong("position", position);
        out.putBoolean("playing", resumePlayback);
        out.putInt("quality", quality);
        out.putString("mime", mime);
        super.onSaveInstanceState(out);
    }

    @Override protected void onStop() {
        rememberPosition();
        releasePlayer();
        super.onStop();
    }

    private void releasePlayer() {
        if (player != null) { playerView.setPlayer(null); player.release(); player = null; }
        if (multicastLock != null) {
            if (multicastLock.isHeld()) multicastLock.release();
            multicastLock = null;
        }
    }

    @Override protected void onDestroy() {
        releasePlayer();
        super.onDestroy();
    }
}
