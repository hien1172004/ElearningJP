package com.jp.elearningjp.repository.srs;

import com.jp.elearningjp.entity.srs.SrsItem;
import com.jp.elearningjp.shared.enums.SrsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Quy ước: thẻ {@code ARCHIVED} không tham gia hàng ôn, thống kê, nhắc ôn, {@code saved-ids}.
 * Thẻ {@code NEW} được lấy riêng để áp hạn mức thẻ mới mỗi ngày.
 */
@Repository
public interface SrsItemRepository extends JpaRepository<SrsItem, Long> {

    /** Thẻ ôn đến hạn (không gồm NEW và ARCHIVED). */
    @Query("SELECT s FROM SrsItem s " +
           "LEFT JOIN FETCH s.vocab " +
           "LEFT JOIN FETCH s.character " +
           "LEFT JOIN FETCH s.grammar " +
           "WHERE s.user.id = :userId " +
           "  AND s.nextReviewAt <= :now " +
           "  AND s.deleted = false " +
           "  AND s.status NOT IN (com.jp.elearningjp.shared.enums.SrsStatus.NEW, " +
           "                       com.jp.elearningjp.shared.enums.SrsStatus.ARCHIVED) " +
           "ORDER BY s.nextReviewAt ASC")
    List<SrsItem> findDueCards(
            @Param("userId") Long userId,
            @Param("now") Instant now,
            Pageable pageable
    );

    /** Thẻ mới chờ học, thẻ thêm trước học trước. */
    @Query("SELECT s FROM SrsItem s " +
           "LEFT JOIN FETCH s.vocab " +
           "LEFT JOIN FETCH s.character " +
           "LEFT JOIN FETCH s.grammar " +
           "WHERE s.user.id = :userId " +
           "  AND s.deleted = false " +
           "  AND s.status = com.jp.elearningjp.shared.enums.SrsStatus.NEW " +
           "ORDER BY s.id ASC")
    List<SrsItem> findNewCards(@Param("userId") Long userId, Pageable pageable);

    /** Đếm thẻ ôn đến hạn (không gồm NEW và ARCHIVED). */
    @Query("SELECT COUNT(s) FROM SrsItem s " +
           "WHERE s.user.id = :userId " +
           "  AND s.nextReviewAt <= :now " +
           "  AND s.deleted = false " +
           "  AND s.status NOT IN (com.jp.elearningjp.shared.enums.SrsStatus.NEW, " +
           "                       com.jp.elearningjp.shared.enums.SrsStatus.ARCHIVED)")
    long countDueCards(@Param("userId") Long userId, @Param("now") Instant now);

    /** Số thẻ đã được học lần đầu kể từ {@code since} (để trừ vào hạn mức thẻ mới trong ngày). */
    @Query("SELECT COUNT(s) FROM SrsItem s " +
           "WHERE s.user.id = :userId " +
           "  AND s.deleted = false " +
           "  AND s.introducedAt >= :since")
    long countIntroducedSince(@Param("userId") Long userId, @Param("since") Instant since);

    /** Tổng số thẻ đang hoạt động (không gồm ARCHIVED). */
    @Query("SELECT COUNT(s) FROM SrsItem s " +
           "WHERE s.user.id = :userId " +
           "  AND s.deleted = false " +
           "  AND s.status <> com.jp.elearningjp.shared.enums.SrsStatus.ARCHIVED")
    long countActiveItems(@Param("userId") Long userId);

    long countByUserIdAndStatusAndDeletedFalse(Long userId, SrsStatus status);

    Optional<SrsItem> findByUserIdAndVocabIdAndDeletedFalse(Long userId, Long vocabId);

    Optional<SrsItem> findByUserIdAndCharacterIdAndDeletedFalse(Long userId, Long characterId);

    Optional<SrsItem> findByUserIdAndGrammarIdAndDeletedFalse(Long userId, Long grammarId);

    Optional<SrsItem> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    /**
     * Đếm số thẻ đến hạn, gom theo user — cho job nhắc SRS hằng ngày.
     * Gồm cả thẻ NEW (người dùng có thẻ mới chờ học cũng nên được nhắc), không gồm ARCHIVED.
     *
     * @return mỗi phần tử là {@code Object[]{userId (Long), dueCount (Long)}}
     */
    @Query("SELECT s.user.id, COUNT(s) FROM SrsItem s " +
           "WHERE s.nextReviewAt <= :now " +
           "  AND s.deleted = false " +
           "  AND s.status <> com.jp.elearningjp.shared.enums.SrsStatus.ARCHIVED " +
           "GROUP BY s.user.id")
    List<Object[]> countDueCardsGroupByUser(@Param("now") Instant now);

    /**
     * Liệt kê thẻ SRS của user (phân trang).
     * {@code status = null} → mọi thẻ đang hoạt động (không gồm ARCHIVED);
     * {@code status = ARCHIVED} → danh sách thẻ đã lưu trữ.
     */
    @Query("SELECT s FROM SrsItem s " +
           "LEFT JOIN FETCH s.vocab " +
           "LEFT JOIN FETCH s.character " +
           "LEFT JOIN FETCH s.grammar " +
           "WHERE s.user.id = :userId " +
           "  AND s.deleted = false " +
           "  AND ((:status IS NULL AND s.status <> com.jp.elearningjp.shared.enums.SrsStatus.ARCHIVED) " +
           "       OR s.status = :status) " +
           "ORDER BY s.nextReviewAt ASC, s.id ASC")
    Page<SrsItem> findByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") SrsStatus status,
            Pageable pageable);

    /**
     * Tự động lưu trữ thẻ quá hạn ôn trước {@code cutoff}.
     * Không đụng thẻ NEW (thẻ mới chờ do hạn mức ngày không phải lỗi của người học).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SrsItem s SET s.status = com.jp.elearningjp.shared.enums.SrsStatus.ARCHIVED " +
           "WHERE s.deleted = false " +
           "  AND s.nextReviewAt < :cutoff " +
           "  AND s.status NOT IN (com.jp.elearningjp.shared.enums.SrsStatus.NEW, " +
           "                       com.jp.elearningjp.shared.enums.SrsStatus.ARCHIVED)")
    int archiveOverdue(@Param("cutoff") Instant cutoff);

    @Query("SELECT s.vocab.id FROM SrsItem s WHERE s.user.id = :userId AND s.vocab IS NOT NULL " +
           "AND s.deleted = false AND s.status <> com.jp.elearningjp.shared.enums.SrsStatus.ARCHIVED")
    Set<Long> findSavedVocabIds(@Param("userId") Long userId);

    @Query("SELECT s.character.id FROM SrsItem s WHERE s.user.id = :userId AND s.character IS NOT NULL " +
           "AND s.deleted = false AND s.status <> com.jp.elearningjp.shared.enums.SrsStatus.ARCHIVED")
    Set<Long> findSavedCharacterIds(@Param("userId") Long userId);

    @Query("SELECT s.grammar.id FROM SrsItem s WHERE s.user.id = :userId AND s.grammar IS NOT NULL " +
           "AND s.deleted = false AND s.status <> com.jp.elearningjp.shared.enums.SrsStatus.ARCHIVED")
    Set<Long> findSavedGrammarIds(@Param("userId") Long userId);
}
