package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.QuizOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizOptionRepository extends JpaRepository<QuizOption, Long> {

    List<QuizOption> findByQuestionIdAndDeletedFalseOrderByOptionKeyAsc(Long questionId);

    Optional<QuizOption> findByQuestionIdAndCorrectTrueAndDeletedFalse(Long questionId);

    Optional<QuizOption> findByIdAndDeletedFalse(Long id);
}
