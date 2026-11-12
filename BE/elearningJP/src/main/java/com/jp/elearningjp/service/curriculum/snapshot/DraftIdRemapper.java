package com.jp.elearningjp.service.curriculum.snapshot;

import com.jp.elearningjp.dto.response.curriculum.LessonGrammarExampleResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonGrammarItemResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonKanjiItemResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonRelatedVocabResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSpeakingResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonVocabularyExampleResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonVocabularyItemResponse;
import com.jp.elearningjp.entity.curriculum.LessonGrammarExample;
import com.jp.elearningjp.entity.curriculum.LessonGrammarItem;
import com.jp.elearningjp.entity.curriculum.LessonKanjiItem;
import com.jp.elearningjp.entity.curriculum.LessonKanjiRelatedVocab;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.LessonSpeakingScenario;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyExample;
import com.jp.elearningjp.entity.curriculum.LessonVocabularyItem;
import com.jp.elearningjp.entity.curriculum.Quiz;
import com.jp.elearningjp.entity.curriculum.QuizOption;
import com.jp.elearningjp.entity.curriculum.QuizQuestion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Đổi id dòng của bản nháp sang id ổn định ({@code COALESCE(origin_id, id)}) trong bản chụp dựng từ nháp (V65).
 * Nhờ vậy id học viên thấy (section / item / câu hỏi / đáp án…) không đổi dù bài được xuất bản từ nháp phía nào,
 * và so sánh nháp ↔ live theo id vẫn đúng.
 */
final class DraftIdRemapper {

    private final Map<Long, Long> sections = new HashMap<>();
    private final Map<Long, Long> kanjiItems = new HashMap<>();
    private final Map<Long, Long> relatedVocabs = new HashMap<>();
    private final Map<Long, Long> vocabItems = new HashMap<>();
    private final Map<Long, Long> vocabExamples = new HashMap<>();
    private final Map<Long, Long> grammarItems = new HashMap<>();
    private final Map<Long, Long> grammarExamples = new HashMap<>();
    private final Map<Long, Long> questions = new HashMap<>();
    private final Map<Long, Long> options = new HashMap<>();
    private final Map<Long, Long> speaking = new HashMap<>();
    private Long quizId;
    private Long quizOrigin;

    private DraftIdRemapper() {
    }

    /** Thu thập id → id ổn định từ cây entity của nháp (đã nạp trong cùng giao dịch). */
    static DraftIdRemapper of(List<LessonSection> sectionEntities,
                              Map<Long, List<LessonKanjiRelatedVocab>> teacherRelatedByKanjiItem,
                              Quiz quiz, List<LessonSpeakingScenario> speakingEntities) {
        DraftIdRemapper m = new DraftIdRemapper();
        for (LessonSection s : nonNull(sectionEntities)) {
            put(m.sections, s.getId(), s.getOriginId());
            for (LessonKanjiItem k : nonNull(s.getKanjiItems())) {
                put(m.kanjiItems, k.getId(), k.getOriginId());
                for (LessonKanjiRelatedVocab rv : nonNull(teacherRelatedByKanjiItem.get(k.getId()))) {
                    put(m.relatedVocabs, rv.getId(), rv.getOriginId());
                }
            }
            for (LessonVocabularyItem v : nonNull(s.getVocabularyItems())) {
                put(m.vocabItems, v.getId(), v.getOriginId());
                for (LessonVocabularyExample e : nonNull(v.getExamples())) {
                    put(m.vocabExamples, e.getId(), e.getOriginId());
                }
            }
            for (LessonGrammarItem g : nonNull(s.getGrammarItems())) {
                put(m.grammarItems, g.getId(), g.getOriginId());
                for (LessonGrammarExample e : nonNull(g.getExamples())) {
                    put(m.grammarExamples, e.getId(), e.getOriginId());
                }
            }
        }
        if (quiz != null) {
            m.quizId = quiz.getId();
            m.quizOrigin = quiz.getOriginId();
            for (QuizQuestion q : nonNull(quiz.getQuestions())) {
                put(m.questions, q.getId(), q.getOriginId());
                for (QuizOption o : nonNull(q.getOptions())) {
                    put(m.options, o.getId(), o.getOriginId());
                }
            }
        }
        for (LessonSpeakingScenario lss : nonNull(speakingEntities)) {
            put(m.speaking, lss.getId(), lss.getOriginId());
        }
        return m;
    }

    /** Ghi đè id trong bản chụp (tại chỗ). */
    void apply(LessonSnapshot snapshot) {
        if (snapshot.getDetail() != null) {
            remapSections(snapshot.getDetail().getSections());
        }
        remapSections(snapshot.getSections());
        QuizSnapshot quiz = snapshot.getQuiz();
        if (quiz != null) {
            if (quiz.getQuizId() != null && quiz.getQuizId().equals(quizId) && quizOrigin != null) {
                quiz.setQuizId(quizOrigin);
            }
            for (QuizSnapshot.Question q : nonNull(quiz.getQuestions())) {
                q.setId(map(questions, q.getId()));
                for (QuizSnapshot.Option o : nonNull(q.getOptions())) {
                    o.setId(map(options, o.getId()));
                }
            }
        }
        for (LessonSpeakingResponse sp : nonNull(snapshot.getSpeaking())) {
            sp.setId(map(speaking, sp.getId()));
        }
    }

    private void remapSections(List<LessonSectionResponse> list) {
        for (LessonSectionResponse s : nonNull(list)) {
            s.setId(map(sections, s.getId()));
            for (LessonKanjiItemResponse k : nonNull(s.getKanjiItems())) {
                k.setId(map(kanjiItems, k.getId()));
                // Từ liên quan tự lấy từ điển có id null → giữ nguyên
                for (LessonRelatedVocabResponse rv : nonNull(k.getRelatedVocabulary())) {
                    rv.setId(map(relatedVocabs, rv.getId()));
                }
            }
            for (LessonVocabularyItemResponse v : nonNull(s.getVocabularyItems())) {
                v.setId(map(vocabItems, v.getId()));
                for (LessonVocabularyExampleResponse e : nonNull(v.getExamples())) {
                    e.setId(map(vocabExamples, e.getId()));
                }
            }
            for (LessonGrammarItemResponse g : nonNull(s.getGrammarItems())) {
                g.setId(map(grammarItems, g.getId()));
                for (LessonGrammarExampleResponse e : nonNull(g.getExamples())) {
                    e.setId(map(grammarExamples, e.getId()));
                }
            }
        }
    }

    private static Long map(Map<Long, Long> m, Long id) {
        return id == null ? null : m.getOrDefault(id, id);
    }

    private static void put(Map<Long, Long> m, Long id, Long origin) {
        if (id != null && origin != null) {
            m.put(id, origin);
        }
    }

    private static <T> List<T> nonNull(List<T> c) {
        return c != null ? c : List.of();
    }
}
