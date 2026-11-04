package com.jp.elearningjp.service.user;

import com.jp.elearningjp.dto.request.user.AdminUpdateUserRolesRequest;
import com.jp.elearningjp.dto.request.user.AdminUpdateUserStatusRequest;
import com.jp.elearningjp.dto.response.user.AdminUserResponse;
import com.jp.elearningjp.entity.user.Role;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.user.RoleRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.redis.UserTokenRevocationService;
import com.jp.elearningjp.shared.constants.PredefinedRole;
import com.jp.elearningjp.shared.enums.UserStatus;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ quản lý người dùng dành cho ADMIN: tra cứu, khóa/mở khóa, phân quyền.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminUserService {

    /** Các role hợp lệ có thể gán qua trang quản trị. */
    static final Set<String> ASSIGNABLE_ROLES = Set.of(
            PredefinedRole.USER_ROLE, PredefinedRole.TEACHER_ROLE, PredefinedRole.ADMIN_ROLE);

    static final int MAX_PAGE_SIZE = 100;

    UserRepository userRepository;
    RoleRepository roleRepository;
    CurrentUserService currentUserService;
    UserTokenRevocationService userTokenRevocationService;

    /**
     * Danh sách người dùng (chưa bị xóa), lọc theo từ khóa (email/họ tên), role, trạng thái.
     * {@code page} bắt đầu từ 1.
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> getUsers(String keyword, String role, UserStatus status, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 1);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        String roleFilter = (role == null || role.isBlank()) ? null : normalizeRole(role);
        if (roleFilter != null && !ASSIGNABLE_ROLES.contains(roleFilter)) {
            throw new AppException(ErrorCode.ROLE_INVALID);
        }

        Page<User> userPage = userRepository.findAll(buildSpec(keyword, roleFilter, status), pageable);
        Map<Long, Set<String>> rolesByUser = loadRoleNames(
                userPage.getContent().stream().map(User::getId).toList());

        List<AdminUserResponse> items = userPage.getContent().stream()
                .map(u -> toResponse(u, rolesByUser.getOrDefault(u.getId(), Set.of())))
                .toList();

        return PageResponse.<AdminUserResponse>builder()
                .items(items)
                .page(safePage)
                .size(safeSize)
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long userId) {
        User user = findActiveUser(userId);
        return toResponse(user, roleNamesOf(user));
    }

    /**
     * Đổi trạng thái tài khoản. Khi chuyển sang INACTIVE/BANNED, mọi token đang dùng của user
     * bị thu hồi ngay để tài khoản bị đăng xuất khỏi mọi thiết bị.
     */
    @Transactional
    public AdminUserResponse updateStatus(Long userId, AdminUpdateUserStatusRequest request) {
        ensureNotSelf(userId);
        User user = findActiveUser(userId);

        UserStatus oldStatus = user.getStatus();
        UserStatus newStatus = request.getStatus();
        if (oldStatus != newStatus) {
            user.setStatus(newStatus);
            userRepository.save(user);
            if (newStatus != UserStatus.ACTIVE) {
                userTokenRevocationService.revokeAllTokens(userId);
            }
            log.info("Admin {} changed status of user {}: {} -> {}",
                    currentUserService.getCurrentUserId(), userId, oldStatus, newStatus);
        }
        return toResponse(user, roleNamesOf(user));
    }

    /**
     * Gán lại role. USER luôn được giữ (mọi tài khoản đều là học viên).
     * Nếu tập role thay đổi, token cũ bị thu hồi để quyền mới có hiệu lực ngay (user phải đăng nhập lại).
     */
    @Transactional
    public AdminUserResponse updateRoles(Long userId, AdminUpdateUserRolesRequest request) {
        ensureNotSelf(userId);
        User user = findActiveUser(userId);

        Set<String> requested = new HashSet<>();
        for (String raw : request.getRoles()) {
            if (raw == null || raw.isBlank()) continue;
            String name = normalizeRole(raw);
            if (!ASSIGNABLE_ROLES.contains(name)) {
                throw new AppException(ErrorCode.ROLE_INVALID);
            }
            requested.add(name);
        }
        requested.add(PredefinedRole.USER_ROLE);

        Set<String> current = roleNamesOf(user);
        if (!current.equals(requested)) {
            Set<Role> newRoles = new HashSet<>();
            for (String name : requested) {
                newRoles.add(roleRepository.findByName(name)
                        .orElseThrow(() -> new AppException(ErrorCode.ROLE_INVALID)));
            }
            user.setRoles(newRoles);
            userRepository.save(user);
            userTokenRevocationService.revokeAllTokens(userId);
            log.info("Admin {} changed roles of user {}: {} -> {}",
                    currentUserService.getCurrentUserId(), userId, current, requested);
        }
        return toResponse(user, requested);
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /** Admin không tự khóa / tự hạ quyền chính mình (tránh mất quyền quản trị). */
    private void ensureNotSelf(Long userId) {
        if (Objects.equals(currentUserService.getCurrentUserId(), userId)) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
        }
    }

    private User findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private static String normalizeRole(String role) {
        String r = role.trim().toUpperCase(Locale.ROOT);
        return r.startsWith("ROLE_") ? r.substring(5) : r;
    }

    private static Set<String> roleNamesOf(User user) {
        if (user.getRoles() == null) return new HashSet<>();
        return user.getRoles().stream().map(Role::getName).collect(Collectors.toCollection(HashSet::new));
    }

    private Map<Long, Set<String>> loadRoleNames(List<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        Map<Long, Set<String>> result = new HashMap<>();
        for (Object[] row : userRepository.findRoleNamesByUserIds(userIds)) {
            result.computeIfAbsent((Long) row[0], k -> new HashSet<>()).add((String) row[1]);
        }
        return result;
    }

    private static Specification<User> buildSpec(String keyword, String role, UserStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("fullName")), pattern)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (role != null) {
                // Dùng EXISTS thay cho JOIN để không nhân bản dòng và count phân trang vẫn đúng
                Subquery<Long> sub = query.subquery(Long.class);
                Root<User> subUser = sub.from(User.class);
                Join<User, Role> subRole = subUser.join("roles");
                sub.select(subUser.get("id"))
                        .where(cb.equal(subUser.get("id"), root.get("id")),
                                cb.equal(subRole.get("name"), role));
                predicates.add(cb.exists(sub));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static AdminUserResponse toResponse(User user, Set<String> roles) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .authProvider(user.getAuthProvider())
                .status(user.getStatus())
                .roles(roles)
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
