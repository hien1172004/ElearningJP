"""
PDFExtractor — Bóc tách hình ảnh nhúng và text từ file PDF đề thi JLPT.
Giữ nguyên các method cũ + thêm render page to image cho Gemini Vision.
"""

import io
import os
from typing import List, Dict, Any
from PIL import Image


class PDFExtractor:
    @staticmethod
    def extract_images_from_pdf(pdf_bytes: bytes, output_dir: str = "extracted_images") -> List[Dict[str, Any]]:
        """
        Tự động bóc tách tất cả các hình ảnh nhúng trong file PDF (tờ rơi, biển quảng cáo, tranh nghe)
        mà không cần phải cắt tay hay chụp màn hình.
        """
        try:
            import fitz  # PyMuPDF
        except ImportError:
            return []

        from pathlib import Path
        default_dir = Path(__file__).resolve().parent.parent.parent / "extracted_images"
        target_dir = Path(output_dir) if os.path.isabs(output_dir) else default_dir
        os.makedirs(target_dir, exist_ok=True)
        doc = fitz.open(stream=pdf_bytes, filetype="pdf")
        extracted_images = []

        for page_index, page in enumerate(doc):
            image_list = page.get_images(full=True)
            for img_index, img in enumerate(image_list):
                xref = img[0]
                base_image = doc.extract_image(xref)
                image_bytes = base_image["image"]
                image_ext = base_image["ext"]

                # Lọc bỏ các icon quá nhỏ (dưới 100x100 px)
                pil_image = Image.open(io.BytesIO(image_bytes))
                if pil_image.width < 100 or pil_image.height < 100:
                    continue

                filename = f"page_{page_index + 1}_img_{img_index + 1}.{image_ext}"
                filepath = str(target_dir / filename)

                with open(filepath, "wb") as f:
                    f.write(image_bytes)

                url = f"http://localhost:8000/static/extracted_images/{filename}"

                extracted_images.append({
                    "page": page_index + 1,
                    "filename": filename,
                    "filepath": filepath,
                    "url": url,
                    "width": pil_image.width,
                    "height": pil_image.height
                })

        return extracted_images

    @staticmethod
    def extract_text_per_page(pdf_bytes: bytes) -> List[str]:
        """Trích xuất text theo từng trang để gửi cho Gemini Vision/Text"""
        try:
            import fitz  # PyMuPDF
            doc = fitz.open(stream=pdf_bytes, filetype="pdf")
            return [page.get_text() for page in doc]
        except ImportError:
            return []

    @staticmethod
    def render_pages_to_images(pdf_bytes: bytes, dpi: int = 200) -> List[bytes]:
        """
        Render từng trang PDF thành ảnh PNG bytes để gửi cho Gemini Vision.

        Args:
            pdf_bytes: Nội dung file PDF dạng bytes
            dpi: Độ phân giải ảnh (200 DPI cho cân bằng chất lượng/kích thước)

        Returns:
            Danh sách các ảnh PNG bytes, mỗi phần tử là 1 trang
        """
        try:
            import fitz  # PyMuPDF
        except ImportError:
            return []

        doc = fitz.open(stream=pdf_bytes, filetype="pdf")
        zoom = dpi / 72
        mat = fitz.Matrix(zoom, zoom)
        page_images = []

        for page in doc:
            pix = page.get_pixmap(matrix=mat)
            page_images.append(pix.tobytes("png"))

        return page_images

    @staticmethod
    def get_page_count(pdf_bytes: bytes) -> int:
        """Đếm số trang PDF."""
        try:
            import fitz
            doc = fitz.open(stream=pdf_bytes, filetype="pdf")
            return len(doc)
        except ImportError:
            return 0
