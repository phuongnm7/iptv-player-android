# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 10/09/2026_

## Trạng thái hiện tại

- Android candidate hiện tại: **Nm7 IPTV 1.10.9** (`versionCode 26`).
- Nhánh: `main`.
- Mốc stable đã được người dùng xác nhận trước đó: **1.10.7**.
- GitHub Actions **Build #118**, run ID `34421017708` — **PASS toàn bộ**.
- Commit kích hoạt build 1.10.9: `bc7c79d4740d9bb3ca9f49eaf40ba0af12243200`.
- Artifact APK: `Nm7-IPTV-1.10.9-APK`, artifact ID `10130973352`.
- SHA-256 APK: `fe334934d4721b3eef478e76d8e996fef2a416c8379bb742f8b0eb29eab296d7`.
- 1.10.9 chưa được đánh dấu stable cho tới khi kiểm thử thực tế trên TV và Mobile hoàn tất.
- Samsung Tizen vẫn tạm dừng cho tới khi có TV Samsung thật để kết nối, ký và kiểm thử.

## Các phần Android đã hoàn thành từ mốc stable

- Giao diện Mobile phát video trực tiếp phía trên danh sách kênh.
- Khi đổi kênh, video tiếp tục phát trong khung Mobile thay vì nhảy sang `PlayerActivity`.
- Back khi đang phát inline: đóng khung phát trước, không thoát ứng dụng ngay.
- Hỗ trợ HLS, DASH, SmoothStreaming, RTSP, HTTP/HTTPS, RTMP, UDP MPEG-TS và DRM/ClearKey theo cấu hình nguồn.
- Tự thử lại khi gặp lỗi mạng/DRM tạm thời và nhận diện lại HLS/DASH sau redirect khi cần.
- Hiển thị độ phân giải thực tế và FPS thực tế khi phát.
- EPG/XMLTV có tên chương trình, giờ phát và tiến độ khi dữ liệu nguồn khớp.
- Hỗ trợ phát nền trên Mobile theo tùy chọn ứng dụng.
- Giữ màn hình sáng khi đang xem trực tiếp.
- Media3 TimeBar/seek hiển thị với nguồn thực sự hỗ trợ tua/DVR.
- Controller mặc định ẩn khi mở kênh, chỉ hiện khi chạm video.
- Nút fullscreen dùng biểu tượng phóng to/thu nhỏ đặt cạnh bánh răng.
- Fullscreen ngang và quay về dọc đã được kiểm thử thực tế OK ở mốc trước.
- Bàn phím Mobile không còn che ô tìm kiếm.
- 1.10.8 bổ sung tối ưu duy trì kết nối luồng, HTTP keep-alive/Wi-Fi hiệu năng cao và nền hộp tùy chọn bán trong suốt.

## Thay đổi Android 1.10.9

### Sửa khởi động trên TV / landscape

- Xác định đường crash cụ thể: `MainActivity` luôn truy cập `btnReloadUrl` khi khởi động, nhưng `layout-land/activity_main.xml` trước đó không khai báo ID này.
- Đã bổ sung `btnReloadUrl` vào layout ngang, loại bỏ lỗi null khi TV mở `MainActivity` bằng resource landscape.
- Bổ sung regression smoke test: CI khởi động ứng dụng ở portrait, sau đó xoay landscape và khởi động lại để kiểm tra layout TV-style không thiếu control bắt buộc.
- Vẫn giữ nhận diện TV vật lý và tách hành vi Mobile khỏi TV/D-pad.

### Giao diện danh sách kênh Mobile

- Thu hẹp khoảng cách giữa các kênh: divider Mobile còn 2dp, padding dọc mặc định còn 1dp.
- Chiều cao card Mobile giảm; chế độ hàng thu gọn có chiều cao nhỏ hơn nữa.
- Logo/badge và vùng nút yêu thích/phát được thu gọn vừa phải để hiển thị nhiều kênh hơn trên màn hình.
- TV vẫn giữ chiều cao hàng lớn hơn để dễ điều khiển bằng D-pad.

### Vuốt chuyển nhóm

- Thay nhận diện vuốt theo khoảng cách cố định 72dp bằng nhận diện hướng + touch slop + vận tốc fling.
- Giảm ngưỡng chuyển nhóm để thao tác nhẹ và tự nhiên hơn.
- Không quay vòng từ nhóm cuối về nhóm đầu hoặc ngược lại khi vuốt quá mép.
- Thanh nhóm tự smooth-scroll để nhóm mới nằm gần giữa vùng nhìn thấy.
- Thêm chuyển động ngắn cho danh sách sau khi đổi nhóm để cảm giác chuyển trang mượt hơn.

### Bỏ long-press không cần thiết

- Trên Mobile, nhấn giữ tên/kênh trong danh sách không còn mở hộp thoại `Phát / Thêm vào Yêu thích`.
- Chạm kênh vẫn phát bình thường; nút ngôi sao vẫn là cách thêm/bỏ Yêu thích.

## Kiểm thử / Build #118

Các bước sau đều **PASS**:
1. Compile.
2. Unit test.
3. Android lint.
4. Assemble APK.
5. Kiểm tra chữ ký APK.
6. Đóng gói artifact.
7. Android 15 portrait smoke test.
8. Android 15 landscape/TV-style startup smoke test.

## Mốc tiếp tục

- Candidate để kiểm thử thực tế: **Nm7 IPTV 1.10.9 / Build #118**.
- Cần kiểm thử thực tế trên Android TV để xác nhận lỗi không khởi động đã hết trên thiết bị thật.
- Cần kiểm thử Mobile: mật độ danh sách, vuốt đổi nhóm, không còn long-press dialog.
- Chỉ sau khi các kiểm thử thực tế trên đạt yêu cầu mới nâng 1.10.9 thành mốc stable mới.
