# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 09/09/2026_

## Trạng thái bàn giao

Nhánh làm việc: `main`.

### Đã hoàn thành trước đợt hiện tại
- Ẩn/hiện URL nguồn phát bằng Settings; mặc định URL nguồn phát bị ẩn.
- Điều hướng remote trong danh sách nhanh và chuyển kênh UP/DOWN khi controller đang ẩn.
- Sửa lưu đồng bộ tùy chọn hiển thị nguồn phát để chịu được khởi động lại tiến trình ngay sau khi đổi Settings.
- Bản trước đã qua build, unit test và lint; APK được tạo bởi GitHub Actions.

## Thay đổi đã đưa lên main trong đợt này
- `74b4213`: tạo file `progress.md` để bàn giao dự án.
- `b22d8ea`: thêm nguồn IPTV mặc định vào `PlaylistSourceStore`:
  `https://iptv-live-merge.phuongnm7-iptv.workers.dev/playlist.m3u`
  Khi chưa có nguồn người dùng, danh sách nguồn hiển thị nguồn mặc định; người dùng vẫn có thể thêm URL thủ công.
- `47da7f6` + `bb00dee`: thêm `ImageView` và tải bất đồng bộ logo từ `Channel.logo()` (`tvg-logo`), có fallback chữ khi logo không tải được.
- `b039cfe`: bỏ ba nút `Chọn đang lọc`, `Bỏ chọn`, `Xuất M3U` khỏi giao diện chính. Các ID được giữ ẩn tạm thời để mã cũ không bị crash trong lúc xóa logic export theo từng bước.

## Việc bắt buộc còn lại
### 1. Sửa remote seek — ưu tiên cao
Trong `PlayerActivity.dispatchKeyEvent()`, hiện `KEYCODE_DPAD_LEFT` bị chặn vô điều kiện để mở danh sách nhanh. Cần đổi thứ tự xử lý thành:

- `OK` / `DPAD_CENTER` khi controller ẩn: gọi `playerView.showController()`.
- Khi controller **đang hiện**: không chặn `DPAD_LEFT` hoặc `DPAD_RIGHT`; trả cho `PlayerView`/Media3 để thực hiện seek backward/forward.
- Khi controller **ẩn**: `DPAD_LEFT` mở danh sách nhanh.
- Khi controller ẩn: `DPAD_UP/DOWN` chuyển kênh như hiện tại.

Lưu ý: với live stream không có DVR/seek window, không thể tua ngược vượt ra ngoài dữ liệu mà nguồn cung cấp.

### 2. Tự tải nguồn mặc định khi cài mới
Hiện nguồn mặc định đã xuất hiện trong danh sách nguồn, nhưng `MainActivity.restoreSession()` vẫn chỉ khôi phục phiên cũ. Cần bổ sung: nếu `SessionStore.load()` trả về `null`, đặt `inputUrl` thành `PlaylistSourceStore.DEFAULT_URL` và tự gọi luồng tải playlist một lần. Không ghi đè phiên đã lưu.

### 3. Xóa sạch logic export M3U
Giao diện đã bỏ các nút, nhưng `MainActivity` vẫn còn các hằng `SAVE_M3U`, listener và các phương thức export cũ. Sau khi smoke test ổn định, xóa hoàn toàn:
- `SAVE_M3U`
- listener `btnSelectAll`, `btnSelectNone`, `btnExport`
- `setVisibleSelection`, `exportFile`, `writeExport` và import/output không còn dùng.

## Kiểm tra trước khi bàn giao APK
- Build + unit test + lint PASS.
- Smoke test Android 15.
- Test remote: phát nguồn có DVR/seek -> `OK` -> `LEFT` tua ngược -> `RIGHT` tua tới; controller ẩn -> `LEFT` mở danh sách nhanh.
- Test logo URL hợp lệ và URL lỗi.
- Cài mới app -> tự tải playlist mặc định.
- Người dùng thêm playlist thứ hai -> không bị nguồn mặc định ghi đè.
- Không còn nút export/chọn lọc ở giao diện chính.

## File chính
- `PlayerActivity.java`: remote, controller, phát và danh sách nhanh.
- `MainActivity.java`: tải nguồn, danh sách chính, phiên và logic export cũ.
- `ChannelAdapter.java` + `item_channel.xml`: logo kênh.
- `PlaylistSourceStore.java`: nguồn mặc định và nguồn người dùng.
- `progress.md`: nhật ký bàn giao.
