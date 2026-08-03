package com.university.assets.audit;

import com.university.assets.common.response.ApiResponse;
import com.university.assets.common.response.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit-logs")
@Tag(name = "Audit Log")
public class AuditController {

    public record AuditLogResponse(UUID id, UUID userId, String userEmail, String action, String module,
                                   String entityType, UUID entityId, String oldValues, String newValues,
                                   String ipAddress, String userAgent, boolean success, Instant createdAt) {
        static AuditLogResponse from(AuditLog a) {
            return new AuditLogResponse(a.getId(), a.getUserId(), a.getUserEmail(), a.getAction(),
                    a.getModule(), a.getEntityType(), a.getEntityId(), a.getOldValues(), a.getNewValues(),
                    a.getIpAddress(), a.getUserAgent(), a.isSuccess(), a.getCreatedAt());
        }
    }

    private final AuditLogRepository repository;

    public AuditController(AuditLogRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public ApiResponse<PageResponse<AuditLogResponse>> list(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @ParameterObject @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (module != null && !module.isBlank()) {
                predicates.add(cb.equal(root.get("module"), module));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (userEmail != null && !userEmail.isBlank()) {
                predicates.add(cb.like(cb.lower(cb.coalesce(root.get("userEmail"), "")),
                        "%" + userEmail.toLowerCase().trim() + "%"));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return ApiResponse.ok(PageResponse.from(repository.findAll(spec, pageable), AuditLogResponse::from));
    }
}
