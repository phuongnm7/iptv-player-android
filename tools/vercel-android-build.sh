#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "== NM7 Android Vercel build =="
java -version || true
python3 --version
git --version

TOOLS="$HOME/nm7-build-tools"
mkdir -p "$TOOLS"

if ! command -v java >/dev/null 2>&1; then
  echo "Installing Temurin JDK 17..."
  curl -fsSL --retry 4 --connect-timeout 10 -o "$TOOLS/jdk.tar.gz"     https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse
  mkdir -p "$TOOLS/jdk"
  tar -xzf "$TOOLS/jdk.tar.gz" -C "$TOOLS/jdk" --strip-components=1
  export JAVA_HOME="$TOOLS/jdk"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if ! command -v gradle >/dev/null 2>&1; then
  echo "Installing Gradle 8.13..."
  curl -fsSL --retry 4 --connect-timeout 10 -o "$TOOLS/gradle.zip"     https://services.gradle.org/distributions/gradle-8.13-bin.zip
  python3 - <<'PY'
from zipfile import ZipFile
from pathlib import Path
p=Path.home()/'nm7-build-tools'/'gradle.zip'
out=Path.home()/'nm7-build-tools'/'gradle'
with ZipFile(p) as z:
    z.extractall(out)
PY
  export PATH="$(find "$TOOLS/gradle" -maxdepth 1 -type d -name 'gradle-*' -print -quit)/bin:$PATH"
fi

SDK="$HOME/android-sdk"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "Installing Android SDK command line tools..."
  mkdir -p "$SDK/cmdline-tools"
  curl -fsSL --retry 4 --connect-timeout 10 -o "$TOOLS/cmdline-tools.zip"     https://dl.google.com/android/repository/commandlinetools-linux-12266719_latest.zip
  rm -rf "$SDK/cmdline-tools/latest" "$TOOLS/cmdline-tools"
  mkdir -p "$TOOLS/cmdline-tools"
  python3 - <<'PY'
from zipfile import ZipFile
from pathlib import Path
src=Path.home()/'nm7-build-tools'/'cmdline-tools.zip'
dst=Path.home()/'nm7-build-tools'/'cmdline-tools'
with ZipFile(src) as z:
    z.extractall(dst)
PY
  mv "$TOOLS/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
fi

export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platform-tools" "platforms;android-34" "platforms;android-36" "build-tools;36.0.0"

rm -rf third_party/SmartTube-droid
git clone --no-checkout https://github.com/systematiq-one/SmartTube-droid.git third_party/SmartTube-droid
git -C third_party/SmartTube-droid checkout --detach 4825d6aa8b6f1d3181927f9e96c7d89cab13d510
git -C third_party/SmartTube-droid submodule update --init --recursive

python3 scripts/patch-mobile-v37.py
python3 scripts/patch-mobile-v106.py

# Apply the same compatibility edits used by the repository CI, but without
# relying on GitHub Actions-specific environment state.
python3 - <<'PY'
from pathlib import Path
import re, shutil
root=Path('third_party/SmartTube-droid')
for p in root.rglob('build.gradle'):
    if '.git' in p.parts: continue
    t=p.read_text(errors='ignore'); orig=t
    manifest=p.parent/'src/main/AndroidManifest.xml'
    pkg=None
    if manifest.is_file():
        m=re.search(r'\bpackage\s*=\s*["\']([^"\']+)["\']',manifest.read_text(errors='ignore'))
        if m: pkg=m.group(1)
    if 'android {' in t:
        if pkg and not re.search(r'\bnamespace\s',t):
            t=re.sub(r'(android\s*\{)',r'\1\n    namespace "'+pkg+'"',t,1)
        if not re.search(r'compileSdkVersion|compileSdk\s*[= ]',t):
            t=re.sub(r'(android\s*\{)',r'\1\n    compileSdkVersion 36',t,1)
        if 'flavorDimensions' not in t:
            t=re.sub(r'(android\s*\{)',r'\1\n    flavorDimensions "device"',t,1)
        if not re.search(r'\bmobile\s*\{\s*dimension\s+["\']device["\']',t,re.S):
            t=re.sub(r'(android\s*\{)',r'\1\n    productFlavors { mobile { dimension "device" } }',t,1)
        t=re.sub(r'(?m)^(\s*)classifier\s+["\']([^"\']+)["\']\s*$',r"\1archiveClassifier.set('\2')",t)
        t=re.sub(r'(?m)^(\s*)classifier\s*=\s*["\']([^"\']+)["\']\s*$',r"\1archiveClassifier.set('\2')",t)
    if t!=orig: p.write_text(t)

ui=root/'exoplayer-amzn-2.10.6/library/ui'
res=ui/'src/main/res/layout'
player=ui/'src/main/java/com/google/android/exoplayer2/ui/PlayerView.java'
if (res/'exo_player_view.xml').is_file():
    shutil.copy2(res/'exo_player_view.xml',res/'st_exo_player_view.xml')
if (res/'exo_simple_player_view.xml').is_file():
    shutil.copy2(res/'exo_simple_player_view.xml',res/'st_exo_simple_player_view.xml')
if (res/'st_exo_player_view.xml').is_file():
    p=res/'st_exo_player_view.xml'
    p.write_text(p.read_text().replace('@layout/exo_simple_player_view','@layout/st_exo_simple_player_view'))
if player.is_file():
    player.write_text(player.read_text().replace('R.layout.exo_player_view','R.layout.st_exo_player_view'))

# Match the build pins used by CI.
sdk_local = Path.home()/'android-sdk'
print("SDK:", sdk_local)
PY

gradle --no-daemon --console=plain   -PcompileSdkVersion=android-36   -PminSdkVersion=23   -PtargetSdkVersion=36   -PbuildToolsVersion=36.0.0   -PjunitXVersion=1.1.5   -PtruthXVersion=1.5.0   -PtestXSupportVersion=1.1.0   -PannotationXVersion=1.1.0   :app:testMobileDebugUnitTest   --tests vn.phuong.iptvplayer.M3uParserTest   --tests vn.phuong.iptvplayer.MainActivityStartupTest

gradle --no-daemon --console=plain   -PcompileSdkVersion=android-36   -PminSdkVersion=23   -PtargetSdkVersion=36   -PbuildToolsVersion=36.0.0   -PjunitXVersion=1.1.5   -PtruthXVersion=1.5.0   -PtestXSupportVersion=1.1.0   -PannotationXVersion=1.1.0   :app:assembleMobileDebug

mkdir -p dist/mobile
cp app/build/outputs/apk/mobile/debug/*.apk dist/mobile/
cd dist/mobile
sha256sum *.apk > SHA256SUMS.txt
ls -lh
