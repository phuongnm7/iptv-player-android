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
check('versionCode = 93' in gradle, 'Mobile versionCode bumped for Mobile 1.10.75 build')
check('versionName = "1.10.75"' in gradle, 'Mobile versionName is 1.10.75')
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
\ncheck('runpy.run_path(\"scripts/patch-mobile-v76.py\")' in patch, 'v76 live chat/status-bar patch is part of the Mobile build chain')\n