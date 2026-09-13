import argparse
import csv
import math
import pathlib
import random
import sys

try:
    import numpy as np
except ImportError:
    print("Thieu numpy. Cai bang: python -m pip install numpy", file=sys.stderr)
    raise


LABELS = ["normal", "impact", "crash"]
WINDOW_SIZE = 100
STRIDE = 25
FEATURE_NAMES = [
    "acc_mean",
    "acc_std",
    "acc_peak_linear",
    "gyro_mean",
    "gyro_peak",
    "orientation_delta",
    "inactivity_ratio",
    "jerk_peak",
]


def parse_args():
    parser = argparse.ArgumentParser(description="Train TinyML model tu dataset BMI160 CSV.")
    parser.add_argument("--data", required=True, help="Thu muc chua CSV hoac mot file CSV")
    parser.add_argument("--out", default="include/generated_model.h")
    parser.add_argument("--epochs", type=int, default=900)
    parser.add_argument("--lr", type=float, default=0.06)
    parser.add_argument("--seed", type=int, default=7)
    return parser.parse_args()


def magnitude3(x, y, z):
    return math.sqrt(x * x + y * y + z * z)


def pitch_roll(ax, ay, az):
    pitch = math.atan2(-ax, math.sqrt(ay * ay + az * az)) * 180.0 / math.pi
    roll = math.atan2(ay, az) * 180.0 / math.pi
    return pitch, roll


def extract_features(window):
    acc = []
    gyro = []
    pitch = []
    roll = []
    jerk_peak = 0.0
    inactive = 0
    previous = None

    for sample in window:
        ts, ax, ay, az, gx, gy, gz, _label = sample
        acc_mag = magnitude3(ax, ay, az)
        linear = abs(acc_mag - 1.0)
        gyro_mag = magnitude3(gx, gy, gz)
        p, r = pitch_roll(ax, ay, az)
        acc.append(acc_mag)
        gyro.append(gyro_mag)
        pitch.append(p)
        roll.append(r)
        if linear <= 0.18 and gyro_mag <= 35.0:
            inactive += 1
        if previous is not None:
            prev_ts, prev_acc = previous
            dt = max(1.0, ts - prev_ts) / 1000.0
            jerk_peak = max(jerk_peak, abs(acc_mag - prev_acc) / dt)
        previous = (ts, acc_mag)

    orientation_delta = math.sqrt((pitch[-1] - pitch[0]) ** 2 + (roll[-1] - roll[0]) ** 2)
    return [
        float(np.mean(acc)),
        float(np.std(acc)),
        float(max(abs(value - 1.0) for value in acc)),
        float(np.mean(gyro)),
        float(max(gyro)),
        float(orientation_delta),
        float(inactive / len(window)),
        float(jerk_peak),
    ]


def read_csv(path):
    rows = []
    with path.open("r", encoding="utf-8", newline="") as fp:
        reader = csv.DictReader(fp)
        for row in reader:
            label = row.get("label", "").strip().lower()
            if label not in LABELS:
                continue
            rows.append(
                (
                    int(float(row["timestamp"])),
                    float(row["ax"]),
                    float(row["ay"]),
                    float(row["az"]),
                    float(row["gx"]),
                    float(row["gy"]),
                    float(row["gz"]),
                    label,
                )
            )
    return rows


def load_windows(data_path):
    path = pathlib.Path(data_path)
    files = [path] if path.is_file() else sorted(path.rglob("*.csv"))
    features = []
    labels = []

    for file_path in files:
        rows = read_csv(file_path)
        for label in LABELS:
            label_rows = [row for row in rows if row[-1] == label]
            for start in range(0, max(0, len(label_rows) - WINDOW_SIZE + 1), STRIDE):
                window = label_rows[start : start + WINDOW_SIZE]
                features.append(extract_features(window))
                labels.append(LABELS.index(label))

    if not features:
        raise RuntimeError("Khong tim thay window hop le. Can CSV co header timestamp,ax,ay,az,gx,gy,gz,label")
    return np.array(features, dtype=np.float32), np.array(labels, dtype=np.int64)


def split_data(x, y, seed):
    indices = list(range(len(y)))
    random.Random(seed).shuffle(indices)
    train_end = int(len(indices) * 0.70)
    val_end = int(len(indices) * 0.85)
    train_idx = indices[:train_end]
    val_idx = indices[train_end:val_end]
    test_idx = indices[val_end:]
    return (x[train_idx], y[train_idx]), (x[val_idx], y[val_idx]), (x[test_idx], y[test_idx])


def softmax(logits):
    logits = logits - np.max(logits, axis=1, keepdims=True)
    exp_values = np.exp(logits)
    return exp_values / np.sum(exp_values, axis=1, keepdims=True)


def train(x_train, y_train, epochs, lr):
    class_count = len(LABELS)
    feature_count = x_train.shape[1]
    weights = np.zeros((class_count, feature_count), dtype=np.float32)
    bias = np.zeros((class_count,), dtype=np.float32)
    target = np.eye(class_count, dtype=np.float32)[y_train]

    for _epoch in range(epochs):
        probs = softmax(x_train @ weights.T + bias)
        grad_logits = (probs - target) / len(x_train)
        weights -= lr * (grad_logits.T @ x_train)
        bias -= lr * np.sum(grad_logits, axis=0)
    return weights, bias


def evaluate(x, y, weights, bias):
    probs = softmax(x @ weights.T + bias)
    pred = np.argmax(probs, axis=1)
    matrix = np.zeros((len(LABELS), len(LABELS)), dtype=np.int64)
    for expected, actual in zip(y, pred):
        matrix[expected, actual] += 1
    accuracy = float(np.mean(pred == y)) if len(y) else 0.0
    return accuracy, matrix


def c_array(values):
    return ", ".join(f"{float(value):.8f}f" for value in values)


def write_header(path, mean, scale, weights, bias, metrics):
    path = pathlib.Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as fp:
        fp.write("#pragma once\n\n")
        fp.write("#define AI_MODEL_HAS_TRAINED_WEIGHTS 1\n")
        fp.write(f"constexpr int GENERATED_FEATURE_COUNT = {len(FEATURE_NAMES)};\n")
        fp.write(f"constexpr int GENERATED_CLASS_COUNT = {len(LABELS)};\n")
        fp.write(f"const float GENERATED_FEATURE_MEAN[GENERATED_FEATURE_COUNT] = {{{c_array(mean)}}};\n")
        fp.write(f"const float GENERATED_FEATURE_SCALE[GENERATED_FEATURE_COUNT] = {{{c_array(scale)}}};\n")
        fp.write("const float GENERATED_WEIGHTS[GENERATED_CLASS_COUNT][GENERATED_FEATURE_COUNT] = {\n")
        for row in weights:
            fp.write(f"  {{{c_array(row)}}},\n")
        fp.write("};\n")
        fp.write(f"const float GENERATED_BIAS[GENERATED_CLASS_COUNT] = {{{c_array(bias)}}};\n\n")


def main():
    args = parse_args()
    np.random.seed(args.seed)
    x, y = load_windows(args.data)
    (x_train, y_train), (x_val, y_val), (x_test, y_test) = split_data(x, y, args.seed)

    mean = np.mean(x_train, axis=0)
    scale = np.std(x_train, axis=0)
    scale[scale < 1e-6] = 1.0
    x_train_n = (x_train - mean) / scale
    x_val_n = (x_val - mean) / scale
    x_test_n = (x_test - mean) / scale

    weights, bias = train(x_train_n, y_train, args.epochs, args.lr)
    train_acc, _train_matrix = evaluate(x_train_n, y_train, weights, bias)
    val_acc, _val_matrix = evaluate(x_val_n, y_val, weights, bias)
    test_acc, test_matrix = evaluate(x_test_n, y_test, weights, bias)

    metrics = {
        "train_accuracy": train_acc,
        "val_accuracy": val_acc,
        "test_accuracy": test_acc,
    }
    write_header(args.out, mean, scale, weights, bias, metrics)

    print(f"Windows: {len(y)}")
    print(f"Model bytes uoc tinh: {(weights.size + bias.size + mean.size + scale.size) * 4}")
    print(f"Train accuracy: {train_acc:.3f}")
    print(f"Validation accuracy: {val_acc:.3f}")
    print(f"Test accuracy: {test_acc:.3f}")
    print("Confusion matrix test rows=true cols=pred normal,impact,crash:")
    print(test_matrix)
    print(f"Da sinh model: {args.out}")


if __name__ == "__main__":
    main()
