import { execFileSync } from "node:child_process";
import { existsSync, mkdirSync, rmSync } from "node:fs";
import { homedir } from "node:os";
import { join } from "node:path";

const root = process.cwd();
const tools = join(homedir(), "nm7-build-tools");
mkdirSync(tools, { recursive: true });

function run(cmd, args = [], env = {}) {
  console.log("\n$ " + [cmd, ...args].join(" "));
  execFileSync(cmd, args, {
    cwd: root,
    env: { ...process.env, ...env },
    stdio: "inherit",
    maxBuffer: 1024 * 1024 * 8
  });
}

function has(cmd) {
  try { execFileSync("sh", ["-lc", "command -v " + cmd], { stdio: "ignore" }); return true; }
  catch { return false; }
}

console.log("== NM7 Mobile Android Vercel build ==");
run("node", ["--version"]);
run("sh", ["-lc", "python3 --version || true"]);
run("sh", ["-lc", "git --version"]);

let javaHome = process.env.JAVA_HOME || "";
if (!has("java")) {
  const tar = join(tools, "jdk17.tar.gz");
  run("curl", ["-fsSL", "--retry", "4", "--connect-timeout", "10",
    "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse",
    "-o", tar]);
  javaHome = join(tools, "jdk17");
  rmSync(javaHome, { recursive: true, force: true });
  mkdirSync(javaHome, { recursive: true });
  run("tar", ["-xzf", tar, "-C", javaHome, "--strip-components=1"]);
}
if (javaHome) process.env.JAVA_HOME = javaHome;
if (process.env.JAVA_HOME) process.env.PATH = join(process.env.JAVA_HOME, "bin") + ":" + process.env.PATH;
run("java", ["-version"]);

let gradleBin = has("gradle") ? "gradle" : "";
if (!gradleBin) {
  const zip = join(tools, "gradle-8.13-bin.zip");
  const dir = join(tools, "gradle");
  run("curl", ["-fsSL", "--retry", "4", "--connect-timeout", "10",
    "https://services.gradle.org/distributions/gradle-8.13-bin.zip", "-o", zip]);
  rmSync(dir, { recursive: true, force: true });
  mkdirSync(dir, { recursive: true });
  run("unzip", ["-q", zip, "-d", dir]);
  gradleBin = join(dir, "gradle-8.13", "bin", "gradle");
}
process.env.PATH = join(tools, "gradle", "gradle-8.13", "bin") + ":" + process.env.PATH;
if (!has("gradle") && existsSync(gradleBin)) gradleBin = gradleBin;
run(gradleBin, ["--version"]);

const sdk = join(homedir(), "android-sdk");
const sdkManager = join(sdk, "cmdline-tools", "latest", "bin", "sdkmanager");
if (!existsSync(sdkManager)) {
  const zip = join(tools, "cmdline-tools.zip");
  const temp = join(tools, "cmdline-tools");
  run("curl", ["-fsSL", "--retry", "4", "--connect-timeout", "10",
    "https://dl.google.com/android/repository/commandlinetools-linux-12266719_latest.zip", "-o", zip]);
  rmSync(temp, { recursive: true, force: true });
  mkdirSync(temp, { recursive: true });
  run("unzip", ["-q", zip, "-d", temp]);
  mkdirSync(join(sdk, "cmdline-tools"), { recursive: true });
  rmSync(join(sdk, "cmdline-tools", "latest"), { recursive: true, force: true });
  run("mv", [join(temp, "cmdline-tools"), join(sdk, "cmdline-tools", "latest")]);
}
process.env.ANDROID_HOME = sdk;
process.env.ANDROID_SDK_ROOT = sdk;
process.env.PATH = join(sdk, "cmdline-tools", "latest", "bin") + ":" +
  join(sdk, "platform-tools") + ":" + process.env.PATH;
run("sh", ["-lc", "yes | sdkmanager --licenses >/dev/null 2>&1 || true"]);
run("sdkmanager", ["platform-tools", "platforms;android-34", "platforms;android-36", "build-tools;36.0.0"]);

rmSync(join(root, "third_party", "SmartTube-droid"), { recursive: true, force: true });
run("git", ["clone", "--no-checkout", "https://github.com/systematiq-one/SmartTube-droid.git", "third_party/SmartTube-droid"]);
run("git", ["-C", "third_party/SmartTube-droid", "checkout", "--detach", "4825d6aa8b6f1d3181927f9e96c7d89cab13d510"]);
run("git", ["-C", "third_party/SmartTube-droid", "submodule", "update", "--init", "--recursive"]);

run("python3", ["scripts/patch-mobile-v37.py"]);
run("python3", ["scripts/patch-mobile-v106.py"]);
run("python3", ["-c", String.raw`
from pathlib import Path
import re, shutil
root=Path('third_party/SmartTube-droid')
for p in root.rglob('build.gradle'):
    if '.git' in p.parts: continue
    t=p.read_text(errors='ignore'); orig=t
    manifest=p.parent/'src/main/AndroidManifest.xml'
    pkg=None
    if manifest.is_file():
        m=re.search(r'\\bpackage\\s*=\\s*["\\']([^"\\']+)["\\']',manifest.read_text(errors='ignore'))
        if m: pkg=m.group(1)
    if 'android {' in t:
        if pkg and not re.search(r'\\bnamespace\\s',t):
            t=re.sub(r'(android\\s*\\{)',r'\\1\\n    namespace "'+pkg+'"',t,1)
        if not re.search(r'compileSdkVersion|compileSdk\\s*[= ]',t):
            t=re.sub(r'(android\\s*\\{)',r'\\1\\n    compileSdkVersion 36',t,1)
        if 'flavorDimensions' not in t:
            t=re.sub(r'(android\\s*\\{)',r'\\1\\n    flavorDimensions "device"',t,1)
        if not re.search(r'\\bmobile\\s*\\{\\s*dimension\\s+["\\']device["\\']',t,re.S):
            t=re.sub(r'(android\\s*\\{)',r'\\1\\n    productFlavors { mobile { dimension "device" } }',t,1)
        t=re.sub(r'(?m)^(\\s*)classifier\\s+["\\']([^"\\']+)["\\']\\s*$',r"\\1archiveClassifier.set('\\2')",t)
        t=re.sub(r'(?m)^(\\s*)classifier\\s*=\\s*["\\']([^"\\']+)["\\']\\s*$',r"\\1archiveClassifier.set('\\2')",t)
    if t!=orig: p.write_text(t)
ui=root/'exoplayer-amzn-2.10.6/library/ui'
res=ui/'src/main/res/layout'
player=ui/'src/main/java/com/google/android/exoplayer2/ui/PlayerView.java'
if (res/'exo_player_view.xml').is_file(): shutil.copy2(res/'exo_player_view.xml',res/'st_exo_player_view.xml')
if (res/'exo_simple_player_view.xml').is_file(): shutil.copy2(res/'exo_simple_player_view.xml',res/'st_exo_simple_player_view.xml')
if (res/'st_exo_player_view.xml').is_file():
    p=res/'st_exo_player_view.xml'; p.write_text(p.read_text().replace('@layout/exo_simple_player_view','@layout/st_exo_simple_player_view'))
if player.is_file(): player.write_text(player.read_text().replace('R.layout.exo_player_view','R.layout.st_exo_player_view'))
`]);

const props = [
  "-PcompileSdkVersion=android-36",
  "-PminSdkVersion=23",
  "-PtargetSdkVersion=36",
  "-PbuildToolsVersion=36.0.0",
  "-PjunitXVersion=1.1.5",
  "-PtruthXVersion=1.5.0",
  "-PtestXSupportVersion=1.1.0",
  "-PannotationXVersion=1.1.0"
];

run(gradleBin, ["--no-daemon", "--console=plain", ...props,
  ":app:testMobileDebugUnitTest",
  "--tests", "vn.phuong.iptvplayer.M3uParserTest",
  "--tests", "vn.phuong.iptvplayer.MainActivityStartupTest"]);

run(gradleBin, ["--no-daemon", "--console=plain", ...props, ":app:assembleMobileDebug"]);

mkdirSync(join(root, "dist", "mobile"), { recursive: true });
run("sh", ["-lc", "cp app/build/outputs/apk/mobile/debug/*.apk dist/mobile/ && cd dist/mobile && sha256sum *.apk > SHA256SUMS.txt && ls -lh"]);
console.log("BUILD COMPLETE");
