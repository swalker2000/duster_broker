# Работа с сервисом через REST API

Брокер принимает и отдаёт сообщения не только по MQTT, но и по HTTP REST. Логика та же: producer ставит сообщение в очередь для `{deviceId}`, consumer забирает его опросом и подтверждает статус. Можно смешивать транспорты (отправить по REST, получить по MQTT и наоборот).

Интерактивная спецификация: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
Авторизация в Swagger: [SWAGGER_AUTH_RUS.md](../swagger/SWAGGER_AUTH_RUS.md)

## Базовый URL и заголовки

| Параметр | Значение |
|----------|----------|
| Базовый URL | `http://localhost:8080` (порт по умолчанию Spring Boot) |
| Привязка адреса | `server.address` / `REST_SERVER_ADDRESS` (по умолчанию `127.0.0.1`; для доступа извне Docker — `0.0.0.0`) |
| Content-Type | `application/json` |
| Accept | `application/json` |
| Авторизация | `Authorization: Bearer <accessToken>` |

В path-параметрах `{deviceId}` при спецсимволах используйте URL-encoding.

## Авторизация

API защищён JWT (HS256, stateless). CSRF отключён.

1. Получите токен: `POST /auth/login`.
2. Передавайте его во всех защищённых запросах в заголовке `Authorization: Bearer …`.
3. Срок жизни токена — `app.jwt.expiration-ms` (по умолчанию 24 часа).

Дефолтный админ (роль `MAN`), если пользователей с этой ролью ещё нет: `admin` / `admin`.

### Роли

| Роль | Назначение |
|------|------------|
| `DEVISE` | устройства: `/producer/**`, `/consumer/**` |
| `MAN` | админ API `/admin/api/**` + всё, что доступно `DEVISE` |

### Правила доступа

| Путь | Доступ |
|------|--------|
| `/auth/login`, `/auth/isClientEnabled`, `/` | без токена |
| `/swagger-ui/**`, `/v3/api-docs/**` | без токена |
| `/producer/**`, `/consumer/**` | любой валидный JWT |
| `/admin/api/**` | JWT с ролью `MAN` |
| `/auth/me` | любой валидный JWT |

Ошибки: `401` → `{"error":"Unauthorized"}`, `403` → `{"error":"Forbidden"}`.

В коде идентификатор клиента везде называется **`deviseId`** (опечатка относительно `deviceId`). В path получателя сообщения — `{deviceId}`.

---

## Перечисления

### DeliveryGuarantee (`believerGuarantee`)

| Значение | Смысл |
|----------|--------|
| `NO` | подтверждение от consumer не ждём; статус доставки ставится сразу после отправки |
| `RECEIPT_CONFIRMATION` | ждём ACK от consumer (значение по умолчанию) |
| `ONLY_LAST` | как `RECEIPT_CONFIRMATION`, плюс недоставленные сообщения с тем же `command` для устройства помечаются отменёнными |

### DeliveryStatus

| Значение | Кто выставляет | Смысл |
|----------|----------------|--------|
| `NOT_DELIVERED` | брокер | ещё не доставлено |
| `DELIVERED` | consumer (или брокер при `NO`) | получено устройством |
| `COMPLETED` | consumer | команда выполнена |
| `COMPLETED_WITH_ERROR` | consumer | команда с ошибкой |
| `CANCELLED` | брокер | отменено (например, из‑за `ONLY_LAST`) |
| `UNKNOWN` | брокер | статус неизвестен |

От consumer принимаются только: `DELIVERED`, `COMPLETED`, `COMPLETED_WITH_ERROR`. Если в ACK нет `deliveryStatus`, считается `DELIVERED`.

---

## Auth API

### `POST /auth/login` — получить JWT

Тело:

```json
{
  "deviseId": "admin",
  "password": "admin"
}
```

Ответ:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "deviseId": "admin",
  "role": "MAN"
}
```

### `GET /auth/me` — текущая сессия

Заголовок: `Authorization: Bearer <token>`.

```json
{
  "deviseId": "admin",
  "role": "MAN"
}
```

### `POST /auth/isClientEnabled` — проверка учётных данных (без токена)

Тело (поля `username` / `password`):

```json
{
  "username": "sensor-1",
  "password": "secret"
}
```

Ответ: `{ "decision": true }` или `{ "decision": false }`.

---

## Типовой сценарий (producer → consumer)

Ниже — аналог MQTT-алгоритма из README, но через REST.

### 1. Логин

```http
POST /auth/login
Content-Type: application/json

{"deviseId":"producer-1","password":"..."}
```

Сохраните `accessToken`. То же для consumer (или один токен с ролью `MAN`).

### 2. Producer отправляет сообщение

```http
PUT /producer/request/{deviceId}
Authorization: Bearer <token>
Content-Type: application/json
```

`{deviceId}` — id **получателя** (consumer).

Тело (`ProducerMessageInDto`):

```json
{
  "believerGuarantee": "RECEIPT_CONFIRMATION",
  "command": "digitalWrite",
  "data": {
    "pinNumber": 13,
    "pinValue": true
  }
}
```

С подпиской на статусы добавьте `messageBirthCertificate`:

```json
{
  "believerGuarantee": "RECEIPT_CONFIRMATION",
  "command": "blink",
  "messageBirthCertificate": {
    "tmpId": 3,
    "producerDeviseId": "0"
  },
  "data": {
    "pinNumber": 13,
    "period": 1000,
    "count": 5
  }
}
```

- `tmpId` — временный id на стороне producer (уникальность брокер не проверяет).
- `producerDeviseId` — id отправителя; для монолита часто `"0"`.

Ответ (`ProducerMessageOutDto`):

```json
{
  "id": 2,
  "tmpId": 3,
  "deliveryStatus": "NOT_DELIVERED"
}
```

`id` — постоянный id сообщения в БД.

### 3. Consumer забирает сообщение (polling)

```http
GET /consumer/getLastMessage/{deviceId}
Authorization: Bearer <token>
```

Ответ — массив из 0 или 1 элемента (самое старое недоставленное):

```json
[
  {
    "id": 2,
    "currentTimestamp": 1772021717684,
    "command": "digitalWrite",
    "believerGuarantee": "RECEIPT_CONFIRMATION",
    "data": {
      "pinNumber": 13,
      "pinValue": true
    }
  }
]
```

Пустой массив `[]` — сообщений нет. Опрашивайте с разумным интервалом (как в тестовом клиенте ~200 ms или реже).

### 4. Consumer подтверждает статус

```http
POST /consumer/request/{deviceId}
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "id": 2,
  "deliveryStatus": "DELIVERED"
}
```

Минимальный ACK (статус станет `DELIVERED`):

```json
{
  "id": 2
}
```

Позже можно отправить `COMPLETED` или `COMPLETED_WITH_ERROR` с тем же `id`.

### 5. Producer проверяет статус

```http
GET /producer/getMessageStatus/{messageId}
Authorization: Bearer <token>
```

```json
{
  "deliveryStatus": "DELIVERED"
}
```

`404` — сообщение не найдено.

При MQTT статусы приходят в топик `producer/response/...`; по REST их нужно опрашивать этим методом (см. `ProducerRest` в тестах).

---

## Producer API

| Метод | Путь | Описание |
|-------|------|----------|
| `PUT` | `/producer/request/{deviceId}` | создать сообщение для consumer |
| `GET` | `/producer/getMessageStatus/{messageId}` | текущий `deliveryStatus` |

Нужен любой валидный JWT.

---

## Consumer API

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/consumer/getLastMessage/{deviceId}` | самое старое недоставленное сообщение (список 0..1) |
| `POST` | `/consumer/request/{deviceId}` | ACK / смена статуса |

Нужен любой валидный JWT.

---

## Admin API

Все методы требуют JWT с ролью **`MAN`**.

### Клиенты — `/admin/api/clients`

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/admin/api/clients` | список клиентов |
| `GET` | `/admin/api/clients/{id}` | один клиент (`404` если нет) |
| `POST` | `/admin/api/clients` | создать (`201`); нужны `deviseId`, `password`; дубликат → `409` |
| `PUT` | `/admin/api/clients/{id}` | обновить; пустой `password` — пароль не менять |
| `DELETE` | `/admin/api/clients/{id}` | удалить (`204`) |

Тело клиента:

```json
{
  "deviseId": "relay-kitchen",
  "password": "secret",
  "role": "DEVISE",
  "description": "Реле на кухне"
}
```

Поле `password` в ответах не отдаётся (write-only). Пароль хранится как bcrypt.

### Сообщения — `/admin/api/messages`

```http
GET /admin/api/messages?deviseId=relay-kitchen&limit=200
```

| Query | Описание |
|-------|----------|
| `deviseId` | опциональный фильтр по устройству |
| `limit` | размер выборки, по умолчанию `200`, максимум `500` |

Элемент ответа (`AdminMessageOutDto`): `id`, `deviseId`, `command`, `deliveryStatus`, `deliveryGuarantee`, `createdDate`, `deliveredDate`, `tmpId`, `producerDeviseId`, `deliveredError`, `data`.

### Сохранённые шаблоны — `/admin/api/saved-messages`

| Метод | Путь | Описание |
|-------|------|----------|
| `GET` | `/admin/api/saved-messages` | список; фильтр `clientId` или `deviseId` |
| `GET` | `/admin/api/saved-messages/{id}` | один шаблон |
| `POST` | `/admin/api/saved-messages` | создать (`201`); только для клиента с ролью `DEVISE` |
| `DELETE` | `/admin/api/saved-messages/{id}` | удалить (`204`) |
| `POST` | `/admin/api/saved-messages/{id}/send` | отправить шаблон **владельцу** устройства → `ProducerMessageOutDto` |

Создание:

```json
{
  "clientId": 5,
  "description": "Включить пин 13",
  "command": "digitalWrite",
  "deliveryGuarantee": "RECEIPT_CONFIRMATION",
  "data": {
    "pinNumber": 13,
    "pinValue": true
  }
}
```

Если `deliveryGuarantee` не указан — используется `NO`. Отправка (`…/send`) всегда идёт на `deviseId` владельца шаблона, не на произвольный адрес.

---

## Конфигурация (релевантная REST)

| Параметр / env | Назначение |
|----------------|------------|
| `REST_SERVER_ADDRESS` | адрес bind HTTP-сервера |
| `JWT_SECRET` | секрет HS256 (≥ 32 байта UTF-8); в проде обязательно свой |
| `JWT_EXPIRATION_MS` | срок JWT, мс (default `86400000`) |
| `APP_SECURITY_PERMIT_ALL` | `true` — отключить проверку auth (только для dev/тестов) |
| `COMMON_SEND_MESSAGE_PERIOD` | интервал выдачи сообщений на устройство (защита буфера IoT) |
| `COMMON_MQTT_WAIT_RESPONSE_TIMEOUT` | таймаут ожидания ответа consumer |

Веб-админка (UI): `http://localhost:8080` — те же учётные данные `admin` / `admin`.

---

## Примеры curl

Логин:

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"deviseId":"admin","password":"admin"}'
```

Отправка команды:

```bash
TOKEN=... # accessToken из ответа login

curl -s -X PUT "http://localhost:8080/producer/request/relay-kitchen" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "believerGuarantee": "RECEIPT_CONFIRMATION",
    "command": "digitalWrite",
    "data": {"pinNumber": 13, "pinValue": true}
  }'
```

Опрос consumer:

```bash
curl -s "http://localhost:8080/consumer/getLastMessage/relay-kitchen" \
  -H "Authorization: Bearer $TOKEN"
```

ACK:

```bash
curl -s -X POST "http://localhost:8080/consumer/request/relay-kitchen" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"id": 2, "deliveryStatus": "DELIVERED"}'
```

---

## Связанные материалы

- [README_RUS.md](../../README_RUS.md) — общий обзор, MQTT-алгоритм, запуск
- [SWAGGER_AUTH_RUS.md](../swagger/SWAGGER_AUTH_RUS.md) — JWT в Swagger UI
- Тестовые HTTP-клиенты: `src/test/kotlin/com/duster/pd/rest/ProducerRest.kt`, `ConsumerRest.kt`
