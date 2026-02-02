# Staff‑app API (Plans + Polling)

Короткая документация для staff‑app (Flutter). Авторизация: **Basic Auth** (username/password из staff_users).

## Базовые настройки
- API base: `http://localhost:8080`
- Заголовок авторизации: `Authorization: Basic <base64(user:pass)>`

## Планы/залы
Получить список залов:
```
GET /api/staff/halls
```
Ответ:
```
[
  { "id": 1, "branchId": 1, "name": "Main", "isActive": true, "sortOrder": 0,
    "backgroundUrl": null, "zonesJson": "[]", "activePlanId": 3 }
]
```

Получить планы зала:
```
GET /api/staff/halls/{hallId}/plans
```

Получить layout (фон + зоны):
```
GET /api/staff/branch-layout?hallId={hallId}
GET /api/staff/branch-layout?planId={planId}
```

Таблицы (с координатами/назначенным официантом):
```
GET /api/staff/tables?hallId={hallId}
```

## Поллинг статусов (Orders / Kitchen / BillRequests / Notifications)

Активные заказы:
```
GET /api/staff/orders/active?statusIn=NEW,IN_PROGRESS,READY&hallId={hallId}
```

Инкрементальные обновления (только новые):
```
GET /api/staff/orders/active/updates?since=2026-02-02T10:00:00Z&statusIn=NEW,IN_PROGRESS,READY&hallId={hallId}
```

Короткий статус по активным заказам:
```
GET /api/staff/orders/active/status?hallId={hallId}
```

Кухня (очередь):
```
GET /api/staff/orders/kitchen?hallId={hallId}
```

Счета:
```
GET /api/staff/bill-requests/active?hallId={hallId}
```

Нотификации (pull‑feed):
```
GET /api/staff/notifications/feed?sinceId={id}
```

## Примеры curl
```
curl -u waiter1:demo123 http://localhost:8080/api/staff/halls
curl -u waiter1:demo123 "http://localhost:8080/api/staff/orders/active/updates?since=2026-02-02T10:00:00Z"
```

## Примечания
- `hallId` опционален. Если не задан — возвращается всё по филиалу.
- `statusIn` — CSV список статусов.
