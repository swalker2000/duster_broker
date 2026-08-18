# Duster Protocol (DBP)

Binary protocol over TCP. Default port: `9091` (`DUSTER_PROTOCOL_TCP_PORT`).

Device identifier in the protocol: **`deviseId`**.

For a producer login, `deviseId` is the queue recipient device.

---

## Frame

Every message is one frame:

```
START | TYPE | escape(payload) | CRC16_HI | CRC16_LO | STOP
```

| Field | Size | Value |
|-------|------|-------|
| START | 1 | `0x00` |
| TYPE | 1 | message type code |
| payload | 0…N | data (see types below) |
| CRC16 | 2 | CRC-16 over the **unescaped** payload: high byte, then low byte |
| STOP | 1 | `0x01` |

Integers in the payload are **little-endian**.

### Escaping

In the payload, bytes `0x00`, `0x01`, and `0x04` are prefixed with the mirror byte `0x04`.  
TYPE and CRC are not escaped.

Example: payload byte `0x00` is sent as `0x04 0x00`.

### CRC-16

Computed over the payload **before** escaping. Initial value `0xFFFF`, reflected polynomial `0xA001` (same as Modbus).

### UTF-8 string

`length[2 LE]` + `length` UTF-8 bytes. Empty string: `00 00`.

---

## Enumerations (ordinal on the wire)

**DeliveryGuarantee**

| Byte | Name |
|------|------|
| 0 | `NO` |
| 1 | `ONLY_LAST` |
| 2 | `RECEIPT_CONFIRMATION` |

**DeliveryStatus**

| Byte | Name | Set by |
|------|------|--------|
| 0 | `COMPLETED_WITH_ERROR` | consumer |
| 1 | `COMPLETED` | consumer |
| 2 | `DELIVERED` | consumer |
| 3 | `NOT_DELIVERED` | broker |
| 4 | `CANCELLED` | broker |
| 5 | `UNKNOWN` | broker |

The consumer may send only `0`, `1`, or `2`.

**Role**

| Byte | Name |
|------|------|
| 0 | `DEVISE` |
| 1 | `MAN` |

---

## Message types

Consumer codes: `0xC*`. Producer codes: `0xB*`.

Payloads below are **after** unescaping.

### Login

Same layout for consumer (`0xC5`) and producer (`0xB5`):

```
deviseIdLen[2] + deviseId[UTF-8] + passwordLen[2] + password[UTF-8]
```

### Login result

Same layout for consumer (`0xC6`) and producer (`0xB6`):

```
ok[1] + role[1] + deviseIdLen[2] + deviseId[UTF-8] + tokenLen[2] + accessToken[UTF-8]
```

`ok`: `1` — success, `0` — failure. On failure, close the connection.

### Consumer messages

**`0xC1` CONSUMER_ASK_MESSAGE** — request a message.

Payload: empty.

**`0xC2` BROKER_SEND_MESSAGE_TO_CONSUMER** — broker delivers a message.

```
id[8]
currentTimestamp[8]
commandLen[2] + command[N]
believerGuarantee[1]
dataJson[M]          UTF-8 JSON object; if there is no data — "{}"
```

`command` is a string; each character is one byte.

**`0xC3` BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER** — queue is empty.

Payload: empty.

**`0xC4` CONSUMER_MESSAGE_STATUS_CHANDGED** — status change (exactly 9 bytes).

```
id[8] + deliveryStatus[1]
```

The broker does not reply.

### Producer messages

**`0xB1` PRODUCER_SEND_MESSAGE** — send a message.

```
tmpId[8]
commandLen[2] + command[N]
believerGuarantee[1]
dataJson[M]
```

`tmpId = 0` — no client-side id subscription.

**`0xB2` BROKER_MESSAGE_RECEIVED_FROM_PRODUCER** — broker accepted the message (exactly 17 bytes).

```
id[8] + tmpId[8] + deliveryStatus[1]
```

**`0xB3` PRODUCER_ASK_MESSAGE_STATUS** — status query (exactly 8 bytes).

```
messageId[8]
```

**`0xB4` BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER** — status (exactly 9 bytes).

```
deliveryStatus[1] + messageId[8]
```

Field order is not the same as in `0xC4`.

---

## Exchange order

The first frame after TCP connect is login. Otherwise the broker closes the connection.

After a successful login, several working frames may be sent in the same TCP session. The socket may also be closed after each operation.

### Producer

```
TCP connect
  P → B   0xB5  PRODUCER_LOGIN
  B → P   0xB6  BROKER_PRODUCER_LOGIN_RESULT     (ok=1, otherwise disconnect)

  P → B   0xB1  PRODUCER_SEND_MESSAGE
  B → P   0xB2  BROKER_MESSAGE_RECEIVED_FROM_PRODUCER

  P → B   0xB3  PRODUCER_ASK_MESSAGE_STATUS      (optional)
  B → P   0xB4  BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER
```

### Consumer

```
TCP connect
  C → B   0xC5  CONSUMER_LOGIN
  B → C   0xC6  BROKER_CONSUMER_LOGIN_RESULT     (ok=1, otherwise disconnect)

  C → B   0xC1  CONSUMER_ASK_MESSAGE
  B → C   0xC2  BROKER_SEND_MESSAGE_TO_CONSUMER
        or
  B → C   0xC3  BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER

  C → B   0xC4  CONSUMER_MESSAGE_STATUS_CHANDGED (if the reply was 0xC2; no response)
```

Type summary:

| Code | Name | Who → whom |
|------|------|------------|
| `0xB5` | PRODUCER_LOGIN | P → B |
| `0xB6` | BROKER_PRODUCER_LOGIN_RESULT | B → P |
| `0xB1` | PRODUCER_SEND_MESSAGE | P → B |
| `0xB2` | BROKER_MESSAGE_RECEIVED_FROM_PRODUCER | B → P |
| `0xB3` | PRODUCER_ASK_MESSAGE_STATUS | P → B |
| `0xB4` | BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER | B → P |
| `0xC5` | CONSUMER_LOGIN | C → B |
| `0xC6` | BROKER_CONSUMER_LOGIN_RESULT | B → C |
| `0xC1` | CONSUMER_ASK_MESSAGE | C → B |
| `0xC2` | BROKER_SEND_MESSAGE_TO_CONSUMER | B → C |
| `0xC3` | BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER | B → C |
| `0xC4` | CONSUMER_MESSAGE_STATUS_CHANDGED | C → B |
