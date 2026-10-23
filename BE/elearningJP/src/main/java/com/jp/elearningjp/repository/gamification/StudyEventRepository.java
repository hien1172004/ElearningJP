package com.jp.elearningjp.repository.gamification;

import com.jp.elearningjp.entity.gamification.StudyEvent;
import com.jp.elearningjp.shared.enums.StudyEventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StudyEventRepository extends JpaRepository<StudyEvent, Long> {

    // ------------------------------------------------------------------
    // Khoi 4 — Top lookups: GROUP BY target, ORDER BY COUNT(*) DESC
    // ------------------------------------------------------------------

    @Query("""
            SELECT e.targetType, e.targetId, e.sourceRef, COUNT(e.id)
            FROM StudyEvent e
            WHERE e.user.id = :userId
              AND e.eventType = :eventType
            GROUP BY e.targetType, e.targetId, e.sourceRef
            ORDER BY COUNT(e.id) DESC
            """)
    List<Object[]> countGroupedByTarget(Long userId, StudyEventType eventType, Pageable pageable);

    /**
     * Top N đối tượng được tra nhiều nhất, <b>giới hạn trong khoảng ngày</b>.
     *
     * <p>Bổ sung {@code created_at >= :from AND created_at < :toExclusive} cho
     * query cũ ({@link #countGroupedByTarget}) vốn xem toàn bộ lịch sử — bảng top
     * phải đi kèm kỳ đã chọn ở trang Progress.</p>
     *
     * <p><b>Vì sao native query?</b> (1) Mỗi tham số chỉ xuất hiện <b>đúng 1 lần</b>
     * → thứ tự bind xác định (xem {@code UserDailyStatsRepository#aggregateTwoPeriods}
     * để biết lý do JPQL named param lặp lại gây sai); (2) {@code event_type} lọc
     * bằng <b>literal</b> {@code 'LOOKUP'} thay vì tham số để index partial
     * {@code idx_study_events_lookup} vẫn được dùng.</p>
     *
     * <p>Thứ tự {@code ORDER BY} có {@code NULLS LAST} + tiebreaker ổn định
     * ({@code target_type}, {@code target_id}, {@code source_ref}) → cùng số lần
     * tra vẫn ra cùng thứ tự, không nhảy loạn giữa các lần gọi.</p>
     *
     * @param from        mốc bắt đầu (inclusive)
     * @param toExclusive mốc kết thúc (exclusive) — exclusive để không trùng ngày
     * @param limit       số dòng tối đa (VD: 30)
     * @return list {@code [targetType, targetId, sourceRef, soLanTra]}
     */
    @Query(value = """
            SELECT e.target_type, e.target_id, e.source_ref, COUNT(*) AS cnt
            FROM study_events e
            WHERE e.user_id = :userId
              AND e.event_type = 'LOOKUP'
              AND e.created_at >= :from
              AND e.created_at < :toExclusive
            GROUP BY e.target_type, e.target_id, e.source_ref
            ORDER BY cnt DESC, e.target_type ASC, e.target_id ASC NULLS LAST, e.source_ref ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> countTopLookupsInRange(Long userId, Instant from, Instant toExclusive, int limit);

    /**
     * Top N đối tượng được tra nhiều nhất, <b>xuyên suốt toàn bộ lịch sử</b>.
     *
     * <p>Khác {@link #countGroupedByTarget} ở chỗ: thứ tự {@code ORDER BY} có
     * tiebreaker ổn định ({@code target_type}, {@code target_id}, {@code source_ref})
     * → cùng số lần tra vẫn ra cùng thứ tự, không nhảy loạn giữa các lần gọi.
     * Và có {@code LIMIT :limit} nên chỉ đọc đúng số dòng cần.</p>
     *
     * <p><b>Vì sao native query?</b> Cùng lý do với
     * {@link #countTopLookupsInRange}: mỗi tham số chỉ xuất hiện đúng 1 lần, và
     * {@code event_type} lọc bằng literal {@code 'LOOKUP'} để index partial
     * {@code idx_study_events_lookup} vẫn được dùng.</p>
     *
     * @param userId user cần thống kê
     * @param limit  số dòng tối đa (VD: 30)
     * @return list {@code [targetType, targetId, sourceRef, soLanTra]}
     */
    @Query(value = """
            SELECT e.target_type, e.target_id, e.source_ref, COUNT(*) AS cnt
            FROM study_events e
            WHERE e.user_id = :userId
              AND e.event_type = 'LOOKUP'
            GROUP BY e.target_type, e.target_id, e.source_ref
            ORDER BY cnt DESC, e.target_type ASC, e.target_id ASC NULLS LAST, e.source_ref ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> countTopLookupsAllTime(Long userId, int limit);

    @Query("""
            SELECT COUNT(e.id)
            FROM StudyEvent e
            WHERE e.user.id = :userId
              AND e.eventType = :eventType
            """)
    long countByUserAndType(Long userId, StudyEventType eventType);

    // ------------------------------------------------------------------
    // Khoi 2 — Lich thang: co ngay nao da hoat dong khong
    // ------------------------------------------------------------------

    @Query("""
            SELECT e.createdAt
            FROM StudyEvent e
            WHERE e.user.id = :userId
              AND e.eventType = :eventType
              AND e.createdAt BETWEEN :from AND :to
            ORDER BY e.createdAt ASC
            """)
    List<Instant> findEventTimestamps(Long userId, StudyEventType eventType, Instant from, Instant to);
}