package com.university.assets.asset.dto;

import com.university.assets.asset.Asset;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class AssetDtos {

    private AssetDtos() {}

    public record AssetRequest(
            // Basic information
            @NotBlank(message = "Asset name is required") String name,
            String assetCode,
            @NotNull(message = "Asset type is required") AssetType assetType,
            @NotNull(message = "Category is required") UUID categoryId,
            String description,
            String brand,
            String model,
            String manufacturer,
            String serialNumber,
            String barcode,
            String tags,
            // Ownership and location
            @NotNull(message = "Faculty is required") UUID facultyId,
            UUID departmentId,
            @NotNull(message = "Location is required") UUID locationId,
            String locationNotes,
            UUID custodianUserId,
            // Financial information
            @DecimalMin(value = "0", message = "Purchase price cannot be negative") BigDecimal purchasePrice,
            String currency,
            LocalDate purchaseDate,
            String purchaseOrderNumber,
            String invoiceNumber,
            String fundingSource,
            String grantCode,
            String depreciationMethod,
            @Min(value = 0, message = "Useful life cannot be negative") Integer usefulLifeYears,
            @DecimalMin(value = "0", message = "Salvage value cannot be negative") BigDecimal salvageValue,
            BigDecimal currentBookValue,
            // Condition and availability
            AssetCondition initialCondition,
            AssetCondition condition,
            AssetStatus status,
            @Min(value = 1, message = "Quantity must be greater than zero") Integer quantity,
            Boolean reservable,
            Boolean approvalRequired,
            Boolean externalUseAllowed,
            Boolean depositRequired,
            @DecimalMin(value = "0", message = "Deposit amount cannot be negative") BigDecimal depositAmount,
            @Min(value = 1, message = "Maximum reservation duration must be positive") Integer maxReservationHours,
            // Warranty and maintenance
            LocalDate warrantyStartDate,
            LocalDate warrantyEndDate,
            String warrantyProvider,
            Integer serviceIntervalMonths,
            LocalDate lastServiceDate,
            LocalDate nextServiceDate,
            Boolean calibrationRequired,
            Integer calibrationIntervalMonths,
            LocalDate lastCalibrationDate,
            LocalDate nextCalibrationDate
    ) {}

    public record AssetSummary(
            UUID id,
            String assetCode,
            String name,
            AssetType assetType,
            String categoryName,
            String facultyName,
            String departmentName,
            String locationName,
            String serialNumber,
            AssetCondition condition,
            AssetStatus status,
            int quantity,
            int availableQuantity,
            boolean reservable,
            String custodianName,
            BigDecimal purchasePrice,
            BigDecimal currentBookValue,
            String currency,
            LocalDate nextServiceDate,
            boolean archived
    ) {
        public static AssetSummary from(Asset a) {
            return new AssetSummary(
                    a.getId(), a.getAssetCode(), a.getName(), a.getAssetType(),
                    a.getCategory().getName(),
                    a.getFaculty() != null ? a.getFaculty().getName() : null,
                    a.getDepartment() != null ? a.getDepartment().getName() : null,
                    a.getLocation().getName(), a.getSerialNumber(),
                    a.getCondition(), a.getStatus(), a.getQuantity(), a.getAvailableQuantity(),
                    a.isReservable(),
                    a.getCustodian() != null ? a.getCustodian().getFullName() : null,
                    a.getPurchasePrice(), a.getCurrentBookValue(), a.getCurrency(),
                    a.getNextServiceDate(), a.isArchived());
        }
    }

    public record AssetDetail(
            UUID id,
            String assetCode,
            String name,
            String description,
            AssetType assetType,
            UUID categoryId,
            String categoryName,
            String brand,
            String model,
            String manufacturer,
            String serialNumber,
            String barcode,
            String qrCode,
            String tags,
            UUID facultyId,
            String facultyName,
            UUID departmentId,
            String departmentName,
            UUID locationId,
            String locationName,
            String locationNotes,
            UUID custodianUserId,
            String custodianName,
            String purchaseOrderNumber,
            String invoiceNumber,
            String fundingSource,
            String grantCode,
            LocalDate purchaseDate,
            BigDecimal purchasePrice,
            String currency,
            BigDecimal currentBookValue,
            String depreciationMethod,
            Integer usefulLifeYears,
            BigDecimal salvageValue,
            int quantity,
            int availableQuantity,
            /** When fully checked out: earliest expected return; otherwise null. */
            Instant nextAvailableAt,
            AssetCondition initialCondition,
            AssetCondition condition,
            AssetStatus status,
            boolean reservable,
            boolean approvalRequired,
            boolean externalUseAllowed,
            boolean depositRequired,
            BigDecimal depositAmount,
            Integer maxReservationHours,
            LocalDate warrantyStartDate,
            LocalDate warrantyEndDate,
            String warrantyProvider,
            Integer serviceIntervalMonths,
            LocalDate lastServiceDate,
            LocalDate nextServiceDate,
            boolean calibrationRequired,
            Integer calibrationIntervalMonths,
            LocalDate lastCalibrationDate,
            LocalDate nextCalibrationDate,
            boolean archived,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static AssetDetail from(Asset a) {
            return from(a, null);
        }

        public static AssetDetail from(Asset a, Instant nextAvailableAt) {
            return new AssetDetail(
                    a.getId(), a.getAssetCode(), a.getName(), a.getDescription(), a.getAssetType(),
                    a.getCategory().getId(), a.getCategory().getName(),
                    a.getBrand(), a.getModel(), a.getManufacturer(), a.getSerialNumber(),
                    a.getBarcode(), a.getQrCode(), a.getTags(),
                    a.getFaculty() != null ? a.getFaculty().getId() : null,
                    a.getFaculty() != null ? a.getFaculty().getName() : null,
                    a.getDepartment() != null ? a.getDepartment().getId() : null,
                    a.getDepartment() != null ? a.getDepartment().getName() : null,
                    a.getLocation().getId(), a.getLocation().getName(), a.getLocationNotes(),
                    a.getCustodian() != null ? a.getCustodian().getId() : null,
                    a.getCustodian() != null ? a.getCustodian().getFullName() : null,
                    a.getPurchaseOrderNumber(), a.getInvoiceNumber(), a.getFundingSource(), a.getGrantCode(),
                    a.getPurchaseDate(), a.getPurchasePrice(), a.getCurrency(), a.getCurrentBookValue(),
                    a.getDepreciationMethod(), a.getUsefulLifeYears(), a.getSalvageValue(),
                    a.getQuantity(), a.getAvailableQuantity(), nextAvailableAt,
                    a.getInitialCondition(), a.getCondition(), a.getStatus(),
                    a.isReservable(), a.isApprovalRequired(), a.isExternalUseAllowed(),
                    a.isDepositRequired(), a.getDepositAmount(), a.getMaxReservationHours(),
                    a.getWarrantyStartDate(), a.getWarrantyEndDate(), a.getWarrantyProvider(),
                    a.getServiceIntervalMonths(), a.getLastServiceDate(), a.getNextServiceDate(),
                    a.isCalibrationRequired(), a.getCalibrationIntervalMonths(),
                    a.getLastCalibrationDate(), a.getNextCalibrationDate(),
                    a.isArchived(), a.getCreatedAt(), a.getUpdatedAt());
        }
    }

    public record AssetFilter(
            String search,
            UUID facultyId,
            UUID departmentId,
            UUID locationId,
            UUID categoryId,
            AssetType assetType,
            AssetStatus status,
            AssetCondition condition,
            UUID custodianUserId,
            LocalDate purchasedFrom,
            LocalDate purchasedTo,
            Boolean maintenanceDue,
            Boolean includeArchived,
            /** True limits to issuable assets: availableQuantity > 0 and not blocked by status. */
            Boolean availableOnly,
            /** Filters on the asset's reservable flag (e.g. booking pickers pass true). */
            Boolean reservable
    ) {}
}
