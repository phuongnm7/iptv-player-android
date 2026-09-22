"""Mobile 1.10.75: fast YouTube playback format path.

The user test still showed ~9 seconds from selection to first video frame in 1.10.74.
The bottleneck is upstream format resolution, not the ExoPlayer renderer: the pinned
MediaServiceCore first requests WEB_EMBED, which requires a web PoToken and can then
perform optional secondary HLS/subtitle requests before returning format info.

For normal playback we use the lightweight TV_DOWNGRADED client first. It does not
require a web PoToken. If it cannot produce a playable result, the original SmartTube
multi-client fallback remains intact. Extended-HLS users keep the original path.
"""
from pathlib import Path

root = Path("third_party/SmartTube-droid")
matches = list(root.rglob("YouTubeMediaItemService.java"))
if len(matches) != 1:
    raise SystemExit(f"v75: expected exactly one YouTubeMediaItemService.java, found {len(matches)}")
p = matches[0]
s = p.read_text(encoding="utf-8")

anchor = '''    @Override
    public MediaItemFormatInfo getFormatInfo(String videoId, String clickTrackingParams) {
        return selectPlaybackFormatInfo(videoId, clickTrackingParams);
    }
'''
replacement = '''    @Override
    public MediaItemFormatInfo getFormatInfo(String videoId, String clickTrackingParams) {
        // NM7 1.10.75: normal playback must not wait for WEB_EMBED + web PoToken
        // before ExoPlayer can receive a playable format. Keep the original path
        // available for extended-HLS and as a fallback for restricted/problematic
        // videos.
        if (!getData().isFormatEnabled(MediaServiceData.FORMATS_EXTENDED_HLS)) {
            MediaItemFormatInfo fast = getFastPlaybackFormatInfo(videoId, clickTrackingParams);
            if (fast != null && !fast.isUnplayable()) {
                return fast;
            }
        }
        return selectPlaybackFormatInfo(videoId, clickTrackingParams);
    }

    private MediaItemFormatInfo getFastPlaybackFormatInfo(String videoId, String clickTrackingParams) {
        if (videoId == null) {
            return null;
        }

        MediaItemFormatInfo cachedFormatInfo = getCachedFormatInfo(videoId);
        if (cachedFormatInfo != null) {
            return cachedFormatInfo;
        }

        checkSigned();
        mUseAuth = true;

        // TV_DOWNGRADED is intentionally used here because it avoids the WEB PoToken
        // path while still returning adaptive playback formats for normal videos.
        VideoInfo videoInfo = getVideoInfo(TV_CLIENT, videoId, clickTrackingParams);
        if (videoInfo == null || videoInfo.isUnplayable()) {
            return videoInfo == null ? null : YouTubeMediaItemFormatInfo.from(videoInfo);
        }

        transformFormats(videoInfo);
        MediaItemFormatInfo formatInfo = YouTubeMediaItemFormatInfo.from(videoInfo);
        setCachedFormatInfo(formatInfo, clickTrackingParams);
        mIsUnplayable = false;
        return formatInfo;
    }
'''
if s.count(anchor) != 1:
    raise SystemExit("v75: getFormatInfo anchor missing/ambiguous")
s = s.replace(anchor, replacement, 1)
p.write_text(s, encoding="utf-8")

# Preserve thumbnail quality exactly.
ui = Path("scripts/patch-mobile-ui.py")
if "maxresdefault.jpg" not in ui.read_text(encoding="utf-8"):
    raise SystemExit("v75: maxresdefault thumbnail target missing")
print("NM7 Mobile 1.10.75 fast playback format path applied")
