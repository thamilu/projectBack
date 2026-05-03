package com.eshop.app.health;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * MEDIUM-004 FIX: HikariCP Connection Pool Health Indicator (Reflective)
 * 
 * <p>
 * Monitors database connection pool health and exposes metrics via Spring Boot
 * Actuator.
 * Uses reflection and dynamic proxies to avoid direct compile-time coupling
 * with Actuator APIs,
 * consistent with the project's architectural pattern.
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class HikariConnectionPoolHealthIndicator {

    private final DataSource dataSource;

    @Bean("hikariPool")
    @ConditionalOnClass(name = "org.springframework.boot.actuate.health.HealthIndicator")
    public Object hikariPoolHealthIndicator() {
        try {
            final Class<?> healthIndicatorClass = Class
                    .forName("org.springframework.boot.actuate.health.HealthIndicator");
            final Class<?> healthClass = Class.forName("org.springframework.boot.actuate.health.Health");
            final Class<?> builderClass = Class.forName("org.springframework.boot.actuate.health.Health$Builder");
            final Class<?> statusClass = Class.forName("org.springframework.boot.actuate.health.Status");

            final Method healthUpMethod = healthClass.getMethod("up");
            final Method healthDownMethod = healthClass.getMethod("down");
            final Method healthStatusMethod = healthClass.getMethod("status", statusClass);
            final Method builderWithDetail = builderClass.getMethod("withDetail", String.class, Object.class);
            final Method builderBuild = builderClass.getMethod("build");

            InvocationHandler handler = (proxy, method, args) -> {
                if ("health".equals(method.getName()) && (args == null || args.length == 0)) {
                    return getHealth(healthUpMethod, healthDownMethod, healthStatusMethod, statusClass,
                            builderWithDetail, builderBuild);
                }
                return null;
            };

            return Proxy.newProxyInstance(healthIndicatorClass.getClassLoader(),
                    new Class[] { healthIndicatorClass }, handler);

        } catch (Exception e) {
            log.warn(
                    "Failed to register Hikari health indicator: Actuator might be missing or version incompatible: {}",
                    e.getMessage());
            return new Object(); // Fallback
        }
    }

    private Object getHealth(Method healthUpMethod, Method healthDownMethod, Method healthStatusMethod,
            Class<?> statusClass, Method builderWithDetail, Method builderBuild) throws Exception {

        if (!(dataSource instanceof HikariDataSource)) {
            Object builder = healthUpMethod.invoke(null);
            builderWithDetail.invoke(builder, "message", "Not a HikariDataSource");
            return builderBuild.invoke(builder);
        }

        HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
        HikariPoolMXBean poolProxy = hikariDataSource.getHikariPoolMXBean();

        if (poolProxy == null) {
            Object builder = healthUpMethod.invoke(null);
            builderWithDetail.invoke(builder, "message", "HikariPoolMXBean not initialized");
            return builderBuild.invoke(builder);
        }

        int active = poolProxy.getActiveConnections();
        int idle = poolProxy.getIdleConnections();
        int total = poolProxy.getTotalConnections();
        int waiting = poolProxy.getThreadsAwaitingConnection();
        int max = hikariDataSource.getMaximumPoolSize();

        double utilization = (double) active / max * 100;

        Object builder;
        if (waiting > 0 || utilization > 90) {
            builder = healthDownMethod.invoke(null);
            log.error("🚨 Critical connection pool utilization: {}%, Waiters: {}",
                    String.format("%.2f", utilization), waiting);
        } else if (utilization > 70) {
            Object degradedStatus = statusClass.getConstructor(String.class, String.class).newInstance("DEGRADED",
                    "Pool usage is high");
            builder = healthStatusMethod.invoke(null, degradedStatus);
            log.warn("⚠️ High connection pool utilization: {}%", String.format("%.2f", utilization));
        } else {
            builder = healthUpMethod.invoke(null);
        }

        builderWithDetail.invoke(builder, "activeConnections", active);
        builderWithDetail.invoke(builder, "idleConnections", idle);
        builderWithDetail.invoke(builder, "totalConnections", total);
        builderWithDetail.invoke(builder, "threadsAwaitingConnection", waiting);
        builderWithDetail.invoke(builder, "maxPoolSize", max);
        builderWithDetail.invoke(builder, "utilization", String.format("%.2f%%", utilization));

        return builderBuild.invoke(builder);
    }
}
