package vn.phuong.iptvplayer.movie;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

public final class Film4kSource implements MovieSource {
    private static final String[] BASES = {
            "https://fiml4k.fun",
            "https://film4k.annnekkk.com",
            "https://film4k.net"
    };

    private final ExecutorService io = Executors.newFixedThreadPool(2);
    private volatile String workingBase = BASES[0];

    @Override public String id() { return "film4k"; }
    @Override public String name() { return "Film4k"; }
    @Override public boolean isPlugin() { return false; }

    private Map<String, String> headers(String base) {
        Map<String, String> h = new LinkedHashMap<>();
        h.put("Referer", base + "/");
        h.put("Origin", base);
        h.put("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7");
        return h;
    }

    private List<String> orderedBases() {
        List<String> all = new ArrayList<>();
        if (workingBase != null && !workingBase.isEmpty()) all.add(workingBase);
        for (String base : BASES) if (!all.contains(base)) all.add(base);
        return all;
    }

    private String fetchFirst(String... paths) throws Exception {
        Exception last = null;
        for (String base : orderedBases()) {
            for (String path : paths) {
                try {
                    String url = path.startsWith("http://") || path.startsWith("https://")
                            ? path
                            : base + path;
                    String response = MovieHttp.get(url, headers(base));
                    if (response != null && !response.trim().isEmpty()) {
                        workingBase = base;
                        return response;
                    }
                } catch (Exception e) {
                    last = e;
                }
            }
        }
        throw new Exception(last == null ? "Không kết nối được Film4k" : last.getMessage());
    }

    @Override public void loadHome(Callback<List<MovieItem>> cb) {
        io.execute(() -> {
            try {
                String json = fetchFirst("/api/home", "/api/home?");
                List<MovieItem> items = MovieJson.parseList(json, workingBase, id());
                if (items.isEmpty()) throw new Exception("Film4k không trả về danh sách phim");
                cb.onSuccess(items);
            } catch (Exception e) {
                cb.onError("Film4k: " + safe(e));
            }
        });
    }

    @Override public void search(String query, Callback<List<MovieItem>> cb) {
        io.execute(() -> {
            try {
                String q = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
                String json = fetchFirst(
                        "/api/search?search=" + q,
                        "/api/search?q=" + q,
                        "/api/home?search=" + q
                );
                List<MovieItem> items = MovieJson.parseList(json, workingBase, id());
                cb.onSuccess(items);
            } catch (Exception e) {
                cb.onError("Không tìm thấy: " + safe(e));
            }
        });
    }

    @Override public void loadDetail(MovieItem item, Callback<MovieDetail> cb) {
        io.execute(() -> {
            try {
                String id = MovieJson.stripPrefix(item.id);
                String fallbackId = id == null || id.isEmpty() ? item.title : id;
                List<String> paths = new ArrayList<>();

                if (item.detailUrl != null && !item.detailUrl.isEmpty()
                        && !MovieJson.findPlayable(item.detailUrl).equals(item.detailUrl)) {
                    paths.add(item.detailUrl);
                }

                String e = URLEncoder.encode(fallbackId, StandardCharsets.UTF_8.name());
                paths.add("/api/title/" + e);
                if (item.show) {
                    paths.add("/api/shows/" + e);
                    paths.add("/api/show/" + e);
                } else {
                    paths.add("/api/movies/" + e);
                    paths.add("/api/movie/" + e);
                    paths.add("/api/film/" + e);
                }

                String[] candidates = paths.toArray(new String[0]);
                String json = fetchFirst(candidates);
                cb.onSuccess(MovieJson.parseDetail(json, workingBase, item));
            } catch (Exception ex) {
                cb.onError("Không tải được chi tiết phim: " + safe(ex));
            }
        });
    }

    @Override public void loadPlayback(MovieEpisode ep, Callback<Playback> cb) {
        io.execute(() -> {
            try {
                if (ep.directUrl != null && !ep.directUrl.isEmpty()) {
                    cb.onSuccess(new Playback(
                            ep.directUrl,
                            mimeOf(ep.directUrl),
                            headers(workingBase),
                            Collections.emptyList()
                    ));
                    return;
                }

                String req = ep.id;
                if (req == null || req.isEmpty()) throw new Exception("Thiếu URL xem phim");

                String id = MovieJson.stripPrefix(req);
                String[] candidates;
                if (req.startsWith("http://") || req.startsWith("https://")) {
                    candidates = new String[]{req};
                } else {
                    String e = URLEncoder.encode(id, StandardCharsets.UTF_8.name());
                    candidates = new String[]{
                            "/api/watch/" + e,
                            "/api/play/" + e
                    };
                }

                String json = fetchFirst(candidates);
                Playback p = MovieJson.parsePlayback(json, req, workingBase);
                if (p == null) {
                    String ticket = MovieJson.findTicket(new org.json.JSONTokener(json).nextValue());
                    if (ticket != null && !ticket.isEmpty()) {
                        String enc = URLEncoder.encode(ticket, StandardCharsets.UTF_8.name());
                        String r = MovieHttp.get(workingBase + "/api/play-ticket?ticket=" + enc, headers(workingBase));
                        p = MovieJson.parsePlayback(r, workingBase + "/api/play-ticket?ticket=" + enc, workingBase);
                        if (p == null) {
                            String body = new org.json.JSONObject().put("ticket", ticket).toString();
                            r = MovieHttp.postJson(workingBase + "/api/play-ticket", headers(workingBase), body);
                            p = MovieJson.parsePlayback(r, workingBase + "/api/play-ticket", workingBase);
                        }
                    }
                }

                if (p == null) throw new Exception("API không trả về link phát HLS/DASH");
                cb.onSuccess(p);
            } catch (Exception e) {
                cb.onError("Không lấy được luồng phim: " + safe(e));
            }
        });
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
    }
}