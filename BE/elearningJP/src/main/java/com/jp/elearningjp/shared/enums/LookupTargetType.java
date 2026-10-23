package com.jp.elearningjp.shared.enums;

/**
 * Loai doi tuong ma nguoi dung tra cuu trong tu dien.
 * Dung cho {@code study_events.target_type} va de JOIN sang bang tuong ung
 * khi dung top-lookups (Khoi 4 cua trang Progress).
 */
public enum LookupTargetType {

    /** Tu vung — {@code study_events.target_id} tro sang {@code vocabulary.id}. */
    VOCAB,

    /** Chu Han — {@code target_id} luu gia tri NULL,
     *  chuoi can tra nam trong {@code source_ref}. */
    KANJI,

    /** Ngu phap — {@code target_id} tro sang {@code grammar_points.id}. */
    GRAMMAR
}