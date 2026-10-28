package com.jp.elearningjp.repository.srs;

import com.jp.elearningjp.entity.srs.SrsItem;
import com.jp.elearningjp.shared.enums.SrsStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface SrsItemRepository extends JpaRepository<SrsItem, Long> {

    @Query("SELECT s FROM SrsItem s " +
           "LEFT JOIN FETCH s.vocab " +
           "LEFT JOIN FETCH s.character " +
           "LEFT JOIN FETCH s.grammar " +
           "WHERE s.user.id = :userId " +
           "  AND s.nextReviewAt <= :now " +
           "  AND s.deleted = false " +
           "ORDER BY s.nextReviewAt ASC")
    List<SrsItem> findDueCards(
            @Param("userId") Long userId,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("SELECT COUNT(s) FROM SrsItem s " +
           "WHERE s.user.id = :userId " +
           "  AND s.nextReviewAt <= :now " +
           "  AND s.deleted = false")
    long countDueCards(@Param("userId") Long userId, @Param("now") Instant now);

    long countByUserIdAndDeletedFalse(Long userId);

    long countByUserIdAndStatusAndDeletedFalse(Long userId, SrsStatus status);

    boolean existsByUserIdAndVocabIdAndDeletedFalse(Long userId, Long vocabId);

    boolean existsByUserIdAndCharacterIdAndDeletedFalse(Long userId, Long characterId);

    boolean existsByUserIdAndGrammarIdAndDeletedFalse(Long userId, Long grammarId);

    Optional<SrsItem> findByUserIdAndVocabIdAndDeletedFalse(Long userId, Long vocabId);

    Optional<SrsItem> findByUserIdAndCharacterIdAndDeletedFalse(Long userId, Long characterId);

    Optional<SrsItem> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    /**
     * Đếm số thẻ đến hạn, gom theo user — cho job nhắc SRS hằng ngày.
     *
     * <p><b>Vì sao 1 query thay vì N query?</b> Job cần biết mọi user có thẻ đến hạn;
     * nếu duyệt từng user rồi gọi {@code countDueCards} sẽ thành N+1. Query này trả về
     * đúng những user có nhu cầu nhắc, kèm số thẻ.</p>
     *
     * @param now mốc thời gian hiện tại
     * @return mỗi phần tử là {@code Object[]{userId (Long), dueCount (Long)}}
     */
    @Query("SELECT s.user.id, COUNT(s) FROM SrsItem s " +
           "WHERE s.nextReviewAt <= :now " +
           "  AND s.deleted = false " +
           "GROUP BY s.user.id")
    List<Object[]> countDueCardsGroupByUser(@Param("now") Instant now);
}
