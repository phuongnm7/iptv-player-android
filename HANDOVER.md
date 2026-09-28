# Tài liệu bàn giao: Tối ưu YouTube & Tinh chỉnh Mobile Playback (NM7 IPTV)

## 1. Trạng thái hiện tại
- **Mục tiêu**: Tối ưu module YouTube (dựa trên SmartTube-droid) trên thiết bị di động: xóa bỏ hiện tượng màn hình đen khi mở video/đổi video liên quan, giải quyết triệt để vấn đề đứng hình và lệch tiếng khi phát video 4K.
- **Workflow CI/CD chính**: `.github/workflows/android-mobile-final.yml` (Task: `:app:assembleMobileDebug`).
- **Script can thiệp chính**: `scripts/patch-mobile-v106.py` (chạy ngay sau `scripts/patch-mobile-v37.py`).

---

## 2. Các vấn đề biên dịch đã xử lý triệt để
- **Lỗi dependency testutils**: Khắc phục lỗi `androidx.test.ext:junit:null` và `truth:null` bằng cách tiêm cứng version (`junitXVersion=1.1.5`, `truthXVersion=1.5.0`) vào `gradle.properties` và thay thế regex trong các file `.gradle`.
- **Lỗi thiếu symbol chuỗi**: Khắc phục `cannot find symbol: variable section_is_empty` trong `PlaybackActivity.java` bằng cách thay trực tiếp bằng chuỗi `"Section is empty"`.
- **Lỗi trùng lặp biến**: Khắc phục `variable NM7_FORMAT_REUSE_MS is already defined` trong `YouTubeMediaItemService.java`.
- **Lỗi truy cập thuộc tính**: Khắc phục lỗi `sNm7TransitionPoster has private access` và lỗi duplicate method trong `VideoCardHolder.java`.

---

## 3. Kiến trúc tối ưu đã cấu hình trong `patch-mobile-v106.py`
1. **Xóa bỏ màn hình đen (`exo_shutter`)**:
   - Vô hiệu hóa màn đen che bề mặt của ExoPlayer bằng cách chuyển toàn bộ background của view `exo_shutter` sang `@android:color/transparent`.
   - Vô hiệu hóa lệnh ép hiển thị màn đen (`shutterView.setVisibility(VISIBLE)`) trong `PlayerView.java`.
2. **Tối ưu luồng phát và chống drop frame 4K**:
   - Cấu hình lại `DefaultLoadControl`: Nâng bộ đệm RAM lên 128MB (`128 * 1024 * 1024`), thời lượng buffer giữ ở mức 35s - 90s.
   - Buffer khởi động phát (`bufferForPlaybackMs`) đặt ở mức 2500ms để âm thanh và khung hình 4K nạp đủ keyframe đầu tiên trước khi phát cùng lúc từ giây 0.0.
   - Nới lỏng ngưỡng cho phép trễ khung hình của `MediaCodecVideoRenderer` (`earlyUs < -300000`) nhằm ngăn trình phát tự động hủy (drop) khung hình video khi luồng tiếng chạy trước.
3. **Cơ chế nạp thumbnail chuyển cảnh**:
   - Bắt sự kiện chạm ngón tay (`ACTION_DOWN`) trên `itemView` của `VideoCardHolder` để nạp `Bitmap` thumbnail vào bộ nhớ đệm trước khi mở Activity.

---

## 4. Các đầu việc người tiếp theo cần xử lý tiếp
1. **Kiểm tra đồng bộ khi đổi video đề xuất trong `PlaybackActivity`**:
   - Khi bấm đổi video liên quan ở danh sách bên dưới, `PlaybackActivity` không khởi tạo lại `onCreate` mà tái sử dụng instance.
   - Cần kiểm tra vòng đời của `SurfaceView` / `TextureView` trong Leanback player để đảm bảo khi nhận intent/luồng phát mới, view không bị chớp đen trước khi khung hình mới được render.
2. **Kiểm thử thực tế bộ giải mã 4K trên thiết bị**:
   - Kiểm tra xem chip máy có hỗ trợ giải mã phần cứng cho codec AV1 4K hay chỉ hỗ trợ VP9/H.264. Nếu máy yếu bị drop frame do chip, cân nhắc thêm logic fallback sang độ phân giải 1440p hoặc 1080p60fps đối với luồng AV1.
3. **Lưu ý về hạ tầng CI/CD**:
   - Giữ kho lưu trữ ở chế độ **Public** để không bị giới hạn 2.000 phút GitHub Actions miễn phí của tài khoản cá nhân.
