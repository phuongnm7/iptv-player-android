# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 09/09/2026_

## Phiên bản 1.7.1 — sửa giao diện TV, khôi phục Cài đặt và icon mới

- Commit mã đã kiểm thử: `7baa5941426fbf7919179c7eba7d7ed0340b7e88`.
- GitHub Actions: **Build #49**, run ID `34315634209`; compile, unit test, lint, đóng gói APK và smoke test Android 15 đều PASS.
- Đã xóa hoàn toàn các nút `Chọn đang lọc`, `Bỏ chọn`, `Xuất M3U` khỏi cả bố cục dọc và bố cục ngang/TV.
- Khôi phục menu Cài đặt: chọn giao diện Tự động/Mobile/TV, đổi hình nền, hiện/ẩn URL, mật độ hàng, FPS, đồng hồ, nguồn phát, xóa lịch sử và thông tin ứng dụng.
- Thêm launcher icon và TV banner mới dựa trên ảnh `nm7 IPTV` người dùng cung cấp.
- Version: `versionCode 10`, `versionName 1.7.1`.
- Artifact: `Nm7-IPTV-1.7.1-APK`, ID `10090014404`, archive SHA-256 `d863c81a4d18a0d0ac3b6fe7d794c99205f0ab140a3b6e8a8df82509f767ec9a`.

## Trạng thái hiện tại: HOÀN TẤT CI

- Repository: `phuongnm7/iptv-player-android`
- Branch: `main`
- Commit đã kiểm thử: `df3576c53e2ff14455e6a33ffe69aa1c71fe82cb`
- GitHub Actions: **Build #47**, run ID `34312750176`
- Kết quả workflow: **PASS toàn bộ**

### Kết quả Build #47

- PASS: Compile Android.
- PASS: Unit test.
- PASS: Lint.
- PASS: Verify và đóng gói APK.
- PASS: Upload APK artifact.
- PASS: Chuẩn bị Android 15 emulator.
- PASS: Smoke test ứng dụng trên Android 15.
- PASS: Upload báo cáo kiểm thử/lint.

## APK đã xác nhận

- Tên file: `Nm7-IPTV-1.7.apk`
- Artifact: `Nm7-IPTV-1.7-APK`
- Artifact ID: `10089006618`
- Thời hạn artifact trên GitHub: 09/10/2026.
- SHA-256: `1b9429d4b1eb977617dd5a4a6ffaaeb69a9de48fdd7d6f566eff048cdd990cc1`

Đây là APK đầu tiên sau mốc bàn giao Build #44 đã PASS toàn bộ workflow, bao gồm kiểm tra chạy ứng dụng trên Android 15.

## Nguyên nhân lỗi Build #44–#46 và cách xử lý

### Build #44

Smoke test báo `Main screen did not open` nhưng log chưa đủ dữ liệu để phân biệt lỗi ứng dụng và lỗi môi trường.

### Build #45

Đã bổ sung chẩn đoán: kết quả xác nhận `MainActivity` vẫn chạy bình thường. Emulator CI không phân giải được hostname của playlist mặc định, ứng dụng hiển thị hộp thoại lỗi mạng và hộp thoại này che cây giao diện.

### Build #46

Smoke test đã đóng riêng hộp thoại lỗi tải playlist và xác nhận màn hình chính xuất hiện. Lỗi còn lại là khi playlist rỗng, Android UI Automator hiển thị `txtEmpty` thay cho `listChannels`.

### Build #47

Smoke test chấp nhận đúng hai trạng thái hợp lệ của vùng nội dung: danh sách kênh `listChannels` hoặc trạng thái rỗng `txtEmpty`. Các nút chính vẫn bắt buộc đầy đủ và các control M3U đã loại bỏ vẫn bắt buộc không xuất hiện. Workflow PASS toàn bộ.

## Các chức năng hiện có

- Cài mới hoặc không có phiên hợp lệ: tự tải nguồn IPTV mặc định:
  `https://iptv-live-merge.phuongnm7-iptv.workers.dev/playlist.m3u`
- Có phiên hợp lệ: khôi phục playlist trước đó, không ghi đè.
- Có thể thêm và chọn nguồn IPTV khác thủ công.
- Có logo/icon kênh.
- Đã loại bỏ nút Chọn đang lọc, Bỏ chọn và Xuất M3U khỏi giao diện chính.

## Điều khiển remote

- Controller ẩn:
  - `OK`: hiện controller.
  - `LEFT`: mở danh sách kênh nhanh.
  - `UP/DOWN`: đổi kênh.
- Controller hiện:
  - `LEFT`: Media3 xử lý tua ngược.
  - `RIGHT`: Media3 xử lý tua tới.
- Khả năng tua phụ thuộc nguồn phát có DVR hoặc seek window.

## Mốc tiếp tục trong tương lai

Bắt đầu từ commit `df3576c53e2ff14455e6a33ffe69aa1c71fe82cb` và Build #47. APK của Build #47 là bản hiện tại đã được CI xác nhận trên Android 15.
