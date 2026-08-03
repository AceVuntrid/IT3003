package com.university.assets.consumable;

import com.university.assets.audit.AuditService;
import com.university.assets.category.AssetCategoryRepository;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.ReservationStatus;
import com.university.assets.common.model.Enums.StockTransactionType;
import com.university.assets.common.response.PageResponse;
import com.university.assets.consumable.ConsumableBatchRepository;
import com.university.assets.consumable.ConsumableItemRepository;
import com.university.assets.consumable.StockTransactionRepository;
import com.university.assets.consumable.dto.ConsumableDtos.AdjustStockRequest;
import com.university.assets.consumable.dto.ConsumableDtos.BatchResponse;
import com.university.assets.consumable.dto.ConsumableDtos.ConsumableDetail;
import com.university.assets.consumable.dto.ConsumableDtos.ConsumableRequest;
import com.university.assets.consumable.dto.ConsumableDtos.ConsumableSummary;
import com.university.assets.consumable.dto.ConsumableDtos.IssueStockRequest;
import com.university.assets.consumable.dto.ConsumableDtos.ReceiveStockRequest;
import com.university.assets.consumable.dto.ConsumableDtos.StockTransactionResponse;
import com.university.assets.department.DepartmentRepository;
import com.university.assets.faculty.FacultyRepository;
import com.university.assets.location.LocationRepository;
import com.university.assets.notification.NotificationService;
import com.university.assets.reservation.Reservation;
import com.university.assets.reservation.ReservationRepository;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ConsumableService {

    private final ConsumableItemRepository itemRepository;
    private final ConsumableBatchRepository batchRepository;
    private final StockTransactionRepository transactionRepository;
    private final AssetCategoryRepository categoryRepository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public ConsumableService(ConsumableItemRepository itemRepository,
                             ConsumableBatchRepository batchRepository,
                             StockTransactionRepository transactionRepository,
                             AssetCategoryRepository categoryRepository,
                             FacultyRepository facultyRepository,
                             DepartmentRepository departmentRepository,
                             LocationRepository locationRepository,
                             UserRepository userRepository,
                             ReservationRepository reservationRepository,
                             AuditService auditService,
                             NotificationService notificationService) {
        this.itemRepository = itemRepository;
        this.batchRepository = batchRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ConsumableSummary> list(String search, UUID facultyId, UUID categoryId,
                                                Boolean lowStock, Boolean hazardous, Pageable pageable) {
        Specification<ConsumableItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                String like = "%" + search.toLowerCase().trim() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("itemCode")), like)));
            }
            if (facultyId != null) {
                predicates.add(cb.equal(root.get("faculty").get("id"), facultyId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (Boolean.TRUE.equals(lowStock)) {
                predicates.add(cb.lessThanOrEqualTo(root.get("currentQuantity"), root.get("reorderLevel")));
            }
            if (hazardous != null) {
                predicates.add(cb.equal(root.get("hazardous"), hazardous));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(itemRepository.findAll(spec, pageable), item -> {
            List<ConsumableBatch> batches = batchRepository.findByItemIdOrderByReceivedDateDesc(item.getId());
            LocalDate earliestExpiry = batches.stream()
                    .filter(b -> b.getQuantityRemaining().signum() > 0 && b.getExpiryDate() != null)
                    .map(ConsumableBatch::getExpiryDate)
                    .min(Comparator.naturalOrder()).orElse(null);
            return ConsumableSummary.from(item, batches.size(), earliestExpiry);
        });
    }

    @Transactional(readOnly = true)
    public ConsumableDetail get(UUID id) {
        return ConsumableDetail.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> batches(UUID itemId) {
        return batchRepository.findByItemIdOrderByReceivedDateDesc(itemId)
                .stream().map(BatchResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<StockTransactionResponse> transactions(UUID itemId) {
        return transactionRepository.findByItemIdOrderByCreatedAtDesc(itemId)
                .stream().map(StockTransactionResponse::from).toList();
    }

    @Transactional
    public ConsumableDetail create(ConsumableRequest request) {
        String code = request.itemCode() == null || request.itemCode().isBlank()
                ? generateItemCode() : request.itemCode().trim();
        if (itemRepository.existsByItemCodeIgnoreCase(code)) {
            throw ApiException.conflict("Item code already exists");
        }
        ConsumableItem item = new ConsumableItem();
        item.setItemCode(code);
        apply(item, request);
        itemRepository.save(item);
        auditService.log("CREATE", "CONSUMABLE", "ConsumableItem", item.getId(), null,
                Map.of("itemCode", item.getItemCode(), "name", item.getName()));
        return ConsumableDetail.from(item);
    }

    @Transactional
    public ConsumableDetail update(UUID id, ConsumableRequest request) {
        ConsumableItem item = find(id);
        Map<String, Object> old = Map.of("name", item.getName(), "active", item.isActive());
        apply(item, request);
        auditService.log("UPDATE", "CONSUMABLE", "ConsumableItem", item.getId(), old,
                Map.of("name", item.getName(), "active", item.isActive()));
        return ConsumableDetail.from(item);
    }

    @Transactional
    public BatchResponse receive(UUID itemId, ReceiveStockRequest request) {
        ConsumableItem item = find(itemId);
        if (request.expiryDate() != null && request.manufactureDate() != null
                && request.expiryDate().isBefore(request.manufactureDate())) {
            throw ApiException.badRequest("Expiry date cannot be before manufacture date");
        }
        ConsumableBatch batch = new ConsumableBatch();
        batch.setItem(item);
        batch.setBatchNumber(request.batchNumber().trim());
        batch.setQuantityReceived(request.quantity());
        batch.setQuantityRemaining(request.quantity());
        batch.setManufactureDate(request.manufactureDate());
        batch.setExpiryDate(request.expiryDate());
        batch.setUnitCost(request.unitCost());
        batch.setReceivedDate(request.receivedDate() != null ? request.receivedDate() : LocalDate.now());
        batchRepository.save(batch);

        item.setCurrentQuantity(item.getCurrentQuantity().add(request.quantity()));
        if (request.unitCost() != null) {
            item.setUnitCost(request.unitCost());
        }

        StockTransaction tx = newTransaction(item, batch, StockTransactionType.RECEIVE, request.quantity());
        tx.setReferenceNumber(request.purchaseOrderNumber() != null
                ? request.purchaseOrderNumber() : request.invoiceNumber());
        tx.setReason(request.notes());
        transactionRepository.save(tx);

        auditService.log("RECEIVE_STOCK", "CONSUMABLE", "ConsumableItem", item.getId(), null,
                Map.of("batch", batch.getBatchNumber(), "quantity", request.quantity().toString()));
        return BatchResponse.from(batch);
    }

    @Transactional
    public List<StockTransactionResponse> issue(UUID itemId, IssueStockRequest request) {
        ConsumableItem item = find(itemId);

        // Fulfilment of an approved consumable reservation: validate the link, the
        // collection code and the remaining approved quantity before touching stock.
        Reservation reservation = null;
        BigDecimal reservationRemaining = null;
        if (request.reservationId() != null) {
            reservation = resolveApprovedReservation(item, request);
            reservationRemaining = remainingReservationQuantity(reservation);
            if (reservationRemaining.signum() <= 0) {
                throw ApiException.badRequest("This reservation has already been fully issued");
            }
            if (request.quantity().compareTo(reservationRemaining) > 0) {
                throw ApiException.badRequest("Cannot issue "
                        + request.quantity().stripTrailingZeros().toPlainString()
                        + " " + item.getUnitOfMeasure() + ": only "
                        + reservationRemaining.stripTrailingZeros().toPlainString()
                        + " of " + reservation.getQuantity() + " remain to be issued");
            }
        }

        if (item.getAvailableQuantity().compareTo(request.quantity()) < 0) {
            throw ApiException.badRequest("Insufficient stock. Available: "
                    + item.getAvailableQuantity().stripTrailingZeros().toPlainString()
                    + " " + item.getUnitOfMeasure());
        }
        // FEFO issue across batches, skipping expired ones (expired chemicals cannot be issued).
        List<ConsumableBatch> batches = batchRepository.findIssuableBatches(itemId).stream()
                .filter(b -> !b.isExpired())
                .toList();
        BigDecimal issuable = batches.stream()
                .map(ConsumableBatch::getQuantityRemaining)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (issuable.compareTo(request.quantity()) < 0) {
            throw ApiException.badRequest(
                    "Insufficient non-expired batch stock. Expired batches cannot be issued.");
        }

        List<StockTransaction> transactions = new ArrayList<>();
        BigDecimal remaining = request.quantity();
        for (ConsumableBatch batch : batches) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal take = batch.getQuantityRemaining().min(remaining);
            batch.setQuantityRemaining(batch.getQuantityRemaining().subtract(take));
            remaining = remaining.subtract(take);

            StockTransaction tx = newTransaction(item, batch, StockTransactionType.ISSUE, take);
            if (request.issuedToUserId() != null) {
                tx.setRelatedUser(userRepository.findById(request.issuedToUserId())
                        .orElseThrow(() -> ApiException.notFound("User")));
            } else if (reservation != null) {
                tx.setRelatedUser(reservation.getRequestedBy());
            }
            tx.setRelatedDepartment(request.departmentId() == null ? null
                    : departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> ApiException.notFound("Department")));
            tx.setPurpose(request.purpose() != null ? request.purpose() : request.courseOrProject());
            tx.setReason(request.notes());
            tx.setReservation(reservation);
            if (reservation != null) {
                // The price-list fee carried by the reservation is the single source
                // of charge; a reservation-linked issue must never charge again.
                tx.setReferenceNumber(reservation.getReservationNumber());
                tx.setChargeable(false);
                tx.setChargeAmount(null);
            } else {
                tx.setChargeable(Boolean.TRUE.equals(request.chargeable()));
                tx.setChargeAmount(request.chargeAmount());
            }
            transactions.add(transactionRepository.save(tx));
        }
        item.setCurrentQuantity(item.getCurrentQuantity().subtract(request.quantity()));

        if (reservation != null && request.quantity().compareTo(reservationRemaining) == 0) {
            // Fully issued: consumables never come back, so the reservation goes
            // straight to COMPLETED (no CHECKED_OUT stage).
            reservation.setStatus(ReservationStatus.COMPLETED);
            notificationService.notifyUser(reservation.getRequestedBy().getId(),
                    "RESERVATION_COMPLETED",
                    "Reservation " + reservation.getReservationNumber() + " completed",
                    "All approved stock of " + item.getName() + " has been issued to you.",
                    "Reservation", reservation.getId());
        }

        Map<String, Object> auditDetails = new LinkedHashMap<>();
        auditDetails.put("quantity", request.quantity().toString());
        if (reservation != null) {
            auditDetails.put("reservation", reservation.getReservationNumber());
        }
        auditService.log("ISSUE_STOCK", "CONSUMABLE", "ConsumableItem", item.getId(), null, auditDetails);
        return transactions.stream().map(StockTransactionResponse::from).toList();
    }

    /**
     * Validates that the reservation referenced by an issue is an approved
     * consumable reservation for this very item, and that the borrower quoted
     * the right collection code.
     */
    private Reservation resolveApprovedReservation(ConsumableItem item, IssueStockRequest request) {
        Reservation reservation = reservationRepository.findDetailedById(request.reservationId())
                .orElseThrow(() -> ApiException.notFound("Reservation"));
        if (reservation.getConsumableItem() == null) {
            throw ApiException.badRequest("This reservation is not for a consumable item");
        }
        if (!reservation.getConsumableItem().getId().equals(item.getId())) {
            throw ApiException.badRequest("This reservation is for a different consumable item");
        }
        if (reservation.getStatus() != ReservationStatus.APPROVED
                && reservation.getStatus() != ReservationStatus.READY_FOR_COLLECTION) {
            throw ApiException.badRequest("Only approved reservations can be issued against");
        }
        validateCollectionCode(reservation, request.collectionCode());
        return reservation;
    }

    /** Approved quantity minus everything already issued against the reservation. */
    private BigDecimal remainingReservationQuantity(Reservation reservation) {
        BigDecimal alreadyIssued = transactionRepository.findByReservationId(reservation.getId())
                .stream()
                .map(StockTransaction::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return BigDecimal.valueOf(reservation.getQuantity()).subtract(alreadyIssued);
    }

    /**
     * Same semantics as CheckoutService#validateCollectionCode: staff type in
     * whatever code the requester quotes at collection, proving they are present.
     * Reservations approved before codes existed (null stored code) pass with a
     * null/blank code.
     */
    private static void validateCollectionCode(Reservation reservation, String providedCode) {
        String stored = reservation.getCollectionCode();
        String provided = providedCode != null ? providedCode.trim() : "";
        if (stored == null ? !provided.isEmpty() : !stored.equals(provided)) {
            throw ApiException.badRequest("Invalid collection code");
        }
    }

    @Transactional
    public ConsumableDetail adjust(UUID itemId, AdjustStockRequest request) {
        ConsumableItem item = find(itemId);
        BigDecimal before = item.getCurrentQuantity();
        if (request.adjustmentType() == AdjustStockRequest.AdjustmentType.INCREASE) {
            item.setCurrentQuantity(before.add(request.quantity()));
        } else {
            if (before.compareTo(request.quantity()) < 0) {
                throw ApiException.badRequest("Stock cannot become negative");
            }
            item.setCurrentQuantity(before.subtract(request.quantity()));
            // Reduce batch remainders FEFO-style so batch totals stay consistent.
            BigDecimal remaining = request.quantity();
            for (ConsumableBatch batch : batchRepository.findIssuableBatches(itemId)) {
                if (remaining.signum() <= 0) {
                    break;
                }
                BigDecimal take = batch.getQuantityRemaining().min(remaining);
                batch.setQuantityRemaining(batch.getQuantityRemaining().subtract(take));
                remaining = remaining.subtract(take);
            }
        }
        StockTransaction tx = newTransaction(item, null,
                request.adjustmentType() == AdjustStockRequest.AdjustmentType.INCREASE
                        ? StockTransactionType.ADJUST_INCREASE : StockTransactionType.ADJUST_DECREASE,
                request.quantity());
        tx.setReason(request.reason());
        tx.setReferenceNumber(request.approvalReference());
        transactionRepository.save(tx);

        auditService.log("ADJUST_STOCK", "CONSUMABLE", "ConsumableItem", item.getId(),
                Map.of("quantity", before.toString()),
                Map.of("quantity", item.getCurrentQuantity().toString(), "reason", request.reason()));
        return ConsumableDetail.from(item);
    }

    @Transactional(readOnly = true)
    public List<ConsumableSummary> lowStock() {
        return itemRepository.findLowStock().stream()
                .map(i -> ConsumableSummary.from(i, 0, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> expiring(int days) {
        return batchRepository.findExpiringBefore(LocalDate.now().plusDays(days))
                .stream().map(BatchResponse::from).toList();
    }

    private ConsumableItem find(UUID id) {
        return itemRepository.findDetailedById(id)
                .orElseThrow(() -> ApiException.notFound("Consumable item"));
    }

    private StockTransaction newTransaction(ConsumableItem item, ConsumableBatch batch,
                                            StockTransactionType type, BigDecimal quantity) {
        StockTransaction tx = new StockTransaction();
        tx.setItem(item);
        tx.setBatch(batch);
        tx.setTransactionType(type);
        tx.setQuantity(quantity);
        tx.setCreatedBy(CurrentUser.id());
        return tx;
    }

    private void apply(ConsumableItem item, ConsumableRequest r) {
        item.setName(r.name().trim());
        item.setDescription(r.description());
        item.setCategory(categoryRepository.findById(r.categoryId())
                .orElseThrow(() -> ApiException.notFound("Category")));
        item.setBrand(r.brand());
        item.setManufacturer(r.manufacturer());
        item.setUnitOfMeasure(r.unitOfMeasure().trim());
        var faculty = facultyRepository.findById(r.facultyId())
                .orElseThrow(() -> ApiException.notFound("Faculty"));
        item.setFaculty(faculty);
        if (r.departmentId() != null) {
            var department = departmentRepository.findById(r.departmentId())
                    .orElseThrow(() -> ApiException.notFound("Department"));
            if (!department.getFaculty().getId().equals(faculty.getId())) {
                throw ApiException.badRequest("Department does not belong to the selected faculty");
            }
            item.setDepartment(department);
        } else {
            item.setDepartment(null);
        }
        item.setLocation(locationRepository.findById(r.locationId())
                .orElseThrow(() -> ApiException.notFound("Location")));
        if (r.reorderLevel() != null) {
            item.setReorderLevel(r.reorderLevel());
        }
        item.setMaximumStockLevel(r.maximumStockLevel());
        if (r.unitCost() != null) {
            item.setUnitCost(r.unitCost());
        }
        if (r.hazardous() != null) {
            item.setHazardous(r.hazardous());
        }
        item.setChemicalClassification(r.chemicalClassification());
        item.setStorageInstructions(r.storageInstructions());
        item.setDisposalInstructions(r.disposalInstructions());
        if (r.active() != null) {
            item.setActive(r.active());
        }
    }

    private String generateItemCode() {
        long count = itemRepository.count() + 1;
        String code;
        do {
            code = String.format("CON-%05d", count++);
        } while (itemRepository.existsByItemCodeIgnoreCase(code));
        return code;
    }
}
