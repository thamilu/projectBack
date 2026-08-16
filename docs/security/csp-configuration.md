# Content Security Policy (CSP) Configuration

To protect the Keycloak login page against Cross-Site Scripting (XSS), clickjacking, and data injection, you must enforce standard security headers at the server level.

## Recommended Realm Headers

Configure these headers in the Keycloak Admin Console under **Realm Settings ➔ Security Defenses ➔ Headers**:

### 1. Content-Security-Policy (CSP)
```http
default-src 'none'; script-src 'self' 'nonce-{KEYCLOAK_NONCE}'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; frame-ancestors 'none'; form-action 'self'; base-uri 'self';
```

* **`default-src 'none'`**: Restrict all requests by default.
* **`script-src 'self' 'nonce-{KEYCLOAK_NONCE}'`**: Allow scripts loaded from the origin, and dynamic inline handlers with server-provided cryptographic nonces.
* **`style-src 'self' 'unsafe-inline'`**: Allow compiled stylesheets from the origin, and inline styles (needed for animated orbs and dynamic positioning).
* **`img-src 'self' data:`**: Allow origin images and base64 SVGs.
* **`font-src 'self'`**: Allow locally hosted fonts.
* **`frame-ancestors 'none'`**: Prevent Clickjacking by blocking embedding inside `<iframe>` tags.
* **`form-action 'self'`**: Restrict submission actions strictly back to Keycloak.

### 2. X-Frame-Options
```http
DENY
```

### 3. X-Content-Type-Options
```http
nosniff
```

### 4. Referrer-Policy
```http
strict-origin-when-cross-origin
```

### 5. Permissions-Policy
```http
camera=(), microphone=(), geolocation=()
```
