package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.CourseReviewCommentRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseReviewCommentResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CourseReviewComment;
import com.jp.elearningjp.entity.curriculum.CourseReviewRequest;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.CourseReviewCommentRepository;
import com.jp.elearningjp.repository.curriculum.CourseReviewRequestRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Nhận xét duyệt theo bài: ADMIN ghi khi lượt duyệt đang chờ (trước khi duyệt / từ chối),
 * giáo viên được giao xem ở đúng bài và đánh dấu đã sửa.
 * Nội dung là văn bản thuần (FE hiển thị dạng text, không render HTML).
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseReviewCommentService {

    CourseReviewCommentRepository commentRepository;
    CourseReviewRequestRepository reviewRequestRepository;
    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    LessonSectionRepository sectionRepository;
    UserRepository userRepository;
    CourseAccessGuard courseAccessGuard;
    CurrentUserService currentUserService;

    /** Nhận xét của 1 lượt duyệt (ADMIN hoặc giáo viên được giao). */
    @Transactional(readOnly = true)
    public List<CourseReviewCommentResponse> getByRequest(Long requestId) {
        CourseReviewRequest request = findRequest(requestId);
        courseAccessGuard.requireAssignedOrAdmin(request.getCourse());
        return toResponses(request, commentRepository.findByReviewRequestIdOrderByCreatedAtAsc(requestId));
    }

    /** Nhận xét của lượt gửi duyệt gần nhất — giáo viên sửa theo, ADMIN xem lại khi duyệt. */
    @Transactional(readOnly = true)
    public List<CourseReviewCommentResponse> getLatestForCourse(Long courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        courseAccessGuard.requireAssignedOrAdmin(course);
        return reviewRequestRepository.findFirstByCourseIdOrderByCreatedAtDescIdDesc(courseId)
                .map(r -> toResponses(r, commentRepository.findByReviewRequestIdOrderByCreatedAtAsc(r.getId())))
                .orElse(List.of());
    }

    /** ADMIN thêm nhận xét — chỉ khi lượt duyệt đang chờ. */
    @Transactional
    public CourseReviewCommentResponse add(Long requestId, CourseReviewCommentRequest body) {
        requireAdmin();
        CourseReviewRequest request = findRequest(requestId);
        requirePending(request);
        Long courseId = request.getCourse().getId();

        if (body.getSectionId() != null && body.getLessonId() == null) {
            throw new AppException(ErrorCode.COURSE_REVIEW_COMMENT_TARGET_INVALID);
        }
        if (body.getLessonId() != null) {
            Lesson lesson = lessonRepository.findById(body.getLessonId())
                    .filter(l -> Objects.equals(l.getCourse().getId(), courseId))
                    .orElseThrow(() -> new AppException(ErrorCode.COURSE_REVIEW_COMMENT_TARGET_INVALID));
            if (body.getSectionId() != null) {
                sectionRepository.findById(body.getSectionId())
                        .filter(s -> Objects.equals(s.getLesson().getId(), lesson.getId()))
                        .orElseThrow(() -> new AppException(ErrorCode.COURSE_REVIEW_COMMENT_TARGET_INVALID));
            }
        }

        Long adminId = currentUserService.getCurrentUserId();
        CourseReviewComment saved = commentRepository.save(CourseReviewComment.builder()
                .reviewRequest(request)
                .courseId(courseId)
                .lessonId(body.getLessonId())
                .sectionId(body.getSectionId())
                .content(body.getContent().trim())
                .author(userRepository.getReferenceById(adminId))
                .build());
        log.info("[COURSE_REVIEW] Admin id={} commented on request id={} (lesson={}, section={})",
                adminId, requestId, body.getLessonId(), body.getSectionId());
        return toResponses(request, List.of(saved)).get(0);
    }

    /** ADMIN xóa nhận xét của mình khi lượt duyệt còn đang chờ. */
    @Transactional
    public void delete(Long commentId) {
        requireAdmin();
        CourseReviewComment comment = findComment(commentId);
        requirePending(comment.getReviewRequest());
        Long authorId = comment.getAuthor() != null ? comment.getAuthor().getId() : null;
        if (!Objects.equals(authorId, currentUserService.getCurrentUserId())) {
            throw new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS);
        }
        commentRepository.delete(comment);
    }

    /** Giáo viên được giao đánh dấu đã sửa / chưa sửa. */
    @Transactional
    public CourseReviewCommentResponse resolve(Long commentId, boolean resolved) {
        CourseReviewComment comment = findComment(commentId);
        Course course = comment.getReviewRequest().getCourse();
        courseAccessGuard.requireAssignedTeacher(course, ErrorCode.FORBIDDEN_COURSE_ACCESS);

        comment.setResolved(resolved);
        if (resolved) {
            comment.setResolvedBy(userRepository.getReferenceById(currentUserService.getCurrentUserId()));
            comment.setResolvedAt(Instant.now());
        } else {
            comment.setResolvedBy(null);
            comment.setResolvedAt(null);
        }
        commentRepository.save(comment);
        return toResponses(comment.getReviewRequest(), List.of(comment)).get(0);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private CourseReviewRequest findRequest(Long requestId) {
        CourseReviewRequest request = reviewRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_REVIEW_REQUEST_NOT_FOUND));
        if (request.getCourse().isDeleted()) {
            throw new AppException(ErrorCode.COURSE_NOT_FOUND);
        }
        return request;
    }

    private CourseReviewComment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_REVIEW_COMMENT_NOT_FOUND));
    }

    private void requireAdmin() {
        if (!currentUserService.isAdmin()) {
            throw new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS);
        }
    }

    private static void requirePending(CourseReviewRequest request) {
        if (request.getStatus() != ReviewRequestStatus.PENDING) {
            throw new AppException(ErrorCode.COURSE_REVIEW_NOT_PENDING);
        }
    }

    private List<CourseReviewCommentResponse> toResponses(CourseReviewRequest request, List<CourseReviewComment> comments) {
        // Tên bài / mục: 2 query gộp thay vì 1 query mỗi nhận xét
        Map<Long, String> lessonTitles = lessonRepository.findAllById(comments.stream()
                        .map(CourseReviewComment::getLessonId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(Lesson::getId, Lesson::getTitle));
        Map<Long, String> sectionTitles = sectionRepository.findAllById(comments.stream()
                        .map(CourseReviewComment::getSectionId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(LessonSection::getId,
                        s -> s.getTitle() != null ? s.getTitle() : "", (a, b) -> a));
        return comments.stream()
                .map(c -> CourseReviewCommentResponse.builder()
                        .id(c.getId())
                        .reviewRequestId(request.getId())
                        .reviewStatus(request.getStatus())
                        .lessonId(c.getLessonId())
                        .lessonTitle(c.getLessonId() != null ? lessonTitles.get(c.getLessonId()) : null)
                        .sectionId(c.getSectionId())
                        .sectionTitle(c.getSectionId() != null ? sectionTitles.get(c.getSectionId()) : null)
                        .content(c.getContent())
                        .authorId(c.getAuthor() != null ? c.getAuthor().getId() : null)
                        .authorName(displayName(c.getAuthor()))
                        .resolved(c.isResolved())
                        .resolvedByName(displayName(c.getResolvedBy()))
                        .resolvedAt(c.getResolvedAt())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();
    }

    private static String displayName(User u) {
        if (u == null) {
            return null;
        }
        return u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getEmail();
    }
}
