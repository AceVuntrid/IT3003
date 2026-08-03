-- Migration V8: unified reservations (consumable reservations) and the price
-- list that replaces approver-set fees.

-- Reservations may now target a consumable item (exactly one of asset_id /
-- location_id / consumable_item_id is set; enforced in the service layer like
-- the existing asset/location pair).
alter table reservations add column if not exists consumable_item_id uuid references consumable_items (id);
create index if not exists idx_reservations_consumable on reservations (consumable_item_id);

-- Price list: null (or zero) means free. Fees are auto-computed at final
-- approval from these columns; approvers no longer enter amounts.
alter table assets add column if not exists reservation_fee numeric(15, 2);
alter table locations add column if not exists booking_fee numeric(15, 2);
alter table consumable_items add column if not exists unit_fee numeric(15, 2);

-- Caretakers price the items whose location chain they are responsible for,
-- via the pricing endpoints guarded by SETTINGS_MANAGE (they currently lack it).
insert into role_permissions (role_id, permission_id)
select r.id, p.id from roles r join permissions p on p.code in ('SETTINGS_MANAGE')
where r.name = 'CARETAKER'
on conflict do nothing;
