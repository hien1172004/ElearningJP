package com.jp.elearningjp.repository.speaking;

import com.jp.elearningjp.entity.speaking.AiScenario;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiScenarioRepository extends JpaRepository<AiScenario, Long>, JpaSpecificationExecutor<AiScenario> {

    /** Danh sách cho trang quản lý — nạp sẵn người tạo (ManyToOne) để tránh N+1. */
    @Override
    @EntityGraph(attributePaths = "createdBy")
    Page<AiScenario> findAll(Specification<AiScenario> spec, Pageable pageable);

    List<AiScenario> findByTopicId(String topicId);

    List<AiScenario> findByJlptLevel(JlptLevel jlptLevel);

    List<AiScenario> findByTopicIdAndJlptLevel(String topicId, JlptLevel jlptLevel);

    List<AiScenario> findAllByOrderByCreatedAtDesc();

    List<AiScenario> findByTopicIdAndIsLessonOnlyFalse(String topicId);

    List<AiScenario> findByJlptLevelAndIsLessonOnlyFalse(JlptLevel jlptLevel);

    List<AiScenario> findByTopicIdAndJlptLevelAndIsLessonOnlyFalse(String topicId, JlptLevel jlptLevel);

    List<AiScenario> findAllByIsLessonOnlyFalseOrderByCreatedAtDesc();
}
