#!/usr/bin/env pwsh
# ============================================
# EShop Docker Startup Script
# Builds and starts all Docker containers
# ============================================

Write-Host "🚀 EShop Docker Setup" -ForegroundColor Cyan
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan

# Check if .env file exists
if (-Not (Test-Path ".env")) {
    Write-Host "⚠️  Creating .env from .env.example..." -ForegroundColor Yellow
    Copy-Item ".env.example" ".env"
    Write-Host "✅ .env file created. Please update it with your configuration." -ForegroundColor Green
}

# Step 1: Build the Spring Boot application
Write-Host ""
Write-Host "📦 Step 1: Building Spring Boot application..." -ForegroundColor Cyan
.\gradlew.bat clean build -x test
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed! Fix errors before continuing." -ForegroundColor Red
    exit 1
}
Write-Host "✅ Build successful!" -ForegroundColor Green

# Step 2: Build Docker image
Write-Host ""
Write-Host "🐳 Step 2: Building Docker images..." -ForegroundColor Cyan
docker compose build
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Docker build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Docker images built!" -ForegroundColor Green

# Step 3: Start all containers
Write-Host ""
Write-Host "🚢 Step 3: Starting containers..." -ForegroundColor Cyan
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to start containers!" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Containers started!" -ForegroundColor Green

# Step 4: Wait for services to be healthy
Write-Host ""
Write-Host "⏳ Step 4: Waiting for services to be healthy..." -ForegroundColor Cyan
Write-Host "This may take 60-90 seconds..." -ForegroundColor Yellow

$maxWait = 120
$elapsed = 0
$interval = 5

while ($elapsed -lt $maxWait) {
    Start-Sleep -Seconds $interval
    $elapsed += $interval
    
    $redisHealth = docker inspect --format='{{.State.Health.Status}}' eshop-redis 2>$null
    $postgresHealth = docker inspect --format='{{.State.Health.Status}}' eshop-postgres 2>$null
    $keycloakHealth = docker inspect --format='{{.State.Health.Status}}' eshop-keycloak 2>$null
    $backendHealth = docker inspect --format='{{.State.Health.Status}}' eshop-backend 2>$null
    
    Write-Host "[$elapsed`s] Redis: $redisHealth | Postgres: $postgresHealth | Keycloak: $keycloakHealth | Backend: $backendHealth" -ForegroundColor Gray
    
    if ($redisHealth -eq "healthy" -and $postgresHealth -eq "healthy" -and $keycloakHealth -eq "healthy" -and $backendHealth -eq "healthy") {
        Write-Host "✅ All services are healthy!" -ForegroundColor Green
        break
    }
}

if ($elapsed -ge $maxWait) {
    Write-Host "⚠️  Timeout waiting for services. Check logs with: docker compose logs" -ForegroundColor Yellow
}

# Step 5: Show status
Write-Host ""
Write-Host "📊 Container Status:" -ForegroundColor Cyan
docker compose ps

# Step 6: Test Redis connectivity
Write-Host ""
Write-Host "🔍 Testing Redis connectivity..." -ForegroundColor Cyan
$redisTest = docker exec eshop-backend sh -c "wget -qO- --timeout=3 http://localhost:8082/actuator/health 2>&1"
if ($redisTest -match "UP") {
    Write-Host "✅ Backend is UP and Redis connection is working!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Backend may still be starting. Check logs: docker compose logs backend" -ForegroundColor Yellow
}

# Summary
Write-Host ""
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
Write-Host "🎉 Docker Setup Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📍 Service Endpoints:" -ForegroundColor Cyan
Write-Host "   Backend API:      http://localhost:8082" -ForegroundColor White
Write-Host "   Swagger UI:       http://localhost:8082/swagger-ui.html" -ForegroundColor White
Write-Host "   Keycloak Admin:   http://localhost:8080" -ForegroundColor White
Write-Host "   Actuator Health:  http://localhost:8082/actuator/health" -ForegroundColor White
Write-Host ""
Write-Host "🔧 Useful Commands:" -ForegroundColor Cyan
Write-Host "   View logs:        docker compose logs -f" -ForegroundColor White
Write-Host "   View backend logs: docker compose logs -f backend" -ForegroundColor White
Write-Host "   Stop containers:  docker compose down" -ForegroundColor White
Write-Host "   Restart backend:  docker compose restart backend" -ForegroundColor White
Write-Host ""
Write-Host "🧪 Test Authentication:" -ForegroundColor Cyan
Write-Host "   .\test-keycloak-auth.ps1" -ForegroundColor White
Write-Host "═══════════════════════════════════════" -ForegroundColor Cyan
