package vn.phuong.iptvplayer.movie;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MoviePluginManager {
    private static final List<MoviePlugin> PLUGINS = new ArrayList<>();
    private static final String PREFS = "movie_plugins";
    private MoviePluginManager() {}

    public static synchronized void register(MoviePlugin plugin) {
        if (plugin == null || plugin.id() == null || plugin.id().trim().isEmpty()) return;
        for (MoviePlugin existing : PLUGINS) {
            if (plugin.id().equals(existing.id())) return;
        }
        PLUGINS.add(plugin);
    }

    public static synchronized void unregister(Context context, String id) {
        if (id == null) return;
        PLUGINS.removeIf(p -> id.equals(p.id()));
        if (context != null) context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove("enabled_" + id).apply();
    }

    public static synchronized void unregister(String id) {
        if (id == null) return;
        PLUGINS.removeIf(p -> id.equals(p.id()));
    }

    public static synchronized List<MoviePlugin> all() {
        return Collections.unmodifiableList(new ArrayList<>(PLUGINS));
    }

    public static synchronized List<MoviePlugin> enabled(Context context) {
        List<MoviePlugin> result = new ArrayList<>();
        for (MoviePlugin plugin : PLUGINS) {
            if (isEnabled(context, plugin.id())) result.add(plugin);
        }
        return result;
    }

    public static synchronized MoviePlugin find(String id) {
        if (id == null) return null;
        for (MoviePlugin p : PLUGINS) if (id.equals(p.id())) return p;
        return null;
    }

    public static synchronized boolean isEnabled(Context context, String id) {
        if (context == null || id == null) return false;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("enabled_" + id, true);
    }

    public static synchronized void setEnabled(Context context, String id, boolean enabled) {
        if (context == null || id == null) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("enabled_" + id, enabled).apply();
    }

    public static synchronized void registerDefaults() {
        if (find("novahd") == null) register(new NovaHdPlugin());
    }

    public static String contentId(String pluginId, String id) {
        return pluginId + ":" + (id == null ? "" : id);
    }

    public static String[] splitContentId(String id) {
        if (id == null) return new String[]{"", ""};
        int p = id.indexOf(':');
        return p <= 0 ? new String[]{"", id} : new String[]{id.substring(0, p), id.substring(p + 1)};
    }
}
