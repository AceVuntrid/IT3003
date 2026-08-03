package com.university.assets.reservation;

import com.university.assets.asset.Asset;
import com.university.assets.common.model.BaseEntity;
import com.university.assets.common.model.Enums.ApprovalStatus;
import com.university.assets.common.model.Enums.ApprovalStep;
import com.university.assets.common.model.Enums.ApprovalTier;
import com.university.assets.common.model.Enums.ReservationStatus;
import com.university.assets.consumable.ConsumableItem;
import com.university.assets.department.Department;
import com.university.assets.faculty.Faculty;
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
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reservations")
public class Reservation extends BaseEntity {

    @Column(name = "reservation_number", nullable = false, unique = true)
    private String reservationNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consumable_item_id")
    private ConsumableItem consumableItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false)
    private String purpose;

    @Column(name = "course_or_project")
    private String courseOrProject;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(nullable = false)
    private int quantity = 1;

    /** Originally requested quantity; {@code quantity} is the approved/effective one. */
    @Column(name = "requested_quantity")
    private Integer requestedQuantity;

    /**
     * Total quantity ever issued against this reservation: asset checkout slips
     * (returned ones still count as issued) plus reservation-linked stock ISSUE
     * transactions fulfilling consumable reservations. Read-only database
     * aggregate matching {@code ReservationRepository#issuedAgainstReservation}.
     */
    @Formula("(select coalesce(sum(c.quantity), 0) from checkouts c where c.reservation_id = id)"
            + " + (select coalesce(cast(sum(t.quantity) as integer), 0) from stock_transactions t"
            + " where t.reservation_id = id and t.transaction_type = 'ISSUE')")
    private int issuedQuantity;

    @Column(name = "fee_amount", precision = 15, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "fee_waived", nullable = false)
    private boolean feeWaived = false;

    @Column(name = "participant_count")
    private Integer participantCount;

    @Column(name = "special_requirements")
    private String specialRequirements;

    @Column(name = "external_use_requested", nullable = false)
    private boolean externalUseRequested = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approval_notes")
    private String approvalNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_approval_tier", nullable = false)
    private ApprovalTier requiredApprovalTier = ApprovalTier.TIER_1_OFFICER;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_approval_step", nullable = false)
    private ApprovalStep currentApprovalStep = ApprovalStep.PENDING_LEVEL_1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level1_approved_by")
    private User level1ApprovedBy;

    @Column(name = "level1_approved_at")
    private Instant level1ApprovedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level2_approved_by")
    private User level2ApprovedBy;

    @Column(name = "level2_approved_at")
    private Instant level2ApprovedAt;

    /**
     * 4-digit code generated at final approval of an asset reservation; the
     * borrower quotes it at handover. Never set for venue/room bookings.
     */
    @Column(name = "collection_code", length = 4)
    private String collectionCode;

    public String getReservationNumber() { return reservationNumber; }
    public void setReservationNumber(String reservationNumber) { this.reservationNumber = reservationNumber; }
    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public ConsumableItem getConsumableItem() { return consumableItem; }
    public void setConsumableItem(ConsumableItem consumableItem) { this.consumableItem = consumableItem; }
    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User requestedBy) { this.requestedBy = requestedBy; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getCourseOrProject() { return courseOrProject; }
    public void setCourseOrProject(String courseOrProject) { this.courseOrProject = courseOrProject; }
    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }
    public Instant getEndAt() { return endAt; }
    public void setEndAt(Instant endAt) { this.endAt = endAt; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Integer getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(Integer requestedQuantity) { this.requestedQuantity = requestedQuantity; }
    public int getIssuedQuantity() { return issuedQuantity; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public boolean isFeeWaived() { return feeWaived; }
    public void setFeeWaived(boolean feeWaived) { this.feeWaived = feeWaived; }
    public Integer getParticipantCount() { return participantCount; }
    public void setParticipantCount(Integer participantCount) { this.participantCount = participantCount; }
    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }
    public boolean isExternalUseRequested() { return externalUseRequested; }
    public void setExternalUseRequested(boolean externalUseRequested) { this.externalUseRequested = externalUseRequested; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    public ApprovalTier getRequiredApprovalTier() { return requiredApprovalTier; }
    public void setRequiredApprovalTier(ApprovalTier requiredApprovalTier) { this.requiredApprovalTier = requiredApprovalTier; }
    public ApprovalStep getCurrentApprovalStep() { return currentApprovalStep; }
    public void setCurrentApprovalStep(ApprovalStep currentApprovalStep) { this.currentApprovalStep = currentApprovalStep; }
    public User getLevel1ApprovedBy() { return level1ApprovedBy; }
    public void setLevel1ApprovedBy(User level1ApprovedBy) { this.level1ApprovedBy = level1ApprovedBy; }
    public Instant getLevel1ApprovedAt() { return level1ApprovedAt; }
    public void setLevel1ApprovedAt(Instant level1ApprovedAt) { this.level1ApprovedAt = level1ApprovedAt; }
    public User getLevel2ApprovedBy() { return level2ApprovedBy; }
    public void setLevel2ApprovedBy(User level2ApprovedBy) { this.level2ApprovedBy = level2ApprovedBy; }
    public Instant getLevel2ApprovedAt() { return level2ApprovedAt; }
    public void setLevel2ApprovedAt(Instant level2ApprovedAt) { this.level2ApprovedAt = level2ApprovedAt; }
    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getApprovalNotes() { return approvalNotes; }
    public void setApprovalNotes(String approvalNotes) { this.approvalNotes = approvalNotes; }
    public String getCollectionCode() { return collectionCode; }
    public void setCollectionCode(String collectionCode) { this.collectionCode = collectionCode; }
}
