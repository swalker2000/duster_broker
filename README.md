# Delivery Guarantee Service Running on Top of MQTT

- Stores messages like Kafka. Guarantees message delivery even if the device is offline at the time of sending.
- Monitors the message sending interval to the device. Sending many messages at once to an IoT device risks overflowing its input buffer. To avoid this, messages are delivered to the IoT device at a controlled interval.

## Service Workflow (message transmission from producer to consumer (consumer ID: {deviceId}))

1. Receives a message transmission command via MQTT on the topic `producer/request/{deviceId}`.  
   The message is of JSON type `ProducerMessageInDto`:

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

2. From `ProducerMessageInDto`, it creates a `ConsumerMessageOutDto`, which is assigned a unique ID:

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

3. Sends `ConsumerMessageOutDto` to the consumer on the topic `consumer/request/{deviceId}`.

4. If the `DeliveryGuarantee` enum is not `NO`, it waits for the consumer to return a `ConsumerMessageInDto` (with the same ID as the sent `ConsumerMessageOutDto`) on the topic `consumer/response/{deviceId}`:

   ```json
   {
     "id": 2
   }
   ```

5. If the message from the consumer is not received within the specified timeout, it returns to step 3.

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

## Example IoT Client

Example consumer based on ESP32: https://github.com/swalker2000/duster_esp32_example

To compile, create a file named `Secret.h` in the main sketch directory with the following content:

```c++
#define SSID          "MY_SSID"
#define WIFI_PASS     "MY_WIFI_PASS"
#define URL           "MQTT_URL"
#define PORT          8883
#define MQTT_USERNAME "MQTT_USERNAME"
#define MQTT_PASS     "MQTT_PASSWORD"
```

The ESP32 chip blinks an LED on command from the broker. It listens on the topic `consumer/request/device123`.

Message to turn on the LED on pin 13:  
Topic: `producer/request/device123`

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

Message to turn off the LED on pin 13:  
Topic: `producer/request/device123`

```json
{
  "believerGuarantee": "RECEIPT_CONFIRMATION",
  "command": "digitalWrite",
  "data": {
    "pinNumber": 13,
    "pinValue": false
  }
}
```