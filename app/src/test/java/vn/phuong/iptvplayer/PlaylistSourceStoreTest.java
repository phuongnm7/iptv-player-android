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
}
