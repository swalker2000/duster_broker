# Сервис гарантии доставки работающий поверх MQTT.
- хранит сообщения как Kafka. Гарантирует доставку сообщения, даже если устройство выключено в момент отправки.
  Может гарантировать как доставку:
    - ```ONLY_LAST``` - последнего сообщения с выбранной командой для данного устройства.
    - ```RECEIPT_CONFIRMATION``` - всех отправленных сообщений.
    - ```NO``` - без гарантии доставки.
- следит за периодом отправки сообщений на устройство. Если разом отправить множество сообщений на iot есть риск переполнения входного буфера. Во избежание подобной ситуации сообщения на iot устройство доходят с определенным интервалом.
- дает возможность отправителю подписаться на смену статуса своего сообщения. Сейчас доступны следующие статусы:
    - ```NOT_DELIVERED``` - сообщение не доставлено.
    - ```DELIVERED``` - сообщение доставлено.
    - ```COMPLETED``` - задача, отправленная в сообщении, выполнена.
    - ```COMPLETED_WITH_ERROR``` - задача, отправленная в сообщении, не выполнена или выполнена с ошибкой.
- дает возможность взаимодействовать как по MQTT, так и по REST API. (документация на нее пока в разработке, смотри через swagger http://localhost:8080/swagger-ui.html, не забудь выставить REST_SERVER_ADDRESS в docker-compose.yaml или application.yml).<br>
  К примеру датчик температуры может отправлять показания по REST, а реле принимающее данные будет получать их по MQTT. Или же устройство, установленное в транспортном средстве, попадая в область, где ловит связь, получает по REST список пропущенных команд.
# Админка  
Доступна по адресу: `http://localhost:8080` <br>
Дефолтный логин: `admin`<br>
Дефолтный пароль: `admin` <br><br>
 Позволят отправлять, просматриваить отправленные сообщения, так же есть возможность для каждого устройсва настроить
набор команд (сохраненных сообщений) и отправлять их по одному клику.


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

## Алгоритм работы сервиса когда producer подписывается на изменения статуса доставки своего сообщения
1. получает по MQTT команду передачу сообщения в топике 'producer/request/{deviceId}'.
   Сообщение имеет тип JSON формата ProducerMessageInDto
    - `messageBirthCertificate` - информация о происхождении сообщения. Если поле не найдено или null мы не информируем producer о сообщении.
      - `tmpId` - (временный id сообщения) не равный 0.
      - `producerDeviseId` - id producer другими словами id устройства (deviceId) которое сгенерировало сообщение.
2. 
```json
  {
  "believerGuarantee": "RECEIPT_CONFIRMATION",
  "messageBirthCertificate" : {
    "tmpId" : 3,
    "producerDeviseId" : "0"
  },
  "command": "some_command",
  "data": {
    "key1" : "value1",
    "key2" : "value2"
    }
  }
```

3. Из ProducerMessageInDto получает ConsumerMessageOutDto последний снабжается уникальным id

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
4. На producer в топик producer/response/{producerDeviceId} передается ProducerMessageOutDto с сгенерированным ранее id (в поле id).
```json
  {
      "id":2,
      "tmpId" : 3,
      "deliveryStatus": "NOT_DELIVERED"
   }
```
5. ConsumerMessageOutDto передает на Consumer в топике 'consumer/request/{deviceId}'
6. Если enum DeliveryGuarantee не NO ожидаем что consumer вернет ConsumerMessageInDto (с тем же id с которым пришел ConsumerMessageOutDto) в топике 'consumer/response/{deviceId}'<br>
Если поле `deliveryStatus` не передается или null считаем, что статус `DELIVERED`.<br>
Если через какое то время мы хотим поменять статус сообщения на `COMPLETED_WITH_ERROR` или `COMPLETED` делаем это в этом же сообщении.
```json
  {
      "id":2,
      "deliveryStatus" : "DELIVERED"
  }
```
7. Если сообщение от consumer не поступило в заданный период возвращаемся к пункту 3. <br>
Если поступило информируем об этом producer в топике producer/response/{producerDeviceId} сообщением ProducerMessageOutDto.<br>
О всех дальнейших изменениях `deliveryStatus` сообщения информируем так же в этом же топике.
```json
  {
      "id":2,
      "tmpId" : 3,
      "deliveryStatus": "DELIVERED"
   }
```

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

## Пример прошивки коммутатора на базе esp32 (lilygo T-Relay ) работающего с сервисом гарантии доставки работающим поверх MQTT
# https://github.com/swalker2000/duster_lilygo_relay

## Интеграция с openremote (https://github.com/openremote/openremote)
# Настройка MQTT
Openremote не поддерживает не подписанные сертификаты по этому mqtts меняем на mqtt.
1. Меняем содержание файла mqtt/docker-mqtt/mosquitto.conf 
```
# Log to stdout
log_dest stdout
log_type ${LOG_TYPE}

# Обычный MQTT listener (без шифрования)
listener ${MQTT_PORT}
protocol mqtt
socket_domain ipv4

# Обычный WebSocket listener (без шифрования)
listener ${WEBSOCKET_PORT}
protocol websockets
socket_domain ipv4

# Authentication
allow_anonymous false
password_file /mosquitto/password/passwd
```
2. Меняем содержание файла docker-compose.yaml
```yamlservices:
  duster:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: duster
    depends_on:
      - postgres
      - mqtt
    environment:
      # --- Postgres ---
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/postgres
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres

      # --- JPA  ---
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      #SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.PostgreSQLDialect

      # --- MQTT ---
      # Если брокер у тебя в контейнере слушает TLS на 8883 — оставь так.
      MQTT_BROKER_URL: tcp://mqtt:${MQTT_PORT}
      MQTT_BROKER_USERNAME: ${MQTT_BROKER_USERNAME}
      MQTT_BROKER_PASSWORD: ${MQTT_BROKER_PASSWORD}
      MQTT_SSL_INSECURE: "false"
      MQTT_QOS: "1"

      # --- Rest ---
      REST_SERVER_ADDRESS: "0.0.0.0"

      # --- Common ---
      COMMON_CHECK_NOT_DELIVERED_TIMEOUT: "60000"
      COMMON_MQTT_WAIT_RESPONSE_TIMEOUT: "30000"
      COMMON_SEND_MESSAGE_PERIOD: "2000"
      COMMON_CONSUMER_TIMEOUT: "2000"
      COMMON_MESSAGE_SEND_TIME_CASH_COLLECTOR_RUN_PERIOD: "3600000"

    ports:
      - "8080:8080"
    restart: unless-stopped
    networks:
      - duster-net

  postgres:
    image: postgres:16
    container_name: duster-postgres
    environment:
      POSTGRES_DB: postgres
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    #ports:
    #  - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    restart: unless-stopped
    networks:
      - duster-net

  mqtt:
    build:
      context: mqtt/docker-mqtt
      dockerfile: Dockerfile
    image: custom-mosquitto:latest
    restart: unless-stopped
    ports:
      - "${MQTT_PORT}:${MQTT_PORT}"
      - "${WEBSOCKET_PORT}:${WEBSOCKET_PORT}"
    volumes:
      - ./mqtt/docker-mqtt/certs:/mosquitto/certs
      - ./mqtt/docker-mqtt/mosquitto.conf:/mosquitto/config/mosquitto.conf
    environment:
      USERNAME: ${MQTT_BROKER_USERNAME}
      PASSWORD: ${MQTT_BROKER_PASSWORD}
      HOSTNAME: 0.0.0.0
      LOG_TYPE: notice
      MQTT_PORT: ${MQTT_PORT}
      WEBSOCKET_PORT: ${WEBSOCKET_PORT}
    networks:
      - duster-net

volumes:
  pgdata:

networks:
  duster-net:
    driver: bridge
```
# Настройка openremote

1. **В OpenRemote создайте MQTT Agent**:
    - Manager → Agents → Create → **MQTT Agent**
    - Укажите host, port, username/password от брокера duster_broker.
    - Сохраните.

2. **Настройте assets и атрибуты**:
    - Создайте/откройте Asset вашего устройства.
    - Добавьте writable-атрибут (например, `command` типа JSON или String).
    - В конфигурации атрибута добавьте **Agent Link** → выберите ваш MQTT Agent.
    - **Publish Topic**: `producer/request/{deviceId}` (где `{deviceId}` — ваш ID устройства, например `esp32-relay-01`).
    - В payload (через value filters / JSON mapper) передавайте структуру `ProducerMessageInDto`:
      ```json
      {
        "believerGuarantee": "RECEIPT_CONFIRMATION",
        "command": "digitalWrite",
        "data": { "pinNumber": 13, "pinValue": true }
      }
      ```
#   Настройка ESP32
Базовый пример : https://github.com/swalker2000/duster_lilygo_relay <br>
В Secret.h должна быть следующая строка

```
#define SECRET_MQTT_TLS 0
```