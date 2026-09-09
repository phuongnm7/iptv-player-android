# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 09/09/2026_

## Commit mới nhất
- `beba7b5`: sửa remote seek: controller hiện thì LEFT/RIGHT được chuyển cho Media3 để tua; controller ẩn thì LEFT mở danh sách nhanh.
- `b9d4044`: thêm cơ chế cài mới tự đặt URL IPTV mặc định và bắt đầu tải playlist; loại bỏ luồng export M3U cũ khỏi `MainActivity`.

## Yêu cầu đã triển khai
- Nguồn mặc định: `https://iptv-live-merge.phuongnm7-iptv.workers.dev/playlist.m3u`.
- Có phiên trước: khôi phục phiên đó.
- Không có phiên trước: tự tải nguồn mặc định.
- Giữ khả năng nhập nguồn khác thủ công.
- Không còn luồng xuất M3U trong `MainActivity`.
- Logo/icon kênh đã được thêm ở các commit trước.

## Remote
- Controller ẩn: OK hiện controller; LEFT mở danh sách nhanh; UP/DOWN đổi kênh.
- Controller hiện: LEFT/RIGHT dành cho thao tác tua của Media3.

## Bắt buộc kiểm tra tiếp
1. Chờ/kiểm tra CI cho commit `b9d4044` trước khi phát hành APK.
2. Smoke test cài mới và xác nhận playlist mặc định tự tải.
3. Smoke test remote trên nguồn có DVR/seek window.
4. Nếu build báo lỗi, ưu tiên sửa lỗi do việc dọn `MainActivity` rồi chạy lại toàn bộ build/lint/smoke test.

**Không bàn giao APK mới cho đến khi CI xác nhận.**
