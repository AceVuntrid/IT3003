package com.university.assets.category;

import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AssetType;
import com.university.assets.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Asset Categories")
public class AssetCategoryController {

    public record CategoryRequest(
            @NotBlank(message = "Code is required") String code,
            @NotBlank(message = "Name is required") String name,
            @NotNull(message = "Asset type is required") AssetType assetType,
            UUID parentId,
            String description,
            Boolean active
    ) {}

    public record CategoryResponse(UUID id, String code, String name, AssetType assetType,
                                   UUID parentId, String parentName, String description, boolean active) {
        static CategoryResponse from(AssetCategory c) {
            return new CategoryResponse(c.getId(), c.getCode(), c.getName(), c.getAssetType(),
                    c.getParent() != null ? c.getParent().getId() : null,
                    c.getParent() != null ? c.getParent().getName() : null,
                    c.getDescription(), c.isActive());
        }
    }

    private final AssetCategoryRepository repository;
    private final AuditService auditService;

    public AssetCategoryController(AssetCategoryRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<CategoryResponse>> list(@RequestParam(required = false) AssetType assetType) {
        List<AssetCategory> categories = assetType != null
                ? repository.findByAssetTypeAndActiveTrueOrderByNameAsc(assetType)
                : repository.findAllByOrderByNameAsc();
        return ApiResponse.ok(categories.stream().map(CategoryResponse::from).toList());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @Transactional
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        if (repository.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("Category code already exists");
        }
        AssetCategory category = new AssetCategory();
        apply(category, request);
        repository.save(category);
        auditService.log("CREATE", "CATEGORY", "AssetCategory", category.getId(), null,
                Map.of("code", category.getCode(), "name", category.getName()));
        return ApiResponse.ok("Category created successfully", CategoryResponse.from(category));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @Transactional
    public ApiResponse<CategoryResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody CategoryRequest request) {
        AssetCategory category = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category"));
        if (!category.getCode().equalsIgnoreCase(request.code())
                && repository.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("Category code already exists");
        }
        Map<String, Object> old = Map.of("code", category.getCode(), "name", category.getName(),
                "active", category.isActive());
        apply(category, request);
        auditService.log("UPDATE", "CATEGORY", "AssetCategory", category.getId(), old,
                Map.of("code", category.getCode(), "name", category.getName(), "active", category.isActive()));
        return ApiResponse.ok("Category updated successfully", CategoryResponse.from(category));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        AssetCategory category = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Category"));
        
        try {
            repository.delete(category);
            repository.flush(); // To trigger ConstraintViolation immediately if any
        } catch (Exception e) {
            throw ApiException.conflict("Cannot delete category because it is currently in use");
        }
        
        auditService.log("DELETE", "CATEGORY", "AssetCategory", id,
                Map.of("code", category.getCode(), "name", category.getName()), null);
        return ApiResponse.message("Category deleted successfully");
    }

    private void apply(AssetCategory category, CategoryRequest request) {
        if (request.parentId() != null && request.parentId().equals(category.getId())) {
            throw ApiException.badRequest("A category cannot be its own parent");
        }
        category.setCode(request.code().trim());
        category.setName(request.name().trim());
        category.setAssetType(request.assetType());
        category.setDescription(request.description());
        if (request.active() != null) {
            category.setActive(request.active());
        }
        category.setParent(request.parentId() == null ? null
                : repository.findById(request.parentId())
                .orElseThrow(() -> ApiException.notFound("Parent category")));
    }
}
