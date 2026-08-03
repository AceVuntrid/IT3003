package com.university.assets.payment;

import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.PayerType;
import com.university.assets.common.model.Enums.PaymentStatus;
import com.university.assets.common.model.Enums.TransactionType;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.common.response.PageResponse;
import com.university.assets.department.DepartmentRepository;
import com.university.assets.notification.NotificationService;
import com.university.assets.reservation.ReservationRepository;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Payments and Charges")
public class PaymentController {

    public record PaymentRequest(
            @NotNull(message = "Transaction type is required") TransactionType transactionType,
            @NotNull(message = "Payer type is required") PayerType payerType,
            UUID payerUserId,
            UUID payerDepartmentId,
            String payerName,
            UUID reservationId,
            UUID assetId,
            String description,
            @NotNull(message = "Amount is required")
            @DecimalMin(value = "0.01", message = "Amount must be greater than zero") BigDecimal amount,
            String currency,
            @NotBlank(message = "Payment method is required") String paymentMethod,
            String referenceNumber,
            Instant paymentDate,
            PaymentStatus status,
            String notes
    ) {}

    public record MarkPaidRequest(
            @NotBlank(message = "Payment method is required") String method,
            String referenceNumber,
            String notes
    ) {}

    public record RefundRequest(
            @NotNull(message = "Refund amount is required")
            @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero") BigDecimal amount,
            @NotBlank(message = "Refund reason is required") String reason,
            String method,
            String referenceNumber
    ) {}

    public record PaymentResponse(
            UUID id, String transactionNumber, TransactionType transactionType, PayerType payerType,
            String payerDisplayName, String reservationNumber, String assetName, String description,
            BigDecimal amount, String currency, String paymentMethod, String referenceNumber,
            Instant paymentDate, PaymentStatus status, BigDecimal refundedAmount,
            String originalTransactionNumber, String notes
    ) {
        static PaymentResponse from(Payment p) {
            String payerDisplayName = p.getPayerUser() != null ? p.getPayerUser().getFullName()
                    : p.getPayerDepartment() != null ? p.getPayerDepartment().getName()
                    : p.getPayerName();
            return new PaymentResponse(p.getId(), p.getTransactionNumber(), p.getTransactionType(),
                    p.getPayerType(), payerDisplayName,
                    p.getReservation() != null ? p.getReservation().getReservationNumber() : null,
                    p.getAsset() != null ? p.getAsset().getName() : null,
                    p.getDescription(), p.getAmount(), p.getCurrency(), p.getPaymentMethod(),
                    p.getReferenceNumber(), p.getPaymentDate(), p.getStatus(), p.getRefundedAmount(),
                    p.getOriginalPayment() != null ? p.getOriginalPayment().getTransactionNumber() : null,
                    p.getNotes());
        }
    }

    private final PaymentRepository repository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ReservationRepository reservationRepository;
    private final AssetRepository assetRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public PaymentController(PaymentRepository repository, UserRepository userRepository,
                             DepartmentRepository departmentRepository,
                             ReservationRepository reservationRepository,
                             AssetRepository assetRepository, AuditService auditService,
                             NotificationService notificationService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.reservationRepository = reservationRepository;
        this.assetRepository = assetRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ApiResponse<PageResponse<PaymentResponse>> list(
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) UUID payerUserId,
            @ParameterObject @PageableDefault(size = 20, sort = "paymentDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        // Without PAYMENT_VIEW a user only ever sees their own payments: the payerUserId
        // parameter is ignored and the filter is forced to the current user.
        UUID effectivePayerUserId = CurrentUser.hasAuthority("PAYMENT_VIEW")
                ? payerUserId
                : CurrentUser.id();
        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (transactionType != null) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (effectivePayerUserId != null) {
                predicates.add(cb.equal(root.get("payerUser").get("id"), effectivePayerUserId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return ApiResponse.ok(PageResponse.from(repository.findAll(spec, pageable), PaymentResponse::from));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('PAYMENT_VIEW')")
    public ApiResponse<Map<String, Object>> summary() {
        Instant now = Instant.now();
        Instant monthAgo = now.minus(30, ChronoUnit.DAYS);
        Instant yearAgo = now.minus(365, ChronoUnit.DAYS);
        return ApiResponse.ok(Map.of(
                "collectedLast30Days", repository.totalCollectedBetween(monthAgo, now),
                "collectedLast365Days", repository.totalCollectedBetween(yearAgo, now),
                "refundCount", repository.countByTransactionType(TransactionType.REFUND)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PaymentResponse> get(@PathVariable UUID id) {
        Payment payment = find(id);
        if (!CurrentUser.hasAuthority("PAYMENT_VIEW")
                && (payment.getPayerUser() == null
                        || !payment.getPayerUser().getId().equals(CurrentUser.id()))) {
            throw ApiException.forbidden("You may only view your own payments");
        }
        return ApiResponse.ok(PaymentResponse.from(payment));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
    @Transactional
    public ApiResponse<PaymentResponse> create(@Valid @RequestBody PaymentRequest request) {
        if (request.transactionType() == TransactionType.REFUND) {
            throw ApiException.badRequest("Use the refund endpoint to process refunds");
        }
        Payment payment = new Payment();
        payment.setTransactionNumber(String.format("PAY-%05d", repository.count() + 1));
        payment.setTransactionType(request.transactionType());
        payment.setPayerType(request.payerType());
        payment.setPayerUser(request.payerUserId() == null ? null
                : userRepository.findById(request.payerUserId())
                .orElseThrow(() -> ApiException.notFound("Payer user")));
        payment.setPayerDepartment(request.payerDepartmentId() == null ? null
                : departmentRepository.findById(request.payerDepartmentId())
                .orElseThrow(() -> ApiException.notFound("Payer department")));
        payment.setPayerName(request.payerName());
        payment.setReservation(request.reservationId() == null ? null
                : reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> ApiException.notFound("Reservation")));
        payment.setAsset(request.assetId() == null ? null
                : assetRepository.findById(request.assetId())
                .orElseThrow(() -> ApiException.notFound("Asset")));
        payment.setDescription(request.description());
        payment.setAmount(request.amount());
        if (request.currency() != null && !request.currency().isBlank()) {
            payment.setCurrency(request.currency().trim().toUpperCase());
        }
        payment.setPaymentMethod(request.paymentMethod().trim());
        payment.setReferenceNumber(request.referenceNumber());
        payment.setPaymentDate(request.paymentDate() != null ? request.paymentDate() : Instant.now());
        if (request.status() != null) {
            payment.setStatus(request.status());
        }
        payment.setNotes(request.notes());
        payment.setCreatedBy(CurrentUser.id());
        repository.save(payment);
        auditService.log("CREATE", "PAYMENT", "Payment", payment.getId(), null,
                Map.of("number", payment.getTransactionNumber(),
                        "type", payment.getTransactionType().name(),
                        "amount", payment.getAmount().toString()));
        return ApiResponse.ok("Payment recorded successfully", PaymentResponse.from(payment));
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAuthority('PAYMENT_CREATE')")
    @Transactional
    public ApiResponse<PaymentResponse> markPaid(@PathVariable UUID id,
                                                 @Valid @RequestBody MarkPaidRequest request) {
        Payment payment = find(id);
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw ApiException.badRequest("Only pending payments can be marked as paid");
        }
        Map<String, Object> before = Map.of(
                "status", payment.getStatus().name(),
                "method", payment.getPaymentMethod(),
                "paymentDate", payment.getPaymentDate().toString());

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentMethod(request.method().trim());
        if (request.referenceNumber() != null && !request.referenceNumber().isBlank()) {
            payment.setReferenceNumber(request.referenceNumber().trim());
        }
        payment.setPaymentDate(Instant.now());
        if (request.notes() != null && !request.notes().isBlank()) {
            payment.setNotes(payment.getNotes() == null || payment.getNotes().isBlank()
                    ? request.notes().trim()
                    : payment.getNotes() + "\n" + request.notes().trim());
        }

        auditService.log("MARK_PAID", "PAYMENT", "Payment", payment.getId(), before,
                Map.of("number", payment.getTransactionNumber(),
                        "status", payment.getStatus().name(),
                        "method", payment.getPaymentMethod(),
                        "amount", payment.getAmount().toString()));
        if (payment.getPayerUser() != null) {
            notificationService.notifyUser(payment.getPayerUser().getId(), "PAYMENT_RECEIVED",
                    "Payment " + payment.getTransactionNumber() + " received",
                    "Your payment of " + payment.getAmount() + " " + payment.getCurrency()
                            + " has been received and marked as paid.",
                    "Payment", payment.getId());
        }
        return ApiResponse.ok("Payment marked as paid successfully", PaymentResponse.from(payment));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('PAYMENT_REFUND')")
    @Transactional
    public ApiResponse<PaymentResponse> refund(@PathVariable UUID id,
                                               @Valid @RequestBody RefundRequest request) {
        Payment original = find(id);
        if (original.getTransactionType() == TransactionType.REFUND) {
            throw ApiException.badRequest("A refund cannot be refunded");
        }
        BigDecimal refundable = original.getAmount().subtract(original.getRefundedAmount());
        if (request.amount().compareTo(refundable) > 0) {
            throw ApiException.badRequest("Refund exceeds the remaining refundable amount of "
                    + refundable + " " + original.getCurrency());
        }
        original.setRefundedAmount(original.getRefundedAmount().add(request.amount()));
        original.setStatus(original.getRefundedAmount().compareTo(original.getAmount()) == 0
                ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);

        Payment refund = new Payment();
        refund.setTransactionNumber(String.format("PAY-%05d", repository.count() + 1));
        refund.setTransactionType(TransactionType.REFUND);
        refund.setPayerType(original.getPayerType());
        refund.setPayerUser(original.getPayerUser());
        refund.setPayerDepartment(original.getPayerDepartment());
        refund.setPayerName(original.getPayerName());
        refund.setReservation(original.getReservation());
        refund.setAsset(original.getAsset());
        refund.setDescription("Refund for " + original.getTransactionNumber() + ": " + request.reason());
        refund.setAmount(request.amount());
        refund.setCurrency(original.getCurrency());
        refund.setPaymentMethod(request.method() != null ? request.method() : original.getPaymentMethod());
        refund.setReferenceNumber(request.referenceNumber());
        refund.setPaymentDate(Instant.now());
        refund.setStatus(PaymentStatus.PAID);
        refund.setOriginalPayment(original);
        refund.setCreatedBy(CurrentUser.id());
        repository.save(refund);

        auditService.log("REFUND", "PAYMENT", "Payment", original.getId(), null,
                Map.of("refundNumber", refund.getTransactionNumber(),
                        "amount", request.amount().toString(), "reason", request.reason()));
        return ApiResponse.ok("Refund processed successfully", PaymentResponse.from(refund));
    }

    private Payment find(UUID id) {
        return repository.findDetailedById(id)
                .orElseThrow(() -> ApiException.notFound("Payment"));
    }
}
