package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.shared.persitence.BaseEntity;

import java.util.Collection;
import java.util.Map;

/**
 * Optimistic locking theo version client đã đọc: chặn trường hợp A mở form, B lưu trước,
 * rồi A lưu đè làm mất thay đổi của B. {@code @Version} của JPA chỉ bắt được 2 giao dịch
 * chạy chồng nhau; trường hợp trên cần client gửi kèm version.
 *
 * <p>Client không gửi version ({@code null}) → bỏ qua (tương thích client cũ).</p>
 */
public final class VersionCheck {

    private VersionCheck() {
    }

    public static void requireVersion(BaseEntity entity, Long expectedVersion) {
        if (expectedVersion != null && !expectedVersion.equals(entity.getVersion())) {
            throw new AppException(ErrorCode.CONCURRENT_MODIFICATION);
        }
    }

    /**
     * Sắp xếp lại hàng loạt: mỗi phần tử client gửi kèm version đã đọc (id → version).
     * Sắp xếp lại / sửa của người khác đều làm tăng version phần tử → danh sách client đang xem đã cũ.
     * {@code expectedVersions} null → bỏ qua; phần tử không có trong map → không kiểm tra phần tử đó.
     */
    public static void requireVersions(Collection<? extends BaseEntity> entities, Map<Long, Long> expectedVersions) {
        if (expectedVersions == null || expectedVersions.isEmpty()) {
            return;
        }
        for (BaseEntity entity : entities) {
            requireVersion(entity, expectedVersions.get(entity.getId()));
        }
    }
}
