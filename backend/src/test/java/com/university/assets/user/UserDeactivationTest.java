package com.university.assets.user;

import com.university.assets.audit.AuditService;
import com.university.assets.auth.AuthService;
import com.university.assets.auth.PasswordResetTokenRepository;
import com.university.assets.auth.dto.AuthDtos.LoginRequest;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AccountStatus;
import com.university.assets.config.AppProperties;
import com.university.assets.department.DepartmentRepository;
import com.university.assets.faculty.FacultyRepository;
import com.university.assets.notification.MailService;
import com.university.assets.notification.NotificationService;
import com.university.assets.role.Role;
import com.university.assets.role.RoleRepository;
import com.university.assets.security.CurrentUser;
import com.university.assets.security.JwtService;
import com.university.assets.security.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDeactivationTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    private User userWithRole(UUID id, String roleName, AccountStatus status) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(id);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("test.user@university.local");
        user.setAccountStatus(status);
        user.setRoles(Set.of(role));
        return user;
    }

    @Test
    void deactivate_rejectsSelfDeactivation() {
        UUID selfId = UUID.randomUUID();

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(selfId);

            assertThatThrownBy(() -> userService.deactivate(selfId))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("your own account");
        }
        verify(refreshTokenRepository, never()).revokeAllForUser(any(), any());
    }

    @Test
    void deactivate_disablesAccountAndRevokesSessions() {
        UUID targetId = UUID.randomUUID();
        User target = userWithRole(targetId, "STUDENT", AccountStatus.ACTIVE);
        when(userRepository.findWithRolesById(targetId)).thenReturn(Optional.of(target));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(UUID.randomUUID());

            var response = userService.deactivate(targetId);

            assertThat(response.accountStatus()).isEqualTo(AccountStatus.DISABLED);
            assertThat(target.getAccountStatus()).isEqualTo(AccountStatus.DISABLED);
        }
        verify(refreshTokenRepository).revokeAllForUser(eq(targetId), any());
        verify(notificationService).notifyUser(eq(targetId), eq("ACCOUNT_DEACTIVATED"),
                anyString(), anyString(), eq("User"), eq(targetId));
    }

    @Test
    void deactivate_superAdminTargetRequiresSuperAdmin() {
        UUID targetId = UUID.randomUUID();
        User target = userWithRole(targetId, "SUPER_ADMIN", AccountStatus.ACTIVE);
        when(userRepository.findWithRolesById(targetId)).thenReturn(Optional.of(target));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(UUID.randomUUID());
            currentUserMock.when(() -> CurrentUser.hasAuthority("ROLE_SUPER_ADMIN")).thenReturn(false);

            assertThatThrownBy(() -> userService.deactivate(targetId))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("super administrator");
        }
        assertThat(target.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(refreshTokenRepository, never()).revokeAllForUser(any(), any());
    }

    @Test
    void activate_reactivatesDisabledAccount() {
        UUID targetId = UUID.randomUUID();
        User target = userWithRole(targetId, "STUDENT", AccountStatus.DISABLED);
        target.setFailedLoginAttempts(3);
        when(userRepository.findWithRolesById(targetId)).thenReturn(Optional.of(target));

        var response = userService.activate(targetId);

        assertThat(response.accountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(target.getFailedLoginAttempts()).isZero();
        verify(notificationService).notifyUser(eq(targetId), eq("ACCOUNT_REACTIVATED"),
                anyString(), anyString(), eq("User"), eq(targetId));
    }

    @Test
    void login_rejectedForDeactivatedUser() {
        AppProperties properties = new AppProperties(null, null, null, null, null, null);
        AuthService authService = new AuthService(
                userRepository, refreshTokenRepository, mock(PasswordResetTokenRepository.class), mock(RoleRepository.class),
                passwordEncoder, mock(JwtService.class), auditService,
                mock(MailService.class), properties);

        User disabled = userWithRole(UUID.randomUUID(), "STUDENT", AccountStatus.DISABLED);
        disabled.setPasswordHash("hash");
        when(userRepository.findByEmailIgnoreCase("test.user@university.local"))
                .thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("test.user@university.local", "whatever", false)))
                .isInstanceOf(DisabledException.class);
    }
}
