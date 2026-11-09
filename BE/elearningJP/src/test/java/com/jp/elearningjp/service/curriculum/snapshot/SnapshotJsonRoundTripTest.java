package com.jp.elearningjp.service.curriculum.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonKanjiItemResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonOutlineResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSpeakingResponse;
import com.jp.elearningjp.dto.response.exam.ExamSummaryResponse;
import com.jp.elearningjp.dto.response.exam.ReviewOptionResponse;
import com.jp.elearningjp.dto.response.exam.ReviewQuestionResponse;
import com.jp.elearningjp.dto.response.exam.TakingOptionResponse;
import com.jp.elearningjp.dto.response.exam.TakingQuestionResponse;
import com.jp.elearningjp.service.exam.snapshot.ExamSnapshot;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.enums.QuizQuestionType;
import com.jp.elearningjp.shared.enums.SessionType;
import com.jp.elearningjp.shared.enums.SkillType;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Snapshot lưu bằng JSON: ghi rồi đọc lại phải giữ nguyên nội dung (kể cả trường boolean
 * kiểu {@code hasQuiz}, JsonNode, enum, BigDecimal) — nếu không học viên sẽ thấy sai nội dung.
 */
class SnapshotJsonRoundTripTest {

    /** Cấu hình gần với ObjectMapper của Spring Boot. */
    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json().build();
    /** Đọc qua đúng đường của service (cache cây JSON → object). Cache tắt để mỗi lần đều parse lại. */
    private final SnapshotJsonCache cache = new SnapshotJsonCache(mapper, 0);

    private <T> T parse(String json, Class<T> type) {
        return cache.get("k", type, () -> Optional.of(json)).orElseThrow();
    }

    @Test
    void lessonSnapshot_roundTrip() throws Exception {
        LessonSectionResponse section = LessonSectionResponse.builder()
                .id(5L).version(2L).sectionType(LessonSectionType.KANJI).title("Chữ Hán").orderIndex(1)
                .kanjiItems(List.of(LessonKanjiItemResponse.builder().id(50L).character("日").meaningVi("ngày").build()))
                .build();
        LessonSnapshot snapshot = LessonSnapshot.builder()
                .detail(LessonDetailResponse.builder().id(10L).courseId(1L).title("Bài 1").orderIndex(1)
                        .hasQuiz(true).hasSpeaking(true).sections(List.of(section)).build())
                .sections(new ArrayList<>(List.of(section)))
                .quiz(QuizSnapshot.builder().quizId(500L).passingScorePercent(80)
                        .questions(new ArrayList<>(List.of(QuizSnapshot.Question.builder()
                                .id(1L).questionType(QuizQuestionType.MATCHING)
                                .matchingData(mapper.readTree("{\"left\":[\"a\"],\"right\":[\"b\"]}"))
                                .options(new ArrayList<>(List.of(QuizSnapshot.Option.builder()
                                        .id(11L).optionKey((short) 1).optionText("b").correct(true).matchKey("L1=R1").build())))
                                .build())))
                        .build())
                .speaking(new ArrayList<>(List.of(LessonSpeakingResponse.builder()
                        .id(3L).lessonId(10L).scenarioId(4L).minTurns(3)
                        .tasks(mapper.readTree("[{\"id\":1,\"text\":\"Chào\"}]")).build())))
                .build();

        LessonSnapshot parsed = parse(mapper.writeValueAsString(snapshot), LessonSnapshot.class);

        assertThat(parsed).isEqualTo(snapshot);
        assertThat(parsed.getDetail().isHasQuiz()).isTrue();
        assertThat(parsed.getQuiz().getQuestions().get(0).getOptions().get(0).isCorrect()).isTrue();
    }

    @Test
    void courseSnapshot_roundTrip_ignoresUnknownFields() throws Exception {
        CourseSnapshot snapshot = CourseSnapshot.builder()
                .lessons(new ArrayList<>(List.of(LessonOutlineResponse.builder()
                        .id(10L).title("Bài 1").orderIndex(1).hasQuiz(true).build())))
                .build();
        String json = mapper.writeValueAsString(snapshot).replaceFirst("\\{", "{\"fieldFromOldVersion\":1,");

        assertThat(parse(json, CourseSnapshot.class)).isEqualTo(snapshot);
    }

    @Test
    void examSnapshot_roundTrip() throws Exception {
        ExamSnapshot snapshot = ExamSnapshot.builder()
                .summary(ExamSummaryResponse.builder().id(9L).title("Đề N5").totalDurationMinutes(105).published(true).build())
                .sections(new ArrayList<>(List.of(ExamSnapshot.Section.builder()
                        .id(1L).sessionType(SessionType.LISTENING).orderIndex(1).durationMinutes(30).scoreGroupId(2L)
                        .questions(new ArrayList<>(List.of(ExamSnapshot.SectionQuestion.builder()
                                .questionId(100L).questionNumber(1).points(new BigDecimal("1.00")).skillType(SkillType.LISTENING)
                                .taking(TakingQuestionResponse.builder().id(100L).hasVisualOptions(true)
                                        .options(List.of(TakingOptionResponse.builder().id(1000L).optionKey((short) 1).build())).build())
                                .review(ReviewQuestionResponse.builder().id(100L).explanationVi("vì...")
                                        .options(List.of(ReviewOptionResponse.builder().id(1000L).correct(true).build())).build())
                                .optionIds(Set.of(1000L, 1001L))
                                .correctOptionIds(Set.of(1000L))
                                .build())))
                        .build())))
                .build();

        String json = mapper.writeValueAsString(snapshot);
        ExamSnapshot parsed = parse(json, ExamSnapshot.class);

        assertThat(parsed).isEqualTo(snapshot);
        assertThat(parsed.getSections().get(0).getQuestions().get(0).getTaking().isHasVisualOptions()).isTrue();
        // Bản làm bài không chứa đáp án
        assertThat(mapper.writeValueAsString(parsed.getSections().get(0).getQuestions().get(0).getTaking()))
                .doesNotContain("correct");
    }
}
