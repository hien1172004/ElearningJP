package com.jp.elearningjp.dto.response.study;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả ghi nhận heartbeat")
public class StudyHeartbeatResponse {

    @Schema(description = "Có ghi nhận thời gian không. False nếu session hiện tại không phải tab leader (lock thuộc tab khác).",
            example = "true")
    private Boolean accepted;

    @Schema(description = "Tổng số giây đã ghi hôm nay", example = "180")
    private Integer studySecondsToday;

    @Schema(description = "Ngày thống kê", example = "2026-10-20")
    private LocalDate statDate;

    /** Lý do từ chối (chỉ có khi {@code accepted=false}). */
    @Schema(description = "Lý do từ chối (chỉ có khi accepted=false)", example = "Một tab khác đang giữ quyền")
    private String reason;
}