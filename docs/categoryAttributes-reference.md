# Product Creation API: categoryAttributes Type Reference

When creating a product, the `categoryAttributes` field in your JSON payload must include a `type` property that matches one of the following **UPPERCASE** values. This is required for correct deserialization on the backend.

## Allowed `type` values and their structures

### 1. ELECTRONICS
```json
"categoryAttributes": {
  "type": "ELECTRONICS",
  // ...other electronics-specific fields
}
```

### 2. SMARTPHONE
```json
"categoryAttributes": {
  "type": "SMARTPHONE",
  "brand": "Apple",
  "model": "iPhone 15 Pro",
  "color": "Black",
  "availableColors": ["Black", "Silver"],
  "storage": "256GB",
  "availableStorage": ["128GB", "256GB", "512GB"],
  "screenSize": "6.1",
  "processor": "A17 Pro"
}
```

### 3. LAPTOP
```json
"categoryAttributes": {
  "type": "LAPTOP",
  // ...other laptop-specific fields
}
```

### 4. CLOTHING
```json
"categoryAttributes": {
  "type": "CLOTHING",
  // ...other clothing-specific fields
}
```

### 5. FOOTWEAR
```json
"categoryAttributes": {
  "type": "FOOTWEAR",
  // ...other footwear-specific fields
}
```

### 6. FOOD
```json
"categoryAttributes": {
  "type": "FOOD",
  // ...other food-specific fields
}
```

## Important
- The `type` value **must be uppercase** (e.g., `"SMARTPHONE"`, not `"Smartphone"`).
- If the type does not match exactly, the backend will return a JSON parse error.
- Include all required fields for the specific type as defined in the backend DTOs.

## Example ProductCreateRequest Payload
```json
{
  "name": "iPhone 15 Pro 256GB",
  "sku": "IPHONE-15-PRO-256",
  "price": 999.99,
  "categoryId": 1,
  "categoryType": "ELECTRONICS",
  "subCategory": "Smartphones",
  "categoryAttributes": {
    "type": "SMARTPHONE",
    "brand": "Apple",
    "model": "iPhone 15 Pro",
    "color": "Black",
    "availableColors": ["Black", "Silver"],
    "storage": "256GB",
    "availableStorage": ["128GB", "256GB", "512GB"],
    "screenSize": "6.1",
    "processor": "A17 Pro"
  },
  "brandId": 1,
  "shopId": 1,
  "tags": ["smartphone", "apple", "5g"]
}
```

---
For more details, see the backend DTOs or contact the backend team.