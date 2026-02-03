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

## Мониторинг и алерты (Prometheus)
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

## 8) Полезные команды

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

---

## SLA timestamps (orders)
В базе фиксируются временные метки статусов заказов:
`accepted_at`, `in_progress_at`, `ready_at`, `served_at`, `closed_at`, `cancelled_at`.
Они используются для расчёта SLA в мотивации официантов.
```

---

## 9) Staff‑app (Flutter) — деплой/доставка

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

## 10) Рекомендации по бэкапам
- Делайте ежедневный cron‑бэкап `pg_dump`.\n- Храните минимум 7‑14 дней.\n- Проверяйте восстановление раз в неделю.

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

## 10.1) Ротация логов контейнеров
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

## 11) Проверка, что .env применяется

После запуска контейнеров:
```
docker compose -f infra/docker-compose.full.yml --env-file .env exec backend printenv | grep APP_
```

Если видите ваши значения (особенно `APP_QR_HMAC_SECRET`), значит `.env` подхватился.

---

## 12) Rate limit через Redis (опционально)

По умолчанию лимиты работают **в памяти** (подходит для одного сервера).  
Если вы планируете несколько инстансов — включите Redis.

### 12.1 В `.env`
```
RATE_LIMIT_REDIS_ENABLED=true
RATE_LIMIT_REDIS_HOST=redis
RATE_LIMIT_REDIS_PORT=6379
RATE_LIMIT_REDIS_PASSWORD=
```

### 12.2 Запуск с Redis
```
docker compose -f infra/docker-compose.full.yml --env-file .env --profile redis up -d --build
```
