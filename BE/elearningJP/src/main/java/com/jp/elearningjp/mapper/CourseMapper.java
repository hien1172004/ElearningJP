package com.jp.elearningjp.mapper;

import com.jp.elearningjp.dto.request.curriculum.CourseCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.CourseUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseDetailResponse;
import com.jp.elearningjp.dto.response.curriculum.CourseResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    Course toCourseEntity(CourseCreateRequest request);

    CourseResponse toCourseResponse(Course course);

    CourseDetailResponse toCourseDetailResponse(Course course);

    void updateCourseFromRequest(CourseUpdateRequest request, @MappingTarget Course course);
}
