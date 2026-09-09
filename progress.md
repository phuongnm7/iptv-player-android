# Tiến độ dự án Nm7 IPTV Player

_Cập nhật: 09/09/2026_

## Trạng thái hiện tại: Android 1.8.0 ổn định, Android 1.8.2 chờ kiểm thử quản lý nguồn; Samsung Tizen tạm dừng

### Android

- Bản ổn định đã được người dùng xác nhận: **Nm7 IPTV 1.8.0** (`versionCode 13`), gồm tính năng phát nền trên Mobile.
- Bản thử nghiệm mới: **Nm7 IPTV 1.8.2** (`versionCode 15`), sửa giao diện quản lý nguồn với nút thao tác hiển thị trực tiếp.
- GitHub Actions 1.8.2: **Build #67**, run ID `34332032653`; compile, unit test, lint, ký APK và smoke test Android 15 đều PASS.
- Artifact 1.8.2: `Nm7-IPTV-1.8.2-APK`, ID `10096242357`.
- Commit tính năng: `2b5cf3fa1d09a3f31db76ac414046134e053eb65`.
- Cần kiểm thử thực tế các thao tác: thêm nguồn, sửa tên/URL, tải nguồn, xóa một nguồn và xóa nguồn cuối cùng.

### Samsung Tizen TV

- Trạng thái: **TẠM DỪNG theo yêu cầu người dùng vì hiện chưa có TV Samsung để kết nối và thử**.
- Mục tiêu: TV Samsung đời 2015–2020, Tizen 2.3–5.5.
- Phiên bản mã nguồn hiện tại: `0.1.0`, nằm trong thư mục `tizen/`.
- Commit mã/CI đã kiểm tra: `9350de76338aca53d4e33c49a605a090235269cb`.
- GitHub Actions Tizen: **Build #2**, run ID `34323731285`, PASS.
- Artifact nguồn: `Nm7-IPTV-Tizen-0.1.0-source`, ID `10092880456`.
- Đây chưa phải gói `.wgt` có thể cài lên TV thật.

## Phần Tizen đã hoàn thành

- Tizen Web App dùng JavaScript ES5 để tương thích TV đời cũ.
- Phát HLS/DASH bằng Samsung Product AVPlay.
- Tải và ghi nhớ playlist M3U.
- Parser M3U hỗ trợ logo, nhóm, User-Agent và Referer.
- Danh sách kênh, nhóm, tìm kiếm, Yêu thích và Gần đây.
- Điều khiển D-pad, danh sách nhanh và hành vi Back hai bước trong màn hình phát.
- Tự nối lại luồng tối đa 4 lần.
- Cài đặt cơ bản và icon Nm7.
- CI kiểm tra cú pháp JavaScript, unit test parser, XML cấu hình, tệp bắt buộc và đóng gói nguồn.

## Môi trường trên máy Windows đã chuẩn bị

- Đã cài Tizen Studio Web CLI/SDK 10.0.
- Đã sửa Package Manager repository sang:
  `https://download.tizen.org/sdk/tizenstudio`
- Đã cài `TV Extensions-10.0`.
- Đã cài `Web app. development`.
- Đã cài `Samsung Certificate Extension`.
- `Samsung Wearable Extension` cũng được cài nhưng không ảnh hưởng dự án.

## Bước tiếp tục khi có TV Samsung

1. Cho TV và máy tính kết nối cùng mạng Wi-Fi/LAN.
2. Trên máy tính chạy `ipconfig` và lấy IPv4.
3. Trên TV mở Apps, bấm `1 2 3 4 5`, bật Developer Mode và nhập IPv4 của máy tính.
4. Khởi động lại TV.
5. Mở Tizen Device Manager, kết nối tới IP của TV qua cổng 26101.
6. Mở Samsung Certificate Manager, tạo Samsung TV Certificate Profile và thêm DUID của TV.
7. Import artifact nguồn Tizen vào Tizen Studio.
8. Build và ký gói `.wgt`.
9. Cài gói lên TV qua mạng bằng Tizen Studio/Device Manager.
10. Chạy smoke test thực tế: tải playlist, điều khiển remote, phát HLS/DASH, chuyển kênh, Back hai bước và tự nối lại.
11. Ghi nhận lỗi theo từng đời TV rồi mới đánh dấu bản Tizen hoàn tất.

## Sửa giao diện quản lý nguồn trên Android 1.8.2

- Sau phản hồi thực tế, bỏ thao tác ẩn khi chạm vào cả dòng nguồn.
- Mỗi nguồn hiển thị trực tiếp ba nút `Chọn`, `Sửa`, `Xóa`.
- Nút `Thêm nguồn` luôn hiển thị ở cuối hộp thoại.
- Danh sách nguồn nằm trong vùng cuộn để vẫn dùng được khi có nhiều nguồn.

## Những thay đổi mới của Android 1.8.1

- Nút `Link` mở màn hình `Quản lý nguồn IPTV`.
- Có thể thêm nguồn mới với tên và URL; tên có thể để trống để tự lấy tên miền.
- Chọn một nguồn để `Tải nguồn`, `Chỉnh sửa` hoặc `Xóa`.
- Kiểm tra URL phải bắt đầu bằng `http://` hoặc `https://` trước khi lưu.
- Cho phép danh sách nguồn trống sau khi xóa nguồn cuối cùng; nguồn mặc định chỉ được tạo ở lần cài sạch.
- Giao diện hộp thoại dùng được trên Mobile và bằng D-pad trên Android TV.

## Những thay đổi mới của Android 1.8.0

- Thêm lựa chọn bật/tắt phát nền khi khóa màn hình hoặc nhấn Home; mặc định **tắt**.
- Tùy chọn chỉ xuất hiện trên giao diện Mobile, không xuất hiện và không chạy trên giao diện TV.
- Khi bật, ứng dụng dùng foreground service có thông báo và giữ kết nối mạng để tiếp tục phát.
- Khi quay lại trình phát hoặc tắt tùy chọn, dịch vụ chạy nền được dừng.
- Android 13 trở lên yêu cầu quyền thông báo khi bật tính năng.

## Những thay đổi chính của Android 1.7.3

- Bỏ thống kê và dòng nguồn khỏi màn hình chính.
- Ô URL trống khi bấm `+ Nguồn`.
- Tự nhận diện HLS/DASH sau chuyển hướng.
- Tự nối lại khi playlist kết thúc hoặc gặp lỗi mạng tạm thời.
- Trên TV/Android: Back lần đầu ẩn controller, lần tiếp theo rời màn hình phát.
- Đã bỏ các nút `Chọn đang lọc`, `Bỏ chọn`, `Xuất M3U`.
- Khôi phục menu cài đặt và thêm icon/banner Nm7.

## Mốc tiếp tục

- Android: 1.8.0 / Build #65 là mốc ổn định; tiếp tục kiểm thử quản lý nguồn trên 1.8.2 / Build #67.
- Tizen: tiếp tục từ commit `9350de76338aca53d4e33c49a605a090235269cb`, Build #2.
- Điều kiện mở lại phần Tizen: có TV Samsung thật tại chỗ để lấy IP/DUID, ký, cài và kiểm thử.
