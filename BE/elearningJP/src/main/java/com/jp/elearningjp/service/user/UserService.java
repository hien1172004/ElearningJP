package com.jp.elearningjp.service.user;

import com.jp.elearningjp.dto.request.user.UpdateProfileRequest;
import com.jp.elearningjp.dto.response.curriculum.CloudinaryUploadResponse;
import com.jp.elearningjp.dto.response.user.ChangePasswordRequest;
import com.jp.elearningjp.dto.response.user.UserProfileResponse;
import com.jp.elearningjp.entity.user.Role;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.entity.user.UserProfile;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.repository.user.UserProfileRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.cloudinary.CloudinaryService;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.LearnerLevel;
import com.jp.elearningjp.shared.util.CurrentUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {

    UserRepository userRepository;
    UserProfileRepository userProfileRepository;
    CurrentUserService currentUserService;
    CloudinaryService cloudinaryService;
    PasswordEncoder passwordEncoder;

    /**
     * Lấy thông tin cá nhân & Profile học tập của user hiện tại.
     */
    @Transactional
    public UserProfileResponse getMyProfile() {
        User user = currentUserService.getCurrentUser();
        UserProfile profile = getOrCreateProfile(user);
        return toUserProfileResponse(user, profile);
    }

    /**
     * Cập nhật thông tin profile cá nhân và mục tiêu học tập.
     */
    @Transactional
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {
        User user = currentUserService.getCurrentUser();
        UserProfile profile = getOrCreateProfile(user);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
            userRepository.save(user);
        }

        if (request.getPhoneNumber() != null) {
            profile.setPhoneNumber(request.getPhoneNumber().trim().isEmpty() ? null : request.getPhoneNumber().trim());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio().trim());
        }
        if (request.getCurrentLevel() != null) {
            profile.setCurrentLevel(request.getCurrentLevel());
        }
        if (request.getTargetLevel() != null) {
            profile.setTargetLevel(request.getTargetLevel());
        }
        if (request.getTargetExamDate() != null) {
            profile.setTargetExamDate(request.getTargetExamDate());
        }
        if (request.getDailyStudyTimeMinutes() != null) {
            profile.setDailyStudyTimeMinutes(request.getDailyStudyTimeMinutes());
        }

        profile = userProfileRepository.save(profile);
        log.info("User profile updated for userId={}", user.getId());
        return toUserProfileResponse(user, profile);
    }

    /**
     * Tải lên và thay đổi ảnh đại diện cá nhân (Avatar) qua Cloudinary.
     */
    @Transactional
    public String changeAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new AppException(ErrorCode.UNCAUGHT_EXCEPTION);
        }

        User user = currentUserService.getCurrentUser();
        CloudinaryUploadResponse uploadResponse = cloudinaryService.upload(
                file,
                "elearningjp/avatars",
                "avatar-user-" + user.getId()
        );

        user.setAvatarUrl(uploadResponse.getSecureUrl());
        userRepository.save(user);

        log.info("Avatar updated for userId={}, newUrl={}", user.getId(), uploadResponse.getSecureUrl());
        return uploadResponse.getSecureUrl();
    }

    /**
     * Đổi mật khẩu cho người dùng đang đăng nhập.
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }

        User user = currentUserService.getCurrentUser();

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.SAME_OLD_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed successfully for userId={}", user.getId());
    }

    /**
     * Lấy UserProfile hoặc tự động tạo mới nếu tài khoản chưa có bản ghi profile.
     */
    private UserProfile getOrCreateProfile(User user) {
        return userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserProfile newProfile = UserProfile.builder()
                            .user(user)
                            .currentLevel(LearnerLevel.BEGINNER)
                            .targetLevel(JlptLevel.N5)
                            .dailyStudyTimeMinutes(30)
                            .timezone("Asia/Ho_Chi_Minh")
                            .build();
                    return userProfileRepository.save(newProfile);
                });
    }

    private UserProfileResponse toUserProfileResponse(User user, UserProfile profile) {
        Set<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet())
                : Collections.emptySet();

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .roles(roleNames)
                .status(user.getStatus())
                .phoneNumber(profile.getPhoneNumber())
                .bio(profile.getBio())
                .currentLevel(profile.getCurrentLevel())
                .targetLevel(profile.getTargetLevel())
                .targetExamDate(profile.getTargetExamDate())
                .dailyStudyTimeMinutes(profile.getDailyStudyTimeMinutes())
                .timezone(profile.getTimezone())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
