package vn.phuong.iptvplayer;

import android.app.Activity;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.net.wifi.WifiManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.liskovsoft.smartyoutubetv2.droid.DroidApplication;

/** Mobile-only Application class. */
public final class MobileNm7Application extends DroidApplication implements android.app.Application.ActivityLifecycleCallbacks {
    private static final String SMARTTUBE_PACKAGE = "com.liskovsoft.smartyoutubetv2.droid.ui.";
    private static final String SMARTTUBE_PLAYBACK = SMARTTUBE_PACKAGE + "playback.PlaybackActivity";
    private static final String SMARTTUBE_BROWSE = SMARTTUBE_PACKAGE + "browse.BrowseActivity";
    private static final String SMARTTUBE_WEB = SMARTTUBE_PACKAGE + "webbrowser.WebBrowserActivity";
    private static final String SMARTTUBE_SIGNIN = SMARTTUBE_PACKAGE + "signin.SignInActivity";
    private static final String SMARTTUBE_DIALOG = SMARTTUBE_PACKAGE + "dialog.AppDialogActivity";

    private static MobileNm7Application instance;
    private WifiManager.WifiLock wifiLock;
    private int startedActivities;
    private Activity iptvPlayerActivity;

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", "8");
        System.setProperty("http.keepAliveDuration", "300000");
        registerActivityLifecycleCallbacks(this);
    }

    private void acquireWifiPerformanceLock() {
        if (wifiLock != null && wifiLock.isHeld()) return;
        try {
            WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wifi == null || !wifi.isWifiEnabled()) return;
            wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Nm7IptvHighPerf");
            wifiLock.setReferenceCounted(false);
            wifiLock.acquire();
        } catch (RuntimeException ignored) { }
    }

    private void releaseWifiPerformanceLock() {
        try {
            if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
        } catch (RuntimeException ignored) { }
        wifiLock = null;
    }

    @Override public void onActivityStarted(Activity activity) {
        if (++startedActivities == 1) acquireWifiPerformanceLock();
        if (activity instanceof PlayerActivity) {
            iptvPlayerActivity = activity;
            pauseExternalMedia(activity);
        } else if (SMARTTUBE_PLAYBACK.equals(activity.getClass().getName())) {
            pauseIptvPlayer();
        }
    }

    @Override public void onActivityStopped(Activity activity) {
        if (activity == iptvPlayerActivity && activity.isFinishing()) iptvPlayerActivity = null;
        if (startedActivities > 0 && --startedActivities == 0) releaseWifiPerformanceLock();
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {
        String name = activity.getClass().getName();
        if (activity instanceof MainActivity || activity instanceof PlayerActivity) {
            boolean youtubeSelected = false;
            activity.getWindow().getDecorView().post(() -> HomeTabBar.attach(activity, youtubeSelected));
        } else if (SMARTTUBE_BROWSE.equals(name)) {
            activity.getWindow().getDecorView().post(() -> {
                HomeTabBar.attach(activity, true);
                installSmartTubeFontFix(activity);
            });
        } else if (SMARTTUBE_WEB.equals(name) || SMARTTUBE_SIGNIN.equals(name) || SMARTTUBE_DIALOG.equals(name)) {
            activity.getWindow().getDecorView().post(() -> installSmartTubeFontFix(activity));
        }
    }

    public static boolean hasIptvPlayer() {
        return instance != null && instance.iptvPlayerActivity instanceof PlayerActivity
                && !instance.iptvPlayerActivity.isFinishing();
    }

    /** Pause the integrated IPTV player as soon as a real SmartTube playback Activity starts. */
    private void pauseIptvPlayer() {
        Activity activity = iptvPlayerActivity;
        if (!(activity instanceof PlayerActivity)) return;
        try {
            Field field = PlayerActivity.class.getDeclaredField("player");
            field.setAccessible(true);
            Object player = field.get(activity);
            if (player != null) {
                Method pause = player.getClass().getMethod("setPlayWhenReady", boolean.class);
                pause.invoke(player, false);
            }
            activity.stopService(new android.content.Intent(activity, BackgroundPlaybackService.class));
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
    }

    /** Pause the currently active external media session before a new IPTV channel starts. */
    public static void pauseExternalMedia(Activity activity) {
        try {
            AudioManager audio = (AudioManager) activity.getSystemService(AUDIO_SERVICE);
            if (audio == null) return;
            long now = SystemClock.uptimeMillis();
            audio.dispatchMediaKeyEvent(new android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE, 0));
            audio.dispatchMediaKeyEvent(new android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_MEDIA_PAUSE, 0));
        } catch (RuntimeException ignored) { }
    }

    /** Force a final system sans-serif span for Vietnamese text while preserving other spans. */
    private void installSmartTubeFontFix(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        applySmartTubeFontFix(root);
        ViewTreeObserver observer = root.getViewTreeObserver();
        if (observer.isAlive()) observer.addOnGlobalLayoutListener(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) applySmartTubeFontFix(root);
        });
    }

    private void applySmartTubeFontFix(View view) {
        if (view instanceof TextView && !(view instanceof EditText)) {
            TextView text = (TextView) view;
            CharSequence value = text.getText();
            if (containsVietnameseText(value)) {
                int style = text.getTypeface() != null ? text.getTypeface().getStyle() : Typeface.NORMAL;
                text.setTypeface(Typeface.create("sans-serif", style));
                if (value.length() > 0 && !hasSansSerifSpan(value)) {
                    SpannableString fixed = new SpannableString(value);
                    TypefaceSpan[] old = fixed.getSpans(0, fixed.length(), TypefaceSpan.class);
                    for (TypefaceSpan span : old) fixed.removeSpan(span);
                    fixed.setSpan(new TypefaceSpan("sans-serif"), 0, fixed.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    text.setText(fixed, TextView.BufferType.SPANNABLE);
                }
            }
        }
        if (view instanceof WebView) {
            WebSettings settings = ((WebView) view).getSettings();
            settings.setStandardFontFamily("sans-serif");
            settings.setSansSerifFontFamily("sans-serif");
            settings.setDefaultFontFamily("sans-serif");
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) applySmartTubeFontFix(group.getChildAt(i));
        }
    }

    private boolean hasSansSerifSpan(CharSequence value) {
        if (!(value instanceof Spannable)) return false;
        TypefaceSpan[] spans = ((Spannable) value).getSpans(0, value.length(), TypefaceSpan.class);
        for (TypefaceSpan span : spans) {
            if ("sans-serif".equals(span.getFamily())) return true;
        }
        return false;
    }

    private boolean containsVietnameseText(CharSequence text) {
        if (text == null) return false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\uFFFD' || (c >= '\u00C0' && c <= '\u024F') || (c >= '\u1E00' && c <= '\u1EFF') || (c >= '\u0300' && c <= '\u036F')) return true;
        }
        return false;
    }

    @Override public void onActivityResumed(Activity activity) { }
    @Override public void onActivityPaused(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
    @Override public void onActivityDestroyed(Activity activity) {
        if (activity == iptvPlayerActivity) iptvPlayerActivity = null;
    }
}
