package com.jp.elearningjp.dto.request.srs;

import com.jp.elearningjp.shared.enums.SrsRating;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SrsReviewRequest {

    @NotNull(message = "Đánh giá không được để trống (AGAIN, HARD, GOOD, EASY)")
    SrsRating rating;

    Integer responseTimeMs;

    UUID clientEventId;
}
