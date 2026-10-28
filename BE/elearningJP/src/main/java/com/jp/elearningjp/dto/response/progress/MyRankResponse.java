package com.jp.elearningjp.dto.response.progress;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hạng cá nhân của user trong tuần.
 *
 * <p>Tách riêng khỏi {@link LeaderboardResponse} vì đây là dữ liệu của <b>1 user</b>
 * (người đang đăng nhập), không phụ thuộc user có nằm trong top 5 hay không.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Hạng cá nhân của user trong tuần")
public class MyRankResponse {

    @Schema(description = "Hạng của user (bắt đầu từ 1). Cùng điểm → cùng hạng", example = "3")
    private Long rank;

    @Schema(description = "Điểm xếp hạng của user trong tuần", example = "42")
    private Long score;

    @Schema(description = "Tổng số người có điểm trong tuần", example = "128")
    private Long totalPlayers;

    @Schema(description = "Tuần xếp hạng (ISO: YYYY-'W'ww)", example = "2026-W42")
    private String weekKey;

    @Schema(description = "ID của user", example = "42")
    private Long userId;

    @Schema(description = "Tên hiển thị của user", example = "日本語マスター")
    private String displayName;

    @Schema(description = "Avatar URL (null nếu chưa có)", example = "https://...")
    private String avatarUrl;

    @Schema(description = "User có nằm trong bảng top 5 không", example = "false")
    private Boolean inTop;
}
