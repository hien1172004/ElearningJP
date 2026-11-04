package com.jp.elearningjp.service.srs;

import com.jp.elearningjp.config.SrsProperties;
import com.jp.elearningjp.config.TimeConfig;
import com.jp.elearningjp.repository.srs.SrsItemRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Job hằng ngày (03:00 giờ VN): tự động lưu trữ thẻ SRS quá hạn ôn hơn
 * {@code srs.archive-after-days} ngày. Thẻ NEW không bị lưu trữ.
 * Người dùng có thể thêm lại từ đó (tìm kiếm → "Thêm vào SRS") hoặc khôi phục trong Sổ tay.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SrsArchiveScheduler {

    SrsItemRepository srsItemRepository;
    SrsProperties srsProperties;
    Clock clock;

    @Scheduled(cron = "0 0 3 * * *", zone = TimeConfig.APP_ZONE)
    @SchedulerLock(name = "srsArchiveJob", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    @Transactional
    public void archiveOverdueCards() {
        Instant cutoff = Instant.now(clock).minus(srsProperties.getArchiveAfterDays(), ChronoUnit.DAYS);
        int archived = srsItemRepository.archiveOverdue(cutoff);
        log.info("[SRS] Archive job done: archived={} (overdue before {})", archived, cutoff);
    }
}
