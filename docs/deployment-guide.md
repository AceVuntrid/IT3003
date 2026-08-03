# Deployment Guide

## Local / staging with Docker Compose

```bash
cp .env.example .env
# edit .env: set JWT_SECRET (48+ random chars) and ADMIN_PASSWORD
docker compose up --build -d
docker compose logs -f backend      # watch Flyway migrate and the app start
```

Services: `postgres`, `backend`, `frontend`, `nginx` (entry point on port 80),
`mailhog` (SMTP capture UI on 8025).

## Production checklist

1. **Secrets** — generate a unique `JWT_SECRET` and strong `DB_PASSWORD` /
   `ADMIN_PASSWORD`; supply them via your orchestrator's secret store, not files in git.
2. **TLS** — terminate HTTPS at the nginx layer (or a cloud load balancer) and forward
   `X-Forwarded-Proto`. Set `APP_FRONTEND_URL` to the public origin so CORS and
   password-reset links are correct.
3. **Database** — use a managed PostgreSQL with automated backups. Flyway migrates on
   startup; never edit an applied migration, always add a new `V<n>__*.sql`.
4. **Mail** — point `MAIL_HOST/PORT/USERNAME/PASSWORD` at a real SMTP relay and remove
   the mailhog service.
5. **File storage** — the default is a Docker volume (`STORAGE_TYPE=local`). For
   multi-instance deployments mount shared storage or front it with an S3-compatible
   store and set `STORAGE_PATH` accordingly.
6. **Seed data** — set `SEED_DEMO_DATA=false` so only the administrator account is
   created; build your own org structure through the UI.
7. **Sign-in hygiene** — the seeded admin must change its password immediately
   (`must_change_password` is enforced for admin-created users).
8. **Monitoring** — `/actuator/health` is unauthenticated for load-balancer checks;
   scrape logs from the `backend` container (JSON-ready via Spring Boot logging config).

## Upgrades

```bash
git pull
docker compose build
docker compose up -d       # Flyway applies any new migrations on boot
```

Rollback strategy: database migrations are forward-only; restore from backup to roll
back schema changes. Application containers can be rolled back freely as long as the
schema version is compatible.
