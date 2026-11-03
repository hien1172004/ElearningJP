package com.jp.elearningjp.dto.response.notebook;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KanjiPdfMetaResponse {
    private int totalPages;
}

