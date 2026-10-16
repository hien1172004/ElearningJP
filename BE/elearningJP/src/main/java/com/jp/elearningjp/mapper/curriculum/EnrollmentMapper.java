package com.jp.elearningjp.mapper.curriculum;

import com.jp.elearningjp.dto.response.curriculum.EnrollmentResponse;
import com.jp.elearningjp.entity.curriculum.CourseEnrollment;
import com.jp.elearningjp.entity.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    @Mapping(target = "userId", source = "enrollment.user.id")
    @Mapping(target = "userEmail", source = "enrollment.user", qualifiedByName = "userToEmail")
    @Mapping(target = "userFullName", source = "enrollment.user", qualifiedByName = "userToFullName")
    @Mapping(target = "courseId", source = "enrollment.course.id")
    @Mapping(target = "courseTitle", source = "enrollment.course.title")
    @Mapping(target = "courseSlug", source = "enrollment.course.slug")
    @Mapping(target = "courseJlptLevel", source = "enrollment.course.jlptLevel", qualifiedByName = "jlptLevelName")
    @Mapping(target = "progressPercent", source = "progressPercent")
    EnrollmentResponse toResponse(CourseEnrollment enrollment, Integer progressPercent);

    // QUALIFIED METHODS

    @Named("userToEmail")
    default String mapUserToEmail(User user) {
        return user == null ? null : user.getEmail();
    }

    @Named("userToFullName")
    default String mapUserToFullName(User user) {
        if (user == null) return null;
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return user.getEmail();
    }

    @Named("jlptLevelName")
    default String mapJlptLevelName(com.jp.elearningjp.shared.enums.JlptLevel level) {
        return level == null ? null : level.name();
    }
}