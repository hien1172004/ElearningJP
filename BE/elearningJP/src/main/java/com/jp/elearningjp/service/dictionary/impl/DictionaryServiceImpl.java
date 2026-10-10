package com.jp.elearningjp.service.dictionary.impl;

import com.jp.elearningjp.dto.response.dictionary.*;
import com.jp.elearningjp.entity.content.CharacterStroke;
import com.jp.elearningjp.entity.content.GrammarPoint;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.VocabularyCharacter;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.dictionary.DictionaryMapper;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyCharacterRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.service.dictionary.DictionaryService;
import com.jp.elearningjp.shared.enums.CharacterType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.response.PageResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class DictionaryServiceImpl implements DictionaryService {

    VocabularyRepository vocabularyRepository;
    WritingCharacterRepository writingCharacterRepository;
    VocabularyCharacterRepository vocabularyCharacterRepository;
    GrammarPointRepository grammarPointRepository;
    DictionaryMapper dictionaryMapper;

    @Override
    public DictionaryUnifiedSearchResponse searchUnified(String keyword, JlptLevel level) {
        String cleanKeyword = (keyword == null) ? "" : keyword.trim();
        if (cleanKeyword.isEmpty()) {
            return DictionaryUnifiedSearchResponse.builder()
                    .keyword("")
                    .vocabularies(Collections.emptyList())
                    .kanjis(Collections.emptyList())
                    .grammars(Collections.emptyList())
                    .build();
        }

        Pageable top5 = PageRequest.of(0, 5);

        // 1. Tìm Top 5 Từ vựng
        Page<Vocabulary> vocabPage = vocabularyRepository.searchWithFilter(cleanKeyword, level, top5);
        List<VocabularySummaryResponse> vocabularies = dictionaryMapper.toVocabSummaryList(vocabPage.getContent());

        // 2. Tìm Top 5 Chữ Hán (Bóc tách thông minh nếu người dùng nhập từ/cụm từ như 記憶, 食べる, 日本語)
        List<WritingCharacter> smartKanjis = getSmartKanjiResults(cleanKeyword, level, top5);
        List<WritingCharacter> top5Kanjis = smartKanjis.stream().limit(5).toList();
        List<KanjiSummaryResponse> kanjis = dictionaryMapper.toKanjiSummaryList(top5Kanjis);

        // 3. Tìm Top 5 Ngữ pháp
        Page<GrammarPoint> grammarPage = grammarPointRepository.searchWithFilter(cleanKeyword, level, top5);
        List<GrammarSummaryResponse> grammars = dictionaryMapper.toGrammarSummaryList(grammarPage.getContent());

        return DictionaryUnifiedSearchResponse.builder()
                .keyword(cleanKeyword)
                .vocabularies(vocabularies)
                .kanjis(kanjis)
                .grammars(grammars)
                .build();
    }

    @Override
    public PageResponse<VocabularySummaryResponse> searchVocabularies(String keyword, JlptLevel level, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        String cleanKeyword = (keyword == null) ? "" : keyword.trim();
        Page<Vocabulary> vocabPage = vocabularyRepository.searchWithFilter(cleanKeyword, level, pageable);

        List<VocabularySummaryResponse> items = dictionaryMapper.toVocabSummaryList(vocabPage.getContent());

        return PageResponse.<VocabularySummaryResponse>builder()
                .items(items)
                .page(vocabPage.getNumber())
                .size(vocabPage.getSize())
                .totalElements(vocabPage.getTotalElements())
                .totalPages(vocabPage.getTotalPages())
                .build();
    }

    @Override
    public VocabularyDetailResponse getVocabularyDetail(Long id) {
        Vocabulary vocab = vocabularyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOCAB_NOT_FOUND));

        List<VocabularySenseResponse> senses = vocab.getSenses().stream()
                .filter(s -> !s.isDeleted())
                .map(dictionaryMapper::toSenseResponse)
                .toList();

        List<VocabularyExampleResponse> examples = new ArrayList<>(vocab.getExamples().stream()
                .filter(e -> !e.isDeleted())
                .map(dictionaryMapper::toExampleResponse)
                .toList());

        // Fallback trực tiếp từ trường exampleJp/exampleVi nếu bảng examples chưa có bản ghi
        if (examples.isEmpty() && vocab.getExampleJp() != null && !vocab.getExampleJp().isBlank()) {
            examples.add(VocabularyExampleResponse.builder()
                    .id(vocab.getId())
                    .exampleJp(vocab.getExampleJp())
                    .exampleVi(vocab.getExampleVi() != null ? vocab.getExampleVi() : "")
                    .exampleRomaji(vocab.getRomaji())
                    .build());
        }

        // Lấy danh sách các chữ Hán cấu thành từ vựng này
        List<VocabularyCharacter> vocabChars = vocabularyCharacterRepository.findCharactersByVocabId(vocab.getId());
        List<KanjiSimpleResponse> kanjiComponents = vocabChars.stream()
                .map(vc -> dictionaryMapper.toKanjiSimpleResponse(vc.getCharacter()))
                .toList();

        VocabularyDetailResponse response = dictionaryMapper.toVocabDetail(vocab);
        response.setSenses(senses);
        response.setExamples(examples);
        response.setKanjiComponents(kanjiComponents);

        return response;
    }

    @Override
    public PageResponse<KanjiSummaryResponse> searchKanjis(String keyword, JlptLevel level, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        String cleanKeyword = (keyword == null) ? "" : keyword.trim();

        List<String> kanjiChars = extractKanjiCharacters(cleanKeyword);
        // Nếu từ khoá chứa chữ Hán (ví dụ: 記憶, 食べる) hoặc có nhiều token Hán Việt cách nhau (ví dụ: kí ức)
        if (!kanjiChars.isEmpty() || cleanKeyword.contains(" ")) {
            List<WritingCharacter> smartList = getSmartKanjiResults(cleanKeyword, level, PageRequest.of(0, 100));
            int total = smartList.size();
            int fromIndex = Math.min(page * size, total);
            int toIndex = Math.min(fromIndex + size, total);
            List<WritingCharacter> paged = smartList.subList(fromIndex, toIndex);

            List<KanjiSummaryResponse> items = dictionaryMapper.toKanjiSummaryList(paged);
            int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);

            return PageResponse.<KanjiSummaryResponse>builder()
                    .items(items)
                    .page(page)
                    .size(size)
                    .totalElements(total)
                    .totalPages(Math.max(1, totalPages))
                    .build();
        }

        // Tìm kiếm thông thường theo từ khoá đơn (ký tự Hán đơn, âm Hán Việt đơn, Romaji, nghĩa)
        Page<WritingCharacter> kanjiPage = writingCharacterRepository.searchKanjiWithFilter(cleanKeyword, level, pageable);
        List<KanjiSummaryResponse> items = dictionaryMapper.toKanjiSummaryList(kanjiPage.getContent());

        return PageResponse.<KanjiSummaryResponse>builder()
                .items(items)
                .page(kanjiPage.getNumber())
                .size(kanjiPage.getSize())
                .totalElements(kanjiPage.getTotalElements())
                .totalPages(kanjiPage.getTotalPages())
                .build();
    }

    @Override
    public KanjiDetailResponse getKanjiDetail(String character) {
        WritingCharacter wc = writingCharacterRepository.findByCharacterAndDeletedFalse(character)
                .orElseThrow(() -> new AppException(ErrorCode.CHARACTER_NOT_FOUND));

        // Nét vẽ vector SVG
        List<CharacterStrokeResponse> strokes = wc.getStrokes().stream()
                .filter(s -> !s.isDeleted())
                .sorted(Comparator.comparing(CharacterStroke::getStrokeNumber))
                .map(dictionaryMapper::toStrokeResponse)
                .toList();

        // Các từ vựng liên quan chứa chữ Hán này, ưu tiên cấp độ JLPT từ N5 -> N1
        List<VocabularyCharacter> relatedVcs = vocabularyCharacterRepository.findRelatedWordsByCharacter(wc.getCharacter());
        List<KanjiRelatedWordResponse> allRelatedWords = relatedVcs.stream()
                .map(vc -> {
                    KanjiRelatedWordResponse r = dictionaryMapper.toKanjiRelatedWordResponse(vc);
                    if (r.getReading() == null || r.getReading().isBlank()) {
                        r.setReading("Khác");
                    }
                    return r;
                })
                .sorted(Comparator.comparing((KanjiRelatedWordResponse w) -> getJlptRank(w.getJlptLevel()))
                        .thenComparing(w -> w.getWord().length()))
                .toList();

        // Gom nhóm theo âm đọc cụ thể, giới hạn mỗi âm đọc tối đa 5 từ tiêu biểu nhất để giao diện gọn gàng
        Map<String, List<KanjiRelatedWordResponse>> wordsByReading = allRelatedWords.stream()
                .collect(Collectors.groupingBy(w -> w.getReading() != null ? w.getReading() : "Khác",
                        LinkedHashMap::new, 
                        Collectors.collectingAndThen(Collectors.toList(), list -> list.stream().limit(5).toList())));

        // Giới hạn danh sách allRelatedWords tối đa 20 từ tiêu biểu nhất
        List<KanjiRelatedWordResponse> limitedAllRelatedWords = allRelatedWords.stream().limit(20).toList();

        return KanjiDetailResponse.builder()
                .id(wc.getId())
                .character(wc.getCharacter())
                .strokeCount(wc.getStrokeCount())
                .onyomi(wc.getOnyomi())
                .kunyomi(wc.getKunyomi())
                .hanViet(wc.getHanViet())
                .meaningVi(wc.getMeaningVi())
                .meaningEn(wc.getMeaningEn())
                .romaji(wc.getRomaji())
                .jlptLevel(wc.getJlptLevel())
                .radicals(wc.getRadicals())
                .strokes(strokes)
                .wordsByReading(wordsByReading)
                .allRelatedWords(limitedAllRelatedWords)
                .build();
    }

    @Override
    public PageResponse<GrammarSummaryResponse> searchGrammars(String keyword, JlptLevel level, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        String cleanKeyword = (keyword == null) ? "" : keyword.trim();
        Page<GrammarPoint> grammarPage = grammarPointRepository.searchWithFilter(cleanKeyword, level, pageable);

        List<GrammarSummaryResponse> items = dictionaryMapper.toGrammarSummaryList(grammarPage.getContent());

        return PageResponse.<GrammarSummaryResponse>builder()
                .items(items)
                .page(grammarPage.getNumber())
                .size(grammarPage.getSize())
                .totalElements(grammarPage.getTotalElements())
                .totalPages(grammarPage.getTotalPages())
                .build();
    }

    @Override
    public GrammarDetailResponse getGrammarDetail(Long id) {
        GrammarPoint gp = grammarPointRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.GRAMMAR_NOT_FOUND));

        return dictionaryMapper.toGrammarDetailResponse(gp);
    }

    @Override
    public KanaMatrixResponse getKanaMatrix(CharacterType charType) {
        CharacterType targetType = (charType == null) ? CharacterType.HIRAGANA : charType;
        List<WritingCharacter> rawChars = writingCharacterRepository.findByCharTypeAndDeletedFalseAndActiveTrue(targetType);

        // Lưu bản đồ tra cứu theo romaji
        Map<String, KanaCharacterResponse> charMap = new HashMap<>();
        for (WritingCharacter wc : rawChars) {
            List<CharacterStrokeResponse> strokes = wc.getStrokes().stream()
                    .filter(s -> !s.isDeleted())
                    .sorted(Comparator.comparing(CharacterStroke::getStrokeNumber))
                    .map(dictionaryMapper::toStrokeResponse)
                    .toList();

            KanaCharacterResponse resp = dictionaryMapper.toKanaCharacterResponse(wc);
            resp.setStrokes(strokes);

            if (wc.getRomaji() != null) {
                charMap.put(wc.getRomaji().toLowerCase(), resp);
            }
            charMap.put(wc.getCharacter(), resp);
        }

        // Định nghĩa 11 hàng cơ bản (Seion - 50 âm)
        List<KanaRowGroupResponse> seion = new ArrayList<>();
        seion.add(buildRow("Hàng A", "a, i, u, e, o", List.of("a", "i", "u", "e", "o"), charMap));
        seion.add(buildRow("Hàng Ka", "ka, ki, ku, ke, ko", List.of("ka", "ki", "ku", "ke", "ko"), charMap));
        seion.add(buildRow("Hàng Sa", "sa, shi, su, se, so", List.of("sa", "shi", "su", "se", "so"), charMap));
        seion.add(buildRow("Hàng Ta", "ta, chi, tsu, te, to", List.of("ta", "chi", "tsu", "te", "to"), charMap));
        seion.add(buildRow("Hàng Na", "na, ni, nu, ne, no", List.of("na", "ni", "nu", "ne", "no"), charMap));
        seion.add(buildRow("Hàng Ha", "ha, hi, fu, he, ho", List.of("ha", "hi", "fu", "he", "ho"), charMap));
        seion.add(buildRow("Hàng Ma", "ma, mi, mu, me, mo", List.of("ma", "mi", "mu", "me", "mo"), charMap));
        seion.add(buildRow("Hàng Ya", "ya, yu, yo", List.of("ya", "yu", "yo"), charMap));
        seion.add(buildRow("Hàng Ra", "ra, ri, ru, re, ro", List.of("ra", "ri", "ru", "re", "ro"), charMap));
        seion.add(buildRow("Hàng Wa", "wa, wo", List.of("wa", "wo"), charMap));
        seion.add(buildRow("Hàng N", "n", List.of("n"), charMap));

        // Định nghĩa 5 hàng Âm đục & Bán đục (Dakuon / Handakuon)
        List<KanaRowGroupResponse> dakuon = new ArrayList<>();
        dakuon.add(buildRow("Hàng Ga", "ga, gi, gu, ge, go", List.of("ga", "gi", "gu", "ge", "go"), charMap));
        dakuon.add(buildRow("Hàng Za", "za, ji, zu, ze, zo", List.of("za", "ji", "zu", "ze", "zo"), charMap));
        dakuon.add(buildRow("Hàng Da", "da, ji, zu, de, do", List.of("da", "ji", "zu", "de", "do"), charMap));
        dakuon.add(buildRow("Hàng Ba", "ba, bi, bu, be, bo", List.of("ba", "bi", "bu", "be", "bo"), charMap));
        dakuon.add(buildRow("Hàng Pa", "pa, pi, pu, pe, po", List.of("pa", "pi", "pu", "pe", "po"), charMap));

        // Định nghĩa các hàng Âm ghép (Yōon)
        List<KanaRowGroupResponse> yoon = new ArrayList<>();
        yoon.add(buildRow("Hàng Kya", "kya, kyu, kyo", List.of("kya", "kyu", "kyo"), charMap));
        yoon.add(buildRow("Hàng Sha", "sha, shu, sho", List.of("sha", "shu", "sho"), charMap));
        yoon.add(buildRow("Hàng Cha", "cha, chu, cho", List.of("cha", "chu", "cho"), charMap));
        yoon.add(buildRow("Hàng Nya", "nya, nyu, nyo", List.of("nya", "nyu", "nyo"), charMap));
        yoon.add(buildRow("Hàng Hya", "hya, hyu, hyo", List.of("hya", "hyu", "hyo"), charMap));
        yoon.add(buildRow("Hàng Mya", "mya, myu, myo", List.of("mya", "myu", "myo"), charMap));
        yoon.add(buildRow("Hàng Rya", "rya, ryu, ryo", List.of("rya", "ryu", "ryo"), charMap));
        yoon.add(buildRow("Hàng Gya", "gya, gyu, gyo", List.of("gya", "gyu", "gyo"), charMap));
        yoon.add(buildRow("Hàng Ja", "ja, ju, jo", List.of("ja", "ju", "jo"), charMap));
        yoon.add(buildRow("Hàng Bya", "bya, byu, byo", List.of("bya", "byu", "byo"), charMap));
        yoon.add(buildRow("Hàng Pya", "pya, pyu, pyo", List.of("pya", "pyu", "pyo"), charMap));

        return KanaMatrixResponse.builder()
                .charType(targetType)
                .seion(seion)
                .dakuon(dakuon)
                .yoon(yoon)
                .build();
    }

    private KanaRowGroupResponse buildRow(String rowName, String rowRomaji, List<String> romajis, Map<String, KanaCharacterResponse> charMap) {
        List<KanaCharacterResponse> chars = new ArrayList<>();
        for (String r : romajis) {
            KanaCharacterResponse item = charMap.get(r.toLowerCase());
            if (item != null) {
                chars.add(item);
            }
        }
        return KanaRowGroupResponse.builder()
                .rowName(rowName)
                .rowRomaji(rowRomaji)
                .characters(chars)
                .build();
    }

    @Override
    public KanaCharacterResponse getKanaDetail(String character) {
        WritingCharacter wc = writingCharacterRepository.findByCharacterAndDeletedFalse(character)
                .orElseThrow(() -> new AppException(ErrorCode.CHARACTER_NOT_FOUND));

        List<CharacterStrokeResponse> strokes = wc.getStrokes().stream()
                .filter(s -> !s.isDeleted())
                .sorted(Comparator.comparing(CharacterStroke::getStrokeNumber))
                .map(dictionaryMapper::toStrokeResponse)
                .toList();

        KanaCharacterResponse resp = dictionaryMapper.toKanaCharacterResponse(wc);
        resp.setStrokes(strokes);
        return resp;
    }

    @Override
    public List<VocabularySummaryResponse> quickLookup(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        List<Vocabulary> exactMatches = vocabularyRepository.findExactMatches(text.trim());
        return dictionaryMapper.toVocabSummaryList(exactMatches);
    }

    /**
     * Bóc tách tất cả các ký tự Chữ Hán (Kanji) đơn lẻ xuất hiện trong chuỗi văn bản.
     * Ví dụ: "記憶" -> ["記", "憶"], "食べる" -> ["食"], "日本語" -> ["日", "本", "語"]
     */
    private List<String> extractKanjiCharacters(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        List<String> kanjis = new ArrayList<>();
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                String ch = new String(Character.toChars(codePoint));
                if (!kanjis.contains(ch)) {
                    kanjis.add(ch);
                }
            }
            i += Character.charCount(codePoint);
        }
        return kanjis;
    }

    /**
     * Tìm kiếm Chữ Hán thông minh:
     * 1. Bóc tách và ưu tiên hiển thị các chữ Hán cấu thành từ vựng (ví dụ: 記憶 -> 記, 憶; 食べる -> 食).
     * 2. Kết hợp kết quả tìm kiếm theo âm Hán Việt, Romaji, Nghĩa.
     * 3. Hỗ trợ tách từ nếu nhập chuỗi âm Hán Việt nhiều âm (ví dụ: "kí ức" -> 記, 憶).
     */
    private List<WritingCharacter> getSmartKanjiResults(String cleanKeyword, JlptLevel level, Pageable pageable) {
        List<String> kanjiChars = extractKanjiCharacters(cleanKeyword);
        List<WritingCharacter> result = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        // 1. Nếu từ khoá chứa chữ Hán (như 記憶, 日本語, 食べる)
        if (!kanjiChars.isEmpty()) {
            List<WritingCharacter> found = writingCharacterRepository.findByCharacterInAndDeletedFalse(kanjiChars);
            Map<String, WritingCharacter> map = found.stream()
                    .filter(w -> w.getCharType() == CharacterType.KANJI && w.isActive())
                    .filter(w -> level == null || w.getJlptLevel() == level)
                    .collect(Collectors.toMap(WritingCharacter::getCharacter, w -> w, (a, b) -> a));

            for (String kc : kanjiChars) {
                WritingCharacter wc = map.get(kc);
                if (wc != null && seenIds.add(wc.getId())) {
                    result.add(wc);
                }
            }
        }

        // 2. Tìm kiếm thông thường theo từ khoá nguyên bản
        Page<WritingCharacter> standardPage = writingCharacterRepository.searchKanjiWithFilter(cleanKeyword, level, pageable);
        for (WritingCharacter w : standardPage.getContent()) {
            if (seenIds.add(w.getId())) {
                result.add(w);
            }
        }

        // 3. Nếu chưa có kết quả và từ khoá gồm nhiều từ Hán Việt cách nhau bởi khoảng trắng (ví dụ "kí ức", "ký ức")
        if (result.isEmpty() && cleanKeyword.contains(" ")) {
            String[] tokens = cleanKeyword.split("\\s+");
            for (String token : tokens) {
                if (token.isBlank()) continue;
                Page<WritingCharacter> tokenPage = writingCharacterRepository.searchKanjiWithFilter(token, level, PageRequest.of(0, 2));
                for (WritingCharacter w : tokenPage.getContent()) {
                    if (seenIds.add(w.getId())) {
                        result.add(w);
                    }
                }
            }
        }

        return result;
    }

    private int getJlptRank(JlptLevel level) {
        if (level == null) return 99;
        return switch (level) {
            case N5 -> 1;
            case N4 -> 2;
            case N3 -> 3;
            case N2 -> 4;
            case N1 -> 5;
        };
    }
}
