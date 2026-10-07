package vn.phuong.iptvplayer;

import android.app.Application;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ActivityController;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(application = MainActivityStartupTest.TestApplication.class, sdk = 34)
public class MainActivityStartupTest {
    public static class TestApplication extends Application {
    }

    @Test
    public void mainActivityInflatesAndBindsReferenceUi() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).create();
        MainActivity activity = controller.get();

        assertNotNull(activity.findViewById(R.id.mainRoot));
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

        controller.pause().stop().destroy();
    }
}
