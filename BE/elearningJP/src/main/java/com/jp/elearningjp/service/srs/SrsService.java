package com.jp.elearningjp.service.srs;

import com.jp.elearningjp.dto.request.srs.SrsBatchCreateRequest;
import com.jp.elearningjp.dto.request.srs.SrsItemCreateRequest;
import com.jp.elearningjp.dto.request.srs.SrsReviewRequest;
import com.jp.elearningjp.dto.response.srs.SrsDueItemResponse;
import com.jp.elearningjp.dto.response.srs.SrsReviewResultResponse;
import com.jp.elearningjp.dto.response.srs.SrsStatsResponse;

import java.util.List;

public interface SrsService {

    List<SrsDueItemResponse> getDueCards(Long userId, int limit);
    List<SrsDueItemResponse> getDueCards(int limit);

    SrsDueItemResponse addItem(Long userId, SrsItemCreateRequest request);
    SrsDueItemResponse addItem(SrsItemCreateRequest request);

    int batchAddFromLesson(Long userId, Long lessonId);
    int batchAddFromLesson(Long lessonId);

    int batchAdd(Long userId, SrsBatchCreateRequest request);
    int batchAdd(SrsBatchCreateRequest request);

    SrsReviewResultResponse reviewCard(Long userId, Long srsItemId, SrsReviewRequest request);
    SrsReviewResultResponse reviewCard(Long srsItemId, SrsReviewRequest request);

    SrsStatsResponse getStatistics(Long userId);
    SrsStatsResponse getStatistics();
}
