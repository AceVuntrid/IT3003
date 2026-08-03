# Database Design

PostgreSQL 16. Schema is owned by Flyway migrations in
`backend/src/main/resources/db/migration` — Hibernate runs in `validate` mode and never
alters the schema.

## Conventions

- **UUID primary keys** on every table (`gen_random_uuid()` for seeds, application-side
  generation at runtime).
- **`created_at` / `updated_at`** timestamps on every business table.
- **Money is `numeric(15,2)`**, stock quantities `numeric(15,3)` — never floating point.
- **Soft deletion**: assets use `archived_at`; nothing with history is ever hard-deleted.
- Enumerations are stored as text (`status`, `condition`, …) and constrained by the
  application's enum types.

## Tables

| Group | Tables |
|---|---|
| Identity | `users`, `roles`, `permissions`, `user_roles`, `role_permissions`, `refresh_tokens`, `password_reset_tokens` |
| Organization | `faculties`, `departments`, `locations` (self-referencing hierarchy) |
| Inventory | `asset_categories`, `assets`, `consumable_items`, `consumable_batches`, `stock_transactions`, `documents` |
| Operations | `reservations`, `checkouts`, `maintenance_requests`, `asset_transfers` |
| Commerce | `suppliers`, `purchases`, `purchase_items`, `payments` |
| System | `notifications`, `audit_logs` (append-only) |

## Notable relationships

- `locations.parent_id` builds the campus → building → room → laboratory → storage tree.
- `consumable_batches` carry expiry dates; `stock_transactions` reference the batch each
  movement touched, so FEFO issuing is fully reconstructable.
- `reservations` hold capacity against `assets` for their `[start_at, end_at)` window
  while in an active status (submitted/pending/approved/checked-out/overdue).
- `payments.original_payment_id` links refunds to the payment they reverse;
  `refunded_amount` on the original enforces the refund ceiling.
- `audit_logs` has no update path in the application and no FK cascade that could erase it.

## Migrations

| Version | Contents |
|---|---|
| `V1__init.sql` | Full schema, indexes on hot paths (asset filters, reservation windows, audit timeline) |
| `V2__seed.sql` | Permissions, 10 system roles with their grants, Science Faculty org structure, locations, categories, suppliers, sample assets/consumables/batches |

User accounts are **not** seeded by SQL — a `DataInitializer` creates them on first start
with the password taken from the `ADMIN_PASSWORD` environment variable, so no credential
ever lives in source control.
