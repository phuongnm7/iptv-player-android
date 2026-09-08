# Nhật ký tiếp tục dự án — IPTV Player Android

Cập nhật: 2026-09-08. Lưu file này trong GitHub riêng tư để nối lại dù phiên trò chuyện hoặc thư mục tạm mất.

**Trạng thái hiện tại:** APK đã build thành công ở Build 2. Đang bổ sung kiểm tra hành vi trên emulator; chưa xác nhận smoke test hoặc phát trên máy thật.

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

- CoreCheck chạy bằng Java thật: PASS 24 assertions.
- Có 10 test JUnit; tác vụ testDebugUnitTest đã chạy thành công ở Build 2.
- 15 XML parse hợp lệ, ID giao diện ngang/dọc khớp.
- Workflow YAML parse hợp lệ.
- Có APK debug-signed ở Build 2; chữ ký APK v1/v2 xác minh thành công.
- Chưa kiểm thử giao diện trên emulator hoặc phát video trên điện thoại thật tại mốc ghi này.

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

### Build 3 — bổ sung kiểm tra trên emulator

- Script tools/android_smoke.py dùng adb và máy ảo Android 15; fixtures playlist và video MP4/HLS/DASH 320×180 được tạo cục bộ.
- Kiểm tra import, trùng/thiếu URL, tìm/lọc/chọn, URL đầy đủ, ngang/dọc, import lỗi không làm mất dữ liệu, khôi phục sau process restart và giao thức không hỗ trợ.
- Phát mẫu qua localhost được adb reverse; không dùng nội dung/credential của bên thứ ba.
- Workflow đã chuẩn bị thêm action emulator pin commit a421e43855164a8197daf9d8d40fe71c6996bb0d và artifact bằng chứng riêng tư.
- Chưa có kết quả Build 3 tại thời điểm ghi mốc này. APK được upload trước smoke test nên nếu smoke lỗi phải phân biệt lỗi hành vi với lỗi tạo APK.

## Làm tiếp

1. Xác minh repo còn private và HEAD hiện tại để không ghi đè thay đổi của người dùng.
2. Commit smoke script/workflow/nhật ký rồi theo dõi Build 3.
3. Đọc log và artifact ảnh thực; sửa lỗi nếu có. Không tắt lint/tests để che lỗi.
4. Tải artifact APK của đúng run đã kiểm thử, kiểm tra checksum và lưu bản cài đặt lâu dài.
5. Bàn giao APK và cập nhật nhật ký với commit/run cuối, kết quả và vị trí file.
6. Kiểm thử trên điện thoại thật bằng playlist được người dùng cấp; xác minh riêng độ phân giải và từng giao thức chưa thử.

## Ghi chú quan trọng

- Máy làm việc hiện tại tải SDK trực tiếp bị chặn quyền mạng; user đã chấp thuận dùng GitHub Actions thay thế. Không tiếp tục tìm cách vượt chặn.
- Có thể đọc/chỉnh repo qua GitHub connector. Git data API tạo tree → commit → update_ref (force=false); luôn dựa trên tree/HEAD hiện có.
- Artifact APK sẽ là debug-signed để cài thử cá nhân, chưa phải release Google Play.
- Các runner khác nhau có thể sinh debug keystore khác; trước khi gỡ bản cũ để cài bản có chữ ký khác, phải xuất playlist.
- EPG toàn playlist, Xtream login, SRT/RTP URL/RTMPS, DRM và TV launcher chưa hỗ trợ.
- Không coi việc biên dịch thành công là chứng minh phát 4K/mọi luồng. Không lưu secret trong nhật ký hoặc source.
