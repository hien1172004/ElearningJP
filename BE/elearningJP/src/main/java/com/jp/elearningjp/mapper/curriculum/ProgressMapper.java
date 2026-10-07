package com.jp.elearningjp.mapper.curriculum;

import com.jp.elearningjp.dto.response.curriculum.ProgressResponse;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ProgressMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "lessonId", source = "lesson.id")
    @Mapping(target = "lessonTitle", source = "lesson", qualifiedByName = "lessonTitle")
    @Mapping(target = "lessonOrderIndex", source = "lesson", qualifiedByName = "lessonOrderIndex")
    @Mapping(target = "lessonType", source = "lesson", qualifiedByName = "lessonTypeName")
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

    @Named("lessonTypeName")
    default String mapLessonTypeName(Lesson lesson) {
        if (lesson == null || lesson.getLessonType() == null) return null;
        return lesson.getLessonType().name();
    }
}