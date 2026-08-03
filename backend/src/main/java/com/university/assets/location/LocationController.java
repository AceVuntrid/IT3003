package com.university.assets.location;

import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.common.exception.ApiException;
import com.university.assets.common.model.Enums.LocationType;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.consumable.ConsumableItemRepository;
import com.university.assets.department.DepartmentRepository;
import com.university.assets.faculty.FacultyRepository;
import com.university.assets.reservation.ReservationRepository;
import com.university.assets.transfer.AssetTransferRepository;
import com.university.assets.user.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@Tag(name = "Locations")
public class LocationController {

    /** Location types that can be booked as venues (lecture rooms, auditoriums, rooms, labs). */
    private static final Set<LocationType> VENUE_TYPES = EnumSet.of(
            LocationType.ROOM, LocationType.LECTURE_ROOM, LocationType.AUDITORIUM,
            LocationType.LABORATORY);

    public record LocationRequest(
            @NotBlank(message = "Code is required") String code,
            @NotBlank(message = "Name is required") String name,
            @NotNull(message = "Type is required") LocationType type,
            UUID parentId,
            UUID facultyId,
            UUID departmentId,
            String address,
            Integer capacity,
            UUID responsibleUserId,
            String description,
            Boolean active
    ) {}

    public record LocationResponse(
            UUID id, String code, String name, LocationType type,
            UUID parentId, String parentName,
            UUID facultyId, String facultyName,
            UUID departmentId, String departmentName,
            String address, Integer capacity,
            UUID responsibleUserId, String responsibleUserName,
            String description, boolean active
    ) {
        static LocationResponse from(Location l) {
            return new LocationResponse(
                    l.getId(), l.getCode(), l.getName(), l.getType(),
                    l.getParent() != null ? l.getParent().getId() : null,
                    l.getParent() != null ? l.getParent().getName() : null,
                    l.getFaculty() != null ? l.getFaculty().getId() : null,
                    l.getFaculty() != null ? l.getFaculty().getName() : null,
                    l.getDepartment() != null ? l.getDepartment().getId() : null,
                    l.getDepartment() != null ? l.getDepartment().getName() : null,
                    l.getAddress(), l.getCapacity(),
                    l.getResponsibleUser() != null ? l.getResponsibleUser().getId() : null,
                    l.getResponsibleUser() != null ? l.getResponsibleUser().getFullName() : null,
                    l.getDescription(), l.isActive());
        }
    }

    public record LocationTreeNode(UUID id, String code, String name, LocationType type,
                                   boolean active, List<LocationTreeNode> children) {}

    private final LocationRepository repository;
    private final FacultyRepository facultyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    /** Read-only: used solely to detect references before a hard delete. */
    private final AssetRepository assetRepository;
    private final ConsumableItemRepository consumableItemRepository;
    private final ReservationRepository reservationRepository;
    private final AssetTransferRepository assetTransferRepository;

    public LocationController(LocationRepository repository, FacultyRepository facultyRepository,
                              DepartmentRepository departmentRepository, UserRepository userRepository,
                              AuditService auditService, AssetRepository assetRepository,
                              ConsumableItemRepository consumableItemRepository,
                              ReservationRepository reservationRepository,
                              AssetTransferRepository assetTransferRepository) {
        this.repository = repository;
        this.facultyRepository = facultyRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.assetRepository = assetRepository;
        this.consumableItemRepository = consumableItemRepository;
        this.reservationRepository = reservationRepository;
        this.assetTransferRepository = assetTransferRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<LocationResponse>> list(
            @RequestParam(required = false) UUID facultyId,
            @RequestParam(required = false, defaultValue = "false") boolean venuesOnly) {
        List<Location> locations = facultyId != null
                ? repository.findByFacultyIdAndActiveTrueOrderByNameAsc(facultyId)
                : repository.findAllByOrderByNameAsc();
        if (venuesOnly) {
            locations = locations.stream()
                    .filter(l -> l.isActive() && VENUE_TYPES.contains(l.getType()))
                    .toList();
        }
        return ApiResponse.ok(locations.stream().map(LocationResponse::from).toList());
    }

    @GetMapping("/tree")
    @Transactional(readOnly = true)
    public ApiResponse<List<LocationTreeNode>> tree() {
        List<Location> all = repository.findAllByOrderByNameAsc();
        Map<UUID, List<Location>> byParent = new LinkedHashMap<>();
        List<Location> roots = new ArrayList<>();
        for (Location l : all) {
            if (l.getParent() == null) {
                roots.add(l);
            } else {
                byParent.computeIfAbsent(l.getParent().getId(), k -> new ArrayList<>()).add(l);
            }
        }
        return ApiResponse.ok(roots.stream().map(r -> toNode(r, byParent)).toList());
    }

    private LocationTreeNode toNode(Location location, Map<UUID, List<Location>> byParent) {
        List<LocationTreeNode> children = byParent.getOrDefault(location.getId(), List.of())
                .stream().map(c -> toNode(c, byParent)).toList();
        return new LocationTreeNode(location.getId(), location.getCode(), location.getName(),
                location.getType(), location.isActive(), children);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ApiResponse<LocationResponse> get(@PathVariable UUID id) {
        Location location = repository.findById(id).orElseThrow(() -> ApiException.notFound("Location"));
        return ApiResponse.ok(LocationResponse.from(location));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LOCATION_MANAGE')")
    @Transactional
    public ApiResponse<LocationResponse> create(@Valid @RequestBody LocationRequest request) {
        if (repository.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("Location code already exists");
        }
        Location location = new Location();
        apply(location, request);
        repository.save(location);
        auditService.log("CREATE", "LOCATION", "Location", location.getId(), null,
                Map.of("code", location.getCode(), "name", location.getName()));
        return ApiResponse.ok("Location created successfully", LocationResponse.from(location));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LOCATION_MANAGE')")
    @Transactional
    public ApiResponse<LocationResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody LocationRequest request) {
        Location location = repository.findById(id).orElseThrow(() -> ApiException.notFound("Location"));
        if (!location.getCode().equalsIgnoreCase(request.code())
                && repository.existsByCodeIgnoreCase(request.code())) {
            throw ApiException.conflict("Location code already exists");
        }
        if (request.parentId() != null && request.parentId().equals(id)) {
            throw ApiException.badRequest("A location cannot be its own parent");
        }
        if (Boolean.FALSE.equals(request.active()) && location.isActive()) {
            requireNoActiveChildren(id);
        }
        Map<String, Object> old = Map.of("code", location.getCode(), "name", location.getName(),
                "active", location.isActive());
        apply(location, request);
        auditService.log("UPDATE", "LOCATION", "Location", location.getId(), old,
                Map.of("code", location.getCode(), "name", location.getName(), "active", location.isActive()));
        return ApiResponse.ok("Location updated successfully", LocationResponse.from(location));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('LOCATION_MANAGE')")
    @Transactional
    public ApiResponse<LocationResponse> toggleStatus(@PathVariable UUID id) {
        Location location = repository.findById(id).orElseThrow(() -> ApiException.notFound("Location"));
        if (location.isActive()) {
            requireNoActiveChildren(id);
        }
        location.setActive(!location.isActive());
        auditService.log("STATUS_CHANGE", "LOCATION", "Location", location.getId(), null,
                Map.of("active", location.isActive()));
        return ApiResponse.ok("Location status updated", LocationResponse.from(location));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('LOCATION_MANAGE')")
    @Transactional
    public ApiResponse<LocationResponse> deactivate(@PathVariable UUID id) {
        Location location = repository.findById(id).orElseThrow(() -> ApiException.notFound("Location"));
        requireNoActiveChildren(id);
        boolean wasActive = location.isActive();
        location.setActive(false);
        auditService.log("DEACTIVATE", "LOCATION", "Location", location.getId(),
                Map.of("active", wasActive), Map.of("active", false));
        return ApiResponse.ok("Location deactivated", LocationResponse.from(location));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('LOCATION_MANAGE')")
    @Transactional
    public ApiResponse<LocationResponse> activate(@PathVariable UUID id) {
        Location location = repository.findById(id).orElseThrow(() -> ApiException.notFound("Location"));
        boolean wasActive = location.isActive();
        location.setActive(true);
        auditService.log("ACTIVATE", "LOCATION", "Location", location.getId(),
                Map.of("active", wasActive), Map.of("active", true));
        return ApiResponse.ok("Location activated", LocationResponse.from(location));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LOCATION_MANAGE')")
    @Transactional
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        Location location = repository.findById(id).orElseThrow(() -> ApiException.notFound("Location"));
        boolean referenced = repository.existsByParentId(id)
                || assetRepository.exists(referencesLocation(id))
                || consumableItemRepository.exists(referencesLocation(id))
                || reservationRepository.exists(referencesLocation(id))
                || assetTransferRepository.exists(referencesTransferLocation(id));
        if (referenced) {
            throw ApiException.conflict("This location has history — deactivate it instead");
        }
        repository.delete(location);
        auditService.log("DELETE", "LOCATION", "Location", id,
                Map.of("code", location.getCode(), "name", location.getName()), null);
        return ApiResponse.message("Location deleted");
    }

    /** Rejects deactivation while the location still has ACTIVE child locations. */
    private void requireNoActiveChildren(UUID id) {
        long activeChildren = repository.countByParentIdAndActiveTrue(id);
        if (activeChildren > 0) {
            throw ApiException.badRequest(
                    "Deactivate or move its " + activeChildren + " active child locations first");
        }
    }

    /** Matches entities whose {@code location} association points at the given location. */
    private static <T> Specification<T> referencesLocation(UUID locationId) {
        return (root, query, cb) -> cb.equal(root.get("location").get("id"), locationId);
    }

    /** Transfers reference locations as either endpoint, so both columns must be checked. */
    private static Specification<com.university.assets.transfer.AssetTransfer> referencesTransferLocation(UUID locationId) {
        return (root, query, cb) -> cb.or(
                cb.equal(root.get("fromLocation").get("id"), locationId),
                cb.equal(root.get("toLocation").get("id"), locationId));
    }

    private void apply(Location location, LocationRequest request) {
        location.setCode(request.code().trim());
        location.setName(request.name().trim());
        location.setType(request.type());
        location.setAddress(request.address());
        location.setCapacity(request.capacity());
        location.setDescription(request.description());
        if (request.active() != null) {
            location.setActive(request.active());
        }
        location.setParent(request.parentId() == null ? null
                : repository.findById(request.parentId())
                .orElseThrow(() -> ApiException.notFound("Parent location")));
        location.setFaculty(request.facultyId() == null ? null
                : facultyRepository.findById(request.facultyId())
                .orElseThrow(() -> ApiException.notFound("Faculty")));
        location.setDepartment(request.departmentId() == null ? null
                : departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> ApiException.notFound("Department")));
        location.setResponsibleUser(request.responsibleUserId() == null ? null
                : userRepository.findById(request.responsibleUserId())
                .orElseThrow(() -> ApiException.notFound("User")));
    }
}
