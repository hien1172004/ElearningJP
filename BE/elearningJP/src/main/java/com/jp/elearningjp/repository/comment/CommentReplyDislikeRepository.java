package com.jp.elearningjp.repository.comment;

import java.util.List;
import java.util.Collection;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import com.jp.elearningjp.entity.comment.CommentReplyDislike;
import com.jp.elearningjp.entity.comment.CommentReplyDislikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReplyDislikeRepository extends JpaRepository<CommentReplyDislike, CommentReplyDislikeId> {

    boolean existsByIdUserIdAndIdReplyId(Long userId, Long replyId);

    long deleteByIdUserIdAndIdReplyId(Long userId, Long replyId);

    /** Thêm reaction; trùng khoá chính (bấm 2 lần đồng thời) thì bỏ qua. @return 1 nếu vừa thêm, 0 nếu đã có */
    @Modifying
    @Query(value = "INSERT INTO comment_reply_dislikes (user_id, reply_id) VALUES (:userId, :targetId) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /** Gỡ reaction. @return số dòng đã xoá (0 nếu chưa có) */
    @Modifying
    @Query("DELETE FROM CommentReplyDislike d WHERE d.id.userId = :userId AND d.id.replyId = :targetId")
    int deleteReaction(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /** Trong các id cho trước, lấy những id mà user đã reaction — 1 query cho cả trang. */
    @Query("SELECT d.id.replyId FROM CommentReplyDislike d WHERE d.id.userId = :userId AND d.id.replyId IN :targetIds")
    List<Long> findReactedIds(@Param("userId") Long userId, @Param("targetIds") Collection<Long> targetIds);

    long countByIdReplyId(Long replyId);
}
