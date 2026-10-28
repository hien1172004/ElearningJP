# Module Gamification — Tài liệu nghiệp vụ & cách xử lý trong code

> Phạm vi: `BE/elearningJP/src/main/java/com/jp/elearningjp/` (gồm phần `service/gamification/`, `service/progress/`, `service/study/`, `service/config/`, `entity/gamification/`) và các migration liên quan.
>
> Module này ghi lại **mọi hoạt động học tập của người dùng** rồi biến chúng thành: XP, streak, mục tiêu ngày, thống kê từ đã học và bảng xếp hạng tuần. Trên FE nó hiện ra ở trang **Tiến độ học tập** (`/progress`).
>
> Tài liệu này mô tả **đúng những gì source code hiện đang làm**, không phải lý thuyết chung. Mọi "suy luận" đã được đánh dấu rõ.

---

## Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Domain model — 4 bảng dữ liệu](#2-domain-model--4-bảng-dữ-liệu)
3. [Cấu hình động `app_config`](#3-cấu-hình-động-app_config)
4. [Múi giờ nghiệp vụ — `TimeConfig`](#4-múi-giờ-nghiệp-vụ--timeconfig)
5. [Luồng ghi — "điểm vào duy nhất"](#5-luồng-ghi--điểm-vào-duy-nhất)
6. [Lookup scoring — trần điểm ngày](#6-lookup-scoring--trần-điểm-ngày)
7. [Heartbeat — đo thời gian học](#7-heartbeat--đo-thời-gian-học)
8. [Bảng xếp hạng tuần — Redis Sorted Set](#8-bảng-xếp-hạng-tuần--redis-sorted-set)
9. [Đọc dữ liệu — trang Tiến độ](#9-đọc-dữ-liệu--trang-tiến-độ)
10. [API](#10-api)
11. [Tóm tắt các quyết định thiết kế đắt giá](#11-tóm-tắt-các-quyết-định-thiết-kế-đắt-giá)
12. [Điểm chưa xác định / hạn chế](#12-điểm-chưa-xác-định--hạn-chế)
    - [12.1 Module Thông báo (in-app notifications)](#121-module-thông-báo-in-app-notifications)

---

## 1. Tổng quan kiến trúc

### 1.1 Phạm vi trách nhiệm

Module Gamification **không** tự biết user đang làm gì. Nó chỉ nhận đầu vào từ 3 nơi khác gọi vào:

| Nguồn phát sinh | Điểm gọi vào module | API/event |
|---|---|---|
| Người dùng tra 1 từ trong từ điển | `DictionaryController.recordLookup` | `POST /api/v1/dictionary/lookup` |
| Người dùng hoàn thành 1 bài học | `LessonCompletedEvent` | (nội bộ, publish từ `curriculum`) |
| Tab học đang mở, đồng hồ chạy | `StudyHeartbeatController.record` | `POST /api/v1/study/heartbeat` |

### 1.2 Sơ đồ package

```
service/gamification/          ← LÕI: ghi dữ liệu
  StudyTrackerService            Điểm gọi DUY NHẤT từ controller/event
  DailyStatsService               user_daily_stats (bảng tổng hợp theo ngày)
  StudyEventService               study_events (append-only, analytics)
  XpService                       user_gamification + xp_logs
  StreakService                   Tính chuỗi ngày
  listener/
    LessonCompletedGamificationListener   Nghe LessonCompletedEvent (AFTER_COMMIT)

service/study/
  StudyHeartbeatService           Đo thời gian học + chống đếm trùng nhiều tab

service/progress/               ← LÕI: đọc dữ liệu (read-only)
  ProgressService                 Tổng hợp cho trang Tiến độ
  LeaderboardService              Redis Sorted Set (index dẫn xuất từ DB)
  LeaderboardScoreReader          Đọc điểm tuyệt đối (REQUIRES_NEW)
  LeaderboardWeekUtil             Chuẩn tuần ISO 8601
  ProgressMonthUtil               Parse "yyyy-MM"
  ProgressCacheService            Cache Redis + generation token

service/config/
  AppConfigService                Đọc app_config, cache 5 phút

entity/gamification/
  UserDailyStats, UserDailyStatsId, UserGamification, StudyEvent, XpLog,
  Badge, UserBadge  (2 entity cuối chưa dùng — xem §12)
  Notification      (đã có module riêng — xem §12.1)

repository/gamification/
  UserDailyStatsRepository, StudyEventRepository, UserGamificationRepository,
  XpLogRepository, NotificationRepository

service/notification/               Module thông báo (§12.1)
  NotificationService               Tạo (REQUIRES_NEW) + đọc + đánh dấu đã đọc
  NotificationPublisher             Đẩy STOMP tới /user/queue/notifications
  NotificationScheduler             Job SRS_DUE (08:00) + STREAK_WARNING (20:30)

config/TimeConfig                 Bean Clock cố định Asia/Ho_Chi_Minh
config/WebSocketConfig            STOMP endpoint /ws (broker /queue, /topic)
config/SchedulerLockConfig        ShedLock cho job đa instance
```

### 1.3 Luồng tổng quát

```
   ┌──────────────────┐
   │ tra từ điển      │────┐
   ├──────────────────┤    │
   │ hoàn thành bài    │────┼──► StudyTrackerService ──┬──► user_daily_stats  (tổng hợp theo ngày)
   ├──────────────────┤    │   (điểm gọi DUY NHẤT)     ├──► study_events      (append-only)
   │ heartbeat        │────┤                          ├──► user_gamification (XP, streak)
   └──────────────────┘    │                          └──► xp_logs           (audit từng lần cộng XP)
                           │
                           └──► LeaderboardService.syncUserScore()   ← Redis ZSET (sau commit)
                                     key: leaderboard:weekly:v3:{YYYY-Www}

   ─── Sau đó ────────────────────────────────────────────
   ProgressService đọc lại các bảng trên để dựng trang /progress
```

**Nguyên tắc then chốt:** `StudyTrackerService` là **điểm gọi duy nhất** từ bên ngoài. Nó điều phối 3 nhánh ghi và mỗi nhánh bọc `try/catch` riêng — analytics **không bao giờ được làm hỏng UX**.

---

## 2. Domain model — 4 bảng dữ liệu

### 2.1 Sơ đồ quan hệ

```
┌──────────────┐                    ┌─────────────────────────┐
│    users     │ 1                N │   user_daily_stats      │   (PK: user_id + stat_date)
└──────┬───────┘   ───────────────► │  lookups                │  ← TỔNG số lần tra (không trần)
       │                           │  lookups_scored         │  ← số lần tra đã tính XP (có trần)
       │ 1                         │  new_items_learned      │  ← số MỤC MỚI (dùng cho leaderboard)
       │                           │  lessons_completed      │
       │                           │  study_seconds          │
       ▼                           │  xp_earned              │
┌──────────────────────┐           │  goal_words / goal_met  │
│  user_gamification   │           │  last_lookup_at         │
│  total_xp            │           └─────────────────────────┘
│  current_streak      │
│  longest_streak      │           ┌─────────────────────────┐
│  last_activity_date  │           │    study_events         │  (append-only, id BIGSERIAL)
└──────────────────────┘           │  event_type             │
                                   │  target_type/target_id  │
┌──────────────────────┐           │  source_ref             │
│      xp_logs         │           │  xp_awarded             │
│  amount              │           └─────────────────────────┘
│  source_type         │
│  source_ref_id       │           ┌─────────────────────────┐
└──────────────────────┘           │      app_config        │  (key-value toàn cục)
                                   └─────────────────────────┘
```

### 2.2 `user_daily_stats` — bảng tổng hợp theo ngày

Khóa chính composite `UserDailyStatsId(userId, statDate)`. Đây là **nguồn sự thật** cho mọi thống kê.

| Cột | Ý nghĩa | Ghi bởi |
|---|---|---|
| `lookups` | Tổng số lần tra từ trong ngày. **Không trần.** | `upsertLookup` |
| `lookups_scored` | Số lần tra **đã được tính XP** (trần theo ngày). Không còn dùng cho leaderboard. | `upsertLookup` |
| `new_items_learned` | Số **MỤC MỚI** học lần đầu trong ngày (= "từ đã học"). Nguồn chống trùng: `user_learned_items`. | `upsertLookup` |
| `lessons_completed` | Số bài học hoàn thành trong ngày. | `incrementLessonsCompleted` |
| `study_seconds` | Tổng giây học trong ngày (từ heartbeat). | `addStudySeconds` |
| `xp_earned` | XP kiếm được trong ngày. | cả 3 upsert |
| `goal_words` | Mục tiêu từ/ngày (mặc định 5). | insert lần đầu |
| `goal_met` | Đã đạt mục tiêu ngày. | re-evaluate mỗi upsert |
| `last_lookup_at` | Thời điểm tra từ gần nhất (debug). | `upsertLookup` |

**Vì sao tách `lookups` / `lookups_scored` / `new_items_learned`?**

- `lookups` = "tôi đã tra bao nhiêu lần hôm nay" → **hiển thị cho user, đầy đủ, không cắt**.
- `lookups_scored` = "hôm nay tôi được cộng XP cho bao nhiêu lượt tra" → **có trần** (`lookup_ceiling`), dùng để giới hạn XP khi tra từ.
- `new_items_learned` = "hôm nay tôi học được bao nhiêu MỤC MỚI" → dùng cho **leaderboard** (mỗi mục chỉ tính 1 lần, không thể spam tra lại).

Chi tiết cơ chế ở §6 và §8.1.

### 2.3 `user_gamification` — trạng thái tích lũy (1 dòng / user)

| Cột | Ý nghĩa |
|---|---|
| `user_id` | PK, đồng thời là khóa ngoại tới `users.id` |
| `total_xp` | Tổng XP mọi thời đại |
| `current_streak` | Chuỗi ngày học liên tiếp (cache) |
| `longest_streak` | Chuỗi dài nhất từng có (cache) |
| `last_activity_date` | Ngày học gần nhất (dùng cho streak) |

> **Ghi chú kỹ thuật quan trọng:** entity này **không dùng `@MapsId`**. Cột join khai báo `insertable = false, updatable = false`, giá trị được ghi từ field `userId`. Lý do: `@MapsId` sẽ reset id về `null` khi `merge()` entity chưa tồn tại trong DB → lỗi "null identifier".

### 2.4 `study_events` — append-only, dùng cho analytics

Bảng **chỉ thêm, không sửa, không xoá**. Không kế thừa `SoftDeletableEntity` (không có `deleted`/`version`).

| Cột | Ý nghĩa |
|---|---|
| `id` | BIGSERIAL |
| `event_type` | `LOOKUP` \| `LESSON_COMPLETE` \| `QUIZ_SUBMIT` \| `HEARTBEAT` |
| `target_type` | `VOCAB` \| `KANJI` \| `GRAMMAR` \| NULL (lesson/heartbeat) |
| `target_id` | ID đối tượng. **KANJI thì NULL** — vì ký tự Hán là chuỗi, không phải số |
| `source_ref` | Chuỗi tham chiếu (ký tự kanji, hoặc "PASSED"/"FAILED" cho quiz) |
| `xp_awarded` | XP ghi kèm trong event (số liệu lịch sử) |
| `created_at` | Timestamp, index để lọc theo kỳ |

**Mục đích duy nhất hiện tại:** tính bảng **"Từ đã tra nhiều nhất"** (Top lookups) bằng `GROUP BY target ORDER BY COUNT(*) DESC`.

> **Heartbeat không ghi vào đây.** Xem §7.3.

### 2.5 `xp_logs` — audit từng lần cộng XP

Bảng này giữ **lịch sử chi tiết**, `user_gamification.total_xp` chỉ là tổng. Nhờ vậy có thể trả lời "XP này đến từ đâu".

> Migration `V25` phải sửa cả bảng `xp_logs` vì `V1` tạo sai schema (cột `source` kiểu VARCHAR thay vì enum + thiếu cột soft-delete). Xem file `V25__add_progress_columns.sql`.

---

## 3. Cấu hình động `app_config`

Bảng key-value toàn cục cho các tham số nghiệp vụ thay đổi được mà **không cần deploy lại**.

| Key | Mặc định | Ý nghĩa |
|---|---|---|
| `lookup_ceiling` | 20 | Số lần tra tối đa được tính điểm trong 1 ngày |
| `lookup_xp` | 2 | XP nhận khi tra từ (chưa vượt trần) |
| `lesson_xp` | 10 | XP nhận khi hoàn thành bài học |
| `min_study_seconds` | 300 | Số giây học tối thiểu để tính `goal_met` (5 phút) |
| `lookup_points` | 1 | Điểm leaderboard cũ cho 1 lần tra (**không còn dùng** sau V37) |
| `learned_item_points` | 1 | Điểm leaderboard cho 1 **từ MỚI đã học** (V37) |
| `lesson_points` | 1 | Điểm leaderboard cho 1 bài học |

**Cơ chế:** `AppConfigService` nạp toàn bộ bảng vào `AtomicReference<Map>` lúc khởi động (`@PostConstruct`) và refresh mỗi **5 phút** (`@Scheduled(fixedRate = 300_000)`).

- Nhờ vậy `getIntValue()` không đánh đổi 1 query DB mỗi lần tra từ.
- Nếu reload lỗi → **giữ cache cũ**, chỉ log warn.
- Đổi giá trị trong DB có hiệu lực trong ≤ 5 phút, không cần restart.

---

## 4. Múi giờ nghiệp vụ — `TimeConfig`

```12:40:BE/elearningJP/src/main/java/com/jp/elearningjp/config/TimeConfig.java
@Configuration
public class TimeConfig {

    /** Múi giờ nghiệp vụ của toàn bộ module gamification. */
    public static final String APP_ZONE = "Asia/Ho_Chi_Minh";

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(APP_ZONE));
    }
}
```

**Vấn đề giải quyết:** múi giờ mặc định của JVM phụ thuộc môi trường chạy — máy dev Windows là GMT+7, còn container Docker là UTC. Cùng một commit sẽ ghi `stat_date` khác nhau giữa dev và deploy → lệch ngày, lệch tuần, lệch chuỗi streak.

**Quy tắc áp dụng:** mọi service trong module đều inject `Clock` và gọi `LocalDate.now(clock)` — **không bao giờ** gọi `LocalDate.now()` trần.

Ngoại lệ: các timestamp tuyệt đối kỹ thuật (`Instant.now()` cho `created_at`, SRS) không phụ thuộc múi giờ nên không cần đổi.

---

## 5. Luồng ghi — "điểm vào duy nhất"

### 5.1 `StudyTrackerService` — orchestrator

Đây là lớp mà mọi thay đổi về gamification đều phải đi qua. Ba method public:

#### `recordLookup(userId, targetType, targetId, sourceRef)`

Chạy 4 bước, **mỗi bước bọc try/catch riêng**:

```58:87:BE/elearningJP/src/main/java/com/jp/elearningjp/service/gamification/StudyTrackerService.java
    public UserDailyStats recordLookup(Long userId, LookupTargetType targetType, Long targetId, String sourceRef) {
        // 1. Ghi study_events - moi lan tra deu ghi (khong phu thuoc tran)
        try {
            studyEventService.logLookup(userId, targetType, targetId, sourceRef, 0);
        } catch (Exception ex) {
            log.warn("[StudyTracker] studyEvents.logLookup failed for user {}: {}", userId, ex.getMessage());
        }

        // 2. Daily stats - upsert nguyen tu, tra ve scored flag
        DailyStatsService.LookupResult result;
        try {
            result = dailyStatsService.recordLookup(userId, xpService.getLookupXp());
        } catch (Exception ex) {
            log.warn("[StudyTracker] dailyStats.recordLookup failed for user {}: {}", userId, ex.getMessage());
            return null;
        }

        // 3. Chi cong XP + ZINCRBY khi duoc tinh diem (chua vuot tran)
        if (result != null && result.scored()) {
            try {
                xpService.awardXp(userId, result.stats().getXpEarned(), XpSource.DAILY_TASK, targetId);
                xpService.touchActivity(userId);
            } catch (Exception ex) {
                log.warn("[StudyTracker] awardXp failed for user {}: {}", userId, ex.getMessage());
            }
        }

        // 4. Vo hieu hoa cache Progress (generation bump) sau khi commit
        invalidateProgressCache(userId);

        return result != null ? result.stats() : null;
    }
```

> **Lưu ý:** đoạn code trên là bản **rút gọn** (bỏ qua bước đăng ký `user_learned_items` và bước sync leaderboard khi `isNew == 1`). Xem `StudyTrackerService` hiện tại để có bản đầy đủ.

Điểm quan trọng: **bước 1 ghi `study_events` không bị ảnh hưởng bởi trần điểm**. Nghĩa là dù đã vượt `lookup_ceiling`, thống kê "từ nào tôi tra nhiều nhất" vẫn phản ánh đúng thói quen thật.

#### `recordLessonComplete(userId, lessonId, courseId, xpReward)`

```108:114:BE/elearningJP/src/main/java/com/jp/elearningjp/service/gamification/StudyTrackerService.java
        safeStep("lessonDailyStats", () ->
                dailyStatsService.recordLessonComplete(userId, xp));

        safeStep("lessonXp", () -> {
            if (xp > 0) {
                xpService.awardXp(userId, xp, XpSource.LESSON_COMPLETE, lessonId);
            }
            xpService.touchActivity(userId);
        });
```

**THỨ TỰ CỰC KỲ QUAN TRỌNG** (đã có bug thật, đã sửa): `recordLessonComplete` (daily stats) phải chạy **TRƯỚC** `awardXp`.

Lý do: `awardXp` → `afterCommit` → `syncUserScore` đọc điểm tuyệt đối từ DB. Nếu `awardXp` chạy trước, cột `lessons_completed` chưa được ghi → `syncUserScore` đọc thiếu điểm bài học → Redis ZSET lệch với DB. Sau khi đảo thứ tự: daily stats đã COMMIT (`REQUIRES_NEW`) → rồi `awardXp` → `afterCommit` đọc DB thấy đủ.

### 5.2 `DailyStatsService` — chống race bằng native upsert

Toàn bộ thay đổi lên `user_daily_stats` đều dùng mẫu `INSERT ... ON CONFLICT DO UPDATE` với điều kiện `WHERE` đặt trong SQL, chạy `@Transactional(propagation = REQUIRES_NEW)`.

Ví dụ `upsertLookup` (`UserDailyStatsRepository`):

```179:205:BE/elearningJP/src/main/java/com/jp/elearningjp/repository/gamification/UserDailyStatsRepository.java
    @Query(value = """
            INSERT INTO user_daily_stats (user_id, stat_date, lookups, lookups_scored, xp_earned, goal_words, goal_met, lessons_completed)
            VALUES (:userId, :statDate, 1, 1, :xpEarned, 5, (1 >= 5), 0)
            ON CONFLICT (user_id, stat_date) DO UPDATE
            SET
                lookups         = user_daily_stats.lookups + 1,
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
                goal_met       = ...
            """, nativeQuery = true)
```

Ba điều được xử lý ở đây:

1. **Chống race:** user tra từ ở nhiều tab/nhiều request cùng lúc → upsert nguyên tử, không cần khoá.
2. **Tính `scored` mà không cần đọc trước:** sau upsert, service đọc `lookups_scored` một lần nữa; nếu tăng so với lần đọc trước → `scored = true`.
3. **`REQUIRES_NEW`:** thay đổi daily stats độc lập với transaction gốc, không bị rollback khi phần khác của hoạt động thất bại.

### 5.3 `XpService` — cộng XP + đồng bộ leaderboard

```93:121:BE/elearningJP/src/main/java/com/jp/elearningjp/service/gamification/XpService.java
    @Transactional
    public void awardXp(Long userId, int amount, XpSource source, Long sourceRefId) {
        if (amount <= 0) {
            return;
        }
        UserGamification gamification = getOrCreate(userId);
        gamification.setTotalXp(gamification.getTotalXp() + amount);
        gamificationRepository.save(gamification);

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            xpLogRepository.save(XpLog.builder()
                    .user(user)
                    .amount(amount)
                    .sourceType(source)
                    .sourceRefId(sourceRefId)
                    .build());
        }

        if (leaderboardPoints(source) > 0) {
            final String weekKey = LeaderboardWeekUtil.currentWeekKey(clock);
            registerAfterCommit(() -> leaderboardService.syncUserScore(userId, weekKey));
        }
    }
```

Ba quyết định đáng chú ý:

- **`getOrCreate`**: nếu user chưa có dòng `user_gamification` (user mới đăng ký), tự tạo với XP = 0 → không cần migration backfill.
- **Chỉ gọi Redis sau commit**: `registerAfterCommit` đăng ký callback `afterCommit`. Nếu transaction rollback → **không** cộng điểm sai.
- **Chỉ nguồn XP liên quan leaderboard mới gọi Redis**: `LESSON_COMPLETE` và `DAILY_TASK`; các nguồn khác (`EXAM`, `BADGE`…) bỏ qua để không query DB thừa.

### 5.4 `StreakService` — chuỗi ngày học

Hai kiểu tính, dùng cho hai mục đích khác nhau:

**a) `updateStreak(gamification)`** — cập nhật dần vào `user_gamification` khi user có hoạt động:

| Tình huống | Kết quả |
|---|---|
| Chưa có `lastActivityDate` | streak = 1 |
| `lastActivityDate == hôm qua` | streak += 1 |
| `lastActivityDate == hôm nay` | giữ nguyên (idempotent) |
| Cách hôm nay > 1 ngày | streak = 1 (đứt chuỗi) |

`longest_streak` được cập nhật bằng `Math.max(longest, current)`.

**b) `computeCurrentStreakFromDesc(activeDatesDesc, today)`** — tính lại từ danh sách ngày active (đã sắp xếp **giảm dần**):

- Chuỗi chỉ còn "sống" nếu ngày mới nhất là **hôm nay hoặc hôm qua**. Hôm nay chưa học nhưng hôm qua có → vẫn giữ chuỗi (quen thuộc kiểu Duolingo).
- Duyệt một lần danh sách, dừng ngay khi gặp ngày không liên tiếp.

> **Vì sao không dựng `HashSet` toàn bộ lịch sử?** Cách cũ tải hết ngày active về RAM rồi set-lookup (O(n) bộ nhớ). Cách này chỉ duyệt danh sách đã tăng dần và dừng sớm — trung bình chỉ vài chục ngày.

### 5.5 `LessonCompletedGamificationListener` — event sau commit

```29:39:BE/elearningJP/src/main/java/com/jp/elearningjp/service/gamification/listener/LessonCompletedGamificationListener.java
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLessonCompleted(LessonCompletedEvent event) {
        try {
            studyTrackerService.recordLessonComplete(
                    event.getUserId(), event.getLessonId(), event.getCourseId(),
                    xpService.getLessonXp());
        } catch (Exception ex) {
            log.warn("[Gamification] lesson complete XP failed for user {}: {}",
                    event.getUserId(), ex.getMessage());
        }
    }
```

`AFTER_COMMIT` + `try/catch` ⇒ bài học bị rollback thì không cộng XP, và lỗi gamification không làm fail request `POST /lessons/{id}/complete`.

---

## 6. Lookup scoring — trần điểm ngày

### 6.1 Vấn đề

Nếu XP/điểm leaderboard cộng mỗi lần tra không giới hạn, hành vi bất thường (spam tra cùng 1 từ 100 lần, hoặc script gọi API) sẽ chiếm sạch bảng xếp hạng.

### 6.2 Cách xử lý hai tầng

**Tầng 1 — tính XP (có trần).** `lookups_scored` chỉ tăng khi `< lookup_ceiling`. `xp_earned` trong `user_daily_stats` cũng chỉ tăng tương ứng. Sau khi vượt trần: `lookups` vẫn tăng, `lookups_scored` và `xp_earned` đứng yên.

> **Leaderboard không còn phụ thuộc trần này.** Điểm xếp hạng dùng `new_items_learned` — trần `lookup_ceiling` chỉ còn giới hạn **XP**, không giới hạn điểm leaderboard.

**Tầng 2 — ghi log (không trần).** `study_events` ghi **mọi lần tra**, không quan tâm trần. Nhờ vậy:
- Thống kê hiển thị cho user → đầy đủ, trung thực.
- Leaderboard → không thể bị spam (vì chỉ đếm mục mới).

### 6.3 Cờ `scored` và `isNew` điều khiển gì

`DailyStatsService.recordLookup` trả về record:

```116:118:BE/elearningJP/src/main/java/com/jp/elearningjp/service/gamification/DailyStatsService.java
    public record LookupResult(UserDailyStats stats, boolean scored) {}
```

- `scored` (do `lookups_scored` tăng? tức chưa vượt `lookup_ceiling`) → quyết định **cộng XP** + streak.
- `isNew` (do `LearnedItemService.register` trả true?) → quyết định **tăng `new_items_learned`** và **sync điểm leaderboard Redis**.

`StudyTrackerService` chỉ sync `syncLeaderboardAfterCommit` khi `isNew == 1` — tức mỗi lần học **từ mới** (không phụ thuộc trần). Trước đó có code gọi `ZINCRBY` trực tiếp; hiện đã thay bằng cách đọc điểm tuyệt đối + `ZADD GT` (xem §8.3) vì `ZINCRBY` không idempotent.

---

## 7. Heartbeat — đo thời gian học

### 7.1 Vì sao cần đo

`study_seconds` là một trong ba điều kiện làm `goal_met = true`. Nhưng "ngồi mở tab cả ngày không học" cũng phải bị loại — nếu không, chỉ cần mở bài học và để đó là đủ đạt mục tiêu ngày.

### 7.2 Cơ chế chống đếm sai — 2 tầng (FE + BE)

**Tầng FE — `useStudyTimer` hook:**

| Vấn đề | Cách xử lý |
|---|---|
| Mở 3 tab cùng lesson → nhân 3 | `BroadcastChannel` bầu **1 tab leader**, chỉ leader gửi heartbeat |
| Mở tab rồi đi pha cà phê | Idle detection 60s (`mousedown`/`keydown`/`scroll`/...) |
| Tab bị ẩn | `document.visibilityState !== 'visible'` → không gửi |
| Đóng tab mất dữ liệu | `fetch keepalive` trong `pagehide` |
| Điều hướng SPA (Dictionary → Progress) | Gửi nốt trong `cleanup()` của effect |

Chu kỳ gửi 30s, số giây gửi = khoảng thời gian từ lần gửi trước, clamp về `[1, 60]`.

**Tầng BE — `StudyHeartbeatService`:** leader lock trong Redis, phòng trường hợp cơ chế FE bị vượt (2 thiết bị khác nhau, 2 browser profile...).

```82:105:BE/elearningJP/src/main/java/com/jp/elearningjp/service/study/StudyHeartbeatService.java
    private boolean tryAcquireLeader(String key, String sessionId) {
        try {
            Object existing = redisTemplate.opsForValue().get(key);
            if (existing == null) {
                Boolean ok = redisTemplate.opsForValue().setIfAbsent(
                        key, sessionId, Duration.ofSeconds(LEADER_TTL_SECONDS));
                return Boolean.TRUE.equals(ok);
            }
            if (sessionId.equals(String.valueOf(existing))) {
                redisTemplate.expire(key, Duration.ofSeconds(LEADER_TTL_SECONDS));
                return true;
            }
            return false;
        } catch (Exception ex) {
            log.warn("[StudyHeartbeat] redis leader check fail, fallback accept: {}", ex.getMessage());
            return true;
        }
    }
```

- Key: `study:hb:{userId}`, TTL **90s** (= 3 × chu kỳ heartbeat 30s).
- Trùng `sessionId` → refresh TTL, ghi nhận.
- Khác `sessionId` → thử `SETNX`; nếu fail thì tab khác đang giữ → **từ chối**.
- Redis lỗi → **fallback accept** (best effort, không chặn user).

**Cap an toàn:** tối đa **16 giờ / ngày**, tính lại từ giá trị `study_seconds` đã lưu trước khi cộng.

### 7.3 Heartbeat không ghi XP, không ghi `study_events`

```45:55:BE/elearningJP/src/main/java/com/jp/elearningjp/service/gamification/StudyEventService.java
    public void logHeartbeat(Long userId, int seconds) {
        if (seconds <= 0) {
            return;
        }
        try {
            log(StudyEventType.HEARTBEAT, userId, null, null, null, 0);
```

- **Không cộng XP** — thời gian học chỉ là điều kiện phụ, không phải nguồn XP.
- **Không ghi row** — heartbeat 30s/lần × nhiều user sẽ làm phình `study_events` vô ích. Method này gần như không được gọi; thời gian đã được ghi trực tiếp vào `user_daily_stats.study_seconds`.

---

## 8. Bảng xếp hạng tuần — Redis Sorted Set

### 8.1 Công thức điểm

```
score = new_items_learned * learned_item_points + lessons_completed * lesson_points
         (mặc định 1)                     (mặc định 1)
```

Dùng `new_items_learned` (**số MỤC MỚI học lần đầu**, nguồn `user_learned_items`) thay cho `lookups_scored`:

- Mỗi mục chỉ tính **1 lần** (UNIQUE constraint) → không thể cày điểm bằng cách tra đi tra lại cùng một từ.
- **Không bị trần theo ngày** — khác `lookups_scored` bị `lookup_ceiling` cắt ở 20/ngày.
- `lookups_scored` vẫn được ghi (để giới hạn XP theo ngày) nhưng **không còn** tham gia tính điểm leaderboard.

> Đổi ngữ nghĩa score → Redis key bump `v2` → **`v3`** (`LeaderboardService.KEY_PREFIX`). Key v2 tự hết TTL.

### 8.2 Chuẩn tuần — ISO 8601

Key tuần theo dạng `YYYY-Www`, **tuần bắt đầu thứ Hai** (`LeaderboardWeekUtil`). Ví dụ tuần hiện tại: `2026-W42`.

- Sai định dạng → `LEADERBOARD_INVALID_WEEK` (HTTP 400)
- Tuần tương lai → `LEADERBOARD_WEEK_IN_FUTURE` (HTTP 400)

### 8.3 Redis là **index dẫn xuất từ DB**, không phải nguồn sự thật

Key: `leaderboard:weekly:v3:{YYYY-Www}`, TTL **8 tuần (56 ngày)**.

> **Vì sao có `v3` trong key?** `v2` ghi điểm theo công thức cũ (`lookups_scored`). Nay điểm đổi sang `new_items_learned` → bump `v3` để không đọc nhầm điểm cũ. (`v1` → `v2` trước đó là do đổi encoding member từ Jackson default typing sang chuỗi thuần.)

**Ghi luôn dùng `ZADD ... GT`** (chỉ ghi khi score mới **lớn hơn** score đang có):

```159:166:BE/elearningJP/src/main/java/com/jp/elearningjp/service/progress/LeaderboardService.java
    private boolean zAddGt(String redisKey, Long userId, long score) {
        Boolean result = redisTemplate.execute((RedisCallback<Boolean>) conn ->
                conn.zSetCommands().zAdd(
                        bytes(redisKey),
                        score,
                        bytes(userId.toString()),
                        RedisZSetCommands.ZAddArgs.empty().gt()));
        return Boolean.TRUE.equals(result);
    }
```

`GT` chặn được 3 kịch bản sai mà `ZINCRBY`/`ZADD` thường sẽ sai:

1. **Hai transaction cùng user ghi sai thứ tự.** T1 đọc score = 10, T2 đọc 11 và ghi trước, T1 ghi sau → `ZADD` thường ghi lùi về 10, lệch DB. `GT` chặn.
2. **Rebuild dùng ảnh chụp DB cũ.** Rebuild đọc DB lúc T0, T1 commit tăng điểm và `syncUserScore` ghi score mới; rebuild sau đó `ZADD` ảnh chụp cũ sẽ đè mất. `GT` chặn.
3. **`RENAME` không bảo toàn increment.** Đã loại bỏ RENAME hoàn toàn.

**Hệ quả phải chấp nhận:** vì `GT` chỉ cho tăng, nếu DB bị **sửa điểm xuống** (xoá row, sửa tay dữ liệu) thì ZSET không tự sửa → phải gọi `hardRebuild()` (xoá key rồi dựng lại từ đầu).

**Vì sao `syncUserScore` thay cho `incrementScore`?** Đọc điểm tuyệt đối của user trong tuần từ DB → `ZADD GT`. Idempotent: gọi 2 lần cho cùng 1 khoảnh khắc vẫn cho cùng kết quả.

**Điều kiện bắt buộc:** `syncUserScore` phải chạy **sau commit** (`registerAfterCommit`). Lúc đó transaction gốc đã commit nhưng connection vẫn bind trên thread → query đọc score chạy trong `REQUIRES_NEW` (class riêng `LeaderboardScoreReader`) để lấy connection mới, snapshot mới, không dính dữ liệu cũ.

### 8.4 Đọc hạng — `ZCOUNT` exclusive

```240:251:BE/elearningJP/src/main/java/com/jp/elearningjp/service/progress/LeaderboardService.java
    private Long countGreaterThan(String redisKey, double score) {
        return redisTemplate.execute((RedisCallback<Long>) conn ->
                conn.zSetCommands().zCount(
                        bytes(redisKey),
                        Range.open(score, Double.POSITIVE_INFINITY)));
    }
```

Hạng = `ZCOUNT (score, +inf) + 1` → **cùng điểm thì cùng hạng** (1, 1, 3).

**Không dùng `ZREVRANK`** vì Redis xếp hạng khác nhau cho member đồng điểm.

**Không dùng `ZSetOperations.count(k, min, max)`** vì nó map sang `ZCOUNT key min max` — **inclusive cả hai đầu** → đếm cả member **cùng score** → hạng sai.

> ⚠️ Cú pháp exclusive range `(10` chỉ có từ **Redis 6.2**. Đang dùng `redis:7-alpine` → OK. Redis bản cũ sẽ parse lỗi → mất hạng.

### 8.5 Chống cache stampede + cooldown

| Lock | Key | TTL | Mục đích |
|---|---|---|---|
| Rebuild lock | `leaderboard:rebuild:lock:{week}` | 30s | Nhiều request thấy ZSET rỗng cùng chạy rebuild |
| Refresh cooldown | `leaderboard:refresh:cd:{week}` | 30s | Chống spam nút "Reload" → HTTP 429 |

Request **thua** rebuild lock → **đọc DB và trả về luôn, không ghi Redis** (tránh ghi đè kết quả của request thắng lock).

Redis lỗi → `tryAcquireRebuildLock` trả `true` (coi như không có khoá) → không chặn user vì lỗi hạ tầng.

### 8.6 Vì sao 2 query riêng cho `rebuild`

`findTopUsersByWeek` có `LIMIT` → chỉ trả top N. Nếu dùng nó để rebuild thì `ZCARD` (totalPlayers) sẽ khớp sai với số user thật và các user ngoài top N sẽ mất hạng. Vì vậy có query riêng `findAllScoresByWeek` — **không LIMIT**, chỉ trả user có `score > 0` (`HAVING`) để không tạo member score 0 làm nhiễu `ZCARD`.

---

## 9. Đọc dữ liệu — trang Tiến độ

`ProgressService` là lớp **read-only**, tự lấy `userId` từ SecurityContext (JWT) qua `CurrentUserService`.

### 9.1 Tối ưu số query

**Khối 1 — Summary (3 query, trước đây 6):**

1. `aggregateTwoPeriods` — gộp **kỳ hiện tại + kỳ trước** vào 1 query (2 kỳ liền kề nên 1 lần scan đủ cả hai), trả về 7 cột.
2. `findById` trên `user_gamification` — đọc `longestStreak` đã lưu sẵn (O(1)).
3. `findStreakWindow` — chỉ quét cửa sổ ngày active để tính `currentStreak`.

> **Bẫy đã tránh:** JPQL với named param lặp lại (`:from` xuất hiện 7 lần) khiến Hibernate sinh SQL với các `?` mà **thứ tự bind không xác định** → `BETWEEN` nhận nhầm giá trị → trả 0 dù DB có data. Query đã viết lại để **mỗi tham số chỉ xuất hiện đúng 1 lần**, đúng thứ tự.

**Khối 2 — Calendar (3 query):** `findCalendarRange` (dùng index `(user_id, stat_date DESC)`) + tái sử dụng streak window + đọc `longestStreak`.

Chỉ trả về **ngày có hoạt động** — ngày trống không có row, FE tự điền lưới. Tháng 31 ngày nhưng user mới chỉ có vài ngày → payload nhỏ hơn nhiều.

**Khối 4 — Top lookups (1 + tối đa 3 query, không N+1):**

1. 1 query `GROUP BY target` trên `study_events` (có index partial `idx_study_events_lookup`).
2. Batch fetch thông tin hiển thị: 1 query `vocabulary` + 1 query `grammar_points` + 1 query `writing_characters`.

Chỉ trả về đối tượng **còn tồn tại** trong từ điển — từ đã xoá mềm bị bỏ khỏi bảng (1 dòng trống tên vô nghĩa còn hơn).

**Khối 5 — Leaderboard:** đọc Redis trước; miss thì query DB + rebuild (có lock); ngoài TTL 8 tuần thì đọc thẳng DB, không ghi lại Redis. Dựng danh sách entries bằng đúng **2 batch query** (`findWeekStatsByUserIds` + `findDisplayInfosByIds`).

### 9.2 Quy tắc "đạt mục tiêu" — hẹp vs rộng

Có **hai khái niệm "đạt mục tiêu"** đang cùng tồn tại, rất dễ nhầm:

| | Quy tắc | Dùng ở đâu |
|---|---|---|
| **Hẹp** | `lookups >= goal_words` | Ô lịch (Khối 2), thẻ "Mục tiêu ngày" (Khối 1) |
| **Rộng** (`goal_met` trong DB) | `lookups >= goalWords` **HOẶC** `lessons_completed > 0` **HOẶC** `study_seconds >= min_study_seconds` | Cột lưu trong DB |

API Progress cố tình **không đọc cột `goal_met`** mà tự tính theo quy tắc hẹp. Lý do: nếu dùng cột `goal_met`, cùng một ngày có thể vừa hiện "đạt" ở lịch vừa "chưa đạt" ở thẻ mục tiêu → mâu thuẫn trên UI.

### 9.3 Cache — generation token (invalidate O(1))

`ProgressCacheService` không dùng `@Cacheable` vì: cache key động (`from`/`to`) → SpEL rườm rà; cần xoá **toàn bộ** cache của 1 user mà `@CacheEvict` không hỗ trợ pattern.

```
user ghi dữ liệu mới
     ↓
invalidate(userId) → gen = "uuid-mới"
     ↓
cache key = progress:summary:{userId}:{from}:{to}:{gen}
cache cũ có gen cũ → không còn ai đọc → tự hết hạn theo TTL
```

O(1), không cần `SCAN` keyspace.

**Vì sao bump generation phải SAU commit?** Nếu bump trước khi commit, request song song có thể đọc lại dữ liệu cũ (chưa commit) rồi cache lại → vẫn thấy 0. Chỉ bump sau khi commit mới đảm bảo lần đọc tiếp theo thấy dữ liệu mới.

**Fail-safe:** Redis lỗi → `currentGeneration` trả `"bypass"` → cache key luôn khác nhau mỗi lần → tự động bypass cache. Redis chết không làm chết trang Progress.

TTL: summary 5 phút, chart 15 phút, calendar 30 phút, generation token 24 giờ.

---

## 10. API

| Method | Path | Mô tả | Quyền |
|---|---|---|---|
| `POST` | `/api/v1/dictionary/lookup` | Ghi nhận 1 lần tra từ (không trả dữ liệu từ điển) | User |
| `POST` | `/api/v1/study/heartbeat` | Gửi thời gian học | User |
| `GET` | `/api/v1/progress/summary` | Tổng hợp theo kỳ (`from`/`to`, mặc định 30 ngày, trần 366) | User |
| `GET` | `/api/v1/progress/calendar` | Lịch + streak theo tháng (`month=yyyy-MM`) | User |
| `GET` | `/api/v1/progress/chart` | Biểu đồ time-series | User |
| `GET` | `/api/v1/progress/top-lookups` | Top từ đã tra (`limit` mặc định 30, trần 100) | User |
| `GET` | `/api/v1/progress/leaderboard` | Bảng xếp hạng tuần (`week=YYYY-Www`) | User |
| `GET` | `/api/v1/progress/leaderboard/me` | Hạng cá nhân | User |
| `POST` | `/api/v1/progress/leaderboard/refresh` | Bù lỡ điểm (cooldown 30s) | User |
| `POST` | `/api/v1/progress/leaderboard/refresh/hard` | Xoá key Redis, dựng lại từ đầu | **ADMIN** |

**Ghi chú:**

- `top-lookups` **mặc định xếp hạng toàn bộ lịch sử**; chỉ khi truyền `from`/`to` mới giới hạn theo kỳ.
- `leaderboard` và `leaderboard/me` trả header `Cache-Control: no-store`.
- `refresh/hard` tách riêng khỏi `?hard=true` để `@PreAuthorize` gắn ở cấp method — không thể bị điều khiển bằng query string.
- Error code riêng: `LEADERBOARD_INVALID_WEEK` (5001), `LEADERBOARD_WEEK_IN_FUTURE` (5002), `LEADERBOARD_REFRESH_TOO_FREQUENT` (5003).

---

## 11. Tóm tắt các quyết định thiết kế đắt giá

Đây là những chỗ "đọc code sẽ thấy lạ" nhưng **đều có lý do**:

| # | Quyết định | Lý do |
|---|---|---|
| 1 | `Clock` bean cố định `Asia/Ho_Chi_Minh` | Dev (GMT+7) vs Docker (UTC) ghi `stat_date` khác nhau → lệch ngày/tuần/streak |
| 2 | `study_events` ghi mọi lần tra, **không trần** | Thống kê "từ tra nhiều nhất" phải phản ánh thói quen thật, không bị cắt cụt |
| 3 | Tách `lookups` / `lookups_scored` / `new_items_learned` | Vừa hiển thị đầy đủ cho user, vừa có trần chống spam XP, vừa đo "học thật" cho leaderboard |
| 4 | `ZADD GT` thay cho `ZINCRBY` | Idempotent + không bao giờ ghi lùi điểm (3 kịch bản race cụ thể) |
| 5 | Mọi thao tác Redis chạy **sau commit** | Không cộng điểm cho transaction bị rollback; invalidate cache không cache nhầm dữ liệu cũ |
| 6 | Query điểm leaderboard chạy `REQUIRES_NEW` | Connection gốc còn bind trên thread → cần connection mới để đọc snapshot mới |
| 7 | `recordLessonComplete` (daily) **trước** `awardXp` | Nếu đảo, `syncUserScore` đọc thiếu `lessons_completed` → ZSET lệch DB |
| 8 | `REQUIRES_NEW` cho daily stats | Phần tích không được rollback theo phần khác của hoạt động |
| 9 | try/catch riêng từng nhánh trong `StudyTrackerService` | Analytics không bao giờ làm hỏng UX |
| 10 | `@TransactionalEventListener(AFTER_COMMIT)` cho lesson | Không chặn response time, không cộng XP cho bài bị rollback |
| 11 | Query `group by`/`aggregate` viết native, mỗi param 1 lần | Tránh bẫy thứ tự bind param của JPQL → trả sai kết quả |
| 12 | `ZCOUNT` exclusive qua connection-level | `ZSetOperations.count` inclusive → đếm cả đồng điểm → hạng sai |
| 13 | `ZREVRANK` không dùng | Redis xếp hạng đồng điểm khác nhau; cần quy tắc "cùng điểm = cùng hạng" |
| 14 | Heartbeat không cộng XP, không ghi `study_events` | Tránh phình bảng (30s/lần) và tránh XP miễn phí |
| 15 | Heartbeat leader lock 2 tầng (FE BroadcastChannel + BE Redis) | FE có thể bị vượt (2 thiết bị, 2 browser profile) |
| 16 | TTL `study_seconds` 16h/ngày | Chặn ghi sai số liệu lớn |
| 17 | Invalidate cache bằng generation token | O(1), không cần `SCAN` pattern |
| 18 | Trả calendar chỉ ngày có hoạt động | Giảm payload, BE không phải phân loại "ngày không học" |
| 19 | `lookup_ceiling` chuyển từ cột sang `app_config` | Đổi trần không cần migration/deploy |
| 20 | `XpSource.DAILY_TASK` dùng cho lookup (tên cũ) | Enum giữ nguyên giá trị để không phá dữ liệu `xp_logs` cũ |

---

## 12. Điểm chưa xác định / hạn chế

1. **`Badge`, `UserBadge`** — entity đã tồn tại trong `entity/gamification/` nhưng **không có service/repository/controller** nào dùng đến. Đây là phần chưa làm của module gamification (huy hiệu). Riêng `Notification` **đã có module riêng** — xem §12.1.

2. **Không có XP cho SRS review / handwriting** — enum `XpSource` có `SRS_REVIEW`, `HANDWRITING` trong DB constraint (`V25`) nhưng không có đường ghi XP cho các hoạt động đó.

3. **`goal_met` trong DB lưu theo quy tắc rộng, API dùng quy tắc hẹp.** Hai khái niệm cùng tồn tại là chủ ý (xem §9.2) nhưng dễ gây nhầm khi đọc cột `goal_met` trực tiếp trong SQL.

4. **Chỉ có leaderboard theo tuần.** Bảng xếp hạng hiện chỉ là **tuần**, TTL 8 tuần → không xem được xếp hạng dài hạn dù `user_gamification.total_xp` vẫn lưu mãi.

5. **`longest_streak` là cache, không tự đồng bộ.** Nếu ai đó sửa tay `user_daily_stats`, `longest_streak` trong `user_gamification` sẽ lệch. Service hiện fallback sang `findLongestActiveStreak` (query "gaps and islands") **chỉ khi** row `user_gamification` chưa tồn tại — tức là user mới. Cần xoá row đó để buộc tính lại.

6. **Không có job dọn `study_events`.** Bảng append-only tăng dần; chưa có retention/purge.

7. **`ProgressCacheService.purgeAll()` dùng `KEYS` không phải `SCAN`** — vốn đã ghi chú trong code là O(N), chỉ dành cho debug/admin. Không được gọi ở production path.

8. **`sessionId` lưu trong `sessionStorage`** (giống localStorage nhưng theo tab) để sống qua các lần mount/unmount trong SPA. Nếu không có, mỗi lần điều hướng sẽ tạo session mới và bị từ chối tới 90 giây → `studySeconds` không bao giờ được ghi.

---

## 12.1 Module Thông báo (in-app notifications)

### Kiến trúc

| Thành phần | Vai trò |
|---|---|
| `config/WebSocketConfig` | Bật STOMP: endpoint handshake `/ws` (root, **không** có `/api/v1`), broker `/queue` + `/topic`, user destination prefix `/user`. Không dùng SockJS. |
| `config/WebSocketAuthInterceptor` | Xác thực frame STOMP `CONNECT` bằng `Authorization: Bearer <token>`; principal = **chuỗi userId**. |
| `service/notification/NotificationService` | Tạo thông báo (`REQUIRES_NEW`), đọc/đếm/danh sách, đánh dấu đã đọc. |
| `service/notification/NotificationPublisher` | `convertAndSendToUser(userId, "/queue/notifications", payload)`, bọc try/catch. |
| `service/notification/NotificationScheduler` | 2 job `@Scheduled` + `@SchedulerLock`. |
| `controller/notification/NotificationController` | REST snapshot `/api/v1/notifications/**`. |

Luồng real-time:

```
event/service  ──save DB──▶  commit  ──afterCommit──▶  STOMP
                                   ▼
                     /queue/notifications-user{session}
                                   ▼
                     client subscribe /user/queue/notifications
```

**Vì sao broker phải là `/queue` chứ không phải `/user`:** `convertAndSendToUser` dịch đích thành `/queue/notifications-user{sessionId}` rồi mới đẩy vào simple broker. `/user` chỉ là prefix phía client khi subscribe.

**Vì sao push ở `afterCommit`:** tránh client nhận thông báo cho bản ghi chưa tồn tại / bị rollback (badge tăng nhưng mở danh sách không thấy).

**Vì sao `REQUIRES_NEW`:** listener `AFTER_COMMIT` chạy khi transaction gốc đã kết thúc — không còn transaction active để ghi bản ghi mới.

### 3 nguồn phát sinh

| Nguồn | Cơ chế | Thời điểm |
|---|---|---|
| `COMMUNITY_REPLY` | `CommentService.addReply` publish `CommentRepliedEvent` → `CommentReplyNotificationListener` (`@TransactionalEventListener(AFTER_COMMIT)`) | Ngay khi có reply |
| `SRS_DUE` | `NotificationScheduler.sendSrsDueReminders` — `countDueCardsGroupByUser` | 08:00 `Asia/Ho_Chi_Minh` |
| `STREAK_WARNING` | `NotificationScheduler.sendStreakWarnings` — `findStreakWindow` + `StreakService.computeCurrentStreakFromDesc` | 20:30 `Asia/Ho_Chi_Minh` |

Tất cả tôn trọng `user_settings`: `notify_community`, `notify_srs_due`, `notify_streak_warning`.

`STREAK_WARNING` dùng **đúng chính sách STRICT**: nguồn sự thật là `user_daily_stats.new_items_learned > 0`, không kết luận từ cache `user_gamification.current_streak`.

### Chống trùng khi scale ngang (2 lớp)

1. **ShedLock** (`@SchedulerLock`) — bảng `shedlock`, `usingDbTime()` để không lệch clock giữa instance.
2. **Unique index** `uq_notifications_daily_reminder` trên `(user_id, type, ngày VN)` cho `SRS_DUE`/`STREAK_WARNING` — instance thua nhận `DataIntegrityViolationException` và bỏ qua.

Ngoài DB còn 1 lớp check `existsByUser_IdAndTypeAndCreatedAtBetween` trước khi tạo.

### Lỗi và mã lỗi

`NOTIFICATION_NOT_FOUND (6000)`, `NOTIFICATION_FORBIDDEN (6001)` — dùng trong `markAsRead` để phân biệt "id không tồn tại" và "của user khác".

Lỗi tạo thông báo **không** được làm hỏng luồng chính: try/catch đặt ở **caller** (listener, scheduler), không đặt trong method `@Transactional` (bắt exception bên trong sẽ để transaction rollback-only → `UnexpectedRollbackException`).

### Frontend

- `components/notification/NotificationBell` — chuông + badge trên `TopBar`, dropdown 10 thông báo.
- `hooks/useNotificationSocket` — STOMP `Client`, `brokerURL` từ `lib/wsUrl` (`ws://`/`wss://`), token lấy động trong `beforeConnect` (không dùng `connectHeaders` tĩnh, nếu không reconnect sẽ gửi mãi token cũ).
- `stores/useNotificationStore` — badge + danh sách tức thời; REST (`useUnreadCount` với `refetchInterval: 60s`) là lưới an toàn khi socket rớt.

