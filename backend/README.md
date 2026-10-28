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

`GET /` is a public status endpoint that confirms the API is running and lists the authentication routes. It does not serve the frontend application.

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

## GIS farm mapping API

The map UI uses the authenticated `/api/data/{table}` API. Farm reads are limited to farms owned by the signed-in user; administrators can read all farms. Creating plots and crops requires ownership of the associated farm (or the `ADMIN` role).

Supported GIS tables are `farms`, `plots`, and `crops`:

- `GET /api/data/farms`
- `GET /api/data/plots?farmId={farmId}`
- `GET /api/data/crops?farmId={farmId}`
- `POST /api/data/farms`
- `POST /api/data/plots`
- `POST /api/data/crops`

List requests accept optional `orderBy` (`created_at` or `name`), `ascending` (default `false`), and `limit` (maximum `5000`) query parameters. Farm, plot, and crop IDs are numeric. Plot boundaries are stored and returned as GeoJSON `Polygon` objects using `[longitude, latitude]` coordinates.

Example plot request:

```json
{
  "farm_id": 1,
  "name": "North Field",
  "area_acres": 2.4,
  "soil_ph": 6.5,
  "soil_type": "Red Sandy Soil",
  "boundary_geojson": {
    "type": "Polygon",
    "coordinates": [[[79.0, 16.0], [79.01, 16.0], [79.01, 16.01], [79.0, 16.01], [79.0, 16.0]]]
  }
}
```

Use the returned plot list to obtain the created plot ID before posting its crop record. Unknown table names return `404`; invalid field values return `400`; cross-farm access returns `403`.

## Verification

From the `backend` directory, run `mvn test`. The API tests use an in-memory H2 database and do not require a local MySQL server.
