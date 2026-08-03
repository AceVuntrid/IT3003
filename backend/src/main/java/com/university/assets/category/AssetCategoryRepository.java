package com.university.assets.category;

import com.university.assets.common.model.Enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetCategoryRepository extends JpaRepository<AssetCategory, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    List<AssetCategory> findAllByOrderByNameAsc();

    List<AssetCategory> findByAssetTypeAndActiveTrueOrderByNameAsc(AssetType assetType);
}
