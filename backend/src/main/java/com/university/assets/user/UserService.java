package com.university.assets.user;

import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AccountStatus;
import com.university.assets.common.response.PageResponse;
import com.university.assets.department.DepartmentRepository;
import com.university.assets.faculty.FacultyRepository;
import com.university.assets.notification.NotificationService;
import com.university.assets.role.Role;
import com.university.assets.role.RoleRepository;
import com.university.assets.security.CurrentUser;
import com.university.assets.security.RefreshTokenRepository;
import com.university.assets.user.dto.UserDtos.CreateUserRequest;
import com.university.assets.user.dto.UserDtos.UpdateUserRequest;
import com.university.assets.user.dto.UserDtos.UserResponse;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       FacultyRepository facultyRepository, DepartmentRepository departmentRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, AuditService auditService,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String search, UUID facultyId, UUID departmentId,
                                           String role, AccountStatus status,
                                           Boolean includeInactive, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase().trim() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("universityId")), like)));
            }
            if (facultyId != null) {
                predicates.add(cb.equal(root.get("faculty").get("id"), facultyId));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("accountStatus"), status));
            } else if (!Boolean.TRUE.equals(includeInactive)) {
                // Pickers and directories only see active accounts by default;
                // administration screens pass includeInactive=true (or a status filter).
                predicates.add(cb.equal(root.get("accountStatus"), AccountStatus.ACTIVE));
            }
            if (role != null && !role.isBlank()) {
                predicates.add(cb.equal(root.join("roles").get("name"), role));
                if (query != null) {
                    query.distinct(true);
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Page<User> page = userRepository.findAll(spec, pageable);
        return PageResponse.from(page, UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return UserResponse.from(userRepository.findWithRolesById(id)
                .orElseThrow(() -> ApiException.notFound("User")));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("A user with this email already exists");
        }
        if (userRepository.existsByUniversityId(request.universityId())) {
            throw ApiException.conflict("A user with this university ID already exists");
        }
        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setUniversityId(request.universityId().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(request.phone());
        user.setUserType(request.userType());
        user.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        user.setMustChangePassword(request.mustChangePassword() == null || request.mustChangePassword());
        user.setReservationLimit(request.reservationLimit());
        user.setExternalBorrowingAllowed(Boolean.TRUE.equals(request.externalBorrowingAllowed()));
        applyOrgAndRoles(user, request.facultyId(), request.departmentId(), request.roleIds());
        userRepository.save(user);
        auditService.log("CREATE", "USER", "User", user.getId(), null,
                Map.of("email", user.getEmail(), "universityId", user.getUniversityId()));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> ApiException.notFound("User"));
        Map<String, Object> old = Map.of(
                "firstName", user.getFirstName(), "lastName", user.getLastName(),
                "roles", user.getRoles().stream().map(Role::getName).sorted().toList());
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone());
        user.setUserType(request.userType());
        user.setReservationLimit(request.reservationLimit());
        if (request.externalBorrowingAllowed() != null) {
            user.setExternalBorrowingAllowed(request.externalBorrowingAllowed());
        }
        applyOrgAndRoles(user, request.facultyId(), request.departmentId(), request.roleIds());
        auditService.log("UPDATE", "USER", "User", user.getId(), old, Map.of(
                "firstName", user.getFirstName(), "lastName", user.getLastName(),
                "roles", user.getRoles().stream().map(Role::getName).sorted().toList()));
        return UserResponse.from(user);
    }

    /** Soft delete: disables the account, revokes sessions and blocks future logins. */
    @Transactional
    public UserResponse deactivate(UUID id) {
        if (id.equals(CurrentUser.id())) {
            throw ApiException.badRequest("You cannot deactivate your own account");
        }
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> ApiException.notFound("User"));
        if (user.getAccountStatus() == AccountStatus.DISABLED) {
            throw ApiException.badRequest("This account is already deactivated");
        }
        boolean targetIsSuperAdmin = user.getRoles().stream()
                .anyMatch(r -> "SUPER_ADMIN".equals(r.getName()));
        if (targetIsSuperAdmin && !CurrentUser.hasAuthority("ROLE_SUPER_ADMIN")) {
            throw ApiException.forbidden(
                    "Only a super administrator can deactivate a super administrator account");
        }
        AccountStatus old = user.getAccountStatus();
        user.setAccountStatus(AccountStatus.DISABLED);
        refreshTokenRepository.revokeAllForUser(id, Instant.now());
        auditService.log("DEACTIVATE", "USER", "User", user.getId(),
                Map.of("status", old.name()),
                Map.of("status", AccountStatus.DISABLED.name(), "email", user.getEmail()));
        notificationService.notifyUser(user.getId(), "ACCOUNT_DEACTIVATED",
                "Your account has been deactivated",
                "Your university asset management account has been deactivated. "
                        + "Contact an administrator if you believe this is a mistake.",
                "User", user.getId());
        return UserResponse.from(user);
    }

    /** Re-enables a previously deactivated account. */
    @Transactional
    public UserResponse activate(UUID id) {
        User user = userRepository.findWithRolesById(id)
                .orElseThrow(() -> ApiException.notFound("User"));
        if (user.getAccountStatus() == AccountStatus.ACTIVE) {
            throw ApiException.badRequest("This account is already active");
        }
        boolean targetIsSuperAdmin = user.getRoles().stream()
                .anyMatch(r -> "SUPER_ADMIN".equals(r.getName()));
        if (targetIsSuperAdmin && !CurrentUser.hasAuthority("ROLE_SUPER_ADMIN")) {
            throw ApiException.forbidden("Only a super administrator can reactivate a super administrator account");
        }
        AccountStatus old = user.getAccountStatus();
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        auditService.log("ACTIVATE", "USER", "User", user.getId(),
                Map.of("status", old.name()),
                Map.of("status", AccountStatus.ACTIVE.name(), "email", user.getEmail()));
        notificationService.notifyUser(user.getId(), "ACCOUNT_REACTIVATED",
                "Your account has been reactivated",
                "Your university asset management account is active again. You can sign in as usual.",
                "User", user.getId());
        return UserResponse.from(user);
    }

    @Transactional
    public String resetPassword(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        String temporary = "Temp@" + UUID.randomUUID().toString().substring(0, 8);
        user.setPasswordHash(passwordEncoder.encode(temporary));
        user.setMustChangePassword(true);
        refreshTokenRepository.revokeAllForUser(id, Instant.now());
        auditService.log("ADMIN_PASSWORD_RESET", "USER", "User", user.getId(), null,
                Map.of("email", user.getEmail()));
        return temporary;
    }

    private void applyOrgAndRoles(User user, UUID facultyId, UUID departmentId, List<UUID> roleIds) {
        user.setFaculty(facultyId == null ? null
                : facultyRepository.findById(facultyId)
                .orElseThrow(() -> ApiException.notFound("Faculty")));
        user.setDepartment(departmentId == null ? null
                : departmentRepository.findById(departmentId)
                .orElseThrow(() -> ApiException.notFound("Department")));
        if (user.getDepartment() != null && user.getFaculty() != null
                && !user.getDepartment().getFaculty().getId().equals(user.getFaculty().getId())) {
            throw ApiException.badRequest("Department does not belong to the selected faculty");
        }
        if (roleIds != null) {
            List<Role> roles = roleRepository.findAllById(roleIds);
            if (roles.size() != new HashSet<>(roleIds).size()) {
                throw ApiException.badRequest("One or more roles do not exist");
            }
            user.setRoles(new HashSet<>(roles));
        }
    }
}
