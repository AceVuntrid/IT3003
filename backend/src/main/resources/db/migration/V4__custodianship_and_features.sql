-- Migration V4: custodianship model, partial-approval and pricing fields,
-- departmental maintenance policy, new roles/permission grants and venue seed
-- data. All statements are written to apply cleanly on a fresh database and on
-- an existing one (guards on unique keys, IF NOT EXISTS on columns).

-- -------------------------------------------------------------- schema changes

-- University-wide assets (e.g. equipment in the ILC/SSC buildings) have no
-- owning faculty.
alter table assets alter column faculty_id drop not null;

-- Preserve the originally requested quantity: "quantity" becomes the
-- approved/effective quantity once an approver reduces it.
alter table reservations add column if not exists requested_quantity integer;
update reservations set requested_quantity = quantity where requested_quantity is null;

-- Reservation pricing decided at approval time.
alter table reservations add column if not exists fee_amount numeric(15, 2);
alter table reservations add column if not exists fee_waived boolean not null default false;

-- Departmental compulsory-maintenance policy (null = disabled).
alter table departments add column if not exists maintenance_interval_days integer;

-- locations.type is plain text (see V1), so the new LECTURE_ROOM / AUDITORIUM
-- values of Enums.LocationType need no schema change.

-- -------------------------------------------------------------- new permission
insert into permissions (id, code, module, action, description, created_at)
select gen_random_uuid(), 'USER_DEACTIVATE', 'USER', 'DEACTIVATE', 'Deactivate and reactivate user accounts', now()
where not exists (select 1 from permissions where code = 'USER_DEACTIVATE');

-- -------------------------------------------------------------------- new roles
insert into roles (id, name, description, system_role, created_at)
select gen_random_uuid(), 'DEPT_ADMIN', 'Approves reservations and manages settings for their department', true, now()
where not exists (select 1 from roles where name = 'DEPT_ADMIN');

insert into roles (id, name, description, system_role, created_at)
select gen_random_uuid(), 'FACULTY_DEAN', 'Approves faculty-owned items and manages faculty-level settings', true, now()
where not exists (select 1 from roles where name = 'FACULTY_DEAN');

insert into roles (id, name, description, system_role, created_at)
select gen_random_uuid(), 'CARETAKER', 'Approves bookings for the unowned buildings and venues in their care', true, now()
where not exists (select 1 from roles where name = 'CARETAKER');

-- Grants shared by all three custodianship roles
insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in (
    'ASSET_VIEW','RESERVATION_VIEW','RESERVATION_APPROVE','CHECKOUT_VIEW',
    'MAINTENANCE_VIEW','MAINTENANCE_CREATE','REPORT_VIEW','CONSUMABLE_VIEW','LOCATION_VIEW')
where r.name in ('DEPT_ADMIN','FACULTY_DEAN','CARETAKER')
on conflict do nothing;

-- DEPT_ADMIN and FACULTY_DEAN additionally manage settings and see payments
insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in ('SETTINGS_MANAGE','PAYMENT_VIEW')
where r.name in ('DEPT_ADMIN','FACULTY_DEAN')
on conflict do nothing;

-- ---------------------------------------- permission grants for existing roles
insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in ('CONSUMABLE_VIEW')
where r.name = 'STUDENT'
on conflict do nothing;

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in ('CHECKOUT_VIEW')
where r.name = 'MAINTENANCE_OFFICER'
on conflict do nothing;

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in ('PURCHASE_VIEW','REPORT_EXPORT')
where r.name = 'STOREKEEPER'
on conflict do nothing;

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in ('CHECKOUT_VIEW','CONSUMABLE_VIEW','AUDIT_VIEW')
where r.name = 'FINANCE_OFFICER'
on conflict do nothing;

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in ('REPORT_EXPORT')
where r.name = 'LAB_MANAGER'
on conflict do nothing;

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in ('LOCATION_MANAGE')
where r.name = 'ASSET_ADMIN'
on conflict do nothing;

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in ('TRANSFER_CREATE','AUDIT_VIEW')
where r.name = 'FACULTY_ADMIN'
on conflict do nothing;

insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in ('USER_DEACTIVATE')
where r.name in ('SUPER_ADMIN','ASSET_ADMIN')
on conflict do nothing;

-- ------------------------------------------------------ organization seed data
-- The Science Faculty's six real departments: Physics and Chemistry already
-- exist (V2); add Statistics, Zoology, Botany and Mathematics.
insert into departments (id, faculty_id, code, name, description, active, created_at)
select 'a0000000-0000-0000-0000-000000000015'::uuid, f.id, 'STAT', 'Statistics Department', null, true, now()
from faculties f where f.code = 'SCI'
  and not exists (select 1 from departments where code = 'STAT');

insert into departments (id, faculty_id, code, name, description, active, created_at)
select 'a0000000-0000-0000-0000-000000000016'::uuid, f.id, 'ZOO', 'Zoology Department', null, true, now()
from faculties f where f.code = 'SCI'
  and not exists (select 1 from departments where code = 'ZOO');

insert into departments (id, faculty_id, code, name, description, active, created_at)
select 'a0000000-0000-0000-0000-000000000017'::uuid, f.id, 'BOT', 'Botany Department', null, true, now()
from faculties f where f.code = 'SCI'
  and not exists (select 1 from departments where code = 'BOT');

insert into departments (id, faculty_id, code, name, description, active, created_at)
select 'a0000000-0000-0000-0000-000000000018'::uuid, f.id, 'MATH', 'Mathematics Department', null, true, now()
from faculties f where f.code = 'SCI'
  and not exists (select 1 from departments where code = 'MATH');

-- ------------------------------------------------------------ venue seed data
-- Unowned buildings (no faculty/department): ownership resolves to the
-- location's responsible user (caretaker), wired by DataInitializer.
insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000011'::uuid,
       (select id from locations where code = 'MAIN'),
       null, null, 'ILC', 'Independent Learning Centre', 'BUILDING', null, true, now()
where not exists (select 1 from locations where code = 'ILC');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000012'::uuid,
       (select id from locations where code = 'MAIN'),
       null, null, 'SSC', 'Student Services Centre', 'BUILDING', null, true, now()
where not exists (select 1 from locations where code = 'SSC');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000013'::uuid,
       (select id from locations where code = 'ILC'),
       null, null, 'ILC-LR1', 'ILC Lecture Room 1', 'LECTURE_ROOM', 120, true, now()
where not exists (select 1 from locations where code = 'ILC-LR1');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000014'::uuid,
       (select id from locations where code = 'ILC'),
       null, null, 'ILC-LR2', 'ILC Lecture Room 2', 'LECTURE_ROOM', 80, true, now()
where not exists (select 1 from locations where code = 'ILC-LR2');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000015'::uuid,
       (select id from locations where code = 'SSC'),
       null, null, 'SSC-LR1', 'SSC Lecture Room 1', 'LECTURE_ROOM', 150, true, now()
where not exists (select 1 from locations where code = 'SSC-LR1');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000016'::uuid,
       (select id from locations where code = 'SSC'),
       null, null, 'SSC-AUD', 'SSC Auditorium', 'AUDITORIUM', 500, true, now()
where not exists (select 1 from locations where code = 'SSC-AUD');

insert into locations (id, parent_id, faculty_id, department_id, code, name, type, capacity, active, created_at)
select 'b0000000-0000-0000-0000-000000000017'::uuid,
       (select id from locations where code = 'ILC'),
       null, null, 'ILC-STORE', 'ILC Store Room', 'STORAGE_AREA', null, true, now()
where not exists (select 1 from locations where code = 'ILC-STORE');

-- ------------------------------------------------------------------ categories
-- FURNITURE already exists in the V2 seed; keep the guard anyway so this also
-- applies on databases where it was removed. ELECTRONICS is new.
insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), null, 'FURNITURE', 'Furniture', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'FURNITURE');

insert into asset_categories (id, parent_id, code, name, description, asset_type, active, created_at)
select gen_random_uuid(), null, 'ELECTRONICS', 'Electronics', null, 'FIXED', true, now()
where not exists (select 1 from asset_categories where code = 'ELECTRONICS');
