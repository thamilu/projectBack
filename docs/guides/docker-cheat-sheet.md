# Quick Reference - Docker Commands Cheat Sheet

## 🚀 Daily Commands

### Start Your Dev Environment

```powershell
# One command to start everything
cd /d G:\Project\eshop_back
docker compose -f docker-compose-dev.yml up -d

# Run your app
.\gradlew bootRun

# Open monitoring
start http://localhost:3002  # Grafana
```

### Stop Everything

```powershell
# Stop containers (keep data)
docker compose -f docker-compose-dev.yml down

# Stop and delete data
docker compose -f docker-compose-dev.yml down -v
```

---

## 📋 Service URLs

### DEV Environment

| Service | URL | Login |
|---------|-----|-------|
| **Your App** | http://localhost:8082 | - |
| **Swagger** | http://localhost:8082/swagger-ui.html | - |
| **Grafana** | http://localhost:3002 | admin / admin |
| **Prometheus** | http://localhost:9090 | - |
| **Zipkin** | http://localhost:9411 | - |
| **pgAdmin** | http://localhost:5050 | admin@eshop.com / admin |
| **Redis Commander** | http://localhost:8081 | - |
| Keycloak | http://localhost:8080 | admin / admin |

### DOCKER Environment

| Service | URL |
|---------|-----|
| **Backend** | http://localhost:8082 |
| **Grafana** | http://localhost:3002 |
| **Prometheus** | http://localhost:9090 |
| **Zipkin** | http://localhost:9411 |

---

## 🐳 Essential Docker Commands

### View Containers

```powershell
# List running containers
docker ps

# List all containers (including stopped)
docker ps -a

# See resource usage
docker stats
```

### Logs

```powershell
# View logs
docker logs eshop-backend

# Follow logs (real-time)
docker logs eshop-backend -f

# Last 100 lines
docker logs eshop-backend --tail 100

# View all services logs
docker compose -f docker-compose-dev.yml logs -f
```

### Start/Stop Services

```powershell
# Start all services
docker compose -f docker-compose-dev.yml up -d

# Start specific service
docker compose -f docker-compose-dev.yml up -d postgres

# Stop all services
docker compose -f docker-compose-dev.yml down

# Restart service
docker compose -f docker-compose-dev.yml restart redis
```

### Execute Commands in Container

```powershell
# Open shell in container
docker exec -it eshop-postgres sh

# Run single command
docker exec -it eshop-postgres psql -U postgres -d eshop_Dev

# Check Java version
docker exec -it eshop-backend java -version
```

### Clean Up

```powershell
# Remove stopped containers
docker container prune

# Remove unused images
docker image prune

# Remove unused volumes
docker volume prune

# Remove everything (CAREFUL!)
docker system prune -a --volumes
```

---

## 🗄️ Database Commands

### PostgreSQL

```powershell
# Connect to database
docker exec -it eshop-postgres psql -U postgres -d eshop_Dev

# Common psql commands (inside psql):
\dt              # List tables
\d users         # Describe table
\l               # List databases
\q               # Quit

# Backup database
docker exec eshop-postgres pg_dump -U postgres eshop_Dev > backup.sql

# Restore database
docker exec -i eshop-postgres psql -U postgres -d eshop_Dev < backup.sql

# Create database
docker exec -it eshop-postgres psql -U postgres -c "CREATE DATABASE eshop_test;"
```

### Redis

```powershell
# Connect to Redis
docker exec -it eshop-redis redis-cli

# Common Redis commands (inside redis-cli):
KEYS *           # List all keys
GET key_name     # Get value
DEL key_name     # Delete key
FLUSHALL         # Delete all keys (CAREFUL!)
INFO stats       # Show statistics
QUIT             # Exit

# Monitor Redis in real-time
docker exec -it eshop-redis redis-cli MONITOR
```

---

## 📊 Monitoring Commands

### Prometheus Queries

```promql
# Request rate (requests per second)
rate(http_server_requests_seconds_count[1m])

# Response time (95th percentile)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m]))

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[1m])

# JVM memory usage
jvm_memory_used_bytes{area="heap"}

# Database connections
hikaricp_connections_active

# System CPU usage
system_cpu_usage
```

### Health Checks

```powershell
# App health
curl http://localhost:8082/actuator/health

# Detailed health (dev only)
curl http://localhost:8082/actuator/health | jq

# Prometheus metrics
curl http://localhost:8082/actuator/prometheus | grep jvm_memory
```

---

## 🔧 Troubleshooting Quick Fixes

### Port Already in Use

```powershell
# Find process using port
netstat -ano | findstr :8082

# Kill process
taskkill /PID <PID> /F
```

### Container Won't Start

```powershell
# Check logs for error
docker logs eshop-postgres --tail 50

# Remove and recreate
docker compose -f docker-compose-dev.yml down
docker compose -f docker-compose-dev.yml up -d
```

### Database Connection Failed

```powershell
# Check if PostgreSQL is running
docker ps | findstr postgres

# Test connection
docker exec -it eshop-postgres psql -U postgres -c "SELECT 1;"

# Restart PostgreSQL
docker compose -f docker-compose-dev.yml restart postgres
```

### Prometheus Not Scraping

```powershell
# Check targets
# Open: http://localhost:9090/targets

# Test metrics endpoint
curl http://localhost:8082/actuator/prometheus

# Restart Prometheus
docker compose -f docker-compose-dev.yml restart prometheus
```

### Out of Disk Space

```powershell
# Check Docker disk usage
docker system df

# Clean up
docker system prune -a --volumes

# Remove old images
docker image prune -a
```

---

## 🔄 Environment Switch

### Switch from DEV to DOCKER

```powershell
# Stop dev environment
docker compose -f docker-compose-dev.yml down

# Build and start docker environment
docker compose build
docker compose up -d

# Verify Java version
docker exec -it eshop-backend java -version
```

### Switch from DOCKER to DEV

```powershell
# Stop docker environment
docker compose down

# Start dev environment
docker compose -f docker-compose-dev.yml up -d

# Run app locally
.\gradlew bootRun
```

---

## 🏗️ Build Commands

### Gradle

```powershell
# Build JAR
.\gradlew build

# Run tests
.\gradlew test

# Clean build
.\gradlew clean build

# Skip tests (faster)
.\gradlew build -x test

# Run application
.\gradlew bootRun
```

### Docker

```powershell
# Build image
docker compose build

# Build without cache (fresh build)
docker compose build --no-cache

# Build specific service
docker compose build backend

# Tag image
docker tag eshop-backend:latest eshop-backend:1.0.0
```

---

## 📦 Docker Compose Environments

### DEV - Local Development

```powershell
docker compose -f docker-compose-dev.yml up -d
docker compose -f docker-compose-dev.yml down
docker compose -f docker-compose-dev.yml logs -f
```

### DOCKER - Full Stack Test

```powershell
docker compose up -d
docker compose down
docker compose logs -f backend
```

### PRODUCTION - Deployment

```powershell
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
docker compose -f docker-compose.prod.yml down
docker compose -f docker-compose.prod.yml logs -f app
```

---

## 🎯 Common Workflows

### Morning Startup

```powershell
cd G:\Project\eshop_back
docker compose -f docker-compose-dev.yml up -d
.\gradlew bootRun
start http://localhost:3002
```

### Reset Database

```powershell
# Stop services
docker compose -f docker-compose-dev.yml down -v

# Start fresh
docker compose -f docker-compose-dev.yml up -d

# Database is now empty
```

### Check Application Status

```powershell
# Container status
docker ps

# Logs
docker logs eshop-backend --tail 50

# Health
curl http://localhost:8082/actuator/health

# Metrics
start http://localhost:3002
```

### Debug Slow Request

```powershell
# 1. Make request
curl http://localhost:8082/api/products

# 2. Check Zipkin
start http://localhost:9411

# 3. Find slow span
# 4. Check database query in pgAdmin
start http://localhost:5050
```

---

## 💾 Backup & Restore

### Backup Everything

```powershell
# Database
docker exec eshop-postgres pg_dump -U postgres eshop_Dev > backup_db.sql

# Application config
cp -r src/main/resources backup_config/

# Docker volumes
docker run --rm -v eshop_back_postgres_dev_data:/data -v ${PWD}:/backup alpine tar czf /backup/postgres_backup.tar.gz /data
```

### Restore Database

```powershell
# Restore from backup
docker exec -i eshop-postgres psql -U postgres -d eshop_Dev < backup_db.sql
```

---

## 🔐 Security Checks

### View Environment Variables

```powershell
# Check what's set
docker exec eshop-backend env | grep DATABASE

# Verify no secrets in logs
docker logs eshop-backend | grep -i password
```

### Check Exposed Ports

```powershell
# List all exposed ports
docker ps --format "table {{.Names}}\t{{.Ports}}"
```

---

## 📞 Emergency Commands

### App is Down

```powershell
# Check if running
docker ps | findstr eshop-backend

# Check logs
docker logs eshop-backend --tail 100

# Restart
docker compose -f docker-compose-dev.yml restart

# If still down, check health
curl http://localhost:8082/actuator/health
```

### Database Locked

```powershell
# Check active connections
docker exec -it eshop-postgres psql -U postgres -d eshop_Dev -c "SELECT * FROM pg_stat_activity;"

# Kill blocking queries
docker exec -it eshop-postgres psql -U postgres -d eshop_Dev -c "SELECT pg_terminate_backend(<pid>);"
```

### Out of Memory

```powershell
# Check usage
docker stats

# Free memory
docker system prune

# Restart containers
docker compose -f docker-compose-dev.yml restart
```

---

## 📚 Useful URLs

### Documentation

- **Complete Guide:** `complete-docker-guide.md`
- **Walkthrough:** `walkthrough.md`
- **Dev Observability:** `dev-observability-guide.md`

### External Docs

- Docker: https://docs.docker.com/
- Spring Boot: https://spring.io/projects/spring-boot
- Prometheus: https://prometheus.io/docs/
- Grafana: https://grafana.com/docs/
- PostgreSQL: https://www.postgresql.org/docs/

---

## 🎓 Most Used Commands (Top 10)

```powershell
# 1. Start dev environment
docker compose -f docker-compose-dev.yml up -d

# 2. View logs
docker logs eshop-backend -f

# 3. Check running containers
docker ps

# 4. Stop everything
docker compose -f docker-compose-dev.yml down

# 5. Restart service
docker compose -f docker-compose-dev.yml restart postgres

# 6. Clean up
docker system prune

# 7. Check health
curl http://localhost:8082/actuator/health

# 8. Connect to database
docker exec -it eshop-postgres psql -U postgres -d eshop_Dev

# 9. Connect to Redis
docker exec -it eshop-redis redis-cli

# 10. Build image
docker compose build
```

---

**Print this and keep it handy! 📋**
