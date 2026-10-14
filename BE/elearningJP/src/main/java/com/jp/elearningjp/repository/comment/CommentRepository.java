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

    /**
     * Danh sách comment của 1 target — sắp xếp mới nhất trước.
     */
    @Query("""
            SELECT c FROM Comment c
            LEFT JOIN FETCH c.user
            WHERE c.targetType = :targetType
              AND c.targetId = :targetId
              AND c.deleted = false
            ORDER BY c.createdAt DESC
            """)
    Page<Comment> findByTarget(
            @Param("targetType") CommentTargetType targetType,
            @Param("targetId") Long targetId,
            Pageable pageable);

    long countByTargetTypeAndTargetIdAndDeletedFalse(CommentTargetType targetType, Long targetId);

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
}
