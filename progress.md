# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 10/09/2026_

## Trạng thái hiện tại

- Android đang triển khai: **Nm7 IPTV 1.10.7** (`versionCode 24`).
- Nhánh: `main`.
- Bản 1.10.6 / Build #88 đã PASS và người dùng xác nhận các thao tác controller, seek, fullscreen và thu/phóng hoạt động OK.
- Lỗi còn lại được ghi nhận từ video thực tế: khi chạm ô tìm kiếm trên Mobile, bàn phím ảo che/đè sát ô tìm kiếm nên không nhìn rõ ký tự đang nhập.
- Sửa 1.10.7: đặt `MainActivity` dùng `windowSoftInputMode="stateAlwaysHidden|adjustPan"` để Android tự dịch toàn bộ cửa sổ lên đủ khoảng trống khi IME mở, giữ ô tìm kiếm và nội dung đang gõ nằm phía trên bàn phím; khi đóng bàn phím giao diện tự trở về vị trí ban đầu.
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

## Thay đổi Android 1.10.7

- Khắc phục bàn phím ảo che ô `Tìm kênh, nhóm hoặc URL…` khi video inline đang phát phía trên.
- Khi bàn phím mở, cửa sổ MainActivity được pan lên theo ô đang focus thay vì để IME phủ lên vùng nhập.
- Không thay đổi logic phát video, controller, seek/DVR, fullscreen, phát nền, EPG, độ phân giải hoặc FPS đã ổn định ở 1.10.6.

## File cập nhật cho 1.10.7

- `app/src/main/AndroidManifest.xml`
- `app/build.gradle.kts`
- `.github/workflows/android.yml`
- `progress.md`

## Kiểm thử / Build

Pipeline Android chạy:
1. Compile.
2. Unit test.
3. Android lint.
4. Assemble APK.
5. Kiểm tra chữ ký APK.
6. Đóng gói artifact.
7. Android 15 emulator smoke test.

Đang theo dõi build 1.10.7. Khi PASS sẽ tải artifact `Nm7-IPTV-1.10.7-APK` và bàn giao APK để kiểm thử trực tiếp lỗi bàn phím.

## Mốc tiếp tục

- Android: tiếp tục từ **1.10.7**, ưu tiên xác nhận trên điện thoại thật rằng ô tìm kiếm luôn nhìn thấy đầy đủ khi bàn phím mở.
- Tizen: giữ nguyên trạng thái tạm dừng cho tới khi có TV thật.
