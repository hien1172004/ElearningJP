#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
crop_jlpt.py — Script trích xuất ảnh đáp án và ảnh minh họa từ đề thi nghe JLPT (N1-N5) dạng PDF.

Thuật toán:
  1. Trích xuất ảnh gốc từng trang: Dùng ảnh nhúng nếu phủ >= 80% trang (đề scan/flatten),
     hoặc render qua PyMuPDF (dpi=200). Lọc bỏ mask rác.
  2. Tiền xử lý: Grayscale, ngưỡng nhị phân đảo (Thresholding), Morphology CLOSE nối nét.
  3. Phát hiện khung chữ nhật: cv2.findContours + approxPolyDP 4 đỉnh.
  4. Lọc khung trùng (viền kép trong/ngoài).
  5. Lọc khung lồng nhau (loại khung con, giữ khung ngoài cùng).
  6. Sắp xếp thứ tự đọc (gom nhóm theo hàng y, sắp xếp x tăng dần).
  7. Gom nhóm theo bố cục: Lưới 2x2 (choices: 4 đáp án 1-2-3-4), đơn lẻ (single), còn lại (unknown).
  8. Cắt ảnh thụt viền (margin) và xuất file PNG + manifest.json.
"""

import os
import sys
import re
import json
import argparse
import logging
from typing import List, Tuple, Dict, Any, Optional

import numpy as np
import cv2
try:
    import pymupdf as fitz
except ImportError:
    import fitz

# ==============================================================================
# HẰNG SỐ CẤU HÌNH NGƯỠNG (TÍNH THEO TỈ LỆ KÍCH THƯỚC TRANG)
# ==============================================================================
PAGE_IMG_COVERAGE_RATIO = 0.80   # Tỉ lệ diện tích ảnh nhúng so với trang để coi là trang scan
PAGE_RENDER_DPI = 200            # DPI khi render trang vector / không có ảnh toàn trang
MASK_MIN_DIM_RATIO = 0.05        # Ngưỡng kích thước tối thiểu để bỏ qua ảnh mask rác

# Ngưỡng kích thước khung chữ nhật hợp lệ so với kích thước trang
MIN_WIDTH_RATIO = 0.15           # Rộng tối thiểu 15% bề rộng trang
MAX_WIDTH_RATIO = 0.95           # Rộng tối đa 95% bề rộng trang
MIN_HEIGHT_RATIO = 0.06          # Cao tối thiểu 6% bề cao trang
MAX_HEIGHT_RATIO = 0.90          # Cao tối đa 90% bề cao trang
APPROX_POLY_EPSILON = 0.02       # Epsilon cho approxPolyDP (2% chu vi)

# Ngưỡng lọc khung trùng (viền kép) và khung lồng
DUP_CENTER_TOL_X = 0.03          # Dung sai lệch tâm X (3% bề rộng trang)
DUP_CENTER_TOL_Y = 0.03          # Dung sai lệch tâm Y (3% bề cao trang)
DUP_SIZE_TOL_W = 0.05            # Dung sai lệch kích thước W (5% bề rộng trang)
DUP_SIZE_TOL_H = 0.05            # Dung sai lệch kích thước H (5% bề cao trang)

# Ngưỡng gom hàng (dòng) theo trục Y
ROW_Y_TOLERANCE_RATIO = 0.035    # Dung sai y giữa các khung cùng hàng (3.5% bề cao trang)
GRID_SIZE_TOLERANCE = 0.20       # Chênh lệch kích thước tối đa giữa 4 khung trong lưới 2x2 (20%)

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger("crop_jlpt")


# ==============================================================================
# BƯỚC 1: LẤY ẢNH NGUỒN CỦA TỪNG TRANG
# ==============================================================================
def get_page_image(page: fitz.Page, doc: fitz.Document) -> np.ndarray:
    """
    Lấy ảnh nguồn của trang:
    - Nếu trang có 1 ảnh lớn phủ >= 80% diện tích trang (đề scan/flatten), dùng trực tiếp ảnh đó.
    - Ngược lại (trang vector, ảnh rời rạc), render toàn bộ trang bằng get_pixmap(dpi=200).
    - Tự động bỏ qua các ảnh mask rác (kích thước < 5% kích thước trang).
    """
    pw, ph = page.rect.width, page.rect.height
    image_infos = page.get_image_info(xrefs=True)
    best_pix: Optional[fitz.Pixmap] = None

    for info in image_infos:
        xref = info.get("xref")
        if not xref:
            continue
        bbox = info.get("bbox")
        if not bbox:
            continue
        bw = bbox[2] - bbox[0]
        bh = bbox[3] - bbox[1]

        # Bỏ qua ảnh mask rác
        if bw < MASK_MIN_DIM_RATIO * pw or bh < MASK_MIN_DIM_RATIO * ph:
            continue

        # Kiểm tra diện tích phủ so với diện tích trang PDF
        area_ratio = (bw * bh) / (pw * ph)
        if area_ratio >= PAGE_IMG_COVERAGE_RATIO:
            try:
                pix = fitz.Pixmap(doc, xref)
                # Nếu không phải RGB tiêu chuẩn thì chuyển sang RGB
                if pix.n - pix.alpha > 3:
                    pix = fitz.Pixmap(fitz.csRGB, pix)
                best_pix = pix
                break
            except Exception as e:
                logger.debug(f"Không thể giải nén Pixmap xref={xref}: {e}")

    # Fallback: Render toàn trang nếu không có ảnh scan bao phủ
    if best_pix is None:
        best_pix = page.get_pixmap(dpi=PAGE_RENDER_DPI)

    # Chuyển Pixmap sang định dạng numpy BGR của OpenCV
    img_data = np.frombuffer(best_pix.samples, dtype=np.uint8)
    res_bgr = None
    if best_pix.n == 4:
        img_np = img_data.reshape((best_pix.height, best_pix.width, 4))
        res_bgr = cv2.cvtColor(img_np, cv2.COLOR_RGBA2BGR)
    elif best_pix.n == 1:
        img_np = img_data.reshape((best_pix.height, best_pix.width))
        res_bgr = cv2.cvtColor(img_np, cv2.COLOR_GRAY2BGR)
    elif best_pix.n == 3:
        img_np = img_data.reshape((best_pix.height, best_pix.width, 3))
        res_bgr = cv2.cvtColor(img_np, cv2.COLOR_RGB2BGR)
    else:
        rgb_pix = fitz.Pixmap(fitz.csRGB, best_pix)
        img_np = np.frombuffer(rgb_pix.samples, dtype=np.uint8).reshape((rgb_pix.height, rgb_pix.width, 3))
        res_bgr = cv2.cvtColor(img_np, cv2.COLOR_RGB2BGR)

    # Đảm bảo nền giấy luôn là màu sáng: nếu ảnh scan bị âm bản / đảo màu (mean < 127) thì đảo lại
    if res_bgr is not None and np.mean(res_bgr) < 127:
        res_bgr = 255 - res_bgr

    return res_bgr


# ==============================================================================
# BƯỚC 2 & 3: TIỀN XỬ LÝ VÀ PHÁT HIỆN CÁC KHUNG CHỮ NHẬT ỨNG VIÊN
# ==============================================================================
def find_candidate_boxes(img: np.ndarray, threshold_val: int = 0) -> List[Tuple[int, int, int, int]]:
    """
    Chuyển xám, nhị phân hóa (mặc định dùng Otsu tự thích ứng threshold_val <= 0),
    morphology CLOSE nối viền đứt, sau đó tìm các contour dạng chữ nhật thỏa mãn tỉ lệ kích thước.
    Nếu threshold_val > 0 nhưng phát hiện ít hơn 4 khung, tự động fallback sang Otsu để bắt trọn viền mờ.
    """
    h, w = img.shape[:2]
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    if np.mean(gray) < 127:
        gray = 255 - gray

    def _get_rects_with_threshold(thresh_img: np.ndarray) -> List[Tuple[int, int, int, int]]:
        kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (3, 3))
        closed = cv2.morphologyEx(thresh_img, cv2.MORPH_CLOSE, kernel)
        contours, _ = cv2.findContours(closed, cv2.RETR_LIST, cv2.CHAIN_APPROX_SIMPLE)

        rects = []
        for cnt in contours:
            peri = cv2.arcLength(cnt, True)
            approx = cv2.approxPolyDP(cnt, APPROX_POLY_EPSILON * peri, True)

            # Kiểm tra đúng 4 đỉnh và lồi (convex)
            if len(approx) == 4 and cv2.isContourConvex(approx):
                x, y, rw, rh = cv2.boundingRect(approx)
                if (MIN_WIDTH_RATIO * w <= rw <= MAX_WIDTH_RATIO * w and
                    MIN_HEIGHT_RATIO * h <= rh <= MAX_HEIGHT_RATIO * h):
                    rects.append((x, y, rw, rh))
        return rects

    # 1. Nhị phân hóa ban đầu (Otsu nếu <= 0, cố định nếu > 0)
    if threshold_val <= 0:
        _, thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
        return _get_rects_with_threshold(thresh)
    else:
        _, thresh = cv2.threshold(gray, threshold_val, 255, cv2.THRESH_BINARY_INV)
        cand = _get_rects_with_threshold(thresh)
        # Nếu ngưỡng cố định tìm được ít hơn 4 khung, tự động kích hoạt Otsu để cứu các nét in mờ
        if len(cand) < 4:
            _, otsu_thresh = cv2.threshold(gray, 0, 255, cv2.THRESH_BINARY_INV + cv2.THRESH_OTSU)
            otsu_cand = _get_rects_with_threshold(otsu_thresh)
            if len(otsu_cand) > len(cand):
                return otsu_cand
        return cand


# ==============================================================================
# BƯỚC 4 & 5: LỌC TRÙNG (VIỀN KÉP) VÀ LỌC KHUNG LỒNG NHAU
# ==============================================================================
def filter_boxes(rects: List[Tuple[int, int, int, int]], page_w: int, page_h: int) -> List[Tuple[int, int, int, int]]:
    """
    1. Loại bỏ viền trùng (viền trong / viền ngoài của cùng 1 khung): giữ khung ngoài.
    2. Loại bỏ khung nằm trọn bên trong khung lớn hơn (ví dụ khung tổng chứa các ảnh con).
    """
    tol_cx = DUP_CENTER_TOL_X * page_w
    tol_cy = DUP_CENTER_TOL_Y * page_h
    tol_w = DUP_SIZE_TOL_W * page_w
    tol_h = DUP_SIZE_TOL_H * page_h

    # Sắp xếp diện tích giảm dần để xử lý khung lớn trước
    sorted_by_area = sorted(rects, key=lambda r: r[2] * r[3], reverse=True)
    unique_rects: List[Tuple[int, int, int, int]] = []

    for r in sorted_by_area:
        x, y, rw, rh = r
        cx, cy = x + rw / 2.0, y + rh / 2.0
        is_dup = False

        for u in unique_rects:
            ux, uy, uw, uh = u
            ucx, ucy = ux + uw / 2.0, uy + uh / 2.0

            # Bước 4: Kiểm tra khung trùng (tâm lệch nhỏ và kích thước gần bằng nhau)
            if (abs(cx - ucx) <= tol_cx and abs(cy - ucy) <= tol_cy and
                abs(rw - uw) <= tol_w and abs(rh - uh) <= tol_h):
                is_dup = True
                break

            # Bước 5: Kiểm tra r có nằm trọn bên trong khung u lớn hơn không
            if (ux <= x + tol_cx and uy <= y + tol_cy and
                (ux + uw) >= (x + rw - tol_cx) and (uy + uh) >= (y + rh - tol_cy)):
                is_dup = True
                break

        if not is_dup:
            unique_rects.append(r)

    return unique_rects


# ==============================================================================
# BƯỚC 5.5: TỰ ĐỘNG BÙ ĐẮP HÌNH HỌC CHO LƯỚI 2x2 KHI BỊ MẤT NÉT IN / SCAN (3/4 KHUNG)
# ==============================================================================
def complete_missing_2x2_boxes(
    rects: List[Tuple[int, int, int, int]],
    page_w: int,
    page_h: int,
    tol_size: float = 0.25,
    tol_align: float = 0.06,
) -> List[Tuple[int, int, int, int]]:
    """
    Nếu một trang scan đề thi có lưới 2x2 gồm 4 phương án nhưng 1 khung bị mờ nét viền
    (ví dụ đáy tranh 4 bị mờ nên chỉ phát hiện được 3 khung 1, 2, 3):
    Hàm sẽ tự động phân tích quan hệ hình học đối xứng và phục hồi khung thứ 4 còn lại.
    """
    if len(rects) < 3:
        return rects

    res = list(rects)
    synthesized: List[Tuple[int, int, int, int]] = []
    n = len(rects)

    for i in range(n):
        for j in range(i + 1, n):
            for k in range(j + 1, n):
                triplet = [rects[i], rects[j], rects[k]]
                widths = [r[2] for r in triplet]
                heights = [r[3] for r in triplet]
                avg_w = sum(widths) / 3.0
                avg_h = sum(heights) / 3.0

                if (max(widths) - min(widths)) > tol_size * avg_w or (max(heights) - min(heights)) > tol_size * avg_h:
                    continue

                xs = sorted([r[0] for r in triplet])
                ys = sorted([r[1] for r in triplet])

                x_groups: List[List[int]] = []
                for x in xs:
                    if not x_groups or abs(x - sum(x_groups[-1]) / len(x_groups)) > tol_align * page_w:
                        x_groups.append([x])
                    else:
                        x_groups[-1].append(x)

                y_groups: List[List[int]] = []
                for y in ys:
                    if not y_groups or abs(y - sum(y_groups[-1]) / len(y_groups)) > tol_align * page_h:
                        y_groups.append([y])
                    else:
                        y_groups[-1].append(y)

                if len(x_groups) == 2 and len(y_groups) == 2:
                    x_left = sum(x_groups[0]) / len(x_groups[0])
                    x_right = sum(x_groups[1]) / len(x_groups[1])
                    y_top = sum(y_groups[0]) / len(y_groups[0])
                    y_bottom = sum(y_groups[1]) / len(y_groups[1])

                    all_4 = [
                        (int(round(x_left)), int(round(y_top))),
                        (int(round(x_right)), int(round(y_top))),
                        (int(round(x_left)), int(round(y_bottom))),
                        (int(round(x_right)), int(round(y_bottom)))
                    ]

                    for tx, ty in all_4:
                        matched = any(
                            abs(r[0] - tx) <= tol_align * page_w and abs(r[1] - ty) <= tol_align * page_h
                            for r in (res + synthesized)
                        )
                        if not matched:
                            new_box = (tx, ty, int(round(avg_w)), int(round(avg_h)))
                            synthesized.append(new_box)
                            logger.info(f"🧩 [2x2 AUTO-HEAL] Đã tự động bù đắp khung ảnh thứ 4 bị mờ viền: {new_box}")

    return res + synthesized


# ==============================================================================
# BƯỚC 6: SẮP XẾP THEO THỨ TỰ ĐỌC (HÀNG Y RỒI ĐẾN CỘT X)
# ==============================================================================
def sort_boxes_into_rows(rects: List[Tuple[int, int, int, int]], page_h: int) -> List[List[Tuple[int, int, int, int]]]:
    """
    Gom các khung có tọa độ y gần nhau vào cùng một hàng (dung sai theo tỉ lệ chiều cao trang).
    Trong mỗi hàng sắp xếp theo x tăng dần.
    Các hàng được sắp xếp theo y tăng dần.
    """
    if not rects:
        return []

    sorted_y = sorted(rects, key=lambda r: r[1])
    rows: List[List[Tuple[int, int, int, int]]] = []
    row_tol = ROW_Y_TOLERANCE_RATIO * page_h

    for r in sorted_y:
        placed = False
        for row in rows:
            avg_y = sum(item[1] for item in row) / len(row)
            if abs(r[1] - avg_y) <= row_tol:
                row.append(r)
                placed = True
                break
        if not placed:
            rows.append([r])

    # Sắp xếp các khung trong mỗi hàng theo x tăng dần
    for row in rows:
        row.sort(key=lambda r: r[0])

    # Sắp xếp các hàng theo y trung bình tăng dần
    rows.sort(key=lambda row: sum(item[1] for item in row) / len(row))
    return rows


# ==============================================================================
# BƯỚC 7: GOM NHÓM TỰ ĐỘNG THEO BỐ CỤC
# ==============================================================================
def group_layout(
    rows: List[List[Tuple[int, int, int, int]]],
    page_w: int,
    page_h: int
) -> List[Tuple[str, List[Tuple[int, int, int, int]]]]:
    """
    Phân tích bố cục các hàng:
    - Nếu 2 hàng liên tiếp mỗi hàng có đúng 2 khung và kích thước gần bằng nhau -> nhóm 'choices' (lưới 2x2, thứ tự 1,2,3,4).
    - Nếu một hàng chỉ có 1 khung đơn lẻ -> nhóm 'single'.
    - Các trường hợp còn lại -> nhóm 'unknown'.
    """
    groups: List[Tuple[str, List[Tuple[int, int, int, int]]]] = []
    i = 0
    num_rows = len(rows)

    while i < num_rows:
        # Kiểm tra lưới 2x2 (choices)
        if i + 1 < num_rows and len(rows[i]) == 2 and len(rows[i + 1]) == 2:
            r1, r2 = rows[i], rows[i + 1]
            all_4 = [r1[0], r1[1], r2[0], r2[1]]
            widths = [b[2] for b in all_4]
            heights = [b[3] for b in all_4]
            w_diff = max(widths) - min(widths)
            h_diff = max(heights) - min(heights)

            if w_diff <= GRID_SIZE_TOLERANCE * max(widths) and h_diff <= GRID_SIZE_TOLERANCE * max(heights):
                groups.append(("choices", all_4))
                i += 2
                continue

        # Kiểm tra khung đơn lẻ (single)
        if len(rows[i]) == 1:
            groups.append(("single", rows[i]))
            i += 1
            continue

        # Các trường hợp không khớp quy tắc chuẩn
        groups.append(("unknown", rows[i]))
        i += 1

    return groups


def safe_cv2_imwrite(file_path: str, img: np.ndarray) -> bool:
    """
    Lưu ảnh an toàn bằng cv2.imencode + open() để hỗ trợ hoàn hảo
    các đường dẫn chứa ký tự Unicode / tiếng Việt trên hệ điều hành Windows.
    """
    ext = os.path.splitext(file_path)[1]
    success, enc = cv2.imencode(ext if ext else ".png", img)
    if success:
        with open(file_path, "wb") as f:
            f.write(enc.tobytes())
        return True
    return False


# ==============================================================================
# BƯỚC 8: CẮT ẢNH THỤT VIỀN (MARGIN) VÀ LƯU FILE
# ==============================================================================
def crop_and_save_boxes(
    img: np.ndarray,
    groups: List[Tuple[str, List[Tuple[int, int, int, int]]]],
    page_num: int,
    output_dir: str,
    margin_ratio: float = 0.005,
    page_question_map: Optional[List[str]] = None,
    prefix_id: str = "",
) -> List[Dict[str, Any]]:
    """
    Cắt từng khung ảnh thụt viền theo tỉ lệ margin, lưu thành file PNG và trả về thông tin cho manifest.
    Nếu có prefix_id, thêm trực tiếp vào tên file khi lưu đĩa để đồng bộ hoàn toàn với manifest.
    """
    h, w = img.shape[:2]
    margin_px = int(round(margin_ratio * w))
    page_manifest = []

    for g_idx, (g_type, boxes) in enumerate(groups, start=1):
        # Xác định tiền tố tên file theo question_map (nếu có)
        qid_prefix: Optional[str] = None
        if page_question_map and (g_idx - 1) < len(page_question_map):
            qid_prefix = page_question_map[g_idx - 1]

        type_code = "c" if g_type == "choices" else ("s" if g_type == "single" else "u")
        saved_files = []
        bboxes_list = []

        h_img, w_img = img.shape[:2]
        for b_idx, (bx, by, bw, bh) in enumerate(boxes, start=1):
            # Thụt vào một khoảng margin để bỏ viền đen và kẹp trong khung ảnh
            cx1 = max(0, min(bx + margin_px, w_img - 1))
            cy1 = max(0, min(by + margin_px, h_img - 1))
            cx2 = min(w_img, max(bx + bw - margin_px, cx1 + 1))
            cy2 = min(h_img, max(by + bh - margin_px, cy1 + 1))

            cropped = img[cy1:cy2, cx1:cx2]
            if cropped is None or cropped.size == 0:
                continue

            # Đặt tên file (kèm prefix_id trực tiếp khi lưu file thật)
            if qid_prefix:
                raw_filename = f"{qid_prefix}_{type_code}{b_idx}.png"
            else:
                raw_filename = f"p{page_num:02d}_g{g_idx:02d}_{type_code}{b_idx}.png"

            filename = f"{prefix_id}_{raw_filename}" if prefix_id else raw_filename

            file_path = os.path.join(output_dir, filename)
            safe_cv2_imwrite(file_path, cropped)
            saved_files.append(filename)
            bboxes_list.append([bx, by, bw, bh])

        min_y = min(b[1] for b in boxes) if boxes else 0
        y_ratio = round(min_y / h, 4) if h > 0 else 0.0

        group_meta: Dict[str, Any] = {
            "page": page_num,
            "page_width": w,
            "page_height": h,
            "group_index": g_idx,
            "type": g_type,
            "y_ratio": y_ratio,
            "images": saved_files,
            "bboxes": bboxes_list,
        }
        if qid_prefix:
            group_meta["question_id"] = qid_prefix

        page_manifest.append(group_meta)

    return page_manifest


# ==============================================================================
# HÀM VẼ ẢNH DEBUG KIỂM TRA BẰNG MẮT
# ==============================================================================
def save_debug_image(
    img: np.ndarray,
    groups: List[Tuple[str, List[Tuple[int, int, int, int]]]],
    page_num: int,
    output_dir: str
):
    """Vẽ bounding box và nhãn từng nhóm lên ảnh trang để kiểm tra trực quan."""
    debug_img = img.copy()
    color_map = {
        "choices": (0, 180, 0),    # Xanh lá
        "single": (220, 100, 0),    # Xanh dương / Cam
        "unknown": (0, 0, 220),     # Đỏ
    }

    for g_idx, (g_type, boxes) in enumerate(groups, start=1):
        color = color_map.get(g_type, (0, 0, 255))
        for b_idx, (bx, by, bw, bh) in enumerate(boxes, start=1):
            cv2.rectangle(debug_img, (bx, by), (bx + bw, by + bh), color, 2)
            label = f"G{g_idx:02d}_{g_type[0].upper()}{b_idx}"
            cv2.putText(debug_img, label, (bx + 6, by + 22), cv2.FONT_HERSHEY_SIMPLEX, 0.65, (0, 0, 0), 3)
            cv2.putText(debug_img, label, (bx + 6, by + 22), cv2.FONT_HERSHEY_SIMPLEX, 0.65, (255, 255, 255), 1)

    debug_dir = os.path.join(output_dir, "debug")
    os.makedirs(debug_dir, exist_ok=True)
    out_path = os.path.join(debug_dir, f"p{page_num:02d}_debug.png")
    safe_cv2_imwrite(out_path, debug_img)


# ==============================================================================
# HÀM API PYTHON DÙNG CHUNG CHO DỰ ÁN
# ==============================================================================
def extract_images_from_pdf(
    pdf_input: Any,
    output_dir: str,
    threshold: int = 0,
    margin: float = 0.005,
    debug: bool = False,
    questions_map: Optional[Dict[str, List[str]]] = None,
    prefix_id: str = "",
) -> List[Dict[str, Any]]:
    """
    Hàm API dùng chung để trích xuất ảnh từ file PDF đề thi nghe JLPT:
    - Nhận vào đường dẫn file PDF (str) hoặc dữ liệu nhị phân (bytes).
    - Xuất ảnh PNG vào output_dir và sinh manifest.json.
    - Trả về danh sách metadata các nhóm ảnh đã trích xuất.
    """
    os.makedirs(output_dir, exist_ok=True)
    if isinstance(pdf_input, (bytes, bytearray)):
        doc = fitz.open(stream=pdf_input, filetype="pdf")
    else:
        doc = fitz.open(pdf_input)

    manifest: List[Dict[str, Any]] = []
    try:
        total_pages = len(doc)
        for page_idx in range(total_pages):
            page_num = page_idx + 1
            page = doc[page_idx]

            img = get_page_image(page, doc)
            h, w = img.shape[:2]

            cand_boxes = find_candidate_boxes(img, threshold_val=threshold)
            filt_boxes = filter_boxes(cand_boxes, w, h)
            filt_boxes = complete_missing_2x2_boxes(filt_boxes, w, h)
            rows = sort_boxes_into_rows(filt_boxes, h)
            groups = group_layout(rows, w, h)

            page_q_map = None
            if questions_map:
                page_q_map = questions_map.get(str(page_num)) or questions_map.get(page_num)

            page_manifest = crop_and_save_boxes(
                img=img,
                groups=groups,
                page_num=page_num,
                output_dir=output_dir,
                margin_ratio=margin,
                page_question_map=page_q_map,
                prefix_id=prefix_id,
            )
            manifest.extend(page_manifest)

            if debug:
                save_debug_image(img, groups, page_num, output_dir)
    finally:
        doc.close()

    manifest_path = os.path.join(output_dir, "manifest.json")
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

    return manifest


def apply_manifest_to_questions(
    questions: List[Dict[str, Any]],
    manifest: List[Dict[str, Any]],
    base_image_url: Optional[str] = None,
    jlpt_level: Optional[str] = None,
) -> List[Dict[str, Any]]:
    """
    Tự động điền ảnh cắt từ manifest.json vào danh sách câu hỏi đề nghe JLPT theo trang (Page-based):
    - Khắc phục triệt để lỗi lệch im lặng khi LLM bỏ sót câu hỏi.
    - Dùng tỉ lệ y_ratio (thay cho pixel cố định 500px) để tự động thích ứng mọi DPI và độ phân giải.
    - Kiểm tra tính tương thích giữa visual_type của câu hỏi và type của nhóm ảnh (choices vs single).
    - Tự động gắn cờ needs_review = True và ghi log cảnh báo khi phát hiện bất thường.
    """
    if not manifest or not questions:
        return questions

    if not base_image_url:
        base_image_url = os.environ.get("APP_IMAGE_BASE_URL", "http://localhost:8000/static/extracted_images")

    flat_manifest = []
    for m in manifest:
        if isinstance(m, list):
            flat_manifest.extend([x for x in m if isinstance(x, dict)])
        elif isinstance(m, dict):
            flat_manifest.append(m)
    manifest = flat_manifest

    # Chuẩn hóa y_ratio cho manifest nếu chưa có
    for g in manifest:
        if "y_ratio" not in g and g.get("bboxes"):
            h = g.get("page_height") or 1400.0
            min_y = min(b[1] for b in g["bboxes"])
            g["y_ratio"] = round(min_y / h, 4)

    # Gom nhóm manifest theo số trang
    manifest_by_page: Dict[int, List[Dict[str, Any]]] = {}
    for g in manifest:
        p = g.get("page")
        if p is not None:
            manifest_by_page.setdefault(p, []).append(g)
    for p in manifest_by_page:
        manifest_by_page[p].sort(key=lambda x: x.get("y_ratio", 0.0))

    def _apply_group_to_question(q: Dict[str, Any], group: Dict[str, Any]):
        g_type = group.get("type")
        images = group.get("images", [])
        if g_type == "choices" and len(images) == 4:
            q["has_visual_options"] = True
            q["has_visual"] = True
            q["image_url"] = None
            if not q.get("options") or len(q["options"]) < 4:
                q["options"] = [
                    {"option_key": i, "option_text": f"{i}", "is_correct": False}
                    for i in range(1, 5)
                ]
            for opt_idx, opt in enumerate(q["options"][:4]):
                img_name = images[opt_idx]
                opt["option_image_url"] = f"{base_image_url}/{img_name}" if not img_name.startswith("http") else img_name
                if not opt.get("option_text") or re.match(r'^(?:Tranh|Đáp án|Lựa chọn|Option)\s*\d+$', opt["option_text"].strip(), re.IGNORECASE) or opt["option_text"].strip() in {"", "1", "2", "3", "4"}:
                    opt["option_text"] = f"{opt_idx + 1}"
        elif g_type == "single" and len(images) >= 1:
            m_code = q.get("mondai_code", "")
            is_listening = m_code in {
                "TASK_BASED", "KEY_POINT", "GENERAL_OUTLINE",
                "UTTERANCE_EXPRESSIONS", "QUICK_RESPONSE", "INTEGRATED_LISTENING"
            }
            if is_listening or q.get("visual_type") == "single":
                q["has_visual_options"] = False
                img_name = images[0]
                q["image_url"] = f"{base_image_url}/{img_name}" if not img_name.startswith("http") else img_name

    def _is_visual_candidate(q: Dict[str, Any]) -> bool:
        if q.get("visual_type") in ("choices", "single"):
            return True
        if q.get("has_visual_options"):
            return True
        if q.get("has_visual") and not q.get("has_visual_options"):
            return True
        if q.get("image_box") and isinstance(q.get("image_box"), (list, tuple)):
            return True
        opts = q.get("options") or []
        if opts and all(
            (o.get("option_text") or "").strip() in ("", "1", "2", "3", "4", "①", "②", "③", "④", "A", "B", "C", "D")
            or re.match(r'^(?:Tranh|Đáp án|Lựa chọn|Option|選択肢)\s*\d+$', (o.get("option_text") or "").strip(), re.IGNORECASE)
            for o in opts
        ):
            return True
        return False

    # Kiểm tra xem danh sách câu hỏi có thông tin trang (source_page hoặc pdf_page) không
    has_page_info = any((q.get("source_page") or q.get("pdf_page")) for q in questions)

    if has_page_info:
        # === CHIẾN LƯỢC 1: GHÉP THEO TRANG (PAGE-BASED MATCHING) ===
        questions_by_page: Dict[int, List[Dict[str, Any]]] = {}
        for q in questions:
            p = q.get("source_page") or q.get("pdf_page")
            if p is not None:
                questions_by_page.setdefault(p, []).append(q)

        # Duyệt qua từng trang có câu hỏi
        for p, p_qs in questions_by_page.items():
            p_groups = manifest_by_page.get(p, [])
            if not p_groups:
                # Trang không có tranh vẽ (ví dụ các câu chữ cuối Mondai 1 hoặc Mondai 2 thuần chữ)
                continue

            # Lọc các câu được kỳ vọng có tranh
            visual_qs = [q for q in p_qs if _is_visual_candidate(q)]
            target_qs = visual_qs if visual_qs else p_qs

            if len(target_qs) == len(p_groups):
                # Khớp 1-1 chính xác trên cùng trang
                for q, g in zip(target_qs, p_groups):
                    _apply_group_to_question(q, g)
                    exp_type = q.get("visual_type") or ("choices" if q.get("has_visual_options") else ("single" if q.get("has_visual") else None))
                    if exp_type and exp_type != g.get("type"):
                        q["needs_review"] = True
                        logger.warning(f"⚠️ [PAGE_MATCH] Lệch loại visual_type trang {p}: câu hỏi cần {exp_type} nhưng nhóm ảnh là {g.get('type')}")
            elif len(p_qs) > len(p_groups):
                # Số câu trên trang nhiều hơn số khung ảnh (ví dụ Trang 5: câu 3 chữ, câu 4 tranh)
                if len(visual_qs) == len(p_groups):
                    for q, g in zip(visual_qs, p_groups):
                        _apply_group_to_question(q, g)
                        exp_type = q.get("visual_type") or ("choices" if q.get("has_visual_options") else "single")
                        if exp_type != g.get("type"):
                            q["needs_review"] = True
                else:
                    # Dùng y_ratio để xác định nửa trên (y_ratio < 0.32) hay nửa dưới (y_ratio >= 0.32)
                    for g in p_groups:
                        y_r = g.get("y_ratio", 0.0)
                        matched_q = p_qs[-1] if (y_r >= 0.32 and len(p_qs) >= 2) else p_qs[0]
                        _apply_group_to_question(matched_q, g)
                        exp_type = matched_q.get("visual_type") or ("choices" if matched_q.get("has_visual_options") else ("single" if matched_q.get("has_visual") else None))
                        if exp_type and exp_type != g.get("type"):
                            matched_q["needs_review"] = True
            else:
                # len(p_groups) > len(p_qs): số nhóm ảnh trên trang nhiều hơn số câu hỏi AI bóc tách được
                # Kiểm tra nếu là câu ví dụ '例' (Rei) ở trang đầu của Mondai 3 (UTTERANCE_EXPRESSIONS)
                m3_on_page = any(q.get("mondai_code") == "UTTERANCE_EXPRESSIONS" for q in p_qs)
                if m3_on_page and len(p_groups) == len(p_qs) + 1:
                    logger.info(f"ℹ️ [PAGE_MATCH] Phát hiện khung ví dụ '例' ở trang {p} Mondai 3, tự động bỏ qua nhóm đầu.")
                    usable_groups = p_groups[1:]
                else:
                    usable_groups = p_groups[:len(p_qs)]
                    for q in p_qs:
                        q["needs_review"] = True
                    logger.warning(f"⚠️ [PAGE_MATCH] Bất thường: Trang {p} có {len(p_groups)} nhóm ảnh nhưng chỉ có {len(p_qs)} câu hỏi! Đã bật needs_review.")

                for q, g in zip(p_qs, usable_groups):
                    _apply_group_to_question(q, g)

    else:
        # === CHIẾN LƯỢC 2: FALLBACK KHI KHÔNG CÓ THÔNG TIN TRANG ===
        lvl = (jlpt_level or "").upper()
        m1_questions = [q for q in questions if q.get("mondai_code") == "TASK_BASED"]
        m3_questions = [q for q in questions if q.get("mondai_code") == "UTTERANCE_EXPRESSIONS"]

        valid_manifest = [g for g in manifest if isinstance(g, dict) and "page" in g]

        # Phân tách manifest thành b1 (Mondai 1) và target_b2 (Mondai 3)
        # Trong đề JLPT nghe (N5, N4, N3): giữa Mondai 1 và Mondai 3 luôn là Mondai 2 (thuần chữ, không tranh).
        # Do đó khoảng cách trang giữa Mondai 1 và Mondai 3 luôn là bước nhảy trang lớn nhất (jump >= 2).
        b1: List[Dict[str, Any]] = []
        target_b2: List[Dict[str, Any]] = []

        if m1_questions and m3_questions and valid_manifest:
            max_jump = 0
            split_idx = -1
            for i in range(1, len(valid_manifest)):
                jump = valid_manifest[i]["page"] - valid_manifest[i - 1]["page"]
                if jump > max_jump and jump >= 2:
                    max_jump = jump
                    split_idx = i

            if split_idx > 0:
                b1 = list(valid_manifest[:split_idx])
                target_b2 = list(valid_manifest[split_idx:])
            else:
                b1 = list(valid_manifest)
        elif m1_questions and valid_manifest:
            b1 = list(valid_manifest)
        elif m3_questions and valid_manifest:
            target_b2 = list(valid_manifest)

        # Mondai 1 (TASK_BASED): N1/N2 đề nghe Mondai 1 không có tranh lưới 2x2 nên không tự ý gán bừa
        if b1 and m1_questions and lvl not in ("N1", "N2"):
            # BƯỚC 1: LỌC BỎ CÂU VÍ DỤ MẪU '例' Ở ĐẦU MONDAI 1 (NẾU CÓ)
            # Dấu hiệu nhận biết câu ví dụ trên trang tiêu đề Mondai:
            # - Có nhiều nhóm ảnh hơn tổng số câu hỏi: len(b1) > len(m1_questions)
            # - HOẶC nhóm ảnh đầu tiên nằm dưới banner tiêu đề (y_ratio > 0.22), là nhóm duy nhất trên trang đó,
            #   và trang tiếp theo bắt đầu ngay từ đỉnh trang (y_ratio < 0.22).
            is_m1_example = False
            if len(b1) > len(m1_questions):
                is_m1_example = True
            elif len(b1) > 1:
                first_g = b1[0]
                second_g = b1[1]
                same_page_groups = [g for g in b1 if g.get("page") == first_g.get("page")]
                if (
                    len(same_page_groups) == 1
                    and first_g.get("y_ratio", 0) > 0.22
                    and second_g.get("page", 0) > first_g.get("page", 0)
                    and second_g.get("y_ratio", 0) < 0.22
                ):
                    is_m1_example = True

            if is_m1_example and b1:
                logger.info(f"ℹ️ [CROP_JLPT] Phát hiện và loại bỏ nhóm ảnh câu ví dụ mẫu '例' trang {b1[0].get('page')} của Mondai 1.")
                b1 = b1[1:]

            # BƯỚC 2: Định vị chính xác từng câu theo vị trí vật lý trên trang PDF (2 câu/trang: nửa trên và nửa dưới)
            has_bboxes = all("page" in g and g.get("bboxes") for g in b1)
            if b1 and has_bboxes:
                p_min = min(g["page"] for g in b1)
                for g in b1:
                    p = g.get("page", p_min)
                    y_r = g.get("y_ratio", 0.0)
                    is_top = (y_r < 0.35)
                    q_idx = (p - p_min) * 2 + (0 if is_top else 1)
                    if q_idx < len(m1_questions):
                        _apply_group_to_question(m1_questions[q_idx], g)
                        logger.info(f"🎨 [CROP_JLPT] Đã gán nhóm ảnh trang {p} ({'nửa trên' if is_top else 'nửa dưới'}) vào Mondai 1 Câu {q_idx + 1}")
            else:
                m1_visual_qs = [q for q in m1_questions if _is_visual_candidate(q)]
                for idx, g in enumerate(b1):
                    target_q = m1_visual_qs[idx] if idx < len(m1_visual_qs) else (m1_questions[idx] if idx < len(m1_questions) else None)
                    if target_q:
                        _apply_group_to_question(target_q, g)

        # Mondai 3 (UTTERANCE_EXPRESSIONS):
        if target_b2 and m3_questions:
            # Lọc bỏ câu ví dụ mẫu '例' ở đầu Mondai 3 (nếu có)
            is_m3_example = False
            if len(target_b2) > len(m3_questions):
                is_m3_example = True
            elif len(target_b2) > 1:
                first_g = target_b2[0]
                second_g = target_b2[1]
                same_page_groups = [g for g in target_b2 if g.get("page") == first_g.get("page")]
                if (
                    len(same_page_groups) == 1
                    and first_g.get("y_ratio", 0) > 0.22
                    and second_g.get("page", 0) > first_g.get("page", 0)
                    and second_g.get("y_ratio", 0) < 0.22
                ):
                    is_m3_example = True

            if is_m3_example and target_b2:
                logger.info(f"ℹ️ [CROP_JLPT] Phát hiện và loại bỏ nhóm ảnh câu ví dụ mẫu '例' trang {target_b2[0].get('page')} của Mondai 3.")
                target_b2 = target_b2[-len(m3_questions):] if len(target_b2) > len(m3_questions) else target_b2[1:]

            # Định vị chính xác từng câu Mondai 3 theo vị trí vật lý trên trang (2 câu/trang: trên và dưới)
            has_bboxes_m3 = all("page" in g and g.get("bboxes") for g in target_b2)
            if target_b2 and has_bboxes_m3:
                p_min_m3 = min(g["page"] for g in target_b2)
                for g in target_b2:
                    p = g.get("page", p_min_m3)
                    y_r = g.get("y_ratio", 0.0)
                    is_top = (y_r < 0.35)
                    q_idx = (p - p_min_m3) * 2 + (0 if is_top else 1)
                    if q_idx < len(m3_questions):
                        _apply_group_to_question(m3_questions[q_idx], g)
                        logger.info(f"🎨 [CROP_JLPT] Đã gán nhóm ảnh trang {p} ({'nửa trên' if is_top else 'nửa dưới'}) vào Mondai 3 Câu {q_idx + 1}")
            else:
                for idx, q in enumerate(m3_questions):
                    if idx < len(target_b2):
                        _apply_group_to_question(q, target_b2[idx])

    # 4. Làm sạch tuyệt đối: KEY_POINT và QUICK_RESPONSE không bao giờ có ảnh
    for q in questions:
        if q.get("mondai_code") in {"KEY_POINT", "QUICK_RESPONSE"}:
            q["image_url"] = None
            q["has_visual_options"] = False
            for o in q.get("options") or []:
                o["option_image_url"] = None

    return questions


# ==============================================================================
# CHƯƠNG TRÌNH CHÍNH (MAIN CLI)
# ==============================================================================
def main():
    parser = argparse.ArgumentParser(
        description="Trích xuất ảnh đáp án & tình huống từ đề thi nghe JLPT dạng PDF thành từng file PNG riêng lẻ."
    )
    parser.add_argument("input_pdf", type=str, help="Đường dẫn file PDF đề thi nghe JLPT.")
    parser.add_argument("output_dir", type=str, help="Thư mục xuất ảnh và manifest.json.")
    parser.add_argument("--questions", type=str, default=None, help="File JSON mapping nhóm câu hỏi (ví dụ: {\"4\": [\"m1_q1\", \"m1_q2\"]}).")
    parser.add_argument("--threshold", type=int, default=0, help="Ngưỡng nhị phân hóa (mặc định: 0 - dùng Otsu tự động. Đặt > 0 nếu muốn cố định).")
    parser.add_argument("--margin", type=float, default=0.005, help="Tỉ lệ margin thụt vào để bỏ viền đen (mặc định: 0.005 bề rộng trang).")
    parser.add_argument("--debug", action="store_true", help="Lưu thêm ảnh debug vẽ khung viền từng trang.")

    args = parser.parse_args()

    if not os.path.exists(args.input_pdf):
        logger.error(f"❌ Không tìm thấy file PDF: {args.input_pdf}")
        sys.exit(1)

    os.makedirs(args.output_dir, exist_ok=True)

    # Đọc questions mapping nếu có
    questions_map: Dict[str, List[str]] = {}
    if args.questions:
        if os.path.exists(args.questions):
            with open(args.questions, "r", encoding="utf-8") as f:
                questions_map = json.load(f)
        else:
            try:
                questions_map = json.loads(args.questions)
            except Exception as e:
                logger.warning(f"⚠️ Không thể đọc tham số --questions: {e}")

    try:
        doc = fitz.open(args.input_pdf)
    except Exception as e:
        logger.error(f"❌ Lỗi mở PDF: {e}")
        sys.exit(1)

    total_pages = len(doc)
    logger.info(f"📂 Đang xử lý: {args.input_pdf} ({total_pages} trang)")
    logger.info(f"📁 Thư mục xuất: {args.output_dir}\n")

    manifest: List[Dict[str, Any]] = []
    total_images_saved = 0

    for page_idx in range(total_pages):
        page_num = page_idx + 1
        page = doc[page_idx]

        # 1. Lấy ảnh trang
        img = get_page_image(page, doc)
        h, w = img.shape[:2]

        # 2 & 3. Phát hiện khung chữ nhật ứng viên
        cand_boxes = find_candidate_boxes(img, threshold_val=args.threshold)

        # 4 & 5. Lọc khung trùng viền và khung con lồng nhau + Tự động bù đắp 2x2
        filt_boxes = filter_boxes(cand_boxes, w, h)
        filt_boxes = complete_missing_2x2_boxes(filt_boxes, w, h)

        # 6. Sắp xếp theo hàng y và cột x
        rows = sort_boxes_into_rows(filt_boxes, h)

        # 7. Gom nhóm theo bố cục (choices / single / unknown)
        groups = group_layout(rows, w, h)

        c_choices = sum(1 for g in groups if g[0] == "choices")
        c_single = sum(1 for g in groups if g[0] == "single")
        c_unknown = sum(1 for g in groups if g[0] == "unknown")

        # In thông tin trang
        logger.info(
            f"Trang {page_num:02d}: tìm thấy {len(filt_boxes):2d} khung -> "
            f"{c_choices} nhóm choices ({c_choices * 4} ảnh), "
            f"{c_single} nhóm single, "
            f"{c_unknown} nhóm unknown"
        )

        # Cảnh báo nếu có nhóm unknown hoặc số khung không khớp
        if c_unknown > 0:
            logger.warning(f"  ⚠️ Cảnh báo: Trang {page_num:02d} có {c_unknown} nhóm chưa xác định rõ bố cục ('unknown')!")
        expected_box_count = c_choices * 4 + c_single
        if len(filt_boxes) != expected_box_count and c_unknown == 0:
            logger.warning(f"  ⚠️ Cảnh báo: Số khung trên trang {page_num:02d} ({len(filt_boxes)}) không khớp tổng số ảnh trong các nhóm ({expected_box_count})!")

        # 8. Cắt ảnh và xuất kết quả
        page_q_map = questions_map.get(str(page_num)) or questions_map.get(page_num)
        page_manifest = crop_and_save_boxes(
            img=img,
            groups=groups,
            page_num=page_num,
            output_dir=args.output_dir,
            margin_ratio=args.margin,
            page_question_map=page_q_map,
        )
        manifest.extend(page_manifest)
        total_images_saved += sum(len(g["images"]) for g in page_manifest)

        # Vẽ ảnh debug nếu bật --debug
        if args.debug:
            save_debug_image(img, groups, page_num, args.output_dir)

    # Lưu manifest.json
    manifest_path = os.path.join(args.output_dir, "manifest.json")
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)

    logger.info(f"\n✅ Hoàn tất! Đã trích xuất {total_images_saved} ảnh vào '{args.output_dir}'")
    logger.info(f"📄 Đã tạo file mô tả manifest: {manifest_path}")


if __name__ == "__main__":
    main()
