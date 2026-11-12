package com.jp.elearningjp.service.curriculum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.config.TimeConfig;
import com.jp.elearningjp.dto.request.curriculum.LessonReviewSubmitRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonReviewRequestResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonReviewStateResponse;
import com.jp.elearningjp.dto.response.curriculum.LessonReviewSubmitResponse;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonDraft;
import com.jp.elearningjp.entity.curriculum.LessonReviewRequest;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.curriculum.LessonDraftRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.repository.curriculum.LessonReviewRequestRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.curriculum.snapshot.CourseSnapshotService;
import com.jp.elearningjp.service.curriculum.snapshot.LessonSnapshot;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import com.jp.elearningjp.shared.enums.DraftKind;
import com.jp.elearningjp.shared.enums.DraftStatus;
import com.jp.elearningjp.shared.enums.ReviewRequestStatus;
import com.jp.elearningjp.shared.event.curriculum.LessonReviewEvent;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Duyệt xuất bản theo bài trên nháp theo phía (V62 + V65, docs/lesson-review-design.md mục 8):
 * <pre>
 *  nháp TEACHER: EDITING --(GV gửi, kèm draftRevision; chụp nội dung)--> SUBMITTED --(ADMIN duyệt)--> bản chụp lên live
 *                   ^                                                         |
 *                   +------------(ADMIN từ chối có lý do / GV được giao rút)--+
 *  nháp ADMIN:   EDITING --(ADMIN lưu bài, không qua duyệt)--> nháp ADMIN lên live
 * </pre>
 * Nháp dựa trên bản live cũ (phía kia vừa xuất bản) không gửi / lưu được ({@code LESSON_DRAFT_OUTDATED}) — người dùng
 * "cập nhật bản mới" ({@link #restartDraft}). ADMIN không lưu bài khi giáo viên đang có yêu cầu chờ duyệt.
 * Chạy song song với luồng duyệt theo khóa ({@link CourseReviewService}); hai luồng chặn nhau khi đang chờ.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonReviewService {

    CourseRepository courseRepository;
    LessonRepository lessonRepository;
    LessonDraftRepository draftRepository;
    LessonReviewRequestRepository requestRepository;
    UserRepository userRepository;
    CurrentUserService currentUserService;
    CourseAccessGuard courseAccessGuard;
    CourseSnapshotService courseSnapshotService;
    LessonDraftService lessonDraftService;
    ApplicationEventPublisher eventPublisher;
    TransactionTemplate transactionTemplate;
    ObjectMapper objectMapper;

    private static final DateTimeFormatter DECISION_TIME =
            DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy").withZone(ZoneId.of(TimeConfig.APP_ZONE));

    // =========================================================
    // GỬI DUYỆT (nhiều bài, mỗi bài độc lập)
    // =========================================================

    /**
     * Giáo viên được giao gửi nhiều bài lên duyệt (không cần giữ khóa sửa khóa học). Mỗi bài chạy trong giao dịch
     * riêng: bài lỗi (xung đột, đang chờ, không có thay đổi, nháp đã cũ…) không làm hỏng bài khác. ADMIN nhận
     * 1 thông báo cho cả lượt. Không đánh dấu {@code @Transactional}: mỗi bài tự mở giao dịch qua {@link TransactionTemplate}.
     */
    public LessonReviewSubmitResponse submit(Long courseId, List<LessonReviewSubmitRequest.Item> items, String note) {
        Course course = findCourse(courseId);
        if (currentUserService.isAdmin()) {
            throw new AppException(ErrorCode.LESSON_REVIEW_SUBMIT_FORBIDDEN);
        }
        courseAccessGuard.requireAssignedTeacher(course, ErrorCode.LESSON_REVIEW_SUBMIT_FORBIDDEN);
        if (course.getReviewStatus() == CourseReviewStatus.PENDING_REVIEW) {
            throw new AppException(ErrorCode.LESSON_REVIEW_COURSE_PENDING);
        }
        // Gộp bài trùng (giữ lần xuất hiện đầu tiên)
        Map<Long, Long> revisions = new LinkedHashMap<>();
        if (items != null) {
            items.stream().filter(i -> i != null && i.getLessonId() != null && i.getDraftRevision() != null)
                    .forEach(i -> revisions.putIfAbsent(i.getLessonId(), i.getDraftRevision()));
        }
        if (revisions.isEmpty()) {
            throw new AppException(ErrorCode.LESSON_REVIEW_EMPTY);
        }

        Long actorId = currentUserService.getCurrentUserId();
        String trimmedNote = trimToNull(note);
        UUID batchId = UUID.randomUUID();
        List<LessonReviewSubmitResponse.Item> results = new ArrayList<>();
        List<LessonReviewRequest> submitted = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : revisions.entrySet()) {
            Long lessonId = entry.getKey();
            try {
                LessonReviewRequest request = transactionTemplate.execute(status ->
                        submitOne(courseId, lessonId, entry.getValue(), batchId, trimmedNote, actorId));
                submitted.add(request);
                results.add(LessonReviewSubmitResponse.Item.builder()
                        .lessonId(lessonId).submitted(true).requestId(request.getId()).build());
            } catch (RuntimeException ex) {
                AppException app = toAppException(ex);
                results.add(LessonReviewSubmitResponse.Item.builder()
                        .lessonId(lessonId).submitted(false)
                        .code(app.getErrorCode().getCode()).message(app.getMessage()).build());
            }
        }
        log.info("[LESSON_REVIEW] Batch {} of course id={} by user id={}: {} submitted, {} failed",
                batchId, courseId, actorId, submitted.size(), results.size() - submitted.size());

        if (!submitted.isEmpty()) {
            // Phát trong 1 giao dịch ngắn để listener AFTER_COMMIT chạy như các luồng khác
            transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(new LessonReviewEvent(
                    this, courseId, course.getTitle(), LessonReviewEvent.Action.SUBMITTED,
                    submitted.stream().map(r -> r.getLesson().getId()).toList(),
                    submitted.stream().map(r -> r.getLesson().getTitle()).toList(),
                    actorId, actorId, trimmedNote, false)));
        }
        return LessonReviewSubmitResponse.builder()
                .batchId(batchId)
                .submittedCount(submitted.size())
                .failedCount(results.size() - submitted.size())
                .results(results)
                .build();
    }

    /**
     * 1 bài: khóa nháp TEACHER bằng UPDATE có điều kiện (đúng draftRevision, dựa trên bản live hiện tại) rồi chụp nội
     * dung nháp vào yêu cầu — ADMIN duyệt xuất bản đúng bản chụp này.
     */
    LessonReviewRequest submitOne(Long courseId, Long lessonId, long draftRevision, UUID batchId,
                                  String note, Long actorId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        if (!Objects.equals(lesson.getCourse().getId(), courseId)) {
            throw new AppException(ErrorCode.LESSON_REVIEW_NOT_IN_COURSE);
        }
        lessonDraftService.requireVisible(lesson, DraftKind.TEACHER);
        LessonDraft draft = lessonDraftService.findActive(lessonId, DraftKind.TEACHER)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_DRAFT_NOT_FOUND));
        if (draftRepository.lockForReview(draft.getId(), draftRevision) == 0) {
            throw whyNotSubmittable(lesson, draft, draftRevision);
        }
        LessonSnapshot content = courseSnapshotService.buildDraft(draft);
        LessonReviewRequest request = requestRepository.save(LessonReviewRequest.builder()
                .lesson(lesson)
                .course(lesson.getCourse())
                .draft(draft)
                .batchId(batchId)
                .submittedBy(userRepository.getReferenceById(actorId))
                .note(note)
                .draftRevision(draftRevision)
                .content(write(content))
                .baseLiveRevision(draft.getBaseLiveRevision())
                .status(ReviewRequestStatus.PENDING)
                .build());
        log.info("[LESSON_REVIEW] Lesson id={} submitted (request id={}, draft id={}, revision={})",
                lessonId, request.getId(), draft.getId(), draftRevision);
        return request;
    }

    /** UPDATE gửi duyệt không khớp dòng nào → báo đúng lý do (nháp vừa đọc, câu UPDATE không đổi gì). */
    private AppException whyNotSubmittable(Lesson lesson, LessonDraft draft, long draftRevision) {
        if (draft.getStatus() == DraftStatus.SUBMITTED) {
            return new AppException(ErrorCode.LESSON_REVIEW_ALREADY_PENDING);
        }
        if (lessonDraftService.isOutdated(draft, lessonRepository.findLiveRevision(lesson.getId()).orElse(0L))) {
            return new AppException(ErrorCode.LESSON_DRAFT_OUTDATED);
        }
        if (!Objects.equals(draft.getDraftRevision(), draftRevision)) {
            // Có người vừa sửa bài sau khi giáo viên mở → tải bản mới nhất rồi gửi lại (không tự gửi)
            return new AppException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        if (!Boolean.TRUE.equals(draft.getHasChanges())) {
            return new AppException(ErrorCode.LESSON_REVIEW_NO_CHANGES);
        }
        return new AppException(ErrorCode.CONCURRENT_MODIFICATION);
    }

    // =========================================================
    // RÚT / DUYỆT / TỪ CHỐI
    // =========================================================

    /** Giáo viên <b>đang được giao</b> (bất kỳ ai) rút yêu cầu đang chờ của bài; giáo viên đã bị gỡ thì không. */
    @Transactional
    public LessonReviewRequestResponse withdraw(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        if (currentUserService.isAdmin()) {
            throw new AppException(ErrorCode.LESSON_REVIEW_WITHDRAW_FORBIDDEN);
        }
        // Xếp hàng với ADMIN đang duyệt / từ chối cùng khóa → đọc yêu cầu sau khi họ xong
        Course course = lockCourse(lesson.getCourse().getId(), ErrorCode.COURSE_NOT_FOUND);
        courseAccessGuard.requireAssignedTeacher(course, ErrorCode.LESSON_REVIEW_WITHDRAW_FORBIDDEN);
        LessonReviewRequest request = requestRepository.findByLessonIdAndStatus(lessonId, ReviewRequestStatus.PENDING)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_REVIEW_NOT_PENDING,
                        requestRepository.findFirstByLessonIdOrderByCreatedAtDescIdDesc(lessonId)
                                .map(LessonReviewService::describeDecision).orElse(null)));

        Long actorId = currentUserService.getCurrentUserId();
        close(request, ReviewRequestStatus.WITHDRAWN, actorId, null);
        unlockSubmittedDraft(request);
        log.info("[LESSON_REVIEW] Request id={} of lesson id={} withdrawn by user id={}",
                request.getId(), lessonId, actorId);

        publish(request, LessonReviewEvent.Action.WITHDRAWN, actorId, null, false);
        return toResponse(request);
    }

    /**
     * ADMIN duyệt: bản chụp lúc gửi lên bản live (phiên bản mới = bản live trước + bài này). Không bật xuất bản khóa.
     * Bản live đã đổi sau khi giáo viên gửi → {@code LESSON_DRAFT_OUTDATED} (ADMIN từ chối để giáo viên cập nhật bản mới).
     * Hai ADMIN cùng duyệt: người đầu thắng, người sau nhận kết quả của người trước.
     *
     * @param majorChange "thay đổi lớn" → học viên đã hoàn thành bài được báo (khi khóa đang xuất bản)
     */
    @Transactional
    public LessonReviewRequestResponse approve(Long requestId, String note, boolean majorChange) {
        LessonReviewRequest request = lockAndFindPending(requestId);
        Lesson lesson = request.getLesson();
        long liveRevision = lessonRepository.findLiveRevision(lesson.getId()).orElse(0L);
        if (request.getBaseLiveRevision() != null && request.getBaseLiveRevision() < liveRevision) {
            throw new AppException(ErrorCode.LESSON_DRAFT_OUTDATED,
                    "Bản học viên đang học của bài đã thay đổi sau khi giáo viên gửi — hãy từ chối để giáo viên cập nhật bản mới.");
        }
        LessonDraft draft = request.getDraft() != null ? request.getDraft()
                : lessonDraftService.findActive(lesson.getId(), DraftKind.TEACHER)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_DRAFT_NOT_FOUND));
        // Yêu cầu cũ (trước V65) chưa có bản chụp → chụp từ nháp (đang khóa chờ duyệt nên không đổi)
        LessonSnapshot content = request.getContent() != null
                ? read(request.getContent())
                : courseSnapshotService.buildDraft(draft);

        Long adminId = currentUserService.getCurrentUserId();
        request.setMajorChange(majorChange);
        close(request, ReviewRequestStatus.APPROVED, adminId, trimToNull(note));
        boolean published = publishToLive(request.getCourse(), lesson.getId(), content, draft.getId());
        log.info("[LESSON_REVIEW] Request id={} (lesson id={}) approved by admin id={} (course published={})",
                requestId, lesson.getId(), adminId, published);

        publish(request, LessonReviewEvent.Action.APPROVED, adminId, request.getReviewNote(), published, majorChange);
        return toResponse(request);
    }

    /** ADMIN từ chối (bắt buộc lý do): nháp giáo viên quay về đang soạn, bản live giữ nguyên. */
    @Transactional
    public LessonReviewRequestResponse reject(Long requestId, String reason) {
        LessonReviewRequest request = lockAndFindPending(requestId);
        Long adminId = currentUserService.getCurrentUserId();
        close(request, ReviewRequestStatus.REJECTED, adminId, trimToNull(reason));
        unlockSubmittedDraft(request);
        log.info("[LESSON_REVIEW] Request id={} (lesson id={}) rejected by admin id={}",
                requestId, request.getLesson().getId(), adminId);

        publish(request, LessonReviewEvent.Action.REJECTED, adminId, request.getReviewNote(), false);
        return toResponse(request);
    }

    /**
     * ADMIN "Lưu" 1 bài: đưa <b>nháp ADMIN</b> của bài lên bản live ngay, không qua duyệt. Phải đang giữ khóa sửa
     * khóa học phía ADMIN; giáo viên đang có yêu cầu chờ duyệt → duyệt / từ chối trước; nháp ADMIN đã cũ → cập nhật
     * bản mới trước. Nháp giáo viên chưa sửa gì tự theo bản live mới; đã sửa thì thành nháp cũ.
     *
     * @param majorChange "thay đổi lớn" → học viên đã hoàn thành bài được báo (khi khóa đang xuất bản)
     */
    @Transactional
    public void saveLesson(Long lessonId, boolean majorChange) {
        if (!currentUserService.isAdmin()) {
            throw new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS);
        }
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        Long courseId = lesson.getCourse().getId();
        lessonDraftService.requireVisible(lesson, DraftKind.ADMIN);
        // Thứ tự khóa dòng: khóa học trước, nháp sau — cùng thứ tự với thao tác ghi nội dung để không deadlock.
        Course course = lockCourse(courseId, ErrorCode.COURSE_NOT_FOUND);
        // Quyền + khóa sửa khóa học phía ADMIN (nháp ADMIN được chụp lên live)
        courseAccessGuard.requireOwnDraftEditable(course, ErrorCode.FORBIDDEN_COURSE_ACCESS);
        if (requestRepository.findByLessonIdAndStatus(lessonId, ReviewRequestStatus.PENDING).isPresent()) {
            throw new AppException(ErrorCode.LESSON_SAVE_BLOCKED_BY_PENDING);
        }
        LessonDraft draft = lessonDraftService.findActive(lessonId, DraftKind.ADMIN)
                .flatMap(d -> draftRepository.findByIdForUpdate(d.getId()))
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_DRAFT_NOT_FOUND));
        if (!Boolean.TRUE.equals(draft.getHasChanges())) {
            throw new AppException(ErrorCode.LESSON_REVIEW_NO_CHANGES);
        }
        if (lessonDraftService.isOutdated(draft, lessonRepository.findLiveRevision(lessonId).orElse(0L))) {
            throw new AppException(ErrorCode.LESSON_DRAFT_OUTDATED);
        }
        LessonSnapshot content = courseSnapshotService.buildDraft(draft);
        Long adminId = currentUserService.getCurrentUserId();
        boolean published = publishToLive(course, lessonId, content, draft.getId());
        log.info("[LESSON_REVIEW] Lesson id={} saved to live from ADMIN draft id={} by admin id={} (course published={})",
                lessonId, draft.getId(), adminId, published);

        eventPublisher.publishEvent(new LessonReviewEvent(this, courseId, course.getTitle(),
                LessonReviewEvent.Action.SAVED, List.of(lessonId), List.of(lesson.getTitle()),
                adminId, null, null, published, majorChange));
    }

    // =========================================================
    // NHÁP CỦA PHÍA MÌNH: BỎ NHÁP / CẬP NHẬT BẢN MỚI / XEM NHÁP CŨ
    // =========================================================

    /**
     * Dựng lại nháp phía mình từ bản học viên đang học (cần giữ khóa sửa khóa học phía mình).
     * <ul>
     *   <li>{@code keepOld = true} — "Cập nhật bản mới": nháp hiện tại thành nháp cũ chỉ đọc (thay nháp cũ trước đó)
     *       để tự chép lại phần còn thiếu.</li>
     *   <li>{@code keepOld = false} — "Bỏ nháp": thay đổi chưa xuất bản của phía mình bị bỏ, <b>không khôi phục được</b>.</li>
     * </ul>
     * Nháp đã gửi duyệt → phải rút / được duyệt / bị từ chối trước. Bài chưa có trong live → nháp mới trống.
     */
    @Transactional
    public void restartDraft(Long lessonId, boolean keepOld) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        Course course = lockCourse(lesson.getCourse().getId(), ErrorCode.COURSE_NOT_FOUND);
        // Chỉ nháp phía mình bị dựng lại → chỉ người soạn cùng phía được nhắc tải lại
        courseAccessGuard.requireOwnDraftEditable(course, ErrorCode.FORBIDDEN_LESSON_ACCESS);
        LessonDraft draft = lessonDraftService.restart(lesson, keepOld);
        log.info("[LESSON_DRAFT] {} draft of lesson id={} restarted from live by user id={} (keepOld={}, new draft id={})",
                draft.getKind(), lessonId, currentUserService.getCurrentUserId(), keepOld, draft.getId());
    }

    /**
     * Bản chụp nội dung bài lúc giáo viên gửi duyệt — ADMIN xem để duyệt (ADMIN không đọc được nháp giáo viên, kể cả
     * bài mới chưa duyệt). Yêu cầu cũ (trước V65) chưa có bản chụp → chụp từ nháp đã gửi (đang khóa).
     */
    @Transactional(readOnly = true)
    public LessonSnapshot getRequestContent(Long requestId) {
        if (!currentUserService.isAdmin()) {
            throw new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS);
        }
        LessonReviewRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_REVIEW_REQUEST_NOT_FOUND));
        if (request.getContent() != null) {
            return read(request.getContent());
        }
        if (request.getStatus() != ReviewRequestStatus.PENDING || request.getDraft() == null) {
            throw new AppException(ErrorCode.LESSON_DRAFT_NOT_FOUND);
        }
        return courseSnapshotService.buildDraft(request.getDraft());
    }

    /** Nội dung nháp cũ chỉ đọc của phía mình (giữ lại khi "cập nhật bản mới"). */
    @Transactional(readOnly = true)
    public LessonSnapshot getArchivedDraft(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        courseAccessGuard.requireAssignedOrAdmin(lesson.getCourse(), ErrorCode.FORBIDDEN_LESSON_ACCESS);
        LessonDraft archived = draftRepository.findArchived(lessonId, lessonDraftService.currentKind().name())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_DRAFT_NO_ARCHIVE));
        return courseSnapshotService.buildDraft(archived);
    }

    /**
     * Đưa nội dung {@code content} (đã chụp từ nháp {@code draftId}) lên bản live: phiên bản mới, tăng
     * {@code live_revision}, nháp vừa xuất bản trùng live, nháp phía kia tự theo live nếu chưa sửa. {@code course}
     * phải đã được khóa dòng ({@link #lockCourse}) để 2 thao tác cùng khóa không cùng tạo version_no.
     * Trả khóa có đang xuất bản không.
     */
    private boolean publishToLive(Course course, Long lessonId, LessonSnapshot content, Long draftId) {
        courseSnapshotService.publishLessons(course, Map.of(lessonId, content), currentUserService.getCurrentUserOrNull());
        courseRepository.save(course);
        lessonRepository.markLessonsPublished(List.of(lessonId));
        lessonRepository.bumpLiveRevision(List.of(lessonId));
        lessonDraftService.afterLivePublished(lessonId, draftId);
        return Boolean.TRUE.equals(course.getPublished());
    }

    /** Rút / từ chối: nháp giáo viên đã gửi quay về đang soạn. */
    private void unlockSubmittedDraft(LessonReviewRequest request) {
        Long draftId = request.getDraft() != null ? request.getDraft().getId()
                : lessonDraftService.findActive(request.getLesson().getId(), DraftKind.TEACHER)
                .map(LessonDraft::getId).orElse(null);
        if (draftId != null) {
            draftRepository.unlockReview(draftId);
        }
    }

    // =========================================================
    // ĐỌC
    // =========================================================

    /** Trạng thái duyệt từng bài của khóa theo nháp phía người xem (ADMIN hoặc giáo viên được giao). */
    @Transactional(readOnly = true)
    public List<LessonReviewStateResponse> getStates(Long courseId) {
        Course course = findCourse(courseId);
        courseAccessGuard.requireAssignedOrAdmin(course);
        DraftKind kind = lessonDraftService.currentKind();
        Map<Long, LessonReviewRequest> pending = requestRepository
                .findByCourseIdAndStatus(courseId, ReviewRequestStatus.PENDING).stream()
                .collect(Collectors.toMap(r -> r.getLesson().getId(), Function.identity(), (a, b) -> a));
        // Khu duyệt: thêm bài mới giáo viên đã gửi (ADMIN cần thấy để duyệt), bài đang soạn vẫn ẩn (V66)
        List<Lesson> lessons = lessonRepository.findReviewableByCourseId(courseId, kind);
        List<Long> lessonIds = lessons.stream().map(Lesson::getId).toList();
        Map<Long, LessonDraft> drafts = lessonIds.isEmpty() ? Map.of()
                : draftRepository.findActiveByLessonIds(lessonIds, kind.name()).stream()
                .collect(Collectors.toMap(d -> d.getLesson().getId(), Function.identity(), (a, b) -> a));
        java.util.Set<Long> withArchive = lessonIds.isEmpty() ? java.util.Set.of()
                : lessonIds.stream()
                .filter(id -> draftRepository.findArchived(id, kind.name()).isPresent())
                .collect(Collectors.toSet());
        return lessons.stream().map(l -> {
            LessonReviewRequest p = pending.get(l.getId());
            LessonDraft d = drafts.get(l.getId());
            boolean pendingReview = p != null;
            return LessonReviewStateResponse.builder()
                    .lessonId(l.getId())
                    .title(l.getTitle())
                    .orderIndex(l.getOrderIndex())
                    .reviewStatus(pendingReview ? CourseReviewStatus.PENDING_REVIEW : CourseReviewStatus.EDITING)
                    .hasUnpublishedChanges(d != null && Boolean.TRUE.equals(d.getHasChanges()))
                    .draftRevision(d != null && d.getDraftRevision() != null ? d.getDraftRevision() : 0L)
                    .inPublishedVersion(Boolean.TRUE.equals(l.getInPublishedVersion()))
                    .draftUpdatedByName(d != null ? d.getUpdatedByName() : null)
                    .draftUpdatedAt(d != null ? d.getContentUpdatedAt() : null)
                    .pendingRequestId(p != null ? p.getId() : null)
                    .pendingSubmittedByName(p != null && p.getSubmittedBy() != null
                            ? displayName(p.getSubmittedBy()) : null)
                    .pendingSubmittedAt(p != null ? p.getCreatedAt() : null)
                    .outdated(d != null && lessonDraftService.isOutdated(d, l.getLiveRevision()))
                    .hasArchivedDraft(withArchive.contains(l.getId()))
                    .build();
        }).toList();
    }

    private String write(LessonSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            log.error("[LESSON_REVIEW] Cannot serialize lesson snapshot", e);
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }
    }

    private LessonSnapshot read(String json) {
        try {
            return objectMapper.readValue(json, LessonSnapshot.class);
        } catch (JsonProcessingException e) {
            log.error("[LESSON_REVIEW] Cannot read submitted lesson snapshot", e);
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }
    }

    /**
     * Lịch sử gửi duyệt theo bài của khóa, mới nhất trước (ADMIN hoặc giáo viên được giao), phân trang ở DB —
     * khóa dùng lâu có thể tích lũy rất nhiều lượt gửi. {@code page} bắt đầu từ 1, {@code size} giới hạn 1–100.
     */
    @Transactional(readOnly = true)
    public PageResponse<LessonReviewRequestResponse> getHistory(Long courseId, int page, int size) {
        Course course = findCourse(courseId);
        courseAccessGuard.requireAssignedOrAdmin(course);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<LessonReviewRequest> result = requestRepository.findByCourseIdOrderByCreatedAtDescIdDesc(
                courseId, PageRequest.of(Math.max(0, page - 1), safeSize));
        return PageResponse.<LessonReviewRequestResponse>builder()
                .items(result.getContent().stream().map(LessonReviewService::toResponse).toList())
                .page(Math.max(1, page))
                .size(safeSize)
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    /** Hàng chờ duyệt theo bài của ADMIN (cũ nhất trước). {@code page} bắt đầu từ 1. */
    @Transactional(readOnly = true)
    public PageResponse<LessonReviewRequestResponse> getQueue(ReviewRequestStatus status, int page, int size) {
        return getQueue(status, null, page, size);
    }

    /** @param keyword tìm theo tên bài, tên khóa hoặc tên người gửi (null / rỗng = không lọc) */
    @Transactional(readOnly = true)
    public PageResponse<LessonReviewRequestResponse> getQueue(ReviewRequestStatus status, String keyword, int page, int size) {
        ReviewRequestStatus effective = status != null ? status : ReviewRequestStatus.PENDING;
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(Math.max(0, page - 1), safeSize);
        Page<LessonReviewRequest> result = keyword == null || keyword.isBlank()
                ? requestRepository.findQueue(effective, pageable)
                : requestRepository.searchQueue(effective,
                        "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT) + "%", pageable);
        return PageResponse.<LessonReviewRequestResponse>builder()
                .items(result.getContent().stream().map(LessonReviewService::toResponse).toList())
                .page(Math.max(1, page))
                .size(safeSize)
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

    /**
     * Khóa dòng khóa học tới hết giao dịch. Phải gọi TRƯỚC khi nạp entity khóa học / yêu cầu duyệt trong
     * giao dịch: thao tác sau chờ thao tác trước commit rồi mới đọc → thấy dữ liệu mới nhất.
     */
    private Course lockCourse(Long courseId, ErrorCode notFound) {
        return courseRepository.findByIdForUpdate(courseId).orElseThrow(() -> new AppException(notFound));
    }

    /**
     * Duyệt / từ chối: khóa dòng khóa học rồi mới đọc yêu cầu → hai ADMIN cùng xử lý thì người sau đọc
     * được kết quả của người trước (báo lại cho họ) thay vì xung đột version.
     */
    private LessonReviewRequest lockAndFindPending(Long requestId) {
        Long courseId = requestRepository.findCourseIdById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_REVIEW_REQUEST_NOT_FOUND));
        lockCourse(courseId, ErrorCode.LESSON_REVIEW_REQUEST_NOT_FOUND);
        LessonReviewRequest request = requestRepository.findById(requestId)
                .filter(r -> !r.getLesson().isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_REVIEW_REQUEST_NOT_FOUND));
        if (request.getStatus() != ReviewRequestStatus.PENDING) {
            throw new AppException(ErrorCode.LESSON_REVIEW_NOT_PENDING, describeDecision(request));
        }
        return request;
    }

    private void close(LessonReviewRequest request, ReviewRequestStatus status, Long actorId, String reviewNote) {
        request.setStatus(status);
        request.setReviewedBy(userRepository.getReferenceById(actorId));
        request.setReviewNote(reviewNote);
        request.setReviewedAt(Instant.now());
        requestRepository.save(request);
    }

    private void publish(LessonReviewRequest request, LessonReviewEvent.Action action, Long actorId, String note,
                         boolean liveChangedWhilePublished) {
        publish(request, action, actorId, note, liveChangedWhilePublished, false);
    }

    private void publish(LessonReviewRequest request, LessonReviewEvent.Action action, Long actorId, String note,
                         boolean liveChangedWhilePublished, boolean majorChange) {
        Lesson lesson = request.getLesson();
        eventPublisher.publishEvent(new LessonReviewEvent(this, request.getCourse().getId(),
                request.getCourse().getTitle(), action, List.of(lesson.getId()), List.of(lesson.getTitle()),
                actorId, request.getSubmittedBy() != null ? request.getSubmittedBy().getId() : null,
                note, liveChangedWhilePublished, majorChange));
    }

    /** Lỗi của 1 bài trong lượt gửi → mã nghiệp vụ để trả riêng cho bài đó. */
    private static AppException toAppException(RuntimeException ex) {
        if (ex instanceof AppException app) {
            return app;
        }
        if (ex instanceof ObjectOptimisticLockingFailureException) {
            return new AppException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        if (ex instanceof DataIntegrityViolationException) {
            // Partial unique index: bài vừa có yêu cầu chờ do lượt gửi khác chạy song song
            return new AppException(ErrorCode.LESSON_REVIEW_ALREADY_PENDING);
        }
        log.error("[LESSON_REVIEW] Unexpected error while submitting lesson", ex);
        return new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
    }

    /** Vd: "Yêu cầu duyệt bài này đã được Nguyễn A duyệt lúc 10:32 11/11/2026. Hãy tải lại trang." */
    static String describeDecision(LessonReviewRequest r) {
        String verb = switch (r.getStatus()) {
            case APPROVED -> "duyệt";
            case REJECTED -> "từ chối";
            case WITHDRAWN -> "rút";
            case PENDING -> null;
        };
        if (verb == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("Yêu cầu duyệt bài này đã được ");
        sb.append(r.getReviewedBy() != null ? displayName(r.getReviewedBy()) : "người khác").append(' ').append(verb);
        if (r.getReviewedAt() != null) {
            sb.append(" lúc ").append(DECISION_TIME.format(r.getReviewedAt()));
        }
        sb.append('.');
        if (r.getStatus() == ReviewRequestStatus.REJECTED && r.getReviewNote() != null) {
            sb.append(" Lý do: ").append(r.getReviewNote());
            if (!r.getReviewNote().matches("(?s).*[.!?…]\\s*$")) {
                sb.append('.');
            }
        }
        return sb.append(" Hãy tải lại trang.").toString();
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String displayName(User u) {
        return u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getEmail();
    }

    private static LessonReviewRequestResponse toResponse(LessonReviewRequest r) {
        User submitter = r.getSubmittedBy();
        User reviewer = r.getReviewedBy();
        return LessonReviewRequestResponse.builder()
                .id(r.getId())
                .lessonId(r.getLesson().getId())
                .lessonTitle(r.getLesson().getTitle())
                .courseId(r.getCourse().getId())
                .courseTitle(r.getCourse().getTitle())
                .batchId(r.getBatchId())
                .status(r.getStatus())
                .note(r.getNote())
                .submittedById(submitter != null ? submitter.getId() : null)
                .submittedByName(submitter != null ? displayName(submitter) : null)
                .createdAt(r.getCreatedAt())
                .reviewedById(reviewer != null ? reviewer.getId() : null)
                .reviewedByName(reviewer != null ? displayName(reviewer) : null)
                .reviewNote(r.getReviewNote())
                .reviewedAt(r.getReviewedAt())
                .majorChange(Boolean.TRUE.equals(r.getMajorChange()))
                .build();
    }
}
