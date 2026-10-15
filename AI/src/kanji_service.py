import io
import json
import logging
import os
from pathlib import Path
from typing import List, Tuple, Optional
import numpy as np
from PIL import Image, ImageOps
import tensorflow as tf

from .schemas import PredictionItem, PredictionResponse

logger = logging.getLogger("kanji_ai")
logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")

CURRENT_DIR = Path(__file__).resolve().parent
MODELS_DIR = CURRENT_DIR.parent / "models"

# Tự động ưu tiên mô hình mở rộng 3.169 lớp (gồm Kanji, Katakana, Số, Chữ cái) nếu có
EXTENDED_MODEL = MODELS_DIR / "kanji_model_extended.h5"
EXTENDED_LABEL = MODELS_DIR / "label_3169.json"

if EXTENDED_MODEL.exists() and EXTENDED_LABEL.exists():
    MODEL_PATH = EXTENDED_MODEL
    LABEL_PATH = EXTENDED_LABEL
else:
    MODEL_PATH = MODELS_DIR / "kanji_model_best.h5"
    LABEL_PATH = MODELS_DIR / "label_3036.json"

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
        logger.info(f"✅ Đã nạp thành công {len(self.labels)} nhãn chữ Kanji/Kana.")

        # 2. Nạp mô hình TensorFlow / Keras
        self.model = tf.keras.models.load_model(str(MODEL_PATH))
        self.input_shape = self.model.input_shape  # (None, 96, 96, 1) hoặc (None, 128, 128, 1)
        self.target_size = (self.input_shape[1], self.input_shape[2]) if len(self.input_shape) >= 3 else (96, 96)
        logger.info(f"✅ Nạp mô hình thành công! Kích thước ảnh đầu vào: {self.target_size}")

    @classmethod
    def get_instance(cls) -> "KanjiRecognitionService":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def preprocess_image(self, image_bytes: bytes) -> Optional[np.ndarray]:
        """
        Tiền xử lý ảnh chuyên sâu cho nét vẽ Canvas:
        1. Nhận diện nền sáng/tối -> chuẩn hóa về NỀN ĐEN NÉT TRẮNG.
        2. Tự động cắt sát nét vẽ (Bounding Box Auto-crop).
        3. Căn giữa vào khung vuông giữ nguyên tỉ lệ (Aspect Ratio Preserving).
        4. Resize về đúng kích thước model đã train (96x96) và chuẩn hóa pixel [0, 1].
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

            # Kiểm tra màu nền: nếu 4 góc ảnh có màu sáng (> 128) -> người dùng vẽ nét đen trên nền trắng
            # Cần đảo ngược lại để thành NỀN ĐEN NÉT TRẮNG chuẩn theo bộ dữ liệu train
            corners = [arr[0, 0], arr[0, -1], arr[-1, 0], arr[-1, -1]]
            if np.mean(corners) > 128:
                gray_img = ImageOps.invert(gray_img)

            # 2. Bounding Box Auto-crop: Tìm khung bao quanh các nét vẽ
            # Đặt ngưỡng để phân biệt nét vẽ với nền đen
            threshold = 30
            binary = gray_img.point(lambda p: 255 if p > threshold else 0)
            bbox = binary.getbbox()

            if bbox is None:
                # Canvas hoàn toàn trống (không có nét vẽ nào)
                logger.warning("Canvas trống, không tìm thấy nét vẽ nào.")
                return None

            # Cắt lấy vùng chứa chữ
            cropped = gray_img.crop(bbox)
            crop_w, crop_h = cropped.size

            # 3. Tạo khung vuông với padding 15% để nét vẽ không bị chạm viền
            max_side = max(crop_w, crop_h)
            padding = int(max_side * 0.15)
            square_size = max_side + (padding * 2)

            square_img = Image.new("L", (square_size, square_size), color=0)
            # Dán vùng chữ vào chính giữa khung vuông
            paste_x = padding + (max_side - crop_w) // 2
            paste_y = padding + (max_side - crop_h) // 2
            square_img.paste(cropped, (paste_x, paste_y))

            # 4. Resize về kích thước chuẩn của model (96x96)
            final_img = square_img.resize(self.target_size, Image.Resampling.BILINEAR)

            # 5. Chuyển sang mảng numpy float32 và chuẩn hóa độ tương phản sắc nét [0, 1]
            arr = np.array(final_img, dtype=np.float32)
            max_val = float(np.max(arr))
            if max_val > 50.0:
                arr = arr / max_val
            else:
                arr = arr / 255.0

            img_array = np.expand_dims(arr, axis=-1)  # (H, W, 1)
            img_array = np.expand_dims(img_array, axis=0)   # (1, H, W, 1)

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
