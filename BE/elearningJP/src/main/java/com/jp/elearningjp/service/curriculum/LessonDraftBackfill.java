package com.jp.elearningjp.service.curriculum;

import com.jp.elearningjp.repository.curriculum.LessonDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Dựng nội dung các nháp còn trống cần lấy từ bản live ({@code lesson_drafts.needs_import}, V65) lúc khởi động — chủ yếu
 * nháp ADMIN tạo bởi migration. Chạy sau {@code PublishedSnapshotBackfill}. Mỗi nháp 1 giao dịch, khóa dòng nháp
 * (chạy nhiều instance: instance sau thấy {@code needs_import} đã tắt thì bỏ qua); lỗi 1 nháp chỉ ghi log.
 * Nháp chưa kịp dựng vẫn được dựng ở lần ghi đầu tiên ({@link LessonDraftService#writableDraft}).
 * Tắt bằng {@code app.snapshot.backfill-enabled=false}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.snapshot.backfill-enabled", havingValue = "true", matchIfMissing = true)
public class LessonDraftBackfill implements ApplicationRunner {

    private final LessonDraftRepository draftRepository;
    private final LessonDraftService lessonDraftService;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<Long> draftIds = draftRepository.findIdsNeedingImport();
        int done = 0;
        for (Long draftId : draftIds) {
            try {
                Boolean imported = transactionTemplate.execute(status -> draftRepository.findByIdForUpdate(draftId)
                        .filter(d -> Boolean.TRUE.equals(d.getNeedsImport()))
                        .map(d -> {
                            lessonDraftService.importFromLive(d);
                            return true;
                        })
                        .orElse(false));
                if (Boolean.TRUE.equals(imported)) {
                    done++;
                }
            } catch (RuntimeException e) {
                log.error("[LESSON_DRAFT-BACKFILL] Draft id={} failed", draftId, e);
            }
        }
        if (!draftIds.isEmpty()) {
            log.info("[LESSON_DRAFT-BACKFILL] Rebuilt {} / {} draft(s) from live", done, draftIds.size());
        }
    }
}
