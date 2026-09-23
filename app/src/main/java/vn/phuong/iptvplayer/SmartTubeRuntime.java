package vn.phuong.iptvplayer;

import android.content.Context;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Initializes the vendored SmartTube phone runtime without compile-time SmartTube imports. */
public final class SmartTubeRuntime {
    // Mobile 1.10.26 CI rebuild trigger after YouTube integration fixes.
    private static boolean initialized;
    private static final String PREFIX = "com.liskovsoft.smartyoutubetv2";

    private SmartTubeRuntime() {}

    public static synchronized void initialize(Context context) {
        if (initialized) return;
        try {
            // Match SmartTube-droid's real DroidApplication bootstrap. This app owns a
            // different Application class, so we must perform the same one-time setup here.
            // In particular, SmartTube expects keep-alive=false for its YouTube service.
            System.setProperty("http.keepAlive", "false");

            Class<?> mother = Class.forName(PREFIX + ".common.misc.MotherActivity");
            mother.getMethod("setTvDpiScalingEnabled", boolean.class).invoke(null, false);
            Class<?> screensaver = Class.forName(PREFIX + ".common.misc.ScreensaverManager");
            screensaver.getMethod("setSupported", boolean.class).invoke(null, false);

            // Force AppPrefs initialization before BrowsePresenter/MediaServiceManager starts.
            Class<?> appPrefs = Class.forName(PREFIX + ".common.prefs.AppPrefs");
            appPrefs.getMethod("instance", Context.class).invoke(null, context.getApplicationContext());

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
        } catch (ReflectiveOperationException | RuntimeException error) {
            initialized = false;
            android.util.Log.e("NM7SmartTube", "SmartTube bootstrap failed", error);
        }
    }

    /** Force SmartTube's Play-Behind mode and HOME shortcut so video/audio continues
     * when NM7 is backgrounded or the screen is turned off. */
    public static synchronized void enableBackgroundPlayback(Context context) {
        try {
            Context app = context.getApplicationContext();
            Class<?> playerDataClass = Class.forName(PREFIX + ".common.prefs.PlayerData");
            Object playerData = playerDataClass.getMethod("instance", Context.class).invoke(null, app);
            Field playBehind = playerDataClass.getField("BACKGROUND_MODE_PLAY_BEHIND");
            playerDataClass.getMethod("setBackgroundMode", int.class).invoke(playerData, playBehind.getInt(null));

            Class<?> generalDataClass = Class.forName(PREFIX + ".common.prefs.GeneralData");
            Object generalData = generalDataClass.getMethod("instance", Context.class).invoke(null, app);
            Field home = generalDataClass.getField("BACKGROUND_PLAYBACK_SHORTCUT_HOME");
            generalDataClass.getMethod("setBackgroundPlaybackShortcut", int.class).invoke(generalData, home.getInt(null));
        } catch (ReflectiveOperationException | RuntimeException ignored) { }
    }

    private static void register(Class<?> vmClass, Object vm, String viewName, String activityPath, Class<?> parent) throws ReflectiveOperationException {
        Class<?> view = Class.forName(PREFIX + ".common.app.views." + viewName);
        Class<?> activity = Class.forName(PREFIX + ".droid.ui." + activityPath);
        vmClass.getMethod("register", Class.class, Class.class, Class.class).invoke(vm, view, activity, parent);
    }
}
