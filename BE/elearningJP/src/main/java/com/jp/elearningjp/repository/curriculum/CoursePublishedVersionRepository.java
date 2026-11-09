package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.CoursePublishedVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CoursePublishedVersionRepository extends JpaRepository<CoursePublishedVersion, Long> {

    @Query("SELECT COALESCE(MAX(v.versionNo), 0) FROM CoursePublishedVersion v WHERE v.courseId = :courseId")
    int findMaxVersionNo(@Param("courseId") Long courseId);
}
