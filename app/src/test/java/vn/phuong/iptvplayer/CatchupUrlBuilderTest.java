package vn.phuong.iptvplayer;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CatchupUrlBuilderTest {
    @Test public void absoluteArchiveTemplateReplacesStart() {
        String result = CatchupUrlBuilder.build(
                "https://live.test/vtv3.m3u8",
                "append",
                "https://archive.test/vtv3/start-$"+"{start}-10800.m3u8",
                1700000000L,
                1700003600L);
        assertEquals("https://archive.test/vtv3/start-1700000000-10800.m3u8", result);
    }

    @Test public void appendQueryUsesLiveUrl() {
        String result = CatchupUrlBuilder.build(
                "https://live.test/vtv3.m3u8?token=abc",
                "append",
                "&start=$"+"{start}",
                1700000000L,
                1700003600L);
        assertEquals("https://live.test/vtv3.m3u8?token=abc&start=1700000000", result);
    }

    @Test public void shiftBuildsUtcAndLutc() {
        String result = CatchupUrlBuilder.build(
                "https://live.test/vtv3.m3u8",
                "shift",
                "",
                1700000000L,
                1700003600L);
        assertEquals("https://live.test/vtv3.m3u8?utc=1700000000&lutc=1700003600", result);
    }
}
