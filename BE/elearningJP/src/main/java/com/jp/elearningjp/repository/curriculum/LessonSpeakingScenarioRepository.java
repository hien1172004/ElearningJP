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

    /** Trong các lesson cho trước, lấy id những lesson có luyện nói trong nháp đang dùng của phía {@code kind}. */
    @Query("SELECT DISTINCT lss.lesson.id FROM LessonSpeakingScenario lss WHERE lss.lesson.id IN :lessonIds "
            + "AND lss.draft.kind = :kind AND lss.draft.status <> com.jp.elearningjp.shared.enums.DraftStatus.ARCHIVED")
    List<Long> findLessonIdsWithSpeaking(@Param("lessonIds") List<Long> lessonIds,
                                         @Param("kind") com.jp.elearningjp.shared.enums.DraftKind kind);

    @Query("SELECT lss FROM LessonSpeakingScenario lss JOIN FETCH lss.scenario WHERE lss.draft.id = :draftId ORDER BY lss.orderIndex ASC")
    List<LessonSpeakingScenario> findByDraftIdWithScenariosOrderByOrderIndexAsc(@Param("draftId") Long draftId);

    List<LessonSpeakingScenario> findByDraftIdOrderByOrderIndexAsc(Long draftId);

    Optional<LessonSpeakingScenario> findByDraftIdAndScenarioId(Long draftId, Long scenarioId);

    Optional<LessonSpeakingScenario> findByIdAndDraftId(Long id, Long draftId);

    boolean existsByDraftId(Long draftId);

    boolean existsByDraftIdAndScenarioId(Long draftId, Long scenarioId);

    void deleteByDraftId(Long draftId);

    void deleteByDraftIdAndScenarioId(Long draftId, Long scenarioId);

    long countByDraftId(Long draftId);
}
