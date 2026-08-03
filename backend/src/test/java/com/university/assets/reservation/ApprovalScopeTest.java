package com.university.assets.reservation;

import com.university.assets.asset.Asset;
import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.checkout.CheckoutRepository;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.ApprovalStatus;
import com.university.assets.common.model.Enums.ApprovalStep;
import com.university.assets.common.model.Enums.ApprovalTier;
import com.university.assets.common.model.Enums.PayerType;
import com.university.assets.common.model.Enums.PaymentStatus;
import com.university.assets.common.model.Enums.ReservationStatus;
import com.university.assets.common.model.Enums.TransactionType;
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
import com.university.assets.reservation.dto.ReservationDtos.ReservationRequest;
import com.university.assets.role.Role;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApprovalScopeTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private ConsumableItemRepository consumableItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private CheckoutRepository checkoutRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;

    private final ApprovalScopeService scopeService = new ApprovalScopeService();

    private ReservationService reservationService;

    private Faculty science;
    private Department physics;
    private Department chemistry;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, assetRepository,
                locationRepository, consumableItemRepository, userRepository, paymentRepository,
                checkoutRepository, scopeService, auditService, notificationService);

        science = new Faculty();
        science.setId(UUID.randomUUID());
        physics = new Department();
        physics.setId(UUID.randomUUID());
        chemistry = new Department();
        chemistry.setId(UUID.randomUUID());
    }

    private User userWithRoles(Faculty faculty, Department department, String... roleNames) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFaculty(faculty);
        user.setDepartment(department);
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            Role role = new Role();
            role.setName(name);
            roles.add(role);
        }
        user.setRoles(roles);
        return user;
    }

    private Asset assetOwnedBy(Faculty faculty, Department department) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setFaculty(faculty);
        asset.setDepartment(department);
        return asset;
    }

    private Reservation assetReservation(Asset asset) {
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setReservationNumber("RSV-00042");
        reservation.setAsset(asset);
        reservation.setRequestedBy(userWithRoles(null, null, "STUDENT"));
        reservation.setQuantity(2);
        reservation.setRequestedQuantity(2);
        reservation.setStatus(ReservationStatus.PENDING_APPROVAL);
        reservation.setApprovalStatus(ApprovalStatus.PENDING);
        reservation.setRequiredApprovalTier(ApprovalTier.TIER_1_OFFICER);
        reservation.setCurrentApprovalStep(ApprovalStep.PENDING_LEVEL_1);
        return reservation;
    }

    private ConsumableItem consumableOwnedBy(Faculty faculty, Department department, String unitFee) {
        ConsumableItem item = new ConsumableItem();
        item.setId(UUID.randomUUID());
        item.setItemCode("CON-0001");
        item.setName("Nitrile gloves");
        item.setUnitOfMeasure("box");
        item.setFaculty(faculty);
        item.setDepartment(department);
        item.setCurrentQuantity(new BigDecimal("100"));
        if (unitFee != null) {
            item.setUnitFee(new BigDecimal(unitFee));
        }
        return item;
    }

    private Reservation consumableReservation(ConsumableItem item, int quantity) {
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setReservationNumber("RSV-00044");
        reservation.setConsumableItem(item);
        reservation.setRequestedBy(userWithRoles(null, null, "STUDENT"));
        reservation.setQuantity(quantity);
        reservation.setRequestedQuantity(quantity);
        reservation.setStatus(ReservationStatus.PENDING_APPROVAL);
        reservation.setApprovalStatus(ApprovalStatus.PENDING);
        reservation.setRequiredApprovalTier(ApprovalTier.TIER_1_OFFICER);
        reservation.setCurrentApprovalStep(ApprovalStep.PENDING_LEVEL_1);
        return reservation;
    }

    // ---------- custodianship scoping ----------

    @Test
    void deptAdmin_canApproveOwnDepartmentAsset() {
        User physicsAdmin = userWithRoles(science, physics, "DEPT_ADMIN");
        Reservation reservation = assetReservation(assetOwnedBy(science, physics));

        assertThat(scopeService.canApprove(physicsAdmin, reservation)).isTrue();
    }

    @Test
    void deptAdmin_cannotApproveOtherDepartmentAsset() {
        User physicsAdmin = userWithRoles(science, physics, "DEPT_ADMIN");
        Reservation reservation = assetReservation(assetOwnedBy(science, chemistry));

        assertThat(scopeService.canApprove(physicsAdmin, reservation)).isFalse();
    }

    @Test
    void dean_canApproveFacultyOwnedAsset() {
        User dean = userWithRoles(science, null, "FACULTY_DEAN");
        // Department-null asset resolves to the faculty dean, not a department admin.
        Reservation reservation = assetReservation(assetOwnedBy(science, null));

        assertThat(scopeService.canApprove(dean, reservation)).isTrue();

        Faculty otherFaculty = new Faculty();
        otherFaculty.setId(UUID.randomUUID());
        User otherDean = userWithRoles(otherFaculty, null, "FACULTY_DEAN");
        assertThat(scopeService.canApprove(otherDean, reservation)).isFalse();
    }

    @Test
    void caretaker_onAncestorBuilding_canApproveUnownedAsset() {
        User caretaker = userWithRoles(null, null, "CARETAKER");

        Location building = new Location();
        building.setId(UUID.randomUUID());
        building.setResponsibleUser(caretaker);

        Location storeRoom = new Location();
        storeRoom.setId(UUID.randomUUID());
        storeRoom.setParent(building);

        Asset unowned = assetOwnedBy(null, null);
        unowned.setLocation(storeRoom);
        Reservation reservation = assetReservation(unowned);

        assertThat(scopeService.canApprove(caretaker, reservation)).isTrue();
        assertThat(scopeService.canApprove(userWithRoles(null, null, "CARETAKER"), reservation)).isFalse();
    }

    @Test
    void superAdmin_bypassesScoping() {
        User superAdmin = userWithRoles(null, null, "SUPER_ADMIN");
        Reservation reservation = assetReservation(assetOwnedBy(science, chemistry));

        assertThat(scopeService.canApprove(superAdmin, reservation)).isTrue();
    }

    @Test
    void approve_throwsForbidden_whenNotTheUnitApprover() {
        User physicsAdmin = userWithRoles(science, physics, "DEPT_ADMIN");
        Reservation reservation = assetReservation(assetOwnedBy(science, chemistry));

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(userRepository.findWithRolesById(physicsAdmin.getId()))
                .thenReturn(Optional.of(physicsAdmin));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(physicsAdmin.getId());

            assertThatThrownBy(() -> reservationService.approve(reservation.getId(),
                    new ApprovalRequest(null, null)))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("You are not the approver for this item's unit");
        }
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING_APPROVAL);
    }

    // ---------- consumable custodianship scoping ----------

    @Test
    void deptAdmin_canApproveOwnDepartmentConsumable() {
        User physicsAdmin = userWithRoles(science, physics, "DEPT_ADMIN");
        Reservation reservation = consumableReservation(
                consumableOwnedBy(science, physics, null), 2);

        assertThat(scopeService.canApprove(physicsAdmin, reservation)).isTrue();
    }

    @Test
    void deptAdmin_approvingOtherDepartmentConsumable_isForbidden() {
        User physicsAdmin = userWithRoles(science, physics, "DEPT_ADMIN");
        Reservation reservation = consumableReservation(
                consumableOwnedBy(science, chemistry, null), 2);

        assertThat(scopeService.canApprove(physicsAdmin, reservation)).isFalse();

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(userRepository.findWithRolesById(physicsAdmin.getId()))
                .thenReturn(Optional.of(physicsAdmin));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(physicsAdmin.getId());

            assertThatThrownBy(() -> reservationService.approve(reservation.getId(),
                    new ApprovalRequest(null, null)))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("You are not the approver for this item's unit");
        }
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING_APPROVAL);
    }

    @Test
    void dean_canApproveDepartmentlessConsumable() {
        User dean = userWithRoles(science, null, "FACULTY_DEAN");
        Reservation reservation = consumableReservation(
                consumableOwnedBy(science, null, null), 1);

        assertThat(scopeService.canApprove(dean, reservation)).isTrue();

        Faculty otherFaculty = new Faculty();
        otherFaculty.setId(UUID.randomUUID());
        assertThat(scopeService.canApprove(
                userWithRoles(otherFaculty, null, "FACULTY_DEAN"), reservation)).isFalse();
    }

    // ---------- pricing at final approval (price list, not approver input) ----------

    @Test
    void approvalRequest_carriesNoFeeFields() {
        // Approvers can no longer send a fee: the record has no such components,
        // and unknown legacy JSON properties are ignored by Jackson.
        assertThat(Arrays.stream(ApprovalRequest.class.getRecordComponents())
                .map(RecordComponent::getName))
                .containsExactly("notes", "approvedQuantity");
    }

    @Test
    void finalApproval_assetFee_comesFlatFromPriceList() {
        User superAdmin = userWithRoles(null, null, "SUPER_ADMIN");
        Asset asset = assetOwnedBy(science, physics);
        asset.setReservationFee(new BigDecimal("1500.00"));
        Reservation reservation = assetReservation(asset); // quantity 2 — flat fee anyway

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(userRepository.findWithRolesById(superAdmin.getId()))
                .thenReturn(Optional.of(superAdmin));
        when(paymentRepository.count()).thenReturn(7L);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(superAdmin.getId());

            var response = reservationService.approve(reservation.getId(),
                    new ApprovalRequest("Approved", null));

            assertThat(response.status()).isEqualTo(ReservationStatus.APPROVED);
            assertThat(response.feeAmount()).isEqualByComparingTo("1500.00");
            assertThat(response.applicableFee()).isEqualByComparingTo("1500.00");
            assertThat(response.feeWaived()).isFalse();
        }

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment payment = captor.getValue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(payment.getTransactionType()).isEqualTo(TransactionType.RESERVATION_FEE);
        assertThat(payment.getPayerType()).isEqualTo(PayerType.USER);
        assertThat(payment.getPayerUser()).isSameAs(reservation.getRequestedBy());
        assertThat(payment.getReservation()).isSameAs(reservation);
        assertThat(payment.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(payment.getCurrency()).isEqualTo("LKR");
        assertThat(payment.getTransactionNumber()).isEqualTo("PAY-00008");
        assertThat(payment.getDescription()).contains("RSV-00042");
        assertThat(payment.getCreatedBy()).isEqualTo(superAdmin.getId());
        assertThat(reservation.getFeeAmount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void finalApproval_noPriceListFee_isFreeAndMarkedWaived() {
        User superAdmin = userWithRoles(null, null, "SUPER_ADMIN");
        // No reservation_fee on the asset: null price = free.
        Reservation reservation = assetReservation(assetOwnedBy(science, physics));

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(userRepository.findWithRolesById(superAdmin.getId()))
                .thenReturn(Optional.of(superAdmin));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(superAdmin.getId());

            var response = reservationService.approve(reservation.getId(),
                    new ApprovalRequest(null, null));

            assertThat(response.status()).isEqualTo(ReservationStatus.APPROVED);
            assertThat(response.feeAmount()).isNull();
            assertThat(response.feeWaived()).isTrue();
        }

        verify(paymentRepository, never()).save(any(Payment.class));
        assertThat(reservation.isFeeWaived()).isTrue();
    }

    @Test
    void finalApproval_consumableFee_isUnitFeeTimesApprovedQuantity() {
        // The department admin of the item's own department is the scoped approver.
        User physicsAdmin = userWithRoles(science, physics, "DEPT_ADMIN");
        ConsumableItem item = consumableOwnedBy(science, physics, "50.00");
        Reservation reservation = consumableReservation(item, 4);

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(userRepository.findWithRolesById(physicsAdmin.getId()))
                .thenReturn(Optional.of(physicsAdmin));
        when(paymentRepository.count()).thenReturn(0L);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(physicsAdmin.getId());

            // Partial approval: 3 of the 4 requested boxes.
            var response = reservationService.approve(reservation.getId(),
                    new ApprovalRequest(null, 3));

            assertThat(response.status()).isEqualTo(ReservationStatus.APPROVED);
            assertThat(response.quantity()).isEqualTo(3);
            assertThat(response.feeAmount()).isEqualByComparingTo("150.00");
            assertThat(response.feeWaived()).isFalse();
        }

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("150.00");
        // Consumable stock is physically collected: a collection code is issued.
        assertThat(reservation.getCollectionCode()).matches("\\d{4}");
    }

    @Test
    void finalApproval_venueFee_isFlatBookingFee() {
        User superAdmin = userWithRoles(null, null, "SUPER_ADMIN");
        Reservation reservation = venueReservation();
        reservation.getLocation().setBookingFee(new BigDecimal("2500.00"));

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(userRepository.findWithRolesById(superAdmin.getId()))
                .thenReturn(Optional.of(superAdmin));
        when(paymentRepository.count()).thenReturn(0L);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(superAdmin.getId());

            var response = reservationService.approve(reservation.getId(),
                    new ApprovalRequest(null, null));

            assertThat(response.feeAmount()).isEqualByComparingTo("2500.00");
            assertThat(response.feeWaived()).isFalse();
        }

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("2500.00");
        // Venue bookings still never get a collection code.
        assertThat(reservation.getCollectionCode()).isNull();
    }

    @Test
    void applicableFee_previewedBeforeApproval() {
        // Consumable: unit fee x current quantity, before any approval happened.
        ConsumableItem item = consumableOwnedBy(science, physics, "50.00");
        Reservation reservation = consumableReservation(item, 4);
        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id)
                    .thenReturn(reservation.getRequestedBy().getId());

            var view = reservationService.get(reservation.getId());

            assertThat(view.applicableFee()).isEqualByComparingTo("200.00");
            assertThat(view.feeAmount()).isNull(); // nothing persisted yet
            assertThat(view.status()).isEqualTo(ReservationStatus.PENDING_APPROVAL);
        }

        // Asset: flat fee regardless of quantity.
        Asset asset = assetOwnedBy(science, physics);
        asset.setReservationFee(new BigDecimal("1500.00"));
        Reservation assetRes = assetReservation(asset); // quantity 2
        when(reservationRepository.findDetailedById(assetRes.getId()))
                .thenReturn(Optional.of(assetRes));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id)
                    .thenReturn(assetRes.getRequestedBy().getId());

            assertThat(reservationService.get(assetRes.getId()).applicableFee())
                    .isEqualByComparingTo("1500.00");
        }
    }

    // ---------- consumable stock availability at create ----------

    private ReservationRequest consumableRequest(UUID itemId, int quantity) {
        return new ReservationRequest(null, null, itemId, "Lab practical", null,
                Instant.now().plus(1, ChronoUnit.DAYS), Instant.now().plus(2, ChronoUnit.DAYS),
                quantity, null, null, null, null);
    }

    @Test
    void create_consumableReservation_rejectedWhenClaimsExceedStock() {
        ConsumableItem item = consumableOwnedBy(science, physics, null);
        item.setCurrentQuantity(new BigDecimal("10"));
        User requester = userWithRoles(science, physics, "STUDENT");

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(consumableItemRepository.findDetailedById(item.getId())).thenReturn(Optional.of(item));
        // 8 of the 10 on hand are already claimed by other open reservations.
        when(reservationRepository.consumableClaimedQuantity(eq(item.getId()), any(), any()))
                .thenReturn(8);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(requester.getId());

            assertThatThrownBy(() -> reservationService.create(consumableRequest(item.getId(), 5)))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("Only 2 box");
        }
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void create_consumableReservation_withinFreeStock_entersApproval() {
        ConsumableItem item = consumableOwnedBy(science, physics, "50.00");
        item.setCurrentQuantity(new BigDecimal("10"));
        User requester = userWithRoles(science, physics, "STUDENT");

        when(userRepository.findById(requester.getId())).thenReturn(Optional.of(requester));
        when(consumableItemRepository.findDetailedById(item.getId())).thenReturn(Optional.of(item));
        when(reservationRepository.consumableClaimedQuantity(eq(item.getId()), any(), any()))
                .thenReturn(3);
        when(reservationRepository.count()).thenReturn(41L);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(requester.getId());

            var response = reservationService.create(consumableRequest(item.getId(), 5));

            assertThat(response.consumableItemId()).isEqualTo(item.getId());
            assertThat(response.consumableItemName()).isEqualTo("Nitrile gloves");
            assertThat(response.status()).isEqualTo(ReservationStatus.PENDING_APPROVAL);
            assertThat(response.quantity()).isEqualTo(5);
            // Preview from the price list: 5 boxes x 50.00.
            assertThat(response.applicableFee()).isEqualByComparingTo("250.00");
            // No code before final approval.
            assertThat(response.collectionCode()).isNull();
        }
        verify(reservationRepository).save(any(Reservation.class));
    }

    // ---------- collection code at final approval ----------

    private Reservation venueReservation() {
        Location hall = new Location();
        hall.setId(UUID.randomUUID());
        Reservation reservation = new Reservation();
        reservation.setId(UUID.randomUUID());
        reservation.setReservationNumber("RSV-00043");
        reservation.setLocation(hall);
        reservation.setRequestedBy(userWithRoles(null, null, "STUDENT"));
        reservation.setQuantity(1);
        reservation.setStatus(ReservationStatus.PENDING_APPROVAL);
        reservation.setApprovalStatus(ApprovalStatus.PENDING);
        reservation.setRequiredApprovalTier(ApprovalTier.TIER_1_OFFICER);
        reservation.setCurrentApprovalStep(ApprovalStep.PENDING_LEVEL_1);
        return reservation;
    }

    @Test
    void finalApproval_assetReservation_generatesFourDigitCollectionCode() {
        User superAdmin = userWithRoles(null, null, "SUPER_ADMIN");
        Reservation reservation = assetReservation(assetOwnedBy(science, physics));

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(userRepository.findWithRolesById(superAdmin.getId()))
                .thenReturn(Optional.of(superAdmin));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(superAdmin.getId());

            var approverView = reservationService.approve(reservation.getId(),
                    new ApprovalRequest(null, null));

            assertThat(reservation.getCollectionCode()).matches("\\d{4}");
            // The approver is not the requester: the code must stay hidden.
            assertThat(approverView.collectionCode()).isNull();
        }

        // The requester's approval notification carries the code.
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyUser(eq(reservation.getRequestedBy().getId()),
                eq("RESERVATION_APPROVED"), any(), messageCaptor.capture(),
                eq("Reservation"), eq(reservation.getId()));
        assertThat(messageCaptor.getValue())
                .contains("Your collection code is " + reservation.getCollectionCode() + ".");
    }

    @Test
    void finalApproval_consumableReservation_generatesCollectionCode() {
        User superAdmin = userWithRoles(null, null, "SUPER_ADMIN");
        Reservation reservation = consumableReservation(
                consumableOwnedBy(science, physics, null), 2);

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(userRepository.findWithRolesById(superAdmin.getId()))
                .thenReturn(Optional.of(superAdmin));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(superAdmin.getId());

            var approverView = reservationService.approve(reservation.getId(),
                    new ApprovalRequest(null, null));

            assertThat(reservation.getCollectionCode()).matches("\\d{4}");
            assertThat(approverView.collectionCode()).isNull();
        }

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyUser(eq(reservation.getRequestedBy().getId()),
                eq("RESERVATION_APPROVED"), any(), messageCaptor.capture(),
                eq("Reservation"), eq(reservation.getId()));
        assertThat(messageCaptor.getValue())
                .contains("Your collection code is " + reservation.getCollectionCode() + ".");
    }

    @Test
    void finalApproval_venueBooking_setsNoCollectionCode() {
        User superAdmin = userWithRoles(null, null, "SUPER_ADMIN");
        Reservation reservation = venueReservation();

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(userRepository.findWithRolesById(superAdmin.getId()))
                .thenReturn(Optional.of(superAdmin));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(superAdmin.getId());

            var response = reservationService.approve(reservation.getId(),
                    new ApprovalRequest(null, null));

            assertThat(response.status()).isEqualTo(ReservationStatus.APPROVED);
            assertThat(reservation.getCollectionCode()).isNull();
            assertThat(response.collectionCode()).isNull();
        }

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).notifyUser(eq(reservation.getRequestedBy().getId()),
                eq("RESERVATION_APPROVED"), any(), messageCaptor.capture(),
                eq("Reservation"), eq(reservation.getId()));
        assertThat(messageCaptor.getValue()).doesNotContain("collection code");
    }

    @Test
    void collectionCode_exposedToRequesterOnly() {
        User superAdmin = userWithRoles(null, null, "SUPER_ADMIN");
        Reservation reservation = assetReservation(assetOwnedBy(science, physics));

        when(reservationRepository.findDetailedById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(userRepository.findWithRolesById(superAdmin.getId()))
                .thenReturn(Optional.of(superAdmin));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(superAdmin.getId());
            reservationService.approve(reservation.getId(),
                    new ApprovalRequest(null, null));
        }
        assertThat(reservation.getCollectionCode()).isNotNull();

        // The requester fetching their own reservation sees the code.
        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id)
                    .thenReturn(reservation.getRequestedBy().getId());

            var requesterView = reservationService.get(reservation.getId());

            assertThat(requesterView.collectionCode())
                    .isEqualTo(reservation.getCollectionCode());
        }

        // Any other viewer (e.g. an approver with view-all rights) does not.
        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(superAdmin.getId());
            currentUserMock.when(() -> CurrentUser.hasAuthority(any())).thenReturn(true);

            var staffView = reservationService.get(reservation.getId());

            assertThat(staffView.collectionCode()).isNull();
        }
    }
}
