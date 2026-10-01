package vn.phuong.iptvplayer.movie;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import vn.phuong.iptvplayer.movie.MovieModels.MovieDetail;
import vn.phuong.iptvplayer.movie.MovieModels.MovieEpisode;
import vn.phuong.iptvplayer.movie.MovieModels.MovieItem;
import vn.phuong.iptvplayer.movie.MovieModels.Playback;
import vn.phuong.iptvplayer.movie.MovieModels.Subtitle;

public final class MovieJsSource implements MovieSource {
    private static final int MAX_EMBED_HOPS = 3;

    private final String sourceId;
    private final String name;
    private final MovieJsRuntime js;
    private final java.util.concurrent.ExecutorService io =
            java.util.concurrent.Executors.newFixedThreadPool(2);

    public MovieJsSource(Context context, String fileName, String script) {
        String stableName = fileName == null ? "movie-plugin.js" : fileName;
        this.sourceId = "plugin_" + Math.abs(stableName.hashCode());
        this.name = stableName.endsWith(".js")
                ? stableName.substring(0, stableName.length() - 3)
                : stableName;
        js = new MovieJsRuntime(context, script == null ? "" : script);
    }

    @Override public String id() { return sourceId; }
    @Override public String name() { return name; }
    @Override public boolean isPlugin() { return true; }

    @Override public void loadHome(Callback<List<MovieItem>> cb) {
        js.call("getUrlList", new MovieJsRuntime.Callback() {
            @Override public void done(String url) {
                fetchListAndParse(url, "parseListResponse", cb);
            }
            @Override public void error(String m) {
                cb.onError("Plugin không tạo được URL danh sách: " + m);
            }
        }, MovieJsRuntime.quote("trending"), MovieJsRuntime.quote("{\"page\":1}"));
    }

    @Override public void search(String query, Callback<List<MovieItem>> cb) {
        js.call("getUrlSearch", new MovieJsRuntime.Callback() {
            @Override public void done(String url) {
                fetchListAndParse(url, "parseSearchResponse", cb);
            }
            @Override public void error(String m) {
                cb.onError("Plugin không hỗ trợ tìm kiếm: " + m);
            }
        }, MovieJsRuntime.quote(query), MovieJsRuntime.quote("{\"page\":1}"));
    }

    private void fetchListAndParse(String url, String parser, Callback<List<MovieItem>> cb) {
        final String requestUrl = cleanResult(url);
        if (!isHttp(requestUrl)) {
            cb.onError("Plugin trả về URL danh sách không hợp lệ");
            return;
        }
        io.execute(() -> {
            try {
                String body = MovieHttp.get(requestUrl, Collections.emptyMap());
                js.call(parser, new MovieJsRuntime.Callback() {
                    @Override public void done(String parsed) {
                        List<MovieItem> items = parseItems(parsed);
                        if (items.isEmpty() && looksJsonObject(body)) {
                            items = parseItems(body);
                        }
                        cb.onSuccess(items);
                    }

                    @Override public void error(String m) {
                        cb.onError("Plugin parse danh sách: " + m);
                    }
                }, MovieJsRuntime.quote(body), MovieJsRuntime.quote(requestUrl));
            } catch (Exception e) {
                cb.onError("Plugin HTTP danh sách: " + safe(e));
            }
        });
    }

    @Override public void loadDetail(MovieItem item, Callback<MovieDetail> cb) {
        final String cleanId = MovieJson.stripPrefix(item.id);
        js.call("getUrlDetail", new MovieJsRuntime.Callback() {
            @Override public void done(String result) {
                String url = cleanResult(result);
                if (!isHttp(url)) {
                    fallbackDetail(item, cb, "Plugin trả về URL chi tiết không hợp lệ");
                    return;
                }
                fetchDetailUrl(url, item, cb);
            }

            @Override public void error(String m) {
                fallbackDetail(item, cb, m);
            }
        }, MovieJsRuntime.quote(cleanId));
    }

    private void fallbackDetail(MovieItem item, Callback<MovieDetail> cb, String reason) {
        if (item.detailUrl == null || item.detailUrl.isEmpty() || !isHttp(item.detailUrl)) {
            cb.onError("Plugin chi tiết thất bại: " + reason);
            return;
        }
        fetchDetailUrl(item.detailUrl, item, cb);
    }

    private void fetchDetailUrl(String url, MovieItem item, Callback<MovieDetail> cb) {
        io.execute(() -> {
            try {
                String body = MovieHttp.get(url, Collections.emptyMap());
                js.call("parseMovieDetail", new MovieJsRuntime.Callback() {
                    @Override public void done(String parsed) {
                        MovieDetail d = parseDetail(parsed, item);
                        if (d.episodes.isEmpty()) {
                            cb.onError("Plugin không trả danh sách tập");
                        } else {
                            cb.onSuccess(d);
                        }
                    }

                    @Override public void error(String m) {
                        cb.onError("Plugin parse chi tiết: " + m);
                    }
                }, MovieJsRuntime.quote(body), MovieJsRuntime.quote(url));
            } catch (Exception e) {
                cb.onError("Plugin HTTP chi tiết: " + safe(e));
            }
        });
    }

    @Override public void loadPlayback(MovieEpisode episode, Callback<Playback> cb) {
        if (episode.directUrl != null && !episode.directUrl.isEmpty()) {
            cb.onSuccess(new Playback(
                    episode.directUrl,
                    mimeOf(episode.directUrl),
                    new LinkedHashMap<String, String>(),
                    Collections.emptyList()
            ));
            return;
        }

        final String rawId = episode.id == null ? "" : episode.id.trim();
        if (rawId.isEmpty()) {
            cb.onError("Plugin không có ID tập phim");
            return;
        }

        // Plugin architecture: episode id may be a slug or page URL.
        // Resolve it through getUrlDetail() before asking parseDetailResponse().
        js.call("getUrlDetail", new MovieJsRuntime.Callback() {
            @Override public void done(String resolved) {
                String url = cleanResult(resolved);
                if (isHttp(url)) {
                    resolvePlaybackFromUrl(url, 0, cb);
                } else if (isHttp(rawId)) {
                    resolvePlaybackFromUrl(rawId, 0, cb);
                } else {
                    cb.onError("Plugin không tạo được URL xem tập");
                }
            }

            @Override public void error(String m) {
                if (isHttp(rawId)) {
                    resolvePlaybackFromUrl(rawId, 0, cb);
                } else {
                    cb.onError("Plugin resolve tập: " + m);
                }
            }
        }, MovieJsRuntime.quote(MovieJson.stripPrefix(rawId)));
    }

    private void resolvePlaybackFromUrl(String url, int hop, Callback<Playback> cb) {
        io.execute(() -> {
            try {
                String body = MovieHttp.get(url, Collections.emptyMap());
                parsePlaybackResponse(body, url, hop, cb);
            } catch (Exception e) {
                cb.onError("Plugin HTTP luồng phát: " + safe(e));
            }
        });
    }

    private void parsePlaybackResponse(String body, String sourceUrl, int hop, Callback<Playback> cb) {
        js.call(hop == 0 ? "parseDetailResponse" : "parseEmbedResponse",
                new MovieJsRuntime.Callback() {
                    @Override public void done(String parsed) {
                        handlePlaybackResult(parsed, sourceUrl, hop, cb);
                    }

                    @Override public void error(String m) {
                        // Some older plugins only expose parseDetailResponse().
                        if (hop > 0) {
                            js.call("parseDetailResponse", new MovieJsRuntime.Callback() {
                                @Override public void done(String parsed2) {
                                    handlePlaybackResult(parsed2, sourceUrl, hop, cb);
                                }
                                @Override public void error(String ignored) {
                                    cb.onError("Plugin parse luồng phát: " + m);
                                }
                            }, MovieJsRuntime.quote(body), MovieJsRuntime.quote(sourceUrl));
                        } else {
                            cb.onError("Plugin parse luồng phát: " + m);
                        }
                    }
                }, MovieJsRuntime.quote(body), MovieJsRuntime.quote(sourceUrl));
    }

    private void handlePlaybackResult(String parsed, String sourceUrl, int hop, Callback<Playback> cb) {
        try {
            JSONObject o = new JSONObject(parsed == null ? "{}" : parsed);
            String url = MovieJson.first(o, "url", "streamUrl", "stream_url", "playUrl", "play_url");
            if (url == null || url.isEmpty()) {
                url = MovieJson.findPlayable(o);
            }

            boolean embed = o.optBoolean("isEmbed", false);
            String postBody = MovieJson.first(o, "postBody", "post_body", "body");
            String mime = MovieJson.first(o, "mimeType", "mime_type", "mime");
            Map<String, String> headers = readHeaders(o.optJSONObject("headers"));
            List<Subtitle> subs = readSubtitles(o.optJSONArray("subtitles"));

            if (embed && url != null && !url.isEmpty()) {
                if (hop >= MAX_EMBED_HOPS) {
                    cb.onError("Plugin embed vượt quá " + MAX_EMBED_HOPS + " cấp");
                    return;
                }
                resolveEmbedded(url, postBody, hop + 1, headers, cb);
                return;
            }

            if (url == null || url.isEmpty()) {
                if (isPlayable(sourceUrl)) {
                    cb.onSuccess(new Playback(sourceUrl, mimeOf(sourceUrl), headers, subs));
                    return;
                }
                cb.onError("Plugin không trả URL phát");
                return;
            }

            if (!isHttp(url)) {
                cb.onError("Plugin trả URL phát không hợp lệ");
                return;
            }

            cb.onSuccess(new Playback(
                    url,
                    mime == null || mime.isEmpty() ? mimeOf(url) : mime,
                    headers,
                    subs
            ));
        } catch (Exception e) {
            // If plugin returned a bare JSON string containing a stream, accept it.
            String direct = cleanResult(parsed);
            if (isPlayable(direct)) {
                cb.onSuccess(new Playback(
                        direct, mimeOf(direct),
                        new LinkedHashMap<String, String>(),
                        Collections.emptyList()
                ));
            } else {
                cb.onError("Plugin playback parse: " + safe(e));
            }
        }
    }

    private void resolveEmbedded(String url, String postBody, int nextHop,
                                 Map<String, String> inheritedHeaders,
                                 Callback<Playback> cb) {
        io.execute(() -> {
            try {
                Map<String, String> h = new LinkedHashMap<>();
                if (inheritedHeaders != null) h.putAll(inheritedHeaders);
                String body;
                if (postBody != null && !postBody.isEmpty()) {
                    body = MovieHttp.postText(url, h, postBody);
                } else {
                    body = MovieHttp.get(url, h);
                }
                final String sourceUrl = url;
                js.call("parseEmbedResponse", new MovieJsRuntime.Callback() {
                    @Override public void done(String parsed) {
                        handlePlaybackResult(mergeInheritedHeaders(parsed, h), sourceUrl, nextHop, cb);
                    }

                    @Override public void error(String m) {
                        js.call("parseDetailResponse", new MovieJsRuntime.Callback() {
                            @Override public void done(String parsed2) {
                                handlePlaybackResult(mergeInheritedHeaders(parsed2, h), sourceUrl, nextHop, cb);
                            }
                            @Override public void error(String ignored) {
                                cb.onError("Plugin embed parse: " + m);
                            }
                        }, MovieJsRuntime.quote(body), MovieJsRuntime.quote(sourceUrl));
                    }
                }, MovieJsRuntime.quote(body), MovieJsRuntime.quote(sourceUrl));
            } catch (Exception e) {
                cb.onError("Plugin embed HTTP: " + safe(e));
            }
        });
    }

    private String mergeInheritedHeaders(String parsed, Map<String, String> inherited) {
        if (parsed == null || parsed.isEmpty() || inherited == null || inherited.isEmpty()) return parsed;
        try {
            JSONObject o = new JSONObject(parsed);
            JSONObject h = o.optJSONObject("headers");
            if (h == null) {
                h = new JSONObject();
                o.put("headers", h);
            }
            for (Map.Entry<String, String> e : inherited.entrySet()) {
                if (!h.has(e.getKey())) h.put(e.getKey(), e.getValue());
            }
            return o.toString();
        } catch (Exception e) {
            return parsed;
        }
    }

    private List<MovieItem> parseItems(String parsed) {
        List<MovieItem> out = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(parsed == null ? "{}" : parsed);
            JSONArray a = root.optJSONArray("items");
            if (a == null) return out;

            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;

                String id = MovieJson.first(o, "id", "slug", "movieId", "showId");
                String title = MovieJson.first(o, "title", "name", "originalTitle", "original_title");
                if (id == null || title == null) continue;

                boolean show = id.startsWith("show/")
                        || "show".equalsIgnoreCase(MovieJson.first(o, "type"))
                        || "tv".equalsIgnoreCase(MovieJson.first(o, "type"))
                        || o.has("seasons")
                        || o.has("episodes");

                String cleanId = MovieJson.stripPrefix(id);
                String normalized = (id.startsWith("movie/") || id.startsWith("show/"))
                        ? id : (show ? "show/" : "movie/") + cleanId;

                String poster = MovieJson.resolveImage("", MovieJson.first(
                        o, "posterUrl", "poster", "poster_path", "posterPath", "image", "thumbnail", "thumbnailUrl"));
                String backdrop = MovieJson.resolveImage("", MovieJson.first(
                        o, "backdropUrl", "backdrop", "backdrop_path", "backdropPath", "cover", "coverUrl"));

                out.add(new MovieItem(
                        normalized,
                        title,
                        poster,
                        backdrop.isEmpty() ? poster : backdrop,
                        MovieJson.first(o, "quality", "resolution", "format"),
                        MovieJson.first(o, "year", "releaseDate", "release_date", "firstAirDate", "episode_current"),
                        sourceId,
                        show,
                        null
                ));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private MovieDetail parseDetail(String parsed, MovieItem seed) {
        try {
            JSONObject o = new JSONObject(parsed == null ? "{}" : parsed);
            String title = MovieJson.first(o, "title", "name", "originalTitle", "original_title");
            if (title == null) title = seed.title;

            String poster = MovieJson.resolveImage("", MovieJson.first(
                    o, "posterUrl", "poster", "poster_path", "posterPath", "image", "thumbnail"));
            String backdrop = MovieJson.resolveImage("", MovieJson.first(
                    o, "backdropUrl", "backdrop", "backdrop_path", "backdropPath", "cover", "coverUrl"));

            String id = MovieJson.first(o, "id", "slug") == null
                    ? seed.id
                    : MovieJson.first(o, "id", "slug");
            String normalizedId = (id.startsWith("movie/") || id.startsWith("show/"))
                    ? id : (seed.show ? "show/" : "movie/") + MovieJson.stripPrefix(id);

            MovieItem m = new MovieItem(
                    normalizedId,
                    title,
                    poster.isEmpty() ? seed.posterUrl : poster,
                    backdrop.isEmpty() ? seed.backdropUrl : backdrop,
                    MovieJson.first(o, "quality", "resolution", "format") == null
                            ? seed.quality : MovieJson.first(o, "quality", "resolution", "format"),
                    MovieJson.first(o, "year", "releaseDate", "release_date", "firstAirDate") == null
                            ? seed.year
                            : MovieJson.first(o, "year", "releaseDate", "release_date", "firstAirDate"),
                    seed.sourceId,
                    seed.show,
                    seed.detailUrl
            );

            List<MovieEpisode> eps = new ArrayList<>();
            JSONArray servers = o.optJSONArray("servers");
            if (servers != null) {
                for (int i = 0; i < servers.length(); i++) {
                    JSONObject server = servers.optJSONObject(i);
                    if (server == null) continue;
                    collectServerEpisodes(server, 0, eps);
                }
            }

            JSONArray directEpisodes = o.optJSONArray("episodes");
            if (directEpisodes != null) collectEpisodeArray(directEpisodes, 0, eps);

            if (eps.isEmpty()) {
                String direct = MovieJson.findPlayable(o);
                eps.add(new MovieEpisode(
                        normalizedId,
                        "▶ Xem phim",
                        "full",
                        0,
                        0,
                        direct
                ));
            }

            return new MovieDetail(
                    m,
                    MovieJson.first(o, "description", "overview", "plot", "summary", "content"),
                    MovieJson.first(o, "category", "genres", "genre") == null
                            ? "Phim" : MovieJson.first(o, "category", "genres", "genre"),
                    MovieJson.first(o, "casts", "cast"),
                    MovieJson.first(o, "director", "directors"),
                    MovieJson.first(o, "rating", "voteAverage", "vote_average", "imdbRating"),
                    eps
            );
        } catch (Exception e) {
            return new MovieDetail(
                    seed, "", "Phim", "", "", "",
                    Collections.singletonList(
                            new MovieEpisode(seed.id, "▶ Xem phim", "full", 0, 0, null)
                    )
            );
        }
    }

    private void collectServerEpisodes(JSONObject server, int inheritedSeason, List<MovieEpisode> out) {
        int serverSeason = integer(server, "seasonNumber", "season", "season_no");
        int season = serverSeason > 0 ? serverSeason : inheritedSeason;
        JSONArray episodes = server.optJSONArray("episodes");
        if (episodes != null) collectEpisodeArray(episodes, season, out);

        JSONArray nested = server.optJSONArray("seasons");
        if (nested != null) {
            for (int i = 0; i < nested.length(); i++) {
                JSONObject s = nested.optJSONObject(i);
                if (s != null) collectServerEpisodes(s, season, out);
            }
        }
    }

    private void collectEpisodeArray(JSONArray episodes, int inheritedSeason, List<MovieEpisode> out) {
        for (int j = 0; j < episodes.length(); j++) {
            JSONObject e = episodes.optJSONObject(j);
            if (e == null) continue;

            int season = integer(e, "seasonNumber", "season", "season_no");
            if (season == 0) season = inheritedSeason;

            int episode = integer(e, "episodeNumber", "episode", "ep", "episode_no");
            if (episode == 0) episode = j + 1;

            String id = MovieJson.first(e, "id", "url", "link", "slug", "episodeId", "episode_id");
            String slug = MovieJson.first(e, "slug", "id", "episodeId", "episode_id");
            String title = MovieJson.first(e, "name", "title", "episodeTitle");
            if (id == null || id.isEmpty()) continue;

            String label = title == null || title.isEmpty()
                    ? "Tập " + episode
                    : (title.startsWith("Tập ") ? title : "Tập " + episode + ": " + title);

            String direct = isPlayable(id) ? id : null;
            out.add(new MovieEpisode(
                    id,
                    label,
                    slug == null ? "s" + season + "-e" + episode : slug,
                    season,
                    episode,
                    direct
            ));
        }
    }

    private static Map<String, String> readHeaders(JSONObject o) {
        Map<String, String> out = new LinkedHashMap<>();
        if (o == null) return out;
        JSONArray names = o.names();
        if (names == null) return out;
        for (int i = 0; i < names.length(); i++) {
            String k = names.optString(i);
            String v = MovieJson.first(o, k);
            if (k != null && v != null) out.put(k, v);
        }
        return out;
    }

    private static List<Subtitle> readSubtitles(JSONArray a) {
        List<Subtitle> out = new ArrayList<>();
        if (a == null) return out;
        for (int i = 0; i < a.length(); i++) {
            JSONObject s = a.optJSONObject(i);
            if (s == null) continue;
            String url = MovieJson.first(s, "url", "src", "file");
            if (url == null || !isHttp(url)) continue;
            String lang = MovieJson.first(s, "lang", "language");
            String label = MovieJson.first(s, "label", "name");
            out.add(new Subtitle(url, lang == null ? "" : lang,
                    label == null ? "Subtitle" : label));
        }
        return out;
    }

    private static int integer(JSONObject o, String... keys) {
        String s = MovieJson.first(o, keys);
        try { return s == null ? 0 : Integer.parseInt(s); }
        catch (Exception e) { return 0; }
    }

    private static String cleanResult(String s) {
        if (s == null) return "";
        String x = s.trim();
        if (x.startsWith("\"") && x.endsWith("\"")) {
            x = x.substring(1, x.length() - 1);
        }
        return x;
    }

    private static boolean looksJsonObject(String s) {
        String x = s == null ? "" : s.trim();
        return x.startsWith("{") && x.endsWith("}");
    }

    private static boolean isHttp(String s) {
        return s != null && (s.startsWith("http://") || s.startsWith("https://"));
    }

    private static boolean isPlayable(String u) {
        if (!isHttp(u)) return false;
        String x = u.toLowerCase();
        return x.contains(".m3u8") || x.contains(".mpd") || x.contains(".mp4")
                || x.contains(".mkv") || x.contains("stream") || x.contains("playlist");
    }

    private static String mimeOf(String u) {
        String x = u == null ? "" : u.toLowerCase();
        if (x.contains(".mpd")) return "application/dash+xml";
        if (x.contains(".m3u8")) return "application/x-mpegURL";
        return "video/mp4";
    }

    private static String safe(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    @Override public void close() {
        io.shutdownNow();
        js.destroy();
    }
}