package com.jp.elearningjp.dto.request.notebook;

import com.jp.elearningjp.shared.enums.SrsRating;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotebookReviewSubmitRequest {

    @NotNull(message = "wordId không được để trống")
    private Long wordId;

    @NotNull(message = "Đánh giá SRS không được để trống")
    private SrsRating rating;

    private Integer timeSpentSeconds;
}

