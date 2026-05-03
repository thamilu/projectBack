#!/usr/bin/env pwsh
# Start Redis for Local Development

Write-Host "Starting Redis..." -ForegroundColor Cyan

docker compose -f docker-compose-redis.yml up -d

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to start Redis!" -ForegroundColor Red
    exit 1
}

Write-Host "Redis starting..." -ForegroundColor Green

# Wait for Redis to be healthy
Write-Host "Waiting for Redis to be ready..." -ForegroundColor Yellow

$maxWait = 30
$elapsed = 0
$interval = 2

while ($elapsed -lt $maxWait) {
    Start-Sleep -Seconds $interval
    $elapsed += $interval
    
    $redisHealth = docker inspect --format='{{.State.Health.Status}}' eshop-redis 2>$null
    
    if ($redisHealth -eq "healthy") {
        Write-Host "Redis is ready!" -ForegroundColor Green
        break
    }
    
    Write-Host "[$elapsed s] Redis: $redisHealth" -ForegroundColor Gray
}

# Test Redis
Write-Host ""
Write-Host "Testing Redis..." -ForegroundColor Cyan
$redisPing = docker exec eshop-redis redis-cli ping 2>&1
if ($redisPing -match "PONG") {
    Write-Host "Redis is working!" -ForegroundColor Green
} else {
    Write-Host "Redis might not be ready yet" -ForegroundColor Yellow
}

# Summary
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Redis Running!" -ForegroundColor Green
Write-Host ""
Write-Host "Connection:" -ForegroundColor Cyan
Write-Host "   Host: localhost" -ForegroundColor White
Write-Host "   Port: 6379" -ForegroundColor White
Write-Host ""
Write-Host "Useful Commands:" -ForegroundColor Cyan
Write-Host "   Test: docker exec eshop-redis redis-cli ping" -ForegroundColor White
Write-Host "   Logs: docker compose -f docker-compose-redis.yml logs -f" -ForegroundColor White
Write-Host "   Stop: docker compose -f docker-compose-redis.yml down" -ForegroundColor White
Write-Host ""
Write-Host "Now run your backend:" -ForegroundColor Cyan
Write-Host "   .\gradlew.bat bootRun --args=`"--spring.profiles.active=dev`"" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
