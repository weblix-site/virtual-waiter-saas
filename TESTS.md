# Virtual Waiter SaaS — Test Checklist (Manual + Smoke)

This document provides step-by-step test cases with expected results and sample data.

## 0) Prerequisites
- Backend running on `http://localhost:8080`
- Guest web on `http://localhost:3000`
- Admin/staff credentials available (demo values if seeded)
- Database migrated
- For QR signed links: `app.qr.hmacSecret` configured

### Suggested demo users
- Admin: `admin1 / demo123` (role ADMIN)
- Waiter: `waiter1 / demo123` (role WAITER)
- Kitchen: `kitchen1 / demo123` (role KITCHEN)

### Required test data
1) One branch with at least:
   - 2 tables
   - 2 menu categories
   - 5 menu items with RU/RO/EN names
2) One modifier group with at least 2 options; attached to 1 menu item
3) Branch settings with default flags (OTP off) and then OTP on

---

## A) Backend/API Smoke

### A1. Health / startup
Steps:
1. Start backend.
Expected:
- App starts without errors
- Flyway migrations complete

### A2. Admin auth
Request:
```
GET /api/admin/me
Authorization: Basic <admin>
```
Expected:
- 200 with admin user data and correct role

### A3. Staff auth
Request:
```
GET /api/staff/me
Authorization: Basic <waiter>
```
Expected:
- 200 with staff user data and role

### A4. QR signature valid/invalid
Steps:
1. Get signed URL for a table via staff/admin endpoint.
2. Call guest page with correct `sig` + `ts`.
3. Modify `sig` or `ts` (expired/out of window) and retry.
Expected:
- Correct `sig+ts`: OK
- Invalid: 403

### A5. Session start
Request:
```
POST /api/public/session/start
{
  "tablePublicId": "<tablePublicId>",
  "sig": "<sig>",
  "ts": <ts>,
  "locale": "ru"
}
```
Expected:
- Returns `guestSessionId`, `sessionSecret`, `otpRequired`.

### A6. Session secret enforced
Steps:
1. Call a public POST without `X-Session-Secret`.
Expected:
- 403

### A7. Rate limits (OTP/Order/Party)
Steps:
1. Repeatedly call `/api/public/otp/send` beyond limit.
2. Repeatedly call `/api/public/orders` beyond limit.
3. Repeatedly call `/api/public/party/create` or `/api/public/party/join` beyond limit.
4. Repeatedly call `/api/public/otp/verify` beyond limit.
5. Repeatedly call `/api/public/waiter-call` beyond limit.
6. Repeatedly call `/api/public/session/start` beyond limit.
7. Repeatedly call `/api/public/menu` beyond limit.
Expected:
- 429 after limit reached for each endpoint.

---

## B) Guest‑web Functional

### B1. Menu load
Steps:
1. Open guest web with valid signed URL.
Expected:
- Categories and items load
- Language matches selected locale

### B2. Menu item details
Steps:
1. Open item detail.
Expected:
- Photos, description, ingredients, K/B/F/C, tags present

### B2.1 Menu card UI
Steps:
1. Check menu cards for weight/Kcal/PCF chips.
2. Open modifiers; verify required/optional badges and selected count.
Expected:
- Chips and modifier badges render correctly

### B3. Modifiers
Steps:
1. Item with modifiers: try to add without required selection.
2. Select valid options.
Expected:
- Validation error when missing required
- Price updates with modifiers

### B4. Cart
Steps:
1. Add multiple items and modifiers.
2. Remove one item.
Expected:
- Totals update correctly

### B5. Create order
Steps:
1. Place order.
Expected:
- Order created, status shown

### B6. Order status polling
Steps:
1. In staff app change order status.
Expected:
- Guest web updates status on poll

### B7. Party PIN create/join
Steps:
1. Create party; note PIN.
2. In another session join using PIN.
Expected:
- Both sessions share same party
- Guest web shows participants

### B8. Waiter call
Steps:
1. Click “Call waiter”.
Expected:
- Staff app shows waiter call

### B9. Bill request modes
Steps:
1. Create BillRequest (MY / SELECTED / WHOLE_TABLE) depending on flags.
Expected:
- Only allowed modes appear
- Bill status updates (created/confirmed/cancelled)

### B9.1 Current charges block
Steps:
1. Place an order and open “Текущие начисления”.
2. Compare “Мои позиции” and “Позиции по столу” totals.
3. Click “Обновить начисления”.
Expected:
- Totals update and match bill options

### B10. Tips
Steps:
1. Enable tips flag; create bill with tips.
Expected:
- Tips applied and shown in totals

---

## C) Staff‑app (Waiter/Kitchen)

### C1. Active Orders list
Steps:
1. Open Orders tab.
Expected:
- Orders list loads
- SLA chips show

### C2. Focus cards (top 3 SLA)
Steps:
1. View Orders/Calls/Bills/Kitchen tabs.
Expected:
- Focus cards appear and toggle works

### C2.1 Floor plan active sync
Steps:
1. In admin set another active plan for the same hall.
2. In staff app (Hall tab) with “Use active = ON” wait for sync.
Expected:
- Layout switches automatically to the new active plan

### C2.2 SLA thresholds
Steps:
1. In staff app (Hall tab) open “SLA settings”, change warn/crit.
2. Observe SLA chips and heatmap legend update.
Expected:
- New thresholds apply without restart

### C3. Order status transitions
Steps:
1. Waiter: set ACCEPTED/READY/SERVED/CLOSED
2. Kitchen: set ACCEPTED/IN_PROGRESS/READY/SERVED
Expected:
- Forbidden transitions rejected
- Allowed transitions succeed

### C4. Kitchen queue
Steps:
1. Filter by status; sort by SLA/priority.
Expected:
- Sorting works

### C5. Waiter calls
Steps:
1. Ack and Close call.
Expected:
- Status changes

### C6. Bills
Steps:
1. Confirm paid
2. Cancel bill (CREATED only) — confirmation dialog
Expected:
- Status updates

### C6.1 Party close confirmation
Steps:
1. In Bills tab, click “Close Party”.
2. Confirm dialog appears, then confirm.
Expected:
- Party closes only after confirmation

### C7. History tab
Steps:
1. Select table, then guest session.
2. Use back button to return to all guests.
3. Set date range filter.
Expected:
- Orders list filtered by selection
- Back clears guest filter
- Date filter applies
 - Presets (Today/7 days/30 days) apply correctly

---

## D) Admin‑web

### D1. Menu categories + items CRUD
Steps:
1. Create, edit, disable category
2. Create item with RU/RO/EN
Expected:
- List updates

### D1.1 Admin filters persistence
Steps:
1. Set menu/table/staff filters.
2. Reload page.
Expected:
- Filters persist (localStorage)

### D2. Menu search
Steps:
1. Search by RU/RO/EN name.
Expected:
- Filtered list

### D3. Modifiers
Steps:
1. Create group + options
2. Attach group to item
Expected:
- Item modifiers updated

### D4. Tables & QR
Steps:
1. Create table, show QR, refresh all
Expected:
- Signed URL includes `ts` and valid

### D4.1 Zones JSON validation
Steps:
1. Save zonesJson with invalid schema (non-array or bad color).
2. Save zonesJson with out-of-range x/y/w/h or w/h <= 0.
Expected:
- 400 with validation error

### D5. Staff CRUD
Steps:
1. Create waiter
2. Filter by role
Expected:
- Filters work

### D6. Branch settings
Steps:
1. Toggle OTP/Party/Tips/Payment methods.
Expected:
- Guest web responds accordingly

### D7. Parties list
Steps:
1. Create party in guest; view in admin Parties list.
2. Filter by Table and PIN.
3. Expand row; copy participant IDs.
4. Set party close/expiry near now, adjust Expiring (min), verify highlight + badge.
Expected:
- Party visible with participant IDs
- Filters work
- Copy button copies IDs
- Expiring parties are highlighted and labeled

### D8. Stats + CSV
Steps:
1. Load stats for range.
2. Download CSV.
3. Set waiter filter and ensure counts match orders handled by that waiter (handled_by_staff_id).
4. Set Hall and Plan filters (plan within hall) and verify stats are scoped.
Expected:
- Data matches activity

### D9. Audit logs
Steps:
1. Filter by action/entity/actor/date.
2. Next page.
2.1 Prev page.
3. CSV export.
Expected:
- Correct filtering and pagination
- Limit affects page size

---

## E) Super‑admin

### E1. Tenants/branches
Steps:
1. Create tenant + branch
2. Disable branch
Expected:
- Admin role obeys branch access

### E2. Global stats + CSV
Steps:
1. Load global stats
2. Download CSV
Expected:
- Data aggregated

---

## F) Security checks

### F1. Cross‑branch access
Steps:
1. Use admin of branch A to access branch B data.
Expected:
- 403

### F2. Invalid session secret
Steps:
1. Use wrong `X-Session-Secret` on public POST.
Expected:
- 403

### F3. OTP required flow
Steps:
1. Enable OTP; try to order without verify.
Expected:
- 403 until verified

---

## Optional Automation (Smoke)

You can script quick checks with `curl` or Postman. If you want, I can generate a `scripts/smoke.sh` and a Postman collection.
