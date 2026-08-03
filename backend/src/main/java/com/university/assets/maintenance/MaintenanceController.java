package com.university.assets.maintenance;

import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.MaintenanceIssueType;
import com.university.assets.common.model.Enums.MaintenancePriority;
import com.university.assets.common.model.Enums.MaintenanceStatus;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.common.response.PageResponse;
import com.university.assets.notification.NotificationService;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/maintenance-requests")
@Tag(name = "Maintenance")
public class MaintenanceController {

    public record CreateRequest(
            @NotNull(message = "Asset is required") UUID assetId,
            @NotNull(message = "Issue type is required") MaintenanceIssueType issueType,
            @NotBlank(message = "Description is required") String description,
            MaintenancePriority priority,
            Instant dueAt,
            Boolean assetOutOfService
    ) {}

    public record AssignRequest(UUID assignedToUserId) {}

    public record UpdateJobRequest(
            String diagnosis,
            String workPerformed,
            String partsUsed,
            BigDecimal labourCost,
            BigDecimal partsCost,
            BigDecimal externalCost,
            String notes,
            MaintenanceStatus status
    ) {}

    public record CompleteRequest(
            String workPerformed,
            String result,
            AssetCondition newCondition,
            LocalDate nextServiceDate,
            BigDecimal labourCost,
            BigDecimal partsCost,
            BigDecimal externalCost,
            Boolean unrepairable,
            String notes
    ) {}

    public record MaintenanceResponse(
            UUID id, String requestNumber, UUID assetId, String assetName, String assetCode,
            MaintenanceIssueType issueType, String description, MaintenancePriority priority,
            String requestedByName, String assignedToName,
            MaintenanceStatus status, Instant openedAt, Instant dueAt, Instant startedAt,
            Instant completedAt, String diagnosis, String workPerformed, String partsUsed,
            BigDecimal labourCost, BigDecimal partsCost, BigDecimal externalCost, BigDecimal totalCost,
            String result, AssetCondition newCondition, LocalDate nextServiceDate, String notes
    ) {
        static MaintenanceResponse from(MaintenanceRequest m) {
            return new MaintenanceResponse(m.getId(), m.getRequestNumber(),
                    m.getAsset().getId(), m.getAsset().getName(), m.getAsset().getAssetCode(),
                    m.getIssueType(), m.getDescription(), m.getPriority(),
                    m.getRequestedBy().getFullName(),
                    m.getAssignedTo() != null ? m.getAssignedTo().getFullName() : null,
                    m.getStatus(), m.getOpenedAt(), m.getDueAt(), m.getStartedAt(), m.getCompletedAt(),
                    m.getDiagnosis(), m.getWorkPerformed(), m.getPartsUsed(),
                    m.getLabourCost(), m.getPartsCost(), m.getExternalCost(), m.getTotalCost(),
                    m.getResult(), m.getNewCondition(), m.getNextServiceDate(), m.getNotes());
        }
    }

    private final MaintenanceRequestRepository repository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public MaintenanceController(MaintenanceRequestRepository repository,
                                 AssetRepository assetRepository,
                                 UserRepository userRepository,
                                 AuditService auditService,
                                 NotificationService notificationService) {
        this.repository = repository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MAINTENANCE_VIEW')")
    public ApiResponse<PageResponse<MaintenanceResponse>> list(
            @RequestParam(required = false) MaintenanceStatus status,
            @RequestParam(required = false) MaintenancePriority priority,
            @RequestParam(required = false) UUID assetId,
            @RequestParam(required = false) UUID assignedToUserId,
            @ParameterObject @PageableDefault(size = 20, sort = "openedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Specification<MaintenanceRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (assetId != null) {
                predicates.add(cb.equal(root.get("asset").get("id"), assetId));
            }
            if (assignedToUserId != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedToUserId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return ApiResponse.ok(PageResponse.from(repository.findAll(spec, pageable), MaintenanceResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('MAINTENANCE_VIEW')")
    public ApiResponse<MaintenanceResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(MaintenanceResponse.from(find(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MAINTENANCE_CREATE')")
    @Transactional
    public ApiResponse<MaintenanceResponse> create(@Valid @RequestBody CreateRequest request) {
        var asset = assetRepository.findDetailedById(request.assetId())
                .orElseThrow(() -> ApiException.notFound("Asset"));
        MaintenanceRequest m = new MaintenanceRequest();
        m.setRequestNumber(String.format("MNT-%05d", repository.count() + 1));
        m.setAsset(asset);
        m.setIssueType(request.issueType());
        m.setDescription(request.description().trim());
        if (request.priority() != null) {
            m.setPriority(request.priority());
        }
        m.setDueAt(request.dueAt());
        m.setRequestedBy(userRepository.getReferenceById(CurrentUser.id()));
        repository.save(m);
        if (Boolean.TRUE.equals(request.assetOutOfService())) {
            asset.setStatus(AssetStatus.UNDER_MAINTENANCE);
        }
        auditService.log("CREATE", "MAINTENANCE", "MaintenanceRequest", m.getId(), null,
                Map.of("number", m.getRequestNumber(), "asset", asset.getAssetCode()));
        return ApiResponse.ok("Maintenance request created", MaintenanceResponse.from(m));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('MAINTENANCE_MANAGE')")
    @Transactional
    public ApiResponse<MaintenanceResponse> assign(@PathVariable UUID id,
                                                   @RequestBody AssignRequest request) {
        MaintenanceRequest m = find(id);
        requireOpen(m);
        if (request.assignedToUserId() != null) {
            m.setAssignedTo(userRepository.findById(request.assignedToUserId())
                    .orElseThrow(() -> ApiException.notFound("User")));
            notificationService.notifyUser(request.assignedToUserId(), "MAINTENANCE_ASSIGNED",
                    "Maintenance job assigned: " + m.getRequestNumber(),
                    m.getAsset().getName() + " — " + m.getDescription(),
                    "MaintenanceRequest", m.getId());
        }
        m.setStatus(MaintenanceStatus.ASSIGNED);
        auditService.log("ASSIGN", "MAINTENANCE", "MaintenanceRequest", m.getId(), null,
                Map.of("number", m.getRequestNumber()));
        return ApiResponse.ok("Maintenance request assigned", MaintenanceResponse.from(m));
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('MAINTENANCE_MANAGE')")
    @Transactional
    public ApiResponse<MaintenanceResponse> start(@PathVariable UUID id) {
        MaintenanceRequest m = find(id);
        requireOpen(m);
        m.setStatus(MaintenanceStatus.IN_PROGRESS);
        m.setStartedAt(Instant.now());
        m.getAsset().setStatus(AssetStatus.UNDER_MAINTENANCE);
        auditService.log("START", "MAINTENANCE", "MaintenanceRequest", m.getId(), null,
                Map.of("number", m.getRequestNumber()));
        return ApiResponse.ok("Work started", MaintenanceResponse.from(m));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MAINTENANCE_MANAGE')")
    @Transactional
    public ApiResponse<MaintenanceResponse> updateJob(@PathVariable UUID id,
                                                      @RequestBody UpdateJobRequest request) {
        MaintenanceRequest m = find(id);
        requireOpen(m);
        if (request.diagnosis() != null) m.setDiagnosis(request.diagnosis());
        if (request.workPerformed() != null) m.setWorkPerformed(request.workPerformed());
        if (request.partsUsed() != null) m.setPartsUsed(request.partsUsed());
        if (request.labourCost() != null) m.setLabourCost(request.labourCost());
        if (request.partsCost() != null) m.setPartsCost(request.partsCost());
        if (request.externalCost() != null) m.setExternalCost(request.externalCost());
        if (request.notes() != null) m.setNotes(request.notes());
        if (request.status() == MaintenanceStatus.WAITING_FOR_PARTS
                || request.status() == MaintenanceStatus.WAITING_FOR_VENDOR
                || request.status() == MaintenanceStatus.IN_PROGRESS) {
            m.setStatus(request.status());
        }
        auditService.log("UPDATE", "MAINTENANCE", "MaintenanceRequest", m.getId(), null,
                Map.of("number", m.getRequestNumber(), "status", m.getStatus().name()));
        return ApiResponse.ok("Maintenance job updated", MaintenanceResponse.from(m));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('MAINTENANCE_MANAGE')")
    @Transactional
    public ApiResponse<MaintenanceResponse> complete(@PathVariable UUID id,
                                                     @Valid @RequestBody CompleteRequest request) {
        MaintenanceRequest m = find(id);
        requireOpen(m);
        boolean unrepairable = Boolean.TRUE.equals(request.unrepairable());
        m.setStatus(unrepairable ? MaintenanceStatus.UNREPAIRABLE : MaintenanceStatus.COMPLETED);
        m.setCompletedAt(Instant.now());
        if (request.workPerformed() != null) m.setWorkPerformed(request.workPerformed());
        m.setResult(request.result());
        m.setNewCondition(request.newCondition());
        m.setNextServiceDate(request.nextServiceDate());
        if (request.labourCost() != null) m.setLabourCost(request.labourCost());
        if (request.partsCost() != null) m.setPartsCost(request.partsCost());
        if (request.externalCost() != null) m.setExternalCost(request.externalCost());
        if (request.notes() != null) m.setNotes(request.notes());
        BigDecimal total = BigDecimal.ZERO;
        if (m.getLabourCost() != null) total = total.add(m.getLabourCost());
        if (m.getPartsCost() != null) total = total.add(m.getPartsCost());
        if (m.getExternalCost() != null) total = total.add(m.getExternalCost());
        m.setTotalCost(total);

        var asset = m.getAsset();
        if (unrepairable) {
            asset.setStatus(AssetStatus.DAMAGED);
            asset.setCondition(AssetCondition.UNSERVICEABLE);
        } else {
            asset.setStatus(AssetStatus.AVAILABLE);
            if (request.newCondition() != null) {
                asset.setCondition(request.newCondition());
            }
            asset.setLastServiceDate(LocalDate.now());
            if (request.nextServiceDate() != null) {
                asset.setNextServiceDate(request.nextServiceDate());
            }
            if (m.getIssueType() == MaintenanceIssueType.CALIBRATION) {
                asset.setLastCalibrationDate(LocalDate.now());
                if (asset.getCalibrationIntervalMonths() != null) {
                    asset.setNextCalibrationDate(LocalDate.now()
                            .plusMonths(asset.getCalibrationIntervalMonths()));
                }
            }
        }
        auditService.log("COMPLETE", "MAINTENANCE", "MaintenanceRequest", m.getId(), null,
                Map.of("number", m.getRequestNumber(), "result", m.getStatus().name(),
                        "totalCost", total.toString()));
        notificationService.notifyUser(m.getRequestedBy().getId(), "MAINTENANCE_COMPLETED",
                "Maintenance " + m.getRequestNumber() + " " + m.getStatus().name().toLowerCase(),
                asset.getName() + " maintenance has been closed.", "MaintenanceRequest", m.getId());
        return ApiResponse.ok("Maintenance job completed", MaintenanceResponse.from(m));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('MAINTENANCE_MANAGE')")
    @Transactional
    public ApiResponse<MaintenanceResponse> cancel(@PathVariable UUID id) {
        MaintenanceRequest m = find(id);
        requireOpen(m);
        m.setStatus(MaintenanceStatus.CANCELLED);
        if (m.getAsset().getStatus() == AssetStatus.UNDER_MAINTENANCE) {
            m.getAsset().setStatus(AssetStatus.AVAILABLE);
        }
        auditService.log("CANCEL", "MAINTENANCE", "MaintenanceRequest", m.getId(), null,
                Map.of("number", m.getRequestNumber()));
        return ApiResponse.ok("Maintenance request cancelled", MaintenanceResponse.from(m));
    }

    private MaintenanceRequest find(UUID id) {
        return repository.findDetailedById(id)
                .orElseThrow(() -> ApiException.notFound("Maintenance request"));
    }

    private void requireOpen(MaintenanceRequest m) {
        if (m.getStatus() == MaintenanceStatus.COMPLETED
                || m.getStatus() == MaintenanceStatus.CANCELLED
                || m.getStatus() == MaintenanceStatus.UNREPAIRABLE) {
            throw ApiException.badRequest("This maintenance request is already closed");
        }
    }
}
