# Virtual Waiter (SaaS) — платформа «Виртуальный официант»

Это монорепозиторий системы для ресторанов/кафе:
гость открывает меню по QR, оформляет заказ, вызывает официанта, запрашивает счёт.
Платформа мульти‑тенантная (заведения/филиалы), с планами залов и защитой QR‑ссылок.

## Что внутри
- **backend/** — API (Java 17, Spring Boot, PostgreSQL, Flyway)
- **guest-web/** — веб‑интерфейс гостя/админки (Next.js)
- **staff-app/** — приложение персонала (Flutter)
- **infra/** — docker‑compose
- **scripts/** — проверки миграций и smoke‑тесты

## Быстрый старт (локально)

### 1) База данных (PostgreSQL)

```bash
cd infra
docker compose up -d
```

### 2) Backend

```bash
cd backend
./gradlew bootRun
```

Backend: `http://localhost:8080`

> Обязательные секреты (иначе backend не запустится):
> - `APP_AUTH_COOKIE_SECRET` (минимум 32 символа)
> - `APP_QR_HMAC_SECRET` (минимум 32 символа)
>
> В локальной разработке можно задать так:
> ```bash
> export APP_AUTH_COOKIE_SECRET=dev_cookie_secret_change_me_32chars
> export APP_QR_HMAC_SECRET=dev_qr_hmac_secret_change_me_32chars
> ```

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

Если `flutter` не найден, установите Flutter SDK и добавьте в PATH на постоянной основе (пример для Linux):
```bash
sudo mkdir -p /opt
sudo git clone https://github.com/flutter/flutter.git -b stable --depth 1 /opt/flutter
echo 'export PATH=/opt/flutter/bin:$PATH' >> ~/.profile
echo 'export PATH=/opt/flutter/bin:$PATH' >> ~/.bashrc
source ~/.profile || true
source ~/.bashrc || true
flutter --version
```
Если `flutter` всё ещё не найден — перезапустите терминал/сессию.
Если Flutter уже установлен, просто добавьте путь в PATH (пример):
```bash
echo 'export PATH=/home/codespace/flutter/bin:$PATH' >> ~/.profile
echo 'export PATH=/home/codespace/flutter/bin:$PATH' >> ~/.bashrc
```

---

## Демо‑данные
Flyway создаёт демо‑данные:
- tenant: **Demo Cafe**
- branch: **Main** (id=1)
- table: **#1**, `public_id = TBL_DEMO_0001`
- staff:
  - waiter: `waiter1` / `demo123`
  - kitchen: `kitchen1` / `demo123`

## Админка / супер‑админка
- Admin: `http://localhost:3000/admin`
- Super Admin: `http://localhost:3000/superadmin`

## Роли и доступы
- `SUPER_ADMIN` — полный доступ ко всем заведениям и филиалам.
- `OWNER` — владелец ресторана (admin‑like для своих филиалов).
- `ADMIN` — админ своих филиалов.
- `MANAGER` — менеджер одного филиала.
- `WAITER`, `HOST`, `KITCHEN`, `BAR` — персонал филиала.

## Аутентификация staff‑пользователей
Используется **httpOnly cookie** (Basic Auth отключён).

- Логин: `POST /api/auth/login`
- Выход: `POST /api/auth/logout`
- Cookie по умолчанию: `vw_auth`

Пример (curl):
```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"waiter1","password":"demo123"}'
```
Далее используйте cookie из ответа:
```bash
curl -i http://localhost:8080/api/admin/branch \
  -H "Cookie: vw_auth=PASTE_TOKEN"
```

## OTP для гостя (SMS / WhatsApp / Telegram)
Гость при входе выбирает канал OTP: **SMS / WhatsApp / Telegram**.

В dev используется **mock‑провайдер**, реальные сообщения не отправляются.

Пример запроса:
```bash
curl -X POST http://localhost:8080/api/public/otp/send \
  -H "Content-Type: application/json" \
  -d '{"phone":"+37360000000","channel":"SMS","tablePublicId":"TBL_DEMO_0001"}'
```

## Онлайн‑оплата (MAIB / Paynet / MIA)
- Включается в админке: **Settings → Online payments enabled**.
- Гость может оплатить **весь счёт** или **частично** (свои позиции).
- Webhook: `POST /api/public/payments/webhook/{provider}`
- Подпись: `X-Signature = HMAC‑SHA256(hex)` от сырого тела.
- Секреты:
  - `APP_PAYMENTS_MAIB_WEBHOOK_SECRET`
  - `APP_PAYMENTS_PAYNET_WEBHOOK_SECRET`
  - `APP_PAYMENTS_MIA_WEBHOOK_SECRET`

## Где лежат конфиги
- Backend: `backend/src/main/resources/application.yml`
- Пример переменных окружения: `.env.example`

## Запуск полного стека (Docker)

```bash
cp .env.example .env
# заполните .env

docker compose -f infra/docker-compose.full.yml --env-file .env up -d --build
```

## Тесты и проверки

```bash
# Проверка миграций
bash scripts/check_migrations.sh

# Smoke‑тесты (поднимают базу и backend локально)
bash scripts/smoke.sh

# Backend unit/integration
docker compose -f infra/docker-compose.yml up -d
./backend/gradlew -p backend test

# Guest web build
npm -C guest-web run build

# Staff app tests
cd staff-app
flutter test
```

## Полезные файлы
- `RUN_WINDOWS.md` — запуск на Windows
- `RUN_HOSTING.md` — запуск на VPS/хостинге
- `TESTS.md` — чек‑лист тестирования

---

Если вы новичок — начните с **RUN_WINDOWS.md** (локально) или **RUN_HOSTING.md** (сервер).
