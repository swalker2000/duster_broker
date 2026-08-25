## 1. Сгенерировать сертификат сервера 
```bash
keytool -genkeypair \
  -alias duster \
  -keyalg RSA \
  -keysize 2048 \
  -validity 3650 \
  -keystore duster-protocol.p12 \
  -storetype PKCS12 \
  -storepass changeit \
  -dname "CN=localhost" \
  -ext "SAN=DNS:localhost,IP:127.0.0.1"
```
Получится файл `duster-protocol.p12`. Его положить в папку `cert` внутри проекта.
## 2. В текущий .env добавить строки

```bash
DUSTER_PROTOCOL_TLS_KEYSTORE=../cert/duster/duster-protocol.p12
DUSTER_PROTOCOL_TLS_KEYSTORE_PASSWORD=changeit
```

## 3. Сгенерировать сертификат клиента

```bash
keytool -exportcert \
  -alias duster \
  -keystore duster-protocol.p12 \
  -storepass changeit \
  -rfc \
  -file duster-protocol.crt
```

