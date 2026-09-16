package vn.phuong.iptvplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Bridge entry point to the native SmartTube Droid phone UI. */
public final class SmartTubeHomeActivity extends Activity {
    private static final String BROWSE = "com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SmartTubeRuntime.initialize(this);
        try {
            Class<?> browse = Class.forName(BROWSE);
            Intent intent = new Intent(this, browse);
            if (getIntent().getData() != null) intent.setData(getIntent().getData());
            Bundle extras = getIntent().getExtras();
            if (extras != null) intent.putExtras(extras);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        } catch (ClassNotFoundException e) {
            finish();
        }
        finish();
    }
}
