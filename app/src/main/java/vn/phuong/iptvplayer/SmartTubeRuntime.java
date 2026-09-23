package vn.phuong.iptvplayer;

import android.content.Context;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class SmartTubeRuntime {
    private static boolean initialized;
    private static final String PREFIX = "com.liskovsoft.smartyoutubetv2";
    private SmartTubeRuntime() {}

    public static synchronized void initialize(Context context) {
        if (initialized) return;
        try {
            Context app = context.getApplicationContext();
            System.setProperty("http.keepAlive", "false");

            Class<?> mother = Class.forName(PREFIX + ".common.misc.MotherActivity");
            mother.getMethod("setTvDpiScalingEnabled", boolean.class).invoke(null, false);
            Class<?> screensaver = Class.forName(PREFIX + ".common.misc.ScreensaverManager");
            screensaver.getMethod("setSupported", boolean.class).invoke(null, false);

            Class<?> appPrefs = Class.forName(PREFIX + ".common.prefs.AppPrefs");
            appPrefs.getMethod("instance", Context.class).invoke(null, app);

            Class<?> mediaGroup = Class.forName("com.liskovsoft.mediaserviceinterfaces.data.MediaGroup");
            int home = mediaGroup.getField("TYPE_HOME").getInt(null);
            int subscriptions = mediaGroup.getField("TYPE_SUBSCRIPTIONS").getInt(null);
            int playlists = mediaGroup.getField("TYPE_USER_PLAYLISTS").getInt(null);
            int history = mediaGroup.getField("TYPE_HISTORY").getInt(null);
            int channels = mediaGroup.getField("TYPE_CHANNEL_UPLOADS").getInt(null);
            int myVideos = mediaGroup.getField("TYPE_MY_VIDEOS").getInt(null);
            int shorts = mediaGroup.getField("TYPE_SHORTS").getInt(null);
            int settings = mediaGroup.getField("TYPE_SETTINGS").getInt(null);

            Class<?> sidebar = Class.forName(PREFIX + ".common.app.presenters.service.SidebarService");
            Object sidebarService = sidebar.getMethod("instance", Context.class).invoke(null, app);
            sidebar.getMethod("orderSections", int[].class, int[].class).invoke(
                    sidebarService,
                    new int[]{home, subscriptions, playlists, history, channels, myVideos, shorts},
                    new int[]{settings});

            Class<?> vmClass = Class.forName(PREFIX + ".common.app.views.ViewManager");
            Object vm = vmClass.getMethod("instance", Context.class).invoke(null, app);

            Class<?> splash = Class.forName(PREFIX + ".common.app.views.SplashView");
            Class<?> browseView = Class.forName(PREFIX + ".common.app.views.BrowseView");
            Class<?> playbackView = Class.forName(PREFIX + ".common.app.views.PlaybackView");
            Class<?> dialogView = Class.forName(PREFIX + ".common.app.views.AppDialogView");
            Class<?> searchView = Class.forName(PREFIX + ".common.app.views.SearchView");
            Class<?> signInView = Class.forName(PREFIX + ".common.app.views.SignInView");
            Class<?> addDeviceView = Class.forName(PREFIX + ".common.app.views.AddDeviceView");
            Class<?> channelView = Class.forName(PREFIX + ".common.app.views.ChannelView");
            Class<?> uploadsView = Class.forName(PREFIX + ".common.app.views.ChannelUploadsView");
            Class<?> webBrowserView = Class.forName(PREFIX + ".common.app.views.WebBrowserView");

            Class<?> splashActivity = Class.forName(PREFIX + ".droid.ui.splash.SplashActivity");
            Class<?> browseActivity = Class.forName(PREFIX + ".droid.ui.browse.BrowseActivity");
            Class<?> playbackActivity = Class.forName(PREFIX + ".droid.ui.playback.PlaybackActivity");
            Class<?> dialogActivity = Class.forName(PREFIX + ".droid.ui.dialogs.AppDialogActivity");
            Class<?> searchActivity = Class.forName(PREFIX + ".droid.ui.search.SearchActivity");
            Class<?> signInActivity = Class.forName(PREFIX + ".droid.ui.signin.SignInActivity");
            Class<?> addDeviceActivity = Class.forName(PREFIX + ".droid.ui.adddevice.AddDeviceActivity");
            Class<?> channelActivity = Class.forName(PREFIX + ".droid.ui.channel.ChannelActivity");
            Class<?> uploadsActivity = Class.forName(PREFIX + ".droid.ui.channeluploads.ChannelUploadsActivity");
            Class<?> webBrowserActivity = Class.forName(PREFIX + ".droid.ui.webbrowser.WebBrowserActivity");

            vmClass.getMethod("setRoot", Class.class).invoke(vm, browseActivity);
            Method register2 = vmClass.getMethod("register", Class.class, Class.class);
            Method register3 = vmClass.getMethod("register", Class.class, Class.class, Class.class);

            register2.invoke(vm, splash, splashActivity);
            register2.invoke(vm, browseView, browseActivity);
            register3.invoke(vm, playbackView, playbackActivity, browseActivity);
            register3.invoke(vm, dialogView, dialogActivity, browseActivity);
            register3.invoke(vm, searchView, searchActivity, browseActivity);
            register3.invoke(vm, signInView, signInActivity, browseActivity);
            register3.invoke(vm, addDeviceView, addDeviceActivity, browseActivity);
            register3.invoke(vm, channelView, channelActivity, browseActivity);
            register3.invoke(vm, uploadsView, uploadsActivity, browseActivity);
            register3.invoke(vm, webBrowserView, webBrowserActivity, browseActivity);
            initialized = true;
        } catch (ReflectiveOperationException | RuntimeException error) {
            initialized = false;
            android.util.Log.e("NM7SmartTube", "SmartTube bootstrap failed", error);
        }
    }

    public static synchronized boolean openBrowse(Context context) {
        initialize(context);
        if (!initialized) return false;
        try {
            Class<?> vmClass = Class.forName(PREFIX + ".common.app.views.ViewManager");
            Object vm = vmClass.getMethod("instance", Context.class)
                    .invoke(null, context.getApplicationContext());
            Class<?> browseView = Class.forName(PREFIX + ".common.app.views.BrowseView");
            vmClass.getMethod("startView", Class.class).invoke(vm, browseView);
            return true;
        } catch (ReflectiveOperationException | RuntimeException error) {
            android.util.Log.e("NM7SmartTube", "Unable to start SmartTube BrowseView", error);
            return false;
        }
    }

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
}
