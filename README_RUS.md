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

## Запуск
### Запуск через docker compose

Поднимутся следующие сервисы:
- mqtt брокер (взят от сюда https://github.com/ericwastaken/docker-mqtt.git)
- база данных Postgres
- экземпляр сервиса duster

Склонируйте текущий репозиторий. Создайте в корне файл .env

```
MQTT_BROKER_USERNAME=YOU_USERNAME
MQTT_BROKER_PASSWORD=YOU_PASSWORD
WEBSOCKET_PORT=9443
MQTT_PORT=8883
```
Запустите docker compose

```bash
  docker compose up -d
```

### Запуск jar файла
Склонируйте текущий репозиторий.
1. Соберите проект командой ```gradle build```
В директории build/libs появится jar файл (прим. duster-0.0.1-SNAPSHOT.jar).
2. Поместите его в отдельную папку. Туда же поместите application.yaml (находится src/main/resources/application.yaml).
3. Выставите требуемые настройки (параметры подключения к базе данных, брокеру сообщений..) в файле application.yaml.
4. Запустите сервис командой ```java -jar duster-0.0.1-SNAPSHOT.jar```

## Пример IOT клиента
Пример consumer на основе esp32 : https://github.com/swalker2000/duster_esp32_example
Для компиляции необходимо в основной директории скетча создать файл Secret.h со следующим содержимым: 
```c++
#define SSID          "MY_SSID"
#define WIFI_PASS     "MY_WIFI_PASS"
#define URL           "MQTT_URL"
#define PORT          8883
#define MQTT_USERNAME "MQTT_USERNAME"
#define MQTT_PASS     "MQTT_PASSWORD"
```

Esp32 чип мигающий светодиодом по команде от брокера. Слушает топик consumer/request/device123
Сообщение для того что бы 13 светодиод загорелся: <br>
Топик : (producer/request/device123)
```json
{
  "believerGuarantee": "RECEIPT_CONFIRMATION",
  "command": "digitalWrite",
  "data": {
    "pinNumber": 13,
    "pinValue" : true
  }
}
```
Сообщение для того что бы 13 светодиод потух. <br>
Топик : (producer/request/device123)
```json
{
  "believerGuarantee": "RECEIPT_CONFIRMATION",
  "command": "digitalWrite",
  "data": {
    "pinNumber": 13,
    "pinValue" : false
  }
}
```