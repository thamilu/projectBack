# 🚀 Quick Reference Guide - Enterprise Features

## Rate Limiting

### Available Tiers
```java
"public"        // 100 req/min  - Public endpoints
"authenticated" // 500 req/min  - Logged-in users
"premium"       // 2000 req/min - Sellers/premium
"admin"         // 5000 req/min - Admin operations
"analytics"     // 20 req/min   - Resource-intensive
"payment"       // 10 req/min   - Payment processing
"upload"        // 30 req/hour  - File uploads
```

### Usage
```java
@GetMapping("/products")
@RateLimited(value = "public", keyType = RateLimitKeyType.IP_ADDRESS)
public Page<ProductResponse> getProducts() { }

@PostMapping("/orders")
@RateLimited(value = "authenticated", keyType = RateLimitKeyType.USER)
public OrderResponse createOrder() { }

@GetMapping("/dashboard")
@RateLimited(value = "analytics", keyType = RateLimitKeyType.USER)
public Dashboard getDashboard() { }
```

---

## Exception Handling

### Custom Exceptions
```java
throw new RateLimitExceededException("Too many requests", "analytics", userId);
throw new ResourceNotFoundException("Product", productId);
throw new ValidationException("Invalid input", fieldErrors);
```

### All exceptions are automatically handled and return:
```json
{
  "timestamp": "2026-01-01T10:15:30.123Z",
  "status": 429,
  "error": "Too Many Requests",
  "message": "Rate limit exceeded",
  "path": "/api/v1/products",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "errorCode": "RATE_LIMIT_EXCEEDED"
}
```

---

## Secure File Upload

### Validation
```java
@Autowired
private SecureFileUploadService fileUploadService;

public void uploadImage(MultipartFile file) {
    // Validates: size, type, content, dimensions, path traversal
    fileUploadService.validateImageFile(file);
    
    // Generate safe filename
    String safeName = fileUploadService.generateSafeFilename(
        file.getOriginalFilename()
    );
}
```

### Configuration
```properties
app.upload.max-file-size=5242880           # 5MB
app.upload.max-image-width=4096
app.upload.max-image-height=4096
app.upload.allowed-extensions=jpg,jpeg,png,webp
```

---

## Distributed Scheduling

### Usage
```java
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

@Scheduled(cron = "0 0 2 * * *")  // Daily at 2 AM
@SchedulerLock(
    name = "cleanupExpiredCarts",
    lockAtLeastFor = "PT5M",      // 5 minutes
    lockAtMostFor = "PT1H"         // 1 hour
)
public void cleanupExpiredCarts() {
    // Only one instance in cluster will run this
}
```

---

## Correlation IDs & Logging

### Automatic Injection
Every request automatically gets:
- `X-Correlation-Id` - Tracks request across services
- `X-Request-Id` - Unique per request

### Usage in Code
```java
import org.slf4j.MDC;

String correlationId = MDC.get("correlationId");
log.info("Processing order [correlationId={}]", correlationId);
```

### Client Usage
```bash
# Send correlation ID
curl -H "X-Correlation-Id: my-trace-123" http://localhost:8082/api/v1/products

# Response includes same ID
# X-Correlation-Id: my-trace-123
```

---

## Caching Best Practices

### L1 (Caffeine) + L2 (Redis)
```java
@Cacheable(
    cacheNames = "products",
    key = "#id",
    unless = "#result == null",
    condition = "#id != null"
)
public Product findById(Long id) { }

@CachePut(cacheNames = "products", key = "#result.id")
public Product update(Product product) { }

@CacheEvict(cacheNames = "products", key = "#id")
public void delete(Long id) { }
```

### Cache Names & TTLs
```
products   -> 15 minutes
categories -> 1 hour
dashboard  -> 5 minutes
analytics  -> 2 minutes
sessions   -> 24 hours
```

---

## N+1 Query Prevention

### Use EntityGraph
```java
@EntityGraph("Product.withAllRelations")
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdWithRelations(@Param("id") Long id);
```

### Use DTO Projections
```java
@Query("""
    SELECT new com.eshop.app.dto.ProductDTO(
        p.id, p.name, c.name, b.name
    )
    FROM Product p
    LEFT JOIN p.category c
    LEFT JOIN p.brand b
    WHERE p.active = true
""")
List<ProductDTO> findAllSummaries();
```

### Batch Fetching
```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=25
```

---

## Circuit Breakers

### Available Instances
```
paymentGateway  - Payment processing
emailService    - Email sending
externalApi     - External APIs
```

### Usage
```java
@CircuitBreaker(name = "paymentGateway", fallbackMethod = "paymentFallback")
public PaymentResult processPayment(Order order) {
    return paymentGateway.charge(order);
}

private PaymentResult paymentFallback(Order order, Exception e) {
    log.error("Payment failed, using fallback", e);
    return PaymentResult.failed();
}
```

---

## OpenAPI Documentation

### Document Endpoints
```java
@Operation(
    summary = "Create product",
    description = "Creates a new product in the catalog"
)
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Created"),
    @ApiResponse(responseCode = "400", description = "Invalid input"),
    @ApiResponse(responseCode = "401", description = "Unauthorized")
})
@PostMapping
public ResponseEntity<ProductResponse> create(
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Product data",
        required = true
    )
    @Valid @RequestBody ProductCreateRequest request
) { }
```

### Access Documentation
```
Swagger UI:    http://localhost:8082/swagger-ui.html
OpenAPI JSON:  http://localhost:8082/v3/api-docs
```

---

## Metrics & Monitoring

### Actuator Endpoints
```
/actuator/health       - Health status
/actuator/metrics      - All metrics
/actuator/prometheus   - Prometheus format
/actuator/caches       - Cache statistics
```

### Custom Metrics
```java
@Autowired
private MeterRegistry meterRegistry;

Counter.builder("products.created")
    .description("Number of products created")
    .tags("shop", shopId)
    .register(meterRegistry)
    .increment();
```

---

## Security Best Practices

### Method Security
```java
@PreAuthorize("hasRole('ADMIN')")
public void deleteProduct(Long id) { }

@PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
public Product createProduct(Product product) { }

@PreAuthorize("@productSecurity.canEdit(#productId, principal)")
public Product update(Long productId, Product product) { }
```

### Input Validation
```java
public record CreateRequest(
    @NotBlank @Size(max = 255)
    String name,
    
    @NotNull @Positive
    BigDecimal price,
    
    @Email
    String email,
    
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
    String slug
) {}
```

---

## Performance Tips

### Pagination
```java
// Always use pagination for large datasets
Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
Page<Product> products = productRepository.findAll(pageable);
```

### Async Processing
```java
@Async
public CompletableFuture<NotificationResult> sendNotification(String email) {
    // Offload to virtual thread pool
    return CompletableFuture.completedFuture(result);
}
```

### Optimistic Locking
```java
@Entity
public class Product {
    @Version
    private Long version;  // Prevents concurrent modification
}
```

---

## Troubleshooting

### Rate Limit Errors
```
Error: 429 Too Many Requests
Solution: Check X-RateLimit-Retry-After header, wait and retry
```

### Cache Issues
```bash
# Clear specific cache
curl -X DELETE http://localhost:8082/actuator/caches/products

# View cache stats
curl http://localhost:8082/actuator/caches
```

### Trace Slow Requests
```
1. Get correlation ID from response header
2. Search logs: grep "550e8400-..." application.log
3. View trace: http://localhost:9411 (Zipkin)
```

---

## Environment Variables

### Essential
```bash
export DB_USERNAME=eshop
export DB_PASSWORD=secret
export REDIS_HOST=localhost
export KEYCLOAK_ISSUER_URI=http://localhost:8080/realms/eshop
```

### Optional
```bash
export RATE_LIMIT_ENABLED=true
export CACHE_ENABLED=true
export TRACING_ENABLED=true
export LOG_LEVEL=DEBUG
```

---

## Quick Commands

### Build
```bash
./gradlew clean build
```

### Run
```bash
./gradlew bootRun
```

### Docker
```bash
docker-compose up -d
```

### Health Check
```bash
curl http://localhost:8082/actuator/health
```

---

## Support

- **Logs:** Check correlation ID in error response
- **Metrics:** http://localhost:8082/actuator/prometheus
- **Tracing:** http://localhost:9411 (Zipkin)
- **API Docs:** http://localhost:8082/swagger-ui.html
