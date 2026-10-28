package com.jp.elearningjp.entity.gamification;

import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.shared.enums.LookupTargetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;

import java.time.Instant;

/**
 * Muc da nguoi dung da HOC (lan dau tien) — bang chong trung cho "từ đã học".
 *
 * <p><b>Vai tro:</b> phan biet 2 khái niem trong trang Progress:</p>
 * <ul>
 *   <li><b>"từ đã học"</b> = số MỤC MỚI (moi muc chi tinh 1 lan)</li>
 *   <li><b>"số lần tra từ"</b> = tổng số lượt tra (kể cả tra lai tu cu)</li>
 * </ul>
 *
 * <p><b>Co duong chay:</b> moi muc da hoc co DUNG MOT row trong bang nay.
 * Ràng buoc {@code UNIQUE (user_id, target_type, target_key)} + lệnh
 * {@code INSERT ... ON CONFLICT DO NOTHING} giúp viec ghi lai "muc nay da hoc
 * chua" la ATOMIC — khong can read-then-write (tranh race khi user mo cung
 * 1 tu o nhieu tab).</p>
 *
 * <p><b>Khong dung {@code SoftDeletableEntity}</b> vi bang nay la append-only
 * (muc da hoc thi khong bao gio bi xoa mem).</p>
 */
@Entity
@Table(name = "user_learned_items")
@DynamicInsert
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLearnedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Loai doi tuong da hoc: VOCAB | KANJI | GRAMMAR. */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private LookupTargetType targetType;

    /**
     * Khoa danh tinh chong trung — duy nhat trong (user, targetType).
     *
     * <ul>
     *   <li>VOCAB / GRAMMAR → {@code "id:<targetId>"}</li>
     *   <li>KANJI → {@code "ref:<sourceRef>"} (ky tu Han)</li>
     * </ul>
     */
    @Column(name = "target_key", nullable = false, length = 160)
    private String targetKey;

    /**
     * ID doi tuong da hoc.
     *
     * <ul>
     *   <li>VOCAB → {@code vocabulary.id}</li>
     *   <li>GRAMMAR → {@code grammar_points.id}</li>
     *   <li>KANJI → {@code null} (chu Han la chuoi, khong phai numeric id)</li>
     * </ul>
     */
    @Column(name = "target_id")
    private Long targetId;

    /** Chuoi tham chieu (ky tu Han) — NULL voi VOCAB/GRAMMAR. */
    @Column(name = "source_ref", length = 100)
    private String sourceRef;

    /** Thoi diem muc do duoc ghi nhan la "da hoc" lan dau. */
    @CreationTimestamp
    @Column(name = "first_learned_at", nullable = false, updatable = false)
    private Instant firstLearnedAt;
}
