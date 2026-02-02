# Virtual Waiter (SaaS) — MVP Starter

This repo is a **starting implementation** of the Technical Spec (ТЗ) for the Virtual Waiter platform.

## What is implemented (Sprint 1 baseline)
- **Backend (Java 17 / Spring Boot / PostgreSQL / Flyway)**
  - Guest session start (`/api/public/session/start`)
  - Menu by table (`/api/public/menu`)
  - Create order (`/api/public/orders`)
  - Call waiter (`/api/public/waiter-call`)
  - Staff endpoints (Basic Auth + DB-backed users)
    - `GET /api/staff/me`
    - `GET /api/staff/orders/active`
    - `POST /api/staff/orders/{id}/status`
    - `GET /api/staff/waiter-calls/active`

- **Guest Web (Next.js)**
  - Table route `/t/{tablePublicId}`: menu, cart, place order, call waiter

- **Staff App (Flutter)**
  - Basic login (Basic Auth), list active orders, order details, update status
  - Hall view (floor plan) with table highlights for new orders

## Quick start (local)

### 1) Start DB
```bash
cd infra
docker compose up -d
```

### 2) Run backend
```bash
cd backend
gradle bootRun
```

> Note: this repo does not include the Gradle Wrapper yet. If you prefer it, generate it later with `gradle wrapper`.

Backend runs on `http://localhost:8080`.

Demo data is seeded by Flyway:
- tenant: Demo Cafe
- branch: Main (id=1)
- table: #1, public_id = `TBL_DEMO_0001`
- staff users (Basic Auth):
  - waiter: `waiter1` / `demo123`
  - kitchen: `kitchen1` / `demo123`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### 3) Run guest web
```bash
cd guest-web
npm install
NEXT_PUBLIC_API_BASE=http://localhost:8080 npm run dev
```
Open: `http://localhost:3000/t/TBL_DEMO_0001?lang=ru`

### 4) Run staff app
```bash
cd staff-app
flutter pub get
flutter run --dart-define=API_BASE=http://localhost:8080
```

## Next steps (per ТЗ)
- OTP (sms.md) as a feature-flag (before first order)
- Party PIN (4 digits) + “join table by PIN”
- Bill requests: pay own / selected items / whole table (flags)
- Tips (percentages configurable in admin)
- Currency: default MDL, set per-branch in admin (super admin manages allowed currencies)



## QR signature (HMAC)
Public guest endpoints now require a **QR signature** (`sig`) to prevent tampering with `tablePublicId`.

- Signature algorithm: `sig = base64url(HMAC-SHA256(secret, tablePublicId))` (no padding)
- Configure secret in `backend/src/main/resources/application.yml` under `app.qr.hmacSecret`.
- Configure public base URL (used for generating signed URLs) under `app.publicBaseUrl`.

### Get a signed URL for a table (dev helper)
Call (Basic Auth as staff):
- `GET /api/staff/tables/{tablePublicId}/signed-url`

Example:
```bash
curl -u waiter1:demo123 "http://localhost:8080/api/staff/tables/TBL_DEMO_0001/signed-url"
```

Then open the returned `url` in the browser.

## Floor plan (Halls & Plans)
Admins can create **halls** and multiple **plans** per hall, then set an active plan.

Key endpoints (admin):
- `GET /api/admin/halls`
- `POST /api/admin/halls`
- `GET /api/admin/halls/{id}`
- `PATCH /api/admin/halls/{id}` (set `activePlanId`)
- `GET /api/admin/halls/{hallId}/plans`
- `POST /api/admin/halls/{hallId}/plans`
- `PATCH /api/admin/hall-plans/{id}`
- `POST /api/admin/hall-plans/{id}/duplicate`

Staff:
- `GET /api/staff/halls`
- `GET /api/staff/branch-layout?hallId=...`

## Rate limits (per IP)
Defaults in `backend/src/main/resources/application.yml`:
- `app.rateLimit.otp.maxRequests`: 5 / `app.rateLimit.otp.windowSeconds`: 300
- `app.rateLimit.otpVerify.maxRequests`: 8 / `app.rateLimit.otpVerify.windowSeconds`: 300
- `app.rateLimit.order.maxRequests`: 10 / `app.rateLimit.order.windowSeconds`: 60
- `app.rateLimit.party.maxRequests`: 10 / `app.rateLimit.party.windowSeconds`: 60
- `app.rateLimit.waiterCall.maxRequests`: 10 / `app.rateLimit.waiterCall.windowSeconds`: 60
- `app.rateLimit.sessionStart.maxRequests`: 30 / `app.rateLimit.sessionStart.windowSeconds`: 60
- `app.rateLimit.menu.maxRequests`: 60 / `app.rateLimit.menu.windowSeconds`: 60
