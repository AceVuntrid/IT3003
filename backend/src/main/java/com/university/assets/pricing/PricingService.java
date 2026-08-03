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
import com.university.assets.role.Role;
import com.university.assets.security.CurrentUser;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Price-list management: which assets, venues and consumable items the current
 * user may price, and the price changes themselves.
 *
 * <p>Scope mirrors the custodianship idiom used for reservation approval
 * ({@code ApprovalScopeService}) and department settings
 * ({@code DepartmentController#requireSettingsScope}):
 * <ul>
 *   <li>SUPER_ADMIN / ASSET_ADMIN — everything;</li>
 *   <li>DEPT_ADMIN (or LAB_MANAGER) — items of their own department;</li>
 *   <li>FACULTY_DEAN (or FACULTY_ADMIN) — items of their own faculty,
 *       including department-less ones;</li>
 *   <li>caretakers — items whose location chain (the location itself or any
 *       ancestor) hits a location they are the responsible user of.</li>
 * </ul>
 * The SETTINGS_MANAGE permission gate lives on the controller; this service
 * enforces the per-item programmatic scope on top of it.
 */
@Service
public class PricingService {

    /** One priceable row: an asset, a bookable venue or a consumable item. */
    public record PricingItem(String type, UUID id, String code, String name,
                              String unit, BigDecimal currentFee) {}

    static final String TYPE_ASSET = "asset";
    static final String TYPE_VENUE = "venue";
    static final String TYPE_CONSUMABLE = "consumable";

    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ROLE_ASSET_ADMIN = "ASSET_ADMIN";
    private static final String ROLE_DEPT_ADMIN = "DEPT_ADMIN";
    private static final String ROLE_LAB_MANAGER = "LAB_MANAGER";
    private static final String ROLE_FACULTY_DEAN = "FACULTY_DEAN";
    private static final String ROLE_FACULTY_ADMIN = "FACULTY_ADMIN";

    /** Bookable location types — the same set LocationController exposes as venues. */
    private static final Set<LocationType> VENUE_TYPES = EnumSet.of(
            LocationType.ROOM, LocationType.LECTURE_ROOM, LocationType.AUDITORIUM,
            LocationType.LABORATORY);

    /** Safety cap when walking the location parent chain. */
    private static final int MAX_LOCATION_DEPTH = 25;

    private final AssetRepository assetRepository;
    private final LocationRepository locationRepository;
    private final ConsumableItemRepository consumableItemRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public PricingService(AssetRepository assetRepository, LocationRepository locationRepository,
                          ConsumableItemRepository consumableItemRepository,
                          UserRepository userRepository, AuditService auditService) {
        this.assetRepository = assetRepository;
        this.locationRepository = locationRepository;
        this.consumableItemRepository = consumableItemRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PricingItem> listPriceableItems() {
        User user = currentUser();
        Set<String> roleNames = roleNames(user);

        List<PricingItem> items = new ArrayList<>();
        assetRepository.findAll().stream()
                .filter(a -> !a.isArchived())
                .filter(a -> canPrice(user, roleNames, a.getFaculty(), a.getDepartment(), a.getLocation()))
                .sorted(Comparator.comparing(Asset::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(a -> items.add(toItem(a)));
        locationRepository.findAllByOrderByNameAsc().stream()
                .filter(l -> l.isActive() && VENUE_TYPES.contains(l.getType()))
                .filter(l -> canPrice(user, roleNames, l.getFaculty(), l.getDepartment(), l))
                .forEach(l -> items.add(toItem(l)));
        consumableItemRepository.findAll().stream()
                .filter(ConsumableItem::isActive)
                .filter(i -> canPrice(user, roleNames, i.getFaculty(), i.getDepartment(), i.getLocation()))
                .sorted(Comparator.comparing(ConsumableItem::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(i -> items.add(toItem(i)));
        return items;
    }

    @Transactional
    public PricingItem updatePrice(String type, UUID id, BigDecimal fee) {
        if (fee != null && fee.signum() < 0) {
            throw ApiException.badRequest("Fee must be zero or positive");
        }
        User user = currentUser();
        Set<String> roleNames = roleNames(user);

        return switch (type == null ? "" : type.toLowerCase(Locale.ROOT)) {
            case TYPE_ASSET -> priceAsset(user, roleNames, id, fee);
            case TYPE_VENUE -> priceVenue(user, roleNames, id, fee);
            case TYPE_CONSUMABLE -> priceConsumable(user, roleNames, id, fee);
            default -> throw ApiException.badRequest(
                    "Unknown pricing type: expected asset, venue or consumable");
        };
    }

    private PricingItem priceAsset(User user, Set<String> roleNames, UUID id, BigDecimal fee) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Asset"));
        if (asset.isArchived()) {
            throw ApiException.badRequest("Archived assets cannot be priced");
        }
        requireScope(user, roleNames, asset.getFaculty(), asset.getDepartment(), asset.getLocation());
        BigDecimal old = asset.getReservationFee();
        asset.setReservationFee(fee);
        audit("Asset", asset.getId(), old, fee);
        return toItem(asset);
    }

    private PricingItem priceVenue(User user, Set<String> roleNames, UUID id, BigDecimal fee) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Venue"));
        if (!VENUE_TYPES.contains(location.getType())) {
            throw ApiException.badRequest("Only bookable venue locations can be priced");
        }
        requireScope(user, roleNames, location.getFaculty(), location.getDepartment(), location);
        BigDecimal old = location.getBookingFee();
        location.setBookingFee(fee);
        audit("Location", location.getId(), old, fee);
        return toItem(location);
    }

    private PricingItem priceConsumable(User user, Set<String> roleNames, UUID id, BigDecimal fee) {
        ConsumableItem item = consumableItemRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Consumable item"));
        requireScope(user, roleNames, item.getFaculty(), item.getDepartment(), item.getLocation());
        BigDecimal old = item.getUnitFee();
        item.setUnitFee(fee);
        audit("ConsumableItem", item.getId(), old, fee);
        return toItem(item);
    }

    // ---------- scope ----------

    private void requireScope(User user, Set<String> roleNames,
                              Faculty faculty, Department department, Location location) {
        if (!canPrice(user, roleNames, faculty, department, location)) {
            throw ApiException.forbidden(
                    "You can only set prices for items of your own department, faculty or locations");
        }
    }

    private boolean canPrice(User user, Set<String> roleNames,
                             Faculty faculty, Department department, Location location) {
        if (roleNames.contains(ROLE_SUPER_ADMIN) || roleNames.contains(ROLE_ASSET_ADMIN)) {
            return true;
        }
        if ((roleNames.contains(ROLE_DEPT_ADMIN) || roleNames.contains(ROLE_LAB_MANAGER))
                && user.getDepartment() != null && department != null
                && user.getDepartment().getId().equals(department.getId())) {
            return true;
        }
        Faculty effectiveFaculty = faculty != null ? faculty
                : (department != null ? department.getFaculty() : null);
        if ((roleNames.contains(ROLE_FACULTY_DEAN) || roleNames.contains(ROLE_FACULTY_ADMIN))
                && user.getFaculty() != null && effectiveFaculty != null
                && user.getFaculty().getId().equals(effectiveFaculty.getId())) {
            return true;
        }
        return isResponsibleForLocationOrAncestor(user, location);
    }

    private boolean isResponsibleForLocationOrAncestor(User user, Location location) {
        Location current = location;
        int depth = 0;
        while (current != null && depth++ < MAX_LOCATION_DEPTH) {
            if (current.getResponsibleUser() != null
                    && current.getResponsibleUser().getId() != null
                    && current.getResponsibleUser().getId().equals(user.getId())) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    // ---------- helpers ----------

    private User currentUser() {
        return userRepository.findWithRolesById(CurrentUser.id())
                .orElseThrow(() -> ApiException.unauthorized("Authentication required"));
    }

    private Set<String> roleNames(User user) {
        return user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    }

    private void audit(String entityType, UUID entityId, BigDecimal oldFee, BigDecimal newFee) {
        auditService.log("UPDATE_PRICE", "SETTINGS", entityType, entityId,
                Collections.singletonMap("fee", oldFee),
                Collections.singletonMap("fee", newFee));
    }

    private PricingItem toItem(Asset asset) {
        return new PricingItem(TYPE_ASSET, asset.getId(), asset.getAssetCode(), asset.getName(),
                null, asset.getReservationFee());
    }

    private PricingItem toItem(Location location) {
        return new PricingItem(TYPE_VENUE, location.getId(), location.getCode(), location.getName(),
                null, location.getBookingFee());
    }

    private PricingItem toItem(ConsumableItem item) {
        return new PricingItem(TYPE_CONSUMABLE, item.getId(), item.getItemCode(), item.getName(),
                item.getUnitOfMeasure(), item.getUnitFee());
    }
}
