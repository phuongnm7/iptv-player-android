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


def adb(*args, timeout=40):
    return subprocess.run(["adb", *args], check=True, capture_output=True, timeout=timeout).stdout


def hierarchy():
    adb("shell", "uiautomator", "dump", "/sdcard/nm7-smoke.xml")
    return ET.fromstring(adb("exec-out", "cat", "/sdcard/nm7-smoke.xml").decode("utf-8", "replace"))


def has_id(root, name):
    return any(n.get("resource-id") == PACKAGE + ":id/" + name for n in root.iter("node"))


def wait_for(predicate, message, timeout=30):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        try:
            root = hierarchy()
            if predicate(root):
                return root
        except Exception:
            pass
        time.sleep(.4)
    raise AssertionError(message)


adb("install", "-r", APK, timeout=90)
adb("shell", "am", "force-stop", PACKAGE)
adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity", timeout=40)
root = wait_for(lambda r: has_id(r, "mainRoot"), "Main screen did not open")
required = ["btnSources", "btnPlaylists", "btnAllChannels", "btnFavorites", "btnRecent", "listChannels"]
missing = [name for name in required if not has_id(root, name)]
if missing:
    raise AssertionError("Missing current UI controls: " + ", ".join(missing))
removed = ["btnSelectAll", "btnSelectNone", "btnExport"]
present = [name for name in removed if has_id(root, name)]
if present:
    raise AssertionError("Removed M3U controls are still visible: " + ", ".join(present))
print("PASS: application launches and current IPTV interface is available")
print("PASS: removed M3U export controls are absent")
