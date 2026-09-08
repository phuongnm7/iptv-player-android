package vn.phuong.iptvplayer;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class DrmAndFpsTest {
    @Test public void parsesWidevineLicenseAndHeaders() {
        DrmSpec spec = DrmSpec.fromOptions(Arrays.asList(
                "#KODIPROP:inputstream.adaptive.license_type=com.widevine.alpha",
                "#KODIPROP:inputstream.adaptive.license_key=https://license.test/wv|Authorization=Bearer%20abc|R{SSM}|"));
        assertEquals("widevine", spec.system);
        assertEquals("https://license.test/wv", spec.license);
        assertEquals("Bearer abc", spec.headers.get("Authorization"));
        assertEquals("", spec.problem);
    }

    @Test public void clearKeyAndIncompleteDrmAreReportedWithoutEchoingSecrets() {
        String key = "00112233445566778899aabbccddeeff:ffeeddccbbaa99887766554433221100";
        assertTrue(DrmSpec.create("clearkey", key).localClearKey());
        DrmSpec missing = DrmSpec.create("widevine", "");
        assertFalse(missing.problem.isEmpty());
        assertFalse(missing.problem.contains(key));
        assertEquals("unknown", DrmSpec.normalizeSystem("made-up-drm"));
    }

    @Test public void rejectsUnsafeLicenseHeadersAndCustomTemplates() {
        assertFalse(DrmSpec.create("widevine", "https://license.test|Bad%0AName=x").problem.isEmpty());
        assertFalse(DrmSpec.create("widevine", "https://license.test||CUSTOM{SSM}|").problem.isEmpty());
    }

    @Test public void fpsUsesRenderedFrameDeltaAndWallClock() {
        FpsMeter meter = new FpsMeter();
        assertTrue(Double.isNaN(meter.sample(1000, 10, true)));
        assertEquals(30.0, meter.sample(3000, 70, true), 0.001);
        assertEquals(0.0, meter.sample(4000, 70, false), 0.001);
        assertTrue(Double.isNaN(meter.sample(5000, 90, true)));
    }
}
