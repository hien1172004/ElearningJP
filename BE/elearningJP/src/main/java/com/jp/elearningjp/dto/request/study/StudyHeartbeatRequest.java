package com.jp.elearningjp.dto.request.study;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Heartbeat thời gian học, FE gửi mỗi ~30 giây khi user còn tương tác.
 *
 * <p>BE validate {@code seconds ∈ [1, 60]} để chống client gửi số lớn bất thường.
 * BE cũng cap theo Redis lock để chống duplicate heartbeat từ nhiều tab.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Heartbeat thời gian học (gửi mỗi ~30s khi user đang tương tác)")
public class StudyHeartbeatRequest {

    @Schema(description = "Số giây học kể từ heartbeat trước. Chấp nhận 1-60.",
            example = "30")
    @Min(1)
    @Max(60)
    private Integer seconds;

    /**
     * Session ID phía client (uuid v4). Dùng để khóa heartbeat khi user
     * mở nhiều tab — tab nào giành quyền ghi nhận thời gian thì chỉ tab đó ghi.
     */
    @NotBlank
    @Size(max = 64)
    @Schema(description = "Session uuid phía client (tab leader). Hết hạn sau 90s không hoạt động.",
            example = "9f4b6e8e-2f0e-4a9b-bd6e-9f3a7e2c5b1d")
    private String sessionId;
}