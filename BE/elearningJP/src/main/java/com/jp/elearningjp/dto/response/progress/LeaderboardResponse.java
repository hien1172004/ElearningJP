package com.jp.elearningjp.dto.response.progress;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Wrapper bảng xếp hạng tuần (Khối 5).
 *
 * <p><b>Vì sao cần wrapper?</b> Bảng rỗng vẫn phải trả về {@code weekKey} để FE
 * điều hướng tuần (biết đang ở tuần nào, tuần trước/sau là tuần nào). Nếu trả
 * {@code List} rỗng thì mất thông tin này.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Bảng xếp hạng tuần")
public class LeaderboardResponse {

    @Schema(description = "Tuần xếp hạng (ISO: YYYY-'W'ww)", example = "2026-W42")
    private String weekKey;

    @Schema(description = "Ngày thứ 2 đầu tuần", example = "2026-10-12")
    private LocalDate weekStart;

    @Schema(description = "Ngày chủ nhật cuối tuần", example = "2026-10-18")
    private LocalDate weekEnd;

    @Schema(description = "Tuần này có phải tuần hiện tại không", example = "true")
    private Boolean isCurrentWeek;

    @Schema(description = "Tổng số người có điểm trong tuần", example = "128")
    private Long totalPlayers;

    @Schema(description = "Danh sách xếp hạng (tối đa 30 dòng)")
    private List<LeaderboardEntryResponse> entries;
}
