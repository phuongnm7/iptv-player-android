package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/** Adds the mobile watch-and-browse layout without changing Android TV playback behaviour. */
@UnstableApi
public final class MobileInlinePlayerProvider extends ContentProvider implements Application.ActivityLifecycleCallbacks {
    private MainActivity currentActivity;
    private ExoPlayer player;
    private PlayerView playerView;
    private LinearLayout panel;
    private TextView title;
    private TextView programme;
    private ListView channelList;
    private AdapterView.OnItemClickListener originalClick;

    @Override public boolean onCreate() {
        if (getContext() != null) {
            ((Application) getContext().getApplicationContext()).registerActivityLifecycleCallbacks(this);
        }
        return true;
    }

    @Override public void onActivityResumed(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        MainActivity main = (MainActivity) activity;
        if (AppPreferences.isTvInterface(main)) {
            if (currentActivity == main) detach();
            return;
        }
        if (currentActivity != main || panel == null || panel.getParent() == null) attach(main);
    }

    private void attach(MainActivity activity) {
        detach();
        currentActivity = activity;
        LinearLayout root = activity.findViewById(R.id.mainRoot);
        channelList = activity.findViewById(R.id.listChannels);
        if (root == null || channelList == null) return;

        panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setVisibility(View.GONE);
        panel.setBackgroundResource(R.drawable.panel);
        panel.setPadding(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 8));

        playerView = new PlayerView(activity);
        playerView.setUseController(true);
        playerView.setControllerAutoShow(true);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        panel.addView(playerView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 220)));

        title = new TextView(activity);
        title.setTextSize(17);
        title.setTextColor(activity.getColor(R.color.text_primary));
        title.setMaxLines(1);
        title.setPadding(dp(activity, 8), dp(activity, 7), dp(activity, 8), 0);
        panel.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        programme = new TextView(activity);
        programme.setTextSize(14);
        programme.setTextColor(activity.getColor(R.color.text_secondary));
        programme.setMaxLines(2);
        programme.setPadding(dp(activity, 8), dp(activity, 2), dp(activity, 8), 0);
        panel.addView(programme, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        // Directly below the Nm7 IPTV heading; channel tabs/search/list remain below the video.
        root.addView(panel, Math.min(1, root.getChildCount()), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        originalClick = channelList.getOnItemClickListener();
        channelList.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getAdapter().getItem(position);
            if (!(item instanceof Channel)) {
                if (originalClick != null) originalClick.onItemClick(parent, view, position, id);
                return;
            }
            Channel channel = (Channel) item;
            if (!playInline(channel) && originalClick != null) {
                originalClick.onItemClick(parent, view, position, id);
            }
        });
    }

    private boolean playInline(Channel channel) {
        if (currentActivity == null || playerView == null) return false;
        String scheme = Uri.parse(channel.url()).getScheme();
        DrmSpec drm = DrmSpec.fromOptions(channel.options());
        // Keep complex DRM/UDP/RTMP paths in the proven full-screen PlayerActivity for now.
        if (drm.hasDrm() || scheme == null || !(scheme.matches("(?i)https?|rtsp"))) return false;
        try {
            releasePlayer();
            panel.setVisibility(View.VISIBLE);
            title.setText(channel.name());
            updateProgramme(channel);
            AppPreferences.recordRecent(currentActivity, channel);

            Map<String, String> headers = new LinkedHashMap<>(channel.headers());
            String ua = headers.containsKey("User-Agent") ? headers.get("User-Agent") : "Nm7-IPTV/1.10.1 Android";
            DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
                    .setUserAgent(ua)
                    .setConnectTimeoutMs(15_000)
                    .setReadTimeoutMs(20_000)
                    .setDefaultRequestProperties(headers);
            DefaultDataSource.Factory data = new DefaultDataSource.Factory(currentActivity, http);
            DefaultMediaSourceFactory mediaFactory = new DefaultMediaSourceFactory(data);
            player = new ExoPlayer.Builder(currentActivity,
                    new DefaultRenderersFactory(currentActivity).setEnableDecoderFallback(true))
                    .setMediaSourceFactory(mediaFactory).build();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
            player.setHandleAudioBecomingNoisy(true);
            playerView.setPlayer(player);

            MediaItem.Builder media = new MediaItem.Builder().setUri(channel.url());
            String mime = channel.mimeHint();
            if (mime == null || mime.isEmpty()) mime = StreamSpec.inferMime(channel.url(), channel.options());
            if (mime != null && !mime.isEmpty()) media.setMimeType(mime);
            MediaItem item = media.build();
            if ("rtsp".equalsIgnoreCase(scheme)) {
                player.setMediaSource(new RtspMediaSource.Factory()
                        .setForceUseRtpTcp(true).setUserAgent(ua).createMediaSource(item));
            } else {
                player.setMediaItem(item);
            }
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_BUFFERING) programme.setText("Đang tải luồng…");
                    if (state == Player.STATE_READY) updateProgramme(channel);
                }
                @Override public void onPlayerError(PlaybackException error) {
                    programme.setText("Không phát được trong khung Mobile • chạm lại để mở trình phát đầy đủ");
                }
            });
            player.prepare();
            player.play();
            playerView.showController();
            return true;
        } catch (Exception error) {
            releasePlayer();
            Toast.makeText(currentActivity, "Mở bằng trình phát đầy đủ", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void updateProgramme(Channel channel) {
        if (programme == null || channelList == null) return;
        try {
            Object adapter = channelList.getAdapter();
            if (!(adapter instanceof ChannelAdapter)) { programme.setText(channel.group()); return; }
            Field field = ChannelAdapter.class.getDeclaredField("guide");
            field.setAccessible(true);
            EpgStore.Guide guide = (EpgStore.Guide) field.get(adapter);
            EpgStore.Programme now = guide == null ? null : guide.find(channel);
            if (now == null) programme.setText(channel.group());
            else programme.setText(now.title + "  •  " + now.startText() + " - " + now.endText());
        } catch (Exception ignored) {
            programme.setText(channel.group());
        }
    }

    private void releasePlayer() {
        if (player != null) {
            playerView.setPlayer(null);
            player.release();
            player = null;
        }
    }

    private void detach() {
        releasePlayer();
        if (channelList != null && originalClick != null) channelList.setOnItemClickListener(originalClick);
        if (panel != null && panel.getParent() instanceof LinearLayout) {
            ((LinearLayout) panel.getParent()).removeView(panel);
        }
        currentActivity = null;
        channelList = null;
        originalClick = null;
        panel = null;
        playerView = null;
        title = null;
        programme = null;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    @Override public void onActivityDestroyed(Activity activity) {
        if (activity == currentActivity) detach();
    }
    @Override public void onActivityCreated(Activity a, Bundle b) { }
    @Override public void onActivityStarted(Activity a) { }
    @Override public void onActivityPaused(Activity a) { if (a == currentActivity && player != null) player.pause(); }
    @Override public void onActivityStopped(Activity a) { }
    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) { }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String sort) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
