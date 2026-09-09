package vn.phuong.iptvplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChannelAdapter extends BaseAdapter {
    interface Listener {
        void onSelectionChanged();
        void onFavoriteChanged(Channel channel, boolean favorite);
    }
    private final LayoutInflater inflater;
    private final Context context;
    private final Listener listener;
    private final ExecutorService logoIo = Executors.newFixedThreadPool(3);
    private List<Channel> channels = new ArrayList<>();

    public ChannelAdapter(Context context, Listener listener) {
        this.context = context; this.inflater = LayoutInflater.from(context); this.listener = listener;
    }
    public void submit(List<Channel> channels) { this.channels = new ArrayList<>(channels); notifyDataSetChanged(); }
    @Override public int getCount() { return channels.size(); }
    @Override public Channel getItem(int position) { return channels.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) { convertView = inflater.inflate(R.layout.item_channel, parent, false); h = new Holder(convertView); convertView.setTag(h); }
        else h = (Holder) convertView.getTag();
        Channel c = getItem(position);
        h.name.setText(c.name()); h.group.setText(c.group()); h.url.setText(c.url());
        h.url.setVisibility(AppPreferences.showUrls(context) ? View.VISIBLE : View.GONE);
        h.badge.setText(c.name().isEmpty() ? "TV" : c.name().substring(0, 1).toUpperCase(java.util.Locale.ROOT));
        h.favorite.setText(AppPreferences.isFavorite(context, c) ? "★" : "☆");
        h.favorite.setOnClickListener(v -> { boolean favorite = AppPreferences.toggleFavorite(context, c); h.favorite.setText(favorite ? "★" : "☆"); listener.onFavoriteChanged(c, favorite); });
        int vertical = dp(AppPreferences.isTvInterface(context) ? 10 : (AppPreferences.compactRows(context) ? 3 : 8));
        convertView.setPadding(0, vertical, 0, vertical);
        loadLogo(h, c.logo());
        return convertView;
    }

    private void loadLogo(Holder h, String url) {
        h.logo.setImageDrawable(null); h.logo.setVisibility(View.GONE); h.badge.setVisibility(View.VISIBLE);
        h.logo.setTag(url);
        if (url == null || url.isEmpty() || !(url.startsWith("http://") || url.startsWith("https://"))) return;
        logoIo.execute(() -> {
            try (InputStream in = new URL(url).openConnection().getInputStream()) {
                final Bitmap bitmap = BitmapFactory.decodeStream(in);
                h.logo.post(() -> { if (url.equals(h.logo.getTag()) && bitmap != null) { h.logo.setImageBitmap(bitmap); h.logo.setVisibility(View.VISIBLE); h.badge.setVisibility(View.GONE); } });
            } catch (Exception ignored) { }
        });
    }
    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
    private static final class Holder {
        final TextView name, group, url, badge, favorite; final ImageView logo;
        Holder(View v) { name=v.findViewById(R.id.txtName); group=v.findViewById(R.id.txtGroup); url=v.findViewById(R.id.txtUrl); badge=v.findViewById(R.id.txtChannelBadge); favorite=v.findViewById(R.id.btnFavorite); logo=v.findViewById(R.id.imgChannelLogo); }
    }
}
