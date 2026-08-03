package com.university.assets.checkout;

import com.university.assets.common.model.Enums.CheckoutStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckoutRepository extends JpaRepository<Checkout, UUID>, JpaSpecificationExecutor<Checkout> {

    @EntityGraph(attributePaths = {"asset", "user", "issuedBy", "receivedBy", "reservation"})
    Optional<Checkout> findDetailedById(UUID id);

    @EntityGraph(attributePaths = {"asset", "user"})
    List<Checkout> findByStatusInAndExpectedReturnAtBefore(
            Collection<CheckoutStatus> statuses, Instant cutoff);

    long countByStatus(CheckoutStatus status);

    /** All issue slips recorded against a reservation, regardless of status. */
    List<Checkout> findByReservationId(UUID reservationId);

    /** Earliest expected return among the given statuses for an asset, or null when none. */
    @Query("select min(c.expectedReturnAt) from Checkout c "
            + "where c.asset.id = :assetId and c.status in :statuses")
    Instant findEarliestExpectedReturn(@Param("assetId") UUID assetId,
                                       @Param("statuses") Collection<CheckoutStatus> statuses);
}
