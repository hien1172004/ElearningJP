package com.jp.elearningjp.repository.comment;

import java.util.List;
import java.util.Collection;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import com.jp.elearningjp.entity.comment.CommentLike;
import com.jp.elearningjp.entity.comment.CommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {

    boolean existsByIdUserIdAndIdCommentId(Long userId, Long commentId);

    long deleteByIdUserIdAndIdCommentId(Long userId, Long commentId);

    /** Thêm reaction; trùng khoá chính (bấm 2 lần đồng thời) thì bỏ qua. @return 1 nếu vừa thêm, 0 nếu đã có */
    @Modifying
    @Query(value = "INSERT INTO comment_likes (user_id, comment_id) VALUES (:userId, :targetId) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /** Gỡ reaction. @return số dòng đã xoá (0 nếu chưa có) */
    @Modifying
    @Query("DELETE FROM CommentLike l WHERE l.id.userId = :userId AND l.id.commentId = :targetId")
    int deleteReaction(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /** Trong các id cho trước, lấy những id mà user đã reaction — 1 query cho cả trang. */
    @Query("SELECT l.id.commentId FROM CommentLike l WHERE l.id.userId = :userId AND l.id.commentId IN :targetIds")
    List<Long> findReactedIds(@Param("userId") Long userId, @Param("targetIds") Collection<Long> targetIds);

    long countByIdCommentId(Long commentId);
}
