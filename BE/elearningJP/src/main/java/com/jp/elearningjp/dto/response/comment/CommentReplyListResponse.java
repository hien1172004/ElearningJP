package com.jp.elearningjp.dto.response.comment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Response danh sách reply của 1 comment — dùng cho endpoint phân trang.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentReplyListResponse {

    List<CommentReplyResponse> items;
    long total;
    int page;
    int size;
    int totalPages;
}
