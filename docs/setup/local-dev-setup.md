# 🔧 Local Development Setup (Without Docker)

If you want to run the backend **locally** (not in Docker) but still use Redis:

---

## Option 1: Redis on Windows (Recommended)

### Using WSL2 (Easiest)
```powershell
# Install Redis in WSL2
wsl
sudo apt update
sudo apt install redis-server
redis-server
```

### Using Docker for Redis Only
```powershell
# Start only Redis container
docker run -d --name redis -p 6379:6379 redis:7-alpine

# Verify
docker ps
```

### Using Memurai (Native Windows Redis)
Download from: https://www.memurai.com/

---

## Option 2: Disable Redis for Local Dev

### Quick Fix: Disable Redis Cache

**In `application-dev.properties`:**
```properties
# Disable Redis - Use in-memory cache instead
spring.cache.type=caffeine
app.redis.enabled=false
```

**Or set environment variable:**
```powershell
$env:SPRING_CACHE_TYPE="caffeine"
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

---

## Option 3: Make Redis Optional

### Graceful Degradation (Enterprise Pattern)

Already implemented in your app! Check:
```properties
# application.properties
app.redis.resilient.mode=true
```

This means:
- ✅ Redis available → Use Redis
- ✅ Redis down → Fallback to Caffeine (local cache)
- ✅ No app crash

---

## Run Locally

```powershell
# Method 1: Using Gradle
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"

# Method 2: Using JAR
.\gradlew.bat clean build -x test
java -jar build/libs/eshop-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# Method 3: IDE (IntelliJ/VS Code)
# Set VM Options: -Dspring.profiles.active=dev
```

---

## Profile Selection Guide

| Profile | Use Case | Redis Host | Database |
|---------|----------|------------|----------|
| **dev** | Local development | `localhost` | localhost:5432 |
| **docker** | Docker containers | `redis` (service name) | postgres:5432 |
| **prod** | Production | Redis cluster | Cloud DB |
| **test** | Unit tests | Mock/Embedded | H2/TestContainers |

---

## Quick Redis Commands

```bash
# Check if Redis is running
redis-cli ping
# Expected: PONG

# Monitor Redis activity
redis-cli monitor

# Check keys
redis-cli keys "*"

# Clear all cache
redis-cli flushall
```

---

## Environment Variables for Local Dev

```powershell
# PowerShell
$env:SPRING_PROFILES_ACTIVE="dev"
$env:REDIS_HOST="localhost"
$env:DATABASE_URL="jdbc:postgresql://localhost:5432/eshop_Dev"

# Then run
.\gradlew.bat bootRun
```

---

## Summary

**For Docker:** Use `docker-compose.yml` → All services managed  
**For Local Dev:** Use `dev` profile → Redis on localhost or disabled  

Choose what works best for your workflow! 🚀
