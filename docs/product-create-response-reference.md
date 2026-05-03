# Product Creation API Response Reference

This document describes the structure and fields of the response returned by the product creation endpoint (`POST /api/v1/products`).

## Example Response
```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "id": 8,
    "name": "iPhone 15 Pro 256GB",
    "description": "The latest iPhone with A17 Pro chip, titanium design, 48MP camera system, and USB-C connectivity.",
    "sku": "IPHONE-15-PRO-256",
    "friendlyUrl": "iphone-15-pro-256gb",
    "price": 999.99,
    "discountPrice": 899.99,
    "stockQuantity": 100,
    "imageUrl": "https://cdn.example.com/products/iphone-15.jpg",
    "active": true,
    "featured": false,
    "categoryId": 1,
    "categoryName": "Electronics",
    "brandId": 1,
    "brandName": "Samsung",
    "shopId": 1,
    "shopName": "Tech Retail Store",
    "tags": ["apple", "5g", "smartphone"],
    "averageRating": 0.0,
    "reviewCount": 0,
    "createdAt": "2025-12-14T16:03:21.4279084",
    "updatedAt": "2025-12-14T16:03:21.4279084",
    "categoryType": null,
    "subCategory": null,
    "baseInfo": { ... },
    "pricing": { ... },
    "locationBasedPricing": null,
    "availability": null,
    "shippingRestrictions": null,
    "inventory": { ... },
    "categoryAttributes": {}
  },
  "metadata": null,
  "timestamp": null,
  "error": null
}
```

## Field Descriptions
- **success**: Indicates if the operation was successful.
- **message**: Human-readable message about the operation.
- **data**: The created product object, with the following fields:
  - **id**: Product ID (unique identifier)
  - **name, description, sku, friendlyUrl**: Basic product info
  - **price, discountPrice**: Pricing details
  - **stockQuantity**: Inventory count
  - **imageUrl**: Main product image
  - **active, featured**: Status flags
  - **categoryId, categoryName, brandId, brandName, shopId, shopName**: Category, brand, and shop info
  - **tags**: List of product tags
  - **averageRating, reviewCount**: Review summary
  - **createdAt, updatedAt**: Timestamps
  - **categoryType, subCategory**: Category details (may be null)
  - **baseInfo, pricing, inventory**: Nested objects for additional details
  - **categoryAttributes**: Category-specific attributes (should reflect input; may be empty if not set)
  - **locationBasedPricing, availability, shippingRestrictions**: Optional, may be null
- **metadata**: Optional metadata (null if not used)
- **timestamp**: Response timestamp (null if not set)
- **error**: Error details (null if success)

## Notes
- Fields like `categoryType`, `subCategory`, `categoryAttributes` may be null or empty if not provided in the request or not mapped in the backend.
- Nested objects (`baseInfo`, `pricing`, `inventory`) provide structured details for frontend use.
- If you expect additional fields, coordinate with the backend team to ensure they are included in the response.

---
For further details or changes, contact the backend team.
