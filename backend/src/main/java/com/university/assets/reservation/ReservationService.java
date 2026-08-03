package com.university.assets.reservation;

import com.university.assets.asset.Asset;
import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.checkout.CheckoutRepository;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AccountStatus;
import com.university.assets.common.model.Enums.ApprovalStatus;
import com.university.assets.common.model.Enums.ApprovalStep;
import com.university.assets.common.model.Enums.ApprovalTier;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.PayerType;
import com.university.assets.common.model.Enums.PaymentStatus;
import com.university.assets.common.model.Enums.ReservationStatus;
import com.university.assets.common.model.Enums.TransactionType;
import com.university.assets.common.response.PageResponse;
import com.university.assets.consumable.ConsumableItem;
import com.university.assets.consumable.ConsumableItemRepository;
import com.university.assets.department.Department;
import com.university.assets.faculty.Faculty;
import com.university.assets.location.Location;
import com.university.assets.location.LocationRepository;
import com.university.assets.notification.NotificationService;
import com.university.assets.payment.Payment;
import com.university.assets.payment.PaymentRepository;
import com.university.assets.reservation.dto.ReservationDtos.ApprovalRequest;
import com.university.assets.reservation.dto.ReservationDtos.AvailabilityResponse;
import com.university.assets.reservation.dto.ReservationDtos.PendingApprover;
import com.university.assets.reservation.dto.ReservationDtos.ReservationRequest;
import com.university.assets.reservation.dto.ReservationDtos.ReservationResponse;
import com.university.assets.role.Permissions;
import com.university.assets.role.Role;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    /** Statuses that hold capacity against an asset or location. */
    static final Set<ReservationStatus> ACTIVE_STATUSES = EnumSet.of(
            ReservationStatus.SUBMITTED, ReservationStatus.PENDING_APPROVAL,
            ReservationStatus.APPROVED, ReservationStatus.READY_FOR_COLLECTION,
            ReservationStatus.CHECKED_OUT, ReservationStatus.OVERDUE);

    /**
     * Statuses of consumable reservations that hold (claim) stock. Consumables
     * are consumed, so availability is stock-based, not window-based: fully
     * issued reservations go straight to COMPLETED and release their claim.
     * READY_FOR_COLLECTION is included defensively — stock issue accepts it as
     * an issuable state, so it must keep claiming stock too.
     */
    static final Set<ReservationStatus> CONSUMABLE_CLAIM_STATUSES = EnumSet.of(
            ReservationStatus.PENDING_APPROVAL, ReservationStatus.APPROVED,
            ReservationStatus.READY_FOR_COLLECTION);

    private static final UUID NO_EXCLUDE = new UUID(0, 0);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ReservationRepository reservationRepository;
    private final AssetRepository assetRepository;
    private final LocationRepository locationRepository;
    private final ConsumableItemRepository consumableItemRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final CheckoutRepository checkoutRepository;
    private final ApprovalScopeService approvalScopeService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public ReservationService(ReservationRepository reservationRepository,
                              AssetRepository assetRepository,
                              LocationRepository locationRepository,
                              ConsumableItemRepository consumableItemRepository,
                              UserRepository userRepository,
                              PaymentRepository paymentRepository,
                              CheckoutRepository checkoutRepository,
                              ApprovalScopeService approvalScopeService,
                              AuditService auditService,
                              NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.assetRepository = assetRepository;
        this.locationRepository = locationRepository;
        this.consumableItemRepository = consumableItemRepository;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.checkoutRepository = checkoutRepository;
        this.approvalScopeService = approvalScopeService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationResponse> list(ReservationStatus status, ApprovalStatus approvalStatus,
                                                  UUID assetId, UUID requestedById, Instant from, Instant to,
                                                  boolean mineOnly, Pageable pageable) {
        ViewerScope scope = viewerScope();
        Specification<Reservation> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (mineOnly) {
                predicates.add(cb.equal(root.get("requestedBy").get("id"), scope.viewerId));
            } else if (scope.viewAll) {
                if (requestedById != null) {
                    predicates.add(cb.equal(root.get("requestedBy").get("id"), requestedById));
                }
            } else {
                // Non-global viewers see their own reservations, plus whatever
                // their duties require: scoped approvers the reservations of
                // their unit, stock-issue staff (storekeepers) every consumable
                // reservation. The collection code stays hidden from them —
                // toResponse only exposes it to the requester.
                List<Predicate> visible = new ArrayList<>();
                visible.add(cb.equal(root.get("requestedBy").get("id"), scope.viewerId));
                if (scope.consumables) {
                    visible.add(cb.isNotNull(root.get("consumableItem")));
                }
                if (scope.isScopedApprover()) {
                    // Left joins keep reservations of the other two target kinds
                    // eligible for the OR (an implicit .get() join would be an
                    // inner join and silently drop them). The nested .get("id")
                    // hops compile to the joined row's FK column — no extra join.
                    Join<Object, Object> asset = root.join("asset", JoinType.LEFT);
                    Join<Object, Object> venue = root.join("location", JoinType.LEFT);
                    Join<Object, Object> item = root.join("consumableItem", JoinType.LEFT);
                    if (scope.departmentId != null) {
                        visible.add(cb.equal(asset.get("department").get("id"), scope.departmentId));
                        visible.add(cb.equal(item.get("department").get("id"), scope.departmentId));
                        visible.add(cb.equal(venue.get("department").get("id"), scope.departmentId));
                    }
                    if (scope.facultyId != null) {
                        visible.add(cb.equal(asset.get("faculty").get("id"), scope.facultyId));
                        visible.add(cb.equal(item.get("faculty").get("id"), scope.facultyId));
                        visible.add(cb.equal(venue.get("faculty").get("id"), scope.facultyId));
                    }
                    if (scope.caretaker) {
                        if (!scope.caretakerLocationIds.isEmpty()) {
                            visible.add(asset.get("location").get("id").in(scope.caretakerLocationIds));
                            visible.add(item.get("location").get("id").in(scope.caretakerLocationIds));
                            visible.add(root.get("location").get("id").in(scope.caretakerLocationIds));
                        }
                        visible.add(cb.equal(asset.get("custodian").get("id"), scope.viewerId));
                    }
                }
                predicates.add(cb.or(visible.toArray(new Predicate[0])));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (approvalStatus != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), approvalStatus));
            }
            if (assetId != null) {
                predicates.add(cb.equal(root.get("asset").get("id"), assetId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        PendingApproverResolver resolver = new PendingApproverResolver();
        return PageResponse.from(reservationRepository.findAll(spec, pageable),
                r -> toResponse(r, resolver));
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(UUID id) {
        Reservation reservation = find(id);
        if (!reservation.getRequestedBy().getId().equals(CurrentUser.id())) {
            ViewerScope scope = viewerScope();
            // Same rule as list(): stock-issue staff may view consumable
            // reservations; scoped approvers only their unit's reservations.
            boolean canViewConsumable = reservation.getConsumableItem() != null && scope.consumables;
            if (!scope.viewAll && !canViewConsumable && !scope.allows(reservation)) {
                throw ApiException.forbidden("You can only view your own reservations");
            }
        }
        return toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> calendar(Instant from, Instant to) {
        PendingApproverResolver resolver = new PendingApproverResolver();
        return reservationRepository
                .findByStartAtLessThanAndEndAtGreaterThanAndStatusIn(to, from, ACTIVE_STATUSES)
                .stream().map(r -> toResponse(r, resolver)).toList();
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(UUID assetId, Instant startAt, Instant endAt,
                                             int quantity, UUID excludeReservationId) {
        Asset asset = assetRepository.findDetailedById(assetId)
                .orElseThrow(() -> ApiException.notFound("Asset"));
        List<String> blockers = assetBlockers(asset, startAt, endAt, quantity);
        UUID exclude = excludeReservationId != null ? excludeReservationId : NO_EXCLUDE;
        int reserved = reservationRepository.reservedQuantityInWindow(
                assetId, startAt, endAt, ACTIVE_STATUSES, exclude);
        int availableInWindow = asset.getQuantity() - reserved;
        if (availableInWindow < quantity) {
            blockers.add("Only " + Math.max(availableInWindow, 0)
                    + " of " + asset.getQuantity() + " units are free in this time window");
        }
        PendingApproverResolver resolver = new PendingApproverResolver();
        List<ReservationResponse> overlapping = reservationRepository
                .findByStartAtLessThanAndEndAtGreaterThanAndStatusIn(endAt, startAt, ACTIVE_STATUSES)
                .stream()
                .filter(r -> r.getAsset() != null && r.getAsset().getId().equals(assetId))
                .map(r -> toResponse(r, resolver))
                .toList();
        return new AvailabilityResponse(blockers.isEmpty(), quantity, asset.getQuantity(),
                reserved, Math.max(availableInWindow, 0), blockers, overlapping);
    }

    @Transactional
    public ReservationResponse create(ReservationRequest request) {
        int targets = (request.assetId() != null ? 1 : 0)
                + (request.locationId() != null ? 1 : 0)
                + (request.consumableItemId() != null ? 1 : 0);
        if (targets != 1) {
            throw ApiException.badRequest(
                    "Select exactly one of an asset, a room/facility, or a consumable item to reserve");
        }
        if (!request.startAt().isBefore(request.endAt())) {
            throw ApiException.badRequest("Start date must be before end date");
        }
        if (request.endAt().isBefore(Instant.now())) {
            throw ApiException.badRequest("Reservation cannot end in the past");
        }
        int quantity = request.quantity() != null ? request.quantity() : 1;

        User requestedBy;
        if (request.requestedForUserId() != null
                && CurrentUser.hasAuthority(Permissions.RESERVATION_MANAGE)) {
            requestedBy = userRepository.findById(request.requestedForUserId())
                    .orElseThrow(() -> ApiException.notFound("User"));
        } else {
            requestedBy = userRepository.findById(CurrentUser.id())
                    .orElseThrow(() -> ApiException.notFound("User"));
        }

        // Reservation limit per user.
        if (requestedBy.getReservationLimit() != null) {
            long open = reservationRepository.countByRequestedByIdAndStatusIn(
                    requestedBy.getId(), ACTIVE_STATUSES);
            if (open >= requestedBy.getReservationLimit()) {
                throw ApiException.badRequest("Reservation limit reached ("
                        + requestedBy.getReservationLimit() + " active reservations)");
            }
        }

        Reservation reservation = new Reservation();

        if (request.assetId() != null) {
            Asset asset = assetRepository.findDetailedById(request.assetId())
                    .orElseThrow(() -> ApiException.notFound("Asset"));
            List<String> blockers = assetBlockers(asset, request.startAt(), request.endAt(), quantity);
            if (Boolean.TRUE.equals(request.externalUseRequested()) && !asset.isExternalUseAllowed()) {
                blockers.add("This asset cannot be taken outside campus");
            }
            int reserved = reservationRepository.reservedQuantityInWindow(
                    asset.getId(), request.startAt(), request.endAt(), ACTIVE_STATUSES, NO_EXCLUDE);
            if (asset.getQuantity() - reserved < quantity) {
                blockers.add("The requested quantity is not available in this time window");
            }
            if (!blockers.isEmpty()) {
                throw ApiException.badRequest(String.join(". ", blockers));
            }
            reservation.setAsset(asset);
        }
        if (request.locationId() != null) {
            Location location = locationRepository.findById(request.locationId())
                    .orElseThrow(() -> ApiException.notFound("Location"));
            long conflicts = reservationRepository.locationConflicts(
                    location.getId(), request.startAt(), request.endAt(), ACTIVE_STATUSES, NO_EXCLUDE);
            if (conflicts > 0) {
                throw ApiException.conflict("The room or facility is already reserved in this time window");
            }
            reservation.setLocation(location);
        }
        if (request.consumableItemId() != null) {
            // Consumables have no time-window semantics — they are consumed.
            // startAt/endAt are recorded as the collection window only, and
            // availability is stock-based: on-hand stock minus units already
            // claimed by other open consumable reservations for this item.
            ConsumableItem item = consumableItemRepository.findDetailedById(request.consumableItemId())
                    .orElseThrow(() -> ApiException.notFound("Consumable item"));
            if (!item.isActive()) {
                throw ApiException.badRequest("This consumable item is inactive and cannot be reserved");
            }
            int claimed = reservationRepository.consumableClaimedQuantity(
                    item.getId(), CONSUMABLE_CLAIM_STATUSES, NO_EXCLUDE);
            BigDecimal free = item.getCurrentQuantity().subtract(BigDecimal.valueOf(claimed));
            if (free.compareTo(BigDecimal.valueOf(quantity)) < 0) {
                throw ApiException.badRequest("Only "
                        + free.max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()
                        + " " + item.getUnitOfMeasure() + " of " + item.getName()
                        + " can be reserved right now (" + claimed
                        + " already claimed by other pending or approved reservations)");
            }
            reservation.setConsumableItem(item);
        }

        reservation.setReservationNumber(generateNumber());
        reservation.setRequestedBy(requestedBy);
        reservation.setFaculty(requestedBy.getFaculty());
        reservation.setDepartment(requestedBy.getDepartment());
        reservation.setPurpose(request.purpose().trim());
        reservation.setCourseOrProject(request.courseOrProject());
        reservation.setStartAt(request.startAt());
        reservation.setEndAt(request.endAt());
        reservation.setQuantity(quantity);
        reservation.setRequestedQuantity(quantity);
        reservation.setParticipantCount(request.participantCount());
        reservation.setSpecialRequirements(request.specialRequirements());
        reservation.setExternalUseRequested(Boolean.TRUE.equals(request.externalUseRequested()));

        ApprovalTier requiredTier = evaluateRequiredTier(reservation);
        reservation.setRequiredApprovalTier(requiredTier);

        reservation.setStatus(ReservationStatus.PENDING_APPROVAL);
        reservation.setApprovalStatus(requiredTier == ApprovalTier.TIER_1_OFFICER ? ApprovalStatus.PENDING : ApprovalStatus.PENDING_LEVEL_1);
        reservation.setCurrentApprovalStep(ApprovalStep.PENDING_LEVEL_1);
        reservationRepository.save(reservation);

        auditService.log("CREATE", "RESERVATION", "Reservation", reservation.getId(), null,
                Map.of("number", reservation.getReservationNumber(),
                        "status", reservation.getStatus().name(),
                        "approvalTier", requiredTier.name()));
        notificationService.notifyUser(requestedBy.getId(), "RESERVATION_SUBMITTED",
                "Reservation " + reservation.getReservationNumber() + " submitted",
                "Your reservation is awaiting " + requiredTier.name() + " approval.",
                "Reservation", reservation.getId());
        return toResponse(reservation);
    }

    private ApprovalTier evaluateRequiredTier(Reservation reservation) {
        if (reservation.isExternalUseRequested()) {
            return ApprovalTier.TIER_3_HOD;
        }
        if (reservation.getAsset() != null) {
            if (reservation.getAsset().getPurchasePrice() != null
                    && reservation.getAsset().getPurchasePrice().doubleValue() > 500000.0) {
                return ApprovalTier.TIER_3_HOD;
            }
            if (reservation.getAsset().getCategory() != null
                    && reservation.getAsset().getCategory().getRequiredApprovalTier() != null) {
                return reservation.getAsset().getCategory().getRequiredApprovalTier();
            }
        }
        return ApprovalTier.TIER_1_OFFICER;
    }

    @Transactional
    public ReservationResponse approve(UUID id, ApprovalRequest request) {
        Reservation reservation = find(id);
        requirePending(reservation);
        User approver = requireScopedApprover(reservation);
        if (request.approvedQuantity() != null && request.approvedQuantity() < reservation.getQuantity()) {
            if (request.approvedQuantity() < 1) {
                throw ApiException.badRequest("Approved quantity must be at least 1");
            }
            reservation.setQuantity(request.approvedQuantity());
        }

        ApprovalTier tier = reservation.getRequiredApprovalTier();
        ApprovalStep step = reservation.getCurrentApprovalStep();

        if (tier == ApprovalTier.TIER_1_OFFICER || step == ApprovalStep.PENDING_LEVEL_2) {
            // Final approval level reached
            reservation.setStatus(ReservationStatus.APPROVED);
            reservation.setApprovalStatus(ApprovalStatus.APPROVED);
            reservation.setCurrentApprovalStep(ApprovalStep.APPROVED);
            reservation.setApprovedBy(approver);
            reservation.setApprovedAt(Instant.now());
            if (step == ApprovalStep.PENDING_LEVEL_2) {
                reservation.setLevel2ApprovedBy(approver);
                reservation.setLevel2ApprovedAt(Instant.now());
            }
            reservation.setApprovalNotes(appendFinalNotes(reservation.getApprovalNotes(), request.notes()));

            // Pricing is auto-computed from the price list at the final approval —
            // approvers no longer set fees (asset flat reservation fee, venue flat
            // booking fee, consumable unit fee × approved quantity). feeWaived is
            // no longer an approver decision either: it records "no charge applied"
            // for history clarity when the price list yields no (or zero) fee.
            BigDecimal feeAmount = ReservationResponse.applicableFee(reservation);
            boolean feeWaived = feeAmount == null || feeAmount.signum() <= 0;
            reservation.setFeeAmount(feeAmount);
            reservation.setFeeWaived(feeWaived);
            boolean feePayable = !feeWaived;
            if (feePayable) {
                createFeePayment(reservation, approver, feeAmount);
            }

            // Anything physically collected gets a collection code the requester
            // must quote at handover: assets and consumable stock. Venue/room
            // bookings never check out, so no code is issued.
            boolean codeGenerated = reservation.getAsset() != null
                    || reservation.getConsumableItem() != null;
            if (codeGenerated) {
                reservation.setCollectionCode(String.format("%04d", SECURE_RANDOM.nextInt(10000)));
            }

            Map<String, Object> auditDetails = new LinkedHashMap<>();
            auditDetails.put("number", reservation.getReservationNumber());
            auditDetails.put("stage", "FINAL");
            if (feeAmount != null) {
                auditDetails.put("feeAmount", feeAmount.toPlainString());
            }
            auditDetails.put("feeWaived", feeWaived);
            if (codeGenerated) {
                // The code value itself is confidential and never audited.
                auditDetails.put("code_generated", true);
            }
            auditService.log("APPROVE", "RESERVATION", "Reservation", reservation.getId(), null,
                    auditDetails);
            String approvalMessage = feePayable
                    ? "Your reservation has been approved — fee LKR " + feeAmount.toPlainString() + "."
                    : "Your reservation has been approved — free of charge.";
            if (codeGenerated) {
                approvalMessage += " Your collection code is " + reservation.getCollectionCode() + ".";
            }
            notificationService.notifyUser(reservation.getRequestedBy().getId(), "RESERVATION_APPROVED",
                    "Reservation " + reservation.getReservationNumber() + " fully approved",
                    approvalMessage,
                    "Reservation", reservation.getId());
        } else {
            // Level 1 approval completed, advance to Level 2 (HOD / Lab Manager)
            reservation.setCurrentApprovalStep(ApprovalStep.PENDING_LEVEL_2);
            reservation.setApprovalStatus(ApprovalStatus.PENDING_LEVEL_2);
            reservation.setLevel1ApprovedBy(approver);
            reservation.setLevel1ApprovedAt(Instant.now());
            if (request.notes() != null) {
                reservation.setApprovalNotes("Level 1 Note: " + request.notes());
            }
            auditService.log("APPROVE_STEP1", "RESERVATION", "Reservation", reservation.getId(), null,
                    Map.of("number", reservation.getReservationNumber(), "nextStage", "LEVEL_2"));
            notificationService.notifyUser(reservation.getRequestedBy().getId(), "RESERVATION_STEP1_APPROVED",
                    "Reservation " + reservation.getReservationNumber() + " passed Level 1 verification",
                    "Passed Level 1 approval. Awaiting final Level 2 approval.", "Reservation", reservation.getId());
        }
        // The approver receives the response: the code stays hidden from them
        // unless they happen to be the requester themselves.
        return toResponse(reservation);
    }

    @Transactional
    public ReservationResponse reject(UUID id, ApprovalRequest request) {
        Reservation reservation = find(id);
        requirePending(reservation);
        User approver = requireScopedApprover(reservation);
        reservation.setStatus(ReservationStatus.REJECTED);
        reservation.setApprovalStatus(ApprovalStatus.REJECTED);
        reservation.setApprovedBy(approver);
        reservation.setApprovedAt(Instant.now());
        reservation.setApprovalNotes(request.notes());
        auditService.log("REJECT", "RESERVATION", "Reservation", reservation.getId(), null,
                Map.of("number", reservation.getReservationNumber()));
        notificationService.notifyUser(reservation.getRequestedBy().getId(), "RESERVATION_REJECTED",
                "Reservation " + reservation.getReservationNumber() + " rejected",
                request.notes() != null ? request.notes() : "Your reservation was rejected.",
                "Reservation", reservation.getId());
        return toResponse(reservation);
    }

    /**
     * Cancels a reservation. The requester may cancel their own reservation at
     * any status before physical fulfilment: for assets, while no checkout slip
     * is un-returned; for consumables, while nothing has been issued (partial
     * issues close as COMPLETED instead); for venues, until the booking starts —
     * after startAt only SUPER_ADMIN (or RESERVATION_MANAGE) may cancel.
     * SUPER_ADMIN and RESERVATION_MANAGE may cancel anyone's reservation, under
     * the same fulfilment guards. Everyone else is forbidden.
     */
    @Transactional
    public ReservationResponse cancel(UUID id) {
        Reservation reservation = find(id);
        boolean isOwner = reservation.getRequestedBy().getId().equals(CurrentUser.id());
        boolean isManager = CurrentUser.hasAuthority(Permissions.RESERVATION_MANAGE);
        boolean isSuperAdmin = CurrentUser.hasAuthority("ROLE_" + ApprovalScopeService.ROLE_SUPER_ADMIN);
        if (!isOwner && !isManager && !isSuperAdmin) {
            throw ApiException.forbidden("You can only cancel your own reservations");
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw ApiException.badRequest("Completed reservations cannot be cancelled");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw ApiException.badRequest("This reservation is already cancelled");
        }
        if (reservation.getStatus() == ReservationStatus.REJECTED) {
            throw ApiException.badRequest("Rejected reservations cannot be cancelled");
        }
        if (reservation.getLocation() != null && !isSuperAdmin && !isManager
                && !Instant.now().isBefore(reservation.getStartAt())) {
            throw ApiException.forbidden(
                    "A venue booking that has already started can only be cancelled by an administrator");
        }
        if (reservation.getConsumableItem() != null
                && (reservation.getStatus() == ReservationStatus.APPROVED
                        || reservation.getStatus() == ReservationStatus.READY_FOR_COLLECTION)) {
            // Consumable analogue of the checked-out rule below: issued stock is
            // consumed and never comes back, so once anything has been issued the
            // reservation can no longer be cancelled — it closes as COMPLETED and
            // the un-issued remainder is released. With nothing issued it cancels
            // normally (falls through).
            BigDecimal issued = reservationRepository.issuedAgainstReservation(reservation.getId());
            if (issued != null && issued.signum() > 0) {
                reservation.setStatus(ReservationStatus.COMPLETED);
                auditService.log("COMPLETE", "RESERVATION", "Reservation", reservation.getId(), null,
                        Map.of("number", reservation.getReservationNumber(),
                                "reason", "Closed after partial issue; remaining quantity released"));
                return toResponse(reservation);
            }
        }
        if (reservation.getStatus() == ReservationStatus.CHECKED_OUT) {
            // A partially issued reservation whose issued items have all come back can
            // be closed: the un-issued remainder is released and the reservation
            // completes instead of staying CHECKED_OUT forever. While anything is
            // still out, closing it remains forbidden.
            boolean anyOutstanding = checkoutRepository.findByReservationId(reservation.getId())
                    .stream().anyMatch(c -> c.getReturnedAt() == null);
            if (anyOutstanding) {
                throw ApiException.badRequest(
                        "Checked-out reservations cannot be cancelled while items are still out");
            }
            reservation.setStatus(ReservationStatus.COMPLETED);
            auditService.log("COMPLETE", "RESERVATION", "Reservation", reservation.getId(), null,
                    Map.of("number", reservation.getReservationNumber(),
                            "reason", "Closed after all issued items were returned"));
            return toResponse(reservation);
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        auditService.log("CANCEL", "RESERVATION", "Reservation", reservation.getId(), null,
                Map.of("number", reservation.getReservationNumber()));
        settleLinkedPaymentsOnCancel(reservation);
        return toResponse(reservation);
    }

    /**
     * Settles fee payments linked to a just-cancelled reservation: a still
     * PENDING payment is cancelled with it (and audited); a PAID one cannot be
     * silently undone, so every active FINANCE_OFFICER is notified that a
     * refund may be due.
     */
    private void settleLinkedPaymentsOnCancel(Reservation reservation) {
        List<Payment> payments = paymentRepository.findAll((root, query, cb) ->
                cb.equal(root.get("reservation").get("id"), reservation.getId()));
        for (Payment payment : payments) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(payment);
                auditService.log("CANCEL", "PAYMENT", "Payment", payment.getId(), null,
                        Map.of("number", payment.getTransactionNumber(),
                                "reason", "Reservation " + reservation.getReservationNumber()
                                        + " cancelled"));
            } else if (payment.getStatus() == PaymentStatus.PAID) {
                for (User officer : activeUsersWithRole(ApprovalScopeService.ROLE_FINANCE_OFFICER)) {
                    notificationService.notifyUser(officer.getId(), "RESERVATION_CANCELLED_PAID",
                            "Reservation " + reservation.getReservationNumber()
                                    + " cancelled — paid fee may need refund",
                            "Reservation " + reservation.getReservationNumber()
                                    + " was cancelled after payment " + payment.getTransactionNumber()
                                    + " (" + payment.getCurrency() + " "
                                    + payment.getAmount().toPlainString()
                                    + ") was received. A refund may be due.",
                            "Reservation", reservation.getId());
                }
            }
        }
    }

    private List<User> activeUsersWithRole(String roleName) {
        return userRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.join("roles").get("name"), roleName),
                cb.equal(root.get("accountStatus"), AccountStatus.ACTIVE)));
    }

    /**
     * Maps a reservation to its API response, exposing the collection code only
     * when the current user is the requester. Approvers, managers and other
     * viewers always receive a {@code null} code.
     */
    private ReservationResponse toResponse(Reservation reservation) {
        return toResponse(reservation, new PendingApproverResolver());
    }

    private ReservationResponse toResponse(Reservation reservation, PendingApproverResolver resolver) {
        return ReservationResponse.from(reservation,
                reservation.getRequestedBy().getId().equals(CurrentUser.id()),
                resolver.resolve(reservation));
    }

    // ---------- viewer visibility scope (list/get) ----------

    /**
     * What the current user may see. Global viewers (RESERVATION_MANAGE, or
     * RESERVATION_APPROVE combined with the SUPER_ADMIN/ASSET_ADMIN role) see
     * everything. Non-global RESERVATION_APPROVE holders are scoped approvers:
     * they see their own reservations plus the ones of the unit they approve
     * for — their department (DEPT_ADMIN/LAB_MANAGER), their faculty
     * (FACULTY_DEAN/FACULTY_ADMIN), or as CARETAKER the locations they are
     * responsible for (including descendants) and assets in their custody.
     */
    static final class ViewerScope {
        final UUID viewerId;
        final boolean viewAll;
        /** CONSUMABLE_ISSUE: fulfilment staff see all consumable reservations. */
        final boolean consumables;
        final UUID departmentId;
        final UUID facultyId;
        final boolean caretaker;
        final Set<UUID> caretakerLocationIds;

        ViewerScope(UUID viewerId, boolean viewAll, boolean consumables, UUID departmentId,
                    UUID facultyId, boolean caretaker, Set<UUID> caretakerLocationIds) {
            this.viewerId = viewerId;
            this.viewAll = viewAll;
            this.consumables = consumables;
            this.departmentId = departmentId;
            this.facultyId = facultyId;
            this.caretaker = caretaker;
            this.caretakerLocationIds = caretakerLocationIds;
        }

        boolean isScopedApprover() {
            return departmentId != null || facultyId != null || caretaker;
        }

        /** Java-side equivalent of the list() Specification prefilter. */
        boolean allows(Reservation r) {
            if (r.getRequestedBy().getId().equals(viewerId)) {
                return true;
            }
            Department dept = itemDepartment(r);
            if (departmentId != null && dept != null && departmentId.equals(dept.getId())) {
                return true;
            }
            Faculty faculty = itemFaculty(r);
            if (facultyId != null && faculty != null && facultyId.equals(faculty.getId())) {
                return true;
            }
            if (caretaker) {
                if (r.getAsset() != null && r.getAsset().getCustodian() != null
                        && viewerId.equals(r.getAsset().getCustodian().getId())) {
                    return true;
                }
                Location itemLocation = itemLocation(r);
                if (itemLocation != null && caretakerLocationIds.contains(itemLocation.getId())) {
                    return true;
                }
            }
            return false;
        }
    }

    ViewerScope viewerScope() {
        UUID viewerId = CurrentUser.id();
        boolean manage = CurrentUser.hasAuthority(Permissions.RESERVATION_MANAGE);
        boolean approve = CurrentUser.hasAuthority(Permissions.RESERVATION_APPROVE);
        boolean globalRole = CurrentUser.hasAuthority("ROLE_" + ApprovalScopeService.ROLE_SUPER_ADMIN)
                || CurrentUser.hasAuthority("ROLE_" + ApprovalScopeService.ROLE_ASSET_ADMIN);
        boolean viewAll = manage || (approve && globalRole);
        boolean consumables = CurrentUser.hasAuthority(Permissions.CONSUMABLE_ISSUE);
        UUID departmentId = null;
        UUID facultyId = null;
        boolean caretaker = false;
        Set<UUID> caretakerLocationIds = Set.of();
        if (approve && !viewAll) {
            User viewer = userRepository.findWithRolesById(viewerId).orElse(null);
            if (viewer != null) {
                Set<String> roles = viewer.getRoles().stream()
                        .map(Role::getName).collect(Collectors.toSet());
                if ((roles.contains(ApprovalScopeService.ROLE_DEPT_ADMIN)
                        || roles.contains(ApprovalScopeService.ROLE_LAB_MANAGER))
                        && viewer.getDepartment() != null) {
                    departmentId = viewer.getDepartment().getId();
                }
                if ((roles.contains(ApprovalScopeService.ROLE_FACULTY_DEAN)
                        || roles.contains(ApprovalScopeService.ROLE_FACULTY_ADMIN))
                        && viewer.getFaculty() != null) {
                    facultyId = viewer.getFaculty().getId();
                }
                if (roles.contains(ApprovalScopeService.ROLE_CARETAKER)) {
                    caretaker = true;
                    caretakerLocationIds = caretakerLocationIds(viewerId);
                }
            }
        }
        return new ViewerScope(viewerId, viewAll, consumables, departmentId, facultyId,
                caretaker, caretakerLocationIds);
    }

    /**
     * Ids of every location the user is responsible for, plus all their
     * descendants, computed in Java by walking children — the locations table
     * is small.
     */
    private Set<UUID> caretakerLocationIds(UUID userId) {
        List<Location> all = locationRepository.findAll();
        Map<UUID, List<Location>> childrenByParent = new HashMap<>();
        Deque<Location> queue = new ArrayDeque<>();
        for (Location location : all) {
            if (location.getParent() != null) {
                childrenByParent.computeIfAbsent(location.getParent().getId(), k -> new ArrayList<>())
                        .add(location);
            }
            if (location.getResponsibleUser() != null
                    && userId.equals(location.getResponsibleUser().getId())) {
                queue.add(location);
            }
        }
        Set<UUID> ids = new HashSet<>();
        while (!queue.isEmpty()) {
            Location location = queue.poll();
            if (ids.add(location.getId())) {
                queue.addAll(childrenByParent.getOrDefault(location.getId(), List.of()));
            }
        }
        return ids;
    }

    /** Owning department of the reserved item, per the custodianship chain. */
    private static Department itemDepartment(Reservation r) {
        if (r.getAsset() != null) return r.getAsset().getDepartment();
        if (r.getConsumableItem() != null) return r.getConsumableItem().getDepartment();
        if (r.getLocation() != null) return r.getLocation().getDepartment();
        return null;
    }

    /** Owning faculty of the reserved item. */
    private static Faculty itemFaculty(Reservation r) {
        if (r.getAsset() != null) return r.getAsset().getFaculty();
        if (r.getConsumableItem() != null) return r.getConsumableItem().getFaculty();
        if (r.getLocation() != null) return r.getLocation().getFaculty();
        return null;
    }

    /** Physical location of the reserved item (the venue itself for bookings). */
    private static Location itemLocation(Reservation r) {
        if (r.getAsset() != null) return r.getAsset().getLocation();
        if (r.getConsumableItem() != null) return r.getConsumableItem().getLocation();
        return r.getLocation();
    }

    // ---------- pending approver resolution ----------

    /** Safety cap when walking a location parent chain. */
    private static final int MAX_LOCATION_DEPTH = 25;

    /**
     * Resolves who a PENDING_APPROVAL reservation is waiting on, via the
     * custodianship chain: department-owned → the department's first active
     * DEPT_ADMIN; faculty-owned → the faculty's active FACULTY_DEAN; unowned →
     * the asset's custodian or the responsible user of the item's location or
     * an ancestor. Lookups are cached per department/faculty for the lifetime
     * of the resolver (one API request), so a list page does at most one user
     * query per distinct unit.
     */
    private final class PendingApproverResolver {
        private final Map<UUID, Optional<User>> deptAdminByDept = new HashMap<>();
        private final Map<UUID, Optional<User>> deanByFaculty = new HashMap<>();

        PendingApprover resolve(Reservation r) {
            if (r.getStatus() != ReservationStatus.PENDING_APPROVAL) {
                return null;
            }
            Department dept = itemDepartment(r);
            if (dept != null) {
                return deptAdminByDept
                        .computeIfAbsent(dept.getId(), id ->
                                firstActiveApprover(ApprovalScopeService.ROLE_DEPT_ADMIN, "department", id))
                        .map(admin -> new PendingApprover(admin.getFullName(),
                                deptAdminLabel(dept.getName())))
                        .orElse(null);
            }
            Faculty faculty = itemFaculty(r);
            if (faculty != null) {
                return deanByFaculty
                        .computeIfAbsent(faculty.getId(), id ->
                                firstActiveApprover(ApprovalScopeService.ROLE_FACULTY_DEAN, "faculty", id))
                        .map(dean -> new PendingApprover(dean.getFullName(),
                                "Dean, " + faculty.getName()))
                        .orElse(null);
            }
            if (r.getAsset() != null && r.getAsset().getCustodian() != null
                    && r.getAsset().getCustodian().getAccountStatus() == AccountStatus.ACTIVE) {
                User custodian = r.getAsset().getCustodian();
                Location assetLocation = r.getAsset().getLocation();
                return new PendingApprover(custodian.getFullName(),
                        assetLocation != null ? "Caretaker, " + assetLocation.getName() : "Caretaker");
            }
            Location location = itemLocation(r);
            int depth = 0;
            while (location != null && depth++ < MAX_LOCATION_DEPTH) {
                User responsible = location.getResponsibleUser();
                if (responsible != null && responsible.getAccountStatus() == AccountStatus.ACTIVE) {
                    return new PendingApprover(responsible.getFullName(),
                            "Caretaker, " + location.getName());
                }
                location = location.getParent();
            }
            return null;
        }

        /** First active holder of the role within the department/faculty unit. */
        private Optional<User> firstActiveApprover(String roleName, String unitAttribute, UUID unitId) {
            return userRepository.findAll((root, query, cb) -> cb.and(
                            cb.equal(root.join("roles").get("name"), roleName),
                            cb.equal(root.get(unitAttribute).get("id"), unitId),
                            cb.equal(root.get("accountStatus"), AccountStatus.ACTIVE)))
                    .stream()
                    .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE)
                    .findFirst();
        }
    }

    Reservation find(UUID id) {
        return reservationRepository.findDetailedById(id)
                .orElseThrow(() -> ApiException.notFound("Reservation"));
    }

    /**
     * Loads the current user (with roles/faculty/department) and verifies they are
     * the approving authority for the reservation's unit per the custodianship rules.
     */
    private User requireScopedApprover(Reservation reservation) {
        User approver = userRepository.findWithRolesById(CurrentUser.id())
                .orElseThrow(() -> ApiException.notFound("User"));
        if (!approvalScopeService.canApprove(approver, reservation)) {
            throw ApiException.forbidden("You are not the approver for this item's unit");
        }
        return approver;
    }

    /** Raises a pending RESERVATION_FEE payment for the requester at final approval. */
    private void createFeePayment(Reservation reservation, User approver, BigDecimal feeAmount) {
        Payment payment = new Payment();
        payment.setTransactionNumber(String.format("PAY-%05d", paymentRepository.count() + 1));
        payment.setTransactionType(TransactionType.RESERVATION_FEE);
        payment.setPayerType(PayerType.USER);
        payment.setPayerUser(reservation.getRequestedBy());
        payment.setReservation(reservation);
        payment.setAsset(reservation.getAsset());
        payment.setDescription("Reservation fee " + reservation.getReservationNumber());
        payment.setAmount(feeAmount);
        payment.setCurrency("LKR");
        payment.setPaymentMethod("Pending");
        payment.setPaymentDate(Instant.now());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedBy(approver.getId());
        paymentRepository.save(payment);
        auditService.log("CREATE", "PAYMENT", "Payment", payment.getId(), null,
                Map.of("number", payment.getTransactionNumber(),
                        "type", TransactionType.RESERVATION_FEE.name(),
                        "amount", feeAmount.toPlainString(),
                        "reservation", reservation.getReservationNumber()));
    }

    private void requirePending(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.PENDING_APPROVAL
                && reservation.getStatus() != ReservationStatus.SUBMITTED) {
            throw ApiException.badRequest("Only pending reservations can be approved or rejected");
        }
    }

    private List<String> assetBlockers(Asset asset, Instant startAt, Instant endAt, int quantity) {
        List<String> blockers = new ArrayList<>();
        if (asset.isArchived()) {
            blockers.add("Archived assets cannot be reserved");
        }
        if (!asset.isReservable()) {
            blockers.add("This asset is not reservable");
        }
        if (asset.getStatus() == AssetStatus.UNDER_MAINTENANCE) {
            blockers.add("The asset is under maintenance");
        }
        if (asset.getStatus() == AssetStatus.DAMAGED || asset.getStatus() == AssetStatus.LOST
                || asset.getStatus() == AssetStatus.DISPOSED) {
            blockers.add("Damaged, lost or disposed assets cannot be reserved");
        }
        if (asset.getMaxReservationHours() != null) {
            long hours = java.time.Duration.between(startAt, endAt).toHours();
            if (hours > asset.getMaxReservationHours()) {
                blockers.add("Reservation exceeds the maximum duration of "
                        + asset.getMaxReservationHours() + " hours");
            }
        }
        return blockers;
    }

    private String generateNumber() {
        long count = reservationRepository.count() + 1;
        return String.format("RSV-%05d", count);
    }

    /** Department names already ending in "Department" must not read "X Department Department Admin". */
    private static String deptAdminLabel(String departmentName) {
        String base = departmentName.trim();
        if (base.toLowerCase().endsWith(" department")) {
            base = base.substring(0, base.length() - " department".length());
        }
        return base + " Department Admin";
    }

    /**
     * Keeps any Level 1 note recorded on the first hop instead of overwriting it with
     * the final approver's note.
     */
    private static String appendFinalNotes(String existing, String finalNotes) {
        if (finalNotes == null || finalNotes.isBlank()) {
            return existing;
        }
        return existing == null || existing.isBlank() ? finalNotes : existing + "\n" + finalNotes;
    }
}
