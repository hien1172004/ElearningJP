package com.jp.elearningjp.service.comment;

import com.jp.elearningjp.dto.request.comment.CommentCreateRequest;
import com.jp.elearningjp.dto.request.comment.CommentReplyCreateRequest;
import com.jp.elearningjp.dto.request.comment.CommentUpdateRequest;
import com.jp.elearningjp.dto.response.comment.CommentDetailResponse;
import com.jp.elearningjp.dto.response.comment.CommentListResponse;
import com.jp.elearningjp.dto.response.comment.CommentReactionResponse;
import com.jp.elearningjp.dto.response.comment.CommentReplyListResponse;
import com.jp.elearningjp.dto.response.comment.CommentReplyResponse;
import com.jp.elearningjp.dto.response.comment.CommentResponse;
import com.jp.elearningjp.entity.comment.Comment;
import com.jp.elearningjp.entity.comment.CommentDislike;
import com.jp.elearningjp.entity.comment.CommentDislikeId;
import com.jp.elearningjp.entity.comment.CommentLike;
import com.jp.elearningjp.entity.comment.CommentLikeId;
import com.jp.elearningjp.entity.comment.CommentReply;
import com.jp.elearningjp.entity.comment.CommentReplyDislike;
import com.jp.elearningjp.entity.comment.CommentReplyDislikeId;
import com.jp.elearningjp.entity.comment.CommentReplyLike;
import com.jp.elearningjp.entity.comment.CommentReplyLikeId;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.comment.CommentDislikeRepository;
import com.jp.elearningjp.repository.comment.CommentLikeRepository;
import com.jp.elearningjp.repository.comment.CommentReplyDislikeRepository;
import com.jp.elearningjp.repository.comment.CommentReplyLikeRepository;
import com.jp.elearningjp.repository.comment.CommentReplyRepository;
import com.jp.elearningjp.repository.comment.CommentRepository;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.shared.enums.CommentTargetType;
import com.jp.elearningjp.shared.event.comment.CommentReactionEvent;
import com.jp.elearningjp.shared.event.comment.CommentRepliedEvent;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service quản lý {@link Comment} + {@link CommentReply}.
 *
 * <p>Module này phục vụ bình luận cho 4 loại đối tượng (LESSON / VOCABULARY /
 * KANJI / GRAMMAR) thông qua cặp {@code (targetType, targetId)}.</p>
 *
 * <p>Các cột {@code like_count}, {@code dislike_count}, {@code reply_count}
 * được cập nhật tại application layer (gọi trong transaction).</p>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CommentService {

    CommentRepository commentRepository;
    CommentLikeRepository commentLikeRepository;
    CommentDislikeRepository commentDislikeRepository;
    CommentReplyRepository commentReplyRepository;
    CommentReplyLikeRepository commentReplyLikeRepository;
    CommentReplyDislikeRepository commentReplyDislikeRepository;

    LessonRepository lessonRepository;
    VocabularyRepository vocabularyRepository;
    WritingCharacterRepository writingCharacterRepository;
    GrammarPointRepository grammarPointRepository;

    CurrentUserService currentUserService;
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
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<Comment> result = commentRepository.findByTarget(targetType, targetId, pageable);

        Long currentUserId = currentUserIdOrNull();
        List<CommentResponse> items = result.getContent().stream()
                .map(c -> toCommentResponse(c, currentUserId))
                .toList();

        return CommentListResponse.builder()
                .items(items)
                .total(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public CommentDetailResponse getCommentDetail(Long commentId) {
        Comment comment = getActiveComment(commentId);
        Long currentUserId = currentUserIdOrNull();

        CommentResponse commentResponse = toCommentResponse(comment, currentUserId);

        // Lấy toàn bộ reply chưa xoá — không phân trang để giữ đơn giản cho giai đoạn đầu
        List<CommentReply> replies = commentReplyRepository
                .findByCommentIdOrderByCreatedAtAsc(commentId, PageRequest.of(0, 200))
                .getContent();

        List<CommentReplyResponse> replyResponses = replies.stream()
                .map(r -> toReplyResponse(r, currentUserId))
                .toList();

        return CommentDetailResponse.builder()
                .comment(commentResponse)
                .replies(replyResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public CommentReplyListResponse getRepliesByComment(Long commentId,
                                                        int page,
                                                        int size) {
        getActiveComment(commentId); // chỉ để chắc chắn comment tồn tại
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<CommentReply> result = commentReplyRepository
                .findByCommentIdOrderByCreatedAtAsc(commentId, pageable);

        Long currentUserId = currentUserIdOrNull();
        List<CommentReplyResponse> items = result.getContent().stream()
                .map(r -> toReplyResponse(r, currentUserId))
                .toList();

        return CommentReplyListResponse.builder()
                .items(items)
                .total(result.getTotalElements())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .build();
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

        return toCommentResponse(saved, user.getId());
    }

    @Transactional
    public CommentReplyResponse addReply(Long commentId, CommentReplyCreateRequest request) {
        Comment comment = getActiveComment(commentId);
        User user = currentUserService.getCurrentUser();

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new AppException(ErrorCode.COMMENT_CONTENT_EMPTY);
        }

        CommentReply parent = null;
        if (request.getParentReplyId() != null) {
            parent = commentReplyRepository.findByIdAndDeletedFalse(request.getParentReplyId())
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

        // Tăng reply_count của comment gốc
        commentRepository.incrementReplyCount(comment.getId());
        comment.setReplyCount(comment.getReplyCount() + 1);

        log.info("[COMMENT] Added reply id={} to commentId={} by userId={} parentReplyId={}",
                saved.getId(), commentId, user.getId(), parent != null ? parent.getId() : null);

        // Phát event cho module thông báo — listener chạy AFTER_COMMIT nên chỉ thông báo
        // khi reply đã thực sự được commit. Mọi field phải resolve TRƯỚC khi publish vì
        // lúc listener chạy, persistence context đã đóng (không lazy-load được nữa).
        Long recipientId = comment.getUser() != null ? comment.getUser().getId() : null;
        eventPublisher.publishEvent(CommentRepliedEvent.builder()
                .recipientUserId(recipientId)
                .replierUserId(user.getId())
                .replierName(user.getFullName())
                .commentId(comment.getId())
                .replyId(saved.getId())
                .targetType(comment.getTargetType())
                .targetId(comment.getTargetId())
                .replyContent(saved.getContent())
                .build());

        return toReplyResponse(saved, user.getId());
    }

    // ============================================================
    // UPDATE / DELETE
    // ============================================================

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request) {
        Comment comment = getActiveComment(commentId);
        User user = currentUserService.getCurrentUser();
        ensureOwner(comment.getUser(), user.getId());

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new AppException(ErrorCode.COMMENT_CONTENT_EMPTY);
        }

        comment.setContent(request.getContent().trim());
        Comment saved = commentRepository.save(comment);

        log.info("[COMMENT] Updated comment id={} by userId={}", commentId, user.getId());
        return toCommentResponse(saved, user.getId());
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = getActiveComment(commentId);
        User user = currentUserService.getCurrentUser();
        ensureOwner(comment.getUser(), user.getId());

        comment.softDelete();
        commentRepository.save(comment);

        log.info("[COMMENT] Soft-deleted comment id={} by userId={}", commentId, user.getId());
    }

    @Transactional
    public void deleteReply(Long replyId) {
        CommentReply reply = commentReplyRepository.findByIdAndDeletedFalse(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_REPLY_NOT_FOUND));
        User user = currentUserService.getCurrentUser();
        ensureOwner(reply.getUser(), user.getId(), ErrorCode.COMMENT_REPLY_FORBIDDEN);

        reply.softDelete();
        commentReplyRepository.save(reply);
        // Giảm reply_count của comment gốc
        commentRepository.decrementReplyCount(reply.getComment().getId());

        log.info("[COMMENT] Soft-deleted reply id={} by userId={}", replyId, user.getId());
    }

    // ============================================================
    // TOGGLE LIKE / DISLIKE  (cho cả comment và reply)
    // ============================================================

    @Transactional
    public CommentReactionResponse toggleLike(Long commentId) {
        Comment comment = getActiveComment(commentId);
        User user = currentUserService.getCurrentUser();
        Long userId = user.getId();

        // Nếu đã like → unlike (không phát thông báo)
        if (commentLikeRepository.existsByIdUserIdAndIdCommentId(userId, commentId)) {
            commentLikeRepository.deleteByIdUserIdAndIdCommentId(userId, commentId);
            commentRepository.decrementLikeCount(commentId);
            log.info("[COMMENT] Unliked comment id={} by userId={}", commentId, userId);
            return buildCommentReaction(comment, userId, false, false);
        }

        // Nếu đang dislike → bỏ dislike trước
        if (commentDislikeRepository.existsByIdUserIdAndIdCommentId(userId, commentId)) {
            commentDislikeRepository.deleteByIdUserIdAndIdCommentId(userId, commentId);
            commentRepository.decrementDislikeCount(commentId);
        }

        CommentLike like = CommentLike.builder()
                .id(CommentLikeId.builder().userId(userId).commentId(commentId).build())
                .user(user)
                .comment(comment)
                .build();
        commentLikeRepository.save(like);
        commentRepository.incrementLikeCount(commentId);

        log.info("[COMMENT] Liked comment id={} by userId={}", commentId, userId);

        // Phát event cho module thông báo — listener chạy AFTER_COMMIT.
        // Mọi field phải resolve TRƯỚC khi publish (không lazy-load được sau commit).
        publishReactionEvent(
                comment.getUser() != null ? comment.getUser().getId() : null,
                user,
                true,
                CommentReactionEvent.ReactionScope.COMMENT,
                comment.getId(),
                null,
                comment.getTargetType(),
                comment.getTargetId());
        return buildCommentReaction(comment, userId, true, false);
    }

    @Transactional
    public CommentReactionResponse toggleDislike(Long commentId) {
        Comment comment = getActiveComment(commentId);
        User user = currentUserService.getCurrentUser();
        Long userId = user.getId();

        // Nếu đã dislike → undislike (không phát thông báo)
        if (commentDislikeRepository.existsByIdUserIdAndIdCommentId(userId, commentId)) {
            commentDislikeRepository.deleteByIdUserIdAndIdCommentId(userId, commentId);
            commentRepository.decrementDislikeCount(commentId);
            log.info("[COMMENT] Undisliked comment id={} by userId={}", commentId, userId);
            return buildCommentReaction(comment, userId, false, false);
        }

        // Nếu đang like → bỏ like trước
        if (commentLikeRepository.existsByIdUserIdAndIdCommentId(userId, commentId)) {
            commentLikeRepository.deleteByIdUserIdAndIdCommentId(userId, commentId);
            commentRepository.decrementLikeCount(commentId);
        }

        CommentDislike dislike = CommentDislike.builder()
                .id(CommentDislikeId.builder().userId(userId).commentId(commentId).build())
                .user(user)
                .comment(comment)
                .build();
        commentDislikeRepository.save(dislike);
        commentRepository.incrementDislikeCount(commentId);

        log.info("[COMMENT] Disliked comment id={} by userId={}", commentId, userId);

        publishReactionEvent(
                comment.getUser() != null ? comment.getUser().getId() : null,
                user,
                false,
                CommentReactionEvent.ReactionScope.COMMENT,
                comment.getId(),
                null,
                comment.getTargetType(),
                comment.getTargetId());
        return buildCommentReaction(comment, userId, false, true);
    }

    @Transactional
    public CommentReactionResponse toggleReplyLike(Long replyId) {
        CommentReply reply = commentReplyRepository.findByIdAndDeletedFalse(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_REPLY_NOT_FOUND));
        User user = currentUserService.getCurrentUser();
        Long userId = user.getId();

        // Nếu đã like → unlike (không phát thông báo)
        if (commentReplyLikeRepository.existsByIdUserIdAndIdReplyId(userId, replyId)) {
            commentReplyLikeRepository.deleteByIdUserIdAndIdReplyId(userId, replyId);
            commentReplyRepository.decrementLikeCount(replyId);
            log.info("[COMMENT] Unliked reply id={} by userId={}", replyId, userId);
            return buildReplyReaction(reply, userId, false, false);
        }

        if (commentReplyDislikeRepository.existsByIdUserIdAndIdReplyId(userId, replyId)) {
            commentReplyDislikeRepository.deleteByIdUserIdAndIdReplyId(userId, replyId);
            commentReplyRepository.decrementDislikeCount(replyId);
        }

        CommentReplyLike like = CommentReplyLike.builder()
                .id(CommentReplyLikeId.builder().userId(userId).replyId(replyId).build())
                .user(user)
                .reply(reply)
                .build();
        commentReplyLikeRepository.save(like);
        commentReplyRepository.incrementLikeCount(replyId);

        log.info("[COMMENT] Liked reply id={} by userId={}", replyId, userId);

        publishReactionEvent(
                reply.getUser() != null ? reply.getUser().getId() : null,
                user,
                true,
                CommentReactionEvent.ReactionScope.REPLY,
                reply.getComment().getId(),
                reply.getId(),
                reply.getComment().getTargetType(),
                reply.getComment().getTargetId());
        return buildReplyReaction(reply, userId, true, false);
    }

    @Transactional
    public CommentReactionResponse toggleReplyDislike(Long replyId) {
        CommentReply reply = commentReplyRepository.findByIdAndDeletedFalse(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_REPLY_NOT_FOUND));
        User user = currentUserService.getCurrentUser();
        Long userId = user.getId();

        // Nếu đã dislike → undislike (không phát thông báo)
        if (commentReplyDislikeRepository.existsByIdUserIdAndIdReplyId(userId, replyId)) {
            commentReplyDislikeRepository.deleteByIdUserIdAndIdReplyId(userId, replyId);
            commentReplyRepository.decrementDislikeCount(replyId);
            log.info("[COMMENT] Undisliked reply id={} by userId={}", replyId, userId);
            return buildReplyReaction(reply, userId, false, false);
        }

        if (commentReplyLikeRepository.existsByIdUserIdAndIdReplyId(userId, replyId)) {
            commentReplyLikeRepository.deleteByIdUserIdAndIdReplyId(userId, replyId);
            commentReplyRepository.decrementLikeCount(replyId);
        }

        CommentReplyDislike dislike = CommentReplyDislike.builder()
                .id(CommentReplyDislikeId.builder().userId(userId).replyId(replyId).build())
                .user(user)
                .reply(reply)
                .build();
        commentReplyDislikeRepository.save(dislike);
        commentReplyRepository.incrementDislikeCount(replyId);

        log.info("[COMMENT] Disliked reply id={} by userId={}", replyId, userId);

        publishReactionEvent(
                reply.getUser() != null ? reply.getUser().getId() : null,
                user,
                false,
                CommentReactionEvent.ReactionScope.REPLY,
                reply.getComment().getId(),
                reply.getId(),
                reply.getComment().getTargetType(),
                reply.getComment().getTargetId());
        return buildReplyReaction(reply, userId, false, true);
    }

    // ============================================================
    // MAPPING
    // ============================================================

    private CommentResponse toCommentResponse(Comment c, Long currentUserId) {
        CommentResponse.CommentUserInfo userInfo = toUserInfo(c.getUser());

        boolean liked = false;
        boolean disliked = false;
        boolean isOwner = false;
        if (currentUserId != null) {
            liked = commentLikeRepository.existsByIdUserIdAndIdCommentId(currentUserId, c.getId());
            disliked = commentDislikeRepository.existsByIdUserIdAndIdCommentId(currentUserId, c.getId());
            isOwner = c.getUser() != null && currentUserId.equals(c.getUser().getId());
        }

        return CommentResponse.builder()
                .id(c.getId())
                .targetType(c.getTargetType())
                .targetId(c.getTargetId())
                .user(userInfo)
                .content(c.getContent())
                .status(c.getStatus())
                .likeCount(c.getLikeCount())
                .dislikeCount(c.getDislikeCount())
                .replyCount(c.getReplyCount())
                .liked(liked)
                .disliked(disliked)
                .owner(isOwner)
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private CommentReplyResponse toReplyResponse(CommentReply r, Long currentUserId) {
        CommentResponse.CommentUserInfo userInfo = toUserInfo(r.getUser());

        boolean liked = false;
        boolean disliked = false;
        boolean isOwner = false;
        if (currentUserId != null) {
            liked = commentReplyLikeRepository.existsByIdUserIdAndIdReplyId(currentUserId, r.getId());
            disliked = commentReplyDislikeRepository.existsByIdUserIdAndIdReplyId(currentUserId, r.getId());
            isOwner = r.getUser() != null && currentUserId.equals(r.getUser().getId());
        }

        return CommentReplyResponse.builder()
                .id(r.getId())
                .commentId(r.getComment().getId())
                .parentReplyId(r.getParentReply() != null ? r.getParentReply().getId() : null)
                .user(userInfo)
                .content(r.getContent())
                .status(r.getStatus())
                .likeCount(r.getLikeCount())
                .dislikeCount(r.getDislikeCount())
                .liked(liked)
                .disliked(disliked)
                .owner(isOwner)
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

    private Comment getActiveComment(Long commentId) {
        return commentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
    }

    /**
     * Đọc lại trạng thái reaction của comment từ DB (sau khi toggle).
     *
     * <p>Dùng {@code countBy...} trên bảng like/dislike (source of truth) thay vì
     * {@code likeCount} trên entity trong bộ nhớ — vì các query {@code @Modifying}
     * increment/decrement chạy thẳng trên DB, không sync lại entity đã load,
     * nên {@code comment.getLikeCount()} sẽ sai lệch sau khi toggle.</p>
     */
    private CommentReactionResponse buildCommentReaction(Comment comment, Long userId,
                                                         boolean liked, boolean disliked) {
        return CommentReactionResponse.builder()
                .liked(liked)
                .disliked(disliked)
                .likeCount((int) commentLikeRepository.countByIdCommentId(comment.getId()))
                .dislikeCount((int) commentDislikeRepository.countByIdCommentId(comment.getId()))
                .build();
    }

    /**
     * Tương tự {@link #buildCommentReaction} nhưng cho reply.
     */
    private CommentReactionResponse buildReplyReaction(CommentReply reply, Long userId,
                                                       boolean liked, boolean disliked) {
        return CommentReactionResponse.builder()
                .liked(liked)
                .disliked(disliked)
                .likeCount((int) commentReplyLikeRepository.countByIdReplyId(reply.getId()))
                .dislikeCount((int) commentReplyDislikeRepository.countByIdReplyId(reply.getId()))
                .build();
    }

    private void ensureOwner(User owner, Long currentUserId) {
        ensureOwner(owner, currentUserId, ErrorCode.COMMENT_FORBIDDEN);
    }

    private void ensureOwner(User owner, Long currentUserId, ErrorCode forbiddenCode) {
        if (owner == null || !currentUserId.equals(owner.getId())) {
            throw new AppException(forbiddenCode);
        }
    }

    /**
     * Kiểm tra target tồn tại trong bảng tương ứng. Tránh để comment "treo"
     * trỏ vào 1 bản ghi đã bị xoá.
     */
    private void ensureTargetExists(CommentTargetType type, Long targetId) {
        if (targetId == null || targetId <= 0) {
            throw new AppException(ErrorCode.COMMENT_INVALID_TARGET);
        }
        boolean exists = switch (type) {
            case LESSON -> lessonRepository.existsById(targetId);
            case VOCABULARY -> vocabularyRepository.findByIdAndDeletedFalse(targetId).isPresent();
            case KANJI -> writingCharacterRepository.findByIdAndDeletedFalse(targetId).isPresent();
            case GRAMMAR -> grammarPointRepository.findByIdAndDeletedFalse(targetId).isPresent();
        };
        if (!exists) {
            throw new AppException(ErrorCode.COMMENT_INVALID_TARGET);
        }
    }

    private Long currentUserIdOrNull() {
        try {
            return currentUserService.getCurrentUserId();
        } catch (AppException ex) {
            return null;
        }
    }

    // Unused - giữ chỗ nếu sau này cần dùng tập user
    @SuppressWarnings("unused")
    private Set<Long> collectUserIds(List<? extends Object> entities) {
        return new HashSet<>();
    }
}
