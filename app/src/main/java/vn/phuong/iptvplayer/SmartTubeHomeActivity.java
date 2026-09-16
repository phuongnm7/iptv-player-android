package vn.phuong.iptvplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Native SmartTube phone UI entry point inside the same NM7 APK/process. */
public final class SmartTubeHomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SmartTubeRuntime.initialize(this);
        try {
            Class<?> browse = Class.forName("com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity");
            Intent intent = new Intent(this, browse);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        } catch (Throwable e) {
            android.widget.Toast.makeText(this, "Không tải được YouTube SmartTube", android.widget.Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
