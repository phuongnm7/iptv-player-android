"""Structural regressions, not a replacement for device playback tests."""
from pathlib import Path
import re
import json
import hashlib
import xml.etree.ElementTree as ET

root = Path('third_party/SmartTube-droid/smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui')
play = (root / 'playback/PlaybackActivity.java').read_text(encoding='utf-8')
browse = (root / 'browse/BrowseActivity.java').read_text(encoding='utf-8')
app = Path('app/src/main/java/vn/phuong/iptvplayer')
inline = (app / 'MobileInlinePlayerProviderV2.java').read_text(encoding='utf-8')
application = (app / 'MobileNm7Application.java').read_text(encoding='utf-8')

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

check('vn.phuongnm7.iptvplayer' not in play, 'Bridge uses actual Java package')
check('pauseIptvForYoutube' not in body('protected void onStart()'), 'Activity start does not release IPTV')
check('Build the decoder/player before the Activity is shown' in play, 'YouTube player prewarms before first frame')
check('if (mPlayer == null) initializePlayer();' in body('protected void onStart()'), 'Resume reuses player')
check('playWhenReady && playbackState == Player.STATE_READY' in play, 'Handoff gated by ready and playing intent')
check('MobileInlinePlayerProviderV2.releaseForYoutube();' in application, 'Handoff reaches inline owner')
check('TAB_YOUTUBE.equals(SharedPlaybackSession.tab(a))' in inline, 'Inline Browse navigation guard')
check('++owner.playGeneration' in inline, 'Cancel stale inline retries on handoff')
check('maybeReleasePlayer' not in body('protected void onPause()'), 'No Android 6 pause release')
check('if (mNm7Stopped || isFinishing())' in body('protected void onStop()'), 'Background and mini session preserved')
check('nm7SetBackground(true)' in body('public void onUserLeaveHint()'), 'Existing Home callback actually replaced')
check('sNm7Mini = true;' in body('public void onBackPressed()'), 'Back enters mini state')
check('registerOnBackInvokedCallback' in play, 'Modern Back callback registered')
overlay = (app / 'MobileMiniPlayer.java').read_text(encoding='utf-8')
check('installNm7MiniPlayer();' in browse and 'vn.phuong.iptvplayer.MobileMiniPlayer' in browse, 'Browse uses shared mini surface')
check('"restoreNm7Player"' in overlay, 'Mini click restores playback')
check('suspendForNm7Iptv' in play, 'YouTube session can be suspended for IPTV without finish')
check('sNm7Mini = true;' in body('public static void suspendForNm7Iptv()'), 'IPTV suspend preserves mini state')
check('finishReally()' not in body('public static void suspendForNm7Iptv()'), 'IPTV suspend does not destroy YouTube player')
check('suspendYoutubeForIptv();' in Path('app/src/main/java/vn/phuong/iptvplayer/PlayerActivity.java').read_text(), 'IPTV READY suspends instead of closes YouTube')
check('SMARTTUBE_BROWSE.equals(name)' in application and 'installYoutubeMiniPlayer(activity)' in application, 'Browse resume reattaches preserved mini player')
check('EXO_PLAYER_VIEW' in overlay and 'createPlayerView' in overlay, 'Mini uses ExoPlayer PlayerView target')
check('switchTargetView' in body('public static void attachNm7MiniPlayer(android.view.View previousSurface, android.view.View nextSurface)'), 'Mini uses supported ExoPlayer target switching')
check('clearVideoSurface()' not in body('public static void attachNm7MiniPlayer(android.view.View previousSurface, android.view.View nextSurface)'), 'Mini target switch does not clear surface manually')
check('seekTo(' not in body('public static void attachNm7MiniPlayer(android.view.View previousSurface, android.view.View nextSurface)'), 'Mini target switch does not force seek/rebuffer')
for path in ['app/src/main/AndroidManifest.xml', 'smarttube/src/main/AndroidManifest.xml']:
    manifest = ET.parse(path)
    ns = '{http://schemas.android.com/apk/res/android}'
    for activity in manifest.findall('.//activity'):
        if activity.get(ns+'name', '').endswith(('.browse.BrowseActivity', '.playback.PlaybackActivity')):
            check(activity.get(ns+'launchMode') == 'singleTop', path + ': no singleTask stack destruction')
check('scripts/patch-mobile-v37.py' in Path('scripts/build-mobile-windows.ps1').read_text(), 'Windows shared patch')
check('scripts/patch-mobile-v37.py' in Path('.github/workflows/android-mobile-final.yml').read_text(), 'CI shared patch')

inline_text = Path('app/src/main/java/vn/phuong/iptvplayer/MobileInlinePlayerProviderV2.java').read_text()
check('stopYoutubeForIptv();' not in inline_text, 'Inline IPTV never destroys preserved YouTube session')
check(inline_text.count('suspendYoutubeForIptv();') >= 2, 'Inline IPTV READY paths suspend YouTube')
check('sNm7ResumeAfterIptv' in play and 'setPlayWhenReady(true)' in body('public static void attachNm7MiniPlayer(android.view.View previousSurface, android.view.View nextSurface)'), 'Mini resumes only after supported target switch')
loader = Path('third_party/SmartTube-droid/common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/models/playback/controllers/VideoLoaderController.java').read_text()
check('prefetchNm7FormatInfo(item);' in loader, 'YouTube format lookup overlaps engine initialization')
check('mNm7PrefetchedFormatInfo' in loader, 'Prefetched YouTube format result is reused')

pause_body = body('protected void onPause()')
back_body = body('public void onBackPressed()')
check('if (sNm7Mini' in pause_body and 'blockEngine(false)' in pause_body, 'Mini lifecycle keeps decoder/renderers active while PlaybackActivity pauses')
check('sNm7Mini = true;' in back_body and 'blockEngine(false)' in back_body, 'Back enters mini without blocking video engine')
check('tapShield' in overlay and 'tapShield.setClickable(true)' in overlay, 'Mini has dedicated touch shield above PlayerView')
check('video.setClickable(false)' in overlay, 'Underlying PlayerView cannot steal mini restore taps')

home_tabs = Path('app/src/main/java/vn/phuong/iptvplayer/HomeTabBar.java').read_text()
check('BAR_TAG' in home_tabs and 'setVisible(Activity activity, boolean visible)' in home_tabs, 'Bottom tab bar exposes fullscreen visibility control')
check('HomeTabBar.setVisible(currentActivity, false)' in inline_text, 'IPTV fullscreen hides YouTube/IPTV tab bar')
check('HomeTabBar.setVisible(currentActivity, true)' in inline_text, 'IPTV fullscreen exit restores YouTube/IPTV tab bar')

check('surfaceView()' in overlay, 'Mini exposes current PlayerView for reverse target switch')
check('switchTargetView' in body('private void completeNm7RestoreOnResume()'), 'Restore uses supported ExoPlayer reverse target switch after PlaybackActivity resumes')

check('sNm7RestorePending' in play, 'YouTube mini restore has explicit pending state')
check('completeNm7RestoreOnResume()' in play, 'Mini-to-player surface handoff is deferred until PlaybackActivity resumes')
check('consumeNm7BrowseBack()' in play, 'Browse Back can close active mini session instead of reopening player')
ui_patch = Path('scripts/patch-mobile-ui.py').read_text()
patch_script = Path('scripts/patch-mobile-v37.py').read_text()
check('consumeNm7BrowseBack()' in ui_patch, 'Browse Back consumes the second Back when mini is visible')
check('hq720.jpg' in ui_patch and 'PREFER_ARGB_8888' in ui_patch and 'DownsampleStrategy.NONE' in ui_patch, 'YouTube cards prefer high-resolution thumbnail decode/fallback')

check("phone / 'search/SearchActivity.java'" in patch_script and 'Search Back mini close failed' in patch_script, 'SearchActivity Back closes active mini before normal back stack')
check('if (video.videoId != null' in ui_patch and 'itemView.findViewById(R.id.nm7_card_menu) != null && video.videoId' not in ui_patch, 'High-resolution thumbnail path applies to Search/grid cards too')


check('if (sNm7RestorePending)' not in body('protected void onResume()'), 'Every foreground entry restores target, not just mini taps')
check('mNm7LeavingForMini = false' in body('protected void onResume()'), 'Foreground entry resets Back transition debounce')
check('if (sNm7Mini && !sNm7RestorePending) return' not in back_body, 'Stale mini flag cannot swallow foreground Back')
check('mNm7VideoTarget' in play, 'Player owner tracks actual video target across Activities')
check('sNm7Mini = false;' in body('private void completeNm7RestoreOnResume()'), 'Foreground normalization always clears mini state')
check('installSmartTubeBackHandling(activity);' in application, 'Android 13 Browse/Search Back callback installed')
check('texture_view' in Path('app/src/main/res/layout/nm7_mini_player.xml').read_text(), 'Mini inflates texture-backed legacy PlayerView')
check('app:surface_type="texture_view"' in Path('third_party/SmartTube-droid/smarttubedroid/src/main/res/layout/playback_activity.xml').read_text(), 'Fullscreen target also uses TextureView')
check('SCREEN_ORIENTATION_SENSOR' in application.split('public void onActivityCreated')[1].split('public void onActivityPaused')[0], 'YouTube playback allows sensor rotation')
check('armNm7FirstFrameResume();' in body('private void completeNm7RestoreOnResume()'), 'Restore waits for new-target frame before releasing media clock')
check('onRenderedFirstFrame()' in play and 'postDelayed(mNm7TargetRestoreTimeout, 750)' in play, 'First-frame gate has bounded audio-only fallback')
check('cancelNm7TargetRestore(true);' in back_body and 'cancelNm7TargetRestore(false);' in body('public static void suspendForNm7Iptv()'), 'Back and IPTV suspend clean up pending restore gate')
check('mPlayer.getPlayWhenReady() || sNm7ResumeAfterIptv' in body('private void beginNm7TargetRestore()'), 'Restore remembers intended playback and preserves user pause')
out = Path('dist/mobile-diagnostics')
out.mkdir(parents=True, exist_ok=True)
(out/'lifecycle-source-proof.json').write_text(json.dumps({
    'structural_checks': checks,
    'runtime_verified': False,
    'smarttube_commit': '4825d6aa8b6f1d3181927f9e96c7d89cab13d510',
    'playback_sha256': hashlib.sha256(play.encode()).hexdigest(),
    'browse_sha256': hashlib.sha256(browse.encode()).hexdigest(),
}, indent=2))
print(f'{checks} structural checks passed; device runtime not verified')
