-- 4-digit collection code proving the borrower is present at handover.
-- Generated at final approval of an asset reservation; venue bookings have none.
alter table reservations add column collection_code varchar(4);
