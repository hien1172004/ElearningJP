package com.jp.elearningjp.repository.curriculum;

import com.jp.elearningjp.entity.curriculum.LessonSpeakingScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonSpeakingScenarioRepository extends JpaRepository<LessonSpeakingScenario, Long> {

    @Query("SELECT DISTINCT lss.lesson.id FROM LessonSpeakingScenario lss WHERE lss.lesson.id IN :lessonIds")
    List<Long> findLessonIdsWithSpeaking(@Param("lessonIds") List<Long> lessonIds);

    @Query("SELECT lss FROM LessonSpeakingScenario lss JOIN FETCH lss.scenario WHERE lss.lesson.id = :lessonId ORDER BY lss.orderIndex ASC")
    List<LessonSpeakingScenario> findByLessonIdWithScenariosOrderByOrderIndexAsc(@Param("lessonId") Long lessonId);

    List<LessonSpeakingScenario> findByLessonIdOrderByOrderIndexAsc(Long lessonId);

    Optional<LessonSpeakingScenario> findByLessonIdAndScenarioId(Long lessonId, Long scenarioId);

    Optional<LessonSpeakingScenario> findByIdAndLessonId(Long id, Long lessonId);

    boolean existsByLessonId(Long lessonId);

    boolean existsByLessonIdAndScenarioId(Long lessonId, Long scenarioId);

    void deleteByLessonId(Long lessonId);

    void deleteByLessonIdAndScenarioId(Long lessonId, Long scenarioId);

    long countByLessonId(Long lessonId);
}
