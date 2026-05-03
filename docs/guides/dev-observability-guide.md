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
