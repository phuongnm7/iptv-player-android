# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 09/09/2026_

## Trạng thái hiện tại

### Phiên bản mã nguồn mới nhất
- Commit hiện tại: `7d2444b01012e6f2858e413e9b5627ac33da7c7d`.
- Nội dung commit: sửa smoke test Android để chờ các thành phần giao diện ổn định thay vì yêu cầu danh sách kênh xuất hiện ngay khi khởi động.
- Workflow GitHub Actions hiện tại: **Build #44**, run ID `34309525888`.

### Kết quả Build #44 đến thời điểm cập nhật
- PASS: Compile Android.
- PASS: Unit test.
- PASS: Lint.
- PASS: Verify và đóng gói APK.
- PASS: Upload APK artifact.
- PASS: Chuẩn bị Android 15 emulator.
- ĐANG CHẠY: kiểm tra ứng dụng trên Android 15 emulator.
- CHƯA CHẠY: upload báo cáo kiểm thử/lint và hoàn tất workflow.

## Các lỗi smoke test đã xử lý

### Build #41
- `MainActivity.java` có lỗi biên dịch.
- Đã sửa `TextWatcher`, lời gọi `SessionStore.save`, snapshot danh sách kênh và ID panel.

### Build #43
- APK build thành công nhưng smoke test tìm `listChannels` ngay khi khởi động.
- Trên lần mở đầu, danh sách có thể chưa là thành phần phù hợp để kiểm tra ngay.

### Build #44
- Smoke test đã được sửa để:
  1. Khởi động `MainActivity`.
  2. Chờ `mainRoot` xuất hiện.
  3. Kiểm tra các control ổn định `btnSources` và `btnAllChannels`.
- Mục tiêu là xác nhận ứng dụng mở được trên Android 15 mà không phụ thuộc vào trạng thái mở/đóng của panel nguồn.

## Chức năng đã thay đổi
- Cài mới hoặc không có phiên hợp lệ: tự tải nguồn IPTV mặc định:
  `https://iptv-live-merge.phuongnm7-iptv.workers.dev/playlist.m3u`
- Có phiên hợp lệ: khôi phục playlist trước đó, không ghi đè.
- Người dùng vẫn có thể thêm và chọn nguồn IPTV khác thủ công.
- Logo/icon kênh đã được thêm ở các thay đổi trước.
- Đã loại bỏ các chức năng/nút Chọn đang lọc, Bỏ chọn và Xuất M3U khỏi giao diện chính.

## Điều khiển remote
- Controller ẩn:
  - `OK`: hiện controller.
  - `LEFT`: mở danh sách kênh nhanh.
  - `UP/DOWN`: đổi kênh.
- Controller hiện:
  - `LEFT`: để Media3 xử lý tua ngược.
  - `RIGHT`: để Media3 xử lý tua tới.
- Khả năng tua thực tế phụ thuộc nguồn phát có DVR hoặc seek window.

## APK hiện tại
- APK của Build #44 đã được tạo và upload dưới dạng artifact.
- Chưa coi là bản bàn giao hoàn chỉnh cho đến khi bước kiểm tra Android 15 kết thúc PASS.

## Việc tiếp theo
1. Theo dõi bước smoke test Android 15 của Build #44.
2. Nếu PASS: tải APK artifact của Build #44 và bàn giao làm bản đã kiểm thử.
3. Nếu FAIL: đọc log, xác định lỗi ứng dụng hay lỗi smoke test, sửa đúng nguyên nhân và build lại.
4. Sau khi có bản PASS, cập nhật `progress.md` với commit và artifact cuối cùng.

## Bàn giao cho người tiếp tục
Không nên dùng trạng thái Build #43 làm mốc bàn giao vì smoke test của lượt đó đã lỗi thời. Mốc hiện tại là commit `7d2444b` và workflow Build #44. Khi tiếp tục, ưu tiên kiểm tra kết quả job `Check current app on Android 15 emulator` trước khi sửa thêm mã ứng dụng.
