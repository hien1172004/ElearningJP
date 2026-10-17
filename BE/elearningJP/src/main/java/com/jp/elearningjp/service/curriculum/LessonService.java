package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonReorderRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseProgressResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonOutlineResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonMapper;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.curriculum.CourseEnrollmentRepository;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.repository.curriculum.QuizRepository;
import com.jp.elearningjp.repository.curriculum.UserLessonProgressRepository;
import com.jp.elearningjp.shared.enums.ProgressStatus;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import com.jp.elearningjp.shared.util.OrderIndexResolver;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service quản lý {@link Lesson} — bài học trong một khóa học.
 * <p>Logic CRUD cho {@code LessonSection} đã được tách ra {@link SectionService}.</p>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonService {

    LessonRepository lessonRepository;
    CourseRepository courseRepository;
    CourseEnrollmentRepository enrollmentRepository;
    UserLessonProgressRepository progressRepository;
    LessonSectionRepository lessonSectionRepository;
    QuizRepository quizRepository;
    SectionService sectionService;
    LessonMapper lessonMapper;
    LessonSectionMapper lessonSectionMapper;
    CurrentUserService currentUserService;
    CourseAccessGuard courseAccessGuard;

    /**
     * Xem nội dung chi tiết bài học (kèm sections + items).
     * BẢO VỆ BẢN QUYỀN: Bắt buộc người dùng phải là ADMIN hoặc ĐÃ ĐĂNG KÝ KHÓA HỌC.
     */
    @Transactional
    public LessonDetailResponse getLessonDetail(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Long courseId = lesson.getCourse().getId();

        // 1. Kiểm tra quyền truy cập (admin pass; user khác phải enrolled)
        courseAccessGuard.requireEnrolled(courseId);
        User currentUser = currentUserService.getCurrentUser();

        // 2. Ghi nhận / cập nhật tiến độ học tập của người dùng (IN_PROGRESS)
        UserLessonProgress progress = progressRepository
                .findByUserIdAndLessonIdAndDeletedFalse(currentUser.getId(), lessonId)
                .orElseGet(() -> {
                    UserLessonProgress newProgress = UserLessonProgress.builder()
                            .user(currentUser)
                            .lesson(lesson)
                            .status(ProgressStatus.IN_PROGRESS)
                            .startedAt(Instant.now())
                            .lastAccessedAt(Instant.now())
                            .build();
                    return progressRepository.save(newProgress);
                });

        if (progress.getStatus() == ProgressStatus.NOT_STARTED) {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
            progress.setStartedAt(Instant.now());
        }
        progress.setLastAccessedAt(Instant.now());
        progressRepository.save(progress);

        // 3. Chuẩn bị dữ liệu phản hồi
        LessonDetailResponse response = lessonMapper.toLessonDetailResponse(lesson);
        // "Lesson có quiz" = lesson_quizzes có row chưa bị xoá (refactor V20, source of truth là bảng lesson_quizzes)
        response.setHasQuiz(quizRepository.existsByLessonIdAndDeletedFalse(lessonId));
        response.setStatus(progress.getStatus());
        response.setBestScore(progress.getBestScore());
        response.setLastAccessedAt(progress.getLastAccessedAt());
        response.setCompletedAt(progress.getCompletedAt());

        // 4. Load sections + nested collections qua SectionService
        List<LessonSection> sections = lessonSectionRepository.findByLessonIdWithAllItems(lessonId);
        sectionService.loadNestedCollections(sections);
        response.setSections(lessonSectionMapper.toSectionResponses(sections));

        return response;
    }

    /**
     * Admin hoặc Teacher (chỉ trong course do mình tạo) thêm bài học mới.
     *
     * <p>Sau V19: orderIndex được xử lý an toàn theo 2 mode:</p>
     * <ul>
     *   <li><b>Auto-assign</b> (client không gửi orderIndex hoặc gửi <= 0):
     *       gán = MAX + 1, chống race condition bằng cách check trùng sau save.</li>
     *   <li><b>Client chỉ định</b> (orderIndex > 0): dời các lesson có
     *       orderIndex >= newIndex lên +1 để chèn vào giữa, nếu newIndex
     *       vượt quá MAX+1 thì gán lại = MAX + 1.</li>
     * </ul>
     *
     * <p>Trước khi save, kiểm tra EXISTS để trả lỗi 409 LESSON_ORDER_CONFLICT
     * sớm và thân thiện với client thay vì để DB ném
     * {@code DataIntegrityViolationException}.</p>
     */
    @Transactional
    public LessonDetailResponse createLesson(Long courseId, LessonCreateRequest request) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        ensureCourseOwnerOrAdmin(course);

        // ADJACENT SWAP: tạo entity với orderIndex tạm = max+1, rồi swap chain
        // từng bước một về vị trí đích để preserve thứ tự tương đối.
        //
        // Ví dụ: items=[A(1),B(2),C(3),D(4)], client tạo E với orderIndex=2.
        //   - max=4, target=2, tempIndex=max+1=5
        //   - Step 1: Save E@5 → [A(1),B(2),C(3),D(4),E(5)]
        //   - Step 2: swap E@5 ↔ D@4 → [A(1),B(2),C(3),E(4),D(5)]
        //   - Step 3: swap E@4 ↔ C@3 → [A(1),B(2),E(3),C(4),D(5)]
        //   - Step 4: swap E@3 ↔ B@2 → [A(1),E(2),B(3),C(4),D(5)]   ← kết quả
        Integer maxOrder = lessonRepository.findMaxOrderIndexByCourseId(courseId);
        int currentMax = (maxOrder != null ? maxOrder : 0);
        Integer requestedIndex = request.getOrderIndex();
        boolean clientChoseIndex = requestedIndex != null && requestedIndex > 0
                && requestedIndex <= currentMax + 1;

        int tempIndex = currentMax + 1;
        int finalIndex = clientChoseIndex ? requestedIndex : tempIndex;

        Lesson lesson = lessonMapper.toLessonEntity(request);
        lesson.setCourse(course);
        lesson.setOrderIndex(tempIndex);

        Lesson saved = lessonRepository.save(lesson);
        lessonRepository.flush();
        log.info("[LESSON] Created lesson id={}, title='{}' at tempIndex={} in course id={}",
                saved.getId(), saved.getTitle(), tempIndex, courseId);

        if (clientChoseIndex && finalIndex != tempIndex) {
            Long newId = saved.getId();
            int step = (finalIndex < tempIndex) ? -1 : 1;
            int current = tempIndex;
            int safetyCounter = 0;
            final int MAX_SWAPS = tempIndex + 2;
            while (current != finalIndex && safetyCounter++ < MAX_SWAPS) {
                int nextIndex = current + step;
                Long neighborId = lessonRepository.findIdByCourseIdAndOrderIndex(courseId, nextIndex);
                if (neighborId == null) break;
                lessonRepository.swapOrderIndexes(newId, nextIndex, neighborId, current);
                lessonRepository.flush();
                current = nextIndex;
            }
            saved.setOrderIndex(finalIndex);
            lessonRepository.flush();
            log.info("[LESSON] Swapped new lesson id={} from tempIndex={} to orderIndex={} (courseId={})",
                    newId, tempIndex, finalIndex, courseId);
        }

        LessonDetailResponse response = lessonMapper.toLessonDetailResponse(saved);
        response.setHasQuiz(false);
        response.setStatus(ProgressStatus.NOT_STARTED);
        response.setSections(List.of()); // bài mới chưa có section
        return response;
    }

    /**
     * Admin hoặc Teacher (chỉ trong course do mình tạo) cập nhật bài học.
     *
     * <p>Sau V19: nếu client đổi orderIndex sang giá trị khác:</p>
     * <ul>
     *   <li>Đổi sang index mới <b>hợp lệ</b> trong khoảng [1, MAX+1]:
     *       dời các lesson khác để giải phóng slot (shift down/up), tránh
     *       để lại lỗ hổng số.</li>
     *   <li>Đổi sang index <b>vượt MAX+1</b>: kẹp lại về MAX+1.</li>
     *   <li>Không đổi orderIndex: giữ nguyên.</li>
     * </ul>
     */
    @Transactional
    public LessonDetailResponse updateLesson(Long lessonId, LessonUpdateRequest request) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        ensureCourseOwnerOrAdmin(lesson.getCourse());

        Integer newOrderIndex = request.getOrderIndex();
        Integer oldOrderIndex = lesson.getOrderIndex();

        if (newOrderIndex != null && newOrderIndex > 0 && !newOrderIndex.equals(oldOrderIndex)) {
            Long courseId = lesson.getCourse().getId();
            int target = OrderIndexResolver.clampToMaxPlusOne(
                    newOrderIndex,
                    () -> lessonRepository.findMaxOrderIndexByCourseId(courseId)
            );

            OrderIndexResolver.Movement movement = OrderIndexResolver.planMove(
                    oldOrderIndex, target,
                    () -> lessonRepository.findMaxOrderIndexByCourseId(courseId)
            );

            if (movement == OrderIndexResolver.Movement.MOVE_UP || movement == OrderIndexResolver.Movement.MOVE_DOWN) {
                int currentOld = lesson.getOrderIndex();
                log.info("[LESSON] Adjacent-swap: moving lesson id={} from orderIndex={} → target={} (courseId={})",
                        lessonId, currentOld, target, courseId);
                int step = (target < currentOld) ? -1 : 1;
                int current = currentOld;
                int safetyCounter = 0;
                final int MAX_SWAPS = (lessonRepository.findMaxOrderIndexByCourseId(courseId) != null
                        ? lessonRepository.findMaxOrderIndexByCourseId(courseId) : 0) + 2;
                while (current != target && safetyCounter++ < MAX_SWAPS) {
                    int nextIndex = current + step;
                    Long neighborId = lessonRepository.findIdByCourseIdAndOrderIndex(courseId, nextIndex);
                    if (neighborId == null) break;
                    lessonRepository.swapOrderIndexes(lessonId, nextIndex, neighborId, current);
                    lessonRepository.flush();
                    current = nextIndex;
                }
                lesson.setOrderIndex(target);
            }
            // Movement.NOOP → giữ nguyên index

            // Check phòng race condition
            if (lessonRepository.existsByCourseIdAndOrderIndexExcludingId(courseId, lesson.getOrderIndex(), lessonId)) {
                log.warn("[LESSON] OrderIndex conflict detected at updateLesson for lesson id={}, orderIndex={}",
                        lessonId, lesson.getOrderIndex());
                throw new AppException(ErrorCode.LESSON_ORDER_CONFLICT);
            }
        }

        lessonMapper.updateLessonFromRequest(request, lesson);
        Lesson updated = lessonRepository.save(lesson);
        log.info("[LESSON] Updated lesson id={}, orderIndex={}", updated.getId(), updated.getOrderIndex());

        LessonDetailResponse response = lessonMapper.toLessonDetailResponse(updated);
        response.setHasQuiz(quizRepository.existsByLessonIdAndDeletedFalse(lessonId));

        // Load sections sau khi update
        List<LessonSection> sections = lessonSectionRepository.findByLessonIdWithAllItems(lessonId);
        sectionService.loadNestedCollections(sections);
        response.setSections(lessonSectionMapper.toSectionResponses(sections));

        return response;
    }

    /**
     * Admin hoặc Teacher (chỉ trong course do mình tạo) xóa mềm bài học.
     */
    @Transactional
    public void deleteLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        ensureCourseOwnerOrAdmin(lesson.getCourse());

        lesson.softDelete();
        lessonRepository.save(lesson);
        log.info("[LESSON] Soft-deleted lesson id={}", lessonId);
    }

    /**
     * Sắp xếp lại thứ tự hàng loạt các lesson trong 1 course (drag-drop UI).
     *
     * <p>Use case: frontend dùng thư viện kéo-thả (react-beautiful-dnd, dnd-kit, ...) để
     * reorder nhiều lesson cùng lúc. Thay vì gọi N lần {@code updateLesson} (gây N+1
     * query + N lần shift + có thể xung đột orderIndex), endpoint này nhận list ID
     * theo thứ tự MỚI và reassign {@code orderIndex = 1..N} liên tục trong 1 transaction.</p>
     *
     * <p>Quy tắc:</p>
     * <ul>
     *   <li>Chỉ reorder các lesson CHƯA xoá mềm thuộc đúng {@code courseId}.</li>
     *   <li>List {@code lessonIds} phải chứa <b>đúng tất cả</b> lesson active của course
     *       (không thiếu, không thừa). Nếu lệch → 409 {@code LESSON_REORDER_LIST_MISMATCH}.</li>
     *   <li>Không cho reorder nếu danh sách rỗng.</li>
     *   <li>Dùng <b>marker tạm âm</b> (-index) để tránh UNIQUE conflict khi swap chỉ số.</li>
     * </ul>
     *
     * <p>Quyền: Admin hoặc Teacher (chỉ reorder lesson trong course do mình tạo).</p>
     *
     * @param courseId khóa học cần reorder
     * @param request  danh sách lesson ID theo thứ tự MỚI (index 0 = orderIndex 1)
     * @return danh sách lesson sau khi reorder (đã sắp xếp theo orderIndex ASC)
     */
    @Transactional
    public List<LessonOutlineResponse> reorderLessons(Long courseId, LessonReorderRequest request) {
        Course course = courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
        ensureCourseOwnerOrAdmin(course);

        if (request.getLessonIds() == null || request.getLessonIds().isEmpty()) {
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_EMPTY);
        }

        // 1) Load tất cả lesson active của course, sắp theo orderIndex ASC
        List<Lesson> currentLessons = lessonRepository.findByCourseIdAndDeletedFalseOrderByOrderIndexAsc(courseId);
        Set<Long> currentIds = new HashSet<>();
        for (Lesson l : currentLessons) currentIds.add(l.getId());

        // 2) Validate list client gửi phải khớp 100% với current (không thiếu, không thừa, không trùng)
        Set<Long> requestedIds = new HashSet<>(request.getLessonIds());
        if (requestedIds.size() != request.getLessonIds().size()) {
            log.warn("[LESSON] Reorder list contains duplicates for course id={}", courseId);
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_INVALID);
        }
        if (!requestedIds.equals(currentIds)) {
            log.warn("[LESSON] Reorder list mismatch for course id={}. Expected {} ids, got {} (diff: missing={}, extra={})",
                    courseId, currentIds.size(), requestedIds.size(),
                    diff(currentIds, requestedIds), diff(requestedIds, currentIds));
            throw new AppException(ErrorCode.LESSON_REORDER_LIST_MISMATCH);
        }

        // 3) Dùng helper generic để gán lại orderIndex theo list mới
        //    Helper tự set marker tạm âm + gán index mới, tránh UNIQUE conflict
        OrderIndexResolver.bulkReorder(
                currentLessons,
                request.getLessonIds(),
                Lesson::getId,
                Lesson::getOrderIndex,
                Lesson::setOrderIndex
        );

        // 4) Save tất cả cùng lúc
        List<Lesson> saved = lessonRepository.saveAll(currentLessons);
        lessonRepository.flush();

        // 5) Check phòng race condition (hiếm)
        for (Lesson l : saved) {
            if (lessonRepository.existsByCourseIdAndOrderIndexExcludingId(
                    courseId, l.getOrderIndex(), l.getId())) {
                log.warn("[LESSON] OrderIndex conflict after bulk reorder: course={}, id={}, idx={}",
                        courseId, l.getId(), l.getOrderIndex());
                throw new AppException(ErrorCode.LESSON_ORDER_CONFLICT);
            }
        }

        log.info("[LESSON] Bulk reordered {} lesson(s) in course id={}", saved.size(), courseId);
        // Trả về list theo thứ tự mới
        return saved.stream()
                .map(lessonMapper::toLessonOutlineResponse)
                .toList();
    }

    private static <T> Set<T> diff(Set<T> a, Set<T> b) {
        Set<T> result = new HashSet<>(a);
        result.removeAll(b);
        return result;
    }

    /**
     * Học viên đánh dấu hoàn thành bài học thủ công (cho các bài lý thuyết không có quiz)
     * Trả về % tiến độ cập nhật của khóa học
     */
    @Transactional
    public CourseProgressResponse completeLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Long courseId = lesson.getCourse().getId();
        courseAccessGuard.requireEnrolled(courseId);
        User currentUser = currentUserService.getCurrentUser();

        UserLessonProgress progress = progressRepository
                .findByUserIdAndLessonIdAndDeletedFalse(currentUser.getId(), lessonId)
                .orElseGet(() -> UserLessonProgress.builder()
                        .user(currentUser)
                        .lesson(lesson)
                        .startedAt(Instant.now())
                        .build());

        progress.setStatus(ProgressStatus.COMPLETED);
        progress.setCompletedAt(Instant.now());
        progress.setLastAccessedAt(Instant.now());
        progressRepository.save(progress);

        // Tính toán lại % tiến độ khóa học
        int totalLessons = (int) lessonRepository.countByCourseIdAndDeletedFalse(courseId);
        int completedCount = (int) progressRepository.countCompletedLessonsByCourse(
                currentUser.getId(), courseId, ProgressStatus.COMPLETED);

        // Bước 2: dùng query có sẵn của repository để tránh tính tay
        Integer progressPercent = progressRepository
                .calculateCourseProgressPercent(currentUser.getId(), courseId);
        BigDecimal progressPercentDecimal = progressPercent != null
                ? BigDecimal.valueOf(progressPercent).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        log.info("[PROGRESS] User id={} completed lesson id={}, course progress={} %", currentUser.getId(), lessonId, progressPercentDecimal);

        // Bước 4: dùng factory summary để gom logic build response
        return CourseProgressResponse.summary(
                courseId,
                lesson.getCourse().getTitle(),
                totalLessons,
                completedCount,
                progressPercentDecimal
        );
    }

    // =========================================================
    // HELPER METHODS
    // =========================================================

    /**
     * Kiểm tra quyền sửa/xoá lesson:
     *  - ADMIN: luôn pass
     *  - TEACHER: chỉ pass nếu lesson.course.createdBy == currentUser
     *  - Các role khác: 403
     */
    private void ensureCourseOwnerOrAdmin(Course course) {
        // Giữ ErrorCode FORBIDDEN_LESSON_ACCESS (1501) để không phá contract với FE.
        courseAccessGuard.requireOwnerOrAdmin(course, ErrorCode.FORBIDDEN_LESSON_ACCESS);
    }
}
