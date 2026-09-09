# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 09/09/2026_

## Trạng thái bàn giao hiện tại

### Mã nguồn mới nhất
- Commit chức năng/smoke test hiện tại: `7d2444b01012e6f2858e413e9b5627ac33da7c7d`.
- Commit này sửa smoke test Android để chờ các thành phần giao diện ổn định thay vì yêu cầu danh sách kênh xuất hiện ngay khi khởi động.
- Workflow GitHub Actions: **Build #44**, run ID `34309525888`.

### Kết quả cuối cùng của Build #44
- PASS: Compile Android.
- PASS: Unit test.
- PASS: Lint.
- PASS: Verify và đóng gói APK.
- PASS: Upload APK artifact.
- PASS: Chuẩn bị Android 15 emulator.
- FAIL: `Check current app on Android 15 emulator` (smoke test).
- Vì smoke test thất bại, toàn bộ workflow có trạng thái **FAILED**.

## APK hiện tại
- APK của Build #44 đã được tạo thành công và upload dưới dạng GitHub Actions artifact.
- APK có thể được cài thử, nhưng **chưa được xác nhận là bản hoàn chỉnh** vì smoke test Android 15 chưa PASS.
- Không bàn giao APK này như bản phát hành cuối cùng cho đến khi lỗi smoke test được xác minh và một lượt build mới PASS toàn bộ.

## Các lỗi đã xử lý trước đó

### Build #41
- `MainActivity.java` có lỗi biên dịch.
- Đã sửa `TextWatcher`, lời gọi `SessionStore.save`, snapshot danh sách kênh và ID panel.

### Build #43
- APK build thành công nhưng smoke test tìm `listChannels` ngay khi khởi động.
- Trên lần mở đầu, danh sách kênh có thể chưa phải thành phần phù hợp để kiểm tra ngay.

### Build #44
- Smoke test đã được sửa để:
  1. Khởi động `MainActivity`.
  2. Chờ `mainRoot` xuất hiện.
  3. Kiểm tra các control ổn định `btnSources` và `btnAllChannels`.
- Tuy nhiên job smoke test cuối cùng vẫn FAIL. Người tiếp tục cần đọc log đầy đủ của job `Check current app on Android 15 emulator` để xác định chính xác control hoặc thao tác nào còn gây lỗi; không nên tiếp tục giả định nguyên nhân.

## Các chức năng đã thay đổi
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

## Việc cần làm tiếp theo

### Ưu tiên 1: Phân tích lỗi Build #44
1. Tải/đọc log đầy đủ của job `Check current app on Android 15 emulator`.
2. Xác định đây là lỗi ứng dụng, lỗi emulator hay lỗi smoke test.
3. Sửa đúng nguyên nhân; không chỉ làm test dễ hơn để bỏ qua lỗi ứng dụng.

### Ưu tiên 2: Chạy lại toàn bộ CI
1. Đẩy commit sửa lỗi.
2. Xác nhận Compile PASS.
3. Xác nhận Unit test PASS.
4. Xác nhận Lint PASS.
5. Xác nhận APK được đóng gói thành công.
6. Xác nhận smoke test Android 15 PASS.

### Ưu tiên 3: Bàn giao
Chỉ sau khi workflow PASS toàn bộ mới tải APK artifact của lượt build cuối và gửi trực tiếp file `.apk` cho người dùng.

## Mốc bàn giao
Người tiếp tục dự án nên bắt đầu từ:
- Repository: `phuongnm7/iptv-player-android`
- Branch: `main`
- Commit chức năng/smoke test gần nhất: `7d2444b01012e6f2858e413e9b5627ac33da7c7d`
- Workflow cần điều tra: Build #44 / run ID `34309525888`

**Lưu ý:** `progress.md` này phản ánh kết quả cuối cùng đã biết của Build #44. Trạng thái hiện tại là APK build thành công nhưng workflow FAILED do smoke test Android 15, nên chưa có bản phát hành hoàn chỉnh.