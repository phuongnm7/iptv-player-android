# Nhật ký tiếp tục dự án — IPTV Player Android

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
