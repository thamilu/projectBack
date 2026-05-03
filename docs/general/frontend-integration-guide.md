# Frontend Integration - Keycloak Authentication

Quick guide for frontend developers to integrate with the Keycloak-enabled backend API.

## 🚀 Base URL
```
Backend API: http://localhost:8082
Auth Endpoints: http://localhost:8082/api/auth
```

---

## 🔐 Authentication Methods

### Method 1: Username/Password Login (Recommended for Admin Panels)

```javascript
async function login(username, password) {
  const response = await fetch('http://localhost:8082/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username, password })
  });
  
  if (!response.ok) {
    throw new Error('Login failed');
  }
  
  const tokens = await response.json();
  // Store tokens securely
  localStorage.setItem('access_token', tokens.access_token);
  localStorage.setItem('refresh_token', tokens.refresh_token);
  localStorage.setItem('token_expiry', Date.now() + (tokens.expires_in * 1000));
  
  return tokens;
}

// Usage
try {
  const tokens = await login('admin', 'admin123');
  console.log('Logged in successfully');
} catch (error) {
  console.error('Login failed:', error);
}
```

### Method 2: OAuth2 Flow (Recommended for Customer-Facing Apps)

```javascript
// Step 1: Redirect to Keycloak login
async function startOAuthLogin() {
  const redirectUri = `${window.location.origin}/callback`;
  const response = await fetch(
    `http://localhost:8082/api/auth/login-url?redirectUri=${redirectUri}`
  );
  
  const { authorizationUrl, state } = await response.json();
  
  // Save state for CSRF protection
  sessionStorage.setItem('oauth_state', state);
  
  // Redirect user to Keycloak
  window.location.href = authorizationUrl;
}

// Step 2: Handle callback (on /callback page)
async function handleOAuthCallback() {
  const params = new URLSearchParams(window.location.search);
  const code = params.get('code');
  const state = params.get('state');
  
  // Verify state
  const savedState = sessionStorage.getItem('oauth_state');
  if (state !== savedState) {
    throw new Error('Invalid state parameter');
  }
  
  // Exchange code for tokens
  const redirectUri = `${window.location.origin}/callback`;
  const response = await fetch(
    `http://localhost:8082/api/auth/callback?code=${code}&redirectUri=${redirectUri}`
  );
  
  if (!response.ok) {
    throw new Error('Callback failed');
  }
  
  const tokens = await response.json();
  
  // Store tokens
  localStorage.setItem('access_token', tokens.access_token);
  localStorage.setItem('refresh_token', tokens.refresh_token);
  
  // Clean up
  sessionStorage.removeItem('oauth_state');
  
  // Redirect to app
  window.location.href = '/dashboard';
}
```

---

## 🔄 Token Management

### Get Current User
```javascript
async function getCurrentUser() {
  const token = localStorage.getItem('access_token');
  
  const response = await fetch('http://localhost:8082/api/auth/me', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (!response.ok) {
    throw new Error('Failed to get user');
  }
  
  return await response.json();
}

// Response:
// {
//   "sub": "user-id",
//   "username": "admin",
//   "email": "admin@eshop.com",
//   "name": "Admin User",
//   "roles": ["ADMIN"],
//   "issuedAt": "2025-12-22T...",
//   "expiresAt": "2025-12-22T..."
// }
```

### Refresh Token
```javascript
async function refreshToken() {
  const refreshToken = localStorage.getItem('refresh_token');
  
  if (!refreshToken) {
    throw new Error('No refresh token');
  }
  
  const response = await fetch('http://localhost:8082/api/auth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ refreshToken })
  });
  
  if (!response.ok) {
    // Refresh failed, user needs to re-login
    localStorage.clear();
    window.location.href = '/login';
    return;
  }
  
  const tokens = await response.json();
  localStorage.setItem('access_token', tokens.access_token);
  localStorage.setItem('refresh_token', tokens.refresh_token);
  
  return tokens;
}
```

### Auto Token Refresh (Axios Interceptor)
```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8082/api'
});

// Request interceptor - add token and check expiry
api.interceptors.request.use(
  async (config) => {
    const token = localStorage.getItem('access_token');
    const expiry = localStorage.getItem('token_expiry');
    
    // Check if token is expired or about to expire (within 30 seconds)
    if (expiry && Date.now() > parseInt(expiry) - 30000) {
      try {
        await refreshToken();
        config.headers.Authorization = `Bearer ${localStorage.getItem('access_token')}`;
      } catch (error) {
        // Refresh failed, redirect to login
        window.location.href = '/login';
        return Promise.reject(error);
      }
    } else if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle 401 errors
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      try {
        await refreshToken();
        // Retry original request
        const config = error.config;
        config.headers.Authorization = `Bearer ${localStorage.getItem('access_token')}`;
        return axios(config);
      } catch (refreshError) {
        // Refresh failed, redirect to login
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default api;
```

---

## 🚪 Logout

```javascript
async function logout() {
  const refreshToken = localStorage.getItem('refresh_token');
  
  if (refreshToken) {
    try {
      await fetch('http://localhost:8082/api/auth/logout', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ refreshToken })
      });
    } catch (error) {
      console.error('Logout error:', error);
    }
  }
  
  // Clear local storage
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
  localStorage.removeItem('token_expiry');
  
  // Redirect to login
  window.location.href = '/login';
}
```

---

## 🛡️ Protected Route Component (React)

```jsx
import { Navigate } from 'react-router-dom';

function ProtectedRoute({ children, requiredRole }) {
  const token = localStorage.getItem('access_token');
  
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  
  // Optional: Check token expiry
  const expiry = localStorage.getItem('token_expiry');
  if (expiry && Date.now() > parseInt(expiry)) {
    localStorage.clear();
    return <Navigate to="/login" replace />;
  }
  
  // Optional: Check role (decode JWT)
  if (requiredRole) {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const roles = payload.realm_access?.roles || [];
    
    if (!roles.includes(requiredRole)) {
      return <Navigate to="/unauthorized" replace />;
    }
  }
  
  return children;
}

// Usage
<Route path="/dashboard" element={
  <ProtectedRoute>
    <Dashboard />
  </ProtectedRoute>
} />

<Route path="/admin" element={
  <ProtectedRoute requiredRole="ADMIN">
    <AdminPanel />
  </ProtectedRoute>
} />
```

---

## 📦 Complete React Service

```javascript
// services/authService.js
class AuthService {
  constructor() {
    this.baseUrl = 'http://localhost:8082/api/auth';
  }
  
  async login(username, password) {
    const response = await fetch(`${this.baseUrl}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    
    if (!response.ok) throw new Error('Login failed');
    
    const tokens = await response.json();
    this.saveTokens(tokens);
    return tokens;
  }
  
  async startOAuthLogin() {
    const redirectUri = `${window.location.origin}/callback`;
    const response = await fetch(
      `${this.baseUrl}/login-url?redirectUri=${redirectUri}`
    );
    
    const { authorizationUrl, state } = await response.json();
    sessionStorage.setItem('oauth_state', state);
    window.location.href = authorizationUrl;
  }
  
  async handleCallback(code, state) {
    const savedState = sessionStorage.getItem('oauth_state');
    if (state !== savedState) {
      throw new Error('Invalid state');
    }
    
    const redirectUri = `${window.location.origin}/callback`;
    const response = await fetch(
      `${this.baseUrl}/callback?code=${code}&redirectUri=${redirectUri}`
    );
    
    if (!response.ok) throw new Error('Callback failed');
    
    const tokens = await response.json();
    this.saveTokens(tokens);
    sessionStorage.removeItem('oauth_state');
    return tokens;
  }
  
  async getCurrentUser() {
    const token = this.getAccessToken();
    const response = await fetch(`${this.baseUrl}/me`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    
    if (!response.ok) throw new Error('Failed to get user');
    return await response.json();
  }
  
  async refreshToken() {
    const refreshToken = localStorage.getItem('refresh_token');
    const response = await fetch(`${this.baseUrl}/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });
    
    if (!response.ok) {
      this.logout();
      throw new Error('Token refresh failed');
    }
    
    const tokens = await response.json();
    this.saveTokens(tokens);
    return tokens;
  }
  
  async logout() {
    const refreshToken = localStorage.getItem('refresh_token');
    
    if (refreshToken) {
      try {
        await fetch(`${this.baseUrl}/logout`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        });
      } catch (error) {
        console.error('Logout error:', error);
      }
    }
    
    this.clearTokens();
  }
  
  saveTokens(tokens) {
    localStorage.setItem('access_token', tokens.access_token);
    localStorage.setItem('refresh_token', tokens.refresh_token);
    localStorage.setItem('token_expiry', Date.now() + (tokens.expires_in * 1000));
  }
  
  getAccessToken() {
    return localStorage.getItem('access_token');
  }
  
  isTokenExpired() {
    const expiry = localStorage.getItem('token_expiry');
    return !expiry || Date.now() > parseInt(expiry);
  }
  
  clearTokens() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('token_expiry');
  }
}

export default new AuthService();
```

---

## 🧪 Testing

### Quick Test
```javascript
// In browser console
fetch('http://localhost:8082/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123' })
})
.then(r => r.json())
.then(console.log);
```

---

## 📝 API Response Examples

### Login Response
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

### User Info Response
```json
{
  "sub": "a1b2c3d4-e5f6-...",
  "username": "admin",
  "email": "admin@eshop.com",
  "name": "Admin User",
  "givenName": "Admin",
  "familyName": "User",
  "emailVerified": true,
  "roles": ["ADMIN"],
  "issuedAt": "2025-12-22T10:00:00Z",
  "expiresAt": "2025-12-22T10:05:00Z"
}
```

---

## ⚠️ Important Notes

1. **CORS**: Backend already configured to allow `http://localhost:3000` and `http://localhost:4200`
2. **Token Storage**: Store tokens in `localStorage` for web apps, secure storage for mobile
3. **Token Expiry**: Access tokens expire in 5 minutes, refresh tokens in 30 minutes
4. **Error Handling**: Always handle 401 errors by refreshing token or redirecting to login
5. **Security**: Never expose tokens in URLs or logs

---

## 🔗 Resources

- [Complete Setup Guide](../KEYCLOAK_IMPLEMENTATION_GUIDE.md)
- [Backend API Docs](http://localhost:8082/swagger-ui.html)
- [Test Scripts](../test-keycloak-auth.ps1)

---

**Need Help?** Check the implementation guide or backend logs for more details.
