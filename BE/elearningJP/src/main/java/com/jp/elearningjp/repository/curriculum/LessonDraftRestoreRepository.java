package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.dto.response.curriculum.LessonGrammarExampleResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonGrammarItemResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonKanjiItemResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonRelatedVocabResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSpeakingResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonVocabularyExampleResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonVocabularyItemResponse;
import com.jp.elearningjp.service.curriculum.snapshot.LessonSnapshot;
import com.jp.elearningjp.service.curriculum.snapshot.QuizSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;

/**
 * Dựng nội dung 1 bản nháp từ bản live (snapshot V56) — V65: tạo nháp mới, "bỏ nháp", "cập nhật bản mới",
 * và nháp phía bên kia tự theo bản live khi nó chưa sửa gì.
 *
 * <p>Cách làm (gọi trong giao dịch của service, chung kết nối với JPA):</p>
 * <ol>
 *   <li>Xóa hẳn nội dung đang có của nháp (section → item → ví dụ / từ liên quan; quiz → câu hỏi → đáp án;
 *       kịch bản luyện nói) — khóa ngoại ON DELETE CASCADE dọn các bảng con. Bình luận duyệt trỏ section
 *       ({@code ON DELETE SET NULL}) mất liên kết section, vẫn giữ nội dung.</li>
 *   <li>Chèn lại theo snapshot; {@code origin_id} = id của phần tử trong snapshot → bản chụp lần sau ghi lại
 *       đúng id học viên đang thấy.</li>
 * </ol>
 * Chỉ dùng SQL thuần (không đụng bộ nhớ đệm entity) — service phải flush / clear persistence context quanh lời gọi.
 * Tham chiếu tới từ điển / kịch bản đã bị xóa hẳn thì bỏ qua phần tử đó (ghi log), không làm hỏng cả lần dựng.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class LessonDraftRestoreRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Thay toàn bộ nội dung của nháp {@code draftId} (thuộc bài {@code lessonId}) bằng {@code live}. */
    public void replaceContent(Long lessonId, Long draftId, LessonSnapshot live) {
        deleteContent(draftId);
        if (live == null) {
            return;
        }
        for (LessonSectionResponse section : nonNull(live.getSections())) {
            insertSection(lessonId, draftId, section);
        }
        if (live.getQuiz() != null) {
            insertQuiz(lessonId, draftId, live.getQuiz());
        }
        for (LessonSpeakingResponse speaking : nonNull(live.getSpeaking())) {
            if (speaking.getScenarioId() == null || !exists("ai_scenario", speaking.getScenarioId())) {
                log.warn("[LESSON_DRAFT] Skip speaking scenario {} of lesson id={} (scenario no longer exists)",
                        speaking.getScenarioId(), lessonId);
                continue;
            }
            jdbc.update("""
                    INSERT INTO lesson_speaking_scenarios (lesson_id, draft_id, scenario_id, order_index, min_turns, origin_id)
                    VALUES (:lessonId, :draftId, :scenarioId, :ord, :minTurns, :origin)
                    ON CONFLICT (draft_id, scenario_id) DO NOTHING
                    """, base(lessonId, draftId)
                    .addValue("scenarioId", speaking.getScenarioId())
                    .addValue("ord", speaking.getOrderIndex() != null ? speaking.getOrderIndex() : 1)
                    .addValue("minTurns", speaking.getMinTurns() != null ? speaking.getMinTurns() : 3)
                    .addValue("origin", speaking.getId(), Types.BIGINT));
        }
    }

    /** Xóa hẳn nội dung của nháp (giữ dòng nháp). */
    public void deleteContent(Long draftId) {
        MapSqlParameterSource p = new MapSqlParameterSource("draftId", draftId);
        jdbc.update("DELETE FROM lesson_sections WHERE draft_id = :draftId", p);
        jdbc.update("DELETE FROM lesson_quizzes WHERE draft_id = :draftId", p);
        jdbc.update("DELETE FROM lesson_speaking_scenarios WHERE draft_id = :draftId", p);
    }

    // =========================================================

    private void insertSection(Long lessonId, Long draftId, LessonSectionResponse s) {
        Long sectionId = insert("INSERT INTO lesson_sections (lesson_id, draft_id, section_type, title, order_index, origin_id) "
                        + "VALUES (:lessonId, :draftId, :type, :title, :ord, :origin) RETURNING id",
                base(lessonId, draftId)
                        .addValue("type", s.getSectionType() != null ? s.getSectionType().name() : null)
                        .addValue("title", s.getTitle(), Types.VARCHAR)
                        .addValue("ord", s.getOrderIndex())
                        .addValue("origin", s.getId(), Types.BIGINT));

        for (LessonKanjiItemResponse k : nonNull(s.getKanjiItems())) {
            if (!exists("writing_characters", k.getCharacterId())) {
                log.warn("[LESSON_DRAFT] Skip kanji item {} of lesson id={} (character no longer exists)", k.getId(), lessonId);
                continue;
            }
            Long itemId = insert("INSERT INTO lesson_kanji_items (section_id, character_id, order_index, note, origin_id) "
                            + "VALUES (:sectionId, :characterId, :ord, :note, :origin) RETURNING id",
                    new MapSqlParameterSource().addValue("sectionId", sectionId)
                            .addValue("characterId", k.getCharacterId()).addValue("ord", k.getOrderIndex())
                            .addValue("note", k.getNote(), Types.VARCHAR).addValue("origin", k.getId(), Types.BIGINT));
            // Chỉ danh sách giáo viên chọn có id; danh sách tự lấy từ từ điển (id null) tự có lại khi không còn dòng nào
            for (LessonRelatedVocabResponse rv : nonNull(k.getRelatedVocabulary())) {
                if (rv.getId() == null || !exists("vocabulary", rv.getVocabId())) continue;
                jdbc.update("INSERT INTO lesson_kanji_related_vocabulary (kanji_item_id, vocab_id, note, origin_id) "
                                + "VALUES (:itemId, :vocabId, :note, :origin)",
                        new MapSqlParameterSource().addValue("itemId", itemId).addValue("vocabId", rv.getVocabId())
                                .addValue("note", rv.getNote(), Types.VARCHAR).addValue("origin", rv.getId(), Types.BIGINT));
            }
        }

        for (LessonVocabularyItemResponse v : nonNull(s.getVocabularyItems())) {
            if (!exists("vocabulary", v.getVocabId())) {
                log.warn("[LESSON_DRAFT] Skip vocabulary item {} of lesson id={} (word no longer exists)", v.getId(), lessonId);
                continue;
            }
            Long itemId = insert("INSERT INTO lesson_vocabulary_items (section_id, vocab_id, order_index, custom_hiragana, "
                            + "custom_romaji, custom_audio_url, custom_meaning, image_url, note, origin_id) "
                            + "VALUES (:sectionId, :vocabId, :ord, :ch, :cr, :ca, :cm, :img, :note, :origin) RETURNING id",
                    new MapSqlParameterSource().addValue("sectionId", sectionId)
                            .addValue("vocabId", v.getVocabId()).addValue("ord", v.getOrderIndex())
                            .addValue("ch", v.getCustomHiragana(), Types.VARCHAR)
                            .addValue("cr", v.getCustomRomaji(), Types.VARCHAR)
                            .addValue("ca", v.getCustomAudioUrl(), Types.VARCHAR)
                            .addValue("cm", v.getCustomMeaning(), Types.VARCHAR)
                            .addValue("img", v.getImageUrl(), Types.VARCHAR)
                            .addValue("note", v.getNote(), Types.VARCHAR)
                            .addValue("origin", v.getId(), Types.BIGINT));
            for (LessonVocabularyExampleResponse e : nonNull(v.getExamples())) {
                jdbc.update("INSERT INTO lesson_vocabulary_examples (vocab_item_id, example_jp, example_vi, example_romaji, "
                                + "audio_url, order_index, is_primary, origin_id) "
                                + "VALUES (:itemId, :jp, :vi, :ro, :audio, :ord, :primary, :origin)",
                        new MapSqlParameterSource().addValue("itemId", itemId)
                                .addValue("jp", e.getExampleJp(), Types.VARCHAR).addValue("vi", e.getExampleVi(), Types.VARCHAR)
                                .addValue("ro", e.getExampleRomaji(), Types.VARCHAR)
                                .addValue("audio", e.getAudioUrl(), Types.VARCHAR)
                                .addValue("ord", e.getOrderIndex() != null ? e.getOrderIndex() : 1)
                                .addValue("primary", Boolean.TRUE.equals(e.getIsPrimary()))
                                .addValue("origin", e.getId(), Types.BIGINT));
            }
        }

        for (LessonGrammarItemResponse g : nonNull(s.getGrammarItems())) {
            if (!exists("grammar_points", g.getGrammarId())) {
                log.warn("[LESSON_DRAFT] Skip grammar item {} of lesson id={} (grammar no longer exists)", g.getId(), lessonId);
                continue;
            }
            Long itemId = insert("INSERT INTO lesson_grammar_items (section_id, grammar_id, order_index, custom_meaning, "
                            + "custom_structure, note, origin_id) "
                            + "VALUES (:sectionId, :grammarId, :ord, :cm, :cs, :note, :origin) RETURNING id",
                    new MapSqlParameterSource().addValue("sectionId", sectionId)
                            .addValue("grammarId", g.getGrammarId()).addValue("ord", g.getOrderIndex())
                            .addValue("cm", g.getCustomMeaning(), Types.VARCHAR)
                            .addValue("cs", g.getCustomStructure(), Types.VARCHAR)
                            .addValue("note", g.getNote(), Types.VARCHAR)
                            .addValue("origin", g.getId(), Types.BIGINT));
            for (LessonGrammarExampleResponse e : nonNull(g.getExamples())) {
                jdbc.update("INSERT INTO lesson_grammar_examples (grammar_item_id, example_jp, example_vi, order_index, origin_id) "
                                + "VALUES (:itemId, :jp, :vi, :ord, :origin)",
                        new MapSqlParameterSource().addValue("itemId", itemId)
                                .addValue("jp", e.getExampleJp(), Types.VARCHAR).addValue("vi", e.getExampleVi(), Types.VARCHAR)
                                .addValue("ord", e.getOrderIndex() != null ? e.getOrderIndex() : 1)
                                .addValue("origin", e.getId(), Types.BIGINT));
            }
        }
    }

    private void insertQuiz(Long lessonId, Long draftId, QuizSnapshot quiz) {
        Long quizId = insert("INSERT INTO lesson_quizzes (lesson_id, draft_id, title, description, passing_score_percent, origin_id) "
                        + "VALUES (:lessonId, :draftId, :title, :desc, COALESCE(:pass, 80), :origin) RETURNING id",
                base(lessonId, draftId)
                        .addValue("title", quiz.getTitle() != null ? quiz.getTitle() : "Quiz", Types.VARCHAR)
                        .addValue("desc", quiz.getDescription(), Types.VARCHAR)
                        .addValue("pass", quiz.getPassingScorePercent(), Types.INTEGER)
                        .addValue("origin", quiz.getQuizId(), Types.BIGINT));

        for (QuizSnapshot.Question q : nonNull(quiz.getQuestions())) {
            Long questionId = insert("INSERT INTO quiz_questions (quiz_id, question_text, explanation, question_type, "
                            + "correct_text_answer, matching_data, order_index, vocab_id, character_id, origin_id) "
                            + "VALUES (:quizId, :text, :expl, :type, :cta, CAST(:md AS jsonb), :ord, :vocabId, :charId, :origin) "
                            + "RETURNING id",
                    new MapSqlParameterSource().addValue("quizId", quizId)
                            .addValue("text", q.getQuestionText(), Types.VARCHAR)
                            .addValue("expl", q.getExplanation(), Types.VARCHAR)
                            .addValue("type", q.getQuestionType() != null ? q.getQuestionType().name() : "MULTIPLE_CHOICE")
                            .addValue("cta", q.getCorrectTextAnswer(), Types.VARCHAR)
                            .addValue("md", q.getMatchingData() != null && !q.getMatchingData().isNull()
                                    ? q.getMatchingData().toString() : null, Types.VARCHAR)
                            .addValue("ord", q.getOrderIndex())
                            // Từ / chữ gắn câu hỏi chỉ để tham khảo: đã bị xóa hẳn thì bỏ liên kết, giữ câu hỏi
                            .addValue("vocabId", existsOrNull("vocabulary", q.getVocabId()), Types.BIGINT)
                            .addValue("charId", existsOrNull("writing_characters", q.getCharacterId()), Types.BIGINT)
                            .addValue("origin", q.getId(), Types.BIGINT));
            for (QuizSnapshot.Option o : nonNull(q.getOptions())) {
                jdbc.update("INSERT INTO quiz_options (question_id, option_key, option_text, is_correct, match_key, origin_id) "
                                + "VALUES (:questionId, :key, :text, :correct, :mk, :origin)",
                        new MapSqlParameterSource().addValue("questionId", questionId)
                                .addValue("key", o.getOptionKey(), Types.SMALLINT)
                                .addValue("text", o.getOptionText(), Types.VARCHAR)
                                .addValue("correct", o.isCorrect())
                                .addValue("mk", o.getMatchKey(), Types.VARCHAR)
                                .addValue("origin", o.getId(), Types.BIGINT));
            }
        }
    }

    // =========================================================

    private Long insert(String sql, MapSqlParameterSource p) {
        return jdbc.queryForObject(sql, p, Long.class);
    }

    /** Dòng tham chiếu còn tồn tại (xóa mềm vẫn tính — bản live vẫn đang dùng nó). Tên bảng chỉ là hằng nội bộ. */
    private boolean exists(String table, Long id) {
        if (id == null) {
            return false;
        }
        Boolean found = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM " + table + " WHERE id = :id)",
                new MapSqlParameterSource("id", id), Boolean.class);
        return Boolean.TRUE.equals(found);
    }

    private Long existsOrNull(String table, Long id) {
        return exists(table, id) ? id : null;
    }

    private static MapSqlParameterSource base(Long lessonId, Long draftId) {
        return new MapSqlParameterSource().addValue("lessonId", lessonId).addValue("draftId", draftId);
    }

    private static <T> List<T> nonNull(List<T> list) {
        return list != null ? list : List.of();
    }
}
