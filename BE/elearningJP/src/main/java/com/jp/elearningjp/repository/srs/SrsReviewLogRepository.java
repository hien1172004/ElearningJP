package com.jp.elearningjp.repository.srs;

import com.jp.elearningjp.entity.srs.SrsReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SrsReviewLogRepository extends JpaRepository<SrsReviewLog, Long> {
    List<SrsReviewLog> findBySrsItemIdOrderByReviewedAtDesc(Long srsItemId);

    /** Số thẻ (không trùng) user đã ôn trong khoảng [from, to) — hiển thị lịch tuần. */
    @org.springframework.data.jpa.repository.Query(
            "SELECT COUNT(DISTINCT l.srsItem.id) FROM SrsReviewLog l " +
            "WHERE l.user.id = :userId AND l.reviewedAt >= :from AND l.reviewedAt < :to")
    long countReviewedItemsBetween(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("from") java.time.Instant from,
            @org.springframework.data.repository.query.Param("to") java.time.Instant to);
}
