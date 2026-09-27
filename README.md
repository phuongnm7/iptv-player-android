# NM7 IPTV Mobile — TRẠNG THÁI HIỆN TẠI — 2026-09-27

> **1.10.102 đã BUILD THÀNH CÔNG và người dùng xác nhận trên thiết bị thật: avatar, status bar/player portrait và spinner đen đã được xử lý. Các lỗi còn lại là YouTube video load chậm, Browse/swipe chậm và 4K playback lag.**

## Kết quả test mới nhất

Người dùng xác nhận **1.10.102 đã xử lý được**:
- **Avatar YouTube:** PASS trên thiết bị thật.
- **Status bar/player portrait:** PASS, không còn đè lên player.
- **Spinner đen:** PASS, không còn xuất hiện khi mở video.

Các lỗi còn lại cần xử lý:
- **YouTube video load:** vẫn chậm.
- **YouTube Browse / vuốt chuyển tab:** vẫn lag/chậm.
- **YouTube 4K:** video 4K bị lag/giật, có video gần như không xem được như video test người dùng gửi.

## Mốc bắt buộc cho vòng sửa tiếp theo

- Dùng **1.10.97** làm mốc hành vi cho **avatar + status bar/player portrait + spinner**.
- **Không** dùng 1.10.98/1.10.99 làm behavioral reference cho các phần trên vì đã có regression.
- Performance chỉ được tác động vào **video loading, Browse/tab loading, swipe responsiveness, data/network scheduling và RecyclerView/render workload**.
- Không thay đổi IPTV, avatar, status-bar/player layout hoặc spinner khi chỉ đang tối ưu performance.
- Không đánh dấu PASS chỉ vì CI build thành công; phải có test thực tế trên thiết bị.

## Kết quả build 1.10.102

- **GitHub Actions:** `NM7 Mobile Final Build #731` — **SUCCESS**
- Commit build: `c676fec63965d6933fda6572ff827ce9466e1cbf`
- Thời gian workflow: khoảng **10m 7s**
- Artifacts: **3**
- Artifact Mobile: `NM7-IPTV-Mobile-FINAL`
- **Trạng thái:** build đã thành công; **chưa kết luận runtime PASS** cho avatar, status bar, spinner, video load hoặc Browse/swipe cho đến khi test trên thiết bị thật.

## Phiên bản/source hiện tại

- Version: **1.10.103**
- versionCode: **119**
- Branch: `fix/mobile-1.10.79-youtube-performance`
- Current HEAD: `4c7cf9fd55c8b689e0cac0c9aaf5049599bf9e41`
- Commit trước đó chứa source correction: `c676fec63965d6933fda6572ff827ce9466e1cbf`

## Hướng tiếp tục

1. Khôi phục chính xác generated/runtime behavior của **1.10.97** cho status bar, avatar và spinner.
2. Sau khi các phần này được bảo toàn, tối ưu riêng video load và Browse/swipe.
3. Build CI, kiểm tra generated source và artifact.
4. Người dùng test APK thật; chỉ sau đó mới chốt baseline mới.

Chi tiết đầy đủ nằm trong `PROGRESS.md`.

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
