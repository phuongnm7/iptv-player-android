package vn.phuong.iptvplayer;

import android.content.Context;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Device-local playlist entries used by the reference-style Mobile Playlist screen. */
final class MobilePlaylistStore {
    private static final String PREFS = "mobile-playlists-v2";
    private static final String KEY = "entries";
    private static final int MAX_ENTRIES = 50;

    static final class Entry {
        final String id;
        final String name;
        final String type; // url | local
        final String value; // URL or private file name

        Entry(String id, String name, String type, String value) {
            this.id = id == null ? "" : id;
            this.name = name == null ? "" : name;
            this.type = type == null ? "url" : type;
            this.value = value == null ? "" : value;
        }

        boolean isLocal() { return "local".equals(type); }
    }

    private MobilePlaylistStore() {}

    static List<Entry> load(Context context) {
        List<Entry> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"));
            for (int i = 0; i < array.length() && result.size() < MAX_ENTRIES; i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "");
                String name = item.optString("name", "My Playlist");
                String type = item.optString("type", "url");
                String value = item.optString("value", "");
                if (!id.isEmpty() && !value.isEmpty() && ("url".equals(type) || "local".equals(type))) {
                    result.add(new Entry(id, name, type, value));
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    static void addUrl(Context context, String name, String url) {
        String cleanUrl = url == null ? "" : url.trim();
        if (!PlaylistSourceStore.isValid(cleanUrl)) throw new IllegalArgumentException("URL playlist không hợp lệ");
        List<Entry> values = new ArrayList<>(load(context));
        String cleanName = cleanName(name, "Playlist");
        for (int i = values.size() - 1; i >= 0; i--) {
            Entry old = values.get(i);
            if ("url".equals(old.type) && cleanUrl.equals(old.value)) values.remove(i);
        }
        values.add(0, new Entry(UUID.randomUUID().toString(), cleanName, "url", cleanUrl));
        save(context, values);
    }

    static Entry addLocal(Context context, String name, Uri uri) throws Exception {
        if (uri == null) throw new IllegalArgumentException("Tệp playlist không hợp lệ");
        File dir = new File(context.getFilesDir(), "mobile-playlists");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Không tạo được thư mục playlist");
        String id = UUID.randomUUID().toString();
        File file = new File(dir, id + ".m3u");
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             OutputStream output = new java.io.FileOutputStream(file)) {
            if (input == null) throw new IllegalArgumentException("Không thể mở tệp playlist");
            byte[] buffer = new byte[16 * 1024];
            long total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > 8L * 1024L * 1024L) throw new IllegalArgumentException("Playlist lớn hơn 8 MB");
                output.write(buffer, 0, read);
            }
        } catch (Exception error) {
            file.delete();
            throw error;
        }
        String displayName = cleanName(name, displayFileName(uri));
        List<Entry> values = new ArrayList<>(load(context));
        values.add(0, new Entry(id, displayName, "local", file.getName()));
        while (values.size() > MAX_ENTRIES) {
            Entry removed = values.remove(values.size() - 1);
            deleteFile(context, removed);
        }
        save(context, values);
        return new Entry(id, displayName, "local", file.getName());
    }

    static InputStream open(Context context, Entry entry) throws Exception {
        if (entry == null || !entry.isLocal()) throw new IllegalArgumentException("Không phải playlist local");
        return new FileInputStream(new File(new File(context.getFilesDir(), "mobile-playlists"), entry.value));
    }

    static void rename(Context context, String id, String name) {
        List<Entry> values = new ArrayList<>(load(context));
        String clean = cleanName(name, "My Playlist");
        for (int i = 0; i < values.size(); i++) {
            Entry value = values.get(i);
            if (value.id.equals(id)) {
                values.set(i, new Entry(value.id, clean, value.type, value.value));
                break;
            }
        }
        save(context, values);
    }

    static void remove(Context context, String id) {
        List<Entry> values = new ArrayList<>(load(context));
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i).id.equals(id)) {
                Entry removed = values.remove(i);
                deleteFile(context, removed);
            }
        }
        save(context, values);
    }

    private static void save(Context context, List<Entry> values) {
        JSONArray array = new JSONArray();
        for (Entry value : values) {
            try {
                array.put(new JSONObject()
                        .put("id", value.id)
                        .put("name", value.name)
                        .put("type", value.type)
                        .put("value", value.value));
            } catch (Exception ignored) {}
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY, array.toString()).apply();
    }

    private static void deleteFile(Context context, Entry entry) {
        if (entry == null || !entry.isLocal()) return;
        try {
            new File(new File(context.getFilesDir(), "mobile-playlists"), entry.value).delete();
        } catch (RuntimeException ignored) {}
    }

    private static String cleanName(String name, String fallback) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) value = fallback;
        return value.length() > 80 ? value.substring(0, 80) : value;
    }

    private static String displayFileName(Uri uri) {
        String text = uri == null ? "" : uri.toString();
        int slash = text.lastIndexOf('/');
        String value = slash >= 0 ? text.substring(slash + 1) : text;
        try {
            value = Uri.decode(value);
        } catch (RuntimeException ignored) {}
        return value.isEmpty() ? "Local playlist" : value;
    }
}
