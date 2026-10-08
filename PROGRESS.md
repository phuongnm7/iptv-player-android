# CẬP NHẬT TIẾN ĐỘ — 1.10.119 — 2026-10-08

## Final video-driven correction — YouTube header/status bar + initial feed layout

Đã đối chiếu trực tiếp video `223364.mp4` và ảnh thiết bị `223375.jpg`.

### Quan sát từ video
- Khi Browse chrome vừa xuất hiện, wordmark YouTube có thể bị vẽ vào vùng status-bar/khung chuyển cảnh.
- Khi chip bar hiện, feed có lúc bắt đầu thấp hơn rất nhiều so với chip bar, tạo vùng trắng lớn.
- Hiện tượng thay đổi theo trạng thái chrome, nên không phải lỗi thumbnail/card đơn lẻ.

### Nguyên nhân ở source
Chuỗi patch cũ trộn hai mô hình:
- v57/v56: Browse overlay + tự thay đổi visibility/padding theo chrome;
- v114: quay lại LinearLayout + AppBarLayout;
- các đoạn scroll-chrome cũ vẫn còn hoạt động.
Ngoài ra v118 trước đó chỉ thay đổi Java nhưng không chuyển source layout sau khi v57/v114 chạy, nên architecture mong muốn chưa thực sự vào APK.

### 1.10.119 đã sửa dứt điểm ở tầng layout
- Patch chạy **sau v114**, sau khi các patch cũ hoàn tất.
- Chuyển Browse cuối cùng sang **CoordinatorLayout**.
- Thêm `nm7_status_bar_spacer` làm child thực sự; spacer nhận chiều cao status bar từ WindowInsets.
- Không dùng root `paddingTop` để đẩy header nữa.
- Header YouTube + chip strip nằm trong AppBarLayout với `scroll|enterAlways`.
- Feed là scrolling sibling của AppBarLayout, dùng `appbar_scrolling_view_behavior`.
- Xóa toàn bộ fake top padding theo chiều cao header.
- Không tự GONE/INVISIBLE hoặc resize AppBar để tạo hiệu ứng chrome.
- Reset scroll **chỉ tại thời điểm section/feed vừa được nạp**, tránh kéo ngược vị trí sau khi người dùng đã cuộn.
- Wordmark tăng từ 76×30dp lên 92×36dp để gần kích thước ảnh YouTube gốc hơn.
- Thêm dependency CoordinatorLayout trực tiếp cho module smarttube.
- Không thay đổi playback/IPTV/network/avatar/spinner/comments/background playback/bottom navigation.

### Build
- Branch: `work/mobile-youtube-ui-v119`
- Version: **1.10.119 / versionCode 135**
- Workflow: **37729414221 — SUCCESS**
- Artifact: `NM7-IPTV-Mobile-1.10.119-FINAL`
- Artifact ID: **11529755026**
- Artifact digest: **d97240bcace9d93e407afeef9566dae668b1a057dc64405be55de22e0843e61d**
- APK SHA-256: **2887bd5f0faaaca1d078262907a46fa2cf085fe8ae1a98109e65048633e1dfe3**
- APK size: **~163 MiB**
- Đã giải nén artifact và xác nhận checksum đúng với `SHA256SUMS.txt`.

### Runtime
Video thực tế đã được dùng làm bằng chứng để thay đổi kiến trúc. Môi trường build không có thiết bị Android thật để tự cài và chụp ảnh runtime, nên chưa đánh dấu pixel-perfect cho đến khi có ảnh/video sau khi cài 1.10.119.

### Baseline
- 1.10.119 lấy nền từ **1.10.118**
- `stable/mobile-1.10.112` không bị thay đổi.

---

# CẬP NHẬT TIẾN ĐỘ — 1.10.118 — 2026-10-08

## Xử lý dứt điểm lỗi status bar + khoảng trắng theo video 223364.mp4

Đã xem video người dùng gửi frame-by-frame. Video cho thấy rõ hai trạng thái:
- Khi chrome YouTube xuất hiện lại, logo bị status bar đè lên một phần.
- Khi chrome mở, một vùng trắng rất lớn xuất hiện giữa dải chip và video đầu tiên.
- Khi chrome ẩn, feed lại dồn lên phía trên. Điều này xác nhận lỗi không nằm ở thumbnail/card mà ở kiến trúc chrome + feed scrolling.

### Nguyên nhân xác định
Chuỗi patch Mobile cũ đã chuyển Browse sang mô hình overlay ở v57 và có code tự thêm top padding theo chiều cao header. Sau đó patch giao diện v114 lại ghi đè layout thành LinearLayout/AppBarLayout nhưng vẫn giữ code scroll-chrome cũ. Hai mô hình này mâu thuẫn, tạo ra blank region và trạng thái header không ổn định.

### 1.10.118 đã sửa
- Browse root chuyển sang CoordinatorLayout.
- AppBarLayout được đặt làm scrolling app bar thực sự.
- RecyclerView là scrolling sibling với appbar_scrolling_view_behavior.
- Header YouTube và dải chip dùng layout_scrollFlags="scroll|enterAlways", để chrome cuộn cùng feed thay vì overlay thủ công.
- Vô hiệu hóa việc v56/v57 tự ẩn AppBarLayout và tự bơm header.getHeight() vào padding RecyclerView.
- Không còn top padding thủ công tạo blank region.
- Status bar được xử lý một lần ở browse_root: lấy inset thật từ WindowInsets và đặt paddingTop của root; không cộng lại vào AppBarLayout.
- Wordmark YouTube vẫn dùng ảnh crop sát từ ảnh YouTube gốc, được đóng gói trực tiếp trong module smarttube để tránh mất resource khi link source-set.
- Không thay đổi playback, IPTV, avatar, spinner, live-chat, background playback hay bottom navigation.

### Build/verification
- Branch: work/mobile-youtube-ui-v118
- Version: 1.10.118 / versionCode 134
- Workflow: 37727225247 — SUCCESS
- Build/assemble: SUCCESS
- Artifact: NM7-IPTV-Mobile-1.10.118-FINAL
- Artifact ID: 11528053938
- Artifact digest: d873badeaf129a069ec2996602f219603105d2d7f613b0ae454d170b79828a66
- APK SHA-256 thực tế sau giải nén artifact: 307c1626505247c71054b3422b60d5b9e3aed1bccfed02b5a0d88280205b87eb
- APK size: 170,424,977 bytes
- Đã xác nhận trong APK có res/layout/browse_activity.xml và res/drawable-nodpi-v4/nm7_youtube_wordmark.png
- Actions: https://github.com/phuongnm7/iptv-player-android/actions/runs/37727225247

### Runtime
Đã phân tích video thực tế do người dùng gửi; chưa có quyền điều khiển thiết bị Android từ môi trường build để tự cài/chụp lại màn hình. Vì vậy chưa tuyên bố pixel-perfect runtime. Tuy nhiên lần này kiến trúc UI đã được sửa đúng theo hiện tượng quan sát được thay vì tiếp tục cộng/trừ padding thủ công.

### Baseline
- 1.10.118 lấy nền từ 1.10.117
- stable/mobile-1.10.112 không bị thay đổi.

---

# CẬP NHẬT TIẾN ĐỘ — 1.10.117 — 2026-10-08

## Sửa lỗi chồng lấn status bar trên Android 15 theo ảnh thiết bị

Ảnh thiết bị mới cho thấy YouTube header vẫn nằm dưới vùng status bar dù 1.10.116 đã thử bật decor fitting. Phân tích cho thấy Android 15/target SDK hiện đại có edge-to-edge bắt buộc, nên không thể chỉ dựa vào setDecorFitsSystemWindows(true).

### 1.10.117 đã thay đổi
- Giữ status bar luôn hiển thị, nền trắng, icon hệ thống màu tối.
- Giữ cửa sổ edge-to-edge ở API 30+, nhưng không để header tự quyết định vị trí.
- Gắn listener trực tiếp lên browse_root và lấy status-bar inset thực tế từ WindowInsets.
- Đưa chính inset đó thành paddingTop của toàn bộ Browse root, bảo đảm logo YouTube + header bắt đầu bên dưới đồng hồ và các icon hệ thống.
- Không cộng inset thêm lần nữa vào AppBarLayout, tránh double-spacing.
- Re-apply trong onWindowFocusChanged hiện có để chống SmartTube/MotherActivity ghi đè fullscreen state.
- Giữ nguyên feed/content layout, playback YouTube, IPTV, avatar, spinner, live-chat, background playback và navigation.

### Build
- Branch: work/mobile-youtube-ui-v117
- Version: 1.10.117 / versionCode 133
- Workflow run: 37725366745 — SUCCESS
- Build/assemble: SUCCESS
- Artifact: NM7-IPTV-Mobile-1.10.117-FINAL
- Artifact ID: 11528100279
- Artifact digest: 9d706af1ccbe2bfef53cb8733579fd4e2d646f5959dc0adffe8d4fff573ac796
- APK SHA-256: 2747650256c38036460d13d00b4be98f1625c69247ca0ecf87dc369d7ab5ba01
- APK size: 170,424,979 bytes
- Actions: https://github.com/phuongnm7/iptv-player-android/actions/runs/37725366745

### Runtime
Chưa có thiết bị Android được điều khiển trực tiếp trong môi trường build, nên chưa thể tự xác nhận ảnh chụp runtime. Tuy nhiên, 1.10.117 đã build thành công từ source với patch status-bar mới.

### Baseline
- 1.10.117 lấy nền từ 1.10.116
- stable/mobile-1.10.112 không bị thay đổi.

---

# CẬP NHẬT TIẾN ĐỘ — 1.10.116 — 2026-10-08

## Device comparison correction — YouTube header + feed gap

Bản 1.10.116 được triển khai sau khi đối chiếu trực tiếp ảnh NM7 người dùng gửi với ảnh YouTube gốc và tập trung đúng hai vùng được khoanh.

### Vấn đề đã xác định
1. Logo/wordmark: bản trước đang dùng một ảnh có vùng trắng thừa và ImageView quá nhỏ, khiến logo YouTube bị thu nhỏ/clipped.
2. Khoảng trắng dưới chip: không phải khoảng cách thiết kế của YouTube; feed RecyclerView đang chịu trạng thái/decor/offset còn sót, làm video đầu tiên rơi xuống rất thấp.
3. Status bar: Browse chưa ép được cửa sổ về layout bình thường ổn định trên thiết bị, nên header có thể bắt đầu sát mép trên và đè vùng status bar.

### 1.10.116 đã sửa
- Dùng wordmark crop sát nội dung từ chính ảnh YouTube gốc người dùng cung cấp, tránh ghép chữ bằng font Android.
- Tăng vùng hiển thị wordmark lên khoảng 76dp × 30dp để giữ tỉ lệ/chiều cao tương ứng ảnh gốc trên thiết bị.
- Portrait Browse dùng normal decor fitting (setDecorFitsSystemWindows(true) trên API 30+) và hiển thị status bar nền trắng với icon tối.
- Không cộng thêm status-bar inset lần hai vào AppBarLayout.
- Dọn toàn bộ RecyclerView.ItemDecoration của feed trên Mobile Browse.
- Ép LinearLayoutManager về reverseLayout=false, stackFromEnd=false và scrollToPositionWithOffset(0, 0).
- Khi section đầu tiên có dữ liệu, thực hiện một lần reset feed về item 0; có thêm post-layout reset để tránh stale layout state.
- Giữ chip typography/chip layout từ vòng trước; không đụng playback/IPTV/avatar/spinner/live-chat/background playback/lifecycle.
- Giữ nguyên bottom navigation của NM7.

### Build
- Branch: work/mobile-youtube-ui-v116
- Version: 1.10.116 / versionCode 132
- Build-only validation: Run 37723730612 — SUCCESS
- Mobile assemble: SUCCESS
- APK artifact: NM7-IPTV-Mobile-1.10.116-FINAL
- Artifact ID: 11527346207
- Artifact SHA-256: 0d2732e8f57db97689e6da621d0c24cdd836697067a7ae3ea82a590ddb518dd4
- APK SHA-256: 25e8c2786a0c9b8eefb52fb10b90aa83892b328c40330f136d4b96ffb9b69142
- APK size: 170,424,987 bytes
- Actions: https://github.com/phuongnm7/iptv-player-android/actions/runs/37723730612

### Validation note
Workflow đầy đủ có bước parser unit-test riêng đã bị runner giữ ở trạng thái chạy quá lâu; build-only workflow đã hoàn tất SUCCESS và tạo APK từ đúng source 1.10.116. Vì môi trường không có thiết bị Android thật nên chưa thể tự chụp màn hình runtime để xác nhận pixel-perfect.

### Baseline
- 1.10.116 lấy đúng nền 1.10.115.
- stable/mobile-1.10.112 không bị thay đổi.

---

# CẬP NHẬT TIẾN ĐỘ — 1.10.115 — 2026-10-08

## YouTube Mobile — final visual alignment pass theo ảnh YouTube gốc

1.10.115 tiếp tục trực tiếp từ **1.10.113 → 1.10.114**, chỉ tập trung vào giao diện Browse YouTube và lỗi khoảng trắng phía dưới dải chip. Không thay đổi playback/decoder, IPTV, avatar, spinner, live-chat hoặc lifecycle.

### Đã xử lý
- Dùng **wordmark YouTube theo ảnh tham chiếu** thay cho việc ghép icon + chữ `YouTube` bằng TextView, để hình dạng logo không phụ thuộc font Android.
- Dải topic chip được chuẩn hóa gần giao diện YouTube gốc: nền đen cho mục đang chọn, xám nhạt cho mục thường, typography sans-serif medium, chữ hoa và không extra font padding.
- Giữ Search ở bên phải và Create/video icon cạnh Search.
- Giữ Explore/compass chip ở đầu dải topic.
- **Xử lý khoảng trắng lớn dưới topic bar:** khi đổi section hoặc batch dữ liệu đầu tiên được nạp, RecyclerView được reset về position 0 và stop stale scroll. Điều này loại bỏ trường hợp section mới kế thừa scroll offset cũ và để lại vùng trắng lớn trước video đầu tiên.
- Giữ nguyên toàn bộ API/ID section và logic dữ liệu hiện có.
- Hệ thống status bar portrait tiếp tục dùng inset thực tế để header nằm dưới thanh status, không chồng lên status bar.
- Bottom navigation **YouTube / IPTV / Tùy chọn** của NM7 được giữ nguyên.

### Source
- Branch: `work/mobile-youtube-ui-v115`
- Version: **1.10.115 / versionCode 131**
- Base: **1.10.114** trên `work/mobile-youtube-ui-v114`
- Patch: `scripts/patch-mobile-v115-youtube-ui.py`
- UI layout source: `scripts/mobile-ui/res/layout/browse_activity.xml`
- Wordmark: `scripts/mobile-ui/res/drawable-nodpi/nm7_youtube_wordmark.jpg`

### CI
- Run **37721283423 — SUCCESS**
- Parser unit test: **SUCCESS**
- Mobile assemble: **SUCCESS**
- APK upload: **SUCCESS**
- Artifact: **NM7-IPTV-Mobile-1.10.115-FINAL**
- Artifact ID: **11526122682**
- Artifact digest: **2224b4b46b9d96452786010a6dd3e92a2d0d3f24d3b51d37474c16d390ca6348**
- APK SHA-256: **802eef97d6f90a9ec1e1741074ae013f35df9b6d339c1c71a43d134e87dc4702**
- APK size: **170,426,201 bytes (~163 MiB)**
- Actions: https://github.com/phuongnm7/iptv-player-android/actions/runs/37721283423

### Runtime
**Chưa có thiết bị Android thực tế trong môi trường build để xác nhận pixel/runtime.** Vì vậy 1.10.115 chưa được đánh dấu stable; người dùng cần cài và đối chiếu với ảnh YouTube gốc. Stable branch `stable/mobile-1.10.112` không bị thay đổi.

---

# CẬP NHẬT TIẾN ĐỘ — 1.10.114 — 2026-10-08

## YouTube Mobile UI — bám sát giao diện YouTube gốc theo 2 ảnh tham chiếu

Đã triển khai trên nền **1.10.113** với phạm vi **UI YouTube Browse + system-bar layout**, không thay đổi đường playback/decoder, IPTV, avatar, spinner, live-chat hoặc lifecycle đã có.

### Đối chiếu ảnh tham chiếu
- Giữ nền trắng của header và status bar.
- Header được sắp lại theo mẫu YouTube phone: **logo YouTube bên trái; Create/video + Search bên phải**.
- Bỏ Mic/Account khỏi hàng trên cùng; các anchor cũ vẫn tồn tại ẩn để không phá code/logic hiện hữu.
- Thêm dải topic chips cuộn ngang bên dưới header.
- Chip đang chọn: nền đen + chữ trắng; chip thường: nền xám nhạt + chữ đen.
- Thêm nút **Khám phá** dạng chip/icon ở đầu dải.
- Không thay đổi ID/API của `browse_tabs`, `browse_grid`, `browse_rows`, `browse_settings`, `browse_toolbar`.
- Sửa system-window handling của `BrowseActivity`: hiện status bar, icon status bar tối trên nền trắng và áp dụng **status-bar inset thực tế vào AppBarLayout** để header không còn nằm/chồng lên vùng status bar.
- Portrait dùng inset thật; landscape không ép thêm top inset.

### Source
- Branch: `work/mobile-youtube-ui-v114`
- Version: **1.10.114 / versionCode 130**
- Base: **1.10.113** @ `work/mobile-app-settings-bottom-v113`
- Workflow: `.github/workflows/nm7-mobile-114-youtube-ui.yml`
- UI patch: `scripts/patch-mobile-v114-youtube-ui.py`

### CI
- Run **37719542318 — SUCCESS**
- Parser unit test: **SUCCESS**
- Mobile assemble: **SUCCESS**
- APK upload: **SUCCESS**
- Artifact: **NM7-IPTV-Mobile-1.10.114-FINAL**
- Artifact ID: **11525530641**
- Artifact digest: **bc4789723d2b6f9add714fd45d36e87dca1247f5deb860d3b9c884208197042e**
- APK SHA-256: **8dadabb459c003d487d1600b13c3bbebb46b044f7ac534045c6933bf1c5acb31**
- GitHub Actions: https://github.com/phuongnm7/iptv-player-android/actions/runs/37719542318

### Runtime status
CI và source verification đã PASS. **Chưa có test trực tiếp trên thiết bị Android trong môi trường này**, nên chưa đánh dấu visual/runtime PASS. APK 1.10.114 được bàn giao để test trên đúng thiết bị đã dùng trong ảnh tham chiếu.

## Nguyên tắc tiếp theo
- Không đổi baseline ổn định `stable/mobile-1.10.112` chỉ vì CI PASS.
- Không chạm các đường playback/UI đã ổn định trong 1.10.113 khi sửa lỗi mới.
- Chỉ đánh dấu 1.10.114 stable sau khi người dùng xác nhận thực tế.

---

# CẬP NHẬT TIẾN ĐỘ — 1.10.113 — 2026-10-08

## Trạng thái chính thức
**1.10.113: source + CI + APK đều hoàn tất; runtime/device chưa được người dùng xác nhận.**

### Source
- Baseline: **1.10.112 / 128**
- Baseline branch: `stable/mobile-1.10.112`
- Baseline commit: `6aea2d995280017c1e7c00310bcca0c9937b50bb`
- Candidate branch: `work/mobile-app-settings-bottom-v113`
- Candidate commit: `1a5ee858ee0bd9a4a44707f9c0f9b3fa81e5a764`

## Phạm vi đã làm
1. **IPTV:** bỏ hàng 4 phím Tất cả/Yêu thích/Tải lại/Gần đây khỏi UI.
2. **IPTV:** đưa Tải lại xuống cạnh Bỏ lọc.
3. **IPTV:** giữ Tất cả/Yêu thích/Gần đây trong menu Tùy chọn IPTV; các anchor cũ vẫn tồn tại ẩn để bảo toàn logic MainActivity.
4. **Bottom navigation:** Tùy chọn ứng dụng tiếp tục là mục riêng bên cạnh YouTube và IPTV.
5. **YouTube:** chỉnh header Mobile về bố cục sáng kiểu giao diện gốc mong muốn, giữ search/micro/account IDs.
6. **Version:** 1.10.113 / versionCode 129.

## CI
- Run **37655352745 — SUCCESS**
- Parser unit test: SUCCESS
- Mobile assemble: SUCCESS
- APK upload: SUCCESS
- Artifact: **11498119053**
- APK: `app-mobile-debug.apk`
- APK SHA-256: `72537004c936a9a358712b28261c9709153252561b57b7952e5fe3131f42019b`
- Artifact digest: `885fccf5dd8e3004ec6962d7d2c2416bebe354452b3c7c3621f1e470e699c75c6`
- [Actions](https://github.com/phuongnm7/iptv-player-android/actions/runs/37655352745)
- [Artifact](https://github.com/phuongnm7/iptv-player-android/actions/runs/37655352745/artifacts/11498119053)

## Files changed
- `app/build.gradle.kts`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/java/vn/phuong/iptvplayer/MobileIptvUi.java`
- `scripts/mobile-ui/res/layout/browse_activity.xml`
- `.github/workflows/nm7-mobile-113-ui-refinement.yml`

## Kỹ thuật cần lưu ý
- `MainActivity` vẫn là source of truth cho playlist, filter, reload và application settings.
- `MobileIptvUi` chỉ xử lý presentation trên Mobile IPTV.
- `HomeTabBar` quản lý bottom navigation.
- `scripts/mobile-ui/...` là source generate-time của SmartTube Mobile UI; CI sẽ clone SmartTube và apply patch chain lại.
- Không xóa các anchor `btnAllChannels`, `btnFavorites`, `btnRecent` cho tới khi refactor xong code MainActivity.

## Chưa chốt stable
`stable/mobile-1.10.112` **vẫn giữ nguyên**. 1.10.113 chỉ trở thành stable sau khi người dùng test máy thật và xác nhận.

## Checklist bàn giao
- Test IPTV layout mới.
- Test reload và filtering.
- Test bottom navigation + application settings.
- Test YouTube header/search/mic/account.
- Regression YouTube player: avatar/status bar/spinner/live chat/background/mini/back.
- Ghi PASS/FAIL vào repo trước khi đổi stable branch.

## Entry point tiếp theo
`work/mobile-app-settings-bottom-v113` @ `1a5ee858ee0bd9a4a44707f9c0f9b3fa81e5a764`.

# HISTORICAL PROGRESS BELOW

# STABLE BASELINE — NM7 IPTV Mobile 1.10.112 — 2026-09-30

## Quyết định ổn định hiện tại

**1.10.112 được chốt làm bản ổn định tạm thời tại thời điểm hiện tại.** Đây là mốc nền bắt buộc cho các bản Mobile tiếp theo.

### Mốc build chính xác

- Version: **1.10.112**
- versionCode: **128**
- Workflow: **NM7 IPTV Mobile 1.10.112 Final Build**
- Workflow file: `.github/workflows/nm7-mobile-112-final.yml`
- GitHub Actions: **Run #15 — SUCCESS**
- Commit build: **`2ee95f0f4906f0aaecc0748f1e13322b5531afa6`**
- Commit message: `ci: prepare 1.10.112 build`
- Thời gian build: **6m 4s**
- Artifacts: **1**
- Artifact: **NM7-IPTV-Mobile-FINAL**
- Stable branch: **`stable/mobile-1.10.112`**

### Quy tắc từ mốc này

1. Mọi build Mobile tiếp theo phải bắt đầu từ **`stable/mobile-1.10.112`**.
2. Không sử dụng một build 1.10.112 khác làm nền.
3. Không lấy `main` làm nền nếu `main` đã có commit sau mốc ổn định này.
4. Không đánh dấu baseline mới chỉ vì CI build thành công; phải có test thực tế của người dùng.
5. Khi một bản mới được xác nhận ổn định, cập nhật lại README/PROGRESS và tạo mốc stable mới.

---

# 1.10.106 — GIỮ PLAYER KHI CHẠY NỀN + GIẢM RELOAD FORMAT YOUTUBE — 2026-09-27

## Kết quả CI
- Version: **1.10.106 / versionCode 122**
- Branch: `fix/mobile-1.10.79-youtube-performance`
- Commit CI: `8347399bb2a8c8937eb3c575e5932df876f5e035`
- **NM7 Mobile Final Build #781 — SUCCESS**
- Run ID: `36296325937`
- Patch + 1.10.102 behavior verifier + lifecycle regression guards: **PASS**
- Unit tests: **PASS**
- APK upload: **PASS**
- Artifact: `NM7-IPTV-Mobile-FINAL`
- Artifact ID: `10923991428`
- Artifact SHA-256: `03f084bf2f8bdc079fecaed3e66e902c1d91a33ef620fb1ddabc584613947e3c`

## Vấn đề người dùng yêu cầu xử lý
Người dùng tiếp tục xác nhận 3 nhóm lỗi:
1. Mở video YouTube vẫn load chậm.
2. Video/Browse còn giật, lag; đặc biệt 4K.
3. Đang phát → chạy nền → mở lại app thì video phải load lại từ đầu.

## Phân tích và thay đổi 1.10.106

### A. Background playback / lifecycle
Đường lifecycle trước đây có thể gọi `blockEngine(true)` và `mPlaybackPresenter.onViewPaused()` khi Activity mất foreground. Điều này có nguy cơ làm mất trạng thái pipeline playback hoặc dispose đường selected-video loading.

1.10.106:
- đánh dấu `mNm7Backgrounding`;
- **không block ExoPlayer** trong HOME/background;
- không gọi `mPlaybackPresenter.onViewPaused()` trong background transition thông thường;
- khi resume, giữ nguyên player/MediaItem nếu không có mini/target restore thực sự;
- chỉ bind lại PlayerView khi target không còn gắn đúng player.

Mục tiêu: quay lại app phải tiếp tục cùng stream, position và buffer, thay vì resolve/load lại.

### B. Format reuse
- Reuse format-info process-local tăng từ **8s → 60s**.
- Không persist signed stream URLs ra disk.
- Đây là tối ưu cho trường hợp mở lại nhanh cùng video hoặc Activity/UI rebinding.

### C. 4K
Không tiếp tục áp đặt `30fps/24Mbps` hoặc fallback 1440p bằng phỏng đoán. Các thử nghiệm v103/v104 cho thấy các thay đổi này không đủ cơ sở để chốt. Nếu 4K vẫn lag, vòng tiếp theo phải lấy dữ liệu runtime: codec, độ phân giải, FPS, bitrate và droppedBufferCount.

## Kết quả build
CI #781 đã build thành công toàn bộ Mobile, gồm APK và unit-test artifact. Đây mới là **build PASS**, chưa phải runtime PASS.

## Kế hoạch test 1.10.106
- Test video YouTube mở mới.
- Test Home/background → quay lại app.
- Test Browse/swipe.
- Test 4K.
- Regression test avatar/status bar/spinner.

Nếu 4K còn lag, không tiếp tục chỉnh ngẫu nhiên; phải truy đúng format/decoder đang được chọn và số frame bị drop.

---

# 1.10.95 — làm theo video mẫu: reset player về màn đen trước khi phát

Ngày 2026-09-25.

Đã đối chiếu trực tiếp hai video người dùng gửi:
- Video mẫu: `219187.mp4` — khi chọn video mới, khung player bỏ nội dung cũ, chuyển sang vùng đen/loading; sau đó thumbnail của video mới xuất hiện rồi mới phát.
- Video bản 1.10.94: `219190.mp4` — khung SurfaceView vẫn giữ hình video cũ trong lúc video mới đang tải, đúng lỗi người dùng phản ánh.

Sửa 1.10.95:
- Dùng cơ chế **PlayerView native shutter/reset** thay vì cố phủ SurfaceView bằng poster ImageView.
- `keep_content_on_player_reset=false` để frame cũ bị ẩn ngay khi player reset.
- Shutter có nền đen.
- Bỏ poster lấy đồng bộ từ card vừa bấm; chỉ cho poster của video mới xuất hiện qua đường Glide sau khi có dữ liệu, khớp trình tự video mẫu.
- Hiện loading indicator ngay khi chọn video mới.
- Không thay đổi IPTV, chat, avatar, background playback, navigation hoặc Android TV.

Version: **1.10.95 / versionCode 111**.

Trạng thái: đã đẩy source + verifier + CI; đang chờ CI build. Chưa coi lỗi đã hết cho đến khi APK được build và người dùng test trên máy thật.

---

# MOBILE 1.10.94 — RETURN TO TESTED 1.10.91 BASELINE

## 2026-09-25

- **Base:** 1.10.91 / versionCode 107, the user-tested stable Mobile baseline.
- Removed experimental **1.10.92 and 1.10.93** from the Mobile patch chain; their poster/buffer changes are not used by 1.10.94.
- 1.10.94 changes only the startup poster reveal decision: after the first rendered frame, the actual ExoPlayer playback position must advance by at least 120 ms before the clicked-card poster is removed.
- Preserved 1.10.91's decoder moving-frame threshold (+3), 180 ms safety window, thumbnail quality, avatar delay, status bar, live chat, IPTV ownership, navigation, and transport path.
- No Android TV changes.
- CI build is required before the APK is considered ready. Device runtime remains unverified until the user tests the APK.

1.10.94 / versionCode 110

---

# MOBILE 1.10.92 / versionCode 108 — SMOOTHER YOUTUBE OPEN — 2026-09-25

## Mục tiêu
Tiếp tục trực tiếp từ baseline người dùng đã xác nhận **1.10.91 / versionCode 107**. Vòng này chỉ xử lý lỗi mở video YouTube còn hơi chậm và hiện tượng nháy/flash thumbnail ở các frame đầu.

## Phân tích lỗi
- 1.10.91 giữ thumbnail chính xác của card cho tới khi decoder có nhiều frame mới.
- Tuy nhiên player vẫn còn **animation 170 ms** trên container trong khi SurfaceView đang handoff, tạo một lớp transition không cần thiết và có thể làm cảm giác hình ảnh bị nháy.
- Ngưỡng chờ trước khi bỏ poster ở 1.10.91 là tương đối bảo thủ: tối thiểu 3 output buffers và khoảng 180 ms.
- Đây là phần được thay đổi; không hạ chất lượng thumbnail.

## Thay đổi 1.10.92
- Loại bỏ animation 170 ms của player container trong lúc SurfaceView handoff.
- Giữ nguyên poster lấy trực tiếp từ **BitmapDrawable/Glide bitmap hiện tại**, không resample thumbnail.
- Chỉ bỏ poster khi decoder đã có **2 output buffers mới** thay vì 3.
- Giảm thời gian an toàn từ **180 ms → 90 ms**, và khoảng chờ sau first-frame từ **120 ms → 60 ms**.
- Giảm `bufferForPlaybackMs` **250 → 200 ms**; không thay đổi rebuffer reserve.
- Không thay đổi IPTV, status bar, avatar, live chat, background playback, navigation hoặc chất lượng thumbnail.

## Version
- versionName: **1.10.92**
- versionCode: **108**
- Branch: `fix/mobile-1.10.83-youtube-player-ui`
- Baseline: **1.10.91 / 107**

## Điều kiện chốt
- CI phải PASS.
- APK phải có đủ ARM64-v8a và armeabi-v7a.
- Sau đó người dùng test thực tế để xác nhận không còn flash thumbnail và thời gian mở video được cải thiện.
- Nếu còn flash, không được tự ý sửa các chức năng ổn định khác; chỉ tiếp tục truy vết SurfaceView/poster handoff.

---

# MOBILE 1.10.91 / versionCode 107 — STABLE BASELINE — USER TEST CONFIRMED — 2026-09-25

## Quyết định baseline

**Người dùng đã test thực tế bản 1.10.91 và xác nhận đây là bản tốt nhất/ổn định nhất hiện tại.** Từ đây, các bản Mobile tiếp theo phải phát triển trực tiếp trên nền 1.10.91.

- versionName: **1.10.91**
- versionCode: **107**
- Branch: `fix/mobile-1.10.83-youtube-player-ui`
- Source commit build: `60f3be09859f608b23b40b066a9da9168f7b0b7b`
- Mobile Final Build: **#611 — SUCCESS**
- Run ID: `36104328880`
- Artifact: `NM7-IPTV-Mobile-FINAL`
- Artifact ID: `10850193056`
- [GitHub Actions #611](https://github.com/phuongnm7/iptv-player-android/actions/runs/36104328880)
- [Artifact APK ARM64/ARMv7](https://github.com/phuongnm7/iptv-player-android/actions/runs/36104328880/artifacts/10850193056)

## Kết quả test thiết bị

Người dùng đã cài/test 1.10.91 trên thiết bị thật và đánh giá đây là bản có chức năng ổn định nhất hiện tại.

Các chức năng cần **giữ nguyên** làm nền:
- IPTV và danh sách kênh; kênh cuối cùng không còn bị thanh YouTube/IPTV đè.
- Status bar Android hiển thị trong player YouTube.
- Avatar video/kênh YouTube hiển thị.
- Live chat hoạt động.
- Video YouTube không còn treo hình ở thumbnail trong khi âm thanh chạy.
- Pipeline tải/phát YouTube đã được cải thiện so với các bản trước.
- Không đưa miniplayer trở lại; giữ kiến trúc hiện tại đã được người dùng test ổn định.

## Lỗi còn lại, không coi là regression của baseline

- Khi mở video YouTube vẫn còn **một nhịp nháy nhẹ khung hình đầu/chuyển cảnh**. 1.10.91 đã giảm hiện tượng này nhưng chưa loại bỏ hoàn toàn.
- Đây là mục ưu tiên tiếp theo, nhưng mọi bản sửa phải **giữ nguyên 1.10.91 làm nền** và không làm ảnh hưởng IPTV/status bar/avatar/live chat.

## Quy tắc phát triển từ 1.10.91

1. Bản kế tiếp phải tăng từ **1.10.91 / 107**.
2. Không quay lại 1.10.75 hoặc các baseline cũ để phát triển tính năng mới.
3. Không sửa các phần đang ổn định nếu không có yêu cầu hoặc bằng chứng regression.
4. Với lỗi nháy mở video, chỉ thay đổi transition/player-loading; không thay đổi hành vi IPTV, status bar, avatar, live chat.
5. CI phải pass trước khi bàn giao APK.
6. Người dùng test thực tế là điều kiện để chốt baseline mới.

---

# MOBILE 1.10.83 / 99 — YOUTUBE PLAYER STATUS BAR + LIVE CHAT + FEED QUALITY + STARTUP POLISH — 2026-09-25

## Yêu cầu người dùng
- Player YouTube dọc không được che thanh status/notification của điện thoại; player phải bắt đầu bên dưới status bar.
- Video YouTube Live phải có khu vực xem live chat giống video mẫu.
- Thumbnail ngoài feed cần rõ nét hơn.
- Avatar kênh trong danh sách video phải hiển thị.
- Tiếp tục giảm thời gian từ lúc chọn video tới frame đầu tiên.
- Thêm hiệu ứng mở video nhẹ giống cách chuyển màn hình trong video mẫu.

## Thay đổi 1.10.83
- Giữ đường window/playback ổn định đã phục hồi ở 1.10.80/1.10.82; **không dùng lại setDecorFitsSystemWindows** từng gây regression khi mở video.
- Ở portrait, yêu cầu Android hiện status bar và lấy inset thật; đặt topMargin của player bằng status-bar inset. Landscape/fullscreen vẫn giữ hành vi fullscreen.
- Giữ panel live chat đã có từ v76 và bật lại SmartTube LiveChatService **chỉ khi metadata có liveChatKey**. Đây là đường chat thật từ SmartTube/YouTube, không tạo dữ liệu giả; hiện hỗ trợ xem chat.
- Mở rộng pipeline avatar để nhận cả URL trực tiếp và object thumbnail/avatar lồng nhau từ MediaService, sau đó chuyển tới VideoCardHolder.
- Thumbnail feed dùng maxresdefault, fallback sddefault rồi URL gốc; yêu cầu decode ARGB_8888 ở mức 1280x720 phù hợp màn hình điện thoại để giảm hiện tượng mờ.
- Thêm animation mở player 170 ms (fade + dịch nhẹ + scale rất nhỏ), chạy song song với network/decoder nên không chặn first frame.
- Giảm nhẹ ngưỡng buffer khởi phát 500 -> 350 ms và rebuffer 1500 -> 1200 ms; giữ nguyên fast format resolver 1.10.75 và selected-request pipeline 1.10.68.
- Mobile-only; không thay đổi IPTV hay Android TV.

## Version / branch
- versionName: **1.10.83**
- versionCode: **99**
- Branch: `fix/mobile-1.10.83-youtube-player-ui`
- Stable baseline vẫn là **1.10.75 / 93** cho tới khi APK mới build SUCCESS và người dùng test thực tế.

## CI
- Workflow đã được cập nhật để build branch 1.10.83 và thêm guard cho status-bar inset, live-chat hook, thumbnail ARGB_8888 và avatar resolver.
- Chưa đánh dấu 1.10.83 stable trước khi CI và test thiết bị hoàn tất.

---

# MOBILE 1.10.77 / 95 — CORRECT PORTRAIT SYSTEM BAR + LIVE CHAT CLOSE

User video re-check found both 1.10.76 UI fixes were ineffective. 1.10.77 replaces the edge-to-edge workaround with explicit portrait WindowInsets/system-bar handling and adds the requested X close control to live chat.

# MOBILE 1.10.75 / 93 — STABLE BASELINE — USER TEST CONFIRMED IMPROVED YOUTUBE LOAD — 2026-09-23

## Trạng thái hiện tại

**1.10.75 / versionCode 93 được người dùng test thực tế và xác nhận tốc độ tải/phát video YouTube đã được cải thiện.** Người dùng cần thêm thời gian để test dài hơn trước khi kết luận mọi trường hợp đã tối ưu hoàn toàn.

Từ thời điểm này:
- **1.10.75 là bản ổn định nhất hiện tại của Mobile.**
- Các bản Mobile tiếp theo phải **phát triển trực tiếp dựa trên 1.10.75**, không quay lại 1.10.72 hoặc các baseline cũ.
- Không thay đổi các phần đã hoạt động ổn định của 1.10.75 nếu không có yêu cầu hoặc bằng chứng regression.
- Tiếp tục giữ phạm vi **Mobile-only**, không đưa Android TV vào vòng phát triển này.

## Kết quả kiểm thử người dùng

- CI Mobile Final **#531 — SUCCESS**.
- Run: 35761975466.
- Commit build: `a383d67c9a5bfb5621afe149b3cfd4cf03f0e96e`.
- Artifact: `NM7-IPTV-Mobile-FINAL`.
- Người dùng xác nhận: **đã cải thiện tốc độ tải video YouTube**.
- Chưa đánh dấu “đã giải quyết hoàn toàn” vì người dùng cần thêm thời gian test thực tế.

## Quy tắc baseline mới

1. 1.10.75 là **stable baseline mới nhất**.
2. Bản kế tiếp phải tăng versionCode/versionName từ 93/1.10.75.
3. Mọi thay đổi mới phải bảo toàn các chức năng, UI, lifecycle và tốc độ đã đạt được ở 1.10.75.
4. Khi sửa lỗi mới, phải xác định nguyên nhân và kiểm chứng CI trước khi bàn giao.
5. Không tuyên bố tốc độ mới đã hoàn toàn ngang IPTV nếu chưa có phép đo/test thực tế tương ứng.

---

# UPDATE 1.10.73 — THUMBNAIL QUALITY CORRECTION — 2026-09-22

- Đã sửa ngay yêu cầu mới: **không hạ chất lượng/kích thước thumbnail** so với 1.10.72.
- YouTube Browse tiếp tục dùng `maxresdefault.jpg` như 1.10.72.
- Đã thêm regression guard: không cho phép `mqdefault.jpg` xuất hiện trong patch tối ưu.
- Các tối ưu tốc độ khác của 1.10.73 vẫn giữ nguyên: không ép transport sang OkHttp và đo riêng thời gian format request/frame.
- Không thay đổi giao diện, chức năng hoặc chất lượng thumbnail.

# MOBILE 1.10.73 / 91 — YOUTUBE LOAD PERFORMANCE OPTIMIZATION — 2026-09-22

## Scope
Tiếp tục trực tiếp từ stable baseline **1.10.72 / versionCode 90**. Mục tiêu của vòng này chỉ là giảm thời gian tải danh sách YouTube và thời gian chờ bắt đầu video. **Không thay đổi chức năng, hành vi, bottom navigation 2 tab YouTube/IPTV hoặc giao diện YouTube của 1.10.72.**

## Source changes
- Bỏ cơ chế ép SmartTube Mobile sang **OkHttp** khi người dùng có cấu hình DNS tùy chỉnh. Cấu hình DNS vẫn được giữ; chỉ bỏ transport override để upstream network stack có thể dùng đường HTTP/2/HTTP/3/QUIC phù hợp khi khả dụng.
- Giữ đường **selected-video format request** của 1.10.68 chạy song song với quá trình khởi tạo PlaybackActivity/decoder; thêm mốc log an toàn `selected_video_start` và `format_ready_ms` để tách thời gian lấy format khỏi thời gian decoder/frame.
- Tối ưu tải danh sách YouTube: card list không còn tự nâng mọi thumbnail lên `maxresdefault.jpg` 1280×720. Mobile one-column feed dùng mục tiêu `mqdefault.jpg` 320×180, vẫn giữ nguyên layout/card/UI.
- Không thêm preload video hàng loạt, không cache URL ký, không tải trước video người dùng chưa chọn để tránh tăng lưu lượng và bộ nhớ.
- Không thay đổi decoder recovery, SurfaceView, lifecycle/ownership, IPTV, Android TV hoặc UI navigation.

## Cơ sở kỹ thuật
Media3 xác nhận lựa chọn network stack ảnh hưởng đáng kể tới streaming performance; Cronet/HttpEngine hỗ trợ HTTP/2 và HTTP/3 over QUIC, trong khi OkHttp hiện hỗ trợ HTTP/2 nhưng không HTTP/3. citeturn6search0turn6search2
Media3 cũng hỗ trợ preload có kiểm soát, nhưng preload quá mạnh sẽ lãng phí băng thông; vòng này chủ động không preload video chưa được chọn. citeturn0search3turn0search5

## Version
- versionCode: **91**
- versionName: **1.10.73**
- Bắt buộc build Mobile-only trước khi coi là hoàn tất.
- Chưa có kết luận runtime cho đến khi CI PASS và người dùng test APK trên thiết bị thật.

## Kịch bản test bắt buộc
1. Mở tab YouTube → đo thời gian danh sách xuất hiện.
2. Cuộn nhanh qua nhiều nhóm video → kiểm tra danh sách không bị spinner kéo dài và thumbnail tải dần ổn định.
3. Chọn video ngay khi thumbnail/list vừa xuất hiện → đo thời gian từ tap đến frame đầu tiên.
4. Mở 5–10 video liên tiếp → so sánh tốc độ với 1.10.72.
5. Mạng Wi-Fi tốt nhưng có độ trễ DNS/CDN → kiểm tra thời gian `format_ready_ms`.
6. Xác nhận UI YouTube, bố cục một cột, search, account, Back, IPTV và toàn bộ lifecycle vẫn giống 1.10.72.
7. Xác nhận avatar channel vẫn là lỗi riêng đã ghi nhận, không đưa vào scope vòng này.

**Trạng thái:** Source đã được đưa lên branch phát triển; đang chờ Mobile Final CI. Không coi build pass là bằng chứng tốc độ đã được cải thiện trên thiết bị thật.

---

# MOBILE 1.10.72 / 90 — STABLE BASELINE — 2026-09-22

## Trạng thái hiện tại

**1.10.72 / versionCode 90 được chốt làm bản ổn định tạm thời (stable baseline) để phát triển các bản Mobile tiếp theo.**

- Mobile Final Build **#502 — SUCCESS**.
- Run: 35699011527.
- Source commit: 771d599fe6be23ebfeb1ac6a319f50c05279501b.
- Artifact: https://github.com/phuongnm7/iptv-player-android/actions/runs/35699011527/artifacts/10682325671
- Artifact ID: 10682325671, dung lượng khoảng 62.13 MB.
- Artifact SHA-256: e3407a549dd46dc0f4151de12693ee13ad249de169bb06e3550249375003e7f7.
- Unit-test artifact: Mobile-unit-test-results, ID 10682720058.
- Lifecycle/source-proof artifact: Mobile-lifecycle-source-proof, ID 10681223908.
- CI đã vượt qua patch SmartTube, lifecycle guards, resource compatibility, build Mobile-only và upload APK.
- Không build Android TV trong run #502. Phạm vi là Mobile.

## Những gì được giữ làm nền

- Bottom navigation đúng **2 tab: YouTube + IPTV**.
- Không có mini-player nhúng trong Mobile build hiện tại.
- Các thay đổi Mobile 1.10.68–1.10.72 và cơ chế lifecycle/ownership hiện tại được giữ nguyên.
- versionCode 90 / versionName 1.10.72 là mốc phiên bản nền.
- Bản tiếp theo bắt buộc tăng lên **versionCode 91 / versionName 1.10.73** trước khi build; các bản sau tiếp tục tăng tuần tự.
- Các bản sửa tiếp theo phải bắt đầu từ baseline 1.10.72 này, không quay về các commit cũ.

## Lỗi còn tồn tại — ghi nhận để xử lý sau

### Avatar kênh YouTube chưa hiển thị

Người dùng đã kiểm tra APK 1.10.72 thực tế và xác nhận **avatar kênh trên các thẻ video YouTube vẫn chưa hiển thị**.

Đây là lỗi đã biết và **không xử lý trong vòng 1.10.72**. Không coi CI PASS là bằng chứng avatar đã hoạt động.

Khi xử lý ở bản sau cần:
1. Giữ nguyên toàn bộ hành vi/UI đang ổn định của 1.10.72.
2. Chỉ truy vết lại đường dữ liệu channelThumbnail → YouTubeMediaItem → Video.channelThumbnailUrl → VideoCardHolder.
3. Không thay đổi thanh dưới 2 tab YouTube/IPTV.
4. Sau khi sửa phải tăng version trước khi build.

## Quy tắc phát triển từ baseline này

- Mỗi APK mới phải tăng **versionCode** và **versionName**.
- Không sửa verifier để chấp nhận lại version cũ.
- Trước mỗi build: kiểm tra version hiện tại, tăng version, đồng bộ verifier nếu verifier có kiểm tra số phiên bản.
- Build phải SUCCESS và artifact APK phải tồn tại mới được coi là build hoàn tất.
- Lỗi avatar hiện được ghi nhận riêng, chưa đưa vào scope sửa của 1.10.72.
- Không thay đổi Android TV khi đang xử lý Mobile.

---

# MOBILE 1.10.68 / 86 — SINGLE PLAYBACK OWNER + SELECTED SOURCE PIPELINE — 2026-09-21

- Người dùng xác nhận 1.10.67 vẫn lỗi qua video `218073.mp4`: mini/player đen và mini báo phiên phát kết thúc. 1.10.67 KHÔNG ổn định; không coi watchdog là giải pháp đầy đủ.
- Phát hiện đường mở video của ViewManager chỉ dùng NEW_TASK trong khi PlaybackActivity là singleTop. Khi Browse/Search nằm trên mini's Activity, chọn video có thể tạo thêm Activity/player, trong khi presenter đã gửi video mới vào player cũ. Hai engine cùng dùng presenter là xung đột lifecycle cụ thể trong mã, cần kiểm chứng lại trên máy sau sửa.
- 1.10.68 thêm REORDER_TO_FRONT + SINGLE_TOP riêng cho Mobile PlaybackActivity, không CLEAR_TOP/CLEAR_TASK. Giữ engine hiện có khi chọn video từ mini.
- Engine cũ không được gửi READY/ENDED/error/seek/track events hoặc onEngineReleased/onFinish vào presenter của owner mới.
- Tăng tốc đường mở lạnh: bắt đầu lấy format cho đúng video vừa chọn trong lúc tạo Activity/engine; gắn kết quả vào owner khi sẵn sàng, tiêu thụ một lần. Chỉ một kết quả tạm trong RAM, không thêm cache URL ký và không tải trước cả feed. Kết quả/lỗi cũ bị loại theo generation, video và owner; đóng/chọn video khác hủy subscription.
- Debounce 300ms việc tắt renderer khi app rời foreground, hủy nếu Activity kế tiếp resume. Keep-alive vẫn bắt đầu ngay; tránh bật/tắt decoder cho chuyển màn hình nội bộ ngắn.
- Dời tải comment preview đến sau frame đầu tiên. Log thời gian format_ready_ms và video_bind_to_frame_ms không chứa URL/token; chưa đo được phần trăm tăng tốc trên điện thoại/mạng thực tế.
- APK YouTube tham khảo có dấu vết AndroidXMedia3/1.11.1 và kiểm tra surface invalid. Chỉ xác nhận thành phần từ APK; không tuyên bố đã khôi phục thuật toán tải/phục hồi độc quyền của YouTube.
- Kiểm thử: thêm các trường hợp phản hồi mạng trước/sau khởi tạo màn hình, callback lỗi/video cũ, owner cũ, hủy rồi thử lại cùng video; kiểm tra Intent và sự kiện ExoController cũ không tác động presenter. CI sẽ xuất XML kết quả test cùng APK.
- Mobile-only; chưa đánh dấu stable và chưa xác nhận đã hết lỗi trên thiết bị.

---

# MOBILE 1.10.67 / 85 — CI BUILD SUCCESS — 2026-09-21

- Mobile Final run **#415**: https://github.com/phuongnm7/iptv-player-android/actions/runs/35589233133
- Source commit: `727023c4c721c923fbffca4dad657dd483979694`.
- 95 structural guards passed; `:app:testMobileDebugUnitTest` executed successfully; build succeeded in 7m 54s.
- Artifact: [NM7-IPTV-Mobile-FINAL](https://github.com/phuongnm7/iptv-player-android/actions/runs/35589233133/artifacts/10634241333), contains signed debug APKs for arm64-v8a and armeabi-v7a.
- arm64 SHA-256: `d8c8ffafc687afb0a97e634f3e741e7d92ced9b732a27bc42b70098d6af60662`.
- armv7 SHA-256: `cf7f4f3a0a5838998f99390bebe485fb89e45e3b10e193cfe5fe564da4488960`.
- **Chưa xác nhận trên thiết bị, chưa stable.** Không coi CI thành công là bằng chứng hết lỗi màn đen/load chậm.

---

# MOBILE 1.10.67 / 85 — FRAME-PROGRESS RECOVERY — 2026-09-21

- Sửa watchdog dùng sai sự kiện first-frame sau mỗi READY: nay đo renderedOutputBufferCount, chỉ khi video thực sự cần hiển thị trên surface hợp lệ. Không tự restart khi pause, buffering, mất audio focus, chạy nền hoặc khóa màn hình.
- Chỉ giữ một tác vụ watchdog; hủy khi release/đổi player/chuyển IPTV/đóng mini. Callback kiểm tra đúng instance và generation thay vì so sánh cùng một field với chính nó.
- Dùng chung ngân sách phục hồi decoder/render theo video: rebind hoặc decoder retry, rebuild, rebuild với giới hạn 720p/4Mbps, rồi báo lỗi để người dùng thử lại. READY và tạo lại engine không xóa ngân sách; 30 giây có frame liên tục mới phục hồi ngân sách.
- Decoder error trong thời gian chờ được xử lý sau 1,5 giây, không bị bỏ qua bởi cooldown. Giữ pause do người dùng; giữ giới hạn video khi tạo lại selector.
- APK YouTube tham khảo có chuỗi Media3 và kiểm tra surface không hợp lệ. Đây chỉ là quan sát thành phần, không phải bằng chứng về thuật toán nội bộ hay mã nguồn YouTube.
- Kiểm tra local: 95 structural guards và 6 kịch bản chạy trực tiếp trên policy Java đã pass. Thêm kiểm thử Robolectric hủy hai tác vụ thật trên Handler; kết quả Android build/test cần đối chiếu CI.
- Chưa xác nhận hết lỗi trên thiết bị; không đánh dấu stable. Cần thử mini/fullscreen nhiều lần, khóa/mở màn hình, đổi IPTV/YouTube, pause và phát liên tục ít nhất 15 phút.
- Giữ nguyên phạm vi Mobile; chưa thay UI hay engine toàn bộ. Lỗi load nguồn/mạng không được kết luận đã hết bởi thay đổi watchdog.

---

# MOBILE 1.10.66 / 84 — CI BUILD SUCCESS — 2026-09-21

## Kiểm tra lỗi GitHub và kết quả

- CI run **#410** đã BUILD SUCCESS cho Mobile-only.
- Lỗi run #408/#409 đã được xác định và xử lý:
  - Run #408: lifecycle verifier không phân biệt `mPlayer.retry()` dùng riêng cho decoder recovery với retry mini.
  - Run #409: sau khi sửa verifier, compiler báo `Player` không có `removeVideoListener()`; đổi observer player sang `SimpleExoPlayer`.
- Run #410 sau hai sửa trên: **Build Mobile only = success**, **Upload Mobile APK = success**.
- Artifact: `NM7-IPTV-Mobile-FINAL`.
- Commit build: `455ae7d1f97ec7b04fffdc5b771e30145f6181d7`.
- Chưa đánh dấu stable cho đến khi test APK thực tế trên máy.

---

# MOBILE 1.10.66 / 84 — FIX DUPLICATE DECODER RECOVERY — 2026-09-21

## Lỗi phát hiện sau 1.10.65

1.10.65 có **hai đường cùng xử lý một decoder error**:
- observer của ExoPlayer gọi `recoverNm7DecoderError()`;
- `Nm7EngineErrorOwner` đồng thời chuyển cùng lỗi vào `ErrorFixerController`.

Hai đường retry/restart có thể chạy cạnh nhau, làm player/surface bị reset chồng và gây lỗi phát.

## Đã sửa

- Bỏ retry decoder trực tiếp khỏi observer.
- Decoder error chỉ đi qua **một owner duy nhất**: `Nm7EngineErrorOwner`.
- Nếu là MediaCodec/Decoder error, owner gọi `recoverNm7DecoderError()` và kết thúc tại đó.
- Các lỗi playback khác vẫn đi qua `ErrorFixerController` như trước.
- Giữ nguyên SurfaceView, render watchdog và các cơ chế phục hồi khác.
- Tăng version thành **1.10.66 / versionCode 84** để không nhầm với APK 1.10.65.

## Commit sửa

- `0f551b3f6b4ad6bc5f1c25d8bd7dd194ea3e3699` — remove duplicate observer decoder recovery.
- `4f65613499ec8d9ad737c622c958300f9ea66e5e` — single-owner decoder recovery.
- Version: **1.10.66 / 84**.

## Trạng thái

Cần chạy lại Mobile-only CI và test đúng kịch bản video đen/decoder failure. Chưa đánh dấu stable.

---

# MOBILE 1.10.65 / 83 — THAM KHẢO YOUTUBE 21.37.42 + HARDEN DECODER/BLACK-FRAME RECOVERY — 2026-09-21

## Bằng chứng app tham khảo

Người dùng cung cấp APK `YouTube_21.37.42_APKPure.apk` để tham khảo cách xử lý lỗi video đen. Phân tích APK cho thấy app tham khảo dùng AndroidX Media3/ExoPlayer (chuỗi build xác định Media3 1.11.1) và Cronet, đồng thời có cả SurfaceView/TextureView trong các đường phát khác nhau. Quan trọng hơn, APK có các đường xử lý lỗi decoder/MediaCodec với retry chủ động, gồm các thông điệp tương ứng `DecoderInitializationException`, `MediaCodec exception`, retry playback và giảm số decoder khi retry không thành công. APK cũng có xử lý trường hợp ExoPlayer vào ENDED nhưng frame cuối bị mất.

## Sửa NM7 1.10.65

Áp dụng nguyên lý tương tự, nhưng không sao chép mã độc quyền của YouTube:

- Thêm playback observer theo từng ExoPlayer instance để bắt trực tiếp `onPlayerError` và lỗi decoder/MediaCodec.
- Khi lỗi decoder/MediaCodec xảy ra: retry tối đa 3 lần, giữ vị trí hiện tại, re-bind video target và tiếp tục phát.
- Ở lần retry thứ 3, giới hạn video tối đa 1280x720 / 4 Mbps để giảm khả năng decoder thất bại do profile/áp lực tài nguyên.
- Nếu chuỗi retry vẫn thất bại, reset engine thay vì để PlaybackActivity rơi vào trạng thái video đen nhưng session vẫn còn.
- Cải tiến render watchdog: không chỉ re-bind surface một lần; theo dõi `onRenderedFirstFrame`, thực hiện tối đa 3 vòng phục hồi khi player READY/BUFFERING nhưng không xuất hiện frame.
- Sau 3 lần không có frame, reset engine để tạo lại decoder/render path.
- Observer được gắn theo đúng instance player và được tháo khi session kết thúc, tránh listener bị nhân đôi sau decoder recreation.
- Giữ SurfaceView của 1.10.64; không quay lại TextureView.
- Không thay đổi luồng IPTV, Android TV hay mốc stable Mobile 1.10.60.

## Commit

- `6e9240880794ff69233ce032856f1249846fc810` — harden decoder/error + black-frame recovery.
- `a0c4a61a749f5657d87961cb5d2b0201a2b3cd0c` — Mobile 1.10.65 / versionCode 83.

## Trạng thái

Source đã sửa và tăng version lên **1.10.65 / 83**. Cần CI Mobile-only build và sau đó test trên thiết bị thật bằng đúng video/kịch bản đã tái hiện. Chưa đánh dấu bản này stable cho đến khi người dùng kiểm tra.

---

# MOBILE 1.10.64 / 82 — XỬ LÝ LỖI VIDEO ĐEN/RENDERER — 2026-09-21

## Cập nhật CI mới nhất
- CI Mobile-only run #397 / 35570031129 đã chạy đến regression guard nhưng fail vì verifier cũ vẫn yêu cầu TextureView.
- Đã xác định đây là lỗi của guard, không phải lỗi compile/source patch.
- Đã sửa verifier để yêu cầu SurfaceView và kiểm tra luôn render watchdog.
- Đã sửa patch-mobile-v37.py để idempotent với pinned source vốn đã là SurfaceView.
- Commit mới nhất: fdceb317daaa0c748495ee8de6db9d2059e66c27.
- CI Mobile-only run #398 / 35570084103 đang build; regression guards đã PASS và build Mobile-only đang chạy.
- Chưa coi 1.10.64 là APK đã xác nhận cho đến khi build/upload hoàn tất.

## Phân tích video mới

Video test 62 giây cho thấy lỗi không chỉ nằm ở `STATE_ENDED`: có nhiều thời điểm Browse/metadata vẫn hoạt động nhưng vùng video YouTube chuyển thành **màu đen**, đồng thời mini-player có lúc hiện trạng thái phát lỗi. Điều này cho thấy cần xử lý cả đường **decoder → video surface**, không chỉ lifecycle/state.

## Sửa 1.10.64
- Thêm render watchdog: nếu player đang READY/BUFFERING + muốn phát nhưng sau 1,8 giây không có frame, thực hiện rebind target và seek về vị trí hiện tại để kích hoạt lại render path.
- Đổi PlayerView video surface của Mobile YouTube từ `TextureView` về `SurfaceView` để giảm lỗi black-frame khi chuyển mini/fullscreen và khi decoder được tái tạo.
- Giữ phục hồi `STATE_ENDED` của 1.10.63.
- Giữ OOM/memory recovery và explicit mini target rebind của 1.10.62.
- Chỉ Mobile, không TV.

## Commit
- `c0b0a048ef3c1e9bb36bf634f4c13b34af380544` — render watchdog.
- `7d1079f5904e0b96175147f4ae4a142b910f1a10` — SurfaceView renderer.
- `f00b5df4d7da0f2ff63e4c192ab70e4fd837edf5` — Mobile 1.10.64 / versionCode 82.

## Trạng thái
Đã sửa source và tăng version. Cần CI build Mobile-only trước khi cài test. Không coi bản 1.10.63 là bản xác nhận cuối cùng.

---

# MOBILE 1.10.63 / 81 — SỬA NGAY LỖI MINI/PLAYER BLACK — 2026-09-21

## Lỗi tái hiện từ video mới
Video người dùng gửi ngày 2026-09-21 cho thấy sau khi mini-player lỗi, khi quay lại màn hình phát YouTube thì vùng video vẫn đen trong khi metadata/Browse vẫn hoạt động. Ở mini-player xuất hiện trạng thái **"Phiên phát đã kết thúc • thử lại"**. Điều này xác nhận lỗi runtime không phải lỗi UI Browse bị chết.

## Nguyên nhân xử lý
Vòng 1.10.62 đã xử lý memory pressure và re-bind target khi player READY, nhưng còn một khe hở: khi ExoPlayer đã rơi vào `STATE_ENDED`, việc chuyển mini → fullscreen chỉ đổi surface/Activity mà không chủ động `prepare()` lại media source. Vì vậy PlaybackActivity có thể trở lại với player đã ENDED và surface đen.

## Sửa 1.10.63
- Thêm đường phục hồi player khi `STATE_ENDED`: `seekTo(0) → prepare() → setPlayWhenReady(true)`.
- Khi người dùng bấm phát từ mini-player mà player đang ENDED, phục hồi source trước khi phát.
- Khi `restoreNm7Player()` được gọi từ mini-player, nếu player đã ENDED thì chuẩn bị lại ngay.
- Sau khi fullscreen restore hoàn tất, nếu player vẫn ENDED thì tự phục hồi và phát lại thay vì giữ màn hình đen.
- Giữ nguyên toàn bộ cơ chế 1.10.62 về giới hạn bộ nhớ, OOM recovery và re-bind mini target.
- Không thêm Android TV; chỉ Mobile.

## Commit
- `efa92e537c55eb3ccd40476e9b12edce6f61efd6` — fix mini restore/recover ended player.
- `24fc48820d9679a4bc11586f94a37ff6face50dd` — bump Mobile to 1.10.63 / versionCode 81.

## Trạng thái
Source đã sửa và tăng version. Cần build Mobile-only CI và cài 1.10.63 để xác nhận runtime. Không dùng APK 1.10.62 để kết luận lỗi này đã hết.

---

# MOBILE 1.10.62 / 80 — BUILD SUCCESS — CHỜ TEST THIẾT BỊ THẬT — 2026-09-21

## Vòng sửa 1.10.62 — giảm áp lực bộ nhớ + phục hồi mini-player
- Nền: Mobile 1.10.60/78 đã được người dùng xác nhận ổn định, sau đó 1.10.61/79 phát hiện mini-player đen/kết thúc bất thường trong video test.
- Không kết luận clip là OOM nếu thiếu logcat; 1.10.62 xử lý cả hai hướng: giảm memory pressure và re-bind mini target sau khi player/decoder READY trở lại.
- Hạ shared ExoPlayer buffer ceiling từ 32 MiB (/8 heap) xuống tối đa 16 MiB (/10 heap).
- Giảm RecyclerView holder retention trong Browse/Playback.
- Khi phát hiện OutOfMemoryError thật: giới hạn recovery, trim tài nguyên hình ảnh nếu có, giảm video kế tiếp tối đa 1280x720 / 4 Mbps và không vượt video constraints.
- Ghi memory diagnostics an toàn (PSS/heap) không ghi URL/token.
- Khi engine/player READY trở lại, re-bind target mini nếu phiên mini còn active; đây là sửa trực tiếp cho hiện tượng mini đen sau recreation.
- Có onTrimMemory ở Application nhưng không thêm dependency Glide vào module Mobile; bản cuối giữ implementation không phụ thuộc Glide.
- Version: **1.10.62 / versionCode 80**.
- Source commit build: `e2b07a42b11ac9b41942a3de864788ecb95e8db9`.

## CI / Build
- **NM7 Mobile Final Build #391 — SUCCESS**.
- Run: `35563683977`; job: `106221213672`.
- Lifecycle regression guards: PASS.
- SmartTube patch/compatibility/resource isolation: PASS.
- `Build Mobile only (current NM7 IPTV Mobile)`: SUCCESS.
- APK artifact: `NM7-IPTV-Mobile-FINAL`, ID `10623695224`, SHA256 `0fdc7f8a63cd767387deea8dd70817466f0e910170ed5b018c7eccf51cace3df`, hết hạn 2026-10-21.
- Artifact chỉ thuộc workflow Mobile; không build Android TV trong run này.
- Bản APK đã build thành công, nhưng **chưa có xác nhận runtime trên điện thoại thật** cho 1.10.62.

## Kiểm thử tiếp theo trên thiết bị thật
1. Phát YouTube fullscreen → thu nhỏ mini → cuộn Browse 15–20 phút.
2. Chuyển video YouTube nhiều lần, đặc biệt khi Browse đang tải nhiều thumbnail.
3. Home/khóa màn hình rồi quay lại, kiểm tra mini không đen và không tự dừng.
4. IPTV ↔ YouTube nhiều lần; IPTV chỉ release khi YouTube thực sự bắt đầu playback.
5. Nếu mini lại đen/lỗi, lấy logcat tại lần lỗi đầu tiên để phân biệt OOM với ExoPlaybackException/MediaCodec/decoder/source.

# MOBILE 1.10.61 / 79 — BUILD SUCCESS, CHỜ TEST THIẾT BỊ — 2026-09-21

## Phản hồi sau khi test dài hơn
- Người dùng báo lại lỗi sau mốc 1.10.60 đã tạm xác nhận ổn định. Đã xem video_2026-09-21_08-50-40.mp4, dài 108,30 giây: mini phát khi cuộn Browse, khoảng giây 97 chuyển đen và hiện **‘Phiên phát đã kết thúc • ▶ thử lại’** trong khi Browse vẫn hoạt động.
- Dòng thông báo này tương ứng owner không còn/đã stopped/destroyed; video không có log để phân biệt chính xác finish/Android hủy Activity. Không quy kết OOM hay khóa màn hình cho clip mới.
- 1.10.60 vẫn là nền phát triển và nhánh stable/mobile-1.10.60 giữ nguyên để đối chiếu, nhưng đã có lỗi tái hiện; không tiếp tục coi mốc đó là bằng chứng mọi lỗi đã hết.

## Sửa 1.10.61
- Phát hiện trong source: ExoPlayerController dừng engine và gửi onPlayEnd khi DISCONTINUITY_REASON_PERIOD_TRANSITION. Với Mobile, bỏ đường legacy này: đổi period không phải hết toàn bộ video; kết thúc thật vẫn đi qua STATE_ENDED.
- ErrorFixer trước đây coi lỗi/buffering trong 180 giây cuối video archived-live là kết thúc. Mobile nay chỉ nhận kết thúc khi engine thực sự STATE_ENDED và không có lỗi; các lỗi nguồn tiếp tục qua recovery có giới hạn.
- Với chế độ đóng sau video: mini thực sự phát hết sẽ dừng/nhả wake lock nhưng giữ phiên để bấm phát lại, không đóng Activity rồi để lại ô đen. Không thay autoplay/queue đã chọn. Mini hiển thị ‘Đã phát hết • ▶ phát lại’; nút phát seek về đầu cho STATE_ENDED.
- Bổ sung log vòng đời finishReally/onDestroy chỉ trạng thái, vị trí, thời lượng; không URL/token. Hai lỗi source là đường có thể đóng sớm, chưa khẳng định duy nhất nguyên nhân clip khi thiếu log trên máy.
- Giữ sửa nền/khóa màn hình, ownership IPTV, UI, bình luận của 1.10.60. Chỉ repo Mobile.

## Kiểm tra
- Toàn bộ patch chạy trên upstream pin sạch: 94 structural guards PASS, Python syntax PASS.
- Thêm ba test regression qua controller thật: 100 period transitions/seek không phát event kết thúc hoặc stop engine; STATE_ENDED thật phát event đúng một lần; lỗi archived-live gần cuối không bị coi là hết video.
- CI compile, unit tests (gồm ba regression mới), assemble và kiểm tra chữ ký/ABI thành công. Chờ người dùng kiểm tra lại mini cùng video ít nhất 10–15 phút, khóa/mở màn hình, hết video/phát lại, đóng mini và chọn IPTV. Chưa stable.

## Build/APK đã xác nhận
- Source commit: `c37977c3905b748005a8c2b1fcbe1ec8f4389b19`.
- [Build #380 SUCCESS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35558054355), run `35558054355`, job `106205450861`; Gradle 8 phút 7 giây, `:app:testMobileDebugUnitTest` thành công.
- [APK ARM64/ARMv7 + SHA256SUMS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35558054355/artifacts/10620709984), hết hạn 2026-10-21.
- ARM64 SHA256: `6afad1ea2a6db4a0f7dbaa44436edf5754601eaaf61d75c35a83e0a9be4f2b32`.
- ARMv7 SHA256: `b1fd2d1970e45bf0f9eb3ed2f0a108808d859afd1b08d855625d49ae746c6206`.
- Chưa xác nhận hết lỗi trong clip trên thiết bị thật; không đánh dấu stable từ CI. Nhánh stable/mobile-1.10.60 không thay đổi.

---

# STABLE BASELINE — MOBILE 1.10.60 / 78 — NGƯỜI DÙNG ĐÃ TEST — 2026-09-21


## Mốc ổn định hiện tại và bàn giao phát triển
- Ngày 2026-09-21, người dùng xác nhận: “bản này đã tét, tạm thời các lỗi đã được khắc phục”, yêu cầu đánh dấu ổn định và dùng làm gốc cho phát triển tiếp theo.
- **Mobile 1.10.60 / versionCode 78 là bản ổn định hiện tại**, theo kết quả sử dụng thực tế do người dùng xác nhận. Không suy diễn rằng mọi thiết bị hoặc mọi ca kiểm thử riêng lẻ đều đã được kiểm tra.
- Mã nguồn APK chuẩn: `3349081178e99f195b4c347f98e781688b913430`; build **#379**, run `35545715388`.
- Nhánh lưu mốc: `stable/mobile-1.10.60`. Giữ nhánh này làm mốc đối chiếu/khôi phục; các thay đổi tiếp theo phát triển trên nhánh công việc dựa trên mốc này, không ghi đè mốc bằng tính năng thử nghiệm.
- Các phiên làm việc tiếp theo phải đọc mục này và lấy 1.10.60 làm nền, không tiếp tục từ các snapshot cũ 1.10.55–1.10.59.
- Giữ các hành vi đã sửa: Back/mini/khôi phục player; phát nền và khóa/mở màn hình; quyền phát giữa YouTube và IPTV; vuốt thẻ không chọn nhầm video; cuộn không nháy; xoay ngang/fullscreen; hẹn giờ tắt.
- Khi sửa phần liên quan, kiểm tra hồi quy các hành vi trên. Các bản mới chỉ được đánh dấu ổn định khi có xác nhận, không tự kế thừa trạng thái stable.
- Đợt cập nhật này chỉ ghi nhận tiến độ và lưu mốc, không thay mã chạy, version hoặc APK. Phạm vi Mobile; không tác động kho phát triển TV.

## Phản hồi và bằng chứng
- Người dùng xác nhận 1.10.59 lỗi nghiêm trọng: mini phát nền sau khi tắt màn hình chỉ được một lúc rồi tự đóng.
- Đã xem video_2026-09-21_06-38-42.mp4 (66,54 giây): mini vẫn có lúc đen khi đổi video/thu nhỏ. Video không có đủ log hệ thống để xác định Android đã kill process hay codec/source lỗi; tình trạng khóa màn hình là phản hồi trực tiếp của người dùng.
- 1.10.59 không stable.

## Phát hiện và sửa 1.10.60
- Callback trạng thái cũ dừng foreground service khi playWhenReady=false, bao gồm tạm dừng trong phục hồi/đổi target; resume mini host cũng dừng service. Nay giữ service theo vòng đời phiên mini hoặc phát nền, không theo các trạng thái tạm IDLE/BUFFERING/pause.
- Bắt đầu giữ service ngay khi chuyển vào mini lúc Activity còn foreground; mở khóa/quay về Browse vẫn giữ phiên. Đóng video, chuyển quyền sang IPTV và sleep timer vẫn dừng đúng luồng.
- Service tách việc giữ notification/session khỏi wake/Wi-Fi lock. User pause/ended giải phóng lock; phát hoặc recovery chủ động giữ lock. Không biến pause thành auto-play.
- Khi vào nền/khóa màn hình, tạm vô hiệu hóa renderer video, giữ audio, nguồn và vị trí. Quay lại foreground phục hồi đúng trạng thái renderer trước đó. Engine mới cũng áp dụng trạng thái audio nền trước mở nguồn.
- Mini còn tồn tại nếu video/session còn sống trong khoảng decoder=null khi engine được tạo lại; không xóa overlay chỉ vì khoảng trống đó. Target mới được lưu để engine gắn lại.
- Giữ các thay đổi giao diện/bình luận/gesture/bộ nhớ và phục hồi lỗi của 1.10.59. Chỉ Mobile, không thay repo TV.

## Xác minh / giới hạn
- Áp dụng toàn bộ patch 1.10.60 lên source pin sạch, gồm MediaServiceCore đúng SHA; 94 structural checks PASS, Python syntax PASS.
- Thêm 6 Robolectric tests: mini lock/unlock; full foreground/background; close/IPTV; wake policy khi pause/recovery; service thật giữ notification và nhả/giữ lock qua pause/resume; predicate mini thật vẫn active khi decoder=null nhưng session còn.
- CI compile, `:app:testMobileDebugUnitTest` (gồm 6 test mới) và assembleMobileDebug SUCCESS. Người dùng đã test và xác nhận tạm thời các lỗi đã được khắc phục ngày 2026-09-21; trạng thái stable dựa trên xác nhận đó kết hợp kết quả CI.
- Danh sách kiểm tra hồi quy cho các bản tiếp theo (không khẳng định người dùng đã thực hiện từng ca): phát mini rồi khóa màn hình 15–30 phút; mở khóa phải giữ video/vị trí; Home 15 phút; pause rồi khóa không tự phát; full→mini→full 10 vòng; mini khi đổi tab; chọn IPTV/đóng mini/hẹn giờ phải nhả phiên nền.
- Kiến trúc player hiện vẫn thuộc PlaybackActivity; bản sửa giữ foreground service và tránh dừng nhầm, chưa chuyển toàn bộ engine sang service owner độc lập. Không cam kết tiếp tục phát sau force-stop hoặc khi hệ thống thực sự hủy process/Activity.

## Build / APK 1.10.60 đã xác nhận
- Source commit: `3349081178e99f195b4c347f98e781688b913430`.
- [NM7 Mobile Final Build #379 — SUCCESS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35545715388); run ID `35545715388`, job ID `106171114435`.
- Gradle BUILD SUCCESSFUL trong 8 phút 6 giây; unit tests, assemble, kiểm tra chữ ký/ABI và upload APK thành công.
- [Tải APK ARM64/ARMv7 và SHA256SUMS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35545715388/artifacts/10616478461).
- Artifact `NM7-IPTV-Mobile-FINAL`, ID `10616478461`, hết hạn 2026-10-20.
- Artifact SHA256: `ac166d196eed0d34af1fe438b738fc2699f4031ae017ba6aa21783fc24f6fdbd`.
- ARM64 APK SHA256: `95b2c01c3b02fbd06d3f1ff04423dec53cbfb647304262836bb53f782cef3a82`.
- ARMv7 APK SHA256: `6d2b3d0cac2fa8ffd71fbf5bdf1a07d358f509b1f24f0d9f8311200190e4fe69`.
- Đã được người dùng xác nhận ổn định hiện tại ngày 2026-09-21; lấy bản này làm nền phát triển tiếp.

---

# BUILD SUCCESS — MOBILE 1.10.59 / 77 — CHỜ TEST THIẾT BỊ — 2026-09-20

- Người dùng xác nhận 1.10.58 vẫn có mini-player đen với thông báo chung; ảnh mới không đủ xác định loại lỗi. Không coi v58 stable.
- Engine dispatch lỗi về PlaybackActivity thực sự sở hữu player, rebind presenter trước khi chuyển qua chuỗi lưu vị trí/phục hồi gốc. Retry xử lý cả player đã mất và nguồn rơi vào IDLE; vẫn giữ giới hạn tự phục hồi, pause intent và guard IPTV.
- Mini hiển thị loại lỗi nguồn/mạng/HTTP/decoder/OOM an toàn, không đưa URL hoặc token lên UI.
- Chi tiết theo ảnh mẫu: title một dòng, dòng thông tin có ‘…xem thêm’, bỏ mô tả lặp và hàng tên kênh thừa; avatar 32dp, pill 32dp, icon viền 18dp, share chỉ biểu tượng. Mô tả đầy đủ vẫn mở trong sheet.
- Bình luận thật được tải sau khi video READY, hiển thị avatar/tác giả/nội dung; cache trang đầu dùng lại khi mở sheet. Hủy request khi đổi video/destroy, loại response cũ. Số bình luận lấy từ contextualInfo nếu YouTube cung cấp; không dùng kích thước trang làm tổng giả.
- Đã áp dụng toàn bộ patch lên source upstream sạch; 89 structural checks PASS. Bổ sung kiểm tra bố cục và phân loại lỗi không lộ URL trong unit tests. Chưa xác nhận runtime trên điện thoại.

## Xác minh 1.10.59
- Source: `dc7f495e09946dc435686c0c999a35455cf1fb46`; retry IDLE: `ef54f9d4afacc83c7a81842680b90de491b74f5c`; commit đã build: `22c0300d23b8025586ae56eb2d640e5f18ea4b11`.
- Build #377: Java/Kotlin compile thành công, 50/51 test qua; test cuộn dùng 200px làm header nhỏ rời viewport, nên đọc bounds cũ. Sửa test cuộn theo nửa chiều cao header (tối đa 80px), vẫn kiểm tra metadata di chuyển, video giữ vị trí/kích thước và nút giữ listener.
- [Build #378 SUCCESS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35542810501): Gradle 7m54s; `:app:testMobileDebugUnitTest` và assembleMobileDebug thành công; kiểm tra chữ ký/ABI, upload hai APK thành công.
- [APK ARM64/ARMv7 + SHA256SUMS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35542810501/artifacts/10615413353), hết hạn 2026-10-20.
- ARM64 SHA256: `e23545c6f09a34a9c1683284c18c646be78ea1483e3592c1f4aea2e047bcdc9a`.
- ARMv7 SHA256: `c7b15fd4d47fb3981400aaeed3e92de0946d47acbd4bb21723728351181ac4bc`.
- Chưa test điện thoại, chưa khẳng định mini đen hoặc dừng nền đã hết. Cần kiểm tra full→mini→full, phát mini/nền kéo dài, pause/retry sau lỗi và chuyển IPTV; đối chiếu UI/nội dung bình luận với ảnh mẫu. Nếu vẫn lỗi, ghi lại loại lỗi mới trong mini để phân biệt nguồn/HTTP/decoder/bộ nhớ.

---

# BUILD SUCCESS — MOBILE 1.10.58 / 76 — CHỜ TEST THIẾT BỊ THẬT — 2026-09-20

## Bằng chứng và yêu cầu
- Mốc nền: commit `576375331c2dc42259a65af010fdcf6cb21501c5`, 1.10.57 / 75. Người dùng cung cấp ảnh build #373 thành công nhưng xác nhận bản này vẫn lỗi; không stable.
- Đã kiểm tra video_2026-09-20_22-04-59.mp4 (40,69 giây): mini đen/dừng, khoảng giây 11 có toast `Unexpected OutOfMemoryError`. Chưa có stack trace để kết luận thành phần cấp phát gây OOM.
- Đã kiểm tra video mẫu video_2026-09-20_22-05-06.mp4 (16,69 giây) và photo_2026-09-20_22-05-03.jpg: player cố định, thông tin/nút/bình luận cuộn khuất theo danh sách gợi ý.
- Người dùng báo thêm vuốt tab lag, mở video chậm và YouTube phát nền một lúc rồi dừng.

## Thay đổi 1.10.58
- Vuốt đổi tab ngay khi vượt ngưỡng chuyển tab, không chờ nhấc ngón tay. Mỗi gesture chỉ đổi một tab; giữ CANCEL để không mở nhầm card. Hit-test mini/header chỉ chạy lúc DOWN. Presenter chạy sau frame cập nhật tab và gộp yêu cầu vuốt nhanh; bỏ dữ liệu thuộc tab cũ.
- Các nhóm trong feed dùng chung view type để tái sử dụng holder, tránh mỗi nhóm tạo một pool riêng. Tắt item animation trong Browse; giữ cache holder nhỏ.
- Glide có ngân sách heap riêng: cache ảnh tối đa 16 MiB, bitmap pool tối đa 8 MiB, array pool 2 MiB. Thumbnail ưu tiên hq720, có fallback maxres/nguồn gốc; decode giới hạn 960px, không SIZE_ORIGINAL. Giảm lưu lượng tải ảnh và cấp phát cạnh tranh với player; chưa có đo thời gian mở video trên máy thật.
- Dọn bitmap poster khi nguồn phát sẵn sàng và trim image cache khi engine báo OOM trước phục hồi. OOM dựng lại engine để giải phóng allocator/decoder; giới hạn một lần tự phục hồi áp dụng cả mini và full khi OOM. Giữ giới hạn bộ đệm video và retry của 1.10.57; không coi đây là bằng chứng OOM đã hết.
- Mini host đi ra nền gọi đúng bridge giữ phiên phát và foreground service. Trước đây Home từ Browse không gọi onUserLeaveHint của PlaybackActivity đã stopped. Service giữ Wi-Fi lock cùng wake lock, giải phóng khi dừng/chuyển ownership. Giữ pause và IPTV guard.
- Metadata/nút/bình luận trở thành một header của RecyclerView gợi ý, không bọc toàn bộ feed trong ScrollView. Cuộn lên sẽ cuộn khuất thông tin, video vẫn ở trên; cuộn xuống đầu danh sách hiện lại.
- Hàng hành động gồm avatar, đăng ký dạng pill đen, like/dislike trong cùng nền bo tròn, chia sẻ/lưu và các chức năng chất lượng/tốc độ/menu. Giữ handler native. Số like/dislike chỉ hiện khi metadata có, không tự tạo số.
- Card bình luận nền xám bo tròn, mở sheet bình luận thật khi chạm; sau khi tải hiện trích đoạn/avatar bình luận. Không giả làm ô gửi bình luận khi backend hiện chỉ hỗ trợ đọc. Sửa chữ/icon bình luận cho nền sáng.
- Chỉ thay repo Mobile, không thay repo TV.

## Kiểm tra và bàn giao
- Patch áp dụng thành công trên upstream pin `4825d6aa8b6f1d3181927f9e96c7d89cab13d510`.
- 89 structural guards PASS; Python syntax và generated XML parse PASS.
- Cập nhật test gesture cho chuyển tab trước UP và chỉ chuyển một lần. Bổ sung 2 Robolectric tests: metadata cuộn trong RecyclerView trong khi player giữ nguyên bounds, listener nút vẫn chạy; đủ controls và card bình luận trong header.
- CI :app:testMobileDebugUnitTest và assembleMobileDebug SUCCESS; Gradle BUILD SUCCESSFUL trong 5 phút 43 giây. Chưa kiểm thử trên thiết bị thật; chưa stable.
- Test tiếp: vuốt tab nhanh 20 lần; đo chạm video đến frame đầu cùng video/mạng; mini + cuộn feed 15 phút; Home/khóa máy 15 phút từ mini và full player; pause rồi Home không tự phát; đổi IPTV; cuộn mô tả và trở lại; like/subscription với tài khoản; bình luận/replies; Back và xoay ngang.


## Build / APK đã xác nhận
- Source commit: `63fc91b594c3629564be13a5dce90a1cb320960b`.
- [NM7 Mobile Final Build #375 — SUCCESS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35519495736), run ID `35519495736`. Đây là bản bàn giao sau bổ sung phục hồi OOM, thay build #374.
- [Tải APK ARM64/ARMv7 và SHA256SUMS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35519495736/artifacts/10607738558).
- Artifact `NM7-IPTV-Mobile-FINAL`, ID `10607738558`, hết hạn 2026-10-20.
- Artifact SHA256: `7d05310686a852b9021a8e599e8656db356fcb446ffe06648a82581bb8626aa3`.
- ARM64 APK SHA256: `be2df03ac1a4395b081bee26725924bc6866aa4d34ebb87f8b23f4131b163503`.
- ARMv7 APK SHA256: `89b0e1880410bde6148dd6247c8d6733a264bd79fee02e9a9b05b2bb0c71f4ca`.
- Không khẳng định hết lag, tải chậm hoặc OOM chỉ từ kết quả build/unit test; chờ người dùng kiểm tra trên máy thật.

---

# SOURCE READY — MOBILE 1.10.57 / 75 — CHỜ CI VÀ TEST THẬT — 2026-09-20

## Phản hồi mới và bằng chứng
- Người dùng test 1.10.56: cuộn lên làm giao diện nháy liên tục; mini chuyển sang thông báo lỗi.
- Đã xem video_2026-09-20_19-52-25.mp4 (110 giây). Mini có lúc phát rồi chuyển đen/báo lỗi; mở video khác có thể phát lại.
- Thông báo trong 1.10.56 chỉ là “Lỗi phát • bấm ▶ để thử lại”, không có nguyên nhân engine/HTTP. Không kết luận video mới là OOM chỉ từ thông báo này.
- 1.10.56 không stable.

## Sửa 1.10.57
- Nguyên nhân nháy từ source: onScrolled ẩn/hiện AppBar bằng GONE/VISIBLE và đổi padding root, làm thay đổi chiều cao RecyclerView ngay trong cuộn; callback/layout sau đó có thể đảo trạng thái.
- Chuyển Browse sang FrameLayout: feed có viewport cố định, app bar và thanh dưới phủ lên; padding đầu/cuối của danh sách đặt một lần. Ẩn header bằng INVISIBLE, không đổi kích thước/padding khi cuộn.
- Chỉ chuyển trạng thái thanh theo chuyển động ngón tay thật; không còn onScrolled phản ứng với relayout/fling. Cuộn lên ẩn, kéo xuống hiện; kéo mini và thao tác trên header/footer không đổi mục phía sau.
- Sửa xung đột phục hồi: lựa chọn DNS transport sớm chỉ chạy một lần cho phiên Activity; restart engine tôn trọng transport mới do ErrorFixer chọn, không ép trở lại OkHttp sau mỗi lỗi.
- Nút thử lại mini đi qua ErrorFixer để tải lại nguồn/client hoặc dựng lại decoder theo loại lỗi; bỏ retry trực tiếp cùng MediaSource lỗi.
- Tự phục hồi lỗi engine trong mini tối đa một lần mỗi video; người dùng được chủ động thử lại. Giữ ý định pause trong lúc phục hồi. Khi IPTV sở hữu playback thì không tự restart hay phát YouTube trở lại.
- Giữ tham chiếu video đang phát trong player owner để recovery không phụ thuộc card còn trong Browse hay WeakReference đã bị thu gom.
- Gắn lại controller view sau engine release/recreate. Thông báo mini phân biệt lỗi nguồn, mạng, giải mã, thiếu bộ nhớ; có mã HTTP nếu engine cung cấp. Log chỉ type/renderer/class, không URL/cookie/token.
- Giữ sửa vuốt chọn nhầm, kính lúp, thumbnail có giới hạn bộ nhớ, xoay ngang/fullscreen, Back/mini, IPTV và hẹn giờ.

## Kiểm tra và bàn giao
- 89 structural checks PASS trên upstream pin 4825d6aa8b6f1d3181927f9e96c7d89cab13d510; XML parse và Python syntax PASS.
- Thêm 7 Robolectric tests: ngón tay đứng yên không đảo thanh; kéo xuống hiện; loại trừ mini/vuốt ngang; inflate và đo layout thật khi header ẩn/hiện 10 lần không đổi viewport; giới hạn retry; pause/IPTV; retry thủ công/video mới.
- Chờ CI unit tests/assemble và APK 1.10.57. Chưa test thiết bị thật, chưa stable.
- Test tiếp: cuộn lên/xuống rồi thả tay 20 lần, không nháy; giữ nguyên ngón tay khi thanh ẩn; vuốt thẻ không mở nhầm; mini phát 10 phút trong lúc cuộn/đổi thẻ; pause khi phục hồi; đổi IPTV; Back/restore/xoay ngang. Nếu mini còn lỗi, ghi lại dòng thông báo mới để phân biệt nguyên nhân.
- Chỉ repo Mobile, không thay repo TV.

---

# BUILD SUCCESS — MOBILE 1.10.56 / 74 — CHỜ TEST THIẾT BỊ THẬT — 2026-09-20

## Phản hồi và bằng chứng
- Người dùng xác nhận 1.10.55 còn nhiều lỗi; không stable.
- Đã xem hai video video_2026-09-20_19-21-55.mp4 (139 giây, NM7 lỗi) và video_2026-09-20_19-21-48.mp4 (32 giây, mẫu thao tác).
- Video NM7 có thông báo Unexpected OutOfMemoryError, player/mini đen; một số lần sau đó vẫn phát. Chưa có logcat để quy kết duy nhất cho bitmap hay video buffer.
- Yêu cầu: vuốt đổi thẻ không mở nhầm video; tìm kiếm chỉ còn kính lúp; cuộn feed ẩn thanh trên/dưới; player mở như panel trong video; mini phát được.

## Thay đổi 1.10.56
- Gesture Browse dùng tọa độ màn hình, loại trừ mini. Khi vuốt ngang vượt touch slop: gửi ACTION_CANCEL xuống card và giữ toàn bộ chuỗi sự kiện, không truyền ACTION_UP xuống card. Bắt cả vuốt nhanh chỉ có DOWN/UP; vuốt ngắn không mở card, vuốt dọc/tap vẫn hoạt động.
- Thay thanh tìm kiếm bằng nút kính lúp 48dp. Giữ tìm kiếm, giọng nói, tài khoản và menu nhấn giữ.
- Cả feed hàng và grid: cuộn lên ẩn app bar/thanh điều hướng và bỏ khoảng trống dưới; cuộn xuống hiện lại; quay về Browse hiện lại điều hướng.
- Watch view mở từ dưới lên, Back/vuốt xuống thu về mini; giữ player 16:9, metadata, gợi ý và fullscreen xoay ngang. Vẫn dùng Activity native hiện có để giữ chức năng; đây là chuyển động panel, chưa phải tái kiến trúc player nhúng chung Activity hay sao chép đầy đủ animation mẫu.
- Sửa màu chữ/icon của các nút hành động bị trắng trên panel trắng.
- Thumbnail giữ nguồn chất lượng cao/fallback, nhưng decode theo chiều rộng màn hình tối đa 1280px và tỷ lệ 16:9; bỏ SIZE_ORIGINAL/DownsampleStrategy.NONE để tránh bitmap không giới hạn.
- Bộ đệm video tính theo heap app: tối đa 32 MiB hoặc 1/8 heap (sàn 4 MiB), ưu tiên giới hạn byte, bỏ back-buffer giữ lâu. Không còn tính theo tổng RAM thiết bị.
- Mini xác nhận gắn đúng player sau target switch; giữ sửa rebind engine restart của 1.10.55. Có vòng tải và thông báo lỗi; nút phát thử lại khi engine báo lỗi. Không tự retry vô hạn, không ép seek hay reset video khi đổi target.
- Vuốt thu nhỏ player giữ chuỗi touch tới UP/CANCEL để không kích hoạt nhầm phần dưới.

## Kiểm tra và bước tiếp
- Patch áp dụng đúng upstream pin 4825d6aa8b6f1d3181927f9e96c7d89cab13d510.
- 82 structural checks PASS. Thêm 6 Robolectric tests cho chuỗi vuốt, tap, cuộn dọc, cancel, vuốt nhanh và mini exclusion; CI :app:testMobileDebugUnitTest đã thành công. XML tạo ra parse thành công.
- APK 1.10.56 đã xác nhận qua build #372; chưa có xác nhận test thiết bị thật.
- Cần thiết bị thật: vuốt qua lại nhanh trên thumbnail 20 lần không mở nhầm; cuộn ẩn/hiện thanh; tap video mở panel; Back/mini/restore 10 vòng; mini phát 10 phút; đổi IPTV; xoay ngang; thumbnail nét và không OutOfMemoryError.
- Không đánh dấu sửa triệt để mini/decoder khi chưa test máy thật. Chỉ repo Mobile.

## Build / bàn giao đã xác nhận
- **VersionName 1.10.56 / VersionCode 74**.
- Source commit: `ae0fa0c9dab62f2c325da77fabc3adc76a8fe92b`.
- [NM7 Mobile Final Build #372 — SUCCESS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35510885621); run ID `35510885621`.
- CI unit tests và assembleMobileDebug SUCCESS; Gradle 7 phút 39 giây.
- [APK ARM64/ARMv7 và SHA256SUMS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35510885621/artifacts/10605087490).
- Artifact `NM7-IPTV-Mobile-FINAL`, ID `10605087490`; hết hạn 2026-10-20.
- Artifact SHA256: `3403841bbdc8c0eda3f7033b2d7d34324dab9be80151e911ecbcc4cce259ed88`.
- ARM64 SHA256: `ce853151b531f32a5d63af0f4c4f836e835807d00c6d87ada8a16282581c75c0`.
- ARMv7 SHA256: `51992a251beaae96dc608287b779bd8b1f10d4231093dd230169e529dec8df11`.
- Source evidence artifact: `10605311501`.
- **Chưa stable:** thành công CI không chứng minh hết OOM/mini đen trên thiết bị. Chờ người dùng test theo danh sách trên.

---

# BUILD SUCCESS — MOBILE 1.10.55 / 73 — CHỜ TEST THIẾT BỊ THẬT — 2026-09-20

## Trạng thái mới nhất
- **Bản hiện tại: 1.10.55 / versionCode 73**, người dùng đã xác nhận lại số phiên bản.
- **NM7 Mobile Final Build #370: SUCCESS**. Có APK; không còn ở trạng thái chờ CI.
- Back và chuyển mini của 1.10.53 đã được người dùng xác nhận khắc phục. Các lỗi phát sinh sau đó và thay đổi mới của 1.10.55 vẫn cần kiểm tra lại trên điện thoại.
- **Chưa đánh dấu 1.10.55 stable.** Build/unit tests thành công không xác nhận hết lỗi hình đứng, xoay ngang hoặc tốc độ tải thực tế.

## Đầu vào
- Người dùng có APK 1.10.54 và báo IPTV mở kênh bị đen, chỉ phát lại sau khi mở YouTube; YouTube tải chậm; mini đang xem dừng/đen.
- Đã xem video 16-54-40 (mini đen), 16-54-57 (IPTV đen, YouTube tải lâu và hiện thông báo sửa DNS), 16-54-46 (mẫu giao diện/chuyển player YouTube).
- ZIP người dùng cung cấp đúng source 1.10.54. Không đánh dấu 1.10.54 stable.

## Phát hiện từ source và bản sửa
- Cold start chỉ xóa cờ background nhưng giữ tab YouTube cũ. Inline provider bỏ qua attach khi tab=YouTube; MainActivity rơi vào PlayerActivity, nơi onStart không startPlayer khi tab cũ là YouTube. Nay reset tab về IPTV khi tạo process và đặt lại ownership/handoff trước thao tác mở kênh trực tiếp.
- SmartTube restartEngine luôn gắn bộ phát mới vào mPlayerView của Activity đã ẩn. Nay gắn lại mini đang attached nếu người dùng đang xem mini; detach target cũ trước release. Đây là sửa lỗi source, chưa chứng minh bao phủ mọi trường hợp mini đứng trên máy thật.
- Suspend IPTV có cờ riêng, idempotent; delayed attach từ Main không được tự giành lại quyền phát. Chỉ quay lại YouTube mới phục hồi intent trước đó.
- Với custom DNS và cơ chế sửa mạng đang bật, chọn OkHttp trước initializePlayer thay vì chờ ErrorFixer phát hiện long buffering rồi restart. Không đổi DNS, không tắt TLS, không log URL/cookie/DRM. Chưa có số đo tốc độ trên điện thoại.
- Giao diện YouTube nền sáng, feed/search/gợi ý một cột; player 16:9 dọc và fullscreen ngang được giữ. Vuốt xuống từ giữa vùng video dọc gọi luồng mini đã có; mini có phát/tạm dừng, đóng, kéo vị trí và chạm khôi phục. Chưa khẳng định giống hoàn toàn animation của app mẫu.
- Giữ Back, tap-shield, xoay cảm biến, first-frame restore gate 750ms, IPTV fullscreen ẩn tab, sleep timer và các tính năng hiện có. Không sửa repo TV.

## Kiểm tra và bàn giao
- Patch áp dụng trên upstream pin 4825d6aa8b6f1d3181927f9e96c7d89cab13d510.
- 75 kiểm tra cấu trúc PASS; Python syntax và git diff --check PASS. Kéo mini không kích hoạt vuốt đổi mục Browse phía sau.
- Thêm 2 Robolectric tests cold start/preserve saved IPTV. CI đã chạy thành công :app:testMobileDebugUnitTest và assembleMobileDebug; Gradle BUILD SUCCESSFUL trong 8 phút 21 giây.
- Cần test: force-stop sau tab YouTube rồi mở IPTV ngay; đổi 5 kênh; mini phát 10 phút và sau lỗi mạng; pause/restore; kéo mini; vuốt player xuống; Back Search/Browse; xoay ngang/dọc; đo thời gian chạm → frame đầu trên cùng video/mạng.

## Build và APK 1.10.55 đã xác nhận
- Source commit: `fa883bbf75ffabaa13f13ff45baadf098e62765f` (bao gồm tách thao tác kéo mini khỏi vuốt Browse).
- [NM7 Mobile Final Build #370 — SUCCESS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35504581687), run ID `35504581687`. Đây là build mới nhất, thay cho #369 cùng phiên bản.
- [Tải NM7-IPTV-Mobile-FINAL: APK ARM64/ARMv7 và SHA256SUMS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35504581687/artifacts/10603078762).
- Artifact ID: `10603078762`; hết hạn 2026-10-20.
- Artifact SHA256: `5a5003706f67c7efdd462c6d3d307c9d276cac9d20197e5b99374eb8f29ea9f5`.
- ARM64 APK SHA256: `25734b1ed80c2ed92d938c24f7ee719e09087c080c1182d349986212230b069e`.
- ARMv7 APK SHA256: `0d5e788520af61a91e95b1e94169f11357e36af1702b72df1c52fafd359e83b9`.
- Source evidence artifact: `10603362594`, hết hạn 2026-12-19.
- Ưu tiên test tiếp: IPTV ngay sau khởi động; mini không đen/đứng sau restart engine; mini → player hình/tiếng; xoay ngang YouTube tự fullscreen; tốc độ tải cùng video/mạng; vuốt đổi mục và độ nét thumbnail. Chưa có xác nhận PASS mới cho các mục này.

---

# MỐC CŨ — MOBILE 1.10.54 — XOAY YOUTUBE / ĐỒNG BỘ MINI → PLAYER — 2026-09-20

## Kết quả người dùng test 1.10.53
- **Back: PASS**, đã khắc phục.
- **Chuyển mini-player: PASS**, đã khắc phục.
- Còn: YouTube không tự xoay ngang/fullscreen khi quay điện thoại; IPTV đã fullscreen được.
- Còn: mini → player hình chậm/đứng một chút so với tiếng.
- Giữ các phần đã PASS, không đánh đồng thành toàn bộ 1.10.53 stable.

## Thay đổi 1.10.54 / versionCode 72
- Bỏ SCREEN_ORIENTATION_PORTRAIT riêng cho PlaybackActivity; dùng SCREEN_ORIENTATION_SENSOR. Browse/Search vẫn dọc.
- Giữ onConfigurationChanged/applyOrientation hiện có: ngang = video toàn màn hình, ẩn panel/thanh hệ thống; dọc = player 16:9 và panel. Manifest giữ configChanges để không tạo lại engine khi xoay.
- Khi restore mini → player: ghi nhớ trạng thái đang phát/tạm dừng và tạm dừng media clock trong quá trình chuyển cửa sổ/target.
- Gắn VideoListener trước switchTargetView; tiếp tục khi onRenderedFirstFrame của target fullscreen đến. Giữ ý định pause của người dùng.
- Timeout 750 ms sau khi gắn target để không kẹt pause với nội dung chỉ có tiếng hoặc decoder không gửi callback. Khi timeout xảy ra không thể khẳng định hình/tiếng đã đồng bộ.
- Dọn listener/timeout khi Back, pause Activity, nhường IPTV và đóng player. Không seek, không tải lại video, giữ fix Back/mini của 1.10.53.

## Kiểm tra
- 64 kiểm tra cấu trúc PASS trên upstream pin.
- Build #368 (run 35498630434) đã SUCCESS. Sau đó người dùng báo còn lỗi; 1.10.55 là bản hiện tại. Chưa xác nhận PASS cảm biến và độ trễ video trên thiết bị.
- Cần test quay dọc/ngang khi đang xem; restore mini ở hai hướng; restore video đang pause; Back sau restore; chuyển IPTV.

---

# BUILD SUCCESS — MOBILE 1.10.53 — BACK / MINI TARGET — 2026-09-20

## Kết quả mới từ người dùng
1.10.52 vẫn lỗi: Back trong YouTube không thoát trình phát; mini-player còn đứng hình. Đã xem ảnh bàn giao 1.10.52 và video `video_2026-09-20_14-02-10.mp4`. **1.10.52 không stable.**

## Phát hiện từ mã hiện tại
- onResume chỉ restore target khi sNm7RestorePending=true (tap mini). Nếu mở trình phát qua đường khác trong lúc sNm7Mini=true, cờ mini không xóa, full PlayerView không được gắn lại.
- onBackPressed có nhánh return khi sNm7Mini=true và restorePending=false: đúng trạng thái trên sẽ bỏ qua Back.
- Overlay tự giữ previousSurface, nhưng không phải lúc nào previousSurface cũng là đích video thực tế.
- Mini và fullscreen dùng SurfaceView. 1.10.53 chuyển cả hai sang PlayerView dùng TextureView để tránh cửa sổ SurfaceView độc lập khi Activity đổi trạng thái. Đây là thay đổi cần kiểm chứng bằng video thực tế, không kết luận decoder đã sửa chỉ từ source.

## Sửa 1.10.53 / versionCode 71
- Mọi onResume của PlaybackActivity đều chuẩn hóa về fullscreen: chuyển target rồi xóa mini/restore flags.
- Back chỉ chống bấm lặp trong cùng lần chuyển Activity; cờ chặn được reset khi foreground, không dùng cờ mini cũ để bỏ qua Back.
- Player owner giữ mNm7VideoTarget và dùng PlayerView.switchTargetView cho hai hướng chuyển; không clearVideoSurface/seekTo ép frame.
- Mini và fullscreen đều texture-backed PlayerView; tapShield/nút đóng mini giữ nguyên.
- Cài callback Android 13+ cho Browse/Search; Playback giữ callback riêng, không đăng ký trùng.
- Giữ IPTV full-screen ẩn tab (PASS), tap mini không xuyên card (PASS), thumbnail, swipe, prefetch, hẹn giờ và IPTV ownership hiện tại.
- Chỉ repo Mobile, không thay repo Android TV.

## Kiểm tra / bàn giao
- 59 kiểm tra cấu trúc PASS trên nguồn pin.
- Thêm Robolectric test inflate XML thực tế sau merge tài nguyên: mini và fullscreen phải tạo TextureView, controller tắt.
- CI :app:testMobileDebugUnitTest và assembleMobileDebug thành công; hai Robolectric tests inflate XML có trong test suite. Chưa test giải mã video và Back trên thiết bị thật.
- Build **NM7 Mobile Final Build #367**, run **35496093973**, SUCCESS; Gradle 8 phút 6 giây.
- Source commit: `6c6502d866b1fd6bc8cc31a681133f2758651ab6`.
- [APK ARM64/ARMv7 và SHA256SUMS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35496093973/artifacts/10601495287), hết hạn 2026-10-20.
- Artifact SHA256: `4b354dd82b5ce6bdd05e04b83868d887aa123c9786bf6e8ba14f4f91c1872b85`.
- ARM64 SHA256: `c0bbd178c77a15c6650443d71eda19ce9c8d8808db6fec00fa0a3565f9e3003d`.
- ARMv7 SHA256: `d96cec579f4ba447a1c2af72c9e07d633f4580fa294cb17faef75c6150845ea5`.
- **Cập nhật sau bàn giao: người dùng đã xác nhận Back và chuyển mini được khắc phục; còn xoay ngang YouTube và độ trễ hình khi mini → player. Không đánh dấu toàn bộ bản stable.**
- Cần test: mở video khi mini cũ tồn tại → Back; lặp mini/fullscreen 5 lần; Back tại Search/Browse; đổi IPTV khi mini; hình chuyển động đồng thời với tiếng.

---

# BUILD SUCCESS — MOBILE 1.10.52 — CHỜ TEST THIẾT BỊ THẬT — 2026-09-20

Người dùng test 1.10.51 và gửi:
- `video_2026-09-20_13-01-09.mp4`
- `photo_2026-09-20_13-00-20.jpg`

## Lỗi đầu vào đã xác định
- Có thể bị kẹt/không thoát được khi đang ở màn hình kết quả tìm kiếm YouTube.
- Thumbnail kết quả tìm kiếm rất mờ/nhòe.
- Màn gặp lỗi là `SearchActivity`, không phải `BrowseActivity`.

## Thay đổi 1.10.52
- `SearchActivity.onBackPressed()`:
  - nếu mini đang active → `consumeNm7BrowseBack()`, đóng mini/session trước, không kéo `PlaybackActivity` quay lại;
  - nếu không còn mini → dùng Back bình thường;
  - nút mũi tên Back trong Search dùng cùng luồng này.
- Nhánh thumbnail chất lượng cao áp dụng cho mọi card YouTube có `videoId` hợp lệ, gồm cả kết quả tìm kiếm:
  - `maxresdefault.jpg`
  - `hq720.jpg`
  - fallback sang `bgImageUrl/getCardImageUrl`
  - `SIZE_ORIGINAL` + `ARGB_8888` + `DownsampleStrategy.NONE`.
- Giữ nguyên các phần đã xác nhận trước đó:
  - IPTV full-screen ẩn tab YouTube/IPTV: PASS.
  - tap mini không xuyên xuống card phía sau: PASS.
  - các thay đổi lifecycle/player từ 1.10.51 được giữ nguyên.

## Build xác nhận
- **VersionName:** `1.10.52`
- **VersionCode:** `70`
- **Source commit:** `3b29e3e68ed536661fcfa6ab97e6421279d4725b`
- **Guard-fix/build commit:** `067b2f8c185c0a0803fff7e92712e3802b188461`
- **GitHub Actions:** `NM7 Mobile Final Build #366`
- **Run ID:** `35493388920`
- **Kết quả:** **SUCCESS**
- **Artifact:** `NM7-IPTV-Mobile-FINAL`
- **Artifact ID:** `10599477922`
- **Artifact SHA256:** `cec71c2986079dc13e75c5a93a775cccb6b309672737d2a89b27a1517655a690`
- **Source proof artifact:** `10599637310`

## Trạng thái hiện tại
**1.10.52 chưa được đánh dấu stable.** CI/build đã thành công nhưng còn chờ người dùng test thực tế trên điện thoại các điểm:
1. Back trong Search khi mini đang active.
2. Có còn bị kẹt/vòng lặp player ↔ mini hay không.
3. Thumbnail Search/Browse có đủ sắc nét hay không.
4. Mini ↔ player có còn tình trạng audio chạy nhưng hình đứng hay không.

Mọi phát triển tiếp theo phải giữ nguyên các phần đã PASS và dựa trên source hiện tại của 1.10.52 cho đến khi có kết quả test mới.

---

# ĐANG BUILD — MOBILE 1.10.51 — 2026-09-20

Người dùng test 1.10.50 và gửi video `video_2026-09-20_12-22-18.mp4`.

## Kết quả test
- IPTV fullscreen ẩn tab dưới: giữ nguyên PASS.
- Tap mini không xuyên xuống card: giữ nguyên PASS.
- Mini → YouTube player: vẫn có trường hợp audio chạy nhưng hình đứng.
- Back trong tab YouTube: đang lặp player → mini → player → mini; chỉ thoát được khi đóng mini thủ công.
- Thumbnail YouTube được người dùng đánh giá kém sắc nét hơn trước.

## Nguyên nhân/sửa 1.10.51
### Restore mini → player
1.10.50 gọi PlayerView.switchTargetView() khi PlaybackActivity vẫn đang stopped/background. Target mPlayerView chưa active hoàn toàn nên surface handoff có thể giữ audio nhưng không render frame.
- restoreNm7Player() nay chỉ đặt `sNm7RestorePending=true` và REORDER_TO_FRONT PlaybackActivity.
- `onResume()` của PlaybackActivity mới gọi `completeNm7RestoreOnResume()` để switchTargetView từ mini → mPlayerView khi target đã foreground.
- Sau khi switch thành công mới remove mini và clear mini state.

### Back state machine YouTube
- Player foreground + Back lần 1: vào mini đúng một lần.
- Browse/Home YouTube + mini đang hiện + Back lần 2: `consumeNm7BrowseBack()` đóng mini/session và giữ Browse ở màn hình chính, không pop PlaybackActivity trở lại.
- Không còn vòng lặp player ↔ mini do back stack.
- Back tiếp theo khi không còn mini dùng hành vi Browse bình thường.

### Thumbnail
- Giữ maxresdefault làm nguồn đầu tiên.
- Thêm hq720 trước các URL fallback thấp hơn.
- Decode ARGB_8888 + DownsampleStrategy.NONE + SIZE_ORIGINAL cho card 16:9 lớn.
- Không thay layout/feed/swipe.

## Mốc
- VersionName: **1.10.51**
- VersionCode: **69**
- Chờ CI và test máy thật.

---

# ĐANG BUILD — MOBILE 1.10.50 — 2026-09-20

Người dùng test 1.10.49:
- IPTV fullscreen ẩn tab YouTube/IPTV: **PASS**.
- Mini-player vẫn đen/đứng hình: **FAIL**.

## Nguyên nhân mới xác định
Các bản 1.10.47–1.10.49 đều chuyển output video bằng thao tác thủ công (detach old view / attach new view hoặc setVideoTextureView). Với ExoPlayer, surface cũ có callback lifecycle bất đồng bộ; callback destroy/detach đến muộn có thể clear output vừa attach sang mini, tạo audio-only/black/frozen video.

## Sửa 1.10.50
- Dùng ExoPlayer PlayerView cho mini.
- Chuyển player bằng API chính thức PlayerView.switchTargetView(player, oldView, newView).
- Khi chuyển full → mini: switchTargetView từ SmartTube mPlayerView sang mini PlayerView.
- Khi mini được reattach giữa Browse/Main: switchTargetView từ mini cũ sang mini mới.
- Khi bấm mini để restore: switchTargetView từ mini về SmartTube mPlayerView.
- Không clearVideoSurface, không setVideoTextureView thủ công, không seekTo.
- Giữ engine/lifecycle fix của 1.10.48.
- Giữ tapShield đã PASS.
- Giữ IPTV fullscreen ẩn tab đã PASS ở 1.10.49.
- Không thay đổi các chức năng khác.

## Mốc
- VersionName: **1.10.50**
- VersionCode: **68**
- Chờ CI + test điện thoại thật.

---

# ĐANG BUILD — MOBILE 1.10.49 — 2026-09-20

Người dùng test 1.10.48:
- Lỗi tap xuyên xuống card video phía sau mini-player: **đã khắc phục**.
- Lỗi mini-player đen/đứng hình: **vẫn còn**.
- Yêu cầu mới: IPTV inline khi mở rộng/toàn màn hình phải ẩn thanh tab YouTube/IPTV phía dưới; thoát toàn màn hình mới hiện lại.

## Sửa 1.10.49
### Mini-player
- Không dùng ExoPlayer PlayerView làm surface mini nữa.
- Dùng TextureView trực tiếp và chỉ attach sau onSurfaceTextureAvailable/isAvailable.
- Giữ engine mở từ sửa 1.10.48.
- Không gọi clearVideoSurface().
- Không seekTo(currentPosition).
- Khi restore fullscreen YouTube: detach TextureView rồi gắn player lại vào SmartTube PlayerView.
- Giữ nguyên tapShield đã được người dùng xác nhận sửa lỗi tap xuyên.

### IPTV fullscreen
- HomeTabBar có BAR_TAG và API setVisible().
- enterFullscreen() của MobileInlinePlayerProviderV2 ẩn HomeTabBar trước khi immersive fullscreen.
- exitFullscreen() hiện HomeTabBar lại.
- Không thay playback IPTV, channel switching hoặc các chức năng khác.

## Mốc
- VersionName: **1.10.49**
- VersionCode: **67**
- Chờ CI + test điện thoại thật.

---

# ⚠️ 1.10.47 STABLE STATUS REVOKED — ĐANG BUILD 1.10.48 — 2026-09-20

Sau khi test thêm và gửi video màn hình, người dùng phát hiện lỗi nghiêm trọng còn tồn tại trong 1.10.47:
- YouTube đang phát → chuyển sang mini-player: mini đen/đứng hình.
- Chạm vào mini-player không restore video hiện tại; thao tác có thể lọt xuống card video nằm phía sau và mở card đó.

Vì vậy nhãn **"bản ổn định hiện tại" của 1.10.47 được thu hồi**. Tuy nhiên 1.10.47 vẫn là baseline source gần nhất để phát triển 1.10.48; các chức năng khác đã ổn phải giữ nguyên.

## Nguyên nhân xác định
1. onBackPressed() của PlaybackActivity đặt sNm7Mini=true nhưng đồng thời gọi blockEngine(true).
2. Sau khi BrowseActivity được đưa lên, PlaybackActivity.onPause() lại tiếp tục blockEngine(true) và gọi presenter onViewPaused().
3. PlayerView mini vì vậy nhận Player nhưng decoder/video renderer đang bị block, gây black/frozen video.
4. PlayerView không có lớp touch độc lập phía trên, khiến tap có thể đi vào RecyclerView/card phía sau thay vì restore mini.

## Sửa 1.10.48
- Khi Back vào mini: sNm7Mini=true nhưng blockEngine(false).
- onPause(): nếu đang ở mini mode thì giữ engine mở, không chuyển sang background state và không gọi presenter onViewPaused như một phiên playback bị pause hoàn toàn.
- attach mini PlayerView luôn bảo đảm blockEngine(false).
- Thêm View tapShield trong suốt phủ toàn bộ PlayerView, clickable/focusable và consume tap để gọi restoreNm7Player().
- PlayerView bên dưới được đặt non-clickable; nút X nằm trên tapShield nên vẫn đóng video bình thường.
- Không thay đổi IPTV, prefetch FormatInfo, gesture, thumbnail, sleep timer, source management hay UI khác.

## Mốc
- VersionName: **1.10.48**
- VersionCode: **66**
- Chờ CI + test điện thoại thật.

---

# ✅ BẢN ỔN ĐỊNH HIỆN TẠI — MOBILE 1.10.47 — 2026-09-20

Người dùng đã test trên điện thoại thật và xác nhận **1.10.47 đã xử lý được lỗi mini-player bị đơ/đứng hình**. Các chức năng khác của 1.10.46/1.10.47 hiện hoạt động ổn định.

## Baseline phát triển mới
- **VersionName:** `1.10.47`
- **VersionCode:** `65`
- **Baseline commit:** `e78d544d0e97eabb5dbac21c3d976414cbd410ed`
- **Feature commit:** `6418e89ff78f5dec09f921282ea5bcfe0a4b652f`
- **GitHub Actions:** `NM7 Mobile Final Build #358`
- **Run ID:** `35485031054`
- **Artifact:** `NM7-IPTV-Mobile-FINAL`
- **Artifact ID:** `10597149065`
- **Artifact SHA256:** `0f9c8fe8c1633d47ad24ca21ca7bc9dd2fd998dce840ac10bd764cf25e6b625b`
- **CI:** SUCCESS
- **Thiết bị thật:** người dùng xác nhận ổn định

## Trạng thái đã chốt
- Giữ được phiên YouTube mini-player khi chuyển sang IPTV và quay lại YouTube.
- Mini-player không còn lỗi đóng như các bản trước.
- Lỗi hình mini-player bị đơ/đứng hoặc có tiếng nhưng hình không chạy đã được xử lý bằng ExoPlayer PlayerView.
- Gesture vuốt tab YouTube, thumbnail rõ nét, prefetch/tối ưu mở video, IPTV, sleep timer và các chức năng đã ổn trước đó được giữ nguyên.

> **Quy ước phát triển:** Tất cả nâng cấp, sửa lỗi và cải tiến Mobile sau ngày 2026-09-20 phải lấy **NM7 IPTV Mobile 1.10.47** làm baseline. Không quay lại baseline cũ nếu không có yêu cầu rõ ràng.

---

# ĐANG BUILD — MOBILE 1.10.47 — 2026-09-20

Người dùng xác nhận 1.10.46 đã sửa được lỗi mini-player bị đóng khi quay lại YouTube. Các chức năng khác hiện ổn định và phải giữ nguyên. Lỗi còn lại: mini-player thường đứng hình/đơ một lúc rồi mới chạy tiếp, đôi khi âm thanh vẫn phát nhưng hình không cập nhật.

## Nguyên nhân kỹ thuật
1. Mini-player 1.10.46 dùng TextureView thô và tự quản lý SurfaceTexture.
2. Mỗi lần chuyển surface, bridge gọi clearVideoSurface() rồi setVideoTextureView(), sau đó seekTo(currentPosition) để ép frame.
3. clear + seek ngay trong lúc player đang chạy có thể làm video renderer flush/rebuffer trong khi audio renderer tiếp tục, tạo đúng hiện tượng audio chạy nhưng hình đứng/đen.

## Sửa 1.10.47
- Thay TextureView thô bằng ExoPlayer PlayerView chuẩn.
- Chỉ chuyển cùng một ExoPlayer giữa PlayerView fullscreen và PlayerView mini.
- Loại bỏ clearVideoSurface(), setVideoTextureView() và seekTo(currentPosition) trong đường mini-player.
- Khi chuyển mini giữa các Activity, detach PlayerView cũ rồi attach PlayerView mới; host cũ chỉ bị gỡ sau khi player đã chuyển.
- Khi mở lại fullscreen, detach mini PlayerView trước rồi mới gắn player lại vào SmartTube PlayerView.
- Giữ nguyên toàn bộ logic 1.10.46: giữ phiên YouTube qua IPTV, prefetch FormatInfo, gesture, thumbnail, IPTV, sleep timer và UI.

## Mốc kỹ thuật
- VersionName: **1.10.47**
- VersionCode: **65**
- Chờ CI và test thiết bị thật.

---

# ĐANG BUILD — MOBILE 1.10.46 — 2026-09-20

Người dùng test 1.10.45 và gửi video màn hình: mini-player vẫn mất sau YouTube mini → IPTV → YouTube, mini đôi lúc audio-only/đứng hình, và video YouTube vẫn mở chậm.

## Nguyên nhân xác định
- Mobile có hai pipeline IPTV. PlayerActivity đã suspend YouTube, nhưng MobileInlinePlayerProviderV2 còn 2 đường READY gọi stopYoutubeForIptv(), nên chính luồng inline trong video vẫn đóng hẳn phiên YouTube.
- Surface mini cũ bị gỡ trước khi surface mới attach chắc chắn.
- Trạng thái playWhenReady trước IPTV chưa được khôi phục sau khi mini surface mới sẵn sàng.
- FormatInfo YouTube bắt đầu sau engine init, làm thời gian khởi tạo player và thời gian gọi mạng nối tiếp nhau.

## Sửa 1.10.46
- Đổi cả 2 đường IPTV inline từ stop sang suspend.
- Ghi nhớ playWhenReady trước IPTV và chỉ resume khi TextureView mới attach thành công.
- Giữ surface cũ tới khi surface mới attach xong rồi mới gỡ.
- Prefetch FormatInfo ngay khi onNewVideo nhận video trong lúc engine chưa init; tái sử dụng kết quả khi engine ready và tránh request trùng.
- Giữ nguyên gesture, thumbnail, UI, IPTV, sleep timer và các chức năng ổn trước đó.

## Mốc kỹ thuật
- VersionName: **1.10.46**
- VersionCode: **64**
- Chờ CI và test thiết bị thật.

---

# BUILD THÀNH CÔNG — MOBILE 1.10.45 — 2026-09-20

GitHub Actions **NM7 Mobile Final Build #354** đã PASS toàn bộ.

- Run ID: `35482091272`
- Source commit build: `7d48d53035ccadd08a041b7f8ae54ac9a1120cf5`
- Artifact: `NM7-IPTV-Mobile-FINAL`
- Artifact ID: `10595859662`
- Artifact digest: `sha256:05bbedc4df9ac4f63e44dc98b10f5e5a40b780794866cd37b12dd72c2e437190`
- Patch SmartTube: PASS
- Lifecycle regression guards: PASS
- Gradle build Mobile: PASS
- Upload APK: PASS

Bản 1.10.45 sửa việc BrowseActivity không attach lại mini-player khi được REORDER_TO_FRONT, chờ SurfaceTexture sẵn sàng trước khi nối ExoPlayer để giảm lỗi audio-only/black mini-player, ép render lại frame hiện tại sau khi đổi surface, và giảm ngưỡng start/rebuffer YouTube để mở video nhanh hơn. Cần test thực tế trên điện thoại.

---

# ĐANG BUILD — MOBILE 1.10.45 — 2026-09-20

Người dùng test 1.10.44:
- Gesture vuốt YouTube và thumbnail tiếp tục ổn.
- Mini-player vẫn mất khi: YouTube mini → phát IPTV → quay lại YouTube.
- Mini-player đôi lúc có tiếng nhưng hình đen/đứng.
- Yêu cầu tối ưu thêm tốc độ mở video YouTube.

## Nguyên nhân xác định
1. BrowseActivity dùng REORDER_TO_FRONT. Sau khi 1.10.44 gỡ overlay mini khỏi màn IPTV, quay lại Browse cũ không chạy lại hàm attach vì trước đây attach chỉ nằm ở lúc khởi tạo Browse.
2. MobileMiniPlayer nối ExoPlayer với TextureView ngay sau addView, khi SurfaceTexture có thể chưa sẵn sàng. Điều này tạo race gây audio-only/black frame trên một số thiết bị.

## Sửa 1.10.45
- Trong MobileNm7Application.onActivityResumed(), mỗi lần BrowseActivity trở lại foreground đều gọi lại MobileMiniPlayer.attach().
- MobileMiniPlayer chờ TextureView SurfaceTexture available rồi mới nối ExoPlayer.
- Khi chuyển surface: clear video surface cũ → set TextureView mới → seek về chính currentPosition để ép renderer xuất frame lên surface mới; không đổi trạng thái play/pause của người dùng.
- Giữ cơ chế suspend 1.10.44: IPTV không finish phiên YouTube.
- Tối ưu startup YouTube: bufferForPlayback 750ms → 500ms; rebuffer 2000ms → 1500ms. Không thay forward-buffer/playlist/player ownership.
- Giữ nguyên gesture, thumbnail, IPTV, mini UI, sleep timer và các chức năng 1.10.43/1.10.44.

## Mốc kỹ thuật
- VersionName: **1.10.45**
- VersionCode: **63**
- Chờ CI và test thiết bị thật.

---

# BUILD THÀNH CÔNG — MOBILE 1.10.44 — 2026-09-20

GitHub Actions **NM7 Mobile Final Build #353** đã PASS toàn bộ.

- Run ID: `35481324535`
- Source commit build: `781d7673c70924dd89162f2020a39b48cd9e72bd`
- Artifact: `NM7-IPTV-Mobile-FINAL`
- Artifact ID: `10595628743`
- Artifact digest: `sha256:e6765564b757e45c84cabeab609735101a975e3e89c813cf9dbe73eff13c9dd9`
- Patch SmartTube: PASS
- Lifecycle regression guards: PASS
- Gradle build Mobile: PASS
- Upload APK: PASS

Bản 1.10.44 giữ nguyên toàn bộ tính năng 1.10.43 và thay hành vi khi IPTV bắt đầu phát: YouTube được suspend thay vì stop/finish, giữ nguyên video/vị trí và trạng thái mini để khi quay lại tab YouTube có thể dựng lại mini-player của video trước đó. Cần test thực tế chuỗi: YouTube mini → phát IPTV → quay lại YouTube.

---

# ĐANG BUILD — MOBILE 1.10.44 — 2026-09-20

Người dùng xác nhận **1.10.43**:
- Vuốt trái/phải giữa các tab YouTube: **đã hoạt động**.
- Thumbnail: **đã rõ nét**.
- Phát sinh lỗi mới: nếu mini-player YouTube đang tồn tại, sau đó phát IPTV thì khi quay lại tab YouTube mini-player bị mất và không còn biết video trước đó.

## Nguyên nhân
Khi IPTV thực sự bắt đầu phát, `PlayerActivity` gọi `MobileNm7Application.stopYoutubeForIptv()`. Hàm này gọi `PlaybackActivity.stopForNm7Iptv()`, đặt `sNm7Mini=false`, đánh dấu stopped và `finishReally()`, tức là đóng hẳn phiên YouTube.

## Sửa 1.10.44
- Thêm `PlaybackActivity.suspendForNm7Iptv()`.
- Khi IPTV bắt đầu phát: YouTube chỉ pause, detach surface/audio và giữ nguyên player/video/vị trí.
- Giữ `sNm7Mini=true` để khi quay lại Browse YouTube, `MobileMiniPlayer.attach()` dựng lại mini-player đúng video cũ.
- Trên màn hình IPTV, overlay mini cũ được gỡ để không che nội dung IPTV.
- Nút X mini-player vẫn gọi `stopYoutubeForIptv()` và đóng YouTube thật như trước.
- Hẹn giờ đóng app vẫn dùng đường stop thật.
- Giữ nguyên toàn bộ gesture vuốt, thumbnail, IPTV, player, phát nền và UI của 1.10.43.

## Mốc kỹ thuật
- VersionName: **1.10.44**
- VersionCode: **62**
- Chờ CI build và test trên thiết bị thật.

---

# BUILD THÀNH CÔNG — MOBILE 1.10.43 — 2026-09-20

GitHub Actions **NM7 Mobile Final Build #352** đã PASS toàn bộ.

- Run ID: `35480267429`
- Source commit build: `3617e902983a23217eb59799600f48bab169c319`
- Artifact: `NM7-IPTV-Mobile-FINAL`
- Artifact ID: `10595786897`
- Artifact digest: `sha256:1e14d566e12b79807c216a83a2d8172af75a1c465747b449bd57ab1e4d5e5c35`
- Patch SmartTube: PASS
- Lifecycle regression guards: PASS
- Gradle build Mobile: PASS
- Upload APK: PASS

Bản 1.10.43 giữ phần thumbnail rõ nét của 1.10.42 và bổ sung bắt gesture ngang tại `BrowseActivity.dispatchTouchEvent()` để chuyển section/tab YouTube. Cần test trên điện thoại thật để xác nhận vuốt trái/phải hoạt động đúng giữa **Trang chủ ↔ Kênh đăng ký ↔ Danh sách phát ↔ ...**.

---

# ĐANG BUILD — MOBILE 1.10.43 — 2026-09-20

Người dùng đã test 1.10.42: thumbnail YouTube đã rõ nét hơn; vuốt trái/phải đổi section vẫn chưa hoạt động.

## Nguyên nhân
Patch 1.10.42 không thực sự chèn listener gesture vào BrowseActivity trong source generated, nên APK không có xử lý vuốt section.

## Sửa 1.10.43
- Bắt gesture tại BrowseActivity.dispatchTouchEvent(), trước RecyclerView/card.
- Chỉ nhận vuốt bắt đầu trong browse_content.
- Vuốt trái chọn section kế tiếp; vuốt phải chọn section trước.
- TabLayout chuyển đồng bộ theo section: Trang chủ ↔ Kênh đăng ký ↔ Danh sách phát ↔ ...
- Không cướp touch: cuộn dọc và tap card giữ nguyên.
- Chặn multi-touch, phân biệt ngang/dọc, không quay vòng đầu/cuối.
- Giữ nguyên sửa thumbnail 1.10.42 và toàn bộ playback/lifecycle/mini-player/IPTV/hẹn giờ.

## Mốc kỹ thuật
- VersionName: 1.10.43
- VersionCode: 61
- Chờ CI build và test thiết bị thật.

---

# ĐANG BUILD — MOBILE 1.10.42 — 2026-09-20

Người dùng xác nhận **1.10.41 vẫn chưa xử lý được hai lỗi của 1.10.40**:
1. Vuốt trái/phải trong giao diện chính YouTube chưa chuyển qua lại các mục ở thanh tab phía trên như Super OK.
2. Thumbnail YouTube vẫn nhòe/mờ trên card lớn.

## Thay đổi 1.10.42 (version code 60)

- Bỏ cơ chế bắt vuốt ở `Nm7SwipeFrameLayout` bao ngoài vì RecyclerView/card con có thể giành chuỗi touch trước khi parent nhận đủ ACTION_UP.
- Gắn `Nm7SectionSwipeTouchListener` trực tiếp vào cả ba RecyclerView nội dung (grid, rows, settings). Khi gesture ngang đủ ngưỡng, RecyclerView dừng cuộn và chuyển đúng section liền kề bằng `selectSection()`; vuốt dọc/tap vẫn giữ hành vi cũ.
- Giảm ngưỡng phân biệt ngang/dọc để thao tác tự nhiên hơn trên điện thoại, vẫn chặn multi-touch và không quay vòng đầu/cuối.
- Thumbnail card video lớn dùng ảnh nguồn nguyên kích thước: Glide `SIZE_ORIGINAL`, `dontTransform()`, cache dữ liệu gốc; ưu tiên `maxresdefault`, sau đó ảnh nền chất lượng cao từ metadata, rồi mới fallback card URL. Không ép centerCrop ở bước decode để tránh lấy bitmap nhỏ rồi phóng.
- Không thay đổi player YouTube, IPTV, mini-player, phát nền, lifecycle chuyển nguồn hoặc hẹn giờ.

## Mốc kỹ thuật

- VersionName: **1.10.42**
- VersionCode: **60**
- Source commit: `5f58d25ceb89d81317c8e22dd5ddc3eb16ddd140`
- GitHub Actions Mobile Final Build run: **35479392064**
- Trạng thái tại thời điểm cập nhật: **đang chạy CI**.
- Chưa đánh dấu hai lỗi là PASS cho đến khi build thành công và người dùng test trên điện thoại thật.

---

# ĐÃ BUILD THÀNH CÔNG — MOBILE 1.10.41 — 2026-09-20

Người dùng báo **1.10.40 chưa ổn**: thumbnail mờ như phóng to và không vuốt ngang để đổi mục YouTube được. 1.10.39 vẫn là mốc chức năng đã được xác nhận.

## Sửa trong 1.10.41 (version code 59)

- Card video lớn dùng thumbnail maxres, dự phòng hq720, hqdefault và URL gốc; Glide giữ cache và lấy kích thước theo ImageView. Không áp dụng URL video cho card kênh/playlist không có video ID hợp lệ.
- Bổ sung nhận diện vuốt ngang trong vùng nội dung YouTube: trái sang mục kế tiếp, phải sang mục trước; đồng bộ lựa chọn thanh mục. Không quay vòng ở đầu/cuối.
- Phân biệt vuốt ngang với cuộn dọc, bỏ qua đa điểm và cử chỉ bị hủy, hủy thao tác chạm card khi nhận diện vuốt để không mở nhầm video.
- Vùng vuốt không chứa mini-player và thanh điều hướng IPTV. Chặn gesture thoát cạnh màn hình của lớp Activity cha trong chuỗi chạm ở vùng nội dung.
- Mã player, mini-player, dịch vụ nền, chuyển IPTV và hẹn giờ giữ nguyên.

## Kiểm tra

- Patch nguồn sạch thành công; 21 kiểm tra cấu trúc lifecycle PASS; XML hợp lệ.
- CI build và unit-test task thành công, có APK ARM64/ARMv7; kiểm tra chữ ký, ABI và manifest Mobile đạt.
- 12 kiểm tra logic cử chỉ bằng Java với stub Android PASS (trái/phải, cuộn dọc, tap, drag ngắn, cancel, đa điểm, RTL). Đây không phải kiểm thử dispatch touch/render trên thiết bị.
- [Build 35477926488 — SUCCESS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35477926488), Gradle 6 phút 44 giây.
- [Tải APK 1.10.41](https://github.com/phuongnm7/iptv-player-android/actions/runs/35477926488/artifacts/10595425938), hết hạn 2026-10-20.
- Source commit: `e29c86cab3a614e22aebd0fb0fa2007778b4e329`.
- Artifact SHA-256: `a4fa679f94eb94a2134dcb6a55d1f638aa9f5900f253d2e887096fbfb55a86df`.
- ARM64 APK SHA-256: `88121a0bab630e7ede509eea33d24c74387e91c45a789c657f6c582cb30e1757`.
- ARMv7 APK SHA-256: `9da0635aa3f35e04bd4d98da9df0c427a6d500128170192bfd2a928138fcaa06`.
- Chưa xác nhận thực tế độ nét/cử chỉ trên điện thoại. Chất lượng thumbnail còn phụ thuộc ảnh YouTube cung cấp; không hứa mọi video đều có maxres.

---

# ĐÃ BUILD THÀNH CÔNG — NM7 IPTV MOBILE 1.10.40 — GIAO DIỆN THEO SUPER OK

## Yêu cầu và mốc giữ nguyên

Người dùng yêu cầu tab YouTube giống ảnh `photo_2026-09-20_06-27-50.jpg` của Super OK và gửi thêm `Super_OK.apk` để tham khảo. Mốc chức năng là **1.10.39**, đã được người dùng xác nhận xử lý xong các lỗi trước đó.

## Thay đổi giao diện

- Nền tối, điểm nhấn cam, logo SmartTube, thanh tìm kiếm, nút tìm bằng giọng nói và tài khoản.
- Các mục nội dung dạng nút ngang; giữ dữ liệu, tên, thứ tự và tùy chọn mục của SmartTube hiện tại.
- Feed video một cột với thumbnail 16:9 bo góc, tiêu đề hai dòng, thông tin phụ, thời lượng và tiến độ xem.
- Nút ba chấm gọi menu video hiện có; thao tác nhấn giữ vẫn hoạt động.
- Thanh dưới của tab YouTube có bốn nút: YouTube, IPTV, Thư viện (playlist hiện có), Cài đặt SmartTube. Tab IPTV vẫn giữ giao diện hiện tại.
- Nhấn giữ thanh tìm kiếm mở menu cũ, bao gồm Làm mới. Nút micro gọi tìm kiếm giọng nói hiện có.
- Nội dung video/thumbnail/tài khoản là dữ liệu thật từ SmartTube, không sao chép nội dung mẫu trong ảnh.

## Giữ nguyên chức năng bản 1.10.39

- Không đổi mã playback YouTube, ngưỡng buffer, loader và luồng bàn giao IPTV/YouTube.
- Không đổi mini-player dùng chung, dịch vụ phát nền, IPTV player, hẹn giờ đóng app và manifest.
- Feed dùng adapter riêng cho từng nhóm dữ liệu để giữ phân trang, xóa, đồng bộ, tiếp tục tải và menu video.
- UI patch dùng chung cho CI và Windows, áp dụng lên đúng upstream đã pin; không sửa repo Android TV.
- RecyclerView của module SmartTube nâng từ 1.1.0 lên 1.2.1 để dùng ConcatAdapter cho feed phẳng.

## Kiểm tra đến hiện tại

- Patch áp dụng thành công trên nguồn upstream sạch.
- 21 kiểm tra cấu trúc lifecycle PASS.
- So sánh byte xác nhận PlaybackActivity, ExoPlayerInitializer, VideoLoaderController sinh ra giống bản 1.10.39; phương thức chuyển tab IPTV/YouTube cũng giữ nguyên.
- XML giao diện parse thành công. CI đã biên dịch thành công; unit-test task và assembleMobileDebug hoàn tất. Hai APK đã qua kiểm tra chữ ký/ABI và kiểm tra manifest Mobile.
- Chưa kiểm thử runtime/giao diện trên điện thoại cho 1.10.40; không xem các kiểm tra source là xác nhận thực tế.

## APK bàn giao 1.10.40

- Source commit: [3cdb120fc59f2cb2395104a0318755430595e789](https://github.com/phuongnm7/iptv-player-android/commit/3cdb120fc59f2cb2395104a0318755430595e789).
- Version code: **58**.
- [Build 35476892012 — SUCCESS](https://github.com/phuongnm7/iptv-player-android/actions/runs/35476892012). Gradle hoàn tất trong 6 phút 45 giây.
- [Tải NM7-IPTV-Mobile-FINAL](https://github.com/phuongnm7/iptv-player-android/actions/runs/35476892012/artifacts/10595085459): ARM64-v8a, armeabi-v7a và SHA256SUMS.txt.
- Artifact SHA-256: `b92bb0917206b5b68f41bfebbb3a8b38275fd5a99a7f5219984228a472390d46`.
- APK ARM64 SHA-256: `62811fed5f02bbfef4217f007cbebed5bca92979de16ea0210096ee316307567`.
- APK ARMv7 SHA-256: `48cbf89de9beec66c052e9364393536007c6a3a3ffc826097c23dd3c1530029a`.
- Artifact hết hạn: **2026-10-19**.
- Cần người dùng xác nhận giao diện/thao tác trên điện thoại: cuộn và tải thêm video, tìm kiếm/micro/tài khoản/menu, mini-player khi đổi tab, IPTV tiếp quản khi phát và hẹn giờ đóng app.
- **1.10.39 vẫn là mốc đã được người dùng test xác nhận. 1.10.40 đã build thành công, đang chờ test thiết bị.**

---

# TRẠNG THÁI XÁC NHẬN — NM7 IPTV MOBILE 1.10.39 — 2026-09-19

## Kết quả test thực tế

Người dùng đã cài và test **NM7 IPTV Mobile 1.10.39**. Các lỗi đã yêu cầu ở vòng 1.10.39 được xác nhận là **đã xử lý xong**:

1. Mini-player YouTube đang phát vẫn bấm được tab IPTV.
2. Chuyển sang tab IPTV khi chưa chọn kênh mới vẫn giữ YouTube mini-player phát.
3. YouTube chỉ dừng khi kênh IPTV đã sẵn sàng phát.
4. Giao diện SmartTube Mobile native đã áp dụng.
5. Hẹn giờ đóng app đã được thêm trong Tùy chọn ứng dụng.

Bản này đang được người dùng test thêm để phát hiện lỗi khác. Không có lỗi mới đang chờ xử lý tại thời điểm cập nhật.

## Mốc kỹ thuật bàn giao

| Hạng mục | Giá trị |
|---|---|
| Phiên bản | 1.10.39 |
| Version code | 57 |
| Nhánh | `fix/mobile-1.10.26-sleep-timer-icon` |
| Commit source | [18f486328c3422163920391d0486b756d7f1fd0c](https://github.com/phuongnm7/iptv-player-android/commit/18f486328c3422163920391d0486b756d7f1fd0c) |
| Build | [NM7 Mobile Final Build #347](https://github.com/phuongnm7/iptv-player-android/actions/runs/35452468579) — SUCCESS |
| Artifact | [NM7-IPTV-Mobile-FINAL](https://github.com/phuongnm7/iptv-player-android/actions/runs/35452468579/artifacts/10587855600) |
| Artifact SHA-256 | `f482d547fdb532864906d44c9690d6cc8c7ab82b38b9f64ebce26caef1b6c08b` |
| Hết hạn artifact | 2026-10-19 |
| ABI | Chỉ ARM64-v8a và armeabi-v7a |

## Phạm vi bản chuẩn hiện tại

- Chỉ **Mobile**; không có thay đổi nào đối với Android TV.
- SmartTube phone native, nền tối/điểm nhấn cam.
- IPTV và YouTube giữ player riêng; bàn giao quyền phát theo trạng thái phát thực tế.
- YouTube mini-player có thể tiếp tục phát khi duyệt tab IPTV, sau đó nhường quyền khi IPTV phát được.
- Hẹn giờ đóng app: preset 15–120 phút và tùy chỉnh 1–480 phút.

## Lưu ý cho vòng test tiếp theo

Nếu phát hiện lỗi mới, lưu kèm thao tác tái hiện, màn hình đang mở và kết quả mong muốn/thực tế. Không xem các build 1.10.35–1.10.38 là mốc ổn định cho luồng IPTV/YouTube; mốc hiện tại là **1.10.39**.

---

# CẬP NHẬT MỚI NHẤT — NM7 IPTV MOBILE 1.10.39 — 2026-09-19

## Kết quả build

- Commit: [18f486328c3422163920391d0486b756d7f1fd0c](https://github.com/phuongnm7/iptv-player-android/commit/18f486328c3422163920391d0486b756d7f1fd0c)
- GitHub Actions: [NM7 Mobile Final Build #347](https://github.com/phuongnm7/iptv-player-android/actions/runs/35452468579)
- Kết quả: **SUCCESS**
- Artifact: [NM7-IPTV-Mobile-FINAL](https://github.com/phuongnm7/iptv-player-android/actions/runs/35452468579/artifacts/10587855600)
- Artifact ID: `10587855600`
- SHA-256 ZIP: `f482d547fdb532864906d44c9690d6cc8c7ab82b38b9f64ebce26caef1b6c08b`
- Hết hạn: **2026-10-19**
- Build có đúng hai APK ARM64-v8a và armeabi-v7a; CI kiểm tra chữ ký, không có x86/x86_64 hoặc libvlc.so.

## Phản hồi và nguyên nhân đã xác minh

Bản 1.10.38 vẫn có lỗi: mini-player YouTube đang mở thì bấm tab IPTV không chuyển được, phải đóng mini-player; tải video YouTube chậm.

Nguyên nhân chính không phải toàn bộ vùng mini-player che thanh tab. `MainActivity.onResume()` tự đặt tab thành YouTube khi thấy cờ phát nền và gọi đưa SmartTube lên trước, kể cả sau thao tác bấm IPTV. Vì vậy ứng dụng bị quay ngược về YouTube.

Source vẫn dùng SmartTube phone native: `BrowseActivity`/RecyclerView và `PlaybackActivity`/ExoPlayer. Không dùng WebView để browse hoặc phát YouTube.

## Thay đổi 1.10.39

- MainActivity chỉ phục hồi YouTube khi tab đang chọn vẫn là YouTube; callback trễ cũng kiểm tra lại tab. Bấm IPTV không còn bị ghi đè.
- Dùng `MobileMiniPlayer` chung giữa Browse và màn hình IPTV. Surface được chuyển sang view mới trước khi bỏ view cũ; mini-player nằm phía trên thanh tab để thanh tab nhận được thao tác.
- Không dừng YouTube lúc bấm tab hay chọn kênh IPTV. IPTV chuẩn bị tắt tiếng, không lấy audio focus; chỉ khi phát được (`READY + playWhenReady`) mới dừng YouTube, nhận audio focus và bật tiếng. Có đường xử lý cho inline và PlayerActivity.
- SmartTube Mobile chuyển sang nền tối, điểm nhấn cam và tiêu đề SmartTube Mobile; giữ giao diện cảm ứng native, không đưa UI TV vào.
- Giảm ngưỡng bộ đệm để bắt đầu YouTube từ 2500ms xuống 750ms, sau rebuffer từ 5000ms xuống 2000ms; bổ sung log `NM7Startup` để đo thời gian lấy format video.
- Thêm **Tùy chọn ứng dụng → Hẹn giờ đóng app**: 15/30/45/60/90/120 phút hoặc tùy chỉnh 1–480 phút, tắt hẹn giờ, handler và AlarmManager dự phòng. Hết giờ dừng IPTV, YouTube, service nền và đóng task.

## Giới hạn xác minh hiện tại

CI đã áp dụng patch lên SmartTube commit pin `4825d6aa8b6f1d3181927f9e96c7d89cab13d510`, chạy 21 structural checks và build/test thành công.

**1.10.39 chưa được xác nhận runtime trên thiết bị thật.** Cần test: bấm tab IPTV khi mini-player còn phát, chuyển tab khi có/không có IPTV trước đó, chọn kênh IPTV tải chậm, Home/khóa màn hình, Back/mini-player và hẹn giờ 1 phút cho cả IPTV lẫn YouTube.

Giảm buffer chỉ cải thiện đoạn chờ dữ liệu trước khi phát; chưa có số đo máy thật để kết luận toàn bộ độ chậm tải YouTube đã được xử lý.

---

# CẬP NHẬT MỚI NHẤT — MOBILE 1.10.38 — 2026-09-19

## Điểm bàn giao hiện tại

- Repo: `phuongnm7/iptv-player-android`.
- Nhánh: `fix/mobile-1.10.26-sleep-timer-icon`.
- **1.10.37: người dùng đã test, đánh giá “gần ổn”; còn lỗi mini-player khi chuyển tab và tải video YouTube chậm.**
- **1.10.38: đã cập nhật source và BUILD SUCCESS; chưa có kết quả test thiết bị thật.**
- Chỉ Mobile. Không tác động repo Android TV, không chuyển sang kiến trúc một ExoPlayer chung.

Mục này cập nhật trạng thái mới nhất; các mục phía dưới là lịch sử, bao gồm trạng thái chờ test ở thời điểm cũ.

## Phản hồi test và yêu cầu cần giữ

Video đã cung cấp: `video_2026-09-19_22-02-48.mp4` (khoảng 92,6 giây). Đã xem các khung hình trích theo thời gian và đối chiếu source. Không yêu cầu gửi lại video.

1. Nếu IPTV đang phát trước khi mở video YouTube: từ mini-player YouTube chuyển sang tab IPTV phải phát lại kênh IPTV đó.
2. Nếu không có IPTV phát trước đó: chuyển sang tab IPTV phải giữ YouTube mini-player tiếp tục phát đến khi người dùng chọn kênh IPTV.
3. Khi chưa chọn kênh IPTV, chuyển qua lại hai tab không được đóng mini-player hoặc mất phiên YouTube.
4. Cải thiện thời gian bắt đầu phát video YouTube.
5. Giữ hành vi Home/khóa màn hình, Back về mini-player, bấm mini-player quay lại video và chuyển nguồn không crash.

## Nguyên nhân và source đã thay đổi

- `HomeTabBar.openIptv()` trước đây gọi `stopYoutubeForIptv()` vô điều kiện ngay khi bấm tab; đây là đường đóng YouTube dù chưa chọn kênh.
- Thêm `MobileInlinePlayerProviderV2.shouldResumeIptvAfterYoutube()`, dựa vào `resumeAfterYoutube` và kênh đang lưu trong phiên inline.
- `HomeTabBar` chỉ đóng YouTube khi cờ khôi phục IPTV inline đang bật; nếu không, mở MainActivity và giữ YouTube.
- `MobileNm7Application` bổ sung `installYoutubeMiniPlayer(Activity)` để gắn cùng SmartTube player vào TextureView ở màn hình IPTV, có nút đóng và thao tác quay lại video.
- Chọn kênh trong `playInline()` tiếp tục là điểm gọi dừng YouTube để chuyển sang IPTV.
- Script SmartTube bổ sung `initializePlayer()` sau `onViewInitialized()` trong onCreate để khởi tạo sớm hơn; onStart/onResume vẫn chỉ tạo player khi null.
- Đây mới là thay đổi thời điểm khởi tạo, **chưa có đo đạc chứng minh giảm độ trễ tải YouTube hoặc xác định đầy đủ nguyên nhân tải chậm**. Không coi phần hiệu năng đã được xác nhận.
- VersionName **1.10.38**, versionCode **56**.
- Bộ kiểm tra source tăng từ 20 lên **21 kiểm tra cấu trúc**. Những kiểm tra này không thay thế test hành vi runtime.

## Kết quả CI đã xác minh

- Commit source cuối: [2e947f81e8e822e5349082313e7ed939128fcadb](https://github.com/phuongnm7/iptv-player-android/commit/2e947f81e8e822e5349082313e7ed939128fcadb).
- Workflow: **NM7 Mobile Final Build**.
- Run: [#345 — 35450879471](https://github.com/phuongnm7/iptv-player-android/actions/runs/35450879471).
- Trạng thái kiểm tra trực tiếp: **completed / success**.
- Artifact APK: [NM7-IPTV-Mobile-FINAL](https://github.com/phuongnm7/iptv-player-android/actions/runs/35450879471/artifacts/10586886241).
- Artifact ID: `10586886241`.
- SHA-256 ZIP: `365454aee8abc25d8b38f4d62199dcb6733cd8eec4bb986b56cccfdd35702e3f`.
- Hết hạn artifact: **2026-10-19**.
- Artifact bằng chứng source: `Mobile-lifecycle-source-proof`, ID `10587120455`.
- Workflow kiểm tra đúng hai APK Mobile ARM64-v8a / armeabi-v7a, chữ ký và loại trừ x86/x86_64/libvlc trước khi upload.
- Dùng artifact của đúng run trên, tránh các build trung gian được tạo khi cập nhật từng file.

## Điểm còn phải kiểm tra ở lượt tiếp theo

**Chưa đánh dấu 1.10.38 ổn định hoặc hết lỗi.** Cần kiểm tra hai tình huống có/không có IPTV trước YouTube, trạng thái IPTV đã pause/đóng, lặp chuyển tab, chọn kênh rồi quay lại YouTube và Home/khóa màn hình.

Cần rà tiếp lifecycle khi giữ YouTube ở MainActivity: provider inline vẫn có đường gọi stopBackgroundService khi resume; lớp mini-player thêm vào MainActivity cần được dọn đúng khi chọn kênh hoặc session YouTube đóng. Nhánh khôi phục mới dựa vào inline provider; cần xác minh riêng nếu phiên IPTV đến từ PlayerActivity. Đây là các điểm cần xác minh từ source/runtime, không phải lỗi mới đã được người dùng xác nhận.

Về tải video: cần phân biệt thời gian lấy metadata/URL, tải dữ liệu, buffer và frame đầu; thay đổi onCreate hiện tại chưa đủ bằng chứng để kết luận xử lý xong độ trễ trong video test.

---

# CẬP NHẬT TIẾN ĐỘ — NM7 IPTV MOBILE 1.10.37 — 2026-09-19

## Phạm vi

- Repository: `phuongnm7/iptv-player-android`
- Branch: `fix/mobile-1.10.26-sleep-timer-icon`
- Chỉ xử lý **NM7 IPTV Mobile**. Không có thay đổi nào đối với dự án Android TV.
- Không quay lại kiến trúc một ExoPlayer dùng chung cho IPTV và YouTube.

## Trạng thái bản 1.10.35 và 1.10.36

Người dùng đã cài và test thực tế: **cả 1.10.35 và 1.10.36 vẫn còn các lỗi chuyển IPTV/YouTube đã báo**. Hai bản này không được xem là ổn định và không được dùng làm mốc bàn giao chức năng.

Nguyên nhân đã xác định từ source:

1. **1.10.35 chỉ là bản sửa compile**, không thay đổi lifecycle runtime so với patch lỗi trước đó.
2. IPTV bị dừng khi `PlaybackActivity` bắt đầu hoặc Activity được mở, thay vì đợi YouTube thật sự đạt `STATE_READY + playWhenReady`.
3. Player IPTV thực tế ở màn hình Mobile là `MobileInlinePlayerProviderV2`; patch cũ chỉ xử lý `PlayerActivity`, nên bỏ sót player người dùng đang xem.
4. Bản Windows và GitHub Actions đã từng dùng các patch khác nhau; một bridge reflection dùng sai package `vn.phuongnm7.iptvplayer` và nuốt exception.
5. Patch Back/Home cũ tự dựng mini-player ngoài lifecycle thực tế của SmartTube và không gắn đầy đủ surface/player.
6. Source SmartTube trước đây không được khóa commit, vì vậy cùng source NM7 có thể cho APK khác nhau khi build.

## Các thay đổi trong 1.10.37

- Khóa SmartTube phone fork tại commit `4825d6aa8b6f1d3181927f9e96c7d89cab13d510`; bỏ việc merge history upstream thay đổi theo thời điểm build.
- Windows và GitHub Actions dùng chung `scripts/patch-mobile-v37.py` cùng các Java fragment.
- Khi chỉ mở YouTube Browse, IPTV inline tiếp tục phát.
- Chỉ khi YouTube player báo **READY và playWhenReady** thì mới bàn giao quyền phát và giải phóng cả hai owner IPTV; đồng thời hủy các callback IPTV retry còn chờ.
- Khi người dùng chọn kênh IPTV, đóng session YouTube đang tồn tại trực tiếp, không gửi phím pause media toàn cục.
- Giữ SmartTube player qua Home/khóa màn hình/mini-player; chỉ khởi tạo player khi player hiện tại là null.
- Back lần đầu (API 33+ và Back cũ) đưa về Browse, gắn cùng player vào `TextureView` của mini-player; bấm mini-player quay lại video mà không tự phát lại video đã bị người dùng pause.
- Browse và Playback dùng `singleTop` + reorder thay cho `singleTask`, tránh xóa PlaybackActivity khỏi back stack.
- Bridge service nền dùng đúng package `vn.phuong.iptvplayer`; notification mở lại YouTube và wake lock được giải phóng cùng service.
- Service nền chỉ được thay đổi sau khi YouTube thực sự nhận quyền phát; không làm tắt service IPTV khi YouTube mới mở nhưng chưa phát video.

## Build 1.10.37 đã hoàn tất

- Version: **1.10.37**
- Version code: **55**
- Commit build cuối: [f0581431ba7299e89a540914451e2e9f5f309644](https://github.com/phuongnm7/iptv-player-android/commit/f0581431ba7299e89a540914451e2e9f5f309644)
- GitHub Actions workflow: **NM7 Mobile Final Build**
- Run: [#338](https://github.com/phuongnm7/iptv-player-android/actions/runs/35437270162)
- Kết quả: **SUCCESS**
- Gradle: `:app:testMobileDebugUnitTest assembleMobileDebug`
- Artifact: [NM7-IPTV-Mobile-FINAL](https://github.com/phuongnm7/iptv-player-android/actions/runs/35437270162/artifacts/10583090783)
- Artifact ID: `10583090783`
- Artifact SHA-256: `973162409d6bf5bd5fe6ee10f15bc88400e6302b1ac086c18fc8ba12a877230c`
- Artifact hết hạn: **2026-10-19**

APK được đóng gói đúng **2 bản Mobile**:

| Kiến trúc | Tên APK | SHA-256 |
|---|---|---|
| ARM64-v8a | `NM7-IPTV-Mobile-1.10.37-arm64-v8a.apk` | `415ef2819fe2f73231cd7034b2012ab4e00171c80bfa368b4cc7a62defef443c` |
| armeabi-v7a | `NM7-IPTV-Mobile-1.10.37-armeabi-v7a.apk` | `f5256b5e3f603591d288ee6cf9ee27e6a0d27c0dd57a6fca943920b7c8a3d867` |

Không chứa ABI x86/x86_64 hoặc `libvlc.so`.

## Mức xác minh hiện tại

CI đã xác nhận patch được áp dụng lên source SmartTube đã khóa, 20 kiểm tra lifecycle source đạt, unit test/build thành công và APK có đúng hai ABI ARM.

**Chưa có kết quả xác nhận runtime trên thiết bị thật cho 1.10.37.** Vì vậy chưa được ghi nhận là đã sửa xong toàn bộ lỗi.

Các tình huống cần xác nhận khi test 1.10.37:

1. IPTV đang phát → mở YouTube Browse, chưa chọn video → IPTV vẫn phát.
2. Chọn video YouTube → IPTV chỉ dừng khi video YouTube thực sự bắt đầu phát.
3. YouTube đang phát → Home hoặc khóa màn hình → YouTube tiếp tục phát.
4. Mở lại app → vẫn giữ đúng phiên YouTube đang phát.
5. YouTube đang phát → Back một lần → Browse + mini-player, video tiếp tục phát.
6. Bấm mini-player → quay lại video đang phát.
7. Chuyển IPTV ↔ YouTube nhiều lần → không crash và không mất player.


---


# CẬP NHẬT TIẾN ĐỘ — MOBILE 1.10.36 — 2026-09-19 11:00 +07:00

## Trạng thái mới nhất

Bản **NM7 IPTV Mobile 1.10.35 đã được người dùng cài và test trên máy thật**. Kết quả: **các lỗi runtime chính vẫn còn**, chưa được xác nhận là đã xử lý.

Vì vậy đã tiếp tục cập nhật source sang **1.10.36**, chỉ dành cho **Mobile**, không build Android TV.

## Source/lifecycle đã cập nhật

### 1. IPTV → YouTube Browse

Không còn dùng việc tạo SmartTube PlaybackActivity làm tín hiệu để pause IPTV.

Trong MobileNm7Application:
- bỏ pauseIptvPlayer() khỏi nhánh onActivityStarted(PlaybackActivity);
- IPTV chỉ được release khi SmartTube báo **ExoPlayer thực sự ở STATE_READY và playWhenReady=true**.

Trong SmartTube PlaybackActivity:
- hook được đặt trực tiếp vào listener của mPlayer;
- khi video thực sự bắt đầu phát, gọi reflection tới MobileNm7Application.pauseIptvForYoutube().

Mục tiêu: **YouTube Browse không làm IPTV dừng; chỉ khi video YouTube thực sự bắt đầu phát thì IPTV mới nhường player.**

### 2. YouTube BACK

Đã patch phone PlaybackActivity.onBackPressed() theo hướng:
- tránh thoát playback session ngay ở BACK đầu tiên;
- đưa BrowseActivity lên trước bằng FLAG_ACTIVITY_REORDER_TO_FRONT;
- giữ player/session để Browse có thể tiếp tục hiển thị mini-player;
- có fallback về super.onBackPressed() nếu đường navigation thất bại.

### 3. HOME / khóa màn hình

SmartTube phone được patch thêm:
- onUserLeaveHint();
- đặt background mode PLAY_BEHIND;
- khởi động BackgroundPlaybackService của NM7 với cờ YouTube;
- tiếp tục dùng player/session hiện tại thay vì tạo một YouTube ExoPlayer thứ hai.

Mục tiêu: **HOME/khóa màn hình không làm mất session YouTube hiện tại.**

### 4. Giữ player khi tab/lifecycle thay đổi

SmartTube onStop() được sửa để không gọi maybeReleasePlayer() khi:
- đang có background mode;
- đang là NM7 tab switch;
- hoặc engine đang bị block.

Điều này nhằm tránh release player do lifecycle transition đơn thuần.

## Kiểm tra source đã hoàn tất

Đã xác minh insertion point thực tế trong SmartTube phone fork:
- PlaybackActivity.createPlayerObjects()
- mPlayer.addListener(new Player.EventListener()...)
- onStop()
- onBackPressed()

Không dựa vào source shape giả định.

## Build 1.10.36

Đã tăng:
- versionCode: **54**
- versionName: **1.10.36**

GitHub Actions:
- Workflow: **NM7 Mobile Final Build**
- Run: **#336**
- Run ID: **35433125590**
- Job ID: **105871107874**
- Commit: **82acad1e2c98ec0d123f1b3258c81c5ab8b2379c**
- Kết quả: **SUCCESS**

Gradle:
- :app:testMobileDebugUnitTest assembleMobileDebug
- Gradle 8.13
- Build thành công.

APK output:
- NM7-IPTV-Mobile-1.10.36-arm64-v8a.apk
- NM7-IPTV-Mobile-1.10.36-armeabi-v7a.apk

Không có x86, x86_64 hoặc libvlc.so.

Artifact:
- NM7-IPTV-Mobile-FINAL
- Artifact ID: **10581711430**
- SHA256 artifact: bfb06d7e9e205db08f20115049bb434be6d3b0da5b51a1ca6396a7eae8ae4999
- Hạn lưu artifact: **2026-10-19**

SHA256 APK:
- ARM64: 103b1b999f88dd0901b70d155ddca11df602d1193e0cd72a66a128c2c0c303d1
- ARMv7: 291f64a2e62dbd82bb244bd94d3c0c04048b26bde6f5d6809562ad51750e5a8e

## Quan trọng: chưa runtime PASS

Run #336 chỉ chứng minh source compile, unit test/build pass, Mobile APK được đóng gói đúng và chỉ có 2 ABI ARM.

**Chưa chứng minh runtime đã hết lỗi.**

### Test bắt buộc trên 1.10.36

1. IPTV đang phát → bấm YouTube → **chưa chọn video** → IPTV vẫn phát.
2. Chọn video YouTube → IPTV chỉ dừng khi video thực sự bắt đầu phát.
3. YouTube đang phát → HOME/khóa màn hình → session/video không mất.
4. Mở lại NM7 → đúng YouTube session/video, không spinner bất thường.
5. YouTube đang phát → BACK 1 lần → Browse + mini-player, video vẫn tiếp tục.
6. Bấm mini-player → quay lại video lớn đúng session/vị trí.
7. Chuyển IPTV ↔ YouTube nhiều lần → không crash, không mất player.
8. Không xuất hiện thành phần Android TV trong bản Mobile.

## Điểm dừng hiện tại

**1.10.36 đã build PASS, đang chờ người dùng test runtime trên máy thật.**

Không tiếp tục sửa theo suy đoán trước khi có kết quả test 1.10.36.

---

# TEST HANDOFF — MOBILE 1.10.35 — 2026-09-19

## Build Windows đã hoàn tất

Người dùng đã chạy build Mobile trên Windows và xác nhận **BUILD SUCCESSFUL**.

APK đã tạo thành công:
- `NM7-IPTV-Mobile-1.10.35-arm64-v8a.apk` — **61,915,373 bytes**
- `NM7-IPTV-Mobile-1.10.35-armeabi-v7a.apk` — **51,421,079 bytes**
- Chỉ có 2 ABI Mobile: **ARM64 + ARMv7**.
- **Không build Android TV** trong vòng này.
- Các cảnh báo `META-INF/* not protected by signature` là warning đóng gói, không phải lỗi build.

## Lỗi compile 1.10.34 đã được xử lý

Build 1.10.34 trước đó dừng tại SmartTube phone `BrowseActivity.java` với lỗi:

```
error: variable active is already defined in method installNm7MiniPlayer()
```

Nguyên nhân: patch mini-player khai báo trùng biến cục bộ `active` trong cùng method/lambda scope.

Đã sửa build script:
- Xóa khai báo trùng.
- Tái sử dụng reference `active` đã có.
- Không thay đổi logic IPTV/YouTube lifecycle vì đây chỉ là compile fix.

Commit:
- `2c7928e96dd416ca9667362edee8a3c85fa8a35a` — fix duplicate mini-player variable.
- `1ac30b9b414f99f92bb59643f04f60cc006f4ef4` — bump Mobile to 1.10.35.

## Nội dung runtime đang chờ test

Bản 1.10.35 chứa toàn bộ source/lifecycle hotfix đã có ở 1.10.34, gồm:

1. **IPTV → YouTube Browse**
   - Chuyển sang tab YouTube khi chưa chọn video không được tự release IPTV.
   - IPTV chỉ nhường player khi SmartTube PlaybackActivity thực sự bắt đầu video.
   - PlayerActivity không tự `startPlayer()` trong lúc tab YouTube/handoff đang active.
   - Ownership switch được giới hạn cho trường hợp thực sự vào IPTV.

2. **YouTube HOME/background**
   - SmartTube giữ player hiện tại khi rời app.
   - Có đường xử lý `onUserLeaveHint()` và `onPause()` để duy trì background playback.
   - Khi quay lại, không initialize lại player nếu player cũ vẫn còn.
   - Giữ đúng YouTube tab/session.

3. **YouTube BACK → mini-player**
   - SmartTube phone dùng `OnBackInvokedCallback.PRIORITY_DEFAULT` trên Android mới.
   - BACK lần đầu phải đưa Browse lên trước, giữ PlaybackActivity/player sống và gắn video vào mini-player.
   - Browse có retry attach mini-player sau 150ms và 500ms để tránh race lifecycle/surface.
   - Không tạo thêm một ExoPlayer YouTube thứ hai.

4. **Mobile-only**
   - Vòng test này chỉ dành cho **NM7 IPTV Mobile**.
   - Không đưa Android TV/UI TV vào APK Mobile.

## Trạng thái

**BUILD PASS — RUNTIME CHƯA XÁC NHẬN.**

Không đánh dấu 1.10.35 là PASS về chức năng cho đến khi người dùng cài APK và test trên máy thật.

### Checklist người dùng đang test

1. IPTV đang phát → bấm YouTube, **chưa chọn video** → IPTV vẫn phát.
2. Chọn video YouTube → IPTV mới dừng/release.
3. YouTube đang phát → HOME/khóa màn hình → YouTube tiếp tục phát nền.
4. Mở lại NM7 → vẫn ở YouTube, đúng video/session, không spinner bất thường.
5. YouTube đang phát → BACK 1 lần → Browse + mini-player, video vẫn tiếp tục.
6. Bấm mini-player → quay lại full player đúng video/vị trí.
7. Chuyển IPTV ↔ YouTube nhiều lần → không crash, không mất player.
8. Không xuất hiện thành phần Android TV trong bản Mobile.

## Điểm dừng hiện tại

**Không sửa thêm source trước khi có kết quả test 1.10.35 từ máy thật.**

Nếu test phát hiện lỗi, vòng tiếp theo sẽ dựa trên **bước tái hiện chính xác + lifecycle/source/log thực tế**, không sửa theo suy đoán.

Cập nhật: **2026-09-19**.

---

# BUILD FIX — MOBILE 1.10.35 — 2026-09-19

Build 1.10.34 failed during SmartTube Java compilation. The compiler reported `variable active is already defined in method installNm7MiniPlayer()` in the generated phone `BrowseActivity.java`. Root cause: the mini-player click lambda redeclared the enclosing local variable `active`; Java does not permit that shadowing in this lambda scope.

## Fix
- Build script PATCH21 removes the duplicate local declaration and reuses the existing `active` reference.
- Mobile version bumped to **1.10.35**, versionCode **53**.
- No IPTV/YouTube lifecycle behavior was otherwise changed by this compile fix.
- TV is not included in the Mobile build.

Commit:
- `2c7928e96dd416ca9667362edee8a3c85fa8a35a` — fix duplicate mini-player variable in build script.
- `1ac30b9b414f99f92bb59643f04f60cc006f4ef4` — bump Mobile to 1.10.35.

## Next Windows build
Run:
```powershell
git pull origin fix/mobile-1.10.26-sleep-timer-icon
powershell -ExecutionPolicy Bypass -File .\scripts\build-mobile-windows.ps1
```

Do not reuse the failed 1.10.34 APK; the next build output must be **1.10.35**.

---

# HOTFIX 1.10.34 — BỔ SUNG PATCH19 SAU KHI 1.10.33 FAIL THỰC TẾ

## Kết luận source sau khi rà lại

1. **Không được dùng lifecycle của PlayerActivity để tự khởi động lại IPTV trong lúc tab YouTube đang active.** Đã chặn `startPlayer()` trong `PlayerActivity.onStart()` khi tab là YouTube/handoff.
2. **Ownership switch chỉ xảy ra khi người dùng thực sự vào IPTV.** `MobileNm7Application.onActivityStarted(PlayerActivity)` không còn gửi MEDIA_PAUSE cho SmartTube nếu tab hiện tại vẫn là YouTube.
3. **Android 13–16 Back dùng API mới.** SmartTube phone PlaybackActivity được patch bằng `OnBackInvokedCallback.PRIORITY_DEFAULT`; giữ thêm `onBackPressed()` cho Android cũ. Android 16 target API 36 không còn dispatch `onBackPressed()` mặc định. citeturn6search0turn5search0
4. **Mini-player attach được retry 150ms + 500ms** và có thể attach lại vào TextureView hiện có nếu BrowseActivity đã tồn tại. Điều này xử lý race giữa `REORDER_TO_FRONT`, `onResume()` của Browse và thời điểm PlaybackActivity chuyển video surface.
5. **HOME:** onPause có fallback start media foreground service; onResume nhận biết `nm7.youtube.background=1`, khôi phục foreground state và xóa marker thay vì để marker tồn tại sang transition tiếp theo.

## Version

- **1.10.34**
- versionCode **52**
- SmartTube patch **PATCH19**

## Cơ sở kỹ thuật

Android yêu cầu media playback background phải được duy trì bằng foreground service loại `mediaPlayback`; Media3 khuyến nghị player/session nằm trong MediaSessionService cho kiến trúc background hoàn chỉnh. Bản NM7 hiện tại vẫn giữ SmartTube làm owner của YouTube player, vì vậy không chuyển ExoPlayer YouTube sang một player thứ hai trong service — làm vậy sẽ tạo thêm một engine và phá yêu cầu giữ đúng session/video. citeturn1search1turn4search1

**Trạng thái: source đã cập nhật, chưa runtime-verified.**

---

# HOTFIX 1.10.34 — 2026-09-19 — SỬA LẠI LIFECYCLE SAU KHI 1.10.33 VẪN LỖI

1. **IPTV → YouTube:** `PlayerActivity.onStart()` không được tự `startPlayer()` khi tab đang là YouTube/handoff. Trước đây Activity IPTV có thể tự dựng lại ExoPlayer trong lúc YouTube đang ở Browse, làm transition sai trạng thái.
2. **Không pause YouTube nhầm khi IPTV Activity xuất hiện:** `MobileNm7Application.onActivityStarted(PlayerActivity)` giờ chỉ gửi MEDIA_PAUSE và dừng service khi tab thực sự là IPTV. Việc tạo/resume PlayerActivity trong ngữ cảnh YouTube không còn cướp ownership của YouTube.
3. **YouTube HOME:** SmartTube `onResume()` giờ nhận biết lần quay lại từ HOME, xóa marker background, phục hồi `BACKGROUND_MODE_DEFAULT`, nhưng vẫn dùng player hiện có. `onPause()` cũng có đường dự phòng khởi động media foreground service nếu `onUserLeaveHint()` không phải callback duy nhất trên thiết bị.
4. **BACK:** không tiếp tục dựa vào `Activity.onBackPressed()` deprecated. SmartTube phone `PlaybackActivity` được patch để đăng ký `OnBackInvokedCallback` ở `PRIORITY_DEFAULT`, rồi gọi đúng handler mini-player. Manifest không còn opt-out `enableOnBackInvokedCallback=false`.
5. **Version:** Mobile 1.10.34 / versionCode 52 / build script PATCH19.

Lý do kỹ thuật: Android 13+ dùng `OnBackInvokedDispatcher`, và Android 16 target API 36 không còn dispatch `onBackPressed()` mặc định. Android docs khuyến nghị callback mới. citeturn0search0turn5search0

**Không thay đổi kiến trúc TV. Bản này vẫn Mobile-only.**

**Chưa runtime-verified.** Phải build Windows và test máy thật trước khi kết luận PASS.

---

# HOTFIX 1.10.33 — 2026-09-19 — PHÂN TÍCH VIDEO MÁY THẬT + SỬA 3 ĐIỂM LIFECYCLE

## Phân tích trực tiếp video người dùng gửi

Video `video_2026-09-19_08-52-18.mp4` (~82.7 giây) cho thấy rõ ba lỗi runtime:

1. **IPTV → YouTube:** trước khi YouTube playback bắt đầu, luồng IPTV không được bảo toàn ổn định qua transition. Source hiện tại có một lỗ hổng rõ trong `PlayerActivity.onDestroy()`: `keepPlayerForTabSwitch`/handoff đã được đặt nhưng `onDestroy()` vẫn có thể gọi `releasePlayer()` vì `backgroundPlaybackActive=false`. Đây là đường release sai đối với navigation-only handoff.
2. **HOME → quay lại YouTube:** video vẫn tồn tại và có thời điểm xuất hiện cửa sổ video nổi trên launcher, nhưng khi quay lại app có các đoạn spinner/loading kéo dài trước khi hình trở lại. Nguyên nhân source rõ ràng: SmartTube `PlaybackActivity.onStart()` luôn gọi `initializePlayer()`, kể cả khi `mPlayer` cũ vẫn còn sống sau HOME. Điều này có thể dựng lại session thay vì dùng player hiện tại.
3. **BACK 1 lần:** video trở về Browse nhưng mini-player nội bộ không xuất hiện ổn định. Build target là API 36. Trên Android 16, `onBackPressed()` không còn được gọi mặc định khi target API 36; vì patch mini-player hiện tại nằm trong `onBackPressed()`, đường đó có thể bị bypass hoàn toàn.

## Sửa 1.10.33

### A. Giữ IPTV trong YouTube Browse

`PlayerActivity.onDestroy()` nay có guard:

- không release IPTV nếu Activity đang rời foreground vì YouTube handoff;
- kiểm tra `keepPlayerForTabSwitch`, `isYoutubeHandoffPending()` và tab YouTube trước khi release;
- chỉ release bình thường khi không còn ngữ cảnh handoff.

Mục tiêu vẫn giữ nguyên:
**YouTube Browse không được dừng IPTV; chỉ khi SmartTube PlaybackActivity thực sự bắt đầu video thì `pauseIptvPlayer()` mới release IPTV.**

### B. Không reset YouTube player khi quay lại từ HOME

Build script PATCH18 sửa SmartTube phone `PlaybackActivity.onStart()`:

```
if (VERSION.SDK_INT > 23 && mPlayer == null) {
    initializePlayer();
}
```

Nếu player cũ còn sống, không gọi `initializePlayer()` lần nữa. Điều này giữ nguyên player/session/vị trí thay vì tạo lại và gây spinner.

### C. Back trên Android 16 phải đi qua patch mini-player

Mobile target API 36 chạy trên Android 16 có predictive back mặc định; Android xác nhận `onBackPressed()` không còn được gọi trong trường hợp này. 1.10.33 đặt:

```
android:enableOnBackInvokedCallback="false"
```

riêng cho SmartTube phone `PlaybackActivity`. Đây là cách để đường `onBackPressed()` hiện tại của SmartTube/NM7 thực sự được gọi, thay vì thêm một callback Android 16 thứ hai gây double-dispatch.

Khi `onBackPressed()` chạy:
- giữ `PlaybackActivity` + `mPlayer`;
- đặt `sMiniPlayerActive=true`;
- đưa Browse lên trước;
- Browse gắn TextureView mini-player vào player đang sống.

## Version

- Mobile: **1.10.33**
- versionCode: **51**
- Build script: **PATCH18**

Các commit:
- `b76897e8b136ee657e04ef5a1d4be1470616e47e` — Fix IPTV handoff destruction guard
- `f7854229f1f053228ad32e309c916fe44030a18c` — Fix YouTube HOME resume and mini-player lifecycle
- `7a888e3ac6f5961cb0709179e1ee8cc7960fb06b` — Use legacy SmartTube Back dispatch on Android 16
- `bc8a62659d84a301921fccfc02adc8757dc5a35a` — Bump Mobile to 1.10.33

## Trạng thái

**1.10.33 đã sửa source nhưng CHƯA được runtime-verified.** Không đánh dấu PASS trước khi build local Windows và test máy thật.

Test bắt buộc:
1. IPTV phát → YouTube Browse → chờ ít nhất 20 giây → IPTV vẫn có hình + tiếng.
2. Chọn video YouTube → IPTV chỉ dừng khi PlaybackActivity bắt đầu.
3. YouTube phát → HOME → chờ 20 giây → audio/video background vẫn còn.
4. Quay lại app → không spinner kéo dài; đúng video/session/vị trí tiếp tục.
5. YouTube phát → BACK 1 lần → Browse + mini-player nội bộ có hình.
6. Bấm mini-player → full player đúng video/vị trí.
7. Từ Browse bấm IPTV → IPTV hoạt động.
8. IPTV ↔ YouTube lặp lại nhiều vòng.
9. Không thêm Android TV vào Mobile.

---

# HOTFIX 1.10.32 — 2026-09-19 — THAY ĐỔI KIẾN TRÚC CHO 3 LỖI RUNTIME

## Trạng thái

1.10.31 đã build thành công nhưng người dùng xác nhận cả các lỗi chính vẫn còn. 1.10.32 là vòng sửa mới, **chưa runtime-verified**.

### Thay đổi chính

**A. IPTV → YouTube Browse**
- Không còn xóa `tabSwitchPending` chỉ vì Browse được resume.
- Chỉ xóa guard khi PlayerActivity IPTV thực sự resume hoặc khi SmartTube PlaybackActivity thực sự start.
- PlayerActivity giữ IPTV ExoPlayer trong cả `onStop()` và `onDestroy()` nếu YouTube handoff vẫn pending/tab YouTube.
- Mục tiêu: không release IPTV chỉ vì Browse xuất hiện.

**B. YouTube BACK → mini-player**
- Bỏ đường `getViewManager().startParentView()` khỏi mini-player.
- Bỏ `blockEngine(true)` và background sound mode trong BACK mini-player.
- BACK giờ mở trực tiếp phone `BrowseActivity` bằng `REORDER_TO_FRONT`.
- `sMiniPlayerActive=true` bảo vệ SmartTube `onStop()` khỏi `maybeReleasePlayer()`.
- Browse `onResume()` cài mini-player và attach TextureView vào cùng mPlayer.
- Lý do: mini-player cần video engine vẫn ở trạng thái video bình thường; block engine/background mode trước khi attach có thể làm mất hình.
- Đây là thay đổi có chủ đích so với các vòng 1.10.28–1.10.31.

**C. YouTube HOME/background**
- Start `BackgroundPlaybackService` ngay trong `onUserLeaveHint()`, tức khi Activity vẫn còn foreground/visible, thay vì chỉ trông chờ `onPause()`.
- Giữ `FOREGROUND_SERVICE_MEDIA_PLAYBACK` và service type `mediaPlayback`.
- Service không còn tự `stopSelf()` trong `onTaskRemoved()`; playback phải được dừng bởi lifecycle playback thực sự.
- Android yêu cầu media playback foreground service cho việc tiếp tục phát media ở background; đây là lý do 1.10.32 chuyển điểm start service lên trước khi app rời foreground.

## Version

- Mobile: **1.10.32**
- versionCode: **50**
- Build script PATCH17.
- Commit build-script fix: `e527157177f56ddc5ed25d8f6030eabd8b1205d6`
- Commit service: `0f71d983996de143e1d1a32272d768746b39b05a`
- Commit version: `714c6678175209f9c1277ba1d37ae35d3859a645`

## Chưa xác nhận

**Không đánh dấu 1.10.32 PASS trước khi build local và test máy thật.**

Checklist bắt buộc:
1. IPTV phát → YouTube Browse, không chọn video → IPTV vẫn phát.
2. Chọn video YouTube → IPTV mới release.
3. YouTube video → BACK 1 lần → Browse + mini-player.
4. Mini-player vẫn có hình + tiếng.
5. Bấm mini-player → full player.
6. YouTube video → HOME → audio tiếp tục.
7. Mở lại app → YouTube/video/session còn.
8. Không thêm Android TV vào Mobile.

---
# BÀN GIAO KHẨN — 2026-09-19 — MOBILE 1.10.31 VẪN FAIL TOÀN BỘ CÁC LỖI RUNTIME CHÍNH

## XÁC NHẬN MỚI NHẤT TỪ MÁY THẬT

**Người dùng đã trực tiếp cài và test APK 1.10.31. Kết quả: các lỗi cần sửa vẫn còn nguyên.**

Không được coi các commit/hotfix 1.10.30 hoặc 1.10.31 là đã giải quyết được runtime. Build thành công chỉ chứng minh source compile/package được; **runtime hiện tại vẫn FAIL**.

### APK 1.10.31 đã build trên Windows

Build local bằng PowerShell thành công:

- `NM7-IPTV-Mobile-1.10.31-arm64-v8a.apk` — 61,915,351 bytes.
- `NM7-IPTV-Mobile-1.10.31-armeabi-v7a.apk` — 51,421,090 bytes.
- Không tạo universal/x86.
- Các cảnh báo `META-INF/* not protected by signature` là warning, không phải nguyên nhân build failure.

Môi trường Windows đã xác nhận:
- JDK 17
- Gradle 8.13
- Android SDK 36
- Build Tools 36.0.0

## 1. LỖI CHƯA XỬ LÝ: YOUTUBE BACK 1 LẦN KHÔNG THU NHỎ VIDEO

### Hành vi bắt buộc

Khi:
1. Mở YouTube.
2. Chọn một video.
3. Video đang phát.
4. Nhấn **BACK đúng 1 lần**.

Kết quả mong muốn:
- Không thoát ứng dụng.
- Không về launcher.
- Không đóng hẳn PlaybackActivity.
- YouTube trở về Browse/Home.
- Video tiếp tục phát trong **mini-player nội bộ của NM7**.
- Nhấn mini-player → quay lại màn hình video lớn và tiếp tục đúng vị trí.

### Kết quả thực tế 1.10.31

**FAIL — người dùng xác nhận lỗi vẫn còn nguyên.**

Do đó toàn bộ cơ chế mini-player hiện tại phải được xem là **chưa được chứng minh đúng**, dù source đã có các patch `sActiveInstance`, `sMiniPlayerActive`, `TextureView`, `attachMiniPlayer()`, `restoreFromMiniPlayer()`.

### Các điểm cần người tiếp quản kiểm tra

1. Android/SmartTube đang dispatch BACK qua API nào trên điện thoại thực tế:
   - legacy `onBackPressed()`;
   - `OnBackInvokedDispatcher`;
   - predictive back;
   - hoặc ViewManager/Presenter intercept trước Activity.
2. `getViewManager().startParentView(this)` có thực sự chuyển sang Browse hay không.
3. `BrowseActivity.onResume()` có được gọi sau BACK không.
4. `PlaybackActivity.sActiveInstance` còn trỏ đúng Activity sau transition không.
5. `mPlayer` còn tồn tại sau BACK không.
6. `mPlayer.setVideoTextureView(miniView)` có thực sự chuyển video output sang TextureView không.
7. Việc `mPlayerView.setPlayer(null)` trước khi chuyển TextureView có làm mất surface/player binding không.
8. Có lifecycle nào sau đó gọi `maybeReleasePlayer()`, `releasePlayer()`, `blockEngine(false)` hoặc reset background mode không.
9. Cần lấy logcat đúng thời điểm BACK, không sửa tiếp theo phỏng đoán.

## 2. LỖI CHƯA XỬ LÝ: IPTV BỊ TẮT KHI CHUYỂN SANG YOUTUBE BROWSE

### Hành vi bắt buộc

Khi:
1. IPTV đang phát.
2. Nhấn tab **YouTube**.
3. YouTube Browse mở ra.
4. **Chưa chọn video YouTube**.

Kết quả bắt buộc:
- IPTV vẫn phải tiếp tục phát.
- Không được pause.
- Không được release ExoPlayer IPTV.
- Không được dừng background service IPTV.
- Có thể duyệt YouTube mà không ảnh hưởng IPTV.

Chỉ khi:
5. Người dùng **chọn một video YouTube**.
6. SmartTube PlaybackActivity thực sự bắt đầu phát video.

thì:
- IPTV mới pause/release.
- YouTube mới lấy quyền decoder.

### Kết quả thực tế 1.10.31

**FAIL — người dùng xác nhận lỗi vẫn còn nguyên.**

Điều này đặc biệt quan trọng: cơ chế `nm7.iptv.youtube.handoff` được thêm ở 1.10.31 **không giải quyết được hành vi trên máy thật**.

### Các điểm cần người tiếp quản kiểm tra

1. `HomeTabBar.openBrowse()` có vô tình kích hoạt lifecycle làm `PlayerActivity.onStop()` release player hay không.
2. `SharedPlaybackSession.setTab(TAB_YOUTUBE)` có làm PlayerActivity tự coi mình là inactive hay không.
3. `MobileNm7Application.onActivityStarted(PlayerActivity)` và `onActivityStarted(SMARTTUBE_BROWSE)` có gọi pause/release gián tiếp hay không.
4. `MobileNm7Application.pauseExternalMedia()` có ảnh hưởng đến IPTV player/session không.
5. `PlayerActivity.onPause()`, `onStop()`, `onDestroy()` và mọi đường `releasePlayer()`.
6. `keepPlayerForTabSwitch`, `isYoutubeHandoffPending()`, `MobileNm7Application.isTabSwitchPending()` có giá trị đúng tại từng callback hay không.
7. Android Activity/task stack thực tế sau khi `REORDER_TO_FRONT` chạy.
8. Quan trọng nhất: **không chỉ kiểm tra cờ; phải log thứ tự lifecycle thực tế và nơi gọi releasePlayer().**

## 3. LỖI CHƯA XỬ LÝ: YOUTUBE HOME / BACKGROUND KHÔNG PHÁT NỀN

### Hành vi bắt buộc

Khi:
1. YouTube đang phát video.
2. Nhấn nút Home của điện thoại hoặc rời app.
3. Chờ 10–20 giây.

Kết quả:
- Audio YouTube vẫn phát nền.
- Player không bị release.
- Khi mở lại NM7:
  - vẫn ở YouTube;
  - video hiện tại vẫn còn;
  - tiếp tục phát;
  - không spinner/loading vô hạn;
  - không quay về IPTV ngoài ý muốn.

### Kết quả thực tế

**FAIL — đã được xác nhận từ test máy thật; lỗi background/home vẫn còn.**

Video test trước đó:
- `video_2026-09-19_08-04-08.mp4`
- khoảng 80 giây
- 576x1280
- H.264 + AAC

Quan sát video trước đó cho thấy sau khi rời app và quay lại, YouTube có trạng thái spinner/loading thay vì phục hồi sạch trạng thái playback. Sau đó 1.10.31 vẫn không giải quyết được lỗi runtime theo xác nhận mới nhất.

### Các điểm cần kiểm tra

1. `PlaybackActivity.onUserLeaveHint()`.
2. `PlaybackActivity.onPause()`.
3. `PlaybackActivity.onStop()`.
4. `maybeReleasePlayer()`.
5. `blockEngine(true/false)`.
6. `getPlayerData().setBackgroundMode(...)`.
7. `nm7.youtube.background`.
8. SmartTube có tự reset background mode sau `onResume()) hay không.
9. Android có kill/stop Activity/player do task/lifecycle hay không.
10. Background service của SmartTube/NM7 có thực sự được start và giữ process/player không.
11. Khi quay lại app, `MainActivity.onResume()`/Browse restoration có vô tình tạo Activity mới hoặc reset player state không.

## 4. LỖI LIÊN QUAN: QUAY LẠI APP KHÔNG GIỮ ĐÚNG YOUTUBE SESSION

Yêu cầu:
- Nếu người dùng đang ở YouTube và video đang phát rồi rời app, quay lại phải vẫn là YouTube.
- Không tự chuyển sang IPTV.
- Không tạo lại video từ đầu nếu player còn sống.
- Không spinner/reload nếu session còn tồn tại.

**Hiện tại chưa PASS.**

Các thành phần liên quan:
- `SharedPlaybackSession`
- `MainActivity.onResume()`
- `MobileNm7Application.bringSmartTubeToFront()`
- `bringSmartTubeBrowseToFront()`
- SmartTube BrowseActivity/PlaybackActivity lifecycle.

## 5. LỖI/NGUY CƠ KIẾN TRÚC CẦN ĐẶC BIỆT LƯU Ý

### A. Không coi `BUILD SUCCESSFUL` là runtime PASS

1.10.31 compile/package thành công nhưng **3 luồng chính vẫn FAIL trên máy thật**:
- IPTV → YouTube Browse làm IPTV dừng.
- YouTube → BACK không tạo mini-player đúng.
- YouTube → HOME/background không giữ playback đúng.

### B. Không tiếp tục sửa hàng loạt lifecycle

Các vòng trước đã thay đổi nhiều điểm:
- PlayerActivity lifecycle.
- SmartTube PlaybackActivity lifecycle.
- BrowseActivity lifecycle.
- Activity reorder.
- background marker.
- mini-player.
- cross-player ownership.

Người tiếp quản cần **debug bằng logcat trước**, sau đó sửa đúng một lifecycle transition tại một thời điểm.

### C. Không đưa Android TV vào bản Mobile

Phạm vi bàn giao vẫn là:
**CHỈ NM7 IPTV Mobile.**

Không lấy logic UI/player của Android TV để thay thế Mobile nếu chưa chứng minh tương thích.

### D. Không quay lại kiến trúc “một ExoPlayer literal” một cách mù quáng

Mục tiêu sản phẩm vẫn là trải nghiệm player thống nhất, nhưng thử nghiệm literal one-ExoPlayer trước đây từng gây regression và đã được rollback. Người tiếp quản nên ưu tiên ổn định ownership/lifecycle trước khi thay đổi kiến trúc player.

## 6. SOURCE/BUILD HIỆN TẠI

Branch:
`fix/mobile-1.10.26-sleep-timer-icon`

Version:
- 1.10.31
- versionCode 49

Các commit mới nhất:
- `310f59ee64243d3558676755009a81f7ae97594c` — deterministic IPTV→YouTube handoff guard.
- `366e17fe1c3c7095e432f62b22985678493eca43` — tab handoff ordering.
- `dc959cd1d591f159779ceaf53cfe331113f2317d` — clear IPTV handoff when YouTube playback starts.
- `440e76f2cb67034eaa04c81092764537f82b5889` — mini-player lifecycle patch.
- `b904efbd91f820fb9420c8b1c0ea910cbfe5da71` — final BACK parent-view path / PATCH16.
- `02ed4332657f2e7a435e50bc90783b6967c62905` — version 1.10.31.
- `5c899170fa9e4e93a4fbc39b0f74a75b390e6206` — this handoff documentation.

## 7. FILE QUAN TRỌNG

### NM7
- `app/src/main/java/vn/phuong/iptvplayer/PlayerActivity.java`
  - IPTV player lifecycle.
  - YouTube handoff.
  - `keepPlayerForTabSwitch`.
  - `nm7.iptv.youtube.handoff`.

- `app/src/main/java/vn/phuong/iptvplayer/HomeTabBar.java`
  - IPTV/YouTube tab switching.
  - `REORDER_TO_FRONT`.

- `app/src/main/java/vn/phuong/iptvplayer/MobileNm7Application.java`
  - cross-player ownership.
  - SmartTube Activity callbacks.
  - IPTV pause/release.
  - task restoration.

- `app/src/main/java/vn/phuong/iptvplayer/SharedPlaybackSession.java`
  - persisted tab state/background state.

- `app/src/main/java/vn/phuong/iptvplayer/MainActivity.java`
  - restore YouTube/Browse after app resume.

### SmartTube generated during build
SmartTube phone source is cloned and patched by:
- `scripts/build-mobile-windows.ps1`
- `.github/workflows/android-mobile-final.yml`

Base:
- `systematiq-one/SmartTube-droid`
- merged with upstream SmartTube tag `32.47s`.

SmartTube files that must be inspected after every build-script patch:
- `smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/playback/PlaybackActivity.java`
- `smarttubedroid/src/main/java/com/liskovsoft/smartyoutubetv2/droid/ui/browse/BrowseActivity.java`

## 8. CÁC PATCH ĐÃ THỬ NHƯNG CHƯA ĐƯỢC CHỨNG MINH

### IPTV handoff
- `prepareForYoutubeHandoff()` không release IPTV khi mở Browse.
- `keepPlayerForTabSwitch`.
- `nm7.tab.switch.until`.
- `nm7.iptv.youtube.handoff`.
- release IPTV khi SmartTube PlaybackActivity thực sự start.

**Kết luận:** chưa đủ; runtime vẫn FAIL.

### Mini-player
- `sActiveInstance`.
- `sMiniPlayerActive`.
- Browse `onResume()` install mini-player.
- TextureView output.
- `attachMiniPlayer()`.
- `restoreFromMiniPlayer()`.
- `closeMiniPlayer()`.
- BACK patch.
- thử cả đường direct Browse và SmartTube `startParentView()`.

**Kết luận:** chưa đủ; runtime vẫn FAIL.

### Background
- `onUserLeaveHint()`.
- `onPause()` fallback.
- `BACKGROUND_MODE_SOUND`.
- `BACKGROUND_MODE_PLAY_BEHIND`.
- `nm7.youtube.background`.
- bỏ reset background trong `onResume()`.
- tránh release trong `onStop()`.

**Kết luận:** chưa đủ; runtime vẫn FAIL.

## 9. TEST PLAN CHUẨN CHO NGƯỜI TIẾP QUẢN

### Test A — IPTV → Browse
- Start IPTV.
- Bấm YouTube.
- Không chọn video.
- Chờ 20 giây.
- IPTV phải vẫn có hình + tiếng.
- Chụp/log lifecycle.

### Test B — Browse → YouTube Playback
- Từ trạng thái A chọn video.
- Xác nhận thời điểm PlaybackActivity start.
- IPTV chỉ được release tại hoặc sau thời điểm này.
- Kiểm tra decoder/audio focus.

### Test C — YouTube BACK
- Phát video.
- BACK một lần.
- Phải hiện Browse + mini-player.
- Video không được restart.
- Bấm mini-player → full player.

### Test D — YouTube HOME
- Phát video.
- HOME.
- Chờ 20 giây.
- Kiểm tra audio.
- Mở app.
- Video/session phải còn.

### Test E — lặp
- IPTV → YouTube Browse → IPTV → YouTube Browse → video → BACK → mini-player → full player → HOME → resume.
- Lặp ít nhất 5 vòng.

## 10. LOGCAT BẮT BUỘC

Không nên sửa tiếp chỉ bằng suy luận UI. Cần capture:

```
adb logcat -c
adb logcat | findstr /i "PlaybackActivity BrowseActivity PlayerActivity MobileNm7Application SmartTube ExoPlayer releasePlayer maybeReleasePlayer blockEngine background"
```

Nếu dùng PowerShell:
```
adb logcat -c
adb logcat | Select-String "PlaybackActivity|BrowseActivity|PlayerActivity|MobileNm7Application|SmartTube|ExoPlayer|releasePlayer|maybeReleasePlayer|blockEngine|background"
```

Cần đánh dấu timestamp cho từng thao tác:
- T0: IPTV đang phát.
- T1: bấm YouTube.
- T2: Browse xuất hiện.
- T3: chọn video.
- T4: PlaybackActivity start.
- T5: BACK.
- T6: HOME.
- T7: mở lại app.

## 11. ĐIỂM DỪNG ĐỂ BÀN GIAO

**Không đánh dấu bất kỳ lỗi nào là FIXED.**

Mục tiêu của người tiếp quản:
1. Xác định chính xác Activity/task/lifecycle transition gây release IPTV khi chỉ mở Browse.
2. Xác định chính xác BACK đang bị xử lý ở đâu và tại sao mini-player không xuất hiện.
3. Xác định chính xác tại sao HOME/background vẫn làm mất playback hoặc khi resume lại spinner.
4. Sau khi có logcat, sửa từng lỗi độc lập.
5. Build lại Mobile ARM64 + ARMv7.
6. Test máy thật toàn bộ Test A–E.
7. Chỉ khi runtime PASS mới tạo mốc release mới.

## 12. CẢNH BÁO VỀ CI

Sau các commit 1.10.31, một số GitHub Actions workflow trên branch đã báo `failure`, trong khi **build local Windows vẫn SUCCESSFUL**. Root cause CI của các workflow đó chưa được điều tra đầy đủ trong mốc này và không được giả định là cùng nguyên nhân với lỗi runtime. Người tiếp quản nên kiểm tra Actions trước khi dùng CI làm nguồn APK chính.

---
# HOTFIX 1.10.31 — 2026-09-19 — XỬ LÝ DỨT ĐIỂM 2 LỖI CÒN LẠI

## 1. IPTV → YouTube: không được dừng IPTV khi chỉ chuyển tab

Đã bổ sung một cờ handoff cấp process: `nm7.iptv.youtube.handoff`.

Luồng mới:
1. Đang phát IPTV.
2. Bấm tab YouTube → đánh dấu handoff đang chờ.
3. `PlayerActivity.onPause()` và `onStop()` đều giữ ExoPlayer nếu handoff còn chờ.
4. Browse YouTube chỉ là navigation, không gọi release IPTV.
5. Chỉ khi SmartTube `PlaybackActivity` thực sự bắt đầu thì `pauseIptvPlayer()` mới pause + release IPTV và xóa cờ handoff.
6. Khi quay lại IPTV, cờ handoff được xóa trước khi đưa PlayerActivity lên foreground.

Mục tiêu chính: **IPTV tiếp tục phát trong thời gian người dùng chỉ đang duyệt YouTube; YouTube PlaybackActivity mới có quyền lấy decoder khi video thực sự mở.**

## 2. YouTube BACK 1 lần → Browse + mini-player

Đã thay đổi patch Mobile SmartTube theo hướng không gọi `finish()` trong BACK.

BACK lần đầu hiện:
- giữ `PlaybackActivity` và ExoPlayer sống;
- bật trạng thái mini-player;
- đặt background mode của SmartTube;
- gọi trực tiếp `getViewManager().blockTop(this)`;
- gọi `getViewManager().startParentView(this)`;
- BrowseActivity cài mini-player trong `onResume()`, kể cả khi Browse đã tồn tại;
- chuyển output video sang `TextureView` của mini-player;
- ép `playWhenReady=true` khi attach;
- khi mở lại mini-player, trả output về PlayerView trước khi đưa PlaybackActivity lên trước.

Cách này tránh đường `finish() → enterPipMode()`, vốn có thể đưa video vào Android system PiP thay vì mini-player nội bộ của NM7.

## 3. Version / commit

- Mobile version: **1.10.31**
- versionCode: **49**
- Build script: **PATCH16**
- Commit handoff guard: `310f59ee64243d3558676755009a81f7ae97594c`
- Commit tab ordering: `366e17fe1c3c7095e432f62b22985678493eca43`
- Commit playback-start handoff cleanup: `dc959cd1d591f159779ceaf53cfe331113f2317d`
- Commit mini-player lifecycle: `440e76f2cb67034eaa04c81092764537f82b5889`
- Commit final BACK parent-view path + PATCH16: `b904efbd91f820fb9420c8b1c0ea910cbfe5da71`
- Version bump: `02ed4332657f2e7a435e50bc90783b6967c62905`

## 4. Trạng thái

**Chưa build và chưa xác nhận máy thật 1.10.31.** Không đánh dấu PASS trước khi APK được build và test.

Checklist bắt buộc:
1. IPTV đang phát → YouTube tab, **không chọn video** → IPTV vẫn phát.
2. Ở Browse YouTube 10–20 giây → IPTV vẫn không bị release.
3. Chọn video YouTube → IPTV mới dừng.
4. YouTube đang phát → BACK 1 lần → về Browse và xuất hiện mini-player.
5. Video trong mini-player vẫn chạy.
6. Bấm mini-player → trở lại full player, video tiếp tục.
7. Lặp IPTV → YouTube → video → BACK → IPTV ít nhất vài lần.
8. Không đưa TV UI/code vào bản Mobile.

---
# HOTFIX 1.10.30 — 2026-09-19 — XỬ LÝ LỖI THỰC TẾ TỪ VIDEO 1.10.29

## Phát hiện từ video máy thật mới nhất

Video người dùng gửi: video_2026-09-19_08-04-08.mp4, dài khoảng 80 giây.

Quan sát chuỗi hình trong video:
- YouTube Browse mở được và video YouTube bắt đầu phát.
- Khi rời YouTube về Home/ra khỏi ứng dụng, trạng thái phát không được giữ ổn định.
- Khi quay lại, player YouTube có trạng thái spinner/loading thay vì tiếp tục ngay trạng thái video đang phát.
- Không có mini-player đúng yêu cầu khi quay lại Browse.
- Vì vậy 1.10.29 KHÔNG được coi là đạt yêu cầu runtime.

## Nguyên nhân mã đã xác định trong 1.10.29

### 1. Mini-player chỉ được cài trong BrowseActivity.onCreate()

1.10.29 gọi installNm7MiniPlayer() trong onCreate(). Nhưng HomeTabBar/PlaybackActivity đưa BrowseActivity lên foreground bằng REORDER_TO_FRONT. Nếu BrowseActivity đã tồn tại, onCreate() không chạy lại, nên mini-player không được tạo khi BACK.

**Sửa 1.10.30:**
- Chuyển hook mini-player sang BrowseActivity.onResume().
- Giữ onCreate() chỉ khởi tạo Browse.
- Mỗi lần Browse được đưa lên foreground, onResume() kiểm tra PlaybackActivity.isMiniPlayerActive() và attach TextureView nếu cần.

### 2. HOME/background chỉ dựa vào onUserLeaveHint()

1.10.29 phụ thuộc quá nhiều vào onUserLeaveHint() để đặt:
- BACKGROUND_MODE_SOUND
- nm7.youtube.background=1
- SharedPlaybackSession TAB_YOUTUBE/background
- blockEngine(true)

Trên thiết bị thực tế, đường lifecycle này không đủ tin cậy để bảo đảm player không bị release trước khi onStop().

**Sửa 1.10.30:**
- Thêm fallback chính xác vào PlaybackActivity.onPause().
- Chỉ coi đây là HOME/background khi:
  - không phải BACK: !mIsBackPressed
  - Activity chưa finishing
  - ViewManager không có view transition pending
  - mPlayer != null và đang playWhenReady.
- Khi thỏa điều kiện, lưu tab YouTube, đặt background sound, đặt nm7.youtube.background=1 và block engine trước khi Activity tiếp tục pause.
- onStop() tiếp tục không gọi maybeReleasePlayer() khi nm7.youtube.background=1.

### 3. onResume() của 1.10.29 tự xóa trạng thái background

Patch 1.10.29 từng thêm:
- nm7.youtube.background=0
- BACKGROUND_MODE_DEFAULT
ngay trong onResume().

Điều này có thể xóa trạng thái background trong lúc Android đang khôi phục task/player.

**Sửa 1.10.30:**
- Bỏ việc ghi đè trạng thái background trong onResume().
- onResume() chỉ khôi phục player nếu thực sự mPlayer == null và mở lại UI engine; không xóa marker HOME.

## Các thay đổi 1.10.30

- versionCode: 48.
- versionName: 1.10.30.
- Build script commit hotfix: e8d6e5abea188277cd63c06cfab5a880c370d40c.
- Version bump commit: 9f820dec0e84316d73e82e836f76101b89560888.
- Đây là hotfix tập trung vào 2 lỗi mới nhất: HOME/background và mini-player resume. Không thay đổi IPTV player engine/network.

## Cảnh báo

- Chưa build 1.10.30 sau hotfix.
- Chưa xác nhận runtime.
- Không được bàn giao APK 1.10.30 cho người dùng trước khi build SUCCESSFUL.
- Sau build phải test lại đúng video flow, đặc biệt:
  1. mở YouTube → phát video;
  2. bấm HOME → chờ 10–20 giây → kiểm tra audio/video còn chạy;
  3. quay lại app → video tiếp tục, không spinner/reload;
  4. YouTube video → BACK 1 lần → Browse + mini-player;
  5. bấm mini-player → full player;
  6. IPTV → YouTube Browse không chọn video → IPTV vẫn phát;
  7. chọn video → IPTV mới release.

---

# BÀN GIAO TIẾN ĐỘ — NM7 IPTV MOBILE 1.10.29 — 2026-09-19

## Trạng thái hiện tại — BUILD THÀNH CÔNG, CHỜ TEST MÁY THẬT

- Repo: phuongnm7/iptv-player-android.
- Branch: fix/mobile-1.10.26-sleep-timer-icon.
- Mobile version: versionCode 47, versionName 1.10.29.
- Phạm vi: CHỈ Android Mobile. Không đưa Android TV vào bản Mobile.
- SmartTube base: build script clone/merge SmartTube upstream tag 32.47s rồi áp dụng patch Mobile.
- Môi trường Windows đã xác nhận: JDK 17, Gradle 8.13, Android SDK 36, Build Tools 36.0.0.
- Build ngày 2026-09-19: SUCCESS. Unit test Mobile và assemble Mobile hoàn tất; tạo đúng 2 APK ARM, không universal.
- APK ARM64: NM7-IPTV-Mobile-1.10.29-arm64-v8a.apk — 61,915,327 bytes.
- APK ARMv7: NM7-IPTV-Mobile-1.10.29-armeabi-v7a.apk — 51,421,069 bytes.
- Các warning META-INF/* not protected by signature trong log là warning ký JAR, không phải build failure.
- Chưa có xác nhận runtime cho 1.10.29. Người dùng đang cài bản này để test. Không đánh dấu lỗi YouTube/IPTV là đã hết chỉ vì build thành công.

## Mục tiêu runtime của 1.10.29

### 1. IPTV → YouTube

Yêu cầu:
1. IPTV đang phát.
2. Chuyển sang tab YouTube nhưng chưa chọn video: IPTV phải tiếp tục phát.
3. Có thể chuyển IPTV ↔ YouTube nhiều lần: IPTV không được tự dừng/release chỉ vì mở Browse.
4. Chỉ khi SmartTube PlaybackActivity thực sự bắt đầu video thì IPTV mới pause/release.

Các thay đổi:
- PlayerActivity.prepareForYoutubeHandoff() không còn release IPTV khi chỉ mở YouTube Browse.
- HomeTabBar.openBrowse() dùng REORDER_TO_FRONT và không finish PlayerActivity.
- PlayerActivity.onStop() giữ player khi Activity chưa finishing và tab hiện tại là YouTube hoặc tab switch đang pending.
- MobileNm7Application có pauseIptvForYoutube() để SmartTube yêu cầu release IPTV trước khi tạo decoder.
- SmartTube PlaybackActivity.onStart() gọi helper này trước initializePlayer(), tránh race giữa decoder YouTube và IPTV.
- Application lifecycle vẫn có lớp bảo vệ thứ hai khi SmartTube PlaybackActivity thực sự bắt đầu.

### 2. YouTube → BACK lần đầu → mini-player

Yêu cầu:
- YouTube đang phát → BACK 1 lần → hiện YouTube Browse/Home và video tiếp tục trong mini-player.
- Không được hiện launcher, màn hình trắng hoặc splash.
- BACK đầu tiên không được finish PlaybackActivity.
- Bấm mini-player phải quay lại player lớn.
- Đóng mini-player phải dừng/finish player.

Kiến trúc 1.10.29:
- Không dùng startParentView() làm cơ chế mini-player chính.
- PlaybackActivity.onBackPressed() được patch để đặt sMiniPlayerActive, giữ player và đưa BrowseActivity lên foreground bằng FLAG_ACTIVITY_REORDER_TO_FRONT | FLAG_ACTIVITY_NO_ANIMATION.
- Thêm sActiveInstance và sMiniPlayerActive.
- BrowseActivity tạo FrameLayout mini-player và TextureView.
- attachMiniPlayer() chuyển output video sang TextureView.
- restoreFromMiniPlayer() khôi phục player view lớn.
- closeMiniPlayer() tắt mini-player và finish PlaybackActivity.
- SmartTube onStop() không release player khi mini-player đang active.
- Đã xóa BACK patch thứ hai dùng startParentView() vì nó từng ghi đè patch mini-player.

### 3. HOME / khóa màn hình YouTube

Các patch background playback từ các vòng trước được giữ:
- onUserLeaveHint() chọn BACKGROUND_MODE_SOUND, đặt nm7.youtube.background=1, lưu tab YouTube và chặn engine release.
- Screen-off path chọn sound background và giữ engine khi có thể.
- SharedPlaybackSession/MainActivity hỗ trợ lưu tab YouTube và khôi phục Browse/Playback khi resume.
- Vẫn phải xác nhận lại bằng test máy thật.

## Các lỗi build/patch đã gặp và đã xử lý trong vòng 1.10.29

1. Biến PowerShell $play undefined → khai báo PlaybackActivity path đúng trước khi đọc source.
2. SmartTube onStart thực tế dùng VERSION.SDK_INT > 23 rồi initializePlayer() → patch được điều chỉnh.
3. SmartTube onStop thực tế dùng VERSION.SDK_INT > 23 rồi maybeReleasePlayer() → patch được điều chỉnh.
4. SmartTube phone onBackPressed thực tế có onDetailsBack() rồi super.onBackPressed() → patch đúng source shape.
5. Script còn BACK patch thứ hai dùng startParentView() và ghi đè patch mới → đã xóa.
6. Compile thiếu android.content.Intent → đã ghi import vào PlaybackActivity.java trước Gradle.
7. SimpleExoPlayer.setVideoTextureView() đã được kiểm tra có trong API ExoPlayer phù hợp.
8. Build cuối cùng đã vượt qua compile/package và tạo đủ 2 APK ARM.

## Các commit/điểm mã quan trọng

- 02c7f32f9d4d58f1fe01aaf94623576a95f38b45 — Keep IPTV playing until YouTube video starts.
- 2cc0d9901b3848569364f1a4d8bcee85e2e10102 — Keep IPTV alive while browsing YouTube.
- b1ceb839c5f3e2274a011d0e65639c85e5a68fde — Make IPTV YouTube handoff lifecycle explicit.
- 0414fcb802537a74789bc3f6cc6c37351e09d488 — Keep IPTV player alive while YouTube tab is browsing.
- 95cc48dc0830ae0436d23a0f658110902a1f1589 — Use native SmartTube Back dispatch for mini-player; không re-add OnBackInvokedCallback cũ.
- 58bd32d3d695a2e2af764d7e83ac7a6a3387c3a0 — Bump Mobile version for tab handoff and mini-player fix.
- 284e8697f9857b5cf879b201f2ba94e17cfa715a — Record 1.10.28 playback fixes.
- 74324d8 — Implement true YouTube mini-player on Mobile Back.
- eff1ccd — Allow mini-player lifecycle patch in Mobile build script.
- 5917488 — Fix Browse mini-player patch write target.
- 729d14f — Remove stale parent-view BACK validation.
- 4c6a58e — Match SmartTube PlaybackActivity onStart source shape.
- ba299166 — Match actual SmartTube PlaybackActivity lifecycle for mini-player.
- 095f22a — Match actual SmartTube phone onBackPressed source shape.
- 02aa40d1d59213f34f109ae4fc6fc01f9a8599af — Remove duplicate BACK patch that overwrote mini-player.
- 4bf09ac6ea4bc005d136f3e1ccfb6d14e69288c2 — Persist Intent import for YouTube mini-player compile.
- b40feb4 — Fix missing PlaybackActivity path in build script.
- 1.10.29: versionCode 47, versionName 1.10.29.

## Các file/area quan trọng để bàn giao

- scripts/build-mobile-windows.ps1: clone/merge SmartTube, patch lifecycle/mini-player, patch ExoPlayer resource conflict, build/test/package/ABI validation.
- app/build.gradle.kts: Mobile-only flavor, version 1.10.29, ARM split chỉ armeabi-v7a + arm64-v8a.
- app/src/main/java/vn/phuong/iptvplayer/PlayerActivity.java: IPTV lifecycle, position persistence, YouTube handoff.
- app/src/main/java/vn/phuong/iptvplayer/MobileNm7Application.java: cross-player lifecycle, pause/release IPTV khi YouTube PlaybackActivity thực sự khởi động, task/tab restoration.
- app/src/main/java/vn/phuong/iptvplayer/SharedPlaybackSession.java: lưu tab IPTV/YouTube và trạng thái playback.
- app/src/main/java/vn/phuong/iptvplayer/HomeTabBar.java: chuyển IPTV ↔ YouTube bằng Activity reorder.
- app/src/main/java/vn/phuong/iptvplayer/MainActivity.java: restore YouTube tab/SmartTube Activity khi resume.
- SmartTube generated/patch-at-build files không nằm cố định trong repo; script clone/merge rồi patch lại mỗi lần build. Khi source SmartTube thay đổi phải kiểm tra source sau merge trước khi sửa patch.

## Checklist test 1.10.29 — CHƯA HOÀN TẤT

### IPTV/YouTube handoff
- [ ] IPTV phát → mở YouTube Browse → IPTV vẫn phát.
- [ ] YouTube Browse không chọn video → quay IPTV → IPTV vẫn phát.
- [ ] Lặp IPTV → YouTube → IPTV ít nhất 5 lần → không tự dừng/release IPTV.
- [ ] Chọn video YouTube → IPTV dừng/release chỉ khi YouTube PlaybackActivity thực sự bắt đầu.
- [ ] Mở video YouTube bình thường, không crash.

### YouTube mini-player
- [ ] YouTube đang phát → BACK 1 lần → Browse/Home + mini-player.
- [ ] Video vẫn chạy, âm thanh/hình ảnh không mất.
- [ ] Không xuất hiện launcher/blank/splash.
- [ ] Bấm mini-player → trở lại full player.
- [ ] BACK tiếp theo có hành vi đúng theo UX mong muốn.
- [ ] HOME/khóa màn hình → YouTube tiếp tục background.
- [ ] Mở app lại → vẫn ở YouTube, không nhảy về IPTV.

### Regression
- [ ] IPTV player vẫn phát bình thường.
- [ ] Search/group/channel UI không bị thay đổi ngoài yêu cầu.
- [ ] Logo NM7 Mobile giữ kích thước đã sửa.
- [ ] Không có TV flavor/TV manifest/component trong APK Mobile.
- [ ] Chỉ tạo đúng ARM64 + ARMv7, không universal/x86.

## Quy tắc xử lý nếu 1.10.29 FAIL runtime

1. Không sửa hàng loạt lifecycle cùng lúc.
2. Nếu IPTV dừng khi chỉ mở Browse: lấy logcat quanh thời điểm chuyển tab, tập trung PlayerActivity onPause/onStop/onDestroy, MobileNm7Application onActivityStarted/onActivityStopped và SmartTube PlaybackActivity onStart/onStop.
3. Nếu YouTube mở video rồi crash: lấy FATAL EXCEPTION và khoảng 100–200 dòng log trước/sau; kiểm tra mini-player field/method trước khi đổi kiến trúc.
4. Nếu BACK hiện launcher/blank: xác định Activity/task transition trước; không quay lại startParentView() chỉ để che hiện tượng.
5. Nếu audio còn nhưng mini-player không có hình: kiểm tra ownership video surface/TextureView và thời điểm setPlayer(null)/setVideoTextureView().
6. Nếu BACK vẫn finish Activity: kiểm tra Android legacy onBackPressed versus predictive/onBackInvoked và Activity stack; không tự động re-add callback cũ.
7. Nếu build script lại báo source shape changed: kiểm tra source SmartTube sau merge tag 32.47s trước khi sửa.
8. Chỉ tối ưu tốc độ load YouTube/playlist sau khi lifecycle/runtime ổn định.

## Bàn giao

- Hiện tại source 1.10.29 đã build thành công trên Windows; người dùng đang cài/test.
- Việc tiếp theo: ghi kết quả test máy thật vào mục này. Nếu FAIL, ưu tiên video + logcat và sửa đúng nguyên nhân. Nếu PASS, ghi kết quả và tạo mốc/tag bàn giao 1.10.29.
- Không coi BUILD SUCCESSFUL là RUNTIME SUCCESSFUL.

---

# CẬP NHẬT QUAN TRỌNG — KHÔI PHỤC LỊCH SỬ WINDOWS POWERSHELL VÀ KẾT QUẢ TEST THỰC TẾ — 2026-09-18

## Mục đích của mốc này

- Người dùng xác nhận bản Mobile 1.10.26 vừa cài vẫn còn **các lỗi cũ**: chuyển IPTV → YouTube vẫn lỗi, YouTube/Home lifecycle chưa đúng, quay lại tab YouTube chưa ổn định, YouTube mở video còn chậm và giao diện YouTube chưa hoàn toàn mobile.
- Không coi việc build thành công là đã sửa lỗi runtime.
- Phần này ghi lại lại đầy đủ chuỗi sửa lỗi đã được thực hiện trước đó bằng **PowerShell trên Windows**, để vòng xử lý tiếp theo không làm mất các thay đổi/giả thuyết đã thử.
- Phạm vi tiếp tục: **chỉ NM7 IPTV Mobile 1.10.26**. Không đưa mã Android TV vào bản Mobile.

## Điểm mã đang dùng để tiếp tục

- Nhánh: `fix/mobile-1.10.26-sleep-timer-icon`.
- Mốc SmartTube lifecycle đã kiểm tra gần nhất: `5bf1fbec30ea7a19512f1df743174746bb4754f8` — `validate YouTube background lifecycle patch`.
- Sau đó nhánh đã từng đi qua chuỗi thay đổi SharedPlaybackSession/lifecycle, nhưng các thay đổi đó đã bị loại khỏi nhánh chính để tránh tiếp tục trên nền đang gây hồi quy.
- Hiện tại **không được coi `5bf1fbec` là bản đã hết lỗi**; test máy thật mới nhất xác nhận lỗi vẫn tái hiện. Đây là điểm rất quan trọng để vòng sửa tiếp theo không nhầm “baseline” với “đã sửa xong”.

## Lịch sử các sửa lỗi Windows/PowerShell đã thực hiện

### 1. Nền SmartTube + Windows build
- `d403a20aa1988b7850d369b84ce784363b271cf9` — ổn định SmartTube Windows và các patch source/CI.
- `b70293744f157a062e4b6fa7a8189909d64fbea3` — ghi nhận tiến độ Mobile 1.10.26 và Windows build.
- `0a600a430b1298cef496fc4b2b2f42cf79a056e1` — harden script Windows cho Mobile UI/playback.
- `42e0a136d2d68f8d29d8feaa696ca3992d43fcc2` — bỏ global-layout recursion tốn chi phí trong SmartTube.
- `9d017590483fc5f1f2da38bbe1c7236e128235fc` — dọn import của performance patch SmartTube.
- Môi trường Windows đã xác nhận: **JDK 17, Gradle 8.13, Android SDK 36, Build Tools 36.0.0**.

### 2. YouTube UI/lifecycle và chuyển tab
- `b988bd81cda4` — sửa clock cho tab switch và timing prewarm.
- `7dfa72e6b0fa` — sửa SmartTube Play-Behind và lock handling.
- `3246bd97654a` — ép feed YouTube một cột sau khi RecyclerView attach.
- `3c0edbf5b380` — xử lý background playback khi khóa màn hình.
- `8d0280508db2` — sửa feed patch và force YouTube background playback.
- `629118aad795` — áp dụng background mode trước player resume.
- `43c504672b88` — gắn nhãn foreground service cho YouTube background playback.
- `e259a39a02c3` — dừng YouTube keepalive khi foreground media thay đổi.
- `089c2b61782e` — thêm helper giữ YouTube foreground.
- `7ac76d885fb7` — giữ SmartTube player trong background.
- `bebe7bc15303` — giữ YouTube playback sống khi chạy nền.
- `e76c1323b20f` — sửa cách CI tạo/apply YouTube background patch.
- `5ec48c6e5afb` — sửa lifecycle IPTV khi chỉ chuyển tab, tránh khởi động background service sai lúc đổi tab.
- `e5d461aeae93` — sửa handoff foreground YouTube.
- `91724438d7f5` — không ép YouTube background mode trong lúc prewarm.
- `ac471c452bb7` — không ép SmartTube background mode khi mở video.
- `ef77dbba582b` — sửa lifecycle HOME của YouTube Mobile.
- `5bf1fbec30ea` — thêm validation trong script để bảo đảm patch HOME-resume-preservation tồn tại.

### 3. UI Mobile
- `8729326843c9` — thu nhỏ và nâng logo NM7 Mobile.
- Trước đó đã có các sửa đưa toolbar/search/tùy chọn xuống dưới player và ép YouTube recommendations một cột; các thay đổi này phải được phân biệt với bản TV.

## Nhánh SharedPlaybackSession đã thử và lý do không dùng làm baseline

Chuỗi thử nghiệm sau `5bf1fbec` đã đưa vào:
- `SharedPlaybackSession.java` để lưu tab IPTV/YouTube, IPTV URL/name/mime/headers/position/playing.
- `PlayerActivity` lưu/khôi phục IPTV position và handoff sang YouTube.
- `HomeTabBar` cố gắng giữ đúng tab và dùng `REORDER_TO_FRONT`.
- `MainActivity` khôi phục tab YouTube sau process recreation.
- `MobileNm7Application` thay đổi thứ tự pause/release khi SmartTube Playback khởi động.
- Manifest bỏ `singleTask`, thêm `alwaysRetainTaskState=true`.
- UI giảm logo và tăng vùng player.

Đây là **thử nghiệm kiến trúc**, không phải kết quả đã xác nhận. Video máy thật sau đó cho thấy YouTube vẫn có thể load rồi xuất hiện splash/launcher và quay lại app, vì vậy không tiếp tục cộng thêm logic vào nhánh thử nghiệm này khi chưa có logcat xác định exception/lifecycle transition.

## Kết quả build Windows sau khi quay về baseline

- Nhánh đã được đưa về `5bf1fbec` để tách lỗi runtime khỏi chuỗi SharedPlaybackSession đang gây hồi quy.
- Các commit CI sau đó chỉ phục vụ đóng gói Mobile ARM:
  - `688b84b83505` — khôi phục ARM ABI split.
  - `577f8aee94be` — không để emulator smoke test chặn APK Mobile.
  - `0ea25e5e4f67` — sửa tìm đầu ra ARM64/ARMv7 trong thư mục output.
- Build thành công với đúng **2 APK ARM**, không universal:
  - ARM64-v8a: khoảng 61.9 MB.
  - armeabi-v7a: khoảng 51.4 MB.
- Đây chỉ xác nhận **compile/package**, không xác nhận lỗi YouTube/Android lifecycle đã hết.

## Kết luận để tiếp tục xử lý

- Không reset lại lịch sử các patch Windows/PowerShell ở trên.
- Không tiếp tục khẳng định SharedPlaybackSession là lời giải.
- Lỗi hiện tại phải được xử lý theo chuỗi: **tái hiện → lấy logcat đúng thời điểm YouTube chuyển từ IPTV → YouTube → xác định Activity/task/process nào bị finish/recreate → sửa một nguyên nhân → build Windows → test máy thật**.
- Đặc biệt phải phân biệt ba tình huống: (1) YouTube Activity bị finish, (2) SmartTube player bị release nhưng Activity còn, (3) toàn bộ NM7 process bị crash/restart. Video hiện tại chỉ chứng minh có hiện tượng rơi ra khỏi trạng thái YouTube và xuất hiện splash/launcher; chưa đủ để kết luận exception cụ thể.

---

# GHI NHẬN LỖI MỚI — NM7 IPTV MOBILE 1.10.24: chưa tự tải lại playlist khi mở ứng dụng (2026-09-14)

## Trạng thái Mobile hiện tại

- Repo chuẩn: `phuongnm7/iptv-player-android`, nhánh `main`, vẫn ở chế độ private.
- Bản ổn định mới nhất: `versionCode 41`, `versionName 1.10.24`. Người dùng đã xác nhận lỗi xóa nguồn đang sử dụng và quay về nguồn mặc định đã hoạt động đúng.
- 1.10.24 tiếp tục là baseline ổn định. Lỗi tải lại khi khởi động được ghi nhận để xử lý ở phiên bản tiếp theo; chưa thay đổi mã player, nguồn, Mobile UI, TV flavor, iOS hoặc Tizen trong bước kiểm tra này.

## Kết quả kiểm tra lỗi tải lại playlist

- Xác nhận lỗi tồn tại trong mã `main`: `MainActivity.onCreate()` gọi `restoreSession()`.
- Khi `SessionStore` có danh sách kênh hợp lệ, `restoreSession()` chỉ gọi `showPlaylist(state.result, state.source)`, điền URL và tắt trạng thái loading; không gọi `reloadPlaylistUrl()` hoặc `loadFromUrl()`.
- Vì vậy ứng dụng hiển thị nhanh playlist đã cache nhưng không kiểm tra nội dung mới từ URL khi mở lại. Tính năng nút **Tải lại** vẫn hoạt động và đã gửi `Cache-Control: no-cache`, `Pragma: no-cache`; lỗi nằm ở việc đường khởi động không kích hoạt nó.
- Nguồn mặc định hiện tại và nguồn URL tự thêm đều có thể giữ dữ liệu cũ nếu phiên cache còn hợp lệ. Chỉ nguồn mặc định cũ (legacy) hoặc phiên rỗng mới đi qua đường tải mạng lúc khởi động.

## Hướng sửa an toàn cho phiên bản tiếp theo

- Giữ cơ chế cache-first: hiển thị ngay danh sách đã lưu để ứng dụng mở nhanh.
- Sau khi giao diện cache xuất hiện, nếu nguồn hiện tại là URL HTTP/HTTPS thì tự tải lại ở nền đúng một lần cho lần mở ứng dụng.
- Khi tải thành công, thay danh sách và lưu phiên mới; khi mạng lỗi hoặc playlist trả về rỗng, giữ nguyên danh sách cache đang xem, không xóa kênh và không bật hộp lỗi chặn giao diện.
- Bảo toàn mục đang chọn, tìm kiếm/nhóm và tránh kết quả tải nền ghi đè một lần tải thủ công mới hơn.
- Bổ sung kiểm thử cho: cache URL kích hoạt refresh, nguồn file cục bộ không refresh, tải lỗi vẫn giữ cache, URL có dòng chuyển hướng lấy đúng dòng đầu và tải thủ công vẫn giữ hành vi hiện tại.
- Nếu triển khai, bắt đầu từ 1.10.24 trên `main`, tạo nhánh Mobile riêng và tăng phiên bản mới; không sửa repo NM7 TV.

---

# BÀN GIAO ỔN ĐỊNH — NM7 IPTV MOBILE 1.10.24 (2026-09-13)

## Sửa xóa nguồn đang dùng không quay về nguồn mặc định

- Đã phân tích video máy thật `video_2026-09-13_21-17-06.mp4`: sau khi xóa nguồn tự thêm đang được sử dụng, mục nguồn biến mất khỏi màn hình quản lý nhưng nhóm/kênh của nguồn đó vẫn còn trên màn hình chính; phiên cũ tiếp tục được lưu.
- Nguyên nhân: luồng xóa chỉ so sánh URL nguồn bằng chuỗi tuyệt đối với dòng đầu của `currentSource`. Trạng thái có mô tả/redirect khác có thể làm nhận diện sai. Ngoài ra, app chờ tải nguồn mặc định xong mới thay dữ liệu trong bộ nhớ, nên khi mạng chậm/lỗi giao diện vẫn treo ở playlist đã xóa.
- Khi xóa, app nay đối chiếu nguồn hiện hành với toàn bộ danh sách nguồn còn lại. Nếu nguồn đang dùng không còn, app lập tức chuyển trạng thái về nguồn mặc định.
- Trước khi gọi mạng, app xóa ngay danh sách kênh, nhóm, tìm kiếm, EPG, bộ đếm và phiên nguồn cũ; cập nhật màn hình về NM7 IPTV rồi tải lại nguồn mặc định. Vì vậy nguồn vừa xóa không thể tiếp tục tồn tại do cache hoặc do tải mặc định chậm.
- Nếu xóa một nguồn không phải nguồn đang dùng, app giữ nguyên nguồn hiện tại và mở lại màn hình quản lý như trước.
- Thêm unit test cho bốn trường hợp: nguồn đang dùng vẫn còn, nguồn đang dùng bị xóa, mô tả nguồn có dòng redirect, và nguồn mặc định/nội dung cục bộ không bị chuyển sai.
- Version: `versionCode 41`, `versionName 1.10.24`. Repo TV không bị thay đổi.
- Người dùng đã xác nhận Mobile 1.10.24 ổn định trên máy thật. Đây là bản nền chính thức trên `main`; mọi sửa lỗi/cải tiến Mobile tiếp theo phải bắt đầu từ mốc này và tăng phiên bản mới.
- Commit: phát hiện nguồn còn lại `6de217ae5fe074a1527df2bd908c47a354fbb172`; reset giao diện/phiên `0a568a4ff62a771df430c127dc562d65a2109c43`; test `39801f77d09021bce08826158d66fc13f9847edc`; tăng version `08da0bf16df557a897063702bfaf4a480ed9eeb4`; CI cuối `e4c713c161eb38381d95355f91490031a6b05ca4`.
- Workflow run `34763079646` (#224), job `103739184185`: compile, unit test, lint, đọc version, xác minh chữ ký, kiểm tra không chứa LibVLC, đóng gói và smoke-test Mobile trên Android 15 đều **SUCCESS**.
- Đã fast-forward `main` tới commit ổn định `28060225824eb12604834658d7c8d69bd6105b95`. Build xác nhận trên `main`: run `34764658816` (#225), attempt 2, job `103743881156` — toàn bộ compile, unit test, lint, ký/đóng gói và smoke-test Android 15 **SUCCESS**. Attempt 1 bị JVM runner crash, không phải lỗi mã.
- Artifact xác nhận trên `main`: `NM7-IPTV-Mobile-1.10.24-APK`, ID `10319919407`, archive SHA-256 `4cfe95fcc2eb415707097f9424b7faad309ff621921249550134e208f64bd86d`.
- Artifact `NM7-IPTV-Mobile-1.10.24-APK`, ID `10319532889`, archive SHA-256 `1dfd99bba7d2585a66a0566bb28ec4d9b74f539cd94f2596bea0354ce7a1d750`.
- APK: 7.723.542 bytes; SHA-256 `eaf2ae2ce216038655f3044777dfa00957c006852d177bb2c28336d20d5fe3e3`; `sha256sum -c` đạt.
- Kiểm thử máy thật đã được người dùng xác nhận ổn định. Luồng xóa nguồn đang dùng quay về NM7 IPTV hoạt động đúng; giữ 1.10.24 làm mốc hồi quy cho các bản sau.

---

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
- Phản hồi máy thật ngày 2026-09-13: bản 1.10.23 **ổn định hơn**; người dùng đang tiếp tục test dài hạn. Chưa đánh dấu là hết lỗi tuyệt đối.
- Hồ sơ nguyên nhân, kiến trúc sửa, kết quả và checklist áp dụng cho Android TV: `docs/PLAYBACK_STABILITY_1.10.23.md`. Khi xử lý TV phải điều chỉnh theo Player/lifecycle TV, không sao chép nguyên trạng lớp Mobile.

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

---

# BÀN GIAO TIẾN ĐỘ — NM7 IPTV MOBILE 1.10.26 — 2026-09-18

## Phạm vi hiện tại

- Dự án đang tiếp tục đúng phạm vi **NM7 IPTV Mobile 1.10.26** trên nhánh `fix/mobile-1.10.26-sleep-timer-icon` của repo riêng tư `phuongnm7/iptv-player-android`.
- **Không build/không bàn giao Android TV** trong mốc này.
- Mục tiêu APK cuối: đúng **2 APK ARM**, gồm `arm64-v8a` và `armeabi-v7a`; không tạo x86/x86_64/universal và không chứa `libvlc.so`.
- Phiên bản: `versionCode 44`, `versionName 1.10.26`.

## SmartTube — tích hợp YouTube mobile

- SmartTube đã được tích hợp vào NM7 IPTV để có tab YouTube bên cạnh IPTV.
- Nguồn SmartTube được lấy từ fork **Systematiq phone/touch**, sau đó merge upstream SmartTube stable **32.47s**, commit upstream `51e87ab4ad57ae672fc24bd216427d3e81bbddb7`, phát hành ngày 2026-09-13.
- Giữ giao diện phone/touch của fork thay vì chuyển dự án sang UI TV.
- Đã xử lý xung đột tài nguyên Media3/ExoPlayer cũ: tách `exo_player_view.xml` thành `st_exo_player_view.xml`, tách `exo_simple_player_view.xml` tương ứng và đổi `PlayerView.java` sang layout riêng. Đây là bản sửa để tránh `ClassCastException` giữa `androidx.media3.ui.AspectRatioFrameLayout` và `com.google.android.exoplayer2.ui.AspectRatioFrameLayout` khi mở YouTube.
- Đã đổi thuộc tính SmartTube `show_buffering` thành `st_show_buffering` để không va chạm với Media3 PlayerView của IPTV.
- Đã sửa API OkHttp trong `DohProviders.kt` sang parser tương thích với OkHttp hiện dùng.
- Đã xử lý placeholder string, namespace/compileSdk/flavor và tương thích AGP 8.13/Kotlin cần thiết cho SmartTube.
- Đã loại bỏ thành phần TV/VLC không dùng cho Mobile, trong đó có `VlcFallbackActivity.java`; không đưa lại vào bản Mobile.

## Luồng IPTV ↔ YouTube và lifecycle

- Tab bar Mobile dùng `REORDER_TO_FRONT` và tắt animation để tránh hiện tượng chuyển Activity chồng hình.
- Khi chỉ **đổi tab**, IPTV player được giữ lại thay vì bị giải phóng; cơ chế đánh dấu `nm7.tab.switch.until` và xử lý lifecycle được thêm để phân biệt tab switch với việc thực sự rời player.
- Khi bắt đầu phát video YouTube thực tế, IPTV được pause.
- Khi bắt đầu phát IPTV thực tế, media bên ngoài được pause; không pause chỉ vì đổi tab.
- Khi PlaybackActivity YouTube kết thúc bằng Back, luồng được đưa về BrowseActivity thay vì thoát khỏi ứng dụng; chuyển Activity không animation.
- Đã thêm cơ chế giữ BrowseActivity và PlayerActivity để quay lại đúng màn hình thay vì tạo lại Activity không cần thiết.

## YouTube UI và font

- Mobile ép BrowseActivity portrait và đã có lớp runtime cố gắng ép các grid/recycler về **một cột**, tránh giao diện 2 cột kiểu TV.
- SmartTube Activity được áp dụng font Android `sans-serif` cho TextView có chữ tiếng Việt; WebView dùng `standard/sans-serif font family` và UTF-8.
- Lưu ý: các mục trên vẫn phải **kiểm thử trên máy thật**. Trước khi người dùng test bản 1.10.26, chưa đánh dấu các lỗi UI/font là đã hết hoàn toàn.
- Đã ghi nhận video test máy thật cho hiện tượng chuyển tab: khoảng 9,6 giây có YouTube ở bên trái và IPTV ở bên phải; khoảng 11,3 giây xuất hiện overlap theo chiều ngược lại. Đây được xem là dấu hiệu cần xử lý lifecycle/transition của Activity, không phải bằng chứng ứng dụng chạy split-screen thật.
- Người dùng trước đó vẫn quan sát thấy: YouTube đôi lúc thoát khi chuyển từ IPTV; chữ tiếng Việt chưa đúng; UI còn giống TV; YouTube tải chậm; Back từ video có thể thoát video/app. Các sửa trên được đưa vào bản test 1.10.26 để xác minh lại.

## Tối ưu khởi động YouTube

- Đã cấu hình SmartTube runtime dùng connection keep-alive và connection pool; bật các cấu hình mạng liên quan để giảm chi phí tạo kết nối lặp.
- Đã thêm Wi-Fi high-performance lock trong vòng đời Activity của Mobile khi app đang hoạt động; mục tiêu là giữ kết nối ổn định khi chuyển giữa IPTV và YouTube.
- Đây là tối ưu runtime, **không coi là chứng minh YouTube đã tải nhanh trên mọi thiết bị** cho đến khi test máy thật hoàn tất.

## Build system — Windows

- Do GitHub Actions gặp giới hạn/bị chặn ở một số lần chạy, quá trình build Mobile 1.10.26 được chuyển sang build trực tiếp trên Windows.
- Môi trường đã xác nhận: JDK 17, Gradle 8.13, Android SDK 36, Build Tools 36.0.0.
- Script build: `scripts/build-mobile-windows.ps1`.
- Script đã được sửa nhiều vòng để xử lý đường dẫn repo, kiểm tra Java trên PowerShell, tham số `$args`, SmartTube shallow clone/merge tag, Git identity cục bộ, lỗi WebSettings và kiểm tra ABI đầu ra.
- SmartTube shallow clone đã được xử lý bằng fetch/unshallow trước khi merge upstream tag 32.47s.
- Đã sửa lỗi compile do gọi API không tồn tại `WebSettings.setDefaultFontFamily`; bản đúng giữ `setStandardFontFamily("sans-serif")`, `setSansSerifFontFamily("sans-serif")` và UTF-8.

## ABI — lỗi đã xác định và sửa

- Một lần build Windows bị chặn bởi cấu hình đồng thời `ndk.abiFilters` và ABI splits, với lỗi: `Conflicting configuration: 'armeabi-v7a,arm64-v8a' in ndk abiFilters cannot be present when splits abi filters are set`.
- Nguyên nhân: dự án đã dùng `splits { abi { include("armeabi-v7a", "arm64-v8a") } }`, vì vậy không thêm `ndk.abiFilters` thứ hai.
- Đã loại bỏ cấu hình `ndk abiFilters` xung đột trong commit `5267b77ebec0229e33471713f61c5b72f0e0560e`.
- Script Windows đã được sửa/khôi phục phần tail kiểm tra APK ở commit `64722c096b283e3dd6cee6525409b75c28a53e65`.
- Các commit sửa script trước đó gồm: `4941c706ed935ef851d4914f8c41816da7457ed4`, `b02c3712122b820681266eea1132ac6f3c8105cb`, `5adfb968a800f5cca207a41ad35f1784f61c8135`, `7cb224d10cac64abb1cf74bf50c298fd6e7bd21a`, `d10c8688622bbc0dedc08e3402841a70252a6d85`, `a749338ff70110835b0860901cf0aa091ce86364`, `eabc49afaf0e1bfc2512a16941a449858bf6911b`, `d593af8c6b7372641b033ad500678bec4872a596`.

## Build Windows — KẾT QUẢ ĐÃ ĐẠT

- Ngày 2026-09-18, build script đã chạy đến cuối và báo:
  `=== BUILD SUCCESSFUL ===`
- Script xác minh đúng **2 APK** và kiểm tra không có x86/x86_64/libvlc; kiểm tra ABI native entries và merged manifest Mobile cũng nằm trong bước xác minh.
- APK đầu ra:
  - `NM7-IPTV-Mobile-1.10.26-arm64-v8a.apk` — **61,892,357 bytes**.
  - `NM7-IPTV-Mobile-1.10.26-armeabi-v7a.apk` — **51,398,095 bytes**.
- Thư mục bàn giao trên Windows:
  `C:\\Users\\Administrator\\Documents\\Codex\\2026-09-08\\hay\\work\\iptv-player-android\\dist\\mobile`.
- Đây là **bản build để test máy thật**, chưa phải xác nhận hết lỗi runtime.

## CI / GitHub Actions

- GitHub Actions đã từng có các lần build Mobile thành công ở các mốc trước, nhưng nhánh 1.10.26 gặp thêm giới hạn workflow/token và giới hạn ngân sách, nên build Windows được dùng để tiếp tục xác minh APK.
- Một lần chạy workflow final gần đây: run `35294946500`, workflow `android-mobile-final.yml`, run #83, kết thúc failure trước khi có job runner; một workflow compatibility khác bị GitHub App token từ chối do thiếu quyền `workflows`.
- Không dùng trạng thái CI lỗi này để kết luận source hiện tại không build được; build Windows ngày 2026-09-18 đã xác minh ngược lại rằng Mobile 1.10.26 hiện có thể compile/package thành công.

## Trạng thái hiện tại — CHỜ TEST MÁY THẬT

**Đã đạt:**
- SmartTube 32.47s tích hợp vào Mobile.
- Media3/ExoPlayer resource collision đã xử lý.
- OkHttp/AGP/Kotlin/namespace/compile compatibility đã xử lý.
- TV/VLC component không nằm trong mục tiêu Mobile.
- Lifecycle IPTV ↔ YouTube đã có cơ chế giữ player khi đổi tab và pause khi bắt đầu phát nguồn kia.
- YouTube portrait/phone UI và các lớp ép một cột/font tiếng Việt đã được đưa vào bản test.
- Windows build thành công.
- Đúng 2 APK ARM: arm64-v8a + armeabi-v7a.

**Chưa xác nhận trên máy thật:**
1. IPTV → YouTube → quay lại IPTV khi **chưa phát YouTube** có giữ nguyên player hay không.
2. IPTV đang phát → phát video YouTube có pause IPTV đúng hay không.
3. YouTube đang phát → phát IPTV có pause YouTube đúng hay không.
4. Đổi tab đơn thuần có còn làm Activity chồng/nhấp nháy/thoát hay không.
5. Giao diện YouTube có thực sự thành một cột mobile hay vẫn còn 2 cột ở một số màn hình.
6. Chữ tiếng Việt trong đăng nhập/cài đặt có hiển thị đúng glyph/font hay không.
7. Thời gian khởi động YouTube có cải thiện đủ trên thiết bị thật hay không.
8. Bấm Back từ video YouTube có quay về Browse/YouTube đúng hay thoát ứng dụng.
9. Phát IPTV dài và chuyển qua lại nhiều lần có còn lỗi lifecycle hoặc mất player hay không.

## Quy tắc cho vòng test tiếp theo

- Không sửa code chỉ dựa trên suy đoán trước khi có lỗi tái hiện/log/ảnh/video từ bản 1.10.26.
- Nếu có lỗi, ghi lại **bước tái hiện → kết quả thực tế → log/ảnh/video → nguyên nhân → sửa → build lại**.
- Mọi sửa Mobile tiếp theo phải tăng versionCode/versionName trước khi tạo APK bàn giao mới.
- Không build TV trong vòng test này.
- Không đưa token, cookie, khóa DRM hoặc thông tin đăng nhập vào log/commit.

## Mốc tiếp theo

Người dùng đang cài và kiểm thử hai APK 1.10.26 trên thiết bị Android thật. Kết quả test thực tế sẽ là đầu vào tiếp theo để quyết định sửa lỗi nào; **không đánh dấu 1.10.26 là ổn định hoàn toàn cho đến khi vòng test này hoàn tất**.

Cập nhật: **2026-09-18 09:14 +07:00**.

---

# TEST MÁY THẬT — LỖI CÒN LẠI VÀ VÒNG SỬA TIẾP THEO — 2026-09-18

## Kết quả test Mobile 1.10.26 từ người dùng

Ảnh/video máy thật xác nhận các lỗi còn tái hiện:

1. IPTV → YouTube → quay lại IPTV: inline player vẫn bị mất/thoát.
2. YouTube mở video: thời gian tải video còn chậm.
3. Giao diện YouTube vẫn mang dấu hiệu SmartTube/TV, chưa khớp hoàn toàn giao diện mobile tham khảo.
4. Đang phát video YouTube → nhấn Home: player bị đóng; chưa giữ được player để quay lại.
5. Giao diện IPTV: thanh tìm kiếm + Tùy chọn đang ở phía trên player; yêu cầu mới là đưa xuống dưới player để vùng video cao hơn.

## Sửa đã commit

### IPTV toolbar xuống cuối màn hình
- MobileIptvUi chèn thanh tìm kiếm/tải lại/tùy chọn ở cuối mainRoot, thay vì phía trên inline player.
- HomeTabBar dành thêm 64dp đáy cho MainActivity để toolbar không bị navigation bar đè.
- Commit: fd6f285abb7682cced76e020b9ae69ec48f61692, aeb6bba044c2b11ab4887be8b92224489342c22.

### YouTube Home không release player
- SmartTube PlaybackActivity được patch để nhận biết onUserLeaveHint() của Home.
- Khi Home được nhấn, player được pause nhưng không release; khi quay lại Activity, player được tiếp tục nếu trước đó đang phát.
- Tab switching dùng FLAG_ACTIVITY_NO_USER_ACTION để không bị nhầm thành Home.
- Mục tiêu là không phát YouTube nền: Home chỉ giữ player trong Activity, không tiếp tục video khi app ở nền.
- Android xác nhận onUserLeaveHint() được gọi khi Activity sắp ra nền do lựa chọn của người dùng như Home; FLAG_ACTIVITY_NO_USER_ACTION ngăn callback khi Activity bị tạm dừng do Activity khác được khởi chạy. citeturn3search0turn6search1
- Media3 background playback là kiến trúc Service riêng và không được dùng cho yêu cầu Home hiện tại. citeturn4search0turn4search1
- Commit script: 84647a4ffd4ce71c290e8ed5955e6b1f82597112.

### Back từ YouTube video
- MobileNm7Application đăng ký Android 13+ Back callback cho SmartTube PlaybackActivity.
- Back đưa BrowseActivity lên trước rồi finish PlaybackActivity.
- Callback dùng reflection để giữ tương thích minSdk 23.
- Commits: 89ab17c90b2133bceb31d3016593bedaca1a8289 và 7c92c07a2036fce15f3981ba063b32d349ebeff2.

### YouTube recommendations một cột
- forceSingleColumn() nay áp dụng cả cho SmartTube PlaybackActivity, không chỉ BrowseActivity.
- Build script tiếp tục patch GRID_COLUMNS trong SmartTube Java/Kotlin.
- GridLayoutManager.setSpanCount(1) là cơ chế Android hỗ trợ cho grid dọc một cột. citeturn7search0turn7search3
- Commit runtime: 74eb83900b5b45fc5b438087e5fa9c5a2619e8e9.

### Giảm thời gian mở YouTube
- MobileNm7Application pre-warm SmartTubeRuntime sau 900ms kể từ khi ứng dụng khởi động.
- Mục tiêu là đưa chi phí reflection/class-loading ra khỏi lần chạm tab YouTube đầu tiên.
- Đây là tối ưu startup; tốc độ tải mạng/video phải đo lại trên máy thật.
- Commit: 2488cf4b6163f7717764ece436cf34d55e34973.

### Giảm branding SmartTube TV
- Windows build script thay text branding SmartTube trong XML/properties của phần SmartTube được clone thành NM7 TV.
- Vẫn giữ phone/touch fork và không đưa TV/VLC vào Mobile.
- Commit script mới nhất: 148bc07a3140084c99d576f2441995b7010ce3c9.

## Trạng thái

Đã sửa mã theo các lỗi vừa tái hiện. Chưa đánh dấu build mới thành công cho đến khi chạy script Windows và cài APK mới.

Test lại: IPTV → YouTube → IPTV; YouTube phát → Home → quay lại; YouTube Back; recommendations một cột; branding; IPTV toolbar dưới player; và thời gian mở YouTube.

---

# BÀN GIAO TIẾN ĐỘ — NM7 IPTV MOBILE 1.10.26 — 2026-09-19 01:56 +07:00

## Trạng thái sau vòng sửa mới nhất

Người dùng đã build lại Mobile 1.10.26 trên Windows và **BUILD SUCCESSFUL**.

Kết quả APK xác nhận:
- `NM7-IPTV-Mobile-1.10.26-arm64-v8a.apk` — **61,915,325 bytes**
- `NM7-IPTV-Mobile-1.10.26-armeabi-v7a.apk` — **51,421,065 bytes**
- Chỉ bàn giao 2 ABI Mobile: ARM64 + ARMv7.
- Không build Android TV trong vòng này.
- Các cảnh báo `META-INF/... not protected by signature` xuất hiện trong quá trình đóng gói là warning, không phải lỗi làm build thất bại.

## Lỗi compile vừa gặp và đã xử lý

Build trước đó dừng với:
```
error: duplicate class: vn.phuong.iptvplayer.HomeTabBar
```

Nguyên nhân là tồn tại đồng thời hai source có cùng class `HomeTabBar`:
- `app/src/main/java/vn/phuong/iptvplayer/HomeTabBar.java`
- `app/src/main/java/vn/phuongnm7/iptvplayer/HomeTabBar.java`

Đã xóa bản trùng trong `vn/phuongnm7/iptvplayer`, giữ lại bản đúng trong `vn/phuong/iptvplayer`, đồng thời giữ các thay đổi mới cho luồng IPTV → YouTube.

Commit xử lý cuối cùng:
- `4b55b3c4005245151ba0b7f68f77e389f4a67e87` — xóa source `HomeTabBar` trùng.
- `6d332fed6bbe6b0a0d6ca3ff06d6a76276fa23f2` — khôi phục/hoàn thiện `HomeTabBar` đúng package và luồng Mobile.

Đã kiểm tra cây source sau sửa: chỉ còn **một** `HomeTabBar.java`.

## IPTV → YouTube: trạng thái mã hiện tại

Yêu cầu đã được sửa theo hướng:
- Đang phát IPTV.
- Chỉ bấm chuyển sang tab YouTube, **chưa chọn video** → IPTV phải tiếp tục phát.
- Khi người dùng thực sự chọn một video và SmartTube `PlaybackActivity` bắt đầu → lúc đó NM7 mới pause/release IPTV để nhường decoder cho YouTube.

Các commit chính:
- `02c7f32f9d4d58f1fe01aaf94623576a95f38b45` — giữ IPTV chạy cho tới khi YouTube thực sự bắt đầu phát.
- `2cc0d9901b3848569364f1a4d8bcee85e2e10102` — không finish `PlayerActivity` khi chỉ mở Browse YouTube.

Trong `HomeTabBar.openBrowse()`, việc chuyển tab không còn gọi `activity.finish()`; SmartTube Browse được mở bằng `REORDER_TO_FRONT`. `PlayerActivity.prepareForYoutubeHandoff()` cũng không release player.

## YouTube BACK → mini-player: trạng thái mã hiện tại

Mục tiêu:
- YouTube đang phát video.
- Nhấn Back lần đầu → quay về Browse/Home YouTube và giữ video ở mini-player.
- Không đóng ngay playback session.

Đã xác định nguyên nhân của bản patch BACK trước:
- SmartTube `PlaybackActivity.onBackPressed()` đặt `mIsBackPressed = true`.
- `finish()` có đường dẫn `enterPipMode()` + `startParentView()`.
- Nhưng `skipPip()` có thể trả về true khi background shortcut là HOME, khiến playback Activity bị finish trực tiếp và bỏ qua đường dẫn mini-player.

Đã đổi patch sang sửa **`skipPip()`**, thay vì phụ thuộc vào hình dạng `onBackPressed()`:
```java
private boolean skipPip() {
    // NM7 Mobile: BACK from a playing video must return to YouTube Browse/mini-player,
    // not close the playback session. HOME/background handling remains separate.
    return false;
}
```

Commit:
- `f877e149c4fa593958f4c4cfe5e897c797356434` — Fix BACK via SmartTube parent view path.
- `ba63b09ad79318f0de7c476c76580edb252c9dda` — Validate SmartTube BACK patch.
- `28e78c504a99732f0002aa221b4a2b9d7e757cb4` — Preserve YouTube mini-player on first BACK.

Build script hiện dùng **PATCH15** và không còn patch trực tiếp `onBackPressed()` theo source shape cũ.

## YouTube background / quay lại ứng dụng

Cơ chế đã có từ vòng trước và vẫn được giữ:
- HOME/khóa màn hình khi YouTube đang phát được xử lý theo lifecycle/background mode của SmartTube.
- Tab YouTube được lưu vào `SharedPlaybackSession`.
- Khi mở lại NM7, có cơ chế đưa SmartTube Playback/Browse hiện hữu lên trước thay vì tạo lại luồng không cần thiết.
- Manifest Mobile giữ task state và launch mode phù hợp để bảo toàn phiên.
- Không coi đây là xác nhận cuối cùng cho đến khi test máy thật trên APK build mới.

## Tối ưu tốc độ YouTube hiện có

- SmartTube runtime dùng Cronet/player data source và connection keep-alive/connection pool.
- Có pre-warm SmartTube runtime.
- Giảm traversal UI không cần thiết.
- Browse/recommendations được ép theo hướng phone/mobile và một cột.
- MainActivity fallback khi khôi phục YouTube đã giảm delay xuống khoảng 80ms.
- Các tối ưu này là tối ưu khởi động/runtime; tốc độ tải video thực tế vẫn phải xác minh trên thiết bị thật.

## Các thay đổi Mobile khác đang giữ nguyên

- Phạm vi vẫn là **NM7 IPTV Mobile**, không đưa Android TV vào bản này.
- SmartTube phone/touch fork + upstream 32.47s.
- Media3/ExoPlayer resource collision đã được xử lý.
- OkHttp/AGP/Kotlin/compile compatibility của SmartTube đã xử lý.
- IPTV vẫn dùng Media3 player.
- ABI output chỉ ARM64 + ARMv7.
- Branding/logo Mobile đã được điều chỉnh theo các yêu cầu trước.
- Thanh tìm kiếm/tùy chọn IPTV đã được đưa xuống dưới player để tăng không gian hiển thị video.

## Trạng thái kiểm thử — CHỜ NGƯỜI DÙNG TEST

Build hiện tại đã **compile/package thành công**, nhưng các hành vi dưới đây chưa được đánh dấu PASS trên máy thật:

1. IPTV đang phát → bấm YouTube, **không chọn video** → IPTV vẫn phát.
2. Sau đó chọn video YouTube → IPTV chỉ dừng khi YouTube PlaybackActivity thực sự bắt đầu.
3. YouTube đang phát → Back 1 lần → Browse/Home + mini-player, video tiếp tục.
4. YouTube đang phát → Home/khóa màn hình → xử lý background đúng yêu cầu.
5. Mở lại NM7 → vẫn ở YouTube và khôi phục đúng video.
6. Chuyển IPTV ↔ YouTube nhiều lần không overlap/thoát bất thường.
7. YouTube load nhanh hơn và không bị thoát khi mở video.
8. UI YouTube vẫn đúng phone/mobile, không quay lại layout TV.
9. Chữ tiếng Việt trong SmartTube hiển thị đúng.
10. IPTV player và thanh tìm kiếm/tùy chọn không bị che hoặc mất diện tích bất thường.

## Quy tắc xử lý vòng tiếp theo

- Chờ kết quả test thực tế từ người dùng trước khi tiếp tục sửa.
- Nếu có lỗi: ghi lại bước tái hiện, kết quả thực tế, ảnh/video/log nếu có; sau đó mới xác định nguyên nhân và sửa.
- Không quay lại phương án dùng một ExoPlayer literal chung nếu chưa có thiết kế lifecycle chắc chắn; các thử nghiệm shared-player trước đây đã gây regression.
- Không đưa TV code/UI vào Mobile.
- Không đánh dấu 1.10.26 ổn định hoàn toàn chỉ vì build thành công.
- Khi cần tạo APK mới sau khi sửa mã ứng dụng, phải cập nhật version theo quy trình của dự án trước khi bàn giao.

## Điểm dừng để tiếp tục

**Hiện tại không cần sửa thêm trước khi test.**

Người dùng đã có APK build thành công và sẽ cài/test trên điện thoại. Kết quả test tiếp theo sẽ là đầu vào trực tiếp cho vòng sửa tiếp theo.


---

# TEST VIDEO 2026-09-19 — PHÂN TÍCH LỖI IPTV → YOUTUBE + BACK

Người dùng gửi video `video_2026-09-19_02-02-22.mp4` (~101,5 giây). Quan sát video cho thấy:
- Lần chuyển đầu IPTV → YouTube có lúc giữ được IPTV, nhưng các lần chuyển sau IPTV bị mất/dừng.
- Có các lần Browse YouTube tải lâu/hiển thị trạng thái loading.
- BACK từ YouTube Playback chưa đạt yêu cầu mini-player ổn định.

## Nguyên nhân kỹ thuật được xác định từ source

### IPTV → YouTube
Cơ chế cũ phụ thuộc vào `ActivityLifecycleCallbacks.onActivityPaused()` để đặt `backgroundPlaybackActive=true`. Trong khi `PlayerActivity.onPause()`/`onStop()` là nơi quyết định release ExoPlayer, thứ tự lifecycle callback này có thể tạo race. Điều này phù hợp với hiện tượng lần đầu có thể hoạt động nhưng các lần sau không ổn định.

### Sửa mới
Thêm cờ trực tiếp trong `PlayerActivity`:
`keepPlayerForTabSwitch`.

Khi `prepareForYoutubeHandoff()` được gọi:
- lưu vị trí IPTV;
- đặt `keepPlayerForTabSwitch=true`;
- không release player;
- chuyển tab sang YouTube.

`onPause()` và `onStop()` nay kiểm tra cờ này trước khi khởi động background service hoặc release ExoPlayer. `onStart()` reset cờ khi PlayerActivity thực sự trở lại foreground.

Commit:
- `b1ceb839c5f3e2274a011d0e65639c85e5a68fde`.

## YouTube BACK → mini-player
Đã xác định thêm một điểm xung đột: MobileNm7Application tự đăng ký `OnBackInvokedCallback` và từ callback đó gọi lại `activity.onBackPressed()`. Cách này có thể bypass/đụng với đường dispatch BACK native của SmartTube, khiến PlaybackActivity đóng thay vì đi qua parent-view/PIP path.

### Sửa mới
Không còn gọi `installSmartTubeBackHandling(activity)` khi tạo SmartTube PlaybackActivity. Để Android/SmartTube native Back dispatch xử lý, kết hợp với patch SmartTube `skipPip() -> false` đã có trong build script PATCH15.

Commit:
- `95cc48dc0830ae0436d23a0f658110902a1f1589`.

## Version
Vì đây là sửa code playback/lifecycle, version Mobile đã tăng:
- versionCode: **45**
- versionName: **1.10.27**

Commit:
- `03bc0a49104cdb9a79a1981e651858273fd0526c`.

## Trạng thái
Đã sửa source nhưng **chưa build APK 1.10.27**. Bước tiếp theo là build Windows và test lại:
1. IPTV → YouTube nhiều lần liên tiếp khi chưa chọn video: IPTV phải tiếp tục phát.
2. Chọn video YouTube: IPTV mới release.
3. YouTube video → BACK 1 lần: phải về Browse với mini-player, không đóng video.
4. YouTube → IPTV → YouTube lặp lại nhiều lần.
5. HOME/khóa màn hình và mở lại YouTube.

Không đánh dấu lỗi đã hết cho đến khi APK 1.10.27 được build và test máy thật.


---

# VÒNG SỬA 1.10.28 — 2026-09-19 06:35 +07:00

Người dùng xác nhận bản 1.10.27 vẫn còn hai lỗi:
- YouTube video → Back 1 lần chưa thu nhỏ về Browse/mini-player.
- IPTV đang phát → chuyển sang tab YouTube vẫn bị dừng ngay, trái yêu cầu phải giữ IPTV cho đến khi YouTube thực sự mở PlaybackActivity.

## Sửa IPTV → YouTube

Nguyên nhân còn sót: dù đã có `keepPlayerForTabSwitch`, `onStop()` vẫn có thể chạy sau khi trạng thái `tabSwitchPending` đã bị clear. Vì vậy lần chuyển tab sau có thể rơi vào nhánh `releasePlayer()`.

Sửa mới trong `PlayerActivity.onStop()`:
- Nếu Activity chưa `isFinishing()` và `SharedPlaybackSession.tab(this) == TAB_YOUTUBE`, luôn giữ IPTV ExoPlayer.
- `MobileNm7Application.isTabSwitchPending()` vẫn là lớp bảo vệ bổ sung.
- Chỉ release IPTV khi Activity thực sự kết thúc hoặc khi SmartTube PlaybackActivity bắt đầu và gọi `pauseIptvPlayer()`.

Commit:
- `0414fcb802537a74789bc3f6cc6c37351e09d488`.

Điều này loại bỏ dependency vào thứ tự callback lifecycle trong các lần chuyển tab lặp lại.

## Sửa YouTube Back → mini-player

Patch `skipPip() -> false` trước đó chưa đủ đối với phone PlaybackActivity. Bản phone có `onBackPressed()` riêng; gọi `super.onBackPressed()` làm PlaybackActivity kết thúc trước khi Browse/parent-view tiếp quản.

Build script nay patch trực tiếp `PlaybackActivity.onBackPressed()`:
- Giữ xử lý details/comments back.
- Đặt `mIsBackPressed=true`.
- Gọi `blockEngine(true)`.
- Gọi `getViewManager().blockTop(this)`.
- Gọi `getViewManager().startParentView(this)`.
- Chỉ fallback `super.onBackPressed()` nếu parent-view path ném exception.

Mục tiêu là giữ playback engine sống và đưa Browse lên làm parent, tạo đúng hành vi mini-player thay vì đóng video.

Commit:
- `82170585b8ac0b6f50cf5520c4c09a4a526bfe94`.

Custom Android `OnBackInvokedCallback` của NM7 vẫn đã được loại bỏ từ commit trước, nên không còn gọi `onBackPressed()` hai lần.

## Version mới

- versionCode: **46**
- versionName: **1.10.28**

Commit:
- `58bd32d3d695a2e2af764d7e83ac7a6a3387c3a0`.

## Chưa build

Source đã sửa nhưng **chưa chạy build Windows 1.10.28**.

Build/test bắt buộc:
1. IPTV phát → YouTube tab, không chọn video → IPTV tiếp tục phát.
2. Lặp IPTV → YouTube ít nhất 5 lần khi chưa mở video → IPTV không được tắt.
3. Mở video YouTube → IPTV mới release.
4. YouTube đang phát → Back 1 lần → Browse/Home + mini-player, video vẫn phát.
5. Back lần 2 mới thực hiện hành vi rời Browse theo thiết kế.


# VÒNG TỐI ƯU 1.10.74 — 2026-09-22

## Phân tích video máy thật người dùng gửi

Video cho thấy sau khi chọn YouTube video, vùng player vẫn đen và spinner kéo dài khoảng 10+ giây trong khi nội dung chưa xuất hiện; hai tab YouTube/IPTV vẫn nằm ở đáy màn hình trong trạng thái player toàn màn hình.

## Sửa 1.10.74

- **Tối ưu đường load video:** giữ request `getFormatInfoObserve(videoId)` đang chạy thay vì để cleanup của controller hủy request rồi tạo lại một vòng network thứ hai khi PlaybackView/Activity được rebind. Đây là điểm có thể tạo spinner dài ngay sau khi người dùng bấm video.
- **Giữ nguyên thumbnail:** vẫn dùng `maxresdefault.jpg`, không hạ xuống `mqdefault.jpg`.
- **Ẩn tab khi player fullscreen:** khi SmartTube PlaybackActivity chuyển sang landscape/fullscreen, HomeTabBar YouTube + IPTV được ẩn; khi trở lại portrait, hai tab được hiện lại.
- Không thay đổi chức năng/UI YouTube ngoài hành vi fullscreen nêu trên.
- Không thêm Android TV vào Mobile.

## Version

- versionName: **1.10.74**
- versionCode: **92**

## Commit chính

- `ff11b17e51f77a0bd3ab50a45f7e1f5fcc203310` — thêm patch v74.
- `65ac7b43e9b2bd258fd2d082de790babe997fadc` — đưa v74 vào Mobile build chain.
- `59063c205b275fa3062e3a1463bc24e77526afff` — bump version 1.10.74.
- `34cd17d29e5415f29dc432fd53ec34a95a14ca1f` — verifier cho v74.

## Trạng thái

Đã sửa source và đẩy lên branch Mobile. **Chưa coi tốc độ đã được xác nhận cho đến khi CI build thành công và người dùng test APK 1.10.74 trên máy thật.**


## CI correction 1.10.74

- Run #520 failed during the v74 patch step because the first v74 script pointed at the wrong generated SmartTube path for `VideoLoaderController.java`.
- Corrected the path to `third_party/SmartTube-droid/common/src/main/java/com/liskovsoft/smartyoutubetv2/common/app/models/playback/controllers/VideoLoaderController.java`.
- No APK was produced from the failed run. The correction is committed and must be rebuilt before testing.


## 1.10.75 — phân tích video thực tế và tối ưu đường phát

### Phân tích video 218815.mp4
- Video test dài ~26.2 giây; ở các mốc ~2.4s, 4.8s và 7.2s player vẫn đen với spinner. Hình video chỉ xuất hiện rõ khoảng ~9.5s sau thao tác chọn.
- Điều này cho thấy độ trễ nằm trước first frame, không phải chỉ do giao diện/tab fullscreen.

### Root cause đã xác định trong MediaServiceCore
- `getFormatInfoObserve(videoId)` đi vào `YouTubeMediaItemService.getFormatInfo()`.
- Đường mặc định gọi `WEB_EMBED` trước. Client này thuộc nhóm cần Web PoToken.
- `VideoInfoService.getVideoInfo()` còn có thể chạy thêm các bước bổ sung trước khi trả format: extended-HLS và lấy thêm translation languages.
- Vì player chỉ có thể `openDash/openHls` sau khi format info trả về, toàn bộ thời gian này trở thành spinner trước first frame.

### 1.10.75
- Thêm fast playback resolver: với playback thông thường, thử `TV_DOWNGRADED` trước vì không đi qua Web PoToken path.
- Nếu client nhanh không trả được video playable, tự động quay về toàn bộ multi-client fallback của SmartTube.
- Nếu người dùng bật Extended HLS, giữ nguyên đường cũ để không làm mất chức năng đó.
- Giữ v74: request đang chạy không bị hủy khi owner/player rebind.
- Giữ thumbnail `maxresdefault.jpg`, không hạ chất lượng.

### Version
- versionName: **1.10.75**
- versionCode: **93**

**Chưa kết luận tốc độ đã đạt mức IPTV cho tới khi APK 1.10.75 được build và người dùng test trên cùng thiết bị/mạng.**


# 1.10.93 — sửa dứt điểm startup YouTube theo video máy thật — 2026-09-25

Người dùng gửi video test sau 1.10.92 và xác nhận hiện tượng nháy khung hình vẫn còn, đồng thời lúc mở video có hiện tượng đơ/lag/giật.

## Phân tích video
- Tại khoảng 9.9s người dùng mở video YouTube.
- Vùng player giữ trạng thái chuyển tiếp trong khoảng ~1 giây trước khi nội dung mới ổn định.
- Vì vậy v92 chỉ giảm thời gian chờ poster/đếm rendered buffer nhưng chưa chứng minh được decoder đã thực sự vào trạng thái đang phát.

## Sửa 1.10.93
- Poster chỉ được gỡ khi đồng thời đạt:
  - ít nhất 4 output buffers mới;
  - đã có first-frame timestamp;
  - first-frame đã tồn tại ít nhất 140ms;
  - ExoPlayer ở STATE_READY, playWhenReady=true và isPlaying=true.
- Tăng safety timeout poster lên 1800ms để không lộ surface chưa ổn định.
- Khôi phục bufferForPlaybackMs từ 200ms lên 500ms; 200ms là quá tích cực cho startup và có thể làm tăng stall/giật khi format/network vừa bắt đầu.
- Không thay đổi IPTV, status bar, avatar, live chat, background playback hoặc navigation.

## Version
- versionName: **1.10.93**
- versionCode: **109**

## Trạng thái
Source đã cập nhật và CI sẽ build lại Mobile. Chưa đánh dấu lỗi đã hết cho đến khi CI thành công và người dùng test APK 1.10.93 trên đúng thiết bị/mạng.


## 1.10.96 — startup spinner removed / faster YouTube start (2026-09-25)
- Built from the tested 1.10.91 → 1.10.94 → 1.10.95 chain; Mobile only.
- Removed the indeterminate YouTube loading spinner during startup. The player now stays visually clean while the native black shutter handles the transition.
- Reduced the ExoPlayer initial playback threshold from 250 ms to 150 ms to reduce time-to-first-play. The rebuffer threshold remains unchanged.
- No changes to IPTV, live chat, avatar, status bar, navigation, or mini-player behavior.
- Runtime behavior still requires device testing; CI verification only proves the patch is structurally applied.
\n\n## 1.10.97 — hard-disable YouTube spinner (2026-09-25)\n- Built on the tested 1.10.91 playback/format path plus v94/v95/v96 handoff fixes.\n- Root cause of the persistent black spinner identified: SmartTube's `showProgressBar(boolean)` callback can re-enable `mProgressBar` after `setVideo()`.\n- v1.10.97 overrides that callback on Mobile so the indeterminate spinner is always hidden.\n- No TV changes and no speculative network/buffer changes.\n- Runtime device test remains required.\n


# CẬP NHẬT TIẾN ĐỘ — 1.10.102 — 2026-09-27

## Phản hồi test thực tế mới nhất của người dùng

Người dùng đã cài bản build mới và xác nhận **các lỗi mục tiêu vẫn chưa được xử lý dứt điểm**:

1. **Avatar video YouTube:** vẫn hiển thị icon mặc định thay vì ảnh avatar thực tế. Chưa coi pipeline avatar là PASS.
2. **Status bar / player portrait:** Android status bar vẫn đè lên vùng player khi mở video. Người dùng xác nhận **1.10.97 trước đây đã xử lý đúng lỗi này**; các bản mới phải khôi phục đúng behavior đó, không tạo regression.
3. **Tốc độ mở video:** YouTube video vẫn tải chậm.
4. **Browse / vuốt chuyển tab:** trang YouTube vẫn lag/chậm khi vuốt chuyển tab.
5. **Spinner:** vòng tròn đen/spinner vẫn xuất hiện khi mở video, dù v1.10.97 đã có cơ chế hard-disable.

## Source hiện tại

Branch: `fix/mobile-1.10.79-youtube-performance`

Version:
- versionName: **1.10.102**
- versionCode: **118**

Commit mới nhất:
- `c676fec63965d6933fda6572ff827ce9466e1cbf`
- `fix(mobile): final YouTube status avatar spinner and feed performance correction`

Chuỗi Mobile hiện tại vẫn chạy patch v84 → v97 trước performance patch. Tuy nhiên test máy thật cho thấy source/verifier PASS không đồng nghĩa runtime PASS. Vòng tiếp theo phải kiểm tra **generated runtime source sau toàn bộ patch chain** và đối chiếu trực tiếp với behavior của 1.10.97.

## Nguyên tắc vòng sửa tiếp theo

- Không sửa đoán.
- Dùng **1.10.97 làm behavioral reference** cho avatar, status bar/player portrait và spinner.
- Không lấy 1.10.98/1.10.99 làm behavioral reference cho các phần này.
- Không đưa lại các thay đổi v98/v99 đã gây regression.
- Performance chỉ tập trung vào YouTube video load, Browse/tab loading, swipe responsiveness, network/data scheduling và RecyclerView/render workload.
- Performance patch không được thay đổi avatar/status-bar/player layout.
- Mỗi vòng phải kiểm tra source trước patch, generated source sau patch, verifier, CI build và artifact trước khi bàn giao APK.
- Không đánh dấu lỗi PASS chỉ dựa trên build thành công; runtime device test là điều kiện bắt buộc.

## Trạng thái

**1.10.102: test máy thật FAIL đối với các mục tiêu avatar, status bar, spinner, video load và Browse/swipe performance.**

Ưu tiên vòng sửa tiếp theo:
**status bar → avatar → spinner → video load → Browse/swipe performance**.

Bản **1.10.97** được giữ làm mốc behavior cho các lỗi UI đã từng được người dùng xác nhận là đã sửa.


# BUILD UPDATE — 1.10.102 — 2026-09-27

## GitHub Actions

- Workflow: **NM7 Mobile Final Build #731**
- Trigger commit: c676fec63965d6933fda6572ff827ce9466e1cbf
- Kết quả: **SUCCESS**
- Tổng thời gian: khoảng **10m 7s**
- Artifacts: **3**
- Artifact chính: NM7-IPTV-Mobile-FINAL

## Trạng thái

**1.10.102 đã build thành công.** Đây là xác nhận về quá trình build/packaging, **không phải xác nhận runtime**. Các lỗi avatar, status bar/player, spinner, YouTube video load và Browse/swipe vẫn phải được người dùng test lại trên thiết bị thật trước khi đánh dấu PASS.

c676fec tiếp tục là mốc source của bản build này.


# HANDOFF — 2026-09-27 — CHUYỂN SANG PHIÊN CHAT MỚI

## Trạng thái thực tế phải giữ nguyên

Current branch:
`fix/mobile-1.10.79-youtube-performance`

Current version:
- **1.10.102**
- versionCode **118**

Current HEAD:
- `475ff88ee05fd58abcd0616ad04c487534c3f88d`
- `docs(mobile): record 1.10.102 regression test results and 1.10.97 reference`

## Kết quả test thiết bị mới nhất

**1.10.102 FAIL.** Không được coi build thành công là đã xử lý xong.

Người dùng xác nhận:
1. Avatar video YouTube vẫn là **icon mặc định**.
2. Status bar Android vẫn **đè lên player** khi mở video.
3. Video YouTube vẫn **load chậm**.
4. Vuốt/chuyển tab trong Browse YouTube vẫn **lag/chậm**.
5. Vòng tròn đen/spinner vẫn **xuất hiện khi mở video**.

## Behavioral reference bắt buộc

**1.10.97** là mốc người dùng đã xác nhận:
- Avatar hiển thị đúng.
- Status bar/player portrait đúng, không bị đè.
- Spinner đã được hard-disable.

Vì vậy vòng tiếp theo phải phục hồi đúng behavior của 1.10.97 trước khi tiếp tục tối ưu performance.

**Không sử dụng 1.10.98/1.10.99 làm behavioral reference** cho ba phần avatar/status-bar/spinner vì các bản đó đã tạo regression.

## Phân tách phạm vi sửa

### A. UI regression — phải khôi phục từ 1.10.97
- Generated `PlaybackActivity.java`: system-window/insets/status-bar path.
- Generated `VideoItem` → `YouTubeMediaItem` → `Video` → `VideoCardHolder`: avatar propagation/render.
- Generated `showProgressBar(boolean)` và startup ProgressBar lifecycle.

### B. Performance — sửa riêng, không chạm A
- YouTube video load / time-to-first-frame.
- Browse page load.
- Swipe giữa các tab/section.
- Network/data scheduling.
- RecyclerView/render workload.

## Các patch quan trọng cần đối chiếu

- v84: parser avatar + initial poster/status-bar.
- v85: avatar propagation vào `YouTubeMediaItem`.
- v86: hard portrait system bars + avatar metadata fallback.
- v87–v90: status/poster/avatar transition corrections.
- v91: smooth open/feed priority.
- v94–v95: poster/player transition.
- v96: startup spinner hide.
- v97: **hard-disable `showProgressBar(boolean)`**.

Generated source sau toàn bộ patch chain phải được kiểm tra, không chỉ kiểm tra script/verifier.

## Nguyên tắc bàn giao

- Mobile only. **Không thêm Android TV code/UI.**
- Không đoán code khi chưa kiểm tra generated source.
- Không đánh dấu PASS dựa riêng trên CI.
- Không giao APK mới cho tới khi CI build thành công và artifact đúng version.
- Runtime device test là điều kiện bắt buộc để xác nhận avatar/status-bar/spinner/performance.
- Phiên chat mới phải đọc `README.md` và phần HANDOFF này trước khi sửa tiếp.


# CẬP NHẬT TIẾN ĐỘ — 1.10.103 — 2026-09-27

## Phản hồi test 1.10.102 đã được người dùng xác nhận

Cập nhật lại trạng thái 1.10.102 theo test thực tế mới nhất:
- **Avatar YouTube:** đã xử lý được.
- **Status bar/player portrait:** đã xử lý được.
- **Spinner đen:** đã xử lý được.

Không tiếp tục thay đổi ba đường này trong vòng performance 1.10.103.

## Lỗi còn lại
1. **YouTube video load còn chậm.**
2. **Browse/vuốt chuyển trang YouTube còn chậm/lag.**
3. **Video 4K rất lag/giật**, theo video test `219395.mp4` người dùng gửi.

## Phân tích video test
Video `219395.mp4` cho thấy nội dung 4K vẫn phát được nhưng chuyển động có hiện tượng không mượt/giật trong player. Vì vậy vòng sửa mới không coi đây là lỗi UI poster/status-bar; trọng tâm chuyển sang đường render/decoder SurfaceView và khả năng phản ứng khi decoder rơi frame.

## 1.10.103 — source correction
### Browse/swipe
- Cache hit của row không chạy lại `BrowseProcessorManager`.
- Thêm grid cache nhẹ nhưng chỉ dùng khi MediaGroup đã hết continuation page, tránh bỏ sót dữ liệu.
- TTL cache: 45 giây; refresh sẽ xóa cache.

### Video startup
- Rút ngắn gate poster từ nhiều rendered buffer xuống 1 rendered buffer mới và giảm thời gian chờ sau first frame.
- Vẫn yêu cầu decoder render frame mới trước khi chuyển sang video thật; không thay đổi avatar/status bar/spinner.

### 4K/high-FPS
- Generated `DebugInfoMediaCodecVideoRenderer.java` nhận diện 4K >=50fps và tránh SurfaceView timestamp pacing trong trường hợp này.
- PlaybackActivity theo dõi `DecoderCounters.droppedBufferCount` sau khi 4K đã render.
- Nếu decoder rơi ít nhất 12 frame trong cửa sổ 2,5 giây, app sẽ hạ trần tạm thời xuống 2560×1440 và bỏ selection override video cũ để giữ playback liên tục.
- Nếu 4K không rơi frame, không hạ chất lượng.

## Version/source
- versionName: **1.10.103**
- versionCode: **119**
- Branch: `fix/mobile-1.10.79-youtube-performance`
- Patch commit: `a799ece305f4559139c63e462957591254115843`
- v37 chain commit: `73ad6e09951d12452a4ec5f03783fdc3da836514`
- Version bump: `c8a659d8a68405c29856a63f56a9df64ffd4131e`
- CI workflow update: `ed4ea6f5cd7e1fc8c787e820719f1d2bbf13ec31`
- Subsequent 1.10.103 patch refinements:
  - `d925794eef92b07034c1346089c3aa0facd28a78`
  - `0b021eba3b7333f714292ae0dcb5fe019d03d8bf`

## Điều kiện xác nhận
1. GitHub Actions build thành công.
2. Generated source sau toàn bộ patch chain đúng.
3. APK artifact đúng versionCode 119.
4. Người dùng test trên thiết bị thật cùng điều kiện với 1.10.102.
5. Không đánh dấu PASS chỉ dựa trên CI.

**Trạng thái hiện tại: source 1.10.103 đã sẵn sàng trong build chain; đang chờ CI và test máy thật.**


## CI WATCH — 1.10.103 — 2026-09-27 02:40 UTC

### Run #741–#745: fail-fast source-patch failures
Các run 741–745 đều dừng ở bước **Patch SmartTube Mobile UI and playback lifecycle**, trước khi Gradle build:
- #741–#744: các iteration đầu của v103 chưa khớp generated source/patch-chain.
- #745 (`44605423...`): lỗi cụ thể là anchor **4K watchdog fields** không tồn tại ở generated PlaybackActivity sau v67/v68; sau đó đã sửa anchor.
- Các run này **không tạo APK**.

### Run #746 — đang build
- Run: **NM7 Mobile Final Build #746**
- Run ID: `36289079120`
- Commit: `20ecf01ad75dd304740e98b6d357cc21eddff49b`
- Bước patch SmartTube: **SUCCESS**
- Verifier 1.10.103: **SUCCESS**
- Lifecycle regression guards: **SUCCESS**
- Generated lifecycle source artifact step: **SUCCESS**
- SmartTube compatibility/resource patches: **SUCCESS**
- Hiện đang ở bước **Build Mobile only**; chưa có APK cho đến khi bước này hoàn tất.

Đây là lần đầu vòng 1.10.103 vượt qua toàn bộ patch/verifier và đi vào Gradle build. Chưa đánh dấu PASS cho đến khi build + artifact hoàn tất và người dùng test máy thật.
