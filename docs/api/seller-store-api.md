# Seller Store API Documentation

## Overview

The **Seller Store Controller** provides RESTful endpoints for sellers to manage their storefronts in the e-commerce platform. This API is located at `/seller/store` and automatically resolves the seller's identity from JWT authentication.

**Base URL:** `http://localhost:8082/seller/store`  
**Swagger UI:** http://localhost:8082/swagger-ui/index.html  
**OpenAPI Spec:** http://localhost:8082/v3/api-docs

---

## Key Features

✅ **Auto-resolves seller identity** from JWT token  
✅ **One-to-one relationship** - Each seller can have only one store  
✅ **Comprehensive error handling** with meaningful HTTP status codes  
✅ **Full Swagger/OpenAPI documentation** with examples  
✅ **Role-based access control** - SELLER role required  

---

## Authentication

All endpoints require:
- **JWT Bearer Token** with SELLER role
- Token in `Authorization` header: `Bearer <your-jwt-token>`

---

## Endpoints

### 1. Get My Store

**GET** `/seller/store`

Retrieve the authenticated seller's store information.

#### Response
```json
{
  "success": true,
  "message": "Operation successful",
  "data": {
    "id": 1,
    "shopName": "Tech Gadgets Store",
    "shopDescription": "Premium electronics and gadgets",
    "sellerId": 123,
    "sellerName": "John Doe",
    "createdAt": "2026-01-10T10:30:00",
    "updatedAt": "2026-01-10T10:30:00"
  }
}
```

#### Status Codes
- `200 OK` - Store retrieved successfully
- `401 Unauthorized` - Invalid or missing token
- `403 Forbidden` - User doesn't have SELLER role
- `404 Not Found` - Seller doesn't have a store yet

---

### 2. Create My Store

**POST** `/seller/store`

Create a new store for the authenticated seller.

#### Request Body
```json
{
  "shopName": "Tech Gadgets Store",
  "shopDescription": "Premium electronics and gadgets for tech enthusiasts"
}
```

#### Validation Rules
- `shopName`: Required, 3-100 characters
- `shopDescription`: Optional, max 500 characters

#### Response
```json
{
  "success": true,
  "message": "Store created successfully",
  "data": {
    "id": 1,
    "shopName": "Tech Gadgets Store",
    "shopDescription": "Premium electronics and gadgets for tech enthusiasts",
    "sellerId": 123,
    "sellerName": "John Doe",
    "createdAt": "2026-01-10T10:30:00",
    "updatedAt": "2026-01-10T10:30:00"
  }
}
```

#### Status Codes
- `201 Created` - Store created successfully
- `400 Bad Request` - Validation failed
- `401 Unauthorized` - Invalid or missing token
- `403 Forbidden` - User doesn't have SELLER role
- `409 Conflict` - Seller already has a store

---

### 3. Update My Store

**PUT** `/seller/store`

Update the authenticated seller's existing store information.

#### Request Body
```json
{
  "shopName": "Tech Gadgets Store - Premium",
  "shopDescription": "Updated description with new product lines and services"
}
```

#### Response
```json
{
  "success": true,
  "message": "Store updated successfully",
  "data": {
    "id": 1,
    "shopName": "Tech Gadgets Store - Premium",
    "shopDescription": "Updated description with new product lines and services",
    "sellerId": 123,
    "sellerName": "John Doe",
    "createdAt": "2026-01-10T10:30:00",
    "updatedAt": "2026-01-10T15:45:00"
  }
}
```

#### Status Codes
- `200 OK` - Store updated successfully
- `400 Bad Request` - Validation failed
- `401 Unauthorized` - Invalid or missing token
- `403 Forbidden` - User doesn't have SELLER role
- `404 Not Found` - Seller doesn't have a store yet

---

### 4. Check Store Exists

**GET** `/seller/store/exists`

Check if the authenticated seller has a store configured.

#### Use Cases
- Frontend conditional rendering (show "Create Store" vs "Manage Store")
- Onboarding workflow validation
- Pre-flight checks before store-dependent operations

#### Response (Store Exists)
```json
{
  "success": true,
  "message": "Operation successful",
  "data": true
}
```

#### Response (Store Doesn't Exist)
```json
{
  "success": true,
  "message": "Operation successful",
  "data": false
}
```

#### Status Codes
- `200 OK` - Check completed (always returns 200, check `data` field)
- `401 Unauthorized` - Invalid or missing token
- `403 Forbidden` - User doesn't have SELLER role

---

## Integration with Product Creation

The Seller Store API works seamlessly with the Product API:

### Auto-Resolve Shop ID

When creating products via **POST** `/api/v1/products`, the `shopId` field is **optional**:

```json
{
  "productName": "Samsung Galaxy S24",
  "productDescription": "Latest flagship smartphone",
  "price": 999.99,
  "stockQuantity": 50,
  "categoryId": 1,
  "brandId": 2
  // shopId is optional - will auto-resolve from seller's store
}
```

If `shopId` is not provided, the system automatically:
1. Extracts seller ID from JWT token
2. Looks up seller's store using `shopRepository.findBySellerId()`
3. Associates product with seller's store

---

## Error Handling

All endpoints return consistent error responses:

### Validation Error (400)
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "shopName": "Shop name must be between 3 and 100 characters"
  }
}
```

### Unauthorized (401)
```json
{
  "success": false,
  "message": "Unauthorized - invalid or missing token"
}
```

### Forbidden (403)
```json
{
  "success": false,
  "message": "Access denied - SELLER role required"
}
```

### Not Found (404)
```json
{
  "success": false,
  "message": "Store not found - please create a store first"
}
```

### Conflict (409)
```json
{
  "success": false,
  "message": "Store already exists - use PUT to update"
}
```

---

## Testing with Swagger UI

1. **Open Swagger UI:** http://localhost:8082/swagger-ui/index.html
2. **Find "Seller Store" section** in the API list
3. **Click "Authorize"** button (top right)
4. **Enter JWT token:** `Bearer <your-jwt-token>`
5. **Try out endpoints** using the interactive UI

### Getting a JWT Token

Use the authentication endpoint to get a token:

**POST** `/api/auth/login`
```json
{
  "email": "seller@example.com",
  "password": "password123"
}
```

Response will include:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}
```

---

## Swagger Documentation Features

Each endpoint includes:

✅ **Detailed descriptions** with business rules  
✅ **Request/response examples** in JSON  
✅ **HTTP status code documentation** with meanings  
✅ **Validation rules** for all fields  
✅ **Error scenarios** and handling  
✅ **Authentication requirements** clearly marked  
✅ **Use case documentation** for practical guidance  

---

## Implementation Details

### Controller Location
```
src/main/java/com/eshop/app/controller/SellerStoreController.java
```

### Swagger Annotations Used
- `@Tag` - API grouping and description
- `@Operation` - Endpoint-level documentation with detailed descriptions
- `@ApiResponses` - HTTP status code documentation
- `@SecurityRequirement` - Authentication requirements
- `@RequestBody` - Request body schema and examples
- `@Schema` - Data model documentation
- `@ExampleObject` - JSON examples for requests/responses

### Business Logic
The controller delegates to `ShopService` for all business operations, maintaining separation of concerns and reusing existing tested logic.

---

## Frontend Integration Example

```typescript
// Check if seller has a store
const checkStore = async () => {
  const response = await fetch('http://localhost:8082/seller/store/exists', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  const data = await response.json();
  return data.data; // true or false
};

// Create store
const createStore = async (storeName, storeDescription) => {
  const response = await fetch('http://localhost:8082/seller/store', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      shopName: storeName,
      shopDescription: storeDescription
    })
  });
  return await response.json();
};

// Get my store
const getMyStore = async () => {
  const response = await fetch('http://localhost:8082/seller/store', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return await response.json();
};
```

---

## Related Documentation

- **Main API Documentation:** [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)
- **Architecture Guide:** [ARCHITECTURE.md](./ARCHITECTURE.md)
- **Shop Controller (Admin):** `/api/v1/shops` - For admin-level shop management

---

## Changelog

### Version 1.0 (2026-01-10)
- ✅ Initial release with 4 endpoints
- ✅ Comprehensive Swagger documentation
- ✅ Auto-resolve seller from JWT
- ✅ Integration with product creation workflow
- ✅ Full error handling and validation

---

## Support

For issues or questions:
- Review Swagger UI for interactive testing
- Check error responses for detailed messages
- Refer to ARCHITECTURE.md for system design details
