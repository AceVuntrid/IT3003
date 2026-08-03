-- Remove the Purchases & Suppliers module.
-- Drops the purchase tables, the supplier links on remaining tables, the
-- suppliers table itself, and the module's permissions.

-- Purchase records (purchase_items references purchases)
drop table if exists purchase_items;
drop table if exists purchases;

-- Supplier links on remaining tables (must go before the suppliers table)
alter table assets drop column if exists supplier_id;
alter table maintenance_requests drop column if exists supplier_id;
alter table consumable_batches drop column if exists supplier_id;

-- Suppliers
drop table if exists suppliers;

-- Permissions: role_permissions has no ON DELETE CASCADE, so remove the
-- role links first, then the permission rows themselves.
delete from role_permissions where permission_id in (
    select id from permissions
    where code in ('PURCHASE_VIEW', 'PURCHASE_CREATE', 'SUPPLIER_VIEW', 'SUPPLIER_MANAGE')
);

delete from permissions
where code in ('PURCHASE_VIEW', 'PURCHASE_CREATE', 'SUPPLIER_VIEW', 'SUPPLIER_MANAGE');
