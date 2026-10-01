package vn.phuong.iptvplayer.movie;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
import vn.phuong.iptvplayer.movie.MovieModels.MovieDetail;
import vn.phuong.iptvplayer.movie.MovieModels.MovieItem;

public class MovieJsonTest {
    @Test public void parseNovaListShape() {
        String json="{\"results\":[{\"tmdbId\":101,\"title\":\"Test Movie\",\"posterPath\":\"/p.jpg\",\"backdropPath\":\"/b.jpg\",\"releaseDate\":\"2025-01-02\",\"voteAverage\":8.1,\"type\":\"movie\"}]}";
        List<MovieItem> items=MovieJson.parseList(json,"https://novahd.cc","novahd");
        assertEquals(1,items.size());
        assertEquals("Test Movie",items.get(0).title);
        assertEquals("movie/101",items.get(0).id);
        assertEquals("https://image.tmdb.org/t/p/w500/p.jpg",items.get(0).posterUrl);
        assertFalse(items.get(0).show);
    }

    @Test public void parseNovaDetailCreatesEpisodes() {
        MovieItem seed=new MovieItem("show/99","Demo","","","","2025","novahd",true,"https://novahd.cc/api/shows/99");
        String json="{\"tmdbId\":99,\"name\":\"Demo\",\"firstAirDate\":\"2025-01-02\",\"overview\":\"Plot\",\"seasons\":[{\"seasonNumber\":1,\"name\":\"Season 1\",\"episodes\":[{\"episodeNumber\":1,\"title\":\"Pilot\"},{\"episodeNumber\":2,\"title\":\"Second\"}]}]}";
        MovieDetail d=MovieJson.parseDetail(json,"https://novahd.cc",seed);
        assertEquals("Demo",d.movie.title);
        assertEquals(2,d.episodes.size());
        assertEquals(1,d.episodes.get(0).season);
        assertEquals(2,d.episodes.get(1).episode);
        assertTrue(d.episodes.get(0).id.contains("/api/watch/"));
    }

    @Test public void parsePlaybackFindsHlsAndHeaders() {
        String json="{\"sources\":[{\"quality\":\"720p\",\"url\":\"https://cdn/a.m3u8\"},{\"quality\":\"1080p\",\"url\":\"https://cdn/b.m3u8\"}],\"headers\":{\"Referer\":\"https://example.com/\"},\"subtitles\":[{\"url\":\"https://cdn/vi.vtt\",\"lang\":\"vi\",\"label\":\"Tiếng Việt\"}]}";
        MovieModels.Playback p=MovieJson.parsePlayback(json,"https://novahd.cc/api/sources","https://novahd.cc");
        assertNotNull(p);
        assertEquals("https://cdn/b.m3u8",p.url);
        assertEquals("https://example.com/",p.headers.get("Referer"));
        assertEquals(1,p.subtitles.size());
        assertEquals("vi",p.subtitles.get(0).lang);
    }
}