package com.jp.elearningjp.repository.gamification;

import com.jp.elearningjp.entity.gamification.NotificationBroadcast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface NotificationBroadcastRepository extends JpaRepository<NotificationBroadcast, Long>,
        JpaSpecificationExecutor<NotificationBroadcast> {

    /**
     * Điều kiện người nhận dùng chung cho đếm & chèn: tài khoản chưa xóa, đang ACTIVE,
     * không phải người gửi, và thuộc đối tượng đã chọn.
     * Tham số có thể null được CAST tường minh (Postgres không suy ra kiểu của NULL).
     */
    String RECIPIENTS_FROM_WHERE = """
            FROM users u
            WHERE u.deleted = false
              AND u.status = 'ACTIVE'
              AND u.id <> :senderId
              AND (
                    :audience = 'ALL_USERS'
                 OR (:audience = 'ROLE' AND EXISTS (
                        SELECT 1 FROM user_role ur JOIN roles r ON r.id = ur.role_id
                        WHERE ur.user_id = u.id AND r.name = CAST(:role AS text)))
                 OR (:audience = 'COURSE_STUDENTS' AND EXISTS (
                        SELECT 1 FROM course_enrollments e
                        WHERE e.user_id = u.id AND e.deleted = false
                          AND e.course_id = CAST(:courseId AS BIGINT)))
              )
            """;

    /** Số người sẽ nhận (xem trước trước khi gửi). */
    @Query(value = "SELECT COUNT(*) " + RECIPIENTS_FROM_WHERE, nativeQuery = true)
    long countRecipients(Long senderId, String audience, String role, Long courseId);

    /**
     * Tạo thông báo ANNOUNCEMENT cho mọi người nhận bằng 1 câu lệnh (không lặp từng user).
     * {@code dataJson} chứa broadcastId để đẩy realtime / tra cứu về sau.
     *
     * @return số thông báo đã tạo
     */
    @Modifying
    @Query(value = """
            INSERT INTO notifications (user_id, title, body, type, data_json, is_read, deleted, created_at, updated_at, version)
            SELECT u.id, :title, :body, 'ANNOUNCEMENT', CAST(:dataJson AS jsonb), false, false, :now, :now, 0
            """ + RECIPIENTS_FROM_WHERE, nativeQuery = true)
    int insertNotifications(Long senderId, String audience, String role, Long courseId,
                            String title, String body, String dataJson, Instant now);

    @EntityGraph(attributePaths = {"sender", "course"})
    Page<NotificationBroadcast> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "course"})
    Page<NotificationBroadcast> findBySenderIdOrderByCreatedAtDesc(Long senderId, Pageable pageable);

    /** Lịch sử gửi có bộ lọc (từ khóa / đối tượng / người gửi) — nạp sẵn sender, course để tránh N+1. */
    @Override
    @EntityGraph(attributePaths = {"sender", "course"})
    Page<NotificationBroadcast> findAll(Specification<NotificationBroadcast> spec, Pageable pageable);
}
