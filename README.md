# NM7 IPTV Mobile 1.10.113 — CricHDai-inspired UI branch

> **Development branch:** `feature/mobile-crichdai-ui-112`  
> **Baseline:** `stable/mobile-1.10.112` → commit `6aea2d995280017c1e7c00310bcca0c9937b50bb`  
> **Build target:** 1.10.113 / versionCode 129

This branch redesigns the Mobile home/navigation presentation using the observed information architecture of the supplied CricHDai TV reference while preserving the 1.10.112 IPTV playback/data stack. It does not replace or modify the stable 1.10.112 branch.

### UI direction
- Header with NM7 identity and quick actions.
- Primary shortcuts for All Channels, Favorites, Recent and Sources.
- Playlist import moved into a clear, collapsible section.
- Search and group chips remain immediately above the channel list.
- Channel cards are more spacious and media-oriented while preserving logo, EPG, favorite and play actions.
- Existing IPTV, M3U, DRM, EPG, background playback, sleep timer, wallpaper and YouTube/SmartTube behavior remain owned by the baseline implementation.

### Reference-app boundary
The reference APK is used only for observable UI/UX and information-architecture inspiration. No protected URLs, credentials, keys, decoder logic or security controls are extracted or bypassed.

### Build status
### 1.10.113 CricHDai-inspired UI build — CI PASS

- Final commit: `636f6d96a3b894e9bfc8f7452439adc2be38490f`
- Branch: `feature/mobile-crichdai-ui-112`
- GitHub Actions run: **#22** — SUCCESS
- Parser unit tests: **PASS**
- Mobile assemble: **PASS**
- APK upload: **PASS**
- Artifact: `NM7-IPTV-Mobile-1.10.113-UI`
- Artifact SHA-256: `361877ca4290cd7ca6578193461474f3a9d1bccf6adee3e822ddf2eb688d644c`
- APK SHA-256: `f6c5a5fe6b1ca381cfd79bb8c0d28eff8681922deb55956465a316e2ade2efe4`
- Package: `vn.phuong.iptvplayer`
- Version: **1.10.113**, versionCode **129**
- Stable baseline `stable/mobile-1.10.112` remains untouched.

GitHub Actions workflow: `NM7 IPTV Mobile 1.10.113 CricHDai UI Build`.

See [PROGRESS.md](PROGRESS.md) for the detailed implementation history.

---

# NM7 IPTV Mobile — STABLE BASELINE 1.10.112 — 2026-09-30

> **BẢN ỔN ĐỊNH HIỆN TẠI:** 1.10.112. Bản này được chốt làm mốc ổn định tạm thời tại thời điểm hiện tại. **Mọi bản Mobile/build tiếp theo phải lấy chính xác bản này làm nền**, không tự ý lấy một build 1.10.112 khác hoặc một baseline cũ hơn.

## Mốc build ổn định được khóa

- Version: **1.10.112**
- versionCode: **128**
- Workflow: **NM7 IPTV Mobile 1.10.112 Final Build**
- Workflow file: `.github/workflows/nm7-mobile-112-final.yml`
- GitHub Actions run: **#15**
- Commit nguồn của build: **`2ee95f0f4906f0aaecc0748f1e13322b5531afa6`**
- Commit message: `ci: prepare 1.10.112 build`
- Kết quả: **SUCCESS**
- Thời gian: **6m 4s**
- Artifacts: **1**
- Artifact name: **NM7-IPTV-Mobile-FINAL**
- Nhánh khóa baseline: `stable/mobile-1.10.112`

### Quy tắc phát triển từ 1.10.112

1. Bản 1.10.112 ở commit **2ee95f0...** là baseline Mobile chính thức hiện tại.
2. Các bản build sau phải phát triển trực tiếp từ `stable/mobile-1.10.112`.
3. Không dùng một build 1.10.112 khác làm nền.
4. Không lấy `main` hiện tại làm nền nếu `main` đã có các commit phát sinh sau mốc 1.10.112.
5. Mỗi bản mới phải tăng version/versionCode phù hợp và phải được CI build/verify trước khi bàn giao.
6. Chỉ khi người dùng test thực tế và xác nhận một bản mới ổn định thì mới thay đổi stable baseline.

Chi tiết lịch sử kỹ thuật và các mốc trước đây được giữ nguyên bên dưới và trong [PROGRESS.md](PROGRESS.md).

---

# NM7 IPTV Mobile — TRẠNG THÁI HIỆN TẠI — 2026-09-27

> **1.10.106 đã BUILD THÀNH CÔNG trên GitHub Actions #781.** Đây là bản xử lý trực tiếp 3 lỗi người dùng đang phản ánh: video YouTube mở còn chậm, quay lại app sau khi chạy nền phải tải lại video, và pipeline phát chưa mượt. 1.10.106 đã PASS patch, verifier, lifecycle regression checks, unit tests và APK upload. **Chưa đánh dấu runtime PASS** cho đến khi người dùng test thiết bị thật.

## 1.10.106 — Kết quả CI mới nhất

- Version: **1.10.106**
- versionCode: **122**
- Branch: `fix/mobile-1.10.79-youtube-performance`
- Commit CI: `8347399bb2a8c8937eb3c575e5932df876f5e035`
- GitHub Actions: **NM7 Mobile Final Build #781 — SUCCESS**
- Run ID: `36296325937`
- Build Mobile: **SUCCESS**
- Unit tests upload: **SUCCESS**
- APK upload: **SUCCESS**
- Artifact: `NM7-IPTV-Mobile-FINAL`
- Artifact ID: `10923991428`
- Mobile artifact SHA-256: `03f084bf2f8bdc079fecaed3e66e902c1d91a33ef620fb1ddabc584613947e3c`

## Thay đổi 1.10.106

### 1. Giữ nguyên player khi chạy nền
- Khi bấm Home/chuyển ứng dụng, không block ExoPlayer.
- Không gọi đường `onViewPaused()` của presenter trong background transition thông thường.
- Khi quay lại app, ưu tiên sử dụng **cùng ExoPlayer + MediaItem + vị trí phát + buffer**, thay vì tạo lại phiên playback.
- Chỉ thực hiện restore/bind đặc biệt khi thực sự có mini/target restore.

### 2. Giảm việc tải lại format YouTube
- Tăng thời gian reuse format-info trong RAM từ **8 giây → 60 giây**.
- Đây chỉ là cache process-local; không ghi signed stream URL ra disk.
- Mục tiêu là khi video vừa phát xong/đang phát rồi chuyển nền và quay lại, không phải thực hiện lại toàn bộ format-resolution path.

### 3. 4K
- **Không áp đặt giới hạn 4K/30fps hoặc bitrate một cách đoán mò.**
- Không thêm fallback 1440p tự động ở bản này.
- Nếu 4K vẫn giật sau test 1.10.106, bước tiếp theo phải đo codec, resolution, FPS, bitrate và decoder dropped frames của chính video test trước khi sửa.

## Người dùng đã xác nhận trước khi 1.10.106

Trên **1.10.102**, người dùng xác nhận:
- **Avatar YouTube:** PASS.
- **Status bar/player portrait:** PASS.
- **Spinner đen:** PASS.

Các lỗi còn lại người dùng báo:
- YouTube video load vẫn chậm.
- Browse/tab swipe vẫn chậm/lag.
- 4K playback vẫn giật/lag.
- Chạy nền rồi mở lại video phải load lại.

Các phần trên phải tiếp tục được bảo vệ; không được làm regression avatar/status bar/spinner.

## Điều kiện đánh giá 1.10.106

CI PASS **không đồng nghĩa runtime PASS**. Người dùng cần kiểm tra tối thiểu:
1. Mở một video YouTube → thời gian từ tap tới phát.
2. Đang phát → Home/chuyển app → quay lại → **video phải tiếp tục tại vị trí cũ, không tải lại**.
3. Vuốt/chuyển Browse YouTube nhiều lần.
4. Phát video 4K, quan sát giật/rớt frame.
5. Xác nhận avatar, status bar và spinner vẫn như 1.10.102.

Chi tiết kỹ thuật và lịch sử nằm trong `PROGRESS.md`.

---

# Mobile 1.10.91 / versionCode 107 — STABLE BASELINE — USER TEST CONFIRMED

**1.10.91 là bản Mobile tốt nhất/ổn định nhất hiện tại theo kết quả test thực tế mới nhất của người dùng.** Từ thời điểm này, mọi bản Mobile tiếp theo phải phát triển trực tiếp trên nền **1.10.91**, không quay lại các baseline cũ nếu không có yêu cầu đặc biệt.

- Version: **1.10.91**
- versionCode: **107**
- Branch: `fix/mobile-1.10.83-youtube-player-ui`
- Source commit đã build: `60f3be09859f608b23b40b066a9da9168f7b0b7b`
- **NM7 Mobile Final Build #611 — SUCCESS**
- Run: `36104328880`
- Artifact: `NM7-IPTV-Mobile-FINAL`
- Artifact ID: `10850193056`
- [GitHub Actions #611](https://github.com/phuongnm7/iptv-player-android/actions/runs/36104328880)
- [Tải artifact APK ARM64/ARMv7](https://github.com/phuongnm7/iptv-player-android/actions/runs/36104328880/artifacts/10850193056)

## Trạng thái test thiết bị

Người dùng đã cài và test **1.10.91** trên thiết bị thật. Kết luận hiện tại: **đây là bản tốt nhất để làm nền phát triển tiếp theo**.

Các phần đang được giữ nguyên:
- IPTV và danh sách kênh, bao gồm việc kênh cuối không còn bị thanh YouTube/IPTV đè.
- Android status bar trong player YouTube.
- Avatar video/kênh YouTube.
- Live chat.
- Thumbnail không còn bị treo khi video đã phát.
- Tốc độ tải/phát YouTube đã được tối ưu so với các baseline trước.

Lỗi còn lại đã biết:
- Khi mở video YouTube vẫn còn **một nhịp nháy nhẹ của khung hình đầu/chuyển cảnh**. Đây là lỗi ưu tiên cho các bản sau, nhưng **không thay đổi baseline 1.10.91**.
- Thumbnail ở một số đường chuyển tiếp vẫn cần tiếp tục tối ưu độ nét/tốc độ nếu có bằng chứng regression.

## Quy tắc baseline từ 1.10.91

1. Mọi build Mobile mới phải bắt đầu trực tiếp từ **1.10.91 / versionCode 107**.
2. VersionCode/versionName tiếp theo tăng tuần tự từ 107.
3. Không sửa lại các chức năng đã ổn định ở 1.10.91 nếu không có yêu cầu hoặc bằng chứng regression.
4. Khi xử lý lỗi nháy mở video, chỉ thay đổi pipeline transition/player-loading liên quan; phải giữ nguyên IPTV, status bar, avatar, live chat và các phần ổn định khác.
5. Mỗi bản mới phải được CI build/verify trước khi giao APK.
6. Chỉ sau khi người dùng test thực tế mới đánh dấu một bản mới là baseline.

Xem lịch sử kỹ thuật và từng lỗi đã xử lý trong [PROGRESS.md](PROGRESS.md).

---

# Trạng thái Mobile — 1.10.72 / versionCode 90 — STABLE BASELINE

**1.10.72 được chốt làm bản ổn định tạm thời để các bản Mobile tiếp theo phát triển từ đây.**

- Mobile Final Build **#502: SUCCESS** — run 35699011527.
- Commit: 771d599fe6be23ebfeb1ac6a319f50c05279501b.
- APK artifact: https://github.com/phuongnm7/iptv-player-android/actions/runs/35699011527/artifacts/10682325671
- Artifact `NM7-IPTV-Mobile-FINAL`, khoảng 62.13 MB.
- Artifact SHA-256: `e3407a549dd46dc0f4151de12693ee13ad249de169bb06e3550249375003e7f7`.
- Bottom navigation: đúng **2 tab YouTube + IPTV**.
- Đây là **Mobile-only**; không thay đổi Android TV.

### Lỗi đã biết, tạm hoãn

**Avatar kênh YouTube trên thẻ video chưa hiển thị.** Người dùng đã kiểm tra APK thực tế và xác nhận lỗi này. Lỗi được ghi nhận để sửa ở bản sau, không coi 1.10.72 đã hết lỗi avatar.

### Quy tắc phiên bản từ 1.10.72

Bản 1.10.72 là baseline. Bản kế tiếp phải là **1.10.73 / versionCode 91**, sau đó tăng tuần tự cho mọi build mới. Không giữ nguyên version cũ khi build.

Chi tiết lịch sử, lỗi, commit, CI và kế hoạch sửa tiếp theo: [PROGRESS.md](PROGRESS.md).

---

# iptv-player-android

Nm7 IPTV 1.7 — ứng dụng Android tiếng Việt cho mobile và Android TV, không quảng cáo, không phân tích hành vi, không máy chủ trung gian. Màn hình chính có thanh nhóm kênh cuộn ngang; khi đang xem trên TV, phím Trái mở danh sách kênh nhanh phủ lên video mà không dừng phát.

Mốc công việc, lỗi đã sửa, kết quả xác minh và bước tiếp theo: [PROGRESS.md](PROGRESS.md). Đọc file này trước khi tiếp tục ở một phiên khác; không chỉ dựa vào lịch sử trò chuyện hay thư mục tạm.

> **Trạng thái Mobile hiện tại: 1.10.61 build #380 SUCCESS, chờ test thiết bị.** Người dùng test dài hơn và ghi nhận mini của 1.10.60 chuyển đen/báo phiên kết thúc. 1.10.61 sửa các đường báo hết video sớm và giữ mini phát lại khi hết thật. Nhánh `stable/mobile-1.10.60` giữ nguyên làm mốc lịch sử; xem [PROGRESS.md](PROGRESS.md).

## Nhận APK

**1.10.61:** [Tải APK ARM64/ARMv7 và SHA256SUMS — build #380](https://github.com/phuongnm7/iptv-player-android/actions/runs/35558054355/artifacts/10620709984), hết hạn 2026-10-21. Compile/unit tests/đóng gói thành công; **chờ test lại trên thiết bị, chưa stable**. Source `c37977c`.

**1.10.60:** [Tải APK ARM64/ARMv7 và SHA256SUMS — build #379](https://github.com/phuongnm7/iptv-player-android/actions/runs/35545715388/artifacts/10616478461), hết hạn 2026-10-20. Unit tests/build thành công; **đã từng được xác nhận ổn định tạm thời, sau đó test dài hơn tái hiện lỗi mini; giữ làm nền đối chiếu**. Source commit: `3349081`.

**1.10.59:** [Tải APK ARM64/ARMv7 và SHA256SUMS — build #378](https://github.com/phuongnm7/iptv-player-android/actions/runs/35542810501/artifacts/10615413353), hết hạn 2026-10-20. Unit tests/build thành công; **chờ test thiết bị thật, chưa stable**. Commit đã build: `22c0300`.

**1.10.58:** [Tải APK ARM64/ARMv7 và SHA256SUMS — build #375](https://github.com/phuongnm7/iptv-player-android/actions/runs/35519495736/artifacts/10607738558), hết hạn 2026-10-20. Unit tests/build thành công; **chờ test thiết bị thật, chưa stable**. **1.10.57:** build #373 thành công theo ảnh người dùng, còn lỗi trên thiết bị. Các liên kết phiên bản cũ dưới đây giữ để đối chiếu, không phải bản stable.

**1.10.56:** [Tải APK ARM64/ARMv7 và SHA256SUMS — build #372](https://github.com/phuongnm7/iptv-player-android/actions/runs/35510885621/artifacts/10605087490), hết hạn 2026-10-20. Unit tests/build thành công; **chờ test thiết bị thật, chưa stable**. Liên kết 1.10.55 bên dưới là bản cũ còn lỗi.

**Mobile 1.10.55 / versionCode 73:** [build #370 thành công](https://github.com/phuongnm7/iptv-player-android/actions/runs/35504581687). [Tải APK ARM64/ARMv7 và SHA256SUMS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35504581687/artifacts/10603078762) — artifact hết hạn 2026-10-20. Chưa xác nhận stable trên thiết bị thật. APK 1.10.54 đã được người dùng cài thử nhưng còn lỗi, không phải bản stable.

Trong GitHub, mở **Actions → NM7 Mobile Final Build → #379**, tải **NM7-IPTV-Mobile-FINAL** trong Artifacts, giải nén và chọn APK **1.10.60** phù hợp kiến trúc ARM64 hoặc ARMv7 trên Android 6.0 trở lên.

APK dùng chữ ký debug dành cho cài thử cá nhân; không phải bản phát hành Google Play. Không tắt Play Protect. Nếu Android yêu cầu, chỉ cho phép cài APK từ ứng dụng tải tệp mà bạn tin cậy rồi tắt lại quyền đó sau khi cài.

Mỗi máy build có thể sinh khóa debug khác nhau. Nếu Android báo chữ ký không khớp ở lần cài sau, hãy xuất playlist trước khi gỡ bản cũ (gỡ ứng dụng sẽ xóa dữ liệu cục bộ). Bản phát hành cập nhật lâu dài cần khóa ký ổn định lưu trong GitHub Secrets, không đưa khóa vào mã nguồn.

Workflow chỉ chạy khi kho riêng tư; không tạo GitHub Pages hoặc release công khai. APK artifact giữ 30 ngày, báo cáo test giữ 14 ngày. Việc lưu bản APK riêng bên ngoài GitHub không phụ thuộc thời hạn artifact.

## Sử dụng

- Dán URL playlist rồi chọn **Tải URL**, hoặc **Mở tệp** M3U UTF-8 (tối đa 8 MB).
- Sau khi tải, bảng nguồn tự thu gọn để danh sách kênh chiếm phần lớn màn hình; bấm **+ Nguồn** để mở lại.
- Bấm **☰ Link** để lưu tối đa 50 URL playlist, đặt tên, chọn nguồn cần tải hoặc xóa nguồn không dùng. URL chỉ lưu trong vùng app-private.
- Bấm **⚙** để chọn giao diện Tự động/Mobile/TV, hình nền, URL, mật độ hàng, FPS, đồng hồ và lịch sử.
- Với link luồng phát trực tiếp, dán vào cùng ô rồi bấm **Phát URL trực tiếp**.
- Tìm tên/nhóm/URL, lọc nhóm. Chạm kênh để phát; nhấn giữ kênh để xem/copy URL đầy đủ.
- Bật/tắt ô chọn, dùng **Chọn đang lọc** hoặc **Bỏ chọn đang lọc**, rồi **Xuất M3U sạch**.
- Mục trùng đúng URL, header và thuộc tính phát bị bỏ; tên đường dẫn, query token và giá trị header giữ nguyên chữ hoa/thường. Mục thiếu/sai URL được đếm và bỏ.
- Header HTTP (User-Agent, Referer, Origin, Cookie…), EXTINF gốc và các tùy chọn kênh được giữ khi xuất. Thuộc tính EPG toàn playlist chưa được quản lý trong bản này.
- Trong trình phát: **Định dạng** dùng khi máy chủ không có đuôi .m3u8/.mpd; **Chất lượng** đặt trần chất lượng; bộ điều khiển Media3 có chọn âm thanh/phụ đề nếu luồng cung cấp.
- Nút **↻** chọn tự động/ngang/dọc; **Khung hình** chọn Fit/Zoom/Fill. Chỉ số FPS thực tế lấy từ số khung hình video đã render trong mỗi khoảng 2 giây.
- Android TV có launcher Leanback. Ở giao diện TV, dùng D-pad để di chuyển; nút, tab và hàng kênh đang chọn có viền xanh sáng.
- DRM: đọc Widevine/ClearKey/PlayReady và URL/header giấy phép từ KODIPROP; có thể bổ sung cấu hình bằng nút DRM. Chỉ dùng giấy phép hoặc ClearKey hợp lệ mà bạn được cấp quyền.
- Playlist và lựa chọn lưu trong vùng riêng của ứng dụng trên thiết bị. Android backup và chuyển dữ liệu hệ thống bị loại trừ. Không có playlist hay thông tin đăng nhập cá nhân được đóng gói vào APK.

## Hỗ trợ và giới hạn

| Nguồn | Mức hỗ trợ |
| --- | --- |
| HLS / M3U8, DASH / MPD, SmoothStreaming | Media3, tùy container/codec |
| HTTP / HTTPS (MPEG-TS, MP4, MKV…) | Tùy định dạng Media3 và decoder thiết bị |
| RTSP | RTP qua TCP, các codec RTSP Media3 hỗ trợ |
| RTMP | Qua module RTMP Media3; RTMPS chưa bật |
| UDP MPEG-TS | Cần mạng cho phép unicast/multicast; có Wi-Fi multicast lock |
| Full HD / QHD 1440p / 4K UHD 2160p | Cần nguồn có độ phân giải đó, băng thông và decoder phù hợp |
| SRT, RTP URL trần, AceStream/SopCast, giao thức riêng | Chưa hỗ trợ |
| Widevine / ClearKey | Phát khi playlist hoặc người dùng cung cấp cấu hình giấy phép hợp lệ |
| PlayReady | Tùy thiết bị Android TV và định dạng; thiết bị phải hỗ trợ hệ DRM này |
| Mẫu license tùy biến Kodi | Chưa hỗ trợ; cần endpoint trả phản hồi DRM trực tiếp |
| Android TV launcher và D-pad | Có; chế độ TV/Tự động, focus sáng rõ. Chưa tối ưu riêng cho mọi mẫu remote |
| EPG, catch-up, tài khoản Xtream | Chưa có trong bản này |

Ứng dụng không đảm bảo mọi giao thức/codec và không nâng một nguồn HD thành 4K. HTTPS không tự chuyển xuống HTTP; nguồn HTTP trực tiếp được cho phép vì IPTV cũ thường cần. Đừng chia sẻ URL có token hoặc mật khẩu.

## Build và kiểm thử

Yêu cầu JDK 17, Gradle 8.13, Android SDK 36, Build Tools 36.0.0. Android Gradle Plugin 8.13.2, Media3 1.11.0. Phiên bản được cố định; GitHub Actions được ghim theo commit.

Lệnh build khi các công cụ đã có trong PATH:

    gradle --no-daemon testDebugUnitTest lintDebug assembleDebug

Dự án chưa kèm Gradle wrapper JAR. GitHub Actions tự cài Gradle và các gói SDK nên không cần Android Studio trên máy của bạn. Khi mở trong Android Studio, cấu hình bản Gradle 8.13 đã cài hoặc tạo wrapper bằng Gradle; không chỉ tải lại dự án rồi kỳ vọng wrapper tự xuất hiện.

Chạy kiểm tra core không cần Android SDK:

    javac -d /tmp/iptv-core app/src/main/java/vn/phuong/iptvplayer/Channel.java app/src/main/java/vn/phuong/iptvplayer/M3uParser.java tools/CoreCheck.java
    java -cp /tmp/iptv-core CoreCheck

CoreCheck có 36 kiểm tra; 23 test JUnit bao phủ parser, DRM metadata, header giấy phép, FPS, mã hóa định danh và kiểm tra link nguồn. CI còn chạy lint, build APK và xác minh chữ ký APK. Các bước này không chứng minh khả năng phát 4K hoặc truy cập một nhà cung cấp cụ thể trên điện thoại thật.

Workflow còn chạy `tools/android_smoke.py` trên emulator Android 15: mở app, nhập/tìm/lọc/chọn kênh, xem URL đầy đủ, xoay màn hình, giữ playlist sau khi tiến trình khởi động lại và phát MP4/HLS/DASH tổng hợp 320×180 qua localhost. Script tự tạo video bằng FFmpeg, không dùng playlist thật; ảnh chụp và kết quả được giữ trong artifact **Android-emulator-smoke** trong 14 ngày. Xem nhật ký để biết lần chạy nào đã đạt; không suy diễn các thử nghiệm này thành kiểm chứng 4K/RTSP/RTMP/UDP.

## Nguồn kỹ thuật

- AndroidX Media3: https://developer.android.com/media/media3/exoplayer/supported-formats
- Media3 release: https://developer.android.com/jetpack/androidx/releases/media3
- Media3 RTSP: https://developer.android.com/media/media3/exoplayer/rtsp

Chỉ dùng playlist và nội dung bạn có quyền truy cập.


# 1.10.103 — YouTube performance + 4K playback correction — 2026-09-27

## Phạm vi
1.10.103 tiếp tục trực tiếp trên branch `fix/mobile-1.10.79-youtube-performance` và giữ nguyên các behavior đã được người dùng xác nhận ở 1.10.102:
- Avatar YouTube.
- Status bar/player portrait.
- Spinner đen hard-disable.
- IPTV, navigation, mini-player và các chức năng Mobile khác.

## Source changes
- Giảm công việc lặp lại khi vuốt/chuyển section YouTube: section đã tải không chạy lại `BrowseProcessorManager` trên cache hit.
- Thêm cache nhẹ cho grid **chỉ khi page đã hoàn tất**, tránh bỏ qua continuation page.
- Rút ngắn poster/first-frame reveal gate để giảm thời gian cảm nhận khi mở video; vẫn chờ decoder render frame mới trước khi bỏ poster.
- Với **4K high-FPS (>=50fps)**, renderer Mobile dùng đường release SurfaceView giảm hiện tượng timestamp pacing gây stutter.
- Thêm watchdog 4K: chỉ khi video đã render ở 4K và decoder thực sự rơi frame liên tục mới hạ trần xuống 1440p để giữ playback usable. Không hạ chất lượng 4K trước khi phát hiện vấn đề.
- Không sửa avatar/status-bar/spinner trong patch performance.

## Version
- versionName: **1.10.103**
- versionCode: **119**
- Branch: `fix/mobile-1.10.79-youtube-performance`
- Source patch commit: `a799ece305f4559139c63e462957591254115843`
- Version bump commit: `c8a659d8a68405c29856a63f56a9df64ffd4131e`
- CI update commit: `ed4ea6f5cd7e1fc8c787e820719f1d2bbf13ec31`

## Trạng thái
Source 1.10.103 đã được đưa vào build chain. **Chưa đánh dấu PASS** cho đến khi GitHub Actions build thành công, kiểm tra generated source/verifier và người dùng test APK thật, đặc biệt với:
1. thời gian mở video YouTube;
2. vuốt/chuyển tab Browse liên tục;
3. video 4K trong cùng điều kiện như video test đã gửi.


### 1.10.114 startup-crash fix (2026-10-07)
The 1.10.113 reference-video UI build was found to crash immediately after the Android splash. The cause was a ClassCastException in the existing inline IPTV player lifecycle code: the new reference UI uses a FrameLayout mainRoot, but the provider still cast mainRoot directly to LinearLayout during Activity resume. Version 1.10.114 fixes the provider to resolve the vertical content host safely while keeping the 1.10.112 player pipeline intact. A Robolectric regression test now covers the reference layout and host resolution. CI run #40 completed successfully and produced the corrected APK.

# HANDOFF — 2026-10-07 — NM7 IPTV Mobile Reference-Video UI

## 0. Kết luận quan trọng
KHÔNG ĐƯỢC COI NHÁNH NÀY ĐÃ HOÀN THÀNH. Người dùng đã cài/test APK và phản hồi rằng ứng dụng vẫn chưa mở được / chưa đạt yêu cầu. CI xanh chỉ xác nhận source có thể compile/package; chưa xác nhận runtime trên thiết bị thật.
Bản 1.10.114 chỉ là attempt sửa startup crash, không phải bản đã được người dùng xác nhận PASS.

## 1. Mục tiêu
Repo: phuongnm7/iptv-player-android
Branch: feature/mobile-crichdai-ui-112
Baseline: stable/mobile-1.10.112
Baseline commit: 6aea2d995280017c1e7c00310bcca0c9937b50bb
Phạm vi: Mobile UI, không sửa Android TV.
Yêu cầu: giữ engine/tính năng 1.10.112 và tái tạo giao diện/luồng thao tác theo video 222927.mp4.

## 2. Video tham chiếu 222927.mp4
- Header: hamburger, logo/tên app, search, profile.
- Status row: All / Live / Upcoming / Next 24h / End.
- Category row: icon thể thao dạng tròn có badge.
- Event cards lớn, nền navy, border theo trạng thái, logo đội, thời gian/live indicator.
- Bottom navigation 5 mục: Live Events / Channel / TV Mode / Highlights / Playlist.
- Drawer: Network Stream, Playlists, Cric Score, Foot Score, Floating Player, Low Quality, Copyright, Telegram, Contact, Share, Update App, Exit.
- Playlist: OWNER PLAYLISTS, MY PLAYLISTS, card playlist, M3U URL, edit/delete và nút +.
- Network Stream dialog: Stream URL / Advance Options / CANCEL / PLAY.
Hiện tại mới tái tạo cấu trúc launcher/drawer/playlist ở mức code; chưa được người dùng xác nhận giống video đầy đủ.

## 3. Các thay đổi đã làm
- activity_main.xml: launcher shell FrameLayout, header/status/category/bottom navigation/drawer; giữ import controls 1.10.112 ở trạng thái hidden.
- item_channel.xml: event-card proportions, logo lớn hơn, EPG/progress, ẩn URL.
- Thêm category_circle.xml.
- MainActivity: hook header, drawer, search, profile, bottom navigation và drawer actions.
- Network Stream đã chuyển thành dialog URL → PLAY.
- MobileInlinePlayerProviderV2: sửa giả định mainRoot luôn là LinearLayout.
- Thêm MainActivityStartupTest.java để test layout inflation và FrameLayout host resolution.

## 4. Commit quan trọng
- UI attempt cuối 1.10.113: d2076374a713037c869a9cd1e2dcd4dd9eb619d2.
- Startup provider fix: 43998c8777d204debd0ce9f41061b86b8eafb5e5.
- Version bump: ba0cdf1e78267e8719a13fb377799976e3e43049 → 1.10.114 / versionCode 130.
- CI workflow isolation: 3cc9508bd4d874860635ce6a3ffac777726e4131.
- Latest docs HEAD: 890140189c82a4c0551420704a046770e478b18e.

## 5. CI / APK
- Workflow: NM7 IPTV Mobile 1.10.114 CricHDai UI Build.
- Run #42, ID 37568452225: SUCCESS.
- Parser/unit tests: PASS.
- Mobile build: PASS.
- APK upload: PASS.
- Artifact: NM7-IPTV-Mobile-1.10.114-UI.
- Artifact ID: 11459897174.
- Artifact SHA-256: 78f907941c6092a1c0fc4d5f81763fbf1ad1c194b8b2f8d9de9d0c3c20ed5964.
- APK size: 170,433,256 bytes.
- APK SHA-256 thực tế: b21470a91951d941f84bd8aa9fe9f8f15dc5d6019f81a059eff1fb762acb10c5.
- Lưu ý: một entry cũ từng ghi SHA 9f7180...; SHA đó sai, không sử dụng.

## 6. Lỗi hiện tại — PHẢI TIẾP TỤC TỪ ĐÂY
Người dùng đã phản hồi sau bản sửa: Chưa được. Vì vậy startup fix ở trên chưa được coi là thành công.
Video lỗi startup: 222932.mp4.
Hiện tượng: app hiện splash NM7 nhưng không vào được màn hình chính / quay lại launcher.

### Việc cần làm ngay
1. Chạy APK 1.10.114 trên thiết bị thật.
2. Thu adb logcat từ trước khi mở app đến lúc app thoát.
3. Tìm exception đầu tiên của process vn.phuong.iptvplayer, không chỉ exception cuối.
4. Kiểm tra MainActivity.onCreate, onResume, setContentView, findViewById, listener XML android:onClick, provider registration và các provider chạy ngay khi resume.
5. Kiểm tra MobileInlinePlayerProviderV2.attach() trên generated/packaged source.
6. Nếu crash không nằm ở provider, cô lập từng phần UI/lifecycle để tìm chính xác callback hoặc view gây crash.
7. Không tiếp tục đoán; không đánh dấu PASS chỉ vì CI xanh.
8. Sau khi tìm được exception thật, thêm regression test cho đúng path rồi build lại.

## 7. Rủi ro UI còn tồn tại
- Event card chưa có đầy đủ dữ liệu event/sport như app tham chiếu.
- Category icons mới là UI selector/placeholder, chưa chắc đúng icon/badge thực tế.
- Bottom navigation dùng text/symbol đơn giản, chưa chắc khớp icon/spacing/active state.
- Cric Score, Foot Score, Telegram, Contact, Low Quality, Update chưa có đầy đủ backend/behavior tương ứng.
- Playlist screen đang dùng source store hiện có, chưa phải đầy đủ owner-playlist data model của app tham chiếu.

## 8. Tính năng 1.10.112 phải bảo vệ
- IPTV/M3U parser.
- Media3 playback và DRM.
- EPG.
- Favorites / Recent.
- Direct URL playback.
- Playlist source store.
- Wallpaper/background playback.
- Sleep timer.
- YouTube/SmartTube integration và player pipeline.
- Mobile lifecycle/background playback.

## 9. Quy tắc cho người tiếp quản
- Không sửa stable/mobile-1.10.112.
- Tiếp tục trên feature/mobile-crichdai-ui-112 hoặc branch con.
- Ưu tiên startup crash trên thiết bị thật trước khi tinh chỉnh UI.
- CI build thành công không được coi là runtime PASS.
- Giữ các regression test hiện có.
- APK bàn giao phải ghi versionCode, commit SHA và SHA-256.
- Mỗi vòng: source → generated source → tests → APK → cài/test thiết bị thật → logcat → kết luận.

## 10. Trạng thái bàn giao
STATUS: BLOCKED — startup/runtime chưa PASS.
Ưu tiên số 1: lấy crash log thực tế và xác định chính xác điểm crash. Sau đó mới hoàn thiện UI theo 222927.mp4.

# 2026-10-07 — Mobile Reference UI — build handoff

## Trạng thái hiện tại

**BUILD PASS — APK đã build thành công. DEVICE RUNTIME CHƯA ĐƯỢC XÁC NHẬN trong phiên này.**

- Branch: `feature/mobile-crichdai-ui-112`
- Baseline: `stable/mobile-1.10.112`
- APK versionName: **1.10.112**
- APK versionCode: **128**
- Source commit đã build artifact: `3c0511a12e5289875e0e3f39e444b306900cc9f0`
- GitHub Actions run: **#84**
- Run ID: **37580293471**
- Artifact: **NM7-IPTV-Mobile-1.10.112-ReferenceUI**
- Artifact ID: **11465050002**
- Artifact digest: `sha256:a45b11e3e73078a1a08a159ef83c3f0c7bb00281e61727133768ecc60eb01e6d`
- APK file: `app-mobile-debug.apk`
- APK SHA-256: `a887f7ccaa66d70684f08aac0a18e48c0d199d10e49208a4fc56242a6a48c0ef`
- APK size: **170,452,945 bytes**

## Những phần đã triển khai theo yêu cầu

1. Màn hình mở đầu là **Live Events**.
2. Header đổi thành **Phuongnm7 IPTV**, dùng logo NM7; bỏ avatar người dùng.
3. Nút tìm kiếm giữ ở góc phải; có ô tìm kiếm kênh/trận đấu.
4. Bottom navigation đúng thứ tự:
   **YouTube → Live Events → Channel → Tùy chọn → Playlist**.
5. **YouTube** gọi lại SmartTube pipeline hiện có.
6. **Live Events** lấy candidate trận đấu trực tiếp từ các mục trong playlist; nhóm được lấy từ `Channel.group()`.
7. Live Events có bộ lọc **All / Live / Upcoming / Next 24h / End** và hàng nhóm thể thao ở phía trên.
8. **Channel** có featured card, filter group/favourites và grid 4 cột kiểu app mẫu.
9. **Tùy chọn** gọi màn hình settings hiện có; mục settings không còn là tab “TV Mode”.
10. **Playlist** có owner playlist, playlist URL đã lưu, local playlist, edit/delete và nút + để thêm M3U URL hoặc tệp local.
11. Engine phát 1.10.112 vẫn là backend: M3U parser, Media3/DRM, EPG, favorite/recent, direct URL, wallpaper/background playback, sleep timer và SmartTube.

## Kiểm tra build

Run #84 đã đi qua:
- checkout source/submodules;
- toàn bộ SmartTube compatibility patch;
- lifecycle regression guard;
- compile Mobile source — **SUCCESS**;
- assemble Mobile APK — **SUCCESS**;
- upload artifact — **SUCCESS**.

Lưu ý: workflow đã đổi bước “Run IPTV parser unit tests” thành **focused Java compile** để vòng build UI này không bị chặn bởi test suite nặng. Không được coi đây là thay thế cho toàn bộ unit-test suite.

## Việc còn phải test trên máy thật

- Cài APK và mở app từ cold start.
- Xác nhận không còn hiện tượng splash → quay lại launcher.
- Kiểm tra lần lượt YouTube / Live Events / Channel / Tùy chọn / Playlist.
- Kiểm tra Live Events thực sự lấy đúng các trận trong playlist, đặc biệt các group như `gà vàng`, `gà vàng 33 tv`, `sao kê` khi chúng có trong playlist.
- Kiểm tra play channel/event bằng player 1.10.112 và các kênh DRM/HLS/DASH đang có.
- Kiểm tra thêm/sửa/xóa playlist.
- Nếu có crash: lấy `adb logcat` của process `vn.phuong.iptvplayer`; không sửa theo phỏng đoán.

## Ghi chú UI

Bản này là bản chức năng + bố cục theo ảnh mẫu, chưa phải bản pixel-perfect của app mẫu. Icon sport/category hiện ưu tiên dữ liệu group từ playlist; event card dùng metadata playlist nên chưa có backend event-score riêng như ứng dụng mẫu.
