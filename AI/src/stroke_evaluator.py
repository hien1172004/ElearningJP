"""
Module đánh giá thứ tự nét và chiều nét viết Kanji / Kana dựa trên KanjiVG.
Thuật toán so khớp hình học vector (Geometric Vector Matching & Arc-length Resampling).
"""

import math
import re
from typing import List, Tuple, Optional
import numpy as np

try:
    from .schemas import (
        StrokePoint,
        ReferenceStroke,
        StrokeAssessmentRequest,
        StrokeEvaluationDetail,
        StrokeAssessmentResponse,
    )
    from .kanjivg_service import KanjiVGService
except ImportError:
    try:
        from src.schemas import (
            StrokePoint,
            ReferenceStroke,
            StrokeAssessmentRequest,
            StrokeEvaluationDetail,
            StrokeAssessmentResponse,
        )
        from src.kanjivg_service import KanjiVGService
    except ImportError:
        from schemas import (
            StrokePoint,
            ReferenceStroke,
            StrokeAssessmentRequest,
            StrokeEvaluationDetail,
            StrokeAssessmentResponse,
        )
        from kanjivg_service import KanjiVGService


def sample_svg_path(d: str, num_points_total: int = 30) -> List[Tuple[float, float]]:
    """
    Phân tích chuỗi SVG path data (d attribute) từ KanjiVG và lấy mẫu các điểm (x, y).
    Hỗ trợ các lệnh chuẩn: M, m, L, l, H, h, V, v, C, c, S, s, Z, z.
    """
    tokens = re.findall(r'([a-zA-Z])|([-+]?(?:\d*\.\d+|\d+)(?:[eE][-+]?\d+)?)', d)
    points: List[Tuple[float, float]] = []

    curr_x, curr_y = 0.0, 0.0
    start_x, start_y = 0.0, 0.0

    # Gom nhóm token thành (command, [args])
    grouped = []
    current_cmd = None
    current_args = []
    for c, a in tokens:
        if c:
            if current_cmd:
                grouped.append((current_cmd, current_args))
            current_cmd = c
            current_args = []
        elif a:
            current_args.append(float(a))
    if current_cmd:
        grouped.append((current_cmd, current_args))

    for cmd, args in grouped:
        is_rel = cmd.islower()
        cmd_type = cmd.upper()
        idx = 0

        while idx < len(args) or (cmd_type == 'Z' and idx == 0):
            if cmd_type == 'M':
                nx = curr_x + args[idx] if is_rel else args[idx]
                ny = curr_y + args[idx + 1] if is_rel else args[idx + 1]
                curr_x, curr_y = nx, ny
                start_x, start_y = nx, ny
                points.append((curr_x, curr_y))
                idx += 2
                cmd_type = 'L'  # Các cặp số tiếp theo trong M coi như lệnh L
            elif cmd_type == 'L':
                nx = curr_x + args[idx] if is_rel else args[idx]
                ny = curr_y + args[idx + 1] if is_rel else args[idx + 1]
                # Lấy 5 mẫu dọc đường thẳng
                for step in range(1, 6):
                    t = step / 5.0
                    points.append((curr_x + t * (nx - curr_x), curr_y + t * (ny - curr_y)))
                curr_x, curr_y = nx, ny
                idx += 2
            elif cmd_type == 'H':
                nx = curr_x + args[idx] if is_rel else args[idx]
                points.append((nx, curr_y))
                curr_x = nx
                idx += 1
            elif cmd_type == 'V':
                ny = curr_y + args[idx] if is_rel else args[idx]
                points.append((curr_x, ny))
                curr_y = ny
                idx += 1
            elif cmd_type == 'C':
                p0 = (curr_x, curr_y)
                p1 = (curr_x + args[idx] if is_rel else args[idx], curr_y + args[idx + 1] if is_rel else args[idx + 1])
                p2 = (curr_x + args[idx + 2] if is_rel else args[idx + 2], curr_y + args[idx + 3] if is_rel else args[idx + 3])
                p3 = (curr_x + args[idx + 4] if is_rel else args[idx + 4], curr_y + args[idx + 5] if is_rel else args[idx + 5])
                # Lấy mẫu đường cong Bézier bậc 3
                for step in range(1, 11):
                    t = step / 10.0
                    bx = (1 - t) ** 3 * p0[0] + 3 * (1 - t) ** 2 * t * p1[0] + 3 * (1 - t) * t ** 2 * p2[0] + t ** 3 * p3[0]
                    by = (1 - t) ** 3 * p0[1] + 3 * (1 - t) ** 2 * t * p1[1] + 3 * (1 - t) * t ** 2 * p2[1] + t ** 3 * p3[1]
                    points.append((bx, by))
                curr_x, curr_y = p3[0], p3[1]
                idx += 6
            elif cmd_type == 'S':
                p0 = (curr_x, curr_y)
                p1 = p0
                p2 = (curr_x + args[idx] if is_rel else args[idx], curr_y + args[idx + 1] if is_rel else args[idx + 1])
                p3 = (curr_x + args[idx + 2] if is_rel else args[idx + 2], curr_y + args[idx + 3] if is_rel else args[idx + 3])
                for step in range(1, 11):
                    t = step / 10.0
                    bx = (1 - t) ** 3 * p0[0] + 3 * (1 - t) ** 2 * t * p1[0] + 3 * (1 - t) * t ** 2 * p2[0] + t ** 3 * p3[0]
                    by = (1 - t) ** 3 * p0[1] + 3 * (1 - t) ** 2 * t * p1[1] + 3 * (1 - t) * t ** 2 * p2[1] + t ** 3 * p3[1]
                    points.append((bx, by))
                curr_x, curr_y = p3[0], p3[1]
                idx += 4
            elif cmd_type == 'Z':
                points.append((start_x, start_y))
                curr_x, curr_y = start_x, start_y
                break
            else:
                break

    if not points:
        return [(0.0, 0.0), (1.0, 1.0)]

    return resample_stroke_points(points, num_points_total)


def resample_stroke_points(points: List[Tuple[float, float]], target_count: int = 30) -> List[Tuple[float, float]]:
    """
    Lấy mẫu lại một nét vẽ thành target_count điểm cách đều nhau theo độ dài cung nét vẽ (Arc-length resampling).
    """
    if len(points) == 0:
        return [(0.0, 0.0)] * target_count
    if len(points) == 1:
        return [points[0]] * target_count

    pts = np.array(points, dtype=np.float64)
    # Loại bỏ các điểm trùng lặp liên tiếp
    diffs = np.diff(pts, axis=0)
    dists = np.hypot(diffs[:, 0], diffs[:, 1])
    valid_mask = np.insert(dists > 1e-4, 0, True)
    pts = pts[valid_mask]

    if len(pts) < 2:
        return [tuple(pts[0])] * target_count

    # Tính khoảng cách tích lũy dọc theo nét vẽ
    seg_dists = np.hypot(np.diff(pts[:, 0]), np.diff(pts[:, 1]))
    cumulative_dist = np.insert(np.cumsum(seg_dists), 0, 0.0)
    total_length = cumulative_dist[-1]

    if total_length < 1e-4:
        return [tuple(pts[0])] * target_count

    # Các mốc khoảng cách đều
    target_dists = np.linspace(0.0, total_length, target_count)
    resampled_x = np.interp(target_dists, cumulative_dist, pts[:, 0])
    resampled_y = np.interp(target_dists, cumulative_dist, pts[:, 1])

    return list(zip(resampled_x.tolist(), resampled_y.tolist()))


def get_direction_description(dx: float, dy: float) -> str:
    """
    Sinh mô tả ngôn ngữ tự nhiên cho hướng vector (dx, dy).
    Ví dụ: 'từ trái sang phải', 'từ trên xuống dưới', 'từ trên-trái xuống dưới-phải'.
    """
    length = math.hypot(dx, dy)
    if length < 1e-4:
        return "tại chỗ (chấm điểm)"

    angle = math.atan2(dy, dx) * 180.0 / math.pi  # -180 đến 180 (trục Y hướng xuống dưới trong SVG)

    if -22.5 <= angle < 22.5:
        return "từ trái sang phải (ngang)"
    elif 22.5 <= angle < 67.5:
        return "từ trên-trái xuống dưới-phải (chéo xuống phải)"
    elif 67.5 <= angle < 112.5:
        return "từ trên xuống dưới (sổ thẳng)"
    elif 112.5 <= angle < 157.5:
        return "từ trên-phải xuống dưới-trái (chéo xuống trái)"
    elif angle >= 157.5 or angle < -157.5:
        return "từ phải sang trái (ngang ngược)"
    elif -157.5 <= angle < -112.5:
        return "từ dưới-phải lên trên-trái"
    elif -112.5 <= angle < -67.5:
        return "từ dưới lên trên (sổ ngược)"
    else:
        return "từ dưới-trái lên trên-phải"


def normalize_user_strokes(
    user_strokes: List[List[StrokePoint]],
    canvas_width: Optional[float] = None,
    canvas_height: Optional[float] = None,
    target_viewbox: float = 109.0,
) -> List[List[Tuple[float, float]]]:
    """
    Chuẩn hóa các nét vẽ của người dùng về hệ tọa độ chuẩn KanjiVG (109x109).
    """
    if not user_strokes:
        return []

    # Nếu người dùng truyền kích thước canvas thực tế
    if canvas_width and canvas_height and canvas_width > 0 and canvas_height > 0:
        scale_x = target_viewbox / canvas_width
        scale_y = target_viewbox / canvas_height
        norm_strokes = []
        for stroke in user_strokes:
            norm_strokes.append([(p.x * scale_x, p.y * scale_y) for p in stroke if p is not None])
        return norm_strokes

    # Nếu không có canvas width/height, tự động tìm bounding box
    all_x = [p.x for stroke in user_strokes for p in stroke]
    all_y = [p.y for stroke in user_strokes for p in stroke]

    if not all_x or not all_y:
        return []

    min_x, max_x = min(all_x), max(all_x)
    min_y, max_y = min(all_y), max(all_y)
    bbox_w = max_x - min_x
    bbox_h = max_y - min_y

    scale = (target_viewbox * 0.85) / max(bbox_w, bbox_h, 1.0)
    offset_x = (target_viewbox - bbox_w * scale) / 2.0
    offset_y = (target_viewbox - bbox_h * scale) / 2.0

    norm_strokes = []
    for stroke in user_strokes:
        norm_stroke = [((p.x - min_x) * scale + offset_x, (p.y - min_y) * scale + offset_y) for p in stroke]
        norm_strokes.append(norm_stroke)

    return norm_strokes


def evaluate_stroke_direction(
    user_pts: List[Tuple[float, float]],
    ref_pts: List[Tuple[float, float]],
) -> Tuple[float, bool, bool, float, str, str]:
    """
    Đánh giá chiều của một nét vẽ so với nét chuẩn.
    Trả về:
    - direction_score (0.0 - 1.0)
    - is_direction_correct (bool)
    - is_reversed (bool)
    - angle_deg (float)
    - user_dir_label (str)
    - ref_dir_label (str)
    """
    # Vector tổng thể nét vẽ (từ điểm bắt đầu đến điểm kết thúc)
    u_dx = user_pts[-1][0] - user_pts[0][0]
    u_dy = user_pts[-1][1] - user_pts[0][1]

    r_dx = ref_pts[-1][0] - ref_pts[0][0]
    r_dy = ref_pts[-1][1] - ref_pts[0][1]

    ref_dir_label = get_direction_description(r_dx, r_dy)
    user_dir_label = get_direction_description(u_dx, u_dy)

    u_norm = math.hypot(u_dx, u_dy)
    r_norm = math.hypot(r_dx, r_dy)

    # Nếu cả 2 đều là nét chấm rất ngắn (chấm điểm)
    if u_norm < 3.0 and r_norm < 3.0:
        return 1.0, True, False, 0.0, user_dir_label, ref_dir_label

    if u_norm < 1e-4 or r_norm < 1e-4:
        return 0.5, True, False, 0.0, user_dir_label, ref_dir_label

    # Cosine Similarity giữa vector người dùng và vector chuẩn
    cos_theta = (u_dx * r_dx + u_dy * r_dy) / (u_norm * r_norm)
    cos_theta = max(-1.0, min(1.0, cos_theta))
    angle_deg = math.degrees(math.acos(cos_theta))

    # Phát hiện vẽ ngược chiều (góc lệch > 115 độ)
    is_reversed = angle_deg > 115.0
    is_direction_correct = angle_deg <= 50.0

    if is_reversed:
        # Ngược chiều thì điểm chiều rất thấp
        direction_score = max(0.0, 0.2 * (180.0 - angle_deg) / 65.0)
    else:
        # Càng gần 0 độ thì điểm càng cao (từ 0.5 đến 1.0)
        direction_score = max(0.0, 1.0 - (angle_deg / 90.0) * 0.6)

    direction_score = round(float(direction_score), 2)
    return direction_score, is_direction_correct, is_reversed, angle_deg, user_dir_label, ref_dir_label


def evaluate_stroke_shape_and_position(
    user_pts: List[Tuple[float, float]],
    ref_pts: List[Tuple[float, float]],
) -> float:
    """
    Tính điểm hình dáng và vị trí nét dựa trên khoảng cách Euclidean trung bình giữa các điểm lấy mẫu.
    """
    u_arr = np.array(user_pts)
    r_arr = np.array(ref_pts)

    # Trung bình khoảng cách Euclidean giữa các cặp điểm tương ứng
    distances = np.hypot(u_arr[:, 0] - r_arr[:, 0], u_arr[:, 1] - r_arr[:, 1])
    mean_dist = float(np.mean(distances))

    # Trong viewBox 109x109, lệch dưới 8px coi như rất đẹp (>0.9), lệch trên 35px thì điểm thấp
    score = max(0.0, min(1.0, 1.0 - (mean_dist / 35.0)))
    return round(score, 2)


class StrokeEvaluator:
    """
    Bộ máy đánh giá thứ tự nét và chiều nét Kanji.
    """

    @staticmethod
    def evaluate(request: StrokeAssessmentRequest) -> StrokeAssessmentResponse:
        char = request.character
        raw_user_strokes = request.user_strokes
        ref_strokes_input = request.reference_strokes or []

        # Tự động tra cứu KanjiVGService nếu người dùng không gửi kèm reference_strokes
        if not ref_strokes_input and char:
            char_data = KanjiVGService.get_instance().get_character(char)
            if char_data and char_data.get("strokes"):
                ref_strokes_input = [
                    ReferenceStroke(
                        stroke_number=s.get("stroke_number", idx + 1),
                        svg_path_data=s.get("svg_path_data", "")
                    )
                    for idx, s in enumerate(char_data["strokes"])
                ]

        # 1. Trích xuất các nét chuẩn (từ SVG path hoặc sample points)
        reference_strokes_pts: List[List[Tuple[float, float]]] = []
        for ref in ref_strokes_input:
            if ref.sample_points and len(ref.sample_points) >= 2:
                pts = [(pt[0], pt[1]) for pt in ref.sample_points]
                reference_strokes_pts.append(resample_stroke_points(pts, 30))
            elif ref.svg_path_data:
                reference_strokes_pts.append(sample_svg_path(ref.svg_path_data, 30))

        total_expected = len(reference_strokes_pts)
        total_received = len(raw_user_strokes)

        if total_expected == 0:
            return StrokeAssessmentResponse(
                success=False,
                character=char,
                total_strokes_expected=0,
                total_strokes_received=total_received,
                is_stroke_count_correct=False,
                is_all_correct=False,
                overall_score=0.0,
                feedback=f"Chưa có dữ liệu nét chuẩn (KanjiVG) cho ký tự '{char}'.",
                stroke_details=[],
            )

        # 2. Chuẩn hóa nét vẽ người dùng về viewBox 109x109
        norm_user_strokes = normalize_user_strokes(
            raw_user_strokes,
            request.canvas_width,
            request.canvas_height,
            target_viewbox=109.0,
        )

        resampled_user_strokes = [
            resample_stroke_points(st, 30) for st in norm_user_strokes if len(st) > 0
        ]

        # 3. Phân tích chi tiết từng nét
        stroke_details: List[StrokeEvaluationDetail] = []
        all_order_correct = True
        all_direction_correct = True
        total_score_acc = 0.0

        evaluated_count = min(len(resampled_user_strokes), total_expected)

        for i in range(evaluated_count):
            user_pts = resampled_user_strokes[i]
            ref_pts = reference_strokes_pts[i]
            stroke_num = i + 1  # 1-based index

            # Đánh giá chiều nét
            dir_score, is_dir_ok, is_rev, angle, user_lbl, ref_lbl = evaluate_stroke_direction(user_pts, ref_pts)

            # Đánh giá hình dáng & vị trí
            shape_sc = evaluate_stroke_shape_and_position(user_pts, ref_pts)

            # Kiểm tra thứ tự nét: So khớp xem nét người dùng vừa vẽ gần với nét chuẩn nào nhất
            best_match_idx = i
            best_match_dist = float('inf')
            for r_idx, r_pts in enumerate(reference_strokes_pts):
                d = float(np.mean(np.hypot(np.array(user_pts)[:, 0] - np.array(r_pts)[:, 0],
                                           np.array(user_pts)[:, 1] - np.array(r_pts)[:, 1])))
                if d < best_match_dist:
                    best_match_dist = d
                    best_match_idx = r_idx

            is_order_ok = (best_match_idx == i)
            if not is_order_ok:
                all_order_correct = False

            if not is_dir_ok:
                all_direction_correct = False

            # Tạo lời nhận xét sư phạm chi tiết
            feedbacks = []
            if not is_order_ok:
                feedbacks.append(f"Sai thứ tự nét: Nét bạn vừa vẽ giống nét số {best_match_idx + 1} hơn là nét số {stroke_num}.")
            else:
                feedbacks.append(f"Đúng thứ tự nét {stroke_num}.")

            if is_rev:
                feedbacks.append(f"Vẽ ngược chiều! Chiều chuẩn cần vẽ là {ref_lbl}, bạn đã vẽ {user_lbl} (lệch {angle:.0f}°).")
            elif not is_dir_ok:
                feedbacks.append(f"Hướng nét chưa chuẩn: Cần vẽ {ref_lbl} (lệch {angle:.0f}°).")
            else:
                feedbacks.append(f"Đúng chiều nét ({ref_lbl}).")

            combined_feedback = " ".join(feedbacks)

            # Điểm từng nét (Trọng số: Chiều 45%, Thứ tự 35%, Hình dáng 20%)
            order_weight = 1.0 if is_order_ok else 0.3
            single_stroke_score = (dir_score * 0.45 + order_weight * 0.35 + shape_sc * 0.20) * 100.0
            total_score_acc += single_stroke_score

            stroke_details.append(
                StrokeEvaluationDetail(
                    stroke_number=stroke_num,
                    user_stroke_index=i,
                    order_correct=is_order_ok,
                    direction_score=dir_score,
                    is_direction_correct=is_dir_ok,
                    is_reversed=is_rev,
                    shape_score=shape_sc,
                    feedback=combined_feedback,
                )
            )

        # Tính điểm tổng thể
        count_correct = (total_received == total_expected)
        base_overall = (total_score_acc / total_expected) if total_expected > 0 else 0.0

        # Nếu vẽ thiếu hoặc thừa nét thì trừ điểm tỷ lệ
        if not count_correct:
            penalty = abs(total_received - total_expected) * 15.0
            final_overall = max(0.0, base_overall - penalty)
        else:
            final_overall = base_overall

        final_overall = round(final_overall, 1)
        is_all_perfect = count_correct and all_order_correct and all_direction_correct and (final_overall >= 80.0)

        # Đánh giá tổng quan
        if is_all_perfect:
            overall_fb = f"Xuất sắc! Bạn đã viết chữ '{char}' đúng chuẩn 100% về cả số nét, thứ tự và chiều nét."
        elif not count_correct:
            overall_fb = f"Chữ '{char}' có chuẩn {total_expected} nét, nhưng bạn đã vẽ {total_received} nét. Vui lòng vẽ đúng số lượng nét."
        elif not all_order_correct:
            overall_fb = "Bạn đã viết sai thứ tự một số nét. Hãy chú ý quy tắc viết nét nào trước, nét nào sau."
        elif not all_direction_correct:
            overall_fb = "Bạn đã viết đúng thứ tự nhưng có nét bị vẽ ngược chiều (ví dụ: từ dưới lên hoặc từ phải sang trái)."
        else:
            overall_fb = f"Tốt! Nét vẽ khá chuẩn, tiếp tục luyện tập để chữ cân đối hơn."

        return StrokeAssessmentResponse(
            success=True,
            character=char,
            total_strokes_expected=total_expected,
            total_strokes_received=total_received,
            is_stroke_count_correct=count_correct,
            is_all_correct=is_all_perfect,
            overall_score=final_overall,
            feedback=overall_fb,
            stroke_details=stroke_details,
        )
