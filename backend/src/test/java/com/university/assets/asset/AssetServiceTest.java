package com.university.assets.asset;

import com.university.assets.asset.dto.AssetDtos.AssetRequest;
import com.university.assets.audit.AuditService;
import com.university.assets.category.AssetCategory;
import com.university.assets.category.AssetCategoryRepository;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.AssetType;
import com.university.assets.department.DepartmentRepository;
import com.university.assets.faculty.Faculty;
import com.university.assets.faculty.FacultyRepository;
import com.university.assets.location.Location;
import com.university.assets.location.LocationRepository;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock private AssetRepository assetRepository;
    @Mock private AssetCategoryRepository categoryRepository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private LocationRepository locationRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    @InjectMocks
    private AssetService assetService;

    private UUID categoryId;
    private UUID facultyId;
    private UUID locationId;
    private UUID currentUserId;
    private AssetCategory category;
    private Faculty faculty;
    private Location location;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        facultyId = UUID.randomUUID();
        locationId = UUID.randomUUID();
        currentUserId = UUID.randomUUID();

        category = new AssetCategory();
        category.setName("Computers");

        faculty = new Faculty();

        location = new Location();
    }

    private AssetRequest defaultRequest() {
        return new AssetRequest(
                "Lab Laptop", "AST-001", AssetType.FIXED, categoryId, "Dell XPS 15",
                "Dell", "XPS 15", "Dell Inc.", "SN123456", "BC123456", null,
                facultyId, null, locationId, "Desk 1", null,
                new BigDecimal("1500.00"), "USD", LocalDate.now().minusMonths(6),
                "PO-100", "INV-100", "State Grant", "GRANT-01",
                "STRAIGHT_LINE", 5, new BigDecimal("200.00"), new BigDecimal("1200.00"),
                AssetCondition.EXCELLENT, AssetCondition.EXCELLENT, AssetStatus.AVAILABLE, 1,
                true, true, false, false, null, 48,
                LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(18), "Dell Care",
                12, LocalDate.now().minusMonths(1), LocalDate.now().plusMonths(11),
                false, null, null, null
        );
    }

    @Test
    void createAsset_successful() {
        AssetRequest req = defaultRequest();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(facultyRepository.findById(facultyId)).thenReturn(Optional.of(faculty));
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(assetRepository.existsByAssetCodeIgnoreCase("AST-001")).thenReturn(false);
        when(assetRepository.existsBySerialNumberIgnoreCase("SN123456")).thenReturn(false);

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(CurrentUser::id).thenReturn(currentUserId);

            assetService.create(req);

            verify(assetRepository).save(any(Asset.class));
        }
    }

    @Test
    void createAsset_throwsWhenWarrantyEndBeforeStart() {
        AssetRequest base = defaultRequest();
        AssetRequest invalid = new AssetRequest(
                base.name(), base.assetCode(), base.assetType(), base.categoryId(), base.description(),
                base.brand(), base.model(), base.manufacturer(), base.serialNumber(), base.barcode(), base.tags(),
                base.facultyId(), base.departmentId(), base.locationId(), base.locationNotes(), base.custodianUserId(),
                base.purchasePrice(), base.currency(), base.purchaseDate(),
                base.purchaseOrderNumber(), base.invoiceNumber(), base.fundingSource(), base.grantCode(),
                base.depreciationMethod(), base.usefulLifeYears(), base.salvageValue(), base.currentBookValue(),
                base.initialCondition(), base.condition(), base.status(), base.quantity(),
                base.reservable(), base.approvalRequired(), base.externalUseAllowed(), base.depositRequired(), base.depositAmount(), base.maxReservationHours(),
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(5), base.warrantyProvider(),
                base.serviceIntervalMonths(), base.lastServiceDate(), base.nextServiceDate(),
                base.calibrationRequired(), base.calibrationIntervalMonths(), base.lastCalibrationDate(), base.nextCalibrationDate()
        );

        assertThatThrownBy(() -> assetService.create(invalid))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Warranty end date cannot be before warranty start date");
    }

    @Test
    void createAsset_throwsWhenDepositRequiredButAmountMissing() {
        AssetRequest base = defaultRequest();
        AssetRequest invalid = new AssetRequest(
                base.name(), base.assetCode(), base.assetType(), base.categoryId(), base.description(),
                base.brand(), base.model(), base.manufacturer(), base.serialNumber(), base.barcode(), base.tags(),
                base.facultyId(), base.departmentId(), base.locationId(), base.locationNotes(), base.custodianUserId(),
                base.purchasePrice(), base.currency(), base.purchaseDate(),
                base.purchaseOrderNumber(), base.invoiceNumber(), base.fundingSource(), base.grantCode(),
                base.depreciationMethod(), base.usefulLifeYears(), base.salvageValue(), base.currentBookValue(),
                base.initialCondition(), base.condition(), base.status(), base.quantity(),
                base.reservable(), base.approvalRequired(), base.externalUseAllowed(), true, null, base.maxReservationHours(),
                base.warrantyStartDate(), base.warrantyEndDate(), base.warrantyProvider(),
                base.serviceIntervalMonths(), base.lastServiceDate(), base.nextServiceDate(),
                base.calibrationRequired(), base.calibrationIntervalMonths(), base.lastCalibrationDate(), base.nextCalibrationDate()
        );

        assertThatThrownBy(() -> assetService.create(invalid))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Deposit amount is required when a deposit is enabled");
    }

    @Test
    void archive_throwsWhenAssetIsCheckedOut() {
        UUID assetId = UUID.randomUUID();
        Asset asset = new Asset();
        asset.setStatus(AssetStatus.CHECKED_OUT);

        when(assetRepository.findDetailedById(assetId)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> assetService.archive(assetId))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("A checked-out asset cannot be archived");
    }
}
