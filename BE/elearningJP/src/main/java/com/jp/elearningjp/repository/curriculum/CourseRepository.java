package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long>, JpaSpecificationExecutor<Course> {

    Page<Course> findByPublishedTrueAndDeletedFalse(Pageable pageable);

    Page<Course> findByJlptLevelAndPublishedTrueAndDeletedFalse(JlptLevel level, Pageable pageable);

    Page<Course> findByDeletedFalse(Pageable pageable);

    Optional<Course> findByIdAndDeletedFalse(Long id);

    Optional<Course> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlugAndDeletedFalse(String slug);
}
