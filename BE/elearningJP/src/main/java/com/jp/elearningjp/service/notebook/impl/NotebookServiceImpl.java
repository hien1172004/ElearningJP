package com.jp.elearningjp.service.notebook.impl;

import com.jp.elearningjp.dto.request.notebook.CreateNotebookRequest;
import com.jp.elearningjp.dto.request.notebook.UpdateNotebookRequest;
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
import com.jp.elearningjp.service.notebook.NotebookService;
import com.jp.elearningjp.entity.notebook.NotebookLike;
import com.jp.elearningjp.entity.notebook.NotebookView;
import com.jp.elearningjp.entity.notebook.NotebookWord;
import com.jp.elearningjp.repository.notebook.NotebookLikeRepository;
import com.jp.elearningjp.repository.notebook.NotebookViewRepository;
import com.jp.elearningjp.repository.notebook.NotebookWordRepository;
import com.jp.elearningjp.shared.enums.MemoryLevel;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import com.jp.elearningjp.shared.util.CurrentUserService;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotebookServiceImpl implements NotebookService {

    NotebookRepository notebookRepository;
    NotebookGroupRepository notebookGroupRepository;
    UserRepository userRepository;
    NotebookMapper notebookMapper;
    CurrentUserService currentUserService;

    NotebookWordRepository notebookWordRepository;
    NotebookLikeRepository notebookLikeRepository;
    NotebookViewRepository notebookViewRepository;


    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();
    public static final int MIN_COMMUNITY_WORDS = 20;

    @Override
    @Transactional
    public NotebookResponse createNotebook(CreateNotebookRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        NotebookGroup group = null;
        if (request.getGroupId() != null) {
            group = notebookGroupRepository.findByIdAndUserIdAndDeletedFalse(request.getGroupId(), currentUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_GROUP_NOT_FOUND));
        }

        Notebook notebook = notebookMapper.toNotebook(request);
        notebook.setUser(currentUser);
        notebook.setGroup(group);
        notebook.setWordCount(0);
        notebook.setLikeCount(0);
        notebook.setViewCount(0);
        notebook.setDownloadCount(0);
        notebook.setSrEnabled(false);
        if (Boolean.TRUE.equals(notebook.getIsPublic())) {
            notebook.setSharingEnabled(true);
        } else {
            notebook.setSharingEnabled(false);
        }
        notebook.setShareCode(generateUniqueShareCode());

        Notebook saved = notebookRepository.save(notebook);
        return notebookMapper.toNotebookResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookResponse> getMyNotebooks() {
        Long currentUserId = currentUserService.getCurrentUserId();
        List<Notebook> notebooks = notebookRepository.findByUserIdAndDeletedFalseOrderByIdDesc(currentUserId);
        return notebooks.stream()
                .map(notebookMapper::toNotebookResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookResponse> getMyUngroupedNotebooks() {
        Long currentUserId = currentUserService.getCurrentUserId();
        List<Notebook> notebooks = notebookRepository.findUngroupedNotebooks(currentUserId);
        return notebooks.stream()
                .map(notebookMapper::toNotebookResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotebookResponse> getMyNotebooksByGroup(Long groupId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        List<Notebook> notebooks = notebookRepository.findByUserIdAndGroupIdAndDeletedFalseOrderByIdDesc(currentUserId, groupId);
        return notebooks.stream()
                .map(notebookMapper::toNotebookResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotebookResponse> searchMyNotebooks(String keyword, String jlptLevel, Long groupId, Pageable pageable) {
        Long currentUserId = currentUserService.getCurrentUserId();

        Specification<Notebook> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), currentUserId));
            predicates.add(cb.isFalse(root.get("deleted")));

            if (keyword != null && !keyword.trim().isEmpty()) {
                String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(nameLike, descLike));
            }

            if (jlptLevel != null && !jlptLevel.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("jlptLevel")), jlptLevel.trim().toUpperCase()));
            }

            if (groupId != null) {
                predicates.add(cb.equal(root.get("group").get("id"), groupId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Notebook> page = notebookRepository.findAll(spec, pageable);
        return page.map(notebookMapper::toNotebookResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public NotebookResponse getNotebookDetail(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));
        return notebookMapper.toNotebookResponse(notebook);
    }

    @Override
    @Transactional
    public NotebookResponse updateNotebook(Long id, UpdateNotebookRequest request) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        if (request.getGroupId() != null) {
            NotebookGroup group = notebookGroupRepository.findByIdAndUserIdAndDeletedFalse(request.getGroupId(), currentUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_GROUP_NOT_FOUND));
            notebook.setGroup(group);
        }

        notebookMapper.updateNotebookFromRequest(request, notebook);
        if (request.getIsPublic() != null) {
            boolean enabled = Boolean.TRUE.equals(request.getIsPublic());
            notebook.setIsPublic(enabled);
            notebook.setSharingEnabled(enabled);
            if (enabled && (notebook.getShareCode() == null || notebook.getShareCode().isBlank())) {
                notebook.setShareCode(generateUniqueShareCode());
            }
        }
        Notebook saved = notebookRepository.save(notebook);
        return notebookMapper.toNotebookResponse(saved);
    }

    @Override
    @Transactional
    public void deleteNotebook(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        notebook.softDelete();
        notebookRepository.save(notebook);
    }

    @Override
    @Transactional
    public NotebookResponse moveToGroup(Long id, Long targetGroupId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        if (targetGroupId == null) {
            notebook.setGroup(null);
        } else {
            NotebookGroup group = notebookGroupRepository.findByIdAndUserIdAndDeletedFalse(targetGroupId, currentUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_GROUP_NOT_FOUND));
            notebook.setGroup(group);
        }

        Notebook saved = notebookRepository.save(notebook);
        return notebookMapper.toNotebookResponse(saved);
    }

    private String generateUniqueShareCode() {
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int j = 0; j < CODE_LENGTH; j++) {
                sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
            }
            String code = sb.toString();
            if (!notebookRepository.existsByShareCode(code)) {
                return code;
            }
        }
        return Long.toString(System.currentTimeMillis(), 36);
    }

    @Override
    @Transactional
    public NotebookResponse toggleSharing(Long id, boolean enabled) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        notebook.setSharingEnabled(enabled);
        notebook.setIsPublic(enabled); // Chuẩn Mazii: Bật chia sẻ là hiển thị lên Khám phá cộng đồng
        if (enabled && (notebook.getShareCode() == null || notebook.getShareCode().isBlank())) {
            notebook.setShareCode(generateUniqueShareCode());
        }

        Notebook saved = notebookRepository.save(notebook);
        return notebookMapper.toNotebookResponse(saved);
    }

    @Override
    @Transactional
    public NotebookResponse togglePublic(Long id, boolean isPublic) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Notebook notebook = notebookRepository.findByIdAndUserIdAndDeletedFalse(id, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        notebook.setIsPublic(isPublic);
        notebook.setSharingEnabled(isPublic);
        if (isPublic && (notebook.getShareCode() == null || notebook.getShareCode().isBlank())) {
            notebook.setShareCode(generateUniqueShareCode());
        }

        Notebook saved = notebookRepository.save(notebook);
        return notebookMapper.toNotebookResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NotebookResponse getNotebookByShareCode(String shareCode) {
        Notebook notebook = notebookRepository.findByShareCodeAndDeletedFalse(shareCode)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_SHARE_CODE_NOT_FOUND));

        Long currentUserId = currentUserService.getCurrentUserId();
        if (!notebook.isSharingEnabled() && !notebook.isPublic() && !notebook.getUser().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.NOTEBOOK_SHARE_CODE_NOT_FOUND);
        }

        NotebookResponse response = notebookMapper.toNotebookResponse(notebook);
        response.setIsLiked(notebookLikeRepository.existsByNotebookIdAndUserId(notebook.getId(), currentUserId));

        boolean isOwner = notebook.getUser().getId().equals(currentUserId);
        response.setIsOwner(isOwner);
        if (isOwner) {
            response.setIsImported(true);
            response.setUserNotebookId(notebook.getId());
        } else {
            notebookRepository.findFirstByUserIdAndSourceNotebookIdAndDeletedFalseOrderByIdDesc(currentUserId, notebook.getId())
                    .ifPresentOrElse(
                            cloned -> {
                                response.setIsImported(true);
                                response.setUserNotebookId(cloned.getId());
                            },
                            () -> response.setIsImported(false)
                    );
        }

        return response;
    }

    @Override
    @Transactional
    public NotebookResponse cloneNotebookByShareCode(String shareCode, Long targetGroupId) {
        Long currentUserId = currentUserService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Notebook original = notebookRepository.findByShareCodeAndDeletedFalse(shareCode)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_SHARE_CODE_NOT_FOUND));

        if (!original.isSharingEnabled() && !original.isPublic() && !original.getUser().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.NOTEBOOK_SHARE_CODE_NOT_FOUND);
        }

        // Không cho phép tự nhân bản chính sổ tay của mình
        if (original.getUser().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.NOTEBOOK_CANNOT_CLONE_OWN);
        }

        // Không cho phép nhân bản lại nếu đã có trong kho cá nhân
        if (notebookRepository.existsByUserIdAndSourceNotebookIdAndDeletedFalse(currentUserId, original.getId())) {
            throw new AppException(ErrorCode.NOTEBOOK_ALREADY_EXISTS);
        }

        NotebookGroup targetGroup = null;
        if (targetGroupId != null) {
            targetGroup = notebookGroupRepository.findByIdAndUserIdAndDeletedFalse(targetGroupId, currentUserId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_GROUP_NOT_FOUND));
        }

        // 1. Tạo bản sao Sổ tay độc lập
        Notebook cloneNotebook = Notebook.builder()
                .user(currentUser)
                .sourceNotebook(original)
                .group(targetGroup)
                .name("Bản sao - " + original.getName())
                .description(original.getDescription())
                .coverColor(original.getCoverColor())
                .coverIcon(original.getCoverIcon())
                .jlptLevel(original.getJlptLevel())
                .wordCount(original.getWordCount())
                .isPublic(false)
                .sharingEnabled(false)
                .shareCode(generateUniqueShareCode())
                .srEnabled(true)
                .build();

        Notebook savedClone = notebookRepository.save(cloneNotebook);

        // 2. Sao chép toàn bộ từ vựng và RESET sạch chỉ số SRS về ban đầu
        List<NotebookWord> originalWords = notebookWordRepository.findByNotebookIdAndDeletedFalseOrderByDisplayOrderAscIdAsc(original.getId());
        List<NotebookWord> clonedWords = new ArrayList<>();

        for (NotebookWord ow : originalWords) {
            NotebookWord cw = NotebookWord.builder()
                    .notebook(savedClone)
                    .vocabulary(ow.getVocabulary())
                    .word(ow.getWord())
                    .reading(ow.getReading())
                    .sinoVietnamese(ow.getSinoVietnamese())
                    .meaning(ow.getMeaning())
                    .partOfSpeech(ow.getPartOfSpeech())
                    .userNote(ow.getUserNote())
                    .audioUrl(ow.getAudioUrl())
                    .jlptLevel(ow.getJlptLevel())
                    .exampleSentence(ow.getExampleSentence())
                    .exampleMeaning(ow.getExampleMeaning())
                    .displayOrder(ow.getDisplayOrder())
                    // Reset sạch chỉ số SRS cho người nhận học lại từ đầu
                    .memoryLevel(MemoryLevel.NOT_LEARNED)
                    .intervalDays(0)
                    .repetitionCount(0)
                    .lapseCount(0)
                    .easeFactor(new BigDecimal("2.50"))
                    .nextReviewAt(Instant.now())
                    .build();
            clonedWords.add(cw);
        }

        if (!clonedWords.isEmpty()) {
            notebookWordRepository.saveAll(clonedWords);
        }

        // 3. Tăng lượt tải của sổ tay gốc
        original.setDownloadCount(original.getDownloadCount() + 1);
        notebookRepository.save(original);

        return notebookMapper.toNotebookResponse(savedClone);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotebookResponse> exploreNotebooks(String keyword, String sortType, Pageable pageable) {
        Long currentUserId = currentUserService.getCurrentUserId();

        // Xây dựng specification: chỉ lấy các sổ tay public và wordCount >= 1
        Specification<Notebook> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Chuẩn Mazii: Chỉ cần sổ tay được bật chia sẻ (isPublic hoặc sharingEnabled)
            predicates.add(cb.or(cb.isTrue(root.get("isPublic")), cb.isTrue(root.get("sharingEnabled"))));
            // Bộ lọc chất lượng cộng đồng: chỉ hiển thị sổ tay có tối thiểu MIN_COMMUNITY_WORDS (20 từ)
            predicates.add(cb.greaterThanOrEqualTo(root.get("wordCount"), MIN_COMMUNITY_WORDS));

            if (keyword != null && !keyword.isBlank()) {
                String searchLike = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), searchLike),
                        cb.like(cb.lower(root.get("description")), searchLike)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Xử lý Sort theo yêu cầu
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if ("downloads".equalsIgnoreCase(sortType)) {
            sort = Sort.by(Sort.Direction.DESC, "downloadCount").and(Sort.by(Sort.Direction.DESC, "id"));
        } else if ("likes".equalsIgnoreCase(sortType)) {
            sort = Sort.by(Sort.Direction.DESC, "likeCount").and(Sort.by(Sort.Direction.DESC, "id"));
        } else if ("views".equalsIgnoreCase(sortType)) {
            sort = Sort.by(Sort.Direction.DESC, "viewCount").and(Sort.by(Sort.Direction.DESC, "id"));
        }

        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<Notebook> pageResult = notebookRepository.findAll(spec, pageRequest);

        List<Long> notebookIds = pageResult.getContent().stream().map(Notebook::getId).toList();
        Map<Long, Long> sourceToClonedIdMap = notebookIds.isEmpty() ? Map.of() :
                notebookRepository.findByUserIdAndSourceNotebookIdInAndDeletedFalse(currentUserId, notebookIds)
                        .stream()
                        .filter(cl -> cl.getSourceNotebook() != null)
                        .collect(Collectors.toMap(cl -> cl.getSourceNotebook().getId(), Notebook::getId, (ex, rep) -> ex));

        return pageResult.map(n -> {
            NotebookResponse resp = notebookMapper.toNotebookResponse(n);
            resp.setIsLiked(notebookLikeRepository.existsByNotebookIdAndUserId(n.getId(), currentUserId));

            boolean isOwner = n.getUser().getId().equals(currentUserId);
            resp.setIsOwner(isOwner);

            Long userClonedId = sourceToClonedIdMap.get(n.getId());
            if (isOwner) {
                resp.setIsImported(true);
                resp.setUserNotebookId(n.getId());
            } else if (userClonedId != null) {
                resp.setIsImported(true);
                resp.setUserNotebookId(userClonedId);
            } else {
                resp.setIsImported(false);
            }
            return resp;
        });
    }

    @Override
    @Transactional
    public NotebookResponse viewPublicNotebookDetail(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Notebook notebook = notebookRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        if (!notebook.isPublic() && !notebook.isSharingEnabled() && !notebook.getUser().getId().equals(currentUserId)) {
            throw new AppException(ErrorCode.NOTEBOOK_FORBIDDEN);
        }

        // Tăng view count & ghi log xem
        notebook.setViewCount(notebook.getViewCount() + 1);
        notebookRepository.save(notebook);

        notebookViewRepository.findByNotebookIdAndUserId(id, currentUserId).ifPresentOrElse(
                nv -> {
                    nv.setViewedAt(Instant.now());
                    notebookViewRepository.save(nv);
                },
                () -> {
                    notebookViewRepository.save(NotebookView.builder()
                            .notebook(notebook)
                            .user(currentUser)
                            .build());
                }
        );

        NotebookResponse resp = notebookMapper.toNotebookResponse(notebook);
        resp.setIsLiked(notebookLikeRepository.existsByNotebookIdAndUserId(id, currentUserId));

        boolean isOwner = notebook.getUser().getId().equals(currentUserId);
        resp.setIsOwner(isOwner);
        if (isOwner) {
            resp.setIsImported(true);
            resp.setUserNotebookId(notebook.getId());
        } else {
            notebookRepository.findFirstByUserIdAndSourceNotebookIdAndDeletedFalseOrderByIdDesc(currentUserId, notebook.getId())
                    .ifPresentOrElse(
                            cloned -> {
                                resp.setIsImported(true);
                                resp.setUserNotebookId(cloned.getId());
                            },
                            () -> resp.setIsImported(false)
                    );
        }
        return resp;
    }

    @Override
    @Transactional
    public boolean toggleLikeNotebook(Long id) {
        Long currentUserId = currentUserService.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Notebook notebook = notebookRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOTEBOOK_NOT_FOUND));

        boolean exists = notebookLikeRepository.existsByNotebookIdAndUserId(id, currentUserId);
        if (exists) {
            notebookLikeRepository.deleteByNotebookIdAndUserId(id, currentUserId);
            notebook.setLikeCount(Math.max(0, notebook.getLikeCount() - 1));
            notebookRepository.save(notebook);
            return false;
        } else {
            notebookLikeRepository.save(NotebookLike.builder()
                    .notebook(notebook)
                    .user(currentUser)
                    .build());
            notebook.setLikeCount(notebook.getLikeCount() + 1);
            notebookRepository.save(notebook);
            return true;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotebookResponse> getFavoriteNotebooks(Pageable pageable) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Page<Long> likedIdsPage = notebookLikeRepository.findLikedNotebookIdsByUserId(currentUserId, pageable);

        if (likedIdsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> ids = likedIdsPage.getContent();
        Map<Long, Notebook> notebookMap = notebookRepository.findByIdInAndDeletedFalse(ids)
                .stream().collect(Collectors.toMap(Notebook::getId, n -> n));

        List<NotebookResponse> responses = ids.stream()
                .filter(notebookMap::containsKey)
                .map(id -> {
                    NotebookResponse resp = notebookMapper.toNotebookResponse(notebookMap.get(id));
                    resp.setIsLiked(true);
                    return resp;
                })
                .toList();

        List<Notebook> loadedNotebooks = ids.stream()
                .filter(notebookMap::containsKey)
                .map(notebookMap::get)
                .toList();
        enrichNotebookResponsesWithOwnershipAndImportStatus(responses, loadedNotebooks, currentUserId);

        return new PageImpl<>(responses, pageable, likedIdsPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotebookResponse> getRecentlyViewedNotebooks(Pageable pageable) {
        Long currentUserId = currentUserService.getCurrentUserId();
        Page<Long> viewedIdsPage = notebookViewRepository.findRecentlyViewedNotebookIdsByUserId(currentUserId, pageable);

        if (viewedIdsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Long> ids = viewedIdsPage.getContent();
        Map<Long, Notebook> notebookMap = notebookRepository.findByIdInAndDeletedFalse(ids)
                .stream().collect(Collectors.toMap(Notebook::getId, n -> n));

        List<NotebookResponse> responses = ids.stream()
                .filter(notebookMap::containsKey)
                .map(id -> {
                    NotebookResponse resp = notebookMapper.toNotebookResponse(notebookMap.get(id));
                    resp.setIsLiked(notebookLikeRepository.existsByNotebookIdAndUserId(id, currentUserId));
                    return resp;
                })
                .toList();

        List<Notebook> loadedNotebooks = ids.stream()
                .filter(notebookMap::containsKey)
                .map(notebookMap::get)
                .toList();
        enrichNotebookResponsesWithOwnershipAndImportStatus(responses, loadedNotebooks, currentUserId);

        return new PageImpl<>(responses, pageable, viewedIdsPage.getTotalElements());
    }

    private void enrichNotebookResponsesWithOwnershipAndImportStatus(
            List<NotebookResponse> responses,
            List<Notebook> notebooks,
            Long currentUserId
    ) {
        if (responses.isEmpty() || notebooks.isEmpty()) {
            return;
        }

        List<Long> notebookIds = notebooks.stream().map(Notebook::getId).toList();
        Map<Long, Long> clonedSourceMap = notebookIds.isEmpty() ? Map.of() :
                notebookRepository.findByUserIdAndSourceNotebookIdInAndDeletedFalse(currentUserId, notebookIds)
                        .stream()
                        .filter(n -> n.getSourceNotebook() != null)
                        .collect(Collectors.toMap(
                                n -> n.getSourceNotebook().getId(),
                                Notebook::getId,
                                (existing, replacement) -> existing
                        ));

        Map<Long, Notebook> notebookById = notebooks.stream()
                .collect(Collectors.toMap(Notebook::getId, n -> n, (ex, rep) -> ex));

        for (NotebookResponse resp : responses) {
            Notebook nb = notebookById.get(resp.getId());
            boolean isOwner = nb != null && nb.getUser() != null && nb.getUser().getId().equals(currentUserId);
            resp.setIsOwner(isOwner);

            if (isOwner) {
                resp.setIsImported(true);
                resp.setUserNotebookId(resp.getId());
            } else if (clonedSourceMap.containsKey(resp.getId())) {
                resp.setIsImported(true);
                resp.setUserNotebookId(clonedSourceMap.get(resp.getId()));
            } else {
                resp.setIsImported(false);
            }
        }
    }

}
