# TLS for Duster Protocol

Encrypted listener default port: **`9092`** (`DUSTER_PROTOCOL_TLS_PORT`).

Clients **do not need their own certificate**. There is no mutual TLS: the device sends `deviseId` and password inside the protocol. Clients only need the **broker's public certificate** (or the CA that signed it) to verify they are talking to your broker.

Never distribute the server private key to clients.

## Where to get the client certificate

Use **the same certificate** the broker presents on the TLS port.

It is not shipped in the repository. If `duster.protocol.tls.keystore` / `DUSTER_PROTOCOL_TLS_KEYSTORE` is **empty**, the broker generates an **ephemeral** self-signed certificate in memory at startup. There is no file on disk, and the certificate changes after a restart — do not pin that on devices.

For clients you need a **persistent** PKCS12 on the server and an exported `.crt` / `.pem` from it.

## 1. Generate a keystore for the broker

With a JDK (`keytool`):

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

- `CN` and `SAN` must match the host clients connect to (broker DNS name or IP). Add your own names/IPs in `-ext SAN=...`.
- Default alias in code: `duster`.
- The password goes into `duster.protocol.tls.keystore-password` / `DUSTER_PROTOCOL_TLS_KEYSTORE_PASSWORD`.

`duster-protocol.p12` is the **server** keystore (private key + certificate). Keep it on the broker only.

## 2. Export the public certificate for clients

From the same PKCS12:

```bash
keytool -exportcert \
  -alias duster \
  -keystore duster-protocol.p12 \
  -storepass changeit \
  -rfc \
  -file duster-protocol.crt
```

`duster-protocol.crt` is PEM (`-----BEGIN CERTIFICATE-----`). Copy it onto devices, ESP32 firmware, a Java trust store, etc.

With OpenSSL:

```bash
openssl pkcs12 -in duster-protocol.p12 -nokeys -clcerts -passin pass:changeit -out duster-protocol.crt
```

## 3. Point the broker at the keystore

`application.yml`:

```yaml
duster:
  protocol:
    tls:
      enabled: true
      port: 9092
      keystore: file:/absolute/path/duster-protocol.p12
      keystore-password: changeit
```

`keystore` is a Spring Resource path (`file:...` or `classpath:...`). Format: **PKCS12**.

Docker Compose: mount the file and set:

```yaml
environment:
  DUSTER_PROTOCOL_TLS_KEYSTORE: file:/app/certs/duster-protocol.p12
  DUSTER_PROTOCOL_TLS_KEYSTORE_PASSWORD: changeit
volumes:
  - ./certs/duster-protocol.p12:/app/certs/duster-protocol.p12:ro
```

Restart the broker after changing the keystore.

## Pull the certificate from a running server

If the PKCS12 is already configured and TLS is listening:

```bash
openssl s_client -connect HOST:9092 -showcerts </dev/null 2>/dev/null \
  | openssl x509 -outform PEM > duster-protocol.crt
```

This captures the **current** listener certificate. If the keystore is empty (ephemeral), that file becomes invalid after a broker restart.

## What to put on the client

| File | Where |
|------|--------|
| `duster-protocol.crt` (PEM) | Clients, ESP32 (`WiFiClientSecure` / CA cert) |
| `duster-protocol.p12` | Broker only |
| private key | Broker only |

Java clients in this repo (`ProducerTcp` / `ConsumerTcp`): `useTls = true`. For your CA, pass an `sslSocketFactory` built from a trust store that contains `duster-protocol.crt`. `insecureTls = true` skips certificate checks — debug only.

A certificate from a public CA (Let's Encrypt, etc.) also works: put the server chain in the PKCS12 and give clients the root (or intermediate) CA rather than necessarily the broker leaf certificate.
