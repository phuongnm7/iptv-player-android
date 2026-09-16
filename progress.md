# Tiến độ dự án NM7 IPTV

_Cập nhật: 16/09/2026_

## Trạng thái hiện tại

Dự án Android tiếp tục phát hành **2 APK riêng từ cùng mã nguồn**:

- **NM7 IPTV Mobile** — bản nhẹ, không đóng gói LibVLC.
- **NM7 IPTV TV** — bản Android TV, có LibVLC/FFmpeg fallback cho các trường hợp MediaCodec/Media3 không phát được.

Phiên bản đang triển khai trên nhánh `fix/mobile-1.10.26-sleep-timer-icon` là **1.10.26**.

## Mobile 1.10.26 — Hẹn giờ đóng app

### Chức năng đã triển khai

- Thêm `SleepTimer.java` cho chức năng **Hẹn giờ đóng app**.
- Các mốc chọn nhanh: **15 / 30 / 45 / 60 / 90 / 120 phút**.
- Có **Tùy chỉnh 30–480 phút**.
- Lưu thời điểm hết hạn bằng `SharedPreferences` để khôi phục khi mở lại app.
- Khi hết thời gian, app gọi `finishAffinity()` để đóng toàn bộ Activity/task của ứng dụng.
- Có thể **bật/tắt hẹn giờ**, **Lưu**, hoặc **Tắt hẹn giờ**.
- Khi đang có timer, màn hình hẹn giờ hiển thị thời gian còn lại.

### Sự cố đã phát hiện và xử lý

Bản APK 1.10.26 đầu tiên có `SleepTimer.java` nhưng **chưa được gọi từ phần Settings**, nên người dùng cài APK thực tế không thấy chức năng hẹn giờ. Đây là lỗi tích hợp, không phải lỗi giao diện.

Sau đó đã sửa pipeline build để trong quá trình CI:

- chèn mục **Hẹn giờ đóng app** vào danh sách Settings;
- nối `SleepTimer.showDialog(this)` vào mục Settings;
- gọi `SleepTimer.restore(this)` khi `MainActivity.onCreate()`.

Workflow cũng có bước **Inject and verify Mobile sleep timer source** và xác nhận đủ 3 điểm tích hợp trước khi compile. Nếu thiếu một điểm, CI dừng build.

### Build 1.10.26 mới nhất

- Branch: `fix/mobile-1.10.26-sleep-timer-icon`
- Commit: `ec5d3faeb8864cdab144a00bf03af07bb1d64ed0`
- Commit message: `fix(mobile): remove duplicate Gradle sleep timer hook`
- Workflow: **Build private Android APK**
- Run: **#239**
- Run ID: `35066217383`
- Trạng thái hiện tại: **đã hoàn thành thành công**.

Các bước CI đã PASS:

1. Checkout / JDK 17 / Gradle 8.13 / Android SDK.
2. Inject và verify nguồn Hẹn giờ đóng app.
3. Compile, unit test và lint Mobile.
4. Đọc version Mobile `1.10.26`.
5. Verify và package APK.
6. Upload APK artifact.
7. Chuẩn bị Android emulator.
8. Smoke test Mobile trên Android 15.
9. Upload reports.

### Artifact

- Artifact: `NM7-IPTV-Mobile-1.10.26-APK`
- Artifact ID: `10433714053`
- SHA-256 của artifact archive: `5ff5936022b4aab55e792df2978c103090e5365127720d65c0cdfdf93a554aa2`

APK đã được lấy ra để người dùng cài đặt và kiểm tra thực tế.

## Kiến trúc Mobile / TV

`app/build.gradle.kts` dùng flavor dimension `device`.

### Mobile

- Flavor: `mobile`.
- Version: `1.10.26-mobile`.
- Không chứa `libvlc.so`.
- Media3/ExoPlayer là engine phát chính.
- Player inline trên màn hình danh sách.
- Có các tùy chọn UI/player đã có từ các bản trước.

### TV

- Flavor: `tv`.
- Version theo nhánh TV riêng.
- Có `tvImplementation("org.videolan.android:libvlc-all:3.6.1")`.
- Media3/ExoPlayer là engine chính.
- LibVLC/FFmpeg là fallback riêng cho TV.

## Các chức năng Mobile đã có trước 1.10.26

- Player inline phía trên danh sách kênh.
- Chọn kênh khác tiếp tục phát trong player inline.
- Controller mặc định ẩn; chạm video mới hiện điều khiển.
- Fullscreen/phóng to/thu nhỏ.
- Xoay ngang/dọc.
- Hiển thị độ phân giải và FPS thực tế.
- Giữ màn hình sáng khi xem.
- Phát nền theo tùy chọn.
- Tìm kiếm, nhóm kênh, Yêu thích, Gần đây.
- Vuốt ngang danh sách để đổi nhóm.
- Vuốt dọc cạnh trái player để chỉnh độ sáng.
- Vuốt dọc cạnh phải player để chỉnh âm lượng.
- Có overlay phần trăm khi chỉnh sáng/âm lượng.
- Lưu playlist/session lớn ngoài UI thread để giảm nguy cơ ANR.
- Highlight dòng kênh đang phát trong danh sách.

## Trạng thái TV

- D-pad/focus dành riêng cho TV.
- Danh sách nhóm/kênh tối ưu cho điều khiển TV.
- Quick channel list bằng phím điều hướng theo thiết kế hiện tại.
- Media3 decoder fallback và các tầng phục hồi 1080p/720p.
- LibVLC/FFmpeg fallback cho TV.
- Controller TV không hiện ngay khi vừa mở kênh; bấm **OK/Enter** mới hiện.
- Menu bánh răng chứa các chức năng **Định dạng / Chất lượng / Khung hình**.

## Reload playlist / chống cache

Cơ chế Reload hiện có:

- `HttpURLConnection.setUseCaches(false)`.
- `Cache-Control: no-cache, no-store, max-age=0`.
- `Pragma: no-cache`.
- Với `raw.githubusercontent.com`, request được thêm tham số timestamp `_nm7_reload=<thời gian>` để hạn chế dữ liệu cũ trong cache.

## DRM / ClearKey

- `DrmPlayback` tiếp tục hỗ trợ Widevine, ClearKey và PlayReady theo metadata do playlist/nhà cung cấp cung cấp.
- Không tự tìm, suy đoán hoặc hiển thị khóa DRM.
- Parser ClearKey tiếp tục hỗ trợ các cấu trúc hợp lệ phổ biến.

## Lưu ý về kiểm thử 1.10.26

CI của Run #239 đã PASS cả compile/test/lint/package và Android 15 smoke test.

**Cần kiểm thử thực tế trên điện thoại** để xác nhận UI Settings hiển thị mục **Hẹn giờ đóng app** và thử thực tế ít nhất một mốc 15 phút hoặc mốc tùy chỉnh.

Khi kiểm tra trên thiết bị, đường dẫn dự kiến là:

`Tùy chọn ứng dụng → Hẹn giờ đóng app`

Sau khi lưu, app phải thông báo đã hẹn và khi hết thời gian phải tự đóng app.

## Mốc tiếp theo

### Mobile

- Kiểm thử thực tế 1.10.26 trên điện thoại.
- Xác nhận Settings hiển thị **Hẹn giờ đóng app**.
- Kiểm tra 15/30/60 phút và Tùy chỉnh.
- Kiểm tra Tắt hẹn giờ và khôi phục trạng thái sau khi mở lại app.
- Tiếp tục theo dõi ANR khi dùng playlist lớn.

### TV

- Tiếp tục kiểm thử riêng theo nhánh/repo TV.
- Không gộp trạng thái build Mobile 1.10.26 vào trạng thái TV.

## Quy ước tiếp tục phát triển

- Android luôn tạo **2 APK riêng: Mobile và TV**.
- LibVLC/FFmpeg tiếp tục là TV-only.
- Thay đổi riêng TV không được làm tăng đáng kể dung lượng Mobile.
- Thay đổi parser/DRM/network dùng chung phải kiểm thử cả Mobile và TV.
- Không regression khả năng phát 4K TV đã xác nhận từ các bản trước.
- Chỉ đánh dấu bản stable sau kiểm thử thực tế trên thiết bị phù hợp.
