package com.jp.elearningjp.service.comment;

import com.jp.elearningjp.dto.request.comment.CommentCreateRequest;
import com.jp.elearningjp.dto.request.comment.CommentReplyCreateRequest;
import com.jp.elearningjp.dto.request.comment.CommentUpdateRequest;
import com.jp.elearningjp.dto.response.comment.CommentListResponse;
import com.jp.elearningjp.entity.comment.Comment;
import com.jp.elearningjp.entity.comment.CommentReply;
import com.jp.elearningjp.entity.content.Vocabulary;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.comment.CommentDislikeRepository;
import com.jp.elearningjp.repository.comment.CommentLikeRepository;
import com.jp.elearningjp.repository.comment.CommentReplyDislikeRepository;
import com.jp.elearningjp.repository.comment.CommentReplyLikeRepository;
import com.jp.elearningjp.repository.comment.CommentReplyRepository;
import com.jp.elearningjp.repository.comment.CommentRepository;
import com.jp.elearningjp.repository.content.GrammarPointRepository;
import com.jp.elearningjp.repository.content.VocabularyRepository;
import com.jp.elearningjp.repository.content.WritingCharacterRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.shared.enums.CommentTargetType;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tái hiện các lỗi của module bình luận. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommentServiceTest {

    private static final Long VIEWER_ID = 1L;
    private static final Long AUTHOR_ID = 2L;
    private static final Long COMMENT_ID = 100L;
    private static final Long LESSON_ID = 20L;

    @Mock CommentRepository commentRepository;
    @Mock CommentLikeRepository commentLikeRepository;
    @Mock CommentDislikeRepository commentDislikeRepository;
    @Mock CommentReplyRepository commentReplyRepository;
    @Mock CommentReplyLikeRepository commentReplyLikeRepository;
    @Mock CommentReplyDislikeRepository commentReplyDislikeRepository;
    @Mock LessonRepository lessonRepository;
    @Mock VocabularyRepository vocabularyRepository;
    @Mock WritingCharacterRepository writingCharacterRepository;
    @Mock GrammarPointRepository grammarPointRepository;
    @Mock CurrentUserService currentUserService;
    @Mock CourseAccessGuard courseAccessGuard;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks CommentService service;

    private User viewer;
    private User author;
    private Course course;
    private Comment comment;

    private static User user(Long id) {
        User u = User.builder().email("u" + id + "@test.local").build();
        u.setId(id);
        return u;
    }

    @BeforeEach
    void setUp() {
        viewer = user(VIEWER_ID);
        author = user(AUTHOR_ID);
        when(currentUserService.getCurrentUser()).thenReturn(viewer);
        when(currentUserService.getCurrentUserId()).thenReturn(VIEWER_ID);

        course = new Course();
        course.setId(10L);
        course.setPublished(true);
        Lesson lesson = Lesson.builder().course(course).build();
        lesson.setId(LESSON_ID);
        when(lessonRepository.findByIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.of(lesson));

        comment = Comment.builder()
                .targetType(CommentTargetType.LESSON).targetId(LESSON_ID)
                .user(author).content("x").replyCount(5).build();
        comment.setId(COMMENT_ID);
        when(commentRepository.findByIdAndDeletedFalse(COMMENT_ID)).thenReturn(Optional.of(comment));
        when(commentRepository.findVisibleById(COMMENT_ID)).thenReturn(Optional.of(comment));
    }

    private void assertRejected(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(expected);
    }

    // ---- quyền xoá

    @Test
    void deleteComment_adminCanRemoveSomeoneElsesComment() {
        when(currentUserService.isAdmin()).thenReturn(true);

        service.deleteComment(COMMENT_ID);

        assertThat(comment.isDeleted()).isTrue();
        verify(commentRepository).save(comment);
    }

    @Test
    void deleteComment_otherUser_forbidden() {
        assertRejected(() -> service.deleteComment(COMMENT_ID), ErrorCode.COMMENT_FORBIDDEN);
        assertThat(comment.isDeleted()).isFalse();
    }

    @Test
    void updateComment_adminCannotRewriteOthersComment() {
        when(currentUserService.isAdmin()).thenReturn(true);
        CommentUpdateRequest request = new CommentUpdateRequest();
        request.setContent("sửa hộ");

        assertRejected(() -> service.updateComment(COMMENT_ID, request), ErrorCode.COMMENT_FORBIDDEN);
    }

    @Test
    void deleteReply_adminRemovesWholeSubtreeAndDecrementsByDeletedCount() {
        when(currentUserService.isAdmin()).thenReturn(true);
        CommentReply reply = CommentReply.builder().comment(comment).user(author).content("r").build();
        reply.setId(7L);
        when(commentReplyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(reply));
        when(commentReplyRepository.softDeleteSubtree(eq(7L), any())).thenReturn(3); // reply + 2 con cháu

        service.deleteReply(7L);

        verify(commentRepository).decrementReplyCountBy(COMMENT_ID, 3);
        verify(commentRepository, never()).decrementReplyCount(anyLong());
    }

    @Test
    void deleteComment_publishesCleanupEventForItsNotifications() {
        when(currentUserService.getCurrentUser()).thenReturn(author);

        service.deleteComment(COMMENT_ID);

        ArgumentCaptor<com.jp.elearningjp.shared.event.comment.CommentDeletedEvent> event =
                ArgumentCaptor.forClass(com.jp.elearningjp.shared.event.comment.CommentDeletedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().getCommentId()).isEqualTo(COMMENT_ID);
        assertThat(event.getValue().isWholeComment()).isTrue();
    }

    @Test
    void deleteReply_publishesCleanupEventWithWholeSubtree() {
        when(currentUserService.getCurrentUser()).thenReturn(author);
        CommentReply reply = CommentReply.builder().comment(comment).user(author).content("r").build();
        reply.setId(7L);
        when(commentReplyRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(reply));
        when(commentReplyRepository.findActiveSubtreeIds(7L)).thenReturn(List.of(7L, 8L));
        when(commentReplyRepository.softDeleteSubtree(eq(7L), any())).thenReturn(2);

        service.deleteReply(7L);

        ArgumentCaptor<com.jp.elearningjp.shared.event.comment.CommentDeletedEvent> event =
                ArgumentCaptor.forClass(com.jp.elearningjp.shared.event.comment.CommentDeletedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().getReplyIds()).containsExactly(7L, 8L);
    }

    @Test
    void addReply_nested_eventCarriesParentReplyAuthor() {
        User parentAuthor = user(3L);
        CommentReply parent = CommentReply.builder().comment(comment).user(parentAuthor).content("p").build();
        parent.setId(50L);
        when(commentReplyRepository.findVisibleById(50L)).thenReturn(Optional.of(parent));
        when(commentReplyRepository.save(any(CommentReply.class))).thenAnswer(i -> i.getArgument(0));
        CommentReplyCreateRequest request = new CommentReplyCreateRequest();
        request.setContent("trả lời lồng");
        request.setParentReplyId(50L);

        service.addReply(COMMENT_ID, request);

        ArgumentCaptor<com.jp.elearningjp.shared.event.comment.CommentRepliedEvent> event =
                ArgumentCaptor.forClass(com.jp.elearningjp.shared.event.comment.CommentRepliedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().getRecipientUserId()).isEqualTo(AUTHOR_ID);
        assertThat(event.getValue().getParentReplyUserId()).isEqualTo(3L);
    }

    // ---- bộ đếm

    @Test
    void addReply_onlyIncrementsInDb_doesNotDirtyEntityCounter() {
        when(commentReplyRepository.save(any(CommentReply.class))).thenAnswer(i -> i.getArgument(0));
        CommentReplyCreateRequest request = new CommentReplyCreateRequest();
        request.setContent("trả lời");

        service.addReply(COMMENT_ID, request);

        verify(commentRepository).incrementReplyCount(COMMENT_ID);
        // Không set trên entity → khi flush không ghi đè reply_count bằng giá trị cũ + 1
        assertThat(comment.getReplyCount()).isEqualTo(5);
    }

    @Test
    void toggleLike_duplicateConcurrentInsert_noDoubleCountNoError() {
        // Request kia vừa like trước: không xoá được gì, INSERT ... ON CONFLICT bỏ qua.
        when(commentLikeRepository.deleteReaction(VIEWER_ID, COMMENT_ID)).thenReturn(0);
        when(commentDislikeRepository.deleteReaction(VIEWER_ID, COMMENT_ID)).thenReturn(0);
        when(commentLikeRepository.insertIfAbsent(VIEWER_ID, COMMENT_ID)).thenReturn(0);

        assertThat(service.toggleLike(COMMENT_ID).isLiked()).isTrue();

        verify(commentRepository, never()).incrementLikeCount(anyLong());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void toggleLike_firstLike_countsOnce() {
        when(commentLikeRepository.insertIfAbsent(VIEWER_ID, COMMENT_ID)).thenReturn(1);

        service.toggleLike(COMMENT_ID);

        verify(commentRepository, times(1)).incrementLikeCount(COMMENT_ID);
    }

    // ---- target

    @Test
    void createComment_onDeletedLesson_rejected() {
        when(lessonRepository.findByIdAndDeletedFalse(LESSON_ID)).thenReturn(Optional.empty());
        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent("hi");

        assertRejected(() -> service.createComment(CommentTargetType.LESSON, LESSON_ID, request),
                ErrorCode.COMMENT_INVALID_TARGET);
    }

    @Test
    void comments_ofUnpublishedCourse_hiddenFromStranger_visibleToEnrolled() {
        course.setPublished(false);
        when(commentRepository.findByTarget(any(), anyLong(), any())).thenReturn(new PageImpl<>(List.of()));

        assertRejected(() -> service.getCommentsByTarget(CommentTargetType.LESSON, LESSON_ID, 1, 5),
                ErrorCode.COMMENT_INVALID_TARGET);
        // Đọc chi tiết theo id cũng bị chặn, không đi vòng qua được
        assertRejected(() -> service.getCommentDetail(COMMENT_ID), ErrorCode.COMMENT_INVALID_TARGET);

        when(courseAccessGuard.canViewUnpublished(course, VIEWER_ID)).thenReturn(true);
        assertThat(service.getCommentsByTarget(CommentTargetType.LESSON, LESSON_ID, 1, 5)).isNotNull();
    }

    // ---- phân trang & hiệu năng

    @Test
    void getComments_pageStartsAtOne_andReactionFlagsLoadedInBatch() {
        Comment second = Comment.builder().targetType(CommentTargetType.LESSON).targetId(LESSON_ID)
                .user(viewer).content("y").build();
        second.setId(101L);
        when(commentRepository.findByTarget(any(), anyLong(), any()))
                .thenAnswer(i -> new PageImpl<>(List.of(comment, second), i.getArgument(2, Pageable.class), 12));
        when(commentLikeRepository.findReactedIds(eq(VIEWER_ID), anyList())).thenReturn(List.of(101L));
        when(commentDislikeRepository.findReactedIds(eq(VIEWER_ID), anyList())).thenReturn(List.of());

        CommentListResponse response = service.getCommentsByTarget(CommentTargetType.LESSON, LESSON_ID, 2, 5);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(commentRepository).findByTarget(any(), anyLong(), pageable.capture());
        assertThat(pageable.getValue()).isEqualTo(PageRequest.of(1, 5)); // trang 2 → offset index 1
        assertThat(response.getPage()).isEqualTo(2);

        assertThat(response.getItems()).extracting(c -> c.getLiked()).containsExactly(false, true);
        assertThat(response.getItems()).extracting(c -> c.getDeletable()).containsExactly(false, true);
        verify(commentLikeRepository, times(1)).findReactedIds(eq(VIEWER_ID), anyList());
    }

    // ---- vị trí comment (mở thẳng từ thông báo)

    @Test
    void getCommentLocation_lesson_returnsCourseAndPositionAmongNewerComments() {
        when(commentRepository.countVisibleNewerThan(CommentTargetType.LESSON, LESSON_ID, comment.getCreatedAt()))
                .thenReturn(7L);

        var location = service.getCommentLocation(COMMENT_ID, null);

        assertThat(location.getTargetType()).isEqualTo(CommentTargetType.LESSON);
        assertThat(location.getTargetId()).isEqualTo(LESSON_ID);
        assertThat(location.getCourseId()).isEqualTo(10L);
        assertThat(location.getKeyword()).isNull();
        assertThat(location.getPosition()).isEqualTo(7L);
        assertThat(location.getReplyId()).isNull();
    }

    @Test
    void getCommentLocation_vocabulary_returnsWordAsKeyword() {
        Vocabulary vocab = Vocabulary.builder().word("食べる").build();
        vocab.setId(30L);
        when(vocabularyRepository.findByIdAndDeletedFalse(30L)).thenReturn(Optional.of(vocab));
        comment.setTargetType(CommentTargetType.VOCABULARY);
        comment.setTargetId(30L);

        var location = service.getCommentLocation(COMMENT_ID, null);

        assertThat(location.getKeyword()).isEqualTo("食べる");
        assertThat(location.getCourseId()).isNull();
    }

    @Test
    void getCommentLocation_replyOfAnotherComment_isDropped() {
        Comment other = Comment.builder()
                .targetType(CommentTargetType.LESSON).targetId(LESSON_ID).user(author).content("o").build();
        other.setId(999L);
        CommentReply foreign = CommentReply.builder().comment(other).user(author).content("r").build();
        foreign.setId(7L);
        when(commentReplyRepository.findVisibleById(7L)).thenReturn(Optional.of(foreign));

        assertThat(service.getCommentLocation(COMMENT_ID, 7L).getReplyId()).isNull();
    }

    @Test
    void getCommentLocation_replyOfThisComment_isKept() {
        CommentReply reply = CommentReply.builder().comment(comment).user(author).content("r").build();
        reply.setId(8L);
        when(commentReplyRepository.findVisibleById(8L)).thenReturn(Optional.of(reply));

        assertThat(service.getCommentLocation(COMMENT_ID, 8L).getReplyId()).isEqualTo(8L);
    }

    @Test
    void getCommentLocation_hiddenOrDeletedComment_notFound() {
        when(commentRepository.findVisibleById(COMMENT_ID)).thenReturn(Optional.empty());

        assertRejected(() -> service.getCommentLocation(COMMENT_ID, null), ErrorCode.COMMENT_NOT_FOUND);
    }
}
