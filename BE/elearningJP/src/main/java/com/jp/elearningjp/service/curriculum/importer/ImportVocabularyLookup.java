package com.jp.elearningjp.service.curriculum.importer;

import com.jp.elearningjp.dto.response.curriculum.importer.ImportRowResult;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.shared.util.JapaneseKanaUtil;
import com.jp.elearningjp.shared.util.JapaneseReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Tra từ vựng trong từ điển cho 1 dòng Excel (dùng chung cho import Từ vựng và Từ liên quan của Kanji),
 * đồng thời quyết định Hiragana / Romaji hiển thị trong bài theo thứ tự ưu tiên:
 * <b>giá trị trong file → dữ liệu từ điển → tự sinh</b> (Kuromoji + {@link JapaneseKanaUtil}).
 *
 * <p>Ghi vào {@code ImportRowResult.data} để FE hiển thị ở bảng xem trước:
 * {@code vocabId, dictHiragana, dictRomaji, dictMeaning, resolvedHiragana, hiraganaSource,
 * resolvedRomaji, romajiSource} — nguồn là {@code FILE | DICT | AUTO}.</p>
 */
@Component
@RequiredArgsConstructor
public class ImportVocabularyLookup {

    public static final String SOURCE_FILE = "FILE";
    public static final String SOURCE_DICT = "DICT";
    public static final String SOURCE_AUTO = "AUTO";

    private final VocabularyRepository vocabularyRepository;
    private final JapaneseReadingService readingService;

    /** Hiragana + Romaji đã chọn cho 1 từ, kèm nguồn. */
    public record Reading(String hiragana, String hiraganaSource, String romaji, String romajiSource) {
        public boolean hiraganaAuto() {
            return SOURCE_AUTO.equals(hiraganaSource);
        }

        public boolean romajiAuto() {
            return SOURCE_AUTO.equals(romajiSource);
        }
    }

    /**
     * Chuẩn hoá cách đọc giáo viên nhập (Katakana → Hiragana, bỏ khoảng trắng), ghi lại vào {@code data}
     * để mọi bước so khớp sau (vd nối câu ví dụ với từ) dùng cùng 1 giá trị.
     */
    public String normalizeHiragana(ImportRowResult r, ImportColumn column) {
        String raw = (String) r.getData().get(column.key());
        String normalized = JapaneseKanaUtil.normalizeKana(raw);
        r.getData().put(column.key(), normalized);
        return normalized;
    }

    /**
     * Tìm từ trong từ điển. Có {@code hiragana} → khớp chính xác (không có thì báo lỗi kèm các cách đọc hợp lệ).
     * Bỏ trống mà từ có nhiều cách đọc → dùng Kuromoji đoán cách đọc để chọn, không đoán được thì lấy nghĩa đầu.
     */
    public Optional<Vocabulary> find(ImportRowResult r, String word, String hiragana) {
        Optional<Vocabulary> found;
        if (hiragana != null) {
            found = vocabularyRepository.findFirstByWordAndHiraganaAndDeletedFalseOrderBySenseNoAsc(word, hiragana);
            if (found.isEmpty()) {
                String readings = readingsOf(word);
                r.error(readings != null
                        ? "Từ '" + word + "' không có cách đọc '" + hiragana + "' trong từ điển. Các cách đọc có: " + readings
                        : "Không tìm thấy từ '" + word + "' trong từ điển");
                return Optional.empty();
            }
        } else {
            found = vocabularyRepository.findFirstByWordAndDeletedFalseOrderBySenseNoAsc(word);
            if (found.isEmpty()) {
                r.error("Không tìm thấy từ '" + word + "' trong từ điển");
                return Optional.empty();
            }
            if (vocabularyRepository.countByWordAndDeletedFalse(word) > 1) {
                found = pickByGuessedReading(r, word, found.get());
            }
        }
        Vocabulary vocab = found.get();
        r.getData().put("vocabId", vocab.getId());
        r.getData().put("dictHiragana", vocab.getHiragana());
        r.getData().put("dictRomaji", vocab.getRomaji());
        r.getData().put("dictMeaning", vocab.getMeaningVi());
        return found;
    }

    /**
     * Chọn Hiragana / Romaji cho từ đã tìm được.
     *
     * @param fileHiragana cách đọc trong file (đã chuẩn hoá; nếu có thì đã khớp từ điển)
     * @param fileRomaji   giá trị cột romaji trong file ({@code -} = để trống), null nếu bỏ trống
     */
    public Reading resolveReading(ImportRowResult r, Vocabulary vocab, String fileHiragana, String fileRomaji) {
        String hiragana;
        String hiraganaSource;
        if (fileHiragana != null) {
            hiragana = fileHiragana;
            hiraganaSource = SOURCE_FILE;
        } else if (notBlank(vocab.getHiragana())) {
            hiragana = vocab.getHiragana();
            hiraganaSource = SOURCE_DICT;
        } else {
            hiragana = readingService.toHiragana(vocab.getWord());
            hiraganaSource = SOURCE_AUTO;
            r.warn("Từ điển chưa có Hiragana, đã tự sinh '" + hiragana + "'. Nên kiểm tra lại");
        }

        String romaji;
        String romajiSource;
        if (fileRomaji != null) {
            romaji = ExcelImportSupport.toOverride(fileRomaji);
            romajiSource = SOURCE_FILE;
        } else if (notBlank(vocab.getRomaji())) {
            romaji = vocab.getRomaji();
            romajiSource = SOURCE_DICT;
        } else {
            romaji = hiragana != null ? JapaneseKanaUtil.kanaToRomaji(hiragana) : null;
            romajiSource = SOURCE_AUTO;
            if (romaji != null) {
                r.warn("Từ điển chưa có Romaji, đã tự sinh '" + romaji + "' từ Hiragana");
            }
        }

        r.getData().put("resolvedHiragana", hiragana);
        r.getData().put("hiraganaSource", hiraganaSource);
        r.getData().put("resolvedRomaji", romaji);
        r.getData().put("romajiSource", romajiSource);
        return new Reading(hiragana, hiraganaSource, romaji, romajiSource);
    }

    /** Romaji cho câu ví dụ: có trong file thì dùng ({@code -} = để trống), thiếu thì Kuromoji sinh từ câu tiếng Nhật. */
    public String resolveSentenceRomaji(ImportRowResult r, String exampleJp, String fileRomaji) {
        String romaji;
        String source;
        if (fileRomaji != null) {
            romaji = ExcelImportSupport.toOverride(fileRomaji);
            source = SOURCE_FILE;
        } else {
            romaji = readingService.toRomaji(exampleJp);
            source = SOURCE_AUTO;
        }
        r.getData().put("resolvedRomaji", romaji);
        r.getData().put("romajiSource", source);
        return romaji;
    }

    private Optional<Vocabulary> pickByGuessedReading(ImportRowResult r, String word, Vocabulary first) {
        String guess = readingService.toHiragana(word);
        if (guess != null && !guess.equals(first.getHiragana())) {
            Optional<Vocabulary> guessed =
                    vocabularyRepository.findFirstByWordAndHiraganaAndDeletedFalseOrderBySenseNoAsc(word, guess);
            if (guessed.isPresent()) {
                first = guessed.get();
            }
        }
        r.warn("Từ '" + word + "' có nhiều nghĩa/cách đọc (" + readingsOf(word) + "), đã chọn '"
                + first.getHiragana() + "'. Điền cột hiragana để chọn chính xác");
        return Optional.of(first);
    }

    /** Các cách đọc (không trùng) của 1 từ trong từ điển, nối bằng dấu phẩy; null nếu từ không có. */
    private String readingsOf(String word) {
        List<String> readings = vocabularyRepository.findByWordAndDeletedFalseOrderBySenseNoAsc(word).stream()
                .map(Vocabulary::getHiragana)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return readings.isEmpty() ? null : String.join(", ", readings);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
