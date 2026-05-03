package com.eshop.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.WebContentInterceptor;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        WebContentInterceptor interceptor = new WebContentInterceptor();
        interceptor.setCacheControl(CacheControl.noCache().cachePrivate());
        interceptor.addCacheMapping(
            CacheControl.maxAge(5, TimeUnit.MINUTES).cachePrivate(),
            "/api/users/*/profile"
        );
        registry.addInterceptor(interceptor);
    }
}
