"""
ElearningJP AI - Kanji Handwriting Recognition Model Training
Architecture: Deep Residual CNN (4-Block ResNet-like + Global Average Pooling)
Classes: 3,036 Japanese Kanji & Kana characters from ETL9G
Hardware: NVIDIA GeForce RTX 3050 Laptop GPU (PyTorch CUDA Acceleration via Keras 3)
Features:
- Transfer learning / Fine-tuning resuming from kanji_3036_best_e4.h5
- Advanced Data Augmentation (Rotation, Zoom, Translation, Contrast, Noise)
- EarlyStopping (stops when accuracy no longer improves or decreases) & ReduceLROnPlateau
- restore_best_weights=True (rolls back to highest accuracy epoch)
- Top-1 and Top-5 evaluation metrics
- CSVLogger for live training history
"""

import os
os.environ["KERAS_BACKEND"] = "torch"
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"

import argparse
import json
import logging
import sys
from pathlib import Path

import keras
from keras import layers, models, callbacks, optimizers
import torch

try:
    sys.stdout.reconfigure(encoding='utf-8')
except Exception:
    pass

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=[logging.StreamHandler(sys.stdout)]
)
logger = logging.getLogger("kanji_trainer")


def build_kanji_residual_cnn(num_classes: int, input_shape=(128, 128, 1)) -> models.Model:
    """
    Xây dựng kiến trúc Deep Residual CNN tương thích với checkpoint gốc:
    Tích hợp trực tiếp Rescaling & Augmentation trên GPU, 4 Blocks Residual và Head.
    """
    inputs = layers.Input(shape=input_shape, name="input_image")

    # Chuẩn hóa pixel [0, 1] và tăng cường nét vẽ trực tiếp trên GPU
    x = layers.Rescaling(1.0 / 255.0, name="rescaling")(inputs)
    x = layers.RandomRotation(0.05, fill_mode='constant', fill_value=0.0, name="random_rotation")(x)
    x = layers.RandomZoom(height_factor=(-0.05, 0.05), width_factor=(-0.05, 0.05), fill_mode='constant', fill_value=0.0, name="random_zoom")(x)
    x = layers.RandomTranslation(height_factor=0.05, width_factor=0.05, fill_mode='constant', fill_value=0.0, name="random_translation")(x)

    # --- Block 1: Trích xuất nét cơ bản ---
    x = layers.Conv2D(64, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.Conv2D(64, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.MaxPooling2D((2, 2))(x)

    # --- Block 2: Trích xuất góc & giao điểm nét ---
    x = layers.Conv2D(128, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.Conv2D(128, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.MaxPooling2D((2, 2))(x)

    # --- Block 3: Residual Block 256 filters (Trích xuất bộ thủ) ---
    x_shortcut = x
    x = layers.Conv2D(256, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.Conv2D(256, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)

    # 1x1 Conv shortcut
    x_shortcut = layers.Conv2D(256, (1, 1), padding='same', kernel_initializer='he_normal')(x_shortcut)
    x = layers.Add()([x, x_shortcut])
    x = layers.LeakyReLU(0.1)(x)
    x = layers.MaxPooling2D((2, 2))(x)

    # --- Block 4: Residual Block 384 filters (Trích xuất cấu trúc tổng thể) ---
    x_shortcut = x
    x = layers.Conv2D(384, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.Conv2D(384, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)

    # 1x1 Conv shortcut
    x_shortcut = layers.Conv2D(384, (1, 1), padding='same', kernel_initializer='he_normal')(x_shortcut)
    x = layers.Add()([x, x_shortcut])
    x = layers.LeakyReLU(0.1)(x)
    x = layers.MaxPooling2D((2, 2))(x)

    # --- Classification Head ---
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dense(512, kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.Dropout(0.3)(x)

    x = layers.Dense(256, kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.Dropout(0.3)(x)

    outputs = layers.Dense(num_classes, activation='softmax', dtype='float32', name="predictions")(x)

    model = models.Model(inputs=inputs, outputs=outputs, name="ElearningJP_Kanji_Residual_CNN")
    return model


def main():
    parser = argparse.ArgumentParser(description="Huấn luyện mô hình Deep CNN nhận diện chữ Kanji - ElearningJP")
    parser.add_argument("--data-dir", type=str, default="D:/DATN/ElearningJP/AI/data/ETL9G_IMG", help="Thư mục chứa dữ liệu ETL9G")
    parser.add_argument("--img-size", type=int, default=128, help="Kích thước ảnh đầu vào (128x128)")
    parser.add_argument("--batch-size", type=int, default=8, help="Kích thước mini-batch an toàn tuyệt đối cho RTX 3050 (4GB VRAM)")
    parser.add_argument("--epochs", type=int, default=100, help="Số lượng epoch tối đa (tự dừng khi bão hòa)")
    parser.add_argument("--patience", type=int, default=5, help="Số epoch kiên nhẫn trước khi dừng sớm nếu accuracy không tăng")
    parser.add_argument("--lr", type=float, default=2e-4, help="Learning rate ban đầu")
    parser.add_argument("--seed", type=int, default=1203, help="Seed phân chia dữ liệu train/val")
    parser.add_argument("--resume-weights", type=str, default="D:/DATN/ElearningJP/AI/models/kanji_3036_best_e4.h5", help="File trọng số checkpoint để fine-tune tiếp")
    parser.add_argument("--save-path", type=str, default="D:/DATN/ElearningJP/AI/models/kanji_model_best.h5", help="Nơi lưu mô hình tốt nhất")

    args = parser.parse_args()

    # Kiểm tra thiết bị GPU
    cuda_avail = torch.cuda.is_available()
    device_name = torch.cuda.get_device_name(0) if cuda_avail else "CPU"
    logger.info(f"⚡ Thiết bị tăng tốc: {device_name} (CUDA={cuda_avail})")

    data_path = Path(args.data_dir)
    if not data_path.exists():
        logger.error(f"Thư mục dữ liệu không tồn tại: {data_path}")
        sys.exit(1)

    img_shape = (args.img_size, args.img_size)

    # 1. Tải Dataset Train và Validation từ thư mục
    logger.info(f"📂 Đang nạp dữ liệu từ: {data_path}")
    train_ds = keras.utils.image_dataset_from_directory(
        str(data_path),
        validation_split=0.2,
        subset="training",
        seed=args.seed,
        image_size=img_shape,
        batch_size=args.batch_size,
        color_mode='grayscale'
    )

    val_ds = keras.utils.image_dataset_from_directory(
        str(data_path),
        validation_split=0.2,
        subset="validation",
        seed=args.seed,
        image_size=img_shape,
        batch_size=args.batch_size,
        color_mode='grayscale'
    )

    class_names = train_ds.class_names
    num_classes = len(class_names)
    logger.info(f"✅ Phát hiện tổng cộng {num_classes} lớp phân loại.")

    # 2. Pipeline tối ưu nạp dữ liệu đệm (prefetching)
    train_ds = train_ds.prefetch(8)
    val_ds = val_ds.prefetch(4)

    # 3. Khởi tạo mô hình
    model = build_kanji_residual_cnn(num_classes, input_shape=(args.img_size, args.img_size, 1))

    # Tải lại trọng số pretrained từ kanji_3036_best_e4.h5
    resume_path = Path(args.resume_weights)
    if resume_path.exists():
        logger.info(f"🔄 Đang nạp trọng số checkpoint từ: {resume_path}")
        try:
            model.load_weights(str(resume_path), by_name=True, skip_mismatch=True)
            logger.info("✅ Nạp trọng số checkpoint thành công!")
        except Exception as e:
            logger.warning(f"⚠️ Không thể nạp weights tự động: {e}. Tiếp tục train từ đầu.")

    # 4. Biên dịch mô hình
    optimizer = optimizers.Adam(learning_rate=args.lr)
    model.compile(
        optimizer=optimizer,
        loss=keras.losses.SparseCategoricalCrossentropy(),
        metrics=[
            keras.metrics.SparseCategoricalAccuracy(name='top1_acc'),
            keras.metrics.SparseTopKCategoricalAccuracy(k=5, name='top5_acc')
        ]
    )

    # 5. Cấu hình Callbacks theo đúng yêu cầu dừng tự động
    save_path = Path(args.save_path)
    save_path.parent.mkdir(parents=True, exist_ok=True)
    csv_log_path = Path("D:/DATN/ElearningJP/AI/data/training_history.csv")
    csv_log_path.parent.mkdir(parents=True, exist_ok=True)

    class CudaMemoryCleanupCallback(callbacks.Callback):
        def on_batch_end(self, batch, logs=None):
            if batch % 50 == 0:
                torch.cuda.empty_cache()

        def on_epoch_end(self, epoch, logs=None):
            torch.cuda.empty_cache()

    cb_list = [
        CudaMemoryCleanupCallback(),
        callbacks.ModelCheckpoint(
            filepath=str(save_path),
            monitor='val_top1_acc',
            save_best_only=True,
            mode='max',
            verbose=1
        ),
        callbacks.EarlyStopping(
            monitor='val_top1_acc',
            patience=args.patience,
            restore_best_weights=True,
            mode='max',
            verbose=1
        ),
        callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=2,
            min_lr=1e-6,
            verbose=1
        ),
        callbacks.CSVLogger(
            filename=str(csv_log_path),
            separator=",",
            append=False
        )
    ]

    logger.info("==================================================")
    logger.info("🚀 BẮT ĐẦU QUÁ TRÌNH HUẤN LUYỆN KANJI RESIDUAL CNN")
    logger.info(f"   Thiết bị: {device_name}")
    logger.info(f"   Batch size: {args.batch_size}")
    logger.info(f"   Max Epochs: {args.epochs}")
    logger.info(f"   EarlyStopping Patience: {args.patience} epochs")
    logger.info(f"   Lưu checkpoint tốt nhất tại: {save_path}")
    logger.info("==================================================")

    history = model.fit(
        train_ds,
        validation_data=val_ds,
        epochs=args.epochs,
        callbacks=cb_list,
        verbose=1
    )

    logger.info(f"🎉 Huấn luyện hoàn tất! Mô hình tốt nhất đã được lưu tại: {save_path}")


if __name__ == "__main__":
    main()
