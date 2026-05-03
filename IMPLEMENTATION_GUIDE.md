# Implementation Guide - Utility Classes and Refactoring

## Quick Start

### For New Development
Add these imports to your controller or service:
```java
import com.eshop.app.util.PaginationUtils;
import com.eshop.app.util.ControllerResponseUtils;
import com.eshop.app.util.ValidationUtils;
import com.eshop.app.util.ExceptionHandlingUtils;
```

### For Existing Code Migration
Run search and replace operations to update:
1. All `PageRequest.of()` calls → `PaginationUtils`
2. All `ResponseEntity.*()` calls → `ControllerResponseUtils`
3. All null/empty checks → `ValidationUtils`
4. All exception throws → `ExceptionHandlingUtils`

---

## Utility Classes Reference

### 1. PaginationUtils

#### Location
`src/main/java/com/eshop/app/util/PaginationUtils.java`

#### Common Use Cases

**Case 1: Get paginated list with default sort (descending by createdAt)**
```java
@GetMapping
public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    
    Pageable pageable = PaginationUtils.createPageableWithCreatedAtDesc(page, size);
    PageResponse<UserResponse> response = userService.getAllUsers(pageable);
    return ControllerResponseUtils.ok(response);
}
```

**Case 2: Custom field with descending sort**
```java
@GetMapping("/trending")
public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getTrendingProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    
    Pageable pageable = PaginationUtils.createPageableWithFieldDesc(page, size, "views");
    PageResponse<ProductResponse> response = productService.getTrendingProducts(pageable);
    return ControllerResponseUtils.ok(response);
}
```

**Case 3: User-controlled sort direction**
```java
@GetMapping("/sorted")
public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getSortedProducts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "DESC") String direction) {
    
    Sort.Direction sortDirection = Sort.Direction.fromOptionalString(direction)
        .orElse(Sort.Direction.DESC);
    Pageable pageable = PaginationUtils.createPageable(page, size, sortBy, sortDirection);
    
    PageResponse<ProductResponse> response = productService.getSortedProducts(pageable);
    return ControllerResponseUtils.ok(response);
}
```

**Case 4: Multiple sort fields**
```java
@GetMapping("/custom-sort")
public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {
    
    Sort.Order[] orders = {
        new Sort.Order(Sort.Direction.DESC, "totalAmount"),
        new Sort.Order(Sort.Direction.ASC, "customerName")
    };
    Pageable pageable = PaginationUtils.createPageable(page, size, orders);
    
    PageResponse<OrderResponse> response = orderService.getOrders(pageable);
    return ControllerResponseUtils.ok(response);
}
```

---

### 2. ControllerResponseUtils

#### Location
`src/main/java/com/eshop/app/util/ControllerResponseUtils.java`

#### Common Use Cases

**Case 1: Successful creation (201 CREATED)**
```java
@PostMapping
public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
        @Valid @RequestBody ProductCreateRequest request) {
    
    ProductResponse response = productService.createProduct(request);
    return ControllerResponseUtils.created("Product created successfully", response);
}
```

**Case 2: Successful read (200 OK)**
```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
        @PathVariable @Positive Long id) {
    
    ProductResponse response = productService.getProductById(id);
    return ControllerResponseUtils.ok(response);
}
```

**Case 3: Successful update (200 OK with message)**
```java
@PutMapping("/{id}")
public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
        @PathVariable @Positive Long id,
        @Valid @RequestBody ProductUpdateRequest request) {
    
    ProductResponse response = productService.updateProduct(id, request);
    return ControllerResponseUtils.ok("Product updated successfully", response);
}
```

**Case 4: Successful delete (204 NO CONTENT)**
```java
@DeleteMapping("/{id}")
public ResponseEntity<ApiResponse<Void>> deleteProduct(
        @PathVariable @Positive Long id) {
    
    productService.deleteProduct(id);
    return ControllerResponseUtils.noContent();
}
```

**Case 5: Async operation (202 ACCEPTED)**
```java
@PostMapping("/export")
public ResponseEntity<ApiResponse<ExportResponse>> exportProducts(
        @RequestBody ExportRequest request) {
    
    ExportResponse response = productService.exportProductsAsync(request);
    return ControllerResponseUtils.accepted("Export initiated", response);
}
```

**Case 6: Bad request error (400)**
```java
@PostMapping
public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
        @Valid @RequestBody ProductCreateRequest request) {
    
    if (!ValidationUtils.isValidEmail(request.getSellerEmail())) {
        return ControllerResponseUtils.badRequest(
            "Invalid email format for seller");
    }
    
    ProductResponse response = productService.createProduct(request);
    return ControllerResponseUtils.created("Product created", response);
}
```

**Case 7: Not found error (404)**
```java
@GetMapping("/{id}")
public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
        @PathVariable @Positive Long id) {
    
    try {
        ProductResponse response = productService.getProductById(id);
        return ControllerResponseUtils.ok(response);
    } catch (ResourceNotFoundException ex) {
        return ControllerResponseUtils.notFound(ex.getMessage());
    }
}
```

**Case 8: Conflict error (409)**
```java
@PostMapping
public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
        @Valid @RequestBody ProductCreateRequest request) {
    
    try {
        ProductResponse response = productService.createProduct(request);
        return ControllerResponseUtils.created("Product created", response);
    } catch (ResourceAlreadyExistsException ex) {
        return ControllerResponseUtils.conflict(
            "Product with SKU " + request.getSku() + " already exists");
    }
}
```

---

### 3. ValidationUtils

#### Location
`src/main/java/com/eshop/app/util/ValidationUtils.java`

#### Common Use Cases

**Case 1: Null and empty checks in service**
```java
@Override
public ProductResponse createProduct(ProductCreateRequest request) {
    // Require non-null request
    ValidationUtils.requireNonNull(request, "Request cannot be null");
    
    // Require non-empty name
    ValidationUtils.requireNonEmpty(request.getName(), 
        "Product name is required");
    
    // Require valid email
    if (!ValidationUtils.isValidEmail(request.getSellerEmail())) {
        throw new InvalidParameterException("Invalid email address");
    }
    
    // Continue with business logic...
}
```

**Case 2: Numeric validations**
```java
@Override
public void updateProductStock(Long productId, int quantity) {
    // Require positive product ID
    ValidationUtils.requirePositive(productId, 
        "Product ID must be positive");
    
    // Require quantity between 0 and 10000
    ValidationUtils.requireBetween(quantity, 0, 10000,
        "Quantity must be between 0 and 10000");
    
    // Continue...
}
```

**Case 3: String length validation**
```java
@Override
public CategoryResponse createCategory(CategoryRequest request) {
    String name = request.getName();
    
    // Validate length between 3 and 100 characters
    if (!ValidationUtils.isLengthValid(name, 3, 100)) {
        throw new InvalidParameterException(
            "Category name must be 3-100 characters");
    }
    
    // Continue...
}
```

**Case 4: Collection validation**
```java
@Override
public List<ProductResponse> createBatch(List<ProductCreateRequest> requests) {
    // Require non-empty list
    ValidationUtils.requireNonEmpty(requests,
        "Request list cannot be empty");
    
    // Validate each request
    for (ProductCreateRequest request : requests) {
        ValidationUtils.requireNonNull(request, 
            "Individual request cannot be null");
    }
    
    // Continue...
}
```

**Case 5: Email and phone validation**
```java
@Override
public UserResponse createUser(UserCreateRequest request) {
    // Validate email
    if (!ValidationUtils.isValidEmail(request.getEmail())) {
        throw new InvalidParameterException("Invalid email format");
    }
    
    // Validate phone (if provided)
    if (ValidationUtils.hasValue(request.getPhone())) {
        if (!ValidationUtils.isValidPhone(request.getPhone())) {
            throw new InvalidParameterException("Invalid phone format");
        }
    }
    
    // Continue...
}
```

---

### 4. ExceptionHandlingUtils

#### Location
`src/main/java/com/eshop/app/util/ExceptionHandlingUtils.java`

#### Common Use Cases

**Case 1: Resource not found with logging**
```java
@Override
public ProductResponse getProductById(Long id) {
    return productRepository.findById(id)
        .map(this::toResponse)
        .orElseThrow(() -> {
            ExceptionHandlingUtils.throwResourceNotFound(
                "Product", "id", id);
            return null; // Never reached
        });
}
```

**Case 2: Resource already exists check**
```java
@Override
public ProductResponse createProduct(ProductCreateRequest request) {
    if (productRepository.existsBySku(request.getSku())) {
        ExceptionHandlingUtils.throwResourceAlreadyExists(
            "Product", "SKU", request.getSku());
    }
    
    // Continue with creation...
}
```

**Case 3: Invalid parameter**
```java
@Override
public PageResponse<ProductResponse> searchProducts(String keyword) {
    String sanitized = keyword.trim();
    
    if (sanitized.length() < 2) {
        ExceptionHandlingUtils.throwInvalidParameter(
            "keyword", "Must be at least 2 characters");
    }
    
    // Continue...
}
```

**Case 4: Business logic exception**
```java
@Override
public OrderResponse createOrder(OrderCreateRequest request) {
    for (CartItem item : cart.getItems()) {
        if (item.getProduct().getStock() < item.getQuantity()) {
            ExceptionHandlingUtils.throwInsufficientStock(
                item.getProduct().getName(),
                item.getQuantity(),
                item.getProduct().getStock());
        }
    }
    
    // Continue...
}
```

**Case 5: Conditional validation with custom exception**
```java
@Override
public void updateOrderStatus(Long orderId, String newStatus) {
    Order order = findOrderById(orderId);
    
    // Validate status transition
    ExceptionHandlingUtils.validateCondition(
        canTransitionTo(order.getStatus(), newStatus),
        () -> new InvalidParameterException(
            "Cannot transition from " + order.getStatus() + 
            " to " + newStatus),
        "Invalid order status transition"
    );
    
    // Continue...
}
```

---

## Migration Checklist

### Controller Migration

For each controller file:

1. **Add imports**
   ```java
   import com.eshop.app.util.PaginationUtils;
   import com.eshop.app.util.ControllerResponseUtils;
   ```

2. **Remove old imports** (if they were only used for PageRequest/ResponseEntity)
   ```java
   // Remove these if no longer needed:
   import org.springframework.data.domain.PageRequest;
   import org.springframework.data.domain.Sort;
   import org.springframework.http.HttpStatus;
   ```

3. **Replace pagination creations**
   ```java
   // BEFORE
   Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
   
   // AFTER
   Pageable pageable = PaginationUtils.createPageableWithCreatedAtDesc(page, size);
   ```

4. **Replace response building**
   ```java
   // BEFORE
   return ResponseEntity.ok(ApiResponse.success(response));
   return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("msg", response));
   
   // AFTER
   return ControllerResponseUtils.ok(response);
   return ControllerResponseUtils.created("msg", response);
   ```

5. **Run tests** to verify behavior is unchanged

### Service Migration

For each service implementation:

1. **Add import**
   ```java
   import com.eshop.app.util.ValidationUtils;
   import com.eshop.app.util.ExceptionHandlingUtils;
   ```

2. **Replace null checks**
   ```java
   // BEFORE
   if (request == null) throw new IllegalArgumentException("Request cannot be null");
   if (!StringUtils.hasText(value)) throw new IllegalArgumentException("Value required");
   
   // AFTER
   ValidationUtils.requireNonNull(request, "Request cannot be null");
   ValidationUtils.requireNonEmpty(value, "Value required");
   ```

3. **Replace exception throws**
   ```java
   // BEFORE
   throw new ResourceNotFoundException("Product not found with id: " + id);
   throw new ResourceAlreadyExistsException("SKU already exists");
   
   // AFTER
   ExceptionHandlingUtils.throwResourceNotFound("Product", "id", id);
   ExceptionHandlingUtils.throwResourceAlreadyExists("Product", "SKU", sku);
   ```

---

## Testing Examples

### Unit Testing Utilities

```java
@SpringBootTest
class PaginationUtilsTest {
    
    @Test
    void testCreatePageableWithCreatedAtDesc() {
        Pageable pageable = PaginationUtils.createPageableWithCreatedAtDesc(0, 10);
        
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals("createdAt: DESC", pageable.getSort().toString());
    }
    
    @Test
    void testValidatePaginationParamsThrowsOnNegativePage() {
        assertThrows(IllegalArgumentException.class,
            () -> PaginationUtils.validatePaginationParams(-1, 10));
    }
}

@SpringBootTest
class ValidationUtilsTest {
    
    @Test
    void testRequireNonEmptyThrowsOnEmpty() {
        assertThrows(IllegalArgumentException.class,
            () -> ValidationUtils.requireNonEmpty("", "Message"));
    }
    
    @Test
    void testIsValidEmailWithValidEmail() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
        assertFalse(ValidationUtils.isValidEmail("invalid"));
    }
}
```

### Integration Testing

```java
@SpringBootTest
@WebMvcTest(ProductController.class)
class ProductControllerTest {
    
    @MockBean
    private ProductService productService;
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testGetProductReturnsOk() throws Exception {
        ProductResponse response = new ProductResponse(...);
        when(productService.getProductById(1L)).thenReturn(response);
        
        mockMvc.perform(get("/api/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
    
    @Test
    void testCreateProductReturnsCreated() throws Exception {
        ProductResponse response = new ProductResponse(...);
        when(productService.createProduct(any())).thenReturn(response);
        
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").exists());
    }
}
```

---

## Common Issues & Solutions

### Issue 1: Import not found for utility classes
**Solution**: Ensure the util package is properly created with all 4 utility classes.

### Issue 2: PageRequest/Sort still imported
**Solution**: Remove unused imports - they're no longer needed with PaginationUtils.

### Issue 3: ResponseEntity casting issues
**Solution**: Use `ControllerResponseUtils` method return types directly.

### Issue 4: Validation exception types don't match
**Solution**: Use ExceptionHandlingUtils which creates the correct exception types.

---

## Performance Considerations

✅ **Zero Performance Impact**
- All utility methods are static (no instantiation)
- No reflection or dynamic code execution
- Direct method calls with minimal overhead
- Same execution path as manual implementation

---

## Security Best Practices

✅ **Enhanced Security Features**
- Input validation in `ValidationUtils` prevents common attacks
- Email/phone validation prevents spam
- Consistent error handling prevents information leakage
- ExceptionHandlingUtils provides secure error messages

---

## Summary

This implementation guide provides:
- ✅ Quick reference for all utility classes
- ✅ Real-world use cases for each utility
- ✅ Migration checklist for existing code
- ✅ Testing examples
- ✅ Troubleshooting guide
- ✅ Performance and security guarantees

**Next Steps**: 
1. Use this guide for new development
2. Follow the migration checklist for existing code
3. Refer to BEST_PRACTICES_GUIDE.md for design patterns
4. Check REFACTORING_REPORT.md for detailed analysis
