# Как запустить Virtual Waiter на Windows 10 (для новичков)

Ниже простой пошаговый гайд: установка, запуск, и проверка работы. Все команды вводятся в **PowerShell**.

---

## 0) Что получится в итоге
- Backend (API): `http://localhost:8080`
- Guest web (QR-меню): `http://localhost:3000`
- Admin web: `http://localhost:3000/admin`
- Staff app (Flutter): отдельное приложение (эмулятор или телефон)

### Роли и доступы (кратко)
- `SUPER_ADMIN` — полный доступ ко всем заведениям и филиалам.
- `OWNER` — владелец ресторана (admin‑like для своих филиалов).
- `ADMIN` — админ своих филиалов.
- `MANAGER` — менеджер одного филиала.
- `WAITER`, `HOST`, `KITCHEN`, `BAR` — персонал филиала.

---

## 1) Установить нужные программы

### 1.1 Git
1. Скачайте Git: https://git-scm.com/download/win
2. Установите по умолчанию.
3. Откройте PowerShell и проверьте:
   ```
   git --version
   ```

### 1.2 Java 17 (для backend)
1. Скачайте Temurin JDK 17: https://adoptium.net/
2. Установите.
3. Проверьте:
   ```
   java -version
   ```

### 1.3 Node.js (для guest/admin web)
1. Скачайте LTS: https://nodejs.org/en
2. Установите.
3. Проверьте:
   ```
   node -v
   npm -v
   ```

### 1.4 PostgreSQL (База данных)
1. Скачайте: https://www.postgresql.org/download/windows/
2. При установке запомните пароль `postgres`.
3. Поставьте порт `5432` (по умолчанию).
4. Проверьте, что служба PostgreSQL запущена.

### 1.5 Flutter (для staff-app)
> Можно установить позже, если пока нужен только web.

1. Скачайте Flutter SDK: https://docs.flutter.dev/get-started/install/windows
2. Распакуйте, добавьте в PATH.
3. Проверьте:
   ```
   flutter --version
   ```

---

## 2) Скачать проект

В PowerShell:
```
git clone <ССЫЛКА_НА_РЕПОЗИТОРИЙ>
cd virtual-waiter-saas
```

---

## 3) Настроить базу данных

Откройте **pgAdmin** или используйте `psql`.

### Вариант A (через pgAdmin)
1. Подключитесь к серверу PostgreSQL.
2. Создайте базу `vw`.
3. Создайте пользователя `vw` с паролем `vw`.
4. Дайте пользователю права на базу `vw`.

### Вариант B (через psql)
```
psql -U postgres
CREATE DATABASE vw;
CREATE USER vw WITH PASSWORD 'vw';
GRANT ALL PRIVILEGES ON DATABASE vw TO vw;
\q
```

---

## 4) Запуск backend

Перейдите в папку `backend`:
```
cd backend
```

Запустите:
```
./gradlew bootRun
```

Ожидаемо:
- в консоли появится сообщение `Started VirtualWaiterApplication`.
- API доступен по `http://localhost:8080`

---

## 5) Запуск guest/admin web

Откройте **новое окно PowerShell**:
```
cd virtual-waiter-saas\guest-web
npm install
npm run dev
```

Ожидаемо:
- сайт доступен на `http://localhost:3000`
- админка: `http://localhost:3000/admin`

---

## 6) Запуск staff‑app (опционально)

### 6.1 Android Emulator
1. Установите Android Studio.
2. Откройте Device Manager и создайте эмулятор.
3. Запустите эмулятор.

### 6.2 Запуск Flutter
В новом PowerShell:
```
cd virtual-waiter-saas\staff-app
flutter pub get
flutter run
```

---

## 7) Настройки (важно)

### 7.1 QR‑подпись
Файл: `backend/src/main/resources/application.yml`
```
app:
  qr:
    hmacSecret: "CHANGE_ME_DEV_SECRET"
```
**Поменяйте на уникальный ключ**, иначе QR‑защита слабая.

### 7.1.1 Обязательные секреты (иначе backend не стартует)
В `application.yml` значения берутся из переменных окружения:
- `APP_QR_HMAC_SECRET`
- `APP_AUTH_COOKIE_SECRET`

Пример (PowerShell):
```
$env:APP_QR_HMAC_SECRET="CHANGE_ME_DEV_SECRET"
$env:APP_AUTH_COOKIE_SECRET="CHANGE_ME_AUTH_SECRET"
```
**Важно:** без этих переменных backend падает при старте (ошибка `app.qr.hmacSecret must be set` / `app.auth.cookieSecret must be set`).

После этого запускайте backend:
```
./gradlew bootRun
```

### 7.1.2 Retention (хранение/удаление гостевых данных)
Переменные окружения:
```
$env:APP_RETENTION_ENABLED="true"
$env:APP_RETENTION_GUEST_SESSIONS_DAYS="90"
$env:APP_RETENTION_GUEST_CONSENTS_DAYS="365"
$env:APP_RETENTION_GUEST_PROFILES_DAYS="365"
$env:APP_RETENTION_ORDER_GUEST_PHONE_DAYS="365"
$env:APP_RETENTION_CRON="0 0 3 * * *"
```
По умолчанию включено: удаляются старые гостевые сессии/логи согласий/профили, а в старых заказах очищается `guest_phone`.

### 7.2 OTP (SMS / WhatsApp / Telegram)
Вход гостя поддерживает выбор канала OTP: **SMS / WhatsApp / Telegram**.

В текущей версии используется **mock‑провайдер**, реальные сообщения не отправляются.
Канал указывается в запросе `/api/public/otp/send` (параметр `channel`), а в UI гостя
это выбор перед вводом телефона.

---

## 8) Первичная проверка (минимальный тест)

### 8.1 Создать таблицу
1. Зайдите в admin: `http://localhost:3000/admin`
2. Введите логин admin (если уже создан).
3. Создайте стол (Tables).
4. Скопируйте QR URL.

### 8.2 Открыть меню гостя
1. Вставьте QR URL в браузер.
2. Должно открыться меню гостя.

### 8.3 Сделать заказ
1. Добавьте блюдо в корзину.
2. Оформите заказ.

### 8.4 Проверить staff‑app
1. Зайдите в staff‑app.
2. Должен появиться новый заказ.

### 8.5 Flutter analyze (staff‑app)
Если установлен Flutter:
```
cd staff-app
flutter analyze
```

---

## 9) Возможные проблемы

### Backend не стартует
- Проверьте PostgreSQL и параметры доступа в `application.yml`.
- Проверьте порт 8080.

### Guest web не открывается
- Проверьте, что `npm run dev` не выдал ошибок.
- Проверьте порт 3000.

### Не видны данные
- Проверьте, что backend и web запущены одновременно.

---

## 10) Запуск на хостинге (кратко)

Минимальный вариант:
- VPS (Ubuntu) + Docker
- PostgreSQL в контейнере
- Backend + Guest web отдельно

Подробный гайд: `RUN_HOSTING.md`.

---

## 11) Запуск через Docker (локально)

> Удобно, если не хотите вручную ставить PostgreSQL и настраивать окружение.

### 11.1 Установите Docker Desktop
Скачать: https://www.docker.com/products/docker-desktop/

### 11.2 Подготовьте .env
В корне проекта:
```
cp .env.example .env
```
Откройте `.env` и при необходимости измените порты и `APP_QR_HMAC_SECRET`.

### 11.3 Запуск
```
docker compose -f infra/docker-compose.full.yml --env-file .env up -d --build
```

Ожидаемо:
- Backend: `http://localhost:8080`
- Web: `http://localhost:3000`

### 11.4 Остановить
```
docker compose -f infra/docker-compose.full.yml down
```
