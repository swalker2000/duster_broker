# A delivery guarantee service operating over MQTT.
- enables interaction via both MQTT and REST API (for now, the feature is only available in the dev branch; documentation for it is available via Swagger at http://localhost:8080/swagger-ui.html).<br>
  For example, a temperature sensor can send readings via REST, while a relay receiving the data will receive it via MQTT.
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

## Service algorithm when a producer subscribes to changes in the delivery status of its message

1. Receives via MQTT a command to transmit a message in the topic `'producer/request/{deviceId}'`.  
   The message is of JSON type `ProducerMessageInDto`.
    - `messageBirthCertificate` – information about the message origin. If the field is missing or null, we do not inform the producer about the message.
        - `tmpId` – temporary message ID, not equal to 0.
        - `producerDeviseId` – producer ID, i.e., the device ID (`deviceId`) that generated the message.

2.
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

3. From `ProducerMessageInDto`, obtain `ConsumerMessageOutDto`; the latter is assigned a unique ID.

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

4. To the producer, in the topic `producer/response/{producerDeviceId}`, send `ProducerMessageOutDto` with the previously generated ID (in the `id` field).

```json
  {
    "id": 2,
    "tmpId": 3,
    "deliveryStatus": "NOT_DELIVERED"
  }
```

5. `ConsumerMessageOutDto` is sent to the consumer in the topic `'consumer/request/{deviceId}'`.

6. If the enum `DeliveryGuarantee` is not `NO`, we expect the consumer to return `ConsumerMessageInDto` (with the same ID as the `ConsumerMessageOutDto`) in the topic `'consumer/response/{deviceId}'`.  
   If the `deliveryStatus` field is missing or null, we consider the status `DELIVERED`.  
   If after some time we want to change the message status to `COMPLETED_WITH_ERROR` or `COMPLETED`, we do so in the same message.

```json
  {
    "id": 2,
    "deliveryStatus": "DELIVERED"
  }
```

7. If a message from the consumer is not received within a specified period, we return to step 3.  
   If it is received, we inform the producer in the topic `producer/response/{producerDeviceId}` with a `ProducerMessageOutDto` message.  
   All further changes to the `deliveryStatus` of the message are also reported in the same topic.

```json
  {
    "id": 2,
    "tmpId": 3,
    "deliveryStatus": "DELIVERED"
  }
```

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