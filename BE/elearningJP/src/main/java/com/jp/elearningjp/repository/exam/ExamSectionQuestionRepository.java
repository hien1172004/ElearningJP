package com.jp.elearningjp.repository.exam;

import com.jp.elearningjp.entity.exam.ExamSectionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamSectionQuestionRepository extends JpaRepository<ExamSectionQuestion, Long> {

    List<ExamSectionQuestion> findBySectionIdAndDeletedFalseOrderByQuestionNumberAsc(Long sectionId);

    List<ExamSectionQuestion> findByExamIdAndDeletedFalseOrderByQuestionNumberAsc(Long examId);

    @Query("SELECT sq FROM ExamSectionQuestion sq " +
           "JOIN FETCH sq.question q " +
           "LEFT JOIN FETCH q.options " +
           "LEFT JOIN FETCH q.passage " +
           "LEFT JOIN FETCH q.mondaiType " +
           "WHERE sq.section.id = :sectionId AND sq.deleted = false " +
           "ORDER BY sq.questionNumber ASC")
    List<ExamSectionQuestion> findBySectionIdWithQuestionDetails(@Param("sectionId") Long sectionId);

    @Query("SELECT sq FROM ExamSectionQuestion sq " +
           "JOIN FETCH sq.question q " +
           "LEFT JOIN FETCH q.options " +
           "LEFT JOIN FETCH q.passage " +
           "LEFT JOIN FETCH q.mondaiType " +
           "WHERE sq.exam.id = :examId AND sq.deleted = false " +
           "ORDER BY sq.questionNumber ASC")
    List<ExamSectionQuestion> findByExamIdWithQuestionDetails(@Param("examId") Long examId);
}
