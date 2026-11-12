package com.jp.elearningjp.service.curriculum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.config.TimeConfig;
import com.jp.elearningjp.dto.response.curriculum.LessonAutosaveResponse;
import com.jp.elearningjp.entity.curriculum.Lesson;
import com.jp.elearningjp.entity.curriculum.LessonAutosave;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.LessonAutosaveRepository;
import com.jp.elearningjp.repository.curriculum.LessonRepository;
import com.jp.elearningjp.shared.util.CourseAccessGuard;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Autosave nội dung chưa lưu theo người và theo bài (docs/lesson-review-design.md mục 5):
 * <ul>
 *   <li>Chỉ ADMIN / giáo viên <b>đang được giao</b> khóa đọc / ghi được (giáo viên đã bị gỡ thì không).</li>
 *   <li>Không tăng version nội dung bài, không ai khác thấy (kể cả ADMIN), vẫn ghi được khi bài đang chờ duyệt
 *       hoặc đang được ADMIN sửa — không đi qua {@code requireLessonEditable}.</li>
 *   <li>Xóa khi lưu thành công / người dùng bấm "Bỏ" (FE gọi {@link #delete}); job dọn bản quá {@link #RETENTION}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LessonAutosaveService {

    /** Kích thước tối đa 1 bản (byte UTF-8). */
    static final int MAX_BYTES = 64 * 1024;
    /** Số bản tối đa của 1 người trong 1 bài. */
    static final int MAX_PER_LESSON = 50;
    static final Duration RETENTION = Duration.ofDays(30);
    static final Pattern FORM_KEY = Pattern.compile("[a-z][a-z0-9:_-]{0,99}");

    LessonAutosaveRepository autosaveRepository;
    LessonRepository lessonRepository;
    CourseAccessGuard courseAccessGuard;
    CurrentUserService currentUserService;
    ObjectMapper objectMapper;
    Clock clock;

    /** Bản chưa lưu của người dùng hiện tại trong bài. */
    @Transactional(readOnly = true)
    public List<LessonAutosaveResponse> list(Long lessonId) {
        requireAccess(lessonId);
        Long userId = currentUserService.getCurrentUserId();
        return autosaveRepository.findByUserIdAndLessonIdOrderByUpdatedAtDesc(userId, lessonId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Ghi đè bản chưa lưu của 1 form. */
    @Transactional
    public void save(Long lessonId, String formKey, JsonNode content) {
        requireValidKey(formKey);
        requireAccess(lessonId);
        String json = content == null || content.isNull() ? "null" : content.toString();
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new AppException(ErrorCode.LESSON_AUTOSAVE_TOO_LARGE);
        }
        Long userId = currentUserService.getCurrentUserId();
        boolean exists = autosaveRepository.findByUserIdAndLessonIdOrderByUpdatedAtDesc(userId, lessonId).stream()
                .anyMatch(a -> a.getFormKey().equals(formKey));
        if (!exists && autosaveRepository.countByUserIdAndLessonId(userId, lessonId) >= MAX_PER_LESSON) {
            throw new AppException(ErrorCode.LESSON_AUTOSAVE_TOO_MANY);
        }
        autosaveRepository.upsert(userId, lessonId, formKey, json, Instant.now(clock));
    }

    /** Xóa bản chưa lưu của 1 form (lưu thành công hoặc người dùng bấm "Bỏ"). Không có thì bỏ qua. */
    @Transactional
    public void delete(Long lessonId, String formKey) {
        requireValidKey(formKey);
        requireAccess(lessonId);
        autosaveRepository.deleteOne(currentUserService.getCurrentUserId(), lessonId, formKey);
    }

    /** Dọn bản không đụng tới quá {@link #RETENTION} — mỗi ngày 3:30, 1 instance (ShedLock). */
    @Scheduled(cron = "0 30 3 * * *", zone = TimeConfig.APP_ZONE)
    @SchedulerLock(name = "lessonAutosaveCleanupJob", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    @Transactional
    public void cleanup() {
        int removed = autosaveRepository.deleteOlderThan(Instant.now(clock).minus(RETENTION));
        if (removed > 0) {
            log.info("[LESSON_AUTOSAVE] Removed {} autosave(s) older than {} days", removed, RETENTION.toDays());
        }
    }

    // =========================================================

    /** ADMIN hoặc giáo viên đang được giao khóa của bài (bài / khóa đã xóa → không tìm thấy). */
    private void requireAccess(Long lessonId) {
        Lesson lesson = lessonRepository.findByIdAndDeletedFalse(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));
        if (lesson.getCourse().isDeleted()) {
            throw new AppException(ErrorCode.COURSE_NOT_FOUND);
        }
        courseAccessGuard.requireAssignedOrAdmin(lesson.getCourse(), ErrorCode.FORBIDDEN_LESSON_ACCESS);
    }

    private static void requireValidKey(String formKey) {
        if (formKey == null || !FORM_KEY.matcher(formKey).matches()) {
            throw new AppException(ErrorCode.LESSON_AUTOSAVE_KEY_INVALID);
        }
    }

    private LessonAutosaveResponse toResponse(LessonAutosave a) {
        JsonNode content;
        try {
            content = objectMapper.readTree(a.getContent());
        } catch (JsonProcessingException e) {
            content = null;
        }
        return LessonAutosaveResponse.builder()
                .formKey(a.getFormKey())
                .content(content)
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
