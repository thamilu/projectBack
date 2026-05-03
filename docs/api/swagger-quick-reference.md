# Swagger Documentation Quick Reference

## 🚀 Access Swagger UI

**URL:** http://localhost:8082/swagger-ui/index.html

The application is now running with comprehensive Swagger documentation!

---

## 📋 What's Been Documented

### ✅ Seller Store Controller (`/seller/store`)

All 4 endpoints now have **enterprise-grade Swagger documentation**:

1. **GET `/seller/store`** - Get my store
2. **POST `/seller/store`** - Create my store  
3. **PUT `/seller/store`** - Update my store
4. **GET `/seller/store/exists`** - Check if store exists

---

## 🎯 Documentation Features

Each endpoint includes:

### ✅ Detailed Descriptions
- Business rules and requirements
- Authentication requirements
- Validation rules
- Use cases and scenarios

### ✅ Request/Response Examples
```json
// Example Request (Create Store)
{
  "shopName": "Tech Gadgets Store",
  "shopDescription": "Premium electronics and gadgets"
}

// Example Response
{
  "success": true,
  "message": "Store created successfully",
  "data": {
    "id": 1,
    "shopName": "Tech Gadgets Store",
    "sellerId": 123
  }
}
```

### ✅ HTTP Status Codes
- `200` - Success
- `201` - Created
- `400` - Bad Request (validation failed)
- `401` - Unauthorized (missing/invalid token)
- `403` - Forbidden (wrong role)
- `404` - Not Found
- `409` - Conflict (duplicate)

### ✅ Error Scenarios
- What can go wrong
- Why it fails
- How to fix it

### ✅ Security Documentation
- JWT Bearer token required
- SELLER role required
- Authentication scope clearly marked

---

## 🧪 Testing in Swagger UI

### Step 1: Open Swagger UI
Navigate to: http://localhost:8082/swagger-ui/index.html

### Step 2: Find "Seller Store" Section
Look for the **"Seller Store"** tag in the API list

### Step 3: Authorize
1. Click **"Authorize"** button (🔓 lock icon, top right)
2. Enter your JWT token: `Bearer <your-token>`
3. Click **"Authorize"**
4. Click **"Close"**

### Step 4: Try Endpoints
1. Expand any endpoint (e.g., `GET /seller/store`)
2. Click **"Try it out"**
3. Modify parameters (if needed)
4. Click **"Execute"**
5. See the response below

---

## 📖 Swagger Annotations Used

### Controller Level
```java
@Tag(
    name = "Seller Store", 
    description = "Seller storefront management endpoints - Manage your store..."
)
```

### Method Level
```java
@Operation(
    summary = "Create my store",
    description = """
        Detailed multi-line description with:
        - Business rules
        - Requirements
        - Error cases
        """,
    security = @SecurityRequirement(name = "Bearer Authentication")
)
```

### Response Documentation
```java
@ApiResponses(value = {
    @ApiResponse(
        responseCode = "201",
        description = "Store created successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class),
            examples = @ExampleObject(value = "{ JSON example }")
        )
    )
})
```

### Request Body
```java
@RequestBody(
    description = "Store creation request with shop name and description",
    required = true,
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ShopCreateRequest.class),
        examples = @ExampleObject(value = "{ JSON example }")
    )
)
```

---

## 🔑 Getting a Test JWT Token

### Using Auth Endpoint

**POST** `http://localhost:8082/api/auth/login`

```json
{
  "email": "seller@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

Copy the `accessToken` value and use it in Swagger:
```
Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 📂 Related Files

### Controller Implementation
```
src/main/java/com/eshop/app/controller/SellerStoreController.java
```

### Documentation
- `SELLER_STORE_API_DOCUMENTATION.md` - Complete API documentation
- `API_DOCUMENTATION.md` - Main API documentation
- `ARCHITECTURE.md` - System architecture

---

## 🎨 Swagger UI Features

### Interactive Testing
- ✅ Execute requests directly from browser
- ✅ See real responses with status codes
- ✅ Test error scenarios
- ✅ Validate request bodies

### Schema Browser
- ✅ View all data models
- ✅ See required fields
- ✅ Check validation rules
- ✅ Understand data types

### Authentication
- ✅ Store JWT token once
- ✅ Auto-applied to all requests
- ✅ Test authenticated endpoints easily

---

## 💡 Tips

### 1. Read the Description
Each endpoint has a detailed description with:
- What it does
- Who can use it
- What can go wrong
- How to use it properly

### 2. Check Examples
Every request/response has JSON examples showing:
- Valid request format
- Expected response structure
- Success and error cases

### 3. Test Error Cases
Try invalid requests to see error handling:
- Missing required fields
- Invalid token
- Wrong role
- Duplicate resources

### 4. Use Schema Documentation
Click on schema names (like `ShopCreateRequest`) to see:
- All fields
- Data types
- Validation rules
- Required vs optional

---

## 🔍 Finding Endpoints

### By Tag/Group
Endpoints are grouped by feature:
- **Seller Store** - Store management
- **Products** - Product CRUD
- **Authentication** - Login/logout
- **Shops** - Admin shop management

### By Path
Filter by path prefix:
- `/seller/store` - Seller store endpoints
- `/api/v1/products` - Product endpoints
- `/api/auth` - Authentication endpoints

### Search
Use Swagger's built-in search (top right) to find:
- Endpoint paths
- Operation IDs
- Descriptions

---

## 📊 OpenAPI Specification

### JSON Format
http://localhost:8082/v3/api-docs

### YAML Format
http://localhost:8082/v3/api-docs.yaml

### Grouped APIs
- http://localhost:8082/v3/api-docs/all
- http://localhost:8082/v3/api-docs/admin
- http://localhost:8082/v3/api-docs/seller

---

## ✨ Benefits

### For Frontend Developers
- ✅ Clear API contracts
- ✅ Request/response examples
- ✅ Error handling guide
- ✅ Interactive testing

### For Backend Developers
- ✅ Self-documenting code
- ✅ Consistent documentation
- ✅ Easy to maintain
- ✅ Version controlled

### For QA/Testing
- ✅ Test all endpoints easily
- ✅ Validate responses
- ✅ Check error cases
- ✅ No Postman needed

### For API Consumers
- ✅ Discover available endpoints
- ✅ Understand requirements
- ✅ See examples
- ✅ Test integration

---

## 🚀 Next Steps

1. **Open Swagger UI** - http://localhost:8082/swagger-ui/index.html
2. **Get a JWT token** - Use the auth endpoint
3. **Authorize in Swagger** - Click the lock icon
4. **Test "Seller Store" endpoints** - Try creating a store
5. **Check the responses** - Verify success/error cases
6. **Integrate with frontend** - Use the documented API

---

## 📝 Summary

✅ **4 endpoints documented** with comprehensive details  
✅ **Request/response examples** for all operations  
✅ **HTTP status codes** explained with meanings  
✅ **Error scenarios** documented with solutions  
✅ **Security requirements** clearly marked  
✅ **Interactive testing** available in Swagger UI  
✅ **Production-ready** documentation  

**Everything is ready for testing and frontend integration!** 🎉
