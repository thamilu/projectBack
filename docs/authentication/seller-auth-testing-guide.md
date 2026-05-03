# How to Test Seller Authentication

## The Problem

You correctly identified that **seller authentication logs are missing** because the seller dashboard endpoint **is not being called** from your frontend.

## The Fix is Complete

I've already enhanced the backend logging in `DashboardController.java`. When a seller successfully calls the `/api/v1/dashboard/seller` endpoint, you'll now see:

```log
INFO  c.e.a.controller.DashboardController - ✅ SELLER authenticated | user=seller@example.com | roles=[SELLER] | sellerId=123
```

## To Verify the Fix Works

You need to **call the seller dashboard endpoint with a valid Bearer token**. Here are 3 ways to do this:

### Option 1: Use Your Frontend (Recommended)

If you have a frontend application:

1. Login as a seller user
2. Navigate to the seller dashboard page
3. The frontend should call: `GET http://localhost:8082/api/v1/dashboard/seller`
4. Check backend logs - you should see the ✅ SELLER authenticated message

### Option 2: Manual Test with Browser Console

1. Open your frontend in a browser
2. Login as a seller
3. Open browser DevTools (F12) → Console
4. Run this code:

```javascript
// Get your session/token (adjust based on your auth library)
const token = "YOUR_ACCESS_TOKEN_HERE";

// Call seller dashboard
fetch('http://localhost:8082/api/v1/dashboard/seller', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }
})
.then(res => res.json())
.then(data => console.log('✅ Seller dashboard:', data))
.catch(err => console.error('❌ Error:', err));
```

### Option 3: Get Token and Test with PowerShell

**Step 1: Get a valid seller token**

You need to know:
- Seller username/email
- Seller password  
- How your frontend authenticates (Keycloak direct grant, or backend wrapper)

**Step 2: Once you have a token, run:**

```powershell
# Replace with your actual token
$TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# Call seller dashboard
Invoke-RestMethod -Uri "http://localhost:8082/api/v1/dashboard/seller" `
    -Method Get `
    -Headers @{ Authorization = "Bearer $TOKEN" } | ConvertTo-Json
```

**Step 3: Check backend logs**

You should see:
```log
INFO  - ✅ SELLER authenticated | user=sellerName | roles=[SELLER]
URI: /api/v1/dashboard/seller
Status: 200
```

## Current State

✅ **Backend configuration:** Correct  
✅ **Security protection:** Correct  
✅ **JWT role mapping:** Correct  
✅ **Logging enhanced:** Complete  
❌ **Seller endpoint not being called:** Need to trigger it from frontend

## Why Admin Works But Not Seller

From your logs:
```
13:30:11.436 DEBUG [tomcat-handler-0] c.e.a.controller.DashboardController - Admin dashboard generated in 60 ms
13:30:11.599 DEBUG [tomcat-handler-0] o.s.web.servlet.DispatcherServlet - Completed 200 OK
URI: /api/v1/dashboard/admin
Status: 200
```

Admin works because **something is calling** `/api/v1/dashboard/admin`.

Seller doesn't show up because **nothing is calling** `/api/v1/dashboard/seller` yet.

## Next Steps

1. **Find or create your frontend code** that should call the seller dashboard
2. **Ensure it's sending the Bearer token** in the Authorization header
3. **Call the endpoint** when a seller logs in
4. **Check the logs** - you should now see the seller authentication message

## Example Frontend Code

### React/Next.js
```typescript
useEffect(() => {
  const fetchSellerDashboard = async () => {
    const session = await getSession();
    if (!session?.accessToken) return;
    
    const response = await fetch('/api/v1/dashboard/seller', {
      headers: {
        Authorization: `Bearer ${session.accessToken}`
      }
    });
    
    const data = await response.json();
    setDashboard(data);
  };
  
  fetchSellerDashboard();
}, []);
```

### Angular
```typescript
ngOnInit() {
  const token = this.authService.getToken();
  
  this.http.get('http://localhost:8082/api/v1/dashboard/seller', {
    headers: { Authorization: `Bearer ${token}` }
  }).subscribe(data => {
    this.dashboardData = data;
  });
}
```

---

**The backend is ready. You just need to call the seller endpoint to see the authentication logs appear!** 🎯
