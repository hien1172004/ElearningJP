package com.jp.elearningjp.repository.comment;

import com.jp.elearningjp.entity.comment.CommentReplyLike;
import com.jp.elearningjp.entity.comment.CommentReplyLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReplyLikeRepository extends JpaRepository<CommentReplyLike, CommentReplyLikeId> {

    boolean existsByIdUserIdAndIdReplyId(Long userId, Long replyId);

    long deleteByIdUserIdAndIdReplyId(Long userId, Long replyId);

    long countByIdReplyId(Long replyId);
}
