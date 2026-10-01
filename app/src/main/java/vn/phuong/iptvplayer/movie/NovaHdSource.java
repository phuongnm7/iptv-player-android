package vn.phuong.iptvplayer.movie;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.phuong.iptvplayer.movie.MovieModels.MovieDetail;
import vn.phuong.iptvplayer.movie.MovieModels.MovieEpisode;
import vn.phuong.iptvplayer.movie.MovieModels.MovieItem;
import vn.phuong.iptvplayer.movie.MovieModels.Playback;
import vn.phuong.iptvplayer.movie.MovieModels.Subtitle;

public final class NovaHdSource implements MovieSource {
    public static final String BASE = "https://novahd.cc";
    private static final String TMDB_POSTER = "https://image.tmdb.org/t/p/w500";
    private static final String TMDB_BACKDROP = "https://image.tmdb.org/t/p/w1280";
    private final ExecutorService io = Executors.newFixedThreadPool(2);
    private final String sourceId = "novahd";
    private final String visitorId = UUID.randomUUID().toString();

    @Override public String id() { return sourceId; }
    @Override public String name() { return "NovaHD"; }
    @Override public boolean isPlugin() { return false; }

    private Map<String,String> headers(String url) {
        return MovieHttp.novaHeaders(url, visitorId);
    }

    private String get(String url) throws Exception {
        return MovieHttp.get(url, headers(url));
    }

    @Override public void loadHome(Callback<List<MovieItem>> cb) {
        io.execute(() -> {
            Exception last = null;
            String[] urls = {
                    BASE + "/api/trending?type=all",
                    BASE + "/api/movies?page=1"
            };
            for (String url : urls) {
                try {
                    List<MovieItem> items = parseList(get(url));
                    if (!items.isEmpty()) {
                        cb.onSuccess(items);
                        return;
                    }
                    last = new Exception("API trả danh sách rỗng: " + url);
                } catch (Exception e) {
                    last = e;
                }
            }
            cb.onError("NovaHD: " + safe(last));
        });
    }

    @Override public void search(String query, Callback<List<MovieItem>> cb) {
        io.execute(() -> {
            try {
                String q = URLEncoder.encode(query == null ? "" : query, StandardCharsets.UTF_8.name());
                String url = BASE + "/api/search?search=" + q + "&page=1";
                List<MovieItem> items = parseList(get(url));
                cb.onSuccess(items);
            } catch (Exception e) {
                cb.onError("NovaHD tìm kiếm: " + safe(e));
            }
        });
    }

    @Override public void loadDetail(MovieItem item, Callback<MovieDetail> cb) {
        io.execute(() -> {
            try {
                String id = MovieJson.stripPrefix(item.id);
                String path = item.show ? "/api/shows/" : "/api/movies/";
                String json;
                try {
                    json = get(BASE + path + enc(id));
                } catch (Exception first) {
                    json = get(BASE + "/api/title/" + enc(id));
                }
                cb.onSuccess(parseDetail(json, item));
            } catch (Exception e) {
                cb.onError("NovaHD chi tiết: " + safe(e));
            }
        });
    }

    @Override public void loadPlayback(MovieEpisode episode, Callback<Playback> cb) {
        io.execute(() -> {
            try {
                if (episode.directUrl != null && !episode.directUrl.isEmpty()) {
                    cb.onSuccess(new Playback(
                            episode.directUrl,
                            mimeOf(episode.directUrl),
                            headers(episode.directUrl),
                            Collections.emptyList()));
                    return;
                }
                String requestUrl = episode.id;
                if (requestUrl == null || !requestUrl.startsWith("http")) {
                    throw new Exception("Thiếu URL API nguồn phát");
                }

                List<StreamCandidate> candidates = parseSources(get(requestUrl));
                if (candidates.isEmpty()) {
                    throw new Exception("NovaHD không trả về nguồn HLS/DASH");
                }
                sortCandidates(candidates);

                Exception last = null;
                for (StreamCandidate c : candidates) {
                    try {
                        if (!verifyStream(c.url, refererFor(episode))) continue;
                        Map<String,String> h = headers(requestUrl);
                        h.put("Referer", refererFor(episode));
                        h.put("Origin", BASE);
                        cb.onSuccess(new Playback(c.url, mimeOf(c.url), h, c.subtitles));
                        return;
                    } catch (Exception e) {
                        last = e;
                    }
                }
                throw new Exception(last == null ? "Không có nguồn phát hợp lệ" : last.getMessage());
            } catch (Exception e) {
                cb.onError("NovaHD phát phim: " + safe(e));
            }
        });
    }

    private boolean verifyStream(String url, String referer) throws Exception {
        Map<String,String> h = headers(url);
        if (referer != null && !referer.isEmpty()) h.put("Referer", referer);
        h.put("Origin", BASE);
        String body = MovieHttp.get(url, h);
        String t = body == null ? "" : body.trim();
        if (t.startsWith("#EXTM3U") || t.contains("<MPD")) return true;
        throw new Exception("CDN không trả playlist hợp lệ");
    }

    private String refererFor(MovieEpisode ep) {
        try {
            java.net.URI u = new java.net.URI(ep.id);
            String qs = u.getQuery();
            String tmdb = query(qs, "tmdbId");
            String type = query(qs, "type");
            String season = query(qs, "season");
            String episode = query(qs, "episode");
            if ("show".equalsIgnoreCase(type) && tmdb != null) {
                return BASE + "/watch/s/" + tmdb + "/" + (season == null ? "1" : season) + "/" + (episode == null ? "1" : episode);
            }
            if (tmdb != null) return BASE + "/watch/m/" + tmdb;
        } catch (Exception ignored) {}
        return BASE + "/watch/m/" + (id.isEmpty() ? "0" : id);
    }

    private static String query(String q, String wanted) {
        if (q == null) return null;
        for (String part : q.split("&")) {
            int p = part.indexOf('=');
            if (p < 0) continue;
            if (wanted.equals(java.net.URLDecoder.decode(part.substring(0, p), StandardCharsets.UTF_8.name()))) {
                return java.net.URLDecoder.decode(part.substring(p + 1), StandardCharsets.UTF_8.name());
            }
        }
        return null;
    }

    private List<MovieItem> parseList(String json) throws Exception {
        Object root = new JSONTokener(json == null ? "{}" : json).nextValue();
        JSONArray a = firstArray(root, "results", "items", "movies", "shows");
        if (a == null && root instanceof JSONObject) {
            Object data = ((JSONObject) root).opt("data");
            a = firstArray(data, "results", "items", "movies", "shows");
        }
        List<MovieItem> out = new ArrayList<>();
        if (a == null) return out;
        for (int i = 0; i < a.length() && out.size() < 80; i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            String id = str(o, "tmdbId", "tmdb_id", "id");
            String title = str(o, "title", "name");
            if (id == null || title == null || id.isEmpty() || title.isEmpty()) continue;
            boolean show = "show".equalsIgnoreCase(str(o, "type", "mediaType"))
                    || "tv".equalsIgnoreCase(str(o, "type", "mediaType"))
                    || o.has("firstAirDate") || o.has("first_air_date") || o.has("seasons");
            String poster = image(o, TMDB_POSTER, "posterUrl", "poster", "posterPath", "poster_path");
            String backdrop = image(o, TMDB_BACKDROP, "backdropUrl", "backdrop", "backdropPath", "backdrop_path");
            String q = str(o, "quality", "resolution", "format");
            String year = str(o, "releaseDate", "release_date", "firstAirDate", "first_air_date", "year");
            if (year != null && year.length() > 4) year = year.substring(0, 4);
            String prefix = show ? "show/" : "movie/";
            out.add(new MovieItem(
                    prefix + MovieJson.stripPrefix(id),
                    title,
                    poster,
                    backdrop == null || backdrop.isEmpty() ? poster : backdrop,
                    q == null ? "FHD" : q,
                    year == null ? "" : year,
                    sourceId,
                    show,
                    BASE + (show ? "/api/shows/" : "/api/movies/") + enc(MovieJson.stripPrefix(id))
            ));
        }
        return out;
    }

    private MovieDetail parseDetail(String json, MovieItem seed) throws Exception {
        Object root = new JSONTokener(json == null ? "{}" : json).nextValue();
        JSONObject o = root instanceof JSONObject ? (JSONObject) root : new JSONObject();
        Object data = o.opt("data");
        if (data instanceof JSONObject) o = (JSONObject) data;

        String id = str(o, "tmdbId", "tmdb_id", "id");
        if (id == null || id.isEmpty()) id = MovieJson.stripPrefix(seed.id);
        boolean show = seed.show || "show".equalsIgnoreCase(str(o, "type", "mediaType"))
                || o.has("seasons") || o.has("firstAirDate") || o.has("first_air_date");
        String title = str(o, "title", "name");
        if (title == null || title.isEmpty()) title = seed.title;
        String poster = image(o, TMDB_POSTER, "posterUrl", "poster", "posterPath", "poster_path");
        String backdrop = image(o, TMDB_BACKDROP, "backdropUrl", "backdrop", "backdropPath", "backdrop_path");
        if (poster == null || poster.isEmpty()) poster = seed.posterUrl;
        if (backdrop == null || backdrop.isEmpty()) backdrop = seed.backdropUrl;

        List<MovieEpisode> eps = new ArrayList<>();
        JSONArray seasons = o.optJSONArray("seasons");
        if (show && seasons != null) {
            for (int sIdx = 0; sIdx < seasons.length(); sIdx++) {
                JSONObject s = seasons.optJSONObject(sIdx);
                if (s == null) continue;
                int sn = intValue(s, "seasonNumber", "season", "season_no");
                if (sn <= 0) sn = sIdx + 1;
                JSONArray ea = s.optJSONArray("episodes");
                int count = intValue(s, "episodeCount", "episode_count");
                if (ea != null && ea.length() > 0) {
                    for (int eIdx = 0; eIdx < ea.length(); eIdx++) {
                        JSONObject ep = ea.optJSONObject(eIdx);
                        if (ep == null) continue;
                        int en = intValue(ep, "episodeNumber", "episode", "number");
                        if (en <= 0) en = eIdx + 1;
                        String ename = str(ep, "title", "name");
                        String eid = BASE + "/api/sources?type=show&tmdbId=" + enc(id) + "&season=" + sn + "&episode=" + en;
                        eps.add(new MovieEpisode(eid,
                                ename == null || ename.isEmpty() ? "Tập " + en : "Tập " + en + ": " + ename,
                                "s" + sn + "-e" + en, sn, en, null));
                    }
                } else {
                    if (count <= 0) count = 1;
                    for (int en = 1; en <= count; en++) {
                        String eid = BASE + "/api/sources?type=show&tmdbId=" + enc(id) + "&season=" + sn + "&episode=" + en;
                        eps.add(new MovieEpisode(eid, "Tập " + en, "s" + sn + "-e" + en, sn, en, null));
                    }
                }
            }
        }
        if (eps.isEmpty()) {
            String eid = BASE + "/api/sources?type=movie&tmdbId=" + enc(id);
            eps.add(new MovieEpisode(eid, "▶ Xem phim", "full", 0, 0, null));
        }

        String year = str(o, "releaseDate", "release_date", "firstAirDate", "first_air_date", "year");
        if (year != null && year.length() > 4) year = year.substring(0, 4);
        MovieItem movie = new MovieItem(
                (show ? "show/" : "movie/") + MovieJson.stripPrefix(id),
                title, poster, backdrop,
                seed.quality == null || seed.quality.isEmpty() ? "FHD" : seed.quality,
                year == null ? seed.year : year,
                sourceId, show,
                seed.detailUrl
        );
        return new MovieDetail(
                movie,
                str(o, "overview", "description", "plot", "summary"),
                textValue(o.opt("genres"), "Phim"),
                textValue(o.opt("cast"), ""),
                textValue(o.opt("director"), ""),
                str(o, "voteAverage", "vote_average", "rating", "imdbRating"),
                eps
        );
    }

    private List<StreamCandidate> parseSources(String body) throws Exception {
        List<StreamCandidate> out = new ArrayList<>();
        if (body == null) return out;
        String trimmed = body.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                Object root = new JSONTokener(trimmed).nextValue();
                collectSources(root, out);
                return out;
            } catch (Exception ignored) {}
        }
        for (String line : body.split("\\r?\\n")) {
            String s = line.trim();
            if (!s.isEmpty() && !s.equals(":")) {
                try { collectSources(new JSONTokener(s).nextValue(), out); } catch (Exception ignored) {}
            }
        }
        return out;
    }

    private void collectSources(Object root, List<StreamCandidate> out) {
        if (root instanceof JSONObject) {
            JSONObject o = (JSONObject) root;
            JSONArray a = o.optJSONArray("sources");
            if (a != null) {
                List<Subtitle> subs = parseSubs(o.optJSONArray("subtitles"));
                for (int i = 0; i < a.length(); i++) {
                    JSONObject s = a.optJSONObject(i);
                    if (s == null) continue;
                    String url = str(s, "url", "src", "file", "streamUrl");
                    if (url != null && url.startsWith("http") && (url.contains(".m3u8") || url.contains(".mpd") || url.contains(".mp4") || "hls".equalsIgnoreCase(str(s, "type")))) {
                        out.add(new StreamCandidate(url, str(s, "quality", "resolution"), str(s, "language", "lang"), subs));
                    }
                }
            }
            Object d = o.opt("data");
            if (d != null) collectSources(d, out);
        } else if (root instanceof JSONArray) {
            JSONArray a = (JSONArray) root;
            for (int i = 0; i < a.length(); i++) collectSources(a.opt(i), out);
        }
    }

    private void sortCandidates(List<StreamCandidate> list) {
        Comparator<StreamCandidate> c = Comparator.comparingInt((StreamCandidate s) -> qualityScore(s.quality)).reversed()
                .thenComparingInt(s -> "en".equalsIgnoreCase(s.language) || "eng".equalsIgnoreCase(s.language) ? 0 : 1);
        Collections.sort(list, c);
    }

    private static int qualityScore(String q) {
        if (q == null) return 0;
        String x = q.toLowerCase();
        if (x.contains("2160") || x.contains("4k")) return 2160;
        if (x.contains("1440")) return 1440;
        if (x.contains("1080")) return 1080;
        if (x.contains("720")) return 720;
        if (x.contains("480")) return 480;
        return x.contains("auto") ? 1080 : 0;
    }

    private static final class StreamCandidate {
        final String url, quality, language;
        final List<Subtitle> subtitles;
        StreamCandidate(String u, String q, String l, List<Subtitle> s) { url=u; quality=q; language=l; subtitles=s; }
    }

    private static JSONArray firstArray(Object root, String... keys) {
        if (!(root instanceof JSONObject)) return null;
        JSONObject o = (JSONObject) root;
        for (String k : keys) {
            JSONArray a = o.optJSONArray(k);
            if (a != null) return a;
        }
        return null;
    }

    private static String str(JSONObject o, String... keys) {
        for (String k : keys) {
            if (!o.has(k) || o.isNull(k)) continue;
            Object v = o.opt(k);
            if (v instanceof String) {
                String s=((String)v).trim();
                if (!s.isEmpty()) return s;
            } else if (v instanceof Number || v instanceof Boolean) {
                return String.valueOf(v);
            }
        }
        return null;
    }

    private static String textValue(Object v, String fallback) {
        String s = MovieJson.valueText(v);
        return s == null || s.isEmpty() ? fallback : s;
    }

    private static String image(JSONObject o, String base, String... keys) {
        String s = str(o, keys);
        if (s == null || s.isEmpty()) return "";
        if (s.startsWith("http://") || s.startsWith("https://")) return s;
        return base + (s.startsWith("/") ? s : "/" + s);
    }

    private static int intValue(JSONObject o, String... keys) {
        String s=str(o,keys);
        try { return s == null ? 0 : Integer.parseInt(s); } catch(Exception e) { return 0; }
    }

    private static List<Subtitle> parseSubs(JSONArray a) {
        List<Subtitle> out=new ArrayList<>();
        if(a==null)return out;
        for(int i=0;i<a.length();i++){
            JSONObject s=a.optJSONObject(i);
            if(s==null)continue;
            String url=str(s,"url","file","src");
            if(url==null||!url.startsWith("http"))continue;
            String lang=str(s,"lang","language");
            String label=str(s,"label","name");
            out.add(new Subtitle(url,lang==null?"":lang,label==null?"Subtitle":label));
        }
        return out;
    }

    private static String enc(String s) {
        try { return URLEncoder.encode(s==null?"":s, StandardCharsets.UTF_8.name()); }
        catch(Exception e){ return s==null?"":s; }
    }

    private static String mimeOf(String url) {
        String x=url==null?"":url.toLowerCase();
        if(x.contains(".mpd"))return "application/dash+xml";
        if(x.contains(".m3u8"))return "application/x-mpegURL";
        return "video/mp4";
    }

    private static String safe(Exception e) {
        return e == null ? "không rõ lỗi" : (e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());
    }

    @Override public void close() { io.shutdownNow(); }
}
