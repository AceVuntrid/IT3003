package com.university.assets.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.university.assets.audit.AuditService;
import com.university.assets.auth.dto.AuthDtos.ChangePasswordRequest;
import com.university.assets.auth.dto.AuthDtos.LoginRequest;
import com.university.assets.auth.dto.AuthDtos.StudentRegistrationRequest;
import com.university.assets.auth.dto.AuthDtos.TokenResponse;
import com.university.assets.auth.dto.AuthDtos.UserProfile;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AccountStatus;
import com.university.assets.config.AppProperties;
import com.university.assets.notification.MailService;
import com.university.assets.role.Permission;
import com.university.assets.role.Role;
import com.university.assets.role.RoleRepository;
import com.university.assets.security.JwtService;
import com.university.assets.security.RefreshToken;
import com.university.assets.security.RefreshTokenRepository;
import com.university.assets.security.SecurityUser;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final MailService mailService;
    private final AppProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditService auditService,
                       MailService mailService,
                       AppProperties properties) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.mailService = mailService;
        this.properties = properties;
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            throw new DisabledException("Account disabled");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new LockedException("Account locked");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= properties.security().failedLoginLimit()) {
                user.setLockedUntil(Instant.now().plus(
                        Duration.ofMinutes(properties.security().lockMinutes())));
                user.setFailedLoginAttempts(0);
            }
            auditService.logAuth("LOGIN_FAILED", user.getEmail(), user.getId(), false);
            throw new BadCredentialsException("Invalid credentials");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        auditService.logAuth("LOGIN", user.getEmail(), user.getId(), true);

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse registerStudent(StudentRegistrationRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedIndex = request.studentIndex().trim().toUpperCase();

        if (!request.password().equals(request.confirmPassword())) {
            throw ApiException.badRequest("Passwords do not match");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw ApiException.conflict("A user with this email already exists");
        }
        if (userRepository.existsByUniversityId(normalizedIndex)) {
            throw ApiException.conflict("A user with this index number already exists");
        }

        Role studentRole = roleRepository.findByNameIgnoreCase("STUDENT")
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Student role is not configured"));

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setUniversityId(normalizedIndex);
        user.setStudentIndex(normalizedIndex);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setMustChangePassword(false);
        user.setUserType("STUDENT");
        user.setCourse(request.course().trim());
        user.getRoles().add(studentRole);
        userRepository.save(user);
        auditService.logAuth("STUDENT_REGISTERED", normalizedEmail, user.getId(), true);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenValue) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(sha256(refreshTokenValue))
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        if (!stored.isActive()) {
            throw ApiException.unauthorized("Refresh token expired or revoked");
        }
        // Rotation: revoke the presented token and issue a fresh pair.
        stored.setRevokedAt(Instant.now());
        User user = userRepository.findWithRolesById(stored.getUser().getId())
                .orElseThrow(() -> ApiException.unauthorized("User no longer exists"));
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw ApiException.unauthorized("Account is not active");
        }
        return issueTokens(user);
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        auditService.logAuth("LOGOUT", null, userId, true);
    }

    @Transactional(readOnly = true)
    public UserProfile me(UUID userId) {
        User user = userRepository.findWithRolesById(userId)
                .orElseThrow(() -> ApiException.notFound("User"));
        return toProfile(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
        auditService.logAuth("PASSWORD_CHANGED", user.getEmail(), userId, true);
    }

    @Transactional
    public void forgotPassword(String email) {
        // Always succeed to avoid leaking which emails exist.
        userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
            String token = randomToken();
            PasswordResetToken entity = new PasswordResetToken();
            entity.setUser(user);
            entity.setTokenHash(sha256(token));
            entity.setExpiresAt(Instant.now().plus(Duration.ofHours(1)));
            resetTokenRepository.save(entity);
            String frontend = properties.cors().allowedOrigins().isEmpty()
                    ? "http://localhost:5173" : properties.cors().allowedOrigins().get(0);
            mailService.send(user.getEmail(), "Password reset request",
                    "A password reset was requested for your account.\n\n"
                            + "Open the link below to choose a new password (valid for 1 hour):\n"
                            + frontend + "/reset-password?token=" + token + "\n\n"
                            + "If you did not request this, you can ignore this email.");
            auditService.logAuth("PASSWORD_RESET_REQUESTED", user.getEmail(), user.getId(), true);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken stored = resetTokenRepository.findByTokenHash(sha256(token))
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset token"));
        if (!stored.isUsable()) {
            throw ApiException.badRequest("Invalid or expired reset token");
        }
        stored.setUsedAt(Instant.now());
        User user = stored.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        refreshTokenRepository.revokeAllForUser(user.getId(), Instant.now());
        auditService.logAuth("PASSWORD_RESET", user.getEmail(), user.getId(), true);
    }

    private TokenResponse issueTokens(User user) {
        SecurityUser securityUser = new SecurityUser(user);
        String accessToken = jwtService.createAccessToken(securityUser);

        String refreshValue = randomToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(sha256(refreshValue));
        refreshToken.setExpiresAt(Instant.now().plus(
                Duration.ofDays(properties.jwt().refreshExpiryDays())));
        refreshTokenRepository.save(refreshToken);

        return new TokenResponse(accessToken, refreshValue, toProfile(user));
    }

    private UserProfile toProfile(User user) {
        return new UserProfile(
                user.getId(),
                user.getUniversityId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getFaculty() != null ? user.getFaculty().getName() : null,
                user.getDepartment() != null ? user.getDepartment().getName() : null,
                user.getRoles().stream().map(Role::getName).sorted().toList(),
                user.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .map(Permission::getCode)
                        .distinct().sorted().toList(),
                user.isMustChangePassword()
        );
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
