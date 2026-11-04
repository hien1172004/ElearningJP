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
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Người dùng báo cáo vi phạm bình luận / trả lời. Mỗi người chỉ báo cáo một nội dung
 * một lần (unique index ở DB) và không tự báo cáo nội dung của mình.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentReportService {

    CommentRepository commentRepository;
    CommentReplyRepository commentReplyRepository;
    CommentReportRepository commentReportRepository;
    CurrentUserService currentUserService;

    @Transactional
    public void reportComment(Long commentId, CommentReportRequest request) {
        Comment comment = commentRepository.findVisibleById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        User reporter = currentUserService.getCurrentUser();
        ensureNotOwn(comment.getUser(), reporter);
        if (commentReportRepository.existsByReporterIdAndCommentId(reporter.getId(), commentId)) {
            throw new AppException(ErrorCode.COMMENT_ALREADY_REPORTED);
        }
        save(CommentReport.builder().reporter(reporter).comment(comment), request);
        log.info("[COMMENT] User {} reported comment {} ({})", reporter.getId(), commentId, request.getReason());
    }

    @Transactional
    public void reportReply(Long replyId, CommentReportRequest request) {
        CommentReply reply = commentReplyRepository.findVisibleById(replyId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_REPLY_NOT_FOUND));
        User reporter = currentUserService.getCurrentUser();
        ensureNotOwn(reply.getUser(), reporter);
        if (commentReportRepository.existsByReporterIdAndReplyId(reporter.getId(), replyId)) {
            throw new AppException(ErrorCode.COMMENT_ALREADY_REPORTED);
        }
        save(CommentReport.builder().reporter(reporter).reply(reply), request);
        log.info("[COMMENT] User {} reported reply {} ({})", reporter.getId(), replyId, request.getReason());
    }

    private static void ensureNotOwn(User author, User reporter) {
        if (author != null && Objects.equals(author.getId(), reporter.getId())) {
            throw new AppException(ErrorCode.COMMENT_REPORT_OWN);
        }
    }

    private void save(CommentReport.CommentReportBuilder builder, CommentReportRequest request) {
        String detail = request.getDetail() == null || request.getDetail().isBlank()
                ? null : request.getDetail().trim();
        try {
            // saveAndFlush: bấm báo cáo 2 lần cùng lúc → unique index báo lỗi ngay tại đây
            commentReportRepository.saveAndFlush(builder.reason(request.getReason()).detail(detail).build());
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.COMMENT_ALREADY_REPORTED);
        }
    }
}
