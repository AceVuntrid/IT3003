package com.university.assets.maintenance;

import com.university.assets.common.model.Enums.MaintenanceIssueType;
import com.university.assets.common.model.Enums.MaintenanceStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceRequestRepository
        extends JpaRepository<MaintenanceRequest, UUID>, JpaSpecificationExecutor<MaintenanceRequest> {

    @EntityGraph(attributePaths = {"asset", "requestedBy", "assignedTo"})
    Optional<MaintenanceRequest> findDetailedById(UUID id);

    long countByStatusIn(Collection<MaintenanceStatus> statuses);

    boolean existsByAssetIdAndIssueTypeAndStatusIn(UUID assetId, MaintenanceIssueType issueType,
                                                   Collection<MaintenanceStatus> statuses);

    @Query("select coalesce(sum(m.totalCost), 0) from MaintenanceRequest m "
            + "where m.completedAt between :from and :to")
    BigDecimal totalCostBetween(@Param("from") Instant from, @Param("to") Instant to);
}
