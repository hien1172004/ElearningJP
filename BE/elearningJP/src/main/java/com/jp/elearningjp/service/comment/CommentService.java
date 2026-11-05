package com.jp.elearningjp.service.comment;

import com.jp.elearningjp.shared.event.comment.CommentDeletedEvent;
import com.jp.elearningjp.dto.request.comment.CommentCreateRequest;
import com.jp.elearningjp.dto.request.comment.CommentReplyCreateRequest;
import com.jp.elearningjp.dto.request.comment.CommentUpdateRequest;
import com.jp.elearningjp.dto.response.comment.CommentDetailResponse;
import com.jp.elearningjp.dto.response.comment.CommentListResponse;
import com.jp.elearningjp.dto.response.comment.CommentLocationResponse;
import com.jp.elearningjp.dto.response.comment.CommentReactionResponse;
import com.jp.elearningjp.dto.response.comment.CommentReplyListResponse;
import com.jp.elearningjp.dto.response.comment.CommentReplyResponse;
import com.jp.elearningjp.dto.response.comment.CommentResponse;
import com.jp.elearningjp.entity.comment.Comment;
import com.jp.elearningjp.entity.comment.CommentReply;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.comment.CommentDislikeRepository;
import com.jp.elearningjp.repository.comment.CommentLikeRepository;
import com.jp.elearningjp.repository.comment.CommentReplyDislikeRepository;
import com.jp.elearningjp.repository.comment.CommentReplyLikeRepository;
import com.jp.elearningjp.repository.comment.CommentReplyRepository;
import com.jp.elearningjp.repository.comment.CommentReportRepository;
import com.jp.elearningjp.repository.comment.CommentRepository;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.shared.enums.CommentTargetType;
import com.jp.elearningjp.shared.enums.ReportStatus;
import com.jp.elearningjp.shared.event.comment.CommentModeratedEvent;
import com.jp.elearningjp.shared.event.comment.CommentReactionEvent;
import com.jp.elearningjp.shared.event.comment.CommentRepliedEvent;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service quản lý {@link Comment} + {@link CommentReply}.
 *
 * <p>Module này phục vụ bình luận cho 4 loại đối tượng (LESSON / VOCABULARY /
 * KANJI / GRAMMAR) thông qua cặp {@code (targetType, targetId)}.</p>
 *
 * <p>Các cột {@code like_count}, {@code dislike_count}, {@code reply_count} chỉ được đổi bằng
 * UPDATE tăng/giảm nguyên tử ở DB (không set trên entity), và entity dùng {@code @DynamicUpdate}
 * nên sửa/xoá comment không ghi đè bộ đếm đang tăng đồng thời.</p>
 *
 * <p>Phân trang bắt đầu từ 1 như phần còn lại của hệ thống.</p>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CommentService {

    /** Số reply tối đa trả kèm trong chi tiết comment; xem thêm qua GET /comments/{id}/replies. */
    static final int DETAIL_REPLY_LIMIT = 50;
    private static final int MAX_PAGE_SIZE = 100;

    CommentRepository commentRepository;
    CommentLikeRepository commentLikeRepository;
    CommentDislikeRepository commentDislikeRepository;
    CommentReplyRepository commentReplyRepository;
    CommentReplyLikeRepository commentReplyLikeRepository;
    CommentReplyDislikeRepository commentReplyDislikeRepository;
    CommentReportRepository commentReportRepository;

    LessonRepository lessonRepository;
    VocabularyRepository vocabularyRepository;
    WritingCharacterRepository writingCharacterRepository;
    GrammarPointRepository grammarPointRepository;

    CurrentUserService currentUserService;
    CourseAccessGuard courseAccessGuard;
    ApplicationEventPublisher eventPublisher;

    // ============================================================
    // READ
    // ============================================================

    @Transactional(readOnly = true)
    public CommentListResponse getCommentsByTarget(CommentTargetType targetType,
                                                   Long targetId,
                                                   int page,
                                                   int size) {
        ensureTargetExists(targetType, targetId);
        Page<Comment> result = commentRepository.findByTarget(targetType, targetId, toPageable(page, size));

        Long currentUserId = currentUserIdOrNull();
        boolean moderator = isModerator(targetType, targetId, currentUserId);
        List<CommentResponse> items = toCommentResponses(result.getContent(), currentUserId, moderator);

        return CommentListResponse.builder()
                .items(items)
                .total(result.getTotalElements())
                .page(result.getNumber() + 1)
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    /**
     * Chi tiết comment kèm tối đa {@link #DETAIL_REPLY_LIMIT} reply đầu tiên.
     * Reply còn lại lấy qua {@link #getRepliesByComment}.
     */
    @Transactional(readOnly = true)
    public CommentDetailResponse getCommentDetail(Long commentId) {
        Comment comment = getVisibleComment(commentId);
        Long currentUserId = currentUserIdOrNull();

        List<CommentReply> replies = commentReplyRepository
                .findByCommentIdOrderByCreatedAtAsc(commentId, PageRequest.of(0, DETAIL_REPLY_LIMIT))
                .getContent();

        boolean moderator = isModerator(comment.getTargetType(), comment.getTargetId(), currentUserId);
        return CommentDetailResponse.builder()
                .comment(toCommentResponses(List.of(comment), currentUserId, moderator).get(0))
                .replies(toReplyResponses(replies, currentUserId, moderator))
                .build();
    }

    @Transactional(readOnly = true)
    public CommentReplyListResponse getRepliesByComment(Long commentId,
                                                        int page,
                                                        int size) {
        Comment comment = getVisibleComment(commentId);
        Page<CommentReply> result = commentReplyRepository
                .findByCommentIdOrderByCreatedAtAsc(commentId, toPageable(page, size));

        Long currentUserId = currentUserIdOrNull();
        boolean moderator = isModerator(comment.getTargetType(), comment.getTargetId(), currentUserId);
        return CommentReplyListResponse.builder()
                .items(toReplyResponses(result.getContent(), currentUserId, moderator))
                .total(result.getTotalElements())
                .page(result.getNumber() + 1)
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    /**
     * Vị trí của comment để FE mở thẳng tới nó (dùng khi bấm thông báo).
     *
     * <p>Comment phải còn hiển thị và target còn xem được (cùng điều kiện như đọc comment).
     * Reply không còn / không thuộc comment này thì bỏ qua ({@code replyId = null}) thay vì
     * báo lỗi — người dùng vẫn được đưa tới comment gốc.</p>
     */
    @Transactional(readOnly = true)
    public CommentLocationResponse getCommentLocation(Long commentId, Long replyId) {
        Comment comment = getVisibleComment(commentId);
        CommentTargetType targetType = comment.getTargetType();
        Long targetId = comment.getTargetId();

        Long visibleReplyId = replyId == null ? null : commentReplyRepository.findVisibleById(replyId)
                .filter(reply -> reply.getComment().getId().equals(commentId))
                .map(CommentReply::getId)
                .orElse(null);

        CommentLocationResponse.CommentLocationResponseBuilder location = CommentLocationResponse.builder()
                .commentId(commentId)
                .replyId(visibleReplyId)
                .targetType(targetType)
                .targetId(targetId)
                .position(commentRepository.countVisibleNewerThan(targetType, targetId, comment.getCreatedAt()));

        // getVisibleComment đã kiểm tra target còn tồn tại, nên các findBy dưới đây luôn có giá trị.
        switch (targetType) {
            case LESSON -> lessonRepository.findByIdAndDeletedFalse(targetId)
                    .ifPresent(lesson -> location.courseId(lesson.getCourse().getId()));
            case VOCABULARY -> vocabularyRepository.findByIdAndDeletedFalse(targetId)
                    .ifPresent(vocab -> location.keyword(vocab.getWord()));
            case KANJI -> writingCharacterRepository.findByIdAndDeletedFalse(targetId)
                    .ifPresent(kanji -> location.keyword(kanji.getCharacter()));
            case GRAMMAR -> grammarPointRepository.findByIdAndDeletedFalse(targetId)
                    .ifPresent(grammar -> location.keyword(grammar.getTitle()));
        }
        return location.build();
    }

    // ============================================================
    // CREATE
    // ============================================================

    @Transactional
    public CommentResponse createComment(CommentTargetType targetType,
                                         Long targetId,
                                         CommentCreateRequest request) {
        ensureTargetExists(targetType, targetId);
        User user = currentUserService.getCurrentUser();

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new AppException(ErrorCode.COMMENT_CONTENT_EMPTY);
        }

        Comment comment = Comment.builder()
                .targetType(targetType)
                .targetId(targetId)
                .user(user)
                .content(request.getContent().trim())
                .likeCount(0)
                .dislikeCount(0)
                .replyCount(0)
                .build();
        Comment saved = commentRepository.save(comment);

        log.info("[COMMENT] Created comment id={} targetType={} targetId={} by userId={}",
                saved.getId(), targetType, targetId, user.getId());

        return toCommentResponse(saved, user.getId(), false, false,
                isModerator(targetType, targetId, user.getId()));
    }

    @Transactional
    public CommentReplyResponse addReply(Long commentId, CommentReplyCreateRequest request) {
        Comment comment = getVisibleComment(commentId);
        User user = currentUserService.getCurrentUser();

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new AppException(ErrorCode.COMMENT_CONTENT_EMPTY);
        }

        CommentReply parent = null;
        if (request.getParentReplyId() != null) {
            parent = commentReplyRepository.findVisibleById(request.getParentReplyId())
                    .orElseThrow(() -> new AppException(ErrorCode.COMMENT_REPLY_NOT_FOUND));
            if (!parent.getComment().getId().equals(comment.getId())) {
                throw new AppException(ErrorCode.COMMENT_REPLY_INVALID_PARENT);
            }
        }

        CommentReply reply = CommentReply.builder()
                .comment(comment)
                .user(user)
                .parentReply(parent)
                .content(request.getContent().trim())
                .likeCount(0)
                .dislikeCount(0)
                .build();
        CommentReply saved = commentReplyRepository.save(reply);

        // Chỉ tăng ở DB. Không set replyCount trên entity: entity bẩn sẽ ghi đè
        // reply_count bằng giá trị cũ + 1, làm mất lượt đếm của người trả lời cùng lúc.
        commentRepository.incrementReplyCount(comment.getId());

        log.info("[COMMENT] Added reply id={} to commentId={} by userId={} parentReplyId={}",
                saved.getId(), commentId, user.getId(), parent != null ? parent.getId() : null);

        // Phát event cho module thông báo — listener chạy AFTER_COMMIT nên chỉ thông báo
        // khi reply đã thực sự được commit. Mọi field phải resolve TRƯỚC khi publish vì
        // lúc listener chạy, persistence context đã đóng (không lazy-load được nữa).
        Long recipientId = comment.getUser() != null ? comment.getUser().getId() : null;
        eventPublisher.publishEvent(CommentRepliedEvent.builder()
                .recipientUserId(recipientId)
                .parentReplyUserId(parent != null ? ownerId(parent.getUser()) : null)
                .replierUserId(user.getId())
                .replierName(user.getFullName())
                .commentId(comment.getId())
                .replyId(saved.getId())
                .targetType(comment.getTargetType())
                .targetId(comment.getTargetId())
                .replyContent(saved.getContent())
                .build());

        return toReplyResponse(saved, user.getId(), false, false,
                isModerator(comment.getTargetType(), comment.getTargetId(), user.getId()));
    }

    // ============================================================
    // UPDATE / DELETE
    // ============================================================

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request) {
        Comment comment = getVisibleComment(commentId);
        User user = currentUserService.getCurrentUser();
        // Sửa nội dung: chỉ chủ bình luận (admin chỉ được xoá, không sửa lời người khác).
        if (!isOwner(comment.getUser(), user.getId())) {
            throw new AppException(ErrorCode.COMMENT_FORBIDDEN);
        }

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new AppException(ErrorCode.COMMENT_CONTENT_EMPTY);
        }

        comment.setContent(request.getContent().trim());
        Comment saved = commentRepository.save(comment);

        log.info("[COMMENT] Updated comment id={} by userId={}", commentId, user.getId());
        return toCommentResponses(List.of(saved), user.getId(),
                isModerator(saved.getTargetType(), saved.getTargetId(), user.getId())).get(0);
    }

    /** Xoá mềm comment — chủ bình luận hoặc admin (gỡ bình luận vi phạm). */
    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = getActiveComment(commentId);
        User user = currentUserService.getCurrentUser();
        ensureOwnerOrAdmin(comment.getUser(), user.getId(), ErrorCode.COMMENT_FORBIDDEN);

        comment.softDelete();
        commentRepository.save(comment);
        // Nội dung đã gỡ → các báo cáo đang mở coi như đã xử lý
        commentReportRepository.closeOpenReportsOfComment(commentId, ReportStatus.RESOLVED.name(), user.getId(), Instant.now());
        // Dọn thông báo trỏ tới comment đã xoá (listener AFTER_COMMIT ở module thông báo)
        eventPublisher.publishEvent(CommentDeletedEvent.builder().commentId(commentId).build());
        // Admin gỡ bình luận của người khác → báo cho người viết
        if (!isOwner(comment.getUser(), user.getId())) {
            publishModeratedDeleted(comment.getUser(), user, CommentModeratedEvent.Scope.COMMENT, comment.getContent(), comment);
        }

        log.info("[COMMENT] Soft-deleted comment id={} by userId={} (admin={})",
                commentId, user.getId(), !isOwner(comment.getUser(), user.getId()));
    }

    /**
     * Xoá mềm reply cùng toàn bộ reply con cháu — chủ reply hoặc admin.
     * Không xoá con thì chúng mất cha và FE đẩy chúng lên cấp gốc.
     */
    @Transactional
    public void deleteReply(Long replyId) {
        CommentReply reply = commentReplyRepository.findByIdAndDeletedFalse(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_REPLY_NOT_FOUND));
        User user = currentUserService.getCurrentUser();
        ensureOwnerOrAdmin(reply.getUser(), user.getId(), ErrorCode.COMMENT_REPLY_FORBIDDEN);

        List<Long> subtreeIds = commentReplyRepository.findActiveSubtreeIds(replyId);
        // reply_count chỉ đếm reply đang hiển thị: reply bị ẩn (HIDDEN) đã được trừ lúc ẩn
        int visible = commentReplyRepository.countVisibleActiveInSubtree(replyId);
        int deleted = commentReplyRepository.softDeleteSubtree(replyId, Instant.now());
        commentRepository.decrementReplyCountBy(reply.getComment().getId(), visible);
        if (!subtreeIds.isEmpty()) {
            commentReportRepository.closeOpenReportsOfReplies(subtreeIds, ReportStatus.RESOLVED.name(), user.getId(), Instant.now());
            eventPublisher.publishEvent(CommentDeletedEvent.builder()
                    .commentId(reply.getComment().getId())
                    .replyIds(subtreeIds)
                    .build());
        }
        if (!isOwner(reply.getUser(), user.getId())) {
            publishModeratedDeleted(reply.getUser(), user, CommentModeratedEvent.Scope.REPLY, reply.getContent(), reply.getComment());
        }

        log.info("[COMMENT] Soft-deleted reply id={} (+{} descendant(s)) by userId={}",
                replyId, Math.max(0, deleted - 1), user.getId());
    }

    // ============================================================
    // TOGGLE LIKE / DISLIKE  (cho cả comment và reply)
    //
    // Thêm reaction bằng INSERT ... ON CONFLICT DO NOTHING và gỡ bằng DELETE trả về số dòng:
    // bộ đếm chỉ đổi khi DB thực sự đổi, nên bấm 2 lần thật nhanh không còn lỗi 500 do trùng
    // khoá chính và không đếm lệch.
    // ============================================================

    @Transactional
    public CommentReactionResponse toggleLike(Long commentId) {
        Comment comment = getVisibleComment(commentId);
        User user = currentUserService.getCurrentUser();
        Long userId = user.getId();

        if (commentLikeRepository.deleteReaction(userId, commentId) > 0) {
            commentRepository.decrementLikeCount(commentId);
            log.info("[COMMENT] Unliked comment id={} by userId={}", commentId, userId);
            return buildCommentReaction(commentId, false, false);
        }
        if (commentDislikeRepository.deleteReaction(userId, commentId) > 0) {
            commentRepository.decrementDislikeCount(commentId);
        }
        if (commentLikeRepository.insertIfAbsent(userId, commentId) > 0) {
            commentRepository.incrementLikeCount(commentId);
            log.info("[COMMENT] Liked comment id={} by userId={}", commentId, userId);
            publishReactionEvent(ownerId(comment.getUser()), user, true,
                    CommentReactionEvent.ReactionScope.COMMENT, comment.getId(), null,
                    comment.getTargetType(), comment.getTargetId());
        }
        return buildCommentReaction(commentId, true, false);
    }

    @Transactional
    public CommentReactionResponse toggleDislike(Long commentId) {
        Comment comment = getVisibleComment(commentId);
        User user = currentUserService.getCurrentUser();
        Long userId = user.getId();

        if (commentDislikeRepository.deleteReaction(userId, commentId) > 0) {
            commentRepository.decrementDislikeCount(commentId);
            log.info("[COMMENT] Undisliked comment id={} by userId={}", commentId, userId);
            return buildCommentReaction(commentId, false, false);
        }
        if (commentLikeRepository.deleteReaction(userId, commentId) > 0) {
            commentRepository.decrementLikeCount(commentId);
        }
        if (commentDislikeRepository.insertIfAbsent(userId, commentId) > 0) {
            commentRepository.incrementDislikeCount(commentId);
            log.info("[COMMENT] Disliked comment id={} by userId={}", commentId, userId);
            publishReactionEvent(ownerId(comment.getUser()), user, false,
                    CommentReactionEvent.ReactionScope.COMMENT, comment.getId(), null,
                    comment.getTargetType(), comment.getTargetId());
        }
        return buildCommentReaction(commentId, false, true);
    }

    @Transactional
    public CommentReactionResponse toggleReplyLike(Long replyId) {
        CommentReply reply = getVisibleReply(replyId);
        User user = currentUserService.getCurrentUser();
        Long userId = user.getId();

        if (commentReplyLikeRepository.deleteReaction(userId, replyId) > 0) {
            commentReplyRepository.decrementLikeCount(replyId);
            log.info("[COMMENT] Unliked reply id={} by userId={}", replyId, userId);
            return buildReplyReaction(replyId, false, false);
        }
        if (commentReplyDislikeRepository.deleteReaction(userId, replyId) > 0) {
            commentReplyRepository.decrementDislikeCount(replyId);
        }
        if (commentReplyLikeRepository.insertIfAbsent(userId, replyId) > 0) {
            commentReplyRepository.incrementLikeCount(replyId);
            log.info("[COMMENT] Liked reply id={} by userId={}", replyId, userId);
            publishReactionEvent(ownerId(reply.getUser()), user, true,
                    CommentReactionEvent.ReactionScope.REPLY, reply.getComment().getId(), reply.getId(),
                    reply.getComment().getTargetType(), reply.getComment().getTargetId());
        }
        return buildReplyReaction(replyId, true, false);
    }

    @Transactional
    public CommentReactionResponse toggleReplyDislike(Long replyId) {
        CommentReply reply = getVisibleReply(replyId);
        User user = currentUserService.getCurrentUser();
        Long userId = user.getId();

        if (commentReplyDislikeRepository.deleteReaction(userId, replyId) > 0) {
            commentReplyRepository.decrementDislikeCount(replyId);
            log.info("[COMMENT] Undisliked reply id={} by userId={}", replyId, userId);
            return buildReplyReaction(replyId, false, false);
        }
        if (commentReplyLikeRepository.deleteReaction(userId, replyId) > 0) {
            commentReplyRepository.decrementLikeCount(replyId);
        }
        if (commentReplyDislikeRepository.insertIfAbsent(userId, replyId) > 0) {
            commentReplyRepository.incrementDislikeCount(replyId);
            log.info("[COMMENT] Disliked reply id={} by userId={}", replyId, userId);
            publishReactionEvent(ownerId(reply.getUser()), user, false,
                    CommentReactionEvent.ReactionScope.REPLY, reply.getComment().getId(), reply.getId(),
                    reply.getComment().getTargetType(), reply.getComment().getTargetId());
        }
        return buildReplyReaction(replyId, false, true);
    }

    // ============================================================
    // MAPPING
    // ============================================================

    /** Trạng thái like/dislike của user lấy bằng 2 query cho cả trang, không phải 2 query mỗi comment. */
    private List<CommentResponse> toCommentResponses(List<Comment> comments, Long currentUserId, boolean moderator) {
        Set<Long> liked = Set.of();
        Set<Long> disliked = Set.of();
        if (currentUserId != null && !comments.isEmpty()) {
            List<Long> ids = comments.stream().map(Comment::getId).toList();
            liked = toSet(commentLikeRepository.findReactedIds(currentUserId, ids));
            disliked = toSet(commentDislikeRepository.findReactedIds(currentUserId, ids));
        }
        Set<Long> finalLiked = liked;
        Set<Long> finalDisliked = disliked;
        return comments.stream()
                .map(c -> toCommentResponse(c, currentUserId,
                        finalLiked.contains(c.getId()), finalDisliked.contains(c.getId()), moderator))
                .toList();
    }

    private List<CommentReplyResponse> toReplyResponses(List<CommentReply> replies, Long currentUserId, boolean moderator) {
        Set<Long> liked = Set.of();
        Set<Long> disliked = Set.of();
        if (currentUserId != null && !replies.isEmpty()) {
            List<Long> ids = replies.stream().map(CommentReply::getId).toList();
            liked = toSet(commentReplyLikeRepository.findReactedIds(currentUserId, ids));
            disliked = toSet(commentReplyDislikeRepository.findReactedIds(currentUserId, ids));
        }
        Set<Long> finalLiked = liked;
        Set<Long> finalDisliked = disliked;
        return replies.stream()
                .map(r -> toReplyResponse(r, currentUserId,
                        finalLiked.contains(r.getId()), finalDisliked.contains(r.getId()), moderator))
                .toList();
    }

    private CommentResponse toCommentResponse(Comment c, Long currentUserId, boolean liked, boolean disliked,
                                              boolean moderator) {
        boolean isOwner = isOwner(c.getUser(), currentUserId);
        return CommentResponse.builder()
                .id(c.getId())
                .targetType(c.getTargetType())
                .targetId(c.getTargetId())
                .user(toUserInfo(c.getUser()))
                .content(c.getContent())
                .status(c.getStatus())
                .likeCount(c.getLikeCount())
                .dislikeCount(c.getDislikeCount())
                .replyCount(c.getReplyCount())
                .liked(liked)
                .disliked(disliked)
                .owner(isOwner)
                .deletable(isOwner || isAdmin(currentUserId))
                .moderator(moderator)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private CommentReplyResponse toReplyResponse(CommentReply r, Long currentUserId, boolean liked, boolean disliked,
                                                 boolean moderator) {
        boolean isOwner = isOwner(r.getUser(), currentUserId);
        return CommentReplyResponse.builder()
                .id(r.getId())
                .commentId(r.getComment().getId())
                .parentReplyId(r.getParentReply() != null ? r.getParentReply().getId() : null)
                .user(toUserInfo(r.getUser()))
                .content(r.getContent())
                .status(r.getStatus())
                .likeCount(r.getLikeCount())
                .dislikeCount(r.getDislikeCount())
                .liked(liked)
                .disliked(disliked)
                .owner(isOwner)
                .deletable(isOwner || isAdmin(currentUserId))
                .moderator(moderator)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private CommentResponse.CommentUserInfo toUserInfo(User user) {
        if (user == null) return null;
        return CommentResponse.CommentUserInfo.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /**
     * Publish event like/dislike cho module thông báo.
     *
     * <p>Chạy trong transaction gốc: mọi lazy field (tên actor, target info)
     * được resolve TRƯỚC khi publish — listener chạy AFTER_COMMIT không
     * truy cập entity được nữa.</p>
     */
    private void publishReactionEvent(Long recipientUserId,
                                      User actor,
                                      boolean like,
                                      CommentReactionEvent.ReactionScope scope,
                                      Long commentId,
                                      Long replyId,
                                      CommentTargetType targetType,
                                      Long targetId) {
        // Tự tác động vào nội dung của chính mình → không thông báo.
        if (recipientUserId == null || recipientUserId.equals(actor.getId())) {
            return;
        }
        eventPublisher.publishEvent(CommentReactionEvent.builder()
                .recipientUserId(recipientUserId)
                .actorUserId(actor.getId())
                .actorName(actor.getFullName())
                .like(like)
                .scope(scope)
                .commentId(commentId)
                .replyId(replyId)
                .targetType(targetType)
                .targetId(targetId)
                .build());
    }

    /** Admin xoá nội dung của người khác → thông báo cho người viết (listener AFTER_COMMIT). */
    private void publishModeratedDeleted(User author, User moderator, CommentModeratedEvent.Scope scope,
                                         String content, Comment root) {
        eventPublisher.publishEvent(CommentModeratedEvent.builder()
                .recipientUserId(ownerId(author))
                .moderatorUserId(moderator.getId())
                .action(CommentModeratedEvent.Action.DELETED)
                .scope(scope)
                .contentPreview(content)
                .targetType(root.getTargetType())
                .targetId(root.getTargetId())
                .build());
    }

    /** Comment chưa xoá (kể cả đang ẩn) — dùng cho thao tác xoá. */
    private Comment getActiveComment(Long commentId) {
        return commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
    }

    /** Comment đang hiển thị và target của nó còn xem được — dùng cho đọc, trả lời, sửa, reaction. */
    private Comment getVisibleComment(Long commentId) {
        Comment comment = commentRepository.findVisibleById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        ensureTargetExists(comment.getTargetType(), comment.getTargetId());
        return comment;
    }

    private CommentReply getVisibleReply(Long replyId) {
        CommentReply reply = commentReplyRepository.findVisibleById(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_REPLY_NOT_FOUND));
        getVisibleComment(reply.getComment().getId()); // comment gốc phải còn và target còn xem được
        return reply;
    }

    /**
     * Đọc lại trạng thái reaction từ bảng like/dislike (source of truth) sau khi toggle —
     * các UPDATE tăng/giảm chạy thẳng trên DB nên số trên entity đã load không còn đúng.
     */
    private CommentReactionResponse buildCommentReaction(Long commentId, boolean liked, boolean disliked) {
        return CommentReactionResponse.builder()
                .liked(liked)
                .disliked(disliked)
                .likeCount((int) commentLikeRepository.countByIdCommentId(commentId))
                .dislikeCount((int) commentDislikeRepository.countByIdCommentId(commentId))
                .build();
    }

    private CommentReactionResponse buildReplyReaction(Long replyId, boolean liked, boolean disliked) {
        return CommentReactionResponse.builder()
                .liked(liked)
                .disliked(disliked)
                .likeCount((int) commentReplyLikeRepository.countByIdReplyId(replyId))
                .dislikeCount((int) commentReplyDislikeRepository.countByIdReplyId(replyId))
                .build();
    }

    private void ensureOwnerOrAdmin(User owner, Long currentUserId, ErrorCode forbiddenCode) {
        if (!isOwner(owner, currentUserId) && !currentUserService.isAdmin()) {
            throw new AppException(forbiddenCode);
        }
    }

    private static boolean isOwner(User owner, Long currentUserId) {
        return currentUserId != null && owner != null && currentUserId.equals(owner.getId());
    }

    private boolean isAdmin(Long currentUserId) {
        return currentUserId != null && currentUserService.isAdmin();
    }

    /**
     * Người xem có quyền kiểm duyệt bình luận của target này không — cùng điều kiện với
     * {@code CommentModerationService.ensureCanModerate}: admin, hoặc giáo viên tạo khóa học
     * chứa bài học. Người kiểm duyệt tự xử lý vi phạm nên FE không hiện nút báo cáo cho họ.
     */
    private boolean isModerator(CommentTargetType targetType, Long targetId, Long currentUserId) {
        if (currentUserId == null) return false;
        if (currentUserService.isAdmin()) return true;
        if (targetType != CommentTargetType.LESSON) return false;
        return lessonRepository.findByIdAndDeletedFalse(targetId)
                .map(Lesson::getCourse)
                .map(Course::getCreatedBy)
                .map(owner -> currentUserId.equals(owner.getId()))
                .orElse(false);
    }

    private static Long ownerId(User user) {
        return user != null ? user.getId() : null;
    }

    /**
     * Target phải còn tồn tại và người xem được phép thấy nó. Bài học: chưa xoá, khóa chưa xoá,
     * và khóa đã publish — khóa nháp chỉ admin, chủ khóa và người đã đăng ký mới thấy bình luận.
     */
    private void ensureTargetExists(CommentTargetType type, Long targetId) {
        if (targetId == null || targetId <= 0) {
            throw new AppException(ErrorCode.COMMENT_INVALID_TARGET);
        }
        boolean exists = switch (type) {
            case LESSON -> lessonRepository.findByIdAndDeletedFalse(targetId)
                    .map(lesson -> isLessonCourseVisible(lesson.getCourse()))
                    .orElse(false);
            case VOCABULARY -> vocabularyRepository.findByIdAndDeletedFalse(targetId).isPresent();
            case KANJI -> writingCharacterRepository.findByIdAndDeletedFalse(targetId).isPresent();
            case GRAMMAR -> grammarPointRepository.findByIdAndDeletedFalse(targetId).isPresent();
        };
        if (!exists) {
            throw new AppException(ErrorCode.COMMENT_INVALID_TARGET);
        }
    }

    private boolean isLessonCourseVisible(Course course) {
        if (course.isDeleted()) {
            return false;
        }
        return Boolean.TRUE.equals(course.getPublished())
                || courseAccessGuard.canViewUnpublished(course, currentUserIdOrNull());
    }

    private Long currentUserIdOrNull() {
        try {
            return currentUserService.getCurrentUserId();
        } catch (AppException ex) {
            return null;
        }
    }

    /** Trang bắt đầu từ 1; size kẹp trong [1, 100]. */
    private static Pageable toPageable(int page, int size) {
        return PageRequest.of(Math.max(page, 1) - 1, Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
    }

    private static Set<Long> toSet(Collection<Long> ids) {
        return new HashSet<>(ids);
    }
}
