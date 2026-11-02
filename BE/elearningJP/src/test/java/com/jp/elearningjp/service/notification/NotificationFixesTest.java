package com.jp.elearningjp.service.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jp.elearningjp.dto.response.notification.NotificationResponse;
import com.jp.elearningjp.repository.gamification.NotificationRepository;
import com.jp.elearningjp.repository.gamification.UserDailyStatsRepository;
import com.jp.elearningjp.repository.gamification.UserGamificationRepository;
import com.jp.elearningjp.repository.srs.SrsItemRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.repository.user.UserSettingsRepository;
import com.jp.elearningjp.service.gamification.StreakService;
import com.jp.elearningjp.service.notification.listener.CommentReactionNotificationListener;
import com.jp.elearningjp.service.notification.listener.CommentReplyNotificationListener;
import com.jp.elearningjp.shared.enums.CommentTargetType;
import com.jp.elearningjp.shared.enums.NotificationType;
import com.jp.elearningjp.shared.event.comment.CommentReactionEvent;
import com.jp.elearningjp.shared.event.comment.CommentRepliedEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tái hiện các lỗi của module thông báo. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationFixesTest {

    private static final Long OWNER = 1L;
    private static final Long ACTOR = 2L;
    private static final Long PARENT_AUTHOR = 3L;

    @Mock NotificationService notificationService;
    @Mock NotificationRepository notificationRepository;
    @Mock UserSettingsRepository userSettingsRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ------------------------------------------------------------------ reaction

    @Nested
    class Reaction {

        private CommentReactionNotificationListener listener() {
            return new CommentReactionNotificationListener(
                    notificationService, notificationRepository, userSettingsRepository, objectMapper);
        }

        private CommentReactionEvent event(boolean like) {
            return CommentReactionEvent.builder()
                    .recipientUserId(OWNER).actorUserId(ACTOR).actorName("An")
                    .like(like).scope(CommentReactionEvent.ReactionScope.COMMENT)
                    .commentId(10L).targetType(CommentTargetType.LESSON).targetId(5L)
                    .build();
        }

        @Test
        void dislike_noNotification() {
            listener().onCommentReaction(event(false));
            verify(notificationService, never()).create(anyLong(), any(), anyString(), anyString(), any());
        }

        @Test
        void likeAgainAfterUnlike_noSecondNotification() {
            when(notificationRepository.existsCommunityLike(OWNER, ACTOR, 10L, -1L)).thenReturn(true);
            listener().onCommentReaction(event(true));
            verify(notificationService, never()).create(anyLong(), any(), anyString(), anyString(), any());
        }

        @Test
        void firstLike_notifiesAndRecordsActorForDedupe() {
            listener().onCommentReaction(event(true));

            ArgumentCaptor<JsonNode> data = ArgumentCaptor.forClass(JsonNode.class);
            verify(notificationService).create(eq(OWNER), eq(NotificationType.COMMUNITY_LIKE),
                    anyString(), anyString(), data.capture());
            assertThat(data.getValue().get("actorUserId").asLong()).isEqualTo(ACTOR);
        }
    }

    // ------------------------------------------------------------------ reply

    @Nested
    class Reply {

        private void onReply(Long commentOwner, Long parentAuthor, Long replier) {
            new CommentReplyNotificationListener(notificationService, userSettingsRepository, objectMapper)
                    .onCommentReplied(CommentRepliedEvent.builder()
                            .recipientUserId(commentOwner).parentReplyUserId(parentAuthor)
                            .replierUserId(replier).replierName("Bình")
                            .commentId(10L).replyId(99L).replyContent("ok").build());
        }

        @Test
        void nestedReply_notifiesCommentOwnerAndParentReplyAuthor() {
            onReply(OWNER, PARENT_AUTHOR, ACTOR);
            verify(notificationService).create(eq(OWNER), eq(NotificationType.COMMUNITY_REPLY), anyString(), anyString(), any());
            verify(notificationService).create(eq(PARENT_AUTHOR), eq(NotificationType.COMMUNITY_REPLY), anyString(), anyString(), any());
        }

        @Test
        void sameRecipientTwice_notifiedOnce_andNeverTheReplier() {
            onReply(OWNER, OWNER, ACTOR);   // chủ comment cũng là người viết reply cha
            verify(notificationService, times(1)).create(eq(OWNER), any(), anyString(), anyString(), any());

            onReply(OWNER, ACTOR, ACTOR);   // trả lời lại chính reply của mình
            verify(notificationService, never()).create(eq(ACTOR), any(), anyString(), anyString(), any());
        }
    }

    // ------------------------------------------------------------------ realtime nhiều instance

    @Nested
    class Realtime {

        @Mock StringRedisTemplate redisTemplate;
        @Mock NotificationLocalDelivery localDelivery;

        private final NotificationResponse payload = NotificationResponse.builder()
                .id(7L).title("t").body("b").type(NotificationType.COMMUNITY_LIKE).build();

        @Test
        void publish_goesThroughRedisChannel_andSubscriberDeliversLocally() throws Exception {
            new NotificationPublisher(redisTemplate, objectMapper, localDelivery).publishToUser(OWNER, payload);

            ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
            verify(redisTemplate).convertAndSend(eq(NotificationPublisher.CHANNEL), message.capture());
            verify(localDelivery, never()).deliver(anyLong(), any()); // không giao trực tiếp — chờ Redis

            // Mọi instance (kể cả instance phát) nhận gói tin và giao cho phiên của mình
            new NotificationRedisSubscriber(objectMapper, localDelivery).onMessage(
                    new DefaultMessage(NotificationPublisher.CHANNEL.getBytes(StandardCharsets.UTF_8),
                            message.getValue().getBytes(StandardCharsets.UTF_8)), null);
            ArgumentCaptor<Object> delivered = ArgumentCaptor.forClass(Object.class);
            verify(localDelivery).deliver(eq(OWNER), delivered.capture());
            assertThat(((JsonNode) delivered.getValue()).get("id").asLong()).isEqualTo(7L);
        }

        @Test
        void redisDown_fallsBackToLocalDelivery() {
            doThrow(new RuntimeException("Redis down")).when(redisTemplate).convertAndSend(anyString(), anyString());

            new NotificationPublisher(redisTemplate, objectMapper, localDelivery).publishToUser(OWNER, payload);

            verify(localDelivery).deliver(OWNER, payload);
        }
    }

    // ------------------------------------------------------------------ danh sách trong dropdown

    @Nested
    class Feed {

        @Mock UserRepository userRepository;
        @Mock NotificationPublisher publisher;

        @Test
        void getList_pageStartsAtOne() {
            NotificationService service = new NotificationService(notificationRepository, userRepository, publisher,
                    Clock.systemUTC());
            when(notificationRepository.findByUserId(eq(OWNER), any()))
                    .thenAnswer(i -> new org.springframework.data.domain.PageImpl<>(List.of(),
                            i.getArgument(1, org.springframework.data.domain.Pageable.class), 25));

            var response = service.getList(OWNER, 2, 10);

            ArgumentCaptor<org.springframework.data.domain.Pageable> pageable =
                    ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
            verify(notificationRepository).findByUserId(eq(OWNER), pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(1); // trang 2 → index 1
            assertThat(response.getPage()).isEqualTo(2);
            assertThat(response.getTotalPages()).isEqualTo(3);
        }
    }

    // ------------------------------------------------------------------ job nhắc nhở

    @Nested
    class Scheduler {

        @Mock UserGamificationRepository userGamificationRepository;
        @Mock UserDailyStatsRepository userDailyStatsRepository;
        @Mock SrsItemRepository srsItemRepository;
        @Mock StreakService streakService;
        @Mock UserRepository userRepository;

        @Test
        void srsReminder_skipsBannedOrInactiveUsers() {
            Clock clock = Clock.fixed(Instant.parse("2026-11-02T01:00:00Z"), ZoneOffset.UTC);
            NotificationScheduler scheduler = new NotificationScheduler(notificationService, notificationRepository,
                    userSettingsRepository, srsItemRepository, userGamificationRepository,
                    userDailyStatsRepository, streakService, userRepository, clock);
            when(srsItemRepository.countDueCardsGroupByUser(any()))
                    .thenReturn(List.of(new Object[]{5L, 3L}, new Object[]{6L, 2L}));
            when(userRepository.findIdsNotEligibleForNotifications()).thenReturn(List.of(6L));

            scheduler.sendSrsDueReminders();

            verify(notificationService).create(eq(5L), eq(NotificationType.SRS_DUE), anyString(), anyString(), any());
            verify(notificationService, never()).create(eq(6L), any(), anyString(), anyString(), any());
        }
    }
}
