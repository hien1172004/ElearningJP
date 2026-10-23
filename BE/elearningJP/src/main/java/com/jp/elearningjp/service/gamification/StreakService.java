package com.jp.elearningjp.service.gamification;

import com.jp.elearningjp.entity.gamification.UserGamification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Tinh va cap nhat streak (chuoi ngay hoc lien tiep).
 *
 * <p>Dinh nghia streak cho trang Progress: 1 ngay duoc coi la "co hoc"
 * neu co {@code >= 1} lan tra tu dien ({@code user_daily_stats.lookups > 0}).</p>
 *
 * <p>{@link UserGamification} luu {@code currentStreak} + {@code lastActivityDate}
 * de tinh nhanh, nguon su that van la {@code user_daily_stats}.</p>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StreakService {

    /** Múi giờ nghiệp vụ — xem {@link com.jp.elearningjp.config.TimeConfig}. */
    private final Clock clock;

    /**
     * Cap nhat streak cua gamification theo quy tac:
     * <ul>
     *   <li>Chua co {@code lastActivityDate} -&gt; streak = 1</li>
     *   <li>Hom qua vua hoc -&gt; streak += 1</li>
     *   <li>Hom nay da hoc -&gt; giu nguyen (idempotent)</li>
     *   <li>Gap &gt; 1 ngay -&gt; streak = 1</li>
     * </ul>
     */
    public void updateStreak(UserGamification gamification) {
        LocalDate today = LocalDate.now(clock);
        LocalDate last = gamification.getLastActivityDate();

        int newStreak;
        if (last == null) {
            newStreak = 1;
        } else if (last.equals(today)) {
            // Hom nay da hoc roi -> khong cong them, chi dam bao >= 1
            newStreak = Math.max(1, gamification.getCurrentStreak());
        } else if (last.equals(today.minusDays(1))) {
            newStreak = Math.max(1, gamification.getCurrentStreak()) + 1;
        } else {
            // Gap ngay -> chuoi bi break
            newStreak = 1;
        }

        gamification.setCurrentStreak(newStreak);
        gamification.setLongestStreak(Math.max(gamification.getLongestStreak(), newStreak));
        gamification.setLastActivityDate(today);
    }

    /**
     * Tinh {@code currentStreak} truc tiep tu cac ngay active da sap xep GIAM DAN.
     *
     * <p><b>Uu viet so voi cach duyet HashSet:</b> khong can build
     * {@code HashSet} toan bo lich su (O(n) RAM) — chi duyet danh sach da
     * <b>tang dan dan</b> va dung ngay khi gap ngay khong lien tiep.</p>
     *
     * <p>Y nghia: chuoi chi con "song" neu hom nay hoac hom qua co hoat dong.</p>
     *
     * @param activeDatesDesc cac ngay co {@code lookups > 0}, sap xep <b>giảm dần</b>
     * @param today           ngay hom nay
     * @return do dai chuoi ngay lien tiep
     */
    public int computeCurrentStreakFromDesc(List<LocalDate> activeDatesDesc, LocalDate today) {
        if (activeDatesDesc == null || activeDatesDesc.isEmpty()) {
            return 0;
        }

        // Da sap xep giam dan: ngay lon nhat la ngay gan nhat nhat.
        LocalDate newest = activeDatesDesc.get(0);

        // Chi dem neu chuoi bat dau tu hom nay hoac hom qua
        if (!newest.equals(today) && !newest.equals(today.minusDays(1))) {
            return 0;
        }

        int streak = 0;
        LocalDate expected = newest;
        for (LocalDate date : activeDatesDesc) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (date.isBefore(expected)) {
                // Da gap ngay khong lien tiep -> chuoi ket thuc o day
                break;
            }
            // date.isAfter(expected): ngay trung lap -> bo qua
        }
        return streak;
    }
}