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
t = replace(t, '        mPlaybackPresenter.onViewInitialized(); // init all controllers',
            '        mPlaybackPresenter.onViewInitialized(); // init all controllers\n        // Build the decoder/player before the Activity is shown so the selected video only waits for its stream.\n        initializePlayer();')
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
t = replace(t, '        toolbar.inflateMenu(R.menu.browse_toolbar);',
            '        toolbar.setTitle("SmartTube Mobile");\n        toolbar.inflateMenu(R.menu.browse_toolbar);')
t = replace(t, 'private static final String TAG = BrowseActivity.class.getSimpleName();',
            'private static final String TAG = BrowseActivity.class.getSimpleName();\n' +
            Path('scripts/smarttube-browse-mobile.java.inc').read_text(encoding='utf-8'))
t = replace(t, '        mJustCreated = false;', '        mJustCreated = false;\n        installNm7MiniPlayer();')
p.write_text(t, encoding='utf-8')
p = phone / 'channeluploads/ChannelUploadsActivity.java'
t = p.read_text(encoding='utf-8').replace('new GridLayoutManager(this, GRID_COLUMNS)', 'new GridLayoutManager(this, 1)')
p.write_text(t, encoding='utf-8')
# Native touch theme: SmartTube cards/sections, dark surface, orange NM7 accent.
theme = Path('third_party/SmartTube-droid/smarttubedroid/src/main/res/values/themes.xml')
t = theme.read_text(encoding='utf-8')
t = t.replace('Theme.MaterialComponents.DayNight.NoActionBar', 'Theme.MaterialComponents.NoActionBar')
t = t.replace('#FF0000', '#FF7A00').replace('#CC0000', '#D96300')
t = t.replace('tools:targetApi="23">true', 'tools:targetApi="23">false')
theme.write_text(t, encoding='utf-8')

initializer = Path('third_party/SmartTube-droid/common/src/main/java/com/liskovsoft/smartyoutubetv2/common/exoplayer/other/ExoPlayerInitializer.java')
t = initializer.read_text(encoding='utf-8')
t = replace(t, 'int bufferForPlaybackMs = 2_500;', 'int bufferForPlaybackMs = 500; // Mobile: faster first-frame threshold; retain forward buffer.')
t = replace(t, 'int bufferForPlaybackAfterRebufferMs = 5_000;', 'int bufferForPlaybackAfterRebufferMs = 1_500;')
initializer.write_text(t, encoding='utf-8')

loader = Path('third_party/SmartTube-droid/common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/models/playback/controllers/VideoLoaderController.java')
t = loader.read_text(encoding='utf-8')
t = replace(t, '    private Disposable mFormatInfoAction;', '''    private Disposable mFormatInfoAction;
    private long mNm7FormatStart;
    private MediaItemFormatInfo mNm7PrefetchedFormatInfo;
    private String mNm7PrefetchedVideoId;''')
t = replace(t, '''        } else {
            mPendingVideo = item;
        }''', '''        } else {
            mPendingVideo = item;
            prefetchNm7FormatInfo(item);
        }''')
t = replace(t, '''        loadVideo(Helpers.firstNonNull(mPendingVideo, getVideo()));
        getPlayer().setButtonState(R.id.action_repeat, getPlayerData().getPlaybackMode());
        mPendingVideo = null;''', '''        Video target = Helpers.firstNonNull(mPendingVideo, getVideo());
        if (target != null && mNm7PrefetchedFormatInfo != null
                && target.videoId != null && target.videoId.equals(mNm7PrefetchedVideoId)) {
            mPlaylist.setCurrent(target);
            getPlayer().setVideo(target);
            getPlayer().resetPlayerState();
            MediaItemFormatInfo ready = mNm7PrefetchedFormatInfo;
            mNm7PrefetchedFormatInfo = null;
            mNm7PrefetchedVideoId = null;
            processFormatInfo(ready);
        } else if (target != null && mFormatInfoAction != null && !mFormatInfoAction.isDisposed()
                && target.videoId != null && target.videoId.equals(mNm7PrefetchedVideoId)) {
            mPlaylist.setCurrent(target);
            getPlayer().setVideo(target);
            getPlayer().resetPlayerState();
        } else {
            loadVideo(target);
        }
        getPlayer().setButtonState(R.id.action_repeat, getPlayerData().getPlaybackMode());
        mPendingVideo = null;''')
t = replace(t, '    private void loadFormatInfo(Video video) {', '''    private void prefetchNm7FormatInfo(Video video) {
        if (video == null || video.videoId == null) return;
        disposeActions();
        mNm7PrefetchedVideoId = video.videoId;
        mNm7PrefetchedFormatInfo = null;
        ServiceManager service = YouTubeServiceManager.instance();
        MediaItemService mediaItemManager = service.getMediaItemService();
        mNm7FormatStart = android.os.SystemClock.elapsedRealtime();
        android.util.Log.i("NM7Startup", "format_prefetch_request");
        mFormatInfoAction = mediaItemManager.getFormatInfoObserve(video.videoId)
                .subscribe(info -> {
                    if (!video.videoId.equals(mNm7PrefetchedVideoId)) return;
                    if (getPlayer() != null && getPlayer().isEngineInitialized()) {
                        mPlaylist.setCurrent(video);
                        getPlayer().setVideo(video);
                        getPlayer().resetPlayerState();
                        mNm7PrefetchedFormatInfo = null;
                        mNm7PrefetchedVideoId = null;
                        processFormatInfo(info);
                    } else {
                        mNm7PrefetchedFormatInfo = info;
                    }
                }, error -> {
                    mNm7PrefetchedFormatInfo = null;
                    mNm7PrefetchedVideoId = null;
                    if (getPlayer() != null) {
                        getPlayer().showProgressBar(false);
                        mErrorFixerController.runFormatErrorAction(error);
                    }
                });
    }

    private void loadFormatInfo(Video video) {''')
t = replace(t, '        mFormatInfoAction = mediaItemManager.getFormatInfoObserve(video.videoId)',
            '        mNm7FormatStart = android.os.SystemClock.elapsedRealtime();\n        android.util.Log.i("NM7Startup", "format_request");\n        mFormatInfoAction = mediaItemManager.getFormatInfoObserve(video.videoId)')
t = replace(t, '    private void processFormatInfo(MediaItemFormatInfo formatInfo) {',
            '    private void processFormatInfo(MediaItemFormatInfo formatInfo) {\n        android.util.Log.i("NM7Startup", "format_ready_ms=" + (android.os.SystemClock.elapsedRealtime() - mNm7FormatStart));')
loader.write_text(t, encoding='utf-8')
print('NM7 Mobile lifecycle and native UI v39 applied to pinned phone source')

# UI overlay deliberately leaves all v39 playback/lifecycle code above unchanged.
import runpy
runpy.run_path("scripts/patch-mobile-ui.py")
