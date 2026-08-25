## 1. Generate a server certificate
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
This will produce a `duster-protocol.p12` file. Place it in the `cert` folder inside the project.

## 2. Add the following lines to the current .env file

```bash
DUSTER_PROTOCOL_TLS_KEYSTORE=../cert/duster/duster-protocol.p12
DUSTER_PROTOCOL_TLS_KEYSTORE_PASSWORD=changeit
```

## 3. Generate a client certificate

```bash
keytool -exportcert \
  -alias duster \
  -keystore duster-protocol.p12 \
  -storepass changeit \
  -rfc \
  -file duster-protocol.crt
```