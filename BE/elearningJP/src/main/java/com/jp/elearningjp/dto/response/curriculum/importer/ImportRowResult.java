package com.jp.elearningjp.dto.response.curriculum.importer;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kết quả kiểm tra 1 dòng trong file Excel.
 * {@code data} chứa giá trị đã đọc (theo tên cột) + giá trị tra từ điển, để FE hiển thị bảng xem trước.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportRowResult {
    /** Tên sheet chứa dòng này. */
    String sheet;
    /** Số dòng hiển thị trong Excel (bắt đầu từ 1, dòng tiêu đề là 1). */
    int rowNumber;
    @Builder.Default
    ImportRowStatus status = ImportRowStatus.OK;
    @Builder.Default
    Map<String, Object> data = new LinkedHashMap<>();
    @Builder.Default
    List<String> messages = new ArrayList<>();

    public void error(String message) {
        messages.add(message);
        status = ImportRowStatus.ERROR;
    }

    public void warn(String message) {
        messages.add(message);
        if (status == ImportRowStatus.OK) {
            status = ImportRowStatus.WARNING;
        }
    }

    public boolean hasError() {
        return status == ImportRowStatus.ERROR;
    }
}
