package com.jp.elearningjp.repository.comment;

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

    /**
     * Danh sách reply của 1 comment — sắp xếp theo thời gian tạo tăng dần.
     */
    @Query("""
            SELECT r FROM CommentReply r
            LEFT JOIN FETCH r.user
            WHERE r.comment.id = :commentId
              AND r.deleted = false
            ORDER BY r.createdAt ASC
            """)
    Page<CommentReply> findByCommentIdOrderByCreatedAtAsc(
            @Param("commentId") Long commentId, Pageable pageable);

    long countByCommentIdAndDeletedFalse(Long commentId);

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
