import io
import json
import logging
import os
from pathlib import Path
from typing import List, Tuple, Optional
import numpy as np
from PIL import Image, ImageOps

from .schemas import PredictionItem, PredictionResponse

logger = logging.getLogger("kanji_ai")
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

CURRENT_DIR = Path(__file__).resolve().parent
MODELS_DIR = CURRENT_DIR.parent / "models"

# Sử dụng mô hình nhận diện Kanji/Kana/Latin/Digit chuẩn 3.170 lớp huấn luyện từ Kaggle
MODEL_PATH = MODELS_DIR / "kanji_model_best.h5"
LABEL_PATH = MODELS_DIR / "label_3169.json"

def _build_kanji_residual_model(num_classes: int, input_shape=(128, 128, 1)):
    """Xây dựng kiến trúc Deep Residual CNN tương thích với checkpoint huấn luyện"""
    try:
        from keras import layers, models
    except ImportError:
        from tensorflow.keras import layers, models

    inputs = layers.Input(shape=input_shape)

    # Block 1
    x = layers.Conv2D(64, (3, 3), padding='same', kernel_initializer='he_normal')(inputs)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.Conv2D(64, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.MaxPooling2D((2, 2))(x)

    # Block 2
    x = layers.Conv2D(128, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.Conv2D(128, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.MaxPooling2D((2, 2))(x)

    # Block 3: Residual 256
    x_res = layers.Conv2D(256, (1, 1), padding='same', kernel_initializer='he_normal')(x)
    x = layers.Conv2D(256, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.Conv2D(256, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.Add()([x, x_res])
    x = layers.LeakyReLU(0.1)(x)
    x = layers.MaxPooling2D((2, 2))(x)

    # Block 4: Residual 384
    x_res = layers.Conv2D(384, (1, 1), padding='same', kernel_initializer='he_normal')(x)
    x = layers.Conv2D(384, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.Conv2D(384, (3, 3), padding='same', kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.Add()([x, x_res])
    x = layers.LeakyReLU(0.1)(x)
    x = layers.MaxPooling2D((2, 2))(x)

    # Classification Head
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.Dense(512, kernel_initializer='he_normal')(x)
    x = layers.BatchNormalization()(x)
    x = layers.LeakyReLU(0.1)(x)
    x = layers.Dropout(0.3)(x)
    outputs = layers.Dense(num_classes, activation='softmax')(x)
    return models.Model(inputs=inputs, outputs=outputs, name='Kanji_Residual_CNN')

class KanjiRecognitionService:
    _instance: Optional["KanjiRecognitionService"] = None

    def __init__(self):
        logger.info(f"Đang khởi tạo KanjiRecognitionService...")
        logger.info(f"Đường dẫn Model: {MODEL_PATH}")
        logger.info(f"Đường dẫn Nhãn: {LABEL_PATH}")

        if not MODEL_PATH.exists():
            raise FileNotFoundError(f"Không tìm thấy file mô hình tại: {MODEL_PATH}")
        if not LABEL_PATH.exists():
            raise FileNotFoundError(f"Không tìm thấy file nhãn tại: {LABEL_PATH}")

        # 1. Nạp danh sách nhãn
        with open(LABEL_PATH, "r", encoding="utf-8") as f:
            self.labels: List[str] = json.load(f)
        logger.info(f"✅ Đã nạp thành công {len(self.labels)} nhãn chữ Kanji/Kana/Latin/Digit.")

        # 2. Nạp mô hình TensorFlow / Keras tương thích đa phiên bản
        try:
            self.model = _build_kanji_residual_model(num_classes=len(self.labels), input_shape=(128, 128, 1))
            self.model.load_weights(str(MODEL_PATH))
            logger.info("✅ Nạp trọng số mô hình qua load_weights thành công!")
        except Exception as e:
            logger.warning(f"Thử nạp trực tiếp bằng load_model do: {e}")
            try:
                import keras
                self.model = keras.models.load_model(str(MODEL_PATH), compile=False)
            except Exception:
                import tensorflow as tf
                self.model = tf.keras.models.load_model(str(MODEL_PATH), compile=False)

        self.input_shape = self.model.input_shape
        self.target_size = (self.input_shape[1], self.input_shape[2]) if len(self.input_shape) >= 3 else (128, 128)
        logger.info(f"✅ Nạp mô hình thành công! Kích thước ảnh đầu vào: {self.target_size}")

    @classmethod
    def get_instance(cls) -> "KanjiRecognitionService":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def preprocess_image(self, image_bytes: bytes) -> Optional[np.ndarray]:
        """
        Tiền xử lý ảnh chuyên sâu cho nét vẽ Canvas tương thích 100% với miền dữ liệu ETL9G:
        1. Nhận diện nền sáng/tối -> tách nét vẽ thành Mask nhị phân.
        2. Tự động cắt sát nét vẽ (Bounding Box Auto-crop).
        3. Căn giữa vào khung giấy chuẩn ETL9G (giữ nguyên tỉ lệ hình học):
           - Kích thước ảnh: 128x128 với viền đen bao quanh.
           - Khung giấy trắng: 104x104 (pixel = 255).
           - Nét vẽ mực đen (pixel = 0) nằm gọn ở trung tâm (~46px).
        4. Chuẩn hóa pixel về [0.0, 1.0] tương ứng với pipeline train mô hình.
        """
        try:
            pil_img = Image.open(io.BytesIO(image_bytes))

            # Nếu ảnh có kênh Alpha (trong suốt), dán lên nền đen
            if pil_img.mode in ("RGBA", "LA") or (pil_img.mode == "P" and "transparency" in pil_img.info):
                background = Image.new("RGBA", pil_img.size, (0, 0, 0, 255))
                background.paste(pil_img, mask=pil_img.split()[-1])
                pil_img = background

            # Chuyển sang ảnh xám Grayscale (L)
            gray_img = pil_img.convert("L")
            arr = np.array(gray_img)

            # Kiểm tra màu nền: nếu 4 góc ảnh có màu sáng (> 128) -> vẽ nét đen trên nền trắng
            # Cần đảo ngược để chuẩn hóa nét vẽ thành nét trắng (255) trên nền đen (0) phục vụ crop mask
            corners = [arr[0, 0], arr[0, -1], arr[-1, 0], arr[-1, -1]]
            if np.mean(corners) > 128:
                gray_img = ImageOps.invert(gray_img)

            # 2. Bounding Box Auto-crop: Tìm khung bao quanh các nét vẽ
            threshold = 30
            binary = gray_img.point(lambda p: 255 if p > threshold else 0)
            bbox = binary.getbbox()

            if bbox is None:
                logger.warning("Canvas trống, không tìm thấy nét vẽ nào.")
                return None

            # Cắt lấy vùng chứa chữ (nét trắng trên nền đen)
            cropped = gray_img.crop(bbox)
            crop_w, crop_h = cropped.size

            # 3. Chuẩn hóa kích thước nét vẽ vào khung giấy trắng ETL9G
            # Kích thước chữ chuẩn trong ảnh ETL9G chiếm ~44 - 48 pixel trên khung 128x128
            target_char_size = 46.0
            scale = target_char_size / max(crop_w, crop_h)
            new_w = max(1, int(round(crop_w * scale)))
            new_h = max(1, int(round(crop_h * scale)))
            resized_char = cropped.resize((new_w, new_h), Image.Resampling.BILINEAR)

            # 4. Tạo ảnh chuẩn ETL9G:
            # - Kích thước tổng thể 128x128, viền ngoài màu đen (0)
            # - Khung giấy bên trong 104x104 màu trắng (255)
            # - Nét chữ mực đen (0) dán vào trung tâm giấy
            final_img = Image.new("L", self.target_size, color=0)
            paper = Image.new("L", (104, 104), color=255)

            paste_x = (104 - new_w) // 2
            paste_y = (104 - new_h) // 2

            # Dán nét chữ đen lên giấy trắng thông qua mask
            char_ink = Image.new("L", (new_w, new_h), color=0)
            paper.paste(char_ink, (paste_x, paste_y), mask=resized_char)

            # Dán tờ giấy trắng vào chính giữa khung 128x128 (offset viền 12px)
            final_img.paste(paper, (12, 12))

            # 5. Chuyển sang mảng numpy float32 và chuẩn hóa [0.0, 1.0]
            arr = np.array(final_img, dtype=np.float32) / 255.0
            img_array = np.expand_dims(arr, axis=-1)  # (128, 128, 1)
            img_array = np.expand_dims(img_array, axis=0)   # (1, 128, 128, 1)

            return img_array

        except Exception as e:
            logger.error(f"Lỗi khi tiền xử lý ảnh: {e}", exc_info=True)
            return None

    def predict(self, image_bytes: bytes, top_k: int = 10) -> PredictionResponse:
        """
        Dự đoán chữ Kanji từ ảnh nét vẽ và trả về Top-K kết quả
        """
        input_tensor = self.preprocess_image(image_bytes)
        if input_tensor is None:
            return PredictionResponse(
                success=False,
                top_char=None,
                predictions=[],
                message="Không phát hiện nét vẽ nào trên Canvas. Vui lòng vẽ ký tự trước khi nhận dạng."
            )

        # Chạy inference
        predictions = self.model.predict(input_tensor, verbose=0)[0]  # Shape: (3036,)

        # Lấy top_k chỉ số có xác suất cao nhất
        top_indices = np.argsort(predictions)[::-1][:top_k]

        results: List[PredictionItem] = []
        for idx in top_indices:
            raw_label = self.labels[idx]
            conf = float(predictions[idx])

            # Nếu nhãn ở dạng mã Hex Unicode (ví dụ '0x4e00') -> chuyển thành ký tự thật ('一')
            actual_char = raw_label
            hex_code = None
            if raw_label.startswith("0x") or raw_label.startswith("0X"):
                try:
                    actual_char = chr(int(raw_label, 16))
                    hex_code = raw_label.lower()
                except Exception:
                    actual_char = raw_label
            else:
                try:
                    hex_code = f"0x{ord(actual_char):04x}"
                except Exception:
                    hex_code = None

            results.append(PredictionItem(
                char=actual_char,
                unicode_hex=hex_code,
                confidence=round(conf, 4),
                percentage=round(conf * 100.0, 2)
            ))

        top_char = results[0].char if results else None

        return PredictionResponse(
            success=True,
            top_char=top_char,
            predictions=results,
            message="Nhận dạng thành công"
        )
