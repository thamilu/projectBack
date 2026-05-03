# 🚀 Local Development Setup

**Keycloak + Redis in Docker, Spring Boot Backend Locally**

---

## 🎯 Overview

This setup allows you to:
- ✅ Run **Keycloak** and **Redis** in Docker (infrastructure)
- ✅ Run **Spring Boot backend** locally (on your machine)
- ✅ Hot reload and debug backend easily
- ✅ No need to rebuild Docker images for code changes

---

## 📋 Prerequisites

- Docker Desktop installed and running
- Java 21 installed
- Gradle (wrapper included)

---

## 🚀 Quick Start

### Option 1: Automated (Recommended)

**Windows PowerShell:**
```powershell
.\start-dev-infra.ps1
```

**Linux/Mac:**
```bash
chmod +x start-dev-infra.sh
./start-dev-infra.sh
```

### Option 2: Manual Steps

```bash
# 1. Start Keycloak + Redis in Docker
docker compose -f docker-compose-dev.yml up -d

# 2. Wait for services (30-60 seconds)
docker compose -f docker-compose-dev.yml ps

# 3. Run Spring Boot backend locally
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

---

## 📊 What's Running Where

| Service | Where | URL | Purpose |
|---------|-------|-----|---------|
| **Keycloak** | Docker | http://localhost:8080 | Authentication |
| **Redis** | Docker | localhost:6379 | Cache |
| **PostgreSQL** | Docker | localhost:5432 | Database |
| **Spring Boot** | Local | http://localhost:8082 | Your API |

---

## 🔧 Configuration

### Docker Services (docker-compose-dev.yml)

**Services included:**
- ✅ Redis 7 Alpine
- ✅ PostgreSQL 16 Alpine
- ✅ Keycloak 23.0

**Exposed Ports:**
- Redis: `6379`
- PostgreSQL: `5432`
- Keycloak: `8080`

### Spring Boot (application-dev.properties)

**Profile:** `dev`

**Connections:**
```properties
spring.data.redis.host=localhost      # Redis in Docker
spring.data.redis.port=6379

spring.datasource.url=jdbc:postgresql://localhost:5432/eshop_Dev

keycloak.auth-server-url=http://localhost:8080
```

---

## 🧪 Verify Everything Works

### 1. Check Docker Services
```powershell
docker compose -f docker-compose-dev.yml ps
```

**Expected:** All services `healthy`

### 2. Test Redis
```bash
docker exec -it eshop-redis-dev redis-cli ping
# Expected: PONG
```

### 3. Test Keycloak
Open browser: http://localhost:8080

**Login:** admin / admin

### 4. Run Spring Boot Backend
```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

**Expected output:**
```
Started EshopApplication in X.XXX seconds
Featured products cache warmed
```

### 5. Test Backend Health
```bash
curl http://localhost:8082/actuator/health
```

**Expected:**
```json
{
  "status": "UP",
  "components": {
    "redis": { "status": "UP" }
  }
}
```

---

## 🔍 Useful Commands

### Docker Management

```bash
# View all logs
docker compose -f docker-compose-dev.yml logs -f

# View specific service logs
docker compose -f docker-compose-dev.yml logs -f redis
docker compose -f docker-compose-dev.yml logs -f keycloak

# Restart services
docker compose -f docker-compose-dev.yml restart

# Stop all services
docker compose -f docker-compose-dev.yml down

# Stop and remove volumes (fresh start)
docker compose -f docker-compose-dev.yml down -v
```

### Backend Management

```bash
# Run with dev profile
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"

# Build without tests
.\gradlew.bat clean build -x test

# Run tests
.\gradlew.bat test

# Clean build
.\gradlew.bat clean
```

### Redis Commands

```bash
# Enter Redis container
docker exec -it eshop-redis-dev redis-cli

# Check keys
keys *

# Monitor Redis activity
monitor

# Clear all cache
flushall
```

---

## 🐛 Troubleshooting

### Problem: "Connection refused" to Redis
**Solution:**
```bash
# Check if Redis is running
docker ps | grep redis

# If not running, start it
docker compose -f docker-compose-dev.yml up -d redis

# Check Redis health
docker inspect eshop-redis-dev | grep Health
```

### Problem: Backend can't connect to Keycloak
**Solution:**
1. Wait 60 seconds for Keycloak to fully start
2. Check: http://localhost:8080
3. Verify logs: `docker compose -f docker-compose-dev.yml logs keycloak`

### Problem: Port already in use
**Solutions:**

**Redis (6379):**
```bash
# Windows
netstat -ano | findstr :6379
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:6379 | xargs kill -9
```

**Keycloak (8080):**
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Linux/Mac
lsof -ti:8080 | xargs kill -9
```

### Problem: Backend starts but dashboard returns 500
**Cause:** Redis not connected

**Solution:**
1. Check Redis health: `docker ps`
2. Verify application-dev.properties has `spring.data.redis.host=localhost`
3. Test Redis: `docker exec -it eshop-redis-dev redis-cli ping`

---

## 💡 Development Workflow

### Starting Your Day
```bash
# 1. Start infrastructure
.\start-dev-infra.ps1

# 2. Wait for services (check with docker ps)
docker compose -f docker-compose-dev.yml ps

# 3. Start backend from IDE or terminal
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

### Ending Your Day
```bash
# Stop Docker services (keeps data)
docker compose -f docker-compose-dev.yml down

# Or stop and remove volumes (fresh start next time)
docker compose -f docker-compose-dev.yml down -v
```

### Making Changes
- Backend code changes → Just save and restart (or use hot reload)
- No need to rebuild Docker images
- Database schema changes → Hibernate will auto-update (ddl-auto=update)

---

## 🎯 IDE Configuration

### IntelliJ IDEA

**Run Configuration:**
1. Edit Run Configuration
2. Set Main class: `com.eshop.app.EshopApplication`
3. Add VM options: `-Dspring.profiles.active=dev`
4. Apply and Run

### VS Code

**.vscode/launch.json:**
```json
{
  "type": "java",
  "name": "EShop Dev",
  "request": "launch",
  "mainClass": "com.eshop.app.EshopApplication",
  "args": "--spring.profiles.active=dev",
  "projectName": "eshop"
}
```

---

## 📈 Advantages of This Setup

✅ **Fast Development**
- No Docker image rebuilds
- Instant code changes
- Easy debugging

✅ **Isolated Infrastructure**
- Keycloak and Redis in containers
- Clean separation of concerns
- Easy to reset

✅ **Flexible**
- Can run backend in IDE
- Full debugging support
- Hot reload works

✅ **Realistic**
- Same services as production
- Tests real Redis behavior
- Keycloak integration

---

## 🔄 Switching to Full Docker

Need to run everything in Docker? Use:
```bash
docker compose -f docker-compose.yml up -d
```

This will run backend in Docker too (profile: docker).

---

## 📚 Related Documentation

- [docker-compose-dev.yml](docker-compose-dev.yml) - Development infrastructure
- [docker-compose.yml](docker-compose.yml) - Full Docker setup
- [application-dev.properties](src/main/resources/application-dev.properties) - Dev configuration
- [DOCKER_REDIS_FIX.md](DOCKER_REDIS_FIX.md) - Full Docker setup guide

---

## ✅ Quick Checklist

Before reporting issues, verify:

- [ ] Docker Desktop is running
- [ ] All containers are healthy: `docker ps`
- [ ] Redis responds: `docker exec -it eshop-redis-dev redis-cli ping`
- [ ] Keycloak is accessible: http://localhost:8080
- [ ] Backend uses `dev` profile: Check startup logs
- [ ] Ports 6379, 8080, 5432, 8082 are not in use

---

**Status:** ✅ Ready for development  
**Setup Time:** ~2 minutes  
**Best For:** Active development with frequent code changes

Run `.\start-dev-infra.ps1` and start coding! 🚀
