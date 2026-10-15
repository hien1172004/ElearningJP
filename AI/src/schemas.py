"""
Tập trung toàn bộ Schemas (Pydantic Models) cho API của AI Service.
Đảm bảo xác thực dữ liệu chặt chẽ và sinh tài liệu Swagger UI OpenAPI tự động.
"""

from typing import List, Optional
from pydantic import BaseModel, Field


# ==============================================================================
# 1. SCHEMAS CHO SYSTEM & HEALTH CHECK
# ==============================================================================

class HealthResponse(BaseModel):
    status: str = Field("ok", description="Trạng thái dịch vụ")
    service: str = Field("ElearningJP AI Kanji Recognition", description="Tên dịch vụ AI")
    classes_count: int = Field(..., description="Tổng số nhãn ký tự mô hình hỗ trợ")


# ==============================================================================
# 2. SCHEMAS CHO HANDWRITING RECOGNITION (CNN)
# ==============================================================================

class PredictionItem(BaseModel):
    char: str = Field(..., description="Ký tự Kanji/Kana được dự đoán (ví dụ: 木, 末)")
    unicode_hex: Optional[str] = Field(None, description="Mã Unicode dạng Hex (ví dụ: 0x4e00)")
    confidence: float = Field(..., description="Độ tin cậy xác suất từ 0.0 đến 1.0")
    percentage: float = Field(..., description="Tỷ lệ phần trăm chính xác (0 - 100%)")


class PredictionResponse(BaseModel):
    success: bool = Field(True, description="Trạng thái xử lý thành công hay không")
    top_char: Optional[str] = Field(None, description="Ký tự có độ tin cậy cao nhất")
    predictions: List[PredictionItem] = Field(default_factory=list, description="Danh sách các dự đoán Top-K")
    message: Optional[str] = Field(None, description="Thông điệp bổ sung")


class Base64ImageRequest(BaseModel):
    image: str = Field(..., description="Chuỗi base64 hoặc dataURL: 'data:image/png;base64,...'")
    top_k: Optional[int] = Field(10, ge=1, le=50, description="Số lượng kết quả gợi ý cần trả về")


# ==============================================================================
# 3. SCHEMAS CHO STROKE ORDER & DIRECTION ASSESSMENT (CHẤM ĐIỂM NÉT VẼ)
# ==============================================================================

class StrokePoint(BaseModel):
    x: float = Field(..., description="Tọa độ X của điểm vẽ")
    y: float = Field(..., description="Tọa độ Y của điểm vẽ")
    t: Optional[float] = Field(None, description="Thời gian (timestamp theo ms hoặc giây, nếu có)")


class ReferenceStroke(BaseModel):
    stroke_number: int = Field(..., description="Số thứ tự chuẩn của nét trong KanjiVG (1-based)")
    svg_path_data: Optional[str] = Field(None, description="Chuỗi SVG path data (KanjiVG d attribute)")
    sample_points: Optional[List[List[float]]] = Field(None, description="Danh sách điểm mẫu chuẩn [[x, y], ...]")


class StrokeAssessmentRequest(BaseModel):
    character: str = Field(..., description="Ký tự mẫu người dùng đang luyện viết (ví dụ: '木', 'あ')")
    user_strokes: List[List[StrokePoint]] = Field(
        ...,
        description="Danh sách các nét người dùng đã vẽ theo đúng thứ tự thời gian. Mỗi nét là một danh sách các điểm (x, y).",
    )
    canvas_width: Optional[float] = Field(None, description="Chiều rộng khung vẽ của người dùng")
    canvas_height: Optional[float] = Field(None, description="Chiều cao khung vẽ của người dùng")
    reference_strokes: Optional[List[ReferenceStroke]] = Field(
        None,
        description="Dữ liệu nét chuẩn từ KanjiVG (nếu BE truyền vào). Nếu bỏ trống, AI service sẽ tự tìm trong kho KanjiVG nội bộ.",
    )


class StrokeEvaluationDetail(BaseModel):
    stroke_number: int = Field(..., description="Thứ tự nét chuẩn (1-based)")
    user_stroke_index: int = Field(..., description="Thứ tự nét tương ứng mà người dùng đã vẽ")
    order_correct: bool = Field(..., description="Người dùng có vẽ đúng thứ tự nét này không")
    direction_score: float = Field(..., description="Điểm đánh giá chiều nét vẽ (0.0 - 1.0)")
    is_direction_correct: bool = Field(..., description="Chiều nét vẽ có đúng hướng chuẩn không")
    is_reversed: bool = Field(False, description="Người dùng có bị vẽ ngược chiều 180 độ không")
    shape_score: Optional[float] = Field(None, description="Điểm tương đồng hình dáng nét (0.0 - 1.0)")
    feedback: str = Field(..., description="Lời nhận xét sư phạm chi tiết cho nét này")


class StrokeAssessmentResponse(BaseModel):
    success: bool = Field(True, description="Trạng thái đánh giá thành công")
    character: str = Field(..., description="Ký tự được đánh giá")
    total_strokes_expected: int = Field(..., description="Tổng số nét chuẩn của ký tự")
    total_strokes_received: int = Field(..., description="Số nét người dùng đã vẽ")
    is_stroke_count_correct: bool = Field(..., description="Số nét vẽ có đủ và đúng không")
    is_all_correct: bool = Field(..., description="Toàn bộ thứ tự và chiều nét có chính xác không")
    overall_score: float = Field(..., description="Điểm tổng thể thang điểm 100")
    feedback: str = Field(..., description="Đánh giá tổng quan về bài viết")
    stroke_details: List[StrokeEvaluationDetail] = Field(default_factory=list, description="Đánh giá chi tiết từng nét")


# ==============================================================================
# 4. SCHEMAS CHO KANJIVG DATASET QUERY
# ==============================================================================

class KanjiStrokeItem(BaseModel):
    stroke_number: int = Field(..., description="Thứ tự nét chuẩn (1-based)")
    svg_path_data: str = Field(..., description="Dữ liệu đường dẫn SVG Path của nét vẽ")


class KanjiCharacterDetailResponse(BaseModel):
    character: str = Field(..., description="Ký tự Kanji/Kana")
    char_type: Optional[str] = Field(None, description="Loại ký tự: KANJI, HIRAGANA, KATAKANA")
    stroke_count: int = Field(..., description="Tổng số nét chuẩn")
    han_viet: Optional[str] = Field(None, description="Âm Hán Việt")
    meaning_vi: Optional[str] = Field(None, description="Nghĩa tiếng Việt")
    meaning_en: Optional[str] = Field(None, description="Nghĩa tiếng Anh")
    romaji: Optional[str] = Field(None, description="Phiên âm Romaji")
    jlpt_level: Optional[str] = Field(None, description="Cấp độ JLPT: N5, N4, N3, N2, N1")
    strokes: List[KanjiStrokeItem] = Field(default_factory=list, description="Danh sách các nét chuẩn KanjiVG")


class KanjiListItem(BaseModel):
    character: str = Field(..., description="Ký tự")
    char_type: Optional[str] = Field(None, description="Loại ký tự")
    stroke_count: int = Field(..., description="Số nét")
    han_viet: Optional[str] = Field(None, description="Âm Hán Việt")
    meaning_vi: Optional[str] = Field(None, description="Nghĩa tiếng Việt")
    jlpt_level: Optional[str] = Field(None, description="Cấp độ JLPT")


class KanjiListResponse(BaseModel):
    total: int = Field(..., description="Tổng số ký tự trả về")
    items: List[KanjiListItem] = Field(default_factory=list, description="Danh sách ký tự")
