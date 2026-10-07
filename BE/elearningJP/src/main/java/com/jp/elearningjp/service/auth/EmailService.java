package com.jp.elearningjp.service.auth;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Gửi email reset password
     * Dùng @Async để không block request — gửi email chạy nền
     */
    @Async
    public void sendPasswordResetEmail(String toEmail, String token, long expiryMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[E-Learning JP] Đặt lại mật khẩu");

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            String htmlContent = buildEmailContent(resetLink, expiryMinutes);

            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            // Không throw exception — tránh lộ thông tin email có tồn tại hay không
        }
    }

    private String buildEmailContent(String resetLink, long expiryMinutes) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto;">
                    <h2 style="color: #e50914;">E-Learning JP</h2>
                    <p>Bạn vừa yêu cầu đặt lại mật khẩu.</p>
                    <p>Nhấn vào nút bên dưới để đặt lại mật khẩu:</p>
                    <a href="%s"
                       style="display: inline-block; padding: 12px 24px;
                              background-color: #e50914; color: white;
                              text-decoration: none; border-radius: 4px;
                              font-weight: bold;">
                        Đặt lại mật khẩu
                    </a>
                    <p style="color: #888; margin-top: 16px;">
                        Link này sẽ hết hạn sau <strong>%d phút</strong>.
                    </p>
                    <p style="color: #888;">
                        Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
                    </p>
                    <hr style="border: none; border-top: 1px solid #eee;" />
                    <p style="color: #aaa; font-size: 12px;">
                        © 2026 E-Learning JP
                    </p>
                </div>
                """.formatted(resetLink, expiryMinutes);
    }
}