package com.jp.elearningjp.repository.comment;

import java.util.List;
import java.util.Collection;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import com.jp.elearningjp.entity.comment.CommentReplyLike;
import com.jp.elearningjp.entity.comment.CommentReplyLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReplyLikeRepository extends JpaRepository<CommentReplyLike, CommentReplyLikeId> {

    boolean existsByIdUserIdAndIdReplyId(Long userId, Long replyId);

    long deleteByIdUserIdAndIdReplyId(Long userId, Long replyId);

    /** Thêm reaction; trùng khoá chính (bấm 2 lần đồng thời) thì bỏ qua. @return 1 nếu vừa thêm, 0 nếu đã có */
    @Modifying
    @Query(value = "INSERT INTO comment_reply_likes (user_id, reply_id) VALUES (:userId, :targetId) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /** Gỡ reaction. @return số dòng đã xoá (0 nếu chưa có) */
    @Modifying
    @Query("DELETE FROM CommentReplyLike l WHERE l.id.userId = :userId AND l.id.replyId = :targetId")
    int deleteReaction(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /** Trong các id cho trước, lấy những id mà user đã reaction — 1 query cho cả trang. */
    @Query("SELECT l.id.replyId FROM CommentReplyLike l WHERE l.id.userId = :userId AND l.id.replyId IN :targetIds")
    List<Long> findReactedIds(@Param("userId") Long userId, @Param("targetIds") Collection<Long> targetIds);

    long countByIdReplyId(Long replyId);
}
