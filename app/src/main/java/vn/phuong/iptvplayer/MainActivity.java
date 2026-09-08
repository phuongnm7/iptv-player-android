package vn.phuong.iptvplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;

@androidx.media3.common.util.UnstableApi
public final class MainActivity extends Activity {
    private static final int OPEN_M3U = 101;
    private static final int SAVE_M3U = 102;
    private static final int PICK_WALLPAPER = 103;
    private static final int MAX_PLAYLIST_BYTES = 8 * 1024 * 1024;

    private final ExecutorService io = SessionStore.IO;
    private final M3uParser parser = new M3uParser();
    private final List<Channel> allChannels = new ArrayList<>();
    private ChannelAdapter adapter;
    private EditText inputUrl;
    private EditText inputSearch;
    private Spinner spinnerGroup;
    private TextView txtSource;
    private TextView txtSummary;
    private TextView txtEmpty;
    private ProgressBar progress;
    private int duplicateCount;
    private int missingUrlCount;
    private String currentSource = "";
    private boolean loading;
    private boolean importExpanded = true;
    private int wallpaperGeneration;
    private int activeSection;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupViews();
        restoreSession();
    }

    private void setupViews() {
        setContentView(R.layout.activity_main);
        Insets.apply(findViewById(R.id.mainRoot));
        applyWallpaper();

        inputUrl = findViewById(R.id.inputUrl);
        inputSearch = findViewById(R.id.inputSearch);
        spinnerGroup = findViewById(R.id.spinnerGroup);
        txtSource = findViewById(R.id.txtSource);
        txtSummary = findViewById(R.id.txtSummary);
        txtEmpty = findViewById(R.id.txtEmpty);
        progress = findViewById(R.id.progress);
        ListView list = findViewById(R.id.listChannels);

        adapter = new ChannelAdapter(this, new ChannelAdapter.Listener() {
            @Override public void onSelectionChanged() { updateSummary(); }
            @Override public void onFavoriteChanged(Channel channel, boolean favorite) {
                toast(favorite ? "Đã thêm vào Yêu thích" : "Đã bỏ khỏi Yêu thích");
                if (activeSection == 1) filter(); else adapter.notifyDataSetChanged();
            }
        });
        list.setAdapter(adapter);
        list.setEmptyView(txtEmpty);
        list.setOnItemClickListener((parent, view, position, id) -> play(adapter.getItem(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            showChannelActions(adapter.getItem(position));
            return true;
        });

        findViewById(R.id.btnLoadUrl).setOnClickListener(v -> loadFromUrl());
        findViewById(R.id.btnOpenFile).setOnClickListener(v -> openFilePicker());
        findViewById(R.id.btnSelectAll).setOnClickListener(v -> setVisibleSelection(true));
        findViewById(R.id.btnSelectNone).setOnClickListener(v -> setVisibleSelection(false));
        findViewById(R.id.btnExport).setOnClickListener(v -> exportFile());
        findViewById(R.id.btnPlayUrl).setOnClickListener(v -> playDirect());
        findViewById(R.id.btnSources).setOnClickListener(v -> setImportExpanded(!importExpanded));
        findViewById(R.id.btnWallpaper).setOnClickListener(v -> showSettings());
        findViewById(R.id.btnAllChannels).setOnClickListener(v -> selectSection(0));
        findViewById(R.id.btnFavorites).setOnClickListener(v -> selectSection(1));
        findViewById(R.id.btnRecent).setOnClickListener(v -> selectSection(2));
        findViewById(R.id.btnClearFilters).setOnClickListener(v -> {
            inputSearch.setText(""); spinnerGroup.setSelection(0); filter();
        });
        findViewById(R.id.btnAbout).setOnClickListener(v -> showAbout());
        txtSource.setOnClickListener(v -> showSource(currentSource));

        inputSearch.addTextChangedListener(new SimpleTextWatcher(this::filter));
        spinnerGroup.setOnItemSelectedListener(new SimpleItemSelectedListener(this::filter));
        setImportExpanded(allChannels.isEmpty());
        updateSectionButtons();
    }

    private void restoreSession() {
        setLoading(true);
        io.execute(() -> {
            try {
                SessionStore.State state = SessionStore.load(getApplicationContext());
                ui(() -> {
                    if (state != null) {
                        showPlaylist(state.result, state.source);
                        if (state.source.startsWith("http")) inputUrl.setText(state.source.split("\n")[0]);
                    }
                    setLoading(false);
                });
            } catch (Exception error) {
                ui(() -> showError("Không khôi phục được phiên trước. Bạn có thể mở lại playlist."));
            }
        });
    }

    @Override public void onConfigurationChanged(android.content.res.Configuration config) {
        super.onConfigurationChanged(config);
        String urlText = inputUrl.getText().toString(), query = inputSearch.getText().toString();
        String group = spinnerGroup.getSelectedItemPosition() > 0 ? spinnerGroup.getSelectedItem().toString() : "";
        boolean wasLoading = loading;
        boolean wasExpanded = importExpanded;
        int section = activeSection;
        setupViews();
        activeSection = section;
        rebuildGroups();
        inputUrl.setText(urlText);
        inputSearch.setText(query);
        txtSource.setText(currentSource.isEmpty() ? getString(R.string.source_none) : "Nguồn: " + currentSource);
        for (int i = 1; i < spinnerGroup.getCount(); i++) {
            if (spinnerGroup.getItemAtPosition(i).toString().equals(group)) { spinnerGroup.setSelection(i); break; }
        }
        filter();
        setLoading(wasLoading);
        setImportExpanded(wasExpanded);
        updateSectionButtons();
    }

    private void loadFromUrl() {
        String source = inputUrl.getText().toString().trim();
        if (!M3uParser.isNetworkUrl(source)
                || !(source.startsWith("http://") || source.startsWith("https://"))) {
            toast("URL phải bắt đầu bằng http:// hoặc https://");
            return;
        }
        setLoading(true);
        io.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(source).openConnection();
                connection.setConnectTimeout(15_000);
                connection.setReadTimeout(20_000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "IPTV-Player/1.5 Android");
                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) throw new Exception("HTTP " + status);
                String effective = connection.getURL().toString();
                String type = connection.getContentType();
                String description = source.equals(effective) ? source : source + "\nChuyển hướng: " + effective;
                if (type != null && (type.startsWith("video/") || type.contains("dash+xml"))) {
                    Channel direct = new Channel("Luồng trực tiếp", "Phát trực tiếp", effective, "", "",
                            java.util.Collections.emptyMap());
                    if (type.contains("dash+xml")) direct.options().add("#KODIPROP:inputstream.adaptive.manifest_type=mpd");
                    M3uParser.Result result = new M3uParser.Result(java.util.Collections.singletonList(direct), 0, 0);
                    ui(() -> showPlaylist(result, description));
                    return;
                }
                String content;
                try (InputStream stream = new BufferedInputStream(connection.getInputStream())) {
                    content = readText(stream);
                }
                M3uParser.Result result = parser.parse(content, effective);
                ui(() -> showPlaylist(result, description));
            } catch (Exception error) {
                ui(() -> showError("Không tải được playlist: " + readable(error)));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, OPEN_M3U);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == OPEN_M3U) readLocalFile(uri);
        if (requestCode == SAVE_M3U) writeExport(uri);
        if (requestCode == PICK_WALLPAPER) {
            io.execute(() -> {
                try {
                    WallpaperStore.importPhoto(getApplicationContext(), uri);
                    ui(() -> { applyWallpaper(); toast("Đã đổi hình nền"); });
                } catch (Exception error) { ui(() -> toast("Không mở được hình nền: " + readable(error))); }
            });
        }
    }

    private void readLocalFile(Uri uri) {
        setLoading(true);
        io.execute(() -> {
            try (InputStream stream = getContentResolver().openInputStream(uri)) {
                if (stream == null) throw new Exception("Không thể mở tệp");
                String content = readText(stream);
                M3uParser.Result result = parser.parse(content, "");
                ui(() -> showPlaylist(result, uri.toString()));
            } catch (Exception error) {
                ui(() -> showError("Không đọc được tệp: " + readable(error)));
            }
        });
    }

    private String readText(InputStream stream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int total = 0;
        int read;
        while ((read = stream.read(chunk)) != -1) {
            total += read;
            if (total > MAX_PLAYLIST_BYTES) throw new Exception("Playlist lớn hơn 8 MB. Với link video, dùng Phát URL.");
            buffer.write(chunk, 0, read);
        }
        return buffer.toString(StandardCharsets.UTF_8.name());
    }

    private void showPlaylist(M3uParser.Result result, String source) {
        if (result.channels.isEmpty()) {
            setLoading(false);
            new AlertDialog.Builder(this).setTitle("Không có kênh hợp lệ")
                    .setMessage("Không thay thế playlist đang mở. Phát hiện "
                            + result.missingUrlCount + " mục thiếu hoặc sai URL.")
                    .setPositiveButton("Đóng", null).show();
            return;
        }
        allChannels.clear();
        allChannels.addAll(result.channels);
        duplicateCount = result.duplicateCount;
        missingUrlCount = result.missingUrlCount;
        currentSource = source;
        txtSource.setText("Nguồn: " + source);
        inputSearch.setText("");
        rebuildGroups();
        filter();
        setLoading(false);
        setImportExpanded(false);
        saveSession();
    }

    private void rebuildGroups() {
        String all = getString(R.string.all_groups);
        Set<String> unique = new LinkedHashSet<>();
        for (Channel channel : allChannels) unique.add(channel.group());
        List<String> groups = new ArrayList<>(unique);
        groups.sort(String.CASE_INSENSITIVE_ORDER);
        groups.add(0, all);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, groups);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerGroup.setAdapter(spinnerAdapter);
    }

    private void filter() {
        if (adapter == null) return;
        String query = inputSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        String group = spinnerGroup.getSelectedItem() == null
                ? getString(R.string.all_groups) : spinnerGroup.getSelectedItem().toString();
        List<Channel> filtered = new ArrayList<>();
        for (Channel channel : allChannels) {
            boolean groupMatches = spinnerGroup.getSelectedItemPosition() <= 0 || channel.group().equals(group);
            boolean sectionMatches = activeSection == 0
                    || (activeSection == 1 && AppPreferences.isFavorite(this, channel))
                    || (activeSection == 2 && AppPreferences.isRecent(this, channel));
            boolean queryMatches = query.isEmpty()
                    || channel.name().toLowerCase(Locale.ROOT).contains(query)
                    || channel.group().toLowerCase(Locale.ROOT).contains(query)
                    || channel.url().toLowerCase(Locale.ROOT).contains(query);
            if (groupMatches && sectionMatches && queryMatches) filtered.add(channel);
        }
        if (activeSection == 2) filtered.sort((left, right) -> Integer.compare(
                AppPreferences.recentRank(this, left), AppPreferences.recentRank(this, right)));
        adapter.submit(filtered);
        updateSummary();
    }

    private void setVisibleSelection(boolean selected) {
        for (int i = 0; i < adapter.getCount(); i++) adapter.getItem(i).setSelected(selected);
        adapter.notifyDataSetChanged();
        updateSummary();
    }

    private void updateSummary() {
        int selected = 0;
        for (Channel channel : allChannels) if (channel.selected()) selected++;
        txtSummary.setText(adapter.getCount() + "/" + allChannels.size() + " kênh • " + selected + " đã chọn\n"
                + duplicateCount + " trùng đã bỏ • " + missingUrlCount + " thiếu/sai URL đã bỏ");
    }

    private void play(Channel channel) {
        AppPreferences.recordRecent(this, channel);
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra(PlayerActivity.EXTRA_NAME, channel.name());
        intent.putExtra(PlayerActivity.EXTRA_URL, channel.url());
        intent.putExtra(PlayerActivity.EXTRA_USER_AGENT, channel.headers().get("User-Agent"));
        intent.putExtra(PlayerActivity.EXTRA_REFERER, channel.headers().get("Referer"));
        intent.putExtra(PlayerActivity.EXTRA_ORIGIN, channel.headers().get("Origin"));
        Bundle headers = new Bundle();
        for (java.util.Map.Entry<String, String> entry : channel.headers().entrySet()) headers.putString(entry.getKey(), entry.getValue());
        intent.putExtra(PlayerActivity.EXTRA_HEADERS, headers);
        intent.putExtra(PlayerActivity.EXTRA_MIME, channel.mimeHint());
        intent.putStringArrayListExtra(PlayerActivity.EXTRA_OPTIONS, new ArrayList<>(channel.options()));
        startActivity(intent);
    }

    private void selectSection(int section) {
        activeSection = section;
        updateSectionButtons();
        filter();
    }

    private void updateSectionButtons() {
        int[] ids = {R.id.btnAllChannels, R.id.btnFavorites, R.id.btnRecent};
        for (int i = 0; i < ids.length; i++) {
            View button = findViewById(ids[i]);
            button.setAlpha(i == activeSection ? 1f : 0.62f);
            button.setSelected(i == activeSection);
        }
        if (txtEmpty != null) txtEmpty.setText(activeSection == 1
                ? "Chưa có kênh yêu thích.\nBấm ☆ trên một kênh để thêm."
                : activeSection == 2 ? "Chưa có kênh đã xem gần đây."
                : "Chưa có kênh phù hợp.\nMở nguồn hoặc bấm Bỏ lọc.");
    }

    private void showChannelActions(Channel channel) {
        boolean favorite = AppPreferences.isFavorite(this, channel);
        new AlertDialog.Builder(this).setTitle(channel.name())
                .setItems(new String[]{favorite ? "★ Bỏ khỏi Yêu thích" : "☆ Thêm vào Yêu thích",
                        channel.selected() ? "Bỏ chọn khi xuất" : "Giữ khi xuất", "Xem URL nguồn đầy đủ", "Phát kênh"},
                        (dialog, which) -> {
                            if (which == 0) {
                                boolean added = AppPreferences.toggleFavorite(this, channel);
                                toast(added ? "Đã thêm vào Yêu thích" : "Đã bỏ khỏi Yêu thích"); filter();
                            } else if (which == 1) {
                                channel.setSelected(!channel.selected()); adapter.notifyDataSetChanged(); updateSummary();
                            } else if (which == 2) showSource(channel.url());
                            else play(channel);
                        }).setNegativeButton("Đóng", null).show();
    }

    private void showSettings() {
        String urls = AppPreferences.showUrls(this) ? "Ẩn URL trong danh sách" : "Hiện URL trong danh sách";
        String rows = AppPreferences.compactRows(this) ? "Hàng kênh thoải mái" : "Hàng kênh thu gọn";
        String fps = AppPreferences.showFps(this) ? "Ẩn FPS khi xem" : "Hiện FPS khi xem";
        String clock = AppPreferences.showClock(this) ? "Ẩn đồng hồ khi xem" : "Hiện đồng hồ khi xem";
        new AlertDialog.Builder(this).setTitle("Tùy chọn ứng dụng")
                .setItems(new String[]{"Đổi hình nền", urls, rows, fps, clock, "Xóa lịch sử Gần đây"},
                        (dialog, which) -> {
                            if (which == 0) chooseWallpaper();
                            if (which == 1) { AppPreferences.setShowUrls(this, !AppPreferences.showUrls(this)); adapter.notifyDataSetChanged(); }
                            if (which == 2) { AppPreferences.setCompactRows(this, !AppPreferences.compactRows(this)); adapter.notifyDataSetChanged(); }
                            if (which == 3) AppPreferences.setShowFps(this, !AppPreferences.showFps(this));
                            if (which == 4) AppPreferences.setShowClock(this, !AppPreferences.showClock(this));
                            if (which == 5) { AppPreferences.clearRecent(this); if (activeSection == 2) filter(); toast("Đã xóa lịch sử"); }
                        }).setNegativeButton("Đóng", null).show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this).setTitle("IPTV Player 1.5")
                .setMessage("Giao diện thư viện kênh gồm Tất cả, Yêu thích và Gần đây; nhấn giữ kênh để mở menu.\n\n"
                        + "HLS, DASH, SmoothStreaming, RTSP, HTTP/HTTPS, RTMP và UDP MPEG-TS. Full HD, 2K và 4K phụ thuộc nguồn, codec và thiết bị.\n\n"
                        + "Widevine và ClearKey chỉ dùng cấu hình/giấy phép hợp lệ của nguồn. Playlist và tùy chọn lưu riêng trên thiết bị; không quảng cáo hay theo dõi.")
                .setPositiveButton("Đóng", null).show();
    }

    private void playDirect() {
        String url = inputUrl.getText().toString().trim();
        if (!M3uParser.isNetworkUrl(url.split("\\|", 2)[0])) {
            toast("Hãy dán một URL luồng hợp lệ trước khi bấm Phát URL.");
            return;
        }
        M3uParser.Result result = parser.parse(url, "");
        if (result.channels.isEmpty()) {
            toast("Dán URL luồng vào ô phía trên trước khi bấm Phát URL.");
            return;
        }
        play(result.channels.get(0));
    }

    private void showSource(String source) {
        if (source.isEmpty()) return;
        TextView view = new TextView(this);
        view.setText(source);
        view.setTextIsSelectable(true);
        view.setPadding(24, 16, 24, 16);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        scroll.addView(view);
        new AlertDialog.Builder(this).setTitle("URL nguồn đầy đủ").setView(scroll)
                .setPositiveButton("Sao chép", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("URL IPTV", source));
                    toast("Đã sao chép URL. URL có thể chứa token, không chia sẻ công khai.");
                }).setNegativeButton("Đóng", null).show();
    }

    private void exportFile() {
        boolean any = false;
        for (Channel channel : allChannels) if (channel.selected()) { any = true; break; }
        if (!any) {
            toast("Hãy chọn ít nhất một kênh");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/x-mpegurl");
        intent.putExtra(Intent.EXTRA_TITLE, "playlist-sach.m3u");
        startActivityForResult(intent, SAVE_M3U);
    }

    private void writeExport(Uri uri) {
        String content = parser.export(allChannels);
        io.execute(() -> {
            try (OutputStream output = getContentResolver().openOutputStream(uri, "wt")) {
                if (output == null) throw new Exception("Không thể tạo tệp");
                output.write(content.getBytes(StandardCharsets.UTF_8));
                ui(() -> toast("Đã lưu playlist sạch"));
            } catch (Exception error) {
                ui(() -> toast("Không lưu được: " + readable(error)));
            }
        });
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        Button load = findViewById(R.id.btnLoadUrl);
        Button open = findViewById(R.id.btnOpenFile);
        load.setEnabled(!loading);
        open.setEnabled(!loading);
        findViewById(R.id.btnPlayUrl).setEnabled(!loading);
    }

    private void showError(String message) {
        setLoading(false);
        toast(message);
    }

    private String readable(Exception error) {
        return error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void ui(Runnable action) {
        runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) action.run(); });
    }

    private void saveSession() {
        if (allChannels.isEmpty()) return;
        List<Channel> snapshot = SessionStore.snapshot(allChannels);
        String source = currentSource;
        int duplicateSnapshot = duplicateCount, missingSnapshot = missingUrlCount;
        io.execute(() -> {
            try { SessionStore.save(getApplicationContext(), snapshot, source, duplicateSnapshot, missingSnapshot); }
            catch (Exception ignored) { ui(() -> toast("Không lưu được phiên; hãy xuất M3U để giữ playlist.")); }
        });
    }

    private void setImportExpanded(boolean expanded) {
        importExpanded = expanded;
        findViewById(R.id.importPanel).setVisibility(expanded ? View.VISIBLE : View.GONE);
        ((Button) findViewById(R.id.btnSources)).setText(expanded ? "Thu gọn" : "+ Nguồn");
    }

    private void chooseWallpaper() {
        new AlertDialog.Builder(this).setTitle("Hình nền")
                .setItems(new String[]{"Xanh đêm", "Biển sâu", "Tím", "Chọn ảnh trên máy"}, (dialog, which) -> {
                    if (which == 3) {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("image/*");
                        startActivityForResult(intent, PICK_WALLPAPER);
                    } else { WallpaperStore.setStyle(this, which); applyWallpaper(); }
                }).setNegativeButton("Đóng", null).show();
    }

    private void applyWallpaper() {
        int generation = ++wallpaperGeneration;
        io.execute(() -> {
            android.graphics.drawable.Drawable background = WallpaperStore.load(getApplicationContext());
            ui(() -> { if (generation == wallpaperGeneration) findViewById(R.id.mainRoot).setBackground(background); });
        });
    }

    @Override protected void onPause() {
        saveSession();
        super.onPause();
    }

    private static final class SimpleTextWatcher implements TextWatcher {
        private final Runnable callback;
        SimpleTextWatcher(Runnable callback) { this.callback = callback; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { callback.run(); }
        @Override public void afterTextChanged(Editable s) { }
    }

    private static final class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable callback;
        SimpleItemSelectedListener(Runnable callback) { this.callback = callback; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { callback.run(); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { callback.run(); }
    }
}
