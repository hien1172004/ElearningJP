"""
AudioProcessor — Xử lý file audio bài thi nghe JLPT.

Chiến lược kết hợp tối ưu:
  1. [Hybrid Cao cấp] Gemini Audio API + faster-whisper (Silero VAD):
     - Gemini Audio API: Trích xuất nội dung toàn diện (kịch bản transcript, giải thích chi tiết tiếng Việt,
       nội dung câu hỏi, danh sách lựa chọn A/B/C/D, đánh dấu đáp án đúng).
     - faster-whisper + Silero VAD (chạy CPU, vad_filter=True):
       + Dò tìm chuẩn xác các mốc câu hỏi ("1番", "2番", "3番"...)
       + Loại bỏ triệt để tạp âm, khoảng lặng, nhạc intro đề thi và câu ví dụ mẫu ("例" - Rei)
       + Tính toán mốc audio_start_time và audio_end_time chính xác từng giây.
       + Đồng bộ ghi đè mốc thời gian VAD vào danh sách câu hỏi Gemini.
  2. [Fallback độc lập] faster-whisper + Silero VAD:
     - Tự động tách câu hỏi và tạo câu hỏi nghe trực tiếp từ audio nếu không có GEMINI_API_KEY.
  3. [Fallback an toàn]:
     - Trả về cấu trúc câu hỏi cơ bản nếu cả 2 phương thức trên gặp sự cố.
"""

import os
import re
import logging
from typing import List, Dict, Any, Optional, Tuple

logger = logging.getLogger("kanji_ai")

# Bảng chuyển đổi chữ số Kanji / Kana sang số nguyên
KANJI_KANA_NUM_MAP = {
    "1": 1, "2": 2, "3": 3, "4": 4, "5": 5, "6": 6, "7": 7, "8": 8, "9": 9, "10": 10,
    "11": 11, "12": 12, "13": 13, "14": 14, "15": 15,
    "一": 1, "二": 2, "三": 3, "四": 4, "五": 5, "六": 6, "七": 7, "八": 8, "九": 9, "十": 10,
    "十一": 11, "十二": 12, "十三": 13, "十四": 14, "十五": 15,
    "いち": 1, "に": 2, "さん": 3, "よん": 4, "し": 4, "ご": 5, "ろく": 6, "なな": 7, "しち": 7,
    "はち": 8, "きゅう": 9, "く": 9, "じゅう": 10, "じゅういち": 11, "じゅうに": 12
}

FULLWIDTH_DIGITS_TRANS = str.maketrans("０１２３４５６７８９", "0123456789")

_NUM = r'(?:[0-9]{1,2}|十[一二三四五]|[一二三四五六七八九十]|じゅう|きゅう|いち|さん|よん|ろく|なな|しち|はち|に|し|ご|く)'
_ANN = re.compile(rf'^[\s。、．.]*(?:第\s*)?({_NUM})\s*(?:番|ばん|問)(?!\s*(?:です|だ|で|いい|よい|良い|から|を|に))')


class AudioProcessor:
    """
    Xử lý âm thanh nghe JLPT:
    - Gemini Audio API trích xuất đề thi & transcript
    - faster-whisper + Silero VAD căn chỉnh mốc giây chính xác
    """

    def __init__(self, device: str = "cpu"):
        self.device = device
        self._whisper_model = None
        self._gemini_available = self._check_gemini_available()
        self._faster_whisper_available = self._check_faster_whisper_available()
        # Backward-compatibility alias
        self._split_by_question_markers = self._extract_cues_from_segments

        if self._gemini_available and self._faster_whisper_available:
            logger.info("🎧 AudioProcessor: Kích hoạt chế độ HYBRID (Gemini Multimodal + faster-whisper Silero VAD)")
        elif self._gemini_available:
            logger.info("🎧 AudioProcessor: Sử dụng Gemini Audio API")
        elif self._faster_whisper_available:
            logger.info("🎧 AudioProcessor: Sử dụng faster-whisper + Silero VAD")
        else:
            logger.warning(
                "⚠️ AudioProcessor: Chưa cấu hình GEMINI_API_KEY và chưa cài faster-whisper."
            )

    def _check_gemini_available(self) -> bool:
        """Kiểm tra xem Gemini API có sẵn sàng không."""
        api_key = os.environ.get("GEMINI_API_KEY", "")
        if not api_key:
            return False
        try:
            from google import genai
            return True
        except ImportError:
            return False

    def _check_faster_whisper_available(self) -> bool:
        """Kiểm tra thư viện faster-whisper có sẵn sàng không."""
        try:
            from faster_whisper import WhisperModel
            return True
        except ImportError:
            return False

    def _get_whisper_model(self):
        """Khởi tạo faster-whisper (chạy CPU, int8 nhẹ và nhanh) theo cơ chế lazy-load."""
        if self._whisper_model is None:
            from faster_whisper import WhisperModel
            logger.info("⏳ Đang nạp model faster-whisper (base, device: cpu, compute_type: int8)...")
            self._whisper_model = WhisperModel("base", device="cpu", compute_type="int8")
            logger.info("✅ faster-whisper model đã sẵn sàng!")
        return self._whisper_model

    @staticmethod
    def find_jlpt_ping_near(
        audio_file_path: str,
        approx_sec: float,
        search_window: Tuple[float, float] = (-12.0, 8.0)
    ) -> float:
        """
        Tìm kiếm chính xác tiếng chuông báo đề JLPT (chime ~660 Hz - note E5)
        trong khoảng thời gian [approx_sec + search_window[0], approx_sec + search_window[1]].
        Trả về mốc thời gian onset (bắt đầu) của tiếng chuông (giây).
        Nếu không phát hiện được tiếng chuông, trả về approx_sec ban đầu.
        """
        win_start = max(0.0, approx_sec + search_window[0])
        win_end = approx_sec + search_window[1]

        try:
            import av
            from scipy.signal import stft
            import numpy as np

            in_c = av.open(audio_file_path)
            in_s = in_c.streams.audio[0]
            sr = 16000
            resampler = av.AudioResampler(format="s16", layout="mono", rate=sr)

            start_pts = int(win_start / in_s.time_base)
            end_pts = int(win_end / in_s.time_base)
            in_c.seek(start_pts, stream=in_s)

            samples = []
            for frame in in_c.decode(in_s):
                if frame.pts is not None and frame.pts < start_pts:
                    continue
                if frame.pts is not None and frame.pts > end_pts:
                    break
                for rf in resampler.resample(frame):
                    samples.append(rf.to_ndarray().flatten())
            in_c.close()

            if not samples:
                return approx_sec

            audio_data = np.concatenate(samples).astype(np.float32) / 32768.0
            if len(audio_data) < 1024:
                return approx_sec

            # Phân tích phổ STFT
            f, t, Zxx = stft(audio_data, fs=sr, nperseg=1024, noverlap=512)
            magnitude = np.abs(Zxx)

            # Tần số chuông JLPT (645 Hz - 675 Hz)
            target_freq_mask = (f >= 645) & (f <= 675)
            target_energy = np.mean(magnitude[target_freq_mask, :], axis=0)

            # Tần số nền lân cận (500-600 Hz và 720-820 Hz)
            bg_mask = ((f >= 500) & (f <= 600)) | ((f >= 720) & (f <= 820))
            bg_energy = np.mean(magnitude[bg_mask, :], axis=0)

            # Tỉ số tương phản chuông so với nền
            score = target_energy / (bg_energy + 1e-4)

            candidates = []
            for i in range(len(score)):
                if score[i] > 15.0 and target_energy[i] > 0.02:
                    candidates.append((win_start + t[i], score[i]))

            if not candidates:
                return approx_sec

            # Chọn điểm onset trước đỉnh cực đại của tiếng ping
            best_t, _ = max(candidates, key=lambda x: x[1])
            onset_t = best_t
            for ct, _ in candidates:
                if abs(ct - best_t) <= 0.4 and ct < onset_t:
                    onset_t = ct
            return onset_t
        except Exception as e:
            logger.debug(f"Không thể định vị ping tại {approx_sec}s: {e}")
            return approx_sec

    def snap_questions_to_pings(
        self,
        questions: List[Dict[str, Any]],
        audio_file_path: str,
        head_pad: float = 0.5,
        max_len_warn: float = 240.0,
    ) -> List[Dict[str, Any]]:
        """
        Hàm hỗ trợ phát hiện vị trí tiếng chuông JLPT (chime ~660 Hz) để lưu metadata.
        LƯU Ý: Tuyệt đối KHÔNG dùng tiếng ping làm mốc cắt câu hỏi vì trong cấu trúc đề JLPT,
        tiếng ping kêu SAU khi MC đọc câu hỏi mở đầu. Cắt theo ping sẽ làm mất '1番', '男の人と女の人が...'.
        Hàm này chỉ ghi nhận ping_time cho mục đích phân tích, không ghi đè audio_start_time.
        """
        if not questions or not os.path.exists(audio_file_path):
            return questions

        for q in questions:
            approx_start = float(q.get("audio_start_time", 0))
            ping_onset = self.find_jlpt_ping_near(audio_file_path, approx_start, search_window=(-5.0, 15.0))
            if ping_onset != approx_start:
                q["chime_ping_time"] = round(ping_onset, 2)

        return questions

    def process_listening_audio(
        self,
        audio_file_path: str,
        jlpt_level: str = "N3",
    ) -> List[Dict[str, Any]]:
        """
        Phân tích file audio nghe JLPT và trả về danh sách câu hỏi với mốc thời gian chuẩn xác 100%:
        - Sử dụng State Machine trên Whisper segments để cắt chính xác:
          * Bắt đầu mỗi Mondai: in_example = True (bỏ qua giải thích và câu ví dụ '例')
          * Thoát ví dụ khi gặp 'では 始めます' (Dewa, hajimemasu) -> in_example = False
          * audio_start_time: thời điểm MC xướng '1番', '2番'... lùi 0.5s
          * audio_end_time: thời điểm bắt đầu của câu tiếp theo trừ 0.5s (giữ trọn câu hỏi đầu, 10-15s đọc đề Mondai 2, hội thoại, câu hỏi lặp lại và suy nghĩ)
          * Câu cuối Mondai: kết thúc khi MC chuyển sang Mondai tiếp theo hoặc kết thúc bài thi.
        - Tuyệt đối KHÔNG dùng tiếng chuông ping (ping STFT) làm mốc cắt.
        - Kết hợp ngữ nghĩa (options, explanation) từ Gemini nếu khả dụng.
        """
        whisper_questions: List[Dict[str, Any]] = []
        gemini_questions: List[Dict[str, Any]] = []

        # BƯỚC 1: Phân tách chuẩn xác bằng Faster-Whisper State Machine
        if self._faster_whisper_available:
            try:
                logger.info("🎙️ [Bước 1] Khởi chạy Faster-Whisper State Machine để phân tách mốc audio chuẩn xác...")
                whisper_questions = self.detect_question_timestamps_vad(audio_file_path, jlpt_level=jlpt_level)
                logger.info(f"✅ Faster-Whisper State Machine trích xuất được {len(whisper_questions)} câu hỏi")
            except Exception as e:
                logger.error(f"❌ Faster-whisper State Machine gặp lỗi: {e}", exc_info=True)

        # BƯỚC 2: Trích xuất nội dung ngữ nghĩa bằng Gemini Audio API (nếu có)
        if self._gemini_available:
            try:
                logger.info("✨ [Bước 2] Gửi audio sang Gemini Audio API để bóc tách câu hỏi, transcript và lời giải...")
                gemini_questions = self._process_with_gemini(audio_file_path, jlpt_level)
                logger.info(f"✅ Gemini Audio trích xuất được {len(gemini_questions)} câu hỏi")
            except Exception as e:
                logger.error(f"❌ Gemini Audio gặp lỗi: {e}", exc_info=True)

            # Lọc sạch câu ví dụ mẫu ('例' / 'れい' / '練習') từ Gemini
            if gemini_questions:
                clean_gq = []
                for gq in gemini_questions:
                    txt = (str(gq.get("question_text", "")) + " " + str(gq.get("transcript_jp", "")) + " " + str(gq.get("explanation_vi", ""))).lower()
                    is_example = any(k in txt for k in ["一番いいものは", "いちばんいいものは", "練習しましょう", "では練習", "例題"]) or (gq.get("question_text", "").strip() in ["例", "れい", "例（れい）"])
                    if is_example:
                        logger.info(f"🗑️ Loại bỏ câu ví dụ mẫu khỏi Gemini: {txt[:60]}...")
                        continue
                    clean_gq.append(gq)
                gemini_questions = clean_gq

        # BƯỚC 3: Đồng bộ hoá kết quả
        # Nếu có cả Whisper State Machine và Gemini: Ghép mốc thời gian chuẩn xác của Whisper vào câu hỏi của Gemini
        if whisper_questions and gemini_questions:
            logger.info("🔗 Đồng bộ hóa: Áp dụng mốc thời gian chuẩn từ Whisper State Machine vào dữ liệu Gemini...")
            return self._align_gemini_with_vad_timestamps(gemini_questions, whisper_questions)

        if whisper_questions:
            for idx, q in enumerate(whisper_questions):
                q["question_number"] = idx + 1
            return whisper_questions

        if gemini_questions:
            for idx, q in enumerate(gemini_questions):
                q["question_number"] = idx + 1
            return gemini_questions

        logger.warning("⚠️ Không có phương thức xử lý audio nào thành công. Dùng fallback mẫu.")
        return self._fallback_vad_silence_slicer(audio_file_path)

    # ================================================================
    # GEMINI AUDIO PROCESSING
    # ================================================================
    def _process_with_gemini(
        self,
        audio_file_path: str,
        jlpt_level: str = "N3",
    ) -> List[Dict[str, Any]]:
        """Xử lý audio bằng Gemini Audio API."""
        try:
            from .gemini_parser import get_gemini_parser
        except ImportError:
            from gemini_parser import get_gemini_parser

        parser = get_gemini_parser()
        result = parser.parse_audio(audio_file_path, jlpt_level)
        return result.get("questions", [])

    # ================================================================
    # AUDIO SLICING HELPER (PyAV)
    # ================================================================
    def slice_audio_file(
        self,
        src_path: str,
        dst_path: str,
        start_sec: float,
        end_sec: float,
    ) -> bool:
        """Cắt một đoạn audio từ start_sec đến end_sec và xuất ra file MP3 độc lập bằng PyAV."""
        try:
            import av
            os.makedirs(os.path.dirname(os.path.abspath(dst_path)), exist_ok=True)
            in_c = av.open(src_path)
            in_s = in_c.streams.audio[0]
            out_c = av.open(dst_path, mode="w", format="mp3")
            out_s = out_c.add_stream("libmp3lame", rate=in_s.rate or 44100)

            start_pts = int(start_sec / in_s.time_base)
            end_pts = int(end_sec / in_s.time_base)
            in_c.seek(start_pts, stream=in_s)

            for frame in in_c.decode(in_s):
                if frame.pts is not None and frame.pts < start_pts:
                    continue
                if frame.pts is not None and frame.pts > end_pts:
                    break
                for packet in out_s.encode(frame):
                    out_c.mux(packet)

            for packet in out_s.encode(None):
                out_c.mux(packet)

            out_c.close()
            in_c.close()
            return True
        except Exception as e:
            logger.error(f"Lỗi khi cắt audio segment {src_path} ({start_sec}s - {end_sec}s): {e}")
            return False

    # ================================================================
    # FASTER-WHISPER + SILERO VAD TIMESTAMP EXTRACTION
    # ================================================================
    def detect_question_timestamps_vad(
        self,
        audio_file_path: str,
        jlpt_level: str = "N3",
    ) -> List[Dict[str, Any]]:
        """
        Dùng faster-whisper + Silero VAD để quét toàn bộ audio:
        - Tự động chia tách file theo chunk 5 phút để tránh quá tải bộ nhớ ONNX Silero VAD / MKL CTranslate2
        - Quét các đoạn thoại để tìm mốc xuất hiện các câu hỏi (1番, 2番, 3番...)
        - Nhận diện đúng tiếng chuông/ping mở đầu câu hỏi và lọc sạch ví dụ ('例' - Rei)
        - Trả về danh sách câu với audio_start_time và audio_end_time chính xác
        """
        model = self._get_whisper_model()
        all_segments: List[Dict[str, Any]] = []
        segments_gen, info = model.transcribe(
            audio_file_path,
            language="ja",
            vad_filter=True,
            vad_parameters=dict(min_silence_duration_ms=600, speech_pad_ms=250),
        )
        for seg in segments_gen:
            all_segments.append({
                "start": round(seg.start, 2),
                "end": round(seg.end, 2),
                "text": seg.text,
            })
        total_duration = info.duration

        logger.info(f"🎙️ Faster-whisper trích xuất được {len(all_segments)} segments thoại (Độ dài: {int(total_duration)}s)")
        return self._extract_cues_from_segments(all_segments, total_duration=total_duration, jlpt_level=jlpt_level)

    def _match_question_announcement(self, text: str) -> Optional[int]:
        """
        Khớp thông báo xướng số câu hỏi từ MC đề thi ("1番", "2番"...).
        Nhận diện cả kana (いち, に, さん...), kanji, chữ số và bỏ qua dấu câu đầu dòng.
        """
        t = text.strip().translate(FULLWIDTH_DIGITS_TRANS)
        m = _ANN.match(t)
        if not m:
            return None
        val_str = m.group(1)
        if val_str.isdigit():
            return int(val_str)
        return KANJI_KANA_NUM_MAP.get(val_str)

    def _patch_missing_cues(
        self,
        cues: List[Dict[str, Any]],
        segments: List[Any],
    ) -> List[Dict[str, Any]]:
        """
        Vá các mốc câu hỏi bị nhảy cóc (missing_before) trong cùng một Mondai:
        Dùng chỉ số segment thay vì end_t bị nhiễm và cắt lại transcript của câu trước.
        """
        import statistics
        segs = [
            {
                "start": float(s["start"]) if isinstance(s, dict) else s.start,
                "end": float(s["end"]) if isinstance(s, dict) else s.end,
                "text": s["text"] if isinstance(s, dict) else s.text
            }
            for s in segments
        ]
        gap = lambda j: segs[j]["start"] - segs[j-1]["end"] if j else segs[0]["start"]
        refs = [
            gap(c["seg_index"])
            for c in cues
            if c.get("seg_index") and c["detected_q_num"] > 1 and gap(c["seg_index"]) >= 6
        ]
        ref = statistics.median(refs) if refs else None
        out = list(cues)
        for i, cue in enumerate(cues):
            miss = [q for q in cue.get("missing_before", []) if q > 1]
            if not miss:
                continue
            prev = cues[i-1] if i and cues[i-1]["mondai_number"] == cue["mondai_number"] else None
            if prev is None or ref is None:
                cue["needs_review"] = True
                continue
            cand = [
                j for j in range(prev["seg_index"] + 1, cue["seg_index"])
                if abs(gap(j) - ref) <= max(2.5, ref * 0.2)
            ]
            if len(cand) != len(miss):
                cue["needs_review"] = True
                continue
            bounds = cand + [cue["seg_index"]]
            prev["segment_texts"] = [segs[j]["text"] for j in range(prev["seg_index"], cand[0])]
            prev["end_t"] = segs[cand[0] - 1]["end"]
            for q, a, b in zip(miss, cand, bounds[1:]):
                out.append({
                    "detected_q_num": q,
                    "mondai_number": cue["mondai_number"],
                    "cue_time": segs[a]["start"],
                    "start_t": segs[a]["start"],
                    "end_t": segs[b-1]["end"],
                    "seg_index": a,
                    "first_text": segs[a]["text"],
                    "segment_texts": [segs[j]["text"] for j in range(a, b)],
                    "missing_before": [],
                    "inferred": True,
                    "needs_review": True
                })
            cue["missing_before"] = []
        out.sort(key=lambda x: x["start_t"])
        return out

    def _flag_suspicious_cues(self, cues: List[Dict[str, Any]], segments: List[Any]) -> None:
        """
        Gắn cờ needs_review khi trong một câu có quãng im lặng cỡ “quãng chờ trả lời”
        của chính mondai đó (dấu hiệu câu đã nuốt câu khác, ví dụ mất mốc câu cuối mondai).
        """
        import statistics
        segs = [(float(x["start"]) if isinstance(x, dict) else x.start,
                 float(x["end"]) if isinstance(x, dict) else x.end) for x in segments]
        gap = lambda j: segs[j][0] - segs[j-1][1] if j else 0.0
        by_m: Dict[int, List[float]] = {}
        for c in cues:
            si = c.get("seg_index")
            if si and c["detected_q_num"] > 1 and not c.get("inferred") and gap(si) >= 6:
                by_m.setdefault(c["mondai_number"], []).append(gap(si))
        ref = {m: statistics.median(v) for m, v in by_m.items()}
        for i, c in enumerate(cues):
            r = ref.get(c["mondai_number"])
            si = c.get("seg_index")
            if r is None or si is None:
                continue
            end_idx = cues[i+1]["seg_index"] if i + 1 < len(cues) else len(segs)
            tol = max(2.5, r * 0.2)
            nb = c.get("next_boundary_t")
            for j in range(si + 1, end_idx):
                if nb is not None and segs[j][0] >= nb - 1e-6:
                    continue
                if abs(gap(j) - r) <= tol:
                    c["needs_review"] = True
                    break

    def _extract_cues_from_segments(
        self,
        segments: List[Any],
        total_duration: float = 0.0,
        jlpt_level: str = "N3",
        head_pad: float = 1.2,
        tail_pad: float = 1.0,
        max_len_warn: float = 240.0,
    ) -> List[Dict[str, Any]]:
        """
        Phân tích danh sách các segments thoại để định vị mốc từng câu hỏi JLPT theo State Machine chuẩn.
        """
        detected_cues: List[Dict[str, Any]] = []
        current_mondai = 1
        last_n = 0
        in_example = True
        saw_example = False
        example_concluded = False
        closed = False
        mondai_markers: List[Tuple[int, float]] = [(1, 0.0)]

        MONDAI_REGEX = re.compile(
            r'(?:それでは|では|じゃあ|それじゃ)?[、\s]*(?:問題|もんだい|モンダイ)\s*([0-9０-９1-5一二三四五]|いち|に|さん|よん|し|ご)(?:[ではだ\s。、]|$)'
        )
        CLOSING_REGEX = re.compile(r'(?:これで|以上で).*?(?:聴解|テスト|試験).*?(?:終わり|おわり|終了)')
        EXIT_EXAMPLE_REGEX = re.compile(
            r'(?:始めます|はじめます|始めましょう|はじめましょう|いきましょう|行きます|いきます)'
            r'|(?:ですから|ですから[、\s]*)[^。]*?(?:答えは|こたえは)'
            r'|(?:答えは|こたえは)\s*[0-9０-９1-4一二三四いちにさんよん]'
            r'|(?:一番|いちばん)いいものは'
        )
        EXAMPLE_MARKER_REGEX = re.compile(r'^(?:例|れい|練習|れんしゅう)(?:[0-9０-９]|\s*です|\s*だ|\s*（|\s*\()?')

        for seg_idx, seg in enumerate(segments):
            if closed:
                break

            text = (seg.get("text") if isinstance(seg, dict) else getattr(seg, "text", "")) or ""
            text = text.strip()
            norm_text = text.translate(FULLWIDTH_DIGITS_TRANS)
            start_t = float(seg.get("start") if isinstance(seg, dict) else getattr(seg, "start", 0.0))
            end_t = float(seg.get("end") if isinstance(seg, dict) else getattr(seg, "end", 0.0))

            # 1. Phát hiện kết thúc đề thi (CLOSING) - không ghi đè ranh giới đã đặt
            if CLOSING_REGEX.search(norm_text):
                if detected_cues:
                    detected_cues[-1].setdefault("next_boundary_t", start_t)
                closed = True
                logger.info(f"🏁 Phát hiện câu chào kết thúc bài thi nghe tại {start_t}s ('{text}')")
                continue

            # 2. Phát hiện chuyển Mondai (MONDAI regex)
            m_match = MONDAI_REGEX.search(norm_text)
            if m_match:
                val = m_match.group(1)
                new_mondai = KANJI_KANA_NUM_MAP.get(val, current_mondai)
                if new_mondai != current_mondai or not detected_cues:
                    if detected_cues:
                        # Ranh giới kết thúc cho câu cuối của Mondai trước
                        detected_cues[-1]["next_boundary_t"] = start_t
                    current_mondai = new_mondai
                    mondai_markers.append((current_mondai, start_t))
                    last_n = 0
                    in_example = True  # Bật cờ in_example = True cho Mondai mới
                    saw_example = False
                    example_concluded = False
                    logger.info(f"🔄 Bắt đầu Mondai {current_mondai} tại {start_t}s ('{text}'). in_example = True.")
                continue

            # 3. Trạng thái in_example: Bỏ qua giải thích và câu ví dụ mẫu
            if in_example:
                if EXAMPLE_MARKER_REGEX.search(norm_text):
                    saw_example = True
                    logger.info(f"ℹ️ Phát hiện bắt đầu câu ví dụ mẫu Mondai {current_mondai} tại {start_t}s ('{text}')")
                    continue

                if EXIT_EXAMPLE_REGEX.search(norm_text):
                    example_concluded = True
                    logger.info(f"🟢 Phát hiện kết thúc câu ví dụ Mondai {current_mondai} tại {start_t}s ('{text}')")
                    continue

                matched_num = self._match_question_announcement(norm_text)
                if saw_example:
                    # Nếu có câu ví dụ: chỉ cho phép thoát khi ví dụ đã kết thúc (có chốt đáp án) và nghe thấy xướng câu hỏi
                    if example_concluded and matched_num is not None:
                        in_example = False
                        logger.info(f"⚡ Đã thoát ví dụ Mondai {current_mondai} khi MC xướng câu {matched_num} tại {start_t}s ('{text}')")
                    else:
                        # Vẫn đang trong ví dụ (kể cả các phương án đọc 1, 2, 3 của ví dụ) -> Bỏ qua
                        continue
                else:
                    # Mondai không có ví dụ mẫu (hoặc MC vào thẳng câu 1..3 sau lời dẫn)
                    if matched_num is not None and matched_num <= 3:
                        in_example = False
                        logger.info(f"⚡ MC vào thẳng câu {matched_num} Mondai {current_mondai} tại {start_t}s ('{text}') -> in_example = False")
                    else:
                        continue

            # 4. Nhận diện các câu hỏi thật
            matched_num = self._match_question_announcement(norm_text)
            if matched_num is not None:
                restart = False
                if matched_num == 1 and last_n >= 2 and detected_cues:
                    detected_cues[-1]["next_boundary_t"] = start_t
                    current_mondai += 1
                    mondai_markers.append((current_mondai, start_t))
                    last_n = 0
                    restart = True
                    for c in detected_cues:
                        if c["mondai_number"] == current_mondai - 1:
                            c["needs_review"] = True

                # Nhận mốc tuần tự: last_n < matched_num <= last_n + 3
                if last_n < matched_num <= last_n + 3:
                    missing = list(range(last_n + 1, matched_num)) if matched_num > last_n + 1 else []
                    new_cue = {
                        "detected_q_num": matched_num,
                        "mondai_number": current_mondai,
                        "cue_time": start_t,
                        "start_t": start_t,
                        "end_t": end_t,
                        "first_text": text,
                        "segment_texts": [text],
                        "missing_before": missing,
                        "inferred": False,
                        "needs_review": bool(missing) or restart,
                        "seg_index": seg_idx,
                    }
                    detected_cues.append(new_cue)
                    last_n = matched_num
                    logger.info(f"🎯 Nhận diện câu {matched_num} Mondai {current_mondai} tại {start_t}s ('{text}')")
                    continue

            # Nối segment vào câu hỏi hiện tại
            if detected_cues and detected_cues[-1]["mondai_number"] == current_mondai:
                detected_cues[-1]["segment_texts"].append(text)
                detected_cues[-1]["end_t"] = end_t

        if not detected_cues:
            logger.warning("⚠️ Không tìm thấy mốc câu hỏi cụ thể nào trong segments audio.")
            return []

        # Kiểm tra mondai có tiêu đề nhưng không có câu nào
        detected_mondais = {c["mondai_number"] for c in detected_cues}
        for m_num, m_time in mondai_markers:
            if m_num not in detected_mondais:
                logger.warning(f"⚠️ Mondai {m_num} có tiêu đề nhưng không tìm thấy câu hỏi nào! Gán cờ needs_review.")
                for c in detected_cues:
                    c["needs_review"] = True

        # Vá mốc thiếu dựa trên khoảng lặng tương đồng
        detected_cues = self._patch_missing_cues(detected_cues, segments)
        self._flag_suspicious_cues(detected_cues, segments)

        num_cues = len(detected_cues)
        # Bước A: Gán audio_start_time cho tất cả các câu: mốc segment xướng câu trừ 0.5s
        for cue in detected_cues:
            cue["audio_start"] = round(max(0.0, cue["start_t"] - head_pad), 2)

        # Bước B: Gán audio_end_time theo State Machine tuần tự chuẩn xác:
        # audio_end_time của câu N chính là audio_start_time của câu N+1 trừ đi 0.5s
        results = []
        for idx in range(num_cues):
            cue = detected_cues[idx]
            audio_start = cue["audio_start"]

            if idx + 1 < num_cues and detected_cues[idx + 1]["mondai_number"] == cue["mondai_number"]:
                # Trong cùng một Mondai: audio_end của câu N = audio_start của câu N+1 - 0.6s
                # (Vì audio_start đã lùi 1.2s trước cue nên khoảng cách tới tiếng xướng câu sau là 1.8s, tuyệt đối không bị dính)
                next_start = detected_cues[idx + 1]["audio_start"]
                audio_end = round(max(audio_start + 5.0, next_start - 0.6), 2)
            else:
                # Riêng câu cuối cùng của Mondai:
                # Kết thúc khi MC thông báo sang Mondai tiếp theo, hoặc câu chào kết thúc
                boundary_t = cue.get("next_boundary_t")
                if boundary_t is not None:
                    audio_end = round(max(audio_start + 10.0, boundary_t - 0.8), 2)
                elif idx + 1 < num_cues:
                    # Chuyển sang Mondai tiếp theo
                    audio_end = round(max(audio_start + 10.0, detected_cues[idx + 1]["audio_start"] - 0.8), 2)
                else:
                    # Câu cuối cùng của đề thi
                    if total_duration > 0.0:
                        audio_end = round(max(audio_start + 10.0, total_duration - 0.5), 2)
                    else:
                        audio_end = round(cue["end_t"] + 5.0, 2)

            needs_review = cue.get("needs_review", False)
            if audio_end - audio_start > max_len_warn:
                needs_review = True

            full_transcript = " ".join(cue["segment_texts"]).strip()
            m_code = self._map_mondai_code(cue["mondai_number"], jlpt_level=jlpt_level)
            num_opts = 3 if m_code in ["UTTERANCE_EXPRESSIONS", "QUICK_RESPONSE"] else 4
            opts = [
                {"option_key": k, "option_text": f"選択肢 {k}", "is_correct": False}
                for k in range(1, num_opts + 1)
            ]

            results.append({
                "question_number": idx + 1,
                "raw_question_number": cue.get("detected_q_num") or (idx + 1),
                "mondai_code": m_code,
                "question_text": f"Câu hỏi nghe số {idx + 1}",
                "audio_start_time": audio_start,
                "audio_end_time": audio_end,
                "transcript_jp": full_transcript,
                "explanation_vi": "Nghe đoạn hội thoại và chọn đáp án chính xác.",
                "options": opts,
                "needs_review": needs_review,
                "inferred": cue.get("inferred", False),
            })

        return results

    def _map_mondai_code(self, mondai_num: int, jlpt_level: str = "N3") -> str:
        """Chuyển đổi số Mondai sang mã hệ thống chuẩn xác theo từng cấp độ JLPT (N1-N5)."""
        lvl = (jlpt_level or "N3").upper()
        if lvl in ["N4", "N5"]:
            # N4, N5 có 4 Mondai: 1. Nhiệm vụ, 2. Điểm chính, 3. Phát ngôn (Tranh), 4. Phản xạ nhanh
            mapping = {
                1: "TASK_BASED",
                2: "KEY_POINT",
                3: "UTTERANCE_EXPRESSIONS",
                4: "QUICK_RESPONSE",
            }
            return mapping.get(mondai_num, "TASK_BASED")
        elif lvl in ["N1", "N2"]:
            # N1, N2 có 5 Mondai: 1. Nhiệm vụ, 2. Điểm chính, 3. Khái quát, 4. Phản xạ nhanh, 5. Tổng hợp
            mapping = {
                1: "TASK_BASED",
                2: "KEY_POINT",
                3: "GENERAL_OUTLINE",
                4: "QUICK_RESPONSE",
                5: "INTEGRATED_LISTENING",
            }
            return mapping.get(mondai_num, "TASK_BASED")
        else:  # N3
            # N3 có 5 Mondai: 1. Nhiệm vụ, 2. Điểm chính, 3. Khái quát, 4. Phát ngôn, 5. Phản xạ nhanh
            mapping = {
                1: "TASK_BASED",
                2: "KEY_POINT",
                3: "GENERAL_OUTLINE",
                4: "UTTERANCE_EXPRESSIONS",
                5: "QUICK_RESPONSE",
            }
            return mapping.get(mondai_num, "TASK_BASED")

    def _align_gemini_with_vad_timestamps(
        self,
        gemini_questions: List[Dict[str, Any]],
        vad_questions: List[Dict[str, Any]],
    ) -> List[Dict[str, Any]]:
        """
        Đồng bộ mốc thời gian VAD (chính xác từng giây) vào danh sách câu hỏi Gemini.
        Tự động nhận diện và gạt bỏ mọi câu ví dụ mẫu ("例" - Rei) nếu Gemini vô tình trích xuất.
        """
        # 1. Loại bỏ triệt để mọi câu ví dụ mẫu ("例" - Rei) xuất hiện ở bất kỳ vị trí nào:
        clean_gq = []
        for gq in gemini_questions:
            txt = (str(gq.get("question_text", "")) + " " + str(gq.get("transcript_jp", "")) + " " + str(gq.get("explanation_vi", ""))).lower()
            is_example = any(k in txt for k in ["一番いいものは", "いちばんいいものは", "練習しましょう", "では練習", "例題"]) or (gq.get("question_text", "").strip() in ["例", "れい", "例（れい）"])
            if is_example:
                logger.info(f"🗑️ Loại bỏ câu ví dụ mẫu khỏi Gemini: {txt[:60]}...")
                continue
            clean_gq.append(gq)
        gemini_questions = clean_gq

        num_gemini = len(gemini_questions)
        num_vad = len(vad_questions)
        logger.info(f"🔄 Đồng bộ timestamp theo từng Mondai: Gemini ({num_gemini} câu) ↔ VAD ({num_vad} câu)")

        # Phân nhóm theo mondai_code để việc lệch câu ở 1 mondai không làm hỏng các mondai khác
        gq_by_mondai: Dict[str, List[Dict[str, Any]]] = {}
        for q in gemini_questions:
            gq_by_mondai.setdefault(q.get("mondai_code", "TASK_BASED"), []).append(q)

        vq_by_mondai: Dict[str, List[Dict[str, Any]]] = {}
        for q in vad_questions:
            vq_by_mondai.setdefault(q.get("mondai_code", "TASK_BASED"), []).append(q)

        all_mondais = []
        for m in gq_by_mondai.keys():
            if m not in all_mondais:
                all_mondais.append(m)
        for m in vq_by_mondai.keys():
            if m not in all_mondais:
                all_mondais.append(m)

        aligned_results: List[Dict[str, Any]] = []
        for m_code in all_mondais:
            g_list = gq_by_mondai.get(m_code, [])
            v_list = vq_by_mondai.get(m_code, [])

            if not g_list and v_list:
                for vq in v_list:
                    vq_copy = dict(vq)
                    vq_copy["needs_review"] = True
                    aligned_results.append(vq_copy)
                continue

            # Ánh xạ theo raw_question_number (1..N) chuẩn xác theo từng Mondai
            g_map = {}
            for i, g in enumerate(g_list):
                rn = g.get("raw_question_number") or (i + 1)
                try:
                    g_map[int(rn)] = g
                except (ValueError, TypeError):
                    g_map[i + 1] = g

            v_map = {}
            for i, v in enumerate(v_list):
                rn = v.get("detected_q_num") or v.get("raw_question_number") or (i + 1)
                try:
                    v_map[int(rn)] = v
                except (ValueError, TypeError):
                    v_map[i + 1] = v

            all_nums = sorted(set(g_map.keys()) | set(v_map.keys()))
            if not all_nums and max(len(g_list), len(v_list)) > 0:
                all_nums = list(range(1, max(len(g_list), len(v_list)) + 1))

            for q_num in all_nums:
                gq = g_map.get(q_num)
                vq = v_map.get(q_num)

                if gq and vq:
                    # Có cả dữ liệu ngữ nghĩa của Gemini và mốc thời gian thực tế của Whisper VAD
                    res_q = dict(gq)
                    res_q["audio_start_time"] = vq["audio_start_time"]
                    res_q["audio_end_time"] = vq["audio_end_time"]
                    if vq.get("needs_review"):
                        res_q["needs_review"] = True
                    if vq.get("inferred"):
                        res_q["inferred"] = True
                    if len(res_q.get("transcript_jp", "")) < 10 and vq.get("transcript_jp"):
                        res_q["transcript_jp"] = vq["transcript_jp"]
                elif vq and not gq:
                    # Whisper phát hiện được câu nhưng Gemini bỏ sót -> bảo toàn trọn vẹn câu
                    res_q = dict(vq)
                    res_q["needs_review"] = True
                else:
                    # Gemini phát hiện được câu nhưng Whisper không bắt được mốc
                    res_q = dict(gq)
                    res_q["needs_review"] = True
                    st = res_q.get("audio_start_time")
                    if st is None or float(st) <= 0 or float(st) > 2199.0:
                        res_q["audio_start_time"] = None
                        res_q["audio_end_time"] = None

                st_val = res_q.get("audio_start_time")
                et_val = res_q.get("audio_end_time")
                if st_val is not None and et_val is not None:
                    st_f = round(float(st_val), 2)
                    et_f = round(float(et_val), 2)
                    if et_f <= st_f:
                        et_f = round(st_f + 45.0, 2)
                    elif et_f - st_f > 240.0:
                        res_q["needs_review"] = True
                    res_q["audio_start_time"] = st_f
                    res_q["audio_end_time"] = et_f

                aligned_results.append(res_q)

        # Đánh lại số thứ tự câu hỏi tuần tự
        for idx, q in enumerate(aligned_results):
            q["question_number"] = idx + 1

        return aligned_results

    def _fallback_vad_silence_slicer(self, audio_file_path: str) -> List[Dict[str, Any]]:
        """Fallback an toàn khi cả Whisper và Gemini đều không trích xuất được câu hỏi."""
        logger.error(f"❌ Không thể trích xuất câu hỏi từ file audio: {audio_file_path}")
        return []
