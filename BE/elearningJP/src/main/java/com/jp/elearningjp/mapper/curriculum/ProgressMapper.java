package com.jp.elearningjp.mapper.curriculum;

import com.jp.elearningjp.dto.response.curriculum.ProgressResponse;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * SAU REFACTOR (V14): bỏ trường {@code lessonType} trên {@link ProgressResponse}.
 * Việc xác định lesson có quiz được dựa vào sự tồn tại row trong bảng
 * {@code lesson_quizzes} (xem {@code QuizRepository.existsByLessonIdAndDeletedFalse}),
 * không còn dựa vào section TEST hay {@code lesson.lessonType}.
 */
@Mapper(componentModel = "spring")
public interface ProgressMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "lessonId", source = "lesson.id")
    @Mapping(target = "lessonTitle", source = "lesson", qualifiedByName = "lessonTitle")
    @Mapping(target = "lessonOrderIndex", source = "lesson", qualifiedByName = "lessonOrderIndex")
    ProgressResponse toResponse(UserLessonProgress progress);

    // ============================================================
    // QUALIFIED METHODS
    // ============================================================

    @Named("lessonTitle")
    default String mapLessonTitle(Lesson lesson) {
        return lesson == null ? null : lesson.getTitle();
    }

    @Named("lessonOrderIndex")
    default Integer mapLessonOrderIndex(Lesson lesson) {
        return lesson == null ? null : lesson.getOrderIndex();
    }
}
