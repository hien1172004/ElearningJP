"""
Bộ kiểm thử tự động cho chức năng đánh giá thứ tự nét và chiều nét Kanji.
Sử dụng thư viện unittest tiêu chuẩn của Python.
"""

import sys
import unittest
from pathlib import Path

# Đảm bảo đường dẫn import tới thư mục AI/src
AI_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(AI_DIR))
sys.path.insert(0, str(AI_DIR / "src"))

from src.schemas import (
    StrokePoint,
    ReferenceStroke,
    StrokeAssessmentRequest,
)
from src.stroke_evaluator import (
    StrokeEvaluator,
    sample_svg_path,
    resample_stroke_points,
    evaluate_stroke_direction,
)
from src.kanjivg_service import KanjiVGService


class TestKanjiStrokeEvaluator(unittest.TestCase):

    def setUp(self):
        # Dữ liệu chuẩn cho chữ "二" (gồm 2 nét ngang từ trái sang phải)
        # Nét 1: ngang ngắn ở trên
        self.ref_stroke1 = ReferenceStroke(
            stroke_number=1,
            svg_path_data="M30,30 L70,30",
            sample_points=[[30.0, 30.0], [70.0, 30.0]],
        )
        # Nét 2: ngang dài ở dưới
        self.ref_stroke2 = ReferenceStroke(
            stroke_number=2,
            svg_path_data="M15,70 L85,70",
            sample_points=[[15.0, 70.0], [85.0, 70.0]],
        )
        self.references = [self.ref_stroke1, self.ref_stroke2]

    def test_svg_path_sampling(self):
        """Kiểm thử việc lấy mẫu điểm từ chuỗi SVG Bézier của KanjiVG"""
        # Đường cong Bézier thực tế từ KanjiVG cho nét 1 chữ 二
        svg_d = "M11,54.25c3.19,0.62,6.25,0.75,9.73,0.5c20.64-1.5,50.39-5.12,68.58-5.24c3.6-0.02,5.77,0.24,7.57,0.49"
        points = sample_svg_path(svg_d, num_points_total=30)

        self.assertEqual(len(points), 30)
        start_x, start_y = points[0]
        end_x, end_y = points[-1]
        self.assertAlmostEqual(start_x, 11.0, places=1)
        self.assertAlmostEqual(start_y, 54.25, places=1)
        self.assertGreater(end_x, start_x, "Nét ngang phải có tọa độ X kết thúc lớn hơn X bắt đầu")

    def test_resample_stroke_points(self):
        """Kiểm thử hàm lấy mẫu đều các điểm theo chiều dài nét vẽ"""
        sparse_points = [(0.0, 0.0), (100.0, 0.0)]
        resampled = resample_stroke_points(sparse_points, target_count=10)

        self.assertEqual(len(resampled), 10)
        self.assertAlmostEqual(resampled[0][0], 0.0)
        self.assertAlmostEqual(resampled[-1][0], 100.0)
        # Điểm giữa phải ở khoảng 55.5 (theo 10 mốc từ 0 đến 100)
        self.assertTrue(40.0 <= resampled[5][0] <= 60.0)

    def test_perfect_writing(self):
        """Kiểm thử trường hợp người dùng viết hoàn hảo cả về thứ tự và chiều nét"""
        user_stroke1 = [StrokePoint(x=30, y=30), StrokePoint(x=50, y=30), StrokePoint(x=70, y=30)]
        user_stroke2 = [StrokePoint(x=15, y=70), StrokePoint(x=50, y=70), StrokePoint(x=85, y=70)]

        req = StrokeAssessmentRequest(
            character="二",
            user_strokes=[user_stroke1, user_stroke2],
            canvas_width=100,
            canvas_height=100,
            reference_strokes=self.references,
        )
        res = StrokeEvaluator.evaluate(req)

        self.assertTrue(res.success)
        self.assertTrue(res.is_all_correct)
        self.assertTrue(res.is_stroke_count_correct)
        self.assertGreaterEqual(res.overall_score, 90.0)
        self.assertEqual(len(res.stroke_details), 2)
        self.assertTrue(res.stroke_details[0].order_correct)
        self.assertTrue(res.stroke_details[0].is_direction_correct)
        self.assertFalse(res.stroke_details[0].is_reversed)

    def test_reversed_stroke_direction(self):
        """Kiểm thử trường hợp nét 1 bị vẽ ngược chiều (phải sang trái thay vì trái sang phải)"""
        user_stroke1_reversed = [StrokePoint(x=70, y=30), StrokePoint(x=50, y=30), StrokePoint(x=30, y=30)]
        user_stroke2_correct = [StrokePoint(x=15, y=70), StrokePoint(x=50, y=70), StrokePoint(x=85, y=70)]

        req = StrokeAssessmentRequest(
            character="二",
            user_strokes=[user_stroke1_reversed, user_stroke2_correct],
            canvas_width=100,
            canvas_height=100,
            reference_strokes=self.references,
        )
        res = StrokeEvaluator.evaluate(req)

        self.assertTrue(res.success)
        self.assertFalse(res.is_all_correct, "Không thể hoàn hảo khi có nét vẽ ngược")
        self.assertTrue(res.stroke_details[0].is_reversed, "Nét 1 phải được đánh dấu là is_reversed=True")
        self.assertLess(res.stroke_details[0].direction_score, 0.3, "Điểm chiều nét ngược phải bị phạt nặng")
        self.assertIn("ngược", res.stroke_details[0].feedback.lower())

    def test_wrong_stroke_order(self):
        """Kiểm thử trường hợp viết sai thứ tự (vẽ nét 2 trước rồi mới vẽ nét 1)"""
        user_stroke1 = [StrokePoint(x=30, y=30), StrokePoint(x=70, y=30)]
        user_stroke2 = [StrokePoint(x=15, y=70), StrokePoint(x=85, y=70)]

        # Đảo ngược thứ tự các nét gửi lên: [nét 2, nét 1]
        req = StrokeAssessmentRequest(
            character="二",
            user_strokes=[user_stroke2, user_stroke1],
            canvas_width=100,
            canvas_height=100,
            reference_strokes=self.references,
        )
        res = StrokeEvaluator.evaluate(req)

        self.assertTrue(res.success)
        self.assertFalse(res.is_all_correct)
        self.assertFalse(res.stroke_details[0].order_correct, "Nét đầu tiên gửi lên phải bị phát hiện sai thứ tự")
        self.assertIn("thứ tự", res.feedback.lower())

    def test_missing_stroke_count(self):
        """Kiểm thử trường hợp vẽ thiếu nét (chữ có 2 nét nhưng chỉ vẽ 1 nét)"""
        user_stroke1 = [StrokePoint(x=30, y=30), StrokePoint(x=70, y=30)]

        req = StrokeAssessmentRequest(
            character="二",
            user_strokes=[user_stroke1],
            canvas_width=100,
            canvas_height=100,
            reference_strokes=self.references,
        )
        res = StrokeEvaluator.evaluate(req)

        self.assertTrue(res.success)
        self.assertFalse(res.is_stroke_count_correct, "Phải phát hiện sai số lượng nét")
        self.assertEqual(res.total_strokes_received, 1)
        self.assertEqual(res.total_strokes_expected, 2)
        self.assertLess(res.overall_score, 80.0, "Bị trừ điểm do thiếu nét")

    def test_extra_stroke_count(self):
        """Kiểm thử trường hợp vẽ thừa nét (chữ có 2 nét nhưng vẽ 3 nét)"""
        user_stroke1 = [StrokePoint(x=30, y=30), StrokePoint(x=70, y=30)]
        user_stroke2 = [StrokePoint(x=15, y=70), StrokePoint(x=85, y=70)]
        user_stroke3 = [StrokePoint(x=50, y=10), StrokePoint(x=50, y=90)]

        req = StrokeAssessmentRequest(
            character="二",
            user_strokes=[user_stroke1, user_stroke2, user_stroke3],
            canvas_width=100,
            canvas_height=100,
            reference_strokes=self.references,
        )
        res = StrokeEvaluator.evaluate(req)

        self.assertTrue(res.success)
        self.assertFalse(res.is_stroke_count_correct, "Phải phát hiện thừa nét")
        self.assertEqual(res.total_strokes_received, 3)
        self.assertEqual(res.total_strokes_expected, 2)

    def test_auto_kanjivg_lookup(self):
        """Kiểm thử tự động tra cứu nét chuẩn từ KanjiVGService khi request không gửi reference_strokes"""
        # Chữ "一" (1 nét ngang)
        user_stroke1 = [StrokePoint(x=15, y=50), StrokePoint(x=50, y=50), StrokePoint(x=85, y=50)]

        req = StrokeAssessmentRequest(
            character="一",
            user_strokes=[user_stroke1],
            canvas_width=100,
            canvas_height=100,
            reference_strokes=None,  # Để trống để hệ thống tự tra cứu KanjiVG
        )
        res = StrokeEvaluator.evaluate(req)

        self.assertTrue(res.success)
        self.assertEqual(res.character, "一")
        self.assertEqual(res.total_strokes_expected, 1)
        self.assertEqual(res.total_strokes_received, 1)
        self.assertTrue(res.is_stroke_count_correct)
        self.assertGreaterEqual(res.overall_score, 80.0)

    def test_kanjivg_service_query(self):
        """Kiểm thử truy vấn từ điển KanjiVGService: Kanji và Kana, bộ lọc level JLPT"""
        service = KanjiVGService.get_instance()
        
        # 1. Truy vấn chữ Kanji thông dụng
        char_data = service.get_character("日")
        self.assertIsNotNone(char_data)
        self.assertEqual(char_data["character"], "日")
        self.assertEqual(char_data["stroke_count"], 4)
        self.assertEqual(len(char_data["strokes"]), 4)

        # 2. Truy vấn chữ Kana (Hiragana / Katakana)
        kana_data = service.get_character("あ")
        self.assertIsNotNone(kana_data)
        self.assertIn(kana_data["char_type"], ["HIRAGANA", "KATAKANA"])

        # 3. Lọc danh sách theo cấp độ N5
        n5_items = service.list_characters(level="N5", limit=50)
        self.assertGreater(len(n5_items), 0)
        for item in n5_items:
            self.assertEqual(item["jlpt_level"], "N5")

    def test_unsupported_character(self):
        """Kiểm thử trường hợp ký tự không tồn tại trong bộ dữ liệu chuẩn KanjiVG"""
        req = StrokeAssessmentRequest(
            character="🌟",
            user_strokes=[[StrokePoint(x=10, y=10), StrokePoint(x=20, y=20)]],
            canvas_width=100,
            canvas_height=100,
            reference_strokes=[],
        )
        res = StrokeEvaluator.evaluate(req)

        self.assertFalse(res.success)
        self.assertEqual(res.total_strokes_expected, 0)
        self.assertIn("Chưa có dữ liệu", res.feedback)


if __name__ == "__main__":
    unittest.main()

