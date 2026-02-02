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
- `https://YOUR_DOMAIN/api/admin/me` — API

---

## 8) Полезные команды

```
# Логи
 docker compose -f infra/docker-compose.full.yml logs -f

# Обновить код
 git pull
 docker compose -f infra/docker-compose.full.yml --env-file .env up -d --build

# Остановить
 docker compose -f infra/docker-compose.full.yml down
```
