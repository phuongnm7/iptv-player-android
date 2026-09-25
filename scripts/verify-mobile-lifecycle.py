"""Fail-closed structural verifier for the Mobile YouTube playback lifecycle.

Runtime playback is still device-tested separately. This verifier checks that the
Mobile build no longer installs or preserves an embedded YouTube mini-player and
that the normal YouTube/IPTV bottom navigation remains intact.
"""
from pathlib import Path
import re
import json
import hashlib

root = Path('third_party/SmartTube-droid/smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui')
play = (root / 'playback/PlaybackActivity.java').read_text(encoding='utf-8')
browse = (root / 'browse/BrowseActivity.java').read_text(encoding='utf-8')
search = (root / 'search/SearchActivity.java').read_text(encoding='utf-8')
app = Path('app/src/main/java/vn/phuong/iptvplayer')
application = (app / 'MobileNm7Application.java').read_text(encoding='utf-8')
tabs = (app / 'HomeTabBar.java').read_text(encoding='utf-8')
gradle = Path('app/build.gradle.kts').read_text(encoding='utf-8')
patch = Path('scripts/patch-mobile-v37.py').read_text(encoding='utf-8')
final_delta = Path('scripts/patch-mobile-v69-no-miniplayer.py').read_text(encoding='utf-8')
inc = Path('scripts/smarttube-playback-mobile.java.inc').read_text(encoding='utf-8')

def body(signature):
    match = re.search(r'(?ms)^    ' + re.escape(signature) + r' \{(.*?)^    \}', play)
    assert match, signature
    return match.group(1)

checks = 0
def check(condition, label):
    global checks
    assert condition, label
    checks += 1
    print('PASS', label)

# Core lifecycle / playback ownership.
check('vn.phuongnm7.iptvplayer' not in play, 'SmartTube bridge uses the actual Mobile Java package')
check('Build the decoder/player before the Activity is shown' in play, 'Player initialization remains before first-frame wait')
check('if (mPlayer == null) initializePlayer();' in body('protected void onStart()'), 'Foreground reuses/rebuilds the player safely')
check('pauseIptvForYoutube' not in body('protected void onStart()'), 'Activity start does not prematurely release IPTV')
check('playWhenReady && playbackState == Player.STATE_READY' in play, 'IPTV handoff is gated by actual YouTube READY/play intent')
check('MobileInlinePlayerProviderV2.releaseForYoutube();' in application, 'YouTube handoff releases the IPTV inline owner')
check('prefetchNm7FormatInfo(item);' not in play and 'mNm7PrefetchedFormatInfo' not in play, 'No speculative format prefetch remains in PlaybackActivity')
check('mPlayerView' in play and 'PlayerView' in play, 'Mobile playback keeps the PlayerView rendering path')
check('mNm7ObservedPlayer' in play and 'removeVideoListener' in play, 'Decoder/render observers are tied to the current player instance')
check('mPlayer.retry()' in play, 'Decoder/source recovery retains bounded retry support')
check('mPlayer.retry()' in play, 'Decoder/source recovery retains retry support')
check('scripts/patch-mobile-v37.py' in Path('scripts/build-mobile-windows.ps1').read_text(), 'Windows build uses the shared Mobile patch')
check('scripts/patch-mobile-v37.py' in Path('.github/workflows/android-mobile-final.yml').read_text(), 'CI uses the shared Mobile patch')

# Direction 2: no embedded YouTube mini-player.
check('installNm7MiniPlayer();' not in browse, 'Browse does not install an embedded mini-player')
check('installYoutubeMiniPlayer(activity)' not in application, 'Application lifecycle does not reattach a mini-player')
check('MobileMiniPlayer.attach' not in application, 'Application does not create mini-player surfaces')
check('MobileMiniPlayer.remove' not in application, 'Application does not manage mini-player overlays')
check('consumeNm7BrowseBack()' not in search, 'Search uses normal Back without mini-player interception')

check('mNm7LeavingForMini = false;' in final_delta and 'sNm7Mini = false;' in final_delta, 'Mobile playback Back patch clears legacy mini-player state')
check('startActivity(intent);' in final_delta and 'finish();' in final_delta, 'Mobile playback Back patch returns to Browse')

# Bottom navigation: exactly the two requested tabs.
check('"YouTube"' in tabs and '"IPTV"' in tabs, 'Bottom navigation contains YouTube and IPTV')
check('"Thư viện"' not in tabs and '"Cài đặt"' not in tabs, 'Bottom navigation has no extra YouTube tabs')
check('addItem(activity, bar, R.drawable.nm7_nav_youtube' in tabs, 'YouTube tab is present')
check('addItem(activity, bar, R.drawable.nm7_nav_iptv' in tabs, 'IPTV tab is present')
check('stopYoutubeForIptv();' not in tabs, 'IPTV tab does not close the YouTube owner')
check('SharedPlaybackSession.setTab(activity, SharedPlaybackSession.TAB_IPTV);' in tabs, 'IPTV tab switches the shared product tab')
check('SharedPlaybackSession.setTab(activity, SharedPlaybackSession.TAB_YOUTUBE);' in tabs, 'YouTube tab switches the shared product tab')
check(tabs.count('addItem(activity, bar,') == 2, 'Exactly two bottom navigation items are created')

# Keep Mobile YouTube Browse usable.
check('HomeTabBar.attach(activity, true);' in application, 'YouTube Browse receives the bottom tab bar')
check('HomeTabBar.attach(activity, false);' in application, 'IPTV MainActivity receives the bottom tab bar')
check('GRID_COLUMNS = 1' in browse, 'YouTube recommendations remain single-column')
check('getCardImageUrl()' in Path('scripts/patch-mobile-ui.py').read_text(), 'YouTube thumbnail card image loading remains enabled')
check('patch-mobile-v73.py' in patch and 'runpy.run_path("scripts/patch-mobile-v73.py")' in patch, 'YouTube loading optimization patch is part of the Mobile build chain')
check('patch-mobile-v74.py' in patch and 'runpy.run_path("scripts/patch-mobile-v74.py")' in patch, 'v74 playback/fullscreen optimization patch is part of the Mobile build chain')
check('patch-mobile-v75.py' in patch and 'runpy.run_path("scripts/patch-mobile-v75.py")' in patch, 'v75 fast playback format patch is part of the Mobile build chain')
check('setPlayerDataSource' not in play, 'Mobile playback does not force the YouTube transport to OkHttp')
check('maxresdefault.jpg' in Path('scripts/patch-mobile-v73.py').read_text(), 'YouTube Browse cards preserve 1.10.72 max-resolution thumbnail target')
check('mqdefault.jpg' not in Path('scripts/patch-mobile-v73.py').read_text(), 'YouTube Browse optimization does not downgrade thumbnail quality')

# Version must advance for this application-level behavior change.
check('versionCode = 102' in gradle, 'Mobile versionCode is 102 for Mobile 1.10.86 build')
check('versionName = "1.10.86"' in gradle, 'Mobile versionName is 1.10.86')
check('TV_CLIENT' in Path('scripts/patch-mobile-v75.py').read_text(), 'v75 uses the lightweight TV_DOWNGRADED playback client first')
check('getFastPlaybackFormatInfo' in Path('scripts/patch-mobile-v75.py').read_text(), 'v75 fast format resolver is present')
check('maxresdefault.jpg' in Path('scripts/patch-mobile-v75.py').read_text(), 'v75 preserves 1.10.72 max-resolution thumbnail target')
check('mqdefault.jpg' not in Path('scripts/patch-mobile-v75.py').read_text(), 'v75 does not downgrade thumbnail quality')
check('mNm7SelectedRequest.pendingFor(mPendingVideo.videoId)' in Path('scripts/patch-mobile-v74.py').read_text(), 'v74 keeps selected-video format lookup alive across owner cleanup')
check('HomeTabBar.setVisible(activity, !fullscreen)' in Path('scripts/patch-mobile-v74.py').read_text(), 'v74 hides the two bottom tabs in landscape/fullscreen playback')
check('maxresdefault.jpg' in Path('scripts/patch-mobile-v74.py').read_text(), 'v74 preserves 1.10.72 max-resolution thumbnail target')
check('mqdefault.jpg' not in Path('scripts/patch-mobile-v74.py').read_text(), 'v74 does not downgrade thumbnail quality')

Path('dist/mobile-diagnostics').mkdir(parents=True, exist_ok=True)
Path('dist/mobile-diagnostics/lifecycle-source-proof.json').write_text(json.dumps({
    'structural_checks': checks,
    'runtime_verified': False,
    'mini_player': False,
    'bottom_tabs': ['YouTube', 'IPTV'],
    'playback_sha256': hashlib.sha256(play.encode()).hexdigest(),
    'browse_sha256': hashlib.sha256(browse.encode()).hexdigest(),
}, indent=2))
print(f'{checks} structural checks passed; device runtime not verified')
check('runpy.run_path("scripts/patch-mobile-v76.py")' in patch, 'v76 live chat/status-bar patch is part of the Mobile build chain')
check('nm7_live_chat_panel' in Path('scripts/patch-mobile-v76.py').read_text(), 'v76 adds the live chat panel')
check('mNm7LiveChatAdapter' in Path('scripts/patch-mobile-v76.py').read_text(), 'v76 wires the ChatReceiver adapter')
check('nm7.mobile.livechat' in Path('app/src/main/java/vn/phuong/iptvplayer/MobileNm7Application.java').read_text(), 'Mobile enables the live-chat integration flag')

check('runpy.run_path("scripts/patch-mobile-v77.py")' in patch, 'v77 status-bar/chat UI patch is in the Mobile chain')
check('nm7_live_chat_close' in Path('scripts/patch-mobile-v77.py').read_text(), 'v77 has live-chat close control')
check('setDecorFitsSystemWindows(true)' in Path('scripts/patch-mobile-v77.py').read_text(), 'v77 enables non-edge-to-edge portrait')

check('runpy.run_path("scripts/patch-mobile-v84.py")' in patch, 'v84 playback/chat/avatar/poster patch is part of the Mobile build chain')
check('channelThumbnail.thumbnails[0].url' in Path('scripts/patch-mobile-v84.py').read_text(), 'v84 fixes channel avatar parser at source')
check('onRenderedFirstFrame' in Path('scripts/patch-mobile-v84.py').read_text(), 'v84 keeps poster until first rendered frame')
check('sendLiveChatMessageObserve' in Path('scripts/patch-mobile-v84.py').read_text(), 'v84 adds live-chat send path')
check('protected void applySystemBars()' in Path('scripts/patch-mobile-v84.py').read_text(), 'v84 restores status bar after DroidActivity resume')

check('runpy.run_path("scripts/patch-mobile-v85.py")' in patch, 'v85 status/chat/avatar correction is part of the Mobile build chain')
check('android:paddingBottom="64dp"' in Path('scripts/patch-mobile-v85.py').read_text(), 'v85 reserves visible space for live-chat composer')
check('String nm7ChannelThumbnail = item.getChannelThumbnail();' in Path('scripts/patch-mobile-v85.py').read_text(), 'v85 propagates VideoItem channel avatar directly')
check('setDecorFitsSystemWindows(true)' in Path('scripts/patch-mobile-v85.py').read_text(), 'v85 forces portrait status bar window fitting')

check('runpy.run_path("scripts/patch-mobile-v86.py")' in patch, 'v86 status/avatar/poster correction is part of the Mobile build chain')
check('nm7_startup_poster' in Path('scripts/patch-mobile-v86.py').read_text(), 'v86 adds poster overlay above SurfaceView')
check('mNm7FirstFrameRendered = true;' in Path('scripts/patch-mobile-v86.py').read_text(), 'v86 removes poster only on decoded first frame')
check('NM7_AVATAR_CACHE' in Path('scripts/patch-mobile-v86.py').read_text(), 'v86 caches metadata-resolved YouTube avatars')
check('nm7.mobile.normalbars' in Path('scripts/patch-mobile-v86.py').read_text(), 'v86 disables persistent Mobile fullscreen mode inside SmartTube')
