package vn.phuong.iptvplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChannelAdapter extends BaseAdapter {
    interface Listener {
        void onSelectionChanged();
        void onFavoriteChanged(Channel channel, boolean favorite);
    }

    private static final int LOGO_CACHE_KB = 16 * 1024;
    private static final LruCache<String, Bitmap> LOGO_CACHE = new LruCache<String, Bitmap>(LOGO_CACHE_KB) {
        @Override protected int sizeOf(String key, Bitmap bitmap) {
            return Math.max(1, bitmap.getAllocationByteCount() / 1024);
        }
    };
    private static final Set<String> LOGO_LOADING =
            Collections.synchronizedSet(new HashSet<>());

    private final LayoutInflater inflater;
    private final Context context;
    private final Listener listener;
    private final ExecutorService logoIo = Executors.newFixedThreadPool(4);
    private List<Channel> channels = new ArrayList<>();
    private EpgStore.Guide guide;
    private String playingChannelId = "";

    public ChannelAdapter(Context context, Listener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void submit(List<Channel> channels) {
        this.channels = new ArrayList<>(channels);
        notifyDataSetChanged();
    }
    public void submitGuide(EpgStore.Guide guide) { this.guide = guide; notifyDataSetChanged(); }
    void setPlayingChannel(Channel channel) {
        String next = channel == null ? "" : AppPreferences.id(channel);
        if (next.equals(playingChannelId)) return;
        playingChannelId = next;
        notifyDataSetChanged();
    }
    @Override public int getCount() { return channels.size(); }
    @Override public Channel getItem(int position) { return channels.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_channel, parent, false);
            h = new Holder(convertView);
            convertView.setTag(h);
        } else h = (Holder) convertView.getTag();

        Channel c = getItem(position);
        h.name.setText(c.name());
        h.group.setText(c.group());
        h.url.setText(c.url());
        EpgStore.Programme programme = guide == null ? null : guide.find(c);
        h.epg.setVisibility(programme == null ? View.GONE : View.VISIBLE);
        if (programme != null) {
            h.programme.setText(programme.title);
            h.epgStart.setText(programme.startText());
            h.epgEnd.setText(programme.endText());
            h.epgProgress.setProgress(programme.progress());
        }
        h.url.setVisibility(AppPreferences.showUrls(context) ? View.VISIBLE : View.GONE);
        h.badge.setText(c.name().isEmpty() ? "TV"
                : c.name().substring(0, 1).toUpperCase(java.util.Locale.ROOT));
        h.favorite.setText(AppPreferences.isFavorite(context, c) ? "★" : "☆");
        h.favorite.setOnClickListener(v -> {
            boolean favorite = AppPreferences.toggleFavorite(context, c);
            h.favorite.setText(favorite ? "★" : "☆");
            listener.onFavoriteChanged(c, favorite);
        });

        boolean tv = AppPreferences.isTvInterface(context);
        boolean compact = AppPreferences.compactRows(context);
        boolean playing = !tv && !playingChannelId.isEmpty()
                && playingChannelId.equals(AppPreferences.id(c));
        convertView.setBackgroundResource(
                playing ? R.drawable.channel_card_playing : R.drawable.channel_card);
        convertView.setMinimumHeight(dp(tv ? 78 : (compact ? 54 : 60)));
        int vertical = dp(tv ? 3 : 0);
        convertView.setPadding(0, vertical, 0, vertical);
        loadLogo(h, c.logo());
        return convertView;
    }

    private void loadLogo(Holder holder, String url) {
        holder.logo.setTag(url);
        Bitmap cached = url == null ? null : LOGO_CACHE.get(url);
        if (cached != null) {
            showLogo(holder, url, cached);
            return;
        }
        holder.logo.setImageDrawable(null);
        holder.logo.setVisibility(View.GONE);
        holder.badge.setVisibility(View.VISIBLE);
        if (url == null || url.isEmpty()
                || !(url.startsWith("http://") || url.startsWith("https://"))) return;

        if (!LOGO_LOADING.add(url)) return;
        logoIo.execute(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(5_000);
                connection.setReadTimeout(8_000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "Nm7-IPTV/1.10.22 Android");
                try (InputStream input = connection.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(input);
                }
                if (bitmap != null) LOGO_CACHE.put(url, bitmap);
            } catch (Exception ignored) {
                // Keep the inexpensive text badge when a remote logo is unavailable.
            } finally {
                if (connection != null) connection.disconnect();
                LOGO_LOADING.remove(url);
            }
            final Bitmap ready = bitmap;
            holder.logo.post(() -> {
                if (ready != null && url.equals(holder.logo.getTag())) showLogo(holder, url, ready);
                notifyDataSetChanged();
            });
        });
    }

    private static void showLogo(Holder holder, String url, Bitmap bitmap) {
        if (!url.equals(holder.logo.getTag())) return;
        holder.logo.setImageBitmap(bitmap);
        holder.logo.setVisibility(View.VISIBLE);
        holder.badge.setVisibility(View.GONE);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class Holder {
        final TextView name, group, url, badge, favorite, programme, epgStart, epgEnd;
        final ImageView logo;
        final View epg;
        final ProgressBar epgProgress;
        Holder(View v) {
            name = v.findViewById(R.id.txtName);
            group = v.findViewById(R.id.txtGroup);
            url = v.findViewById(R.id.txtUrl);
            badge = v.findViewById(R.id.txtChannelBadge);
            favorite = v.findViewById(R.id.btnFavorite);
            logo = v.findViewById(R.id.imgChannelLogo);
            epg = v.findViewById(R.id.epgSection);
            programme = v.findViewById(R.id.txtProgramme);
            epgStart = v.findViewById(R.id.txtEpgStart);
            epgEnd = v.findViewById(R.id.txtEpgEnd);
            epgProgress = v.findViewById(R.id.epgProgress);
        }
    }
}
