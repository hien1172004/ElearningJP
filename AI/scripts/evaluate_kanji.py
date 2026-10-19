"""
ElearningJP AI - Kanji Handwriting Model Evaluation & Benchmark Script
Evaluates:
- Top-1 Accuracy
- Top-3 Accuracy
- Top-5 Accuracy
- Average Inference Latency (ms/sample)
"""

import argparse
import json
import logging
import time
from pathlib import Path
import numpy as np
import tensorflow as tf

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("kanji_evaluator")


def evaluate_model(model_path: str, data_dir: str, batch_size: int = 64, img_size: int = 128, test_split: float = 0.1, seed: int = 1203):
    model_file = Path(model_path)
    if not model_file.exists():
        raise FileNotFoundError(f"Không tìm thấy mô hình: {model_path}")

    logger.info(f"🔄 Đang tải mô hình từ: {model_path}")
    model = tf.keras.models.load_model(model_path)
    logger.info("✅ Tải mô hình thành công!")

    logger.info(f"📂 Đang tải tập dữ liệu kiểm thử từ: {data_dir}")
    test_ds = tf.keras.utils.image_dataset_from_directory(
        data_dir,
        validation_split=test_split,
        subset="validation",
        seed=seed,
        image_size=(img_size, img_size),
        batch_size=batch_size,
        color_mode="grayscale",
        shuffle=False
    )

    normalization_layer = tf.keras.layers.Rescaling(1.0 / 255.0)
    test_ds = test_ds.map(lambda x, y: (normalization_layer(x), y)).prefetch(tf.data.AUTOTUNE)

    total_samples = 0
    top1_correct = 0
    top3_correct = 0
    top5_correct = 0

    latencies = []

    logger.info("⚡ Bắt đầu chấm điểm và đánh giá trên tập kiểm thử...")
    for batch_x, batch_y in test_ds:
        start_time = time.perf_counter()
        preds = model.predict_on_batch(batch_x)
        latencies.append((time.perf_counter() - start_time) / len(batch_y) * 1000)

        # argsort lấy top 5 nhãn có xác suất cao nhất
        top5_indices = np.argsort(preds, axis=-1)[:, -5:]

        y_true = batch_y.numpy()
        for i, true_label in enumerate(y_true):
            total_samples += 1
            if true_label == top5_indices[i, -1]:
                top1_correct += 1
            if true_label in top5_indices[i, -3:]:
                top3_correct += 1
            if true_label in top5_indices[i, -5:]:
                top5_correct += 1

    top1_acc = (top1_correct / total_samples) * 100
    top3_acc = (top3_correct / total_samples) * 100
    top5_acc = (top5_correct / total_samples) * 100
    avg_latency = np.mean(latencies)

    import sys
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

    result = {
        "model": str(model_path),
        "total_samples": total_samples,
        "top1_accuracy": round(top1_acc, 2),
        "top3_accuracy": round(top3_acc, 2),
        "top5_accuracy": round(top5_acc, 2),
        "avg_latency_ms": round(avg_latency, 2)
    }

    report_path = Path("D:/DATN/ElearningJP/AI/data/benchmark_report.json")
    report_path.parent.mkdir(parents=True, exist_ok=True)
    with open(report_path, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2)
    logger.info(f"Bao cao danh gia da duoc luu vao: {report_path}")

    print("\n" + "=" * 50)
    print(" [KET QUA DANH GIA MO HINH NHAN DIEN KANJI] ")
    print("=" * 50)
    print(f"Tong so anh kiem thu:      {total_samples:,}")
    print(f"Do chinh xac Top-1:        {top1_acc:.2f}%")
    print(f"Do chinh xac Top-3:        {top3_acc:.2f}%")
    print(f"Do chinh xac Top-5:        {top5_acc:.2f}%")
    print(f"Thoi gian du doan TB:      {avg_latency:.2f} ms / anh")
    print("=" * 50 + "\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Đánh giá mô hình Kanji CNN - ElearningJP")
    parser.add_argument("--model-path", type=str, default="D:/DATN/ElearningJP/AI/models/kanji_3036_best_e4.h5")
    parser.add_argument("--data-dir", type=str, default="D:/DATN/ElearningJP/AI/data/ETL9G_IMG")
    parser.add_argument("--batch-size", type=int, default=64)
    parser.add_argument("--img-size", type=int, default=128)
    parser.add_argument("--test-split", type=float, default=0.1)
    args = parser.parse_args()

    evaluate_model(
        model_path=args.model_path,
        data_dir=args.data_dir,
        batch_size=args.batch_size,
        img_size=args.img_size,
        test_split=args.test_split
    )
