package com.university.assets.asset;

import com.university.assets.category.AssetCategory;
import com.university.assets.common.model.BaseEntity;
import com.university.assets.common.model.Enums.AssetCondition;
import com.university.assets.common.model.Enums.AssetStatus;
import com.university.assets.common.model.Enums.AssetType;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "assets")
public class Asset extends BaseEntity {

    @Column(name = "asset_code", nullable = false, unique = true)
    private String assetCode;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private AssetCategory category;

    private String brand;
    private String model;
    private String manufacturer;

    @Column(name = "serial_number")
    private String serialNumber;

    private String barcode;

    @Column(name = "qr_code")
    private String qrCode;

    private String tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "location_notes")
    private String locationNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "custodian_user_id")
    private User custodian;

    @Column(name = "purchase_order_number")
    private String purchaseOrderNumber;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "funding_source")
    private String fundingSource;

    @Column(name = "grant_code")
    private String grantCode;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_price", precision = 15, scale = 2)
    private BigDecimal purchasePrice;

    @Column(nullable = false)
    private String currency = "LKR";

    @Column(name = "current_book_value", precision = 15, scale = 2)
    private BigDecimal currentBookValue;

    @Column(name = "depreciation_method")
    private String depreciationMethod;

    @Column(name = "useful_life_years")
    private Integer usefulLifeYears;

    @Column(name = "salvage_value", precision = 15, scale = 2)
    private BigDecimal salvageValue;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "initial_condition")
    private AssetCondition initialCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetCondition condition = AssetCondition.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus status = AssetStatus.AVAILABLE;

    @Column(nullable = false)
    private boolean reservable = true;

    @Column(name = "approval_required", nullable = false)
    private boolean approvalRequired = true;

    @Column(name = "external_use_allowed", nullable = false)
    private boolean externalUseAllowed = false;

    @Column(name = "deposit_required", nullable = false)
    private boolean depositRequired = false;

    @Column(name = "deposit_amount", precision = 15, scale = 2)
    private BigDecimal depositAmount;

    /** Price-list flat fee charged per reservation; null or zero = free. */
    @Column(name = "reservation_fee", precision = 15, scale = 2)
    private BigDecimal reservationFee;

    @Column(name = "max_reservation_hours")
    private Integer maxReservationHours;

    @Column(name = "warranty_start_date")
    private LocalDate warrantyStartDate;

    @Column(name = "warranty_end_date")
    private LocalDate warrantyEndDate;

    @Column(name = "warranty_provider")
    private String warrantyProvider;

    @Column(name = "service_interval_months")
    private Integer serviceIntervalMonths;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "next_service_date")
    private LocalDate nextServiceDate;

    @Column(name = "calibration_required", nullable = false)
    private boolean calibrationRequired = false;

    @Column(name = "calibration_interval_months")
    private Integer calibrationIntervalMonths;

    @Column(name = "last_calibration_date")
    private LocalDate lastCalibrationDate;

    @Column(name = "next_calibration_date")
    private LocalDate nextCalibrationDate;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_by")
    private java.util.UUID createdBy;

    public boolean isArchived() {
        return archivedAt != null;
    }

    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }
    public AssetCategory getCategory() { return category; }
    public void setCategory(AssetCategory category) { this.category = category; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getQrCode() { return qrCode; }
    public void setQrCode(String qrCode) { this.qrCode = qrCode; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Faculty getFaculty() { return faculty; }
    public void setFaculty(Faculty faculty) { this.faculty = faculty; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public String getLocationNotes() { return locationNotes; }
    public void setLocationNotes(String locationNotes) { this.locationNotes = locationNotes; }
    public User getCustodian() { return custodian; }
    public void setCustodian(User custodian) { this.custodian = custodian; }
    public String getPurchaseOrderNumber() { return purchaseOrderNumber; }
    public void setPurchaseOrderNumber(String purchaseOrderNumber) { this.purchaseOrderNumber = purchaseOrderNumber; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getFundingSource() { return fundingSource; }
    public void setFundingSource(String fundingSource) { this.fundingSource = fundingSource; }
    public String getGrantCode() { return grantCode; }
    public void setGrantCode(String grantCode) { this.grantCode = grantCode; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getCurrentBookValue() { return currentBookValue; }
    public void setCurrentBookValue(BigDecimal currentBookValue) { this.currentBookValue = currentBookValue; }
    public String getDepreciationMethod() { return depreciationMethod; }
    public void setDepreciationMethod(String depreciationMethod) { this.depreciationMethod = depreciationMethod; }
    public Integer getUsefulLifeYears() { return usefulLifeYears; }
    public void setUsefulLifeYears(Integer usefulLifeYears) { this.usefulLifeYears = usefulLifeYears; }
    public BigDecimal getSalvageValue() { return salvageValue; }
    public void setSalvageValue(BigDecimal salvageValue) { this.salvageValue = salvageValue; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
    public AssetCondition getInitialCondition() { return initialCondition; }
    public void setInitialCondition(AssetCondition initialCondition) { this.initialCondition = initialCondition; }
    public AssetCondition getCondition() { return condition; }
    public void setCondition(AssetCondition condition) { this.condition = condition; }
    public AssetStatus getStatus() { return status; }
    public void setStatus(AssetStatus status) { this.status = status; }
    public boolean isReservable() { return reservable; }
    public void setReservable(boolean reservable) { this.reservable = reservable; }
    public boolean isApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(boolean approvalRequired) { this.approvalRequired = approvalRequired; }
    public boolean isExternalUseAllowed() { return externalUseAllowed; }
    public void setExternalUseAllowed(boolean externalUseAllowed) { this.externalUseAllowed = externalUseAllowed; }
    public boolean isDepositRequired() { return depositRequired; }
    public void setDepositRequired(boolean depositRequired) { this.depositRequired = depositRequired; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public BigDecimal getReservationFee() { return reservationFee; }
    public void setReservationFee(BigDecimal reservationFee) { this.reservationFee = reservationFee; }
    public Integer getMaxReservationHours() { return maxReservationHours; }
    public void setMaxReservationHours(Integer maxReservationHours) { this.maxReservationHours = maxReservationHours; }
    public LocalDate getWarrantyStartDate() { return warrantyStartDate; }
    public void setWarrantyStartDate(LocalDate warrantyStartDate) { this.warrantyStartDate = warrantyStartDate; }
    public LocalDate getWarrantyEndDate() { return warrantyEndDate; }
    public void setWarrantyEndDate(LocalDate warrantyEndDate) { this.warrantyEndDate = warrantyEndDate; }
    public String getWarrantyProvider() { return warrantyProvider; }
    public void setWarrantyProvider(String warrantyProvider) { this.warrantyProvider = warrantyProvider; }
    public Integer getServiceIntervalMonths() { return serviceIntervalMonths; }
    public void setServiceIntervalMonths(Integer serviceIntervalMonths) { this.serviceIntervalMonths = serviceIntervalMonths; }
    public LocalDate getLastServiceDate() { return lastServiceDate; }
    public void setLastServiceDate(LocalDate lastServiceDate) { this.lastServiceDate = lastServiceDate; }
    public LocalDate getNextServiceDate() { return nextServiceDate; }
    public void setNextServiceDate(LocalDate nextServiceDate) { this.nextServiceDate = nextServiceDate; }
    public boolean isCalibrationRequired() { return calibrationRequired; }
    public void setCalibrationRequired(boolean calibrationRequired) { this.calibrationRequired = calibrationRequired; }
    public Integer getCalibrationIntervalMonths() { return calibrationIntervalMonths; }
    public void setCalibrationIntervalMonths(Integer calibrationIntervalMonths) { this.calibrationIntervalMonths = calibrationIntervalMonths; }
    public LocalDate getLastCalibrationDate() { return lastCalibrationDate; }
    public void setLastCalibrationDate(LocalDate lastCalibrationDate) { this.lastCalibrationDate = lastCalibrationDate; }
    public LocalDate getNextCalibrationDate() { return nextCalibrationDate; }
    public void setNextCalibrationDate(LocalDate nextCalibrationDate) { this.nextCalibrationDate = nextCalibrationDate; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public java.util.UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(java.util.UUID createdBy) { this.createdBy = createdBy; }
}
