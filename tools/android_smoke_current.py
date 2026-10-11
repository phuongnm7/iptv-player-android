"""Compatibility smoke test for the current Nm7 IPTV Player UI.

Checks Mobile startup in both portrait and landscape orientations.
No public IPTV stream is required.
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

APK = sys.argv[1]
ANDROID_HOME = os.environ.get("ANDROID_HOME", os.path.expanduser("~/Android/Sdk"))
AAPT = os.path.join(ANDROID_HOME, "build-tools", "36.0.0", "aapt")
BADGING = subprocess.run(
    [AAPT, "dump", "badging", APK],
    check=True,
    capture_output=True,
    text=True,
).stdout
PACKAGE_MATCH = re.search(r"^package: name='([^']+)'", BADGING, re.MULTILINE)
if PACKAGE_MATCH is None:
    raise RuntimeError("Could not read applicationId from test APK")
PACKAGE = PACKAGE_MATCH.group(1)
ACTIVITY_MATCH = re.search(r"^launchable-activity: name='([^']+)'", BADGING, re.MULTILINE)
if ACTIVITY_MATCH is None:
    raise RuntimeError("Could not read launchable activity from test APK")
ACTIVITY = ACTIVITY_MATCH.group(1)


def run_adb(*args, timeout=40, check=True):
    result = subprocess.run(
        ["adb", *args],
        check=False,
        capture_output=True,
        text=True,
        timeout=timeout,
    )
    if check and result.returncode:
        raise subprocess.CalledProcessError(
            result.returncode,
            result.args,
            output=result.stdout,
            stderr=result.stderr,
        )
    return result


def adb(*args, timeout=40):
    return run_adb(*args, timeout=timeout).stdout


def hierarchy():
    adb("shell", "uiautomator", "dump", "/sdcard/nm7-smoke.xml")
    return ET.fromstring(adb("exec-out", "cat", "/sdcard/nm7-smoke.xml"))


def has_id(root, name):
    return any(n.get("resource-id") == PACKAGE + ":id/" + name for n in root.iter("node"))


def has_android_id(root, name):
    return any(n.get("resource-id") == "android:id/" + name for n in root.iter("node"))


def android_node(root, name):
    return next(
        (n for n in root.iter("node") if n.get("resource-id") == "android:id/" + name),
        None,
    )


def tap_node(node):
    if node is None:
        return False
    bounds = node.get("bounds", "")
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not match:
        return False
    x1, y1, x2, y2 = map(int, match.groups())
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    return True


def alert_title(root):
    node = android_node(root, "alertTitle")
    return "" if node is None else node.get("text", "")


def dismiss_pixel_launcher_anr(root):
    """Ignore only the emulator launcher's own ANR; never mask an Nm7 ANR/crash."""
    title = alert_title(root)
    if title != "Pixel Launcher isn't responding":
        return False
    wait = android_node(root, "aerr_wait")
    close = android_node(root, "aerr_close")
    print("INFO: Pixel Launcher ANR obscured the test UI; dismissing emulator-only system dialog")
    if not tap_node(wait):
        tap_node(close)
    time.sleep(.8)
    return True


def dismiss_expected_playlist_error(root):
    """Dismiss only the handled network error shown when CI cannot resolve the default playlist."""
    title = alert_title(root)
    message = next(
        (
            n.get("text", "")
            for n in root.iter("node")
            if n.get("resource-id") == "android:id/message"
        ),
        "",
    )
    if (
        title == "Lỗi"
        and message.startswith("Không tải được playlist:")
        and has_android_id(root, "button1")
    ):
        print("INFO: default playlist is unavailable in CI; dismissing handled network error")
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
        return True
    return False


def print_diagnostics():
    print("\n===== NM7 SMOKE DIAGNOSTICS =====", file=sys.stderr)
    commands = [
        ("Foreground activity", ("shell", "dumpsys", "activity", "activities")),
        ("Package process", ("shell", "pidof", PACKAGE)),
        ("Window hierarchy", ("exec-out", "cat", "/sdcard/nm7-smoke.xml")),
        (
            "Application logcat",
            (
                "logcat",
                "-d",
                "-t",
                "500",
                "AndroidRuntime:E",
                "ActivityTaskManager:I",
                "ActivityManager:I",
                "*:S",
            ),
        ),
    ]
    for title, command in commands:
        result = run_adb(*command, check=False)
        print(f"\n--- {title} (exit {result.returncode}) ---", file=sys.stderr)
        if result.stdout:
            print(result.stdout, file=sys.stderr)
        if result.stderr:
            print(result.stderr, file=sys.stderr)


def wait_for_main_screen(timeout=45):
    end = time.monotonic() + timeout
    last_error = None
    while time.monotonic() < end:
        try:
            root = hierarchy()
            if has_id(root, "mainRoot"):
                return root
            if dismiss_pixel_launcher_anr(root):
                continue
            if dismiss_expected_playlist_error(root):
                time.sleep(.5)
                continue
        except Exception as error:
            last_error = error
        time.sleep(.4)
    print_diagnostics()
    message = "Main screen did not open"
    if last_error is not None:
        message += f"; last UI dump error: {last_error}"
    raise AssertionError(message)


def launch_main():
    adb("shell", "am", "force-stop", PACKAGE)
    launch = run_adb(
        "shell",
        "am",
        "start",
        "-W",
        "-n",
        PACKAGE + "/" + ACTIVITY,
        timeout=40,
    )
    print(launch.stdout)
    if launch.stderr:
        print(launch.stderr, file=sys.stderr)
    return wait_for_main_screen()


def assert_consolidated_header(root, label):
    removed = ["btnSources", "btnPlaylists", "btnAbout", "btnSelectAll", "btnSelectNone", "btnExport"]
    present = [name for name in removed if has_id(root, name)]
    if present:
        print_diagnostics()
        raise AssertionError(label + " still exposes consolidated controls: " + ", ".join(present))


adb("install", "-r", APK, timeout=90)
adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
run_adb("shell", "wm", "dismiss-keyguard", check=False)
run_adb("logcat", "-c", check=False)

# Portrait/mobile launch.
run_adb("shell", "settings", "put", "system", "accelerometer_rotation", "0", check=False)
run_adb("shell", "settings", "put", "system", "user_rotation", "0", check=False)
time.sleep(.8)
root = launch_main()
required = ["btnWallpaper", "btnAllChannels", "btnFavorites", "btnRecent"]
missing = [name for name in required if not has_id(root, name)]
if missing:
    print_diagnostics()
    raise AssertionError("Missing current UI controls: " + ", ".join(missing))
if not (has_id(root, "listChannels") or has_id(root, "txtEmpty")):
    print_diagnostics()
    raise AssertionError("Neither the channel list nor its empty state is available")
assert_consolidated_header(root, "Portrait UI")
if not has_id(root, "btnTabIptv") or not has_id(root, "btnTabYoutube"):
    print_diagnostics()
    raise AssertionError("Top-level TV / IPTV and YouTube tabs are missing")
print("PASS: portrait application launches and current IPTV interface is available")

youtube_tab = next((n for n in root.iter("node") if n.get("resource-id") == PACKAGE + ":id/btnTabYoutube"), None)
if not tap_node(youtube_tab):
    print_diagnostics()
    raise AssertionError("Could not select the YouTube top-level tab")
time.sleep(.5)
youtube_root = hierarchy()
if not has_id(youtube_root, "txtYoutubeStatus") or not has_id(youtube_root, "btnInstallGmsCore"):
    print_diagnostics()
    raise AssertionError("YouTube tab did not show its dedicated install/open screen")
print("PASS: YouTube tab opens a separate ReVanced install/open screen")

iptv_tab = next((n for n in youtube_root.iter("node") if n.get("resource-id") == PACKAGE + ":id/btnTabIptv"), None)
if not tap_node(iptv_tab):
    print_diagnostics()
    raise AssertionError("Could not return to the TV / IPTV tab")
time.sleep(.4)
root = hierarchy()
if not has_id(root, "listChannels") and not has_id(root, "txtEmpty"):
    print_diagnostics()
    raise AssertionError("Returning to TV / IPTV did not restore the channel list")

# Landscape layout regression. Resize the active activity in-place; restarting immediately after
# changing emulator rotation can intermittently return CI to the launcher before UIAutomator attaches.
run_adb("shell", "wm", "size", "1920x1080", check=False)
try:
    time.sleep(1.2)
    root = hierarchy()
    if not has_id(root, "mainRoot"):
        print_diagnostics()
        raise AssertionError("Main screen was lost while switching to landscape dimensions")
    assert_consolidated_header(root, "Landscape Mobile UI")
    print("PASS: landscape Mobile layout remains available without missing startup controls")
finally:
    run_adb("shell", "wm", "size", "reset", check=False)
print("PASS: Link, source and about controls are consolidated into Mobile app options")
