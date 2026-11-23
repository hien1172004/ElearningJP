package com.jp.elearningjp.service.exam;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jp.elearningjp.dto.response.exam.LeaderboardItemResponse;
import com.jp.elearningjp.dto.response.exam.LeaderboardResponse;
import com.jp.elearningjp.entity.exam.Exam;
import com.jp.elearningjp.entity.exam.ExamAttempt;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.repository.exam.ExamAttemptRepository;
import com.jp.elearningjp.service.exam.impl.ExamServiceImpl;
import com.jp.elearningjp.shared.enums.AttemptStatus;
import com.jp.elearningjp.shared.enums.JlptLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Cache bảng xếp hạng thi thử: hit không chạm DB, cờ "là tôi" tính riêng mỗi request, JSON đọc lại được. */
@ExtendWith(MockitoExtension.class)
class ExamLeaderboardCacheTest {

    @Mock ExamAttemptRepository examAttemptRepository;
    @Mock ExamLeaderboardCacheService leaderboardCache;

    @InjectMocks ExamServiceImpl examService;

    private static LeaderboardItemResponse item(int rank, long userId, int score) {
        return LeaderboardItemResponse.builder().rank(rank).userId(userId).fullName("U" + userId)
                .score(score).jlptLevel(JlptLevel.N3).submittedAt(Instant.parse("2026-11-20T10:00:00Z")).build();
    }

    private static ExamAttempt attempt(long userId, int score) {
        User u = User.builder().fullName("U" + userId).build();
        u.setId(userId);
        return ExamAttempt.builder().user(u).exam(Exam.builder().title("Đề 1").build())
                .totalScaledScore(score).timeSpentSeconds(600).build();
    }

    @Test
    void cacheHit_doesNotQueryDb_andMarksOnlyCurrentViewer() {
        when(leaderboardCache.get(JlptLevel.N3, "WEEK"))
                .thenReturn(new ArrayList<>(List.of(item(1, 9, 150), item(2, 5, 120))));

        LeaderboardResponse res = examService.getLeaderboard(JlptLevel.N3, "week", 5L);

        verifyNoInteractions(examAttemptRepository);
        verify(leaderboardCache, never()).put(any(), anyString(), any());
        assertThat(res.getMyRank().getUserId()).isEqualTo(5L);
        assertThat(res.getRankings()).extracting(LeaderboardItemResponse::isCurrentUser).containsExactly(false, true);
        assertThat(res.getTotalParticipants()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void cacheMiss_buildsBestAttemptPerUser_andStoresUnderNormalizedPeriod() {
        when(leaderboardCache.get(JlptLevel.N5, "ALL")).thenReturn(null); // Mockito mặc định trả list rỗng
        // Repo trả theo điểm giảm dần → bài đầu tiên của mỗi user là bài cao nhất
        when(examAttemptRepository.findLeaderboardAttemptsAllTime(AttemptStatus.SUBMITTED, JlptLevel.N5))
                .thenReturn(List.of(attempt(9, 160), attempt(5, 140), attempt(9, 100)));

        LeaderboardResponse res = examService.getLeaderboard(null, null, null); // mặc định N5, ALL, khách

        ArgumentCaptor<List<LeaderboardItemResponse>> stored = ArgumentCaptor.forClass(List.class);
        verify(leaderboardCache).put(eq(JlptLevel.N5), eq("ALL"), stored.capture());
        assertThat(stored.getValue()).extracting(LeaderboardItemResponse::getUserId).containsExactly(9L, 5L);
        assertThat(stored.getValue()).extracting(LeaderboardItemResponse::getRank).containsExactly(1, 2);
        assertThat(res.getMyRank()).isNull();
    }

    @Test
    void json_usesIsCurrentUserName_expectedByFe() throws Exception {
        LeaderboardItemResponse me = item(1, 5, 150);
        me.setCurrentUser(true);
        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(me);

        assertThat(json).contains("\"isCurrentUser\":true").doesNotContain("\"currentUser\"");
    }

    @Test
    void cacheService_jsonRoundTrip_keepsItems() {
        StringRedisTemplate redis = org.mockito.Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = org.mockito.Mockito.mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        ExamLeaderboardCacheService cache =
                new ExamLeaderboardCacheService(redis, new ObjectMapper().registerModule(new JavaTimeModule()));

        cache.put(JlptLevel.N3, "WEEK", List.of(item(1, 9, 150)));
        ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        verify(ops).set(eq("exam-leaderboard:N3:WEEK"), json.capture(), eq(Duration.ofMinutes(5)));

        when(ops.get("exam-leaderboard:N3:WEEK")).thenReturn(json.getValue());
        assertThat(cache.get(JlptLevel.N3, "WEEK")).usingRecursiveFieldByFieldElementComparator()
                .containsExactly(item(1, 9, 150));
    }
}
