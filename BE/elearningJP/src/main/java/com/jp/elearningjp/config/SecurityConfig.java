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
            // Promotion preview/apply endpoints (kept public if frontend needs them before auth)
            "/api/v1/promotions/preview",
            "/api/v1/promotions/apply",
            // Thi thử JLPT công khai
            "/api/v1/exams/*/attempts",
            "/api/v1/exam-attempts/*/submit",
            // Upload media (ảnh, audio)
            "/api/v1/media/upload",
            // AI Speaking tools & scenarios
            ApiPaths.API_V1 + "/ai/**",
    };

    private static final String[] PUBLIC_PUT_ENDPOINTS = {
            // Tự động lưu đáp án câu hỏi thi thử
            "/api/v1/exam-attempts/*/answers",
    };

    private static final String[] PUBLIC_DELETE_ENDPOINTS = {
            ApiPaths.API_V1 + "/ai/**",
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            // Auth
            "/api/v1/auth/reset-password/validate",
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
            "/api/v1/exam-attempts/*/result",
            "/api/v1/exam-attempts/*/review",
            "/api/v1/exam-attempts/my-history",
            // Comments — cho phép xem danh sách / chi tiết / reply công khai
            // (các method POST/PUT/DELETE tự bảo vệ qua @PreAuthorize("isAuthenticated()"))
            "/api/v1/LESSON/*/comments",
            "/api/v1/VOCABULARY/*/comments",
            "/api/v1/KANJI/*/comments",
            "/api/v1/GRAMMAR/*/comments",
            "/api/v1/comments/*",
            "/api/v1/comments/*/replies",
            // AI Speaking & Scenarios công khai
            ApiPaths.API_V1 + "/ai/**"
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
                                "/actuator/health"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.PUT, PUBLIC_PUT_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.DELETE, PUBLIC_DELETE_ENDPOINTS).permitAll()
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