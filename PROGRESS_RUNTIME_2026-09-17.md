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

## 4. Multi-source overlay cho các nhóm còn lại
Các commit mới sau khi Chuối Chiên đã xác nhận hoạt động:
- `83169dc8cbebde453aaf3ba05f9502ce70ee749a` — thêm module `api/nm7-schedule-overlay.mjs`.
- `f684095426a2cf9118ee58a0fd9e1f0e44422dd9` — thêm router `api/nm7-private-router.m3u.js` để giữ endpoint cũ nhưng chạy lớp metadata overlay mới.
- `c4c0aafbc2b15e34939670c4dc8496a273a06212` — cập nhật `vercel.json`: endpoint `/api/nm7-private.m3u` được rewrite qua router và các chunk playlist được include cho router.
- `b7e3994be5f89e7290604e903a1da640ab240c64` — xóa endpoint debug tạm thời sau khi hoàn tất kiểm tra kiến trúc.

Overlay mới hoạt động theo nguyên tắc an toàn:
- không thay URL stream canonical;
- không xóa canonical entries;
- lấy lịch từ các nguồn công khai đã xác định cho Vua Sân Cỏ, Giờ Vàng, Bia Ôm, Xôi Lạc Z, Sao Kê, Gà Vàng, Cola, Khán Đài và Socolive;
- chỉ đổi tên entry khi có mức khớp đủ mạnh với dữ liệu trận;
- nếu nguồn lịch lỗi hoặc không khớp thì giữ nguyên entry cũ, tránh gán sai trận vào stream.

## Ví dụ nguồn công khai đã xác nhận
- Vua Sân Cỏ có trang live/lịch với giờ, đội và trạng thái trận.
- Bia Ôm hiển thị trực tiếp và lịch theo BLV.
- Giờ Vàng có dữ liệu trận/lịch trực tiếp.
- Sao Kê có lịch và trận theo ngày.
- Gà Vàng có trang lịch thi đấu.
- Cola có hệ thống danh sách trận.
- Khán Đài có danh sách trận theo giờ và BLV.

## Kiến trúc hiện tại

`canonical 4 chunks` → `legacy API` → `Chuối runtime overlay` → `multi-source metadata overlay` → `filter/response` → app

Endpoint người dùng vẫn giữ nguyên:
`https://byvn-m3u-proxy.vercel.app/api/nm7-private.m3u`

## Vercel
Project production: `byvn-m3u-proxy`
Git repository: `phuongnm7/iptv-player-android`
Production branch: `main`

Vercel Git integration tự động tạo deployment khi có push vào production branch. Sau các commit mới ở trên, cần chờ deployment Production mới chuyển sang Ready trước khi test app. Vercel xác nhận Git-connected projects deploy theo push và `main` là production branch mặc định trong cấu hình thông thường.

## Kiểm tra bắt buộc sau deploy
1. HTTP 200 tại `/api/nm7-private.m3u`.
2. Response bắt đầu bằng `#EXTM3U`.
3. Có đủ 12 `group-title` canonical.
4. Có ít nhất 424 playable entries trước/không làm suy giảm canonical inventory.
5. Chuối Chiên vẫn hiển thị trận động ở đầu response.
6. Các nhóm khác chỉ được đổi tên khi có mapping đủ mạnh; không gán bừa stream.
7. Reload lại app phải giữ nguyên URL endpoint và nhận metadata mới.
8. Khi trận quá 180 phút sau giờ bắt đầu, entry động Chuối được lọc khỏi response.
9. Khi nguồn lịch phụ lỗi, canonical playlist vẫn được phục vụ.

## Ràng buộc
- Không sửa APK.
- Không dùng `superok-live-private` hoặc `byvn.net`.
- Không bypass token, DRM, authentication hoặc access control.
- Không thay canonical 4 chunks chỉ để sửa lỗi runtime.
