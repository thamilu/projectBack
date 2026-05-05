

# --- File: complete-docker-guide.md ---

# Complete Docker Guide for E-Shop Backend
## A Beginner-Friendly Guide to Your Production-Ready Setup

---

## 📚 Table of Contents

1. [Introduction - What is This All About?](#introduction)
2. [Understanding Docker - The Basics](#docker-basics)
3. [Understanding Each Service](#understanding-services)
4. [The Three Environments Explained](#three-environments)
5. [Complete Setup Guide](#setup-guide)
6. [Working with Your Setup Daily](#daily-workflow)
7. [Monitoring & Observability](#monitoring)
8. [Troubleshooting Common Issues](#troubleshooting)
9. [Best Practices](#best-practices)
10. [Glossary of Terms](#glossary)

---

<a name="introduction"></a>
## 📖 1. Introduction - What is This All About?

### What Problem Are We Solving?

Imagine you're building a house (your Spring Boot application). You need:
- **Plumbing** (Database - PostgreSQL)
- **Electricity** (Cache - Redis)
- **Security System** (Authentication - Keycloak)
- **Surveillance Cameras** (Monitoring - Prometheus, Grafana, Zipkin)
- **Tools** (Management - pgAdmin, Redis Commander)

**The Old Way:**
You'd install all these on your computer, configure them manually, and hope everything works when you move to production.

**The Docker Way:**
Everything runs in isolated "containers" that:
- ✅ Work the same on your laptop and production server
- ✅ Don't mess up your computer's setup
- ✅ Can be started/stopped with one command
- ✅ Can be deleted and recreated easily

### What You Have Now

You have **THREE complete setups** (environments):

| Environment | Purpose | Your App Runs | Services in Docker |
|-------------|---------|---------------|-------------------|
| **DEV** | Daily coding | On your computer | 8 containers |
| **DOCKER** | Testing everything | In Docker | 8 containers |
| **PRODUCTION** | Real users | In Docker | 6+ containers |

Each environment is **completely independent** - they won't interfere with each other!

---

<a name="docker-basics"></a>
## 🐳 2. Understanding Docker - The Basics

### What is Docker? (Simple Explanation)

Think of Docker like a **shipping container for software**:

**Without Docker:**
```
Your code needs:
- PostgreSQL 16
- Java 21
- Redis 7.4
- Specific configurations

Your friend's computer has:
- PostgreSQL 14 ❌
- Java 17 ❌
- Redis 6 ❌
- Different settings ❌

Result: "It works on my machine!" 😤
```

**With Docker:**
```
Your code + All dependencies packaged together
→ Works the same EVERYWHERE ✅
```

### Key Docker Concepts

#### 1. **Container** (The Running Service)
A container is like a **mini-computer running inside your computer**.

```
┌─────────────────────────────┐
│  Container: PostgreSQL      │
│  - Has its own file system  │
│  - Has its own network      │
│  - Isolated from your PC    │
│  - Lightweight (not a VM)   │
└─────────────────────────────┘
```

**Example:**
```powershell
# This starts a PostgreSQL container
docker run postgres:16-alpine

# It's like having a PostgreSQL server
# without installing anything on your PC!
```

#### 2. **Image** (The Blueprint)
An image is like a **recipe** to create a container.

```
Image: postgres:16-alpine
   ↓
Container: Running PostgreSQL database

(Like: Recipe → Cake)
```

#### 3. **Volume** (Persistent Storage)
Containers are temporary - when deleted, data is lost.
Volumes are like **external hard drives** that keep data safe.

```
Container (Temporary)  →  Volume (Permanent)
   PostgreSQL          →  Database files
   Redis               →  Cache data
   Prometheus          →  Metrics history
```

#### 4. **Network** (Container Communication)
Containers talk to each other through Docker networks.

```
Container A (Backend)
      ↓ network
Container B (PostgreSQL)
      ↓ network
Container C (Redis)
```

They use **service names** instead of IP addresses:
- `postgres` → PostgreSQL container
- `redis` → Redis container
- `keycloak` → Keycloak container

#### 5. **Docker Compose** (Orchestra Conductor)
Docker Compose manages **multiple containers** together.

```yaml
# docker-compose.yml is like a music sheet
services:
  postgres:    # Violin
  redis:       # Piano
  keycloak:    # Drums
  backend:     # Conductor

# One command starts all:
docker compose up
```

---

<a name="understanding-services"></a>
## 🛠️ 3. Understanding Each Service

Let's understand what each "container" does in your setup:

### 🗄️ PostgreSQL (Database)

**What it is:** A powerful database that stores all your data.

**What it stores:**
- User accounts
- Products
- Orders
- Reviews
- Everything your app needs to remember

**Why in Docker:**
- ✅ No need to install PostgreSQL on your PC
- ✅ Easy to reset database (just delete container)
- ✅ Same version everywhere (16-alpine)

**Real-world analogy:** Like a filing cabinet that stores all your business records.

**Container facts:**
- **Image:** `postgres:16-alpine`
- **Port:** `5432`
- **Data saved in:** Volume `postgres_data`

**Example usage:**
```powershell
# Connect to database
docker exec -it eshop-postgres psql -U eshop -d eshop_db

# View tables
\dt

# Run query
SELECT * FROM users;
```

---

### ⚡ Redis (Cache)

**What it is:** A super-fast in-memory database for caching.

**What it does:**
- Stores frequently accessed data in RAM
- Makes your app **100x faster** for repeated requests
- Stores temporary data (sessions, tokens)

**Example:**
```
User requests product list
1st time: Get from PostgreSQL (slow, 100ms)
2nd time: Get from Redis (fast, 1ms) ⚡
```

**Why in Docker:**
- ✅ Easy to start/stop
- ✅ Easy to clear cache
- ✅ Latest version (7.4-alpine)

**Real-world analogy:** Like keeping sticky notes on your desk instead of looking through filing cabinets.

**Container facts:**
- **Image:** `redis:7.4-alpine`
- **Port:** `6379`
- **Data saved in:** Volume `redis_data`

**Example usage:**
```powershell
# Connect to Redis
docker exec -it eshop-redis redis-cli

# See all keys
KEYS *

# Get a value
GET product:123

# Clear all cache
FLUSHALL
```

---

### 🔐 Keycloak (Authentication & Authorization)

**What it is:** A complete user management and authentication system.

**What it does:**
- User login/logout
- Password management
- Social login (Google, Facebook)
- JWT token generation
- Role-based access control

**Example flow:**
```
User enters username/password
    ↓
Keycloak verifies credentials
    ↓
Keycloak generates JWT token
    ↓
Your app uses token to verify user
```

**Why Keycloak (instead of coding it yourself):**
- ✅ Battle-tested security
- ✅ Saves months of development
- ✅ Industry standard
- ✅ Handles complex scenarios

**Real-world analogy:** Like a security guard at a building entrance with a master key system.

**Container facts:**
- **Image:** `quay.io/keycloak/keycloak:26.5.2`
- **Port:** `8080`
- **Admin UI:** http://localhost:8080
- **Login:** admin / admin

---

### 📊 Prometheus (Metrics Collection)

**What it is:** A monitoring system that collects metrics from your app.

**What it collects:**
- How many requests per second
- Response times (fast/slow)
- Error rates
- Memory usage
- CPU usage
- Database connection counts

**How it works:**
```
Your Spring Boot app exposes metrics
    ↓
Prometheus scrapes (pulls) metrics every 15 seconds
    ↓
Stores time-series data
    ↓
You can query historical data
```

**Why you need it:**
- 🐛 Detect problems before users complain
- 📈 See trends over time
- ⚡ Find slow endpoints
- 💾 Monitor resource usage

**Real-world analogy:** Like a heart rate monitor that tracks your health 24/7.

**Container facts:**
- **Image:** `prom/prometheus:latest`
- **Port:** `9090`
- **UI:** http://localhost:9090
- **Config:** `prometheus/prometheus.yml`

**Example queries:**
```promql
# Request rate
rate(http_server_requests_seconds_count[1m])

# Memory usage
jvm_memory_used_bytes

# Error rate
rate(http_server_requests_seconds_count{status="500"}[1m])
```

---

### 📈 Grafana (Visualization & Dashboards)

**What it is:** A beautiful dashboard tool that visualizes Prometheus data.

**What it does:**
- Creates graphs and charts
- Shows real-time metrics
- Creates custom dashboards
- Sends alerts (email, Slack)

**Why use Grafana:**
- Prometheus data is raw numbers
- Grafana makes it **beautiful and understandable**

**Example:**
```
Prometheus: "http_server_requests_seconds_count = 12547"
          ↓
Grafana: [Beautiful graph showing requests over time]
```

**Real-world analogy:** Prometheus is the thermometer, Grafana is the colorful weather app.

**Container facts:**
- **Image:** `grafana/grafana:latest`
- **Port:** `3002`
- **UI:** http://localhost:3002
- **Login:** admin / admin

**What you'll see:**
- 📊 Request rates over time
- ⏱️ Response time percentiles (p50, p95, p99)
- 💾 Memory and CPU usage
- 🗄️ Database connection pool status

---

### 🔍 Zipkin (Distributed Tracing)

**What it is:** A tool that traces requests through your entire application.

**What it shows:**
```
User Request: GET /api/orders/123
    ↓ 5ms - Controller
    ↓ 50ms - Service Layer
    ↓ 100ms - Database Query ← SLOW! 🐌
    ↓ 10ms - Cache Check
Total: 165ms
```

**Why you need it:**
- 🐛 Find exactly **where** time is spent
- 🔍 Debug slow requests
- 📊 See call chains
- ⚡ Optimize bottlenecks

**Example scenario:**
```
User complains: "Checkout is slow!"
    ↓
Check Zipkin trace:
  - Payment API: 50ms ✅
  - Database query: 5000ms ❌ ← FOUND IT!
  - Fix: Add database index
  - Result: Now 50ms ✅
```

**Real-world analogy:** Like GPS tracking showing exactly where you spent time during a road trip.

**Container facts:**
- **Image:** `openzipkin/zipkin:latest`
- **Port:** `9411`
- **UI:** http://localhost:9411

---

### 🖥️ pgAdmin (Database Management Tool)

**What it is:** A graphical interface for PostgreSQL.

**What you can do:**
- ✅ Browse tables visually
- ✅ Run SQL queries
- ✅ View data without code
- ✅ Design database schema
- ✅ Backup/restore databases

**Why it's helpful:**
```
Without pgAdmin:
docker exec -it postgres psql -U eshop -d eshop_db
SELECT * FROM users WHERE email = 'test@example.com';

With pgAdmin:
[Beautiful table view with click to filter] 🖱️
```

**Real-world analogy:** Command line = driving manual, pgAdmin = driving automatic.

**Container facts:**
- **Image:** `dpage/pgadmin4:latest`
- **Port:** `5050`
- **UI:** http://localhost:5050
- **Login:** admin@eshop.com / admin

---

### 🔴 Redis Commander (Redis Browser)

**What it is:** A graphical interface for Redis.

**What you can do:**
- ✅ Browse all cached keys
- ✅ View cached values
- ✅ Delete specific keys
- ✅ See key expiration times
- ✅ Monitor Redis in real-time

**Why it's helpful:**
```
Debug cache issue:
"Why is old data showing?"
    ↓
Open Redis Commander
    ↓
See key: product:123 (expires in 5 minutes)
    ↓
Delete key manually
    ↓
Problem solved!
```

**Real-world analogy:** Like viewing your browser's cookies visually instead of reading raw cookie files.

**Container facts:**
- **Image:** `rediscommander/redis-commander:latest`
- **Port:** `8081`
- **UI:** http://localhost:8081

---

### 🌐 Nginx (Reverse Proxy - Production Only)

**What it is:** A web server that sits in front of your app.

**What it does:**
```
Internet → Nginx → Your Spring Boot App
```

**Why use it:**
- ✅ SSL/HTTPS termination (secure connections)
- ✅ Load balancing (multiple app instances)
- ✅ Static file serving
- ✅ Security hardening
- ✅ Rate limiting

**Example:**
```
User: https://yourdomain.com/api/products
    ↓
Nginx: Check SSL certificate ✅
    ↓
Nginx: Proxy to → Backend:8082
    ↓
Backend: Process request
    ↓
Nginx: Send response back
```

**Real-world analogy:** Like a receptionist who handles visitors before they meet you.

**Container facts:**
- **Image:** `nginx:alpine`
- **Ports:** `80` (HTTP), `443` (HTTPS)
- **Config:** `nginx/nginx.conf`

---

<a name="three-environments"></a>
## 🏗️ 4. The Three Environments Explained

### Why Three Environments?

Think of it like building a car:
1. **Dev (Workshop):** Where you build and test parts
2. **Docker (Test Track):** Where you test the complete car
3. **Production (Highway):** Where real people drive

---

### 🔧 Environment 1: DEV (Development)

**Purpose:** Fast daily development

**How it works:**
```
┌─────────────────────────┐
│  YOUR COMPUTER          │
│                         │
│  IntelliJ/VS Code       │
│  Spring Boot App        │
│  Port: 8082             │
│  [You can edit code     │
│   and see changes       │
│   immediately]          │
└─────────────────────────┘
         ↓ connects to
┌─────────────────────────┐
│  DOCKER                 │
│                         │
│  8 Containers:          │
│  • PostgreSQL           │
│  • Redis                │
│  • Keycloak             │
│  • pgAdmin              │
│  • Redis Commander      │
│  • Prometheus           │
│  • Grafana              │
│  • Zipkin               │
└─────────────────────────┘
```

**Advantages:**
✅ **Instant reload** - Change code → Save → See results immediately
✅ **Debugging** - Set breakpoints in your IDE
✅ **Fast** - No need to rebuild Docker images
✅ **Monitoring** - Still have full observability

**When to use:**
- Daily coding
- Writing new features
- Quick debugging
- Testing locally

**File:** `docker-compose-dev.yml`

---

### 🐳 Environment 2: DOCKER (Docker Testing)

**Purpose:** Test the complete Dockerized stack

**How it works:**
```
┌─────────────────────────────────────┐
│  DOCKER                             │
│                                     │
│  All 8 Containers Running:          │
│  ┌─────────────────────────────┐   │
│  │  Spring Boot (Java 21)      │   │
│  │  Your app in Docker         │   │
│  └─────────────────────────────┘   │
│         ↓                           │
│  ┌─────────────────────────────┐   │
│  │  PostgreSQL + Redis +       │   │
│  │  Keycloak                   │   │
│  └─────────────────────────────┘   │
│         ↓                           │
│  ┌─────────────────────────────┐   │
│  │  Prometheus + Grafana +     │   │
│  │  Zipkin                     │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

**Advantages:**
✅ **Realistic** - Exactly like production
✅ **Java 21 verified** - Test correct Java version
✅ **Container testing** - Test Docker build process
✅ **CI/CD ready** - Use in automated testing

**When to use:**
- Before deploying to production
- Testing container configuration
- Verifying Java 21 setup
- Running automated tests
- CI/CD pipeline

**File:** `docker-compose.yml`

---

### 🚀 Environment 3: PRODUCTION

**Purpose:** Serve real users

**How it works:**
```
           ┌──────────────┐
           │   Internet   │
           └──────────────┘
                  ↓
           ┌──────────────┐
           │   Nginx      │
           │   SSL/HTTPS  │
           └──────────────┘
                  ↓
┌──────────────────────────────────────┐
│  DOCKER (Internal Network)           │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  Spring Boot App (Private)     │ │
│  └────────────────────────────────┘ │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  PostgreSQL (Private)          │ │
│  └────────────────────────────────┘ │
│                                      │
│  ┌────────────────────────────────┐ │
│  │  Monitoring (127.0.0.1 only)   │ │
│  │  • Prometheus                  │ │
│  │  • Grafana                     │ │
│  │  • Zipkin                      │ │
│  └────────────────────────────────┘ │
└──────────────────────────────────────┘
```

**Security features:**
✅ **SSL/HTTPS** - Encrypted traffic
✅ **Private network** - Services not exposed to internet
✅ **Resource limits** - Prevent resource exhaustion
✅ **Restricted endpoints** - Only essential endpoints public
✅ **Secrets management** - Passwords in environment variables

**When to use:**
- Serving real users
- Production deployment

**File:** `docker-compose.prod.yml`

---

### Quick Comparison

| Feature | DEV | DOCKER | PRODUCTION |
|---------|-----|--------|------------|
| **App Location** | Your PC | Docker | Docker |
| **Speed** | ⚡ Instant reload | 🐢 Must rebuild | 🐢 Must rebuild |
| **Debugging** | ✅ Full IDE support | ❌ Limited | ❌ Limited |
| **Realistic** | ⚠️ Mostly | ✅ Very | ✅ 100% |
| **Containers** | 8 | 8 | 6+ |
| **Dev Tools** | ✅ pgAdmin, Redis Cmd | ❌ | ❌ |
| **Monitoring** | ✅ All tools | ✅ All tools | ✅ Production only |
| **SSL/HTTPS** | ❌ | ❌ | ✅ |
| **Public Access** | ❌ localhost only | ❌ localhost only | ✅ Internet |

---

<a name="setup-guide"></a>
## 🚀 5. Complete Setup Guide

### Prerequisites

Before starting, make sure you have:

1. **Docker Desktop Installed**
   ```powershell
   # Check if Docker is installed
   docker --version
   # Should show: Docker version 20.x.x or higher
   
   docker compose version
   # Should show: Docker Compose version v2.x.x
   ```

2. **Java 21 Installed (for local dev)**
   ```powershell
   java -version
   # Should show: openjdk version "21.0.x"
   ```

3. **Gradle (included in project)**
   ```powershell
   cd G:\Project\eshop_back
   .\gradlew --version
   ```

---

### 📋 Setup 1: DEV Environment (Recommended for Daily Work)

**Step 1: Start Docker Services**

```powershell
# Navigate to project
cd G:\Project\eshop_back

# Start all 8 containers
docker compose -f docker-compose-dev.yml up -d

# Output you'll see:
# Creating network "eshop-dev-network"
# Creating eshop-postgres-dev ... done
# Creating eshop-redis-dev ... done
# Creating eshop-keycloak-dev ... done
# Creating eshop-pgadmin-dev ... done
# Creating eshop-redis-commander-dev ... done
# Creating eshop-prometheus-dev ... done
# Creating eshop-grafana-dev ... done
# Creating eshop-zipkin-dev ... done
```

**Step 2: Verify All Containers Are Running**

```powershell
docker ps

# You should see 8 containers:
# ✅ eshop-postgres-dev
# ✅ eshop-redis-dev
# ✅ eshop-keycloak-dev
# ✅ eshop-pgadmin-dev
# ✅ eshop-redis-commander-dev
# ✅ eshop-prometheus-dev
# ✅ eshop-grafana-dev
# ✅ eshop-zipkin-dev
```

**Step 3: Check Service Health**

```powershell
# Check logs (if you see errors)
docker compose -f docker-compose-dev.yml logs

# Check specific service
docker logs eshop-postgres-dev
docker logs eshop-keycloak-dev
```

**Step 4: Access Services**

Open these URLs in your browser:

| Service | URL | Expected Result |
|---------|-----|-----------------|
| pgAdmin | http://localhost:5050 | Login page |
| Redis Commander | http://localhost:8081 | Redis browser |
| Prometheus | http://localhost:9090 | Prometheus UI |
| Grafana | http://localhost:3002 | Grafana login |
| Zipkin | http://localhost:9411 | Zipkin UI |
| Keycloak | http://localhost:8080 | Keycloak admin |

**Step 5: Configure pgAdmin (First Time Only)**

1. Open http://localhost:5050
2. Login: `admin@eshop.com` / `admin`
3. Right-click "Servers" → Create → Server
4. **General Tab:**
   - Name: `Eshop Dev`
5. **Connection Tab:**
   - Host: `localhost` (or `postgres` if running in Docker)
   - Port: `5432`
   - Database: `eshop_Dev`
   - Username: `postgres`
   - Password: `thamilu*884*`
6. Click **Save**

**Step 6: Run Your Spring Boot App Locally**

```powershell
# Option 1: Using Gradle
.\gradlew bootRun

# Option 2: Using IntelliJ
# 1. Open EshopApplication.java
# 2. Right-click → Run
# 3. Make sure profile is set to 'dev'

# Option 3: Using VS Code
# 1. Open Spring Boot Dashboard
# 2. Click Run/Debug on your app
```

**Step 7: Verify Your App is Running**

```powershell
# Check health endpoint
curl http://localhost:8082/actuator/health

# Expected response:
# {"status":"UP"}

# Check Swagger UI
# Open: http://localhost:8082/swagger-ui.html
```

**Step 8: Verify Monitoring is Working**

1. **Prometheus:**
   - Open http://localhost:9090
   - Go to **Status** → **Targets**
   - `spring-boot-local` should show **UP** (green)

2. **Grafana:**
   - Open http://localhost:3002
   - Login: admin / admin
   - Click **Explore**
   - Select **Prometheus** datasource
   - Run query: `up`
   - Should see result = 1

3. **Zipkin:**
   - Make API request: `curl http://localhost:8082/api/products`
   - Open http://localhost:9411
   - Click **Run Query**
   - Should see your trace

**Step 9: Import Grafana Dashboard**

1. Open http://localhost:3002
2. Click **+** (left sidebar) → **Import**
3. Enter Dashboard ID: `6756`
4. Click **Load**
5. Select datasource: **Prometheus**
6. Click **Import**
7. You now have a beautiful Spring Boot dashboard! 🎉

**✅ Dev Environment Complete!**

Now you can:
- Edit code and see changes instantly
- Use pgAdmin to browse database
- Use Redis Commander to check cache
- See metrics in Grafana in real-time
- Trace requests in Zipkin

---

### 📋 Setup 2: DOCKER Environment (Full Stack Testing)

**Step 1: Stop Dev Environment (if running)**

```powershell
docker compose -f docker-compose-dev.yml down
```

**Step 2: Build Docker Image**

```powershell
# Build your Spring Boot app into a Docker image
docker compose build

# This will:
# 1. Use Gradle 8.14 with JDK 21 to build
# 2. Create JAR file
# 3. Use Java 21 JRE for runtime
# 4. Create optimized Docker image

# You'll see output like:
# [+] Building 120.5s (15/15) FINISHED
```

**Step 3: Start All Services**

```powershell
docker compose up -d

# This starts all 8 containers including your app
```

**Step 4: Verify All Containers**

```powershell
docker ps

# You should see:
# ✅ eshop-backend (YOUR APP - NEW!)
# ✅ eshop-postgres
# ✅ eshop-redis
# ✅ eshop-keycloak
# ✅ eshop-prometheus
# ✅ eshop-grafana
# ✅ eshop-zipkin
```

**Step 5: CRITICAL - Verify Java 21**

```powershell
# Check Java version inside container
docker exec -it eshop-backend java -version

# Expected output:
# openjdk version "21.0.5" 2024-10-15 LTS
# OpenJDK Runtime Environment Temurin-21+35 (build 21.0.5+11-LTS)
# OpenJDK 64-Bit Server VM Temurin-21+35 (build 21.0.5+11-LTS, mixed mode)

# If you see version 21.x.x ← SUCCESS! ✅
```

**Step 6: Check Application Logs**

```powershell
# Watch backend logs
docker logs eshop-backend -f

# Look for:
# "Started EshopApplication in X.XXX seconds"
# This means app started successfully ✅

# Press Ctrl+C to stop watching logs
```

**Step 7: Test the Application**

```powershell
# Health check
curl http://localhost:8082/actuator/health

# Expected: {"status":"UP"}

# Test API endpoint
curl http://localhost:8082/api/products

# Open Swagger
# http://localhost:8082/swagger-ui.html
```

**Step 8: Access All Services**

| Service | URL |
|---------|-----|
| **Backend** | http://localhost:8082 |
| **Swagger** | http://localhost:8082/swagger-ui.html |
| **Prometheus** | http://localhost:9090 |
| **Grafana** (3002)| http://localhost:3002 |
| **Zipkin** | http://localhost:9411 |
| Keycloak | http://localhost:8080 |

**Step 9: Verify Monitoring**

1. **Prometheus Targets:**
   - http://localhost:9090/targets
   - `spring-boot-eshop` should be **UP**

2.| **Grafana** | 3002 | Dashboards |:**
   - Import dashboard ID: `6756`
   - Should show metrics from Dockerized app

3. **Zipkin Traces:**
   - Make requests to your API
   - View traces at http://localhost:9411

**Step 10: Stop Everything**

```powershell
# Stop all containers
docker compose down

# Or stop and remove volumes (DELETES DATA!)
docker compose down -v
```

**✅ Docker Environment Complete!**

---

### 📋 Setup 3: PRODUCTION Environment

> ⚠️ **Warning:** This is for production deployment. Test thoroughly in Docker environment first!

**Prerequisites:**
- ✅ Domain name configured
- ✅ SSL certificates obtained
- ✅ Production server with Docker installed
- ✅ Environment variables configured

**Step 1: Prepare SSL Certificates**

```powershell
# Create ssl directory
mkdir nginx\ssl

# Add your SSL certificates
# - nginx/ssl/cert.pem (certificate)
# - nginx/ssl/key.pem (private key)

# For testing, create self-signed certificate:
# (Don't use in real production!)
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/key.pem \
  -out nginx/ssl/cert.pem
```

**Step 2: Create Production Environment File**

```powershell
# Copy example
cp .env.example .env.prod

# Edit with production values
notepad .env.prod
```

**Critical variables to set:**
```bash
# Database
DATABASE_URL=jdbc:postgresql://postgres:5432/eshop_prod
DATABASE_USERNAME=eshop_prod_user
DATABASE_PASSWORD=your-secure-password-here

# Redis (use external managed service recommended)
REDIS_HOST=your-redis-server.com
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# Keycloak (use external managed service)
KEYCLOAK_ISSUER_URI=https://auth.yourdomain.com/realms/eshop
KEYCLOAK_JWK_URI=https://auth.yourdomain.com/realms/eshop/protocol/openid-connect/certs

# Security
JWT_SECRET=your-256-bit-secret-key-must-be-very-long-and-random
CORS_ORIGINS=https://yourdomain.com,https://www.yourdomain.com

# Payment (if using)
STRIPE_SECRET_KEY=sk_live_your_stripe_key
RAZORPAY_KEY_ID=rzp_live_your_key_id
RAZORPAY_KEY_SECRET=your_razorpay_secret

# Monitoring
GRAFANA_ADMIN_PASSWORD=your-secure-grafana-password
```

**Step 3: Update Nginx Configuration**

Edit `nginx/nginx.conf`:
```nginx
# Change this line:
server_name yourdomain.com;
# To your actual domain:
server_name api.yourdomain.com;
```

**Step 4: Build Production Image**

```powershell
# Build with production optimizations
docker compose -f docker-compose.prod.yml build

# Tag for registry (if using)
docker tag eshop-backend:latest your-registry.azurecr.io/eshop-backend:1.0.0
```

**Step 5: Deploy to Production**

```powershell
# Start production stack
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Verify all services started
docker ps
```

**Step 6: Verify Production Deployment**

```powershell
# Check health (from server)
curl http://localhost:8082/actuator/health

# Check through Nginx (public)
curl https://yourdomain.com/actuator/health

# Check SSL
curl -I https://yourdomain.com
```

**Step 7: Set Up Monitoring Access**

```powershell
# From your local machine, create SSH tunnel:
ssh -L 3002:localhost:3002 user@your-production-server
ssh -L 9090:localhost:9090 user@your-production-server

# Now access from your local browser:
# http://localhost:3002 (Grafana)
# http://localhost:9090 (Prometheus)
```

**Step 8: Set Up Backups**

```powershell
# Create backup script
# backups/backup.sh

#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
docker exec eshop-postgres-prod pg_dump -U eshop_prod_user eshop_prod > /backups/backup_$DATE.sql

# Add to crontab:
# 0 2 * * * /path/to/backup.sh
```

**✅ Production Environment Complete!**

---

<a name="daily-workflow"></a>
## 💼 6. Working with Your Setup Daily

### Morning Routine (Starting Work)

```powershell
# 1. Navigate to project
cd G:\Project\eshop_back

# 2. Start dev services
docker compose -f docker-compose-dev.yml up -d

# 3. Open monitoring tools (optional but recommended)
start http://localhost:3002     # Grafana
start http://localhost:9090     # Prometheus
start http://localhost:9411     # Zipkin
start http://localhost:5050     # pgAdmin

# 4. Start your app
.\gradlew bootRun

# 5. Start coding! 🚀
```

### During Development

**Scenario 1: You Changed Code**
```
1. Save file (Ctrl+S)
2. Spring Boot auto-reloads (devtools)
3. See changes immediately
4. Watch metrics update in Grafana
```

**Scenario 2: Database Changes**
```
1. Write migration script
2. Restart app
3. Check pgAdmin to verify:
   - Open http://localhost:5050
   - Navigate to table
   - Verify changes
```

**Scenario 3: Cache Issues**
```
1. Open Redis Commander: http://localhost:8081
2. Find problematic key
3. Delete key
4. Test again
```

**Scenario 4: Performance Problem**
```
1. Make request to slow endpoint
2. Open Zipkin: http://localhost:9411
3. Find the trace
4. See where time is spent
5. Optimize that part
6. Test again
7. See improvement in Zipkin
```

### Evening Routine (End of Day)

```powershell
# Stop your Spring Boot app
# (Ctrl+C in terminal or stop in IDE)

# Optional: Keep containers running for tomorrow
# (They use minimal resources)

# Or stop everything:
docker compose -f docker-compose-dev.yml down

# Keep data? Don't use -v
# Delete data? Use -v
docker compose -f docker-compose-dev.yml down -v
```

---

### Testing Before Production

**Before deploying to production, always:**

```powershell
# 1. Test in Docker environment
docker compose down -v              # Clean state
docker compose build                # Rebuild
docker compose up -d                # Start
docker logs eshop-backend -f        # Watch logs

# 2. Verify Java 21
docker exec -it eshop-backend java -version

# 3. Run tests
docker exec -it eshop-backend ./gradlew test

# 4. Check metrics
# Open http://localhost:9090/targets
# Verify all targets UP

# 5. Load test (optional)
# Use tools like Apache Bench or k6

# 6. If all good → Deploy to production
docker compose down
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

---

<a name="monitoring"></a>
## 📊 7. Monitoring & Observability

### Understanding the Monitoring Stack

```
Your Application
      ↓ exposes /actuator/prometheus
Prometheus (scrapes every 15s)
      ↓ stores time-series data
Grafana (queries Prometheus)
      ↓ displays beautiful graphs
You (see what's happening)
```

### Important Metrics to Watch

#### 1. **Request Rate**
Shows how many requests per second

```promql
# Prometheus query
rate(http_server_requests_seconds_count[1m])

# What to watch:
# Sudden spike → Traffic increase or attack
# Gradual increase → Growing user base
# Drop to zero → App is down!
```

#### 2. **Response Time (Latency)**
Shows how fast your app responds

```promql
# 95th percentile response time
histogram_quantile(0.95, 
  rate(http_server_requests_seconds_bucket[1m])
)

# What it means:
# 95% of requests complete in X seconds
# If X > 1 second → investigate slow endpoints

# 50th percentile (median)
histogram_quantile(0.50, 
  rate(http_server_requests_seconds_bucket[1m])
)
```

#### 3. **Error Rate**
Shows how many requests are failing

```promql
# 5xx errors (server errors)
rate(http_server_requests_seconds_count{status=~"5.."}[1m])

# 4xx errors (client errors)
rate(http_server_requests_seconds_count{status=~"4.."}[1m])

# What to watch:
# Any 5xx errors → critical bugs!
# Many 401/403 → authentication issues
```

#### 4. **JVM Memory**
Shows memory usage

```promql
# Heap memory used
jvm_memory_used_bytes{area="heap"}

# What to watch:
# Constantly growing → memory leak
# Sudden spike → large operation
# Near max → about to crash
```

#### 5. **Database Connections**
Shows connection pool usage

```promql
# Active connections
hikaricp_connections_active

# Pending connections (waiting)
hikaricp_connections_pending

# What to watch:
# Always at max → increase pool size
# Many pending → database is slow
```

#### 6. **Cache Hit Rate**
Shows how effective your cache is

```promql
# Redis operations
rate(cache_gets_total[1m])

# What to watch:
# Hit rate should be > 80%
# Low hit rate → cache not effective
```

### Setting Up Alerts in Grafana

1. Open dashboard panel
2. Click **Edit**
3. Go to **Alert** tab
4. Click **Create Alert**
5. Set conditions:
   ```
   Example: Alert if error rate > 10/min
   WHEN avg() OF query (5xx errors)
   IS ABOVE 10
   FOR 5m
   ```
6. Add notification channel (email, Slack)
7. Save

### Using Zipkin for Debugging

**Example: Finding a Slow Endpoint**

1. User reports: "Product page is slow"
2. Open Zipkin: http://localhost:9411
3. Search for service: `eshop-backend`
4. Look for long duration traces
5. Click on a slow trace
6. You see:
   ```
   Total: 5000ms
   ├─ Controller: 5ms
   ├─ Service: 10ms
   └─ Repository.findProduct: 4985ms ← PROBLEM!
   ```
7. Check database query
8. Add index or optimize query
9. Test again
10. See improvement: 5000ms → 50ms ✅

---

<a name="troubleshooting"></a>
## 🐛 8. Troubleshooting Common Issues

### Issue 1: Container Won't Start

**Symptoms:**
```powershell
docker ps
# Container not in list
```

**Diagnosis:**
```powershell
# Check logs
docker logs eshop-postgres

# Common errors:
# - Port already in use
# - Volume permission error
# - Configuration error
```

**Solutions:**

**Problem: Port already in use**
```powershell
# Find process using port 5432
netstat -ano | findstr :5432

# Output:
# TCP  0.0.0.0:5432  0.0.0.0:0  LISTENING  12345
#                                          ^^^^^
#                                          PID

# Kill the process
taskkill /PID 12345 /F

# Or change port in docker-compose
ports:
  - "15432:5432"  # Use different external port
```

**Problem: Container keeps restarting**
```powershell
# Check logs for error
docker logs eshop-backend --tail 100

# Common causes:
# - Database not ready
# - Wrong environment variables
# - Missing dependency
```

---

### Issue 2: Can't Connect to Database

**Symptoms:**
```
Application error:
Connection refused: localhost:5432
```

**Solutions:**

**Check 1: Is PostgreSQL running?**
```powershell
docker ps | findstr postgres
# Should see: eshop-postgres

# If not running:
docker compose -f docker-compose-dev.yml up -d postgres
```

**Check 2: Correct connection string?**
```properties
# In application-dev.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/eshop_Dev
# ↑ Should be localhost when app runs locally

# If app in Docker:
spring.datasource.url=jdbc:postgresql://postgres:5432/eshop_db
# ↑ Use service name
```

**Check 3: Test connection manually**
```powershell
# Connect to PostgreSQL
docker exec -it eshop-postgres psql -U postgres -d eshop_Dev

# If successful, you'll see:
# eshop_Dev=#

# Try query:
SELECT 1;
# Should work

# Exit:
\q
```

---

### Issue 3: Prometheus Shows "DOWN"

**Symptoms:**
http://localhost:9090/targets shows red "DOWN"

**Solutions:**

**Check 1: Is your app exposing metrics?**
```powershell
# Test actuator endpoint
curl http://localhost:8082/actuator/prometheus

# Should see metrics like:
# jvm_memory_used_bytes...
# http_server_requests_seconds_count...
```

**Check 2: Prometheus configuration**
```yaml
# prometheus/prometheus-dev.yml should have:
- targets: ['host.docker.internal:8082']
# NOT localhost:8082 (won't work from Docker)
```

**Check 3: Restart Prometheus**
```powershell
docker compose -f docker-compose-dev.yml restart prometheus

# Check logs
docker logs eshop-prometheus-dev
```

---

### Issue 4: Out of Memory Errors

**Symptoms:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solutions:**

**Increase JVM memory:**

```dockerfile
# In Dockerfile, update JAVA_OPTS:
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Xms512m \
    -Xmx2048m"
```

Or in docker-compose:
```yaml
backend:
  environment:
    JAVA_OPTS: "-Xms1g -Xmx2g"
```

**Monitor memory usage:**
```powershell
# Watch container resources
docker stats eshop-backend

# Check Grafana dashboard
# Look for JVM memory usage growing
```

---

### Issue 5: Keycloak Won't Start

**Symptoms:**
```powershell
docker logs eshop-keycloak
# Error: Failed to connect to database
```

**Solutions:**

**Check 1: PostgreSQL ready?**
```powershell
# Keycloak needs PostgreSQL
# docker-compose should have:
depends_on:
  postgres:
    condition: service_healthy
```

**Check 2: Database exists?**
```powershell
# Connect to PostgreSQL
docker exec -it eshop-postgres psql -U postgres

# Create database if missing:
CREATE DATABASE eshop_Dev;
```

**Check 3: Restart with logs**
```powershell
docker compose -f docker-compose-dev.yml restart keycloak
docker logs eshop-keycloak -f
```

---

### Issue 6: Slow Performance

**Diagnosis Process:**

**Step 1: Check Grafana**
```
1. Open http://localhost:3002
2. Look at Spring Boot dashboard
3. Check:
   - Request rate (sudden spike?)
   - Response time (increasing?)
   - Error rate (errors slowing down?)
```

**Step 2: Check Zipkin**
```
1. Open http://localhost:9411
2. Find slow traces
3. Identify bottleneck (usually database)
```

**Step 3: Check Database**
```powershell
# Slow queries in PostgreSQL
docker exec -it eshop-postgres psql -U postgres -d eshop_Dev

# Enable slow query log:
ALTER SYSTEM SET log_min_duration_statement = 1000;
-- Log queries taking > 1 second

# Reload config:
SELECT pg_reload_conf();

# Check logs:
docker logs eshop-postgres | findstr duration
```

**Step 4: Check Redis**
```powershell
# Redis should be fast
# Check hit rate:
docker exec -it eshop-redis redis-cli INFO stats

# Look for:
# keyspace_hits
# keyspace_misses
# Hit rate should be > 80%
```

---

### Issue 7: "Works Locally, Fails in Docker"

**Common causes:**

**1. Different environment variables**
```yaml
# Check environment section in docker-compose.yml
environment:
  SPRING_PROFILES_ACTIVE: docker  # Not 'dev'!
```

**2. Service names vs localhost**
```properties
# Local (dev):
spring.datasource.url=jdbc:postgresql://localhost:5432/...

# Docker:
spring.datasource.url=jdbc:postgresql://postgres:5432/...
#                                       ^^^^^^^^
#                                       Service name!
```

**3. Volumes not mounted**
```yaml
# Make sure volumes are defined:
volumes:
  - ./uploads:/var/eshop/uploads
```

---

<a name="best-practices"></a>
## ✨ 9. Best Practices

### Development Workflow

#### ✅ DO:
- **Use dev environment for daily coding**
  - Faster iteration
  - Better debugging
  - Instant reload

- **Keep monitoring tools open**
  - Catch issues early
  - See performance impact immediately

- **Test in Docker before production**
  - Verify containerization works
  - Test with correct Java version

- **Use meaningful commit messages**
  ```bash
  ❌ git commit -m "fix"
  ✅ git commit -m "fix: slow product query by adding index"
  ```

- **Monitor metrics during development**
  - Check Grafana after big changes
  - Verify no performance regression

#### ❌ DON'T:
- **Don't commit secrets**
  ```bash
  # Add to .gitignore:
  .env.dev
  .env.docker
  .env.prod
  ```

- **Don't use `latest` tag in production**
  ```yaml
  ❌ image: postgres:latest
  ✅ image: postgres:16-alpine
  ```

- **Don't ignore monitoring**
  - Check dashboards regularly
  - Set up alerts

- **Don't skip testing in Docker**
  - Always test before production deploy

---

### Database Best Practices

#### Use Migrations (Flyway)

**Why:** Track database changes like code changes

```sql
-- V1__initial_schema.sql
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT NOW()
);

-- V2__add_user_roles.sql
ALTER TABLE users ADD COLUMN role VARCHAR(50);
```

#### Regular Backups

```bash
# Automated backup script
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
docker exec eshop-postgres pg_dump -U eshop eshop_db > backup_$DATE.sql

# Schedule with cron:
# Run daily at 2 AM
0 2 * * * /path/to/backup.sh
```

#### Connection Pooling

```properties
# Adjust based on load
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

---

### Caching Best Practices

#### Cache Strategically

```java
// ✅ Cache expensive operations
@Cacheable("products")
public Product findById(Long id) {
    return repository.findById(id);
}

// ❌ Don't cache everything
@Cacheable("user-balance")  // This changes too often!
public BigDecimal getUserBalance(Long userId) {
    return calculateBalance(userId);
}
```

#### Set Appropriate TTL

```properties
# Short TTL for frequently changing data
spring.cache.redis.time-to-live=300000  # 5 minutes

# Longer TTL for static data
# product-images: 1 hour
# configuration: 24 hours
```

#### Monitor Cache Hit Rate

```promql
# In Prometheus
cache_gets_total{result="hit"} / cache_gets_total
# Should be > 0.8 (80% hit rate)
```

---

### Security Best Practices

#### Never Commit Secrets

```bash
# .gitignore
.env
.env.dev
.env.docker
.env.prod
*.pem
*.key
application-secret.properties
```

#### Use Environment Variables

```yaml
# ✅ Good
environment:
  DATABASE_PASSWORD: ${DATABASE_PASSWORD}

# ❌ Bad
environment:
  DATABASE_PASSWORD: mypassword123
```

#### Rotate Secrets Regularly

```bash
# Production JWT secret should be:
# - At least 256 bits
# - Cryptographically random
# - Changed every 90 days

# Generate strong secret:
openssl rand -base64 64
```

#### Limit Exposed Endpoints

```yaml
# Production - restrict actuator
management.endpoints.web.exposure.include=health,prometheus

# Development - more endpoints OK
management.endpoints.web.exposure.include=health,info,metrics,prometheus,caches,env
```

---

### Monitoring Best Practices

#### Set Up Alerts

**Critical alerts:**
- App is down (health check fails)
- Error rate > 1% for 5 minutes
- Memory usage > 90%
- Disk space < 10%

**Warning alerts:**
- Response time p95 > 1 second
- Database connection pool > 80% used
- Cache hit rate < 70%

#### Create Meaningful Dashboards

```
Dashboard: Application Health
├─ Request Rate (requests/sec)
├─ Response Time (p50, p95, p99)
├─ Error Rate (%)
├─ JVM Memory (heap used/max)
├─ Database Connections (active/max)
└─ Cache Hit Rate (%)
```

#### Review Metrics Regularly

- **Daily:** Quick check of dashboards
- **Weekly:** Review trends, identify issues
- **Monthly:** Analyze patterns, plan optimizations

---

### Resource Management

#### Set Resource Limits

```yaml
# docker-compose.prod.yml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
        reservations:
          cpus: '1.0'
          memory: 1G
```

#### Monitor Resource Usage

```powershell
# Real-time monitoring
docker stats

# Check limits
docker inspect eshop-backend | grep -A 10 Resources
```

---

<a name="glossary"></a>
## 📖 10. Glossary of Terms

### Docker Terms

**Container**
> A lightweight, standalone package that includes everything needed to run software: code, runtime, system tools, libraries, and settings.
> 
> *Example:* PostgreSQL container has database software + data + configuration

**Image**
> A template or blueprint for creating containers. Like a recipe.
> 
> *Example:* `postgres:16-alpine` is an image, `eshop-postgres` is a container created from it

**Volume**
> Persistent storage for containers. Data survives container deletion.
> 
> *Example:* `postgres_data` volume stores your database even if container is removed

**Network**
> Allows containers to communicate with each other.
> 
> *Example:* `eshop-network` lets backend talk to PostgreSQL

**Docker Compose**
> Tool for defining and running multi-container applications using YAML files.
> 
> *Example:* `docker-compose.yml` defines 8 containers that work together

### Application Terms

**Backend**
> Your Spring Boot application - the server that handles business logic and APIs.

**Frontend**
> Web or mobile app that users interact with (not in this setup).

**API (Application Programming Interface)**
> Endpoints that frontend calls to get/send data.
> 
> *Example:* `GET /api/products` returns list of products

**Endpoint**
> A specific URL in your API.
> 
> *Example:* `/api/users/123`, `/api/orders`

**CRUD**
> Create, Read, Update, Delete - basic database operations.

### Database Terms

**PostgreSQL**
> A powerful relational database. Stores data in tables with rows and columns.

**Database Migration**
> Version-controlled changes to database schema.
> 
> *Example:* V1__create_users_table.sql

**Connection Pool**
> Pre-created database connections ready to use (faster than creating new connections).
> 
> *Setting:* `maximum-pool-size=20` means 20 concurrent connections

**Index**
> Special database structure that makes queries faster.
> 
> *Example:* Index on `email` column makes `WHERE email=?` fast

### Caching Terms

**Redis**
> In-memory data store used for caching (super fast).

**Cache Hit**
> Data found in cache (fast! 1-5ms).

**Cache Miss**
> Data not in cache, must get from database (slower, 10-100ms).

**TTL (Time To Live)**
> How long cached data stays before expiring.
> 
> *Example:* TTL=300 means cached for 5 minutes

### Monitoring Terms

**Prometheus**
> Time-series database for metrics. Collects and stores measurements over time.

**Metric**
> A measurement of something.
> 
> *Examples:* Request count, response time, memory usage

**Scraping**
> Prometheus pulling metrics from your app every 15 seconds.

**Time Series**
> Data points measured at successive time intervals.
> 
> *Example:* Memory usage every 15 seconds for past 7 days

**Grafana**
> Visualization tool that creates beautiful dashboards from Prometheus data.

**Dashboard**
> Collection of graphs/charts showing metrics.

**Alert**
> Notification when metric crosses threshold.
> 
> *Example:* Send email if error rate > 10/min

**Zipkin**
> Distributed tracing system - shows request flow through your app.

**Trace**
> One complete request through your system.
> 
> *Example:* User clicks "Buy" → trace shows all steps

**Span**
> One step in a trace.
> 
> *Example:* Database query span, cache check span

### Authentication Terms

**Keycloak**
> Identity and access management system (handles logins).

**JWT (JSON Web Token)**
> Secure token that proves user identity.
> 
> *Flow:* User logs in → Keycloak gives JWT → App verifies JWT

**OAuth2**
> Industry-standard protocol for authorization.

**Realm**
> A Keycloak space for users/apps (like a tenant).
> 
> *Example:* "eshop" realm for your e-commerce app

### Performance Terms

**Latency**
> Time between request and response.
> 
> *Example:* API responds in 50ms = low latency (good!)

**Throughput**
> Number of requests handled per second.
> 
> *Example:* 1000 requests/sec

**Percentile**
> Statistical measure.
> 
> *p50 (median):* 50% of requests are faster
> *p95:* 95% of requests are faster (only 5% slower)
> *p99:* 99% of requests are faster (worst case)

**Bottleneck**
> Slowest part limiting overall performance.
> 
> *Example:* Slow database query is bottleneck

### Network Terms

**Port**
> Virtual door number for services.
> 
> *Example:* PostgreSQL uses port 5432

**Localhost**
> Your own computer (127.0.0.1).

**Host**
> Computer/server running services.

**Reverse Proxy**
> Server that sits in front of your app (Nginx).
> 
> *Benefits:* SSL, load balancing, security

**SSL/TLS**
> Encryption for secure connections (HTTPS).

**CORS (Cross-Origin Resource Sharing)**
> Security feature allowing/blocking requests from different domains.

### Spring Boot Terms

**Actuator**
> Spring Boot module that exposes operational endpoints.
> 
> *Examples:* `/actuator/health`, `/actuator/prometheus`

**Profile**
> Different configurations for different environments.
> 
> *Profiles:* dev, docker, prod

**Auto-configuration**
> Spring Boot automatically configures based on dependencies.

**Bean**
> Object managed by Spring framework.

**JPA (Java Persistence API)**
> Standard for database access in Java.

**Hibernate**
> Implementation of JPA (ORM - Object Relational Mapping).

### DevOps Terms

**CI/CD**
> Continuous Integration / Continuous Deployment - automated testing and deployment.

**Environment**
> Complete setup for running an application.
> 
> *Types:* Development, Testing, Production

**Infrastructure as Code**
> Managing infrastructure (servers, networks) using code/config files.
> 
> *Example:* docker-compose.yml is infrastructure as code

**Observability**
> Ability to understand system state by examining outputs (metrics, logs, traces).

---

## 🎓 Learning Resources

### Docker
- **Official Docs:** https://docs.docker.com/
- **Docker Compose:** https://docs.docker.com/compose/
- **Best Practices:** https://docs.docker.com/develop/dev-best-practices/

### Spring Boot
- **Spring Boot Docs:** https://spring.io/projects/spring-boot
- **Actuator Guide:** https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html

### Monitoring
- **Prometheus Docs:** https://prometheus.io/docs/
- **Grafana Tutorials:** https://grafana.com/tutorials/
- **Zipkin Quickstart:** https://zipkin.io/pages/quickstart

### PostgreSQL
- **PostgreSQL Tutorial:** https://www.postgresqltutorial.com/
- **Performance Tips:** https://wiki.postgresql.org/wiki/Performance_Optimization

### Redis
- **Redis University:** https://university.redis.com/
- **Redis Commands:** https://redis.io/commands

---

## 🎉 Conclusion

You now have a **production-grade development environment** with:

✅ **Three complete setups** (Dev, Docker, Production)
✅ **Full observability** (Prometheus, Grafana, Zipkin)
✅ **Developer tools** (pgAdmin, Redis Commander)
✅ **Modern stack** (Java 21, Spring Boot 4, PostgreSQL 16, Redis 7.4)
✅ **Best practices** (Security, monitoring, resource management)

### What Makes This Setup Special

1. **Fast Development**
   - Edit code → See changes immediately
   - Full monitoring while coding
   - Catch issues before they reach production

2. **Production-Ready**
   - Test in exact production environment
   - SSL/HTTPS support
   - Resource limits and security hardening

3. **Beginner-Friendly**
   - Everything works with one command
   - No complex installation
   - Easy to reset and start fresh

### Next Steps

1. **Start Small**
   ```powershell
   docker compose -f docker-compose-dev.yml up -d
   .\gradlew bootRun
   ```

2. **Explore Tools**
   - Open Grafana → See your app in real-time
   - Make requests → Watch traces in Zipkin
   - Query metrics in Prometheus

3. **Build Features**
   - Write code with confidence
   - Monitor performance
   - Test thoroughly

4. **Deploy**
   - Test in Docker environment
   - Deploy to production
   - Monitor in production

### Remember

- 🐛 **Bugs happen** - Use Zipkin to find them
- 📊 **Performance matters** - Watch Grafana
- 🔒 **Security first** - Never commit secrets
- 📚 **Keep learning** - Tech evolves constantly

### Get Help

- Check **Troubleshooting** section
- Review **Glossary** for terms
- Use **Docker logs** to debug
- Ask for help when stuck!

---

**Happy Coding! 🚀**

*You're now ready to build amazing things with a rock-solid development environment!*


# --- File: dev-observability-guide.md ---

# Dev Environment with Observability - Quick Start

## 🎯 What You Have Now

Your **dev environment** now includes the **same observability tools** as Docker testing:

- ✅ **Prometheus** (9090) - Collects metrics from your local app
- ✅ **Grafana** (3002) - Visualizes metrics in real-time
- ✅ **Zipkin** (9411) - Traces requests through your app
- ✅ **pgAdmin** (5050) - Manage PostgreSQL
- ✅ **Redis Commander** (8081) - Browse Redis keys

**Total: 8 containers** supporting your locally running Spring Boot app!

---

## 🚀 How to Use

### Step 1: Start All Dev Services

```powershell
cd G:\Project\eshop_back

# Start all 8 containers
docker compose -f docker-compose-dev.yml up -d

# Verify all containers are running
docker ps
```

**Expected containers:**
- eshop-postgres-dev
- eshop-redis-dev
- eshop-keycloak-dev
- eshop-pgadmin-dev
- eshop-redis-commander-dev
- **eshop-prometheus-dev** 🆕
- **eshop-grafana-dev** 🆕
- **eshop-zipkin-dev** 🆕

### Step 2: Run Your Spring Boot App Locally

```powershell
# From your IDE or terminal
./gradlew bootRun

# Or run from IntelliJ/VS Code
# Profile: dev
```

**Your app starts on:** `http://localhost:8082`

### Step 3: Watch Metrics in Real-Time! 🔥

#### Prometheus (Metrics Collection)
1. Open: http://localhost:9090
2. Go to **Status** → **Targets**
3. You should see `spring-boot-local` status: **UP** ✅
4. Try queries:
   ```promql
   # HTTP requests per second
   rate(http_server_requests_seconds_count[1m])
   
   # JVM memory usage
   jvm_memory_used_bytes
   
   # Active database connections
   hikaricp_connections_active
   
   # Request duration (95th percentile)
   histogram_quantile(0.95, http_server_requests_seconds_bucket)
   ```

#### Grafana (Beautiful Dashboards)
1. Open: http://localhost:3002
2. Login: `admin` / `admin`
3. **Import Spring Boot Dashboard:**
   - Click **+** → **Import Dashboard**
   - Enter ID: **6756** (Spring Boot 2.1 Statistics)
   - Select **Prometheus** datasource
   - Click **Import**
4. **Watch your app in real-time!** 📊

#### Zipkin (Distributed Tracing)
1. Open: http://localhost:9411
2. Make some API requests to your app
3. Click **Run Query**
4. Click on a trace to see:
   - Request path through your app
   - Time spent in each layer
   - Database queries
   - Redis cache hits/misses

---

## 💡 Benefits: Catch Errors While Coding

### 1. **Instant Feedback on Performance**
```
You write code → Save → App reloads → See metrics in Grafana
```

### 2. **Detect Slow Queries Immediately**
- Grafana dashboard shows slow DB queries
- Zipkin shows which endpoint is slow
- Prometheus shows query duration

### 3. **Monitor Memory Issues**
- See JVM memory usage in real-time
- Detect memory leaks early
- Monitor garbage collection

### 4. **Track Error Rates**
```promql
# Error rate per endpoint
rate(http_server_requests_seconds_count{status="500"}[1m])
```

### 5. **Trace Complex Flows**
- User registration → Email → Database → Cache
- See exactly where time is spent
- Debug slow operations

---

## 📊 Monitoring Your Local App

### Common Scenarios

#### Scenario 1: Testing a New API Endpoint
```
1. Write your endpoint code
2. Start app with ./gradlew bootRun
3. Make request: curl http://localhost:8082/api/products
4. Watch in Zipkin: See the complete trace
5. Check Prometheus: See request count and duration
6. View in Grafana: See dashboard update in real-time
```

#### Scenario 2: Database Performance Issue
```
1. Notice slow response time in browser
2. Open Zipkin → Find the slow trace
3. See database query took 2 seconds
4. Open pgAdmin → Analyze the query
5. Add index in your migration
6. Restart app → See improvement immediately
```

#### Scenario 3: Memory Leak Detection
```
1. Open Grafana dashboard
2. Watch JVM memory usage over time
3. If it keeps growing → memory leak
4. Use Prometheus query to identify:
   jvm_memory_used_bytes{area="heap"}
5. Fix the leak → Watch memory stabilize
```

---

## 🎯 Service Access URLs

### Your App (Running Locally)
| Service | URL | Purpose |
|---------|-----|---------|
| Spring Boot API | http://localhost:8082 | Your application |
| Swagger UI | http://localhost:8082/swagger-ui.html | API documentation |
| Actuator Health | http://localhost:8082/actuator/health | Health check |
| **Prometheus Metrics** | http://localhost:8082/actuator/prometheus | Raw metrics |

### Observability Tools (In Docker)
| Service | URL | Credentials |
|---------|-----|-------------|
| **Prometheus** | http://localhost:9090 | None |
| **Grafana** | http://localhost:3002 | admin / admin |
| **Zipkin** | http://localhost:9411 | None |

### Dev Tools
| Service | URL | Credentials |
|---------|-----|-------------|
| pgAdmin | http://localhost:5050 | admin@eshop.com / admin |
| Redis Commander | http://localhost:8081 | None |
| Keycloak | http://localhost:8080 | admin / admin |

---

## 🔧 Troubleshooting

### Issue: Prometheus shows "DOWN" for spring-boot-local

**Solution:**
```powershell
# 1. Verify your app is running
curl http://localhost:8082/actuator/prometheus

# 2. Check if actuator is exposed
# In application-dev.properties:
management.endpoints.web.exposure.include=health,info,metrics,prometheus

# 3. Restart Prometheus
docker compose -f docker-compose-dev.yml restart prometheus
```

### Issue: No traces in Zipkin

**Solution:**
```powershell
# 1. Verify Zipkin URL in application-dev.properties
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans
management.tracing.sampling.probability=1.0

# 2. Make sure you have the dependency
# build.gradle should have:
implementation 'io.micrometer:micrometer-tracing-bridge-brave'
runtimeOnly 'io.zipkin.reporter2:zipkin-reporter-brave'

# 3. Make an API request to generate a trace
curl http://localhost:8082/api/products
```

### Issue: Grafana dashboard empty

**Solution:**
```
1. Check Prometheus datasource:
   - Settings → Data Sources → Prometheus
   - URL should be: http://prometheus:9090
   - Click "Save & Test"

2. Verify data in Prometheus first
   - http://localhost:9090
   - Run query: up
   - Should show spring-boot-local target

3. Refresh Grafana dashboard
```

---

## 📈 Grafana Dashboard Examples

### Import These Dashboards

1. **Spring Boot 2.1 System Monitor** - ID: `6756`
   - JVM metrics, HTTP requests, DB connections
   
2. **JVM (Micrometer)** - ID: `4701`
   - Detailed JVM statistics
   
3. **Spring Boot Statistics** - ID: `12900`
   - Comprehensive Spring Boot metrics

### How **Step 2: Open monitoring tools**
```
1. Open Grafana: http://localhost:3002
2. Click "+" → Import
3. Enter dashboard ID (e.g., 6756)
4. Select "Prometheus" datasource
5. Click "Import"
```

---

## 🎉 Example Workflow

### Daily Development with Monitoring

```powershell
# Morning: Start dev environment
docker compose -f docker-compose-dev.yml up -d

# Open monitoring tools
start http://localhost:3002     # Grafana
start http://localhost:9090     # Prometheus
start http://localhost:9411     # Zipkin

# Run your app
./gradlew bootRun

# Code → Save → See metrics update in real-time! 🚀

# Evening: Stop services
docker compose -f docker-compose-dev.yml down
```

---

## 🚀 Pro Tips

### 1. **Keep Grafana Open While Coding**
- Second monitor? Put Grafana there
- Watch metrics change as you code
- Catch performance issues immediately

### 2. **Use Zipkin for Debugging**
- API call slow? Check Zipkin first
- See exact breakdown of time spent
- Identify bottlenecks instantly

### 3. **Create Custom Dashboards**
- Track metrics specific to your features
- Monitor business logic performance
- Set up alerts for errors

### 4. **Save Prometheus Queries**
```promql
# Save useful queries as dashboard panels
rate(http_server_requests_seconds_count{uri="/api/products"}[1m])
```

---

## 📋 Summary

### Before (Old Dev Setup)
- 5 containers (PostgreSQL, Redis, Keycloak, pgAdmin, Redis Commander)
- Run app → Hope it works → Deploy → Find problems ❌

### After (New Dev Setup)  
- **8 containers** (+ Prometheus, Grafana, Zipkin)
- Run app → **Watch metrics** → **Catch issues immediately** → Deploy with confidence ✅

**You now have production-grade monitoring in your dev environment!** 🎯

---

## 🔗 Next Steps

1. **Start exploring**: Try the workflow above
2. **Import dashboards**: Add the Spring Boot dashboards
3. **Create alerts**: Set up Grafana alerts for errors
4. **Monitor daily**: Make it part of your workflow

**Happy coding with real-time observability!** 🚀


# --- File: docker-cheat-sheet.md ---

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


# --- File: ROLE_MANAGEMENT.md ---

# Role Management Guide

## Table of Contents
1. [Overview](#overview)
2. [Role Hierarchy](#role-hierarchy)
3. [Role Assignment Rules](#role-assignment-rules)
4. [Configuration](#configuration)
5. [User Journeys](#user-journeys)
6. [API Reference](#api-reference)
7. [Troubleshooting](#troubleshooting)

---

## Overview

The e-commerce application uses **Keycloak** for centralized user management and role-based access control (RBAC). All authentication, user registration, and role management are handled through Keycloak.

### Authentication Architecture

```
┌─────────────┐         ┌──────────────┐         ┌─────────────────┐
│   Frontend  │ ◄────► │   Keycloak   │ ◄────► │  Backend (API)  │
│  (Next.js)  │  OAuth2 │              │  JWT   │   (Spring Boot) │
└─────────────┘         └──────────────┘         └─────────────────┘
                               │
                               │ User DB
                               ▼
                        ┌──────────────┐
                        │  PostgreSQL  │
                        └──────────────┘
```

### Key Principles

- ✅ **Single Source of Truth**: Keycloak manages all users and roles
- ✅ **No Backend Registration**: Registration happens only in Keycloak
- ✅ **Role-Based Access**: All endpoints use `@PreAuthorize` annotations
- ✅ **Automatic Role Assignment**: CUSTOMER role assigned during registration
- ✅ **Admin Approval Required**: SELLER and DELIVERY_AGENT roles require approval

---

## Role Hierarchy

### Available Roles

| Role | Keycloak Name | Backend Constant | Auto-Assigned | Approval Required |
|------|---------------|------------------|---------------|-------------------|
| Customer | `Customer` | `Roles.CUSTOMER` | ✅ Yes | ❌ No |
| Seller | `Seller` | `Roles.SELLER` | ❌ No | ✅ Yes (Admin) |
| Delivery Agent | `DELIVERY_AGENT` | `Roles.DELIVERY_AGENT` | ❌ No | ✅ Yes (Admin) |
| Administrator | `ADMIN` | `Roles.ADMIN` | ❌ No | Manual |

### Role Capabilities

#### CUSTOMER Role
**Permissions:**
- Browse products and categories
- Add items to cart
- Place orders
- View order history
- Update profile
- Apply to become seller/delivery agent

**Endpoints:**
- `GET /api/v1/products/**`
- `POST /api/v1/cart/**`
- `POST /api/v1/orders/**`
- `GET /api/v1/me`

#### SELLER Role
**Permissions:**
- All CUSTOMER permissions
- Create/manage products
- Manage shop/store
- View seller dashboard
- Process orders

**Endpoints:**
- `POST /api/v1/sellers/register`
- `GET /api/v1/sellers/profile`
- `PUT /api/v1/sellers/profile`
- `GET /api/v1/dashboard/seller/**`
- `POST /api/v1/products`

#### DELIVERY_AGENT Role
**Permissions:**
- All CUSTOMER permissions
- View delivery assignments
- Update delivery status
- Manage delivery routes

**Endpoints:**
- `POST /api/v1/delivery/register`
- `GET /api/v1/delivery/assignments`
- `PUT /api/v1/delivery/status`

#### ADMIN Role
**Permissions:**
- All system permissions
- Approve/reject sellers
- Approve/reject delivery agents
- Manage users
- System configuration

**Endpoints:**
- `GET /api/v1/admin/approvals/**`
- `POST /api/v1/admin/approvals/**`
- `GET /api/v1/admin/users`

---

## Role Assignment Rules

### Automatic Assignment (CUSTOMER)

**When**: During user registration in Keycloak

**How**: Keycloak realm configured with `defaultRoles: ["Customer"]`

**Process:**
```
User submits registration form
    ↓
Keycloak creates user
    ↓
Keycloak auto-assigns "Customer" role (via default roles)
    ↓
User can immediately login and access customer endpoints
```

**Fallback**: If Keycloak default role fails, backend assigns CUSTOMER role on first login (JIT sync)

### Manual Assignment (SELLER)

**When**: After admin approval of seller application

**How**: Admin approves via API, backend assigns role using KeycloakService

**Process:**
```
Customer applies to become seller
    ↓
POST /api/v1/sellers/register
    ↓
SellerProfile created with status: PENDING
    ↓
Admin reviews application
    ↓
GET /api/v1/admin/approvals/sellers
    ↓
Admin approves
    ↓
POST /api/v1/admin/approvals/sellers/{id}/APPROVE
    ↓
SellerService.approveSeller() executes:
  - Updates status to ACTIVE
  - Calls KeycloakService.assignRoleByUsername(username, "SELLER")
    ↓
User now has both CUSTOMER and SELLER roles
```

### Manual Assignment (DELIVERY_AGENT)

**When**: After admin approval of delivery agent application

**Process**: Similar to SELLER role assignment

---

## Configuration

### Keycloak Realm Configuration

**File**: `realm-export.json`

```json
{
  "realm": "eshop",
  "enabled": true,
  "registrationAllowed": true,
  "registrationEmailAsUsername": false,
  "editUsernameAllowed": false,
  "resetPasswordAllowed": true,
  "defaultRoles": ["Customer"],
  "roles": {
    "realm": [
      {
        "name": "Customer",
        "description": "Customer role"
      },
      {
        "name": "Seller",
        "description": "Seller role"
      },
      {
        "name": "DELIVERY_AGENT",
        "description": "Delivery Agent role"
      }
    ]
  }
}
```

### Backend Configuration

**File**: `src/main/java/com/eshop/app/constants/Roles.java`

```java
public final class Roles {
    public static final String ADMIN = "ADMIN";
    public static final String SELLER = "SELLER";
    public static final String CUSTOMER = "CUSTOMER";
    public static final String DELIVERY_AGENT = "DELIVERY_AGENT";
}
```

### Security Configuration

**File**: `src/main/java/com/eshop/app/config/OAuth2SecurityConfig.java`

Example endpoint protection:
```java
@PreAuthorize("hasRole('CUSTOMER')")
public ResponseEntity<?> getCart() { ... }

@PreAuthorize("hasRole('SELLER')")
public ResponseEntity<?> createProduct() { ... }

@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> approveSeller() { ... }
```

---

## User Journeys

### Journey 1: New Customer Registration

```
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: User Registration (Keycloak)                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
User navigates to: http://localhost:8080/realms/eshop/account
User clicks "Register"
User fills form:
  - Username: john_doe
  - Email: john@example.com
  - Password: ********
  - First Name: John
  - Last Name: Doe
User submits registration
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 2: Keycloak Auto-Assigns CUSTOMER Role                    │
└─────────────────────────────────────────────────────────────────┘
                              ↓
Keycloak creates user account
Keycloak assigns "Customer" role automatically (defaultRoles)
User receives confirmation email (if configured)
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 3: User Logs In                                           │
└─────────────────────────────────────────────────────────────────┘
                              ↓
User navigates to frontend (e.g., http://localhost:3000)
User enters credentials and logs in
Keycloak redirects with authorization code
Frontend exchanges code for JWT tokens
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 4: Access Customer Features                               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
Frontend calls: GET /api/v1/me
Response includes: "roles": ["CUSTOMER"]
User can now:
  - Browse products
  - Add to cart
  - Place orders
  - View order history
```

### Journey 2: Customer Becomes Seller

```
┌─────────────────────────────────────────────────────────────────┐
│ Step 1: Customer Applies to Become Seller                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
Customer (logged in) navigates to "Become a Seller"
Customer fills seller registration form:
  - Business Name
  - Business Type (FARMER/WHOLESALER/RETAILER)
  - Contact Details
  - Tax ID (if applicable)
  - Bank Details
  - Accepts Terms & Conditions
                              ↓
Frontend calls: POST /api/v1/sellers/register
Request body:
{
  "displayName": "Green Valley Farm",
  "businessTypes": ["FARMER"],
  "email": "contact@greenvalley.com",
  "phone": "+919876543210",
  "acceptedTerms": true,
  ...
}
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 2: Backend Creates Seller Profile (PENDING)               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
SellerService.registerSeller() executes:
  - Validates terms acceptance
  - Creates SellerProfile entity
  - Sets status: PENDING
  - Saves to database
  
Response:
{
  "status": "success",
  "data": {
    "id": 1,
    "status": "PENDING",
    "displayName": "Green Valley Farm",
    ...
  }
}
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 3: Admin Reviews Application                              │
└─────────────────────────────────────────────────────────────────┘
                              ↓
Admin logs in with ADMIN role
Admin calls: GET /api/v1/admin/approvals/sellers
Response shows pending applications:
[
  {
    "id": 1,
    "displayName": "Green Valley Farm",
    "status": "PENDING",
    "userId": 123,
    ...
  }
]
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 4: Admin Approves Seller                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
Admin reviews application details
Admin decides to approve
Admin calls: POST /api/v1/admin/approvals/sellers/1/APPROVE
                              ↓
SellerService.approveSeller() executes:
  1. Updates SellerProfile:
     - status = ACTIVE
     - approvedBy = "admin"
     - approvedAt = current timestamp
  2. Assigns SELLER role in Keycloak:
     - Calls KeycloakService.assignRoleByUsername(username, "SELLER")
     - Keycloak adds "Seller" role to user
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│ Step 5: User Now Has SELLER Role                               │
└─────────────────────────────────────────────────────────────────┘
                              ↓
User logs in again (or refreshes token)
JWT now includes: "roles": ["CUSTOMER", "SELLER"]
User can now access:
  - Seller dashboard
  - Product management
  - Order management
  - Shop settings
```

### Journey 3: Admin Rejects Seller Application

```
Admin calls: POST /api/v1/admin/approvals/sellers/1/REJECT

SellerService.rejectSeller() executes:
  1. Updates SellerProfile:
     - status = REJECTED
     - rejectedBy = "admin"
     - rejectedAt = current timestamp
     - rejectionReason = "Incomplete documentation"
  2. Does NOT assign SELLER role

User remains with CUSTOMER role only
User can re-apply after addressing rejection reasons
```

---

## API Reference

### Customer Endpoints

#### Get Current User Info
```http
GET /api/v1/me
Authorization: Bearer {jwt-token}

Response:
{
  "sub": "uuid-here",
  "username": "john_doe",
  "email": "john@example.com",
  "roles": ["CUSTOMER"],
  ...
}
```

### Seller Endpoints

#### Register as Seller
```http
POST /api/v1/sellers/register
Authorization: Bearer {customer-jwt-token}
Content-Type: application/json

{
  "identityType": "INDIVIDUAL",
  "businessTypes": ["FARMER"],
  "displayName": "Green Valley Farm",
  "businessName": "Green Valley Organic Farms Pvt Ltd",
  "email": "contact@greenvalley.com",
  "phone": "+919876543210",
  "description": "Organic vegetables and fruits",
  "acceptedTerms": true,
  "aadhar": "123456789012",
  "farmLocationVillage": "Pune",
  "landArea": "10 acres"
}

Response (201 Created):
{
  "status": "success",
  "message": "Seller profile registered successfully",
  "data": {
    "id": 1,
    "userId": 123,
    "status": "PENDING",
    "displayName": "Green Valley Farm",
    ...
  }
}
```

#### Get Seller Profile
```http
GET /api/v1/sellers/profile
Authorization: Bearer {seller-jwt-token}

Response:
{
  "id": 1,
  "userId": 123,
  "status": "ACTIVE",
  "displayName": "Green Valley Farm",
  ...
}
```

### Admin Endpoints

#### Get Pending Sellers
```http
GET /api/v1/admin/approvals/sellers
Authorization: Bearer {admin-jwt-token}

Response:
[
  {
    "id": 1,
    "userId": 123,
    "status": "PENDING",
    "displayName": "Green Valley Farm",
    "createdAt": "2026-02-17T10:30:00Z",
    ...
  }
]
```

#### Approve Seller
```http
POST /api/v1/admin/approvals/sellers/1/APPROVE
Authorization: Bearer {admin-jwt-token}

Response (200 OK):
{
  "message": "Seller approved successfully"
}
```

#### Reject Seller
```http
POST /api/v1/admin/approvals/sellers/1/REJECT
Authorization: Bearer {admin-jwt-token}
Content-Type: application/json

{
  "reason": "Incomplete documentation"
}

Response (200 OK):
{
  "message": "Seller rejected"
}
```

---

## Troubleshooting

### Issue 1: New User Doesn't Have CUSTOMER Role

**Symptoms:**
- User can register and login
- API calls return 403 Forbidden
- JWT doesn't include CUSTOMER role

**Diagnosis:**
```bash
# Check user roles in Keycloak
curl http://localhost:8080/admin/realms/eshop/users/{userId}/role-mappings/realm \
  -H "Authorization: Bearer {admin-token}"
```

**Solutions:**

1. **Check Keycloak default roles configuration:**
   ```bash
   # Via API
   curl http://localhost:8080/admin/realms/eshop \
     -H "Authorization: Bearer {admin-token}" | jq '.defaultRoles'
   
   # Should return: ["Customer"]
   ```

2. **Via Keycloak Admin Console:**
   - Login: http://localhost:8080
   - Realm: eshop
   - Realm Settings → User Registration → Default Roles
   - Verify "Customer" is listed

3. **Manually assign role:**
   - Keycloak Admin Console
   - Users → Select user → Role Mappings
   - Add "Customer" role

4. **Backend safety net:**
   - The application has a fallback mechanism
   - User logs in → Backend checks for CUSTOMER role
   - If missing, assigns it automatically (see `SellerService.resolveUserId()`)

### Issue 2: Seller Application Auto-Approved (Should Be PENDING)

**Symptoms:**
- Seller application status shows ACTIVE immediately
- SELLER role assigned without admin approval

**Diagnosis:**
Check code in `SellerService.registerSeller()` around line 93-108

**Solution:**
Ensure auto-approval code is removed:
```java
// WRONG (old code):
approveSeller(saved.getId(), "SYSTEM (Auto-Approve)");

// CORRECT (new code):
return toResponse(saved);
```

### Issue 3: Admin Can't Approve Seller

**Symptoms:**
- Admin calls approve endpoint
- Returns error or seller stays PENDING
- SELLER role not assigned in Keycloak

**Diagnosis:**
1. Check logs for errors during role assignment
2. Verify user has `keycloakId` set in database
3. Check Keycloak service account permissions

**Solutions:**

1. **Check user's Keycloak ID:**
   ```sql
   SELECT id, username, email, keycloak_id FROM users WHERE id = 123;
   ```
   
   If `keycloak_id` is NULL, sync it:
   ```java
   // This happens automatically on next login
   // Or manually update via admin panel
   ```

2. **Verify Keycloak service account permissions:**
   - Keycloak Admin Console
   - Clients → eshop-backend → Service Account Roles
   - Ensure it has:
     - `manage-users`
     - `manage-realm`
     - `view-users`

3. **Check KeycloakService configuration:**
   ```properties
   # application.properties
   keycloak.auth-server-url=http://localhost:8080
   keycloak.realm=eshop
   keycloak.resource=eshop-backend
   keycloak.credentials.secret=your-client-secret
   ```

### Issue 4: JWT Doesn't Include Roles

**Symptoms:**
- User has roles in Keycloak
- JWT token doesn't include roles in claims
- Backend can't validate permissions

**Diagnosis:**
```bash
# Decode JWT token
echo "{jwt-token}" | cut -d. -f2 | base64 -d | jq
```

**Solution:**

1. **Check Keycloak client mappers:**
   - Keycloak Admin Console
   - Clients → eshop-client → Client Scopes → Mappers
   - Ensure "realm roles" mapper exists

2. **Add realm roles mapper** (if missing):
   - Type: User Realm Role
   - Token Claim Name: `realm_access.roles`
   - Add to ID token: ON
   - Add to access token: ON
   - Add to userinfo: ON

### Issue 5: Role Assignment Fails with "User Not Found"

**Symptoms:**
- Admin approves seller
- Error log: "User not found in Keycloak with username: xyz"
- Role not assigned

**Diagnosis:**
Username mismatch between database and Keycloak

**Solution:**

1. **Check username in database:**
   ```sql
   SELECT username, keycloak_id FROM users WHERE id = 123;
   ```

2. **Check username in Keycloak:**
   - Keycloak Admin Console → Users
   - Search for user

3. **Ensure exact match** (case-sensitive)
   - If mismatch, update database or Keycloak to match

---

## Best Practices

### Security

1. ✅ **Always use roles in endpoint security**
   ```java
   @PreAuthorize("hasRole('CUSTOMER')")
   ```

2. ✅ **Validate JWT on every request**
   - Spring Security handles this automatically

3. ✅ **Don't trust client-side role checks**
   - Always enforce on backend

4. ✅ **Use service accounts for Keycloak operations**
   - Don't use admin credentials in application

### Development

1. ✅ **Test role assignment in staging first**
2. ✅ **Monitor logs for role assignment failures**
3. ✅ **Keep Keycloak and backend in sync**
4. ✅ **Document any custom role logic**

### Production

1. ✅ **Back up Keycloak realm configuration**
2. ✅ **Monitor failed login attempts**
3. ✅ **Regular security audits**
4. ✅ **Keep Keycloak updated**

---

## Related Documentation

- [Keycloak Role Configuration](../KEYCLOAK_ROLE_CONFIGURATION.md)
- [Implementation Changes](../CHANGES_CUSTOMER_ROLE_FIX.md)
- [API Documentation](../../README.md)

---

## Appendix

### Role Matrix

| Endpoint | CUSTOMER | SELLER | DELIVERY_AGENT | ADMIN |
|----------|----------|--------|----------------|-------|
| GET /api/v1/products | ✅ | ✅ | ✅ | ✅ |
| POST /api/v1/cart | ✅ | ✅ | ✅ | ✅ |
| POST /api/v1/orders | ✅ | ✅ | ✅ | ✅ |
| POST /api/v1/sellers/register | ✅ | ✅ | ✅ | ✅ |
| POST /api/v1/products | ❌ | ✅ | ❌ | ✅ |
| GET /api/v1/dashboard/seller | ❌ | ✅ | ❌ | ✅ |
| POST /api/v1/delivery/register | ✅ | ✅ | ✅ | ✅ |
| PUT /api/v1/delivery/status | ❌ | ❌ | ✅ | ✅ |
| GET /api/v1/admin/approvals | ❌ | ❌ | ❌ | ✅ |
| POST /api/v1/admin/users | ❌ | ❌ | ❌ | ✅ |

---

**Document Version:** 1.0  
**Last Updated:** 2026-02-17  
**Author:** Development Team



# --- File: frontend-integration-guide.md ---

# Frontend Integration - Keycloak Authentication

Quick guide for frontend developers to integrate with the Keycloak-enabled backend API.

## 🚀 Base URL
```
Backend API: http://localhost:8082
Auth Endpoints: http://localhost:8082/api/auth
```

---

## 🔐 Authentication Methods

### Method 1: Username/Password Login (Recommended for Admin Panels)

```javascript
async function login(username, password) {
  const response = await fetch('http://localhost:8082/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ username, password })
  });
  
  if (!response.ok) {
    throw new Error('Login failed');
  }
  
  const tokens = await response.json();
  // Store tokens securely
  localStorage.setItem('access_token', tokens.access_token);
  localStorage.setItem('refresh_token', tokens.refresh_token);
  localStorage.setItem('token_expiry', Date.now() + (tokens.expires_in * 1000));
  
  return tokens;
}

// Usage
try {
  const tokens = await login('admin', 'admin123');
  console.log('Logged in successfully');
} catch (error) {
  console.error('Login failed:', error);
}
```

### Method 2: OAuth2 Flow (Recommended for Customer-Facing Apps)

```javascript
// Step 1: Redirect to Keycloak login
async function startOAuthLogin() {
  const redirectUri = `${window.location.origin}/callback`;
  const response = await fetch(
    `http://localhost:8082/api/auth/login-url?redirectUri=${redirectUri}`
  );
  
  const { authorizationUrl, state } = await response.json();
  
  // Save state for CSRF protection
  sessionStorage.setItem('oauth_state', state);
  
  // Redirect user to Keycloak
  window.location.href = authorizationUrl;
}

// Step 2: Handle callback (on /callback page)
async function handleOAuthCallback() {
  const params = new URLSearchParams(window.location.search);
  const code = params.get('code');
  const state = params.get('state');
  
  // Verify state
  const savedState = sessionStorage.getItem('oauth_state');
  if (state !== savedState) {
    throw new Error('Invalid state parameter');
  }
  
  // Exchange code for tokens
  const redirectUri = `${window.location.origin}/callback`;
  const response = await fetch(
    `http://localhost:8082/api/auth/callback?code=${code}&redirectUri=${redirectUri}`
  );
  
  if (!response.ok) {
    throw new Error('Callback failed');
  }
  
  const tokens = await response.json();
  
  // Store tokens
  localStorage.setItem('access_token', tokens.access_token);
  localStorage.setItem('refresh_token', tokens.refresh_token);
  
  // Clean up
  sessionStorage.removeItem('oauth_state');
  
  // Redirect to app
  window.location.href = '/dashboard';
}
```

---

## 🔄 Token Management

### Get Current User
```javascript
async function getCurrentUser() {
  const token = localStorage.getItem('access_token');
  
  const response = await fetch('http://localhost:8082/api/auth/me', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  
  if (!response.ok) {
    throw new Error('Failed to get user');
  }
  
  return await response.json();
}

// Response:
// {
//   "sub": "user-id",
//   "username": "admin",
//   "email": "admin@eshop.com",
//   "name": "Admin User",
//   "roles": ["ADMIN"],
//   "issuedAt": "2025-12-22T...",
//   "expiresAt": "2025-12-22T..."
// }
```

### Refresh Token
```javascript
async function refreshToken() {
  const refreshToken = localStorage.getItem('refresh_token');
  
  if (!refreshToken) {
    throw new Error('No refresh token');
  }
  
  const response = await fetch('http://localhost:8082/api/auth/refresh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ refreshToken })
  });
  
  if (!response.ok) {
    // Refresh failed, user needs to re-login
    localStorage.clear();
    window.location.href = '/login';
    return;
  }
  
  const tokens = await response.json();
  localStorage.setItem('access_token', tokens.access_token);
  localStorage.setItem('refresh_token', tokens.refresh_token);
  
  return tokens;
}
```

### Auto Token Refresh (Axios Interceptor)
```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8082/api'
});

// Request interceptor - add token and check expiry
api.interceptors.request.use(
  async (config) => {
    const token = localStorage.getItem('access_token');
    const expiry = localStorage.getItem('token_expiry');
    
    // Check if token is expired or about to expire (within 30 seconds)
    if (expiry && Date.now() > parseInt(expiry) - 30000) {
      try {
        await refreshToken();
        config.headers.Authorization = `Bearer ${localStorage.getItem('access_token')}`;
      } catch (error) {
        // Refresh failed, redirect to login
        window.location.href = '/login';
        return Promise.reject(error);
      }
    } else if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle 401 errors
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      try {
        await refreshToken();
        // Retry original request
        const config = error.config;
        config.headers.Authorization = `Bearer ${localStorage.getItem('access_token')}`;
        return axios(config);
      } catch (refreshError) {
        // Refresh failed, redirect to login
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    return Promise.reject(error);
  }
);

export default api;
```

---

## 🚪 Logout

```javascript
async function logout() {
  const refreshToken = localStorage.getItem('refresh_token');
  
  if (refreshToken) {
    try {
      await fetch('http://localhost:8082/api/auth/logout', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ refreshToken })
      });
    } catch (error) {
      console.error('Logout error:', error);
    }
  }
  
  // Clear local storage
  localStorage.removeItem('access_token');
  localStorage.removeItem('refresh_token');
  localStorage.removeItem('token_expiry');
  
  // Redirect to login
  window.location.href = '/login';
}
```

---

## 🛡️ Protected Route Component (React)

```jsx
import { Navigate } from 'react-router-dom';

function ProtectedRoute({ children, requiredRole }) {
  const token = localStorage.getItem('access_token');
  
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  
  // Optional: Check token expiry
  const expiry = localStorage.getItem('token_expiry');
  if (expiry && Date.now() > parseInt(expiry)) {
    localStorage.clear();
    return <Navigate to="/login" replace />;
  }
  
  // Optional: Check role (decode JWT)
  if (requiredRole) {
    const payload = JSON.parse(atob(token.split('.')[1]));
    const roles = payload.realm_access?.roles || [];
    
    if (!roles.includes(requiredRole)) {
      return <Navigate to="/unauthorized" replace />;
    }
  }
  
  return children;
}

// Usage
<Route path="/dashboard" element={
  <ProtectedRoute>
    <Dashboard />
  </ProtectedRoute>
} />

<Route path="/admin" element={
  <ProtectedRoute requiredRole="ADMIN">
    <AdminPanel />
  </ProtectedRoute>
} />
```

---

## 📦 Complete React Service

```javascript
// services/authService.js
class AuthService {
  constructor() {
    this.baseUrl = 'http://localhost:8082/api/auth';
  }
  
  async login(username, password) {
    const response = await fetch(`${this.baseUrl}/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    
    if (!response.ok) throw new Error('Login failed');
    
    const tokens = await response.json();
    this.saveTokens(tokens);
    return tokens;
  }
  
  async startOAuthLogin() {
    const redirectUri = `${window.location.origin}/callback`;
    const response = await fetch(
      `${this.baseUrl}/login-url?redirectUri=${redirectUri}`
    );
    
    const { authorizationUrl, state } = await response.json();
    sessionStorage.setItem('oauth_state', state);
    window.location.href = authorizationUrl;
  }
  
  async handleCallback(code, state) {
    const savedState = sessionStorage.getItem('oauth_state');
    if (state !== savedState) {
      throw new Error('Invalid state');
    }
    
    const redirectUri = `${window.location.origin}/callback`;
    const response = await fetch(
      `${this.baseUrl}/callback?code=${code}&redirectUri=${redirectUri}`
    );
    
    if (!response.ok) throw new Error('Callback failed');
    
    const tokens = await response.json();
    this.saveTokens(tokens);
    sessionStorage.removeItem('oauth_state');
    return tokens;
  }
  
  async getCurrentUser() {
    const token = this.getAccessToken();
    const response = await fetch(`${this.baseUrl}/me`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    
    if (!response.ok) throw new Error('Failed to get user');
    return await response.json();
  }
  
  async refreshToken() {
    const refreshToken = localStorage.getItem('refresh_token');
    const response = await fetch(`${this.baseUrl}/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });
    
    if (!response.ok) {
      this.logout();
      throw new Error('Token refresh failed');
    }
    
    const tokens = await response.json();
    this.saveTokens(tokens);
    return tokens;
  }
  
  async logout() {
    const refreshToken = localStorage.getItem('refresh_token');
    
    if (refreshToken) {
      try {
        await fetch(`${this.baseUrl}/logout`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ refreshToken })
        });
      } catch (error) {
        console.error('Logout error:', error);
      }
    }
    
    this.clearTokens();
  }
  
  saveTokens(tokens) {
    localStorage.setItem('access_token', tokens.access_token);
    localStorage.setItem('refresh_token', tokens.refresh_token);
    localStorage.setItem('token_expiry', Date.now() + (tokens.expires_in * 1000));
  }
  
  getAccessToken() {
    return localStorage.getItem('access_token');
  }
  
  isTokenExpired() {
    const expiry = localStorage.getItem('token_expiry');
    return !expiry || Date.now() > parseInt(expiry);
  }
  
  clearTokens() {
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
    localStorage.removeItem('token_expiry');
  }
}

export default new AuthService();
```

---

## 🧪 Testing

### Quick Test
```javascript
// In browser console
fetch('http://localhost:8082/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123' })
})
.then(r => r.json())
.then(console.log);
```

---

## 📝 API Response Examples

### Login Response
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI...",
  "token_type": "Bearer",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI...",
  "scope": "openid profile email"
}
```

### User Info Response
```json
{
  "sub": "a1b2c3d4-e5f6-...",
  "username": "admin",
  "email": "admin@eshop.com",
  "name": "Admin User",
  "givenName": "Admin",
  "familyName": "User",
  "emailVerified": true,
  "roles": ["ADMIN"],
  "issuedAt": "2025-12-22T10:00:00Z",
  "expiresAt": "2025-12-22T10:05:00Z"
}
```

---

## ⚠️ Important Notes

1. **CORS**: Backend already configured to allow `http://localhost:3000` and `http://localhost:4200`
2. **Token Storage**: Store tokens in `localStorage` for web apps, secure storage for mobile
3. **Token Expiry**: Access tokens expire in 5 minutes, refresh tokens in 30 minutes
4. **Error Handling**: Always handle 401 errors by refreshing token or redirecting to login
5. **Security**: Never expose tokens in URLs or logs

---

## 🔗 Resources

- [Complete Setup Guide](../KEYCLOAK_IMPLEMENTATION_GUIDE.md)
- [Backend API Docs](http://localhost:8082/swagger-ui.html)
- [Test Scripts](../test-keycloak-auth.ps1)

---

**Need Help?** Check the implementation guide or backend logs for more details.


# --- File: help.md ---

# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.0/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.0/gradle-plugin/packaging-oci-image.html)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/4.0.0/reference/actuator/index.html)
* [Spring Cache Abstraction](https://docs.spring.io/spring-boot/4.0.0/reference/io/caching.html)
* [Spring Data JPA](https://docs.spring.io/spring-boot/4.0.0/reference/data/sql.html#data.sql.jpa-and-spring-data)
* [OAuth2 Resource Server](https://docs.spring.io/spring-boot/4.0.0/reference/web/spring-security.html#web.security.oauth2.server)
* [Spring Security](https://docs.spring.io/spring-boot/4.0.0/reference/web/spring-security.html)
* [Validation](https://docs.spring.io/spring-boot/4.0.0/reference/io/validation.html)
* [Spring Web](https://docs.spring.io/spring-boot/4.0.0/reference/web/servlet.html)

### Guides
The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service with Spring Boot Actuator](https://spring.io/guides/gs/actuator-service/)
* [Caching Data with Spring](https://spring.io/guides/gs/caching/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Validation](https://spring.io/guides/gs/validating-form-input/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Additional Links
These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)



# --- File: implementation-guide.md ---

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


# --- File: quick-reference.md ---

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


# --- File: technology-stack-full.md ---

# EShop — Full Technology Stack

This document lists the complete technology stack used in the EShop project, including the primary libraries and versions (taken from `build.gradle`), runtime services, configuration pointers, and quick run/debug commands.

---

## Project summary
- Language: Java 21 (toolchain configured in Gradle)
- Framework: Spring Boot 4 (Spring Framework 7)
- Build: Gradle (wrapper provided)

## Key build plugins
- `org.springframework.boot` plugin — 4.0.1
- `io.spring.dependency-management` plugin — 1.1.7

## Primary runtime libraries (selected, with versions)
- Spring Boot starters: WebMVC, Data JPA, Security, Actuator, Cache, Validation (via Spring Boot 4)
- Caffeine: `com.github.ben-manes.caffeine:caffeine:3.1.8`
- Springdoc OpenAPI (Swagger UI): `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.0`
- JJWT (JWT helpers): `io.jsonwebtoken:jjwt-api:0.12.3` (+ `jjwt-impl` and `jjwt-jackson` at runtime)
- Flyway (DB migrations): `org.flywaydb:flyway-core:10.10.0` (+ PostgreSQL db module)
- Resilience4j: `io.github.resilience4j:resilience4j-spring-boot3:2.2.0` and related modules (circuitbreaker, retry, ratelimiter, bulkhead, timelimiter)
- Micrometer: `io.micrometer:micrometer-core:1.16.0` and Prometheus registry `1.16.0`
- MapStruct: `org.mapstruct:mapstruct:1.6.3` (+ processor)
- Lombok: `org.projectlombok:lombok:1.18.42` and `lombok-mapstruct-binding:0.2.0`
- Jackson Databind: `com.fasterxml.jackson.core:jackson-databind:2.16.0`
- PostgreSQL JDBC Driver: `org.postgresql:postgresql` (runtime)
- Stripe SDK: `com.stripe:stripe-java:24.18.0`
- PayPal Checkout SDK: `com.paypal.sdk:checkout-sdk:2.0.0`
- Razorpay SDK: `com.razorpay:razorpay-java:1.4.6`
- Thumbnailator (image processing): `net.coobird:thumbnailator:0.4.19`
- Cloudinary SDK: `com.cloudinary:cloudinary-http44:1.36.0`
- Apache Commons Lang: `org.apache.commons:commons-lang3:3.14.0`
- Apache POI (Excel): `org.apache.poi:poi-ooxml:5.2.5`
- iText (PDF): `com.itextpdf:itext7-core:8.0.2`
- ShedLock (scheduled locking): `net.javacrumbs.shedlock:shedlock-spring:5.10.0` + JDBC provider
- JSR-354 Money API + Moneta: `javax.money:money-api:1.1`, `org.javamoney:moneta:1.4.2`
- Jsoup (HTML sanitization): `org.jsoup:jsoup:1.18.1`

## Observability & Tracing
- Spring Boot Actuator
- Micrometer Prometheus registry
- Micrometer tracing bridge + Zipkin reporter (for distributed tracing): `micrometer-tracing-bridge-brave`, `zipkin-reporter-brave`

## Caching
- Caffeine as primary in-memory cache (configured in `CacheConfig`)
- Cache names centralized in code (`CacheConfig`) to avoid missing-cache errors

## Resilience & Reliability
- Resilience patterns via Resilience4j and Spring Retry
- Retry used for transient operations; business exceptions should not be wrapped so they propagate to global handlers

## Security & Identity
- Spring Security (OAuth2 Resource Server) for JWT validation
- Keycloak (containerized) used as identity provider for dev (Docker Compose files included)
- CSRF disabled for stateless REST endpoints in `EnhancedSecurityConfig`

## Persistence
- Primary DB: PostgreSQL (production); H2 used in tests
- Flyway for DB migrations (scripts in `src/main/resources/db/migration`)
- HikariCP as the default connection pool (via Spring Boot)

## Testing
- JUnit 5 + Spring Boot test starter
- Spring Security test helpers
- H2 runtime for faster in-memory tests

## Dev tooling & infra
- Gradle wrapper (`gradlew`, `gradlew.bat`) — use for builds and running app
- Docker & Docker Compose (compose files: `docker-compose.keycloak.yml`, `keycloak-docker-compose.yml`, `docker-compose.prod.yml`)
- Git for version control

## Important project files / paths
- Application config: `src/main/resources/application.properties` and profile-specific variants
- Cache config: `src/main/java/com/eshop/app/config/CacheConfig.java`
- Security config: `src/main/java/com/eshop/app/config/EnhancedSecurityConfig.java`
- Global exception handler: `src/main/java/com/eshop/app/common/exception/ProblemDetailExceptionHandler.java`
- DB migrations: `src/main/resources/db/migration`
- DTOs / Responses: `src/main/java/com/eshop/app/dto` (includes `PageResponse` compatibility getters)

## Defaults & local run commands
1. Start infra (Keycloak, Postgres) via compose if needed:

```powershell
# start Keycloak (example)
docker-compose -f docker-compose.keycloak.yml up -d
# start Postgres (if you have a compose target)
docker-compose up -d postgres
```

2. Build the project (skip tests for faster iteration):

```powershell
./gradlew.bat clean build -x test
```

3. Run the app (dev profile). If port 8082 is in use, override port:

```powershell
./gradlew.bat bootRun -x test --args="--spring.profiles.active=dev --server.port=8083"
```

## Environment variables commonly used
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `SPRING_PROFILES_ACTIVE`
- `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK-SET-URI` or `spring.security.oauth2.resourceserver.jwt.*` props
- Payment provider keys: `STRIPE_API_KEY`, `PAYPAL_CLIENT_ID`, `PAYPAL_SECRET`, `RAZORPAY_KEY` (store securely)

## Troubleshooting tips
- SpEL `content` missing on responses: ensure `PageResponse` exposes `getContent()` (compatibility helper added in DTO).
- Missing cache name error: confirm cache names exist in `CacheConfig` and that your `@Cacheable`/`@CachePut` annotations use constants.
- Port already in use: change `--server.port` or stop the occupying process.

## How to get a complete dependency BOM
Run the Gradle dependency report for the runtime classpath:

```powershell
./gradlew.bat dependencies --configuration runtimeClasspath > deps.txt
```

## Next recommendations (optional)
- Add `README.md` with explicit Windows and Linux run/debug recipes.
- Add `CONTRIBUTING.md` describing local dev flow and how to run tests and migrations.
- Add a `docker-compose.dev.yml` that brings up Postgres + Keycloak + the app (optional automated local dev stack).

---

If you want, I can now:
- create a concise `README.md` with the exact commands tailored for Windows (I can do that next), or
- add a `CONTRIBUTING.md` describing how to set up Keycloak and Postgres locally.

Tell me which one to do next.


# --- File: technology-stack.md ---

# 🛠️ Technology Stack — EShop Application

This document describes the primary technologies, libraries, and conventions used by the EShop backend and how to run/configure the service locally.

## Overview
- Language: Java 21
- Framework: Spring Boot 4 / Spring Framework 7
- Build: Gradle

## Core Backend
- Java 21 (LTS)
- Spring Boot 4 (Spring Framework 7)
- Spring Web MVC (REST controllers)
- Spring Data JPA (Hibernate) — data access layer
- Flyway — database migrations (db/migration)

## Caching
- Caffeine — primary in-memory cache (configured via `CacheConfig`)
- Cache names centralized in `CacheConfig` to avoid typos (e.g. `CATEGORIES_CACHE`, `CATEGORY_CACHE`, `CATEGORY_LIST_CACHE`).

## Resilience & Reliability
- Spring Retry / Resilience4j patterns used for transient failures
- Retry policies are used selectively; business exceptions are not wrapped so they propagate to the global exception handler.

## Security & Identity
- Spring Security (OAuth2 Resource Server) for JWT validation
- Keycloak used as identity provider (local docker-compose for dev)
- CSRF is disabled for stateless REST endpoints (see `EnhancedSecurityConfig`).

## Persistence
- PostgreSQL (recommended 14+) — configured via application properties
- HikariCP as connection pool

## API & Documentation
- OpenAPI 3 / Springdoc (Swagger UI) for interactive API docs
- ProblemDetail-based global exception handling for consistent error responses

## Observability
- Spring Boot Actuator endpoints enabled
- Prometheus-compatible metrics export recommended (Actuator + Prometheus)
- Logback for structured logging

## Testing & Quality
- JUnit 5 for unit/integration tests
- Mockito / Spring Test utilities for mocking and slice tests

## Dev & Deployment Tooling
- Gradle wrapper (`gradlew` / `gradlew.bat`) for builds
- Docker & Docker Compose for local environment (Keycloak, DB)
- Recommended CI: any Gradle-capable runner (GitHub Actions, Azure Pipelines, etc.)

## Notable Libraries and Uses
- Lombok: reduces boilerplate for DTOs/Entities
- MapStruct: compile-time DTO mapping where appropriate
- Spring Retry / Resilience4j: transparent retry and circuit-breaker strategies

## Important Files & Locations
- Application config: [src/main/resources/application.properties](src/main/resources/application.properties)
- Security config: [src/main/java/com/eshop/app/config/EnhancedSecurityConfig.java](src/main/java/com/eshop/app/config/EnhancedSecurityConfig.java)
- Cache config and names: [src/main/java/com/eshop/app/config/CacheConfig.java](src/main/java/com/eshop/app/config/CacheConfig.java)
- Global exception handling: [src/main/java/com/eshop/app/common/exception/ProblemDetailExceptionHandler.java](src/main/java/com/eshop/app/common/exception/ProblemDetailExceptionHandler.java)
- DB migrations: [src/main/resources/db/migration](src/main/resources/db/migration)

## Running Locally (quick)
1. Start dependent services (Postgres, Keycloak) via Docker Compose if needed:

```powershell
docker-compose -f docker-compose.keycloak.yml up -d
docker-compose up -d postgres
```

2. Build (skip tests for quicker feedback):

```powershell
./gradlew.bat clean build -x test
```

3. Run with `dev` profile on alternate port if 8082 is occupied:

```powershell
./gradlew.bat bootRun -x test --args="--spring.profiles.active=dev --server.port=8083"
```

## Environment & Common Properties
- `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password` — DB connection
- `spring.profiles.active` — profile selection (`dev`, `prod`, `test`)
- `spring.security.oauth2.resourceserver.jwt.*` — JWT validation settings

## Conventions & Best Practices
- Centralize cache names in `CacheConfig` to avoid IllegalArgumentException caused by missing caches.
- Throw domain/business exceptions (e.g. `DuplicateResourceException`) directly so the ProblemDetail handler can map them to 409/404.
- Use DTOs for public APIs; map entities via `MapStruct`.

## Quick Troubleshooting
- If you see `SpelEvaluationException: 'content' cannot be found` — ensure `PageResponse` has `getContent()` accessor (compatibility with templates/SpEL).
- If app fails to start due to port conflict: change `--server.port` in bootRun args or stop the conflicting process.

---

If you'd like, I can expand this into a CONTRIBUTING section, add exact dependency versions, or generate a short README with run/debug recipes for Windows and Linux.

