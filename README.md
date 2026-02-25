# Delivery guarantee service operating over MQTT.
- Stores messages like Kafka. Guarantees message delivery even if the device is turned off at the time of sending.
- Monitors the sending interval of messages to the device. If many messages are sent to the IoT device at once, there is a risk of input buffer overflow. To avoid this situation, messages to the IoT device arrive at a certain interval.

## Service operation algorithm (message transmission from producer to consumer (consumer id: {deviceId}))
1. Receives a message transmission command via MQTT in the topic 'producer/request/{deviceId}'.
   The message is of JSON type ProducerMessageInDto.
```json
  {
    "believerGuarantee": "RECEIPT_CONFIRMATION",
    "command": "some_command",
    "data": {
      "key1" : "value1",
      "key2" : "value2"
    }
  }
```

2. From ProducerMessageInDto, it creates ConsumerMessageOutDto, which is assigned a unique id.

```json
  {
    "id":2,
    "believerGuarantee":"RECEIPT_CONFIRMATION",
    "command":"digitalWrite",
    "currentTimestamp":1772021717684,
    "data":{
      "key1" : "value1",
      "key2" : "value2"
    }
  }
```
3. Sends ConsumerMessageOutDto to the Consumer in the topic 'consumer/request/{deviceId}'.
4. If the enum DeliveryGuarantee is not NO, we expect the consumer to return ConsumerMessageInDto (with the same id as in the incoming ConsumerMessageOutDto) in the topic 'consumer/response/{deviceId}'.
```json
  {
    "id":2
  }
```
5. If no message is received from the consumer within the specified period, we return to step 3.