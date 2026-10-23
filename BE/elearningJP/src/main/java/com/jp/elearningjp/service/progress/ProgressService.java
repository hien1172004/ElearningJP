package com.jp.elearningjp.service.progress;

import com.jp.elearningjp.dto.response.progress.CalendarDayResponse;
import com.jp.elearningjp.dto.response.progress.CalendarResponse;
import com.jp.elearningjp.dto.response.progress.LeaderboardEntryResponse;
import com.jp.elearningjp.dto.response.progress.LeaderboardResponse;
import com.jp.elearningjp.dto.response.progress.MyRankResponse;
import com.jp.elearningjp.dto.response.progress.ProgressSummaryResponse;
import com.jp.elearningjp.dto.response.progress.TopLookupResponse;
import com.jp.elearningjp.entity.gamification.UserDailyStats;
import com.jp.elearningjp.entity.gamification.UserGamification;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.gamification.StudyEventRepository;
import com.jp.elearningjp.repository.gamification.UserDailyStatsRepository;
import com.jp.elearningjp.repository.gamification.UserGamificationRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.config.AppConfigService;
import com.jp.elearningjp.service.gamification.StreakService;
import com.jp.elearningjp.shared.enums.LookupTargetType;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Đọc & tổng hợp dữ liệu tiến độ học tập (read-only).
 *
 * <p>Khác với các service ghi sự kiện ở Bước 2/3, service này chỉ đọc —
 * không có side-effect, không cần transaction ghi.</p>
 *
 * <p>Quy ước nghiệp vụ đã chốt:</p>
 * <ul>
 *   <li>{@code wordsLearned} = số lần tra từ điển (= {@code user_daily_stats.lookups})</li>
 *   <li>Streak chỉ tính ngày có {@code >= 1} lần tra từ</li>
 *   <li>Mục tiêu mặc định 5 từ/ngày</li>
 * </ul>
 *
 * <p><b>Cache:</b> summary/calendar/chart đọc trực tiếp từ DB (không cache).
 * Bảng xếp hạng dùng Redis Sorted Set (key {@code leaderboard:weekly:v2:{week}}) —
 * ZSET là index dẫn xuất từ DB, xem {@link LeaderboardService}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProgressService {

    /** Số ngày tối đa cho 1 request (366 ngay ~ 1 năm). */
    public static final int MAX_RANGE_DAYS = 366;

    /** Số ngày mặc định khi FE không truyền `from` (tháng gần nhất). */
    private static final int DEFAULT_RANGE_DAYS = 30;

    /** Mục tiêu mặc định (đã chốt ở plan). */
    private static final int DEFAULT_GOAL_WORDS = 5;

    /**
     * Cửa sổ quét tối đa để tính streak (ngày).
     *
     * <p>Streak hợp lý hiếm khi vượt 365 ngày. Giới hạn này biến query
     * "tải toàn bộ lịch sử" thành "quét tối đa 1 năm" — có index theo
     * {@code (user_id, stat_date)} nên vẫn rất nhanh, nhưng trần được độ phức tạp.</p>
     */
    private static final int STREAK_WINDOW_DAYS = 365;

    /** Số dòng hiển thị trong bảng xếp hạng (top 5). */
    private static final int LEADERBOARD_LIMIT = 5;

    /** Số dòng mặc định của bảng "Từ đã tra nhiều nhất" (Khoi 4). */
    private static final int DEFAULT_TOP_LOOKUPS = 30;

    /** Trần cứng cho {@code limit} của bảng top từ — chặn client xin 10000 dòng. */
    private static final int MAX_TOP_LOOKUPS = 100;

    /** Số tuần Redis giữ lại key xếp hạng (8 tuần = 56 ngày). */
    private static final int LEADERBOARD_RETENTION_WEEKS = 8;

    /** App config keys for leaderboard scoring */
    private static final String CFG_LOOKUP_POINTS = "lookup_points";
    private static final String CFG_LESSON_POINTS = "lesson_points";
    private static final int DEFAULT_LOOKUP_POINTS = 1;
    private static final int DEFAULT_LESSON_POINTS = 1;

    private final UserDailyStatsRepository dailyStatsRepository;
    private final UserGamificationRepository gamificationRepository;
    private final StreakService streakService;
    private final LeaderboardService leaderboardService;
    private final UserRepository userRepository;
    private final AppConfigService appConfigService;
    private final CurrentUserService currentUserService;

    // --- Khoai 2 (lich) + Khoi 4 (top tu) ---
    private final StudyEventRepository studyEventRepository;
    private final VocabularyRepository vocabularyRepository;
    private final GrammarPointRepository grammarPointRepository;
    private final WritingCharacterRepository writingCharacterRepository;

    /** Múi giờ nghiệp vụ — xem {@link com.jp.elearningjp.config.TimeConfig}. */
    private final Clock clock;

    // =====================================================================
    // Khoi 1 — Summary
    // =====================================================================

    /**
     * Tổng hợp tiến độ trong kỳ {@code from..to} (inclusive) cho user hiện tại.
     *
     * <p>Đây là entry point dùng bởi Controller — tự lấy {@code userId} từ
     * SecurityContext (JWT), không cần caller truyền vào.</p>
     *
     * @param from ngày bắt đầu (null → mặc định {@code to - 29 ngày})
     * @param to   ngày kết thúc (null → mặc định hôm nay)
     */
    @Transactional(readOnly = true)
    public ProgressSummaryResponse getSummary(LocalDate from, LocalDate to) {
        return getSummary(currentUserService.getCurrentUserId(), from, to);
    }

    /**
     * Tổng hợp tiến độ trong kỳ {@code from..to} (inclusive) cho user chỉ định.
     *
     * <p><b>Số query DB: 3</b> (trước đây là 6):</p>
     * <ol>
     *   <li>1 query gộp aggregate kỳ hiện tại + kỳ trước</li>
     *   <li>1 query lấy {@code longestStreak} đã lưu sẵn trong {@code user_gamification}</li>
     *   <li>1 query lấy cửa sổ ngày active để tính {@code currentStreak}</li>
     * </ol>
     *
     * <p>Dùng cho các call nội bộ (job, admin, test) — không phụ thuộc SecurityContext.</p>
     *
     * @param userId id user cần lấy tiến độ
     * @param from   ngày bắt đầu (null → mặc định {@code to - 29 ngày})
     * @param to     ngày kết thúc (null → mặc định hôm nay)
     */
    @Transactional(readOnly = true)
    public ProgressSummaryResponse getSummary(Long userId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate end = (to != null) ? to : today;
        LocalDate start = (from != null) ? from : end.minusDays(DEFAULT_RANGE_DAYS - 1);

        // Chặn khoảng thời gian quá dài để tránh query nặng
        int days = (int) (ChronoUnit.DAYS.between(start, end) + 1);
        if (days > MAX_RANGE_DAYS) {
            start = end.minusDays(MAX_RANGE_DAYS - 1);
            days = MAX_RANGE_DAYS;
        } else if (days < 1) {
            // from > to → người dùng nhập sai tham số, trả về 0 thay vì query rỗng
            start = end;
            days = 1;
        }

        // --- Kỳ trước: cùng độ dài, liền kề trước kỳ hiện tại ---
        // prevTo = start - 1, nên ranh giới phân loại chỉ cần 1 mốc: stat_date >= start
        LocalDate prevFrom = start.minusDays(days);

        // --- 1 query gộp: kỳ hiện tại + kỳ trước (liền kề → 1 lần scan) ---
        Aggregate agg = aggregateTwoPeriods(userId, start, end, prevFrom);

        // --- Streak: chuỗi ngày liên tiếp kết thúc ở hôm nay/hôm qua ---
        List<LocalDate> activeDatesDesc = dailyStatsRepository.findStreakWindow(
                userId, today, today.minusDays(STREAK_WINDOW_DAYS - 1));
        int currentStreak = streakService.computeCurrentStreakFromDesc(activeDatesDesc, today);

        // --- longestStreak: ưu tiên giá trị đã lưu trong user_gamification (O(1)),
        //     fallback tính lại từ DB khi row chưa tồn tại (user mới / chưa ghi XP) ---
        Integer longestFromDb = gamificationRepository.findById(userId)
                .map(UserGamification::getLongestStreak)
                .orElse(null);
        int longestStreak = (longestFromDb != null)
                ? longestFromDb
                : (int) Math.min(dailyStatsRepository.findLongestActiveStreak(userId), Integer.MAX_VALUE);

        // --- Số từ đã tra HÔM NAY (độc lập kỳ đã chọn) ---
        // Thẻ "Mục tiêu ngày" cần tiến độ hôm nay, không phải tổng của cả kỳ
        // → đọc riêng 1 row (PK (user_id, stat_date) → O(1)).
        UserDailyStats todayRow = dailyStatsRepository
                .findByIdUserIdAndIdStatDate(userId, today)
                .orElse(null);
        int wordsToday = (todayRow != null && todayRow.getLookups() != null)
                ? todayRow.getLookups() : 0;

        return ProgressSummaryResponse.builder()
                .from(start)
                .to(end)
                .wordsLearned(agg.lookups())
                .studySeconds(agg.studySeconds())
                .xpEarned(agg.xpEarned())
                .activeDays(agg.activeDays())
                .goalMetDays(agg.goalMetDays())
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .wordsLearnedDeltaPct(deltaPct(agg.lookups(), agg.prevLookups()))
                .studySecondsDeltaPct(deltaPct(agg.studySeconds(), agg.prevStudySeconds()))
                .goalWords(DEFAULT_GOAL_WORDS)
                .today(today)
                .wordsLearnedToday(wordsToday)
                // Quy tắc HẸP (khớp lịch ở Khối 2 và top-lookups):
                // chỉ tính "đạt" khi đủ số từ đã tra.
                .goalMetToday(wordsToday >= DEFAULT_GOAL_WORDS)
                .build();
    }

    // =====================================================================
    // Khối 2 — Lịch học + streak
    // =====================================================================

    /**
     * Lịch học + chuỗi streak cho 1 tháng (Khoi 2).
     *
     * <p><b>Số query DB: 3</b></p>
     * <ol>
     *   <li>1 query lấy các ngày có hoạt động trong tháng (dùng index
     *       {@code idx_user_daily_stats_user_date}).</li>
     *   <li>1 query cửa sổ streak (tái sử dụng, không query lại toàn bộ lịch sử).</li>
     *   <li>1 query đọc {@code longestStreak} đã lưu sẵn trong {@code user_gamification}.</li>
     * </ol>
     *
     * <p><b>Chỉ trả về ngày CÓ hoạt động.</b> Ngày trống không có row trong
     * {@code user_daily_stats} → FE tự điền các ô trống khi dựng lưới.
     * Cách này tiết kiệm payload (tháng 31 ngày nhưng user mới học chỉ có
     * vài ngày) và tránh phải phân loại "ngày không học" ở phía BE.</p>
     *
     * <p><b>Tháng tương lai được phép</b> (chỉ trả về mảng rỗng) — khác với
     * {@code week} của leaderboard vốn trả 400 vì sai nghĩa dữ liệu.</p>
     *
     * @param month chuỗi {@code yyyy-MM} (null/blank/sai → tháng hiện tại)
     */
    @Transactional(readOnly = true)
    public CalendarResponse getCalendar(String month) {
        Long userId = currentUserService.getCurrentUserId();
        LocalDate today = LocalDate.now(clock);
        YearMonth ym = ProgressMonthUtil.resolve(month, clock);

        // Ngày 1 tháng đầu → ngày cuối tháng cuối.
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        // 1. Các ngày có hoạt động trong tháng
        List<UserDailyStats> rows = dailyStatsRepository.findCalendarRange(
                userId, monthStart, monthEnd);

        // 2. Streak — cửa sổ cuộn ngược, không phụ thuộc tháng đang xem
        List<LocalDate> activeDatesDesc = dailyStatsRepository.findStreakWindow(
                userId, today, today.minusDays(STREAK_WINDOW_DAYS - 1));
        int currentStreak = streakService.computeCurrentStreakFromDesc(activeDatesDesc, today);

        // 3. longestStreak — ưu tiên giá trị đã lưu trong user_gamification (O(1))
        Integer longestFromDb = gamificationRepository.findById(userId)
                .map(UserGamification::getLongestStreak)
                .orElse(null);
        int longestStreak = (longestFromDb != null)
                ? longestFromDb
                : (int) Math.min(dailyStatsRepository.findLongestActiveStreak(userId), Integer.MAX_VALUE);

        // Dựng danh sách ngày + tổng hợp tháng
        List<CalendarDayResponse> days = new ArrayList<>(rows.size());
        int goalMetDays = 0;
        int activeDays = 0;
        long totalLookups = 0L;

        for (UserDailyStats row : rows) {
            LocalDate date = row.getId().getStatDate();
            int lookups = nz(row.getLookups());
            int lessons = nz(row.getLessonsCompleted());
            int seconds = nz(row.getStudySeconds());

            // Mục tiêu từ của chính ngày đó (mặc định 5 nếu dữ liệu cũ chưa có).
            int goalWords = (row.getGoalWords() != null && row.getGoalWords() > 0)
                    ? row.getGoalWords() : DEFAULT_GOAL_WORDS;

            boolean hasActivity = lookups > 0 || lessons > 0 || seconds > 0;
            // Quy tắc HẸP: chỉ tính "đạt mục tiêu" khi đủ số từ đã tra.
            // (Không dùng cột goal_met vì cột đó còn tính cả học bài / học đủ
            //  thời gian — sẽ lệch với thẻ "Mục tiêu ngày" ở Khối 1.)
            boolean goalMet = lookups >= goalWords;

            days.add(CalendarDayResponse.builder()
                    .date(date)
                    .lookups(lookups)
                    .lessonsCompleted(lessons)
                    .studySeconds(seconds)
                    .xpEarned(nz(row.getXpEarned()))
                    .hasActivity(hasActivity)
                    .goalMet(goalMet)
                    .build());

            if (hasActivity) activeDays++;
            if (goalMet) goalMetDays++;
            totalLookups += lookups;
        }

        return CalendarResponse.builder()
                .month(ym.toString())
                .monthStart(monthStart)
                .monthEnd(monthEnd)
                .today(today)
                .goalWords(DEFAULT_GOAL_WORDS)
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .activeDays(activeDays)
                .goalMetDays(goalMetDays)
                .totalLookups((int) Math.min(totalLookups, Integer.MAX_VALUE))
                .days(days)
                .build();
    }

    // =====================================================================
    // Khối 4 — Top từ đã tra
    // =====================================================================

/**
     * Bảng "Từ đã tra nhiều nhất" (Khoi 4).
     *
     * <p><b>Mặc định: top N theo TOÀN BỘ lịch sử.</b> Bỏ trống cả {@code from}
     * và {@code to} → xếp hạng mọi lần tra từ khi user bắt đầu học, không giới
     * hạn 30 ngày. Chỉ khi truyền {@code from}/{@code to} mới lọc theo kỳ
     * (kỳ mặc định 30 ngày gần nhất).</p>
     *
     * <p><b>Số query DB: 1 + tối đa 3</b> (không N+1):</p>
     * <ol>
   *   <li>1 query {@code GROUP BY target} trên {@code study_events} — có index
   *       partial {@code idx_study_events_lookup}.</li>
   *   <li>Batch fetch thông tin hiển thị: 1 query {@code vocabulary} +
   *       1 query {@code grammar_points} + 1 query {@code writing_characters}.</li>
   * </ol>
     *
   * <p><b>Vì sao gộp 3 loại vào 1 bảng?</b> Người dùng tra cả từ vựng, ngữ pháp
   * và Hán tự. Tách 3 bảng riêng sẽ tốn thêm 2 tiêu đề + 2 request, trong khi
   * {@link LookupTargetType} đã ghi sẵn loại đối tượng vào mỗi event → 1 bảng
   * hợp nhất có thứ tự "tra nhiều nhất" nhất quán.</p>
     *
   * <p><b>Chỉ trả v�� những gì còn tồn tại trong từ điển.</b> Từ đã bị xoá mềm
   * hoặc Kanji không có trong {@code writing_characters} → bỏ khỏi bảng
   * (hiện 1 dòng trống tên vô nghĩa còn hơn).</p>
   *
   * @param from ngày bắt đầu (inclusive; <b>null cùng {@code to} → toàn bộ lịch sử</b>)
   * @param to   ngày kết thúc (inclusive; null → hôm nay)
   * @param limit số dòng tối đa (null → {@link #DEFAULT_TOP_LOOKUPS})
   */
    /**
     * Bảng "Từ đã tra nhiều nhất" (Khoi 4).
     *
     * @param from ngày bắt đầu (inclusive). <b>null → xếp hạng toàn bộ lịch sử</b>
     * @param to   ngày kết thúc (inclusive). null → hôm nay
     * @param limit số dòng tối đa (null → 30)
     */
    @Transactional(readOnly = true)
    public List<TopLookupResponse> getTopLookups(LocalDate from, LocalDate to, Integer limit) {
        Long userId = currentUserService.getCurrentUserId();

        int max = (limit == null || limit <= 0) ? DEFAULT_TOP_LOOKUPS : Math.min(limit, MAX_TOP_LOOKUPS);

        // Khong truyen from/to → xep hang TOAN BO lich su, khong gioi han 30 ngay.
        List<Object[]> grouped;
        if (from == null && to == null) {
            grouped = studyEventRepository.countTopLookupsAllTime(userId, max);
        } else {
            LocalDate today = LocalDate.now(clock);
            LocalDate end = (to != null) ? to : today;
            LocalDate start = (from != null) ? from : end.minusDays(DEFAULT_RANGE_DAYS - 1);

            int days = (int) (ChronoUnit.DAYS.between(start, end) + 1);
            if (days < 1) {
                return List.of();
            }
            if (days > MAX_RANGE_DAYS) {
                start = end.minusDays(MAX_RANGE_DAYS - 1);
            }

            // Chuyen khoang ngay (theo Asia/Ho_Chi_Minh) sang moc Instant.
            // `end + 1 ngay` roi dung moc exclusive → tranh lech khi so sanh created_at.
            ZoneId zone = clock.getZone();
            Instant fromInstant = start.atStartOfDay(zone).toInstant();
            Instant toExclusive = end.plusDays(1).atStartOfDay(zone).toInstant();

            grouped = studyEventRepository.countTopLookupsInRange(
                    userId, fromInstant, toExclusive, max);
        }

        if (grouped == null || grouped.isEmpty()) {
            return List.of();
        }

        // Gom id theo từng loại để batch fetch (3 query thay vì N+1).
        Set<Long> vocabIds = new LinkedHashSet<>();
        Set<Long> grammarIds = new LinkedHashSet<>();
        Set<String> kanjiChars = new LinkedHashSet<>();
        for (Object[] row : grouped) {
            String type = (row[0] != null) ? row[0].toString() : null;
            if (LookupTargetType.VOCAB.name().equals(type) && row[1] instanceof Number id) {
                vocabIds.add(id.longValue());
            } else if (LookupTargetType.GRAMMAR.name().equals(type) && row[1] instanceof Number id) {
                grammarIds.add(id.longValue());
            } else if (LookupTargetType.KANJI.name().equals(type) && row[2] != null) {
                kanjiChars.add(row[2].toString());
            }
        }

        Map<Long, Object[]> vocabMap = new HashMap<>();
        if (!vocabIds.isEmpty()) {
            for (Object[] row : vocabularyRepository.findDisplayInfoByIds(vocabIds)) {
                if (row != null && row.length >= 6 && row[0] instanceof Number id) {
                    vocabMap.put(id.longValue(), row);
                }
            }
        }

        Map<Long, Object[]> grammarMap = new HashMap<>();
        if (!grammarIds.isEmpty()) {
            for (Object[] row : grammarPointRepository.findDisplayInfoByIds(grammarIds)) {
                if (row != null && row.length >= 5 && row[0] instanceof Number id) {
                    grammarMap.put(id.longValue(), row);
                }
            }
        }

        Map<String, Object[]> kanjiMap = new HashMap<>();
        if (!kanjiChars.isEmpty()) {
            for (Object[] row : writingCharacterRepository.findDisplayInfoByCharacters(kanjiChars)) {
                if (row != null && row.length >= 4 && row[0] != null) {
                    kanjiMap.put(row[0].toString(), row);
                }
            }
        }

        List<TopLookupResponse> result = new ArrayList<>(grouped.size());
        for (Object[] row : grouped) {
            if (row == null || row.length < 4) continue;
            String type = (row[0] != null) ? row[0].toString() : null;
            Long id = (row[1] instanceof Number n) ? n.longValue() : null;
            String sourceRef = (row[2] != null) ? row[2].toString() : null;
            int count = (int) toLong(row[3]);

            if (LookupTargetType.VOCAB.name().equals(type) && id != null) {
                Object[] v = vocabMap.get(id);
                if (v == null) continue; // từ đã bị xoá mềm → bỏ khỏi bảng
                result.add(TopLookupResponse.builder()
                        .targetType(type)
                        .targetId(id)
                        .label(asString(v[1]))
                        .subLabel(asString(v[2]))
                        .extraLabel(asString(v[3]))
                        .meaningVi(asString(v[4]))
                        .jlptLevel(asString(v[5]))
                        .lookupCount(count)
                        .build());
            } else if (LookupTargetType.GRAMMAR.name().equals(type) && id != null) {
                Object[] g = grammarMap.get(id);
                if (g == null) continue;
                result.add(TopLookupResponse.builder()
                        .targetType(type)
                        .targetId(id)
                        .label(asString(g[1]))
                        // "Cấu trúc" ngữ pháp rất dài → cắt bớt cho ô hiển thị.
                        .subLabel(truncate(asString(g[2]), 28))
                        .meaningVi(asString(g[3]))
                        .jlptLevel(asString(g[4]))
                        .lookupCount(count)
                        .build());
            } else if (LookupTargetType.KANJI.name().equals(type) && sourceRef != null) {
                Object[] k = kanjiMap.get(sourceRef);
                if (k == null) continue;
                result.add(TopLookupResponse.builder()
                        .targetType(type)
                        .kanjiCharacter(sourceRef)
                        .label(sourceRef)
                        .extraLabel(asString(k[1]))
                        .meaningVi(asString(k[2]))
                        .jlptLevel(asString(k[3]))
                        .lookupCount(count)
                        .build());
            }
        }
        return result;
    }

    // =====================================================================
    // Khối 5 — Leaderboard
    // =====================================================================

    /**
     * Lấy bảng xếp hạng tuần.
     *
     * <p>Luồng:</p>
     * <ol>
     *   <li>Validate + resolve {@code weekKey} (tuần tương lai → 400).</li>
     *   <li>{@code ZREVRANGE} top 5 → nếu có: đi qua <b>2 batch query</b>
     *       ({@code findWeekStatsByUserIds} + {@code findDisplayInfosByIds}) → dựng
     *       {@code entries} (hết N+1).</li>
     *   <li>Nếu rỗng <b>và tuần còn trong TTL</b> → {@code findAllScoresByWeek} từ DB →
     *       {@code rebuild} (có lock) → dựng entries từ kết quả DB.</li>
     *   <li>Nếu rỗng <b>và tuần ngoài TTL</b> → đọc thẳng DB, <b>không</b> ghi Redis
     *       (tránh hồi sinh key cũ).</li>
     *   <li>{@code totalPlayers} = {@code ZCARD} (Redis) hoặc
     *       {@code countUsersWithScoreInWeek} (DB).</li>
     * </ol>
     *
     * @param week tuần ISO {@code YYYY-Www} (null/blank → tuần hiện tại)
     * @return wrapper bảng xếp hạng
     */
    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(String week) {
        String weekKey = resolveWeekKey(week);
        LocalDate[] range = LeaderboardWeekUtil.weekRange(weekKey);

        // 1. Redis Sorted Set — top 5
        List<LeaderboardService.LeaderboardRow> top = leaderboardService.getTop(LEADERBOARD_LIMIT, weekKey);
        if (!top.isEmpty()) {
            log.debug("[Progress] leaderboard Redis HIT week={}", weekKey);
            return buildResponse(weekKey, range, top,
                    leaderboardService.getTotalPlayers(weekKey));
        }

        // 2. Redis miss
        log.debug("[Progress] leaderboard Redis MISS week={}", weekKey);

        boolean withinRetention = LeaderboardWeekUtil.isWithinRetention(
                weekKey, clock, LEADERBOARD_RETENTION_WEEKS);

        List<Object[]> dbScores = findAllScoresByWeek(range);

        if (withinRetention) {
            // Có thể xây lại ZSET — dùng lock chống cache stampede.
            if (leaderboardService.tryAcquireRebuildLock(weekKey)) {
                leaderboardService.rebuild(weekKey, toRows(dbScores));
            } else {
                // Thua lock → đọc DB và trả về luôn, KHÔNG ghi Redis
                // (tránh ghi đè kết quả của request thắng lock).
                log.debug("[Progress] rebuild lock busy, serve from DB week={}", weekKey);
            }
        } else {
            // Tuần ngoài TTL → đọc thẳng DB, không ghi lại Redis.
            log.debug("[Progress] week outside retention, serve from DB only week={}", weekKey);
        }

        List<LeaderboardService.LeaderboardRow> rows = toRows(dbScores);
        List<LeaderboardService.LeaderboardRow> limited = rows.size() > LEADERBOARD_LIMIT
                ? rows.subList(0, LEADERBOARD_LIMIT)
                : rows;

        Long totalPlayers = withinRetention
                ? leaderboardService.getTotalPlayers(weekKey)
                : countUsersWithScoreInWeek(range);

        return buildResponse(weekKey, range, limited, totalPlayers);
    }

    /**
     * Hạng cá nhân của user hiện tại trong tuần.
     *
     * <p>Nguồn: {@code ZSCORE} + {@code ZCOUNT(Range.open(score, +inf))} + {@code ZCARD}.
     * Redis lỗi/rỗng → fallback DB: {@code countUsersWithScoreGreaterThan} +
     * {@code findUserWeekScore} + {@code countUsersWithScoreInWeek}.</p>
     *
     * @param week tuần ISO {@code YYYY-Www} (null/blank → tuần hiện tại)
     * @return thông tin hạng cá nhân
     */
    @Transactional(readOnly = true)
    public MyRankResponse getMyRank(String week) {
        String weekKey = resolveWeekKey(week);
        LocalDate[] range = LeaderboardWeekUtil.weekRange(weekKey);
        Long userId = currentUserService.getCurrentUserId();

        Long score = leaderboardService.getScore(userId, weekKey);
        // Chỉ gọi ZCOUNT khi đã có điểm — tránh ZSCORE thừa lần nữa.
        Long rank = (score != null)
                ? leaderboardService.getRankByScore(userId, weekKey, score)
                : null;
        Long totalPlayers = leaderboardService.getTotalPlayers(weekKey);

        if (score == null || rank == null || totalPlayers == null) {
            // Fallback sang DB
            score = findUserWeekScore(userId, range);
            Long greater = countUsersWithScoreGreaterThan(range, score);
            rank = (greater == null) ? null : greater + 1;
            totalPlayers = countUsersWithScoreInWeek(range);
        }

        Object[] info = findDisplayInfo(userId);

        return MyRankResponse.builder()
                .rank(rank)
                .score(score)
                .totalPlayers(totalPlayers)
                .weekKey(weekKey)
                .userId(userId)
                .displayName(info != null && info[1] != null ? (String) info[1] : "User #" + userId)
                .avatarUrl(info != null ? (String) info[2] : null)
                .inTop(rank != null && rank <= LEADERBOARD_LIMIT)
                .build();
    }

    /**
     * Refresh bảng xếp hạng tuần.
     *
     * <p><b>Cooldown 30s</b> — lần gọi thứ 2 trong 30s trả HTTP 429.</p>
     *
     * <p><b>{@code hard=false} (mặc định) — chỉ "bù lỡ", KHÔNG sửa điểm giảm.</b> Vì
     * {@code ZADD GT} chỉ nâng điểm, rebuild thường <b>không sửa được</b> điểm đã bị
     * giảm trong DB. Mục đích thật của nút Reload thường là bù cho trường hợp
     * {@code syncUserScore} <b>bị lỡ</b> (Redis lỗi đúng lúc {@code afterCommit} → điểm
     * trong DB có nhưng ZSET thiếu).</p>
     *
     * <p><b>{@code hard=true} — xoá key rồi dựng lại từ đầu.</b> Dùng khi cần sửa
     * điểm giảm (sửa tay dữ liệu DB, xoá row {@code user_daily_stats}). Vì app không
     * có đường nào giảm điểm nên trường hợp này rất hiếm → là task riêng.</p>
     *
     * @param week tuần ISO {@code YYYY-Www} (null/blank → tuần hiện tại)
     * @param hard {@code true} → xoá key rồi dựng lại từ đầu
     * @return bảng xếp hạng sau refresh
     */
    @Transactional(readOnly = true)
    public LeaderboardResponse refreshLeaderboard(String week, boolean hard) {
        String weekKey = resolveWeekKey(week);
        LocalDate[] range = LeaderboardWeekUtil.weekRange(weekKey);

        // Cooldown — chống spam nút Reload
        if (!leaderboardService.acquireRefreshCooldown(weekKey)) {
            throw new AppException(ErrorCode.LEADERBOARD_REFRESH_TOO_FREQUENT);
        }

        List<Object[]> dbScores = findAllScoresByWeek(range);
        List<LeaderboardService.LeaderboardRow> rows = toRows(dbScores);

        if (hard) {
            // Xoá key rồi dựng lại từ đầu — lock phải được giữ xuyên suốt.
            if (leaderboardService.tryAcquireRebuildLock(weekKey)) {
                leaderboardService.hardRebuild(weekKey, rows);
            } else {
                log.debug("[Progress] hard rebuild lock busy week={}", weekKey);
            }
        } else {
            // Rebuild thường — ZADD GT nên không đè bản mới hơn bằng ảnh chụp DB cũ.
            if (leaderboardService.tryAcquireRebuildLock(weekKey)) {
                leaderboardService.rebuild(weekKey, rows);
            } else {
                log.debug("[Progress] rebuild lock busy week={}", weekKey);
            }
        }

        List<LeaderboardService.LeaderboardRow> limited = rows.size() > LEADERBOARD_LIMIT
                ? rows.subList(0, LEADERBOARD_LIMIT)
                : rows;

        return buildResponse(weekKey, range, limited, leaderboardService.getTotalPlayers(weekKey));
    }

    // =====================================================================
    // Leaderboard helpers
    // =====================================================================

    /**
     * Resolve & validate tuần.
     *
     * @param week tuần do FE truyền (null/blank → tuần hiện tại)
     * @return weekKey đã validate
     * @throws AppException {@code LEADERBOARD_INVALID_WEEK} nếu sai định dạng;
     *                      {@code LEADERBOARD_WEEK_IN_FUTURE} nếu tuần ở tương lai
     */
    private String resolveWeekKey(String week) {
        if (week == null || week.isBlank()) {
            return LeaderboardWeekUtil.currentWeekKey(clock);
        }
        // parseWeekKey tự validate định dạng (regex + khoảng 01-53)
        // rồi throw LEADERBOARD_INVALID_WEEK nếu sai.
        LeaderboardWeekUtil.WeekKey parsed = LeaderboardWeekUtil.parseWeekKey(week.trim());
        String weekKey = LeaderboardWeekUtil.format(
                parsed.weekBasedYear(), parsed.weekOfWeekBasedYear());

        if (LeaderboardWeekUtil.isFuture(weekKey, clock)) {
            throw new AppException(ErrorCode.LEADERBOARD_WEEK_IN_FUTURE);
        }
        return weekKey;
    }

    /**
     * Dựng {@link LeaderboardResponse} từ danh sách hàng đã xếp sẵn.
     *
     * <p><b>2 batch query</b> (không N+1): 1 query chỉ số tuần + 1 query thông tin user.</p>
     */
    private LeaderboardResponse buildResponse(String weekKey, LocalDate[] range,
                                             List<LeaderboardService.LeaderboardRow> rows,
                                             Long totalPlayers) {
        List<LeaderboardEntryResponse> entries = buildEntries(weekKey, range, rows);
        long total = totalPlayers != null ? totalPlayers : entries.size();
        String currentWeek = LeaderboardWeekUtil.currentWeekKey(clock);

        return LeaderboardResponse.builder()
                .weekKey(weekKey)
                .weekStart(range[0])
                .weekEnd(range[1])
                .isCurrentWeek(weekKey.equals(currentWeek))
                .totalPlayers(total)
                .entries(entries)
                .build();
    }

    /**
     * Dựng danh sách entries — gọi đúng 2 batch query.
     */
    private List<LeaderboardEntryResponse> buildEntries(String weekKey, LocalDate[] range,
                                                        List<LeaderboardService.LeaderboardRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = rows.stream().map(LeaderboardService.LeaderboardRow::userId).toList();

        // Batch 1 — chỉ số tuần (lookups, lookups_scored, lessons, xp)
        Map<Long, Object[]> statsMap = new HashMap<>();
        for (Object[] row : dailyStatsRepository.findWeekStatsByUserIds(userIds, range[0], range[1])) {
            if (row != null && row.length >= 5 && row[0] instanceof Number id) {
                statsMap.put(id.longValue(), row);
            }
        }

        // Batch 2 — displayName + avatarUrl
        Map<Long, Object[]> userInfoMap = new HashMap<>();
        for (Object[] row : userRepository.findDisplayInfosByIds(userIds)) {
            if (row != null && row.length >= 3 && row[0] instanceof Number id) {
                userInfoMap.put(id.longValue(), row);
            }
        }

        List<LeaderboardEntryResponse> result = new ArrayList<>(rows.size());
        int rank = 1;
        Long previousScore = null;

        for (int i = 0; i < rows.size(); i++) {
            LeaderboardService.LeaderboardRow row = rows.get(i);
            Long userId = row.userId();
            long score = row.score();

            // Cùng score → cùng rank (1, 1, 3)
            if (previousScore != null && previousScore != score) {
                rank = i + 1;
            }
            previousScore = score;

            Object[] stats = statsMap.get(userId);
            Object[] info = userInfoMap.get(userId);

            result.add(LeaderboardEntryResponse.builder()
                    .rank(rank)
                    .userId(userId)
                    .displayName(info != null && info[1] != null ? (String) info[1] : "User #" + userId)
                    .avatarUrl(info != null ? (String) info[2] : null)
                    .score(score)
                    .totalLookups(stats != null ? toLong(stats[1]) : 0L)
                    .totalXp(stats != null ? toLong(stats[4]) : 0L)
                    .weekKey(weekKey)
                    .build());
        }
        return result;
    }

    /** Điểm của 1 user trong tuần (DB). */
    private long findUserWeekScore(Long userId, LocalDate[] range) {
        int lookupPoints = appConfigService.getIntValue(CFG_LOOKUP_POINTS, DEFAULT_LOOKUP_POINTS);
        int lessonPoints = appConfigService.getIntValue(CFG_LESSON_POINTS, DEFAULT_LESSON_POINTS);
        return dailyStatsRepository.findUserWeekScore(
                userId, range[0], range[1], lookupPoints, lessonPoints);
    }

    /** Số user có điểm cao hơn {@code score} (DB) — tương đương ZCOUNT exclusive. */
    private long countUsersWithScoreGreaterThan(LocalDate[] range, long score) {
        int lookupPoints = appConfigService.getIntValue(CFG_LOOKUP_POINTS, DEFAULT_LOOKUP_POINTS);
        int lessonPoints = appConfigService.getIntValue(CFG_LESSON_POINTS, DEFAULT_LESSON_POINTS);
        return dailyStatsRepository.countUsersWithScoreGreaterThan(
                range[0], range[1], score, lookupPoints, lessonPoints);
    }

    /** Tổng số user có điểm trong tuần (DB) — tương đương ZCARD. */
    private long countUsersWithScoreInWeek(LocalDate[] range) {
        int lookupPoints = appConfigService.getIntValue(CFG_LOOKUP_POINTS, DEFAULT_LOOKUP_POINTS);
        int lessonPoints = appConfigService.getIntValue(CFG_LESSON_POINTS, DEFAULT_LESSON_POINTS);
        return dailyStatsRepository.countUsersWithScoreInWeek(
                range[0], range[1], lookupPoints, lessonPoints);
    }

    /** Toàn bộ điểm trong tuần từ DB (không LIMIT) — nguồn cho rebuild. */
    private List<Object[]> findAllScoresByWeek(LocalDate[] range) {
        int lookupPoints = appConfigService.getIntValue(CFG_LOOKUP_POINTS, DEFAULT_LOOKUP_POINTS);
        int lessonPoints = appConfigService.getIntValue(CFG_LESSON_POINTS, DEFAULT_LESSON_POINTS);
        List<Object[]> rows = dailyStatsRepository.findAllScoresByWeek(
                range[0], range[1], lookupPoints, lessonPoints);
        return rows == null ? List.of() : rows;
    }

    /** Chuyển rows {@code [userId, score]} từ DB sang {@link LeaderboardService.LeaderboardRow}, sắp xếp giảm dần. */
    private List<LeaderboardService.LeaderboardRow> toRows(List<Object[]> dbScores) {
        if (dbScores == null || dbScores.isEmpty()) {
            return List.of();
        }
        List<LeaderboardService.LeaderboardRow> rows = new ArrayList<>(dbScores.size());
        for (Object[] row : dbScores) {
            if (row == null || row.length < 2 || !(row[0] instanceof Number id)) {
                continue;
            }
            long score = toLong(row[1]);
            if (score <= 0) {
                continue;
            }
            rows.add(new LeaderboardService.LeaderboardRow(id.longValue(), score));
        }
        rows.sort((a, b) -> Long.compare(b.score(), a.score()));
        return Collections.unmodifiableList(rows);
    }

    /** Thông tin hiển thị của 1 user (displayName, avatarUrl). */
    private Object[] findDisplayInfo(Long userId) {
        if (userId == null) {
            return null;
        }
        List<Object[]> rows = userRepository.findDisplayInfosByIds(List.of(userId));
        return (rows == null || rows.isEmpty()) ? null : rows.get(0);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /**
     * Kết quả aggregate 1 request — gộp kỳ hiện tại + kỳ trước.
     *
     * <p>Tất cả field là {@code primitive} để tránh boxing không cần thiết
     * khi đọc ra để tính delta.</p>
     */
    private record Aggregate(long lookups, long studySeconds, long xpEarned,
                             int activeDays, int goalMetDays,
                             long prevLookups, long prevStudySeconds) {
    }

    /**
     * Aggregate gộp 2 kỳ liền kề bằng <b>1 query</b>.
     *
     * <p>Trước đây {@code aggregate()} chạy 2 lần × 2 query = 4 query.
     * Vì 2 kỳ là liền kề nhau, 1 lần scan {@code [prevFrom, to]} đủ cả 2.</p>
     */
    private Aggregate aggregateTwoPeriods(Long userId, LocalDate from, LocalDate to,
                                          LocalDate prevFrom) {
        List<Object[]> rows = dailyStatsRepository.aggregateTwoPeriods(userId, from, to, prevFrom);
        if (rows == null || rows.isEmpty()) {
            return new Aggregate(0, 0, 0, 0, 0, 0, 0);
        }
        Object[] row = rows.get(0);
        if (row == null || row.length < 7) {
            return new Aggregate(0, 0, 0, 0, 0, 0, 0);
        }
        return new Aggregate(
                toLong(row[0]),
                toLong(row[1]),
                toLong(row[2]),
                (int) toLong(row[3]),
                (int) toLong(row[4]),
                toLong(row[5]),
                toLong(row[6])
        );
    }

    private long toLong(Object o) {
        return (o instanceof Number n) ? n.longValue() : 0L;
    }

    /** Integer null-safe: {@code null} → 0. */
    private int nz(Integer o) {
        return (o != null) ? o : 0;
    }

    /** Object null-safe sang String: {@code null} → {@code null} (giữ nguyên cho optional). */
    private String asString(Object o) {
        return (o != null) ? o.toString() : null;
    }

    /** Cắt chuỗi dài kèm dấu "…" để vừa ô hiển thị (VD cấu trúc ngữ pháp). */
    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }

    /**
     * % thay đổi so với kỳ trước.
     * Nếu kỳ trước = 0 → null (không chia được) thay vì trả Infinity.
     */
    private Double deltaPct(long current, long previous) {
        if (previous == 0) {
            return (current == 0) ? 0.0 : null;
        }
        double pct = ((current - previous) * 100.0) / previous;
        return Math.round(pct * 10.0) / 10.0; // làm tròn 1 chữ số thập phân
    }
}
