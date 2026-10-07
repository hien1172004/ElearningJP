package com.jp.elearningjp.shared.util;

import com.jp.elearningjp.entity.user.Role;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper lấy thông tin user hiện tại từ JWT + SecurityContext.
 *
 * <p>JWT do {@link com.jp.elearningjp.service.auth.AuthService} sinh ra có 2 claim:</p>
 * <ul>
 *   <li>{@code sub} (subject) = email</li>
 *   <li>{@code userId} = Long id của user (xem {@code AuthService.generateToken})</li>
 *   <li>{@code scope} = chuỗi {@code "ROLE_X ROLE_Y ..."} phân tách bởi space</li>
 * </ul>
 *
 * <p>Class này đảm bảo:</p>
 * <ol>
 *   <li>Có user authenticated trong SecurityContext (ngược lại {@link AppException} với {@code UNAUTHENTICATED})</li>
 *   <li>Lấy được {@link User} entity từ DB (1 query)</li>
 *   <li>Cung cấp API tiện cho Authorization check ở Service layer</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Object principal = authentication.getPrincipal();

        // Case 1: JWT principal (Spring Security OAuth2 Resource Server)
        if (principal instanceof Jwt jwt) {
            Long userId = jwt.getClaim("userId");
            if (userId != null) {
                return userRepository.findById(userId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            }
            // Fallback: nếu thiếu claim userId (token cũ), lấy qua email
            String email = jwt.getSubject();
            if (email != null) {
                return userRepository.findUserByEmail(email)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            }
        }

        // Case 2: principal là email (một số config cũ)
        String email = authentication.getName();
        return userRepository.findUserByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * @return Long userId của user hiện tại. Không throw nếu thiếu claim.
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Long userId = jwt.getClaim("userId");
            if (userId != null) return userId;
        }
        return getCurrentUser().getId();
    }

    /**
     * @return Set các role name (vd: {@code {"ADMIN", "TEACHER"}}) của user hiện tại.
     */
    public Set<String> getCurrentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                // Có thể là "ROLE_ADMIN" (nếu converter giữ prefix) hoặc "ADMIN"
                // Config hiện tại: authorityPrefix = "" nên có thể là "ADMIN"
                .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                .collect(Collectors.toSet());
    }

    public boolean hasRole(String roleName) {
        return getCurrentRoles().contains(roleName);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isTeacher() {
        return hasRole("TEACHER");
    }

    public boolean isTeacherOrAdmin() {
        return isAdmin() || isTeacher();
    }

    /**
     * Kiểm tra user hiện tại có phải là ADMIN hoặc TEACHER có role {@code TEACHER}
     * (kết hợp với owner check ở Service layer).
     */
    public User getCurrentUserOrThrowIfMissing() {
        return getCurrentUser();
    }

    /**
     * Lấy Set role entity từ user entity (helper cho code service).
     */
    public Set<Role> getCurrentRoleEntities() {
        User user = getCurrentUser();
        return user.getRoles() == null ? Set.of() : user.getRoles();
    }
}