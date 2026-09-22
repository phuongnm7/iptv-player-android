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

    private static final int GRID_COLUMNS = 1;

    // NM7 1.10.43: observe horizontal section swipes at Activity dispatch level.
    private android.view.View mNm7BrowseContent;
    private float mNm7SwipeDownX;
    private float mNm7SwipeDownY;
    private boolean mNm7SwipeEligible;
    private boolean mNm7SwipeVertical;
    private boolean mNm7SwipeMultiple;
    private int mNm7SwipeSlop;''')
s = replace(s, '        mGridView = findViewById(R.id.browse_grid);', '''        mNm7BrowseContent = findViewById(R.id.browse_content);
        mNm7SwipeSlop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();
        mGridView = findViewById(R.id.browse_grid);''')
s = replace(s, '    public void onBackPressed() {', '''    public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        final int action = event.getActionMasked();

        if (action == android.view.MotionEvent.ACTION_DOWN) {
            mNm7SwipeDownX = event.getX();
            mNm7SwipeDownY = event.getY();
            mNm7SwipeVertical = false;
            mNm7SwipeMultiple = false;
            mNm7SwipeEligible = mNm7BrowseContent != null
                    && mNm7BrowseContent.getVisibility() == android.view.View.VISIBLE
                    && mNm7SwipeDownY >= mNm7BrowseContent.getTop()
                    && mNm7SwipeDownY <= mNm7BrowseContent.getBottom();
        } else if (action == android.view.MotionEvent.ACTION_POINTER_DOWN) {
            mNm7SwipeMultiple = true;
        } else if (action == android.view.MotionEvent.ACTION_MOVE && mNm7SwipeEligible) {
            float dx = Math.abs(event.getX() - mNm7SwipeDownX);
            float dy = Math.abs(event.getY() - mNm7SwipeDownY);
            if (dy > mNm7SwipeSlop && dy >= dx) {
                mNm7SwipeVertical = true;
            }
        } else if (action == android.view.MotionEvent.ACTION_UP && mNm7SwipeEligible) {
            float dx = event.getX() - mNm7SwipeDownX;
            float dy = event.getY() - mNm7SwipeDownY;
            float density = getResources().getDisplayMetrics().density;
            float threshold = Math.max(mNm7SwipeSlop * 3f,
                    Math.min(mNm7BrowseContent.getWidth() * 0.14f, 56f * density));

            if (!mNm7SwipeMultiple && !mNm7SwipeVertical
                    && Math.abs(dx) >= threshold
                    && Math.abs(dx) > Math.abs(dy) * 1.20f
                    && mTabLayout != null && mTabLayout.getTabCount() > 0) {
                int direction = dx < 0 ? 1 : -1;
                if (mNm7BrowseContent.getLayoutDirection() == android.view.View.LAYOUT_DIRECTION_RTL) {
                    direction = -direction;
                }
                int target = mTabLayout.getSelectedTabPosition() + direction;
                if (target >= 0 && target < mTabLayout.getTabCount()) {
                    selectSection(target, false);
                }
            }
            mNm7SwipeEligible = false;
        } else if (action == android.view.MotionEvent.ACTION_CANCEL) {
            mNm7SwipeEligible = false;
        }

        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        try {
            if (com.liskovsoft.smartyoutubetv2.droid.ui.playback.PlaybackActivity.consumeNm7BrowseBack()) {
                return;
            }
        } catch (RuntimeException error) {
            android.util.Log.e("NM7Playback", "Browse Back mini close failed", error);
        }''')

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
        if (menu != null) menu.setOnClickListener(null);
        View avatar = itemView.findViewById(R.id.shared_card_avatar);
        if (avatar instanceof android.widget.ImageView) ((android.widget.ImageView) avatar).setImageResource(R.drawable.browse_ic_account);''')
s = replace(s, '        Glide.with(context)\n                .load(video.getCardImageUrl())', '''        // NM7 channel avatar: SmartTube model fields changed across releases. Resolve
        // direct avatar/image URLs first, then inspect the nested author/channel object.
        android.widget.ImageView avatar = itemView.findViewById(R.id.shared_card_avatar);
        if (avatar != null) {
            String avatarUrl = null;
            String[] directMethods = {
                    "getAuthorAvatarUrl", "getAuthorAvatar", "getChannelAvatarUrl", "getChannelAvatar",
                    "getAuthorImageUrl", "getChannelImageUrl", "getAuthorIconUrl", "getChannelIconUrl"
            };
            for (String name : directMethods) {
                try {
                    java.lang.reflect.Method m = video.getClass().getMethod(name);
                    Object value = m.invoke(video);
                    if (value instanceof String && ((String) value).startsWith("http")) {
                        avatarUrl = (String) value;
                        break;
                    }
                    if (value != null) {
                        String[] nestedMethods = {"getAvatarUrl", "getAvatar", "getImageUrl", "getImage", "getThumbnailUrl", "getThumbnail", "getUrl"};
                        for (String nested : nestedMethods) {
                            try {
                                java.lang.reflect.Method nm = value.getClass().getMethod(nested);
                                Object nv = nm.invoke(value);
                                if (nv instanceof String && ((String) nv).startsWith("http")) {
                                    avatarUrl = (String) nv;
                                    break;
                                }
                            } catch (ReflectiveOperationException | RuntimeException ignored) {}
                        }
                    }
                    if (avatarUrl != null) break;
                } catch (ReflectiveOperationException | RuntimeException ignored) {}
            }
            if (avatarUrl == null) {
                String[] fields = {
                        "authorAvatarUrl", "authorAvatar", "channelAvatarUrl", "channelAvatar",
                        "authorImageUrl", "channelImageUrl", "authorIconUrl", "channelIconUrl"
                };
                for (String name : fields) {
                    try {
                        java.lang.reflect.Field f = video.getClass().getDeclaredField(name);
                        f.setAccessible(true);
                        Object value = f.get(video);
                        if (value instanceof String && ((String) value).startsWith("http")) {
                            avatarUrl = (String) value;
                            break;
                        }
                        if (value != null) {
                            for (String nested : new String[]{"getAvatarUrl", "getAvatar", "getImageUrl", "getThumbnailUrl", "getUrl"}) {
                                try {
                                    java.lang.reflect.Method nm = value.getClass().getMethod(nested);
                                    Object nv = nm.invoke(value);
                                    if (nv instanceof String && ((String) nv).startsWith("http")) {
                                        avatarUrl = (String) nv;
                                        break;
                                    }
                                } catch (ReflectiveOperationException | RuntimeException ignored) {}
                            }
                        }
                        if (avatarUrl != null) break;
                    } catch (ReflectiveOperationException | RuntimeException ignored) {}
                }
            }
            if (avatarUrl != null) {
                Glide.with(context).load(avatarUrl).circleCrop()
                        .placeholder(R.drawable.browse_ic_account)
                        .error(R.drawable.browse_ic_account).into(avatar);
            } else {
                avatar.setImageResource(R.drawable.browse_ic_account);
            }
        }

        if (video.videoId != null && video.videoId.matches("[A-Za-z0-9_-]{11}")) {
            // Decode for the physical display width, never an unbounded original bitmap.
            int imageWidth = Math.max(320, Math.min(1280, context.getResources().getDisplayMetrics().widthPixels));
            RequestOptions options = new RequestOptions()
                    .dontTransform()
                    .override(imageWidth, (imageWidth * 9 + 15) / 16)
                    .format(com.bumptech.glide.load.DecodeFormat.PREFER_ARGB_8888)
                    .downsample(com.bumptech.glide.load.resource.bitmap.DownsampleStrategy.AT_MOST)
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.DATA);
            String imageRoot = "https://i.ytimg.com/vi/" + video.videoId + "/";
            com.bumptech.glide.RequestBuilder<android.graphics.drawable.Drawable> request =
                    Glide.with(context).load(imageRoot + "maxresdefault.jpg").apply(options)
                            .error(Glide.with(context).load(imageRoot + "hq720.jpg").apply(options));
            if (video.bgImageUrl != null && video.bgImageUrl.startsWith("http")) {
                request = request.error(Glide.with(context).load(imageRoot + "hq720.jpg").apply(options)
                        .error(Glide.with(context).load(video.bgImageUrl).apply(options)
                                .error(Glide.with(context).load(video.getCardImageUrl()).apply(options)
                                        .error(R.drawable.shared_card_placeholder))));
            } else {
                request = request.error(Glide.with(context).load(imageRoot + "hq720.jpg").apply(options)
                        .error(Glide.with(context).load(video.getCardImageUrl()).apply(options)
                                .error(R.drawable.shared_card_placeholder)));
            }
            request.placeholder(R.drawable.shared_card_placeholder).into(mThumbnail);
            return;
        }
        Glide.with(context)
                .load(video.getCardImageUrl())''')
p.write_text(s)
print('NM7 Mobile v43 Super-style browse presentation applied')

