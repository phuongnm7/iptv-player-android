package vn.phuong.iptvplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SearchEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.DecoderCounters;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Mobile inline player with DVR seekbar, reliable full-screen rotation and background playback. */
@UnstableApi
public final class MobileInlinePlayerProviderV2 extends ContentProvider implements Application.ActivityLifecycleCallbacks {
    private MainActivity currentActivity;
    private ExoPlayer player;
    private PlayerView playerView;
    private FrameLayout videoContainer;
    private FrameLayout fullscreenHost;
    private LinearLayout panel;
    private TextView title;
    private TextView programme;
    private TextView stats;
    private Button rotateButton;
    private ListView channelList;
    private AdapterView.OnItemClickListener originalClick;
    private Channel currentChannel;
    private DecoderCounters videoCounters;
    private final FpsMeter fpsMeter = new FpsMeter();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private WifiManager.MulticastLock multicastLock;
    private int playGeneration;
    private int recoveryAttempts;
    private String resolutionText = "Độ phân giải: —";
    private Window wrappedWindow;
    private Window.Callback originalWindowCallback;
    private Object backDispatcher33;
    private Object backCallback33;
    private boolean backgroundActive;
    private boolean resumeAfterLifecyclePause;
    private boolean fullscreen;
    private View.OnLayoutChangeListener rootLayoutListener;

    private final Runnable statsTick = new Runnable() {
        @Override public void run() {
            updateStatsNow();
            updateSeekUi();
            if (player != null) mainHandler.postDelayed(this, 2000);
        }
    };

    @Override public boolean onCreate() {
        if (getContext() != null) {
            ((Application) getContext().getApplicationContext()).registerActivityLifecycleCallbacks(this);
        }
        return true;
    }

    @Override public void onActivityResumed(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        MainActivity main = (MainActivity) activity;
        if (AppPreferences.isTvInterface(main)) {
            if (currentActivity == main) detach();
            return;
        }
        if (currentActivity != main || panel == null || panel.getParent() == null) attach(main);
        stopBackgroundService(main);
        backgroundActive = false;
        if (player != null) {
            player.setWakeMode(C.WAKE_MODE_NONE);
            if (resumeAfterLifecyclePause) player.play();
        }
        resumeAfterLifecyclePause = false;
        if (panel != null && panel.getVisibility() == View.VISIBLE) keepScreenAwake(true);
        mainHandler.post(this::syncOrientationUi);
    }

    private void attach(MainActivity activity) {
        detach();
        currentActivity = activity;
        LinearLayout root = activity.findViewById(R.id.mainRoot);
        channelList = activity.findViewById(R.id.listChannels);
        fullscreenHost = activity.findViewById(android.R.id.content);
        if (root == null || channelList == null || fullscreenHost == null) return;

        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setVisibility(View.GONE);
        panel.setBackgroundResource(R.drawable.panel);
        panel.setPadding(0, 0, 0, dp(activity, 7));

        videoContainer = new FrameLayout(activity);
        videoContainer.setBackgroundColor(Color.BLACK);

        playerView = new PlayerView(activity);
        playerView.setUseController(true);
        playerView.setControllerAutoShow(true);
        playerView.setControllerShowTimeoutMs(4500);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        playerView.setKeepContentOnPlayerReset(true);
        videoContainer.addView(playerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Use Media3's native play/pause + TimeBar so DVR/catch-up streams can be dragged precisely.
        // Remove only the skip/10-second controls that made the live-TV overlay too busy.
        hideControllerView("exo_prev");
        hideControllerView("exo_next");
        hideControllerView("exo_rew");
        hideControllerView("exo_ffwd");

        rotateButton = new Button(activity);
        rotateButton.setAllCaps(false);
        rotateButton.setTextSize(12);
        rotateButton.setText("Toàn màn hình");
        rotateButton.setVisibility(View.GONE);
        rotateButton.setOnClickListener(v -> rotateScreen());
        FrameLayout.LayoutParams rotateParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, dp(activity, 44), Gravity.TOP | Gravity.END);
        rotateParams.setMargins(dp(activity, 8), dp(activity, 8), dp(activity, 8), 0);
        videoContainer.addView(rotateButton, rotateParams);

        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            if (rotateButton != null) {
                rotateButton.setVisibility(fullscreen || visibility == View.VISIBLE ? View.VISIBLE : View.GONE);
            }
            if (visibility == View.VISIBLE) updateSeekUi();
        });

        int videoHeight = calculateVideoHeight(activity, activity.getResources().getDisplayMetrics().widthPixels);
        panel.addView(videoContainer, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, videoHeight));
        videoContainer.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (fullscreen) return;
            int width = right - left;
            if (width <= 0) return;
            int wanted = calculateVideoHeight(activity, width);
            ViewGroup.LayoutParams params = videoContainer.getLayoutParams();
            if (params != null && params.height != wanted) {
                params.height = wanted;
                videoContainer.setLayoutParams(params);
            }
        });

        title = new TextView(activity);
        title.setTextSize(17);
        title.setTextColor(activity.getColor(R.color.text_primary));
        title.setMaxLines(1);
        title.setPadding(dp(activity, 12), dp(activity, 7), dp(activity, 12), 0);
        panel.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        programme = new TextView(activity);
        programme.setTextSize(14);
        programme.setTextColor(activity.getColor(R.color.text_secondary));
        programme.setMaxLines(2);
        programme.setPadding(dp(activity, 12), dp(activity, 2), dp(activity, 12), 0);
        panel.addView(programme, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        stats = new TextView(activity);
        stats.setTextSize(13);
        stats.setTextColor(activity.getColor(R.color.accent));
        stats.setText("Độ phân giải: —  •  FPS: —");
        stats.setPadding(dp(activity, 12), dp(activity, 2), dp(activity, 12), 0);
        panel.addView(stats, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        panelParams.setMarginStart(-root.getPaddingLeft());
        panelParams.setMarginEnd(-root.getPaddingRight());
        root.setClipToPadding(false);
        root.addView(panel, Math.min(1, root.getChildCount()), panelParams);

        originalClick = channelList.getOnItemClickListener();
        channelList.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getAdapter().getItem(position);
            if (item instanceof Channel) playInline((Channel) item);
            else if (originalClick != null) originalClick.onItemClick(parent, view, position, id);
        });

        rootLayoutListener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                mainHandler.post(this::syncOrientationUi);
        fullscreenHost.addOnLayoutChangeListener(rootLayoutListener);
        installBackHandling(activity);
    }

    private void hideControllerView(String resourceName) {
        if (currentActivity == null || playerView == null) return;
        int id = currentActivity.getResources().getIdentifier(resourceName, "id", currentActivity.getPackageName());
        if (id != 0) {
            View view = playerView.findViewById(id);
            if (view != null) view.setVisibility(View.GONE);
        }
    }

    private void setControllerViewVisible(String resourceName, boolean visible) {
        if (currentActivity == null || playerView == null) return;
        int id = currentActivity.getResources().getIdentifier(resourceName, "id", currentActivity.getPackageName());
        if (id != 0) {
            View view = playerView.findViewById(id);
            if (view != null) view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void updateSeekUi() {
        boolean seekable = player != null
                && player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                && player.isCurrentMediaItemSeekable();
        setControllerViewVisible("exo_progress", seekable);
        setControllerViewVisible("exo_position", seekable);
        setControllerViewVisible("exo_duration", seekable);
    }

    private int calculateVideoHeight(Activity activity, int width) {
        return Math.max(dp(activity, 180), width * 9 / 16);
    }

    private void rotateScreen() {
        if (currentActivity == null || videoContainer == null) return;
        if (fullscreen) exitFullscreen(true);
        else enterFullscreen(true);
    }

    private void enterFullscreen(boolean requestLandscape) {
        if (currentActivity == null || videoContainer == null || fullscreenHost == null || fullscreen) return;
        ViewGroup parent = (ViewGroup) videoContainer.getParent();
        if (parent != null) parent.removeView(videoContainer);
        fullscreen = true;
        FrameLayout.LayoutParams full = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        fullscreenHost.addView(videoContainer, full);
        videoContainer.bringToFront();
        hideSystemBars();
        updateRotateButton();
        if (playerView != null) playerView.showController();
        if (requestLandscape) currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    private void exitFullscreen(boolean requestPortrait) {
        if (currentActivity == null || videoContainer == null || panel == null || !fullscreen) return;
        ViewGroup parent = (ViewGroup) videoContainer.getParent();
        if (parent != null) parent.removeView(videoContainer);
        fullscreen = false;
        int width = currentActivity.getResources().getDisplayMetrics().widthPixels;
        panel.addView(videoContainer, 0, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, calculateVideoHeight(currentActivity, width)));
        showSystemBars();
        updateRotateButton();
        videoContainer.post(() -> {
            if (videoContainer == null || fullscreen) return;
            int actualWidth = videoContainer.getWidth();
            if (actualWidth <= 0) return;
            ViewGroup.LayoutParams params = videoContainer.getLayoutParams();
            if (params != null) {
                params.height = calculateVideoHeight(currentActivity, actualWidth);
                videoContainer.setLayoutParams(params);
            }
        });
        if (playerView != null) playerView.showController();
        if (requestPortrait) currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private void syncOrientationUi() {
        if (currentActivity == null || panel == null || panel.getVisibility() != View.VISIBLE) return;
        int orientation = currentActivity.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE && !fullscreen) {
            enterFullscreen(false);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT && fullscreen
                && currentActivity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            exitFullscreen(false);
        }
        updateRotateButton();
    }

    private void updateRotateButton() {
        if (rotateButton == null) return;
        rotateButton.setText(fullscreen ? "Xoay dọc" : "Toàn màn hình");
        if (fullscreen) rotateButton.setVisibility(View.VISIBLE);
    }

    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        if (currentActivity == null) return;
        Window window = currentActivity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
    }

    @SuppressWarnings("deprecation")
    private void showSystemBars() {
        if (currentActivity == null) return;
        Window window = currentActivity.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (Build.VERSION.SDK_INT >= 30) {
            android.view.WindowInsetsController controller = window.getInsetsController();
            if (controller != null) controller.show(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars());
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void playInline(Channel channel) {
        if (currentActivity == null || playerView == null) return;
        int generation = ++playGeneration;
        recoveryAttempts = 0;
        resumeAfterLifecyclePause = false;
        releasePlayer();
        currentChannel = channel;
        panel.setVisibility(View.VISIBLE);
        title.setText(channel.name());
        resolutionText = "Độ phân giải: —";
        if (stats != null) stats.setText("Độ phân giải: —  •  FPS: —");
        updateProgramme(channel);
        AppPreferences.recordRecent(currentActivity, channel);
        keepScreenAwake(true);
        mainHandler.post(this::syncOrientationUi);

        String scheme = Uri.parse(channel.url()).getScheme();
        if (scheme == null || !scheme.matches("(?i)https?|rtsp|udp|rtmp")) {
            programme.setText("Nguồn này chưa hỗ trợ trong khung Mobile: " + (scheme == null ? "không rõ giao thức" : scheme));
            return;
        }
        DrmSpec drm = DrmSpec.fromOptions(channel.options());
        if (drm.remoteClearKey()) {
            programme.setText("Đang lấy giấy phép ClearKey…");
            resolveRemoteClearKey(channel, drm, generation);
        } else startInlinePlayer(channel, drm, generation, "");
    }

    private void startInlinePlayer(Channel channel, DrmSpec drm, int generation, String forcedMime) {
        if (currentActivity == null || currentChannel != channel || generation != playGeneration) return;
        try {
            Map<String, String> headers = new LinkedHashMap<>(channel.headers());
            String ua = headers.containsKey("User-Agent") ? headers.get("User-Agent") : "Nm7-IPTV/1.10.5 Android";
            DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                    .setUserAgent(ua).setConnectTimeoutMs(15_000).setReadTimeoutMs(20_000)
                    .setDefaultRequestProperties(headers);
            DefaultDataSource.Factory data = new DefaultDataSource.Factory(currentActivity, http);
            DefaultMediaSourceFactory mediaFactory = new DefaultMediaSourceFactory(data);

            MediaItem.Builder media = new MediaItem.Builder().setUri(channel.url());
            String mime = forcedMime;
            if (mime == null || mime.isEmpty()) mime = channel.mimeHint();
            if (mime == null || mime.isEmpty()) mime = StreamSpec.inferMime(channel.url(), channel.options());
            if (mime != null && !mime.isEmpty()) media.setMimeType(mime);
            DrmPlayback.configure(drm, media, mediaFactory);

            ExoPlayer next = new ExoPlayer.Builder(currentActivity,
                    new DefaultRenderersFactory(currentActivity).setEnableDecoderFallback(true))
                    .setMediaSourceFactory(mediaFactory).build();
            next.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
            next.setHandleAudioBecomingNoisy(true);
            next.setWakeMode(C.WAKE_MODE_NONE);
            player = next;
            playerView.setPlayer(next);
            videoCounters = null;
            fpsMeter.reset();
            updateSeekUi();

            next.addAnalyticsListener(new AnalyticsListener() {
                @Override public void onVideoEnabled(EventTime eventTime, DecoderCounters counters) {
                    if (generation != playGeneration) return;
                    videoCounters = counters;
                    fpsMeter.reset();
                }
            });
            next.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (generation != playGeneration || next != player) return;
                    if (state == Player.STATE_BUFFERING) programme.setText("Đang tải luồng…");
                    if (state == Player.STATE_READY) {
                        updateProgramme(channel);
                        updateSeekUi();
                        keepScreenAwake(true);
                        mainHandler.postDelayed(() -> {
                            if (generation == playGeneration && next == player && next.isPlaying()) recoveryAttempts = 0;
                        }, 5000);
                    }
                }
                @Override public void onIsPlayingChanged(boolean isPlaying) {
                    if (generation != playGeneration || next != player) return;
                    if (isPlaying && !backgroundActive) keepScreenAwake(true);
                }
                @Override public void onAvailableCommandsChanged(Player.Commands availableCommands) {
                    if (generation != playGeneration || next != player) return;
                    updateSeekUi();
                }
                @Override public void onVideoSizeChanged(VideoSize size) {
                    if (generation != playGeneration || next != player) return;
                    resolutionText = size.width > 0 && size.height > 0
                            ? "Độ phân giải: " + size.width + " × " + size.height : "Độ phân giải: —";
                    updateStatsNow();
                }
                @Override public void onPlayerError(PlaybackException error) {
                    if (generation != playGeneration || next != player) return;
                    handlePlaybackError(channel, drm, generation, forcedMime, error);
                }
            });

            MediaItem item = media.build();
            String scheme = Uri.parse(channel.url()).getScheme();
            if ("rtsp".equalsIgnoreCase(scheme)) {
                next.setMediaSource(new RtspMediaSource.Factory().setForceUseRtpTcp(true)
                        .setUserAgent(ua).createMediaSource(item));
            } else {
                if ("udp".equalsIgnoreCase(scheme)) acquireMulticast();
                next.setMediaItem(item);
            }
            next.prepare();
            next.play();
            mainHandler.removeCallbacks(statsTick);
            mainHandler.post(statsTick);
            playerView.showController();
            updateRotateButton();
        } catch (Exception error) {
            releasePlayer();
            programme.setText("Không phát được kênh này • " + readable(error));
        }
    }

    private void handlePlaybackError(Channel channel, DrmSpec drm, int generation, String forcedMime, PlaybackException error) {
        if (error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED
                && (forcedMime == null || forcedMime.isEmpty())) {
            programme.setText("Đang nhận diện lại định dạng luồng…");
            probeRedirectedMime(channel, drm, generation);
            return;
        }
        boolean drmSystem = error.errorCode == PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR;
        if ((drmSystem || isTransientPlaybackError(error)) && recoveryAttempts < 3) {
            int attempt = ++recoveryAttempts;
            long delay = drmSystem ? (attempt == 1 ? 450 : attempt == 2 ? 1000 : 1800)
                    : (attempt == 1 ? 600 : attempt == 2 ? 1400 : 2500);
            programme.setText(drmSystem ? "DRM đang khởi tạo lại…" : "Luồng tạm gián đoạn, đang thử lại…");
            releasePlayer();
            mainHandler.postDelayed(() -> {
                if (generation == playGeneration && currentChannel == channel) startInlinePlayer(channel, drm, generation, forcedMime);
            }, delay);
            return;
        }
        programme.setText("Không phát được kênh này • " + error.getErrorCodeName());
        updateStatsNow();
    }

    private boolean isTransientPlaybackError(PlaybackException error) {
        if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                || error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT) return true;
        Throwable cause = error.getCause();
        while (cause != null) {
            if (cause instanceof HttpDataSource.InvalidResponseCodeException) {
                int code = ((HttpDataSource.InvalidResponseCodeException) cause).responseCode;
                return code == 408 || code == 429 || code >= 500;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void probeRedirectedMime(Channel channel, DrmSpec drm, int generation) {
        Map<String, String> headers = new LinkedHashMap<>(channel.headers());
        io.execute(() -> {
            String current = channel.url();
            String detected = "";
            try {
                for (int hop = 0; hop < 5; hop++) {
                    HttpURLConnection connection = (HttpURLConnection) new URL(current).openConnection();
                    try {
                        connection.setConnectTimeout(10_000);
                        connection.setReadTimeout(10_000);
                        connection.setInstanceFollowRedirects(false);
                        connection.setRequestMethod("GET");
                        for (Map.Entry<String, String> header : headers.entrySet()) connection.setRequestProperty(header.getKey(), header.getValue());
                        if (!headers.containsKey("User-Agent")) connection.setRequestProperty("User-Agent", "Nm7-IPTV/1.10.5 Android");
                        int code = connection.getResponseCode();
                        if (code >= 300 && code < 400) {
                            String location = connection.getHeaderField("Location");
                            if (location == null || location.isEmpty()) break;
                            current = new URL(new URL(current), location).toString();
                            String hint = StreamSpec.inferMime(current, channel.options());
                            if (hint != null && !hint.isEmpty()) { detected = hint; break; }
                            continue;
                        }
                        String type = connection.getContentType();
                        if (type != null) {
                            String lower = type.toLowerCase(Locale.ROOT);
                            if (lower.contains("mpegurl") || lower.contains("m3u8")) detected = MimeTypes.APPLICATION_M3U8;
                            else if (lower.contains("dash+xml")) detected = MimeTypes.APPLICATION_MPD;
                        }
                        break;
                    } finally { connection.disconnect(); }
                }
            } catch (Exception ignored) { }
            final String mime = detected;
            if (currentActivity != null) currentActivity.runOnUiThread(() -> {
                if (generation != playGeneration || currentChannel != channel) return;
                if (!mime.isEmpty()) {
                    releasePlayer();
                    startInlinePlayer(channel, drm, generation, mime);
                } else programme.setText("Không nhận diện được định dạng nguồn phát của kênh này");
            });
        });
    }

    private void resolveRemoteClearKey(Channel channel, DrmSpec drm, int generation) {
        Map<String, String> streamHeaders = new LinkedHashMap<>(channel.headers());
        io.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(drm.license).openConnection();
                connection.setConnectTimeout(15_000);
                connection.setReadTimeout(20_000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestMethod("GET");
                for (Map.Entry<String, String> entry : drm.headers.entrySet()) connection.setRequestProperty(entry.getKey(), entry.getValue());
                String streamHost = Uri.parse(channel.url()).getHost();
                String licenseHost = Uri.parse(drm.license).getHost();
                if (streamHost != null && streamHost.equalsIgnoreCase(licenseHost)) {
                    for (String name : new String[]{"User-Agent", "Referer", "Origin", "Cookie"}) {
                        String value = streamHeaders.get(name);
                        if (value != null && !value.isEmpty() && !drm.headers.containsKey(name)) connection.setRequestProperty(name, value);
                    }
                }
                if (connection.getRequestProperty("User-Agent") == null) connection.setRequestProperty("User-Agent", "Dalvik/2.1.0");
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw new IllegalArgumentException("HTTP " + code);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try (InputStream input = connection.getInputStream()) {
                    byte[] buffer = new byte[4096];
                    int total = 0, count;
                    while ((count = input.read(buffer)) >= 0) {
                        total += count;
                        if (total > 65_536) throw new IllegalArgumentException("phản hồi giấy phép quá lớn");
                        output.write(buffer, 0, count);
                    }
                }
                String response = output.toString("UTF-8").trim();
                DrmPlayback.clearKeyResponse(response);
                DrmSpec local = DrmSpec.create("clearkey", response);
                if (currentActivity != null) currentActivity.runOnUiThread(() -> {
                    if (generation == playGeneration && currentChannel == channel) startInlinePlayer(channel, local, generation, "");
                });
            } catch (Exception error) {
                if (currentActivity != null) currentActivity.runOnUiThread(() -> {
                    if (generation == playGeneration && programme != null) programme.setText("Không lấy được giấy phép ClearKey cho kênh này");
                });
            } finally { if (connection != null) connection.disconnect(); }
        });
    }

    private void updateProgramme(Channel channel) {
        if (programme == null || channelList == null) return;
        try {
            Object adapter = channelList.getAdapter();
            if (!(adapter instanceof ChannelAdapter)) { programme.setText(channel.group()); return; }
            Field field = ChannelAdapter.class.getDeclaredField("guide");
            field.setAccessible(true);
            EpgStore.Guide guide = (EpgStore.Guide) field.get(adapter);
            EpgStore.Programme now = guide == null ? null : guide.find(channel);
            if (now == null) programme.setText(channel.group());
            else programme.setText(now.title + "  •  " + now.startText() + " - " + now.endText());
        } catch (Exception ignored) { programme.setText(channel.group()); }
    }

    private void updateStatsNow() {
        if (stats == null) return;
        String fpsText = "FPS: —";
        if (player != null && videoCounters != null) {
            double value = fpsMeter.sample(android.os.SystemClock.elapsedRealtime(), videoCounters.renderedOutputBufferCount, player.isPlaying());
            if (!Double.isNaN(value)) fpsText = String.format(Locale.ROOT, "FPS: %.1f", value);
            else if (player.isPlaying()) fpsText = "FPS: đang đo…";
        }
        stats.setText(resolutionText + "  •  " + fpsText);
    }

    private void keepScreenAwake(boolean awake) {
        if (currentActivity == null) return;
        if (awake) currentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else currentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (playerView != null) playerView.setKeepScreenOn(awake);
    }

    private void startBackgroundService(Activity activity) {
        if (currentChannel == null) return;
        Intent service = new Intent(activity, BackgroundPlaybackService.class)
                .putExtra(BackgroundPlaybackService.EXTRA_CHANNEL_NAME, currentChannel.name())
                .putExtra(BackgroundPlaybackService.EXTRA_RETURN_MAIN, true);
        if (Build.VERSION.SDK_INT >= 26) activity.startForegroundService(service);
        else activity.startService(service);
    }

    private void stopBackgroundService(Activity activity) {
        activity.stopService(new Intent(activity, BackgroundPlaybackService.class));
    }

    private boolean consumeBack() {
        if (fullscreen) {
            exitFullscreen(true);
            return true;
        }
        if (panel == null || panel.getVisibility() != View.VISIBLE) return false;
        ++playGeneration;
        releasePlayer();
        currentChannel = null;
        backgroundActive = false;
        resumeAfterLifecyclePause = false;
        panel.setVisibility(View.GONE);
        keepScreenAwake(false);
        if (currentActivity != null) stopBackgroundService(currentActivity);
        if (channelList != null) channelList.requestFocus();
        return true;
    }

    private void installBackHandling(Activity activity) {
        if (Build.VERSION.SDK_INT >= 26) {
            wrappedWindow = activity.getWindow();
            originalWindowCallback = wrappedWindow.getCallback();
            wrappedWindow.setCallback(new BackWindowCallback(this, originalWindowCallback));
        }
        if (Build.VERSION.SDK_INT >= 33) Api33Back.register(this, activity);
    }

    private void uninstallBackHandling() {
        if (Build.VERSION.SDK_INT >= 33) Api33Back.unregister(this);
        if (wrappedWindow != null && originalWindowCallback != null && wrappedWindow.getCallback() instanceof BackWindowCallback) {
            wrappedWindow.setCallback(originalWindowCallback);
        }
        wrappedWindow = null;
        originalWindowCallback = null;
    }

    private void acquireMulticast() {
        if (currentActivity == null || multicastLock != null) return;
        WifiManager wifi = (WifiManager) currentActivity.getApplicationContext().getSystemService(android.content.Context.WIFI_SERVICE);
        if (wifi != null) {
            multicastLock = wifi.createMulticastLock("Nm7InlineUdpV2");
            multicastLock.setReferenceCounted(false);
            multicastLock.acquire();
        }
    }

    private void releasePlayer() {
        mainHandler.removeCallbacks(statsTick);
        videoCounters = null;
        fpsMeter.reset();
        if (player != null) {
            ExoPlayer old = player;
            player = null;
            try { old.stop(); } catch (RuntimeException ignored) { }
            try { old.clearMediaItems(); } catch (RuntimeException ignored) { }
            if (playerView != null) playerView.setPlayer(null);
            old.release();
        }
        updateSeekUi();
        if (multicastLock != null) {
            try { if (multicastLock.isHeld()) multicastLock.release(); } catch (RuntimeException ignored) { }
            multicastLock = null;
        }
    }

    private void detach() {
        ++playGeneration;
        if (fullscreen && currentActivity != null && videoContainer != null && panel != null) exitFullscreen(false);
        keepScreenAwake(false);
        if (currentActivity != null) {
            stopBackgroundService(currentActivity);
            showSystemBars();
            currentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
        releasePlayer();
        uninstallBackHandling();
        if (fullscreenHost != null && rootLayoutListener != null) fullscreenHost.removeOnLayoutChangeListener(rootLayoutListener);
        if (channelList != null && originalClick != null) channelList.setOnItemClickListener(originalClick);
        if (panel != null && panel.getParent() instanceof LinearLayout) ((LinearLayout) panel.getParent()).removeView(panel);
        currentActivity = null;
        currentChannel = null;
        channelList = null;
        originalClick = null;
        panel = null;
        videoContainer = null;
        fullscreenHost = null;
        playerView = null;
        rotateButton = null;
        title = null;
        programme = null;
        stats = null;
        backgroundActive = false;
        resumeAfterLifecyclePause = false;
        fullscreen = false;
        rootLayoutListener = null;
    }

    private static String readable(Exception error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? error.getClass().getSimpleName() : message;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    @TargetApi(33)
    private static final class Api33Back {
        static void register(MobileInlinePlayerProviderV2 provider, Activity activity) {
            android.window.OnBackInvokedDispatcher dispatcher = activity.getOnBackInvokedDispatcher();
            android.window.OnBackInvokedCallback callback = () -> { if (!provider.consumeBack()) activity.finish(); };
            dispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            provider.backDispatcher33 = dispatcher;
            provider.backCallback33 = callback;
        }
        static void unregister(MobileInlinePlayerProviderV2 provider) {
            if (provider.backDispatcher33 instanceof android.window.OnBackInvokedDispatcher
                    && provider.backCallback33 instanceof android.window.OnBackInvokedCallback) {
                ((android.window.OnBackInvokedDispatcher) provider.backDispatcher33)
                        .unregisterOnBackInvokedCallback((android.window.OnBackInvokedCallback) provider.backCallback33);
            }
            provider.backDispatcher33 = null;
            provider.backCallback33 = null;
        }
    }

    @TargetApi(26)
    private static final class BackWindowCallback implements Window.Callback {
        private final MobileInlinePlayerProviderV2 provider;
        private final Window.Callback base;
        BackWindowCallback(MobileInlinePlayerProviderV2 provider, Window.Callback base) { this.provider = provider; this.base = base; }
        @Override public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && provider.panel != null && provider.panel.getVisibility() == View.VISIBLE) {
                if (event.getAction() == KeyEvent.ACTION_UP) provider.consumeBack();
                return true;
            }
            return base.dispatchKeyEvent(event);
        }
        @Override public boolean dispatchKeyShortcutEvent(KeyEvent event) { return base.dispatchKeyShortcutEvent(event); }
        @Override public boolean dispatchTouchEvent(MotionEvent event) { return base.dispatchTouchEvent(event); }
        @Override public boolean dispatchTrackballEvent(MotionEvent event) { return base.dispatchTrackballEvent(event); }
        @Override public boolean dispatchGenericMotionEvent(MotionEvent event) { return base.dispatchGenericMotionEvent(event); }
        @Override public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) { return base.dispatchPopulateAccessibilityEvent(event); }
        @Override public View onCreatePanelView(int featureId) { return base.onCreatePanelView(featureId); }
        @Override public boolean onCreatePanelMenu(int featureId, Menu menu) { return base.onCreatePanelMenu(featureId, menu); }
        @Override public boolean onPreparePanel(int featureId, View view, Menu menu) { return base.onPreparePanel(featureId, view, menu); }
        @Override public boolean onMenuOpened(int featureId, Menu menu) { return base.onMenuOpened(featureId, menu); }
        @Override public boolean onMenuItemSelected(int featureId, MenuItem item) { return base.onMenuItemSelected(featureId, item); }
        @Override public void onWindowAttributesChanged(WindowManager.LayoutParams attrs) { base.onWindowAttributesChanged(attrs); }
        @Override public void onContentChanged() { base.onContentChanged(); }
        @Override public void onWindowFocusChanged(boolean hasFocus) { base.onWindowFocusChanged(hasFocus); }
        @Override public void onAttachedToWindow() { base.onAttachedToWindow(); }
        @Override public void onDetachedFromWindow() { base.onDetachedFromWindow(); }
        @Override public void onPanelClosed(int featureId, Menu menu) { base.onPanelClosed(featureId, menu); }
        @Override public boolean onSearchRequested() { return base.onSearchRequested(); }
        @Override public boolean onSearchRequested(SearchEvent searchEvent) { return base.onSearchRequested(searchEvent); }
        @Override public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) { return base.onWindowStartingActionMode(callback); }
        @Override public ActionMode onWindowStartingActionMode(ActionMode.Callback callback, int type) { return base.onWindowStartingActionMode(callback, type); }
        @Override public void onActionModeStarted(ActionMode mode) { base.onActionModeStarted(mode); }
        @Override public void onActionModeFinished(ActionMode mode) { base.onActionModeFinished(mode); }
        @Override public void onProvideKeyboardShortcuts(List<KeyboardShortcutGroup> data, Menu menu, int deviceId) { base.onProvideKeyboardShortcuts(data, menu, deviceId); }
        @Override public void onPointerCaptureChanged(boolean hasCapture) { base.onPointerCaptureChanged(hasCapture); }
    }

    @Override public void onActivityDestroyed(Activity activity) { if (activity == currentActivity) detach(); }
    @Override public void onActivityCreated(Activity a, Bundle b) { }
    @Override public void onActivityStarted(Activity a) { }
    @Override public void onActivityPaused(Activity a) {
        if (a != currentActivity || player == null) return;
        boolean wantedPlayback = player.getPlayWhenReady();
        if (AppPreferences.backgroundPlayback(a) && wantedPlayback && currentChannel != null
                && panel != null && panel.getVisibility() == View.VISIBLE) {
            backgroundActive = true;
            resumeAfterLifecyclePause = false;
            keepScreenAwake(false);
            player.setWakeMode(C.WAKE_MODE_NETWORK);
            startBackgroundService(a);
        } else {
            backgroundActive = false;
            resumeAfterLifecyclePause = wantedPlayback;
            player.pause();
        }
    }
    @Override public void onActivityStopped(Activity a) { }
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String sort) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
