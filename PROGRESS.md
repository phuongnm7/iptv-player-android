# BÀN GIAO HIỆN TẠI — NM7 IPTV MOBILE 1.10.23 (2026-09-13)

## Bản Mobile 1.10.23 — duy trì phiên live và loại bỏ nhấp nháy logo

- Phân tích ảnh/video máy thật của 1.10.22 cho thấy watchdog cũ chỉ dựa vào thời gian BUFFERING, reset lịch sử lỗi sau 8 giây phát và gọi prepare/recreate lặp lại; do đó hình thành vòng phát–tải–phát dù chức năng auto-play đã hoạt động.
- Chuyển HTTP Media3 sang OkHttp DataSource dùng connection pool 8 socket, giữ kết nối 5 phút, retryOnConnectionFailure và ping 20 giây. Manifest/segment tái sử dụng socket thay vì liên tục tạo kết nối mới.
- Watchdog theo dõi thêm tiến triển mạng từ bandwidth callback, buffered-ahead, trạng thái isLoading, vị trí phát và frame decoder. Luồng chậm nhưng còn nhận dữ liệu được chờ tối đa 35 giây; socket không tiến triển phục hồi sau 18 giây.
- Phục hồi phân cấp: lần đầu giữ Player/timeline/decoder và prepare lại; các lần sau mới tạo MediaSource/Player mới qua connection pool. Backoff tự động được giới hạn 0,4–7 giây và không dừng chờ người dùng bấm Play.
- Không reset lịch sử lỗi sau một đoạn phát ngắn; chỉ reset sau 2 phút phát liên tục, giúp lỗi CDN lặp lại được nâng cấp sang phương án phục hồi mạnh hơn.
- Bỏ seekToDefaultPosition vô điều kiện. Chỉ nhảy live edge khi luồng kết thúc hoặc live offset vượt 45 giây, giảm phát lặp đoạn cũ. Live target offset tăng từ 6 lên 10 giây.
- Sửa nháy logo: bỏ notifyDataSetChanged sau mỗi ảnh; một URL chỉ có một tác vụ tải và cập nhật đúng các Holder đang chờ. Giữ drawable nếu URL của hàng không đổi.
- Logo có cache RAM LRU 24 MB và cache ổ đĩa trong cache app; giới hạn mỗi ảnh 2 MB, timeout 5/8 giây.
- Version: `versionCode 40`, `versionName 1.10.23`; repo TV không bị thay đổi.
- Commit tính năng: `d48a84cd64d848c5259cc185dbc05f92ef105fb0`; workflow: `b768815250fd0d480a2a73f9f28a0961ae0e6e76`; sửa tương thích Media3: `454735411f92a39a855d2f5f4024b42804a150c4`.
- Build đầu run `34727897253` bị compiler chặn do Media3 1.11 không có getPlaybackError; đã sửa đúng API, không tạo/bàn giao APK từ run lỗi.
- Build bàn giao run `34728059143`, job `103645667798`: compile, unit test, lint, ký/đóng gói và smoke-test Mobile trên Android 15 đều **SUCCESS**.
- Artifact `NM7-IPTV-Mobile-1.10.23-APK`, ID `10309245086`, archive digest `sha256:d1a714f66863d428b547b1931e7c33cd7e2df20d7fb8501f935b86d52952754d`.
- APK: 7.723.539 bytes; SHA-256 `0b3755ce087f3920fadbe7c4489d410e28bce431ab548dca386cc24c3740becb`.
- Cần thử dài hạn trên điện thoại thật với đúng kênh nước ngoài/4K. Không thể bảo đảm nguồn máy chủ thiếu segment vẫn phát liên tục, nhưng app không còn restart chỉ vì BUFFERING còn tiến triển.

---

# BÀN GIAO HIỆN TẠI — NM7 IPTV MOBILE 1.10.22 (2026-09-13)

## Bản Mobile 1.10.22 — tự nối lại timeout, giảm giật và cache logo

- Đã phân tích video máy thật `216837.mp4`: luồng 4K rơi vào `ERROR_CODE_TIMEOUT`, FPS về 0; bấm Play tạo lại yêu cầu và phát tiếp. Nguyên nhân phía ứng dụng là nhánh lỗi tạm thời dừng hẳn sau khi dùng hết số lần retry.
- `ERROR_CODE_TIMEOUT`, lỗi kết nối/đọc, IO tạm thời và HTTP 401/403/408/429/5xx được coi là lỗi có thể phục hồi. Player tự tạo lại kết nối vô hạn với backoff giới hạn tối đa 7 giây, không còn yêu cầu người dùng bấm Play.
- Tắt spinner buffering mặc định trong khung video và bật `keepContentOnPlayerReset`; khi nối lại/chuyển nguồn, khung hình cuối được giữ thay vì hiện vòng tròn quay trên nền đen.
- Bộ đệm Mobile tăng thành 30–120 giây, bắt đầu ở 1,2 giây, sau rebuffer cần 5 giây và back-buffer 20 giây để ưu tiên ổn định cho kênh nước ngoài/4K.
- Cấu hình live target offset 6 giây và tốc độ bám live 0,97–1,03x, giúp quay về live edge mềm hơn và giảm nguy cơ phát lặp đoạn cũ.
- Logo kênh dùng cache RAM LRU 16 MB, chặn tải trùng cùng URL, tải song song tối đa 4 tác vụ và có timeout 5/8 giây; hàng tái sử dụng lấy bitmap từ cache nên không tải lại mỗi lần vuốt.
- Version: `versionCode 39`, `versionName 1.10.22`; repo TV không bị thay đổi.
- Commit mã: `21fdaf3853e614af9806021a903f2c59b4192f26`; commit workflow: `cdbc47a91bf7726d1f0a8b67f1db740a59f95b71`.
- Workflow run `34726563618`, job `103641603760`: compile, unit test, lint, ký/đóng gói APK và smoke-test Mobile trên Android 15 đều **SUCCESS**.
- Artifact `NM7-IPTV-Mobile-1.10.22-APK`, ID `10307104801`, archive digest `sha256:dbfac5a6b80428bb5f81a5d7e5290a821f97d85f5df344201e6c62ec74b50006`.
- APK: 7.173.560 bytes; SHA-256 `67288e8479e5d594827052aff9afcc33a03108f309228a67b36dd009244c77ad`; kiểm tra ZIP và `sha256sum -c` đều đạt.
- Cần thử dài hạn trên điện thoại thật bằng chính các kênh nước ngoài. CI xác nhận app không crash và luồng mẫu hoạt động nhưng không thể mô phỏng timeout/token/CDN của nguồn riêng.

---

# BÀN GIAO HIỆN TẠI — NM7 IPTV MOBILE 1.10.21 (2026-09-12)

## Bản Mobile 1.10.21 — phát hiện đứng hình thật và chuyển kênh nhanh hơn

- Phân tích video máy thật cho thấy Media3 có thể quay lại READY và vẫn có `playWhenReady=true` nhưng decoder không xuất thêm frame; FPS giữ 0.0 nên watchdog chỉ theo dõi BUFFERING của 1.10.20 không bắt được.
- Watchdog mới theo dõi trực tiếp `DecoderCounters.renderedOutputBufferCount` mỗi 2 giây. Nếu READY/đang phát/không bị suppression nhưng không có frame mới trong 8 giây, ứng dụng tự phục hồi.
- Khi phục hồi luồng live, Player luôn `seekToDefaultPosition()` để trở về live edge trước `prepare/play`, tránh giữ vị trí đã rơi khỏi cửa sổ HLS/DASH.
- Ngưỡng BUFFERING trước khi tự nối lại giảm từ 15 xuống 10 giây.
- Sửa lỗi callback pause bất đồng bộ: cờ lifecycle không bị xóa ngay sau `player.pause()`, tránh nhận nhầm pause do hệ thống thành thao tác Pause của người dùng rồi chặn auto-play.
- Bộ đệm cân bằng lại thành 20–90 giây, bắt đầu phát ở 750 ms và phát lại sau rebuffer ở 2,5 giây để chuyển kênh nhanh hơn nhưng vẫn giữ vùng đệm dài.
- Luồng đổi kênh cập nhật kênh/UI trước khi giải phóng Player cũ; bỏ các lệnh `stop()` và `clearMediaItems()` dư thừa trước `release()`.
- Version: `versionCode 38`, `versionName 1.10.21`.
- Commit sửa Player: `3d1ef78`; phát hành: `8b63538`; workflow: `b4c0c8b`.
- Workflow run `34724994307`, job `103637476574`: compile, unit test, lint, ký/đóng gói APK và smoke-test Mobile trên Android 15 đều **SUCCESS**.
- Artifact `NM7-IPTV-Mobile-1.10.21-APK`, ID `10307701954`, archive digest `sha256:43f34e99c893ac3f8b1fd9e74aa9dcef3f632cca67682c4575602b48578dd652`.
- APK: 7.173.562 bytes; SHA-256 `a5c19e97eaca26a3ca688fa05b3f051eb5527444e6a08ecaac36cd43474d9004`.
- Cần thử dài hạn trên điện thoại thật với chính nhóm kênh nước ngoài. Nếu vẫn đứng, thu `adb logcat` tại thời điểm đó để phân biệt decoder 4K, manifest/server HLS hoặc token/session nguồn.

---

# BÀN GIAO HIỆN TẠI — NM7 IPTV MOBILE 1.10.20 (2026-09-12)

## Bản Mobile 1.10.20 — ổn định phát và nút tải lại nhanh

- Chỉ thay đổi repo Mobile `phuongnm7/iptv-player-android`; repo TV không bị tác động.
- Sửa trường hợp đang xem bị chuyển sang pause ngoài ý muốn: Player phân biệt pause do người dùng với pause do lifecycle/hệ thống và tự tiếp tục khi `playWhenReady` bị mất nhưng không có playback suppression.
- Thêm watchdog kiểm tra mỗi 2 giây. Nếu luồng ở trạng thái buffering quá 15 giây, ứng dụng tự prepare/play hoặc tạo lại Player; tối đa 6 lần với backoff.
- Khi luồng báo ENDED ngoài ý muốn, ứng dụng tự nối lại từ vị trí live mặc định.
- Bộ đệm Mobile tăng từ 12–45 giây lên 25–90 giây; ngưỡng bắt đầu 1,5 giây, sau rebuffer 5 giây; giữ back-buffer 15 giây.
- Timeout kết nối/đọc tăng từ 20/35 giây lên 30/60 giây; số lần tải lại Media3 tăng từ 6 lên 8.
- Nút **↻ Tải lại** được đưa ra thanh chức năng, đặt ngay cạnh **★ Yêu thích** ở cả giao diện dọc và ngang; nút cũ trong bảng nhập nguồn đã được bỏ.
- Version: `versionCode 37`, `versionName 1.10.20`; User-Agent Mobile đồng bộ thành 1.10.20.
- Commit mã chính: `bd55dc5`; giao diện: `0b8d24d`, `54262d9`; phát hành: `3ebaf4f`; workflow: `a397156`.
- Workflow run `34709802587`, job `103596376811`: compile, unit test, lint, kiểm tra/đóng gói APK và smoke-test Mobile trên Android 15 đều **SUCCESS**.
- Artifact `NM7-IPTV-Mobile-1.10.20-APK`, ID `10302258664`, archive digest `sha256:21769088fc2468a4e441e90f0cb9a67144e56c31adc1289bd2f95fba4ce73699`.
- APK: 7.173.555 bytes; SHA-256 `f7f742623b1001fce94eced4ef6fd3626eaac42bcd4ce63b2aaa12402a68c237`.
- Cần kiểm tra trên điện thoại thật với chính các kênh nước ngoài trong thời gian dài. CI không dùng playlist/credential/DRM riêng nên không thể chứng minh chất lượng của máy chủ nguồn.

---

# BÀN GIAO HIỆN TẠI — NM7 IPTV MOBILE 1.10.19 (2026-09-12)

> Đây là mốc phải đọc trước khi tiếp tục. Repo chuẩn: `phuongnm7/iptv-player-android` (private), nhánh `main`.

## Bản Mobile 1.10.19 — nguồn mặc định riêng và khôi phục nguồn

- Chỉ thay đổi repo Mobile `phuongnm7/iptv-player-android`; không đọc, sửa hoặc kích hoạt workflow của repo TV `phuongnm7/nm7-tv-android`.
- Đổi nguồn tích hợp mặc định sang Worker mới do chủ dự án cung cấp.
- Nguồn mặc định không được lưu chung với danh sách nguồn tự thêm và URL không hiển thị trong màn hình quản lý nguồn.
- Màn hình quản lý nguồn luôn có mục **NM7 IPTV** với nút **Chọn nguồn mặc định**; khi đang dùng nguồn này, nút đổi thành **Đang sử dụng nguồn mặc định**.
- Khi xóa nguồn tự thêm đang được chọn, ứng dụng tự quay về nguồn mặc định và thông báo rõ cho người dùng.
- Có migration nhận diện URL mặc định cũ, kể cả phiên được cache trước khi nâng cấp, rồi chuyển sang nguồn mặc định mới.
- Thêm unit test cho URL mặc định mới, nhận diện nguồn mặc định/nguồn cũ và luồng migration.
- Version: `versionCode 36`, `versionName 1.10.19`; User-Agent được đồng bộ thành 1.10.19.
- Workflow chỉ build Mobile. Run `34674734089`, job `103502434843`: compile, unit test, lint, kiểm tra/đóng gói APK, cài và smoke-test Mobile trên Android 15 ở dọc/ngang đều **SUCCESS**.
- Artifact `NM7-IPTV-Mobile-1.10.19-APK`, ID `10291729098`, archive digest `sha256:2fea60f5328309fd8907908feaf644b2eb012723041d43f0fc5d4305a42063ef`.
- Các commit chính: `36653ec`, `f305c11`, `492845c`, `69d64a3`, `6465509`, `6810541`, `fca332a`, `c6ff2af`, `7bea29d`.



## Bản Mobile 1.10.18 — thông tin ứng dụng, vuốt nhóm và ổn định luồng

- Từ mốc này repo này được phát triển và đóng gói **chỉ cho Mobile**; dự án TV riêng nằm tại `phuongnm7/nm7-tv-android`.
- “Thông tin ứng dụng” tự đọc version từ gói cài đặt và hiển thị `Phiên bản: 1.10.18 (Mobile)`.
- Thay mô tả bằng: “Ứng dụng được phát triển bởi Phuongnm7 vì mục đích cá nhân, không vì mục đích thương mại.”
- Player dựng sẵn chỉ mục kênh theo nhóm, không quét toàn playlist mỗi lần đổi nhóm; adapter không sao chép lại danh sách và dùng ID ổn định.
- Thanh nhóm hỗ trợ vuốt ngang đổi nhóm sau khi nhấc tay, cuộn tới nhóm đang chọn; bật hardware layer và tắt overscroll/fading edge để thao tác nhẹ hơn.
- Tăng bộ đệm chống giật lên 15–60 giây, giữ back-buffer 10 giây, bắt đầu phát từ 500 ms, thêm HTTP keep-alive.
- Nếu Player mắc ở trạng thái buffering liên tục 20 giây, cơ chế phục hồi phiên hiện có được kích hoạt thay vì đứng hình vô hạn.
- Đặt `playWhenReady` trước `prepare` để giảm độ trễ bắt đầu phát.
- Version: `versionCode 35`, `versionName 1.10.18`.
- Workflow đã tách sang chỉ chạy `testMobileDebugUnitTest`, `lintMobileDebug`, `assembleMobileDebug`; không build hoặc upload TV.
- Build run `34671826668`, job `103494506653`: compile, unit test, lint, kiểm tra chữ ký, đóng gói APK và smoke-test Mobile trên Android 15 đều **PASS**.
- Artifact `NM7-IPTV-Mobile-1.10.18-APK`, ID `10291245926`, archive digest `sha256:60a03b8179cb74f7e33008155c5a4751a490adbdc7893ce022e98e1da3e53323`.
- Các commit chính: `1bbde5e`, `8877abb`, `cbf5ca0`, `7aac12a`, `39afa7a`, `767ab1e`, `9507505`.

## Trạng thái nguồn và bản bàn giao

- Commit mã Mobile 1.10.18 cuối trước tài liệu: `95075051940e42bced32f92ce4983f860f4cce97`.
- Version hiện tại: `versionCode 35`, `versionName 1.10.18`; trong ứng dụng hiển thị **1.10.18 (Mobile)**.
- Build gần nhất: run `34671826668`, job `103494506653`.
- Compile, unit test, Android lint, kiểm tra chữ ký, đóng gói và upload APK Mobile đều **SUCCESS**.
- Smoke-test cài và mở APK Mobile trên Android 15 cũng **SUCCESS**; toàn workflow kết luận **success**.
- Artifact hiện hành: `NM7-IPTV-Mobile-1.10.18-APK`, ID `10291245926`, archive digest `sha256:60a03b8179cb74f7e33008155c5a4751a490adbdc7893ce022e98e1da3e53323`.
- Không tiếp tục build hoặc phát triển bản TV trong repo này. Mọi nội dung TV chuyển sang repo riêng `phuongnm7/nm7-tv-android`.

## Thay đổi mới nhất cần giữ nguyên

### Logo

- Logo gốc chung/TV: `app/src/main/res/drawable/nm7_main_logo.png`.
  - Nền trong suốt.
  - Giữ biểu tượng màu + chữ **Phuongnm7 TV**.
- Logo ghi đè riêng cho Mobile: `app/src/mobile/res/drawable/nm7_main_logo.png`.
  - Chỉ giữ chữ **Phuongnm7 TV** và hai đường kẻ; không có biểu tượng.
  - Được thu nhỏ còn khoảng 75% so với lần đầu và căn giữa bằng vùng đệm trong suốt.
- Không sửa logo TV khi tinh chỉnh kích thước logo Mobile.

### Player TV

- Đã xóa nút xoay màn hình `↻` khỏi `app/src/tv/res/layout/activity_player.xml`.
- Bản TV không cần chức năng xoay màn hình; bản Mobile vẫn giữ chức năng này.
- `app/src/tv/res/values/ids.xml` khai báo ID `btnRotate` để lớp `PlayerActivity` dùng chung vẫn biên dịch, nhưng layout TV không tạo View nên không có nút/chức năng xoay.
- Commit liên quan:
  - `2428fa7994112e9f81f7f911f9641f3f03ed6eda` — cập nhật logo chung và bỏ điều khiển xoay TV.
  - `fdcd9037e860c81d0a5a10915aecef6148e6ed96` — sửa biên dịch TV sau khi bỏ nút.
  - `3a3a555bec63b7a435f4a00b610a8473242ff962` — logo chữ-only riêng cho Mobile.
  - `767e03bc9197c51b6937f41fa0e8869ee46e6eb3` — thu nhỏ logo Mobile.

## Playlist động và reload

- Ứng dụng 1.10.17 có nút **Tải lại** playlist.
- Khi tải từ `raw.githubusercontent.com`, ứng dụng thêm tham số thời gian và gửi `Cache-Control: no-cache, no-store, max-age=0` cùng `Pragma: no-cache` để tránh dữ liệu cũ.
- User-Agent hiện dùng `Nm7-IPTV/1.10.17 Android` hoặc `Nm7-IPTV/1.10.17 Android-TV`.
- Endpoint M3U động/Vercel và khóa giải mã thuộc dự án/repo private tách riêng; không đưa khóa hoặc secret vào repo Android/tài liệu công khai.

## Việc xử lý tiếp theo

1. Cài APK Mobile Build #190 trên điện thoại thật và xác nhận logo chữ đã đủ nhỏ; nếu chưa, chỉ chỉnh asset trong `app/src/mobile/res/drawable/`.
2. Cài APK TV Build #190 và xác nhận nút xoay không còn trong Player, điều khiển D-pad/OK/LEFT và chuyển kênh vẫn hoạt động.
3. Mở artifact/log của smoke-test run `34489487767` để phân biệt lỗi test/emulator với lỗi ứng dụng; không tắt test để che lỗi.
4. Nếu sửa mã hoặc tài nguyên tiếp, tăng version chỉ khi chuẩn bị một bản phát hành mới theo yêu cầu; build cả hai flavor và ghi lại run/artifact/SHA.
5. Repo phải tiếp tục **private**. Không commit playlist riêng, token, cookie, khóa DRM, khóa giải mã hoặc secret Vercel.

---

# Nhật ký tiếp tục dự án — IPTV Player Android

## Phiên bản 1.7 — Nm7 IPTV và chọn kênh nhanh trên TV

- Đổi tên hiển thị ứng dụng thành `Nm7 IPTV`; giữ nguyên applicationId để tương thích dữ liệu/cài đặt hiện có khi chữ ký cài đặt khớp.
- Màn hình chính thay ô chọn nhóm xổ xuống bằng thanh nút nhóm cuộn ngang. Chọn một nhóm sẽ hiển thị ngay danh sách kênh thuộc nhóm; từng nút có focus D-pad rõ ràng.
- Trên giao diện TV, khi đang xem bấm phím Trái sẽ mở bảng nhóm/kênh nhanh bên trái. Luồng hiện tại tiếp tục phát phía sau; chọn kênh khác mới chuyển nguồn, Back đóng bảng.
- Bảng kênh nhanh đọc playlist đã lưu trong vùng riêng của ứng dụng, giữ URL/header/DRM hợp lệ của từng kênh và đánh dấu kênh đang xem.
- Tăng versionCode 9, versionName 1.7.0; đổi artifact thành `Nm7-IPTV-1.7-APK`. Việc tiếp theo: compiler, JUnit, lint, smoke Android 15/D-pad và bàn giao APK.

## Phiên bản 1.6.1 — sửa focus lựa chọn nhóm trên TV

- Sửa ô chọn nhóm và từng dòng trong danh sách xổ xuống: khi điều hướng bằng D-pad, mục hiện tại có nền xanh sáng, viền xanh lá 3dp và vẫn rõ ở trạng thái nhấn/chọn.
- Áp dụng đồng nhất cho màn hình dọc và ngang; giữ thao tác cảm ứng trên mobile.
- Tăng versionCode 8, versionName 1.6.1. Workflow riêng tư sẽ chạy compiler, JUnit, lint, đóng gói APK và smoke Android 15 trước khi bàn giao.
- Mã bản vá ở commit `be021b23bebe0aad3ebb7baf7750625180a98c9e`. Build run `34292430994`, job `102281669427`: compiler, 23 JUnit tests, lint, xác minh chữ ký và upload APK đều đạt; smoke Android 15 đang chạy độc lập lúc lưu checkpoint.
- Artifact `IPTV-Player-1.6.1-APK`: ID `10081938250`, archive SHA-256 `eb7393b119089cbe3e75ddd89002927c8374c1c06543f8ea65ec2d12c601e354`.
- APK 1.6.1 đã tải và kiểm tra `sha256sum -c` đạt: 6.681.741 bytes, SHA-256 `aaaaf42c1b30634c6572fd92b4d43574a362b8641c64232cfc09832410f1309f`; đã lưu bản bàn giao.

## Phiên bản 1.6 — Mobile/TV, D-pad và nhiều link IPTV

- Thêm lựa chọn giao diện Tự động, Mobile hoặc TV. Chế độ tự động nhận diện `UI_MODE_TYPE_TELEVISION`; lựa chọn được lưu riêng trên thiết bị.
- Thêm launcher `LEANBACK_LAUNCHER`, khai báo TV/touchscreen không bắt buộc và hỗ trợ activity co giãn để APK có thể cài/chạy trên Android TV lẫn điện thoại.
- Nút, tab và thẻ kênh có trạng thái focus viền xanh sáng; danh sách dùng selector riêng và chế độ TV tự đưa focus tới tab Tất cả để điều khiển bằng D-pad.
- Thêm kho tối đa 50 link playlist: đặt tên, lưu, chọn để tải, hoặc xóa. Link được lưu trong vùng app-private; tải thành công tự ghi nguồn nhưng giữ lại tên tùy chỉnh.
- Tăng versionCode 7, versionName 1.6.0. Việc tiếp theo: build/test/lint, smoke điều hướng và bàn giao APK 1.6; repo tiếp tục private.
- Build đầu run `34289998336`, job `102274190988`: compiler và 23 JUnit tests đạt; lint chặn đúng lỗi `MissingTvBanner` vì đã khai báo Leanback launcher. Thêm banner vector 320×180 và build lại, không tắt lint.
- Mã cuối 1.6 ở commit `ace3b68aba1acefeb92a68b97797b0cf9484300a`. Build run `34290442421`, job `102275564081`: compiler, 23 JUnit tests, lint, đóng gói, xác minh chữ ký và upload APK đều đạt; smoke Android 15/D-pad còn chạy độc lập lúc bàn giao.
- Artifact `IPTV-Player-1.6-APK`: ID `10081166815`, archive SHA-256 `27ed2c0f1dcb14c860030b21e241a709f2f971bd309f24b87ac9675b5a03061f`.
- APK 1.6 đã tải, kiểm tra ZIP và `sha256sum -c` đạt: 6.679.803 bytes, SHA-256 `aae483f7e110a3440ad33cabd0cbabbd61b2271d7880ae4a8bb084ee93de74fc`; đã lưu bản bàn giao.

## Phiên bản 1.5 — giao diện kiểu thư viện Super OK

- Người dùng đã xác nhận bản 1.4 phát được đúng nguồn trước đây lỗi DASH/ClearKey.
- Phân tích APK tham chiếu cho thấy bố cục trọng tâm là thư viện kênh, mục Yêu thích/Gần đây, cài đặt tập trung và nhiều điều khiển khi xem. Chỉ tái tạo luồng sử dụng; không sao chép mã, tài nguyên hay thương hiệu của APK.
- Thêm thanh Tất cả/Yêu thích/Gần đây; danh sách kênh dạng thẻ, nút sao và menu nhấn giữ để yêu thích, chọn xuất, xem URL hoặc phát.
- Yêu thích và lịch sử được lưu riêng trên thiết bị bằng mã băm SHA-256 của định danh kênh, không lưu thêm URL/token ở kho tùy chọn.
- Gom tùy chọn hình nền, hiện URL, mật độ hàng, FPS, đồng hồ và xóa lịch sử vào một menu. Trình phát thêm lựa chọn Fit/Zoom/Fill và đồng hồ tùy chọn; giữ nguyên luồng DRM 1.4 đã hoạt động.
- Tăng versionCode 6, versionName 1.5.0. Việc tiếp theo: compile/test/lint, kiểm tra giao diện/smoke và bàn giao APK 1.5; repo tiếp tục private.
- Mã 1.5 đã commit tại `c6baa1dd4d8cb73ee142f276ef0de398018d0248`. Build run `34287894390`, job `102267591874`: compiler, 22 JUnit tests, lint, đóng gói, kiểm tra chữ ký và upload APK đều đạt; smoke Android 15 còn chạy độc lập lúc bàn giao.
- Artifact `IPTV-Player-1.5-APK`: ID `10080288540`, archive SHA-256 `1a91c5050fa21d6d63cd549fc9512bbc0d9d6babdac112da6fb1a44a1e2e4a90`.
- APK 1.5 đã tải, kiểm tra ZIP và `sha256sum -c` đạt: 6.677.638 bytes, SHA-256 `fe28cef7341d82cfde0de8121e2a4e34b7194fbd0eefdf979e8d697f92bce50b`; đã lưu bản bàn giao.

## Tóm tắt bàn giao hiện tại — 2026-09-08 15:12 UTC

- Kho chuẩn: `phuongnm7/iptv-player-android`, nhánh `main`, trạng thái **private** đã xác minh. Không đổi public và không triển khai Play Store/Sites.
- Commit chứa mã ứng dụng 1.4: `e4d6eaf1d7b024e619ccaa16db732bb63b2eddab`. Commit checkpoint trước khi viết mục bàn giao này: `d008f35f66bcc947cb6ceb181cb410c7ca8a08cf`.
- Build bàn giao: run `34242285983`, job `102115239390`. Compiler, 21/21 JUnit tests, lint, đóng gói, xác minh chữ ký và upload APK đã thành công.
- Smoke Android 15 của cùng run vẫn `in_progress` tại thời điểm chốt nhật ký. Không diễn giải build thành công là bằng chứng luồng DRM thật đã phát được trên điện thoại người dùng.
- APK bàn giao: `IPTV-Player-1.4.apk`, 6.658.988 bytes, SHA-256 `6897e461141db5398582d23aebbbf8b2edc926372bc6d5fe0bf5b66e65065939`. Artifact GitHub riêng tư: `IPTV-Player-1.4-APK`, ID `10062651950`, archive SHA-256 `1ca931361d57df280647c48d1c7fcd012bc1c521191975c8338b4685fb2481df`.
- Lỗi máy thật gần nhất của bản 1.3: `ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED` trên kênh DASH/ClearKey. Bản 1.4 đổi cách lấy URL ClearKey động từ Media3 POST mặc định sang GET nền, sau đó chuẩn hóa JSON/JWK hoặc KID:KEY trong bộ nhớ.
- Sửa lỗi này dựa trên metadata playlist và so sánh hành vi APK tham chiếu, nhưng chưa có xác nhận máy thật rằng đúng kênh đã phát. Không trích xuất khóa từ APK, không ghi phản hồi giấy phép vào log hoặc lưu trữ.

### Việc người tiếp nhận nên làm tiếp

1. Cài đúng APK 1.4 lên điện thoại thật và thử lại cùng kênh. Nếu xung đột chữ ký debug, xuất playlist trước, gỡ bản cũ rồi cài lại.
2. Mở run `34242285983`, ghi kết quả cuối của smoke vào file này. Nếu smoke treo, đọc artifact/log emulator trước khi sửa hoặc chạy lại.
3. Nếu DRM vẫn lỗi, lấy `adb logcat` tại lúc mở kênh, tập trung Media3/MediaDrm/HTTP status và exception chain; tuyệt đối không ghi hay commit body phản hồi, token, KID/KEY hoặc cookie.
4. Xác minh endpoint giấy phép do playlist cung cấp trả HTTP 2xx và định dạng nào trong JSON/JWK/KID:KEY; kiểm tra yêu cầu User-Agent/Referer/header hợp lệ. Chỉ hỗ trợ giấy phép người dùng có quyền sử dụng, không vượt DRM.
5. Mọi sửa mã tiếp theo phải tăng `versionCode`/`versionName`, chạy lại `testDebugUnitTest`, `lintDebug`, `assembleDebug`, xác minh chữ ký, tải artifact và kiểm tra `sha256sum -c` trước khi bàn giao APK mới.

## Phiên bản 1.4 — sửa lấy giấy phép ClearKey từ URL động

- Ảnh máy thật của bản 1.3 báo `ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED`; nhận diện DASH đã hoạt động nhưng giấy phép chưa lấy được.
- Đã phân tích APK tham chiếu do người dùng cung cấp (SHA-256 `701a66c7ff6e901313bf6c1260003f7ffe3ddb2fe05028610c2b82e33f70398d`): app này có trình IPTV tùy biến, bộ phát hiện luồng và các thư viện phát bổ sung. Không sao chép mã hay trích xuất khóa từ APK.
- Nguyên nhân trong app: Media3 mặc định gửi challenge bằng POST tới mọi URL giấy phép, trong khi playlist này khai báo ClearKey qua endpoint HTTP động trả KID/KEY bằng GET.
- Bản sửa gọi GET ở luồng nền, giới hạn phản hồi 64 KiB, chỉ chuyển tiếp User-Agent/Referer/Origin/Cookie khi máy chủ luồng và giấy phép cùng host, rồi chuẩn hóa JSON/JWK hoặc KID:KEY cho Media3.
- Dữ liệu ClearKey chỉ giữ trong bộ nhớ của màn hình phát, không ghi log, không lưu vào playlist hay trạng thái ứng dụng; không tự tìm hoặc vượt DRM.
- Thêm kiểm thử URL ClearKey từ xa và JSON GET; tăng versionCode 5, versionName 1.4.0. Việc tiếp theo: build/test/lint, tải và bàn giao APK 1.4; repo tiếp tục để private.
- Build đầu của 1.4: run 34241384570, job 102112137194. Java app đã biên dịch; 20/21 test đạt. Test JSON ClearKey thất bại vì `android.util.Base64` là stub trong JVM test. Đang thay bằng `java.util.Base64` (core-library desugaring đã bật) để cùng mã chạy được trên Android 6+ và trong kiểm thử, không bỏ test.
- Build thứ hai: run 34241952754, job 102114101849. Java app tiếp tục biên dịch nhưng cùng test bị chặn bởi `org.json` stub của Android SDK trong JVM. Bổ sung `org.json` chỉ ở `testImplementation`; không đưa thêm thư viện này vào APK runtime.
- Build bàn giao dùng commit `e4d6eaf1d7b024e619ccaa16db732bb63b2eddab`: run 34242285983, job 102115239390. Compiler, 21 JUnit tests, lint, đóng gói, kiểm tra chữ ký và upload APK đều đạt; smoke Android 15 tiếp tục chạy độc lập.
- Artifact `IPTV-Player-1.4-APK`: ID 10062651950, archive SHA-256 `1ca931361d57df280647c48d1c7fcd012bc1c521191975c8338b4685fb2481df`.
- APK 1.4 đã tải, kiểm tra ZIP và `sha256sum -c` đạt: 6.658.988 bytes, SHA-256 `6897e461141db5398582d23aebbbf8b2edc926372bc6d5fe0bf5b66e65065939`; đã lưu bản bàn giao.

## Phiên bản 1.3 — sửa URL `.php` báo container không hỗ trợ

- Ảnh máy thật báo `ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED` ở URL `tv360.php?...`.
- Nguyên nhân trong app: Media3 không thể suy ra HLS/DASH từ đuôi `.php`; PlayerActivity chưa dùng `#KODIPROP:inputstream.adaptive.manifest_type` mà parser đã giữ lại.
- Thêm `StreamSpec`: ưu tiên chỉ dẫn `manifest_type=hls/mpd/dash/ism`, sau đó mới suy từ đuôi hoặc query URL.
- Thêm kiểm thử cho URL PHP không đuôi có manifest DASH/HLS; tăng versionCode 4, versionName 1.3.0.
- Việc tiếp theo: build/test/lint, chạy smoke và bàn giao APK 1.3; repo tiếp tục để private.
- Build cuối dùng commit `aa17f50c4cd948cfb539c3bff0c6081240e54753`: run 34238328861, job 102102081333. Compile, 20 JUnit tests, lint, đóng gói, chữ ký và upload APK đã đạt.
- Artifact `IPTV-Player-1.3-APK`: ID 10061032953, archive SHA-256 `7bed40d9dcb6431262d7b961d9f07cb811179b31b3aa039701a86d1768d40fe7`.
- APK đã tải và `sha256sum -c` đạt: 6.658.989 bytes, SHA-256 `99bef991708137b3fdb527a7797de2f2a354192d23d34c4900a236e24ecd0e01`; đã lưu bản bàn giao. Smoke Android 15 vẫn chạy độc lập.

## Phiên bản 1.2 đang thực hiện — 2026-09-08

- Yêu cầu mới từ ảnh điện thoại: bỏ việc chặn chung mọi kênh DRM; thêm xoay khi xem, hình nền tùy chọn, FPS thực tế và làm danh sách kênh nổi bật ở màn hình chính.
- Đã xác minh repo vẫn private; HEAD trước khi sửa: e6054675dcc08f165d90a3f1411b7ad10dcdd8d7, tree dab57f21659b56a73424a6af7316669abab7f60a.
- Đã khôi phục đủ 35 file từ GitHub vì thư mục làm việc tạm không còn source; không phụ thuộc bản APK/zip cũ.
- Thêm DrmSpec/DrmPlayback: Widevine, ClearKey và PlayReady bằng Media3/MediaDrm; đọc KODIPROP license_type/license_key và header URL-encoded. Không tự tìm hay vượt DRM; thiếu giấy phép thì báo phần thiếu và cho nhập cấu hình hợp lệ.
- Thêm FpsMeter: lấy chênh lệch renderedOutputBufferCount theo thời gian thực, cập nhật mỗi 2 giây; không hiển thị FPS khai báo hay tần số quét màn hình.
- Thêm nút xoay tự động/ngang/dọc trong PlayerActivity; bỏ ép sensorLandscape trong manifest.
- Thêm WallpaperStore: 3 nền màu và ảnh do người dùng chọn, giới hạn 32 MB/1600 px, sửa EXIF, lưu JPEG trong vùng app-private.
- Viết lại màn hình chính một cột thích ứng dọc/ngang: danh sách kênh luôn chiếm vùng co giãn; bảng nguồn tự thu gọn sau khi tải; bộ lọc và nút xuất luôn thấy.
- Tăng versionCode 3, versionName 1.2.0; artifact đổi thành IPTV-Player-1.2-APK.
- Kiểm tra cục bộ hiện tại: PASS 36 core assertions; 15 XML hợp lệ; tổng 18 JUnit tests trong source. Chưa có kết quả Android compiler/build 1.2 tại mốc này.
- Việc tiếp theo: commit bản 1.2, chạy testDebugUnitTest/lintDebug/assembleDebug; sửa lỗi compile thực; tải và lưu APK mới; cập nhật run/SHA tại đây.
- Build run 34233343918 cho commit f4cfb942cdfc3d26841eea01fcf3b162a65bf4bc đã qua compile/test/lint, ký và upload APK 1.2. Artifact ID 10058977161; archive SHA-256 0950e8786909b2ec8bef0cb632168bc68b4ff67995ee0039637bd49813b3c7d9.
- APK sơ bộ 1.2 đã tải, `sha256sum -c` đạt; kích thước 6.658.987 bytes và đã lưu. Cần build lại một lần sau sửa nhỏ: nút DRM phải hiện trước khi báo thiếu license; không lưu license người dùng nhập trong instance state.
- Bản sửa cuối đã commit tại `7f805c28e3badced2e9b1334f405c4ff285b98d2` (tree `35549be7714211ef5885ce270f9ef228856294ff`).
- Build cuối: run 34234206211, job 102088004063. Compile, 18 JUnit tests, lint, đóng gói, kiểm tra chữ ký và upload APK đều đạt; smoke Android 15 đang chạy tại thời điểm ghi mốc này.
- Artifact cuối `IPTV-Player-1.2-APK`: ID 10059316148, archive SHA-256 `fb1f5bd3a143cff7aea71ac86aa2694d352cf63f23814b77e767741095bcb11d`, hết hạn 2026-10-08.
- APK cuối đã tải, kiểm tra ZIP và `sha256sum -c` đạt: 6.658.984 bytes, SHA-256 `c4dc7199f474498717b1bb6895ae202807994c9ed962de2aa140f0eb906a34f8`.

Cập nhật: 2026-09-08. Lưu file này trong GitHub riêng tư để nối lại dù phiên trò chuyện hoặc thư mục tạm mất.

**Trạng thái bàn giao:** Đã tạo, tải về, kiểm tra checksum và lưu APK 1.1 từ Build 4. Compiler/test/lint, chữ ký và upload APK đều đạt. Theo yêu cầu làm nhanh, bàn giao APK trước khi smoke mở rộng chạy hết. Không coi toàn bộ workflow hoặc máy thật là đã kiểm tra thành công; mở run Build 4 bên dưới để lấy kết quả mới nhất.

## Mục tiêu và quyền đã được xác nhận

- Tạo APK Android cài được: nhập URL/tệp M3U, tìm/lọc, phát IPTV, chọn và xuất kênh; hỗ trợ Full HD/QHD/4K nếu codec/thiết bị/nguồn cho phép.
- Người dùng cho phép đưa source và workflow lên đúng kho **phuongnm7/iptv-player-android** và chạy GitHub Actions.
- Kho đã xác minh **private**. Không đổi sang public, không dùng kho playlist Iptv khác, không phát hành Pages/Play Store.
- Không có playlist thật, thông tin đăng nhập hay khóa DRM của người dùng trong mã nguồn.
- Người dùng yêu cầu lưu quá trình để tiếp tục sau này.

## Nguồn chuẩn và điểm kiểm tra

- Repo: https://github.com/phuongnm7/iptv-player-android
- Nhánh: main.
- Commit đầu của app: 60c57efd6abfe06767965f8e06abbf18361d7281
- Tree app đầu: c92f2e826c9d6882c078d2323dafbc43d64aea61
- Commit khởi tạo có README của người dùng: 2d342528e154bba7eb26dbe02b04d4a4ab1f57dd.
- Bản app: 1.1.0, applicationId vn.phuong.iptvplayer, min SDK 23, compile/target SDK 36.
- JDK 17, Gradle 8.13, AGP 8.13.2, Media3 1.11.0.
- Source đã đưa lên gồm 33 file (trước khi thêm nhật ký này).
- Thư mục làm việc tạm: /workspace/scratch/fd876c6053ed/IptvPlayer. Đừng phụ thuộc thư mục này; lấy lại source từ repo nếu mất.

## Những gì đã thực hiện

- Sửa parser: không tách segment HLS thành kênh; BOM/CRLF/URL tương đối; bảo toàn case của token/path.
- Phân biệt URL trùng có header/tùy chọn khác; xuất giữ các header và metadata kênh, nhận biết DRM để báo giới hạn.
- Thêm nhập luồng trực tiếp, URL đầy đủ qua nhấn giữ, chọn theo bộ lọc, không ghi đè playlist khi import rỗng/lỗi.
- Lưu playlist/chọn kênh trong AtomicFile riêng của ứng dụng; loại khỏi backup và device transfer.
- Giao diện ngang/dọc, giữ trạng thái khi đổi hướng; trình phát có retry, ép định dạng, giới hạn chất lượng và độ phân giải thực.
- Media3: HLS/DASH/SS/HTTP/RTSP TCP, module RTMP, DefaultDataSource và multicast lock cho UDP MPEG-TS.
- Giao thức không hỗ trợ/DRM được thông báo; không cam kết mọi giao thức.
- Thêm workflow riêng tư, pin actions theo SHA; quyền contents:read; không publish build scan.

## Kết quả kiểm tra đã có

- CoreCheck chạy bằng Java thật: ban đầu PASS 24 assertions; bản sửa khóa nhận dạng mới PASS 28 assertions.
- Build 2 có 10/10 test JUnit đạt, 0 failed/ignored (đã đọc HTML report). Build 4 có 14 test trong source và tác vụ testDebugUnitTest đã đạt; report chi tiết sẽ upload sau bước emulator.
- 15 XML parse hợp lệ, ID giao diện ngang/dọc khớp.
- Workflow YAML parse hợp lệ.
- Đã đọc XML lint Build 2: 0 errors, 24 warnings không chặn (style, autofill, target/version và bố cục).
- Có APK debug-signed ở Build 2; chữ ký APK v1/v2 xác minh thành công.
- Build 3 đã đạt 8 kiểm tra UI trên Android 15 emulator; đã xem ảnh dọc/ngang và ảnh video MP4 320×180. Smoke chưa chạy hết; điện thoại thật chưa thử.

## Lịch sử build

### Build 1 — thất bại ở môi trường SDK

- Run: https://github.com/phuongnm7/iptv-player-android/actions/runs/34204423381
- Run ID: 34204423381, job ID: 101990471708.
- Commit: 60c57efd6abfe06767965f8e06abbf18361d7281.
- Java 17 và Gradle 8.13 đã cài thành công.
- Bước Install Android SDK packages thất bại: sdkmanager: command not found, exit 127.
- Chưa chạy compiler/test/lint; không phải lỗi mã Java đã xác định.
- Cách sửa đang thực hiện: thêm bước cài Android command-line tools trước khi gọi sdkmanager; không dựa vào image runner đã có SDK.

### Build 2 — thành công, đã có APK

- Đã xác minh lại kho private và HEAD vẫn là commit app đầu, không có thay đổi của người dùng bị ghi đè.
- Thêm android-actions/setup-android v3, pin commit 9fc6c4e9069bf8d3d10b2204b1fb8f6ef7065407.
- Cài command-line tools 12266719 và platform-tools, thiết lập PATH/ANDROID_HOME trước khi cài API 36 và build-tools 36.0.0.
- Commit: 8495f2cde2463813dcb8b4ac4bc50764e097dc67.
- Tree: d71176a74d81352c913f45882e3a4e4f1d387c6f.
- Run: https://github.com/phuongnm7/iptv-player-android/actions/runs/34205233861 (job 101993064010).
- SDK setup, testDebugUnitTest, lintDebug, assembleDebug, kiểm tra chữ ký và upload artifact đều thành công.
- BUILD SUCCESSFUL in 3m 16s. APK có chữ ký v1/v2 hợp lệ (Android Debug).
- Artifact IPTV-Player-1.1-APK: ID 10047594665, hết hạn 2026-10-08.
- SHA-256 APK Build 2: 081ea974eab47f5b649db8f8951ff5ff7318f191d1f311203cb278ffa3fdffe1.
- Android-test-reports: ID 10047595347, hết hạn 2026-09-22.
- Còn cảnh báo compiler về annotation Scope.LIBRARY_GROUP và setup-android Node20 bị runner chuyển Node24; không có lỗi build/lint chặn.

### Build 3 — APK đạt, smoke dừng vì hộp hướng dẫn hệ thống

- Script tools/android_smoke.py dùng adb và máy ảo Android 15; fixtures playlist và video MP4/HLS/DASH 320×180 được tạo cục bộ.
- Kiểm tra import, trùng/thiếu URL, tìm/lọc/chọn, URL đầy đủ, ngang/dọc, import lỗi không làm mất dữ liệu, khôi phục sau process restart và giao thức không hỗ trợ.
- Phát mẫu qua localhost được adb reverse; không dùng nội dung/credential của bên thứ ba.
- Workflow đã chuẩn bị thêm action emulator pin commit a421e43855164a8197daf9d8d40fe71c6996bb0d và artifact bằng chứng riêng tư.
- APK được upload trước smoke test nên phải phân biệt lỗi smoke với lỗi tạo APK.
- Commit: 85bb17ba6734daac901d9a0ad75fbcc879fe4d22; tree: 2abf5d328a5799d736922e26cdb1a3d3f3b4d2ab.
- Run: https://github.com/phuongnm7/iptv-player-android/actions/runs/34206001050 (job 101995501939).
- Các bước compile/test/lint, chữ ký và upload APK đã đạt. Artifact APK ID 10047824932, hết hạn 2026-10-08.
- Smoke đã đạt 8 checks: khởi động, URL/relative/duplicate/missing, tìm/chọn, lọc nhóm, URL đầy đủ, ngang/dọc, không mất playlist khi nhập HTML lỗi, khôi phục sau process restart.
- Smoke dừng ở HTTP-MP4: wait_for không nhìn thấy status vì Android phủ màn hình "Viewing full screen / Got it". failure.xml xác nhận android:id/immersive_cling_title và android:id/ok.
- Đã nhìn failure.png: video màu tổng hợp đã xuất hiện, status 320×180 và timeline 00:20/00:20. Không diễn giải đây là lỗi decoder; log emulator có cảnh báo libcuda nhưng không ngăn khung hình này.
- Artifact smoke ID 10048042342, reports ID 10048043186; hết hạn 2026-09-22. Bằng chứng tạm tại /workspace/scratch/fd876c6053ed/build3-smoke.

### Build 4 — bản giao, khóa trùng an toàn và bảo toàn tùy chọn

- Header khác chữ hoa/thường phải được coi là cùng tên (Cookie/cookie); giá trị header vẫn phân biệt case.
- Thay khóa Map/List.toString bằng các trường có độ dài rõ ràng, tránh loại nhầm kênh khi giá trị có dấu phẩy/dấu bằng/ký tự phân cách.
- Giữ tùy chọn EXTVLCOPT không nhận diện (ví dụ network-caching) khi xuất; không tuyên bố Media3 áp dụng các tùy chọn này.
- Thêm 4 test JUnit và 4 CoreCheck hồi quy; CoreCheck đã đạt 28/28, bước compile/test/lint của Build 4 đã đạt.
- Sửa smoke để nhấn nút Got it của hướng dẫn toàn màn hình khi nó hiện; thêm kiểm tra tạo M3U qua Android file picker và mở lại tệp.
- Commit mã của APK: d2a21c03077ed9b563d1a1d388e7832d6c89cb51; tree: b04d8b4c8e18540ca775d1053c091737f344d8fb.
- Run: https://github.com/phuongnm7/iptv-player-android/actions/runs/34207283159 (job 101999650047).
- Đã kiểm tra trạng thái bước: Compile, test and lint = success; Verify and package APK = success; Upload installable APK = success.
- Khi bàn giao, Check app on Android 15 emulator còn in_progress. Chưa xác nhận đầy đủ HLS/DASH, file picker và tổng số smoke checks.
- APK artifact: ID 10048330039, hết hạn 2026-10-08. Archive SHA-256: 0312fa353c5e029500cce62962603c44706a567e4d8f62af47cd68f92b57b34c (tải về khớp digest GitHub).
- File bàn giao: IPTV-Player-1.1.apk, 6.641.439 bytes; đã lưu cho người dùng, không chỉ giữ trong artifact có hạn.
- SHA-256 APK: f98188e0f2a05405d6f2236d271a542255e5412c18ebd4f10d46a05c7c958032. sha256sum -c SHA256SUMS.txt = OK.
- Bản cài đặt là debug-signed cá nhân, chưa phát hành Play Store. Kho vẫn private khi kiểm tra lúc bàn giao.
- Đường dẫn làm việc lúc giao: /workspace/scratch/fd876c6053ed/deliverables/IPTV-Player-1.1.apk. Nếu scratch mất, tìm bản đã lưu theo tên/SHA hoặc tải lại artifact của đúng run.

## Làm tiếp

1. Xác minh repo còn private và HEAD hiện tại để không ghi đè thay đổi của người dùng.
2. Mở run Build 4 (34207283159), lấy trạng thái cuối và artifact Android-emulator-smoke/Android-test-reports. Không chạy lại build trước khi đọc kết quả hiện có.
3. Đọc result.json, log và ảnh thực. Cập nhật file nhật ký này; sửa nếu có lỗi thật. Không tắt test để che lỗi, không coi hộp hướng dẫn hệ thống là lỗi decoder.
4. APK đã giao là bản từ commit d2a21c0; nếu sửa mã ứng dụng, tăng versionCode và build/kiểm tra lại trước khi thay bản giao.
5. Kiểm thử trên điện thoại thật bằng playlist được người dùng cấp; xác minh riêng Full HD/1440p/4K và RTSP/RTMP/UDP. Không lấy playlist lạ thay cho nguồn của người dùng.
6. Nếu muốn cập nhật lâu dài không phải gỡ app, thống nhất khóa ký ổn định và lưu bằng secret, không commit khóa hoặc mật khẩu.

## Ghi chú quan trọng

- Máy làm việc hiện tại tải SDK trực tiếp bị chặn quyền mạng; user đã chấp thuận dùng GitHub Actions thay thế. Không tiếp tục tìm cách vượt chặn.
- Có thể đọc/chỉnh repo qua GitHub connector. Git data API tạo tree → commit → update_ref (force=false); luôn dựa trên tree/HEAD hiện có.
- Artifact APK sẽ là debug-signed để cài thử cá nhân, chưa phải release Google Play.
- Các runner khác nhau có thể sinh debug keystore khác; trước khi gỡ bản cũ để cài bản có chữ ký khác, phải xuất playlist.
- EPG toàn playlist, Xtream login, SRT/RTP URL/RTMPS, DRM và TV launcher chưa hỗ trợ.
- Không coi việc biên dịch thành công là chứng minh phát 4K/mọi luồng. Không lưu secret trong nhật ký hoặc source.
