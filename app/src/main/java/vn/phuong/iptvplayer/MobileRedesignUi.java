package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Mobile-only reference UI. It owns the visual navigation layer while MainActivity
 * keeps the 1.10.112 M3U/EPG/player engine untouched.
 */
final class MobileRedesignUi {
    static final int REQUEST_LOCAL_PLAYLIST = 119;

    interface Host {
        void play(Channel channel);
        void loadUrl(String url);
        void loadLocalEntry(MobilePlaylistStore.Entry entry);
        void openLocalPlaylistPicker();
        void showSettings();
        void showNetworkStream();
    }

    private final MainActivity activity;
    private final Host host;
    private final java.util.function.Supplier<List<Channel>> channelSupplier;
    private final LinearLayout content;
    private final EditText search;
    private final ImageView logo;
    private final TextView title;
    private final Handler main = new Handler(Looper.getMainLooper());

    private final List<Channel> empty = new ArrayList<>();
    private int selectedTab = 1; // 0=YouTube, 1=Live Events, 2=Channel, 3=Options, 4=Playlist
    private int eventStatus = 0; // 0 all, 1 live, 2 upcoming, 3 next24h, 4 ended
    private String selectedEventGroup = "";
    private String selectedChannelGroup = "";
    private String searchQuery = "";
    private int columns = 4;

    MobileRedesignUi(
            MainActivity activity,
            Host host,
            java.util.function.Supplier<List<Channel>> channelSupplier,
            LinearLayout content,
            EditText search,
            ImageView logo,
            TextView title) {
        this.activity = activity;
        this.host = host;
        this.channelSupplier = channelSupplier;
        this.content = content;
        this.search = search;
        this.logo = logo;
        this.title = title;
    }

    void install() {
        configureHeader();
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                searchQuery = s == null ? "" : s.toString().trim().toLowerCase(Locale.ROOT);
                renderCurrent();
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        activity.findViewById(R.id.btnLiveEvents).setOnClickListener(v -> {
            selectedTab = 1;
            renderLiveEvents();
        });
        activity.findViewById(R.id.btnChannel).setOnClickListener(v -> {
            selectedTab = 2;
            renderChannels();
        });
        activity.findViewById(R.id.btnTvMode).setOnClickListener(v -> {
            selectedTab = 3;
            host.showSettings();
        });
        activity.findViewById(R.id.btnPlaylist).setOnClickListener(v -> {
            selectedTab = 4;
            renderPlaylists();
        });
        activity.findViewById(R.id.btnYoutube).setOnClickListener(v -> openYouTube());
        activity.findViewById(R.id.btnSearch).setOnClickListener(v -> toggleSearch());
        renderLiveEvents();
    }

    void refreshAfterPlaylist() {
        if (selectedTab == 1) renderLiveEvents();
        else if (selectedTab == 2) renderChannels();
    }

    void showPlaylistScreen() {
        selectedTab = 4;
        renderPlaylists();
    }

    void handleLocalPlaylistResult(Uri uri) {
        if (uri == null) return;
        try {
            String suggested = uri.getLastPathSegment();
            MobilePlaylistStore.Entry entry = MobilePlaylistStore.addLocal(activity, suggested, uri);
            host.loadLocalEntry(entry);
            selectedTab = 4;
            renderPlaylists();
        } catch (Exception e) {
            Toast.makeText(activity, "Không thêm được playlist: " + readable(e), Toast.LENGTH_LONG).show();
        }
    }

    private void configureHeader() {
        title.setTextSize(20);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        SpannableString value = new SpannableString("Phuongnm7 IPTV");
        value.setSpan(new ForegroundColorSpan(activity.getColor(R.color.accent)), 0, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        value.setSpan(new ForegroundColorSpan(activity.getColor(R.color.text_primary)), 9, value.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(value);
        logo.setImageResource(R.drawable.nm7_main_logo);
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    private void toggleSearch() {
        boolean visible = search.getVisibility() == View.VISIBLE;
        search.setVisibility(visible ? View.GONE : View.VISIBLE);
        if (!visible) {
            search.requestFocus();
            android.view.inputmethod.InputMethodManager imm =
                    (android.view.inputmethod.InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(search, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void openYouTube() {
        Intent intent = new Intent(activity, SmartTubeHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        activity.startActivity(intent);
    }

    private void renderCurrent() {
        if (selectedTab == 1) renderLiveEvents();
        else if (selectedTab == 2) renderChannels();
        else if (selectedTab == 4) renderPlaylists();
    }

    private List<Channel> channels() {
        List<Channel> value = channelSupplier.get();
        return value == null ? empty : value;
    }

    private List<Channel> eventChannels() {
        List<Channel> result = new ArrayList<>();
        for (Channel c : channels()) {
            if (isEvent(c) && matchesSearch(c)) result.add(c);
        }
        if (!selectedEventGroup.isEmpty()) {
            result.removeIf(c -> !selectedEventGroup.equals(c.group()));
        }
        result.removeIf(c -> {
            int state = eventState(c);
            return eventStatus == 1 ? state != 1
                    : eventStatus == 2 ? state != 2
                    : eventStatus == 3 ? state != 2
                    : eventStatus == 4 && state != 3;
        });
        return result;
    }

    private List<Channel> channelList() {
        List<Channel> result = new ArrayList<>();
        for (Channel c : channels()) {
            if (!matchesSearch(c)) continue;
            if (!selectedChannelGroup.isEmpty() && !selectedChannelGroup.equals(c.group())) continue;
            result.add(c);
        }
        return result;
    }

    private boolean matchesSearch(Channel c) {
        if (searchQuery.isEmpty()) return true;
        String hay = (c.name() + " " + c.group()).toLowerCase(Locale.ROOT);
        return hay.contains(searchQuery);
    }

    private boolean isEvent(Channel c) {
        String n = c.name() == null ? "" : c.name().trim().toLowerCase(Locale.ROOT);
        if (n.contains("http://") || n.contains("https://")) return false;

        boolean matchDelimiter = n.matches("(?s).*\\s(vs\\.?|v|@)\\s.*");
        boolean hasDate = n.matches("(?s).*\\b[0-3]?\\d[/-][01]?\\d[/-](?:20)?\\d{2}\\b.*");
        boolean hasTime = n.matches("(?s).*\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b.*");
        boolean explicitLive = n.startsWith("live ") || n.startsWith("live:") || n.contains(" on air");
        boolean competition = n.contains("match") || n.contains("fixture") || n.contains("qualif")
                || n.contains("league") || n.contains("championship") || n.contains("world cup")
                || n.contains("cup ") || n.contains("final") || n.contains("test series")
                || n.contains(" olympic");
        // A generic TV channel such as "ON Sports HD" is not an event. Prefer
        // match/date/time signals from the playlist itself.
        return matchDelimiter || (hasDate && hasTime) || (explicitLive && (hasTime || competition))
                || (competition && hasDate);
    }

    private int eventState(Channel c) {
        String n = c.name() == null ? "" : c.name().toLowerCase(Locale.ROOT);
        if (n.contains("ended") || n.contains("finished") || n.contains(" full time")) return 3;
        java.util.Date start = parseEventStart(c.name());
        if (start != null) {
            long delta = start.getTime() - System.currentTimeMillis();
            if (delta > 0) return 2;
            // Match entries without explicit duration are considered live for
            // three hours after their scheduled start.
            if (-delta <= 3L * 60L * 60L * 1000L) return 1;
            return 3;
        }
        if (n.contains("live") || n.contains("on air") || n.contains("now")) return 1;
        return 2;
    }

    private java.util.Date parseEventStart(String name) {
        if (name == null) return null;
        java.util.regex.Matcher date = java.util.regex.Pattern
                .compile("\\b([0-3]?\\d)[/-]([01]?\\d)[/-](20?\\d{2})\\b")
                .matcher(name);
        java.util.regex.Matcher time = java.util.regex.Pattern
                .compile("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\s*(AM|PM)?\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(name);
        if (!time.find()) return null;

        java.util.Calendar cal = java.util.Calendar.getInstance();
        if (date.find()) {
            int year = Integer.parseInt(date.group(3));
            if (year < 100) year += 2000;
            cal.set(java.util.Calendar.YEAR, year);
            cal.set(java.util.Calendar.MONTH, Integer.parseInt(date.group(2)) - 1);
            cal.set(java.util.Calendar.DAY_OF_MONTH, Integer.parseInt(date.group(1)));
        }
        int hour = Integer.parseInt(time.group(1));
        int minute = Integer.parseInt(time.group(2));
        String ampm = time.group(3);
        if (ampm != null) {
            boolean pm = ampm.equalsIgnoreCase("PM");
            hour %= 12;
            if (pm) hour += 12;
        }
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
        cal.set(java.util.Calendar.MINUTE, minute);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private List<String> eventGroups() {
        LinkedHashSet<String> groups = new LinkedHashSet<>();
        for (Channel c : channels()) if (isEvent(c)) groups.add(c.group());
        if (groups.isEmpty()) for (Channel c : channels()) groups.add(c.group());
        return new ArrayList<>(groups);
    }

    private void renderLiveEvents() {
        selectedTab = 1;
        content.removeAllViews();

        HorizontalScrollView statusScroll = new HorizontalScrollView(activity);
        statusScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout statuses = row();
        List<Channel> all = new ArrayList<>();
        List<Channel> live = new ArrayList<>();
        List<Channel> upcoming = new ArrayList<>();
        List<Channel> end = new ArrayList<>();
        for (Channel c : channels()) {
            if (!isEvent(c)) continue;
            all.add(c);
            int state = eventState(c);
            if (state == 1) live.add(c);
            else if (state == 2) upcoming.add(c);
            else end.add(c);
        }
        int next24 = 0;
        long now = System.currentTimeMillis();
        for (Channel c : upcoming) {
            java.util.Date start = parseEventStart(c.name());
            if (start != null && start.getTime() - now <= 24L * 60L * 60L * 1000L) next24++;
        }
        addStatusChip(statuses, "✓ All (" + all.size() + ")", 0);
        addStatusChip(statuses, "Live (" + live.size() + ")", 1);
        addStatusChip(statuses, "Upcoming (" + upcoming.size() + ")", 2);
        addStatusChip(statuses, "Next 24h (" + next24 + ")", 3);
        addStatusChip(statuses, "End (" + end.size() + ")", 4);
        statusScroll.addView(statuses);
        content.addView(statusScroll, new LinearLayout.LayoutParams(-1, dp(56)));

        HorizontalScrollView groupsScroll = new HorizontalScrollView(activity);
        groupsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout groups = row();
        for (String group : eventGroups()) addEventGroup(groups, group);
        groupsScroll.addView(groups);
        content.addView(groupsScroll, new LinearLayout.LayoutParams(-1, dp(112)));

        ListView list = new ListView(activity);
        list.setDivider(null);
        list.setSelector(android.R.color.transparent);
        list.setPadding(dp(4), dp(2), dp(4), dp(78));
        list.setClipToPadding(false);
        list.setAdapter(new MobileEventAdapter(eventChannels()));
        list.setOnItemClickListener((p, v, i, id) -> {
            Channel c = (Channel) p.getAdapter().getItem(i);
            host.play(c);
        });
        content.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
        updateNav();
    }

    private void addStatusChip(LinearLayout row, String text, int state) {
        Button b = chip(text, state == eventStatus);
        b.setOnClickListener(v -> {
            eventStatus = state;
            renderLiveEvents();
        });
        row.addView(b, lp(dp(120), dp(44), 0));
    }

    private void addEventGroup(LinearLayout row, String group) {
        LinearLayout item = new LinearLayout(activity);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(4), 0, dp(4), 0);
        TextView icon = new TextView(activity);
        icon.setGravity(Gravity.CENTER);
        icon.setText("⚽");
        icon.setTextSize(25);
        icon.setTextColor(activity.getColor(R.color.text_primary));
        icon.setBackground(circleBackground(false));
        String[] sample = group == null ? new String[0] : group.split("\\s+");
        String initials = sample.length == 0 ? "★" : sample[0].substring(0, 1).toUpperCase(Locale.ROOT);
        TextView badge = badge(String.valueOf(countGroup(group)));
        FrameLayout circle = new FrameLayout(activity);
        circle.addView(icon, new FrameLayout.LayoutParams(dp(62), dp(62)));
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.END | Gravity.TOP);
        circle.addView(badge, badgeLp);
        item.addView(circle);
        TextView label = text(group == null ? "Group" : group, 10, R.color.text_primary);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        item.addView(label, new LinearLayout.LayoutParams(dp(70), dp(24)));
        item.setOnClickListener(v -> {
            selectedEventGroup = group;
            renderLiveEvents();
        });
        row.addView(item, lp(dp(78), dp(106), 0));
    }

    private int countGroup(String group) {
        int n = 0;
        for (Channel c : channels()) if (isEvent(c) && group.equals(c.group())) n++;
        return n;
    }

    private void renderChannels() {
        selectedTab = 2;
        content.removeAllViews();
        List<Channel> values = channelList();

        if (!values.isEmpty()) {
            content.addView(featured(values.get(0)), new LinearLayout.LayoutParams(-1, dp(238)));
        }

        HorizontalScrollView filterScroll = new HorizontalScrollView(activity);
        filterScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filters = row();
        addChannelFilter(filters, "All (" + values.size() + ")", "");
        int fav = 0;
        for (Channel c : channels()) if (AppPreferences.isFavorite(activity, c)) fav++;
        addChannelFilter(filters, "♥ Favourites (" + fav + ")", "__fav__");
        Set<String> groupSet = new LinkedHashSet<>();
        for (Channel c : channels()) groupSet.add(c.group());
        for (String group : groupSet) addChannelFilter(filters, group + " (" + countChannels(group) + ")", group);
        filterScroll.addView(filters);
        content.addView(filterScroll, new LinearLayout.LayoutParams(-1, dp(64)));

        ListView grid = new ListView(activity);
        grid.setDivider(null);
        grid.setSelector(android.R.color.transparent);
        grid.setPadding(dp(4), dp(2), dp(4), dp(86));
        grid.setClipToPadding(false);
        columns = Math.max(2, Math.min(4, activity.getResources().getDisplayMetrics().widthPixels / dp(136)));
        grid.setAdapter(new MobileChannelGridAdapter(values));
        grid.setOnItemClickListener((p, v, i, id) -> {
            MobileChannelGridAdapter a = (MobileChannelGridAdapter) p.getAdapter();
            Channel c = a.channelForCell(i, 0);
            if (c != null) host.play(c);
        });
        content.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));
        updateNav();
    }

    private void addChannelFilter(LinearLayout row, String label, String group) {
        boolean selected = "__fav__".equals(group)
                ? selectedChannelGroup.equals("__fav__")
                : selectedChannelGroup.equals(group);
        Button b = chip(label, selected);
        b.setOnClickListener(v -> {
            selectedChannelGroup = group;
            renderChannels();
        });
        row.addView(b, lp(dp(Math.max(112, 14 + label.length() * 8)), dp(48), 0));
    }

    private int countChannels(String group) {
        int count = 0;
        for (Channel c : channels()) if (group.equals(c.group())) count++;
        return count;
    }

    private View featured(Channel c) {
        FrameLayout root = new FrameLayout(activity);
        root.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable bg = rounded(Color.WHITE, Color.TRANSPARENT, 2, 22);
        root.setBackground(bg);

        ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        MobileLogoLoader.load(c, image);
        root.addView(image, new FrameLayout.LayoutParams(-1, -1));

        TextView shade = text(c.name(), 14, R.color.text_primary);
        shade.setTypeface(Typeface.DEFAULT_BOLD);
        shade.setGravity(Gravity.BOTTOM | Gravity.START);
        shade.setPadding(dp(16), 0, dp(16), dp(16));
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        TextView live = text(eventState(c) == 1 ? "LIVE" : "CHANNEL", 11, R.color.text_primary);
        live.setGravity(Gravity.CENTER);
        live.setBackground(rounded(eventState(c) == 1 ? 0xffee3e65 : 0xff38506c, 0, 1, 14));
        FrameLayout.LayoutParams liveLp = new FrameLayout.LayoutParams(dp(64), dp(42), Gravity.TOP | Gravity.START);
        liveLp.setMargins(dp(14), dp(14), 0, 0);
        root.addView(live, liveLp);

        Button play = new Button(activity);
        play.setText("▶");
        play.setTextSize(20);
        play.setTextColor(activity.getColor(R.color.navy));
        play.setGravity(Gravity.CENTER);
        play.setBackground(rounded(activity.getColor(R.color.accent), 0, 0, 50));
        play.setPadding(0, 0, 0, 0);
        play.setOnClickListener(v -> host.play(c));
        FrameLayout.LayoutParams playLp = new FrameLayout.LayoutParams(dp(58), dp(58), Gravity.END | Gravity.BOTTOM);
        playLp.setMargins(0, 0, dp(12), dp(10));
        root.addView(play, playLp);
        return root;
    }

    private void renderPlaylists() {
        selectedTab = 4;
        content.removeAllViews();

        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(14), dp(16), dp(10));

        LinearLayout appBar = row();
        TextView back = text("‹", 36, R.color.text_primary);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> renderLiveEvents());
        appBar.addView(back, lp(dp(44), dp(54), 0));
        TextView titleView = text("Playlists/IPTV", 20, R.color.text_primary);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        appBar.addView(titleView, lp(0, dp(54), 1f));
        TextView more = text("⋮", 28, R.color.text_primary);
        more.setGravity(Gravity.CENTER);
        more.setOnClickListener(v -> showPlaylistMenu());
        appBar.addView(more, lp(dp(44), dp(54), 0));
        page.addView(appBar);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);

        body.addView(sectionTitle("OWNER PLAYLISTS", "Managed by Phuongnm7 IPTV"));
        body.addView(ownerCard("NM7 IPTV", "Official • Owner playlist", true));

        body.addView(sectionTitle("MY PLAYLISTS", "Added on this device"));
        List<PlaylistSourceStore.Source> urls = PlaylistSourceStore.load(activity);
        for (int i = 0; i < urls.size(); i++) {
            final int index = i;
            PlaylistSourceStore.Source source = urls.get(i);
            body.addView(urlCard(source, index));
        }
        for (MobilePlaylistStore.Entry local : MobilePlaylistStore.load(activity)) {
            body.addView(localCard(local));
        }
        if (urls.isEmpty() && MobilePlaylistStore.load(activity).isEmpty()) {
            TextView emptyView = text("Chưa có playlist trên thiết bị. Nhấn + để thêm M3U URL hoặc tệp.", 13, R.color.text_secondary);
            emptyView.setPadding(dp(8), dp(24), dp(8), dp(24));
            body.addView(emptyView);
        }

        scroll.addView(body);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button add = new Button(activity);
        add.setText("+");
        add.setTextSize(24);
        add.setTextColor(activity.getColor(R.color.navy));
        add.setBackground(rounded(activity.getColor(R.color.accent), 0, 0, 50));
        add.setOnClickListener(v -> addPlaylistDialog());
        FrameLayout bottom = new FrameLayout(activity);
        bottom.addView(page, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams addLp = new FrameLayout.LayoutParams(dp(64), dp(64), Gravity.END | Gravity.BOTTOM);
        addLp.setMargins(0, 0, dp(16), dp(78));
        bottom.addView(add, addLp);
        content.addView(bottom, new LinearLayout.LayoutParams(-1, 0, 1f));
        updateNav();
    }

    private View sectionTitle(String header, String sub) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(6), dp(14), 0, dp(10));
        TextView h = text(header, 12, R.color.accent);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        TextView s = text(sub, 12, R.color.text_secondary);
        box.addView(h);
        box.addView(s);
        TextView result = text("", 1, R.color.text_primary);
        // Use a horizontal container in a single TextView is not supported, return the header
        // to keep the helper lightweight.
        return h;
    }

    private View ownerCard(String name, String sub, boolean clickable) {
        LinearLayout card = playlistCard();
        ImageView icon = icon(R.drawable.nm7_main_logo, 58);
        card.addView(icon, lp(dp(72), dp(72), 0));
        LinearLayout textBox = new LinearLayout(activity);
        textBox.setOrientation(LinearLayout.VERTICAL);
        TextView n = text(name, 17, R.color.text_primary);
        n.setTypeface(Typeface.DEFAULT_BOLD);
        TextView s = text(sub, 12, R.color.accent);
        textBox.addView(n);
        textBox.addView(s);
        card.addView(textBox, lp(0, -2, 1f));
        TextView play = text("▶", 22, R.color.accent);
        play.setGravity(Gravity.CENTER);
        card.addView(play, lp(dp(50), -2, 0));
        if (clickable) card.setOnClickListener(v -> host.loadUrl(PlaylistSourceStore.DEFAULT_URL));
        return card;
    }

    private View urlCard(PlaylistSourceStore.Source source, int index) {
        LinearLayout card = playlistCard();
        ImageView icon = icon(R.drawable.nm7_nav_library, 50);
        card.addView(icon, lp(dp(70), dp(72), 0));
        LinearLayout textBox = new LinearLayout(activity);
        textBox.setOrientation(LinearLayout.VERTICAL);
        TextView n = text(source.name, 17, R.color.text_primary);
        n.setTypeface(Typeface.DEFAULT_BOLD);
        TextView u = text(source.url, 11, R.color.text_secondary);
        u.setMaxLines(2);
        textBox.addView(n);
        textBox.addView(u);
        card.addView(textBox, lp(0, -2, 1f));

        TextView edit = text("✎", 24, R.color.accent);
        edit.setGravity(Gravity.CENTER);
        edit.setOnClickListener(v -> editUrlSource(source, index));
        card.addView(edit, lp(dp(44), -2, 0));

        TextView remove = text("▮", 22, 0xffff3e63);
        remove.setGravity(Gravity.CENTER);
        remove.setOnClickListener(v -> confirmRemoveUrl(source, index));
        card.addView(remove, lp(dp(40), -2, 0));

        card.setOnClickListener(v -> host.loadUrl(source.url));
        return card;
    }

    private View localCard(MobilePlaylistStore.Entry entry) {
        LinearLayout card = playlistCard();
        ImageView icon = icon(R.drawable.nm7_nav_library, 50);
        card.addView(icon, lp(dp(70), dp(72), 0));
        LinearLayout textBox = new LinearLayout(activity);
        textBox.setOrientation(LinearLayout.VERTICAL);
        TextView n = text(entry.name, 17, R.color.text_primary);
        n.setTypeface(Typeface.DEFAULT_BOLD);
        TextView u = text("Local playlist file", 12, R.color.text_secondary);
        textBox.addView(n);
        textBox.addView(u);
        card.addView(textBox, lp(0, -2, 1f));

        TextView edit = text("✎", 24, 0xff3db7ff);
        edit.setGravity(Gravity.CENTER);
        edit.setOnClickListener(v -> renameLocal(entry));
        card.addView(edit, lp(dp(44), -2, 0));

        TextView remove = text("▮", 22, 0xffff3e63);
        remove.setGravity(Gravity.CENTER);
        remove.setOnClickListener(v -> {
            MobilePlaylistStore.remove(activity, entry.id);
            renderPlaylists();
        });
        card.addView(remove, lp(dp(40), -2, 0));

        card.setOnClickListener(v -> host.loadLocalEntry(entry));
        return card;
    }

    private LinearLayout playlistCard() {
        LinearLayout card = row();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(8), dp(8), dp(8));
        card.setBackground(rounded(Color.TRANSPARENT, 0xff263955, 1, 18));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(88));
        p.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(p);
        return card;
    }

    private void addPlaylistDialog() {
        String[] choices = {"M3U URL", "Tệp playlist trên máy"};
        new AlertDialog.Builder(activity)
                .setTitle("Thêm playlist")
                .setItems(choices, (d, which) -> {
                    if (which == 0) {
                        final EditText name = new EditText(activity);
                        name.setHint("Tên playlist");
                        final EditText url = new EditText(activity);
                        url.setHint("https://.../playlist.m3u");
                        url.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
                        LinearLayout form = new LinearLayout(activity);
                        form.setOrientation(LinearLayout.VERTICAL);
                        form.setPadding(dp(20), dp(6), dp(20), 0);
                        form.addView(name, new LinearLayout.LayoutParams(-1, dp(54)));
                        form.addView(url, new LinearLayout.LayoutParams(-1, dp(54)));
                        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("M3U URL")
                                .setView(form).setPositiveButton("Lưu", null).setNegativeButton("Hủy", null).create();
                        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ok -> {
                            String value = url.getText().toString().trim();
                            try {
                                PlaylistSourceStore.add(activity, name.getText().toString(), value);
                                dialog.dismiss();
                                host.loadUrl(value);
                                renderPlaylists();
                            } catch (Exception e) {
                                url.setError(readable(e));
                            }
                        }));
                        dialog.show();
                    } else {
                        host.openLocalPlaylistPicker();
                    }
                }).show();
    }

    private void editUrlSource(PlaylistSourceStore.Source source, int index) {
        final EditText name = new EditText(activity);
        name.setText(source.name);
        final EditText url = new EditText(activity);
        url.setText(source.url);
        url.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        LinearLayout form = new LinearLayout(activity);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(20), dp(6), dp(20), 0);
        form.addView(name, new LinearLayout.LayoutParams(-1, dp(54)));
        form.addView(url, new LinearLayout.LayoutParams(-1, dp(54)));
        AlertDialog dialog = new AlertDialog.Builder(activity).setTitle("Chỉnh sửa playlist")
                .setView(form).setPositiveButton("Lưu", null).setNegativeButton("Hủy", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ok -> {
            try {
                PlaylistSourceStore.update(activity, index, name.getText().toString(), url.getText().toString());
                dialog.dismiss();
                renderPlaylists();
            } catch (Exception e) {
                url.setError(readable(e));
            }
        }));
        dialog.show();
    }

    private void renameLocal(MobilePlaylistStore.Entry entry) {
        EditText field = new EditText(activity);
        field.setText(entry.name);
        field.setSingleLine(true);
        new AlertDialog.Builder(activity).setTitle("Đổi tên playlist").setView(field)
                .setPositiveButton("Lưu", (d, w) -> {
                    try {
                        MobilePlaylistStore.rename(activity, entry.id, field.getText().toString());
                    } catch (Exception ignored) {}
                    renderPlaylists();
                }).setNegativeButton("Hủy", null).show();
    }

    private void confirmRemoveUrl(PlaylistSourceStore.Source source, int index) {
        new AlertDialog.Builder(activity).setTitle("Xóa playlist?")
                .setMessage(source.name + "\n" + source.url)
                .setPositiveButton("Xóa", (d, w) -> {
                    try { PlaylistSourceStore.remove(activity, index); } catch (Exception ignored) {}
                    renderPlaylists();
                }).setNegativeButton("Hủy", null).show();
    }

    private void showPlaylistMenu() {
        new AlertDialog.Builder(activity).setTitle("Playlist")
                .setItems(new String[]{"Thêm playlist", "Làm mới playlist hiện tại", "Network Stream"}, (d, which) -> {
                    if (which == 0) addPlaylistDialog();
                    else if (which == 1) {
                        Channel first = channels().isEmpty() ? null : channels().get(0);
                        if (first != null) host.loadUrl(first.url());
                    } else host.showNetworkStream();
                }).show();
    }

    private void updateNav() {
        setNav(R.id.btnYoutube, 0);
        setNav(R.id.btnLiveEvents, 1);
        setNav(R.id.btnChannel, 2);
        setNav(R.id.btnTvMode, 3);
        setNav(R.id.btnPlaylist, 4);
    }

    private void setNav(int id, int tab) {
        View v = activity.findViewById(id);
        if (v == null) return;
        boolean active = selectedTab == tab;
        v.setBackground(active ? rounded(activity.getColor(R.color.accent_dark), 0, 0, 28)
                : rounded(Color.TRANSPARENT, 0, 0, 28));
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View child = g.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(activity.getColor(active ? R.color.accent : R.color.text_secondary));
                } else if (child instanceof ImageView) {
                    ((ImageView) child).setColorFilter(activity.getColor(active ? R.color.accent : R.color.text_secondary));
                }
            }
        }
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private Button chip(String label, boolean selected) {
        Button b = new Button(activity);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setTextColor(activity.getColor(selected ? R.color.navy : R.color.text_primary));
        b.setBackground(rounded(selected ? activity.getColor(R.color.accent) : R.color.surface_alt,
                selected ? activity.getColor(R.color.accent) : 0xff263955, 1, 26));
        return b;
    }

    private TextView badge(String value) {
        TextView t = text(value, 10, R.color.text_primary);
        t.setGravity(Gravity.CENTER);
        t.setBackground(rounded(0xffed335d, 0, 0, 30));
        return t;
    }

    private TextView text(String value, int sp, int color) {
        TextView t = new TextView(activity);
        t.setText(value == null ? "" : value);
        t.setTextSize(sp);
        t.setTextColor(resolveColor(color));
        return t;
    }

    private ImageView icon(int drawable, int size) {
        ImageView i = new ImageView(activity);
        i.setImageResource(drawable);
        i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        i.setPadding(dp(10), dp(10), dp(10), dp(10));
        i.setBackground(rounded(0xff1b3653, 0, 0, 40));
        return i;
    }

    private LinearLayout.LayoutParams lp(int width, int height, float weight) {
        int w = width == -2 ? LinearLayout.LayoutParams.WRAP_CONTENT : width;
        int h = height == -2 ? LinearLayout.LayoutParams.WRAP_CONTENT : height;
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    private GradientDrawable rounded(int fill, int stroke, int width, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp((int) radius));
        if (width > 0) d.setStroke(dp(width), stroke);
        return d;
    }

    private GradientDrawable circleBackground(boolean selected) {
        return rounded(selected ? activity.getColor(R.color.accent) : 0xff152942, 0xff314963, 1, 50);
    }

    private int resolveColor(int value) {
        int alpha = value & 0xFF000000;
        if (alpha == 0xFF000000 || alpha == 0) return value;
        return activity.getColor(value);
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private String readable(Exception e) {
        return e == null ? "Không rõ nguyên nhân" : e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    /** Four-column lazy channel grid. */
    private final class MobileChannelGridAdapter extends BaseAdapter {
        private final List<Channel> data;
        MobileChannelGridAdapter(List<Channel> data) { this.data = new ArrayList<>(data); }
        @Override public int getCount() { return (data.size() + columns - 1) / columns; }
        @Override public Object getItem(int position) { return data.get(Math.min(data.size() - 1, position * columns)); }
        @Override public long getItemId(int position) { return position; }
        Channel channelForCell(int row, int col) {
            int index = row * columns + col;
            return index >= 0 && index < data.size() ? data.get(index) : null;
        }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(dp(2), dp(2), dp(2), dp(2));
            for (int col = 0; col < columns; col++) {
                Channel c = channelForCell(position, col);
                if (c == null) {
                    View emptyCell = new View(activity);
                    row.addView(emptyCell, new LinearLayout.LayoutParams(0, dp(144), 1f));
                    continue;
                }
                View cell = LayoutInflater.from(activity).inflate(R.layout.item_mobile_channel_cell, row, false);
                ImageView image = cell.findViewById(R.id.mobileChannelLogo);
                TextView fallback = cell.findViewById(R.id.mobileChannelFallback);
                TextView favorite = cell.findViewById(R.id.mobileChannelFavorite);
                TextView name = cell.findViewById(R.id.mobileChannelName);
                name.setText(c.name());
                String first = c.name().trim();
                fallback.setText(first.isEmpty() ? "TV" : first.substring(0, 1).toUpperCase(Locale.ROOT));
                boolean fav = AppPreferences.isFavorite(activity, c);
                favorite.setText(fav ? "♥" : "♡");
                favorite.setTextColor(resolveColor(fav ? 0xffff5477 : R.color.text_secondary));
                favorite.setOnClickListener(v -> {
                    boolean now = AppPreferences.toggleFavorite(activity, c);
                    favorite.setText(now ? "♥" : "♡");
                    renderChannels();
                });
                cell.setOnClickListener(v -> host.play(c));
                MobileLogoLoader.load(c, image, fallback);
                row.addView(cell, new LinearLayout.LayoutParams(0, dp(144), 1f));
            }
            return row;
        }
    }

    /** Event cards mirror the sample: title, two sides, center time/status. */
    private final class MobileEventAdapter extends BaseAdapter {
        private final List<Channel> data;
        MobileEventAdapter(List<Channel> data) { this.data = new ArrayList<>(data); }
        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int position) { return data.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            Channel c = data.get(position);
            String[] teams = splitTeams(c.name());
            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(8), dp(8), dp(8), dp(8));
            card.setBackground(rounded(0xff17263b, eventState(c) == 1 ? 0xffef3d6b : 0xff405673, 1, 18));

            TextView group = text("⚽  " + c.group(), 11, R.color.text_primary);
            group.setTypeface(Typeface.DEFAULT_BOLD);
            group.setGravity(Gravity.CENTER);
            card.addView(group, new LinearLayout.LayoutParams(-1, dp(28)));

            LinearLayout middle = new LinearLayout(activity);
            middle.setGravity(Gravity.CENTER_VERTICAL);
            TextView left = teamView(teams[0]);
            TextView center = text(extractTime(c), 17, 0xff24b8ef);
            center.setTypeface(Typeface.DEFAULT_BOLD);
            center.setGravity(Gravity.CENTER);
            TextView right = teamView(teams[1]);
            middle.addView(left, lp(0, dp(92), 1f));
            middle.addView(center, lp(dp(92), dp(92), 0));
            middle.addView(right, lp(0, dp(92), 1f));
            card.addView(middle);

            String statusText = eventState(c) == 1 ? "● LIVE" : eventState(c) == 3 ? "Ended" : startsText(c);
            TextView status = text(statusText, 11,
                    eventState(c) == 1 ? 0xffff4f72 : R.color.text_secondary);
            status.setGravity(Gravity.CENTER);
            card.addView(status, new LinearLayout.LayoutParams(-1, dp(28)));
            card.setOnClickListener(v -> host.play(c));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(144));
            p.setMargins(0, 0, 0, dp(8));
            card.setLayoutParams(p);
            return card;
        }

        private TextView teamView(String value) {
            TextView t = text(value, 12, R.color.text_primary);
            t.setGravity(Gravity.CENTER);
            t.setTypeface(Typeface.DEFAULT_BOLD);
            return t;
        }

        private String[] splitTeams(String name) {
            String n = name == null ? "" : name.trim();
            n = n.replaceFirst("(?i)^live\\s*[:|-]?\\s*", "");
            String[] split = n.split("(?i)\\s+(?:vs\\.?|v|@)\\s+|\\s+@\\s+|\\s+-\\s+", 2);
            if (split.length == 2) return new String[]{cleanTeam(split[0]), cleanTeam(split[1])};
            if (n.length() > 34) {
                int mid = n.lastIndexOf(' ', 34);
                if (mid > 6) return new String[]{cleanTeam(n.substring(0, mid)), cleanTeam(n.substring(mid + 1))};
            }
            return new String[]{cleanTeam(n), "Live Stream"};
        }

        private String cleanTeam(String value) {
            String v = value == null ? "" : value.trim();
            return v.isEmpty() ? "Team" : v;
        }

        private String extractTime(Channel c) {
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\b([01]?\\d|2[0-3]):[0-5]\\d\\b")
                    .matcher(c.name());
            return m.find() ? m.group() : (eventState(c) == 1 ? "LIVE" : "TBD");
        }

        private String startsText(Channel c) {
            java.util.Date start = parseEventStart(c.name());
            if (start == null) return "Starts soon";
            long delta = start.getTime() - System.currentTimeMillis();
            if (delta <= 0) return "Started";
            long hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(delta);
            long days = hours / 24;
            if (days > 0) return "Starts in " + days + (days == 1 ? " day" : " days");
            if (hours > 0) return "Starts in " + hours + (hours == 1 ? " hour" : " hours");
            long minutes = Math.max(1, java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(delta));
            return "Starts in " + minutes + " min";
        }
    }

    private static final class MobileLogoLoader {
        private static final OkHttpClient HTTP = new OkHttpClient.Builder()
                .connectTimeout(4, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
        private static final Handler MAIN = new Handler(Looper.getMainLooper());
        private static final android.util.LruCache<String, Bitmap> CACHE =
                new android.util.LruCache<String, Bitmap>(8 * 1024 * 1024) {
                    @Override protected int sizeOf(String key, Bitmap bitmap) {
                        return Math.max(1, bitmap.getAllocationByteCount());
                    }
                };

        static void load(Channel c, ImageView image) {
            load(c, image, null);
        }

        static void load(Channel c, ImageView image, TextView fallback) {
            if (c == null || image == null) return;
            List<String> urls = c.logoCandidates();
            if (urls.isEmpty()) {
                image.setVisibility(View.GONE);
                if (fallback != null) fallback.setVisibility(View.VISIBLE);
                return;
            }
            String key = urls.get(0);
            Bitmap cached = CACHE.get(key);
            image.setTag(key);
            if (cached != null) {
                image.setImageBitmap(cached);
                image.setVisibility(View.VISIBLE);
                if (fallback != null) fallback.setVisibility(View.GONE);
                return;
            }
            image.setVisibility(View.GONE);
            if (fallback != null) fallback.setVisibility(View.VISIBLE);
            fetch(c, urls, 0, image, fallback);
        }

        private static void fetch(Channel c, List<String> urls, int index, ImageView image, TextView fallback) {
            if (index >= urls.size()) return;
            String raw = urls.get(index);
            if (raw == null || !raw.startsWith("http")) {
                fetch(c, urls, index + 1, image, fallback);
                return;
            }
            HTTP.newCall(new Request.Builder().url(raw).build()).enqueue(new okhttp3.Callback() {
                @Override public void onFailure(okhttp3.Call call, java.io.IOException error) {
                    fetch(c, urls, index + 1, image, fallback);
                }
                @Override public void onResponse(okhttp3.Call call, Response response) {
                    try (Response r = response) {
                        if (!r.isSuccessful() || r.body() == null) {
                            fetch(c, urls, index + 1, image, fallback);
                            return;
                        }
                        InputStream stream = r.body().byteStream();
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int read;
                        int total = 0;
                        while ((read = stream.read(buf)) != -1 && total < 2 * 1024 * 1024) {
                            out.write(buf, 0, read);
                            total += read;
                        }
                        Bitmap bitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
                        if (bitmap == null) {
                            fetch(c, urls, index + 1, image, fallback);
                            return;
                        }
                        CACHE.put(raw, bitmap);
                        MAIN.post(() -> {
                            Object tag = image.getTag();
                            if (raw.equals(tag)) {
                                image.setImageBitmap(bitmap);
                                image.setVisibility(View.VISIBLE);
                                if (fallback != null) fallback.setVisibility(View.GONE);
                            }
                        });
                    } catch (Exception e) {
                        fetch(c, urls, index + 1, image, fallback);
                    }
                }
            });
        }
    }
}
