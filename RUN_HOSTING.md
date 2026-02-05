# Запуск на хостинге (VPS) — инструкция для новичков

Инструкция проверена для **Ubuntu 22.04+**.

---

## 0) Что вы получите
После выполнения шагов:
- сайт гостя: `https://YOUR_DOMAIN/`
- админка: `https://YOUR_DOMAIN/admin`
- супер‑админка: `https://YOUR_DOMAIN/superadmin`
- API: `https://YOUR_DOMAIN/api/...`

### Роли и доступы (кратко)
- `SUPER_ADMIN` — полный доступ ко всем заведениям и филиалам.
- `OWNER` — владелец ресторана (admin‑like для своих филиалов).
- `ADMIN` — админ своих филиалов.
- `MANAGER` — менеджер одного филиала.
- `WAITER`, `HOST`, `KITCHEN`, `BAR` — персонал филиала.

---

## 1) Подключиться к серверу

```bash
ssh root@<SERVER_IP>
```

Обновить систему:
```bash
apt update && apt -y upgrade
```

---

## 2) Установить Docker и Compose

```bash
apt -y install ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

cat <<'EOF2' > /etc/apt/sources.list.d/docker.list
deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable
EOF2

apt update
apt -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

Проверка:
```bash
docker --version
docker compose version
```

---

## 3) Загрузить проект на сервер

### Вариант A — Git
```bash
cd /opt
mkdir -p virtual-waiter
cd virtual-waiter
git clone <ССЫЛКА_НА_РЕПО>
cd virtual-waiter-saas
```

### Вариант B — scp
```bash
scp -r ./virtual-waiter-saas root@<SERVER_IP>:/opt/virtual-waiter/
```

---

## 4) Настроить переменные окружения

В корне проекта:
```bash
cp .env.example .env
nano .env
```

### Обязательные переменные (иначе backend не стартует)
```env
APP_QR_HMAC_SECRET=СЕКРЕТ_32_СИМВОЛА_ИЛИ_БОЛЕЕ
APP_AUTH_COOKIE_SECRET=ДРУГОЙ_СЕКРЕТ_32_СИМВОЛА_ИЛИ_БОЛЕЕ
```
**Важно:** без этих переменных backend завершится с ошибкой (`app.qr.hmacSecret must be set` / `app.auth.cookieSecret must be set`).

### Минимальный набор для запуска
```env
# База
POSTGRES_DB=vw
POSTGRES_USER=vw
POSTGRES_PASSWORD=vw
POSTGRES_PORT=5432

# Backend
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/vw
SPRING_DATASOURCE_USERNAME=vw
SPRING_DATASOURCE_PASSWORD=vw

# Порты
BACKEND_PORT=8080
WEB_PORT=3000

# Домен
APP_PUBLIC_BASE_URL=https://YOUR_DOMAIN
NEXT_PUBLIC_API_BASE=https://YOUR_DOMAIN/api

# Секреты
APP_QR_HMAC_SECRET=СЕКРЕТ_32_СИМВОЛА_ИЛИ_БОЛЕЕ
APP_AUTH_COOKIE_SECRET=ДРУГОЙ_СЕКРЕТ_32_СИМВОЛА_ИЛИ_БОЛЕЕ
APP_AUTH_COOKIE_SECURE=true

# Медиа (загрузка изображений)
APP_MEDIA_PUBLIC_BASE_URL=https://YOUR_DOMAIN
```

### Retention (хранение/удаление гостевых данных)
```env
# включить/выключить фоновые чистки
APP_RETENTION_ENABLED=true
# когда удалять гостевые сессии
APP_RETENTION_GUEST_SESSIONS_DAYS=90
# когда удалять логи согласий
APP_RETENTION_GUEST_CONSENTS_DAYS=365
# когда удалять профили гостей (по last_visit_at)
APP_RETENTION_GUEST_PROFILES_DAYS=365
# когда обезличивать телефон в заказах
APP_RETENTION_ORDER_GUEST_PHONE_DAYS=365
# расписание (cron) — по умолчанию каждый день 03:00
APP_RETENTION_CRON=0 0 3 * * *
```
По умолчанию включено: удаляются старые гостевые сессии/логи согласий/профили, а в старых заказах **очищается guest_phone**.

---

## 5) Запуск контейнеров

```bash
docker compose -f infra/docker-compose.full.yml --env-file .env up -d --build
```

Проверка:
```bash
docker compose -f infra/docker-compose.full.yml ps
```

### Хранение изображений
Файлы загружаются в `/data/uploads` внутри контейнера `backend`. В `docker-compose.full.yml` это volume `vw_uploads`, поэтому файлы сохраняются между перезапусками.

---

## 6) Настроить Nginx (обязателен для домена)

Установка:
```bash
apt -y install nginx
```

Конфиг:
```bash
cat <<'EOF2' > /etc/nginx/sites-available/virtual-waiter
server {
    listen 80;
    server_name YOUR_DOMAIN;

    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
EOF2
```

Активировать:
```bash
ln -s /etc/nginx/sites-available/virtual-waiter /etc/nginx/sites-enabled/
nginx -t
systemctl restart nginx
```

---

## 7) SSL (Let’s Encrypt)

```bash
apt -y install certbot python3-certbot-nginx
certbot --nginx -d YOUR_DOMAIN
```

---

## 8) Проверка

- `https://YOUR_DOMAIN` — Guest Web
- `https://YOUR_DOMAIN/admin` — Admin
- `https://YOUR_DOMAIN/superadmin` — Super Admin
- `https://YOUR_DOMAIN/api/actuator/health` — healthcheck

---

## 9) Важно про аутентификацию staff
- Логин: `POST /api/auth/login` (устанавливает httpOnly cookie)
- Basic Auth **не используется**.

Пример:
```bash
curl -i -X POST https://YOUR_DOMAIN/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"demo123"}'
```
Далее:
```bash
curl -i https://YOUR_DOMAIN/api/admin/branch \
  -H "Cookie: vw_auth=PASTE_TOKEN"
```

---

## 9.1) OTP для гостей (SMS / WhatsApp / Telegram)

Сервис OTP поддерживает каналы:
- `SMS` (по умолчанию)
- `WHATSAPP`
- `TELEGRAM`

Канал выбирается на стороне гостевого UI при вводе телефона.
В dev используется mock‑провайдер, в проде подключите реальный канал.

Пример запроса OTP:
```bash
curl -i -X POST https://YOUR_DOMAIN/api/public/otp/send \
  -H "Content-Type: application/json" \
  -H "X-Session-Secret: <SESSION_SECRET>" \
  -d '{
    "guestSessionId": 123,
    "phoneE164": "+37369000000",
    "locale": "ru",
    "channel": "WHATSAPP"
  }'
```

Если канал не указан или невалиден — используется `SMS`.

---

## 10) Бэкапы базы (очень желательно)

### Скрипт
```bash
mkdir -p /opt/backup/virtual-waiter

cat <<'EOF2' > /usr/local/bin/vw-backup.sh
#!/usr/bin/env bash
set -euo pipefail
BACKUP_DIR="/opt/backup/virtual-waiter"
TS="$(date +%Y-%m-%d_%H-%M)"
POSTGRES_CONTAINER="virtual-waiter-postgres-1"

mkdir -p "$BACKUP_DIR"
docker exec -t "$POSTGRES_CONTAINER" pg_dump -U "${POSTGRES_USER}" "${POSTGRES_DB}" > "$BACKUP_DIR/vw_${TS}.sql"

# Ротация: хранить последние 14 файлов
ls -1t "$BACKUP_DIR"/vw_*.sql | tail -n +15 | xargs -r rm -f
EOF2

chmod +x /usr/local/bin/vw-backup.sh
```

### Cron (ежедневно 02:00)
```bash
crontab -e
```
Добавьте:
```bash
0 2 * * * POSTGRES_USER=vw POSTGRES_DB=vw /usr/local/bin/vw-backup.sh >> /var/log/vw-backup.log 2>&1
```

---

## 11) Полезные команды

```bash
# Логи
docker compose -f infra/docker-compose.full.yml logs -f

# Обновить код
git pull
docker compose -f infra/docker-compose.full.yml --env-file .env up -d --build

# Остановить
 docker compose -f infra/docker-compose.full.yml down
```

---

## 12) Частые проблемы

**Backend не стартует:**
- Проверьте `APP_QR_HMAC_SECRET` и `APP_AUTH_COOKIE_SECRET`.

**Админка не логинится:**
- Используйте `/api/auth/login` и cookie `vw_auth`.

**Нет меню / пустой стол:**
- Убедитесь, что миграции применились, и в базе есть демо‑данные.

---

Если хотите локальный запуск на Windows — смотрите `RUN_WINDOWS.md`.
