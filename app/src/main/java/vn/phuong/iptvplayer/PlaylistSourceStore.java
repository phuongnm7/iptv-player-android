package vn.phuong.iptvplayer;

import android.content.Context;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** User-managed playlist URLs, stored only in app-private preferences on the device. */
final class PlaylistSourceStore {
    private static final String FILE = "playlist-sources";
    private static final String KEY = "sources";
    private static final int MAX_SOURCES = 50;

    static final class Source {
        final String name, url;
        Source(String name, String url) { this.name = name; this.url = url; }
    }

    static List<Source> load(Context context) {
        List<Source> result = new ArrayList<>();
        try {
            JSONArray values = new JSONArray(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, "[]"));
            for (int i = 0; i < values.length() && result.size() < MAX_SOURCES; i++) {
                JSONObject item = values.getJSONObject(i);
                String url = item.optString("url", "").trim();
                if (isValid(url)) result.add(new Source(cleanName(item.optString("name"), url), url));
            }
        } catch (Exception ignored) { }
        return result;
    }

    static void add(Context context, String name, String url) throws Exception {
        String cleanUrl = url == null ? "" : url.trim();
        if (!isValid(cleanUrl)) throw new IllegalArgumentException("Link playlist phải bắt đầu bằng http:// hoặc https://");
        List<Source> values = load(context);
        String requestedName = name == null ? "" : name.trim();
        for (int i = values.size() - 1; i >= 0; i--) if (values.get(i).url.equals(cleanUrl)) {
            if (requestedName.isEmpty()) requestedName = values.get(i).name;
            values.remove(i);
        }
        values.add(0, new Source(cleanName(requestedName, cleanUrl), cleanUrl));
        if (values.size() > MAX_SOURCES) values = new ArrayList<>(values.subList(0, MAX_SOURCES));
        save(context, values);
    }

    static void remove(Context context, int index) throws Exception {
        List<Source> values = load(context);
        if (index >= 0 && index < values.size()) { values.remove(index); save(context, values); }
    }

    private static void save(Context context, List<Source> values) throws Exception {
        JSONArray array = new JSONArray();
        for (Source value : values) array.put(new JSONObject().put("name", value.name).put("url", value.url));
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply();
    }

    static boolean isValid(String url) { return url != null && (url.startsWith("https://") || url.startsWith("http://")); }
    private static String cleanName(String name, String url) {
        String clean = name == null ? "" : name.trim();
        if (!clean.isEmpty()) return clean.length() > 80 ? clean.substring(0, 80) : clean;
        String host = Uri.parse(url).getHost();
        return host == null || host.isEmpty() ? "Nguồn IPTV" : host;
    }
}
