package com.university.assets.category;

import com.university.assets.common.model.BaseEntity;
import com.university.assets.common.model.Enums.ApprovalTier;
import com.university.assets.common.model.Enums.AssetType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "asset_categories")
public class AssetCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private AssetCategory parent;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_approval_tier", nullable = false)
    private ApprovalTier requiredApprovalTier = ApprovalTier.TIER_1_OFFICER;

    @Column(nullable = false)
    private boolean active = true;

    public AssetCategory getParent() { return parent; }
    public void setParent(AssetCategory parent) { this.parent = parent; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }
    public ApprovalTier getRequiredApprovalTier() { return requiredApprovalTier; }
    public void setRequiredApprovalTier(ApprovalTier requiredApprovalTier) { this.requiredApprovalTier = requiredApprovalTier; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
