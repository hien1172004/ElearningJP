package com.jp.elearningjp.dto.response.comment;

import com.jp.elearningjp.shared.enums.CommentTargetType;
import com.jp.elearningjp.shared.enums.ReportReason;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

/**
 * Một bình luận hoặc trả lời trên trang kiểm duyệt (hàng chờ báo cáo / tra cứu).
 * Trả kèm email người viết — chỉ dành cho người kiểm duyệt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModerationItemResponse {

    public enum Kind { COMMENT, REPLY }

    Kind kind;
    Long id;
    /** Bình luận gốc (với REPLY là bình luận chứa trả lời; với COMMENT bằng {@code id}). */
    Long commentId;
    String content;
    /** VISIBLE | HIDDEN | DELETED */
    String status;
    Author author;

    CommentTargetType targetType;
    Long targetId;
    /** Tên nội dung được bình luận: tên bài học, từ vựng, chữ kanji, mẫu ngữ pháp. */
    String targetLabel;
    /** Chỉ có với bình luận bài học. */
    Long courseId;
    String courseTitle;

    Instant createdAt;

    /** Số báo cáo (theo trạng thái đang lọc ở hàng chờ; số báo cáo OPEN ở trang tra cứu). */
    long reportCount;
    Instant latestReportAt;
    List<ReasonCount> reasons;
    /** Vài ghi chú gần nhất người báo cáo viết thêm. */
    List<String> recentDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Author {
        Long id;
        String fullName;
        String email;
        String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ReasonCount {
        ReportReason reason;
        long count;
    }
}
