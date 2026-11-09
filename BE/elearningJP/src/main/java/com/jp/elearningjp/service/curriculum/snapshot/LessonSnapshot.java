package com.jp.elearningjp.service.curriculum.snapshot;

import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSpeakingResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Nội dung JSON của {@code course_published_lessons.content}: 1 bài học đã duyệt.
 * Không chứa dữ liệu theo người dùng (tiến độ, phiên luyện nói được ghép lúc đọc).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonSnapshot {

    @Builder.Default
    private int schemaVersion = 1;

    /** Như {@code GET /lessons/{id}}; các trường tiến độ và luyện nói để trống. */
    private LessonDetailResponse detail;

    /** Như {@code GET /lessons/{id}/sections} (kèm từ liên quan của chữ Hán). */
    @Builder.Default
    private List<LessonSectionResponse> sections = new ArrayList<>();

    /** Quiz kèm đáp án để chấm điểm; null = bài không có quiz. */
    private QuizSnapshot quiz;

    /** Luyện nói; activeSessionId / userTurnsCount / completed để trống. */
    @Builder.Default
    private List<LessonSpeakingResponse> speaking = new ArrayList<>();
}
