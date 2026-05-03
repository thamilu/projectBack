# Seller Authentication Frontend Integration Guide

## 🎯 Problem Summary

**Issue**: Seller login works in frontend (Keycloak), but backend never authenticates because no SELLER-protected API is called with the JWT token.

**Root Cause**: Backend is stateless (JWT-based). Authentication only occurs when a protected endpoint receives a valid JWT token with the correct role.

## ✅ Backend Changes (COMPLETED)

### 1. Created `SellerDashboardController`
- **File**: `src/main/java/com/eshop/app/controller/SellerDashboardController.java`
- **Endpoints**:
  - `GET /api/v1/dashboard/seller` - Main dashboard (requires ROLE_SELLER)
  - `GET /api/v1/dashboard/seller/stats` - Seller statistics
  - `GET /api/v1/dashboard/seller/verify` - Authentication verification

### 2. Updated `OAuth2SecurityConfig`
- **File**: `src/main/java/com/eshop/app/config/OAuth2SecurityConfig.java`
- **Change**: Added `.requestMatchers(ApiConstants.BASE_PATH + "/dashboard/seller/**").hasRole("SELLER")`
- **Effect**: Backend now enforces SELLER role on these endpoints

### 3. Enhanced Logging
- **File**: `src/main/resources/application.properties`
- **Change**: Added `logging.level.org.springframework.security.oauth2=TRACE`
- **Effect**: Backend logs JWT authentication in detail

### 4. Authentication Logs
When a seller calls a protected API, you'll see:
```
🔐 JWT Authentication | user=seller@example.com | roles=[SELLER] | subject=...
✅ Granted authorities: [ROLE_SELLER]
✅ SELLER AUTHENTICATED | user=seller@example.com | roles=ROLE_SELLER | timestamp=...
```

---

## 🔧 Frontend Integration (YOU NEED TO DO THIS)

### Option 1: Next.js (App Router)

#### File: `app/seller/page.tsx`

```typescript
"use client";

import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";
import axios from "axios";

interface DashboardData {
  message: string;
  username: string;
  roles: string;
  authenticated: boolean;
  stats: {
    totalProducts: number;
    pendingOrders: number;
    totalRevenue: number;
    activeListings: number;
  };
}

export default function SellerDashboard() {
  const { data: session, status } = useSession();
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (status === "authenticated" && session?.accessToken) {
      fetchSellerDashboard();
    }
  }, [status, session]);

  const fetchSellerDashboard = async () => {
    setLoading(true);
    setError(null);

    try {
      console.log("🔐 Calling backend with JWT token...");
      
      const response = await axios.get<{
        status: string;
        message: string;
        data: DashboardData;
      }>(
        "http://localhost:8082/api/v1/dashboard/seller",
        {
          headers: {
            Authorization: `Bearer ${session.accessToken}`,
          },
        }
      );
      
      console.log("✅ Backend response:", response.data);
      setDashboardData(response.data.data);
      
    } catch (err: any) {
      console.error("❌ Backend error:", err.response?.data || err.message);
      
      if (err.response?.status === 401) {
        setError("Authentication failed. Please login again.");
      } else if (err.response?.status === 403) {
        setError("Access denied. You don't have SELLER role.");
      } else {
        setError(err.response?.data?.message || "Failed to load dashboard");
      }
    } finally {
      setLoading(false);
    }
  };

  if (status === "loading" || loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading seller dashboard...</p>
        </div>
      </div>
    );
  }

  if (status === "unauthenticated") {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-red-600">Not Authenticated</h1>
          <p className="mt-2">Please login to access seller dashboard</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-red-600">Error</h1>
          <p className="mt-2">{error}</p>
          <button
            onClick={fetchSellerDashboard}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-3xl font-bold mb-6">Seller Dashboard</h1>
      
      {dashboardData && (
        <>
          <div className="bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded mb-6">
            <p className="font-bold">✅ {dashboardData.message}</p>
            <p className="text-sm mt-1">User: {dashboardData.username}</p>
            <p className="text-sm">Roles: {dashboardData.roles}</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="bg-white p-6 rounded-lg shadow">
              <h3 className="text-gray-500 text-sm">Total Products</h3>
              <p className="text-3xl font-bold mt-2">{dashboardData.stats.totalProducts}</p>
            </div>
            
            <div className="bg-white p-6 rounded-lg shadow">
              <h3 className="text-gray-500 text-sm">Pending Orders</h3>
              <p className="text-3xl font-bold mt-2">{dashboardData.stats.pendingOrders}</p>
            </div>
            
            <div className="bg-white p-6 rounded-lg shadow">
              <h3 className="text-gray-500 text-sm">Total Revenue</h3>
              <p className="text-3xl font-bold mt-2">${dashboardData.stats.totalRevenue}</p>
            </div>
            
            <div className="bg-white p-6 rounded-lg shadow">
              <h3 className="text-gray-500 text-sm">Active Listings</h3>
              <p className="text-3xl font-bold mt-2">{dashboardData.stats.activeListings}</p>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
```

---

### Option 2: Next.js (Pages Router)

#### File: `pages/seller/dashboard.tsx`

```typescript
import { useSession } from "next-auth/react";
import { useEffect, useState } from "react";
import axios from "axios";

export default function SellerDashboard() {
  const { data: session, status } = useSession();
  const [dashboardData, setDashboardData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (status === "authenticated" && session?.accessToken) {
      fetchDashboard();
    }
  }, [status, session]);

  const fetchDashboard = async () => {
    try {
      const response = await axios.get(
        "http://localhost:8082/api/v1/dashboard/seller",
        {
          headers: {
            Authorization: `Bearer ${session.accessToken}`,
          },
        }
      );
      setDashboardData(response.data.data);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load dashboard");
    }
  };

  if (status === "loading") return <div>Loading...</div>;
  if (status === "unauthenticated") return <div>Please login</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h1>Seller Dashboard</h1>
      {dashboardData && (
        <pre>{JSON.stringify(dashboardData, null, 2)}</pre>
      )}
    </div>
  );
}
```

---

### Option 3: React (with Keycloak JS Adapter)

#### File: `src/pages/SellerDashboard.jsx`

```javascript
import { useEffect, useState } from "react";
import axios from "axios";
import { useKeycloak } from "@react-keycloak/web";

export default function SellerDashboard() {
  const { keycloak, initialized } = useKeycloak();
  const [dashboardData, setDashboardData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (initialized && keycloak.authenticated) {
      fetchDashboard();
    }
  }, [initialized, keycloak.authenticated]);

  const fetchDashboard = async () => {
    try {
      const response = await axios.get(
        "http://localhost:8082/api/v1/dashboard/seller",
        {
          headers: {
            Authorization: `Bearer ${keycloak.token}`,
          },
        }
      );
      setDashboardData(response.data.data);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load dashboard");
    }
  };

  if (!initialized) return <div>Loading...</div>;
  if (!keycloak.authenticated) return <div>Please login</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div>
      <h1>Seller Dashboard</h1>
      {dashboardData && (
        <pre>{JSON.stringify(dashboardData, null, 2)}</pre>
      )}
    </div>
  );
}
```

---

## 🔍 Verification Steps

### 1. Test with PowerShell Script
```powershell
cd f:\MyprojectAgent\EcomApp\eshop
.\test-seller-authentication.ps1
```

Update the script with your:
- `CLIENT_SECRET`
- `SELLER_USERNAME`
- `SELLER_PASSWORD`

### 2. Test with cURL
```bash
# Get token from Keycloak
TOKEN=$(curl -X POST "http://localhost:8080/realms/eshop/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=eshop-client" \
  -d "client_secret=YOUR_SECRET" \
  -d "username=seller@example.com" \
  -d "password=password" \
  -d "grant_type=password" | jq -r '.access_token')

# Call seller dashboard
curl -X GET "http://localhost:8082/api/v1/dashboard/seller" \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Expected Backend Logs
```
🔐 JWT Authentication | user=seller@example.com | roles=[SELLER] | subject=...
✅ Granted authorities: [ROLE_SELLER]
✅ SELLER AUTHENTICATED | user=seller@example.com | roles=ROLE_SELLER | timestamp=2026-01-01T...
```

---

## 🐛 Troubleshooting

### Issue: 401 Unauthorized
**Cause**: JWT token missing, invalid, or expired

**Fix**:
1. Check if `Authorization` header is sent: `Bearer <token>`
2. Verify token is not expired (default: 5 minutes)
3. Check backend logs for JWT validation errors
4. Verify issuer-uri in `application.properties` matches Keycloak

### Issue: 403 Forbidden
**Cause**: JWT valid but user doesn't have SELLER role

**Fix**:
1. Check Keycloak user roles: User → Role Mapping → Assigned Roles
2. Verify Keycloak client mapper:
   - Client Scopes → roles → Mappers → realm roles
   - Token Claim Name: `roles`
   - Multivalued: ON
3. Decode JWT token and check if `roles` claim contains `SELLER`

### Issue: No backend logs
**Cause**: Frontend not calling the API

**Fix**:
1. Check browser console for errors
2. Verify API URL: `http://localhost:8082/api/v1/dashboard/seller`
3. Check if `session.accessToken` exists
4. Add console.log to debug token value

---

## 📝 Summary

✅ **Backend is ready** - Seller-protected endpoints exist and log authentication  
⚠️ **Frontend needs update** - Must call these endpoints with JWT token  
🔍 **Verification** - Use test script or implement frontend code above  

Once frontend calls `/api/v1/dashboard/seller` with valid JWT token, you'll see seller authentication in backend logs! 🎉
