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
    public void referenceLauncherLayoutInflatesAndContainsRequiredViews() {
        Context context = RuntimeEnvironment.getApplication();
        View root = LayoutInflater.from(context).inflate(R.layout.activity_main, null);

        assertTrue(root instanceof FrameLayout);
        assertNotNull(root.findViewById(R.id.btnMenu));
        assertNotNull(root.findViewById(R.id.btnSearch));
        assertNotNull(root.findViewById(R.id.btnProfile));
        assertNotNull(root.findViewById(R.id.statusRow));
        assertNotNull(root.findViewById(R.id.groupRow));
        assertNotNull(root.findViewById(R.id.listChannels));
        assertNotNull(root.findViewById(R.id.btnLiveEvents));
        assertNotNull(root.findViewById(R.id.btnChannel));
        assertNotNull(root.findViewById(R.id.btnTvMode));
        assertNotNull(root.findViewById(R.id.btnHighlights));
        assertNotNull(root.findViewById(R.id.btnPlaylist));
        assertNotNull(root.findViewById(R.id.drawerScrim));
        assertNotNull(root.findViewById(R.id.drawerPanel));
    }

    @Test
    public void inlinePlayerResolvesVerticalHostInsideReferenceFrameLayout() {
        Context context = RuntimeEnvironment.getApplication();
        FrameLayout shell = new FrameLayout(context);
        LinearLayout content = new LinearLayout(context);
        shell.addView(content);

        assertSame(content, MobileInlinePlayerProviderV2.resolveLayoutHost(shell));
        assertSame(content, MobileInlinePlayerProviderV2.resolveLayoutHost(content));
    }
}
