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

import java.io.ByteArrayOutputStream;
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
    private static final int CONNECT_TIMEOUT_MS = 6_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private static final LruCache<String, Bitmap> LOGO_CACHE =
            new LruCache<String, Bitmap>(LOGO_CACHE_KB) {
                @Override protected int sizeOf(String key, Bitmap bitmap) {
                    return Math.max(1, bitmap.getAllocationByteCount() / 1024);
                }
            };

    private static final Object WAITERS_LOCK = new Object();
    private static final Map<String, List<WeakReference<LogoWaiter>>> LOGO_WAITERS = new HashMap<>();

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
        logoCacheDir = new File(context.getCacheDir(), "channel-logos-v3");
        if (!logoCacheDir.exists()) logoCacheDir.mkdirs();
    }

    public void submit(List<Channel> channels) {
        this.channels = new ArrayList<>(channels);
        notifyDataSetChanged();
    }

    public void submitGuide(EpgStore.Guide guide) {
        this.guide = guide;
        notifyDataSetChanged();
    }

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
        } else {
            h = (Holder) convertView.getTag();
        }

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
        convertView.setMinimumHeight(dp(tv ? 82 : (compact ? 58 : 64)));
        int vertical = dp(tv ? 4 : 2);
        convertView.setPadding(0, vertical, 0, vertical);

        loadLogo(h, c);
        return convertView;
    }

    /**
     * Super OK has a separate icon-URL resolution layer. NM7 mirrors that separation:
     * resolve a channel's ordered logo candidates first, then fetch/cache the image without
     * involving the IPTV player. Playlist request headers are forwarded to the image host.
     */
    private void loadLogo(Holder holder, Channel channel) {
        List<String> candidates = channel == null
                ? java.util.Collections.emptyList()
                : channel.logoCandidates();

        holder.logo.setImageDrawable(null);
        holder.logo.setVisibility(View.GONE);
        holder.badge.setVisibility(View.VISIBLE);

        if (candidates.isEmpty()) {
            holder.logo.setTag(null);
            return;
        }

        requestLogo(holder, candidates,
                channel == null ? java.util.Collections.emptyMap() : channel.headers(), 0);
    }

    private void requestLogo(Holder holder, List<String> candidates,
                             Map<String, String> headers, int index) {
        if (index >= candidates.size()) return;

        String url = candidates.get(index);
        if (!isRemoteLogo(url)) {
            requestLogo(holder, candidates, headers, index + 1);
            return;
        }

        holder.logo.setTag(url);
        String cacheKey = cacheKey(url, headers);

        Bitmap cached = LOGO_CACHE.get(cacheKey);
        if (cached != null) {
            showLogo(holder, url, cached);
            return;
        }

        Bitmap disk = readCachedLogo(cacheKey);
        if (disk != null) {
            LOGO_CACHE.put(cacheKey, disk);
            showLogo(holder, url, disk);
            return;
        }

        boolean startLoad = false;
        synchronized (WAITERS_LOCK) {
            List<WeakReference<LogoWaiter>> waiters = LOGO_WAITERS.get(cacheKey);
            if (waiters == null) {
                waiters = new ArrayList<>();
                LOGO_WAITERS.put(cacheKey, waiters);
                startLoad = true;
            }
            waiters.add(new WeakReference<>(
                    new LogoWaiter(holder, candidates, headers, index, url)));
        }

        if (!startLoad) return;

        logoIo.execute(() -> {
            Bitmap bitmap = downloadLogo(url, headers);
            if (bitmap != null) {
                LOGO_CACHE.put(cacheKey, bitmap);
                writeCachedLogo(cacheKey, bitmap);
            }

            List<WeakReference<LogoWaiter>> waiters;
            synchronized (WAITERS_LOCK) {
                waiters = LOGO_WAITERS.remove(cacheKey);
            }
            if (waiters == null) return;

            final Bitmap ready = bitmap;
            for (WeakReference<LogoWaiter> reference : waiters) {
                LogoWaiter waiter = reference.get();
                if (waiter == null) continue;

                waiter.holder.logo.post(() -> {
                    if (!url.equals(waiter.holder.logo.getTag())) return;
                    if (ready != null) {
                        showLogo(waiter.holder, url, ready);
                    } else {
                        requestLogo(waiter.holder, waiter.candidates,
                                waiter.headers, waiter.index + 1);
                    }
                });
            }
        });
    }

    private Bitmap readCachedLogo(String cacheKey) {
        File file = cacheFile(cacheKey);
        if (!file.isFile()) return null;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        if (bitmap == null) file.delete();
        return bitmap;
    }

    private void writeCachedLogo(String cacheKey, Bitmap bitmap) {
        File target = cacheFile(cacheKey);
        File temp = new File(target.getAbsolutePath() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temp)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) return;
            if (!temp.renameTo(target)) {
                try (FileOutputStream copy = new FileOutputStream(target)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, copy);
                }
            }
        } catch (Exception ignored) {
            if (temp.exists()) temp.delete();
        }
    }

    private Bitmap downloadLogo(String url, Map<String, String> headers) {
        for (int attempt = 0; attempt < 2; attempt++) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setInstanceFollowRedirects(true);
                connection.setUseCaches(true);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept",
                        "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
                connection.setRequestProperty("User-Agent",
                        headerOrDefault(headers, "User-Agent",
                                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 "
                                        + "(KHTML, like Gecko) Chrome/138.0.0.0 Mobile Safari/537.36"));

                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key == null || value == null || value.isEmpty()) continue;
                    if ("User-Agent".equalsIgnoreCase(key)) continue;
                    if (key.matches("[!#$%&'*+.^_|~0-9A-Za-z-]+")
                            && value.indexOf('\r') < 0 && value.indexOf('\n') < 0) {
                        connection.setRequestProperty(key, value);
                    }
                }

                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) continue;

                int advertised = connection.getContentLength();
                if (advertised > MAX_LOGO_BYTES) return null;

                ByteArrayOutputStream output = new ByteArrayOutputStream(
                        Math.min(Math.max(advertised, 4096), MAX_LOGO_BYTES));
                try (InputStream input = connection.getInputStream()) {
                    byte[] buffer = new byte[16 * 1024];
                    int total = 0;
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        if (total > MAX_LOGO_BYTES) return null;
                        output.write(buffer, 0, count);
                    }
                }

                byte[] data = output.toByteArray();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                if (bitmap != null) return bitmap;
            } catch (Exception ignored) {
                // Retry once for transient CDN/redirect failures.
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        return null;
    }

    private String headerOrDefault(Map<String, String> headers, String name, String fallback) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())
                    && entry.getValue() != null && !entry.getValue().isEmpty()) {
                return entry.getValue();
            }
        }
        return fallback;
    }

    private String cacheKey(String url, Map<String, String> headers) {
        StringBuilder value = new StringBuilder(url);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if ("User-Agent".equalsIgnoreCase(entry.getKey())
                    || "Referer".equalsIgnoreCase(entry.getKey())
                    || "Origin".equalsIgnoreCase(entry.getKey())) {
                value.append('\n').append(entry.getKey()).append(':').append(entry.getValue());
            }
        }
        return value.toString();
    }

    private File cacheFile(String value) {
        return new File(logoCacheDir, sha256(value) + ".img");
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte b : digest) {
                result.append(String.format(java.util.Locale.ROOT, "%02x", b));
            }
            return result.toString();
        } catch (Exception impossible) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static boolean isRemoteLogo(String url) {
        return url != null
                && (url.startsWith("http://") || url.startsWith("https://"));
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

    private static final class LogoWaiter {
        final Holder holder;
        final List<String> candidates;
        final Map<String, String> headers;
        final int index;
        final String url;

        LogoWaiter(Holder holder, List<String> candidates, Map<String, String> headers,
                   int index, String url) {
            this.holder = holder;
            this.candidates = candidates;
            this.headers = headers;
            this.index = index;
            this.url = url;
        }
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
