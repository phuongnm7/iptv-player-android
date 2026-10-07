package vn.phuong.iptvplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MainActivityStartupTest {
    @Test
    public void redesignedLauncherLayoutInflatesAndContainsRequiredViews() {
        Context context = RuntimeEnvironment.getApplication();
        View root = LayoutInflater.from(context).inflate(R.layout.activity_main, null);

        assertTrue("mainRoot must stay LinearLayout for 1.10.112 compatibility",
                root instanceof LinearLayout);
        assertNotNull(root.findViewById(R.id.mainFrame));
        assertNotNull(root.findViewById(R.id.playerLayoutHost));
        assertNotNull(root.findViewById(R.id.mobileHeader));
        assertNotNull(root.findViewById(R.id.imgAppLogo));
        assertNotNull(root.findViewById(R.id.txtAppTitle));
        assertNotNull(root.findViewById(R.id.btnMenu));
        assertNotNull(root.findViewById(R.id.btnSearch));
        assertNotNull(root.findViewById(R.id.contentContainer));
        assertNotNull(root.findViewById(R.id.btnYoutube));
        assertNotNull(root.findViewById(R.id.btnLiveEvents));
        assertNotNull(root.findViewById(R.id.btnChannel));
        assertNotNull(root.findViewById(R.id.btnTvMode));
        assertNotNull(root.findViewById(R.id.btnPlaylist));
        assertNotNull(root.findViewById(R.id.drawerScrim));
        assertNotNull(root.findViewById(R.id.drawerPanel));
    }

    @Test
    public void inlinePlayerPrefersDedicatedHostInsideCompatibleRoot() {
        Context context = RuntimeEnvironment.getApplication();
        LinearLayout root = new LinearLayout(context);
        LinearLayout playerHost = new LinearLayout(context);
        playerHost.setId(R.id.playerLayoutHost);
        root.addView(playerHost);

        assertSame(playerHost, MobileInlinePlayerProviderV2.resolveLayoutHost(root));
        LinearLayout plain = new LinearLayout(context);
        assertSame(plain, MobileInlinePlayerProviderV2.resolveLayoutHost(plain));
    }

    @Test
    public void inlinePlayerResolvesFirstChildForGenericFrameShell() {
        Context context = RuntimeEnvironment.getApplication();
        FrameLayout shell = new FrameLayout(context);
        LinearLayout content = new LinearLayout(context);
        shell.addView(content);

        assertSame(content, MobileInlinePlayerProviderV2.resolveLayoutHost(shell));
    }
}
