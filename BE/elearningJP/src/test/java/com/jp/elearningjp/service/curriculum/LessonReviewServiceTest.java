package com.jp.elearningjp.service.curriculum;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.dto.request.curriculum.LessonReviewSubmitRequest;
import com.jp.elearningjp.dto.response.curriculum.LessonDetailResponse;
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
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Duyệt theo bài trên nháp theo phía (V62 + V65): gửi nhiều bài độc lập (chụp nội dung lúc gửi), rút, duyệt bản chụp /
 * từ chối, ADMIN lưu nháp ADMIN, nháp đã cũ, bỏ nháp / cập nhật bản mới.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LessonReviewServiceTest {

    private static final Long COURSE_ID = 10L;
    private static final Long TEACHER_ID = 5L;
    private static final Long OTHER_TEACHER_ID = 6L;
    private static final Long ADMIN_ID = 1L;
    private static final Long REQUEST_ID = 100L;

    @Mock CourseRepository courseRepository;
    @Mock LessonRepository lessonRepository;
    @Mock LessonDraftRepository draftRepository;
    @Mock LessonReviewRequestRepository requestRepository;
    @Mock UserRepository userRepository;
    @Mock CurrentUserService currentUserService;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock CourseSnapshotService courseSnapshotService;
    @Mock LessonDraftService lessonDraftService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TransactionTemplate transactionTemplate;
    @Spy ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @InjectMocks LessonReviewService service;

    private Course course;
    private Lesson lessonA;
    private Lesson lessonB;
    private Lesson lessonC;

    private static User user(Long id) {
        User u = User.builder().email("u" + id + "@test.local").fullName("U" + id).build();
        u.setId(id);
        return u;
    }

    private Lesson lesson(Long id, String title) {
        Lesson l = Lesson.builder().course(course).title(title).orderIndex(id.intValue()).build();
        l.setId(id);
        when(lessonRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(l));
        when(lessonRepository.findLiveRevision(id)).thenReturn(Optional.of(0L));
        return l;
    }

    /** Nháp đang dùng của 1 phía (id = lessonId * 10 + 0 / 1). */
    private LessonDraft draft(Lesson lesson, DraftKind kind, DraftStatus status, long revision, boolean changed, long base) {
        LessonDraft d = LessonDraft.builder().lesson(lesson).kind(kind).status(status)
                .draftRevision(revision).hasChanges(changed).baseLiveRevision(base).build();
        d.setId(lesson.getId() * 10 + (kind == DraftKind.ADMIN ? 1 : 0));
        when(lessonDraftService.findActive(lesson.getId(), kind)).thenReturn(Optional.of(d));
        when(draftRepository.findByIdForUpdate(d.getId())).thenReturn(Optional.of(d));
        return d;
    }

    private static LessonSnapshot snapshot(String title) {
        return LessonSnapshot.builder().detail(LessonDetailResponse.builder().title(title).build()).build();
    }

    private static void assertRejected(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private void actAs(Long userId, boolean admin) {
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(currentUserService.isAdmin()).thenReturn(admin);
        when(lessonDraftService.currentKind()).thenReturn(admin ? DraftKind.ADMIN : DraftKind.TEACHER);
    }

    private static LessonReviewSubmitRequest.Item item(Long lessonId, long revision) {
        return LessonReviewSubmitRequest.Item.builder().lessonId(lessonId).draftRevision(revision).build();
    }

    private LessonReviewRequest pending(Lesson lesson, Long submitterId, LessonDraft draft, String content) {
        LessonReviewRequest r = LessonReviewRequest.builder()
                .lesson(lesson).course(course).draft(draft).submittedBy(user(submitterId)).draftRevision(3L)
                .content(content).baseLiveRevision(0L)
                .status(ReviewRequestStatus.PENDING).build();
        r.setId(REQUEST_ID);
        when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(r));
        when(requestRepository.findCourseIdById(REQUEST_ID)).thenReturn(Optional.of(COURSE_ID));
        when(requestRepository.findByLessonIdAndStatus(lesson.getId(), ReviewRequestStatus.PENDING))
                .thenReturn(Optional.of(r));
        return r;
    }

    private List<LessonReviewEvent> events() {
        ArgumentCaptor<ApplicationEvent> captor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeast(0)).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .filter(LessonReviewEvent.class::isInstance).map(LessonReviewEvent.class::cast).toList();
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        course = Course.builder().title("JLPT N5").published(true)
                .reviewStatus(CourseReviewStatus.EDITING).hasUnpublishedChanges(true).build();
        course.setId(COURSE_ID);
        when(courseRepository.findByIdAndDeletedFalse(COURSE_ID)).thenReturn(Optional.of(course));
        when(courseRepository.findByIdForUpdate(COURSE_ID)).thenReturn(Optional.of(course));
        lessonA = lesson(1L, "Bài A");
        lessonB = lesson(2L, "Bài B");
        lessonC = lesson(3L, "Bài C");
        when(requestRepository.save(any(LessonReviewRequest.class))).thenAnswer(inv -> {
            LessonReviewRequest r = inv.getArgument(0);
            if (r.getId() == null) r.setId(500L + r.getLesson().getId());
            return r;
        });
        when(userRepository.getReferenceById(any())).thenAnswer(inv -> user(inv.getArgument(0)));
        when(courseSnapshotService.buildDraft(any())).thenAnswer(inv ->
                snapshot("nháp " + ((LessonDraft) inv.getArgument(0)).getId()));
        when(lessonDraftService.isOutdated(any(), any())).thenAnswer(inv -> {
            LessonDraft d = inv.getArgument(0);
            Long live = inv.getArgument(1);
            return d.getBaseLiveRevision() < (live != null ? live : 0L);
        });
        // TransactionTemplate thật mở giao dịch; ở đây chạy thẳng callback
        when(transactionTemplate.execute(any())).thenAnswer(inv ->
                ((TransactionCallback<Object>) inv.getArgument(0)).doInTransaction(null));
        doAnswer(inv -> {
            ((Consumer<TransactionStatus>) inv.getArgument(0)).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        actAs(TEACHER_ID, false);
    }

    // ---------------- submit (nhiều bài, mỗi bài độc lập)

    @Test
    void submit_eachLessonIndependent_snapshotsContent_conflictAndPendingPerLesson_oneAdminNotification() {
        LessonDraft a = draft(lessonA, DraftKind.TEACHER, DraftStatus.EDITING, 3L, true, 0L);
        when(draftRepository.lockForReview(a.getId(), 3L)).thenReturn(1);
        // Bài B: có người vừa sửa sau khi giáo viên mở (revision 4 ≠ 3) → xung đột
        LessonDraft b = draft(lessonB, DraftKind.TEACHER, DraftStatus.EDITING, 4L, true, 0L);
        when(draftRepository.lockForReview(b.getId(), 3L)).thenReturn(0);
        // Bài C: giáo viên khác vừa gửi
        LessonDraft c = draft(lessonC, DraftKind.TEACHER, DraftStatus.SUBMITTED, 7L, true, 0L);
        when(draftRepository.lockForReview(c.getId(), 7L)).thenReturn(0);

        LessonReviewSubmitResponse res = service.submit(COURSE_ID,
                List.of(item(1L, 3L), item(2L, 3L), item(3L, 7L)), "  Nhờ duyệt  ");

        assertThat(res.getSubmittedCount()).isEqualTo(1);
        assertThat(res.getResults()).extracting(LessonReviewSubmitResponse.Item::getLessonId,
                        LessonReviewSubmitResponse.Item::isSubmitted, LessonReviewSubmitResponse.Item::getCode)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, true, null),
                        org.assertj.core.groups.Tuple.tuple(2L, false, ErrorCode.CONCURRENT_MODIFICATION.getCode()),
                        org.assertj.core.groups.Tuple.tuple(3L, false, ErrorCode.LESSON_REVIEW_ALREADY_PENDING.getCode()));

        ArgumentCaptor<LessonReviewRequest> saved = ArgumentCaptor.forClass(LessonReviewRequest.class);
        verify(requestRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getDraft()).isEqualTo(a);
        assertThat(saved.getValue().getContent()).contains("nháp 10");
        assertThat(saved.getValue().getBaseLiveRevision()).isZero();
        // Gửi duyệt không cần giữ khóa sửa khóa học
        verify(courseAccessGuard, never()).requireContentEditable(any());
        verify(courseAccessGuard, never()).requireOwnDraftEditable(any(), any());

        List<LessonReviewEvent> events = events();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getAction()).isEqualTo(LessonReviewEvent.Action.SUBMITTED);
        assertThat(events.get(0).getLessonIds()).containsExactly(1L);
        assertThat(events.get(0).getNote()).isEqualTo("Nhờ duyệt");
    }

    @Test
    void submit_outdatedDraft_or_noChanges_reportedPerLesson() {
        // Bài A: ADMIN vừa lưu bài (live 1) sau khi nháp được tạo (base 0)
        when(lessonRepository.findLiveRevision(1L)).thenReturn(Optional.of(1L));
        draft(lessonA, DraftKind.TEACHER, DraftStatus.EDITING, 3L, true, 0L);
        LessonDraft b = draft(lessonB, DraftKind.TEACHER, DraftStatus.EDITING, 3L, false, 0L);
        when(draftRepository.lockForReview(anyLong(), anyLong())).thenReturn(0);

        LessonReviewSubmitResponse res = service.submit(COURSE_ID, List.of(item(1L, 3L), item(b.getLesson().getId(), 3L)), null);

        assertThat(res.getResults()).extracting(LessonReviewSubmitResponse.Item::getCode)
                .containsExactly(ErrorCode.LESSON_DRAFT_OUTDATED.getCode(), ErrorCode.LESSON_REVIEW_NO_CHANGES.getCode());
        assertThat(events()).isEmpty();
    }

    @Test
    void submit_lessonOfAnotherCourse_rejectedForThatLesson() {
        Course other = Course.builder().title("Khác").build();
        other.setId(99L);
        lessonA.setCourse(other);

        LessonReviewSubmitResponse res = service.submit(COURSE_ID, List.of(item(1L, 3L)), null);

        assertThat(res.getResults().get(0).getCode()).isEqualTo(ErrorCode.LESSON_REVIEW_NOT_IN_COURSE.getCode());
        verify(draftRepository, never()).lockForReview(anyLong(), anyLong());
    }

    @Test
    void submit_byAdmin_or_notAssigned_or_coursePending_or_empty_rejected() {
        actAs(ADMIN_ID, true);
        assertRejected(() -> service.submit(COURSE_ID, List.of(item(1L, 3L)), null),
                ErrorCode.LESSON_REVIEW_SUBMIT_FORBIDDEN);

        actAs(OTHER_TEACHER_ID, false);
        doThrow(new AppException(ErrorCode.LESSON_REVIEW_SUBMIT_FORBIDDEN))
                .when(courseAccessGuard).requireAssignedTeacher(course, ErrorCode.LESSON_REVIEW_SUBMIT_FORBIDDEN);
        assertRejected(() -> service.submit(COURSE_ID, List.of(item(1L, 3L)), null),
                ErrorCode.LESSON_REVIEW_SUBMIT_FORBIDDEN);

        org.mockito.Mockito.reset(courseAccessGuard);
        course.setReviewStatus(CourseReviewStatus.PENDING_REVIEW);
        assertRejected(() -> service.submit(COURSE_ID, List.of(item(1L, 3L)), null),
                ErrorCode.LESSON_REVIEW_COURSE_PENDING);

        course.setReviewStatus(CourseReviewStatus.EDITING);
        assertRejected(() -> service.submit(COURSE_ID, List.of(), null), ErrorCode.LESSON_REVIEW_EMPTY);
        verify(draftRepository, never()).lockForReview(anyLong(), anyLong());
    }

    // ---------------- withdraw

    @Test
    void withdraw_byAnyAssignedTeacher_unlocksTeacherDraft() {
        LessonDraft a = draft(lessonA, DraftKind.TEACHER, DraftStatus.SUBMITTED, 3L, true, 0L);
        LessonReviewRequest r = pending(lessonA, TEACHER_ID, a, null);
        actAs(OTHER_TEACHER_ID, false);

        service.withdraw(1L);

        assertThat(r.getStatus()).isEqualTo(ReviewRequestStatus.WITHDRAWN);
        assertThat(r.getReviewedBy().getId()).isEqualTo(OTHER_TEACHER_ID);
        verify(draftRepository).unlockReview(a.getId());
        verify(courseRepository).findByIdForUpdate(COURSE_ID);
        assertThat(events().get(0).getAction()).isEqualTo(LessonReviewEvent.Action.WITHDRAWN);
    }

    @Test
    void withdraw_unassignedTeacher_or_admin_rejected() {
        LessonDraft a = draft(lessonA, DraftKind.TEACHER, DraftStatus.SUBMITTED, 3L, true, 0L);
        LessonReviewRequest r = pending(lessonA, TEACHER_ID, a, null);
        doThrow(new AppException(ErrorCode.LESSON_REVIEW_WITHDRAW_FORBIDDEN))
                .when(courseAccessGuard).requireAssignedTeacher(course, ErrorCode.LESSON_REVIEW_WITHDRAW_FORBIDDEN);
        assertRejected(() -> service.withdraw(1L), ErrorCode.LESSON_REVIEW_WITHDRAW_FORBIDDEN);

        actAs(ADMIN_ID, true);
        assertRejected(() -> service.withdraw(1L), ErrorCode.LESSON_REVIEW_WITHDRAW_FORBIDDEN);
        assertThat(r.getStatus()).isEqualTo(ReviewRequestStatus.PENDING);
        verify(draftRepository, never()).unlockReview(anyLong());
    }

    @Test
    void withdraw_afterAdminDecided_isToldTheResult() {
        LessonReviewRequest decided = LessonReviewRequest.builder().lesson(lessonA).course(course)
                .status(ReviewRequestStatus.APPROVED).reviewedBy(user(ADMIN_ID))
                .reviewedAt(Instant.parse("2026-11-11T03:32:00Z")).build();
        when(requestRepository.findByLessonIdAndStatus(1L, ReviewRequestStatus.PENDING)).thenReturn(Optional.empty());
        when(requestRepository.findFirstByLessonIdOrderByCreatedAtDescIdDesc(1L)).thenReturn(Optional.of(decided));

        assertThatThrownBy(() -> service.withdraw(1L))
                .isInstanceOf(AppException.class)
                .hasMessage("Yêu cầu duyệt bài này đã được U1 duyệt lúc 10:32 11/11/2026. Hãy tải lại trang.");
    }

    // ---------------- approve / reject

    @Test
    void approve_publishesSubmittedSnapshot_notCurrentDraft_thenUpdatesDrafts() throws Exception {
        LessonDraft a = draft(lessonA, DraftKind.TEACHER, DraftStatus.SUBMITTED, 3L, true, 0L);
        String content = objectMapper.writeValueAsString(snapshot("bản gửi"));
        LessonReviewRequest r = pending(lessonA, TEACHER_ID, a, content);
        actAs(ADMIN_ID, true);

        service.approve(REQUEST_ID, " OK ", false);

        assertThat(r.getStatus()).isEqualTo(ReviewRequestStatus.APPROVED);
        assertThat(r.getReviewNote()).isEqualTo("OK");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, LessonSnapshot>> published = ArgumentCaptor.forClass(Map.class);
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(courseRepository, requestRepository,
                courseSnapshotService, lessonRepository, lessonDraftService);
        order.verify(courseRepository).findByIdForUpdate(COURSE_ID);
        order.verify(requestRepository).findById(REQUEST_ID);
        order.verify(courseSnapshotService).publishLessons(eq(course), published.capture(), any());
        order.verify(lessonRepository).bumpLiveRevision(List.of(1L));
        order.verify(lessonDraftService).afterLivePublished(1L, a.getId());
        assertThat(published.getValue().get(1L).getDetail().getTitle()).isEqualTo("bản gửi");
        verify(courseSnapshotService, never()).buildDraft(any());

        LessonReviewEvent e = events().get(0);
        assertThat(e.getAction()).isEqualTo(LessonReviewEvent.Action.APPROVED);
        assertThat(e.getSubmitterId()).isEqualTo(TEACHER_ID);
        assertThat(e.isLiveChangedWhilePublished()).isTrue();
    }

    @Test
    void approve_legacyRequestWithoutSnapshot_buildsFromLockedDraft_majorChangeRecorded() {
        LessonDraft a = draft(lessonA, DraftKind.TEACHER, DraftStatus.SUBMITTED, 3L, true, 0L);
        LessonReviewRequest r = pending(lessonA, TEACHER_ID, a, null);
        actAs(ADMIN_ID, true);

        var res = service.approve(REQUEST_ID, null, true);

        verify(courseSnapshotService).buildDraft(a);
        assertThat(r.getMajorChange()).isTrue();
        assertThat(res.isMajorChange()).isTrue();
        assertThat(events().get(0).isMajorChange()).isTrue();
    }

    @Test
    void approve_liveChangedSinceSubmit_outdated_nothingPublished() {
        LessonDraft a = draft(lessonA, DraftKind.TEACHER, DraftStatus.SUBMITTED, 3L, true, 0L);
        LessonReviewRequest r = pending(lessonA, TEACHER_ID, a, null);
        when(lessonRepository.findLiveRevision(1L)).thenReturn(Optional.of(1L));
        actAs(ADMIN_ID, true);

        assertRejected(() -> service.approve(REQUEST_ID, null, false), ErrorCode.LESSON_DRAFT_OUTDATED);
        assertThat(r.getStatus()).isEqualTo(ReviewRequestStatus.PENDING);
        verify(courseSnapshotService, never()).publishLessons(any(), any(), any());
    }

    @Test
    void approve_secondAdmin_isToldFirstAdminsDecision_noSnapshot() {
        LessonReviewRequest r = pending(lessonA, TEACHER_ID, null, null);
        r.setStatus(ReviewRequestStatus.REJECTED);
        r.setReviewedBy(user(ADMIN_ID));
        r.setReviewNote("Thiếu ví dụ");
        r.setReviewedAt(Instant.parse("2026-11-11T03:32:00Z"));
        actAs(2L, true);

        assertThatThrownBy(() -> service.approve(REQUEST_ID, null, false))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode(), Throwable::getMessage)
                .containsExactly(ErrorCode.LESSON_REVIEW_NOT_PENDING,
                        "Yêu cầu duyệt bài này đã được U1 từ chối lúc 10:32 11/11/2026. Lý do: Thiếu ví dụ. "
                                + "Hãy tải lại trang.");
        verify(courseSnapshotService, never()).publishLessons(any(), any(), any());
    }

    @Test
    void reject_unlocksTeacherDraft_keepsLive_carriesReason() {
        LessonDraft a = draft(lessonA, DraftKind.TEACHER, DraftStatus.SUBMITTED, 3L, true, 0L);
        LessonReviewRequest r = pending(lessonA, TEACHER_ID, a, null);
        actAs(ADMIN_ID, true);

        service.reject(REQUEST_ID, "Thiếu ví dụ");

        assertThat(r.getStatus()).isEqualTo(ReviewRequestStatus.REJECTED);
        verify(draftRepository).unlockReview(a.getId());
        verify(courseSnapshotService, never()).publishLessons(any(), any(), any());
        LessonReviewEvent e = events().get(0);
        assertThat(e.getAction()).isEqualTo(LessonReviewEvent.Action.REJECTED);
        assertThat(e.getNote()).isEqualTo("Thiếu ví dụ");
    }

    @Test
    void approveOrReject_unknownRequest_notFound() {
        when(requestRepository.findCourseIdById(404L)).thenReturn(Optional.empty());
        actAs(ADMIN_ID, true);
        assertRejected(() -> service.approve(404L, null, false), ErrorCode.LESSON_REVIEW_REQUEST_NOT_FOUND);
        assertRejected(() -> service.reject(404L, "x"), ErrorCode.LESSON_REVIEW_REQUEST_NOT_FOUND);
    }

    // ---------------- ADMIN lưu bài (nháp ADMIN)

    @Test
    void saveLesson_publishesAdminDraft_neverTeacherDraft() {
        actAs(ADMIN_ID, true);
        LessonDraft admin = draft(lessonA, DraftKind.ADMIN, DraftStatus.EDITING, 2L, true, 0L);
        draft(lessonA, DraftKind.TEACHER, DraftStatus.EDITING, 9L, true, 0L); // giáo viên đang sửa dở

        service.saveLesson(1L, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Long, LessonSnapshot>> published = ArgumentCaptor.forClass(Map.class);
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(courseRepository, courseAccessGuard,
                courseSnapshotService, lessonRepository, lessonDraftService);
        order.verify(courseRepository).findByIdForUpdate(COURSE_ID);
        order.verify(courseAccessGuard).requireOwnDraftEditable(course, ErrorCode.FORBIDDEN_COURSE_ACCESS);
        order.verify(courseSnapshotService).buildDraft(admin);
        order.verify(courseSnapshotService).publishLessons(eq(course), published.capture(), any());
        order.verify(lessonRepository).bumpLiveRevision(List.of(1L));
        order.verify(lessonDraftService).afterLivePublished(1L, admin.getId());
        assertThat(published.getValue().get(1L).getDetail().getTitle()).isEqualTo("nháp 11");
        verify(courseSnapshotService, times(1)).buildDraft(any());
        assertThat(events().get(0).getAction()).isEqualTo(LessonReviewEvent.Action.SAVED);
    }

    @Test
    void saveLesson_teacherPendingRequest_mustDecideFirst() {
        actAs(ADMIN_ID, true);
        draft(lessonA, DraftKind.ADMIN, DraftStatus.EDITING, 2L, true, 0L);
        pending(lessonA, TEACHER_ID, null, null);

        assertRejected(() -> service.saveLesson(1L, false), ErrorCode.LESSON_SAVE_BLOCKED_BY_PENDING);
        verify(courseSnapshotService, never()).publishLessons(any(), any(), any());
    }

    @Test
    void saveLesson_noChanges_outdated_noLock_or_byTeacher_rejected() {
        actAs(ADMIN_ID, true);
        LessonDraft admin = draft(lessonA, DraftKind.ADMIN, DraftStatus.EDITING, 2L, false, 0L);
        assertRejected(() -> service.saveLesson(1L, false), ErrorCode.LESSON_REVIEW_NO_CHANGES);

        admin.setHasChanges(true);
        when(lessonRepository.findLiveRevision(1L)).thenReturn(Optional.of(1L));
        assertRejected(() -> service.saveLesson(1L, false), ErrorCode.LESSON_DRAFT_OUTDATED);

        doThrow(new AppException(ErrorCode.COURSE_EDIT_SESSION_REQUIRED))
                .when(courseAccessGuard).requireOwnDraftEditable(course, ErrorCode.FORBIDDEN_COURSE_ACCESS);
        assertRejected(() -> service.saveLesson(1L, false), ErrorCode.COURSE_EDIT_SESSION_REQUIRED);

        actAs(TEACHER_ID, false);
        assertRejected(() -> service.saveLesson(1L, false), ErrorCode.FORBIDDEN_COURSE_ACCESS);
        verify(courseSnapshotService, never()).publishLessons(any(), any(), any());
    }

    // ---------------- nháp phía mình: bỏ nháp / cập nhật bản mới

    @Test
    void restartDraft_requiresLockFirst_thenRestartsOwnSide() {
        LessonDraft fresh = LessonDraft.builder().lesson(lessonA).kind(DraftKind.TEACHER).build();
        fresh.setId(99L);
        when(lessonDraftService.restart(lessonA, true)).thenReturn(fresh);

        service.restartDraft(1L, true);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(courseRepository, courseAccessGuard, lessonDraftService);
        order.verify(courseRepository).findByIdForUpdate(COURSE_ID);
        order.verify(courseAccessGuard).requireOwnDraftEditable(course, ErrorCode.FORBIDDEN_LESSON_ACCESS);
        order.verify(lessonDraftService).restart(lessonA, true);
    }

    @Test
    void restartDraft_withoutLock_nothingChanged() {
        doThrow(new AppException(ErrorCode.COURSE_EDIT_LOCKED_BY_OTHER))
                .when(courseAccessGuard).requireOwnDraftEditable(course, ErrorCode.FORBIDDEN_LESSON_ACCESS);
        assertRejected(() -> service.restartDraft(1L, false), ErrorCode.COURSE_EDIT_LOCKED_BY_OTHER);
        verify(lessonDraftService, never()).restart(any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    // ---------------- trạng thái từng bài (theo nháp phía người xem)

    @Test
    void states_useViewerSideDraft_pendingAndOutdatedFlags() {
        actAs(ADMIN_ID, true);
        lessonA.setInPublishedVersion(true);
        when(lessonRepository.findReviewableByCourseId(COURSE_ID, DraftKind.ADMIN))
                .thenReturn(List.of(lessonA, lessonB));
        org.springframework.test.util.ReflectionTestUtils.setField(lessonA, "liveRevision", 2L);
        LessonDraft adminA = LessonDraft.builder().lesson(lessonA).kind(DraftKind.ADMIN).status(DraftStatus.EDITING)
                .draftRevision(4L).hasChanges(true).baseLiveRevision(1L).build();
        adminA.setId(11L);
        LessonDraft adminB = LessonDraft.builder().lesson(lessonB).kind(DraftKind.ADMIN).status(DraftStatus.EDITING)
                .draftRevision(0L).hasChanges(false).baseLiveRevision(0L).build();
        adminB.setId(21L);
        when(draftRepository.findActiveByLessonIds(List.of(1L, 2L), "ADMIN")).thenReturn(List.of(adminA, adminB));
        when(draftRepository.findArchived(1L, "ADMIN")).thenReturn(Optional.of(adminA));
        LessonReviewRequest r = pending(lessonB, TEACHER_ID, null, null);
        when(requestRepository.findByCourseIdAndStatus(COURSE_ID, ReviewRequestStatus.PENDING)).thenReturn(List.of(r));

        var states = service.getStates(COURSE_ID);

        assertThat(states).hasSize(2);
        assertThat(states.get(0).getDraftRevision()).isEqualTo(4L);
        assertThat(states.get(0).isHasUnpublishedChanges()).isTrue();
        assertThat(states.get(0).isOutdated()).isTrue();
        assertThat(states.get(0).isHasArchivedDraft()).isTrue();
        assertThat(states.get(1).getReviewStatus()).isEqualTo(CourseReviewStatus.PENDING_REVIEW);
        assertThat(states.get(1).getPendingRequestId()).isEqualTo(REQUEST_ID);
        assertThat(states.get(1).getPendingSubmittedByName()).isEqualTo("U5");
        assertThat(states.get(1).isHasUnpublishedChanges()).isFalse();
        verify(courseAccessGuard).requireAssignedOrAdmin(course);
    }

    @Test
    void history_isPagedInDb_withOneBasedPageAndClampedSize() {
        LessonReviewRequest r = pending(lessonA, 5L, draft(lessonA, DraftKind.TEACHER, DraftStatus.SUBMITTED, 3, true, 0),
                "{}");
        ArgumentCaptor<org.springframework.data.domain.Pageable> pageable =
                ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        when(requestRepository.findByCourseIdOrderByCreatedAtDescIdDesc(eq(COURSE_ID), pageable.capture()))
                .thenAnswer(inv -> new org.springframework.data.domain.PageImpl<>(List.of(r), inv.getArgument(1), 201)); // trang cuối: 200 mục trước + 1

        var page = service.getHistory(COURSE_ID, 3, 500);

        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2); // page 3 (1-based) → index 2
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100); // size tối đa 100
        assertThat(page.getPage()).isEqualTo(3);
        assertThat(page.getSize()).isEqualTo(100);
        assertThat(page.getTotalElements()).isEqualTo(201);
        assertThat(page.getItems()).extracting(i -> i.getId()).containsExactly(REQUEST_ID);
        verify(courseAccessGuard).requireAssignedOrAdmin(course);
    }
}
