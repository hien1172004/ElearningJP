package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Optional<Quiz> findByLessonIdAndDeletedFalse(Long lessonId);

    boolean existsByLessonIdAndDeletedFalse(Long lessonId);
}
