package com.jp.elearningjp.service.curriculum.importer;

import java.util.List;

/**
 * Định nghĩa 1 cột trong mẫu Excel.
 *
 * @param key           tên cột in ở dòng tiêu đề (cũng là key trong {@code ImportRowResult.data})
 * @param required      bắt buộc nhập — tiêu đề hiển thị thêm dấu {@code *}
 * @param width         độ rộng cột (số ký tự)
 * @param description   chú thích hiện khi rê chuột vào tiêu đề + liệt kê trong sheet Hướng dẫn
 * @param allowedValues danh sách giá trị cho phép (tạo dropdown), null nếu nhập tự do
 */
public record ImportColumn(String key, boolean required, int width, String description, List<String> allowedValues) {

    public static ImportColumn required(String key, int width, String description) {
        return new ImportColumn(key, true, width, description, null);
    }

    public static ImportColumn optional(String key, int width, String description) {
        return new ImportColumn(key, false, width, description, null);
    }

    public ImportColumn withAllowedValues(List<String> values) {
        return new ImportColumn(key, required, width, description, values);
    }

    /** Tiêu đề in trong file, vd {@code word*}. */
    public String header() {
        return required ? key + "*" : key;
    }
}
