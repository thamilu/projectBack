# Product Creation API: Example Request, Errors, and Solutions

## Example Product Creation Request Payload
```json
{
  "name": "iPhone 15 Pro 256GB",
  "description": "The latest iPhone with A17 Pro chip, titanium design, 48MP camera system, and USB-C connectivity.",
  "sku": "IPHONE-15-PRO-256",
  "friendlyUrl": "iphone-15-pro-256gb",
  "price": 999.99,
  "discountPrice": 899.99,
  "stockQuantity": 100,
  "imageUrl": "https://cdn.example.com/products/iphone-15.jpg",
  "categoryId": 1,
  "categoryType": "ELECTRONICS",
  "subCategory": "Smartphones",
  "categoryAttributes": {
    "type": "SMARTPHONE",
    "brand": "Apple",
    "size": "6.1 inch",
    "availableSizes": [
      "128GB",
      "256GB",
      "512GB",
      "1TB"
    ],
    "color": "Natural Titanium",
    "availableColors": [
      "Natural Titanium",
      "Blue Titanium",
      "White Titanium",
      "Black Titanium"
    ]
  },
  "brandId": 1,
  "shopId": 1,
  "tags": [
    "smartphone",
    "apple",
    "5g"
  ],
  "featured": false
}
```

## Common Errors Faced & Solutions

### 1. Cache Not Found (productList, productCount)
- **Error:** `Cannot find cache named 'productList'` or `Cannot find cache named 'productCount'`
- **Solution:**
  - Added missing cache names to both `spring.cache.cache-names` and `app.cache.cache-names` in `application.properties`.

### 2. JSON Parse Error for categoryAttributes
- **Error:** `Could not resolve type id 'Smartphone' as a subtype of CategoryAttributes` (case sensitivity issue)
- **Solution:**
  - Ensure the `type` field in `categoryAttributes` is uppercase (e.g., `"type": "SMARTPHONE"`).
  - Updated frontend documentation to highlight case sensitivity.

### 3. DataIntegrityViolationException for is_master Column
- **Error:** `null value in column "is_master" of relation "products" violates not-null constraint`
- **Solution:**
  - Added `isMaster` field to the Product entity, mapped to the `is_master` column, with a default value.

### 4. DataIntegrityViolationException for created_at Column
- **Error:** `null value in column "created_at" of relation "products" violates not-null constraint`
- **Solution:**
  - Added JPA lifecycle hooks (`@PrePersist`, `@PreUpdate`) to set `createdAt` and `updatedAt` automatically.

### 5. Unsupported JWT Token
- **Error:** `Unsupported JWT token` in logs
- **Solution:**
  - Ensured the frontend uses a valid, non-expired JWT token issued by the correct Keycloak realm.

## Additional Notes
- Always use the correct case for enum/type fields in payloads.
- Ensure all required fields (including those with database NOT NULL constraints) are set in the entity or via code.
- See also: `docs/categoryAttributes-reference.md` and `docs/product-create-response-reference.md` for more details.

---
For further troubleshooting or integration help, contact the backend team.
