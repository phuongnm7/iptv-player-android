"""NM7 Mobile 1.10.101 performance patch — built directly on the user-confirmed 1.10.97 source chain.

Scope is limited to YouTube feed/tab responsiveness and repeated data work.
It does not modify the 1.10.97 avatar pipeline, portrait WindowInsets/status-bar path,
PlayerView/shutter, spinner suppression, or tested playback transport/buffer settings.
"""
from pathlib import Path
ROOT=Path("third_party/SmartTube-droid")

p=ROOT/"common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/presenters/BrowsePresenter.java"
s=p.read_text(encoding="utf-8")
anchor="    private int mBootstrapSectionId = -1;\n"
fields="""    private int mBootstrapSectionId = -1;
    private static final long NM7_SECTION_CACHE_TTL_MS = 45_000L;
    private final java.util.Map<Integer, Nm7CachedGrid> mNm7GridCache = new java.util.HashMap<>();
    private final java.util.Map<Integer, java.util.List<MediaGroup>> mNm7RowsCache = new java.util.HashMap<>();
    private final java.util.Map<Integer, Long> mNm7RowsCacheTime = new java.util.HashMap<>();
    private static final class Nm7CachedGrid {
        final MediaGroup group;
        final long timestampMs;
        Nm7CachedGrid(MediaGroup group, long timestampMs) { this.group=group; this.timestampMs=timestampMs; }
    }
"""
if s.count(anchor)!=1: raise SystemExit("performance: BrowsePresenter field anchor missing/ambiguous")
s=s.replace(anchor,fields,1)
anchor="""    public void refresh(boolean focusOnContent) {
        updateCurrentSection();
        if (focusOnContent && getView() != null) {
            getView().focusOnContent();
        }
    }
"""
replacement="""    public void refresh(boolean focusOnContent) {
        mNm7GridCache.clear();
        mNm7RowsCache.clear();
        mNm7RowsCacheTime.clear();
        updateCurrentSection();
        if (focusOnContent && getView() != null) {
            getView().focusOnContent();
        }
    }
"""
if s.count(anchor)!=1: raise SystemExit("performance: refresh anchor missing")
s=s.replace(anchor,replacement,1)
anchor="""                        mediaGroups -> {
                            getView().showProgressBar(false);

                            filterHomeIfNeeded(mediaGroups);
"""
replacement="""                        mediaGroups -> {
                            getView().showProgressBar(false);

                            filterHomeIfNeeded(mediaGroups);
                            mNm7RowsCache.put(section.getId(), new java.util.ArrayList<>(mediaGroups));
                            mNm7RowsCacheTime.put(section.getId(), System.currentTimeMillis());
"""
if s.count(anchor)!=1: raise SystemExit("performance: rows callback anchor missing")
s=s.replace(anchor,replacement,1)
anchor="""        Disposable updateAction = groups
                .subscribe(
"""
replacement="""        java.util.List<MediaGroup> cachedRows = mNm7RowsCache.get(section.getId());
        Long cachedRowsAt = mNm7RowsCacheTime.get(section.getId());
        if (cachedRows != null && cachedRowsAt != null
                && System.currentTimeMillis() - cachedRowsAt < NM7_SECTION_CACHE_TTL_MS) {
            getView().showProgressBar(false);
            for (MediaGroup mediaGroup : cachedRows) {
                if (mediaGroup == null || mediaGroup.isEmpty()) continue;
                VideoGroup videoGroup = VideoGroup.from(mediaGroup, section);
                if (android.text.TextUtils.isEmpty(videoGroup.getTitle())) {
                    videoGroup.setTitle(getContext().getString(R.string.suggestions));
                }
                getView().updateSection(videoGroup);
                mBrowseProcessor.process(videoGroup);
            }
            return;
        }

        Disposable updateAction = groups
                .subscribe(
"""
if s.index(anchor)<0: raise SystemExit("performance: rows subscription anchor missing")
s=s.replace(anchor,replacement,1)
anchor="""                        mediaGroup -> {
                            getView().showProgressBar(false);

                            if (getView() == null) {
"""
if s.count(anchor)!=1: raise SystemExit("performance: grid callback anchor missing")
anchor2="""                                return;
                            }

                            VideoGroup videoGroup = VideoGroup.from(baseGroup, mediaGroup);
"""
replacement2="""                                return;
                            }

                            mNm7GridCache.put(section.getId(),
                                    new Nm7CachedGrid(mediaGroup, System.currentTimeMillis()));

                            VideoGroup videoGroup = VideoGroup.from(baseGroup, mediaGroup);
"""
if s.count(anchor2)!=1: raise SystemExit("performance: grid cache anchor missing")
s=s.replace(anchor2,replacement2,1)
idx=s.index("        Disposable updateAction = group")
if idx<0: raise SystemExit("performance: grid subscription anchor missing")
insert="""        Nm7CachedGrid cachedGrid = mNm7GridCache.get(section.getId());
        if (cachedGrid != null && cachedGrid.group != null
                && System.currentTimeMillis() - cachedGrid.timestampMs < NM7_SECTION_CACHE_TTL_MS) {
            getView().showProgressBar(false);
            VideoGroup videoGroup = VideoGroup.from(baseGroup, cachedGrid.group);
            appendLocalHistory(videoGroup);
            getView().updateSection(videoGroup);
            mBrowseProcessor.process(videoGroup);
            return;
        }

"""
s=s.slice(0,idx)+insert+s.slice(idx)
p.write_text(s,encoding="utf-8")

p=ROOT/"smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/browse/BrowseActivity.java"
s=p.read_text(encoding="utf-8")
anchor="        mGridView.setAdapter(mGridAdapter);"
repl="""        mGridView.setAdapter(mGridAdapter);
        mGridView.setHasFixedSize(true);
        mGridView.setItemViewCacheSize(6);
        if (mGridView.getLayoutManager() instanceof androidx.recyclerview.widget.GridLayoutManager) {
            ((androidx.recyclerview.widget.GridLayoutManager) mGridView.getLayoutManager()).setInitialPrefetchItemCount(6);
        }
"""
if s.count(anchor)!=1: raise SystemExit("performance: YouTube grid adapter anchor missing/ambiguous")
s=s.replace(anchor,repl,1)
p.write_text(s,encoding="utf-8")

print("NM7 1.10.101 performance patch prepared")