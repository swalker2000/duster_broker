# Сервис гарантии доставки работающий поверх MQTT.
- хранит сообщения как Kafka. Гарантирует доставку сообщения даже если устройство выключено в момент отправки.
- следит за периодом отправки сообщений на устройство. Если разом отправить множество сообщений на iot есть риск переполнения входного буфера. Во избежание подобной ситуации сообщения на iot устройство доходят с определенным интервалом.

## Алгоритм работы сервиса (передача сообщения от producer к consumer (id consumer : {deviceId}) )
1. получает по MQTT команду передачу сообщения в топике 'producer/request/{deviceId}'.
    Сообщение имеет тип JSON формата ProducerMessageInDto
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

2. Из ProducerMessageInDto получает ConsumerMessageOutDto последний снабжается уникальным id

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
3. ConsumerMessageOutDto передает на Consumer в топике 'consumer/request/{deviceId}' 
4. Если enum DeliveryGuarantee не NO ожидаем что consumer вернет ConsumerMessageInDto (с тем же id с которым пришел ConsumerMessageOutDto) в топике 'consumer/response/{deviceId}'
```json
  {
      "id":2
  }
```
5. Если сообщение от consumer не поступило в заданный период возвращаемся к пункту 3.
