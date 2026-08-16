# Keycloak Access Guide

## 📋 Quick Reference

| Setting | Value |
|---|---|
| **Image** | `quay.io/keycloak/keycloak:26.7.1` |
| **Admin Console** | http://127.0.0.1:8080/admin |
| **Admin Username** | `admin` |
| **Admin Password** | `Admin@@Secret123` |
| **Startup Time** | ~90 seconds after docker compose up |
| **Realm: Users** | `eshop` |
| **Realm: Admins** | `eshop-admin` |

---

## ⚠️ Important: Use 127.0.0.1, NOT localhost

Always open Keycloak using **http://127.0.0.1:8080/admin** in your browser.
Do NOT use http://localhost:8080.

**Why?** Windows resolves localhost to IPv6 (::1) first, hitting Docker's
WSL2 relay (wslrelay) in a broken state, causing ERR_EMPTY_RESPONSE.
Using 127.0.0.1 (IPv4) bypasses this and always works.

**Permanent fix** (run PowerShell as Administrator, one time only):
```
Add-Content -Path "C:\Windows\System32\drivers\etc\hosts" -Value "`n127.0.0.1`tlocalhost" -Encoding ASCII
```
After this, http://localhost:8080 will also work.

---

## ▶️ Start / Stop Commands

Run from G:\Project\eshop_back:

```powershell
# Start all dev services (postgres, redis, keycloak, pgadmin)
docker compose -f docker-compose-dev.yml up -d

# Stop and remove containers (keeps volumes/data)
docker compose -f docker-compose-dev.yml down

# Stop and remove containers + ALL DATA (full reset)
docker compose -f docker-compose-dev.yml down -v

# View live Keycloak logs
docker compose -f docker-compose-dev.yml logs -f keycloak

# Restart only Keycloak
docker compose -f docker-compose-dev.yml restart keycloak

# Check status of all services
docker compose -f docker-compose-dev.yml ps
```

---

## 🔐 Login Credentials

| Field | Value | Notes |
|---|---|---|
| **Username** | `admin` | Plain username — NOT an email address |
| **Password** | `Admin@@Secret123` | Set via KC_BOOTSTRAP_ADMIN_PASSWORD |

WARNING: Do NOT login with admin@gmail.com or any email format.

---

## 🌐 All Dev Service URLs

| Service | URL | Credentials |
|---|---|---|
| **Keycloak Admin** | http://127.0.0.1:8080/admin | admin / Admin@@Secret123 |
| **pgAdmin** | http://localhost:5050 | admin@eshop.com / admin |
| **PostgreSQL** | localhost:5432 | postgres / thamilu*884* |
| **Redis** | localhost:6379 | No password |

---

## 🌐 Realm Details

### eshop Realm
- Purpose: Marketplace users — Sellers and Customers
- OIDC: http://127.0.0.1:8080/realms/eshop/.well-known/openid-configuration
- Client Secret: eshop-backend-dev-secret

### eshop-admin Realm
- Purpose: Internal platform administration
- OIDC: http://127.0.0.1:8080/realms/eshop-admin/.well-known/openid-configuration
- Client Secret: eshop-admin-backend-dev-secret

---

## 🐛 Troubleshooting

### ERR_EMPTY_RESPONSE in browser
Use http://127.0.0.1:8080/admin instead of localhost:8080

### user_not_found on login
Username must be: admin (not an email address)

### Keycloak crashes on startup
Check logs: docker compose -f docker-compose-dev.yml logs keycloak
Cause: SMTP from field in realm JSON has unresolved placeholder.
Fixed in keycloak-import/eshop-admin-realm.json using format: ${env.VAR:default}

### Redirects going to port 80 instead of 8080
Ensure KC_HOSTNAME_URL: "http://localhost:8080" is in docker-compose-dev.yml
Do NOT use KC_HOSTNAME: localhost alone (omits port in generated links).

### Container healthy but page not accessible
Wait the full 90 seconds for Keycloak startup to complete.
Test: Invoke-WebRequest -Uri "http://127.0.0.1:8080/admin/" -UseBasicParsing

---

## 📦 Version History

| Version | Notes |
|---|---|
| 26.7.1 | Current. Stricter email validation. Use ${env.VAR:default} in realm JSON. |
| 26.5.2 | Previous version. |

Last updated: 2026-08-15
