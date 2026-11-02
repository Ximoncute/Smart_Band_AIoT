# Smartband backend

Backend cần Python và MongoDB cục bộ. Cài gói Python:

```powershell
cd backend
python -m pip install -r requirements.txt
python server.py
```

Trên Windows có MongoDB Server 8.2, có thể chạy `powershell -File backend/start.ps1`
từ thư mục gốc; script sẽ mở MongoDB cục bộ nếu chưa chạy.

Firmware gửi `POST /api/v1/telemetry`. App đọc:

- `GET /api/v1/devices/smartband-001/snapshot`
- `GET /api/v1/route`
- `GET /api/v1/alerts`

Dữ liệu cảm biến được lưu ở `backend/data/*.jsonl`. Tài khoản, hồ sơ,
hash mật khẩu, phiên đăng nhập và OTP nằm trong MongoDB database `smartband`.

Mở `http://localhost:8787/admin` để quản trị. Khóa quản trị được tạo tự động
trong `backend/data/admin.key`; không đưa file này lên Git. MongoDB chỉ cần
nghe tại `127.0.0.1:27017`. Có thể đặt `MONGODB_URI` để dùng địa chỉ khác.

Sau mỗi lần mở máy, chạy `setup-and-run.bat` tại thư mục `BOARD`. Khi backend
đang chạy, mở `http://localhost:8787/admin` trên máy backend, đọc khóa trong
`backend/data/admin.key`, dán vào ô khóa quản trị và bấm tải dữ liệu. Trang này
hiển thị số tài khoản, từng user, hồ sơ, trạng thái khóa, và dữ liệu thiết bị.
MongoDB giữ tài khoản/hồ sơ; các file JSONL trong `backend/data/` giữ lịch sử
telemetry, cảnh báo và điểm dừng. Không xóa `backend/data/` nếu muốn giữ lịch sử
và khóa quản trị. Thư mục MongoDB nằm ở `%LOCALAPPDATA%\SmartbandMongo`.

Firmware gửi HTTP/JSON qua Wi-Fi. App cấu hình Wi-Fi và địa chỉ backend cho
kit qua BLE. Mỗi lần đổi mạng, nhập IP backend mới trong Hồ sơ rồi gửi lại.

FormSubmit hiện được gọi từ app để chuyển OTP do backend tạo. Đây là cơ chế
demo: mã đi qua app nên chưa phù hợp để bảo vệ tài khoản triển khai công khai.
Muốn triển khai thật, cấu hình dịch vụ email hoặc SMTP ở backend để mã OTP
không bao giờ đi qua client.
