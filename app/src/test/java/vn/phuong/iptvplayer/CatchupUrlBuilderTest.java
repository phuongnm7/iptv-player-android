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

    @Test public void offsetTokenUsesPositiveDuration() {
        String result = CatchupUrlBuilder.build(
                "https://live.test/vtv3.m3u8",
                "append",
                "https://archive.test/vtv3/start-$"+"{start}-$"+"{offset}.m3u8",
                1700000000L,
                1700003600L);
        assertEquals("https://archive.test/vtv3/start-1700000000-3600.m3u8", result);
    }

    @Test public void vtvStyleMpdReplacesStartAndStop() {
        String result = CatchupUrlBuilder.build(
                "https://live.test/vtv1/manifest.mpd",
                "default",
                "https://archive.test/VTV1_HD/manifest.mpd?startTime=$"+"{start}&stopTime=$"+"{stop}",
                1700000000L,
                1700003600L);
        assertEquals("https://archive.test/VTV1_HD/manifest.mpd?startTime=1700000000&stopTime=1700003600", result);
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
