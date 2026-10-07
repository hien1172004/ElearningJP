package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.GrammarPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GrammarPointRepository extends JpaRepository<GrammarPoint, Long> {
    Optional<GrammarPoint> findByIdAndDeletedFalse(Long id);
}
