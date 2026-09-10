package vn.phuong.iptvplayer;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;

/**
 * TV-only fallback player used after Media3/MediaCodec cannot decode a stream.
 *
 * The first attempt lets VLC use hardware decoding. If that also fails, the
 * activity retries once with VLC software decoding, which uses VLC/FFmpeg's
 * bundled codec stack instead of relying on the TV firmware decoder.
 */
@androidx.media3.common.util.UnstableApi
public final class VlcFallbackActivity extends Activity {
    private LibVLC libVlc;
    private MediaPlayer vlcPlayer;
    private VLCVideoLayout videoLayout;
    private TextView status;
    private View errorPanel;
    private TextView errorText;
    private String url = "";
    private String name = "";
    private Bundle headers = new Bundle();
    private boolean softwareAttempt;
    private boolean restarting;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_vlc_fallback);
        Insets.apply(findViewById(R.id.vlcRoot));

        url = value(PlayerActivity.EXTRA_URL);
        name = value(PlayerActivity.EXTRA_NAME);
        Bundle passed = getIntent().getBundleExtra(PlayerActivity.EXTRA_HEADERS);
        if (passed != null) headers = passed;

        videoLayout = findViewById(R.id.vlcVideoLayout);
        status = findViewById(R.id.txtVlcStatus);
        errorPanel = findViewById(R.id.vlcError);
        errorText = findViewById(R.id.txtVlcError);
        ((TextView) findViewById(R.id.txtVlcTitle)).setText(name);

        findViewById(R.id.btnVlcBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnVlcRetry).setOnClickListener(v -> {
            softwareAttempt = false;
            restart(false);
        });
    }

    @Override protected void onStart() {
        super.onStart();
        if (vlcPlayer == null) startVlc(false);
    }

    private void startVlc(boolean softwareOnly) {
        releaseVlc();
        softwareAttempt = softwareOnly;
        restarting = false;
        errorPanel.setVisibility(View.GONE);
        status.setText(softwareOnly
                ? "VLC • đang mở bằng decoder phần mềm…"
                : "VLC • đang thử decoder phần cứng…");
        try {
            ArrayList<String> args = new ArrayList<>();
            args.add("--network-caching=3000");
            args.add("--clock-jitter=0");
            args.add("--clock-synchro=0");
            args.add("--no-video-title-show");
            libVlc = new LibVLC(this, args);
            vlcPlayer = new MediaPlayer(libVlc);
            vlcPlayer.attachViews(videoLayout, null, false, false);
            vlcPlayer.setEventListener(event -> {
                switch (event.type) {
                    case MediaPlayer.Event.Opening:
                        runOnUiThread(() -> status.setText(softwareAttempt
                                ? "VLC • đang mở luồng bằng decoder phần mềm…"
                                : "VLC • đang mở luồng…"));
                        break;
                    case MediaPlayer.Event.Buffering:
                        runOnUiThread(() -> status.setText(softwareAttempt
                                ? "VLC • đang tải luồng bằng decoder phần mềm…"
                                : "VLC • đang tải luồng…"));
                        break;
                    case MediaPlayer.Event.Playing:
                        runOnUiThread(() -> {
                            restarting = false;
                            errorPanel.setVisibility(View.GONE);
                            status.setText(softwareAttempt
                                    ? "Đang phát • VLC decoder phần mềm"
                                    : "Đang phát • VLC");
                        });
                        break;
                    case MediaPlayer.Event.Paused:
                        runOnUiThread(() -> status.setText("Đã tạm dừng • VLC"));
                        break;
                    case MediaPlayer.Event.EncounteredError:
                        runOnUiThread(this::handleVlcError);
                        break;
                    case MediaPlayer.Event.EndReached:
                        runOnUiThread(() -> restart(softwareAttempt));
                        break;
                    default:
                        break;
                }
            });

            Media media = new Media(libVlc, Uri.parse(url));
            media.setHWDecoderEnabled(!softwareOnly, !softwareOnly);
            media.addOption(":network-caching=3000");
            String ua = header("User-Agent");
            String referer = header("Referer");
            String origin = header("Origin");
            String cookie = header("Cookie");
            if (!ua.isEmpty()) media.addOption(":http-user-agent=" + ua);
            if (!referer.isEmpty()) media.addOption(":http-referrer=" + referer);
            if (!origin.isEmpty()) media.addOption(":http-origin=" + origin);
            if (!cookie.isEmpty()) media.addOption(":http-cookie=" + cookie);
            vlcPlayer.setMedia(media);
            media.release();
            vlcPlayer.play();
        } catch (Throwable error) {
            handleStartException(error);
        }
    }

    private void handleVlcError() {
        if (restarting) return;
        if (!softwareAttempt) {
            restarting = true;
            status.setText("VLC hardware lỗi • chuyển sang decoder phần mềm…");
            videoLayout.postDelayed(() -> startVlc(true), 300L);
            return;
        }
        showFinalError("VLC cũng không giải mã được nguồn 4K này trên thiết bị.\nNguồn có thể vượt quá khả năng CPU/GPU hoặc dùng codec/profile không được VLC hỗ trợ trên Android TV này.");
    }

    private void handleStartException(Throwable error) {
        if (!softwareAttempt) {
            status.setText("VLC hardware không khởi tạo được • thử decoder phần mềm…");
            videoLayout.postDelayed(() -> startVlc(true), 250L);
        } else {
            String detail = error.getMessage();
            showFinalError("Không khởi tạo được VLC decoder phần mềm" +
                    (detail == null || detail.isEmpty() ? "." : ": " + detail));
        }
    }

    private void restart(boolean softwareOnly) {
        if (restarting) return;
        restarting = true;
        status.setText("VLC • đang nối lại luồng…");
        videoLayout.postDelayed(() -> startVlc(softwareOnly), 350L);
    }

    private void showFinalError(String message) {
        restarting = false;
        status.setText("Không phát được bằng VLC");
        errorText.setText(message);
        errorPanel.setVisibility(View.VISIBLE);
        findViewById(R.id.btnVlcRetry).requestFocus();
    }

    private String header(String key) {
        String value = headers.getString(key);
        if (value == null) {
            for (String candidate : headers.keySet()) {
                if (candidate.equalsIgnoreCase(key)) {
                    value = headers.getString(candidate);
                    break;
                }
            }
        }
        return value == null ? "" : value;
    }

    private String value(String key) {
        String value = getIntent().getStringExtra(key);
        return value == null ? "" : value;
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                    event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER ||
                    event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (vlcPlayer != null) {
                    if (vlcPlayer.isPlaying()) vlcPlayer.pause(); else vlcPlayer.play();
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override protected void onStop() {
        releaseVlc();
        super.onStop();
    }

    @Override protected void onDestroy() {
        releaseVlc();
        super.onDestroy();
    }

    private void releaseVlc() {
        if (vlcPlayer != null) {
            try { vlcPlayer.stop(); } catch (Throwable ignored) { }
            try { vlcPlayer.detachViews(); } catch (Throwable ignored) { }
            try { vlcPlayer.release(); } catch (Throwable ignored) { }
            vlcPlayer = null;
        }
        if (libVlc != null) {
            try { libVlc.release(); } catch (Throwable ignored) { }
            libVlc = null;
        }
    }
}
