# Запуск на хостинге (VPS) — подробная инструкция

Эта инструкция подходит для Ubuntu 22.04+.

---

## 1) Подготовка сервера

### 1.1 Подключиться по SSH
```
ssh root@<SERVER_IP>
```

### 1.2 Установить обновления
```
apt update && apt -y upgrade
```

### 1.3 Установить Docker и Compose
```
apt -y install ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

cat <<'EOF' > /etc/apt/sources.list.d/docker.list
deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable
EOF

apt update
apt -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

Проверка:
```
docker --version
docker compose version
```

---

## 2) Загрузить проект на сервер

### Вариант A (Git)
```
cd /opt
mkdir -p virtual-waiter
cd virtual-waiter
git clone <ССЫЛКА_НА_РЕПО>
cd virtual-waiter-saas
```

### Вариант B (scp)
```
scp -r ./virtual-waiter-saas root@<SERVER_IP>:/opt/virtual-waiter/
```

---

## 3) Настроить переменные окружения

В корне проекта:
```
cp .env.example .env
nano .env
```

В `APP_QR_HMAC_SECRET` установите **сильный ключ**.

### Обязательные переменные (иначе backend не стартует)
Эти переменные должны быть заданы обязательно:
- `APP_QR_HMAC_SECRET`
- `APP_AUTH_COOKIE_SECRET`

Минимальный пример:
```
APP_QR_HMAC_SECRET=СЛОЖНЫЙ_СЕКРЕТ_32_СИМВОЛА_ИЛИ_БОЛЕЕ
APP_AUTH_COOKIE_SECRET=ДРУГОЙ_СЛОЖНЫЙ_СЕКРЕТ_32_СИМВОЛА_ИЛИ_БОЛЕЕ
```

### Важно про аутентификацию staff
- Используется `POST /api/auth/login` → httpOnly cookie, Basic Auth отключён.
- В проде обязательно ставьте:
  - `APP_AUTH_COOKIE_SECURE=true` (только HTTPS)
  - `APP_AUTH_COOKIE_SECRET` (сложный ключ, ≥32 символов)

Пример логина (curl):
```
curl -i -X POST https://YOUR_DOMAIN/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"demo123"}'
```
Для запросов после логина передайте cookie из ответа:
```
curl -i https://YOUR_DOMAIN/api/admin/branch \
  -H "Cookie: vw_auth=PASTE_TOKEN"
```
Пример выхода (curl):
```
curl -i -X POST https://YOUR_DOMAIN/api/auth/logout \
  -H "Cookie: vw_auth=PASTE_TOKEN"
```

> Важно для домена:
> - `APP_PUBLIC_BASE_URL=https://YOUR_DOMAIN`
> - `NEXT_PUBLIC_API_BASE=https://YOUR_DOMAIN/api`

### Минимальный список переменных (для новичков)
Скопируйте и заполните по образцу:
```
# База данных
POSTGRES_DB=vw
POSTGRES_USER=vw
POSTGRES_PASSWORD=vw
POSTGRES_PORT=5432

# URL базы для backend (если всё в Docker)
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/vw
SPRING_DATASOURCE_USERNAME=vw
SPRING_DATASOURCE_PASSWORD=vw

# Порты
BACKEND_PORT=8080
WEB_PORT=3000

# Адреса приложения
APP_PUBLIC_BASE_URL=https://YOUR_DOMAIN
NEXT_PUBLIC_API_BASE=https://YOUR_DOMAIN/api

# Безопасность (обязательно!)
APP_QR_HMAC_SECRET=СЛОЖНЫЙ_СЕКРЕТ_32_СИМВОЛА_ИЛИ_БОЛЕЕ
APP_AUTH_COOKIE_SECRET=ДРУГОЙ_СЛОЖНЫЙ_СЕКРЕТ_32_СИМВОЛА_ИЛИ_БОЛЕЕ
APP_AUTH_COOKIE_SECURE=true

# Push (если не используете FCM — оставьте пустым)
FCM_SERVER_KEY=
```

### SLA‑алерты (push)
По умолчанию включены. При необходимости можно настроить:
```
APP_SLAALERTS_ENABLED=true
APP_SLAALERTS_POLLMS=60000
APP_SLAALERTS_ORDERCRITMIN=10
APP_SLAALERTS_CALLCRITMIN=5
APP_SLAALERTS_BILLCRITMIN=10
APP_SLAALERTS_KITCHENCRITMIN=15
APP_SLAALERTS_COOLDOWNMINUTES=5
APP_SLAALERTS_LOOKBACKMINUTES=240
```
Тексты алертов локализуются по `branch_settings.default_lang` (ru/ro/en).

### Онлайн‑оплата (MAIB/Paynet/MIA)
1) В админке включите **Online payments enabled** и выберите провайдера (MAIB/Paynet/MIA).
2) Укажите валюту онлайн‑оплаты (обычно совпадает с валютой филиала).
3) В личном кабинете провайдера настройте webhook:
```
https://YOUR_DOMAIN/api/public/payments/webhook/{provider}
```
где `{provider}` = `maib` | `paynet` | `mia`.

Дополнительно включите проверку подписи webhook:
```
X-Signature: <HMAC-SHA256 hex>
```
Сигнатура считается от **сырого тела запроса** и общего секрета провайдера.
Переменные окружения:
- `APP_PAYMENTS_MAIB_WEBHOOK_SECRET`
- `APP_PAYMENTS_PAYNET_WEBHOOK_SECRET`
- `APP_PAYMENTS_MIA_WEBHOOK_SECRET`

> Примечание: реальные интеграции требуют ключей/подписей от провайдера.
> В текущем MVP провайдеры подключены как заглушки и принимают JSON вида:
> `{"providerRef":"...","status":"PAID","amountCents":12345,"currencyCode":"MDL"}`.

---

## 4) Запуск контейнеров

```
docker compose -f infra/docker-compose.full.yml --env-file .env up -d --build
```

Проверка:
```
docker compose -f infra/docker-compose.full.yml ps
```

---

## 5) Настроить Nginx (обязателен для домена и SSL)

### 5.1 Установить Nginx
```
apt -y install nginx
```

### 5.2 Конфиг прокси
Создайте файл:
```
cat <<'EOF' > /etc/nginx/sites-available/virtual-waiter
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
EOF
```

Активируйте:
```
ln -s /etc/nginx/sites-available/virtual-waiter /etc/nginx/sites-enabled/
nginx -t
systemctl restart nginx
```

---

## 6) SSL (Let's Encrypt)

```
apt -y install certbot python3-certbot-nginx
certbot --nginx -d YOUR_DOMAIN
```

---

## 7) Проверка

- `https://YOUR_DOMAIN` — Guest web
- `https://YOUR_DOMAIN/admin` — Admin web
- `https://YOUR_DOMAIN/superadmin` — Super Admin web
- `https://YOUR_DOMAIN/api/admin/me` — API
- В staff‑app есть вкладка **Профиль** (просмотр имени/фото/данных).

---

## 8) Мониторинг и алерты (Prometheus)
Включён эндпойнт: `https://YOUR_DOMAIN/api/actuator/prometheus` (если проксируете `/api` на backend).

Пример scrape‑настройки:
```yaml
scrape_configs:
  - job_name: virtual-waiter
    metrics_path: /api/actuator/prometheus
    static_configs:
      - targets: ["YOUR_DOMAIN:443"]
```

Минимальные алерты (ориентиры):
- `up == 0` — backend недоступен
- `http_server_requests_seconds_count{status=~"5.."} > 0` — ошибки 5xx
- `jvm_memory_used_bytes / jvm_memory_max_bytes > 0.9` — высокий расход памяти

---

## 9) Полезные команды

```
# Логи
docker compose -f infra/docker-compose.full.yml logs -f

# Бэкап базы (PostgreSQL)
docker exec -t <POSTGRES_CONTAINER> pg_dump -U ${POSTGRES_USER} ${POSTGRES_DB} > backup.sql

# Восстановление базы
cat backup.sql | docker exec -i <POSTGRES_CONTAINER> psql -U ${POSTGRES_USER} ${POSTGRES_DB}

# Обновить код
git pull
docker compose -f infra/docker-compose.full.yml --env-file .env up -d --build

# Остановить
docker compose -f infra/docker-compose.full.yml down
```

---

## 9.1) SLA timestamps (orders)
В базе фиксируются временные метки статусов заказов:
`accepted_at`, `in_progress_at`, `ready_at`, `served_at`, `closed_at`, `cancelled_at`.
Они используются для расчёта SLA в мотивации официантов.

---

## 10) Staff‑app (Flutter) — деплой/доставка

### Вариант A — Web (самый простой)
1) На локальной машине:
```
cd staff-app
flutter build web --dart-define=API_BASE=https://YOUR_DOMAIN
```
2) Результат: `staff-app/build/web`
3) Загрузите содержимое на сервер (например, в `/var/www/staff-app/`)

### Вариант B — Android APK
1) На локальной машине:
```
cd staff-app
flutter build apk --dart-define=API_BASE=https://YOUR_DOMAIN
```
2) APK будет в `staff-app/build/app/outputs/flutter-apk/app-release.apk`
3) Раздайте APK персоналу

---

## 11) Рекомендации по бэкапам
- Делайте ежедневный cron‑бэкап `pg_dump`.
- Храните минимум 7‑14 дней.
- Проверяйте восстановление раз в неделю.

### Пример cron + ротация
1) Создайте папку:
```
mkdir -p /opt/backup/virtual-waiter
```

2) Создайте скрипт `/usr/local/bin/vw-backup.sh`:
```
#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="/opt/backup/virtual-waiter"
TS="$(date +%Y-%m-%d_%H-%M)"

mkdir -p "$BACKUP_DIR"

# Имя контейнера postgres можно посмотреть: docker compose -f infra/docker-compose.full.yml ps
POSTGRES_CONTAINER="virtual-waiter-postgres-1"

docker exec -t "$POSTGRES_CONTAINER" pg_dump -U "${POSTGRES_USER}" "${POSTGRES_DB}" > "${BACKUP_DIR}/vw_${TS}.sql"

# Ротация: хранить последние 14 файлов
ls -1t "${BACKUP_DIR}"/vw_*.sql | tail -n +15 | xargs -r rm -f
```

3) Сделайте исполняемым:
```
chmod +x /usr/local/bin/vw-backup.sh
```

4) Добавьте cron (ежедневно в 02:00):
```
crontab -e
```
И вставьте:
```
0 2 * * * POSTGRES_USER=vw POSTGRES_DB=vw /usr/local/bin/vw-backup.sh >> /var/log/vw-backup.log 2>&1
```

### Проверка бэкапа
- Убедитесь, что в `/opt/backup/virtual-waiter/` появились новые файлы.
- Раз в неделю пробуйте восстановление на тестовом сервере.

---

## 11.1) Ротация логов контейнеров
Чтобы логи не заполнили диск, в `infra/docker-compose.full.yml` включена ротация:
```
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "5"
```
Это значит, что на каждый контейнер хранится максимум ~50MB логов.

---

## 12) Проверка, что .env применяется

После запуска контейнеров:
```
docker compose -f infra/docker-compose.full.yml --env-file .env exec backend printenv | grep APP_
```

Если видите ваши значения (особенно `APP_QR_HMAC_SECRET`), значит `.env` подхватился.

---

## 13) Rate limit через Redis (опционально)

По умолчанию лимиты работают **в памяти** (подходит для одного сервера).  
Если вы планируете несколько инстансов — включите Redis.

### 13.1 В `.env`
```
RATE_LIMIT_REDIS_ENABLED=true
RATE_LIMIT_REDIS_HOST=redis
RATE_LIMIT_REDIS_PORT=6379
RATE_LIMIT_REDIS_PASSWORD=
```

### 13.2 Запуск с Redis
```
docker compose -f infra/docker-compose.full.yml --env-file .env --profile redis up -d --build
```
