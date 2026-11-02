package com.jp.elearningjp.repository.gamification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chạy SQL thật trên Postgres, qua toàn bộ migration: mọi đường tạo row trong ngày
 * đều phải chốt mục tiêu của user vào {@code goal_words}. Cần Docker.
 */
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserDailyStatsGoalSqlIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    private static final LocalDate DAY = LocalDate.of(2026, 11, 2);

    @Autowired UserDailyStatsRepository repository;
    @Autowired JdbcTemplate jdbc;

    private long userId;

    @BeforeEach
    void createUser() {
        userId = jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, full_name) VALUES (?, 'x', 'Goal Test') RETURNING id",
                Long.class, "goal-" + System.nanoTime() + "@test.local");
    }

    private void chooseWeeklyGoal(Integer weekly) {
        jdbc.update("INSERT INTO user_profiles (user_id, weekly_goal_words) VALUES (?, ?)", userId, weekly);
    }

    private int goalWords() {
        return jdbc.queryForObject(
                "SELECT goal_words FROM user_daily_stats WHERE user_id = ? AND stat_date = ?",
                Integer.class, userId, DAY);
    }

    @Test
    void lookup_usesUserGoal() {
        chooseWeeklyGoal(50);
        repository.upsertLookup(userId, DAY, 2, 100, 300, Instant.now(), 1);
        assertThat(goalWords()).isEqualTo(8);
    }

    @Test
    void studySeconds_usesUserGoal() {
        chooseWeeklyGoal(60);
        repository.addStudySeconds(userId, DAY, 30, 0, 300);
        assertThat(goalWords()).isEqualTo(9);
    }

    @Test
    void lessonCompleted_usesUserGoal() {
        chooseWeeklyGoal(140);
        repository.incrementLessonsCompleted(userId, DAY, 10, 300);
        assertThat(goalWords()).isEqualTo(20);
    }

    @Test
    void addXp_usesUserGoal() {
        chooseWeeklyGoal(40);
        repository.addXp(userId, DAY, 2);
        assertThat(goalWords()).isEqualTo(6);
    }

    @Test
    void noGoalChosen_orNoProfile_defaultsToFive() {
        repository.addXp(userId, DAY, 2);
        assertThat(goalWords()).isEqualTo(5);

        createUser();
        chooseWeeklyGoal(null);
        repository.addXp(userId, DAY, 2);
        assertThat(goalWords()).isEqualTo(5);
    }

    @Test
    void findFirstStatDate_returnsEarliest_orNull() {
        assertThat(repository.findFirstStatDate(userId)).isNull();

        repository.addXp(userId, DAY, 2);
        repository.addXp(userId, DAY.minusDays(400), 2);

        assertThat(repository.findFirstStatDate(userId)).isEqualTo(DAY.minusDays(400));
    }

    @Test
    void updateGoalWords_changesOnlyThatDay_andRecomputesGoalMet() {
        chooseWeeklyGoal(30);
        LocalDate yesterday = DAY.minusDays(1);
        repository.addXp(userId, yesterday, 2);
        repository.upsertLookup(userId, DAY, 2, 100, 300, Instant.now(), 1);

        repository.updateGoalWords(userId, DAY, 1, 300);

        assertThat(goalWords()).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT goal_met FROM user_daily_stats WHERE user_id = ? AND stat_date = ?",
                Boolean.class, userId, DAY)).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT goal_words FROM user_daily_stats WHERE user_id = ? AND stat_date = ?",
                Integer.class, userId, yesterday)).isEqualTo(5);
    }
}
