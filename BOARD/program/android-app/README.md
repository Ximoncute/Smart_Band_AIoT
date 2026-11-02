# Smartband Care - Ứng Dụng Android Native (Kotlin + Jetpack Compose)

Dự án ứng dụng di động giám sát y tế, cảnh báo té ngã khẩn cấp & định vị GPS thời gian thực cho vòng đeo thông minh Smartband IoT (ESP32-S3 + Cảm biến 6 trục BMI160).

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)
- **Ngôn ngữ**: Kotlin 2.0+
- **Giao diện**: Jetpack Compose (Material 3)
- **IDE**: Android Studio (Ladybug / Koala / Hedgehog)
- **Kiến trúc**: Clean Architecture + MVVM (StateFlow, Coroutines)
- **Bản đồ**: Google Maps Compose (`com.google.maps.android:maps-compose`)
- **Điều hướng**: Navigation Compose

---

## 📱 Luồng Màn Hình & Tính Năng Hoàn Chỉnh

### 1. Luồng Khởi Động Onboarding 3 Tabs (Trước Login)
- **Tab 1: Sức Khỏe & Rủi Ro**: Cảnh báo tỷ lệ té ngã ở người già >30%/năm, rối loạn nhịp vận động, đi lạc do suy giảm trí nhớ.
- **Tab 2: Phần Cứng Smartband**: Cảm biến gia tốc 6 trục BMI160 lấy mẫu 50Hz, vi điều khiển ESP32-S3, định vị GPS + SIM 4G LTE độc lập, màn hình OLED 0.96 inch, phím cứng SOS.
- **Tab 3: Dịch Vụ Bảo Vệ 24/7**: Còi hú khẩn cấp, khoanh vùng an toàn Geofence 250m, gửi lịch nhắc thuốc tự động.
- Nút **"Bỏ qua"** góc trên, thanh chỉ số bước (1/3, 2/3, 3/3), nút **"Tiếp theo"** và ở Tab 3 đổi thành **"Đăng Nhập / Đăng Ký →"**.

### 2. Xác Thực (Authentication)
- Màn hình Đăng nhập (Gmail + Mật khẩu).
- Màn hình Đăng ký tài khoản.
- Nút **"← Xem lại thông tin sức khỏe & thiết bị"** cho phép quay lại Onboarding bất kỳ lúc nào.

### 3. Hệ Thống 5 Tab Chính (Bottom Navigation)
1. **Trang chủ (Dashboard)**: 4 thẻ chỉ số nhanh (Hành vi AI, Vị trí GPS, An toàn, Pin 4G), bản đồ thu nhỏ, nhật ký cảnh báo gần nhất.
2. **Định vị (Live Tracking)**: Google Maps Compose toàn màn hình, vòng tròn Geofence 250m, nút chuyển đổi Bản đồ đường sá / Vệ tinh, thông số tốc độ di chuyển và số vệ tinh GPS.
3. **Hành vi AI (Behavior AI)**: Phân loại hoạt động thời gian thực (Đi bộ, Đứng, Ngồi, Ngã) kèm % tin cậy AI, **đồ thị sóng Compose Canvas vẽ tín hiệu gia tốc 3 trục (Ax, Ay, Az) 50Hz**.
4. **Cảnh báo (Alerts Center)**: Banner còi hú màu đỏ khi có sự cố, nút 1 chạm **"Gọi cứu hộ"** quay số người giám hộ, nút xác nhận đã an toàn.
5. **Hồ sơ & Nhắc thuốc (Profile & Reminders)**: Quản lý bệnh lý người thân; form đặt lịch uống thuốc tự động lọc tiếng Việt không dấu; **mô phỏng màn hình OLED monochrome 0.96 inch vi điều khiển**.

### 4. Bảng Giả Lập Sự Kiện (Demo Controller Sheet)
- Bấm nút tia sét ⚡ ở góc trên màn hình để mở bảng điều khiển:
  - 🚨 Kích hoạt Giả lập Té ngã khẩn cấp
  - 🆘 Kích hoạt Giả lập Nút bấm SOS
  - 📍 Kích hoạt Bước di chuyển GPS ngẫu nhiên
  - 🚶 Đổi hoạt động AI: Đi bộ / Đứng yên / Đang ngồi / Chạy
  - 📶 Bật/Tắt trạng thái thiết bị 4G Online/Offline

---

## 🚀 Hướng Dẫn Mở Dự Án Bằng Android Studio
1. Mở **Android Studio**.
2. Chọn **Open** (hoặc *File -> Open*).
3. Trỏ tới thư mục: `FE/android-app`.
4. Chờ Android Studio đồng bộ Gradle (**Gradle Sync**).
5. (Tùy chọn) Thêm Google Maps API Key vào file `app/src/main/AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_REAL_GOOGLE_MAPS_API_KEY" />
   ```
6. Chọn máy ảo Android Emulator hoặc cắm điện thoại thật, nhấn **Run 'app' (Shift + F10)**.
