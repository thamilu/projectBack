package com.eshop.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "google")
public class GoogleProperties {
    private final Maps maps = new Maps();
    private String geolocationApiUrl;
    private String placesApiUrl;
    private String distanceMatrixApiUrl;

    public Maps getMaps() { return maps; }
    public String getGeolocationApiUrl() { return geolocationApiUrl; }
    public void setGeolocationApiUrl(String geolocationApiUrl) { this.geolocationApiUrl = geolocationApiUrl; }
    public String getPlacesApiUrl() { return placesApiUrl; }
    public void setPlacesApiUrl(String placesApiUrl) { this.placesApiUrl = placesApiUrl; }
    public String getDistanceMatrixApiUrl() { return distanceMatrixApiUrl; }
    public void setDistanceMatrixApiUrl(String distanceMatrixApiUrl) { this.distanceMatrixApiUrl = distanceMatrixApiUrl; }

    public static class Maps {
        private String apiKey;
        private boolean apiEnabled;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public boolean isApiEnabled() { return apiEnabled; }
        public void setApiEnabled(boolean apiEnabled) { this.apiEnabled = apiEnabled; }
    }
}
