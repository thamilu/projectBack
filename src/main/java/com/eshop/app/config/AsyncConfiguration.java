package com.eshop.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Unified Async Configuration for the E-Shop application.
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
 * // Use CPU-bound executor for heavy computation
 * {@code @Async("cpuBoundExecutor")}
 * public CompletableFuture&lt;Report&gt; generateReport() { ... }
 * 
 * // Use dashboard executor for dashboard operations
 * {@code @Async("dashboardExecutor")}
 * public CompletableFuture&lt;DashboardData&gt; loadDashboard() { ... }
 * </pre>
 * 
 * <p>
 * <b>Executor Selection Guide:</b>
 * <ul>
 * <li>Database queries, API calls, file I/O → Use default (virtual
 * threads)</li>
 * <li>Image processing, report generation, data analysis → Use
 * cpuBoundExecutor</li>
 * <li>Dashboard data aggregation → Use dashboardExecutor</li>
 * </ul>
 * 
 * @author E-Shop Team
 * @version 3.0
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

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
     * Platform thread pool executor for CPU-bound async tasks.
     * 
     * <p>
     * Use this for computationally intensive operations:
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
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();

        log.info("Initialized dashboard executor with 4 core threads, 8 max threads");
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return (Executor) virtualThreadExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    /**
     * Custom exception handler for uncaught exceptions in async methods.
     * Logs detailed information about the failed async operation.
     */
    private static class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {
        private static final Logger log = LoggerFactory.getLogger(AsyncExceptionHandler.class);

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error(
                    "Async method failed: {}.{}() with params: {}",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName(),
                    Arrays.toString(params),
                    ex);
        }
    }
}
