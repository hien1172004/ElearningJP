package com.jp.elearningjp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import com.jp.elearningjp.shared.constants.ApiPaths;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/introspect",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/google",
            // Payment gateway webhook (IPN) called by VNPay
            "/api/v1/payments/vnpay/ipn",
            "/api/v1/subscriptions/webhook",
            // Promotion preview/apply endpoints (kept public if frontend needs them before auth)
            "/api/v1/promotions/preview",
            "/api/v1/promotions/apply",
            // Làm bài thi, upload media, luyện nói AI: bắt buộc đăng nhập (trước đây khách bị gán vào user id 1)
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            // Auth
            "/api/v1/auth/reset-password/validate",
            "/api/v1/subscriptions/plans",
            // Public courses browsing (catalog + detail + slug/level + public lesson list)
            "/api/v1/courses",
            "/api/v1/courses/{id:[0-9]+}",
            "/api/v1/courses/slug/*",
            "/api/v1/courses/level/*",
            "/api/v1/courses/*/lessons",
            "/api/v1/courses/*/lessons/*",
            // Tra cứu từ điển & Bảng chữ cái công khai
            "/api/v1/dictionary/**",
            // Đề thi công khai
            "/api/v1/exams",
            "/api/v1/exams/leaderboard",
            "/api/v1/exams/{id:[0-9]+}",
            // Comments — cho phép xem danh sách / chi tiết / reply công khai
            // (các method POST/PUT/DELETE tự bảo vệ qua @PreAuthorize("isAuthenticated()"))
            "/api/v1/LESSON/*/comments",
            "/api/v1/VOCABULARY/*/comments",
            "/api/v1/KANJI/*/comments",
            "/api/v1/GRAMMAR/*/comments",
            "/api/v1/comments/*",
            "/api/v1/comments/*/replies",
            // Xem kịch bản luyện nói công khai (sảnh luyện nói)
            ApiPaths.API_V1 + ApiPaths.AiScenario.BASE,
            ApiPaths.API_V1 + ApiPaths.AiScenario.BASE + "/*",
            // File audio TTS: thẻ <audio> không gắn được header Authorization; tên file là hash, không chứa dữ liệu user
            ApiPaths.API_V1 + "/ai/tts/audio/*",
            ApiPaths.API_V1 + "/ai/tts/stream"
            // POST /api/v1/courses/*/lessons/reorder cần auth (owner/admin) - để mặc định
    };

    private final CustomJwtDecoder customJwtDecoder;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(CustomJwtDecoder customJwtDecoder,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.customJwtDecoder = customJwtDecoder;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(request -> request
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/actuator/health",
                                // WebSocket handshake — browser không gắn được header
                                // Authorization vào handshake HTTP, nên phải permitAll.
                                // Xác thực thật sự diễn ra ở frame STOMP CONNECT
                                // (WebSocketAuthInterceptor), origin do
                                // WebSocketConfig.setAllowedOriginPatterns kiểm soát.
                                "/ws",
                                "/ws/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .decoder(customJwtDecoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                .csrf(AbstractHttpConfigurer::disable)

                // JWT là stateless — không dùng session
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.up.railway.app",
                "https://*.onrender.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Bỏ prefix "SCOPE_" mặc định vì scope đã có "ROLE_" từ buildScope()
        authoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

}