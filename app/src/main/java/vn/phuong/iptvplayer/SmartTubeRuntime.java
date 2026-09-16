package vn.phuong.iptvplayer;

import android.content.Context;

import java.lang.reflect.Method;

/** Initializes the vendored SmartTube phone runtime without coupling the bridge classes to its Java types. */
public final class SmartTubeRuntime {
    private static boolean initialized;
    private static final String PREFIX = "com.liskovsoft.smartyoutubetv2";

    private SmartTubeRuntime() {}

    public static synchronized void initialize(Context context) {
        if (initialized) return;
        try {
            System.setProperty("http.keepAlive", "false");
            Class<?> mother = Class.forName(PREFIX + ".common.misc.MotherActivity");
            mother.getMethod("setTvDpiScalingEnabled", boolean.class).invoke(null, false);
            Class<?> screensaver = Class.forName(PREFIX + ".common.misc.ScreensaverManager");
            screensaver.getMethod("setSupported", boolean.class).invoke(null, false);

            Class<?> vmClass = Class.forName(PREFIX + ".common.app.views.ViewManager");
            Object vm = vmClass.getMethod("instance", Context.class).invoke(null, context.getApplicationContext());
            Class<?> browseActivity = Class.forName(PREFIX + ".droid.ui.browse.BrowseActivity");
            vmClass.getMethod("setRoot", Class.class).invoke(vm, browseActivity);

            register(vmClass, vm, "SplashView", "SplashActivity", browseActivity);
            register(vmClass, vm, "BrowseView", "BrowseActivity", browseActivity);
            register(vmClass, vm, "PlaybackView", "PlaybackActivity", browseActivity);
            register(vmClass, vm, "AppDialogView", "AppDialogActivity", browseActivity);
            register(vmClass, vm, "SearchView", "SearchActivity", browseActivity);
            register(vmClass, vm, "SignInView", "SignInActivity", browseActivity);
            register(vmClass, vm, "AddDeviceView", "AddDeviceActivity", browseActivity);
            register(vmClass, vm, "ChannelView", "ChannelActivity", browseActivity);
            register(vmClass, vm, "ChannelUploadsView", "ChannelUploadsActivity", browseActivity);
            register(vmClass, vm, "WebBrowserView", "WebBrowserActivity", browseActivity);
            initialized = true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            initialized = false;
        }
    }

    private static void register(Class<?> vmClass, Object vm, String viewName, String activityName, Class<?> parent) throws ReflectiveOperationException {
        Class<?> view = Class.forName(PREFIX + ".common.app.views." + viewName);
        Class<?> activity = Class.forName(PREFIX + ".droid.ui." + activityPath(activityName) + "." + activityName);
        Method register = vmClass.getMethod("register", Class.class, Class.class, Class.class);
        register.invoke(vm, view, activity, parent);
    }

    private static String activityPath(String activityName) {
        switch (activityName) {
            case "SplashActivity": return "splash";
            case "BrowseActivity": return "browse";
            case "PlaybackActivity": return "playback";
            case "AppDialogActivity": return "dialogs";
            case "SearchActivity": return "search";
            case "SignInActivity": return "signin";
            case "AddDeviceActivity": return "adddevice";
            case "ChannelActivity": return "channel";
            case "ChannelUploadsActivity": return "channeluploads";
            case "WebBrowserActivity": return "webbrowser";
            default: throw new IllegalArgumentException(activityName);
        }
    }
}
