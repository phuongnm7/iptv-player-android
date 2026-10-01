package vn.phuong.iptvplayer.movie;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NovaHdNetworkSmokeTest {
    private static final String BASE = "https://novahd.cc";

    @Test
    public void novaApiAndSourceParserSmoke() throws Exception {
        String fixture = "{\"results\":[{" +
                "\"tmdbId\":550,\"type\":\"movie\",\"title\":\"Fight Club\"," +
                "\"posterPath\":\"/poster.jpg\",\"backdropPath\":\"/backdrop.jpg\"}]}";
        List<MovieModels.MovieItem> items = MovieJson.parseList(fixture, BASE, "novahd");
        assertTrue("Nova fixture did not parse into a movie", !items.isEmpty());
        assertTrue(items.get(0).id.startsWith("movie/"));

        String ndjson = "{\"sources\":[{\"quality\":\"1080p\",\"type\":\"hls\",\"url\":\"https://cdn.example/test.m3u8\"}],"
                + "\"subtitles\":[{\"lang\":\"en\",\"url\":\"https://cdn.example/en.vtt\"}]}";
        MovieModels.Playback playback = MovieJson.parsePlayback(ndjson, "", BASE);
        assertNotNull("Nova NDJSON fixture did not produce playback", playback);
        assertTrue(playback.url.contains(".m3u8"));
    }
}
