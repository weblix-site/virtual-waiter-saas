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

if [[ -z "$TABLE_PUBLIC_ID" ]]; then
  echo "==> Resolve tablePublicId"
  TABLE_PUBLIC_ID="$(curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" \
    "${API_BASE}/api/admin/tables" | python3 - <<'PY'
import json,sys
data=json.load(sys.stdin)
print(data[0]["publicId"] if data else "")
PY
)"
  if [[ -z "$TABLE_PUBLIC_ID" ]]; then
    echo "==> Create table"
    CREATED_JSON="$(curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" \
      -H "Content-Type: application/json" \
      -X POST "${API_BASE}/api/admin/tables" \
      -d "{\"number\":1}")"
    TABLE_PUBLIC_ID="$(python3 - <<'PY'
import json,sys
print(json.load(sys.stdin)["publicId"])
PY
<<<"$CREATED_JSON")"
  fi
fi

echo "==> Fetch signed URL for table ${TABLE_PUBLIC_ID}"
SIGNED_JSON="$(curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" \
  "${API_BASE}/api/admin/tables/${TABLE_PUBLIC_ID}/signed-url")"
SIGNED_URL="$(python3 - <<'PY'
import json,sys
print(json.load(sys.stdin)["url"])
PY
<<<"$SIGNED_JSON")"
SIG="$(python3 - <<'PY'
import sys,urllib.parse
u=sys.stdin.read().strip()
q=urllib.parse.urlparse(u).query
print(urllib.parse.parse_qs(q)["sig"][0])
PY
<<<"$SIGNED_URL")"
TS="$(python3 - <<'PY'
import sys,urllib.parse
u=sys.stdin.read().strip()
q=urllib.parse.urlparse(u).query
print(urllib.parse.parse_qs(q)["ts"][0])
PY
<<<"$SIGNED_URL")"

echo "==> Start session"
SESSION_JSON="$(curl -sS -X POST "${API_BASE}/api/public/session/start" \
  -H "Content-Type: application/json" \
  -d "{\"tablePublicId\":\"${TABLE_PUBLIC_ID}\",\"sig\":\"${SIG}\",\"ts\":${TS},\"locale\":\"${LOCALE}\"}")"
GUEST_SESSION_ID="$(python3 - <<'PY'
import json,sys
print(json.load(sys.stdin)["guestSessionId"])
PY
<<<"$SESSION_JSON")"
SESSION_SECRET="$(python3 - <<'PY'
import json,sys
print(json.load(sys.stdin)["sessionSecret"])
PY
<<<"$SESSION_JSON")"
OTP_REQUIRED="$(python3 - <<'PY'
import json,sys
print(str(json.load(sys.stdin).get("otpRequired", False)).lower())
PY
<<<"$SESSION_JSON")"

if [[ "$OTP_REQUIRED" == "true" ]]; then
  echo "==> OTP send/verify"
  OTP_SEND_JSON="$(curl -sS -X POST "${API_BASE}/api/public/otp/send" \
    -H "Content-Type: application/json" \
    -H "X-Session-Secret: ${SESSION_SECRET}" \
    -d "{\"guestSessionId\":${GUEST_SESSION_ID},\"phoneE164\":\"${PHONE_E164}\",\"locale\":\"${LOCALE}\"}")"
  OTP_CHALLENGE_ID="$(python3 - <<'PY'
import json,sys
print(json.load(sys.stdin)["challengeId"])
PY
<<<"$OTP_SEND_JSON")"
  OTP_DEV_CODE="$(python3 - <<'PY'
import json,sys
print(json.load(sys.stdin).get("devCode",""))
PY
<<<"$OTP_SEND_JSON")"
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
FIRST_ITEM_ID="$(python3 - <<'PY'
import json,sys
data=json.load(sys.stdin)
items=[]
for c in data.get("categories",[]):
  items.extend(c.get("items",[]))
print(items[0]["id"] if items else "")
PY
<<<"$MENU_JSON")"
if [[ -z "$FIRST_ITEM_ID" ]]; then
  echo "No menu items found; aborting order test."
  exit 1
fi

echo "==> Create order"
ORDER_JSON="$(curl -sS -X POST "${API_BASE}/api/public/orders" \
  -H "Content-Type: application/json" \
  -H "X-Session-Secret: ${SESSION_SECRET}" \
  -d "{\"guestSessionId\":${GUEST_SESSION_ID},\"items\":[{\"menuItemId\":${FIRST_ITEM_ID},\"qty\":1}]}")"
ORDER_ID="$(python3 - <<'PY'
import json,sys
print(json.load(sys.stdin)["orderId"])
PY
<<<"$ORDER_JSON")"
curl -sS "${API_BASE}/api/public/orders/${ORDER_ID}?guestSessionId=${GUEST_SESSION_ID}" \
  -H "X-Session-Secret: ${SESSION_SECRET}" >/dev/null

echo "==> Create party"
PARTY_JSON="$(curl -sS -X POST "${API_BASE}/api/public/party/create" \
  -H "Content-Type: application/json" \
  -H "X-Session-Secret: ${SESSION_SECRET}" \
  -d "{\"guestSessionId\":${GUEST_SESSION_ID}}")"
PARTY_ID="$(python3 - <<'PY'
import json,sys
print(json.load(sys.stdin)["partyId"])
PY
<<<"$PARTY_JSON")"
echo "Party id: ${PARTY_ID}"

echo "==> Create bill request (MY, CASH)"
curl -sS -X POST "${API_BASE}/api/public/bill-request/create" \
  -H "Content-Type: application/json" \
  -H "X-Session-Secret: ${SESSION_SECRET}" \
  -d "{\"guestSessionId\":${GUEST_SESSION_ID},\"mode\":\"MY\",\"paymentMethod\":\"CASH\",\"tipsPercent\":5}" >/dev/null

echo "==> Hall plans versions"
HALL_ID="$(curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" "${API_BASE}/api/admin/halls" | python3 - <<'PY'
import json,sys
data=json.load(sys.stdin)
print(data[0]["id"] if data else "")
PY
)"
if [[ -n "$HALL_ID" ]]; then
  PLAN_ID="$(curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" "${API_BASE}/api/admin/halls/${HALL_ID}/plans" | python3 - <<'PY'
import json,sys
data=json.load(sys.stdin)
print(data[0]["id"] if data else "")
PY
)"
  if [[ -n "$PLAN_ID" ]]; then
    curl -sS -u "${ADMIN_USER}:${ADMIN_PASS}" "${API_BASE}/api/admin/hall-plans/${PLAN_ID}/versions" >/dev/null
  else
    echo "No hall plans found; skip versions check."
  fi
else
  echo "No halls found; skip hall plans check."
fi

echo "Smoke tests completed."
