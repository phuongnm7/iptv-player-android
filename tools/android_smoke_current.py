"""Compatibility smoke test for the current Nm7 IPTV Player UI.

The older smoke suite expected controls that were intentionally removed from the
main screen. This test checks the current supported UI without requiring any
public IPTV stream.
"""
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PACKAGE = "vn.phuong.iptvplayer"
APK = sys.argv[1]


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


def wait_for(predicate, message, timeout=30):
    end = time.monotonic() + timeout
    last_error = None
    while time.monotonic() < end:
        try:
            root = hierarchy()
            if predicate(root):
                return root
        except Exception as error:
            last_error = error
        time.sleep(.4)
    print_diagnostics()
    if last_error is not None:
        raise AssertionError(f"{message}; last UI dump error: {last_error}")
    raise AssertionError(message)


adb("install", "-r", APK, timeout=90)
adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
run_adb("shell", "wm", "dismiss-keyguard", check=False)
run_adb("logcat", "-c", check=False)
adb("shell", "am", "force-stop", PACKAGE)
launch = run_adb(
    "shell",
    "am",
    "start",
    "-W",
    "-n",
    PACKAGE + "/.MainActivity",
    timeout=40,
)
print(launch.stdout)
if launch.stderr:
    print(launch.stderr, file=sys.stderr)
root = wait_for(lambda r: has_id(r, "mainRoot"), "Main screen did not open")
required = ["btnSources", "btnPlaylists", "btnAllChannels", "btnFavorites", "btnRecent", "listChannels"]
missing = [name for name in required if not has_id(root, name)]
if missing:
    print_diagnostics()
    raise AssertionError("Missing current UI controls: " + ", ".join(missing))
removed = ["btnSelectAll", "btnSelectNone", "btnExport"]
present = [name for name in removed if has_id(root, name)]
if present:
    print_diagnostics()
    raise AssertionError("Removed M3U controls are still visible: " + ", ".join(present))
print("PASS: application launches and current IPTV interface is available")
print("PASS: removed M3U export controls are absent")
