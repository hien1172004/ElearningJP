package com.jp.elearningjp.service.curriculum;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jp.elearningjp.entity.curriculum.Course;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.shared.enums.DraftKind;
import com.jp.elearningjp.shared.event.curriculum.CourseContentChangedEvent;
import com.jp.elearningjp.repository.curriculum.CourseTeacherRepository;
import com.jp.elearningjp.repository.curriculum.LessonReviewRequestRepository;
import com.jp.elearningjp.shared.enums.CourseReviewStatus;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Khóa sửa khóa học theo phía (V65, docs/lesson-review-design.md mục 8): lock trong Redis, key theo
 * {@code (phía, khóa học)}.
 * <ul>
 *   <li>Giáo viên được giao dùng chung 1 khóa phía TEACHER; các ADMIN dùng chung 1 khóa phía ADMIN. Hai phía sửa
 *       hai bản nháp khác nhau nên không chặn nhau.</li>
 *   <li>Bấm "Sửa khóa học" → {@link #acquire}: người cùng phía đang giữ thì từ chối, trừ khi tiếp quản
 *       (người cũ ở lần ghi kế tiếp nhận {@code COURSE_EDIT_SESSION_REQUIRED}). Lock sống {@link #TTL},
 *       FE gửi {@link #heartbeat} để gia hạn; đóng tab / mất mạng thì tự hết.</li>
 *   <li>Mọi thao tác ghi nội dung khóa / bài ({@link #requireHeld}) phải đang giữ lock của phía mình.
 *       Gửi duyệt / rút yêu cầu không cần lock.</li>
 *   <li>ADMIN: khóa học còn bài chờ duyệt (hoặc cả khóa chờ duyệt) → không bắt đầu sửa / không ghi được
 *       ({@code COURSE_EDIT_BLOCKED_BY_PENDING}) — phải duyệt / từ chối hết trước.</li>
 *   <li>Redis lỗi: không ai bắt đầu / tiếp tục sửa được (không ghi khi không có lock bảo vệ).</li>
 * </ul>
 * Lấy / nhả / tiếp quản đẩy realtime ({@link CourseContentChangedEvent.Scope#LOCK}) để banner của người khác tự cập nhật.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CourseEditLockService {

    static final String KEY_PREFIX = "course-edit-lock:";
    /** Thời gian sống của lock; FE gia hạn mỗi 60 giây khi trang soạn mở. */
    public static final Duration TTL = Duration.ofMinutes(5);

    /** Xóa key nếu đúng người giữ (so khớp userId trong giá trị). */
    private static final DefaultRedisScript<Long> RELEASE_IF_OWNER = new DefaultRedisScript<>("""
            local v = redis.call('GET', KEYS[1])
            if v and cjson.decode(v).userId == tonumber(ARGV[1]) then
              return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    /** Gia hạn nếu đúng người giữ. */
    private static final DefaultRedisScript<Long> EXPIRE_IF_OWNER = new DefaultRedisScript<>("""
            local v = redis.call('GET', KEYS[1])
            if v and cjson.decode(v).userId == tonumber(ARGV[1]) then
              return redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final CourseRepository courseRepository;
    private final CurrentUserService currentUserService;
    private final CourseTeacherRepository courseTeacherRepository;
    private final LessonReviewRequestRepository requestRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** Người đang giữ lock sửa khóa học của 1 phía. */
    public record LockInfo(Long userId, String userName, Instant since) {
    }

    static String key(DraftKind kind, Long courseId) {
        return KEY_PREFIX + kind.name() + ":" + courseId;
    }

    /** Phía của người dùng hiện tại: ADMIN → nháp ADMIN, còn lại (giáo viên được giao) → nháp TEACHER. */
    public DraftKind currentKind() {
        return currentUserService.isAdmin() ? DraftKind.ADMIN : DraftKind.TEACHER;
    }

    // =========================================================
    // BẮT ĐẦU / GIA HẠN / KẾT THÚC
    // =========================================================

    /**
     * Bắt đầu sửa khóa học. Đã giữ lock → gia hạn. Người cùng phía đang giữ → {@code COURSE_EDIT_LOCKED_BY_OTHER}
     * kèm tên người đó, trừ khi {@code takeover}.
     */
    public LockInfo acquire(Long courseId, boolean takeover) {
        Course course = findCourse(courseId);
        requireAssignedOrAdmin(course);
        DraftKind kind = currentKind();
        requireNoPendingReviews(course, kind);
        Long userId = currentUserService.getCurrentUserId();
        LockInfo mine = new LockInfo(userId, currentDisplayName(), Instant.now(clock));
        String key = key(kind, courseId);
        try {
            Optional<LockInfo> current = read(kind, courseId);
            if (current.isPresent() && Objects.equals(current.get().userId(), userId)) {
                redis.expire(key, TTL);
                return current.get();
            }
            if (current.isPresent() && !takeover) {
                throw lockedByOther(current.get());
            }
            if (current.isPresent()) {
                redis.opsForValue().set(key, write(mine), TTL); // tiếp quản
                log.info("[COURSE_LOCK] User id={} took over {} lock of course id={} from user id={}",
                        userId, kind, courseId, current.get().userId());
            } else if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, write(mine), TTL))) {
                // Người khác vừa lấy trước trong tích tắc → báo tên họ
                Optional<LockInfo> winner = read(kind, courseId);
                if (winner.isPresent()) {
                    throw lockedByOther(winner.get());
                }
                // Key còn nhưng giá trị hỏng (không đọc được người giữ) → ghi đè, không để khóa kẹt mãi
                redis.opsForValue().set(key, write(mine), TTL);
                log.warn("[COURSE_LOCK] Corrupted {} lock of course id={} overwritten by user id={}", kind, courseId, userId);
            } else {
                log.info("[COURSE_LOCK] User id={} started editing course id={} ({})", userId, courseId, kind);
            }
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[COURSE_LOCK] Redis unavailable while acquiring course id={}: {}", courseId, ex.getMessage());
            throw new AppException(ErrorCode.COURSE_EDIT_LOCK_UNAVAILABLE);
        }
        pushLockChanged(courseId, userId);
        return mine;
    }

    /** FE gọi định kỳ khi trang soạn mở. Không còn giữ lock (hết hạn / bị tiếp quản) → {@code COURSE_EDIT_SESSION_REQUIRED}. */
    public void heartbeat(Long courseId) {
        requireAssignedOrAdmin(findCourse(courseId));
        Long userId = currentUserService.getCurrentUserId();
        Long ok;
        try {
            ok = redis.execute(EXPIRE_IF_OWNER, List.of(key(currentKind(), courseId)),
                    String.valueOf(userId), String.valueOf(TTL.toMillis()));
        } catch (Exception ex) {
            log.warn("[COURSE_LOCK] Redis unavailable on heartbeat course id={}: {}", courseId, ex.getMessage());
            throw new AppException(ErrorCode.COURSE_EDIT_LOCK_UNAVAILABLE);
        }
        if (ok == null || ok == 0L) {
            throw new AppException(ErrorCode.COURSE_EDIT_SESSION_REQUIRED);
        }
    }

    /** "Xong": nhả lock nếu đang giữ (phần đã ghi vẫn nằm trong bản nháp của phía mình). */
    public void release(Long courseId) {
        Long userId = currentUserService.getCurrentUserId();
        DraftKind kind = currentKind();
        try {
            Long deleted = redis.execute(RELEASE_IF_OWNER, List.of(key(kind, courseId)), String.valueOf(userId));
            if (deleted != null && deleted > 0) {
                log.info("[COURSE_LOCK] User id={} stopped editing course id={} ({})", userId, courseId, kind);
                pushLockChanged(courseId, userId);
            }
        } catch (Exception ex) {
            log.warn("[COURSE_LOCK] Redis unavailable while releasing course id={}: {}", courseId, ex.getMessage());
        }
    }

    // =========================================================
    // KIỂM TRA KHI GHI
    // =========================================================

    /**
     * Gọi trong mọi thao tác ghi nội dung khóa / bài: người dùng hiện tại phải đang giữ lock của phía mình.
     * Người cùng phía đang giữ → {@code COURSE_EDIT_LOCKED_BY_OTHER}; không ai giữ → {@code COURSE_EDIT_SESSION_REQUIRED}.
     */
    public void requireHeld(Long courseId) {
        DraftKind kind = currentKind();
        // Giáo viên gửi duyệt không cần khóa → có thể gửi giữa phiên sửa của ADMIN; ADMIN phải xử lý xong mới ghi tiếp
        if (kind == DraftKind.ADMIN) {
            requireNoPendingReviews(findCourse(courseId), kind);
        }
        Long userId = currentUserService.getCurrentUserId();
        Optional<LockInfo> lock;
        try {
            lock = read(kind, courseId);
        } catch (Exception ex) {
            log.warn("[COURSE_LOCK] Redis unavailable, write refused for course id={}", courseId);
            throw new AppException(ErrorCode.COURSE_EDIT_LOCK_UNAVAILABLE);
        }
        if (lock.isPresent() && Objects.equals(lock.get().userId(), userId)) {
            return;
        }
        throw lock.map(this::lockedByOther).orElseGet(() -> new AppException(ErrorCode.COURSE_EDIT_SESSION_REQUIRED));
    }

    // =========================================================
    // ĐỌC
    // =========================================================

    /** Lock hiện tại của 1 phía; lỗi Redis → ném lên (caller quyết định). */
    public Optional<LockInfo> read(DraftKind kind, Long courseId) {
        String raw = redis.opsForValue().get(key(kind, courseId));
        return Optional.ofNullable(raw).map(this::parse);
    }

    /** Lock của phía người dùng hiện tại (để hiển thị). Lỗi Redis → trống. */
    public Optional<LockInfo> readForCurrentUser(Long courseId) {
        requireAssignedOrAdmin(findCourse(courseId));
        try {
            return read(currentKind(), courseId);
        } catch (Exception ex) {
            log.warn("[COURSE_LOCK] Redis unavailable while reading lock of course id={}: {}", courseId, ex.getMessage());
            return Optional.empty();
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /**
     * ADMIN chỉ sửa khi khóa học không còn gì chờ duyệt (bài chờ duyệt theo bài, hoặc cả khóa chờ duyệt) — chặn theo
     * khóa học. Lỗi kèm tên các bài cần xử lý. Giáo viên không bị chặn ở đây (nháp đã gửi tự khóa theo bài).
     */
    private void requireNoPendingReviews(Course course, DraftKind kind) {
        if (kind != DraftKind.ADMIN) {
            return;
        }
        List<String> pending = requestRepository.findPendingLessonTitles(course.getId());
        boolean coursePending = course.getReviewStatus() == CourseReviewStatus.PENDING_REVIEW;
        if (pending.isEmpty() && !coursePending) {
            return;
        }
        StringBuilder message = new StringBuilder("Khóa học còn ");
        if (!pending.isEmpty()) {
            message.append(pending.size()).append(" bài chờ duyệt (")
                    .append(String.join(", ", pending.stream().map(t -> "\"" + t + "\"")
                            .limit(5).toList()))
                    .append(pending.size() > 5 ? ", …" : "").append(')');
        }
        if (coursePending) {
            message.append(pending.isEmpty() ? "" : " và ").append("yêu cầu duyệt cả khóa");
        }
        message.append(" — hãy duyệt / từ chối hết trước khi sửa.");
        throw new AppException(ErrorCode.COURSE_EDIT_BLOCKED_BY_PENDING, message.toString());
    }

    /** ADMIN hoặc giáo viên được giao (không dùng CourseAccessGuard — guard phụ thuộc service này). */
    private void requireAssignedOrAdmin(Course course) {
        if (currentUserService.isAdmin()) {
            return;
        }
        Long userId = currentUserService.getCurrentUserId();
        if (userId == null || !courseTeacherRepository.existsByCourseIdAndTeacherId(course.getId(), userId)) {
            throw new AppException(ErrorCode.FORBIDDEN_COURSE_ACCESS);
        }
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findByIdAndDeletedFalse(courseId)
                .orElseThrow(() -> new AppException(ErrorCode.COURSE_NOT_FOUND));
    }

    private AppException lockedByOther(LockInfo lock) {
        String name = lock.userName() != null && !lock.userName().isBlank() ? lock.userName() : "Người khác";
        return new AppException(ErrorCode.COURSE_EDIT_LOCKED_BY_OTHER,
                name + " đang sửa khóa học này — bạn vẫn xem được, hoặc tiếp quản nếu cần.");
    }

    private void pushLockChanged(Long courseId, Long actorId) {
        // Không có giao dịch → listener chạy ngay (fallbackExecution); lỗi đẩy chỉ log trong listener
        eventPublisher.publishEvent(new CourseContentChangedEvent(
                this, courseId, CourseContentChangedEvent.Scope.LOCK, actorId));
    }

    private String currentDisplayName() {
        User user = currentUserService.getCurrentUserOrNull();
        if (user == null) return null;
        return user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getEmail();
    }

    private String write(LockInfo lock) {
        try {
            return objectMapper.writeValueAsString(lock);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }
    }

    /** Giá trị hỏng → null (coi như không có lock, tránh chặn mọi người vì dữ liệu rác). */
    private LockInfo parse(String raw) {
        try {
            LockInfo lock = objectMapper.readValue(raw, LockInfo.class);
            return lock.userId() != null ? lock : null;
        } catch (JsonProcessingException e) {
            log.warn("[COURSE_LOCK] Corrupted lock value ignored: {}", raw);
            return null;
        }
    }
}
