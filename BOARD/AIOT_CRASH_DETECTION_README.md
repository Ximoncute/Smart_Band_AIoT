# AIoT Crash Detection - ESP32-S3 + BMI160

## Giai thich nhanh

TinyML la AI nho chay truc tiep tren vi dieu khien. Trong project nay model khong gui du lieu len Internet, ma chay ngay tren ESP32-S3.

Sliding Window la cua so truot: thay vi dua tung dong `Ax Ay Az Gx Gy Gz` vao AI, firmware gom 100 mau lien tiep, tuong duong khoang 2 giay o 50 Hz.

Feature la dac trung tinh tu cua so du lieu, vi du gia toc dinh, gyro dinh, do doi pitch/roll, ty le gan nhu dung yen.

Inference la luc ESP32-S3 dua feature vao model AI de lay xac suat `normal`, `impact`, `crash`.

State Machine la cach chia chuong trinh thanh cac trang thai ro rang: `NORMAL`, `POSSIBLE_IMPACT`, `ANALYZING`, `USER_CONFIRMATION`, `EMERGENCY`, `RECOVERY`.

Dataset la tap du lieu cam bien that de train AI. Label la nhan cua du lieu, ban dau gom `normal`, `impact`, `crash`.

## GPIO da co dinh

```text
BMI160 SDA = GPIO6
BMI160 SCL = GPIO7
OLED   SDA = GPIO9
OLED   SCL = GPIO8
BUZZER     = GPIO5
BUTTON     = GPIO4
```

Tat ca nam trong `include/config.h`.

## Cach chay tung giai doan

1. Build firmware:

```bash
pio run
```

2. Khi can nap code, cam kit ESP32-S3 roi chay:

```bash
pio run -t upload
```

3. Mo Serial Monitor:

```bash
pio device monitor -b 115200
```

Firmware se in `Ax Ay Az Gx Gy Gz`, gia toc tong, gyro tong, AI prediction, crash score va state hien tai.

## Thu dataset khong copy thu cong

Ban van can tao chuyen dong that cho tung label, nhung khong can chep tung dong tu Serial Monitor.

Vi du thu 30 giay binh thuong:

```bash
python tools/collect_dataset.py --port COM5 --label normal --seconds 30 --out data/raw/normal_01.csv
```

Thu impact:

```bash
python tools/collect_dataset.py --port COM5 --label impact --seconds 30 --out data/raw/impact_01.csv
```

Thu crash mo phong an toan:

```bash
python tools/collect_dataset.py --port COM5 --label crash --seconds 30 --out data/raw/crash_01.csv
```

## Train AI

Sau khi co CSV that:

```bash
python tools/train_tinyml_model.py --data data/raw --out include/generated_model.h
```

Script se in accuracy, confusion matrix va kich thuoc model uoc tinh. Sau do build/nap lai firmware, ESP32-S3 se dung model da train trong `include/generated_model.h`.

## Luu y an toan

Khong tao va cham nguy hiem cho nguoi hoac xe that trong giai doan thu dataset. Label `crash` o prototype nen la mo phong an toan: tac dong manh vao thiet bi duoc bao ve, xoay doi tu the lon, roi de yen tren ban/mat phang mem.
