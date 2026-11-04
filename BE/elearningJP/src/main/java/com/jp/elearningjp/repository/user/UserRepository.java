package com.jp.elearningjp.repository.user;

import com.jp.elearningjp.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findUserByEmail(String email);

    boolean existsByEmail(String email);

    /** Batch fetch user displayName + avatarUrl theo danh sách ID. */
    @Query("SELECT u.id, u.fullName, u.avatarUrl FROM User u WHERE u.id IN :ids")
    List<Object[]> findDisplayInfosByIds(Collection<Long> ids);

    /** User không được nhận thông báo nhắc nhở: không ACTIVE (bị khoá BANNED, INACTIVE) hoặc đã xoá. */
    @Query("SELECT u.id FROM User u WHERE u.status <> com.jp.elearningjp.shared.enums.UserStatus.ACTIVE OR u.deleted = true")
    List<Long> findIdsNotEligibleForNotifications();

    /** Batch lấy (userId, roleName) cho một trang user — tránh N+1 khi map danh sách admin. */
    @Query("SELECT u.id, r.name FROM User u JOIN u.roles r WHERE u.id IN :ids")
    List<Object[]> findRoleNamesByUserIds(Collection<Long> ids);

    // =========================================================
    // THỐNG KÊ CHO DASHBOARD ADMIN (chỉ tính user chưa xóa)
    // =========================================================

    /**
     * 1 dòng: [tổng user, user mới trong [from,to), user mới kỳ trước [prevFrom,from),
     * số BANNED, số INACTIVE, số đăng ký bằng Google].
     */
    @Query(value = """
            SELECT COUNT(*),
                   COUNT(*) FILTER (WHERE created_at >= :from AND created_at < :to),
                   COUNT(*) FILTER (WHERE created_at >= :prevFrom AND created_at < :from),
                   COUNT(*) FILTER (WHERE status = 'BANNED'),
                   COUNT(*) FILTER (WHERE status = 'INACTIVE'),
                   COUNT(*) FILTER (WHERE auth_provider = 'GOOGLE')
            FROM users
            WHERE deleted = false
            """, nativeQuery = true)
    List<Object[]> aggregateUserOverview(Instant prevFrom, Instant from, Instant to);

    /** Số user mới theo ngày (ngày tính theo múi giờ {@code zone}): [ngày, số user]. */
    @Query(value = """
            SELECT CAST((created_at AT TIME ZONE :zone) AS date) AS d, COUNT(*)
            FROM users
            WHERE deleted = false AND created_at >= :from AND created_at < :to
            GROUP BY d
            """, nativeQuery = true)
    List<Object[]> countNewUsersByDay(Instant from, Instant to, String zone);
}
