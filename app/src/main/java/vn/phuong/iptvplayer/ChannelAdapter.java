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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChannelAdapter extends BaseAdapter {
    interface Listener {
        void onSelectionChanged();
        void onFavoriteChanged(Channel channel, boolean favorite);
    }

    private static final int MAX_LOGO_BYTES = 2 * 1024 * 1024;
    private static final int LOGO_CACHE_KB = 24 * 1024;
    private static final LruCache<String, Bitmap> LOGO_CACHE =
            new LruCache<String, Bitmap>(LOGO_CACHE_KB) {
                @Override protected int sizeOf(String key, Bitmap bitmap) {
                    return Math.max(1, bitmap.getAllocationByteCount() / 1024);
                }
            };
    private static final Object WAITERS_LOCK = new Object();
    private static final Map<String, List<WeakReference<Holder>>> LOGO_WAITERS = new HashMap<>();

    private final LayoutInflater inflater;
    private final Context context;
    private final Listener listener;
    private final ExecutorService logoIo = Executors.newFixedThreadPool(4);
    private final File logoCacheDir;
    private List<Channel> channels = new ArrayList<>();
    private EpgStore.Guide guide;
    private String playingChannelId = "";

    public ChannelAdapter(Context context, Listener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
        logoCacheDir = new File(context.getCacheDir(), "channel-logos-v2");
        if (!logoCacheDir.exists()) logoCacheDir.mkdirs();
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
        Object previous = holder.logo.getTag();
        if (url != null && url.equals(previous) && holder.logo.getDrawable() != null) {
            holder.logo.setVisibility(View.VISIBLE);
            holder.badge.setVisibility(View.GONE);
            return;
        }
        holder.logo.setTag(url);
        Bitmap cached = url == null ? null : LOGO_CACHE.get(url);
        if (cached != null) {
            showLogo(holder, url, cached);
            return;
        }

        holder.logo.setImageDrawable(null);
        holder.logo.setVisibility(View.GONE);
        holder.badge.setVisibility(View.VISIBLE);
        if (!isRemoteLogo(url)) return;

        boolean startLoad = false;
        synchronized (WAITERS_LOCK) {
            List<WeakReference<Holder>> waiters = LOGO_WAITERS.get(url);
            if (waiters == null) {
                waiters = new ArrayList<>();
                LOGO_WAITERS.put(url, waiters);
                startLoad = true;
            }
            waiters.add(new WeakReference<>(holder));
        }
        if (!startLoad) return;

        logoIo.execute(() -> {
            Bitmap bitmap = readCachedLogo(url);
            if (bitmap == null) bitmap = downloadLogo(url);
            if (bitmap != null) LOGO_CACHE.put(url, bitmap);

            List<WeakReference<Holder>> waiters;
            synchronized (WAITERS_LOCK) {
                waiters = LOGO_WAITERS.remove(url);
            }
            if (waiters == null) return;
            final Bitmap ready = bitmap;
            for (WeakReference<Holder> reference : waiters) {
                Holder waiting = reference.get();
                if (waiting == null) continue;
                waiting.logo.post(() -> {
                    if (ready != null && url.equals(waiting.logo.getTag())) {
                        showLogo(waiting, url, ready);
                    }
                });
            }
        });
    }

    private Bitmap readCachedLogo(String url) {
        File file = cacheFile(url);
        if (!file.isFile()) return null;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) file.delete();
        return bitmap;
    }

    private Bitmap downloadLogo(String url) {
        HttpURLConnection connection = null;
        File target = cacheFile(url);
        File temporary = new File(target.getAbsolutePath() + ".tmp");
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(8_000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Nm7-IPTV/1.10.23 Android");
            int length = connection.getContentLength();
            if (length > MAX_LOGO_BYTES) return null;
            int total = 0;
            try (InputStream input = connection.getInputStream();
                 FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    total += count;
                    if (total > MAX_LOGO_BYTES) throw new IllegalArgumentException("logo too large");
                    output.write(buffer, 0, count);
                }
            }
            Bitmap bitmap = BitmapFactory.decodeFile(temporary.getAbsolutePath());
            if (bitmap == null) return null;
            if (!temporary.renameTo(target)) {
                try (FileOutputStream output = new FileOutputStream(target)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, output);
                }
            }
            return bitmap;
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
            if (temporary.exists()) temporary.delete();
        }
    }

    private File cacheFile(String url) {
        return new File(logoCacheDir, sha256(url) + ".img");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte b : digest) result.append(String.format(java.util.Locale.ROOT, "%02x", b));
            return result.toString();
        } catch (Exception impossible) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static boolean isRemoteLogo(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
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
