package com.university.assets.maintenance;

import com.university.assets.asset.Asset;
import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.MaintenanceIssueType;
import com.university.assets.common.model.Enums.MaintenancePriority;
import com.university.assets.department.Department;
import com.university.assets.department.DepartmentController;
import com.university.assets.department.DepartmentController.DepartmentSettingsRequest;
import com.university.assets.department.DepartmentRepository;
import com.university.assets.faculty.Faculty;
import com.university.assets.faculty.FacultyRepository;
import com.university.assets.notification.NotificationService;
import com.university.assets.role.Role;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompulsoryMaintenanceTest {

    @Mock private MaintenanceRequestRepository maintenanceRepository;
    @Mock private AssetRepository assetRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private NotificationService notificationService;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private FacultyRepository facultyRepository;

    @InjectMocks
    private CompulsoryMaintenanceService service;

    @InjectMocks
    private DepartmentController departmentController;

    private Faculty science;
    private Department physics;
    private Department chemistry;
    private Asset asset;
    private User custodian;
    private User physicsAdmin;

    @BeforeEach
    void setUp() {
        science = new Faculty();
        science.setId(UUID.randomUUID());
        science.setCode("SCI");
        science.setName("Faculty of Science");

        physics = new Department();
        physics.setId(UUID.randomUUID());
        physics.setFaculty(science);
        physics.setCode("PHYS");
        physics.setName("Physics");
        physics.setMaintenanceIntervalDays(30);

        chemistry = new Department();
        chemistry.setId(UUID.randomUUID());
        chemistry.setFaculty(science);
        chemistry.setCode("CHEM");
        chemistry.setName("Chemistry");

        custodian = new User();
        custodian.setId(UUID.randomUUID());
        custodian.setFirstName("Carl");
        custodian.setLastName("Custodian");
        custodian.setEmail("custodian@university.local");

        Role deptAdminRole = new Role();
        deptAdminRole.setName("DEPT_ADMIN");
        physicsAdmin = new User();
        physicsAdmin.setId(UUID.randomUUID());
        physicsAdmin.setFirstName("Petra");
        physicsAdmin.setLastName("Admin");
        physicsAdmin.setEmail("physics.admin@university.local");
        physicsAdmin.setDepartment(physics);
        physicsAdmin.setFaculty(science);
        physicsAdmin.setRoles(Set.of(deptAdminRole));

        asset = new Asset();
        asset.setId(UUID.randomUUID());
        asset.setAssetCode("AST-00001");
        asset.setName("Oscilloscope");
        asset.setStatus(AssetStatus.AVAILABLE);
        asset.setDepartment(physics);
        asset.setCustodian(custodian);
    }

    @Test
    void dueAsset_createsExactlyOnePreventiveRequest() {
        // Interval exceeded by 15 days -> due, and past the 7-day grace period.
        asset.setLastServiceDate(LocalDate.now().minusDays(45));

        when(assetRepository.findAll(ArgumentMatchers.<Specification<Asset>>any()))
                .thenReturn(List.of(asset));
        when(maintenanceRepository.existsByAssetIdAndIssueTypeAndStatusIn(
                eq(asset.getId()), eq(MaintenanceIssueType.PREVENTIVE), any()))
                .thenReturn(false);
        when(maintenanceRepository.count()).thenReturn(0L);
        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any()))
                .thenReturn(List.of(physicsAdmin));

        int created = service.runCompulsoryMaintenance();

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<MaintenanceRequest> captor = ArgumentCaptor.forClass(MaintenanceRequest.class);
        verify(maintenanceRepository).save(captor.capture());
        MaintenanceRequest request = captor.getValue();
        assertThat(request.getRequestNumber()).isEqualTo("MNT-00001");
        assertThat(request.getIssueType()).isEqualTo(MaintenanceIssueType.PREVENTIVE);
        assertThat(request.getPriority()).isEqualTo(MaintenancePriority.HIGH);
        assertThat(request.getDescription()).contains("every 30 days");
        assertThat(request.getRequestedBy()).isEqualTo(custodian);
        // Grace period (7 days) exceeded -> asset pulled out of service so the
        // existing reservation blocker ("The asset is under maintenance") applies.
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.UNDER_MAINTENANCE);
        // Custodian and department admin are both notified.
        verify(notificationService).notifyUserOncePerDay(eq(custodian.getId()),
                eq("COMPULSORY_MAINTENANCE"), anyString(), anyString(), eq("MaintenanceRequest"), any());
        verify(notificationService).notifyUserOncePerDay(eq(physicsAdmin.getId()),
                eq("COMPULSORY_MAINTENANCE"), anyString(), anyString(), eq("MaintenanceRequest"), any());
    }

    @Test
    void dueAsset_withinGracePeriod_staysAvailable() {
        // Due 3 days ago -> request created but the asset is not pulled from service.
        asset.setLastServiceDate(LocalDate.now().minusDays(33));

        when(assetRepository.findAll(ArgumentMatchers.<Specification<Asset>>any()))
                .thenReturn(List.of(asset));
        when(maintenanceRepository.existsByAssetIdAndIssueTypeAndStatusIn(
                eq(asset.getId()), eq(MaintenanceIssueType.PREVENTIVE), any()))
                .thenReturn(false);
        when(maintenanceRepository.count()).thenReturn(0L);
        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any()))
                .thenReturn(List.of(physicsAdmin));

        int created = service.runCompulsoryMaintenance();

        assertThat(created).isEqualTo(1);
        verify(maintenanceRepository).save(any(MaintenanceRequest.class));
        assertThat(asset.getStatus()).isEqualTo(AssetStatus.AVAILABLE);
    }

    @Test
    void secondRun_isIdempotent_whenOpenPreventiveRequestExists() {
        asset.setLastServiceDate(LocalDate.now().minusDays(45));

        when(assetRepository.findAll(ArgumentMatchers.<Specification<Asset>>any()))
                .thenReturn(List.of(asset));
        when(maintenanceRepository.existsByAssetIdAndIssueTypeAndStatusIn(
                eq(asset.getId()), eq(MaintenanceIssueType.PREVENTIVE), any()))
                .thenReturn(true);

        int created = service.runCompulsoryMaintenance();

        assertThat(created).isZero();
        verify(maintenanceRepository, never()).save(any(MaintenanceRequest.class));
    }

    @Test
    void assetNotDue_createsNothing() {
        asset.setLastServiceDate(LocalDate.now().minusDays(10));

        when(assetRepository.findAll(ArgumentMatchers.<Specification<Asset>>any()))
                .thenReturn(List.of(asset));

        int created = service.runCompulsoryMaintenance();

        assertThat(created).isZero();
        verify(maintenanceRepository, never()).save(any(MaintenanceRequest.class));
    }

    @Test
    void settings_deptAdmin_canSetOwnDepartmentInterval() {
        when(departmentRepository.findById(physics.getId())).thenReturn(Optional.of(physics));
        when(userRepository.findWithRolesById(physicsAdmin.getId())).thenReturn(Optional.of(physicsAdmin));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(() -> CurrentUser.hasAuthority(anyString())).thenReturn(false);
            currentUserMock.when(CurrentUser::id).thenReturn(physicsAdmin.getId());

            var response = departmentController.updateSettings(
                    physics.getId(), new DepartmentSettingsRequest(90));

            assertThat(response.data().maintenanceIntervalDays()).isEqualTo(90);
            assertThat(physics.getMaintenanceIntervalDays()).isEqualTo(90);
        }
    }

    @Test
    void settings_deptAdmin_cannotSetOtherDepartmentInterval() {
        when(departmentRepository.findById(chemistry.getId())).thenReturn(Optional.of(chemistry));
        when(userRepository.findWithRolesById(physicsAdmin.getId())).thenReturn(Optional.of(physicsAdmin));

        try (MockedStatic<CurrentUser> currentUserMock = mockStatic(CurrentUser.class)) {
            currentUserMock.when(() -> CurrentUser.hasAuthority(anyString())).thenReturn(false);
            currentUserMock.when(CurrentUser::id).thenReturn(physicsAdmin.getId());

            assertThatThrownBy(() -> departmentController.updateSettings(
                    chemistry.getId(), new DepartmentSettingsRequest(60)))
                    .isInstanceOf(ApiException.class)
                    .hasMessageContaining("own department or faculty");
        }
        assertThat(chemistry.getMaintenanceIntervalDays()).isNull();
    }
}
