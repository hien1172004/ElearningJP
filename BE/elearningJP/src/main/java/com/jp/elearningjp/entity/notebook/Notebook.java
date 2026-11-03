package com.jp.elearningjp.entity.notebook;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.persitence.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "notebooks", indexes = {
    @Index(name = "idx_notebooks_user_id", columnList = "user_id"),
    @Index(name = "idx_notebooks_group_id", columnList = "group_id"),
    @Index(name = "idx_notebooks_share_code", columnList = "share_code")
})
@SQLRestriction("deleted = false")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notebook extends SoftDeletableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private NotebookGroup group;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "cover_color", length = 20)
    private String coverColor;

    @Column(name = "cover_icon", length = 50)
    private String coverIcon;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Builder.Default
    @Column(name = "sharing_enabled", nullable = false)
    private Boolean sharingEnabled = false;

    @Column(name = "share_code", length = 10, unique = true)
    private String shareCode;

    @Column(name = "jlpt_level", length = 2)
    private String jlptLevel;

    @Builder.Default
    @Column(name = "word_count", nullable = false)
    private Integer wordCount = 0;

    @Builder.Default
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Builder.Default
    @Column(name = "view_count", nullable = false)
    private Integer viewCount = 0;

    @Builder.Default
    @Column(name = "download_count", nullable = false)
    private Integer downloadCount = 0;

    @Builder.Default
    @Column(name = "sr_enabled", nullable = false)
    private Boolean srEnabled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_notebook_id")
    private Notebook sourceNotebook;

    public boolean isPublic() {
        return Boolean.TRUE.equals(this.isPublic);
    }

    public boolean isSharingEnabled() {
        return Boolean.TRUE.equals(this.sharingEnabled);
    }
}

