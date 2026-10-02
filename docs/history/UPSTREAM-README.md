# MindTrigger Assist v16.2

Biến thao tác nhấn giữ của ColorOS thành Circle to Search hoặc gọi Trợ lý
Android. Bản này dành chủ yếu cho OPPO Find X7 / ColorOS 16 China.

## 🎬 Setup nhanh nhất — xem video bên dưới

Video hướng dẫn chính thức sẽ được gắn tại đây. Hiện chưa có link/video nguồn
để nhúng; trong lúc chờ, cứ làm đúng sáu bước ngắn bên dưới.

## Cần chuẩn bị

- MindTrigger Assist v16.2;
- ứng dụng Shizuku đã cài và đang chạy;
- Google hoặc Gemini đã cài nếu muốn gọi Assistant;
- máy đã bật Wireless debugging hoặc kết nối PC để khởi động Shizuku.

Không cần root. Shizuku chỉ cần ở lúc setup; sau đó MindTrigger chạy độc lập.

## Cài trong 3 phút

1. Mở **Shizuku** và khởi động nó bằng Wireless debugging hoặc PC.
2. Mở **MindTrigger Assist**. Đồng ý điều khoản rồi bấm **Run setup with
   Shizuku** ở trang đầu.
3. Khi Shizuku hỏi quyền, bấm **Allow**. Chờ bảng kết quả hiện `SUCCESS`.
4. Làm các mục ColorOS mà wizard hiển thị:
   - bật **Touch and hold gesture guide bar to wake Breeno**;
   - khóa MindTrigger Assist trong **Recent tasks**;
   - bật **Auto launch** cho Google/Gemini.
5. Bấm **Run MindTrigger Assist**. Khi Android hiện hộp thoại quyền đọc log,
   hãy bấm **Allow**.
6. Test:
   - nhấn giữ Home/cử chỉ → Circle to Search;
   - nhấn giữ Power → Assistant.

Xong. Notification của MindTrigger phải còn hiển thị khi runtime đang chạy.

## Nếu bạn không dùng Shizuku

Trong bước 1, bấm **Copy ADB one-shot**. Kết nối điện thoại với máy tính, chạy
lệnh mà app vừa copy, rồi quay lại MindTrigger Assist và tiếp tục các bước
ColorOS. Không cần giữ ADB hoặc Shizuku chạy sau khi setup xong.

## Những mục ColorOS bắt buộc

ColorOS không cho app đọc đáng tin cậy các lựa chọn OEM này, nên bạn phải tự
bật rồi xác nhận trong wizard:

| Mục | Vì sao cần |
| --- | --- |
| Gesture Breeno | Đây là tín hiệu nhấn giữ mà MindTrigger dùng. |
| Lock trong Recent tasks | **Clear all** sẽ không quét watcher của MindTrigger. |
| Auto launch Google/Gemini | Assistant đỡ bị chết nền sau một thời gian. |
| Hiển thị trên ứng dụng khác | Giúp watcher giữ được recovery path của ColorOS. |

Ở ColorOS 16.0.7, đọc cảnh báo đỏ ở cuối tab **Thiết lập**. Nếu bạn đã tự tắt
giám sát quyền / System Optimization theo hướng dẫn của mình thì có thể bỏ qua
cảnh báo đó.

## Khi đang chạy mà không còn nhận Home / Power

1. Mở MindTrigger Assist một lần.
2. Nếu Android hiện hộp thoại quyền đọc log, bấm **Allow**.
3. Vào tab **Thiết lập** và kiểm tra trạng thái *phiên đọc log*, không chỉ nhìn
   dòng `READ_LOGS`.
4. Nếu đã bật tile **Quick Settings log recovery** trong tab Beta, bạn cũng có
   thể bấm tile đó từ bảng Cài đặt nhanh để yêu cầu phục hồi phiên log.

`READ_LOGS` được cấp vẫn chưa chắc phiên đọc log còn sống. Notification foreground
cũng chưa đủ để chứng minh reader còn hoạt động; trạng thái phiên log mới là thứ
quan trọng.

## Lưu ý quan trọng trước khi bấm setup

Lần setup Shizuku đầu tiên có thể gỡ `com.heytap.speechassist` và
`com.coloros.colordirectservice` khỏi user 0 để nhường đường cho trigger. APK hệ
thống gốc không bị xóa khỏi phân vùng system, nhưng đây vẫn là thay đổi thiết bị
thật. Đọc bảng lệnh/kết quả trong app và chỉ tiếp tục khi bạn đồng ý.

MindTrigger không có Device Admin, Accessibility service, child-process logcat,
runtime Shizuku, hoặc permanent WakeLock.

## Beta

Tab **Beta** có animation, voice wake, âm báo CTS/Assistant tối đa 3 giây, đổi
mapping CTS ↔ Google Assistant và tile khôi phục log. Những mục này là tùy chọn;
setup cơ bản không cần bật.

## Dành cho người build source

```sh
./gradlew assembleRelease
```

- package: `dev.evoker.homeholdcts`
- minSdk: 32
- targetSdk / compileSdk: 36
- Java: 17
- APK release: ký bằng APK Signature Scheme v3

Release cần keystore thật qua `keystore.properties`. Xem [SIGNING.md](SIGNING.md).

## License và nguồn

MindTrigger Assist phát hành theo **GPL-3.0-only**. Xem [LICENSE](LICENSE),
[NOTICE.md](NOTICE.md), [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) và
[SOURCE_PROVENANCE.md](SOURCE_PROVENANCE.md).

Repository: <https://github.com/evokermc098-coder/MindTriggerAssist>
