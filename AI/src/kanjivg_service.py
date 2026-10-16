"""
Dịch vụ quản lý và truy vấn bộ dữ liệu nét chuẩn KanjiVG (2.353 ký tự Kanji & Kana).
Sử dụng Singleton Pattern và bộ nhớ đệm In-Memory để tối ưu tốc độ truy vấn (< 0.001s).
"""

import json
import logging
from pathlib import Path
from typing import Dict, Any, List, Optional

logger = logging.getLogger("kanji_ai")


class KanjiVGService:
    _instance: Optional["KanjiVGService"] = None

    def __init__(self):
        self.data_dir = Path(__file__).resolve().parent.parent / "data"
        self.db_path = self.data_dir / "kanjivg_all.json"
        self.db: Dict[str, Dict[str, Any]] = {}
        self._load_database()

    def _load_database(self) -> None:
        """Nạp dữ liệu từ file JSON vào bộ nhớ đệm RAM."""
        if not self.db_path.exists():
            logger.warning(f"⚠️ Không tìm thấy file dữ liệu KanjiVG tại: {self.db_path}")
            return

        try:
            with open(self.db_path, "r", encoding="utf-8") as f:
                self.db = json.load(f)
            logger.info(f"✅ KanjiVGService: Đã nạp thành công {len(self.db)} ký tự vào bộ nhớ RAM.")
        except Exception as e:
            logger.error(f"❌ Lỗi khi đọc file dữ liệu KanjiVG: {e}", exc_info=True)

    @classmethod
    def get_instance(cls) -> "KanjiVGService":
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def get_character(self, character: str) -> Optional[Dict[str, Any]]:
        """Truy xuất thông tin nét vẽ và từ điển của một ký tự."""
        if not character:
            return None
        return self.db.get(character.strip())

    def list_characters(
        self,
        level: Optional[str] = None,
        limit: int = 250,
    ) -> List[Dict[str, Any]]:
        """
        Lấy danh sách các ký tự theo bộ lọc cấp độ JLPT hoặc Kana.
        """
        results: List[Dict[str, Any]] = []
        lvl_upper = level.upper().strip() if level else None

        for ch, data in self.db.items():
            if lvl_upper:
                if lvl_upper in ["KANA", "HIRAGANA", "KATAKANA"]:
                    if data.get("char_type") not in ["HIRAGANA", "KATAKANA"]:
                        continue
                else:
                    if data.get("jlpt_level") != lvl_upper:
                        continue

            results.append({
                "character": ch,
                "char_type": data.get("char_type"),
                "stroke_count": data.get("stroke_count", len(data.get("strokes", []))),
                "han_viet": data.get("han_viet"),
                "meaning_vi": data.get("meaning_vi"),
                "jlpt_level": data.get("jlpt_level"),
            })

            if len(results) >= limit:
                break

        return results
