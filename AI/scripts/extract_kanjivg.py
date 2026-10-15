"""
Script trích xuất dữ liệu nét vẽ KanjiVG và thông tin từ điển từ các file migration Flyway của Backend (BE)
sang file cơ sở dữ liệu JSON: AI/data/kanjivg_all.json (hơn 2.353 ký tự và 23.609 nét vẽ SVG).
"""

import glob
import json
import logging
import re
from pathlib import Path

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("kanjivg_extractor")


def detect_char_type(ch: str) -> str:
    """Xác định loại ký tự: HIRAGANA, KATAKANA, hoặc KANJI."""
    if not ch:
        return "KANJI"
    code = ord(ch[0])
    if 0x3040 <= code <= 0x309F:
        return "HIRAGANA"
    elif 0x30A0 <= code <= 0x30FF:
        return "KATAKANA"
    return "KANJI"


def extract_kanjivg_from_migrations() -> None:
    # Xác định thư mục gốc của repository
    script_dir = Path(__file__).resolve().parent
    repo_root = script_dir.parent.parent  # ElearningJP/

    migration_dir = repo_root / "BE" / "elearningJP" / "src" / "main" / "resources" / "db" / "migration"
    if not migration_dir.exists():
        logger.error(f"Không tìm thấy thư mục migration tại: {migration_dir}")
        return

    sql_files = sorted(glob.glob(str(migration_dir / "V*.sql")))
    logger.info(f"Đang quét {len(sql_files)} file SQL migration...")

    characters_data = {}

    stroke_regex = re.compile(
        r"INSERT INTO character_strokes\s*\([^)]+\)\s*"
        r"SELECT\s+id,\s*(\d+),\s*'([^']+)',\s*'([^']*)'::jsonb\s*"
        r"FROM writing_characters WHERE character = '([^']+)'",
        re.MULTILINE
    )

    char_regex = re.compile(
        r"INSERT INTO writing_characters\s*\([^)]+\)\s*"
        r"VALUES\s*\('([^']+)',\s*'([^']+)',\s*(\d+),\s*ARRAY\[([^\]]*)\](?:::TEXT\[\])?,\s*ARRAY\[([^\]]*)\](?:::TEXT\[\])?,\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)'",
        re.MULTILINE
    )

    for fpath in sql_files:
        with open(fpath, "r", encoding="utf-8") as f:
            content = f.read()

        # Trích xuất metadata của ký tự
        for match in char_regex.finditer(content):
            ch = match.group(1)
            char_type = match.group(2)
            stroke_count = int(match.group(3))
            han_viet = match.group(6)
            meaning_vi = match.group(7)
            meaning_en = match.group(8)
            romaji = match.group(9)
            jlpt_level = match.group(10)

            if ch not in characters_data:
                characters_data[ch] = {
                    "character": ch,
                    "char_type": char_type,
                    "stroke_count": stroke_count,
                    "han_viet": han_viet,
                    "meaning_vi": meaning_vi,
                    "meaning_en": meaning_en,
                    "romaji": romaji,
                    "jlpt_level": jlpt_level,
                    "strokes": []
                }
            else:
                characters_data[ch]["char_type"] = char_type
                characters_data[ch]["stroke_count"] = stroke_count
                characters_data[ch]["han_viet"] = han_viet
                characters_data[ch]["meaning_vi"] = meaning_vi
                characters_data[ch]["meaning_en"] = meaning_en
                characters_data[ch]["romaji"] = romaji
                characters_data[ch]["jlpt_level"] = jlpt_level

        # Trích xuất thông tin các nét SVG
        for match in stroke_regex.finditer(content):
            stroke_num = int(match.group(1))
            svg_path = match.group(2)
            char_ref = match.group(4)

            if char_ref not in characters_data:
                c_type = detect_char_type(char_ref)
                characters_data[char_ref] = {
                    "character": char_ref,
                    "char_type": c_type,
                    "stroke_count": 0,
                    "han_viet": "",
                    "meaning_vi": "",
                    "meaning_en": "",
                    "romaji": "",
                    "jlpt_level": "N5" if c_type in ["HIRAGANA", "KATAKANA"] else None,
                    "strokes": []
                }

            characters_data[char_ref]["strokes"].append({
                "stroke_number": stroke_num,
                "svg_path_data": svg_path
            })

    # Sắp xếp các nét theo đúng thứ tự 1..N và hoàn thiện stroke_count
    valid_count = 0
    total_strokes = 0
    for ch, data in characters_data.items():
        data["strokes"].sort(key=lambda s: s["stroke_number"])
        n_strokes = len(data["strokes"])
        total_strokes += n_strokes
        if data["stroke_count"] == 0:
            data["stroke_count"] = n_strokes
        if n_strokes > 0:
            valid_count += 1

    logger.info(f"Tổng số ký tự trích xuất được: {len(characters_data)}")
    logger.info(f"Số ký tự có đầy đủ nét vẽ SVG: {valid_count}")
    logger.info(f"Tổng số nét vẽ trích xuất: {total_strokes}")

    out_dir = script_dir.parent / "data"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_file = out_dir / "kanjivg_all.json"

    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(characters_data, f, ensure_ascii=False)

    logger.info(f"✅ Đã ghi thành công vào {out_file} ({out_file.stat().st_size / 1024:.1f} KB)")


if __name__ == "__main__":
    extract_kanjivg_from_migrations()
