package com.jp.elearningjp.service.curriculum.importer;

import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Load section đích của import + kiểm tra loại section và quyền chỉnh sửa (chủ khóa hoặc ADMIN). */
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
        courseAccessGuard.requireOwnerOrAdmin(section.getLesson().getCourse(), ErrorCode.FORBIDDEN_LESSON_ACCESS);
        return section;
    }
}
