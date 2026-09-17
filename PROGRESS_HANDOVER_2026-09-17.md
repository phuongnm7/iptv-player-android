# NM7 IPTV – TIẾN ĐỘ BÀN GIAO

**Cập nhật:** 17/09/2026
**Repo:** `phuongnm7/iptv-player-android`
**Nhánh:** `main`

## 1. Mục tiêu hiện tại

Không sửa APK ở giai đoạn này.

Mục tiêu đang xử lý là chuỗi:

`GitHub playlist → Vercel byvn-m3u-proxy → API /api/nm7-private.m3u`

Ứng dụng Android TV đã có URL nguồn cố định; playlist phải tự cập nhật trận mới và loại trận cũ, không yêu cầu người dùng tải/upload M3U thủ công.

## 2. Playlist gốc

File người dùng cung cấp: `New Text Docum1ent (3)(1).txt`

- 684 EXTINF ban đầu.
- 566 entry có URL phát.
- 176 URL stream duy nhất.
- Sau dedupe theo `header + URL`: **424 entry phát được**.

12 nhóm sau khi khôi phục/dedupe:

| Nhóm | Entry |
|---|---:|
| Vua Sân Cỏ TV | 25 |
| Chuối Chiên TV | 10 |
| Giờ Vàng TV | 27 |
| Bia Ôm TV | 26 |
| Xôi Lạc Z TV | 25 |
| Sao Kê TV | 15 |
| Gà Vàng 33 TV | 42 |
| Cola TV | 117 |
| S8 TV | 32 |
| Gà Vàng TV | 24 |
| Khán Đài TV | 44 |
| Socolive TV | 37 |
| **Tổng** | **424** |

## 3. Khôi phục dữ liệu 4 chunk trên GitHub

Các chunk hiện đã được khôi phục trên `main`:

- `playlist/nm7-private.part1.b64`
  - SHA: `62c33e2275e896a2d184c4d0e9f74750a7f949c8`
  - commit: `443ec7d088de930864ca900850c5b35def68a26a`
- `playlist/nm7-private.part2.b64`
  - SHA: `c21903a17835621d945f5a32b9ef0858558f6737`
  - commit: `7e3e44b8d74173a4bdcb056bd6f6121562cdd73d`
- `playlist/nm7-private.part3.b64`
  - SHA: `1342925e1918dbe5348d1b520d5c96f7ce38bf39`
  - commit: `b09fcda20b71858f3f924b60a9831f046a31e091`
- `playlist/nm7-private.part4.b64`
  - SHA: `92d6488ef50a8604b4a75acf4fd4c8ec2e6ce6ab`
  - commit: `e2c702bda13637e5bf38ff9b87d0f3dbb7ca923a`

Latest subsequent workflow-protection commit:
- `05a4ddc040ec3cf980bee43739472827933a8637`

## 4. Vấn đề từng xảy ra

Workflow cũ đã có lần lấy một baseline playlist bị thu nhỏ và tạo kết quả:

- preserved inventory: 262
- fresh dynamic matches: 3
- final entries: 265

Điều này làm playlist production chỉ còn một số nhóm (người dùng nhìn thấy 4 nhóm). Nguyên nhân nằm ở chuỗi cập nhật playlist, không phải yêu cầu sửa APK.

## 5. Workflow hiện tại

File:
`.github/workflows/update-nm7-playlist.yml`

Workflow:
- chạy schedule mỗi 10 phút;
- lấy lịch công khai Chuối Chiên từ `https://chuoichientv.link/lich-thi-dau/`;
- giữ inventory hiện có;
- thay các entry `Chuối Chiên TV` bằng lịch mới lấy được;
- đóng gói M3U thành 4 chunk base64/gzip;
- validate trước khi commit;
- chỉ commit khi playlist thay đổi.

### Bảo vệ đã bổ sung

Workflow đã được sửa để không tiếp tục chấp nhận playlist bị thu nhỏ. Cần duy trì kiểm tra tối thiểu:
- đủ **12 nhóm**;
- tối thiểu **424 entry phát được**;
- nếu không đạt thì job phải fail và **không commit playlist**.

Lưu ý: workflow hiện tại mới tự động tạo dynamic match cho **Chuối Chiên TV**. 11 nhóm còn lại đang được giữ từ inventory. Tự động hóa lịch/nguồn cho cả 12 nhóm vẫn là phần công việc tiếp theo.

## 6. Vercel

Project cần dùng:
- `byvn-m3u-proxy`
- production endpoint dự kiến:
  `https://byvn-m3u-proxy.vercel.app/api/nm7-private.m3u`

**Không dùng:** `superok-live-private`, `byvn.net`.

File API:
- `api/nm7-private.m3u.js`
- API đọc 4 chunk, giải nén, chuẩn hóa M3U, dedupe và có xử lý lọc entry đã kết thúc.

`vercel.json` đã cấu hình `includeFiles: playlist/*.b64` cho API và no-cache headers cho `/api/nm7-private.m3u`.

### Trạng thái Vercel chưa hoàn tất

Kết nối Vercel trong phiên làm việc hiện tại vẫn không expose được team/project:
- `list_teams` trả `teams: []`.
- Truy cập production URL qua connector đã trả `403 Forbidden`.
- Vì vậy **chưa có bằng chứng xác nhận production endpoint hiện đang trả đủ 12 nhóm**.

Đây là blocker hiện tại, không phải APK.

## 7. GitHub Actions

Một run trước đó đã hoàn tất thành công tất cả các bước:
- Checkout
- Fetch current public match schedule
- Rebuild dynamic matches
- Pack playlist
- Validate
- Commit/push

Run tham chiếu trước khi bổ sung protection:
- run #12
- run id `35188774704`
- commit `b799c680f925064be256e78bf4b8bc472de5c5ea`
- job id `105096488302`

## 8. Android TV – không phải phần đang sửa

Android parser hiện không hardcode 4 nhóm. Parser đọc `group-title`/`#EXTGRP` và tạo nhóm động từ playlist trả về. Do đó việc người dùng chỉ thấy 4 nhóm được điều tra ở tầng playlist/API, không chuyển sang sửa APK.

## 9. Công việc tiếp theo cho người bàn giao

### Ưu tiên 1 – Vercel
1. Cấp lại quyền truy cập project `byvn-m3u-proxy` cho công cụ/agent làm việc.
2. Xác nhận deployment production lấy đúng `main` GitHub.
3. Gọi:
   `https://byvn-m3u-proxy.vercel.app/api/nm7-private.m3u`
4. Kiểm tra kết quả có đủ 12 `group-title` và ít nhất 424 entry phát được.

### Ưu tiên 2 – Không để inventory suy giảm
- Giữ protection 12 nhóm / 424 playable entries.
- Tốt hơn nữa: tách **canonical static inventory** khỏi **dynamic match entries**, thay vì lấy chính playlist hiện tại làm baseline mỗi lần chạy.

### Ưu tiên 3 – Tự động hóa 12 nhóm
Mục tiêu cuối:
- lấy lịch công khai của từng provider;
- ánh xạ trận → nguồn stream hợp lệ/public;
- tạo entry mới khi có trận;
- loại entry trận cũ;
- giữ các nhóm khác nguyên vẹn;
- không bypass token, DRM, authentication hoặc access control.

## 10. Nguồn công khai đã khảo sát

Chuối Chiên:
- `https://chuoichientv.link/lich-thi-dau/`
- `https://chuoichientv1.com/`

Socolive:
- `https://www.socolive-wap.com/`

Các URL stream phát hiện trong nguồn công khai chỉ được dùng để discovery/đối chiếu; không coi playlist bên thứ ba là bằng chứng quyền sử dụng và không bypass bảo vệ stream.

## 11. Ghi chú bàn giao

**Trạng thái:** GitHub playlist/inventory đã được khôi phục và workflow đã có cơ chế chống mất nhóm. Vercel production chưa được xác nhận do quyền Vercel chưa được expose trong phiên hiện tại.

**Không sửa APK ở giai đoạn này.**

Người tiếp nhận nên bắt đầu từ mục **9.1 – Vercel**, sau đó kiểm tra API thực tế trước khi thay đổi thêm code.