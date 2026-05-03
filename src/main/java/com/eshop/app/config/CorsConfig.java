package com.eshop.app.config;

import com.eshop.app.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final AppProperties appProperties;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                AppProperties.Cors cors = appProperties.getCors();
                if (!cors.isEnabled()) {
                    return;
                }

                registry.addMapping("/**")
                        .allowedOriginPatterns(cors.getAllowedOrigins().split(","))
                        .allowedMethods(cors.getAllowedMethods().split(","))
                        .allowedHeaders(cors.getAllowedHeaders().split(","))
                        .exposedHeaders(cors.getExposedHeaders().split(","))
                        .allowCredentials(cors.isAllowCredentials())
                        .maxAge(cors.getMaxAge());
            }
        };
    }
}
