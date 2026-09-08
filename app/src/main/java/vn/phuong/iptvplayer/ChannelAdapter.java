package vn.phuong.iptvplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class ChannelAdapter extends BaseAdapter {
    interface Listener {
        void onSelectionChanged();
        void onFavoriteChanged(Channel channel, boolean favorite);
    }
    private final LayoutInflater inflater;
    private final Context context;
    private final Listener listener;
    private List<Channel> channels = new ArrayList<>();

    public ChannelAdapter(Context context, Listener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void submit(List<Channel> channels) {
        this.channels = new ArrayList<>(channels);
        notifyDataSetChanged();
    }

    @Override public int getCount() { return channels.size(); }
    @Override public Channel getItem(int position) { return channels.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_channel, parent, false);
            holder = new Holder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        Channel channel = getItem(position);
        holder.name.setText(channel.name());
        holder.group.setText(channel.group());
        holder.url.setText(channel.url());
        holder.url.setVisibility(AppPreferences.showUrls(context) ? View.VISIBLE : View.GONE);
        holder.badge.setText(channel.name().isEmpty() ? "TV" : channel.name().substring(0, 1).toUpperCase(java.util.Locale.ROOT));
        holder.favorite.setText(AppPreferences.isFavorite(context, channel) ? "★" : "☆");
        holder.favorite.setOnClickListener(v -> {
            boolean favorite = AppPreferences.toggleFavorite(context, channel);
            holder.favorite.setText(favorite ? "★" : "☆");
            listener.onFavoriteChanged(channel, favorite);
        });
        int vertical = dp(AppPreferences.isTvInterface(context) ? 10 : (AppPreferences.compactRows(context) ? 3 : 8));
        convertView.setPadding(0, vertical, 0, vertical);
        holder.keep.setOnCheckedChangeListener(null);
        holder.keep.setChecked(channel.selected());
        holder.keep.setOnCheckedChangeListener((button, checked) -> {
            channel.setSelected(checked);
            listener.onSelectionChanged();
        });
        return convertView;
    }

    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

    private static final class Holder {
        final CheckBox keep;
        final TextView name;
        final TextView group;
        final TextView url;
        final TextView badge;
        final TextView favorite;

        Holder(View view) {
            keep = view.findViewById(R.id.checkKeep);
            name = view.findViewById(R.id.txtName);
            group = view.findViewById(R.id.txtGroup);
            url = view.findViewById(R.id.txtUrl);
            badge = view.findViewById(R.id.txtChannelBadge);
            favorite = view.findViewById(R.id.btnFavorite);
        }
    }
}
