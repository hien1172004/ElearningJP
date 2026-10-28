package com.jp.elearningjp.repository.gamification;

import com.jp.elearningjp.entity.gamification.UserLearnedItem;
import com.jp.elearningjp.shared.enums.LookupTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLearnedItemRepository extends JpaRepository<UserLearnedItem, Long> {

    // ------------------------------------------------------------------
    // Ghi nhan "da hoc" — atomic, chong trung bang UNIQUE constraint
    // ------------------------------------------------------------------

    /**
     * Ghi nhan 1 muc da hoc (chi lan dau tien moi tinh).
     *
     * <p><b>Vì sao {@code INSERT ... ON CONFLICT DO NOTHING}?</b> Day la cach
     * <b>atomic</b> de kiem tra "muc nay da duoc hoc chua":</p>
     * <ul>
     *   <li>Trả về <b>1</b> → muc nay MỚI (chua co trong bang).</li>
     *   <li>Trả về <b>0</b> → muc nay da hoc truoc do (conflict).</li>
     * </ul>
     *
     * <p>Nếu lam theo kieu "SELECT xem co chua, roi INSERT" se co race condition
     * khi user mo cung 1 tu o 2 tab (ca 2 request deu thay "chua hoc" → ca 2 deu
     * tinh la tu moi). Lệnh INSERT nay duoc PostgreSQL xu ly trong 1 lenh
     * → chi 1 request duoc tinh la "moi".</p>
     *
     * <p><b>Vì sao native query?</b> {@code ON CONFLICT} khong co ban tuong duong
     * trong JPQL, va lenh INSERT co tham so phai bind theo thu tu (xem
     * {@code UserDailyStatsRepository#aggregateTwoPeriods} de biet ly do JPQL
     * named param lap lai gay sai).</p>
     *
     * @param userId    user tra tu
     * @param targetType loai doi tuong (VOCAB | KANJI | GRAMMAR)
     * @param targetKey  khoa danh tinh: {@code "id:<targetId>"} hoac {@code "ref:<sourceRef>"}
     * @param targetId   id doi tuong (null voi KANJI)
     * @param sourceRef  chuoi tham chieu (null voi VOCAB/GRAMMAR)
     * @return so dong bi anh huong (1 = moi, 0 = da hoc truoc do)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_learned_items (user_id, target_type, target_key, target_id, source_ref)
            VALUES (:userId, :targetType, :targetKey, :targetId, :sourceRef)
            ON CONFLICT (user_id, target_type, target_key) DO NOTHING
            """, nativeQuery = true)
    int register(Long userId, String targetType, String targetKey, Long targetId, String sourceRef);

    // ------------------------------------------------------------------
    // Truy van
    // ------------------------------------------------------------------

    /** Muc da hoc cua user theo loai doi tuong. */
    List<UserLearnedItem> findByUserIdAndTargetType(Long userId, LookupTargetType targetType);

    /** Da user hoc muc nay chua? */
    boolean existsByUserIdAndTargetTypeAndTargetKey(
            Long userId, LookupTargetType targetType, String targetKey);

    /** Tong so muc da hoc (moi hoc) cua user — dung cho bao cao. */
    long countByUserId(Long userId);
}
