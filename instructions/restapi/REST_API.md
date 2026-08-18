# Using the service via REST API

The broker accepts and delivers messages not only over MQTT but also over HTTP REST. The logic is the same: a producer enqueues a message for `{deviceId}`, a consumer polls for it and acknowledges status. You can mix transports (send via REST, receive via MQTT, and vice versa).

Interactive spec: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
Swagger authorization: [SWAGGER_AUTH.md](../swagger/SWAGGER_AUTH.md)

## Base URL and headers

| Parameter | Value |
|-----------|-------|
| Base URL | `http://localhost:8080` (Spring Boot default port) |
| Bind address | `server.address` / `REST_SERVER_ADDRESS` (default `127.0.0.1`; for access from outside Docker use `0.0.0.0`) |
| Content-Type | `application/json` |
| Accept | `application/json` |
| Authorization | `Authorization: Bearer <accessToken>` |

URL-encode `{deviceId}` path parameters when they contain special characters.

## Authentication

The API is protected with JWT (HS256, stateless). CSRF is disabled.

1. Obtain a token: `POST /auth/login`.
2. Send it on every protected request in the `Authorization: Bearer …` header.
3. Token lifetime is `app.jwt.expiration-ms` (default 24 hours).

Default admin (role `MAN`), created at startup if no `MAN` users exist yet: `admin` / `admin`.

### Roles

| Role | Purpose |
|------|---------|
| `DEVISE` | devices: `/producer/**`, `/consumer/**` |
| `MAN` | admin API `/admin/api/**` plus everything available to `DEVISE` |

### Access rules

| Path | Access |
|------|--------|
| `/auth/login`, `/auth/isClientEnabled`, `/` | no token |
| `/swagger-ui/**`, `/v3/api-docs/**` | no token |
| `/producer/**`, `/consumer/**` | any valid JWT |
| `/admin/api/**` | JWT with role `MAN` |
| `/auth/me` | any valid JWT |

Errors: `401` → `{"error":"Unauthorized"}`, `403` → `{"error":"Forbidden"}`.

In the code, the client identifier is always named **`deviseId`** (a misspelling of `deviceId`). The message recipient path parameter is `{deviceId}`.

---

## Enumerations

### DeliveryGuarantee (`believerGuarantee`)

| Value | Meaning |
|-------|---------|
| `NO` | do not wait for consumer confirmation; delivery status is set right after send |
| `RECEIPT_CONFIRMATION` | wait for consumer ACK (default) |
| `ONLY_LAST` | same as `RECEIPT_CONFIRMATION`, plus undelivered messages with the same `command` for that device are marked cancelled |

### DeliveryStatus

| Value | Set by | Meaning |
|-------|--------|---------|
| `NOT_DELIVERED` | broker | not delivered yet |
| `DELIVERED` | consumer (or broker when `NO`) | received by the device |
| `COMPLETED` | consumer | command completed |
| `COMPLETED_WITH_ERROR` | consumer | command failed or completed with error |
| `CANCELLED` | broker | cancelled (e.g. due to `ONLY_LAST`) |
| `UNKNOWN` | broker | status unknown |

Only these statuses are accepted from the consumer: `DELIVERED`, `COMPLETED`, `COMPLETED_WITH_ERROR`. If the ACK has no `deliveryStatus`, it is treated as `DELIVERED`.

---

## Auth API

### `POST /auth/login` — obtain JWT

Body:

```json
{
  "deviseId": "admin",
  "password": "admin"
}
```

Response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "deviseId": "admin",
  "role": "MAN"
}
```

### `GET /auth/me` — current session

Header: `Authorization: Bearer <token>`.

```json
{
  "deviseId": "admin",
  "role": "MAN"
}
```

### `POST /auth/isClientEnabled` — check credentials (no token)

Body (`username` / `password` fields):

```json
{
  "username": "sensor-1",
  "password": "secret"
}
```

Response: `{ "decision": true }` or `{ "decision": false }`.

---

## Typical flow (producer → consumer)

Below is the REST equivalent of the MQTT flow in [MQTT_API.md](../mqtt/MQTT_API.md).

### 1. Login

```http
POST /auth/login
Content-Type: application/json

{"deviseId":"producer-1","password":"..."}
```

Save `accessToken`. Do the same for the consumer (or use a single token with role `MAN`).

### 2. Producer sends a message

```http
PUT /producer/request/{deviceId}
Authorization: Bearer <token>
Content-Type: application/json
```

`{deviceId}` is the **recipient** (consumer) id.

Body (`ProducerMessageInDto`):

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

To subscribe to status updates, add `messageBirthCertificate`:

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

- `tmpId` — temporary id on the producer side (the broker does not check uniqueness).
- `producerDeviseId` — sender id; for a monolith this is often `"0"`.

Response (`ProducerMessageOutDto`):

```json
{
  "id": 2,
  "tmpId": 3,
  "deliveryStatus": "NOT_DELIVERED"
}
```

`id` is the permanent message id in the database.

### 3. Consumer fetches a message (polling)

```http
GET /consumer/getLastMessage/{deviceId}
Authorization: Bearer <token>
```

Response — an array of 0 or 1 element (the oldest undelivered message):

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

An empty array `[]` means there are no messages. Poll at a reasonable interval (as in the test client ~200 ms or less often).

### 4. Consumer acknowledges status

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

Minimal ACK (status becomes `DELIVERED`):

```json
{
  "id": 2
}
```

Later you can send `COMPLETED` or `COMPLETED_WITH_ERROR` with the same `id`.

### 5. Producer checks status

```http
GET /producer/getMessageStatus/{messageId}
Authorization: Bearer <token>
```

```json
{
  "deliveryStatus": "DELIVERED"
}
```

`404` — message not found.

With MQTT, statuses arrive on the `producer/response/...` topic; over REST you must poll this endpoint (see `ProducerRest` in tests).

---

## Producer API

| Method | Path | Description |
|--------|------|-------------|
| `PUT` | `/producer/request/{deviceId}` | create a message for the consumer |
| `GET` | `/producer/getMessageStatus/{messageId}` | current `deliveryStatus` |

Requires any valid JWT.

---

## Consumer API

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/consumer/getLastMessage/{deviceId}` | oldest undelivered message (list of 0..1) |
| `POST` | `/consumer/request/{deviceId}` | ACK / status update |

Requires any valid JWT.

---

## Admin API

All methods require a JWT with role **`MAN`**.

### Clients — `/admin/api/clients`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/admin/api/clients` | list clients |
| `GET` | `/admin/api/clients/{id}` | one client (`404` if missing) |
| `POST` | `/admin/api/clients` | create (`201`); requires `deviseId`, `password`; duplicate → `409` |
| `PUT` | `/admin/api/clients/{id}` | update; empty `password` leaves the password unchanged |
| `DELETE` | `/admin/api/clients/{id}` | delete (`204`) |

Client body:

```json
{
  "deviseId": "relay-kitchen",
  "password": "secret",
  "role": "DEVISE",
  "description": "Kitchen relay"
}
```

The `password` field is not returned in responses (write-only). Passwords are stored as bcrypt hashes.

### Messages — `/admin/api/messages`

```http
GET /admin/api/messages?deviseId=relay-kitchen&limit=200
```

| Query | Description |
|-------|-------------|
| `deviseId` | optional filter by device |
| `limit` | page size, default `200`, max `500` |

Response item (`AdminMessageOutDto`): `id`, `deviseId`, `command`, `deliveryStatus`, `deliveryGuarantee`, `createdDate`, `deliveredDate`, `tmpId`, `producerDeviseId`, `deliveredError`, `data`.

### Saved templates — `/admin/api/saved-messages`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/admin/api/saved-messages` | list; filter by `clientId` or `deviseId` |
| `GET` | `/admin/api/saved-messages/{id}` | one template |
| `POST` | `/admin/api/saved-messages` | create (`201`); only for a client with role `DEVISE` |
| `DELETE` | `/admin/api/saved-messages/{id}` | delete (`204`) |
| `POST` | `/admin/api/saved-messages/{id}/send` | send the template to the device **owner** → `ProducerMessageOutDto` |

Create:

```json
{
  "clientId": 5,
  "description": "Turn on pin 13",
  "command": "digitalWrite",
  "deliveryGuarantee": "RECEIPT_CONFIRMATION",
  "data": {
    "pinNumber": 13,
    "pinValue": true
  }
}
```

If `deliveryGuarantee` is omitted, `NO` is used. Send (`…/send`) always targets the template owner's `deviseId`, not an arbitrary address.

---

## Configuration (REST-related)

| Parameter / env | Purpose |
|-----------------|---------|
| `REST_SERVER_ADDRESS` | HTTP server bind address |
| `JWT_SECRET` | HS256 secret (≥ 32 UTF-8 bytes); must be your own value in production |
| `JWT_EXPIRATION_MS` | JWT lifetime in ms (default `86400000`) |
| `APP_SECURITY_PERMIT_ALL` | `true` — disable auth checks (dev/tests only) |
| `COMMON_SEND_MESSAGE_PERIOD` | interval for delivering messages to a device (IoT buffer protection) |
| `COMMON_MQTT_WAIT_RESPONSE_TIMEOUT` | timeout waiting for a consumer response |

Admin web UI: `http://localhost:8080` — same credentials `admin` / `admin`.

---

## curl examples

Login:

```bash
curl -s -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"deviseId":"admin","password":"admin"}'
```

Send a command:

```bash
TOKEN=... # accessToken from the login response

curl -s -X PUT "http://localhost:8080/producer/request/relay-kitchen" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "believerGuarantee": "RECEIPT_CONFIRMATION",
    "command": "digitalWrite",
    "data": {"pinNumber": 13, "pinValue": true}
  }'
```

Consumer poll:

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

## Related materials

- [README.md](../../README.md) — overview, how to run
- [MQTT_API.md](../mqtt/MQTT_API.md) — MQTT topics and exchange order
- [SWAGGER_AUTH.md](../swagger/SWAGGER_AUTH.md) — JWT in Swagger UI
- Test HTTP clients: `src/test/kotlin/com/duster/pd/rest/ProducerRest.kt`, `ConsumerRest.kt`
