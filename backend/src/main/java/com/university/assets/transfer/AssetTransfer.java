package com.university.assets.transfer;

import com.university.assets.asset.Asset;
import com.university.assets.common.model.BaseEntity;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.TransferStatus;
import com.university.assets.location.Location;
import com.university.assets.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "asset_transfers")
public class AssetTransfer extends BaseEntity {

    @Column(name = "transfer_number", nullable = false, unique = true)
    private String transferNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Column(nullable = false)
    private int quantity = 1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_location_id")
    private Location fromLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_location_id")
    private Location toLocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_custodian_id")
    private User fromCustodian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_custodian_id")
    private User toCustodian;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.PENDING_APPROVAL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private User receivedBy;

    @Column(name = "expected_date")
    private Instant expectedDate;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_at_destination")
    private AssetCondition conditionAtDestination;

    private String notes;

    public String getTransferNumber() { return transferNumber; }
    public void setTransferNumber(String transferNumber) { this.transferNumber = transferNumber; }
    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Location getFromLocation() { return fromLocation; }
    public void setFromLocation(Location fromLocation) { this.fromLocation = fromLocation; }
    public Location getToLocation() { return toLocation; }
    public void setToLocation(Location toLocation) { this.toLocation = toLocation; }
    public User getFromCustodian() { return fromCustodian; }
    public void setFromCustodian(User fromCustodian) { this.fromCustodian = fromCustodian; }
    public User getToCustodian() { return toCustodian; }
    public void setToCustodian(User toCustodian) { this.toCustodian = toCustodian; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    public User getReceivedBy() { return receivedBy; }
    public void setReceivedBy(User receivedBy) { this.receivedBy = receivedBy; }
    public Instant getExpectedDate() { return expectedDate; }
    public void setExpectedDate(Instant expectedDate) { this.expectedDate = expectedDate; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public AssetCondition getConditionAtDestination() { return conditionAtDestination; }
    public void setConditionAtDestination(AssetCondition conditionAtDestination) { this.conditionAtDestination = conditionAtDestination; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
