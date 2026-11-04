package com.jp.elearningjp.service.dictionary;

import com.jp.elearningjp.dto.response.dictionary.*;
import com.jp.elearningjp.shared.enums.CharacterType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.response.PageResponse;

import java.util.List;

public interface DictionaryService {

    /**
     * Tra cứu tổng hợp đa năng: trả về top 5 kết quả cho Từ vựng, Hán tự, Ngữ pháp.
     */
    DictionaryUnifiedSearchResponse searchUnified(String keyword, JlptLevel level);

    /**
     * Tìm kiếm từ vựng phân trang kèm bộ lọc JLPT.
     */
    PageResponse<VocabularySummaryResponse> searchVocabularies(String keyword, JlptLevel level, int page, int size);

    /**
     * Lấy chi tiết từ vựng kèm các tầng nghĩa (senses), câu ví dụ và các chữ Hán cấu thành.
     */
    VocabularyDetailResponse getVocabularyDetail(Long id);

    /**
     * Tìm kiếm Hán tự phân trang kèm bộ lọc JLPT.
     */
    PageResponse<KanjiSummaryResponse> searchKanjis(String keyword, JlptLevel level, int page, int size);

    /**
     * Lấy chi tiết Hán tự kèm nét vẽ vector SVG và các từ vựng liên quan gom nhóm theo âm đọc On/Kun.
     */
    default KanjiDetailResponse getKanjiDetail(String character) {
        return getKanjiDetail(character, false);
    }

    /**
     * Như {@link #getKanjiDetail(String)}; {@code includeAllRelatedWords = true} trả về TOÀN BỘ
     * {@code allRelatedWords} (không cắt 20 từ) — dùng cho màn soạn bài của giáo viên.
     */
    KanjiDetailResponse getKanjiDetail(String character, boolean includeAllRelatedWords);

    /**
     * Tìm kiếm điểm ngữ pháp phân trang kèm bộ lọc JLPT.
     */
    PageResponse<GrammarSummaryResponse> searchGrammars(String keyword, JlptLevel level, int page, int size);

    /**
     * Lấy chi tiết điểm ngữ pháp kèm cấu trúc và ví dụ JSON.
     */
    GrammarDetailResponse getGrammarDetail(Long id);

    /**
     * Lấy dữ liệu ma trận bảng chữ cái (Hiragana / Katakana) nhóm theo hàng A, Ka, Sa... kèm âm đục, âm ghép.
     */
    KanaMatrixResponse getKanaMatrix(CharacterType charType);

    /**
     * Lấy chi tiết nét viết và thông tin một chữ cái Kana cụ thể.
     */
    KanaCharacterResponse getKanaDetail(String character);

    /**
     * Tra cứu nhanh tức thì cho popup / bôi đen từ (Exact match).
     */
    List<VocabularySummaryResponse> quickLookup(String text);
}
