package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.CoursePublishedLesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoursePublishedLessonRepository extends JpaRepository<CoursePublishedLesson, Long> {

    Optional<CoursePublishedLesson> findByPublishedVersionIdAndLessonId(Long publishedVersionId, Long lessonId);

    List<CoursePublishedLesson> findByPublishedVersionIdOrderByOrderIndexAsc(Long publishedVersionId);
}
