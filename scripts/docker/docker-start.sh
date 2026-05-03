#!/bin/bash
# ============================================
# EShop Docker Startup Script (Linux/Mac)
# Builds and starts all Docker containers
# ============================================

echo "🚀 EShop Docker Setup"
echo "═══════════════════════════════════════"

# Check if .env file exists
if [ ! -f .env ]; then
    echo "⚠️  Creating .env from .env.example..."
    cp .env.example .env
    echo "✅ .env file created. Please update it with your configuration."
fi

# Step 1: Build the Spring Boot application
echo ""
echo "📦 Step 1: Building Spring Boot application..."
./gradlew clean build -x test
if [ $? -ne 0 ]; then
    echo "❌ Build failed! Fix errors before continuing."
    exit 1
fi
echo "✅ Build successful!"

# Step 2: Build Docker image
echo ""
echo "🐳 Step 2: Building Docker images..."
docker compose build
if [ $? -ne 0 ]; then
    echo "❌ Docker build failed!"
    exit 1
fi
echo "✅ Docker images built!"

# Step 3: Start all containers
echo ""
echo "🚢 Step 3: Starting containers..."
docker compose up -d
if [ $? -ne 0 ]; then
    echo "❌ Failed to start containers!"
    exit 1
fi
echo "✅ Containers started!"

# Step 4: Wait for services to be healthy
echo ""
echo "⏳ Step 4: Waiting for services to be healthy..."
echo "This may take 60-90 seconds..."

max_wait=120
elapsed=0
interval=5

while [ $elapsed -lt $max_wait ]; do
    sleep $interval
    elapsed=$((elapsed + interval))
    
    redis_health=$(docker inspect --format='{{.State.Health.Status}}' eshop-redis 2>/dev/null || echo "unknown")
    postgres_health=$(docker inspect --format='{{.State.Health.Status}}' eshop-postgres 2>/dev/null || echo "unknown")
    keycloak_health=$(docker inspect --format='{{.State.Health.Status}}' eshop-keycloak 2>/dev/null || echo "unknown")
    backend_health=$(docker inspect --format='{{.State.Health.Status}}' eshop-backend 2>/dev/null || echo "unknown")
    
    echo "[${elapsed}s] Redis: $redis_health | Postgres: $postgres_health | Keycloak: $keycloak_health | Backend: $backend_health"
    
    if [ "$redis_health" = "healthy" ] && [ "$postgres_health" = "healthy" ] && [ "$keycloak_health" = "healthy" ] && [ "$backend_health" = "healthy" ]; then
        echo "✅ All services are healthy!"
        break
    fi
done

if [ $elapsed -ge $max_wait ]; then
    echo "⚠️  Timeout waiting for services. Check logs with: docker compose logs"
fi

# Step 5: Show status
echo ""
echo "📊 Container Status:"
docker compose ps

# Summary
echo ""
echo "═══════════════════════════════════════"
echo "🎉 Docker Setup Complete!"
echo ""
echo "📍 Service Endpoints:"
echo "   Backend API:      http://localhost:8082"
echo "   Swagger UI:       http://localhost:8082/swagger-ui.html"
echo "   Keycloak Admin:   http://localhost:8080"
echo "   Actuator Health:  http://localhost:8082/actuator/health"
echo ""
echo "🔧 Useful Commands:"
echo "   View logs:        docker compose logs -f"
echo "   View backend logs: docker compose logs -f backend"
echo "   Stop containers:  docker compose down"
echo "   Restart backend:  docker compose restart backend"
echo ""
echo "🧪 Test Authentication:"
echo "   ./test-keycloak-auth.sh"
echo "═══════════════════════════════════════"
