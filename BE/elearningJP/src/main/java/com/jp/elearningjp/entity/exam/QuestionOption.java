package com.jp.elearningjp.entity.exam;

import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;

/** Ánh xạ bảng {@code question_options}. */
@Entity
@Table(name = "question_options")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class QuestionOption extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_key", nullable = false)
    private Short optionKey;

    @Column(name = "option_text", columnDefinition = "text")
    private String optionText;

    @Column(name = "option_image_url", length = 500)
    private String optionImageUrl;

    @Builder.Default
    @Column(name = "is_correct", nullable = false)
    private boolean correct = false;

}
