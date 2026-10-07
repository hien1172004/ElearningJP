package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.WritingCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WritingCharacterRepository extends JpaRepository<WritingCharacter, Long> {
    Optional<WritingCharacter> findByIdAndDeletedFalse(Long id);
}
