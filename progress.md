# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 10/09/2026_

## Trạng thái hiện tại

- Android candidate hiện tại: **Nm7 IPTV 1.10.10** (`versionCode 27`).
- Nhánh: `main`.
- **1.10.9 đã được người dùng xác nhận khởi động thành công trên TV thật**, khắc phục lỗi không mở ứng dụng của 1.10.8.
- Mốc stable đã được xác nhận đầy đủ trước đó: **1.10.7**; 1.10.9 là mốc TV-boot đã xác nhận.
- GitHub Actions **Build #127**, run ID `34423183621` — **PASS toàn bộ**.
- Commit build 1.10.10: `464c56d07b2dda6ee2284a5a92339da591207202`.
- Artifact APK: `Nm7-IPTV-1.10.10-APK`, artifact ID `10131732396`.
- SHA-256 APK: `85a7fb8baca7b4adaab21eed9cfc393c7597800c942de5b89ac52445d515289f`.
- 1.10.10 đang chờ kiểm thử thực tế TV/Mobile trước khi nâng thành stable mới.
- Samsung Tizen vẫn tạm dừng cho tới khi có TV Samsung thật để kết nối, ký và kiểm thử.

## Các phần Android đã hoàn thành từ mốc stable

- Mobile phát video inline phía trên danh sách kênh, đổi kênh ngay trong khung phát.
- Back đóng player inline trước, không thoát ứng dụng ngay.
- Hỗ trợ HLS, DASH, SmoothStreaming, RTSP, HTTP/HTTPS, RTMP, UDP MPEG-TS và DRM/ClearKey theo cấu hình nguồn.
- Tự thử lại lỗi mạng/DRM tạm thời; tối ưu HTTP keep-alive, Wi-Fi hiệu năng cao và duy trì phiên player.
- Hiển thị độ phân giải thực tế, FPS thực tế và EPG/XMLTV khi dữ liệu khớp.
- Phát nền, giữ màn hình sáng, seek/DVR, controller ẩn mặc định và fullscreen Mobile đã hoàn thiện qua các bản 1.10.x.
- Bàn phím Mobile không còn che ô tìm kiếm.
- Hộp thoại tùy chọn dùng nền bán trong suốt.

## Mốc 1.10.9 đã xác nhận

- Sửa crash TV/landscape do `btnReloadUrl` thiếu trong `layout-land/activity_main.xml`.
- CI có smoke test riêng cho portrait và landscape/TV-style.
- Người dùng đã cài trên TV thật và xác nhận ứng dụng **khởi động được**.
- Bỏ long-press `Phát / Yêu thích` trên danh sách Mobile.
- Bắt đầu thu gọn chiều cao hàng và cải thiện vuốt chuyển nhóm.

## Thay đổi Android 1.10.10

### TV

- Ẩn các nút `Link`, `Nguồn`, `Thông tin` ở header TV để đồng nhất với Mobile; các chức năng này vẫn nằm trong `Tùy chọn ứng dụng`.
- Giữ nút bánh răng Tùy chọn ở header.
- Thêm smoke assertion để bảo đảm các nút đã hợp nhất không xuất hiện lại ở cả portrait và landscape/TV-style.
- Hạ khoảng cách divider danh sách TV từ mức cũ 9dp xuống 3dp bằng tuner TV riêng, không thay đổi đường khởi động đã ổn định ở 1.10.9.

### Danh sách kênh

- Card kênh mặc định Mobile giảm còn khoảng 60dp; compact khoảng 54dp; TV khoảng 78dp.
- Logo/badge giảm còn 42dp; khoảng cách nội bộ, nút Yêu thích và biểu tượng phát được thu gọn tương ứng.
- Divider Mobile còn 1dp, TV 3dp.
- EPG vẫn tự mở rộng hàng khi có nội dung nên không cắt thông tin chương trình.

### Vuốt chuyển nhóm Mobile

- Đã tham khảo riêng phần stream của APK mẫu người dùng cung cấp.
- APK mẫu có `MobileIptvPlayerActivity`, `HorizontalGroupSwipeHelper` và dùng `GestureDetector` cho gesture ngang.
- Nm7 chuyển từ logic tự giữ `VelocityTracker`/chiếm touch stream sang `GestureDetector` quan sát gesture.
- `ListView` tiếp tục tự xử lý cuộn dọc, tap và fling gốc; detector chỉ đổi nhóm khi cú vuốt ngang đủ rõ.
- Hỗ trợ cả fling nhanh và vuốt chậm đủ khoảng cách; thanh nhóm tự smooth-scroll để nhóm mới nằm gần giữa màn hình.
- Bỏ hiệu ứng dịch ngang cưỡng bức của toàn danh sách để tránh cảm giác khựng.

## Kiểm thử / Build #127

Các bước sau đều **PASS**:
1. Compile.
2. Unit test.
3. Android lint.
4. Assemble APK.
5. Kiểm tra chữ ký APK.
6. Đóng gói artifact.
7. Android 15 portrait smoke test.
8. Android 15 landscape/TV-style startup smoke test.
9. Kiểm tra header đã ẩn `Link / Nguồn / Thông tin` trong smoke test.

## Mốc tiếp tục

- Candidate để kiểm thử thực tế: **Nm7 IPTV 1.10.10 / Build #127**.
- Cần kiểm thử TV: header đã gọn, khoảng cách hàng kênh hợp lý và điều khiển D-pad vẫn rõ focus.
- Cần kiểm thử Mobile: mật độ danh sách và cảm giác vuốt trái/phải đổi nhóm sau khi chuyển sang `GestureDetector`.
- Nếu các kiểm thử thực tế đạt yêu cầu, nâng 1.10.10 thành mốc stable mới.
