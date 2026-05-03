# E-Shop Backend Documentation

## Table of Contents

### Getting Started
- [Main README](../README.md) - Project overview and quick start
- [Tech Stack](../TECH_STACK.md) - Technology stack and architecture

### Setup Guides
- [Keycloak Setup](./setup/KEYCLOAK_SETUP.md) - Complete Keycloak installation and configuration
- [Database Access](./database/DATABASE_ACCESS.md) - Database setup and access guide

### User Guides
- [Role Management Guide](./guides/ROLE_MANAGEMENT.md) - Complete guide to role-based access control
- [Keycloak Role Configuration](./KEYCLOAK_ROLE_CONFIGURATION.md) - Keycloak role setup and troubleshooting

### Deployment
- [Deployment Guide](./deployment/DEPLOYMENT_GUIDE.md) - Step-by-step deployment procedures
- [Implementation Changes](./CHANGES_CUSTOMER_ROLE_FIX.md) - Recent changes and updates

### Operations
- [Database Operations SOP](./database/DATABASE_OPERATIONS_SOP.md) - Standard operating procedures for database
- [Product Image Upload API](./PRODUCT_IMAGE_UPLOAD_API.md) - Image upload documentation

### Code Quality & Performance
- [Performance Optimization Guide](./refactoring/PERFORMANCE_OPTIMIZATION_GUIDE.md) - N+1 query fixes for Cart, Order & Product modules
- [Code Reusability Guide](./refactoring/CODE_REUSABILITY_GUIDE.md) - DRY principle enforcement in ProductServiceImpl
- [Product Controller Refactoring](./PRODUCT_CONTROLLER_REFACTORING.md) - Product creation & update refactoring history
- [Before/After Comparison](./BEFORE_AFTER_COMPARISON.md) - Detailed before/after code comparisons

---

## Quick Navigation

### For Developers

**Just starting?**
1. Read [Main README](../README.md)
2. Follow [Keycloak Setup](./setup/KEYCLOAK_SETUP.md)
3. Review [Role Management Guide](./guides/ROLE_MANAGEMENT.md)

**Need to understand roles?**
- [Role Management Guide](./guides/ROLE_MANAGEMENT.md) - Complete role system documentation

**Deploying changes?**
- [Deployment Guide](./deployment/DEPLOYMENT_GUIDE.md) - Production deployment procedures

### For System Administrators

**Setting up Keycloak?**
- [Keycloak Setup](./setup/KEYCLOAK_SETUP.md) - Installation and configuration
- [Keycloak Role Configuration](./KEYCLOAK_ROLE_CONFIGURATION.md) - Role setup

**Managing users?**
- [Role Management Guide](./guides/ROLE_MANAGEMENT.md) - User role assignment procedures

**Troubleshooting?**
- Check troubleshooting sections in relevant guides

### For DevOps Engineers

**Deploying to production?**
- [Deployment Guide](./deployment/DEPLOYMENT_GUIDE.md) - Complete deployment procedures

**Database operations?**
- [Database Operations SOP](./database/DATABASE_OPERATIONS_SOP.md) - Standard procedures

---

## Recent Updates

### 2026-02-17: Customer Role Auto-Assignment

**What Changed:**
- ✅ CUSTOMER role now auto-assigned during registration
- ✅ Seller applications require admin approval
- ✅ Backend safety net for role assignment

**Documentation:**
- [Implementation Changes](./CHANGES_CUSTOMER_ROLE_FIX.md) - Detailed changes
- [Deployment Guide](./deployment/DEPLOYMENT_GUIDE.md) - How to deploy
- [Role Management Guide](./guides/ROLE_MANAGEMENT.md) - Updated workflows

### 2026-02-22: Codebase Performance Optimization

**What Changed:**
- ✅ Fixed N+1 read queries in `CartRepository` using `@EntityGraph`
- ✅ Fixed N+1 read queries in `OrderRepository` using `@EntityGraph`
- ✅ Fixed N+1 write operations in `OrderServiceImpl` using `saveAll()` batch
- ✅ Eliminated 4 code duplication violations in `ProductServiceImpl` (DRY principle)
- ✅ Removed unsafe infinite-loop in `ensureUniqueFriendlyUrl()` — replaced with circuit-breaker version

**Documentation:**
- [Performance Optimization Guide](./refactoring/PERFORMANCE_OPTIMIZATION_GUIDE.md) - Full details of all N+1 fixes
- [Code Reusability Guide](./refactoring/CODE_REUSABILITY_GUIDE.md) - Details of DRY improvements

---

## Architecture Overview

### Authentication Flow

```
┌─────────────┐         ┌──────────────┐         ┌─────────────────┐
│   Frontend  │ ◄────► │   Keycloak   │ ◄────► │  Backend (API)  │
│  (Next.js)  │  OAuth2 │              │  JWT   │   (Spring Boot) │
└─────────────┘         └──────────────┘         └─────────────────┘
                               │                          │
                               │                          │
                               ▼                          ▼
                        ┌──────────────┐         ┌──────────────┐
                        │  PostgreSQL  │ ◄─────► │  PostgreSQL  │
                        │  (Keycloak)  │         │   (E-Shop)   │
                        └──────────────┘         └──────────────┘
```

### Role Hierarchy

```
CUSTOMER (Auto-assigned)
    │
    ├─► SELLER (Admin approval required)
    │
    ├─► DELIVERY_AGENT (Admin approval required)
    │
    └─► ADMIN (Manual assignment)
```

---

## Common Tasks

### Register a New User
1. User registers via Keycloak UI
2. CUSTOMER role auto-assigned (via Keycloak default roles)
3. User can login and access customer endpoints

See: [Role Management Guide - Customer Registration](./guides/ROLE_MANAGEMENT.md#journey-1-new-customer-registration)

### Approve a Seller Application
1. Admin reviews pending applications: `GET /api/v1/admin/approvals/sellers`
2. Admin approves: `POST /api/v1/admin/approvals/sellers/{id}/APPROVE`
3. SELLER role assigned via KeycloakService
4. User can access seller endpoints

See: [Role Management Guide - Seller Approval](./guides/ROLE_MANAGEMENT.md#journey-2-customer-becomes-seller)

### Troubleshoot Role Assignment
1. Check Keycloak default roles: Realm Settings → User Registration → Default Roles
2. Verify user roles: Keycloak Admin Console → Users → {user} → Role Mappings
3. Check backend logs: `grep "Assigning.*role" logs/eshop-dev.log`

See: [Keycloak Role Configuration - Troubleshooting](./KEYCLOAK_ROLE_CONFIGURATION.md#troubleshooting)

---

## API Documentation

### Authentication Endpoints

| Endpoint | Method | Description | Auth Required |
|----------|--------|-------------|---------------|
| `/api/v1/auth/login` | POST | Login with username/password | No |
| `/api/v1/auth/refresh` | POST | Refresh access token | No |
| `/api/v1/auth/logout` | POST | Logout user | Yes |
| `/api/v1/auth/userinfo` | GET | Get user info | Yes |
| `/api/v1/me` | GET | Get current user | Yes |

### Customer Endpoints

| Endpoint | Method | Description | Required Role |
|----------|--------|-------------|---------------|
| `/api/v1/products` | GET | List products | CUSTOMER |
| `/api/v1/cart` | POST | Add to cart | CUSTOMER |
| `/api/v1/orders` | POST | Place order | CUSTOMER |
| `/api/v1/orders` | GET | View orders | CUSTOMER |

### Seller Endpoints

| Endpoint | Method | Description | Required Role |
|----------|--------|-------------|---------------|
| `/api/v1/sellers/register` | POST | Apply to become seller | CUSTOMER |
| `/api/v1/sellers/profile` | GET | Get seller profile | SELLER |
| `/api/v1/sellers/profile` | PUT | Update seller profile | SELLER |
| `/api/v1/products` | POST | Create product | SELLER |
| `/api/v1/dashboard/seller` | GET | Seller dashboard | SELLER |

### Admin Endpoints

| Endpoint | Method | Description | Required Role |
|----------|--------|-------------|---------------|
| `/api/v1/admin/approvals/sellers` | GET | List pending sellers | ADMIN |
| `/api/v1/admin/approvals/sellers/{id}/APPROVE` | POST | Approve seller | ADMIN |
| `/api/v1/admin/approvals/sellers/{id}/REJECT` | POST | Reject seller | ADMIN |
| `/api/v1/admin/approvals/delivery-agents` | GET | List pending agents | ADMIN |

Full API documentation: [Swagger UI](http://localhost:8082/swagger-ui.html)

---

## Configuration Reference

### Environment Variables

```bash
# Keycloak
KEYCLOAK_URL=http://localhost:8080
KEYCLOAK_REALM=eshop
KEYCLOAK_CLIENT_ID=eshop-backend
KEYCLOAK_CLIENT_SECRET=your-secret-here

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/eshop
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# Application
SERVER_PORT=8082
SPRING_PROFILES_ACTIVE=dev
```

### Configuration Files

| File | Purpose |
|------|---------|
| `application.properties` | Base configuration |
| `application-dev.properties` | Development overrides |
| `application-staging.properties` | Staging overrides |
| `application-production.properties` | Production overrides |
| `realm-export.json` | Keycloak realm configuration |
| `docker-compose.yml` | Docker services configuration |

---

## Testing

### Run Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests SellerServiceTest

# Integration tests
./gradlew integrationTest

# With coverage
./gradlew test jacocoTestReport
```

### Manual Testing

See testing sections in:
- [Role Management Guide - Testing](./guides/ROLE_MANAGEMENT.md#testing)
- [Keycloak Setup - Testing](./setup/KEYCLOAK_SETUP.md#testing)

---

## Monitoring

### Health Checks

```bash
# Application health
curl http://localhost:8082/actuator/health

# Keycloak health
curl http://localhost:8080/health

# Database health
docker exec postgres pg_isready
```

### Metrics

```bash
# Application metrics
curl http://localhost:8082/actuator/metrics

# Prometheus metrics
curl http://localhost:8082/actuator/prometheus
```

### Logs

```bash
# View application logs
docker logs -f eshop-backend

# View Keycloak logs
docker logs -f keycloak

# View database logs
docker logs -f postgres

# Application log files
tail -f logs/eshop-dev.log
```

---

## Security

### Best Practices

1. ✅ **Never commit secrets** - Use environment variables
2. ✅ **Use HTTPS in production** - Configure SSL/TLS
3. ✅ **Rotate secrets regularly** - Client secrets, passwords
4. ✅ **Enable email verification** - For production
5. ✅ **Monitor failed login attempts** - Set up alerts
6. ✅ **Keep Keycloak updated** - Security patches
7. ✅ **Use strong passwords** - Enforce password policies
8. ✅ **Backup regularly** - Database and Keycloak realm

### Security Headers

The application includes security headers:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security` (in production)

---

## Troubleshooting

### Common Issues

1. **Keycloak not accessible**
   - Check if container is running: `docker ps | grep keycloak`
   - Check logs: `docker logs keycloak`
   - Verify port 8080 is not in use

2. **User doesn't have CUSTOMER role**
   - Check Keycloak default roles configuration
   - Verify realm-export.json is imported
   - Check backend logs for JIT role assignment

3. **Seller auto-approved (should be pending)**
   - Verify latest code is deployed
   - Check `SellerService.registerSeller()` method
   - Should NOT have auto-approval code

4. **JWT doesn't include roles**
   - Check Keycloak client scope mappers
   - Verify "realm roles" mapper exists
   - Check token claim name: `realm_access.roles`

See detailed troubleshooting:
- [Keycloak Role Configuration - Troubleshooting](./KEYCLOAK_ROLE_CONFIGURATION.md#troubleshooting)
- [Role Management Guide - Troubleshooting](./guides/ROLE_MANAGEMENT.md#troubleshooting)

---

## Contributing

### Code Style

- Follow Java code conventions
- Use meaningful variable names
- Add JavaDoc comments for public methods
- Write unit tests for new features

### Documentation

- Update README when adding features
- Document API changes in Swagger annotations
- Update relevant guides when changing workflows

### Pull Request Process

1. Create feature branch from `main`
2. Make changes and commit
3. Write/update tests
4. Update documentation
5. Create pull request
6. Wait for review and approval
7. Merge to `main`

---

## Support

### Getting Help

**For Developers:**
- Check relevant documentation guides
- Review troubleshooting sections
- Check application logs

**For Issues:**
- Create issue in project repository
- Include error logs and steps to reproduce
- Tag with appropriate labels

**For Urgent Production Issues:**
- Contact on-call engineer via PagerDuty
- Use emergency contact: [Contact Info]
- Post in #incidents Slack channel

---

## Related Resources

### External Documentation
- [Keycloak Documentation](https://www.keycloak.org/documentation)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security OAuth2](https://spring.io/projects/spring-security-oauth)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

### Internal Resources
- [Project Wiki](#) - Team wiki and knowledge base
- [Confluence Page](#) - Architecture decisions
- [Jira Board](#) - Project tracking

---

## License

[Your License Here]

---

## Changelog

### 2026-02-22 - v1.3.0
- ✅ N+1 query fix: CartRepository — single JOIN FETCH for items + products
- ✅ N+1 query fix: OrderRepository — single JOIN FETCH for items across 6 methods
- ✅ N+1 write fix: OrderServiceImpl — batch `saveAll()` instead of per-item saves
- ✅ DRY refactor: ProductServiceImpl — 4 code duplication violations resolved
- ✅ Unsafe infinite loop removed from `ensureUniqueFriendlyUrl()`
- 📚 New docs: `docs/refactoring/PERFORMANCE_OPTIMIZATION_GUIDE.md`
- 📚 New docs: `docs/refactoring/CODE_REUSABILITY_GUIDE.md`

### 2026-02-17 - v1.2.0
- ✅ Customer role auto-assignment feature
- ✅ Seller approval workflow enforcement
- ✅ JIT role assignment safety net
- 📚 Comprehensive documentation updates

### Previous Versions
See [CHANGELOG.md](../CHANGELOG.md) for full history

---

**Last Updated:** 2026-02-22  
**Maintained By:** Development Team  
**Version:** 1.3.0
