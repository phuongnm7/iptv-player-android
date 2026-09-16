package vn.phuong.iptvplayer;

import android.content.Context;

import com.liskovsoft.smartyoutubetv2.common.app.views.AddDeviceView;
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelUploadsView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelView;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.app.views.SearchView;
import com.liskovsoft.smartyoutubetv2.common.app.views.SignInView;
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.app.views.WebBrowserView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.common.misc.ScreensaverManager;
import com.liskovsoft.smartyoutubetv2.droid.ui.adddevice.AddDeviceActivity;
import com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity;
import com.liskovsoft.smartyoutubetv2.droid.ui.channel.ChannelActivity;
import com.liskovsoft.smartyoutubetv2.droid.ui.channeluploads.ChannelUploadsActivity;
import com.liskovsoft.smartyoutubetv2.droid.ui.dialogs.AppDialogActivity;
import com.liskovsoft.smartyoutubetv2.droid.ui.playback.PlaybackActivity;
import com.liskovsoft.smartyoutubetv2.droid.ui.search.SearchActivity;
import com.liskovsoft.smartyoutubetv2.droid.ui.signin.SignInActivity;
import com.liskovsoft.smartyoutubetv2.droid.ui.splash.SplashActivity;
import com.liskovsoft.smartyoutubetv2.droid.ui.webbrowser.WebBrowserActivity;

/** Initializes SmartTube's shared runtime inside the NM7 Application process. */
public final class SmartTubeRuntime {
    private static boolean initialized;

    private SmartTubeRuntime() {}

    public static synchronized void initialize(Context context) {
        if (initialized) return;
        initialized = true;

        System.setProperty("http.keepAlive", "false");
        MotherActivity.setTvDpiScalingEnabled(false);
        ScreensaverManager.setSupported(false);

        ViewManager viewManager = ViewManager.instance(context.getApplicationContext());
        viewManager.setRoot(BrowseActivity.class);
        viewManager.register(SplashView.class, SplashActivity.class);
        viewManager.register(BrowseView.class, BrowseActivity.class);
        viewManager.register(PlaybackView.class, PlaybackActivity.class, BrowseActivity.class);
        viewManager.register(AppDialogView.class, AppDialogActivity.class, BrowseActivity.class);
        viewManager.register(SearchView.class, SearchActivity.class, BrowseActivity.class);
        viewManager.register(SignInView.class, SignInActivity.class, BrowseActivity.class);
        viewManager.register(AddDeviceView.class, AddDeviceActivity.class, BrowseActivity.class);
        viewManager.register(ChannelView.class, ChannelActivity.class, BrowseActivity.class);
        viewManager.register(ChannelUploadsView.class, ChannelUploadsActivity.class, BrowseActivity.class);
        viewManager.register(WebBrowserView.class, WebBrowserActivity.class, BrowseActivity.class);
    }
}
