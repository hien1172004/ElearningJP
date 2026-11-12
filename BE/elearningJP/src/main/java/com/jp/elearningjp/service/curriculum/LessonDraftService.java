package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonDraft;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseTeacherRepository;
import com.jp.elearningjp.repository.curriculum.LessonDraftRepository;
import com.jp.elearningjp.repository.curriculum.LessonDraftRestoreRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.service.curriculum.snapshot.CourseSnapshotReader;
import com.jp.elearningjp.service.curriculum.snapshot.LessonSnapshot;
import com.jp.elearningjp.shared.enums.DraftKind;
import com.jp.elearningjp.shared.enums.DraftStatus;
import com.jp.elearningjp.shared.util.CurrentUserService;
import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Bản nháp theo phía của bài học (V65, docs/lesson-review-design.md mục 8).
 *
 * <ul>
 *   <li>Mỗi bài luôn có đúng 1 nháp TEACHER (giáo viên được giao dùng chung) và 1 nháp ADMIN (các ADMIN dùng chung),
 *       tạo cùng bài ({@link #createDraftsFor}). Hai phía không thấy nháp của nhau.</li>
 *   <li>Người quản lý đọc / ghi nháp của phía mình ({@link #readDraftId}, {@link #writableDraft}); học viên của khóa
 *       chưa có bản live đọc nháp TEACHER như trước.</li>
 *   <li>Một phía xuất bản → nháp đó trùng live; nháp phía kia chưa sửa gì thì tự dựng lại theo live, đã sửa thì
 *       thành nháp cũ (phải "cập nhật bản mới") — {@link #afterLivePublished}.</li>
 * </ul>
 * Không phụ thuộc {@code CourseAccessGuard} / service nội dung (guard gọi vào đây) — tránh vòng phụ thuộc.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonDraftService {

    LessonDraftRepository draftRepository;
    LessonRepository lessonRepository;
    LessonDraftRestoreRepository restoreRepository;
    CourseSnapshotReader snapshotReader;
    CourseTeacherRepository courseTeacherRepository;
    CurrentUserService currentUserService;
    EntityManager entityManager;
    Clock clock;

    /** Phía của người dùng hiện tại: ADMIN → nháp ADMIN; còn lại → nháp TEACHER. */
    public DraftKind currentKind() {
        return currentUserService.isAdmin() ? DraftKind.ADMIN : DraftKind.TEACHER;
    }

    // =========================================================
    // ĐỌC
    // =========================================================

    /** Nháp đang dùng (EDITING / SUBMITTED) của 1 phía; tạo nếu thiếu chỉ ở các thao tác ghi. */
    public Optional<LessonDraft> findActive(Long lessonId, DraftKind kind) {
        return draftRepository.findActive(lessonId, kind.name());
    }

    /**
     * Id nháp mà người dùng hiện tại đọc nội dung bài: người quản lý → nháp phía mình; học viên (khóa chưa có bản live)
     * → nháp TEACHER. Không có nháp (dữ liệu lỗi) → {@code LESSON_DRAFT_NOT_FOUND}.
     */
    public Long readDraftId(Lesson lesson) {
        DraftKind kind = readKind(lesson.getCourse());
        requireVisible(lesson, kind);
        return findActive(lesson.getId(), kind)
                .map(LessonDraft::getId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_DRAFT_NOT_FOUND));
    }

    /**
     * Phía {@code kind} có thấy bài (mở / sửa bài, danh sách bài) không (V66): bài đã lên bản học viên hoặc bài do chính
     * phía này tạo. Bài mới phía bên kia tạo chưa được duyệt / lưu coi như không tồn tại — ADMIN xem bản giáo viên đã
     * gửi qua yêu cầu duyệt ({@code GET /lesson-review-requests/{id}/content}), không qua trang bài.
     */
    public static boolean isVisible(Lesson lesson, DraftKind kind) {
        return Boolean.TRUE.equals(lesson.getInPublishedVersion())
                || lesson.getCreatedKind() == null
                || lesson.getCreatedKind() == kind;
    }

    /** Bài phía bên kia mới tạo (chưa duyệt / lưu) → {@code LESSON_NOT_FOUND} (không để lộ bài tồn tại). */
    public void requireVisible(Lesson lesson, DraftKind kind) {
        if (!isVisible(lesson, kind)) {
            throw new AppException(ErrorCode.LESSON_NOT_FOUND);
        }
    }

    /** Như {@link #requireVisible(Lesson, DraftKind)} với phía người dùng hiện tại đọc trong khóa của bài. */
    public void requireVisible(Lesson lesson) {
        requireVisible(lesson, readKind(lesson.getCourse()));
    }

    /** Bài của khóa mà người dùng hiện tại thấy, theo thứ tự (người quản lý → phía mình; còn lại → TEACHER). */
    public java.util.List<Lesson> visibleLessons(Course course) {
        return lessonRepository.findVisibleByCourseId(course.getId(), readKind(course));
    }

    /** Phía nháp người dùng hiện tại đọc trong khóa: người quản lý → phía mình; còn lại → TEACHER. */
    public DraftKind readKind(Course course) {
        return isManager(course) ? currentKind() : DraftKind.TEACHER;
    }

    /**
     * Phần tử nội dung (section / item / quiz / kịch bản) thuộc nháp {@code draft} có được người dùng hiện tại đọc không:
     * người quản lý → chỉ nháp đang dùng của phía mình; học viên → chỉ nháp TEACHER. Không được → {@code notFound}
     * (không để lộ phần tử của phía bên kia tồn tại).
     */
    public void requireReadable(LessonDraft draft, ErrorCode notFound) {
        if (draft == null || draft.getStatus() == DraftStatus.ARCHIVED) {
            throw new AppException(notFound);
        }
        DraftKind expected = isManager(draft.getLesson().getCourse()) ? currentKind() : DraftKind.TEACHER;
        if (draft.getKind() != expected) {
            throw new AppException(notFound);
        }
    }

    // =========================================================
    // GHI
    // =========================================================

    /**
     * Nháp phía mình để ghi nội dung bài (guard đã kiểm quyền + khóa sửa khóa học): dựng từ live nếu còn trống,
     * chặn khi đã gửi duyệt, rồi ghi nhận lần sửa (tăng {@code draft_revision}, đánh dấu có thay đổi). Câu UPDATE giữ
     * khóa dòng tới hết giao dịch nên "gửi duyệt" chạy chồng lên sẽ chờ rồi thấy {@code draft_revision} đã đổi.
     */
    public LessonDraft writableDraft(Lesson lesson) {
        DraftKind kind = currentKind();
        requireVisible(lesson, kind);
        LessonDraft draft = findActive(lesson.getId(), kind).orElseGet(() -> createDraft(lesson, kind));
        if (Boolean.TRUE.equals(draft.getNeedsImport())) {
            importFromLive(draft);
        }
        int updated = draftRepository.markEdited(draft.getId(), currentUserService.getCurrentUserId(),
                currentDisplayName(), Instant.now(clock));
        if (updated == 0) {
            throw new AppException(ErrorCode.LESSON_PENDING_REVIEW_LOCKED);
        }
        return draft;
    }

    /** Bài mới tạo: 2 nháp trống (bài chưa có trong live nên không cần dựng). */
    public void createDraftsFor(Lesson lesson) {
        for (DraftKind kind : DraftKind.values()) {
            if (findActive(lesson.getId(), kind).isEmpty()) {
                createDraft(lesson, kind);
            }
        }
    }

    private LessonDraft createDraft(Lesson lesson, DraftKind kind) {
        boolean inLive = lesson.getCourse().getPublishedVersionId() != null
                && Boolean.TRUE.equals(lesson.getInPublishedVersion());
        LessonDraft draft = draftRepository.saveAndFlush(LessonDraft.builder()
                .lesson(lesson)
                .kind(kind)
                .baseLiveRevision(lesson.getLiveRevision() != null ? lesson.getLiveRevision() : 0L)
                .needsImport(inLive)
                .build());
        log.info("[LESSON_DRAFT] Created {} draft id={} for lesson id={}", kind, draft.getId(), lesson.getId());
        return draft;
    }

    /**
     * Dựng lại nội dung nháp theo bản live hiện tại của bài (nháp trùng live, hết thay đổi). Bài chưa có trong live →
     * nháp trống. Chỉ SQL thuần → đẩy thay đổi JPA xuống trước. Không xóa bộ nhớ đệm (caller còn giữ entity khác):
     * caller không được dùng lại entity nội dung cũ của nháp này đã nạp trước đó.
     */
    public void importFromLive(LessonDraft draft) {
        Lesson lesson = draft.getLesson();
        Long lessonId = lesson.getId();
        Long liveVersionId = lesson.getCourse().getPublishedVersionId();
        LessonSnapshot live = liveVersionId != null
                ? snapshotReader.findLesson(liveVersionId, lessonId).orElse(null)
                : null;
        long liveRevision = lessonRepository.findLiveRevision(lessonId).orElse(0L);
        entityManager.flush();
        restoreRepository.replaceContent(lessonId, draft.getId(), live);
        draftRepository.markImported(draft.getId(), liveRevision);
        log.info("[LESSON_DRAFT] {} draft id={} of lesson id={} rebuilt from live (revision {})",
                draft.getKind(), draft.getId(), lessonId, liveRevision);
    }

    /**
     * Sau khi nội dung live của bài đổi (đã tăng {@code live_revision}): nháp vừa được xuất bản
     * ({@code publishedDraftId}, null = xuất bản không từ nháp nào) trùng live; nháp còn lại chưa sửa gì và đang soạn
     * thì dựng lại theo live, đã sửa / đang chờ duyệt thì giữ nguyên (thành nháp cũ).
     */
    public void afterLivePublished(Long lessonId, Long publishedDraftId) {
        long liveRevision = lessonRepository.findLiveRevision(lessonId).orElse(0L);
        for (DraftKind kind : DraftKind.values()) {
            Optional<LessonDraft> found = findActive(lessonId, kind);
            if (found.isEmpty()) {
                continue;
            }
            LessonDraft draft = found.get();
            if (draft.getId().equals(publishedDraftId)) {
                draftRepository.markPublished(draft.getId(), liveRevision);
            } else if (!Boolean.TRUE.equals(draft.getHasChanges()) && draft.getStatus() == DraftStatus.EDITING) {
                importFromLive(draft);
            } else {
                log.info("[LESSON_DRAFT] {} draft id={} of lesson id={} is now outdated (base {} < live {})",
                        kind, draft.getId(), lessonId, draft.getBaseLiveRevision(), liveRevision);
            }
        }
    }

    /**
     * "Cập nhật bản mới" / "Bỏ nháp" của phía mình: nháp hiện tại thành nháp cũ chỉ đọc ({@code keepOld}) hoặc bị bỏ,
     * nháp mới dựng từ bản live. Nháp đã gửi duyệt → phải rút / được duyệt / bị từ chối trước.
     */
    public LessonDraft restart(Lesson lesson, boolean keepOld) {
        DraftKind kind = currentKind();
        LessonDraft current = findActive(lesson.getId(), kind).orElse(null);
        if (current != null && current.getStatus() == DraftStatus.SUBMITTED) {
            throw new AppException(ErrorCode.LESSON_PENDING_REVIEW_LOCKED);
        }
        if (current != null && keepOld) {
            draftRepository.findArchived(lesson.getId(), kind.name())
                    .ifPresent(old -> draftRepository.deleteDraft(old.getId()));
            draftRepository.archive(current.getId());
        } else if (current != null) {
            draftRepository.deleteDraft(current.getId());
        }
        entityManager.flush();
        entityManager.clear();
        Lesson fresh = lessonRepository.findById(lesson.getId())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        LessonDraft draft = createDraft(fresh, kind);
        importFromLive(draft);
        return draftRepository.findById(draft.getId())
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_DRAFT_NOT_FOUND));
    }

    /** Nháp đã cũ: dựa trên bản live cũ hơn bản hiện tại của bài. */
    public boolean isOutdated(LessonDraft draft, Long liveRevision) {
        long live = liveRevision != null ? liveRevision : 0L;
        return draft.getBaseLiveRevision() != null && draft.getBaseLiveRevision() < live;
    }

    // =========================================================

    /** ADMIN hoặc giáo viên được giao. */
    private boolean isManager(Course course) {
        if (currentUserService.isAdmin()) {
            return true;
        }
        User user = currentUserService.getCurrentUserOrNull();
        return user != null && course != null
                && courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), user.getId());
    }

    private String currentDisplayName() {
        User user = currentUserService.getCurrentUserOrNull();
        if (user == null) return null;
        return user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getEmail();
    }
}
