package com.eshop.app.core.infrastructure.config.async;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Unified Async Configuration for the E-Shop application.
 *
 * <p>
 * This is the application's single {@link AsyncConfigurer} — Spring resolves the default
 * executor/exception-handler for bare {@code @Async} calls lazily via {@code ObjectProvider},
 * and throws {@code IllegalStateException: Only one AsyncConfigurer may exist} the first time
 * that lazy resolution runs if more than one {@link AsyncConfigurer} bean is present. Because the
 * resolution is lazy, that failure does not surface at context-startup or in a plain
 * context-load test — it surfaces on the first real invocation of a bare (unqualified)
 * {@code @Async} method, e.g. {@code ProductEventListener}'s {@code @TransactionalEventListener}
 * handlers for product-created / stock-changed / low-stock events. Keep exactly one
 * {@code AsyncConfigurer} implementation in this codebase.
 *
 * <p>
 * Provides multiple task executors optimized for different workload types:
 * <ul>
 * <li><b>eshopVirtualThreadExecutor</b> - Default executor using Java 21
 * virtual threads for I/O-bound tasks</li>
 * <li><b>cpuBoundExecutor</b> - Platform thread pool for CPU-intensive
 * tasks</li>
 * <li><b>dashboardExecutor</b> - Dedicated executor for dashboard data
 * aggregation</li>
 * <li><b>virtualThreadExecutor</b> - Virtual threads for general I/O-bound tasks</li>
 * <li><b>notificationExecutor</b> - Email/SMS/push notification delivery</li>
 * <li><b>auditExecutor</b> - Audit trail persistence</li>
 * <li><b>analyticsExecutor</b> - Background analytics processing</li>
 * <li><b>reportExecutor</b> - Report generation (CPU-intensive)</li>
 * <li><b>taskExecutor</b> - General-purpose platform-thread pool</li>
 * </ul>
 *
 * <p>
 * <b>Usage Examples:</b>
 *
 * <pre>
 * // Use default virtual thread executor (I/O-bound)
 * {@code @Async}
 * public CompletableFuture&lt;String&gt; fetchData() { ... }
 *
 * // Use CPU-bound executor for heavy coinputation
 * {@code @Async("cpuBoundExecutor")}
 * public CompletableFuture&lt;Report&gt; generateReport() { ... }
 *
 * // Use dashboard executor for dashboard operations
 * {@code @Async("dashboardExecutor")}
 * public CompletableFuture&lt;DashboardData&gt; loadDashboard() { ... }
 * </pre>
 *
 * @author E-Shop Team
 * @version 3.1
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

    @Value("${spring.task.execution.pool.core-size:10}")
    private int corePoolSize;

    @Value("${spring.task.execution.pool.max-size:50}")
    private int maxPoolSize;

    @Value("${spring.task.execution.pool.queue-capacity:100}")
    private int queueCapacity;

    /**
     * Propagates MDC context (correlationId) to platform-thread pool executors.
     */
    private static class MdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }

    /**
     * Virtual thread executor for I/O-bound async tasks (default).
     *
     * <p>
     * Benefits of virtual threads:
     * <ul>
     * <li>Extremely lightweight (millions can be created)</li>
     * <li>Perfect for I/O-bound operations (database, API calls)</li>
     * <li>Automatic scaling without pool size configuration</li>
     * <li>Better resource utilization than traditional thread pools</li>
     * </ul>
     */
    @Bean(name = "eshopVirtualThreadExecutor")
    @ConditionalOnMissingBean(name = "eshopVirtualThreadExecutor")
    public TaskExecutor virtualThreadExecutor() {
        log.info("Initializing virtual thread executor for async I/O-bound tasks");
        return new VirtualThreadTaskExecutor("async-vt-");
    }

    /**
     * General-purpose virtual thread executor (legacy bean name, kept for existing
     * {@code @Async("virtualThreadExecutor")} call sites).
     */
    @Bean(name = "virtualThreadExecutor")
    @ConditionalOnMissingBean(name = "virtualThreadExecutor")
    public Executor legacyVirtualThreadExecutor() {
        log.info("Configuring virtual thread executor (Java 21)");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Platform thread pool executor for CPU-bound async tasks.
     *
     * <p>
     * Use this for coinputationally intensive operations:
     * <ul>
     * <li>Image/video processing</li>
     * <li>Report generation with heavy calculations</li>
     * <li>Data transformation and ETL operations</li>
     * <li>Encryption/decryption</li>
     * </ul>
     *
     * <p>
     * Pool size is configured based on available CPU cores.
     */
    @Bean(name = "cpuBoundExecutor")
    @ConditionalOnMissingBean(name = "cpuBoundExecutor")
    public TaskExecutor cpuBoundExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int cores = Math.max(2, Runtime.getRuntime().availableProcessors());
        executor.setCorePoolSize(cores);
        executor.setMaxPoolSize(cores * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("cpu-async-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("Initialized CPU-bound executor with {} core threads, {} max threads",
                cores, cores * 2);
        return executor;
    }

    /**
     * Dedicated executor for dashboard data aggregation and analytics.
     *
     * <p>
     * Optimized for parallel dashboard queries without blocking other async
     * operations.
     * Use this executor for:
     * <ul>
     * <li>Admin dashboard data loading</li>
     * <li>Seller dashboard statistics</li>
     * <li>Real-time analytics aggregation</li>
     * <li>Multi-source data collection for dashboards</li>
     * </ul>
     */
    @Bean(name = "dashboardExecutor")
    @ConditionalOnMissingBean(name = "dashboardExecutor")
    public Executor dashboardExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Dashboard-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();

        log.info("Initialized dashboard executor with 4 core threads, 8 max threads");
        return executor;
    }

    /**
     * Executor for notification tasks (email, SMS, push).
     */
    @Bean(name = "notificationExecutor")
    @ConditionalOnMissingBean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        log.info("Configuring notification executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notification-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return executor;
    }

    /**
     * Executor for audit logging (async to avoid blocking main operations).
     */
    @Bean(name = "auditExecutor")
    @ConditionalOnMissingBean(name = "auditExecutor")
    public Executor auditExecutor() {
        log.info("Configuring audit executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("audit-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return executor;
    }

    /**
     * Executor for analytics and reporting tasks.
     */
    @Bean(name = "analyticsExecutor")
    @ConditionalOnMissingBean(name = "analyticsExecutor")
    public Executor analyticsExecutor() {
        log.info("Configuring analytics executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("analytics-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false); // Can drop unfinished analytics
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();

        return executor;
    }

    /**
     * Executor for report generation (CPU-intensive).
     */
    @Bean(name = "reportExecutor")
    @ConditionalOnMissingBean(name = "reportExecutor")
    public Executor reportExecutor() {
        log.info("Configuring report executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("report-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();

        return executor;
    }

    /**
     * General-purpose platform-thread executor (legacy bean name, kept for existing
     * {@code @Async("taskExecutor")} call sites, if any are added later).
     */
    @Bean(name = "taskExecutor")
    @ConditionalOnMissingBean(name = "taskExecutor")
    public Executor taskExecutor() {
        log.info("Configuring default task executor: core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, queueCapacity);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return (Executor) virtualThreadExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new EnhancedAsyncExceptionHandler();
    }

    /**
     * Exception handler for uncaught exceptions in bare {@code @Async} methods.
     * Logs detailed information (method, parameters, stack trace) about the failed
     * async operation and flags likely-critical failures.
     */
    private static class EnhancedAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

            log.error("Async exception in '{}': {}", methodName, ex.getMessage());
            log.error("Exception Type: {}", ex.getClass().getName());

            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    Object param = params[i];
                    String paramStr = param != null ? param.toString() : "null";
                    if (paramStr.length() > 200) {
                        paramStr = paramStr.substring(0, 200) + "... [truncated]";
                    }
                    log.error("Parameter[{}]: {}", i, paramStr);
                }
            }

            log.error("Stack trace:", ex);

            if (isCriticalError(ex)) {
                log.error("CRITICAL async error in {}: {}", methodName, ex.getMessage());
            }
        }

        /**
         * Determines if an exception should trigger critical alerts.
         */
        private boolean isCriticalError(Throwable ex) {
            return ex instanceof OutOfMemoryError
                    || ex instanceof StackOverflowError
                    || (ex.getCause() != null && ex.getCause() instanceof java.sql.SQLException);
        }
    }
}
