package com.jp.elearningjp.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cấu hình chính sách SRS (có thể ghi đè trong application.yaml / biến môi trường).
 * <ul>
 *   <li>{@code srs.new-cards-per-day}: số thẻ mới tối đa được đưa vào ôn mỗi ngày.</li>
 *   <li>{@code srs.archive-after-days}: thẻ quá hạn ôn hơn N ngày sẽ bị tự động lưu trữ.</li>
 * </ul>
 */
@Getter
@Component
public class SrsProperties {

    @Value("${srs.new-cards-per-day:20}")
    private int newCardsPerDay;

    @Value("${srs.archive-after-days:30}")
    private int archiveAfterDays;
}
