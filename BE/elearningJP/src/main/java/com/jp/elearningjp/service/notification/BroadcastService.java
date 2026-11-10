package com.jp.elearningjp.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jp.elearningjp.dto.request.notification.BroadcastRequest;
import com.jp.elearningjp.dto.response.notification.BroadcastResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.gamification.NotificationBroadcast;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.gamification.NotificationBroadcastRepository;
import com.jp.elearningjp.shared.constants.PredefinedRole;
import com.jp.elearningjp.shared.enums.BroadcastAudience;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Gửi thông báo hàng loạt.
 *
 * <p>Phạm vi: ADMIN — toàn bộ người dùng / theo role / học viên một khóa bất kỳ;
 * TEACHER — chỉ học viên khóa được giao. Người nhận luôn là tài khoản đang ACTIVE,
 * chưa xóa, không gồm người gửi.</p>
 *
 * <p>Thông báo cho mọi người nhận được chèn bằng 1 câu INSERT ... SELECT; realtime đẩy sau
 * khi commit (người offline vẫn thấy khi mở danh sách thông báo).</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BroadcastService {

    static final int MAX_PAGE_SIZE = 100;
    static final Set<String> ROLES = Set.of(
            PredefinedRole.USER_ROLE, PredefinedRole.TEACHER_ROLE, PredefinedRole.ADMIN_ROLE);

    NotificationBroadcastRepository broadcastRepository;
    CourseRepository courseRepository;
    NotificationService notificationService;
    CurrentUserService currentUserService;
    CourseAccessGuard courseAccessGuard;
    ObjectMapper objectMapper;

    /** Số người sẽ nhận với đối tượng đã chọn (để xác nhận trước khi gửi). */
    @Transactional(readOnly = true)
    public long countRecipients(BroadcastAudience audience, String role, Long courseId) {
        Target target = resolveTarget(audience, role, courseId);
        return broadcastRepository.countRecipients(currentUserService.getCurrentUserId(),
                target.audience().name(), target.role(), target.courseId());
    }

    @Transactional
    public BroadcastResponse send(BroadcastRequest request) {
        Target target = resolveTarget(request.getAudience(), request.getRole(), request.getCourseId());
        User sender = currentUserService.getCurrentUser();

        long recipients = broadcastRepository.countRecipients(sender.getId(),
                target.audience().name(), target.role(), target.courseId());
        if (recipients == 0) {
            throw new AppException(ErrorCode.BROADCAST_NO_RECIPIENTS);
        }

        String link = request.getLink() == null || request.getLink().isBlank() ? null : request.getLink().trim();
        NotificationBroadcast broadcast = broadcastRepository.save(NotificationBroadcast.builder()
                .sender(sender)
                .title(request.getTitle().trim())
                .body(request.getBody().trim())
                .audience(target.audience())
                .role(target.role())
                .course(target.course())
                .link(link)
                .build());

        ObjectNode data = objectMapper.createObjectNode();
        data.put("broadcastId", broadcast.getId());
        if (target.courseId() != null) data.put("courseId", target.courseId());
        if (link != null) data.put("link", link);

        int inserted = broadcastRepository.insertNotifications(sender.getId(),
                target.audience().name(), target.role(), target.courseId(),
                broadcast.getTitle(), broadcast.getBody(), data.toString(), Instant.now());
        broadcast.setRecipientCount(inserted);
        broadcastRepository.save(broadcast);

        schedulePush(broadcast.getId());
        log.info("[BROADCAST] User {} sent broadcast {} ({}) to {} recipient(s)",
                sender.getId(), broadcast.getId(), target.audience(), inserted);
        return toResponse(broadcast);
    }

    /** Lịch sử gửi: ADMIN thấy mọi lần gửi, TEACHER chỉ lần gửi của mình. */
    @Transactional(readOnly = true)
    public PageResponse<BroadcastResponse> getHistory(int page, int size) {
        return getHistory(page, size, null, null);
    }

    /**
     * Lịch sử gửi kèm bộ lọc: {@code keyword} tìm trong tiêu đề / nội dung, {@code audience} lọc theo đối tượng nhận
     * (null = không lọc).
     */
    @Transactional(readOnly = true)
    public PageResponse<BroadcastResponse> getHistory(int page, int size, String keyword, BroadcastAudience audience) {
        int safePage = Math.max(page, 1);
        Pageable pageable = PageRequest.of(safePage - 1, Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        Page<NotificationBroadcast> result;
        if (hasKeyword || audience != null) {
            Long senderId = currentUserService.isAdmin() ? null : currentUserService.getCurrentUserId();
            result = broadcastRepository.findAll(historySpec(senderId, hasKeyword ? keyword : null, audience),
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                            Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            result = currentUserService.isAdmin()
                    ? broadcastRepository.findAllByOrderByCreatedAtDesc(pageable)
                    : broadcastRepository.findBySenderIdOrderByCreatedAtDesc(currentUserService.getCurrentUserId(), pageable);
        }
        return PageResponse.<BroadcastResponse>builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(safePage)
                .size(pageable.getPageSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    static Specification<NotificationBroadcast> historySpec(Long senderId, String keyword, BroadcastAudience audience) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (senderId != null) {
                predicates.add(cb.equal(root.get("sender").get("id"), senderId));
            }
            if (keyword != null) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("body")), pattern)));
            }
            if (audience != null) {
                predicates.add(cb.equal(root.get("audience"), audience));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private record Target(BroadcastAudience audience, String role, Course course) {
        Long courseId() {
            return course != null ? course.getId() : null;
        }
    }

    /** Kiểm tra đối tượng nhận hợp lệ và người gửi có quyền gửi tới đối tượng đó. */
    private Target resolveTarget(BroadcastAudience audience, String role, Long courseId) {
        if (audience == null) {
            throw new AppException(ErrorCode.BROADCAST_INVALID_AUDIENCE);
        }
        boolean admin = currentUserService.isAdmin();
        switch (audience) {
            case ALL_USERS -> {
                if (!admin) throw new AppException(ErrorCode.BROADCAST_FORBIDDEN);
                return new Target(audience, null, null);
            }
            case ROLE -> {
                if (!admin) throw new AppException(ErrorCode.BROADCAST_FORBIDDEN);
                String roleName = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
                if (roleName.startsWith("ROLE_")) roleName = roleName.substring(5);
                if (!ROLES.contains(roleName)) throw new AppException(ErrorCode.BROADCAST_INVALID_AUDIENCE);
                return new Target(audience, roleName, null);
            }
            case COURSE_STUDENTS -> {
                if (courseId == null) throw new AppException(ErrorCode.BROADCAST_INVALID_AUDIENCE);
                Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                        .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
                // TEACHER chỉ gửi cho học viên khóa được giao
                courseAccessGuard.requireAssignedOrAdmin(course, ErrorCode.BROADCAST_FORBIDDEN);
                return new Target(audience, null, course);
            }
            default -> throw new AppException(ErrorCode.BROADCAST_INVALID_AUDIENCE);
        }
    }

    /** Đẩy realtime sau khi commit — lỗi đẩy không làm hỏng việc gửi (bản ghi đã lưu). */
    private void schedulePush(Long broadcastId) {
        Runnable push = () -> {
            try {
                notificationService.pushBroadcast(broadcastId);
            } catch (Exception ex) {
                log.warn("[BROADCAST] Realtime push failed for broadcast {}: {}", broadcastId, ex.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push.run();
                }
            });
        } else {
            push.run();
        }
    }

    private BroadcastResponse toResponse(NotificationBroadcast b) {
        User sender = b.getSender();
        Course course = b.getCourse();
        return BroadcastResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .body(b.getBody())
                .audience(b.getAudience())
                .role(b.getRole())
                .courseId(course != null ? course.getId() : null)
                .courseTitle(course != null ? course.getTitle() : null)
                .link(b.getLink())
                .recipientCount(b.getRecipientCount() == null ? 0 : b.getRecipientCount())
                .senderId(sender != null ? sender.getId() : null)
                .senderName(sender != null ? sender.getFullName() : null)
                .createdAt(b.getCreatedAt())
                .build();
    }
}
