package com.jp.elearningjp.service.comment;

import com.jp.elearningjp.entity.comment.Comment;
import com.jp.elearningjp.entity.comment.CommentReply;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.comment.CommentReplyRepository;
import com.jp.elearningjp.repository.comment.CommentReportRepository;
import com.jp.elearningjp.repository.comment.CommentRepository;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.shared.enums.CommentTargetType;
import com.jp.elearningjp.shared.enums.ContentStatus;
import com.jp.elearningjp.shared.event.comment.CommentDeletedEvent;
import com.jp.elearningjp.shared.event.comment.CommentModeratedEvent;
import com.jp.elearningjp.dto.response.comment.ModerationItemResponse;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentModerationServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long TEACHER_ID = 2L;
    private static final Long AUTHOR_ID = 3L;
    private static final Long LESSON_ID = 20L;

    private CommentRepository commentRepository;
    private CommentReplyRepository replyRepository;
    private CommentReportRepository reportRepository;
    private LessonRepository lessonRepository;
    private CurrentUserService currentUserService;
    private ApplicationEventPublisher eventPublisher;
    private CommentModerationService service;

    private Comment lessonComment;
    private Comment vocabComment;

    private static User user(Long id) {
        User u = User.builder().email("u" + id + "@test.local").fullName("U" + id).build();
        u.setId(id);
        return u;
    }

    private static Comment comment(Long id, CommentTargetType type, Long targetId) {
        Comment c = Comment.builder().targetType(type).targetId(targetId).user(user(AUTHOR_ID))
                .content("nội dung vi phạm").replyCount(3).build();
        c.setId(id);
        return c;
    }

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        replyRepository = mock(CommentReplyRepository.class);
        reportRepository = mock(CommentReportRepository.class);
        lessonRepository = mock(LessonRepository.class);
        currentUserService = mock(CurrentUserService.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new CommentModerationService(commentRepository, replyRepository, reportRepository, lessonRepository,
                mock(VocabularyRepository.class), mock(WritingCharacterRepository.class),
                mock(GrammarPointRepository.class), currentUserService, eventPublisher);

        // Bài học thuộc khóa do TEACHER_ID tạo
        Course course = new Course();
        course.setId(10L);
        course.setCreatedBy(user(TEACHER_ID));
        Lesson lesson = Lesson.builder().course(course).title("Bài 1").build();
        lesson.setId(LESSON_ID);
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        lessonComment = comment(100L, CommentTargetType.LESSON, LESSON_ID);
        vocabComment = comment(200L, CommentTargetType.VOCABULARY, 5L);
        when(commentRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(lessonComment));
        when(commentRepository.findByIdAndDeletedFalse(200L)).thenReturn(Optional.of(vocabComment));
        when(commentRepository.findById(100L)).thenReturn(Optional.of(lessonComment));
    }

    private void loginAs(Long id, boolean admin) {
        when(currentUserService.isAdmin()).thenReturn(admin);
        when(currentUserService.getCurrentUserId()).thenReturn(id);
        when(currentUserService.getCurrentUser()).thenReturn(user(id));
    }

    private void assertRejected(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(expected);
    }

    // ============================================================
    // Phạm vi của TEACHER
    // ============================================================

    @Test
    void teacher_canModerateCommentInOwnCourseLesson() {
        loginAs(TEACHER_ID, false);

        service.hideComment(100L, null);

        assertThat(lessonComment.getStatus()).isEqualTo(ContentStatus.HIDDEN);
    }

    @Test
    void teacher_cannotModerateLessonOfAnotherTeachersCourse() {
        loginAs(99L, false);

        assertRejected(() -> service.hideComment(100L, null), ErrorCode.COMMENT_MODERATION_FORBIDDEN);
        assertThat(lessonComment.getStatus()).isEqualTo(ContentStatus.VISIBLE);
        verify(commentRepository, never()).save(any());
    }

    @Test
    void teacher_cannotModerateDictionaryComments() {
        loginAs(TEACHER_ID, false);

        assertRejected(() -> service.deleteComment(200L, null), ErrorCode.COMMENT_MODERATION_FORBIDDEN);
        assertThat(vocabComment.isDeleted()).isFalse();
    }

    @Test
    void admin_canModerateDictionaryComments() {
        loginAs(ADMIN_ID, true);

        service.deleteComment(200L, "Spam");

        assertThat(vocabComment.isDeleted()).isTrue();
    }

    // ============================================================
    // Bình luận gốc
    // ============================================================

    @Test
    void hideComment_closesReportsAndNotifiesAuthorWithNote() {
        loginAs(ADMIN_ID, true);

        service.hideComment(100L, "  Ngôn từ xúc phạm  ");

        verify(commentRepository).save(lessonComment);
        verify(reportRepository).closeOpenReportsOfComment(eq(100L), eq("RESOLVED"), eq(ADMIN_ID), any());
        ArgumentCaptor<CommentModeratedEvent> event = ArgumentCaptor.forClass(CommentModeratedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().getRecipientUserId()).isEqualTo(AUTHOR_ID);
        assertThat(event.getValue().getAction()).isEqualTo(CommentModeratedEvent.Action.HIDDEN);
        assertThat(event.getValue().getScope()).isEqualTo(CommentModeratedEvent.Scope.COMMENT);
        assertThat(event.getValue().getNote()).isEqualTo("Ngôn từ xúc phạm");
    }

    @Test
    void hideComment_alreadyHidden_doesNotNotifyAgain() {
        loginAs(ADMIN_ID, true);
        lessonComment.setStatus(ContentStatus.HIDDEN);

        service.hideComment(100L, null);

        verify(commentRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void restoreComment_setsVisible_withoutNotification() {
        loginAs(ADMIN_ID, true);
        lessonComment.setStatus(ContentStatus.HIDDEN);

        service.restoreComment(100L);

        assertThat(lessonComment.getStatus()).isEqualTo(ContentStatus.VISIBLE);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void deleteComment_publishesCleanupAndModeratedEvents() {
        loginAs(ADMIN_ID, true);

        service.deleteComment(100L, null);

        assertThat(lessonComment.isDeleted()).isTrue();
        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(events.capture());
        assertThat(events.getAllValues()).hasAtLeastOneElementOfType(CommentDeletedEvent.class)
                .hasAtLeastOneElementOfType(CommentModeratedEvent.class);
    }

    @Test
    void dismissReports_marksRejected_andKeepsCommentVisible() {
        loginAs(ADMIN_ID, true);

        service.dismissCommentReports(100L);

        verify(reportRepository).closeOpenReportsOfComment(eq(100L), eq("REJECTED"), eq(ADMIN_ID), any());
        assertThat(lessonComment.getStatus()).isEqualTo(ContentStatus.VISIBLE);
    }

    // ============================================================
    // Trả lời: giữ reply_count chỉ đếm trả lời đang hiển thị
    // ============================================================

    private CommentReply reply(Long id) {
        CommentReply r = CommentReply.builder().comment(lessonComment).user(user(AUTHOR_ID)).content("r").build();
        r.setId(id);
        when(replyRepository.findByIdAndDeletedFalse(id)).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void hideReply_decrementsReplyCount_restoreIncrements() {
        loginAs(ADMIN_ID, true);
        CommentReply r = reply(7L);

        service.hideReply(7L, null);
        assertThat(r.getStatus()).isEqualTo(ContentStatus.HIDDEN);
        verify(commentRepository).decrementReplyCount(100L);

        service.restoreReply(7L);
        assertThat(r.getStatus()).isEqualTo(ContentStatus.VISIBLE);
        verify(commentRepository).incrementReplyCount(100L);
    }

    @Test
    void deleteReply_decrementsByVisibleCountInSubtree_andClosesSubtreeReports() {
        loginAs(TEACHER_ID, false);
        reply(7L);
        when(replyRepository.findActiveSubtreeIds(7L)).thenReturn(List.of(7L, 8L));
        when(replyRepository.countVisibleActiveInSubtree(7L)).thenReturn(1);

        service.deleteReply(7L, null);

        verify(commentRepository).decrementReplyCountBy(100L, 1);
        verify(reportRepository).closeOpenReportsOfReplies(eq(List.of(7L, 8L)), eq("RESOLVED"), eq(TEACHER_ID), any());
    }

    @Test
    void replyOfDeletedComment_isNotFound() {
        loginAs(ADMIN_ID, true);
        reply(7L);
        lessonComment.softDelete();

        assertRejected(() -> service.hideReply(7L, null), ErrorCode.COMMENT_REPLY_NOT_FOUND);
    }

    // ============================================================
    // Tra cứu gộp (kind = null): phân trang chung bình luận + trả lời
    // ============================================================

    private static Instant at(long minute) {
        return Instant.parse("2026-01-01T00:00:00Z").plusSeconds(minute * 60);
    }

    @SuppressWarnings("unchecked")
    private void stubBrowseSources(List<Comment> comments, List<CommentReply> replies) {
        when(commentRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(comments, inv.getArgument(1), comments.size()));
        when(replyRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(replies, inv.getArgument(1), replies.size()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void browseAll_mergesByCreatedAtAndSlicesExactPage() {
        loginAs(ADMIN_ID, true);
        Comment c1 = comment(1L, CommentTargetType.LESSON, LESSON_ID);
        c1.setCreatedAt(at(10));
        Comment c2 = comment(2L, CommentTargetType.LESSON, LESSON_ID);
        c2.setCreatedAt(at(7));
        CommentReply r1 = CommentReply.builder().comment(lessonComment).user(user(AUTHOR_ID)).content("r1").build();
        r1.setId(11L);
        r1.setCreatedAt(at(9));
        CommentReply r2 = CommentReply.builder().comment(lessonComment).user(user(AUTHOR_ID)).content("r2").build();
        r2.setId(12L);
        r2.setCreatedAt(at(6));
        stubBrowseSources(List.of(c1, c2), List.of(r1, r2));

        // Thứ tự gộp: c1(10) → r1(9) → c2(7) → r2(6); trang 2, size 2 → [c2, r2]
        PageResponse<ModerationItemResponse> page2 = service.browse(null, null, null, null, 2, 2);

        assertThat(page2.getItems()).extracting(ModerationItemResponse::getKind, ModerationItemResponse::getId)
                .containsExactly(tuple(ModerationItemResponse.Kind.COMMENT, 2L),
                        tuple(ModerationItemResponse.Kind.REPLY, 12L));
        assertThat(page2.getTotalElements()).isEqualTo(4);
        assertThat(page2.getTotalPages()).isEqualTo(2);

        // Mỗi bảng chỉ cần đọc offset + size = 4 dòng đầu
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(commentRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(4);
    }

    @Test
    void browseAll_tooDeep_returnsEmptyItemsButKeepsTotals() {
        loginAs(ADMIN_ID, true);
        stubBrowseSources(List.of(lessonComment), List.of());

        int deepPage = CommentModerationService.MAX_BROWSE_ALL_WINDOW; // offset vượt giới hạn
        PageResponse<ModerationItemResponse> res = service.browse(null, null, null, null, deepPage, 10);

        assertThat(res.getItems()).isEmpty();
        assertThat(res.getTotalElements()).isEqualTo(1);
    }
}
