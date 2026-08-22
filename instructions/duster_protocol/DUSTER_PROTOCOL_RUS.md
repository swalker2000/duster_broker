# Duster Protocol (DBP)

Бинарный протокол поверх TCP. Порты по умолчанию: `9091` без TLS (`DUSTER_PROTOCOL_TCP_PORT`) и `9092` с TLS (`DUSTER_PROTOCOL_TLS_PORT`). Нешифрованный слушатель включается флагом `duster.protocol.tcp.enabled` / `DUSTER_PROTOCOL_TCP_ENABLED`.

Сертификат для TLS-клиентов: [tls/TLS_RUS.md](tls/TLS_RUS.md).

Идентификатор устройства в протоколе: **`deviseId`**.

Для producer в логине `deviseId` — это устройство-получатель очереди.

---

## Кадр

Каждое сообщение — один кадр:

```
START | TYPE | escape(payload) | CRC16_HI | CRC16_LO | STOP
```

| Поле | Размер | Значение |
|------|--------|----------|
| START | 1 | `0x00` |
| TYPE | 1 | код типа сообщения |
| payload | 0…N | данные (см. типы ниже) |
| CRC16 | 2 | CRC-16 по **неэкранированному** payload: старший байт, затем младший |
| STOP | 1 | `0x01` |

Числа в payload — **little-endian**.

### Экранирование

В payload байты `0x00`, `0x01` и `0x04` предваряются зеркалом `0x04`.  
TYPE и CRC не экранируются.

Пример: байт `0x00` в payload на проводе становится `0x04 0x00`.

### CRC-16

Считается по payload **до** экранирования. Старт `0xFFFF`, отражённый полином `0xA001` (как Modbus).

### Строка UTF-8

`длина[2 LE]` + `длина` байт UTF-8. Пустая строка: `00 00`.

---

## Перечисления (ordinal на проводе)

**DeliveryGuarantee**

| Байт | Имя |
|------|-----|
| 0 | `NO` |
| 1 | `ONLY_LAST` |
| 2 | `RECEIPT_CONFIRMATION` |

**DeliveryStatus**

| Байт | Имя | Кто ставит |
|------|-----|------------|
| 0 | `COMPLETED_WITH_ERROR` | consumer |
| 1 | `COMPLETED` | consumer |
| 2 | `DELIVERED` | consumer |
| 3 | `NOT_DELIVERED` | брокер |
| 4 | `CANCELLED` | брокер |
| 5 | `UNKNOWN` | брокер |

Consumer может прислать только `0`, `1` или `2`.

**Role**

| Байт | Имя |
|------|-----|
| 0 | `DEVISE` |
| 1 | `MAN` |

---

## Типы сообщений

Коды consumer: `0xC*`. Коды producer: `0xB*`.

Ниже payload **после** снятия экранирования.

### Логин

Одинаковая структура у consumer (`0xC5`) и producer (`0xB5`):

```
deviseIdLen[2] + deviseId[UTF-8] + passwordLen[2] + password[UTF-8]
```

### Результат логина

Одинаковая структура у consumer (`0xC6`) и producer (`0xB6`):

```
ok[1] + role[1] + deviseIdLen[2] + deviseId[UTF-8] + tokenLen[2] + accessToken[UTF-8]
```

`ok`: `1` — успех, `0` — отказ. При отказе соединение закрывают.

### Сообщения consumer

**`0xC1` CONSUMER_ASK_MESSAGE** — запрос сообщения.

Payload: пусто.

**`0xC2` BROKER_SEND_MESSAGE_TO_CONSUMER** — брокер отдаёт сообщение.

```
id[8]
currentTimestamp[8]
commandLen[2] + command[N]
believerGuarantee[1]
dataJson[M]          UTF-8 JSON-объект; если данных нет — "{}"
```

`command` — строка, каждый символ одним байтом.

**`0xC3` BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER** — очередь пуста.

Payload: пусто.

**`0xC4` CONSUMER_MESSAGE_STATUS_CHANDGED** — смена статуса (ровно 9 байт).

```
id[8] + deliveryStatus[1]
```

Ответа от брокера нет.

### Сообщения producer

**`0xB1` PRODUCER_SEND_MESSAGE** — отправка сообщения.

```
tmpId[8]
commandLen[2] + command[N]
believerGuarantee[1]
dataJson[M]
```

`tmpId = 0` — без клиентской подписки на id.

**`0xB2` BROKER_MESSAGE_RECEIVED_FROM_PRODUCER** — брокер принял сообщение (ровно 17 байт).

```
id[8] + tmpId[8] + deliveryStatus[1]
```

**`0xB3` PRODUCER_ASK_MESSAGE_STATUS** — запрос статуса (ровно 8 байт).

```
messageId[8]
```

**`0xB4` BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER** — статус (ровно 9 байт).

```
deliveryStatus[1] + messageId[8]
```

Порядок полей не такой, как у `0xC4`.

---

## Порядок обмена

Первый кадр после TCP-connect — логин. Иначе брокер закрывает соединение.

После успешного логина в том же TCP-сеансе можно слать несколько рабочих кадров. Можно и закрывать сокет после каждой операции.

### Producer

```
TCP connect
  P → B   0xB5  PRODUCER_LOGIN
  B → P   0xB6  BROKER_PRODUCER_LOGIN_RESULT     (ok=1, иначе disconnect)

  P → B   0xB1  PRODUCER_SEND_MESSAGE
  B → P   0xB2  BROKER_MESSAGE_RECEIVED_FROM_PRODUCER

  P → B   0xB3  PRODUCER_ASK_MESSAGE_STATUS      (по необходимости)
  B → P   0xB4  BROKER_RETURN_MESSAGE_STATUS_TO_PRODUCER
```

### Consumer

```
TCP connect
  C → B   0xC5  CONSUMER_LOGIN
  B → C   0xC6  BROKER_CONSUMER_LOGIN_RESULT     (ok=1, иначе disconnect)

  C → B   0xC1  CONSUMER_ASK_MESSAGE
  B → C   0xC2  BROKER_SEND_MESSAGE_TO_CONSUMER
        или
  B → C   0xC3  BROKER_DONT_HAVE_MESSAGE_FOR_CONSUMER

  C → B   0xC4  CONSUMER_MESSAGE_STATUS_CHANDGED (если было 0xC2; ответа нет)
```

Сводка типов:

| Код | Имя | Кто → кому |
|-----|-----|------------|
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
