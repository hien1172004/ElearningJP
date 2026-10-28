package com.jp.elearningjp.repository.gamification;

import com.jp.elearningjp.entity.gamification.UserDailyStats;
import com.jp.elearningjp.entity.gamification.UserDailyStatsId;
import com.jp.elearningjp.service.gamification.StreakService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserDailyStatsRepository extends JpaRepository<UserDailyStats, UserDailyStatsId> {

    Optional<UserDailyStats> findByIdUserIdAndIdStatDate(Long userId, LocalDate statDate);

    /** Du lieu 1 ky cho lich + bieu do (Buc 5/6). */
    List<UserDailyStats> findByIdUserIdAndIdStatDateBetweenOrderByIdStatDateAsc(
            Long userId, LocalDate from, LocalDate to);

    /**
     * Chuỗi ngày hoạt động dài nhất (longest streak) — tính trực tiếp trong DB.
     *
     * <p><b>Vì sao không tính ở Java?</b> Cách cũ tải toàn bộ lịch sử active về
     * RAM rồi sort + quét (O(n log n) + O(n) bộ nhớ). Query này dùng kỹ thuật
     * "gaps and islands": cùng {@code statDate - row_number()} thì thuộc cùng
     * một chuỗi liên tiếp → group theo {@code grp} rồi lấy max độ dài.</p>
     *
     * <p>Chỉ trả về <b>một số</b> — không tải dữ liệu về app.</p>
     *
     * <p><b>Điều kiện {@code new_items_learned > 0}</b> (không phải
     * {@code lookups > 0}): theo chính sách STRICT, ngày chỉ được tính là
     * "có học" khi có ít nhất 1 MỤC MỚI. Tra lại từ cũ không giữ streak.</p>
     *
     * @param userId user cần tính
     * @return độ dài chuỗi dài nhất (0 nếu không có ngày hoạt động nào)
     */
    @Query(value = """
            SELECT COALESCE(MAX(island.cnt), 0)
            FROM (
                SELECT grp, COUNT(*) AS cnt
                FROM (
                    SELECT d.stat_date
                           - CAST(ROW_NUMBER() OVER (ORDER BY d.stat_date) AS INT) AS grp
                    FROM user_daily_stats d
                    WHERE d.user_id = :userId
                      AND d.new_items_learned > 0
                    GROUP BY d.stat_date
                ) gaps
                GROUP BY grp
            ) island
            """, nativeQuery = true)
    long findLongestActiveStreak(Long userId);

    /**
     * Cửa sổ ngày hoạt động <b>liên tục</b> kết thúc ở {@code endDate} — dùng tính streak.
     *
     * <p><b>Vì sao tối ưu?</b> Cách cũ tải <i>toàn bộ</i> lịch sử active rồi
     * sort ở Java (O(n log n) + O(n) RAM). Trong khi streak chỉ
     * cần chuỗi ngày liên tiếp tính từ hôm nay/hôm qua ngược lại — trung bình chỉ
     * vài chục ngày. Query này chỉ quét vùng dữ liệu liên quan.</p>
     *
     * <p>Điều kiện {@code statDate <= :endDate} giữ đúng ngữ nghĩa "chuỗi kết thúc tại
     * {@code endDate}" (gọi với {@code today} hoặc {@code today - 1} tuừ
     * {@link StreakService}). Kết quả <b>giảm dần</b> — caller chỉ cần duyệt cho đến
     * khi gặp ngày không liên tục.</p>
     *
     * <p><b>Điều kiện {@code newItemsLearned > 0}</b> (không phải {@code lookups > 0}):
     * theo chính sách STRICT, chỉ ngày có ít nhất 1 MỤC MỚI mới giữ được
     * streak. Tra lại từ cũ không giữ streak.</p>
     *
     * @param userId user cần tính streak
     * @param endDate ngày kết thúc chuỗi (thường là hôm nay hoặc hôm qua)
     * @param windowDays số ngày tối đa quét ngược (chặn truy vấn quá rộng)
     */
    @Query("""
            SELECT d.id.statDate
            FROM UserDailyStats d
            WHERE d.id.userId = :userId
              AND d.newItemsLearned > 0
              AND d.id.statDate <= :endDate
              AND d.id.statDate >= :minDate
            ORDER BY d.id.statDate DESC
            """)
    List<LocalDate> findStreakWindow(Long userId, LocalDate endDate, LocalDate minDate);

/**
 * Chi tiết từng ngày trong khoang (dung cho lich thang — Khoi 2, va bien do
 * time-series — Khoi 3).
 *
 * <p>Tra ve day du entity de FE khong phai tinh lai: lich can
 * {@code newItemsLearned} (so tu MOI da hoc) va {@code lookups} (so lan tra)
 * va {@code study_seconds} de ve o.</p>
 *
 * @param userId user can xem
 * @param from   ngay bat dau (inclusive)
 * @param to     ngay ket thuc (inclusive)
 * @return cac ngay co row trong DB, tang dan theo {@code statDate}
 */
    @Query("""
            SELECT d
            FROM UserDailyStats d
            WHERE d.id.userId = :userId
              AND d.id.statDate >= :from
              AND d.id.statDate <= :to
            ORDER BY d.id.statDate ASC
            """)
    List<UserDailyStats> findCalendarRange(Long userId, LocalDate from, LocalDate to);

    /**
     * Tổng hợp <b>2 kỳ liền kề</b> trong 1 query duy nhất (Khoi 1 — summary).
     *
     * <p><b>Vì sao gộp?</b> Kỳ trước và kỳ hiện tại nằm liền nhau
     * ({@code prevTo = from - 1}), nên 1 lần scan trên {@code [prevFrom, to]}
     * cho ra cả 2 kỳ. Trước đây cần 4 query (2 aggregate + 2 đếm activeDays).</p>
     *
     * <p><b>Vì sao native query + mỗi tham số chỉ xuất hiện 1 lần?</b></p>
     * <p>JPQL với named param lặp lại (vd {@code :from} xuất hiện 7 lần) khiến
     * Hibernate sinh SQL với các {@code ?} mà <b>thứ tự bind không xác định</b>
     * → {@code BETWEEN} nhận nhầm giá trị → trả về 0 dù DB có data.
     * Bản này viết lại để mỗi tham số xuất hiện <b>đúng 1 lần</b> và
     * <b>đúng thứ tự</b> (userId → from → to → prevFrom), nên không còn cách
     * hiểu nào cho ra kết quả khác.</p>
     *
     * <p>Phân loại kỳ bằng {@code stat_date >= from} (1 mốc duy nhất) thay vì
     * {@code BETWEEN from AND to} cho từng cột — vì kỳ trước kết thúc đúng tại
     * {@code from - 1} nên chỉ cần 1 phép so sánh.</p>
     *
     * <p>Cột trả về (đúng thứ tự):</p>
     * <ol>
     *   <li>{@code SUM(new_items_learned)} kỳ hiện tại (= "từ đã học")</li>
     *   <li>{@code SUM(lookups)} kỳ hiện tại (= "số lần tra từ")</li>
     *   <li>{@code SUM(studySeconds)} kỳ hiện tại</li>
     *   <li>{@code SUM(xpEarned)} kỳ hiện tại</li>
     *   <li>Số ngày có {@code new_items_learned > 0} kỳ hiện tại (= activeDays)</li>
     *   <li>Số ngày đủ {@code new_items_learned >= goal_words} kỳ hiện tại</li>
     *   <li>{@code SUM(new_items_learned)} kỳ trước (tính % thay đổi)</li>
     *   <li>{@code SUM(lookups)} kỳ trước</li>
     *   <li>{@code SUM(studySeconds)} kỳ trước (tính % thay đổi)</li>
     * </ol>
     *
     * @param userId   user cần tổng hợp
     * @param from     ngày bắt đầu kỳ hiện tại
     * @param to       ngày kết thúc kỳ hiện tại
     * @param prevFrom ngày bắt đầu kỳ trước
     */
    @Query("""
            SELECT
                COALESCE(SUM(CASE WHEN d.id.statDate >= :from AND d.id.statDate <= :to THEN d.newItemsLearned ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN d.id.statDate >= :from AND d.id.statDate <= :to THEN d.lookups ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN d.id.statDate >= :from AND d.id.statDate <= :to THEN d.studySeconds ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN d.id.statDate >= :from AND d.id.statDate <= :to THEN d.xpEarned ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN d.id.statDate >= :from AND d.id.statDate <= :to AND d.newItemsLearned > 0 THEN 1L ELSE 0L END), 0),
                COALESCE(SUM(CASE WHEN d.id.statDate >= :from AND d.id.statDate <= :to AND d.newItemsLearned >= d.goalWords THEN 1L ELSE 0L END), 0),
                COALESCE(SUM(CASE WHEN d.id.statDate >= :prevFrom AND d.id.statDate < :from THEN d.newItemsLearned ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN d.id.statDate >= :prevFrom AND d.id.statDate < :from THEN d.lookups ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN d.id.statDate >= :prevFrom AND d.id.statDate < :from THEN d.studySeconds ELSE 0 END), 0)
            FROM UserDailyStats d
            WHERE d.id.userId = :userId
              AND d.id.statDate >= :prevFrom
              AND d.id.statDate <= :to
            """)
    List<Object[]> aggregateTwoPeriods(Long userId, LocalDate from, LocalDate to, LocalDate prevFrom);

    /**
     * Ghi 1 lan tra tu dien - nguyen tu, chi tang lookups_scored khi chua vuot tran.
     *
     * <p>Upsert nguyen tu voi WHERE: chỉ UPDATE khi lookups_scored < :lookupCeiling.
     * Tra ve so dong bi anh huong (=1 neu thanh cong, =0 neu vuot tran).</p>
     *
     * <p>Thay doi:</p>
     * <ul>
     *   <li>lookups: tang moi lan tra (khong tran) → "số lần tra từ"</li>
     *   <li>new_items_learned: chi tang khi {@code :isNew} = 1 → "từ đã học"
     *       (muc moi, chua tung hoc)</li>
     *   <li>lookups_scored: chi tang khi < ceiling</li>
     *   <li>xp_earned: chi tang khi lookups_scored tang</li>
     *   <li>goal_met: tinh lai khi new_items_learned >= goalWords</li>
     * </ul>
     *
     * @param userId user tra tu
     * @param statDate ngay tra
     * @param xpEarned XP nhan (chi cong khi lookups_scored tang)
     * @param lookupCeiling so lookup toi da tinh diem trong ngay (tu app_config)
     * @param minStudySeconds so giay toi thieu de goal_met (tu app_config)
     * @param now thoi diem hien tai
     * @param isNew 1 neu muc vua duoc hoc LAN DAU TIEN, 0 neu da hoc truoc do
     * @return so dong bi anh huong (1 = thanh cong, 0 = vuot tran)
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_daily_stats (user_id, stat_date, lookups, lookups_scored, xp_earned, goal_words, goal_met, lessons_completed, new_items_learned)
            VALUES (:userId, :statDate, 1, 1, :xpEarned, 5, (:isNew >= 5), 0, :isNew)
            ON CONFLICT (user_id, stat_date) DO UPDATE
            SET
                lookups         = user_daily_stats.lookups + 1,
                new_items_learned = user_daily_stats.new_items_learned + :isNew,
                lookups_scored  = CASE
                                    WHEN user_daily_stats.lookups_scored < :lookupCeiling
                                    THEN user_daily_stats.lookups_scored + 1
                                    ELSE user_daily_stats.lookups_scored
                                  END,
                xp_earned      = CASE
                                    WHEN user_daily_stats.lookups_scored < :lookupCeiling
                                    THEN user_daily_stats.xp_earned + :xpEarned
                                    ELSE user_daily_stats.xp_earned
                                  END,
                goal_met       = (user_daily_stats.new_items_learned + :isNew) >= user_daily_stats.goal_words
                               OR user_daily_stats.lessons_completed > 0
                               OR user_daily_stats.study_seconds >= :minStudySeconds,
                last_lookup_at = :now
            """, nativeQuery = true)
    int upsertLookup(Long userId, LocalDate statDate, int xpEarned,
                     int lookupCeiling, int minStudySeconds, Instant now, int isNew);

    /**
     * Cong them thoi gian hoc cho 1 ngay, tao row neu chua co.
     * Tu dong re-evaluate {@code goal_met}.
     *
     * <p>goal_met = true khi: study_seconds >= :minStudySeconds OR
     * new_items_learned > 0 OR lessons_completed > 0.</p>
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_daily_stats (user_id, stat_date, study_seconds, xp_earned, goal_words, goal_met, lessons_completed, lookups, lookups_scored, new_items_learned)
            VALUES (:userId, :statDate, :seconds, :xpEarned, 5,
                    (:seconds >= :minStudySeconds), 0, 0, 0, 0)
            ON CONFLICT (user_id, stat_date) DO UPDATE
            SET
                study_seconds = user_daily_stats.study_seconds + :seconds,
                xp_earned    = user_daily_stats.xp_earned + :xpEarned,
                goal_met     = (user_daily_stats.study_seconds + :seconds) >= :minStudySeconds
                               OR user_daily_stats.new_items_learned > 0
                               OR user_daily_stats.lessons_completed > 0
            """, nativeQuery = true)
    void addStudySeconds(Long userId, LocalDate statDate, int seconds, int xpEarned, int minStudySeconds);

    /**
     * Tong hop top N user trong 1 tuan cho bang xep hang.
     *
     * <p>Score = {@code new_items_learned * learned_item_points + lessons_completed * lesson_points}.</p>
     *
     * <p>Tra: [userId, totalScore, totalXpEarned], sap xep score giam dan.</p>
     *
     * @param learnedItemPoints diem moi tu MOI da hoc (tu app_config)
     * @param lessonPoints      diem moi lesson (tu app_config)
     */
    @Query(value = """
            SELECT d.user_id,
                   COALESCE(SUM(d.new_items_learned * :learnedItemPoints + d.lessons_completed * :lessonPoints), 0) AS score,
                   COALESCE(SUM(d.xp_earned), 0) AS xp
            FROM user_daily_stats d
            WHERE d.stat_date BETWEEN :weekStart AND :weekEnd
            GROUP BY d.user_id
            HAVING SUM(d.new_items_learned * :learnedItemPoints + d.lessons_completed * :lessonPoints) > 0
            ORDER BY score DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findTopUsersByWeek(LocalDate weekStart, LocalDate weekEnd, int limit,
                                      int learnedItemPoints, int lessonPoints);

    /**
     * TẤT CẢ điểm của TẤT CẢ user trong 1 tuần — <b>không LIMIT</b>.
     *
     * <p><b>Vì sao không dùng {@link #findTopUsersByWeek}?</b> Query đó có
     * {@code LIMIT :limit} → chỉ trả về top N. Nếu dùng nó để rebuild ZSET thì
     * {@code ZCARD} (totalPlayers) sẽ khớp sai với số user thật, và các user
     * ngoài top N sẽ mất hạng. Đây là nguồn cho {@code rebuild()}.</p>
     *
     * <p>Chỉ trả về user có score &gt; 0 ({@code HAVING}) → không tạo member
     * score 0 làm nhiễu {@code ZCARD}.</p>
     *
     * @return list {@code [userId, score]} — score = new_items_learned*learnedItemPoints
     *         + lessons_completed*lessonPoints
     */
    @Query(value = """
            SELECT d.user_id,
                   COALESCE(SUM(d.new_items_learned * :learnedItemPoints + d.lessons_completed * :lessonPoints), 0) AS score
            FROM user_daily_stats d
            WHERE d.stat_date BETWEEN :weekStart AND :weekEnd
            GROUP BY d.user_id
            HAVING SUM(d.new_items_learned * :learnedItemPoints + d.lessons_completed * :lessonPoints) > 0
            """, nativeQuery = true)
    List<Object[]> findAllScoresByWeek(LocalDate weekStart, LocalDate weekEnd,
                                       int learnedItemPoints, int lessonPoints);

    /**
     * Tổng hợp chỉ số tuần cho một danh sách user — <b>1 query cho cả bảng top 30</b>.
     *
     * <p>Tránh N+1: thay vì gọi 1 query/user, gom tất cả id vào 1 {@code IN}.</p>
     *
     * <p>Cột trả về (đúng thứ tự):</p>
     * <ol>
     *   <li>{@code user_id}</li>
     *   <li>{@code SUM(lookups)} — tổng lần tra trong tuần (KHÔNG trần)</li>
     *   <li>{@code SUM(new_items_learned)} — tổng số từ MỚI đã học</li>
     *   <li>{@code SUM(lessons_completed)}</li>
     *   <li>{@code SUM(xp_earned)}</li>
     * </ol>
     */
    @Query(value = """
            SELECT d.user_id,
                   COALESCE(SUM(d.lookups), 0),
                   COALESCE(SUM(d.new_items_learned), 0),
                   COALESCE(SUM(d.lessons_completed), 0),
                   COALESCE(SUM(d.xp_earned), 0)
            FROM user_daily_stats d
            WHERE d.user_id IN (:userIds)
              AND d.stat_date BETWEEN :weekStart AND :weekEnd
            GROUP BY d.user_id
            """, nativeQuery = true)
    List<Object[]> findWeekStatsByUserIds(List<Long> userIds, LocalDate weekStart, LocalDate weekEnd);

    /**
     * Điểm xếp hạng tuyệt đối của 1 user trong tuần — dùng cho {@code syncUserScore}.
     *
     * @return điểm (0 nếu user chưa có hoạt động trong tuần)
     */
    @Query(value = """
            SELECT COALESCE(SUM(d.new_items_learned * :learnedItemPoints + d.lessons_completed * :lessonPoints), 0)
            FROM user_daily_stats d
            WHERE d.user_id = :userId
              AND d.stat_date BETWEEN :weekStart AND :weekEnd
            """, nativeQuery = true)
    long findUserWeekScore(Long userId, LocalDate weekStart, LocalDate weekEnd,
                           int learnedItemPoints, int lessonPoints);

    /**
     * Số user có điểm <b>cao hơn</b> {@code score} trong tuần.
     *
     * <p><b>Tương đương đúng</b> với {@code ZCOUNT (score, +inf)} trên Redis
     * (exclusive ở {@code score}) → cho cùng kết quả "cùng điểm thì cùng hạng"
     * (1, 1, 3). Dùng làm fallback khi Redis lỗi / rỗng.</p>
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM (
                SELECT d.user_id,
                       SUM(d.new_items_learned * :learnedItemPoints + d.lessons_completed * :lessonPoints) AS score
                FROM user_daily_stats d
                WHERE d.stat_date BETWEEN :weekStart AND :weekEnd
                GROUP BY d.user_id
                HAVING SUM(d.new_items_learned * :learnedItemPoints + d.lessons_completed * :lessonPoints) > :score
            ) t
            """, nativeQuery = true)
    long countUsersWithScoreGreaterThan(LocalDate weekStart, LocalDate weekEnd, long score,
                                        int learnedItemPoints, int lessonPoints);

    /**
     * Tổng số user có điểm &gt; 0 trong tuần (= {@code ZCARD} của Redis).
     *
     * <p>Dùng làm fallback cho {@code totalPlayers} khi Redis lỗi / rỗng.</p>
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM (
                SELECT d.user_id,
                       SUM(d.new_items_learned * :learnedItemPoints + d.lessons_completed * :lessonPoints) AS score
                FROM user_daily_stats d
                WHERE d.stat_date BETWEEN :weekStart AND :weekEnd
                GROUP BY d.user_id
                HAVING SUM(d.new_items_learned * :learnedItemPoints + d.lessons_completed * :lessonPoints) > 0
            ) t
            """, nativeQuery = true)
    long countUsersWithScoreInWeek(LocalDate weekStart, LocalDate weekEnd,
                                   int learnedItemPoints, int lessonPoints);

    /**
     * Tang lessons_completed += 1 khi hoan thanh bai hoc.
     *
     * <p>goal_met = true khi: lessons_completed + 1 >= 1 OR
     * new_items_learned > 0 OR study_seconds >= :minStudySeconds.</p>
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_daily_stats (user_id, stat_date, lessons_completed, xp_earned, lookups, goal_words, goal_met, lookups_scored, new_items_learned)
            VALUES (:userId, :statDate, 1, :xpEarned, 0, 5, FALSE, 0, 0)
            ON CONFLICT (user_id, stat_date) DO UPDATE
            SET
                lessons_completed = user_daily_stats.lessons_completed + 1,
                xp_earned        = user_daily_stats.xp_earned + :xpEarned,
                goal_met         = (user_daily_stats.lessons_completed + 1) >= 1
                                   OR user_daily_stats.new_items_learned > 0
                                   OR user_daily_stats.study_seconds >= :minStudySeconds
            """, nativeQuery = true)
    void incrementLessonsCompleted(Long userId, LocalDate statDate, int xpEarned, int minStudySeconds);

    /**
     * Cong them XP cho 1 ngay, tao row neu chua co.
     * Dung khi lookup vuot tran — chi cong XP, khong tang lookups.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_daily_stats (user_id, stat_date, xp_earned, lookups, lessons_completed, goal_words, goal_met, lookups_scored, new_items_learned)
            VALUES (:userId, :statDate, :xpEarned, 0, 0, 5, FALSE, 0, 0)
            ON CONFLICT (user_id, stat_date) DO UPDATE
            SET xp_earned = user_daily_stats.xp_earned + :xpEarned
            """, nativeQuery = true)
    void addXp(Long userId, LocalDate statDate, int xpEarned);
}
