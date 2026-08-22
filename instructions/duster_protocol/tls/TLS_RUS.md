# TLS для Duster Protocol

Шифрованный слушатель по умолчанию: порт **`9092`** (`DUSTER_PROTOCOL_TLS_PORT`).

Клиенту **не нужен свой сертификат**. Mutual TLS нет: устройство предъявляет `deviseId` и пароль внутри протокола. Клиенту нужен только **публичный сертификат сервера** (или CA, которым этот сертификат подписан), чтобы проверить, что он подключается к вашему брокеру.

Приватный ключ сервера клиентам **не раздают**.

## Откуда взять сертификат для клиентов

Берётся **тот же сертификат**, который слушает брокер на порту TLS.

Его нет «внутри репозитория по умолчанию». Если в конфиге `duster.protocol.tls.keystore` / `DUSTER_PROTOCOL_TLS_KEYSTORE` **пустой**, брокер при старте создаёт **эфемерный** self-signed сертификат в памяти. Файла нет, после перезапуска сертификат другой — раздавать устройствам его нельзя.

Для клиентов нужен **постоянный** PKCS12 на сервере и экспорт из него `.crt` / `.pem`.

## 1. Сгенерировать keystore для брокера

На машине, где есть JDK (`keytool`):

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

- `CN` и `SAN` должны совпадать с хостом, к которому подключаются клиенты (имя или IP брокера). Добавьте свои DNS/IP в `-ext SAN=...`.
- `alias` по умолчанию в коде: `duster`.
- Пароль затем задаётся в `duster.protocol.tls.keystore-password` / `DUSTER_PROTOCOL_TLS_KEYSTORE_PASSWORD`.

Файл `duster-protocol.p12` — это keystore **сервера** (ключ + сертификат). Его кладут только на брокер.

## 2. Экспортировать публичный сертификат для клиентов

Из того же PKCS12:

```bash
keytool -exportcert \
  -alias duster \
  -keystore duster-protocol.p12 \
  -storepass changeit \
  -rfc \
  -file duster-protocol.crt
```

`duster-protocol.crt` — PEM (`-----BEGIN CERTIFICATE-----`). Его копируют на устройства, в прошивку ESP32, в trust store Java-клиента и т.д.

Через OpenSSL:

```bash
openssl pkcs12 -in duster-protocol.p12 -nokeys -clcerts -passin pass:changeit -out duster-protocol.crt
```

## 3. Указать keystore брокеру

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

`keystore` — путь Spring Resource (`file:...` или `classpath:...`). Формат: **PKCS12**.

Docker Compose: смонтируйте файл в контейнер и задайте переменные:

```yaml
environment:
  DUSTER_PROTOCOL_TLS_KEYSTORE: file:/app/certs/duster-protocol.p12
  DUSTER_PROTOCOL_TLS_KEYSTORE_PASSWORD: changeit
volumes:
  - ./certs/duster-protocol.p12:/app/certs/duster-protocol.p12:ro
```

После смены keystore брокер нужно перезапустить.

## Снять сертификат с уже запущенного сервера

Если PKCS12 уже подключён и брокер слушает TLS:

```bash
openssl s_client -connect HOST:9092 -showcerts </dev/null 2>/dev/null \
  | openssl x509 -outform PEM > duster-protocol.crt
```

Так получают **текущий** сертификат слушателя. Если keystore пустой (эфемерный), этот файл перестанет быть валидным после рестарта брокера.

## Что класть на клиент

| Файл | Куда |
|------|------|
| `duster-protocol.crt` (PEM) | Клиенты, ESP32 (`WiFiClientSecure` / CA cert) |
| `duster-protocol.p12` | Только брокер |
| приватный ключ | Никуда, кроме брокера |

Java-клиент в этом репозитории (`ProducerTcp` / `ConsumerTcp`): `useTls = true`. Для своего CA передайте `sslSocketFactory` из trust store с `duster-protocol.crt`. Режим `insecureTls = true` сертификат не проверяет — только для отладки.

Сертификат от публичного CA (Let's Encrypt и т.п.) тоже подходит: в PKCS12 кладут цепочку сервера, клиентам раздают корневой (или промежуточный) CA, а не обязательно leaf-сертификат брокера.
