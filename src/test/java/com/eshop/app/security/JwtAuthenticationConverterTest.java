package com.eshop.app.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.*;

public class JwtAuthenticationConverterTest {

  private JwtAuthenticationConverter jwtAuthenticationConverter;

  @BeforeEach
  void setUp() {
    jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
      List<GrantedAuthority> authorities = new ArrayList<>();
      // Realm roles
      Map<String, Object> realmAccess = jwt.getClaim("realm_access");
      if (realmAccess != null) {
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles != null) {
          roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
        }
      }
      // Resource roles
      Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
      if (resourceAccess != null) {
        for (Object innerObj : ((Map<?, ?>) resourceAccess).values()) {
          if (innerObj instanceof Map) {
            Object r = ((Map<?, ?>) innerObj).get("roles");
            if (r instanceof List) {
              for (Object ro : (List<?>) r) {
                if (ro != null) {
                  authorities.add(new SimpleGrantedAuthority("ROLE_" + ro.toString().toUpperCase()));
                }
              }
            }
          }
        }
      }
      return authorities;
    });
  }

  @Test
  void shouldMapSellerFromRealmAndResourceAccess() {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(3600);

    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", "u1");
    claims.put("email", "seller@example.com");
    claims.put("preferred_username", "sellerUser");

    Map<String, Object> realmAccess = new HashMap<>();
    realmAccess.put("roles", Arrays.asList("SELLER", "CUSTOMER"));
    claims.put("realm_access", realmAccess);

    Map<String, Object> resourceAccess = new HashMap<>();
    Map<String, Object> clientRoles = new HashMap<>();
    clientRoles.put("roles", Arrays.asList("SELLER"));
    resourceAccess.put("eshop-client", clientRoles);
    claims.put("resource_access", resourceAccess);

    Map<String, Object> headers = new HashMap<>();
    headers.put("alg", "none");
    Jwt jwt = new Jwt("token", now, exp, headers, claims);

    Collection<? extends GrantedAuthority> authorities = jwtAuthenticationConverter.convert(jwt).getAuthorities();
    boolean hasSeller = false;
    for (GrantedAuthority a : authorities) {
      if ("ROLE_SELLER".equals(a.getAuthority())) {
        hasSeller = true;
        break;
      }
    }
    assertTrue(hasSeller, "JWT should map to ROLE_SELLER from realm and resource access");
  }
}
