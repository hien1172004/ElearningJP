package com.jp.elearningjp.repository.comment;

import com.jp.elearningjp.entity.comment.CommentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    boolean existsByReporterIdAndCommentId(Long reporterId, Long commentId);

    boolean existsByReporterIdAndReplyId(Long reporterId, Long replyId);

    /** Đóng mọi báo cáo OPEN của 1 bình luận (RESOLVED khi đã ẩn/xóa, REJECTED khi bỏ qua). */
    @Modifying
    @Query(value = """
            UPDATE comment_reports
            SET status = :status, handled_by = :handlerId, handled_at = :now, updated_at = :now
            WHERE comment_id = :commentId AND status = 'OPEN'
            """, nativeQuery = true)
    int closeOpenReportsOfComment(Long commentId, String status, Long handlerId, Instant now);

    /** Đóng mọi báo cáo OPEN của các trả lời (1 trả lời hoặc cả nhánh con khi xóa). */
    @Modifying
    @Query(value = """
            UPDATE comment_reports
            SET status = :status, handled_by = :handlerId, handled_at = :now, updated_at = :now
            WHERE reply_id IN (:replyIds) AND status = 'OPEN'
            """, nativeQuery = true)
    int closeOpenReportsOfReplies(Collection<Long> replyIds, String status, Long handlerId, Instant now);

    /**
     * Hàng chờ kiểm duyệt: gom báo cáo theo nội dung bị báo cáo, nhiều báo cáo nhất lên đầu.
     * Mỗi dòng: [comment_id, reply_id, số báo cáo, thời điểm báo cáo gần nhất].
     *
     * <p>{@code teacherId} khác null → chỉ bình luận trong bài học thuộc khóa giáo viên đó được giao.</p>
     */
    @Query(value = """
            SELECT r.comment_id, r.reply_id, COUNT(*) AS cnt, MAX(r.created_at) AS latest
            FROM comment_reports r
            LEFT JOIN comment_replies rp ON rp.id = r.reply_id
            JOIN comments c ON c.id = COALESCE(r.comment_id, rp.comment_id)
            LEFT JOIN lessons l ON c.target_type = 'LESSON' AND l.id = c.target_id
            LEFT JOIN courses co ON co.id = l.course_id
            WHERE r.status = :status
              AND (CAST(:teacherId AS BIGINT) IS NULL OR EXISTS (
                    SELECT 1 FROM course_teachers ct
                    WHERE ct.course_id = co.id AND ct.teacher_id = CAST(:teacherId AS BIGINT)))
            GROUP BY r.comment_id, r.reply_id
            ORDER BY cnt DESC, latest DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM (
                SELECT 1
                FROM comment_reports r
                LEFT JOIN comment_replies rp ON rp.id = r.reply_id
                JOIN comments c ON c.id = COALESCE(r.comment_id, rp.comment_id)
                LEFT JOIN lessons l ON c.target_type = 'LESSON' AND l.id = c.target_id
                LEFT JOIN courses co ON co.id = l.course_id
                WHERE r.status = :status
                  AND (CAST(:teacherId AS BIGINT) IS NULL OR EXISTS (
                    SELECT 1 FROM course_teachers ct
                    WHERE ct.course_id = co.id AND ct.teacher_id = CAST(:teacherId AS BIGINT)))
                GROUP BY r.comment_id, r.reply_id
            ) grouped
            """,
            nativeQuery = true)
    Page<Object[]> findReportGroups(String status, Long teacherId, Pageable pageable);

    /** Báo cáo (theo trạng thái) của các bình luận gốc — để tổng hợp lý do & ghi chú. */
    @Query("""
            SELECT r FROM CommentReport r
            WHERE r.comment.id IN :commentIds AND r.status = :status
            ORDER BY r.createdAt DESC
            """)
    List<CommentReport> findByCommentIdsAndStatus(Collection<Long> commentIds,
                                                  com.jp.elearningjp.shared.enums.ReportStatus status);

    /** Báo cáo (theo trạng thái) của các trả lời — để tổng hợp lý do & ghi chú. */
    @Query("""
            SELECT r FROM CommentReport r
            WHERE r.reply.id IN :replyIds AND r.status = :status
            ORDER BY r.createdAt DESC
            """)
    List<CommentReport> findByReplyIdsAndStatus(Collection<Long> replyIds,
                                                com.jp.elearningjp.shared.enums.ReportStatus status);

    /** Số báo cáo OPEN theo bình luận gốc: [comment_id, count]. */
    @Query("""
            SELECT r.comment.id, COUNT(r) FROM CommentReport r
            WHERE r.comment.id IN :commentIds AND r.status = com.jp.elearningjp.shared.enums.ReportStatus.OPEN
            GROUP BY r.comment.id
            """)
    List<Object[]> countOpenByCommentIds(Collection<Long> commentIds);

    /** Số báo cáo OPEN theo trả lời: [reply_id, count]. */
    @Query("""
            SELECT r.reply.id, COUNT(r) FROM CommentReport r
            WHERE r.reply.id IN :replyIds AND r.status = com.jp.elearningjp.shared.enums.ReportStatus.OPEN
            GROUP BY r.reply.id
            """)
    List<Object[]> countOpenByReplyIds(Collection<Long> replyIds);
}
