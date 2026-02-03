#!/usr/bin/env bash
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
DB_NAME="${DB_NAME:-vw}"
DB_USER="${DB_USER:-vw}"
DB_PASS="${DB_PASS:-vw}"
DB_PORT="${DB_PORT:-5432}"

COMPOSE_FILE="${COMPOSE_FILE:-infra/docker-compose.yml}"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required."
  exit 1
fi

echo "==> Start postgres (docker compose)"
docker compose -f "$COMPOSE_FILE" up -d postgres >/dev/null
DB_CONTAINER="$(docker compose -f "$COMPOSE_FILE" ps -q postgres)"
if [[ -z "$DB_CONTAINER" ]]; then
  echo "Postgres container not found."
  exit 1
fi

echo "==> Wait for postgres"
for i in {1..30}; do
  if docker exec "$DB_CONTAINER" pg_isready -U "$DB_USER" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

echo "==> Run backend to apply migrations"
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:${DB_PORT}/${DB_NAME}"
export SPRING_DATASOURCE_USERNAME="$DB_USER"
export SPRING_DATASOURCE_PASSWORD="$DB_PASS"
timeout 60s ./backend/gradlew -p backend bootRun --args="--spring.main.web-application-type=none --spring.main.banner-mode=off" || true

echo "==> Verify migrations count"
MIG_FILES_COUNT="$(ls backend/src/main/resources/db/migration | wc -l | tr -d ' ')"
APPLIED_COUNT="$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -tAc "select count(*) from flyway_schema_history where success = true;")"
echo "Migrations files: $MIG_FILES_COUNT, applied: $APPLIED_COUNT"

echo "==> Verify demo seed users"
SEED_COUNT="$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -tAc "select count(*) from staff_users where username in ('admin1','waiter1','kitchen1','superadmin');")"
echo "Seed users found: $SEED_COUNT"

if [[ "$APPLIED_COUNT" -lt "$MIG_FILES_COUNT" ]]; then
  echo "Migration count mismatch."
  exit 1
fi

if [[ "$SEED_COUNT" -lt 3 ]]; then
  echo "Seed users missing."
  exit 1
fi

echo "OK: migrations applied and seed data exists."
