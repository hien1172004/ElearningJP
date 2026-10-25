import io
import os
import shutil
import sys
import tempfile
import unittest
from PIL import Image, ImageDraw
import fitz  # PyMuPDF
from fastapi.testclient import TestClient

if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

from src.main import app
from src.exam.pdf_extractor import PDFExtractor
from src.exam.audio_processor import AudioProcessor


class TestPdfAndAudioFeatures(unittest.TestCase):

    def setUp(self):
        self.temp_dir = tempfile.mkdtemp()
        self.client = TestClient(app)

    def tearDown(self):
        if os.path.exists(self.temp_dir):
            shutil.rmtree(self.temp_dir)

    def _create_sample_exam_pdf(self) -> bytes:
        """Tạo một file PDF đề thi mẫu bằng PyMuPDF có text và ảnh nhúng"""
        doc = fitz.open()

        # Trang 1: Text đề thi JLPT N5
        page1 = doc.new_page()
        page1.insert_text(
            (50, 72),
            "JLPT N5 MOCK EXAM\n"
            "Mondai 1: Vocabulary and Grammar\n"
            "Question 1: Choose the correct answer.\n"
            "1. Option A\n"
            "2. Option B\n",
            fontsize=12
        )

        # Tạo 1 ảnh mẫu (tranh bài nghe) kích thước 250x250 px
        img = Image.new("RGB", (250, 250), color=(240, 240, 255))
        draw = ImageDraw.Draw(img)
        draw.rectangle([(20, 20), (230, 230)], outline=(30, 60, 180), width=4)
        draw.text((40, 110), "Exam Illustration", fill=(20, 20, 20))
        img_bytes = io.BytesIO()
        img.save(img_bytes, format="PNG")
        img_data = img_bytes.getvalue()

        # Trang 2: Đề thi có nhúng ảnh minh họa
        page2 = doc.new_page()
        page2.insert_text((50, 50), "Mondai 2: Look at the picture and choose answer.", fontsize=12)
        page2.insert_image(fitz.Rect(50, 100, 300, 350), stream=img_data)

        # Lưu thành bytes
        pdf_bytes = doc.write()
        doc.close()
        return pdf_bytes

    def test_pdf_extract_text_per_page(self):
        """Kiểm thử tính năng trích xuất text từng trang từ file PDF đề thi"""
        print("\n--- Testing PDF Extractor: Text per page ---")
        pdf_bytes = self._create_sample_exam_pdf()
        pages_text = PDFExtractor.extract_text_per_page(pdf_bytes)

        self.assertEqual(len(pages_text), 2, "PDF phải có đúng 2 trang")
        self.assertIn("JLPT N5 MOCK EXAM", pages_text[0])
        self.assertIn("Mondai 1", pages_text[0])
        self.assertIn("Mondai 2", pages_text[1])
        print(f"SUCCESS: Extracted text from {len(pages_text)} pages successfully!")

    def test_pdf_extract_images(self):
        """Kiểm thử tính năng bóc tách ảnh nhúng trong PDF (loại bỏ icon rác, giữ tranh đề thi)"""
        print("\n--- Testing PDF Extractor: Embedded Images ---")
        pdf_bytes = self._create_sample_exam_pdf()
        out_img_dir = os.path.join(self.temp_dir, "extracted_imgs")

        images = PDFExtractor.extract_images_from_pdf(pdf_bytes, output_dir=out_img_dir)

        self.assertGreaterEqual(len(images), 1, "Phải tìm thấy ít nhất 1 ảnh nhúng trong trang 2")
        img_info = images[0]
        print(f"Image extracted: page={img_info['page']}, file={img_info['filename']}, size={img_info['width']}x{img_info['height']}")

        self.assertEqual(img_info["page"], 2)
        self.assertEqual(img_info["width"], 250)
        self.assertEqual(img_info["height"], 250)
        self.assertTrue(os.path.exists(img_info["filepath"]))
        print("SUCCESS: Extracted embedded exam images successfully!")

    def test_audio_split_by_question_markers(self):
        """Kiểm thử thuật toán gom mốc giây câu hỏi audio dựa trên từ khóa '1番', '2番', '３番'"""
        print("\n--- Testing Audio Processor: Marker Segmentation ---")
        processor = AudioProcessor()

        # Giả lập segments nhận dạng được từ Whisper ASR
        mock_segments = [
            {"start": 0.0, "end": 15.0, "text": "第1回 日本語能力試験 N5 聴解テストをはじめます。"},
            {"start": 16.0, "end": 20.0, "text": "問題1。"},
            {"start": 21.0, "end": 24.0, "text": "1番。"},
            {"start": 24.5, "end": 35.0, "text": "男の人と女 của người Nhật nói chuyện."},
            {"start": 36.0, "end": 45.0, "text": "図書館へ行きます。"},
            {"start": 46.0, "end": 50.0, "text": "2番。"},
            {"start": 50.5, "end": 65.0, "text": "何番線に乗りますか。"},
            {"start": 66.0, "end": 70.0, "text": "３番。"},  # Full-width '３'
            {"start": 70.5, "end": 85.0, "text": "学生は明日何を持ってきますか。"}
        ]

        questions = processor._split_by_question_markers(mock_segments)

        print(f"Total audio questions split: {len(questions)}")
        for q in questions:
            print(f"  - Question {q['question_number']}: [{q['audio_start_time']}s -> {q['audio_end_time']}s]")

        self.assertEqual(len(questions), 3, "Phải phân tách đúng 3 câu hỏi (1番, 2番, 3番)")
        self.assertEqual(questions[0]["question_number"], 1)
        self.assertEqual(questions[1]["question_number"], 2)
        self.assertEqual(questions[2]["question_number"], 3)
        print("SUCCESS: Audio segmentation by Japanese question markers works accurately!")

    def test_audio_fallback_slicer(self):
        """Kiểm thử fallback audio processor khi không có môi trường GPU/WhisperX"""
        print("\n--- Testing Audio Processor: Fallback Slicer ---")
        processor = AudioProcessor()
        results = processor._fallback_vad_silence_slicer("dummy_path.mp3")
        self.assertIsInstance(results, list)
        self.assertGreater(len(results), 0)
        self.assertIn("question_number", results[0])
        print(f"SUCCESS: Fallback slicer returned: {results[0]}")

    def test_fastapi_endpoints_pdf_and_audio(self):
        """Kiểm thử trực tiếp qua HTTP Request tới các FastAPI Endpoints"""
        print("\n--- Testing FastAPI Endpoints for PDF & Audio ---")
        pdf_bytes = self._create_sample_exam_pdf()

        # 1. Endpoint /api/v1/ai/extract-pdf-images
        res_pdf = self.client.post(
            "/api/v1/ai/extract-pdf-images",
            files={"file": ("sample_exam.pdf", pdf_bytes, "application/pdf")}
        )
        self.assertEqual(res_pdf.status_code, 200)
        data_pdf = res_pdf.json()
        print(f"Response /extract-pdf-images: success={data_pdf['success']}, total_images={data_pdf['total_images']}")
        self.assertTrue(data_pdf["success"])
        self.assertGreaterEqual(data_pdf["total_images"], 1)

        # 2. Endpoint /api/v1/ai/process-audio
        mock_mp3 = b"ID3\x03\x00\x00\x00\x00\x00\x00FAKE_AUDIO_DATA"
        res_audio = self.client.post(
            "/api/v1/ai/process-audio",
            files={"file": ("listening_n5.mp3", mock_mp3, "audio/mpeg")}
        )
        self.assertEqual(res_audio.status_code, 200)
        data_audio = res_audio.json()
        print(f"Response /process-audio: success={data_audio['success']}, total_data={len(data_audio['data'])}")
        self.assertTrue(data_audio["success"])
        self.assertGreaterEqual(len(data_audio["data"]), 1)

        # 3. Endpoint /api/v1/ai/extract-exam
        res_exam = self.client.post(
            "/api/v1/ai/extract-exam?title=De_Thi_Thu_N5&jlpt_level=N5",
            files={
                "pdf_file": ("sample_exam.pdf", pdf_bytes, "application/pdf"),
                "audio_file": ("listening.mp3", mock_mp3, "audio/mpeg")
            }
        )
        self.assertEqual(res_exam.status_code, 200)
        data_exam = res_exam.json()
        print(f"Response /extract-exam: title={data_exam['title']}, jlpt_level={data_exam['jlpt_level']}")
        self.assertEqual(data_exam["title"], "De_Thi_Thu_N5")
        self.assertEqual(data_exam["jlpt_level"], "N5")
        print("SUCCESS: All PDF & Audio FastAPI Endpoints working perfectly!")


if __name__ == "__main__":
    unittest.main()
