-- University Asset Management and Reservation System — initial schema

create table faculties (
    id uuid primary key,
    code text not null unique,
    name text not null,
    description text,
    active boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table departments (
    id uuid primary key,
    faculty_id uuid not null references faculties (id),
    code text not null unique,
    name text not null,
    description text,
    active boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table roles (
    id uuid primary key,
    name text not null unique,
    description text,
    system_role boolean not null default false,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table permissions (
    id uuid primary key,
    code text not null unique,
    module text not null,
    action text not null,
    description text,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table role_permissions (
    role_id uuid not null references roles (id),
    permission_id uuid not null references permissions (id),
    primary key (role_id, permission_id)
);

create table users (
    id uuid primary key,
    university_id text not null unique,
    first_name text not null,
    last_name text not null,
    email text not null unique,
    phone text,
    password_hash text not null,
    faculty_id uuid references faculties (id),
    department_id uuid references departments (id),
    account_status text not null default 'ACTIVE',
    user_type text,
    last_login_at timestamptz,
    must_change_password boolean not null default false,
    failed_login_attempts integer not null default 0,
    locked_until timestamptz,
    reservation_limit integer,
    external_borrowing_allowed boolean not null default false,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table user_roles (
    user_id uuid not null references users (id),
    role_id uuid not null references roles (id),
    primary key (user_id, role_id)
);

create table refresh_tokens (
    id uuid primary key,
    user_id uuid not null references users (id),
    token_hash text not null unique,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table password_reset_tokens (
    id uuid primary key,
    user_id uuid not null references users (id),
    token_hash text not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table locations (
    id uuid primary key,
    parent_id uuid references locations (id),
    faculty_id uuid references faculties (id),
    department_id uuid references departments (id),
    code text not null unique,
    name text not null,
    type text not null,
    address text,
    capacity integer,
    responsible_user_id uuid references users (id),
    description text,
    active boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table asset_categories (
    id uuid primary key,
    parent_id uuid references asset_categories (id),
    code text not null unique,
    name text not null,
    description text,
    asset_type text not null,
    active boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table suppliers (
    id uuid primary key,
    supplier_code text not null unique,
    name text not null,
    registration_number text,
    tax_number text,
    contact_person text,
    email text,
    phone text,
    website text,
    address text,
    city text,
    country text,
    payment_terms text,
    notes text,
    rating integer,
    active boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table assets (
    id uuid primary key,
    asset_code text not null unique,
    name text not null,
    description text,
    asset_type text not null,
    category_id uuid not null references asset_categories (id),
    brand text,
    model text,
    manufacturer text,
    serial_number text,
    barcode text,
    qr_code text,
    tags text,
    faculty_id uuid not null references faculties (id),
    department_id uuid references departments (id),
    location_id uuid not null references locations (id),
    location_notes text,
    custodian_user_id uuid references users (id),
    supplier_id uuid references suppliers (id),
    purchase_order_number text,
    invoice_number text,
    funding_source text,
    grant_code text,
    purchase_date date,
    purchase_price numeric(15, 2),
    currency text not null default 'LKR',
    current_book_value numeric(15, 2),
    depreciation_method text,
    useful_life_years integer,
    salvage_value numeric(15, 2),
    quantity integer not null default 1,
    available_quantity integer not null default 1,
    initial_condition text,
    condition text not null default 'GOOD',
    status text not null default 'AVAILABLE',
    reservable boolean not null default true,
    approval_required boolean not null default true,
    external_use_allowed boolean not null default false,
    deposit_required boolean not null default false,
    deposit_amount numeric(15, 2),
    max_reservation_hours integer,
    warranty_start_date date,
    warranty_end_date date,
    warranty_provider text,
    service_interval_months integer,
    last_service_date date,
    next_service_date date,
    calibration_required boolean not null default false,
    calibration_interval_months integer,
    last_calibration_date date,
    next_calibration_date date,
    archived_at timestamptz,
    created_by uuid,
    created_at timestamptz not null,
    updated_at timestamptz
);

create index idx_assets_faculty on assets (faculty_id);
create index idx_assets_location on assets (location_id);
create index idx_assets_category on assets (category_id);
create index idx_assets_status on assets (status);

create table consumable_items (
    id uuid primary key,
    item_code text not null unique,
    name text not null,
    description text,
    category_id uuid not null references asset_categories (id),
    brand text,
    manufacturer text,
    faculty_id uuid not null references faculties (id),
    department_id uuid references departments (id),
    location_id uuid not null references locations (id),
    unit_of_measure text not null,
    current_quantity numeric(15, 3) not null default 0,
    reserved_quantity numeric(15, 3) not null default 0,
    reorder_level numeric(15, 3) not null default 0,
    maximum_stock_level numeric(15, 3),
    unit_cost numeric(15, 2),
    hazardous boolean not null default false,
    chemical_classification text,
    storage_instructions text,
    disposal_instructions text,
    active boolean not null default true,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table consumable_batches (
    id uuid primary key,
    consumable_item_id uuid not null references consumable_items (id),
    batch_number text not null,
    quantity_received numeric(15, 3) not null,
    quantity_remaining numeric(15, 3) not null,
    manufacture_date date,
    expiry_date date,
    unit_cost numeric(15, 2),
    supplier_id uuid references suppliers (id),
    received_date date not null,
    created_at timestamptz not null,
    updated_at timestamptz
);

create index idx_batches_item on consumable_batches (consumable_item_id);

create table stock_transactions (
    id uuid primary key,
    consumable_item_id uuid not null references consumable_items (id),
    batch_id uuid references consumable_batches (id),
    transaction_type text not null,
    quantity numeric(15, 3) not null,
    related_user_id uuid references users (id),
    related_department_id uuid references departments (id),
    purpose text,
    reason text,
    reference_number text,
    chargeable boolean not null default false,
    charge_amount numeric(15, 2),
    created_by uuid not null,
    created_at timestamptz not null,
    updated_at timestamptz
);

create index idx_stock_tx_item on stock_transactions (consumable_item_id);

create table reservations (
    id uuid primary key,
    reservation_number text not null unique,
    asset_id uuid references assets (id),
    location_id uuid references locations (id),
    requested_by uuid not null references users (id),
    faculty_id uuid references faculties (id),
    department_id uuid references departments (id),
    purpose text not null,
    course_or_project text,
    start_at timestamptz not null,
    end_at timestamptz not null,
    quantity integer not null default 1,
    participant_count integer,
    special_requirements text,
    external_use_requested boolean not null default false,
    status text not null default 'SUBMITTED',
    approval_status text not null default 'PENDING',
    approved_by uuid references users (id),
    approved_at timestamptz,
    approval_notes text,
    created_at timestamptz not null,
    updated_at timestamptz
);

create index idx_reservations_asset on reservations (asset_id);
create index idx_reservations_window on reservations (start_at, end_at);
create index idx_reservations_status on reservations (status);

create table checkouts (
    id uuid primary key,
    checkout_number text not null unique,
    reservation_id uuid references reservations (id),
    asset_id uuid not null references assets (id),
    user_id uuid not null references users (id),
    quantity integer not null default 1,
    checked_out_at timestamptz not null,
    expected_return_at timestamptz not null,
    returned_at timestamptz,
    condition_before text not null,
    condition_after text,
    accessories text,
    missing_accessories text,
    damage_detected boolean not null default false,
    damage_description text,
    deposit_paid numeric(15, 2),
    penalty_amount numeric(15, 2),
    issued_by uuid not null references users (id),
    received_by uuid references users (id),
    status text not null default 'CHECKED_OUT',
    notes text,
    created_at timestamptz not null,
    updated_at timestamptz
);

create index idx_checkouts_asset on checkouts (asset_id);
create index idx_checkouts_user on checkouts (user_id);

create table maintenance_requests (
    id uuid primary key,
    request_number text not null unique,
    asset_id uuid not null references assets (id),
    issue_type text not null,
    description text not null,
    priority text not null default 'MEDIUM',
    requested_by uuid not null references users (id),
    assigned_to uuid references users (id),
    supplier_id uuid references suppliers (id),
    status text not null default 'OPEN',
    opened_at timestamptz not null,
    due_at timestamptz,
    started_at timestamptz,
    completed_at timestamptz,
    diagnosis text,
    work_performed text,
    parts_used text,
    labour_cost numeric(15, 2),
    parts_cost numeric(15, 2),
    external_cost numeric(15, 2),
    total_cost numeric(15, 2),
    result text,
    new_condition text,
    next_service_date date,
    notes text,
    created_at timestamptz not null,
    updated_at timestamptz
);

create index idx_maintenance_asset on maintenance_requests (asset_id);

create table asset_transfers (
    id uuid primary key,
    transfer_number text not null unique,
    asset_id uuid not null references assets (id),
    quantity integer not null default 1,
    from_location_id uuid not null references locations (id),
    to_location_id uuid not null references locations (id),
    from_custodian_id uuid references users (id),
    to_custodian_id uuid references users (id),
    reason text not null,
    status text not null default 'PENDING_APPROVAL',
    requested_by uuid not null references users (id),
    approved_by uuid references users (id),
    received_by uuid references users (id),
    expected_date timestamptz,
    approved_at timestamptz,
    completed_at timestamptz,
    condition_at_destination text,
    notes text,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table purchases (
    id uuid primary key,
    purchase_order_number text,
    supplier_id uuid not null references suppliers (id),
    faculty_id uuid not null references faculties (id),
    department_id uuid references departments (id),
    purchase_date date not null,
    invoice_number text,
    invoice_date date,
    currency text not null default 'LKR',
    subtotal numeric(15, 2) not null default 0,
    tax numeric(15, 2) not null default 0,
    shipping numeric(15, 2) not null default 0,
    discount numeric(15, 2) not null default 0,
    total_amount numeric(15, 2) not null default 0,
    funding_source text,
    payment_status text not null default 'UNPAID',
    notes text,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table purchase_items (
    id uuid primary key,
    purchase_id uuid not null references purchases (id) on delete cascade,
    item_name text not null,
    category_id uuid references asset_categories (id),
    quantity integer not null default 1,
    unit_price numeric(15, 2) not null default 0,
    tax numeric(15, 2) not null default 0,
    total numeric(15, 2) not null default 0,
    asset_creation_required boolean not null default false,
    assets_generated boolean not null default false,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table payments (
    id uuid primary key,
    transaction_number text not null unique,
    transaction_type text not null,
    payer_type text not null,
    payer_user_id uuid references users (id),
    payer_department_id uuid references departments (id),
    payer_name text,
    reservation_id uuid references reservations (id),
    asset_id uuid references assets (id),
    description text,
    amount numeric(15, 2) not null,
    currency text not null default 'LKR',
    payment_method text not null,
    reference_number text,
    payment_date timestamptz not null,
    status text not null default 'PAID',
    refunded_amount numeric(15, 2) not null default 0,
    original_payment_id uuid references payments (id),
    notes text,
    created_by uuid not null,
    created_at timestamptz not null,
    updated_at timestamptz
);

create table documents (
    id uuid primary key,
    entity_type text not null,
    entity_id uuid not null,
    document_type text not null,
    original_filename text not null,
    storage_key text not null,
    mime_type text not null,
    size_bytes bigint not null,
    uploaded_by uuid not null,
    created_at timestamptz not null,
    updated_at timestamptz
);

create index idx_documents_entity on documents (entity_type, entity_id);

create table notifications (
    id uuid primary key,
    user_id uuid not null references users (id),
    type text not null,
    title text not null,
    message text not null,
    entity_type text,
    entity_id uuid,
    read_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz
);

create index idx_notifications_user on notifications (user_id, read_at);

create table audit_logs (
    id uuid primary key,
    user_id uuid,
    user_email text,
    action text not null,
    module text not null,
    entity_type text,
    entity_id uuid,
    old_values text,
    new_values text,
    ip_address text,
    user_agent text,
    success boolean not null default true,
    created_at timestamptz not null
);

create index idx_audit_created on audit_logs (created_at desc);
create index idx_audit_entity on audit_logs (entity_type, entity_id);
