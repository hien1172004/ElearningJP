package com.jp.elearningjp.mapper;

import com.jp.elearningjp.dto.request.curriculum.LessonCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonOutlineResponse;
import com.jp.elearningjp.entity.curriculum.Lesson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    Lesson toLessonEntity(LessonCreateRequest request);

    @Mapping(target = "hasQuiz", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "bestScore", ignore = true)
    LessonOutlineResponse toLessonOutlineResponse(Lesson lesson);

    @Mapping(source = "course.id", target = "courseId")
    @Mapping(source = "course.title", target = "courseTitle")
    @Mapping(target = "hasQuiz", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "bestScore", ignore = true)
    @Mapping(target = "lastAccessedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    LessonDetailResponse toLessonDetailResponse(Lesson lesson);

    void updateLessonFromRequest(LessonUpdateRequest request, @MappingTarget Lesson lesson);
}
