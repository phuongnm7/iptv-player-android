package vn.phuong.iptvplayer.movie;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Assume;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NovaHdNetworkSmokeTest {
    private static final String BASE = "https://novahd.cc";

    @Test(timeout = 45000)
    public void novaApiAndSourceParserSmoke() throws Exception {
        String fixture = "{\"results\":[{" +
                "\"tmdbId\":550,\"type\":\"movie\",\"title\":\"Fight Club\"," +
                "\"posterPath\":\"/poster.jpg\",\"backdropPath\":\"/backdrop.jpg\"}]}";
        List<MovieModels.MovieItem> items = MovieJson.parseList(fixture, BASE, "novahd");
        assertTrue("Nova fixture did not parse into a movie", !items.isEmpty());
        assertTrue(items.get(0).id.startsWith("movie/"));

        String ndjson = "{\"sources\":[{\"quality\":\"1080p\",\"type\":\"hls\",\"url\":\"https://cdn.example/test.m3u8\"}],"
                + "\"subtitles\":[{\"lang\":\"en\",\"url\":\"https://cdn.example/en.vtt\"}]}";
        MovieModels.Playback playback =
                MovieJson.parsePlayback(ndjson, "", BASE);
        assertNotNull("Nova NDJSON fixture did not produce playback", playback);
        assertTrue(playback.url.contains(".m3u8"));

        try {
            String home = fetchOnce(BASE + "/api/trending?type=all");
            assertTrue("Nova live API response is unexpectedly empty", home.length() > 100);

            Object root = new JSONTokener(home).nextValue();
            JSONArray results = null;
            if (root instanceof JSONObject) {
                JSONObject o = (JSONObject) root;
                results = o.optJSONArray("results");
                if (results == null) results = o.optJSONArray("items");
                if (results == null && o.opt("data") instanceof JSONObject) {
                    JSONObject d = o.optJSONObject("data");
                    if (d != null) {
                        results = d.optJSONArray("results");
                        if (results == null) results = d.optJSONArray("items");
                    }
                }
            } else if (root instanceof JSONArray) {
                results = (JSONArray) root;
            }
            assertNotNull("Nova live API did not contain a result array", results);
            assertTrue("Nova live API returned no titles", results.length() > 0);
        } catch (Exception networkError) {
            Assume.assumeNoException("Nova live API is unreachable from this CI runner", networkError);
        }
    }

    private static String fetchOnce(String url) throws Exception {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(7000);
            c.setReadTimeout(9000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 "
                            + "(KHTML, like Gecko) Chrome/131.0 Mobile Safari/537.36");
            c.setRequestProperty("Accept", "*/*");
            c.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
            c.setRequestProperty("Origin", BASE);
            c.setRequestProperty("Referer", BASE + "/");
            c.setRequestProperty("x-nova-visitor", UUID.randomUUID().toString());
            int code = c.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new Exception("HTTP " + code);
            }
            java.io.InputStream in = c.getInputStream();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            in.close();
            return out.toString(java.nio.charset.StandardCharsets.UTF_8.name());
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
