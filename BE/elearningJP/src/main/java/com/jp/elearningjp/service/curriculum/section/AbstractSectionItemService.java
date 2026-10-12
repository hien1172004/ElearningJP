package com.jp.elearningjp.service.curriculum.section;

import com.jp.elearningjp.entity.curriculum.LessonSection;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.LessonSectionRepository;
import com.jp.elearningjp.shared.enums.LessonSectionType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Base class chung cho 3 service quản lý item theo section type
 * (KanjiSectionItemService, VocabularySectionItemService, GrammarSectionItemService).
 *
 * <p>Mục đích: gom các helper dùng chung (load active section, kiểm tra section type,
 * truy cập repository section chung).</p>
 *
 * <p>Các service con sẽ tự định nghĩa logic CRUD riêng cho entity item của mình.</p>
 */
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public abstract class AbstractSectionItemService {

    LessonSectionRepository lessonSectionRepository;

    /**
     * Load section còn active (chưa soft-delete). Nếu {@code expectedType} != null,
     * ném lỗi {@link ErrorCode#LESSON_SECTION_TYPE_INVALID} khi type không khớp.
     */
    protected LessonSection getActiveSection(Long sectionId, LessonSectionType expectedType) {
        LessonSection section = lessonSectionRepository.findById(sectionId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND));
        if (section.isDeleted()) {
            throw new AppException(ErrorCode.LESSON_SECTION_NOT_FOUND);
        }
        if (expectedType != null && section.getSectionType() != expectedType) {
            throw new AppException(ErrorCode.LESSON_SECTION_TYPE_INVALID);
        }
        return section;
    }
}
