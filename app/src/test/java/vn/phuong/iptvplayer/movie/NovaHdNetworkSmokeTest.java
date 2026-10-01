package vn.phuong.iptvplayer.movie;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NovaHdNetworkSmokeTest {
    private static final String BASE = "https://novahd.cc";

    @Test
    public void novaApiAndOneMovieStreamSmoke() throws Exception {
        Map<String, String> headers =
                MovieHttp.novaHeaders(BASE + "/api/trending?type=all", UUID.randomUUID().toString());

        String home = MovieHttp.get(BASE + "/api/trending?type=all", headers);
        assertNotNull(home);
        assertTrue("Nova home response is unexpectedly empty", home.length() > 100);

        Object root = new JSONTokener(home).nextValue();
        JSONArray results = null;
        if (root instanceof JSONObject) {
            JSONObject o = (JSONObject) root;
            results = o.optJSONArray("results");
            if (results == null) results = o.optJSONArray("items");
            if (results == null && o.opt("data") instanceof JSONObject) {
                JSONObject d = o.optJSONObject("data");
                results = d == null ? null : d.optJSONArray("results");
                if (results == null && d != null) results = d.optJSONArray("items");
            }
        } else if (root instanceof JSONArray) {
            results = (JSONArray) root;
        }
        assertNotNull("Nova home did not contain a result array", results);
        assertTrue("Nova home returned no titles", results.length() > 0);

        JSONObject movie = null;
        for (int i = 0; i < results.length(); i++) {
            JSONObject candidate = results.optJSONObject(i);
            if (candidate == null) continue;
            String id = candidate.optString("tmdbId", candidate.optString("id", ""));
            String type = candidate.optString("type", candidate.optString("mediaType", ""));
            if (!id.isEmpty() && !"show".equalsIgnoreCase(type) && !"tv".equalsIgnoreCase(type)) {
                movie = candidate;
                break;
            }
        }
        assertNotNull("Nova home returned no movie item for source smoke test", movie);

        String tmdbId = movie.optString("tmdbId", movie.optString("id", ""));
        assertFalse(tmdbId.isEmpty());

        String sourceUrl = BASE + "/api/sources?type=movie&tmdbId=" + tmdbId;
        String sourcesBody = MovieHttp.get(sourceUrl, MovieHttp.novaHeaders(sourceUrl, UUID.randomUUID().toString()));
        assertNotNull(sourcesBody);
        assertTrue("Nova source response is unexpectedly empty", sourcesBody.length() > 20);

        boolean sourceFound = containsHlsOrDash(sourcesBody);
        assertTrue("Nova source response did not contain an HLS/DASH URL", sourceFound);
    }

    private static boolean containsHlsOrDash(String body) {
        String lower = body.toLowerCase();
        return lower.contains(".m3u8") || lower.contains(".mpd");
    }
}
