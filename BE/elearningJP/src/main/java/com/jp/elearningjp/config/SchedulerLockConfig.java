package com.jp.elearningjp.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * ShedLock — khoá phân tán cho {@code @Scheduled}.
 *
 * <p>Khi BE chạy nhiều instance (Railway/Render scale ngang), mỗi instance đều có
 * scheduler riêng nên job nhắc nhở sẽ chạy trùng. ShedLock ghi 1 row khoá vào bảng
 * {@code shedlock} (tạo ở migration V35) và chỉ instance giành được khoá mới chạy job.</p>
 *
 * <p>{@code usingDbTime()} lấy giờ từ DB thay vì đồng hồ từng instance — tránh lệch
 * clock giữa các container làm khoá bị giải phóng sớm/muộn.</p>
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .usingDbTime()
                        .build()
        );
    }
}
