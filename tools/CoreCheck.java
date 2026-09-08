import vn.phuong.iptvplayer.Channel;
import vn.phuong.iptvplayer.M3uParser;
import java.util.Collections;

/** Lightweight real-Java checks; does not replace Android build/device tests. */
public final class CoreCheck {
    private static int assertions;
    public static void main(String[] args) {
        M3uParser parser = new M3uParser();
        M3uParser.Result r = parser.parse("\uFEFF#EXTM3U\r\n#EXTINF:-1 group-title=\"A, B\",Name, One\r\n"
                + "#EXTVLCOPT:http-user-agent=Demo\r\n"
                + "../ch/live.m3u8|Origin=https%3A%2F%2Forigin.test&Cookie=a%3Db%2Bc\r\n",
                "https://example.test/list/channels.m3u");
        eq(1, r.channels.size());
        Channel c = r.channels.get(0);
        eq("Name, One", c.name());
        eq("A, B", c.group());
        eq("https://example.test/ch/live.m3u8", c.url());
        eq("Demo", c.headers().get("User-Agent"));
        eq("a=b+c", c.headers().get("Cookie"));
        Channel roundTrip = parser.parse(parser.export(r.channels), "").channels.get(0);
        eq(c.headers(), roundTrip.headers());
        eq(c.name(), roundTrip.name());
        eq(c.group(), roundTrip.group());
        eq(1, parser.parse("https://example.test/x\nhttps://example.test/x", "").duplicateCount);
        eq(4, parser.parse("https://x.test/A?key=Ab\nhttps://x.test/a?key=Ab\n"
                + "https://x.test/A?key=ab\nhttps://x.test/A?key=Ab|User-Agent=Other\n", "").channels.size());
        r = parser.parse("#EXTM3U\n#EXTINF:-1,A\n#EXTINF:-1,B\nnot a url\n", "");
        eq(0, r.channels.size()); eq(2, r.missingUrlCount);
        r = parser.parse("#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXTINF:6,\nseg1.ts\n",
                "https://x.test/hls");
        eq(1, r.channels.size()); eq("https://x.test/hls", r.channels.get(0).url());
        eq("application/x-mpegURL", r.channels.get(0).mimeHint());
        r = parser.parse("#EXTM3U\n#EXTINF:-1,DRM\n"
                + "#KODIPROP:inputstream.adaptive.license_type=com.widevine.alpha\n"
                + "https://example.test/ch.mpd\n", "");
        eq(true, r.channels.get(0).needsDrm());
        eq(true, parser.parse(parser.export(r.channels), "").channels.get(0).needsDrm());
        r.channels.get(0).setSelected(false);
        eq("#EXTM3U\n", parser.export(r.channels));
        eq(1, parser.parse(parser.exportAll(r.channels), "").channels.size());
        eq(3, parser.parse("rtsp://192.0.2.1/cam\rudp://239.1.1.1:1234\rrtmp://example.test/live", "").channels.size());
        try { parser.parse("<html>Login</html>", "https://x.test"); throw new AssertionError("Accepted HTML"); }
        catch (IllegalArgumentException expected) { assertions++; }
        try { parser.parse("#EXTM3U\n#EXT-X-VERSION:3", ""); throw new AssertionError("Accepted local HLS"); }
        catch (IllegalArgumentException expected) { assertions++; }
        eq("#EXTM3U\n", parser.export(Collections.emptyList()));
        System.out.println("PASS: " + assertions + " core assertions");
    }
    private static void eq(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual))
            throw new AssertionError("Expected " + expected + " but got " + actual);
        assertions++;
    }
}
