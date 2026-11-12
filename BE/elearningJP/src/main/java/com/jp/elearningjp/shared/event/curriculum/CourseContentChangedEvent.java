package com.jp.elearningjp.shared.event.curriculum;

import com.jp.elearningjp.shared.enums.DraftKind;
import lombok.Getter;

/**
 * Nội dung khóa học vừa đổi — listener đẩy realtime để người đang mở khóa tải lại
 * (thay vì tới lúc lưu mới gặp 409 do bản đang xem đã cũ).
 */
@Getter
public class CourseContentChangedEvent extends CurriculumEvent {

    public enum Scope {
        /**
         * Bản đang soạn đổi → người soạn cần tải lại. Có {@code draftKind} = chỉ nháp phía đó đổi (V65: ADMIN và giáo
         * viên mỗi bên một nháp) → chỉ người soạn cùng phía được nhắc; null = phần dùng chung (cấu trúc khóa, đề thi).
         */
        DRAFT,
        /** Bản học viên đang học đổi (phiên bản mới, ẩn / hiện khóa, thông tin khóa) → cả học viên. */
        PUBLISHED,
        /**
         * Phiên sửa bài của ADMIN vừa bắt đầu / kết thúc / bị tiếp quản → người soạn cập nhật trạng thái khóa sửa
         * (banner "Admin đang sửa"); nội dung không đổi nên không nhắc tải lại.
         */
        LOCK,
        /**
         * Chỉ trạng thái duyệt đổi (gửi / rút / từ chối duyệt): nội dung người xem không đổi → người soạn tự làm mới
         * trạng thái duyệt, không nhắc tải lại.
         */
        REVIEW
    }

    private final Long courseId;
    private final Scope scope;
    /** Người thao tác — client của chính họ bỏ qua. */
    private final Long actorId;
    /** Chỉ với {@link Scope#DRAFT}: nháp phía nào đổi; null = dùng chung cả hai phía. */
    private final DraftKind draftKind;

    public CourseContentChangedEvent(Object source, Long courseId, Scope scope, Long actorId) {
        this(source, courseId, scope, actorId, null);
    }

    public CourseContentChangedEvent(Object source, Long courseId, Scope scope, Long actorId, DraftKind draftKind) {
        super(source);
        this.courseId = courseId;
        this.scope = scope;
        this.actorId = actorId;
        this.draftKind = draftKind;
    }
}
