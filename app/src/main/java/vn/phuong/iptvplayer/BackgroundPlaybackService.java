package vn.phuong.iptvplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.media3.common.util.UnstableApi;

/** Keeps the mobile player process foreground while playback continues after Home/lock. */
@UnstableApi
public final class BackgroundPlaybackService extends Service {
    static final String EXTRA_CHANNEL_NAME = "channel_name";
    static final String EXTRA_RETURN_MAIN = "return_main";
    private static final String CHANNEL_ID = "background_playback";
    private static final int NOTIFICATION_ID = 180;

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Phát IPTV nền", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Hiển thị khi Nm7 IPTV tiếp tục phát sau khi khóa màn hình hoặc nhấn Home.");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String name = intent == null ? "" : intent.getStringExtra(EXTRA_CHANNEL_NAME);
        boolean returnMain = intent != null && intent.getBooleanExtra(EXTRA_RETURN_MAIN, false);
        Intent launch = new Intent(this, returnMain ? MainActivity.class : PlayerActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent content = PendingIntent.getActivity(this, returnMain ? 1 : 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        Notification notification = builder.setSmallIcon(R.mipmap.ic_launcher).setContentTitle("Nm7 IPTV đang phát nền")
                .setContentText(name == null || name.isEmpty() ? "Chạm để quay lại ứng dụng" : name)
                .setContentIntent(content).setOngoing(true).setCategory(Notification.CATEGORY_SERVICE).build();
        startForeground(NOTIFICATION_ID, notification);
        return START_NOT_STICKY;
    }

    @Override public void onTaskRemoved(Intent rootIntent) { stopSelf(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
