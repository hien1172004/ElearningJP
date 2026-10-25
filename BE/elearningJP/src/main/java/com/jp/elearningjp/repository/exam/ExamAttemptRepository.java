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

    long countByExamIdAndDeletedFalse(Long examId);

    long countByExamIdAndStatusAndDeletedFalse(Long examId, AttemptStatus status);

    java.util.List<ExamAttempt> findByExamIdAndDeletedFalse(Long examId);

    @Query("SELECT a FROM ExamAttempt a " +
           "JOIN FETCH a.exam e " +
           "LEFT JOIN FETCH a.section " +
           "WHERE a.id = :id AND a.deleted = false")
    Optional<ExamAttempt> findByIdWithExamAndSection(@Param("id") Long id);

    @Query("SELECT a FROM ExamAttempt a " +
           "JOIN FETCH a.user u " +
           "JOIN FETCH a.exam e " +
           "WHERE a.status = :status AND a.deleted = false " +
           "AND e.jlptLevel = :level " +
           "AND a.submittedAt >= :since " +
           "ORDER BY a.totalScaledScore DESC, a.timeSpentSeconds ASC, a.submittedAt ASC")
    java.util.List<ExamAttempt> findLeaderboardAttemptsWithSince(
            @Param("status") AttemptStatus status,
            @Param("level") com.jp.elearningjp.shared.enums.JlptLevel level,
            @Param("since") java.time.Instant since);

    @Query("SELECT a FROM ExamAttempt a " +
           "JOIN FETCH a.user u " +
           "JOIN FETCH a.exam e " +
           "WHERE a.status = :status AND a.deleted = false " +
           "AND e.jlptLevel = :level " +
           "ORDER BY a.totalScaledScore DESC, a.timeSpentSeconds ASC, a.submittedAt ASC")
    java.util.List<ExamAttempt> findLeaderboardAttemptsAllTime(
            @Param("status") AttemptStatus status,
            @Param("level") com.jp.elearningjp.shared.enums.JlptLevel level);
}
