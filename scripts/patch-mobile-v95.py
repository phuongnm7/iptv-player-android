"""NM7 Mobile 1.10.95: match the reference YouTube loading handoff.

Reference video behavior:
- when a new video is selected, the previous video frame disappears immediately;
- the player area is black while the new media is loading;
- the selected video's thumbnail appears only when it is actually available;
- playback then replaces the thumbnail.

The 1.10.94 poster-position gate was not enough because the underlying
SurfaceView could keep the previous frame visible. Use PlayerView's native
shutter/reset contract instead of trying to cover a SurfaceView with another
ImageView. Also disable the synchronous clicked-card poster so the sequence
matches the reference video rather than showing a stale/early frame.
"""
from pathlib import Path

ROOT = Path("third_party/SmartTube-droid")
PHONE = ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui"
PLAYBACK = PHONE / "playback/PlaybackActivity.java"
LAYOUT = ROOT / "smarttubedroid/src/main/res/layout/playback_activity.xml"

def once(s, old, new, label):
    if s.count(old) != 1:
        raise SystemExit(f"v95: expected exactly one {label}, found {s.count(old)}")
    return s.replace(old, new, 1)

# ---------------------------------------------------------------------------
# 1) Use the PlayerView shutter to clear the old SurfaceView frame on media reset.
# This is the native ExoPlayer mechanism for preventing a previous frame from
# remaining visible while the next media item is prepared.
xml = LAYOUT.read_text(encoding="utf-8")
old_view = '''            app:resize_mode="fit"
            app:surface_type="surface_view"
            app:use_controller="false" />'''
new_view = '''            app:resize_mode="fit"
            app:surface_type="surface_view"
            app:keep_content_on_player_reset="false"
            app:shutter_background_color="@android:color/black"
            app:use_controller="false" />'''
xml = once(xml, old_view, new_view, "PlayerView reset/shutter attributes")
LAYOUT.write_text(xml, encoding="utf-8")

# ---------------------------------------------------------------------------
# 2) The reference video does not show the tapped feed card as an immediate
# transition poster. Let the normal async poster path reveal the selected
# video's own thumbnail after the player has reset to black.
s = PLAYBACK.read_text(encoding="utf-8")
old_transition = '''        android.graphics.Bitmap nm7TransitionPoster = item == null ? null :
                com.liskovsoft.smartyoutubetv2.droid.ui.shared.VideoCardHolder
                        .consumeNm7TransitionPoster(item.videoId);'''
s = once(s, old_transition, '''        // NM7 1.10.95: do not use the synchronous clicked-card poster.
        // The reference flow is black shutter -> selected thumbnail -> playback.
        android.graphics.Bitmap nm7TransitionPoster = null;''', "clicked-card poster source disable")

# Keep the existing Glide fallback intact; it now becomes the only poster path.
PLAYBACK.write_text(s, encoding="utf-8")

# ---------------------------------------------------------------------------
# 3) Show the existing loading indicator immediately after selecting a new video.
# It is hidden by the existing poster/first-frame lifecycle once playback is ready.
anchor = '''    public void setVideo(Video item) {
        nm7CancelPosterReadyFallback();'''
repl = '''    public void setVideo(Video item) {
        nm7CancelPosterReadyFallback();
        if (mProgressBar != null) mProgressBar.setVisibility(View.VISIBLE);'''
s = once(s, anchor, repl, "setVideo loading indicator")
PLAYBACK.write_text(s, encoding="utf-8")

print("NM7 Mobile 1.10.95 reference-style YouTube shutter/loading handoff applied")
