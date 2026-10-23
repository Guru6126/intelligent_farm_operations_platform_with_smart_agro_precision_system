# Backend authentication

## Local configuration

The backend uses MySQL by default. Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` if your local connection differs from `jdbc:mysql://localhost:3306/smart_agro`.

Set `JWT_SECRET` to a Base64-encoded random value containing at least 32 bytes. For example, generate one with:

```sh
openssl rand -base64 32
```

Do not use a development secret in production. `JWT_EXPIRATION` accepts a Java duration (default `PT1H`), and `CORS_ALLOWED_ORIGINS` accepts a comma-separated list of frontend origins (default `http://localhost:5173`).

## Authentication API

All request and response bodies are JSON.

### Register

`POST /api/auth/register`

```json
{"name":"Farm Owner","email":"owner@example.com","password":"FarmPass123!"}
```

Returns `201 Created` with an access token and public user details. Public sign-up always creates a `FARMER`; clients cannot assign themselves `ADMIN`.

### Log in

`POST /api/auth/login`

```json
{"email":"owner@example.com","password":"FarmPass123!"}
```

Returns `200 OK` with the same response shape as registration. Invalid credentials return `401 Unauthorized`.

### Current user

`GET /api/auth/me` requires `Authorization: Bearer <accessToken>` and returns the authenticated user's public details.

Endpoints under `/api/admin/**` require an `ADMIN` role. Other API endpoints require a valid bearer token unless explicitly marked public. Input validation errors return `400 Bad Request`; duplicate email registration returns `409 Conflict`.

## Verification

From the `backend` directory, run `mvn test`. The auth API tests use an in-memory H2 database and do not require a local MySQL server.
