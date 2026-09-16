# Tiến độ dự án NM7 IPTV

_Cập nhật: 16/09/2026_

## Trạng thái hiện tại

Dự án Android tiếp tục phát hành **2 APK riêng từ cùng mã nguồn**:

- **NM7 IPTV Mobile** — bản nhẹ, không đóng gói LibVLC.
- **NM7 IPTV TV** — bản Android TV, có LibVLC/FFmpeg fallback cho các trường hợp MediaCodec/Media3 không phát được.

Phiên bản đang triển khai trên nhánh `feature/mobile-youtube-smarttube` là **1.10.26**, tích hợp YouTube native SmartTube Droid vào cùng APK Mobile.

## Mobile 1.10.26 — YouTube + IPTV

### Kiến trúc

- Một APK Mobile duy nhất.
- Thanh điều hướng cấp cao chỉ có **YouTube** và **IPTV**.
- IPTV giữ giao diện NM7 IPTV hiện tại.
- YouTube mở SmartTube Droid phone UI/native runtime trong cùng process, không dùng WebView và không mở ứng dụng ngoài.
- TV repo/nhánh riêng không bị thay đổi.

### Trạng thái CI hiện tại

Đang kích hoạt lại CI trên head mới sau khi thay bridge SmartTube bằng reflection để loại compile-time dependency khỏi các Java entry point của app.

- Branch: `feature/mobile-youtube-smarttube`
- APK mục tiêu: `NM7-IPTV-Mobile-1.10.26.apk`

