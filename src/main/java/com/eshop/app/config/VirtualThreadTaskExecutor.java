package com.eshop.app.config;

import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TaskExecutor implementation using Java 21 Virtual Threads.
 */
public class VirtualThreadTaskExecutor implements TaskExecutor, AutoCloseable {

    private final ExecutorService executorService;

    public VirtualThreadTaskExecutor(String threadNamePrefix) {
        Assert.hasText(threadNamePrefix, "Thread name prefix must not be empty");
        this.executorService = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                .name(threadNamePrefix, 0)
                .factory()
        );
    }

    @Override
    public void execute(Runnable task) {
        executorService.execute(task);
    }

    @Override
    public void close() {
        executorService.shutdown();
    }
}
