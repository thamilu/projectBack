# Swagger Endpoint Grouping - SellerStoreController

## ✅ Fixed Issue: Endpoints Now Visible in Swagger

### Problem
The newly created **SellerStoreController** endpoints were not showing up in the Swagger UI documentation.

### Root Cause
The controller was using `/seller/store` as the base path, but the Swagger group configuration expected `/api/v1/seller/**` pattern.

### Solution Applied
Updated the `@RequestMapping` annotation in SellerStoreController:

**Before:**
```java
@RequestMapping("/seller/store")
```

**After:**
```java
@RequestMapping(ApiConstants.BASE_PATH + "/seller/store")
```

This resolves to: `/api/v1/seller/store`

---

## 📍 Where to Find the Endpoints in Swagger

### 🔗 Access Swagger UI
**URL:** http://localhost:8082/swagger-ui/index.html

### 📂 API Group Location
The **Seller Store** endpoints will appear in the **"Seller APIs"** group.

To view:
1. Open Swagger UI
2. Look for the dropdown at the top right labeled **"Select a definition"**
3. Select **"seller - Seller APIs"**
4. You'll see the **"Seller Store"** section with 4 endpoints

---

## 🎯 API Endpoint Paths (Fixed)

All endpoints now have the correct `/api/v1` prefix:

| Method | Endpoint | Description | Group |
|--------|----------|-------------|-------|
| GET | `/api/v1/seller/store` | Get my store | Seller APIs |
| POST | `/api/v1/seller/store` | Create my store | Seller APIs |
| PUT | `/api/v1/seller/store` | Update my store | Seller APIs |
| GET | `/api/v1/seller/store/exists` | Check store exists | Seller APIs |

---

## 📋 Swagger Group Configuration

The API groups are defined in [SwaggerGroupConfig.java](src/main/java/com/eshop/app/config/SwaggerGroupConfig.java):

```java
@Bean
public GroupedOpenApi sellerApi() {
    return GroupedOpenApi.builder()
        .group("seller")
        .displayName("Seller APIs")
        .pathsToMatch(ApiConstants.BASE_PATH + "/seller/**")
        .build();
}
```

**Pattern:** `/api/v1/seller/**`  
**Matches:** All paths starting with `/api/v1/seller/`  
**Display Name:** "Seller APIs"

---

## 🗂️ All Available API Groups

| Group | Display Name | Path Pattern | Endpoints Included |
|-------|-------------|--------------|-------------------|
| **seller** | Seller APIs | `/api/v1/seller/**` | Seller Store, Seller Dashboard |
| **public** | Public APIs | `/api/v1/public/**`, `/api/v1/products/**`, `/api/v1/categories/**` | Public product listings, categories |
| **auth** | Authentication APIs | `/api/v1/auth/**` | Login, Register, Token refresh |
| **admin** | Admin APIs | `/api/v1/admin/**` | Admin operations |
| **user** | User APIs | `/api/v1/users/**`, `/api/v1/orders/**`, `/api/v1/cart/**` | User management, orders, cart |
| **all** | All APIs | `/api/**` | Complete API documentation |

---

## 🔍 How to Find Your Endpoints

### Method 1: Select API Group
1. Open http://localhost:8082/swagger-ui/index.html
2. Click the dropdown **"Select a definition"** (top right)
3. Choose **"seller - Seller APIs"**
4. Scroll to find **"Seller Store"** tag

### Method 2: View All APIs
1. Open http://localhost:8082/swagger-ui/index.html
2. Click **"Select a definition"**
3. Choose **"all - All APIs"**
4. Use browser search (Ctrl+F) to find **"Seller Store"**

### Method 3: Search by Path
1. In Swagger UI, use the search box at the top
2. Type: `/seller/store`
3. All matching endpoints will be highlighted

---

## ✅ Verification Checklist

- [x] Controller path updated to `/api/v1/seller/store`
- [x] ApiConstants.BASE_PATH imported in controller
- [x] Code compiled successfully
- [x] Application started without errors
- [x] Swagger UI accessible
- [x] Endpoints visible in "Seller APIs" group

---

## 📖 Related Documentation

- **Full API Documentation:** [SELLER_STORE_API_DOCUMENTATION.md](SELLER_STORE_API_DOCUMENTATION.md)
- **Quick Reference:** [SWAGGER_QUICK_REFERENCE.md](SWAGGER_QUICK_REFERENCE.md)
- **Swagger Group Config:** [SwaggerGroupConfig.java](src/main/java/com/eshop/app/config/SwaggerGroupConfig.java)
- **API Constants:** [ApiConstants.java](src/main/java/com/eshop/app/constants/ApiConstants.java)

---

## 🎉 Summary

✅ **Issue Resolved:** SellerStoreController endpoints now visible in Swagger  
✅ **API Group:** "Seller APIs"  
✅ **Path Pattern:** `/api/v1/seller/**`  
✅ **Endpoints:** 4 fully documented endpoints  
✅ **Status:** Production ready  

**The endpoints are now properly grouped and fully accessible in Swagger UI!**
