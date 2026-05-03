# Keycloak Client Secret Update Guide & Troubleshooting

## Summary
If the backend application fails with `HTTP 401 Unauthorized` during Keycloak interactions (assigning roles, verifying tokens), it is most likely due to an invalid or rotated Client Secret for the `eshop-backend` client.

## Resolution Steps

### 1. Retrieve New Secret from Keycloak
1.  Log in to **Keycloak Admin Console** (http://localhost:8080/admin).
2.  Navigate to **Clients** -> **`eshop-backend`** -> **Credentials**.
3.  Click **Regenerate** (if needed) and copy the **Client Secret**.

### 2. Update Configuration Files (CRITICAL)
Due to profile precedence, you must ensure the secret is updated in **application-dev.properties** if running in the development profile (default).

#### A. Development Override (Highest Priority in Dev)
**File**: `src/main/resources/application-dev.properties`
**Property**: `keycloak.client-secret`
```properties
keycloak.client-secret=${KEYCLOAK_CLIENT_SECRET:YOUR_NEW_SECRET_HERE}
```

#### B. Base Configuration (Fallback)
**File**: `src/main/resources/application.properties`
**Property**: `keycloak.credentials.secret`
```properties
keycloak.credentials.secret=${ESHOP_BACKEND_CLIENT_SECRET:YOUR_NEW_SECRET_HERE}
```

### 3. Restart Application
After updating the `.properties` files, you **must restart the backend application** for changes to take effect.
```bash
./gradlew bootRun
```

## Best Practice: Environment Variables
To avoid editing files and committing secrets, set the environment variable on your deployment or local machine. This overrides all file-based configurations.

| Environment Variable | Description |
| :--- | :--- |
| `ESHOP_BACKEND_CLIENT_SECRET` | Used by `application.properties` |
| `KEYCLOAK_CLIENT_SECRET` | Used by `application-dev.properties` |

Setting both to the same secret value is recommended.
