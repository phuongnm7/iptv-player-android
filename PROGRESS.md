# Nhật ký tiếp tục dự án — IPTV Player Android

Cập nhật: 2026-09-08. Lưu file này trong GitHub riêng tư để nối lại dù phiên trò chuyện hoặc thư mục tạm mất.

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
- Có 10 test JUnit; chưa chạy được JUnit Android trước khi build SDK hoàn tất.
- 15 XML parse hợp lệ, ID giao diện ngang/dọc khớp.
- Workflow YAML parse hợp lệ.
- Chưa có APK. Chưa kiểm thử giao diện trên emulator hoặc phát video trên điện thoại thật.

## Lịch sử build

### Build 1 — thất bại ở môi trường SDK

- Run: https://github.com/phuongnm7/iptv-player-android/actions/runs/34204423381
- Run ID: 34204423381, job ID: 101990471708.
- Commit: 60c57efd6abfe06767965f8e06abbf18361d7281.
- Java 17 và Gradle 8.13 đã cài thành công.
- Bước Install Android SDK packages thất bại: sdkmanager: command not found, exit 127.
- Chưa chạy compiler/test/lint; không phải lỗi mã Java đã xác định.
- Cách sửa đang thực hiện: thêm bước cài Android command-line tools trước khi gọi sdkmanager; không dựa vào image runner đã có SDK.

### Build 2 — bản sửa môi trường đã chuẩn bị

- Đã xác minh lại kho private và HEAD vẫn là commit app đầu, không có thay đổi của người dùng bị ghi đè.
- Thêm android-actions/setup-android v3, pin commit 9fc6c4e9069bf8d3d10b2204b1fb8f6ef7065407.
- Cài command-line tools 12266719 và platform-tools, thiết lập PATH/ANDROID_HOME trước khi cài API 36 và build-tools 36.0.0.
- Bản sửa và file nhật ký này được lưu cùng một commit. Chưa có kết quả build mới tại thời điểm ghi mốc này.

## Làm tiếp

1. Xác minh repo còn private và HEAD hiện tại để không ghi đè thay đổi của người dùng.
2. Thêm bước thiết lập SDK, commit cùng nhật ký này rồi theo dõi run mới.
3. Đọc log thực; sửa lỗi build/test/lint. Không tắt lint/tests để che lỗi.
4. Khi build thành công, xác minh chữ ký APK, tải artifact IPTV-Player-1.1-APK, kiểm tra checksum.
5. Nếu có điều kiện, kiểm thử emulator/máy thật; báo chính xác những gì chưa kiểm chứng.
6. Bàn giao APK và cập nhật nhật ký với commit/run cuối, kết quả và vị trí file.

## Ghi chú quan trọng

- Máy làm việc hiện tại tải SDK trực tiếp bị chặn quyền mạng; user đã chấp thuận dùng GitHub Actions thay thế. Không tiếp tục tìm cách vượt chặn.
- Có thể đọc/chỉnh repo qua GitHub connector. Git data API tạo tree → commit → update_ref (force=false); luôn dựa trên tree/HEAD hiện có.
- Artifact APK sẽ là debug-signed để cài thử cá nhân, chưa phải release Google Play.
- Các runner khác nhau có thể sinh debug keystore khác; trước khi gỡ bản cũ để cài bản có chữ ký khác, phải xuất playlist.
- EPG toàn playlist, Xtream login, SRT/RTP URL/RTMPS, DRM và TV launcher chưa hỗ trợ.
- Không coi việc biên dịch thành công là chứng minh phát 4K/mọi luồng. Không lưu secret trong nhật ký hoặc source.
