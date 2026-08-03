package com.university.assets.role;

/**
 * Canonical permission codes. These are the single source of truth used by
 * {@code @PreAuthorize} checks, the database seeder and the frontend.
 */
public final class Permissions {

    private Permissions() {}

    public static final String ASSET_VIEW = "ASSET_VIEW";
    public static final String ASSET_CREATE = "ASSET_CREATE";
    public static final String ASSET_EDIT = "ASSET_EDIT";
    public static final String ASSET_ARCHIVE = "ASSET_ARCHIVE";
    public static final String ASSET_EXPORT = "ASSET_EXPORT";

    public static final String CONSUMABLE_VIEW = "CONSUMABLE_VIEW";
    public static final String CONSUMABLE_CREATE = "CONSUMABLE_CREATE";
    public static final String CONSUMABLE_EDIT = "CONSUMABLE_EDIT";
    public static final String CONSUMABLE_RECEIVE = "CONSUMABLE_RECEIVE";
    public static final String CONSUMABLE_ISSUE = "CONSUMABLE_ISSUE";
    public static final String CONSUMABLE_ADJUST = "CONSUMABLE_ADJUST";

    public static final String RESERVATION_VIEW = "RESERVATION_VIEW";
    public static final String RESERVATION_CREATE = "RESERVATION_CREATE";
    public static final String RESERVATION_APPROVE = "RESERVATION_APPROVE";
    public static final String RESERVATION_MANAGE = "RESERVATION_MANAGE";

    public static final String CHECKOUT_VIEW = "CHECKOUT_VIEW";
    public static final String CHECKOUT_CREATE = "CHECKOUT_CREATE";
    public static final String CHECKOUT_MANAGE = "CHECKOUT_MANAGE";

    public static final String MAINTENANCE_VIEW = "MAINTENANCE_VIEW";
    public static final String MAINTENANCE_CREATE = "MAINTENANCE_CREATE";
    public static final String MAINTENANCE_MANAGE = "MAINTENANCE_MANAGE";

    public static final String TRANSFER_VIEW = "TRANSFER_VIEW";
    public static final String TRANSFER_CREATE = "TRANSFER_CREATE";
    public static final String TRANSFER_APPROVE = "TRANSFER_APPROVE";

    public static final String LOCATION_VIEW = "LOCATION_VIEW";
    public static final String LOCATION_MANAGE = "LOCATION_MANAGE";

    public static final String ORG_MANAGE = "ORG_MANAGE";
    public static final String CATEGORY_MANAGE = "CATEGORY_MANAGE";

    public static final String PAYMENT_VIEW = "PAYMENT_VIEW";
    public static final String PAYMENT_CREATE = "PAYMENT_CREATE";
    public static final String PAYMENT_REFUND = "PAYMENT_REFUND";

    public static final String USER_VIEW = "USER_VIEW";
    public static final String USER_CREATE = "USER_CREATE";
    public static final String USER_EDIT = "USER_EDIT";
    public static final String USER_DEACTIVATE = "USER_DEACTIVATE";
    public static final String ROLE_MANAGE = "ROLE_MANAGE";

    public static final String REPORT_VIEW = "REPORT_VIEW";
    public static final String REPORT_EXPORT = "REPORT_EXPORT";

    public static final String AUDIT_VIEW = "AUDIT_VIEW";
    public static final String SETTINGS_MANAGE = "SETTINGS_MANAGE";
}
