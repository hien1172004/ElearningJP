package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.CharacterStroke;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CharacterStrokeRepository extends JpaRepository<CharacterStroke, Long> {

    @Query("SELECT cs FROM CharacterStroke cs JOIN cs.character wc WHERE wc.character = :charStr ORDER BY cs.strokeNumber ASC")
    List<CharacterStroke> findByCharacterString(@Param("charStr") String charStr);

    /** Nét của nhiều ký tự trong 1 query (kèm ký tự để gom nhóm), sắp theo thứ tự nét. */
    @Query("SELECT cs FROM CharacterStroke cs JOIN FETCH cs.character wc WHERE wc.character IN :chars ORDER BY wc.character ASC, cs.strokeNumber ASC")
    List<CharacterStroke> findByCharacterStrings(@Param("chars") Collection<String> chars);
}
