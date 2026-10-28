package com.jp.elearningjp.repository.comment;

import com.jp.elearningjp.entity.comment.CommentReplyDislike;
import com.jp.elearningjp.entity.comment.CommentReplyDislikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReplyDislikeRepository extends JpaRepository<CommentReplyDislike, CommentReplyDislikeId> {

    boolean existsByIdUserIdAndIdReplyId(Long userId, Long replyId);

    long deleteByIdUserIdAndIdReplyId(Long userId, Long replyId);

    long countByIdReplyId(Long replyId);
}
