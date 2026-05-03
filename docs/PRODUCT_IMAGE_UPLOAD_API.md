# Product Image Upload API - Technical Reference

## Overview

Product image upload system with local file storage for development and designed for easy migration to cloud storage (Cloudflare R2) in production.

**Version**: 1.0  
**Last Updated**: February 7, 2026  
**Storage Provider**: Local File System (Development)

---

## Architecture

### Storage Abstraction Layer

```
┌─────────────────────────────────────┐
│  ProductImageController             │
│  (REST API Endpoints)               │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  ProductImageService                │
│  (Business Logic)                   │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  ImageStorageFactory                │
│  (Provider Selection)               │
└──────────────┬──────────────────────┘
               │
       ┌───────┴───────┐
       ▼               ▼
┌─────────────┐  ┌──────────────────┐
│   Local     │  │   Cloudinary     │
│  Storage    │  │   / Bunny.net    │
└─────────────┘  └──────────────────┘
```

---

## Configuration

### application.properties

```properties
# ═══════════════════════════════════════════════════════════
# FILE UPLOAD LIMITS
# ═══════════════════════════════════════════════════════════
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=50MB

# ═══════════════════════════════════════════════════════════
# IMAGE STORAGE CONFIGURATION
# ═══════════════════════════════════════════════════════════
# Provider: local (default), cloudinary, bunny
image.storage.provider=${IMAGE_STORAGE_PROVIDER:local}

# Local Storage Settings (Development)
app.storage.upload-dir=${UPLOAD_DIR:./uploads}
app.storage.base-url=${APP_BASE_URL:http://localhost:8082}

# ═══════════════════════════════════════════════════════════
# IMAGE UPLOAD VALIDATION
# ═══════════════════════════════════════════════════════════
app.upload.max-file-size=5242880          # 5MB in bytes
app.upload.max-files=10
app.upload.allowed-mime-types=image/jpeg,image/png,image/webp
app.upload.allowed-extensions=jpg,jpeg,png,webp
app.upload.max-image-width=1920
app.upload.max-image-height=1080
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `IMAGE_STORAGE_PROVIDER` | `local` | Storage provider: `local`, `cloudinary`, `bunny` |
| `UPLOAD_DIR` | `./uploads` | Base directory for uploaded files |
| `APP_BASE_URL` | `http://localhost:8082` | Base URL for generating image URLs |

---

## API Endpoints

### Base URL
```
http://localhost:8082/api/product-images
```

### Authentication
All endpoints except GET require authentication with `SELLER` or `ADMIN` role.

**Header**:
```
Authorization: Bearer {jwt_token}
```

---

### 1. Upload Product Image

**Endpoint**: `POST /api/product-images/upload`

**Content-Type**: `multipart/form-data`

**Request Parameters**:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `file` | File | Yes | Image file (JPG, PNG, WEBP) |
| `productId` | Long | Yes | Product ID to associate image with |
| `altText` | String | No | Alternative text for accessibility |
| `isPrimary` | Boolean | No | Set as primary image (default: false) |

**cURL Example**:
```bash
curl -X POST http://localhost:8082/api/product-images/upload \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI..." \
  -F "productId=123" \
  -F "file=@/path/to/image.jpg" \
  -F "altText=Product showcase image" \
  -F "isPrimary=true"
```

**Success Response** (201 Created):
```json
{
  "success": true,
  "message": "Product image uploaded",
  "data": {
    "id": 1,
    "productId": 123,
    "url": "http://localhost:8082/uploads/products/123/20260207223500_a1b2c3d4.jpg",
    "thumbnailUrl": "http://localhost:8082/uploads/products/123/thumb_20260207223500_a1b2c3d4.jpg",
    "altText": "Product showcase image",
    "isPrimary": true,
    "sortOrder": 0,
    "width": 1920,
    "height": 1080,
    "fileSize": 245678,
    "provider": "LocalImageStorageService",
    "publicId": "products/123/20260207223500_a1b2c3d4.jpg",
    "active": true,
    "createdAt": "2026-02-07T22:35:00Z",
    "updatedAt": "2026-02-07T22:35:00Z"
  }
}
```

**Error Responses**:

| Status | Code | Message |
|--------|------|---------|
| 400 | `INVALID_FILE_SIZE` | File size exceeds maximum allowed size of 5 MB |
| 400 | `INVALID_FILE_TYPE` | Invalid file type: {type}. Allowed types: JPG, PNG, WEBP |
| 401 | `UNAUTHORIZED` | Authentication required |
| 403 | `FORBIDDEN` | Insufficient permissions (requires SELLER or ADMIN) |
| 404 | `PRODUCT_NOT_FOUND` | Product not found with id: {id} |
| 500 | `IMAGE_UPLOAD_FAILED` | Failed to upload image: {error} |

---

### 2. Get Product Images

**Endpoint**: `GET /api/product-images/product/{productId}`

**Parameters**:
- `productId` (path) - Product ID

**Success Response** (200 OK):
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "productId": 123,
      "url": "http://localhost:8082/uploads/products/123/20260207223500_a1b2c3d4.jpg",
      "thumbnailUrl": "http://localhost:8082/uploads/products/123/thumb_20260207223500_a1b2c3d4.jpg",
      "isPrimary": true,
      "sortOrder": 0
    },
    {
      "id": 2,
      "productId": 123,
      "url": "http://localhost:8082/uploads/products/123/20260207223530_e5f6g7h8.png",
      "thumbnailUrl": "http://localhost:8082/uploads/products/123/thumb_20260207223530_e5f6g7h8.png",
      "isPrimary": false,
      "sortOrder": 1
    }
  ]
}
```

---

### 3. Delete Product Image

**Endpoint**: `DELETE /api/product-images/{imageId}`

**Parameters**:
- `imageId` (path) - Image ID to delete

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "Product image deleted successfully",
  "data": null
}
```

**Behavior**:
- Soft delete (sets `active=false`)
- Attempts to delete file from storage
- Deletes thumbnail if exists

---

### 4. Set Primary Image

**Endpoint**: `PUT /api/product-images/product/{productId}/primary/{imageId}`

**Parameters**:
- `productId` (path) - Product ID
- `imageId` (path) - Image ID to set as primary

**Success Response** (200 OK):
```json
{
  "success": true,
  "message": "Primary image set successfully",
  "data": {
    "id": 2,
    "productId": 123,
    "isPrimary": true
  }
}
```

**Behavior**:
- Unsets all other images as primary for this product
- Sets specified image as primary

---

## Storage Implementation

### Local File Storage

**Class**: `LocalImageStorageService`

**Storage Path**:
```
{app.storage.upload-dir}/products/{productId}/{filename}
```

**Example**:
```
./uploads/products/123/20260207223500_a1b2c3d4.jpg
./uploads/products/123/thumb_20260207223500_a1b2c3d4.jpg
```

**Filename Format**:
```
{timestamp}_{uuid}.{extension}
```
- `timestamp`: yyyyMMddHHmmss format
- `uuid`: First 8 characters of UUID
- `extension`: Original file extension (lowercase)

**Features**:
- ✅ Automatic directory creation
- ✅ Unique filename generation
- ✅ Filename sanitization (removes special characters)
- ✅ Directory traversal prevention
- ✅ Automatic thumbnail generation (150x150px)
- ✅ Image dimension detection
- ✅ File size and type validation

---

### Where Images Are Stored

#### Absolute Path (When running from project root)

```
G:\Project\eshop_back\
└── uploads\
    └── products\
        └── {productId}\
            ├── 20260207224000_abc123.jpg        ← Original image
            ├── thumb_20260207224000_abc123.jpg  ← Thumbnail (150x150)
            ├── 20260207224030_def456.webp
            └── thumb_20260207224030_def456.webp
```

**Base Directory**: `G:\Project\eshop_back\uploads\`
- Default configured by: `app.storage.upload-dir=./uploads`
- Relative to Spring Boot application startup directory

**Product Subfolders**: `uploads\products\{productId}\`
- Each product gets its own folder
- Automatically created on first image upload
- Example: Product ID `123` → `uploads\products\123\`

**No Manual Folder Creation Required** ✅
- Backend automatically creates folders
- Just upload images via API
- Folders are created with proper permissions

#### Access URLs

**Original Image**:
```
http://localhost:8082/uploads/products/{productId}/{filename}
```

**Thumbnail**:
```
http://localhost:8082/uploads/products/{productId}/thumb_{filename}
```

**Example URLs**:
- Original: `http://localhost:8082/uploads/products/123/20260207224000_abc123.jpg`
- Thumbnail: `http://localhost:8082/uploads/products/123/thumb_20260207224000_abc123.jpg`

#### Customizing Storage Location

**Option 1: Configuration File**

Edit `application.properties`:
```properties
# Use absolute path
app.storage.upload-dir=D:/ProductImages

# Or network path
app.storage.upload-dir=//nas-server/shared/uploads
```

**Option 2: Environment Variable**
```bash
# Windows
set UPLOAD_DIR=D:\ProductImages

# Linux/Mac
export UPLOAD_DIR=/var/www/product-images
```

**Option 3: Docker Volume**
```yaml
volumes:
  - ./product-images:/app/uploads
```

---

## Validation Rules

### File Size
- **Maximum**: 5 MB (5,242,880 bytes)
- **Validation**: Apache Tika MIME type detection
- **Error**: `ImageUploadException` with descriptive message

### File Types

**Allowed MIME Types**:
- `image/jpeg`
- `image/png`
- `image/webp`

**Allowed Extensions**:
- `.jpg`, `.jpeg`
- `.png`
- `.webp`

**Validation Method**: Dual validation
1. MIME type detection using Apache Tika (prevents fake extensions)
2. File extension check

### Security

**Filename Sanitization**:
- Removes all special characters except `._-`
- Replaces invalid characters with `_`
- Prevents directory traversal (`../`, `..\\`)
- Maximum length: 100 characters

**Example**:
- Input: `../../malicious file (copy).jpg`
- Output: `malicious_file__copy_.jpg`
- Final: `20260207223500_a1b2c3d4.jpg`

---

## Database Schema

### product_images Table

```sql
CREATE TABLE product_images (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    url VARCHAR(1000) NOT NULL,
    alt_text VARCHAR(255),
    provider VARCHAR(50),
    public_id VARCHAR(500),
    thumbnail_url VARCHAR(500),
    width INTEGER,
    height INTEGER,
    file_size BIGINT,
    is_primary BOOLEAN DEFAULT FALSE,
    sort_order INTEGER DEFAULT 0,
    image_type VARCHAR(20) DEFAULT 'GALLERY',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_image_product (product_id),
    INDEX idx_image_primary (is_primary),
    INDEX idx_image_sort (sort_order)
);
```

---

## Frontend Integration

### API Client

**File**: `lib/api/product-images.ts`

```typescript
const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8082';

export const productImagesApi = {
  async upload(
    productId: string,
    file: File,
    altText?: string,
    isPrimary = false
  ): Promise<ProductImage> {
    const formData = new FormData();
    formData.append('productId', productId);
    formData.append('file', file);
    if (altText) formData.append('altText', altText);
    formData.append('isPrimary', String(isPrimary));

    const response = await fetch(`${API_BASE}/api/product-images/upload`, {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) throw new Error('Upload failed');
    return response.json();
  }
};
```

### Usage Example

```typescript
// In product creation form
const handleSubmit = async (productData) => {
  // 1. Create product
  const product = await createProduct(productData);
  
  // 2. Upload images
  for (const [index, file] of imageFiles.entries()) {
    await productImagesApi.upload(
      product.id,
      file,
      file.name,
      index === 0 // First image is primary
    );
  }
};
```

---

## Error Handling

### Service Layer Exceptions

| Exception | HTTP Status | Description |
|-----------|-------------|-------------|
| `ImageUploadException` | 400 | File validation failed |
| `ResourceNotFoundException` | 404 | Product or image not found |
| `ConflictException` | 409 | Business logic conflict |
| `IOException` | 500 | File system error |

### Example Error Response

```json
{
  "success": false,
  "error": {
    "code": "INVALID_FILE_SIZE",
    "message": "File size exceeds maximum allowed size of 5 MB",
    "timestamp": "2026-02-07T22:35:00Z"
  }
}
```

---

## Migration to Cloud Storage

### Switching to Cloudflare R2 (Future)

**Step 1**: Create R2 Service Implementation
```java
@Service("r2StorageService")
public class CloudflareR2StorageService implements ImageStorageService {
    @Override
    public ImageUploadResult upload(byte[] bytes, String filename, String folder) {
        // Implement R2 upload using AWS S3 SDK
    }
    
    @Override
    public void delete(String publicId, String folder) {
        // Implement R2 delete
    }
}
```

**Step 2**: Update Configuration
```properties
# Switch provider
image.storage.provider=r2

# Add R2 credentials
app.storage.r2.account-id=${R2_ACCOUNT_ID}
app.storage.r2.access-key=${R2_ACCESS_KEY}
app.storage.r2.secret-key=${R2_SECRET_KEY}
app.storage.r2.bucket-name=product-images
app.storage.r2.public-url=https://images.yourdomain.com
```

**Step 3**: Update Factory
```java
@Component
public class ImageStorageFactory {
    private final LocalImageStorageService localService;
    private final CloudflareR2StorageService r2Service;
    
    public ImageStorageService get() {
        switch (provider.toLowerCase()) {
            case "r2": return r2Service;
            case "local": return localService;
            default: return localService;
        }
    }
}
```

**No frontend changes required** ✅

---

## Testing

### Manual Testing

1. **Upload Valid Image**:
   ```bash
   curl -X POST http://localhost:8082/api/product-images/upload \
     -F "productId=1" -F "file=@test.jpg"
   ```

2. **Upload Oversized File** (expect 400):
   ```bash
   curl -X POST http://localhost:8082/api/product-images/upload \
     -F "productId=1" -F "file=@large6mb.jpg"
   ```

3. **Upload Invalid Type** (expect 400):
   ```bash
   curl -X POST http://localhost:8082/api/product-images/upload \
     -F "productId=1" -F "file=@document.pdf"
   ```

### Verification

- Check files exist in: `./uploads/products/{productId}/`
- Verify thumbnails created with `thumb_` prefix
- Access via browser: `http://localhost:8082/uploads/products/1/...jpg`
- Check database: `SELECT * FROM product_images WHERE product_id = 1;`

---

## Troubleshooting

### Images Not Uploading

**Symptoms**: Upload returns 500 error

**Solutions**:
1. Check `./uploads` directory exists and is writable
2. Verify Spring Boot has file system permissions
3. Check logs for `ImageUploadException` details

### Images Not Accessible

**Symptoms**: 404 when accessing image URL

**Solutions**:
1. Verify `WebMvcConfig` has `/uploads/**` resource handler
2. Check file exists on disk
3. Verify `app.storage.base-url` matches your server URL

### Thumbnails Not Generated

**Symptoms**: `thumbnailUrl` is null or 404

**Solutions**:
1. Check Thumbnailator library is in classpath
2. Verify source image is valid format
3. Check disk space available

---

## Performance Considerations

### Optimization Tips

1. **Async Upload**: Consider async processing for large batches
2. **CDN Integration**: Serve from CDN in production
3. **Image Compression**: Consider compressing before upload
4. **Lazy Loading**: Load thumbnails first, full images on demand

### Monitoring

**Metrics to Track**:
- Upload success rate
- Average upload time
- Storage usage
- Failed upload reasons

---

## Security Checklist

- ✅ MIME type validation (prevents fake extensions)
- ✅ File size limits enforced
- ✅ Filename sanitization (prevents directory traversal)
- ✅ Authentication required for uploads
- ✅ Role-based authorization (SELLER/ADMIN only)
- ⚠️ Virus scanning (disabled by default, enable in production)
- ⚠️ Rate limiting (consider adding for production)

---

## Changelog

### Version 1.0 (2026-02-07)
- Initial implementation with local file storage
- Support for JPG, PNG, WEBP formats
- Automatic thumbnail generation
- File validation and security features
- RESTful API endpoints
- Database schema integration

---

## Support

For issues or questions:
1. Check logs in `logs/application.log`
2. Review this documentation
3. Check implementation plan and walkthrough artifacts
4. Consult team lead or senior developer

---

**Document Version**: 1.0  
**Last Updated**: February 7, 2026  
**Maintained By**: Development Team
