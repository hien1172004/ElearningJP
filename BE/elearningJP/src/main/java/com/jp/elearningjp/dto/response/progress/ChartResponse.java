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
 * Du lieu bieu do tien do (Khoi 3).
 *
 * <p>Bieu do gom 2 series:</p>
 * <ul>
 *   <li>{@code wordsLearned} (cot) — so tu da tra trong tung bucket</li>
 *   <li>{@code cumulativeWords} (duong) — tong cong don trong ky, bat dau tu 0</li>
 * </ul>
 *
 * <p><b>Co wrapper</b> vi BE co the tu cat ky (toi da 366 ngay) va tu chon
 * granularity — FE can biet ky thuc te + don vi bucket de ve dung truc X.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Du lieu bieu do tien do theo ky")
public class ChartResponse {

    @Schema(description = "Ngay bat dau ky thuc te (inclusive)", example = "2026-10-01")
    private LocalDate from;

    @Schema(description = "Ngay ket thuc ky thuc te (inclusive)", example = "2026-10-31")
    private LocalDate to;

    @Schema(description = "Don vi bucket: day | week | month", example = "day")
    private String granularity;

    @Schema(description = "Tong so tu da hoc trong ky", example = "128")
    private Integer totalWords;

    @Schema(description = "Cac diem cua bieu do, tang dan theo thoi gian")
    private List<ChartPointResponse> points;
}
