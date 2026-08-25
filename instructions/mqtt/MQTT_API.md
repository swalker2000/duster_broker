# Using the service via MQTT

JSON over MQTT topics. `{deviceId}` is the consumer (recipient) identifier.

## Topics

| Topic | Direction | Payload |
|-------|-----------|---------|
| `producer/request/{deviceId}` | producer → broker | `ProducerMessageInDto` |
| `consumer/request/{deviceId}` | broker → consumer | `ConsumerMessageOutDto` |
| `consumer/response/{deviceId}` | consumer → broker | `ConsumerMessageInDto` |
| `producer/response/{producerDeviceId}` | broker → producer | `ProducerMessageOutDto` |

`producer/response/...` is used only when the producer subscribed via `messageBirthCertificate`.

---

## Delivery without status subscription

1. The producer publishes `ProducerMessageInDto` to `producer/request/{deviceId}`:

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

2. The broker creates `ConsumerMessageOutDto` and assigns a unique `id`:

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

3. The broker publishes `ConsumerMessageOutDto` to `consumer/request/{deviceId}`.

4. If `DeliveryGuarantee` is not `NO`, the broker waits for `ConsumerMessageInDto` with the same `id` on `consumer/response/{deviceId}`:

```json
{
  "id": 2
}
```

5. If the consumer reply does not arrive within the timeout, the broker repeats step 3.

---

## Delivery with producer status subscription

1. The producer publishes `ProducerMessageInDto` to `producer/request/{deviceId}`.

`messageBirthCertificate` is the origin of the message. If the field is missing or null, the producer is not notified.

- `tmpId` — temporary message id, must not be `0`.
- `producerDeviseId` — producer device id (`deviceId` of the sender).

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

2. The broker creates `ConsumerMessageOutDto` and assigns a unique `id`.

3. The broker publishes `ProducerMessageOutDto` to `producer/response/{producerDeviceId}` with that `id`:

```json
{
  "id": 2,
  "tmpId": 3,
  "deliveryStatus": "NOT_DELIVERED"
}
```

4. The broker publishes `ConsumerMessageOutDto` to `consumer/request/{deviceId}`.

5. If `DeliveryGuarantee` is not `NO`, the broker waits for `ConsumerMessageInDto` with the same `id` on `consumer/response/{deviceId}`.

If `deliveryStatus` is missing or null, it is treated as `DELIVERED`. Later the same message can carry `COMPLETED` or `COMPLETED_WITH_ERROR`.

```json
{
  "id": 2,
  "deliveryStatus": "DELIVERED"
}
```

6. If the consumer reply does not arrive within the timeout, the broker repeats step 4.  
If it arrives, the broker notifies the producer on `producer/response/{producerDeviceId}` with `ProducerMessageOutDto`. Further `deliveryStatus` changes are published to the same topic.

```json
{
  "id": 2,
  "tmpId": 3,
  "deliveryStatus": "DELIVERED"
}
```

---

## Exchange order

Without subscription:

```
P → broker   producer/request/{deviceId}      ProducerMessageInDto
broker → C   consumer/request/{deviceId}      ConsumerMessageOutDto
C → broker   consumer/response/{deviceId}     ConsumerMessageInDto
```

With subscription (`messageBirthCertificate`):

```
P → broker   producer/request/{deviceId}              ProducerMessageInDto
broker → P   producer/response/{producerDeviceId}     ProducerMessageOutDto  (id assigned)
broker → C   consumer/request/{deviceId}              ConsumerMessageOutDto
C → broker   consumer/response/{deviceId}             ConsumerMessageInDto
broker → P   producer/response/{producerDeviceId}     ProducerMessageOutDto  (status updates)
```

---

## Example: LED on ESP32

Topic: `producer/request/device123`  
Consumer listens on: `consumer/request/device123`

Turn pin 13 on:

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

Turn pin 13 off: same topic, `"pinValue": false`.
