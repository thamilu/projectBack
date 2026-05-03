#!/bin/bash
# ============================================
# Start Redis for Local Development (Linux/Mac)
# ============================================

echo "🚀 Starting Redis..."

# Start Redis
docker compose -f docker-compose-redis.yml up -d

if [ $? -ne 0 ]; then
    echo "❌ Failed to start Redis!"
    exit 1
fi

echo "✅ Redis starting..."

# Wait for Redis
echo "⏳ Waiting for Redis to be ready..."

max_wait=30
elapsed=0
interval=2

while [ $elapsed -lt $max_wait ]; do
    sleep $interval
    elapsed=$((elapsed + interval))
    
    redis_health=$(docker inspect --format='{{.State.Health.Status}}' eshop-redis 2>/dev/null || echo "unknown")
    
    if [ "$redis_health" = "healthy" ]; then
        echo "✅ Redis is ready!"
        break
    fi
    
    echo "[${elapsed}s] Redis: $redis_health"
done

# Test Redis
echo ""
echo "🔍 Testing Redis..."
redis_ping=$(docker exec eshop-redis redis-cli ping 2>&1)
if [[ $redis_ping == *"PONG"* ]]; then
    echo "✅ Redis is working!"
else
    echo "⚠️  Redis might not be ready yet"
fi

# Summary
echo ""
echo "═══════════════════════════════════════"
echo "✅ Redis Running!"
echo ""
echo "📍 Connection:"
echo "   Host: localhost"
echo "   Port: 6379"
echo ""
echo "🔧 Useful Commands:"
echo "   Test: docker exec eshop-redis redis-cli ping"
echo "   Logs: docker compose -f docker-compose-redis.yml logs -f"
echo "   Stop: docker compose -f docker-compose-redis.yml down"
echo ""
echo "🚀 Now run your backend:"
echo "   ./gradlew bootRun --args='--spring.profiles.active=dev'"
echo "═══════════════════════════════════════"
