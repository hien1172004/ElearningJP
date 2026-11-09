package com.jp.elearningjp.dto.response.notebook;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Id các bản ghi từ điển (từ vựng / chữ Hán / ngữ pháp) đã có trong ít nhất 1 sổ tay của người dùng —
 * FE dùng để hiện trạng thái "đã lưu" trên thẻ bài học / từ điển.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotebookSavedIdsResponse {
    List<Long> vocabularyIds;
    List<Long> characterIds;
    List<Long> grammarIds;
}
