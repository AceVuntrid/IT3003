package com.university.assets.consumable;

import com.university.assets.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "consumable_batches")
public class ConsumableBatch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumable_item_id")
    private ConsumableItem item;

    @Column(name = "batch_number", nullable = false)
    private String batchNumber;

    @Column(name = "quantity_received", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantityReceived;

    @Column(name = "quantity_remaining", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantityRemaining;

    @Column(name = "manufacture_date")
    private LocalDate manufactureDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "unit_cost", precision = 15, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    public ConsumableItem getItem() { return item; }
    public void setItem(ConsumableItem item) { this.item = item; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public BigDecimal getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(BigDecimal quantityReceived) { this.quantityReceived = quantityReceived; }
    public BigDecimal getQuantityRemaining() { return quantityRemaining; }
    public void setQuantityRemaining(BigDecimal quantityRemaining) { this.quantityRemaining = quantityRemaining; }
    public LocalDate getManufactureDate() { return manufactureDate; }
    public void setManufactureDate(LocalDate manufactureDate) { this.manufactureDate = manufactureDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public void setReceivedDate(LocalDate receivedDate) { this.receivedDate = receivedDate; }
}
