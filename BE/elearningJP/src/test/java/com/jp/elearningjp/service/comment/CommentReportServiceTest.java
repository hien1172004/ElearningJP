package com.jp.elearningjp.service.comment;

import com.jp.elearningjp.dto.request.comment.CommentReportRequest;
import com.jp.elearningjp.entity.comment.Comment;
import com.jp.elearningjp.entity.comment.CommentReply;
import com.jp.elearningjp.entity.comment.CommentReport;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.comment.CommentReplyRepository;
import com.jp.elearningjp.repository.comment.CommentReportRepository;
import com.jp.elearningjp.repository.comment.CommentRepository;
import com.jp.elearningjp.shared.enums.ReportReason;
import com.jp.elearningjp.shared.enums.ReportStatus;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommentReportServiceTest {

    private static final Long REPORTER_ID = 1L;
    private static final Long AUTHOR_ID = 2L;

    private CommentRepository commentRepository;
    private CommentReplyRepository replyRepository;
    private CommentReportRepository reportRepository;
    private CommentReportService service;
    private Comment comment;

    private static User user(Long id) {
        User u = User.builder().email("u" + id + "@t.local").build();
        u.setId(id);
        return u;
    }

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        replyRepository = mock(CommentReplyRepository.class);
        reportRepository = mock(CommentReportRepository.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getCurrentUser()).thenReturn(user(REPORTER_ID));
        service = new CommentReportService(commentRepository, replyRepository, reportRepository, currentUserService);

        comment = Comment.builder().user(user(AUTHOR_ID)).content("x").build();
        comment.setId(100L);
        when(commentRepository.findVisibleById(100L)).thenReturn(Optional.of(comment));
    }

    private static CommentReportRequest request(String detail) {
        return CommentReportRequest.builder().reason(ReportReason.SPAM).detail(detail).build();
    }

    @Test
    void reportComment_savesOpenReportWithTrimmedDetail() {
        service.reportComment(100L, request("  quảng cáo  "));

        ArgumentCaptor<CommentReport> saved = ArgumentCaptor.forClass(CommentReport.class);
        verify(reportRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getComment()).isSameAs(comment);
        assertThat(saved.getValue().getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(saved.getValue().getDetail()).isEqualTo("quảng cáo");
        assertThat(saved.getValue().getStatus()).isEqualTo(ReportStatus.OPEN);
    }

    @Test
    void reportComment_blankDetail_storedAsNull() {
        service.reportComment(100L, request("   "));

        ArgumentCaptor<CommentReport> saved = ArgumentCaptor.forClass(CommentReport.class);
        verify(reportRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getDetail()).isNull();
    }

    @Test
    void reportOwnComment_rejected() {
        comment.setUser(user(REPORTER_ID));

        assertThatThrownBy(() -> service.reportComment(100L, request(null)))
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.COMMENT_REPORT_OWN);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateReport_rejected_evenOnConcurrentInsert() {
        when(reportRepository.existsByReporterIdAndCommentId(REPORTER_ID, 100L)).thenReturn(true);
        assertThatThrownBy(() -> service.reportComment(100L, request(null)))
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.COMMENT_ALREADY_REPORTED);

        // Hai request cùng lúc: kiểm tra exists đều qua, unique index chặn ở DB
        when(reportRepository.existsByReporterIdAndCommentId(REPORTER_ID, 100L)).thenReturn(false);
        when(reportRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));
        assertThatThrownBy(() -> service.reportComment(100L, request(null)))
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.COMMENT_ALREADY_REPORTED);
    }

    @Test
    void reportHiddenOrMissingReply_notFound() {
        when(replyRepository.findVisibleById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportReply(7L, request(null)))
                .extracting(e -> ((AppException) e).getErrorCode()).isEqualTo(ErrorCode.COMMENT_REPLY_NOT_FOUND);
    }

    @Test
    void reportReply_savesReportOnReply() {
        CommentReply reply = CommentReply.builder().comment(comment).user(user(AUTHOR_ID)).content("r").build();
        reply.setId(7L);
        when(replyRepository.findVisibleById(7L)).thenReturn(Optional.of(reply));

        service.reportReply(7L, request(null));

        ArgumentCaptor<CommentReport> saved = ArgumentCaptor.forClass(CommentReport.class);
        verify(reportRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getReply()).isSameAs(reply);
        assertThat(saved.getValue().getComment()).isNull();
    }
}
