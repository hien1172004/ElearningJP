package com.jp.elearningjp.repository.comment;

import java.util.List;
import java.time.Instant;
import com.jp.elearningjp.entity.comment.CommentReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentReplyRepository extends JpaRepository<CommentReply, Long> {

    @Query("""
            SELECT r FROM CommentReply r
            LEFT JOIN FETCH r.user
            WHERE r.id = :id AND r.deleted = false
            """)
    Optional<CommentReply> findByIdAndDeletedFalse(@Param("id") Long id);

    /** Reply chưa xoá và đang hiển thị (không bị ẩn HIDDEN). */
    @Query("""
            SELECT r FROM CommentReply r
            LEFT JOIN FETCH r.user
            WHERE r.id = :id AND r.deleted = false
              AND r.status = com.jp.elearningjp.shared.enums.ContentStatus.VISIBLE
            """)
    Optional<CommentReply> findVisibleById(@Param("id") Long id);

    /**
     * Danh sách reply của 1 comment — sắp xếp theo thời gian tạo tăng dần.
     */
    @Query("""
            SELECT r FROM CommentReply r
            LEFT JOIN FETCH r.user
            WHERE r.comment.id = :commentId
              AND r.deleted = false
              AND r.status = com.jp.elearningjp.shared.enums.ContentStatus.VISIBLE
            ORDER BY r.createdAt ASC
            """)
    Page<CommentReply> findByCommentIdOrderByCreatedAtAsc(
            @Param("commentId") Long commentId, Pageable pageable);

    long countByCommentIdAndDeletedFalse(Long commentId);

    /**
     * Xoá mềm 1 reply cùng toàn bộ reply con cháu (đệ quy theo parent_reply_id).
     *
     * @return số reply vừa chuyển sang deleted (để trừ reply_count của comment gốc)
     */
    /** Id của reply cùng toàn bộ reply con cháu còn chưa xoá — để dọn thông báo trỏ tới chúng. */
    @Query(value = """
            WITH RECURSIVE subtree AS (
                SELECT id, deleted FROM comment_replies WHERE id = :replyId
                UNION
                SELECT r.id, r.deleted FROM comment_replies r JOIN subtree s ON r.parent_reply_id = s.id
            )
            SELECT id FROM subtree WHERE deleted = FALSE
            """, nativeQuery = true)
    List<Long> findActiveSubtreeIds(@Param("replyId") Long replyId);

    @Modifying
    @Query(value = """
            WITH RECURSIVE subtree AS (
                SELECT id FROM comment_replies WHERE id = :replyId
                UNION
                SELECT r.id FROM comment_replies r JOIN subtree s ON r.parent_reply_id = s.id
            )
            UPDATE comment_replies
            SET deleted = TRUE, deleted_at = :now, updated_at = :now
            WHERE id IN (SELECT id FROM subtree) AND deleted = FALSE
            """, nativeQuery = true)
    int softDeleteSubtree(@Param("replyId") Long replyId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE CommentReply r SET r.likeCount = r.likeCount + 1 WHERE r.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE CommentReply r SET r.likeCount = r.likeCount - 1 WHERE r.id = :id AND r.likeCount > 0")
    int decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE CommentReply r SET r.dislikeCount = r.dislikeCount + 1 WHERE r.id = :id")
    int incrementDislikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE CommentReply r SET r.dislikeCount = r.dislikeCount - 1 WHERE r.id = :id AND r.dislikeCount > 0")
    int decrementDislikeCount(@Param("id") Long id);
}
