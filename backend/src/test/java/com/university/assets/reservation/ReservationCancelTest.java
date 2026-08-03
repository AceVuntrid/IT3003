package com.university.assets.reservation;

import com.university.assets.asset.Asset;
import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.checkout.Checkout;
import com.university.assets.checkout.CheckoutRepository;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.ReservationStatus;
import com.university.assets.consumable.ConsumableItem;
import com.university.assets.consumable.ConsumableItemRepository;
import com.university.assets.common.model.Enums.PaymentStatus;
import com.university.assets.location.Location;
import com.university.assets.location.LocationRepository;
import com.university.assets.notification.NotificationService;
import com.university.assets.payment.Payment;
import com.university.assets.payment.PaymentRepository;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import org.springframework.data.jpa.domain.Specification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationCancelTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private ConsumableItemRepository consumableItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CheckoutRepository checkoutRepository;
    @Mock private ApprovalScopeService approvalScopeService;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private ReservationService reservationService;

    private User owner;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, assetRepository,
                locationRepository, consumableItemRepository, userRepository, paymentRepository,
                checkoutRepository, approvalScopeService, auditService, notificationService);

        owner = new User();
        owner.setId(UUID.randomUUID());

        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setName("Microscope X1");

        reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setReservationNumber("RSV-00007");
        reservation.setAsset(asset);
        reservation.setRequestedBy(owner);
        reservation.setQuantity(5);
        reservation.setRequestedQuantity(5);
        reservation.setStartAt(Instant.now());
        reservation.setEndAt(Instant.now().plus(2, ChronoUnit.DAYS));

        // lenient: the consumable-reservation tests below look up their own
        // reservation instead of this asset one.
        lenient().when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
    }

    private Checkout slip(int quantity, Instant returnedAt) {
        Checkout slip = new Checkout();
        slip.setQuantity(quantity);
        slip.setReturnedAt(returnedAt);
        return slip;
    }

    private Reservation consumableReservation(ReservationStatus status) {
        ConsumableItem item = new ConsumableItem();
        item.setId(UUID.randomUUID());
        item.setName("Ethanol 95%");
        item.setUnitOfMeasure("L");

        Reservation r = new Reservation();
        r.setId(UUID.randomUUID());
        r.setReservationNumber("RSV-00008");
        r.setConsumableItem(item);
        r.setRequestedBy(owner);
        r.setQuantity(10);
        r.setRequestedQuantity(10);
        r.setStartAt(Instant.now());
        r.setEndAt(Instant.now().plus(2, ChronoUnit.DAYS));
        r.setStatus(status);
        when(reservationRepository.findDetailedById(r.getId())).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void cancel_completesCheckedOutReservationOnceEverythingIssuedIsBack() {
        // Reserve 5, issue 2, return both: the remainder is no longer wanted.
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        when(checkoutRepository.findByReservationId(reservation.getId()))
                .thenReturn(List.of(slip(2, Instant.now())));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(owner.getId());

            var response = reservationService.cancel(reservation.getId());

            // Completed (not stuck CHECKED_OUT forever): it stops counting against
            // the requester's reservation limit and the asset's window capacity.
            assertThat(response.status()).isEqualTo(ReservationStatus.COMPLETED);
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        }
    }

    @Test
    void cancel_rejectsCheckedOutReservationWhileItemsAreStillOut() {
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        when(checkoutRepository.findByReservationId(reservation.getId()))
                .thenReturn(List.of(slip(2, Instant.now()), slip(1, null)));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(owner.getId());

            assertThatThrownBy(() -> reservationService.cancel(reservation.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("still out");
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
        }
    }

    @Test
    void cancel_cancelsApprovedReservation() {
        reservation.setStatus(ReservationStatus.APPROVED);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(owner.getId());

            var response = reservationService.cancel(reservation.getId());

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        }
    }

    @Test
    void cancel_rejectsCompletedReservation() {
        reservation.setStatus(ReservationStatus.COMPLETED);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(owner.getId());

            assertThatThrownBy(() -> reservationService.cancel(reservation.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Completed reservations cannot be cancelled");
        }
    }

    // ---------- consumable reservations (fulfilled by stock issue, not checkout) ----------

    @Test
    void cancel_consumableReservation_nothingIssued_cancels() {
        Reservation consumable = consumableReservation(ReservationStatus.APPROVED);
        when(reservationRepository.issuedAgainstReservation(consumable.getId()))
                .thenReturn(BigDecimal.ZERO);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(owner.getId());

            var response = reservationService.cancel(consumable.getId());

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
            assertThat(consumable.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        }
    }

    @Test
    void cancel_consumableReservation_partiallyIssued_completesAndReleasesRemainder() {
        // 2 of the 10 approved litres were already issued (consumed) — they never
        // come back, so instead of cancelling, the reservation closes as COMPLETED
        // and the remaining 8 stop claiming stock.
        Reservation consumable = consumableReservation(ReservationStatus.APPROVED);
        when(reservationRepository.issuedAgainstReservation(consumable.getId()))
                .thenReturn(new BigDecimal("2"));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(owner.getId());

            var response = reservationService.cancel(consumable.getId());

            assertThat(response.status()).isEqualTo(ReservationStatus.COMPLETED);
            assertThat(consumable.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        }
    }

    // ---------- permission matrix ----------

    @Test
    void cancel_otherUserWithoutPrivileges_isForbidden() {
        reservation.setStatus(ReservationStatus.APPROVED);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(UUID.randomUUID());

            assertThatThrownBy(() -> reservationService.cancel(reservation.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("your own reservations");
        }
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.APPROVED);
    }

    @Test
    void cancel_superAdmin_cancelsSomeoneElsesReservation() {
        reservation.setStatus(ReservationStatus.APPROVED);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(UUID.randomUUID());
            currentUserMock.when(() -> CurrentUser.hasAuthority("ROLE_SUPER_ADMIN")).thenReturn(true);

            var response = reservationService.cancel(reservation.getId());

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        }
    }

    @Test
    void cancel_superAdmin_stillBlockedWhileItemsArePhysicallyOut() {
        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        when(checkoutRepository.findByReservationId(reservation.getId()))
                .thenReturn(List.of(slip(2, null)));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(UUID.randomUUID());
            currentUserMock.when(() -> CurrentUser.hasAuthority("ROLE_SUPER_ADMIN")).thenReturn(true);

            assertThatThrownBy(() -> reservationService.cancel(reservation.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("still out");
        }
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
    }

    // ---------- venue bookings ----------

    private Reservation venueReservation(Instant startAt) {
        Location hall = new Location();
        hall.setId(UUID.randomUUID());
        hall.setName("Main Hall");

        Reservation r = new Reservation();
        r.setId(UUID.randomUUID());
        r.setReservationNumber("RSV-00009");
        r.setLocation(hall);
        r.setRequestedBy(owner);
        r.setQuantity(1);
        r.setStartAt(startAt);
        r.setEndAt(startAt.plus(4, ChronoUnit.HOURS));
        r.setStatus(ReservationStatus.APPROVED);
        when(reservationRepository.findDetailedById(r.getId())).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    void cancel_venueBooking_ownerCanCancelBeforeStart() {
        Reservation venue = venueReservation(Instant.now().plus(1, ChronoUnit.DAYS));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(owner.getId());

            var response = reservationService.cancel(venue.getId());

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        }
    }

    @Test
    void cancel_venueBooking_ownerCannotCancelAfterStart() {
        Reservation venue = venueReservation(Instant.now().minus(1, ChronoUnit.HOURS));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(owner.getId());

            assertThatThrownBy(() -> reservationService.cancel(venue.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("already started");
        }
        assertThat(venue.getStatus()).isEqualTo(ReservationStatus.APPROVED);
    }

    @Test
    void cancel_venueBooking_superAdminCanCancelAfterStart() {
        Reservation venue = venueReservation(Instant.now().minus(1, ChronoUnit.HOURS));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(owner.getId());
            currentUserMock.when(() -> CurrentUser.hasAuthority("ROLE_SUPER_ADMIN")).thenReturn(true);

            var response = reservationService.cancel(venue.getId());

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        }
    }

    // ---------- linked payments on cancellation ----------

    private Payment paymentFor(Reservation r, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setTransactionNumber("PAY-00042");
        payment.setReservation(r);
        payment.setAmount(new BigDecimal("1500.00"));
        payment.setStatus(status);
        return payment;
    }

    @Test
    void cancel_approvedUncollectedReservation_cancelsPendingFeePayment() {
        reservation.setStatus(ReservationStatus.APPROVED);
        Payment pending = paymentFor(reservation, PaymentStatus.PENDING);
        when(paymentRepository.findAll(any(Specification.class))).thenReturn(List.of(pending));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(owner.getId());

            var response = reservationService.cancel(reservation.getId());

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        }
        assertThat(pending.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(paymentRepository).save(pending);
        verify(auditService).log(eq("CANCEL"), eq("PAYMENT"), eq("Payment"),
                eq(pending.getId()), any(), any());
        // No finance alert for a payment that was never made.
        verifyNoInteractions(notificationService);
    }

    @Test
    void cancel_reservationWithPaidFee_notifiesFinanceOfficersForRefund() {
        reservation.setStatus(ReservationStatus.APPROVED);
        Payment paid = paymentFor(reservation, PaymentStatus.PAID);
        when(paymentRepository.findAll(any(Specification.class))).thenReturn(List.of(paid));

        User financeOfficer = new User();
        financeOfficer.setId(UUID.randomUUID());
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(financeOfficer));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(owner.getId());

            var response = reservationService.cancel(reservation.getId());

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        }
        // Paid money is never silently voided: the payment keeps its status and
        // finance is told a refund may be due.
        assertThat(paid.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(notificationService).notifyUser(eq(financeOfficer.getId()),
                eq("RESERVATION_CANCELLED_PAID"),
                contains("paid fee may need refund"),
                contains("PAY-00042"),
                eq("Reservation"), eq(reservation.getId()));
    }

    @Test
    void cancel_pendingConsumableReservation_cancelsWithoutIssueCheck() {
        // Nothing can have been issued before approval: no stock lookup happens.
        Reservation consumable = consumableReservation(ReservationStatus.PENDING_APPROVAL);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(owner.getId());

            var response = reservationService.cancel(consumable.getId());

            assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        }
        verify(reservationRepository, never()).issuedAgainstReservation(any(UUID.class));
    }
}
