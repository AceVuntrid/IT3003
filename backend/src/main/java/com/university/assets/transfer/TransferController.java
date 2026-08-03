package com.university.assets.transfer;

import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.TransferStatus;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.common.response.PageResponse;
import com.university.assets.location.LocationRepository;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Asset Transfers")
public class TransferController {

    public record CreateTransferRequest(
            @NotNull(message = "Asset is required") UUID assetId,
            @NotNull(message = "Destination location is required") UUID toLocationId,
            UUID toCustodianUserId,
            @NotBlank(message = "Transfer reason is required") String reason,
            Instant expectedDate,
            Integer quantity,
            String notes
    ) {}

    public record CompleteTransferRequest(AssetCondition conditionAtDestination, String notes) {}

    public record DecisionRequest(String notes) {}

    public record TransferResponse(
            UUID id, String transferNumber, UUID assetId, String assetName, String assetCode,
            int quantity, String fromLocationName, String toLocationName,
            String fromCustodianName, String toCustodianName, String reason,
            TransferStatus status, String requestedByName, String approvedByName, String receivedByName,
            Instant expectedDate, Instant approvedAt, Instant completedAt,
            AssetCondition conditionAtDestination, String notes, Instant createdAt
    ) {
        static TransferResponse from(AssetTransfer t) {
            return new TransferResponse(t.getId(), t.getTransferNumber(),
                    t.getAsset().getId(), t.getAsset().getName(), t.getAsset().getAssetCode(),
                    t.getQuantity(), t.getFromLocation().getName(), t.getToLocation().getName(),
                    t.getFromCustodian() != null ? t.getFromCustodian().getFullName() : null,
                    t.getToCustodian() != null ? t.getToCustodian().getFullName() : null,
                    t.getReason(), t.getStatus(), t.getRequestedBy().getFullName(),
                    t.getApprovedBy() != null ? t.getApprovedBy().getFullName() : null,
                    t.getReceivedBy() != null ? t.getReceivedBy().getFullName() : null,
                    t.getExpectedDate(), t.getApprovedAt(), t.getCompletedAt(),
                    t.getConditionAtDestination(), t.getNotes(), t.getCreatedAt());
        }
    }

    private final AssetTransferRepository repository;
    private final AssetRepository assetRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public TransferController(AssetTransferRepository repository, AssetRepository assetRepository,
                              LocationRepository locationRepository, UserRepository userRepository,
                              AuditService auditService) {
        this.repository = repository;
        this.assetRepository = assetRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TRANSFER_VIEW')")
    public ApiResponse<PageResponse<TransferResponse>> list(
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(required = false) UUID assetId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Specification<AssetTransfer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (assetId != null) {
                predicates.add(cb.equal(root.get("asset").get("id"), assetId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return ApiResponse.ok(PageResponse.from(repository.findAll(spec, pageable), TransferResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TRANSFER_VIEW')")
    public ApiResponse<TransferResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(TransferResponse.from(find(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TRANSFER_CREATE')")
    @Transactional
    public ApiResponse<TransferResponse> create(@Valid @RequestBody CreateTransferRequest request) {
        var asset = assetRepository.findDetailedById(request.assetId())
                .orElseThrow(() -> ApiException.notFound("Asset"));
        if (asset.isArchived()) {
            throw ApiException.badRequest("Archived assets cannot be transferred");
        }
        var toLocation = locationRepository.findById(request.toLocationId())
                .orElseThrow(() -> ApiException.notFound("Destination location"));
        if (toLocation.getId().equals(asset.getLocation().getId())) {
            throw ApiException.badRequest("The asset is already at the selected location");
        }
        AssetTransfer transfer = new AssetTransfer();
        transfer.setTransferNumber(String.format("TRF-%05d", repository.count() + 1));
        transfer.setAsset(asset);
        transfer.setQuantity(request.quantity() != null ? request.quantity() : asset.getQuantity());
        transfer.setFromLocation(asset.getLocation());
        transfer.setToLocation(toLocation);
        transfer.setFromCustodian(asset.getCustodian());
        transfer.setToCustodian(request.toCustodianUserId() == null ? null
                : userRepository.findById(request.toCustodianUserId())
                .orElseThrow(() -> ApiException.notFound("Custodian user")));
        transfer.setReason(request.reason().trim());
        transfer.setExpectedDate(request.expectedDate());
        transfer.setNotes(request.notes());
        transfer.setRequestedBy(userRepository.getReferenceById(CurrentUser.id()));
        repository.save(transfer);
        auditService.log("CREATE", "TRANSFER", "AssetTransfer", transfer.getId(), null,
                Map.of("number", transfer.getTransferNumber(), "asset", asset.getAssetCode()));
        return ApiResponse.ok("Transfer request submitted", TransferResponse.from(transfer));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('TRANSFER_APPROVE')")
    @Transactional
    public ApiResponse<TransferResponse> approve(@PathVariable UUID id,
                                                 @RequestBody(required = false) DecisionRequest request) {
        AssetTransfer transfer = find(id);
        requirePending(transfer);
        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setApprovedBy(userRepository.getReferenceById(CurrentUser.id()));
        transfer.setApprovedAt(Instant.now());
        if (request != null && request.notes() != null) {
            transfer.setNotes(request.notes());
        }
        auditService.log("APPROVE", "TRANSFER", "AssetTransfer", transfer.getId(), null,
                Map.of("number", transfer.getTransferNumber()));
        return ApiResponse.ok("Transfer approved", TransferResponse.from(transfer));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('TRANSFER_APPROVE')")
    @Transactional
    public ApiResponse<TransferResponse> reject(@PathVariable UUID id,
                                                @RequestBody(required = false) DecisionRequest request) {
        AssetTransfer transfer = find(id);
        requirePending(transfer);
        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setApprovedBy(userRepository.getReferenceById(CurrentUser.id()));
        transfer.setApprovedAt(Instant.now());
        if (request != null && request.notes() != null) {
            transfer.setNotes(request.notes());
        }
        auditService.log("REJECT", "TRANSFER", "AssetTransfer", transfer.getId(), null,
                Map.of("number", transfer.getTransferNumber()));
        return ApiResponse.ok("Transfer rejected", TransferResponse.from(transfer));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('TRANSFER_APPROVE')")
    @Transactional
    public ApiResponse<TransferResponse> complete(@PathVariable UUID id,
                                                  @RequestBody(required = false) CompleteTransferRequest request) {
        AssetTransfer transfer = find(id);
        if (transfer.getStatus() != TransferStatus.APPROVED) {
            throw ApiException.badRequest("Only approved transfers can be completed");
        }
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setCompletedAt(Instant.now());
        transfer.setReceivedBy(userRepository.getReferenceById(CurrentUser.id()));
        if (request != null) {
            transfer.setConditionAtDestination(request.conditionAtDestination());
            if (request.notes() != null) {
                transfer.setNotes(request.notes());
            }
        }
        // Move the asset: location, faculty/department follow the destination, custodian updates.
        var asset = transfer.getAsset();
        Map<String, Object> old = Map.of("locationId", asset.getLocation().getId().toString());
        asset.setLocation(transfer.getToLocation());
        if (transfer.getToLocation().getFaculty() != null) {
            asset.setFaculty(transfer.getToLocation().getFaculty());
        }
        if (transfer.getToLocation().getDepartment() != null) {
            asset.setDepartment(transfer.getToLocation().getDepartment());
        }
        if (transfer.getToCustodian() != null) {
            asset.setCustodian(transfer.getToCustodian());
        }
        if (request != null && request.conditionAtDestination() != null) {
            asset.setCondition(request.conditionAtDestination());
        }
        auditService.log("COMPLETE", "TRANSFER", "AssetTransfer", transfer.getId(), old,
                Map.of("number", transfer.getTransferNumber(),
                        "locationId", asset.getLocation().getId().toString()));
        return ApiResponse.ok("Transfer completed", TransferResponse.from(transfer));
    }

    private AssetTransfer find(UUID id) {
        return repository.findDetailedById(id)
                .orElseThrow(() -> ApiException.notFound("Transfer"));
    }

    private void requirePending(AssetTransfer transfer) {
        if (transfer.getStatus() != TransferStatus.PENDING_APPROVAL) {
            throw ApiException.badRequest("Only pending transfers can be approved or rejected");
        }
    }
}
