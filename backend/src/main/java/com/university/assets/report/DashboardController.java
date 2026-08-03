package com.university.assets.report;

import com.university.assets.asset.AssetRepository;
import com.university.assets.checkout.CheckoutRepository;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.CheckoutStatus;
import com.university.assets.common.model.Enums.MaintenanceStatus;
import com.university.assets.common.model.Enums.ReservationStatus;
import com.university.assets.common.response.ApiResponse;
import com.university.assets.consumable.ConsumableBatchRepository;
import com.university.assets.consumable.ConsumableItemRepository;
import com.university.assets.maintenance.MaintenanceRequestRepository;
import com.university.assets.reservation.ReservationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    public record NameValue(String name, Number value) {}

    private final AssetRepository assetRepository;
    private final ConsumableItemRepository consumableRepository;
    private final ConsumableBatchRepository batchRepository;
    private final ReservationRepository reservationRepository;
    private final CheckoutRepository checkoutRepository;
    private final MaintenanceRequestRepository maintenanceRepository;
    private final EntityManager entityManager;

    public DashboardController(AssetRepository assetRepository,
                               ConsumableItemRepository consumableRepository,
                               ConsumableBatchRepository batchRepository,
                               ReservationRepository reservationRepository,
                               CheckoutRepository checkoutRepository,
                               MaintenanceRequestRepository maintenanceRepository,
                               EntityManager entityManager) {
        this.assetRepository = assetRepository;
        this.consumableRepository = consumableRepository;
        this.batchRepository = batchRepository;
        this.reservationRepository = reservationRepository;
        this.checkoutRepository = checkoutRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.entityManager = entityManager;
    }

    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> summary() {
        Map<String, Object> cards = new LinkedHashMap<>();
        cards.put("totalAssets", assetRepository.countByArchivedAtIsNull());
        cards.put("totalAssetValue", assetRepository.totalPurchaseValue());
        cards.put("availableAssets",
                assetRepository.countByStatusAndArchivedAtIsNull(AssetStatus.AVAILABLE));
        cards.put("reservedAssets",
                assetRepository.countByStatusAndArchivedAtIsNull(AssetStatus.RESERVED));
        cards.put("checkedOutAssets",
                assetRepository.countByStatusAndArchivedAtIsNull(AssetStatus.CHECKED_OUT));
        cards.put("underMaintenance",
                assetRepository.countByStatusAndArchivedAtIsNull(AssetStatus.UNDER_MAINTENANCE));
        cards.put("damagedAssets",
                assetRepository.countByStatusAndArchivedAtIsNull(AssetStatus.DAMAGED));
        cards.put("lostAssets",
                assetRepository.countByStatusAndArchivedAtIsNull(AssetStatus.LOST));
        cards.put("lowStockConsumables", consumableRepository.countLowStock());
        cards.put("expiringConsumables",
                batchRepository.countItemsExpiringBefore(LocalDate.now().plusDays(60)));
        cards.put("pendingApprovals",
                reservationRepository.countByStatus(ReservationStatus.PENDING_APPROVAL));
        cards.put("overdueReturns", checkoutRepository.countByStatus(CheckoutStatus.OVERDUE)
                + checkoutRepository.findByStatusInAndExpectedReturnAtBefore(
                        EnumSet.of(CheckoutStatus.CHECKED_OUT), Instant.now()).size());
        cards.put("maintenanceJobsOpen", maintenanceRepository.countByStatusIn(EnumSet.of(
                MaintenanceStatus.OPEN, MaintenanceStatus.ASSIGNED, MaintenanceStatus.IN_PROGRESS,
                MaintenanceStatus.WAITING_FOR_PARTS, MaintenanceStatus.WAITING_FOR_VENDOR)));
        cards.put("maintenanceDueSoon",
                assetRepository.countMaintenanceDueBy(LocalDate.now().plusDays(30)));
        return ApiResponse.ok(cards);
    }

    @GetMapping("/charts")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> charts() {
        Map<String, Object> charts = new LinkedHashMap<>();
        charts.put("assetsByCategory", groupCount(
                "select a.category.name, count(a) from Asset a where a.archivedAt is null "
                        + "group by a.category.name order by count(a) desc"));
        charts.put("assetsByFaculty", groupCount(
                "select a.faculty.name, count(a) from Asset a where a.archivedAt is null "
                        + "group by a.faculty.name order by count(a) desc"));
        charts.put("assetsByCondition", groupCount(
                "select a.condition, count(a) from Asset a where a.archivedAt is null "
                        + "group by a.condition order by count(a) desc"));
        charts.put("assetsByStatus", groupCount(
                "select a.status, count(a) from Asset a where a.archivedAt is null "
                        + "group by a.status order by count(a) desc"));
        charts.put("acquisitionValueByYear", groupSum(
                "select year(a.purchaseDate), sum(a.purchasePrice) from Asset a "
                        + "where a.purchaseDate is not null and a.archivedAt is null "
                        + "group by year(a.purchaseDate) order by year(a.purchaseDate)"));
        charts.put("monthlyReservations", monthlyReservations());
        return ApiResponse.ok(charts);
    }

    private List<NameValue> groupCount(String jpql) {
        List<Object[]> rows = entityManager.createQuery(jpql, Object[].class).getResultList();
        List<NameValue> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new NameValue(String.valueOf(row[0]), (Number) row[1]));
        }
        return result;
    }

    private List<NameValue> groupSum(String jpql) {
        List<Object[]> rows = entityManager.createQuery(jpql, Object[].class).getResultList();
        List<NameValue> result = new ArrayList<>();
        for (Object[] row : rows) {
            Number value = row[1] instanceof BigDecimal bd ? bd : (Number) row[1];
            result.add(new NameValue(String.valueOf(row[0]), value));
        }
        return result;
    }

    private List<NameValue> monthlyReservations() {
        List<NameValue> result = new ArrayList<>();
        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            Instant from = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Long count = entityManager.createQuery(
                            "select count(r) from Reservation r where r.createdAt >= :from and r.createdAt < :to",
                            Long.class)
                    .setParameter("from", from)
                    .setParameter("to", to)
                    .getSingleResult();
            result.add(new NameValue(month.toString(), count));
        }
        return result;
    }
}
