package com.jp.elearningjp.shared.enums;

/**
 * Loai su kien hoc tap ghi vao bang {@code study_events}.
 * Gia tri khop voi cot {@code event_type} (VARCHAR(24)) trong DB.
 */
public enum StudyEventType {

    /** Mot lan tra tu dien. 1 lookup = 1 "tu da hoc". */
    LOOKUP,

    /** Hoan thanh 1 bai hoc (lesson). */
    LESSON_COMPLETE,

    /** Hoan thanh 1 bai quiz. */
    QUIZ_SUBMIT,

    /** Heartbeat thoi gian hoc (chi ghi thoi gian, khong XP). */
    HEARTBEAT
}