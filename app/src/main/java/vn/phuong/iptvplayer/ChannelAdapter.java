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
    private final LayoutInflater inflater;
    private final Runnable selectionChanged;
    private List<Channel> channels = new ArrayList<>();

    public ChannelAdapter(Context context, Runnable selectionChanged) {
        this.inflater = LayoutInflater.from(context);
        this.selectionChanged = selectionChanged;
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
        holder.keep.setOnCheckedChangeListener(null);
        holder.keep.setChecked(channel.selected());
        holder.keep.setOnCheckedChangeListener((button, checked) -> {
            channel.setSelected(checked);
            selectionChanged.run();
        });
        return convertView;
    }

    private static final class Holder {
        final CheckBox keep;
        final TextView name;
        final TextView group;
        final TextView url;

        Holder(View view) {
            keep = view.findViewById(R.id.checkKeep);
            name = view.findViewById(R.id.txtName);
            group = view.findViewById(R.id.txtGroup);
            url = view.findViewById(R.id.txtUrl);
        }
    }
}
