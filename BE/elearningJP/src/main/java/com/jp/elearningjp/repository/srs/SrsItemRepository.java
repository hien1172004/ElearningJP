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
}
