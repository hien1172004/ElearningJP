package com.jp.elearningjp.repository.srs;

import com.jp.elearningjp.entity.srs.SrsReviewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SrsReviewLogRepository extends JpaRepository<SrsReviewLog, Long> {
    List<SrsReviewLog> findBySrsItemIdOrderByReviewedAtDesc(Long srsItemId);
}
