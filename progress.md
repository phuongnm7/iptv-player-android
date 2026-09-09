# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 09/09/2026_

## Trạng thái hiện tại: BẢN 1.7.3 ĐÃ ĐƯỢC NGƯỜI DÙNG XÁC NHẬN OK

- Repository: `phuongnm7/iptv-player-android`
- Branch: `main`
- Commit hiện tại: `f03cf8447a8d6a3c2a56db879b1e74bc2ca00fd4`
- Version: `versionCode 12`, `versionName 1.7.3`
- GitHub Actions: **Build #57**, run ID `34321819246`
- Kết quả workflow: **PASS toàn bộ**
- Artifact APK: `Nm7-IPTV-1.7.3-APK`
- Artifact ID: `10092247830`
- Archive SHA-256: `4478d1618f973f428136b7ee7d1f8f12ab38a0edc06b161db36a506982978f78`
- Artifact hết hạn: 09/10/2026
- Người dùng đã cài/thử và xác nhận: **“bản này đã ok”**.

## Các thay đổi đã hoàn thành trong 1.7.3

- Bỏ khối chữ thống kê số kênh, số kênh trùng và URL thiếu/sai khỏi màn hình chính.
- Bỏ dòng `Nguồn: …` khỏi màn hình chính.
- Khi bấm `+ Nguồn`, bảng nhập nguồn mở ra với ô URL trống và được focus.
- Sửa lỗi `ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED` đối với URL không có đuôi nhưng chuyển hướng sang HLS/DASH.
- Trình phát tự kiểm tra địa chỉ chuyển hướng và chọn đúng định dạng HLS hoặc DASH; không cần chọn thủ công.

## Các thay đổi kế thừa từ 1.7.2

- Tự nối lại khi playlist đi đến trạng thái kết thúc.
- Tự nối lại đối với lỗi mạng tạm thời và HTTP 408/429/5xx.
- Giới hạn tối đa 4 lần thử với thời gian chờ tăng dần; lỗi quyền truy cập, DRM và codec không bị lặp vô hạn.
- Trên TV, khi bảng điều khiển trình phát đang hiện:
  - Back lần đầu: ẩn bảng điều khiển/tùy chọn.
  - Back lần tiếp theo: rời màn hình phát.

## Các thay đổi kế thừa từ 1.7.1

- Xóa các nút `Chọn đang lọc`, `Bỏ chọn`, `Xuất M3U` khỏi giao diện Mobile và TV.
- Khôi phục menu Cài đặt với lựa chọn giao diện, hình nền, URL, mật độ hàng, FPS, đồng hồ, nguồn phát, lịch sử và thông tin ứng dụng.
- Thêm launcher icon và TV banner từ ảnh `nm7 IPTV` do người dùng cung cấp.
- Build #49 / run ID `34315634209` đã PASS toàn bộ.

## Kết quả kiểm tra Build #57

- PASS: Compile Android.
- PASS: Unit test.
- PASS: Lint.
- PASS: Verify và đóng gói APK.
- PASS: Upload APK artifact.
- PASS: Chuẩn bị Android 15 emulator.
- PASS: Smoke test ứng dụng trên Android 15.
- PASS: Upload báo cáo kiểm thử/lint.

## Chức năng và điều khiển hiện tại

- Tự tải playlist mặc định khi chưa có phiên hợp lệ.
- Khôi phục playlist của phiên trước khi có dữ liệu hợp lệ.
- Hỗ trợ thêm/chọn nguồn IPTV khác, mở file và phát URL trực tiếp.
- Có logo kênh, Yêu thích, Gần đây, lọc nhóm và tìm kiếm.
- Controller ẩn trên TV:
  - `OK`: hiện controller.
  - `LEFT`: mở danh sách kênh nhanh.
  - `UP/DOWN`: đổi kênh.
- Controller hiện:
  - `BACK`: ẩn controller.
  - `LEFT/RIGHT`: Media3 xử lý tua nếu nguồn có DVR/seek window.

## Mốc tiếp tục trong tương lai

Tiếp tục từ commit `f03cf8447a8d6a3c2a56db879b1e74bc2ca00fd4`, Build #57, phiên bản 1.7.3. Đây là bản hiện tại đã PASS CI trên Android 15 và được người dùng xác nhận hoạt động ổn.
