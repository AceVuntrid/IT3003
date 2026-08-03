package com.university.assets.maintenance;

import com.university.assets.asset.Asset;
import com.university.assets.common.model.BaseEntity;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.MaintenanceIssueType;
import com.university.assets.common.model.Enums.MaintenancePriority;
import com.university.assets.common.model.Enums.MaintenanceStatus;
import com.university.assets.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "maintenance_requests")
public class MaintenanceRequest extends BaseEntity {

    @Column(name = "request_number", nullable = false, unique = true)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false)
    private MaintenanceIssueType issueType;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenancePriority priority = MaintenancePriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status = MaintenanceStatus.OPEN;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    private String diagnosis;

    @Column(name = "work_performed")
    private String workPerformed;

    @Column(name = "parts_used")
    private String partsUsed;

    @Column(name = "labour_cost", precision = 15, scale = 2)
    private BigDecimal labourCost;

    @Column(name = "parts_cost", precision = 15, scale = 2)
    private BigDecimal partsCost;

    @Column(name = "external_cost", precision = 15, scale = 2)
    private BigDecimal externalCost;

    @Column(name = "total_cost", precision = 15, scale = 2)
    private BigDecimal totalCost;

    private String result;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_condition")
    private AssetCondition newCondition;

    @Column(name = "next_service_date")
    private LocalDate nextServiceDate;

    private String notes;

    public String getRequestNumber() { return requestNumber; }
    public void setRequestNumber(String requestNumber) { this.requestNumber = requestNumber; }
    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }
    public MaintenanceIssueType getIssueType() { return issueType; }
    public void setIssueType(MaintenanceIssueType issueType) { this.issueType = issueType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public MaintenancePriority getPriority() { return priority; }
    public void setPriority(MaintenancePriority priority) { this.priority = priority; }
    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }
    public User getAssignedTo() { return assignedTo; }
    public void setAssignedTo(User assignedTo) { this.assignedTo = assignedTo; }
    public MaintenanceStatus getStatus() { return status; }
    public void setStatus(MaintenanceStatus status) { this.status = status; }
    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getWorkPerformed() { return workPerformed; }
    public void setWorkPerformed(String workPerformed) { this.workPerformed = workPerformed; }
    public String getPartsUsed() { return partsUsed; }
    public void setPartsUsed(String partsUsed) { this.partsUsed = partsUsed; }
    public BigDecimal getLabourCost() { return labourCost; }
    public void setLabourCost(BigDecimal labourCost) { this.labourCost = labourCost; }
    public BigDecimal getPartsCost() { return partsCost; }
    public void setPartsCost(BigDecimal partsCost) { this.partsCost = partsCost; }
    public BigDecimal getExternalCost() { return externalCost; }
    public void setExternalCost(BigDecimal externalCost) { this.externalCost = externalCost; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public AssetCondition getNewCondition() { return newCondition; }
    public void setNewCondition(AssetCondition newCondition) { this.newCondition = newCondition; }
    public LocalDate getNextServiceDate() { return nextServiceDate; }
    public void setNextServiceDate(LocalDate nextServiceDate) { this.nextServiceDate = nextServiceDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
