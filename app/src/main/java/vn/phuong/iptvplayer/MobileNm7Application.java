package vn.phuong.iptvplayer;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.net.wifi.WifiManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.liskovsoft.smartyoutubetv2.droid.DroidApplication;

/** Mobile-only Application class. */
public final class MobileNm7Application extends DroidApplication implements android.app.Application.ActivityLifecycleCallbacks {
    private WifiManager.WifiLock wifiLock;
    private int startedActivities;

    @Override public void onCreate() {
        super.onCreate();
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
    }

    @Override public void onActivityStopped(Activity activity) {
        if (startedActivities > 0 && --startedActivities == 0) releaseWifiPerformanceLock();
    }

    @Override public void onActivityCreated(Activity activity, Bundle state) {
        String name = activity.getClass().getName();
        if (activity instanceof MainActivity) {
            activity.getWindow().getDecorView().post(() -> HomeTabBar.attach(activity, false));
        } else if ("com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity".equals(name)) {
            activity.getWindow().getDecorView().post(() -> {
                HomeTabBar.attach(activity, true);
                applySmartTubeFont(activity.findViewById(android.R.id.content));
            });
        }
    }

    /** Keep SmartTube text on the device's standard sans-serif family for Vietnamese glyph fallback. */
    private void applySmartTubeFont(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            int style = text.getTypeface() != null ? text.getTypeface().getStyle() : Typeface.NORMAL;
            text.setTypeface(Typeface.create("sans-serif", style));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applySmartTubeFont(group.getChildAt(i));
            }
        }
    }

    @Override public void onActivityResumed(Activity activity) { }
    @Override public void onActivityPaused(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
    @Override public void onActivityDestroyed(Activity activity) { }
}
