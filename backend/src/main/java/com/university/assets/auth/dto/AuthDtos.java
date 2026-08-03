package com.university.assets.auth.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class AuthDtos {

    private AuthDtos() {}

    public static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";
    public static final String PASSWORD_MESSAGE =
            "Password must be at least 8 characters and contain uppercase, lowercase, a number and a special character";
    public static final String UNIVERSITY_EMAIL_PATTERN =
            "^\\d{4}s20\\d{2,5}@stu\\.cmb\\.ac\\.lk$";
    public static final String STUDENT_INDEX_PATTERN = "^S\\d{5}$";
    public static final String STUDENT_COURSE_PATTERN =
            "^(Physical Science|Biological Science|ISMF|Molecular Biology|MIT)$";

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password,
            boolean rememberMe
    ) {}

    public record TokenResponse(String accessToken, String refreshToken, UserProfile user) {}

    public record RefreshRequest(@NotBlank(message = "Refresh token is required") String refreshToken) {}

    public record UserProfile(
            UUID id,
            String universityId,
            String firstName,
            String lastName,
            String email,
            String phone,
            String facultyName,
            String departmentName,
            List<String> roles,
            List<String> permissions,
            boolean mustChangePassword
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email is required") @Email(message = "Invalid email") String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Token is required") String token,
            @NotBlank(message = "Password is required")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String newPassword
    ) {}

    public record ChangePasswordRequest(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank(message = "New password is required")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String newPassword
    ) {}

    public record StudentRegistrationRequest(
            @NotBlank(message = "First name is required") String firstName,
            @NotBlank(message = "Last name is required") String lastName,
            @NotBlank(message = "University email is required")
            @Email(message = "Invalid email")
            @Pattern(regexp = UNIVERSITY_EMAIL_PATTERN, message = "University email must match the format 2023s20133@stu.cmb.ac.lk")
            String email,
            @NotBlank(message = "Index number is required")
            @Pattern(regexp = STUDENT_INDEX_PATTERN, message = "Index number must start with S followed by 5 digits")
            String studentIndex,
            @NotBlank(message = "Course is required")
            @Pattern(regexp = STUDENT_COURSE_PATTERN, message = "Select a valid course")
            String course,
            @NotBlank(message = "Password is required")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String password,
            @NotBlank(message = "Confirm password is required") String confirmPassword
    ) {}
}
