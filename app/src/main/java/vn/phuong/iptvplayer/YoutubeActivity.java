package vn.phuong.iptvplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Mobile YouTube entry point.
 *
 * The viewer stays inside the NM7 process. When a compatible SmartTube build is
 * already installed, the user can hand off to SmartTube from the same screen;
 * otherwise NM7 provides a built-in YouTube web player fallback.
 *
 * This class is intentionally isolated so a future SmartTube source/AAR can
 * replace the fallback without changing the NM7 home UI or sleep timer.
 */
public final class YoutubeActivity extends Activity {
    private static final String YOUTUBE_URL = "https://m.youtube.com/";
    private static final String[] SMARTTUBE_PACKAGES = {
            "org.smarttube.stable",
            "org.smarttube.beta",
            "app.smarttube",
            "app.smarttube.fdroid"
    };

    private WebView webView;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 22, 52));
        root.setPadding(12, 12, 12, 12);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("YouTube");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, 48, 1));

        Button smartTube = new Button(this);
        smartTube.setText("SmartTube");
        smartTube.setAllCaps(false);
        smartTube.setOnClickListener(v -> openSmartTubeOrToast());
        top.addView(smartTube, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 48));

        Button close = new Button(this);
        close.setText("Đóng");
        close.setAllCaps(false);
        close.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(96, 48);
        closeLp.setMarginStart(6);
        top.addView(close, closeLp);

        root.addView(top);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, 3));

        webView = new WebView(this);
        configureWebView(webView);
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);
        webView.loadUrl(YOUTUBE_URL);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView(WebView wv) {
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportMultipleWindows(false);
        s.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 15; Mobile) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/151.0.0.0 Mobile Safari/537.36 NM7YouTube/1.10.26");

        wv.setBackgroundColor(Color.BLACK);
        wv.setWebViewClient(new WebViewClient());
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                progress.setProgress(newProgress);
            }
        });
    }

    private String findInstalledSmartTube() {
        for (String pkg : SMARTTUBE_PACKAGES) {
            try {
                getPackageManager().getApplicationInfo(pkg, 0);
                return pkg;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void openSmartTubeOrToast() {
        String pkg = findInstalledSmartTube();
        if (pkg == null) {
            Toast.makeText(this, "Chưa cài SmartTube. NM7 đang dùng trình YouTube tích hợp.", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch == null) throw new ActivityNotFoundException(pkg);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        } catch (Exception e) {
            Toast.makeText(this, "Không mở được SmartTube", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
