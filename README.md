# Kisan Daily backend

Java 17+, Spring Boot 3.5, Spring Security, JPA, Flyway, PostgreSQL and an isolated H2 local profile. Authentication uses mobile/password with separate opaque access and refresh tokens. No OTP or fixed demo password is accepted.

## Structure

```text
src/main/java/com/kisandaily/
  config/             Security and CORS configuration
  controller/         Auth, user and health HTTP endpoints
  domain/             Authenticated principal and token-pair values
  dto/                Validated request bodies
  entity/             FarmerUser and AuthSession database entities
  exceptions/         Business exceptions and API error handling
  repository/         JPA queries and refresh-token locking
  response/           AuthResponse, UserResponse and ApiError
  security/           Bearer-token and authentication rate-limit filters
  service/            AuthService interface
    impl/             Transactional authentication implementation
  util/               Token generation, hashing and input validation
  KisanDailyApplication.java
src/main/resources/
  db/migration/       V1 farmer tables; V2 token sessions; V3 mobile/email
  application.properties
  application-local.properties
  application-postgres.properties
```

## Local run

```sh
./mvnw verify
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The local profile binds to `127.0.0.1:8080` and uses a persistent H2 database in the ignored `data/` folder. It explicitly overrides the PostgreSQL driver/dialect and does not connect to the PostgreSQL server in `application.properties`. Tests activate their own `test` profile and use a separate in-memory database.

The existing root PostgreSQL configuration is preserved. To run a deployed PostgreSQL environment use the `postgres` profile and `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` environment variables; provider/database secrets belong outside source control. Do not run migrations against a shared database without reviewing them first. Flyway baselines existing nonempty schemas at version 0, then creates the farmer/auth tables. Existing farmer rows are preserved; the former single-token sessions are not accepted by the new token protocol, so existing clients must log in once again. Hibernate validates the schema instead of modifying it.

## Register and login response

`POST /api/auth/register` returns 201. `POST /api/auth/login` returns 200. Both return the same complete response shape:

```json
{
  "accessToken": "<random-access-token>",
  "refreshToken": "<random-refresh-token>",
  "tokenType": "Bearer",
  "accessTokenExpiresAt": "<ISO-8601 UTC timestamp>",
  "refreshTokenExpiresAt": "<ISO-8601 UTC timestamp>",
  "user": {
    "id": "<immutable UUID>",
    "userId": "<internal reference>",
    "name": "Ramesh Patel",
    "email": "ramesh@example.com",
    "mobile": "9876543210",
    "phone": "9876543210",
    "phoneVerified": false,
    "village": "Rampur",
    "district": "Neemuch",
    "state": "Madhya Pradesh",
    "language": "हिंदी (Hindi)",
    "landArea": "2 acres",
    "selectedCrops": ["Wheat"],
    "profileCompleted": true
  }
}
```

All stored profile fields are returned; password/hash/token hashes are never user fields. Unfilled optional fields are empty/default values, not invented user details. Registration authenticates immediately and Flutter opens Home directly even when optional profile fields are incomplete. The farmer can complete/edit them from Home/Profile later.

Register body:

```json
{"name":"Ramesh Patel","email":"ramesh@example.com","mobile":"9876543210","password":"choose-your-password","confirmPassword":"choose-your-password"}
```

`name`, `mobile`, `password` and matching `confirmPassword` are required. `email` is optional: omitted, null, empty or whitespace is accepted; a supplied email must be valid and is normalized to lowercase. Email is not a login credential and does not need to be unique. Mobile is the unique login credential, canonicalized to an Indian 10-digit number; `+91` and common spacing are accepted. No OTP verification is performed.

Passwords are never trimmed and require at least 8 characters, maximum 72 UTF-8 bytes. Confirmation is validated on the backend and never stored or returned. Optional farmer fields (`village`, `district`, `state`, `language`, `landArea`, `selectedCrops`) may also be submitted; they are not a mandatory step before Home.

Login body:

```json
{"mobile":"9876543210","password":"choose-your-password"}
```

The old `userId` request field is no longer accepted in place of a mobile number. The legacy `user_id` database column remains an internal reference so existing records and UUID-based local data stay intact. V3 adds nullable `mobile_number` and `email` columns plus a unique mobile index, without deleting records. A legacy account with a unique matching contact phone is linked to mobile login only after its password is verified. Accounts with missing or shared legacy contact numbers require an administrator to resolve the number; the app never assigns an ambiguous number to an arbitrary account. Profile edits cannot silently change the login mobile.

## Endpoints

| Method | Endpoint | Credential / body | Response |
| --- | --- | --- | --- |
| GET | `/api/health` | None | `{status:"UP"}` |
| POST | `/api/auth/register` | Registration JSON | Full user + access/refresh pair |
| POST | `/api/auth/login` | Mobile/password | Full user + access/refresh pair |
| POST | `/api/auth/refresh` | `{refreshToken:"..."}` | Full user + rotated token pair |
| GET | `/api/auth/me` | `Authorization: Bearer <accessToken>` | Complete current user |
| PUT | `/api/users/me/profile` | Access token + profile JSON | Updated complete user |
| POST | `/api/auth/logout` | `{refreshToken:"..."}` | 204, revoke that login family |

Profile update requires name, village, district, state, language and 1–8 selected crops. Phone and landArea are optional. Error shape: `{code,message,fields}`. Statuses: 400 validation, 401 invalid/expired credentials, 409 duplicate user ID, 429 too many auth attempts.

## Token lifecycle

- Access lifetime: 15 minutes. Refresh lifetime: 30 days from login, without sliding extension. Both lifetimes are configurable.
- Tokens are independent cryptographically random 256-bit values. Database stores only SHA-256 hashes; passwords use salted BCrypt cost 12. These tokens are opaque, so a JWT signing secret is not needed.
- Refresh locks the submitted session row, rotates both tokens and invalidates the old pair in one transaction.
- Reuse of an already rotated refresh token revokes the whole login family. Other separately issued device/login families remain valid. The replay revocation commits even though the request returns 401.
- Logout uses the refresh credential so it also works after access expiry. It revokes the whole current login family. A syntactically valid unknown refresh token receives an idempotent 204.
- Refresh tokens cannot authorize protected endpoints, and access tokens cannot refresh a session.
- Expired refresh sessions are cleaned up hourly. No authentication cookies, HTTP Basic, default generated user or OTP endpoints are enabled.
- Default rate limit is 20 auth requests per remote address per minute per process. Multiple instances should use shared gateway rate limiting. Forwarded IP headers are not trusted automatically.

Settings: `app.access-token-minutes` (1–60), `app.refresh-token-days` (1–90), `app.auth-requests-per-minute`, `app.cors-origins` (also supports the existing `app.cors.allowed-origins`). Local CORS permits localhost/127.0.0.1 ports 8765 and 3000. Use HTTPS for deployed APIs. Password recovery is outside this change because no verified recovery channel was requested.

See `../shop/docs/AUTH_SETUP.md` for Flutter connection and USB/device instructions.

## Verified checks

18 backend tests pass, including refresh rotation, replay revocation, expiry, logout, user/profile isolation, validation and CORS. Local HTTP checks additionally verified the complete token lifecycle and preservation of pre-migration farmer/profile data. Flutter has 33 passing tests, including registration directly to Home, startup refresh, concurrent refresh sharing and logout during a delayed token write. Browser checks covered registration → Home and logout → password login → Home. PostgreSQL deployment/migrations were not executed against the configured remote database.

The Flutter register form contains name, optional email, required mobile, password and confirmation in that order. Login contains only mobile and password. Mobile validation, optional email, duplicate numbers, confirmation mismatch and safe legacy-account linking are covered by tests.
