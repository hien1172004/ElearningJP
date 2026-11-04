package com.jp.elearningjp.repository.exam;

import com.jp.elearningjp.entity.exam.ExamAttempt;
import com.jp.elearningjp.shared.enums.AttemptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    // =========================================================
    // THỐNG KÊ CHO DASHBOARD ADMIN (bài đã nộp = SUBMITTED hoặc TIMED_OUT)
    // =========================================================

    /**
     * 1 dòng: [số bài nộp trong [from,to), số bài nộp kỳ trước [prevFrom,from),
     * số bài bắt đầu trong [from,to) nhưng vẫn IN_PROGRESS].
     */
    @Query(value = """
            SELECT COUNT(*) FILTER (WHERE a.status IN ('SUBMITTED','TIMED_OUT')
                                      AND a.submitted_at >= :from AND a.submitted_at < :to),
                   COUNT(*) FILTER (WHERE a.status IN ('SUBMITTED','TIMED_OUT')
                                      AND a.submitted_at >= :prevFrom AND a.submitted_at < :from),
                   COUNT(*) FILTER (WHERE a.status = 'IN_PROGRESS'
                                      AND a.started_at >= :from AND a.started_at < :to)
            FROM exam_attempts a
            JOIN users u ON u.id = a.user_id AND u.deleted = false
            WHERE a.deleted = false
            """, nativeQuery = true)
    List<Object[]> aggregateAttemptOverview(java.time.Instant prevFrom, java.time.Instant from, java.time.Instant to);

    /**
     * Theo cấp độ JLPT, bài nộp trong [from,to):
     * [jlpt_level, số bài nộp, số bài có kết quả đỗ/trượt, số bài đỗ, điểm scaled trung bình, số bài có điểm].
     */
    @Query(value = """
            SELECT e.jlpt_level,
                   COUNT(*),
                   COUNT(a.is_passed),
                   COUNT(*) FILTER (WHERE a.is_passed),
                   AVG(a.total_scaled_score),
                   COUNT(a.total_scaled_score)
            FROM exam_attempts a
            JOIN exams e ON e.id = a.exam_id
            JOIN users u ON u.id = a.user_id AND u.deleted = false
            WHERE a.deleted = false
              AND a.status IN ('SUBMITTED','TIMED_OUT')
              AND a.submitted_at >= :from AND a.submitted_at < :to
            GROUP BY e.jlpt_level
            """, nativeQuery = true)
    List<Object[]> aggregateAttemptsByLevel(java.time.Instant from, java.time.Instant to);
}
