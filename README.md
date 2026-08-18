# IoT message broker with delivery guarantees, status tracking, and rate limiting (includes connection examples for ESP32).

Duster Protocol: [instructions/duster_protocol/DUSTER_PROTOCOL.md](instructions/duster_protocol/DUSTER_PROTOCOL.md)

<img width="1082" height="719" alt="Снимок экрана 2026-04-08 в 16 02 02" src="https://github.com/user-attachments/assets/01db0192-6aa2-4611-b5a5-bd9529f1906f" />


- Stores messages like Kafka. Guarantees message delivery even if the device is offline at the time of sending.
  Can guarantee delivery as:
    - ```ONLY_LAST``` - only the last message with a selected command for a given device.
    - ```RECEIPT_CONFIRMATION``` - all sent messages.
    - ```NO``` - no delivery guarantee.
- Monitors the period of sending messages to the device. Sending many messages to an IoT device at once risks overflowing its input buffer. To avoid this situation, messages reach the IoT device at a specific interval.
- Provides the sender with the ability to subscribe to changes in their message's status. The following statuses are currently available:
    - ```NOT_DELIVERED``` - the message was not delivered.
    - ```DELIVERED``` - the message was delivered.
    - ```COMPLETED``` - the task sent in the message was completed successfully.
    - ```COMPLETED_WITH_ERROR``` - the task sent in the message was not completed or was completed with an error.
  - Transports: binary [Duster Protocol](instructions/duster_protocol/DUSTER_PROTOCOL.md), [MQTT](instructions/mqtt/MQTT_API.md), [REST API](instructions/restapi/REST_API.md) (interactive spec: [Swagger UI](http://localhost:8080/swagger-ui.html); Swagger login: [instructions/swagger/SWAGGER_AUTH.md](instructions/swagger/SWAGGER_AUTH.md)). Don't forget to set `REST_SERVER_ADDRESS` in `docker-compose.yaml` or `application.yml`.<br>
    For example, a temperature sensor might send readings via REST, while the receiving relay gets the data via MQTT. Alternatively, a device installed in a vehicle could retrieve a list of missed commands via REST upon entering an area with network coverage.
# Admin Panel
Available at: `http://localhost:8080` <br>
Default login: `admin`<br>
Default password: `admin` <br><br>
Allows you to send and view sent messages, and also provides the ability to configure a set of commands (saved messages) for each device and send them with a single click.

MQTT topics, JSON payloads, and producer/consumer exchange order: [instructions/mqtt/MQTT_API.md](instructions/mqtt/MQTT_API.md).

## Launch

### Running with Docker Compose

The following services will be started:
- MQTT broker (taken from https://github.com/ericwastaken/docker-mqtt.git)
- PostgreSQL database
- One instance of the duster service

Clone the current repository. Create a `.env` file in the root directory:

```
MQTT_BROKER_USERNAME=YOUR_USERNAME
MQTT_BROKER_PASSWORD=YOUR_PASSWORD
WEBSOCKET_PORT=9443
MQTT_PORT=8883
```

Run Docker Compose:

```bash
docker compose up -d
```

### Running the JAR File

Clone the current repository.

1. Build the project with the command:
   ```bash
   gradle build
   ```  
   A JAR file (e.g., `duster-0.0.1-SNAPSHOT.jar`) will be created in the `build/libs` directory.

2. Place it in a separate folder. Also place the `application.yaml` file (located at `src/main/resources/application.yaml`) in the same folder.

3. Adjust the required settings (database connection parameters, message broker connection, etc.) in the `application.yaml` file.

4. Run the service with the command:
   ```bash
   java -jar duster-0.0.1-SNAPSHOT.jar
   ```

## Example of an IoT Client Operating via the Duster Protocol

Example consumer based on esp32: https://github.com/swalker2000/duster_esp32_protocol_example

To compile, you need to create a file named `Secret.h` in the main sketch directory with the following content:
```c++
#define SSID          "MY_SSID"
#define WIFI_PASS     "MY_WIFI_PASS"
#define HOST          "BROKER_HOST"
#define PORT          9091
#define DEVICE_ID     "device1"
#define DEVICE_PASS   "DEVICE_PASSWORD"
```

The ESP32 chip blinks an LED upon command from the broker. It listens to the topic `consumer/request/device123`.

The message payload to turn on the LED at pin 13:
<br>
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

The message payload to turn off the LED at pin 13:
<br>
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
## Example of firmware for a switch based on ESP32 (LilyGO T-Relay) operating with a service via the MQTT protocol
## https://github.com/swalker2000/duster_lilygo_relay

## Integration with OpenRemote ([https://github.com/openremote/openremote](https://github.com/openremote/openremote))

### MQTT Setup

OpenRemote does not support unsigned certificates, so we switch from `mqtts` to `mqtt`.

1. Update the contents of the file `mqtt/docker-mqtt/mosquitto.conf`:

```
# Log to stdout
log_dest stdout
log_type ${LOG_TYPE}

# Regular MQTT listener (without encryption)
listener ${MQTT_PORT}
protocol mqtt
socket_domain ipv4

# Regular WebSocket listener (without encryption)
listener ${WEBSOCKET_PORT}
protocol websockets
socket_domain ipv4

# Authentication
allow_anonymous false
password_file /mosquitto/password/passwd
```

2. Update the contents of the `docker-compose.yaml` file:

```yaml
services:
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

      # --- JPA ---
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      #SPRING_JPA_DATABASE_PLATFORM: org.hibernate.dialect.PostgreSQLDialect

      # --- MQTT ---
      # If your broker inside the container listens with TLS on 8883 — leave it as is.
      MQTT_BROKER_URL: tcp://mqtt:${MQTT_PORT}
      MQTT_BROKER_USERNAME: ${MQTT_BROKER_USERNAME}
      MQTT_BROKER_PASSWORD: ${MQTT_BROKER_PASSWORD}
      MQTT_SSL_INSECURE: "false"
      MQTT_QOS: "1"

      # --- REST ---
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

### OpenRemote Setup

1. **Create an MQTT Agent in OpenRemote**:

    * Manager → Agents → Create → **MQTT Agent**
    * Enter the host, port, username/password of the `duster_broker`.
    * Save.

2. **Configure assets and attributes**:

    * Create/open your device asset.
    * Add a writable attribute (e.g. `command` of type JSON or String).
    * In the attribute configuration, add an **Agent Link** → select your MQTT Agent.
    * **Publish Topic**: `producer/request/{deviceId}` (where `{deviceId}` is your device ID, e.g. `esp32-relay-01`).
    * In the payload (via value filters / JSON mapper), send the `ProducerMessageInDto` structure:

   ```json
   {
     "believerGuarantee": "RECEIPT_CONFIRMATION",
     "command": "digitalWrite",
     "data": { "pinNumber": 13, "pinValue": true }
   }
   ```

### ESP32 Setup

Basic example: [https://github.com/swalker2000/duster_lilygo_relay](https://github.com/swalker2000/duster_lilygo_relay)
In `Secret.h` the following line must be present:

```
#define SECRET_MQTT_TLS 0
```
