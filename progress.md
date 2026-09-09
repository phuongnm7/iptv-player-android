# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 09/09/2026_

## Trạng thái hiện tại

Nhánh làm việc: `main`.

### Đã hoàn thành
- Ẩn/hiện URL nguồn phát bằng Settings; mặc định URL nguồn phát bị ẩn.
- Điều hướng remote trong danh sách nhanh và chuyển kênh UP/DOWN khi controller đang ẩn.
- Sửa lưu đồng bộ tùy chọn hiển thị nguồn phát để chịu được khởi động lại tiến trình ngay sau khi đổi Settings.
- `b22d8ea`: thêm nguồn IPTV mặc định:
  `https://iptv-live-merge.phuongnm7-iptv.workers.dev/playlist.m3u`
- `47da7f6` + `bb00dee`: hiển thị logo/icon kênh từ `tvg-logo`, có fallback chữ khi logo lỗi.
- `b039cfe`: bỏ ba nút `Chọn đang lọc`, `Bỏ chọn`, `Xuất M3U` khỏi giao diện chính.
- `beba7b5`: sửa xử lý remote khi xem video:
  - Controller ẩn: `LEFT` mở danh sách kênh.
  - Controller ẩn: `UP/DOWN` chuyển kênh.
  - `OK` khi controller ẩn: hiện controller.
  - Controller đang hiện: không chặn `LEFT/RIGHT`, để Media3 xử lý tua ngược/tua tới.

## Việc đang tiếp tục
### 1. Tự tải nguồn mặc định khi cài mới
`PlaylistSourceStore` đã có nguồn mặc định, nhưng `MainActivity.restoreSession()` hiện chỉ khôi phục `SessionStore`.

Cần sửa theo nguyên tắc:
- Có phiên cũ: khôi phục phiên cũ, không ghi đè.
- Không có phiên cũ: đặt URL mặc định và tự động tải playlist một lần.
- Sau khi người dùng thêm nguồn khác: nguồn mới vẫn được lưu và hoạt động độc lập.

### 2. Xóa sạch logic export M3U
Giao diện đã bỏ các nút nhưng `MainActivity` vẫn còn mã cũ:
- `SAVE_M3U`
- `btnSelectAll`, `btnSelectNone`, `btnExport`
- `setVisibleSelection`, `exportFile`, `writeExport`

Cần xóa sau khi rà lại toàn bộ tham chiếu để tránh lỗi biên dịch.

### 3. Kiểm tra bản sửa remote trên thiết bị/emulator
Luồng bắt buộc:
1. Phát một nguồn có DVR/seek window.
2. Bấm `OK` để hiện controller.
3. Bấm `LEFT` để tua ngược.
4. Bấm `RIGHT` để tua tới.
5. Đợi controller tự ẩn.
6. Bấm `LEFT` và xác nhận danh sách kênh nhanh mở ra.

Lưu ý: live stream không có DVR/seek window sẽ không thể tua ngược vượt ngoài dữ liệu mà máy chủ cung cấp.

## Kiểm tra trước khi bàn giao APK
- Build + unit test + lint PASS.
- Smoke test Android 15.
- Remote seek PASS.
- Logo URL hợp lệ và URL lỗi.
- Cài mới app tự tải playlist mặc định.
- Người dùng thêm playlist khác không bị nguồn mặc định ghi đè.
- Không còn nút export/chọn lọc trên giao diện chính.

## File chính
- `PlayerActivity.java`: remote, controller, phát và danh sách nhanh.
- `MainActivity.java`: tải nguồn, danh sách chính, phiên và logic export cũ.
- `ChannelAdapter.java` + `item_channel.xml`: logo kênh.
- `PlaylistSourceStore.java`: nguồn mặc định và nguồn người dùng.
- `progress.md`: nhật ký bàn giao.
