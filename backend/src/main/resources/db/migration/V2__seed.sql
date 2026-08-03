-- Seed data: permissions, roles, organization structure, categories, locations,
-- suppliers and example inventory. User accounts are created by the application
-- on first start so passwords are never stored in source control.

-- ---------------------------------------------------------------- permissions
insert into permissions (id, code, module, action, description, created_at) values
    (gen_random_uuid(), 'ASSET_VIEW', 'ASSET', 'VIEW', 'View assets', now()),
    (gen_random_uuid(), 'ASSET_CREATE', 'ASSET', 'CREATE', 'Add assets', now()),
    (gen_random_uuid(), 'ASSET_EDIT', 'ASSET', 'EDIT', 'Edit assets', now()),
    (gen_random_uuid(), 'ASSET_ARCHIVE', 'ASSET', 'ARCHIVE', 'Archive and restore assets', now()),
    (gen_random_uuid(), 'ASSET_EXPORT', 'ASSET', 'EXPORT', 'Export asset data', now()),
    (gen_random_uuid(), 'CONSUMABLE_VIEW', 'CONSUMABLE', 'VIEW', 'View consumables', now()),
    (gen_random_uuid(), 'CONSUMABLE_CREATE', 'CONSUMABLE', 'CREATE', 'Add consumables', now()),
    (gen_random_uuid(), 'CONSUMABLE_EDIT', 'CONSUMABLE', 'EDIT', 'Edit consumables', now()),
    (gen_random_uuid(), 'CONSUMABLE_RECEIVE', 'CONSUMABLE', 'RECEIVE', 'Receive stock', now()),
    (gen_random_uuid(), 'CONSUMABLE_ISSUE', 'CONSUMABLE', 'ISSUE', 'Issue stock', now()),
    (gen_random_uuid(), 'CONSUMABLE_ADJUST', 'CONSUMABLE', 'ADJUST', 'Adjust stock', now()),
    (gen_random_uuid(), 'RESERVATION_VIEW', 'RESERVATION', 'VIEW', 'View reservations', now()),
    (gen_random_uuid(), 'RESERVATION_CREATE', 'RESERVATION', 'CREATE', 'Create reservations', now()),
    (gen_random_uuid(), 'RESERVATION_APPROVE', 'RESERVATION', 'APPROVE', 'Approve reservations', now()),
    (gen_random_uuid(), 'RESERVATION_MANAGE', 'RESERVATION', 'MANAGE', 'Manage all reservations', now()),
    (gen_random_uuid(), 'CHECKOUT_VIEW', 'CHECKOUT', 'VIEW', 'View check-outs', now()),
    (gen_random_uuid(), 'CHECKOUT_CREATE', 'CHECKOUT', 'CREATE', 'Check out assets', now()),
    (gen_random_uuid(), 'CHECKOUT_MANAGE', 'CHECKOUT', 'MANAGE', 'Process returns and extensions', now()),
    (gen_random_uuid(), 'MAINTENANCE_VIEW', 'MAINTENANCE', 'VIEW', 'View maintenance requests', now()),
    (gen_random_uuid(), 'MAINTENANCE_CREATE', 'MAINTENANCE', 'CREATE', 'Create maintenance requests', now()),
    (gen_random_uuid(), 'MAINTENANCE_MANAGE', 'MAINTENANCE', 'MANAGE', 'Manage maintenance jobs', now()),
    (gen_random_uuid(), 'TRANSFER_VIEW', 'TRANSFER', 'VIEW', 'View transfers', now()),
    (gen_random_uuid(), 'TRANSFER_CREATE', 'TRANSFER', 'CREATE', 'Request transfers', now()),
    (gen_random_uuid(), 'TRANSFER_APPROVE', 'TRANSFER', 'APPROVE', 'Approve and complete transfers', now()),
    (gen_random_uuid(), 'LOCATION_VIEW', 'LOCATION', 'VIEW', 'View locations', now()),
    (gen_random_uuid(), 'LOCATION_MANAGE', 'LOCATION', 'MANAGE', 'Manage locations', now()),
    (gen_random_uuid(), 'ORG_MANAGE', 'ORGANIZATION', 'MANAGE', 'Manage faculties and departments', now()),
    (gen_random_uuid(), 'CATEGORY_MANAGE', 'CATEGORY', 'MANAGE', 'Manage asset categories', now()),
    (gen_random_uuid(), 'SUPPLIER_VIEW', 'SUPPLIER', 'VIEW', 'View suppliers', now()),
    (gen_random_uuid(), 'SUPPLIER_MANAGE', 'SUPPLIER', 'MANAGE', 'Manage suppliers', now()),
    (gen_random_uuid(), 'PURCHASE_VIEW', 'PURCHASE', 'VIEW', 'View purchases', now()),
    (gen_random_uuid(), 'PURCHASE_CREATE', 'PURCHASE', 'CREATE', 'Record purchases', now()),
    (gen_random_uuid(), 'PAYMENT_VIEW', 'PAYMENT', 'VIEW', 'View payments and charges', now()),
    (gen_random_uuid(), 'PAYMENT_CREATE', 'PAYMENT', 'CREATE', 'Record payments and charges', now()),
    (gen_random_uuid(), 'PAYMENT_REFUND', 'PAYMENT', 'REFUND', 'Process refunds', now()),
    (gen_random_uuid(), 'USER_VIEW', 'USER', 'VIEW', 'View users', now()),
    (gen_random_uuid(), 'USER_CREATE', 'USER', 'CREATE', 'Create users', now()),
    (gen_random_uuid(), 'USER_EDIT', 'USER', 'EDIT', 'Edit users', now()),
    (gen_random_uuid(), 'ROLE_MANAGE', 'ROLE', 'MANAGE', 'Manage roles and permissions', now()),
    (gen_random_uuid(), 'REPORT_VIEW', 'REPORT', 'VIEW', 'View reports', now()),
    (gen_random_uuid(), 'REPORT_EXPORT', 'REPORT', 'EXPORT', 'Export reports', now()),
    (gen_random_uuid(), 'AUDIT_VIEW', 'AUDIT', 'VIEW', 'View the audit log', now()),
    (gen_random_uuid(), 'SETTINGS_MANAGE', 'SETTINGS', 'MANAGE', 'Manage system settings', now());

-- ---------------------------------------------------------------------- roles
insert into roles (id, name, description, system_role, created_at) values
    (gen_random_uuid(), 'SUPER_ADMIN', 'Full access to every module and setting', true, now()),
    (gen_random_uuid(), 'ASSET_ADMIN', 'Manages the asset register, categories, suppliers and purchases', true, now()),
    (gen_random_uuid(), 'FACULTY_ADMIN', 'Manages assets, reservations and locations for a faculty', true, now()),
    (gen_random_uuid(), 'LAB_MANAGER', 'Manages laboratory equipment, reservations, issue and returns', true, now()),
    (gen_random_uuid(), 'STOREKEEPER', 'Receives, issues and adjusts consumable inventory', true, now()),
    (gen_random_uuid(), 'MAINTENANCE_OFFICER', 'Works on assigned maintenance and calibration jobs', true, now()),
    (gen_random_uuid(), 'FINANCE_OFFICER', 'Records charges, deposits, refunds and reviews financial reports', true, now()),
    (gen_random_uuid(), 'LECTURER', 'Searches assets, reserves equipment and reports faults', true, now()),
    (gen_random_uuid(), 'STUDENT', 'Reserves permitted assets and views own items', true, now()),
    (gen_random_uuid(), 'AUDITOR', 'Read-only access to records, reports and audit history', true, now());

-- SUPER_ADMIN: everything
insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r, permissions p where r.name = 'SUPER_ADMIN';

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in (
    'ASSET_VIEW','ASSET_CREATE','ASSET_EDIT','ASSET_ARCHIVE','ASSET_EXPORT',
    'CATEGORY_MANAGE','SUPPLIER_VIEW','SUPPLIER_MANAGE','PURCHASE_VIEW','PURCHASE_CREATE',
    'LOCATION_VIEW','TRANSFER_VIEW','TRANSFER_CREATE','TRANSFER_APPROVE',
    'CONSUMABLE_VIEW','RESERVATION_VIEW','CHECKOUT_VIEW','MAINTENANCE_VIEW','MAINTENANCE_CREATE',
    'REPORT_VIEW','REPORT_EXPORT')
where r.name = 'ASSET_ADMIN';

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in (
    'ASSET_VIEW','ASSET_EDIT','RESERVATION_VIEW','RESERVATION_APPROVE','RESERVATION_MANAGE',
    'LOCATION_VIEW','LOCATION_MANAGE','REPORT_VIEW','REPORT_EXPORT','USER_VIEW',
    'TRANSFER_VIEW','TRANSFER_APPROVE','CONSUMABLE_VIEW','CHECKOUT_VIEW','MAINTENANCE_VIEW')
where r.name = 'FACULTY_ADMIN';

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in (
    'ASSET_VIEW','ASSET_EDIT','RESERVATION_VIEW','RESERVATION_CREATE','RESERVATION_APPROVE',
    'CHECKOUT_VIEW','CHECKOUT_CREATE','CHECKOUT_MANAGE',
    'CONSUMABLE_VIEW','CONSUMABLE_ISSUE','CONSUMABLE_RECEIVE',
    'MAINTENANCE_VIEW','MAINTENANCE_CREATE','REPORT_VIEW','TRANSFER_VIEW','TRANSFER_CREATE',
    'LOCATION_VIEW')
where r.name = 'LAB_MANAGER';

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in (
    'CONSUMABLE_VIEW','CONSUMABLE_CREATE','CONSUMABLE_EDIT','CONSUMABLE_RECEIVE',
    'CONSUMABLE_ISSUE','CONSUMABLE_ADJUST','ASSET_VIEW','REPORT_VIEW','LOCATION_VIEW',
    'SUPPLIER_VIEW')
where r.name = 'STOREKEEPER';

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in (
    'MAINTENANCE_VIEW','MAINTENANCE_CREATE','MAINTENANCE_MANAGE','ASSET_VIEW','LOCATION_VIEW')
where r.name = 'MAINTENANCE_OFFICER';

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in (
    'PAYMENT_VIEW','PAYMENT_CREATE','PAYMENT_REFUND','PURCHASE_VIEW','SUPPLIER_VIEW',
    'REPORT_VIEW','REPORT_EXPORT','ASSET_VIEW','RESERVATION_VIEW')
where r.name = 'FINANCE_OFFICER';

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in (
    'ASSET_VIEW','RESERVATION_VIEW','RESERVATION_CREATE','CONSUMABLE_VIEW',
    'MAINTENANCE_CREATE','MAINTENANCE_VIEW','CHECKOUT_VIEW','LOCATION_VIEW')
where r.name = 'LECTURER';

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in (
    'ASSET_VIEW','RESERVATION_VIEW','RESERVATION_CREATE','MAINTENANCE_CREATE','LOCATION_VIEW')
where r.name = 'STUDENT';

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in (
    'ASSET_VIEW','CONSUMABLE_VIEW','RESERVATION_VIEW','CHECKOUT_VIEW','MAINTENANCE_VIEW',
    'TRANSFER_VIEW','LOCATION_VIEW','SUPPLIER_VIEW','PURCHASE_VIEW','PAYMENT_VIEW',
    'USER_VIEW','REPORT_VIEW','AUDIT_VIEW')
where r.name = 'AUDITOR';

-- ------------------------------------------------------- organization + sites
insert into faculties (id, code, name, description, active, created_at) values
    ('a0000000-0000-0000-0000-000000000001', 'SCI', 'Science Faculty',
     'Initial deployment scope: science laboratories and departments', true, now());

insert into departments (id, faculty_id, code, name, description, active, created_at) values
    ('a0000000-0000-0000-0000-000000000011', 'a0000000-0000-0000-0000-000000000001', 'CHEM', 'Chemistry Department', null, true, now()),
    ('a0000000-0000-0000-0000-000000000012', 'a0000000-0000-0000-0000-000000000001', 'PHYS', 'Physics Department', null, true, now()),
    ('a0000000-0000-0000-0000-000000000013', 'a0000000-0000-0000-0000-000000000001', 'BIO', 'Biology Department', null, true, now()),
    ('a0000000-0000-0000-0000-000000000014', 'a0000000-0000-0000-0000-000000000001', 'CS', 'Computer Science Department', null, true, now());

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at) values
    ('b0000000-0000-0000-0000-000000000001', null, null, null, 'MAIN', 'Main Campus', 'CAMPUS', null, true, now()),
    ('b0000000-0000-0000-0000-000000000002', 'b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', null, 'SCI-B1', 'Science Building 1', 'BUILDING', null, true, now()),
    ('b0000000-0000-0000-0000-000000000003', 'b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', 'CHEM-LAB1', 'Chemistry Laboratory 1', 'LABORATORY', 32, true, now()),
    ('b0000000-0000-0000-0000-000000000004', 'b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000012', 'PHYS-LAB1', 'Physics Laboratory 1', 'LABORATORY', 28, true, now()),
    ('b0000000-0000-0000-0000-000000000005', 'b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000013', 'BIO-LAB1', 'Biology Laboratory 1', 'LABORATORY', 30, true, now()),
    ('b0000000-0000-0000-0000-000000000006', 'b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', 'CHEM-STORE', 'Chemistry Store Room', 'STORAGE_AREA', null, true, now()),
    ('b0000000-0000-0000-0000-000000000007', 'b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', null, 'LECT-101', 'Lecture Room 101', 'ROOM', 120, true, now()),
    ('b0000000-0000-0000-0000-000000000008', 'b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000014', 'CS-LAB1', 'Computer Science Laboratory 1', 'LABORATORY', 40, true, now());

-- ----------------------------------------------------------------- categories
insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at) values
    ('c0000000-0000-0000-0000-000000000001', null, 'LAB-EQUIP', 'Laboratory Equipment', null, 'FIXED', true, now()),
    ('c0000000-0000-0000-0000-000000000002', null, 'COMPUTING', 'Computers and IT', null, 'FIXED', true, now()),
    ('c0000000-0000-0000-0000-000000000003', null, 'OPTICS', 'Microscopes and Optics', null, 'FIXED', true, now()),
    ('c0000000-0000-0000-0000-000000000004', null, 'AV', 'Projectors and AV', null, 'FIXED', true, now()),
    ('c0000000-0000-0000-0000-000000000005', null, 'FURNITURE', 'Furniture', null, 'FIXED', true, now()),
    ('c0000000-0000-0000-0000-000000000006', null, 'VEHICLES', 'Vehicles', null, 'VEHICLE', true, now()),
    ('c0000000-0000-0000-0000-000000000007', null, 'MEASURE', 'Measuring Instruments', null, 'FIXED', true, now()),
    ('c0000000-0000-0000-0000-000000000011', null, 'CHEMICALS', 'Chemicals', null, 'CONSUMABLE', true, now()),
    ('c0000000-0000-0000-0000-000000000012', null, 'GLASSWARE', 'Glassware', null, 'CONSUMABLE', true, now()),
    ('c0000000-0000-0000-0000-000000000013', null, 'LAB-SUPPLY', 'Lab Supplies', null, 'CONSUMABLE', true, now()),
    ('c0000000-0000-0000-0000-000000000014', null, 'SAFETY', 'Disposable Safety Equipment', null, 'CONSUMABLE', true, now()),
    ('c0000000-0000-0000-0000-000000000015', null, 'STATIONERY', 'Stationery and Cartridges', null, 'CONSUMABLE', true, now());

-- ------------------------------------------------------------------ suppliers
insert into suppliers (id, supplier_code, name, contact_person, email, phone, city, country, payment_terms, rating, active, created_at) values
    ('d0000000-0000-0000-0000-000000000001', 'SUP-0001', 'LabTech Scientific', 'Maria Santos', 'sales@labtech.example', '+61 2 9000 1000', 'Sydney', 'Australia', 'Net 30', 5, true, now()),
    ('d0000000-0000-0000-0000-000000000002', 'SUP-0002', 'ChemSupply Australia', 'John Wu', 'orders@chemsupply.example', '+61 3 8000 2000', 'Melbourne', 'Australia', 'Net 14', 4, true, now());

-- ----------------------------------------------------------------- demo assets
insert into assets (id, asset_code, name, description, asset_type, category_id, brand, model, serial_number, qr_code,
                    faculty_id, department_id, location_id, purchase_date, purchase_price, currency, current_book_value,
                    quantity, available_quantity, initial_condition, condition, status, reservable, approval_required,
                    deposit_required, deposit_amount, max_reservation_hours, calibration_required,
                    next_service_date, created_at) values
    ('e0000000-0000-0000-0000-000000000001', 'AST-00001', 'Compound Microscope', 'Binocular compound microscope, 1000x', 'FIXED',
     'c0000000-0000-0000-0000-000000000003', 'Olympus', 'CX23', 'OLY-CX23-0451', 'ASSET:AST-00001',
     'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000013', 'b0000000-0000-0000-0000-000000000005',
     '2024-02-15', 2400.00, 'LKR', 1900.00, 6, 6, 'NEW', 'GOOD', 'AVAILABLE', true, true, false, null, 72, false, '2026-09-30', now()),
    ('e0000000-0000-0000-0000-000000000002', 'AST-00002', 'UV-Vis Spectrophotometer', 'Double-beam UV-Vis spectrophotometer', 'FIXED',
     'c0000000-0000-0000-0000-000000000007', 'Shimadzu', 'UV-1900i', 'SHM-1900-2201', 'ASSET:AST-00002',
     'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000003',
     '2023-08-01', 18500.00, 'LKR', 14000.00, 1, 1, 'NEW', 'EXCELLENT', 'AVAILABLE', true, true, true, 500.00, 48, true, '2026-08-15', now()),
    ('e0000000-0000-0000-0000-000000000003', 'AST-00003', 'Data Projector', '4K laser projector for lecture rooms', 'FIXED',
     'c0000000-0000-0000-0000-000000000004', 'Epson', 'EB-800F', 'EPS-800F-1104', 'ASSET:AST-00003',
     'a0000000-0000-0000-0000-000000000001', null, 'b0000000-0000-0000-0000-000000000007',
     '2024-11-20', 3200.00, 'LKR', 2900.00, 2, 2, 'NEW', 'GOOD', 'AVAILABLE', true, false, false, null, 24, false, null, now()),
    ('e0000000-0000-0000-0000-000000000004', 'AST-00004', 'Analytical Balance', '0.1 mg readability analytical balance', 'FIXED',
     'c0000000-0000-0000-0000-000000000007', 'Mettler Toledo', 'ME204', 'MT-ME204-3320', 'ASSET:AST-00004',
     'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000003',
     '2022-05-10', 5200.00, 'LKR', 3100.00, 2, 2, 'NEW', 'GOOD', 'AVAILABLE', true, true, false, null, 24, true, '2026-08-05', now()),
    ('e0000000-0000-0000-0000-000000000005', 'AST-00005', 'Laptop Trolley (24 units)', 'Charging trolley with 24 student laptops', 'FIXED',
     'c0000000-0000-0000-0000-000000000002', 'Dell', 'Latitude 3450', null, 'ASSET:AST-00005',
     'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000014', 'b0000000-0000-0000-0000-000000000008',
     '2025-01-30', 38000.00, 'LKR', 34000.00, 24, 24, 'NEW', 'GOOD', 'AVAILABLE', true, false, false, null, 8, false, null, now()),
    ('e0000000-0000-0000-0000-000000000006', 'AST-00006', 'Oscilloscope', '200 MHz 4-channel digital oscilloscope', 'FIXED',
     'c0000000-0000-0000-0000-000000000007', 'Keysight', 'DSOX1204G', 'KEY-1204-8874', 'ASSET:AST-00006',
     'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000012', 'b0000000-0000-0000-0000-000000000004',
     '2023-03-18', 4100.00, 'LKR', 2800.00, 4, 4, 'NEW', 'FAIR', 'AVAILABLE', true, true, false, null, 48, true, '2026-07-30', now());

-- ------------------------------------------------------------ demo consumables
insert into consumable_items (id, item_code, name, description, category_id, faculty_id, department_id, location_id,
                              unit_of_measure, current_quantity, reserved_quantity, reorder_level, unit_cost,
                              hazardous, chemical_classification, active, created_at) values
    ('f0000000-0000-0000-0000-000000000001', 'CON-00001', 'Ethanol 96%', 'Laboratory grade ethanol', 'c0000000-0000-0000-0000-000000000011',
     'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000006',
     'L', 48, 0, 20, 6.50, true, 'Flammable liquid, Class 3', true, now()),
    ('f0000000-0000-0000-0000-000000000002', 'CON-00002', 'Nitrile Gloves (M)', 'Powder-free nitrile examination gloves', 'c0000000-0000-0000-0000-000000000014',
     'a0000000-0000-0000-0000-000000000001', null, 'b0000000-0000-0000-0000-000000000006',
     'box', 14, 0, 20, 12.00, false, null, true, now()),
    ('f0000000-0000-0000-0000-000000000003', 'CON-00003', 'Beaker 250 mL', 'Borosilicate glass beaker', 'c0000000-0000-0000-0000-000000000012',
     'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000006',
     'pcs', 120, 0, 40, 4.20, false, null, true, now()),
    ('f0000000-0000-0000-0000-000000000004', 'CON-00004', 'Hydrochloric Acid 37%', 'Concentrated HCl', 'c0000000-0000-0000-0000-000000000011',
     'a0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000011', 'b0000000-0000-0000-0000-000000000006',
     'L', 9, 0, 10, 14.80, true, 'Corrosive, Class 8', true, now());

insert into consumable_batches (id, consumable_item_id, batch_number, quantity_received, quantity_remaining,
                                manufacture_date, expiry_date, unit_cost, supplier_id, received_date, created_at) values
    (gen_random_uuid(), 'f0000000-0000-0000-0000-000000000001', 'ETH-2025-11', 30, 28, '2025-11-01', '2027-11-01', 6.50, 'd0000000-0000-0000-0000-000000000002', '2025-12-01', now()),
    (gen_random_uuid(), 'f0000000-0000-0000-0000-000000000001', 'ETH-2026-03', 20, 20, '2026-03-05', '2028-03-05', 6.80, 'd0000000-0000-0000-0000-000000000002', '2026-04-02', now()),
    (gen_random_uuid(), 'f0000000-0000-0000-0000-000000000002', 'GLV-2025-08', 40, 14, '2025-08-15', '2028-08-15', 12.00, 'd0000000-0000-0000-0000-000000000001', '2025-09-10', now()),
    (gen_random_uuid(), 'f0000000-0000-0000-0000-000000000003', 'BKR-2024-01', 150, 120, null, null, 4.20, 'd0000000-0000-0000-0000-000000000001', '2024-02-01', now()),
    (gen_random_uuid(), 'f0000000-0000-0000-0000-000000000004', 'HCL-2025-06', 12, 9, '2025-06-20', '2026-09-15', 14.80, 'd0000000-0000-0000-0000-000000000002', '2025-07-08', now());
