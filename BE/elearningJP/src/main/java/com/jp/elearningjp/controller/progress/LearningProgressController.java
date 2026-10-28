package com.jp.elearningjp.controller.progress;

import com.jp.elearningjp.dto.response.progress.CalendarResponse;
import com.jp.elearningjp.dto.response.progress.ChartResponse;
import com.jp.elearningjp.dto.response.progress.LeaderboardResponse;
import com.jp.elearningjp.dto.response.progress.MyRankResponse;
import com.jp.elearningjp.dto.response.progress.ProgressSummaryResponse;
import com.jp.elearningjp.dto.response.progress.TopLookupResponse;
import com.jp.elearningjp.service.progress.ProgressService;
import com.jp.elearningjp.shared.constants.ApiPaths;
import com.jp.elearningjp.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * API trang "Tiến độ học tập".
 *
 * <p>Bao gồm 4 khối: tổng hợp theo kỳ, lịch học + streak, top từ đã tra,
 * và bảng xếp hạng tuần.</p>
 */
@RestController
@RequestMapping(ApiPaths.API_V1 + ApiPaths.LearningProgress.BASE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Progress (Tiến độ học tập)",
        description = "API tổng hợp tiến độ: từ đã học, streak, thời gian học, biểu đồ, bảng xếp hạng")
public class LearningProgressController {

    ProgressService progressService;

    @Operation(
            summary = "Tổng hợp tiến độ học tập theo kỳ",
            description = """
                    **Định nghĩa nghiệp vụ:**
                    - `wordsLearned` = số **MỤC MỚI** đã học trong kỳ (mỗi từ vựng / ngữ pháp /
                      Hán tự chỉ tính 1 lần, khi học lần đầu tiên)
                    - `totalLookups` = **TỔNG** số lần tra từ điển trong kỳ (kể cả tra lại từ đã học)
                    - `currentStreak` = số ngày liên tiếp học được ít nhất 1 mục mới
                    - `goalWords` = mục tiêu mặc định 5 mục mới/ngày

                    **Tham số:**
                    - `from` / `to`: định dạng `yyyy-MM-dd`. Bỏ trống → mặc định 30 ngày gần nhất.
                    - Giới hạn tối đa 366 ngày; nếu vượt sẽ tự cắt về 366 ngày gần nhất.""")
    @GetMapping(ApiPaths.LearningProgress.SUMMARY)
    public ApiResponse<ProgressSummaryResponse> summary(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {

        // Service tự lấy userId hiện tại từ SecurityContext (JWT).
        return ApiResponse.<ProgressSummaryResponse>builder()
                .success(true)
                .data(progressService.getSummary(from, to))
                .build();
    }

    // =====================================================================
    // Khối 2 — Lịch học + streak
    // =====================================================================

    @Operation(
            summary = "Lịch học + chuỗi streak theo tháng",
            description = """
                    Dùng cho ô lịch trên trang Tiến độ. Quy tắc màu ở FE:
                    - Ngày **đạt mục tiêu** (`newWords >= goalWords`) → màu xanh dương.
                    - Ngày **có học nhưng chưa đạt** → màu xanh lá.
                    - Ngày **không học** → không tô màu.

                    ⚠️ `goalMet` ở đây dùng quy tắc **hẹp** (`newWords >= goalWords`),
                    **không** đọc cột `user_daily_stats.goal_met` (cột đó còn tính cả
                    "học xong 1 bài" hoặc "học đủ 5 phút"). Nhờ vậy cùng một ngày không
                    thể vừa hiện "đạt" ở lịch vừa "chưa đạt" ở thẻ Mục tiêu ngày.

                    Mỗi ngày có 2 chỉ số khác nhau:
                    - `newWords` — số từ MỚI học (mỗi mục chỉ tính 1 lần) → "từ đã học"
                    - `lookups` — tổng số lần tra từ điển (kể cả tra lại từ cũ)

                    `days` **chỉ chứa ngày có hoạt động** — ngày trống không có row,
                    FE tự điền các ô còn trống.""")
    @GetMapping(ApiPaths.LearningProgress.CALENDAR)
    public ApiResponse<CalendarResponse> calendar(
            @Parameter(description = "Tháng theo định dạng yyyy-MM. Bỏ trống → tháng hiện tại.")
            @RequestParam(required = false)
            String month) {

        return ApiResponse.<CalendarResponse>builder()
                .success(true)
                .data(progressService.getCalendar(month))
                .build();
    }

    // =====================================================================
    // Khối 3 — Biểu đồ tiến đồ (time-series)
    // =====================================================================

    @Operation(
            summary = "Dữ liệu biểu đồ tiến độ theo kỳ",
            description = """
                    **Nguồn dữ liệu:** bảng `user_daily_stats` (cột `new_items_learned` = số từ MỚI học).

                    **Gộp bucket (granularity):** `day` | `week` | `month`.
                    - `day` — mỗi ngày 1 cột.
                    - `week` — tuần ISO 8601 (thứ 2 → chủ nhật), dùng **cùng** quy tắc với
                      bảng xếp hạng tuần nên cột "tuần" luôn khớp với tuần của Khối 5.
                    - `month` — tháng lịch (ngày 1 → hết tháng).

                    **Bucket không có hoạt động vẫn được trả về** với `wordsLearned = 0`
                    để đường "tích lũy" không bị đứt khoảng.

                    **`cumulativeWords` = tổng cộng dồn TRONG KỲ, bắt đầu từ 0**
                    (không cộng dồn lịch sử trước kỳ) → đổi preset là đường luôn bắt đầu từ 0.

                    **Tham số:**
                    - `from` / `to`: định dạng `yyyy-MM-dd`. Bỏ trống → mặc định 30 ngày gần nhất.
                    - Giới hạn tối đa 366 ngày; nếu vượt sẽ tự cắt về 366 ngày gần nhất.""")
    @GetMapping(ApiPaths.LearningProgress.CHART)
    public ApiResponse<ChartResponse> chart(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @Parameter(description = "Đơn vị bucket: day | week | month. Bỏ trống → day.")
            @RequestParam(required = false)
            String granularity) {

        // Service tự lấy userId hiện tại từ SecurityContext (JWT).
        return ApiResponse.<ChartResponse>builder()
                .success(true)
                .data(progressService.getChart(from, to, granularity))
                .build();
    }

    // =====================================================================
    // Khối 4 — Top từ đã tra
    // =====================================================================

    @Operation(
            summary = "Top N từ/đối tượng được tra cứu nhiều nhất",
            description = """
                    Gộp **cả 3 loại** đối tượng tra cứu vào một danh sách, sắp xếp chung theo
                    số lần tra: từ vựng (`VOCAB`), Hán tự (`KANJI`), ngữ pháp (`GRAMMAR`).
                    Nhờ vậy "từ tra nhiều nhất" phản ánh đúng thói quen thật.

                    **Mặc định xếp hạng TOÀN BỘ lịch sử.** Bỏ trống cả `from` và `to`
                    → tính trên mọi lần tra từ trước đến nay. Chỉ khi truyền
                    `from`/`to` mới giới hạn theo kỳ (thiếu `from` → 30 ngày gần nhất).

                    Nguồn dữ liệu: bảng append-only `study_events` (`event_type = 'LOOKUP'`),
                    `GROUP BY target` và đếm.

                    Chỉ trả về đối tượng **còn tồn tại** trong từ điển (bỏ qua mục đã
                    xoá mềm) để không có dòng trống tên trong danh sách.""")
    @GetMapping(ApiPaths.LearningProgress.TOP_LOOKUPS)
    public ApiResponse<List<TopLookupResponse>> topLookups(
            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd). Bỏ trống cùng `to` → toàn bộ lịch sử.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd). Bỏ trống → hôm nay.")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @Parameter(description = "Số dòng tối đa. Bỏ trống → 30. Trần cứng 100.")
            @RequestParam(required = false)
            Integer limit) {

        return ApiResponse.<List<TopLookupResponse>>builder()
                .success(true)
                .data(progressService.getTopLookups(from, to, limit))
                .build();
    }

    // =====================================================================
    // Khối 5 — Leaderboard
    // =====================================================================

    @Operation(
            summary = "Bảng xếp hạng tuần (đọc Redis Sorted Set)",
            description = """
                    **Nguồn dữ liệu:** Redis Sorted Set (key `leaderboard:weekly:v3:{week}`),
                    là **index dẫn xuất từ DB** — DB (`user_daily_stats`) mới là nguồn sự thật.
                    - ZSET được ghi bằng `ZADD GT` → không bao giờ ghi lùi điểm.
                    - Redis miss → query DB → rebuild ZSET (có lock chống cache stampede).
                    - Tuần ngoài TTL 8 tuần → đọc thẳng DB, không ghi lại Redis.

                    **Điểm (score)** = `new_items_learned * learned_item_points + lessons_completed * lesson_points`.
                    **Hạng**: cùng điểm → cùng hạng (1, 1, 3).

                    **Tuần:** chuẩn ISO 8601 (`YYYY-'W'ww`), tuần bắt đầu thứ 2.
                    Tuần tương lai → HTTP 400.""")
    @GetMapping(ApiPaths.LearningProgress.LEADERBOARD)
    public ResponseEntity<ApiResponse<LeaderboardResponse>> leaderboard(
            @Parameter(description = "Tuần theo chuẩn ISO: YYYY-'W'ww  (VD: 2026-W42). Bỏ trống → tuần hiện tại.")
            @RequestParam(required = false)
            String week) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.<LeaderboardResponse>builder()
                        .success(true)
                        .data(progressService.getLeaderboard(week))
                        .build());
    }

    @Operation(
            summary = "Hạng cá nhân trong bảng xếp hạng tuần",
            description = """
                    Đọc trực tiếp từ Redis Sorted Set:
                    - `ZSCORE` → điểm của tôi
                    - `ZCOUNT (score, +inf)` (exclusive) → số user có điểm **cao hơn** tôi
                    - rank = số đó + 1 → **cùng điểm thì cùng hạng** (1, 1, 3)
                    - `ZCARD` → tổng số người có điểm trong tuần

                    Redis lỗi / rỗng → fallback sang query DB (kết quả tương đương).""")
    @GetMapping(ApiPaths.LearningProgress.LEADERBOARD_ME)
    public ResponseEntity<ApiResponse<MyRankResponse>> myRank(
            @Parameter(description = "Tuần theo chuẩn ISO: YYYY-'W'ww. Bỏ trống → tuần hiện tại.")
            @RequestParam(required = false)
            String week) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.<MyRankResponse>builder()
                        .success(true)
                        .data(progressService.getMyRank(week))
                        .build());
    }

    @Operation(
            summary = "Refresh bảng xếp hạng tuần",
            description = """
                    **Mục đích thật của nút Reload — bù cho trường hợp `syncUserScore` bị lỡ**
                    (Redis lỗi đúng lúc `afterCommit` → điểm trong DB có nhưng ZSET thiếu).

                    ⚠️ **Không sửa điểm giảm.** Vì `ZADD GT` chỉ nâng điểm, rebuild thường
                    **không** ghi đè được điểm đã bị giảm trong DB. Muốn sửa điểm giảm
                    (sửa tay dữ liệu, xoá row `user_daily_stats`) → dùng endpoint **hard**
                    (chỉ ADMIN).

                    **Cooldown 30s** → gọi lại trong 30s sẽ nhận HTTP 429.""")
    @PostMapping(ApiPaths.LearningProgress.LEADERBOARD_REFRESH)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> refreshLeaderboard(
            @Parameter(description = "Tuần cần refresh: YYYY-'W'ww. Bỏ trống → tuần hiện tại.")
            @RequestParam(required = false)
            String week) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.<LeaderboardResponse>builder()
                        .success(true)
                        .data(progressService.refreshLeaderboard(week, false))
                        .build());
    }

    @Operation(
            summary = "Hard refresh bảng xếp hạng tuần (xoá key Redis, dựng lại từ đầu)",
            description = """
                    **Dành cho ADMIN.** Đây là thao tác **xoá dữ liệu Redis** nên tách
                    thành endpoint riêng (không phải `?hard=true`) — quyền được cố định
                    bằng `@PreAuthorize` ở cấp method, không thể bị điều khiển bằng
                    query string.

                    **Khi nào dùng:** khi điểm trong DB đã bị **giảm** (sửa tay dữ liệu,
                    xoá row `user_daily_stats`) → `ZADD GT` thường không sửa được.
                    Endpoint này `DEL` key rồi dựng lại từ đầu → sửa được cả điểm giảm.

                    **Cooldown 30s** → HTTP 429 nếu gọi lại quá nhanh.""")
    @PostMapping(ApiPaths.LearningProgress.LEADERBOARD_REFRESH_HARD)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> hardRefreshLeaderboard(
            @Parameter(description = "Tuần cần refresh: YYYY-'W'ww. Bỏ trống → tuần hiện tại.")
            @RequestParam(required = false)
            String week) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.<LeaderboardResponse>builder()
                        .success(true)
                        .data(progressService.refreshLeaderboard(week, true))
                        .build());
    }
}
