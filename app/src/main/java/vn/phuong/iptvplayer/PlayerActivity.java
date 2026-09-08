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
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.EditText;
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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private long position;
    private boolean resumePlayback = true;
    private int quality = Integer.MAX_VALUE;
    private WifiManager.MulticastLock multicastLock;
    private DecoderCounters videoCounters;
    private final FpsMeter fpsMeter = new FpsMeter();
    private final ExecutorService drmIo = Executors.newSingleThreadExecutor();
    private boolean resolvingClearKey;
    private boolean activityStarted;
    private final Handler fpsHandler = new Handler(Looper.getMainLooper());
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
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_player);
        Insets.apply(findViewById(R.id.playerRoot));
        url = value(EXTRA_URL);
        name = value(EXTRA_NAME);
        mime = value(EXTRA_MIME);
        ArrayList<String> passedOptions = getIntent().getStringArrayListExtra(EXTRA_OPTIONS);
        if (passedOptions != null) options = passedOptions;
        DrmSpec initialDrm = DrmSpec.fromOptions(options);
        drmSystem = initialDrm.system;
        drmLicense = initialDrm.license;
        if (state != null) {
            position = state.getLong("position");
            resumePlayback = state.getBoolean("playing", true);
            quality = state.getInt("quality", Integer.MAX_VALUE);
            mime = state.getString("mime", mime);
            drmSystem = state.getString("drm_system", drmSystem);
            drmLicense = state.getString("drm_license", drmLicense);
        }
        playerView = findViewById(R.id.playerView);
        status = findViewById(R.id.txtPlayerStatus);
        fpsView = findViewById(R.id.txtFps);
        ((TextView) findViewById(R.id.txtPlayerTitle)).setText(name);
        TextView source = findViewById(R.id.txtPlayerUrl);
        source.setText(url);
        source.setOnClickListener(v -> showSource());
        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility ->
                findViewById(R.id.playerHeader).setVisibility(visibility));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnQuality).setOnClickListener(v -> chooseQuality());
        findViewById(R.id.btnFormat).setOnClickListener(v -> chooseFormat());
        findViewById(R.id.btnRotate).setOnClickListener(v -> chooseOrientation());
        findViewById(R.id.btnDrm).setOnClickListener(v -> configureDrm());
        findViewById(R.id.btnRetry).setOnClickListener(v -> { position = 0; resumePlayback = true; releasePlayer(); startPlayer(); });
    }

    @Override protected void onStart() {
        super.onStart();
        activityStarted = true;
        fpsHandler.post(fpsUpdate);
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
            String ua = headers.containsKey("User-Agent") ? headers.get("User-Agent") : "IPTV-Player/1.4 Android";
            DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                    .setUserAgent(ua).setConnectTimeoutMs(15000).setReadTimeoutMs(20000)
                    .setDefaultRequestProperties(headers);
            DefaultDataSource.Factory data = new DefaultDataSource.Factory(this, http);
            DefaultMediaSourceFactory mediaFactory = new DefaultMediaSourceFactory(data);
            MediaItem.Builder builder = new MediaItem.Builder().setUri(url);
            String inferred = mime.isEmpty() ? StreamSpec.inferMime(url, options) : mime;
            if (inferred != null && !inferred.isEmpty()) builder.setMimeType(inferred);
            DrmSpec drm = DrmSpec.create(drmSystem, drmLicense);
            findViewById(R.id.btnDrm).setVisibility(drm.hasDrm() ? View.VISIBLE : View.GONE);
            if (drm.remoteClearKey()) {
                resolveRemoteClearKey(drm, headers);
                return;
            }
            DrmPlayback.configure(drm, builder, mediaFactory);
            player = new ExoPlayer.Builder(this, new DefaultRenderersFactory(this).setEnableDecoderFallback(true))
                    .setMediaSourceFactory(mediaFactory).build();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
            player.setHandleAudioBecomingNoisy(true);
            playerView.setPlayer(player);
            player.addAnalyticsListener(new AnalyticsListener() {
                @Override public void onVideoEnabled(EventTime eventTime, DecoderCounters counters) {
                    videoCounters = counters; fpsMeter.reset();
                }
            });
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
            String message = error.getMessage();
            showError(message == null ? "Không mở được nguồn phát: " + error.getClass().getSimpleName() : message);
        }
    }

    private void resolveRemoteClearKey(DrmSpec drm, Map<String, String> streamHeaders) {
        if (resolvingClearKey) return;
        resolvingClearKey = true;
        status.setText("Đang lấy giấy phép ClearKey…");
        drmIo.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(drm.license).openConnection();
                connection.setConnectTimeout(15000); connection.setReadTimeout(20000);
                connection.setInstanceFollowRedirects(true); connection.setRequestMethod("GET");
                for (Map.Entry<String, String> entry : drm.headers.entrySet())
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                String streamHost = Uri.parse(url).getHost(), licenseHost = Uri.parse(drm.license).getHost();
                if (streamHost != null && streamHost.equalsIgnoreCase(licenseHost)) {
                    for (String name : new String[]{"User-Agent", "Referer", "Origin", "Cookie"}) {
                        String value = streamHeaders.get(name);
                        if (value != null && !value.isEmpty() && !drm.headers.containsKey(name))
                            connection.setRequestProperty(name, value);
                    }
                }
                if (connection.getRequestProperty("User-Agent") == null)
                    connection.setRequestProperty("User-Agent", "Dalvik/2.1.0");
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalArgumentException("HTTP " + code);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (InputStream in = connection.getInputStream()) {
                    byte[] buffer = new byte[4096]; int count, total = 0;
                    while ((count = in.read(buffer)) >= 0) {
                        total += count; if (total > 65536) throw new IllegalArgumentException("phản hồi quá lớn");
                        out.write(buffer, 0, count);
                    }
                }
                String response = out.toString("UTF-8").trim();
                DrmPlayback.clearKeyResponse(response);
                runOnUiThread(() -> {
                    resolvingClearKey = false;
                    drmLicense = response;
                    if (activityStarted && !isFinishing() && !isDestroyed()) startPlayer();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    resolvingClearKey = false;
                    showError("Không lấy được giấy phép ClearKey bằng GET. Kiểm tra token hoặc quyền truy cập nguồn.");
                });
            } finally { if (connection != null) connection.disconnect(); }
        });
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

    private void chooseOrientation() {
        String[] labels = {"Tự động theo điện thoại", "Màn hình ngang", "Màn hình dọc"};
        int[] values = {ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR,
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT};
        new AlertDialog.Builder(this).setTitle("Xoay màn hình khi xem")
                .setItems(labels, (dialog, which) -> setRequestedOrientation(values[which]))
                .setNegativeButton("Đóng", null).show();
    }

    private void configureDrm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL); form.setPadding(32, 8, 32, 0);
        android.widget.Spinner type = new android.widget.Spinner(this);
        String[] values = {"Widevine", "ClearKey", "PlayReady (Android TV)"};
        type.setAdapter(new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
        if ("clearkey".equals(drmSystem)) type.setSelection(1);
        if ("playready".equals(drmSystem)) type.setSelection(2);
        EditText license = new EditText(this);
        license.setHint("URL giấy phép hoặc ClearKey KID:KEY");
        license.setSingleLine(false); license.setMaxLines(4); license.setText(drmLicense);
        form.addView(type); form.addView(license);
        new AlertDialog.Builder(this).setTitle("DRM do nhà cung cấp cấp")
                .setMessage("Không nhập khóa hoặc giấy phép bạn không có quyền sử dụng. Dữ liệu này chỉ giữ trong màn hình phát hiện tại.")
                .setView(form).setPositiveButton("Áp dụng", (dialog, which) -> {
                    drmSystem = new String[]{"widevine", "clearkey", "playready"}[type.getSelectedItemPosition()];
                    drmLicense = license.getText().toString().trim();
                    position = 0; resumePlayback = true; releasePlayer(); startPlayer();
                }).setNeutralButton("Tắt DRM", (dialog, which) -> {
                    drmSystem = ""; drmLicense = ""; releasePlayer(); startPlayer();
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
        activityStarted = false;
        fpsHandler.removeCallbacks(fpsUpdate);
        rememberPosition();
        releasePlayer();
        super.onStop();
    }

    private void releasePlayer() {
        if (player != null) { playerView.setPlayer(null); player.release(); player = null; }
        videoCounters = null; fpsMeter.reset();
        if (multicastLock != null) {
            if (multicastLock.isHeld()) multicastLock.release();
            multicastLock = null;
        }
    }

    @Override protected void onDestroy() {
        drmIo.shutdownNow();
        releasePlayer();
        super.onDestroy();
    }
}
