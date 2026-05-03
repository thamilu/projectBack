# Fix Keycloak Default Roles - Post-Import Script
# This script adds the CUSTOMER role to the default-roles-eshop composite
# Run this after starting Keycloak with fresh volumes

Write-Host "Waiting for Keycloak to fully start..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

Write-Host "Adding CUSTOMER role to default roles composite..." -ForegroundColor Cyan

$sql = @"
INSERT INTO public.composite_role (composite, child_role) 
SELECT 
  (SELECT id FROM public.keycloak_role WHERE name='default-roles-eshop' AND realm_id=(SELECT id FROM public.realm WHERE name='eshop')),
  (SELECT id FROM public.keycloak_role WHERE name='CUSTOMER' AND realm_id=(SELECT id FROM public.realm WHERE name='eshop'))
WHERE NOT EXISTS (
  SELECT 1 FROM public.composite_role cr 
  JOIN public.keycloak_role r1 ON cr.composite = r1.id 
  JOIN public.keycloak_role r2 ON cr.child_role = r2.id 
  WHERE r1.name='default-roles-eshop' AND r2.name='CUSTOMER'
);
"@

docker exec eshop-postgres-dev psql -U postgres -d eshop_Dev -c $sql

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ CUSTOMER role added successfully!" -ForegroundColor Green
    
    Write-Host "Restarting Keycloak to clear cache..." -ForegroundColor Cyan
    docker restart eshop-keycloak-dev
    
    Write-Host "✅ Done! CUSTOMER role is now a default role for new registrations." -ForegroundColor Green
} else {
    Write-Host "❌ Failed to add CUSTOMER role. Check the error above." -ForegroundColor Red
}
