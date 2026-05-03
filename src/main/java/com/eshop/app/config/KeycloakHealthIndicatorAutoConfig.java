package com.eshop.app.config;

import org.keycloak.admin.client.Keycloak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.micrometer.core.instrument.MeterRegistry;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Auto-configuration which registers a runtime `HealthIndicator` proxy for Keycloak
 * if Actuator health APIs and the Keycloak admin client are available on the classpath.
 *
 * This avoids compile-time coupling to Actuator types by using String-based
 * conditional checks and a dynamic proxy that implements the runtime HealthIndicator.
 */
@Configuration
@ConditionalOnClass(name = {"org.springframework.boot.actuate.health.HealthIndicator", "org.keycloak.admin.client.Keycloak"})
@ConditionalOnBean(Keycloak.class)
public class KeycloakHealthIndicatorAutoConfig {

    private static final Logger log = LoggerFactory.getLogger(KeycloakHealthIndicatorAutoConfig.class);

    @Bean
    public Object keycloakHealthIndicator(Keycloak keycloakAdminClient, KeycloakConfig config, MeterRegistry meterRegistry) {
        try {
            final Class<?> healthIndicatorClass = Class.forName("org.springframework.boot.actuate.health.HealthIndicator");
            final Class<?> healthClass = Class.forName("org.springframework.boot.actuate.health.Health");
            final Class<?> builderClass = Class.forName("org.springframework.boot.actuate.health.Health$Builder");

            final Method healthUpMethod = healthClass.getMethod("up"); // static
            final Method healthDownMethod = healthClass.getMethod("down"); // static
            final Method builderWithDetail = builderClass.getMethod("withDetail", String.class, Object.class);
            final Method builderBuild = builderClass.getMethod("build");

            InvocationHandler handler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    if ("health".equals(method.getName()) && (args == null || args.length == 0)) {
                        try {
                            String version = null;
                            try {
                                version = keycloakAdminClient.serverInfo().getInfo().getSystemInfo().getVersion();
                            } catch (Exception e) {
                                // ignore, we will mark down
                                log.debug("Failed reading Keycloak server info: {}", e.getMessage());
                            }

                            if (version != null) {
                                Object builder = healthUpMethod.invoke(null);
                                builderWithDetail.invoke(builder, "realm", config.getRealm());
                                builderWithDetail.invoke(builder, "serverVersion", version);
                                builderWithDetail.invoke(builder, "authServerUrl", config.getAuthServerUrl());
                                return builderBuild.invoke(builder);
                            } else {
                                Object builder = healthDownMethod.invoke(null);
                                builderWithDetail.invoke(builder, "realm", config.getRealm());
                                builderWithDetail.invoke(builder, "error", "unable to contact Keycloak");
                                return builderBuild.invoke(builder);
                            }
                        } catch (Throwable t) {
                            log.debug("Health check reflective invocation failed: {}", t.getMessage());
                            Object builder = healthDownMethod.invoke(null);
                            builderWithDetail.invoke(builder, "realm", config.getRealm());
                            builderWithDetail.invoke(builder, "error", t.getMessage());
                            return builderBuild.invoke(builder);
                        }
                    }
                    // If other methods are called on the proxy, return sensible defaults
                    if (method.getReturnType().equals(void.class)) return null;
                    return method.getReturnType().isPrimitive() ? 0 : null;
                }
            };

            Object proxy = Proxy.newProxyInstance(healthIndicatorClass.getClassLoader(), new Class[]{healthIndicatorClass}, handler);
            // Registering the proxy object as a bean of runtime HealthIndicator type
            return proxy;
        } catch (ClassNotFoundException e) {
            // Should not happen because of @ConditionalOnClass, but guard defensively
            log.debug("Actuator HealthIndicator class not available at runtime: {}", e.getMessage());
            return new KeycloakHealthIndicator();
        } catch (Exception e) {
            log.error("Failed to create Keycloak HealthIndicator proxy", e);
            return new KeycloakHealthIndicator();
        }
    }
}
