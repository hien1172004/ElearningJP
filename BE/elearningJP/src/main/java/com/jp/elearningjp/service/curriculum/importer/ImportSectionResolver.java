package com.jp.elearningjp.service.curriculum.importer;

import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Load section đích của import + kiểm tra loại section và quyền (giáo viên được giao hoặc ADMIN).
 * {@link #requireEditable} cho thao tác chỉ đọc (xem trước, xuất Excel);
 * {@link #requireWritable} cho thao tác ghi (commit) — bị khóa khi khóa đang chờ duyệt.
 */
@Component
@RequiredArgsConstructor
public class ImportSectionResolver {

    private final LessonSectionRepository lessonSectionRepository;
    private final CourseAccessGuard courseAccessGuard;

    public LessonSection requireEditable(Long sectionId, LessonSectionType expectedType) {
        LessonSection section = lessonSectionRepository.findById(sectionId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND));
        if (section.getSectionType() != expectedType) {
            throw new AppException(ErrorCode.LESSON_SECTION_TYPE_INVALID);
        }
        courseAccessGuard.requireAssignedOrAdmin(section.getLesson().getCourse(), ErrorCode.FORBIDDEN_LESSON_ACCESS);
        return section;
    }

    public LessonSection requireWritable(Long sectionId, LessonSectionType expectedType) {
        LessonSection section = requireEditable(sectionId, expectedType);
        // Đúng nháp phía mình (V65)
        courseAccessGuard.requireDraftEditable(section.getDraft(), ErrorCode.FORBIDDEN_LESSON_ACCESS,
                ErrorCode.LESSON_SECTION_NOT_FOUND);
        return section;
    }
}
