# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 09/09/2026_

## Trạng thái build hiện tại

Commit sửa lỗi chính: `85642c6`.
Workflow GitHub Actions: `34307798626`.

### Đã PASS
- Compile Android.
- Unit test.
- Lint.
- Verify và đóng gói APK.
- APK artifact đã được tạo: `Nm7-IPTV-1.7-APK`.

### Đang chạy
- Smoke test ứng dụng trên Android 15 emulator.
- Tại thời điểm cập nhật, bước build/test/package đã hoàn tất thành công; chỉ còn bước kiểm tra emulator chưa kết thúc.

## Các chức năng đã thay đổi
- Cài mới hoặc không có phiên hợp lệ: tự tải nguồn IPTV mặc định `https://iptv-live-merge.phuongnm7-iptv.workers.dev/playlist.m3u`.
- Có phiên hợp lệ: khôi phục playlist trước đó, không ghi đè.
- Người dùng vẫn có thể thêm/chọn nguồn khác thủ công.
- Logo/icon kênh đã được thêm ở các commit trước.
- Các nút Chọn đang lọc, Bỏ chọn và Xuất M3U đã được bỏ khỏi giao diện chính.

## Điều khiển remote
- Controller ẩn: `OK` hiện controller; `LEFT` mở danh sách nhanh; `UP/DOWN` đổi kênh.
- Controller hiện: `LEFT/RIGHT` không còn bị PlayerActivity chặn để Media3 xử lý tua ngược/tua tới.
- Khả năng tua thực tế phụ thuộc nguồn phát có DVR hoặc seek window.

## Việc còn lại
1. Chờ smoke test Android 15 kết thúc và đọc kết quả.
2. Nếu PASS: xác nhận APK là bản bàn giao đã kiểm thử.
3. Nếu FAIL: đọc log, sửa lỗi, build lại và thay APK artifact.

## Bàn giao hiện tại
APK artifact đã có thể tải, nhưng được tạo trước khi smoke test cuối cùng hoàn tất. Nếu cần cài ngay, có thể dùng artifact này để thử nghiệm; kết quả smoke test cần được kiểm tra lại trước khi coi đây là bản phát hành hoàn chỉnh.
