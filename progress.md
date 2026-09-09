# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 09/09/2026_

## Trạng thái bàn giao

Nhánh làm việc: `main`  
Bản gần nhất trước đợt sửa này: commit `fc95f13`.

### Đã hoàn thành
- Ẩn/hiện URL nguồn phát bằng Settings; mặc định URL nguồn phát bị ẩn.
- Điều hướng remote trong danh sách nhanh và chuyển kênh UP/DOWN khi controller đang ẩn.
- Sửa lưu đồng bộ tùy chọn hiển thị nguồn phát để chịu được khởi động lại tiến trình ngay sau khi đổi Settings.
- Build, unit test và lint của bản trước đã chạy thành công; APK được tạo bởi GitHub Actions.

### Vấn đề còn tồn tại được người dùng báo
1. Trong khi xem kênh, phím LEFT luôn mở danh sách kênh nên không thể tua ngược.
2. Cần hành vi remote rõ ràng: `OK` mở controller; khi controller đang mở, `LEFT` tua ngược và `RIGHT` tua tới. Khi controller ẩn, `LEFT` mở danh sách nhanh; `UP/DOWN` chuyển kênh.
3. Danh sách chính chưa hiển thị logo/icon kênh dù parser đã lưu `tvg-logo` vào `Channel.logo()`.
4. Cần bỏ hoàn toàn chức năng xuất M3U và các nút dưới giao diện chính: `Chọn đang lọc`, `Bỏ chọn`, `Xuất M3U`.
5. Cần có nguồn IPTV mặc định khi cài app lần đầu:
   `https://iptv-live-merge.phuongnm7-iptv.workers.dev/playlist.m3u`
   Người dùng vẫn có thể thêm nguồn khác thủ công.

## Đợt sửa hiện tại
Đang thực hiện các thay đổi trên trực tiếp trên `main`. Sau khi commit, cần theo dõi GitHub Actions và xác minh đặc biệt:
- `OK -> LEFT/RIGHT` hoạt động tua seek trên nguồn có seek window; với live stream không có DVR, player không thể tua ngoài cửa sổ thời gian mà nguồn cung cấp.
- `LEFT` chỉ mở danh sách nhanh khi controller đang ẩn.
- Logo kênh tải được và có fallback chữ khi URL logo lỗi.
- Không còn nút/chức năng export M3U ở giao diện chính.
- Cài mới app tự tải playlist mặc định, nhưng không tự ghi đè nguồn người dùng đã thêm hoặc phiên đã lưu.

## Gợi ý cho người tiếp nhận
Các file chính:
- `PlayerActivity.java`: remote, controller, phát và danh sách nhanh.
- `MainActivity.java`: tải nguồn, danh sách chính và phiên làm việc.
- `ChannelAdapter.java` + `item_channel.xml`: hiển thị từng kênh/logo.
- `PlaylistSourceStore.java`: nguồn playlist mặc định và nguồn do người dùng thêm.
- `progress.md`: nhật ký bàn giao này.
