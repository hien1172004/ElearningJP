package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.dto.request.curriculum.LessonCreateRequest;
import com.jp.elearningjp.dto.request.curriculum.LessonUpdateRequest;
import com.jp.elearningjp.dto.response.curriculum.CourseProgressResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonSectionResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.entity.curriculum.UserLessonProgress;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.LessonMapper;
import com.jp.elearningjp.mapper.LessonSectionMapper;
import com.jp.elearningjp.repository.curriculum.*;
import com.jp.elearningjp.shared.enums.LessonSectionType;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonService {

    LessonRepository lessonRepository;
    CourseRepository courseRepository;
    CourseEnrollmentRepository enrollmentRepository;
    UserLessonProgressRepository progressRepository;
    QuizRepository quizRepository;
    LessonSectionRepository lessonSectionRepository;
    LessonKanjiItemRepository kanjiItemRepository;
    LessonVocabularyItemRepository vocabularyItemRepository;
    LessonGrammarItemRepository grammarItemRepository;
    LessonKanjiRelatedVocabRepository kanjiRelatedVocabRepository;
    LessonGrammarExampleRepository grammarExampleRepository;
    LessonVocabularyExampleRepository vocabularyExampleRepository;
    QuizOptionRepository quizOptionRepository;
    LessonMapper lessonMapper;
    LessonSectionMapper lessonSectionMapper;
    CurrentUserService currentUserService;
    CourseAccessGuard courseAccessGuard;

    /**
     * Xem nội dung chi tiết bài học (kèm sections + items)
     * BẢO VỆ BẢN QUYỀN: Bắt buộc người dùng phải là ADMIN hoặc ĐÃ ĐĂNG KÝ KHÓA HỌC
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
        // "Lesson có quiz" = lesson có section TEST chưa bị xoá (sau refactor V13/V14)
        response.setHasQuiz(lessonSectionRepository
                .existsByLessonIdAndSectionTypeAndDeletedFalse(lessonId, LessonSectionType.TEST));
        response.setStatus(progress.getStatus());
        response.setBestScore(progress.getBestScore());
        response.setLastAccessedAt(progress.getLastAccessedAt());
        response.setCompletedAt(progress.getCompletedAt());

        // 4. Load sections kèm tất cả items.
        // Repository chỉ load section cơ bản; các collection (kanjiItems, vocabularyItems,
        // grammarItems, quizzes + các collection con bên trong) được load tách riêng trong
        // loadNestedCollections() để tránh MultipleBagFetchException.
        // Cùng persistence context → Hibernate tự merge collection vào entity đã load.
        List<LessonSection> sections = lessonSectionRepository.findByLessonIdWithAllItems(lessonId);
        loadNestedCollections(sections, lessonId);
        List<LessonSectionResponse> sectionResponses = lessonSectionMapper.toSectionResponses(sections);
        response.setSections(sectionResponses);

        return response;
    }

    /**
     * Load các collection (kanjiItems, vocabularyItems, grammarItems, quizzes + các
     * collection bên trong: examples, relatedVocabulary, questions, options) theo
     * từng query riêng biệt. Mỗi query chỉ fetch tối đa 1 collection List<>, nên
     * KHÔNG BAO GIỜ trigger MultipleBagFetchException.
     * <p>
     * Cơ chế: vì cùng {@code EntityManager} (persistence context), Hibernate tự động
     * resolve entity đã load trước đó (theo id) và gắn collection vào. Ta chỉ cần
     * truy cập collection trong mapper là Hibernate trả về dữ liệu đã batch.
     * </p>
     * <p>
     * Tổng cộng 8 query (1 section + 3 item kanji/vocab/grammar + 1 quiz + 3 collection con),
     * nhưng mỗi query đều có index theo FK nên rẻ. Tránh được N+1 hoàn toàn.
     * </p>
     */
    private void loadNestedCollections(List<LessonSection> sections, Long lessonId) {
        if (sections == null || sections.isEmpty()) {
            return;
        }
        List<Long> sectionIds = sections.stream().map(LessonSection::getId).toList();

        // === Level 1: 4 collection trên LessonSection — mỗi query 1 collection List<> ===
        kanjiItemRepository.findByLessonIdWithCharacter(lessonId);      // + character (many-to-1)
        vocabularyItemRepository.findByLessonIdWithVocab(lessonId);     // + vocab (many-to-1)
        grammarItemRepository.findByLessonIdWithGrammar(lessonId);      // + grammar (many-to-1)
        quizRepository.findByLessonIdWithQuestions(lessonId);           // + questions (List, 1 collection)

        // === Level 2: collection bên trong item — mỗi query 1 collection List<> ===
        kanjiRelatedVocabRepository.findBySectionIds(sectionIds);
        grammarExampleRepository.findBySectionIds(sectionIds);
        vocabularyExampleRepository.findBySectionIds(sectionIds);
        quizOptionRepository.findByLessonId(lessonId);

        // Hibernate persistence context tự merge các collection này vào entity đã load.
        // Mapper chỉ cần section.getKanjiItems(), ki.getExamples(), ... là có dữ liệu.
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

        // Bước 5: gom logic MAX/shift vào OrderIndexResolver
        Integer resolvedIndex = OrderIndexResolver.resolveForCreate(
                request.getOrderIndex(),
                () -> lessonRepository.findMaxOrderIndexByCourseId(courseId)
        );
        // Nếu client chỉ định index hợp lệ (1..max+1) → shift các record >= index lên +1
        if (request.getOrderIndex() != null && request.getOrderIndex() > 0
                && resolvedIndex == request.getOrderIndex()) {
            int shifted = OrderIndexResolver.shiftUpForCreate(
                    resolvedIndex,
                    () -> lessonRepository.shiftOrderIndexesUp(courseId, resolvedIndex, null)
            );
            log.info("[LESSON] Shifted {} lesson(s) with orderIndex >= {} in course id={}",
                    shifted, resolvedIndex, courseId);
        }
        int orderIndex = resolvedIndex;

        // Check phòng trường hợp hiếm (race condition giữa shift và save)
        if (lessonRepository.existsByCourseIdAndOrderIndexExcludingId(courseId, orderIndex, null)) {
            log.warn("[LESSON] OrderIndex conflict detected at createLesson for course id={}, orderIndex={}",
                    courseId, orderIndex);
            throw new AppException(ErrorCode.LESSON_ORDER_CONFLICT);
        }

        Lesson lesson = lessonMapper.toLessonEntity(request);
        lesson.setCourse(course);
        lesson.setOrderIndex(orderIndex);

        Lesson saved = lessonRepository.save(lesson);
        log.info("[LESSON] Created lesson id={}, title='{}', orderIndex={} in course id={}",
                saved.getId(), saved.getTitle(), saved.getOrderIndex(), courseId);

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

            if (movement == OrderIndexResolver.Movement.MOVE_UP) {
                // Di chuyển lên trên: dời các lesson trong [target, oldOrderIndex-1] xuống +1.
                // Dùng marker tạm (-oldOrderIndex) để tránh UNIQUE conflict khi shift.
                lesson.setOrderIndex(-oldOrderIndex);
                int shifted = lessonRepository.shiftOrderIndexesUp(courseId, target, lessonId);
                log.info("[LESSON] Move-up: shifted {} lesson(s) in [{}, {}] for lesson id={}",
                        shifted, target, oldOrderIndex - 1, lessonId);
                lesson.setOrderIndex(target);
            } else if (movement == OrderIndexResolver.Movement.MOVE_DOWN) {
                // Di chuyển xuống dưới: dời các lesson trong (oldOrderIndex, target] xuống -1.
                Integer maxOrder = lessonRepository.findMaxOrderIndexByCourseId(courseId);
                int currentMax = (maxOrder != null ? maxOrder : 0);
                if (target <= currentMax) {
                    lesson.setOrderIndex(-oldOrderIndex);
                    lessonRepository.shiftOrderIndexesDownInRange(courseId, oldOrderIndex, target, lessonId);
                    log.info("[LESSON] Move-down: shifted lessons in ({}, {}] for lesson id={}",
                            oldOrderIndex, target, lessonId);
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
        response.setHasQuiz(lessonSectionRepository
                .existsByLessonIdAndSectionTypeAndDeletedFalse(lessonId, LessonSectionType.TEST));

        // Load sections sau khi update
        List<LessonSection> sections = lessonSectionRepository.findByLessonIdWithAllItems(lessonId);
        loadNestedCollections(sections, lessonId);
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
    // SECTION MANAGEMENT — dành cho giáo viên/admin
    // =========================================================

    /**
     * Lấy danh sách sections của 1 lesson
     */
    @Transactional(readOnly = true)
    public List<LessonSectionResponse> getLessonSections(Long lessonId) {
        if (lessonRepository.findByIdAndDeletedFalse(lessonId).isEmpty()) {
            throw new AppException(ErrorCode.LESSON_NOT_FOUND);
        }
        List<LessonSection> sections = lessonSectionRepository.findByLessonIdWithAllItems(lessonId);
        loadNestedCollections(sections, lessonId);
        return lessonSectionMapper.toSectionResponses(sections);
    }

    /**
     * Lấy 1 section theo id
     */
    @Transactional(readOnly = true)
    public LessonSectionResponse getSection(Long sectionId) {
        LessonSection section = lessonSectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND));
        if (section.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND);
        }
        // Load full graph
        List<LessonSection> sections = lessonSectionRepository
                .findByLessonIdWithAllItems(section.getLesson().getId());
        loadNestedCollections(sections, section.getLesson().getId());
        return sections.stream()
                .filter(s -> s.getId().equals(sectionId))
                .findFirst()
                .map(lessonSectionMapper::toSectionResponse)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND));
    }

    /**
     * Admin/Giáo viên tạo section mới trong lesson.
     *
     * <p>Sau V19: orderIndex của section được bảo vệ tương tự lesson —
     * check trùng + shift các section phía sau +1.</p>
     */
    @Transactional
    public LessonSectionResponse createSection(Long lessonId, LessonSectionType sectionType, String title) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        // Bước 5: dùng OrderIndexResolver với maxOrder lấy từ list sections
        int newOrderIndex = OrderIndexResolver.maxOrderIndex(
                lessonSectionRepository.findByLessonIdAndDeletedFalseOrderByOrderIndexAsc(lessonId),
                LessonSection::getOrderIndex
        ) + 1;

        // Check phòng race condition (hiếm nhưng vẫn nên có)
        if (lessonSectionRepository.existsByLessonIdAndOrderIndexExcludingId(lessonId, newOrderIndex, null)) {
            log.warn("[SECTION] OrderIndex conflict detected at createSection for lesson id={}, orderIndex={}",
                    lessonId, newOrderIndex);
            throw new AppException(ErrorCode.LESSON_ORDER_CONFLICT);
        }

        LessonSection section = LessonSection.builder()
                .lesson(lesson)
                .sectionType(sectionType)
                .title(title)
                .orderIndex(newOrderIndex)
                .build();

        lesson.addSection(section);
        LessonSection saved = lessonSectionRepository.save(section);
        log.info("[SECTION] Created section id={}, type={}, orderIndex={} in lesson id={}",
                saved.getId(), sectionType, saved.getOrderIndex(), lessonId);

        return lessonSectionMapper.toSectionResponse(saved);
    }

    /**
     * Admin/Giáo viên cập nhật tiêu đề / orderIndex section.
     *
     * <p>Sau V19: nếu client đổi orderIndex sang giá trị khác thì shift các
     * section khác để giải phóng slot (giống logic lesson).</p>
     */
    @Transactional
    public LessonSectionResponse updateSection(Long sectionId, String title, Integer orderIndex) {
        LessonSection section = lessonSectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND));
        if (section.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND);
        }

        if (title != null) {
            section.setTitle(title);
        }

        if (orderIndex != null && orderIndex > 0 && !orderIndex.equals(section.getOrderIndex())) {
            Long lessonId = section.getLesson().getId();
            int currentMax = OrderIndexResolver.maxOrderIndex(
                    lessonSectionRepository.findByLessonIdAndDeletedFalseOrderByOrderIndexAsc(lessonId),
                    LessonSection::getOrderIndex
            );
            int target = OrderIndexResolver.clampToMaxPlusOne(
                    orderIndex, () -> currentMax
            );

            if (target != section.getOrderIndex()) {
                int oldOrderIndex = section.getOrderIndex();
                // Marker tạm để tránh UNIQUE conflict khi shift
                section.setOrderIndex(-oldOrderIndex);

                if (target < oldOrderIndex) {
                    // Move up: shift [target, oldOrderIndex-1] lên +1
                    int shifted = lessonSectionRepository.shiftOrderIndexesUp(lessonId, target, sectionId);
                    log.info("[SECTION] Move-up: shifted {} section(s) in lesson id={}", shifted, lessonId);
                } else if (target <= currentMax) {
                    // Move down: shift (oldOrderIndex, target] xuống -1
                    int shifted = lessonSectionRepository.shiftOrderIndexesDownInRange(
                            lessonId, oldOrderIndex, target, sectionId);
                    log.info("[SECTION] Move-down: shifted {} section(s) in lesson id={}", shifted, lessonId);
                }
                section.setOrderIndex(target);
            }

            // Check phòng race condition
            if (lessonSectionRepository.existsByLessonIdAndOrderIndexExcludingId(
                    section.getLesson().getId(), section.getOrderIndex(), sectionId)) {
                log.warn("[SECTION] OrderIndex conflict detected at updateSection for section id={}, orderIndex={}",
                        sectionId, section.getOrderIndex());
                throw new AppException(ErrorCode.LESSON_ORDER_CONFLICT);
            }
        }

        lessonSectionRepository.save(section);
        log.info("[SECTION] Updated section id={}, orderIndex={}", sectionId, section.getOrderIndex());

        return lessonSectionMapper.toSectionResponse(section);
    }

    /**
     * Admin/Giáo viên xoá mềm section
     */
    @Transactional
    public void deleteSection(Long sectionId) {
        LessonSection section = lessonSectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND));
        if (section.isDeleted()) {
            return; // đã xoá rồi, idempotent
        }
        section.softDelete();
        lessonSectionRepository.save(section);
        log.info("[SECTION] Soft-deleted section id={}", sectionId);
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
