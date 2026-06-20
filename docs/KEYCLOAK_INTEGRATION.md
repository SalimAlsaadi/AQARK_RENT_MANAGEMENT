 # Keycloak Integration

Authentication & authorization for the AQARK Rent Management backend are handled by
[Keycloak](https://www.keycloak.org/) using the **BFF (Backend-for-Frontend)** pattern. The
Angular SPA never talks to Keycloak directly: it drives login through the Spring Boot backend,
which runs the OIDC authorization-code flow, keeps the tokens **server-side in the HTTP
session**, and hands the browser a session cookie. The backend also keeps a **stateless
Bearer-JWT resource server** for non-browser / machine-to-machine clients.

- **Keycloak version:** 24.0.3 (custom image, see `docker/Dockerfile.keycloak`)
- **Realm:** `AQARK-realm`
- **Client:** `spring-boot-app` (confidential, client-secret auth, S256 PKCE)
- **Keycloak DB:** PostgreSQL 16
- **Frontend (dev):** Angular at `http://localhost:4200` (`app.frontend.url`)

---

## 1. Architecture

Two cooperating Spring Security filter chains protect the same URLs; routing is by the
presence of an `Authorization: Bearer` header.

```
   Angular SPA (http://localhost:4200)
        │
        │  1. full-page nav GET /api/auth/login
        ▼
   ┌──────────────────────────────────┐        2. 302 → Keycloak login
   │ Spring Boot app  :8008 (HTTPS)   │──────────────────────────────────┐
   │  Chain 2: oauth2Login (session)  │                                   ▼
   │  Chain 1: Bearer JWT (M2M)       │                            ┌────────────┐
   │  Rate limiting + Request IDs     │◀───────────────────────────│ keycloak   │
   └──────────────────────────────────┘   4. 302 callback w/ code  │ :8443      │
        │        ▲                          /login/oauth2/code/...  └─────┬──────┘
        │        │ 5. exchange code → tokens (kept in session)           │
        │        └──────────────────────────────────────────────────────┘
        │  6. 302 → http://localhost:4200  + Set-Cookie: JSESSIONID
        ▼                                              │
   Angular SPA  ── 7. /api/** XHR with JSESSIONID ─────┘  (cookie sent automatically)
```

**Login flow (BFF):**
1. Angular sends the **browser** (full-page navigation, not XHR) to `/api/auth/login`.
2. The backend redirects to Keycloak's login page (authorization-code flow + PKCE).
3. The user authenticates on Keycloak.
4. Keycloak redirects back to the **backend** callback (`/login/oauth2/code/keycloak`) with the code.
5. The backend exchanges the code for tokens and stores them in the **server-side session**.
6. The backend issues a `JSESSIONID` cookie and redirects the browser to `app.frontend.url`.
7. Every later Angular `/api/**` call carries the cookie automatically (`withCredentials`).

**Two chains:**

| | Chain 1 — Bearer JWT | Chain 2 — BFF (browser/SPA) |
|---|---|---|
| Matches | requests with `Authorization: Bearer …` | everything else |
| State | stateless | HTTP session |
| Auth | validates JWT against Keycloak JWKS | `oauth2Login()` + session cookie |
| Unauthenticated | 401 JSON | 401 JSON (no redirect on XHR) |
| CSRF | disabled (token-based) | enabled (`XSRF-TOKEN` cookie) |
| For | M2M / API clients | Angular |

> **Why 401 and not a redirect on the API?** A browser XHR cannot follow the cross-origin
> redirect to Keycloak. So `/api/**` returns `401 JSON`; Angular detects it and triggers a
> full-page navigation to `/api/auth/login` to start the flow.

---

## 2. Running it (Docker)

The stack lives under `docker/`. It is **separate** from the Spring Boot app — start
Keycloak first, then run the app from your IDE / `mvn`.

```bash
cd docker
cp .env.example .env          # first time only — fill in real secrets
docker compose up -d --build
docker compose ps             # wait until keycloak is "healthy" (~1-2 min)
docker compose logs -f keycloak
```

Tear down:

```bash
docker compose down           # keep data
docker compose down -v        # also wipe postgres + keycloak volumes (fresh realm import)
```

> The realm is imported on startup via `--import-realm` from `docker/keycloak/realm-export.json`.
> Changes to that file only take effect on a **fresh** start (`down -v`), because an
> already-imported realm is not re-imported.

### Environment variables (`docker/.env`)

| Variable | Purpose | Example |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Keycloak's database | `keycloak` / `keycloak` / `keycloak_pass` |
| `KC_ADMIN` / `KC_ADMIN_PASSWORD` | Keycloak master admin login | `admin` / `admin_pass` |
| `KC_HOSTNAME` | Must match the TLS cert SAN | `localhost` |
| `KC_HTTPS_PORT` | Published HTTPS port | `8443` |

### TLS

Keycloak runs **HTTPS-only** (`KC_HTTP_ENABLED=false`, `sslRequired=all`). Certs are bind-mounted:

- `docker/certs/keycloak.local+2.pem` → `tls.crt`
- `docker/certs/keycloak.local+2-key.pem` → `tls.key`

Generate dev certs with `docker/scripts/generate-certs.sh` (mkcert). Because they're
self-signed, the browser will warn on `localhost`; the Spring app skips cert verification
**only** for the Keycloak token WebClient (see §6) — JWT signatures are still verified.

---

## 3. Important URLs

Realm base: `https://localhost:8443/realms/AQARK-realm`

| Purpose | URL |
|---|---|
| Admin console | `https://localhost:8443/admin/` |
| User account console | `https://localhost:8443/realms/AQARK-realm/account` |
| Login (authorization endpoint) | `https://localhost:8443/realms/AQARK-realm/protocol/openid-connect/auth` |
| Token endpoint | `…/protocol/openid-connect/token` |
| Logout endpoint | `…/protocol/openid-connect/logout` |
| JWKS (public keys) | `…/protocol/openid-connect/certs` |
| UserInfo | `…/protocol/openid-connect/userinfo` |
| OIDC discovery | `…/.well-known/openid-configuration` |

---

## 4. Realm configuration (`docker/keycloak/realm-export.json`)

**Client `spring-boot-app`**
- Confidential (`publicClient: false`, `client-secret` auth), `bearerOnly: false`
- Enabled flows: standard (auth-code, used by the BFF) + direct access grants (for tests) + service accounts
- PKCE `S256` enforced — Spring enables PKCE explicitly on the authorization request
- Redirect URIs: `https://localhost:8443/*`, `https://localhost:8008/*` (covers the backend callback)
- Post-logout redirect URIs: also includes `http://localhost:4200/*` (the frontend)
- Access token lifespan: **300s**; SSO idle 1800s / max 28800s
- **Dev client secret** pinned to `change-me-in-keycloak-admin` (matches `KEYCLOAK_CLIENT_SECRET`) so a fresh import works without copying the generated secret. Change for any shared/non-local use.
- **Protocol mappers:** an *audience* mapper adds `spring-boot-app` to the `aud` claim (Bearer chain validates it), and a *realm roles* mapper writes `realm_access.roles` into **both** the access token and the **ID token** (the BFF reads roles from the ID token).
- **Scope mappings:** the client is granted `ROLE_USER` / `ROLE_ADMIN` / `ROLE_ACTUATOR` (with `fullScopeAllowed:false`, only these realm roles appear in tokens — least privilege).

**Realm roles** (default assigned: `ROLE_USER`)

| Role | Meaning | Guards |
|---|---|---|
| `ROLE_USER` | Standard authenticated user | `/api/**` |
| `ROLE_ADMIN` | Full admin access | `/api/admin/**`, `/api/users/**` |
| `ROLE_ACTUATOR` | Spring Actuator endpoints | `/actuator/**` |

> Roles are named with the `ROLE_` prefix in the realm (`ROLE_ADMIN` etc.). Both authority
> mappers (`KeycloakJwtRolesConverter` for the Bearer chain, `KeycloakOidcAuthoritiesMapper`
> for the BFF chain) use the role name **as-is** as the Spring authority, so `hasRole("ADMIN")`
> (which expects authority `ROLE_ADMIN`) matches correctly.

**Security hardening in the realm**
- Brute-force protection on (lock after 5 failures, 900s max wait)
- Strong password policy: length 12, upper/lower/digit/special, not-username
- TOTP OTP policy configured (HmacSHA1, 6 digits, 30s)
- `verifyEmail: true`, `registrationAllowed: false`, `resetPasswordAllowed: false`
- Audit events enabled (login, token, admin events)

> The realm export ships **no users**. Create them in the admin console (or via the admin
> API / a future seed) and assign roles before logging in.

---

## 5. Spring Boot side

### Configuration (`application.properties`)

```properties
# Resource server — validate incoming Bearer JWTs (Chain 1, M2M)
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://localhost:8443/realms/AQARK-realm
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://localhost:8443/realms/AQARK-realm/protocol/openid-connect/certs

# Client — authorization-code flow used by the BFF (Chain 2)
spring.security.oauth2.client.registration.keycloak.client-id=spring-boot-app
spring.security.oauth2.client.registration.keycloak.client-secret=${KEYCLOAK_CLIENT_SECRET:change-me-in-env}
spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/keycloak

# BFF / SPA
app.frontend.url=http://localhost:4200
cors.allowed-origins=https://localhost:8443,https://localhost:8008,http://localhost:4200

# Session cookie must be SameSite=None + Secure (cross-scheme: http:4200 → https:8008)
server.servlet.session.cookie.same-site=None
server.servlet.session.cookie.secure=true

# Dev: trust the mkcert CA that signed Keycloak's cert (absent in prod → skipped)
keycloak.tls.ca-cert=file:docker/certs/mkcert-rootCA.pem
```

> Run the backend with the required env vars, e.g.
> `KEYCLOAK_CLIENT_SECRET=change-me-in-keycloak-admin SSL_KEYSTORE_PASSWORD=changeit ./mvnw spring-boot:run`

> **Secrets:** `KEYCLOAK_CLIENT_SECRET` (and `SSL_KEYSTORE_PASSWORD`) must be supplied as
> environment variables. The literal `change-me-in-env` default will fail real auth.

### Key classes (`…/Security/`)

| Class | Responsibility |
|---|---|
| `SecurityConfig` | Two filter chains. **Chain 1** (`@Order(1)`, matches `Authorization: Bearer …`): stateless JWT resource server. **Chain 2** (`@Order(2)`, everything else): `oauth2Login()` BFF with PKCE, session token storage, success handler → `app.frontend.url`, OIDC logout, CSRF cookie. Both: CORS, security headers, 401/403 JSON, request-id + rate-limit filters. Method security enabled. |
| `JwtConfig` | Builds the `JwtDecoder` with **issuer + audience (`aud` contains `spring-boot-app`) + nbf** validators (used by Chain 1). |
| `KeycloakJwtRolesConverter` | Chain 1: extracts roles from `realm_access.roles` + `resource_access.<client>.roles` → authorities (name as-is). |
| `KeycloakOidcAuthoritiesMapper` | Chain 2: maps `realm_access.roles` from the OIDC user → authorities (name as-is), so both chains yield identical roles. |
| `CsrfCookieFilter` | Forces the `XSRF-TOKEN` cookie to be emitted so Angular can echo it as `X-XSRF-TOKEN`. |
| `KeycloakTlsTrustConfig` | **Dev only.** Adds the mkcert root CA (`keycloak.tls.ca-cert`) to the JVM trust store so all Spring→Keycloak HTTPS calls (JWKS, token exchange, userinfo, ID-token) validate. Skipped if the cert file is absent (prod). |
| `KeycloakWebClientConfig` | `keycloakWebClient` bean used by `UserServicesImpl` for admin calls. |
| `RateLimitingFilter` | Token-bucket, 100 req / 60s, keyed by authenticated principal (falls back to client IP). Adds `X-Rate-Limit-*` headers, returns 429. |
| `RequestIdFilter` | Assigns/propagates `X-Request-ID` and puts it in the MDC for logging. |

### Authorization rules (both chains)

```
/api/auth/login                        → permitAll  (Chain 2: starts the login redirect)
/oauth2/authorization/**, /login/oauth2/code/**  → permitAll  (Chain 2: OIDC flow)
/actuator/health, /actuator/health/**  → permitAll
/actuator/**                           → hasRole("ACTUATOR")
/api/admin/**                          → hasRole("ADMIN")
/api/**                                → authenticated
anyRequest                             → authenticated (Chain 2) / denyAll (Chain 1)
```

---

## 6. The `/api/auth` BFF API

Implemented by `AuthController` plus Spring Security's `oauth2Login()` / `logout()` config.
Login and logout must be reached by **full-page browser navigation** (not XHR), because the
OIDC flow involves cross-origin redirects to Keycloak that an XHR cannot follow.

### GET `/api/auth/login`
Starts login. Redirects the browser into the Keycloak authorization-code flow
(`/oauth2/authorization/keycloak`). After successful login the backend stores the tokens in
the session and redirects the browser to `app.frontend.url` with a `JSESSIONID` cookie set.

### GET `/api/auth/me`
Returns the current user so the SPA can check login state. Requires the session cookie.
```jsonc
// 200
{ "username": "alice", "email": "alice@example.com", "name": "Alice", "roles": ["ROLE_USER"] }
// 401 when there is no session
```

### GET `/api/auth/logout`
Invalidates the session, clears the `JSESSIONID` cookie, performs RP-initiated logout at
Keycloak, and redirects the browser back to `app.frontend.url`.

### Calling protected endpoints

- **From Angular (BFF):** normal XHR with credentials — the `JSESSIONID` cookie authenticates.
  Send `X-XSRF-TOKEN` (read from the `XSRF-TOKEN` cookie) on mutating requests.
- **From M2M / API clients (Chain 1):** `Authorization: Bearer <access_token>`.

---

## 7. Quick smoke test

Prereqs: create a user in the Keycloak admin console with a password meeting the policy
(≥12 chars, upper/lower/digit/special, not the username) and assign realm role `ROLE_USER`
(plus `ROLE_ADMIN` for admin tests). The realm ships **no users**.

**BFF login redirect (the main flow):**
```
# In a browser, navigate to:
https://localhost:8008/api/auth/login
# → 302 to the Keycloak login page → log in →
# → 302 back to http://localhost:4200, with a JSESSIONID cookie set on localhost:8008
```

**Session API:**
```bash
# Without a session → 401 JSON
curl -k https://localhost:8008/api/auth/me
# With the browser session cookie → 200 with the user JSON
```

**Bearer chain still works (M2M):**
```bash
# Obtain a token straight from Keycloak, then call a protected endpoint:
curl -k https://localhost:8008/api/... -H "Authorization: Bearer <access_token>"
```

`-k` skips cert verification for the self-signed dev certs.

---

## 8. Troubleshooting

| Symptom | Likely cause |
|---|---|
| `/api/**` returns `401` instead of redirecting | By design — the API never redirects. Drive login via full-page nav to `/api/auth/login`. |
| `401` with a valid Bearer token | `aud` claim missing `spring-boot-app` (audience mapper), or issuer/clock mismatch |
| `403` despite being logged in | Missing realm role; check `hasRole(...)` vs the realm role name (`ROLE_ADMIN`) |
| Login fails / `invalid_grant` PKCE error | Keycloak client enforces S256 PKCE; the BFF enables it via the PKCE request resolver |
| `PKIX path building failed` calling Keycloak | JVM doesn't trust Keycloak's mkcert cert. `KeycloakTlsTrustConfig` adds the CA from `keycloak.tls.ca-cert` (default `docker/certs/mkcert-rootCA.pem`); ensure that file exists (`cp "$(mkcert -CAROOT)/rootCA.pem" docker/certs/mkcert-rootCA.pem`) |
| Logged-in user has no roles | `realm_access` must be in the **ID token** (the *realm roles* client mapper) and the client must be granted the roles via scope mappings (or `fullScopeAllowed:true`); re-import with `down -v` |
| Angular not authenticated after login | Session cookie blocked — needs `SameSite=None; Secure` (set) and CORS `allowCredentials` + the Angular origin in `cors.allowed-origins`. Easiest: use an Angular dev proxy so it's same-origin. |
| CSRF `403` on POST/PUT/DELETE | Angular must send `X-XSRF-TOKEN` (auto from the `XSRF-TOKEN` cookie via `HttpClient`) |
| Keycloak container unhealthy | Still starting (120s start period), cert files missing, or Postgres not healthy |
| Realm changes ignored | Already imported — run `docker compose down -v` for a fresh import |
| Browser cert warning | Self-signed cert for `keycloak.local`; add a hosts entry or accept the warning |

---

## 9. Angular dev setup (reference)

Simplest dev configuration is a same-origin proxy so there are no CORS/SameSite concerns —
`proxy.conf.json`:
```json
{
  "/api":    { "target": "https://localhost:8008", "secure": false, "changeOrigin": true },
  "/oauth2": { "target": "https://localhost:8008", "secure": false, "changeOrigin": true },
  "/login":  { "target": "https://localhost:8008", "secure": false, "changeOrigin": true }
}
```
Run with `ng serve --proxy-config proxy.conf.json`. In Angular, enable
`withXsrfConfiguration`/`provideHttpClient(withXsrfConfiguration(...))` and call APIs with
`withCredentials: true`. On a `401` from `/api/**`, navigate the browser to `/api/auth/login`.
