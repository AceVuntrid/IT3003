package com.university.assets.consumable;

import com.university.assets.category.AssetCategory;
import com.university.assets.common.model.BaseEntity;
import com.university.assets.department.Department;
import com.university.assets.faculty.Faculty;
import com.university.assets.location.Location;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "consumable_items")
public class ConsumableItem extends BaseEntity {

    @Column(name = "item_code", nullable = false, unique = true)
    private String itemCode;

    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private AssetCategory category;

    private String brand;
    private String manufacturer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "unit_of_measure", nullable = false)
    private String unitOfMeasure;

    @Column(name = "current_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal currentQuantity = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false, precision = 15, scale = 3)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Column(name = "reorder_level", nullable = false, precision = 15, scale = 3)
    private BigDecimal reorderLevel = BigDecimal.ZERO;

    @Column(name = "maximum_stock_level", precision = 15, scale = 3)
    private BigDecimal maximumStockLevel;

    @Column(name = "unit_cost", precision = 15, scale = 2)
    private BigDecimal unitCost;

    /** Price-list fee charged per unit issued against a reservation; null or zero = free. */
    @Column(name = "unit_fee", precision = 15, scale = 2)
    private BigDecimal unitFee;

    @Column(nullable = false)
    private boolean hazardous = false;

    @Column(name = "chemical_classification")
    private String chemicalClassification;

    @Column(name = "storage_instructions")
    private String storageInstructions;

    @Column(name = "disposal_instructions")
    private String disposalInstructions;

    @Column(nullable = false)
    private boolean active = true;

    public BigDecimal getAvailableQuantity() {
        return currentQuantity.subtract(reservedQuantity);
    }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AssetCategory getCategory() { return category; }
    public void setCategory(AssetCategory category) { this.category = category; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
    public BigDecimal getCurrentQuantity() { return currentQuantity; }
    public void setCurrentQuantity(BigDecimal currentQuantity) { this.currentQuantity = currentQuantity; }
    public BigDecimal getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(BigDecimal reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    public BigDecimal getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(BigDecimal reorderLevel) { this.reorderLevel = reorderLevel; }
    public BigDecimal getMaximumStockLevel() { return maximumStockLevel; }
    public void setMaximumStockLevel(BigDecimal maximumStockLevel) { this.maximumStockLevel = maximumStockLevel; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public BigDecimal getUnitFee() { return unitFee; }
    public void setUnitFee(BigDecimal unitFee) { this.unitFee = unitFee; }
    public boolean isHazardous() { return hazardous; }
    public void setHazardous(boolean hazardous) { this.hazardous = hazardous; }
    public String getChemicalClassification() { return chemicalClassification; }
    public void setChemicalClassification(String chemicalClassification) { this.chemicalClassification = chemicalClassification; }
    public String getStorageInstructions() { return storageInstructions; }
    public void setStorageInstructions(String storageInstructions) { this.storageInstructions = storageInstructions; }
    public String getDisposalInstructions() { return disposalInstructions; }
    public void setDisposalInstructions(String disposalInstructions) { this.disposalInstructions = disposalInstructions; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
