# Авторизация в Swagger

API защищён JWT. В Swagger UI токен нужно получить через `/auth/login`, затем передать его через кнопку **Authorize**.

## Открыть Swagger

После запуска приложения:

[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Получить токен

1. Найдите метод **`POST /auth/login`**.
2. Нажмите **Try it out**.
3. Укажите тело запроса:

```json
{
  "deviseId": "admin",
  "password": "admin"
}
```

`admin` / `admin` — учётная запись с ролью `MAN`, которая создаётся при старте, если пользователей с этой ролью ещё нет. Для других клиентов используйте их `deviseId` и пароль.

4. Нажмите **Execute**.
5. В ответе скопируйте значение поля **`accessToken`**.

Пример ответа:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "deviseId": "admin",
  "role": "MAN"
}
```

## Передать токен в Swagger

1. Вверху страницы нажмите **Authorize**.
2. В поле для `bearerAuth` вставьте **только** `accessToken` — **без** префикса `Bearer`.
3. Подтвердите (**Authorize** → **Close**).

После этого Swagger будет автоматически добавлять заголовок:

```http
Authorization: Bearer <ваш accessToken>
```

ко всем защищённым запросам.

## Что доступно с токеном

| Область | Требование |
|--------|------------|
| `/producer/**`, `/consumer/**` | любой валидный JWT |
| `/admin/api/**` | JWT с ролью **`MAN`** |
| `/auth/login`, `/auth/isClientEnabled`, Swagger UI | без токена |

Проверить текущую сессию можно через **`GET /auth/me`** (нужен уже введённый токен).

## Если авторизация не работает

- Убедитесь, что токен вставлен **без** слова `Bearer`.
- Проверьте, что токен не истёк (срок задаётся `app.jwt.expiration-ms`, по умолчанию 24 часа).
- Для админ-API нужна роль `MAN`; обычный producer/consumer получит `403 Forbidden`.
- Если снова получили `401 Unauthorized` — выполните `/auth/login` заново и обновите токен в **Authorize**.
