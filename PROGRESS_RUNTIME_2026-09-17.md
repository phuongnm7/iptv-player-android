# NM7 IPTV – RUNTIME UPDATE 17/09/2026

## Mục tiêu
- Không sửa APK.
- Giữ nguyên canonical playlist 12 nhóm / 424 playable entries.
- Endpoint `/api/nm7-private.m3u` phải tự cập nhật trận Chuối Chiên mà không phụ thuộc GitHub Actions.
- Khi người dùng Reload/Refresh, các trận hiện tại phải hiện rõ **giờ + ngày + đội A vs đội B**, thay vì chỉ hiện tên file stream kiểu `75748097_tsc.flv`.

## Thay đổi đã làm

### 1. GitHub Actions
Commit `856de00bc83c2fe4c401fe1bc41c1a12766de586` bảo vệ canonical inventory và không cho playlist tụt dưới 12 nhóm / 424 playable entries.

Commit `ff837a739ac1da1d163cda3fb3ec5bcdf481145e`:
- pin runner thành `ubuntu-24.04`;
- parser lịch Chuối Chiên hỗ trợ h3/h4;
- thêm fallback parser cho HTML/text thay đổi.

GitHub Actions run trước đó vẫn có hiện tượng job failure với `steps: []`, nên không dùng Actions làm nguồn sống duy nhất của playlist.

### 2. API runtime dynamic
Commit `8ea35f43417e0c0c913c045fb904e7ca0e8e1318` thêm khả năng:
- API đọc 4 canonical chunks;
- gọi lịch công khai `https://chuoichientv.link/lich-thi-dau/` tại thời điểm request;
- nhận các trận có BLV Chuối Lá/Nhỏ/To/Chao/Kem/Tây;
- ánh xạ sang các stream Chuối tương ứng;
- thêm `#EXTVLCOPT:http-referrer`;
- nếu nguồn lịch tạm thời lỗi thì vẫn trả canonical playlist, không làm endpoint fail.

Commit `c28544cd9805001230487f72d9ee187793859bfe` sửa parser JavaScript, thay `\\Z` không phù hợp bằng kết thúc regex `$` và tách hàm escape regex.

### 3. Fix mới nhất – hiển thị trận + refresh đúng
Commit `41398edd9498633a376cdafc107d1b3a154dc252`:
- dynamic match được đưa **lên đầu playlist** để app nhìn thấy ngay sau Reload;
- tên kênh không còn chỉ là tên file stream;
- format mới gồm: `HH:MM DD/MM ⚽ Đội A vs Đội B (BLV) [trạng thái/score]`;
- mỗi request lấy lại lịch công khai, không cache playlist;
- runtime overlay cũ được thay bằng dữ liệu mới, không cộng dồn trận cũ;
- quá 180 phút sau giờ bắt đầu sẽ tự bị lọc;
- canonical 424 entry không bị xóa để tránh lặp lại lỗi mất nhóm.

## Ví dụ lịch công khai đã xác nhận ngày 17/09/2026
Nguồn lịch hiện có:
- Coventry vs Aston Villa — 02:00 — Chuối Lá — 1-3
- Manchester United vs Brighton — 02:00 — Chuối Nhỏ, Trốc Tru — 2-3
- Barcelona vs Racing Santander — 02:30 — Chuối Chao — Đang đá 4-1
- Inter Miami vs Cruz Azul — 07:00 — Chuối Chao — Chưa đá
- Bogota FC vs Independiente Medellin — 08:20 — Chuối To — Chưa đá

Các trận này phải xuất hiện ở đầu M3U sau khi endpoint production nhận commit mới. Nguồn lịch chỉ cung cấp kênh BLV được ánh xạ; các nhóm khác vẫn đang dùng canonical inventory cho tới khi tìm được nguồn cập nhật hợp lệ tương ứng.

## Kiến trúc hiện tại

`canonical 4 chunks` → `API normalize` → `runtime fetch Chuối schedule` → `prepend current matches` → `filter finished (>180 phút)` → `M3U response`

## Vercel
Project production dự kiến: `byvn-m3u-proxy`
Endpoint: `https://byvn-m3u-proxy.vercel.app/api/nm7-private.m3u`

Trong phiên hiện tại chưa có bằng chứng xác nhận deployment production đã nhận commit `41398ed...`. Cần kiểm tra production sau deploy.

## Kiểm tra bắt buộc sau deploy
1. HTTP 200 tại `/api/nm7-private.m3u`.
2. Response bắt đầu bằng `#EXTM3U`.
3. Có đủ 12 `group-title` canonical.
4. Có ít nhất 424 playable entries trước/không làm suy giảm canonical inventory.
5. Entry động `Chuối Chiên TV` xuất hiện ở đầu response và chứa giờ + DD/MM + `Đội A vs Đội B`.
6. Reload lại sau khi lịch thay đổi phải nhận tên trận/link hiện hành, không cộng dồn entry động cũ.
7. Khi trận quá 180 phút sau giờ bắt đầu, entry động được lọc khỏi response.
8. Khi trang lịch Chuối Chiên lỗi, API vẫn trả canonical playlist.

## Ràng buộc
- Không sửa APK.
- Không dùng `superok-live-private` hoặc `byvn.net`.
- Không bypass token, DRM, authentication hoặc access control.
- Không thay canonical 4 chunks chỉ để sửa lỗi runtime.
