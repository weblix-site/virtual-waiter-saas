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
- План зала (Halls/Plans), шаблоны, импорт/экспорт JSON, версии
- Статистика и аудит (админ/супер‑админ)
- Валюты: управляются супер‑админом, выбираются для филиалов
- Ограничения по IP (rate limits)
- Локализация UI и ошибок (RU/RO/EN)

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
- staff (Basic Auth):
  - waiter: `waiter1` / `demo123`
  - kitchen: `kitchen1` / `demo123`

## Админка и супер‑админка
- Admin Web: `http://localhost:3000/admin`
- Super Admin Web: `http://localhost:3000/superadmin`

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
- OTP, Order, Party, WaiterCall, SessionStart, Menu

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

## Структура репозитория
- `backend/` — API и миграции
- `guest-web/` — интерфейс гостя/админа/супер‑админа
- `staff-app/` — Flutter приложение
- `infra/` — docker‑compose
- `scripts/` — проверки и smoke‑тесты
