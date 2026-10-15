package com.jp.elearningjp.repository.exam;

import com.jp.elearningjp.entity.exam.ExamAttempt;
import com.jp.elearningjp.shared.enums.AttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    Optional<ExamAttempt> findByIdAndDeletedFalse(Long id);

    Optional<ExamAttempt> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    Page<ExamAttempt> findByUserIdAndDeletedFalseOrderByStartedAtDesc(Long userId, Pageable pageable);

    Optional<ExamAttempt> findFirstByExamIdAndUserIdAndStatusAndDeletedFalse(
            Long examId, Long userId, AttemptStatus status);

    @Query("SELECT a FROM ExamAttempt a " +
           "JOIN FETCH a.exam e " +
           "LEFT JOIN FETCH a.section " +
           "WHERE a.id = :id AND a.deleted = false")
    Optional<ExamAttempt> findByIdWithExamAndSection(@Param("id") Long id);
}
