"""Fail-closed Mobile-only patch, shared by Windows and CI; input is pinned fork."""
from pathlib import Path
import re

phone = Path('third_party/SmartTube-droid/smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui')

def replace(text, old, new):
    if text.count(old) != 1:
        raise SystemExit('Expected exactly one anchor: ' + old[:100])
    return text.replace(old, new, 1)

def method(text, signature, body):
    pattern = re.compile(r'(?ms)^    ' + re.escape(signature) + r' \{.*?^    \}')
    text, count = pattern.subn(lambda _: '    ' + signature + ' {\n' + body + '\n    }', text)
    if count != 1:
        raise SystemExit('Missing/ambiguous method: ' + signature)
    return text

p = phone / 'playback/PlaybackActivity.java'
t = p.read_text(encoding='utf-8')
t = replace(t, 'import android.content.Context;', 'import android.content.Context;\nimport android.content.Intent;')
t = replace(t, 'private static final String TAG = PlaybackActivity.class.getSimpleName();',
            'private static final String TAG = PlaybackActivity.class.getSimpleName();\n' +
            Path('scripts/smarttube-playback-mobile.java.inc').read_text(encoding='utf-8'))
t = replace(t, '        super.onCreate(savedInstanceState);', '''        super.onCreate(savedInstanceState);
        sNm7Active = this;
        if (VERSION.SDK_INT >= 33) Nm7BackApi.register(this);''')
t = method(t, 'protected void onStart()', '''        super.onStart();
        sNm7Active = this;
        if (mPlayer == null) initializePlayer();''')
t = method(t, 'protected void onResume()', '''        super.onResume();
        mIsBackPressed = false;
        sNm7Mini = false;
        nm7SetBackground(false);
        if (mPlayer == null) initializePlayer();
        blockEngine(false);
        mPlayerView.setPlayer(mPlayer);
        mPlaybackPresenter.onViewResumed();
        showHideWidgets(true);''')
t = method(t, 'protected void onPause()', '''        // Guard before all presenter/lifecycle callbacks, including Android 6.
        if (!mNm7Stopped && !isFinishing() && mPlayer != null) {
            blockEngine(true);
            if (mPlayer.getPlayWhenReady()) nm7SetBackground(true);
        }
        super.onPause();
        mPlaybackPresenter.onViewPaused();
        showHideWidgets(false);''')
t = method(t, 'protected void onStop()', '''        super.onStop();
        if (mNm7Stopped || isFinishing()) maybeReleasePlayer();''')
t = method(t, 'public void onUserLeaveHint()', '''        super.onUserLeaveHint();
        if (!mNm7Stopped && !isFinishing() && mPlayer != null) {
            blockEngine(true);
            if (mPlayer.getPlayWhenReady()) nm7SetBackground(true);
        }''')
t = method(t, 'public void onBackPressed()', '''        if (onDetailsBack()) return;
        if (mPlayer == null || mNm7Stopped) { super.onBackPressed(); return; }
        mIsBackPressed = true;
        sNm7Mini = true;
        blockEngine(true);
        Intent intent = new Intent(this,
                com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        startActivity(intent);''')
t = replace(t, '    protected void onDestroy() {\n        super.onDestroy();', '''    protected void onDestroy() {
        if (VERSION.SDK_INT >= 33) Nm7BackApi.unregister(this);
        if (sNm7Active == this) {
            sNm7Active = null;
            sNm7Mini = false;
            nm7SetBackground(false);
            stopService(new Intent().setClassName(this, "vn.phuong.iptvplayer.BackgroundPlaybackService"));
        }
        super.onDestroy();''')
t = replace(t, '            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {', '''            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playWhenReady && playbackState == Player.STATE_READY && !mNm7Stopped && !mNm7OwnsPlayback) {
                    try {
                        Class.forName("vn.phuong.iptvplayer.MobileNm7Application")
                                .getMethod("pauseIptvForYoutube").invoke(null);
                        mNm7OwnsPlayback = true;
                    } catch (ReflectiveOperationException | RuntimeException error) {
                        android.util.Log.e("NM7Playback", "IPTV ownership handoff failed", error);
                    }
                }
                if (mNm7OwnsPlayback && (!playWhenReady || playbackState == Player.STATE_ENDED)) {
                    stopService(new Intent().setClassName(PlaybackActivity.this,
                            "vn.phuong.iptvplayer.BackgroundPlaybackService"));
                } else if (mNm7OwnsPlayback && isEngineBlocked() && !mNm7Stopped) {
                    nm7SetBackground(true);
                }''')
t = re.sub(r'    @Override\n(    public (?:float getPitch|void setPitch)\()', r'\1', t)
t = t.replace('showDetailsMessage(getString(R.string.section_is_empty));', 'showDetailsMessage("No comments available");')
p.write_text(t, encoding='utf-8')
p = phone / 'browse/BrowseActivity.java'
t = p.read_text(encoding='utf-8')
t = replace(t, 'private static final int GRID_COLUMNS = 2;', 'private static final int GRID_COLUMNS = 1;')
t = replace(t, 'private static final String TAG = BrowseActivity.class.getSimpleName();',
            'private static final String TAG = BrowseActivity.class.getSimpleName();\n' +
            Path('scripts/smarttube-browse-mobile.java.inc').read_text(encoding='utf-8'))
t = replace(t, '        mJustCreated = false;', '        mJustCreated = false;\n        installNm7MiniPlayer();')
p.write_text(t, encoding='utf-8')
p = phone / 'channeluploads/ChannelUploadsActivity.java'
t = p.read_text(encoding='utf-8').replace('new GridLayoutManager(this, GRID_COLUMNS)', 'new GridLayoutManager(this, 1)')
p.write_text(t, encoding='utf-8')
print('NM7 Mobile lifecycle v37 applied to pinned phone source')
