package com.jp.elearningjp.service.auth;

import com.jp.elearningjp.dto.request.auth.GoogleLoginRequest;
import com.jp.elearningjp.dto.request.auth.IntrospectReq;
import com.jp.elearningjp.dto.request.auth.LoginRequest;
import com.jp.elearningjp.dto.request.auth.LogoutRequest;
import com.jp.elearningjp.dto.request.auth.RefreshRequest;
import com.jp.elearningjp.dto.request.user.RegisterRequest;
import com.jp.elearningjp.dto.response.auth.AuthResponse;
import com.jp.elearningjp.dto.response.auth.LoginResponse;
import com.jp.elearningjp.dto.response.user.RegisterResponse;
import com.jp.elearningjp.entity.user.Role;
import com.jp.elearningjp.entity.user.User;
import com.jp.elearningjp.exception.AppException;
import com.jp.elearningjp.exception.ErrorCode;
import com.jp.elearningjp.mapper.UserMapper;
import com.jp.elearningjp.repository.user.RoleRepository;
import com.jp.elearningjp.repository.user.UserRepository;
import com.jp.elearningjp.service.redis.TokenBlacklistService;
import com.jp.elearningjp.shared.constants.PredefinedRole;
import com.jp.elearningjp.entity.user.UserProfile;
import com.jp.elearningjp.repository.user.UserProfileRepository;
import com.jp.elearningjp.shared.enums.AuthProvider;
import com.jp.elearningjp.shared.enums.JlptLevel;
import com.jp.elearningjp.shared.enums.LearnerLevel;
import com.jp.elearningjp.shared.enums.UserStatus;
import com.jp.elearningjp.shared.response.IntrospectResponse;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;

    @Value("${app.google.client-id:}")
    String googleClientId;
    @Value("${jwt.signerKey}")
    String signerKey;

    @Value("${jwt.valid-duration:3600}")        // mặc định 1 giờ
    long validDuration;

    @Value("${jwt.refreshable-duration:86400}") // mặc định 24 giờ
    long refreshableDuration;

    // =========================================================
    // ĐĂNG KÝ
    // =========================================================
    public RegisterResponse registerUser(RegisterRequest registerRequest) {
        // Kiểm tra username đã tồn tại chưa
        if (userRepo.existsByEmail((registerRequest.getEmail()))) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if(!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())){
            throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
        }
        User userEntity = userMapper.toUserEntity(registerRequest);
        userEntity.setPassword(passwordEncoder.encode(userEntity.getPassword()));
        Role role = roleRepository
                .findByName(PredefinedRole.USER_ROLE)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        userEntity.setRoles(Set.of(role));
        userRepo.save(userEntity);

        return RegisterResponse.builder()
                .userInfoResponse(userMapper.toUserInfoResponse(userEntity))
                .token(generateToken(userEntity))
                .build();
    }

    // =========================================================
    // ĐĂNG NHẬP
    // =========================================================
    public LoginResponse loginUser(LoginRequest loginRequest) {
        User userEntity = userRepo.findUserByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(loginRequest.getPassword(), userEntity.getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_INVALID);
        }
        if(!userEntity.getStatus() .equals(UserStatus.ACTIVE)) {
            throw new AppException(ErrorCode.USER_BANNED);
        }
        return LoginResponse.builder()
                .success(true)
                .accessToken(generateToken(userEntity))
                .build();
    }

    // =========================================================
    // ĐĂNG NHẬP BẰNG GOOGLE (OAUTH2 ID TOKEN)
    // =========================================================
    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        Map<String, Object> tokenInfo;
        try {
            RestClient restClient = RestClient.create();
            tokenInfo = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token=" + request.getIdToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("Google token verification failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (tokenInfo == null || !"true".equals(String.valueOf(tokenInfo.get("email_verified")))) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (googleClientId != null && !googleClientId.isBlank() && !googleClientId.contains("your-google-client-id")) {
            String aud = (String) tokenInfo.get("aud");
            if (!googleClientId.equals(aud)) {
                log.warn("Google token audience mismatch: expected={}, actual={}", googleClientId, aud);
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }
        }

        String email = (String) tokenInfo.get("email");
        String googleId = (String) tokenInfo.get("sub");
        String name = (String) tokenInfo.get("name");
        String picture = (String) tokenInfo.get("picture");

        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        User user = userRepo.findUserByEmail(email)
                .map(existingUser -> {
                    if (existingUser.getStatus() == UserStatus.BANNED) {
                        throw new AppException(ErrorCode.USER_BANNED);
                    }
                    if (existingUser.getAuthProvider() == null || existingUser.getAuthProvider() == AuthProvider.LOCAL) {
                        existingUser.setAuthProvider(AuthProvider.GOOGLE);
                    }
                    if (existingUser.getProviderId() == null || existingUser.getProviderId().isBlank()) {
                        existingUser.setProviderId(googleId);
                    }
                    if ((existingUser.getAvatarUrl() == null || existingUser.getAvatarUrl().isBlank()) && picture != null) {
                        existingUser.setAvatarUrl(picture);
                    }
                    return userRepo.save(existingUser);
                })
                .orElseGet(() -> {
                    Role role = roleRepository.findByName(PredefinedRole.USER_ROLE)
                            .orElseThrow(() -> new RuntimeException("Role not found"));

                    String displayName = (name != null && !name.isBlank()) ? name : email.split("@")[0];

                    User newUser = User.builder()
                            .email(email)
                            .fullName(displayName)
                            .avatarUrl(picture)
                            .authProvider(AuthProvider.GOOGLE)
                            .providerId(googleId)
                            .status(UserStatus.ACTIVE)
                            .emailVerifiedAt(Instant.now())
                            .roles(Set.of(role))
                            .build();

                    User savedUser = userRepo.save(newUser);

                    UserProfile profile = UserProfile.builder()
                            .user(savedUser)
                            .currentLevel(LearnerLevel.BEGINNER)
                            .targetLevel(JlptLevel.N5)
                            .dailyStudyTimeMinutes(30)
                            .timezone("Asia/Ho_Chi_Minh")
                            .build();
                    userProfileRepository.save(profile);

                    log.info("Created new user via Google login: email={}, id={}", email, savedUser.getId());
                    return savedUser;
                });

        return LoginResponse.builder()
                .success(true)
                .accessToken(generateToken(user))
                .build();
    }

    // =========================================================
    // ĐĂNG XUẤT
    // =========================================================
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            SignedJWT signedJWT = verifyToken(request.getToken(), false);

            String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            // Lưu vào Redis blacklist — tự xóa khi token hết hạn
            tokenBlacklistService.blacklist(jwtId, expiryTime);

        } catch (AppException e) {
            // Token đã hết hạn rồi — không cần làm gì
            log.info("Token already expired, logout skipped.");
        }
    }

    // =========================================================
    // REFRESH TOKEN
    // =========================================================
    public AuthResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        // Verify với refreshable duration (dài hơn valid duration)
        SignedJWT signedJWT = verifyToken(request.getToken(), true);

        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

        // Vô hiệu hóa token cũ
        tokenBlacklistService.blacklist(jwtId, expiryTime);

        // Tìm user và cấp token mới
        String email = signedJWT.getJWTClaimsSet().getSubject();
        User userEntity = userRepo.findUserByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return AuthResponse.builder()
                .token(generateToken(userEntity))
                .build();
    }

    // =========================================================
    // INTROSPECT
    // =========================================================
    public IntrospectResponse introspect(IntrospectReq introspectReq) {
        boolean isValid = true;
        try {
            verifyToken(introspectReq.getToken(), false);
        } catch (AppException | JOSEException | ParseException e) {
            isValid = false;
        }
        return IntrospectResponse.builder().valid(isValid).build();
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /**
     * Verify token — dùng chung cho introspect, logout, refresh
     * @param isRefresh true → dùng refreshableDuration thay vì validDuration
     */
    public SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(getSignerKey());
        SignedJWT signedJWT = SignedJWT.parse(token);

        // Nếu là refresh → cho phép token đã hết hạn bình thường nhưng vẫn trong refreshable window
        Instant issueTime = signedJWT.getJWTClaimsSet().getIssueTime().toInstant();
        Date expiryTime = isRefresh
                ? Date.from(issueTime.plus(refreshableDuration, ChronoUnit.SECONDS))
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        boolean verified = signedJWT.verify(verifier);

        if (!verified || expiryTime.before(new Date()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        // Kiểm tra blacklist (Redis)
        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        if (tokenBlacklistService.isBlacklisted(jwtId))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    public String generateToken(User user) {
        Instant now = Instant.now();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issuer("E-Learning JP Service")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(validDuration, ChronoUnit.SECONDS)))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .claim("userId", user.getId())
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);

        try {
            signedJWT.sign(new MACSigner(getSignerKey()));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    private byte[] getSignerKey() {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(signerKey);
        } catch (IllegalArgumentException e) {
            keyBytes = signerKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 64) {
            return Arrays.copyOf(keyBytes, 64);
        }
        return keyBytes;
    }

    private String buildScope(User user) {
        StringJoiner scopeJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> scopeJoiner.add("ROLE_" + role.getName()));
        }
        return scopeJoiner.toString();
    }
}