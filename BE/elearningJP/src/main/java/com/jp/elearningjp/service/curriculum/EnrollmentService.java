package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.shared.util.CourseAccessGuard;
import java.util.Map;
import com.jp.elearningjp.dto.response.curriculum.EnrollmentResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CourseEnrollment;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.curriculum.EnrollmentMapper;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.shared.enums.EnrollmentStatus;
import com.jp.elearningjp.shared.event.curriculum.CourseFullyCompletedEvent;
import com.jp.elearningjp.shared.event.curriculum.UserEnrolledEvent;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnrollmentService {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserLessonProgressRepository progressRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final CurrentUserService currentUserService;
    private final CourseAccessGuard courseAccessGuard;
    private final ApplicationEventPublisher eventPublisher;

    // ============================================================
    // ENROLL
    // ============================================================

    /**
     * User hiện tại enroll vào course.
     *
     * <p>Điều kiện:</p>
     * <ul>
     *   <li>Course đã publish, chưa archived</li>
     *   <li>User chưa có enrollment nào chưa xoá (ACTIVE hoặc COMPLETED)</li>
     * </ul>
     */
    @Transactional
    public EnrollmentResponse enrollInCourse(Long courseId) {
        User currentUser = currentUserService.getCurrentUser();
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getPublished() == null || !course.getPublished()) {
            // Người ngoài nhận 404 như getCourseDetail để không dò được khóa nháp;
            // chỉ admin/giáo viên được giao (vốn đã biết khóa tồn tại) mới thấy lý do thật.
            boolean canSeeDraft = currentUserService.isAdmin()
                    || courseAccessGuard.isAssignedTeacher(course, currentUser.getId());
            throw new AppException(canSeeDraft ? ErrorCode.COURSE_NOT_PUBLISHED : ErrorCode.COURSE_NOT_FOUND);
        }
        if (course.getArchivedAt() != null) {
            throw new AppException(ErrorCode.COURSE_ARCHIVED);
        }

        // Khớp index uq_course_enrollments_active: chặn mọi bản ghi chưa xoá, không riêng ACTIVE
        if (enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(currentUser.getId(), courseId)) {
            throw new AppException(ErrorCode.ALREADY_ENROLLED);
        }

        CourseEnrollment enrollment = CourseEnrollment.builder()
                .user(currentUser)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        CourseEnrollment saved;
        try {
            saved = enrollmentRepository.saveAndFlush(enrollment);
        } catch (DataIntegrityViolationException e) {
            // Bấm đăng ký 2 lần gần như cùng lúc: cả 2 đều qua bước kiểm tra, index chặn request sau.
            throw new AppException(ErrorCode.ALREADY_ENROLLED);
        }

        eventPublisher.publishEvent(new UserEnrolledEvent(this,
                currentUser.getId(), courseId, saved.getId()));

        Integer progressPercent = progressRepository
                .calculateCourseProgressPercent(currentUser.getId(), courseId);
        return enrollmentMapper.toResponse(saved, progressPercent);
    }

    // ============================================================
    // MARK AS COMPLETED (auto-trigger khi hoàn thành hết lesson)
    // ============================================================

    @Transactional
    public void markAsCompletedIfFull(Long userId, Long courseId) {
        Optional<CourseEnrollment> enrollment = enrollmentRepository
                .findByUser_IdAndCourse_IdAndStatusAndDeletedFalse(
                        userId, courseId, EnrollmentStatus.ACTIVE);

        if (enrollment.isEmpty()) {
            return; // user không enroll active → bỏ qua
        }
        if (enrollment.get().getStatus() == EnrollmentStatus.COMPLETED) {
            return; // đã complete rồi → idempotent
        }

        int progressPercent = progressRepository.calculateCourseProgressPercent(userId, courseId);
        if (progressPercent >= 100) {
            CourseEnrollment e = enrollment.get();
            e.setStatus(EnrollmentStatus.COMPLETED);
            // Có thể set completedAt (nhưng CourseEnrollment chưa có field này) → bỏ qua
            enrollmentRepository.save(e);

            eventPublisher.publishEvent(new CourseFullyCompletedEvent(this, userId, courseId, e.getId()));
            log.info("Enrollment {} marked as COMPLETED (userId={}, courseId={})", e.getId(), userId, courseId);
        }
    }

    // ============================================================
    // READ
    // ============================================================

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyEnrollments() {
        User currentUser = currentUserService.getCurrentUser();
        List<CourseEnrollment> enrollments = enrollmentRepository
                .findByUser_IdAndDeletedFalseAndCourse_DeletedFalseOrderByCreatedAtDesc(currentUser.getId());
        return toResponses(currentUser.getId(), enrollments);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyActiveEnrollments() {
        User currentUser = currentUserService.getCurrentUser();
        List<CourseEnrollment> enrollments = enrollmentRepository
                .findByUser_IdAndStatusAndDeletedFalseAndCourse_DeletedFalseOrderByCreatedAtDesc(
                        currentUser.getId(), EnrollmentStatus.ACTIVE);
        return toResponses(currentUser.getId(), enrollments);
    }

    /** Tính % tiến độ của mọi khóa bằng 1 query gộp thay vì 1 query cho mỗi enrollment. */
    private List<EnrollmentResponse> toResponses(Long userId, List<CourseEnrollment> enrollments) {
        if (enrollments.isEmpty()) {
            return List.of();
        }
        List<Long> courseIds = enrollments.stream().map(e -> e.getCourse().getId()).toList();
        Map<Long, Integer> percents = progressRepository.calculateCourseProgressPercents(userId, courseIds);
        return enrollments.stream()
                .map(e -> enrollmentMapper.toResponse(e, percents.getOrDefault(e.getCourse().getId(), 0)))
                .toList();
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(Long enrollmentId) {
        User currentUser = currentUserService.getCurrentUser();
        CourseEnrollment enrollment = enrollmentRepository.findByIdAndDeletedFalseAndCourse_DeletedFalse(enrollmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

        if (!enrollment.getUser().getId().equals(currentUser.getId())
                && !currentUserService.isAdmin()) {
            throw new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS);
        }

        Integer progressPercent = progressRepository
                .calculateCourseProgressPercent(
                        enrollment.getUser().getId(), enrollment.getCourse().getId());
        return enrollmentMapper.toResponse(enrollment, progressPercent);
    }
}