"""Presentation-only overlay on the verified v39 playback patch."""
from pathlib import Path
import shutil

root = Path('third_party/SmartTube-droid/smarttubedroid/src/main')
ui = root / 'java/com/liskovsoft/smartyoutubetv2/droid/ui'

def replace(text, old, new):
    if text.count(old) != 1:
        raise SystemExit('UI anchor missing/ambiguous: ' + old[:100])
    return text.replace(old, new, 1)

for source in Path('scripts/mobile-ui/res').rglob('*.xml'):
    dest = root / 'res' / source.relative_to('scripts/mobile-ui/res')
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, dest)
for source in Path('scripts/mobile-ui/java').glob('*.java'):
    shutil.copyfile(source, ui / 'shared' / source.name)
p = ui / 'browse/BrowseActivity.java'
s = p.read_text()
s = s.replace('VideoRowsAdapter', 'Nm7FeedAdapter')
s = replace(s, 'mRowsView.setAdapter(mRowsAdapter);', 'mRowsView.setAdapter(mRowsAdapter.adapter);')
s = replace(s, '        toolbar.setTitle("SmartTube Mobile");', '''        findViewById(R.id.nm7_search).setOnClickListener(v -> SearchPresenter.instance(this).startSearch(null));
        findViewById(R.id.nm7_voice).setOnClickListener(v -> SearchPresenter.instance(this).startVoice());
        findViewById(R.id.nm7_account).setOnClickListener(v -> AccountSettingsPresenter.instance(this).show());
        findViewById(R.id.nm7_search).setOnLongClickListener(v -> {
            android.widget.PopupMenu menu = new android.widget.PopupMenu(this, v);
            menu.getMenuInflater().inflate(R.menu.browse_toolbar, menu.getMenu());
            menu.setOnMenuItemClickListener(this::onToolbarItemClicked);
            menu.show();
            return true;
        });''')
s = replace(s, '    private static final int GRID_COLUMNS = 1;', '''    public void nm7OpenLibrary() {
        mBrowsePresenter.selectSection(com.liskovsoft.mediaserviceinterfaces.data.MediaGroup.TYPE_USER_PLAYLISTS);
    }

    public void nm7OpenSettings() {
        mBrowsePresenter.selectSection(com.liskovsoft.mediaserviceinterfaces.data.MediaGroup.TYPE_SETTINGS);
    }

    private static final int GRID_COLUMNS = 1;''')
s = replace(s, '        mGridView = findViewById(R.id.browse_grid);', '''        ((com.liskovsoft.smartyoutubetv2.droid.ui.shared.Nm7SwipeFrameLayout)
                findViewById(R.id.browse_content)).setListener(direction -> {
            int target = mTabLayout.getSelectedTabPosition() + direction;
            if (target >= 0 && target < mTabLayout.getTabCount()) selectSection(target, false);
        });
        mGridView = findViewById(R.id.browse_grid);''')
p.write_text(s)
p = ui / 'shared/VideoCardHolder.java'
s = p.read_text()
s = replace(s, '.inflate(isRow ? R.layout.shared_video_card_row : R.layout.shared_video_card, parent, false);',
    '.inflate(!isRow && parent.getContext() instanceof com.liskovsoft.smartyoutubetv2/droid/ui/browse/BrowseActivity'.replace('/', '.') +
    ' ? R.layout.nm7_video_card : isRow ? R.layout.shared_video_card_row : R.layout.shared_video_card, parent, false);')
s = replace(s, '        if (listener != null) {', '''        View menu = itemView.findViewById(R.id.nm7_card_menu);
        if (menu != null) menu.setOnClickListener(listener == null ? null : v -> listener.onVideoLongClicked(video));
        if (listener != null) {''')
s = replace(s, '        mVideo = null;', '''        mVideo = null;
        View menu = itemView.findViewById(R.id.nm7_card_menu);
        if (menu != null) menu.setOnClickListener(null);''')
s = replace(s, '        Glide.with(context)\n                .load(video.getCardImageUrl())', '''        if (itemView.findViewById(R.id.nm7_card_menu) != null && video.videoId != null
                && video.videoId.matches("[A-Za-z0-9_-]{11}")) {
            // Full-width phone cards need a full-resolution source, not an enlarged TV card.
            RequestOptions options = new RequestOptions().centerCrop()
                    .skipMemoryCache(false).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC);
            String imageRoot = "https://i.ytimg.com/vi/" + video.videoId + "/";
            Glide.with(context).load(imageRoot + "maxresdefault.jpg").apply(options)
                    .placeholder(R.drawable.shared_card_placeholder)
                    .error(Glide.with(context).load(imageRoot + "hq720.jpg").apply(options)
                        .error(Glide.with(context).load(imageRoot + "hqdefault.jpg").apply(options)
                            .error(Glide.with(context).load(video.getCardImageUrl()).apply(options)
                                .error(Glide.with(context).load(video.cardImageUrl).apply(options)
                                    .error(R.drawable.shared_card_placeholder)))))
                    .into(mThumbnail);
            return;
        }
        Glide.with(context)
                .load(video.getCardImageUrl())''')
p.write_text(s)
print('NM7 Mobile v41 Super-style browse presentation applied')
