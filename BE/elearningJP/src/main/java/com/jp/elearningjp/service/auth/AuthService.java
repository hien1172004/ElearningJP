package com.jp.elearningjp.service.auth;

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
        return Base64.getDecoder().decode(signerKey);
    }

    private String buildScope(User user) {
        StringJoiner scopeJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(user.getRoles())) {
            user.getRoles().forEach(role -> scopeJoiner.add("ROLE_" + role.getName()));
        }
        return scopeJoiner.toString();
    }
}