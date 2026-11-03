package com.jp.elearningjp.dto.response.notebook;

import com.jp.elearningjp.shared.enums.MemoryLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotebookReviewSubmitResponse {

    private Long wordId;
    private MemoryLevel memoryLevel;
    private Integer repetitionCount;
    private Integer intervalDays;
    private BigDecimal easeFactor;
    private Instant nextReviewAt;
}

