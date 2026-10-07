package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonItemRepository extends JpaRepository<LessonItem, Long> {
    List<LessonItem> findByLessonIdAndDeletedFalseOrderByOrderIndexAsc(Long lessonId);
}
