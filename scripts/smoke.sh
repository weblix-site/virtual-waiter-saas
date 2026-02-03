#!/usr/bin/env bash
set -euo pipefail

# Minimal integration smoke tests for public flows and hall plans.
# Requires: curl, python3

API_BASE="${API_BASE:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin1}"
ADMIN_PASS="${ADMIN_PASS:-demo123}"
TABLE_PUBLIC_ID="${TABLE_PUBLIC_ID:-}"
PHONE_E164="${PHONE_E164:-+37369000000}"
LOCALE="${LOCALE:-ru}"
DB_NAME="${DB_NAME:-vw}"
DB_USER="${DB_USER:-vw}"
DB_PASS="${DB_PASS:-vw}"
DB_PORT="${DB_PORT:-5432}"
COMPOSE_FILE="${COMPOSE_FILE:-infra/docker-compose.yml}"
LOG_FILE="${SMOKE_BACKEND_LOG:-/tmp/vw_smoke_backend.log}"

BACKEND_PID=""

cleanup() {
  if [[ -n "${BACKEND_PID}" ]]; then
    kill "${BACKEND_PID}" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

if [[ -n "${JAVA_HOME_17_X64:-}" ]]; then
  export JAVA_HOME="${JAVA_HOME_17_X64}"
  export PATH="${JAVA_HOME}/bin:${PATH}"
fi

ensure_backend() {
  if curl -fsS "${API_BASE}/actuator/health" >/dev/null 2>&1; then
    return 0
  fi
  if command -v docker >/dev/null 2>&1; then
    echo "==> Start postgres (docker compose)"
    docker compose -f "$COMPOSE_FILE" up -d --force-recreate postgres >/dev/null
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
  fi

  if [[ -x "./backend/gradlew" ]]; then
    echo "==> Start backend"
    export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:${DB_PORT}/${DB_NAME}"
    export SPRING_DATASOURCE_USERNAME="$DB_USER"
    export SPRING_DATASOURCE_PASSWORD="$DB_PASS"
    export APP_AUTH_COOKIE_SECRET="${APP_AUTH_COOKIE_SECRET:-dev_cookie_secret_change_me}"
    export APP_QR_HMAC_SECRET="${APP_QR_HMAC_SECRET:-dev_qr_hmac_secret_change_me}"
    mkdir -p "$(dirname "$LOG_FILE")"
    : > "$LOG_FILE"
    ./backend/gradlew -p backend bootRun --args="--spring.main.banner-mode=off" >"$LOG_FILE" 2>&1 &
    BACKEND_PID=$!
  else
    echo "backend/gradlew not found."
    exit 1
  fi

  echo "==> Wait for backend"
  for i in {1..120}; do
    if curl -fsS "${API_BASE}/actuator/health" >/dev/null 2>&1; then
      return 0
    fi
    if [[ -n "${BACKEND_PID}" ]] && ! kill -0 "${BACKEND_PID}" >/dev/null 2>&1; then
      echo "Backend process exited early. Logs:"
      tail -n 200 "$LOG_FILE" || true
      exit 1
    fi
    sleep 1
  done
  echo "Backend did not become ready. Logs:"
  tail -n 200 "$LOG_FILE" || true
  if [[ -n "${DB_CONTAINER:-}" ]]; then
    echo "==> Postgres logs:"
    docker logs "$DB_CONTAINER" | tail -n 200 || true
  fi
  exit 1
}

ensure_backend

if [[ -z "$TABLE_PUBLIC_ID" ]]; then
  echo "==> Resolve tablePublicId"
  TABLES_RESP="$(curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" -w "HTTPSTATUS:%{http_code}" \
    "${API_BASE}/api/admin/tables")"
  TABLES_BODY="${TABLES_RESP%HTTPSTATUS:*}"
  TABLES_STATUS="${TABLES_RESP##*HTTPSTATUS:}"
  if [[ "$TABLES_STATUS" != "200" ]]; then
    echo "Admin tables request failed: HTTP $TABLES_STATUS"
    echo "$TABLES_BODY"
    exit 1
  fi
  if ! TABLE_PUBLIC_ID="$(python3 -c 'import json,sys; data=json.load(sys.stdin); \
val=(data[0].get("publicId") if data else None); \
sys.exit(2) if not data else None; \
sys.exit(3) if not val else None; \
print(val)' <<<"$TABLES_BODY")"; then
    echo "Failed to parse publicId from admin tables response:"
    echo "$TABLES_BODY"
    exit 1
  fi
  if [[ -z "$TABLE_PUBLIC_ID" ]]; then
    echo "==> Create table"
    CREATED_RESP="$(curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" -w "HTTPSTATUS:%{http_code}" \
      -H "Content-Type: application/json" \
      -X POST "${API_BASE}/api/admin/tables" \
      -d "{\"number\":1}")"
    CREATED_BODY="${CREATED_RESP%HTTPSTATUS:*}"
    CREATED_STATUS="${CREATED_RESP##*HTTPSTATUS:}"
    if [[ "$CREATED_STATUS" != "200" ]]; then
      echo "Create table failed: HTTP $CREATED_STATUS"
      echo "$CREATED_BODY"
      exit 1
    fi
    if ! TABLE_PUBLIC_ID="$(python3 -c 'import json,sys; data=json.load(sys.stdin); val=data.get("publicId"); \
sys.exit(3) if not val else None; \
print(val)' <<<"$CREATED_BODY")"; then
      echo "Failed to parse publicId from create table response:"
      echo "$CREATED_BODY"
      exit 1
    fi
    if [[ -z "${TABLE_PUBLIC_ID//[[:space:]]/}" ]]; then
      echo "Create table returned empty publicId (raw): $(printf '%q' "$TABLE_PUBLIC_ID")"
      echo "$CREATED_BODY"
      exit 1
    fi
  fi
fi

echo "==> Fetch signed URL for table ${TABLE_PUBLIC_ID}"
SIGNED_RESP="$(curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" -w "HTTPSTATUS:%{http_code}" \
  "${API_BASE}/api/admin/tables/${TABLE_PUBLIC_ID}/signed-url")"
SIGNED_BODY="${SIGNED_RESP%HTTPSTATUS:*}"
SIGNED_STATUS="${SIGNED_RESP##*HTTPSTATUS:}"
if [[ "$SIGNED_STATUS" != "200" ]]; then
  echo "Signed URL failed: HTTP $SIGNED_STATUS"
  echo "$SIGNED_BODY"
  exit 1
fi
SIGNED_JSON="$SIGNED_BODY"
if ! SIGNED_URL="$(python3 -c 'import json,sys; data=json.load(sys.stdin); val=data.get("url"); \
sys.exit(3) if not val else None; \
print(val)' <<<"$SIGNED_JSON")"; then
  echo "Failed to parse signed url response:"
  echo "$SIGNED_JSON"
  exit 1
fi
SIG="$(python3 -c 'import sys,urllib.parse; u=sys.stdin.read().strip(); q=urllib.parse.urlparse(u).query; print(urllib.parse.parse_qs(q)["sig"][0])' <<<"$SIGNED_URL")"
TS="$(python3 -c 'import sys,urllib.parse; u=sys.stdin.read().strip(); q=urllib.parse.urlparse(u).query; print(urllib.parse.parse_qs(q)["ts"][0])' <<<"$SIGNED_URL")"

echo "==> Start session"
SESSION_JSON="$(curl -sS -X POST "${API_BASE}/api/public/session/start" \
  -H "Content-Type: application/json" \
  -d "{\"tablePublicId\":\"${TABLE_PUBLIC_ID}\",\"sig\":\"${SIG}\",\"ts\":${TS},\"locale\":\"${LOCALE}\"}")"
GUEST_SESSION_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["guestSessionId"])' <<<"$SESSION_JSON")"
SESSION_SECRET="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["sessionSecret"])' <<<"$SESSION_JSON")"
OTP_REQUIRED="$(python3 -c 'import json,sys; print(str(json.load(sys.stdin).get("otpRequired", False)).lower())' <<<"$SESSION_JSON")"

if [[ "$OTP_REQUIRED" == "true" ]]; then
  echo "==> OTP send/verify"
  OTP_SEND_JSON="$(curl -sS -X POST "${API_BASE}/api/public/otp/send" \
    -H "Content-Type: application/json" \
    -H "X-Session-Secret: ${SESSION_SECRET}" \
    -d "{\"guestSessionId\":${GUEST_SESSION_ID},\"phoneE164\":\"${PHONE_E164}\",\"locale\":\"${LOCALE}\"}")"
  OTP_CHALLENGE_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["challengeId"])' <<<"$OTP_SEND_JSON")"
  OTP_DEV_CODE="$(python3 -c 'import json,sys; print(json.load(sys.stdin).get("devCode",""))' <<<"$OTP_SEND_JSON")"
  if [[ -n "$OTP_DEV_CODE" ]]; then
    curl -sS -X POST "${API_BASE}/api/public/otp/verify" \
      -H "Content-Type: application/json" \
      -H "X-Session-Secret: ${SESSION_SECRET}" \
      -d "{\"guestSessionId\":${GUEST_SESSION_ID},\"challengeId\":${OTP_CHALLENGE_ID},\"code\":\"${OTP_DEV_CODE}\"}" >/dev/null
  else
    echo "OTP devCode is empty; verify manually."
  fi
fi

echo "==> Load menu"
MENU_JSON="$(curl -sS "${API_BASE}/api/public/menu?tablePublicId=${TABLE_PUBLIC_ID}&sig=${SIG}&ts=${TS}&locale=${LOCALE}")"
FIRST_ITEM_ID="$(python3 -c 'import json,sys; data=json.load(sys.stdin); items=[]; [items.extend(c.get("items",[])) for c in data.get("categories",[])]; print(items[0]["id"] if items else "")' <<<"$MENU_JSON")"
if [[ -z "$FIRST_ITEM_ID" ]]; then
  echo "==> No menu items found; creating demo category + item"
  CAT_RESP="$(curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" -w "HTTPSTATUS:%{http_code}" \
    -H "Content-Type: application/json" \
    -X POST "${API_BASE}/api/admin/menu/categories" \
    -d "{\"nameRu\":\"Тест\",\"nameRo\":\"Test\",\"nameEn\":\"Test\",\"sortOrder\":1,\"isActive\":true}")"
  CAT_BODY="${CAT_RESP%HTTPSTATUS:*}"
  CAT_STATUS="${CAT_RESP##*HTTPSTATUS:}"
  if [[ "$CAT_STATUS" != "200" ]]; then
    echo "Create category failed: HTTP $CAT_STATUS"
    echo "$CAT_BODY"
    exit 1
  fi
  CAT_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<<"$CAT_BODY")"
  ITEM_RESP="$(curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" -w "HTTPSTATUS:%{http_code}" \
    -H "Content-Type: application/json" \
    -X POST "${API_BASE}/api/admin/menu/items" \
    -d "{\"categoryId\":${CAT_ID},\"nameRu\":\"Тест\",\"nameRo\":\"Test\",\"nameEn\":\"Test\",\"descriptionRu\":\"\",\"descriptionRo\":\"\",\"descriptionEn\":\"\",\"price\":10,\"isActive\":true}")"
  ITEM_BODY="${ITEM_RESP%HTTPSTATUS:*}"
  ITEM_STATUS="${ITEM_RESP##*HTTPSTATUS:}"
  if [[ "$ITEM_STATUS" != "200" ]]; then
    echo "Create item failed: HTTP $ITEM_STATUS"
    echo "$ITEM_BODY"
    exit 1
  fi
  FIRST_ITEM_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])' <<<"$ITEM_BODY")"
fi

echo "==> Create order"
ORDER_JSON="$(curl -sS -X POST "${API_BASE}/api/public/orders" \
  -H "Content-Type: application/json" \
  -H "X-Session-Secret: ${SESSION_SECRET}" \
  -d "{\"guestSessionId\":${GUEST_SESSION_ID},\"items\":[{\"menuItemId\":${FIRST_ITEM_ID},\"qty\":1}]}")"
ORDER_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["orderId"])' <<<"$ORDER_JSON")"
curl -sS "${API_BASE}/api/public/orders/${ORDER_ID}?guestSessionId=${GUEST_SESSION_ID}" \
  -H "X-Session-Secret: ${SESSION_SECRET}" >/dev/null

echo "==> Create party"
PARTY_JSON="$(curl -sS -X POST "${API_BASE}/api/public/party/create" \
  -H "Content-Type: application/json" \
  -H "X-Session-Secret: ${SESSION_SECRET}" \
  -d "{\"guestSessionId\":${GUEST_SESSION_ID}}")"
PARTY_ID="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["partyId"])' <<<"$PARTY_JSON")"
echo "Party id: ${PARTY_ID}"

echo "==> Create bill request (MY, CASH)"
curl -sS -X POST "${API_BASE}/api/public/bill-request/create" \
  -H "Content-Type: application/json" \
  -H "X-Session-Secret: ${SESSION_SECRET}" \
  -d "{\"guestSessionId\":${GUEST_SESSION_ID},\"mode\":\"MY\",\"paymentMethod\":\"CASH\",\"tipsPercent\":5}" >/dev/null

echo "==> Hall plans versions"
HALL_ID="$(curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" "${API_BASE}/api/admin/halls" | python3 -c 'import json,sys; data=json.load(sys.stdin); print(data[0]["id"] if data else "")')"
if [[ -n "$HALL_ID" ]]; then
  PLAN_ID="$(curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" "${API_BASE}/api/admin/halls/${HALL_ID}/plans" | python3 -c 'import json,sys; data=json.load(sys.stdin); print(data[0]["id"] if data else "")')"
  if [[ -n "$PLAN_ID" ]]; then
    curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" "${API_BASE}/api/admin/hall-plans/${PLAN_ID}/versions" >/dev/null
  else
    echo "No hall plans found; skip versions check."
  fi
else
  echo "No halls found; skip hall plans check."
fi

echo "Smoke tests completed."
