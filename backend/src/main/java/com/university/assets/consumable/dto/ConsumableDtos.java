package com.university.assets.consumable.dto;

import com.university.assets.common.model.Enums.StockTransactionType;
import com.university.assets.consumable.ConsumableBatch;
import com.university.assets.consumable.ConsumableItem;
import com.university.assets.consumable.StockTransaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ConsumableDtos {

    private ConsumableDtos() {}

    public record ConsumableRequest(
            @NotBlank(message = "Item name is required") String name,
            String itemCode,
            @NotNull(message = "Category is required") UUID categoryId,
            String description,
            String brand,
            String manufacturer,
            @NotBlank(message = "Unit of measure is required") String unitOfMeasure,
            @NotNull(message = "Faculty is required") UUID facultyId,
            UUID departmentId,
            @NotNull(message = "Store location is required") UUID locationId,
            @DecimalMin(value = "0", message = "Reorder level cannot be negative") BigDecimal reorderLevel,
            BigDecimal maximumStockLevel,
            @DecimalMin(value = "0", message = "Unit cost cannot be negative") BigDecimal unitCost,
            Boolean hazardous,
            String chemicalClassification,
            String storageInstructions,
            String disposalInstructions,
            Boolean active
    ) {}

    public record ConsumableSummary(
            UUID id, String itemCode, String name, String categoryName,
            String facultyName, String locationName, String unitOfMeasure,
            BigDecimal currentQuantity, BigDecimal reservedQuantity, BigDecimal availableQuantity,
            BigDecimal reorderLevel, boolean lowStock, boolean hazardous, boolean active,
            long batchCount, LocalDate earliestExpiry
    ) {
        public static ConsumableSummary from(ConsumableItem i, long batchCount, LocalDate earliestExpiry) {
            return new ConsumableSummary(i.getId(), i.getItemCode(), i.getName(),
                    i.getCategory().getName(), i.getFaculty().getName(), i.getLocation().getName(),
                    i.getUnitOfMeasure(), i.getCurrentQuantity(), i.getReservedQuantity(),
                    i.getAvailableQuantity(), i.getReorderLevel(),
                    i.getCurrentQuantity().compareTo(i.getReorderLevel()) <= 0,
                    i.isHazardous(), i.isActive(), batchCount, earliestExpiry);
        }
    }

    public record ConsumableDetail(
            UUID id, String itemCode, String name, String description,
            UUID categoryId, String categoryName, String brand, String manufacturer,
            UUID facultyId, String facultyName, UUID departmentId, String departmentName,
            UUID locationId, String locationName, String unitOfMeasure,
            BigDecimal currentQuantity, BigDecimal reservedQuantity, BigDecimal availableQuantity,
            BigDecimal reorderLevel, BigDecimal maximumStockLevel, BigDecimal unitCost,
            boolean hazardous, String chemicalClassification,
            String storageInstructions, String disposalInstructions, boolean active
    ) {
        public static ConsumableDetail from(ConsumableItem i) {
            return new ConsumableDetail(i.getId(), i.getItemCode(), i.getName(), i.getDescription(),
                    i.getCategory().getId(), i.getCategory().getName(), i.getBrand(), i.getManufacturer(),
                    i.getFaculty().getId(), i.getFaculty().getName(),
                    i.getDepartment() != null ? i.getDepartment().getId() : null,
                    i.getDepartment() != null ? i.getDepartment().getName() : null,
                    i.getLocation().getId(), i.getLocation().getName(), i.getUnitOfMeasure(),
                    i.getCurrentQuantity(), i.getReservedQuantity(), i.getAvailableQuantity(),
                    i.getReorderLevel(), i.getMaximumStockLevel(), i.getUnitCost(),
                    i.isHazardous(), i.getChemicalClassification(),
                    i.getStorageInstructions(), i.getDisposalInstructions(), i.isActive());
        }
    }

    public record BatchResponse(
            UUID id, String batchNumber, BigDecimal quantityReceived, BigDecimal quantityRemaining,
            LocalDate manufactureDate, LocalDate expiryDate, BigDecimal unitCost,
            LocalDate receivedDate, boolean expired
    ) {
        public static BatchResponse from(ConsumableBatch b) {
            return new BatchResponse(b.getId(), b.getBatchNumber(), b.getQuantityReceived(),
                    b.getQuantityRemaining(), b.getManufactureDate(), b.getExpiryDate(), b.getUnitCost(),
                    b.getReceivedDate(), b.isExpired());
        }
    }

    public record ReceiveStockRequest(
            @NotNull(message = "Quantity is required")
            @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") BigDecimal quantity,
            @NotBlank(message = "Batch number is required") String batchNumber,
            String purchaseOrderNumber,
            String invoiceNumber,
            BigDecimal unitCost,
            LocalDate manufactureDate,
            LocalDate expiryDate,
            LocalDate receivedDate,
            String notes
    ) {}

    public record IssueStockRequest(
            @NotNull(message = "Quantity is required")
            @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") BigDecimal quantity,
            UUID reservationId,
            String collectionCode,
            UUID issuedToUserId,
            UUID departmentId,
            String courseOrProject,
            String purpose,
            Boolean chargeable,
            BigDecimal chargeAmount,
            String notes
    ) {}

    public record AdjustStockRequest(
            @NotNull(message = "Adjustment type is required") AdjustmentType adjustmentType,
            @NotNull(message = "Quantity is required")
            @DecimalMin(value = "0.001", message = "Quantity must be greater than zero") BigDecimal quantity,
            @NotBlank(message = "Reason is required") String reason,
            String approvalReference,
            String notes
    ) {
        public enum AdjustmentType { INCREASE, DECREASE }
    }

    public record StockTransactionResponse(
            UUID id, StockTransactionType transactionType, BigDecimal quantity,
            String batchNumber, String relatedUserName, String relatedDepartmentName,
            String purpose, String reason, String referenceNumber,
            boolean chargeable, BigDecimal chargeAmount,
            UUID reservationId, String reservationNumber, Instant createdAt
    ) {
        public static StockTransactionResponse from(StockTransaction t) {
            return new StockTransactionResponse(t.getId(), t.getTransactionType(), t.getQuantity(),
                    t.getBatch() != null ? t.getBatch().getBatchNumber() : null,
                    t.getRelatedUser() != null ? t.getRelatedUser().getFullName() : null,
                    t.getRelatedDepartment() != null ? t.getRelatedDepartment().getName() : null,
                    t.getPurpose(), t.getReason(), t.getReferenceNumber(),
                    t.isChargeable(), t.getChargeAmount(),
                    t.getReservation() != null ? t.getReservation().getId() : null,
                    t.getReservation() != null ? t.getReservation().getReservationNumber() : null,
                    t.getCreatedAt());
        }
    }
}
