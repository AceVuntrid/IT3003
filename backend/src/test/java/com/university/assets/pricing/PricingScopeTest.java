package com.university.assets.pricing;

import com.university.assets.asset.Asset;
import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.LocationType;
import com.university.assets.consumable.ConsumableItem;
import com.university.assets.consumable.ConsumableItemRepository;
import com.university.assets.department.Department;
import com.university.assets.faculty.Faculty;
import com.university.assets.location.Location;
import com.university.assets.location.LocationRepository;
import com.university.assets.pricing.PricingService.PricingItem;
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
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingScopeTest {

    @Mock private AssetRepository assetRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private ConsumableItemRepository consumableItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    private PricingService pricingService;

    private Faculty science;
    private Department physics;
    private Department chemistry;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(assetRepository, locationRepository,
                consumableItemRepository, userRepository, auditService);

        science = new Faculty();
        science.setId(UUID.randomUUID());
        physics = new Department();
        physics.setId(UUID.randomUUID());
        physics.setFaculty(science);
        chemistry = new Department();
        chemistry.setId(UUID.randomUUID());
        chemistry.setFaculty(science);
    }

    // ---------- fixtures ----------

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

    private Asset asset(String code, String name, Faculty faculty, Department department, Location location) {
        Asset asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetCode(code);
        asset.setName(name);
        asset.setFaculty(faculty);
        asset.setDepartment(department);
        asset.setLocation(location);
        return asset;
    }

    private Location venue(String code, String name, LocationType type,
                           Faculty faculty, Department department, Location parent) {
        Location location = new Location();
        location.setId(UUID.randomUUID());
        location.setCode(code);
        location.setName(name);
        location.setType(type);
        location.setFaculty(faculty);
        location.setDepartment(department);
        location.setParent(parent);
        return location;
    }

    private ConsumableItem consumable(String code, String name, Faculty faculty,
                                      Department department, Location location) {
        ConsumableItem item = new ConsumableItem();
        item.setId(UUID.randomUUID());
        item.setItemCode(code);
        item.setName(name);
        item.setUnitOfMeasure("pcs");
        item.setFaculty(faculty);
        item.setDepartment(department);
        item.setLocation(location);
        return item;
    }

    private void stubCurrentUser(User user, MockedStatic<CurrentUser> mock) {
        mock.when(CurrentUser::id).thenReturn(user.getId());
        when(userRepository.findWithRolesById(user.getId())).thenReturn(Optional.of(user));
    }

    // ---------- department admin scope ----------

    @Test
    void physicsAdmin_listsOnlyPhysicsItems() {
        User physicsAdmin = userWithRoles(science, physics, "DEPT_ADMIN");

        Asset physicsAsset = asset("PHY-001", "Oscilloscope", science, physics, null);
        Asset chemAsset = asset("CHE-001", "Spectrometer", science, chemistry, null);
        when(assetRepository.findAll()).thenReturn(List.of(physicsAsset, chemAsset));

        Location physicsLab = venue("LAB-P1", "Physics Lab 1", LocationType.LABORATORY,
                science, physics, null);
        Location chemLab = venue("LAB-C1", "Chemistry Lab 1", LocationType.LABORATORY,
                science, chemistry, null);
        // Same department but not a bookable venue type: must not be listed.
        Location physicsStore = venue("STO-P1", "Physics Store", LocationType.STORAGE_AREA,
                science, physics, null);
        when(locationRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(chemLab, physicsLab, physicsStore));

        ConsumableItem physicsItem = consumable("CON-P1", "Copper wire", science, physics, null);
        ConsumableItem chemItem = consumable("CON-C1", "Ethanol", science, chemistry, null);
        when(consumableItemRepository.findAll()).thenReturn(List.of(physicsItem, chemItem));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            stubCurrentUser(physicsAdmin, currentUserMock);

            List<PricingItem> items = pricingService.listPriceableItems();

            assertThat(items).extracting(PricingItem::id).containsExactlyInAnyOrder(
                    physicsAsset.getId(), physicsLab.getId(), physicsItem.getId());
            assertThat(items).extracting(PricingItem::type)
                    .containsExactlyInAnyOrder("asset", "venue", "consumable");
        }
    }

    @Test
    void physicsAdmin_canPriceOwnDepartmentAsset_andChangeIsAudited() {
        User physicsAdmin = userWithRoles(science, physics, "DEPT_ADMIN");
        Asset physicsAsset = asset("PHY-001", "Oscilloscope", science, physics, null);
        when(assetRepository.findById(physicsAsset.getId())).thenReturn(Optional.of(physicsAsset));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            stubCurrentUser(physicsAdmin, currentUserMock);

            PricingItem updated = pricingService.updatePrice("asset", physicsAsset.getId(),
                    new BigDecimal("250.00"));

            assertThat(updated.currentFee()).isEqualByComparingTo("250.00");
        }

        assertThat(physicsAsset.getReservationFee()).isEqualByComparingTo("250.00");
        verify(auditService).log(eq("UPDATE_PRICE"), eq("SETTINGS"), eq("Asset"),
                eq(physicsAsset.getId()),
                eq(Collections.singletonMap("fee", null)),
                eq(Collections.singletonMap("fee", new BigDecimal("250.00"))));
    }

    @Test
    void physicsAdmin_cannotPriceChemistryItem() {
        User physicsAdmin = userWithRoles(science, physics, "DEPT_ADMIN");
        ConsumableItem chemItem = consumable("CON-C1", "Ethanol", science, chemistry, null);
        when(consumableItemRepository.findById(chemItem.getId())).thenReturn(Optional.of(chemItem));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            stubCurrentUser(physicsAdmin, currentUserMock);

            assertThatThrownBy(() -> pricingService.updatePrice("consumable", chemItem.getId(),
                    new BigDecimal("10.00")))
                    .isInstanceOf(ApiException.class)
                    .satisfies(e -> assertThat(((ApiException) e).getStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }
        assertThat(chemItem.getUnitFee()).isNull();
    }

    // ---------- caretaker scope ----------

    @Test
    void caretaker_listsAndPricesOnlyOwnBuildingVenuesAndItems() {
        User caretaker = userWithRoles(null, null, "CARETAKER");

        Location building = venue("BLD-01", "Main Building", LocationType.BUILDING, null, null, null);
        building.setResponsibleUser(caretaker);
        Location hallInBuilding = venue("AUD-01", "Main Hall", LocationType.AUDITORIUM,
                null, null, building);
        Location otherHall = venue("AUD-02", "Other Hall", LocationType.AUDITORIUM,
                null, null, null);
        when(locationRepository.findAllByOrderByNameAsc())
                .thenReturn(List.of(building, hallInBuilding, otherHall));

        Asset assetInBuilding = asset("GEN-001", "Projector", null, null, hallInBuilding);
        Asset assetElsewhere = asset("GEN-002", "Camera", null, null, otherHall);
        when(assetRepository.findAll()).thenReturn(List.of(assetInBuilding, assetElsewhere));

        ConsumableItem itemInBuilding = consumable("CON-G1", "Whiteboard markers",
                null, null, building);
        ConsumableItem itemElsewhere = consumable("CON-G2", "Batteries", null, null, otherHall);
        when(consumableItemRepository.findAll()).thenReturn(List.of(itemInBuilding, itemElsewhere));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            stubCurrentUser(caretaker, currentUserMock);

            List<PricingItem> items = pricingService.listPriceableItems();

            // The building itself is not a bookable venue; only the hall inside
            // it plus the asset/consumable located in the chain are listed.
            assertThat(items).extracting(PricingItem::id).containsExactlyInAnyOrder(
                    assetInBuilding.getId(), hallInBuilding.getId(), itemInBuilding.getId());
        }

        when(locationRepository.findById(hallInBuilding.getId()))
                .thenReturn(Optional.of(hallInBuilding));
        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            stubCurrentUser(caretaker, currentUserMock);

            PricingItem updated = pricingService.updatePrice("venue", hallInBuilding.getId(),
                    new BigDecimal("5000.00"));

            assertThat(updated.currentFee()).isEqualByComparingTo("5000.00");
        }
        assertThat(hallInBuilding.getBookingFee()).isEqualByComparingTo("5000.00");
        verify(auditService).log(eq("UPDATE_PRICE"), eq("SETTINGS"), eq("Location"),
                eq(hallInBuilding.getId()),
                eq(Collections.singletonMap("fee", null)),
                eq(Collections.singletonMap("fee", new BigDecimal("5000.00"))));
    }

    @Test
    void caretaker_cannotPriceVenueOutsideOwnBuildings() {
        User caretaker = userWithRoles(null, null, "CARETAKER");
        Location otherHall = venue("AUD-02", "Other Hall", LocationType.AUDITORIUM,
                null, null, null);
        when(locationRepository.findById(otherHall.getId())).thenReturn(Optional.of(otherHall));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            stubCurrentUser(caretaker, currentUserMock);

            assertThatThrownBy(() -> pricingService.updatePrice("venue", otherHall.getId(),
                    new BigDecimal("5000.00")))
                    .isInstanceOf(ApiException.class)
                    .satisfies(e -> assertThat(((ApiException) e).getStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }
        assertThat(otherHall.getBookingFee()).isNull();
    }

    // ---------- global admin scope ----------

    @Test
    void superAdmin_pricesAnything() {
        User superAdmin = userWithRoles(null, null, "SUPER_ADMIN");

        ConsumableItem chemItem = consumable("CON-C1", "Ethanol", science, chemistry, null);
        when(consumableItemRepository.findById(chemItem.getId())).thenReturn(Optional.of(chemItem));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            stubCurrentUser(superAdmin, currentUserMock);

            PricingItem updated = pricingService.updatePrice("consumable", chemItem.getId(),
                    new BigDecimal("12.50"));

            assertThat(updated.currentFee()).isEqualByComparingTo("12.50");
            assertThat(updated.unit()).isEqualTo("pcs");
        }
        assertThat(chemItem.getUnitFee()).isEqualByComparingTo("12.50");
    }

    // ---------- fee validation and clearing ----------

    @Test
    void negativeFee_isRejected() {
        // Rejected before any user or entity lookup.
        assertThatThrownBy(() -> pricingService.updatePrice("asset", UUID.randomUUID(),
                new BigDecimal("-1")))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getStatus())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void nullFee_clearsThePrice() {
        User superAdmin = userWithRoles(null, null, "SUPER_ADMIN");
        Asset priced = asset("PHY-001", "Oscilloscope", science, physics, null);
        priced.setReservationFee(new BigDecimal("100.00"));
        when(assetRepository.findById(priced.getId())).thenReturn(Optional.of(priced));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            stubCurrentUser(superAdmin, currentUserMock);

            PricingItem updated = pricingService.updatePrice("asset", priced.getId(), null);

            assertThat(updated.currentFee()).isNull();
        }
        assertThat(priced.getReservationFee()).isNull();
        verify(auditService).log(eq("UPDATE_PRICE"), eq("SETTINGS"), eq("Asset"),
                eq(priced.getId()),
                eq(Collections.singletonMap("fee", new BigDecimal("100.00"))),
                eq(Collections.singletonMap("fee", null)));
    }

    @Test
    void unknownType_isRejected() {
        User superAdmin = userWithRoles(null, null, "SUPER_ADMIN");

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            stubCurrentUser(superAdmin, currentUserMock);

            assertThatThrownBy(() -> pricingService.updatePrice("gadget", UUID.randomUUID(),
                    BigDecimal.ONE))
                    .isInstanceOf(ApiException.class)
                    .satisfies(e -> assertThat(((ApiException) e).getStatus())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }
    }
}
