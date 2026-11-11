package com.jp.elearningjp.service.curriculum.snapshot;

import com.jp.elearningjp.entity.exam.ExamPublishedVersion;
import com.jp.elearningjp.repository.curriculum.CourseRepository;
import com.jp.elearningjp.repository.exam.ExamAttemptRepository;
import com.jp.elearningjp.repository.exam.ExamRepository;
import com.jp.elearningjp.service.exam.snapshot.ExamSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Khóa học / đề thi đang xuất bản chưa có snapshot → tạo lúc khởi động
 * để học viên chuyển sang học trên bản xuất bản. Mỗi khóa / đề chạy trong giao dịch riêng;
 * lỗi 1 khóa/đề chỉ ghi log (không làm rollback các khóa/đề khác).
 * Chạy nhiều instance cùng lúc: ràng buộc UNIQUE (course_id/exam_id, version_no) chặn tạo trùng.
 * Tắt bằng {@code app.snapshot.backfill-enabled=false}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.snapshot.backfill-enabled", havingValue = "true", matchIfMissing = true)
public class PublishedSnapshotBackfill implements ApplicationRunner {

    private final CourseRepository courseRepository;
    private final ExamRepository examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final CourseSnapshotService courseSnapshotService;
    private final ExamSnapshotService examSnapshotService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<Long> courseIds = courseRepository.findPublishedIdsWithoutSnapshot();
        for (Long courseId : courseIds) {
            try {
                transactionTemplate.executeWithoutResult(status -> courseRepository.findById(courseId)
                        .filter(c -> c.getPublishedVersionId() == null)
                        .ifPresent(c -> courseSnapshotService.publish(c, null)));
            } catch (RuntimeException e) {
                log.error("[SNAPSHOT-BACKFILL] Course id={} failed", courseId, e);
            }
        }

        List<Long> examIds = examRepository.findPublishedExamIdsWithoutSnapshot();
        for (Long examId : examIds) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    ExamPublishedVersion version = examSnapshotService.publish(
                            examId, null, "Phiên bản phát hành đầu tiên (V1)");
                    if (version != null) {
                        examAttemptRepository.backfillPublishedVersion(examId, version.getId());
                    }
                });
            } catch (RuntimeException e) {
                log.error("[SNAPSHOT-BACKFILL] Exam id={} failed", examId, e);
            }
        }
        if (!courseIds.isEmpty() || !examIds.isEmpty()) {
            log.info("[SNAPSHOT-BACKFILL] Created snapshots for {} course(s), {} exam(s)", courseIds.size(), examIds.size());
        }
    }
}
