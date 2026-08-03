package com.university.assets.checkout.dto;

import com.university.assets.checkout.Checkout;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.CheckoutStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class CheckoutDtos {

    private CheckoutDtos() {}

    public record CheckoutRequest(
            UUID reservationId,
            UUID assetId,
            UUID userId,
            @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
            AssetCondition conditionBefore,
            String accessories,
            Instant expectedReturnAt,
            BigDecimal depositPaid,
            String notes,
            String collectionCode
    ) {}

    public record ReturnRequest(
            AssetCondition conditionAfter,
            String missingAccessories,
            Boolean damageDetected,
            String damageDescription,
            Boolean sendToMaintenance,
            BigDecimal penaltyAmount,
            String notes
    ) {}

    public record ExtendRequest(
            @NotNull(message = "New return date is required") Instant newExpectedReturnAt
    ) {}

    public record CheckoutResponse(
            UUID id,
            String checkoutNumber,
            UUID reservationId,
            String reservationNumber,
            UUID assetId,
            String assetName,
            String assetCode,
            UUID userId,
            String userName,
            String userEmail,
            int quantity,
            Instant checkedOutAt,
            Instant expectedReturnAt,
            Instant returnedAt,
            AssetCondition conditionBefore,
            AssetCondition conditionAfter,
            String accessories,
            String missingAccessories,
            boolean damageDetected,
            String damageDescription,
            BigDecimal depositPaid,
            BigDecimal penaltyAmount,
            String issuedByName,
            String receivedByName,
            CheckoutStatus status,
            String notes,
            long daysOverdue
    ) {
        public static CheckoutResponse from(Checkout c) {
            long daysOverdue = 0;
            Instant reference = c.getReturnedAt() != null ? c.getReturnedAt() : Instant.now();
            if (reference.isAfter(c.getExpectedReturnAt())) {
                daysOverdue = java.time.Duration.between(c.getExpectedReturnAt(), reference).toDays();
            }
            return new CheckoutResponse(
                    c.getId(), c.getCheckoutNumber(),
                    c.getReservation() != null ? c.getReservation().getId() : null,
                    c.getReservation() != null ? c.getReservation().getReservationNumber() : null,
                    c.getAsset().getId(), c.getAsset().getName(), c.getAsset().getAssetCode(),
                    c.getUser().getId(), c.getUser().getFullName(), c.getUser().getEmail(),
                    c.getQuantity(), c.getCheckedOutAt(), c.getExpectedReturnAt(), c.getReturnedAt(),
                    c.getConditionBefore(), c.getConditionAfter(),
                    c.getAccessories(), c.getMissingAccessories(),
                    c.isDamageDetected(), c.getDamageDescription(),
                    c.getDepositPaid(), c.getPenaltyAmount(),
                    c.getIssuedBy().getFullName(),
                    c.getReceivedBy() != null ? c.getReceivedBy().getFullName() : null,
                    c.getStatus(), c.getNotes(), daysOverdue);
        }
    }
}
