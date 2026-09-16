# Tiến độ dự án NM7 IPTV

_Cập nhật: 16/09/2026 22:08 (GMT+7)_

## 1. Phạm vi dự án

Repo đang làm việc: `phuongnm7/iptv-player-android`.

Repo TV `phuongnm7/nm7-tv-android` **không được thay đổi** trong đợt tích hợp này.

Mục tiêu hiện tại: hoàn thiện **NM7 IPTV Mobile 1.10.26** thành **một APK duy nhất** có hai khu vực cấp cao:

- **IPTV**: giữ giao diện NM7 IPTV hiện tại.
- **YouTube**: chạy giao diện/runtime điện thoại của **SmartTube Droid native** trong cùng APK và cùng process; không dùng WebView và không mở app YouTube/SmartTube bên ngoài.

---

## 2. Phiên bản Mobile 1.10.26

Đã nâng Mobile lên:

- `versionCode = 44`
- `versionName = 1.10.26`
- application id: `vn.phuong.iptvplayer`
- flavor Mobile: `mobile`
- Mobile không đóng gói LibVLC; LibVLC vẫn chỉ dành cho TV.

File cấu hình hiện tại xác nhận Mobile có dependency:

```kotlin
add("mobileImplementation", project(":smarttube"))
```

và SmartTube module mặc định map về `device = mobile`. 

---

## 3. Giao diện điều hướng YouTube / IPTV

Đã triển khai `HomeTabBar.java` với đúng **2 mục cấp cao**:

1. YouTube
2. IPTV

Khi ở IPTV, chọn YouTube sẽ mở `SmartTubeHomeActivity`.

Khi ở YouTube, chọn IPTV sẽ quay về `MainActivity`.

`Nm7Application` đã được nối để gắn thanh điều hướng vào màn hình `MainActivity` của Mobile.

SmartTube screen sử dụng `SmartTubeHomeActivity` kế thừa `BrowseActivity` của SmartTube Droid.

---

## 4. Tích hợp SmartTube Droid

Đã đưa SmartTube Droid vào source tree dưới:

`third_party/SmartTube-droid`

và khai báo các module cần thiết trong `settings.gradle.kts`, gồm các nhóm:

- SmartTube common / UI support
- SharedModules
- MediaServiceCore
- ExoPlayer 2.10.6 stack mà SmartTube sử dụng

Đã tạo module Android library nội bộ:

`smarttube/build.gradle.kts`

Module này lấy source phone UI/runtime từ SmartTube Droid và được đóng gói vào Mobile thông qua `mobileImplementation`.

Đã thêm AndroidManifest cho các Activity SmartTube cần dùng trong APK tích hợp.

---

## 5. Bridge runtime YouTube

Đã tạo:

- `SmartTubeHomeActivity.java`
- `YoutubeActivity.java`
- `SmartTubeRuntime.java`

Mục đích:

- `YoutubeActivity` là entry point tương thích cho Settings/shortcut cũ.
- `SmartTubeHomeActivity` mở native SmartTube phone Browse UI.
- `SmartTubeRuntime` đăng ký `ViewManager`/các Activity native của SmartTube.

Đã kiểm tra source upstream SmartTube Droid: `DroidApplication` của upstream thực hiện việc khởi tạo `ViewManager`, tắt TV DPI scaling và tắt screensaver support cho phone UI. Vì APK NM7 vẫn sử dụng `Nm7Application`, phần khởi tạo SmartTube đang được bridge trong app thay vì thay thế Application của NM7.

---

## 6. Các thay đổi Mobile 1.10.26 khác đã có

### Hẹn giờ đóng app

Đã có `SleepTimer.java` với:

- 15 / 30 / 45 / 60 / 90 / 120 phút.
- Tùy chỉnh 30–480 phút.
- Lưu deadline bằng `SharedPreferences`.
- Khôi phục timer khi mở lại app.
- Hết thời gian gọi `finishAffinity()` để đóng task của app.
- Có nút lưu, tắt timer và hiển thị thời gian còn lại.

CI trước đây đã từng có bước inject/verify mục **Hẹn giờ đóng app** vào Settings và `SleepTimer.restore(this)` khi khởi động.

### Playlist / reload

Các thay đổi trước đó trong Mobile 1.10.26 gồm:

- reload playlist có chống cache;
- request Raw GitHub được thêm timestamp khi tải lại;
- tránh tình trạng request cũ ghi đè request mới;
- background refresh không làm mất navigation hiện tại;
- lưu/khôi phục source URL đã được chuẩn hóa.

### Player / UI

Các chức năng Mobile trước 1.10.26 tiếp tục được giữ:

- player inline;
- highlight kênh đang phát;
- điều khiển sáng/âm lượng bằng gesture;
- hỗ trợ DRM hiện có;
- session/playlist xử lý ngoài UI thread;
- các UI/player settings hiện tại của NM7 IPTV.

---

## 7. CI / Build pipeline SmartTube

Workflow đã được mở rộng để build SmartTube tích hợp:

- checkout source + submodules;
- clone/restore nested SmartTube dependencies;
- patch Gradle legacy của SmartTube để phù hợp Gradle/AGP hiện tại;
- bổ sung `device` flavor mapping `mobile` cho SmartTube;
- cài Android SDK 34/36 và Build Tools 30.0.3/36.0.0;
- chạy unit test + lint + assemble Mobile;
- package APK và kiểm tra chữ ký;
- kiểm tra Mobile không chứa `libvlc.so`;
- upload artifact;
- chạy Android 15 smoke test.

---

## 8. CI hiện tại — CHƯA CÓ APK GỬI ĐƯỢC

PR:

`https://github.com/phuongnm7/iptv-player-android/pull/1`

Title: `Integrate Mobile 1.10.26 YouTube SmartTube UI`

Branch: `feature/mobile-youtube-smarttube`

Current PR head đã được xác nhận là:

`7cfe0cf2dd8035a0b706eae1e1630db94d0794a6`

Base `main`:

`cc172cc492bb11803da90a069fac7750f21776da`

CI run mới nhất của head này:

- Run ID: `35097142282`
- Job: `build`
- Kết quả: **FAIL**
- Thời gian: 16/09/2026 12:40–12:42 UTC.

### Lỗi chính hiện tại

Build dừng ở bước compile Mobile do các entry point Java của NM7 còn import trực tiếp các class SmartTube:

- `SmartTubeHomeActivity` kế thừa trực tiếp `com.liskovsoft.smartyoutubetv2.droid.ui.browse.BrowseActivity`.
- `SmartTubeRuntime` import trực tiếp các `ViewManager`, `BrowseView`, `PlaybackView`, `BrowseActivity`, v.v.

CI trước đó đã từng xác định vấn đề tương ứng là classpath/dependency integration của `app` với SmartTube chưa hoàn chỉnh trên runner. Vì vậy **chưa được phép coi build là xanh** và **chưa có APK 1.10.26 hợp lệ để gửi**.

Lưu ý: workflow và source hiện tại đã có `mobileImplementation(project(":smarttube"))`, nhưng run `35097142282` vẫn fail. Cần chạy lại một head mới sau khi hoàn tất bridge/build fix và xác nhận bằng CI thực tế.

---

## 9. Một mốc build 1.10.26 trước đó

Trước khi ghép SmartTube, nhánh Mobile 1.10.26 riêng đã từng có build thành công:

- Run: `#239`
- Run ID: `35066217383`
- Commit: `ec5d3faeb8864cdab144a00bf03af07bb1d64ed0`
- Compile/test/lint/package Mobile: PASS.
- Artifact từng có tên: `NM7-IPTV-Mobile-1.10.26-APK`.

Mốc này **không phải APK SmartTube 2-trong-1 hiện tại**; không sử dụng nó để tuyên bố rằng bản YouTube + IPTV đã hoàn thành.

---

## 10. Trạng thái chính thức hiện tại

| Hạng mục | Trạng thái |
|---|---|
| Mobile version 1.10.26 | ✅ Có |
| IPTV UI hiện tại | ✅ Giữ nguyên |
| Top-level YouTube / IPTV | ✅ Đã viết |
| SmartTube source tích hợp | ✅ Đã đưa vào repo |
| SmartTube phone UI bridge | ⚠️ Đang hoàn thiện |
| TV repo | ✅ Không đụng vào |
| Unit test/lint/assemble của bản SmartTube | ❌ CI hiện tại fail |
| APK SmartTube + IPTV 1.10.26 | ❌ Chưa có bản xác nhận |
| Artifact tải về cho bản SmartTube | ❌ Chưa có |
| Smoke test Android 15 của bản SmartTube | ⏳ Chưa tới bước vì compile đang fail |

---

## 11. Việc còn lại để chốt APK

Thứ tự xử lý:

1. Sửa bridge SmartTube để `app` compile ổn định với module `:smarttube`.
2. Push một head mới lên `feature/mobile-youtube-smarttube`.
3. Chờ CI chạy toàn bộ `testMobileDebugUnitTest`, `lintMobileDebug`, `assembleMobileDebug`.
4. Nếu compile xanh, tiếp tục kiểm tra package/signature và chạy Android 15 smoke test.
5. Khi artifact `NM7-IPTV-Mobile-1.10.26-APK` xuất hiện và build thành công, tải artifact ra và kiểm tra SHA-256/APK thực tế.
6. Chỉ sau khi xác minh artifact mới gửi APK cho người dùng.

---

## 12. Quy ước tiếp tục

- Chỉ làm việc trên repo `phuongnm7/iptv-player-android` cho Mobile.
- **Không thay đổi repo TV** trong luồng này.
- Không đánh dấu bản stable khi CI compile/package hoặc smoke test chưa xác nhận.
- Không gửi link APK giả hoặc link artifact của build khác.
- Khi có APK mới, phải ghi rõ commit, run ID, artifact ID và SHA-256 vào file này.
