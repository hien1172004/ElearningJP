package com.jp.elearningjp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Cung cấp {@link Clock} cố định ở múi giờ nghiệp vụ {@value #APP_ZONE}.
 *
 * <p><b>Vì sao cần?</b> Múi giờ mặc định của JVM phụ thuộc môi trường chạy:
 * máy dev (Windows) là GMT+7, còn container Docker là UTC. Cùng một commit
 * sẽ ghi {@code stat_date} khác nhau giữa dev và deploy → lệch ngày, lệch tuần,
 * lệch chuỗi streak.</p>
 *
 * <p>Khai báo tường minh bằng {@link Clock} giúp hành vi <b>giống nhau ở mọi
 * môi trường</b>, thay vì phụ thuộc cấu hình máy chủ.</p>
 *
 * <p>Chỉ dùng cho các mốc thời gian <b>nghiệp vụ</b> (ngày học, tuần xếp hạng,
 * streak). Các timestamp tuyệt đối kỹ thuật (SRS, {@code Instant.now()}) không
 * phụ thuộc múi giờ nên không cần đổi.</p>
 */
@Configuration
public class TimeConfig {

    /** Múi giờ nghiệp vụ của toàn bộ module gamification. */
    public static final String APP_ZONE = "Asia/Ho_Chi_Minh";

    /**
     * Clock hệ thống neo về múi giờ nghiệp vụ.
     *
     * <p>Dùng {@code Clock.system(zone)} thay vì {@code Clock.fixed(...)} để thời gian
     * vẫn trôi bình thường; chỉ thay đổi nguồn của "hôm nay" và "tuần này".</p>
     */
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(APP_ZONE));
    }
}
