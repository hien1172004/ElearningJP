package com.jp.elearningjp.dto.response.comment;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Response danh sách comment theo target — dùng cho endpoint phân trang.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentListResponse {

    List<CommentResponse> items;
    long total;
    int page;
    int size;
    int totalPages;
}
