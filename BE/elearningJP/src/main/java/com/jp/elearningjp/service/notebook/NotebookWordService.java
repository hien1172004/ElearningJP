package com.jp.elearningjp.service.notebook;

import com.jp.elearningjp.dto.request.notebook.CreateNotebookWordRequest;
import com.jp.elearningjp.dto.request.notebook.UpdateNotebookWordRequest;
import com.jp.elearningjp.dto.response.notebook.ExcelImportResultResponse;
import com.jp.elearningjp.dto.response.notebook.NotebookStatsResponse;
import com.jp.elearningjp.dto.response.notebook.NotebookWordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface NotebookWordService {

    NotebookWordResponse addWord(Long notebookId, CreateNotebookWordRequest request);

    NotebookWordResponse updateWord(Long notebookId, Long wordId, UpdateNotebookWordRequest request);

    void deleteWord(Long notebookId, Long wordId);

    void batchDeleteWords(Long notebookId, List<Long> wordIds);

    List<NotebookWordResponse> getWords(Long notebookId);

    Page<NotebookWordResponse> searchWords(Long notebookId, String keyword, Pageable pageable);

    List<NotebookWordResponse> getShuffledWords(Long notebookId);

    ExcelImportResultResponse importWordsFromExcel(Long notebookId, MultipartFile file);

    byte[] exportWordsToExcel(Long notebookId);

    NotebookStatsResponse getStatsOverview();

    List<com.jp.elearningjp.dto.response.notebook.NotebookReviewItemResponse> getReviewItems(Long notebookId, Integer limit);

    com.jp.elearningjp.dto.response.notebook.NotebookReviewSubmitResponse submitReview(Long notebookId, com.jp.elearningjp.dto.request.notebook.NotebookReviewSubmitRequest request);
}

