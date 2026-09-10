# Tiến độ dự án NM7 IPTV

_Cập nhật: 10/09/2026_

## Trạng thái hiện tại

Dự án Android đã được tách thành **2 bản phát hành độc lập từ cùng mã nguồn** để tiếp tục phát triển riêng theo thiết bị:

- **NM7 IPTV Mobile** — bản nhẹ, không đóng gói LibVLC.
- **NM7 IPTV TV** — bản dành cho Android TV, có LibVLC/FFmpeg fallback để xử lý các luồng 4K/codec mà MediaCodec của TV không phát được.

Phiên bản candidate hiện tại của cả hai nhánh sản phẩm là **1.10.15** (`versionCode 32`). Nhánh mã nguồn chính: `main`.

Mốc thực tế quan trọng:

- **TV 1.10.13** đã được người dùng xác nhận chạy tốt trên TV thật và phát được kênh 4K sau khi bổ sung LibVLC fallback.
- **1.10.14** bắt đầu tách Mobile/TV nhưng phát sinh regression khi mở một số nguồn: TV có thể gặp `ERROR_CODE_IO_BAD_HTTP_STATUS`; kênh HBO/ClearKey lỗi trên cả Mobile và TV.
- **1.10.15** là bản sửa regression hiện tại, đang chờ kiểm thử thực tế lại trên Mobile và TV.

## Kiến trúc build Mobile / TV

`app/build.gradle.kts` hiện dùng product flavor `device`:

### Mobile

- Flavor: `mobile`.
- Version: `1.10.15-mobile`.
- Không đóng gói `libvlc.so`.
- Media3/ExoPlayer là engine phát chính.
- Giữ dung lượng APK nhỏ, phù hợp điện thoại/tablet.

### TV

- Flavor: `tv`.
- Version: `1.10.15-tv`.
- Có `tvImplementation("org.videolan.android:libvlc-all:3.6.1")`.
- Media3/ExoPlayer vẫn là engine chính.
- Khi codec/MediaCodec TV không phát được nguồn phù hợp, có thể chuyển sang LibVLC/FFmpeg fallback.

## Giao diện / nhận diện 1.10.15

- Tên ứng dụng chuẩn hóa thành **NM7 IPTV**.
- Phần chữ tiêu đề `NM7 IPTV` ở đầu giao diện chính đã được thay bằng **logo ứng dụng** trên cả Mobile và TV.
- Logo hiện dùng `@mipmap/ic_launcher`, căn giữa trong vùng header.
- TV tiếp tục giữ hàng chức năng gọn: `Tất cả | ★ Yêu thích | ◷ Gần đây | ⚙ Tùy chọn`.
- TV đã bỏ các dòng trạng thái dư ở đầu màn hình như `Mở playlist để bắt đầu` và `Nguồn: chưa mở playlist`.

## NM7 IPTV Mobile — trạng thái chức năng

Các phần đã triển khai:

- Player inline nằm phía trên danh sách kênh.
- Chọn kênh khác tiếp tục phát trong player inline, không bật PlayerActivity cũ.
- Controller mặc định ẩn; chạm vào video mới hiện Play/Pause và seek/DVR khi nguồn hỗ trợ.
- Fullscreen/phóng to/thu nhỏ bằng biểu tượng cạnh bánh răng.
- Xoay ngang/dọc và quay lại màn hình danh sách.
- Hiển thị độ phân giải thực tế và FPS thực tế.
- Giữ màn hình sáng khi đang xem.
- Hỗ trợ phát nền theo tùy chọn ứng dụng.
- Tìm kiếm không còn bị bàn phím che ô nhập.
- Vuốt ngang trên danh sách để đổi nhóm kênh bằng `GestureDetector`; cuộn dọc/tap/fling vẫn do `ListView` xử lý.
- Bỏ long-press mở hộp `Phát / Yêu thích`; yêu thích dùng biểu tượng ngôi sao riêng.
- Danh sách kênh đã thu gọn card/logo/khoảng cách so với các bản đầu.
- Hộp `Tùy chọn ứng dụng` dùng nền bán trong suốt.
- Bổ sung gesture trên player Mobile:
  - Vuốt dọc cạnh trái: chỉnh độ sáng.
  - Vuốt dọc cạnh phải: chỉnh âm lượng.
  - Có phản hồi phần trăm trên màn hình khi điều chỉnh.
- Tác vụ lưu playlist/session lớn đã được chuyển khỏi UI thread để giảm nguy cơ ANR khi thêm hoặc chuyển playlist.

### Việc đang kiểm thử trên Mobile 1.10.15

- Kiểm tra lại thêm/chọn/chuyển playlist với playlist lớn, bảo đảm không còn treo giao diện.
- Kiểm tra kênh HBO/ClearKey sau thay đổi parser DRM.
- Kiểm tra gesture sáng/âm lượng không xung đột với controller/seek của player inline.
- Xác nhận logo header hiển thị cân đối trên nhiều kích thước màn hình.

## NM7 IPTV TV — trạng thái chức năng

Các phần đã triển khai:

- Ứng dụng đã khởi động được trên TV thật sau khi sửa crash layout-land ở 1.10.9.
- Header TV được thu gọn, các nút Link/Nguồn/Thông tin đã đưa vào Tùy chọn thay vì chiếm chỗ ngoài màn hình chính.
- Hàng chức năng `Tất cả / Yêu thích / Gần đây / Tùy chọn` nằm cùng một hàng.
- Danh sách kênh TV đã giảm chiều cao card, logo và divider để hiển thị được nhiều kênh hơn.
- Playlist loader TV có timeout dài hơn và retry để giảm lỗi tải playlist tạm thời.
- Media3 có decoder fallback và các tầng phục hồi 1080p/720p.
- Từ 1.10.13, TV có **LibVLC/FFmpeg fallback**, đã được người dùng xác nhận phát được kênh 4K mà MediaCodec trước đó báo `ERROR_CODE_DECODING_FAILED`.
- 1.10.15 bổ sung hướng xử lý lỗi HTTP của luồng TV: nguồn không DRM gặp `IO_BAD_HTTP_STATUS` có thể được chuyển sang VLC fallback thay vì dừng ngay ở Media3.

### Việc đang kiểm thử trên TV 1.10.15

- Kiểm tra lại các kênh 4K đã chạy tốt ở 1.10.13, đặc biệt ASTRO/Eleven Sports 4K.
- Kiểm tra trường hợp `ERROR_CODE_IO_BAD_HTTP_STATUS` xuất hiện ở 1.10.14.
- Kiểm tra kênh HBO/ClearKey sau thay đổi parser DRM.
- Xác nhận D-pad/focus vẫn rõ sau khi thay chữ tiêu đề bằng logo.

## DRM / ClearKey 1.10.15

- `DrmPlayback` vẫn hỗ trợ Widevine, ClearKey và PlayReady theo metadata do playlist/nhà cung cấp cung cấp.
- Không tự tìm, suy đoán hoặc hiển thị khóa DRM.
- Parser ClearKey đã được nới ở lớp nhận dạng để không loại sớm các phản hồi hợp lệ từ nhà cung cấp trước khi `DrmPlayback` thực hiện bước chuẩn hóa/kiểm tra thực tế.
- Mục tiêu sửa regression HBO là giữ đúng KID/KEY do nguồn cung cấp nhưng chấp nhận thêm các cấu trúc JSON/JWK hợp lệ thường gặp.

## Build / CI hiện tại

GitHub Actions run hiện tại: **Build #169**, run ID `34439906587`, commit `0c98bf6656226e7121966f10d560dc802479c445`.

Các bước đã PASS:

1. Checkout/JDK/Gradle/Android SDK.
2. Compile cả Mobile và TV.
3. Unit test cả Mobile và TV.
4. Android lint cả Mobile và TV.
5. Assemble APK cả hai flavor.
6. Kiểm tra chữ ký APK.
7. Xác nhận Mobile APK không chứa LibVLC.
8. Xác nhận TV APK có LibVLC.
9. Upload artifact Mobile và TV.

Artifact hiện tại:

- `NM7-IPTV-Mobile-1.10.15-APK` — artifact ID `10137653429`.
- `NM7-IPTV-TV-1.10.15-APK` — artifact ID `10137656327`.

Bước **Android 15 Mobile smoke test của Build #169 bị fail** sau khi APK đã build/package thành công. Chưa được đánh dấu PASS toàn bộ cho tới khi phân tích/fix smoke test hoặc xác nhận đây là lỗi môi trường emulator. Vì vậy **1.10.15 hiện là candidate, chưa phải stable**.

## Mốc phát triển tiếp theo

### Mobile

- Ưu tiên xác nhận sửa ANR khi đổi playlist.
- Xác nhận HBO/ClearKey.
- Hoàn thiện gesture sáng/âm lượng và giữ APK nhẹ.
- Tiếp tục tối ưu danh sách/gesture nhóm nếu có phản hồi thực tế.

### TV

- Giữ nền tảng phát 4K của 1.10.13 làm chuẩn không được regression.
- Xác nhận 1.10.15 xử lý được lỗi HTTP status của các kênh 4K.
- Xác nhận HBO/ClearKey.
- Không đưa LibVLC sang Mobile trừ khi có yêu cầu riêng.

## Quy ước từ thời điểm này

- Mọi bản phát hành tiếp theo phải tạo **2 APK riêng**: Mobile và TV.
- Thay đổi dành riêng cho TV không được làm tăng đáng kể dung lượng Mobile.
- LibVLC/FFmpeg là thành phần TV-only.
- Tính năng Mobile inline/gesture tiếp tục phát triển riêng nhưng dùng chung phần parser/DRM/network khi phù hợp.
- Khi một thay đổi ảnh hưởng phần dùng chung, cần kiểm thử cả Mobile và TV trước khi nâng candidate thành stable.
