#!/bin/bash
# ============================================
# Local Development Setup (Linux/Mac)
# Keycloak + Redis in Docker, Backend locally
# ============================================

echo "🚀 Starting Development Infrastructure..."
echo "═══════════════════════════════════════"

# Step 1: Start Docker containers
echo ""
echo "🐳 Starting Keycloak & Redis in Docker..."
docker compose -f docker-compose-dev.yml up -d

if [ $? -ne 0 ]; then
    echo "❌ Failed to start Docker containers!"
    exit 1
fi

echo "✅ Docker containers starting!"

# Step 2: Wait for services to be healthy
echo ""
echo "⏳ Waiting for services to be healthy..."
echo "This may take 30-60 seconds..."

max_wait=90
elapsed=0
interval=5

while [ $elapsed -lt $max_wait ]; do
    sleep $interval
    elapsed=$((elapsed + interval))
    
    redis_health=$(docker inspect --format='{{.State.Health.Status}}' eshop-redis-dev 2>/dev/null || echo "unknown")
    postgres_health=$(docker inspect --format='{{.State.Health.Status}}' eshop-postgres-dev 2>/dev/null || echo "unknown")
    keycloak_health=$(docker inspect --format='{{.State.Health.Status}}' eshop-keycloak-dev 2>/dev/null || echo "unknown")
    
    echo "[${elapsed}s] Redis: $redis_health | Postgres: $postgres_health | Keycloak: $keycloak_health"
    
    if [ "$redis_health" = "healthy" ] && [ "$postgres_health" = "healthy" ] && [ "$keycloak_health" = "healthy" ]; then
        echo "✅ All services are healthy!"
        break
    fi
done

if [ $elapsed -ge $max_wait ]; then
    echo "⚠️  Timeout waiting for services. Check logs with: docker compose -f docker-compose-dev.yml logs"
fi

# Step 3: Show status
echo ""
echo "📊 Container Status:"
docker compose -f docker-compose-dev.yml ps

# Summary
echo ""
echo "═══════════════════════════════════════"
echo "🎉 Infrastructure Ready!"
echo ""
echo "📍 Services Running in Docker:"
echo "   Redis:            localhost:6379"
echo "   PostgreSQL:       localhost:5432"
echo "   Keycloak:         http://localhost:8080"
echo "   Keycloak Admin:   admin / admin"
echo ""
echo "🔧 Now start your Spring Boot backend:"
echo "   ./gradlew bootRun --args='--spring.profiles.active=dev'"
echo ""
echo "   Or from your IDE with profile: dev"
echo ""
echo "🛑 To stop infrastructure:"
echo "   docker compose -f docker-compose-dev.yml down"
echo ""
echo "📋 View logs:"
echo "   docker compose -f docker-compose-dev.yml logs -f"
echo "═══════════════════════════════════════"
