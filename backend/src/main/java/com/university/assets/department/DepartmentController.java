package com.university.assets.department;

import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.faculty.Faculty;
import com.university.assets.faculty.FacultyRepository;
import com.university.assets.role.Permissions;
import com.university.assets.role.Role;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "Departments")
public class DepartmentController {

    public record DepartmentRequest(
            @NotNull(message = "Faculty is required") UUID facultyId,
            @NotBlank(message = "Code is required") String code,
            @NotBlank(message = "Name is required") String name,
            String description,
            Boolean active
    ) {}

    public record DepartmentSettingsRequest(
            @Positive(message = "Maintenance interval must be a positive number of days")
            Integer maintenanceIntervalDays
    ) {}

    public record DepartmentResponse(UUID id, UUID facultyId, String facultyName,
                                     String code, String name, String description, boolean active,
                                     Integer maintenanceIntervalDays) {
        static DepartmentResponse from(Department d) {
            return new DepartmentResponse(d.getId(), d.getFaculty().getId(), d.getFaculty().getName(),
                    d.getCode(), d.getName(), d.getDescription(), d.isActive(),
                    d.getMaintenanceIntervalDays());
        }
    }

    private final DepartmentRepository repository;
    private final FacultyRepository facultyRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public DepartmentController(DepartmentRepository repository, FacultyRepository facultyRepository,
                                UserRepository userRepository, AuditService auditService) {
        this.repository = repository;
        this.facultyRepository = facultyRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<DepartmentResponse>> list(@RequestParam(required = false) UUID facultyId,
                                                      @RequestParam(required = false) String scope) {
        List<Department> departments = facultyId != null
                ? repository.findByFacultyIdOrderByNameAsc(facultyId)
                : repository.findAllByOrderByNameAsc();
        if ("manageable".equalsIgnoreCase(scope)) {
            departments = manageableOnly(departments);
        }
        return ApiResponse.ok(departments.stream().map(DepartmentResponse::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    @Transactional
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        if (repository.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("Department code already exists");
        }
        Department department = new Department();
        apply(department, request);
        repository.save(department);
        auditService.log("CREATE", "ORGANIZATION", "Department", department.getId(),
                null, Map.of("code", department.getCode(), "name", department.getName()));
        return ApiResponse.ok("Department created successfully", DepartmentResponse.from(department));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    @Transactional
    public ApiResponse<DepartmentResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody DepartmentRequest request) {
        Department department = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Department"));
        if (!department.getCode().equalsIgnoreCase(request.code())
                && repository.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("Department code already exists");
        }
        Map<String, Object> old = Map.of("code", department.getCode(), "name", department.getName(),
                "active", department.isActive());
        apply(department, request);
        auditService.log("UPDATE", "ORGANIZATION", "Department", department.getId(), old,
                Map.of("code", department.getCode(), "name", department.getName(), "active", department.isActive()));
        return ApiResponse.ok("Department updated successfully", DepartmentResponse.from(department));
    }

    @PutMapping("/{id}/settings")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    @Transactional
    public ApiResponse<DepartmentResponse> updateSettings(@PathVariable UUID id,
                                                          @Valid @RequestBody DepartmentSettingsRequest request) {
        Department department = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Department"));
        requireSettingsScope(department);
        Integer old = department.getMaintenanceIntervalDays();
        department.setMaintenanceIntervalDays(request.maintenanceIntervalDays());
        auditService.log("UPDATE_SETTINGS", "ORGANIZATION", "Department", department.getId(),
                Collections.singletonMap("maintenanceIntervalDays", old),
                Collections.singletonMap("maintenanceIntervalDays", request.maintenanceIntervalDays()));
        return ApiResponse.ok("Department settings updated", DepartmentResponse.from(department));
    }

    /**
     * SETTINGS_MANAGE alone is not enough: SUPER_ADMIN/ASSET_ADMIN may manage any
     * department, a DEPT_ADMIN only their own department, and a FACULTY_DEAN or
     * FACULTY_ADMIN only departments within their own faculty.
     */
    private void requireSettingsScope(Department department) {
        if (CurrentUser.hasAuthority("ROLE_SUPER_ADMIN") || CurrentUser.hasAuthority("ROLE_ASSET_ADMIN")) {
            return;
        }
        User current = userRepository.findWithRolesById(CurrentUser.id())
                .orElseThrow(() -> ApiException.unauthorized("Authentication required"));
        if (!canManageSettings(current, department)) {
            throw ApiException.forbidden("You can only manage settings for your own department or faculty");
        }
    }

    /**
     * Restricts a listing to the departments the current user may manage settings
     * for — the same scope {@link #requireSettingsScope} enforces on writes.
     */
    private List<Department> manageableOnly(List<Department> departments) {
        if (!CurrentUser.hasAuthority(Permissions.SETTINGS_MANAGE)) {
            throw ApiException.forbidden("Settings management permission is required");
        }
        if (CurrentUser.hasAuthority("ROLE_SUPER_ADMIN") || CurrentUser.hasAuthority("ROLE_ASSET_ADMIN")) {
            return departments;
        }
        User current = userRepository.findWithRolesById(CurrentUser.id())
                .orElseThrow(() -> ApiException.unauthorized("Authentication required"));
        return departments.stream().filter(d -> canManageSettings(current, d)).toList();
    }

    private boolean canManageSettings(User current, Department department) {
        Set<String> roleNames = current.getRoles().stream().map(Role::getName)
                .collect(Collectors.toSet());
        if (roleNames.contains("DEPT_ADMIN") && current.getDepartment() != null
                && current.getDepartment().getId().equals(department.getId())) {
            return true;
        }
        return (roleNames.contains("FACULTY_DEAN") || roleNames.contains("FACULTY_ADMIN"))
                && current.getFaculty() != null
                && current.getFaculty().getId().equals(department.getFaculty().getId());
    }

    private void apply(Department department, DepartmentRequest request) {
        Faculty faculty = facultyRepository.findById(request.facultyId())
                .orElseThrow(() -> ApiException.notFound("Faculty"));
        department.setFaculty(faculty);
        department.setCode(request.code().trim());
        department.setName(request.name().trim());
        department.setDescription(request.description());
        if (request.active() != null) {
            department.setActive(request.active());
        }
    }
}
