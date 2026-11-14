package com.jp.elearningjp.service.comment;

import com.jp.elearningjp.dto.response.comment.ModerationItemResponse;
import com.jp.elearningjp.dto.response.comment.ModerationItemResponse.Kind;
import com.jp.elearningjp.entity.comment.Comment;
import com.jp.elearningjp.entity.comment.CommentReply;
import com.jp.elearningjp.entity.comment.CommentReport;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CourseTeacher;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.comment.CommentReplyRepository;
import com.jp.elearningjp.repository.comment.CommentReportRepository;
import com.jp.elearningjp.repository.comment.CommentRepository;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.shared.enums.CommentTargetType;
import com.jp.elearningjp.shared.enums.ContentStatus;
import com.jp.elearningjp.shared.enums.ReportReason;
import com.jp.elearningjp.shared.enums.ReportStatus;
import com.jp.elearningjp.shared.event.comment.CommentDeletedEvent;
import com.jp.elearningjp.shared.event.comment.CommentModeratedEvent;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import jakarta.persistence.criteria.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Kiểm duyệt bình luận / trả lời.
 *
 * <p>Phạm vi: ADMIN — mọi bình luận; TEACHER — chỉ bình luận trong bài học (LESSON) thuộc
 * khóa học được giao. Từ vựng / kanji / ngữ pháp là dữ liệu chung nên chỉ ADMIN.</p>
 *
 * <p>Ẩn (HIDDEN) gỡ nội dung khỏi trang công khai nhưng khôi phục được; xóa là xóa mềm như
 * khi chủ bình luận tự xóa. Ẩn/xóa đóng mọi báo cáo OPEN thành RESOLVED và gửi thông báo
 * cho người viết; "bỏ qua" đóng báo cáo thành REJECTED.</p>
 *
 * <p>{@code reply_count} của bình luận gốc chỉ đếm trả lời đang hiển thị: ẩn trả lời → trừ 1,
 * khôi phục → cộng 1, xóa nhánh → trừ số trả lời còn hiển thị trong nhánh.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentModerationService {

    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_RECENT_DETAILS = 5;
    /**
     * Tra cứu gộp (kind = null) phải đọc {@code offset + size} dòng đầu của mỗi bảng để trộn,
     * nên giới hạn độ sâu tối đa để tránh truy vấn quá lớn khi page rất lớn.
     */
    static final int MAX_BROWSE_ALL_WINDOW = 5000;

    CommentRepository commentRepository;
    CommentReplyRepository commentReplyRepository;
    CommentReportRepository commentReportRepository;
    LessonRepository lessonRepository;
    VocabularyRepository vocabularyRepository;
    WritingCharacterRepository writingCharacterRepository;
    GrammarPointRepository grammarPointRepository;
    CurrentUserService currentUserService;
    CourseAccessGuard courseAccessGuard;
    ApplicationEventPublisher eventPublisher;

    // =========================================================
    // READ
    // =========================================================

    /**
     * Hàng chờ: nội dung bị báo cáo (theo trạng thái báo cáo), nhiều báo cáo nhất lên đầu; lọc theo từ khóa
     * (nội dung / tên / email người viết) như tra cứu.
     */
    @Transactional(readOnly = true)
    public PageResponse<ModerationItemResponse> getReportQueue(ReportStatus status, String keyword, int page, int size) {
        ReportStatus reportStatus = status != null ? status : ReportStatus.OPEN;
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, clampSize(size));
        String pattern = keyword == null || keyword.isBlank() ? null
                : "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        Page<Object[]> groups = commentReportRepository.findReportGroups(
                reportStatus.name(), teacherScopeId(), pattern, pageable);

        List<Long> commentIds = new ArrayList<>();
        List<Long> replyIds = new ArrayList<>();
        for (Object[] row : groups.getContent()) {
            if (row[0] != null) commentIds.add(toLong(row[0]));
            else replyIds.add(toLong(row[1]));
        }
        Map<Long, Comment> comments = commentRepository.findAllById(commentIds).stream()
                .collect(Collectors.toMap(Comment::getId, Function.identity()));
        Map<Long, CommentReply> replies = commentReplyRepository.findAllById(replyIds).stream()
                .collect(Collectors.toMap(CommentReply::getId, Function.identity()));

        Map<Long, List<CommentReport>> commentReports = commentIds.isEmpty() ? Map.of()
                : commentReportRepository.findByCommentIdsAndStatus(commentIds, reportStatus).stream()
                .collect(Collectors.groupingBy(r -> r.getComment().getId()));
        Map<Long, List<CommentReport>> replyReports = replyIds.isEmpty() ? Map.of()
                : commentReportRepository.findByReplyIdsAndStatus(replyIds, reportStatus).stream()
                .collect(Collectors.groupingBy(r -> r.getReply().getId()));

        TargetLabels labels = loadTargetLabels(
                comments.values().stream().toList(),
                replies.values().stream().map(CommentReply::getComment).toList());

        List<ModerationItemResponse> items = new ArrayList<>();
        for (Object[] row : groups.getContent()) {
            long count = toLong(row[2]);
            Instant latest = toInstant(row[3]);
            ModerationItemResponse item;
            List<CommentReport> reports;
            if (row[0] != null) {
                Comment c = comments.get(toLong(row[0]));
                if (c == null) continue;
                item = toItem(c, labels);
                reports = commentReports.getOrDefault(c.getId(), List.of());
            } else {
                CommentReply r = replies.get(toLong(row[1]));
                if (r == null) continue;
                item = toItem(r, labels);
                reports = replyReports.getOrDefault(r.getId(), List.of());
            }
            item.setReportCount(count);
            item.setLatestReportAt(latest);
            applyReasons(item, reports);
            items.add(item);
        }
        return toPage(items, page, pageable.getPageSize(), groups.getTotalElements(), groups.getTotalPages());
    }

    /**
     * Tra cứu bình luận (kind = COMMENT), trả lời (kind = REPLY) hoặc cả hai (kind = null) chưa xóa,
     * lọc theo trạng thái hiển thị, loại nội dung và từ khóa (nội dung / tên / email người viết).
     */
    @Transactional(readOnly = true)
    public PageResponse<ModerationItemResponse> browse(Kind kind, ContentStatus status, CommentTargetType targetType,
                                                       String keyword, int page, int size) {
        if (kind == null) {
            return browseAll(status, targetType, keyword, page, size);
        }
        Pageable pageable = PageRequest.of(Math.max(page, 1) - 1, clampSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Long teacherId = teacherScopeId();
        List<ModerationItemResponse> items;
        Page<?> result;

        if (kind == Kind.REPLY) {
            Page<CommentReply> replies = commentReplyRepository.findAll(
                    replySpec(status, targetType, keyword, teacherId), pageable);
            TargetLabels labels = loadTargetLabels(List.of(),
                    replies.getContent().stream().map(CommentReply::getComment).toList());
            Map<Long, Long> openCounts = toCountMap(replies.isEmpty() ? List.of()
                    : commentReportRepository.countOpenByReplyIds(ids(replies.getContent(), CommentReply::getId)));
            items = replies.getContent().stream().map(r -> {
                ModerationItemResponse item = toItem(r, labels);
                item.setReportCount(openCounts.getOrDefault(r.getId(), 0L));
                return item;
            }).toList();
            result = replies;
        } else {
            Page<Comment> comments = commentRepository.findAll(
                    commentSpec(status, targetType, keyword, teacherId), pageable);
            TargetLabels labels = loadTargetLabels(comments.getContent(), List.of());
            Map<Long, Long> openCounts = toCountMap(comments.isEmpty() ? List.of()
                    : commentReportRepository.countOpenByCommentIds(ids(comments.getContent(), Comment::getId)));
            items = comments.getContent().stream().map(c -> {
                ModerationItemResponse item = toItem(c, labels);
                item.setReportCount(openCounts.getOrDefault(c.getId(), 0L));
                return item;
            }).toList();
            result = comments;
        }
        return toPage(items, page, pageable.getPageSize(), result.getTotalElements(), result.getTotalPages());
    }

    /**
     * Tra cứu gộp bình luận gốc + trả lời, phân trang chính xác theo createdAt giảm dần.
     *
     * <p>Hai loại nằm ở 2 bảng với Specification riêng, nên không phân trang trực tiếp được.
     * Cách làm: lấy {@code offset + size} dòng đầu của mỗi bảng (cùng thứ tự), trộn 2 danh sách
     * đã sắp xếp rồi cắt đúng đoạn {@code [offset, offset + size)}. Bộ lọc giữ nguyên như khi
     * tra cứu từng loại. Độ sâu bị giới hạn bởi {@link #MAX_BROWSE_ALL_WINDOW}.</p>
     */
    private PageResponse<ModerationItemResponse> browseAll(ContentStatus status, CommentTargetType targetType,
                                                           String keyword, int page, int size) {
        int pageSize = clampSize(size);
        long offset = (long) (Math.max(page, 1) - 1) * pageSize;
        Long teacherId = teacherScopeId();

        // Cần offset + pageSize dòng đầu mỗi bảng; quá giới hạn thì chỉ lấy để đếm tổng
        boolean tooDeep = offset + pageSize > MAX_BROWSE_ALL_WINDOW;
        int window = tooDeep ? 1 : (int) (offset + pageSize);
        Pageable top = PageRequest.of(0, window,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Page<Comment> comments = commentRepository.findAll(
                commentSpec(status, targetType, keyword, teacherId), top);
        Page<CommentReply> replies = commentReplyRepository.findAll(
                replySpec(status, targetType, keyword, teacherId), top);

        long total = comments.getTotalElements() + replies.getTotalElements();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);
        if (tooDeep) {
            log.warn("Moderation browseAll vượt giới hạn độ sâu: page={}, size={}", page, pageSize);
            return toPage(List.of(), page, pageSize, total, totalPages);
        }

        // Trộn 2 danh sách đã sắp xếp (createdAt giảm dần; trùng thời gian thì bình luận gốc trước, rồi id giảm dần)
        List<Object> merged = new ArrayList<>(comments.getContent().size() + replies.getContent().size());
        merged.addAll(comments.getContent());
        merged.addAll(replies.getContent());
        merged.sort(Comparator.comparing(CommentModerationService::createdAtOf, Comparator.reverseOrder())
                .thenComparing(o -> o instanceof CommentReply ? 1 : 0)
                .thenComparing(CommentModerationService::idOf, Comparator.reverseOrder()));
        List<Object> slice = merged.subList((int) Math.min(offset, merged.size()),
                (int) Math.min(offset + pageSize, merged.size()));

        List<Comment> pageComments = slice.stream()
                .filter(Comment.class::isInstance).map(Comment.class::cast).toList();
        List<CommentReply> pageReplies = slice.stream()
                .filter(CommentReply.class::isInstance).map(CommentReply.class::cast).toList();
        TargetLabels labels = loadTargetLabels(pageComments,
                pageReplies.stream().map(CommentReply::getComment).toList());
        Map<Long, Long> commentCounts = toCountMap(pageComments.isEmpty() ? List.of()
                : commentReportRepository.countOpenByCommentIds(ids(pageComments, Comment::getId)));
        Map<Long, Long> replyCounts = toCountMap(pageReplies.isEmpty() ? List.of()
                : commentReportRepository.countOpenByReplyIds(ids(pageReplies, CommentReply::getId)));

        List<ModerationItemResponse> items = slice.stream().map(o -> {
            ModerationItemResponse item;
            if (o instanceof CommentReply r) {
                item = toItem(r, labels);
                item.setReportCount(replyCounts.getOrDefault(r.getId(), 0L));
            } else {
                Comment c = (Comment) o;
                item = toItem(c, labels);
                item.setReportCount(commentCounts.getOrDefault(c.getId(), 0L));
            }
            return item;
        }).toList();
        return toPage(items, page, pageSize, total, totalPages);
    }

    private static Instant createdAtOf(Object o) {
        Instant t = o instanceof CommentReply r ? r.getCreatedAt() : ((Comment) o).getCreatedAt();
        return t != null ? t : Instant.EPOCH;
    }

    private static Long idOf(Object o) {
        return o instanceof CommentReply r ? r.getId() : ((Comment) o).getId();
    }

    // =========================================================
    // ACTIONS — BÌNH LUẬN GỐC
    // =========================================================

    @Transactional
    public void hideComment(Long commentId, String note) {
        Comment comment = findActiveComment(commentId);
        ensureCanModerate(comment);
        User moderator = currentUserService.getCurrentUser();
        if (comment.getStatus() == ContentStatus.VISIBLE) {
            comment.setStatus(ContentStatus.HIDDEN);
            commentRepository.save(comment);
            publishModerated(comment.getUser(), moderator, CommentModeratedEvent.Action.HIDDEN,
                    CommentModeratedEvent.Scope.COMMENT, comment.getContent(), note, comment);
        }
        commentReportRepository.closeOpenReportsOfComment(commentId, ReportStatus.RESOLVED.name(), moderator.getId(), Instant.now());
        log.info("[MODERATION] User {} hid comment {}", moderator.getId(), commentId);
    }

    @Transactional
    public void restoreComment(Long commentId) {
        Comment comment = findActiveComment(commentId);
        ensureCanModerate(comment);
        if (comment.getStatus() == ContentStatus.HIDDEN) {
            comment.setStatus(ContentStatus.VISIBLE);
            commentRepository.save(comment);
        }
        log.info("[MODERATION] User {} restored comment {}", currentUserService.getCurrentUserId(), commentId);
    }

    @Transactional
    public void deleteComment(Long commentId, String note) {
        Comment comment = findActiveComment(commentId);
        ensureCanModerate(comment);
        User moderator = currentUserService.getCurrentUser();

        comment.softDelete();
        commentRepository.save(comment);
        commentReportRepository.closeOpenReportsOfComment(commentId, ReportStatus.RESOLVED.name(), moderator.getId(), Instant.now());
        eventPublisher.publishEvent(CommentDeletedEvent.builder().commentId(commentId).build());
        publishModerated(comment.getUser(), moderator, CommentModeratedEvent.Action.DELETED,
                CommentModeratedEvent.Scope.COMMENT, comment.getContent(), note, comment);
        log.info("[MODERATION] User {} deleted comment {}", moderator.getId(), commentId);
    }

    @Transactional
    public void dismissCommentReports(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        ensureCanModerate(comment);
        Long moderatorId = currentUserService.getCurrentUserId();
        commentReportRepository.closeOpenReportsOfComment(commentId, ReportStatus.REJECTED.name(), moderatorId, Instant.now());
        log.info("[MODERATION] User {} dismissed reports of comment {}", moderatorId, commentId);
    }

    // =========================================================
    // ACTIONS — TRẢ LỜI
    // =========================================================

    @Transactional
    public void hideReply(Long replyId, String note) {
        CommentReply reply = findActiveReply(replyId);
        Comment root = reply.getComment();
        ensureCanModerate(root);
        User moderator = currentUserService.getCurrentUser();
        if (reply.getStatus() == ContentStatus.VISIBLE) {
            reply.setStatus(ContentStatus.HIDDEN);
            commentReplyRepository.save(reply);
            commentRepository.decrementReplyCount(root.getId());
            publishModerated(reply.getUser(), moderator, CommentModeratedEvent.Action.HIDDEN,
                    CommentModeratedEvent.Scope.REPLY, reply.getContent(), note, root);
        }
        commentReportRepository.closeOpenReportsOfReplies(List.of(replyId), ReportStatus.RESOLVED.name(), moderator.getId(), Instant.now());
        log.info("[MODERATION] User {} hid reply {}", moderator.getId(), replyId);
    }

    @Transactional
    public void restoreReply(Long replyId) {
        CommentReply reply = findActiveReply(replyId);
        Comment root = reply.getComment();
        ensureCanModerate(root);
        if (reply.getStatus() == ContentStatus.HIDDEN) {
            reply.setStatus(ContentStatus.VISIBLE);
            commentReplyRepository.save(reply);
            commentRepository.incrementReplyCount(root.getId());
        }
        log.info("[MODERATION] User {} restored reply {}", currentUserService.getCurrentUserId(), replyId);
    }

    /** Xóa trả lời cùng nhánh con (giống chủ trả lời tự xóa). */
    @Transactional
    public void deleteReply(Long replyId, String note) {
        CommentReply reply = findActiveReply(replyId);
        Comment root = reply.getComment();
        ensureCanModerate(root);
        User moderator = currentUserService.getCurrentUser();

        List<Long> subtreeIds = commentReplyRepository.findActiveSubtreeIds(replyId);
        int visible = commentReplyRepository.countVisibleActiveInSubtree(replyId);
        commentReplyRepository.softDeleteSubtree(replyId, Instant.now());
        commentRepository.decrementReplyCountBy(root.getId(), visible);
        if (!subtreeIds.isEmpty()) {
            commentReportRepository.closeOpenReportsOfReplies(subtreeIds, ReportStatus.RESOLVED.name(), moderator.getId(), Instant.now());
            eventPublisher.publishEvent(CommentDeletedEvent.builder()
                    .commentId(root.getId()).replyIds(subtreeIds).build());
        }
        publishModerated(reply.getUser(), moderator, CommentModeratedEvent.Action.DELETED,
                CommentModeratedEvent.Scope.REPLY, reply.getContent(), note, root);
        log.info("[MODERATION] User {} deleted reply {} (+{} descendant(s))",
                moderator.getId(), replyId, Math.max(0, subtreeIds.size() - 1));
    }

    @Transactional
    public void dismissReplyReports(Long replyId) {
        CommentReply reply = commentReplyRepository.findById(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_REPLY_NOT_FOUND));
        ensureCanModerate(reply.getComment());
        Long moderatorId = currentUserService.getCurrentUserId();
        commentReportRepository.closeOpenReportsOfReplies(List.of(replyId), ReportStatus.REJECTED.name(), moderatorId, Instant.now());
        log.info("[MODERATION] User {} dismissed reports of reply {}", moderatorId, replyId);
    }

    // =========================================================
    // PHÂN QUYỀN
    // =========================================================

    /** null = ADMIN (không giới hạn); khác null = id giáo viên, chỉ bình luận bài học trong khóa được giao. */
    private Long teacherScopeId() {
        return currentUserService.isAdmin() ? null : currentUserService.getCurrentUserId();
    }

    private void ensureCanModerate(Comment root) {
        if (currentUserService.isAdmin()) return;
        if (root.getTargetType() != CommentTargetType.LESSON) {
            throw new AppException(ErrorCode.COMMENT_MODERATION_FORBIDDEN);
        }
        Long me = currentUserService.getCurrentUserId();
        boolean assigned = lessonRepository.findById(root.getTargetId())
                .map(Lesson::getCourse)
                .map(course -> courseAccessGuard.isAssignedTeacher(course, me))
                .orElse(false);
        if (!assigned) {
            throw new AppException(ErrorCode.COMMENT_MODERATION_FORBIDDEN);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Comment findActiveComment(Long id) {
        return commentRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private CommentReply findActiveReply(Long id) {
        CommentReply reply = commentReplyRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_REPLY_NOT_FOUND));
        if (reply.getComment() == null || reply.getComment().isDeleted()) {
            throw new AppException(ErrorCode.COMMENT_REPLY_NOT_FOUND);
        }
        return reply;
    }

    private void publishModerated(User author, User moderator, CommentModeratedEvent.Action action,
                                  CommentModeratedEvent.Scope scope, String content, String note, Comment root) {
        eventPublisher.publishEvent(CommentModeratedEvent.builder()
                .recipientUserId(author != null ? author.getId() : null)
                .moderatorUserId(moderator.getId())
                .action(action)
                .scope(scope)
                .contentPreview(content)
                .note(note == null || note.isBlank() ? null : note.trim())
                .targetType(root.getTargetType())
                .targetId(root.getTargetId())
                .build());
    }

    private ModerationItemResponse toItem(Comment c, TargetLabels labels) {
        ModerationItemResponse item = baseItem(c.getTargetType(), c.getTargetId(), labels);
        item.setKind(Kind.COMMENT);
        item.setId(c.getId());
        item.setCommentId(c.getId());
        item.setContent(c.getContent());
        item.setStatus(statusOf(c.isDeleted(), c.getStatus()));
        item.setAuthor(toAuthor(c.getUser()));
        item.setCreatedAt(c.getCreatedAt());
        return item;
    }

    private ModerationItemResponse toItem(CommentReply r, TargetLabels labels) {
        Comment root = r.getComment();
        ModerationItemResponse item = baseItem(root.getTargetType(), root.getTargetId(), labels);
        item.setKind(Kind.REPLY);
        item.setId(r.getId());
        item.setCommentId(root.getId());
        item.setContent(r.getContent());
        // Trả lời thuộc bình luận gốc đã xóa cũng coi như đã xóa
        item.setStatus(statusOf(r.isDeleted() || root.isDeleted(), r.getStatus()));
        item.setAuthor(toAuthor(r.getUser()));
        item.setCreatedAt(r.getCreatedAt());
        return item;
    }

    private static ModerationItemResponse baseItem(CommentTargetType type, Long targetId, TargetLabels labels) {
        ModerationItemResponse item = ModerationItemResponse.builder()
                .targetType(type)
                .targetId(targetId)
                .reasons(List.of())
                .recentDetails(List.of())
                .build();
        TargetInfo info = labels.get(type, targetId);
        if (info != null) {
            item.setTargetLabel(info.label());
            item.setCourseId(info.courseId());
            item.setCourseTitle(info.courseTitle());
        }
        return item;
    }

    private static String statusOf(boolean deleted, ContentStatus status) {
        if (deleted) return ContentStatus.DELETED.name();
        return status != null ? status.name() : ContentStatus.VISIBLE.name();
    }

    private static ModerationItemResponse.Author toAuthor(User u) {
        if (u == null) return null;
        return ModerationItemResponse.Author.builder()
                .id(u.getId()).fullName(u.getFullName()).email(u.getEmail()).avatarUrl(u.getAvatarUrl())
                .build();
    }

    private static void applyReasons(ModerationItemResponse item, List<CommentReport> reports) {
        Map<ReportReason, Long> counts = new EnumMap<>(ReportReason.class);
        for (CommentReport r : reports) counts.merge(r.getReason(), 1L, Long::sum);
        item.setReasons(counts.entrySet().stream()
                .sorted(Map.Entry.<ReportReason, Long>comparingByValue().reversed())
                .map(e -> ModerationItemResponse.ReasonCount.builder().reason(e.getKey()).count(e.getValue()).build())
                .toList());
        item.setRecentDetails(reports.stream()
                .map(CommentReport::getDetail)
                .filter(d -> d != null && !d.isBlank())
                .limit(MAX_RECENT_DETAILS)
                .toList());
    }

    /** Tên hiển thị của nội dung được bình luận — mỗi loại 1 query cho cả trang. */
    private TargetLabels loadTargetLabels(List<Comment> comments, List<Comment> replyRoots) {
        Map<CommentTargetType, Set<Long>> idsByType = new EnumMap<>(CommentTargetType.class);
        for (List<Comment> list : List.of(comments, replyRoots)) {
            for (Comment c : list) {
                if (c == null) continue;
                idsByType.computeIfAbsent(c.getTargetType(), k -> new HashSet<>()).add(c.getTargetId());
            }
        }
        TargetLabels labels = new TargetLabels();
        Set<Long> lessonIds = idsByType.getOrDefault(CommentTargetType.LESSON, Set.of());
        if (!lessonIds.isEmpty()) {
            lessonRepository.findAllById(lessonIds).forEach(l -> {
                Course course = l.getCourse();
                labels.put(CommentTargetType.LESSON, l.getId(), new TargetInfo(l.getTitle(),
                        course != null ? course.getId() : null, course != null ? course.getTitle() : null));
            });
        }
        Set<Long> vocabIds = idsByType.getOrDefault(CommentTargetType.VOCABULARY, Set.of());
        if (!vocabIds.isEmpty()) {
            vocabularyRepository.findAllById(vocabIds).forEach(v ->
                    labels.put(CommentTargetType.VOCABULARY, v.getId(), new TargetInfo(v.getWord(), null, null)));
        }
        Set<Long> kanjiIds = idsByType.getOrDefault(CommentTargetType.KANJI, Set.of());
        if (!kanjiIds.isEmpty()) {
            writingCharacterRepository.findAllById(kanjiIds).forEach(k ->
                    labels.put(CommentTargetType.KANJI, k.getId(), new TargetInfo(k.getCharacter(), null, null)));
        }
        Set<Long> grammarIds = idsByType.getOrDefault(CommentTargetType.GRAMMAR, Set.of());
        if (!grammarIds.isEmpty()) {
            grammarPointRepository.findAllById(grammarIds).forEach(g ->
                    labels.put(CommentTargetType.GRAMMAR, g.getId(), new TargetInfo(g.getTitle(), null, null)));
        }
        return labels;
    }

    private static Specification<Comment> commentSpec(ContentStatus status, CommentTargetType targetType,
                                                      String keyword, Long teacherId) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (targetType != null) p.add(cb.equal(root.get("targetType"), targetType));
            addKeyword(p, cb, root.get("content"), root.join("user", JoinType.LEFT), keyword);
            if (teacherId != null) p.add(lessonOwnedBy(root.get("targetType"), root.get("targetId"), teacherId, query, cb));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static Specification<CommentReply> replySpec(ContentStatus status, CommentTargetType targetType,
                                                         String keyword, Long teacherId) {
        return (root, query, cb) -> {
            Join<CommentReply, Comment> parent = root.join("comment");
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("deleted")));
            p.add(cb.isFalse(parent.get("deleted")));
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (targetType != null) p.add(cb.equal(parent.get("targetType"), targetType));
            addKeyword(p, cb, root.get("content"), root.join("user", JoinType.LEFT), keyword);
            if (teacherId != null) p.add(lessonOwnedBy(parent.get("targetType"), parent.get("targetId"), teacherId, query, cb));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static void addKeyword(List<Predicate> p, CriteriaBuilder cb, Path<String> content,
                                   Join<?, User> user, String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        p.add(cb.or(
                cb.like(cb.lower(content), pattern),
                cb.like(cb.lower(user.get("fullName")), pattern),
                cb.like(cb.lower(user.get("email")), pattern)));
    }

    /** targetType = LESSON và bài học thuộc khóa {@code teacherId} được giao. */
    private static Predicate lessonOwnedBy(Path<CommentTargetType> targetType, Path<Long> targetId, Long teacherId,
                                           CriteriaQuery<?> query, CriteriaBuilder cb) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<Lesson> lesson = sub.from(Lesson.class);
        Subquery<Long> assigned = query.subquery(Long.class);
        Root<CourseTeacher> ct = assigned.from(CourseTeacher.class);
        assigned.select(ct.get("id")).where(
                cb.equal(ct.get("course"), lesson.get("course")),
                cb.equal(ct.get("teacher").get("id"), teacherId));
        sub.select(lesson.get("id")).where(
                cb.equal(lesson.get("id"), targetId),
                cb.exists(assigned));
        return cb.and(cb.equal(targetType, CommentTargetType.LESSON), cb.exists(sub));
    }

    private static <T> List<Long> ids(List<T> list, Function<T, Long> id) {
        return list.stream().map(id).toList();
    }

    private static Map<Long, Long> toCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) map.put(toLong(row[0]), toLong(row[1]));
        return map;
    }

    private static PageResponse<ModerationItemResponse> toPage(List<ModerationItemResponse> items, int page, int size,
                                                               long total, int totalPages) {
        return PageResponse.<ModerationItemResponse>builder()
                .items(items)
                .page(Math.max(page, 1))
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .build();
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private static long toLong(Object o) {
        return o instanceof Number n ? n.longValue() : 0L;
    }

    private static Instant toInstant(Object o) {
        if (o instanceof Instant i) return i;
        if (o instanceof java.sql.Timestamp t) return t.toInstant();
        if (o instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        return null;
    }

    private record TargetInfo(String label, Long courseId, String courseTitle) {
    }

    private static final class TargetLabels {
        private final Map<CommentTargetType, Map<Long, TargetInfo>> map = new EnumMap<>(CommentTargetType.class);

        void put(CommentTargetType type, Long id, TargetInfo info) {
            map.computeIfAbsent(type, k -> new HashMap<>()).put(id, info);
        }

        TargetInfo get(CommentTargetType type, Long id) {
            Map<Long, TargetInfo> byId = map.get(type);
            return byId == null ? null : byId.get(id);
        }
    }
}
