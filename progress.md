# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 10/09/2026_

## Trạng thái hiện tại

- Android hiện tại: **Nm7 IPTV 1.10.6** (`versionCode 23`).
- Nhánh: `main`.
- Commit kích hoạt build 1.10.6: `3fc8bf33c3a94f9d22c22c1c2816fd53896c23af`.
- GitHub Actions: **Build #88**, run ID `34382403385` — **PASS toàn bộ**.
- Artifact APK: `Nm7-IPTV-1.10.6-APK`, artifact ID `10116564840`.
- SHA-256 APK: `b240fa504923c0709985966c2f30405f5c708f9e0dc1f9ec1cd8210c08478b7e`.
- Samsung Tizen vẫn **tạm dừng** cho tới khi có TV Samsung thật để kết nối, ký và kiểm thử.

## Các phần Android đã hoàn thành tới 1.10.5

- Giao diện Mobile phát video trực tiếp phía trên danh sách kênh.
- Khi đổi kênh, video tiếp tục phát trong khung Mobile thay vì nhảy sang PlayerActivity.
- Back khi đang phát inline: đóng khung phát trước, không thoát ứng dụng ngay.
- Hỗ trợ HLS, DASH, SmoothStreaming, RTSP, HTTP/HTTPS, RTMP, UDP MPEG-TS và DRM/ClearKey theo cấu hình nguồn.
- Tự thử lại khi gặp lỗi mạng/DRM tạm thời và nhận diện lại HLS/DASH sau redirect khi cần.
- Hiển thị độ phân giải thực tế và FPS thực tế khi phát.
- EPG/XMLTV có tên chương trình, giờ phát và tiến độ khi dữ liệu nguồn khớp.
- Hỗ trợ phát nền trên Mobile theo tùy chọn ứng dụng.
- Giữ màn hình sáng khi đang xem trực tiếp.
- Có fullscreen ngang và quay về dọc.
- Media3 TimeBar/seek hiển thị với nguồn thực sự hỗ trợ tua/DVR.

## Thay đổi hoàn thành trong Android 1.10.6

- Khi vừa mở kênh, bộ điều khiển phát mặc định **ẩn hoàn toàn**; chỉ hiện khi người dùng chạm vào video.
- `PlayerView` tắt `controllerAutoShow` để không tự bật nút Pause/seek sau khi bắt đầu phát hoặc đổi kênh.
- Giữ Play/Pause và thanh TimeBar chuẩn Media3 khi người dùng chạm vào video.
- Thanh TimeBar, thời gian hiện tại và tổng thời lượng chỉ hiện nếu nguồn thật sự hỗ trợ seek/DVR.
- Bỏ hoàn toàn nút chữ `Toàn màn hình` và `Xoay dọc`.
- Thêm nút **biểu tượng phóng to/thu nhỏ** trực tiếp trong hàng điều khiển Media3, đặt cạnh nút bánh răng.
- Khi ở khung Mobile, biểu tượng là phóng to; khi fullscreen, biểu tượng đổi thành thu nhỏ.
- Bấm biểu tượng phóng to: chuyển sang fullscreen ngang và ẩn system bars.
- Bấm biểu tượng thu nhỏ hoặc Back khi fullscreen: quay lại khung Mobile và màn hình dọc.
- Không tự bật controller sau khi vào/thoát fullscreen; người dùng chạm video mới hiện điều khiển.
- Tiếp tục giữ phát nền, giữ màn hình sáng, EPG, độ phân giải và FPS từ các bản trước.

## Các file chính vừa cập nhật

- `app/src/main/java/vn/phuong/iptvplayer/MobileInlinePlayerProviderV2.java`
- `app/src/main/res/drawable/ic_nm7_fullscreen.xml`
- `app/src/main/res/drawable/ic_nm7_fullscreen_exit.xml`
- `app/build.gradle.kts`
- `.github/workflows/android.yml`
- `progress.md`

## Kiểm thử / Build #88

Các bước sau đều **PASS**:
1. Compile.
2. Unit test.
3. Android lint.
4. Assemble APK.
5. Kiểm tra chữ ký APK.
6. Đóng gói artifact.
7. Android 15 emulator smoke test.

## Mốc tiếp tục

- Android: **1.10.6 / Build #88** là mốc hiện tại để tiếp tục kiểm thử thực tế trên điện thoại.
- Tập trung phản hồi tiếp theo vào trải nghiệm controller Mobile, DVR/seek và fullscreen/phóng to-thu nhỏ.
- Tizen: giữ nguyên trạng thái tạm dừng cho tới khi có TV thật.
