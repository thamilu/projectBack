package com.eshop.app.user.infrastructure.health;

import com.eshop.app.user.application.service.KeycloakAuthService;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a Spring Boot Actuator {@code HealthIndicator} for Keycloak connectivity, without
 * introducing a hard compile-time dependency on {@code spring-boot-starter-actuator}.
 *
 * <p><b>Why reflection/proxy:</b> Actuator classes are loaded via {@link Class#forName(String)}
 * and the returned {@code HealthIndicator} bean is created via a JDK dynamic {@link Proxy}. This
 * allows the module to run correctly whether or not Actuator is present on the classpath: if it
 * is absent, {@link Class#forName(String)} fails, the {@code catch} block logs a warning, and a
 * harmless placeholder bean is registered instead of failing application startup.
 *
 * <p><b>Maintenance note:</b> the actual connectivity-check decision logic lives in
 * {@link #checkKeycloakConnectivity()}, which has no dependency on reflection and can be unit
 * tested directly. Only the translation of that result into an Actuator {@code Health} object
 * requires reflection.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class KeycloakConnectivityHealthIndicator {

    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(2);
    private static final String GENERIC_FAILURE_DETAIL = "Keycloak connectivity check failed";
    private static final String INVALID_CONFIG_DETAIL = "Invalid configuration response";

    private final KeycloakAuthService authService;

    @Bean("keycloakConnectivityHealth")
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    public Object keycloakConnectivityHealthIndicator() {
        try {
            final Class<?> healthIndicatorClass =
                    Class.forName("org.springframework.boot.actuate.health.HealthIndicator");
            final Class<?> healthClass =
                    Class.forName("org.springframework.boot.actuate.health.Health");
            final Class<?> builderClass =
                    Class.forName("org.springframework.boot.actuate.health.Health$Builder");

            final Method healthUpMethod = healthClass.getMethod("up");
            final Method healthDownMethod = healthClass.getMethod("down");
            final Method builderWithDetail =
                    builderClass.getMethod("withDetail", String.class, Object.class);
            final Method builderBuild = builderClass.getMethod("build");

            InvocationHandler handler =
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return handleObjectMethod(proxy, method, args);
                        }
                        if ("health".equals(method.getName())
                                && (args == null || args.length == 0)) {
                            return buildHealth(
                                    checkKeycloakConnectivity(),
                                    healthUpMethod,
                                    healthDownMethod,
                                    builderWithDetail,
                                    builderBuild);
                        }
                        throw new UnsupportedOperationException(
                                "Unsupported method on health indicator proxy: " + method);
                    };

            return Proxy.newProxyInstance(
                    healthIndicatorClass.getClassLoader(),
                    new Class[] {healthIndicatorClass},
                    handler);

        } catch (Exception e) {
            log.warn(
                    "Failed to register Keycloak connectivity health indicator: Actuator might be"
                            + " missing: {}",
                    e.getMessage());
            return new Object();
        }
    }

    /**
     * Performs the actual Keycloak connectivity check by requesting the OpenID configuration.
     * Package-private and free of reflection so it can be unit tested directly by mocking
     * {@link KeycloakAuthService}, independent of the Actuator {@link Proxy} plumbing.
     *
     * @return the outcome of the connectivity check
     */
    ConnectivityResult checkKeycloakConnectivity() {
        try {
            Map<String, Object> config =
                    authService.getOpenIdConfiguration().block(HEALTH_CHECK_TIMEOUT);
            if (config != null && config.containsKey("issuer")) {
                return new ConnectivityResult(true, String.valueOf(config.get("issuer")), null);
            }
            log.warn(
                    "Keycloak connectivity check received an invalid OpenID configuration"
                            + " response (missing 'issuer')");
            return new ConnectivityResult(false, null, INVALID_CONFIG_DETAIL);
        } catch (Exception ex) {
            log.warn("Keycloak connectivity check failed", ex);
            return new ConnectivityResult(false, null, GENERIC_FAILURE_DETAIL);
        }
    }

    /**
     * Translates a {@link ConnectivityResult} into a reflectively-built Actuator {@code Health}
     * object.
     *
     * <p>Return values of {@code withDetail} invocations are intentionally not reused for further
     * chaining beyond direct sequential calls on the same {@code builder} reference, since the
     * Actuator {@code Health.Builder} contract mutates and returns the same instance.
     */
    private Object buildHealth(
            ConnectivityResult result,
            Method healthUpMethod,
            Method healthDownMethod,
            Method builderWithDetail,
            Method builderBuild)
            throws Exception {
        Object builder;
        if (result.up()) {
            builder = healthUpMethod.invoke(null);
            builderWithDetail.invoke(builder, "issuer", result.issuer());
        } else {
            builder = healthDownMethod.invoke(null);
            builderWithDetail.invoke(builder, "error", result.errorDetail());
        }
        return builderBuild.invoke(builder);
    }

    /**
     * Handles {@link Object}-declared methods ({@code equals}, {@code hashCode}, {@code toString})
     * invoked on the dynamic proxy. This is required per the {@link InvocationHandler} contract:
     * returning {@code null} for a primitive-returning method (such as {@code equals}/{@code
     * hashCode}) causes the JDK to throw a {@link NullPointerException} at proxy-dispatch time.
     */
    private static Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "KeycloakConnectivityHealthIndicatorProxy";
            default -> throw new UnsupportedOperationException(
                    "Unsupported Object method on health indicator proxy: " + method);
        };
    }

    /**
     * Outcome of a Keycloak connectivity check, independent of the Actuator {@code Health} type.
     *
     * @param up         whether Keycloak responded with a valid OpenID configuration
     * @param issuer     the reported issuer, present only when {@code up} is {@code true}
     * @param errorDetail a safe, non-sensitive detail message, present only when {@code up} is
     *                    {@code false}
     */
    record ConnectivityResult(boolean up, String issuer, String errorDetail) {}
}

