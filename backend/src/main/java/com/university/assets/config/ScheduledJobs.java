package com.university.assets.config;

import com.university.assets.asset.AssetRepository;
import com.university.assets.checkout.Checkout;
import com.university.assets.checkout.CheckoutRepository;
import com.university.assets.common.model.Enums.CheckoutStatus;
import com.university.assets.consumable.ConsumableBatchRepository;
import com.university.assets.consumable.ConsumableItemRepository;
import com.university.assets.maintenance.CompulsoryMaintenanceService;
import com.university.assets.notification.NotificationService;
import com.university.assets.security.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final CheckoutRepository checkoutRepository;
    private final ConsumableItemRepository consumableRepository;
    private final ConsumableBatchRepository batchRepository;
    private final AssetRepository assetRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationService notificationService;
    private final CompulsoryMaintenanceService compulsoryMaintenanceService;

    public ScheduledJobs(CheckoutRepository checkoutRepository,
                         ConsumableItemRepository consumableRepository,
                         ConsumableBatchRepository batchRepository,
                         AssetRepository assetRepository,
                         RefreshTokenRepository refreshTokenRepository,
                         NotificationService notificationService,
                         CompulsoryMaintenanceService compulsoryMaintenanceService) {
        this.checkoutRepository = checkoutRepository;
        this.consumableRepository = consumableRepository;
        this.batchRepository = batchRepository;
        this.assetRepository = assetRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.notificationService = notificationService;
        this.compulsoryMaintenanceService = compulsoryMaintenanceService;
    }

    /** Every 15 minutes: flag overdue checkouts and remind borrowers. */
    @Scheduled(fixedRate = 15 * 60 * 1000, initialDelay = 60 * 1000)
    @Transactional
    public void markOverdueCheckouts() {
        List<Checkout> overdue = checkoutRepository.findByStatusInAndExpectedReturnAtBefore(
                EnumSet.of(CheckoutStatus.CHECKED_OUT), Instant.now());
        for (Checkout checkout : overdue) {
            checkout.setStatus(CheckoutStatus.OVERDUE);
            notificationService.notifyUserOncePerDay(checkout.getUser().getId(), "RETURN_OVERDUE",
                    "Overdue return: " + checkout.getAsset().getName(),
                    "The item was due back on " + checkout.getExpectedReturnAt()
                            + ". Please return it as soon as possible.",
                    "Checkout", checkout.getId());
        }
        if (!overdue.isEmpty()) {
            log.info("Marked {} checkouts as overdue", overdue.size());
        }
    }

    /** Daily at 07:00: low stock, expiring batches, maintenance due (notifications go to custodians). */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void dailyAlerts() {
        consumableRepository.findLowStock().forEach(item -> {
            var responsible = item.getLocation().getResponsibleUser();
            if (responsible != null) {
                notificationService.notifyUserOncePerDay(responsible.getId(), "LOW_STOCK",
                        "Low stock: " + item.getName(),
                        "Current quantity " + item.getCurrentQuantity() + " " + item.getUnitOfMeasure()
                                + " is at or below the reorder level.",
                        "ConsumableItem", item.getId());
            }
        });
        batchRepository.findExpiringBefore(LocalDate.now().plusDays(30)).forEach(batch -> {
            var responsible = batch.getItem().getLocation().getResponsibleUser();
            if (responsible != null) {
                notificationService.notifyUserOncePerDay(responsible.getId(), "CONSUMABLE_EXPIRING",
                        "Expiring batch: " + batch.getItem().getName(),
                        "Batch " + batch.getBatchNumber() + " expires on " + batch.getExpiryDate() + ".",
                        "ConsumableItem", batch.getItem().getId());
            }
        });
        assetRepository.findByArchivedAtIsNullAndNextServiceDateLessThanEqual(
                LocalDate.now().plusDays(14)).forEach(asset -> {
            if (asset.getCustodian() != null) {
                notificationService.notifyUserOncePerDay(asset.getCustodian().getId(), "MAINTENANCE_DUE",
                        "Maintenance due: " + asset.getName(),
                        "Next service is due on " + asset.getNextServiceDate() + ".",
                        "Asset", asset.getId());
            }
        });
    }

    /**
     * Daily at 07:00: auto-create PREVENTIVE maintenance requests for assets whose
     * department declares a compulsory maintenance interval. Idempotent — assets with
     * an open PREVENTIVE request are skipped (see {@link CompulsoryMaintenanceService}).
     */
    @Scheduled(cron = "0 0 7 * * *")
    public void compulsoryMaintenance() {
        int created = compulsoryMaintenanceService.runCompulsoryMaintenance();
        if (created > 0) {
            log.info("Compulsory maintenance: {} request(s) auto-created", created);
        }
    }

    /** Nightly: purge expired refresh tokens. */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpired(Instant.now().minus(1, ChronoUnit.DAYS));
    }
}
