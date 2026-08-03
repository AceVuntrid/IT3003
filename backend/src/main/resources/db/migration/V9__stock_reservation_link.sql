-- Migration V9: link stock transactions to the consumable reservation they
-- fulfil (approved consumable reservations are fulfilled by stock ISSUE).
alter table stock_transactions add column if not exists reservation_id uuid references reservations (id);
