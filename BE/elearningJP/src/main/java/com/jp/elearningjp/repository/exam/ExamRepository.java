package com.jp.elearningjp.repository.exam;

import com.jp.elearningjp.entity.exam.Exam;
import com.jp.elearningjp.shared.enums.ExamType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findByIdAndDeletedFalse(Long id);

    Page<Exam> findByPublishedTrueAndDeletedFalse(Pageable pageable);

    Page<Exam> findByJlptLevelAndPublishedTrueAndDeletedFalse(JlptLevel jlptLevel, Pageable pageable);

    Page<Exam> findByJlptLevelAndExamTypeAndPublishedTrueAndDeletedFalse(
            JlptLevel jlptLevel, ExamType examType, Pageable pageable);

    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.scoreGroups WHERE e.id = :id AND e.deleted = false")
    Optional<Exam> findByIdWithScoreGroups(@Param("id") Long id);

    @Query("SELECT DISTINCT e FROM Exam e " +
           "LEFT JOIN FETCH e.sections s " +
           "LEFT JOIN FETCH e.scoreGroups " +
           "WHERE e.id = :id AND e.deleted = false")
    Optional<Exam> findByIdWithSectionsAndScoreGroups(@Param("id") Long id);
}
