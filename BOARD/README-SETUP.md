# Chay du an tren Windows

Chay `setup-and-run.bat` trong thu muc `BOARD`. Can Internet lan dau de tai
Python 3.10, MongoDB Server, JDK 17, Android SDK command-line tools, Gradle
va cac thu vien. Khong can Android Studio. Ket noi dien thoai bang USB va bat
USB debugging; chap nhan thong bao cho phep tren dien thoai.

Script build APK tu ma nguon, cai APK vao dien thoai da duoc ADB cho phep,
khoi dong backend o cong 8787. Giu cua so script mo khi su dung app. Neu may
da chay backend, script se giu tien trinh hien co.

Mo `http://localhost:8787/admin` tren may backend. Khoa quan tri nam trong
`backend/data/admin.key`. Day la khoa rieng cua may, khong day len GitHub.
Du lieu user nam trong MongoDB tai `%LOCALAPPDATA%\SmartbandMongo`; lich su
thiet bi nam trong `backend/data/*.jsonl`.

Firmware ESP32-S3 duoc build/nap rieng bang PlatformIO trong thu muc `BOARD`.
APK va firmware khong duoc luu vao Git; script se tai lai thu vien va build.
Ket noi LAN tu app/kit den backend can IP may backend hien tai, nhap trong
Ho so > Thiet bi Smartband.
