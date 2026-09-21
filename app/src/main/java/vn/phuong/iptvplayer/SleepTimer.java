package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

final class SleepTimer {
    private static final String PREFS = "ui-preferences";
    private static final String DEADLINE = "sleep_timer_deadline";
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static WeakReference<Activity> anchor = new WeakReference<>(null);

    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static void showDialog(Activity activity) {
        anchor = new WeakReference<>(activity);
        long deadline = prefs(activity).getLong(DEADLINE, 0L);
        boolean enabled = deadline > System.currentTimeMillis();
        LinearLayout root = new LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 16); root.setPadding(pad, dp(activity, 4), pad, pad);
        LinearLayout header = new LinearLayout(activity); header.setGravity(Gravity.CENTER_VERTICAL); header.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
        TextView title = new TextView(activity); title.setText("Hẹn giờ đóng app"); title.setTextSize(18); title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        Switch toggle = new Switch(activity); toggle.setChecked(enabled); header.addView(toggle, new LinearLayout.LayoutParams(-2, -2)); root.addView(header);
        TextView hint = new TextView(activity); hint.setText("Tự động đóng ứng dụng sau khoảng thời gian đã chọn"); hint.setTextSize(13); root.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        RadioGroup group = new RadioGroup(activity); group.setOrientation(RadioGroup.VERTICAL);
        String[] labels = {"15 phút", "30 phút", "45 phút", "60 phút", "90 phút", "120 phút", "Tùy chỉnh"};
        int[] values = {15, 30, 45, 60, 90, 120, -1};
        for (int i = 0; i < labels.length; i++) { RadioButton radio = new RadioButton(activity); radio.setText(labels[i]); radio.setTextSize(15); radio.setId(View.generateViewId()); radio.setTag(values[i]); radio.setPadding(dp(activity, 4), dp(activity, 6), dp(activity, 4), dp(activity, 6)); group.addView(radio, new RadioGroup.LayoutParams(-1, -2)); }
        EditText custom = new EditText(activity); custom.setSingleLine(true); custom.setInputType(InputType.TYPE_CLASS_NUMBER); custom.setHint("Số phút (1 - 480)"); custom.setTextSize(15); custom.setVisibility(View.GONE);
        LinearLayout.LayoutParams customParams = new LinearLayout.LayoutParams(-1, -2); customParams.setMargins(dp(activity, 44), 0, dp(activity, 4), dp(activity, 4)); root.addView(group, new LinearLayout.LayoutParams(-1, -2)); root.addView(custom, customParams);
        if (enabled) { TextView active = new TextView(activity); active.setText("Đang hẹn: " + formatRemaining(deadline - System.currentTimeMillis())); active.setTextSize(13); active.setPadding(0, dp(activity, 8), 0, 0); root.addView(active, new LinearLayout.LayoutParams(-1, -2)); }
        toggle.setOnCheckedChangeListener((button, checked) -> { group.setEnabled(checked); custom.setEnabled(checked); for (int i = 0; i < group.getChildCount(); i++) group.getChildAt(i).setEnabled(checked); if (!checked) cancel(activity, false); });
        group.setOnCheckedChangeListener((g, checkedId) -> { RadioButton selected = g.findViewById(checkedId); if (selected != null && ((Integer) selected.getTag()) == -1) { custom.setVisibility(View.VISIBLE); custom.requestFocus(); } else custom.setVisibility(View.GONE); });
        if (!enabled) toggle.setChecked(true);
        int defaultMinutes = enabled ? nearestPreset(deadline - System.currentTimeMillis()) : 60;
        for (int i = 0; i < group.getChildCount(); i++) { RadioButton radio = (RadioButton) group.getChildAt(i); if ((Integer) radio.getTag() == defaultMinutes) { radio.setChecked(true); break; } }
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Hẹn giờ đóng app").setView(root).setNegativeButton("Hủy", null).setNeutralButton("Tắt hẹn giờ", null).setPositiveButton("Lưu", null).create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> { cancel(activity, true); dialog.dismiss(); });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                if (!toggle.isChecked()) { cancel(activity, true); dialog.dismiss(); return; }
                RadioButton selected = group.findViewById(group.getCheckedRadioButtonId());
                if (selected == null) { Toast.makeText(activity, "Hãy chọn thời gian hẹn giờ", Toast.LENGTH_SHORT).show(); return; }
                int minutes = (Integer) selected.getTag();
                if (minutes == -1) {
                    try { minutes = Integer.parseInt(custom.getText().toString().trim()); }
                    catch (NumberFormatException e) { Toast.makeText(activity, "Nhập số phút từ 1 đến 480", Toast.LENGTH_SHORT).show(); return; }
                    if (minutes < 1 || minutes > 480) { Toast.makeText(activity, "Nhập số phút từ 1 đến 480", Toast.LENGTH_SHORT).show(); return; }
                }
                setMinutes(activity, minutes); dialog.dismiss();
            });
        });
        dialog.show();
    }

    private static int nearestPreset(long remainingMs) { long minutes = Math.max(1L, remainingMs / 60000L); int[] presets = {15,30,45,60,90,120}; int best=60; long diff=Long.MAX_VALUE; for(int p:presets){long d=Math.abs(minutes-p);if(d<diff){diff=d;best=p;}} return best; }
    private static void setMinutes(Activity activity, int minutes) { long deadline=System.currentTimeMillis()+minutes*60000L; prefs(activity).edit().putLong(DEADLINE,deadline).apply(); schedule(activity,deadline); Toast.makeText(activity,"Đã hẹn đóng app sau "+minutes+" phút",Toast.LENGTH_SHORT).show(); }
    private static void cancel(Activity activity, boolean showToast) { prefs(activity).edit().remove(DEADLINE).apply(); HANDLER.removeCallbacksAndMessages(null); if(showToast) Toast.makeText(activity,"Đã tắt hẹn giờ đóng app",Toast.LENGTH_SHORT).show(); }
    static void restore(Activity activity) { anchor=new WeakReference<>(activity); long deadline=prefs(activity).getLong(DEADLINE,0L); if(deadline<=0L)return; if(deadline<=System.currentTimeMillis()){prefs(activity).edit().remove(DEADLINE).apply();return;} schedule(activity,deadline); }
    private static void schedule(final Activity activity, final long deadline) { anchor=new WeakReference<>(activity); HANDLER.removeCallbacksAndMessages(null); long remaining=deadline-System.currentTimeMillis(); if(remaining<=0L){expire(activity);return;} HANDLER.postDelayed(new Runnable(){@Override public void run(){long left=deadline-System.currentTimeMillis();if(left<=0L)expire(activity);else schedule(activity,deadline);}},Math.min(remaining,60000L)); }
    private static void expire(Activity activity) { if(activity==null||activity.isFinishing())return; prefs(activity).edit().remove(DEADLINE).apply(); activity.runOnUiThread(()->{try{activity.finishAffinity();}catch(RuntimeException ignored){activity.finish();}}); }
    private static String formatRemaining(long millis) { long total=Math.max(0L,millis/60000L); long hours=total/60L,minutes=total%60L; return hours>0?hours+" giờ "+minutes+" phút":minutes+" phút"; }
    private static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
}
