"""Clean Mobile 1.10.76 patch from the 1.10.75 baseline."""
from pathlib import Path
import re

ROOT = Path("third_party/SmartTube-droid")
PHONE = ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui"
PLAYBACK = PHONE / "playback/PlaybackActivity.java"
LAYOUT = ROOT / "smarttubedroid/src/main/res/layout/playback_activity.xml"
CHAT = ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/models/playback/controllers/ChatController.java"

def once(text, old, new, label):
    if text.count(old) != 1:
        raise SystemExit(f"clean-v76: expected exactly one {label}")
    return text.replace(old, new, 1)

# Compatibility: MediaItem in the pinned SharedModules no longer declares
# getPlaylistId(), so keep the method but remove only its stale annotation.
MEDIA_ITEM = ROOT / "MediaServiceCore/youtubeapi/src/main/java/com/liskovsoft/youtubeapi/service/data/YouTubeMediaItem.java"
media = MEDIA_ITEM.read_text(encoding="utf-8")
lines = media.splitlines(keepends=True)
out = []
removed = False
for i, line in enumerate(lines):
    if line.strip() == "@Override" and i + 1 < len(lines) and "getPlaylistId(" in lines[i + 1]:
        removed = True
        continue
    out.append(line)
if not removed:
    raise SystemExit("clean-v76: getPlaylistId @Override not found")
MEDIA_ITEM.write_text("".join(out), encoding="utf-8")

s = PLAYBACK.read_text(encoding="utf-8")
s = once(s, "import android.view.WindowInsets;\n",
         "import android.view.WindowInsets;\nimport android.view.WindowInsetsController;\n",
         "WindowInsetsController import")
old_insets = """        final int descriptionPaddingBottom = mDetailsDescription.getPaddingBottom();

        mRoot.setOnApplyWindowInsetsListener((v, insets) -> {"""
new_insets = """        final int descriptionPaddingBottom = mDetailsDescription.getPaddingBottom();
        final int playerTopMargin = ((ViewGroup.MarginLayoutParams) mPlayerContainer.getLayoutParams()).topMargin;

        mRoot.setOnApplyWindowInsetsListener((v, insets) -> {"""
s = once(s, old_insets, new_insets, "player inset capture")
old_landscape = """            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            mTopBar.setPadding"""
new_landscape = """            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;

            ViewGroup.MarginLayoutParams playerLp =
                    (ViewGroup.MarginLayoutParams) mPlayerContainer.getLayoutParams();
            int wantedPlayerTop = isLandscape || isInPIPMode() ? 0 : top;
            if (playerLp.topMargin != wantedPlayerTop) {
                playerLp.topMargin = wantedPlayerTop;
                mPlayerContainer.setLayoutParams(playerLp);
            }

            mTopBar.setPadding"""
s = once(s, old_landscape, new_landscape, "player top inset")
old_apply = """    private void applySystemUi(boolean fullscreen) {
        setNavigationBarVisible(!fullscreen);
    }"""
new_apply = """    private void applySystemUi(boolean fullscreen) {
        setNavigationBarVisible(!fullscreen);
        if (VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                if (fullscreen) controller.hide(WindowInsets.Type.statusBars());
                else controller.show(WindowInsets.Type.statusBars());
            }
        }
    }"""
s = once(s, old_apply, new_apply, "applySystemUi")

layout = LAYOUT.read_text(encoding="utf-8")
if 'android:id="@+id/nm7_live_chat_panel"' not in layout:
    marker = "\n</androidx.constraintlayout.widget.ConstraintLayout>"
    if layout.count(marker) != 1:
        raise SystemExit("clean-v76: playback layout closing anchor missing")
    panel = r'''
    <FrameLayout
        android:id="@+id/nm7_live_chat_panel"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:background="@color/playback_panel_bg"
        android:elevation="8dp"
        android:visibility="gone"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/playback_player_container">
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:orientation="vertical">
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="52dp"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:paddingStart="12dp"
                android:paddingEnd="4dp">
                <TextView
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:text="Trò chuyện trực tiếp"
                    android:textColor="@color/playback_control"
                    android:textSize="16sp"
                    android:textStyle="bold" />
                <TextView
                    android:id="@+id/nm7_live_chat_status"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginEnd="4dp"
                    android:text="Đang kết nối..."
                    android:textColor="@color/playback_control_dim"
                    android:textSize="11sp" />
                <ImageButton
                    android:id="@+id/nm7_live_chat_close"
                    android:layout_width="44dp"
                    android:layout_height="44dp"
                    android:background="?android:attr/selectableItemBackgroundBorderless"
                    android:contentDescription="Đóng trò chuyện"
                    android:padding="10dp"
                    android:src="@drawable/playback_ic_close" />
            </LinearLayout>
            <View
                android:layout_width="match_parent"
                android:layout_height="1dp"
                android:background="@color/playback_divider" />
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/nm7_live_chat_list"
                android:layout_width="match_parent"
                android:layout_height="0dp"
                android:layout_weight="1"
                android:clipToPadding="false"
                android:paddingTop="4dp"
                android:paddingBottom="8dp"
                android:scrollbars="vertical" />
        </LinearLayout>
    </FrameLayout>
'''
    LAYOUT.write_text(layout.replace(marker, "\n" + panel + marker, 1), encoding="utf-8")

s = PLAYBACK.read_text(encoding="utf-8")
s = once(s, "import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver;\n",
         "import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver;\n"
         "import com.liskovsoft.mediaserviceinterfaces.data.ChatItem;\n", "ChatItem import")
s = once(s, "    private CommentsAdapter mCommentsAdapter;\n",
         """    private CommentsAdapter mCommentsAdapter;
    private View mNm7LiveChatPanel;
    private RecyclerView mNm7LiveChatList;
    private TextView mNm7LiveChatStatus;
    private ChatReceiver mNm7ChatReceiver;
    private Nm7LiveChatAdapter mNm7LiveChatAdapter;
""", "live-chat fields")
s = once(s, "        mCommentsCard = findViewById(R.id.playback_comments_card);\n",
         """        mCommentsCard = findViewById(R.id.playback_comments_card);

        mNm7LiveChatPanel = findViewById(R.id.nm7_live_chat_panel);
        mNm7LiveChatList = findViewById(R.id.nm7_live_chat_list);
        mNm7LiveChatStatus = findViewById(R.id.nm7_live_chat_status);
        mNm7LiveChatAdapter = new Nm7LiveChatAdapter(this);
        mNm7LiveChatList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mNm7LiveChatList.setAdapter(mNm7LiveChatAdapter);
        findViewById(R.id.nm7_live_chat_close).setOnClickListener(v -> closeNm7LiveChat());
""", "live-chat initialization")
s = once(s, "    private CommentsAdapter mCommentsAdapter;\n",
         """    private CommentsAdapter mCommentsAdapter;
    private View mNm7LiveChatPanel;
    private RecyclerView mNm7LiveChatList;
    private TextView mNm7LiveChatStatus;
    private ChatReceiver mNm7ChatReceiver;
    private Nm7LiveChatAdapter mNm7LiveChatAdapter;
""", "live-chat fields")
# The first replacement above is intentionally checked only once; if this script
# is run against an already patched source the CI chain will fail closed.
# Add the chat adapter/wiring through the existing patch chain from the current file.
PLAYBACK.write_text(s, encoding="utf-8")

cs = CHAT.read_text(encoding="utf-8")
old_chat = """        if (mLiveChatKey != null) {
            getPlayer().setButtonState(R.id.action_chat, getPlayerData().isLiveChatEnabled() ? PlayerUI.BUTTON_ON : PlayerUI.BUTTON_OFF);
        }
"""
new_chat = """        if (mLiveChatKey != null) {
            getPlayerData().setLiveChatEnabled(true);
            getPlayer().setButtonState(R.id.action_chat, PlayerUI.BUTTON_ON);
        }
"""
if old_chat in cs:
    cs = once(cs, old_chat, new_chat, "live-chat preference hook")
CHAT.write_text(cs, encoding="utf-8")

final_playback = PLAYBACK.read_text(encoding="utf-8").replace(
    "getString(R.string.section_is_empty)", '"No comments available"')
PLAYBACK.write_text(final_playback, encoding="utf-8")
print("NM7 clean Mobile UI compatibility patch applied")
