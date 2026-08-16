package com.eshop.app.admin.application.service;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.core.kernel.ApiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Profile("!prod")
@RequiredArgsConstructor
public class OpenApiAdminProxyService {

    private final AppProperties appProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchOpenApiDocs() {
        String url = appProperties.getBackendUrl() + ApiConstants.API_DOCS_PATH;
        return restTemplate.getForObject(url, String.class);
    }
}
