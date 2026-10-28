package com.jp.elearningjp.service.gamification;

import com.jp.elearningjp.repository.gamification.UserLearnedItemRepository;
import com.jp.elearningjp.shared.enums.LookupTargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi nhan "da hoc" cho 1 muc tu dien — nguon su that cho viec chong trung.
 *
 * <p><b>Vai tro:</b> phan biet "từ đã học" (MỚI) voi "số lần tra từ" (TỔNG).
 * Mot muc chi duoc coi la <b>da hoc</b> khi la DONG LAN DAU TIEN.</p>
 *
 * <p><b>Luu y ve transaction:</b> method nay chay {@code REQUIRED} (mac dinh)
 * — tức là <b>chung transaction ngoài</b> với caller. Vì vậy:</p>
 * <ul>
 *   <li>Neu caller rollback → ghi nhan "da hoc" cung rollback → khong "mat" muc
 *       do mot request khong hoan thanh.</li>
 *   <li>Khong tao transaction rieng (khong {@code REQUIRES_NEW}) — tranh
 *       trang thai nua ma khong nhat quan.</li>
 * </ul>
 *
 * <p><b>Vì sao dung khoa "id:" / "ref:"?</b> Ba loai doi tuong duoc mo ta
 * khac nhau trong DB: VOCAB/GRAMMAR co {@code target_id} la so, con KANJI chi
 * co khu tu (chuoi). Dung 1 cot {@code target_key} de chuan hoa ve chuoi de
 * rang buoc UNIQUE ap dung duoc cho ca 3 loai:</p>
 * <ul>
 *   <li>VOCAB / GRAMMAR → {@code "id:123"}</li>
 *   <li>KANJI → {@code "ref:日"}</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearnedItemService {

    /** Tien to cho khoa dua tren ID (VOCAB / GRAMMAR). */
    private static final String KEY_PREFIX_ID = "id:";

    /** Tien to cho khoa dua tren chuoi tham chieu (KANJI). */
    private static final String KEY_PREFIX_REF = "ref:";

    private final UserLearnedItemRepository learnedItemRepository;

    /**
     * Ghi nhan 1 muc da hoc; tra ve {@code true} neu day la LAN DAU TIEN.
     *
     * @param userId    user tra tu
     * @param targetType loai doi tuong (VOCAB | KANJI | GRAMMAR)
     * @param targetId  id doi tuong (null voi KANJI)
     * @param sourceRef chuoi tham chieu (ky tu Han voi KANJI)
     * @return {@code true} neu muc nay MOI (vua duoc hoc lan dau);
     *         {@code false} neu da hoc truoc do, hoac du lieu khong hop le
     */
    @Transactional
    public boolean register(Long userId, LookupTargetType targetType, Long targetId, String sourceRef) {
        if (userId == null || targetType == null) {
            log.warn("[LearnedItem] register skipped: userId={} targetType={}", userId, targetType);
            return false;
        }

        String targetKey = buildTargetKey(targetType, targetId, sourceRef);
        if (targetKey == null) {
            // Khong tao duoc khoa danh tinh (thiếu id hoặc ký tự) →
            // coi như "không phải mục mới" để không làm sai số liệu.
            log.warn("[LearnedItem] register skipped (no target key): user={} type={} id={} ref={}",
                    userId, targetType, targetId, sourceRef);
            return false;
        }

        int affected = learnedItemRepository.register(
                userId,
                targetType.name(),
                targetKey,
                targetId,
                sourceRef);

        return affected == 1;
    }

    /**
     * Dung khoa danh tinh cho 1 muc.
     *
     * @return {@code "id:<targetId>"} / {@code "ref:<sourceRef>"}, hoặc {@code null}
     *         neu du lieu khong du de tao khoa.
     */
    private String buildTargetKey(LookupTargetType targetType, Long targetId, String sourceRef) {
        return switch (targetType) {
            case VOCAB, GRAMMAR -> (targetId != null)
                    ? KEY_PREFIX_ID + targetId
                    : null;
            case KANJI -> (sourceRef != null && !sourceRef.isBlank())
                    ? KEY_PREFIX_REF + sourceRef.trim()
                    : null;
        };
    }
}
