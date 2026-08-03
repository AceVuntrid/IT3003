package com.university.assets.reservation.dto;

import com.university.assets.common.model.Enums.ApprovalStatus;
import com.university.assets.common.model.Enums.ReservationStatus;
import com.university.assets.reservation.Reservation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ReservationDtos {

    private ReservationDtos() {}

    /** Exactly one of assetId / locationId / consumableItemId must be set. */
    public record ReservationRequest(
            UUID assetId,
            UUID locationId,
            UUID consumableItemId,
            @NotBlank(message = "Purpose is required") String purpose,
            String courseOrProject,
            @NotNull(message = "Start date and time is required") Instant startAt,
            @NotNull(message = "End date and time is required") Instant endAt,
            @Min(value = 1, message = "Quantity must be at least 1") Integer quantity,
            Integer participantCount,
            String specialRequirements,
            Boolean externalUseRequested,
            UUID requestedForUserId
    ) {}

    /**
     * Approval decision payload. Fees are no longer set by approvers — they come
     * from the price list at final approval (see {@link ReservationResponse#applicableFee}).
     * Unknown legacy properties (feeAmount/feeWaived) from old clients are ignored
     * by Jackson.
     */
    public record ApprovalRequest(String notes, Integer approvedQuantity) {}

    /**
     * Who a PENDING_APPROVAL reservation is currently waiting on, resolved via
     * the custodianship chain (department admin → faculty dean → caretaker).
     * {@code role} is a human-readable label, e.g. "Physics Department Admin",
     * "Dean, Faculty of Science" or "Caretaker, Main Building".
     */
    public record PendingApprover(String name, String role) {}

    public record ReservationResponse(
            UUID id,
            String reservationNumber,
            UUID assetId,
            String assetName,
            String assetCode,
            UUID locationId,
            String locationName,
            UUID consumableItemId,
            String consumableItemName,
            String consumableItemCode,
            String consumableUnit,
            UUID requestedById,
            String requestedByName,
            String facultyName,
            String departmentName,
            String purpose,
            String courseOrProject,
            Instant startAt,
            Instant endAt,
            int quantity,
            Integer requestedQuantity,
            int issuedQuantity,
            Integer participantCount,
            String specialRequirements,
            boolean externalUseRequested,
            ReservationStatus status,
            ApprovalStatus approvalStatus,
            String requiredApprovalTier,
            String currentApprovalStep,
            String approvedByName,
            Instant approvedAt,
            String approvalNotes,
            BigDecimal feeAmount,
            boolean feeWaived,
            BigDecimal applicableFee,
            String collectionCode,
            Instant createdAt,
            PendingApprover pendingApprover
    ) {
        /**
         * Maps a reservation to its response. The collection code is confidential:
         * pass {@code includeCode=true} only when the current user is the requester —
         * everyone else (approvers, staff) receives {@code null}.
         * {@code pendingApprover} is non-null only for PENDING_APPROVAL reservations
         * whose awaiting authority could be resolved (see ReservationService).
         */
        public static ReservationResponse from(Reservation r, boolean includeCode,
                                               PendingApprover pendingApprover) {
            var item = r.getConsumableItem();
            return new ReservationResponse(
                    r.getId(), r.getReservationNumber(),
                    r.getAsset() != null ? r.getAsset().getId() : null,
                    r.getAsset() != null ? r.getAsset().getName() : null,
                    r.getAsset() != null ? r.getAsset().getAssetCode() : null,
                    r.getLocation() != null ? r.getLocation().getId() : null,
                    r.getLocation() != null ? r.getLocation().getName() : null,
                    item != null ? item.getId() : null,
                    item != null ? item.getName() : null,
                    item != null ? item.getItemCode() : null,
                    item != null ? item.getUnitOfMeasure() : null,
                    r.getRequestedBy().getId(), r.getRequestedBy().getFullName(),
                    r.getFaculty() != null ? r.getFaculty().getName() : null,
                    r.getDepartment() != null ? r.getDepartment().getName() : null,
                    r.getPurpose(), r.getCourseOrProject(), r.getStartAt(), r.getEndAt(),
                    r.getQuantity(), r.getRequestedQuantity(), r.getIssuedQuantity(),
                    r.getParticipantCount(), r.getSpecialRequirements(),
                    r.isExternalUseRequested(), r.getStatus(), r.getApprovalStatus(),
                    r.getRequiredApprovalTier() != null ? r.getRequiredApprovalTier().name() : "TIER_1_OFFICER",
                    r.getCurrentApprovalStep() != null ? r.getCurrentApprovalStep().name() : "PENDING_LEVEL_1",
                    r.getApprovedBy() != null ? r.getApprovedBy().getFullName() : null,
                    r.getApprovedAt(), r.getApprovalNotes(),
                    r.getFeeAmount(), r.isFeeWaived(),
                    applicableFee(r),
                    includeCode ? r.getCollectionCode() : null,
                    r.getCreatedAt(),
                    pendingApprover);
        }

        /**
         * The price-list fee that would apply to this reservation right now:
         * asset → flat reservation fee, venue → flat booking fee, consumable →
         * unit fee × current (requested or approved) quantity. Returns {@code null}
         * when the item has no price ("free"). Used both as the pre-approval
         * preview shown to approvers and as the authoritative amount computed at
         * final approval.
         */
        public static BigDecimal applicableFee(Reservation r) {
            if (r.getAsset() != null) {
                return r.getAsset().getReservationFee();
            }
            if (r.getLocation() != null) {
                return r.getLocation().getBookingFee();
            }
            if (r.getConsumableItem() != null) {
                BigDecimal unitFee = r.getConsumableItem().getUnitFee();
                return unitFee != null ? unitFee.multiply(BigDecimal.valueOf(r.getQuantity())) : null;
            }
            return null;
        }
    }

    public record AvailabilityResponse(
            boolean available,
            int requestedQuantity,
            int totalQuantity,
            int reservedInWindow,
            int availableInWindow,
            List<String> blockers,
            List<ReservationResponse> overlapping
    ) {}
}
