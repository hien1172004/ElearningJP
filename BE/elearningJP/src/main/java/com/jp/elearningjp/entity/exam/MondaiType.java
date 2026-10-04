package com.jp.elearningjp.entity.exam;

import com.jp.elearningjp.shared.enums.SkillType;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

@Entity
@Table(name = "mondai_types")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class MondaiType extends SoftDeletableEntity {

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_type", nullable = false, length = 30)
    private SkillType skillType;

    @Column(name = "name_vi", nullable = false, length = 150)
    private String nameVi;

    @Column(name = "instruction_jp", columnDefinition = "text")
    private String instructionJp;

    @Column(name = "instruction_vi", columnDefinition = "text")
    private String instructionVi;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

}
