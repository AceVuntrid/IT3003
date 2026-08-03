package com.university.assets.consumable;

import com.university.assets.common.model.BaseEntity;
import com.university.assets.common.model.Enums.StockTransactionType;
import com.university.assets.department.Department;
import com.university.assets.reservation.Reservation;
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
import java.util.UUID;

@Entity
@Table(name = "stock_transactions")
public class StockTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumable_item_id")
    private ConsumableItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private ConsumableBatch batch;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private StockTransactionType transactionType;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_user_id")
    private User relatedUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_department_id")
    private Department relatedDepartment;

    /** The approved consumable reservation this ISSUE fulfils, when linked. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    private String purpose;

    private String reason;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "chargeable", nullable = false)
    private boolean chargeable = false;

    @Column(name = "charge_amount", precision = 15, scale = 2)
    private BigDecimal chargeAmount;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    public ConsumableItem getItem() { return item; }
    public void setItem(ConsumableItem item) { this.item = item; }
    public ConsumableBatch getBatch() { return batch; }
    public void setBatch(ConsumableBatch batch) { this.batch = batch; }
    public StockTransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(StockTransactionType transactionType) { this.transactionType = transactionType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public User getRelatedUser() { return relatedUser; }
    public void setRelatedUser(User relatedUser) { this.relatedUser = relatedUser; }
    public Department getRelatedDepartment() { return relatedDepartment; }
    public void setRelatedDepartment(Department relatedDepartment) { this.relatedDepartment = relatedDepartment; }
    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public boolean isChargeable() { return chargeable; }
    public void setChargeable(boolean chargeable) { this.chargeable = chargeable; }
    public BigDecimal getChargeAmount() { return chargeAmount; }
    public void setChargeAmount(BigDecimal chargeAmount) { this.chargeAmount = chargeAmount; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
