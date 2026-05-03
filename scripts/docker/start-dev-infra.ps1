#!/usr/bin/env pwsh
# ============================================
# Local Development Setup
# Keycloak + Redis in Docker, Backend locally
# ============================================

Write-Host "🚀 Starting Development Infrastructure..." -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan

# Step 1: Start Docker containers (Keycloak + Redis)
Write-Host ""
Write-Host "🐳 Starting Keycloak & Redis in Docker..." -ForegroundColor Cyan
docker compose -f docker-compose-dev.yml up -d

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to start Docker containers!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Docker containers starting!" -ForegroundColor Green

# Step 2: Wait for services to be healthy
Write-Host ""
Write-Host "⏳ Waiting for services to be healthy..." -ForegroundColor Cyan
Write-Host "This may take 30-60 seconds..." -ForegroundColor Yellow

$maxWait = 90
$elapsed = 0
$interval = 5

while ($elapsed -lt $maxWait) {
    Start-Sleep -Seconds $interval
    $elapsed += $interval
    
    $redisHealth = docker inspect --format='{{.State.Health.Status}}' eshop-redis-dev 2>$null
    $postgresHealth = docker inspect --format='{{.State.Health.Status}}' eshop-postgres-dev 2>$null
    $keycloakHealth = docker inspect --format='{{.State.Health.Status}}' eshop-keycloak-dev 2>$null
    
    Write-Host "[$elapsed`s] Redis: $redisHealth | Postgres: $postgresHealth | Keycloak: $keycloakHealth" -ForegroundColor Gray
    
    if ($redisHealth -eq "healthy" -and $postgresHealth -eq "healthy" -and $keycloakHealth -eq "healthy") {
        Write-Host "✅ All services are healthy!" -ForegroundColor Green
        break
    }
}

if ($elapsed -ge $maxWait) {
    Write-Host "⚠️  Timeout waiting for services. Check logs with: docker compose -f docker-compose-dev.yml logs" -ForegroundColor Yellow
}

# Step 3: Show status
Write-Host ""
Write-Host "📊 Container Status:" -ForegroundColor Cyan
docker compose -f docker-compose-dev.yml ps

# Step 4: Test Redis connectivity
Write-Host ""
Write-Host "🔍 Testing Redis connectivity..." -ForegroundColor Cyan
try {
    $redisTest = docker exec eshop-redis-dev redis-cli ping 2>&1
    if ($redisTest -match "PONG") {
        Write-Host "✅ Redis is responding!" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠️  Could not test Redis connectivity" -ForegroundColor Yellow
}

# Summary
Write-Host ""
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
Write-Host "🎉 Infrastructure Ready!" -ForegroundColor Green
Write-Host ""
Write-Host "📍 Services Running in Docker:" -ForegroundColor Cyan
Write-Host "   Redis:            localhost:6379" -ForegroundColor White
Write-Host "   PostgreSQL:       localhost:5432" -ForegroundColor White
Write-Host "   Keycloak:         http://localhost:8080" -ForegroundColor White
Write-Host "   Keycloak Admin:   admin / admin" -ForegroundColor White
Write-Host ""
Write-Host "🔧 Now start your Spring Boot backend:" -ForegroundColor Cyan
Write-Host "   .\gradlew.bat bootRun --args=`"--spring.profiles.active=dev`"" -ForegroundColor Yellow
Write-Host ""
Write-Host "   Or from your IDE with profile: dev" -ForegroundColor Yellow
Write-Host ""
Write-Host "🛑 To stop infrastructure:" -ForegroundColor Cyan
Write-Host "   docker compose -f docker-compose-dev.yml down" -ForegroundColor White
Write-Host ""
Write-Host "📋 View logs:" -ForegroundColor Cyan
Write-Host "   docker compose -f docker-compose-dev.yml logs -f" -ForegroundColor White
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
