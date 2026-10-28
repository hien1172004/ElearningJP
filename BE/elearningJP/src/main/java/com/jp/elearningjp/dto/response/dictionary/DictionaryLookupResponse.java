package com.jp.elearningjp.dto.response.dictionary;

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
@Schema(description = "Kết quả ghi nhận 1 lần tra từ điển (cho tiến độ học tập)")
public class DictionaryLookupResponse {

    @Schema(description = "Số từ đã học hôm nay (= số MỤC MỚI học lần đầu trong ngày)", example = "7")
    private Integer wordsLearnedToday;

    @Schema(description = "Mục tiêu từ/ngày", example = "5")
    private Integer goalWords;

    @Schema(description = "Đã đạt mục tiêu hôm nay chưa", example = "true")
    private Boolean goalMet;

    @Schema(description = "XP vừa cộng", example = "2")
    private Integer xpAwarded;

    @Schema(description = "Ngày thống kê", example = "2026-10-20")
    private LocalDate statDate;
}