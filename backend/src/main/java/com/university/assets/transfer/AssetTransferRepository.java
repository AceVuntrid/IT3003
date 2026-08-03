package com.university.assets.transfer;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface AssetTransferRepository
        extends JpaRepository<AssetTransfer, UUID>, JpaSpecificationExecutor<AssetTransfer> {

    @EntityGraph(attributePaths = {"asset", "fromLocation", "toLocation", "fromCustodian",
            "toCustodian", "requestedBy", "approvedBy", "receivedBy"})
    Optional<AssetTransfer> findDetailedById(UUID id);
}
