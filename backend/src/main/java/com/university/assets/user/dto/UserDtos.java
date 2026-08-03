package com.university.assets.user.dto;

import com.university.assets.common.model.Enums.AccountStatus;
import com.university.assets.user.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class UserDtos {

    private UserDtos() {}

    public record CreateUserRequest(
            @NotBlank(message = "First name is required") String firstName,
            @NotBlank(message = "Last name is required") String lastName,
            @NotBlank(message = "University ID is required") String universityId,
            @NotBlank(message = "Email is required") @Email(message = "Invalid email") String email,
            String phone,
            String userType,
            UUID facultyId,
            UUID departmentId,
            @NotEmpty(message = "At least one role is required") List<UUID> roleIds,
            @NotBlank(message = "Temporary password is required") String temporaryPassword,
            Boolean mustChangePassword,
            Integer reservationLimit,
            Boolean externalBorrowingAllowed
    ) {}

    public record UpdateUserRequest(
            @NotBlank(message = "First name is required") String firstName,
            @NotBlank(message = "Last name is required") String lastName,
            String phone,
            String userType,
            UUID facultyId,
            UUID departmentId,
            List<UUID> roleIds,
            Integer reservationLimit,
            Boolean externalBorrowingAllowed
    ) {}

    public record UserResponse(
            UUID id,
            String universityId,
            String firstName,
            String lastName,
            String fullName,
            String email,
            String phone,
            String userType,
            UUID facultyId,
            String facultyName,
            UUID departmentId,
            String departmentName,
            List<String> roles,
            AccountStatus accountStatus,
            Instant lastLoginAt,
            boolean mustChangePassword,
            Integer reservationLimit,
            boolean externalBorrowingAllowed,
            Instant createdAt
    ) {
        public static UserResponse from(User u) {
            return new UserResponse(
                    u.getId(), u.getUniversityId(), u.getFirstName(), u.getLastName(), u.getFullName(),
                    u.getEmail(), u.getPhone(), u.getUserType(),
                    u.getFaculty() != null ? u.getFaculty().getId() : null,
                    u.getFaculty() != null ? u.getFaculty().getName() : null,
                    u.getDepartment() != null ? u.getDepartment().getId() : null,
                    u.getDepartment() != null ? u.getDepartment().getName() : null,
                    u.getRoles().stream().map(r -> r.getName()).sorted().toList(),
                    u.getAccountStatus(), u.getLastLoginAt(), u.isMustChangePassword(),
                    u.getReservationLimit(), u.isExternalBorrowingAllowed(), u.getCreatedAt());
        }
    }
}
