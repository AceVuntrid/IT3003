package com.university.assets.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.university.assets.auth.dto.AuthDtos.ChangePasswordRequest;
import com.university.assets.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.university.assets.auth.dto.AuthDtos.LoginRequest;
import com.university.assets.auth.dto.AuthDtos.RefreshRequest;
import com.university.assets.auth.dto.AuthDtos.ResetPasswordRequest;
import com.university.assets.auth.dto.AuthDtos.StudentRegistrationRequest;
import com.university.assets.auth.dto.AuthDtos.TokenResponse;
import com.university.assets.auth.dto.AuthDtos.UserProfile;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.security.CurrentUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Signed in successfully", authService.login(request));
    }

    @PostMapping("/register/student")
    public ApiResponse<TokenResponse> registerStudent(@Valid @RequestBody StudentRegistrationRequest request) {
        return ApiResponse.ok("Student account created successfully", authService.registerStudent(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout(CurrentUser.id());
        return ApiResponse.message("Signed out successfully");
    }

    @GetMapping("/me")
    public ApiResponse<UserProfile> me() {
        return ApiResponse.ok(authService.me(CurrentUser.id()));
    }

    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(CurrentUser.id(), request);
        return ApiResponse.message("Password changed successfully");
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ApiResponse.message("If the email exists, a reset link has been sent");
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.message("Password has been reset. You can now sign in.");
    }
}
