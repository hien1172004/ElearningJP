package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSpeakingResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonVocabularyExampleResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonVocabularyItemResponse;
import com.jp.elearningjp.service.curriculum.snapshot.LessonSnapshot;
import com.jp.elearningjp.service.curriculum.snapshot.QuizSnapshot;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.enums.QuizQuestionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dựng nội dung nháp từ bản live (V65) trên Postgres thật (qua toàn bộ migration): nháp dựng lại chứa đúng nội dung
 * live với {@code origin_id} = id trong live, không đụng nháp phía bên kia, chạy lại được nhiều lần. Cần Docker.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(LessonDraftRestoreRepository.class)
class LessonDraftRestoreQueriesIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired LessonDraftRestoreRepository restoreRepository;
    @Autowired JdbcTemplate jdbc;

    private long lessonId;
    private long teacherDraftId;
    private long adminDraftId;
    private long sectionId;
    private long itemId;
    private long exampleId;
    private long quizId;
    private long questionId;
    private long optionAId;
    private long optionBId;
    private long vocabId;
    private Long scenarioId;
    private LessonSnapshot live;

    private Long insert(String sql, Object... args) {
        return jdbc.queryForObject(sql + " RETURNING id", Long.class, args);
    }

    @BeforeEach
    void seedLiveState() {
        vocabId = jdbc.queryForObject("SELECT MIN(id) FROM vocabulary", Long.class);
        long courseId = insert("INSERT INTO courses (title, slug, jlpt_level) VALUES ('C', ?, 'N5')", "dr-" + System.nanoTime());
        lessonId = insert("INSERT INTO lessons (course_id, title, order_index) VALUES (?, 'Bài', 1)", courseId);
        teacherDraftId = insert("INSERT INTO lesson_drafts (lesson_id, kind) VALUES (?, 'TEACHER')", lessonId);
        adminDraftId = insert("INSERT INTO lesson_drafts (lesson_id, kind, needs_import) VALUES (?, 'ADMIN', TRUE)", lessonId);
        // Nội dung live = nháp TEACHER vừa được duyệt (id các dòng này là id ổn định)
        sectionId = insert("INSERT INTO lesson_sections (lesson_id, draft_id, section_type, title, order_index) "
                + "VALUES (?, ?, 'VOCABULARY', 'Từ vựng', 1)", lessonId, teacherDraftId);
        itemId = insert("INSERT INTO lesson_vocabulary_items (section_id, vocab_id, order_index, custom_meaning, note) "
                + "VALUES (?, ?, 1, 'nghĩa live', 'ghi chú live')", sectionId, vocabId);
        exampleId = insert("INSERT INTO lesson_vocabulary_examples (vocab_item_id, example_jp, example_vi, order_index, is_primary) "
                + "VALUES (?, '例文', 'Ví dụ live', 1, true)", itemId);
        quizId = insert("INSERT INTO lesson_quizzes (lesson_id, draft_id, title, passing_score_percent) VALUES (?, ?, 'Quiz live', 70)",
                lessonId, teacherDraftId);
        questionId = insert("INSERT INTO quiz_questions (quiz_id, question_text, question_type, order_index) "
                + "VALUES (?, 'Câu live', 'MULTIPLE_CHOICE', 1)", quizId);
        optionAId = insert("INSERT INTO quiz_options (question_id, option_key, option_text, is_correct) VALUES (?, 1, 'Đúng', true)", questionId);
        optionBId = insert("INSERT INTO quiz_options (question_id, option_key, option_text, is_correct) VALUES (?, 2, 'Sai', false)", questionId);
        scenarioId = jdbc.queryForObject("SELECT MIN(id) FROM ai_scenario", Long.class);

        LessonDetailResponse detail = new LessonDetailResponse();
        detail.setTitle("Bài");
        LessonVocabularyItemResponse item = LessonVocabularyItemResponse.builder()
                .id(itemId).orderIndex(1).vocabId(vocabId).customMeaning("nghĩa live").note("ghi chú live")
                .examples(new ArrayList<>(List.of(LessonVocabularyExampleResponse.builder()
                        .id(exampleId).orderIndex(1).exampleJp("例文").exampleVi("Ví dụ live").isPrimary(true).build())))
                .build();
        LessonSectionResponse section = LessonSectionResponse.builder()
                .id(sectionId).sectionType(LessonSectionType.VOCABULARY).title("Từ vựng").orderIndex(1)
                .vocabularyItems(new ArrayList<>(List.of(item))).build();
        QuizSnapshot quiz = QuizSnapshot.builder().quizId(quizId).title("Quiz live").passingScorePercent(70)
                .questions(new ArrayList<>(List.of(QuizSnapshot.Question.builder()
                        .id(questionId).questionText("Câu live").orderIndex(1).questionType(QuizQuestionType.MULTIPLE_CHOICE)
                        .options(new ArrayList<>(List.of(
                                QuizSnapshot.Option.builder().id(optionAId).optionKey((short) 1).optionText("Đúng").correct(true).build(),
                                QuizSnapshot.Option.builder().id(optionBId).optionKey((short) 2).optionText("Sai").correct(false).build())))
                        .build())))
                .build();
        List<LessonSpeakingResponse> speaking = scenarioId != null
                ? List.of(LessonSpeakingResponse.builder().id(900L).scenarioId(scenarioId).orderIndex(1).minTurns(4).build())
                : List.of();
        live = LessonSnapshot.builder().detail(detail).sections(new ArrayList<>(List.of(section)))
                .quiz(quiz).speaking(new ArrayList<>(speaking)).build();
    }

    @Test
    void replaceContent_buildsAdminDraftFromLive_withOriginIds_teacherDraftUntouched() {
        restoreRepository.replaceContent(lessonId, adminDraftId, live);

        Map<String, Object> section = jdbc.queryForMap(
                "SELECT * FROM lesson_sections WHERE draft_id = ? AND deleted = FALSE", adminDraftId);
        assertThat(section.get("id")).isNotEqualTo(sectionId);
        assertThat(section.get("origin_id")).isEqualTo(sectionId);
        assertThat(section.get("lesson_id")).isEqualTo(lessonId);

        Map<String, Object> item = jdbc.queryForMap("SELECT * FROM lesson_vocabulary_items WHERE section_id = ?", section.get("id"));
        assertThat(item.get("origin_id")).isEqualTo(itemId);
        assertThat(item.get("custom_meaning")).isEqualTo("nghĩa live");
        assertThat(jdbc.queryForObject("SELECT origin_id FROM lesson_vocabulary_examples WHERE vocab_item_id = ?",
                Long.class, item.get("id"))).isEqualTo(exampleId);

        Map<String, Object> quiz = jdbc.queryForMap("SELECT * FROM lesson_quizzes WHERE draft_id = ?", adminDraftId);
        assertThat(quiz.get("origin_id")).isEqualTo(quizId);
        assertThat(quiz.get("passing_score_percent")).isEqualTo(70);
        assertThat(jdbc.queryForList("SELECT o.origin_id FROM quiz_options o JOIN quiz_questions q ON q.id = o.question_id "
                + "WHERE q.quiz_id = ? ORDER BY o.option_key", Long.class, quiz.get("id")))
                .containsExactly(optionAId, optionBId);
        if (scenarioId != null) {
            assertThat(jdbc.queryForObject("SELECT min_turns FROM lesson_speaking_scenarios WHERE draft_id = ?",
                    Integer.class, adminDraftId)).isEqualTo(4);
        }

        // Nháp TEACHER (giáo viên) không bị đụng tới
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lesson_sections WHERE draft_id = ?", Integer.class, teacherDraftId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT custom_meaning FROM lesson_vocabulary_items WHERE id = ?", String.class, itemId))
                .isEqualTo("nghĩa live");
    }

    @Test
    void replaceContent_twice_replacesInsteadOfDuplicating_unchangedLiveWithoutQuizRemovesQuiz() {
        restoreRepository.replaceContent(lessonId, adminDraftId, live);
        // ADMIN sửa nháp của mình rồi bị dựng lại từ live (bỏ nháp / cập nhật bản mới)
        jdbc.update("UPDATE lesson_vocabulary_items SET custom_meaning = 'nghĩa nháp' WHERE section_id IN "
                + "(SELECT id FROM lesson_sections WHERE draft_id = ?)", adminDraftId);
        live.setQuiz(null);

        restoreRepository.replaceContent(lessonId, adminDraftId, live);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lesson_sections WHERE draft_id = ?", Integer.class, adminDraftId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT vi.custom_meaning FROM lesson_vocabulary_items vi "
                + "JOIN lesson_sections s ON s.id = vi.section_id WHERE s.draft_id = ?", String.class, adminDraftId))
                .isEqualTo("nghĩa live");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lesson_quizzes WHERE draft_id = ?", Integer.class, adminDraftId))
                .isZero();
        // Quiz của nháp TEACHER vẫn còn (unique theo nháp, không theo bài)
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lesson_quizzes WHERE lesson_id = ?", Integer.class, lessonId))
                .isEqualTo(1);
    }

    @Test
    void replaceContent_referenceDeletedFromDictionary_skipsOnlyThatItem() {
        LessonVocabularyItemResponse ghost = LessonVocabularyItemResponse.builder()
                .id(-5L).orderIndex(2).vocabId(-999L).build();
        live.getSections().get(0).getVocabularyItems().add(ghost);

        restoreRepository.replaceContent(lessonId, adminDraftId, live);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lesson_vocabulary_items vi "
                + "JOIN lesson_sections s ON s.id = vi.section_id WHERE s.draft_id = ?", Integer.class, adminDraftId))
                .isEqualTo(1);
    }
}
