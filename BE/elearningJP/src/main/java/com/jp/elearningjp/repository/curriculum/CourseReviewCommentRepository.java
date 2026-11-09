package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.CourseReviewComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseReviewCommentRepository extends JpaRepository<CourseReviewComment, Long> {

    @EntityGraph(attributePaths = {"author", "resolvedBy"})
    List<CourseReviewComment> findByReviewRequestIdOrderByCreatedAtAsc(Long reviewRequestId);

    long countByReviewRequestId(Long reviewRequestId);
}
