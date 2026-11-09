package com.jp.elearningjp.repository.exam;

import com.jp.elearningjp.entity.exam.ExamPublishedVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamPublishedVersionRepository extends JpaRepository<ExamPublishedVersion, Long> {

    @Query("SELECT COALESCE(MAX(v.versionNo), 0) FROM ExamPublishedVersion v WHERE v.examId = :examId")
    int findMaxVersionNo(@Param("examId") Long examId);
}
