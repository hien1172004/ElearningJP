package com.jp.elearningjp.service.notebook;

import com.jp.elearningjp.dto.response.notebook.KanjiPdfMetaResponse;
import com.jp.elearningjp.dto.response.notebook.KanjiPdfPreviewResponse;

public interface NotebookExportService {

    byte[] exportWordsToExcel(Long notebookId);

    byte[] generateKanjiPracticePdf(Long notebookId);

    KanjiPdfPreviewResponse generateKanjiPracticePdfPreview(Long notebookId, Integer pageNumber);

    KanjiPdfMetaResponse getKanjiPracticePdfMeta(Long notebookId);
}


