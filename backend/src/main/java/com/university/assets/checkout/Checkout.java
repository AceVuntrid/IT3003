package com.university.assets.checkout;

import com.university.assets.asset.Asset;
import com.university.assets.common.model.BaseEntity;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.CheckoutStatus;
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
import java.time.Instant;

@Entity
@Table(name = "checkouts")
public class Checkout extends BaseEntity {

    @Column(name = "checkout_number", nullable = false, unique = true)
    private String checkoutNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "checked_out_at", nullable = false)
    private Instant checkedOutAt;

    @Column(name = "expected_return_at", nullable = false)
    private Instant expectedReturnAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_before", nullable = false)
    private AssetCondition conditionBefore;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_after")
    private AssetCondition conditionAfter;

    @Column(name = "accessories")
    private String accessories;

    @Column(name = "missing_accessories")
    private String missingAccessories;

    @Column(name = "damage_detected", nullable = false)
    private boolean damageDetected = false;

    @Column(name = "damage_description")
    private String damageDescription;

    @Column(name = "deposit_paid", precision = 15, scale = 2)
    private BigDecimal depositPaid;

    @Column(name = "penalty_amount", precision = 15, scale = 2)
    private BigDecimal penaltyAmount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_by")
    private User issuedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private User receivedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckoutStatus status = CheckoutStatus.CHECKED_OUT;

    private String notes;

    public String getCheckoutNumber() { return checkoutNumber; }
    public void setCheckoutNumber(String checkoutNumber) { this.checkoutNumber = checkoutNumber; }
    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) { this.reservation = reservation; }
    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public Instant getCheckedOutAt() { return checkedOutAt; }
    public void setCheckedOutAt(Instant checkedOutAt) { this.checkedOutAt = checkedOutAt; }
    public Instant getExpectedReturnAt() { return expectedReturnAt; }
    public void setExpectedReturnAt(Instant expectedReturnAt) { this.expectedReturnAt = expectedReturnAt; }
    public Instant getReturnedAt() { return returnedAt; }
    public void setReturnedAt(Instant returnedAt) { this.returnedAt = returnedAt; }
    public AssetCondition getConditionBefore() { return conditionBefore; }
    public void setConditionBefore(AssetCondition conditionBefore) { this.conditionBefore = conditionBefore; }
    public AssetCondition getConditionAfter() { return conditionAfter; }
    public void setConditionAfter(AssetCondition conditionAfter) { this.conditionAfter = conditionAfter; }
    public String getAccessories() { return accessories; }
    public void setAccessories(String accessories) { this.accessories = accessories; }
    public String getMissingAccessories() { return missingAccessories; }
    public void setMissingAccessories(String missingAccessories) { this.missingAccessories = missingAccessories; }
    public boolean isDamageDetected() { return damageDetected; }
    public void setDamageDetected(boolean damageDetected) { this.damageDetected = damageDetected; }
    public String getDamageDescription() { return damageDescription; }
    public void setDamageDescription(String damageDescription) { this.damageDescription = damageDescription; }
    public BigDecimal getDepositPaid() { return depositPaid; }
    public void setDepositPaid(BigDecimal depositPaid) { this.depositPaid = depositPaid; }
    public BigDecimal getPenaltyAmount() { return penaltyAmount; }
    public void setPenaltyAmount(BigDecimal penaltyAmount) { this.penaltyAmount = penaltyAmount; }
    public User getIssuedBy() { return issuedBy; }
    public void setIssuedBy(User issuedBy) { this.issuedBy = issuedBy; }
    public User getReceivedBy() { return receivedBy; }
    public void setReceivedBy(User receivedBy) { this.receivedBy = receivedBy; }
    public CheckoutStatus getStatus() { return status; }
    public void setStatus(CheckoutStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
