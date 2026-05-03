# Deployment Guide - Customer Role Auto-Assignment

## Table of Contents
1. [Overview](#overview)
2. [Pre-Deployment Checklist](#pre-deployment-checklist)
3. [Development Environment](#development-environment)
4. [Staging Environment](#staging-environment)
5. [Production Environment](#production-environment)
6. [Rollback Procedures](#rollback-procedures)
7. [Post-Deployment Verification](#post-deployment-verification)
8. [Monitoring and Alerts](#monitoring-and-alerts)

---

## Overview

This guide covers the deployment of the Customer Role Auto-Assignment feature, which includes:

✅ **Keycloak Configuration**: Auto-assign CUSTOMER role to new users  
✅ **Backend Changes**: Remove seller auto-approval, add JIT role assignment safety net  
✅ **Admin Approval Flow**: Sellers require admin approval before getting SELLER role

### Impact Assessment

| Component | Change Type | Risk Level | Downtime Required |
|-----------|-------------|------------|-------------------|
| Keycloak | Configuration | Low | No |
| Backend | Code Change | Low | Yes (~5 min) |
| Frontend | No Change | None | No |
| Database | No Change | None | No |

---

## Pre-Deployment Checklist

### Code Review
- [ ] All changes reviewed and approved
- [ ] Unit tests passing
- [ ] Integration tests passing
- [ ] No security vulnerabilities introduced

### Documentation
- [ ] README updated
- [ ] API documentation updated
- [ ] Changelog updated
- [ ] Deployment guide reviewed

### Backups
- [ ] Database backup completed
- [ ] Keycloak realm exported
- [ ] Configuration files backed up
- [ ] Docker images tagged

### Testing
- [ ] Tested in local environment
- [ ] Tested in staging environment
- [ ] Load testing completed (if applicable)
- [ ] Security testing completed

### Communication
- [ ] Stakeholders notified of deployment
- [ ] Maintenance window scheduled (if needed)
- [ ] Support team briefed
- [ ] Rollback plan prepared

---

## Development Environment

### Step 1: Backup Current State

```bash
# Navigate to project
cd G:\Project\eshop_back

# Backup Keycloak realm
docker exec -it keycloak /opt/keycloak/bin/kc.sh export \
  --realm eshop \
  --file /tmp/realm-backup-$(date +%Y%m%d-%H%M%S).json

docker cp keycloak:/tmp/realm-backup-*.json ./backups/

# Backup database
docker exec postgres pg_dump -U postgres eshop > backups/db-backup-$(date +%Y%m%d-%H%M%S).sql

# Commit current code state
git add .
git commit -m "Pre-deployment backup - Customer role feature"
git tag pre-customer-role-deployment-$(date +%Y%m%d-%H%M%S)
```

### Step 2: Pull Latest Changes

```bash
# Pull changes
git pull origin main

# Or if working on a branch
git checkout feature/customer-role-auto-assignment
git pull origin feature/customer-role-auto-assignment
```

### Step 3: Update Keycloak Configuration

**Option A: Re-import Realm (Recommended for Dev)**

```bash
# Stop Keycloak
docker-compose stop keycloak

# Re-import realm with new configuration
docker-compose up -d keycloak

# Keycloak will auto-import realm-export.json on startup
# Check logs to verify
docker logs -f keycloak | grep "import"
```

**Option B: Manual Configuration**

```bash
# Run configuration script
cd scripts
./configure-keycloak-roles.sh

# Or for Windows
configure-keycloak-roles.bat
```

**Option C: Via Admin Console**

1. Login to http://localhost:8080
2. Select realm: `eshop`
3. Go to: Realm Settings → User Registration → Default Roles
4. Add: `Customer`
5. Save

### Step 4: Build and Deploy Backend

```bash
# Build application
./gradlew clean build

# Run tests
./gradlew test

# Restart backend
docker-compose restart eshop-backend

# Or rebuild if needed
docker-compose up -d --build eshop-backend
```

### Step 5: Verify Deployment

```bash
# Check backend logs
docker logs -f eshop-backend | grep "Started EshopApplication"

# Test health endpoint
curl http://localhost:8082/actuator/health

# Test authentication
curl http://localhost:8082/api/v1/me \
  -H "Authorization: Bearer {test-token}"
```

### Step 6: Test New Functionality

See [Post-Deployment Verification](#post-deployment-verification) section.

---

## Staging Environment

### Prerequisites

- [ ] Development testing completed successfully
- [ ] Staging database restored from production backup
- [ ] Staging environment matches production configuration
- [ ] All stakeholders notified

### Step 1: Pre-Deployment Tasks

```bash
# SSH to staging server
ssh user@staging-server

# Navigate to application directory
cd /opt/eshop

# Backup current state
./scripts/backup-staging.sh

# Pull latest code
git fetch origin
git checkout main
git pull origin main
```

### Step 2: Update Configuration Files

```bash
# Update application.properties (if needed)
nano src/main/resources/application-staging.properties

# Verify Keycloak configuration
grep keycloak src/main/resources/application-staging.properties
```

### Step 3: Deploy Keycloak Configuration

```bash
# Export current realm as backup
docker exec keycloak /opt/keycloak/bin/kc.sh export \
  --realm eshop \
  --file /tmp/realm-backup-staging-$(date +%Y%m%d-%H%M%S).json

# Import new realm configuration
docker exec -it keycloak /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/realm-export.json \
  --override true

# OR use configuration script
./scripts/configure-keycloak-roles.sh
```

### Step 4: Build and Deploy Backend

```bash
# Build application
./gradlew clean build -Pprofile=staging

# Stop application
docker-compose -f docker-compose-staging.yml stop eshop-backend

# Deploy new version
docker-compose -f docker-compose-staging.yml up -d --build eshop-backend

# Monitor startup
docker logs -f eshop-backend
```

### Step 5: Smoke Tests

```bash
# Run automated smoke tests
./scripts/smoke-test-staging.sh

# Or manual tests
curl https://staging.eshop.com/actuator/health
curl https://staging.eshop.com/api/v1/me -H "Authorization: Bearer $TOKEN"
```

### Step 6: Verify with Test Users

1. Register new test user
2. Verify CUSTOMER role assigned
3. Apply as seller
4. Verify status is PENDING
5. Login as admin and approve
6. Verify SELLER role assigned

---

## Production Environment

### Prerequisites

- [ ] Staging deployment successful
- [ ] All tests passing in staging
- [ ] Change approval obtained
- [ ] Maintenance window scheduled
- [ ] Rollback plan ready
- [ ] Monitoring alerts configured

### Deployment Timeline

```
T-1 hour:  Final checks, team briefing
T-30 min:  Begin maintenance window notification
T-15 min:  Start deployment preparation
T-10 min:  Database and Keycloak backups
T-5 min:   Stop application (if needed)
T-0:       Deploy changes
T+5 min:   Start application
T+10 min:  Smoke tests
T+15 min:  Monitor metrics
T+30 min:  End maintenance window
```

### Step 1: Pre-Deployment

```bash
# SSH to production server
ssh user@production-server

# Navigate to application directory
cd /opt/eshop

# Create deployment directory
DEPLOY_DIR=/tmp/deploy-$(date +%Y%m%d-%H%M%S)
mkdir -p $DEPLOY_DIR

# Backup everything
./scripts/backup-production.sh $DEPLOY_DIR

# Verify backups
ls -lh $DEPLOY_DIR
```

### Step 2: Notify Users

```bash
# Enable maintenance mode (if available)
./scripts/enable-maintenance-mode.sh

# Send notification to users
# (via email, in-app notification, status page, etc.)
```

### Step 3: Deploy Keycloak Configuration

```bash
# Export current realm
docker exec keycloak /opt/keycloak/bin/kc.sh export \
  --realm eshop \
  --file /tmp/realm-backup-prod-$(date +%Y%m%d-%H%M%S).json

# Copy to backup location
docker cp keycloak:/tmp/realm-backup-prod-*.json $DEPLOY_DIR/

# Import new configuration
docker exec -it keycloak /opt/keycloak/bin/kc.sh import \
  --file /opt/keycloak/data/import/realm-export.json \
  --override true

# Verify
docker exec keycloak curl -s http://localhost:8080/realms/eshop/.well-known/openid-configuration | jq
```

### Step 4: Deploy Backend Application

```bash
# Pull latest code
git fetch origin
git checkout v1.2.0  # Use specific version tag
git pull origin v1.2.0

# Build application
./gradlew clean build -Pprofile=production

# Run production tests
./gradlew test -Pprofile=production

# Stop application gracefully
docker-compose -f docker-compose-production.yml stop eshop-backend

# Wait for graceful shutdown
sleep 10

# Deploy new version
docker-compose -f docker-compose-production.yml up -d --build eshop-backend

# Monitor startup
docker logs -f eshop-backend | grep "Started"
```

### Step 5: Post-Deployment Verification

```bash
# Wait for application to start
sleep 30

# Health check
curl https://eshop.com/actuator/health

# Test authentication
./scripts/test-authentication.sh

# Test role assignment
./scripts/test-role-assignment.sh

# Check logs for errors
docker logs eshop-backend --tail 100 | grep -i error
```

### Step 6: Monitor Metrics

```bash
# Watch application metrics
watch -n 5 'curl -s https://eshop.com/actuator/metrics | jq'

# Monitor error rate
# (via Grafana, Prometheus, CloudWatch, etc.)

# Check user registrations
# Monitor first 10 new registrations for CUSTOMER role
```

### Step 7: Disable Maintenance Mode

```bash
# Disable maintenance mode
./scripts/disable-maintenance-mode.sh

# Send "all clear" notification
./scripts/notify-deployment-complete.sh
```

---

## Rollback Procedures

### When to Rollback

Rollback immediately if:
- ❌ Application fails to start
- ❌ Health checks failing
- ❌ Critical errors in logs
- ❌ Users cannot login
- ❌ Roles not being assigned
- ❌ Error rate > 5%

### Rollback Steps

#### Option 1: Quick Rollback (Code Only)

```bash
# SSH to server
ssh user@production-server
cd /opt/eshop

# Checkout previous version
git checkout v1.1.0  # Previous stable version

# Rebuild and restart
./gradlew clean build -Pprofile=production
docker-compose -f docker-compose-production.yml up -d --build eshop-backend

# Verify
curl https://eshop.com/actuator/health
```

#### Option 2: Full Rollback (Code + Keycloak)

```bash
# Restore Keycloak realm
docker exec -it keycloak /opt/keycloak/bin/kc.sh import \
  --file /tmp/realm-backup-prod-20260217-143000.json \
  --override true

# Rollback code
git checkout v1.1.0
./gradlew clean build -Pprofile=production
docker-compose -f docker-compose-production.yml up -d --build eshop-backend

# Verify
./scripts/smoke-test-production.sh
```

#### Option 3: Database Rollback (if needed)

```bash
# Stop application
docker-compose -f docker-compose-production.yml stop eshop-backend

# Restore database
docker exec -i postgres psql -U postgres eshop < $DEPLOY_DIR/db-backup-*.sql

# Restart application
docker-compose -f docker-compose-production.yml up -d eshop-backend
```

### Post-Rollback Tasks

```bash
# Notify stakeholders
./scripts/notify-rollback.sh

# Investigate issue
docker logs eshop-backend > rollback-investigation.log

# Create incident report
# Document what went wrong and why rollback was needed

# Schedule post-mortem
# Review and improve deployment process
```

---

## Post-Deployment Verification

### Automated Tests

```bash
# Run full test suite
./scripts/post-deployment-tests.sh

# Example script content:
#!/bin/bash
set -e

echo "Running post-deployment tests..."

# Test 1: Health check
echo "1. Health check"
curl -f https://eshop.com/actuator/health || exit 1

# Test 2: User registration
echo "2. Test user registration"
./scripts/test-user-registration.sh || exit 1

# Test 3: Role assignment
echo "3. Test role assignment"
./scripts/test-role-assignment.sh || exit 1

# Test 4: Seller application
echo "4. Test seller application"
./scripts/test-seller-application.sh || exit 1

echo "All tests passed!"
```

### Manual Verification Checklist

#### Test 1: Customer Registration and Auto-Role Assignment

1. **Register new user**
   - Go to Keycloak registration page
   - Register with:
     - Username: `test_customer_001`
     - Email: `test_customer_001@eshop.com`
     - Password: `TestPassword123!`

2. **Verify role in Keycloak**
   - Login to Keycloak Admin Console
   - Go to: Users → Search for `test_customer_001`
   - Go to: Role Mappings tab
   - ✅ Verify: "Customer" role is assigned

3. **Test login and access**
   - Login to frontend with new user
   - Call: `GET /api/v1/me`
   - ✅ Verify response includes: `"roles": ["CUSTOMER"]`
   - Navigate to products page
   - ✅ Verify: Products load successfully
   - Add item to cart
   - ✅ Verify: Cart works correctly

#### Test 2: Seller Application Flow (Admin Approval)

1. **Apply to become seller**
   - Login as customer (from Test 1)
   - Navigate to "Become a Seller"
   - Fill seller registration form:
     ```json
     {
       "displayName": "Test Shop 001",
       "businessTypes": ["RETAILER"],
       "email": "test_shop_001@eshop.com",
       "phone": "+1234567890",
       "acceptedTerms": true
     }
     ```
   - Submit application

2. **Verify PENDING status**
   - Call: `GET /api/v1/sellers/profile`
   - ✅ Verify: `"status": "PENDING"`
   - Check Keycloak: User should NOT have SELLER role yet

3. **Admin approval**
   - Login as admin
   - Call: `GET /api/v1/admin/approvals/sellers`
   - ✅ Verify: Application appears in pending list
   - Call: `POST /api/v1/admin/approvals/sellers/{id}/APPROVE`
   - ✅ Verify: Returns 200 OK

4. **Verify SELLER role assigned**
   - Check Keycloak: User now has both "Customer" and "Seller" roles
   - Login as seller
   - Call: `GET /api/v1/me`
   - ✅ Verify: `"roles": ["CUSTOMER", "SELLER"]`
   - Call: `GET /api/v1/sellers/profile`
   - ✅ Verify: `"status": "ACTIVE"`
   - Access seller dashboard
   - ✅ Verify: Dashboard loads successfully

#### Test 3: JIT User Sync Safety Net

1. **Create user in Keycloak without default role**
   - Keycloak Admin Console → Users → Create user
   - Username: `test_jit_user`
   - Do NOT assign any roles manually

2. **Login for first time**
   - Login with `test_jit_user`
   - Backend should detect missing CUSTOMER role

3. **Verify auto-assignment**
   - Check backend logs:
     ```
     INFO - Assigning CUSTOMER role to new user: test_jit_user
     ```
   - Check Keycloak: CUSTOMER role should now be assigned
   - Call: `GET /api/v1/me`
   - ✅ Verify: `"roles": ["CUSTOMER"]`

#### Test 4: Existing Users Not Affected

1. **Login with existing customer**
   - Use a customer account created before deployment
   - Call: `GET /api/v1/me`
   - ✅ Verify: Still has CUSTOMER role
   - ✅ Verify: Can access cart and orders

2. **Login with existing seller**
   - Use a seller account created before deployment
   - Call: `GET /api/v1/me`
   - ✅ Verify: Still has both CUSTOMER and SELLER roles
   - ✅ Verify: Can access seller dashboard

---

## Monitoring and Alerts

### Metrics to Monitor

#### Application Metrics

```
- HTTP 2xx response rate
- HTTP 4xx/5xx error rate
- Request latency (p50, p95, p99)
- Active sessions
- JVM memory usage
- Database connection pool
```

#### Business Metrics

```
- User registrations per hour
- Successful logins per hour
- Role assignment success rate
- Seller applications submitted
- Seller approvals per day
```

#### Keycloak Metrics

```
- Token requests per minute
- Failed authentications
- Active users
- Realm availability
```

### Alert Configuration

#### Critical Alerts (Immediate Action)

```yaml
# Application Down
- alert: ApplicationDown
  expr: up{job="eshop-backend"} == 0
  for: 1m
  severity: critical

# High Error Rate
- alert: HighErrorRate
  expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
  for: 5m
  severity: critical

# Keycloak Down
- alert: KeycloakDown
  expr: up{job="keycloak"} == 0
  for: 1m
  severity: critical
```

#### Warning Alerts (Monitor Closely)

```yaml
# Role Assignment Failures
- alert: RoleAssignmentFailures
  expr: rate(role_assignment_failures_total[10m]) > 0.01
  for: 10m
  severity: warning

# High Login Failures
- alert: HighLoginFailures
  expr: rate(keycloak_login_failures_total[5m]) > 10
  for: 5m
  severity: warning
```

### Logging

#### Important Log Patterns to Monitor

```bash
# Role assignment success
grep "Successfully assigned role" /var/log/eshop/application.log

# Role assignment failures
grep "Failed to assign role" /var/log/eshop/application.log

# Seller approvals
grep "approveSeller" /var/log/eshop/application.log

# JIT user creation
grep "Creating new user from claims" /var/log/eshop/application.log
```

#### Log Aggregation Queries

If using ELK Stack, Splunk, or similar:

```
# Count role assignments per hour
source="/var/log/eshop/application.log" 
| search "Assigning CUSTOMER role" 
| timechart span=1h count

# Track seller approval rate
source="/var/log/eshop/application.log" 
| search "approveSeller" OR "rejectSeller"
| stats count by action
```

---

## Environment-Specific Configurations

### Development

```properties
# application-dev.properties
keycloak.auth-server-url=http://localhost:8080
logging.level.com.eshop.app.service.KeycloakService=DEBUG
```

### Staging

```properties
# application-staging.properties
keycloak.auth-server-url=https://keycloak-staging.eshop.com
logging.level.com.eshop.app.service.KeycloakService=INFO
```

### Production

```properties
# application-production.properties
keycloak.auth-server-url=https://keycloak.eshop.com
logging.level.com.eshop.app.service.KeycloakService=WARN
```

---

## Deployment Scripts

### Script: backup-production.sh

```bash
#!/bin/bash
# Backup production environment before deployment

BACKUP_DIR=${1:-/tmp/backup-$(date +%Y%m%d-%H%M%S)}
mkdir -p $BACKUP_DIR

echo "Creating backup in $BACKUP_DIR"

# Backup Keycloak realm
docker exec keycloak /opt/keycloak/bin/kc.sh export \
  --realm eshop \
  --file /tmp/realm-backup.json
docker cp keycloak:/tmp/realm-backup.json $BACKUP_DIR/

# Backup database
docker exec postgres pg_dump -U postgres eshop > $BACKUP_DIR/database.sql

# Backup configuration files
cp -r src/main/resources/application*.properties $BACKUP_DIR/

# Backup current code
git rev-parse HEAD > $BACKUP_DIR/git-commit.txt
git diff > $BACKUP_DIR/uncommitted-changes.diff

echo "Backup complete: $BACKUP_DIR"
ls -lh $BACKUP_DIR
```

### Script: smoke-test-production.sh

```bash
#!/bin/bash
# Run smoke tests after deployment

set -e

BASE_URL=${1:-https://eshop.com}

echo "Running smoke tests against $BASE_URL"

# Test 1: Health check
echo "Test 1: Health check"
curl -f $BASE_URL/actuator/health || exit 1

# Test 2: Keycloak availability
echo "Test 2: Keycloak availability"
curl -f $BASE_URL/realms/eshop/.well-known/openid-configuration || exit 1

# Test 3: API availability
echo "Test 3: API availability"
curl -f $BASE_URL/api/v1/products || exit 1

echo "All smoke tests passed!"
```

---

## Troubleshooting

### Issue: Deployment Fails During Build

**Symptoms:** `./gradlew build` fails

**Solutions:**
1. Check Java version: `java -version` (should be 17+)
2. Clear Gradle cache: `./gradlew clean`
3. Check for test failures: `./gradlew test --info`
4. Review build logs for specific errors

### Issue: Application Won't Start After Deployment

**Symptoms:** Application keeps restarting, health check fails

**Solutions:**
1. Check logs: `docker logs eshop-backend --tail 100`
2. Verify database connection
3. Verify Keycloak is accessible
4. Check for configuration errors in `application.properties`
5. Rollback to previous version if critical

### Issue: Keycloak Configuration Not Applied

**Symptoms:** Default role not assigned, realm settings unchanged

**Solutions:**
1. Verify import succeeded: `docker logs keycloak | grep "import"`
2. Check realm-export.json is valid JSON
3. Try manual import via Admin Console
4. Run configuration script: `./scripts/configure-keycloak-roles.sh`

---

## Contact and Support

### Deployment Team
- Lead: [Name]
- Email: deployment@eshop.com
- Slack: #deployments

### Incident Response
- On-Call Engineer: Check PagerDuty rotation
- Emergency Contact: +1-xxx-xxx-xxxx
- Incident Slack Channel: #incidents

---

**Document Version:** 1.0  
**Last Updated:** 2026-02-17  
**Author:** Development Team  
**Next Review Date:** 2026-03-17
