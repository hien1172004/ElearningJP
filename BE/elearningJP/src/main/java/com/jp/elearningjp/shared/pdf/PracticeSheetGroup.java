package com.jp.elearningjp.shared.pdf;

import java.util.List;

/** Nhóm khối luyện viết (vd 1 nhóm/chủ đề trong bài học). {@code title} null → không in dòng tiêu đề. */
public record PracticeSheetGroup(String title, List<PracticeSheetBlock> blocks) {
}
