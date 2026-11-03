package com.jp.elearningjp.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemoryLevel {
    NOT_LEARNED("Chưa thuộc", "#ef4444"),
    BARELY("Thuộc lơ mơ", "#f97316"),
    MODERATE("Thuộc kha khá", "#3b82f6"),
    MASTERED("Thuộc làu làu", "#22c55e");

    private final String title;
    private final String colorHex;

    public static MemoryLevel fromIntervalDays(int intervalDays, int repetitionCount) {
        if (repetitionCount == 0 || intervalDays <= 1) {
            return NOT_LEARNED;
        } else if (intervalDays <= 7) {
            return BARELY;
        } else if (intervalDays <= 30) {
            return MODERATE;
        } else {
            return MASTERED;
        }
    }
}

