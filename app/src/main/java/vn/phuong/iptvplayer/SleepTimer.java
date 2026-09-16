package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;

/** In-app sleep timer that closes the current task when it expires. */
final class SleepTimer {
    private static final String PREFS = "ui-preferences";
    private static final String DEADLINE = "sleep_timer_deadline";
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static WeakReference<Activity> anchor = new WeakReference<>(null);
    private static final Runnable EXPIRY = () -> {
        Activity activity = anchor.get();
        if (activity == null || activity.isFinishing()) return;
        long deadline = prefs(activity).getLong(DEADLINE, 0L);
        if (deadline == 0L || System.currentTimeMillis() < deadline) return;
        prefs(activity).edit().remove(DEADLINE).apply();
        activity.runOnUiThread(() -> {
            try { activity.finishAffinity(); }
            catch (RuntimeException ignored) { activity.finish(); }
        });
    };

    private static android.content.SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static void showDialog(Activity activity) {
        anchor = new WeakReference<>(activity);
        long current = prefs(activity).getLong(DEADLINE, 0L);
        String status = current > System.currentTimeMillis()
                ? "Đang hẹn đóng sau " + formatRemaining(current - System.currentTimeMillis())
                : "Chưa đặt hẹn giờ";
        String[] options = {"Tắt hẹn giờ", "15 phút", "30 phút", "45 phút", "60 phút", "90 phút", "120 phút", "Tùy chỉnh"};
        new AlertDialog.Builder(activity)
                .setTitle("Hẹn giờ đóng app")
                .setMessage(status)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { cancel(activity); return; }
                    if (which == 7) { custom(activity); return; }
                    setMinutes(activity, new int[]{0,15,30,45,60,90,120}[which]);
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private static void setMinutes(Activity activity, int minutes) {
        long deadline = System.currentTimeMillis() + minutes * 60_000L;
        prefs(activity).edit().putLong(DEADLINE, deadline).apply();
        schedule(activity, deadline);
        android.widget.Toast.makeText(activity, "Đã hẹn đóng app sau " + minutes + " phút", android.widget.Toast.LENGTH_SHORT).show();
    }

    private static void custom(Activity activity) {
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint("Số phút, ví dụ 120");
        LinearLayout box = new LinearLayout(activity);
        box.setPadding(36, 0, 36, 0);
        box.addView(input, new LinearLayout.LayoutParams(-1, -2));
        new AlertDialog.Builder(activity).setTitle("Tùy chỉnh hẹn giờ").setView(box)
                .setPositiveButton("Bắt đầu", (d, w) -> {
                    try {
                        int minutes = Integer.parseInt(input.getText().toString().trim());
                        if (minutes < 1 || minutes > 1440) throw new NumberFormatException();
                        setMinutes(activity, minutes);
                    } catch (NumberFormatException e) {
                        android.widget.Toast.makeText(activity, "Nhập số phút từ 1 đến 1440", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("Hủy", null).show();
    }

    private static void cancel(Activity activity) {
        prefs(activity).edit().remove(DEADLINE).apply();
        HANDLER.removeCallbacks(EXPIRY);
        android.widget.Toast.makeText(activity, "Đã tắt hẹn giờ đóng app", android.widget.Toast.LENGTH_SHORT).show();
    }

    static void restore(Activity activity) {
        anchor = new WeakReference<>(activity);
        long deadline = prefs(activity).getLong(DEADLINE, 0L);
        if (deadline <= 0L) return;
        if (deadline <= System.currentTimeMillis()) {
            prefs(activity).edit().remove(DEADLINE).apply();
            return;
        }
        schedule(activity, deadline);
    }

    private static void schedule(Activity activity, long deadline) {
        anchor = new WeakReference<>(activity);
        HANDLER.removeCallbacks(EXPIRY);
        HANDLER.postDelayed(EXPIRY, Math.max(1L, deadline - System.currentTimeMillis()));
    }

    private static String formatRemaining(long millis) {
        long total = Math.max(0L, millis / 60_000L);
        long hours = total / 60L, minutes = total % 60L;
        return hours > 0 ? hours + " giờ " + minutes + " phút" : minutes + " phút";
    }
}
