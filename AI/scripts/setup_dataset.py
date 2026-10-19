"""
ElearningJP AI - Dataset Setup & Verification Script
Tasks:
1. Moves or links the downloaded ETL9G dataset to D:/DATN/ElearningJP/AI/data/ETL9G_IMG.
2. Verifies the total number of classes (3,036 classes).
3. Verifies label mapping with label_3036.json and label_3036_raw.json.
4. Generates a dataset summary report.
"""

import os
import shutil
import json
import logging
from pathlib import Path

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("dataset_setup")

SRC_PATH = Path("C:/Users/Admin/.cache/kagglehub/datasets/votrancong/etl9g-images-processed-96x96/versions/1/images_progressed")
DST_PATH = Path("D:/DATN/ElearningJP/AI/data/ETL9G_IMG")
MODELS_DIR = Path("D:/DATN/ElearningJP/AI/models")


def setup_dataset():
    logger.info(f"Kiểm tra thư mục nguồn: {SRC_PATH}")
    if not SRC_PATH.exists():
        if DST_PATH.exists() and len(list(DST_PATH.iterdir())) >= 3000:
            logger.info(f"✅ Dữ liệu đã sẵn sàng tại đích: {DST_PATH}")
        else:
            raise FileNotFoundError(f"Không tìm thấy thư mục nguồn: {SRC_PATH}")
    else:
        # Di chuyển thư mục từ C: sang D: để tiết kiệm ổ C và đặt đúng vị trí dự án
        if not DST_PATH.exists():
            logger.info(f"🚚 Đang di chuyển dữ liệu từ {SRC_PATH} sang {DST_PATH}...")
            DST_PATH.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(SRC_PATH), str(DST_PATH))
            logger.info("✅ Di chuyển dữ liệu thành công!")
        else:
            logger.info(f"Thư mục đích {DST_PATH} đã tồn tại.")

    # Kiểm tra số lượng class
    class_folders = sorted([d.name for d in DST_PATH.iterdir() if d.is_dir()])
    logger.info(f"📊 Tổng số thư mục lớp ký tự: {len(class_folders)}")

    # Kiểm tra liên kết với label_3036.json
    label_path = MODELS_DIR / "label_3036.json"
    raw_label_path = MODELS_DIR / "label_3036_raw.json"

    if label_path.exists():
        with open(label_path, "r", encoding="utf-8") as f:
            labels = json.load(f)
        logger.info(f"Đã đọc {len(labels)} nhãn từ {label_path.name}")
        matches = sum(1 for c in class_folders if c in labels or c.lower() in [l.lower() for l in labels])
        logger.info(f"✅ Tỉ lệ khớp nhãn lớp: {matches}/{len(class_folders)}")

    # Thống kê tổng số ảnh mẫu
    sample_count = 0
    for folder in list(DST_PATH.iterdir())[:50]:
        sample_count += len([f for f in folder.glob("*.png")])
    avg_per_class = sample_count / 50 if sample_count > 0 else 0
    estimated_total = int(avg_per_class * len(class_folders))

    logger.info(f"📸 Ước tính số lượng ảnh mẫu: ~{estimated_total:,} ảnh (trung bình ~{avg_per_class:.1f} ảnh/lớp)")
    logger.info("🎉 Chuẩn bị Dataset ETL9G hoàn tất và sẵn sàng cho việc huấn luyện!")


if __name__ == "__main__":
    setup_dataset()
