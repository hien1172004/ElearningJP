package com.jp.elearningjp.repository.gamification;

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
           "ORDER BY n.createdAt DESC")
    Page<Notification> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /** 20 thông báo CHƯA ĐỌC mới nhất — cho dropdown nhanh trên TopBar. */
    @Query("SELECT n FROM Notification n " +
           "WHERE n.user.id = :userId AND n.read = false AND n.deleted = false " +
           "ORDER BY n.createdAt DESC")
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
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.read = true, n.updatedAt = :now " +
           "WHERE n.user.id = :userId AND n.read = false AND n.deleted = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("now") Instant now);
}
