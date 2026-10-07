package com.jp.elearningjp.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SrsRating {
    AGAIN((short) 1, "Quên, cần học lại ngay"),
    HARD((short) 3, "Khó nhớ, tốn nhiều thời gian"),
    GOOD((short) 4, "Nhớ bình thường, phản xạ chuẩn"),
    EASY((short) 5, "Quá dễ, nhớ rất sâu");

    private final short qualityScore; // Điểm chất lượng q (từ 1 đến 5 theo chuẩn SM-2)
    private final String description;
}
