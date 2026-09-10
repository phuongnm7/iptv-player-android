# Tiến độ dự án NM7 IPTV

_Cập nhật: 10/09/2026_

## Trạng thái hiện tại

Dự án Android tiếp tục phát hành **2 APK riêng từ cùng mã nguồn**:

- **NM7 IPTV Mobile** — bản nhẹ, không đóng gói LibVLC.
- **NM7 IPTV TV** — bản Android TV, có LibVLC/FFmpeg fallback cho các trường hợp MediaCodec/Media3 không phát được.

Phiên bản Android hiện tại là **1.10.17** (`versionCode 34`), nhánh chính `main`.

Mốc ổn định thực tế:

- **TV 1.10.13** đã được xác nhận chạy tốt trên TV thật, bao gồm kênh 4K từng lỗi MediaCodec.
- **1.10.15** sửa regression HTTP/ClearKey sau khi tách flavor Mobile/TV.
- **1.10.16** bổ sung highlight kênh đang phát trên Mobile và chỉnh lại controller/player UI riêng cho TV.
- **1.10.17** bổ sung cơ chế Reload playlist luôn lấy dữ liệu mới, thay logo giao diện chính bằng logo **Phuongnm7 TV nền trong suốt**, và đã đóng gói lại cả Mobile/TV.

## Kiến trúc Mobile / TV

`app/build.gradle.kts` dùng flavor dimension `device`.

### Mobile

- Flavor: `mobile`.
- Version: `1.10.17-mobile`.
- Không chứa `libvlc.so`.
- Media3/ExoPlayer là engine phát chính.
- Player inline trên màn hình danh sách.
- APK giữ dung lượng nhỏ cho điện thoại/tablet Android.

### TV

- Flavor: `tv`.
- Version: `1.10.17-tv`.
- Có `tvImplementation("org.videolan.android:libvlc-all:3.6.1")`.
- Media3/ExoPlayer là engine chính.
- LibVLC/FFmpeg là fallback riêng cho TV.

## Giao diện / nhận diện 1.10.17

- Tên ứng dụng: **NM7 IPTV**.
- Logo trên màn hình chính đã đổi sang **Phuongnm7 TV**.
- File logo dùng nền trong suốt, chỉ giữ biểu tượng và chữ.
- Màn hình Mobile portrait hiển thị logo lớn hơn để dễ nhìn.
- Layout ngang/TV dùng logo gọn hơn để không chiếm nhiều chiều cao.
- Logo giao diện chính dùng `@drawable/nm7_main_logo`, không còn dùng launcher icon làm header.

## NM7 IPTV Mobile — trạng thái chức năng

Đã triển khai:

- Player inline phía trên danh sách kênh.
- Chọn kênh khác tiếp tục phát trong player inline.
- Controller mặc định ẩn; chạm video mới hiện điều khiển.
- Fullscreen/phóng to/thu nhỏ.
- Xoay ngang/dọc.
- Hiển thị độ phân giải và FPS thực tế.
- Giữ màn hình sáng khi xem.
- Phát nền theo tùy chọn.
- Tìm kiếm, nhóm kênh, Yêu thích, Gần đây.
- Vuốt ngang danh sách để đổi nhóm.
- Vuốt dọc cạnh trái player để chỉnh độ sáng.
- Vuốt dọc cạnh phải player để chỉnh âm lượng.
- Có overlay phần trăm khi chỉnh sáng/âm lượng.
- Lưu playlist/session lớn ngoài UI thread để giảm nguy cơ ANR.
- **Highlight rõ dòng kênh đang phát** trong danh sách; đổi kênh thì highlight chuyển theo kênh mới.

## NM7 IPTV TV — trạng thái chức năng

Đã triển khai:

- D-pad/focus dành riêng cho TV.
- Danh sách nhóm/kênh tối ưu cho điều khiển TV.
- Quick channel list bằng phím điều hướng theo thiết kế hiện tại.
- Media3 decoder fallback và các tầng phục hồi 1080p/720p.
- LibVLC/FFmpeg fallback cho TV, đã được xác nhận phát được nguồn 4K từng lỗi decoder.
- Nguồn không DRM gặp lỗi HTTP phù hợp có thể chuyển sang VLC fallback.
- Khi vừa mở kênh, controller không cần hiện ngay.
- Bấm **OK/Enter** mới hiện controller.
- Phần overlay trên/dưới của controller TV được làm trong suốt hơn, tránh lớp nền mờ che video.
- Các chức năng **Định dạng / Chất lượng / Khung hình** được gom vào khu vực bánh răng ở góc dưới bên phải.

## Reload playlist / chống cache 1.10.17

Mục tiêu của 1.10.17 là khi người dùng bấm **Tải lại / Reload**, app không dùng lại bản playlist cũ do cache.

Đã triển khai:

- `HttpURLConnection.setUseCaches(false)`.
- Gửi `Cache-Control: no-cache, no-store, max-age=0`.
- Gửi `Pragma: no-cache`.
- Với `raw.githubusercontent.com`, mỗi lần tải tạo URL request mới bằng tham số timestamp `_nm7_reload=<thời gian>`.
- URL gốc người dùng lưu trong app vẫn giữ nguyên; tham số chống cache chỉ được thêm ở request thực tế.

Cơ chế này giúp app lấy **bản mới nhất đang có trên GitHub Raw** mỗi lần Reload. Nếu file GitHub trung gian chưa đồng bộ với nguồn gốc thì app không thể vượt trước dữ liệu đang tồn tại trên GitHub; việc đồng bộ nguồn gốc vẫn do updater của repo playlist đảm nhiệm.

## DRM / ClearKey

- `DrmPlayback` tiếp tục hỗ trợ Widevine, ClearKey và PlayReady theo metadata do playlist/nhà cung cấp cung cấp.
- Không tự tìm, suy đoán hoặc hiển thị khóa DRM.
- Parser ClearKey đã được nới để chấp nhận các cấu trúc hợp lệ phổ biến hơn trước bước chuẩn hóa thực tế.
- Các thay đổi giao diện/logo/reload 1.10.16–1.10.17 không thay đổi pipeline DRM/decoder đã ổn trước đó.

## Build / CI Android 1.10.17

Build đóng gói hiện tại:

- **Build #186**
- Run ID: `34472031833`
- Head commit build: `499d173fe8eaa20e4d9e777023b5c2bdb4ddc2cb`

Các bước đã PASS:

1. Checkout/JDK/Gradle/Android SDK.
2. Compile Mobile và TV.
3. Unit test Mobile và TV.
4. Android lint Mobile và TV.
5. Assemble cả hai flavor.
6. Kiểm tra chữ ký APK.
7. Xác nhận Mobile không chứa LibVLC.
8. Xác nhận TV có LibVLC.
9. Upload artifact Mobile và TV.
10. Upload test/lint reports.

Artifact 1.10.17:

- `NM7-IPTV-Mobile-1.10.17-APK` — artifact ID `10149993362`.
- `NM7-IPTV-TV-1.10.17-APK` — artifact ID `10149997794`.

### Trạng thái smoke test

**Android 15 Mobile smoke test của Build #186 vẫn FAIL**, giống vấn đề smoke-test ở các build gần đây. Vì compile/test/lint/package/sign đều PASS nhưng smoke emulator chưa PASS, **1.10.17 vẫn nên được coi là candidate cho tới khi kiểm thử thực tế trên điện thoại và TV xác nhận ổn**.

Không được ghi CI là PASS toàn bộ khi smoke-test còn fail.

## Playlist SuperOK liên quan

Repo public `phuongnm7/Iptv` đang lưu:

`https://raw.githubusercontent.com/phuongnm7/Iptv/main/SuperOK_playlist.m3u`

Updater của playlist đã được sửa để đồng bộ nguồn thường xuyên hơn. Ứng dụng Android 1.10.17 bổ sung chống cache để mỗi lần Reload lấy bản hiện tại của link GitHub Raw thay vì giữ bản cũ ở phía app/CDN.

## iPhone / iPad

Phần iOS/iPadOS đã có mã nguồn riêng trong thư mục `ios/` và đã từng build bản **1.0.0 unsigned IPA**.

Trạng thái hiện tại: **TẠM DỪNG theo yêu cầu người dùng**. Không tiếp tục thay đổi iOS cho tới khi người dùng yêu cầu khởi động lại phần này.

## Mốc tiếp theo

### Mobile

- Kiểm thử 1.10.17 trên máy thật.
- Xác nhận highlight kênh đang phát hoạt động đúng khi đổi nhóm/tìm kiếm/Yêu thích/Gần đây.
- Kiểm tra Reload link GitHub Raw luôn nhận playlist mới nhất đang có trên repo.
- Tiếp tục theo dõi ANR khi dùng playlist lớn.

### TV

- Kiểm thử 1.10.17 trên TV thật.
- Xác nhận controller chỉ hiện sau OK/Enter như thiết kế.
- Xác nhận menu bánh răng Định dạng/Chất lượng/Khung hình hoạt động bằng D-pad.
- Xác nhận 4K/LibVLC fallback không regression.
- Kiểm tra Reload playlist bằng remote.

### CI

- Phân tích riêng lỗi Android 15 smoke-test để phân biệt lỗi app với lỗi emulator/test script.
- Không để lỗi smoke-test che mất trạng thái compile/test/lint/package/sign đã PASS.

## Quy ước tiếp tục phát triển

- Android luôn tạo **2 APK riêng: Mobile và TV**.
- LibVLC/FFmpeg tiếp tục là TV-only.
- Thay đổi riêng TV không được làm tăng đáng kể dung lượng Mobile.
- Thay đổi parser/DRM/network dùng chung phải kiểm thử cả Mobile và TV.
- Không regression khả năng phát 4K TV đã xác nhận từ 1.10.13.
- Khi thay logo/UI, giữ player/DRM/decoder tách biệt để tránh làm hỏng phần phát.
- Chỉ đánh dấu bản stable sau kiểm thử thực tế trên thiết bị phù hợp.
