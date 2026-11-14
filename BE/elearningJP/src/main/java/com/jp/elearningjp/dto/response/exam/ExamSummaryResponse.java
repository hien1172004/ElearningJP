package com.jp.elearningjp.dto.response.exam;

import com.jp.elearningjp.shared.enums.ExamType;
import com.jp.elearningjp.shared.enums.JlptLevel;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExamSummaryResponse {
    Long id;
    String title;
    ExamType examType;
    JlptLevel jlptLevel;
    Integer totalDurationMinutes;
    Integer totalScaledMaxScore;
    Integer overallPassingScore;
    Integer totalQuestions;
    boolean published;
    boolean isPremium;
    /** Đề gắn khóa: phiên bản đã duyệt (null = chưa được duyệt lần nào, chưa hiện được cho học viên). */
    Long publishedVersionId;
    /** Số phiên bản phát hành (V1, V2...). */
    Integer versionNo;
    /** Ghi chú tóm tắt của phiên bản (changelog). */
    String changelog;
    /** Đề gắn khóa đang chờ gỡ (bị xóa thật khi khóa được duyệt lần tới). */
    boolean pendingRemoval;
}
