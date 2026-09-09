# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 10/09/2026_

## Trạng thái hiện tại

- Android đang triển khai: **Nm7 IPTV 1.10.6** (`versionCode 23`).
- Nhánh: `main`.
- Commit kích hoạt build 1.10.6: `3fc8bf33c3a94f9d22c22c1c2816fd53896c23af`.
- GitHub Actions hiện tại: **Build #88**, run ID `34382403385`.
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

## Thay đổi đang triển khai trong Android 1.10.6

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

## Kiểm thử / Build

Pipeline Android chạy các bước:
1. Compile.
2. Unit test.
3. Android lint.
4. Assemble APK.
5. Kiểm tra chữ ký APK.
6. Đóng gói artifact.
7. Android 15 emulator smoke test.

Build đang theo dõi: **#88 / run `34382403385`**. Nếu build lỗi sẽ đọc log và sửa ngay; khi PASS sẽ tải artifact `Nm7-IPTV-1.10.6-APK` và bàn giao APK để cài thử.

## Mốc tiếp tục

- Android: tiếp tục từ **1.10.6**, tập trung hoàn thiện trải nghiệm điều khiển video Mobile theo phản hồi thực tế.
- Tizen: giữ nguyên trạng thái tạm dừng cho tới khi có TV thật.
