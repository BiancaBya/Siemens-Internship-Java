package com.siemens.internship.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

    /**
     * Creates a fixed-size thread pool for item processing.
     * Pool size comes from application.properties (default 10).
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService processingExecutor(
            @Value("${app.processing.pool-size:10}") int poolSize
    ) {
        return Executors.newFixedThreadPool(poolSize);
    }
}


