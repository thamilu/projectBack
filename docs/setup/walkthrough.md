# Production-Ready Docker Stack - Complete Guide

## 🎯 What's Been Implemented

You now have a **complete, enterprise-grade Docker stack** with:

✅ **3 Environments**: Dev, Docker Testing, Production  
✅ **Observability**: Prometheus + Grafana + Zipkin  
✅ **Security**: Nginx reverse proxy, SSL support, Redis auth  
✅ **Dev Tools**: pgAdmin + Redis Commander  
✅ **Java 21 LTS**: Guaranteed across all environments  
✅ **Latest Versions**: Redis 7.4, Keycloak 26.5.2, PostgreSQL 16  

---

## 🏗️ Architecture Overview

### Development Environment
```
┌──────────────────────────────────────────────────────┐
│ Your IDE (IntelliJ / VS Code)                        │
│ Spring Boot runs locally                             │
│ Port: 8082                                           │
└──────────────────────────────────────────────────────┘
                    ↓ connects to
┌──────────────────────────────────────────────────────┐
│ Docker Services  (5 containers)                      │
│  • PostgreSQL:5432     • Keycloak:8080              │
│  • Redis:6379          • pgAdmin:5050                │
│  • Redis Commander:8081                              │
└──────────────────────────────────────────────────────┘
```

### Docker Testing Environment
```
┌──────────────────────────────────────────────────────┐
│ Complete Stack in Docker  (8 containers)             │
│                                                       │
│  Application Layer:                                   │
│    • Spring Boot (Java 21):8082                      │
│                                                       │
│  Infrastructure:                                      │
│    • PostgreSQL:5432     • Redis:6379                │
│    • Keycloak:8080                                   │
│                                                       │
│  Observability:                                       │
│    • Prometheus:9090     • Grafana:3002              │
│    • Zipkin:9411                                     │
└──────────────────────────────────────────────────────┘
```

### Production Environment
```
                    Internet
                      ↓
┌──────────────────────────────────────────────────────┐
│              Nginx (80/443)                          │
│   SSL Termination + Reverse Proxy                    │
└──────────────────────────────────────────────────────┘
                      ↓
┌──────────────────────────────────────────────────────┐
│  Application:                                         │
│    • Spring Boot (Java 21) - Internal only           │
│                                                       │
│  Infrastructure:                                      │
│    • PostgreSQL - Internal                           │
│    • Redis - External/Managed (recommended)          │
│    • Keycloak - External/Managed (recommended)       │
│                                                       │
│  Monitoring (Internal Access Only):                   │
│    • Prometheus:127.0.0.1:9090                       │
│    • Grafana:127.0.0.1:3002                          │
│    • Zipkin:127.0.0.1:9411                           │
└──────────────────────────────────────────────────────┘
```

---

## 📋 Environment 1: Development (Local IDE + Docker Services)

### Purpose
- Daily development with **instant code reload**
- Spring Boot runs on your machine (not in Docker)
- Supporting services in Docker

### Services (5 containers)
| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL | 5432 | Development database |
| Redis | 6379 | Cache |
| Keycloak | 8080 | Authentication |
| **pgAdmin** | 5050 | Database GUI |
| **Redis Commander** | 8081 | Redis GUI |

### How to Start

```powershell
# Navigate to project
cd G:\Project\eshop_back

# Start supporting services
docker compose -f docker-compose-dev.yml up -d

# Check status
docker ps

# View logs
docker compose -f docker-compose-dev.yml logs -f
```

### Run Your Application Locally

```powershell
# Option 1: Gradle
./gradlew bootRun

# Option 2: IDE
# Run EshopApplication.java with profile: dev
```

### Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| Your App (Local) | http://localhost:8082 | N/A |
| Swagger | http://localhost:8082/swagger-ui.html | N/A |
| **pgAdmin** | http://localhost:5050 | admin@eshop.com / admin |
| **Redis Commander** | http://localhost:8081 | N/A |
| Keycloak | http://localhost:8080 | admin / admin |

### pgAdmin Setup (First Time)

1. Open http://localhost:5050
2. Login: `admin@eshop.com` / `admin`
3. Add Server:
   - Name: `Eshop Dev`
   - Host: `postgres` (or `localhost`)
   - Port: `5432`
   - Database: `eshop_Dev`
   - Username: `postgres`
   - Password: `thamilu*884*`

### Stop Services

```powershell
docker compose -f docker-compose-dev.yml down
```

---

## 📋 Environment 2: Docker Testing (Full Stack in Docker)

### Purpose
- Test complete Dockerized stack
- Verify Java 21 is working
- Test with observability tools
- CI/CD testing

### Services (8 containers)
| Service | Port | Purpose |
|---------|------|---------|
| Spring Boot | 8082 | Your application (Java 21) |
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache |
| Keycloak | 8080 | Authentication |
| **Prometheus** | 9090 | Metrics collection |
| **Grafana** | 3002 | Dashboards |
| **Zipkin** | 9411 | Distributed tracing |

### How to Start

```powershell
# Navigate to project
cd G:\Project\eshop_back

# Build images
docker compose build

# Start all services
docker compose up -d

# Check status
docker ps

# View backend logs
docker logs eshop-backend -f

# View all logs
docker compose logs -f
```

### Verify Java 21

```powershell
# CRITICAL: Verify Java version inside container
docker exec -it eshop-backend java -version

# Expected output:
# openjdk version "21.0.x" YYYY-MM-DD LTS
# OpenJDK Runtime Environment Temurin-21...
```

### Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **Spring Boot** | http://localhost:8082 | N/A |
| **Swagger** | http://localhost:8082/swagger-ui.html | N/A |
| **Health** | http://localhost:8082/actuator/health | N/A |
| **Metrics** | http://localhost:8082/actuator/prometheus | N/A |
| **Prometheus** | http://localhost:9090 | N/A |
| **Grafana** | http://localhost:3002 (admin/admin) | |
| **Zipkin** | http://localhost:9411 | N/A |
| Keycloak | http://localhost:8080 | admin / admin |

### Explore Monitoring

#### Prometheus
1. Open http://localhost:9090
2. Go to **Status** → **Targets**
3. Verify `spring-boot-eshop` is **UP**
4. Try queries:
   ```
   http_server_requests_seconds_count
   jvm_memory_used_bytes
   system_cpu_usage
   ```

#### Grafana
1. Open http://localhost:3002
2. Login:admin / admin
3. Go to **Explore**
4. Select **Prometheus** datasource
5. Run queries or create dashboards

#### Zipkin
1. Open http://localhost:9411
2. Click **Run Query** to see traces
3. Click on a trace to see timing breakdown

### Rebuild After Code Changes

```powershell
# Rebuild only backend
docker compose build backend

# Restart backend
docker compose up -d backend

# Or rebuild everything
docker compose down
docker compose build
docker compose up -d
```

### Stop Everything

```powershell
docker compose down

# Or with volume cleanup
docker compose down -v
```

---

## 📋 Environment 3: Production Deployment

### Purpose
- Production deployment
- SSL/HTTPS with Nginx
- Resource limits
- External managed services

### Services (6+ containers)
| Service | Port | Purpose |
|---------|------|---------|
| **Nginx** | 80, 443 | Reverse proxy + SSL |
| Spring Boot | Internal | Your application |
| PostgreSQL | Internal | Database (or external) |
| Prometheus | 127.0.0.1:9090 | Metrics (internal only) |
| Grafana | 127.0.0.1:3002 | Dashboards (internal only) |
| Zipkin | 127.0.0.1:9411 | Tracing (internal only) |

### Prerequisites

1. **SSL Certificates**
   ```powershell
   # Create ssl directory
   mkdir nginx/ssl
   
   # Add your SSL certificates
   # nginx/ssl/cert.pem
   # nginx/ssl/key.pem
   ```

2. **Environment Variables**
   ```powershell
   # Copy example and edit
   cp .env.example .env.prod
   
   # Edit .env.prod with production values
   notepad .env.prod
   ```

3. **Set Production Secrets**
   - `DATABASE_PASSWORD` - Strong password
   - `REDIS_PASSWORD` - Strong password
   - `JWT_SECRET` - 256-bit secret
   - `KEYCLOAK_ADMIN_PASSWORD` - Strong password
   - Payment gateway keys
   - `CORS_ORIGINS` - Your frontend URL

### Deploy to Production

```powershell
# Build production image
docker compose -f docker-compose.prod.yml build

# Start with environment file
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Check status
docker ps

# View  logs
docker compose -f docker-compose.prod.yml logs -f app
```

### Access Production Services

| Service | URL | Access |
|---------|-----|--------|
| **Application** | https://yourdomain.com | Public |
| Health | https://yourdomain.com/actuator/health | Public |
| Grafana | http://SERVER_IP:3002 | Internal only (SSH tunnel) |
| Prometheus | http://SERVER_IP:9090 | Internal only (SSH tunnel) |

### SSH Tunnel for Monitoring (From Your Local Machine)

```powershell
# Access Grafana
ssh -L 3002:localhost:3002 user@your-server

# Access Prometheus
ssh -L 9090:localhost:9090 user@your-server

# Then open http://localhost:3002 locally
```

### Production Monitoring

```powershell
# Check health
curl https://yourdomain.com/actuator/health

# View application logs
docker logs eshop-backend-prod --tail 100 -f

# Check resource usage
docker stats

# Check Nginx logs
docker logs eshop-nginx --tail 100 -f
```

---

## 🔧 Useful Commands

### General Docker Commands

```powershell
# View all containers
docker ps -a

# View images
docker images

# Remove stopped containers
docker container prune

# Remove unused images
docker image prune

# Remove everything (CAREFUL!)
docker system prune -a --volumes

# View resource usage
docker stats

# Inspect container
docker inspect eshop-backend

# Execute command in container
docker exec -it eshop-backend sh
```

### Environment-Specific Commands

```powershell
# DEV
docker compose -f docker-compose-dev.yml up -d
docker compose -f docker-compose-dev.yml down
docker compose -f docker-compose-dev.yml logs -f

# DOCKER
docker compose up -d
docker compose down
docker compose logs -f backend

# PROD
docker compose -f docker compose.prod.yml --env-file .env.prod up -d
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml logs -f app
```

### Database Commands

```powershell
# Backup PostgreSQL
docker exec eshop-postgres pg_dump -U eshop eshop_db > backup.sql

# Restore PostgreSQL
docker exec -i eshop-postgres psql -U eshop eshop_db < backup.sql

# Connect to PostgreSQL
docker exec docker exec -it eshop-postgres psql -U eshop -d eshop_db
```

### Redis Commands

```powershell
# Connect to Redis
docker exec -it eshop-redis redis-cli

# Check keys
docker exec -it eshop-redis redis-cli KEYS '*'

# Flush all (CAREFUL!)
docker exec -it eshop-redis redis-cli FLUSHALL
```

---

## 🐛 Troubleshooting

### Issue: Container won't start

```powershell
# Check logs
docker logs eshop-backend

# Check last 50 lines
docker logs eshop-backend --tail 50

# Follow logs in real-time
docker logs eshop-backend -f
```

### Issue: Port already in use

```powershell
# Find process using port 8082
netstat -ano | findstr :8082

# Kill process (use PID from above)
taskkill /PID <PID> /F
```

### Issue: Can't connect to database

```powershell
# Check if PostgreSQL is running
docker ps | findstr postgres

# Check PostgreSQL logs
docker logs eshop-postgres

# Test connection
docker exec -it eshop-postgres psql -U eshop -d eshop_db -c "SELECT 1;"
```

### Issue: Java version is wrong

```powershell
# Rebuild from scratch
docker compose build --no-cache backend

# Verify Dockerfile is using correct images
# Build: gradle:8.14-jdk21
# Runtime: eclipse-temurin:21-jre

# Check inside container
docker exec -it eshop-backend java -version
```

### Issue: Keycloak not starting

```powershell
# Check logs
docker logs eshop-keycloak -f

# Common issue: Database not ready
# Solution: Wait for PostgreSQL health check

# Restart Keycloak
docker compose restart keycloak
```

### Issue: Prometheus not scraping metrics

```powershell
# Check Prometheus targets
# Open: http://localhost:9090/targets

# Check if backend actuator is accessible
curl http://localhost:8082/actuator/prometheus

# Check prometheus.yml configuration
cat prometheus/prometheus.yml
```

---

## 📊 Monitoring Dashboards

### Recommended Grafana Dashboards

Import these from https://grafana.com/grafana/dashboards/:

1. **Spring Boot Dashboard** - ID: 6756
   - JVM metrics, HTTP requests, database connections

2. **JVM Micrometer** - ID: 4701
   - Detailed JVM statistics

3. **PostgreSQL Database** - ID: 9628
   - Database performance metrics

### How to Import

1. Open Grafana: http://localhost:3002
2. Click **+** → **Import Dashboard**
3. Enter dashboard ID
4. Select **Prometheus** datasource
5. Click **Import**

---

## 🎯 Next Steps

### 1. Configure Keycloak Realm

```
1. Open http://localhost:8080
2. Login: admin / admin
3. Create realm: "eshop"
4. Create client: "eshop-backend"
5. Configure redirect URIs
6. Get client secret
7. Update application properties
```

### 2. Set Up Flyway Migrations

```powershell
# Create migration directory
mkdir src/main/resources/db/migration

# Create first migration
# src/main/resources/db/migration/V1__initial_schema.sql
```

### 3. Configure Production SSL

```powershell
# Option 1: Let's Encrypt (Free)
# Use Certbot to generate certificates

# Option 2: Commercial Certificate
# Place cert.pem and key.pem in nginx/ssl/
```

### 4. Set Up CI/CD

```yaml
# Example GitHub Actions
name: Docker Build
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Build Docker image
        run: docker compose build
```

---

## 🎉 Summary

You now have:

✅ **Dev Environment** - Fast development with Docker services  
✅ **Docker Environment** - Full stack testing with monitoring  
✅ **Production Environment** - Production-ready with SSL and security  
✅ **Observability** - Prometheus, Grafana, Zipkin  
✅ **Dev Tools** - pgAdmin, Redis Commander  
✅ **Java 21 LTS** - Guaranteed and verified  

**All environments work independently and won't interfere with each other!**

---

## 📞 Quick Reference

### Service URLs Summary

| Environment | Dev | Docker | Production |
|-------------|-----|--------|------------|
| **App** | localhost:8082 | localhost:8082 | https://domain.com |
| **pgAdmin** | localhost:5050 | ❌ | ❌ |
| **Redis GUI** | localhost:8081 | ❌ | ❌ |
| **Prometheus** | ❌ | localhost:9090 | Internal only |
| **Grafana** | ❌ | localhost:3002 | Internal only |
| **Zipkin** | ❌ | localhost:9411 | Internal only |
| **Keycloak** | localhost:8080 | localhost:8080 | External |

### Common Commands

```powershell
# DEV: Start
docker compose -f docker-compose-dev.yml up -d

# DOCKER: Build and start
docker compose build && docker compose up -d

# PROD: Deploy
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Check Java version
docker exec -it eshop-backend java -version

# View logs
docker logs eshop-backend -f

# Stop everything
docker compose down
```

🚀 **You're all set! Happy coding!**
