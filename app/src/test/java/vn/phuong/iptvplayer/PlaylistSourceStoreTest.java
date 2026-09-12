package vn.phuong.iptvplayer;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlaylistSourceStoreTest {
    @Test public void acceptsOnlyHttpPlaylistSources() {
        assertTrue(PlaylistSourceStore.isValid("https://example.test/list.m3u"));
        assertTrue(PlaylistSourceStore.isValid("http://example.test/list"));
        assertFalse(PlaylistSourceStore.isValid("file:///sdcard/list.m3u"));
        assertFalse(PlaylistSourceStore.isValid("javascript:alert(1)"));
        assertFalse(PlaylistSourceStore.isValid(null));
    }

    @Test public void recognizesOnlyBuiltInDefaultSources() {
        assertEquals("https://phuongnm7-playlist.phuongnm7-iptv.workers.dev/", PlaylistSourceStore.DEFAULT_URL);
        assertEquals("NM7 IPTV", PlaylistSourceStore.DEFAULT_NAME);
        assertTrue(PlaylistSourceStore.isDefault(PlaylistSourceStore.DEFAULT_URL));
        assertTrue(PlaylistSourceStore.isDefault("https://iptv-live-merge.phuongnm7-iptv.workers.dev/playlist.m3u"));
        assertFalse(PlaylistSourceStore.isDefault("https://example.test/list.m3u"));
    }
}
