package com.jp.elearningjp.service.notebook.impl;

import com.jp.elearningjp.dto.request.notebook.CreateNotebookGroupRequest;
import com.jp.elearningjp.dto.request.notebook.UpdateNotebookGroupRequest;
import com.jp.elearningjp.dto.response.notebook.NotebookGroupResponse;
import com.jp.elearningjp.dto.response.notebook.NotebookGroupWithNotebooksResponse;
import com.jp.elearningjp.dto.response.notebook.NotebookResponse;
import com.jp.elearningjp.entity.notebook.Notebook;
import com.jp.elearningjp.entity.notebook.NotebookGroup;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.notebook.NotebookMapper;
import com.jp.elearningjp.repository.notebook.NotebookGroupRepository;
import com.jp.elearningjp.repository.notebook.NotebookRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.notebook.NotebookGroupService;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotebookGroupServiceImpl implements NotebookGroupService {

    NotebookGroupRepository notebookGroupRepository;
    NotebookRepository notebookRepository;
    UserRepository userRepository;
    NotebookMapper notebookMapper;
    CurrentUserService currentUserService;

    @Override
    @Transactional
    public NotebookGroupResponse createGroup(CreateNotebookGroupRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (notebookGroupRepository.existsByUserIdAndNameAndDeletedFalse(currentUserId, request.getName().trim())) {
            throw new AppException(ErrorCode.NOTEBOOK_GROUP_NAME_EXISTED);
        }

        NotebookGroup group = notebookMapper.toNotebookGroup(request);
        group.setUser(currentUser);
        group.setName(request.getName().trim());
        if (group.getDisplayOrder() == null) {
            group.setDisplayOrder(0);
        }

        NotebookGroup saved = notebookGroupRepository.save(group);
        return notebookMapper.toNotebookGroupResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookGroupResponse> getMyGroups() {
        Long currentUserId = currentUserService.getCurrentUserId();
        List<NotebookGroup> groups = notebookGroupRepository.findByUserIdAndDeletedFalseOrderByDisplayOrderAsc(currentUserId);
        return groups.stream()
                .map(notebookMapper::toNotebookGroupResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookGroupWithNotebooksResponse> getMyGroupsWithNotebooks() {
        Long currentUserId = currentUserService.getCurrentUserId();
        List<NotebookGroup> groups = notebookGroupRepository.findByUserIdAndDeletedFalseOrderByDisplayOrderAsc(currentUserId);

        return groups.stream()
                .map(group -> {
                    List<Notebook> notebooks = notebookRepository.findByUserIdAndGroupIdAndDeletedFalseOrderByIdDesc(currentUserId, group.getId());
                    List<NotebookResponse> notebookResponses = notebooks.stream()
                            .map(notebookMapper::toNotebookResponse)
                            .toList();

                    return NotebookGroupWithNotebooksResponse.builder()
                            .id(group.getId())
                            .name(group.getName())
                            .displayOrder(group.getDisplayOrder())
                            .notebooks(notebookResponses)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public NotebookGroupResponse getGroupById(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        NotebookGroup group = notebookGroupRepository.findByIdAndUserIdAndDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_GROUP_NOT_FOUND));
        return notebookMapper.toNotebookGroupResponse(group);
    }

    @Override
    @Transactional
    public NotebookGroupResponse updateGroup(Long id, UpdateNotebookGroupRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        NotebookGroup group = notebookGroupRepository.findByIdAndUserIdAndDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_GROUP_NOT_FOUND));

        if (request.getName() != null && !request.getName().trim().equalsIgnoreCase(group.getName())) {
            if (notebookGroupRepository.existsByUserIdAndNameAndDeletedFalse(currentUserId, request.getName().trim())) {
                throw new AppException(ErrorCode.NOTEBOOK_GROUP_NAME_EXISTED);
            }
            group.setName(request.getName().trim());
        }

        notebookMapper.updateNotebookGroupFromRequest(request, group);
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            group.setName(request.getName().trim());
        }
        NotebookGroup saved = notebookGroupRepository.save(group);
        return notebookMapper.toNotebookGroupResponse(saved);
    }

    @Override
    @Transactional
    public void deleteGroup(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        NotebookGroup group = notebookGroupRepository.findByIdAndUserIdAndDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_GROUP_NOT_FOUND));

        // Giải phóng các notebook thuộc nhóm này về trạng thái chưa phân nhóm (group = null)
        List<Notebook> notebooksInGroup = notebookRepository.findByUserIdAndGroupIdAndDeletedFalseOrderByIdDesc(currentUserId, id);
        for (Notebook notebook : notebooksInGroup) {
            notebook.setGroup(null);
        }
        notebookRepository.saveAll(notebooksInGroup);

        group.softDelete();
        notebookGroupRepository.save(group);
    }
}

