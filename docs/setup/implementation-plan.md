# Production-Ready Docker Stack - Implementation Plan

## Overview

Implement a complete, enterprise-grade Docker stack for **all environments** (dev, docker, prod) with:
- ✅ **Observability**: Prometheus + Grafana + Zipkin
- ✅ **Security**: Redis authentication, secure secrets management
- ✅ **Database**: Flyway migrations, automated backups
- ✅ **Monitoring**: Health checks, resource limits, logging
- ✅ **Production**: Nginx reverse proxy, SSL/TLS support
- ✅ **Development**: GUI tools (pgAdmin, Redis Commander)

---

## Architecture Overview

### Development Environment (`docker-compose-dev.yml`)
```
┌─────────────────────────────────────────────────────────┐
│  Developer's Machine (IDE + Local Spring Boot)          │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  Docker Services (Supporting Infrastructure)            │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐       │
│  │ PostgreSQL │  │   Redis    │  │  Keycloak  │       │
│  └────────────┘  └────────────┘  └────────────┘       │
│  ┌────────────┐  ┌────────────┐                       │
│  │  pgAdmin   │  │ Redis GUI  │  (Dev Tools)          │
│  └────────────┘  └────────────┘                       │
└─────────────────────────────────────────────────────────┘
```

### Docker Testing Environment (`docker-compose.yml`)
```
┌─────────────────────────────────────────────────────────┐
│  Complete Dockerized Stack                              │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐       │
│  │ PostgreSQL │  │   Redis    │  │  Keycloak  │       │
│  └────────────┘  └────────────┘  └────────────┘       │
│  ┌──────────────────────────────────────────┐          │
│  │     Spring Boot App (Java 21)            │          │
│  └──────────────────────────────────────────┘          │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐       │
│  │ Prometheus │  │  Grafana   │  │   Zipkin   │       │
│  └────────────┘  └────────────┘  └────────────┘       │
└─────────────────────────────────────────────────────────┘
```

### Production Environment (`docker-compose.prod.yml`)
```
┌─────────────────────────────────────────────────────────┐
│                Internet (HTTPS)                          │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  Nginx (Reverse Proxy + SSL Termination)                │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│  Application Stack                                       │
│  ┌──────────────────────────────────────────┐          │
│  │     Spring Boot App (Java 21)            │          │
│  └──────────────────────────────────────────┘          │
│  ┌────────────┐  ┌────────────┐                       │
│  │ PostgreSQL │  │Managed Redis│ (External)            │
│  └────────────┘  └────────────┘                       │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐       │
│  │ Prometheus │  │  Grafana   │  │   Zipkin   │       │
│  └────────────┘  └────────────┘  └────────────┘       │
└─────────────────────────────────────────────────────────┘
```

---

## New Services to Add

### 1. Prometheus (Metrics Collection)
- **Purpose**: Collect metrics from Spring Boot, PostgreSQL, Redis
- **Image**: `prom/prometheus:latest`
- **Port**: 9090
- **Config**: Scrape Spring Boot `/actuator/prometheus` endpoint

### 2. Grafana (Metrics Visualization)
- **Purpose**: Beautiful dashboards for monitoring
- **Image**: `grafana/grafana:latest`
- **Port**: 3000
- **Default Credentials**: admin/admin
- **Pre-configured**: Spring Boot dashboard

### 3. Zipkin (Distributed Tracing)
- **Purpose**: Trace requests across microservices
- **Image**: `openzipkin/zipkin:latest`
- **Port**: 9411
- **Integration**: Spring Boot Micrometer

### 4. pgAdmin (PostgreSQL GUI - Dev Only)
- **Purpose**: Database management interface
- **Image**: `dpage/pgadmin4:latest`
- **Port**: 5050
- **Credentials**: admin@eshop.com / admin

### 5. Redis Commander (Redis GUI - Dev Only)
- **Purpose**: Redis key-value browser
- **Image**: `rediscommander/redis-commander:latest`
- **Port**: 8081

### 6. Nginx (Production Only)
- **Purpose**: Reverse proxy, SSL termination, load balancing
- **Image**: `nginx:alpine`
- **Ports**: 80 (HTTP), 443 (HTTPS)

---

## Implementation Steps

### STEP 1: Update `docker-compose-dev.yml`

#### Changes:
1. Upgrade Redis: `7-alpine` → `7.4-alpine`
2. Upgrade Keycloak: `23.0` → `26.5.2`
3. Add pgAdmin for database management
4. Add Redis Commander for cache inspection
5. Add proper health checks
6. Use latest Java 21 in comments

#### Additional Services (Dev):
```yaml
services:
  pgadmin:
    image: dpage/pgadmin4:latest
    container_name: eshop-pgadmin-dev
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@eshop.com
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    depends_on:
      - postgres

  redis-commander:
    image: rediscommander/redis-commander:latest
    container_name: eshop-redis-commander
    environment:
      - REDIS_HOSTS=local:redis:6379
    ports:
      - "8081:8081"
    depends_on:
      - redis
```

---

### STEP 2: Update `docker-compose.yml` (Docker Testing)

#### Changes:
1. Update to use our new Dockerfile with Java 21 + Gradle 8.14
2. Add Prometheus for metrics
3. Add Grafana for dashboards
4. Add Zipkin for distributed tracing
5. Configure proper networks
6. Add resource limits

#### New Services:
```yaml
services:
  prometheus:
    image: prom/prometheus:latest
    container_name: eshop-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
    depends_on:
      - backend

  grafana:
    image: grafana/grafana:latest
    container_name: eshop-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./grafana/datasources:/etc/grafana/provisioning/datasources
    depends_on:
      - prometheus

  zipkin:
    image: openzipkin/zipkin:latest
    container_name: eshop-zipkin
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem
```

---

### STEP 3: Update `docker-compose.prod.yml` (Production)

#### Changes:
1. Add Nginx reverse proxy with SSL
2. Add Prometheus + Grafana (monitoring)
3. Configure Redis with authentication
4. Add proper secrets management
5. Configure resource limits
6. Add logging configuration
7. Use external Keycloak (not in Docker)

#### Production-Specific:
```yaml
services:
  nginx:
    image: nginx:alpine
    container_name: eshop-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - ./nginx/logs:/var/log/nginx
    depends_on:
      - app
    restart: always

  app:
    image: ${DOCKER_REGISTRY}/eshop-backend:${VERSION}
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
        reservations:
          cpus: '1.0'
          memory: 1G
```

---

### STEP 4: Application Configuration Updates

#### `application-dev.properties`
```properties
# No changes needed - already working
```

#### `application-docker.properties`
**Add observability endpoints:**
```properties
# Prometheus metrics
management.endpoints.web.exposure.include=health,info,metrics,prometheus

# Zipkin tracing
management.zipkin.tracing.endpoint=http://zipkin:9411/api/v2/spans
management.tracing.sampling.probability=1.0
```

#### Create `application-prod.properties`
```properties
server.port=8082

# Production database (external)
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}

# Redis with authentication
spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT:6379}
spring.data.redis.password=${REDIS_PASSWORD}

# External Keycloak
spring.security.oauth2.resourceserver.jwt.issuer-uri=${KEYCLOAK_ISSUER_URI}

# Flyway migrations
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Production JPA settings
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Actuator (restricted)
management.endpoints.web.exposure.include=health,prometheus
management.endpoint.health.show-details=never
```

---

### STEP 5: Flyway Database Migrations

#### Enable Flyway in `build.gradle` (already present)
```gradle
implementation 'org.flywaydb:flyway-core:10.10.0'
implementation 'org.flywaydb:flyway-database-postgresql:10.10.0'
```

#### Create Migration Structure
```
src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_indexes.sql
└── V3__seed_categories.sql
```

#### Configuration per environment:
- **Dev**: Flyway disabled (use `ddl-auto=create`)
- **Docker**: Flyway enabled (migrations on startup)
- **Prod**: Flyway enabled (controlled migrations)

---

### STEP 6: Prometheus Configuration

#### Create `prometheus/prometheus.yml`
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8082']
        labels:
          application: 'eshop-backend'
          environment: 'docker'

  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']

  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
```

---

### STEP 7: Environment Variables

#### Create `.env.example`
```bash
# Database
POSTGRES_DB=eshop_db
DATABASE_USERNAME=eshop
DATABASE_PASSWORD=change_me_in_production
DATABASE_URL=jdbc:postgresql://postgres:5432/eshop_db

# Redis
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=change_me_in_production

# Keycloak
KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/eshop
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=change_me_in_production

# Application
SPRING_PROFILES_ACTIVE=docker
JWT_SECRET=your-super-secret-jwt-key-min-256-bits

# Monitoring
GRAFANA_ADMIN_PASSWORD=admin

# Production Only
DOCKER_REGISTRY=your-registry.com
VERSION=1.0.0
CORS_ORIGINS=https://yourdomain.com
```

---

### STEP 8: Nginx Configuration (Production)

#### Create `nginx/nginx.conf`
```nginx
events {
    worker_connections 1024;
}

http {
    upstream backend {
        server app:8082;
    }

    server {
        listen 80;
        server_name _;
        
        # Redirect HTTP to HTTPS
        return 301 https://$host$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name yourdomain.com;

        ssl_certificate /etc/nginx/ssl/cert.pem;
        ssl_certificate_key /etc/nginx/ssl/key.pem;
        
        location / {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        location /actuator/health {
            proxy_pass http://backend/actuator/health;
            access_log off;
        }
    }
}
```

---

## Service Access URLs

### Development Environment
| Service | URL | Credentials |
|---------|-----|-------------|
| Spring Boot (Local) | http://localhost:8082 | N/A |
| PostgreSQL | localhost:5432 | postgres / thamilu*884* |
| pgAdmin | http://localhost:5050 | admin@eshop.com / admin |
| Redis | localhost:6379 | (no password) |
| Redis Commander | http://localhost:8081 | N/A |
| Keycloak | http://localhost:8080 | admin / admin |

### Docker Testing Environment
| Service | URL | Credentials |
|---------|-----|-------------|
| Spring Boot | http://localhost:8082 | N/A |
| PostgreSQL | localhost:5432 | eshop / eshop |
| Redis | localhost:6379 | (no password) |
| Keycloak | http://localhost:8080 | admin / admin |
| **Prometheus** | http://localhost:9090 | N/A |
| **Grafana** | http://localhost:3002 | admin / admin |
| **Zipkin** | http://localhost:9411 | N/A |

### Production Environment
| Service | URL | Credentials |
|---------|-----|-------------|
| Application | https://yourdomain.com | N/A |
| Grafana (Internal) | http://localhost:3002 | admin / ${GRAFANA_PASSWORD} |
| Prometheus (Internal) | http://localhost:9090 | N/A |

---

## Verification Steps

### After Dev Setup:
```powershell
docker compose -f docker-compose-dev.yml up -d
# Visit pgAdmin: http://localhost:5050
# Visit Redis Commander: http://localhost:8081
```

### After Docker Setup:
```powershell
docker compose build
docker compose up -d
docker logs eshop-backend
# Visit Grafana: http://localhost:3002
# Visit Prometheus: http://localhost:9090
# Visit Zipkin: http://localhost:9411
```

### After Production Setup:
```powershell
# Set environment variables
cp .env.example .env.prod
# Edit .env.prod with production secrets

# Deploy
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

---

## Summary of Changes

| File | Changes |
|------|---------|
| `docker-compose-dev.yml` | Upgraded versions, added pgAdmin + Redis Commander |
| `docker-compose.yml` | Added Prometheus, Grafana, Zipkin |
| `docker-compose.prod.yml` | Added Nginx, monitoring, security hardening |
| `application-docker.properties` | Added observability config |
| `application-prod.properties` | Created with production settings |
| `.env.example` | Created with all required variables |
| `prometheus/prometheus.yml` | Created Prometheus config |
| `nginx/nginx.conf` | Created Nginx reverse proxy config |

---

## Next Actions

After approval, I will:
1. ✅ Update all three docker-compose files
2. ✅ Create application-prod.properties
3. ✅ Create Prometheus configuration
4. ✅ Create Grafana dashboards
5. ✅ Create Nginx configuration
6. ✅ Create .env templates
7. ✅ Update application-docker.properties
8. ✅ Create startup guides for each environment
9. ✅ Test and verify each environment

This will give you a **production-ready, enterprise-grade Docker stack**! 🚀
