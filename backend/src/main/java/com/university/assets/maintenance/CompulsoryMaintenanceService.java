package com.university.assets.maintenance;

import com.university.assets.asset.Asset;
import com.university.assets.asset.AssetRepository;
import com.university.assets.audit.AuditService;
import com.university.assets.common.model.Enums.AccountStatus;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.MaintenanceIssueType;
import com.university.assets.common.model.Enums.MaintenancePriority;
import com.university.assets.common.model.Enums.MaintenanceStatus;
import com.university.assets.notification.NotificationService;
import com.university.assets.user.User;
import com.university.assets.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Auto-creates PREVENTIVE maintenance requests for assets whose owning
 * department has a compulsory maintenance policy
 * ({@code Department.maintenanceIntervalDays}). Runs from {@code ScheduledJobs}
 * daily; idempotency is guaranteed by skipping assets that already have an
 * open PREVENTIVE request.
 */
@Service
public class CompulsoryMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(CompulsoryMaintenanceService.class);

    /** Statuses that count as "still open" for the dedupe check. */
    static final Set<MaintenanceStatus> OPEN_STATUSES = EnumSet.of(
            MaintenanceStatus.OPEN, MaintenanceStatus.ASSIGNED, MaintenanceStatus.IN_PROGRESS,
            MaintenanceStatus.WAITING_FOR_PARTS, MaintenanceStatus.WAITING_FOR_VENDOR);

    /** Days past the due date before the asset is pulled out of service. */
    static final int GRACE_PERIOD_DAYS = 7;

    private final MaintenanceRequestRepository maintenanceRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public CompulsoryMaintenanceService(MaintenanceRequestRepository maintenanceRepository,
                                        AssetRepository assetRepository,
                                        UserRepository userRepository,
                                        AuditService auditService,
                                        NotificationService notificationService) {
        this.maintenanceRepository = maintenanceRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    /** @return the number of maintenance requests created in this run. */
    @Transactional
    public int runCompulsoryMaintenance() {
        LocalDate today = LocalDate.now();
        List<Asset> candidates = assetRepository.findAll(assetsWithDepartmentPolicy());
        Map<UUID, List<User>> deptAdminCache = new HashMap<>();
        int created = 0;
        for (Asset asset : candidates) {
            Integer intervalDays = asset.getDepartment() != null
                    ? asset.getDepartment().getMaintenanceIntervalDays() : null;
            if (intervalDays == null || intervalDays <= 0) {
                continue;
            }
            LocalDate baseline = asset.getLastServiceDate() != null
                    ? asset.getLastServiceDate()
                    : asset.getCreatedAt() != null
                            ? LocalDate.ofInstant(asset.getCreatedAt(), ZoneId.systemDefault())
                            : null;
            if (baseline == null) {
                continue;
            }
            LocalDate dueDate = baseline.plusDays(intervalDays);
            if (dueDate.isAfter(today)) {
                continue;
            }
            if (maintenanceRepository.existsByAssetIdAndIssueTypeAndStatusIn(
                    asset.getId(), MaintenanceIssueType.PREVENTIVE, OPEN_STATUSES)) {
                continue;
            }
            if (createCompulsoryRequest(asset, intervalDays, dueDate, today, deptAdminCache)) {
                created++;
            }
        }
        if (created > 0) {
            log.info("Compulsory maintenance job created {} preventive request(s)", created);
        }
        return created;
    }

    private boolean createCompulsoryRequest(Asset asset, int intervalDays, LocalDate dueDate,
                                            LocalDate today, Map<UUID, List<User>> deptAdminCache) {
        List<User> deptAdmins = deptAdminCache.computeIfAbsent(asset.getDepartment().getId(),
                deptId -> findActiveUsersByRole("DEPT_ADMIN", deptId));
        User requestedBy = resolveRequestedBy(asset, deptAdmins);
        if (requestedBy == null) {
            log.warn("Skipping compulsory maintenance for asset {}: no custodian, department admin "
                    + "or super admin could be resolved as requester", asset.getAssetCode());
            return false;
        }

        MaintenanceRequest request = new MaintenanceRequest();
        request.setRequestNumber(String.format("MNT-%05d", maintenanceRepository.count() + 1));
        request.setAsset(asset);
        request.setIssueType(MaintenanceIssueType.PREVENTIVE);
        request.setPriority(MaintenancePriority.HIGH);
        request.setDescription("Compulsory maintenance — departmental policy: every "
                + intervalDays + " days");
        request.setRequestedBy(requestedBy);
        maintenanceRepository.save(request);

        // Reservations are blocked via asset status (see ReservationService.assetBlockers).
        // Only escalate once the interval has been exceeded beyond the grace period, and
        // only pull available assets out of service.
        boolean graceExceeded = today.isAfter(dueDate.plusDays(GRACE_PERIOD_DAYS));
        if (graceExceeded && asset.getStatus() == AssetStatus.AVAILABLE) {
            asset.setStatus(AssetStatus.UNDER_MAINTENANCE);
        }

        auditService.log("AUTO_CREATE", "MAINTENANCE", "MaintenanceRequest", request.getId(), null,
                Map.of("number", request.getRequestNumber(),
                        "asset", asset.getAssetCode(),
                        "policyDays", intervalDays,
                        "dueDate", dueDate.toString(),
                        "outOfService", graceExceeded));

        Set<UUID> recipients = new LinkedHashSet<>();
        if (asset.getCustodian() != null) {
            recipients.add(asset.getCustodian().getId());
        }
        deptAdmins.forEach(admin -> recipients.add(admin.getId()));
        for (UUID userId : recipients) {
            notificationService.notifyUserOncePerDay(userId, "COMPULSORY_MAINTENANCE",
                    "Compulsory maintenance created: " + asset.getName(),
                    "Request " + request.getRequestNumber() + " was created automatically under the "
                            + "departmental policy (every " + intervalDays + " days). Service was due on "
                            + dueDate + ".",
                    "MaintenanceRequest", request.getId());
        }
        return true;
    }

    private User resolveRequestedBy(Asset asset, List<User> deptAdmins) {
        if (asset.getCustodian() != null) {
            return asset.getCustodian();
        }
        if (!deptAdmins.isEmpty()) {
            return deptAdmins.get(0);
        }
        List<User> superAdmins = findActiveUsersByRole("SUPER_ADMIN", null);
        return superAdmins.isEmpty() ? null : superAdmins.get(0);
    }

    /** Non-archived assets whose department declares a maintenance interval. */
    private Specification<Asset> assetsWithDepartmentPolicy() {
        return (root, query, cb) -> cb.and(
                cb.isNull(root.get("archivedAt")),
                cb.isNotNull(root.get("department").get("maintenanceIntervalDays")));
    }

    private List<User> findActiveUsersByRole(String roleName, UUID departmentId) {
        Specification<User> spec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.join("roles").get("name"), roleName));
            predicates.add(cb.equal(root.get("accountStatus"), AccountStatus.ACTIVE));
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return userRepository.findAll(spec);
    }
}
