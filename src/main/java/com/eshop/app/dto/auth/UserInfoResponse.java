package com.eshop.app.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    
    private String sub;
    
    @JsonProperty("email_verified")
    private Boolean emailVerified;
    
    private String name;
    
    @JsonProperty("preferred_username")
    private String preferredUsername;
    
    @JsonProperty("given_name")
    private String givenName;
    
    @JsonProperty("family_name")
    private String familyName;
    
    private String email;
    
    private List<String> roles;
    
    @JsonProperty("realm_access")
    private Map<String, Object> realmAccess;
    
    @JsonProperty("resource_access")
    private Map<String, Object> resourceAccess;
}
