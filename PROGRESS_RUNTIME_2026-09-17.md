# NM7 IPTV – RUNTIME UPDATE 17/09/2026

## Mục tiêu
- Không sửa APK.
- Giữ nguyên canonical playlist 12 nhóm / 424 playable entries.
- Endpoint `/api/nm7-private.m3u` phải tự cập nhật trận Chuối Chiên mà không phụ thuộc GitHub Actions.

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
- thêm `#NM7-DYNAMIC:ChuoiChien` và `#EXTVLCOPT:http-referrer`;
- nếu nguồn lịch tạm thời lỗi thì vẫn trả canonical playlist, không làm endpoint fail.

Commit `c28544cd9805001230487f72d9ee187793859bfe` sửa parser JavaScript, thay `\\Z` không phù hợp bằng kết thúc regex `$` và tách hàm escape regex.

## Kiến trúc hiện tại

`canonical 4 chunks` → `API normalize` → `runtime fetch Chuối schedule` → `append dynamic matches` → `filter finished` → `M3U response`

Như vậy việc GitHub Actions tạm thời không chạy không còn làm mất khả năng cập nhật trận Chuối Chiên tại API.

## Nguồn lịch hiện tại
Ngày 17/09/2026, trang lịch công khai đang có các trận hôm nay và ngày mai; các trận có BLV Chuối được ánh xạ tự động. Parser không cần lấy playlist bên thứ ba làm baseline.

## Vercel
Project production dự kiến: `byvn-m3u-proxy`
Endpoint: `https://byvn-m3u-proxy.vercel.app/api/nm7-private.m3u`

Trong phiên hiện tại connector Vercel chưa expose team/project (`teams: []`), nên chưa thể xác nhận deployment production đã nhận các commit mới. Cần deploy qua Git integration hoặc cấp lại quyền project Vercel.

## Kiểm tra bắt buộc sau deploy
1. HTTP 200 tại `/api/nm7-private.m3u`.
2. Response bắt đầu bằng `#EXTM3U`.
3. Có đủ 12 `group-title` canonical.
4. Có ít nhất 424 playable entries trước/không làm suy giảm canonical inventory.
5. Có entry động `Chuối Chiên TV` chứa giờ + DD/MM của lịch hiện tại.
6. Khi trận quá 180 phút sau giờ bắt đầu, entry động được lọc khỏi response.
7. Khi trang lịch Chuối Chiên lỗi, API vẫn trả canonical playlist.

## Ràng buộc
- Không sửa APK.
- Không dùng `superok-live-private` hoặc `byvn.net`.
- Không bypass token, DRM, authentication hoặc access control.
- Không thay canonical 4 chunks chỉ để sửa lỗi runtime.
