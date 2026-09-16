# Tiến độ dự án NM7 IPTV

_Cập nhật: 16/09/2026_

## Trạng thái hiện tại

Dự án Android tiếp tục phát hành **2 APK riêng từ cùng mã nguồn**:

- **NM7 IPTV Mobile** — bản nhẹ, không đóng gói LibVLC.
- **NM7 IPTV TV** — bản Android TV, có LibVLC/FFmpeg fallback cho các trường hợp MediaCodec/Media3 không phát được.

Phiên bản đang triển khai trên nhánh `fix/mobile-1.10.26-sleep-timer-icon` là **1.10.26**, tích hợp YouTube native SmartTube Droid vào cùng APK Mobile.

## Mobile 1.10.26 — YouTube + IPTV

### Kiến trúc

- Một APK Mobile duy nhất.
- Thanh điều hướng cấp cao chỉ có **YouTube** và **IPTV**.
- IPTV giữ giao diện NM7 IPTV hiện tại.
- YouTube mở SmartTube Droid phone UI/native runtime trong cùng process, không dùng WebView và không mở ứng dụng ngoài.
- TV repo/nhánh riêng không bị thay đổi.

### Trạng thái tích hợp

- Đã hợp nhất commit SmartTube Mobile `724d89a5c94b5e9a4b3c45d667a18c8d493b1dca` vào head phát triển `ec5d3faeb8864cdab144a00bf03af07bb1d64ed0` thông qua merge commit có sẵn.
- Đã tạo nhánh sao lưu `backup/mobile-1.10.26-before-smarttube-20260916` trước khi chuyển head.
- CI tiếp theo phải xác nhận lại toàn bộ SmartTube submodule, Gradle patch, Mobile APK và Android 15 smoke test.

## Mục tiêu kiểm thử tiếp theo

1. Compile/test/lint Mobile 1.10.26.
2. Xác nhận SmartTube submodule và các dependency lồng nhau được khôi phục.
3. Package APK Mobile và bảo đảm không có LibVLC.
4. Smoke test mở IPTV.
5. Smoke test mở YouTube native SmartTube và quay lại IPTV.
6. Kiểm tra không tạo Activity stack lỗi khi chuyển YouTube ↔ IPTV nhiều lần.

