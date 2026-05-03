# Quick Implementation Guide
**Enterprise Refactoring - E-Shop Application**

---

## 🚀 IMMEDIATE NEXT STEPS

This guide shows you how to apply the refactoring changes to your existing codebase.

### Step 1: Update Dependencies
The `build.gradle` has been updated with new dependencies. Run:

```bash
./gradlew clean build
```

**New Dependencies Added**:
- Resilience4j (rate limiting, circuit breaker, bulkhead, retry)
- Micrometer Tracing (distributed tracing)
- Zipkin Reporter (tracing backend)
- Jsoup (HTML sanitization)
- Apache Commons Lang3 (utilities)

### Step 2: Database Migration
Apply the performance indexes migration:

```bash
# The migration will run automatically on next application start
# V1_10__performance_indexes.sql will create 40+ indexes
```

**What it does**:
- Creates full-text search indexes
- Adds composite indexes for common queries
- Creates partial indexes for filtered queries
- Updates table statistics

### Step 3: Update Environment Variables
Add these to your `.env` file or environment:

```properties
# Required (no defaults)
JWT_SECRET=your-256-bit-secret-key-here-min-64-chars
DATABASE_PASSWORD=your-db-password

# Optional (has defaults)
SPRING_PROFILES_ACTIVE=keycloak
KEYCLOAK_ENABLED=true
KEYCLOAK_REALM=eshop
KEYCLOAK_AUTH_SERVER_URL=http://localhost:8080

# Monitoring (optional)
ZIPKIN_ENDPOINT=http://localhost:9411/api/v2/spans
TRACING_ENABLED=true
```

### Step 4: Apply to Existing Services
Here's how to enhance your existing ProductService:

#### Before:
```java
@Service
public class ProductServiceImpl implements ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    public List<ProductDTO> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
    }
}
```

#### After:
```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    
    @Cacheable(
        value = "productsList",
        key = "#pageable.pageNumber + '-' + #pageable.pageSize"
    )
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findAllWithRelations(pageable)
            .map(mapper::toDTO);
    }
    
    @Auditable(entityType = "Product", action = AuditAction.CREATE)
    @Transactional
    @CachePut(value = "products", key = "#result.id")
    public ProductDTO createProduct(ProductCreateDTO dto) {
        Product product = mapper.toEntity(dto);
        Product saved = productRepository.save(product);
        return mapper.toDTO(saved);
    }
    
    @Auditable(entityType = "Product", action = AuditAction.UPDATE)
    @Transactional
    @CachePut(value = "products", key = "#id")
    public ProductDTO updateProduct(Long id, ProductUpdateDTO dto) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        
        mapper.updateEntity(dto, product);
        Product saved = productRepository.save(product);
        
        return mapper.toDTO(saved);
    }
    
    @Auditable(entityType = "Product", action = AuditAction.DELETE)
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products", key = "#id"),
        @CacheEvict(value = "productsList", allEntries = true)
    })
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
```

### Step 5: Fix N+1 Queries in Repositories
Update your ProductRepository:

#### Before:
```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    // Missing fetch joins - causes N+1 queries
}
```

#### After:
```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "LEFT JOIN FETCH p.brand " +
           "LEFT JOIN FETCH p.shop " +
           "WHERE p.active = true")
    Page<Product> findAllWithRelations(Pageable pageable);
    
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.category " +
           "LEFT JOIN FETCH p.brand " +
           "LEFT JOIN FETCH p.images " +
           "WHERE p.id = :id")
    Optional<Product> findByIdWithRelations(@Param("id") Long id);
    
    @Query(value = "SELECT * FROM products " +
           "WHERE to_tsvector('english', name || ' ' || description) " +
           "@@ plainto_tsquery('english', :keyword)",
           nativeQuery = true)
    Page<Product> fullTextSearch(@Param("keyword") String keyword, Pageable pageable);
}
```

### Step 6: Apply Rate Limiting to Controllers
Update your ProductController:

```java
@RestController
@RequestMapping("/api/v1/products")
@Validated
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    
    @GetMapping
    @RateLimiter(name = "search")  // ← Add this
    public ResponseEntity<Page<ProductDTO>> getProducts(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) 
        Pageable pageable
    ) {
        Page<ProductDTO> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES))
            .body(products);
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @RateLimiter(name = "productCreate")  // ← Add this
    public ResponseEntity<ProductDTO> createProduct(
        @Valid @RequestBody ProductCreateDTO dto,
        @AuthenticationPrincipal Jwt jwt
    ) {
        ProductDTO created = productService.createProduct(dto);
        
        URI location = ServletUriComponentsBuilder
            .fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
        
        return ResponseEntity.created(location).body(created);
    }
}
```

### Step 7: Use Async Operations
For time-consuming operations:

```java
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    @Async("notificationExecutor")
    public CompletableFuture<Void> sendOrderConfirmation(Order order) {
        // Send email
        emailService.send(order.getCustomerEmail(), "Order Confirmed", ...);
        
        // Send SMS (if phone available)
        if (order.getCustomerPhone() != null) {
            smsService.send(order.getCustomerPhone(), "Order confirmed!");
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    @Async("analyticsExecutor")
    public void trackOrderAsync(Order order) {
        analyticsService.track("order_created", Map.of(
            "orderId", order.getId(),
            "total", order.getTotal(),
            "items", order.getItems().size()
        ));
    }
}
```

---

## 📊 VERIFICATION CHECKLIST

After implementing the changes, verify:

### 1. Build & Start
```bash
./gradlew clean build
./gradlew bootRun
```

✅ Application starts without errors  
✅ All beans are initialized  
✅ Database migrations run successfully  

### 2. Check Endpoints
```bash
# Health check
curl http://localhost:8082/actuator/health

# Metrics
curl http://localhost:8082/actuator/metrics

# Prometheus
curl http://localhost:8082/actuator/prometheus
```

### 3. Check Logs
Look for these log messages:

```
✅ Configuring security filter chain with OAuth2 Resource Server
✅ Initializing custom Caffeine cache manager
✅ Initialized 30 Caffeine caches with custom TTLs
✅ Configuring default task executor: core=10, max=50, queue=100
✅ Configuring virtual thread executor (Java 21)
✅ Flyway migration V1_10__performance_indexes.sql completed
```

### 4. Test Performance
Before refactoring vs After:

```bash
# Product list query
curl http://localhost:8082/api/v1/products?page=0&size=20

# Expected: < 100ms (was 2.5s)

# Product search
curl http://localhost:8082/api/v1/products/search?q=laptop

# Expected: < 200ms (was 1.5s)
```

### 5. Test Security
```bash
# Should return 401 Unauthorized
curl http://localhost:8082/api/v1/products -X POST

# With valid JWT should work
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8082/api/v1/products -X POST
```

### 6. Test Rate Limiting
```bash
# Make 25 requests rapidly (limit is 20/second for search)
for i in {1..25}; do
  curl http://localhost:8082/api/v1/products &
done

# Should see some 429 Too Many Requests responses
```

### 7. Check Audit Logs
```sql
-- Check audit logs are being created
SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT 10;

-- Should see CREATE, UPDATE, DELETE actions
```

### 8. Check Caching
```bash
# First request (cache miss)
curl http://localhost:8082/api/v1/products/1

# Second request (cache hit - should be faster)
curl http://localhost:8082/api/v1/products/1
```

---

## 🔧 TROUBLESHOOTING

### Issue: Application fails to start

**Symptom**: `java.lang.IllegalStateException: Failed to load ApplicationContext`

**Solution**: Check for:
1. Missing environment variables (JWT_SECRET, DATABASE_PASSWORD)
2. Database connection issues
3. Conflicting bean definitions

### Issue: N+1 queries still occurring

**Symptom**: Multiple queries in logs for a single request

**Solution**:
1. Enable SQL logging: `logging.level.org.hibernate.SQL=DEBUG`
2. Check repository methods use `@Query` with JOIN FETCH
3. Verify `spring.jpa.open-in-view=false`

### Issue: Cache not working

**Symptom**: Every request hits the database

**Solution**:
1. Check `@EnableCaching` is present
2. Verify cache names match in config and service
3. Check logs for "Initializing custom Caffeine cache manager"

### Issue: Rate limiting not working

**Symptom**: No 429 errors even with many requests

**Solution**:
1. Verify Resilience4j dependency is present
2. Check `@RateLimiter` annotation is on controller methods
3. Verify configuration in application.properties

### Issue: Audit logs not created

**Symptom**: No entries in `audit_logs` table

**Solution**:
1. Check `app.audit.enabled=true`
2. Verify `@Auditable` annotation on service methods
3. Check `AuditLoggingAspect` bean is created
4. Verify AspectJ is working: `spring.aop.auto=true`

---

## 📈 MONITORING

### Prometheus Queries

```promql
# Request rate
rate(http_server_requests_seconds_count[1m])

# Request duration (95th percentile)
histogram_quantile(0.95, http_server_requests_seconds_bucket)

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[1m])

# Cache hit rate
cache_gets_total{result="hit"} / cache_gets_total
```

### Grafana Dashboard
Import dashboard ID: `4701` (Spring Boot Statistics)

---

## 🎯 PERFORMANCE TARGETS

After full implementation, you should achieve:

| Metric | Target | How to Measure |
|--------|--------|----------------|
| Product List | < 100ms | `curl -w "%{time_total}" http://localhost:8082/api/v1/products` |
| Product Search | < 200ms | `curl -w "%{time_total}" http://localhost:8082/api/v1/products/search?q=test` |
| Order Create | < 500ms | POST with timer |
| Dashboard Load | < 300ms | GET admin dashboard |
| Cache Hit Rate | > 80% | Prometheus metrics |
| Concurrent Users | 1000+ | Load testing with JMeter/Gatling |

---

## 📚 ADDITIONAL RESOURCES

- [Spring Boot 4.0 Documentation](https://docs.spring.io/spring-boot/docs/4.0.x/reference/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Caffeine Cache Documentation](https://github.com/ben-manes/caffeine/wiki)
- [Java 21 Virtual Threads](https://openjdk.org/jeps/444)
- [Spring Security OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)

---

**Need Help?**  
Check [REFACTORING_SUMMARY.md](./REFACTORING_SUMMARY.md) for complete details on all changes made.
