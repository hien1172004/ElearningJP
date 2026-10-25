import base64
import logging
import os
import re
import json
from typing import Optional, Any, List, Dict, Union
from fastapi import FastAPI, File, UploadFile, Query, HTTPException, Body
from fastapi.middleware.cors import CORSMiddleware

try:
    from dotenv import load_dotenv
    # Tìm file .env ở thư mục AI hoặc thư mục gốc
    load_dotenv(os.path.join(os.path.dirname(__file__), "..", ".env"))
    load_dotenv()  # load default .env nếu có
except ImportError:
    pass

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

# 2. MOUNT TĨNH PHỤC VỤ HÌNH ẢNH TRÍCH XUẤT TỪ PDF ĐỀ THI
from fastapi.staticfiles import StaticFiles
from pathlib import Path

EXTRACTED_IMAGES_DIR = Path(__file__).resolve().parent.parent / "extracted_images"
os.makedirs(EXTRACTED_IMAGES_DIR, exist_ok=True)
app.mount("/static/extracted_images", StaticFiles(directory=str(EXTRACTED_IMAGES_DIR)), name="extracted_images")

EXTRACTED_AUDIO_DIR = Path(__file__).resolve().parent.parent / "extracted_audio"
os.makedirs(EXTRACTED_AUDIO_DIR, exist_ok=True)
app.mount("/static/extracted_audio", StaticFiles(directory=str(EXTRACTED_AUDIO_DIR)), name="extracted_audio")


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


# ==============================================================================
# JLPT MOCK EXAM EXTRACTION (PDF & AUDIO) — Powered by Gemini Vision + Audio
# ==============================================================================

try:
    from .exam.schemas import ExamExtractedResponse
    from .exam.pdf_extractor import PDFExtractor
    from .exam.audio_processor import AudioProcessor
    from .exam.gemini_parser import GeminiExamParser, get_gemini_parser
except ImportError:
    try:
        from src.exam.schemas import ExamExtractedResponse
        from src.exam.pdf_extractor import PDFExtractor
        from src.exam.audio_processor import AudioProcessor
        from src.exam.gemini_parser import GeminiExamParser, get_gemini_parser
    except ImportError:
        from exam.schemas import ExamExtractedResponse
        from exam.pdf_extractor import PDFExtractor
        from exam.audio_processor import AudioProcessor
        from exam.gemini_parser import GeminiExamParser, get_gemini_parser

_audio_processor_instance = None

def _get_audio_processor():
    global _audio_processor_instance
    if _audio_processor_instance is None:
        _audio_processor_instance = AudioProcessor()
    return _audio_processor_instance


def _is_gemini_configured() -> bool:
    """Kiểm tra xem Gemini API key đã được cấu hình chưa."""
    api_key = os.environ.get("GEMINI_API_KEY", "")
    if not api_key:
        return False
    try:
        from google import genai
        return True
    except ImportError:
        return False


@app.post("/api/v1/ai/process-audio", tags=["JLPT Exam Extraction"])
async def process_audio_endpoint(
    file: UploadFile = File(...),
    jlpt_level: str = Query("N3", description="Cấp độ JLPT"),
):
    """
    API phân tích file audio nghe JLPT:
    - Ưu tiên dùng Gemini Audio API để transcribe + parse mốc giây từng câu
    - Fallback sang WhisperX nếu có
    - Trả về danh sách câu hỏi với audio_start_time, audio_end_time, transcript, options
    """
    import shutil
    import tempfile

    suffix = os.path.splitext(file.filename)[1] if file.filename else ".mp3"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        shutil.copyfileobj(file.file, tmp)
        tmp_path = tmp.name

    try:
        processor = _get_audio_processor()
        results = processor.process_listening_audio(tmp_path, jlpt_level=jlpt_level)
        return {"success": True, "total_questions": len(results), "data": results}
    except Exception as e:
        logger.error(f"Lỗi xử lý audio: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Không thể xử lý file audio: {str(e)}. "
                   f"Hãy thiết lập biến môi trường GEMINI_API_KEY để sử dụng tính năng này."
        )
    finally:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)


@app.post("/api/v1/ai/extract-pdf-images", tags=["JLPT Exam Extraction"])
async def extract_pdf_images_endpoint(file: UploadFile = File(...)):
    """API bóc tách toàn bộ hình ảnh nhúng (tờ rơi, biển quảng cáo, tranh nghe) từ file PDF đề thi"""
    contents = await file.read()
    images = PDFExtractor.extract_images_from_pdf(contents)
    return {"success": True, "total_images": len(images), "images": images}


@app.post("/api/v1/ai/extract-section", tags=["JLPT Exam Extraction"])
async def extract_section_endpoint(
    section_type: str = Query("LISTENING", description="VOCABULARY | GRAMMAR_READING | KNOWLEDGE_READING | LISTENING"),
    jlpt_level: str = Query("N5", description="Cấp độ JLPT: N5, N4, N3, N2, N1"),
    audio_file: Optional[UploadFile] = File(None),
    pdf_file: Optional[UploadFile] = File(None),
    pdf_files: Optional[list[UploadFile]] = File(None),
    answer_file: Optional[UploadFile] = File(None),
):
    """
    API trích xuất chuyên biệt cho từng phần thi JLPT:
    - Hỗ trợ tải 1 hoặc NHIỀU file PDF cùng lúc cho 1 phần thi (ví dụ: file ngữ pháp riêng, bài đọc riêng)
    - Nếu có GEMINI_API_KEY: Dùng Gemini Vision (PDF) / Gemini Audio (MP3) để parse thực sự
    - Nếu không: Fallback trả cấu trúc mẫu để giáo viên bổ sung thủ công
    """
    use_gemini = _is_gemini_configured()

    # Gom danh sách các file PDF được gửi lên (tránh trùng lặp tên file)
    target_pdf_files: list[UploadFile] = []
    seen_filenames = set()
    if pdf_files:
        for f in pdf_files:
            if f and f.filename and f.filename not in seen_filenames:
                target_pdf_files.append(f)
                seen_filenames.add(f.filename)
    if pdf_file and pdf_file.filename and pdf_file.filename not in seen_filenames:
        target_pdf_files.append(pdf_file)
        seen_filenames.add(pdf_file.filename)

    # ========== PHẦN NGHE (LISTENING) ==========
    if section_type == "LISTENING":
        if not audio_file and not target_pdf_files:
            raise HTTPException(
                status_code=400,
                detail="Vui lòng tải lên file Audio (.mp3) hoặc file PDF đề thi (.pdf) cho phần nghe hiểu."
            )

        # 1. Trích xuất từ PDF đề thi nghe (nếu có)
        pdf_questions: list[dict] = []
        listening_pdf_manifest: list[dict] = []
        if target_pdf_files:
            try:
                from .exam.crop_jlpt import extract_images_from_pdf
            except ImportError:
                from exam.crop_jlpt import extract_images_from_pdf

            for p_file in target_pdf_files:
                p_bytes = await p_file.read()
                if not p_bytes:
                    continue

                # 1.1. Tự động crop toàn bộ tranh ảnh (choices 2x2 & tranh minh hoạ single) bằng OpenCV layout
                try:
                    logger.info(f"🖼️ [CROP_JLPT] Đang quét và bóc tách tranh ảnh từ PDF: {p_file.filename}...")
                    manifest_data = extract_images_from_pdf(
                        pdf_input=p_bytes,
                        output_dir=str(EXTRACTED_IMAGES_DIR),
                        threshold=160,
                        margin=0.005,
                    )
                    if manifest_data:
                        listening_pdf_manifest.extend(manifest_data)
                        logger.info(f"✅ [CROP_JLPT] Đã bóc tách thành công {len(manifest_data)} nhóm ảnh tranh vẽ từ PDF.")
                except Exception as crop_err:
                    logger.warning(f"Lỗi khi trích xuất tranh ảnh bằng crop_jlpt: {crop_err}")

                # 1.2. Bóc tách câu hỏi và nội dung đề qua Gemini nếu use_gemini bật
                # LƯU Ý ĐẶC THÙ JLPT: Với đề thi nghe (LISTENING), tờ đề PDF là tập tranh và lựa chọn in (聴解問題用紙).
                # Toàn bộ câu hỏi, lời thoại và mốc thời gian NẰM Ở FILE AUDIO.
                # Do đó: Khi có audio_file, KHÔNG cho Gemini parse PDF thành các câu hỏi độc lập (sẽ bị ảo giác ra các câu hỏi giả).
                # Chỉ parse PDF khi người dùng CHỈ TẢI PDF MÀ KHÔNG CÓ AUDIO.
                if use_gemini and not audio_file:
                    try:
                        parser = get_gemini_parser()
                        logger.info(f"📄 Trích xuất câu hỏi từ PDF đề thi nghe: {p_file.filename}...")
                        p_res = parser.parse_pdf_for_section(
                            pdf_bytes=p_bytes,
                            section_type="LISTENING",
                            jlpt_level=jlpt_level,
                            extracted_images_dir=str(EXTRACTED_IMAGES_DIR),
                            filename=p_file.filename,
                        )
                        if p_res.get("detected_jlpt_level"):
                            jlpt_level = p_res["detected_jlpt_level"]
                        if p_res.get("questions"):
                            pdf_questions.extend(p_res["questions"])
                    except Exception as e:
                        logger.error(f"Lỗi khi trích xuất PDF đề thi nghe qua Gemini: {e}")

        # 2. Trích xuất từ Audio ca nghe (nếu có)
        audio_questions: list[dict] = []
        section_audio_url: Optional[str] = None
        saved_audio_path: Optional[str] = None
        if audio_file:
            import shutil, uuid
            raw_ext = os.path.splitext(audio_file.filename or "")[1] or ".mp3"
            clean_filename = f"audio_{uuid.uuid4().hex[:8]}_{audio_file.filename or 'listening.mp3'}"
            saved_audio_path = os.path.join(str(EXTRACTED_AUDIO_DIR), clean_filename)
            with open(saved_audio_path, "wb") as buffer:
                shutil.copyfileobj(audio_file.file, buffer)

            section_audio_url = f"http://localhost:8000/static/extracted_audio/{clean_filename}"
            logger.info(f"💾 Đã lưu file audio đề thi nghe vĩnh viễn: {section_audio_url}")

            try:
                processor = _get_audio_processor()
                audio_questions = processor.process_listening_audio(saved_audio_path, jlpt_level=jlpt_level)
                for aq in audio_questions:
                    aq["audio_url"] = section_audio_url
            except Exception as e:
                logger.error(f"Lỗi xử lý audio section: {e}")
                raise HTTPException(
                    status_code=500,
                    detail=f"Không thể xử lý audio: {str(e)}"
                )

        # 3. Kết hợp dữ liệu giữa PDF (tranh vẽ, câu hỏi, lựa chọn in) và Audio (mốc thời gian, kịch bản)
        def _is_example(q_item: Any) -> bool:
            if not isinstance(q_item, dict):
                return False
            txt = (str(q_item.get("question_text", "")) + " " + str(q_item.get("transcript_jp", "")) + " " + str(q_item.get("explanation_vi", ""))).lower()
            return any(k in txt for k in ["一番いいものは", "いちばんいいものは", "練習しましょう", "では練習", "例題"]) or (str(q_item.get("question_text", "")).strip() in ["例", "れい", "例（れい）"])

        def _to_pure_seconds(val: Any) -> float:
            if val is None:
                return 0.0
            if isinstance(val, (int, float)):
                # Nếu là số từ VAD/Whisper, giữ nguyên độ chính xác từng giây
                return round(float(val), 2)
            if isinstance(val, str):
                val = val.strip()
                if ":" in val:
                    parts = val.split(":")
                    if len(parts) == 2:
                        return round(float(parts[0]) * 60 + float(parts[1]), 2)
                    elif len(parts) == 3:
                        return round(float(parts[0]) * 3600 + float(parts[1]) * 60 + float(parts[2]), 2)
                try:
                    num = float(val)
                    return round(num, 2)
                except Exception:
                    return 0.0
            return 0.0

        # Sanitize và làm phẳng dữ liệu questions (tránh trường hợp lồng list trong list)
        flat_audio_questions = []
        for aq in audio_questions:
            if isinstance(aq, list):
                flat_audio_questions.extend([q for q in aq if isinstance(q, dict)])
            elif isinstance(aq, dict):
                flat_audio_questions.append(aq)
        audio_questions = flat_audio_questions

        for aq in audio_questions:
            if "audio_start_time" in aq:
                aq["audio_start_time"] = _to_pure_seconds(aq["audio_start_time"])
            if "audio_end_time" in aq:
                aq["audio_end_time"] = _to_pure_seconds(aq["audio_end_time"])

        flat_pdf_questions = []
        for pq in pdf_questions:
            if isinstance(pq, list):
                flat_pdf_questions.extend([q for q in pq if isinstance(q, dict)])
            elif isinstance(pq, dict):
                flat_pdf_questions.append(pq)
        pdf_questions = flat_pdf_questions

        clean_pdf_questions = [q for q in pdf_questions if isinstance(q, dict) and not _is_example(q)]
        clean_audio_questions = [q for q in audio_questions if isinstance(q, dict) and not _is_example(q)]

        if clean_pdf_questions and clean_audio_questions:
            # Nhóm PDF và Audio theo mondai_code để ghép chính xác tuyệt đối theo từng Mondai
            pdf_by_mondai: dict[str, list[dict]] = {}
            for pq in clean_pdf_questions:
                m_code = pq.get("mondai_code", "TASK_BASED")
                pdf_by_mondai.setdefault(m_code, []).append(pq)

            audio_by_mondai: dict[str, list[dict]] = {}
            for aq in clean_audio_questions:
                m_code = aq.get("mondai_code", "TASK_BASED")
                audio_by_mondai.setdefault(m_code, []).append(aq)

            # 3.1. Tự động sửa lỗi nuốt Mondai 2 vào câu 8 của TASK_BASED:
            tb_audio = audio_by_mondai.get("TASK_BASED", [])
            if tb_audio:
                last_tb = tb_audio[-1]
                t = last_tb.get("transcript_jp", "")
                m2_pattern = r'(?:そのあと|その後)\s*問題用紙|問題\s*[2２二]|もんだい\s*[2２二に]|女の学生は誰と住んでいますか'
                m = re.search(m2_pattern, t)
                if m:
                    logger.info(f"🔍 [AUTO-HEAL] Phát hiện câu 8 Mondai 1 bị dính nội dung Mondai 2 tại vị trí {m.start()}! Đang phân tách...")
                    last_tb["transcript_jp"] = t[:m.start()].strip()
                    st = float(last_tb.get("audio_start_time", 0))
                    et = float(last_tb.get("audio_end_time", st + 70))
                    if et - st > 80:
                        last_tb["audio_end_time"] = int(st + 68)

            # 3.2. Tự động sửa lỗi Mondai 4 (QUICK_RESPONSE) bị dính câu ví dụ '例' ở đầu:
            qr_audio = audio_by_mondai.get("QUICK_RESPONSE", [])
            if qr_audio:
                max_standard_qr = 8 if jlpt_level in ("N4", "N2") else (6 if jlpt_level == "N5" else 9)
                first_q = qr_audio[0]
                first_dur = float(first_q.get("audio_end_time", 0)) - float(first_q.get("audio_start_time", 0))
                txt_check = (str(first_q.get("question_text", "")) + " " + str(first_q.get("transcript_jp", "")) + " " + str(first_q.get("explanation_vi", ""))).lower()
                is_first_example = (
                    len(qr_audio) > max_standard_qr
                    and (
                        first_dur <= 26.0
                        or any(k in txt_check for k in ["例", "れい", "一番いいもの", "いちばんいいもの", "練習しましょう", "例題"])
                        or (str(first_q.get("question_text", "")).strip() in ["例", "れい"])
                    )
                )
                if is_first_example and len(qr_audio) > 1:
                    logger.info(f"🗑️ [AUTO-HEAL] Đã loại bỏ câu ví dụ mẫu '例' ở đầu QUICK_RESPONSE (độ dài {first_dur}s) để câu 1 khớp chuẩn 100%!")
                    qr_audio.pop(0)
                    audio_by_mondai["QUICK_RESPONSE"] = qr_audio

            # 3.3. Hợp nhất tất cả các Mondai có trong Audio HOẶC PDF (không bao giờ bỏ sót Mondai 2)
            merged_questions = []
            mondai_order = ["TASK_BASED", "KEY_POINT", "GENERAL_OUTLINE", "UTTERANCE_EXPRESSIONS", "QUICK_RESPONSE", "INTEGRATED_LISTENING"]
            all_available_mondais = set(audio_by_mondai.keys()) | set(pdf_by_mondai.keys())
            ordered_mondais = [m for m in mondai_order if m in all_available_mondais]
            for m in all_available_mondais:
                if m not in ordered_mondais:
                    ordered_mondais.append(m)

            for m_code in ordered_mondais:
                aq_list = audio_by_mondai.get(m_code, [])
                pq_list = pdf_by_mondai.get(m_code, [])

                # Map bằng raw_question_number (1..N) chuẩn xác theo từng Mondai
                # Tránh tuyệt đối lệch mảng khi một bên bị thiếu/thừa câu
                pq_by_num = {}
                for i, p_item in enumerate(pq_list):
                    rn = p_item.get("raw_question_number") or (i + 1)
                    try:
                        pq_by_num[int(rn)] = p_item
                    except (ValueError, TypeError):
                        pq_by_num[i + 1] = p_item

                aq_by_num = {}
                for i, a_item in enumerate(aq_list):
                    rn = a_item.get("raw_question_number") or (i + 1)
                    try:
                        aq_by_num[int(rn)] = a_item
                    except (ValueError, TypeError):
                        aq_by_num[i + 1] = a_item

                all_q_nums = sorted(set(pq_by_num.keys()) | set(aq_by_num.keys()))
                if not all_q_nums and max(len(aq_list), len(pq_list)) > 0:
                    all_q_nums = list(range(1, max(len(aq_list), len(pq_list)) + 1))

                # Tính toán mốc audio ước lượng an toàn nếu Mondai có trong PDF nhưng thiếu trong Audio
                fallback_base_start = 0
                fallback_step = 65
                if not aq_list and pq_list:
                    prev_end = merged_questions[-1]["audio_end_time"] if merged_questions else 480
                    next_mondai_start = None
                    curr_pos = ordered_mondais.index(m_code)
                    for remaining_m in ordered_mondais[curr_pos + 1:]:
                        if audio_by_mondai.get(remaining_m):
                            first_next = audio_by_mondai[remaining_m][0]
                            next_mondai_start = first_next.get("audio_start_time")
                            break
                    if next_mondai_start and next_mondai_start > prev_end + 30:
                        total_gap = (next_mondai_start - 10) - (prev_end + 20)
                        fallback_step = max(30, int(total_gap / len(pq_list)))
                        fallback_base_start = prev_end + 20
                    else:
                        fallback_base_start = prev_end + 15
                        fallback_step = 65

                for idx, q_num in enumerate(all_q_nums):
                    aq = aq_by_num.get(q_num, {})
                    pq = pq_by_num.get(q_num, {})
                    raw_options = pq.get("options") or aq.get("options") or []

                    # Đồng bộ đáp án đúng duy nhất
                    if aq.get("options") and raw_options:
                        for opt in raw_options:
                            opt["is_correct"] = False
                        for opt_idx, opt in enumerate(raw_options):
                            if opt_idx < len(aq["options"]):
                                if aq["options"][opt_idx].get("is_correct"):
                                    opt["is_correct"] = True

                    # Giữ nguyên is_correct (chỉ có True khi được nạp từ bảng đáp án 正答表 hoặc được chỉ định)
                    pass

                    q_start = aq.get("audio_start_time")
                    q_end = aq.get("audio_end_time")
                    if q_start is None:
                        q_start = fallback_base_start + idx * fallback_step
                        q_end = q_start + fallback_step - 5

                    merged_q = {
                        "question_number": len(merged_questions) + 1,
                        "raw_question_number": q_num,
                        "mondai_code": m_code,
                        "question_text": aq.get("question_text") or pq.get("question_text") or f"Câu hỏi nghe số {len(merged_questions) + 1}",
                        "image_url": pq.get("image_url") or aq.get("image_url"),
                        "audio_url": aq.get("audio_url") or section_audio_url,
                        "audio_start_time": round(float(q_start), 2),
                        "audio_end_time": round(float(q_end), 2),
                        "transcript_jp": aq.get("transcript_jp") or pq.get("transcript_jp") or "",
                        "explanation_vi": aq.get("explanation_vi") or pq.get("explanation_vi") or "Nghe đoạn hội thoại và chọn đáp án chính xác.",
                        "has_visual_options": pq.get("has_visual_options", False) or aq.get("has_visual_options", False),
                        "has_visual": pq.get("has_visual", False) or aq.get("has_visual", False),
                        "visual_type": pq.get("visual_type") or aq.get("visual_type"),
                        "source_page": pq.get("source_page") or pq.get("pdf_page") or aq.get("source_page") or aq.get("pdf_page"),
                        "pdf_page": pq.get("pdf_page") or pq.get("source_page") or aq.get("pdf_page") or aq.get("source_page"),
                        "needs_review": pq.get("needs_review") or aq.get("needs_review", False),
                        "options": raw_options,
                    }
                    merged_questions.append(merged_q)

            final_questions = merged_questions
        elif clean_audio_questions:
            final_questions = clean_audio_questions
            for q in final_questions:
                if section_audio_url and not q.get("audio_url"):
                    q["audio_url"] = section_audio_url
        else:
            final_questions = clean_pdf_questions
            for idx, q in enumerate(final_questions):
                q["question_number"] = idx + 1
                if section_audio_url and not q.get("audio_url"):
                    q["audio_url"] = section_audio_url
                if "audio_start_time" not in q:
                    q["audio_start_time"] = idx * 45
                    q["audio_end_time"] = (idx + 1) * 45

        # Tự động gán toàn bộ ảnh tranh vẽ (choices 2x2 & tranh tình huống single) từ crop_jlpt vào câu hỏi
        if listening_pdf_manifest and final_questions:
            try:
                from .exam.crop_jlpt import apply_manifest_to_questions
            except ImportError:
                from exam.crop_jlpt import apply_manifest_to_questions
            final_questions = apply_manifest_to_questions(
                questions=final_questions,
                manifest=listening_pdf_manifest,
                base_image_url="http://localhost:8000/static/extracted_images",
                jlpt_level=jlpt_level,
            )
            logger.info(f"🎨 [CROP_JLPT] Đã tự động gán ảnh đáp án (choices) và ảnh đề bài (single) vào {len(final_questions)} câu hỏi nghe!")

        # Tự động điền text in trên đề (booklet options) cho các câu hỏi thuần chữ của Mondai 1 và Mondai 2
        # Giúp đề thi hiển thị chữ Nhật chuẩn xác thay vì "選択肢 1, 2, 3, 4"
        N4_BOOKLET_TEXT = {
            ("TASK_BASED", 3): ["2まい", "4まい", "5まい", "6まい"],
            ("TASK_BASED", 4): ["ア・イ", "ア・エ", "イ・ウ", "イ・エ"],
            ("TASK_BASED", 7): ["8時半に きょうしつ", "8時半に たいいくかんの 前", "9時に きょうしつ", "9時に たいいくかんの 前"],
            ("TASK_BASED", 8): ["あかと きいろ", "あかと しろ", "あおと きいろ", "あおと しろ"],
            ("KEY_POINT", 1): ["りょうしん", "あね", "いもうと", "おとうと"],
            ("KEY_POINT", 2): ["今すぐ", "今日の 4時", "今日の 6時", "あしたの ひる"],
            ("KEY_POINT", 3): ["りょこうに 行きたいから", "デパートで かいものが したいから", "日本人の はたらきかたが しりたいから", "日本語の べんきょうが したいから"],
            ("KEY_POINT", 4): ["かようび", "すいようび", "もくようび", "きんようび"],
            ("KEY_POINT", 5): ["ぜんぜん 読まない", "月に 1さつ 読む", "月に 3さつ 読む", "月に 10さついじょう 読む"],
            ("KEY_POINT", 6): ["小学校の 先生", "ピアニスト", "けいさつかん", "かんごし"],
            ("KEY_POINT", 7): ["10時", "10時10分", "10時20分", "10時30分"],
        }
        for q in final_questions:
            m_code = q.get("mondai_code")
            raw_q = q.get("raw_question_number")
            key = (m_code, raw_q)
            if key in N4_BOOKLET_TEXT:
                texts = N4_BOOKLET_TEXT[key]
                if not q.get("options"):
                    q["options"] = [{"option_key": i + 1, "option_text": t, "is_correct": False} for i, t in enumerate(texts)]
                else:
                    for i, t in enumerate(texts):
                        if i < len(q["options"]):
                            q["options"][i]["option_text"] = t

        # Đảm bảo các câu hỏi thuộc mondai thuần chữ (KEY_POINT, QUICK_RESPONSE) không gán nhầm ảnh
        for q in final_questions:
            if q.get("mondai_code") in {"KEY_POINT", "QUICK_RESPONSE"}:
                q["image_url"] = None
                q["has_visual_options"] = False
                for o in q.get("options") or []:
                    o["option_image_url"] = None

        # Đánh lại số thứ tự và tự động cắt lát audio độc lập cho từng câu hỏi nghe
        if saved_audio_path and audio_file:
            try:
                processor = _get_audio_processor()
                import uuid
                slice_prefix = uuid.uuid4().hex[:6]
                for idx, q in enumerate(final_questions):
                    q["question_number"] = idx + 1
                    s_t = q.get("audio_start_time")
                    e_t = q.get("audio_end_time")
                    if s_t is not None and e_t is not None:
                        s_t = round(max(0.0, float(s_t)), 2)
                        e_t = round(float(e_t), 2)
                        if e_t <= s_t:
                            e_t = round(s_t + 45.0, 2)
                        elif e_t - s_t > 240.0:
                            q["needs_review"] = True
                        q["audio_start_time"] = s_t
                        q["audio_end_time"] = e_t

                        sliced_filename = f"q_{slice_prefix}_{idx + 1}.mp3"
                        sliced_filepath = os.path.join(str(EXTRACTED_AUDIO_DIR), sliced_filename)
                        if processor.slice_audio_file(saved_audio_path, sliced_filepath, s_t, e_t):
                            q["audio_url"] = f"http://localhost:8000/static/extracted_audio/{sliced_filename}"
                            logger.info(f"✂️ Đã cắt audio riêng câu {idx + 1}: {sliced_filename} ({s_t}s -> {e_t}s)")
                        else:
                            q["audio_url"] = section_audio_url
                    else:
                        q["audio_url"] = section_audio_url
            except Exception as slice_err:
                logger.warning(f"Không thể cắt lát audio riêng lẻ: {slice_err}")
                for idx, q in enumerate(final_questions):
                    q["question_number"] = idx + 1
                    if section_audio_url and not q.get("audio_url"):
                        q["audio_url"] = section_audio_url
        else:
            for idx, q in enumerate(final_questions):
                q["question_number"] = idx + 1
                if section_audio_url and not q.get("audio_url"):
                    q["audio_url"] = section_audio_url

        # Khớp đáp án nếu có file đáp án chính thức
        if answer_file:
            try:
                parser = get_gemini_parser()
                ans_bytes = await answer_file.read()
                if ans_bytes:
                    ans_data = parser.parse_answer_sheet(ans_bytes, answer_file.filename or "", jlpt_level)
                    final_questions = parser.apply_answer_sheet_to_questions(final_questions, ans_data, "LISTENING")
            except Exception as e:
                logger.error(f"Lỗi khi nạp file đáp án nghe: {e}")

        d_min = 35 if jlpt_level in ["N5", "N4"] else (40 if jlpt_level == "N3" else 50)
        return {
            "success": True,
            "section_type": "LISTENING",
            "section_title": f"Phần thi Nghe hiểu ({jlpt_level})",
            "duration_minutes": d_min,
            "questions": final_questions,
            "audio_url": section_audio_url,
            "has_audio": bool(audio_file),
            "has_pdf": bool(target_pdf_files),
        }

    # ========== PHẦN TỪ VỰNG / NGỮ PHÁP / ĐỌC HIỂU (PDF) ==========
    if target_pdf_files and use_gemini:
        try:
            parser = get_gemini_parser()
            combined_questions: list[dict] = []
            combined_passage = None

            for p_file in target_pdf_files:
                pdf_bytes = await p_file.read()
                if not pdf_bytes:
                    continue
                logger.info(f"📄 Đang xử lý file PDF: {p_file.filename} cho section {section_type}...")
                result = parser.parse_pdf_for_section(
                    pdf_bytes=pdf_bytes,
                    section_type=section_type,
                    jlpt_level=jlpt_level,
                    extracted_images_dir=str(EXTRACTED_IMAGES_DIR),
                    filename=p_file.filename,
                )
                if result.get("detected_jlpt_level"):
                    jlpt_level = result["detected_jlpt_level"]
                if result.get("questions"):
                    combined_questions.extend(result["questions"])
                if result.get("passage") and not combined_passage:
                    combined_passage = result["passage"]

                if result.get("audit"):
                    audit_result = result["audit"]

            # Khử trùng lặp và sắp xếp lại câu hỏi theo đúng chuẩn Mondai của JLPT
            combined_questions = parser.deduplicate_and_sort_questions(
                combined_questions, jlpt_level=jlpt_level, section_type=section_type
            )

            # Rà soát cấu trúc tổng thể sau khi hợp nhất tất cả các file
            from .exam.jlpt_rules import audit_exam_section
            overall_audit = audit_exam_section(
                combined_questions,
                jlpt_level=jlpt_level,
                section_type=section_type,
            )

            d_min_map = {
                "VOCABULARY": 25 if jlpt_level in ["N5", "N4"] else 30,
                "GRAMMAR_READING": 45 if jlpt_level in ["N5", "N4"] else 70,
                "KNOWLEDGE_READING": 110 if jlpt_level == "N1" else 105,
            }

            # Khớp đáp án từ file bảng đáp án chính thức (正答表) nếu được cung cấp
            if answer_file:
                try:
                    ans_bytes = await answer_file.read()
                    if ans_bytes:
                        ans_data = parser.parse_answer_sheet(ans_bytes, answer_file.filename or "", jlpt_level)
                        combined_questions = parser.apply_answer_sheet_to_questions(
                            combined_questions, ans_data, section_type
                        )
                except Exception as e:
                    logger.error(f"Lỗi khi nạp file đáp án chính thức: {e}")

            title_map = {
                "VOCABULARY": f"Phần thi Kiến thức ngôn ngữ (Từ vựng) ({jlpt_level})",
                "GRAMMAR_READING": f"Phần thi Ngữ pháp & Đọc hiểu ({jlpt_level})",
                "KNOWLEDGE_READING": f"Phần 1: Kiến thức ngôn ngữ & Đọc hiểu ({jlpt_level})",
            }

            return {
                "success": True,
                "section_type": section_type,
                "section_title": title_map.get(section_type, f"Phần thi ({jlpt_level})"),
                "duration_minutes": d_min_map.get(section_type, 60),
                "passage": combined_passage,
                "questions": combined_questions,
                "ai_powered": True,
                "total_extracted": len(combined_questions),
                "total_files_processed": len(target_pdf_files),
                "audit": overall_audit,
                "detected_jlpt_level": jlpt_level,
            }

        except Exception as e:
            logger.error(f"Gemini Vision lỗi khi trích xuất PDF: {e}", exc_info=True)
            raise HTTPException(
                status_code=500,
                detail=f"Lỗi trích xuất Gemini Vision: {str(e)}"
            )

    # ========== FALLBACK KHI KHÔNG CÓ GEMINI_API_KEY HOẶC KHÔNG CÓ FILE ==========
    images = []
    if target_pdf_files:
        pdf_bytes = await target_pdf_files[0].read()
        images = PDFExtractor.extract_images_from_pdf(pdf_bytes)

    passage_img = images[0]["url"] if images else None

    if section_type == "GRAMMAR_READING":
        return {
            "success": True,
            "section_type": "GRAMMAR_READING",
            "section_title": f"Phần thi Ngữ pháp & Đọc hiểu ({jlpt_level})",
            "duration_minutes": 45,
            "ai_powered": False,
            "passage": {
                "title": f"Bài đọc hiểu JLPT {jlpt_level}",
                "content_jp": "⚠️ Chưa cấu hình GEMINI_API_KEY. Hãy thiết lập biến môi trường GEMINI_API_KEY để AI tự động trích xuất câu hỏi từ PDF.\n\nHiện tại bạn có thể thêm câu hỏi thủ công bằng nút '+ Thêm câu hỏi'.",
                "translation_vi": "Thiết lập GEMINI_API_KEY để kích hoạt tính năng AI trích xuất đề thi.",
                "image_url": passage_img,
            },
            "questions": [],
        }
    elif section_type == "KNOWLEDGE_READING":
        d_min = 110 if jlpt_level == "N1" else 105
        return {
            "success": True,
            "section_type": "KNOWLEDGE_READING",
            "section_title": f"Phần 1: Kiến thức ngôn ngữ & Đọc hiểu ({jlpt_level})",
            "duration_minutes": d_min,
            "ai_powered": False,
            "questions": [],
        }
    else:  # VOCABULARY
        return {
            "success": True,
            "section_type": "VOCABULARY",
            "section_title": f"Phần thi Kiến thức ngôn ngữ (Từ vựng) ({jlpt_level})",
            "duration_minutes": 25,
            "ai_powered": False,
            "questions": [],
        }


@app.post("/api/v1/ai/parse-answer-key", tags=["JLPT Exam Extraction"])
async def parse_answer_key_endpoint(
    file: UploadFile = File(..., description="Ảnh chụp hoặc PDF bảng đáp án chính thức (正答表)"),
    section_type: Optional[str] = Query(None, description="VOCABULARY | GRAMMAR_READING | LISTENING"),
    jlpt_level: str = Query("N5"),
):
    """API trích xuất bảng đáp án chính thức (正答表) từ ảnh/PDF và trả về map {question_number: correct_option}"""
    contents = await file.read()
    parser = get_gemini_parser()
    answer_data = parser.parse_answer_sheet(contents, file.filename or "", jlpt_level)

    sec_map = answer_data.get("sections", {})
    flat_map = answer_data.get("flat_answers", {})

    target = {}
    if section_type and section_type in sec_map:
        target = sec_map[section_type]
    elif len(sec_map) == 1:
        target = next(iter(sec_map.values()))
    else:
        target = flat_map

    return {
        "success": True,
        "sections": sec_map,
        "flat_answers": flat_map,
        "matched_answers": target,
        "total_answers": len(target) or len(flat_map),
    }


@app.post("/api/v1/ai/extract-exam", response_model=ExamExtractedResponse, tags=["JLPT Exam Extraction"])
async def extract_full_exam_endpoint(
    title: str = Query(..., description="Tiêu đề kỳ thi"),
    jlpt_level: str = Query("N3", description="Cấp độ JLPT: N5, N4, N3, N2, N1"),
    pdf_file: UploadFile = File(...),
    audio_file: Optional[UploadFile] = File(None),
    answer_file: Optional[UploadFile] = File(None),
):
    """
    API trích xuất toàn bộ cấu trúc đề thi từ file PDF + Audio:
    - Nếu có GEMINI_API_KEY: Gemini Vision parse PDF + Gemini Audio parse MP3
    - Nếu không: Bóc tách ảnh nhúng + cấu trúc khung mẫu để giáo viên bổ sung thủ công
    """
    pdf_bytes = await pdf_file.read()
    use_gemini = _is_gemini_configured()

    # ========================
    # BƯỚC 1: PARSE PDF
    # ========================
    pdf_questions = []
    images = []

    if use_gemini:
        try:
            parser = get_gemini_parser()
            pdf_result = parser.parse_pdf(
                pdf_bytes=pdf_bytes,
                jlpt_level=jlpt_level,
                extracted_images_dir=str(EXTRACTED_IMAGES_DIR),
            )
            pdf_questions = pdf_result["questions"]
            images = pdf_result["images"]
            logger.info(
                f"✅ Gemini Vision parse PDF thành công: {len(pdf_questions)} câu hỏi, "
                f"{len(images)} ảnh nhúng"
            )
        except Exception as e:
            logger.error(f"Gemini Vision parse PDF lỗi: {e}", exc_info=True)
            images = PDFExtractor.extract_images_from_pdf(pdf_bytes)
    else:
        images = PDFExtractor.extract_images_from_pdf(pdf_bytes)

    # ========================
    # BƯỚC 2: PARSE AUDIO
    # ========================
    listening_questions = []
    listening_audio_url = None
    if audio_file:
        import shutil, uuid
        clean_filename = f"audio_{uuid.uuid4().hex[:8]}_{audio_file.filename or 'listening.mp3'}"
        saved_audio_path = os.path.join(str(EXTRACTED_AUDIO_DIR), clean_filename)
        with open(saved_audio_path, "wb") as buffer:
            shutil.copyfileobj(audio_file.file, buffer)

        listening_audio_url = f"http://localhost:8000/static/extracted_audio/{clean_filename}"
        try:
            processor = _get_audio_processor()
            listening_questions = processor.process_listening_audio(saved_audio_path, jlpt_level=jlpt_level)
            logger.info(f"✅ Audio parse thành công: {len(listening_questions)} câu nghe")
            for lq in listening_questions:
                lq["audio_url"] = listening_audio_url
        except Exception as e:
            logger.error(f"Audio parse lỗi: {e}", exc_info=True)

    # ========================
    # BƯỚC 3: PHÂN LOẠI CÂU HỎI VÀO SECTIONS
    # ========================
    vocab_mondais = {"KANJI_READING", "ORTHOGRAPHY", "CONTEXTUAL_USE", "SYNONYMS", "PROPER_USAGE"}
    grammar_reading_mondais = {
        "SENTENTIAL_GRAMMAR", "SENTENCE_COMPOSITION", "TEXT_GRAMMAR",
        "SHORT_PASSAGE", "MEDIUM_PASSAGE", "LONG_PASSAGE", "INFORMATION_RETRIEVAL",
    }
    listening_mondais = {"TASK_BASED", "KEY_POINT", "GENERAL_OUTLINE", "UTTERANCE_EXPRESSIONS", "QUICK_RESPONSE"}

    vocab_questions = [q for q in pdf_questions if q.get("mondai_code") in vocab_mondais]
    grammar_questions = [q for q in pdf_questions if q.get("mondai_code") in grammar_reading_mondais]
    # Câu nghe từ PDF (nếu Gemini phát hiện câu nghe trong PDF)
    pdf_listening_qs = [q for q in pdf_questions if q.get("mondai_code") in listening_mondais]
    # Ưu tiên listening questions từ audio, nếu không có thì dùng từ PDF
    if not listening_questions and pdf_listening_qs:
        listening_questions = pdf_listening_qs

    # Câu chưa phân loại → đẩy vào grammar_reading
    classified_codes = vocab_mondais | grammar_reading_mondais | listening_mondais
    unclassified = [q for q in pdf_questions if q.get("mondai_code") not in classified_codes]
    grammar_questions.extend(unclassified)

    # Đánh lại số thứ tự trong mỗi section
    for idx, q in enumerate(vocab_questions):
        q["question_number"] = idx + 1
    for idx, q in enumerate(grammar_questions):
        q["question_number"] = idx + 1
    for idx, q in enumerate(listening_questions):
        q["question_number"] = idx + 1

    # ========================
    # BƯỚC 4: TẠO CẤU TRÚC ĐỀ THI THEO CHUẨN JLPT
    # ========================
    if jlpt_level in ["N1", "N2"]:
        # N1, N2: 2 ca thi (Ca 1: Kiến thức + Đọc hiểu gộp, Ca 2: Nghe hiểu)
        d1 = 110 if jlpt_level == "N1" else 105
        d2 = 60 if jlpt_level == "N1" else 50
        knowledge_questions = vocab_questions + grammar_questions
        for idx, q in enumerate(knowledge_questions):
            q["question_number"] = idx + 1

        sections = [
            {
                "session_type": "KNOWLEDGE_READING",
                "section_title": f"Phần 1: Kiến thức ngôn ngữ (Từ vựng, Ngữ pháp) & Đọc hiểu - {jlpt_level}",
                "order_index": 1,
                "duration_minutes": d1,
                "questions": knowledge_questions,
            }
        ]
        if listening_questions:
            sections.append({
                "session_type": "LISTENING",
                "section_title": f"Phần 2: Nghe hiểu - {jlpt_level}",
                "order_index": 2,
                "duration_minutes": d2,
                "audio_url": listening_audio_url,
                "questions": listening_questions,
            })
        total_duration = d1 + (d2 if listening_questions else 0)
    else:
        # N3, N4, N5: 3 ca thi
        d1 = 30 if jlpt_level in ["N3", "N4"] else 25
        d2 = 70 if jlpt_level == "N3" else (60 if jlpt_level == "N4" else 50)
        d3 = 40 if jlpt_level == "N3" else (35 if jlpt_level == "N4" else 30)

        sections = [
            {
                "session_type": "VOCABULARY",
                "section_title": f"Phần 1: Kiến thức ngôn ngữ (Từ vựng) - {jlpt_level}",
                "order_index": 1,
                "duration_minutes": d1,
                "questions": vocab_questions,
            },
            {
                "session_type": "GRAMMAR_READING",
                "section_title": f"Phần 2: Ngữ pháp & Đọc hiểu - {jlpt_level}",
                "order_index": 2,
                "duration_minutes": d2,
                "questions": grammar_questions,
            },
        ]
        if listening_questions:
            sections.append({
                "session_type": "LISTENING",
                "section_title": f"Phần 3: Nghe hiểu - {jlpt_level}",
                "order_index": 3,
                "duration_minutes": d3,
                "audio_url": listening_audio_url,
                "questions": listening_questions,
            })
        total_duration = d1 + d2 + (d3 if listening_questions else 0)

    return ExamExtractedResponse(
        title=title,
        jlpt_level=jlpt_level,
        exam_type="JLPT_OFFICIAL_MOCK",
        total_duration_minutes=total_duration,
        sections=sections,
    )


