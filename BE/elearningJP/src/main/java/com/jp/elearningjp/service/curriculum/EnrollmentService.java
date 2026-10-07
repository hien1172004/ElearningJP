package com.jp.elearningjp.service.curriculum;

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
import com.jp.elearningjp.shared.event.curriculum.UserDroppedCourseEvent;
import com.jp.elearningjp.shared.event.curriculum.UserEnrolledEvent;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnrollmentService {

    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final UserLessonProgressRepository progressRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final CurrentUserService currentUserService;
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
     *   <li>User chưa có enrollment ACTIVE</li>
     *   <li>Nếu đã từng enroll (DROPPED) → set lại status = ACTIVE</li>
     * </ul>
     */
    @Transactional
    public EnrollmentResponse enrollInCourse(Long courseId) {
        User currentUser = currentUserService.getCurrentUser();
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        if (!course.isPublished()) {
            throw new AppException(ErrorCode.COURSE_NOT_PUBLISHED);
        }
        if (course.getArchivedAt() != null) {
            throw new AppException(ErrorCode.COURSE_ARCHIVED);
        }

        // Check enrollment hiện tại
        var existing = enrollmentRepository
                .findByUser_IdAndCourse_IdAndDeletedFalse(currentUser.getId(), courseId);

        CourseEnrollment enrollment;
        if (existing.isPresent()) {
            enrollment = existing.get();
            if (enrollment.getStatus() == EnrollmentStatus.ACTIVE) {
                throw new AppException(ErrorCode.ALREADY_ENROLLED);
            }
            // Re-enroll: ACTIVE lại
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            log.info("User {} re-enrolled in course {}", currentUser.getId(), courseId);
        } else {
            enrollment = CourseEnrollment.builder()
                    .user(currentUser)
                    .course(course)
                    .status(EnrollmentStatus.ACTIVE)
                    .build();
        }

        CourseEnrollment saved = enrollmentRepository.save(enrollment);

        eventPublisher.publishEvent(new UserEnrolledEvent(this,
                currentUser.getId(), courseId, saved.getId()));

        Integer progressPercent = progressRepository
                .calculateCourseProgressPercent(currentUser.getId(), courseId);
        return enrollmentMapper.toResponse(saved, progressPercent);
    }

    // ============================================================
    // DROP
    // ============================================================

    @Transactional
    public void dropEnrollment(Long enrollmentId) {
        User currentUser = currentUserService.getCurrentUser();
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));

        // Owner hoặc admin mới được drop
        if (!enrollment.getUser().getId().equals(currentUser.getId())
                && !currentUserService.isAdmin()) {
            throw new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS);
        }

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new AppException(ErrorCode.ENROLLMENT_DROPPED);
        }
        if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            // Cho phép drop cả course đã completed (edge case) — set về DROPPED
            log.warn("User {} dropping already-completed enrollment {}", currentUser.getId(), enrollmentId);
        }

        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);

        eventPublisher.publishEvent(new UserDroppedCourseEvent(this,
                currentUser.getId(), enrollment.getCourse().getId(), enrollmentId));
        log.info("Enrollment {} dropped by userId={}", enrollmentId, currentUser.getId());
    }

    @Transactional
    public void dropEnrollmentByCourseId(Long courseId) {
        User currentUser = currentUserService.getCurrentUser();
        CourseEnrollment enrollment = enrollmentRepository
                .findByUser_IdAndCourse_IdAndStatusAndDeletedFalse(
                        currentUser.getId(), courseId, EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.ENROLLMENT_NOT_FOUND));
        dropEnrollment(enrollment.getId());
    }

    // ============================================================
    // MARK AS COMPLETED (auto-trigger khi hoàn thành hết lesson)
    // ============================================================

    @Transactional
    public void markAsCompletedIfFull(Long userId, Long courseId) {
        var enrollment = enrollmentRepository
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
                .findByUser_IdAndDeletedFalseOrderByCreatedAtDesc(currentUser.getId());
        return enrollments.stream()
                .map(e -> enrollmentMapper.toResponse(e,
                        progressRepository.calculateCourseProgressPercent(
                                currentUser.getId(), e.getCourse().getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getMyActiveEnrollments() {
        User currentUser = currentUserService.getCurrentUser();
        List<CourseEnrollment> enrollments = enrollmentRepository
                .findByUser_IdAndStatusAndDeletedFalseOrderByCreatedAtDesc(
                        currentUser.getId(), EnrollmentStatus.ACTIVE);
        return enrollments.stream()
                .map(e -> enrollmentMapper.toResponse(e,
                        progressRepository.calculateCourseProgressPercent(
                                currentUser.getId(), e.getCourse().getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EnrollmentResponse getEnrollmentById(Long enrollmentId) {
        User currentUser = currentUserService.getCurrentUser();
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
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