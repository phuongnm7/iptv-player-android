# Tiến độ dự án NM7 IPTV

_Cập nhật: 17/09/2026_

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

## NM7 playlist / Vercel proxy — cập nhật 17/09/2026

### Đã xử lý

- Đã phân tích file playlist được bàn giao.
- File gốc gồm **684 block `#EXTINF`**.
- Sau chuẩn hóa, bỏ entry không có URL HTTP/HTTPS, loại `None` và dedup, còn **303 entry phát duy nhất / 303 URL HTTP(S) duy nhất**.
- File gồm nhiều nhóm nguồn; không phải một M3U nguồn duy nhất.
- Đã tạo snapshot chuẩn hóa từ chính file bàn giao và đóng gói gzip + base64 thành 4 chunk.
- Đã cập nhật cả 4 chunk vào `playlist/nm7-private.part1.b64` đến `part4.b64` trên `main`.
- Vercel API tiếp tục đọc bốn chunk này và tự lọc các sự kiện đã quá thời gian.
- Endpoint production giữ nguyên: `https://byvn-m3u-proxy.vercel.app/api/nm7-private.m3u`.
- Parser API đã được sửa để đọc đúng snapshot M3U dạng một dòng/đa nguồn và các URL HLS thực tế.
- GitHub Actions updater chạy mỗi 10 phút.
- Workflow không tự đoán nguồn ngoài; khi chưa có upstream URL được phép, snapshot hiện tại được giữ an toàn.

### Phần còn lại

- Cần một upstream M3U/M3U8/API **được phép sử dụng** để biến updater thành cập nhật live thực sự. Snapshot hiện tại chỉ chứa playlist đã tổng hợp và các URL phát; nó không xác định chắc chắn một URL nguồn duy nhất.
- Không tự tạo hoặc đoán URL nguồn.
- Không sử dụng `byvn.net`.
