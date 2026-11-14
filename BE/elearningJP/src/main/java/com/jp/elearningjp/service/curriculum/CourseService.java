package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.shared.util.VersionCheck;
import java.time.Instant;
import com.jp.elearningjp.dto.request.curriculum.CourseCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.CourseUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.*;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.CourseMapper;
import com.jp.elearningjp.mapper.LessonMapper;
import com.jp.elearningjp.repository.curriculum.*;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.curriculum.snapshot.CourseSnapshot;
import com.jp.elearningjp.service.curriculum.snapshot.CourseSnapshotReader;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.util.SlugUtils;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import com.jp.elearningjp.shared.event.curriculum.CoursePublishStatusChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.jp.elearningjp.entity.curriculum.CourseTeacher;
import com.jp.elearningjp.entity.curriculum.CourseEnrollment;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CourseService {

    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    CourseEnrollmentRepository enrollmentRepository;
    UserLessonProgressRepository progressRepository;
    QuizRepository quizRepository;
    LessonSpeakingScenarioRepository lessonSpeakingRepo;
    LessonDraftService lessonDraftService;
    UserRepository userRepository;
    CourseMapper courseMapper;
    LessonMapper lessonMapper;
    CurrentUserService currentUserService;
    CourseAccessGuard courseAccessGuard;
    CourseReviewService courseReviewService;
    CourseSnapshotReader courseSnapshotReader;
    CoursePublishedVersionRepository publishedVersionRepository;
    ApplicationEventPublisher eventPublisher;
    com.jp.elearningjp.repository.curriculum.LessonReviewRequestRepository lessonReviewRequestRepository;

    private static final String DEFAULT_SLUG = "khoa-hoc";

    /**
     * Lấy danh sách khóa học công khai cho học viên (có phân trang và lọc theo cấp độ JLPT).
     * Đã đăng nhập: kèm cả khóa đã chuyển nháp mà người dùng đã đăng ký — học viên cũ vẫn học tiếp được
     * (khớp {@link #getCourseDetail}), người khác vẫn không thấy.
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getPublicCourses(JlptLevel jlptLevel, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        User currentUser = currentUserService.getCurrentUserOrNull();
        Page<Course> coursePage;
        if (currentUser != null) {
            coursePage = courseRepository.findAll(learnerListSpec(currentUser.getId(), jlptLevel), pageable);
        } else {
            coursePage = (jlptLevel != null)
                    ? courseRepository.findByJlptLevelAndPublishedTrueAndDeletedFalse(jlptLevel, pageable)
                    : courseRepository.findByPublishedTrueAndDeletedFalse(pageable);
        }

        return toCoursePageResponse(coursePage, page, size, true);
    }

    /** Danh sách cho học viên {@code userId}: khóa chưa xóa, đang xuất bản hoặc người đó đã đăng ký (chưa hủy). */
    public static Specification<Course> learnerListSpec(Long userId, JlptLevel jlptLevel) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (jlptLevel != null) {
                predicates.add(cb.equal(root.get("jlptLevel"), jlptLevel));
            }
            Subquery<Long> enrolled = query.subquery(Long.class);
            Root<CourseEnrollment> enrollment = enrolled.from(CourseEnrollment.class);
            enrolled.select(enrollment.get("id"))
                    .where(cb.equal(enrollment.get("course"), root),
                            cb.equal(enrollment.get("user").get("id"), userId),
                            cb.equal(enrollment.get("deleted"), false));
            predicates.add(cb.or(cb.equal(root.get("published"), true), cb.exists(enrolled)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Danh sách khóa học cho trang quản lý (kể cả chưa xuất bản):
     * ADMIN thấy toàn bộ; TEACHER chỉ thấy khóa học được giao cho mình.
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getAllCoursesForAdmin(int page, int size) {
        return getAllCoursesForAdmin(page, size, false);
    }

    /**
     * @param pendingChangesOnly true = chỉ khóa có thay đổi chưa duyệt (bản đang soạn khác bản học viên đang học)
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getAllCoursesForAdmin(int page, int size, boolean pendingChangesOnly) {
        return getAllCoursesForAdmin(page, size, pendingChangesOnly, null, null, null);
    }

    /**
     * Như trên, kèm bộ lọc của trang quản lý: từ khóa theo tên khóa, cấp JLPT, trạng thái xuất bản
     * (null = không lọc). Không có bộ lọc nào → đi đường truy vấn cũ.
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getAllCoursesForAdmin(int page, int size, boolean pendingChangesOnly,
                                                               String keyword, JlptLevel jlptLevel, Boolean published) {
        return getAllCoursesForAdmin(page, size, pendingChangesOnly, keyword, jlptLevel, published, null);
    }

    /**
     * Như trên, kèm lọc trạng thái duyệt: PENDING_REVIEW = đã gửi, chờ duyệt; EDITING (kèm
     * {@code pendingChangesOnly}) = đang soạn, có thay đổi nhưng chưa gửi duyệt.
     */
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> getAllCoursesForAdmin(int page, int size, boolean pendingChangesOnly,
                                                               String keyword, JlptLevel jlptLevel, Boolean published,
                                                               CourseReviewStatus reviewStatus) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        Page<Course> coursePage;
        if (hasKeyword || jlptLevel != null || published != null || reviewStatus != null) {
            Long teacherId = currentUserService.isAdmin() ? null : currentUserService.getCurrentUserId();
            coursePage = courseRepository.findAll(
                    adminListSpec(teacherId, pendingChangesOnly, hasKeyword ? keyword : null, jlptLevel, published,
                            reviewStatus),
                    pageable);
        } else if (currentUserService.isAdmin()) {
            coursePage = pendingChangesOnly
                    ? courseRepository.findByDeletedFalseAndHasUnpublishedChangesTrue(pageable)
                    : courseRepository.findByDeletedFalse(pageable);
        } else {
            Long teacherId = currentUserService.getCurrentUserId();
            coursePage = pendingChangesOnly
                    ? courseRepository.findAssignedToTeacherWithPendingChanges(teacherId, pageable)
                    : courseRepository.findAssignedToTeacher(teacherId, pageable);
        }

        return toCoursePageResponse(coursePage, page, size, false);
    }

    /** Điều kiện lọc danh sách quản lý; {@code teacherId} != null → chỉ khóa được giao cho giáo viên đó. */
    public static Specification<Course> adminListSpec(Long teacherId, boolean pendingChangesOnly,
                                               String keyword, JlptLevel jlptLevel, Boolean published) {
        return adminListSpec(teacherId, pendingChangesOnly, keyword, jlptLevel, published, null);
    }

    /** Như trên, kèm lọc {@code reviewStatus} (null = không lọc). */
    public static Specification<Course> adminListSpec(Long teacherId, boolean pendingChangesOnly,
                                               String keyword, JlptLevel jlptLevel, Boolean published,
                                               CourseReviewStatus reviewStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), false));
            if (teacherId != null) {
                Subquery<Long> assigned = query.subquery(Long.class);
                Root<CourseTeacher> ct = assigned.from(CourseTeacher.class);
                assigned.select(ct.get("id"))
                        .where(cb.equal(ct.get("course"), root), cb.equal(ct.get("teacher").get("id"), teacherId));
                predicates.add(cb.exists(assigned));
            }
            if (pendingChangesOnly) {
                predicates.add(cb.equal(root.get("hasUnpublishedChanges"), true));
            }
            if (keyword != null) {
                predicates.add(cb.like(cb.lower(root.get("title")),
                        "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (jlptLevel != null) {
                predicates.add(cb.equal(root.get("jlptLevel"), jlptLevel));
            }
            if (published != null) {
                predicates.add(cb.equal(root.get("published"), published));
            }
            if (reviewStatus != null) {
                predicates.add(cb.equal(root.get("reviewStatus"), reviewStatus));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Xem chi tiết khóa học và mục lục lộ trình bài học
     * Tự động nhận diện tài khoản: nếu đã đăng ký thì trả về isEnrolled = true và % tiến độ.
     * Khóa chưa xuất bản chỉ admin, giáo viên được giao và học viên đã đăng ký thấy được;
     * người khác nhận 404 để không lộ sự tồn tại của khóa.
     */
    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        User currentUser = currentUserService.getCurrentUserOrNull();
        boolean isEnrolled = false;
        Map<Long, UserLessonProgress> progressMap = Collections.emptyMap();
        int completedCount = 0;

        if (currentUser != null) {
            isEnrolled = enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(currentUser.getId(), courseId);
            if (isEnrolled) {
                List<UserLessonProgress> progressList = progressRepository.findAllByUserIdAndCourseId(currentUser.getId(), courseId);
                progressMap = progressList.stream()
                        .collect(Collectors.toMap(p -> p.getLesson().getId(), p -> p));
                completedCount = (int) progressRepository.countCompletedLessonsByCourse(
                        currentUser.getId(), courseId, ProgressStatus.COMPLETED);
            }
        }

        boolean canManage = currentUser != null
                && (currentUserService.isAdmin() || courseAccessGuard.isAssignedTeacher(course, currentUser.getId()));
        if (!Boolean.TRUE.equals(course.getPublished())) {
            if (!isEnrolled && !canManage) {
                throw new AppException(ErrorCode.COURSE_NOT_FOUND);
            }
        }

        // Học viên của khóa đã có bản xuất bản: mục lục theo bản đã duyệt
        Optional<CourseSnapshot> snapshot = canManage
                ? Optional.empty()
                : courseSnapshotReader.findCourse(course.getPublishedVersionId());
        if (snapshot.isPresent()) {
            return buildCourseDetail(course, snapshot.get().getLessons(), progressMap, isEnrolled, false,
                    completedCount, currentUser);
        }

        // V66: bài mới phía bên kia tạo (chưa lên bản học viên) bị ẩn
        List<Lesson> lessons = lessonDraftService.visibleLessons(course);
        List<Long> allLessonIds = lessons.stream().map(Lesson::getId).toList();
        // Cờ quiz / luyện nói theo nháp mà người xem đọc (V65)
        com.jp.elearningjp.shared.enums.DraftKind draftKind = lessonDraftService.readKind(course);
        Set<Long> lessonIdsWithQuiz = lessons.isEmpty()
                ? Set.of()
                : new HashSet<>(quizRepository.findLessonIdsWithQuiz(allLessonIds, draftKind));
        Set<Long> lessonIdsWithSpeaking = lessons.isEmpty()
                ? Set.of()
                : new HashSet<>(lessonSpeakingRepo.findLessonIdsWithSpeaking(allLessonIds, draftKind));

        List<LessonOutlineResponse> outlineList = lessons.stream()
                .map(lesson -> {
                    LessonOutlineResponse outline = lessonMapper.toLessonOutlineResponse(lesson);
                    outline.setHasQuiz(lessonIdsWithQuiz.contains(lesson.getId()));
                    outline.setHasSpeaking(lessonIdsWithSpeaking.contains(lesson.getId()));
                    return outline;
                })
                .toList();

        return buildCourseDetail(course, outlineList, progressMap, isEnrolled, canManage, completedCount, currentUser);
    }

    /** Mục lục bài (bản đang soạn hoặc snapshot) + ghép tiến độ của người dùng. */
    private CourseDetailResponse buildCourseDetail(Course course, List<LessonOutlineResponse> outlineList,
                                                   Map<Long, UserLessonProgress> progressMap, boolean isEnrolled,
                                                   boolean canManage, int completedCount, User currentUser) {
        Long courseId = course.getId();
        for (LessonOutlineResponse outline : outlineList) {
            UserLessonProgress progress = progressMap.get(outline.getId());
            if (progress != null) {
                outline.setStatus(progress.getStatus());
                outline.setBestScore(progress.getBestScore());
                // Bài đã hoàn thành luôn 100% (kể cả dòng tạo trước V67 có progress_percent = 0)
                outline.setProgressPercent(progress.getStatus() == ProgressStatus.COMPLETED
                        ? 100 : progress.getProgressPercent());
            } else {
                outline.setStatus(ProgressStatus.NOT_STARTED);
                outline.setProgressPercent(0);
            }
        }

        // Bước 2: dùng query SQL tổng hợp đã có sẵn (chính xác, tránh lệch do tính tay)
        BigDecimal progressPercent = isEnrolled && currentUser != null
                ? toProgressDecimal(progressRepository.calculateCourseProgressPercent(
                        currentUser.getId(), courseId))
                : BigDecimal.ZERO;

        CourseDetailResponse response = courseMapper.toCourseDetailResponse(course);
        response.setEnrolled(isEnrolled);
        response.setCanManage(canManage);
        if (!canManage) {
            // Người sửa / thời điểm sửa bản đang soạn là thông tin nội bộ của người soạn
            response.setDraftUpdatedById(null);
            response.setDraftUpdatedByName(null);
            response.setDraftUpdatedAt(null);
        }
        if (canManage) {
            response.setPendingLessonReviews((long) lessonReviewRequestRepository
                    .findPendingLessonTitles(course.getId()).size());
        }
        if (canManage && course.getPublishedVersionId() != null) {
            publishedVersionRepository.findById(course.getPublishedVersionId()).ifPresent(v -> {
                response.setPublishedVersionNo(v.getVersionNo());
                response.setPublishedVersionAt(v.getCreatedAt());
            });
        }
        response.setTotalLessons(outlineList.size());
        // Trước đây field này không được gán → trang quản lý / chi tiết khóa học luôn hiện 0 học viên
        response.setTotalEnrollments(enrollmentRepository.countActiveByCourseId(courseId));
        response.setCompletedLessonsCount(completedCount);
        response.setProgressPercent(progressPercent);
        response.setLessons(outlineList);

        return response;
    }

    /**
     * ADMIN tạo khóa học mới (createdBy = admin). Giáo viên được giao sau qua {@code CourseTeacherService}.
     */
    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request) {
        User owner = currentUserService.getCurrentUser();

        // Tên toàn tiếng Nhật không còn ký tự Latin nào nên slug rỗng → dùng slug mặc định.
        String titleSlug = SlugUtils.toSlug(request.getTitle());
        String baseSlug = titleSlug.isEmpty() ? DEFAULT_SLUG : titleSlug;
        String slug = baseSlug;
        int counter = 1;
        // Cột slug UNIQUE trên toàn bảng, nên phải tính cả khóa đã xoá mềm.
        while (courseRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }

        Course course = courseMapper.toCourseEntity(request);
        course.setSlug(slug);
        course.setCreatedBy(owner);
        // Khóa mới luôn là nháp: chỉ lên cho học viên khi giáo viên gửi duyệt và ADMIN duyệt
        course.setPublished(false);
        course.setReviewStatus(CourseReviewStatus.EDITING);
        course.setHasUnpublishedChanges(false);

        Course saved = courseRepository.save(course);
        log.info("[COURSE] Created course id={}, title='{}' by user id={}", saved.getId(), saved.getTitle(), owner.getId());

        CourseResponse response = courseMapper.toCourseResponse(saved);
        response.setTotalLessons(0);
        return response;
    }

    /**
     * ADMIN cập nhật thông tin khóa học (tiêu đề, mô tả, ảnh bìa, cấp JLPT) — có hiệu lực ngay.
     */
    @Transactional
    public CourseResponse updateCourse(Long courseId, CourseUpdateRequest request) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        VersionCheck.requireVersion(course, request.getVersion());

        courseMapper.updateCourseFromRequest(request, course);
        // flush để response mang version mới
        Course updated = courseRepository.save(course);
        courseRepository.flush();
        log.info("[COURSE] Updated course id={}", updated.getId());
        // Thông tin khóa không qua duyệt → học viên đang mở khóa cũng cần tải lại (listener AFTER_COMMIT)
        eventPublisher.publishEvent(new com.jp.elearningjp.shared.event.curriculum.CourseContentChangedEvent(
                this, updated.getId(), com.jp.elearningjp.shared.event.curriculum.CourseContentChangedEvent.Scope.PUBLISHED,
                currentUserService.getCurrentUserId()));

        CourseResponse response = courseMapper.toCourseResponse(updated);
        long totalLessons = lessonRepository.countByCourseIdAndDeletedFalse(courseId);
        response.setTotalLessons((int) totalLessons);
        return response;
    }

    /**
     * ADMIN xóa mềm khóa học.
     */
    @Transactional
    public void deleteCourse(Long courseId) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        course.softDelete();
        courseRepository.save(course);
        // Khóa đã xoá không còn xuất hiện trong "khóa của tôi" và không còn tính tiến độ.
        Instant now = Instant.now();
        enrollmentRepository.softDeleteByCourseId(courseId, now);
        progressRepository.softDeleteByCourseId(courseId, now);
        log.info("[COURSE] Soft-deleted course id={}", courseId);
    }

    /**
     * ADMIN chuyển nháp ({@code publish=false}) hoặc mở lại khóa ({@code publish=true}).
     * Mở lại chỉ hiện lại <b>bản đã duyệt gần nhất</b> — nội dung mới chỉ tới học viên qua duyệt yêu cầu.
     */
    @Transactional
    public CourseResponse publishCourse(Long courseId, boolean publish) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        if (publish && course.getPublishedVersionId() == null) {
            throw new AppException(ErrorCode.COURSE_NOT_YET_APPROVED);
        }

        boolean changed = !Boolean.valueOf(publish).equals(course.getPublished());
        course.setPublished(publish);
        if (changed) {
            courseReviewService.onAdminPublishChanged(course, publish);
        }
        Course saved = courseRepository.save(course);
        log.info("[COURSE] Course id={} published status set to {}", courseId, publish);

        // Báo cho học viên đã đăng ký (listener chạy AFTER_COMMIT) — chỉ khi trạng thái thật sự đổi
        if (changed) {
            eventPublisher.publishEvent(new CoursePublishStatusChangedEvent(
                    this, saved.getId(), saved.getTitle(), publish, currentUserService.getCurrentUserId()));
        }

        CourseResponse response = courseMapper.toCourseResponse(saved);
        long totalLessons = lessonRepository.countByCourseIdAndDeletedFalse(courseId);
        response.setTotalLessons((int) totalLessons);
        return response;
    }

    /**
     * Lấy tiến độ học tập chi tiết của học viên trong khóa học
     */
    @Transactional(readOnly = true)
    public CourseProgressResponse getCourseProgress(Long courseId) {
        User user = currentUserService.getCurrentUser();
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));

        if (!enrollmentRepository.existsByUserIdAndCourseIdAndDeletedFalse(user.getId(), courseId)) {
            throw new AppException(ErrorCode.COURSE_NOT_ENROLLED);
        }

        int totalLessons = (int) lessonRepository.countLearnerLessons(courseId);
        int completedLessons = (int) progressRepository.countCompletedLessonsByCourse(
                user.getId(), courseId, ProgressStatus.COMPLETED);

        // Bước 2: dùng query SQL tổng hợp có sẵn
        BigDecimal progressPercent = toProgressDecimal(
                progressRepository.calculateCourseProgressPercent(user.getId(), courseId));

        // Bước 4: dùng factory summary để gom logic build response
        return CourseProgressResponse.summary(
                course.getId(),
                course.getTitle(),
                totalLessons,
                completedLessons,
                progressPercent
        );
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    /** Map trang khóa học, đếm số bài và số học viên bằng query gộp thay vì 1 query cho mỗi khóa. */
    /**
     * @param learnerCounts true = đếm số bài học viên đang học (bản xuất bản) cho trang công khai;
     *                      false = đếm bài của bản đang soạn cho trang quản lý.
     */
    private PageResponse<CourseResponse> toCoursePageResponse(Page<Course> coursePage, int page, int size,
                                                              boolean learnerCounts) {
        List<Long> courseIds = coursePage.getContent().stream().map(Course::getId).toList();
        Map<Long, Long> lessonCounts = courseIds.isEmpty()
                ? Map.of()
                : (learnerCounts
                        ? lessonRepository.countLearnerLessonsByCourseIds(courseIds)
                        : lessonRepository.countActiveLessonsByCourseIds(courseIds)).stream()
                        .collect(Collectors.toMap(
                                LessonRepository.CourseLessonCount::getCourseId,
                                LessonRepository.CourseLessonCount::getLessonCount));
        // Trang quản lý: số bài chờ duyệt theo bài của từng khóa
        Map<Long, Long> pendingReviews = learnerCounts || courseIds.isEmpty()
                ? Map.of()
                : lessonReviewRequestRepository.countPendingByCourseIds(courseIds).stream()
                        .collect(Collectors.toMap(
                                com.jp.elearningjp.repository.curriculum.LessonReviewRequestRepository.CoursePendingCount::getCourseId,
                                com.jp.elearningjp.repository.curriculum.LessonReviewRequestRepository.CoursePendingCount::getPendingCount));
        Map<Long, Long> enrollmentCounts = courseIds.isEmpty()
                ? Map.of()
                : enrollmentRepository.countActiveByCourseIds(courseIds).stream()
                        .collect(Collectors.toMap(
                                CourseEnrollmentRepository.CourseEnrollmentCount::getCourseId,
                                CourseEnrollmentRepository.CourseEnrollmentCount::getEnrollmentCount));

        List<CourseResponse> items = coursePage.getContent().stream()
                .map(course -> {
                    CourseResponse response = courseMapper.toCourseResponse(course);
                    if (learnerCounts) {
                        // Danh sách công khai: không lộ người / thời điểm sửa bản đang soạn
                        response.setDraftUpdatedById(null);
                        response.setDraftUpdatedByName(null);
                        response.setDraftUpdatedAt(null);
                    }
                    response.setTotalLessons(lessonCounts.getOrDefault(course.getId(), 0L).intValue());
                    response.setTotalEnrollments(enrollmentCounts.getOrDefault(course.getId(), 0L));
                    if (!learnerCounts) {
                        response.setPendingLessonReviews(pendingReviews.getOrDefault(course.getId(), 0L));
                    }
                    return response;
                })
                .toList();

        return PageResponse.<CourseResponse>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(coursePage.getTotalElements())
                .totalPages(coursePage.getTotalPages())
                .build();
    }

    /**
     * Chuyển Integer từ {@code calculateCourseProgressPercent} (đã làm tròn số nguyên ở SQL)
     * sang BigDecimal có 1 chữ số thập phân — đồng nhất với format cũ khi tính tay.
     */
    private static BigDecimal toProgressDecimal(Integer percent) {
        if (percent == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(percent).setScale(1, RoundingMode.HALF_UP);
    }
}
