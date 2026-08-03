package com.university.assets.consumable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ConsumableBatchRepository extends JpaRepository<ConsumableBatch, UUID> {

    List<ConsumableBatch> findByItemIdOrderByReceivedDateDesc(UUID itemId);

    /** FEFO: earliest expiry first (nulls last), then oldest received. */
    @Query("select b from ConsumableBatch b where b.item.id = :itemId and b.quantityRemaining > 0 "
            + "order by case when b.expiryDate is null then 1 else 0 end, b.expiryDate asc, b.receivedDate asc")
    List<ConsumableBatch> findIssuableBatches(@Param("itemId") UUID itemId);

    @Query("select b from ConsumableBatch b join fetch b.item where b.quantityRemaining > 0 "
            + "and b.expiryDate is not null and b.expiryDate <= :cutoff order by b.expiryDate asc")
    List<ConsumableBatch> findExpiringBefore(@Param("cutoff") LocalDate cutoff);

    @Query("select count(distinct b.item.id) from ConsumableBatch b where b.quantityRemaining > 0 "
            + "and b.expiryDate is not null and b.expiryDate <= :cutoff")
    long countItemsExpiringBefore(@Param("cutoff") LocalDate cutoff);
}
