package com.jp.elearningjp.repository.comment;

import com.jp.elearningjp.entity.comment.Comment;
import com.jp.elearningjp.shared.enums.CommentTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Lấy 1 comment chưa bị xoá (kèm user để hiển thị avatar / tên).
     */
    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.user
            WHERE c.id = :id AND c.deleted = false
            """)
    Optional<Comment> findByIdAndDeletedFalse(@Param("id") Long id);

    /** Comment chưa xoá và đang hiển thị (không bị ẩn HIDDEN). */
    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.user
            WHERE c.id = :id AND c.deleted = false
              AND c.status = com.jp.elearningjp.shared.enums.ContentStatus.VISIBLE
            """)
    Optional<Comment> findVisibleById(@Param("id") Long id);

    /**
     * Danh sách comment của 1 target — sắp xếp mới nhất trước.
     */
    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.user
            WHERE c.targetType = :targetType
              AND c.targetId = :targetId
              AND c.deleted = false
              AND c.status = com.jp.elearningjp.shared.enums.ContentStatus.VISIBLE
            ORDER BY c.createdAt DESC
            """)
    Page<Comment> findByTarget(
            @Param("targetType") CommentTargetType targetType,
            @Param("targetId") Long targetId,
            Pageable pageable);

    long countByTargetTypeAndTargetIdAndDeletedFalse(CommentTargetType targetType, Long targetId);

    /**
     * Số comment đang hiển thị của target được tạo SAU {@code createdAt} — chính là thứ tự
     * (0-based) của comment đó trong {@link #findByTarget} (mới nhất trước).
     */
    @Query("""
            SELECT COUNT(c) FROM Comment c
            WHERE c.targetType = :targetType
              AND c.targetId = :targetId
              AND c.deleted = false
              AND c.status = com.jp.elearningjp.shared.enums.ContentStatus.VISIBLE
              AND c.createdAt > :createdAt
            """)
    long countVisibleNewerThan(
            @Param("targetType") CommentTargetType targetType,
            @Param("targetId") Long targetId,
            @Param("createdAt") Instant createdAt);

    /**
     * Tăng like_count của comment lên 1 (gọi khi insert comment_like).
     */
    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    /**
     * Giảm like_count của comment xuống 1 (gọi khi xoá comment_like).
     */
    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id AND c.likeCount > 0")
    int decrementLikeCount(@Param("id") Long id);

    /**
     * Tăng/giảm dislike_count tương tự like_count.
     */
    @Modifying
    @Query("UPDATE Comment c SET c.dislikeCount = c.dislikeCount + 1 WHERE c.id = :id")
    int incrementDislikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.dislikeCount = c.dislikeCount - 1 WHERE c.id = :id AND c.dislikeCount > 0")
    int decrementDislikeCount(@Param("id") Long id);

    /**
     * Tăng/giảm reply_count khi thêm / xoá reply.
     */
    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount + 1 WHERE c.id = :id")
    int incrementReplyCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.replyCount = c.replyCount - 1 WHERE c.id = :id AND c.replyCount > 0")
    int decrementReplyCount(@Param("id") Long id);

    /** Giảm reply_count đi {@code amount} (xoá reply kèm reply con), không xuống dưới 0. */
    @Modifying
    @Query("""
            UPDATE Comment c
            SET c.replyCount = CASE WHEN c.replyCount > :amount THEN c.replyCount - :amount ELSE 0 END
            WHERE c.id = :id
            """)
    int decrementReplyCountBy(@Param("id") Long id, @Param("amount") int amount);
}
