package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.response.curriculum.CourseTeacherResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.CourseTeacher;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.CourseTeacherRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.shared.constants.PredefinedRole;
import com.jp.elearningjp.shared.enums.UserStatus;
import com.jp.elearningjp.shared.event.curriculum.CourseTeacherAssignmentChangedEvent;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Phân công giáo viên cho khóa học: ADMIN giao / gỡ, giáo viên được giao xem danh sách đồng nghiệp.
 * Mọi giáo viên được giao có quyền ngang nhau trên nội dung khóa (xem {@link CourseAccessGuard}).
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseTeacherService {

    CourseRepository courseRepository;
    CourseTeacherRepository courseTeacherRepository;
    UserRepository userRepository;
    CurrentUserService currentUserService;
    CourseAccessGuard courseAccessGuard;
    ApplicationEventPublisher eventPublisher;

    /** Danh sách giáo viên được giao: ADMIN hoặc giáo viên được giao của khóa đó. */
    @Transactional(readOnly = true)
    public List<CourseTeacherResponse> getTeachers(Long courseId) {
        Course course = findCourse(courseId);
        courseAccessGuard.requireAssignedOrAdmin(course);
        return courseTeacherRepository.findByCourseIdWithTeacher(courseId).stream()
                .map(CourseTeacherService::toResponse)
                .toList();
    }

    /** ADMIN giao một giáo viên (role TEACHER, tài khoản đang hoạt động) cho khóa học. */
    @Transactional
    public CourseTeacherResponse assignTeacher(Long courseId, Long teacherId) {
        Course course = findCourse(courseId);
        User teacher = userRepository.findById(teacherId)
                .filter(u -> !u.isDeleted() && u.getStatus() == UserStatus.ACTIVE)
                .filter(CourseTeacherService::hasTeacherRole)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_TEACHER_INVALID));
        if (courseTeacherRepository.existsByCourseIdAndTeacherId(courseId, teacherId)) {
            throw new AppException(ErrorCode.COURSE_TEACHER_ALREADY_ASSIGNED);
        }

        Long adminId = currentUserService.getCurrentUserId();
        CourseTeacher saved = courseTeacherRepository.save(CourseTeacher.builder()
                .course(course)
                .teacher(teacher)
                .assignedBy(userRepository.getReferenceById(adminId))
                .build());
        log.info("[COURSE] Assigned teacher id={} to course id={} by admin id={}", teacherId, courseId, adminId);

        eventPublisher.publishEvent(new CourseTeacherAssignmentChangedEvent(
                this, courseId, course.getTitle(), teacherId, true, adminId));
        return toResponse(saved);
    }

    /** ADMIN gỡ giáo viên khỏi khóa học; bài học giáo viên đó đã tạo vẫn giữ nguyên. */
    @Transactional
    public void unassignTeacher(Long courseId, Long teacherId) {
        Course course = findCourse(courseId);
        CourseTeacher assignment = courseTeacherRepository.findByCourseIdAndTeacherId(courseId, teacherId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_TEACHER_NOT_ASSIGNED));

        courseTeacherRepository.delete(assignment);
        Long adminId = currentUserService.getCurrentUserId();
        log.info("[COURSE] Unassigned teacher id={} from course id={} by admin id={}", teacherId, courseId, adminId);

        eventPublisher.publishEvent(new CourseTeacherAssignmentChangedEvent(
                this, courseId, course.getTitle(), teacherId, false, adminId));
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
    }

    private static boolean hasTeacherRole(User user) {
        return user.getRoles() != null
                && user.getRoles().stream().anyMatch(r -> PredefinedRole.TEACHER_ROLE.equals(r.getName()));
    }

    private static CourseTeacherResponse toResponse(CourseTeacher ct) {
        User t = ct.getTeacher();
        return CourseTeacherResponse.builder()
                .teacherId(t.getId())
                .fullName(t.getFullName())
                .email(t.getEmail())
                .avatarUrl(t.getAvatarUrl())
                .assignedAt(ct.getCreatedAt())
                .build();
    }
}
