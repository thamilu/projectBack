#!/bin/bash
# Wait for Keycloak to be fully started
sleep 30

# Add CUSTOMER role to default-roles-eshop composite
docker exec eshop-postgres-dev psql -U postgres -d eshop_Dev -c "
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
"

echo "CUSTOMER role added to default roles composite"

# Restart Keycloak to clear cache
docker restart eshop-keycloak-dev
