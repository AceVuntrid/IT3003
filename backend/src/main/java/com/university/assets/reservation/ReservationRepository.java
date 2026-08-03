package com.university.assets.reservation;

import com.university.assets.common.model.Enums.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository
        extends JpaRepository<Reservation, UUID>, JpaSpecificationExecutor<Reservation> {

    @EntityGraph(attributePaths = {"asset", "location", "consumableItem", "requestedBy", "faculty", "department", "approvedBy"})
    Optional<Reservation> findDetailedById(UUID id);

    /**
     * Total quantity already committed for the asset in an overlapping window,
     * net of items fulfilled from a different same-category asset (multi-source
     * issue) that are still out — those hold stock on the source asset's
     * availableQuantity instead, so counting them here too would block the same
     * items on two assets at once.
     */
    default int reservedQuantityInWindow(UUID assetId, Instant startAt, Instant endAt,
                                         Collection<ReservationStatus> activeStatuses,
                                         UUID excludeId) {
        return grossReservedQuantityInWindow(assetId, startAt, endAt, activeStatuses, excludeId)
                - crossSourcedQuantityInWindow(assetId, startAt, endAt, activeStatuses, excludeId);
    }

    /** Total quantity of overlapping active reservations for the asset. */
    @Query("select coalesce(sum(r.quantity), 0) from Reservation r "
            + "where r.asset.id = :assetId and r.id <> :excludeId "
            + "and r.status in :activeStatuses "
            + "and r.startAt < :endAt and r.endAt > :startAt")
    int grossReservedQuantityInWindow(@Param("assetId") UUID assetId,
                                      @Param("startAt") Instant startAt,
                                      @Param("endAt") Instant endAt,
                                      @Param("activeStatuses") Collection<ReservationStatus> activeStatuses,
                                      @Param("excludeId") UUID excludeId);

    /**
     * Quantity of those same reservations that was issued from a different asset
     * and has not been returned yet.
     */
    @Query("select coalesce(sum(c.quantity), 0) from Checkout c "
            + "where c.reservation.asset.id = :assetId and c.reservation.id <> :excludeId "
            + "and c.reservation.status in :activeStatuses "
            + "and c.reservation.startAt < :endAt and c.reservation.endAt > :startAt "
            + "and c.asset.id <> :assetId and c.returnedAt is null")
    int crossSourcedQuantityInWindow(@Param("assetId") UUID assetId,
                                     @Param("startAt") Instant startAt,
                                     @Param("endAt") Instant endAt,
                                     @Param("activeStatuses") Collection<ReservationStatus> activeStatuses,
                                     @Param("excludeId") UUID excludeId);

    @Query("select count(r) from Reservation r where r.location.id = :locationId and r.id <> :excludeId "
            + "and r.status in :activeStatuses and r.startAt < :endAt and r.endAt > :startAt")
    long locationConflicts(@Param("locationId") UUID locationId,
                           @Param("startAt") Instant startAt,
                           @Param("endAt") Instant endAt,
                           @Param("activeStatuses") Collection<ReservationStatus> activeStatuses,
                           @Param("excludeId") UUID excludeId);

    @EntityGraph(attributePaths = {"asset", "location", "consumableItem", "requestedBy"})
    List<Reservation> findByStartAtLessThanAndEndAtGreaterThanAndStatusIn(
            Instant endBefore, Instant startAfter, Collection<ReservationStatus> statuses);

    /**
     * Units of a consumable item still claimed by open reservations
     * (no time-window semantics: consumables are consumed, so any open claim
     * holds stock regardless of its collection window). Partially issued
     * reservations only claim their un-issued remainder: the issued portion has
     * already been deducted from {@code currentQuantity}, so counting it here
     * too would subtract it twice and under-report availability. The issued sum
     * is truncated to whole units, staying conservative — never overselling.
     */
    default int consumableClaimedQuantity(UUID itemId, Collection<ReservationStatus> statuses,
                                          UUID excludeId) {
        int gross = grossConsumableClaimedQuantity(itemId, statuses, excludeId);
        BigDecimal issued = issuedAgainstOpenClaims(itemId,
                statuses.stream().map(Enum::name).toList(), excludeId);
        return gross - (issued != null ? issued.intValue() : 0);
    }

    /** Full quantity of open claiming reservations for the item. */
    @Query("select coalesce(sum(r.quantity), 0) from Reservation r "
            + "where r.consumableItem.id = :itemId and r.id <> :excludeId "
            + "and r.status in :statuses")
    int grossConsumableClaimedQuantity(@Param("itemId") UUID itemId,
                                       @Param("statuses") Collection<ReservationStatus> statuses,
                                       @Param("excludeId") UUID excludeId);

    /**
     * Stock already issued (partial fulfilments) against those same open
     * claiming reservations. Native query on the
     * {@code stock_transactions.reservation_id} fulfillment linkage, mirroring
     * {@link #issuedAgainstReservation}.
     */
    @Query(value = "select coalesce(sum(t.quantity), 0) from stock_transactions t "
            + "join reservations r on r.id = t.reservation_id "
            + "where r.consumable_item_id = :itemId and r.id <> :excludeId "
            + "and r.status in (:statuses) and t.transaction_type = 'ISSUE'",
            nativeQuery = true)
    BigDecimal issuedAgainstOpenClaims(@Param("itemId") UUID itemId,
                                       @Param("statuses") Collection<String> statuses,
                                       @Param("excludeId") UUID excludeId);

    /**
     * Total quantity already issued against a consumable reservation via stock
     * ISSUE transactions. Native query on {@code stock_transactions.reservation_id}
     * (the fulfillment linkage added with the stock-issue work) so this interface
     * does not depend on the consumable package's entity mapping.
     */
    @Query(value = "select coalesce(sum(t.quantity), 0) from stock_transactions t "
            + "where t.reservation_id = :reservationId and t.transaction_type = 'ISSUE'",
            nativeQuery = true)
    BigDecimal issuedAgainstReservation(@Param("reservationId") UUID reservationId);

    long countByStatus(ReservationStatus status);

    long countByRequestedByIdAndStatusIn(UUID userId, Collection<ReservationStatus> statuses);

    @Query("select r from Reservation r where r.status in :statuses and r.startAt between :from and :to")
    List<Reservation> findStartingBetween(@Param("from") Instant from, @Param("to") Instant to,
                                          @Param("statuses") Collection<ReservationStatus> statuses);
}
