package vn.phuong.iptvplayer.movie;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.test.InstrumentationRegistry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import vn.phuong.iptvplayer.PlayerActivity;
import vn.phuong.iptvplayer.movie.MovieModels.MovieDetail;
import vn.phuong.iptvplayer.movie.MovieModels.MovieEpisode;
import vn.phuong.iptvplayer.movie.MovieModels.MovieItem;
import vn.phuong.iptvplayer.movie.MovieModels.Playback;

public class MoviePluginEndToEndTest {
    private static final String BASE = "https://novahd.cc";
    private static final long CALL_TIMEOUT_SECONDS = 45;
    private static final long PLAYER_TIMEOUT_SECONDS = 90;

    @Test
    public void novaPluginSearchDetailEpisodeSourceAndPlayerAreUsable() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final String script = readAsset(context, "movie-plugins/novahd_plugin.js");

        final AtomicReference<MovieJsRuntime> runtimeRef = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> runtimeRef.set(new MovieJsRuntime(context, script, BASE + "/")));
        MovieJsRuntime runtime = runtimeRef.get();
        assertNotNull(runtime);

        Activity playerActivity = null;
        try {
            final String manifest;
            try {
                manifest = call(runtime, "getManifest");
            } catch (Exception e) {
                if (isNovaAccessBlocked(e)) {
                    Assume.assumeTrue("NovaHD Cloudflare is not automatable in headless CI: " + e.getMessage(), false);
                    return;
                }
                throw e;
            }
            assertTrue("Plugin manifest missing NovaHD id", manifest.contains("\"id\":\"novahd\""));
            assertTrue("Plugin manifest missing Media3 player contract", manifest.contains("\"playerType\":\"media3\""));

            String searchUrl = call(runtime, "getUrlSearch",
                    MovieJsRuntime.quote("Avatar"),
                    MovieJsRuntime.quote("{\"page\":1}"));
            assertTrue("Plugin search URL is wrong: " + searchUrl,
                    searchUrl.startsWith(BASE + "/api/search?search="));

            final String searchBody;
            try {
                searchBody = fetchWeb(runtime, searchUrl, MovieHttp.novaHeaders(searchUrl));
            } catch (Exception e) {
                if (isNovaAccessBlocked(e)) {
                    Assume.assumeTrue("NovaHD Cloudflare blocks GitHub CI: " + e.getMessage(), false);
                    return;
                }
                throw e;
            }
            assertFalse("NovaHD search API returned an empty body", searchBody.trim().isEmpty());

            String pluginSearch = call(runtime, "parseSearchResponse",
                    MovieJsRuntime.quote(normalize(searchBody)),
                    MovieJsRuntime.quote(searchUrl));
            List<MovieItem> items = MovieJson.parseList(
                    normalize(searchBody), BASE, "novahd");
            if (items.isEmpty()) {
                items = parsePluginItems(pluginSearch);
            }
            assertFalse("NovaHD search returned no usable movie records", items.isEmpty());

            final StreamResult selected;
            try {
                selected = resolveFirstPlayable(context, runtime, items);
            } catch (NovaAccessBlockedException e) {
                Assume.assumeTrue("NovaHD Cloudflare blocks GitHub CI: " + e.getMessage(), false);
                return;
            }
            assertNotNull("Could not resolve a playable NovaHD result", selected);
            assertNotNull(selected.playback);
            System.out.println("E2E_PHASE=stream-resolved");
            System.out.println("E2E_STREAM_URL=" + selected.playback.url);
            System.out.println("E2E_STREAM_MIME=" + selected.playback.mime);
            System.out.println("E2E_STREAM_HEADERS=" + selected.playback.headers.keySet());
            assertTrue("Resolved stream URL is not HTTP(S)",
                    selected.playback.url.startsWith("http://")
                            || selected.playback.url.startsWith("https://"));

            Intent intent = new Intent(context, PlayerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(PlayerActivity.EXTRA_NAME, selected.episodeName);
            intent.putExtra(PlayerActivity.EXTRA_URL, selected.playback.url);
            intent.putExtra(PlayerActivity.EXTRA_MIME, selected.playback.mime);
            intent.putExtra(PlayerActivity.EXTRA_CONTENT_TYPE, PlayerActivity.CONTENT_MOVIE);

            Bundle headers = new Bundle();
            for (Map.Entry<String, String> e : selected.playback.headers.entrySet()) {
                headers.putString(e.getKey(), e.getValue());
            }
            intent.putExtra(PlayerActivity.EXTRA_HEADERS, headers);

            ArrayList<Bundle> subtitles = new ArrayList<>();
            for (MovieModels.Subtitle sub : selected.playback.subtitles) {
                Bundle b = new Bundle();
                b.putString("url", sub.url);
                b.putString("lang", sub.lang);
                b.putString("label", sub.label);
                subtitles.add(b);
            }
            intent.putParcelableArrayListExtra(PlayerActivity.EXTRA_SUBTITLES, subtitles);

            System.out.println("E2E_PHASE=starting-player-activity");
            playerActivity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
            System.out.println("E2E_PHASE=player-activity-started");
            assertNotNull(playerActivity);

            System.out.println("E2E_PHASE=waiting-player-ready");
            boolean ready = waitForPlayerReady(playerActivity, PLAYER_TIMEOUT_SECONDS);
            assertTrue(
                    "Movie PlayerActivity did not reach STATE_READY. stream="
                            + selected.playback.url,
                    ready
            );
        } finally {
            if (playerActivity != null) {
                final Activity activityToClose = playerActivity;
                InstrumentationRegistry.getInstrumentation().runOnMainSync(
                        activityToClose::finish);
            }
            final MovieJsRuntime runtimeToClose = runtime;
            InstrumentationRegistry.getInstrumentation().runOnMainSync(
                    runtimeToClose::destroy);
        }
    }

    private static String fetchWeb(MovieJsRuntime runtime, String url,
                                   Map<String, String> headers) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        runtime.webGet(url, headers, new MovieJsRuntime.Callback() {
            @Override public void done(String value) {
                result.set(value);
                latch.countDown();
            }

            @Override public void error(String message) {
                error.set(message);
                latch.countDown();
            }
        });

        if (!latch.await(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new Exception("Timed out fetching " + url);
        }
        if (error.get() != null) throw new Exception(error.get());
        return result.get() == null ? "" : result.get();
    }

    private static StreamResult resolveFirstPlayable(
            Context context, MovieJsRuntime runtime, List<MovieItem> items) throws Exception {
        Exception last = null;
        int limit = Math.min(5, items.size());

        for (int i = 0; i < limit; i++) {
            MovieItem item = items.get(i);
            try {
                String detailUrl = call(runtime, "getUrlDetail", MovieJsRuntime.quote(item.id));
                assertTrue("Plugin detail URL is not HTTP: " + detailUrl, isHttp(detailUrl));

                String detailBody;
                try {
                    detailBody = fetchWeb(
                            runtime, detailUrl, MovieHttp.novaHeaders(detailUrl));
                } catch (Exception e) {
                    if (isNovaAccessBlocked(e)) {
                        throw new NovaAccessBlockedException(e.getMessage());
                    }
                    throw e;
                }
                String parsedDetail = call(
                        runtime, "parseMovieDetail",
                        MovieJsRuntime.quote(normalize(detailBody)),
                        MovieJsRuntime.quote(detailUrl));

                MovieDetail detail = parseDetailForTest(parsedDetail, item);
                if (detail == null || detail.episodes.isEmpty()) continue;

                for (MovieEpisode episode : detail.episodes) {
                    if (episode == null || episode.id == null || episode.id.trim().isEmpty()) {
                        continue;
                    }

                    String sourceUrl = episode.id.trim();
                    if (!isHttp(sourceUrl)) {
                        continue;
                    }

                    String sourceBody;
                    try {
                        sourceBody = fetchWeb(
                                runtime, sourceUrl, MovieHttp.novaHeaders(sourceUrl));
                    } catch (Exception e) {
                        if (isNovaAccessBlocked(e)) {
                            throw new NovaAccessBlockedException(e.getMessage());
                        }
                        throw e;
                    }
                    String parsedPlayback = call(
                            runtime, "parseDetailResponse",
                            MovieJsRuntime.quote(normalize(sourceBody)),
                            MovieJsRuntime.quote(sourceUrl));

                    Playback playback = parsePluginPlayback(parsedPlayback, sourceUrl);
                    if (playback == null) {
                        playback = MovieJson.parsePlayback(
                                normalize(sourceBody), sourceUrl, BASE);
                    }
                    if (playback == null || !isHttp(playback.url)) continue;
                    if (playback.url.contains("/api/sources")) {
                        throw new Exception("Plugin returned source API URL instead of a playable stream: " + playback.url);
                    }

                    return new StreamResult(episode.name, playback);
                }
            } catch (NovaAccessBlockedException e) {
                throw e;
            } catch (Exception e) {
                last = e;
                System.out.println("Skipping candidate " + item.title + ": " + e);
            }
        }

        if (last instanceof NovaAccessBlockedException) {
            throw last;
        }
        throw new Exception("No playable candidate found", last);
    }

    private static boolean isNovaAccessBlocked(Exception e) {
        String x = e == null ? "" : String.valueOf(e.getMessage()).toLowerCase();
        return x.contains("http 403")
                || x.contains("http 503")
                || x.contains("cloudflare")
                || x.contains("just a moment")
                || x.contains("no activity")
                || x.contains("xác minh cloudflare")
                || x.contains("không có activity");
    }

    private static final class NovaAccessBlockedException extends Exception {
        NovaAccessBlockedException(String message) {
            super(message);
        }
    }

    @Test
    public void media3PlayerSmokeWithKnownPublicHls() throws Exception {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // The CI workflow serves a deterministic local H.264 MP4 via the emulator host alias.
        final String stream = "http://10.0.2.2:8765/movie-smoke.mp4";

        Intent intent = new Intent(context, PlayerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(PlayerActivity.EXTRA_NAME, "Player Smoke Test");
        intent.putExtra(PlayerActivity.EXTRA_URL, stream);
        intent.putExtra(PlayerActivity.EXTRA_MIME, "video/mp4");
        intent.putExtra(PlayerActivity.EXTRA_CONTENT_TYPE, PlayerActivity.CONTENT_MOVIE);

        Activity activity = null;
        try {
            activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
            assertNotNull(activity);
            assertTrue(
                    "Media3 PlayerActivity did not reach STATE_READY for deterministic local H.264 asset",
                    waitForPlayerReady(activity, PLAYER_TIMEOUT_SECONDS)
            );
        } finally {
            if (activity != null) {
                final Activity toClose = activity;
                InstrumentationRegistry.getInstrumentation().runOnMainSync(toClose::finish);
            }
        }
    }

    private static MovieDetail parseDetailForTest(String parsed, MovieItem seed) {
        try {
            JSONObject o = new JSONObject(parsed == null ? "{}" : parsed);
            JSONArray servers = o.optJSONArray("servers");
            if (servers != null) {
                List<MovieEpisode> eps = new ArrayList<>();
                for (int i = 0; i < servers.length(); i++) {
                    JSONObject server = servers.optJSONObject(i);
                    if (server == null) continue;
                    JSONArray a = server.optJSONArray("episodes");
                    if (a == null) continue;
                    for (int j = 0; j < a.length(); j++) {
                        JSONObject ep = a.optJSONObject(j);
                        if (ep == null) continue;
                        String id = first(ep, "id", "url", "link");
                        if (!isHttp(id)) continue;
                        String name = first(ep, "name", "title");
                        eps.add(new MovieEpisode(
                                id,
                                name == null ? "Tập " + (j + 1) : name,
                                "test-" + j, 0, j + 1, null));
                    }
                }
                if (!eps.isEmpty()) {
                    return new MovieDetail(seed, "", "Phim", "", "", "", eps);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Playback parsePluginPlayback(String parsed, String sourceUrl) {
        try {
            JSONObject o = new JSONObject(parsed == null ? "{}" : parsed);
            String url = first(o, "url", "streamUrl", "stream_url", "playUrl", "play_url");
            if (!isPlayable(url)) {
                url = MovieJson.findPlayable(o);
            }
            if (!isHttp(url) || !isPlayable(url)) return null;

            java.util.LinkedHashMap<String, String> headers =
                    new java.util.LinkedHashMap<>();
            JSONObject h = o.optJSONObject("headers");
            if (h != null) {
                JSONArray names = h.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String k = names.optString(i);
                        if ("custom-js".equalsIgnoreCase(k)) continue;
                        String v = first(h, k);
                        if (v != null) headers.put(k, v);
                    }
                }
            }

            return new Playback(
                    stripFragment(url),
                    mimeOf(url),
                    headers,
                    new ArrayList<>());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static List<MovieItem> parsePluginItems(String parsed) {
        List<MovieItem> out = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(parsed == null ? "{}" : parsed);
            JSONArray a = root.optJSONArray("items");
            if (a == null) return out;
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o == null) continue;
                String id = first(o, "id", "slug", "tmdbId", "movieId", "showId");
                String title = first(o, "title", "name");
                if (id == null || title == null) continue;
                boolean show = id.startsWith("show/");
                String normalized = (id.startsWith("movie/") || id.startsWith("show/"))
                        ? id : (show ? "show/" : "movie/") + id;
                out.add(new MovieItem(
                        normalized, title,
                        first(o, "posterUrl", "poster", "posterPath"),
                        first(o, "backdropUrl", "backdrop", "backdropPath"),
                        first(o, "quality"), first(o, "year"),
                        "novahd", show, null));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static String call(MovieJsRuntime runtime, String fn, String... args)
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> result = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();

        runtime.call(fn, new MovieJsRuntime.Callback() {
            @Override public void done(String value) {
                result.set(value);
                latch.countDown();
            }

            @Override public void error(String message) {
                error.set(message);
                latch.countDown();
            }
        }, args);

        if (!latch.await(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            throw new Exception("Timed out calling plugin function " + fn);
        }
        if (error.get() != null) throw new Exception(error.get());
        return result.get() == null ? "" : result.get();
    }

    private static boolean waitForPlayerReady(Activity activity, long seconds)
            throws Exception {
        Field field = PlayerActivity.class.getDeclaredField("player");
        field.setAccessible(true);

        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            final AtomicInteger state = new AtomicInteger(androidx.media3.common.Player.STATE_IDLE);
            final AtomicReference<androidx.media3.common.PlaybackException> error =
                    new AtomicReference<>();
            final AtomicReference<Object> playerRef = new AtomicReference<>();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(
                    () -> {
                        try {
                            Object value = field.get(activity);
                            playerRef.set(value);
                            if (value instanceof androidx.media3.common.Player) {
                                androidx.media3.common.Player player =
                                        (androidx.media3.common.Player) value;
                                // Media3 Player is Main-thread confined: all reads happen
                                // inside the UI thread block, not on the instrumentation thread.
                                state.set(player.getPlaybackState());
                                error.set(player.getPlayerError());
                            }
                        } catch (Exception ignored) {}
                    });
            if (state.get() == androidx.media3.common.Player.STATE_READY) {
                return true;
            }
            if (state.get() == androidx.media3.common.Player.STATE_IDLE && error.get() != null) {
                return false;
            }
            Thread.sleep(250L);
        }
        return false;
    }

    private static String readAsset(Context context, String path) throws Exception {
        try (InputStream in = context.getAssets().open(path);
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            return out.toString("UTF-8");
        }
    }

    private static String normalize(String body) {
        if (body == null) return "";
        return body.trim();
    }

    private static String first(JSONObject o, String... keys) {
        for (String k : keys) {
            if (!o.has(k) || o.isNull(k)) continue;
            String s = String.valueOf(o.opt(k)).trim();
            if (!s.isEmpty()) return s;
        }
        return null;
    }

    private static boolean isHttp(String url) {
        return url != null
                && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private static boolean isPlayable(String url) {
        if (!isHttp(url)) return false;
        String x = url.toLowerCase();
        return x.contains(".m3u8") || x.contains(".mpd")
                || x.contains(".mp4") || x.contains("stream")
                || x.contains("playlist");
    }

    private static String stripFragment(String url) {
        int p = url == null ? -1 : url.indexOf('#');
        return p >= 0 ? url.substring(0, p) : url;
    }

    private static String mimeOf(String url) {
        String x = url == null ? "" : url.toLowerCase();
        if (x.contains(".mpd")) return "application/dash+xml";
        if (x.contains(".m3u8")) return "application/x-mpegURL";
        return "video/mp4";
    }

    private static final class StreamResult {
        final String episodeName;
        final Playback playback;
        StreamResult(String episodeName, Playback playback) {
            this.episodeName = episodeName;
            this.playback = playback;
        }
    }
}
