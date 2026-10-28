package com.jp.elearningjp.repository.comment;

import com.jp.elearningjp.entity.comment.CommentLike;
import com.jp.elearningjp.entity.comment.CommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {

    boolean existsByIdUserIdAndIdCommentId(Long userId, Long commentId);

    long deleteByIdUserIdAndIdCommentId(Long userId, Long commentId);

    long countByIdCommentId(Long commentId);
}
