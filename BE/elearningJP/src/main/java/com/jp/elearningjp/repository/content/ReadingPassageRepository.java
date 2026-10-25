package com.jp.elearningjp.repository.content;

import com.jp.elearningjp.entity.content.ReadingPassage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingPassageRepository extends JpaRepository<ReadingPassage, Long> {
}
