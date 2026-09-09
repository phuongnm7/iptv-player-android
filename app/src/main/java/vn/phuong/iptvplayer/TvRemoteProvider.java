package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** TV remote additions kept separate from the player core so 1.7 playback behaviour stays intact. */
@UnstableApi
public final class TvRemoteProvider extends ContentProvider implements Application.ActivityLifecycleCallbacks {
    @Override public boolean onCreate() {
        if (getContext() != null) ((Application) getContext().getApplicationContext()).registerActivityLifecycleCallbacks(this);
        return true;
    }

    @Override public void onActivityResumed(Activity activity) {
        if (!(activity instanceof PlayerActivity)) return;
        applySourceVisibility(activity);
        Window window = activity.getWindow();
        Window.Callback current = window.getCallback();
        if (!(current instanceof TvWindowCallback)) window.setCallback(new TvWindowCallback(current, (PlayerActivity) activity));
    }

    private static void applySourceVisibility(Activity activity) {
        TextView source = activity.findViewById(R.id.txtPlayerUrl);
        if (source != null) source.setVisibility(AppPreferences.showPlayerSource(activity) ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private static final class TvWindowCallback implements Window.Callback {
        private final Window.Callback base;
        private final PlayerActivity activity;
        TvWindowCallback(Window.Callback base, PlayerActivity activity) { this.base = base; this.activity = activity; }

        @Override public boolean dispatchKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && AppPreferences.isTvInterface(activity)) {
                int key = event.getKeyCode();
                if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    PlayerView view = activity.findViewById(R.id.playerView);
                    if (view != null) { view.showController(); view.requestFocus(); return true; }
                }
                // Requested TV mapping: UP = next channel, DOWN = previous channel.
                if (key == KeyEvent.KEYCODE_DPAD_UP) { if (switchRelative(1)) return true; }
                if (key == KeyEvent.KEYCODE_DPAD_DOWN) { if (switchRelative(-1)) return true; }
                if (key == KeyEvent.KEYCODE_MENU) {
                    AppPreferences.setShowPlayerSource(activity, !AppPreferences.showPlayerSource(activity));
                    applySourceVisibility(activity);
                    Toast.makeText(activity, AppPreferences.showPlayerSource(activity) ? "Đã hiện nguồn phát" : "Đã ẩn nguồn phát", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
            return base.dispatchKeyEvent(event);
        }

        @SuppressWarnings("unchecked")
        private boolean switchRelative(int delta) {
            try {
                Field panelField = PlayerActivity.class.getDeclaredField("quickPanel");
                panelField.setAccessible(true);
                android.view.View panel = (android.view.View) panelField.get(activity);
                if (panel != null && panel.getVisibility() == android.view.View.VISIBLE) return false;

                Field channelsField = PlayerActivity.class.getDeclaredField("quickChannels");
                channelsField.setAccessible(true);
                List<Channel> loaded = (List<Channel>) channelsField.get(activity);
                if (loaded == null || loaded.isEmpty()) return false;
                List<Channel> channels = new ArrayList<>(loaded);

                Method isCurrent = PlayerActivity.class.getDeclaredMethod("isCurrentChannel", Channel.class);
                isCurrent.setAccessible(true);
                int current = -1;
                for (int i = 0; i < channels.size(); i++) {
                    if (Boolean.TRUE.equals(isCurrent.invoke(activity, channels.get(i)))) { current = i; break; }
                }
                if (current < 0) return false;

                int next = (current + delta + channels.size()) % channels.size();
                Method switchChannel = PlayerActivity.class.getDeclaredMethod("switchChannel", Channel.class);
                switchChannel.setAccessible(true);
                switchChannel.invoke(activity, channels.get(next));
                PlayerView view = activity.findViewById(R.id.playerView);
                if (view != null) view.hideController();
                return true;
            } catch (Exception ignored) { return false; }
        }

        @Override public boolean dispatchKeyShortcutEvent(KeyEvent e) { return base.dispatchKeyShortcutEvent(e); }
        @Override public boolean dispatchTouchEvent(android.view.MotionEvent e) { return base.dispatchTouchEvent(e); }
        @Override public boolean dispatchTrackballEvent(android.view.MotionEvent e) { return base.dispatchTrackballEvent(e); }
        @Override public boolean dispatchGenericMotionEvent(android.view.MotionEvent e) { return base.dispatchGenericMotionEvent(e); }
        @Override public boolean dispatchPopulateAccessibilityEvent(android.view.accessibility.AccessibilityEvent e) { return base.dispatchPopulateAccessibilityEvent(e); }
        @Override public android.view.View onCreatePanelView(int featureId) { return base.onCreatePanelView(featureId); }
        @Override public boolean onCreatePanelMenu(int featureId, android.view.Menu menu) { return base.onCreatePanelMenu(featureId, menu); }
        @Override public boolean onPreparePanel(int featureId, android.view.View view, android.view.Menu menu) { return base.onPreparePanel(featureId, view, menu); }
        @Override public boolean onMenuOpened(int featureId, android.view.Menu menu) { return base.onMenuOpened(featureId, menu); }
        @Override public boolean onMenuItemSelected(int featureId, android.view.MenuItem item) { return base.onMenuItemSelected(featureId, item); }
        @Override public void onWindowAttributesChanged(android.view.WindowManager.LayoutParams attrs) { base.onWindowAttributesChanged(attrs); }
        @Override public void onContentChanged() { base.onContentChanged(); }
        @Override public void onWindowFocusChanged(boolean hasFocus) { base.onWindowFocusChanged(hasFocus); }
        @Override public void onAttachedToWindow() { base.onAttachedToWindow(); }
        @Override public void onDetachedFromWindow() { base.onDetachedFromWindow(); }
        @Override public void onPanelClosed(int featureId, android.view.Menu menu) { base.onPanelClosed(featureId, menu); }
        @Override public boolean onSearchRequested() { return base.onSearchRequested(); }
        @Override public boolean onSearchRequested(android.view.SearchEvent event) { return base.onSearchRequested(event); }
        @Override public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback callback) { return base.onWindowStartingActionMode(callback); }
        @Override public android.view.ActionMode onWindowStartingActionMode(android.view.ActionMode.Callback callback, int type) { return base.onWindowStartingActionMode(callback, type); }
        @Override public void onActionModeStarted(android.view.ActionMode mode) { base.onActionModeStarted(mode); }
        @Override public void onActionModeFinished(android.view.ActionMode mode) { base.onActionModeFinished(mode); }
    }

    @Override public void onActivityCreated(Activity a, Bundle b) { }
    @Override public void onActivityStarted(Activity a) { }
    @Override public void onActivityPaused(Activity a) { }
    @Override public void onActivityStopped(Activity a) { }
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }
    @Override public void onActivityDestroyed(Activity a) { }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String sort) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
