package com.gradion.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Executor for pipeline step runs. Gemini calls take 10–30s+ (longer for
 * images), so each step runs on its own worker thread and the HTTP request
 * returns immediately with the claimed RUNNING state.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "stepExecutor")
    public Executor stepExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("step-runner-");
        executor.initialize();
        return executor;
    }
}