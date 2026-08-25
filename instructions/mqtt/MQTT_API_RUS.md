# Работа с сервисом через MQTT

JSON в MQTT-топиках. `{deviceId}` — идентификатор consumer (получателя).

## Топики

| Топик | Направление | Payload |
|-------|-------------|---------|
| `producer/request/{deviceId}` | producer → брокер | `ProducerMessageInDto` |
| `consumer/request/{deviceId}` | брокер → consumer | `ConsumerMessageOutDto` |
| `consumer/response/{deviceId}` | consumer → брокер | `ConsumerMessageInDto` |
| `producer/response/{producerDeviceId}` | брокер → producer | `ProducerMessageOutDto` |

Топик `producer/response/...` используется, только если producer подписался через `messageBirthCertificate`.

---

## Доставка без подписки на статус

1. Producer публикует `ProducerMessageInDto` в `producer/request/{deviceId}`:

```json
{
  "believerGuarantee": "RECEIPT_CONFIRMATION",
  "command": "some_command",
  "data": {
    "key1": "value1",
    "key2": "value2"
  }
}
```

2. Брокер создаёт `ConsumerMessageOutDto` и назначает уникальный `id`:

```json
{
  "id": 2,
  "believerGuarantee": "RECEIPT_CONFIRMATION",
  "command": "digitalWrite",
  "currentTimestamp": 1772021717684,
  "data": {
    "key1": "value1",
    "key2": "value2"
  }
}
```

3. Брокер публикует `ConsumerMessageOutDto` в `consumer/request/{deviceId}`.

4. Если `DeliveryGuarantee` не `NO`, брокер ждёт `ConsumerMessageInDto` с тем же `id` в `consumer/response/{deviceId}`:

```json
{
  "id": 2
}
```

5. Если ответ consumer не пришёл за таймаут, брокер повторяет шаг 3.

---

## Доставка с подпиской producer на статус

1. Producer публикует `ProducerMessageInDto` в `producer/request/{deviceId}`.

`messageBirthCertificate` — происхождение сообщения. Если поле отсутствует или `null`, producer не уведомляется.

- `tmpId` — временный id сообщения, не равный `0`.
- `producerDeviseId` — id устройства-отправителя (`deviceId` producer).

```json
{
  "believerGuarantee": "RECEIPT_CONFIRMATION",
  "messageBirthCertificate": {
    "tmpId": 3,
    "producerDeviseId": "0"
  },
  "command": "some_command",
  "data": {
    "key1": "value1",
    "key2": "value2"
  }
}
```

2. Брокер создаёт `ConsumerMessageOutDto` и назначает уникальный `id`.

3. Брокеру на `producer/response/{producerDeviceId}` уходит `ProducerMessageOutDto` с этим `id`:

```json
{
  "id": 2,
  "tmpId": 3,
  "deliveryStatus": "NOT_DELIVERED"
}
```

4. Брокер публикует `ConsumerMessageOutDto` в `consumer/request/{deviceId}`.

5. Если `DeliveryGuarantee` не `NO`, брокер ждёт `ConsumerMessageInDto` с тем же `id` в `consumer/response/{deviceId}`.

Если `deliveryStatus` нет или `null`, считается `DELIVERED`. Позже в том же сообщении можно передать `COMPLETED` или `COMPLETED_WITH_ERROR`.

```json
{
  "id": 2,
  "deliveryStatus": "DELIVERED"
}
```

6. Если ответ consumer не пришёл за таймаут, брокер повторяет шаг 4.  
Если пришёл — уведомляет producer в `producer/response/{producerDeviceId}` сообщением `ProducerMessageOutDto`. Дальнейшие смены `deliveryStatus` публикуются в тот же топик.

```json
{
  "id": 2,
  "tmpId": 3,
  "deliveryStatus": "DELIVERED"
}
```

---

## Порядок обмена

Без подписки:

```
P → брокер   producer/request/{deviceId}      ProducerMessageInDto
брокер → C   consumer/request/{deviceId}      ConsumerMessageOutDto
C → брокер   consumer/response/{deviceId}     ConsumerMessageInDto
```

С подпиской (`messageBirthCertificate`):

```
P → брокер   producer/request/{deviceId}              ProducerMessageInDto
брокер → P   producer/response/{producerDeviceId}     ProducerMessageOutDto  (выдан id)
брокер → C   consumer/request/{deviceId}              ConsumerMessageOutDto
C → брокер   consumer/response/{deviceId}             ConsumerMessageInDto
брокер → P   producer/response/{producerDeviceId}     ProducerMessageOutDto  (смены статуса)
```

---

## Пример: светодиод на ESP32

Топик: `producer/request/device123`  
Consumer слушает: `consumer/request/device123`

Включить пин 13:

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

Выключить пин 13: тот же топик, `"pinValue": false`.
