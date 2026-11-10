package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.response.curriculum.CourseReviewRequestResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CourseReviewRequest;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.CourseReviewCommentRepository;
import com.jp.elearningjp.repository.curriculum.CourseReviewRequestRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.curriculum.snapshot.CourseSnapshotService;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import com.jp.elearningjp.shared.event.curriculum.CoursePublishStatusChangedEvent;
import com.jp.elearningjp.shared.event.curriculum.CourseReviewEvent;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Luồng duyệt xuất bản khóa học:
 * <pre>
 *  EDITING --(giáo viên gửi)--> PENDING_REVIEW --(ADMIN duyệt)--> EDITING + published
 *     ^                              |
 *     +---(ADMIN từ chối / rút yêu cầu)
 * </pre>
 * Trong lúc PENDING_REVIEW, mọi giáo viên bị khóa sửa nội dung ({@link CourseAccessGuard#requireContentEditable}).
 * Chỉ ADMIN quyết định nháp / xuất bản.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseReviewService {

    CourseRepository courseRepository;
    CourseReviewRequestRepository reviewRequestRepository;
    LessonRepository lessonRepository;
    UserRepository userRepository;
    CurrentUserService currentUserService;
    CourseAccessGuard courseAccessGuard;
    ApplicationEventPublisher eventPublisher;
    CourseSnapshotService courseSnapshotService;
    CourseReviewCommentRepository reviewCommentRepository;

    /** Giáo viên được giao gửi khóa lên duyệt (ADMIN chỉ duyệt, không gửi). */
    @Transactional
    public CourseReviewRequestResponse submit(Long courseId, String note) {
        Course course = findCourse(courseId);
        if (currentUserService.isAdmin()) {
            throw new AppException(ErrorCode.COURSE_REVIEW_SUBMIT_FORBIDDEN);
        }
        courseAccessGuard.requireAssignedTeacher(course, ErrorCode.COURSE_REVIEW_SUBMIT_FORBIDDEN);
        if (course.getReviewStatus() == CourseReviewStatus.PENDING_REVIEW
                || reviewRequestRepository.findByCourseIdAndStatus(courseId, ReviewRequestStatus.PENDING).isPresent()) {
            throw new AppException(ErrorCode.COURSE_REVIEW_ALREADY_PENDING);
        }
        if (!Boolean.TRUE.equals(course.getHasUnpublishedChanges())) {
            throw new AppException(ErrorCode.COURSE_REVIEW_NO_CHANGES);
        }
        if (lessonRepository.countByCourseIdAndDeletedFalse(courseId) == 0) {
            throw new AppException(ErrorCode.COURSE_REVIEW_NO_LESSONS);
        }

        Long actorId = currentUserService.getCurrentUserId();
        CourseReviewRequest request = reviewRequestRepository.save(CourseReviewRequest.builder()
                .course(course)
                .submittedBy(userRepository.getReferenceById(actorId))
                .note(trimToNull(note))
                .status(ReviewRequestStatus.PENDING)
                .build());
        course.setReviewStatus(CourseReviewStatus.PENDING_REVIEW);
        courseRepository.save(course);
        log.info("[COURSE_REVIEW] Course id={} submitted for review by user id={} (request id={})",
                courseId, actorId, request.getId());

        publish(course, CourseReviewEvent.Action.SUBMITTED, actorId, request.getNote(), false);
        return toResponse(request);
    }

    /** Người gửi rút yêu cầu đang chờ — giáo viên sửa tiếp được. ADMIN muốn trả lại thì từ chối kèm lý do. */
    @Transactional
    public CourseReviewRequestResponse withdraw(Long courseId) {
        Course course = findCourse(courseId);
        CourseReviewRequest request = reviewRequestRepository
                .findByCourseIdAndStatus(courseId, ReviewRequestStatus.PENDING)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_REVIEW_NOT_PENDING));

        Long actorId = currentUserService.getCurrentUserId();
        Long submitterId = request.getSubmittedBy() != null ? request.getSubmittedBy().getId() : null;
        if (!Objects.equals(submitterId, actorId)) {
            throw new AppException(ErrorCode.COURSE_REVIEW_WITHDRAW_FORBIDDEN);
        }

        close(request, ReviewRequestStatus.WITHDRAWN, actorId, null);
        course.setReviewStatus(CourseReviewStatus.EDITING);
        courseRepository.save(course);
        log.info("[COURSE_REVIEW] Request id={} of course id={} withdrawn by user id={}",
                request.getId(), courseId, actorId);

        publish(course, CourseReviewEvent.Action.WITHDRAWN, actorId, null, false);
        return toResponse(request);
    }

    /** ADMIN chấp nhận: khóa được xuất bản với nội dung hiện tại của bản đang soạn. */
    @Transactional
    public CourseReviewRequestResponse approve(Long requestId, String note) {
        CourseReviewRequest request = findPendingRequest(requestId);
        Course course = request.getCourse();
        Long adminId = currentUserService.getCurrentUserId();

        close(request, ReviewRequestStatus.APPROVED, adminId, trimToNull(note));
        boolean wasPublished = Boolean.TRUE.equals(course.getPublished());
        // Chụp bản đang soạn thành bản học viên đang học (kể cả đề thi gắn khóa)
        courseSnapshotService.publish(course, currentUserService.getCurrentUserOrNull());
        course.setPublished(true);
        course.setReviewStatus(CourseReviewStatus.EDITING);
        course.setHasUnpublishedChanges(false);
        courseRepository.save(course);
        log.info("[COURSE_REVIEW] Request id={} approved by admin id={} → course id={} published",
                requestId, adminId, course.getId());

        publish(course, CourseReviewEvent.Action.APPROVED, adminId, request.getReviewNote(), wasPublished);
        if (!wasPublished) {
            // Học viên đã đăng ký (nếu có) nhận "khóa đã được xuất bản" như khi ADMIN tự xuất bản
            eventPublisher.publishEvent(new CoursePublishStatusChangedEvent(
                    this, course.getId(), course.getTitle(), true, adminId));
        }
        return toResponse(request);
    }

    /** ADMIN từ chối (bắt buộc lý do): bản đang chạy giữ nguyên, giáo viên sửa tiếp. */
    @Transactional
    public CourseReviewRequestResponse reject(Long requestId, String reason) {
        CourseReviewRequest request = findPendingRequest(requestId);
        Course course = request.getCourse();
        Long adminId = currentUserService.getCurrentUserId();

        close(request, ReviewRequestStatus.REJECTED, adminId, trimToNull(reason));
        course.setReviewStatus(CourseReviewStatus.EDITING);
        courseRepository.save(course);
        log.info("[COURSE_REVIEW] Request id={} rejected by admin id={} (course id={})",
                requestId, adminId, course.getId());

        // Báo kèm số nhận xét theo bài để giáo viên biết vào từng bài xem
        long comments = reviewCommentRepository.countByReviewRequestId(requestId);
        String note = comments > 0
                ? request.getReviewNote() + " (kèm " + comments + " nhận xét theo bài)"
                : request.getReviewNote();
        publish(course, CourseReviewEvent.Action.REJECTED, adminId, note, false);
        return toResponse(request);
    }

    /**
     * ADMIN chuyển nháp / mở lại khóa (không qua yêu cầu). Không đụng tới bản đang soạn và yêu cầu
     * đang chờ: mở lại dùng đúng bản đã duyệt gần nhất. Gọi từ {@code CourseService.publishCourse}.
     */
    @Transactional
    public void onAdminPublishChanged(Course course, boolean publish) {
        publish(course, publish ? CourseReviewEvent.Action.PUBLISHED : CourseReviewEvent.Action.UNPUBLISHED,
                currentUserService.getCurrentUserId(), null, false);
    }

    /** Lịch sử gửi duyệt của 1 khóa (ADMIN hoặc giáo viên được giao). */
    @Transactional(readOnly = true)
    public List<CourseReviewRequestResponse> getHistory(Long courseId) {
        Course course = findCourse(courseId);
        courseAccessGuard.requireAssignedOrAdmin(course);
        return reviewRequestRepository.findByCourseIdOrderByCreatedAtDesc(courseId).stream()
                .map(CourseReviewService::toResponse)
                .toList();
    }

    /** Hàng chờ duyệt của ADMIN (cũ nhất trước). {@code page} bắt đầu từ 1. */
    @Transactional(readOnly = true)
    public PageResponse<CourseReviewRequestResponse> getQueue(ReviewRequestStatus status, int page, int size) {
        return getQueue(status, null, page, size);
    }

    /** @param keyword tìm theo tên khóa học hoặc tên người gửi (null/rỗng = không lọc) */
    @Transactional(readOnly = true)
    public PageResponse<CourseReviewRequestResponse> getQueue(ReviewRequestStatus status, String keyword, int page, int size) {
        ReviewRequestStatus effective = status != null ? status : ReviewRequestStatus.PENDING;
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), size);
        Page<CourseReviewRequest> result = keyword == null || keyword.isBlank()
                ? reviewRequestRepository.findByStatusAndCourseDeletedFalseOrderByCreatedAtAsc(effective, pageable)
                : reviewRequestRepository.searchQueue(
                        effective, "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%", pageable);
        return PageResponse.<CourseReviewRequestResponse>builder()
                .items(result.getContent().stream().map(CourseReviewService::toResponse).toList())
                .page(page)
                .size(size)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private Course findCourse(Long courseId) {
        return courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
    }

    private CourseReviewRequest findPendingRequest(Long requestId) {
        CourseReviewRequest request = reviewRequestRepository.findById(requestId)
                .filter(r -> !r.getCourse().isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_REVIEW_REQUEST_NOT_FOUND));
        if (request.getStatus() != ReviewRequestStatus.PENDING) {
            throw new AppException(ErrorCode.COURSE_REVIEW_NOT_PENDING);
        }
        return request;
    }

    private void close(CourseReviewRequest request, ReviewRequestStatus status, Long actorId, String reviewNote) {
        request.setStatus(status);
        request.setReviewedBy(userRepository.getReferenceById(actorId));
        request.setReviewNote(reviewNote);
        request.setReviewedAt(Instant.now());
        reviewRequestRepository.save(request);
    }

    private void publish(Course course, CourseReviewEvent.Action action, Long actorId, String note,
                         boolean contentUpdatedWhilePublished) {
        eventPublisher.publishEvent(new CourseReviewEvent(
                this, course.getId(), course.getTitle(), action, actorId, note, contentUpdatedWhilePublished));
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static CourseReviewRequestResponse toResponse(CourseReviewRequest r) {
        User submitter = r.getSubmittedBy();
        User reviewer = r.getReviewedBy();
        return CourseReviewRequestResponse.builder()
                .id(r.getId())
                .courseId(r.getCourse().getId())
                .courseTitle(r.getCourse().getTitle())
                .status(r.getStatus())
                .note(r.getNote())
                .submittedById(submitter != null ? submitter.getId() : null)
                .submittedByName(submitter != null ? displayName(submitter) : null)
                .createdAt(r.getCreatedAt())
                .reviewedById(reviewer != null ? reviewer.getId() : null)
                .reviewedByName(reviewer != null ? displayName(reviewer) : null)
                .reviewNote(r.getReviewNote())
                .reviewedAt(r.getReviewedAt())
                .build();
    }

    private static String displayName(User u) {
        return u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getEmail();
    }
}
