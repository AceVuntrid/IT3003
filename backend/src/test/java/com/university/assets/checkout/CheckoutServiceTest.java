package com.university.assets.checkout;

import com.university.assets.asset.Asset;
import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.category.AssetCategory;
import com.university.assets.checkout.dto.CheckoutDtos.CheckoutRequest;
import com.university.assets.checkout.dto.CheckoutDtos.ReturnRequest;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.CheckoutStatus;
import com.university.assets.common.model.Enums.ReservationStatus;
import com.university.assets.config.AppProperties;
import com.university.assets.notification.NotificationService;
import com.university.assets.reservation.Reservation;
import com.university.assets.reservation.ReservationRepository;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private CheckoutRepository checkoutRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private AppProperties properties;

    @InjectMocks
    private CheckoutService checkoutService;

    private UUID assetId;
    private UUID userId;
    private UUID currentUserId;
    private Asset asset;
    private User user;

    @BeforeEach
    void setUp() {
        assetId = UUID.randomUUID();
        userId = UUID.randomUUID();
        currentUserId = UUID.randomUUID();

        asset = new Asset();
        asset.setAssetCode("AST-00001");
        asset.setQuantity(5);
        asset.setAvailableQuantity(5);
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setCondition(AssetCondition.GOOD);
        asset.setName("Microscope X1");

        user = new User();
        user.setEmail("student@university.edu");
    }

    @Test
    void checkOut_direct_successful() {
        CheckoutRequest request = new CheckoutRequest(
                null, assetId, userId, 1, AssetCondition.GOOD, "Lens cover",
                Instant.now().plus(2, ChronoUnit.HOURS), null, "Lab experiment", null
        );

        when(assetRepository.findDetailedById(assetId)).thenReturn(Optional.of(asset));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.getReferenceById(currentUserId)).thenReturn(user);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(currentUserId);

            var response = checkoutService.checkOut(request);

            assertThat(response).isNotNull();
            assertThat(asset.getAvailableQuantity()).isEqualTo(4);
            verify(checkoutRepository).save(any(Checkout.class));
        }
    }

    @Test
    void checkOut_throwsWhenInsufficientQuantity() {
        asset.setAvailableQuantity(0);
        CheckoutRequest request = new CheckoutRequest(
                null, assetId, userId, 1, AssetCondition.GOOD, null,
                Instant.now().plus(2, ChronoUnit.HOURS), null, null, null
        );

        when(assetRepository.findDetailedById(assetId)).thenReturn(Optional.of(asset));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> checkoutService.checkOut(request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Insufficient available quantity");
    }

    private Reservation reservationFor(Asset reservedAsset, int quantity) {
        AssetCategory category = reservedAsset.getCategory();
        if (category == null) {
            category = new AssetCategory();
            category.setId(UUID.randomUUID());
            category.setName("Microscopes");
            reservedAsset.setCategory(category);
        }
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setReservationNumber("RSV-00001");
        reservation.setAsset(reservedAsset);
        reservation.setRequestedBy(user);
        reservation.setQuantity(quantity);
        reservation.setStatus(ReservationStatus.APPROVED);
        reservation.setStartAt(Instant.now());
        reservation.setEndAt(Instant.now().plus(2, ChronoUnit.DAYS));
        reservation.setCollectionCode("4321");
        return reservation;
    }

    private Checkout issuedSlip(int quantity, Instant returnedAt) {
        Checkout slip = new Checkout();
        slip.setId(UUID.randomUUID());
        slip.setQuantity(quantity);
        slip.setReturnedAt(returnedAt);
        slip.setStatus(returnedAt != null ? CheckoutStatus.RETURNED : CheckoutStatus.CHECKED_OUT);
        return slip;
    }

    @Test
    void checkOut_reservation_partialIssueThenSecondIssueFromDifferentSource() {
        asset.setId(assetId);
        Reservation reservation = reservationFor(asset, 5);

        Asset alternative = new Asset();
        alternative.setId(UUID.randomUUID());
        alternative.setAssetCode("AST-00002");
        alternative.setName("Microscope X1 (ILC store)");
        alternative.setCategory(asset.getCategory());
        alternative.setQuantity(3);
        alternative.setAvailableQuantity(3);
        alternative.setStatus(AssetStatus.AVAILABLE);
        alternative.setCondition(AssetCondition.GOOD);

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(userRepository.getReferenceById(currentUserId)).thenReturn(user);
        when(checkoutRepository.findByReservationId(reservation.getId())).thenReturn(List.of());

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(currentUserId);

            var first = checkoutService.checkOut(new CheckoutRequest(
                    reservation.getId(), null, null, 2, null, null, null, null, null, "4321"));

            assertThat(first.quantity()).isEqualTo(2);
            assertThat(asset.getAvailableQuantity()).isEqualTo(3);
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);

            // Second issue: 3 remaining, taken from a same-category asset elsewhere.
            when(checkoutRepository.findByReservationId(reservation.getId()))
                    .thenReturn(List.of(issuedSlip(2, null)));
            when(assetRepository.findDetailedById(alternative.getId()))
                    .thenReturn(Optional.of(alternative));

            var second = checkoutService.checkOut(new CheckoutRequest(
                    reservation.getId(), alternative.getId(), null, null, null, null, null, null, null, "4321"));

            assertThat(second.quantity()).isEqualTo(3);
            assertThat(second.assetCode()).isEqualTo("AST-00002");
            assertThat(alternative.getAvailableQuantity()).isZero();
            assertThat(asset.getAvailableQuantity()).isEqualTo(3);
        }
    }

    @Test
    void checkOut_reservation_rejectsOverIssue() {
        asset.setId(assetId);
        Reservation reservation = reservationFor(asset, 2);

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(checkoutRepository.findByReservationId(reservation.getId()))
                .thenReturn(List.of(issuedSlip(1, null)));

        assertThatThrownBy(() -> checkoutService.checkOut(new CheckoutRequest(
                reservation.getId(), null, null, 2, null, null, null, null, null, "4321")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("only 1 of 2 remain");
    }

    @Test
    void checkOut_reservation_rejectsWhenFullyIssued() {
        asset.setId(assetId);
        Reservation reservation = reservationFor(asset, 2);
        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(checkoutRepository.findByReservationId(reservation.getId()))
                .thenReturn(List.of(issuedSlip(2, null)));

        assertThatThrownBy(() -> checkoutService.checkOut(new CheckoutRequest(
                reservation.getId(), null, null, null, null, null, null, null, null, "4321")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("fully issued");
    }

    @Test
    void checkOut_reservation_rejectsDifferentCategorySource() {
        asset.setId(assetId);
        Reservation reservation = reservationFor(asset, 2);

        AssetCategory otherCategory = new AssetCategory();
        otherCategory.setId(UUID.randomUUID());
        otherCategory.setName("Projectors");
        Asset other = new Asset();
        other.setId(UUID.randomUUID());
        other.setCategory(otherCategory);
        other.setQuantity(1);
        other.setAvailableQuantity(1);
        other.setStatus(AssetStatus.AVAILABLE);

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(checkoutRepository.findByReservationId(reservation.getId())).thenReturn(List.of());
        when(assetRepository.findDetailedById(other.getId())).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> checkoutService.checkOut(new CheckoutRequest(
                reservation.getId(), other.getId(), null, null, null, null, null, null, null, "4321")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("same category");
    }

    @Test
    void checkOut_reservation_rejectsWrongCollectionCode() {
        asset.setId(assetId);
        Reservation reservation = reservationFor(asset, 2);

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> checkoutService.checkOut(new CheckoutRequest(
                reservation.getId(), null, null, 1, null, null, null, null, null, "0000")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid collection code");
        verify(checkoutRepository, never()).save(any(Checkout.class));
    }

    @Test
    void checkOut_reservation_rejectsMissingCollectionCode() {
        asset.setId(assetId);
        Reservation reservation = reservationFor(asset, 2);

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> checkoutService.checkOut(new CheckoutRequest(
                reservation.getId(), null, null, 1, null, null, null, null, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid collection code");
    }

    @Test
    void checkOut_reservation_legacyWithoutStoredCode_passesWithBlankCode() {
        // Reservations approved before collection codes existed have no stored
        // code and must remain collectable without one.
        asset.setId(assetId);
        Reservation reservation = reservationFor(asset, 1);
        reservation.setCollectionCode(null);

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(checkoutRepository.findByReservationId(reservation.getId())).thenReturn(List.of());
        when(userRepository.getReferenceById(currentUserId)).thenReturn(user);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(currentUserId);

            var response = checkoutService.checkOut(new CheckoutRequest(
                    reservation.getId(), null, null, 1, null, null, null, null, null, null));

            assertThat(response.quantity()).isEqualTo(1);
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
        }
    }

    @Test
    void processReturn_completesReservationOnlyWhenAllReturnedAndFullyIssued() {
        asset.setId(assetId);
        Reservation reservation = reservationFor(asset, 5);
        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        Checkout current = new Checkout();
        current.setId(UUID.randomUUID());
        current.setCheckoutNumber("CHK-00002");
        current.setReservation(reservation);
        current.setAsset(asset);
        current.setUser(user);
        current.setIssuedBy(user);
        current.setQuantity(3);
        current.setConditionBefore(AssetCondition.GOOD);
        current.setExpectedReturnAt(Instant.now().plus(1, ChronoUnit.DAYS));
        current.setStatus(CheckoutStatus.CHECKED_OUT);

        ReturnRequest returnReq = new ReturnRequest(
                AssetCondition.GOOD, null, false, null, false, null, null);

        when(checkoutRepository.findDetailedById(current.getId())).thenReturn(Optional.of(current));
        when(userRepository.getReferenceById(currentUserId)).thenReturn(user);
        when(checkoutRepository.findByReservationId(reservation.getId()))
                .thenReturn(List.of(issuedSlip(2, Instant.now()), current));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(currentUserId);

            checkoutService.processReturn(current.getId(), returnReq);

            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        }
    }

    @Test
    void processReturn_keepsReservationOpenWhileQuantityRemainsToIssue() {
        asset.setId(assetId);
        Reservation reservation = reservationFor(asset, 5);
        reservation.setStatus(ReservationStatus.CHECKED_OUT);

        Checkout current = new Checkout();
        current.setId(UUID.randomUUID());
        current.setCheckoutNumber("CHK-00003");
        current.setReservation(reservation);
        current.setAsset(asset);
        current.setUser(user);
        current.setIssuedBy(user);
        current.setQuantity(2);
        current.setConditionBefore(AssetCondition.GOOD);
        current.setExpectedReturnAt(Instant.now().plus(1, ChronoUnit.DAYS));
        current.setStatus(CheckoutStatus.CHECKED_OUT);

        ReturnRequest returnReq = new ReturnRequest(
                AssetCondition.GOOD, null, false, null, false, null, null);

        when(checkoutRepository.findDetailedById(current.getId())).thenReturn(Optional.of(current));
        when(userRepository.getReferenceById(currentUserId)).thenReturn(user);
        when(checkoutRepository.findByReservationId(reservation.getId()))
                .thenReturn(List.of(current));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(currentUserId);

            checkoutService.processReturn(current.getId(), returnReq);

            // Only 2 of 5 were ever issued: the reservation must stay open for the rest.
            assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CHECKED_OUT);
        }
    }

    @Test
    void processReturn_calculatesOverduePenalty() {
        UUID checkoutId = UUID.randomUUID();
        Checkout checkout = new Checkout();
        checkout.setCheckoutNumber("CHK-00001");
        checkout.setAsset(asset);
        checkout.setUser(user);
        checkout.setIssuedBy(user);
        checkout.setQuantity(1);
        checkout.setConditionBefore(AssetCondition.GOOD);
        checkout.setExpectedReturnAt(Instant.now().minus(2, ChronoUnit.DAYS));
        checkout.setStatus(CheckoutStatus.OVERDUE);

        ReturnRequest returnReq = new ReturnRequest(
                AssetCondition.GOOD, null, false, null, false, null, "Returned late"
        );

        AppProperties.Reservation reservationProps = new AppProperties.Reservation(
                new BigDecimal("10.00")
        );

        when(checkoutRepository.findDetailedById(checkoutId)).thenReturn(Optional.of(checkout));
        when(userRepository.getReferenceById(currentUserId)).thenReturn(user);
        when(properties.reservation()).thenReturn(reservationProps);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(currentUserId);

            var response = checkoutService.processReturn(checkoutId, returnReq);

            assertThat(response.status()).isEqualTo(CheckoutStatus.RETURNED);
            assertThat(response.penaltyAmount()).isGreaterThanOrEqualTo(new BigDecimal("20.00"));
        }
    }
}
