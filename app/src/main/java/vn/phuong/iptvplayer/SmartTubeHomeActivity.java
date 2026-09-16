package vn.phuong.iptvplayer;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity;

public final class SmartTubeHomeActivity extends BrowseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SmartTubeRuntime.initialize(this);
        super.onCreate(savedInstanceState);

        ViewGroup content = findViewById(android.R.id.content);
        if (content != null && content.getChildCount() > 0) {
            View child = content.getChildAt(0);
            if (child instanceof LinearLayout) {
                HomeTabBar.attach(this, (LinearLayout) child, true);
            }
        }
    }
}
