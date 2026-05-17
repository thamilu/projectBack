package com.eshop.app.seed.provider;

import com.eshop.app.core.infrastructure.config.properties.SeedProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JSON-based implementation of UserDataProvider.
 * Loads user data and resolves Spring property placeholders in passwords.
 */
@Slf4j
@Component
@Profile({ "dev", "test", "local" })
@RequiredArgsConstructor
public class JsonFileUserDataProvider implements UserDataProvider {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final Environment environment;

    @Override
    public List<SeedProperties.UserSeed> getUsers() {
        try {
            Resource resource = resourceLoader.getResource("classpath:seed/users.json");
            if (!resource.exists()) {
                log.warn("User seed data file not found: classpath:seed/users.json");
                return Collections.emptyList();
            }

            List<SeedProperties.UserSeed> users = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<SeedProperties.UserSeed>>() {
                    });

            // Resolve placeholders for sensitive fields (like passwords)
            users.forEach(user -> {
                if (user.getPassword() != null) {
                    user.setPassword(environment.resolvePlaceholders(user.getPassword()));
                }
            });

            return users;
        } catch (IOException e) {
            log.error("Failed to load user seed data: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
