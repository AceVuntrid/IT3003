package com.university.assets.user;

import com.university.assets.common.model.Enums.AccountStatus;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.common.response.PageResponse;
import com.university.assets.user.dto.UserDtos.CreateUserRequest;
import com.university.assets.user.dto.UserDtos.UpdateUserRequest;
import com.university.assets.user.dto.UserDtos.UserResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ApiResponse<PageResponse<UserResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID facultyId,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) Boolean includeInactive,
            @ParameterObject @PageableDefault(size = 20, sort = "lastName", direction = Sort.Direction.ASC)
            Pageable pageable) {
        return ApiResponse.ok(userService.list(
                search, facultyId, departmentId, role, status, includeInactive, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ApiResponse<UserResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(userService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok("User created successfully", userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ApiResponse<UserResponse> update(@PathVariable UUID id,
                                            @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok("User updated successfully", userService.update(id, request));
    }

    // Account status changes go exclusively through deactivate/activate below so the
    // self-deactivation and SUPER_ADMIN-target guards can never be bypassed.

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('USER_DEACTIVATE')")
    public ApiResponse<UserResponse> deactivate(@PathVariable UUID id) {
        return ApiResponse.ok("User deactivated", userService.deactivate(id));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('USER_DEACTIVATE')")
    public ApiResponse<UserResponse> activate(@PathVariable UUID id) {
        return ApiResponse.ok("User activated", userService.activate(id));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ApiResponse<Map<String, String>> resetPassword(@PathVariable UUID id) {
        String temporary = userService.resetPassword(id);
        return ApiResponse.ok("Temporary password generated",
                Map.of("temporaryPassword", temporary));
    }
}
