package com.example.jobscheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous task execution.
 * Sets up thread pools for handling asynchronous job scheduling.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Creates a thread pool task executor for job scheduling operations.
     * This executor is used by the AsyncJobService for asynchronous job operations.
     *
     * @return the configured executor
     */
    @Bean(name = "jobSchedulerTaskExecutor")
    public Executor jobSchedulerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);        // Base number of threads
        executor.setMaxPoolSize(10);        // Maximum number of threads
        executor.setQueueCapacity(25);      // Queue capacity for tasks when all threads are busy
        executor.setThreadNamePrefix("job-scheduler-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}