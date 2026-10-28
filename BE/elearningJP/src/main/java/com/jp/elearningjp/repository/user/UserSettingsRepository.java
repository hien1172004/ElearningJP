package com.jp.elearningjp.repository.user;

import com.jp.elearningjp.entity.user.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository cho {@code user_settings}.
 *
 * <p>Entity dùng {@code @MapsId} nên khoá chính chính là {@code user_id}; property
 * path vì vậy phải là {@code user.id} (viết {@code User_Id}).</p>
 */
@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

    Optional<UserSettings> findByUser_Id(Long userId);

    /** Danh sách user đã TẮT nhắc SRS — job sẽ bỏ qua (không tải cả entity). */
    @Query("SELECT s.user.id FROM UserSettings s WHERE s.notifySrsDue = false")
    List<Long> findUserIdsWithSrsDueDisabled();

    /** Danh sách user đã TẮT cảnh báo streak. */
    @Query("SELECT s.user.id FROM UserSettings s WHERE s.notifyStreakWarning = false")
    List<Long> findUserIdsWithStreakWarningDisabled();

    /** Danh sách user đã TẮT thông báo cộng đồng (reply bình luận). */
    @Query("SELECT s.user.id FROM UserSettings s WHERE s.notifyCommunity = false")
    List<Long> findUserIdsWithCommunityDisabled();
}
