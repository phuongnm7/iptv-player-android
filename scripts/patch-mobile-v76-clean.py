# 1) Handle Android 15 edge-to-edge correctly using the already-existing
#    WindowInsets listener. Do NOT change player initialization or lifecycle.
# 0) Keep the pinned MediaServiceCore source compatible with the pinned
#    SharedModules interface used by the clean 1.10.75 build. Some current
#    upstream snapshots no longer expose getPlaylistId() on MediaItem.
MEDIA_ITEM = ROOT / "MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/service/data/YouTubeMediaItem.java"
media = MEDIA_ITEM.read_text(encoding="utf-8")
# Restore the interface contract used by YouTubeMediaItem before Gradle starts.
# This is safer than stripping @Override: the implementation and interface stay aligned.
INTERFACE = ROOT / "MediaServiceCore/mediaserviceinterfaces/src/main/java/com/liskovsoft/mediaserviceinterfaces/data/MediaItem.java"
interface_text = INTERFACE.read_text(encoding="utf-8")
if "String getPlaylistId();" not in interface_text:
    anchor = "    // Playlist props\n"
    if interface_text.count(anchor) != 1:
        raise SystemExit("clean-v76: MediaItem playlist anchor missing")
    interface_text = interface_text.replace(anchor, anchor + "    String getPlaylistId();\n", 1)
    INTERFACE.write_text(interface_text, encoding="utf-8")
    print("clean-v76: restored MediaItem.getPlaylistId() contract")
if "String getPlaylistId();" not in INTERFACE.read_text(encoding="utf-8"):
    raise SystemExit("clean-v76: MediaItem.getPlaylistId() contract is still missing")

# The resolved javac ABI in this Mobile build does not expose getPlaylistId()
# even though the pinned source interface may contain it. Keep the implementation
# source compatible with that ABI by removing only this annotation; the method remains.
import re
patched, count = re.subn(
    r'(?m)^\\s*@Override\\s*\\n(?=\\s*public String getPlaylistId\\()',
    '',
    media,
    count=1,
)
if count != 1:
    raise SystemExit("clean-v76: could not remove getPlaylistId @Override")
MEDIA_ITEM.write_text(patched, encoding="utf-8")
print("clean-v76: removed getPlaylistId @Override for legacy ABI compatibility")

s = PLAYBACK.read_text(encoding="utf-8")