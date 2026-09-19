# HOTFIX 1.10.31 — 2026-09-19 — XỬ LÝ DỨT ĐIỂM 2 LỖI CÒN LẠI

## 1. IPTV → YouTube: không được dừng IPTV khi chỉ chuyển tab

Đã bổ sung một cờ handoff cấp process: `nm7.iptv.youtube.handoff`.

Luồng mới:
1. Đang phát IPTV.
2. Bấm tab YouTube → đánh dấu handoff đang chờ.
3. `PlayerActivity.onPause()` và `onStop()` đều giữ ExoPlayer nếu handoff còn chờ.
4. Browse YouTube chỉ là navigation, không gọi release IPTV.
5. Chỉ khi SmartTube `PlaybackActivity` thực sự bắt đầu thì `pauseIptvPlayer()` mới pause + release IPTV và xóa cờ handoff.
6. Khi quay lại IPTV, cờ handoff được xóa trước khi đưa PlayerActivity lên foreground.

Mục tiêu chính: **IPTV tiếp tục phát trong thời gian người dùng chỉ đang duyệt YouTube; YouTube PlaybackActivity mới có quyền lấy decoder khi video thực sự mở.**

## 2. YouTube BACK 1 lần → Browse + mini-player

Đã thay đổi patch Mobile SmartTube theo hướng không gọi `finish()` trong BACK.

BACK lần đầu hiện:
- giữ `PlaybackActivity` và ExoPlayer sống;
- bật trạng thái mini-player;
- đặt background mode của SmartTube;
- gọi trực tiếp `getViewManager().blockTop(this)`;
- gọi `getViewManager().startParentView(this)`;
- BrowseActivity cài mini-player trong `onResume()`, kể cả khi Browse đã tồn tại;
- chuyển output video sang `TextureView` của mini-player;
- ép `playWhenReady=true` khi attach;
- khi mở lại mini-player, trả output về PlayerView trước khi đưa PlaybackActivity lên trước.

Cách này tránh đường `finish() → enterPipMode()`, vốn có thể đưa video vào Android system PiP thay vì mini-player nội bộ của NM7.

## 3. Version / commit

- Mobile version: **1.10.31**
- versionCode: **49**
- Build script: **PATCH16**
- Commit handoff guard: `310f59ee64243d3558676755009a81f7ae97594c`
- Commit tab ordering: `366e17fe1c3c7095e432f62b22985678493eca43`
- Commit playback-start handoff cleanup: `dc959cd1d591f159779ceaf53cfe331113f2317d`
- Commit mini-player lifecycle: `440e76f2cb67034eaa04c81092764537f82b5889`
- Commit final BACK parent-view path + PATCH16: `b904efbd91f820fb9420c8b1c0ea910cbfe5da71`
- Version bump: `02ed4332657f2e7a435e50bc90783b6967c62905`

## 4. Trạng thái

**Chưa build và chưa xác nhận máy thật 1.10.31.** Không đánh dấu PASS trước khi APK được build và test.

Checklist bắt buộc:
1. IPTV đang phát → YouTube tab, **không chọn video** → IPTV vẫn phát.
2. Ở Browse YouTube 10–20 giây → IPTV vẫn không bị release.
3. Chọn video YouTube → IPTV mới dừng.
4. YouTube đang phát → BACK 1 lần → về Browse và xuất hiện mini-player.
5. Video trong mini-player vẫn chạy.
6. Bấm mini-player → trở lại full player, video tiếp tục.
7. Lặp IPTV → YouTube → video → BACK → IPTV ít nhất vài lần.
8. Không đưa TV UI/code vào bản Mobile.

---
# HOTFIX 1.10.30 — 2026-09-19 — XỬ LÝ LỖI THỰC TẾ TỪ VIDEO 1.10.29

## Phát hiện từ video máy thật mới nhất

Video người dùng gửi: video_2026-09-19_08-04-08.mp4, dài khoảng 80 giây.

Quan sát chuỗi hình trong video:
- YouTube Browse mở được và video YouTube bắt đầu phát.
- Khi rời YouTube về Home/ra khỏi ứng dụng, trạng thái phát không được giữ ổn định.
- Khi quay lại, player YouTube có trạng thái spinner/loading thay vì tiếp tục ngay trạng thái video đang phát.
- Không có mini-player đúng yêu cầu khi quay lại Browse.
- Vì vậy 1.10.29 KHÔNG được coi là đạt yêu cầu runtime.

## Nguyên nhân mã đã xác định trong 1.10.29

### 1. Mini-player chỉ được cài trong BrowseActivity.onCreate()

1.10.29 gọi installNm7MiniPlayer() trong onCreate(). Nhưng HomeTabBar/PlaybackActivity đưa BrowseActivity lên foreground bằng REORDER_TO_FRONT. Nếu BrowseActivity đã tồn tại, onCreate() không chạy lại, nên mini-player không được tạo khi BACK.

**Sửa 1.10.30:**
- Chuyển hook mini-player sang BrowseActivity.onResume().
- Giữ onCreate() chỉ khởi tạo Browse.
- Mỗi lần Browse được đưa lên foreground, onResume() kiểm tra PlaybackActivity.isMiniPlayerActive() và attach TextureView nếu cần.

### 2. HOME/background chỉ dựa vào onUserLeaveHint()

1.10.29 phụ thuộc quá nhiều vào onUserLeaveHint() để đặt:
- BACKGROUND_MODE_SOUND
- nm7.youtube.background=1
- SharedPlaybackSession TAB_YOUTUBE/background
- blockEngine(true)

Trên thiết bị thực tế, đường lifecycle này không đủ tin cậy để bảo đảm player không bị release trước khi onStop().

**Sửa 1.10.30:**
- Thêm fallback chính xác vào PlaybackActivity.onPause().
- Chỉ coi đây là HOME/background khi:
  - không phải BACK: !mIsBackPressed
  - Activity chưa finishing
  - ViewManager không có view transition pending
  - mPlayer != null và đang playWhenReady.
- Khi thỏa điều kiện, lưu tab YouTube, đặt background sound, đặt nm7.youtube.background=1 và block engine trước khi Activity tiếp tục pause.
- onStop() tiếp tục không gọi maybeReleasePlayer() khi nm7.youtube.background=1.

### 3. onResume() của 1.10.29 tự xóa trạng thái background

Patch 1.10.29 từng thêm:
- nm7.youtube.background=0
- BACKGROUND_MODE_DEFAULT
ngay trong onResume().

Điều này có thể xóa trạng thái background trong lúc Android đang khôi phục task/player.

**Sửa 1.10.30:**
- Bỏ việc ghi đè trạng thái background trong onResume().
- onResume() chỉ khôi phục player nếu thực sự mPlayer == null và mở lại UI engine; không xóa marker HOME.

## Các thay đổi 1.10.30

- versionCode: 48.
- versionName: 1.10.30.
- Build script commit hotfix: e8d6e5abea188277cd63c06cfab5a880c370d40c.
- Version bump commit: 9f820dec0e84316d73e82e836f76101b89560888.
- Đây là hotfix tập trung vào 2 lỗi mới nhất: HOME/background và mini-player resume. Không thay đổi IPTV player engine/network.

## Cảnh báo

- Chưa build 1.10.30 sau hotfix.
- Chưa xác nhận runtime.
- Không được bàn giao APK 1.10.30 cho người dùng trước khi build SUCCESSFUL.
- Sau build phải test lại đúng video flow, đặc biệt:
  1. mở YouTube → phát video;
  2. bấm HOME → chờ 10–20 giây → kiểm tra audio/video còn chạy;
  3. quay lại app → video tiếp tục, không spinner/reload;
  4. YouTube video → BACK 1 lần → Browse + mini-player;
  5. bấm mini-player → full player;
  6. IPTV → YouTube Browse không chọn video → IPTV vẫn phát;
  7. chọn video → IPTV mới release.

---

# BÀN GIAO TIẾN ĐỘ — NM7 IPTV MOBILE 1.10.29 — 2026-09-19

## Trạng thái hiện tại — BUILD THÀNH CÔNG, CHỜ TEST MÁY THẬT

- Repo: phuongnm7/iptv-player-android.
- Branch: fix/mobile-1.10.26-sleep-timer-icon.
- Mobile version: versionCode 47, versionName 1.10.29.
- Phạm vi: CHỈ Android Mobile. Không đưa Android TV vào bản Mobile.
- SmartTube base: build script clone/merge SmartTube upstream tag 32.47s rồi áp dụng patch Mobile.
- Môi trường Windows đã xác nhận: JDK 17, Gradle 8.13, Android SDK 36, Build Tools 36.0.0.
- Build ngày 2026-09-19: SUCCESS. Unit test Mobile và assemble Mobile hoàn tất; tạo đúng 2 APK ARM, không universal.
- APK ARM64: NM7-IPTV-Mobile-1.10.29-arm64-v8a.apk — 61,915,327 bytes.
- APK ARMv7: NM7-IPTV-Mobile-1.10.29-armeabi-v7a.apk — 51,421,069 bytes.
- Các warning META-INF/* not protected by signature trong log là warning ký JAR, không phải build failure.
- Chưa có xác nhận runtime cho 1.10.29. Người dùng đang cài bản này để test. Không đánh dấu lỗi YouTube/IPTV là đã hết chỉ vì build thành công.

## Mục tiêu runtime của 1.10.29

### 1. IPTV → YouTube

Yêu cầu:
1. IPTV đang phát.
2. Chuyển sang tab YouTube nhưng chưa chọn video: IPTV phải tiếp tục phát.
3. Có thể chuyển IPTV ↔ YouTube nhiều lần: IPTV không được tự dừng/release chỉ vì mở Browse.
4. Chỉ khi SmartTube PlaybackActivity thực sự bắt đầu video thì IPTV mới pause/release.

Các thay đổi:
- PlayerActivity.prepareForYoutubeHandoff() không còn release IPTV khi chỉ mở YouTube Browse.
- HomeTabBar.openBrowse() dùng REORDER_TO_FRONT và không finish PlayerActivity.
- PlayerActivity.onStop() giữ player khi Activity chưa finishing và tab hiện tại là YouTube hoặc tab switch đang pending.
- MobileNm7Application có pauseIptvForYoutube() để SmartTube yêu cầu release IPTV trước khi tạo decoder.
- SmartTube PlaybackActivity.onStart() gọi helper này trước initializePlayer(), tránh race giữa decoder YouTube và IPTV.
- Application lifecycle vẫn có lớp bảo vệ thứ hai khi SmartTube PlaybackActivity thực sự bắt đầu.

### 2. YouTube → BACK lần đầu → mini-player

Yêu cầu:
- YouTube đang phát → BACK 1 lần → hiện YouTube Browse/Home và video tiếp tục trong mini-player.
- Không được hiện launcher, màn hình trắng hoặc splash.
- BACK đầu tiên không được finish PlaybackActivity.
- Bấm mini-player phải quay lại player lớn.
- Đóng mini-player phải dừng/finish player.

Kiến trúc 1.10.29:
- Không dùng startParentView() làm cơ chế mini-player chính.
- PlaybackActivity.onBackPressed() được patch để đặt sMiniPlayerActive, giữ player và đưa BrowseActivity lên foreground bằng FLAG_ACTIVITY_REORDER_TO_FRONT | FLAG_ACTIVITY_NO_ANIMATION.
- Thêm sActiveInstance và sMiniPlayerActive.
- BrowseActivity tạo FrameLayout mini-player và TextureView.
- attachMiniPlayer() chuyển output video sang TextureView.
- restoreFromMiniPlayer() khôi phục player view lớn.
- closeMiniPlayer() tắt mini-player và finish PlaybackActivity.
- SmartTube onStop() không release player khi mini-player đang active.
- Đã xóa BACK patch thứ hai dùng startParentView() vì nó từng ghi đè patch mini-player.

### 3. HOME / khóa màn hình YouTube

Các patch background playback từ các vòng trước được giữ:
- onUserLeaveHint() chọn BACKGROUND_MODE_SOUND, đặt nm7.youtube.background=1, lưu tab YouTube và chặn engine release.
- Screen-off path chọn sound background và giữ engine khi có thể.
- SharedPlaybackSession/MainActivity hỗ trợ lưu tab YouTube và khôi phục Browse/Playback khi resume.
- Vẫn phải xác nhận lại bằng test máy thật.

## Các lỗi build/patch đã gặp và đã xử lý trong vòng 1.10.29

1. Biến PowerShell $play undefined → khai báo PlaybackActivity path đúng trước khi đọc source.
2. SmartTube onStart thực tế dùng VERSION.SDK_INT > 23 rồi initializePlayer() → patch được điều chỉnh.
3. SmartTube onStop thực tế dùng VERSION.SDK_INT > 23 rồi maybeReleasePlayer() → patch được điều chỉnh.
4. SmartTube phone onBackPressed thực tế có onDetailsBack() rồi super.onBackPressed() → patch đúng source shape.
5. Script còn BACK patch thứ hai dùng startParentView() và ghi đè patch mới → đã xóa.
6. Compile thiếu android.content.Intent → đã ghi import vào PlaybackActivity.java trước Gradle.
7. SimpleExoPlayer.setVideoTextureView() đã được kiểm tra có trong API ExoPlayer phù hợp.
8. Build cuối cùng đã vượt qua compile/package và tạo đủ 2 APK ARM.

## Các commit/điểm mã quan trọng

- 02c7f32f9d4d58f1fe01aaf94623576a95f38b45 — Keep IPTV playing until YouTube video starts.
- 2cc0d9901b3848569364f1a4d8bcee85e2e10102 — Keep IPTV alive while browsing YouTube.
- b1ceb839c5f3e2274a011d0e65639c85e5a68fde — Make IPTV YouTube handoff lifecycle explicit.
- 0414fcb802537a74789bc3f6cc6c37351e09d488 — Keep IPTV player alive while YouTube tab is browsing.
- 95cc48dc0830ae0436d23a0f658110902a1f1589 — Use native SmartTube Back dispatch for mini-player; không re-add OnBackInvokedCallback cũ.
- 58bd32d3d695a2e2af764d7e83ac7a6a3387c3a0 — Bump Mobile version for tab handoff and mini-player fix.
- 284e8697f9857b5cf879b201f2ba94e17cfa715a — Record 1.10.28 playback fixes.
- 74324d8 — Implement true YouTube mini-player on Mobile Back.
- eff1ccd — Allow mini-player lifecycle patch in Mobile build script.
- 5917488 — Fix Browse mini-player patch write target.
- 729d14f — Remove stale parent-view BACK validation.
- 4c6a58e — Match SmartTube PlaybackActivity onStart source shape.
- ba299166 — Match actual SmartTube PlaybackActivity lifecycle for mini-player.
- 095f22a — Match actual SmartTube phone onBackPressed source shape.
- 02aa40d1d59213f34f109ae4fc6fc01f9a8599af — Remove duplicate BACK patch that overwrote mini-player.
- 4bf09ac6ea4bc005d136f3e1ccfb6d14e69288c2 — Persist Intent import for YouTube mini-player compile.
- b40feb4 — Fix missing PlaybackActivity path in build script.
- 1.10.29: versionCode 47, versionName 1.10.29.

## Các file/area quan trọng để bàn giao

- scripts/build-mobile-windows.ps1: clone/merge SmartTube, patch lifecycle/mini-player, patch ExoPlayer resource conflict, build/test/package/ABI validation.
- app/build.gradle.kts: Mobile-only flavor, version 1.10.29, ARM split chỉ armeabi-v7a + arm64-v8a.
- app/src/main/java/vn/phuong/iptvplayer/PlayerActivity.java: IPTV lifecycle, position persistence, YouTube handoff.
- app/src/main/java/vn/phuong/iptvplayer/MobileNm7Application.java: cross-player lifecycle, pause/release IPTV khi YouTube PlaybackActivity thực sự khởi động, task/tab restoration.
- app/src/main/java/vn/phuong/iptvplayer/SharedPlaybackSession.java: lưu tab IPTV/YouTube và trạng thái playback.
- app/src/main/java/vn/phuong/iptvplayer/HomeTabBar.java: chuyển IPTV ↔ YouTube bằng Activity reorder.
- app/src/main/java/vn/phuong/iptvplayer/MainActivity.java: restore YouTube tab/SmartTube Activity khi resume.
- SmartTube generated/patch-at-build files không nằm cố định trong repo; script clone/merge rồi patch lại mỗi lần build. Khi source SmartTube thay đổi phải kiểm tra source sau merge trước khi sửa patch.

## Checklist test 1.10.29 — CHƯA HOÀN TẤT

### IPTV/YouTube handoff
- [ ] IPTV phát → mở YouTube Browse → IPTV vẫn phát.
- [ ] YouTube Browse không chọn video → quay IPTV → IPTV vẫn phát.
- [ ] Lặp IPTV → YouTube → IPTV ít nhất 5 lần → không tự dừng/release IPTV.
- [ ] Chọn video YouTube → IPTV dừng/release chỉ khi YouTube PlaybackActivity thực sự bắt đầu.
- [ ] Mở video YouTube bình thường, không crash.

### YouTube mini-player
- [ ] YouTube đang phát → BACK 1 lần → Browse/Home + mini-player.
- [ ] Video vẫn chạy, âm thanh/hình ảnh không mất.
- [ ] Không xuất hiện launcher/blank/splash.
- [ ] Bấm mini-player → trở lại full player.
- [ ] BACK tiếp theo có hành vi đúng theo UX mong muốn.
- [ ] HOME/khóa màn hình → YouTube tiếp tục background.
- [ ] Mở app lại → vẫn ở YouTube, không nhảy về IPTV.

### Regression
- [ ] IPTV player vẫn phát bình thường.
- [ ] Search/group/channel UI không bị thay đổi ngoài yêu cầu.
- [ ] Logo NM7 Mobile giữ kích thước đã sửa.
- [ ] Không có TV flavor/TV manifest/component trong APK Mobile.
- [ ] Chỉ tạo đúng ARM64 + ARMv7, không universal/x86.

## Quy tắc xử lý nếu 1.10.29 FAIL runtime

1. Không sửa hàng loạt lifecycle cùng lúc.
2. Nếu IPTV dừng khi chỉ mở Browse: lấy logcat quanh thời điểm chuyển tab, tập trung PlayerActivity onPause/onStop/onDestroy, MobileNm7Application onActivityStarted/onActivityStopped và SmartTube PlaybackActivity onStart/onStop.
3. Nếu YouTube mở video rồi crash: lấy FATAL EXCEPTION và khoảng 100–200 dòng log trước/sau; kiểm tra mini-player field/method trước khi đổi kiến trúc.
4. Nếu BACK hiện launcher/blank: xác định Activity/task transition trước; không quay lại startParentView() chỉ để che hiện tượng.
5. Nếu audio còn nhưng mini-player không có hình: kiểm tra ownership video surface/TextureView và thời điểm setPlayer(null)/setVideoTextureView().
6. Nếu BACK vẫn finish Activity: kiểm tra Android legacy onBackPressed versus predictive/onBackInvoked và Activity stack; không tự động re-add callback cũ.
7. Nếu build script lại báo source shape changed: kiểm tra source SmartTube sau merge tag 32.47s trước khi sửa.
8. Chỉ tối ưu tốc độ load YouTube/playlist sau khi lifecycle/runtime ổn định.

## Bàn giao

- Hiện tại source 1.10.29 đã build thành công trên Windows; người dùng đang cài/test.
- Việc tiếp theo: ghi kết quả test máy thật vào mục này. Nếu FAIL, ưu tiên video + logcat và sửa đúng nguyên nhân. Nếu PASS, ghi kết quả và tạo mốc/tag bàn giao 1.10.29.
- Không coi BUILD SUCCESSFUL là RUNTIME SUCCESSFUL.

---

# CẬP NHẬT QUAN TRỌNG — KHÔI PHỤC LỊCH SỬ WINDOWS POWERSHELL VÀ KẾT QUẢ TEST THỰC TẾ — 2026-09-18

## Mục đích của mốc này

- Người dùng xác nhận bản Mobile 1.10.26 vừa cài vẫn còn **các lỗi cũ**: chuyển IPTV → YouTube vẫn lỗi, YouTube/Home lifecycle chưa đúng, quay lại tab YouTube chưa ổn định, YouTube mở video còn chậm và giao diện YouTube chưa hoàn toàn mobile.
- Không coi việc build thành công là đã sửa lỗi runtime.
- Phần này ghi lại lại đầy đủ chuỗi sửa lỗi đã được thực hiện trước đó bằng **PowerShell trên Windows**, để vòng xử lý tiếp theo không làm mất các thay đổi/giả thuyết đã thử.
- Phạm vi tiếp tục: **chỉ NM7 IPTV Mobile 1.10.26**. Không đưa mã Android TV vào bản Mobile.

## Điểm mã đang dùng để tiếp tục

- Nhánh: `fix/mobile-1.10.26-sleep-timer-icon`.
- Mốc SmartTube lifecycle đã kiểm tra gần nhất: `5bf1fbec30ea7a19512f1df743174746bb4754f8` — `validate YouTube background lifecycle patch`.
- Sau đó nhánh đã từng đi qua chuỗi thay đổi SharedPlaybackSession/lifecycle, nhưng các thay đổi đó đã bị loại khỏi nhánh chính để tránh tiếp tục trên nền đang gây hồi quy.
- Hiện tại **không được coi `5bf1fbec` là bản đã hết lỗi**; test máy thật mới nhất xác nhận lỗi vẫn tái hiện. Đây là điểm rất quan trọng để vòng sửa tiếp theo không nhầm “baseline” với “đã sửa xong”.

## Lịch sử các sửa lỗi Windows/PowerShell đã thực hiện

### 1. Nền SmartTube + Windows build
- `d403a20aa1988b7850d369b84ce784363b271cf9` — ổn định SmartTube Windows và các patch source/CI.
- `b70293744f157a062e4b6fa7a8189909d64fbea3` — ghi nhận tiến độ Mobile 1.10.26 và Windows build.
- `0a600a430b1298cef496fc4b2b2f42cf79a056e1` — harden script Windows cho Mobile UI/playback.
- `42e0a136d2d68f8d29d8feaa696ca3992d43fcc2` — bỏ global-layout recursion tốn chi phí trong SmartTube.
- `9d017590483fc5f1f2da38bbe1c7236e128235fc` — dọn import của performance patch SmartTube.
- Môi trường Windows đã xác nhận: **JDK 17, Gradle 8.13, Android SDK 36, Build Tools 36.0.0**.

### 2. YouTube UI/lifecycle và chuyển tab
- `b988bd81cda4` — sửa clock cho tab switch và timing prewarm.
- `7dfa72e6b0fa` — sửa SmartTube Play-Behind và lock handling.
- `3246bd97654a` — ép feed YouTube một cột sau khi RecyclerView attach.
- `3c0edbf5b380` — xử lý background playback khi khóa màn hình.
- `8d0280508db2` — sửa feed patch và force YouTube background playback.
- `629118aad795` — áp dụng background mode trước player resume.
- `43c504672b88` — gắn nhãn foreground service cho YouTube background playback.
- `e259a39a02c3` — dừng YouTube keepalive khi foreground media thay đổi.
- `089c2b61782e` — thêm helper giữ YouTube foreground.
- `7ac76d885fb7` — giữ SmartTube player trong background.
- `bebe7bc15303` — giữ YouTube playback sống khi chạy nền.
- `e76c1323b20f` — sửa cách CI tạo/apply YouTube background patch.
- `5ec48c6e5afb` — sửa lifecycle IPTV khi chỉ chuyển tab, tránh khởi động background service sai lúc đổi tab.
- `e5d461aeae93` — sửa handoff foreground YouTube.
- `91724438d7f5` — không ép YouTube background mode trong lúc prewarm.
- `ac471c452bb7` — không ép SmartTube background mode khi mở video.
- `ef77dbba582b` — sửa lifecycle HOME của YouTube Mobile.
- `5bf1fbec30ea` — thêm validation trong script để bảo đảm patch HOME-resume-preservation tồn tại.

### 3. UI Mobile
- `8729326843c9` — thu nhỏ và nâng logo NM7 Mobile.
- Trước đó đã có các sửa đưa toolbar/search/tùy chọn xuống dưới player và ép YouTube recommendations một cột; các thay đổi này phải được phân biệt với bản TV.

## Nhánh SharedPlaybackSession đã thử và lý do không dùng làm baseline

Chuỗi thử nghiệm sau `5bf1fbec` đã đưa vào:
- `SharedPlaybackSession.java` để lưu tab IPTV/YouTube, IPTV URL/name/mime/headers/position/playing.
- `PlayerActivity` lưu/khôi phục IPTV position và handoff sang YouTube.
- `HomeTabBar` cố gắng giữ đúng tab và dùng `REORDER_TO_FRONT`.
- `MainActivity` khôi phục tab YouTube sau process recreation.
- `MobileNm7Application` thay đổi thứ tự pause/release khi SmartTube Playback khởi động.
- Manifest bỏ `singleTask`, thêm `alwaysRetainTaskState=true`.
- UI giảm logo và tăng vùng player.

Đây là **thử nghiệm kiến trúc**, không phải kết quả đã xác nhận. Video máy thật sau đó cho thấy YouTube vẫn có thể load rồi xuất hiện splash/launcher và quay lại app, vì vậy không tiếp tục cộng thêm logic vào nhánh thử nghiệm này khi chưa có logcat xác định exception/lifecycle transition.

## Kết quả build Windows sau khi quay về baseline

- Nhánh đã được đưa về `5bf1fbec` để tách lỗi runtime khỏi chuỗi SharedPlaybackSession đang gây hồi quy.
- Các commit CI sau đó chỉ phục vụ đóng gói Mobile ARM:
  - `688b84b83505` — khôi phục ARM ABI split.
  - `577f8aee94be` — không để emulator smoke test chặn APK Mobile.
  - `0ea25e5e4f67` — sửa tìm đầu ra ARM64/ARMv7 trong thư mục output.
- Build thành công với đúng **2 APK ARM**, không universal:
  - ARM64-v8a: khoảng 61.9 MB.
  - armeabi-v7a: khoảng 51.4 MB.
- Đây chỉ xác nhận **compile/package**, không xác nhận lỗi YouTube/Android lifecycle đã hết.

## Kết luận để tiếp tục xử lý

- Không reset lại lịch sử các patch Windows/PowerShell ở trên.
- Không tiếp tục khẳng định SharedPlaybackSession là lời giải.
- Lỗi hiện tại phải được xử lý theo chuỗi: **tái hiện → lấy logcat đúng thời điểm YouTube chuyển từ IPTV → YouTube → xác định Activity/task/process nào bị finish/recreate → sửa một nguyên nhân → build Windows → test máy thật**.
- Đặc biệt phải phân biệt ba tình huống: (1) YouTube Activity bị finish, (2) SmartTube player bị release nhưng Activity còn, (3) toàn bộ NM7 process bị crash/restart. Video hiện tại chỉ chứng minh có hiện tượng rơi ra khỏi trạng thái YouTube và xuất hiện splash/launcher; chưa đủ để kết luận exception cụ thể.

---

# GHI NHẬN LỖI MỚI — NM7 IPTV MOBILE 1.10.24: chưa tự tải lại playlist khi mở ứng dụng (2026-09-14)

## Trạng thái Mobile hiện tại

- Repo chuẩn: `phuongnm7/iptv-player-android`, nhánh `main`, vẫn ở chế độ private.
- Bản ổn định mới nhất: `versionCode 41`, `versionName 1.10.24`. Người dùng đã xác nhận lỗi xóa nguồn đang sử dụng và quay về nguồn mặc định đã hoạt động đúng.
- 1.10.24 tiếp tục là baseline ổn định. Lỗi tải lại khi khởi động được ghi nhận để xử lý ở phiên bản tiếp theo; chưa thay đổi mã player, nguồn, Mobile UI, TV flavor, iOS hoặc Tizen trong bước kiểm tra này.

## Kết quả kiểm tra lỗi tải lại playlist

- Xác nhận lỗi tồn tại trong mã `main`: `MainActivity.onCreate()` gọi `restoreSession()`.
- Khi `SessionStore` có danh sách kênh hợp lệ, `restoreSession()` chỉ gọi `showPlaylist(state.result, state.source)`, điền URL và tắt trạng thái loading; không gọi `reloadPlaylistUrl()` hoặc `loadFromUrl()`.
- Vì vậy ứng dụng hiển thị nhanh playlist đã cache nhưng không kiểm tra nội dung mới từ URL khi mở lại. Tính năng nút **Tải lại** vẫn hoạt động và đã gửi `Cache-Control: no-cache`, `Pragma: no-cache`; lỗi nằm ở việc đường khởi động không kích hoạt nó.
- Nguồn mặc định hiện tại và nguồn URL tự thêm đều có thể giữ dữ liệu cũ nếu phiên cache còn hợp lệ. Chỉ nguồn mặc định cũ (legacy) hoặc phiên rỗng mới đi qua đường tải mạng lúc khởi động.

## Hướng sửa an toàn cho phiên bản tiếp theo

- Giữ cơ chế cache-first: hiển thị ngay danh sách đã lưu để ứng dụng mở nhanh.
- Sau khi giao diện cache xuất hiện, nếu nguồn hiện tại là URL HTTP/HTTPS thì tự tải lại ở nền đúng một lần cho lần mở ứng dụng.
- Khi tải thành công, thay danh sách và lưu phiên mới; khi mạng lỗi hoặc playlist trả về rỗng, giữ nguyên danh sách cache đang xem, không xóa kênh và không bật hộp lỗi chặn giao diện.
- Bảo toàn mục đang chọn, tìm kiếm/nhóm và tránh kết quả tải nền ghi đè một lần tải thủ công mới hơn.
- Bổ sung kiểm thử cho: cache URL kích hoạt refresh, nguồn file cục bộ không refresh, tải lỗi vẫn giữ cache, URL có dòng chuyển hướng lấy đúng dòng đầu và tải thủ công vẫn giữ hành vi hiện tại.
- Nếu triển khai, bắt đầu từ 1.10.24 trên `main`, tạo nhánh Mobile riêng và tăng phiên bản mới; không sửa repo NM7 TV.

---

# BÀN GIAO ỔN ĐỊNH — NM7 IPTV MOBILE 1.10.24 (2026-09-13)

## Sửa xóa nguồn đang dùng không quay về nguồn mặc định

- Đã phân tích video máy thật `video_2026-09-13_21-17-06.mp4`: sau khi xóa nguồn tự thêm đang được sử dụng, mục nguồn biến mất khỏi màn hình quản lý nhưng nhóm/kênh của nguồn đó vẫn còn trên màn hình chính; phiên cũ tiếp tục được lưu.
- Nguyên nhân: luồng xóa chỉ so sánh URL nguồn bằng chuỗi tuyệt đối với dòng đầu của `currentSource`. Trạng thái có mô tả/redirect khác có thể làm nhận diện sai. Ngoài ra, app chờ tải nguồn mặc định xong mới thay dữ liệu trong bộ nhớ, nên khi mạng chậm/lỗi giao diện vẫn treo ở playlist đã xóa.
- Khi xóa, app nay đối chiếu nguồn hiện hành với toàn bộ danh sách nguồn còn lại. Nếu nguồn đang dùng không còn, app lập tức chuyển trạng thái về nguồn mặc định.
- Trước khi gọi mạng, app xóa ngay danh sách kênh, nhóm, tìm kiếm, EPG, bộ đếm và phiên nguồn cũ; cập nhật màn hình về NM7 IPTV rồi tải lại nguồn mặc định. Vì vậy nguồn vừa xóa không thể tiếp tục tồn tại do cache hoặc do tải mặc định chậm.
- Nếu xóa một nguồn không phải nguồn đang dùng, app giữ nguyên nguồn hiện tại và mở lại màn hình quản lý như trước.
- Thêm unit test cho bốn trường hợp: nguồn đang dùng vẫn còn, nguồn đang dùng bị xóa, mô tả nguồn có dòng redirect, và nguồn mặc định/nội dung cục bộ không bị chuyển sai.
- Version: `versionCode 41`, `versionName 1.10.24`. Repo TV không bị thay đổi.
- Người dùng đã xác nhận Mobile 1.10.24 ổn định trên máy thật. Đây là bản nền chính thức trên `main`; mọi sửa lỗi/cải tiến Mobile tiếp theo phải bắt đầu từ mốc này và tăng phiên bản mới.
- Commit: phát hiện nguồn còn lại `6de217ae5fe074a1527df2bd908c47a354fbb172`; reset giao diện/phiên `0a568a4ff62a771df430c127dc562d65a2109c43`; test `39801f77d09021bce08826158d66fc13f9847edc`; tăng version `08da0bf16df557a897063702bfaf4a480ed9eeb4`; CI cuối `e4c713c161eb38381d95355f91490031a6b05ca4`.
- Workflow run `34763079646` (#224), job `103739184185`: compile, unit test, lint, đọc version, xác minh chữ ký, kiểm tra không chứa LibVLC, đóng gói và smoke-test Mobile trên Android 15 đều **SUCCESS**.
- Đã fast-forward `main` tới commit ổn định `28060225824eb12604834658d7c8d69bd6105b95`. Build xác nhận trên `main`: run `34764658816` (#225), attempt 2, job `103743881156` — toàn bộ compile, unit test, lint, ký/đóng gói và smoke-test Android 15 **SUCCESS**. Attempt 1 bị JVM runner crash, không phải lỗi mã.
- Artifact xác nhận trên `main`: `NM7-IPTV-Mobile-1.10.24-APK`, ID `10319919407`, archive SHA-256 `4cfe95fcc2eb415707097f9424b7faad309ff621921249550134e208f64bd86d`.
- Artifact `NM7-IPTV-Mobile-1.10.24-APK`, ID `10319532889`, archive SHA-256 `1dfd99bba7d2585a66a0566bb28ec4d9b74f539cd94f2596bea0354ce7a1d750`.
- APK: 7.723.542 bytes; SHA-256 `eaf2ae2ce216038655f3044777dfa00957c006852d177bb2c28336d20d5fe3e3`; `sha256sum -c` đạt.
- Kiểm thử máy thật đã được người dùng xác nhận ổn định. Luồng xóa nguồn đang dùng quay về NM7 IPTV hoạt động đúng; giữ 1.10.24 làm mốc hồi quy cho các bản sau.

---

# BÀN GIAO HIỆN TẠI — NM7 IPTV MOBILE 1.10.23 (2026-09-13)

## Bản Mobile 1.10.23 — duy trì phiên live và loại bỏ nhấp nháy logo

- Phân tích ảnh/video máy thật của 1.10.22 cho thấy watchdog cũ chỉ dựa vào thời gian BUFFERING, reset lịch sử lỗi sau 8 giây phát và gọi prepare/recreate lặp lại; do đó hình thành vòng phát–tải–phát dù chức năng auto-play đã hoạt động.
- Chuyển HTTP Media3 sang OkHttp DataSource dùng connection pool 8 socket, giữ kết nối 5 phút, retryOnConnectionFailure và ping 20 giây. Manifest/segment tái sử dụng socket thay vì liên tục tạo kết nối mới.
- Watchdog theo dõi thêm tiến triển mạng từ bandwidth callback, buffered-ahead, trạng thái isLoading, vị trí phát và frame decoder. Luồng chậm nhưng còn nhận dữ liệu được chờ tối đa 35 giây; socket không tiến triển phục hồi sau 18 giây.
- Phục hồi phân cấp: lần đầu giữ Player/timeline/decoder và prepare lại; các lần sau mới tạo MediaSource/Player mới qua connection pool. Backoff tự động được giới hạn 0,4–7 giây và không dừng chờ người dùng bấm Play.
- Không reset lịch sử lỗi sau một đoạn phát ngắn; chỉ reset sau 2 phút phát liên tục, giúp lỗi CDN lặp lại được nâng cấp sang phương án phục hồi mạnh hơn.
- Bỏ seekToDefaultPosition vô điều kiện. Chỉ nhảy live edge khi luồng kết thúc hoặc live offset vượt 45 giây, giảm phát lặp đoạn cũ. Live target offset tăng từ 6 lên 10 giây.
- Sửa nháy logo: bỏ notifyDataSetChanged sau mỗi ảnh; một URL chỉ có một tác vụ tải và cập nhật đúng các Holder đang chờ. Giữ drawable nếu URL của hàng không đổi.
- Logo có cache RAM LRU 24 MB và cache ổ đĩa trong cache app; giới hạn mỗi ảnh 2 MB, timeout 5/8 giây.
- Version: `versionCode 40`, `versionName 1.10.23`; repo TV không bị thay đổi.
- Commit tính năng: `d48a84cd64d848c5259cc185dbc05f92ef105fb0`; workflow: `b768815250fd0d480a2a73f9f28a0961ae0e6e76`; sửa tương thích Media3: `454735411f92a39a855d2f5f4024b42804a150c4`.
- Build đầu run `34727897253` bị compiler chặn do Media3 1.11 không có getPlaybackError; đã sửa đúng API, không tạo/bàn giao APK từ run lỗi.
- Build bàn giao run `34728059143`, job `103645667798`: compile, unit test, lint, ký/đóng gói và smoke-test Mobile trên Android 15 đều **SUCCESS**.
- Artifact `NM7-IPTV-Mobile-1.10.23-APK`, ID `10309245086`, archive digest `sha256:d1a714f66863d428b547b1931e7c33cd7e2df20d7fb8501f935b86d52952754d`.
- APK: 7.723.539 bytes; SHA-256 `0b3755ce087f3920fadbe7c4489d410e28bce431ab548dca386cc24c3740becb`.
- Cần thử dài hạn trên điện thoại thật với đúng kênh nước ngoài/4K. Không thể bảo đảm nguồn máy chủ thiếu segment vẫn phát liên tục, nhưng app không còn restart chỉ vì BUFFERING còn tiến triển.
- Phản hồi máy thật ngày 2026-09-13: bản 1.10.23 **ổn định hơn**; người dùng đang tiếp tục test dài hạn. Chưa đánh dấu là hết lỗi tuyệt đối.
- Hồ sơ nguyên nhân, kiến trúc sửa, kết quả và checklist áp dụng cho Android TV: `docs/PLAYBACK_STABILITY_1.10.23.md`. Khi xử lý TV phải điều chỉnh theo Player/lifecycle TV, không sao chép nguyên trạng lớp Mobile.

---

# BÀN GIAO HIỆN TẠI — NM7 IPTV MOBILE 1.10.22 (2026-09-13)

## Bản Mobile 1.10.22 — tự nối lại timeout, giảm giật và cache logo

- Đã phân tích video máy thật `216837.mp4`: luồng 4K rơi vào `ERROR_CODE_TIMEOUT`, FPS về 0; bấm Play tạo lại yêu cầu và phát tiếp. Nguyên nhân phía ứng dụng là nhánh lỗi tạm thời dừng hẳn sau khi dùng hết số lần retry.
- `ERROR_CODE_TIMEOUT`, lỗi kết nối/đọc, IO tạm thời và HTTP 401/403/408/429/5xx được coi là lỗi có thể phục hồi. Player tự tạo lại kết nối vô hạn với backoff giới hạn tối đa 7 giây, không còn yêu cầu người dùng bấm Play.
- Tắt spinner buffering mặc định trong khung video và bật `keepContentOnPlayerReset`; khi nối lại/chuyển nguồn, khung hình cuối được giữ thay vì hiện vòng tròn quay trên nền đen.
- Bộ đệm Mobile tăng thành 30–120 giây, bắt đầu ở 1,2 giây, sau rebuffer cần 5 giây và back-buffer 20 giây để ưu tiên ổn định cho kênh nước ngoài/4K.
- Cấu hình live target offset 6 giây và tốc độ bám live 0,97–1,03x, giúp quay về live edge mềm hơn và giảm nguy cơ phát lặp đoạn cũ.
- Logo kênh dùng cache RAM LRU 16 MB, chặn tải trùng cùng URL, tải song song tối đa 4 tác vụ và có timeout 5/8 giây; hàng tái sử dụng lấy bitmap từ cache nên không tải lại mỗi lần vuốt.
- Version: `versionCode 39`, `versionName 1.10.22`; repo TV không bị thay đổi.
- Commit mã: `21fdaf3853e614af9806021a903f2c59b4192f26`; commit workflow: `cdbc47a91bf7726d1f0a8b67f1db740a59f95b71`.
- Workflow run `34726563618`, job `103641603760`: compile, unit test, lint, ký/đóng gói APK và smoke-test Mobile trên Android 15 đều **SUCCESS**.
- Artifact `NM7-IPTV-Mobile-1.10.22-APK`, ID `10307104801`, archive digest `sha256:dbfac5a6b80428bb5f81a5d7e5290a821f97d85f5df344201e6c62ec74b50006`.
- APK: 7.173.560 bytes; SHA-256 `67288e8479e5d594827052aff9afcc33a03108f309228a67b36dd009244c77ad`; kiểm tra ZIP và `sha256sum -c` đều đạt.
- Cần thử dài hạn trên điện thoại thật bằng chính các kênh nước ngoài. CI xác nhận app không crash và luồng mẫu hoạt động nhưng không thể mô phỏng timeout/token/CDN của nguồn riêng.

---

# BÀN GIAO HIỆN TẠI — NM7 IPTV MOBILE 1.10.21 (2026-09-12)

## Bản Mobile 1.10.21 — phát hiện đứng hình thật và chuyển kênh nhanh hơn

- Phân tích video máy thật cho thấy Media3 có thể quay lại READY và vẫn có `playWhenReady=true` nhưng decoder không xuất thêm frame; FPS giữ 0.0 nên watchdog chỉ theo dõi BUFFERING của 1.10.20 không bắt được.
- Watchdog mới theo dõi trực tiếp `DecoderCounters.renderedOutputBufferCount` mỗi 2 giây. Nếu READY/đang phát/không bị suppression nhưng không có frame mới trong 8 giây, ứng dụng tự phục hồi.
- Khi phục hồi luồng live, Player luôn `seekToDefaultPosition()` để trở về live edge trước `prepare/play`, tránh giữ vị trí đã rơi khỏi cửa sổ HLS/DASH.
- Ngưỡng BUFFERING trước khi tự nối lại giảm từ 15 xuống 10 giây.
- Sửa lỗi callback pause bất đồng bộ: cờ lifecycle không bị xóa ngay sau `player.pause()`, tránh nhận nhầm pause do hệ thống thành thao tác Pause của người dùng rồi chặn auto-play.
- Bộ đệm cân bằng lại thành 20–90 giây, bắt đầu phát ở 750 ms và phát lại sau rebuffer ở 2,5 giây để chuyển kênh nhanh hơn nhưng vẫn giữ vùng đệm dài.
- Luồng đổi kênh cập nhật kênh/UI trước khi giải phóng Player cũ; bỏ các lệnh `stop()` và `clearMediaItems()` dư thừa trước `release()`.
- Version: `versionCode 38`, `versionName 1.10.21`.
- Commit sửa Player: `3d1ef78`; phát hành: `8b63538`; workflow: `b4c0c8b`.
- Workflow run `34724994307`, job `103637476574`: compile, unit test, lint, ký/đóng gói APK và smoke-test Mobile trên Android 15 đều **SUCCESS**.
- Artifact `NM7-IPTV-Mobile-1.10.21-APK`, ID `10307701954`, archive digest `sha256:43f34e99c893ac3f8b1fd9e74aa9dcef3f632cca67682c4575602b48578dd652`.
- APK: 7.173.562 bytes; SHA-256 `a5c19e97eaca26a3ca688fa05b3f051eb5527444e6a08ecaac36cd43474d9004`.
- Cần thử dài hạn trên điện thoại thật với chính nhóm kênh nước ngoài. Nếu vẫn đứng, thu `adb logcat` tại thời điểm đó để phân biệt decoder 4K, manifest/server HLS hoặc token/session nguồn.

---

# BÀN GIAO HIỆN TẠI — NM7 IPTV MOBILE 1.10.20 (2026-09-12)

## Bản Mobile 1.10.20 — ổn định phát và nút tải lại nhanh

- Chỉ thay đổi repo Mobile `phuongnm7/iptv-player-android`; repo TV không bị tác động.
- Sửa trường hợp đang xem bị chuyển sang pause ngoài ý muốn: Player phân biệt pause do người dùng với pause do lifecycle/hệ thống và tự tiếp tục khi `playWhenReady` bị mất nhưng không có playback suppression.
- Thêm watchdog kiểm tra mỗi 2 giây. Nếu luồng ở trạng thái buffering quá 15 giây, ứng dụng tự prepare/play hoặc tạo lại Player; tối đa 6 lần với backoff.
- Khi luồng báo ENDED ngoài ý muốn, ứng dụng tự nối lại từ vị trí live mặc định.
- Bộ đệm Mobile tăng từ 12–45 giây lên 25–90 giây; ngưỡng bắt đầu 1,5 giây, sau rebuffer 5 giây; giữ back-buffer 15 giây.
- Timeout kết nối/đọc tăng từ 20/35 giây lên 30/60 giây; số lần tải lại Media3 tăng từ 6 lên 8.
- Nút **↻ Tải lại** được đưa ra thanh chức năng, đặt ngay cạnh **★ Yêu thích** ở cả giao diện dọc và ngang; nút cũ trong bảng nhập nguồn đã được bỏ.
- Version: `versionCode 37`, `versionName 1.10.20`; User-Agent Mobile đồng bộ thành 1.10.20.
- Commit mã chính: `bd55dc5`; giao diện: `0b8d24d`, `54262d9`; phát hành: `3ebaf4f`; workflow: `a397156`.
- Workflow run `34709802587`, job `103596376811`: compile, unit test, lint, kiểm tra/đóng gói APK và smoke-test Mobile trên Android 15 đều **SUCCESS**.
- Artifact `NM7-IPTV-Mobile-1.10.20-APK`, ID `10302258664`, archive digest `sha256:21769088fc2468a4e441e90f0cb9a67144e56c31adc1289bd2f95fba4ce73699`.
- APK: 7.173.555 bytes; SHA-256 `f7f742623b1001fce94eced4ef6fd3626eaac42bcd4ce63b2aaa12402a68c237`.
- Cần kiểm tra trên điện thoại thật với chính các kênh nước ngoài trong thời gian dài. CI không dùng playlist/credential/DRM riêng nên không thể chứng minh chất lượng của máy chủ nguồn.

---

# BÀN GIAO HIỆN TẠI — NM7 IPTV MOBILE 1.10.19 (2026-09-12)

> Đây là mốc phải đọc trước khi tiếp tục. Repo chuẩn: `phuongnm7/iptv-player-android` (private), nhánh `main`.

## Bản Mobile 1.10.19 — nguồn mặc định riêng và khôi phục nguồn

- Chỉ thay đổi repo Mobile `phuongnm7/iptv-player-android`; không đọc, sửa hoặc kích hoạt workflow của repo TV `phuongnm7/nm7-tv-android`.
- Đổi nguồn tích hợp mặc định sang Worker mới do chủ dự án cung cấp.
- Nguồn mặc định không được lưu chung với danh sách nguồn tự thêm và URL không hiển thị trong màn hình quản lý nguồn.
- Màn hình quản lý nguồn luôn có mục **NM7 IPTV** với nút **Chọn nguồn mặc định**; khi đang dùng nguồn này, nút đổi thành **Đang sử dụng nguồn mặc định**.
- Khi xóa nguồn tự thêm đang được chọn, ứng dụng tự quay về nguồn mặc định và thông báo rõ cho người dùng.
- Có migration nhận diện URL mặc định cũ, kể cả phiên được cache trước khi nâng cấp, rồi chuyển sang nguồn mặc định mới.
- Thêm unit test cho URL mặc định mới, nhận diện nguồn mặc định/nguồn cũ và luồng migration.
- Version: `versionCode 36`, `versionName 1.10.19`; User-Agent được đồng bộ thành 1.10.19.
- Workflow chỉ build Mobile. Run `34674734089`, job `103502434843`: compile, unit test, lint, kiểm tra/đóng gói APK, cài và smoke-test Mobile trên Android 15 ở dọc/ngang đều **SUCCESS**.
- Artifact `NM7-IPTV-Mobile-1.10.19-APK`, ID `10291729098`, archive digest `sha256:2fea60f5328309fd8907908feaf644b2eb012723041d43f0fc5d4305a42063ef`.
- Các commit chính: `36653ec`, `f305c11`, `492845c`, `69d64a3`, `6465509`, `6810541`, `fca332a`, `c6ff2af`, `7bea29d`.



## Bản Mobile 1.10.18 — thông tin ứng dụng, vuốt nhóm và ổn định luồng

- Từ mốc này repo này được phát triển và đóng gói **chỉ cho Mobile**; dự án TV riêng nằm tại `phuongnm7/nm7-tv-android`.
- “Thông tin ứng dụng” tự đọc version từ gói cài đặt và hiển thị `Phiên bản: 1.10.18 (Mobile)`.
- Thay mô tả bằng: “Ứng dụng được phát triển bởi Phuongnm7 vì mục đích cá nhân, không vì mục đích thương mại.”
- Player dựng sẵn chỉ mục kênh theo nhóm, không quét toàn playlist mỗi lần đổi nhóm; adapter không sao chép lại danh sách và dùng ID ổn định.
- Thanh nhóm hỗ trợ vuốt ngang đổi nhóm sau khi nhấc tay, cuộn tới nhóm đang chọn; bật hardware layer và tắt overscroll/fading edge để thao tác nhẹ hơn.
- Tăng bộ đệm chống giật lên 15–60 giây, giữ back-buffer 10 giây, bắt đầu phát từ 500 ms, thêm HTTP keep-alive.
- Nếu Player mắc ở trạng thái buffering liên tục 20 giây, cơ chế phục hồi phiên hiện có được kích hoạt thay vì đứng hình vô hạn.
- Đặt `playWhenReady` trước `prepare` để giảm độ trễ bắt đầu phát.
- Version: `versionCode 35`, `versionName 1.10.18`.
- Workflow đã tách sang chỉ chạy `testMobileDebugUnitTest`, `lintMobileDebug`, `assembleMobileDebug`; không build hoặc upload TV.
- Build run `34671826668`, job `103494506653`: compile, unit test, lint, kiểm tra chữ ký, đóng gói APK và smoke-test Mobile trên Android 15 đều **PASS**.
- Artifact `NM7-IPTV-Mobile-1.10.18-APK`, ID `10291245926`, archive digest `sha256:60a03b8179cb74f7e33008155c5a4751a490adbdc7893ce022e98e1da3e53323`.
- Các commit chính: `1bbde5e`, `8877abb`, `cbf5ca0`, `7aac12a`, `39afa7a`, `767ab1e`, `9507505`.

## Trạng thái nguồn và bản bàn giao

- Commit mã Mobile 1.10.18 cuối trước tài liệu: `95075051940e42bced32f92ce4983f860f4cce97`.
- Version hiện tại: `versionCode 35`, `versionName 1.10.18`; trong ứng dụng hiển thị **1.10.18 (Mobile)**.
- Build gần nhất: run `34671826668`, job `103494506653`.
- Compile, unit test, Android lint, kiểm tra chữ ký, đóng gói và upload APK Mobile đều **SUCCESS**.
- Smoke-test cài và mở APK Mobile trên Android 15 cũng **SUCCESS**; toàn workflow kết luận **success**.
- Artifact hiện hành: `NM7-IPTV-Mobile-1.10.18-APK`, ID `10291245926`, archive digest `sha256:60a03b8179cb74f7e33008155c5a4751a490adbdc7893ce022e98e1da3e53323`.
- Không tiếp tục build hoặc phát triển bản TV trong repo này. Mọi nội dung TV chuyển sang repo riêng `phuongnm7/nm7-tv-android`.

## Thay đổi mới nhất cần giữ nguyên

### Logo

- Logo gốc chung/TV: `app/src/main/res/drawable/nm7_main_logo.png`.
  - Nền trong suốt.
  - Giữ biểu tượng màu + chữ **Phuongnm7 TV**.
- Logo ghi đè riêng cho Mobile: `app/src/mobile/res/drawable/nm7_main_logo.png`.
  - Chỉ giữ chữ **Phuongnm7 TV** và hai đường kẻ; không có biểu tượng.
  - Được thu nhỏ còn khoảng 75% so với lần đầu và căn giữa bằng vùng đệm trong suốt.
- Không sửa logo TV khi tinh chỉnh kích thước logo Mobile.

### Player TV

- Đã xóa nút xoay màn hình `↻` khỏi `app/src/tv/res/layout/activity_player.xml`.
- Bản TV không cần chức năng xoay màn hình; bản Mobile vẫn giữ chức năng này.
- `app/src/tv/res/values/ids.xml` khai báo ID `btnRotate` để lớp `PlayerActivity` dùng chung vẫn biên dịch, nhưng layout TV không tạo View nên không có nút/chức năng xoay.
- Commit liên quan:
  - `2428fa7994112e9f81f7f911f9641f3f03ed6eda` — cập nhật logo chung và bỏ điều khiển xoay TV.
  - `fdcd9037e860c81d0a5a10915aecef6148e6ed96` — sửa biên dịch TV sau khi bỏ nút.
  - `3a3a555bec63b7a435f4a00b610a8473242ff962` — logo chữ-only riêng cho Mobile.
  - `767e03bc9197c51b6937f41fa0e8869ee46e6eb3` — thu nhỏ logo Mobile.

## Playlist động và reload

- Ứng dụng 1.10.17 có nút **Tải lại** playlist.
- Khi tải từ `raw.githubusercontent.com`, ứng dụng thêm tham số thời gian và gửi `Cache-Control: no-cache, no-store, max-age=0` cùng `Pragma: no-cache` để tránh dữ liệu cũ.
- User-Agent hiện dùng `Nm7-IPTV/1.10.17 Android` hoặc `Nm7-IPTV/1.10.17 Android-TV`.
- Endpoint M3U động/Vercel và khóa giải mã thuộc dự án/repo private tách riêng; không đưa khóa hoặc secret vào repo Android/tài liệu công khai.

## Việc xử lý tiếp theo

1. Cài APK Mobile Build #190 trên điện thoại thật và xác nhận logo chữ đã đủ nhỏ; nếu chưa, chỉ chỉnh asset trong `app/src/mobile/res/drawable/`.
2. Cài APK TV Build #190 và xác nhận nút xoay không còn trong Player, điều khiển D-pad/OK/LEFT và chuyển kênh vẫn hoạt động.
3. Mở artifact/log của smoke-test run `34489487767` để phân biệt lỗi test/emulator với lỗi ứng dụng; không tắt test để che lỗi.
4. Nếu sửa mã hoặc tài nguyên tiếp, tăng version chỉ khi chuẩn bị một bản phát hành mới theo yêu cầu; build cả hai flavor và ghi lại run/artifact/SHA.
5. Repo phải tiếp tục **private**. Không commit playlist riêng, token, cookie, khóa DRM, khóa giải mã hoặc secret Vercel.

---

# Nhật ký tiếp tục dự án — IPTV Player Android

## Phiên bản 1.7 — Nm7 IPTV và chọn kênh nhanh trên TV

- Đổi tên hiển thị ứng dụng thành `Nm7 IPTV`; giữ nguyên applicationId để tương thích dữ liệu/cài đặt hiện có khi chữ ký cài đặt khớp.
- Màn hình chính thay ô chọn nhóm xổ xuống bằng thanh nút nhóm cuộn ngang. Chọn một nhóm sẽ hiển thị ngay danh sách kênh thuộc nhóm; từng nút có focus D-pad rõ ràng.
- Trên giao diện TV, khi đang xem bấm phím Trái sẽ mở bảng nhóm/kênh nhanh bên trái. Luồng hiện tại tiếp tục phát phía sau; chọn kênh khác mới chuyển nguồn, Back đóng bảng.
- Bảng kênh nhanh đọc playlist đã lưu trong vùng riêng của ứng dụng, giữ URL/header/DRM hợp lệ của từng kênh và đánh dấu kênh đang xem.
- Tăng versionCode 9, versionName 1.7.0; đổi artifact thành `Nm7-IPTV-1.7-APK`. Việc tiếp theo: compiler, JUnit, lint, smoke Android 15/D-pad và bàn giao APK.

## Phiên bản 1.6.1 — sửa focus lựa chọn nhóm trên TV

- Sửa ô chọn nhóm và từng dòng trong danh sách xổ xuống: khi điều hướng bằng D-pad, mục hiện tại có nền xanh sáng, viền xanh lá 3dp và vẫn rõ ở trạng thái nhấn/chọn.
- Áp dụng đồng nhất cho màn hình dọc và ngang; giữ thao tác cảm ứng trên mobile.
- Tăng versionCode 8, versionName 1.6.1. Workflow riêng tư sẽ chạy compiler, JUnit, lint, đóng gói APK và smoke Android 15 trước khi bàn giao.
- Mã bản vá ở commit `be021b23bebe0aad3ebb7baf7750625180a98c9e`. Build run `34292430994`, job `102281669427`: compiler, 23 JUnit tests, lint, xác minh chữ ký và upload APK đều đạt; smoke Android 15 đang chạy độc lập lúc lưu checkpoint.
- Artifact `IPTV-Player-1.6.1-APK`: ID `10081938250`, archive SHA-256 `eb7393b119089cbe3e75ddd89002927c8374c1c06543f8ea65ec2d12c601e354`.
- APK 1.6.1 đã tải và kiểm tra `sha256sum -c` đạt: 6.681.741 bytes, SHA-256 `aaaaf42c1b30634c6572fd92b4d43574a362b8641c64232cfc09832410f1309f`; đã lưu bản bàn giao.

## Phiên bản 1.6 — Mobile/TV, D-pad và nhiều link IPTV

- Thêm lựa chọn giao diện Tự động, Mobile hoặc TV. Chế độ tự động nhận diện `UI_MODE_TYPE_TELEVISION`; lựa chọn được lưu riêng trên thiết bị.
- Thêm launcher `LEANBACK_LAUNCHER`, khai báo TV/touchscreen không bắt buộc và hỗ trợ activity co giãn để APK có thể cài/chạy trên Android TV lẫn điện thoại.
- Nút, tab và thẻ kênh có trạng thái focus viền xanh sáng; danh sách dùng selector riêng và chế độ TV tự đưa focus tới tab Tất cả để điều khiển bằng D-pad.
- Thêm kho tối đa 50 link playlist: đặt tên, lưu, chọn để tải, hoặc xóa. Link được lưu trong vùng app-private; tải thành công tự ghi nguồn nhưng giữ lại tên tùy chỉnh.
- Tăng versionCode 7, versionName 1.6.0. Việc tiếp theo: build/test/lint, smoke điều hướng và bàn giao APK 1.6; repo tiếp tục private.
- Build đầu run `34289998336`, job `102274190988`: compiler và 23 JUnit tests đạt; lint chặn đúng lỗi `MissingTvBanner` vì đã khai báo Leanback launcher. Thêm banner vector 320×180 và build lại, không tắt lint.
- Mã cuối 1.6 ở commit `ace3b68aba1acefeb92a68b97797b0cf9484300a`. Build run `34290442421`, job `102275564081`: compiler, 23 JUnit tests, lint, đóng gói, xác minh chữ ký và upload APK đều đạt; smoke Android 15/D-pad còn chạy độc lập lúc bàn giao.
- Artifact `IPTV-Player-1.6-APK`: ID `10081166815`, archive SHA-256 `27ed2c0f1dcb14c860030b21e241a709f2f971bd309f24b87ac9675b5a03061f`.
- APK 1.6 đã tải, kiểm tra ZIP và `sha256sum -c` đạt: 6.679.803 bytes, SHA-256 `aae483f7e110a3440ad33cabd0cbabbd61b2271d7880ae4a8bb084ee93de74fc`; đã lưu bản bàn giao.

## Phiên bản 1.5 — giao diện kiểu thư viện Super OK

- Người dùng đã xác nhận bản 1.4 phát được đúng nguồn trước đây lỗi DASH/ClearKey.
- Phân tích APK tham chiếu cho thấy bố cục trọng tâm là thư viện kênh, mục Yêu thích/Gần đây, cài đặt tập trung và nhiều điều khiển khi xem. Chỉ tái tạo luồng sử dụng; không sao chép mã, tài nguyên hay thương hiệu của APK.
- Thêm thanh Tất cả/Yêu thích/Gần đây; danh sách kênh dạng thẻ, nút sao và menu nhấn giữ để yêu thích, chọn xuất, xem URL hoặc phát.
- Yêu thích và lịch sử được lưu riêng trên thiết bị bằng mã băm SHA-256 của định danh kênh, không lưu thêm URL/token ở kho tùy chọn.
- Gom tùy chọn hình nền, hiện URL, mật độ hàng, FPS, đồng hồ và xóa lịch sử vào một menu. Trình phát thêm lựa chọn Fit/Zoom/Fill và đồng hồ tùy chọn; giữ nguyên luồng DRM 1.4 đã hoạt động.
- Tăng versionCode 6, versionName 1.5.0. Việc tiếp theo: compile/test/lint, kiểm tra giao diện/smoke và bàn giao APK 1.5; repo tiếp tục private.
- Mã 1.5 đã commit tại `c6baa1dd4d8cb73ee142f276ef0de398018d0248`. Build run `34287894390`, job `102267591874`: compiler, 22 JUnit tests, lint, đóng gói, kiểm tra chữ ký và upload APK đều đạt; smoke Android 15 còn chạy độc lập lúc bàn giao.
- Artifact `IPTV-Player-1.5-APK`: ID `10080288540`, archive SHA-256 `1a91c5050fa21d6d63cd549fc9512bbc0d9d6babdac112da6fb1a44a1e2e4a90`.
- APK 1.5 đã tải, kiểm tra ZIP và `sha256sum -c` đạt: 6.677.638 bytes, SHA-256 `fe28cef7341d82cfde0de8121e2a4e34b7194fbd0eefdf979e8d697f92bce50b`; đã lưu bản bàn giao.

## Tóm tắt bàn giao hiện tại — 2026-09-08 15:12 UTC

- Kho chuẩn: `phuongnm7/iptv-player-android`, nhánh `main`, trạng thái **private** đã xác minh. Không đổi public và không triển khai Play Store/Sites.
- Commit chứa mã ứng dụng 1.4: `e4d6eaf1d7b024e619ccaa16db732bb63b2eddab`. Commit checkpoint trước khi viết mục bàn giao này: `d008f35f66bcc947cb6ceb181cb410c7ca8a08cf`.
- Build bàn giao: run `34242285983`, job `102115239390`. Compiler, 21/21 JUnit tests, lint, đóng gói, xác minh chữ ký và upload APK đã thành công.
- Smoke Android 15 của cùng run vẫn `in_progress` tại thời điểm chốt nhật ký. Không diễn giải build thành công là bằng chứng luồng DRM thật đã phát được trên điện thoại người dùng.
- APK bàn giao: `IPTV-Player-1.4.apk`, 6.658.988 bytes, SHA-256 `6897e461141db5398582d23aebbbf8b2edc926372bc6d5fe0bf5b66e65065939`. Artifact GitHub riêng tư: `IPTV-Player-1.4-APK`, ID `10062651950`, archive SHA-256 `1ca931361d57df280647c48d1c7fcd012bc1c521191975c8338b4685fb2481df`.
- Lỗi máy thật gần nhất của bản 1.3: `ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED` trên kênh DASH/ClearKey. Bản 1.4 đổi cách lấy URL ClearKey động từ Media3 POST mặc định sang GET nền, sau đó chuẩn hóa JSON/JWK hoặc KID:KEY trong bộ nhớ.
- Sửa lỗi này dựa trên metadata playlist và so sánh hành vi APK tham chiếu, nhưng chưa có xác nhận máy thật rằng đúng kênh đã phát. Không trích xuất khóa từ APK, không ghi phản hồi giấy phép vào log hoặc lưu trữ.

### Việc người tiếp nhận nên làm tiếp

1. Cài đúng APK 1.4 lên điện thoại thật và thử lại cùng kênh. Nếu xung đột chữ ký debug, xuất playlist trước, gỡ bản cũ rồi cài lại.
2. Mở run `34242285983`, ghi kết quả cuối của smoke vào file này. Nếu smoke treo, đọc artifact/log emulator trước khi sửa hoặc chạy lại.
3. Nếu DRM vẫn lỗi, lấy `adb logcat` tại lúc mở kênh, tập trung Media3/MediaDrm/HTTP status và exception chain; tuyệt đối không ghi hay commit body phản hồi, token, KID/KEY hoặc cookie.
4. Xác minh endpoint giấy phép do playlist cung cấp trả HTTP 2xx và định dạng nào trong JSON/JWK/KID:KEY; kiểm tra yêu cầu User-Agent/Referer/header hợp lệ. Chỉ hỗ trợ giấy phép người dùng có quyền sử dụng, không vượt DRM.
5. Mọi sửa mã tiếp theo phải tăng `versionCode`/`versionName`, chạy lại `testDebugUnitTest`, `lintDebug`, `assembleDebug`, xác minh chữ ký, tải artifact và kiểm tra `sha256sum -c` trước khi bàn giao APK mới.

## Phiên bản 1.4 — sửa lấy giấy phép ClearKey từ URL động

- Ảnh máy thật của bản 1.3 báo `ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED`; nhận diện DASH đã hoạt động nhưng giấy phép chưa lấy được.
- Đã phân tích APK tham chiếu do người dùng cung cấp (SHA-256 `701a66c7ff6e901313bf6c1260003f7ffe3ddb2fe05028610c2b82e33f70398d`): app này có trình IPTV tùy biến, bộ phát hiện luồng và các thư viện phát bổ sung. Không sao chép mã hay trích xuất khóa từ APK.
- Nguyên nhân trong app: Media3 mặc định gửi challenge bằng POST tới mọi URL giấy phép, trong khi playlist này khai báo ClearKey qua endpoint HTTP động trả KID/KEY bằng GET.
- Bản sửa gọi GET ở luồng nền, giới hạn phản hồi 64 KiB, chỉ chuyển tiếp User-Agent/Referer/Origin/Cookie khi máy chủ luồng và giấy phép cùng host, rồi chuẩn hóa JSON/JWK hoặc KID:KEY cho Media3.
- Dữ liệu ClearKey chỉ giữ trong bộ nhớ của màn hình phát, không ghi log, không lưu vào playlist hay trạng thái ứng dụng; không tự tìm hoặc vượt DRM.
- Thêm kiểm thử URL ClearKey từ xa và JSON GET; tăng versionCode 5, versionName 1.4.0. Việc tiếp theo: build/test/lint, tải và bàn giao APK 1.4; repo tiếp tục để private.
- Build đầu của 1.4: run 34241384570, job 102112137194. Java app đã biên dịch; 20/21 test đạt. Test JSON ClearKey thất bại vì `android.util.Base64` là stub trong JVM test. Đang thay bằng `java.util.Base64` (core-library desugaring đã bật) để cùng mã chạy được trên Android 6+ và trong kiểm thử, không bỏ test.
- Build thứ hai: run 34241952754, job 102114101849. Java app tiếp tục biên dịch nhưng cùng test bị chặn bởi `org.json` stub của Android SDK trong JVM. Bổ sung `org.json` chỉ ở `testImplementation`; không đưa thêm thư viện này vào APK runtime.
- Build bàn giao dùng commit `e4d6eaf1d7b024e619ccaa16db732bb63b2eddab`: run 34242285983, job 102115239390. Compiler, 21 JUnit tests, lint, đóng gói, kiểm tra chữ ký và upload APK đều đạt; smoke Android 15 tiếp tục chạy độc lập.
- Artifact `IPTV-Player-1.4-APK`: ID 10062651950, archive SHA-256 `1ca931361d57df280647c48d1c7fcd012bc1c521191975c8338b4685fb2481df`.
- APK 1.4 đã tải, kiểm tra ZIP và `sha256sum -c` đạt: 6.658.988 bytes, SHA-256 `6897e461141db5398582d23aebbbf8b2edc926372bc6d5fe0bf5b66e65065939`; đã lưu bản bàn giao.

## Phiên bản 1.3 — sửa URL `.php` báo container không hỗ trợ

- Ảnh máy thật báo `ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED` ở URL `tv360.php?...`.
- Nguyên nhân trong app: Media3 không thể suy ra HLS/DASH từ đuôi `.php`; PlayerActivity chưa dùng `#KODIPROP:inputstream.adaptive.manifest_type` mà parser đã giữ lại.
- Thêm `StreamSpec`: ưu tiên chỉ dẫn `manifest_type=hls/mpd/dash/ism`, sau đó mới suy từ đuôi hoặc query URL.
- Thêm kiểm thử cho URL PHP không đuôi có manifest DASH/HLS; tăng versionCode 4, versionName 1.3.0.
- Việc tiếp theo: build/test/lint, chạy smoke và bàn giao APK 1.3; repo tiếp tục để private.
- Build cuối dùng commit `aa17f50c4cd948cfb539c3bff0c6081240e54753`: run 34238328861, job 102102081333. Compile, 20 JUnit tests, lint, đóng gói, chữ ký và upload APK đã đạt.
- Artifact `IPTV-Player-1.3-APK`: ID 10061032953, archive SHA-256 `7bed40d9dcb6431262d7b961d9f07cb811179b31b3aa039701a86d1768d40fe7`.
- APK đã tải và `sha256sum -c` đạt: 6.658.989 bytes, SHA-256 `99bef991708137b3fdb527a7797de2f2a354192d23d34c4900a236e24ecd0e01`; đã lưu bản bàn giao. Smoke Android 15 vẫn chạy độc lập.

## Phiên bản 1.2 đang thực hiện — 2026-09-08

- Yêu cầu mới từ ảnh điện thoại: bỏ việc chặn chung mọi kênh DRM; thêm xoay khi xem, hình nền tùy chọn, FPS thực tế và làm danh sách kênh nổi bật ở màn hình chính.
- Đã xác minh repo vẫn private; HEAD trước khi sửa: e6054675dcc08f165d90a3f1411b7ad10dcdd8d7, tree dab57f21659b56a73424a6af7316669abab7f60a.
- Đã khôi phục đủ 35 file từ GitHub vì thư mục làm việc tạm không còn source; không phụ thuộc bản APK/zip cũ.
- Thêm DrmSpec/DrmPlayback: Widevine, ClearKey và PlayReady bằng Media3/MediaDrm; đọc KODIPROP license_type/license_key và header URL-encoded. Không tự tìm hay vượt DRM; thiếu giấy phép thì báo phần thiếu và cho nhập cấu hình hợp lệ.
- Thêm FpsMeter: lấy chênh lệch renderedOutputBufferCount theo thời gian thực, cập nhật mỗi 2 giây; không hiển thị FPS khai báo hay tần số quét màn hình.
- Thêm nút xoay tự động/ngang/dọc trong PlayerActivity; bỏ ép sensorLandscape trong manifest.
- Thêm WallpaperStore: 3 nền màu và ảnh do người dùng chọn, giới hạn 32 MB/1600 px, sửa EXIF, lưu JPEG trong vùng app-private.
- Viết lại màn hình chính một cột thích ứng dọc/ngang: danh sách kênh luôn chiếm vùng co giãn; bảng nguồn tự thu gọn sau khi tải; bộ lọc và nút xuất luôn thấy.
- Tăng versionCode 3, versionName 1.2.0; artifact đổi thành IPTV-Player-1.2-APK.
- Kiểm tra cục bộ hiện tại: PASS 36 core assertions; 15 XML hợp lệ; tổng 18 JUnit tests trong source. Chưa có kết quả Android compiler/build 1.2 tại mốc này.
- Việc tiếp theo: commit bản 1.2, chạy testDebugUnitTest/lintDebug/assembleDebug; sửa lỗi compile thực; tải và lưu APK mới; cập nhật run/SHA tại đây.
- Build run 34233343918 cho commit f4cfb942cdfc3d26841eea01fcf3b162a65bf4bc đã qua compile/test/lint, ký và upload APK 1.2. Artifact ID 10058977161; archive SHA-256 0950e8786909b2ec8bef0cb632168bc68b4ff67995ee0039637bd49813b3c7d9.
- APK sơ bộ 1.2 đã tải, `sha256sum -c` đạt; kích thước 6.658.987 bytes và đã lưu. Cần build lại một lần sau sửa nhỏ: nút DRM phải hiện trước khi báo thiếu license; không lưu license người dùng nhập trong instance state.
- Bản sửa cuối đã commit tại `7f805c28e3badced2e9b1334f405c4ff285b98d2` (tree `35549be7714211ef5885ce270f9ef228856294ff`).
- Build cuối: run 34234206211, job 102088004063. Compile, 18 JUnit tests, lint, đóng gói, kiểm tra chữ ký và upload APK đều đạt; smoke Android 15 đang chạy tại thời điểm ghi mốc này.
- Artifact cuối `IPTV-Player-1.2-APK`: ID 10059316148, archive SHA-256 `fb1f5bd3a143cff7aea71ac86aa2694d352cf63f23814b77e767741095bcb11d`, hết hạn 2026-10-08.
- APK cuối đã tải, kiểm tra ZIP và `sha256sum -c` đạt: 6.658.984 bytes, SHA-256 `c4dc7199f474498717b1bb6895ae202807994c9ed962de2aa140f0eb906a34f8`.

Cập nhật: 2026-09-08. Lưu file này trong GitHub riêng tư để nối lại dù phiên trò chuyện hoặc thư mục tạm mất.

**Trạng thái bàn giao:** Đã tạo, tải về, kiểm tra checksum và lưu APK 1.1 từ Build 4. Compiler/test/lint, chữ ký và upload APK đều đạt. Theo yêu cầu làm nhanh, bàn giao APK trước khi smoke mở rộng chạy hết. Không coi toàn bộ workflow hoặc máy thật là đã kiểm tra thành công; mở run Build 4 bên dưới để lấy kết quả mới nhất.

## Mục tiêu và quyền đã được xác nhận

- Tạo APK Android cài được: nhập URL/tệp M3U, tìm/lọc, phát IPTV, chọn và xuất kênh; hỗ trợ Full HD/QHD/4K nếu codec/thiết bị/nguồn cho phép.
- Người dùng cho phép đưa source và workflow lên đúng kho **phuongnm7/iptv-player-android** và chạy GitHub Actions.
- Kho đã xác minh **private**. Không đổi sang public, không dùng kho playlist Iptv khác, không phát hành Pages/Play Store.
- Không có playlist thật, thông tin đăng nhập hay khóa DRM của người dùng trong mã nguồn.
- Người dùng yêu cầu lưu quá trình để tiếp tục sau này.

## Nguồn chuẩn và điểm kiểm tra

- Repo: https://github.com/phuongnm7/iptv-player-android
- Nhánh: main.
- Commit đầu của app: 60c57efd6abfe06767965f8e06abbf18361d7281
- Tree app đầu: c92f2e826c9d6882c078d2323dafbc43d64aea61
- Commit khởi tạo có README của người dùng: 2d342528e154bba7eb26dbe02b04d4a4ab1f57dd.
- Bản app: 1.1.0, applicationId vn.phuong.iptvplayer, min SDK 23, compile/target SDK 36.
- JDK 17, Gradle 8.13, AGP 8.13.2, Media3 1.11.0.
- Source đã đưa lên gồm 33 file (trước khi thêm nhật ký này).
- Thư mục làm việc tạm: /workspace/scratch/fd876c6053ed/IptvPlayer. Đừng phụ thuộc thư mục này; lấy lại source từ repo nếu mất.

## Những gì đã thực hiện

- Sửa parser: không tách segment HLS thành kênh; BOM/CRLF/URL tương đối; bảo toàn case của token/path.
- Phân biệt URL trùng có header/tùy chọn khác; xuất giữ các header và metadata kênh, nhận biết DRM để báo giới hạn.
- Thêm nhập luồng trực tiếp, URL đầy đủ qua nhấn giữ, chọn theo bộ lọc, không ghi đè playlist khi import rỗng/lỗi.
- Lưu playlist/chọn kênh trong AtomicFile riêng của ứng dụng; loại khỏi backup và device transfer.
- Giao diện ngang/dọc, giữ trạng thái khi đổi hướng; trình phát có retry, ép định dạng, giới hạn chất lượng và độ phân giải thực.
- Media3: HLS/DASH/SS/HTTP/RTSP TCP, module RTMP, DefaultDataSource và multicast lock cho UDP MPEG-TS.
- Giao thức không hỗ trợ/DRM được thông báo; không cam kết mọi giao thức.
- Thêm workflow riêng tư, pin actions theo SHA; quyền contents:read; không publish build scan.

## Kết quả kiểm tra đã có

- CoreCheck chạy bằng Java thật: ban đầu PASS 24 assertions; bản sửa khóa nhận dạng mới PASS 28 assertions.
- Build 2 có 10/10 test JUnit đạt, 0 failed/ignored (đã đọc HTML report). Build 4 có 14 test trong source và tác vụ testDebugUnitTest đã đạt; report chi tiết sẽ upload sau bước emulator.
- 15 XML parse hợp lệ, ID giao diện ngang/dọc khớp.
- Workflow YAML parse hợp lệ.
- Đã đọc XML lint Build 2: 0 errors, 24 warnings không chặn (style, autofill, target/version và bố cục).
- Có APK debug-signed ở Build 2; chữ ký APK v1/v2 xác minh thành công.
- Build 3 đã đạt 8 kiểm tra UI trên Android 15 emulator; đã xem ảnh dọc/ngang và ảnh video MP4 320×180. Smoke chưa chạy hết; điện thoại thật chưa thử.

## Lịch sử build

### Build 1 — thất bại ở môi trường SDK

- Run: https://github.com/phuongnm7/iptv-player-android/actions/runs/34204423381
- Run ID: 34204423381, job ID: 101990471708.
- Commit: 60c57efd6abfe06767965f8e06abbf18361d7281.
- Java 17 và Gradle 8.13 đã cài thành công.
- Bước Install Android SDK packages thất bại: sdkmanager: command not found, exit 127.
- Chưa chạy compiler/test/lint; không phải lỗi mã Java đã xác định.
- Cách sửa đang thực hiện: thêm bước cài Android command-line tools trước khi gọi sdkmanager; không dựa vào image runner đã có SDK.

### Build 2 — thành công, đã có APK

- Đã xác minh lại kho private và HEAD vẫn là commit app đầu, không có thay đổi của người dùng bị ghi đè.
- Thêm android-actions/setup-android v3, pin commit 9fc6c4e9069bf8d3d10b2204b1fb8f6ef7065407.
- Cài command-line tools 12266719 và platform-tools, thiết lập PATH/ANDROID_HOME trước khi cài API 36 và build-tools 36.0.0.
- Commit: 8495f2cde2463813dcb8b4ac4bc50764e097dc67.
- Tree: d71176a74d81352c913f45882e3a4e4f1d387c6f.
- Run: https://github.com/phuongnm7/iptv-player-android/actions/runs/34205233861 (job 101993064010).
- SDK setup, testDebugUnitTest, lintDebug, assembleDebug, kiểm tra chữ ký và upload artifact đều thành công.
- BUILD SUCCESSFUL in 3m 16s. APK có chữ ký v1/v2 hợp lệ (Android Debug).
- Artifact IPTV-Player-1.1-APK: ID 10047594665, hết hạn 2026-10-08.
- SHA-256 APK Build 2: 081ea974eab47f5b649db8f8951ff5ff7318f191d1f311203cb278ffa3fdffe1.
- Android-test-reports: ID 10047595347, hết hạn 2026-09-22.
- Còn cảnh báo compiler về annotation Scope.LIBRARY_GROUP và setup-android Node20 bị runner chuyển Node24; không có lỗi build/lint chặn.

### Build 3 — APK đạt, smoke dừng vì hộp hướng dẫn hệ thống

- Script tools/android_smoke.py dùng adb và máy ảo Android 15; fixtures playlist và video MP4/HLS/DASH 320×180 được tạo cục bộ.
- Kiểm tra import, trùng/thiếu URL, tìm/lọc/chọn, URL đầy đủ, ngang/dọc, import lỗi không làm mất dữ liệu, khôi phục sau process restart và giao thức không hỗ trợ.
- Phát mẫu qua localhost được adb reverse; không dùng nội dung/credential của bên thứ ba.
- Workflow đã chuẩn bị thêm action emulator pin commit a421e43855164a8197daf9d8d40fe71c6996bb0d và artifact bằng chứng riêng tư.
- APK được upload trước smoke test nên phải phân biệt lỗi smoke với lỗi tạo APK.
- Commit: 85bb17ba6734daac901d9a0ad75fbcc879fe4d22; tree: 2abf5d328a5799d736922e26cdb1a3d3f3b4d2ab.
- Run: https://github.com/phuongnm7/iptv-player-android/actions/runs/34206001050 (job 101995501939).
- Các bước compile/test/lint, chữ ký và upload APK đã đạt. Artifact APK ID 10047824932, hết hạn 2026-10-08.
- Smoke đã đạt 8 checks: khởi động, URL/relative/duplicate/missing, tìm/chọn, lọc nhóm, URL đầy đủ, ngang/dọc, không mất playlist khi nhập HTML lỗi, khôi phục sau process restart.
- Smoke dừng ở HTTP-MP4: wait_for không nhìn thấy status vì Android phủ màn hình "Viewing full screen / Got it". failure.xml xác nhận android:id/immersive_cling_title và android:id/ok.
- Đã nhìn failure.png: video màu tổng hợp đã xuất hiện, status 320×180 và timeline 00:20/00:20. Không diễn giải đây là lỗi decoder; log emulator có cảnh báo libcuda nhưng không ngăn khung hình này.
- Artifact smoke ID 10048042342, reports ID 10048043186; hết hạn 2026-09-22. Bằng chứng tạm tại /workspace/scratch/fd876c6053ed/build3-smoke.

### Build 4 — bản giao, khóa trùng an toàn và bảo toàn tùy chọn

- Header khác chữ hoa/thường phải được coi là cùng tên (Cookie/cookie); giá trị header vẫn phân biệt case.
- Thay khóa Map/List.toString bằng các trường có độ dài rõ ràng, tránh loại nhầm kênh khi giá trị có dấu phẩy/dấu bằng/ký tự phân cách.
- Giữ tùy chọn EXTVLCOPT không nhận diện (ví dụ network-caching) khi xuất; không tuyên bố Media3 áp dụng các tùy chọn này.
- Thêm 4 test JUnit và 4 CoreCheck hồi quy; CoreCheck đã đạt 28/28, bước compile/test/lint của Build 4 đã đạt.
- Sửa smoke để nhấn nút Got it của hướng dẫn toàn màn hình khi nó hiện; thêm kiểm tra tạo M3U qua Android file picker và mở lại tệp.
- Commit mã của APK: d2a21c03077ed9b563d1a1d388e7832d6c89cb51; tree: b04d8b4c8e18540ca775d1053c091737f344d8fb.
- Run: https://github.com/phuongnm7/iptv-player-android/actions/runs/34207283159 (job 101999650047).
- Đã kiểm tra trạng thái bước: Compile, test and lint = success; Verify and package APK = success; Upload installable APK = success.
- Khi bàn giao, Check app on Android 15 emulator còn in_progress. Chưa xác nhận đầy đủ HLS/DASH, file picker và tổng số smoke checks.
- APK artifact: ID 10048330039, hết hạn 2026-10-08. Archive SHA-256: 0312fa353c5e029500cce62962603c44706a567e4d8f62af47cd68f92b57b34c (tải về khớp digest GitHub).
- File bàn giao: IPTV-Player-1.1.apk, 6.641.439 bytes; đã lưu cho người dùng, không chỉ giữ trong artifact có hạn.
- SHA-256 APK: f98188e0f2a05405d6f2236d271a542255e5412c18ebd4f10d46a05c7c958032. sha256sum -c SHA256SUMS.txt = OK.
- Bản cài đặt là debug-signed cá nhân, chưa phát hành Play Store. Kho vẫn private khi kiểm tra lúc bàn giao.
- Đường dẫn làm việc lúc giao: /workspace/scratch/fd876c6053ed/deliverables/IPTV-Player-1.1.apk. Nếu scratch mất, tìm bản đã lưu theo tên/SHA hoặc tải lại artifact của đúng run.

## Làm tiếp

1. Xác minh repo còn private và HEAD hiện tại để không ghi đè thay đổi của người dùng.
2. Mở run Build 4 (34207283159), lấy trạng thái cuối và artifact Android-emulator-smoke/Android-test-reports. Không chạy lại build trước khi đọc kết quả hiện có.
3. Đọc result.json, log và ảnh thực. Cập nhật file nhật ký này; sửa nếu có lỗi thật. Không tắt test để che lỗi, không coi hộp hướng dẫn hệ thống là lỗi decoder.
4. APK đã giao là bản từ commit d2a21c0; nếu sửa mã ứng dụng, tăng versionCode và build/kiểm tra lại trước khi thay bản giao.
5. Kiểm thử trên điện thoại thật bằng playlist được người dùng cấp; xác minh riêng Full HD/1440p/4K và RTSP/RTMP/UDP. Không lấy playlist lạ thay cho nguồn của người dùng.
6. Nếu muốn cập nhật lâu dài không phải gỡ app, thống nhất khóa ký ổn định và lưu bằng secret, không commit khóa hoặc mật khẩu.

## Ghi chú quan trọng

- Máy làm việc hiện tại tải SDK trực tiếp bị chặn quyền mạng; user đã chấp thuận dùng GitHub Actions thay thế. Không tiếp tục tìm cách vượt chặn.
- Có thể đọc/chỉnh repo qua GitHub connector. Git data API tạo tree → commit → update_ref (force=false); luôn dựa trên tree/HEAD hiện có.
- Artifact APK sẽ là debug-signed để cài thử cá nhân, chưa phải release Google Play.
- Các runner khác nhau có thể sinh debug keystore khác; trước khi gỡ bản cũ để cài bản có chữ ký khác, phải xuất playlist.
- EPG toàn playlist, Xtream login, SRT/RTP URL/RTMPS, DRM và TV launcher chưa hỗ trợ.
- Không coi việc biên dịch thành công là chứng minh phát 4K/mọi luồng. Không lưu secret trong nhật ký hoặc source.

---

# BÀN GIAO TIẾN ĐỘ — NM7 IPTV MOBILE 1.10.26 — 2026-09-18

## Phạm vi hiện tại

- Dự án đang tiếp tục đúng phạm vi **NM7 IPTV Mobile 1.10.26** trên nhánh `fix/mobile-1.10.26-sleep-timer-icon` của repo riêng tư `phuongnm7/iptv-player-android`.
- **Không build/không bàn giao Android TV** trong mốc này.
- Mục tiêu APK cuối: đúng **2 APK ARM**, gồm `arm64-v8a` và `armeabi-v7a`; không tạo x86/x86_64/universal và không chứa `libvlc.so`.
- Phiên bản: `versionCode 44`, `versionName 1.10.26`.

## SmartTube — tích hợp YouTube mobile

- SmartTube đã được tích hợp vào NM7 IPTV để có tab YouTube bên cạnh IPTV.
- Nguồn SmartTube được lấy từ fork **Systematiq phone/touch**, sau đó merge upstream SmartTube stable **32.47s**, commit upstream `51e87ab4ad57ae672fc24bd216427d3e81bbddb7`, phát hành ngày 2026-09-13.
- Giữ giao diện phone/touch của fork thay vì chuyển dự án sang UI TV.
- Đã xử lý xung đột tài nguyên Media3/ExoPlayer cũ: tách `exo_player_view.xml` thành `st_exo_player_view.xml`, tách `exo_simple_player_view.xml` tương ứng và đổi `PlayerView.java` sang layout riêng. Đây là bản sửa để tránh `ClassCastException` giữa `androidx.media3.ui.AspectRatioFrameLayout` và `com.google.android.exoplayer2.ui.AspectRatioFrameLayout` khi mở YouTube.
- Đã đổi thuộc tính SmartTube `show_buffering` thành `st_show_buffering` để không va chạm với Media3 PlayerView của IPTV.
- Đã sửa API OkHttp trong `DohProviders.kt` sang parser tương thích với OkHttp hiện dùng.
- Đã xử lý placeholder string, namespace/compileSdk/flavor và tương thích AGP 8.13/Kotlin cần thiết cho SmartTube.
- Đã loại bỏ thành phần TV/VLC không dùng cho Mobile, trong đó có `VlcFallbackActivity.java`; không đưa lại vào bản Mobile.

## Luồng IPTV ↔ YouTube và lifecycle

- Tab bar Mobile dùng `REORDER_TO_FRONT` và tắt animation để tránh hiện tượng chuyển Activity chồng hình.
- Khi chỉ **đổi tab**, IPTV player được giữ lại thay vì bị giải phóng; cơ chế đánh dấu `nm7.tab.switch.until` và xử lý lifecycle được thêm để phân biệt tab switch với việc thực sự rời player.
- Khi bắt đầu phát video YouTube thực tế, IPTV được pause.
- Khi bắt đầu phát IPTV thực tế, media bên ngoài được pause; không pause chỉ vì đổi tab.
- Khi PlaybackActivity YouTube kết thúc bằng Back, luồng được đưa về BrowseActivity thay vì thoát khỏi ứng dụng; chuyển Activity không animation.
- Đã thêm cơ chế giữ BrowseActivity và PlayerActivity để quay lại đúng màn hình thay vì tạo lại Activity không cần thiết.

## YouTube UI và font

- Mobile ép BrowseActivity portrait và đã có lớp runtime cố gắng ép các grid/recycler về **một cột**, tránh giao diện 2 cột kiểu TV.
- SmartTube Activity được áp dụng font Android `sans-serif` cho TextView có chữ tiếng Việt; WebView dùng `standard/sans-serif font family` và UTF-8.
- Lưu ý: các mục trên vẫn phải **kiểm thử trên máy thật**. Trước khi người dùng test bản 1.10.26, chưa đánh dấu các lỗi UI/font là đã hết hoàn toàn.
- Đã ghi nhận video test máy thật cho hiện tượng chuyển tab: khoảng 9,6 giây có YouTube ở bên trái và IPTV ở bên phải; khoảng 11,3 giây xuất hiện overlap theo chiều ngược lại. Đây được xem là dấu hiệu cần xử lý lifecycle/transition của Activity, không phải bằng chứng ứng dụng chạy split-screen thật.
- Người dùng trước đó vẫn quan sát thấy: YouTube đôi lúc thoát khi chuyển từ IPTV; chữ tiếng Việt chưa đúng; UI còn giống TV; YouTube tải chậm; Back từ video có thể thoát video/app. Các sửa trên được đưa vào bản test 1.10.26 để xác minh lại.

## Tối ưu khởi động YouTube

- Đã cấu hình SmartTube runtime dùng connection keep-alive và connection pool; bật các cấu hình mạng liên quan để giảm chi phí tạo kết nối lặp.
- Đã thêm Wi-Fi high-performance lock trong vòng đời Activity của Mobile khi app đang hoạt động; mục tiêu là giữ kết nối ổn định khi chuyển giữa IPTV và YouTube.
- Đây là tối ưu runtime, **không coi là chứng minh YouTube đã tải nhanh trên mọi thiết bị** cho đến khi test máy thật hoàn tất.

## Build system — Windows

- Do GitHub Actions gặp giới hạn/bị chặn ở một số lần chạy, quá trình build Mobile 1.10.26 được chuyển sang build trực tiếp trên Windows.
- Môi trường đã xác nhận: JDK 17, Gradle 8.13, Android SDK 36, Build Tools 36.0.0.
- Script build: `scripts/build-mobile-windows.ps1`.
- Script đã được sửa nhiều vòng để xử lý đường dẫn repo, kiểm tra Java trên PowerShell, tham số `$args`, SmartTube shallow clone/merge tag, Git identity cục bộ, lỗi WebSettings và kiểm tra ABI đầu ra.
- SmartTube shallow clone đã được xử lý bằng fetch/unshallow trước khi merge upstream tag 32.47s.
- Đã sửa lỗi compile do gọi API không tồn tại `WebSettings.setDefaultFontFamily`; bản đúng giữ `setStandardFontFamily("sans-serif")`, `setSansSerifFontFamily("sans-serif")` và UTF-8.

## ABI — lỗi đã xác định và sửa

- Một lần build Windows bị chặn bởi cấu hình đồng thời `ndk.abiFilters` và ABI splits, với lỗi: `Conflicting configuration: 'armeabi-v7a,arm64-v8a' in ndk abiFilters cannot be present when splits abi filters are set`.
- Nguyên nhân: dự án đã dùng `splits { abi { include("armeabi-v7a", "arm64-v8a") } }`, vì vậy không thêm `ndk.abiFilters` thứ hai.
- Đã loại bỏ cấu hình `ndk abiFilters` xung đột trong commit `5267b77ebec0229e33471713f61c5b72f0e0560e`.
- Script Windows đã được sửa/khôi phục phần tail kiểm tra APK ở commit `64722c096b283e3dd6cee6525409b75c28a53e65`.
- Các commit sửa script trước đó gồm: `4941c706ed935ef851d4914f8c41816da7457ed4`, `b02c3712122b820681266eea1132ac6f3c8105cb`, `5adfb968a800f5cca207a41ad35f1784f61c8135`, `7cb224d10cac64abb1cf74bf50c298fd6e7bd21a`, `d10c8688622bbc0dedc08e3402841a70252a6d85`, `a749338ff70110835b0860901cf0aa091ce86364`, `eabc49afaf0e1bfc2512a16941a449858bf6911b`, `d593af8c6b7372641b033ad500678bec4872a596`.

## Build Windows — KẾT QUẢ ĐÃ ĐẠT

- Ngày 2026-09-18, build script đã chạy đến cuối và báo:
  `=== BUILD SUCCESSFUL ===`
- Script xác minh đúng **2 APK** và kiểm tra không có x86/x86_64/libvlc; kiểm tra ABI native entries và merged manifest Mobile cũng nằm trong bước xác minh.
- APK đầu ra:
  - `NM7-IPTV-Mobile-1.10.26-arm64-v8a.apk` — **61,892,357 bytes**.
  - `NM7-IPTV-Mobile-1.10.26-armeabi-v7a.apk` — **51,398,095 bytes**.
- Thư mục bàn giao trên Windows:
  `C:\\Users\\Administrator\\Documents\\Codex\\2026-09-08\\hay\\work\\iptv-player-android\\dist\\mobile`.
- Đây là **bản build để test máy thật**, chưa phải xác nhận hết lỗi runtime.

## CI / GitHub Actions

- GitHub Actions đã từng có các lần build Mobile thành công ở các mốc trước, nhưng nhánh 1.10.26 gặp thêm giới hạn workflow/token và giới hạn ngân sách, nên build Windows được dùng để tiếp tục xác minh APK.
- Một lần chạy workflow final gần đây: run `35294946500`, workflow `android-mobile-final.yml`, run #83, kết thúc failure trước khi có job runner; một workflow compatibility khác bị GitHub App token từ chối do thiếu quyền `workflows`.
- Không dùng trạng thái CI lỗi này để kết luận source hiện tại không build được; build Windows ngày 2026-09-18 đã xác minh ngược lại rằng Mobile 1.10.26 hiện có thể compile/package thành công.

## Trạng thái hiện tại — CHỜ TEST MÁY THẬT

**Đã đạt:**
- SmartTube 32.47s tích hợp vào Mobile.
- Media3/ExoPlayer resource collision đã xử lý.
- OkHttp/AGP/Kotlin/namespace/compile compatibility đã xử lý.
- TV/VLC component không nằm trong mục tiêu Mobile.
- Lifecycle IPTV ↔ YouTube đã có cơ chế giữ player khi đổi tab và pause khi bắt đầu phát nguồn kia.
- YouTube portrait/phone UI và các lớp ép một cột/font tiếng Việt đã được đưa vào bản test.
- Windows build thành công.
- Đúng 2 APK ARM: arm64-v8a + armeabi-v7a.

**Chưa xác nhận trên máy thật:**
1. IPTV → YouTube → quay lại IPTV khi **chưa phát YouTube** có giữ nguyên player hay không.
2. IPTV đang phát → phát video YouTube có pause IPTV đúng hay không.
3. YouTube đang phát → phát IPTV có pause YouTube đúng hay không.
4. Đổi tab đơn thuần có còn làm Activity chồng/nhấp nháy/thoát hay không.
5. Giao diện YouTube có thực sự thành một cột mobile hay vẫn còn 2 cột ở một số màn hình.
6. Chữ tiếng Việt trong đăng nhập/cài đặt có hiển thị đúng glyph/font hay không.
7. Thời gian khởi động YouTube có cải thiện đủ trên thiết bị thật hay không.
8. Bấm Back từ video YouTube có quay về Browse/YouTube đúng hay thoát ứng dụng.
9. Phát IPTV dài và chuyển qua lại nhiều lần có còn lỗi lifecycle hoặc mất player hay không.

## Quy tắc cho vòng test tiếp theo

- Không sửa code chỉ dựa trên suy đoán trước khi có lỗi tái hiện/log/ảnh/video từ bản 1.10.26.
- Nếu có lỗi, ghi lại **bước tái hiện → kết quả thực tế → log/ảnh/video → nguyên nhân → sửa → build lại**.
- Mọi sửa Mobile tiếp theo phải tăng versionCode/versionName trước khi tạo APK bàn giao mới.
- Không build TV trong vòng test này.
- Không đưa token, cookie, khóa DRM hoặc thông tin đăng nhập vào log/commit.

## Mốc tiếp theo

Người dùng đang cài và kiểm thử hai APK 1.10.26 trên thiết bị Android thật. Kết quả test thực tế sẽ là đầu vào tiếp theo để quyết định sửa lỗi nào; **không đánh dấu 1.10.26 là ổn định hoàn toàn cho đến khi vòng test này hoàn tất**.

Cập nhật: **2026-09-18 09:14 +07:00**.

---

# TEST MÁY THẬT — LỖI CÒN LẠI VÀ VÒNG SỬA TIẾP THEO — 2026-09-18

## Kết quả test Mobile 1.10.26 từ người dùng

Ảnh/video máy thật xác nhận các lỗi còn tái hiện:

1. IPTV → YouTube → quay lại IPTV: inline player vẫn bị mất/thoát.
2. YouTube mở video: thời gian tải video còn chậm.
3. Giao diện YouTube vẫn mang dấu hiệu SmartTube/TV, chưa khớp hoàn toàn giao diện mobile tham khảo.
4. Đang phát video YouTube → nhấn Home: player bị đóng; chưa giữ được player để quay lại.
5. Giao diện IPTV: thanh tìm kiếm + Tùy chọn đang ở phía trên player; yêu cầu mới là đưa xuống dưới player để vùng video cao hơn.

## Sửa đã commit

### IPTV toolbar xuống cuối màn hình
- MobileIptvUi chèn thanh tìm kiếm/tải lại/tùy chọn ở cuối mainRoot, thay vì phía trên inline player.
- HomeTabBar dành thêm 64dp đáy cho MainActivity để toolbar không bị navigation bar đè.
- Commit: fd6f285abb7682cced76e020b9ae69ec48f61692, aeb6bba044c2b11ab4887be8b92224489342c22.

### YouTube Home không release player
- SmartTube PlaybackActivity được patch để nhận biết onUserLeaveHint() của Home.
- Khi Home được nhấn, player được pause nhưng không release; khi quay lại Activity, player được tiếp tục nếu trước đó đang phát.
- Tab switching dùng FLAG_ACTIVITY_NO_USER_ACTION để không bị nhầm thành Home.
- Mục tiêu là không phát YouTube nền: Home chỉ giữ player trong Activity, không tiếp tục video khi app ở nền.
- Android xác nhận onUserLeaveHint() được gọi khi Activity sắp ra nền do lựa chọn của người dùng như Home; FLAG_ACTIVITY_NO_USER_ACTION ngăn callback khi Activity bị tạm dừng do Activity khác được khởi chạy. citeturn3search0turn6search1
- Media3 background playback là kiến trúc Service riêng và không được dùng cho yêu cầu Home hiện tại. citeturn4search0turn4search1
- Commit script: 84647a4ffd4ce71c290e8ed5955e6b1f82597112.

### Back từ YouTube video
- MobileNm7Application đăng ký Android 13+ Back callback cho SmartTube PlaybackActivity.
- Back đưa BrowseActivity lên trước rồi finish PlaybackActivity.
- Callback dùng reflection để giữ tương thích minSdk 23.
- Commits: 89ab17c90b2133bceb31d3016593bedaca1a8289 và 7c92c07a2036fce15f3981ba063b32d349ebeff2.

### YouTube recommendations một cột
- forceSingleColumn() nay áp dụng cả cho SmartTube PlaybackActivity, không chỉ BrowseActivity.
- Build script tiếp tục patch GRID_COLUMNS trong SmartTube Java/Kotlin.
- GridLayoutManager.setSpanCount(1) là cơ chế Android hỗ trợ cho grid dọc một cột. citeturn7search0turn7search3
- Commit runtime: 74eb83900b5b45fc5b438087e5fa9c5a2619e8e9.

### Giảm thời gian mở YouTube
- MobileNm7Application pre-warm SmartTubeRuntime sau 900ms kể từ khi ứng dụng khởi động.
- Mục tiêu là đưa chi phí reflection/class-loading ra khỏi lần chạm tab YouTube đầu tiên.
- Đây là tối ưu startup; tốc độ tải mạng/video phải đo lại trên máy thật.
- Commit: 2488cf4b6163f7717764ece436cf34d55e34973.

### Giảm branding SmartTube TV
- Windows build script thay text branding SmartTube trong XML/properties của phần SmartTube được clone thành NM7 TV.
- Vẫn giữ phone/touch fork và không đưa TV/VLC vào Mobile.
- Commit script mới nhất: 148bc07a3140084c99d576f2441995b7010ce3c9.

## Trạng thái

Đã sửa mã theo các lỗi vừa tái hiện. Chưa đánh dấu build mới thành công cho đến khi chạy script Windows và cài APK mới.

Test lại: IPTV → YouTube → IPTV; YouTube phát → Home → quay lại; YouTube Back; recommendations một cột; branding; IPTV toolbar dưới player; và thời gian mở YouTube.

---

# BÀN GIAO TIẾN ĐỘ — NM7 IPTV MOBILE 1.10.26 — 2026-09-19 01:56 +07:00

## Trạng thái sau vòng sửa mới nhất

Người dùng đã build lại Mobile 1.10.26 trên Windows và **BUILD SUCCESSFUL**.

Kết quả APK xác nhận:
- `NM7-IPTV-Mobile-1.10.26-arm64-v8a.apk` — **61,915,325 bytes**
- `NM7-IPTV-Mobile-1.10.26-armeabi-v7a.apk` — **51,421,065 bytes**
- Chỉ bàn giao 2 ABI Mobile: ARM64 + ARMv7.
- Không build Android TV trong vòng này.
- Các cảnh báo `META-INF/... not protected by signature` xuất hiện trong quá trình đóng gói là warning, không phải lỗi làm build thất bại.

## Lỗi compile vừa gặp và đã xử lý

Build trước đó dừng với:
```
error: duplicate class: vn.phuong.iptvplayer.HomeTabBar
```

Nguyên nhân là tồn tại đồng thời hai source có cùng class `HomeTabBar`:
- `app/src/main/java/vn/phuong/iptvplayer/HomeTabBar.java`
- `app/src/main/java/vn/phuongnm7/iptvplayer/HomeTabBar.java`

Đã xóa bản trùng trong `vn/phuongnm7/iptvplayer`, giữ lại bản đúng trong `vn/phuong/iptvplayer`, đồng thời giữ các thay đổi mới cho luồng IPTV → YouTube.

Commit xử lý cuối cùng:
- `4b55b3c4005245151ba0b7f68f77e389f4a67e87` — xóa source `HomeTabBar` trùng.
- `6d332fed6bbe6b0a0d6ca3ff06d6a76276fa23f2` — khôi phục/hoàn thiện `HomeTabBar` đúng package và luồng Mobile.

Đã kiểm tra cây source sau sửa: chỉ còn **một** `HomeTabBar.java`.

## IPTV → YouTube: trạng thái mã hiện tại

Yêu cầu đã được sửa theo hướng:
- Đang phát IPTV.
- Chỉ bấm chuyển sang tab YouTube, **chưa chọn video** → IPTV phải tiếp tục phát.
- Khi người dùng thực sự chọn một video và SmartTube `PlaybackActivity` bắt đầu → lúc đó NM7 mới pause/release IPTV để nhường decoder cho YouTube.

Các commit chính:
- `02c7f32f9d4d58f1fe01aaf94623576a95f38b45` — giữ IPTV chạy cho tới khi YouTube thực sự bắt đầu phát.
- `2cc0d9901b3848569364f1a4d8bcee85e2e10102` — không finish `PlayerActivity` khi chỉ mở Browse YouTube.

Trong `HomeTabBar.openBrowse()`, việc chuyển tab không còn gọi `activity.finish()`; SmartTube Browse được mở bằng `REORDER_TO_FRONT`. `PlayerActivity.prepareForYoutubeHandoff()` cũng không release player.

## YouTube BACK → mini-player: trạng thái mã hiện tại

Mục tiêu:
- YouTube đang phát video.
- Nhấn Back lần đầu → quay về Browse/Home YouTube và giữ video ở mini-player.
- Không đóng ngay playback session.

Đã xác định nguyên nhân của bản patch BACK trước:
- SmartTube `PlaybackActivity.onBackPressed()` đặt `mIsBackPressed = true`.
- `finish()` có đường dẫn `enterPipMode()` + `startParentView()`.
- Nhưng `skipPip()` có thể trả về true khi background shortcut là HOME, khiến playback Activity bị finish trực tiếp và bỏ qua đường dẫn mini-player.

Đã đổi patch sang sửa **`skipPip()`**, thay vì phụ thuộc vào hình dạng `onBackPressed()`:
```java
private boolean skipPip() {
    // NM7 Mobile: BACK from a playing video must return to YouTube Browse/mini-player,
    // not close the playback session. HOME/background handling remains separate.
    return false;
}
```

Commit:
- `f877e149c4fa593958f4c4cfe5e897c797356434` — Fix BACK via SmartTube parent view path.
- `ba63b09ad79318f0de7c476c76580edb252c9dda` — Validate SmartTube BACK patch.
- `28e78c504a99732f0002aa221b4a2b9d7e757cb4` — Preserve YouTube mini-player on first BACK.

Build script hiện dùng **PATCH15** và không còn patch trực tiếp `onBackPressed()` theo source shape cũ.

## YouTube background / quay lại ứng dụng

Cơ chế đã có từ vòng trước và vẫn được giữ:
- HOME/khóa màn hình khi YouTube đang phát được xử lý theo lifecycle/background mode của SmartTube.
- Tab YouTube được lưu vào `SharedPlaybackSession`.
- Khi mở lại NM7, có cơ chế đưa SmartTube Playback/Browse hiện hữu lên trước thay vì tạo lại luồng không cần thiết.
- Manifest Mobile giữ task state và launch mode phù hợp để bảo toàn phiên.
- Không coi đây là xác nhận cuối cùng cho đến khi test máy thật trên APK build mới.

## Tối ưu tốc độ YouTube hiện có

- SmartTube runtime dùng Cronet/player data source và connection keep-alive/connection pool.
- Có pre-warm SmartTube runtime.
- Giảm traversal UI không cần thiết.
- Browse/recommendations được ép theo hướng phone/mobile và một cột.
- MainActivity fallback khi khôi phục YouTube đã giảm delay xuống khoảng 80ms.
- Các tối ưu này là tối ưu khởi động/runtime; tốc độ tải video thực tế vẫn phải xác minh trên thiết bị thật.

## Các thay đổi Mobile khác đang giữ nguyên

- Phạm vi vẫn là **NM7 IPTV Mobile**, không đưa Android TV vào bản này.
- SmartTube phone/touch fork + upstream 32.47s.
- Media3/ExoPlayer resource collision đã được xử lý.
- OkHttp/AGP/Kotlin/compile compatibility của SmartTube đã xử lý.
- IPTV vẫn dùng Media3 player.
- ABI output chỉ ARM64 + ARMv7.
- Branding/logo Mobile đã được điều chỉnh theo các yêu cầu trước.
- Thanh tìm kiếm/tùy chọn IPTV đã được đưa xuống dưới player để tăng không gian hiển thị video.

## Trạng thái kiểm thử — CHỜ NGƯỜI DÙNG TEST

Build hiện tại đã **compile/package thành công**, nhưng các hành vi dưới đây chưa được đánh dấu PASS trên máy thật:

1. IPTV đang phát → bấm YouTube, **không chọn video** → IPTV vẫn phát.
2. Sau đó chọn video YouTube → IPTV chỉ dừng khi YouTube PlaybackActivity thực sự bắt đầu.
3. YouTube đang phát → Back 1 lần → Browse/Home + mini-player, video tiếp tục.
4. YouTube đang phát → Home/khóa màn hình → xử lý background đúng yêu cầu.
5. Mở lại NM7 → vẫn ở YouTube và khôi phục đúng video.
6. Chuyển IPTV ↔ YouTube nhiều lần không overlap/thoát bất thường.
7. YouTube load nhanh hơn và không bị thoát khi mở video.
8. UI YouTube vẫn đúng phone/mobile, không quay lại layout TV.
9. Chữ tiếng Việt trong SmartTube hiển thị đúng.
10. IPTV player và thanh tìm kiếm/tùy chọn không bị che hoặc mất diện tích bất thường.

## Quy tắc xử lý vòng tiếp theo

- Chờ kết quả test thực tế từ người dùng trước khi tiếp tục sửa.
- Nếu có lỗi: ghi lại bước tái hiện, kết quả thực tế, ảnh/video/log nếu có; sau đó mới xác định nguyên nhân và sửa.
- Không quay lại phương án dùng một ExoPlayer literal chung nếu chưa có thiết kế lifecycle chắc chắn; các thử nghiệm shared-player trước đây đã gây regression.
- Không đưa TV code/UI vào Mobile.
- Không đánh dấu 1.10.26 ổn định hoàn toàn chỉ vì build thành công.
- Khi cần tạo APK mới sau khi sửa mã ứng dụng, phải cập nhật version theo quy trình của dự án trước khi bàn giao.

## Điểm dừng để tiếp tục

**Hiện tại không cần sửa thêm trước khi test.**

Người dùng đã có APK build thành công và sẽ cài/test trên điện thoại. Kết quả test tiếp theo sẽ là đầu vào trực tiếp cho vòng sửa tiếp theo.


---

# TEST VIDEO 2026-09-19 — PHÂN TÍCH LỖI IPTV → YOUTUBE + BACK

Người dùng gửi video `video_2026-09-19_02-02-22.mp4` (~101,5 giây). Quan sát video cho thấy:
- Lần chuyển đầu IPTV → YouTube có lúc giữ được IPTV, nhưng các lần chuyển sau IPTV bị mất/dừng.
- Có các lần Browse YouTube tải lâu/hiển thị trạng thái loading.
- BACK từ YouTube Playback chưa đạt yêu cầu mini-player ổn định.

## Nguyên nhân kỹ thuật được xác định từ source

### IPTV → YouTube
Cơ chế cũ phụ thuộc vào `ActivityLifecycleCallbacks.onActivityPaused()` để đặt `backgroundPlaybackActive=true`. Trong khi `PlayerActivity.onPause()`/`onStop()` là nơi quyết định release ExoPlayer, thứ tự lifecycle callback này có thể tạo race. Điều này phù hợp với hiện tượng lần đầu có thể hoạt động nhưng các lần sau không ổn định.

### Sửa mới
Thêm cờ trực tiếp trong `PlayerActivity`:
`keepPlayerForTabSwitch`.

Khi `prepareForYoutubeHandoff()` được gọi:
- lưu vị trí IPTV;
- đặt `keepPlayerForTabSwitch=true`;
- không release player;
- chuyển tab sang YouTube.

`onPause()` và `onStop()` nay kiểm tra cờ này trước khi khởi động background service hoặc release ExoPlayer. `onStart()` reset cờ khi PlayerActivity thực sự trở lại foreground.

Commit:
- `b1ceb839c5f3e2274a011d0e65639c85e5a68fde`.

## YouTube BACK → mini-player
Đã xác định thêm một điểm xung đột: MobileNm7Application tự đăng ký `OnBackInvokedCallback` và từ callback đó gọi lại `activity.onBackPressed()`. Cách này có thể bypass/đụng với đường dispatch BACK native của SmartTube, khiến PlaybackActivity đóng thay vì đi qua parent-view/PIP path.

### Sửa mới
Không còn gọi `installSmartTubeBackHandling(activity)` khi tạo SmartTube PlaybackActivity. Để Android/SmartTube native Back dispatch xử lý, kết hợp với patch SmartTube `skipPip() -> false` đã có trong build script PATCH15.

Commit:
- `95cc48dc0830ae0436d23a0f658110902a1f1589`.

## Version
Vì đây là sửa code playback/lifecycle, version Mobile đã tăng:
- versionCode: **45**
- versionName: **1.10.27**

Commit:
- `03bc0a49104cdb9a79a1981e651858273fd0526c`.

## Trạng thái
Đã sửa source nhưng **chưa build APK 1.10.27**. Bước tiếp theo là build Windows và test lại:
1. IPTV → YouTube nhiều lần liên tiếp khi chưa chọn video: IPTV phải tiếp tục phát.
2. Chọn video YouTube: IPTV mới release.
3. YouTube video → BACK 1 lần: phải về Browse với mini-player, không đóng video.
4. YouTube → IPTV → YouTube lặp lại nhiều lần.
5. HOME/khóa màn hình và mở lại YouTube.

Không đánh dấu lỗi đã hết cho đến khi APK 1.10.27 được build và test máy thật.


---

# VÒNG SỬA 1.10.28 — 2026-09-19 06:35 +07:00

Người dùng xác nhận bản 1.10.27 vẫn còn hai lỗi:
- YouTube video → Back 1 lần chưa thu nhỏ về Browse/mini-player.
- IPTV đang phát → chuyển sang tab YouTube vẫn bị dừng ngay, trái yêu cầu phải giữ IPTV cho đến khi YouTube thực sự mở PlaybackActivity.

## Sửa IPTV → YouTube

Nguyên nhân còn sót: dù đã có `keepPlayerForTabSwitch`, `onStop()` vẫn có thể chạy sau khi trạng thái `tabSwitchPending` đã bị clear. Vì vậy lần chuyển tab sau có thể rơi vào nhánh `releasePlayer()`.

Sửa mới trong `PlayerActivity.onStop()`:
- Nếu Activity chưa `isFinishing()` và `SharedPlaybackSession.tab(this) == TAB_YOUTUBE`, luôn giữ IPTV ExoPlayer.
- `MobileNm7Application.isTabSwitchPending()` vẫn là lớp bảo vệ bổ sung.
- Chỉ release IPTV khi Activity thực sự kết thúc hoặc khi SmartTube PlaybackActivity bắt đầu và gọi `pauseIptvPlayer()`.

Commit:
- `0414fcb802537a74789bc3f6cc6c37351e09d488`.

Điều này loại bỏ dependency vào thứ tự callback lifecycle trong các lần chuyển tab lặp lại.

## Sửa YouTube Back → mini-player

Patch `skipPip() -> false` trước đó chưa đủ đối với phone PlaybackActivity. Bản phone có `onBackPressed()` riêng; gọi `super.onBackPressed()` làm PlaybackActivity kết thúc trước khi Browse/parent-view tiếp quản.

Build script nay patch trực tiếp `PlaybackActivity.onBackPressed()`:
- Giữ xử lý details/comments back.
- Đặt `mIsBackPressed=true`.
- Gọi `blockEngine(true)`.
- Gọi `getViewManager().blockTop(this)`.
- Gọi `getViewManager().startParentView(this)`.
- Chỉ fallback `super.onBackPressed()` nếu parent-view path ném exception.

Mục tiêu là giữ playback engine sống và đưa Browse lên làm parent, tạo đúng hành vi mini-player thay vì đóng video.

Commit:
- `82170585b8ac0b6f50cf5520c4c09a4a526bfe94`.

Custom Android `OnBackInvokedCallback` của NM7 vẫn đã được loại bỏ từ commit trước, nên không còn gọi `onBackPressed()` hai lần.

## Version mới

- versionCode: **46**
- versionName: **1.10.28**

Commit:
- `58bd32d3d695a2e2af764d7e83ac7a6a3387c3a0`.

## Chưa build

Source đã sửa nhưng **chưa chạy build Windows 1.10.28**.

Build/test bắt buộc:
1. IPTV phát → YouTube tab, không chọn video → IPTV tiếp tục phát.
2. Lặp IPTV → YouTube ít nhất 5 lần khi chưa mở video → IPTV không được tắt.
3. Mở video YouTube → IPTV mới release.
4. YouTube đang phát → Back 1 lần → Browse/Home + mini-player, video vẫn phát.
5. Back lần 2 mới thực hiện hành vi rời Browse theo thiết kế.
