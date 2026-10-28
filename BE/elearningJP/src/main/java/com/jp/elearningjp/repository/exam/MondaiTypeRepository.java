package com.jp.elearningjp.repository.exam;

import com.jp.elearningjp.entity.exam.MondaiType;
import com.jp.elearningjp.shared.enums.SkillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MondaiTypeRepository extends JpaRepository<MondaiType, Long> {

    Optional<MondaiType> findByCodeAndDeletedFalse(String code);

    List<MondaiType> findBySkillTypeAndActiveTrueAndDeletedFalse(SkillType skillType);

    List<MondaiType> findByActiveTrueAndDeletedFalse();
}
