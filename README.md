# University Asset Management and Reservation System

A full-stack web application for registering, locating, reserving, issuing, maintaining,
auditing and reporting on university assets — fixed equipment and consumable stock alike.
The first deployment targets a Science Faculty; the architecture supports rolling out to
every faculty later.

| Layer | Stack |
|---|---|
| Backend | Java 21 · Spring Boot 3.5 · Spring Security (JWT + refresh rotation) · Spring Data JPA · Flyway · springdoc-openapi |
| Frontend | React 19 · TypeScript · Vite · Material UI · TanStack Query · React Hook Form · Recharts |
| Database | PostgreSQL 16 (UUID keys, decimal money types) |
| Infra | Docker Compose (postgres · backend · frontend · nginx · mailhog) |

## What's inside

- **Assets** — full register with categories, locations, custodians, financials, warranty,
  calibration, QR labels, documents, archive (soft delete) and CSV export.
- **Consumables** — quantity-based stock with batches, FEFO issuing (expired batches are
  never issued), receiving, auditable adjustments, low-stock and expiry alerts.
- **Reservations** — availability checking with capacity-aware conflict detection,
  approval workflow, per-user reservation limits, max-duration rules.
- **Check-out / returns** — deposits, condition capture, damage handling, automatic late
  penalties, overdue tracking with daily reminders.
- **Maintenance** — fault reports, preventive maintenance, calibration jobs with costs.
- **Transfers** — location/custodian moves with approval and permanent history.
- **Purchases & suppliers** — purchase records that can generate assets per line item.
- **Payments & charges** — fees, deposits, penalties and refunds (refunds can never
  exceed the remaining refundable amount).
- **RBAC** — 10 seeded roles (Super Admin → Auditor) over 40+ fine-grained permissions,
  enforced on every endpoint and mirrored in the UI navigation.
- **Audit log** — immutable, append-only trail of every important action.
- **Notifications** — in-app plus email (MailHog in development).

## Quick start (Docker)

```bash
cp .env.example .env       # then edit at least JWT_SECRET and ADMIN_PASSWORD
docker compose up --build
```

| URL | What |
|---|---|
| http://localhost | Application |
| http://localhost/swagger-ui.html | Interactive API documentation |
| http://localhost:8025 | MailHog inbox (password-reset and notification emails) |

### Default development accounts

Created on first start with the password from `ADMIN_PASSWORD` (never hard-coded):

| Email | Role |
|---|---|
| `admin@university.local` | SUPER_ADMIN |
| `asset.admin@university.local` | ASSET_ADMIN |
| `lab.manager@university.local` | LAB_MANAGER |
| `storekeeper@university.local` | STOREKEEPER |
| `maintenance@university.local` | MAINTENANCE_OFFICER |
| `finance@university.local` | FINANCE_OFFICER |
| `lecturer@university.local` | LECTURER |
| `student@university.local` | STUDENT |
| `auditor@university.local` | AUDITOR |

Demo users are only created when `SEED_DEMO_DATA=true`.

## Local development (without Docker)

Prerequisites: JDK 21+, Maven 3.9+, Node 20+, PostgreSQL 14+.

```bash
# 1. Database
createdb uniassets   # with a uniassets/uniassets user, or set DB_* env vars

# 2. Backend — Flyway migrates the schema automatically on start
cd backend
DB_URL=jdbc:postgresql://localhost:5432/uniassets mvn spring-boot:run

# 3. Frontend — dev server proxies /api to localhost:8080
cd frontend
npm install
npm run dev            # http://localhost:5173
```

## Running tests

```bash
cd backend && mvn test          # unit tests for password policy, refunds, FEFO expiry
cd frontend && npx tsc -b       # strict type-check
cd frontend && npm run build    # production build
```

## Configuration

All secrets come from environment variables — see `.env.example` for the full list
(`DB_*`, `JWT_*`, `MAIL_*`, `STORAGE_*`, `ADMIN_*`, `APP_FRONTEND_URL`).

## Documentation

- `docs/system-specification.md` — the full functional specification
- `docs/api-design.md` — REST conventions and endpoint map
- `docs/database-design.md` — schema and migration strategy
- `docs/deployment-guide.md` — production deployment notes

## Repository layout

```
├── backend/     Spring Boot modular monolith (one package per business module)
├── frontend/    React SPA (feature-organised pages, shared components)
├── nginx/       Reverse-proxy config used by docker compose
├── docs/        Specification and design documents
└── scripts/     Helper scripts
```
