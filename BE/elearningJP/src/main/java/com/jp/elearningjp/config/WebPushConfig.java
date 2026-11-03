package com.jp.elearningjp.config;

import nl.martijndwars.webpush.PushService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Web Push (VAPID). Public/private key được nạp từ cấu hình
 * {@code app.web-push.*} (dev dùng key mặc định, production ghi đè bằng env).
 */
@Configuration
public class WebPushConfig {

    @Bean
    public PushService pushService(
            @Value("${app.web-push.vapid-public-key:}") String publicKey,
            @Value("${app.web-push.vapid-private-key:}") String privateKey,
            @Value("${app.web-push.vapid-subject:mailto:admin@elearningjp.com}") String subject
    ) {
        PushService pushService = new PushService();
        try {
            pushService.setPublicKey(publicKey);
            pushService.setPrivateKey(privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("VAPID keys không hợp lệ — kiểm tra cấu hình app.web-push.*", e);
        }
        pushService.setSubject(subject);
        return pushService;
    }
}
