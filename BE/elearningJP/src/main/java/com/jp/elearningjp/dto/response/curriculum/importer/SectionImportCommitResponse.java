package com.jp.elearningjp.dto.response.curriculum.importer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Kết quả ghi dữ liệu import. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SectionImportCommitResponse {
    /** Số item (kanji/từ vựng/ngữ pháp) đã thêm. */
    int importedItems;
    /** Số dòng con đã thêm (từ liên quan / câu ví dụ). */
    int importedChildren;
}
