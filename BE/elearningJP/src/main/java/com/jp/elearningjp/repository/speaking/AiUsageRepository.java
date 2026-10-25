package com.jp.elearningjp.repository.speaking;

import com.jp.elearningjp.entity.speaking.AiUsage;
import com.jp.elearningjp.entity.speaking.AiUsageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AiUsageRepository extends JpaRepository<AiUsage, AiUsageId> {

    Optional<AiUsage> findByIdUserIdAndIdUsageDate(Long userId, LocalDate usageDate);

    @Modifying
    @Query(value = """
        INSERT INTO ai_usage (user_id, usage_date, tokens_used)
        VALUES (:userId, :usageDate, :tokens)
        ON CONFLICT (user_id, usage_date)
        DO UPDATE SET tokens_used = ai_usage.tokens_used + EXCLUDED.tokens_used
        """, nativeQuery = true)
    int recordUsage(@Param("userId") Long userId, @Param("usageDate") LocalDate usageDate, @Param("tokens") int tokens);
}
