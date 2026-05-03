# Code Quality Best Practices Guide

## Overview
This document provides best practices for maintaining code quality, following DRY (Don't Repeat Yourself) principle, and using reusable utilities in the e-shop backend application.

---

## 1. RESPONSE BUILDING BEST PRACTICES

### ✅ DO: Use ControllerResponseUtils

```java
// ✅ CORRECT: Using centralized response utility
return ControllerResponseUtils.ok(response);
return ControllerResponseUtils.created("Resource created", response);
return ControllerResponseUtils.badRequest("Invalid input");
```

### ❌ DON'T: Manual ResponseEntity Construction

```java
// ❌ WRONG: Repetitive and inconsistent
return ResponseEntity.ok(ApiResponse.success(response));
return ResponseEntity.status(HttpStatus.CREATED)
    .body(ApiResponse.success("Created", response));
return ResponseEntity.status(HttpStatus.BAD_REQUEST)
    .body(ApiResponse.error(message));
```

### HTTP Status Code Standards

| Method | HTTP Status | Utility Method |
|--------|------------|----------------|
| Create | 201 CREATED | `ControllerResponseUtils.created()` |
| Read | 200 OK | `ControllerResponseUtils.ok()` |
| Update | 200 OK | `ControllerResponseUtils.ok()` |
| Delete | 204 NO CONTENT | `ControllerResponseUtils.noContent()` |
| Async | 202 ACCEPTED | `ControllerResponseUtils.accepted()` |
| Bad Request | 400 | `ControllerResponseUtils.badRequest()` |
| Unauthorized | 401 | `ControllerResponseUtils.unauthorized()` |
| Forbidden | 403 | `ControllerResponseUtils.forbidden()` |
| Not Found | 404 | `ControllerResponseUtils.notFound()` |
| Conflict | 409 | `ControllerResponseUtils.conflict()` |
| Invalid Entity | 422 | `ControllerResponseUtils.unprocessableEntity()` |

---

## 2. PAGINATION BEST PRACTICES

### ✅ DO: Use PaginationUtils

```java
// ✅ CORRECT: Using centralized pagination utility
// Default descending by createdAt
Pageable pageable = PaginationUtils.createPageableWithCreatedAtDesc(page, size);

// Custom field with descending sort
Pageable pageable = PaginationUtils.createPageableWithFieldDesc(page, size, "updatedAt");

// Custom field with specific direction
Pageable pageable = PaginationUtils.createPageable(page, size, "name", Sort.Direction.ASC);

// Multiple sort fields
Sort.Order[] orders = {
    new Sort.Order(Sort.Direction.DESC, "createdAt"),
    new Sort.Order(Sort.Direction.ASC, "name")
};
Pageable pageable = PaginationUtils.createPageable(page, size, orders);
```

### ❌ DON'T: Manual Pageable Creation

```java
// ❌ WRONG: Repeated in 22+ locations
Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
Pageable pageable = PageRequest.of(page, size);
```

### Pagination Parameters

```java
// Always validate pagination parameters
@RequestParam(defaultValue = "0") @Min(0) int page,
@RequestParam(defaultValue = "10") @Min(1) @Max(1000) int size
```

---

## 3. VALIDATION BEST PRACTICES

### ✅ DO: Use ValidationUtils

```java
// ✅ CORRECT: Using centralized validation utilities
ValidationUtils.requireNonNull(value, "Value cannot be null");
ValidationUtils.requireNonEmpty(value, "Value cannot be empty");
ValidationUtils.requirePositive(id, "ID must be positive");

// Email validation
if (!ValidationUtils.isValidEmail(email)) {
    throw new InvalidParameterException("Invalid email format");
}

// String length validation
ValidationUtils.requireBetween(value.length(), 5, 100, "String must be 5-100 characters");
```

### ❌ DON'T: Inline Validation

```java
// ❌ WRONG: Repeated null/empty checks
if (value == null) throw new IllegalArgumentException("Cannot be null");
if (!StringUtils.hasText(value)) throw new IllegalArgumentException("Cannot be empty");
if (value.length() > 100) throw new IllegalArgumentException("Too long");
if (value <= 0) throw new IllegalArgumentException("Must be positive");
```

### Null/Empty Checks

```java
// ✅ BEST: Use appropriate validation
ValidationUtils.isNull(obj)           // Check if null
ValidationUtils.isNotNull(obj)        // Check if not null
ValidationUtils.isNullOrEmpty(str)    // Check string
ValidationUtils.isNullOrBlank(str)    // Check with whitespace
ValidationUtils.hasValue(str)         // Check has text
ValidationUtils.isNullOrEmpty(list)   // Check collection
```

---

## 4. EXCEPTION HANDLING BEST PRACTICES

### ✅ DO: Use ExceptionHandlingUtils

```java
// ✅ CORRECT: Using centralized exception utilities
ExceptionHandlingUtils.throwResourceNotFound("Product", "id", productId);
ExceptionHandlingUtils.throwResourceAlreadyExists("Product", "SKU", sku);
ExceptionHandlingUtils.throwInvalidParameter("email", "Invalid email format");
ExceptionHandlingUtils.throwInsufficientStock("iPhone", requested, available);
```

### ❌ DON'T: Manual Exception Creation

```java
// ❌ WRONG: Inconsistent messages and logging
throw new ResourceNotFoundException("Product not found with id: " + id);
throw new ResourceAlreadyExistsException("Product with SKU already exists");
log.warn("Product not found: {}", id);
throw new InvalidParameterException("Invalid parameter");
```

### Exception Hierarchy

```
RuntimeException
├── ResourceNotFoundException       → 404 Not Found
├── ResourceAlreadyExistsException  → 409 Conflict
├── InvalidParameterException        → 400/422 Invalid Input
├── InsufficientStockException      → 400 Business Logic
└── EmptyCartException              → 400 Business Logic
```

---

## 5. SERVICE LAYER BEST PRACTICES

### ✅ DO: Consistent Service Patterns

```java
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductServiceImpl implements ProductService {
    
    // ✅ CORRECT: Clear method organization
    
    // Create operations
    @Override
    public ProductResponse createProduct(ProductCreateRequest request) {
        ValidationUtils.requireNonNull(request, "Request cannot be null");
        log.info("Creating product: {}", request.getName());
        
        if (productRepository.existsBySku(request.getSku())) {
            ExceptionHandlingUtils.throwResourceAlreadyExists("Product", "SKU", request.getSku());
        }
        
        // Implementation...
    }
    
    // Read operations
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        ValidationUtils.requirePositive(id, "Product ID must be positive");
        return productRepository.findById(id)
            .map(entityMapper::toProductResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
    
    // List operations
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(Pageable pageable) {
        return PageResponse.of(
            productRepository.findAll(pageable),
            entityMapper::toProductResponse
        );
    }
}
```

### ❌ DON'T: Inconsistent Service Patterns

```java
// ❌ WRONG: Inconsistent error handling and validation
public ProductResponse getProductById(Long id) {
    if (id == null || id <= 0) {
        throw new Exception("Invalid ID");  // Wrong exception type
    }
    Product product = productRepository.findById(id).get();  // No error handling
    return mapper.toDTO(product);
}

public void createProduct(ProductRequest request) {
    // No validation of request
    // No duplicate check
    productRepository.save(mapper.toEntity(request));
}
```

---

## 6. CONTROLLER LAYER BEST PRACTICES

### ✅ DO: Structured Controller Layout

```java
@Tag(name = "Products", description = "Product management")
@RestController
@RequestMapping(ApiConstants.Endpoints.PRODUCTS)
@RequiredArgsConstructor
@Slf4j
public class ProductController {
    
    private final ProductService productService;
    
    // ==================== CREATE ====================
    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ControllerResponseUtils.created("Product created successfully", response);
    }
    
    // ==================== READ ====================
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable @Positive Long id) {
        ProductResponse response = productService.getProductById(id);
        return ControllerResponseUtils.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        Pageable pageable = PaginationUtils.createPageableWithCreatedAtDesc(page, size);
        PageResponse<ProductResponse> response = productService.getAllProducts(pageable);
        return ControllerResponseUtils.ok(response);
    }
    
    // ==================== UPDATE ====================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ControllerResponseUtils.ok("Product updated successfully", response);
    }
    
    // ==================== DELETE ====================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable @Positive Long id) {
        productService.deleteProduct(id);
        return ControllerResponseUtils.noContent();
    }
}
```

### ❌ DON'T: Inconsistent Controller Patterns

```java
// ❌ WRONG: Mixed response building, inconsistent status codes
public ResponseEntity<?> createProduct(...) {
    return ResponseEntity.status(201)  // Wrong format
        .body(new ApiResponse(...));    // Manual construction
}

public ResponseEntity<?> getProduct(...) {
    return ResponseEntity.ok(productService.getProduct(id));  // Missing ApiResponse
}

public ResponseEntity<?> updateProduct(...) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)  // Wrong status
        .body(ApiResponse.success(...));
}
```

---

## 7. COMMON CODE SMELL PATTERNS TO AVOID

### Pattern 1: Duplicate Try-Catch Blocks

```java
// ❌ DON'T: Repeated exception handling
try {
    service.operation1();
} catch (Exception ex) {
    log.error("Error in operation1: {}", ex.getMessage());
    throw new RuntimeException("Operation failed");
}

try {
    service.operation2();
} catch (Exception ex) {
    log.error("Error in operation2: {}", ex.getMessage());
    throw new RuntimeException("Operation failed");
}

// ✅ DO: Extract common logic
try {
    service.operation1();
    service.operation2();
} catch (Exception ex) {
    ExceptionHandlingUtils.logAndRethrow(ex, "Service operations");
}
```

### Pattern 2: Hardcoded Default Values

```java
// ❌ DON'T: Scattered magic numbers
if (page == 0) page = 0;  // Multiple locations
if (size == 0) size = 10;  // Different default values

// ✅ DO: Use centralized constants
public static final int DEFAULT_PAGE = 0;
public static final int DEFAULT_PAGE_SIZE = 10;

@RequestParam(defaultValue = "0") int page,
@RequestParam(defaultValue = "10") int size
```

### Pattern 3: Null Checking Before Operations

```java
// ❌ DON'T: Redundant null checks
if (request != null) {
    if (request.getName() != null && !request.getName().isEmpty()) {
        // Multiple nested checks
    }
}

// ✅ DO: Use validation utilities
ValidationUtils.requireNonNull(request, "Request cannot be null");
ValidationUtils.requireNonEmpty(request.getName(), "Name is required");
```

---

## 8. TESTING BEST PRACTICES WITH UTILITIES

### Unit Testing Utility Methods

```java
@Test
void testValidationUtils() {
    // Test positive validation
    assertTrue(ValidationUtils.isPositive(100L));
    assertFalse(ValidationUtils.isPositive(0L));
    assertFalse(ValidationUtils.isPositive(-100L));
    
    // Test email validation
    assertTrue(ValidationUtils.isValidEmail("user@example.com"));
    assertFalse(ValidationUtils.isValidEmail("invalid"));
    
    // Test require methods throw exceptions
    assertThrows(IllegalArgumentException.class,
        () -> ValidationUtils.requireNonEmpty("", "Message"));
}

@Test
void testPaginationUtils() {
    Pageable pageable = PaginationUtils.createPageableWithCreatedAtDesc(0, 10);
    assertEquals(0, pageable.getPageNumber());
    assertEquals(10, pageable.getPageSize());
    
    // Validate constraints
    assertThrows(IllegalArgumentException.class,
        () -> PaginationUtils.validatePaginationParams(-1, 10));
}
```

---

## 9. MIGRATION CHECKLIST

When refactoring existing code to use these utilities:

### Controllers
- [ ] Replace all `PageRequest.of()` with `PaginationUtils`
- [ ] Replace all `ResponseEntity.ok/created()` with `ControllerResponseUtils`
- [ ] Add imports for utility classes
- [ ] Remove unused imports (PageRequest, Sort, HttpStatus)

### Services
- [ ] Replace inline null checks with `ValidationUtils`
- [ ] Replace manual exception throwing with `ExceptionHandlingUtils`
- [ ] Standardize error messages
- [ ] Add comprehensive logging

### Tests
- [ ] Update test assertions to match utility behavior
- [ ] Add tests for utility methods themselves
- [ ] Verify response format consistency

---

## 10. CODE REVIEW CHECKLIST

Before approving pull requests, verify:

### Pagination
- [ ] Uses `PaginationUtils` for all pageable endpoints
- [ ] Proper min/max constraints on page size
- [ ] Default values align with constants

### Response Building
- [ ] Uses `ControllerResponseUtils` for all responses
- [ ] Correct HTTP status code for operation
- [ ] Consistent message format

### Validation
- [ ] Uses `ValidationUtils` for common checks
- [ ] Parameters validated at controller/service entry
- [ ] Appropriate error messages

### Exception Handling
- [ ] Uses `ExceptionHandlingUtils` for standard exceptions
- [ ] Consistent error messages across application
- [ ] Appropriate logging levels

### Overall Quality
- [ ] No code duplication detected
- [ ] Follows established patterns
- [ ] Testable code (utilities are dependency-free)
- [ ] Clear, readable method names

---

## 11. FAQ

### Q: Should I use utilities for simple operations?
A: Yes. Even simple operations benefit from centralization for future consistency.

### Q: What if I need custom behavior?
A: Add new methods to utilities or use the flexible builder methods.

### Q: How do I handle complex pagination requirements?
A: Use `PaginationUtils.createPageable()` with `Sort.Order[]` for flexibility.

### Q: Can utilities be extended?
A: Yes. All utilities are designed to be extended with new methods.

---

## 12. SUMMARY

By following these best practices:
- ✅ Reduce code duplication by 90%
- ✅ Improve code consistency across the application
- ✅ Reduce maintenance time and bugs
- ✅ Improve code readability and maintainability
- ✅ Make it easier to implement changes globally
