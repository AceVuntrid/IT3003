package com.university.assets.role;

import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles and Permissions")
public class RoleController {

    public record RoleRequest(
            @NotBlank(message = "Role name is required") String name,
            String description,
            List<String> permissionCodes
    ) {}

    public record PermissionResponse(UUID id, String code, String module, String action, String description) {
        static PermissionResponse from(Permission p) {
            return new PermissionResponse(p.getId(), p.getCode(), p.getModule(), p.getAction(), p.getDescription());
        }
    }

    public record RoleResponse(UUID id, String name, String description, boolean systemRole,
                               List<String> permissions) {
        static RoleResponse from(Role r) {
            return new RoleResponse(r.getId(), r.getName(), r.getDescription(), r.isSystemRole(),
                    r.getPermissions().stream().map(Permission::getCode).sorted().toList());
        }
    }

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AuditService auditService;

    public RoleController(RoleRepository roleRepository, PermissionRepository permissionRepository,
                          AuditService auditService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER_VIEW', 'ROLE_MANAGE')")
    @Transactional(readOnly = true)
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.ok(roleRepository.findAllByOrderByNameAsc().stream()
                .map(RoleResponse::from).toList());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ApiResponse<List<PermissionResponse>> permissions() {
        return ApiResponse.ok(permissionRepository.findAllByOrderByModuleAscActionAsc().stream()
                .map(PermissionResponse::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Transactional
    public ApiResponse<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        if (roleRepository.existsByNameIgnoreCase(request.name())) {
            throw ApiException.conflict("A role with this name already exists");
        }
        Role role = new Role();
        role.setName(request.name().trim());
        role.setDescription(request.description());
        applyPermissions(role, request.permissionCodes());
        roleRepository.save(role);
        auditService.log("CREATE", "ROLE", "Role", role.getId(), null, Map.of("name", role.getName()));
        return ApiResponse.ok("Role created successfully", RoleResponse.from(role));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    @Transactional
    public ApiResponse<RoleResponse> update(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        Role role = roleRepository.findWithPermissionsById(id)
                .orElseThrow(() -> ApiException.notFound("Role"));
        if (!role.getName().equalsIgnoreCase(request.name())
                && roleRepository.existsByNameIgnoreCase(request.name())) {
            throw ApiException.conflict("A role with this name already exists");
        }
        if (role.isSystemRole() && !role.getName().equals(request.name().trim())) {
            throw ApiException.badRequest("System roles cannot be renamed");
        }
        Map<String, Object> old = Map.of("name", role.getName(),
                "permissions", role.getPermissions().stream().map(Permission::getCode).sorted().toList());
        role.setName(request.name().trim());
        role.setDescription(request.description());
        applyPermissions(role, request.permissionCodes());
        auditService.log("UPDATE", "ROLE", "Role", role.getId(), old, Map.of("name", role.getName(),
                "permissions", role.getPermissions().stream().map(Permission::getCode).sorted().toList()));
        return ApiResponse.ok("Role updated successfully", RoleResponse.from(role));
    }

    private void applyPermissions(Role role, List<String> permissionCodes) {
        if (permissionCodes == null) {
            return;
        }
        List<Permission> permissions = permissionCodes.stream()
                .map(code -> permissionRepository.findByCode(code)
                        .orElseThrow(() -> ApiException.badRequest("Unknown permission: " + code)))
                .toList();
        role.setPermissions(new HashSet<>(permissions));
    }
}
