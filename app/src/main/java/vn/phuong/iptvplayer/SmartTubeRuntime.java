package vn.phuong.iptvplayer;

import android.content.Context;
import java.lang.reflect.Method;

/** Initializes SmartTube's native phone runtime without compile-time coupling to vendored classes. */
public final class SmartTubeRuntime {
    private SmartTubeRuntime() {}
    public static synchronized void initialize(Context context) {
        try {
            System.setProperty("http.keepAlive", "false");
            Class<?> mother = Class.forName("com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity");
            mother.getMethod("setTvDpiScalingEnabled", boolean.class).invoke(null, false);
            Class<?> screensaver = Class.forName("com.liskovsoft.smartyoutubetv2.common.misc.ScreensaverManager");
            screensaver.getMethod("setSupported", boolean.class).invoke(null, false);
            Class<?> vmClass = Class.forName("com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager");
            Object vm = vmClass.getMethod("instance", Context.class).invoke(null, context.getApplicationContext());
            Class<?> browse = Class.forName("com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity");
            vmClass.getMethod("setRoot", Class.class).invoke(vm, browse);
            register(vmClass, vm, "SplashView", "com.liskovsoft.smartyoutubetv2.droid.ui.splash.SplashActivity");
            register(vmClass, vm, "BrowseView", "com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity");
            register(vmClass, vm, "PlaybackView", "com.liskovsoft.smartyoutubetv2.droid.ui.playback.PlaybackActivity", true);
            register(vmClass, vm, "AppDialogView", "com.liskovsoft.smartyoutubetv2.droid.ui.dialogs.AppDialogActivity", true);
            register(vmClass, vm, "SearchView", "com.liskovsoft.smartyoutubetv2.droid.ui.search.SearchActivity", true);
            register(vmClass, vm, "SignInView", "com.liskovsoft.smartyoutubetv2.droid.ui.signin.SignInActivity", true);
            register(vmClass, vm, "AddDeviceView", "com.liskovsoft.smartyoutubetv2.droid.ui.adddevice.AddDeviceActivity", true);
            register(vmClass, vm, "ChannelView", "com.liskovsoft.smartyoutubetv2.droid.ui.channel.ChannelActivity", true);
            register(vmClass, vm, "ChannelUploadsView", "com.liskovsoft.smartyoutubetv2.droid.ui.channeluploads.ChannelUploadsActivity", true);
            register(vmClass, vm, "WebBrowserView", "com.liskovsoft.smartyoutubetv2.droid.ui.webbrowser.WebBrowserActivity", true);
        } catch (Throwable ignored) { }
    }
    private static void register(Class<?> vmClass, Object vm, String viewSimpleName, String activityName, boolean withParent) throws Exception {
        Class<?> view = Class.forName("com.liskovsoft.smartyoutubetv2.common.app.views." + viewSimpleName);
        Class<?> activity = Class.forName(activityName);
        if (withParent) {
            Class<?> parent = Class.forName("com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity");
            vmClass.getMethod("register", Class.class, Class.class, Class.class).invoke(vm, view, activity, parent);
        } else {
            vmClass.getMethod("register", Class.class, Class.class).invoke(vm, view, activity);
        }
    }
}
