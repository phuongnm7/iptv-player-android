package vn.phuong.iptvplayer;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;


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
        loadLogo(h, c);
        return convertView;
    }

    private void loadLogo(Holder holder, Channel channel) {
        String url = channel == null ? "" : channel.effectiveLogoUrl();
        holder.logo.setTag(url);
        Glide.with(holder.logo).clear(holder.logo);
        holder.logo.setImageDrawable(null);
        holder.logo.setVisibility(View.GONE);
        holder.badge.setVisibility(View.VISIBLE);

        if (url == null || url.trim().isEmpty()) return;

        // Super OK uses Glide directly from the effective M3U logo URL.
        // Glide owns request cancellation, memory/disk caching and RecyclerView/View recycling.
        Glide.with(holder.logo)
                .load(url)
                .placeholder(R.drawable.channel_badge)
                .error(R.drawable.channel_badge)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model,
                            Target<Drawable> target, boolean isFirstResource) {
                        if (url.equals(holder.logo.getTag())) {
                            holder.logo.setVisibility(View.GONE);
                            holder.badge.setVisibility(View.VISIBLE);
                        }
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                            Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource,
                            boolean isFirstResource) {
                        if (url.equals(holder.logo.getTag())) {
                            holder.logo.setVisibility(View.VISIBLE);
                            holder.badge.setVisibility(View.GONE);
                        }
                        return false;
                    }
                })
                .into(holder.logo);
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
