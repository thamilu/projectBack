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
