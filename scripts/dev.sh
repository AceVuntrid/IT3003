#!/usr/bin/env bash
# Convenience launcher for local development without Docker.
# Requires: JDK 21+, Maven, Node 20+, a PostgreSQL server.
set -euo pipefail
cd "$(dirname "$0")/.."

export DB_URL="${DB_URL:-jdbc:postgresql://localhost:5432/uniassets}"
export DB_USERNAME="${DB_USERNAME:-uniassets}"
export DB_PASSWORD="${DB_PASSWORD:-uniassets}"

(cd backend && mvn -q spring-boot:run) &
BACKEND_PID=$!
trap 'kill $BACKEND_PID 2>/dev/null || true' EXIT

(cd frontend && npm run dev)
