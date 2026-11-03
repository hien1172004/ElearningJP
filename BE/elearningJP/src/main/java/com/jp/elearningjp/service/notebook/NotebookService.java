package com.jp.elearningjp.service.notebook;

import com.jp.elearningjp.dto.request.notebook.CreateNotebookRequest;
import com.jp.elearningjp.dto.request.notebook.UpdateNotebookRequest;
import com.jp.elearningjp.dto.response.notebook.NotebookResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotebookService {

    NotebookResponse createNotebook(CreateNotebookRequest request);

    List<NotebookResponse> getMyNotebooks();

    List<NotebookResponse> getMyUngroupedNotebooks();

    List<NotebookResponse> getMyNotebooksByGroup(Long groupId);

    Page<NotebookResponse> searchMyNotebooks(String keyword, String jlptLevel, Long groupId, Pageable pageable);

    NotebookResponse getNotebookDetail(Long id);

    NotebookResponse updateNotebook(Long id, UpdateNotebookRequest request);

    void deleteNotebook(Long id);

    NotebookResponse moveToGroup(Long id, Long targetGroupId);

    // Chia sẻ & Clone
    NotebookResponse toggleSharing(Long id, boolean enabled);

    NotebookResponse togglePublic(Long id, boolean isPublic);

    NotebookResponse getNotebookByShareCode(String shareCode);

    NotebookResponse cloneNotebookByShareCode(String shareCode, Long targetGroupId);

    // Khám phá Cộng đồng
    Page<NotebookResponse> exploreNotebooks(String keyword, String sort, Pageable pageable);

    NotebookResponse viewPublicNotebookDetail(Long id);

    boolean toggleLikeNotebook(Long id);

    Page<NotebookResponse> getFavoriteNotebooks(Pageable pageable);

    Page<NotebookResponse> getRecentlyViewedNotebooks(Pageable pageable);
}

