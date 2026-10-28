package com.jp.elearningjp.repository.comment;

import com.jp.elearningjp.entity.comment.CommentDislike;
import com.jp.elearningjp.entity.comment.CommentDislikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentDislikeRepository extends JpaRepository<CommentDislike, CommentDislikeId> {

    boolean existsByIdUserIdAndIdCommentId(Long userId, Long commentId);

    long deleteByIdUserIdAndIdCommentId(Long userId, Long commentId);

    long countByIdCommentId(Long commentId);
}
