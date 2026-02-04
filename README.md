# Virtual Waiter (SaaS) — платформа «Виртуальный официант»

Репозиторий содержит MVP SaaS‑платформы для ресторанов/кафе: гость открывает меню по QR, оформляет заказ, вызывает официанта, запрашивает счёт и оставляет чаевые. Платформа мульти‑тенантная, с планами залов и защитой QR‑ссылок.

## Стек
- Backend: Java 17, Spring Boot, PostgreSQL, Flyway
- Guest Web: Next.js (app router)
- Staff App: Flutter
- Infra: Docker Compose

## Основной функционал
- Мульти‑тенантность: заведения/филиалы
- Гостевой поток: меню, корзина, заказ, вызов официанта, счёт, чаевые
- OTP по SMS (feature flag)
- Party PIN (объединение гостей)
- Оффлайн‑оплата: наличные/терминал
- Онлайн‑оплата: MAIB/Paynet/MIA (через провайдера, по счёту)
- План зала (Halls/Plans), шаблоны, импорт/экспорт JSON, версии
- Статистика и аудит (админ/супер‑админ)
- Валюты: управляются супер‑админом, выбираются для филиалов
- Ограничения по IP (rate limits)
- Локализация UI и ошибок (RU/RO/EN)
- SLA по заказам: фиксируются timestamps статусов (accepted/in_progress/ready/served/closed/cancelled) и используются в мотивации официантов.

## Быстрый старт (локально)

### 1) База данных
```bash
cd infra
docker compose up -d
```

### 2) Backend
```bash
cd backend
gradle bootRun
```
Backend: `http://localhost:8080`

### 3) Guest Web
```bash
cd guest-web
npm install
NEXT_PUBLIC_API_BASE=http://localhost:8080 npm run dev
```
Guest web: `http://localhost:3000/t/TBL_DEMO_0001?lang=ru`

### 4) Staff App (Flutter)
```bash
cd staff-app
flutter pub get
flutter run --dart-define=API_BASE=http://localhost:8080
```

## Демо‑данные
Flyway создаёт демо‑данные:
- tenant: Demo Cafe
- branch: Main (id=1)
- table: #1, public_id = `TBL_DEMO_0001`
- staff (логин через `/api/auth/login`, httpOnly cookie):
  - waiter: `waiter1` / `demo123`
  - kitchen: `kitchen1` / `demo123`

## Админка и супер‑админка
- Admin Web: `http://localhost:3000/admin`
- Super Admin Web: `http://localhost:3000/superadmin`

## Аутентификация staff‑пользователей
- Логин: `POST /api/auth/login` (устанавливает httpOnly cookie `vw_auth`)
- Выход: `POST /api/auth/logout`
- Basic Auth не используется
- Параметры (env → application.yml):
  - `APP_AUTH_COOKIE_SECRET` — обязательный HMAC‑секрет (минимум 32 символа)
  - `APP_AUTH_COOKIE_NAME` — имя cookie (по умолчанию `vw_auth`)
  - `APP_AUTH_COOKIE_MAXAGESECONDS` — срок жизни cookie в секундах (по умолчанию 604800)
  - `APP_AUTH_COOKIE_SECURE` — `true` в проде (HTTPS)

Пример логина (curl):
```bash
curl -i -X POST http://localhost:8080/api/auth/login \\
  -H "Content-Type: application/json" \\
  -d '{"username":"waiter1","password":"demo123"}'
```
Для запросов после логина передайте cookie из ответа:
```bash
curl -i http://localhost:8080/api/admin/branch \\
  -H "Cookie: vw_auth=PASTE_TOKEN"
```
Пример выхода (curl):
```bash
curl -i -X POST http://localhost:8080/api/auth/logout \\
  -H "Cookie: vw_auth=PASTE_TOKEN"
```

## Профили официантов
- Профиль официанта (имя/фамилия/возраст/пол/фото) заполняется **только** админом или супер‑админом.
- Гость видит только **имя официанта** и **фото** (если есть).
- Staff‑app показывает **профиль** в отдельной вкладке.

## Валюты (end‑to‑end управление)
- Глобальные валюты создаёт и включает/выключает **Super Admin**.
- Admin выбирает валюту заведения из активного списка (по умолчанию `MDL`).
- Backend: `GET /api/admin/currencies` отдаёт только активные валюты для admin; `includeInactive=true` — только для superadmin.

## i18n API (админ/суперадмин и backend‑ошибки)
- Админка: `guest-web/app/admin`, суперадминка: `guest-web/app/superadmin`.
- Ошибки backend локализованы через `backend/src/main/resources/i18n/messages_{ru,ro,en}.properties`.
- Выбор языка для backend:
  - Заголовок: `X-Lang: ru|ro|en`
  - Или query‑параметр: `?lang=ru|ro|en`
  - Если не задано — `Accept-Language`.
- Для клиента важно передавать язык на все запросы (админ/суперадмин UI уже делает это автоматически).

## QR‑защита
- Формат ссылки: `.../t/{tablePublicId}?sig=...&ts=...`
- HMAC‑подпись включена
- Настройка: `APP_QR_HMAC_SECRET`

## Локализация ошибок (Backend)
Поддерживается:
- `Accept-Language: ru|ro|en`
- `X-Lang: ru|ro|en`
- `?lang=ru|ro|en`

## Rate limits (по IP)
Смотри `backend/src/main/resources/application.yml`:
- OTP, Order, Party, WaiterCall, SessionStart, Menu, Chat

## SLA‑алерты (push)
Backend автоматически отправляет `SLA_ALERT` в staff‑app (FCM), если заявка превысила критический SLA.
Настройка через `application.yml` или env:
```
app.slaAlerts.enabled=true
app.slaAlerts.pollMs=60000
app.slaAlerts.orderCritMin=10
app.slaAlerts.callCritMin=5
app.slaAlerts.billCritMin=10
app.slaAlerts.kitchenCritMin=15
app.slaAlerts.cooldownMinutes=5
app.slaAlerts.lookbackMinutes=240
```
Текст алерта локализуется по `branch_settings.default_lang` (ru/ro/en).

## Docker Compose (полный стек)
```bash
cp .env.example .env
# отредактировать .env

docker compose -f infra/docker-compose.full.yml --env-file .env up -d --build
```

## Документация и тесты
- `RUN_WINDOWS.md` — запуск на Windows
- `RUN_HOSTING.md` — запуск на VPS
- `TESTS.md` — тест‑сценарии
- `scripts/` — smoke‑скрипты и проверка миграций

## Известные уязвимости (npm audit)
В `guest-web` остаются известные уязвимости (eslint/next), которые требуют мажорных обновлений.
Мы их **не обновляли** автоматически, чтобы не сломать проект.
План: поднять версии зависимостей отдельным PR и пройти регрессионные проверки.

## Структура репозитория
- `backend/` — API и миграции
- `guest-web/` — интерфейс гостя/админа/супер‑админа
- `staff-app/` — Flutter приложение
- `infra/` — docker‑compose
- `scripts/` — проверки и smoke‑тесты
## Онлайн‑оплата (MAIB/Paynet/MIA)
- Включается в админке: **Settings → Online payments enabled**.
- Для филиала выбирается один провайдер (MAIB / Paynet / MIA) и валюта онлайн‑платежей.
- Гость оплачивает онлайн **весь счёт** или **частично** (свои/выбранные позиции) — через стандартный `BillRequest`.
- Webhook для провайдеров: `POST /api/public/payments/webhook/{provider}`.
- Для webhook используется подпись `X-Signature` = HMAC‑SHA256 (hex) от тела запроса.
- Секреты webhook: `APP_PAYMENTS_MAIB_WEBHOOK_SECRET`, `APP_PAYMENTS_PAYNET_WEBHOOK_SECRET`, `APP_PAYMENTS_MIA_WEBHOOK_SECRET`.
