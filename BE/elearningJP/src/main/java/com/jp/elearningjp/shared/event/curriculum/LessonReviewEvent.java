package com.jp.elearningjp.shared.event.curriculum;

import lombok.Getter;

import java.util.List;

/**
 * Sự kiện trong luồng duyệt theo bài (V62). Listener chạy AFTER_COMMIT:
 * <ul>
 *   <li>thông báo: SUBMITTED / WITHDRAWN → mọi ADMIN (1 thông báo cho cả lượt gửi);
 *       APPROVED / REJECTED → giáo viên đã gửi;</li>
 *   <li>realtime: người đang mở khóa tải lại (bài vừa khóa / mở khóa / lên bản live).</li>
 * </ul>
 */
@Getter
public class LessonReviewEvent extends CurriculumEvent {

    /**
     * SAVED: ADMIN tự đưa bản nháp của bài lên bản live (không qua duyệt).
     * DISCARDED: ADMIN bỏ thay đổi chưa duyệt — bản nháp quay về đúng bản live (bản live không đổi).
     */
    public enum Action { SUBMITTED, WITHDRAWN, APPROVED, REJECTED, SAVED, DISCARDED }

    private final Long courseId;
    private final String courseTitle;
    private final Action action;
    /** Các bài trong thao tác (gửi duyệt: cả lượt; còn lại: 1 bài). */
    private final List<Long> lessonIds;
    private final List<String> lessonTitles;
    /** Người thao tác — không gửi thông báo cho chính họ; client của họ bỏ qua tín hiệu realtime. */
    private final Long actorId;
    /** Người gửi yêu cầu (nhận thông báo duyệt / từ chối); null nếu không áp dụng. */
    private final Long submitterId;
    /** Ghi chú khi gửi / lý do từ chối / ghi chú khi duyệt (có thể null). */
    private final String note;
    /** Bản live vừa đổi trong lúc khóa đang xuất bản → học viên cũng cần tải lại. */
    private final boolean liveChangedWhilePublished;
    /** APPROVED / SAVED: ADMIN đánh dấu "thay đổi lớn" → báo học viên đã hoàn thành bài (LESSON_UPDATED). */
    private final boolean majorChange;

    public LessonReviewEvent(Object source, Long courseId, String courseTitle, Action action,
                             List<Long> lessonIds, List<String> lessonTitles, Long actorId, Long submitterId,
                             String note, boolean liveChangedWhilePublished) {
        this(source, courseId, courseTitle, action, lessonIds, lessonTitles, actorId, submitterId, note,
                liveChangedWhilePublished, false);
    }

    public LessonReviewEvent(Object source, Long courseId, String courseTitle, Action action,
                             List<Long> lessonIds, List<String> lessonTitles, Long actorId, Long submitterId,
                             String note, boolean liveChangedWhilePublished, boolean majorChange) {
        super(source);
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.action = action;
        this.lessonIds = List.copyOf(lessonIds);
        this.lessonTitles = List.copyOf(lessonTitles);
        this.actorId = actorId;
        this.submitterId = submitterId;
        this.note = note;
        this.liveChangedWhilePublished = liveChangedWhilePublished;
        this.majorChange = majorChange;
    }
}
