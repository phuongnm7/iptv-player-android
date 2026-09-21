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
