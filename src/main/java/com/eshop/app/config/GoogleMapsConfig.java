package com.eshop.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Google Maps and Geolocation services
 */
@Configuration
public class GoogleMapsConfig {
    
    @Value("${google.maps.api.key:}")
    private String apiKey;
    
    @Value("${google.maps.api.enabled:false}")
    private Boolean apiEnabled;
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public Boolean isApiEnabled() {
        return apiEnabled;
    }
}
