package com.university.assets.consumable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID> {

    @EntityGraph(attributePaths = {"batch", "relatedUser", "relatedDepartment", "reservation"})
    List<StockTransaction> findByItemIdOrderByCreatedAtDesc(UUID itemId);

    /** All issues recorded against a consumable reservation (partial fulfilments). */
    List<StockTransaction> findByReservationId(UUID reservationId);
}
