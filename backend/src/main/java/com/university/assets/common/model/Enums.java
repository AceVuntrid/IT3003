package com.university.assets.common.model;

public final class Enums {

    private Enums() {}

    public enum AssetType { FIXED, CONSUMABLE, FACILITY, ROOM, VEHICLE }

    public enum AssetCondition { NEW, EXCELLENT, GOOD, FAIR, POOR, DAMAGED, UNSERVICEABLE }

    public enum AssetStatus { AVAILABLE, RESERVED, CHECKED_OUT, UNDER_MAINTENANCE, DAMAGED, LOST, ARCHIVED, DISPOSED }

    public enum LocationType { CAMPUS, BUILDING, FLOOR, ROOM, LECTURE_ROOM, AUDITORIUM, LABORATORY, STORAGE_AREA }

    public enum AccountStatus { ACTIVE, DISABLED, LOCKED }

    public enum ReservationStatus {
        DRAFT, SUBMITTED, PENDING_APPROVAL, APPROVED, REJECTED, CANCELLED,
        READY_FOR_COLLECTION, CHECKED_OUT, COMPLETED, OVERDUE, NO_SHOW
    }

    public enum ApprovalTier { TIER_1_OFFICER, TIER_2_TECHNICAL, TIER_3_HOD }

    public enum ApprovalStep { PENDING_LEVEL_1, PENDING_LEVEL_2, APPROVED, REJECTED }

    public enum ApprovalStatus { PENDING, PENDING_LEVEL_1, PENDING_LEVEL_2, APPROVED, REJECTED }

    public enum CheckoutStatus { CHECKED_OUT, RETURNED, OVERDUE }

    public enum MaintenanceIssueType { FAULT, PREVENTIVE, CALIBRATION, INSPECTION, CLEANING, SOFTWARE_UPDATE, OTHER }

    public enum MaintenancePriority { LOW, MEDIUM, HIGH, URGENT }

    public enum MaintenanceStatus {
        OPEN, ASSIGNED, IN_PROGRESS, WAITING_FOR_PARTS, WAITING_FOR_VENDOR, COMPLETED, CANCELLED, UNREPAIRABLE
    }

    public enum TransferStatus { PENDING_APPROVAL, APPROVED, REJECTED, COMPLETED, CANCELLED }

    public enum StockTransactionType { RECEIVE, ISSUE, ADJUST_INCREASE, ADJUST_DECREASE }

    public enum TransactionType {
        RESERVATION_FEE, EQUIPMENT_USAGE_FEE, LAB_SETUP_FEE, FACILITY_FEE, CONSUMABLE_CHARGE,
        SECURITY_DEPOSIT, DAMAGE_CHARGE, LATE_PENALTY, REFUND, INTERNAL_CHARGE, OTHER
    }

    public enum PayerType { USER, DEPARTMENT, FACULTY, EXTERNAL }

    public enum PaymentStatus { PENDING, PAID, PARTIALLY_REFUNDED, REFUNDED, CANCELLED }
}
