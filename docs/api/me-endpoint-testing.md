# Backend Identity Endpoint Testing Guide

## Overview
The `/api/me` endpoint has been implemented as a backend REST endpoint that validates JWT Bearer tokens and returns user identity information. This replaces the frontend-only `/api/auth/me` NextAuth route.

## Endpoint Details

**URL:** `GET /api/me`

**Authentication:** Bearer Token (JWT from Keycloak)

**Response:**
```json
{
  "sub": "user-id-from-keycloak",
  "email": "user@example.com",
  "roles": {
    "roles": ["CUSTOMER", "ADMIN"]
  }
}
```

---

## Testing Methods

### 1. Using Postman

1. **Get a valid access token** from Keycloak:
   - Use the existing authentication flow in your frontend
   - OR use Postman's OAuth 2.0 Authorization:
     - Grant Type: `Authorization Code with PKCE`
     - Auth URL: `http://localhost:8080/realms/eshop/protocol/openid-connect/auth`
     - Token URL: `http://localhost:8080/realms/eshop/protocol/openid-connect/token`
     - Client ID: `eshop-client`
     - Scope: `openid email profile`

2. **Make the request:**
   - Method: `GET`
   - URL: `http://localhost:8080/api/me`
   - Authorization Tab:
     - Type: `Bearer Token`
     - Token: Paste your access token

3. **Expected Response:** 200 OK with JSON containing `sub`, `email`, and `roles`

---

### 2. Using cURL (Command Line)

```bash
# Replace $TOKEN with your actual access token
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/me
```

**Windows PowerShell:**
```powershell
$token = "your-access-token-here"
curl -H "Authorization: Bearer $token" http://localhost:8080/api/me
```

---

### 3. Using Frontend JavaScript

#### Option A: With Fetch API
```javascript
// Assuming you have the access token from Keycloak
const accessToken = "your-access-token"; // Get from your auth context/state

fetch('http://localhost:8080/api/me', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${accessToken}`,
    'Content-Type': 'application/json'
  }
})
  .then(response => response.json())
  .then(data => {
    console.log('Backend User Identity:', data);
    // data will contain: { sub, email, roles }
  })
  .catch(error => {
    console.error('Error fetching user identity:', error);
  });
```

#### Option B: With Axios
```javascript
import axios from 'axios';

const accessToken = "your-access-token"; // Get from your auth context/state

axios.get('http://localhost:8080/api/me', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  }
})
  .then(response => {
    console.log('Backend User Identity:', response.data);
  })
  .catch(error => {
    console.error('Error fetching user identity:', error);
  });
```

#### Option C: React Component Example
```jsx
import { useEffect, useState } from 'react';

function UserProfile() {
  const [userInfo, setUserInfo] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Get access token from your auth provider (e.g., from session, context, etc.)
    const accessToken = getAccessToken(); // Implement this based on your auth setup

    if (accessToken) {
      fetch('/api/me', {
        headers: {
          'Authorization': `Bearer ${accessToken}`
        }
      })
        .then(res => {
          if (!res.ok) throw new Error('Failed to fetch user info');
          return res.json();
        })
        .then(data => setUserInfo(data))
        .catch(err => setError(err.message));
    }
  }, []);

  if (error) return <div>Error: {error}</div>;
  if (!userInfo) return <div>Loading...</div>;

  return (
    <div>
      <h2>User Profile</h2>
      <p><strong>ID:</strong> {userInfo.sub}</p>
      <p><strong>Email:</strong> {userInfo.email}</p>
      <p><strong>Roles:</strong> {JSON.stringify(userInfo.roles)}</p>
    </div>
  );
}
```

---

## Key Differences from `/api/auth/me`

| Feature | `/api/auth/me` (Old) | `/api/me` (New) |
|---------|---------------------|-----------------|
| **Location** | NextAuth frontend route | Spring Boot backend endpoint |
| **Authentication** | Session cookies | Bearer JWT token |
| **Identity Source** | NextAuth session | Keycloak JWT claims |
| **Usage** | Frontend-only | Frontend + Backend + External clients |
| **Validation** | Cookie-based | JWT signature validation |

---

## Troubleshooting

### 401 Unauthorized
- **Cause:** Missing or invalid Bearer token
- **Solution:** Ensure you're sending a valid access token in the Authorization header

### 403 Forbidden
- **Cause:** Token is valid but user doesn't have required permissions
- **Solution:** Check that the `/api/me` endpoint allows authenticated users (already configured)

### Token Expired
- **Cause:** Access token has expired
- **Solution:** Refresh the token using your Keycloak refresh token flow

### CORS Issues (from frontend)
- **Cause:** Frontend running on different origin (e.g., localhost:3000)
- **Solution:** CORS is already configured in `OAuth2SecurityConfig` to allow localhost:3000

---

## Implementation Files

1. **Controller:** `src/main/java/com/eshop/app/controller/MeController.java`
2. **Security Config:** `src/main/java/com/eshop/app/config/OAuth2SecurityConfig.java`

---

## Next Steps

1. ✅ Endpoint implemented and secured
2. ✅ Build successful
3. 🔄 Test with Postman or curl (get access token first)
4. 🔄 Update frontend to call `/api/me` instead of `/api/auth/me`
5. 🔄 Verify JWT validation works correctly

---

## Getting an Access Token for Testing

If you need to quickly get an access token for testing:

1. **Start your backend:** `./gradlew bootRun`
2. **Use the existing test script** (if available): `./test-keycloak-auth.ps1`
3. **Or manually login through frontend** and extract the token from browser DevTools
4. **Or use Keycloak Direct Access Grant** (Resource Owner Password Credentials):

```bash
curl -X POST http://localhost:8080/realms/eshop/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=eshop-client" \
  -d "grant_type=password" \
  -d "username=your-username" \
  -d "password=your-password"
```

This will return a JSON response with an `access_token` field. Use that token in the Authorization header.
