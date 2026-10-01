package vn.phuong.iptvplayer.movie;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService io = Executors.newFixedThreadPool(3);

    public MovieJsSource(Context context, String fileName, String script) {
        String stableName = fileName == null ? "movie-plugin.js" : fileName;
        this.sourceId = "plugin_" + Math.abs(stableName.hashCode());
        this.name = stableName.endsWith(".js")
                ? stableName.substring(0, stableName.length() - 3)
                : stableName;
        this.js = new MovieJsRuntime(context, script == null ? "" : script);
    }

    @Override public String id() { return sourceId; }
    @Override public String name() { return name; }
    @Override public boolean isPlugin() { return true; }

    @Override public void loadHome(Callback<List<MovieItem>> cb) {
        js.call("getUrlList", new MovieJsRuntime.Callback() {
            @Override public void done(String url) {
                fetchListAndParse(url, "parseListResponse", cb);
            }
            @Override public void error(String message) {
                cb.onError("Plugin không tạo được URL trang phim: " + message);
            }
        }, MovieJsRuntime.quote("trending"),
                MovieJsRuntime.quote("{\"page\":1}"));
    }

    @Override public void search(String query, Callback<List<MovieItem>> cb) {
        js.call("getUrlSearch", new MovieJsRuntime.Callback() {
            @Override public void done(String url) {
                fetchListAndParse(url, "parseSearchResponse", cb);
            }
            @Override public void error(String message) {
                cb.onError("Plugin không tạo được URL tìm kiếm: " + message);
            }
        }, MovieJsRuntime.quote(query == null ? "" : query),
                MovieJsRuntime.quote("{\"page\":1}"));
    }

    @Override public void loadDetail(MovieItem item, Callback<MovieDetail> cb) {
        final String id = item == null ? "" : item.id;
        if (id == null || id.trim().isEmpty()) {
            cb.onError("Plugin không có ID phim");
            return;
        }

        js.call("getUrlDetail", new MovieJsRuntime.Callback() {
            @Override public void done(String result) {
                String url = cleanResult(result);
                if (!isHttp(url)) {
                    if (item.detailUrl != null && isHttp(item.detailUrl)) {
                        fetchDetailUrl(item.detailUrl, item, cb);
                    } else {
                        cb.onError("Plugin trả về URL chi tiết không hợp lệ");
                    }
                    return;
                }
                fetchDetailUrl(url, item, cb);
            }

            @Override public void error(String message) {
                if (item.detailUrl != null && isHttp(item.detailUrl)) {
                    fetchDetailUrl(item.detailUrl, item, cb);
                } else {
                    cb.onError("Plugin chi tiết thất bại: " + message);
                }
            }
        }, MovieJsRuntime.quote(id));
    }

    @Override public void loadPlayback(MovieEpisode episode, Callback<Playback> cb) {
        if (episode == null || episode.id == null || episode.id.trim().isEmpty()) {
            cb.onError("Plugin không có ID tập phim");
            return;
        }

        final String rawId = episode.id.trim();
        if (isHttp(rawId)) {
            resolvePlaybackFromUrl(rawId, 0, cb);
            return;
        }

        resolveEpisodeUrl(rawId, new MovieJsRuntime.Callback() {
            @Override public void done(String resolved) {
                String url = cleanResult(resolved);
                if (isHttp(url)) {
                    resolvePlaybackFromUrl(url, 0, cb);
                } else {
                    cb.onError("Plugin không tạo được URL nguồn phát");
                }
            }

            @Override public void error(String message) {
                cb.onError("Plugin resolve tập: " + message);
            }
        });
    }

    private void fetchListAndParse(String url, String parser,
                                   Callback<List<MovieItem>> cb) {
        final String requestUrl = cleanResult(url);
        if (!isHttp(requestUrl)) {
            cb.onError("Plugin trả về URL danh sách không hợp lệ");
            return;
        }

        io.execute(() -> {
            try {
                String body = MovieHttp.get(requestUrl, pluginHeaders(requestUrl));
                js.call(parser, new MovieJsRuntime.Callback() {
                    @Override public void done(String parsed) {
                        List<MovieItem> items = parseItems(parsed, requestUrl);
                        if (items.isEmpty()) {
                            items = MovieJson.parseList(normalizeJsonBody(body),
                                    baseOf(requestUrl), sourceId);
                        }
                        cb.onSuccess(items);
                    }

                    @Override public void error(String message) {
                        List<MovieItem> items = MovieJson.parseList(normalizeJsonBody(body),
                                baseOf(requestUrl), sourceId);
                        if (!items.isEmpty()) {
                            cb.onSuccess(items);
                        } else {
                            cb.onError("Plugin parse danh sách: " + message);
                        }
                    }
                }, MovieJsRuntime.quote(normalizeJsonBody(body)),
                        MovieJsRuntime.quote(requestUrl));
            } catch (Exception e) {
                cb.onError("Plugin HTTP danh sách: " + safe(e));
            }
        });
    }

    private void fetchDetailUrl(String url, MovieItem item,
                                Callback<MovieDetail> cb) {
        io.execute(() -> {
            try {
                String body = MovieHttp.get(url, pluginHeaders(url));
                final String normalizedBody = normalizeJsonBody(body);

                js.call("parseMovieDetail", new MovieJsRuntime.Callback() {
                    @Override public void done(String parsed) {
                        MovieDetail detail = parseDetail(parsed, item);
                        if (hasUsableEpisodes(detail)) {
                            cb.onSuccess(detail);
                            return;
                        }

                        MovieDetail fallback = MovieJson.parseDetail(
                                normalizedBody, baseOf(url), item);
                        if (hasUsableEpisodes(fallback)) {
                            cb.onSuccess(fallback);
                        } else {
                            cb.onError("Plugin không trả được danh sách tập phim");
                        }
                    }

                    @Override public void error(String message) {
                        MovieDetail fallback = MovieJson.parseDetail(
                                normalizedBody, baseOf(url), item);
                        if (hasUsableEpisodes(fallback)) {
                            cb.onSuccess(fallback);
                        } else {
                            cb.onError("Plugin parse chi tiết: " + message);
                        }
                    }
                }, MovieJsRuntime.quote(normalizedBody),
                        MovieJsRuntime.quote(url));
            } catch (Exception e) {
                cb.onError("Plugin HTTP chi tiết: " + safe(e));
            }
        });
    }

    private void resolveEpisodeUrl(String rawId, MovieJsRuntime.Callback cb) {
        String[] functions = {
                "getUrlEpisode",
                "getUrlSource",
                "getUrlPlay",
                "getUrlDetail"
        };
        resolveEpisodeUrlAt(rawId, functions, 0, cb);
    }

    private void resolveEpisodeUrlAt(String rawId, String[] functions, int index,
                                     MovieJsRuntime.Callback cb) {
        if (index >= functions.length) {
            cb.error("plugin không có hàm resolve tập");
            return;
        }

        js.call(functions[index], new MovieJsRuntime.Callback() {
            @Override public void done(String resolved) {
                String url = cleanResult(resolved);
                if (isHttp(url)) {
                    cb.done(url);
                } else {
                    resolveEpisodeUrlAt(rawId, functions, index + 1, cb);
                }
            }

            @Override public void error(String ignored) {
                resolveEpisodeUrlAt(rawId, functions, index + 1, cb);
            }
        }, MovieJsRuntime.quote(rawId));
    }

    private void resolvePlaybackFromUrl(String url, int hop,
                                        Callback<Playback> cb) {
        io.execute(() -> {
            try {
                String body = MovieHttp.get(url, pluginHeaders(url));
                parsePlaybackResponse(body, url, hop, cb);
            } catch (Exception e) {
                cb.onError("Plugin HTTP luồng phát: " + safe(e));
            }
        });
    }

    private void parsePlaybackResponse(String body, String sourceUrl, int hop,
                                       Callback<Playback> cb) {
        final String normalizedBody = normalizeJsonBody(body);
        js.call(hop == 0 ? "parseDetailResponse" : "parseEmbedResponse",
                new MovieJsRuntime.Callback() {
                    @Override public void done(String parsed) {
                        if (!hasPlayableResult(parsed, sourceUrl)) {
                            fallbackPlayback(normalizedBody, sourceUrl, cb,
                                    "Plugin không trả URL phát");
                            return;
                        }
                        handlePlaybackResult(parsed, sourceUrl, hop, cb);
                    }

                    @Override public void error(String message) {
                        fallbackPlayback(normalizedBody, sourceUrl, cb,
                                "Plugin parse luồng phát: " + message);
                    }
                }, MovieJsRuntime.quote(normalizedBody),
                MovieJsRuntime.quote(sourceUrl));
    }

    private void fallbackPlayback(String body, String sourceUrl,
                                   Callback<Playback> cb, String reason) {
        Playback playback = MovieJson.parsePlayback(body, sourceUrl, baseOf(sourceUrl));
        if (playback != null && isHttp(playback.url)) {
            cb.onSuccess(sanitizePlayback(playback, sourceUrl));
        } else {
            cb.onError(reason);
        }
    }

    private void handlePlaybackResult(String parsed, String sourceUrl, int hop,
                                      Callback<Playback> cb) {
        try {
            JSONObject o = new JSONObject(parsed == null ? "{}" : parsed);
            String url = MovieJson.first(o, "url", "streamUrl", "stream_url",
                    "playUrl", "play_url");
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
                    cb.onSuccess(new Playback(stripFragment(sourceUrl),
                            mimeOf(sourceUrl), headers, subs));
                    return;
                }
                cb.onError("Plugin không trả URL phát");
                return;
            }

            if (!isHttp(url)) {
                cb.onError("Plugin trả URL phát không hợp lệ");
                return;
            }

            Playback playback = new Playback(
                    stripFragment(url),
                    mime == null || mime.isEmpty() ? mimeOf(url) : mime,
                    headers,
                    subs
            );
            if (!isPlayablePlayback(playback)) {
                fallbackPlayback(normalizeJsonBody(parsed), sourceUrl, cb,
                        "Plugin trả dữ liệu nguồn nhưng chưa có URL stream cuối");
                return;
            }
            cb.onSuccess(sanitizePlayback(playback, sourceUrl));
        } catch (Exception e) {
            String direct = cleanResult(parsed);
            if (isPlayable(direct)) {
                cb.onSuccess(new Playback(stripFragment(direct), mimeOf(direct),
                        pluginHeaders(sourceUrl), Collections.emptyList()));
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

                String body = (postBody != null && !postBody.isEmpty())
                        ? MovieHttp.postText(url, h, postBody)
                        : MovieHttp.get(url, h);

                final String sourceUrl = url;
                parseEmbeddedBody(normalizeJsonBody(body), sourceUrl, nextHop, h, cb);
            } catch (Exception e) {
                cb.onError("Plugin embed HTTP: " + safe(e));
            }
        });
    }

    private void parseEmbeddedBody(String body, String sourceUrl, int hop,
                                   Map<String, String> inheritedHeaders,
                                   Callback<Playback> cb) {
        js.call("parseEmbedResponse", new MovieJsRuntime.Callback() {
            @Override public void done(String parsed) {
                handlePlaybackResult(
                        mergeInheritedHeaders(parsed, inheritedHeaders),
                        sourceUrl, hop, cb);
            }

            @Override public void error(String message) {
                js.call("parseDetailResponse", new MovieJsRuntime.Callback() {
                    @Override public void done(String parsed2) {
                        handlePlaybackResult(
                                mergeInheritedHeaders(parsed2, inheritedHeaders),
                                sourceUrl, hop, cb);
                    }

                    @Override public void error(String ignored) {
                        fallbackPlayback(body, sourceUrl, cb,
                                "Plugin embed parse: " + message);
                    }
                }, MovieJsRuntime.quote(body),
                        MovieJsRuntime.quote(sourceUrl));
            }
        }, MovieJsRuntime.quote(body), MovieJsRuntime.quote(sourceUrl));
    }

    private List<MovieItem> parseItems(String parsed, String requestUrl) {
        List<MovieItem> out = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(parsed == null ? "{}" : parsed);
            JSONArray a = root.optJSONArray("items");

            if (a != null) {
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.optJSONObject(i);
                    if (o == null) continue;

                    String id = MovieJson.first(o, "id", "slug", "movieId", "showId");
                    String title = MovieJson.first(o, "title", "name",
                            "originalTitle", "original_title");
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
                            o, "posterUrl", "poster", "poster_path",
                            "posterPath", "image", "thumbnail", "thumbnailUrl"));
                    String backdrop = MovieJson.resolveImage("", MovieJson.first(
                            o, "backdropUrl", "backdrop", "backdrop_path",
                            "backdropPath", "cover", "coverUrl"));

                    String detailUrl = normalized.startsWith("show/")
                            ? baseOf(requestUrl) + "/api/shows/" + cleanId
                            : baseOf(requestUrl) + "/api/movies/" + cleanId;

                    out.add(new MovieItem(
                            normalized,
                            title,
                            poster,
                            backdrop.isEmpty() ? poster : backdrop,
                            MovieJson.first(o, "quality", "resolution", "format"),
                            MovieJson.first(o, "year", "releaseDate",
                                    "release_date", "firstAirDate", "episode_current"),
                            sourceId,
                            show,
                            detailUrl
                    ));
                }
            }
        } catch (Exception ignored) {
        }

        if (out.isEmpty()) {
            out.addAll(MovieJson.parseList(normalizeJsonBody(parsed),
                    baseOf(requestUrl), sourceId));
        }
        return out;
    }

    private MovieDetail parseDetail(String parsed, MovieItem seed) {
        try {
            JSONObject o = new JSONObject(parsed == null ? "{}" : parsed);
            String title = MovieJson.first(o, "title", "name",
                    "originalTitle", "original_title");
            if (title == null) title = seed.title;

            String poster = MovieJson.resolveImage("", MovieJson.first(
                    o, "posterUrl", "poster", "poster_path",
                    "posterPath", "image", "thumbnail"));
            String backdrop = MovieJson.resolveImage("", MovieJson.first(
                    o, "backdropUrl", "backdrop", "backdrop_path",
                    "backdropPath", "cover", "coverUrl"));

            String id = MovieJson.first(o, "id", "slug") == null
                    ? seed.id : MovieJson.first(o, "id", "slug");
            String normalizedId = (id.startsWith("movie/") || id.startsWith("show/"))
                    ? id : (seed.show ? "show/" : "movie/") + MovieJson.stripPrefix(id);

            MovieItem movie = new MovieItem(
                    normalizedId,
                    title,
                    poster.isEmpty() ? seed.posterUrl : poster,
                    backdrop.isEmpty() ? seed.backdropUrl : backdrop,
                    MovieJson.first(o, "quality", "resolution", "format") == null
                            ? seed.quality
                            : MovieJson.first(o, "quality", "resolution", "format"),
                    MovieJson.first(o, "year", "releaseDate",
                            "release_date", "firstAirDate") == null
                            ? seed.year
                            : MovieJson.first(o, "year", "releaseDate",
                            "release_date", "firstAirDate"),
                    seed.sourceId,
                    seed.show,
                    seed.detailUrl
            );

            List<MovieEpisode> eps = new ArrayList<>();
            JSONArray servers = o.optJSONArray("servers");
            if (servers != null) {
                for (int i = 0; i < servers.length(); i++) {
                    JSONObject server = servers.optJSONObject(i);
                    if (server != null) collectServerEpisodes(server, 0, eps);
                }
            }

            JSONArray directEpisodes = o.optJSONArray("episodes");
            if (directEpisodes != null) collectEpisodeArray(directEpisodes, 0, eps);

            if (eps.isEmpty()) {
                String direct = MovieJson.findPlayable(o);
                if (direct != null) {
                    eps.add(new MovieEpisode(
                            direct, "▶ Xem phim", "full", 0, 0, direct));
                }
            }

            return new MovieDetail(
                    movie,
                    MovieJson.first(o, "description", "overview", "plot",
                            "summary", "content"),
                    MovieJson.first(o, "category", "genres", "genre") == null
                            ? "Phim" : MovieJson.first(o, "category", "genres", "genre"),
                    MovieJson.first(o, "casts", "cast"),
                    MovieJson.first(o, "director", "directors"),
                    MovieJson.first(o, "rating", "voteAverage",
                            "vote_average", "imdbRating"),
                    eps
            );
        } catch (Exception e) {
            return new MovieDetail(seed, "", "Phim", "", "", "",
                    Collections.<MovieEpisode>emptyList());
        }
    }

    private void collectServerEpisodes(JSONObject server, int inheritedSeason,
                                       List<MovieEpisode> out) {
        int serverSeason = integer(server, "seasonNumber", "season", "season_no");
        int season = serverSeason > 0 ? serverSeason : inheritedSeason;

        JSONArray episodes = server.optJSONArray("episodes");
        if (episodes != null) collectEpisodeArray(episodes, season, out);

        JSONArray nested = server.optJSONArray("seasons");
        if (nested != null) {
            for (int i = 0; i < nested.length(); i++) {
                JSONObject child = nested.optJSONObject(i);
                if (child != null) collectServerEpisodes(child, season, out);
            }
        }
    }

    private void collectEpisodeArray(JSONArray episodes, int inheritedSeason,
                                     List<MovieEpisode> out) {
        for (int j = 0; j < episodes.length(); j++) {
            JSONObject e = episodes.optJSONObject(j);
            if (e == null) continue;

            int season = integer(e, "seasonNumber", "season", "season_no");
            if (season == 0) season = inheritedSeason;

            int episode = integer(e, "episodeNumber", "episode",
                    "ep", "episode_no");
            if (episode == 0) episode = j + 1;

            String id = MovieJson.first(e, "id", "url", "link", "slug",
                    "episodeId", "episode_id");
            String slug = MovieJson.first(e, "slug", "id", "episodeId", "episode_id");
            String title = MovieJson.first(e, "name", "title", "episodeTitle");
            if (id == null || id.isEmpty()) continue;

            String label = (title == null || title.isEmpty())
                    ? "Tập " + episode
                    : (title.startsWith("Tập ")
                    ? title
                    : "Tập " + episode + ": " + title);

            String direct = isPlayable(id) ? stripFragment(id) : null;
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
            String key = names.optString(i);
            if ("custom-js".equalsIgnoreCase(key)) continue;
            String value = MovieJson.first(o, key);
            if (key != null && value != null) out.put(key, value);
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
            if (!isHttp(url)) continue;

            String lang = MovieJson.first(s, "lang", "language");
            String label = MovieJson.first(s, "label", "name");
            out.add(new Subtitle(url,
                    lang == null ? "" : lang,
                    label == null ? "Subtitle" : label));
        }
        return out;
    }

    private Map<String, String> pluginHeaders(String url) {
        if (url != null && url.contains("novahd.cc")) {
            return MovieHttp.novaHeaders(url);
        }
        return Collections.emptyMap();
    }

    private Playback sanitizePlayback(Playback playback, String sourceUrl) {
        Map<String, String> headers = new LinkedHashMap<>();
        Map<String, String> defaults = pluginHeaders(sourceUrl);
        if (defaults != null) headers.putAll(defaults);
        if (playback.headers != null) {
            for (Map.Entry<String, String> e : playback.headers.entrySet()) {
                if ("custom-js".equalsIgnoreCase(e.getKey())) continue;
                headers.put(e.getKey(), e.getValue());
            }
        }
        return new Playback(stripFragment(playback.url),
                playback.mime, headers, playback.subtitles);
    }

    private String mergeInheritedHeaders(String parsed,
                                         Map<String, String> inherited) {
        if (parsed == null || parsed.isEmpty()
                || inherited == null || inherited.isEmpty()) {
            return parsed;
        }

        try {
            JSONObject o = new JSONObject(parsed);
            JSONObject headers = o.optJSONObject("headers");
            if (headers == null) {
                headers = new JSONObject();
                o.put("headers", headers);
            }

            for (Map.Entry<String, String> e : inherited.entrySet()) {
                if (!headers.has(e.getKey())) {
                    headers.put(e.getKey(), e.getValue());
                }
            }
            return o.toString();
        } catch (Exception e) {
            return parsed;
        }
    }

    private boolean hasUsableEpisodes(MovieDetail d) {
        if (d == null || d.episodes == null || d.episodes.isEmpty()) return false;
        for (MovieEpisode ep : d.episodes) {
            if (ep != null && ep.id != null && !ep.id.trim().isEmpty()) return true;
        }
        return false;
    }

    private boolean hasPlayableResult(String parsed, String sourceUrl) {
        if (isPlayable(sourceUrl)) return true;
        if (parsed == null || parsed.trim().isEmpty()) return false;
        try {
            JSONObject o = new JSONObject(parsed);
            if (o.optBoolean("isEmbed", false)) return true;
            String u = MovieJson.first(o, "url", "streamUrl", "stream_url",
                    "playUrl", "play_url");
            if (isPlayable(u)) return true;
            return MovieJson.findPlayable(o) != null;
        } catch (Exception ignored) {
            return isPlayable(cleanResult(parsed));
        }
    }

    private boolean isPlayablePlayback(Playback p) {
        return p != null && isHttp(p.url)
                && (isPlayable(p.url)
                || (p.mime != null && p.mime.toLowerCase().startsWith("video/"))
                || (p.mime != null && p.mime.toLowerCase().contains("mpegurl"))
                || (p.mime != null && p.mime.toLowerCase().contains("dash")));
    }

    private String normalizeJsonBody(String body) {
        if (body == null) return "";
        String x = body.trim();
        if (x.isEmpty()) return x;
        if ((x.startsWith("{") && x.endsWith("}"))
                || (x.startsWith("[") && x.endsWith("]"))) {
            return x;
        }

        String[] lines = x.split("\\r?\\n");
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("{") || t.startsWith("[")) {
                if (out.length() > 0) out.append('\n');
                out.append(t);
            }
        }
        return out.length() == 0 ? x : out.toString();
    }

    private static String cleanResult(String value) {
        if (value == null) return "";
        String x = value.trim();
        if (x.length() >= 2 && x.startsWith("\"") && x.endsWith("\"")) {
            try {
                Object parsed = new org.json.JSONTokener(x).nextValue();
                if (parsed instanceof String) return ((String) parsed).trim();
            } catch (Exception ignored) {
                x = x.substring(1, x.length() - 1);
            }
        }
        return x;
    }

    private static String stripFragment(String value) {
        if (value == null) return "";
        int p = value.indexOf('#');
        return p >= 0 ? value.substring(0, p) : value;
    }

    private static String baseOf(String url) {
        if (!isHttp(url)) return "https://novahd.cc";
        try {
            java.net.URI u = new java.net.URI(url);
            return u.getScheme() + "://" + u.getHost();
        } catch (Exception e) {
            return "https://novahd.cc";
        }
    }

    private static boolean isHttp(String value) {
        return value != null
                && (value.startsWith("http://") || value.startsWith("https://"));
    }

    private static boolean isPlayable(String u) {
        if (!isHttp(u)) return false;
        String x = u.toLowerCase();
        return x.contains(".m3u8") || x.contains(".mpd") || x.contains(".mp4")
                || x.contains(".mkv") || x.contains("stream")
                || x.contains("playlist");
    }

    private static String mimeOf(String u) {
        String x = u == null ? "" : u.toLowerCase();
        if (x.contains(".mpd")) return "application/dash+xml";
        if (x.contains(".m3u8")) return "application/x-mpegURL";
        if (x.contains(".mp4")) return "video/mp4";
        return "video/mp4";
    }

    private static int integer(JSONObject o, String... keys) {
        String s = MovieJson.first(o, keys);
        try {
            return s == null ? 0 : Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String safe(Exception e) {
        return e.getMessage() == null
                ? e.getClass().getSimpleName()
                : e.getMessage();
    }

    @Override public void close() {
        io.shutdownNow();
        js.destroy();
    }
}
