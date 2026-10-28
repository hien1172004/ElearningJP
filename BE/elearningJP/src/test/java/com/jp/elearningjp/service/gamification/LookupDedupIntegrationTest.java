package com.jp.elearningjp.service.gamification;

import com.jp.elearningjp.entity.gamification.UserDailyStats;
import com.jp.elearningjp.repository.gamification.UserDailyStatsRepository;
import com.jp.elearningjp.repository.gamification.UserLearnedItemRepository;
import com.jp.elearningjp.shared.enums.LookupTargetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test cho luồng chống trùng "từ đã học".
 *
 * <p><b>Mục tiêu:</b> chứng minh 2 khái niệm được tách bạch:</p>
 * <ul>
 *   <li>{@code lookups} — TỔNG số lần tra (kể cả tra lại từ cũ) → luôn tăng</li>
 *   <li>{@code new_items_learned} — số MỤC MỚI (mỗi mục chỉ tính 1 lần)</li>
 * </ul>
 *
 * <p>Chạy trên PostgreSQL thật (Testcontainers) vì cơ chế chống trùng dựa vào
 * {@code UNIQUE constraint} + {@code ON CONFLICT DO NOTHING} — H2/HSQLDB không
 * hỗ trợ nên test sẽ cho kết quả sai.</p>
 *
 * <p>Datasource bị ghi đè bằng {@link DynamicPropertySource} (nên ưu tiên cao hơn
 * {@code application-dev.yaml}), còn lại cấu hình lấy từ {@code .env} (JWT key,
 * Redis) — cần thiết để context của app khởi động được.</p>
 *
 * <p><b>Vì sao KHÔNG dùng {@code @Transactional}?</b> {@code DailyStatsService}
 * chạy {@code REQUIRES_NEW} → transaction con KHÔNG thấy row user mới insert
 * trong transaction test (chưa commit) → vi phạm FK. Nên test chạy ở chế độ
 * không rollback, mỗi test tự tạo user riêng (email unique).</p>
 */
@Testcontainers
@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.hibernate.show-sql=false",
        "spring.data.redis.repositories.enabled=false",
})
class LookupDedupIntegrationTest {

    // ------------------------------------------------------------------
    // Testcontainers
    // ------------------------------------------------------------------

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("elearning_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    // ------------------------------------------------------------------
    // Beans
    // ------------------------------------------------------------------

    @Autowired LearnedItemService learnedItemService;
    @Autowired DailyStatsService dailyStatsService;
    @Autowired UserDailyStatsRepository dailyStatsRepository;
    @Autowired UserLearnedItemRepository learnedItemRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    private Long userId;

    @BeforeEach
    void setUp() {
        // Tạo user trực tiếp bằng JDBC (bỏ qua JPA entity để không cần role/soft-delete).
        String email = "dedup-" + System.nanoTime() + "@test.local";
        jdbcTemplate.update(
                "INSERT INTO users (email, password_hash, full_name, auth_provider, status, deleted, version) "
                        + "VALUES (?, ?, ?, 'LOCAL', 'ACTIVE', FALSE, 0)",
                email, "hash", "Dedup User");
        userId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Ghi 1 lần tra qua đúng luồng production: register → daily stats. */
    private void recordLookup(LookupTargetType type, Long targetId, String sourceRef) {
        int isNew = learnedItemService.register(userId, type, targetId, sourceRef) ? 1 : 0;
        dailyStatsService.recordLookup(userId, 2, isNew);
    }

    private UserDailyStats today() {
        return dailyStatsRepository
                .findByIdUserIdAndIdStatDate(userId, LocalDate.now())
                .orElse(null);
    }

    // ================================================================
    // Tra cùng 1 từ nhiều lần
    // ================================================================

    @Test
    @DisplayName("Tra cùng 1 từ 3 lần → lookups = 3, new_items_learned = 1")
    void sameWordThreeTimes_lookupsThree_newItemsOne() {
        recordLookup(LookupTargetType.VOCAB, 100L, null);
        recordLookup(LookupTargetType.VOCAB, 100L, null);
        recordLookup(LookupTargetType.VOCAB, 100L, null);

        UserDailyStats stats = today();
        assertThat(stats).isNotNull();
        assertThat(stats.getLookups()).isEqualTo(3);
        assertThat(stats.getNewItemsLearned()).isEqualTo(1);

        // Chỉ 1 row trong bảng chống trùng
        assertThat(learnedItemRepository.countByUserId(userId)).isEqualTo(1);
    }

    @Test
    @DisplayName("Tra 3 từ khác nhau → lookups = 3, new_items_learned = 3")
    void threeDifferentWords_allNew() {
        recordLookup(LookupTargetType.VOCAB, 100L, null);
        recordLookup(LookupTargetType.VOCAB, 200L, null);
        recordLookup(LookupTargetType.VOCAB, 300L, null);

        UserDailyStats stats = today();
        assertThat(stats.getLookups()).isEqualTo(3);
        assertThat(stats.getNewItemsLearned()).isEqualTo(3);
        assertThat(learnedItemRepository.countByUserId(userId)).isEqualTo(3);
    }

    // ================================================================
    // 3 loại đối tượng
    // ================================================================

    @Test
    @DisplayName("Tra 3 loại khác nhau (VOCAB/KANJI/GRAMMAR) → mỗi loại +1 từ mới")
    void threeTargetTypes_eachCountsOnce() {
        recordLookup(LookupTargetType.VOCAB, 100L, null);
        recordLookup(LookupTargetType.KANJI, null, "日");
        recordLookup(LookupTargetType.GRAMMAR, 7L, null);

        UserDailyStats stats = today();
        assertThat(stats.getLookups()).isEqualTo(3);
        assertThat(stats.getNewItemsLearned()).isEqualTo(3);
        assertThat(learnedItemRepository.countByUserId(userId)).isEqualTo(3);

        // Tra lại từng loại lần nữa → không tăng từ mới
        recordLookup(LookupTargetType.VOCAB, 100L, null);
        recordLookup(LookupTargetType.KANJI, null, "日");
        recordLookup(LookupTargetType.GRAMMAR, 7L, null);

        UserDailyStats after = today();
        assertThat(after.getLookups()).isEqualTo(6);
        assertThat(after.getNewItemsLearned()).isEqualTo(3);
    }

    @Test
    @DisplayName("KANJI: cùng 1 ký tự tra 2 lần → chỉ +1 từ mới (khoá theo 'ref:')")
    void kanjiSameChar_dedupesByRef() {
        recordLookup(LookupTargetType.KANJI, null, "日");
        recordLookup(LookupTargetType.KANJI, null, "日");

        UserDailyStats stats = today();
        assertThat(stats.getLookups()).isEqualTo(2);
        assertThat(stats.getNewItemsLearned()).isEqualTo(1);
    }

    @Test
    @DisplayName("VOCAB id 100 và GRAMMAR id 100 là 2 mục khác nhau (khoá 'id:' chung nhưng tách theo loại)")
    void sameIdDifferentTypes_areDistinctItems() {
        recordLookup(LookupTargetType.VOCAB, 100L, null);
        recordLookup(LookupTargetType.GRAMMAR, 100L, null);

        UserDailyStats stats = today();
        assertThat(stats.getLookups()).isEqualTo(2);
        assertThat(stats.getNewItemsLearned()).isEqualTo(2);
    }

    // ================================================================
    // Tính chất atomic của UNIQUE constraint
    // ================================================================

    @Test
    @DisplayName("register() trả false khi mục đã tồn tại → không tăng new_items_learned")
    void registerReturnsFalseOnConflict() {
        assertThat(learnedItemService.register(userId, LookupTargetType.VOCAB, 100L, null)).isTrue();
        assertThat(learnedItemService.register(userId, LookupTargetType.VOCAB, 100L, null)).isFalse();
        assertThat(learnedItemService.register(userId, LookupTargetType.VOCAB, 100L, null)).isFalse();
    }

    @Test
    @DisplayName("2 user khác nhau tra cùng 1 từ → mỗi user đều tính là mới")
    void differentUsers_eachCountAsNew() {
        // User thứ 2
        String email2 = "dedup2-" + System.nanoTime() + "@test.local";
        jdbcTemplate.update(
                "INSERT INTO users (email, password_hash, full_name, auth_provider, status, deleted, version) "
                        + "VALUES (?, ?, ?, 'LOCAL', 'ACTIVE', FALSE, 0)",
                email2, "hash", "Dedup User 2");
        Long otherUserId = jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", Long.class, email2);

        assertThat(learnedItemService.register(userId, LookupTargetType.VOCAB, 100L, null)).isTrue();
        assertThat(learnedItemService.register(otherUserId, LookupTargetType.VOCAB, 100L, null)).isTrue();
    }
}
