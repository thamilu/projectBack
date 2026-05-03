# Port Allocation Reference

## 📊 Current Port Usage

### Your Applications
| Port | Service | Purpose |
|------|---------|---------|
| **3000** | Frontend (User App) | Main user-facing application |
| **3001** | Frontend Admin | Admin panel/dashboard |
| **8082** | Spring Boot Backend | REST API server |

### Docker Services - DEV & DOCKER
| Port | Service | Purpose |
|------|---------|---------|
| **3002** | **Grafana** | Metrics visualization |
| **5050** | pgAdmin | PostgreSQL GUI |
| **5432** | PostgreSQL | Database |
| **6379** | Redis | Cache |
| **8080** | Keycloak | Authentication |
| **8081** | Redis Commander | Redis browser |
| **9090** | Prometheus | Metrics collection |
| **9411** | Zipkin | Distributed tracing |

### Production (Internal Only)
| Port | Service | Access |
|------|---------|--------|
| 127.0.0.1:3000 | Grafana | SSH tunnel only |
| 127.0.0.1:9090 | Prometheus | SSH tunnel only |
| 127.0.0.1:9411 | Zipkin | SSH tunnel only |
| 80/443 | Nginx | Public |

---

## 🚀 Quick Access URLs

### Development Environment

```powershell
# Frontend
http://localhost:3000        # User App
http://localhost:3001        # Admin Panel

# Backend
http://localhost:8082        # API
http://localhost:8082/swagger-ui.html

# Monitoring
http://localhost:3002        # Grafana (admin/admin)
http://localhost:9090        # Prometheus
http://localhost:9411        # Zipkin

# Management Tools
http://localhost:5050        # pgAdmin (admin@eshop.com/admin)
http://localhost:8081        # Redis Commander
http://localhost:8080        # Keycloak (admin/admin)
```

---

## ✅ No Port Conflicts!

All services are now properly separated:
- ✅ Frontend uses 3000 & 3001
- ✅ Grafana uses 3002
- ✅ Backend uses 8082
- ✅ All other services use their standard ports

---

## 🔄 If You Need to Change Ports

### Change Grafana Port

Edit these files:
1. `docker-compose-dev.yml`
   ```yaml
   grafana:
     ports:
       - "NEW_PORT:3000"
     environment:
       - GF_SERVER_ROOT_URL=http://localhost:NEW_PORT
   ```

2. `docker-compose.yml` (same changes)

### Change Backend Port

Edit `application.properties`:
```properties
server.port=8082  # Change to your desired port
```

And docker-compose files:
```yaml
backend:
  ports:
    - "NEW_PORT:NEW_PORT"
```

---

## 🎯 Complete Stack Startup

```powershell
# Start all Docker services
docker compose -f docker-compose-dev.yml up -d

# Start backend locally
cd G:\Project\eshop_back
.\gradlew bootRun

# Start frontend (in your frontend directory)
cd path\to\frontend
npm run dev

# Access everything:
# Frontend:     http://localhost:3000
# Admin:        http://localhost:3001
# Backend:      http://localhost:8082
# Grafana:      http://localhost:3002
# Prometheus:   http://localhost:9090
```

---

**All ports configured! No conflicts! 🎉**
