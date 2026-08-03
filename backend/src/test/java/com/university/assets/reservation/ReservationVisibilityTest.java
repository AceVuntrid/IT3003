package com.university.assets.reservation;

import com.university.assets.asset.Asset;
import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.checkout.CheckoutRepository;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.ApprovalStatus;
import com.university.assets.common.model.Enums.ApprovalStep;
import com.university.assets.common.model.Enums.ApprovalTier;
import com.university.assets.common.model.Enums.ReservationStatus;
import com.university.assets.consumable.ConsumableItem;
import com.university.assets.consumable.ConsumableItemRepository;
import com.university.assets.department.Department;
import com.university.assets.faculty.Faculty;
import com.university.assets.location.Location;
import com.university.assets.location.LocationRepository;
import com.university.assets.notification.NotificationService;
import com.university.assets.payment.PaymentRepository;
import com.university.assets.role.Permissions;
import com.university.assets.role.Role;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Scoped approver visibility (list()/get() prefilter) and pending-approver
 * resolution on responses.
 */
@ExtendWith(MockitoExtension.class)
class ReservationVisibilityTest {

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

    private Faculty science;
    private Department physics;
    private Department chemistry;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, assetRepository,
                locationRepository, consumableItemRepository, userRepository, paymentRepository,
                checkoutRepository, approvalScopeService, auditService, notificationService);

        science = new Faculty();
        science.setId(UUID.randomUUID());
        science.setName("Faculty of Science");
        physics = new Department();
        physics.setId(UUID.randomUUID());
        physics.setName("Physics");
        chemistry = new Department();
        chemistry.setId(UUID.randomUUID());
        chemistry.setName("Chemistry");
    }

    private User user(String firstName, String lastName, Faculty faculty, Department department,
                      String... roleNames) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setFaculty(faculty);
        u.setDepartment(department);
        Set<Role> roles = new HashSet<>();
        for (String name : roleNames) {
            Role role = new Role();
            role.setName(name);
            roles.add(role);
        }
        u.setRoles(roles);
        return u;
    }

    private Asset assetOwnedBy(Faculty faculty, Department department) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setName("Oscilloscope");
        asset.setFaculty(faculty);
        asset.setDepartment(department);
        return asset;
    }

    private Reservation pendingReservationFor(Asset asset) {
        Reservation r = new Reservation();
        r.setId(UUID.randomUUID());
        r.setReservationNumber("RSV-00051");
        r.setAsset(asset);
        r.setRequestedBy(user("Sanduni", "Fernando", null, null, "STUDENT"));
        r.setQuantity(1);
        r.setStartAt(Instant.now().plus(1, ChronoUnit.DAYS));
        r.setEndAt(Instant.now().plus(2, ChronoUnit.DAYS));
        r.setStatus(ReservationStatus.PENDING_APPROVAL);
        r.setApprovalStatus(ApprovalStatus.PENDING);
        r.setRequiredApprovalTier(ApprovalTier.TIER_1_OFFICER);
        r.setCurrentApprovalStep(ApprovalStep.PENDING_LEVEL_1);
        // lenient: the list() test fetches a page instead of this single row.
        lenient().when(reservationRepository.findDetailedById(r.getId())).thenReturn(Optional.of(r));
        return r;
    }

    private Reservation pendingConsumableReservation(Faculty faculty, Department department) {
        ConsumableItem item = new ConsumableItem();
        item.setId(UUID.randomUUID());
        item.setName("Nitrile gloves");
        item.setUnitOfMeasure("box");
        item.setFaculty(faculty);
        item.setDepartment(department);
        item.setCurrentQuantity(new BigDecimal("100"));

        Reservation r = new Reservation();
        r.setId(UUID.randomUUID());
        r.setReservationNumber("RSV-00052");
        r.setConsumableItem(item);
        r.setRequestedBy(user("Sanduni", "Fernando", null, null, "STUDENT"));
        r.setQuantity(2);
        r.setStartAt(Instant.now().plus(1, ChronoUnit.DAYS));
        r.setEndAt(Instant.now().plus(2, ChronoUnit.DAYS));
        r.setStatus(ReservationStatus.PENDING_APPROVAL);
        r.setApprovalStatus(ApprovalStatus.PENDING);
        when(reservationRepository.findDetailedById(r.getId())).thenReturn(Optional.of(r));
        return r;
    }

    private MockedStatic<CurrentUser> approverSession(User approver) {
        MockedStatic<CurrentUser> mock = mockStatic(CurrentUser.class);
        mock.when(CurrentUser::id).thenReturn(approver.getId());
        mock.when(() -> CurrentUser.hasAuthority(Permissions.RESERVATION_APPROVE)).thenReturn(true);
        when(userRepository.findWithRolesById(approver.getId())).thenReturn(Optional.of(approver));
        return mock;
    }

    // ---------- scoped approver visibility ----------

    @Test
    void viewerScope_deptAdmin_isScopedToTheirDepartment() {
        User physicsAdmin = user("Priya", "Perera", science, physics, "DEPT_ADMIN");

        try (MockedStatic<CurrentUser> session = approverSession(physicsAdmin)) {
            var scope = reservationService.viewerScope();

            assertThat(scope.viewAll).isFalse();
            assertThat(scope.isScopedApprover()).isTrue();
            assertThat(scope.departmentId).isEqualTo(physics.getId());
            assertThat(scope.facultyId).isNull();
            assertThat(scope.caretaker).isFalse();
        }
    }

    @Test
    void viewerScope_globalAdminApprover_seesEverythingWithoutUserLookup() {
        try (MockedStatic<CurrentUser> mock = mockStatic(CurrentUser.class)) {
            mock.when(CurrentUser::id).thenReturn(UUID.randomUUID());
            mock.when(() -> CurrentUser.hasAuthority(Permissions.RESERVATION_APPROVE)).thenReturn(true);
            mock.when(() -> CurrentUser.hasAuthority("ROLE_SUPER_ADMIN")).thenReturn(true);

            var scope = reservationService.viewerScope();

            assertThat(scope.viewAll).isTrue();
        }
        verify(userRepository, never()).findWithRolesById(any(UUID.class));
    }

    @Test
    void get_deptAdmin_seesOwnDepartmentPendingReservation() {
        User physicsAdmin = user("Priya", "Perera", science, physics, "DEPT_ADMIN");
        Reservation physicsReservation = pendingReservationFor(assetOwnedBy(science, physics));

        try (MockedStatic<CurrentUser> session = approverSession(physicsAdmin)) {
            var view = reservationService.get(physicsReservation.getId());

            assertThat(view.id()).isEqualTo(physicsReservation.getId());
        }
    }

    @Test
    void get_deptAdmin_cannotSeeOtherDepartmentsPendingReservation() {
        User physicsAdmin = user("Priya", "Perera", science, physics, "DEPT_ADMIN");
        Reservation chemistryReservation = pendingReservationFor(assetOwnedBy(science, chemistry));
        // Faculty-null variants would even hide it from a same-faculty dean.
        chemistryReservation.getAsset().setFaculty(null);

        try (MockedStatic<CurrentUser> session = approverSession(physicsAdmin)) {
            assertThatThrownBy(() -> reservationService.get(chemistryReservation.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("your own reservations");
        }
    }

    @Test
    void get_dean_seesFacultyReservations_butNotOtherFaculties() {
        User dean = user("Sunil", "Bandara", science, null, "FACULTY_DEAN");
        Reservation scienceReservation = pendingReservationFor(assetOwnedBy(science, physics));

        Faculty arts = new Faculty();
        arts.setId(UUID.randomUUID());
        Reservation artsReservation = pendingReservationFor(assetOwnedBy(arts, null));

        try (MockedStatic<CurrentUser> session = approverSession(dean)) {
            assertThat(reservationService.get(scienceReservation.getId()).id())
                    .isEqualTo(scienceReservation.getId());

            assertThatThrownBy(() -> reservationService.get(artsReservation.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("your own reservations");
        }
    }

    @Test
    void get_caretaker_seesReservationsForAssetsInTheirBuildings() {
        User caretaker = user("Kamal", "Silva", null, null, "CARETAKER");

        Location building = new Location();
        building.setId(UUID.randomUUID());
        building.setName("Main Building");
        building.setResponsibleUser(caretaker);
        Location storeRoom = new Location();
        storeRoom.setId(UUID.randomUUID());
        storeRoom.setName("Store Room");
        storeRoom.setParent(building);
        when(locationRepository.findAll()).thenReturn(List.of(building, storeRoom));

        Asset unowned = assetOwnedBy(null, null);
        unowned.setLocation(storeRoom);
        Reservation inBuilding = pendingReservationFor(unowned);

        Location elsewhere = new Location();
        elsewhere.setId(UUID.randomUUID());
        Asset otherAsset = assetOwnedBy(null, null);
        otherAsset.setLocation(elsewhere);
        Reservation outsideBuilding = pendingReservationFor(otherAsset);

        try (MockedStatic<CurrentUser> session = approverSession(caretaker)) {
            assertThat(reservationService.get(inBuilding.getId()).id())
                    .isEqualTo(inBuilding.getId());

            assertThatThrownBy(() -> reservationService.get(outsideBuilding.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("your own reservations");
        }
    }

    @Test
    void get_caretaker_seesReservationsForAssetsInTheirCustody() {
        User caretaker = user("Kamal", "Silva", null, null, "CARETAKER");
        when(locationRepository.findAll()).thenReturn(List.of());

        Asset custodied = assetOwnedBy(null, null);
        custodied.setCustodian(caretaker);
        Reservation reservation = pendingReservationFor(custodied);

        try (MockedStatic<CurrentUser> session = approverSession(caretaker)) {
            assertThat(reservationService.get(reservation.getId()).id())
                    .isEqualTo(reservation.getId());
        }
    }

    @Test
    void get_storekeeper_stillSeesConsumableReservations_butNoAssetOnes() {
        User storekeeper = user("Ruwan", "Jaya", null, null, "STOREKEEPER");
        Reservation consumable = pendingConsumableReservation(science, chemistry);
        Reservation assetReservation = pendingReservationFor(assetOwnedBy(science, physics));

        try (MockedStatic<CurrentUser> mock = mockStatic(CurrentUser.class)) {
            mock.when(CurrentUser::id).thenReturn(storekeeper.getId());
            mock.when(() -> CurrentUser.hasAuthority(Permissions.CONSUMABLE_ISSUE)).thenReturn(true);

            assertThat(reservationService.get(consumable.getId()).id())
                    .isEqualTo(consumable.getId());

            assertThatThrownBy(() -> reservationService.get(assetReservation.getId()))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("your own reservations");
        }
    }

    @Test
    void list_scopedApprover_queriesWithPrefilterInsteadOfViewAll() {
        User physicsAdmin = user("Priya", "Perera", science, physics, "DEPT_ADMIN");
        Reservation physicsReservation = pendingReservationFor(assetOwnedBy(science, physics));
        when(reservationRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(physicsReservation)));

        try (MockedStatic<CurrentUser> session = approverSession(physicsAdmin)) {
            var page = reservationService.list(null, null, null, null, null, null,
                    false, PageRequest.of(0, 20));

            assertThat(page.content()).hasSize(1);
        }
        // The scope (and with it the department prefilter) was computed for the query.
        verify(userRepository).findWithRolesById(physicsAdmin.getId());
    }

    // ---------- pending approver resolution ----------

    @Test
    void pendingApprover_departmentOwnedItem_isFirstActiveDeptAdmin() {
        Reservation reservation = pendingReservationFor(assetOwnedBy(science, physics));
        User deptAdmin = user("Priya", "Perera", science, physics, "DEPT_ADMIN");
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(deptAdmin));

        try (MockedStatic<CurrentUser> mock = mockStatic(CurrentUser.class)) {
            mock.when(CurrentUser::id).thenReturn(reservation.getRequestedBy().getId());

            var view = reservationService.get(reservation.getId());

            assertThat(view.pendingApprover()).isNotNull();
            assertThat(view.pendingApprover().name()).isEqualTo("Priya Perera");
            assertThat(view.pendingApprover().role()).isEqualTo("Physics Department Admin");
        }
    }

    @Test
    void pendingApprover_facultyOwnedItem_isTheDean() {
        Reservation reservation = pendingReservationFor(assetOwnedBy(science, null));
        User dean = user("Sunil", "Bandara", science, null, "FACULTY_DEAN");
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(dean));

        try (MockedStatic<CurrentUser> mock = mockStatic(CurrentUser.class)) {
            mock.when(CurrentUser::id).thenReturn(reservation.getRequestedBy().getId());

            var view = reservationService.get(reservation.getId());

            assertThat(view.pendingApprover()).isNotNull();
            assertThat(view.pendingApprover().name()).isEqualTo("Sunil Bandara");
            assertThat(view.pendingApprover().role()).isEqualTo("Dean, Faculty of Science");
        }
    }

    @Test
    void pendingApprover_unownedItem_isCaretakerFoundWalkingParentLocations() {
        User caretaker = user("Kamal", "Silva", null, null, "CARETAKER");

        Location building = new Location();
        building.setId(UUID.randomUUID());
        building.setName("Main Building");
        building.setResponsibleUser(caretaker);
        Location storeRoom = new Location();
        storeRoom.setId(UUID.randomUUID());
        storeRoom.setName("Store Room");
        storeRoom.setParent(building);

        Asset unowned = assetOwnedBy(null, null);
        unowned.setLocation(storeRoom);
        Reservation reservation = pendingReservationFor(unowned);

        try (MockedStatic<CurrentUser> mock = mockStatic(CurrentUser.class)) {
            mock.when(CurrentUser::id).thenReturn(reservation.getRequestedBy().getId());

            var view = reservationService.get(reservation.getId());

            assertThat(view.pendingApprover()).isNotNull();
            assertThat(view.pendingApprover().name()).isEqualTo("Kamal Silva");
            assertThat(view.pendingApprover().role()).isEqualTo("Caretaker, Main Building");
        }
    }

    @Test
    void pendingApprover_unownedItemWithCustodian_isTheCustodian() {
        User custodian = user("Nimal", "Perera", null, null, "CARETAKER");
        Location lab = new Location();
        lab.setId(UUID.randomUUID());
        lab.setName("Physics Lab");

        Asset asset = assetOwnedBy(null, null);
        asset.setCustodian(custodian);
        asset.setLocation(lab);
        Reservation reservation = pendingReservationFor(asset);

        try (MockedStatic<CurrentUser> mock = mockStatic(CurrentUser.class)) {
            mock.when(CurrentUser::id).thenReturn(reservation.getRequestedBy().getId());

            var view = reservationService.get(reservation.getId());

            assertThat(view.pendingApprover()).isNotNull();
            assertThat(view.pendingApprover().name()).isEqualTo("Nimal Perera");
            assertThat(view.pendingApprover().role()).isEqualTo("Caretaker, Physics Lab");
        }
    }

    @Test
    void pendingApprover_nullWhenNotPendingOrNobodyResolvable() {
        // Approved reservations carry no pending approver even if one would resolve.
        Reservation approved = pendingReservationFor(assetOwnedBy(science, physics));
        approved.setStatus(ReservationStatus.APPROVED);

        // Department-owned but the department has no active DEPT_ADMIN.
        Reservation orphaned = pendingReservationFor(assetOwnedBy(science, chemistry));
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of());

        try (MockedStatic<CurrentUser> mock = mockStatic(CurrentUser.class)) {
            mock.when(CurrentUser::id).thenReturn(approved.getRequestedBy().getId());
            assertThat(reservationService.get(approved.getId()).pendingApprover()).isNull();
        }
        try (MockedStatic<CurrentUser> mock = mockStatic(CurrentUser.class)) {
            mock.when(CurrentUser::id).thenReturn(orphaned.getRequestedBy().getId());
            assertThat(reservationService.get(orphaned.getId()).pendingApprover()).isNull();
        }
    }
}
