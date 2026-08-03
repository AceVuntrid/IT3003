package com.university.assets.asset;

import com.university.assets.common.model.Enums.AssetStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID>, JpaSpecificationExecutor<Asset> {

    boolean existsByAssetCodeIgnoreCase(String assetCode);

    boolean existsBySerialNumberIgnoreCase(String serialNumber);

    @EntityGraph(attributePaths = {"category", "faculty", "department", "location", "custodian"})
    Optional<Asset> findDetailedById(UUID id);

    long countByArchivedAtIsNull();

    long countByStatusAndArchivedAtIsNull(AssetStatus status);

    @Query("select coalesce(sum(a.purchasePrice), 0) from Asset a where a.archivedAt is null")
    BigDecimal totalPurchaseValue();

    @Query("select count(a) from Asset a where a.archivedAt is null and a.nextServiceDate is not null and a.nextServiceDate <= :date")
    long countMaintenanceDueBy(@Param("date") LocalDate date);

    List<Asset> findByArchivedAtIsNullAndNextServiceDateLessThanEqual(LocalDate date);

    List<Asset> findByArchivedAtIsNullAndWarrantyEndDateBetween(LocalDate from, LocalDate to);
}
