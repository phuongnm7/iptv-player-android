package vn.phuong.iptvplayer;

import android.app.Application;
import android.view.View;
import android.widget.FrameLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ActivityController;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(application = MainActivityStartupTest.TestApplication.class, sdk = 34)
public class MainActivityStartupTest {
    public static class TestApplication extends Application {
    }

    @Test
    public void mainActivityInflatesAndBindsReferenceUi() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create();
        MainActivity activity = controller.get();
        // MainActivity starts its shared background executor during normal startup.
        // Stop it in this JVM test so the test process cannot remain alive waiting
        // on restore/network work after the assertions have completed.
        SessionStore.IO.shutdownNow();

        View root = activity.findViewById(R.id.mainRoot);
        assertNotNull(root);
        assertTrue("reference launcher shell must be a ViewGroup", root instanceof android.view.ViewGroup);
        assertTrue("reference launcher shell must remain a FrameLayout for overlays", root instanceof FrameLayout);
        assertNotNull(activity.findViewById(R.id.btnMenu));
        assertNotNull(activity.findViewById(R.id.btnSearch));
        assertNotNull(activity.findViewById(R.id.btnProfile));
        assertNotNull(activity.findViewById(R.id.statusRow));
        assertNotNull(activity.findViewById(R.id.groupRow));
        assertNotNull(activity.findViewById(R.id.listChannels));
        assertNotNull(activity.findViewById(R.id.btnLiveEvents));
        assertNotNull(activity.findViewById(R.id.btnChannel));
        assertNotNull(activity.findViewById(R.id.btnTvMode));
        assertNotNull(activity.findViewById(R.id.btnHighlights));
        assertNotNull(activity.findViewById(R.id.btnPlaylist));
        assertNotNull(activity.findViewById(R.id.drawerScrim));
        assertNotNull(activity.findViewById(R.id.drawerPanel));

        // Exercise the provider path that runs immediately after Activity resume.
        // This used to cast mainRoot directly to LinearLayout and crashed the app
        // after the Android launch splash when the reference UI changed the shell
        // to FrameLayout.
        MobileInlinePlayerProviderV2 provider = new MobileInlinePlayerProviderV2();
        provider.attachInfo(activity.getApplication(), null);
        provider.onActivityResumed(activity);
        provider.onActivityPaused(activity);
        provider.onActivityDestroyed(activity);

        controller.pause().stop().destroy();
    }
}
