package com.university.assets.asset;

import com.university.assets.asset.dto.AssetDtos.AssetDetail;
import com.university.assets.asset.dto.AssetDtos.AssetFilter;
import com.university.assets.asset.dto.AssetDtos.AssetRequest;
import com.university.assets.asset.dto.AssetDtos.AssetSummary;
import com.university.assets.asset.specification.AssetSpecifications;
import com.university.assets.audit.AuditService;
import com.university.assets.category.AssetCategoryRepository;
import com.university.assets.checkout.CheckoutRepository;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.CheckoutStatus;
import com.university.assets.common.response.PageResponse;
import com.university.assets.department.DepartmentRepository;
import com.university.assets.faculty.FacultyRepository;
import com.university.assets.location.LocationRepository;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final AssetCategoryRepository categoryRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final CheckoutRepository checkoutRepository;
    private final AuditService auditService;

    public AssetService(AssetRepository assetRepository, AssetCategoryRepository categoryRepository,
                        FacultyRepository facultyRepository, DepartmentRepository departmentRepository,
                        LocationRepository locationRepository, UserRepository userRepository,
                        CheckoutRepository checkoutRepository, AuditService auditService) {
        this.assetRepository = assetRepository;
        this.categoryRepository = categoryRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.checkoutRepository = checkoutRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AssetSummary> list(AssetFilter filter, Pageable pageable) {
        return PageResponse.from(
                assetRepository.findAll(AssetSpecifications.withFilter(filter), pageable),
                AssetSummary::from);
    }

    @Transactional(readOnly = true)
    public AssetDetail get(UUID id) {
        Asset asset = find(id);
        Instant nextAvailableAt = null;
        if (asset.getAvailableQuantity() <= 0) {
            nextAvailableAt = checkoutRepository.findEarliestExpectedReturn(
                    asset.getId(), EnumSet.of(CheckoutStatus.CHECKED_OUT, CheckoutStatus.OVERDUE));
        }
        return AssetDetail.from(asset, nextAvailableAt);
    }

    @Transactional
    public AssetDetail create(AssetRequest request) {
        validate(request);
        Asset asset = new Asset();
        String code = request.assetCode() == null || request.assetCode().isBlank()
                ? generateAssetCode() : request.assetCode().trim();
        if (assetRepository.existsByAssetCodeIgnoreCase(code)) {
            throw ApiException.conflict("Asset code already exists");
        }
        if (request.serialNumber() != null && !request.serialNumber().isBlank()
                && assetRepository.existsBySerialNumberIgnoreCase(request.serialNumber().trim())) {
            throw ApiException.conflict("Serial number already exists");
        }
        asset.setAssetCode(code);
        asset.setQrCode("ASSET:" + code);
        apply(asset, request, true);
        asset.setCreatedBy(CurrentUser.id());
        assetRepository.save(asset);
        auditService.log("CREATE", "ASSET", "Asset", asset.getId(), null,
                Map.of("assetCode", asset.getAssetCode(), "name", asset.getName()));
        return AssetDetail.from(asset);
    }

    @Transactional
    public AssetDetail update(UUID id, AssetRequest request) {
        validate(request);
        Asset asset = find(id);
        if (asset.isArchived()) {
            throw ApiException.badRequest("Archived assets cannot be edited. Restore the asset first.");
        }
        if (request.serialNumber() != null && !request.serialNumber().isBlank()
                && !request.serialNumber().trim().equalsIgnoreCase(asset.getSerialNumber())
                && assetRepository.existsBySerialNumberIgnoreCase(request.serialNumber().trim())) {
            throw ApiException.conflict("Serial number already exists");
        }
        Map<String, Object> old = sensitiveSnapshot(asset);
        apply(asset, request, false);
        Map<String, Object> updated = sensitiveSnapshot(asset);
        // Only record an audit entry when sensitive values actually changed.
        if (!old.equals(updated)) {
            auditService.log("UPDATE_SENSITIVE", "ASSET", "Asset", asset.getId(), old, updated);
        } else {
            auditService.log("UPDATE", "ASSET", "Asset", asset.getId(), null,
                    Map.of("assetCode", asset.getAssetCode()));
        }
        return AssetDetail.from(asset);
    }

    @Transactional
    public void archive(UUID id) {
        Asset asset = find(id);
        if (asset.isArchived()) {
            throw ApiException.badRequest("Asset is already archived");
        }
        if (asset.getStatus() == AssetStatus.CHECKED_OUT) {
            throw ApiException.badRequest("A checked-out asset cannot be archived");
        }
        asset.setArchivedAt(Instant.now());
        asset.setStatus(AssetStatus.ARCHIVED);
        auditService.log("ARCHIVE", "ASSET", "Asset", asset.getId(), null,
                Map.of("assetCode", asset.getAssetCode()));
    }

    @Transactional
    public void restore(UUID id) {
        Asset asset = find(id);
        if (!asset.isArchived()) {
            throw ApiException.badRequest("Asset is not archived");
        }
        asset.setArchivedAt(null);
        asset.setStatus(AssetStatus.AVAILABLE);
        auditService.log("RESTORE", "ASSET", "Asset", asset.getId(), null,
                Map.of("assetCode", asset.getAssetCode()));
    }

    @Transactional
    public AssetDetail changeStatus(UUID id, AssetStatus status, AssetCondition condition) {
        Asset asset = find(id);
        Map<String, Object> old = Map.of("status", asset.getStatus().name(),
                "condition", asset.getCondition().name());
        if (status != null) {
            asset.setStatus(status);
        }
        if (condition != null) {
            asset.setCondition(condition);
        }
        auditService.log("STATUS_CHANGE", "ASSET", "Asset", asset.getId(), old,
                Map.of("status", asset.getStatus().name(), "condition", asset.getCondition().name()));
        return AssetDetail.from(asset);
    }

    Asset find(UUID id) {
        return assetRepository.findDetailedById(id)
                .orElseThrow(() -> ApiException.notFound("Asset"));
    }

    private void validate(AssetRequest r) {
        if (r.warrantyStartDate() != null && r.warrantyEndDate() != null
                && r.warrantyEndDate().isBefore(r.warrantyStartDate())) {
            throw ApiException.badRequest("Warranty end date cannot be before warranty start date");
        }
        if (r.lastServiceDate() != null && r.nextServiceDate() != null
                && r.nextServiceDate().isBefore(r.lastServiceDate())) {
            throw ApiException.badRequest("Next service date cannot be before last service date");
        }
        if (Boolean.TRUE.equals(r.depositRequired())
                && (r.depositAmount() == null || r.depositAmount().signum() <= 0)) {
            throw ApiException.badRequest("Deposit amount is required when a deposit is enabled");
        }
    }

    private void apply(Asset asset, AssetRequest r, boolean isNew) {
        asset.setName(r.name().trim());
        asset.setDescription(r.description());
        asset.setAssetType(r.assetType());
        asset.setCategory(categoryRepository.findById(r.categoryId())
                .orElseThrow(() -> ApiException.notFound("Category")));
        asset.setBrand(r.brand());
        asset.setModel(r.model());
        asset.setManufacturer(r.manufacturer());
        asset.setSerialNumber(r.serialNumber() == null || r.serialNumber().isBlank()
                ? null : r.serialNumber().trim());
        asset.setBarcode(r.barcode());
        asset.setTags(r.tags());

        var faculty = facultyRepository.findById(r.facultyId())
                .orElseThrow(() -> ApiException.notFound("Faculty"));
        asset.setFaculty(faculty);
        if (r.departmentId() != null) {
            var department = departmentRepository.findById(r.departmentId())
                    .orElseThrow(() -> ApiException.notFound("Department"));
            if (!department.getFaculty().getId().equals(faculty.getId())) {
                throw ApiException.badRequest("Department does not belong to the selected faculty");
            }
            asset.setDepartment(department);
        } else {
            asset.setDepartment(null);
        }
        asset.setLocation(locationRepository.findById(r.locationId())
                .orElseThrow(() -> ApiException.notFound("Location")));
        asset.setLocationNotes(r.locationNotes());
        asset.setCustodian(r.custodianUserId() == null ? null
                : userRepository.findById(r.custodianUserId())
                .orElseThrow(() -> ApiException.notFound("Custodian user")));

        asset.setPurchaseOrderNumber(r.purchaseOrderNumber());
        asset.setInvoiceNumber(r.invoiceNumber());
        asset.setFundingSource(r.fundingSource());
        asset.setGrantCode(r.grantCode());
        asset.setPurchaseDate(r.purchaseDate());
        asset.setPurchasePrice(r.purchasePrice());
        if (r.currency() != null && !r.currency().isBlank()) {
            asset.setCurrency(r.currency().trim().toUpperCase());
        }
        asset.setCurrentBookValue(r.currentBookValue() != null ? r.currentBookValue() : r.purchasePrice());
        asset.setDepreciationMethod(r.depreciationMethod());
        asset.setUsefulLifeYears(r.usefulLifeYears());
        asset.setSalvageValue(r.salvageValue());

        int quantity = r.quantity() != null ? r.quantity() : 1;
        if (isNew) {
            asset.setQuantity(quantity);
            asset.setAvailableQuantity(quantity);
        } else {
            int inUse = asset.getQuantity() - asset.getAvailableQuantity();
            if (quantity < inUse) {
                throw ApiException.badRequest(
                        "Quantity cannot fall below the checked-out or reserved quantity (" + inUse + ")");
            }
            asset.setAvailableQuantity(quantity - inUse);
            asset.setQuantity(quantity);
        }

        if (r.initialCondition() != null) {
            asset.setInitialCondition(r.initialCondition());
        } else if (isNew) {
            asset.setInitialCondition(r.condition() != null ? r.condition() : AssetCondition.GOOD);
        }
        if (r.condition() != null) {
            asset.setCondition(r.condition());
        }
        if (r.status() != null) {
            asset.setStatus(r.status());
        }
        if (r.reservable() != null) {
            asset.setReservable(r.reservable());
        }
        if (r.approvalRequired() != null) {
            asset.setApprovalRequired(r.approvalRequired());
        }
        if (r.externalUseAllowed() != null) {
            asset.setExternalUseAllowed(r.externalUseAllowed());
        }
        if (r.depositRequired() != null) {
            asset.setDepositRequired(r.depositRequired());
        }
        asset.setDepositAmount(r.depositAmount());
        asset.setMaxReservationHours(r.maxReservationHours());

        asset.setWarrantyStartDate(r.warrantyStartDate());
        asset.setWarrantyEndDate(r.warrantyEndDate());
        asset.setWarrantyProvider(r.warrantyProvider());
        asset.setServiceIntervalMonths(r.serviceIntervalMonths());
        asset.setLastServiceDate(r.lastServiceDate());
        asset.setNextServiceDate(r.nextServiceDate());
        if (r.calibrationRequired() != null) {
            asset.setCalibrationRequired(r.calibrationRequired());
        }
        asset.setCalibrationIntervalMonths(r.calibrationIntervalMonths());
        asset.setLastCalibrationDate(r.lastCalibrationDate());
        asset.setNextCalibrationDate(r.nextCalibrationDate());
    }

    private Map<String, Object> sensitiveSnapshot(Asset asset) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("assetCode", asset.getAssetCode());
        map.put("serialNumber", asset.getSerialNumber());
        map.put("purchasePrice", asset.getPurchasePrice() != null ? asset.getPurchasePrice().toString() : null);
        map.put("facultyId", asset.getFaculty() != null ? asset.getFaculty().getId().toString() : null);
        map.put("departmentId", asset.getDepartment() != null ? asset.getDepartment().getId().toString() : null);
        map.put("locationId", asset.getLocation().getId().toString());
        map.put("custodianId", asset.getCustodian() != null ? asset.getCustodian().getId().toString() : null);
        map.put("status", asset.getStatus().name());
        map.put("condition", asset.getCondition().name());
        return map;
    }

    private String generateAssetCode() {
        long count = assetRepository.count() + 1;
        String code;
        do {
            code = String.format("AST-%05d", count++);
        } while (assetRepository.existsByAssetCodeIgnoreCase(code));
        return code;
    }
}
