package com.university.assets.faculty;

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

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/faculties")
@Tag(name = "Faculties")
public class FacultyController {

    public record FacultyRequest(
            @NotBlank(message = "Code is required") String code,
            @NotBlank(message = "Name is required") String name,
            String description,
            Boolean active
    ) {}

    public record FacultyResponse(UUID id, String code, String name, String description, boolean active) {
        static FacultyResponse from(Faculty f) {
            return new FacultyResponse(f.getId(), f.getCode(), f.getName(), f.getDescription(), f.isActive());
        }
    }

    private final FacultyRepository repository;
    private final AuditService auditService;

    public FacultyController(FacultyRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @GetMapping
    public ApiResponse<List<FacultyResponse>> list() {
        return ApiResponse.ok(repository.findAllByOrderByNameAsc().stream()
                .map(FacultyResponse::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    @Transactional
    public ApiResponse<FacultyResponse> create(@Valid @RequestBody FacultyRequest request) {
        if (repository.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("Faculty code already exists");
        }
        Faculty faculty = new Faculty();
        apply(faculty, request);
        repository.save(faculty);
        auditService.log("CREATE", "ORGANIZATION", "Faculty", faculty.getId(),
                null, Map.of("code", faculty.getCode(), "name", faculty.getName()));
        return ApiResponse.ok("Faculty created successfully", FacultyResponse.from(faculty));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG_MANAGE')")
    @Transactional
    public ApiResponse<FacultyResponse> update(@PathVariable UUID id,
                                               @Valid @RequestBody FacultyRequest request) {
        Faculty faculty = repository.findById(id).orElseThrow(() -> ApiException.notFound("Faculty"));
        if (!faculty.getCode().equalsIgnoreCase(request.code())
                && repository.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("Faculty code already exists");
        }
        Map<String, Object> old = Map.of("code", faculty.getCode(), "name", faculty.getName(),
                "active", faculty.isActive());
        apply(faculty, request);
        auditService.log("UPDATE", "ORGANIZATION", "Faculty", faculty.getId(),
                old, Map.of("code", faculty.getCode(), "name", faculty.getName(), "active", faculty.isActive()));
        return ApiResponse.ok("Faculty updated successfully", FacultyResponse.from(faculty));
    }

    private void apply(Faculty faculty, FacultyRequest request) {
        faculty.setCode(request.code().trim());
        faculty.setName(request.name().trim());
        faculty.setDescription(request.description());
        if (request.active() != null) {
            faculty.setActive(request.active());
        }
    }
}
