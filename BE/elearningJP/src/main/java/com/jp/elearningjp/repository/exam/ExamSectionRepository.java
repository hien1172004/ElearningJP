package com.jp.elearningjp.repository.exam;

import com.jp.elearningjp.entity.exam.ExamSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamSectionRepository extends JpaRepository<ExamSection, Long> {

    Optional<ExamSection> findByIdAndDeletedFalse(Long id);

    List<ExamSection> findByExamIdAndDeletedFalseOrderByOrderIndexAsc(Long examId);

    @Query("SELECT s FROM ExamSection s " +
           "LEFT JOIN FETCH s.questions sq " +
           "LEFT JOIN FETCH sq.question q " +
           "WHERE s.id = :sectionId AND s.deleted = false " +
           "ORDER BY sq.questionNumber ASC")
    Optional<ExamSection> findByIdWithQuestions(@Param("sectionId") Long sectionId);
}
