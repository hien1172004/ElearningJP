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

        os.makedirs(output_dir, exist_ok=True)
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
                filepath = os.path.join(output_dir, filename)

                with open(filepath, "wb") as f:
                    f.write(image_bytes)

                extracted_images.append({
                    "page": page_index + 1,
                    "filename": filename,
                    "filepath": filepath,
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
