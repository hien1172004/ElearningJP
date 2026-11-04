package com.jp.elearningjp.service.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.service.notification.listener.CommentModeratedNotificationListener;
import com.jp.elearningjp.shared.enums.CommentTargetType;
import com.jp.elearningjp.shared.enums.NotificationType;
import com.jp.elearningjp.shared.event.comment.CommentModeratedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CommentModeratedNotificationListenerTest {

    private NotificationService notificationService;
    private CommentModeratedNotificationListener listener;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        listener = new CommentModeratedNotificationListener(notificationService, new ObjectMapper());
    }

    private static CommentModeratedEvent event(Long recipient, Long moderator, String note) {
        return CommentModeratedEvent.builder()
                .recipientUserId(recipient).moderatorUserId(moderator)
                .action(CommentModeratedEvent.Action.HIDDEN).scope(CommentModeratedEvent.Scope.REPLY)
                .contentPreview("x".repeat(100)).note(note)
                .targetType(CommentTargetType.LESSON).targetId(20L)
                .build();
    }

    @Test
    void notifiesAuthor_withoutCommentIdInData() {
        listener.onCommentModerated(event(5L, 1L, "Spam"));

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<JsonNode> data = ArgumentCaptor.forClass(JsonNode.class);
        verify(notificationService).create(eq(5L), eq(NotificationType.COMMENT_MODERATED),
                title.capture(), body.capture(), data.capture());

        assertThat(title.getValue()).isEqualTo("Trả lời của bạn đã bị ẩn do vi phạm quy định cộng đồng");
        assertThat(body.getValue()).endsWith("— Lý do: Spam").contains("…");
        // Không có commentId: listener dọn thông báo khi xóa bình luận sẽ không xóa nhầm thông báo này
        assertThat(data.getValue().has("commentId")).isFalse();
        assertThat(data.getValue().path("targetId").asLong()).isEqualTo(20L);
    }

    @Test
    void skips_whenModeratorIsAuthor_orAuthorMissing() {
        listener.onCommentModerated(event(1L, 1L, null));
        listener.onCommentModerated(event(null, 1L, null));

        verifyNoInteractions(notificationService);
    }

    @Test
    void notificationFailure_doesNotPropagate() {
        when(notificationService.create(anyLong(), any(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("db down"));

        listener.onCommentModerated(event(5L, 1L, null));
    }
}
