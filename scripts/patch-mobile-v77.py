"""NM7 Mobile 1.10.77: real portrait status bar + closable live chat.

This patch is based on the verified 1.10.76 build. It corrects two issues
seen in device testing:
- The inherited SmartTube fullscreen mode was still hiding the Android status
  bar in portrait, so the player was drawn from y=0. Portrait now explicitly
  clears FLAG_FULLSCREEN and requests the normal status-bar inset.
- Live chat is presented as a closable panel. The header gets an X button;
  closing it restores the normal portrait details panel without stopping the
  current video.
"""
from pathlib import Path

ROOT = Path("third_party/SmartTube-droid")
PLAYBACK = ROOT / "smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/playback/PlaybackActivity.java"
LAYOUT = ROOT / "smarttubedroid/src/main/res/layout/playback_activity.xml"


def once(text, old, new, label):
    if text.count(old) != 1:
        raise SystemExit(f"v77: expected exactly one {label}, got {text.count(old)}")
    return text.replace(old, new, 1)


s = PLAYBACK.read_text(encoding="utf-8")

# Portrait must show the actual Android status bar. The previous v76 margin
# could not work because the parent activity still had FLAG_FULLSCREEN.
old = """    private void applySystemUi(boolean fullscreen) {
        setNavigationBarVisible(!fullscreen);
    }
"""
new = """    private void applySystemUi(boolean fullscreen) {
        if (!fullscreen) {
            // MotherActivity may have enabled global fullscreen before the phone
            // playback activity was created. Explicitly restore the status bar
            // for portrait so WindowInsets reports the real top inset.
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().setStatusBarColor(android.graphics.Color.BLACK);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            setNavigationBarVisible(true);
        } else {
            // Preserve the established 1.10.75/1.10.76 landscape fullscreen path.
            setNavigationBarVisible(false);
        }
    }
"""
s = once(s, old, new, "applySystemUi")

# Add close button to the live-chat header created by v76.
layout = LAYOUT.read_text(encoding="utf-8")
old_header = """                <TextView
                    android:id="@+id/nm7_live_chat_status"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginEnd="8dp"
                    android:text="Đang kết nối..."
                    android:textColor="#777777"
                    android:textSize="12sp" />
"""
new_header = """                <TextView
                    android:id="@+id/nm7_live_chat_status"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginEnd="8dp"
                    android:text="Đang kết nối..."
                    android:textColor="#777777"
                    android:textSize="12sp" />

                <TextView
                    android:id="@+id/nm7_live_chat_close"
                    android:layout_width="44dp"
                    android:layout_height="44dp"
                    android:gravity="center"
                    android:contentDescription="Đóng trò chuyện trực tiếp"
                    android:text="×"
                    android:textColor="#555555"
                    android:textSize="30sp"
                    android:clickable="true"
                    android:focusable="true" />
"""
s_layout = once(layout, old_header, new_header, "live-chat close button")

# Bind and handle close. Do not detach ChatReceiver: the current live stream
# stays alive, and a subsequent setChatReceiver() can simply reopen the panel.
old_fields = """    private TextView mNm7LiveChatStatus;
    private Nm7LiveChatAdapter mNm7LiveChatAdapter;
"""
new_fields = """    private TextView mNm7LiveChatStatus;
    private TextView mNm7LiveChatClose;
    private Nm7LiveChatAdapter mNm7LiveChatAdapter;
"""
s = once(s, old_fields, new_fields, "close button field")

old_init = """        mNm7LiveChatStatus = findViewById(R.id.nm7_live_chat_status);
        mNm7LiveChatAdapter = new Nm7LiveChatAdapter(this);
"""
new_init = """        mNm7LiveChatStatus = findViewById(R.id.nm7_live_chat_status);
        mNm7LiveChatClose = findViewById(R.id.nm7_live_chat_close);
        mNm7LiveChatClose.setOnClickListener(v -> {
            mNm7LiveChatPanel.setVisibility(View.GONE);
            if (mPanel != null) {
                mPanel.setVisibility(View.VISIBLE);
            }
        });
        mNm7LiveChatAdapter = new Nm7LiveChatAdapter(this);
"""
s = once(s, old_init, new_init, "close button listener")

PLAYBACK.write_text(s, encoding="utf-8")
LAYOUT.write_text(s_layout, encoding="utf-8")

print("NM7 1.10.77 status-bar and closable-chat patch applied")
