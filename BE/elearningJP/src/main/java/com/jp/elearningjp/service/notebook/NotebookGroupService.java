package com.jp.elearningjp.service.notebook;

import com.jp.elearningjp.dto.request.notebook.CreateNotebookGroupRequest;
import com.jp.elearningjp.dto.request.notebook.UpdateNotebookGroupRequest;
import com.jp.elearningjp.dto.response.notebook.NotebookGroupResponse;
import com.jp.elearningjp.dto.response.notebook.NotebookGroupWithNotebooksResponse;

import java.util.List;

public interface NotebookGroupService {

    NotebookGroupResponse createGroup(CreateNotebookGroupRequest request);

    List<NotebookGroupResponse> getMyGroups();

    List<NotebookGroupWithNotebooksResponse> getMyGroupsWithNotebooks();

    NotebookGroupResponse getGroupById(Long id);

    NotebookGroupResponse updateGroup(Long id, UpdateNotebookGroupRequest request);

    void deleteGroup(Long id);
}

