package com.university.assets.checkout;

import com.university.assets.asset.Asset;
import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.CheckoutStatus;
import com.university.assets.common.model.Enums.ReservationStatus;
import com.university.assets.common.response.PageResponse;
import com.university.assets.config.AppProperties;
import com.university.assets.checkout.dto.CheckoutDtos.CheckoutRequest;
import com.university.assets.checkout.dto.CheckoutDtos.CheckoutResponse;
import com.university.assets.checkout.dto.CheckoutDtos.ExtendRequest;
import com.university.assets.checkout.dto.CheckoutDtos.ReturnRequest;
import com.university.assets.notification.NotificationService;
import com.university.assets.reservation.Reservation;
import com.university.assets.reservation.ReservationRepository;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class CheckoutService {

    private final CheckoutRepository checkoutRepository;
    private final ReservationRepository reservationRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final AppProperties properties;

    public CheckoutService(CheckoutRepository checkoutRepository,
                           ReservationRepository reservationRepository,
                           AssetRepository assetRepository,
                           UserRepository userRepository,
                           AuditService auditService,
                           NotificationService notificationService,
                           AppProperties properties) {
        this.checkoutRepository = checkoutRepository;
        this.reservationRepository = reservationRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public PageResponse<CheckoutResponse> list(CheckoutStatus status, UUID assetId, UUID userId,
                                               Boolean overdueOnly, Pageable pageable) {
        Specification<Checkout> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (assetId != null) {
                predicates.add(cb.equal(root.get("asset").get("id"), assetId));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (Boolean.TRUE.equals(overdueOnly)) {
                predicates.add(cb.and(
                        cb.isNull(root.get("returnedAt")),
                        cb.lessThan(root.get("expectedReturnAt"), Instant.now())));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(checkoutRepository.findAll(spec, pageable), CheckoutResponse::from);
    }

    @Transactional(readOnly = true)
    public CheckoutResponse get(UUID id) {
        return CheckoutResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<CheckoutResponse> overdue() {
        return checkoutRepository.findByStatusInAndExpectedReturnAtBefore(
                        EnumSet.of(CheckoutStatus.CHECKED_OUT, CheckoutStatus.OVERDUE), Instant.now())
                .stream().map(CheckoutResponse::from).toList();
    }

    @Transactional
    public CheckoutResponse checkOut(CheckoutRequest request) {
        Asset asset;
        User user;
        Reservation reservation = null;
        int quantity;

        if (request.reservationId() != null) {
            reservation = reservationRepository.findDetailedById(request.reservationId())
                    .orElseThrow(() -> ApiException.notFound("Reservation"));
            if (reservation.getStatus() != ReservationStatus.APPROVED
                    && reservation.getStatus() != ReservationStatus.READY_FOR_COLLECTION
                    && reservation.getStatus() != ReservationStatus.CHECKED_OUT) {
                throw ApiException.badRequest("Only approved reservations can be checked out");
            }
            if (reservation.getAsset() == null) {
                throw ApiException.badRequest("This reservation is for a room or facility, not an asset");
            }
            validateCollectionCode(reservation, request.collectionCode());
            int alreadyIssued = checkoutRepository.findByReservationId(reservation.getId())
                    .stream().mapToInt(Checkout::getQuantity).sum();
            int remaining = reservation.getQuantity() - alreadyIssued;
            if (remaining <= 0) {
                throw ApiException.badRequest("This reservation has already been fully issued");
            }
            quantity = request.quantity() != null ? request.quantity() : remaining;
            if (quantity > remaining) {
                throw ApiException.badRequest("Cannot issue " + quantity + " item(s): only "
                        + remaining + " of " + reservation.getQuantity() + " remain to be issued");
            }
            asset = resolveSourceAsset(reservation, request.assetId());
            user = reservation.getRequestedBy();
        } else {
            if (request.assetId() == null || request.userId() == null) {
                throw ApiException.badRequest("Asset and user are required for a direct check-out");
            }
            asset = assetRepository.findDetailedById(request.assetId())
                    .orElseThrow(() -> ApiException.notFound("Asset"));
            user = userRepository.findById(request.userId())
                    .orElseThrow(() -> ApiException.notFound("User"));
            quantity = request.quantity() != null ? request.quantity() : 1;
        }

        if (asset.isArchived() || asset.getStatus() == AssetStatus.UNDER_MAINTENANCE
                || asset.getStatus() == AssetStatus.LOST || asset.getStatus() == AssetStatus.DISPOSED) {
            throw ApiException.badRequest("This asset is not available for check-out");
        }
        if (asset.getAvailableQuantity() < quantity) {
            throw ApiException.badRequest("Insufficient available quantity. Available: "
                    + asset.getAvailableQuantity());
        }
        if (asset.isDepositRequired()
                && (request.depositPaid() == null
                || request.depositPaid().compareTo(asset.getDepositAmount()) < 0)) {
            throw ApiException.badRequest("A deposit of " + asset.getDepositAmount()
                    + " " + asset.getCurrency() + " must be paid before check-out");
        }
        Instant expectedReturn = reservation != null ? reservation.getEndAt() : request.expectedReturnAt();
        if (expectedReturn == null) {
            throw ApiException.badRequest("Expected return date and time is required");
        }

        Checkout checkout = new Checkout();
        checkout.setCheckoutNumber(String.format("CHK-%05d", checkoutRepository.count() + 1));
        checkout.setReservation(reservation);
        checkout.setAsset(asset);
        checkout.setUser(user);
        checkout.setQuantity(quantity);
        checkout.setCheckedOutAt(Instant.now());
        checkout.setExpectedReturnAt(expectedReturn);
        checkout.setConditionBefore(request.conditionBefore() != null
                ? request.conditionBefore() : asset.getCondition());
        checkout.setAccessories(request.accessories());
        checkout.setDepositPaid(request.depositPaid());
        checkout.setIssuedBy(userRepository.getReferenceById(CurrentUser.id()));
        checkout.setNotes(request.notes());
        checkoutRepository.save(checkout);

        asset.setAvailableQuantity(asset.getAvailableQuantity() - quantity);
        if (asset.getAvailableQuantity() == 0) {
            asset.setStatus(AssetStatus.CHECKED_OUT);
        }
        if (reservation != null) {
            reservation.setStatus(ReservationStatus.CHECKED_OUT);
        }

        Map<String, Object> auditDetails = new LinkedHashMap<>();
        auditDetails.put("number", checkout.getCheckoutNumber());
        auditDetails.put("asset", asset.getAssetCode());
        auditDetails.put("user", user.getEmail());
        auditDetails.put("quantity", String.valueOf(quantity));
        if (reservation != null && asset != reservation.getAsset()) {
            auditDetails.put("reservedAsset", reservation.getAsset().getAssetCode());
        }
        auditService.log("CHECK_OUT", "CHECKOUT", "Checkout", checkout.getId(), null, auditDetails);
        notificationService.notifyUser(user.getId(), "ASSET_CHECKED_OUT",
                "Asset checked out: " + asset.getName(),
                "Please return it by " + expectedReturn + ".", "Checkout", checkout.getId());
        return CheckoutResponse.from(checkout);
    }

    @Transactional
    public CheckoutResponse processReturn(UUID id, ReturnRequest request) {
        Checkout checkout = find(id);
        if (checkout.getReturnedAt() != null) {
            throw ApiException.badRequest("This checkout has already been returned");
        }
        if (Boolean.TRUE.equals(request.damageDetected())
                && (request.damageDescription() == null || request.damageDescription().isBlank())) {
            throw ApiException.badRequest("Damage description is required when damage is detected");
        }

        Instant returnedAt = Instant.now();
        checkout.setReturnedAt(returnedAt);
        checkout.setConditionAfter(request.conditionAfter() != null
                ? request.conditionAfter() : checkout.getConditionBefore());
        checkout.setMissingAccessories(request.missingAccessories());
        checkout.setDamageDetected(Boolean.TRUE.equals(request.damageDetected()));
        checkout.setDamageDescription(request.damageDescription());
        checkout.setReceivedBy(userRepository.getReferenceById(CurrentUser.id()));
        checkout.setStatus(CheckoutStatus.RETURNED);
        if (request.notes() != null) {
            checkout.setNotes(request.notes());
        }

        // Late penalty (per started day) from configuration, overridable per return.
        if (request.penaltyAmount() != null) {
            checkout.setPenaltyAmount(request.penaltyAmount());
        } else if (returnedAt.isAfter(checkout.getExpectedReturnAt())) {
            long lateDays = Math.max(1, Duration.between(
                    checkout.getExpectedReturnAt(), returnedAt).toDays());
            checkout.setPenaltyAmount(properties.reservation().overduePenaltyPerDay()
                    .multiply(BigDecimal.valueOf(lateDays)));
        }

        Asset asset = checkout.getAsset();
        asset.setAvailableQuantity(
                Math.min(asset.getQuantity(), asset.getAvailableQuantity() + checkout.getQuantity()));
        if (Boolean.TRUE.equals(request.damageDetected())) {
            asset.setCondition(request.conditionAfter() != null
                    ? request.conditionAfter() : AssetCondition.DAMAGED);
            asset.setStatus(Boolean.TRUE.equals(request.sendToMaintenance())
                    ? AssetStatus.UNDER_MAINTENANCE : AssetStatus.DAMAGED);
        } else if (asset.getStatus() == AssetStatus.CHECKED_OUT) {
            asset.setStatus(AssetStatus.AVAILABLE);
        }

        // The reservation is completed only once every issue slip has come back
        // and nothing remains to be issued against it.
        if (checkout.getReservation() != null) {
            Reservation reservation = checkout.getReservation();
            List<Checkout> issues = checkoutRepository.findByReservationId(reservation.getId());
            int issuedTotal = issues.stream().mapToInt(Checkout::getQuantity).sum();
            boolean allReturned = issues.stream().allMatch(c -> c.getReturnedAt() != null);
            if (allReturned && issuedTotal >= reservation.getQuantity()) {
                reservation.setStatus(ReservationStatus.COMPLETED);
            }
        }

        auditService.log("RETURN", "CHECKOUT", "Checkout", checkout.getId(), null,
                Map.of("number", checkout.getCheckoutNumber(),
                        "damage", String.valueOf(checkout.isDamageDetected()),
                        "penalty", checkout.getPenaltyAmount() != null
                                ? checkout.getPenaltyAmount().toString() : "0"));
        return CheckoutResponse.from(checkout);
    }

    @Transactional
    public CheckoutResponse extend(UUID id, ExtendRequest request) {
        Checkout checkout = find(id);
        if (checkout.getReturnedAt() != null) {
            throw ApiException.badRequest("Returned checkouts cannot be extended");
        }
        if (!request.newExpectedReturnAt().isAfter(checkout.getExpectedReturnAt())) {
            throw ApiException.badRequest("The new return date must be after the current one");
        }
        Instant old = checkout.getExpectedReturnAt();
        checkout.setExpectedReturnAt(request.newExpectedReturnAt());
        if (checkout.getStatus() == CheckoutStatus.OVERDUE
                && request.newExpectedReturnAt().isAfter(Instant.now())) {
            checkout.setStatus(CheckoutStatus.CHECKED_OUT);
        }
        auditService.log("EXTEND", "CHECKOUT", "Checkout", checkout.getId(),
                Map.of("expectedReturnAt", old.toString()),
                Map.of("expectedReturnAt", request.newExpectedReturnAt().toString()));
        notificationService.notifyUser(checkout.getUser().getId(), "RETURN_EXTENDED",
                "Return date extended for " + checkout.getAsset().getName(),
                "New return date: " + request.newExpectedReturnAt(), "Checkout", checkout.getId());
        return CheckoutResponse.from(checkout);
    }

    /**
     * Resolves which asset the items are issued from. By default it is the reserved
     * asset; a different asset of the same category may be supplied to fulfil the
     * reservation from another location ("same stuff, different place").
     */
    private Asset resolveSourceAsset(Reservation reservation, UUID requestedAssetId) {
        Asset reserved = reservation.getAsset();
        if (requestedAssetId == null || requestedAssetId.equals(reserved.getId())) {
            return reserved;
        }
        Asset source = assetRepository.findDetailedById(requestedAssetId)
                .orElseThrow(() -> ApiException.notFound("Asset"));
        if (reserved.getCategory() == null || source.getCategory() == null
                || !Objects.equals(reserved.getCategory().getId(), source.getCategory().getId())) {
            throw ApiException.badRequest(
                    "The issuing asset must belong to the same category as the reserved asset ("
                            + (reserved.getCategory() != null
                            ? reserved.getCategory().getName() : "uncategorised") + ")");
        }
        return source;
    }

    /**
     * The requester receives a 4-digit collection code when their asset
     * reservation is finally approved; staff type in whatever code the borrower
     * quotes at handover, proving the requester is present. Reservations approved
     * before codes existed (null stored code) pass with a null/blank code.
     */
    private static void validateCollectionCode(Reservation reservation, String providedCode) {
        String stored = reservation.getCollectionCode();
        String provided = providedCode != null ? providedCode.trim() : "";
        if (stored == null ? !provided.isEmpty() : !stored.equals(provided)) {
            throw ApiException.badRequest("Invalid collection code");
        }
    }

    private Checkout find(UUID id) {
        return checkoutRepository.findDetailedById(id)
                .orElseThrow(() -> ApiException.notFound("Checkout"));
    }
}
