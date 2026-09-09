# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 09/09/2026_

## Trạng thái hiện tại

Commit sửa lỗi mới nhất: `85642c6`.

### Đã xử lý trong lượt này
Build trước thất bại ở `MainActivity.java`. Đã sửa trực tiếp các lỗi:
- Thay `SimpleTextWatcher` không tồn tại bằng `TextWatcher` chuẩn Android.
- Sửa lời gọi `SessionStore.save()` đúng chữ ký: channels, source, duplicates, missing.
- Dùng `SessionStore.snapshot(allChannels)` trước khi lưu phiên.
- Sửa ID panel từ `importSection` sang `importPanel` đúng với `activity_main.xml`.
- Khôi phục quản lý nguồn IPTV bằng `PlaylistSourceStore`.
- Cài mới hoặc không có phiên hợp lệ: tự tải `PlaylistSourceStore.DEFAULT_URL`.
- Có phiên hợp lệ: khôi phục playlist trước đó, không ghi đè.

## Các thay đổi đã có từ trước
- Nguồn mặc định: `https://iptv-live-merge.phuongnm7-iptv.workers.dev/playlist.m3u`.
- Người dùng vẫn có thể thêm/chọn nguồn khác thủ công.
- Logo/icon kênh đã được thêm ở các commit trước.
- Các nút Chọn đang lọc, Bỏ chọn và Xuất M3U đã được bỏ khỏi giao diện chính.

## Remote
- Controller ẩn: OK hiện controller; LEFT mở danh sách nhanh; UP/DOWN đổi kênh.
- Controller hiện: LEFT/RIGHT không còn bị PlayerActivity chặn để Media3 có thể xử lý tua ngược/tua tới.

## Việc bắt buộc tiếp theo
1. Theo dõi CI/build của commit `85642c6`.
2. Nếu build PASS, lấy APK artifact.
3. Chạy smoke test Android 15.
4. Kiểm tra cài mới tự tải playlist mặc định.
5. Kiểm tra remote trên nguồn có DVR/seek window:
   - OK → LEFT tua ngược.
   - OK → RIGHT tua tới.
   - Controller tự ẩn → LEFT mở danh sách nhanh.
6. Chỉ bàn giao APK sau khi build hợp lệ.

## Lưu ý kỹ thuật
Live stream không có DVR hoặc seek window không thể tua về thời điểm dữ liệu không còn trong bộ đệm hoặc không được máy chủ cung cấp. Đây là giới hạn của nguồn phát, không phải chỉ riêng phím remote.
