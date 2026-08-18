# Брокер сообщений для IOT с гарантиями доставки, отслеживанием статуса и ограничением скорости. (есть примеры подключения для esp32).

Описание Duster Protocol: [instructions/duster_protocol/DUSTER_PROTOCOL_RUS.md](instructions/duster_protocol/DUSTER_PROTOCOL_RUS.md)

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
- транспорты: бинарный [Duster Protocol](instructions/duster_protocol/DUSTER_PROTOCOL_RUS.md), [MQTT](instructions/mqtt/MQTT_API_RUS.md), [REST API](instructions/restapi/REST_API_RUS.md) (интерактивная спецификация — [Swagger UI](http://localhost:8080/swagger-ui.html); авторизация в Swagger: [instructions/swagger/SWAGGER_AUTH_RUS.md](instructions/swagger/SWAGGER_AUTH_RUS.md)). Не забудь выставить `REST_SERVER_ADDRESS` в `docker-compose.yaml` или `application.yml`.<br>
  К примеру датчик температуры может отправлять показания по REST, а реле принимающее данные будет получать их по MQTT. Или же устройство, установленное в транспортном средстве, попадая в область, где ловит связь, получает по REST список пропущенных команд.
# Админка  
Доступна по адресу: `http://localhost:8080` <br>
Дефолтный логин: `admin`<br>
Дефолтный пароль: `admin` <br><br>
 Позволят отправлять, просматриваить отправленные сообщения, так же есть возможность для каждого устройсва настроить
набор команд (сохраненных сообщений) и отправлять их по одному клику.


Топики MQTT, JSON и порядок обмена producer/consumer: [instructions/mqtt/MQTT_API_RUS.md](instructions/mqtt/MQTT_API_RUS.md).

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

## Пример IOT клиента работающего по протоколу Duster Protocol
Пример consumer на основе esp32 : https://github.com/swalker2000/duster_esp32_protocol_example
Для компиляции необходимо в основной директории скетча создать файл Secret.h со следующим содержимым: 
```c++
#define SSID          "MY_SSID"
#define WIFI_PASS     "MY_WIFI_PASS"
#define HOST          "BROKER_HOST"
#define PORT          9091
#define DEVICE_ID     "device1"
#define DEVICE_PASS   "DEVICE_PASSWORD"
```

Esp32 чип мигающий светодиодом по команде от брокера. Слушает топик consumer/request/device123
Полезная нагрузка сообщения для того что бы 13 светодиод загорелся: <br>
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
Полезная нагрузка сообщения для того что бы 13 светодиод потух. <br>
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

## Пример прошивки коммутатора на базе esp32 (lilygo T-Relay ) работающего с сервисом по протоколу MQTT
# https://github.com/swalker2000/duster_lilygo_relay

# Интеграция с openremote (https://github.com/openremote/openremote)
## Настройка MQTT
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
## Настройка openremote

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
##   Настройка ESP32
Базовый пример : https://github.com/swalker2000/duster_lilygo_relay <br>
В Secret.h должна быть следующая строка

```
#define SECRET_MQTT_TLS 0
```