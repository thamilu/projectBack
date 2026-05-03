package com.eshop.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String realm;
    private String authServerUrl;
    private String resource;
    private boolean publicClient;
    private boolean bearerOnly;

    public String getRealm() { return realm; }
    public void setRealm(String realm) { this.realm = realm; }
    public String getAuthServerUrl() { return authServerUrl; }
    public void setAuthServerUrl(String authServerUrl) { this.authServerUrl = authServerUrl; }
    public String getResource() { return resource; }
    public void setResource(String resource) { this.resource = resource; }
    public boolean isPublicClient() { return publicClient; }
    public void setPublicClient(boolean publicClient) { this.publicClient = publicClient; }
    public boolean isBearerOnly() { return bearerOnly; }
    public void setBearerOnly(boolean bearerOnly) { this.bearerOnly = bearerOnly; }
}
