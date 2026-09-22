"""Final Mobile 1.10.69 delta: keep the complete 1.10.68 YouTube UI, remove only mini-player navigation."""
from pathlib import Path
import re

phone = Path("third_party/SmartTube-droid/smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui")

def method(text, signature, body):
    pattern = re.compile(r"(?ms)^    " + re.escape(signature) + r" \\{.*?^    \\}")
    text, count = pattern.subn(lambda _: "    " + signature + " {\n" + body + "\n    }", text)
    if count != 1:
        raise SystemExit("Missing/ambiguous method: " + signature)
    return text

# Playback: Back returns to the normal YouTube Browse screen and closes the
# fullscreen player. It never enters the old mini-player state.
p = phone / "playback/PlaybackActivity.java"
t = p.read_text(encoding="utf-8")
t = method(t, "public void onBackPressed()", """        if (onDetailsBack()) return;
        if (mPlayer == null || mNm7Stopped) { super.onBackPressed(); return; }
        mNm7LeavingForMini = false;
        cancelNm7TargetRestore(true);
        mIsBackPressed = true;
        sNm7RestorePending = false;
        sNm7Mini = false;
        nm7SetBackground(false);
        Intent intent = new Intent(this,
                com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        startActivity(intent);
        finish();""")

# No Browse screen may create or attach the old mini surface.
p = phone / "browse/BrowseActivity.java"
t = p.read_text(encoding="utf-8")
count = t.count("installNm7MiniPlayer();")
if count:
    t = t.replace("        installNm7MiniPlayer();\n", "")
p.write_text(t, encoding="utf-8")

# Search Back remains native; it no longer needs to consume a mini-player session.
p = phone / "search/SearchActivity.java"
t = p.read_text(encoding="utf-8")
t = re.sub(r"""    @Override
    public void onBackPressed() {
        try {
            if (com.liskovsoft.smartyoutubetv2.droid.ui.playback.PlaybackActivity.consumeNm7BrowseBack()) {
                return;
            }
        } catch (RuntimeException error) {
            android.util.Log.e("NM7Playback", "Search Back mini close failed", error);
        }
        super.onBackPressed();
    }

""", "", t, count=1)
p.write_text(t, encoding="utf-8")

print("NM7 Mobile 1.10.69: removed only mini-player navigation/attachment; 1.10.68 YouTube UI patches remain active.")
