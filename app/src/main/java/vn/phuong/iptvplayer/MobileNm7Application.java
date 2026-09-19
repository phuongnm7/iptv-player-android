package vn.phuong.iptvplayer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import android.os.Build;
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
    private static final String SMARTTUBE_DIALOG = SMARTTUBE_PACKAGE + "dialogs.AppDialogActivity";

    private static MobileNm7Application instance;
    private WifiManager.WifiLock wifiLock;
    private int startedActivities;
    private Activity iptvPlayerActivity;
    private Activity smartTubeBrowseActivity;
    private Activity smartTubePlaybackActivity;
    private volatile boolean tabSwitchPending;
    private final java.util.Map<Activity, Object> smartTubeBackCallbacks = new java.util.HashMap<>();

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        // This flag is process-transient: if Android killed the process, the YouTube
        // player is gone too, so a new launch must not be forced back into YouTube.
        SharedPlaybackSession.clearTransientState(getApplicationContext());
        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", "8");
        System.setProperty("http.keepAliveDuration", "300000");
        registerActivityLifecycleCallbacks(this);
        // Initialize SmartTube before any YouTube tab can be opened.
        // Delayed-only prewarm created a first-tap race with ViewManager registration.
        SmartTubeRuntime.initialize(getApplicationContext());
    }

    public static void markTabSwitch() {
        if (instance != null) {
            instance.tabSwitchPending = true;
            // Shared process marker read by the vendored SmartTube phone player.
            // Keep it alive long enough for the outgoing Activity to reach onStop().
            System.setProperty("nm7.tab.switch.until",
                    Long.toString(System.currentTimeMillis() + 5000L));
        }
    }

    private void clearTabSwitch() {
        tabSwitchPending = false;
    }

    public static boolean isTabSwitchPending() {
        return instance != null && instance.tabSwitchPending;
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
        String name = activity.getClass().getName();
        if (activity instanceof PlayerActivity) {
            iptvPlayerActivity = activity;
            // Only an explicit IPTV tab start may take ownership from YouTube.
            // Creating/resuming PlayerActivity while YouTube owns the session must not
            // send a global MEDIA_PAUSE event to SmartTube.
            if (SharedPlaybackSession.TAB_IPTV.equals(SharedPlaybackSession.tab(activity))) {
                stopYoutubeForIptv();
                activity.stopService(new Intent(activity, BackgroundPlaybackService.class));
            }
        } else if (SMARTTUBE_BROWSE.equals(name)) {
            smartTubeBrowseActivity = activity;
        } else if (SMARTTUBE_PLAYBACK.equals(name)) {
            smartTubePlaybackActivity = activity;
            clearTabSwitch();
            SharedPlaybackSession.setTab(activity, SharedPlaybackSession.TAB_YOUTUBE);
            SharedPlaybackSession.setYoutubeBackground(activity, false);
            // PlaybackActivity creation is NOT proof that a YouTube video is playing.
            // IPTV ownership is released only by the real ExoPlayer STATE_READY +
            // playWhenReady callback patched into SmartTube PlaybackActivity.
        }
    }

    @Override public void onActivityStopped(Activity activity) {
        if (activity == iptvPlayerActivity && activity.isFinishing()) iptvPlayerActivity = null;
        // YouTube explicit-close owns its service cleanup before starting IPTV.
        if (startedActivities > 0 && --startedActivities == 0) releaseWifiPerformanceLock();
    }

    private void bringSmartTubeBrowseToFront() {
        Activity browse = smartTubeBrowseActivity;
        try {
            if (browse != null && !browse.isFinishing() && !browse.isDestroyed()) {
                Intent intent = new Intent(browse, browse.getClass());
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                browse.startActivity(intent);
                browse.overridePendingTransition(0, 0);
                return;
            }
            Class<?> clazz = Class.forName(SMARTTUBE_BROWSE);
            Activity source = iptvPlayerActivity;
            if (source != null && !source.isFinishing() && !source.isDestroyed()) {
                Intent intent = new Intent(source, clazz);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                source.startActivity(intent);
                source.overridePendingTransition(0, 0);
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {
        String name = activity.getClass().getName();
        if (activity instanceof MainActivity) {
            activity.getWindow().getDecorView().post(() -> {
                MobileIptvUi.install(activity);
                HomeTabBar.attach(activity, false);
            });
        } else if (SMARTTUBE_BROWSE.equals(name)) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            activity.getWindow().getDecorView().post(() -> {
                HomeTabBar.attach(activity, true);
                installSmartTubeBrowseFixes(activity);
            });
        } else if (SMARTTUBE_PLAYBACK.equals(name)) {
            // Use SmartTube's native Android Back dispatch. The custom callback previously
            // called onBackPressed() from onBackInvoked(), which could bypass the patched
            // parent-view/PIP path and close PlaybackActivity immediately.
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            activity.getWindow().getDecorView().post(() -> {
                installSmartTubeFontFix(activity);
                View root = activity.findViewById(android.R.id.content);
                if (root != null) forceSingleColumn(root);
            });
        } else if (name.startsWith(SMARTTUBE_PACKAGE) && !SMARTTUBE_PLAYBACK.equals(name)) {
            if (!SMARTTUBE_WEB.equals(name)) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            activity.getWindow().getDecorView().post(() -> {
                installSmartTubeFontFix(activity);
                View root = activity.findViewById(android.R.id.content);
                if (root != null) forceSingleColumn(root);
            });
        }
    }

    @Override public void onActivityPaused(Activity activity) {
        if (activity instanceof PlayerActivity && tabSwitchPending) {
            // PlayerActivity normally releases ExoPlayer in onStop when background playback is off.
            // During a tab transition keep the player object alive so returning to IPTV restores
            // the exact player surface instead of reopening the channel from scratch.
            try {
                Field field = PlayerActivity.class.getDeclaredField("backgroundPlaybackActive");
                field.setAccessible(true);
                field.setBoolean(activity, true);
            } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
    }

    @Override public void onActivityResumed(Activity activity) {
        String name = activity.getClass().getName();
        if (activity instanceof PlayerActivity) {
            clearTabSwitch();
        } else if (activity instanceof MainActivity
                && SharedPlaybackSession.TAB_IPTV.equals(SharedPlaybackSession.tab(activity))
                && !MobileInlinePlayerProviderV2.shouldResumeIptvAfterYoutube()) {
            // The IPTV tab is only a list until a channel is selected. Keep the live
            // YouTube mini-player visible and attached while that list is in front.
            activity.getWindow().getDecorView().post(() -> installYoutubeMiniPlayer(activity));
        }
        if (SMARTTUBE_PLAYBACK.equals(name)) {
            // Foreground YouTube no longer needs the background keep-alive service.
            activity.stopService(new Intent(activity, BackgroundPlaybackService.class));
        }
    }

    public static boolean hasIptvPlayer() {
        return instance != null && instance.iptvPlayerActivity instanceof PlayerActivity
                && !instance.iptvPlayerActivity.isFinishing();
    }

    /**
     * SmartTube owns the actual YouTube player. Attach that one player to a compact
     * TextureView in MainActivity while the user only browses the IPTV tab.
     */
    private static void installYoutubeMiniPlayer(Activity activity) {
        try {
            Class<?> playback = Class.forName(SMARTTUBE_PLAYBACK);
            Object active = playback.getMethod("isNm7MiniPlayerActive").invoke(null);
            if (!(active instanceof Boolean) || !((Boolean) active)) return;
            View content = activity.findViewById(android.R.id.content);
            if (!(content instanceof ViewGroup)) return;
            ViewGroup root = (ViewGroup) content;
            final int tag = 0x7f0a7e31;
            View old = root.findViewWithTag(tag);
            if (old != null) root.removeView(old);
            float density = activity.getResources().getDisplayMetrics().density;
            android.widget.FrameLayout box = new android.widget.FrameLayout(activity);
            box.setTag(tag);
            box.setBackgroundColor(android.graphics.Color.BLACK);
            android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                    (int) (180 * density), (int) (101 * density),
                    android.view.Gravity.BOTTOM | android.view.Gravity.END);
            params.bottomMargin = (int) (76 * density);
            params.rightMargin = (int) (12 * density);
            android.view.TextureView video = new android.view.TextureView(activity);
            box.addView(video, new android.widget.FrameLayout.LayoutParams(-1, -1));
            video.setOnClickListener(v -> {
                try { playback.getMethod("restoreNm7Player").invoke(null); }
                catch (ReflectiveOperationException | RuntimeException ignored) { }
            });
            android.widget.ImageButton close = new android.widget.ImageButton(activity);
            close.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            close.setContentDescription("Đóng video YouTube");
            box.addView(close, new android.widget.FrameLayout.LayoutParams(
                    (int) (36 * density), (int) (36 * density),
                    android.view.Gravity.TOP | android.view.Gravity.END));
            close.setOnClickListener(v -> {
                try { playback.getMethod("stopForNm7Iptv").invoke(null); }
                catch (ReflectiveOperationException | RuntimeException ignored) { }
                root.removeView(box);
            });
            root.addView(box, params);
            playback.getMethod("attachNm7MiniPlayer", android.view.TextureView.class).invoke(null, video);
        } catch (ReflectiveOperationException | RuntimeException error) {
            android.util.Log.e("NM7Playback", "IPTV mini-player attach failed", error);
        }
    }

    /** Bring the actual YouTube playback/browse Activity back after HOME/process recreation. */
    public static boolean bringSmartTubeToFront() {
        if (instance == null) return false;
        try {
            Activity playback = instance.smartTubePlaybackActivity;
            if (playback != null && !playback.isFinishing() && !playback.isDestroyed()) {
                Intent intent = new Intent(playback, playback.getClass());
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                playback.startActivity(intent);
                playback.overridePendingTransition(0, 0);
                return true;
            }
            Activity browse = instance.smartTubeBrowseActivity;
            if (browse != null && !browse.isFinishing() && !browse.isDestroyed()) {
                Intent intent = new Intent(browse, browse.getClass());
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                browse.startActivity(intent);
                browse.overridePendingTransition(0, 0);
                return true;
            }
        } catch (RuntimeException ignored) { }
        return false;
    }

    /** Pause the integrated IPTV player as soon as a real SmartTube playback Activity starts. */
    // Called by SmartTube PlaybackActivity before its first player initialization.
    // This closes the ownership race where SmartTube onStart could initialize its decoder
    // before Application.onActivityStarted() had released IPTV.
    public static void pauseIptvForYoutube() {
        MobileInlinePlayerProviderV2.releaseForYoutube();
        PlayerActivity.releaseForYoutube();
        android.util.Log.i("NM7Playback", "YouTube READY: released inline and Activity IPTV owners");
    }

    private void pauseIptvPlayer() {
        Activity activity = iptvPlayerActivity;
        if (!(activity instanceof PlayerActivity)) return;
        try {
            Field field = PlayerActivity.class.getDeclaredField("player");
            field.setAccessible(true);
            Method remember = PlayerActivity.class.getDeclaredMethod("rememberPosition");
            remember.setAccessible(true);
            remember.invoke(activity);
            Object player = field.get(activity);
            if (player != null) {
                Method pause = player.getClass().getMethod("setPlayWhenReady", boolean.class);
                pause.invoke(player, false);
            }
            Field background = PlayerActivity.class.getDeclaredField("backgroundPlaybackActive");
            background.setAccessible(true);
            background.setBoolean(activity, false);
            Method release = PlayerActivity.class.getDeclaredMethod("releasePlayer");
            release.setAccessible(true);
            release.invoke(activity);
            PlayerActivity.cancelYoutubeHandoff();
            activity.stopService(new android.content.Intent(activity, BackgroundPlaybackService.class));
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
    }

    public static void stopYoutubeForIptv() {
        try {
            Class.forName(SMARTTUBE_PLAYBACK).getMethod("stopForNm7Iptv").invoke(null);
        } catch (ReflectiveOperationException | RuntimeException error) {
            android.util.Log.e("NM7Playback", "YouTube handoff failed", error);
        }
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

    private void installSmartTubeBrowseFixes(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        // GRID_COLUMNS is patched directly in SmartTube BrowseActivity.java during build.
        // Do not recursively rewrite every RecyclerView after layout; that work caused
        // unnecessary measure/layout passes and delayed the first YouTube frame.
        replaceSmartTubeBranding(root);
        installSmartTubeFontFix(activity);
    }

    /** Convert the phone Browse feed from the fork's 2-column grid to a single-column feed. */
    private void replaceSmartTubeBranding(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            CharSequence value = text.getText();
            if (value != null) {
                String v = value.toString();
                if (v.contains("SmartTube")) text.setText(v.replace("SmartTube Droid", "NM7 TV").replace("SmartTube", "NM7 TV"));
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) replaceSmartTubeBranding(group.getChildAt(i));
        }
    }

    private void forceSingleColumn(View view) {
        String className = view.getClass().getName();
        try {
            if (className.contains("BaseGridView") || className.contains("VerticalGridView")) {
                Method setNumColumns = findMethod(view.getClass(), "setNumColumns", int.class);
                if (setNumColumns != null) setNumColumns.invoke(view, 1);
            }
            if (className.contains("RecyclerView")) {
                Method getLayoutManager = findMethod(view.getClass(), "getLayoutManager");
                if (getLayoutManager != null) {
                    Object lm = getLayoutManager.invoke(view);
                    if (lm != null) {
                        String lmName = lm.getClass().getName();
                        Method setSpanCount = findMethod(lm.getClass(), "setSpanCount", int.class);
                        if (setSpanCount != null && (lmName.contains("GridLayoutManager") || lmName.contains("StaggeredGridLayoutManager"))) {
                            setSpanCount.invoke(lm, 1);
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) forceSingleColumn(group.getChildAt(i));
        }
    }

    private Method findMethod(Class<?> type, String name, Class<?>... args) {
        try {
            Method method = type.getMethod(name, args);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    /** Force Android/WebView to use UTF-8 and a system sans-serif font for Vietnamese text. */
    private void installSmartTubeFontFix(Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        applySmartTubeFontFix(root);

    }

    private void applySmartTubeFontFix(View view) {
        if (view instanceof TextView && !(view instanceof EditText)) {
            TextView text = (TextView) view;
            CharSequence value = text.getText();
            // Only touch Vietnamese/malformed text. Applying Typeface/Locale to every
            // SmartTube TextView caused a large amount of unnecessary UI work on Browse.
            if (containsVietnameseText(value)) {
                int style = text.getTypeface() != null ? text.getTypeface().getStyle() : Typeface.NORMAL;
                text.setTypeface(Typeface.create("sans-serif", style));
                if (Build.VERSION.SDK_INT >= 24) text.setTextLocale(java.util.Locale.forLanguageTag("vi-VN"));
                if (Build.VERSION.SDK_INT >= 23) text.setFallbackLineSpacing(true);
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
            settings.setDefaultTextEncodingName("UTF-8");
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

    private void installSmartTubeBackHandling(Activity activity) {
        if (Build.VERSION.SDK_INT < 33) return;
        try {
            Class<?> callbackType = Class.forName("android.window.OnBackInvokedCallback");
            Object callback = java.lang.reflect.Proxy.newProxyInstance(
                    callbackType.getClassLoader(), new Class<?>[]{callbackType},
                    (proxy, method, args) -> {
                        if ("onBackInvoked".equals(method.getName())) {
                            // Let the patched SmartTube PlaybackActivity handle BACK.
                            // Its first BACK navigates to Browse/mini-player instead of
                            // destroying the playback session immediately.
                            try {
                                Method back = activity.getClass().getMethod("onBackPressed");
                                back.invoke(activity);
                            } catch (ReflectiveOperationException | RuntimeException ignored) {
                                try { activity.finish(); } catch (RuntimeException ignoredAgain) { }
                            }
                        }
                        return null;
                    });
            Method dispatcherGetter = Activity.class.getMethod("getOnBackInvokedDispatcher");
            Object dispatcher = dispatcherGetter.invoke(activity);
            Class<?> dispatcherType = Class.forName("android.window.OnBackInvokedDispatcher");
            Method register = dispatcherType.getMethod("registerOnBackInvokedCallback", int.class, callbackType);
            register.invoke(dispatcher, 0, callback);
            smartTubeBackCallbacks.put(activity, callback);
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
    }

    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
    @Override public void onActivityDestroyed(Activity activity) {
        if (Build.VERSION.SDK_INT >= 33) {
            Object callback = smartTubeBackCallbacks.remove(activity);
            if (callback != null) {
                try {
                    Object dispatcher = Activity.class.getMethod("getOnBackInvokedDispatcher").invoke(activity);
                    Class<?> callbackType = Class.forName("android.window.OnBackInvokedCallback");
                    Class<?> dispatcherType = Class.forName("android.window.OnBackInvokedDispatcher");
                    dispatcherType.getMethod("unregisterOnBackInvokedCallback", callbackType).invoke(dispatcher, callback);
                } catch (ReflectiveOperationException | RuntimeException ignored) { }
            }
        }
        if (activity == iptvPlayerActivity) iptvPlayerActivity = null;
        if (activity == smartTubeBrowseActivity) smartTubeBrowseActivity = null;
        if (activity == smartTubePlaybackActivity) smartTubePlaybackActivity = null;
    }
}
