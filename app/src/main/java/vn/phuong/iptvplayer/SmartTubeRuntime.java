package vn.phuong.iptvplayer;

import android.content.Context;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Initializes the vendored SmartTube phone runtime without compile-time SmartTube imports. */
public final class SmartTubeRuntime {
    private static boolean initialized;
    private static final String PREFIX = "com.liskovsoft.smartyoutubetv2";

    private SmartTubeRuntime() {}

    public static synchronized void initialize(Context context) {
        if (initialized) return;
        try {
            // Keep pooled HTTP connections enabled. MobileNm7Application sets the same
            // defaults; disabling keep-alive here caused avoidable reconnect latency.
            System.setProperty("http.keepAlive", "true");
            System.setProperty("http.maxConnections", "8");

            // SmartTube exposes IPv4 DNS as a player/network preference because some
            // networks resolve YouTube over IPv6 slowly or unreliably. Apply the same
            // setting through reflection so the mobile fork uses the optimized path
            // without taking a compile-time dependency on SmartTube internals.
            try {
                Class<?> tweaks = Class.forName(PREFIX + ".common.prefs.PlayerTweaksData");
                Object data = tweaks.getMethod("instance", Context.class).invoke(null, context.getApplicationContext());
                Field ipv4 = tweaks.getField("DNS_TYPE_IPV4");
                Method setDns = tweaks.getMethod("setPreferredDnsType", int.class);
                setDns.invoke(data, ipv4.getInt(null));
            } catch (ReflectiveOperationException | RuntimeException ignored) { }

            Class<?> mother = Class.forName(PREFIX + ".common.misc.MotherActivity");
            mother.getMethod("setTvDpiScalingEnabled", boolean.class).invoke(null, false);
            Class<?> screensaver = Class.forName(PREFIX + ".common.misc.ScreensaverManager");
            screensaver.getMethod("setSupported", boolean.class).invoke(null, false);

            Class<?> vmClass = Class.forName(PREFIX + ".common.app.views.ViewManager");
            Object vm = vmClass.getMethod("instance", Context.class).invoke(null, context.getApplicationContext());
            Class<?> browse = Class.forName(PREFIX + ".droid.ui.browse.BrowseActivity");
            vmClass.getMethod("setRoot", Class.class).invoke(vm, browse);

            register(vmClass, vm, "SplashView", "splash.SplashActivity", browse);
            register(vmClass, vm, "BrowseView", "browse.BrowseActivity", browse);
            register(vmClass, vm, "PlaybackView", "playback.PlaybackActivity", browse);
            register(vmClass, vm, "AppDialogView", "dialogs.AppDialogActivity", browse);
            register(vmClass, vm, "SearchView", "search.SearchActivity", browse);
            register(vmClass, vm, "SignInView", "signin.SignInActivity", browse);
            register(vmClass, vm, "AddDeviceView", "adddevice.AddDeviceActivity", browse);
            register(vmClass, vm, "ChannelView", "channel.ChannelActivity", browse);
            register(vmClass, vm, "ChannelUploadsView", "channeluploads.ChannelUploadsActivity", browse);
            register(vmClass, vm, "WebBrowserView", "webbrowser.WebBrowserActivity", browse);
            initialized = true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            initialized = false;
        }
    }

    private static void register(Class<?> vmClass, Object vm, String viewName, String activityPath, Class<?> parent) throws ReflectiveOperationException {
        Class<?> view = Class.forName(PREFIX + ".common.app.views." + viewName);
        Class<?> activity = Class.forName(PREFIX + ".droid.ui." + activityPath);
        vmClass.getMethod("register", Class.class, Class.class, Class.class).invoke(vm, view, activity, parent);
    }
}
