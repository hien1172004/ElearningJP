package com.jp.elearningjp.controller.dictionary;

import com.jp.elearningjp.dto.response.dictionary.*;
import com.jp.elearningjp.service.dictionary.DictionaryService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.enums.CharacterType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.response.ApiResponse;
import com.jp.elearningjp.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.Dictionary.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Dictionary (Tra cứu từ điển)", description = "API tra cứu từ vựng, Hán tự, Ngữ pháp và Bảng chữ cái tiếng Nhật kiểu Mazii")
public class DictionaryController {

    DictionaryService dictionaryService;

    @Operation(summary = "Tìm kiếm tổng hợp đa năng (Unified Search)", description = "Tìm nhanh top 5 Từ vựng, top 5 Hán tự, top 5 Ngữ pháp theo từ khóa để gợi ý ngay lập tức.")
    @GetMapping(ApiPaths.Dictionary.SEARCH)
    public ApiResponse<DictionaryUnifiedSearchResponse> searchUnified(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) JlptLevel level
    ) {
        return ApiResponse.<DictionaryUnifiedSearchResponse>builder()
                .success(true)
                .data(dictionaryService.searchUnified(keyword, level))
                .build();
    }

    @Operation(summary = "Tìm kiếm Từ vựng phân trang", description = "Tìm kiếm từ vựng theo từ viết, hiragana, romaji, hán việt hoặc nghĩa tiếng Việt.")
    @GetMapping(ApiPaths.Dictionary.SEARCH_VOCAB)
    public ApiResponse<PageResponse<VocabularySummaryResponse>> searchVocabularies(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) JlptLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<VocabularySummaryResponse>>builder()
                .success(true)
                .data(dictionaryService.searchVocabularies(keyword, level, page, size))
                .build();
    }

    @Operation(summary = "Xem chi tiết Từ vựng", description = "Lấy đầy đủ thông tin từ vựng, các tầng nghĩa, câu ví dụ song ngữ và các chữ Hán cấu thành.")
    @GetMapping(ApiPaths.Dictionary.VOCAB)
    public ApiResponse<VocabularyDetailResponse> getVocabularyDetail(@PathVariable Long id) {
        return ApiResponse.<VocabularyDetailResponse>builder()
                .success(true)
                .data(dictionaryService.getVocabularyDetail(id))
                .build();
    }

    @Operation(summary = "Tìm kiếm Hán tự (Kanji) phân trang", description = "Tìm kiếm Hán tự theo ký tự Kanji, âm Hán Việt hoặc nghĩa tiếng Việt.")
    @GetMapping(ApiPaths.Dictionary.SEARCH_KANJI)
    public ApiResponse<PageResponse<KanjiSummaryResponse>> searchKanjis(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) JlptLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<KanjiSummaryResponse>>builder()
                .success(true)
                .data(dictionaryService.searchKanjis(keyword, level, page, size))
                .build();
    }

    @Operation(summary = "Xem chi tiết Hán tự (Kanji)", description = "Lấy chi tiết Hán tự, thứ tự các nét vẽ vector SVG và các từ vựng liên quan gom nhóm theo âm đọc On/Kun.")
    @GetMapping(ApiPaths.Dictionary.KANJI)
    public ApiResponse<KanjiDetailResponse> getKanjiDetail(@PathVariable String character) {
        return ApiResponse.<KanjiDetailResponse>builder()
                .success(true)
                .data(dictionaryService.getKanjiDetail(character))
                .build();
    }

    @Operation(summary = "Tìm kiếm Ngữ pháp phân trang", description = "Tìm kiếm mẫu ngữ pháp theo tên, cấu trúc hoặc ý nghĩa.")
    @GetMapping(ApiPaths.Dictionary.SEARCH_GRAMMAR)
    public ApiResponse<PageResponse<GrammarSummaryResponse>> searchGrammars(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) JlptLevel level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<GrammarSummaryResponse>>builder()
                .success(true)
                .data(dictionaryService.searchGrammars(keyword, level, page, size))
                .build();
    }

    @Operation(summary = "Xem chi tiết Ngữ pháp", description = "Lấy chi tiết cấu trúc ngữ pháp, hướng dẫn cách dùng và các câu ví dụ mẫu.")
    @GetMapping(ApiPaths.Dictionary.GRAMMAR)
    public ApiResponse<GrammarDetailResponse> getGrammarDetail(@PathVariable Long id) {
        return ApiResponse.<GrammarDetailResponse>builder()
                .success(true)
                .data(dictionaryService.getGrammarDetail(id))
                .build();
    }

    @Operation(summary = "Lấy ma trận Bảng chữ cái tiếng Nhật (Kana)", description = "Lấy cấu trúc toàn bộ bảng chữ cái Hiragana hoặc Katakana chia theo hàng A, Ka, Sa... kèm âm đục và âm ghép.")
    @GetMapping(ApiPaths.Dictionary.KANA)
    public ApiResponse<KanaMatrixResponse> getKanaMatrix(
            @RequestParam(required = false, defaultValue = "HIRAGANA") CharacterType type
    ) {
        return ApiResponse.<KanaMatrixResponse>builder()
                .success(true)
                .data(dictionaryService.getKanaMatrix(type))
                .build();
    }

    @Operation(summary = "Xem chi tiết chữ cái Kana", description = "Lấy thông tin và nét vẽ vector SVG của chữ cái Hiragana hoặc Katakana.")
    @GetMapping(ApiPaths.Dictionary.KANA_DETAIL)
    public ApiResponse<KanaCharacterResponse> getKanaDetail(@PathVariable String character) {
        return ApiResponse.<KanaCharacterResponse>builder()
                .success(true)
                .data(dictionaryService.getKanaDetail(character))
                .build();
    }

    @Operation(summary = "Tra cứu nhanh tức thì (Popup Lookup)", description = "Tra cứu chính xác từ vựng khi người dùng bôi đen văn bản.")
    @GetMapping(ApiPaths.Dictionary.QUICK_LOOKUP)
    public ApiResponse<List<VocabularySummaryResponse>> quickLookup(@RequestParam String text) {
        return ApiResponse.<List<VocabularySummaryResponse>>builder()
                .success(true)
                .data(dictionaryService.quickLookup(text))
                .build();
    }
}
