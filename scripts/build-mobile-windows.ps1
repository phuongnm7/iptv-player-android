# NM7 IPTV Mobile 1.10.26 - Windows local build
# SCRIPT_VERSION: 2026-09-18-PATCH11
$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root
$ST = Join-Path $Root "third_party\SmartTube-droid"
$Dist = Join-Path $Root "dist\mobile"
$BT = "36.0.0"
function Fail([string]$m) { Write-Host ""; Write-Host "BUILD STOPPED: $m" -ForegroundColor Red; exit 1 }
function Run([string]$exe,[string[]]$cmdArgs) { Write-Host (">> " + $exe + " " + ($cmdArgs -join " ")) -ForegroundColor DarkCyan; & $exe @cmdArgs; if ($LASTEXITCODE -ne 0) { Fail ("Command failed: " + $exe + " (exit $LASTEXITCODE)") } }
function ReadT([string]$p) { [IO.File]::ReadAllText($p) }
function WriteT([string]$p,[string]$s) { [IO.File]::WriteAllText($p,$s,(New-Object Text.UTF8Encoding($false))) }

Write-Host "=== NM7 IPTV Mobile 1.10.26 / Windows ===" -ForegroundColor Cyan
if (-not (Get-Command git -ErrorAction SilentlyContinue)) { Fail "Git is not installed or not in PATH." }
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { Fail "Java is not installed or not in PATH." }

$Gradle = $null
$g = Get-Command gradle.bat -ErrorAction SilentlyContinue
if ($g) { $Gradle = $g.Source }
if (-not $Gradle) {
  foreach ($p in @("C:\Gradle\gradle-8.13\bin\gradle.bat","$env:USERPROFILE\gradle\gradle-8.13\bin\gradle.bat")) {
    if (Test-Path $p) { $Gradle=$p; break }
  }
}
if (-not $Gradle) { Fail "Gradle 8.13 not found." }

if (-not $env:ANDROID_HOME -or -not (Test-Path $env:ANDROID_HOME)) {
  if ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) { $env:ANDROID_HOME=$env:ANDROID_SDK_ROOT }
  elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk") { $env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk" }
  else { Fail "Android SDK not found. Set ANDROID_HOME." }
}
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME

if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
  $j = @(Get-Item "C:\Program Files\Eclipse Adoptium\jdk-17*" -ErrorAction SilentlyContinue | Where-Object { Test-Path (Join-Path $_.FullName "bin\java.exe") }) | Select-Object -First 1
  if ($j) { $env:JAVA_HOME=$j.FullName }
}
if (-not $env:JAVA_HOME) { Fail "JDK 17 not found. Set JAVA_HOME." }
$env:PATH=(Join-Path $env:JAVA_HOME "bin") + ";" + $env:PATH
$javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
if (-not (Test-Path $javaExe)) { Fail "JAVA_HOME does not point to a valid JDK 17 installation." }
$javaVersionFile = Join-Path $env:TEMP "nm7-java-version.txt"
Remove-Item $javaVersionFile -Force -ErrorAction SilentlyContinue
$javaProc = Start-Process -FilePath $javaExe -ArgumentList "-version" -Wait -PassThru -NoNewWindow -RedirectStandardError $javaVersionFile
$ver = if (Test-Path $javaVersionFile) { (Get-Content $javaVersionFile | Select-Object -First 1) } else { "" }
Remove-Item $javaVersionFile -Force -ErrorAction SilentlyContinue
if ($javaProc.ExitCode -ne 0 -or $ver -notmatch '"17\.') { Fail ("JDK 17 required. Current: " + $ver) }

if ((& git branch --show-current).Trim() -ne "fix/mobile-1.10.26-sleep-timer-icon") { Run git @("checkout","fix/mobile-1.10.26-sleep-timer-icon") }

$sdk = Join-Path $env:ANDROID_HOME "cmdline-tools\latest\bin\sdkmanager.bat"
if (-not (Test-Path $sdk)) { $sdk=Join-Path $env:ANDROID_HOME "cmdline-tools\bin\sdkmanager.bat" }
if (Test-Path $sdk) { Run $sdk @("platforms;android-34","platforms;android-36","build-tools;36.0.0") }

if (Test-Path $ST) { Remove-Item -Recurse -Force $ST }
New-Item -ItemType Directory -Force (Split-Path $ST) | Out-Null
Run git @("clone","--depth","1","--recurse-submodules","https://github.com/systematiq-one/SmartTube-droid.git",$ST)
Run git @("-C",$ST,"submodule","update","--init","--force","--recursive")
Run git @("-C",$ST,"remote","add","upstream","https://github.com/yuliskov/SmartTube.git")
Run git @("-C",$ST,"config","user.name","NM7 Windows Build")
Run git @("-C",$ST,"config","user.email","nm7-build@users.noreply.github.com")
Run git @("-C",$ST,"fetch","--unshallow","origin")
Run git @("-C",$ST,"fetch","upstream","refs/tags/32.47s")
Run git @("-C",$ST,"merge","--no-edit","FETCH_HEAD")
Run git @("-C",$ST,"submodule","update","--init","--force","--recursive")

$nl=[Environment]::NewLine
Get-ChildItem $ST -Recurse -Filter "build.gradle" -File | Where-Object {$_.FullName -notmatch "\\.git\\"} | ForEach-Object {
  $p=$_.FullName; $t=ReadT $p; $orig=$t; $mf=Join-Path $_.Directory.FullName "src\main\AndroidManifest.xml"; $pkg=$null
  if (Test-Path $mf) { $m=[regex]::Match((ReadT $mf),'\bpackage\s*=\s*["'']([^"'']+)["'']'); if($m.Success){$pkg=$m.Groups[1].Value} }
  if($t -match 'android\s*\{'){
    if($pkg -and $t -notmatch '\bnamespace\s'){$t=[regex]::Replace($t,'(android\s*\{)',('$1'+$nl+'    namespace "'+$pkg+'"'),1)}
    if($t -notmatch 'compileSdkVersion|compileSdk\s*[= ]'){$t=[regex]::Replace($t,'(android\s*\{)',('$1'+$nl+'    compileSdkVersion project.properties.compileSdkVersion'),1)}
    if($t -notmatch 'flavorDimensions'){$t=[regex]::Replace($t,'(android\s*\{)',('$1'+$nl+'    flavorDimensions "device"'),1)}
    if($t -notmatch 'mobile\s*\{\s*dimension\s+["'']device["'']'){$t=[regex]::Replace($t,'(android\s*\{)',('$1'+$nl+'    productFlavors { mobile { dimension "device" } }'),1)}
    foreach($f in @("stbeta","ststable","stfdroid")){$t=[regex]::Replace($t,"(?m)^(\s*)$f\s*\{\s*\}",'$1'+$f+' { dimension "default" }')}
    $t=[regex]::Replace($t,"(?m)^(\s*)classifier\s+['""]([^'""]+)['""]\s*$",'$1archiveClassifier.set(''$2'')')
    $t=[regex]::Replace($t,"(?m)^(\s*)classifier\s*=\s*['""]([^'""]+)['""]\s*$",'$1archiveClassifier.set(''$2'')')
    if($t -match 'kotlin-android' -and $t -notmatch 'kotlinOptions'){$t=[regex]::Replace($t,'(android\s*\{)',('$1'+$nl+'    kotlinOptions { jvmTarget = "1.8" }'),1)}
  }
  if($t -ne $orig){WriteT $p $t}
}

$browse=Join-Path $ST "smarttubedroid\src\main\java\com\liskovsoft\smartyoutubetv2\droid\ui\browse\BrowseActivity.java"
if(-not(Test-Path $browse)){Fail "BrowseActivity.java not found."}
$t=ReadT $browse
if($t -notmatch "extends DroidActivity"){Fail "SmartTube phone BrowseActivity is not active; TV UI source detected."}
$n=$t.Replace("private static final int GRID_COLUMNS = 2;","private static final int GRID_COLUMNS = 1;")
if($n -eq $t){Fail "GRID_COLUMNS declaration not found."}; WriteT $browse $n

$uploads=Join-Path $ST "smarttubedroid\\src\\main\\java\\com\\liskovsoft\\smartyoutubetv2\\droid\\ui\\channeluploads\\ChannelUploadsActivity.java"
if(Test-Path $uploads){
    $x=ReadT $uploads
    if($x -match '\bGRID_COLUMNS\b' -and $x -notmatch '(?m)\bGRID_COLUMNS\s*='){
        $idx=$x.IndexOf('{')
        if($idx -ge 0){
            $n=$x.Insert($idx+1,$nl+'    private static final int GRID_COLUMNS = 1;'+$nl)
            WriteT $uploads $n
        }
    }
}

# Do not globally rename the SmartTube token in XML/properties resources.
# Android resource names must not contain spaces; branding changes are handled by
# targeted string resources only, never by rewriting style/layout resource names.
$play=Join-Path $ST "smarttubedroid\src\main\java\com\liskovsoft\smartyoutubetv2\droid\ui\playback\PlaybackActivity.java"
if(-not(Test-Path $play)){Fail "PlaybackActivity.java not found."}
# SmartTube 32.47s PlaybackActivity references this resource, but some upstream
# source snapshots omit it. Add the missing resource before Java compilation.
$smartStrings=Join-Path $ST "smarttubedroid\src\main\res\values\strings.xml"
if(-not(Test-Path $smartStrings)){Fail "SmartTube strings.xml not found."}
$stxt=ReadT $smartStrings
if($stxt -notmatch 'name="section_is_empty"'){
    $resource='    <string name="section_is_empty">Section is empty</string>'+[Environment]::NewLine
    if($stxt -notmatch '</resources>'){Fail "SmartTube strings.xml has no resources terminator."}
    $stxt=$stxt.Replace('</resources>',$resource+'</resources>')
    WriteT $smartStrings $stxt
}
$t=ReadT $play
# Replace the missing generated resource reference directly in the cloned Java source.
$t=$t.Replace('R.string.section_is_empty', '"Section is empty"')
WriteT $play $t
$t=ReadT $play
if($t -match 'R.string.section_is_empty'){
    Fail "SmartTube section_is_empty source patch did not persist to PlaybackActivity.java."
}
if($t -match 'getString\("Section is empty"\)'){
    $t=$t.Replace('getString("Section is empty")','"Section is empty"')
    WriteT $play $t
    $t=ReadT $play
}
if($t -match 'getString\("Section is empty"\)'){
    Fail "SmartTube section_is_empty patch produced an invalid getString(String) call."
}
# The SmartTube snapshot may omit this generated string resource; keep the build independent of it.
if($t -notmatch 'import android.content.Intent;'){$t=$t.Replace("import android.content.Context;",("import android.content.Context;"+$nl+"import android.content.Intent;"))}

# Keep the phone SmartTube ExoPlayer instance alive across Android HOME/background.
$oldStart=@'
    @Override
    protected void onStart() {
        super.onStart();

        if (VERSION.SDK_INT > 23) {
            initializePlayer();
        }
    }
'@
$newStart=@'
    @Override
    protected void onStart() {
        super.onStart();

        if (VERSION.SDK_INT > 23 && mPlayer == null) {
            initializePlayer();
        }
    }
'@
if($t.Contains($oldStart)){$t=$t.Replace($oldStart,$newStart)}
if($t -notmatch 'VERSION.SDK_INT > 23 && mPlayer == null'){
    Write-Host "NOTE: SmartTube onStart shape differs; keeping upstream onStart." -ForegroundColor Yellow
}

$oldResume=@'
    @Override
    protected void onResume() {
        super.onResume();

        mIsBackPressed = false;

        if (VERSION.SDK_INT <= 23 || mPlayer == null) {
            initializePlayer();
        }

        // NOTE: don't move this into another place! Multiple components rely on it.
        mPlaybackPresenter.onViewResumed();

        showHideWidgets(true); // PIP mode fix
        blockEngine(false); // reset bg mode
    }
'@
$newResume=@'
    @Override
    protected void onResume() {
        super.onResume();

        mIsBackPressed = false;

        if (VERSION.SDK_INT <= 23 || mPlayer == null) {
            initializePlayer();
        }

        // NOTE: don't move this into another place! Multiple components rely on it.
        mPlaybackPresenter.onViewResumed();

        showHideWidgets(true);
        blockEngine(false);
        System.setProperty("nm7.youtube.background", "0");
        try {
            getPlayerData().setBackgroundMode(PlayerData.BACKGROUND_MODE_DEFAULT);
        } catch (RuntimeException ignored) {
        }
    }
'@
if($t.Contains($oldResume)){$t=$t.Replace($oldResume,$newResume)}
if($t -notmatch 'setBackgroundMode(PlayerData.BACKGROUND_MODE_DEFAULT)'){
    Write-Host "NOTE: SmartTube onResume shape differs; keeping upstream onResume." -ForegroundColor Yellow
}
if($t -notmatch 'VERSION.SDK_INT > 23 && mPlayer == null'){
    Write-Host "NOTE: HOME resume-preservation patch did not match this SmartTube source variant." -ForegroundColor Yellow
}

# Patch SmartTube phone lifecycle for deterministic NM7 background playback.
# IMPORTANT: SmartTube Mobile uses PlaybackActivity directly. Do not use a broad regex
# over Java methods here: a malformed regex can delete unrelated methods.

$oldStop=@'
    @Override
    protected void onStop() {
        super.onStop();

        if (VERSION.SDK_INT > 23) {
            maybeReleasePlayer();
        }
    }
'@
$newStop=@'
    @Override
    protected void onStop() {
        super.onStop();

        boolean nm7YoutubeBackground = "1".equals(System.getProperty("nm7.youtube.background", "0"));
        if (VERSION.SDK_INT > 23 && !nm7YoutubeBackground) {
            maybeReleasePlayer();
        }
    }
'@

if($t.Contains($oldStop)){
    $t=$t.Replace($oldStop,$newStop)
}else{
    Fail "SmartTube phone onStop source shape changed; refusing unsafe lifecycle patch."
}

$oldLeave=@'
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();

        // The activity may just be overlapped by a dialog/search: not a real leave
        if (mIsBackPressed || isFinishing() || getViewManager().isNewViewPending()
                || getGeneralData().getBackgroundPlaybackShortcut() == GeneralData.BACKGROUND_PLAYBACK_SHORTCUT_BACK) {
            return;
        }

        switch (getPlayerData().getBackgroundMode()) {
            case PlayerData.BACKGROUND_MODE_PIP:
                enterPipMode();
                if (doNotDestroy()) {
                    blockEngine(true);
                    getViewManager().blockTop(this);
                }
                break;
            case PlayerData.BACKGROUND_MODE_PLAY_BEHIND:
                // 'Play behind' is an Android TV only API (requestVisibleBehind, removed in API 26).
                // On phones it degrades to the sound-only background mode below.
            case PlayerData.BACKGROUND_MODE_SOUND:
                if (doNotDestroy()) {
                    blockEngine(true);
                    getViewManager().blockTop(this);
                }
                break;
        }
    }
'@

$newLeave=@'
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();

        // HOME on NM7 Mobile: explicitly select sound background mode and block engine release.
        if (mIsBackPressed || isFinishing() || getViewManager().isNewViewPending()
                || getGeneralData().getBackgroundPlaybackShortcut() == GeneralData.BACKGROUND_PLAYBACK_SHORTCUT_BACK) {
            return;
        }

        getPlayerData().setBackgroundMode(PlayerData.BACKGROUND_MODE_SOUND);
        System.setProperty("nm7.youtube.background", "1");
        vn.phuongnm7.iptvplayer.SharedPlaybackSession.setTab(this, vn.phuongnm7.iptvplayer.SharedPlaybackSession.TAB_YOUTUBE);
        blockEngine(true);
        getViewManager().blockTop(this);
    }
'@

if($t.Contains($oldLeave)){
    $t=$t.Replace($oldLeave,$newLeave)
}else{
    Fail "SmartTube phone onUserLeaveHint source shape changed; refusing unsafe lifecycle patch."
}

if($t -notmatch 'void\s+onUserLeaveHint\s*\(' -or
   $t -notmatch 'setBackgroundMode\(PlayerData\.BACKGROUND_MODE_SOUND\)' -or
   $t -notmatch 'setProperty\("nm7\.youtube\.background", "1"\)' -or
   $t -notmatch 'boolean\s+nm7YoutubeBackground'){
    Fail "SmartTube HOME lifecycle patch is incomplete."
}

# Lock-screen transitions can arrive through onPause without onUserLeaveHint.
# Force Play-Behind before the engine is blocked so video/audio continues.
$pauseScreen = 'boolean isScreenOff = getPlayerData().getBackgroundMode() != PlayerData.BACKGROUND_MODE_DEFAULT && Utils.isHardScreenOff(this);'
$pauseScreenNew = 'boolean isScreenOff = Utils.isHardScreenOff(this);' + $nl + '        if (isScreenOff) {' + $nl + '            getPlayerData().setBackgroundMode(PlayerData.BACKGROUND_MODE_SOUND);' + $nl + '            if (doNotDestroy()) {' + $nl + '                blockEngine(true);' + $nl + '                getViewManager().blockTop(this);' + $nl + '            }' + $nl + '        }'
if($t.Contains($pauseScreen)){
    $t=$t.Replace($pauseScreen,$pauseScreenNew)
}

# Back from the SmartTube player must return to the phone Browse screen.
if($t -notmatch 'FLAG_ACTIVITY_REORDER_TO_FRONT\s*\|\s*Intent\.FLAG_ACTIVITY_NO_ANIMATION'){
$oldBack=@'
    @Override
    public void onBackPressed() {
        mIsBackPressed = true;

        super.onBackPressed();
    }
'@
$newBack=@'
    @Override
    public void onBackPressed() {
        if (mIsBackPressed) {
            return;
        }
        mIsBackPressed = true;
        try {
            Intent intent = new Intent(this,
                    Class.forName("com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity"));
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
            overridePendingTransition(0, 0);
        } catch (ReflectiveOperationException | RuntimeException e) {
            super.onBackPressed();
        }
    }
'@
if($t.Contains($oldBack)){$t=$t.Replace($oldBack,$newBack)}
}
WriteT $play $t


$appJava=ReadT (Join-Path $Root "app\src\main\java\vn\phuong\iptvplayer\MobileNm7Application.java")
$appUi=ReadT (Join-Path $Root "app\src\main\java\vn\phuong\iptvplayer\MobileIptvUi.java")
$appManifest=ReadT (Join-Path $Root "app\src\main\AndroidManifest.xml")
if($appJava -notmatch "replaceSmartTubeBranding|finishReally"){Fail "NM7 runtime fixes are missing from source."}
if($appJava -notmatch "SmartTubeRuntime\.enableBackgroundPlayback|releasePlayer"){Fail "Cross-player handoff/background playback fix is missing from source."}
if((ReadT (Join-Path $Root "app\src\main\java\vn\phuong\iptvplayer\SmartTubeRuntime.java")) -notmatch "PLAYER_DATA_SOURCE_CRONET|BACKGROUND_MODE_PLAY_BEHIND|BACKGROUND_PLAYBACK_SHORTCUT_HOME"){Fail "SmartTube fast/background playback runtime patch is missing from source."}
if($appUi -notmatch "nm7_inline_player|repositionToolbar"){Fail "IPTV toolbar placement fix is missing from source."}
if($appManifest -notmatch 'MainActivity.*launchMode="singleTask"'){Fail "MainActivity singleTask preservation fix is missing."}

$ui=Join-Path $ST "exoplayer-amzn-2.10.6\library\ui"; $res=Join-Path $ui "src\main\res\layout"; $pv=Join-Path $ui "src\main\java\com\google\android\exoplayer2\ui\PlayerView.java"
foreach($p in @((Join-Path $res "exo_player_view.xml"),(Join-Path $res "exo_simple_player_view.xml"),$pv)){if(-not(Test-Path $p)){Fail "Missing player source: $p"}}
Copy-Item (Join-Path $res "exo_player_view.xml") (Join-Path $res "st_exo_player_view.xml") -Force
Copy-Item (Join-Path $res "exo_simple_player_view.xml") (Join-Path $res "st_exo_simple_player_view.xml") -Force
$p=Join-Path $res "st_exo_player_view.xml"; WriteT $p (ReadT $p).Replace("@layout/exo_simple_player_view","@layout/st_exo_simple_player_view")
$t=ReadT $pv; $n=$t.Replace("R.layout.exo_player_view","R.layout.st_exo_player_view"); if($n -eq $t){Fail "PlayerView layout reference not found."}; WriteT $pv $n

$changed=0
$roots=@(
  (Join-Path $ST "exoplayer-amzn-2.10.6\library\ui\src\main\java"),
  (Join-Path $ST "exoplayer-amzn-2.10.6\library\ui\src\main\res")
)
foreach($r in $roots){
  if(Test-Path $r){
    Get-ChildItem $r -Recurse -File | Where-Object {$_.Extension -in @(".java",".xml")} | ForEach-Object {
      $x=ReadT $_.FullName
      if($x.Contains("show_buffering")){
        $n=$x.Replace("show_buffering","st_show_buffering")
        if($n -ne $x){WriteT $_.FullName $n;$changed++}
      }
    }
  }
}
if($changed -eq 0){Fail "show_buffering patch found no references in legacy ExoPlayer UI."}

$doh=Join-Path $ST "SharedModules\sharedutils\src\main\java\com\liskovsoft\sharedutils\okhttp\DohProviders.kt"
if(Test-Path $doh){$t=ReadT $doh;$t=$t.Replace("import okhttp3.toHttpUrlOrNull"+$nl,"");$t=$t.Replace('return s.toHttpUrlOrNull() ?: throw NullPointerException("unable to parse url")','return HttpUrl.get(s)');$t=$t.Replace('return HttpUrl.parse(s) ?: throw NullPointerException("unable to parse url")','return HttpUrl.get(s)');WriteT $doh $t}

Remove-Item -Recurse -Force (Join-Path $Root "app\build"),$Dist -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $Dist | Out-Null
$ga=@("--no-daemon","--console=plain","-PcompileSdkVersion=android-36","-PminSdkVersion=23","-PtargetSdkVersion=36","-PbuildToolsVersion=36.0.0","-PtestXSupportVersion=1.1.0","-PannotationXVersion=1.1.0","-ProbolectricVersion=4.6.1","-PmobileAbiSplits=true",":app:testMobileDebugUnitTest","assembleMobileDebug","-x",":slidableactivity:testMobileDebugUnitTest")
Run $Gradle $ga

$out=Join-Path $Root "app\build\outputs\apk\mobile\debug"
$apks=@(Get-ChildItem $out -Filter "app-mobile-*-debug.apk" -File)
if($apks.Count -ne 2){Fail "Expected exactly 2 APKs, found $($apks.Count)."}
$a64=$apks|Where-Object{$_.Name -like "*arm64-v8a*"}
$a7=$apks|Where-Object{$_.Name -like "*armeabi-v7a*"}
if(-not $a64 -or -not $a7){Fail "Expected arm64-v8a and armeabi-v7a APKs."}

$sign=Join-Path $env:ANDROID_HOME "build-tools\$BT\apksigner.bat"
foreach($a in @($a64,$a7)){
    Run $sign @("verify","--verbose","--print-certs",$a.FullName)
    $z=@(& jar tf $a.FullName)
    if(($z -join [Environment]::NewLine) -match "lib/x86/|lib/x86_64/|libvlc\.so"){Fail "Forbidden ABI/VLC content in $($a.Name)."}
}

$z64=@(& jar tf $a64.FullName)
$native64=@($z64 | Where-Object { $_ -match '^lib/[^/]+/[^/]+\.so$' })
$arm64Entries=@($native64 | Where-Object { $_ -match '^lib/arm64-v8a/' })
$wrong64=@($native64 | Where-Object { $_ -match '^lib/armeabi-v7a/|^lib/x86/|^lib/x86_64/' })
if($arm64Entries.Count -eq 0 -or $wrong64.Count -gt 0){
    Write-Host "arm64 APK native entries:" -ForegroundColor Yellow
    $native64 | ForEach-Object { Write-Host ("  "+$_) }
    Fail "arm64 ABI validation failed."
}

$z7=@(& jar tf $a7.FullName)
$native7=@($z7 | Where-Object { $_ -match '^lib/[^/]+/[^/]+\.so$' })
$arm7Entries=@($native7 | Where-Object { $_ -match '^lib/armeabi-v7a/' })
$wrong7=@($native7 | Where-Object { $_ -match '^lib/arm64-v8a/|^lib/x86/|^lib/x86_64/' })
if($arm7Entries.Count -eq 0 -or $wrong7.Count -gt 0){
    Write-Host "armeabi-v7a APK native entries:" -ForegroundColor Yellow
    $native7 | ForEach-Object { Write-Host ("  "+$_) }
    Fail "armv7 ABI validation failed."
}

$manifest=Join-Path $Root "app\build\intermediates\merged_manifest\mobileDebug\processMobileDebugManifest\AndroidManifest.xml"
if(Test-Path $manifest){
    $m=ReadT $manifest
    if($m -match "TvLayoutTunerProvider|TvStreamRecoveryProvider|TvPlayerUiProvider|VlcFallbackActivity|LEANBACK_LAUNCHER"){
        Fail "TV/VLC component detected in Mobile manifest."
    }
}

$bg=ReadT (Join-Path $Root "app\build.gradle.kts")
$mm=[regex]::Match($bg,'versionName\s*=\s*"([^"]+)"')
$v="1.10.26"
if($mm.Success){$v=$mm.Groups[1].Value}

Copy-Item $a64.FullName (Join-Path $Dist "NM7-IPTV-Mobile-$v-arm64-v8a.apk") -Force
Copy-Item $a7.FullName (Join-Path $Dist "NM7-IPTV-Mobile-$v-armeabi-v7a.apk") -Force
Write-Host ""
Write-Host "=== BUILD SUCCESSFUL ===" -ForegroundColor Green
Write-Host "APK output: $Dist"
Get-ChildItem $Dist
