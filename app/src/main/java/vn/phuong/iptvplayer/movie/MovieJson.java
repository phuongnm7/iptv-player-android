package vn.phuong.iptvplayer.movie;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import vn.phuong.iptvplayer.movie.MovieModels.MovieDetail;
import vn.phuong.iptvplayer.movie.MovieModels.MovieEpisode;
import vn.phuong.iptvplayer.movie.MovieModels.MovieItem;
import vn.phuong.iptvplayer.movie.MovieModels.Playback;
import vn.phuong.iptvplayer.movie.MovieModels.Subtitle;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MovieJson {
    private MovieJson() {}

    public static List<MovieItem> parseList(String json, String base, String sourceId) {
        List<MovieItem> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try {
            Object root = new JSONTokener(json == null ? "" : json).nextValue();
            collectMovies(root, base, sourceId, out, seen);
        } catch (Exception ignored) {}
        return out;
    }

    private static void collectMovies(Object node, String base, String sourceId,
                                      List<MovieItem> out, Set<String> seen) {
        if (node instanceof JSONObject) {
            JSONObject o = (JSONObject) node;
            String title = first(o, "title", "name", "originalTitle", "original_title");
            String id = first(o, "slug", "id", "tmdbId", "tmdb_id", "movieId", "showId");
            String poster = first(o, "posterUrl", "poster", "poster_path", "posterPath",
                    "image", "imageUrl", "thumbnail", "thumbnailUrl");
            String backdrop = first(o, "backdropUrl", "backdrop", "backdrop_path",
                    "backdropPath", "cover", "coverUrl");
            if (title != null && id != null) {
                String cleanId = stripPrefix(id);
                String key = sourceId + ":" + cleanId;
                if (seen.add(key) && out.size() < 80) {
                    boolean show = isShow(o);
                    String normalizedId = id.startsWith("movie/") || id.startsWith("show/")
                            ? id : (show ? "show/" : "movie/") + id;
                    String detail = firstDetailUrl(o);
                    if (detail == null) {
                        detail = base + "/api/title/" + enc(cleanId);
                    }
                    out.add(new MovieItem(
                            normalizedId,
                            title,
                            resolveImage(base, poster),
                            resolveImage(base, backdrop == null ? poster : backdrop),
                            first(o, "quality", "resolution", "format"),
                            year(o),
                            sourceId,
                            show,
                            detail
                    ));
                }
            }
            JSONArray names = o.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    collectMovies(o.opt(names.optString(i)), base, sourceId, out, seen);
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray a = (JSONArray) node;
            for (int i = 0; i < a.length() && out.size() < 80; i++) {
                collectMovies(a.opt(i), base, sourceId, out, seen);
            }
        }
    }

    public static MovieDetail parseDetail(String json, String base, MovieItem seed) {
        try {
            JSONObject o = new JSONObject(json);
            String title = first(o, "title", "name", "originalTitle", "original_title");
            if (title == null) title = seed.title;

            String poster = first(o, "posterUrl", "poster", "poster_path", "posterPath",
                    "image", "thumbnail");
            String backdrop = first(o, "backdropUrl", "backdrop", "backdrop_path",
                    "backdropPath", "cover", "coverUrl");

            String responseId = first(o, "slug", "id", "tmdbId", "tmdb_id");
            String movieId = responseId == null ? seed.id : (
                    responseId.startsWith("movie/") || responseId.startsWith("show/")
                            ? responseId
                            : (seed.show ? "show/" : "movie/") + responseId
            );

            MovieItem movie = new MovieItem(
                    movieId,
                    title,
                    resolveImage(base, poster == null ? seed.posterUrl : poster),
                    resolveImage(base, backdrop == null ? seed.backdropUrl : backdrop),
                    first(o, "quality", "resolution", "format") == null
                            ? seed.quality
                            : first(o, "quality", "resolution", "format"),
                    year(o).isEmpty() ? seed.year : year(o),
                    seed.sourceId,
                    isShow(o) || seed.show,
                    seed.detailUrl
            );

            List<MovieEpisode> eps = new ArrayList<>();
            collectEpisodes(o, 0, eps, base, stripPrefix(seed.id));
            if (eps.isEmpty()) {
                String direct = findPlayable(o);
                if (direct != null) {
                    eps.add(new MovieEpisode(direct, "▶ Xem phim", "full", 0, 0, direct));
                } else if (base != null && base.contains("novahd.cc")) {
                    String tmdbId = stripPrefix(movie.id);
                    eps.add(new MovieEpisode(
                            base + "/api/sources?type=movie&tmdbId=" + enc(tmdbId),
                            "▶ Xem phim", "full", 0, 0, null));
                }
            }

            return new MovieDetail(
                    movie,
                    first(o, "overview", "description", "plot", "summary", "content"),
                    simpleText(o.has("genres") ? o.opt("genres") : o.opt("genre"), "Phim"),
                    simpleText(o.has("cast") ? o.opt("cast") : o.opt("casts"), ""),
                    first(o, "director", "directors"),
                    first(o, "rating", "voteAverage", "vote_average", "imdbRating"),
                    eps
            );
        } catch (Exception e) {
            return new MovieDetail(
                    seed,
                    "Không lấy được thông tin chi tiết phim.",
                    "Phim", "", "", "",
                    Collections.singletonList(
                            new MovieEpisode(seed.id, "▶ Xem phim", "full", 0, 0, null)
                    )
            );
        }
    }

    private static void collectEpisodes(Object node, int season, List<MovieEpisode> out,
                                        String base, String slug) {
        if (node instanceof JSONObject) {
            JSONObject o = (JSONObject) node;
            int s = integer(o, "seasonNumber", "season", "season_no");
            int e = integer(o, "episodeNumber", "episode", "ep", "episode_no");
            String name = first(o, "episodeTitle", "title", "name");

            if (e > 0 || (name != null && (o.has("episodeNumber") || o.has("episode")))) {
                if (s == 0) s = season;

                String id = firstHttp(o, "streamUrl", "stream_url", "playUrl", "play_url", "url");
                if (id == null) {
                    String episodeId = first(o, "episodeId", "episode_id", "watchId", "watch_id", "slug");
                    if (episodeId != null && (episodeId.contains("/") || episodeId.contains("-"))) {
                        id = episodeId;
                    }
                }
                if (id == null && slug != null && !slug.isEmpty()) {
                    if (base != null && base.contains("novahd.cc")) {
                        String type = seedIsShow(base, slug) ? "show" : "movie";
                        if ("show".equals(type)) {
                            id = base + "/api/sources?type=show&tmdbId=" + enc(slug)
                                    + "&season=" + (s > 0 ? s : 1)
                                    + "&episode=" + e;
                        } else {
                            id = base + "/api/sources?type=movie&tmdbId=" + enc(slug);
                        }
                    } else {
                        id = base + "/api/watch/" + enc(slug) + "/" + e + "?fromStart=false";
                    }
                }

                String label;
                if (name == null || name.isEmpty()) {
                    label = "Tập " + e;
                } else {
                    String n = name.startsWith("Tập ") ? name.substring(5) : name;
                    label = "Tập " + e + ": " + n;
                }

                out.add(new MovieEpisode(
                        id == null ? slug : id,
                        label,
                        "s" + s + "-e" + e,
                        s,
                        e,
                        isHttp(id) && looksPlayable(id) ? id : null
                ));
            }

            int currentSeason = s > 0 ? s : season;
            JSONArray names = o.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    collectEpisodes(o.opt(names.optString(i)), currentSeason, out, base, slug);
                }
            }
        } else if (node instanceof JSONArray) {
            JSONArray a = (JSONArray) node;
            for (int i = 0; i < a.length(); i++) {
                collectEpisodes(a.opt(i), season, out, base, slug);
            }
        }
    }

    public static Playback parsePlayback(String json, String requestedUrl, String base) {
        try {
            Object root = new JSONTokener(json == null || json.isEmpty() ? "{}" : json).nextValue();
            String url = null;

            if (root instanceof JSONObject) {
                JSONObject obj = (JSONObject) root;
                JSONArray sources = obj.optJSONArray("sources");
                if (sources != null) {
                    for (int i = 0; i < sources.length(); i++) {
                        JSONObject src = sources.optJSONObject(i);
                        if (src == null) continue;
                        String u = findPlayable(src);
                        if (u == null) continue;
                        if (url == null) url = u;
                        String q = first(src, "quality", "resolution", "format");
                        if (isHighQuality(q)) {
                            url = u;
                            break;
                        }
                    }
                }
            }

            if (url == null) url = findPlayable(root);
            if (url == null && isHttp(requestedUrl) && looksPlayable(requestedUrl)) {
                url = requestedUrl;
            }
            if (url == null || url.isEmpty()) return null;

            Map<String, String> headers = collectHeaders(root);
            List<Subtitle> subs = collectSubs(root, base);
            String lower = url.toLowerCase();
            String mime = lower.contains(".mpd")
                    ? "application/dash+xml"
                    : (lower.contains(".m3u8") ? "application/x-mpegURL" : "video/mp4");
            return new Playback(url, mime, headers, subs);
        } catch (Exception e) {
            return null;
        }
    }

    public static String findTicket(Object root) {
        return findByKey(root, "ticket", "playTicket", "play_ticket", "token", "playToken");
    }

    public static String findPlayable(Object root) {
        if (root instanceof String) {
            String s = (String) root;
            return isHttp(s) && looksPlayable(s) ? s : null;
        }
        if (root instanceof JSONObject) {
            JSONObject o = (JSONObject) root;
            String[] keys = {
                    "url", "streamUrl", "stream_url", "playUrl", "play_url",
                    "m3u8", "mpd", "file", "source", "src", "videoUrl", "video_url"
            };
            for (String k : keys) {
                String v = first(o, k);
                if (isHttp(v) && looksPlayable(v)) return v;
            }
            JSONArray names = o.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String v = findPlayable(o.opt(names.optString(i)));
                    if (v != null) return v;
                }
            }
        } else if (root instanceof JSONArray) {
            JSONArray a = (JSONArray) root;
            for (int i = 0; i < a.length(); i++) {
                String v = findPlayable(a.opt(i));
                if (v != null) return v;
            }
        }
        return null;
    }

    private static Map<String, String> collectHeaders(Object root) {
        Map<String, String> m = new LinkedHashMap<>();
        if (root instanceof JSONObject) {
            Object h = ((JSONObject) root).opt("headers");
            if (h instanceof JSONObject) {
                JSONObject ho = (JSONObject) h;
                JSONArray ns = ho.names();
                if (ns != null) {
                    for (int i = 0; i < ns.length(); i++) {
                        String k = ns.optString(i);
                        String v = first(ho, k);
                        if (v != null) m.put(k, v);
                    }
                }
            }
        }
        return m;
    }

    private static List<Subtitle> collectSubs(Object root, String base) {
        List<Subtitle> out = new ArrayList<>();
        if (root instanceof JSONObject) {
            Object x = ((JSONObject) root).opt("subtitles");
            if (x instanceof JSONArray) {
                JSONArray a = (JSONArray) x;
                for (int i = 0; i < a.length(); i++) {
                    JSONObject s = a.optJSONObject(i);
                    if (s == null) continue;
                    String u = firstHttp(s, "url", "src", "file");
                    if (u != null) {
                        String lang = first(s, "lang", "language");
                        String label = first(s, "label", "name");
                        out.add(new Subtitle(resolveImage(base, u),
                                lang == null ? "" : lang,
                                label == null ? "Subtitle" : label));
                    }
                }
            }
        }
        return out;
    }

    private static String findByKey(Object root, String... wanted) {
        if (root instanceof JSONObject) {
            JSONObject o = (JSONObject) root;
            for (String k : wanted) {
                String v = first(o, k);
                if (v != null && !v.isEmpty()) return v;
            }
            JSONArray ns = o.names();
            if (ns != null) {
                for (int i = 0; i < ns.length(); i++) {
                    String v = findByKey(o.opt(ns.optString(i)), wanted);
                    if (v != null) return v;
                }
            }
        } else if (root instanceof JSONArray) {
            JSONArray a = (JSONArray) root;
            for (int i = 0; i < a.length(); i++) {
                String v = findByKey(a.opt(i), wanted);
                if (v != null) return v;
            }
        }
        return null;
    }

    static String first(JSONObject o, String... keys) {
        for (String k : keys) {
            if (!o.has(k) || o.isNull(k)) continue;
            String value = valueText(o.opt(k));
            if (value != null && !value.isEmpty()) return value;
        }
        return null;
    }

    static String valueText(Object v) {
        if (v == null || v == JSONObject.NULL) return null;
        if (v instanceof String) {
            String s = ((String) v).trim();
            return s.isEmpty() ? null : s;
        }
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        if (v instanceof JSONObject) {
            JSONObject o = (JSONObject) v;
            String[] priority = {
                    "vi", "vn", "name", "title", "originalTitle", "original_title",
                    "en", "original", "url", "src", "file", "path", "value"
            };
            for (String k : priority) {
                if (o.has(k) && !o.isNull(k)) {
                    String s = valueText(o.opt(k));
                    if (s != null && !s.isEmpty()) return s;
                }
            }
            JSONArray names = o.names();
            if (names != null) {
                for (int i = 0; i < names.length(); i++) {
                    String s = valueText(o.opt(names.optString(i)));
                    if (s != null && !s.isEmpty()) return s;
                }
            }
            return null;
        }
        if (v instanceof JSONArray) {
            JSONArray a = (JSONArray) v;
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < a.length(); i++) {
                String s = valueText(a.opt(i));
                if (s == null || s.isEmpty()) continue;
                if (b.length() > 0) b.append(", ");
                b.append(s);
            }
            return b.length() == 0 ? null : b.toString();
        }
        return String.valueOf(v);
    }

    private static String firstHttp(JSONObject o, String... keys) {
        String s = first(o, keys);
        return isHttp(s) ? s : null;
    }

    private static String firstDetailUrl(JSONObject o) {
        String direct = firstHttp(o, "detailUrl", "detail_url", "detail", "href", "pageUrl", "page_url");
        if (direct != null) return direct;
        String generic = first(o, "url", "link");
        if (isHttp(generic) && !looksPlayable(generic)) return generic;
        return null;
    }

    private static String simpleText(Object v, String fallback) {
        String s = valueText(v);
        return s == null || s.isEmpty() ? fallback : s;
    }

    private static int integer(JSONObject o, String... keys) {
        String s = first(o, keys);
        try {
            return s == null ? 0 : Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String year(JSONObject o) {
        String s = first(o, "releaseDate", "release_date", "firstAirDate", "first_air_date", "year");
        if (s == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4})").matcher(s);
        return m.find() ? m.group(1) : "";
    }

    private static boolean isHighQuality(String q) {
        if (q == null) return false;
        String x = q.toLowerCase();
        return x.contains("2160") || x.contains("4k") || x.contains("1440") || x.contains("1080");
    }

    private static boolean isShow(JSONObject o) {
        String t = first(o, "type", "mediaType", "media_type");
        return (t != null && ("show".equalsIgnoreCase(t) || "tv".equalsIgnoreCase(t)
                || "series".equalsIgnoreCase(t)))
                || o.has("seasons") || o.has("episodes")
                || o.has("firstAirDate") || o.has("first_air_date");
    }

    private static boolean seedIsShow(String base, String slug) {
        return false;
    }

    private static boolean isHttp(String s) {
        return s != null && (s.startsWith("http://") || s.startsWith("https://"));
    }

    private static boolean looksPlayable(String s) {
        String x = s.toLowerCase();
        return x.contains(".m3u8") || x.contains(".mpd") || x.contains(".mp4")
                || x.contains("stream") || x.contains("play");
    }

    static String resolveImage(String base, String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.startsWith("{")) {
            try {
                String nested = valueText(new JSONObject(s));
                if (nested != null && !nested.equals(s)) s = nested;
            } catch (Exception ignored) {}
        }
        if (isHttp(s)) return s;
        if (s.startsWith("//")) return "https:" + s;
        if (s.startsWith("/")) return base + s;
        if (s.startsWith("data:")) return s;
        if (base == null || base.isEmpty()) return s;
        return base + "/" + s;
    }

    static String stripPrefix(String s) {
        if (s == null) return "";
        if (s.startsWith("movie/")) return s.substring(6);
        if (s.startsWith("show/")) return s.substring(5);
        return s;
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }
}