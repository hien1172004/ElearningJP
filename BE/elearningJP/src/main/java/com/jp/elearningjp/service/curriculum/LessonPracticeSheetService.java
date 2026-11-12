package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.PracticeSheetRequest;
import com.jp.elearningjp.dto.response.curriculum.PracticeSheetCheckResponse;
import com.jp.elearningjp.dto.response.curriculum.PracticeSheetCheckResponse.MissingStrokeItem;
import com.jp.elearningjp.entity.content.CharacterStroke;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.content.WritingCharacter;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.content.CharacterStrokeRepository;
import com.jp.elearningjp.repository.curriculum.LessonKanjiItemRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.curriculum.LessonVocabularyItemRepository;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.pdf.PracticeSheetBlock;
import com.jp.elearningjp.shared.pdf.PracticeSheetGroup;
import com.jp.elearningjp.shared.pdf.PracticeSheetRenderer;
import com.jp.elearningjp.service.curriculum.snapshot.LessonSnapshot;
import com.jp.elearningjp.service.curriculum.snapshot.PublishedLessonResolver;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.JapaneseKanaUtil;
import com.jp.elearningjp.shared.util.JapaneseReadingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * In phiếu luyện viết (PDF ô chữ Mễ + thứ tự nét) cho mục Chữ Hán / Từ vựng của bài học.
 *
 * <ul>
 *   <li>Mỗi nhóm (section) của bài là 1 nhóm trong PDF, có dòng tiêu đề; thứ tự theo bài học.</li>
 *   <li>Chữ Hán: mỗi chữ 1 khối, cách đọc lấy âm On (không có thì âm Kun).</li>
 *   <li>Từ vựng: hàng thứ tự nét cho các chữ Hán trong từ; từ không có chữ Hán (toàn Kana) thì luyện từng Kana.
 *       Hàng đồ chữ mờ lặp lại cả từ.</li>
 *   <li>Ký tự chưa có dữ liệu nét vẫn in (chữ mẫu + chữ mờ) — FE gọi {@link #check} trước để hỏi người dùng.</li>
 * </ul>
 * Quyền: admin, chủ khóa học hoặc học viên đã đăng ký (giống quyền xem nội dung bài).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LessonPracticeSheetService {

    static final int MAX_ITEMS = 300;

    LessonRepository lessonRepository;
    LessonSectionRepository sectionRepository;
    LessonKanjiItemRepository kanjiItemRepository;
    LessonVocabularyItemRepository vocabItemRepository;
    CharacterStrokeRepository strokeRepository;
    CourseAccessGuard courseAccessGuard;
    PracticeSheetRenderer renderer;
    PublishedLessonResolver publishedLessonResolver;
    LessonDraftService lessonDraftService;

    /** File PDF trả về cho trình duyệt. */
    public record PracticeSheetFile(String fileName, byte[] content) {
    }

    /** Liệt kê các mục có ký tự thiếu dữ liệu nét (không tạo PDF). */
    @Transactional(readOnly = true)
    public PracticeSheetCheckResponse check(Long lessonId, PracticeSheetRequest request) {
        Collected collected = collect(lessonId, request);
        List<MissingStrokeItem> missing = new ArrayList<>();
        for (Entry e : collected.entries) {
            List<String> chars = e.block.strokeChars().stream()
                    .filter(c -> e.block.strokes().getOrDefault(c, List.of()).isEmpty())
                    .toList();
            if (!chars.isEmpty()) {
                missing.add(new MissingStrokeItem(e.itemId, e.groupTitle, e.block.word(), chars));
            }
        }
        return new PracticeSheetCheckResponse(collected.entries.size(), missing);
    }

    /** Tạo PDF phiếu luyện viết cho các mục đã chọn, nhóm theo section. */
    @Transactional(readOnly = true)
    public PracticeSheetFile render(Long lessonId, PracticeSheetRequest request) {
        Collected collected = collect(lessonId, request);
        Map<String, List<PracticeSheetBlock>> byGroup = new LinkedHashMap<>();
        for (Entry e : collected.entries) {
            byGroup.computeIfAbsent(e.groupKey, k -> new ArrayList<>()).add(e.block);
        }
        // Chỉ 1 nhóm → không cần dòng tiêu đề nhóm (header trang đã có tên bài)
        boolean showTitles = byGroup.size() > 1;
        List<PracticeSheetGroup> groups = byGroup.entrySet().stream()
                .map(en -> new PracticeSheetGroup(
                        showTitles ? collected.titles.get(en.getKey()) : null, en.getValue()))
                .toList();
        byte[] pdf = renderer.render(collected.lesson.getTitle(), groups);
        String prefix = request.sectionType() == LessonSectionType.KANJI ? "luyen_viet_chu_han" : "luyen_viet_tu_vung";
        log.info("[PRACTICE-SHEET] lesson id={} type={} items={} groups={}",
                lessonId, request.sectionType(), collected.entries.size(), groups.size());
        return new PracticeSheetFile(prefix + "_bai_" + lessonId + ".pdf", pdf);
    }

    // ============================================================
    // Gom dữ liệu
    // ============================================================

    private record Entry(Long itemId, String groupKey, String groupTitle, PracticeSheetBlock block) {
    }

    private record Collected(Lesson lesson, List<Entry> entries, Map<String, String> titles) {
    }

    /** Id các mục Chữ Hán / Từ vựng của bài trong bản xuất bản. */
    private static Set<Long> publishedItemIds(LessonSnapshot snapshot, LessonSectionType type) {
        return snapshot.getSections().stream()
                .flatMap(s -> type == LessonSectionType.KANJI
                        ? (s.getKanjiItems() == null ? java.util.stream.Stream.<Long>empty()
                                : s.getKanjiItems().stream().map(i -> i.getId()))
                        : (s.getVocabularyItems() == null ? java.util.stream.Stream.<Long>empty()
                                : s.getVocabularyItems().stream().map(i -> i.getId())))
                .collect(Collectors.toSet());
    }

    private Collected collect(Long lessonId, PracticeSheetRequest request) {
        LessonSectionType type = request.sectionType();
        if (type != LessonSectionType.KANJI && type != LessonSectionType.VOCABULARY) {
            throw new AppException(ErrorCode.PRACTICE_SHEET_TYPE_INVALID);
        }
        Set<Long> wanted = request.itemIds() == null ? Set.of()
                : request.itemIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (wanted.isEmpty()) throw new AppException(ErrorCode.PRACTICE_SHEET_NO_ITEMS);
        if (wanted.size() > MAX_ITEMS) throw new AppException(ErrorCode.PRACTICE_SHEET_TOO_MANY_ITEMS);

        Lesson lesson;
        Optional<PublishedLessonResolver.PublishedLesson> published = publishedLessonResolver.forReading(lessonId);
        if (published.isPresent()) {
            // Học viên của khóa đã xuất bản: chỉ in các mục có trong bản đã duyệt
            lesson = published.get().lesson();
            Set<Long> publishedItemIds = publishedItemIds(published.get().snapshot(), type);
            wanted = wanted.stream().filter(publishedItemIds::contains).collect(Collectors.toSet());
            if (wanted.isEmpty()) throw new AppException(ErrorCode.PRACTICE_SHEET_NO_ITEMS);
        } else {
            lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                    .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
            courseAccessGuard.requireLessonContentAccess(lesson);
        }

        // Bước 1: ký tự cần nét của từng mục đã chọn, theo thứ tự nhóm → thứ tự mục trong nhóm
        record Draft(Long itemId, LessonSection section, String word, String reading,
                     List<String> strokeChars, List<String> wordChars) {
        }
        List<Draft> drafts = new ArrayList<>();
        // Bản đã duyệt dùng id ổn định (origin_id) của phần tử; người quản lý dùng id dòng trong nháp phía mình (V65)
        boolean byOrigin = published.isPresent();
        for (LessonSection section : sectionRepository
                .findByDraftIdAndSectionTypeAndDeletedFalseOrderByOrderIndexAsc(lessonDraftService.readDraftId(lesson), type)) {
            if (type == LessonSectionType.KANJI) {
                for (LessonKanjiItem item : kanjiItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(section.getId())) {
                    WritingCharacter ch = item.getCharacter();
                    Long key = byOrigin && item.getOriginId() != null ? item.getOriginId() : item.getId();
                    if (!wanted.contains(key) || ch == null || ch.getCharacter() == null) continue;
                    String k = ch.getCharacter().trim();
                    List<String> chars = splitChars(k);
                    drafts.add(new Draft(key, section, k, kanjiReading(ch), distinct(chars), chars));
                }
            } else {
                for (LessonVocabularyItem item : vocabItemRepository.findBySectionIdAndDeletedFalseOrderByOrderIndexAsc(section.getId())) {
                    Vocabulary v = item.getVocab();
                    Long key = byOrigin && item.getOriginId() != null ? item.getOriginId() : item.getId();
                    if (!wanted.contains(key) || v == null || v.getWord() == null) continue;
                    String word = v.getWord().trim();
                    List<String> wordChars = splitChars(word);
                    List<String> kanji = PracticeSheetRenderer.extractKanji(word);
                    // Có chữ Hán → luyện nét chữ Hán; toàn Kana → luyện nét từng Kana
                    List<String> strokeChars = distinct(kanji.isEmpty() ? wordChars : kanji);
                    drafts.add(new Draft(key, section, word, vocabReading(item, v), strokeChars, wordChars));
                }
            }
        }
        if (drafts.isEmpty()) throw new AppException(ErrorCode.PRACTICE_SHEET_NO_ITEMS);

        // Bước 2: nạp nét của mọi ký tự trong 1 query
        Set<String> allChars = drafts.stream().flatMap(d -> d.strokeChars().stream()).collect(Collectors.toSet());
        Map<String, List<CharacterStroke>> strokes = strokeRepository.findByCharacterStrings(allChars).stream()
                .collect(Collectors.groupingBy(cs -> cs.getCharacter().getCharacter(), LinkedHashMap::new, Collectors.toList()));

        Map<String, String> titles = new LinkedHashMap<>();
        List<Entry> entries = new ArrayList<>();
        for (Draft d : drafts) {
            String key = String.valueOf(d.section().getId());
            titles.putIfAbsent(key, d.section().getTitle());
            Map<String, List<CharacterStroke>> own = new LinkedHashMap<>();
            d.strokeChars().forEach(c -> own.put(c, strokes.getOrDefault(c, List.of())));
            entries.add(new Entry(d.itemId(), key, d.section().getTitle(),
                    new PracticeSheetBlock(d.word(), d.reading(), d.strokeChars(), d.wordChars(), own)));
        }
        return new Collected(lesson, entries, titles);
    }

    /** Tách ký tự (theo code point), bỏ khoảng trắng và dấu câu. */
    static List<String> splitChars(String text) {
        List<String> out = new ArrayList<>();
        text.codePoints()
                .mapToObj(Character::toString)
                .filter(s -> !s.isBlank() && !JapaneseReadingService.isPunctuation(s) && !"、。「」・".contains(s))
                .forEach(out::add);
        return out;
    }

    private static List<String> distinct(List<String> chars) {
        return new ArrayList<>(new LinkedHashSet<>(chars));
    }

    /** Cách đọc chữ Hán: âm On đầu tiên, không có thì âm Kun (bỏ dấu "." và "-" của từ điển). */
    private static String kanjiReading(WritingCharacter ch) {
        String r = firstNonBlank(ch.getOnyomi());
        if (r == null) r = firstNonBlank(ch.getKunyomi());
        return r == null ? ch.getCharacter() : r.replace(".", "").replace("-", "");
    }

    /** Cách đọc từ vựng: hiragana tuỳ chỉnh trong bài, không có thì hiragana từ điển. */
    private static String vocabReading(LessonVocabularyItem item, Vocabulary v) {
        if (item.getCustomHiragana() != null && !item.getCustomHiragana().isBlank()) return item.getCustomHiragana().trim();
        if (v.getHiragana() != null && !v.getHiragana().isBlank()) return v.getHiragana().trim();
        return JapaneseKanaUtil.containsKanji(v.getWord()) ? "" : v.getWord();
    }

    private static String firstNonBlank(List<String> values) {
        if (values == null) return null;
        return values.stream().filter(s -> s != null && !s.isBlank()).findFirst().orElse(null);
    }
}
