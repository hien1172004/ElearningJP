import base64
import logging
from typing import Optional
from fastapi import FastAPI, File, UploadFile, Query, HTTPException, Body
from fastapi.middleware.cors import CORSMiddleware

try:
    from .schemas import (
        PredictionResponse,
        HealthResponse,
        Base64ImageRequest,
        StrokeAssessmentRequest,
        StrokeAssessmentResponse,
        KanjiCharacterDetailResponse,
        KanjiListResponse,
    )
    from .kanji_service import KanjiRecognitionService
    from .kanjivg_service import KanjiVGService
    from .stroke_evaluator import StrokeEvaluator
except ImportError:
    try:
        from src.schemas import (
            PredictionResponse,
            HealthResponse,
            Base64ImageRequest,
            StrokeAssessmentRequest,
            StrokeAssessmentResponse,
            KanjiCharacterDetailResponse,
            KanjiListResponse,
        )
        from src.kanji_service import KanjiRecognitionService
        from src.kanjivg_service import KanjiVGService
        from src.stroke_evaluator import StrokeEvaluator
    except ImportError:
        from schemas import (
            PredictionResponse,
            HealthResponse,
            Base64ImageRequest,
            StrokeAssessmentRequest,
            StrokeAssessmentResponse,
            KanjiCharacterDetailResponse,
            KanjiListResponse,
        )
        from kanji_service import KanjiRecognitionService
        from kanjivg_service import KanjiVGService
        from stroke_evaluator import StrokeEvaluator

logger = logging.getLogger("kanji_ai")

app = FastAPI(
    title="ElearningJP AI - Kanji Handwriting Recognition & Stroke Assessment API",
    description="Dịch vụ AI nhận dạng chữ viết tay tiếng Nhật (Kanji/Kana) và chấm điểm thứ tự nét vẽ KanjiVG",
    version="1.1.0"
)

# 1. CẤU HÌNH CORS CHO PHÉP FRONTEND REACT GỌI API
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:3000",
        "*"
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
async def startup_event():
    """Khởi động và nạp trước mô hình AI & cơ sở dữ liệu KanjiVG vào bộ nhớ RAM."""
    logger.info("Đang khởi động dịch vụ AI...")
    try:
        KanjiRecognitionService.get_instance()
        KanjiVGService.get_instance()
        logger.info("✅ Dịch vụ AI & KanjiVG sẵn sàng phục vụ!")
    except Exception as e:
        logger.error(f"❌ Không thể nạp mô hình hoặc dữ liệu KanjiVG: {e}", exc_info=True)


# ==============================================================================
# HEALTH CHECK
# ==============================================================================

@app.get("/health", response_model=HealthResponse, tags=["Health"])
@app.get("/api/v1/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """Kiểm tra tình trạng hoạt động của dịch vụ AI và số lượng lớp nhận diện."""
    service = KanjiRecognitionService.get_instance()
    return HealthResponse(
        status="ok",
        service="ElearningJP AI Kanji Recognition",
        classes_count=len(service.labels)
    )


# ==============================================================================
# KANJI RECOGNITION (CNN)
# ==============================================================================

@app.post("/api/v1/predict", response_model=PredictionResponse, tags=["Recognition"])
@app.post("/api/v1/imgreg", response_model=PredictionResponse, tags=["Recognition"])
async def predict_image_file(
    file: UploadFile = File(..., description="File ảnh nét vẽ PNG hoặc JPEG từ Canvas"),
    top_k: int = Query(10, ge=1, le=50, description="Số lượng chữ gợi ý hàng đầu cần trả về")
):
    """
    Nhận diện chữ Kanji từ file ảnh gửi lên dạng FormData (multipart/form-data)
    """
    if file.content_type not in ["image/png", "image/jpeg", "image/webp", "application/octet-stream"]:
        raise HTTPException(status_code=400, detail="Định dạng file không hợp lệ. Vui lòng gửi ảnh PNG hoặc JPEG.")

    contents = await file.read()
    if not contents:
        raise HTTPException(status_code=400, detail="File ảnh rỗng.")

    service = KanjiRecognitionService.get_instance()
    return service.predict(contents, top_k=top_k)


@app.post("/api/v1/predict-base64", response_model=PredictionResponse, tags=["Recognition"])
async def predict_base64_image(
    request: Base64ImageRequest = Body(..., description="Dữ liệu base64 trực tiếp từ canvas.toDataURL()")
):
    """
    Nhận diện chữ Kanji trực tiếp từ chuỗi Base64 DataURL (canvas.toDataURL)
    """
    try:
        data_str = request.image
        if "," in data_str:
            # Tách bỏ header "data:image/png;base64,"
            data_str = data_str.split(",", 1)[1]

        image_bytes = base64.b64decode(data_str)
        service = KanjiRecognitionService.get_instance()
        return service.predict(image_bytes, top_k=request.top_k or 10)
    except Exception as e:
        logger.error(f"Lỗi khi giải mã base64: {e}")
        raise HTTPException(status_code=400, detail=f"Không thể xử lý chuỗi base64: {str(e)}")


# ==============================================================================
# STROKE ORDER & DIRECTION ASSESSMENT (KANJIVG)
# ==============================================================================

@app.post("/api/v1/evaluate-strokes", response_model=StrokeAssessmentResponse, tags=["Stroke Assessment"])
@app.post("/evaluate-strokes", response_model=StrokeAssessmentResponse, tags=["Stroke Assessment"])
async def evaluate_strokes_endpoint(
    request: StrokeAssessmentRequest = Body(..., description="Dữ liệu các nét vẽ của người dùng và nét chuẩn KanjiVG")
):
    """
    Đánh giá thứ tự nét và chiều nét viết Kanji/Kana:
    - So sánh với dữ liệu chuẩn KanjiVG (tự động tra cứu nếu không truyền nét chuẩn).
    - Phát hiện nét vẽ ngược chiều (ví dụ: từ dưới lên thay vì từ trên xuống).
    - Phát hiện nét vẽ sai thứ tự.
    - Đưa ra phản hồi sư phạm chi tiết cho từng nét và chấm điểm tổng thể thang điểm 100.
    """
    try:
        return StrokeEvaluator.evaluate(request)
    except Exception as e:
        logger.error(f"Lỗi khi đánh giá nét vẽ: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Lỗi trong quá trình đánh giá nét vẽ: {str(e)}")


# ==============================================================================
# KANJIVG DATA API (2,353 Ký tự Kanji & Kana chuẩn)
# ==============================================================================

@app.get("/api/v1/kanji-strokes/{character}", response_model=KanjiCharacterDetailResponse, tags=["Kanji Data"])
async def get_kanji_strokes_endpoint(character: str):
    """
    Truy xuất thông tin nét vẽ chuẩn (KanjiVG) của một ký tự bất kỳ trong 2,353 chữ.
    """
    ch_data = KanjiVGService.get_instance().get_character(character)
    if not ch_data:
        raise HTTPException(status_code=404, detail=f"Chưa có dữ liệu nét chuẩn cho ký tự '{character}'.")
    return ch_data


@app.get("/api/v1/kanji-list", response_model=KanjiListResponse, tags=["Kanji Data"])
async def get_kanji_list_endpoint(
    level: Optional[str] = Query(None, description="Lọc theo cấp độ: N5, N4, N3, N2, N1, KANA"),
    limit: int = Query(250, ge=1, le=2500, description="Giới hạn số lượng ký tự trả về")
):
    """
    Lấy danh sách các ký tự mẫu theo cấp độ JLPT hoặc Kana.
    """
    items = KanjiVGService.get_instance().list_characters(level=level, limit=limit)
    return KanjiListResponse(total=len(items), items=items)
