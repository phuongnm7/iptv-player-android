package vn.phuong.iptvplayer;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity;

/** Native SmartTube Droid home rendered inside the NM7 Mobile process. */
public final class SmartTubeHomeActivity extends BrowseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SmartTubeRuntime.initialize(this);
        super.onCreate(savedInstanceState);

        View root = findViewById(android.R.id.content);
        if (root instanceof android.view.ViewGroup) {
            android.view.View child = ((android.view.ViewGroup) root).getChildCount() > 0
                    ? ((android.view.ViewGroup) root).getChildAt(0) : null;
            if (child instanceof LinearLayout) {
                HomeTabBar.attach((android.app.Activity) this, (LinearLayout) child, true);
            }
        }
    }
}
