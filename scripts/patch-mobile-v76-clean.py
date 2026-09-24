"""Clean Mobile 1.10.76 patch from the 1.10.75 baseline.

Only changes:
- keep the existing 16:9 portrait player, but apply the real Android status-bar inset
  to the player container so the video never sits underneath notification icons;
- show the status bar in portrait without touching the playback engine/lifecycle;
- expose SmartTube's existing live-chat receiver in a phone panel below the player;
- add a close button that hides the chat panel only; playback continues.
"""
from pathlib import Path

ROOT = Path("third_party/SmartTube-droid")
PHONE = ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui"
PLAYBACK = PHONE / "playback/PlaybackActivity.java"
LAYOUT = ROOT / "smarttubedroid/src/main/res/layout/playback_activity.xml"
CHAT = ROOT / "common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/models/playback/controllers/ChatController.java"


def once(text, old, new, label):
    if text.count(old) != 1:
        raise SystemExit(f"clean-v76: expected exactly one {label}")
    return text.replace(old, new, 1)


# 1) Handle Android 15 edge-to-edge correctly using the already-existing
#    WindowInsets listener. Do NOT change player initialization or lifecycle.
s = PLAYBACK.read_text(encoding="utf-8")

s = once(
    s,
    "import android.view.WindowInsets;\n",
    "import android.view.WindowInsets;\nimport android.view.WindowInsetsController;\n",
    "WindowInsetsController import",
)

old_insets = """        final int descriptionPaddingBottom = mDetailsDescription.getPaddingBottom();\n\n        mRoot.setOnApplyWindowInsetsListener((v, insets) -> {"""
new_insets = """        final int descriptionPaddingBottom = mDetailsDescription.getPaddingBottom();\n        final int playerTopMargin = ((ViewGroup.MarginLayoutParams) mPlayerContainer.getLayoutParams()).topMargin;\n\n        mRoot.setOnApplyWindowInsetsListener((v, insets) -> {"""
s = once(s, old_insets, new_insets, "player inset capture")

old_landscape = """            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;\n\n            mTopBar.setPadding"""
new_landscape = """            boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;\n\n            // Android 15 enforces edge-to-edge for targetSdk 35+. The player must consume\n            // the actual status-bar/cutout inset instead of drawing behind notification icons.\n            ViewGroup.MarginLayoutParams playerLp =\n                    (ViewGroup.MarginLayoutParams) mPlayerContainer.getLayoutParams();\n            int wantedPlayerTop = isLandscape || isInPIPMode() ? 0 : top;\n            if (playerLp.topMargin != wantedPlayerTop) {\n                playerLp.topMargin = wantedPlayerTop;\n                mPlayerContainer.setLayoutParams(playerLp);\n            }\n\n            mTopBar.setPadding"""
s = once(s, old_landscape, new_landscape, "player top inset")

old_apply = """    private void applySystemUi(boolean fullscreen) {\n        setNavigationBarVisible(!fullscreen);\n    }"""
new_apply = """    private void applySystemUi(boolean fullscreen) {\n        setNavigationBarVisible(!fullscreen);\n\n        // Keep the status bar visible in portrait. This does not change decor fitting,\n        // player initialization, renderer ownership, or Activity lifecycle. The actual\n        // portrait offset is applied from initWindowInsets() above.\n        if (VERSION.SDK_INT >= 30) {\n            WindowInsetsController controller = getWindow().getInsetsController();\n            if (controller != null) {\n                if (fullscreen) {\n                    controller.hide(WindowInsets.Type.statusBars());\n                } else {\n                    controller.show(WindowInsets.Type.statusBars());\n                }\n            }\n        }\n    }"""
s = once(s, old_apply, new_apply, "applySystemUi")

# 2) Add a dedicated live-chat panel below the player.
if 'android:id="@+id/nm7_live_chat_panel"' not in LAYOUT.read_text(encoding="utf-8"):
    layout = LAYOUT.read_text(encoding="utf-8")
    marker = "\n</androidx.constraintlayout.widget.ConstraintLayout>"
    if layout.count(marker) != 1:
        raise SystemExit("clean-v76: playback layout closing anchor missing")
    panel = r'''
    <!-- NM7 clean 1.10.76: live chat covers only the portrait metadata area. -->
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

# 3) Wire the existing ChatReceiver to the panel.
s = PLAYBACK.read_text(encoding="utf-8")
s = once(
    s,
    "import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver;\n",
    "import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver;\n"
    "import com.liskovsoft.mediaserviceinterfaces.data.ChatItem;\n",
    "ChatItem import",
)
s = once(
    s,
    "    private CommentsAdapter mCommentsAdapter;\n",
    """    private CommentsAdapter mCommentsAdapter;
    private View mNm7LiveChatPanel;
    private RecyclerView mNm7LiveChatList;
    private TextView mNm7LiveChatStatus;
    private ChatReceiver mNm7ChatReceiver;
    private Nm7LiveChatAdapter mNm7LiveChatAdapter;
""",
    "live-chat fields",
)
s = once(
    s,
    "        mCommentsCard = findViewById(R.id.playback_comments_card);\n",
    """        mCommentsCard = findViewById(R.id.playback_comments_card);

        mNm7LiveChatPanel = findViewById(R.id.nm7_live_chat_panel);
        mNm7LiveChatList = findViewById(R.id.nm7_live_chat_list);
        mNm7LiveChatStatus = findViewById(R.id.nm7_live_chat_status);
        mNm7LiveChatAdapter = new Nm7LiveChatAdapter(this);
        mNm7LiveChatList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mNm7LiveChatList.setAdapter(mNm7LiveChatAdapter);
        findViewById(R.id.nm7_live_chat_close).setOnClickListener(v -> closeNm7LiveChat());
""",
    "live-chat view initialization",
)
# Extend the existing inset padding to the chat list.
s = once(
    s,
    "        final int descriptionPaddingBottom = mDetailsDescription.getPaddingBottom();\n",
    """        final int descriptionPaddingBottom = mDetailsDescription.getPaddingBottom();
        final int chatPaddingBottom = mNm7LiveChatList.getPaddingBottom();
""",
    "chat inset capture",
)
s = once(
    s,
    """            mDetailsDescription.setPadding(mDetailsDescription.getPaddingLeft(), mDetailsDescription.getPaddingTop(),
                    mDetailsDescription.getPaddingRight(), descriptionPaddingBottom + (isLandscape ? 0 : bottom));

            return insets;
""",
    """            mDetailsDescription.setPadding(mDetailsDescription.getPaddingLeft(), mDetailsDescription.getPaddingTop(),
                    mDetailsDescription.getPaddingRight(), descriptionPaddingBottom + (isLandscape ? 0 : bottom));

            mNm7LiveChatList.setPadding(mNm7LiveChatList.getPaddingLeft(), mNm7LiveChatList.getPaddingTop(),
                    mNm7LiveChatList.getPaddingRight(), chatPaddingBottom + (isLandscape ? 0 : bottom));

            return insets;
""",
    "chat bottom inset",
)

stub = """    @Override
    public void setChatReceiver(ChatReceiver chatReceiver) {
        // v1: live chat isn't part of the phone player yet.
    }
"""
impl = """    @Override
    public void setChatReceiver(ChatReceiver chatReceiver) {
        runOnUiThread(() -> {
            if (mNm7ChatReceiver != null) {
                mNm7ChatReceiver.setCallback(null);
            }

            mNm7ChatReceiver = chatReceiver;
            mNm7LiveChatAdapter.clear();

            if (chatReceiver == null) {
                closeNm7LiveChat();
                return;
            }

            if (mPanel != null) {
                mPanel.setVisibility(View.GONE);
            }
            mNm7LiveChatPanel.setVisibility(View.VISIBLE);
            mNm7LiveChatStatus.setText("Đang xem trực tiếp");

            chatReceiver.setCallback(chatItem -> runOnUiThread(() -> {
                mNm7LiveChatAdapter.add(chatItem);
                int last = mNm7LiveChatAdapter.getItemCount() - 1;
                if (last >= 0) {
                    mNm7LiveChatList.scrollToPosition(last);
                }
            }));
        });
    }

    private void closeNm7LiveChat() {
        if (mNm7LiveChatPanel != null) {
            mNm7LiveChatPanel.setVisibility(View.GONE);
        }
        if (mPanel != null && !mIsLandscape && !isInPIPMode()) {
            mPanel.setVisibility(View.VISIBLE);
        }
    }

"""
s = once(s, stub, impl, "setChatReceiver stub")

marker = "    // -------------------------------------------------------- PlayerManager\n"
adapter = r'''    private static final class Nm7LiveChatAdapter
            extends RecyclerView.Adapter<Nm7LiveChatAdapter.Holder> {
        private static final int MAX_ITEMS = 100;
        private final android.content.Context mContext;
        private final java.util.List<ChatItem> mItems = new java.util.ArrayList<>();

        Nm7LiveChatAdapter(android.content.Context context) {
            mContext = context;
        }

        void clear() {
            mItems.clear();
            notifyDataSetChanged();
        }

        void add(ChatItem item) {
            if (item == null || TextUtils.isEmpty(item.getMessage())) {
                return;
            }
            mItems.add(item);
            if (mItems.size() > MAX_ITEMS) {
                mItems.remove(0);
                notifyDataSetChanged();
            } else {
                notifyItemInserted(mItems.size() - 1);
            }
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(mContext);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(12, 5, 12, 5);

            TextView avatar = new TextView(mContext);
            avatar.setGravity(Gravity.CENTER);
            avatar.setTextSize(13);
            avatar.setTextColor(0xFFFFFFFF);
            avatar.setBackgroundColor(0xFF607D8B);
            avatar.setLayoutParams(new LinearLayout.LayoutParams(34, 34));
            row.addView(avatar);

            LinearLayout textBox = new LinearLayout(mContext);
            textBox.setOrientation(LinearLayout.VERTICAL);
            textBox.setPadding(10, 0, 0, 0);
            textBox.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView author = new TextView(mContext);
            author.setTextColor(0xFF777777);
            author.setTextSize(11);
            author.setMaxLines(1);

            TextView message = new TextView(mContext);
            message.setTextColor(0xFF222222);
            message.setTextSize(14);
            message.setPadding(0, 1, 0, 0);

            textBox.addView(author);
            textBox.addView(message);
            row.addView(textBox);
            return new Holder(row, avatar, author, message);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            ChatItem item = mItems.get(position);
            String author = item.getAuthorName();
            holder.author.setText(author);
            holder.message.setText(item.getMessage());
            String initial = !TextUtils.isEmpty(author) ? author.substring(0, 1).toUpperCase(Locale.getDefault()) : "?";
            holder.avatar.setText(initial);
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        static final class Holder extends RecyclerView.ViewHolder {
            final TextView avatar;
            final TextView author;
            final TextView message;

            Holder(View itemView, TextView avatar, TextView author, TextView message) {
                super(itemView);
                this.avatar = avatar;
                this.author = author;
                this.message = message;
            }
        }
    }

'''
s = once(s, marker, adapter + marker, "live-chat adapter insertion")
PLAYBACK.write_text(s, encoding="utf-8")

# 4) Enable SmartTube's existing live-chat stream automatically only for a video
#    that actually exposes a liveChatKey. No player/lifecycle code is changed.
cs = CHAT.read_text(encoding="utf-8")
cs = once(
    cs,
    """        if (mLiveChatKey != null) {
            getPlayer().setButtonState(R.id.action_chat, getPlayerData().isLiveChatEnabled() ? PlayerUI.BUTTON_ON : PlayerUI.BUTTON_OFF);
        }
""",
    """        if (mLiveChatKey != null) {
            // NM7 Mobile: the phone UI has its own receive-only chat panel.
            // Do not alter the playback engine or Activity lifecycle.
            getPlayerData().setLiveChatEnabled(true);
            getPlayer().setButtonState(R.id.action_chat, PlayerUI.BUTTON_ON);
        }
""",
    "live-chat preference hook",
)
CHAT.write_text(cs, encoding="utf-8")

# 5) The current pinned SmartTube snapshot references an upstream string that is absent
#    from its resource table. Normalize this compatibility-only message after every
#    baseline patch has run, so later source transforms cannot reintroduce it.
final_playback = PLAYBACK.read_text(encoding="utf-8")
final_playback = final_playback.replace(
    "getString(R.string.section_is_empty)",
    '"No comments available"',
)
if "R.string.section_is_empty" in final_playback:
    raise SystemExit("clean-v76: unresolved section_is_empty resource reference")
PLAYBACK.write_text(final_playback, encoding="utf-8")

print("NM7 clean Mobile 1.10.76 UI patch applied on top of 1.10.75")
