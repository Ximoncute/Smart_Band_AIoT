import argparse
import csv
import pathlib
import sys
import time

try:
    import serial
except ImportError:
    print("Thieu pyserial. Cai bang: python -m pip install pyserial", file=sys.stderr)
    raise


HEADER = ["timestamp", "ax", "ay", "az", "gx", "gy", "gz", "label"]


def parse_args():
    parser = argparse.ArgumentParser(description="Thu dataset that tu firmware AIoT Crash Detection.")
    parser.add_argument("--port", required=True, help="Cong serial, vi du COM5")
    parser.add_argument("--baud", type=int, default=115200)
    parser.add_argument("--label", required=True, choices=["normal", "impact", "crash"])
    parser.add_argument("--seconds", type=float, default=30.0)
    parser.add_argument("--out", required=True, help="File CSV dau ra")
    return parser.parse_args()


def is_csv_sample(row):
    if len(row) != 8:
        return False
    if row[0] == "timestamp":
        return False
    try:
        int(row[0])
        for value in row[1:7]:
            float(value)
    except ValueError:
        return False
    return row[7] in {"normal", "impact", "crash"}


def main():
    args = parse_args()
    output_path = pathlib.Path(args.out)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    duration_ms = int(args.seconds * 1000)

    with serial.Serial(args.port, args.baud, timeout=1) as ser:
        time.sleep(2.0)
        ser.reset_input_buffer()
        ser.write(f"dataset {args.label} {duration_ms}\n".encode("utf-8"))
        ser.flush()

        rows_written = 0
        started = time.time()
        with output_path.open("w", newline="", encoding="utf-8") as fp:
            writer = csv.writer(fp)
            writer.writerow(HEADER)
            while time.time() - started < args.seconds + 3:
                line = ser.readline().decode("utf-8", errors="replace").strip()
                if not line:
                    continue
                row = [item.strip() for item in line.split(",")]
                if is_csv_sample(row):
                    writer.writerow(row)
                    rows_written += 1
                elif line.startswith("[DATASET] STOP"):
                    break
                else:
                    print(line)

    print(f"Da luu {rows_written} mau vao {output_path}")


if __name__ == "__main__":
    main()
