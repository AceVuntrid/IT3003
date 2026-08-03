# API Design

Base URL: `/api/v1` — JSON only. Interactive documentation: `/swagger-ui.html`.

## Conventions

**Envelope.** Every response uses one envelope:

```json
{ "success": true,  "message": "Asset created successfully", "data": { }, "timestamp": "…" }
{ "success": false, "message": "Validation failed", "errors": { "assetCode": "Asset code already exists" }, "timestamp": "…" }
```

**Pagination.** List endpoints accept `page`, `size`, `sort` (e.g. `sort=name,asc`) and
return `{ content, page, size, totalElements, totalPages }` inside `data`.

**Authentication.** `Authorization: Bearer <accessToken>`. Access tokens are short-lived
JWTs; refresh tokens are opaque, stored hashed, and rotated on every refresh. On 401 the
client calls `POST /auth/refresh` once and retries.

**Authorization.** Every protected endpoint checks a permission code
(e.g. `ASSET_CREATE`) carried by the user's roles. The same codes drive UI visibility.

**Dates.** ISO 8601. Instants are UTC; date-only fields are plain `yyyy-MM-dd`.

## Endpoint map

| Area | Endpoints |
|---|---|
| Auth | `POST /auth/login` · `/refresh` · `/logout` · `/forgot-password` · `/reset-password` · `GET /auth/me` · `PUT /auth/change-password` |
| Users | `GET/POST /users` · `GET/PUT /users/{id}` · `PATCH /users/{id}/status` · `POST /users/{id}/reset-password` |
| Roles | `GET/POST /roles` · `PUT /roles/{id}` · `GET /roles/permissions` |
| Org | `GET/POST/PUT /faculties` · `/departments` |
| Locations | `GET /locations` · `GET /locations/tree` · `GET/POST/PUT /locations/{id}` · `PATCH /locations/{id}/status` |
| Categories | `GET/POST/PUT /categories` |
| Assets | `GET/POST /assets` · `GET/PUT /assets/{id}` · `POST /assets/{id}/archive|restore|status` · `GET /assets/{id}/qr-code` |
| Documents | `GET/POST /documents` · `GET /documents/{id}/download` · `DELETE /documents/{id}` |
| Consumables | `GET/POST /consumables` · `GET/PUT /consumables/{id}` · `POST /consumables/{id}/receive|issue|adjust` · `GET /consumables/{id}/batches|transactions` · `GET /consumables/low-stock|expiring` |
| Reservations | `GET/POST /reservations` · `GET /reservations/{id}` · `POST /reservations/{id}/approve|reject|cancel` · `GET /reservations/calendar|availability` |
| Checkouts | `GET/POST /checkouts` · `GET /checkouts/{id}` · `POST /checkouts/{id}/return|extend` · `GET /checkouts/overdue` |
| Maintenance | `GET/POST /maintenance-requests` · `GET/PUT /maintenance-requests/{id}` · `POST …/{id}/assign|start|complete|cancel` |
| Transfers | `GET/POST /transfers` · `GET /transfers/{id}` · `POST /transfers/{id}/approve|reject|complete` |
| Suppliers | `GET/POST /suppliers` · `GET/PUT /suppliers/{id}` |
| Purchases | `GET/POST /purchases` · `GET /purchases/{id}` · `POST /purchases/{id}/generate-assets` |
| Payments | `GET/POST /payments` · `GET /payments/{id}` · `POST /payments/{id}/refund` · `GET /payments/summary` |
| Reports | `GET /reports/assets|consumables|expiry|checkouts|maintenance|payments` — add `?format=csv` for CSV download |
| Dashboard | `GET /dashboard/summary` · `GET /dashboard/charts` |
| Notifications | `GET /notifications` · `GET /notifications/unread-count` · `POST /notifications/{id}/read` · `POST /notifications/read-all` · `DELETE /notifications/{id}` |
| Audit | `GET /audit-logs` (read-only; records are immutable) |

## Key business rules enforced server-side

- Asset codes and serial numbers are unique; archived assets cannot be edited or reserved.
- Reservation capacity is checked against overlapping active reservations; assets under
  maintenance, damaged, lost or archived are blocked; per-asset max duration and per-user
  reservation limits apply.
- Deposits must be paid before check-out when the asset requires one.
- Stock can never go negative; expired batches are never issued; issuing follows FEFO.
- Damage on return requires a description; late returns accrue a configurable daily penalty.
- Refunds cannot exceed the remaining refundable amount of the original payment.
- Every sensitive change writes an immutable audit record.
