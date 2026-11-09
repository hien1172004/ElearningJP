package com.jp.elearningjp.dto.response.notebook;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/** Kết quả lưu kiến thức bài học vào sổ tay. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SaveLessonItemsResponse {
    Long notebookId;
    String notebookName;
    /** true nếu sổ tay được tạo mới trong lần lưu này. */
    boolean createdNotebook;
    int addedCount;
    /** Số mục bỏ qua vì sổ tay đã có sẵn. */
    int skippedCount;
    int addedVocab;
    int addedKanji;
    int addedGrammar;
}
