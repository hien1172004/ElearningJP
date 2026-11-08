package com.jp.elearningjp.service.curriculum.importer;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

/** File Excel (mẫu import hoặc dữ liệu xuất) trả về cho giáo viên tải. */
public record ImportTemplateFile(String fileName, byte[] content) {

    public static final String XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** Response tải file .xlsx (Content-Disposition: attachment). */
    public ResponseEntity<byte[]> toResponse() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(XLSX_MIME))
                .body(content);
    }
}
