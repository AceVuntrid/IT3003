package com.university.assets.location;

import com.university.assets.asset.Asset;
import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.LocationType;
import com.university.assets.consumable.ConsumableItem;
import com.university.assets.consumable.ConsumableItemRepository;
import com.university.assets.department.DepartmentRepository;
import com.university.assets.faculty.FacultyRepository;
import com.university.assets.reservation.Reservation;
import com.university.assets.reservation.ReservationRepository;
import com.university.assets.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationLifecycleTest {

    @Mock private LocationRepository repository;
    @Mock private FacultyRepository facultyRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;
    @Mock private AssetRepository assetRepository;
    @Mock private ConsumableItemRepository consumableItemRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private com.university.assets.transfer.AssetTransferRepository assetTransferRepository;

    @InjectMocks
    private LocationController controller;

    private Location location(UUID id, boolean active) {
        Location location = new Location();
        location.setId(id);
        location.setCode("BLD-A");
        location.setName("Building A");
        location.setType(LocationType.BUILDING);
        location.setActive(active);
        return location;
    }

    @Test
    void deactivate_rejectedWhileActiveChildrenExist() {
        UUID id = UUID.randomUUID();
        Location building = location(id, true);
        when(repository.findById(id)).thenReturn(Optional.of(building));
        when(repository.countByParentIdAndActiveTrue(id)).thenReturn(3L);

        ApiException ex = catchThrowableOfType(() -> controller.deactivate(id), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("Deactivate or move its 3 active child locations first");
        assertThat(building.isActive()).isTrue();
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void deactivate_succeedsWithoutActiveChildren() {
        UUID id = UUID.randomUUID();
        Location room = location(id, true);
        when(repository.findById(id)).thenReturn(Optional.of(room));
        when(repository.countByParentIdAndActiveTrue(id)).thenReturn(0L);

        var response = controller.deactivate(id);

        assertThat(room.isActive()).isFalse();
        assertThat(response.data().active()).isFalse();
        verify(auditService).log(eq("DEACTIVATE"), eq("LOCATION"), eq("Location"), eq(id),
                anyMap(), eq(Map.of("active", false)));
    }

    @Test
    void activate_reactivatesInactiveLocation() {
        UUID id = UUID.randomUUID();
        Location room = location(id, false);
        when(repository.findById(id)).thenReturn(Optional.of(room));

        var response = controller.activate(id);

        assertThat(room.isActive()).isTrue();
        assertThat(response.data().active()).isTrue();
        verify(auditService).log(eq("ACTIVATE"), eq("LOCATION"), eq("Location"), eq(id),
                eq(Map.of("active", false)), eq(Map.of("active", true)));
    }

    @Test
    void delete_rejectedWhenAssetsReferenceLocation() {
        UUID id = UUID.randomUUID();
        Location room = location(id, true);
        when(repository.findById(id)).thenReturn(Optional.of(room));
        when(repository.existsByParentId(id)).thenReturn(false);
        when(assetRepository.exists(org.mockito.ArgumentMatchers.<Specification<Asset>>any()))
                .thenReturn(true);

        ApiException ex = catchThrowableOfType(() -> controller.delete(id), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ex.getMessage()).isEqualTo("This location has history — deactivate it instead");
        verify(repository, never()).delete(any(Location.class));
        verify(auditService, never()).log(any(), any(), any(), any(), any(), any());
    }

    @Test
    void delete_rejectedWhenChildLocationsExist() {
        UUID id = UUID.randomUUID();
        Location building = location(id, false);
        when(repository.findById(id)).thenReturn(Optional.of(building));
        when(repository.existsByParentId(id)).thenReturn(true);

        ApiException ex = catchThrowableOfType(() -> controller.delete(id), ApiException.class);

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).delete(any(Location.class));
    }

    @Test
    void delete_succeedsWhenNothingReferencesLocation() {
        UUID id = UUID.randomUUID();
        Location room = location(id, false);
        when(repository.findById(id)).thenReturn(Optional.of(room));
        when(repository.existsByParentId(id)).thenReturn(false);
        when(assetRepository.exists(org.mockito.ArgumentMatchers.<Specification<Asset>>any()))
                .thenReturn(false);
        when(consumableItemRepository.exists(
                org.mockito.ArgumentMatchers.<Specification<ConsumableItem>>any())).thenReturn(false);
        when(reservationRepository.exists(
                org.mockito.ArgumentMatchers.<Specification<Reservation>>any())).thenReturn(false);
        when(assetTransferRepository.exists(org.mockito.ArgumentMatchers
                .<Specification<com.university.assets.transfer.AssetTransfer>>any())).thenReturn(false);

        var response = controller.delete(id);

        assertThat(response.message()).isEqualTo("Location deleted");
        verify(repository).delete(room);
        verify(auditService).log(eq("DELETE"), eq("LOCATION"), eq("Location"), eq(id),
                eq(Map.of("code", "BLD-A", "name", "Building A")), isNull());
    }

    @Test
    void statusToggle_alsoGuardedByActiveChildren() {
        UUID id = UUID.randomUUID();
        Location building = location(id, true);
        when(repository.findById(id)).thenReturn(Optional.of(building));
        when(repository.countByParentIdAndActiveTrue(id)).thenReturn(1L);

        assertThatThrownBy(() -> controller.toggleStatus(id))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("active child locations first");
        assertThat(building.isActive()).isTrue();
    }
}
