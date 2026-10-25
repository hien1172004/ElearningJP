package com.jp.elearningjp.repository.speaking;

import com.jp.elearningjp.entity.speaking.AiScenario;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiScenarioRepository extends JpaRepository<AiScenario, Long> {

    List<AiScenario> findByTopicId(String topicId);

    List<AiScenario> findByJlptLevel(JlptLevel jlptLevel);

    List<AiScenario> findByTopicIdAndJlptLevel(String topicId, JlptLevel jlptLevel);

    List<AiScenario> findAllByOrderByCreatedAtDesc();
}
