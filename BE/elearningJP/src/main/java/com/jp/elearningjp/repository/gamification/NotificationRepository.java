package com.jp.elearningjp.repository.gamification;

import java.util.Collection;
import com.jp.elearningjp.entity.gamification.Notification;
import com.jp.elearningjp.shared.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho bảng {@code notifications}.
 *
 * <p>Lưu ý về tên derived query: entity có field {@code user} (ManyToOne) nên
 * property path phải là {@code User_Id} — viết {@code UserId} sẽ khiến Spring Data
 * hiểu là field {@code userId} không tồn tại. Các query dưới đây dùng {@code @Query}
 * tường minh để tránh nhầm lẫn đó.</p>
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Số thông báo chưa đọc — cho badge trên chuông. */
    @Query("SELECT COUNT(n) FROM Notification n " +
           "WHERE n.user.id = :userId AND n.read = false AND n.deleted = false")
    long countUnread(@Param("userId") Long userId);

    /** Danh sách phân trang, mới nhất trước. */
    @Query("SELECT n FROM Notification n " +
           "WHERE n.user.id = :userId AND n.deleted = false " +
           "ORDER BY n.createdAt DESC, n.id DESC")
    Page<Notification> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /** 20 thông báo CHƯA ĐỌC mới nhất — cho dropdown nhanh trên TopBar. */
    @Query("SELECT n FROM Notification n " +
           "WHERE n.user.id = :userId AND n.read = false AND n.deleted = false " +
           "ORDER BY n.createdAt DESC, n.id DESC")
    List<Notification> findRecentUnread(@Param("userId") Long userId, Pageable pageable);

    /** Load để service tự phân biệt NOT_FOUND và FORBIDDEN. */
    Optional<Notification> findByIdAndDeletedFalse(Long id);

    /** Chống trùng lớp 1: đã có thông báo cùng loại trong khoảng thời gian chưa? */
    boolean existsByUser_IdAndTypeAndCreatedAtBetween(
            Long userId, NotificationType type, Instant from, Instant to);

    /**
     * Đánh dấu tất cả đã đọc.
     *
     * <p>Query {@code @Modifying} bắt buộc chạy trong transaction — caller phải có
     * {@code @Transactional}. {@code clearAutomatically} để entity đã load trong
     * persistence context không còn giữ giá trị {@code read = false} cũ.</p>
     */
    /**
     * Chống spam like: user {@code actorId} đã từng tạo thông báo like cho đúng comment/reply này
     * chưa (bỏ like rồi like lại không tạo thông báo mới). {@code replyId = -1} nghĩa là like comment.
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM notifications n
                WHERE n.user_id = :userId
                  AND n.type = 'COMMUNITY_LIKE'
                  AND n.deleted = false
                  AND n.data_json ->> 'actorUserId' = CAST(:actorId AS text)
                  AND n.data_json ->> 'commentId' = CAST(:commentId AS text)
                  AND COALESCE(n.data_json ->> 'replyId', '-1') = CAST(:replyId AS text)
            )
            """, nativeQuery = true)
    boolean existsCommunityLike(@Param("userId") Long userId,
                                @Param("actorId") Long actorId,
                                @Param("commentId") Long commentId,
                                @Param("replyId") Long replyId);

    /** Xoá mềm mọi thông báo cộng đồng trỏ tới 1 comment (comment đã bị xoá). */
    @Modifying
    @Query(value = """
            UPDATE notifications
            SET deleted = TRUE, deleted_at = :now, updated_at = :now
            WHERE deleted = FALSE
              AND type IN ('COMMUNITY_REPLY', 'COMMUNITY_LIKE', 'COMMUNITY_DISLIKE')
              AND data_json ->> 'commentId' = CAST(:commentId AS text)
            """, nativeQuery = true)
    int softDeleteByCommentId(@Param("commentId") Long commentId, @Param("now") Instant now);

    /** Xoá mềm mọi thông báo cộng đồng trỏ tới các reply (đã bị xoá). Id truyền dạng chuỗi để so với jsonb. */
    @Modifying
    @Query(value = """
            UPDATE notifications
            SET deleted = TRUE, deleted_at = :now, updated_at = :now
            WHERE deleted = FALSE
              AND type IN ('COMMUNITY_REPLY', 'COMMUNITY_LIKE', 'COMMUNITY_DISLIKE')
              AND data_json ->> 'replyId' IN (:replyIds)
            """, nativeQuery = true)
    int softDeleteByReplyIds(@Param("replyIds") Collection<String> replyIds, @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true, n.updatedAt = :now " +
           "WHERE n.user.id = :userId AND n.read = false AND n.deleted = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") Instant now);

    /** Thông báo của 1 lần gửi hàng loạt — để đẩy realtime sau khi commit. */
    @Query(value = """
            SELECT * FROM notifications
            WHERE type = 'ANNOUNCEMENT' AND deleted = false
              AND data_json ->> 'broadcastId' = CAST(:broadcastId AS text)
            """, nativeQuery = true)
    List<Notification> findByBroadcastId(@Param("broadcastId") Long broadcastId);
}
