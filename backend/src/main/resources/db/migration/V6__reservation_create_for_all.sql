-- Every role except AUDITOR (read-only by design) can create reservations
-- (book equipment and venues). Maintenance officer and storekeeper also gain
-- RESERVATION_VIEW so they can see their own bookings.

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
cross join permissions p
where p.code = 'RESERVATION_CREATE'
  and r.name in ('ASSET_ADMIN', 'CARETAKER', 'DEPT_ADMIN', 'FACULTY_ADMIN',
                 'FACULTY_DEAN', 'FINANCE_OFFICER', 'MAINTENANCE_OFFICER', 'STOREKEEPER')
on conflict do nothing;

insert into role_permissions (role_id, permission_id)
select r.id, p.id
from roles r
cross join permissions p
where p.code = 'RESERVATION_VIEW'
  and r.name in ('MAINTENANCE_OFFICER', 'STOREKEEPER')
on conflict do nothing;
