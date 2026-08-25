# Authorization in Swagger

The API is protected with JWT. In Swagger UI, obtain a token via `/auth/login`, then pass it using the **Authorize** button.

## Open Swagger

After starting the application:

[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Get a token

1. Find the **`POST /auth/login`** endpoint.
2. Click **Try it out**.
3. Set the request body:

```json
{
  "deviseId": "admin",
  "password": "admin"
}
```

`admin` / `admin` is the `MAN` account created on startup if no users with that role exist yet. For other clients, use their `deviseId` and password.

4. Click **Execute**.
5. Copy the **`accessToken`** value from the response.

Example response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "deviseId": "admin",
  "role": "MAN"
}
```

## Pass the token in Swagger

1. At the top of the page, click **Authorize**.
2. In the `bearerAuth` field, paste **only** the `accessToken` — **without** the `Bearer` prefix.
3. Confirm (**Authorize** → **Close**).

Swagger will then automatically add the header:

```http
Authorization: Bearer <your accessToken>
```

to all protected requests.

## What the token grants access to

| Area | Requirement |
|------|-------------|
| `/producer/**`, `/consumer/**` | any valid JWT |
| `/admin/api/**` | JWT with role **`MAN`** |
| `/auth/login`, `/auth/isClientEnabled`, Swagger UI | no token required |

You can check the current session via **`GET /auth/me`** (requires a token already set in Authorize).

## If authorization does not work

- Make sure the token is pasted **without** the word `Bearer`.
- Check that the token has not expired (lifetime is set by `app.jwt.expiration-ms`, 24 hours by default).
- Admin API requires the `MAN` role; a regular producer/consumer will get `403 Forbidden`.
- If you get `401 Unauthorized` again — call `/auth/login` again and update the token in **Authorize**.
