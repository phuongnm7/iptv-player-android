"""Run deterministic device checks against this app's debug APK only.

Requires an already booted disposable Android emulator, adb and ffmpeg.
All media and playlists are synthetic and served on loopback; no provider
playlist, credentials or public streaming site is used.
"""
import functools
import http.server
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile
import threading
import time
import traceback
import xml.etree.ElementTree as ET

PACKAGE = "vn.phuong.iptvplayer"
BASE = "http://127.0.0.1:8765"
REPORT = Path("smoke-results")
CHECKS = []
REQUESTS = []


def command(*args, timeout=40, binary=False):
    result = subprocess.run(args, capture_output=True, timeout=timeout, check=True)
    return result.stdout if binary else result.stdout.decode("utf-8", errors="replace")


def adb(*args, **kwargs):
    return command("adb", *args, **kwargs)


def hierarchy():
    adb("shell", "uiautomator", "dump", "/sdcard/iptv-smoke.xml")
    return ET.fromstring(adb("exec-out", "cat", "/sdcard/iptv-smoke.xml"))


def find(root, resource=None, text=None):
    for node in root.iter("node"):
        if resource and node.get("resource-id") != PACKAGE + ":id/" + resource:
            continue
        if text is not None and node.get("text") != text:
            continue
        if resource or text is not None:
            return node
    return None


def wait_for(predicate, description, timeout=40):
    deadline = time.monotonic() + timeout
    last_error = None
    while time.monotonic() < deadline:
        try:
            root = hierarchy()
            value = predicate(root)
            if isinstance(value, ET.Element) or value:
                return value
        except (subprocess.SubprocessError, ET.ParseError) as error:
            last_error = error
        time.sleep(0.4)
    raise AssertionError(description + ("; " + str(last_error) if last_error else ""))


def bounds(node):
    return tuple(map(int, re.findall(r"\d+", node.get("bounds", ""))))


def tap_node(node, long_press=False):
    left, top, right, bottom = bounds(node)
    assert right > left and bottom > top, "Control is not visible"
    x, y = str((left + right) // 2), str((top + bottom) // 2)
    if long_press:
        adb("shell", "input", "swipe", x, y, x, y, "900")
    else:
        adb("shell", "input", "tap", x, y)


def tap(resource=None, text=None):
    tap_node(wait_for(lambda root: find(root, resource, text), "Missing control: " + str(resource or text)))


def enter(resource, value):
    node = wait_for(lambda root: find(root, resource), "Missing input " + resource)
    tap_node(node)
    adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
    count = len(node.get("text", ""))
    if count:
        adb("shell", "input", "keyevent", *(["KEYCODE_DEL"] * count))
    if value:
        adb("shell", "input", "text", value)
    adb("shell", "input", "keyevent", "KEYCODE_BACK")


def summary(*parts):
    def matches(root):
        node = find(root, "txtSummary")
        return node is not None and all(part in node.get("text", "") for part in parts)
    wait_for(matches, "Expected summary: " + repr(parts))


def check(name):
    CHECKS.append(name)
    print("PASS: " + name, flush=True)


def screenshot(name):
    (REPORT / (name + ".png")).write_bytes(adb("exec-out", "screencap", "-p", binary=True))


def launch():
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")
    wait_for(lambda root: find(root, "inputUrl"), "App did not open")


def play_direct(path, name):
    tap("btnSources")
    enter("inputUrl", BASE + path)
    tap("btnPlayUrl")
    def ready(root):
        # A fresh emulator shows Android's one-time immersive-mode tutorial.
        # It is a system overlay, not a player error; acknowledge its button.
        if any(n.get("resource-id") == "android:id/immersive_cling_title" for n in root.iter("node")):
            for node in root.iter("node"):
                if node.get("resource-id") == "android:id/ok":
                    tap_node(node)
                    return False
        error = find(root, "txtPlayerError")
        if error is not None:
            raise AssertionError(name + " playback error: " + error.get("text", ""))
        status = find(root, "txtPlayerStatus")
        if status is not None and "320 × 180" in status.get("text", ""):
            return True
        view = find(root, "playerView")
        if view is not None and status is None:
            tap_node(view)
        return False
    wait_for(ready, name + " did not decode the synthetic video", timeout=50)
    screenshot("player-" + name.lower())
    check(name + " synthetic video reports 320 x 180 without player error")
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    wait_for(lambda root: find(root, "inputUrl"), "Did not return from player")
    tap("btnSources")


def fixtures(directory):
    media = directory / "sample.mp4"
    command("ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "lavfi", "-i",
            "testsrc2=size=320x180:rate=24", "-f", "lavfi", "-i", "sine=frequency=440:sample_rate=48000",
            "-t", "20", "-c:v", "libx264", "-preset", "ultrafast", "-profile:v", "baseline",
            "-pix_fmt", "yuv420p", "-g", "48", "-c:a", "aac", "-b:a", "64k", "-movflags", "+faststart", str(media))
    command("ffmpeg", "-hide_banner", "-loglevel", "error", "-i", str(media), "-c", "copy",
            "-hls_time", "2", "-hls_playlist_type", "vod", str(directory / "sample.m3u8"))
    command("ffmpeg", "-hide_banner", "-loglevel", "error", "-i", str(media), "-c", "copy",
            "-f", "dash", str(directory / "sample.mpd"))
    (directory / "playlist.m3u").write_text(
        '#EXTM3U\n#EXTINF:-1 group-title="News",Alpha\nsample.m3u8\n'
        '#EXTINF:-1 group-title="Sports",Bravo\nsample.mp4\n'
        '#EXTINF:-1,Duplicate\nsample.m3u8\n#EXTINF:-1,Missing\n', encoding="utf-8")
    (directory / "invalid.html").write_text("<html>Not a playlist</html>", encoding="utf-8")


class Handler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, *args):
        pass

    def do_GET(self):
        REQUESTS.append(self.path)
        super().do_GET()


def run_checks():
    adb("shell", "settings", "put", "secure", "show_ime_with_hard_keyboard", "1")
    adb("shell", "settings", "put", "system", "accelerometer_rotation", "0")
    adb("shell", "settings", "put", "system", "user_rotation", "0")
    adb("logcat", "-c")
    launch()
    check("Application launches")
    enter("inputUrl", BASE + "/playlist.m3u")
    tap("btnLoadUrl")
    summary("2/2 kênh", "2 đã chọn", "1 trùng", "1 thiếu/sai")
    screenshot("playlist-portrait")
    check("URL import resolves relative channels, removes duplicate and counts missing URL")

    enter("inputSearch", "Bravo")
    summary("1/2 kênh", "2 đã chọn")
    tap("checkKeep")
    summary("1/2 kênh", "1 đã chọn")
    check("Search and per-channel selection")
    enter("inputSearch", "")
    tap("spinnerGroup")
    tap(text="News")
    summary("1/2 kênh", "1 đã chọn")
    check("Group filtering")

    tap_node(wait_for(lambda root: find(root, "txtName", "Alpha"), "Missing Alpha"), long_press=True)
    wait_for(lambda root: find(root, text=BASE + "/sample.m3u8"), "Full source URL not displayed")
    check("Full channel URL is visible in source dialog")
    adb("shell", "input", "keyevent", "KEYCODE_BACK")

    adb("shell", "settings", "put", "system", "user_rotation", "1")
    wait_for(lambda root: (lambda b: b[2] - b[0] > b[3] - b[1])(
        bounds(find(root, "mainRoot"))) if find(root, "mainRoot") is not None else False,
        "Main screen did not rotate")
    summary("1/2 kênh", "1 đã chọn")
    screenshot("playlist-landscape")
    check("Landscape layout retains group and selection")
    adb("shell", "settings", "put", "system", "user_rotation", "0")
    wait_for(lambda root: (lambda b: b[3] - b[1] > b[2] - b[0])(
        bounds(find(root, "mainRoot"))) if find(root, "mainRoot") is not None else False,
        "Main screen did not return to portrait")

    enter("inputUrl", BASE + "/invalid.html")
    tap("btnLoadUrl")
    wait_for(lambda root: "/invalid.html" in REQUESTS and
        find(root, "btnLoadUrl") is not None and find(root, "btnLoadUrl").get("enabled") == "true",
        "Invalid import did not complete")
    summary("1/2 kênh", "1 đã chọn")
    check("Invalid HTML import does not replace loaded playlist")

    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    time.sleep(1)
    adb("shell", "am", "force-stop", PACKAGE)
    launch()
    summary("2/2 kênh", "1 đã chọn", "1 trùng", "1 thiếu/sai")
    check("Playlist and selected channels survive process restart")

    tap("btnExport")
    def save_button(root):
        for node in root.iter("node"):
            if node.get("resource-id", "").endswith(":id/action_menu_save") or (
                    node.get("text", "").upper() == "SAVE" and node.get("clickable") == "true"):
                return node
        return None
    tap_node(wait_for(save_button, "Android file picker did not offer Save"))
    wait_for(lambda root: find(root, "inputUrl"), "Export did not return to app")
    exported = adb("shell", "cat", "/sdcard/Download/playlist-sach.m3u")
    assert exported.startswith("#EXTM3U") and exported.count("#EXTINF:") == 1, "Export must contain one selected channel"
    assert "Alpha" in exported and "Bravo" not in exported, "Export included an unchecked channel"
    check("Android file picker exports only selected channel to M3U")
    tap("btnOpenFile")
    tap(text="playlist-sach.m3u")
    summary("1/1 kênh", "1 đã chọn", "0 trùng", "0 thiếu/sai")
    check("Android file picker imports the exported M3U")

    play_direct("/sample.mp4", "HTTP-MP4")
    play_direct("/sample.m3u8", "HLS")
    play_direct("/sample.mpd", "DASH")

    enter("inputUrl", "srt://127.0.0.1:9999")
    tap("btnPlayUrl")
    wait_for(lambda root: (lambda n: n is not None and "chưa hỗ trợ" in n.get("text", ""))(
        find(root, "txtPlayerError")), "Unsupported protocol did not show a clear error")
    check("Unsupported protocol gives a visible error without crashing")

    logs = adb("logcat", "-d", "-s", "AndroidRuntime:E")
    (REPORT / "android-runtime.txt").write_text(logs, encoding="utf-8")
    assert "FATAL EXCEPTION" not in logs, "Android runtime crash found"
    check("No fatal Android runtime exception during smoke checks")


def main():
    REPORT.mkdir(exist_ok=True)
    result = {"passed": False, "checks": CHECKS, "limitations":
              "Synthetic 320x180 HTTP MP4/HLS/DASH only; no proof of device-specific 4K, RTSP, RTMP, UDP or provider streams."}
    try:
        with tempfile.TemporaryDirectory(prefix="iptv-smoke-") as temp:
            directory = Path(temp)
            fixtures(directory)
            server = http.server.ThreadingHTTPServer(("127.0.0.1", 8765), functools.partial(Handler, directory=str(directory)))
            threading.Thread(target=server.serve_forever, daemon=True).start()
            adb("install", "-r", sys.argv[1] if len(sys.argv) > 1 else "dist/IPTV-Player-1.2.apk", timeout=120)
            adb("reverse", "tcp:8765", "tcp:8765")
            try:
                run_checks()
                result["passed"] = True
            finally:
                server.shutdown()
                adb("reverse", "--remove", "tcp:8765")
    except Exception:
        result["error"] = traceback.format_exc()
        try:
            screenshot("failure")
            (REPORT / "failure.xml").write_text(ET.tostring(hierarchy(), encoding="unicode"), encoding="utf-8")
            (REPORT / "logcat.txt").write_text(adb("logcat", "-d", "-t", "800"), encoding="utf-8")
        except Exception:
            pass
        raise
    finally:
        (REPORT / "result.json").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
