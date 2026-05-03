# Complete Keycloak Integration Setup Guide

## 🚀 Quick Start

This guide walks you through setting up Keycloak authentication for your EShop application running on port 8082.

---

## 📋 Prerequisites

- Java 21+
- Docker (for running Keycloak)
- Gradle
- Node.js (for frontend)

---

## 🐳 Step 1: Start Keycloak with Docker

### Option 1: Docker Command
```bash
docker run -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest start-dev
```

### Option 2: Docker Compose
Create `docker-compose-keycloak.yml`:
```yaml
version: '3.8'

services:
  keycloak:
    image: quay.io/keycloak/keycloak:latest
    container_name: keycloak-server
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_HTTP_PORT: 8080
    command: start-dev
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health/ready"]
      interval: 30s
      timeout: 10s
      retries: 5
```

Start Keycloak:
```bash
docker-compose -f docker-compose-keycloak.yml up -d
```

Wait for Keycloak to start (about 30-60 seconds), then access:
- **Admin Console**: http://localhost:8080
- **Login**: admin / admin

---

## ⚙️ Step 2: Configure Keycloak

### 1. Create a Client

1. Go to **Clients** → **Create Client**
2. **Client ID**: `eshop-client`
3. **Client Protocol**: `openid-connect`
4. Click **Next**

### 2. Configure Client Settings

**Capability config**:
- ✅ Client authentication: **ON**
- ✅ Authorization: **OFF**
- ✅ Authentication flow:
  - ✅ Standard flow (Authorization Code)
  - ✅ Direct access grants (Password Grant)
  - ✅ Service accounts roles (Client Credentials)

Click **Next**

### 3. Set Redirect URIs

**Login settings**:
- **Root URL**: `http://localhost:8082`
- **Valid redirect URIs**: 
  - `http://localhost:8082/*`
  - `http://localhost:3000/*`
  - `http://localhost:4200/*`
- **Valid post logout redirect URIs**: `+`
- **Web origins**: `*` (or specific origins for production)

Click **Save**

### 4. Get Client Secret

1. Go to **Credentials** tab
2. Copy the **Client Secret**
3. Update `src/main/resources/application-keycloak.properties`:
   ```properties
   keycloak.client-secret=YOUR_COPIED_SECRET_HERE
   ```

### 5. Create Test Users

**Create User 1 (Admin)**:
1. Go to **Users** → **Add user**
2. Fill in:
   - **Username**: `admin`
   - **Email**: `admin@eshop.com`
   - **First name**: `Admin`
   - **Last name**: `User`
   - **Email verified**: ✅ ON
3. Click **Create**
4. Go to **Credentials** tab
   - Set password: `admin123`
   - **Temporary**: ❌ OFF
   - Click **Set Password**
5. Go to **Role Mapping** tab
   - Click **Assign role**
   - Select **admin** (realm role)

**Create User 2 (Customer)**:
1. **Username**: `customer`
2. **Email**: `customer@eshop.com`
3. **Password**: `customer123`
4. Assign **CUSTOMER** role (or default roles)

**Create User 3 (Seller)**:
1. **Username**: `seller`
2. **Email**: `seller@eshop.com`
3. **Password**: `seller123`
4. Assign **SELLER** role

### 6. Create Realm Roles (if not exist)

1. Go to **Realm roles** → **Create role**
2. Create these roles:
   - `ADMIN`
   - `CUSTOMER`
   - `SELLER`
   - `DELIVERY_AGENT`

---

## 🏗️ Step 3: Build and Run the Application

### 1. Build the application
```bash
./gradlew clean build
```

### 2. Run with Keycloak profile
```bash
./gradlew bootRun --args='--spring.profiles.active=keycloak,oauth2'
```

Or with environment variable:
```bash
export SPRING_PROFILES_ACTIVE=keycloak,oauth2
./gradlew bootRun
```

The application will start on **http://localhost:8082**

---

## 🧪 Step 4: Test the Authentication

### Using cURL

#### 1. Login with Username/Password
```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }' | jq
```

**Expected Response**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "token_type": "Bearer",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "scope": "openid profile email"
}
```

#### 2. Get User Info
```bash
# Save the access token
ACCESS_TOKEN="paste-your-access-token-here"

curl -X GET http://localhost:8082/api/auth/userinfo \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq
```

#### 3. Get Current User (from JWT)
```bash
curl -X GET http://localhost:8082/api/auth/me \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq
```

#### 4. Introspect Token
```bash
curl -X POST http://localhost:8082/api/auth/introspect \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq
```

#### 5. Refresh Token
```bash
REFRESH_TOKEN="paste-your-refresh-token-here"

curl -X POST http://localhost:8082/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" | jq
```

#### 6. Logout
```bash
curl -X POST http://localhost:8082/api/auth/logout \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" | jq
```

#### 7. Get OAuth2 Login URL
```bash
curl http://localhost:8082/api/auth/login-url?redirectUri=http://localhost:3000/callback | jq
```

### Using Postman

1. Import the collection (create new requests):
   - **Login**: POST `http://localhost:8082/api/auth/login`
   - **Get User**: GET `http://localhost:8082/api/auth/me` (with Bearer token)
   - **Logout**: POST `http://localhost:8082/api/auth/logout`

---

## 🎨 Frontend Integration

### React/Next.js Example

#### Install Axios
```bash
npm install axios
```

#### Create Auth Service (`lib/keycloakService.js`)
```javascript
import axios from 'axios';

const API_URL = 'http://localhost:8082/api/auth';

class KeycloakService {
  async login(username, password) {
    const response = await axios.post(`${API_URL}/login`, {
      username,
      password
    });
    
    const tokens = response.data;
    localStorage.setItem('access_token', tokens.access_token);
    localStorage.setItem('refresh_token', tokens.refresh_token);
    localStorage.setItem('id_token', tokens.id_token);
    
    return tokens;
  }

  async getLoginUrl() {
    const response = await axios.get(
      `${API_URL}/login-url?redirectUri=${window.location.origin}/callback`
    );
    return response.data;
  }

  async handleCallback(code) {
    const redirectUri = `${window.location.origin}/callback`;
    const response = await axios.get(
      `${API_URL}/callback?code=${code}&redirectUri=${redirectUri}`
    );
    
    const tokens = response.data;
    localStorage.setItem('access_token', tokens.access_token);
    localStorage.setItem('refresh_token', tokens.refresh_token);
    
    return tokens;
  }

  async getCurrentUser() {
    const token = localStorage.getItem('access_token');
    const response = await axios.get(`${API_URL}/me`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    return response.data;
  }

  async refreshToken() {
    const refreshToken = localStorage.getItem('refresh_token');
    const response = await axios.post(`${API_URL}/refresh`, {
      refreshToken
    });
    
    const tokens = response.data;
    localStorage.setItem('access_token', tokens.access_token);
    localStorage.setItem('refresh_token', tokens.refresh_token);
    
    return tokens;
  }

  async logout() {
    const refreshToken = localStorage.getItem('refresh_token');
    await axios.post(`${API_URL}/logout`, { refreshToken });
    
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('id_token');
  }

  getAccessToken() {
    return localStorage.getItem('access_token');
  }
}

export default new KeycloakService();
```

#### Login Page (`pages/login.js`)
```jsx
import { useState } from 'react';
import keycloakService from '../lib/keycloakService';
import { useRouter } from 'next/router';

export default function Login() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const router = useRouter();

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      await keycloakService.login(username, password);
      router.push('/dashboard');
    } catch (err) {
      setError('Login failed. Check your credentials.');
    }
  };

  const handleOAuthLogin = async () => {
    const { authorizationUrl, state } = await keycloakService.getLoginUrl();
    sessionStorage.setItem('oauth_state', state);
    window.location.href = authorizationUrl;
  };

  return (
    <div className="login-container">
      <h1>Login</h1>
      
      <form onSubmit={handleLogin}>
        <input
          type="text"
          placeholder="Username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
        />
        <input
          type="password"
          placeholder="Password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button type="submit">Login</button>
      </form>

      <div>
        <p>Or</p>
        <button onClick={handleOAuthLogin}>
          Login with Keycloak
        </button>
      </div>

      {error && <p className="error">{error}</p>}
    </div>
  );
}
```

#### Callback Page (`pages/callback.js`)
```jsx
import { useEffect } from 'react';
import { useRouter } from 'next/router';
import keycloakService from '../lib/keycloakService';

export default function Callback() {
  const router = useRouter();

  useEffect(() => {
    const handleCallback = async () => {
      const { code, state } = router.query;
      
      if (code && state) {
        const savedState = sessionStorage.getItem('oauth_state');
        
        if (state !== savedState) {
          console.error('State mismatch!');
          router.push('/login?error=state_mismatch');
          return;
        }

        try {
          await keycloakService.handleCallback(code);
          sessionStorage.removeItem('oauth_state');
          router.push('/dashboard');
        } catch (error) {
          console.error('Callback error:', error);
          router.push('/login?error=callback_failed');
        }
      }
    };

    if (router.isReady) {
      handleCallback();
    }
  }, [router]);

  return <div>Processing login...</div>;
}
```

---

## 📊 API Endpoints Reference

### Public Endpoints (No Authentication)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/login` | Login with username/password |
| POST | `/api/auth/refresh` | Refresh access token |
| GET | `/api/auth/login-url` | Get OAuth2 authorization URL |
| GET | `/api/auth/callback` | OAuth2 callback handler |
| GET | `/api/auth/config` | Get OpenID configuration |
| GET | `/api/auth/error` | Error handler |

### Protected Endpoints (Authentication Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/auth/userinfo` | Get user info from token |
| GET | `/api/auth/me` | Get current user from JWT |
| POST | `/api/auth/introspect` | Validate token |
| POST | `/api/auth/logout` | Logout user |

### Admin Endpoints (Admin Role Required)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/users` | Create user |
| GET | `/api/admin/users` | Get all users |
| GET | `/api/admin/users/{username}` | Get user by username |
| DELETE | `/api/admin/users/{userId}` | Delete user |
| PUT | `/api/admin/users/{userId}/reset-password` | Reset password |
| PUT | `/api/admin/users/{userId}` | Update user |

---

## 🔍 Troubleshooting

### Issue: "Invalid client credentials"
**Solution**: 
1. Verify client secret in `application-keycloak.properties`
2. Ensure client authentication is enabled in Keycloak

### Issue: "401 Unauthorized" when accessing protected endpoints
**Solution**:
1. Check token expiration
2. Verify JWT issuer URI matches Keycloak realm
3. Check if JWK set URI is accessible

### Issue: "CORS error" from frontend
**Solution**:
1. Add frontend origin to `cors.allowed-origins` in properties
2. Configure Web Origins in Keycloak client settings

### Issue: Token missing roles
**Solution**:
1. Verify realm roles are assigned to user
2. Check role mappers in Keycloak client settings
3. Ensure `realm_access.roles` claim exists in token

---

## 🎯 Production Checklist

- [ ] Use HTTPS for all URLs
- [ ] Change Keycloak admin password
- [ ] Use strong client secret
- [ ] Configure proper redirect URIs (no wildcards)
- [ ] Set specific CORS origins
- [ ] Enable token rotation
- [ ] Set appropriate token lifetimes
- [ ] Enable MFA for admin accounts
- [ ] Use PostgreSQL for Keycloak (not H2)
- [ ] Monitor failed login attempts
- [ ] Set up logging and monitoring

---

## 📚 Additional Resources

- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Security OAuth2](https://spring.io/projects/spring-security-oauth)
- [OpenID Connect Specification](https://openid.net/connect/)

---

## 🆘 Support

If you encounter issues:
1. Check Keycloak logs: `docker logs keycloak-server`
2. Check Spring Boot logs: Check console output
3. Verify all configurations match this guide
4. Test with cURL before frontend integration

---

**Version**: 1.0.0  
**Last Updated**: December 2025
