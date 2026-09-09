package vn.phuong.iptvplayer;

import android.app.Application;

/** Process-wide networking defaults for long-running IPTV streams. */
public final class Nm7Application extends Application {
    @Override public void onCreate() {
        super.onCreate();
        // HttpURLConnection is used by Media3 DefaultHttpDataSource. Keep pooled
        // sockets alive longer so segment-based live streams do not reconnect
        // more often than necessary between playlist/segment requests.
        System.setProperty("http.keepAlive", "true");
        System.setProperty("http.maxConnections", "8");
        System.setProperty("http.keepAliveDuration", "300000");
    }
}
