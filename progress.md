# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 10/09/2026_

## Trạng thái hiện tại

- Android hiện tại: **Nm7 IPTV 1.10.7** (`versionCode 24`).
- Nhánh: `main`.
- Bản 1.10.6 / Build #88 đã được người dùng xác nhận các thao tác controller, seek, fullscreen và thu/phóng hoạt động OK.
- Lỗi bàn phím che ô tìm kiếm đã được sửa trong 1.10.7 bằng `windowSoftInputMode="stateAlwaysHidden|adjustPan"` trên `MainActivity`, để Android dịch cửa sổ theo vùng đang nhập và giữ ô tìm kiếm phía trên bàn phím ảo.
- GitHub Actions **Build #91**, run ID `34384069614` — **PASS toàn bộ**.
- Commit kích hoạt build 1.10.7: `ce2e1f431f38bf322bd152d1df9d22a7f759ca4a`.
- Artifact APK: `Nm7-IPTV-1.10.7-APK`, artifact ID `10117193677`.
- SHA-256 APK: `f3fa7932b9124988a561aa27c24751e8a4fd83314f3df82575016c9cd54cf9bf`.
- Samsung Tizen vẫn **tạm dừng** cho tới khi có TV Samsung thật để kết nối, ký và kiểm thử.

## Các phần Android đã hoàn thành

- Giao diện Mobile phát video trực tiếp phía trên danh sách kênh.
- Khi đổi kênh, video tiếp tục phát trong khung Mobile thay vì nhảy sang PlayerActivity.
- Back khi đang phát inline: đóng khung phát trước, không thoát ứng dụng ngay.
- Hỗ trợ HLS, DASH, SmoothStreaming, RTSP, HTTP/HTTPS, RTMP, UDP MPEG-TS và DRM/ClearKey theo cấu hình nguồn.
- Tự thử lại khi gặp lỗi mạng/DRM tạm thời và nhận diện lại HLS/DASH sau redirect khi cần.
- Hiển thị độ phân giải thực tế và FPS thực tế khi phát.
- EPG/XMLTV có tên chương trình, giờ phát và tiến độ khi dữ liệu nguồn khớp.
- Hỗ trợ phát nền trên Mobile theo tùy chọn ứng dụng.
- Giữ màn hình sáng khi đang xem trực tiếp.
- Media3 TimeBar/seek hiển thị với nguồn thực sự hỗ trợ tua/DVR.
- Controller mặc định ẩn khi mở kênh, chỉ hiện khi chạm video.
- Nút fullscreen dùng biểu tượng phóng to/thu nhỏ đặt cạnh bánh răng; fullscreen ngang và quay về dọc đã được kiểm thử thực tế OK.
- Khi bàn phím Mobile mở ở ô tìm kiếm, giao diện được pan lên để ô nhập và ký tự đang gõ không bị IME che.

## Thay đổi Android 1.10.7

- Khắc phục bàn phím ảo che ô `Tìm kênh, nhóm hoặc URL…` khi video inline đang phát phía trên.
- Khi bàn phím mở, `MainActivity` dùng chế độ `adjustPan`, ưu tiên đưa EditText đang focus lên phía trên bàn phím thay vì để IME phủ lên vùng nhập.
- `stateAlwaysHidden` đảm bảo bàn phím không tự bật khi vừa mở ứng dụng; bàn phím chỉ hiện khi người dùng chạm ô nhập.
- Không thay đổi logic phát video, controller, seek/DVR, fullscreen, phát nền, EPG, độ phân giải hoặc FPS đã ổn định ở 1.10.6.

## File cập nhật cho 1.10.7

- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `.github/workflows/android.yml`
- `progress.md`

## Kiểm thử / Build #91

Các bước sau đều **PASS**:
1. Compile.
2. Unit test.
3. Android lint.
4. Assemble APK.
5. Kiểm tra chữ ký APK.
6. Đóng gói artifact.
7. Android 15 emulator smoke test.

## Mốc tiếp tục

- Android: **1.10.7 / Build #91** là mốc hiện tại. Bước cần xác nhận trên điện thoại thật: mở một kênh để có player inline, chạm ô tìm kiếm, bật bàn phím và gõ nhiều ký tự để chắc chắn toàn bộ ô nhập luôn nằm phía trên bàn phím.
- Tizen: giữ nguyên trạng thái tạm dừng cho tới khi có TV thật.
