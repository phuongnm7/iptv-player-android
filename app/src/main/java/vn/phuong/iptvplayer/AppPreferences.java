package vn.phuong.iptvplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.UiModeManager;
import android.content.res.Configuration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Private, device-only UI preferences. Channel identifiers are hashed before storage. */
final class AppPreferences {
    private static final String FILE = "ui-preferences";
    private static final String FAVORITES = "favorites";
    private static final String RECENT = "recent";
    private static final int MAX_RECENT = 30;

    private static SharedPreferences prefs(Context context) { return context.getSharedPreferences(FILE, Context.MODE_PRIVATE); }
    static String id(Channel channel) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(channel.identityKey().getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(digest.length * 2);
            for (byte value : digest) out.append(String.format(java.util.Locale.ROOT, "%02x", value & 0xff));
            return out.toString();
        } catch (Exception impossible) { return Integer.toHexString(channel.identityKey().hashCode()); }
    }
    static boolean isFavorite(Context context, Channel channel) { return prefs(context).getStringSet(FAVORITES, java.util.Collections.emptySet()).contains(id(channel)); }
    static boolean toggleFavorite(Context context, Channel channel) {
        Set<String> values = new LinkedHashSet<>(prefs(context).getStringSet(FAVORITES, java.util.Collections.emptySet()));
        String id = id(channel); boolean added;
        if (values.contains(id)) { values.remove(id); added = false; } else { values.add(id); added = true; }
        prefs(context).edit().putStringSet(FAVORITES, values).apply(); return added;
    }
    static void recordRecent(Context context, Channel channel) {
        List<String> values = recentIds(context); String id = id(channel);
        values.remove(id); values.add(0, id);
        if (values.size() > MAX_RECENT) values = new ArrayList<>(values.subList(0, MAX_RECENT));
        prefs(context).edit().putString(RECENT, android.text.TextUtils.join(",", values)).apply();
    }
    static boolean isRecent(Context context, Channel channel) { return recentIds(context).contains(id(channel)); }
    static int recentRank(Context context, Channel channel) { int index = recentIds(context).indexOf(id(channel)); return index < 0 ? Integer.MAX_VALUE : index; }
    static void clearRecent(Context context) { prefs(context).edit().remove(RECENT).apply(); }
    private static List<String> recentIds(Context context) {
        String value = prefs(context).getString(RECENT, "");
        if (value == null || value.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(value.split(",")));
    }
    static boolean showUrls(Context context) { return prefs(context).getBoolean("show_urls", false); }
    static void setShowUrls(Context context, boolean value) { prefs(context).edit().putBoolean("show_urls", value).apply(); }
    static boolean compactRows(Context context) { return prefs(context).getBoolean("compact_rows", false); }
    static void setCompactRows(Context context, boolean value) { prefs(context).edit().putBoolean("compact_rows", value).apply(); }
    static boolean showFps(Context context) { return prefs(context).getBoolean("show_fps", true); }
    static void setShowFps(Context context, boolean value) { prefs(context).edit().putBoolean("show_fps", value).apply(); }
    static boolean showClock(Context context) { return prefs(context).getBoolean("show_clock", true); }
    static void setShowClock(Context context, boolean value) { prefs(context).edit().putBoolean("show_clock", value).apply(); }
    static boolean showPlayerSource(Context context) { return prefs(context).getBoolean("show_player_source", false); }
    static void setShowPlayerSource(Context context, boolean value) {
        // This preference controls the player UI and is expected to survive an
        // immediate process restart, so persist it synchronously.
        prefs(context).edit().putBoolean("show_player_source", value).commit();
    }
    static boolean backgroundPlayback(Context context) { return prefs(context).getBoolean("background_playback", false); }
    static void setBackgroundPlayback(Context context, boolean value) { prefs(context).edit().putBoolean("background_playback", value).apply(); }
    static String interfaceMode(Context context) { return prefs(context).getString("interface_mode", "auto"); }
    static void setInterfaceMode(Context context, String value) { prefs(context).edit().putString("interface_mode", value).apply(); }
    static boolean isTvInterface(Context context) {
        String value = interfaceMode(context); if ("tv".equals(value)) return true; if ("mobile".equals(value)) return false;
        UiModeManager manager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return manager != null && manager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }
}
