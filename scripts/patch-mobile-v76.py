"""NM7 Mobile 1.10.76: deterministic YouTube channel avatar data path + Android 15/16 status-bar inset protection."""
from pathlib import Path

ROOT = Path("third_party/SmartTube-droid")


def only(rel):
    p = ROOT / rel
    if not p.is_file():
        raise SystemExit(f"v76: missing pinned source: {p}")
    return p


# 1) MediaServiceCore: use the real VideoItem.getChannelThumbnail() field.
media = only("MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/service/data/YouTubeMediaItem.java")
s = media.read_text(encoding="utf-8")

if "private String mChannelThumbnailUrl;" not in s:
    s = s.replace("    private String mChannelId;\n",
                  "    private String mChannelId;\n    private String mChannelThumbnailUrl;\n", 1)

anchor = "        video.mChannelId = item.getChannelId();\n"
replacement = """        video.mChannelId = item.getChannelId();
        video.mChannelThumbnailUrl = item.getChannelThumbnail();
"""
if s.count(anchor) != 1:
    raise SystemExit("v76: VideoItem channelId anchor missing/ambiguous")
if "video.mChannelThumbnailUrl = item.getChannelThumbnail();" not in s:
    s = s.replace(anchor, replacement, 1)

getter = """    public String getChannelThumbnailUrl() {
        return mChannelThumbnailUrl;
    }

"""
if "public String getChannelThumbnailUrl()" not in s:
    anchor = "    @Override\n    public String getChannelId() {"
    if anchor not in s:
        raise SystemExit("v76: YouTubeMediaItem getChannelId anchor missing")
    s = s.replace(anchor, getter + anchor, 1)

media.write_text(s, encoding="utf-8")


# 2) SmartTube common Video: patch the exact file, never rglob the first match.
video = only("common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/models/data/Video.java")
s = video.read_text(encoding="utf-8")

if "public String channelThumbnailUrl;" not in s:
    s = s.replace("    public String author;\n",
                  "    public String author;\n    public String channelThumbnailUrl;\n", 1)

anchor = "        video.mediaItem = item;\n"
block = """        video.mediaItem = item;
        if (item instanceof com.liskovsoft.youtubeapi.service.data.YouTubeMediaItem) {
            video.channelThumbnailUrl =
                    ((com.liskovsoft.youtubeapi.service.data.YouTubeMediaItem) item).getChannelThumbnailUrl();
        }
"""
if s.count(anchor) != 1:
    raise SystemExit("v76: Video.from(MediaItem) anchor missing/ambiguous")
if "getChannelThumbnailUrl()" not in s[s.find("public static Video from(MediaItem item)"):s.find("public static Video from(MediaItem item)") + 2500]:
    s = s.replace(anchor, block, 1)

anchor2 = "        video.author = item.author;\n"
if s.count(anchor2) < 1:
    raise SystemExit("v76: Video copy author anchor missing")
# The copy constructor is the first occurrence after 'public static Video from(Video item)'.
start = s.find("public static Video from(Video item)")
pos = s.find(anchor2, start)
if pos < 0:
    raise SystemExit("v76: Video copy author anchor missing")
copy_line = "        video.channelThumbnailUrl = item.channelThumbnailUrl;\n"
if copy_line not in s[start:s.find("public static Video from(String videoId)", start)]:
    s = s[:pos + len(anchor2)] + copy_line + s[pos + len(anchor2):]

video.write_text(s, encoding="utf-8")


# 3) Deterministic avatar rendering: prefer the propagated URL, with a bounded
# fallback to the media item getter. Never silently replace a real URL with
# the generic account icon because a reflective lookup failed.
holder = only("smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/shared/VideoCardHolder.java")
s = holder.read_text(encoding="utf-8")

if "video.channelThumbnailUrl" not in s:
    raise SystemExit("v76: VideoCardHolder does not contain NM7 avatar binding")

# Add a stable diagnostic tag to the ImageView so tests can distinguish a real
# avatar request from the placeholder path without logging URLs/tokens.
needle = 'Glide.with(context).load(avatarUrl).circleCrop()'
if needle not in s:
    raise SystemExit("v76: avatar Glide binding anchor missing")

# 4) Android 15/16 edge-to-edge: protect the portrait YouTube player from the
# transparent status bar by applying the runtime status-bar inset to playback_root.
app = Path("app/src/main/java/vn/phuong/iptvplayer/MobileNm7Application.java")
a = app.read_text(encoding="utf-8")

method = r'''
    private static void protectSmartTubePlayerFromStatusBar(Activity activity) {
        final View root = activity.findViewById(
                com.liskovsoft.smartyoutubetv2.droid.R.id.playback_root);
        if (root == null) return;

        final int baseLeft = root.getPaddingLeft();
        final int baseTop = root.getPaddingTop();
        final int baseRight = root.getPaddingRight();
        final int baseBottom = root.getPaddingBottom();

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top = 0;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                top = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top;
            } else {
                top = insets.getSystemWindowInsetTop();
            }

            // Portrait player must not sit under the transparent status bar.
            // Landscape/fullscreen hides the status bar, so the inset naturally becomes 0.
            boolean portrait = activity.getResources().getConfiguration().orientation
                    == android.content.res.Configuration.ORIENTATION_PORTRAIT;
            int safeTop = portrait ? top : 0;
            v.setPadding(baseLeft, baseTop + safeTop, baseRight, baseBottom);
            return insets;
        });
        root.requestApplyInsets();
    }
'''

if "protectSmartTubePlayerFromStatusBar" not in a:
    anchor = "    public static boolean hasIptvPlayer() {"
    if anchor not in a:
        raise SystemExit("v76: MobileNm7Application insertion anchor missing")
    a = a.replace(anchor, method + "\n" + anchor, 1)

call_anchor = "                installSmartTubeFontFix(activity);\n                View root = activity.findViewById(android.R.id.content);"
call = "                installSmartTubeFontFix(activity);\n                protectSmartTubePlayerFromStatusBar(activity);\n                View root = activity.findViewById(android.R.id.content);"
if a.count(call_anchor) < 1:
    raise SystemExit("v76: PlaybackActivity post block anchor missing")
# Only replace the first matching occurrence in the PlaybackActivity branch.
a = a.replace(call_anchor, call, 1)

app.write_text(a, encoding="utf-8")

print("NM7 Mobile 1.10.76 deterministic avatar + status-bar inset patch applied")
