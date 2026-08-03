package com.university.assets.consumable;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsumableItemRepository
        extends JpaRepository<ConsumableItem, UUID>, JpaSpecificationExecutor<ConsumableItem> {

    boolean existsByItemCodeIgnoreCase(String itemCode);

    @EntityGraph(attributePaths = {"category", "faculty", "department", "location"})
    Optional<ConsumableItem> findDetailedById(UUID id);

    @Query("select i from ConsumableItem i where i.active = true and i.currentQuantity <= i.reorderLevel")
    List<ConsumableItem> findLowStock();

    @Query("select count(i) from ConsumableItem i where i.active = true and i.currentQuantity <= i.reorderLevel")
    long countLowStock();
}
