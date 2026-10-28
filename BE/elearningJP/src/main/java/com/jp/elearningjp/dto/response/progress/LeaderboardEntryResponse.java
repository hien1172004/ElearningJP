package com.jp.elearningjp.dto.response.progress;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một dòng trong bảng xếp hạng tuần (Khối 5).
 *
 * <p>Sắp xếp theo {@code score} giảm dần. Cùng score → cùng rank.</p>
 *
 * <p><b>Quy ước field:</b></p>
 * <ul>
 *   <li>{@code score} = điểm xếp hạng =
 *       {@code new_items_learned * learned_item_points + lessons_completed * lesson_points}</li>
 *   <li>{@code totalNewWords} = tổng số từ MỚI đã học trong tuần (= {@code new_items_learned})</li>
 *   <li>{@code totalLookups} = tổng lần tra trong tuần (KHÔNG trần)</li>
 *   <li>{@code totalXp} = tổng XP trong tuần</li>
 * </ul>
 *
 * <p>Hết trường → {@code null} (đường Redis và đường DB phải trả cùng kiểu dữ liệu
 * để bảng hiển thị giống nhau tuỳ cache).</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Một dòng trong bảng xếp hạng tuần")
public class LeaderboardEntryResponse {

    @Schema(description = "Hạng trong tuần (bắt đầu từ 1). Cùng điểm → cùng hạng", example = "1")
    private Integer rank;

    @Schema(description = "ID của user", example = "42")
    private Long userId;

    @Schema(description = "Tên hiển thị của user", example = "日本語マスター")
    private String displayName;

    @Schema(description = "Avatar URL (null nếu chưa có)", example = "https://...")
    private String avatarUrl;

    @Schema(description = "Điểm xếp hạng trong tuần", example = "42")
    private Long score;

    @Schema(description = "Tổng số từ MỚI đã học trong tuần (mỗi mục tính 1 lần)", example = "35")
    private Long totalNewWords;

    @Schema(description = "Tổng số lần tra từ điển trong tuần (không trần)", example = "87")
    private Long totalLookups;

    @Schema(description = "Tổng XP kiếm được trong tuần", example = "240")
    private Long totalXp;

    @Schema(description = "Tuần xếp hạng (ISO: YYYY-'W'ww)", example = "2026-W42")
    private String weekKey;
}
