package com.jp.elearningjp.service.user;

import com.jp.elearningjp.dto.request.user.AdminCreateUserRequest;
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
import com.jp.elearningjp.shared.enums.UserStatus;
import com.jp.elearningjp.shared.response.PageResponse;
import com.jp.elearningjp.shared.util.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminUserServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long TARGET_ID = 2L;

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock CurrentUserService currentUserService;
    @Mock UserTokenRevocationService userTokenRevocationService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks AdminUserService service;

    private final Role userRole = role("USER");
    private final Role teacherRole = role("TEACHER");
    private final Role adminRole = role("ADMIN");

    @BeforeEach
    void setUp() {
        when(currentUserService.getCurrentUserId()).thenReturn(ADMIN_ID);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.of(teacherRole));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
    }

    // ============================================================
    // updateStatus
    // ============================================================

    @Test
    void banUser_changesStatus_andRevokesTokens() {
        User target = user(TARGET_ID, UserStatus.ACTIVE, userRole);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        AdminUserResponse res = service.updateStatus(TARGET_ID, statusReq(UserStatus.BANNED));

        assertThat(res.getStatus()).isEqualTo(UserStatus.BANNED);
        assertThat(target.getStatus()).isEqualTo(UserStatus.BANNED);
        verify(userRepository).save(target);
        verify(userTokenRevocationService).revokeAllTokens(TARGET_ID);
    }

    @Test
    void unbanUser_doesNotRevokeTokens() {
        User target = user(TARGET_ID, UserStatus.BANNED, userRole);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        service.updateStatus(TARGET_ID, statusReq(UserStatus.ACTIVE));

        assertThat(target.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userTokenRevocationService, never()).revokeAllTokens(anyLong());
    }

    @Test
    void sameStatus_isNoOp() {
        User target = user(TARGET_ID, UserStatus.ACTIVE, userRole);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        service.updateStatus(TARGET_ID, statusReq(UserStatus.ACTIVE));

        verify(userRepository, never()).save(any());
        verify(userTokenRevocationService, never()).revokeAllTokens(anyLong());
    }

    @Test
    void updateStatus_ownAccount_throws() {
        assertThatThrownBy(() -> service.updateStatus(ADMIN_ID, statusReq(UserStatus.BANNED)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateStatus_userNotFound_throws() {
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(TARGET_ID, statusReq(UserStatus.BANNED)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateStatus_deletedUser_treatedAsNotFound() {
        User target = user(TARGET_ID, UserStatus.ACTIVE, userRole);
        target.softDelete();
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.updateStatus(TARGET_ID, statusReq(UserStatus.BANNED)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // ============================================================
    // updateRoles
    // ============================================================

    @Test
    void promoteToTeacher_keepsUserRole_andRevokesTokens() {
        User target = user(TARGET_ID, UserStatus.ACTIVE, userRole);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        AdminUserResponse res = service.updateRoles(TARGET_ID, rolesReq("teacher"));

        assertThat(res.getRoles()).containsExactlyInAnyOrder("USER", "TEACHER");
        assertThat(target.getRoles()).containsExactlyInAnyOrder(userRole, teacherRole);
        verify(userTokenRevocationService).revokeAllTokens(TARGET_ID);
    }

    @Test
    void emptyRoles_demotesToPlainUser() {
        User target = user(TARGET_ID, UserStatus.ACTIVE, userRole, teacherRole, adminRole);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        AdminUserResponse res = service.updateRoles(TARGET_ID, rolesReq());

        assertThat(res.getRoles()).containsExactly("USER");
        verify(userTokenRevocationService).revokeAllTokens(TARGET_ID);
    }

    @Test
    void unchangedRoles_isNoOp() {
        User target = user(TARGET_ID, UserStatus.ACTIVE, userRole, teacherRole);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        service.updateRoles(TARGET_ID, rolesReq("ROLE_TEACHER", "USER"));

        verify(userRepository, never()).save(any());
        verify(userTokenRevocationService, never()).revokeAllTokens(anyLong());
    }

    @Test
    void unknownRole_throwsRoleInvalid() {
        User target = user(TARGET_ID, UserStatus.ACTIVE, userRole);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> service.updateRoles(TARGET_ID, rolesReq("SUPERADMIN")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROLE_INVALID);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateRoles_ownAccount_throws() {
        assertThatThrownBy(() -> service.updateRoles(ADMIN_ID, rolesReq()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
    }

    @Test
    void updateRoles_roleMissingInDb_throwsRoleInvalid() {
        User target = user(TARGET_ID, UserStatus.ACTIVE, userRole);
        when(userRepository.findById(TARGET_ID)).thenReturn(Optional.of(target));
        when(roleRepository.findByName("TEACHER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateRoles(TARGET_ID, rolesReq("TEACHER")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROLE_INVALID);
    }

    // ============================================================
    // getUsers
    // ============================================================

    @Test
    @SuppressWarnings("unchecked")
    void getUsers_mapsRolesInBatch_andClampsPaging() {
        User u = user(TARGET_ID, UserStatus.ACTIVE);
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> new PageImpl<>(List.of(u), inv.getArgument(1), 1));
        when(userRepository.findRoleNamesByUserIds(List.of(TARGET_ID)))
                .thenReturn(List.of(new Object[]{TARGET_ID, "USER"}, new Object[]{TARGET_ID, "TEACHER"}));

        PageResponse<AdminUserResponse> res = service.getUsers(" a@x ", "teacher", UserStatus.ACTIVE, 0, 500);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue()).extracting(Pageable::getPageNumber, Pageable::getPageSize)
                .containsExactly(0, AdminUserService.MAX_PAGE_SIZE);
        assertThat(res.getPage()).isEqualTo(1);
        assertThat(res.getItems()).hasSize(1);
        assertThat(res.getItems().get(0).getRoles()).containsExactlyInAnyOrder("USER", "TEACHER");
    }

    @Test
    void getUsers_invalidRoleFilter_throws() {
        assertThatThrownBy(() -> service.getUsers(null, "HACKER", null, 1, 20))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROLE_INVALID);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getUsers_emptyPage_skipsRoleQuery() {
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PageResponse<AdminUserResponse> res = service.getUsers(null, null, null, 1, 20);

        assertThat(res.getItems()).isEmpty();
        verify(userRepository, never()).findRoleNamesByUserIds(any());
    }

    // ============================================================
    // Helpers
    // ============================================================

    private static Role role(String name) {
        Role r = Role.builder().name(name).build();
        r.setId((long) name.hashCode());
        return r;
    }

    private static User user(Long id, UserStatus status, Role... roles) {
        User u = User.builder()
                .email("u" + id + "@test.com")
                .fullName("User " + id)
                .status(status)
                .roles(new HashSet<>(Set.of(roles)))
                .build();
        u.setId(id);
        return u;
    }

    private static AdminUpdateUserStatusRequest statusReq(UserStatus status) {
        return AdminUpdateUserStatusRequest.builder().status(status).build();
    }

    private static AdminUpdateUserRolesRequest rolesReq(String... roles) {
        return AdminUpdateUserRolesRequest.builder().roles(new HashSet<>(Set.of(roles))).build();
    }

    // ============================================================
    // createUser — cấp phát tài khoản giáo viên / quản trị viên
    // ============================================================

    private static AdminCreateUserRequest createReq(String role) {
        return AdminCreateUserRequest.builder()
                .email("  gv@test.local ").fullName(" Giáo viên A ").password("matkhau123").role(role).build();
    }

    @Test
    void createUser_teacher_savesActiveVerifiedAccount_withSingleTeacherRole() {
        when(userRepository.existsByEmail("gv@test.local")).thenReturn(false);
        when(passwordEncoder.encode("matkhau123")).thenReturn("HASHED");

        AdminUserResponse res = service.createUser(createReq("teacher"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User u = saved.getValue();
        assertThat(u.getEmail()).isEqualTo("gv@test.local");
        assertThat(u.getFullName()).isEqualTo("Giáo viên A");
        assertThat(u.getPassword()).isEqualTo("HASHED");
        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(u.getEmailVerifiedAt()).isNotNull();
        // Mỗi tài khoản chỉ có 1 role — không gán kèm USER
        assertThat(u.getRoles()).containsExactly(teacherRole);
        assertThat(res.getRoles()).containsExactly("TEACHER");
    }

    @Test
    void createUser_emailExists_throwsUserExisted_andDoesNotSave() {
        when(userRepository.existsByEmail("gv@test.local")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(createReq("ADMIN")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.USER_EXISTED);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_plainUserRole_throwsRoleInvalid() {
        when(userRepository.existsByEmail("gv@test.local")).thenReturn(false);

        assertThatThrownBy(() -> service.createUser(createReq("USER")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROLE_INVALID);
        verify(userRepository, never()).save(any());
    }
}
