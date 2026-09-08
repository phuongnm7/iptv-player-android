package vn.phuong.iptvplayer;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Device-only state. The manifest excludes it from Android backup. */
final class SessionStore {
    static final ExecutorService IO = Executors.newSingleThreadExecutor();

    static List<Channel> snapshot(List<Channel> channels) {
        List<Channel> copy = new ArrayList<>();
        for (Channel c : channels) {
            Channel n = new Channel(c.name(), c.group(), c.url(), c.logo(), c.tvgId(), c.headers());
            n.setSelected(c.selected());
            n.setOriginalExtInf(c.originalExtInf());
            n.options().addAll(c.options());
            copy.add(n);
        }
        return copy;
    }

    static void save(Context context, List<Channel> channels, String source, int duplicates, int missing) throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("playlist", new M3uParser().exportAll(channels));
        obj.put("source", source);
        obj.put("duplicates", duplicates);
        obj.put("missing", missing);
        JSONArray unchecked = new JSONArray();
        for (Channel channel : channels) if (!channel.selected()) unchecked.put(channel.identityKey());
        obj.put("unchecked", unchecked);
        AtomicFile file = file(context);
        FileOutputStream out = null;
        try {
            out = file.startWrite();
            out.write(obj.toString().getBytes(StandardCharsets.UTF_8));
            file.finishWrite(out);
        } catch (Exception error) {
            if (out != null) file.failWrite(out);
            throw error;
        }
    }

    static State load(Context context) throws Exception {
        AtomicFile file = file(context);
        if (!file.getBaseFile().exists()) return null;
        JSONObject obj = new JSONObject(new String(file.readFully(), StandardCharsets.UTF_8));
        M3uParser.Result result = new M3uParser().parse(obj.getString("playlist"), "");
        Set<String> unchecked = new HashSet<>();
        JSONArray arr = obj.optJSONArray("unchecked");
        if (arr != null) for (int i = 0; i < arr.length(); i++) unchecked.add(arr.getString(i));
        for (Channel channel : result.channels) channel.setSelected(!unchecked.contains(channel.identityKey()));
        return new State(new M3uParser.Result(result.channels, obj.optInt("duplicates"), obj.optInt("missing")),
                obj.optString("source", ""));
    }

    private static AtomicFile file(Context context) {
        return new AtomicFile(new File(context.getFilesDir(), "playlist-session.json"));
    }

    static final class State {
        final M3uParser.Result result;
        final String source;
        State(M3uParser.Result result, String source) { this.result = result; this.source = source; }
    }
}
