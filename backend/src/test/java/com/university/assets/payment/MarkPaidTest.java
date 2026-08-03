package com.university.assets.payment;

import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.PayerType;
import com.university.assets.common.model.Enums.PaymentStatus;
import com.university.assets.common.model.Enums.TransactionType;
import com.university.assets.department.DepartmentRepository;
import com.university.assets.notification.NotificationService;
import com.university.assets.payment.PaymentController.MarkPaidRequest;
import com.university.assets.reservation.ReservationRepository;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Only pending payments may be settled; settling must stamp the payment fields. */
@ExtendWith(MockitoExtension.class)
class MarkPaidTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private PaymentController controller;

    private User payer;
    private Payment payment;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(paymentRepository, userRepository, departmentRepository,
                reservationRepository, assetRepository, auditService, notificationService);

        payer = new User();
        payer.setId(UUID.randomUUID());

        payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setTransactionNumber("PAY-00001");
        payment.setTransactionType(TransactionType.RESERVATION_FEE);
        payment.setPayerType(PayerType.USER);
        payment.setPayerUser(payer);
        payment.setAmount(new BigDecimal("500.00"));
        payment.setPaymentMethod("PENDING");
        payment.setPaymentDate(Instant.now().minus(2, ChronoUnit.DAYS));
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findDetailedById(payment.getId()))
                .thenReturn(Optional.of(payment));
    }

    @Test
    void markPaid_settlesPendingPaymentAndStampsFields() {
        Instant beforeCall = Instant.now();

        var response = controller.markPaid(payment.getId(),
                new MarkPaidRequest("CASH", "REC-123", "Paid at front desk"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaymentMethod()).isEqualTo("CASH");
        assertThat(payment.getReferenceNumber()).isEqualTo("REC-123");
        assertThat(payment.getPaymentDate()).isAfterOrEqualTo(beforeCall);
        assertThat(payment.getNotes()).contains("Paid at front desk");

        assertThat(response.data().status()).isEqualTo(PaymentStatus.PAID);
        assertThat(response.data().paymentMethod()).isEqualTo("CASH");

        verify(auditService).log(eq("MARK_PAID"), eq("PAYMENT"), eq("Payment"),
                eq(payment.getId()), any(), any());
        verify(notificationService).notifyUser(eq(payer.getId()), eq("PAYMENT_RECEIVED"),
                anyString(), anyString(), eq("Payment"), eq(payment.getId()));
    }

    @Test
    void markPaid_rejectsNonPendingPayment() {
        payment.setStatus(PaymentStatus.PAID);

        assertThatThrownBy(() -> controller.markPaid(payment.getId(),
                new MarkPaidRequest("CASH", null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Only pending payments can be marked as paid");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
    }
}
